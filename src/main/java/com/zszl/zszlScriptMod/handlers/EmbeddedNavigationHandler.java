package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.config.BaritoneSettingsConfig;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.process.PathingCommandType;
import com.zszl.zszlScriptMod.shadowbaritone.api.process.IBaritoneProcess;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalBlock;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalXZ;
import com.zszl.zszlScriptMod.shadowbaritone.utils.GoalTargetNormalizer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

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
    private static final Minecraft mc = Minecraft.getInstance();
    private static final long SAME_COMMAND_MIN_INTERVAL_MS = 250L;
    private static final long STOP_COMMAND_MIN_INTERVAL_MS = 350L;
    private static final long GOTO_COMMAND_MIN_INTERVAL_MS = 1000L;

    private long lastAnyCommandAt = 0L;
    private long lastStopCommandAt = 0L;
    private long lastGotoCommandAt = 0L;
    private String lastCommandSent = "";
    private String lastCommandReason = "";
    private NavigationOwner lastCommandOwner = NavigationOwner.NONE;
    private NavigationOwner lastStopCommandOwner = NavigationOwner.NONE;
    private NavigationOwner lastGotoCommandOwner = NavigationOwner.NONE;
    private NavigationOwner currentOwner = NavigationOwner.NONE;
    private NavigationOwner lastSuccessfulOwner = NavigationOwner.NONE;
    private NavigationSessionType currentSessionType = NavigationSessionType.NONE;

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

        NavigationCommandType commandType = getCommandType(c);
        switch (commandType) {
            case STOP:
                forceStop("聊天命令: " + c);
                return true;
            case FOLLOW_ENTITIES:
                dispatchNavigationCommand(c, false, NavigationOwner.MANUAL_CHAT, "聊天命令: " + c);
                return true;
            case GOTO:
                dispatchNavigationCommand(c, false, NavigationOwner.MANUAL_CHAT, "聊天命令: " + c);
                return true;
            case PAUSE:
            case RESUME:
                dispatchNavigationCommand(c, false, NavigationOwner.NONE, "聊天命令: " + c);
                return true;
            default:
                return false;
        }
    }

    private boolean isNavigationCommand(String cmd) {
        return getCommandType(cmd) != NavigationCommandType.OTHER;
    }

    private void dispatchNavigationCommand(String rawCommand, boolean bypassGotoThrottle, NavigationOwner owner,
            String reason) {
        if (mc.player == null || mc.player.isSpectator()) {
            return;
        }

        NavigationOwner effectiveOwner = owner == null ? NavigationOwner.NONE : owner;
        String normalizedReason = normalizeReason(reason);
        NavigationCommandType commandType = getCommandType(rawCommand);
        boolean builtinRoute = BaritoneSettingsConfig.isUseBuiltinBaritone() && !isBridgeOnlyCommandType(commandType);
        String route = builtinRoute ? "内置直调" : "命令桥接";
        String throttleReason = getThrottleReason(rawCommand, bypassGotoThrottle, effectiveOwner, normalizedReason);
        if (throttleReason != null) {
            logBaritoneDebug(String.format(
                    "[dispatch][%s] route=%s command=%s bypassGotoThrottle=%s throttled=true throttleReason=%s reason=%s",
                    describeOwner(effectiveOwner),
                    route,
                    rawCommand,
                    bypassGotoThrottle,
                    throttleReason,
                    normalizedReason));
            return;
        }

        boolean executed = builtinRoute
                ? executeBuiltinNavigationCommand(rawCommand, bypassGotoThrottle, effectiveOwner, normalizedReason)
                : InternalBaritoneBridge.executeRawChatLikeCommand(rawCommand);
        logBaritoneDebug(String.format(
                "[dispatch][%s] route=%s command=%s bypassGotoThrottle=%s throttled=false executed=%s reason=%s",
                describeOwner(effectiveOwner),
                route,
                rawCommand,
                bypassGotoThrottle,
                executed,
                normalizedReason));
        if (!executed) {
            return;
        }

        rememberCommand(rawCommand, effectiveOwner, normalizedReason);
    }

    private boolean executeBuiltinNavigationCommand(String rawCommand, boolean bypassGotoThrottle,
            NavigationOwner owner, String reason) {
        String normalized = normalizeCommand(rawCommand);

        try {
            if (normalized.equals("!stop")) {
                return executeBuiltinStop(owner, reason);
            }

            if (normalized.equals("!follow entities")) {
                return executeBuiltinFollowEntities(owner, reason);
            }

            if (normalized.startsWith("!goto ") || normalized.startsWith(".goto ")) {
                return executeBuiltinGotoCommand(normalized, bypassGotoThrottle, owner, reason);
            }
        } catch (Throwable t) {
            logBaritoneDebug(String.format("[builtin][%s] command=%s failed: %s",
                    describeOwner(owner),
                    rawCommand,
                    t.toString()));
            return false;
        }

        return false;
    }

    private boolean executeBuiltinGotoCommand(String normalizedCommand, boolean forceRestart, NavigationOwner owner,
            String reason) {
        String[] parts = normalizedCommand.split("\\s+");
        if (parts.length == 0) {
            return false;
        }

        if ("!goto".equals(parts[0])) {
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                return executeBuiltinGoto(x, Double.NaN, z, forceRestart, owner, reason);
            }
            if (parts.length == 4) {
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                return executeBuiltinGoto(x, y, z, forceRestart, owner, reason);
            }
            return false;
        }

        if (".goto".equals(parts[0])) {
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                return executeBuiltinGoto(x, Double.NaN, z, forceRestart, owner, reason);
            }
            if (parts.length == 4) {
                double x = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                return executeBuiltinGoto(x, y, z, forceRestart, owner, reason);
            }
            return false;
        }

        return false;
    }

    private boolean executeBuiltinGoto(double x, double y, double z, boolean forceRestart, NavigationOwner owner,
            String reason) {
        Goal rawGoal = Double.isNaN(y)
                ? new GoalXZ(Mth.floor(x), Mth.floor(z))
                : new GoalBlock(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        return executeBuiltinGoto(rawGoal, forceRestart, owner, reason);
    }

    private boolean executeBuiltinGoto(Goal rawGoal, boolean forceRestart, NavigationOwner owner, String reason) {
        IBaritone baritone = getPrimaryBaritone();
        resumeIfPaused(baritone, owner, reason);
        if (forceRestart && baritone.getPathingBehavior() != null) {
            baritone.getPathingBehavior().cancelEverything();
        }
        Goal goal = GoalTargetNormalizer.normalize(baritone, rawGoal);
        if (goal != rawGoal) {
            logBaritoneDebug(String.format("[goto][%s] normalized goal from %s to %s reason=%s",
                    describeOwner(owner),
                    rawGoal,
                    goal,
                    reason));
        }
        logPathingSnapshot("before-goto", baritone, owner, reason, rawGoal, goal);
        baritone.getCustomGoalProcess().setGoal(goal);
        baritone.getCustomGoalProcess().path();
        logPathingSnapshot("after-goto", baritone, owner, reason, rawGoal, goal);
        return true;
    }

    private boolean executeBuiltinFollowEntities(NavigationOwner owner, String reason) {
        IBaritone baritone = getPrimaryBaritone();
        logPathingSnapshot("before-follow", baritone, owner, reason, null, baritone.getPathingBehavior().getGoal());
        baritone.getFollowProcess().follow(this::shouldFollowEntity);
        logPathingSnapshot("after-follow", baritone, owner, reason, null, baritone.getPathingBehavior().getGoal());
        return true;
    }

    private boolean shouldFollowEntity(Entity entity) {
        return entity != null && entity != mc.player && entity.isAlive();
    }

    private boolean executeBuiltinStop(NavigationOwner owner, String reason) {
        IBaritone baritone = getPrimaryBaritone();
        logPathingSnapshot("before-stop", baritone, owner, reason, null, baritone.getPathingBehavior().getGoal());
        baritone.getPathingBehavior().cancelEverything();
        logPathingSnapshot("after-stop", baritone, owner, reason, null, baritone.getPathingBehavior().getGoal());
        return true;
    }

    private IBaritone getPrimaryBaritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    private String getThrottleReason(String cmd, boolean bypassGotoThrottle, NavigationOwner owner, String reason) {
        long now = System.currentTimeMillis();
        String normalized = normalizeCommand(cmd);
        NavigationCommandType commandType = getCommandType(normalized);

        if (normalized.equals(lastCommandSent)
                && owner == lastCommandOwner
                && reason.equals(lastCommandReason)
                && (now - lastAnyCommandAt) < SAME_COMMAND_MIN_INTERVAL_MS) {
            return "same-command:" + (now - lastAnyCommandAt) + "ms";
        }

        if (commandType == NavigationCommandType.STOP
                && owner == lastStopCommandOwner
                && (now - lastStopCommandAt) < STOP_COMMAND_MIN_INTERVAL_MS) {
            return "stop-throttle:" + (now - lastStopCommandAt) + "ms";
        }

        if (!bypassGotoThrottle && commandType == NavigationCommandType.GOTO
                && owner == lastGotoCommandOwner
                && (now - lastGotoCommandAt) < GOTO_COMMAND_MIN_INTERVAL_MS) {
            return "goto-throttle:" + (now - lastGotoCommandAt) + "ms";
        }

        return null;
    }

    private void rememberCommand(String cmd, NavigationOwner owner, String reason) {
        long now = System.currentTimeMillis();
        String normalized = normalizeCommand(cmd);
        NavigationCommandType commandType = getCommandType(normalized);
        lastCommandSent = normalized;
        lastCommandReason = reason;
        lastAnyCommandAt = now;
        lastCommandOwner = owner;

        if (commandType == NavigationCommandType.STOP) {
            lastStopCommandAt = now;
            lastStopCommandOwner = owner;
            clearNavigationSession();
        }
        if (commandType == NavigationCommandType.GOTO) {
            lastGotoCommandAt = now;
            lastGotoCommandOwner = owner;
            currentOwner = owner;
            lastSuccessfulOwner = owner;
            currentSessionType = NavigationSessionType.GOTO;
        }
        if (commandType == NavigationCommandType.FOLLOW_ENTITIES) {
            currentOwner = owner;
            lastSuccessfulOwner = owner;
            currentSessionType = NavigationSessionType.FOLLOW;
        }
    }

    private String normalizeCommand(String cmd) {
        return cmd == null ? "" : cmd.trim().toLowerCase();
    }

    private String normalizeReason(String reason) {
        String normalized = reason == null ? "" : reason.trim();
        return normalized.isEmpty() ? buildCallerHint() : normalized;
    }

    private String buildCallerHint() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            if (element == null) {
                continue;
            }
            String className = element.getClassName();
            if (className == null
                    || className.equals(Thread.class.getName())
                    || className.equals(EmbeddedNavigationHandler.class.getName())) {
                continue;
            }
            int lastDot = className.lastIndexOf('.');
            String simpleName = lastDot >= 0 ? className.substring(lastDot + 1) : className;
            return simpleName + "#" + element.getMethodName() + ":" + element.getLineNumber();
        }
        return "未标注来源";
    }

    private void clearNavigationSession() {
        currentOwner = NavigationOwner.NONE;
        currentSessionType = NavigationSessionType.NONE;
    }

    private boolean isOwnedBy(NavigationOwner owner) {
        return owner != null
                && owner != NavigationOwner.NONE
                && currentSessionType != NavigationSessionType.NONE
                && currentOwner == owner;
    }

    private boolean isBridgeOnlyCommandType(NavigationCommandType commandType) {
        return commandType == NavigationCommandType.PAUSE || commandType == NavigationCommandType.RESUME;
    }

    private void resumeIfPaused(IBaritone baritone, NavigationOwner owner, String reason) {
        boolean pausedByPauseCommand = baritone.getPathingControlManager().mostRecentCommand()
                .map(command -> command.commandType == PathingCommandType.REQUEST_PAUSE)
                .orElse(false)
                && baritone.getPathingControlManager().mostRecentInControl()
                        .map(process -> "Pause/Resume Commands".equals(process.displayName()))
                        .orElse(false);
        if (!pausedByPauseCommand) {
            return;
        }
        boolean resumed = baritone.getCommandManager().execute("resume");
        logBaritoneDebug(String.format("[goto-resume][%s] pausedByPauseCommand=true resumeIssued=%s reason=%s",
                describeOwner(owner),
                resumed,
                reason));
    }

    private void logPathingSnapshot(String stage, IBaritone baritone, NavigationOwner owner, String reason,
            Goal inputGoal, Goal effectiveGoal) {
        String inControl = baritone.getPathingControlManager().mostRecentInControl()
                .map(proc -> {
                    try {
                        return proc.displayName();
                    } catch (Throwable ignored) {
                        return proc.getClass().getSimpleName();
                    }
                })
                .orElse("<none>");
        String lastCommand = baritone.getPathingControlManager().mostRecentCommand()
                .map(cmd -> cmd.commandType + " " + cmd.goal)
                .orElse("<none>");
        boolean isPathing = baritone.getPathingBehavior().isPathing();
        boolean hasCurrent = baritone.getPathingBehavior().getCurrent() != null;
        boolean hasCalc = baritone.getPathingBehavior().getInProgress().isPresent();
        Goal activeGoal = baritone.getPathingBehavior().getGoal();
        logBaritoneDebug(String.format(
                "[%s][%s] reason=%s inputGoal=%s effectiveGoal=%s activeGoal=%s inControl=%s lastCommand=%s isPathing=%s hasCurrentPath=%s hasCalc=%s",
                stage,
                describeOwner(owner),
                reason,
                inputGoal,
                effectiveGoal,
                activeGoal,
                inControl,
                lastCommand,
                isPathing,
                hasCurrent,
                hasCalc));
    }

    private void logBaritoneDebug(String message) {
        ModConfig.debugLog(DebugModule.BARITONE, "[DBG][EmbeddedNavigationHandler] " + message);
    }

    private String describeOwner(NavigationOwner owner) {
        return owner == null ? NavigationOwner.NONE.getDisplayName() : owner.getDisplayName();
    }

    private NavigationCommandType getCommandType(String cmd) {
        String normalized = normalizeCommand(cmd);
        if (normalized.equals("!stop")) {
            return NavigationCommandType.STOP;
        }
        if (normalized.equals("!pause")) {
            return NavigationCommandType.PAUSE;
        }
        if (normalized.equals("!resume")) {
            return NavigationCommandType.RESUME;
        }
        if (normalized.equals("!follow entities")) {
            return NavigationCommandType.FOLLOW_ENTITIES;
        }
        if (normalized.startsWith("!goto ") || normalized.startsWith(".goto ")) {
            return NavigationCommandType.GOTO;
        }
        return NavigationCommandType.OTHER;
    }

    private boolean isBaritoneNavigationActive() {
        try {
            IBaritone baritone = getPrimaryBaritone();
            if (baritone.getPathingBehavior().hasPath() || baritone.getPathingBehavior().getInProgress().isPresent()) {
                return true;
            }
            return baritone.getPathingControlManager().mostRecentInControl()
                    .map(IBaritoneProcess::isActive)
                    .orElse(false);
        } catch (Throwable t) {
            // Fail open here so we don't accidentally swallow a needed stop command.
            return true;
        }
    }

    public void startGoto(NavigationOwner owner, double x, double y, double z) {
        startGoto(owner, x, y, z, false, null);
    }

    public void startGoto(NavigationOwner owner, double x, double y, double z, boolean bypassGotoThrottle) {
        startGoto(owner, x, y, z, bypassGotoThrottle, null);
    }

    public void startGoto(NavigationOwner owner, double x, double y, double z, boolean bypassGotoThrottle,
            String reason) {
        if (Double.isNaN(y)) {
            dispatchNavigationCommand(String.format("!goto %.2f %.2f", x, z), bypassGotoThrottle, owner, reason);
        } else {
            dispatchNavigationCommand(String.format("!goto %.2f %.2f %.2f", x, y, z), bypassGotoThrottle, owner,
                    reason);
        }
    }

    public void startGoto(double x, double y, double z) {
        startGoto(NavigationOwner.MANUAL_UI, x, y, z, false);
    }

    public void startGoto(double x, double y, double z, boolean bypassGotoThrottle) {
        startGoto(NavigationOwner.MANUAL_UI, x, y, z, bypassGotoThrottle);
    }

    public void startGotoXZ(NavigationOwner owner, double x, double z) {
        startGotoXZ(owner, x, z, false, null);
    }

    public void startGotoXZ(NavigationOwner owner, double x, double z, boolean bypassGotoThrottle) {
        startGotoXZ(owner, x, z, bypassGotoThrottle, null);
    }

    public void startGotoXZ(NavigationOwner owner, double x, double z, boolean bypassGotoThrottle, String reason) {
        dispatchNavigationCommand(String.format("!goto %.2f %.2f", x, z), bypassGotoThrottle, owner, reason);
    }

    public void startGotoXZ(double x, double z) {
        startGotoXZ(NavigationOwner.MANUAL_UI, x, z, false);
    }

    public void startGotoXZ(double x, double z, boolean bypassGotoThrottle) {
        startGotoXZ(NavigationOwner.MANUAL_UI, x, z, bypassGotoThrottle);
    }

    public void startFollowEntities(NavigationOwner owner) {
        startFollowEntities(owner, null);
    }

    public void startFollowEntities(NavigationOwner owner, String reason) {
        dispatchNavigationCommand("!follow entities", false, owner, reason);
    }

    public void startFollowEntities() {
        startFollowEntities(NavigationOwner.MANUAL_UI);
    }

    public void stopOwned(NavigationOwner owner) {
        stopOwned(owner, null);
    }

    public void stopOwned(NavigationOwner owner, String reason) {
        String normalizedReason = normalizeReason(reason);
        if (!isOwnedBy(owner)) {
            logBaritoneDebug(String.format("[stopOwned] ignored owner=%s currentOwner=%s session=%s reason=%s",
                    describeOwner(owner),
                    describeOwner(currentOwner),
                    currentSessionType,
                    normalizedReason));
            return;
        }
        if (!isBaritoneNavigationActive()) {
            logBaritoneDebug(String.format("[stopOwned] no active navigation owner=%s reason=%s",
                    describeOwner(owner),
                    normalizedReason));
            clearNavigationSession();
            return;
        }
        dispatchNavigationCommand("!stop", false, owner, normalizedReason);
    }

    public void forceStop() {
        forceStop(null);
    }

    public void forceStop(String reason) {
        String normalizedReason = normalizeReason(reason);
        if (!isBaritoneNavigationActive()) {
            logBaritoneDebug(String.format("[forceStop] no active navigation currentOwner=%s session=%s reason=%s",
                    describeOwner(currentOwner),
                    currentSessionType,
                    normalizedReason));
            clearNavigationSession();
            return;
        }
        dispatchNavigationCommand("!stop", false, NavigationOwner.NONE, normalizedReason);
    }

    public void stop() {
        forceStop();
    }

    public void pause() {
        pause(null);
    }

    public void pause(String reason) {
        dispatchNavigationCommand("!pause", false, NavigationOwner.NONE, reason);
    }

    public void resume() {
        resume(null);
    }

    public void resume(String reason) {
        dispatchNavigationCommand("!resume", false, NavigationOwner.NONE, reason);
    }

    public enum NavigationOwner {
        NONE(false, "全局"),
        MANUAL_CHAT(true, "手动聊天"),
        MANUAL_UI(true, "手动界面"),
        AUTO_FOLLOW(false, "自动追怪"),
        AUTO_PICKUP(false, "自动拾取"),
        KILL_AURA_HUNT(false, "杀戮光环追击"),
        KILL_AURA_PICKUP(false, "杀戮光环拾取"),
        GO_TO_AND_OPEN(false, "前往开箱"),
        PATH_SEQUENCE(false, "路径序列");

        private final boolean manual;
        private final String displayName;

        NavigationOwner(boolean manual, String displayName) {
            this.manual = manual;
            this.displayName = displayName;
        }

        public boolean isManual() {
            return manual;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private enum NavigationSessionType {
        NONE,
        GOTO,
        FOLLOW
    }

    private enum NavigationCommandType {
        STOP,
        PAUSE,
        RESUME,
        FOLLOW_ENTITIES,
        GOTO,
        OTHER
    }
}
