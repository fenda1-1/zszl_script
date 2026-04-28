package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class SpeedHandler {

    public static final SpeedHandler INSTANCE = new SpeedHandler();

    public static final String MODE_GROUND = "GROUND";
    public static final String MODE_AIR = "AIR";
    public static final String MODE_BHOP = "BHOP";
    public static final String MODE_LOWHOP = "LOWHOP";
    public static final String MODE_ONGROUND = "ONGROUND";
    public static final String BYPASS_SAFE = "SAFE";
    public static final String BYPASS_NORMAL = "NORMAL";
    public static final String BYPASS_AGGRESSIVE = "AGGRESSIVE";

    public static final String PRESET_BALANCED = "BALANCED";
    public static final String PRESET_SAFE = "SAFE";
    public static final String PRESET_AGGRESSIVE = "AGGRESSIVE";
    public static final String PRESET_CUSTOM = "CUSTOM";

    private static final Random RAND = new Random();

    public static boolean enabled = false;
    public static boolean showStatusHud = true;
    public static String speedMode = MODE_GROUND;
    public static String presetId = PRESET_BALANCED;
    public static float vanillaSpeed = 1.05F;
    public static float jumpHeight = 0.42F;
    public static boolean useTimerBoost = true;
    public static float timerSpeed = 1.08F;
    public static String bypassLevel = BYPASS_NORMAL;
    public static boolean randomizeJump = true;

    private static volatile float appliedTimerSpeedMultiplier = 1.0F;

    private boolean slowDown;
    private double playerSpeed = MovementFeatureSupport.getBaseMoveSpeed();
    private int airTicks;
    private int groundTicks;
    private int movementTicks;
    private int nextTimerBreathTick = 8;
    private int timerBurstTicks;
    private boolean lastOnGround = false;
    private double lastHorizontalSpeed = 0.0D;

    private SpeedHandler() {
    }

    public static void loadConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_speed.json");
            if (!Files.exists(file)) {
                normalizeConfig();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            showStatusHud = !root.has("showStatusHud") || root.get("showStatusHud").getAsBoolean();
            speedMode = root.has("speedMode") ? root.get("speedMode").getAsString() : MODE_GROUND;
            presetId = root.has("presetId") ? root.get("presetId").getAsString() : PRESET_BALANCED;
            vanillaSpeed = root.has("vanillaSpeed") ? root.get("vanillaSpeed").getAsFloat() : 1.05F;
            jumpHeight = root.has("jumpHeight") ? root.get("jumpHeight").getAsFloat() : 0.42F;
            useTimerBoost = !root.has("useTimerBoost") || root.get("useTimerBoost").getAsBoolean();
            timerSpeed = root.has("timerSpeed") ? root.get("timerSpeed").getAsFloat() : 1.08F;
            bypassLevel = root.has("bypassLevel") ? root.get("bypassLevel").getAsString() : BYPASS_NORMAL;
            randomizeJump = !root.has("randomizeJump") || root.get("randomizeJump").getAsBoolean();
            normalizeConfig();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载加速配置失败", e);
        }
    }

    public static void saveConfig() {
        normalizeConfig();
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_speed.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("showStatusHud", showStatusHud);
            root.addProperty("speedMode", speedMode);
            root.addProperty("presetId", presetId);
            root.addProperty("vanillaSpeed", vanillaSpeed);
            root.addProperty("jumpHeight", jumpHeight);
            root.addProperty("useTimerBoost", useTimerBoost);
            root.addProperty("timerSpeed", timerSpeed);
            root.addProperty("bypassLevel", bypassLevel);
            root.addProperty("randomizeJump", randomizeJump);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存加速配置失败", e);
        }
    }

    public static void applyPreset(String newPresetId) {
        String normalized = safe(newPresetId).toUpperCase(Locale.ROOT);
        presetId = normalized.isEmpty() ? PRESET_BALANCED : normalized;
        if (PRESET_SAFE.equals(presetId)) {
            speedMode = MODE_GROUND;
            vanillaSpeed = 1.02F;
            jumpHeight = 0.42F;
            useTimerBoost = false;
            timerSpeed = 1.0F;
            bypassLevel = BYPASS_SAFE;
            randomizeJump = true;
        } else if (PRESET_AGGRESSIVE.equals(presetId)) {
            speedMode = MODE_BHOP;
            vanillaSpeed = 1.12F;
            jumpHeight = 0.46F;
            useTimerBoost = true;
            timerSpeed = 1.10F;
            bypassLevel = BYPASS_AGGRESSIVE;
            randomizeJump = true;
        } else if (PRESET_BALANCED.equals(presetId)) {
            speedMode = MODE_GROUND;
            vanillaSpeed = 1.05F;
            jumpHeight = 0.42F;
            useTimerBoost = true;
            timerSpeed = 1.08F;
            bypassLevel = BYPASS_NORMAL;
            randomizeJump = true;
        }
        normalizeConfig();
        saveConfig();
    }

    public static void markCustomPreset() {
        presetId = PRESET_CUSTOM;
        normalizeConfig();
        saveConfig();
    }

    public static String getPresetDisplayName() {
        if (PRESET_SAFE.equalsIgnoreCase(presetId)) {
            return "稳妥";
        }
        if (PRESET_AGGRESSIVE.equalsIgnoreCase(presetId)) {
            return "激进";
        }
        if (PRESET_CUSTOM.equalsIgnoreCase(presetId)) {
            return "自定义";
        }
        return "均衡";
    }

    public static String getModeDisplayName() {
        if (MODE_AIR.equalsIgnoreCase(speedMode)) {
            return "Air";
        }
        if (MODE_BHOP.equalsIgnoreCase(speedMode)) {
            return "Bhop";
        }
        if (MODE_LOWHOP.equalsIgnoreCase(speedMode)) {
            return "LowHop";
        }
        if (MODE_ONGROUND.equalsIgnoreCase(speedMode)) {
            return "OnGround";
        }
        return "Ground";
    }

    public static String getBypassDisplayName() {
        if (BYPASS_SAFE.equalsIgnoreCase(bypassLevel)) {
            return "Safe";
        }
        if (BYPASS_AGGRESSIVE.equalsIgnoreCase(bypassLevel)) {
            return "Aggressive";
        }
        return "Normal";
    }

    public static boolean usesJumpHeight() {
        return MODE_BHOP.equalsIgnoreCase(speedMode) || MODE_LOWHOP.equalsIgnoreCase(speedMode)
                || MODE_AIR.equalsIgnoreCase(speedMode);
    }

    public static boolean isTimerManagedBySpeed() {
        return enabled && useTimerBoost;
    }

    public static float getAppliedTimerSpeedMultiplier() {
        return Math.max(1.0F, appliedTimerSpeedMultiplier);
    }

    public void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            restoreVanillaTimer();
            resetRuntimeState();
            return;
        }

        if (!enabled) {
            restoreVanillaTimer();
            resetRuntimeState();
            return;
        }

        LocalPlayer player = mc.player;
        if (!player.isAlive() || player.getAbilities().flying || player.isInWater() || player.isInLava()
                || player.onClimbable() || player.isPassenger()) {
            restoreVanillaTimer();
            resetRuntimeState();
            return;
        }

        normalizeConfig();
        if (!MovementFeatureSupport.isMoving(player)) {
            restoreVanillaTimer();
            resetRuntimeState();
            return;
        }

        updateGroundAirState(player);
        applyStrafeSpeed(player);
    }

    public void setEnabled(boolean enabled) {
        SpeedHandler.enabled = enabled;
        if (!enabled) {
            restoreVanillaTimer();
            resetRuntimeState();
        }
        saveConfig();
    }

    public void toggleEnabled() {
        setEnabled(!enabled);
    }

    public void onClientDisconnect() {
        restoreVanillaTimer();
        resetRuntimeState();
    }

    public static void applyTimerBoost(float speed) {
        appliedTimerSpeedMultiplier = Mth.clamp(speed, 1.0F, 12.50F);
    }

    public static void restoreVanillaTimer() {
        appliedTimerSpeedMultiplier = 1.0F;
    }

    public static List<String> getStatusLines() {
        return getStatusLines(false);
    }

    public static List<String> getStatusLines(boolean forcePreview) {
        List<String> lines = new ArrayList<>();
        if (enabled && (showStatusHud || forcePreview) && (forcePreview || MovementFeatureManager.isMasterStatusHudEnabled())) {
            lines.add("§a[加速] §f" + getModeDisplayName()
                    + " §7/§f" + getBypassDisplayName()
                    + " §7x" + String.format(Locale.ROOT, "%.2f", vanillaSpeed)
                    + (useTimerBoost ? " §bT" + String.format(Locale.ROOT, "%.2f", timerSpeed) : ""));
        }
        return lines;
    }

    private void applyStrafeSpeed(LocalPlayer player) {
        applyStealthTimer();
        if (!MovementFeatureSupport.isMoving(player)) {
            return;
        }

        double baseSpeed = MovementFeatureSupport.getBaseMoveSpeed();
        double targetSpeed = getConfiguredHorizontalSpeed();
        double maxMultiplier = getBypassMaxMultiplier();
        double speedNoise = RAND.nextGaussian() * 0.008D;
        boolean onGroundNow = player.onGround();
        double currentHorizontalSpeed = Math.max(MovementFeatureSupport.getHorizontalSpeed(player),
                lastHorizontalSpeed * (lastOnGround == onGroundNow ? 0.985D : 0.96D));

        if (onGroundNow) {
            double candidateSpeed = targetSpeed * maxMultiplier * (1.0D + speedNoise);
            if (!lastOnGround && lastHorizontalSpeed > candidateSpeed) {
                candidateSpeed = Math.max(candidateSpeed,
                        lastHorizontalSpeed * (0.94D + RAND.nextDouble() * 0.04D));
            }
            Vec3 predictedMotion = computeStrafeMotion(player, candidateSpeed, 1.0D);
            playerSpeed = simulatePredictedServerSpeed(player, predictedMotion, candidateSpeed, true, baseSpeed);

            double jumpVelocity = 0.0D;
            if (usesJumpHeight()) {
                jumpVelocity = getJumpVelocity();
                if (MODE_LOWHOP.equalsIgnoreCase(speedMode)) {
                    jumpVelocity = clampDouble(jumpVelocity * 0.58D, 0.12D, 0.24D);
                }
            }

            Vec3 appliedMotion = computeStrafeMotion(player, playerSpeed, 1.0D + RAND.nextGaussian() * 0.004D);
            player.setDeltaMovement(appliedMotion.x, usesJumpHeight() ? jumpVelocity : player.getDeltaMovement().y,
                    appliedMotion.z);
            player.hurtMarked = RAND.nextFloat() < 0.70F;
            slowDown = usesJumpHeight();
        } else {
            double decay = 0.91D + RAND.nextGaussian() * 0.015D;
            double carrySpeed = Math.max(currentHorizontalSpeed, playerSpeed);
            if (slowDown) {
                playerSpeed = Math.max(targetSpeed, carrySpeed * (0.82D + RAND.nextFloat() * 0.08D));
                slowDown = false;
            } else {
                playerSpeed = Math.max(targetSpeed, carrySpeed * decay);
            }

            if (MODE_AIR.equalsIgnoreCase(speedMode)) {
                playerSpeed = Math.max(playerSpeed, targetSpeed * (1.08D + RAND.nextDouble() * 0.08D));
            }

            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0.0D) {
                double fallBoost = MODE_LOWHOP.equalsIgnoreCase(speedMode) ? 0.92D : 0.985D;
                player.setDeltaMovement(motion.x, motion.y * fallBoost, motion.z);
            }
            if (MODE_ONGROUND.equalsIgnoreCase(speedMode) && player.getDeltaMovement().y < -0.08D) {
                player.setDeltaMovement(player.getDeltaMovement().x, -0.08D, player.getDeltaMovement().z);
            }

            double airMultiplier = MODE_AIR.equalsIgnoreCase(speedMode) ? 1.35D
                    : (MODE_BHOP.equalsIgnoreCase(speedMode) ? 1.18D : 1.0D);
            Vec3 predictedMotion = computeStrafeMotion(player, playerSpeed, 1.0D);
            playerSpeed = simulatePredictedServerSpeed(player, predictedMotion, playerSpeed, false, baseSpeed);
            playerSpeed = clampDouble(playerSpeed,
                    targetSpeed * 0.95D,
                    Math.max(targetSpeed * 1.05D,
                            baseSpeed * maxMultiplier * 1.35D * airMultiplier
                                    * (1.0D + RAND.nextGaussian() * 0.03D)));
            applyHorizontalMotion(player, playerSpeed);
        }

        lastOnGround = onGroundNow;
        lastHorizontalSpeed = playerSpeed;
    }

    private void applyStealthTimer() {
        if (!useTimerBoost) {
            restoreVanillaTimer();
            return;
        }

        float minTimer = BYPASS_SAFE.equalsIgnoreCase(bypassLevel) ? 1.0F : 1.02F;
        float maxTimer = getStealthTimerCap();
        float variation = (float) (RAND.nextGaussian() * 0.035D);
        float finalTimer = clampFloat(timerSpeed + variation, minTimer, maxTimer);

        if (movementTicks >= nextTimerBreathTick) {
            finalTimer = Math.max(1.0F, finalTimer - (0.03F + RAND.nextFloat() * 0.02F));
            nextTimerBreathTick = movementTicks + 8 + RAND.nextInt(8);
        }

        if (timerBurstTicks > 0) {
            finalTimer = Math.min(maxTimer, finalTimer + 0.015F + RAND.nextFloat() * 0.02F);
            timerBurstTicks--;
        } else if (RAND.nextFloat() < getTimerBurstChance()) {
            timerBurstTicks = 1 + RAND.nextInt(2);
        }

        applyTimerBoost(finalTimer);
    }

    private void updateGroundAirState(LocalPlayer player) {
        if (player.onGround()) {
            groundTicks++;
            airTicks = 0;
        } else {
            airTicks++;
            groundTicks = 0;
        }
        movementTicks++;
    }

    private void resetRuntimeState() {
        slowDown = false;
        playerSpeed = MovementFeatureSupport.getBaseMoveSpeed();
        airTicks = 0;
        groundTicks = 0;
        movementTicks = 0;
        nextTimerBreathTick = 8;
        timerBurstTicks = 0;
        lastOnGround = false;
        lastHorizontalSpeed = 0.0D;
    }

    private static double getConfiguredHorizontalSpeed() {
        return MovementFeatureSupport.getBaseMoveSpeed() * Math.max(0.10F, vanillaSpeed);
    }

    private static void normalizeConfig() {
        speedMode = safe(speedMode).toUpperCase(Locale.ROOT);
        if (!MODE_AIR.equals(speedMode) && !MODE_BHOP.equals(speedMode) && !MODE_LOWHOP.equals(speedMode)
                && !MODE_ONGROUND.equals(speedMode) && !MODE_GROUND.equals(speedMode)) {
            speedMode = MODE_GROUND;
        }
        presetId = safe(presetId).toUpperCase(Locale.ROOT);
        if (!PRESET_SAFE.equals(presetId) && !PRESET_BALANCED.equals(presetId) && !PRESET_AGGRESSIVE.equals(presetId)
                && !PRESET_CUSTOM.equals(presetId)) {
            presetId = PRESET_BALANCED;
        }
        bypassLevel = safe(bypassLevel).toUpperCase(Locale.ROOT);
        if (!BYPASS_SAFE.equals(bypassLevel) && !BYPASS_NORMAL.equals(bypassLevel)
                && !BYPASS_AGGRESSIVE.equals(bypassLevel)) {
            bypassLevel = BYPASS_NORMAL;
        }
        vanillaSpeed = Mth.clamp(vanillaSpeed, 0.10F, 3.00F);
        jumpHeight = Mth.clamp(jumpHeight, 0.00F, 1.00F);
        timerSpeed = Mth.clamp(timerSpeed, 1.00F, 1.20F);
        RAND.setSeed(System.currentTimeMillis() ^ Double.doubleToLongBits(INSTANCE.playerSpeed));
    }

    private double getBypassMaxMultiplier() {
        if (BYPASS_SAFE.equalsIgnoreCase(bypassLevel)) {
            return 1.08D;
        }
        if (BYPASS_AGGRESSIVE.equalsIgnoreCase(bypassLevel)) {
            return 1.28D;
        }
        return 1.18D;
    }

    private float getStealthTimerCap() {
        if (BYPASS_SAFE.equalsIgnoreCase(bypassLevel)) {
            return 1.06F;
        }
        if (BYPASS_AGGRESSIVE.equalsIgnoreCase(bypassLevel)) {
            return 1.14F;
        }
        return 1.12F;
    }

    private float getTimerBurstChance() {
        if (BYPASS_SAFE.equalsIgnoreCase(bypassLevel)) {
            return 0.04F;
        }
        if (BYPASS_AGGRESSIVE.equalsIgnoreCase(bypassLevel)) {
            return 0.14F;
        }
        return 0.08F;
    }

    private double getJumpVelocity() {
        double baseJump = jumpHeight <= 0.0F ? 0.42D : jumpHeight;
        if (!randomizeJump) {
            return baseJump;
        }
        return baseJump * (0.95D + RAND.nextDouble() * 0.10D);
    }

    private double simulatePredictedServerSpeed(LocalPlayer player, Vec3 desiredMotion, double candidateSpeed,
            boolean onGroundNow, double baseSpeed) {
        if (player == null) {
            return candidateSpeed;
        }

        Vec3 heading = desiredMotion == null || desiredMotion.lengthSqr() < 1.0E-6D
                ? MovementFeatureSupport.getMovementHeading(player)
                : desiredMotion;
        double probeDistance = clampDouble(Math.max(0.18D, candidateSpeed * 1.65D), 0.18D, 0.80D);
        boolean obstacleAhead = player.horizontalCollision
                || MovementFeatureSupport.hasObstacleAhead(player, heading, probeDistance);
        boolean canOccupy = MovementFeatureSupport.canOccupy(player,
                player.getX() + desiredMotion.x, player.getY(), player.getZ() + desiredMotion.z)
                || MovementFeatureSupport.canOccupy(player,
                        player.getX() + desiredMotion.x, player.getY() + 0.60D, player.getZ() + desiredMotion.z)
                || MovementFeatureSupport.canOccupy(player,
                        player.getX() + desiredMotion.x, player.getY() - 1.00D, player.getZ() + desiredMotion.z);

        if (obstacleAhead || !canOccupy) {
            if (onGroundNow && MovementFeatureSupport.canStepUp(player, heading)) {
                return Math.max(baseSpeed, candidateSpeed * (0.94D + RAND.nextDouble() * 0.03D));
            }
            return Math.max(baseSpeed * 0.96D, candidateSpeed * (onGroundNow ? 0.72D : 0.88D));
        }

        if (!onGroundNow && MODE_ONGROUND.equalsIgnoreCase(speedMode)
                && !MovementFeatureSupport.hasGroundBelow(player, 2.25D)) {
            return Math.max(baseSpeed, candidateSpeed * 0.90D);
        }

        return candidateSpeed;
    }

    private void applyHorizontalMotion(LocalPlayer player, double horizontalSpeed) {
        Vec3 motion = computeStrafeMotion(player, horizontalSpeed, 1.0D + RAND.nextGaussian() * 0.004D);
        player.setDeltaMovement(motion.x, player.getDeltaMovement().y, motion.z);
        player.hurtMarked = RAND.nextFloat() < 0.85F;
    }

    private Vec3 computeStrafeMotion(LocalPlayer player, double speed, double speedMultiplier) {
        if (player == null || player.input == null) {
            return Vec3.ZERO;
        }
        float forward = player.input.moveVector.y;
        float strafe = player.input.moveVector.x;
        double length = Math.sqrt(forward * forward + strafe * strafe);
        if (length < 1.0E-4D) {
            return Vec3.ZERO;
        }

        forward /= length;
        strafe /= length;

        double yawRadians = Math.toRadians(player.getYRot());
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);
        double appliedSpeed = Math.max(0.0D, speed * speedMultiplier);
        double motionX = (strafe * cos - forward * sin) * appliedSpeed;
        double motionZ = (forward * cos + strafe * sin) * appliedSpeed;
        return new Vec3(motionX, 0.0D, motionZ);
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
