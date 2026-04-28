package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MovementFeatureManager {

    public static final MovementFeatureManager INSTANCE = new MovementFeatureManager();

    public static final float DEFAULT_STEP_HEIGHT = 0.6F;
    public static final float DEFAULT_COLLISION_REDUCTION = 0.0F;
    public static final float DEFAULT_TIMER_SPEED = 1.0F;

    private static final Map<String, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final LiquidWalkSettings LIQUID_WALK_SETTINGS = new LiquidWalkSettings();
    private static boolean masterStatusHudEnabled = true;
    private static int masterStatusHudX = 6;
    private static int masterStatusHudY = 6;

    boolean externalTimerApplied = false;
    float lastTimerSpeed = DEFAULT_TIMER_SPEED;
    int blockPhaseStuckTicks = 0;
    int longJumpChargeTicks = 0;
    int longJumpCooldownTicks = 0;
    int longJumpBoostTicks = 0;
    double longJumpBoostSpeed = 0.0D;
    boolean wasLongJumpSneakDown = false;
    int blinkCooldownTicks = 0;
    boolean wasBlinkTriggerDown = false;
    int scaffoldPlaceCooldownTicks = 0;
    int obstacleAvoidDirection = 0;
    int obstacleAvoidTicks = 0;
    double lastProtectionSafeMotionX = 0.0D;
    double lastProtectionSafeMotionY = 0.0D;
    double lastProtectionSafeMotionZ = 0.0D;

    static {
        register(new FeatureState("no_slow", "不受减速", "尽量维持使用物品时的移动速度。", "保速系数", 0.85F, 0.40F, 1.20F));
        register(new FeatureState("force_sprint", "强制疾跑", "检测到移动输入时自动保持疾跑状态。", "", 0.0F, 0.0F, 0.0F));
        register(new FeatureState("anti_knockback", "防击退", "受击后尽量快速清空水平击退位移。", "", 0.0F, 0.0F, 0.0F));
        register(new FeatureState("gui_move", "GUI界面下移动", "打开 GUI 时依旧允许 WASD、跳跃和下蹲。", "", 0.0F, 0.0F, 0.0F));
        register(new FeatureState("auto_step", "自动台阶", "自动抬升 1 到 2 格台阶。", "台阶高度", 1.50F, 1.00F, 2.00F));
        register(new FeatureState("block_phase", "方块穿透", "预留轻微脱困的方块碰撞修正。", "脱困强度", 0.12F, 0.05F, 0.40F));
        register(new FeatureState("no_collision", "无碰撞", "尽量减少与实体之间的推动和挤压。", "", 0.0F, 0.0F, 0.0F));
        register(new FeatureState("long_jump", "长距离跳跃", "预留长距离跳跃能力。", "蓄力秒数", 1.20F, 0.20F, 3.00F));
        register(new FeatureState("timer_accel", "定时器", "预留客户端时钟倍率调整。", "Timer倍率", 1.10F, 1.00F, 12.50F));
        register(new FeatureState("blink_move", "闪烁移动", "预留短距离瞬间位移能力。", "闪烁距离", 3.00F, 1.00F, 8.00F));
        register(new FeatureState("safe_walk", "安全行走", "模拟原版潜行时的贴边防掉落。", "边缘预留", 0.35F, 0.10F, 1.00F));
        register(new FeatureState("scaffold", "脚手架", "预留边走边自动铺方块能力。", "铺路距离", 1.00F, 1.00F, 4.00F));
        register(new FeatureState("low_gravity", "低重力模式", "降低下落速度，让空中停留更久。", "下落系数", 0.72F, 0.45F, 0.98F));
        register(new FeatureState("ice_boost", "冰面加速", "在冰面上额外提高水平速度。", "冰面倍率", 1.25F, 1.00F, 2.20F));
        register(new FeatureState("lava_walk", "液体行走", "将液体表面临时视作可行走平台。", "托举力度", 0.90F, 0.40F, 1.50F));
        register(new FeatureState("auto_obstacle_avoid", "自动避障", "预留自动绕开近距离障碍物能力。", "探测距离", 1.50F, 0.50F, 4.00F));
        register(new FeatureState("hover_mode", "悬停模式", "空中尝试保持定点悬停。", "垂直漂移", 0.00F, -0.08F, 0.08F));
        register(new FeatureState("fall_cushion", "下落缓冲", "快速下落接近地面时自动削弱坠落速度。", "缓冲速度", 0.24F, 0.10F, 0.60F));
        register(new FeatureState("no_fall", "无摔伤", "落地前自动补发落地状态，尽量规避摔落伤害。", "", 0.0F, 0.0F, 0.0F));
        register(new FeatureState("anti_arrow_knockback", "反击飞", "尽量压低箭矢命中后的击退位移。", "抵消强度", 0.72F, 0.00F, 0.95F));
        loadConfig();
    }

    private MovementFeatureManager() {
    }

    public static void register() {
        LivingKnockBackEvent.BUS.addListener(INSTANCE::onLivingKnockBack);
    }

    public static final class FeatureState {
        public final String id;
        public final String name;
        public final String description;
        public final String valueLabel;
        public final float defaultValue;
        public final float minValue;
        public final float maxValue;
        public final boolean behaviorImplemented;

        private boolean enabled;
        private float value;
        private boolean statusHudEnabled = true;

        private FeatureState(String id, String name, String description, String valueLabel, float defaultValue,
                float minValue, float maxValue) {
            this.id = safe(id);
            this.name = safe(name);
            this.description = safe(description);
            this.valueLabel = valueLabel == null ? "" : valueLabel.trim();
            this.defaultValue = defaultValue;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.behaviorImplemented = isFeatureBehaviorImplemented(this.id);
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

    public static final class LiquidWalkSettings {
        public static final boolean DEFAULT_WALK_ON_WATER = true;
        public static final boolean DEFAULT_DANGEROUS_ONLY = false;
        public static final boolean DEFAULT_SNEAK_TO_DESCEND = false;

        private boolean walkOnWater = DEFAULT_WALK_ON_WATER;
        private boolean dangerousOnly = DEFAULT_DANGEROUS_ONLY;
        private boolean sneakToDescend = DEFAULT_SNEAK_TO_DESCEND;

        private LiquidWalkSettings() {
        }

        private LiquidWalkSettings(LiquidWalkSettings other) {
            this.walkOnWater = other.walkOnWater;
            this.dangerousOnly = other.dangerousOnly;
            this.sneakToDescend = other.sneakToDescend;
        }

        public boolean isWalkOnWater() {
            return walkOnWater;
        }

        public boolean isDangerousOnly() {
            return dangerousOnly;
        }

        public boolean isSneakToDescend() {
            return sneakToDescend;
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

    public static void setValue(String featureId, float value) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.value = clamp(value, state.minValue, state.maxValue);
        saveConfig();
    }

    public static float getConfiguredValue(String featureId, float fallback) {
        FeatureState state = getFeature(featureId);
        return state == null ? fallback : state.value;
    }

    public static boolean isMasterStatusHudEnabled() {
        return masterStatusHudEnabled;
    }

    public static void setMasterStatusHudEnabled(boolean enabled) {
        masterStatusHudEnabled = enabled;
        saveConfig();
    }

    public static int getMasterStatusHudX() {
        return masterStatusHudX;
    }

    public static int getMasterStatusHudY() {
        return masterStatusHudY;
    }

    public static void setMasterStatusHudPositionTransient(int x, int y) {
        masterStatusHudX = Math.max(0, x);
        masterStatusHudY = Math.max(0, y);
    }

    public static void persistMasterStatusHudPosition() {
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
        if ("lava_walk".equals(state.id)) {
            setLiquidWalkSettings(LiquidWalkSettings.DEFAULT_WALK_ON_WATER, LiquidWalkSettings.DEFAULT_DANGEROUS_ONLY,
                    LiquidWalkSettings.DEFAULT_SNEAK_TO_DESCEND);
        }
        saveConfig();
    }

    public static LiquidWalkSettings getLiquidWalkSettings() {
        return new LiquidWalkSettings(LIQUID_WALK_SETTINGS);
    }

    public static boolean shouldLiquidWalkOnWater() {
        return isEnabled("lava_walk") && LIQUID_WALK_SETTINGS.walkOnWater;
    }

    public static boolean isLiquidWalkDangerousOnly() {
        return LIQUID_WALK_SETTINGS.dangerousOnly;
    }

    public static boolean shouldLiquidWalkSneakToDescend() {
        return LIQUID_WALK_SETTINGS.sneakToDescend;
    }

    public static void setLiquidWalkSettings(boolean walkOnWater, boolean dangerousOnly, boolean sneakToDescend) {
        LIQUID_WALK_SETTINGS.walkOnWater = walkOnWater;
        LIQUID_WALK_SETTINGS.dangerousOnly = dangerousOnly;
        LIQUID_WALK_SETTINGS.sneakToDescend = sneakToDescend;
        saveConfig();
    }

    public static boolean shouldApplyVanillaSafeWalk(Entity entity) {
        return entity != null && isEnabled("safe_walk");
    }

    public static boolean shouldAllowMovementDuringGui(Minecraft mc) {
        return GuiMoveFeatureHandler.shouldAllowMovementDuringGui(mc);
    }

    public static void tickClientPlayerFeatures(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || player == null || mc.level == null) {
            INSTANCE.resetRuntimeState();
            GuiMoveFeatureHandler.onClientDisconnect();
            MovementTimerFeatureHandler.reset(INSTANCE, false);
            MovementPersistentAttributeHandler.reset(player);
            return;
        }

        if (!player.isAlive()) {
            INSTANCE.resetRuntimeState();
            GuiMoveFeatureHandler.onClientDisconnect();
            MovementTimerFeatureHandler.reset(INSTANCE, false);
            MovementPersistentAttributeHandler.reset(player);
            return;
        }

        INSTANCE.tickRuntimeState();
        GuiMoveFeatureHandler.apply(mc);
        MovementPersistentAttributeHandler.apply(player);
        MovementTimerFeatureHandler.apply(INSTANCE);
        INSTANCE.applyMovementProtection(player);
        AntiKnockbackFeatureHandler.apply(player);
        NoFallFeatureHandler.apply(player);
        BlockPhaseFeatureHandler.apply(INSTANCE, player);
        ForceSprintFeatureHandler.apply(player);
        AutoObstacleAvoidFeatureHandler.apply(INSTANCE, player);
        LongJumpFeatureHandler.apply(INSTANCE, player);
        BlinkMoveFeatureHandler.apply(INSTANCE, player);
        NoSlowFeatureHandler.apply(player);
        IceBoostFeatureHandler.apply(player);
        ScaffoldFeatureHandler.apply(INSTANCE, player);
        AntiArrowKnockbackFeatureHandler.apply(player);
        AirMotionFeatureHandler.apply(player);
        SafeWalkFeatureHandler.apply(player);
    }

    public static String getFeatureRuntimeSummary(String featureId) {
        return INSTANCE.buildFeatureRuntimeSummary(normalizeId(featureId));
    }

    public void onLivingKnockBack(LivingKnockBackEvent event) {
        AntiKnockbackFeatureHandler.onKnockback(event);
    }

    public static List<String> getStatusLines() {
        return getStatusLines(false);
    }

    public static List<String> getStatusLines(boolean forcePreview) {
        if (!forcePreview && !masterStatusHudEnabled) {
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

        lines.add("§a[移动] §f" + activeNames.size() + " 项开启");
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
        if ("no_collision".equals(state.id)) {
            return "已启用 / 抑制实体推挤";
        }
        if ("timer_accel".equals(state.id)) {
            return "已启用 / " + formatFloat(state.value) + "x";
        }
        return state.supportsValue() ? "已启用 / " + formatFloat(state.value) : "已启用";
    }

    public static void loadConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_movement.json");
            if (!Files.exists(file)) {
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject features = root.has("features") && root.get("features").isJsonObject()
                    ? root.getAsJsonObject("features")
                    : root;
            if (root.has("masterStatusHudEnabled")) {
                masterStatusHudEnabled = root.get("masterStatusHudEnabled").getAsBoolean();
            }
            if (root.has("masterStatusHudX")) {
                masterStatusHudX = Math.max(0, root.get("masterStatusHudX").getAsInt());
            }
            if (root.has("masterStatusHudY")) {
                masterStatusHudY = Math.max(0, root.get("masterStatusHudY").getAsInt());
            }
            if (root.has("liquidWalkSettings") && root.get("liquidWalkSettings").isJsonObject()) {
                JsonObject liquid = root.getAsJsonObject("liquidWalkSettings");
                LIQUID_WALK_SETTINGS.walkOnWater = !liquid.has("walkOnWater")
                        || liquid.get("walkOnWater").getAsBoolean();
                LIQUID_WALK_SETTINGS.dangerousOnly = liquid.has("dangerousOnly")
                        && liquid.get("dangerousOnly").getAsBoolean();
                LIQUID_WALK_SETTINGS.sneakToDescend = liquid.has("sneakToDescend")
                        && liquid.get("sneakToDescend").getAsBoolean();
            }
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
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载移动增强配置失败", e);
        }
    }

    public static void saveConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_movement.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject features = new JsonObject();
            root.addProperty("masterStatusHudEnabled", masterStatusHudEnabled);
            root.addProperty("masterStatusHudX", masterStatusHudX);
            root.addProperty("masterStatusHudY", masterStatusHudY);
            JsonObject liquid = new JsonObject();
            liquid.addProperty("walkOnWater", LIQUID_WALK_SETTINGS.walkOnWater);
            liquid.addProperty("dangerousOnly", LIQUID_WALK_SETTINGS.dangerousOnly);
            liquid.addProperty("sneakToDescend", LIQUID_WALK_SETTINGS.sneakToDescend);
            root.add("liquidWalkSettings", liquid);
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
            zszlScriptMod.LOGGER.error("保存移动增强配置失败", e);
        }
    }

    public void onClientDisconnect() {
        resetRuntimeState();
        GuiMoveFeatureHandler.onClientDisconnect();
        MovementTimerFeatureHandler.reset(this, true);
        MovementPersistentAttributeHandler.reset(Minecraft.getInstance().player);
    }

    private void tickRuntimeState() {
        if (blockPhaseStuckTicks > 0) {
            blockPhaseStuckTicks--;
        }
        if (longJumpCooldownTicks > 0) {
            longJumpCooldownTicks--;
        }
        if (longJumpBoostTicks > 0) {
            longJumpBoostTicks--;
        }
        if (blinkCooldownTicks > 0) {
            blinkCooldownTicks--;
        }
        if (scaffoldPlaceCooldownTicks > 0) {
            scaffoldPlaceCooldownTicks--;
        }
        if (obstacleAvoidTicks > 0) {
            obstacleAvoidTicks--;
        }
    }

    private void resetRuntimeState() {
        blockPhaseStuckTicks = 0;
        longJumpChargeTicks = 0;
        longJumpCooldownTicks = 0;
        longJumpBoostTicks = 0;
        longJumpBoostSpeed = 0.0D;
        wasLongJumpSneakDown = false;
        blinkCooldownTicks = 0;
        wasBlinkTriggerDown = false;
        scaffoldPlaceCooldownTicks = 0;
        obstacleAvoidDirection = 0;
        obstacleAvoidTicks = 0;
        lastProtectionSafeMotionX = 0.0D;
        lastProtectionSafeMotionY = 0.0D;
        lastProtectionSafeMotionZ = 0.0D;
    }

    private void applyMovementProtection(LocalPlayer player) {
        if (player == null) {
            return;
        }

        boolean antiKnockback = isEnabled("anti_knockback");
        if (!isEnabled("no_collision") && !antiKnockback) {
            lastProtectionSafeMotionX = 0.0D;
            lastProtectionSafeMotionY = 0.0D;
            lastProtectionSafeMotionZ = 0.0D;
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        if (antiKnockback && player.hurtTime > 0) {
            double preservedSpeed = Math.sqrt(lastProtectionSafeMotionX * lastProtectionSafeMotionX
                    + lastProtectionSafeMotionZ * lastProtectionSafeMotionZ);
            if (preservedSpeed <= 1.0E-4D) {
                preservedSpeed = Math.max(MovementFeatureSupport.getHorizontalSpeed(player),
                        MovementFeatureSupport.getBaseMoveSpeed() * 0.72D);
            }
            double[] preservedMotion = resolveMovementProtectionMotion(player, preservedSpeed);
            player.setDeltaMovement(preservedMotion[0], Math.min(0.0D, motion.y), preservedMotion[1]);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            return;
        }

        lastProtectionSafeMotionX = motion.x;
        lastProtectionSafeMotionY = motion.y;
        lastProtectionSafeMotionZ = motion.z;
    }

    private double[] resolveMovementProtectionMotion(LocalPlayer player, double speed) {
        Vec3 heading = MovementFeatureSupport.getMovementHeading(player);
        if (heading.lengthSqr() < 1.0E-4D) {
            return new double[] { lastProtectionSafeMotionX, lastProtectionSafeMotionZ };
        }
        return new double[] { heading.x * speed, heading.z * speed };
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

    private static boolean isFeatureBehaviorImplemented(String featureId) {
        String normalized = normalizeId(featureId);
        return "no_slow".equals(normalized) || "force_sprint".equals(normalized) || "anti_knockback".equals(normalized)
                || "gui_move".equals(normalized) || "auto_step".equals(normalized) || "block_phase".equals(normalized)
                || "no_collision".equals(normalized) || "long_jump".equals(normalized)
                || "timer_accel".equals(normalized) || "blink_move".equals(normalized) || "safe_walk".equals(normalized)
                || "scaffold".equals(normalized) || "low_gravity".equals(normalized) || "ice_boost".equals(normalized)
                || "lava_walk".equals(normalized) || "auto_obstacle_avoid".equals(normalized)
                || "hover_mode".equals(normalized) || "fall_cushion".equals(normalized) || "no_fall".equals(normalized)
                || "anti_arrow_knockback".equals(normalized);
    }

    private String getRuntimeHudLine() {
        List<String> parts = new ArrayList<>();
        if (isEnabled("timer_accel") && getFeature("timer_accel").statusHudEnabled) {
            parts.add("§bTimer:" + formatFloat(getConfiguredValue("timer_accel", DEFAULT_TIMER_SPEED)) + "x");
        }
        if (isEnabled("long_jump") && getFeature("long_jump").statusHudEnabled) {
            if (longJumpChargeTicks > 0) {
                parts.add("§e长跳蓄力:" + longJumpChargeTicks);
            } else if (longJumpCooldownTicks > 0) {
                parts.add("§e长跳冷却:" + longJumpCooldownTicks);
            }
        }
        if (isEnabled("blink_move") && getFeature("blink_move").statusHudEnabled && blinkCooldownTicks > 0) {
            parts.add("§d闪步冷却:" + blinkCooldownTicks);
        }
        if (isEnabled("scaffold") && getFeature("scaffold").statusHudEnabled && scaffoldPlaceCooldownTicks > 0) {
            parts.add("§a脚手架");
        }
        if (isEnabled("auto_obstacle_avoid") && getFeature("auto_obstacle_avoid").statusHudEnabled
                && obstacleAvoidTicks > 0) {
            parts.add("§9避障");
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
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return "未找到功能";
        }
        if (!state.enabled) {
            return "未启用";
        }
        if ("no_collision".equals(featureId)) {
            return "已启用 / 减少实体推挤并保留安全运动量";
        }
        if ("timer_accel".equals(featureId)) {
            return "已启用 / " + formatFloat(state.value) + "x";
        }
        if ("safe_walk".equals(featureId)) {
            return "已启用 / 边缘预留 " + formatFloat(state.value);
        }
        if ("lava_walk".equals(featureId)) {
            return "已启用 / 托举 " + formatFloat(state.value) + (LIQUID_WALK_SETTINGS.walkOnWater ? " / 水面" : "")
                    + (LIQUID_WALK_SETTINGS.dangerousOnly ? " / 仅危险液体" : "")
                    + (LIQUID_WALK_SETTINGS.sneakToDescend ? " / 潜行下降" : "");
        }
        return state.supportsValue() ? "已启用 / " + formatFloat(state.value) : "已启用";
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
