package com.zszl.zszlScriptMod.otherfeatures.handler.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorldFeatureManager {

    public static final WorldFeatureManager INSTANCE = new WorldFeatureManager();

    private static final Map<String, FeatureState> FEATURES = new LinkedHashMap<>();
    private static long visualWorldTime = 6000L;

    private long cachedNaturalWorldTime = 6000L;
    private float cachedNaturalRainStrength = 0.0F;
    private float cachedNaturalThunderStrength = 0.0F;
    private boolean timeOverrideActive;
    private boolean weatherOverrideActive;
    private BlockPos lastPlayerPos = BlockPos.ZERO;
    private String lastBiomeName = "unknown";
    private final List<StructureInfo> nearbyStructures = new ArrayList<>();

    static {
        register(new FeatureState("time_modifier", "时间修改", "自定义世界时间。", "时间值", 6000.0F, 0.0F, 24000.0F));
        register(new FeatureState("weather_control", "天气控制", "控制雨雪天气强度。", "雨强度", 0.0F, 0.0F, 1.0F));
        register(new FeatureState("coord_display", "坐标显示", "增强版坐标HUD。", "", 0.0F, 0.0F, 0.0F));
        loadConfig();
    }

    private WorldFeatureManager() {
    }

    public static final class FeatureState {
        public final String id;
        public final String name;
        public final String description;
        public final String valueLabel;
        public final float defaultValue;
        public final float minValue;
        public final float maxValue;
        public final boolean behaviorImplemented = true;

        private boolean enabled;
        private float value;
        private boolean statusHudEnabled = true;

        private FeatureState(String id, String name, String description, String valueLabel,
                float defaultValue, float minValue, float maxValue) {
            this.id = safe(id);
            this.name = safe(name);
            this.description = safe(description);
            this.valueLabel = valueLabel == null ? "" : valueLabel.trim();
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.value = defaultValue;
        }

        public boolean supportsValue() {
            return !valueLabel.isEmpty() && maxValue > minValue;
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

    public static final class StructureInfo {
        public final String type;
        public final BlockPos position;
        public final double distance;

        public StructureInfo(String type, BlockPos position, double distance) {
            this.type = safe(type);
            this.position = position == null ? BlockPos.ZERO : position;
            this.distance = distance;
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
        if ("time_modifier".equals(state.id)) {
            visualWorldTime = (long) state.value;
        }
        saveConfig();
    }

    public static void setValue(String featureId, float value) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.value = clamp(value, state.minValue, state.maxValue);
        if ("time_modifier".equals(state.id)) {
            visualWorldTime = (long) state.value;
        }
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
        state.value = state.defaultValue;
        state.statusHudEnabled = true;
        if ("time_modifier".equals(state.id)) {
            visualWorldTime = (long) state.defaultValue;
        }
        saveConfig();
    }

    public static boolean shouldOverrideVisualTime(Level world) {
        return world instanceof ClientLevel && isEnabled("time_modifier");
    }

    public static long getVisualWorldTime() {
        return visualWorldTime;
    }

    public void tick(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            resetRuntimeState();
            return;
        }

        updateCoordinateCache(mc);
        applyTimeModifier(mc);
        applyWeatherControl(mc);
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

        lines.add("§a[世界] §f" + activeNames.size() + " 项开启");
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

    public static String getCoordInfo() {
        if (!isEnabled("coord_display")) {
            return "";
        }
        BlockPos pos = INSTANCE.lastPlayerPos;
        if (pos == null) {
            pos = BlockPos.ZERO;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("§fXYZ: §a")
                .append(pos.getX()).append(" §f/ §a")
                .append(pos.getY()).append(" §f/ §a")
                .append(pos.getZ());
        if (!INSTANCE.lastBiomeName.isEmpty()) {
            builder.append(" §7生物群系: §b").append(INSTANCE.lastBiomeName);
        }
        return builder.toString();
    }

    public static List<StructureInfo> getNearbyStructures() {
        return new ArrayList<>(INSTANCE.nearbyStructures);
    }

    public static String getFeatureRuntimeSummaryLegacy(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return "未注册";
        }
        if (!state.enabled) {
            return "未启用";
        }
        if ("coord_display".equals(state.id)) {
            return "已启用 / " + INSTANCE.lastPlayerPos.getX() + ", " + INSTANCE.lastPlayerPos.getY() + ", "
                    + INSTANCE.lastPlayerPos.getZ();
        }
        if ("weather_control".equals(state.id)) {
            return "已启用 / 雨强度 " + formatFloat(state.value);
        }
        return state.supportsValue() ? "已启用 / " + formatFloat(state.value) : "已启用";
    }

    public static void loadConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_world.json");
            if (!Files.exists(file)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject features = root.has("features") && root.get("features").isJsonObject()
                    ? root.getAsJsonObject("features")
                    : root;
            for (FeatureState state : FEATURES.values()) {
                if (features.has(state.id) && features.get(state.id).isJsonObject()) {
                    JsonObject json = features.getAsJsonObject(state.id);
                    if (json.has("enabled")) {
                        state.enabled = json.get("enabled").getAsBoolean();
                    }
                    if (json.has("value")) {
                        state.value = clamp(json.get("value").getAsFloat(), state.minValue, state.maxValue);
                    }
                    if (json.has("statusHudEnabled")) {
                        state.statusHudEnabled = json.get("statusHudEnabled").getAsBoolean();
                    }
                }
            }
            FeatureState timeState = getFeature("time_modifier");
            if (timeState != null) {
                visualWorldTime = (long) timeState.value;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载世界功能配置失败", e);
        }
    }

    public static void saveConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_world.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject features = new JsonObject();
            for (FeatureState state : FEATURES.values()) {
                JsonObject json = new JsonObject();
                json.addProperty("enabled", state.enabled);
                json.addProperty("value", state.value);
                json.addProperty("statusHudEnabled", state.statusHudEnabled);
                features.add(state.id, json);
            }
            root.add("features", features);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存世界功能配置失败", e);
        }
    }

    public void onClientDisconnect() {
        resetRuntimeState();
    }

    private void updateCoordinateCache(Minecraft mc) {
        this.lastPlayerPos = mc.player.blockPosition();
        try {
            this.lastBiomeName = mc.level.getBiome(this.lastPlayerPos)
                    .unwrapKey()
                    .map(resourceKey -> resourceKey.identifier().toString())
                    .orElse("unknown");
        } catch (Exception ignored) {
            this.lastBiomeName = "unknown";
        }
    }

    private void applyTimeModifier(Minecraft mc) {
        if (!isEnabled("time_modifier")) {
            if (this.timeOverrideActive && mc.level != null) {
                mc.level.getLevelData().setDayTime(this.cachedNaturalWorldTime);
            }
            this.timeOverrideActive = false;
            return;
        }
        this.cachedNaturalWorldTime = mc.level.getDayTime();
        visualWorldTime = Math.round(getFeature("time_modifier").value);
        mc.level.getLevelData().setDayTime(visualWorldTime);
        this.timeOverrideActive = true;
    }

    private void applyWeatherControl(Minecraft mc) {
        if (!isEnabled("weather_control")) {
            if (this.weatherOverrideActive && mc.level != null) {
                mc.level.setRainLevel(this.cachedNaturalRainStrength);
                mc.level.setThunderLevel(this.cachedNaturalThunderStrength);
            }
            this.weatherOverrideActive = false;
            return;
        }
        this.cachedNaturalRainStrength = mc.level.getRainLevel(1.0F);
        this.cachedNaturalThunderStrength = mc.level.getThunderLevel(1.0F);
        float strength = clamp(getFeature("weather_control").value, 0.0F, 1.0F);
        mc.level.setRainLevel(strength);
        mc.level.setThunderLevel(strength);
        this.weatherOverrideActive = true;
    }

    private void resetRuntimeState() {
        this.timeOverrideActive = false;
        this.weatherOverrideActive = false;
        this.lastPlayerPos = BlockPos.ZERO;
        this.lastBiomeName = "unknown";
        this.nearbyStructures.clear();
    }

    private String getRuntimeHudLine() {
        List<String> parts = new ArrayList<>();
        if (isEnabled("time_modifier") && getFeature("time_modifier").statusHudEnabled) {
            parts.add("§e时间: " + formatWorldTime(visualWorldTime));
        }
        if (isEnabled("weather_control") && getFeature("weather_control").statusHudEnabled) {
            parts.add("§b雨强: " + formatFloat(getFeature("weather_control").value));
        }
        if (isEnabled("coord_display") && getFeature("coord_display").statusHudEnabled) {
            parts.add(String.format(Locale.ROOT, "§f%d,%d,%d",
                    lastPlayerPos.getX(), lastPlayerPos.getY(), lastPlayerPos.getZ()));
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
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return "未找到功能";
        }
        if (!state.enabled) {
            return "未启用";
        }
        switch (featureId) {
        case "time_modifier":
            return "当前时间 " + formatWorldTime(visualWorldTime);
        case "weather_control":
            return "当前雨强 " + formatFloat(state.value);
        case "coord_display":
            return lastPlayerPos.getX() + ", " + lastPlayerPos.getY() + ", " + lastPlayerPos.getZ()
                    + (lastBiomeName.isEmpty() ? "" : " / " + lastBiomeName);
        default:
            return state.supportsValue() ? "已启用 / " + formatFloat(state.value) : "已启用";
        }
    }

    private String formatWorldTime(long time) {
        long wrapped = ((time % 24000L) + 24000L) % 24000L;
        int hours = (int) ((wrapped / 1000L + 6L) % 24L);
        int minutes = (int) ((wrapped % 1000L) * 60L / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
