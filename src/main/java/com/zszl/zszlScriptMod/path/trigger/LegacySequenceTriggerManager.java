package com.zszl.zszlScriptMod.path.trigger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.WorldLoadSafety;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class LegacySequenceTriggerManager {
    public static final String CATEGORY_UNGROUPED = "未分组";

    public static final String TRIGGER_GUI_OPEN = "gui_open";
    public static final String TRIGGER_GUI_CLOSE = "gui_close";
    public static final String TRIGGER_CHAT = "chat";
    public static final String TRIGGER_PACKET = "packet";
    public static final String TRIGGER_TITLE = "title";
    public static final String TRIGGER_ACTIONBAR = "actionbar";
    public static final String TRIGGER_SCOREBOARD_CHANGED = "scoreboard_changed";
    public static final String TRIGGER_BOSSBAR = "bossbar";
    public static final String TRIGGER_KEY_INPUT = "key_input";
    public static final String TRIGGER_PLAYER_IDLE = "player_idle";
    public static final String TRIGGER_TIMER = "timer";
    public static final String TRIGGER_HP_LOW = "hp_low";
    public static final String TRIGGER_DEATH = "death";
    public static final String TRIGGER_RESPAWN = "respawn";
    public static final String TRIGGER_PLAYER_HURT = "player_hurt";
    public static final String TRIGGER_ATTACK_ENTITY = "attack_entity";
    public static final String TRIGGER_TARGET_KILL = "target_kill";
    public static final String TRIGGER_WORLD_CHANGED = "world_changed";
    public static final String TRIGGER_AREA_CHANGED = "area_changed";
    public static final String TRIGGER_INVENTORY_CHANGED = "inventory_changed";
    public static final String TRIGGER_INVENTORY_FULL = "inventory_full";
    public static final String TRIGGER_ENTITY_NEARBY = "entity_nearby";
    public static final String TRIGGER_PLAYER_LIST = "player_list";
    public static final String TRIGGER_ITEM_PICKUP = "item_pickup";
    public static final String TRIGGER_SERVER_CONNECT = "server_connect";
    public static final String TRIGGER_SERVER_DISCONNECT = "server_disconnect";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<TriggerRule> RULES = new CopyOnWriteArrayList<>();
    private static final List<String> CATEGORIES = new CopyOnWriteArrayList<>();
    private static final Map<String, Long> LAST_TRIGGER_TIMES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> ENABLED_RULE_COUNT_BY_TRIGGER = new ConcurrentHashMap<>();
    private static final Map<String, String> LAST_DEBUG_EVENT_STATE = new ConcurrentHashMap<>();
    private static final Map<String, String> LAST_DEBUG_RULE_STATE = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    public static final class RuleEditModel {
        public String name = "";
        public String category = CATEGORY_UNGROUPED;
        public boolean enabled = true;
        public String triggerType = TRIGGER_GUI_OPEN;
        public String contains = "";
        public JsonObject params = new JsonObject();
        public String sequenceName = "";
        public boolean backgroundExecution = false;
        public int cooldownMs = 1000;
        public String note = "";
    }

    private static final class ConfigRoot {
        private List<String> categories = new ArrayList<>();
        private List<TriggerRule> rules = new ArrayList<>();
    }

    private static final class TriggerRule {
        private String name = "";
        private String category = CATEGORY_UNGROUPED;
        private boolean enabled = true;
        private String triggerType = TRIGGER_GUI_OPEN;
        private String contains = "";
        private JsonObject params = new JsonObject();
        private String sequenceName = "";
        private boolean backgroundExecution = false;
        private int cooldownMs = 1000;
        private String note = "";
    }

    private LegacySequenceTriggerManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        reloadRules();
        initialized = true;
    }

    public static synchronized void reloadRules() {
        RULES.clear();
        CATEGORIES.clear();
        LAST_TRIGGER_TIMES.clear();
        ENABLED_RULE_COUNT_BY_TRIGGER.clear();
        LAST_DEBUG_EVENT_STATE.clear();
        LAST_DEBUG_RULE_STATE.clear();
        initialized = true;
        Path path = getConfigPath();
        ensureConfigExists(path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ConfigRoot root = GSON.fromJson(reader, ConfigRoot.class);
            if (root == null || root.rules == null) {
                ensureCategoriesSynced();
                return;
            }
            if (root.categories != null) {
                for (String category : root.categories) {
                    CATEGORIES.add(normalizeCategory(category));
                }
            }
            for (TriggerRule rule : root.rules) {
                if (rule == null || isBlank(rule.sequenceName)) {
                    continue;
                }
                rule.category = normalizeCategory(rule.category);
                rule.triggerType = normalizeTriggerType(rule.triggerType);
                rule.cooldownMs = Math.max(0, rule.cooldownMs);
                RULES.add(rule);
                if (rule.enabled) {
                    ENABLED_RULE_COUNT_BY_TRIGGER.merge(rule.triggerType, 1, Integer::sum);
                }
            }
            ensureCategoriesSynced();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LegacyTrigger] 加载规则失败", e);
            ensureCategoriesSynced();
        }
    }

    public static synchronized List<RuleEditModel> getRuleModels() {
        initialize();
        List<RuleEditModel> models = new ArrayList<>();
        for (TriggerRule rule : RULES) {
            if (rule == null) {
                continue;
            }
            RuleEditModel model = new RuleEditModel();
            model.name = safe(rule.name);
            model.category = normalizeCategory(rule.category);
            model.enabled = rule.enabled;
            model.triggerType = safe(rule.triggerType);
            model.contains = safe(rule.contains);
            model.params = copyJson(rule.params);
            model.sequenceName = safe(rule.sequenceName);
            model.backgroundExecution = rule.backgroundExecution;
            model.cooldownMs = Math.max(0, rule.cooldownMs);
            model.note = safe(rule.note);
            models.add(model);
        }
        return models;
    }

    public static synchronized void saveRuleModels(List<RuleEditModel> models) {
        saveRuleModels(models, null);
    }

    public static synchronized void saveRuleModels(List<RuleEditModel> models, List<String> categories) {
        Path path = getConfigPath();
        ensureConfigExists(path);
        ConfigRoot root = new ConfigRoot();
        root.categories = normalizeCategoryList(categories);
        root.rules = new ArrayList<>();
        if (models != null) {
            for (RuleEditModel model : models) {
                if (model == null || isBlank(model.sequenceName)) {
                    continue;
                }
                TriggerRule rule = new TriggerRule();
                rule.name = safe(model.name).trim();
                rule.category = normalizeCategory(model.category);
                rule.enabled = model.enabled;
                rule.triggerType = normalizeTriggerType(model.triggerType);
                rule.contains = safe(model.contains).trim();
                rule.params = sanitizeParams(rule.triggerType, model.params);
                rule.sequenceName = safe(model.sequenceName).trim();
                rule.backgroundExecution = model.backgroundExecution;
                rule.cooldownMs = Math.max(0, model.cooldownMs);
                rule.note = safe(model.note).trim();
                root.rules.add(rule);
                if (!containsCategoryIgnoreCase(root.categories, rule.category)) {
                    root.categories.add(rule.category);
                }
            }
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LegacyTrigger] 保存规则失败", e);
        }
        reloadRules();
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        initialize();
        ensureCategoriesSynced();
        return new ArrayList<>(CATEGORIES);
    }

    public static void triggerEvent(String triggerType, JsonObject eventData) {
        initialize();
        String normalizedType = normalizeTriggerType(triggerType);
        if (normalizedType.isEmpty()) {
            return;
        }
        int enabledRuleCount = ENABLED_RULE_COUNT_BY_TRIGGER.getOrDefault(normalizedType, 0);
        if (enabledRuleCount <= 0) {
            return;
        }
        if (!TRIGGER_SERVER_DISCONNECT.equals(normalizedType)
                && WorldLoadSafety.shouldDeferAutomation(Minecraft.getInstance())) {
            return;
        }

        boolean debugEnabled = isDebugEnabled();
        String searchText = buildSearchText(eventData);
        long now = System.currentTimeMillis();
        if (debugEnabled) {
            emitTriggerEventDebug(normalizedType, eventData);
        }
        for (TriggerRule rule : RULES) {
            if (rule == null || !rule.enabled) {
                continue;
            }
            if (!normalizedType.equals(rule.triggerType)) {
                continue;
            }
            if (!matchesRule(rule, eventData, searchText)) {
                if (debugEnabled) {
                    emitTriggerRuleDebug(normalizedType, rule, "未命中", "未通过当前触发条件匹配。", "miss");
                }
                continue;
            }

            String ruleKey = normalizedType + "|" + safe(rule.name) + "|" + safe(rule.sequenceName);
            long lastTime = LAST_TRIGGER_TIMES.containsKey(ruleKey) ? LAST_TRIGGER_TIMES.get(ruleKey) : 0L;
            if (rule.cooldownMs > 0 && now - lastTime < rule.cooldownMs) {
                if (debugEnabled) {
                    long remainMs = Math.max(0L, rule.cooldownMs - (now - lastTime));
                    emitTriggerRuleDebug(normalizedType, rule, "冷却中",
                            "剩余冷却 " + remainMs + "ms", "cooldown|" + (remainMs / 250L));
                }
                continue;
            }

            PathSequence sequence = PathSequenceManager.getSequence(rule.sequenceName);
            if (sequence == null || sequence.getSteps().isEmpty()) {
                if (debugEnabled) {
                    emitTriggerRuleDebug(normalizedType, rule, "序列缺失",
                            "目标序列不存在或为空: " + safe(rule.sequenceName),
                            "missing_sequence|" + safe(rule.sequenceName));
                }
                continue;
            }

            writeTriggerContext(normalizedType, eventData, rule);
            LAST_TRIGGER_TIMES.put(ruleKey, now);

            if (rule.backgroundExecution) {
                if (debugEnabled) {
                    emitTriggerRuleDebug(normalizedType, rule, "已执行",
                            "已执行后台序列: " + safe(rule.sequenceName),
                            "executed_bg|" + now, true);
                }
                PathSequenceEventListener.startBackgroundSequence(sequence, 1);
            } else {
                if (debugEnabled) {
                    emitTriggerRuleDebug(normalizedType, rule, "已执行",
                            "已执行前台序列: " + safe(rule.sequenceName),
                            "executed_fg|" + now, true);
                }
                PathSequenceManager.runPathSequenceOnce(rule.sequenceName);
            }
        }
    }

    public static boolean hasRulesForTrigger(String triggerType) {
        initialize();
        String normalizedType = normalizeTriggerType(triggerType);
        if (normalizedType.isEmpty()) {
            return false;
        }
        return ENABLED_RULE_COUNT_BY_TRIGGER.getOrDefault(normalizedType, 0) > 0;
    }

    public static boolean isDebugEnabled() {
        return ModConfig.isDebugFlagEnabled(DebugModule.TRIGGER_RULES);
    }

    private static void ensureCategoriesSynced() {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (String category : CATEGORIES) {
            String safeCategory = normalizeCategory(category);
            normalized.put(safeCategory.toLowerCase(Locale.ROOT), safeCategory);
        }
        for (TriggerRule rule : RULES) {
            if (rule == null) {
                continue;
            }
            String safeCategory = normalizeCategory(rule.category);
            rule.category = safeCategory;
            normalized.put(safeCategory.toLowerCase(Locale.ROOT), safeCategory);
        }
        if (normalized.isEmpty()) {
            normalized.put(CATEGORY_UNGROUPED.toLowerCase(Locale.ROOT), CATEGORY_UNGROUPED);
        }
        CATEGORIES.clear();
        CATEGORIES.addAll(normalized.values());
    }

    private static List<String> normalizeCategoryList(List<String> categories) {
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        if (categories != null) {
            for (String category : categories) {
                String safeCategory = normalizeCategory(category);
                normalized.put(safeCategory.toLowerCase(Locale.ROOT), safeCategory);
            }
        }
        if (!normalized.containsKey(CATEGORY_UNGROUPED.toLowerCase(Locale.ROOT))) {
            normalized.put(CATEGORY_UNGROUPED.toLowerCase(Locale.ROOT), CATEGORY_UNGROUPED);
        }
        return new ArrayList<>(normalized.values());
    }

    private static boolean containsCategoryIgnoreCase(List<String> values, String target) {
        String normalizedTarget = normalizeCategory(target);
        for (String value : values) {
            if (normalizeCategory(value).equalsIgnoreCase(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeCategory(String category) {
        String normalized = safe(category).trim();
        return normalized.isEmpty() ? CATEGORY_UNGROUPED : normalized;
    }

    private static boolean matchesRule(TriggerRule rule, JsonObject eventData, String searchText) {
        if (rule == null) {
            return false;
        }
        if (!matchesContains(rule.contains, searchText)) {
            return false;
        }
        JsonObject params = sanitizeParams(rule.triggerType, rule.params);
        switch (rule.triggerType) {
            case TRIGGER_GUI_OPEN:
            case TRIGGER_GUI_CLOSE:
                return matchesGuiOpen(params, eventData);
            case TRIGGER_CHAT:
                return matchesTextParam(params, "chatText", eventData, "message");
            case TRIGGER_PACKET:
                return matchesPacket(params, eventData);
            case TRIGGER_TITLE:
            case TRIGGER_ACTIONBAR:
            case TRIGGER_SCOREBOARD_CHANGED:
            case TRIGGER_BOSSBAR:
                return matchesTextParam(params, "text", eventData, "text");
            case TRIGGER_KEY_INPUT:
                return matchesTextParam(params, "keyName", eventData, "keyName");
            case TRIGGER_PLAYER_IDLE:
                return matchesPlayerIdle(params, eventData);
            case TRIGGER_TIMER:
                return matchesTimer(params, eventData);
            case TRIGGER_HP_LOW:
                return matchesHpLow(params, eventData);
            case TRIGGER_PLAYER_HURT:
                return matchesDamageEvent(params, eventData);
            case TRIGGER_ATTACK_ENTITY:
            case TRIGGER_TARGET_KILL:
                return matchesTextParam(params, "entityText", eventData, "entityName");
            case TRIGGER_ENTITY_NEARBY:
                return matchesEntityNearby(params, eventData);
            case TRIGGER_PLAYER_LIST:
                return matchesPlayerList(params, eventData);
            case TRIGGER_ITEM_PICKUP:
                return matchesItemPickup(params, eventData);
            case TRIGGER_WORLD_CHANGED:
                return matchesAreaChanged(params, eventData);
            case TRIGGER_AREA_CHANGED:
                return matchesAreaChanged(params, eventData);
            case TRIGGER_INVENTORY_CHANGED:
                return matchesTextParam(params, "inventoryText", eventData, "after");
            case TRIGGER_INVENTORY_FULL:
                return matchesInventoryFull(params, eventData);
            case TRIGGER_DEATH:
            case TRIGGER_RESPAWN:
            case TRIGGER_SERVER_CONNECT:
            case TRIGGER_SERVER_DISCONNECT:
            default:
                return true;
        }
    }

    private static boolean matchesGuiOpen(JsonObject params, JsonObject eventData) {
        String expectedTitle = getStringParam(params, "guiTitle");
        String expectedClass = getStringParam(params, "guiClass");
        String actualTitle = getStringValue(eventData, "title");
        String actualGui = getStringValue(eventData, "gui");
        if (!expectedTitle.isEmpty() && !safe(actualTitle).toLowerCase(Locale.ROOT)
                .contains(expectedTitle.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!expectedClass.isEmpty() && !safe(actualGui).toLowerCase(Locale.ROOT)
                .contains(expectedClass.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return true;
    }

    private static boolean matchesPacket(JsonObject params, JsonObject eventData) {
        String packetText = getStringParam(params, "packetText");
        String channel = getStringParam(params, "channel");
        String direction = getStringParam(params, "direction");
        if (!packetText.isEmpty()) {
            String combined = (getStringValue(eventData, "packet") + " | "
                    + getStringValue(eventData, "decoded") + " | "
                    + getStringValue(eventData, "packetClass")).toLowerCase(Locale.ROOT);
            if (!combined.contains(packetText.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (!channel.isEmpty() && !getStringValue(eventData, "channel").toLowerCase(Locale.ROOT)
                .contains(channel.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!direction.isEmpty() && !direction.equalsIgnoreCase(getStringValue(eventData, "direction"))) {
            return false;
        }
        return true;
    }

    private static boolean matchesTimer(JsonObject params, JsonObject eventData) {
        int intervalSeconds = Math.max(1, getIntParam(params, "intervalSeconds", 1));
        long tick = getLongParam(eventData, "tick", 0L);
        return intervalSeconds <= 1 || tick % (intervalSeconds * 20L) == 0L;
    }

    private static boolean matchesPlayerIdle(JsonObject params, JsonObject eventData) {
        long requiredIdleMs = Math.max(0L, getLongParam(params, "idleMs", 1000L));
        boolean excludePathTracking = getBooleanParam(params, "excludePathTracking", true);
        boolean ignoreDamageReset = getBooleanParam(params, "ignoreDamageReset", false);
        long actualIdleMs;
        if (excludePathTracking) {
            actualIdleMs = ignoreDamageReset
                    ? Math.max(0L, getLongParam(eventData, "idleMsExcludingPathTrackingIgnoringDamage", 0L))
                    : Math.max(0L, getLongParam(eventData, "idleMsExcludingPathTracking", 0L));
        } else {
            actualIdleMs = ignoreDamageReset
                    ? Math.max(0L, getLongParam(eventData, "idleMsIgnoringDamage", 0L))
                    : Math.max(0L, getLongParam(eventData, "idleMs", 0L));
        }
        return actualIdleMs >= requiredIdleMs;
    }

    private static boolean matchesHpLow(JsonObject params, JsonObject eventData) {
        double threshold = getDoubleParam(params, "hpThreshold", 6.0D);
        double hp = getDoubleParam(eventData, "hp", Double.MAX_VALUE);
        return hp <= threshold;
    }

    private static boolean matchesEntityNearby(JsonObject params, JsonObject eventData) {
        String entityText = getStringParam(params, "entityText");
        int minCount = Math.max(0, getIntParam(params, "minCount", 1));
        long count = getLongParam(eventData, "count", 0L);
        if (count < minCount) {
            return false;
        }
        if (entityText.isEmpty()) {
            return true;
        }
        return getStringValue(eventData, "after").toLowerCase(Locale.ROOT)
                .contains(entityText.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesItemPickup(JsonObject params, JsonObject eventData) {
        String itemText = getStringParam(params, "itemText");
        int minCount = Math.max(0, getIntParam(params, "minCount", 1));
        long count = getLongParam(eventData, "count", 0L);
        if (count < minCount) {
            return false;
        }
        if (itemText.isEmpty()) {
            return true;
        }
        String itemName = getStringValue(eventData, "itemName");
        String registryName = getStringValue(eventData, "registryName");
        String combined = (itemName + " | " + registryName).toLowerCase(Locale.ROOT);
        return combined.contains(itemText.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesPlayerList(JsonObject params, JsonObject eventData) {
        return PlayerListTriggerSupport.matchesConfiguredPlayers(params, eventData);
    }

    private static boolean matchesDamageEvent(JsonObject params, JsonObject eventData) {
        double minDamage = getDoubleParam(params, "minDamage", 0.0D);
        double damage = getDoubleParam(eventData, "damage", 0.0D);
        if (damage < minDamage) {
            return false;
        }
        String expectedSource = getStringParam(params, "damageSource");
        if (expectedSource.isEmpty()) {
            return true;
        }
        String actualSource = getStringValue(eventData, "damageSource");
        return actualSource.toLowerCase(Locale.ROOT).contains(expectedSource.toLowerCase(Locale.ROOT));
    }

    private static boolean matchesAreaChanged(JsonObject params, JsonObject eventData) {
        String fromText = getStringParam(params, "fromText");
        String toText = getStringParam(params, "toText");
        if (!fromText.isEmpty() && !getStringValue(eventData, "from").toLowerCase(Locale.ROOT)
                .contains(fromText.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!toText.isEmpty() && !getStringValue(eventData, "to").toLowerCase(Locale.ROOT)
                .contains(toText.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return true;
    }

    private static boolean matchesInventoryFull(JsonObject params, JsonObject eventData) {
        int minFilledSlots = Math.max(0, getIntParam(params, "minFilledSlots", 0));
        long filledSlots = getLongParam(eventData, "filledSlots", 0L);
        return filledSlots >= minFilledSlots;
    }

    private static boolean matchesTextParam(JsonObject params, String paramKey, JsonObject eventData, String dataKey) {
        String expected = getStringParam(params, paramKey);
        if (expected.isEmpty()) {
            return true;
        }
        return getStringValue(eventData, dataKey).toLowerCase(Locale.ROOT)
                .contains(expected.toLowerCase(Locale.ROOT));
    }

    private static void writeTriggerContext(String triggerType, JsonObject eventData, TriggerRule rule) {
        Map<String, Object> triggerMap = new LinkedHashMap<>();
        triggerMap.put("type", triggerType);
        triggerMap.put("timestamp", System.currentTimeMillis());
        triggerMap.put("ruleName", safe(rule == null ? "" : rule.name));
        triggerMap.put("sequenceName", safe(rule == null ? "" : rule.sequenceName));
        if (eventData != null) {
            for (Map.Entry<String, JsonElement> entry : eventData.entrySet()) {
                triggerMap.put(entry.getKey(), toJavaValue(entry.getValue()));
            }
        }
        ScopedRuntimeVariables.setGlobalValue("trigger", triggerMap);
        ScopedRuntimeVariables.setGlobalValue("triggerType", triggerType);
    }

    private static Object toJavaValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean();
            }
            if (value.getAsJsonPrimitive().isNumber()) {
                try {
                    double number = value.getAsDouble();
                    long whole = (long) number;
                    return number == whole ? whole : number;
                } catch (Exception ignored) {
                    return value.getAsString();
                }
            }
            return value.getAsString();
        }
        if (value.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement child : value.getAsJsonArray()) {
                list.add(toJavaValue(child));
            }
            return list;
        }
        if (value.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), toJavaValue(entry.getValue()));
            }
            return map;
        }
        return value.toString();
    }

    private static boolean matchesContains(String contains, String searchText) {
        String expected = safe(contains).trim().toLowerCase(Locale.ROOT);
        if (expected.isEmpty()) {
            return true;
        }
        return safe(searchText).toLowerCase(Locale.ROOT).contains(expected);
    }

    private static JsonObject copyJson(JsonObject source) {
        if (source == null) {
            return new JsonObject();
        }
        try {
            return new JsonParser().parse(source.toString()).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private static JsonObject sanitizeParams(String triggerType, JsonObject source) {
        JsonObject sanitized = copyJson(source);
        if (sanitized == null) {
            sanitized = new JsonObject();
        }
        normalizeStringParam(sanitized, "guiTitle");
        normalizeStringParam(sanitized, "guiClass");
        normalizeStringParam(sanitized, "chatText");
        normalizeStringParam(sanitized, "packetText");
        normalizeStringParam(sanitized, "channel");
        normalizeStringParam(sanitized, "direction");
        normalizeStringParam(sanitized, "text");
        normalizeStringParam(sanitized, "keyName");
        normalizeStringParam(sanitized, "damageSource");
        normalizeStringParam(sanitized, "serverText");
        normalizeStringParam(sanitized, "fromText");
        normalizeStringParam(sanitized, "toText");
        normalizeStringParam(sanitized, "inventoryText");
        normalizeStringParam(sanitized, "entityText");
        normalizeStringParam(sanitized, "itemText");
        sanitizeBooleanParam(sanitized, "excludePathTracking", true);
        sanitizeBooleanParam(sanitized, "ignoreDamageReset", false);
        sanitizeIntParam(sanitized, "idleMs", 1000, 0);
        sanitizeIntParam(sanitized, "intervalSeconds", 1, 1);
        sanitizeIntParam(sanitized, "minCount", 1, 0);
        sanitizeIntParam(sanitized, "minFilledSlots", 0, 0);
        sanitizeDoubleParam(sanitized, "minDamage", 0.0D, 0.0D);
        sanitizeDoubleParam(sanitized, "hpThreshold", 6.0D, 0.0D);
        if (TRIGGER_PLAYER_LIST.equals(triggerType)) {
            PlayerListTriggerSupport.sanitizeParams(sanitized);
        }
        return sanitized;
    }

    private static void normalizeStringParam(JsonObject params, String key) {
        String value = getStringParam(params, key);
        if (value.isEmpty()) {
            params.remove(key);
        } else {
            params.addProperty(key, value);
        }
    }

    private static void sanitizeIntParam(JsonObject params, String key, int fallback, int minValue) {
        int value = Math.max(minValue, getIntParam(params, key, fallback));
        params.addProperty(key, value);
    }

    private static void sanitizeBooleanParam(JsonObject params, String key, boolean fallback) {
        params.addProperty(key, getBooleanParam(params, key, fallback));
    }

    private static void sanitizeDoubleParam(JsonObject params, String key, double fallback, double minValue) {
        double value = Math.max(minValue, getDoubleParam(params, key, fallback));
        params.addProperty(key, value);
    }

    private static String getStringParam(JsonObject params, String key) {
        if (params == null || key == null || !params.has(key)) {
            return "";
        }
        try {
            return safe(params.get(key).getAsString()).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static int getIntParam(JsonObject params, String key, int fallback) {
        if (params == null || key == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getDoubleParam(JsonObject params, String key, double fallback) {
        if (params == null || key == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long getLongParam(JsonObject params, String key, long fallback) {
        if (params == null || key == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsLong();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBooleanParam(JsonObject params, String key, boolean fallback) {
        if (params == null || key == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String getStringValue(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return "";
        }
        try {
            return safe(object.get(key).getAsString());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String buildSearchText(JsonObject eventData) {
        if (eventData == null || eventData.entrySet().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, JsonElement> entry : eventData.entrySet()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static void emitTriggerEventDebug(String triggerType, JsonObject eventData) {
        String message = "[" + getTriggerDisplayName(triggerType) + "] " + shortenDebugText(buildSearchText(eventData), 120);
        String stateKey = triggerType + "|" + shortenDebugText(buildSearchText(eventData), 80);
        String previous = LAST_DEBUG_EVENT_STATE.put(triggerType, stateKey);
        if (!stateKey.equals(previous)) {
            ModConfig.debugPrint(DebugModule.TRIGGER_RULES, message);
        }
    }

    private static void emitTriggerRuleDebug(String triggerType, TriggerRule rule, String status, String detail,
            String stateKey) {
        emitTriggerRuleDebug(triggerType, rule, status, detail, stateKey, false);
    }

    private static void emitTriggerRuleDebug(String triggerType, TriggerRule rule, String status, String detail,
            String stateKey, boolean alwaysLog) {
        String key = safe(triggerType) + "|" + safe(rule == null ? "" : rule.name)
                + "|" + safe(rule == null ? "" : rule.sequenceName) + "|" + safe(status);
        String actualStateKey = safe(stateKey);
        String previous = LAST_DEBUG_RULE_STATE.put(key, actualStateKey);
        if (!alwaysLog && actualStateKey.equals(previous)) {
            return;
        }
        ModConfig.debugPrint(DebugModule.TRIGGER_RULES,
                "[" + getTriggerDisplayName(triggerType) + "][" + getRuleDisplayName(rule) + "] "
                        + status + (isBlank(detail) ? "" : " | " + detail));
    }

    private static String getTriggerDisplayName(String triggerType) {
        switch (safe(triggerType)) {
            case TRIGGER_GUI_OPEN:
                return "界面打开";
            case TRIGGER_GUI_CLOSE:
                return "界面关闭";
            case TRIGGER_CHAT:
                return "聊天";
            case TRIGGER_PACKET:
                return "数据包";
            case TRIGGER_TITLE:
                return "标题";
            case TRIGGER_ACTIONBAR:
                return "ActionBar";
            case TRIGGER_SCOREBOARD_CHANGED:
                return "记分板";
            case TRIGGER_BOSSBAR:
                return "Boss条";
            case TRIGGER_KEY_INPUT:
                return "按键";
            case TRIGGER_PLAYER_IDLE:
                return "站立不动";
            case TRIGGER_TIMER:
                return "定时器";
            case TRIGGER_HP_LOW:
                return "低血量";
            case TRIGGER_DEATH:
                return "死亡";
            case TRIGGER_RESPAWN:
                return "复活";
            case TRIGGER_PLAYER_HURT:
                return "受击";
            case TRIGGER_ATTACK_ENTITY:
                return "攻击实体";
            case TRIGGER_TARGET_KILL:
                return "击杀目标";
            case TRIGGER_WORLD_CHANGED:
                return "世界切换";
            case TRIGGER_AREA_CHANGED:
                return "区域切换";
            case TRIGGER_INVENTORY_CHANGED:
                return "背包变化";
            case TRIGGER_INVENTORY_FULL:
                return "背包满";
            case TRIGGER_ENTITY_NEARBY:
                return "附近实体";
            case TRIGGER_PLAYER_LIST:
                return "玩家列表";
            case TRIGGER_ITEM_PICKUP:
                return "拾取物品";
            case TRIGGER_SERVER_CONNECT:
                return "连接服务器";
            case TRIGGER_SERVER_DISCONNECT:
                return "断开服务器";
            default:
                return safe(triggerType);
        }
    }

    private static String getRuleDisplayName(TriggerRule rule) {
        if (rule == null) {
            return "<空规则>";
        }
        String name = safe(rule.name).trim();
        return name.isEmpty() ? "<未命名规则>" : name;
    }

    private static String shortenDebugText(String text, int maxLength) {
        String value = safe(text);
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static synchronized Path getConfigPath() {
        return ProfileManager.getCurrentProfileDir().resolve("legacy_sequence_trigger_rules.json");
    }

    private static void ensureConfigExists(Path path) {
        if (path == null || Files.exists(path)) {
            return;
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("{\"rules\":[]}");
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LegacyTrigger] 创建规则文件失败", e);
        }
    }

    private static String normalizeTriggerType(String triggerType) {
        String value = safe(triggerType).trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "";
        }
        if (TRIGGER_GUI_OPEN.equals(value)
                || TRIGGER_GUI_CLOSE.equals(value)
                || TRIGGER_CHAT.equals(value)
                || TRIGGER_PACKET.equals(value)
                || TRIGGER_TITLE.equals(value)
                || TRIGGER_ACTIONBAR.equals(value)
                || TRIGGER_SCOREBOARD_CHANGED.equals(value)
                || TRIGGER_BOSSBAR.equals(value)
                || TRIGGER_KEY_INPUT.equals(value)
                || TRIGGER_PLAYER_IDLE.equals(value)
                || TRIGGER_TIMER.equals(value)
                || TRIGGER_HP_LOW.equals(value)
                || TRIGGER_DEATH.equals(value)
                || TRIGGER_RESPAWN.equals(value)
                || TRIGGER_PLAYER_HURT.equals(value)
                || TRIGGER_ATTACK_ENTITY.equals(value)
                || TRIGGER_TARGET_KILL.equals(value)
                || TRIGGER_WORLD_CHANGED.equals(value)
                || TRIGGER_AREA_CHANGED.equals(value)
                || TRIGGER_INVENTORY_CHANGED.equals(value)
                || TRIGGER_INVENTORY_FULL.equals(value)
                || TRIGGER_ENTITY_NEARBY.equals(value)
                || TRIGGER_PLAYER_LIST.equals(value)
                || TRIGGER_ITEM_PICKUP.equals(value)
                || TRIGGER_SERVER_CONNECT.equals(value)
                || TRIGGER_SERVER_DISCONNECT.equals(value)) {
            return value;
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

