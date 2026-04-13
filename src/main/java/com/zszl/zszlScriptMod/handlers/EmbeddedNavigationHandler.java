package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.config.BaritoneSettingsConfig;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalBlock;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalXZ;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;

/**
 * 导航适配层。
 *
 * 保留统一 API（startGoto / stop / pause / resume / follow），
 * 根据“使用内置 Baritone”设置决定：
 * 1. 直接调用内置 shadowbaritone process/pathing API
 * 2. 继续走命令桥接模式
 */
public class EmbeddedNavigationHandler {
    public static final EmbeddedNavigationHandler INSTANCE = new EmbeddedNavigationHandler();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final long SAME_COMMAND_MIN_INTERVAL_MS = 250L;
    private static final long STOP_COMMAND_MIN_INTERVAL_MS = 350L;
    private static final long GOTO_COMMAND_MIN_INTERVAL_MS = 1000L;

    private long lastAnyCommandAt = 0L;
    private long lastStopCommandAt = 0L;
    private long lastGotoCommandAt = 0L;
    private String lastCommandSent = "";

    private EmbeddedNavigationHandler() {
    }

    public boolean handleInternalCommand(String command) {
        if (command == null) {
            return false;
        }
        String c = command.trim();
        if (c.isEmpty()) {
            return false;
        }

        if (isNavigationCommand(c)) {
            dispatchNavigationCommand(c, false);
            return true;
        }

        return false;
    }

    private boolean isNavigationCommand(String cmd) {
        String lower = normalizeCommand(cmd);
        return lower.equals("!stop")
                || lower.equals("!pause")
                || lower.equals("!resume")
                || lower.equals("!follow entities")
                || lower.startsWith("!goto ")
                || lower.startsWith(".goto ");
    }

    private void dispatchNavigationCommand(String rawCommand, boolean bypassGotoThrottle) {
        if (mc.player == null || mc.player.isSpectator()) {
            return;
        }

        if (shouldThrottle(rawCommand, bypassGotoThrottle)) {
            return;
        }

        boolean directBuiltinMode = BaritoneSettingsConfig.isUseBuiltinBaritone();
        boolean executed = directBuiltinMode
                ? executeBuiltinNavigationCommand(rawCommand)
                : InternalBaritoneBridge.executeRawChatLikeCommand(rawCommand);
        if (!executed) {
            return;
        }

        rememberCommand(rawCommand);
        if (ModConfig.isDebugModeEnabled) {
            String route = directBuiltinMode ? "内置直调" : "命令桥接";
            mc.player.sendMessage(new TextComponentString("§d[DEBUG] §7发送导航命令(" + route + "): §f" + rawCommand));
        }
    }

    private boolean executeBuiltinNavigationCommand(String rawCommand) {
        String normalized = normalizeCommand(rawCommand);

        try {
            if (normalized.equals("!stop")) {
                return executeBuiltinStop();
            }

            if (normalized.equals("!follow entities")) {
                return executeBuiltinFollowEntities();
            }

            if (normalized.startsWith("!goto ") || normalized.startsWith(".goto ")) {
                return executeBuiltinGotoCommand(normalized);
            }

            if (normalized.equals("!pause") || normalized.equals("!resume")) {
                // pause/resume 没有稳定的公开直调 API，这里保留命令桥接兼容
                return InternalBaritoneBridge.executeRawChatLikeCommand(rawCommand);
            }
        } catch (Throwable t) {
            return false;
        }

        return false;
    }

    private boolean executeBuiltinGotoCommand(String normalizedCommand) {
        String[] parts = normalizedCommand.split("\\s+");
        if (parts.length == 0) {
            return false;
        }

        if ("!goto".equals(parts[0])) {
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                return executeBuiltinGoto(x, Double.NaN, z);
            }
            if (parts.length == 4) {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                return executeBuiltinGoto(x, y, z);
            }
            return false;
        }

        if (".goto".equals(parts[0])) {
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                return executeBuiltinGoto(x, Double.NaN, z);
            }
            if (parts.length == 4) {
                double x = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                return executeBuiltinGoto(x, y, z);
            }
            return false;
        }

        return false;
    }

    private boolean executeBuiltinGoto(double x, double y, double z) {
        IBaritone baritone = getPrimaryBaritone();
        Goal goal = Double.isNaN(y)
                ? new GoalXZ(MathHelper.floor(x), MathHelper.floor(z))
                : new GoalBlock(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        baritone.getCustomGoalProcess().setGoalAndPath(goal);
        return true;
    }

    private boolean executeBuiltinFollowEntities() {
        IBaritone baritone = getPrimaryBaritone();
        baritone.getFollowProcess().follow(this::shouldFollowEntity);
        return true;
    }

    private boolean shouldFollowEntity(Entity entity) {
        return entity != null && entity != mc.player && entity.isEntityAlive();
    }

    private boolean executeBuiltinStop() {
        IBaritone baritone = getPrimaryBaritone();
        baritone.getPathingBehavior().cancelEverything();
        return true;
    }

    private IBaritone getPrimaryBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    private boolean shouldThrottle(String cmd, boolean bypassGotoThrottle) {
        long now = System.currentTimeMillis();
        String normalized = normalizeCommand(cmd);

        if (normalized.equals(lastCommandSent) && (now - lastAnyCommandAt) < SAME_COMMAND_MIN_INTERVAL_MS) {
            return true;
        }

        if (normalized.endsWith(" stop") && (now - lastStopCommandAt) < STOP_COMMAND_MIN_INTERVAL_MS) {
            return true;
        }

        if (!bypassGotoThrottle && normalized.contains(" goto ") && (now - lastGotoCommandAt) < GOTO_COMMAND_MIN_INTERVAL_MS) {
            return true;
        }

        return false;
    }

    private void rememberCommand(String cmd) {
        long now = System.currentTimeMillis();
        String normalized = normalizeCommand(cmd);
        lastCommandSent = normalized;
        lastAnyCommandAt = now;

        if (normalized.endsWith(" stop")) {
            lastStopCommandAt = now;
        }
        if (normalized.contains(" goto ")) {
            lastGotoCommandAt = now;
        }
    }

    private String normalizeCommand(String cmd) {
        return cmd == null ? "" : cmd.trim().toLowerCase();
    }

    public void startGoto(double x, double y, double z) {
        startGoto(x, y, z, false);
    }

    public void startGoto(double x, double y, double z, boolean bypassGotoThrottle) {
        if (Double.isNaN(y)) {
            dispatchNavigationCommand(String.format("!goto %.2f %.2f", x, z), bypassGotoThrottle);
        } else {
            dispatchNavigationCommand(String.format("!goto %.2f %.2f %.2f", x, y, z), bypassGotoThrottle);
        }
    }

    public void startGotoXZ(double x, double z) {
        startGotoXZ(x, z, false);
    }

    public void startGotoXZ(double x, double z, boolean bypassGotoThrottle) {
        dispatchNavigationCommand(String.format("!goto %.2f %.2f", x, z), bypassGotoThrottle);
    }

    public void startFollowEntities() {
        dispatchNavigationCommand("!follow entities", false);
    }

    public void stop() {
        dispatchNavigationCommand("!stop", false);
    }

    public void pause() {
        dispatchNavigationCommand("!pause", false);
    }

    public void resume() {
        dispatchNavigationCommand("!resume", false);
    }

    public boolean isPathingOrCalculating() {
        try {
            IBaritone baritone = getPrimaryBaritone();
            if (baritone == null || baritone.getPathingBehavior() == null) {
                return false;
            }
            return baritone.getPathingBehavior().isPathing()
                    || baritone.getPathingBehavior().hasPath()
                    || baritone.getPathingBehavior().getInProgress().isPresent();
        } catch (Throwable t) {
            return false;
        }
    }
}
