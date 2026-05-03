package com.zszl.zszlScriptMod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WorldLoadSafety {
    private static final long MIN_AUTOMATION_DELAY_MS = 3000L;
    private static final long MAX_CHUNK_WAIT_MS = 75000L;

    private static volatile long loginAtMs = 0L;
    private static volatile boolean loginWarmupActive = false;
    private static volatile String lastDeferredReason = "";
    private static volatile Field overlayField = null;
    private static volatile boolean overlayFieldResolved = false;

    private WorldLoadSafety() {
    }

    public static void onNetworkLogin() {
        loginAtMs = System.currentTimeMillis();
        loginWarmupActive = true;
        lastDeferredReason = "network-login";
    }

    public static void onNetworkLogout() {
        loginAtMs = 0L;
        loginWarmupActive = false;
        lastDeferredReason = "";
    }

    public static boolean shouldDeferAutomation(Minecraft mc) {
        if (mc == null) {
            return false;
        }
        if (ScreenSafety.isLoadingOrTransitionScreen(mc.screen)) {
            lastDeferredReason = "loading-screen";
            return true;
        }
        if (hasActiveOverlay(mc)) {
            lastDeferredReason = "loading-overlay";
            return true;
        }
        if (!loginWarmupActive) {
            return false;
        }

        long elapsedMs = System.currentTimeMillis() - loginAtMs;
        if (mc.player == null || mc.level == null) {
            lastDeferredReason = "waiting-player-level";
            return true;
        }
        if (elapsedMs < MIN_AUTOMATION_DELAY_MS) {
            lastDeferredReason = "login-grace";
            return true;
        }
        if (elapsedMs <= MAX_CHUNK_WAIT_MS && !isPlayerAreaRendered(mc)) {
            lastDeferredReason = "waiting-player-chunk";
            return true;
        }

        loginWarmupActive = false;
        lastDeferredReason = "";
        return false;
    }

    public static boolean shouldDeferNetworkCapture(Minecraft mc) {
        return shouldDeferAutomation(mc);
    }

    public static String getLastDeferredReason() {
        return lastDeferredReason;
    }

    private static boolean isPlayerAreaRendered(Minecraft mc) {
        if (mc.player.isSpectator() || !mc.player.isAlive()) {
            return true;
        }
        BlockPos pos = mc.player.blockPosition();
        Boolean rendered = tryLevelRendererChunkCheck(mc, pos);
        return rendered == null || rendered;
    }

    private static Boolean tryLevelRendererChunkCheck(Minecraft mc, BlockPos pos) {
        if (mc.levelRenderer == null || pos == null) {
            return null;
        }
        for (Method method : mc.levelRenderer.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getReturnType() != boolean.class
                    || parameterTypes.length != 1
                    || parameterTypes[0] != BlockPos.class) {
                continue;
            }
            try {
                Object value = method.invoke(mc.levelRenderer, pos);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static boolean hasActiveOverlay(Minecraft mc) {
        Field field = resolveOverlayField(mc);
        if (field == null) {
            return false;
        }
        try {
            return field.get(mc) != null;
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private static Field resolveOverlayField(Minecraft mc) {
        if (overlayFieldResolved) {
            return overlayField;
        }
        overlayFieldResolved = true;
        if (mc == null) {
            return null;
        }
        for (Class<?> type = mc.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (fieldType != null && "net.minecraft.client.gui.screens.Overlay".equals(fieldType.getName())) {
                    try {
                        field.setAccessible(true);
                        overlayField = field;
                        return field;
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
