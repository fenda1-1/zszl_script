package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FlyHandler {

    public static final FlyHandler INSTANCE = new FlyHandler();

    public static final String MODE_MOTION = "MOTION";
    public static final String MODE_GLIDE = "GLIDE";
    public static final String MODE_PULSE = "PULSE";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean enabled = false;
    public static String flightMode = MODE_MOTION;
    public static boolean autoTakeoff = true;
    public static boolean stopMotionOnDisable = true;
    public static boolean enableNoCollision = true;
    public static boolean enableAntiKnockback = true;
    public static boolean enableAntiKick = false;
    public static float horizontalSpeed = 0.85F;
    public static float verticalSpeed = 0.42F;
    public static float glideFallSpeed = 0.04F;
    public static float sprintMultiplier = 1.25F;
    public static float pulseBoost = 0.28F;
    public static int pulseIntervalTicks = 4;
    public static int antiKickIntervalTicks = 16;
    public static float antiKickDistance = 0.04F;

    private int tickCounter = 0;
    private int pendingTakeoffTicks = 0;

    private static final class ConfigData {
        boolean enabled;
        String flightMode;
        boolean autoTakeoff;
        boolean stopMotionOnDisable;
        boolean enableNoCollision;
        boolean enableAntiKnockback;
        boolean enableAntiKick;
        float horizontalSpeed;
        float verticalSpeed;
        float glideFallSpeed;
        float sprintMultiplier;
        float pulseBoost;
        int pulseIntervalTicks;
        int antiKickIntervalTicks;
        float antiKickDistance;
    }

    static {
        loadConfig();
    }

    private FlyHandler() {
    }

    public static void loadConfig() {
        resetDefaults();
        Path file = ProfileManager.getCurrentProfileDir().resolve("keycommand_fly.json");
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                enabled = data.enabled;
                flightMode = data.flightMode == null ? MODE_MOTION : data.flightMode;
                autoTakeoff = data.autoTakeoff;
                stopMotionOnDisable = data.stopMotionOnDisable;
                enableNoCollision = data.enableNoCollision;
                enableAntiKnockback = data.enableAntiKnockback;
                enableAntiKick = data.enableAntiKick;
                horizontalSpeed = data.horizontalSpeed;
                verticalSpeed = data.verticalSpeed;
                glideFallSpeed = data.glideFallSpeed;
                sprintMultiplier = data.sprintMultiplier;
                pulseBoost = data.pulseBoost;
                pulseIntervalTicks = data.pulseIntervalTicks;
                antiKickIntervalTicks = data.antiKickIntervalTicks;
                antiKickDistance = data.antiKickDistance;
            }
            normalizeConfig();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载飞行配置失败", e);
        }
    }

    public static void saveConfig() {
        normalizeConfig();
        Path file = ProfileManager.getCurrentProfileDir().resolve("keycommand_fly.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.enabled = enabled;
                data.flightMode = flightMode;
                data.autoTakeoff = autoTakeoff;
                data.stopMotionOnDisable = stopMotionOnDisable;
                data.enableNoCollision = enableNoCollision;
                data.enableAntiKnockback = enableAntiKnockback;
                data.enableAntiKick = enableAntiKick;
                data.horizontalSpeed = horizontalSpeed;
                data.verticalSpeed = verticalSpeed;
                data.glideFallSpeed = glideFallSpeed;
                data.sprintMultiplier = sprintMultiplier;
                data.pulseBoost = pulseBoost;
                data.pulseIntervalTicks = pulseIntervalTicks;
                data.antiKickIntervalTicks = antiKickIntervalTicks;
                data.antiKickDistance = antiKickDistance;
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存飞行配置失败", e);
        }
    }

    public void toggleEnabled() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean targetEnabled) {
        Minecraft mc = Minecraft.getInstance();
        normalizeConfig();
        if (enabled == targetEnabled) {
            saveConfig();
            return;
        }

        enabled = targetEnabled;
        tickCounter = 0;
        pendingTakeoffTicks = enabled && autoTakeoff ? 6 : 0;

        if (!enabled) {
            stopFlight(mc.player, stopMotionOnDisable);
        }

        saveConfig();

        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(enabled ? "§a飞行已开启" : "§c飞行已关闭"));
        }
    }

    public void onClientDisconnect() {
        enabled = false;
        tickCounter = 0;
        pendingTakeoffTicks = 0;
        stopFlight(Minecraft.getInstance().player, true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level() == null || !enabled) {
            return;
        }
        if (player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            return;
        }

        normalizeConfig();
        tickCounter++;
        applyFlight(player);
    }

    private void applyFlight(LocalPlayer player) {
        boolean jump = player.input != null && player.input.jumping;
        boolean sneak = player.input != null && player.input.shiftKeyDown;

        double[] horizontalMotion = computeHorizontalMotion(player);
        double motionX = horizontalMotion[0];
        double motionZ = horizontalMotion[1];
        double motionY = computeVerticalMotion(player, jump, sneak);

        if (enableAntiKick && !jump && !sneak && shouldApplyAntiKickPulse()) {
            motionY = Math.min(motionY, -antiKickDistance);
        }

        player.setDeltaMovement(motionX, motionY, motionZ);
        player.fallDistance = 0.0F;
        player.setOnGround(false);
        player.hasImpulse = true;
    }

    private double[] computeHorizontalMotion(LocalPlayer player) {
        float forward = player.input == null ? 0.0F : player.input.forwardImpulse;
        float strafe = player.input == null ? 0.0F : player.input.leftImpulse;
        double length = Math.sqrt(forward * forward + strafe * strafe);

        if (length < 0.01D) {
            return new double[] { 0.0D, 0.0D };
        }

        forward /= (float) length;
        strafe /= (float) length;

        double speed = horizontalSpeed;
        if (player.isSprinting()) {
            speed *= sprintMultiplier;
        }

        double yawRad = Math.toRadians(player.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double motionX = (-sin * forward + cos * strafe) * speed;
        double motionZ = (cos * forward + sin * strafe) * speed;
        return new double[] { motionX, motionZ };
    }

    private double computeVerticalMotion(LocalPlayer player, boolean jump, boolean sneak) {
        if (pendingTakeoffTicks > 0 && autoTakeoff && player.onGround() && !jump && !sneak) {
            pendingTakeoffTicks--;
            return Math.max(0.24D, pulseBoost);
        }

        if (!player.onGround()) {
            pendingTakeoffTicks = 0;
        }

        if (jump) {
            return verticalSpeed;
        }
        if (sneak) {
            return -verticalSpeed;
        }

        if (MODE_GLIDE.equalsIgnoreCase(flightMode)) {
            return -glideFallSpeed;
        }
        if (MODE_PULSE.equalsIgnoreCase(flightMode)) {
            return (tickCounter % pulseIntervalTicks == 0) ? pulseBoost : -glideFallSpeed;
        }
        return 0.0D;
    }

    private boolean shouldApplyAntiKickPulse() {
        return antiKickIntervalTicks > 0 && tickCounter % antiKickIntervalTicks == 0;
    }

    private void stopFlight(LocalPlayer player, boolean stopMotion) {
        if (player == null) {
            return;
        }
        player.fallDistance = 0.0F;
        if (stopMotion) {
            player.setDeltaMovement(0.0D, Math.min(0.0D, player.getDeltaMovement().y), 0.0D);
            player.hasImpulse = true;
        }

        if (!KillAuraHandler.enabled) {
            KillAuraHandler.INSTANCE.applyMovementProtection(player, false, false, false);
        }
    }

    private static void normalizeConfig() {
        if (!MODE_GLIDE.equalsIgnoreCase(flightMode) && !MODE_PULSE.equalsIgnoreCase(flightMode)) {
            flightMode = MODE_MOTION;
        } else if (MODE_GLIDE.equalsIgnoreCase(flightMode)) {
            flightMode = MODE_GLIDE;
        } else {
            flightMode = MODE_PULSE;
        }

        horizontalSpeed = Mth.clamp(horizontalSpeed, 0.05F, 3.00F);
        verticalSpeed = Mth.clamp(verticalSpeed, 0.05F, 1.50F);
        glideFallSpeed = Mth.clamp(glideFallSpeed, 0.00F, 0.50F);
        sprintMultiplier = Mth.clamp(sprintMultiplier, 1.00F, 3.00F);
        pulseBoost = Mth.clamp(pulseBoost, 0.05F, 1.50F);
        pulseIntervalTicks = Mth.clamp(pulseIntervalTicks, 1, 40);
        antiKickIntervalTicks = Mth.clamp(antiKickIntervalTicks, 4, 80);
        antiKickDistance = Mth.clamp(antiKickDistance, 0.01F, 0.20F);
    }

    private static void resetDefaults() {
        enabled = false;
        flightMode = MODE_MOTION;
        autoTakeoff = true;
        stopMotionOnDisable = true;
        enableNoCollision = true;
        enableAntiKnockback = true;
        enableAntiKick = false;
        horizontalSpeed = 0.85F;
        verticalSpeed = 0.42F;
        glideFallSpeed = 0.04F;
        sprintMultiplier = 1.25F;
        pulseBoost = 0.28F;
        pulseIntervalTicks = 4;
        antiKickIntervalTicks = 16;
        antiKickDistance = 0.04F;
    }
}
