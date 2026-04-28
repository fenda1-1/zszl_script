package com.zszl.zszlScriptMod.path.node;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.zszlScriptMod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class NodeTriggerManager {

    public static final String TRIGGER_CHAT = "onchat";
    public static final String TRIGGER_PACKET = "onpacket";
    public static final String TRIGGER_GUI_OPEN = "onguiopen";
    public static final String TRIGGER_HP_LOW = "onhplow";
    public static final String TRIGGER_TIMER = "ontimer";
    public static final String TRIGGER_DEATH = "ondeath";
    public static final String TRIGGER_RESPAWN = "onrespawn";
    public static final String TRIGGER_AREA_CHANGED = "onareachanged";
    public static final String TRIGGER_INVENTORY_CHANGED = "oninventorychanged";
    public static final String TRIGGER_INVENTORY_FULL = "oninventoryfull";
    public static final String TRIGGER_ENTITY_NEARBY = "onentitynearby";

    private static final int DEFAULT_MAX_CONCURRENT_RUNS = 8;
    private static final long STORAGE_REFRESH_CHECK_INTERVAL_MS = 3000L;
    private static final Map<String, Long> lastTriggerTimeByKey = new LinkedHashMap<>();
    private static final List<ActiveRun> activeRuns = new ArrayList<>();
    private static final Map<String, Boolean> cachedTriggerAvailability = new LinkedHashMap<>();
    private static volatile long cachedTriggerFileTimestamp = Long.MIN_VALUE;
    private static volatile long nextStorageRefreshCheckAt = 0L;
    private static volatile NodeSequenceStorage.LoadResult cachedLoadResult = NodeSequenceStorage.LoadResult
            .success(NodeSequenceStorage.CURRENT_VERSION, new ArrayList<NodeGraph>());

    private NodeTriggerManager() {
    }

    public static synchronized void tick() {
        if (activeRuns.isEmpty()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            activeRuns.clear();
            return;
        }

        List<ActiveRun> finished = new ArrayList<>();
        for (ActiveRun run : activeRuns) {
            try {
                run.runner.tick(player);
                if (run.runner.getContext().isCompleted()
                        || run.runner.getContext().hasError()
                        || run.runner.getContext().isPaused()) {
                    finished.add(run);
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("[NodeTrigger] 触发图运行异常: {}", run.graphName, e);
                finished.add(run);
            }
        }
        activeRuns.removeAll(finished);
    }

    public static synchronized TriggerResult trigger(String triggerType, JsonObject eventData) {
        String normalizedType = normalize(triggerType);
        if (normalizedType.isEmpty()) {
            return TriggerResult.ignored("triggerType 为空");
        }

        NodeSequenceStorage.LoadResult loadResult = getCachedLoadResult();
        if (!loadResult.isCompatible()) {
            return TriggerResult.failed("节点图存储不兼容: " + loadResult.getMessage());
        }

        List<NodeGraph> matchedGraphs = findTriggeredGraphs(loadResult.getSequences(), normalizedType, eventData);
        if (matchedGraphs.isEmpty()) {
            return TriggerResult.ignored("无匹配触发图: " + normalizedType);
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return TriggerResult.failed("当前无玩家实例");
        }

        int started = 0;
        for (NodeGraph graph : matchedGraphs) {
            if (activeRuns.size() >= DEFAULT_MAX_CONCURRENT_RUNS) {
                break;
            }
            if (!allowTrigger(graph, normalizedType, eventData)) {
                continue;
            }

            NodeExecutionContext context = new NodeExecutionContext();
            if (eventData != null) {
                context.setVariablesFromJson(eventData);
            }
            context.setVariable("triggerType", normalizedType);
            context.setVariable("triggerSource", buildTriggerSource(normalizedType, eventData));

            NodeSequenceRunner runner = new NodeSequenceRunner(graph, context);
            activeRuns.add(new ActiveRun(graph.getName(), normalizedType, runner));
            rememberTrigger(graph, normalizedType);
            started++;
        }

        if (started <= 0) {
            return TriggerResult.ignored("触发被节流或并发上限阻止");
        }
        return TriggerResult.started(started, matchedGraphs.size());
    }

    public static synchronized boolean hasGraphsForTrigger(String triggerType) {
        String normalizedType = normalize(triggerType);
        if (normalizedType.isEmpty()) {
            return false;
        }
        refreshCachedLoadResultIfNeeded();
        return cachedTriggerAvailability.getOrDefault(normalizedType, Boolean.FALSE);
    }

    public static synchronized void preload() {
        nextStorageRefreshCheckAt = 0L;
        refreshCachedLoadResultIfNeeded();
    }

    public static synchronized void invalidateCache() {
        cachedTriggerAvailability.clear();
        cachedTriggerFileTimestamp = Long.MIN_VALUE;
        nextStorageRefreshCheckAt = 0L;
        cachedLoadResult = NodeSequenceStorage.LoadResult.success(NodeSequenceStorage.CURRENT_VERSION,
                new ArrayList<NodeGraph>());
    }

    public static synchronized List<String> getActiveRunSummaries() {
        if (activeRuns.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        for (ActiveRun run : activeRuns) {
            NodeExecutionContext context = run.runner.getContext();
            lines.add(run.graphName + " | " + run.triggerType + " | current=" + context.getCurrentNodeId());
        }
        return lines;
    }

    private static List<NodeGraph> findTriggeredGraphs(List<NodeGraph> graphs, String triggerType, JsonObject eventData) {
        List<NodeGraph> result = new ArrayList<>();
        for (NodeGraph graph : graphs) {
            if (graph == null || graph.getNodes() == null) {
                continue;
            }
            for (NodeNode node : graph.getNodes()) {
                if (node == null || !NodeNode.TYPE_TRIGGER.equals(node.getNormalizedType())) {
                    continue;
                }
                JsonObject data = node.getData() == null ? new JsonObject() : node.getData();
                String nodeTriggerType = normalize(readString(data, "triggerType", "event", "type"));
                if (!triggerType.equals(nodeTriggerType)) {
                    continue;
                }
                if (!matchesFilter(data, eventData)) {
                    continue;
                }
                result.add(graph);
                break;
            }
        }
        return result;
    }

    private static NodeSequenceStorage.LoadResult getCachedLoadResult() {
        refreshCachedLoadResultIfNeeded();
        return cachedLoadResult;
    }

    private static void refreshCachedLoadResultIfNeeded() {
        long now = System.currentTimeMillis();
        if (cachedLoadResult != null && now < nextStorageRefreshCheckAt) {
            return;
        }
        nextStorageRefreshCheckAt = now + STORAGE_REFRESH_CHECK_INTERVAL_MS;

        long currentTimestamp = resolveStorageTimestamp();
        if (currentTimestamp == cachedTriggerFileTimestamp && cachedLoadResult != null) {
            return;
        }

        NodeSequenceStorage.LoadResult loadResult = NodeSequenceStorage.loadAll();
        cachedLoadResult = loadResult;
        cachedTriggerFileTimestamp = currentTimestamp;
        rebuildTriggerAvailability(loadResult);
    }

    private static long resolveStorageTimestamp() {
        Path storageFile = NodeSequenceStorage.getStorageFile();
        try {
            return Files.exists(storageFile) ? Files.getLastModifiedTime(storageFile).toMillis() : -1L;
        } catch (Exception ignored) {
            return Long.MIN_VALUE + 1L;
        }
    }

    private static void rebuildTriggerAvailability(NodeSequenceStorage.LoadResult loadResult) {
        cachedTriggerAvailability.clear();
        if (loadResult == null || !loadResult.isCompatible()) {
            return;
        }

        for (NodeGraph graph : loadResult.getSequences()) {
            if (graph == null || graph.getNodes() == null) {
                continue;
            }
            for (NodeNode node : graph.getNodes()) {
                if (node == null || !NodeNode.TYPE_TRIGGER.equals(node.getNormalizedType())) {
                    continue;
                }
                JsonObject data = node.getData() == null ? new JsonObject() : node.getData();
                String nodeTriggerType = normalize(readString(data, "triggerType", "event", "type"));
                if (!nodeTriggerType.isEmpty()) {
                    cachedTriggerAvailability.put(nodeTriggerType, Boolean.TRUE);
                }
            }
        }
    }

    private static boolean matchesFilter(JsonObject triggerData, JsonObject eventData) {
        if (triggerData == null) {
            return true;
        }
        String contains = readString(triggerData, "contains", "filter", "keyword");
        if (contains.isEmpty()) {
            return true;
        }
        if (eventData == null) {
            return false;
        }
        String text = readString(eventData, "message", "packet", "gui", "text");
        return !text.isEmpty() && text.toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT));
    }

    private static boolean allowTrigger(NodeGraph graph, String triggerType, JsonObject eventData) {
        long now = System.currentTimeMillis();
        String key = buildTriggerKey(graph, triggerType);
        long throttleMs = resolveThrottleMs(graph, triggerType);
        Long last = lastTriggerTimeByKey.get(key);
        return last == null || throttleMs <= 0L || now - last.longValue() >= throttleMs;
    }

    private static long resolveThrottleMs(NodeGraph graph, String triggerType) {
        if (graph == null || graph.getNodes() == null) {
            return 0L;
        }
        for (NodeNode node : graph.getNodes()) {
            if (node == null || !NodeNode.TYPE_TRIGGER.equals(node.getNormalizedType())) {
                continue;
            }
            JsonObject data = node.getData() == null ? new JsonObject() : node.getData();
            String nodeTriggerType = normalize(readString(data, "triggerType", "event", "type"));
            if (!triggerType.equals(nodeTriggerType)) {
                continue;
            }
            if (data.has("throttleMs") && data.get("throttleMs").isJsonPrimitive()
                    && data.get("throttleMs").getAsJsonPrimitive().isNumber()) {
                return Math.max(0L, data.get("throttleMs").getAsLong());
            }
        }
        return 0L;
    }

    private static void rememberTrigger(NodeGraph graph, String triggerType) {
        lastTriggerTimeByKey.put(buildTriggerKey(graph, triggerType), System.currentTimeMillis());
    }

    private static String buildTriggerKey(NodeGraph graph, String triggerType) {
        return (graph == null ? "" : safe(graph.getName())) + "|" + normalize(triggerType);
    }

    private static String buildTriggerSource(String triggerType, JsonObject eventData) {
        String normalized = normalize(triggerType);
        String detail = readString(eventData, "message", "packet", "gui", "text", "source");
        if (detail.length() > 64) {
            detail = detail.substring(0, 64) + "...";
        }
        if (detail.isEmpty()) {
            return normalized;
        }
        return normalized + ": " + detail;
    }

    private static String readString(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key != null && object.has(key) && !object.get(key).isJsonNull()) {
                return object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : object.get(key).toString();
            }
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class ActiveRun {
        private final String graphName;
        private final String triggerType;
        private final NodeSequenceRunner runner;

        private ActiveRun(String graphName, String triggerType, NodeSequenceRunner runner) {
            this.graphName = graphName == null ? "" : graphName;
            this.triggerType = triggerType == null ? "" : triggerType;
            this.runner = runner;
        }
    }

    public static final class TriggerResult {
        private final boolean started;
        private final String message;
        private final int startedCount;
        private final int matchedCount;

        private TriggerResult(boolean started, String message, int startedCount, int matchedCount) {
            this.started = started;
            this.message = message == null ? "" : message;
            this.startedCount = startedCount;
            this.matchedCount = matchedCount;
        }

        public static TriggerResult started(int startedCount, int matchedCount) {
            return new TriggerResult(true, "started", startedCount, matchedCount);
        }

        public static TriggerResult ignored(String message) {
            return new TriggerResult(false, message, 0, 0);
        }

        public static TriggerResult failed(String message) {
            return new TriggerResult(false, message, 0, 0);
        }

        public boolean isStarted() {
            return started;
        }

        public String getMessage() {
            return message;
        }

        public int getStartedCount() {
            return startedCount;
        }

        public int getMatchedCount() {
            return matchedCount;
        }
    }
}



