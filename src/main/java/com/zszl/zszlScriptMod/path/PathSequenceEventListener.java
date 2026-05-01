package com.zszl.zszlScriptMod.path;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher.Side;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.EmbeddedNavigationHandler;
import com.zszl.zszlScriptMod.handlers.HuntOrbitController;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.handlers.ItemSpreadHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager;
import com.zszl.zszlScriptMod.path.runtime.locks.ResourceLockManager;
import com.zszl.zszlScriptMod.path.runtime.safety.PathSafetyManager;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.utils.HudTextScanner;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PacketFieldRuleManager;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiElementInspector;
import com.zszl.zszlScriptMod.utils.vision.ScreenVisionUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PathSequenceEventListener {

    public static final PathSequenceEventListener instance = new PathSequenceEventListener(false);
    private static final int PATH_RETRY_NOTIFY_TICKS = 20;
    private static final double PATH_RETRY_MOVEMENT_EPSILON_SQ = 0.16D;
    private static final int EXECUTION_LOG_VARIABLE_PREVIEW_LIMIT = 256;

    private static final Set<String> DEBUG_BREAKPOINTS = ConcurrentHashMap.newKeySet();
    private static final List<PathSequenceEventListener> BACKGROUND_RUNNERS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<PathSequenceEventListener> ACTION_EXECUTION_CONTEXT = new ThreadLocal<>();

    private static boolean builtinSequenceDelayEnabled = false;
    private static int builtinSequenceDelayTicks = 0;

    public PathSequenceManager.PathSequence currentSequence;

    private final boolean backgroundRunner;
    private final String lockOwnerId = UUID.randomUUID().toString();
    private final ScopedRuntimeVariables runtimeVariables = new ScopedRuntimeVariables();
    private final Deque<String> debugTraceLines = new ArrayDeque<>();
    private final Map<String, Object> initialSequenceVariables = new LinkedHashMap<>();

    private boolean tracking;
    private boolean paused;
    private boolean pausedByGui;
    private boolean debugStepArmed;
    private boolean atTarget;
    private boolean waitingForNavigationToFinishAtTarget;
    private boolean registered;

    private String status = "";
    private String currentActionDescription = "";
    private String executionLogSessionId = "";
    private boolean executionResultSuccess = false;
    private String executionResultReason = "";

    private int stepIndex;
    private int actionIndex;
    private int remainingLoops;
    private int tickDelay;
    private boolean explicitDelay;
    private int stepRetryUsed;
    private int currentStepIdleTicks;
    private boolean currentStepIdleAnnounced;
    private double currentStepLastMovementX = Double.NaN;
    private double currentStepLastMovementY = Double.NaN;
    private double currentStepLastMovementZ = Double.NaN;

    private boolean waitConditionRunning;
    private int waitConditionElapsedTicks;
    private long waitConditionStartCapturedUpdateVersion;
    private long waitConditionStartCapturedRecaptureVersion;
    private long waitConditionStartPacketTextVersion;

    private boolean repeatActionRunning;
    private int repeatHeaderIndex = -1;
    private int repeatBodyStartIndex = -1;
    private int repeatBodyEndIndex = -1;
    private int repeatRemainingLoops;
    private int repeatIteration;
    private String repeatLoopVarName = "loop_index";

    private String pendingAsyncActionType = "";
    private int pendingAsyncStepIndex = -1;
    private int pendingAsyncActionIndex = -1;

    private boolean isHunting;
    private double huntRadius;
    private boolean huntAutoAttack;
    private String huntAttackMode = KillAuraHandler.ATTACK_MODE_NORMAL;
    private String huntAttackSequenceName = "";
    private boolean huntAimLockEnabled = true;
    private double huntTrackingDistanceSq;
    private double huntUpRange = KillAuraHandler.DEFAULT_HUNT_UP_RANGE;
    private double huntDownRange = KillAuraHandler.DEFAULT_HUNT_DOWN_RANGE;
    private LivingEntity huntTargetEntity;
    private int lastHuntGotoTargetEntityId = Integer.MIN_VALUE;
    private boolean huntMovementStopped;
    private String huntMode = KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
    private boolean huntOrbitEnabled;
    private boolean huntChaseIntervalEnabled;
    private int huntChaseIntervalTicks;
    private int huntChaseCooldownTicks;
    private boolean huntWasWithinDesiredDistance;
    private int huntAttackCooldownTicks;
    private int huntOrbitLoopNodeIndex = -1;
    private int huntLastOrbitGotoTick = -99999;
    private int huntOrbitStuckTicks = 0;
    private double huntLastOrbitPlayerX = Double.NaN;
    private double huntLastOrbitPlayerZ = Double.NaN;
    private double lastHuntGotoTargetX = Double.NaN;
    private double lastHuntGotoTargetY = Double.NaN;
    private double lastHuntGotoTargetZ = Double.NaN;
    private int huntAttackRemaining = -1;
    private int huntNoTargetSkipCount;
    private int huntNoDamageAttackLimit = KillAuraHandler.DEFAULT_NO_DAMAGE_ATTACK_LIMIT;
    private boolean huntRestrictTargetGroups = true;
    private boolean huntTargetHostile = true;
    private boolean huntTargetPassive;
    private boolean huntTargetPlayers;
    private boolean huntEnableNameWhitelist;
    private boolean huntEnableNameBlacklist;
    private final List<String> huntNameWhitelist = new ArrayList<>();
    private final List<HuntWhitelistTarget> huntWhitelistTargets = new ArrayList<>();
    private final Map<String, Integer> huntWhitelistKillProgress = new LinkedHashMap<>();
    private final Set<Integer> countedHuntKillEntityIds = new LinkedHashSet<>();
    private final Map<Integer, HuntNoDamageAttackTracker> huntNoDamageAttackTrackers = new LinkedHashMap<>();
    private final Set<Integer> huntNoDamageExcludedEntityIds = new LinkedHashSet<>();
    private final List<String> huntNameBlacklist = new ArrayList<>();
    private boolean huntShowRange;
    private boolean huntIgnoreInvisible;
    private double huntCenterX;
    private double huntCenterY;
    private double huntCenterZ;
    private boolean huntPendingCompleteAfterSequence;
    private final HuntAttackSequenceExecutor huntAttackSequenceExecutor = new HuntAttackSequenceExecutor();
    private final HuntOrbitController huntOrbitController = new HuntOrbitController();
    private static final double HUNT_FIXED_DISTANCE_TOLERANCE = 0.30D;
    private static final double HUNT_ORBIT_ENTRY_BUFFER = 1.25D;
    private static final double HUNT_ORBIT_MAX_VERTICAL_DELTA = 3.5D;
    private static final float HUNT_NO_DAMAGE_HEALTH_EPSILON = 0.001F;
    private static final int HUNT_NO_DAMAGE_OBSERVATION_DELAY_TICKS = 1;
    private static final int HUNT_NO_DAMAGE_MAX_TRACKED_TARGETS = 256;
    private static final int HUNT_NO_DAMAGE_MAX_EXCLUDED_TARGETS = 512;

    private static final class HuntWhitelistTarget {
        private final String name;
        private final int killCount;

        private HuntWhitelistTarget(String name, int killCount) {
            this.name = name == null ? "" : name.trim();
            this.killCount = Math.max(0, killCount);
        }

        private boolean hasKillLimit() {
            return killCount > 0;
        }
    }

    private static final class HuntNoDamageAttackTracker {
        private float baselineHealth;
        private int pendingAttempts;
        private int observationTicks;
        private int confirmedNoDamageAttempts;

        private HuntNoDamageAttackTracker(float baselineHealth) {
            this.baselineHealth = baselineHealth;
        }
    }

    private PathSequenceEventListener(boolean backgroundRunner) {
        this.backgroundRunner = backgroundRunner;
    }

    public static class ProgressSnapshot {
        private final String sequenceName;
        private final int stepIndex;
        private final int actionIndex;
        private final boolean atTarget;
        private final int remainingLoops;
        private final int tickDelay;
        private final boolean explicitDelay;
        private final String status;
        private final ScopedRuntimeVariables.ScopeSnapshot variableSnapshot;
        private final int stepRetryUsed;

        public ProgressSnapshot(String sequenceName, int stepIndex, int actionIndex, boolean atTarget,
                int remainingLoops, int tickDelay, boolean explicitDelay, String status,
                ScopedRuntimeVariables.ScopeSnapshot variableSnapshot, int stepRetryUsed) {
            this.sequenceName = sequenceName == null ? "" : sequenceName;
            this.stepIndex = stepIndex;
            this.actionIndex = actionIndex;
            this.atTarget = atTarget;
            this.remainingLoops = remainingLoops;
            this.tickDelay = tickDelay;
            this.explicitDelay = explicitDelay;
            this.status = status == null ? "" : status;
            this.variableSnapshot = variableSnapshot == null
                    ? new ScopedRuntimeVariables.ScopeSnapshot(Collections.emptyMap(), Collections.emptyMap(),
                            Collections.emptyMap())
                    : variableSnapshot;
            this.stepRetryUsed = stepRetryUsed;
        }

        public String getSequenceName() {
            return sequenceName;
        }

        public int getStepIndex() {
            return stepIndex;
        }

        public int getActionIndex() {
            return actionIndex;
        }

        public boolean isAtTarget() {
            return atTarget;
        }

        public int getRemainingLoops() {
            return remainingLoops;
        }

        public int getTickDelay() {
            return tickDelay;
        }

        public boolean isExplicitDelay() {
            return explicitDelay;
        }

        public String getStatus() {
            return status;
        }

        public ScopedRuntimeVariables.ScopeSnapshot getVariableSnapshot() {
            return variableSnapshot;
        }

        public int getStepRetryUsed() {
            return stepRetryUsed;
        }
    }

    public static class DebugSnapshot {
        public enum Resource {
            FOREGROUND, BACKGROUND
        }

        private final boolean tracking;
        private final String sequenceName;
        private final String status;
        private final int stepIndex;
        private final int actionIndex;
        private final boolean paused;
        private final String currentActionDescription;
        private final List<String> traceLines;
        private final Map<String, Object> variablePreview;
        private final Resource resource;
        private final boolean background;

        public DebugSnapshot(boolean tracking, String sequenceName, String status, int stepIndex, int actionIndex,
                boolean paused, String currentActionDescription, List<String> traceLines,
                Map<String, Object> variablePreview, Resource resource, boolean background) {
            this.tracking = tracking;
            this.sequenceName = sequenceName == null ? "" : sequenceName;
            this.status = status == null ? "" : status;
            this.stepIndex = stepIndex;
            this.actionIndex = actionIndex;
            this.paused = paused;
            this.currentActionDescription = currentActionDescription == null ? "" : currentActionDescription;
            this.traceLines = traceLines == null ? Collections.emptyList() : new ArrayList<>(traceLines);
            this.variablePreview = variablePreview == null ? Collections.emptyMap()
                    : new LinkedHashMap<>(variablePreview);
            this.resource = resource == null ? Resource.FOREGROUND : resource;
            this.background = background;
        }

        public boolean isTracking() {
            return tracking;
        }

        public String getSequenceName() {
            return sequenceName;
        }

        public String getStatus() {
            return status;
        }

        public int getStepIndex() {
            return stepIndex;
        }

        public int getActionIndex() {
            return actionIndex;
        }

        public boolean isPaused() {
            return paused;
        }

        public String getCurrentActionDescription() {
            return currentActionDescription;
        }

        public List<String> getTraceLines() {
            return traceLines;
        }

        public Map<String, Object> getVariablePreview() {
            return variablePreview;
        }

        public Resource getResource() {
            return resource;
        }

        public boolean isBackground() {
            return background;
        }
    }

    public static PathSequenceEventListener getCurrentExecutionContext() {
        PathSequenceEventListener context = ACTION_EXECUTION_CONTEXT.get();
        return context == null ? instance : context;
    }

    public static boolean isSequenceRunningInForeground(String sequenceName) {
        return instance.isTracking() && instance.currentSequence != null
                && instance.currentSequence.getName().equals(sequenceName);
    }

    public static boolean isSequenceRunningInBackground(String sequenceName) {
        if (sequenceName == null || sequenceName.trim().isEmpty()) {
            return false;
        }
        for (PathSequenceEventListener runner : BACKGROUND_RUNNERS) {
            if (runner.tracking && runner.currentSequence != null
                    && sequenceName.equalsIgnoreCase(runner.currentSequence.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnySequenceRunning() {
        if (instance != null && instance.isTracking()) {
            return true;
        }
        for (PathSequenceEventListener runner : BACKGROUND_RUNNERS) {
            if (runner != null && runner.isTracking()) {
                return true;
            }
        }
        return false;
    }

    public static boolean startBackgroundSequence(PathSequenceManager.PathSequence sequence) {
        return startBackgroundSequence(sequence, 0, null);
    }

    public static boolean startBackgroundSequence(PathSequenceManager.PathSequence sequence, int remainingLoops) {
        return startBackgroundSequence(sequence, remainingLoops, null);
    }

    public static boolean startBackgroundSequence(PathSequenceManager.PathSequence sequence, int remainingLoops,
            Map<String, Object> initialSequenceVariables) {
        return startBackgroundSequence(sequence, remainingLoops, initialSequenceVariables, 0);
    }

    public static boolean startBackgroundSequence(PathSequenceManager.PathSequence sequence, int remainingLoops,
            Map<String, Object> initialSequenceVariables, int startStepIndex) {
        if (sequence == null || sequence.getSteps() == null || sequence.getSteps().isEmpty()) {
            return false;
        }
        PathSafetyManager.SafetyDecision decision = PathSafetyManager.evaluateBackgroundSequence(sequence);
        if (decision.isBlocked()) {
            return false;
        }
        stopAllBackgroundRunners();
        PathSequenceEventListener runner = new PathSequenceEventListener(true);
        BACKGROUND_RUNNERS.add(runner);
        runner.startTracking(sequence, remainingLoops <= 0 ? -1 : remainingLoops, initialSequenceVariables,
                startStepIndex);
        runner.setStatus(sequence.getName() + " | 后台执行");
        runner.resume();
        return true;
    }

    public static void stopAllBackgroundRunners() {
        for (PathSequenceEventListener runner : new ArrayList<>(BACKGROUND_RUNNERS)) {
            runner.stopTrackingAndStopNavigation("路径序列", "停止全部后台序列时清理后台导航");
        }
        BACKGROUND_RUNNERS.clear();
    }

    public static boolean stopForegroundSequenceByAction() {
        if (instance == null || !instance.isTracking()) {
            return false;
        }
        instance.markExecutionResult(false, "动作停止前台序列");
        instance.stopTrackingAndStopNavigation("动作指令", "动作指令要求停止前台序列");
        return true;
    }

    public static boolean stopBackgroundSequencesByAction() {
        boolean stopped = false;
        for (PathSequenceEventListener runner : new ArrayList<>(BACKGROUND_RUNNERS)) {
            if (runner != null && runner.isTracking()) {
                runner.markExecutionResult(false, "动作停止后台序列");
                runner.stopTrackingAndStopNavigation("动作指令", "动作指令要求停止后台序列");
                stopped = true;
            }
        }
        return stopped;
    }

    public static synchronized boolean isBuiltinSequenceDelayEnabled() {
        return builtinSequenceDelayEnabled;
    }

    public static synchronized int getBuiltinSequenceDelayTicks() {
        return builtinSequenceDelayTicks;
    }

    public static synchronized void updateBuiltinSequenceDelayConfig(boolean enabled, int ticks) {
        builtinSequenceDelayEnabled = enabled;
        builtinSequenceDelayTicks = Math.max(0, ticks);
    }

    public boolean isTracking() {
        return tracking;
    }

    public boolean isAutoEatStepPathingActive() {
        if (!tracking || paused || currentSequence == null || atTarget || waitingForNavigationToFinishAtTarget) {
            return false;
        }
        if (stepIndex < 0 || currentSequence.getSteps() == null || stepIndex >= currentSequence.getSteps().size()) {
            return false;
        }
        PathSequenceManager.PathStep step = currentSequence.getSteps().get(stepIndex);
        return step != null
                && step.hasGotoTarget()
                && EmbeddedNavigationHandler.INSTANCE.isPathingOrCalculating();
    }

    public boolean isBackgroundRunner() {
        return backgroundRunner;
    }

    public void setStatus(String s) {
        this.status = s == null ? "" : s;
    }

    public String getStatus() {
        return status;
    }

    public void pauseForDebug() {
        this.paused = true;
        this.debugStepArmed = false;
    }

    public void resumeFromDebug() {
        this.paused = false;
        this.debugStepArmed = false;
    }

    public void requestDebugStep() {
        this.paused = false;
        this.debugStepArmed = true;
    }

    public void clearDebugTrace() {
        this.debugTraceLines.clear();
    }

    public DebugSnapshot getDebugSnapshot() {
        return new DebugSnapshot(tracking, currentSequence == null ? "" : currentSequence.getName(), status, stepIndex,
                actionIndex, paused, currentActionDescription, new ArrayList<>(debugTraceLines),
                buildVariablePreview(12),
                backgroundRunner ? DebugSnapshot.Resource.BACKGROUND : DebugSnapshot.Resource.FOREGROUND,
                backgroundRunner);
    }

    public static boolean hasDebugBreakpoint(String sequenceName, int stepIndex, int actionIndex) {
        return DEBUG_BREAKPOINTS.contains(sequenceName + "#" + stepIndex + "#" + actionIndex);
    }

    public static void toggleDebugBreakpoint(String sequenceName, int stepIndex, int actionIndex) {
        String key = sequenceName + "#" + stepIndex + "#" + actionIndex;
        if (!DEBUG_BREAKPOINTS.add(key)) {
            DEBUG_BREAKPOINTS.remove(key);
        }
    }

    public boolean wasPausedByGui() {
        return pausedByGui;
    }

    public void pause() {
        this.paused = true;
        this.pausedByGui = false;
    }

    public void resume() {
        this.paused = false;
        this.pausedByGui = false;
        ensureRegistered();
    }

    public void startTracking(PathSequenceManager.PathSequence sequence, int remainingLoops) {
        startTracking(sequence, remainingLoops, null);
    }

    public void startTracking(PathSequenceManager.PathSequence sequence, int remainingLoops,
            Map<String, Object> initialSequenceVariables) {
        startTracking(sequence, remainingLoops, initialSequenceVariables, 0);
    }

    public void startTracking(PathSequenceManager.PathSequence sequence, int remainingLoops,
            Map<String, Object> initialSequenceVariables, int startStepIndex) {
        this.currentSequence = sequence;
        this.remainingLoops = remainingLoops == 0 ? 1 : remainingLoops;
        this.tracking = sequence != null;
        this.paused = false;
        this.pausedByGui = false;
        this.debugStepArmed = false;
        this.stepIndex = clampStartStepIndex(sequence, startStepIndex);
        this.actionIndex = 0;
        this.tickDelay = 0;
        this.explicitDelay = false;
        this.stepRetryUsed = 0;
        this.atTarget = false;
        this.currentActionDescription = "";
        this.pendingAsyncActionType = "";
        this.pendingAsyncActionIndex = -1;
        this.pendingAsyncStepIndex = -1;
        this.waitConditionRunning = false;
        this.waitConditionElapsedTicks = 0;
        this.repeatActionRunning = false;
        this.repeatHeaderIndex = -1;
        this.repeatBodyStartIndex = -1;
        this.repeatBodyEndIndex = -1;
        this.repeatRemainingLoops = 0;
        this.repeatIteration = 0;
        this.repeatLoopVarName = "loop_index";
        this.status = sequence == null ? "" : sequence.getName();
        this.debugTraceLines.clear();
        resetStepPathRetryMonitor();
        resetStepArrivalWaitState();
        finishExecutionLogSessionIfNeeded();
        resetExecutionResultState();
        this.runtimeVariables.clear();
        this.initialSequenceVariables.clear();
        if (initialSequenceVariables != null) {
            for (Map.Entry<String, Object> entry : initialSequenceVariables.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                    continue;
                }
                this.initialSequenceVariables.put(entry.getKey().trim(), entry.getValue());
                this.runtimeVariables.putSequence(entry.getKey().trim(), entry.getValue());
            }
        }
        this.runtimeVariables.enterStep(this.stepIndex);
        startExecutionLogSession();
        restartCurrentStepTarget();
        ensureRegistered();
        recordDebugTrace("序列开始: " + this.status);
    }

    private int clampStartStepIndex(PathSequenceManager.PathSequence sequence, int startStepIndex) {
        if (sequence == null || sequence.getSteps() == null || sequence.getSteps().isEmpty()) {
            return 0;
        }
        return Math.max(0, Math.min(startStepIndex, sequence.getSteps().size() - 1));
    }

    public void stopTracking() {
        if (tracking) {
            recordDebugTrace("序列停止: " + (currentSequence == null ? "unknown" : currentSequence.getName()));
        }
        finishExecutionLogSessionIfNeeded();
        resetHuntState();
        tracking = false;
        paused = false;
        pausedByGui = false;
        explicitDelay = false;
        resetStepPathRetryMonitor();
        resetStepArrivalWaitState();
        currentSequence = null;
        status = "";
        atTarget = false;
        currentActionDescription = "";
        debugStepArmed = false;
        pendingAsyncActionType = "";
        runtimeVariables.clear();
        debugTraceLines.clear();
        ResourceLockManager.releaseAll(lockOwnerId);
        ensureUnregistered();
        if (backgroundRunner) {
            BACKGROUND_RUNNERS.remove(this);
        }
    }

    public void stopTrackingAndStopNavigation(String source, String reason) {
        logSequenceStop(source, reason);
        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                normalizeStopReason(reason));
        stopTracking();
    }

    public ProgressSnapshot captureProgressSnapshot() {
        return new ProgressSnapshot(currentSequence == null ? "" : currentSequence.getName(), stepIndex, actionIndex,
                atTarget, remainingLoops, tickDelay, explicitDelay, status, runtimeVariables.captureSnapshot(),
                stepRetryUsed);
    }

    public boolean resumeFromSnapshot(PathSequenceManager.PathSequence sequence, ProgressSnapshot snapshot) {
        if (sequence == null || snapshot == null) {
            return false;
        }
        this.currentSequence = sequence;
        this.stepIndex = Math.max(0, snapshot.getStepIndex());
        this.actionIndex = Math.max(0, snapshot.getActionIndex());
        this.remainingLoops = snapshot.getRemainingLoops();
        this.tickDelay = Math.max(0, snapshot.getTickDelay());
        this.explicitDelay = snapshot.isExplicitDelay();
        this.status = snapshot.getStatus();
        this.stepRetryUsed = snapshot.getStepRetryUsed();
        this.atTarget = snapshot.isAtTarget();
        this.tracking = true;
        this.paused = false;
        this.pausedByGui = false;
        resetStepPathRetryMonitor();
        this.runtimeVariables.clear();
        this.runtimeVariables.enterStep(this.stepIndex);
        this.runtimeVariables.restoreSnapshot(snapshot.getVariableSnapshot());
        restartCurrentStepTarget();
        ensureRegistered();
        return true;
    }

    public void startHunting(JsonObject params) {
        resetHuntState();
        this.isHunting = true;
        this.huntRadius = Math.max(0.0D, getDouble(params, "radius", 3.0D));
        this.huntUpRange = Math.max(0.0D,
                getDouble(params, "huntUpRange", KillAuraHandler.DEFAULT_HUNT_UP_RANGE));
        this.huntDownRange = Math.max(0.0D,
                getDouble(params, "huntDownRange", KillAuraHandler.DEFAULT_HUNT_DOWN_RANGE));
        this.huntAutoAttack = params == null || !params.has("autoAttack") || params.get("autoAttack").getAsBoolean();
        this.huntAttackMode = normalizeHuntAttackMode(getString(params, "attackMode"));
        this.huntAttackSequenceName = getString(params, "attackSequenceName");
        this.huntAimLockEnabled = params == null || !params.has("huntAimLockEnabled")
                || params.get("huntAimLockEnabled").getAsBoolean();
        double trackingDistance = Math.max(0.5D, getDouble(params, "trackingDistance", 1.0D));
        this.huntTrackingDistanceSq = trackingDistance * trackingDistance;
        this.huntMode = readHuntModeParam(params, "huntMode", KillAuraHandler.HUNT_MODE_FIXED_DISTANCE);
        this.huntOrbitEnabled = isHuntFixedDistanceMode()
                && params != null
                && params.has("huntOrbitEnabled")
                && params.get("huntOrbitEnabled").getAsBoolean();
        this.huntChaseIntervalEnabled = params != null
                && params.has("huntChaseIntervalEnabled")
                && params.get("huntChaseIntervalEnabled").getAsBoolean();
        this.huntChaseIntervalTicks = Math.max(0,
                (int) Math.round(getDouble(params, "huntChaseIntervalSeconds", 0.0D) * 20.0D));
        int attackCount = getInt(params, "attackCount", 0);
        this.huntAttackRemaining = attackCount > 0 ? attackCount : -1;
        this.huntNoTargetSkipCount = Math.max(0, getInt(params, "noTargetSkipCount", 0));
        this.huntNoDamageAttackLimit = KillAuraHandler.getNoDamageAttackLimit();
        this.huntShowRange = params != null
                && params.has("showHuntRange")
                && params.get("showHuntRange").getAsBoolean();
        this.huntIgnoreInvisible = params != null
                && params.has("ignoreInvisible")
                && params.get("ignoreInvisible").getAsBoolean();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            this.huntCenterX = player.getX();
            this.huntCenterY = player.getY();
            this.huntCenterZ = player.getZ();
        }

        boolean hasTargetGroupConfig = params != null
                && (params.has("targetHostile") || params.has("targetPassive") || params.has("targetPlayers"));
        if (hasTargetGroupConfig) {
            this.huntTargetHostile = params.has("targetHostile") && params.get("targetHostile").getAsBoolean();
            this.huntTargetPassive = params.has("targetPassive") && params.get("targetPassive").getAsBoolean();
            this.huntTargetPlayers = params.has("targetPlayers") && params.get("targetPlayers").getAsBoolean();
            this.huntRestrictTargetGroups = this.huntTargetHostile || this.huntTargetPassive || this.huntTargetPlayers;
        } else {
            this.huntTargetHostile = true;
            this.huntTargetPassive = false;
            this.huntTargetPlayers = false;
            this.huntRestrictTargetGroups = true;
        }

        this.huntEnableNameWhitelist = params != null
                && params.has("enableNameWhitelist")
                && params.get("enableNameWhitelist").getAsBoolean();
        this.huntEnableNameBlacklist = params != null
                && params.has("enableNameBlacklist")
                && params.get("enableNameBlacklist").getAsBoolean();
        this.huntNameWhitelist.addAll(readHuntNameList(params, "nameWhitelist", "nameWhitelistText"));
        this.huntWhitelistTargets.addAll(readHuntWhitelistTargets(params));
        for (HuntWhitelistTarget target : this.huntWhitelistTargets) {
            if (target == null || target.name.isEmpty()) {
                continue;
            }
            addHuntNameKeyword(this.huntNameWhitelist, target.name);
            if (target.hasKillLimit()) {
                this.huntWhitelistKillProgress.put(target.name, 0);
            }
        }
        this.huntNameBlacklist.addAll(readHuntNameList(params, "nameBlacklist", "nameBlacklistText"));

        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                "中心搜怪击杀动作开始");
        status = currentSequence == null ? "hunt" : currentSequence.getName() + " | 中心搜怪击杀";
        recordDebugTrace("hunt 动作已启动");
        zszlScriptMod.LOGGER.info(
                "[路径序列] 中心搜怪配置: 半径={}, 垂直=+{}/-{}, 攻击模式={}, 追击模式={}, 绕圈={}, 无目标跳过={}, 无掉血排除={}, 目标类型[敌对={}, 被动={}, 玩家={}], 白名单={}, 黑名单={}, 显示范围={}",
                huntRadius,
                huntUpRange,
                huntDownRange,
                huntAttackMode,
                huntMode,
                huntOrbitEnabled,
                huntNoTargetSkipCount,
                huntNoDamageAttackLimit > 0 ? huntNoDamageAttackLimit : "关闭",
                huntTargetHostile,
                huntTargetPassive,
                huntTargetPlayers,
                huntEnableNameWhitelist ? formatHuntWhitelistForLog() : "关闭",
                huntEnableNameBlacklist ? huntNameBlacklist : "关闭",
                huntShowRange);
        recordDebugTrace("hunt config: 中心=("
                + String.format(Locale.ROOT, "%.2f, %.2f, %.2f", huntCenterX, huntCenterY, huntCenterZ)
                + "), 半径=" + String.format(Locale.ROOT, "%.1f", huntRadius)
                + ", 垂直=+" + String.format(Locale.ROOT, "%.1f", huntUpRange)
                + "/-" + String.format(Locale.ROOT, "%.1f", huntDownRange)
                + ", 目标类型[敌对=" + huntTargetHostile
                + ", 被动=" + huntTargetPassive
                + ", 玩家=" + huntTargetPlayers
                + "], 动作白名单="
                + (huntEnableNameWhitelist ? formatHuntWhitelistForLog() : "关闭")
                + ", 动作黑名单="
                + (huntEnableNameBlacklist ? huntNameBlacklist : "关闭")
                + ", 忽略隐身=" + huntIgnoreInvisible);
    }

    public void startFollowingEntity(JsonObject params) {
        recordDebugTrace("follow_entity 动作已触发");
        EmbeddedNavigationHandler.INSTANCE.startFollowEntities(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                "路径动作 follow_entity 开始跟随实体");
        status = currentSequence == null ? "follow" : currentSequence.getName() + " | follow";
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!tracking || paused || currentSequence == null) {
            return;
        }
        if (event.phase != TickEvent.Phase.START || event.side != Side.CLIENT) {
            return;
        }
        if (!(event.player instanceof LocalPlayer)) {
            return;
        }
        if (isHunting) {
            executeHuntTick((LocalPlayer) event.player);
            return;
        }
        runTick((LocalPlayer) event.player);
    }

    private void runTick(LocalPlayer player) {
        if (player == null || currentSequence == null || currentSequence.getSteps() == null) {
            stopTrackingAndStopNavigation("路径序列", "路径序列运行时缺少有效步骤数据");
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            if (tickDelay > 0) {
                return;
            }
            explicitDelay = false;
        }

        List<PathSequenceManager.PathStep> steps = currentSequence.getSteps();
        if (stepIndex >= steps.size()) {
            finishSequence(player);
            return;
        }

        PathSequenceManager.PathStep step = steps.get(stepIndex);
        if (!atTarget) {
            if (step != null && step.hasGotoTarget()) {
                if (AutoEatHandler.isEating) {
                    return;
                }
                if (hasReachedGotoTarget(player, step, step.getGotoPoint())) {
                    if (EmbeddedNavigationHandler.INSTANCE.isPathingOrCalculating()) {
                        if (!waitingForNavigationToFinishAtTarget) {
                            waitingForNavigationToFinishAtTarget = true;
                            recordDebugTrace("进入到达范围，等待寻路自然结束");
                            setStatus(currentSequence.getName() + (backgroundRunner ? " | 后台执行" : "")
                                    + " | 到达范围内，等待寻路完成");
                        }
                        resetStepPathRetryMonitor();
                        return;
                    }
                    if (waitingForNavigationToFinishAtTarget) {
                        recordDebugTrace("寻路自然结束，开始执行步骤动作");
                        restoreSequenceBaseStatus();
                    }
                    resetStepArrivalWaitState();
                } else {
                    if (waitingForNavigationToFinishAtTarget) {
                        resetStepArrivalWaitState();
                        restoreSequenceBaseStatus();
                    }
                    if (handleCurrentStepPathRetry(player, step)) {
                        return;
                    }
                    return;
                }
            }
            atTarget = true;
            actionIndex = 0;
            stepRetryUsed = 0;
            resetStepPathRetryMonitor();
            runtimeVariables.enterStep(stepIndex);
            recordDebugTrace("到达步骤目标: " + stepIndex);
        }

        List<PathSequenceManager.ActionData> actions = step == null || step.getActions() == null
                ? Collections.emptyList()
                : step.getActions();

        if (repeatActionRunning && actionIndex > repeatBodyEndIndex) {
            if (repeatRemainingLoops > 1) {
                repeatRemainingLoops--;
                repeatIteration++;
                runtimeVariables.putLocal(repeatLoopVarName, repeatIteration);
                runtimeVariables.putLocal(repeatLoopVarName + "_remaining", repeatRemainingLoops);
                actionIndex = repeatBodyStartIndex;
                tickDelay = 1;
                return;
            }
            repeatActionRunning = false;
            runtimeVariables.remove(repeatLoopVarName);
            runtimeVariables.remove(repeatLoopVarName + "_remaining");
        }

        if (actionIndex >= actions.size()) {
            advanceStep();
            return;
        }

        PathSequenceManager.ActionData rawAction = actions.get(actionIndex);
        runtimeVariables.beginAction(stepIndex, actionIndex);
        PathSequenceManager.ActionData action = resolveRuntimeActionData(rawAction, player);
        currentActionDescription = action == null ? "" : action.getDescription();

        if (currentSequence != null && hasDebugBreakpoint(currentSequence.getName(), stepIndex, actionIndex)) {
            pauseForDebug();
            recordDebugTrace("命中断点: step=" + stepIndex + ", action=" + actionIndex);
            return;
        }

        if (action == null || action.type == null) {
            recordDebugTrace("动作解析失败，已跳过");
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        PathSafetyManager.SafetyDecision safetyDecision = PathSafetyManager.evaluateAction(action.type);
        if (safetyDecision.isBlocked()) {
            recordDebugTrace("安全模式跳过: " + action.type + " / " + safetyDecision.getReason());
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        if (!ensureResources(resolveActionResources(action), currentActionDescription)) {
            return;
        }

        int executedStepIndex = stepIndex;
        int executedActionIndex = actionIndex;

        if (handleRuntimeControlAction(player, actions, rawAction, action)) {
            afterActionProgress("控制流动作", executedStepIndex, executedActionIndex);
            return;
        }

        if (handleConditionalOrWaitAction(player, action)) {
            return;
        }

        if ("hunt".equals(normalize(action.type))) {
            if (!isHunting) {
                startHunting(action.params);
            }
            releaseResources();
            recordDebugTrace("中心搜怪动作运行中");
            return;
        }

        Consumer<LocalPlayer> parsedAction = PathSequenceManager.parseAction(action.type, action.params);
        if (parsedAction == null) {
            recordDebugTrace("动作缺少实现: " + action.type);
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        if (handleAsyncAction(player, action, parsedAction)) {
            return;
        }

        try {
            runWithExecutionContext(() -> parsedAction.accept(player));
        } catch (Exception error) {
            recordDebugTrace("动作执行异常: " + error.getMessage());
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        if (parsedAction instanceof ModUtils.DelayAction) {
            ModUtils.DelayAction delayAction = (ModUtils.DelayAction) parsedAction;
            tickDelay = Math.max(0, delayAction.getDelayTicks());
            explicitDelay = tickDelay > 0;
        } else {
            applyBuiltinSequenceDelay();
        }

        actionIndex++;
        releaseResources();
        afterActionProgress("动作执行完成", executedStepIndex, executedActionIndex);
    }

    private void afterActionProgress(String message) {
        afterActionProgress(message, stepIndex, actionIndex);
    }

    private void afterActionProgress(String message, int executedStepIndex, int executedActionIndex) {
        recordDebugTrace(message + ": step=" + executedStepIndex + ", action=" + executedActionIndex,
                executedStepIndex, executedActionIndex);
        if (debugStepArmed) {
            debugStepArmed = false;
            paused = true;
        }
    }

    private void finishSequence(LocalPlayer player) {
        if (!backgroundRunner && PathSequenceManager.resumeCallerSequenceAfterAction(player)) {
            markExecutionResult(true, "执行完成");
            stopTracking();
            return;
        }

        if (remainingLoops == -1) {
            restartLoop();
            return;
        }
        if (remainingLoops > 1) {
            remainingLoops--;
            restartLoop();
            return;
        }
        markExecutionResult(true, "执行完成");
        stopTrackingAndStopNavigation("路径序列", "路径序列正常执行完成");
    }

    private void restartLoop() {
        if (backgroundRunner) {
            stepIndex = 0;
            actionIndex = 0;
            atTarget = false;
            tickDelay = 0;
            explicitDelay = false;
            stepRetryUsed = 0;
            resetStepPathRetryMonitor();
            resetStepArrivalWaitState();
            runtimeVariables.clearNonGlobal();
            for (Map.Entry<String, Object> entry : initialSequenceVariables.entrySet()) {
                runtimeVariables.putSequence(entry.getKey(), entry.getValue());
            }
            runtimeVariables.enterStep(0);
            restartCurrentStepTarget();
            return;
        }
        PathSequenceManager.startNextLoopWithVariables(currentSequence.getName(),
                new LinkedHashMap<>(initialSequenceVariables));
        stopTracking();
    }

    private void advanceStep() {
        stepIndex++;
        actionIndex = 0;
        atTarget = false;
        stepRetryUsed = 0;
        resetStepPathRetryMonitor();
        resetStepArrivalWaitState();
        repeatActionRunning = false;
        waitConditionRunning = false;
        waitConditionElapsedTicks = 0;
        pendingAsyncActionType = "";
        restartCurrentStepTarget();
    }

    private void restartCurrentStepTarget() {
        resetStepArrivalWaitState();
        if (currentSequence == null || currentSequence.getSteps() == null
                || stepIndex >= currentSequence.getSteps().size()) {
            EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                    "当前步骤不存在更多目标，停止路径序列导航");
            resetStepPathRetryMonitor();
            return;
        }
        PathSequenceManager.PathStep step = currentSequence.getSteps().get(stepIndex);
        if (step == null || !step.hasGotoTarget()) {
            atTarget = true;
            EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                    "当前步骤没有 goto 目标，停止路径序列导航");
            resetStepPathRetryMonitor();
            return;
        }
        double[] target = step.getGotoPoint();
        EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                target[0], target[1], target[2], true, "路径序列刷新当前步骤目标导航");
        initializeStepPathRetryMonitor(Minecraft.getInstance().player);
    }

    private boolean hasReachedGotoTarget(LocalPlayer player, PathSequenceManager.PathStep step, double[] target) {
        if (player == null || target == null || target.length < 3 || Double.isNaN(target[0])) {
            return true;
        }
        int tolerance = step == null ? 0 : step.getArrivalToleranceBlocks();
        int playerX = (int) Math.floor(player.getX());
        int playerY = (int) Math.floor(player.getY());
        int playerZ = (int) Math.floor(player.getZ());
        int targetX = (int) Math.floor(target[0]);
        int targetZ = (int) Math.floor(target[2]);
        if (Double.isNaN(target[1])) {
            return Math.abs(playerX - targetX) <= tolerance && Math.abs(playerZ - targetZ) <= tolerance;
        }
        int targetY = (int) Math.floor(target[1]);
        return Math.abs(playerX - targetX) <= tolerance
                && Math.abs(playerY - targetY) <= tolerance
                && Math.abs(playerZ - targetZ) <= tolerance;
    }

    private PathSequenceManager.ActionData resolveRuntimeActionData(PathSequenceManager.ActionData rawAction,
            LocalPlayer player) {
        if (rawAction == null) {
            return null;
        }
        JsonObject resolvedParams = LegacyActionRuntime.resolveParams(rawAction.params, runtimeVariables, player,
                currentSequence, stepIndex, actionIndex, getLiteralParamKeysForAction(rawAction.type));
        return new PathSequenceManager.ActionData(rawAction.type, resolvedParams);
    }

    private boolean handleRuntimeControlAction(LocalPlayer player, List<PathSequenceManager.ActionData> actions,
            PathSequenceManager.ActionData rawAction, PathSequenceManager.ActionData action) {
        String type = normalize(action.type);
        if ("set_var".equals(type)) {
            String name = getString(action.params, "name");
            if (!name.isEmpty()) {
                JsonObject rawParams = rawAction == null || rawAction.params == null ? new JsonObject() : rawAction.params;
                ensureSetVarDefaultValue(name, rawParams, player);
                try {
                    Object value = LegacyActionRuntime.resolveAssignedValue(
                            rawParams, runtimeVariables, player,
                            currentSequence, stepIndex, actionIndex);
                    runtimeVariables.put(name, value);
                    recordDebugTrace("set_var: " + name + " = " + LegacyActionRuntime.stringifyValue(value));
                } catch (Exception error) {
                    Object fallbackValue = LegacyActionRuntime.inferAssignedValueDefault(rawParams);
                    runtimeVariables.put(name, fallbackValue);
                    recordDebugTrace("set_var 失败: " + name + " -> " + safeError(error)
                            + " ; fallback=" + LegacyActionRuntime.stringifyValue(fallbackValue));
                }
            }
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("goto_action".equals(type)) {
            actionIndex = Math.max(0, getInt(action.params, "targetActionIndex", actionIndex + 1));
            releaseResources();
            return true;
        }
        if ("skip_actions".equals(type)) {
            actionIndex = Math.min(actions.size(), actionIndex + 1 + Math.max(0, getInt(action.params, "count", 1)));
            releaseResources();
            return true;
        }
        if ("skip_steps".equals(type)) {
            stepIndex = Math.min(currentSequence.getSteps().size(),
                    stepIndex + 1 + Math.max(0, getInt(action.params, "count", 0)));
            actionIndex = 0;
            atTarget = false;
            releaseResources();
            resetStepArrivalWaitState();
            restartCurrentStepTarget();
            return true;
        }
        if ("repeat_actions".equals(type)) {
            int bodyCount = Math.max(0, getInt(action.params, "bodyCount", 0));
            int count = Math.max(0, getInt(action.params, "count", 0));
            if (bodyCount <= 0 || count <= 0 || actionIndex + 1 >= actions.size()) {
                actionIndex++;
                releaseResources();
                return true;
            }
            repeatActionRunning = true;
            repeatHeaderIndex = actionIndex;
            repeatBodyStartIndex = actionIndex + 1;
            repeatBodyEndIndex = Math.min(actions.size() - 1, repeatBodyStartIndex + bodyCount - 1);
            repeatRemainingLoops = count;
            repeatIteration = 0;
            repeatLoopVarName = getString(action.params, "loopVar");
            if (repeatLoopVarName.isEmpty()) {
                repeatLoopVarName = "loop_index";
            }
            runtimeVariables.putLocal(repeatLoopVarName, repeatIteration);
            runtimeVariables.putLocal(repeatLoopVarName + "_remaining", repeatRemainingLoops);
            actionIndex = repeatBodyStartIndex;
            releaseResources();
            return true;
        }
        if ("capture_gui_title".equals(type)) {
            String varName = getString(action.params, "varName");
            if (varName.isEmpty()) {
                varName = "gui_title";
            }
            runtimeVariables.put(varName, getCurrentGuiTitle());
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("capture_inventory_slot".equals(type)) {
            captureInventorySlot(action.params);
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("capture_hotbar".equals(type)) {
            captureHotbar(action.params);
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("capture_packet_field".equals(type)) {
            capturePacketField(action.params);
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("capture_gui_element".equals(type)) {
            captureGuiElement(action.params);
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("capture_screen_region".equals(type)) {
            captureScreenRegion(action.params);
            actionIndex++;
            releaseResources();
            return true;
        }
        if ("capture_block_at".equals(type)) {
            captureBlockAt(action.params);
            actionIndex++;
            releaseResources();
            return true;
        }
        return false;
    }

    private boolean handleConditionalOrWaitAction(LocalPlayer player, PathSequenceManager.ActionData action) {
        String type = normalize(action.type);
        boolean waitAction = type.startsWith("wait_until_") || "wait_combined".equals(type);
        boolean conditionAction = type.startsWith("condition_");
        if (!waitAction && !conditionAction) {
            return false;
        }

        if ("wait_until_packet_text".equals(type)) {
            PacketCaptureHandler.requestRecentPacketTextTracking();
        }

        if (!waitConditionRunning) {
            waitConditionRunning = true;
            waitConditionElapsedTicks = 0;
            String capturedId = getString(action.params, "capturedId");
            waitConditionStartCapturedUpdateVersion = CapturedIdRuleManager.getCapturedUpdateVersion(capturedId);
            waitConditionStartCapturedRecaptureVersion = CapturedIdRuleManager.getCapturedRecaptureVersion(capturedId);
            waitConditionStartPacketTextVersion = PacketCaptureHandler.getRecentPacketTextVersion();
        }

        boolean matched = evaluateConditionAction(player, action);
        if (conditionAction) {
            if (matched) {
                actionIndex++;
            } else {
                actionIndex += Math.max(1, getInt(action.params, "skipCount", 1)) + 1;
            }
            waitConditionRunning = false;
            waitConditionElapsedTicks = 0;
            releaseResources();
            return true;
        }

        int timeoutTicks = Math.max(0, getInt(action.params, "timeoutTicks", 0));
        if (matched) {
            actionIndex++;
            waitConditionRunning = false;
            waitConditionElapsedTicks = 0;
            releaseResources();
            applyBuiltinSequenceDelay();
            return true;
        }
        if (timeoutTicks > 0 && waitConditionElapsedTicks >= timeoutTicks) {
            actionIndex += Math.max(0, getInt(action.params, "timeoutSkipCount", 0)) + 1;
            waitConditionRunning = false;
            waitConditionElapsedTicks = 0;
            releaseResources();
            applyBuiltinSequenceDelay();
            return true;
        }

        waitConditionElapsedTicks++;
        tickDelay = 1;
        return true;
    }

    private boolean evaluateConditionAction(LocalPlayer player, PathSequenceManager.ActionData action) {
        String type = normalize(action.type);
        if ("condition_expression".equals(type) || "wait_until_expression".equals(type)) {
            String expression = getString(action.params, "expression");
            return !expression.isEmpty() && LegacyActionRuntime.evaluateExpression(expression, action.params,
                    runtimeVariables, player, currentSequence, stepIndex, actionIndex);
        }
        if ("wait_combined".equals(type)) {
            List<String> expressions = splitLines(getString(action.params, "conditionsText"));
            if (expressions.isEmpty()) {
                return false;
            }
            boolean anyMode = !"ALL".equalsIgnoreCase(getString(action.params, "combinedMode"));
            boolean anyMatched = false;
            for (String expression : expressions) {
                boolean matched = LegacyActionRuntime.evaluateExpression(expression, action.params, runtimeVariables,
                        player, currentSequence, stepIndex, actionIndex);
                anyMatched |= matched;
                if (anyMode && matched) {
                    return true;
                }
                if (!anyMode && !matched) {
                    return false;
                }
            }
            return anyMode ? anyMatched : true;
        }
        if ("condition_inventory_item".equals(type) || "wait_until_inventory_item".equals(type)) {
            return inventoryHasMatchingItem(player, action.params, readMainInventorySlotSelection(action.params));
        }
        if ("condition_gui_title".equals(type) || "wait_until_gui_title".equals(type)) {
            String title = getCurrentGuiTitle().toLowerCase(Locale.ROOT);
            String expected = getString(action.params, "title").toLowerCase(Locale.ROOT);
            return expected.isEmpty() || title.contains(expected);
        }
        if ("condition_player_in_area".equals(type) || "wait_until_player_in_area".equals(type)) {
            double[] center = parsePosition(getString(action.params, "center"));
            double radius = getDouble(action.params, "radius", 3D);
            if (player == null || center == null) {
                return false;
            }
            double dx = player.getX() - center[0];
            double dy = player.getY() - center[1];
            double dz = player.getZ() - center[2];
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
        if ("condition_entity_nearby".equals(type) || "wait_until_entity_nearby".equals(type)) {
            return hasNearbyMatchingEntity(player, getString(action.params, "entityName"),
                    getDouble(action.params, "radius", 6D));
        }
        if ("wait_until_hud_text".equals(type)) {
            return hasMatchingHudText(getString(action.params, "contains"),
                    action.params.has("matchBlock") && action.params.get("matchBlock").getAsBoolean(),
                    getString(action.params, "separator"));
        }
        if ("wait_until_captured_id".equals(type)) {
            String capturedId = getString(action.params, "capturedId");
            String waitMode = getString(action.params, "waitMode");
            if ("RECAPTURE".equalsIgnoreCase(waitMode)) {
                return CapturedIdRuleManager
                        .getCapturedRecaptureVersion(capturedId) > waitConditionStartCapturedRecaptureVersion;
            }
            if (CapturedIdRuleManager.getCapturedUpdateVersion(capturedId) > waitConditionStartCapturedUpdateVersion) {
                return true;
            }
            return !CapturedIdRuleManager.getCapturedIdHex(capturedId).isEmpty();
        }
        if ("wait_until_packet_text".equals(type)) {
            String expected = getString(action.params, "packetText").toLowerCase(Locale.ROOT);
            if (PacketCaptureHandler.getRecentPacketTextVersion() <= waitConditionStartPacketTextVersion) {
                return false;
            }
            for (String line : PacketCaptureHandler.getRecentPacketTextsSnapshot()) {
                if (line != null && line.toLowerCase(Locale.ROOT).contains(expected)) {
                    return true;
                }
            }
            return false;
        }
        if ("wait_until_screen_region".equals(type)) {
            return evaluateScreenRegionWait(action.params);
        }
        return false;
    }

    private boolean handleAsyncAction(LocalPlayer player, PathSequenceManager.ActionData action,
            Consumer<LocalPlayer> parsedAction) {
        String type = normalize(action.type);
        if (!"transferitemstowarehouse".equals(type) && !"move_inventory_items_to_chest_slots".equals(type)
                && !"spread_inventory_item".equals(type)
                && !"warehouse_auto_deposit".equals(type)) {
            return false;
        }

        boolean samePending = type.equals(pendingAsyncActionType) && pendingAsyncStepIndex == stepIndex
                && pendingAsyncActionIndex == actionIndex;
        if (!samePending) {
            try {
                runWithExecutionContext(() -> parsedAction.accept(player));
            } catch (Exception error) {
                recordDebugTrace("异步动作异常: " + error.getMessage());
                actionIndex++;
                releaseResources();
                return true;
            }
            pendingAsyncActionType = type;
            pendingAsyncStepIndex = stepIndex;
            pendingAsyncActionIndex = actionIndex;
        }

        if (isAsyncActionInProgress(type)) {
            tickDelay = 2;
            return true;
        }

        pendingAsyncActionType = "";
        pendingAsyncStepIndex = -1;
        pendingAsyncActionIndex = -1;
        actionIndex++;
        releaseResources();
        applyBuiltinSequenceDelay();
        return true;
    }

    private boolean isAsyncActionInProgress(String actionType) {
        if ("transferitemstowarehouse".equals(actionType)) {
            return ItemFilterHandler.isWarehouseTransferInProgress();
        }
        if ("move_inventory_items_to_chest_slots".equals(actionType)) {
            return ItemFilterHandler.isWarehouseTransferInProgress();
        }
        if ("spread_inventory_item".equals(actionType)) {
            return ItemSpreadHandler.isSpreadInProgress();
        }
        if ("warehouse_auto_deposit".equals(actionType)) {
            return WarehouseEventHandler.isAutoDepositRouteRunning();
        }
        return false;
    }

    private void applyBuiltinSequenceDelay() {
        if (builtinSequenceDelayEnabled && builtinSequenceDelayTicks > 0) {
            tickDelay = Math.max(tickDelay, builtinSequenceDelayTicks);
        }
    }

    private boolean inventoryHasMatchingItem(LocalPlayer player, JsonObject params,
            Set<Integer> selectedInventorySlots) {
        if (player == null) {
            return false;
        }
        List<String> itemFilterExpressions = InventoryItemFilterExpressionEngine.readExpressions(params);
        String itemName = params != null && params.has("itemName") ? params.get("itemName").getAsString() : "";
        String matchMode = params != null && params.has("matchMode") ? params.get("matchMode").getAsString() : "CONTAINS";
        List<String> requiredNbtTags = ItemFilterHandler.readTagFilters(params, "requiredNbtTags", "requiredNbtTagsText");
        String requiredNbtTagMatchMode = ItemFilterHandler.readRequiredNbtTagMatchMode(params);
        int minCount = params != null && params.has("count") ? params.get("count").getAsInt() : 1;
        String expected = itemName == null ? "" : itemName.trim().toLowerCase(Locale.ROOT);
        boolean hasExpressionCondition = itemFilterExpressions != null && !itemFilterExpressions.isEmpty();
        boolean hasNameCondition = !expected.isEmpty();
        boolean hasNbtCondition = requiredNbtTags != null && !requiredNbtTags.isEmpty();
        if (!hasExpressionCondition && !hasNameCondition && !hasNbtCondition) {
            return false;
        }
        int totalCount = 0;
        boolean exact = "EXACT".equalsIgnoreCase(matchMode);
        List<ItemStack> mainInventory = player.getInventory().items;
        boolean restrictSlots = selectedInventorySlots != null && !selectedInventorySlots.isEmpty();
        for (int slotIndex = 0; slotIndex < mainInventory.size(); slotIndex++) {
            if (restrictSlots && !selectedInventorySlots.contains(slotIndex)) {
                continue;
            }
            ItemStack stack = mainInventory.get(slotIndex);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            boolean matched;
            if (hasExpressionCondition) {
                matched = false;
                for (String expression : itemFilterExpressions) {
                    if (expression == null || expression.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        if (InventoryItemFilterExpressionEngine.matches(stack, slotIndex, expression)) {
                            matched = true;
                            break;
                        }
                    } catch (Exception e) {
                        zszlScriptMod.LOGGER.warn("[legacy_path] 物品过滤表达式解析失败: {}", expression, e);
                    }
                }
            } else {
                String actual = stack.getHoverName().getString();
                if (actual == null) {
                    continue;
                }
                actual = actual.trim().toLowerCase(Locale.ROOT);
                boolean matchedName = !hasNameCondition || (exact ? actual.equals(expected) : actual.contains(expected));
                matched = matchedName
                        && ItemFilterHandler.matchesRequiredNbtTags(stack, requiredNbtTags, requiredNbtTagMatchMode);
            }
            if (!matched) {
                continue;
            }
            totalCount += stack.getCount();
            if (totalCount >= Math.max(1, minCount)) {
                return true;
            }
        }
        return false;
    }

    private Set<Integer> readMainInventorySlotSelection(JsonObject params) {
        java.util.LinkedHashSet<Integer> result = new java.util.LinkedHashSet<>();
        if (params == null) {
            return result;
        }
        if (params.has("inventorySlots") && params.get("inventorySlots").isJsonArray()) {
            for (JsonElement element : params.getAsJsonArray("inventorySlots")) {
                try {
                    result.add(Math.max(0, element.getAsInt()));
                } catch (Exception ignored) {
                }
            }
            return result;
        }
        if (params.has("inventorySlots") && params.get("inventorySlots").isJsonPrimitive()) {
            for (String token : params.get("inventorySlots").getAsString().split("[,\\r\\n\\s]+")) {
                if (token == null || token.trim().isEmpty()) {
                    continue;
                }
                try {
                    result.add(Math.max(0, Integer.parseInt(token.trim())));
                } catch (Exception ignored) {
                }
            }
            return result;
        }
        if (params.has("inventorySlotsText") && params.get("inventorySlotsText").isJsonPrimitive()) {
            for (String token : params.get("inventorySlotsText").getAsString().split("[,\\r\\n\\s]+")) {
                if (token == null || token.trim().isEmpty()) {
                    continue;
                }
                try {
                    result.add(Math.max(0, Integer.parseInt(token.trim())));
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private boolean hasNearbyMatchingEntity(LocalPlayer player, String entityName, double radius) {
        if (player == null || player.level() == null) {
            return false;
        }
        String expected = entityName == null ? "" : entityName.trim().toLowerCase(Locale.ROOT);
        AABB bounds = player.getBoundingBox().inflate(radius);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (entity == null || entity == player || !entity.isAlive()) {
                continue;
            }
            String actual = entity.getName().getString().toLowerCase(Locale.ROOT);
            if (expected.isEmpty() || actual.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMatchingHudText(String contains, boolean matchBlock, String separator) {
        String expected = contains == null ? "" : contains.trim();
        if (matchBlock) {
            String joiner = separator == null || separator.isEmpty() ? " | " : separator;
            for (HudTextScanner.TextBlock block : HudTextScanner.INSTANCE.getProcessedTextBlocks()) {
                String text = block.getJoinedText(joiner);
                if (expected.isEmpty() || text.contains(expected)) {
                    return true;
                }
            }
            return false;
        }
        for (HudTextScanner.CapturedText text : HudTextScanner.INSTANCE.getCurrentHudText()) {
            if (expected.isEmpty() || text.text.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateScreenRegionWait(JsonObject params) {
        int[] rect = parseVisionRegionRect(params);
        if (rect == null) {
            return false;
        }
        String mode = getString(params, "visionCompareMode");
        if ("TEMPLATE".equalsIgnoreCase(mode)) {
            double threshold = getDouble(params, "similarityThreshold", 0.92D);
            ScreenVisionUtils.TemplateMatchResult result = ScreenVisionUtils.compareRegionToTemplate(rect[0], rect[1],
                    rect[2], rect[3], getString(params, "imagePath"));
            return result.isFound() && result.getSimilarity() >= threshold;
        }
        ScreenVisionUtils.RegionMetrics metrics = ScreenVisionUtils.analyzeRegion(rect[0], rect[1], rect[2], rect[3]);
        if (!metrics.isFound()) {
            return false;
        }
        if ("EDGE_DENSITY".equalsIgnoreCase(mode)) {
            return metrics.getEdgeDensity() >= getDouble(params, "edgeThreshold", 0.12D);
        }
        int targetColor = ScreenVisionUtils.parseColor(getString(params, "targetColor"), -1);
        if (targetColor < 0) {
            return false;
        }
        int averageColor = ((metrics.getAverageR() & 0xFF) << 16) | ((metrics.getAverageG() & 0xFF) << 8)
                | (metrics.getAverageB() & 0xFF);
        return ScreenVisionUtils.colorDistance(targetColor, averageColor) <= getDouble(params, "colorTolerance", 48D);
    }

    private void captureInventorySlot(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String varName = getString(params, "varName");
        if (varName.isEmpty()) {
            varName = "slot";
        }
        String area = getString(params, "slotArea");
        int slotIndex = Math.max(0, getInt(params, "slotIndex", 0));
        ItemStack stack = ItemStack.EMPTY;
        if ("HOTBAR".equalsIgnoreCase(area)) {
            if (slotIndex < mc.player.getInventory().items.size()) {
                stack = mc.player.getInventory().items.get(slotIndex);
            }
        } else if ("ARMOR".equalsIgnoreCase(area)) {
            if (slotIndex < mc.player.getInventory().armor.size()) {
                stack = mc.player.getInventory().armor.get(slotIndex);
            }
        } else if ("OFFHAND".equalsIgnoreCase(area)) {
            if (slotIndex < mc.player.getInventory().offhand.size()) {
                stack = mc.player.getInventory().offhand.get(slotIndex);
            }
        } else if (slotIndex < mc.player.getInventory().items.size()) {
            stack = mc.player.getInventory().items.get(slotIndex);
        }
        writeCapturedStack(varName, stack);
        runtimeVariables.put(varName + "_slot_index", slotIndex);
        runtimeVariables.put(varName + "_slot_area", area == null ? "" : area);
    }

    private void captureHotbar(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String varName = getString(params, "varName");
        if (varName.isEmpty()) {
            varName = "hotbar";
        }
        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<String> registries = new ArrayList<>();
        List<String> nbts = new ArrayList<>();
        int filledCount = 0;
        for (int i = 0; i < 9 && i < mc.player.getInventory().items.size(); i++) {
            ItemStack stack = mc.player.getInventory().items.get(i);
            writeCapturedStack(varName + "_" + i, stack);
            if (stack != null && !stack.isEmpty()) {
                filledCount++;
            }
            names.add(stack == null || stack.isEmpty() ? "" : stack.getHoverName().getString());
            counts.add(stack == null || stack.isEmpty() ? 0 : stack.getCount());
            registries.add(getRegistryName(stack));
            nbts.add(getStackNbtText(stack));
        }
        int selectedIndex = mc.player.getInventory().selected;
        runtimeVariables.put(varName + "_selected_index", selectedIndex);
        runtimeVariables.put(varName + "_selected_slot", selectedIndex);
        runtimeVariables.put(varName + "_filled_count", filledCount);
        runtimeVariables.put(varName + "_names", names);
        runtimeVariables.put(varName + "_counts", counts);
        runtimeVariables.put(varName + "_registries", registries);
        runtimeVariables.put(varName + "_nbts", nbts);
        writeCapturedStack(varName + "_selected", mc.player.getInventory().getSelected());
    }

    private void capturePacketField(JsonObject params) {
        String varName = getString(params, "varName");
        if (varName.isEmpty()) {
            varName = "packet_field";
        }
        PacketFieldRuleManager.CapturedFieldSnapshot snapshot = PacketFieldRuleManager
                .getLatestCapturedField(getString(params, "fieldKey"));
        Object value = snapshot == null ? null : snapshot.getValue();
        runtimeVariables.put(varName, value);
        runtimeVariables.put(varName + "_found", snapshot != null);
        runtimeVariables.put(varName + "_value_text", value == null ? "" : LegacyActionRuntime.stringifyValue(value));
    }

    private void captureGuiElement(JsonObject params) {
        String varName = getString(params, "varName");
        if (varName.isEmpty()) {
            varName = "gui_element";
        }
        String locatorMode = getString(params, "guiElementLocatorMode");
        String locatorText = getString(params, "locatorText");
        String matchMode = getString(params, "locatorMatchMode");
        GuiElementInspector.GuiElementInfo best = "PATH".equalsIgnoreCase(locatorMode)
                ? GuiElementInspector.findFirstByPath(locatorText, matchMode)
                : null;
        runtimeVariables.put(varName + "_found", best != null);
        if (best == null) {
            return;
        }
        runtimeVariables.put(varName + "_type", best.getType().name());
        runtimeVariables.put(varName + "_path", best.getPath());
        runtimeVariables.put(varName + "_text", best.getText());
        runtimeVariables.put(varName + "_x", best.getX());
        runtimeVariables.put(varName + "_y", best.getY());
        runtimeVariables.put(varName + "_width", best.getWidth());
        runtimeVariables.put(varName + "_height", best.getHeight());
    }

    private void captureScreenRegion(JsonObject params) {
        String varName = getString(params, "varName");
        if (varName.isEmpty()) {
            varName = "vision_region";
        }
        int[] rect = parseVisionRegionRect(params);
        if (rect == null) {
            runtimeVariables.put(varName + "_found", false);
            return;
        }
        ScreenVisionUtils.RegionMetrics metrics = ScreenVisionUtils.analyzeRegion(rect[0], rect[1], rect[2], rect[3]);
        runtimeVariables.put(varName + "_found", metrics.isFound());
        runtimeVariables.put(varName + "_avg_hex", metrics.getAverageHex());
        runtimeVariables.put(varName + "_center_hex", metrics.getCenterHex());
        runtimeVariables.put(varName + "_brightness", metrics.getBrightness());
        runtimeVariables.put(varName + "_edge_density", metrics.getEdgeDensity());
    }

    private void captureBlockAt(JsonObject params) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        String varName = getString(params, "varName");
        if (varName.isEmpty()) {
            varName = "block";
        }
        double[] pos = parsePositionFromJson(params.getAsJsonArray("pos"));
        if (pos == null) {
            runtimeVariables.put(varName + "_found", false);
            return;
        }
        BlockPos blockPos = new BlockPos((int) Math.floor(pos[0]), (int) Math.floor(pos[1]), (int) Math.floor(pos[2]));
        BlockState state = mc.level.getBlockState(blockPos);
        runtimeVariables.put(varName + "_found", state != null && !state.isAir());
        if (state == null) {
            return;
        }
        runtimeVariables.put(varName + "_x", blockPos.getX());
        runtimeVariables.put(varName + "_y", blockPos.getY());
        runtimeVariables.put(varName + "_z", blockPos.getZ());
        runtimeVariables.put(varName + "_name", state.getBlock().getName().getString());
        runtimeVariables.put(varName + "_registry", getRegistryName(state));
    }

    private void writeCapturedStack(String varName, ItemStack stack) {
        runtimeVariables.put(varName + "_found", stack != null && !stack.isEmpty());
        if (stack == null || stack.isEmpty()) {
            runtimeVariables.put(varName + "_name", "");
            runtimeVariables.put(varName + "_count", 0);
            runtimeVariables.put(varName + "_registry", "");
            runtimeVariables.put(varName + "_nbt", "");
            return;
        }
        runtimeVariables.put(varName + "_name", stack.getHoverName().getString());
        runtimeVariables.put(varName + "_count", stack.getCount());
        runtimeVariables.put(varName + "_registry", getRegistryName(stack));
        runtimeVariables.put(varName + "_nbt", getStackNbtText(stack));
    }

    private String getRegistryName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return formatRegistryName(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private String getRegistryName(BlockState state) {
        if (state == null || state.isAir()) {
            return "";
        }
        return formatRegistryName(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private String formatRegistryName(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private String getStackNbtText(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getTag() == null ? "" : stack.getTag().toString();
    }

    private int[] parseVisionRegionRect(JsonObject params) {
        if (params == null || !params.has("regionRect")) {
            return null;
        }
        try {
            if (params.get("regionRect").isJsonArray()) {
                JsonArray array = params.getAsJsonArray("regionRect");
                return new int[] { array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt(),
                        array.get(3).getAsInt() };
            }
            return parseIntArray(params.get("regionRect").getAsString(), 4);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double[] parsePosition(String raw) {
        int[] ints = parseIntArray(raw, 3);
        if (ints == null) {
            return null;
        }
        return new double[] { ints[0], ints[1], ints[2] };
    }

    private double[] parsePositionFromJson(JsonArray array) {
        if (array == null || array.size() < 3) {
            return null;
        }
        return new double[] { array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble() };
    }

    private int[] parseIntArray(String raw, int expectedLength) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String normalized = raw.replace("[", "").replace("]", "");
        String[] parts = normalized.split(",");
        if (parts.length < expectedLength) {
            return null;
        }
        int[] values = new int[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            values[i] = (int) Math.round(Double.parseDouble(parts[i].trim()));
        }
        return values;
    }

    private EnumSet<ResourceLockManager.Resource> resolveActionResources(PathSequenceManager.ActionData action) {
        EnumSet<ResourceLockManager.Resource> resources = EnumSet.noneOf(ResourceLockManager.Resource.class);
        String type = action == null ? "" : normalize(action.type);
        switch (type) {
            case "setview":
                resources.add(ResourceLockManager.Resource.LOOK);
                break;
            case "click":
            case "rightclickblock":
            case "rightclickentity":
                resources.add(ResourceLockManager.Resource.INTERACT);
                resources.add(ResourceLockManager.Resource.LOOK);
                break;
            case "window_click":
            case "conditional_window_click":
            case "takeallitems":
            case "take_all_items_safe":
            case "dropfiltereditems":
            case "autochestclick":
            case "move_inventory_items_to_chest_slots":
            case "spread_inventory_item":
            case "warehouse_auto_deposit":
            case "transferitemstowarehouse":
            case "move_inventory_item_to_hotbar":
            case "switch_hotbar_slot":
            case "silentuse":
            case "use_hotbar_item":
            case "use_held_item":
            case "autoeat":
            case "autoequip":
            case "autopickup":
                resources.add(ResourceLockManager.Resource.INVENTORY);
                break;
            case "send_packet":
                resources.add(ResourceLockManager.Resource.PACKET);
                break;
            case "wait_until_inventory_item":
            case "wait_until_gui_title":
            case "wait_until_player_in_area":
            case "wait_until_entity_nearby":
            case "wait_until_hud_text":
            case "wait_until_expression":
            case "wait_until_captured_id":
            case "wait_until_packet_text":
            case "wait_until_screen_region":
            case "wait_combined":
                resources.add(ResourceLockManager.Resource.WAIT);
                break;
            case "hunt":
            case "follow_entity":
                resources.add(ResourceLockManager.Resource.MOVE);
                resources.add(ResourceLockManager.Resource.COMBAT);
                resources.add(ResourceLockManager.Resource.LOOK);
                break;
            default:
                break;
        }
        return resources;
    }

    private boolean ensureResources(EnumSet<ResourceLockManager.Resource> resources, String detail) {
        if (resources == null || resources.isEmpty()) {
            return true;
        }
        String blocked = ResourceLockManager.acquireOrSync(lockOwnerId,
                currentSequence == null ? "" : currentSequence.getName(), backgroundRunner, resources, detail);
        return blocked == null || blocked.isEmpty();
    }

    private void releaseResources() {
        ResourceLockManager.releaseAll(lockOwnerId);
    }

    private void ensureRegistered() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(this);
            registered = true;
        }
    }

    private void ensureUnregistered() {
        if (registered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = false;
        }
    }

    private void runWithExecutionContext(Runnable runnable) {
        PathSequenceEventListener previous = ACTION_EXECUTION_CONTEXT.get();
        ACTION_EXECUTION_CONTEXT.set(this);
        try {
            runnable.run();
        } finally {
            if (previous == null) {
                ACTION_EXECUTION_CONTEXT.remove();
            } else {
                ACTION_EXECUTION_CONTEXT.set(previous);
            }
        }
    }

    private void recordDebugTrace(String message) {
        recordDebugTrace(message, stepIndex, actionIndex);
    }

    private void recordDebugTrace(String message, int logStepIndex, int logActionIndex) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String trimmed = message.trim();
        debugTraceLines.addLast(trimmed);
        while (debugTraceLines.size() > 14) {
            debugTraceLines.removeFirst();
        }
        appendExecutionLogEvent("trace", trimmed, logStepIndex, logActionIndex);
    }

    private void appendExecutionLogEvent(String type, String message) {
        appendExecutionLogEvent(type, message, stepIndex, actionIndex);
    }

    private void appendExecutionLogEvent(String type, String message, int logStepIndex, int logActionIndex) {
        if (executionLogSessionId == null || executionLogSessionId.trim().isEmpty()) {
            return;
        }
        ExecutionLogManager.appendEvent(executionLogSessionId, type, logStepIndex, logActionIndex, message, status,
                buildExecutionLogVariablePreview(EXECUTION_LOG_VARIABLE_PREVIEW_LIMIT));
    }

    private void resetExecutionResultState() {
        this.executionResultSuccess = false;
        this.executionResultReason = "";
    }

    private void markExecutionResult(boolean success, String reason) {
        this.executionResultSuccess = success;
        this.executionResultReason = reason == null ? "" : reason;
    }

    private void startExecutionLogSession() {
        if (currentSequence == null) {
            executionLogSessionId = "";
            return;
        }
        executionLogSessionId = ExecutionLogManager.startSession(currentSequence.getName(), backgroundRunner,
                buildExecutionLogVariablePreview(EXECUTION_LOG_VARIABLE_PREVIEW_LIMIT));
        resetExecutionResultState();
        ExecutionLogManager.appendEvent(executionLogSessionId, "start", stepIndex, actionIndex,
                "执行开始: " + currentSequence.getName(), status,
                buildExecutionLogVariablePreview(EXECUTION_LOG_VARIABLE_PREVIEW_LIMIT));
    }

    private void finishExecutionLogSessionIfNeeded() {
        if (executionLogSessionId == null || executionLogSessionId.trim().isEmpty()) {
            return;
        }
        String finishReason = executionResultReason == null || executionResultReason.trim().isEmpty()
                ? (executionResultSuccess ? "执行完成" : "执行停止")
                : executionResultReason;
        ExecutionLogManager.finishSession(executionLogSessionId, executionResultSuccess, finishReason, status,
                buildExecutionLogVariablePreview(EXECUTION_LOG_VARIABLE_PREVIEW_LIMIT));
        executionLogSessionId = "";
    }

    private Map<String, Object> buildVariablePreview(int limit) {
        Map<String, Object> preview = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : runtimeVariables.getCanonicalSnapshot().entrySet()) {
            preview.put(entry.getKey(), entry.getValue());
            count++;
            if (count >= Math.max(1, limit)) {
                break;
            }
        }
        return preview;
    }

    private Map<String, String> buildExecutionLogVariablePreview(int limit) {
        Map<String, String> preview = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : runtimeVariables.getCanonicalSnapshot().entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            preview.put(entry.getKey(), LegacyActionRuntime.stringifyValue(entry.getValue()));
            count++;
            if (count >= Math.max(1, limit)) {
                break;
            }
        }
        return preview;
    }

    private void resetStepPathRetryMonitor() {
        currentStepIdleTicks = 0;
        currentStepIdleAnnounced = false;
        currentStepLastMovementX = Double.NaN;
        currentStepLastMovementY = Double.NaN;
        currentStepLastMovementZ = Double.NaN;
    }

    private void resetStepArrivalWaitState() {
        waitingForNavigationToFinishAtTarget = false;
    }

    private void initializeStepPathRetryMonitor(LocalPlayer player) {
        resetStepPathRetryMonitor();
        if (player == null) {
            return;
        }
        currentStepLastMovementX = player.getX();
        currentStepLastMovementY = player.getY();
        currentStepLastMovementZ = player.getZ();
    }

    private boolean handleCurrentStepPathRetry(LocalPlayer player, PathSequenceManager.PathStep step) {
        if (player == null || step == null || !step.hasGotoTarget()) {
            resetStepPathRetryMonitor();
            return false;
        }

        int retryCount = step.getRetryCount();
        int timeoutSeconds = step.getPathRetryTimeoutSeconds();
        if (retryCount <= 0 || timeoutSeconds <= 0) {
            initializeStepPathRetryMonitor(player);
            return false;
        }

        if (Double.isNaN(currentStepLastMovementX)) {
            initializeStepPathRetryMonitor(player);
        }

        double dx = player.getX() - currentStepLastMovementX;
        double dy = player.getY() - currentStepLastMovementY;
        double dz = player.getZ() - currentStepLastMovementZ;
        double movedSq = dx * dx + dy * dy + dz * dz;
        if (movedSq > PATH_RETRY_MOVEMENT_EPSILON_SQ) {
            boolean wasIdleAnnounced = currentStepIdleAnnounced;
            initializeStepPathRetryMonitor(player);
            if (wasIdleAnnounced) {
                restoreSequenceBaseStatus();
            }
            return false;
        }

        currentStepIdleTicks++;
        if (!currentStepIdleAnnounced && currentStepIdleTicks >= PATH_RETRY_NOTIFY_TICKS) {
            int remainingRetries = Math.max(0, retryCount - stepRetryUsed);
            sendPathRetryMessage("步骤 " + (stepIndex + 1) + " 检测到人物原地不动，" + timeoutSeconds
                    + " 秒后将自动重发寻路命令，剩余重试 " + remainingRetries + " 次。", ChatFormatting.YELLOW);
            currentStepIdleAnnounced = true;
            setStatus((currentSequence == null ? "" : currentSequence.getName())
                    + (backgroundRunner ? " | 后台执行" : "") + " | 寻路停留检测中");
        }

        if (currentStepIdleTicks < timeoutSeconds * 20) {
            return false;
        }

        releaseResources();
        waitConditionRunning = false;
        waitConditionElapsedTicks = 0;
        repeatActionRunning = false;
        pendingAsyncActionType = "";
        pendingAsyncStepIndex = -1;
        pendingAsyncActionIndex = -1;

        if (stepRetryUsed < retryCount) {
            stepRetryUsed++;
            actionIndex = 0;
            currentActionDescription = "";
            sendPathRetryMessage("步骤 " + (stepIndex + 1) + " 寻路停留超时，重新发送寻路命令。剩余重试 "
                    + Math.max(0, retryCount - stepRetryUsed) + " 次。", ChatFormatting.GOLD);
            recordDebugTrace("步骤寻路重试: step=" + stepIndex + ", retry=" + stepRetryUsed + "/" + retryCount);
            restartCurrentStepTarget();
            setStatus((currentSequence == null ? "" : currentSequence.getName())
                    + (backgroundRunner ? " | 后台执行" : "") + " | 寻路重试 " + stepRetryUsed + "/" + retryCount);
            tickDelay = 1;
            return true;
        }

        stepRetryUsed = 0;
        recordDebugTrace("步骤寻路重试耗尽: step=" + stepIndex);
        String exhaustedPolicy = step.getRetryExhaustedPolicy();
        if ("RESTART_SEQUENCE".equalsIgnoreCase(exhaustedPolicy)) {
            sendPathRetryMessage("步骤 " + (stepIndex + 1) + " 寻路重试已耗尽，按设置从序列开头重新执行。",
                    ChatFormatting.RED);
            return restartSequenceFromBeginning("path_retry_exhausted_restart_sequence");
        }
        if ("RUN_SEQUENCE".equalsIgnoreCase(exhaustedPolicy)) {
            return runRetryExhaustedSequence(player, step);
        }

        sendPathRetryMessage("步骤 " + (stepIndex + 1) + " 寻路重试已耗尽，序列已停止。", ChatFormatting.RED);
        markExecutionResult(false, "path_retry_exhausted");
        stopTrackingAndStopNavigation("路径序列", "路径序列寻路重试耗尽并按停止策略结束");
        return true;
    }

    private boolean restartSequenceFromBeginning(String reason) {
        if (currentSequence == null || currentSequence.getSteps() == null || currentSequence.getSteps().isEmpty()) {
            markExecutionResult(false, reason == null ? "seek_retry_restart_failed" : reason);
            stopTrackingAndStopNavigation("路径序列", "路径序列无法从头重试，停止并清理导航");
            return true;
        }

        stepIndex = 0;
        actionIndex = 0;
        atTarget = false;
        tickDelay = 1;
        explicitDelay = false;
        stepRetryUsed = 0;
        waitConditionRunning = false;
        waitConditionElapsedTicks = 0;
        repeatActionRunning = false;
        pendingAsyncActionType = "";
        pendingAsyncStepIndex = -1;
        pendingAsyncActionIndex = -1;
        currentActionDescription = "";
        resetStepPathRetryMonitor();
        resetStepArrivalWaitState();
        runtimeVariables.clear();
        for (Map.Entry<String, Object> entry : initialSequenceVariables.entrySet()) {
            if (entry != null && entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                runtimeVariables.putSequence(entry.getKey().trim(), entry.getValue());
            }
        }
        runtimeVariables.enterStep(stepIndex);
        recordDebugTrace("序列从头重试: " + (reason == null ? "unknown" : reason));
        restartCurrentStepTarget();
        setStatus(currentSequence.getName() + (backgroundRunner ? " | 后台执行" : "") + " | 寻路重试已回到开头");
        return true;
    }

    private boolean runRetryExhaustedSequence(LocalPlayer player, PathSequenceManager.PathStep step) {
        String callerSequenceName = currentSequence == null ? "" : currentSequence.getName();
        String targetSequenceName = step == null ? "" : step.getRetryExhaustedSequenceName();
        if (targetSequenceName == null || targetSequenceName.trim().isEmpty()) {
            sendPathRetryMessage("步骤 " + (stepIndex + 1) + " 寻路重试已耗尽，但未配置失败后执行序列，序列已停止。",
                    ChatFormatting.RED);
            markExecutionResult(false, "寻路重试耗尽，但未配置失败后执行序列");
            stopTrackingAndStopNavigation("路径序列", "路径序列重试耗尽且未配置后续序列，停止并清理导航");
            return true;
        }

        String target = targetSequenceName.trim();
        sendPathRetryMessage("步骤 " + (stepIndex + 1) + " 寻路重试已耗尽，按设置执行序列: " + target + "。",
                ChatFormatting.RED);
        recordDebugTrace("步骤寻路重试耗尽后执行序列: step=" + stepIndex + ", target=" + target);
        markExecutionResult(false, "寻路重试耗尽，转执行序列: " + target);
        stopTrackingAndStopNavigation("路径序列", "路径序列重试耗尽，切换执行后续序列前清理导航");

        boolean started = PathSequenceManager.executeSequenceByConfiguredMode(target, player, callerSequenceName, null);
        if (!started && player != null) {
            player.sendSystemMessage(new TextComponentString("§c[路径序列] 启动失败后执行序列失败: " + target));
        }
        return true;
    }

    private void logSequenceStop(String source, String reason) {
        if (!tracking || currentSequence == null) {
            return;
        }
        List<PathSequenceManager.PathStep> steps = currentSequence.getSteps();
        PathSequenceManager.PathStep currentStep = null;
        int totalSteps = steps == null ? 0 : steps.size();
        if (steps != null && stepIndex >= 0 && stepIndex < steps.size()) {
            currentStep = steps.get(stepIndex);
        }
        int currentActionTotal = currentStep == null || currentStep.getActions() == null ? 0 : currentStep.getActions().size();
        boolean externalTermination = currentStep != null && currentStep.hasGotoTarget() && !atTarget;
        int stepDisplay = totalSteps <= 0 ? 0 : Math.min(stepIndex + 1, totalSteps);
        int actionDisplay = currentActionTotal <= 0 ? 0 : Math.min(actionIndex + 1, currentActionTotal);
        String currentAction = currentActionDescription == null || currentActionDescription.trim().isEmpty()
                ? "<none>"
                : currentActionDescription.trim();
        zszlScriptMod.LOGGER.info(
                "[路径序列] 停止{} sequence={} source={} reason={} step={}/{} action={}/{} atTarget={} externalTermination={} remainingLoops={} background={} currentAction={}",
                backgroundRunner ? "后台序列" : "前台序列",
                currentSequence.getName(),
                normalizeStopSource(source),
                normalizeStopReason(reason),
                stepDisplay,
                totalSteps,
                actionDisplay,
                currentActionTotal,
                atTarget,
                externalTermination,
                remainingLoops,
                backgroundRunner,
                currentAction);
    }

    private String normalizeStopSource(String source) {
        return source == null || source.trim().isEmpty() ? "未知来源" : source.trim();
    }

    private String normalizeStopReason(String reason) {
        return reason == null || reason.trim().isEmpty() ? "未提供停止原因" : reason.trim();
    }

    private void sendPathRetryMessage(String message, ChatFormatting color) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || message == null || message.trim().isEmpty()) {
            return;
        }
        player.sendSystemMessage(new TextComponentString(color + "[路径序列] " + message));
    }

    private void ensureSetVarDefaultValue(String varName, JsonObject rawParams, LocalPlayer player) {
        if (varName == null || varName.trim().isEmpty() || rawParams == null) {
            return;
        }
        if (runtimeVariables.containsInDeclaredScope(varName)) {
            return;
        }
        runtimeVariables.put(varName, LegacyActionRuntime.inferAssignedValueDefault(rawParams));
    }

    private void restoreSequenceBaseStatus() {
        if (currentSequence == null) {
            return;
        }
        String suffix = backgroundRunner ? " | 后台执行" : "";
        if (status == null || status.trim().isEmpty()) {
            setStatus(currentSequence.getName() + suffix);
            return;
        }
        int splitIndex = status.indexOf(" | ");
        if (splitIndex < 0) {
            return;
        }
        String base = status.substring(0, splitIndex).trim();
        if (base.isEmpty()) {
            base = currentSequence.getName();
        }
        setStatus(base + suffix);
    }

    private void executeHuntTick(LocalPlayer player) {
        if (!tracking || !isHunting || player == null || player.level() == null) {
            completeHuntAction();
            return;
        }

        if (huntAttackCooldownTicks > 0) {
            huntAttackCooldownTicks--;
        }
        if (huntChaseCooldownTicks > 0) {
            huntChaseCooldownTicks--;
        }
        tickHuntNoDamageAttackTrackers(player);

        if (huntTargetEntity != null && isHuntTargetKilled(huntTargetEntity)) {
            recordHuntTargetKill(huntTargetEntity);
            huntTargetEntity = null;
        }

        if (huntTargetEntity != null && !isLockedHuntTargetStillTrackable(player, huntTargetEntity)) {
            if (!huntPendingCompleteAfterSequence && huntAttackSequenceExecutor.isRunning()) {
                huntAttackSequenceExecutor.stop();
            }
            clearCurrentHuntTarget();
            stopHuntNavigationMode();
        }

        if (huntTargetEntity == null) {
            if (huntPendingCompleteAfterSequence) {
                if (huntAttackSequenceExecutor.isRunning()) {
                    huntAttackSequenceExecutor.tick(player);
                    if (huntPendingCompleteAfterSequence && !huntAttackSequenceExecutor.isRunning()) {
                        completeHuntAction();
                    }
                    return;
                }
                completeHuntAction();
                return;
            }
            if (huntAttackSequenceExecutor.isRunning()) {
                huntAttackSequenceExecutor.stop();
            }
            LivingEntity nextTarget = findNextHuntTarget(player);
            if (nextTarget == null) {
                completeHuntAction(huntNoTargetSkipCount);
                return;
            }
            huntTargetEntity = nextTarget;
            resetHuntNavigationCache();
            huntOrbitController.stop();
            zszlScriptMod.LOGGER.info("[路径序列] 中心搜怪锁定目标: {}", getHuntFilterableEntityName(huntTargetEntity));
        }

        LivingEntity target = huntTargetEntity;
        if (target == null) {
            return;
        }

        double distance = player.distanceTo(target);
        if (huntAutoAttack) {
            tickHuntAttackSequenceExecutor(player);
            if (huntPendingCompleteAfterSequence && !huntAttackSequenceExecutor.isRunning()) {
                completeHuntAction();
                return;
            }
            if (huntAimLockEnabled && !isHuntOrbitMode()) {
                rotatePlayerTowardHuntTarget(player, target);
            }
            if (tryHuntAttack(player, target) && huntAttackRemaining > 0) {
                huntAttackRemaining--;
                if (huntAttackRemaining <= 0) {
                    if (isHuntSequenceAttackMode() && huntAttackSequenceExecutor.isRunning()) {
                        huntPendingCompleteAfterSequence = true;
                    } else {
                        completeHuntAction();
                        return;
                    }
                }
            }
            if (huntPendingCompleteAfterSequence && !huntAttackSequenceExecutor.isRunning()) {
                completeHuntAction();
                return;
            }
        }

        boolean withinDesiredDistance = isWithinHuntDesiredDistance(distance);
        boolean shouldRunMovement = shouldRunHuntMovementForDistance(distance);
        if (huntChaseIntervalEnabled && withinDesiredDistance && !huntWasWithinDesiredDistance) {
            huntChaseCooldownTicks = huntChaseIntervalTicks;
        }
        huntWasWithinDesiredDistance = withinDesiredDistance;

        if (huntChaseIntervalEnabled && huntChaseCooldownTicks > 0) {
            stopHuntNavigationMode();
            return;
        }
        if (!shouldRunMovement) {
            stopHuntNavigationMode();
            return;
        }

        huntMovementStopped = false;
        if (shouldUseOrbitMovement(player, target, distance)) {
            stopHuntEmbeddedNavigation();
            huntOrbitController.tick(player, target,
                    new HuntOrbitController.OrbitConfig(getHuntDesiredDistance(),
                            HUNT_FIXED_DISTANCE_TOLERANCE, true, true, true));
            return;
        }

        huntOrbitController.stop();
        navigateHuntTowardsTarget(player, target);
    }

    private boolean isHuntNoDamageExclusionEnabled() {
        return huntNoDamageAttackLimit > 0;
    }

    private boolean isHuntNoDamageExcludedTarget(Entity entity) {
        return isHuntNoDamageExclusionEnabled()
                && entity != null
                && huntNoDamageExcludedEntityIds.contains(entity.getId());
    }

    private void tickHuntNoDamageAttackTrackers(LocalPlayer player) {
        if (!isHuntNoDamageExclusionEnabled() || player == null || player.level() == null) {
            huntNoDamageAttackTrackers.clear();
            huntNoDamageExcludedEntityIds.clear();
            return;
        }

        List<Integer> trackerIds = new ArrayList<>(huntNoDamageAttackTrackers.keySet());
        for (Integer entityId : trackerIds) {
            if (entityId == null) {
                continue;
            }
            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof LivingEntity)) {
                huntNoDamageAttackTrackers.remove(entityId);
                huntNoDamageExcludedEntityIds.remove(entityId);
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            if (!living.isAlive() || living.isRemoved() || living.getHealth() <= 0.0F) {
                huntNoDamageAttackTrackers.remove(entityId);
                huntNoDamageExcludedEntityIds.remove(entityId);
                continue;
            }
            HuntNoDamageAttackTracker tracker = huntNoDamageAttackTrackers.get(entityId);
            if (tracker != null) {
                updateHuntNoDamageAttackTracker(entityId, living, tracker);
            }
        }
        pruneHuntNoDamageExcludedTargets(player);
    }

    private void updateHuntNoDamageAttackTracker(int entityId, LivingEntity target,
            HuntNoDamageAttackTracker tracker) {
        float currentHealth = target.getHealth();
        if (currentHealth + HUNT_NO_DAMAGE_HEALTH_EPSILON < tracker.baselineHealth) {
            tracker.baselineHealth = currentHealth;
            tracker.pendingAttempts = 0;
            tracker.observationTicks = 0;
            tracker.confirmedNoDamageAttempts = 0;
            return;
        }

        if (tracker.pendingAttempts <= 0) {
            tracker.baselineHealth = currentHealth;
            return;
        }
        if (tracker.observationTicks > 0) {
            tracker.observationTicks--;
            return;
        }

        tracker.confirmedNoDamageAttempts += tracker.pendingAttempts;
        tracker.pendingAttempts = 0;
        tracker.baselineHealth = currentHealth;
        if (tracker.confirmedNoDamageAttempts >= huntNoDamageAttackLimit) {
            excludeHuntNoDamageTarget(entityId, target);
        }
    }

    private void recordHuntNoDamageAttackAttempt(LivingEntity target) {
        if (!isHuntNoDamageExclusionEnabled() || target == null || target.getHealth() <= 0.0F) {
            return;
        }
        int entityId = target.getId();
        if (huntNoDamageExcludedEntityIds.contains(entityId)) {
            return;
        }

        float currentHealth = target.getHealth();
        HuntNoDamageAttackTracker tracker = huntNoDamageAttackTrackers.get(entityId);
        if (tracker == null) {
            tracker = new HuntNoDamageAttackTracker(currentHealth);
            huntNoDamageAttackTrackers.put(entityId, tracker);
        } else if (currentHealth + HUNT_NO_DAMAGE_HEALTH_EPSILON < tracker.baselineHealth) {
            tracker.baselineHealth = currentHealth;
            tracker.pendingAttempts = 0;
            tracker.observationTicks = 0;
            tracker.confirmedNoDamageAttempts = 0;
        }
        if (tracker.pendingAttempts <= 0) {
            tracker.baselineHealth = currentHealth;
            tracker.observationTicks = HUNT_NO_DAMAGE_OBSERVATION_DELAY_TICKS;
        }
        tracker.pendingAttempts++;
        pruneHuntNoDamageTrackingSize();
    }

    private void excludeHuntNoDamageTarget(int entityId, LivingEntity target) {
        if (!huntNoDamageExcludedEntityIds.add(entityId)) {
            return;
        }
        huntNoDamageAttackTrackers.remove(entityId);
        pruneHuntNoDamageTrackingSize();
        if (huntTargetEntity != null && huntTargetEntity.getId() == entityId) {
            clearHuntTargetLock(true);
        }
        String targetName = target == null ? "" : getHuntFilterableEntityName(target);
        recordDebugTrace("hunt no-damage exclude -> id=" + entityId + ", name=" + targetName
                + ", limit=" + huntNoDamageAttackLimit);
    }

    private void clearHuntTargetLock(boolean stopSequence) {
        if (stopSequence && !huntPendingCompleteAfterSequence && huntAttackSequenceExecutor.isRunning()) {
            huntAttackSequenceExecutor.stop();
        }
        huntTargetEntity = null;
        lastHuntGotoTargetEntityId = Integer.MIN_VALUE;
        huntMovementStopped = false;
        huntWasWithinDesiredDistance = false;
        lastHuntGotoTargetX = Double.NaN;
        lastHuntGotoTargetY = Double.NaN;
        lastHuntGotoTargetZ = Double.NaN;
        huntOrbitLoopNodeIndex = -1;
        huntLastOrbitGotoTick = -99999;
        huntOrbitStuckTicks = 0;
        huntLastOrbitPlayerX = Double.NaN;
        huntLastOrbitPlayerZ = Double.NaN;
        huntOrbitController.stop();
        stopHuntNavigationMode();
    }

    private void pruneHuntNoDamageExcludedTargets(LocalPlayer player) {
        if (player == null || player.level() == null || huntNoDamageExcludedEntityIds.isEmpty()) {
            return;
        }
        List<Integer> excludedIds = new ArrayList<>(huntNoDamageExcludedEntityIds);
        for (Integer entityId : excludedIds) {
            if (entityId == null) {
                continue;
            }
            Entity entity = player.level().getEntity(entityId);
            if (!(entity instanceof LivingEntity)) {
                huntNoDamageExcludedEntityIds.remove(entityId);
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            if (!living.isAlive() || living.isRemoved() || living.getHealth() <= 0.0F) {
                huntNoDamageExcludedEntityIds.remove(entityId);
            }
        }
    }

    private void pruneHuntNoDamageTrackingSize() {
        while (huntNoDamageAttackTrackers.size() > HUNT_NO_DAMAGE_MAX_TRACKED_TARGETS) {
            Integer first = huntNoDamageAttackTrackers.keySet().iterator().next();
            huntNoDamageAttackTrackers.remove(first);
        }
        while (huntNoDamageExcludedEntityIds.size() > HUNT_NO_DAMAGE_MAX_EXCLUDED_TARGETS) {
            Integer first = huntNoDamageExcludedEntityIds.iterator().next();
            huntNoDamageExcludedEntityIds.remove(first);
        }
    }

    private void completeHuntAction() {
        completeHuntAction(0);
    }

    private void completeHuntAction(int additionalSkipCount) {
        stopHuntNavigationMode();
        resetHuntState();
        actionIndex += Math.max(0, additionalSkipCount) + 1;
        applyBuiltinSequenceDelay();
        restoreSequenceBaseStatus();
    }

    private void resetHuntState() {
        huntAttackSequenceExecutor.stop();
        huntOrbitController.stop();
        isHunting = false;
        huntRadius = 0.0D;
        huntAutoAttack = false;
        huntAttackMode = KillAuraHandler.ATTACK_MODE_NORMAL;
        huntAttackSequenceName = "";
        huntAimLockEnabled = true;
        huntTrackingDistanceSq = 0.0D;
        huntUpRange = KillAuraHandler.DEFAULT_HUNT_UP_RANGE;
        huntDownRange = KillAuraHandler.DEFAULT_HUNT_DOWN_RANGE;
        huntTargetEntity = null;
        huntMovementStopped = false;
        huntMode = KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
        huntOrbitEnabled = false;
        huntChaseIntervalEnabled = false;
        huntChaseIntervalTicks = 0;
        huntChaseCooldownTicks = 0;
        huntWasWithinDesiredDistance = false;
        huntAttackCooldownTicks = 0;
        huntAttackRemaining = -1;
        huntNoTargetSkipCount = 0;
        huntNoDamageAttackLimit = KillAuraHandler.DEFAULT_NO_DAMAGE_ATTACK_LIMIT;
        huntRestrictTargetGroups = true;
        huntTargetHostile = true;
        huntTargetPassive = false;
        huntTargetPlayers = false;
        huntEnableNameWhitelist = false;
        huntEnableNameBlacklist = false;
        huntNameWhitelist.clear();
        huntWhitelistTargets.clear();
        huntWhitelistKillProgress.clear();
        countedHuntKillEntityIds.clear();
        huntNoDamageAttackTrackers.clear();
        huntNoDamageExcludedEntityIds.clear();
        huntNameBlacklist.clear();
        huntShowRange = false;
        huntIgnoreInvisible = false;
        huntCenterX = 0.0D;
        huntCenterY = 0.0D;
        huntCenterZ = 0.0D;
        huntPendingCompleteAfterSequence = false;
        resetHuntNavigationCache();
    }

    private void resetHuntNavigationCache() {
        lastHuntGotoTargetEntityId = Integer.MIN_VALUE;
        lastHuntGotoTargetX = Double.NaN;
        lastHuntGotoTargetY = Double.NaN;
        lastHuntGotoTargetZ = Double.NaN;
        huntMovementStopped = false;
        huntWasWithinDesiredDistance = false;
    }

    private void clearCurrentHuntTarget() {
        huntTargetEntity = null;
        resetHuntNavigationCache();
    }

    private String normalizeHuntAttackMode(String mode) {
        return KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(mode)
                ? KillAuraHandler.ATTACK_MODE_SEQUENCE
                : KillAuraHandler.ATTACK_MODE_NORMAL;
    }

    private boolean isHuntSequenceAttackMode() {
        return KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(huntAttackMode);
    }

    private boolean isHuntFixedDistanceMode() {
        return KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equalsIgnoreCase(huntMode);
    }

    private boolean isHuntOrbitMode() {
        return isHuntFixedDistanceMode() && huntOrbitEnabled;
    }

    private String readHuntModeParam(JsonObject params, String key, String defaultValue) {
        String mode = getString(params, key);
        if (KillAuraHandler.HUNT_MODE_APPROACH.equalsIgnoreCase(mode)) {
            return KillAuraHandler.HUNT_MODE_APPROACH;
        }
        if (KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equalsIgnoreCase(mode)) {
            return KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
        }
        return defaultValue;
    }

    private double getHuntDesiredDistance() {
        return Math.max(0.5D, Math.sqrt(Math.max(0.0D, huntTrackingDistanceSq)));
    }

    private boolean isWithinHuntDesiredDistance(double distance) {
        if (isHuntFixedDistanceMode()) {
            return Math.abs(distance - getHuntDesiredDistance()) <= HUNT_FIXED_DISTANCE_TOLERANCE;
        }
        return distance <= getHuntDesiredDistance();
    }

    private boolean shouldRunHuntMovementForDistance(double distance) {
        if (isHuntFixedDistanceMode()) {
            if (isHuntOrbitMode()) {
                return true;
            }
            return Math.abs(distance - getHuntDesiredDistance()) > HUNT_FIXED_DISTANCE_TOLERANCE;
        }
        return distance > getHuntDesiredDistance();
    }

    private boolean shouldUseOrbitMovement(LocalPlayer player, LivingEntity target, double distance) {
        if (!isHuntOrbitMode() || player == null || target == null) {
            return false;
        }
        if (player.isFallFlying() || player.getAbilities().flying) {
            return false;
        }
        if (Math.abs(player.getY() - target.getY()) > HUNT_ORBIT_MAX_VERTICAL_DELTA) {
            return false;
        }
        double entryDistance = Math.max(getHuntDesiredDistance() + HUNT_ORBIT_ENTRY_BUFFER,
                KillAuraHandler.attackRange + 0.9D);
        return distance <= entryDistance;
    }

    private void rotatePlayerTowardHuntTarget(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return;
        }
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double horizontalDistance = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(
                target.getY() + target.getEyeHeight() * 0.85D - (player.getY() + player.getEyeHeight()),
                horizontalDistance));
        ModUtils.setPlayerViewAngles(player, yaw, pitch);
    }

    private void tickHuntAttackSequenceExecutor(LocalPlayer player) {
        if (!isHuntSequenceAttackMode()) {
            if (huntAttackSequenceExecutor.isRunning()) {
                huntAttackSequenceExecutor.stop();
            }
            return;
        }
        huntAttackSequenceExecutor.tick(player);
    }

    private boolean tryTriggerHuntAttackSequence(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null || huntAttackCooldownTicks > 0 || huntAttackSequenceExecutor.isRunning()) {
            return false;
        }
        String sequenceName = huntAttackSequenceName == null ? "" : huntAttackSequenceName.trim();
        if (sequenceName.isEmpty()) {
            return false;
        }
        PathSequenceManager.PathSequence configuredSequence = PathSequenceManager.getSequence(sequenceName);
        if (configuredSequence == null || configuredSequence.getSteps() == null || configuredSequence.getSteps().isEmpty()) {
            return false;
        }
        huntAttackSequenceExecutor.start(configuredSequence, player, target);
        if (!huntAttackSequenceExecutor.isRunning()) {
            return false;
        }
        huntAttackCooldownTicks = Math.max(0, KillAuraHandler.attackSequenceDelayTicks);
        return true;
    }

    private boolean tryHuntAttack(LocalPlayer player, LivingEntity target) {
        if (isHuntSequenceAttackMode()) {
            boolean triggered = tryTriggerHuntAttackSequence(player, target);
            if (triggered) {
                recordHuntNoDamageAttackAttempt(target);
            }
            return triggered;
        }
        if (!canAttackHuntTarget(player, target)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return false;
        }
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        recordHuntNoDamageAttackAttempt(target);
        huntAttackCooldownTicks = Math.max(0, KillAuraHandler.minAttackIntervalTicks);
        return true;
    }

    private boolean canAttackHuntTarget(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null || huntAttackCooldownTicks > 0) {
            return false;
        }
        if (player.distanceTo(target) > KillAuraHandler.attackRange) {
            return false;
        }
        if (KillAuraHandler.requireLineOfSight && !player.hasLineOfSight(target)) {
            return false;
        }
        return player.getAttackStrengthScale(0.0F) >= KillAuraHandler.minAttackStrength;
    }

    private void stopHuntNavigationMode() {
        huntOrbitController.stop();
        stopHuntEmbeddedNavigation();
    }

    private void stopHuntEmbeddedNavigation() {
        boolean hadEmbeddedGoal = lastHuntGotoTargetEntityId != Integer.MIN_VALUE
                || !Double.isNaN(lastHuntGotoTargetX)
                || !Double.isNaN(lastHuntGotoTargetY)
                || !Double.isNaN(lastHuntGotoTargetZ);
        if (hadEmbeddedGoal) {
            EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                    "中心搜怪击杀停止追击导航");
        }
        huntMovementStopped = true;
        lastHuntGotoTargetEntityId = Integer.MIN_VALUE;
        lastHuntGotoTargetX = Double.NaN;
        lastHuntGotoTargetY = Double.NaN;
        lastHuntGotoTargetZ = Double.NaN;
    }

    private void navigateHuntTowardsTarget(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return;
        }
        if (isHuntFixedDistanceMode()) {
            double[] destination = computeFixedDistanceHuntDestination(player, target, getHuntDesiredDistance());
            double[] safeDestination = findSafeHuntNavigationDestination(destination[0], destination[1], destination[2]);
            double[] finalDestination = safeDestination == null ? destination : safeDestination;
            EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                    finalDestination[0], finalDestination[1], finalDestination[2], false, "中心搜怪固定距离追击");
            lastHuntGotoTargetEntityId = target.getId();
            lastHuntGotoTargetX = destination[0];
            lastHuntGotoTargetY = destination[1];
            lastHuntGotoTargetZ = destination[2];
            return;
        }
        if (shouldRefreshHuntGoto(target)) {
            EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.PATH_SEQUENCE,
                    target.getX(), target.getY(), target.getZ(), false, "中心搜怪靠近目标追击");
            lastHuntGotoTargetEntityId = target.getId();
            lastHuntGotoTargetX = target.getX();
            lastHuntGotoTargetY = target.getY();
            lastHuntGotoTargetZ = target.getZ();
        }
    }

    private double[] computeFixedDistanceHuntDestination(LocalPlayer player, LivingEntity target, double desiredDistance) {
        double dx = player.getX() - target.getX();
        double dy = player.getY() - target.getY();
        double dz = player.getZ() - target.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= 1.0E-4D) {
            double yawRadians = Math.toRadians(player.getYRot());
            dx = -Math.sin(yawRadians);
            dy = 0.0D;
            dz = Math.cos(yawRadians);
            distance = Math.sqrt(dx * dx + dz * dz);
        }
        double scale = desiredDistance / Math.max(distance, 1.0E-4D);
        double destinationX = target.getX() + dx * scale;
        double destinationY = target.getY() + dy * scale;
        double destinationZ = target.getZ() + dz * scale;
        return clipHuntDestinationXZ(target.getX(), target.getZ(), destinationX, destinationY, destinationZ);
    }

    private double[] clipHuntDestinationXZ(double centerX, double centerZ, double destinationX, double destinationY,
            double destinationZ) {
        if (!AutoFollowHandler.hasActiveLockChaseRestriction()
                || AutoFollowHandler.isPositionWithinActiveLockChaseBounds(destinationX, destinationZ)) {
            return new double[] { destinationX, destinationY, destinationZ };
        }
        double dx = destinationX - centerX;
        double dz = destinationZ - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 1.0E-4D) {
            return new double[] { centerX, destinationY, centerZ };
        }
        double[] clipped = getClippedHuntPoint(centerX, centerZ, distance, Math.atan2(dz, dx));
        return new double[] { clipped[0], destinationY, clipped[1] };
    }

    private double[] getClippedHuntPoint(double centerX, double centerZ, double radius, double angle) {
        double dirX = Math.cos(angle);
        double dirZ = Math.sin(angle);
        double endX = centerX + dirX * radius;
        double endZ = centerZ + dirZ * radius;
        if (!AutoFollowHandler.hasActiveLockChaseRestriction()
                || AutoFollowHandler.isPositionWithinActiveLockChaseBounds(endX, endZ)) {
            return new double[] { endX, endZ };
        }
        double low = 0.0D;
        double high = radius;
        for (int i = 0; i < 14; i++) {
            double mid = (low + high) * 0.5D;
            double testX = centerX + dirX * mid;
            double testZ = centerZ + dirZ * mid;
            if (AutoFollowHandler.isPositionWithinActiveLockChaseBounds(testX, testZ)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return new double[] { centerX + dirX * low, centerZ + dirZ * low };
    }

    private boolean shouldRefreshHuntGoto(Entity targetEntity) {
        if (targetEntity == null) {
            return false;
        }
        if (lastHuntGotoTargetEntityId != targetEntity.getId()) {
            return true;
        }
        if (Double.isNaN(lastHuntGotoTargetX) || Double.isNaN(lastHuntGotoTargetY) || Double.isNaN(lastHuntGotoTargetZ)) {
            return true;
        }
        double dx = targetEntity.getX() - lastHuntGotoTargetX;
        double dy = targetEntity.getY() - lastHuntGotoTargetY;
        double dz = targetEntity.getZ() - lastHuntGotoTargetZ;
        return dx * dx + dy * dy + dz * dz >= 1.0D;
    }

    private double[] findSafeHuntNavigationDestination(double desiredX, double desiredY, double desiredZ) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            return null;
        }
        int baseX = (int) Math.floor(desiredX);
        int baseY = (int) Math.floor(desiredY);
        int baseZ = (int) Math.floor(desiredZ);
        BlockPos bestStandPos = null;
        double bestScore = Double.MAX_VALUE;
        int maxFeetY = (int) Math.floor(player.getBoundingBox().minY + 0.001D) + 1;
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 3; dy >= -4; dy--) {
                        BlockPos candidate = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                        if (!isStandableHuntFeetPos(candidate, maxFeetY)) {
                            continue;
                        }
                        double centerX = candidate.getX() + 0.5D;
                        double centerY = candidate.getY();
                        double centerZ = candidate.getZ() + 0.5D;
                        double dxScore = centerX - desiredX;
                        double dyScore = centerY - desiredY;
                        double dzScore = centerZ - desiredZ;
                        double score = dxScore * dxScore + dzScore * dzScore + dyScore * dyScore * 0.45D;
                        if (score < bestScore) {
                            bestScore = score;
                            bestStandPos = candidate;
                        }
                    }
                }
            }
            if (bestStandPos != null) {
                break;
            }
        }
        if (bestStandPos == null) {
            return null;
        }
        return new double[] { bestStandPos.getX() + 0.5D, bestStandPos.getY(), bestStandPos.getZ() + 0.5D };
    }

    private boolean isStandableHuntFeetPos(BlockPos standPos, int maxFeetY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || standPos == null || standPos.getY() > maxFeetY) {
            return false;
        }
        BlockState feetState = mc.level.getBlockState(standPos);
        BlockState headState = mc.level.getBlockState(standPos.above());
        BlockState belowState = mc.level.getBlockState(standPos.below());
        boolean feetPassable = !feetState.blocksMotion();
        boolean headPassable = !headState.blocksMotion();
        boolean hasGround = belowState.blocksMotion();
        return feetPassable && headPassable && hasGround;
    }

    private LivingEntity findNextHuntTarget(LocalPlayer player) {
        if (player == null || player.level() == null) {
            return null;
        }
        LivingEntity best = null;
        AABB searchBounds = getHuntSearchBounds();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchBounds)) {
            if (!isValidHuntCandidate(player, entity)) {
                continue;
            }
            if (best == null || compareHuntTargets(player, entity, best) < 0) {
                best = entity;
            }
        }
        return best;
    }

    private boolean isValidHuntCandidate(LocalPlayer player, LivingEntity entity) {
        if (player == null || entity == null || entity == player || !entity.isAlive() || entity.isRemoved()
                || entity instanceof ArmorStand) {
            return false;
        }
        if (isHuntNoDamageExcludedTarget(entity)) {
            return false;
        }
        if (getDistanceSqToHuntCenter(entity) > getHuntRadiusSq()) {
            return false;
        }
        if (!isWithinHuntVerticalRange(entity)) {
            return false;
        }
        if (huntIgnoreInvisible && entity.isInvisible()) {
            return false;
        }
        String filterName = getHuntFilterableEntityName(entity);
        if (huntEnableNameBlacklist
                && KillAuraHandler.getNameListMatchIndex(filterName, huntNameBlacklist) != Integer.MAX_VALUE) {
            return false;
        }
        if (huntEnableNameWhitelist) {
            return getActiveHuntWhitelistMatchIndex(filterName) != Integer.MAX_VALUE;
        }
        return matchesHuntTargetGroup(entity);
    }

    private boolean isLockedHuntTargetStillTrackable(LocalPlayer player, LivingEntity entity) {
        return player != null
                && entity != null
                && entity != player
                && entity.isAlive()
                && !entity.isRemoved()
                && entity.level() == player.level();
    }

    private boolean matchesHuntTargetGroup(LivingEntity entity) {
        if (!huntRestrictTargetGroups) {
            return true;
        }
        if (entity instanceof Player) {
            return huntTargetPlayers;
        }
        if (isHostileHuntTarget(entity)) {
            return huntTargetHostile;
        }
        if (isPassiveHuntTarget(entity)) {
            return huntTargetPassive;
        }
        return false;
    }

    private boolean isHostileHuntTarget(LivingEntity entity) {
        return entity instanceof Enemy
                || entity instanceof EnderDragon
                || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private boolean isPassiveHuntTarget(LivingEntity entity) {
        MobCategory category = entity.getType().getCategory();
        return entity instanceof Animal
                || entity instanceof AmbientCreature
                || entity instanceof WaterAnimal
                || entity instanceof AbstractVillager
                || entity instanceof IronGolem
                || entity instanceof SnowGolem
                || category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_CREATURE;
    }

    private int compareHuntTargets(LocalPlayer player, LivingEntity left, LivingEntity right) {
        if (huntEnableNameWhitelist && !huntNameWhitelist.isEmpty()) {
            int leftPriority = getActiveHuntWhitelistMatchIndex(getHuntFilterableEntityName(left));
            int rightPriority = getActiveHuntWhitelistMatchIndex(getHuntFilterableEntityName(right));
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }
        }
        int centerCompare = Double.compare(getDistanceSqToHuntCenter(left), getDistanceSqToHuntCenter(right));
        if (centerCompare != 0) {
            return centerCompare;
        }
        return Double.compare(player.distanceToSqr(left), player.distanceToSqr(right));
    }

    private AABB getHuntSearchBounds() {
        return new AABB(
                huntCenterX - huntRadius, huntCenterY - huntDownRange, huntCenterZ - huntRadius,
                huntCenterX + huntRadius, huntCenterY + huntUpRange, huntCenterZ + huntRadius);
    }

    private double getDistanceSqToHuntCenter(Entity entity) {
        if (entity == null) {
            return Double.MAX_VALUE;
        }
        double dx = entity.getX() - huntCenterX;
        double dz = entity.getZ() - huntCenterZ;
        return dx * dx + dz * dz;
    }

    private double getHuntRadiusSq() {
        return huntRadius * huntRadius;
    }

    private boolean isWithinHuntVerticalRange(Entity entity) {
        if (entity == null) {
            return false;
        }
        double dy = entity.getY() - huntCenterY;
        return dy <= huntUpRange + 1.0E-6D && -dy <= huntDownRange + 1.0E-6D;
    }

    private String getHuntFilterableEntityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        String normalized = KillAuraHandler.normalizeFilterName(entity.getDisplayName().getString());
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return KillAuraHandler.normalizeFilterName(entity.getName().getString());
    }

    private List<String> readHuntNameList(JsonObject params, String arrayKey, String textKey) {
        List<String> values = new ArrayList<>();
        if (params == null) {
            return values;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            for (JsonElement element : params.getAsJsonArray(arrayKey)) {
                if (element != null && element.isJsonPrimitive()) {
                    addHuntNameKeyword(values, element.getAsString());
                }
            }
        } else if (params.has(arrayKey) && params.get(arrayKey).isJsonPrimitive()) {
            addHuntNameKeywordsFromText(values, params.get(arrayKey).getAsString());
        }
        if (values.isEmpty() && params.has(textKey) && params.get(textKey).isJsonPrimitive()) {
            addHuntNameKeywordsFromText(values, params.get(textKey).getAsString());
        }
        return values;
    }

    private List<HuntWhitelistTarget> readHuntWhitelistTargets(JsonObject params) {
        List<HuntWhitelistTarget> values = new ArrayList<>();
        if (params == null) {
            return values;
        }
        if (params.has("nameWhitelistEntries") && params.get("nameWhitelistEntries").isJsonArray()) {
            for (JsonElement element : params.getAsJsonArray("nameWhitelistEntries")) {
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    addHuntWhitelistTarget(values,
                            readFirstHuntString(object, "name", "keyword", "target", "value"),
                            readFirstHuntInt(object, 0, "killCount", "count", "kills", "targetCount"));
                } else if (element.isJsonPrimitive()) {
                    addHuntWhitelistTarget(values, element.getAsString(), 0);
                }
            }
        }
        if (!values.isEmpty()) {
            return values;
        }
        for (String name : readHuntNameList(params, "nameWhitelist", "nameWhitelistText")) {
            addHuntWhitelistTarget(values, name, 0);
        }
        return values;
    }

    private String readFirstHuntString(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key != null && object.has(key) && object.get(key).isJsonPrimitive()) {
                return object.get(key).getAsString();
            }
        }
        return "";
    }

    private int readFirstHuntInt(JsonObject object, int defaultValue, String... keys) {
        if (object == null || keys == null) {
            return defaultValue;
        }
        for (String key : keys) {
            if (key == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
                continue;
            }
            try {
                return Math.max(0, object.get(key).getAsInt());
            } catch (Exception ignored) {
                try {
                    return Math.max(0, (int) Math.round(Double.parseDouble(object.get(key).getAsString().trim())));
                } catch (Exception ignoredAgain) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    private void addHuntWhitelistTarget(List<HuntWhitelistTarget> target, String rawValue, int killCount) {
        if (target == null) {
            return;
        }
        String normalized = KillAuraHandler.normalizeFilterName(rawValue);
        if (normalized.isEmpty()) {
            return;
        }
        for (int i = 0; i < target.size(); i++) {
            HuntWhitelistTarget existing = target.get(i);
            if (existing != null && existing.name.equalsIgnoreCase(normalized)) {
                target.set(i, new HuntWhitelistTarget(normalized, killCount));
                return;
            }
        }
        target.add(new HuntWhitelistTarget(normalized, killCount));
    }

    private String formatHuntWhitelistForLog() {
        if (huntWhitelistTargets.isEmpty()) {
            return huntNameWhitelist.toString();
        }
        List<String> parts = new ArrayList<>();
        for (HuntWhitelistTarget target : huntWhitelistTargets) {
            if (target == null || target.name.isEmpty()) {
                continue;
            }
            parts.add(target.hasKillLimit() ? target.name + " x" + target.killCount : target.name + " 清完");
        }
        return parts.toString();
    }

    private boolean isHuntTargetKilled(LivingEntity entity) {
        return entity != null && (!entity.isAlive() || entity.isRemoved() || entity.getHealth() <= 0.0F);
    }

    private void recordHuntTargetKill(LivingEntity entity) {
        if (entity == null || !huntEnableNameWhitelist || huntWhitelistTargets.isEmpty()) {
            return;
        }
        int entityId = entity.getId();
        if (countedHuntKillEntityIds.contains(entityId)) {
            return;
        }
        String filterName = getHuntFilterableEntityName(entity);
        int index = getAnyHuntWhitelistMatchIndex(filterName);
        if (index == Integer.MAX_VALUE || index < 0 || index >= huntWhitelistTargets.size()) {
            return;
        }
        countedHuntKillEntityIds.add(entityId);
        HuntWhitelistTarget target = huntWhitelistTargets.get(index);
        if (target != null && target.hasKillLimit()) {
            int next = huntWhitelistKillProgress.getOrDefault(target.name, 0) + 1;
            huntWhitelistKillProgress.put(target.name, next);
            recordDebugTrace("hunt whitelist kill -> " + target.name + " " + next + "/" + target.killCount);
        }
    }

    private int getAnyHuntWhitelistMatchIndex(String filterName) {
        if (huntWhitelistTargets.isEmpty()) {
            return KillAuraHandler.getNameListMatchIndex(filterName, huntNameWhitelist);
        }
        for (int i = 0; i < huntWhitelistTargets.size(); i++) {
            HuntWhitelistTarget target = huntWhitelistTargets.get(i);
            if (target == null || target.name.isEmpty()) {
                continue;
            }
            if (KillAuraHandler.getNameListMatchIndex(filterName, Collections.singletonList(target.name))
                    != Integer.MAX_VALUE) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private int getActiveHuntWhitelistMatchIndex(String filterName) {
        if (huntWhitelistTargets.isEmpty()) {
            return KillAuraHandler.getNameListMatchIndex(filterName, huntNameWhitelist);
        }
        for (int i = 0; i < huntWhitelistTargets.size(); i++) {
            HuntWhitelistTarget target = huntWhitelistTargets.get(i);
            if (target == null || target.name.isEmpty() || isHuntWhitelistTargetComplete(target)) {
                continue;
            }
            if (KillAuraHandler.getNameListMatchIndex(filterName, Collections.singletonList(target.name))
                    != Integer.MAX_VALUE) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean isHuntWhitelistTargetComplete(HuntWhitelistTarget target) {
        return target != null
                && target.hasKillLimit()
                && huntWhitelistKillProgress.getOrDefault(target.name, 0) >= target.killCount;
    }

    private boolean shouldCompleteHuntByWhitelistKillGoals() {
        if (!huntEnableNameWhitelist || huntWhitelistTargets.isEmpty()) {
            return false;
        }
        for (HuntWhitelistTarget target : huntWhitelistTargets) {
            if (target == null || target.name.isEmpty()) {
                continue;
            }
            if (target.hasKillLimit() && isHuntWhitelistTargetComplete(target)) {
                return true;
            }
        }
        return false;
    }

    private void addHuntNameKeywordsFromText(List<String> target, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        for (String token : text.split("\\r?\\n|,|，")) {
            addHuntNameKeyword(target, token);
        }
    }

    private void addHuntNameKeyword(List<String> target, String rawValue) {
        String normalized = KillAuraHandler.normalizeFilterName(rawValue);
        if (normalized.isEmpty()) {
            return;
        }
        for (String existing : target) {
            if (existing.equalsIgnoreCase(normalized)) {
                return;
            }
        }
        target.add(normalized);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!tracking || !isHunting || !huntShowRange) {
            return;
        }
        RenderWorldLastEvent.WorldRenderContext renderContext = event.getWorldRenderContext();
        PoseStack poseStack = event.createWorldPoseStack();
        if (renderContext == null || poseStack == null) {
            return;
        }
        drawHuntRangeAura(huntCenterX, huntCenterY + 0.05D, huntCenterZ, huntRadius, poseStack, renderContext);
    }

    private void drawHuntRangeAura(double centerX, double centerY, double centerZ, double radius,
            PoseStack poseStack, RenderWorldLastEvent.WorldRenderContext renderContext) {
        Vec3 center = renderContext.toCameraSpace(new Vec3(centerX, centerY, centerZ));
        if (center == null) {
            return;
        }
        double drawRadius = Math.max(0.5D, radius);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= 72; i++) {
            double angle = (Math.PI * 2.0D * i) / 72.0D;
            double x = center.x + Math.cos(angle) * drawRadius;
            double z = center.z + Math.sin(angle) * drawRadius;
            buffer.vertex(poseStack.last().pose(), (float) x, (float) center.y, (float) z)
                    .color(0.25F, 0.95F, 0.45F, 0.95F)
                    .endVertex();
        }
        Tesselator.getInstance().end();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static final class HuntAttackSequenceExecutor {
        private static final int POST_ACTION_DELAY_TICKS = 5;

        private PathSequenceManager.PathSequence sequence;
        private int stepIndex;
        private int actionIndex;
        private int tickDelay;
        private int targetEntityId = Integer.MIN_VALUE;
        private final ScopedRuntimeVariables runtimeVariables = new ScopedRuntimeVariables();
        private final Map<String, String> heldKeys = new LinkedHashMap<>();

        private boolean isRunning() {
            return sequence != null;
        }

        private void start(PathSequenceManager.PathSequence sourceSequence, LocalPlayer player, LivingEntity target) {
            stop();
            if (sourceSequence == null || sourceSequence.getSteps() == null || sourceSequence.getSteps().isEmpty()) {
                return;
            }
            this.sequence = new PathSequenceManager.PathSequence(sourceSequence);
            this.targetEntityId = target == null ? Integer.MIN_VALUE : target.getId();
            populateTargetVariables(player, target);
            this.runtimeVariables.enterStep(0);
        }

        private void stop() {
            releaseHeldKeys();
            this.sequence = null;
            this.stepIndex = 0;
            this.actionIndex = 0;
            this.tickDelay = 0;
            this.targetEntityId = Integer.MIN_VALUE;
            this.runtimeVariables.clear();
            this.heldKeys.clear();
        }

        private void tick(LocalPlayer player) {
            if (!isRunning() || player == null) {
                if (player == null) {
                    stop();
                }
                return;
            }
            refreshTargetVariables(player);
            if (tickDelay > 0) {
                tickDelay--;
                return;
            }
            if (sequence == null || stepIndex >= sequence.getSteps().size()) {
                stop();
                return;
            }
            PathSequenceManager.PathStep currentStep = sequence.getSteps().get(stepIndex);
            List<PathSequenceManager.ActionData> actions = currentStep == null ? null : currentStep.getActions();
            if (actions == null || actionIndex >= actions.size()) {
                stepIndex++;
                actionIndex = 0;
                runtimeVariables.enterStep(stepIndex);
                return;
            }
            PathSequenceManager.ActionData rawAction = actions.get(actionIndex);
            runtimeVariables.beginAction(stepIndex, actionIndex);
            JsonObject resolvedParams = LegacyActionRuntime.resolveParams(rawAction.params, runtimeVariables,
                    player, sequence, stepIndex, actionIndex, getLiteralParamKeysForAction(rawAction.type));
            String actionType = rawAction.type == null ? "" : rawAction.type.trim().toLowerCase(Locale.ROOT);
            if (shouldSkipAction(actionType)) {
                actionIndex++;
                return;
            }
            Consumer<LocalPlayer> action = PathSequenceManager.parseAction(rawAction.type, resolvedParams);
            actionIndex++;
            if (action == null) {
                return;
            }
            if (action instanceof ModUtils.DelayAction delayAction) {
                tickDelay = delayAction.getDelayTicks();
                return;
            }
            try {
                action.accept(player);
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("[hunt_sequence] 执行动作失败: {}", rawAction.getDescription(), e);
            }
            updateHeldKeyState(rawAction);
            tickDelay = POST_ACTION_DELAY_TICKS;
        }

        private void populateTargetVariables(LocalPlayer player, LivingEntity target) {
            runtimeVariables.put("target_found", target != null);
            if (target == null) {
                return;
            }
            runtimeVariables.put("target_name", target.getDisplayName().getString());
            runtimeVariables.put("target_id", target.getId());
            runtimeVariables.put("target_x", target.getX());
            runtimeVariables.put("target_y", target.getY());
            runtimeVariables.put("target_z", target.getZ());
            if (player != null) {
                runtimeVariables.put("target_distance", player.distanceTo(target));
            }
        }

        private void refreshTargetVariables(LocalPlayer player) {
            if (player == null || player.level() == null || targetEntityId == Integer.MIN_VALUE) {
                return;
            }
            Entity targetEntity = player.level().getEntity(targetEntityId);
            populateTargetVariables(player, targetEntity instanceof LivingEntity living ? living : null);
        }

        private boolean shouldSkipAction(String actionType) {
            return "run_sequence".equals(actionType)
                    || "hunt".equals(actionType)
                    || "set_var".equals(actionType)
                    || "goto_action".equals(actionType)
                    || "repeat_actions".equals(actionType)
                    || "capture_nearby_entity".equals(actionType)
                    || "capture_gui_title".equals(actionType)
                    || "capture_block_at".equals(actionType)
                    || actionType.startsWith("condition_")
                    || actionType.startsWith("wait_until_");
        }

        private void updateHeldKeyState(PathSequenceManager.ActionData actionData) {
            if (actionData == null || actionData.params == null || !"key".equalsIgnoreCase(actionData.type)) {
                return;
            }
            String key = actionData.params.has("key") ? actionData.params.get("key").getAsString().trim() : "";
            String state = actionData.params.has("state") ? actionData.params.get("state").getAsString().trim() : "";
            if (key.isEmpty() || state.isEmpty()) {
                return;
            }
            String normalizedState = state.toLowerCase(Locale.ROOT);
            if ("down".equals(normalizedState) || "robotdown".equals(normalizedState)) {
                heldKeys.put(key, "Up");
            } else if ("up".equals(normalizedState) || "robotup".equals(normalizedState)) {
                heldKeys.remove(key);
            }
        }

        private void releaseHeldKeys() {
            if (heldKeys.isEmpty()) {
                return;
            }
            for (Map.Entry<String, String> entry : heldKeys.entrySet()) {
                try {
                    ModUtils.simulateKey(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    zszlScriptMod.LOGGER.warn("[hunt_sequence] 释放按键失败: {}", entry.getKey(), e);
                }
            }
        }
    }

    private static Set<String> getLiteralParamKeysForAction(String actionType) {
        String literalKey = ActionVariableRegistry.resolveVariableParamKey(actionType);
        return literalKey == null || literalKey.trim().isEmpty()
                ? Collections.emptySet()
                : Collections.singleton(literalKey);
    }

    private String safeError(Exception error) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return "unknown";
        }
        return error.getMessage().trim();
    }

    private String getCurrentGuiTitle() {
        if (Minecraft.getInstance().screen == null || Minecraft.getInstance().screen.getTitle() == null) {
            return "";
        }
        return Minecraft.getInstance().screen.getTitle().getString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String getString(JsonObject params, String key) {
        return params != null && params.has(key) ? params.get(key).getAsString().trim() : "";
    }

    private int getInt(JsonObject params, String key, int fallback) {
        try {
            return params != null && params.has(key) ? params.get(key).getAsInt() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double getDouble(JsonObject params, String key, double fallback) {
        try {
            return params != null && params.has(key) ? params.get(key).getAsDouble() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private List<String> splitLines(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        for (String part : text.split("\\r?\\n")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                lines.add(value);
            }
        }
        return lines;
    }
}
