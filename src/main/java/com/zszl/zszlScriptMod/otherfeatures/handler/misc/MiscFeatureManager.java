package com.zszl.zszlScriptMod.otherfeatures.handler.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.runtime.AutoReconnectRuntime;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.runtime.AutoRespawnRuntime;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MiscFeatureManager {

    public static final MiscFeatureManager INSTANCE = new MiscFeatureManager();

    private static final Map<String, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final int DEFAULT_AUTO_RECONNECT_DELAY_TICKS = 40;
    private static final int DEFAULT_AUTO_RECONNECT_MAX_ATTEMPTS = 3;
    private static final boolean DEFAULT_AUTO_RECONNECT_INFINITE_ATTEMPTS = false;
    private static final int DEFAULT_AUTO_RESPAWN_DELAY_TICKS = 20;

    private static int autoReconnectDelayTicks = DEFAULT_AUTO_RECONNECT_DELAY_TICKS;
    private static int autoReconnectMaxAttempts = DEFAULT_AUTO_RECONNECT_MAX_ATTEMPTS;
    private static boolean autoReconnectInfiniteAttempts = DEFAULT_AUTO_RECONNECT_INFINITE_ATTEMPTS;
    private static int autoRespawnDelayTicks = DEFAULT_AUTO_RESPAWN_DELAY_TICKS;

    private final AutoReconnectRuntime autoReconnectRuntime = new AutoReconnectRuntime();
    private final AutoRespawnRuntime autoRespawnRuntime = new AutoRespawnRuntime();

    static {
        register(new FeatureState("auto_reconnect", "自动重连", "被踢出或断开服务器后自动尝试重新连接到上一个服务器。"));
        register(new FeatureState("auto_respawn", "死亡自动复活", "死亡界面出现后自动发送复活请求并关闭死亡界面。"));
        loadConfig();
    }

    private MiscFeatureManager() {
    }

    public static final class FeatureState {
        public final String id;
        public final String name;
        public final String description;
        public final String valueLabel = "";
        public final float defaultValue = 0.0F;
        public final float minValue = 0.0F;
        public final float maxValue = 0.0F;
        public final boolean behaviorImplemented = true;

        private boolean enabled;
        private float value;
        private boolean statusHudEnabled = true;

        private FeatureState(String id, String name, String description) {
            this.id = safe(id);
            this.name = safe(name);
            this.description = safe(description);
        }

        public boolean supportsValue() {
            return false;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public float getValue() {
            return value;
        }

        public boolean isStatusHudEnabled() {
            return statusHudEnabled;
        }
    }

    public static List<FeatureState> getFeatures() {
        return new ArrayList<>(FEATURES.values());
    }

    public static FeatureState getFeature(String featureId) {
        return FEATURES.get(normalizeId(featureId));
    }

    public static boolean isManagedFeature(String featureId) {
        return FEATURES.containsKey(normalizeId(featureId));
    }

    public static boolean isEnabled(String featureId) {
        FeatureState state = getFeature(featureId);
        return state != null && state.enabled;
    }

    public static void toggleFeature(String featureId) {
        setEnabled(featureId, !isEnabled(featureId));
    }

    public static void setEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.enabled = enabled;
        saveConfig();
    }

    public static void setFeatureStatusHudEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.statusHudEnabled = enabled;
        saveConfig();
    }

    public static void resetFeature(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.enabled = false;
        state.statusHudEnabled = true;
        if ("auto_reconnect".equals(state.id)) {
            autoReconnectDelayTicks = DEFAULT_AUTO_RECONNECT_DELAY_TICKS;
            autoReconnectMaxAttempts = DEFAULT_AUTO_RECONNECT_MAX_ATTEMPTS;
            autoReconnectInfiniteAttempts = DEFAULT_AUTO_RECONNECT_INFINITE_ATTEMPTS;
        } else if ("auto_respawn".equals(state.id)) {
            autoRespawnDelayTicks = DEFAULT_AUTO_RESPAWN_DELAY_TICKS;
        }
        saveConfig();
    }

    public static int getAutoReconnectDelayTicks() {
        return autoReconnectDelayTicks;
    }

    public static void setAutoReconnectDelayTicks(int ticks) {
        autoReconnectDelayTicks = clampInt(ticks, 5, 200);
        saveConfig();
    }

    public static int getAutoReconnectMaxAttempts() {
        return autoReconnectMaxAttempts;
    }

    public static void setAutoReconnectMaxAttempts(int attempts) {
        autoReconnectMaxAttempts = clampInt(attempts, 1, 10);
        saveConfig();
    }

    public static boolean isAutoReconnectInfiniteAttempts() {
        return autoReconnectInfiniteAttempts;
    }

    public static void setAutoReconnectInfiniteAttempts(boolean infiniteAttempts) {
        autoReconnectInfiniteAttempts = infiniteAttempts;
        saveConfig();
    }

    public static int getAutoRespawnDelayTicks() {
        return autoRespawnDelayTicks;
    }

    public static void setAutoRespawnDelayTicks(int ticks) {
        autoRespawnDelayTicks = clampInt(ticks, 1, 100);
        saveConfig();
    }

    public static String getFeatureRuntimeSummary(String featureId) {
        return INSTANCE.buildFeatureRuntimeSummary(normalizeId(featureId));
    }

    public static List<String> getStatusLines() {
        return getStatusLines(false);
    }

    public static List<String> getStatusLines(boolean forcePreview) {
        if (!forcePreview && !MovementFeatureManager.isMasterStatusHudEnabled()) {
            return new ArrayList<>();
        }

        List<String> activeNames = new ArrayList<>();
        for (FeatureState state : FEATURES.values()) {
            if (state != null && state.enabled && (state.statusHudEnabled || forcePreview)) {
                activeNames.add(state.name);
            }
        }

        List<String> lines = new ArrayList<>();
        if (activeNames.isEmpty()) {
            return lines;
        }

        lines.add("§a[杂项] §f" + activeNames.size() + " 项开启");
        StringBuilder builder = new StringBuilder("§7");
        for (int i = 0; i < activeNames.size() && i < 4; i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(activeNames.get(i));
        }
        if (activeNames.size() > 4) {
            builder.append(" §8+").append(activeNames.size() - 4);
        }
        lines.add(builder.toString());

        String runtime = INSTANCE.getRuntimeHudLine();
        if (!runtime.isEmpty()) {
            lines.add(runtime);
        }
        return lines;
    }

    public static String getFeatureRuntimeSummaryLegacy(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return "未注册";
        }
        if (!state.enabled) {
            return "未启用";
        }
        if ("auto_reconnect".equals(state.id)) {
            ServerReconnectState reconnectState = INSTANCE.autoReconnectRuntime.snapshot();
            if (reconnectState.pending) {
                return "等待重连 / " + reconnectState.delayTicks + " tick / 第 "
                        + (reconnectState.attemptCount + 1) + " 次";
            }
            return autoReconnectInfiniteAttempts
                    ? "已启用 / 延迟 " + autoReconnectDelayTicks + " tick / 无限重试"
                    : "已启用 / 延迟 " + autoReconnectDelayTicks + " tick / 最多 " + autoReconnectMaxAttempts + " 次";
        }
        if ("auto_respawn".equals(state.id)) {
            int cooldown = INSTANCE.autoRespawnRuntime.getCooldownTicks();
            return cooldown > 0
                    ? "复活冷却中 / " + cooldown + " tick"
                    : "已启用 / 冷却 " + autoRespawnDelayTicks + " tick";
        }
        return "已启用";
    }

    public static void loadConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_misc.json");
            if (!Files.exists(file)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject features = root.has("features") && root.get("features").isJsonObject()
                    ? root.getAsJsonObject("features")
                    : root;
            autoReconnectDelayTicks = root.has("autoReconnectDelayTicks")
                    ? clampInt(root.get("autoReconnectDelayTicks").getAsInt(), 5, 200)
                    : DEFAULT_AUTO_RECONNECT_DELAY_TICKS;
            autoReconnectMaxAttempts = root.has("autoReconnectMaxAttempts")
                    ? clampInt(root.get("autoReconnectMaxAttempts").getAsInt(), 1, 10)
                    : DEFAULT_AUTO_RECONNECT_MAX_ATTEMPTS;
            autoReconnectInfiniteAttempts = root.has("autoReconnectInfiniteAttempts")
                    ? root.get("autoReconnectInfiniteAttempts").getAsBoolean()
                    : DEFAULT_AUTO_RECONNECT_INFINITE_ATTEMPTS;
            autoRespawnDelayTicks = root.has("autoRespawnDelayTicks")
                    ? clampInt(root.get("autoRespawnDelayTicks").getAsInt(), 1, 100)
                    : DEFAULT_AUTO_RESPAWN_DELAY_TICKS;
            for (FeatureState state : FEATURES.values()) {
                if (features.has(state.id) && features.get(state.id).isJsonObject()) {
                    JsonObject json = features.getAsJsonObject(state.id);
                    if (json.has("enabled")) {
                        state.enabled = json.get("enabled").getAsBoolean();
                    }
                    if (json.has("statusHudEnabled")) {
                        state.statusHudEnabled = json.get("statusHudEnabled").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载杂项功能配置失败", e);
        }
    }

    public static void saveConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_misc.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject features = new JsonObject();
            root.addProperty("autoReconnectDelayTicks", autoReconnectDelayTicks);
            root.addProperty("autoReconnectMaxAttempts", autoReconnectMaxAttempts);
            root.addProperty("autoReconnectInfiniteAttempts", autoReconnectInfiniteAttempts);
            root.addProperty("autoRespawnDelayTicks", autoRespawnDelayTicks);
            for (FeatureState state : FEATURES.values()) {
                JsonObject json = new JsonObject();
                json.addProperty("enabled", state.enabled);
                json.addProperty("statusHudEnabled", state.statusHudEnabled);
                features.add(state.id, json);
            }
            root.add("features", features);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存杂项功能配置失败", e);
        }
    }

    public void tick(Minecraft mc) {
        autoReconnectRuntime.tick(mc, isEnabled("auto_reconnect"), autoReconnectDelayTicks, autoReconnectMaxAttempts,
                autoReconnectInfiniteAttempts);
        autoRespawnRuntime.tick(mc, isEnabled("auto_respawn"), autoRespawnDelayTicks);
    }

    public void onClientConnected() {
        autoReconnectRuntime.clearState();
        autoRespawnRuntime.onClientDisconnect();
    }

    public void onClientDisconnect() {
        autoReconnectRuntime.onClientDisconnected(Minecraft.getInstance(), isEnabled("auto_reconnect"),
                autoReconnectDelayTicks);
        autoRespawnRuntime.onClientDisconnect();
    }

    public static final class ServerReconnectState {
        public final boolean pending;
        public final int delayTicks;
        public final int attemptCount;

        public ServerReconnectState(boolean pending, int delayTicks, int attemptCount) {
            this.pending = pending;
            this.delayTicks = delayTicks;
            this.attemptCount = attemptCount;
        }
    }

    private static void register(FeatureState state) {
        FEATURES.put(state.id, state);
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String normalizeId(String featureId) {
        return safe(featureId).toLowerCase(Locale.ROOT);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String getRuntimeHudLine() {
        List<String> parts = new ArrayList<>();
        if (isEnabled("auto_reconnect") && getFeature("auto_reconnect").statusHudEnabled) {
            ServerReconnectState reconnectState = autoReconnectRuntime.snapshot();
            if (reconnectState.pending) {
                parts.add((autoReconnectInfiniteAttempts ? "§b无限重连倒计时: " : "§b重连倒计时: ")
                        + reconnectState.delayTicks);
            } else {
                parts.add(autoReconnectInfiniteAttempts ? "§b自动重连(无限)" : "§b自动重连");
            }
        }
        if (isEnabled("auto_respawn") && getFeature("auto_respawn").statusHudEnabled) {
            parts.add("§a自动复活");
        }
        if (parts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("§7");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append("  ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String buildFeatureRuntimeSummary(String featureId) {
        if (featureId == null || featureId.isEmpty()) {
            return "待机";
        }
        if (!isManagedFeature(featureId)) {
            return "未找到功能";
        }
        switch (featureId) {
        case "auto_reconnect":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            ServerReconnectState reconnectState = autoReconnectRuntime.snapshot();
            if (reconnectState.pending) {
                return "等待重连 / " + reconnectState.delayTicks + " tick / 第 "
                        + (reconnectState.attemptCount + 1) + " 次";
            }
            return autoReconnectInfiniteAttempts
                    ? "已启用 / 延迟 " + autoReconnectDelayTicks + " tick / 无限重试"
                    : "已启用 / 延迟 " + autoReconnectDelayTicks + " tick / 最多 " + autoReconnectMaxAttempts + " 次";
        case "auto_respawn":
            int cooldown = autoRespawnRuntime.getCooldownTicks();
            return cooldown > 0 ? "复活冷却中 / " + cooldown + " tick"
                    : "已启用 / 冷却 " + autoRespawnDelayTicks + " tick";
        default:
            return "已启用";
        }
    }
}
