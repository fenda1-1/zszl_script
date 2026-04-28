package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFishingHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final AutoFishingHandler INSTANCE = new AutoFishingHandler();

    public static final String BITE_MODE_SMART = "SMART";
    public static final String BITE_MODE_MOTION_ONLY = "MOTION_ONLY";
    public static final String BITE_MODE_STRICT = "STRICT";

    public static boolean enabled = false;

    public static boolean requireFishingRod = true;
    public static boolean autoSwitchToRod = false;
    public static int preferredRodSlot = 0;
    public static boolean disableWhenGuiOpen = true;
    public static boolean allowWhilePlayerMoving = false;
    public static boolean sendStatusMessage = true;

    public static boolean enableAutoCastOnStart = true;
    public static int initialCastDelayTicks = 8;
    public static boolean autoRecastAfterCatch = true;
    public static int recastDelayMinTicks = 10;
    public static int recastDelayMaxTicks = 16;
    public static boolean retryCastWhenBobberMissing = true;
    public static int retryCastDelayTicks = 20;
    public static boolean timeoutRecastEnabled = true;
    public static int maxFishingWaitTicks = 600;

    public static String biteDetectMode = BITE_MODE_SMART;
    public static int ignoreInitialBobberSettleTicks = 8;
    public static int reelDelayTicks = 2;
    public static float minVerticalDropThreshold = 0.08F;
    public static float minHorizontalMoveThreshold = 0.03F;
    public static int confirmBiteTicks = 1;
    public static boolean debugBiteInfo = false;

    public static int postReelPauseTicks = 6;
    public static int preventDoubleReelTicks = 6;
    public static boolean recastOnlyIfLootSuccess = false;
    public static boolean resetStateWhenHookGone = true;
    public static boolean autoRecoverFromInterruptedCast = true;

    public static boolean stopWhenRodDurabilityLow = true;
    public static int minRodDurability = 5;
    public static boolean stopWhenNoRodFound = true;
    public static boolean pauseWhenHookedEntity = true;
    public static boolean stopOnWorldChange = true;

    private enum FishingState {
        IDLE,
        WAITING_CAST,
        WAITING_BOBBER,
        FISHING,
        WAITING_REEL,
        POST_REEL_DELAY
    }

    private FishingState state = FishingState.IDLE;
    private int actionDelayTicks = 0;
    private int reelCooldownTicks = 0;
    private int fishingTicks = 0;
    private int biteConfirmCounter = 0;
    private int bobberWaterStableTicks = 0;
    private boolean lastReelLikelyCatch = false;
    private boolean lastBobberInWater = false;
    private double lastBobberX = 0.0D;
    private double lastBobberY = 0.0D;
    private double lastBobberZ = 0.0D;

    private static class ConfigData {
        boolean enabled;
        boolean requireFishingRod;
        boolean autoSwitchToRod;
        int preferredRodSlot;
        boolean disableWhenGuiOpen;
        boolean allowWhilePlayerMoving;
        boolean sendStatusMessage;
        boolean enableAutoCastOnStart;
        int initialCastDelayTicks;
        boolean autoRecastAfterCatch;
        int recastDelayMinTicks;
        int recastDelayMaxTicks;
        boolean retryCastWhenBobberMissing;
        int retryCastDelayTicks;
        boolean timeoutRecastEnabled;
        int maxFishingWaitTicks;
        String biteDetectMode;
        int ignoreInitialBobberSettleTicks;
        int reelDelayTicks;
        float minVerticalDropThreshold;
        float minHorizontalMoveThreshold;
        int confirmBiteTicks;
        boolean debugBiteInfo;
        int postReelPauseTicks;
        int preventDoubleReelTicks;
        boolean recastOnlyIfLootSuccess;
        boolean resetStateWhenHookGone;
        boolean autoRecoverFromInterruptedCast;
        boolean stopWhenRodDurabilityLow;
        int minRodDurability;
        boolean stopWhenNoRodFound;
        boolean pauseWhenHookedEntity;
        boolean stopOnWorldChange;
    }

    static {
        loadConfig();
    }

    private AutoFishingHandler() {
    }

    public static void register() {
        TickEvent.ClientTickEvent.BUS.addListener(INSTANCE::onClientTick);
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_auto_fishing.json");
    }

    public static synchronized void loadConfig() {
        applyDefaultSettings();

        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            normalizeConfig();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                enabled = data.enabled;
                requireFishingRod = data.requireFishingRod;
                autoSwitchToRod = data.autoSwitchToRod;
                preferredRodSlot = data.preferredRodSlot;
                disableWhenGuiOpen = data.disableWhenGuiOpen;
                allowWhilePlayerMoving = data.allowWhilePlayerMoving;
                sendStatusMessage = data.sendStatusMessage;

                enableAutoCastOnStart = data.enableAutoCastOnStart;
                initialCastDelayTicks = data.initialCastDelayTicks;
                autoRecastAfterCatch = data.autoRecastAfterCatch;
                recastDelayMinTicks = data.recastDelayMinTicks;
                recastDelayMaxTicks = data.recastDelayMaxTicks;
                retryCastWhenBobberMissing = data.retryCastWhenBobberMissing;
                retryCastDelayTicks = data.retryCastDelayTicks;
                timeoutRecastEnabled = data.timeoutRecastEnabled;
                maxFishingWaitTicks = data.maxFishingWaitTicks;

                biteDetectMode = data.biteDetectMode;
                ignoreInitialBobberSettleTicks = data.ignoreInitialBobberSettleTicks;
                reelDelayTicks = data.reelDelayTicks;
                minVerticalDropThreshold = data.minVerticalDropThreshold;
                minHorizontalMoveThreshold = data.minHorizontalMoveThreshold;
                confirmBiteTicks = data.confirmBiteTicks;
                debugBiteInfo = data.debugBiteInfo;

                postReelPauseTicks = data.postReelPauseTicks;
                preventDoubleReelTicks = data.preventDoubleReelTicks;
                recastOnlyIfLootSuccess = data.recastOnlyIfLootSuccess;
                resetStateWhenHookGone = data.resetStateWhenHookGone;
                autoRecoverFromInterruptedCast = data.autoRecoverFromInterruptedCast;

                stopWhenRodDurabilityLow = data.stopWhenRodDurabilityLow;
                minRodDurability = data.minRodDurability;
                stopWhenNoRodFound = data.stopWhenNoRodFound;
                pauseWhenHookedEntity = data.pauseWhenHookedEntity;
                stopOnWorldChange = data.stopOnWorldChange;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载自动钓鱼配置失败", e);
        }

        normalizeConfig();
    }

    public static synchronized void saveConfig() {
        normalizeConfig();

        ConfigData data = new ConfigData();
        data.enabled = enabled;
        data.requireFishingRod = requireFishingRod;
        data.autoSwitchToRod = autoSwitchToRod;
        data.preferredRodSlot = preferredRodSlot;
        data.disableWhenGuiOpen = disableWhenGuiOpen;
        data.allowWhilePlayerMoving = allowWhilePlayerMoving;
        data.sendStatusMessage = sendStatusMessage;

        data.enableAutoCastOnStart = enableAutoCastOnStart;
        data.initialCastDelayTicks = initialCastDelayTicks;
        data.autoRecastAfterCatch = autoRecastAfterCatch;
        data.recastDelayMinTicks = recastDelayMinTicks;
        data.recastDelayMaxTicks = recastDelayMaxTicks;
        data.retryCastWhenBobberMissing = retryCastWhenBobberMissing;
        data.retryCastDelayTicks = retryCastDelayTicks;
        data.timeoutRecastEnabled = timeoutRecastEnabled;
        data.maxFishingWaitTicks = maxFishingWaitTicks;

        data.biteDetectMode = biteDetectMode;
        data.ignoreInitialBobberSettleTicks = ignoreInitialBobberSettleTicks;
        data.reelDelayTicks = reelDelayTicks;
        data.minVerticalDropThreshold = minVerticalDropThreshold;
        data.minHorizontalMoveThreshold = minHorizontalMoveThreshold;
        data.confirmBiteTicks = confirmBiteTicks;
        data.debugBiteInfo = debugBiteInfo;

        data.postReelPauseTicks = postReelPauseTicks;
        data.preventDoubleReelTicks = preventDoubleReelTicks;
        data.recastOnlyIfLootSuccess = recastOnlyIfLootSuccess;
        data.resetStateWhenHookGone = resetStateWhenHookGone;
        data.autoRecoverFromInterruptedCast = autoRecoverFromInterruptedCast;

        data.stopWhenRodDurabilityLow = stopWhenRodDurabilityLow;
        data.minRodDurability = minRodDurability;
        data.stopWhenNoRodFound = stopWhenNoRodFound;
        data.pauseWhenHookedEntity = pauseWhenHookedEntity;
        data.stopOnWorldChange = stopOnWorldChange;

        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动钓鱼配置失败", e);
        }
    }

    public static synchronized void resetToDefaults() {
        applyDefaultSettings();
        normalizeConfig();
    }

    private static void applyDefaultSettings() {
        enabled = false;

        requireFishingRod = true;
        autoSwitchToRod = false;
        preferredRodSlot = 0;
        disableWhenGuiOpen = true;
        allowWhilePlayerMoving = false;
        sendStatusMessage = true;

        enableAutoCastOnStart = true;
        initialCastDelayTicks = 8;
        autoRecastAfterCatch = true;
        recastDelayMinTicks = 10;
        recastDelayMaxTicks = 16;
        retryCastWhenBobberMissing = true;
        retryCastDelayTicks = 20;
        timeoutRecastEnabled = true;
        maxFishingWaitTicks = 600;

        biteDetectMode = BITE_MODE_SMART;
        ignoreInitialBobberSettleTicks = 8;
        reelDelayTicks = 2;
        minVerticalDropThreshold = 0.08F;
        minHorizontalMoveThreshold = 0.03F;
        confirmBiteTicks = 1;
        debugBiteInfo = false;

        postReelPauseTicks = 6;
        preventDoubleReelTicks = 6;
        recastOnlyIfLootSuccess = false;
        resetStateWhenHookGone = true;
        autoRecoverFromInterruptedCast = true;

        stopWhenRodDurabilityLow = true;
        minRodDurability = 5;
        stopWhenNoRodFound = true;
        pauseWhenHookedEntity = true;
        stopOnWorldChange = true;
    }

    private static void normalizeConfig() {
        preferredRodSlot = clampInt(preferredRodSlot, 0, 9);

        initialCastDelayTicks = clampInt(initialCastDelayTicks, 0, 100);
        recastDelayMinTicks = clampInt(recastDelayMinTicks, 0, 100);
        recastDelayMaxTicks = clampInt(recastDelayMaxTicks, recastDelayMinTicks, 100);
        retryCastDelayTicks = clampInt(retryCastDelayTicks, 5, 100);
        maxFishingWaitTicks = clampInt(maxFishingWaitTicks, 40, 2400);

        ignoreInitialBobberSettleTicks = clampInt(ignoreInitialBobberSettleTicks, 0, 40);
        reelDelayTicks = clampInt(reelDelayTicks, 0, 20);
        minVerticalDropThreshold = clampFloat(minVerticalDropThreshold, 0.01F, 1.0F);
        minHorizontalMoveThreshold = clampFloat(minHorizontalMoveThreshold, 0.0F, 1.0F);
        confirmBiteTicks = clampInt(confirmBiteTicks, 1, 5);

        postReelPauseTicks = clampInt(postReelPauseTicks, 0, 40);
        preventDoubleReelTicks = clampInt(preventDoubleReelTicks, 0, 20);
        minRodDurability = clampInt(minRodDurability, 1, 64);

        if (!BITE_MODE_MOTION_ONLY.equalsIgnoreCase(biteDetectMode)
                && !BITE_MODE_STRICT.equalsIgnoreCase(biteDetectMode)) {
            biteDetectMode = BITE_MODE_SMART;
        } else if (BITE_MODE_MOTION_ONLY.equalsIgnoreCase(biteDetectMode)) {
            biteDetectMode = BITE_MODE_MOTION_ONLY;
        } else {
            biteDetectMode = BITE_MODE_STRICT;
        }
    }

    public synchronized void toggleEnabled() {
        setEnabled(!enabled);
    }

    public synchronized void setEnabled(boolean targetEnabled) {
        normalizeConfig();
        if (enabled == targetEnabled) {
            saveConfig();
            return;
        }

        enabled = targetEnabled;
        resetRuntimeState();
        if (enabled && enableAutoCastOnStart) {
            this.state = FishingState.WAITING_CAST;
            this.actionDelayTicks = initialCastDelayTicks;
        }
        saveConfig();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && sendStatusMessage) {
            mc.player.displayClientMessage(new TextComponentString(
                    I18n.format(enabled ? "msg.auto_fishing.enabled" : "msg.auto_fishing.disabled")), false);
        }
    }

    public synchronized void onClientDisconnect() {
        if (stopOnWorldChange && enabled) {
            enabled = false;
            saveConfig();
        }
        resetRuntimeState();
    }

    public synchronized void resetRuntimeState() {
        this.state = FishingState.IDLE;
        this.actionDelayTicks = 0;
        this.reelCooldownTicks = 0;
        this.fishingTicks = 0;
        this.biteConfirmCounter = 0;
        this.bobberWaterStableTicks = 0;
        this.lastReelLikelyCatch = false;
        this.lastBobberInWater = false;
        this.lastBobberX = 0.0D;
        this.lastBobberY = 0.0D;
        this.lastBobberZ = 0.0D;
    }

    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        if (this.actionDelayTicks > 0) {
            this.actionDelayTicks--;
        }
        if (this.reelCooldownTicks > 0) {
            this.reelCooldownTicks--;
        }

        if (!enabled) {
            if (this.state != FishingState.IDLE) {
                resetRuntimeState();
            }
            return;
        }

        if (player.isDeadOrDying() || player.isSpectator()) {
            return;
        }

        if (shouldPauseForCurrentScreen(mc)) {
            return;
        }

        if (!allowWhilePlayerMoving && isPlayerActivelyMoving(player)) {
            return;
        }

        if (!ensureRodReady(player)) {
            return;
        }

        FishingHook bobber = player.fishing;
        if (bobber != null && this.state != FishingState.WAITING_REEL && this.state != FishingState.POST_REEL_DELAY) {
            if (this.state != FishingState.FISHING) {
                beginFishing(bobber);
            }
        }

        switch (this.state) {
            case IDLE:
                if (bobber != null) {
                    beginFishing(bobber);
                }
                break;
            case WAITING_CAST:
                if (bobber != null) {
                    beginFishing(bobber);
                    return;
                }
                if (this.actionDelayTicks <= 0 && performRodUse(player)) {
                    this.state = FishingState.WAITING_BOBBER;
                    this.actionDelayTicks = retryCastDelayTicks;
                }
                break;
            case WAITING_BOBBER:
                if (bobber != null) {
                    beginFishing(bobber);
                    return;
                }
                if (retryCastWhenBobberMissing && this.actionDelayTicks <= 0) {
                    if (performRodUse(player)) {
                        this.actionDelayTicks = retryCastDelayTicks;
                    }
                } else if (!retryCastWhenBobberMissing && this.actionDelayTicks <= 0) {
                    this.state = FishingState.IDLE;
                }
                break;
            case FISHING:
                if (bobber == null) {
                    handleUnexpectedHookLoss();
                    return;
                }
                if (pauseWhenHookedEntity && bobber.getHookedIn() != null) {
                    return;
                }
                this.fishingTicks++;
                if (shouldTriggerBite(bobber)) {
                    this.biteConfirmCounter++;
                    if (this.biteConfirmCounter >= confirmBiteTicks && this.reelCooldownTicks <= 0) {
                        this.state = FishingState.WAITING_REEL;
                        this.actionDelayTicks = reelDelayTicks;
                        this.lastReelLikelyCatch = true;
                        if (debugBiteInfo && player != null) {
                            player.displayClientMessage(new TextComponentString(I18n.format("msg.auto_fishing.debug_bite")), false);
                        }
                    }
                } else {
                    this.biteConfirmCounter = 0;
                }

                if (timeoutRecastEnabled && this.fishingTicks >= maxFishingWaitTicks) {
                    if (performRodUse(player)) {
                        this.state = FishingState.POST_REEL_DELAY;
                        this.actionDelayTicks = postReelPauseTicks;
                        this.reelCooldownTicks = preventDoubleReelTicks;
                        this.lastReelLikelyCatch = false;
                    }
                }
                break;
            case WAITING_REEL:
                if (bobber == null) {
                    this.state = FishingState.POST_REEL_DELAY;
                    this.actionDelayTicks = Math.max(this.actionDelayTicks, postReelPauseTicks);
                    return;
                }
                if (this.actionDelayTicks <= 0 && this.reelCooldownTicks <= 0) {
                    if (performRodUse(player)) {
                        this.state = FishingState.POST_REEL_DELAY;
                        this.actionDelayTicks = postReelPauseTicks;
                        this.reelCooldownTicks = preventDoubleReelTicks;
                    }
                }
                break;
            case POST_REEL_DELAY:
                if (bobber != null) {
                    if (autoRecoverFromInterruptedCast && this.actionDelayTicks <= 0) {
                        beginFishing(bobber);
                    }
                    return;
                }
                if (this.actionDelayTicks <= 0) {
                    if (autoRecastAfterCatch && (!recastOnlyIfLootSuccess || this.lastReelLikelyCatch)) {
                        scheduleRandomRecast();
                    } else {
                        this.state = FishingState.IDLE;
                    }
                }
                break;
            default:
                break;
        }
    }

    private void beginFishing(FishingHook bobber) {
        this.state = FishingState.FISHING;
        this.fishingTicks = 0;
        this.biteConfirmCounter = 0;
        this.lastBobberInWater = bobber != null && bobber.isInWater();
        this.bobberWaterStableTicks = this.lastBobberInWater ? 1 : 0;
        syncBobberPosition(bobber);
    }

    private void handleUnexpectedHookLoss() {
        if (!resetStateWhenHookGone) {
            this.state = FishingState.IDLE;
            return;
        }
        if (autoRecoverFromInterruptedCast && autoRecastAfterCatch) {
            scheduleRandomRecast();
        } else {
            this.state = FishingState.IDLE;
        }
    }

    private void scheduleRandomRecast() {
        this.state = FishingState.WAITING_CAST;
        this.actionDelayTicks = ThreadLocalRandom.current().nextInt(recastDelayMinTicks, recastDelayMaxTicks + 1);
        this.biteConfirmCounter = 0;
        this.fishingTicks = 0;
    }

    private boolean shouldTriggerBite(FishingHook bobber) {
        if (bobber == null) {
            return false;
        }
        double horizontalMove = Math.sqrt(
                Math.pow(bobber.getX() - this.lastBobberX, 2)
                        + Math.pow(bobber.getZ() - this.lastBobberZ, 2));
        double verticalDrop = this.lastBobberY - bobber.getY();
        boolean downward = verticalDrop >= minVerticalDropThreshold
                || bobber.getDeltaMovement().y <= -minVerticalDropThreshold;
        boolean horizontal = horizontalMove >= minHorizontalMoveThreshold;
        boolean inWater = bobber.isInWater();

        if (inWater) {
            this.bobberWaterStableTicks = this.lastBobberInWater ? this.bobberWaterStableTicks + 1 : 1;
        } else {
            this.bobberWaterStableTicks = 0;
        }
        this.lastBobberInWater = inWater;

        int requiredWaterStableTicks = Math.max(8, ignoreInitialBobberSettleTicks);
        if (!inWater || this.bobberWaterStableTicks <= requiredWaterStableTicks) {
            syncBobberPosition(bobber);
            return false;
        }

        boolean result;
        if (BITE_MODE_MOTION_ONLY.equalsIgnoreCase(biteDetectMode)) {
            result = downward || horizontal;
        } else if (BITE_MODE_STRICT.equalsIgnoreCase(biteDetectMode)) {
            result = downward && horizontal;
        } else {
            result = downward || (horizontal && bobber.getDeltaMovement().y < -0.02D);
        }

        syncBobberPosition(bobber);
        return result;
    }

    private void syncBobberPosition(FishingHook bobber) {
        if (bobber == null) {
            return;
        }
        this.lastBobberX = bobber.getX();
        this.lastBobberY = bobber.getY();
        this.lastBobberZ = bobber.getZ();
    }

    private boolean ensureRodReady(LocalPlayer player) {
        if (player == null) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (isFishingRod(held)) {
            return validateRodDurability(held);
        }

        boolean shouldTrySwitch = autoSwitchToRod || !requireFishingRod;
        if (!shouldTrySwitch) {
            return false;
        }

        int rodSlot = findRodHotbarSlot(player);
        if (rodSlot < 0) {
            if (stopWhenNoRodFound) {
                disableWithMessage("msg.auto_fishing.stop_no_rod");
            }
            return false;
        }

        player.getInventory().setSelectedSlot(rodSlot);
        ItemStack switched = player.getMainHandItem();
        return isFishingRod(switched) && validateRodDurability(switched);
    }

    private boolean validateRodDurability(ItemStack stack) {
        if (!isFishingRod(stack)) {
            return false;
        }
        if (!stopWhenRodDurabilityLow || !stack.isDamageableItem()) {
            return true;
        }

        int remain = stack.getMaxDamage() - stack.getDamageValue();
        if (remain <= minRodDurability) {
            disableWithMessage("msg.auto_fishing.stop_low_durability");
            return false;
        }
        return true;
    }

    private int findRodHotbarSlot(LocalPlayer player) {
        if (player == null) {
            return -1;
        }

        int preferredIndex = preferredRodSlot - 1;
        if (preferredIndex >= 0 && preferredIndex < 9) {
            ItemStack preferred = player.getInventory().getItem(preferredIndex);
            if (isFishingRod(preferred)) {
                return preferredIndex;
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isFishingRod(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFishingRod(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof FishingRodItem;
    }

    private boolean performRodUse(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.gameMode == null || mc.level == null) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!isFishingRod(held)) {
            return false;
        }
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private boolean shouldPauseForCurrentScreen(Minecraft mc) {
        if (!disableWhenGuiOpen || mc == null || mc.screen == null) {
            return false;
        }
        String screenClassName = mc.screen.getClass().getName();
        return screenClassName.startsWith("com.zszl.zszlScriptMod.gui");
    }

    private boolean isPlayerActivelyMoving(LocalPlayer player) {
        return player != null
                && (Math.abs(player.input.moveVector.x) > 0.01F
                || Math.abs(player.input.moveVector.y) > 0.01F
                || player.input.keyPresses.jump()
                || player.input.keyPresses.shift());
    }

    private void disableWithMessage(String key) {
        Minecraft mc = Minecraft.getInstance();
        enabled = false;
        saveConfig();
        resetRuntimeState();
        if (mc.player != null && sendStatusMessage) {
            mc.player.displayClientMessage(new TextComponentString(I18n.format(key)), false);
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

