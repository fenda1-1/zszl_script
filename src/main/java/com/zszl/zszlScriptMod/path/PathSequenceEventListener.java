// 文件路径: src/main/java/com/zszl/zszlScriptMod/path/PathSequenceEventListener.java
package com.zszl.zszlScriptMod.path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiElementInspector;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.AutoUseItemHandler;
import com.zszl.zszlScriptMod.handlers.EmbeddedNavigationHandler;
import com.zszl.zszlScriptMod.handlers.HuntOrbitController;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BetterBlockPos;
import com.zszl.zszlScriptMod.shadowbaritone.process.KillAuraOrbitProcess;
import com.zszl.zszlScriptMod.shadowbaritone.utils.PathRenderer;
import com.zszl.zszlScriptMod.system.AutoUseItemRule;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager;
import com.zszl.zszlScriptMod.path.runtime.safety.PathSafetyManager;
import com.zszl.zszlScriptMod.path.runtime.locks.ResourceLockManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PacketFieldRuleManager;
import com.zszl.zszlScriptMod.utils.vision.ScreenVisionUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class PathSequenceEventListener {
    private static final Path RUNTIME_CONFIG_PATH = Paths.get(ModConfig.CONFIG_DIR, "path_sequence_runtime_config.json");
    private static final Gson RUNTIME_CONFIG_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_BUILTIN_SEQUENCE_DELAY_TICKS = 5;
    private static final int PATH_RETRY_NOTIFY_TICKS = 20;
    private static final double PATH_RETRY_MOVEMENT_EPSILON_SQ = 0.16D;
    private static boolean builtinSequenceDelayEnabled = true;
    private static int builtinSequenceDelayTicks = DEFAULT_BUILTIN_SEQUENCE_DELAY_TICKS;
    private static boolean runtimeConfigLoaded = false;
    private static final ThreadLocal<PathSequenceEventListener> ACTION_EXECUTION_CONTEXT = new ThreadLocal<>();
    private static final List<PathSequenceEventListener> backgroundRunners = new CopyOnWriteArrayList<>();
    private static final List<String> debugBreakpoints = new CopyOnWriteArrayList<>();
    public static final PathSequenceEventListener instance = new PathSequenceEventListener();

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
            this.sequenceName = sequenceName;
            this.stepIndex = stepIndex;
            this.actionIndex = actionIndex;
            this.atTarget = atTarget;
            this.remainingLoops = remainingLoops;
            this.tickDelay = tickDelay;
            this.explicitDelay = explicitDelay;
            this.status = status;
            this.variableSnapshot = variableSnapshot;
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
        private final boolean tracking;
        private final boolean paused;
        private final boolean pausedForDebug;
        private final String sequenceName;
        private final int stepIndex;
        private final int actionIndex;
        private final String currentActionDescription;
        private final String status;
        private final List<String> traceLines;
        private final Map<String, String> variablePreview;
        private final int breakpointCount;

        public DebugSnapshot(boolean tracking, boolean paused, boolean pausedForDebug, String sequenceName, int stepIndex, int actionIndex,
                String currentActionDescription, String status, List<String> traceLines,
                Map<String, String> variablePreview, int breakpointCount) {
            this.tracking = tracking;
            this.paused = paused;
            this.pausedForDebug = pausedForDebug;
            this.sequenceName = sequenceName == null ? "" : sequenceName;
            this.stepIndex = stepIndex;
            this.actionIndex = actionIndex;
            this.currentActionDescription = currentActionDescription == null ? "" : currentActionDescription;
            this.status = status == null ? "" : status;
            this.traceLines = traceLines == null ? Collections.emptyList() : new ArrayList<>(traceLines);
            this.variablePreview = variablePreview == null ? Collections.emptyMap() : new LinkedHashMap<>(variablePreview);
            this.breakpointCount = breakpointCount;
        }

        public boolean isTracking() {
            return tracking;
        }

        public boolean isPaused() {
            return paused;
        }

        public boolean isPausedForDebug() {
            return pausedForDebug;
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

        public String getCurrentActionDescription() {
            return currentActionDescription;
        }

        public String getStatus() {
            return status;
        }

        public List<String> getTraceLines() {
            return new ArrayList<>(traceLines);
        }

        public Map<String, String> getVariablePreview() {
            return new LinkedHashMap<>(variablePreview);
        }

        public int getBreakpointCount() {
            return breakpointCount;
        }
    }

    public PathSequence currentSequence;
    private int currentStepIndex = 0;
    private int actionIndex = 0;
    private boolean tracking = false;
    private int tickDelay = 0;
    private boolean atTarget = false;
    private int remainingLoops = 0;
    private String status = "";
    private volatile boolean isPaused = false;
    private boolean pausedForDebug = false;
    private boolean debugStepArmed = false;
    private String debugIgnoreBreakpointKey = "";
    private boolean isPerformingExplicitDelay = false;
    private boolean explicitDelayNormalizeTo20Tps = false;
    private double explicitDelayRemainingBaselineTicks = 0.0D;
    private boolean pausedByGui = false;
    private boolean pendingLoopRestart = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final boolean backgroundRunner;
    private final String lockOwnerId = UUID.randomUUID().toString();
    private String waitingLockResource = "";
    private int currentStepRetryUsed = 0;
    private int currentStepIdleTicks = 0;
    private boolean currentStepIdleAnnounced = false;
    private double currentStepLastMovementX = Double.NaN;
    private double currentStepLastMovementY = Double.NaN;
    private double currentStepLastMovementZ = Double.NaN;
    private final Map<String, Object> initialSequenceVariables = new LinkedHashMap<>();

    public static int loopDelayTicks = 20;

    // --- 新增：狩猎状态变量 ---
    private boolean isHunting = false;
    private double huntRadius;
    private boolean huntAutoAttack;
    private String huntAttackMode = KillAuraHandler.ATTACK_MODE_NORMAL;
    private String huntAttackSequenceName = "";
    private boolean huntAimLockEnabled = true;
    private double huntTrackingDistanceSq; // 使用距离的平方以提高性能
    private Entity huntTargetEntity = null;
    private long lastHuntGotoTargetEntityId = -1L;
    private boolean huntMovementStopped = false;
    private String huntMode = KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
    private boolean huntOrbitEnabled = false;
    private boolean huntChaseIntervalEnabled = false;
    private int huntChaseIntervalTicks = 0;
    private int huntChaseCooldownTicks = 0;
    private boolean huntWasWithinDesiredDistance = false;
    private int huntAttackCooldownTicks = 0;
    private double lastHuntGotoTargetX = Double.NaN;
    private double lastHuntGotoTargetY = Double.NaN;
    private double lastHuntGotoTargetZ = Double.NaN;
    private int huntAttackRemaining = -1;
    private int huntNoTargetSkipCount = 0;
    private boolean huntRestrictTargetGroups = true;
    private boolean huntTargetHostile = true;
    private boolean huntTargetPassive = false;
    private boolean huntTargetPlayers = false;
    private boolean huntEnableNameWhitelist = false;
    private boolean huntEnableNameBlacklist = false;
    private final List<String> huntNameWhitelist = new ArrayList<>();
    private final List<String> huntNameBlacklist = new ArrayList<>();
    private boolean huntShowRange = false; // 是否显示搜怪范围
    private boolean huntIgnoreInvisible = false; // 是否忽略隐身目标
    private double huntCenterX = 0.0D; // 搜怪中心点X
    private double huntCenterY = 0.0D; // 搜怪中心点Y
    private double huntCenterZ = 0.0D; // 搜怪中心点Z
    private int huntOrbitLoopNodeIndex = -1;
    private int huntLastOrbitGotoTick = -99999;
    private int huntOrbitStuckTicks = 0;
    private double huntLastOrbitPlayerX = Double.NaN;
    private double huntLastOrbitPlayerZ = Double.NaN;
    private boolean huntPendingCompleteAfterSequence = false;
    private final HuntAttackSequenceExecutor huntAttackSequenceExecutor = new HuntAttackSequenceExecutor();
    private final HuntOrbitController huntOrbitController = new HuntOrbitController();
    private static final double HUNT_FIXED_DISTANCE_TOLERANCE = 0.30D;
    private static final double HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER = 1.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER = 2.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_MAX_VERTICAL_DELTA = 3.5D;
    // --- 新增结束 ---

    // --- 新增：跟随实体状态变量 ---
    private boolean isFollowingEntity = false;
    private String followEntityType = "player"; // player, hostile, passive, all
    private String followTargetName = ""; // 目标名称（支持部分匹配）
    private double followSearchRadius = 16.0D;
    private double followDistance = 3.0D;
    private int followTimeoutSeconds = 0; // 0表示无限跟随
    private boolean followStopOnLost = true;
    private Entity followTargetEntity = null;
    private long followStartTime = 0L;
    // --- 新增结束 ---

    // --- 使用快捷栏物品动作状态 ---
    private boolean hotbarUseActionRunning = false;
    private String hotbarUseItemName = "";
    private AutoUseItemRule.MatchMode hotbarUseMatchMode = AutoUseItemRule.MatchMode.CONTAINS;
    private AutoUseItemRule.UseMode hotbarUseMode = AutoUseItemRule.UseMode.RIGHT_CLICK;
    private boolean hotbarUseChangeLocalSlot = false;
    private int hotbarUseSwitchItemDelayTicks = 0;
    private int hotbarUseSwitchDelayTicks = 0;
    private int hotbarUseSwitchBackDelayTicks = 0;
    private int hotbarUseRemainingCount = 0;
    private int hotbarUseIntervalTicks = 0;
    private int hotbarUseWaitTicks = 0;
    // --- 新增结束 ---

    // --- 异步动作状态 ---
    private String pendingAsyncActionType = "";
    private int pendingAsyncActionStepIndex = -1;
    private int pendingAsyncActionIndex = -1;
    // --- 新增结束 ---

    // --- 等待条件动作状态 ---
    private boolean waitConditionRunning = false;
    private int waitConditionElapsedTicks = 0;
    private long waitConditionStartCapturedUpdateVersion = 0L;
    private long waitConditionStartCapturedRecaptureVersion = 0L;
    private long waitConditionStartPacketTextVersion = 0L;
    private ActionData deferredWaitActionData = null;
    private int deferredWaitResumeActionIndex = -1;
    private int deferredWaitElapsedTicks = 0;
    private long deferredWaitStartCapturedUpdateVersion = 0L;
    private long deferredWaitStartCapturedRecaptureVersion = 0L;
    private long deferredWaitStartPacketTextVersion = 0L;
    // --- 新增结束 ---

    // --- 旧动作系统运行时变量/控制流状态 ---
    private final ScopedRuntimeVariables runtimeVariables = new ScopedRuntimeVariables();
    private boolean repeatActionRunning = false;
    private int repeatActionStepIndex = -1;
    private int repeatActionHeaderIndex = -1;
    private int repeatActionBodyStartIndex = -1;
    private int repeatActionBodyEndIndex = -1;
    private int repeatActionRemainingLoops = 0;
    private int repeatActionIteration = 0;
    private String repeatActionLoopVarName = "loop_index";
    private final Deque<String> debugTraceLines = new ArrayDeque<>();
    private String currentDebugActionDescription = "";
    private String executionLogSessionId = "";
    private boolean executionResultSuccess = false;
    private String executionResultReason = "";
    // --- 新增结束 ---

    private PathSequenceEventListener() {
        this(false);
    }

    private PathSequenceEventListener(boolean backgroundRunner) {
        this.backgroundRunner = backgroundRunner;
        // 运行时配置改为首次使用时再加载，避免静态字段初始化顺序导致的 NPE。
    }

    static PathSequenceEventListener getCurrentExecutionContext() {
        return ACTION_EXECUTION_CONTEXT.get();
    }

    boolean isBackgroundRunner() {
        return backgroundRunner;
    }

    private boolean ensureResources(EnumSet<ResourceLockManager.Resource> resources, String detail) {
        String sequenceName = currentSequence == null ? "" : currentSequence.getName();
        String blocked = ResourceLockManager.acquireOrSync(lockOwnerId, sequenceName, backgroundRunner, resources, detail);
        String policy = currentSequence == null ? "WAIT" : currentSequence.getLockConflictPolicy();
        if (blocked != null && !blocked.isEmpty()
                && "PREEMPT_BACKGROUND".equalsIgnoreCase(policy)
                && !backgroundRunner) {
            ResourceLockManager.Resource blockedResource = ResourceLockManager.parseResource(blocked);
            if (ResourceLockManager.isHeldByBackground(blockedResource)) {
                stopAllBackgroundRunners();
                blocked = ResourceLockManager.acquireOrSync(lockOwnerId, sequenceName, backgroundRunner, resources, detail);
            }
        }
        if (blocked != null && !blocked.isEmpty()) {
            if ("FAIL".equalsIgnoreCase(policy)) {
                waitingLockResource = blocked;
                status = sequenceName + " | 锁冲突失败:" + blocked;
                recordDebugTrace("锁冲突失败: " + blocked + " / " + detail);
                markExecutionResult(false, "锁冲突失败: " + blocked);
                stopTracking();
                return false;
            }
            waitingLockResource = blocked;
            status = sequenceName + " | 等待锁:" + blocked;
            tickDelay = 2;
            return false;
        }
        if (!waitingLockResource.isEmpty() && status != null && status.contains("等待锁:")) {
            status = sequenceName + (backgroundRunner ? " | 后台执行" : "");
        }
        waitingLockResource = "";
        return true;
    }

    private void releaseResources() {
        waitingLockResource = "";
        ResourceLockManager.releaseAll(lockOwnerId);
    }

    private EnumSet<ResourceLockManager.Resource> resolveMovementResources(double[] target) {
        EnumSet<ResourceLockManager.Resource> resources = EnumSet.noneOf(ResourceLockManager.Resource.class);
        if (target != null && target.length >= 3 && !Double.isNaN(target[0])) {
            resources.add(ResourceLockManager.Resource.MOVE);
        }
        return resources;
    }

    private EnumSet<ResourceLockManager.Resource> resolveActionResources(ActionData actionData) {
        EnumSet<ResourceLockManager.Resource> resources = EnumSet.noneOf(ResourceLockManager.Resource.class);
        String type = actionData == null || actionData.type == null ? "" : actionData.type.trim().toLowerCase(Locale.ROOT);
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
            case "toggle_kill_aura":
                resources.add(ResourceLockManager.Resource.COMBAT);
                resources.add(ResourceLockManager.Resource.LOOK);
                resources.add(ResourceLockManager.Resource.MOVE);
                break;
            default:
                break;
        }
        return resources;
    }

    public static boolean isSequenceRunningInForeground(String sequenceName) {
        return sequenceName != null
                && instance.isTracking()
                && instance.currentSequence != null
                && sequenceName.equals(instance.currentSequence.getName());
    }

    public static boolean isSequenceRunningInBackground(String sequenceName) {
        if (sequenceName == null || sequenceName.trim().isEmpty()) {
            return false;
        }
        for (PathSequenceEventListener runner : new ArrayList<>(backgroundRunners)) {
            if (runner != null
                    && runner.isTracking()
                    && runner.currentSequence != null
                    && sequenceName.equals(runner.currentSequence.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnySequenceRunning() {
        if (instance != null && instance.isTracking()) {
            return true;
        }
        for (PathSequenceEventListener runner : new ArrayList<>(backgroundRunners)) {
            if (runner != null && runner.isTracking()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyHuntOrbitActionRunning() {
        if (instance != null && instance.isHuntOrbitActionActive()) {
            return true;
        }
        for (PathSequenceEventListener runner : new ArrayList<>(backgroundRunners)) {
            if (runner != null && runner.isHuntOrbitActionActive()) {
                return true;
            }
        }
        return false;
    }

    private boolean isHuntOrbitActionActive() {
        return isHunting
                && isHuntOrbitMode()
                && huntTargetEntity instanceof EntityLivingBase
                && huntTargetEntity.isEntityAlive();
    }

    public static boolean startBackgroundSequence(PathSequence sequence) {
        return startBackgroundSequence(sequence, 0, null);
    }

    public static boolean startBackgroundSequence(PathSequence sequence, int remainingLoops) {
        return startBackgroundSequence(sequence, remainingLoops, null);
    }

    public static boolean startBackgroundSequence(PathSequence sequence, int remainingLoops,
            Map<String, Object> initialSequenceVariables) {
        if (sequence == null || sequence.getSteps().isEmpty()) {
            return false;
        }

        PathSafetyManager.SafetyDecision decision = PathSafetyManager.evaluateBackgroundSequence(sequence);
        if (decision.isBlocked()) {
            zszlScriptMod.LOGGER.warn("[PathSafety] {}", decision.getReason());
            return false;
        }

        stopAllBackgroundRunners();

        PathSequenceEventListener runner = new PathSequenceEventListener(true);
        backgroundRunners.add(runner);
        runner.setStatus(sequence.getName() + " | 后台执行");
        runner.startTracking(sequence, remainingLoops, initialSequenceVariables);
        runner.resume();
        primeBackgroundNavigation(runner, sequence);
        return true;
    }

    public static void stopAllBackgroundRunners() {
        for (PathSequenceEventListener runner : new ArrayList<>(backgroundRunners)) {
            if (runner != null && runner.isTracking()) {
                runner.markExecutionResult(false, "后台序列被新的后台执行打断");
                runner.stopTracking();
            }
        }
        backgroundRunners.clear();
    }

    public static boolean stopForegroundSequenceByAction() {
        if (instance == null || !instance.isTracking()) {
            return false;
        }
        instance.markExecutionResult(false, "动作停止前台序列");
        instance.stopTracking();
        return true;
    }

    public static boolean stopBackgroundSequencesByAction() {
        boolean stopped = false;
        for (PathSequenceEventListener runner : new ArrayList<>(backgroundRunners)) {
            if (runner != null && runner.isTracking()) {
                runner.markExecutionResult(false, "动作停止后台序列");
                runner.stopTracking();
                stopped = true;
            }
        }
        return stopped;
    }

    private static boolean restoreAnyBackgroundNavigation() {
        for (PathSequenceEventListener runner : new ArrayList<>(backgroundRunners)) {
            if (runner != null && runner.isTracking() && runner.restoreNavigationStateIfNeeded()) {
                return true;
            }
        }
        return false;
    }

    private boolean restoreNavigationStateIfNeeded() {
        if (!backgroundRunner || !tracking || currentSequence == null) {
            return false;
        }
        if (isHunting && huntTargetEntity != null && huntTargetEntity.isEntityAlive()) {
            if (mc.player != null) {
                EntityLivingBase livingTarget = huntTargetEntity instanceof EntityLivingBase
                        ? (EntityLivingBase) huntTargetEntity
                        : null;
                if (livingTarget == null) {
                    return false;
                }
                double distance = mc.player.getDistance(livingTarget);
                if (shouldUseContinuousHuntOrbit(mc.player, livingTarget, distance)) {
                    stopHuntEmbeddedNavigation();
                    stopHuntOrbitProcess();
                    driveContinuousHuntOrbit(mc.player, livingTarget);
                    return true;
                }
                if (shouldRunHuntMovementForDistance(distance)) {
                    huntOrbitController.stop();
                    navigateHuntTowardsTarget(mc.player, livingTarget);
                    return true;
                }
            }
            return false;
        }
        if (currentStepIndex < 0 || currentStepIndex >= currentSequence.getSteps().size()) {
            return false;
        }
        if (atTarget) {
            return false;
        }
        double[] target = currentSequence.getSteps().get(currentStepIndex).getGotoPoint();
        if (target != null && target.length >= 3 && !Double.isNaN(target[0])) {
            EmbeddedNavigationHandler.INSTANCE.startGoto(target[0], target[1], target[2]);
            return true;
        }
        return false;
    }

    private boolean shouldStopNavigationOnFinish() {
        if (backgroundRunner) {
            return !(PathSequenceEventListener.instance.isTracking()
                    && PathSequenceEventListener.instance.currentSequence != null);
        }
        return !restoreAnyBackgroundNavigation();
    }

    private boolean shouldStopNavigationForStepTransition() {
        if (!backgroundRunner) {
            return true;
        }
        return !(PathSequenceEventListener.instance.isTracking()
                && PathSequenceEventListener.instance.currentSequence != null);
    }

    private static void primeBackgroundNavigation(PathSequenceEventListener runner, PathSequence sequence) {
        if (runner == null || sequence == null || sequence.getSteps().isEmpty()) {
            return;
        }
        int firstStepIndex = Math.max(0, Math.min(runner.currentStepIndex, sequence.getSteps().size() - 1));
        double[] firstTarget = sequence.getSteps().get(firstStepIndex).getGotoPoint();
        if (firstTarget != null && firstTarget.length >= 3 && !Double.isNaN(firstTarget[0])) {
            EmbeddedNavigationHandler.INSTANCE.startGoto(firstTarget[0], firstTarget[1], firstTarget[2]);
        }
    }

    private static synchronized void ensureRuntimeConfigLoaded() {
        if (runtimeConfigLoaded) {
            return;
        }
        runtimeConfigLoaded = true;
        builtinSequenceDelayEnabled = true;
        builtinSequenceDelayTicks = DEFAULT_BUILTIN_SEQUENCE_DELAY_TICKS;

        if (!Files.exists(RUNTIME_CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(RUNTIME_CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = RUNTIME_CONFIG_GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            if (root.has("builtinSequenceDelayEnabled")) {
                builtinSequenceDelayEnabled = root.get("builtinSequenceDelayEnabled").getAsBoolean();
            }
            if (root.has("builtinSequenceDelayTicks")) {
                builtinSequenceDelayTicks = Math.max(0, root.get("builtinSequenceDelayTicks").getAsInt());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("读取序列运行时配置失败: {}", RUNTIME_CONFIG_PATH, e);
        }
    }

    private static synchronized void saveRuntimeConfig() {
        ensureRuntimeConfigLoaded();
        try {
            Files.createDirectories(RUNTIME_CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("builtinSequenceDelayEnabled", builtinSequenceDelayEnabled);
            root.addProperty("builtinSequenceDelayTicks", Math.max(0, builtinSequenceDelayTicks));
            try (Writer writer = Files.newBufferedWriter(RUNTIME_CONFIG_PATH, StandardCharsets.UTF_8)) {
                RUNTIME_CONFIG_GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("保存序列运行时配置失败: {}", RUNTIME_CONFIG_PATH, e);
        }
    }

    public static synchronized boolean isBuiltinSequenceDelayEnabled() {
        ensureRuntimeConfigLoaded();
        return builtinSequenceDelayEnabled;
    }

    public static synchronized int getBuiltinSequenceDelayTicks() {
        ensureRuntimeConfigLoaded();
        return Math.max(0, builtinSequenceDelayTicks);
    }

    public static synchronized void updateBuiltinSequenceDelayConfig(boolean enabled, int ticks) {
        ensureRuntimeConfigLoaded();
        builtinSequenceDelayEnabled = enabled;
        builtinSequenceDelayTicks = Math.max(0, ticks);
        saveRuntimeConfig();
    }

    private static synchronized int getBuiltinSequenceActionDelayTicks() {
        ensureRuntimeConfigLoaded();
        return builtinSequenceDelayEnabled ? Math.max(0, builtinSequenceDelayTicks) : 0;
    }

    private void applyBuiltinSequenceDelay() {
        this.tickDelay = getBuiltinSequenceActionDelayTicks();
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setStatus(String s) {
        String next = s == null ? "" : s;
        if (next.equals(status)) {
            return;
        }
        status = next;
        appendExecutionLogEvent("status", "状态更新: " + next);
    }

    public String getStatus() {
        return status;
    }

    public void pauseForDebug() {
        if (!tracking || isPaused) {
            return;
        }
        pauseForDebugInternal("手动调试暂停");
    }

    public void resumeFromDebug() {
        if (!tracking) {
            return;
        }
        if (isPaused) {
            debugIgnoreBreakpointKey = buildBreakpointKey(currentSequence == null ? "" : currentSequence.getName(),
                    currentStepIndex, actionIndex);
            resume();
        }
        recordDebugTrace("调试继续执行");
    }

    public void requestDebugStep() {
        if (!tracking) {
            return;
        }
        debugStepArmed = true;
        debugIgnoreBreakpointKey = buildBreakpointKey(currentSequence == null ? "" : currentSequence.getName(),
                currentStepIndex, actionIndex);
        if (isPaused) {
            resume();
        }
        recordDebugTrace("请求单步执行");
    }

    public void clearDebugTrace() {
        debugTraceLines.clear();
        currentDebugActionDescription = "";
    }

    public DebugSnapshot getDebugSnapshot() {
        List<String> traces = new ArrayList<>(debugTraceLines);
        Map<String, String> preview = buildVariablePreview();
        return new DebugSnapshot(tracking, isPaused, pausedForDebug,
                currentSequence == null ? "" : currentSequence.getName(),
                currentStepIndex,
                actionIndex,
                currentDebugActionDescription,
                status,
                traces,
                preview,
                countBreakpointsForSequence(currentSequence == null ? "" : currentSequence.getName()));
    }

    public static boolean hasDebugBreakpoint(String sequenceName, int stepIndex, int actionIndex) {
        String key = buildBreakpointKey(sequenceName, stepIndex, actionIndex);
        return !key.isEmpty() && debugBreakpoints.contains(key);
    }

    public static void toggleDebugBreakpoint(String sequenceName, int stepIndex, int actionIndex) {
        String key = buildBreakpointKey(sequenceName, stepIndex, actionIndex);
        if (key.isEmpty()) {
            return;
        }
        if (debugBreakpoints.contains(key)) {
            debugBreakpoints.remove(key);
        } else {
            debugBreakpoints.add(key);
        }
    }

    public static void clearDebugBreakpoints() {
        debugBreakpoints.clear();
    }

    private static String buildBreakpointKey(String sequenceName, int stepIndex, int actionIndex) {
        String sequence = sequenceName == null ? "" : sequenceName.trim();
        if (sequence.isEmpty() || stepIndex < 0 || actionIndex < 0) {
            return "";
        }
        return sequence + "#" + stepIndex + "#" + actionIndex;
    }

    private int countBreakpointsForSequence(String sequenceName) {
        String sequence = sequenceName == null ? "" : sequenceName.trim();
        if (sequence.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String key : debugBreakpoints) {
            if (key != null && key.startsWith(sequence + "#")) {
                count++;
            }
        }
        return count;
    }

    private Map<String, String> buildVariablePreview() {
        return buildVariablePreview(10);
    }

    private Map<String, String> buildVariablePreview(int limit) {
        LinkedHashMap<String, String> preview = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : runtimeVariables.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            preview.put(entry.getKey(), LegacyActionRuntime.stringifyValue(entry.getValue()));
            if (++count >= Math.max(1, limit)) {
                break;
            }
        }
        return preview;
    }

    private void recordDebugTrace(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        String trimmed = message.trim();
        debugTraceLines.addLast(trimmed);
        while (debugTraceLines.size() > 14) {
            debugTraceLines.removeFirst();
        }
        appendExecutionLogEvent("trace", trimmed);
    }

    private void appendExecutionLogEvent(String type, String message) {
        if (executionLogSessionId == null || executionLogSessionId.trim().isEmpty()) {
            return;
        }
        ExecutionLogManager.appendEvent(executionLogSessionId, type, currentStepIndex, actionIndex, message, status,
                buildVariablePreview(12));
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
        finishExecutionLogSessionIfNeeded();
        if (currentSequence == null) {
            return;
        }
        executionLogSessionId = ExecutionLogManager.startSession(currentSequence.getName(), backgroundRunner,
                buildVariablePreview(12));
        resetExecutionResultState();
        ExecutionLogManager.appendEvent(executionLogSessionId, "start", currentStepIndex, actionIndex,
                "执行开始: " + currentSequence.getName(), status, buildVariablePreview(12));
    }

    private void finishExecutionLogSessionIfNeeded() {
        if (executionLogSessionId == null || executionLogSessionId.trim().isEmpty()) {
            return;
        }
        String finishReason = executionResultReason == null || executionResultReason.trim().isEmpty()
                ? (executionResultSuccess ? "执行完成" : "执行停止")
                : executionResultReason;
        ExecutionLogManager.finishSession(executionLogSessionId, executionResultSuccess, finishReason, status,
                buildVariablePreview(12));
        executionLogSessionId = "";
    }

    private void pauseForDebugInternal(String reason) {
        if (!tracking || isPaused) {
            return;
        }
        this.isPaused = true;
        this.pausedByGui = false;
        this.pausedForDebug = true;
        EmbeddedNavigationHandler.INSTANCE.pause();
        if (!getStatus().contains(" | " + I18n.format("status.path.paused"))) {
            setStatus(getStatus() + " | " + I18n.format("status.path.paused"));
        }
        recordDebugTrace(reason);
    }

    private void consumeDebugProgress(String message) {
        if (message != null && !message.trim().isEmpty()) {
            recordDebugTrace(message);
        }
        if (debugStepArmed) {
            debugStepArmed = false;
            pauseForDebugInternal("单步执行完成");
        }
    }

    private void runWithExecutionContext(Runnable action) {
        PathSequenceEventListener previous = ACTION_EXECUTION_CONTEXT.get();
        ACTION_EXECUTION_CONTEXT.set(this);
        try {
            action.run();
        } finally {
            if (previous == null) {
                ACTION_EXECUTION_CONTEXT.remove();
            } else {
                ACTION_EXECUTION_CONTEXT.set(previous);
            }
        }
    }

    public void pauseByGui() {
        if (isTracking() && !isPaused) {
            this.isPaused = true;
            this.pausedByGui = true;
            this.pausedForDebug = false;
            EmbeddedNavigationHandler.INSTANCE.pause();
            releaseResources();
            setStatus(getStatus() + " | " + I18n.format("status.path.paused_gui"));
            zszlScriptMod.LOGGER.info(I18n.format("log.path.paused_by_gui"));
            recordDebugTrace("GUI 打开，序列暂停");
        }
    }

    public boolean wasPausedByGui() {
        return this.pausedByGui;
    }

    public void pause() {
        if (!isTracking() || isPaused) {
            return;
        }
        this.isPaused = true;
        this.pausedByGui = false;
        this.pausedForDebug = false;
        EmbeddedNavigationHandler.INSTANCE.pause();
        releaseResources();
        setStatus(getStatus() + " | " + I18n.format("status.path.paused"));
        zszlScriptMod.LOGGER.info(I18n.format("log.path.paused"));
        recordDebugTrace("序列暂停");
    }

    public void resume() {
        if (!isPaused) {
            return;
        }
        this.isPaused = false;
        this.pausedByGui = false;
        this.pausedForDebug = false;
        EmbeddedNavigationHandler.INSTANCE.resume();

        String pausedGuiSuffix = " | " + I18n.format("status.path.paused_gui");
        String pausedSuffix = " | " + I18n.format("status.path.paused");
        if (getStatus().contains(pausedGuiSuffix)) {
            setStatus(getStatus().replace(pausedGuiSuffix, ""));
        } else if (getStatus().contains(pausedSuffix)) {
            setStatus(getStatus().replace(pausedSuffix, ""));
        }
        zszlScriptMod.LOGGER.info(I18n.format("log.path.resumed"));
        recordDebugTrace("序列恢复");
    }

    public void startTracking(PathSequence sequence, int remainingLoops) {
        startTracking(sequence, remainingLoops, null);
    }

    public void startTracking(PathSequence sequence, int remainingLoops, Map<String, Object> initialSequenceVariables) {
        releaseResources();
        this.currentSequence = sequence;
        this.currentStepIndex = 0;
        this.actionIndex = 0;
        this.tracking = true;
        this.atTarget = false;
        this.tickDelay = 0;
        this.remainingLoops = remainingLoops;
        this.isPerformingExplicitDelay = false;
        this.explicitDelayNormalizeTo20Tps = false;
        this.explicitDelayRemainingBaselineTicks = 0.0D;
        this.currentStepRetryUsed = 0;
        this.pendingLoopRestart = false;
        this.pausedForDebug = false;
        this.debugStepArmed = false;
        this.debugIgnoreBreakpointKey = "";
        resetStepPathRetryMonitor();
        resetHuntState();
        resetHotbarUseActionState();
        resetAsyncActionState();
        resetWaitConditionState();
        clearDeferredWaitState();
        resetRepeatActionState();
        runtimeVariables.clear();
        this.initialSequenceVariables.clear();
        if (initialSequenceVariables != null) {
            for (Map.Entry<String, Object> entry : initialSequenceVariables.entrySet()) {
                if (entry != null && entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                    String key = entry.getKey().trim();
                    this.initialSequenceVariables.put(key, entry.getValue());
                    runtimeVariables.putSequence(key, entry.getValue());
                }
            }
        }
        runtimeVariables.enterStep(this.currentStepIndex);
        startExecutionLogSession();
        clearDebugTrace();
        currentDebugActionDescription = "";
        recordDebugTrace("序列开始: " + sequence.getName());

        if (PathSequenceManager.isStopSequenceName(sequence.getName())) {
            EntityPlayerSP player = mc.player;
            if (player != null
                    && (player.posX < -1702 && player.posX > -1888 && player.posZ < -2011 && player.posZ > -2056)) {
                this.currentStepIndex = 2;
                runtimeVariables.enterStep(this.currentStepIndex);
            }
        }

        if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE) && mc.player != null) {
            mc.player.sendMessage(new TextComponentString(
                    I18n.format("msg.path.debug.sequence_start", sequence.getName())));
        }

        initializeStepPathRetryMonitor(mc.player);

        MinecraftForge.EVENT_BUS.register(this);
        zszlScriptMod.LOGGER.info(I18n.format("log.path.tracking_started") + sequence.getName());
    }

    public void stopTracking() {
        if (tracking || pendingLoopRestart) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE) && mc.player != null) {
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.path.debug.sequence_stop",
                        currentSequence != null ? currentSequence.getName() : I18n.format("msg.common.unknown"))));
            }
            this.tracking = false;
            this.pendingLoopRestart = false;
            this.pausedForDebug = false;
            this.debugStepArmed = false;
            this.debugIgnoreBreakpointKey = "";
            resetStepPathRetryMonitor();
            recordDebugTrace("序列停止: " + (currentSequence != null ? currentSequence.getName() : "unknown"));
            finishExecutionLogSessionIfNeeded();
            this.currentSequence = null;
            MinecraftForge.EVENT_BUS.unregister(this);
            status = I18n.format("status.path.stopped");
            resetHuntState();
            resetHotbarUseActionState();
            resetAsyncActionState();
            resetWaitConditionState();
            resetRepeatActionState();
            releaseResources();
            runtimeVariables.clear();
            if (backgroundRunner) {
                backgroundRunners.remove(this);
            } else {
                GuiInventory.isLooping = false;
                loopDelayTicks = 20;
                PathSequenceManager.clearRunSequenceCallStack();
            }
            zszlScriptMod.LOGGER.info(I18n.format("log.path.tracking_stopped"));
        }
    }

    public ProgressSnapshot captureProgressSnapshot() {
        if (!tracking || currentSequence == null) {
            return null;
        }
        return new ProgressSnapshot(
                currentSequence.getName(),
                currentStepIndex,
                actionIndex,
                atTarget,
                remainingLoops,
                tickDelay,
                isPerformingExplicitDelay,
                status,
                runtimeVariables.captureSnapshot(),
                currentStepRetryUsed);
    }

    public boolean resumeFromSnapshot(PathSequence sequence, ProgressSnapshot snapshot) {
        if (sequence == null || snapshot == null || sequence.getSteps().isEmpty()) {
            return false;
        }

        int maxStep = sequence.getSteps().size() - 1;
        int resumeStep = Math.max(0, Math.min(snapshot.getStepIndex(), maxStep));

        this.currentSequence = sequence;
        this.currentStepIndex = resumeStep;
        this.actionIndex = Math.max(0, snapshot.getActionIndex());
        this.tracking = true;
        this.atTarget = snapshot.isAtTarget();
        this.tickDelay = Math.max(0, snapshot.getTickDelay());
        this.remainingLoops = snapshot.getRemainingLoops();
        this.isPerformingExplicitDelay = snapshot.isExplicitDelay();
        this.explicitDelayNormalizeTo20Tps = false;
        this.explicitDelayRemainingBaselineTicks = this.tickDelay;
        this.currentStepRetryUsed = Math.max(0, snapshot.getStepRetryUsed());
        this.isPaused = false;
        this.pausedByGui = false;
        this.pausedForDebug = false;
        this.debugStepArmed = false;
        this.debugIgnoreBreakpointKey = "";
        resetStepPathRetryMonitor();
        resetHuntState();
        resetHotbarUseActionState();
        resetAsyncActionState();
        resetWaitConditionState();
        clearDeferredWaitState();
        resetRepeatActionState();
        this.initialSequenceVariables.clear();
        runtimeVariables.clear();
        runtimeVariables.enterStep(this.currentStepIndex);
        runtimeVariables.restoreSnapshot(snapshot.getVariableSnapshot());
        startExecutionLogSession();
        clearDebugTrace();
        currentDebugActionDescription = "";
        recordDebugTrace("恢复序列: " + sequence.getName() + " @ step=" + this.currentStepIndex + ", action=" + this.actionIndex);
        this.status = (snapshot.getStatus() == null || snapshot.getStatus().trim().isEmpty())
                ? sequence.getName()
                : snapshot.getStatus();
        GuiInventory.isLooping = true;

        if (!this.atTarget) {
            double[] target = sequence.getSteps().get(this.currentStepIndex).getGotoPoint();
            if (!Double.isNaN(target[0])) {
                EmbeddedNavigationHandler.INSTANCE.startGoto(target[0], target[1], target[2]);
                initializeStepPathRetryMonitor(mc.player);
            } else {
                EmbeddedNavigationHandler.INSTANCE.stop();
                resetStepPathRetryMonitor();
            }
        } else {
            EmbeddedNavigationHandler.INSTANCE.stop();
            resetStepPathRetryMonitor();
        }

        MinecraftForge.EVENT_BUS.register(this);
        return true;
    }

    // --- 新增：启动狩猎模式的方法 ---
    public void startHunting(JsonObject params) {
        resetHuntState();
        this.isHunting = true;
        double radius = readHuntDoubleParam(params, "radius", 3.0D);
        boolean autoAttack = readHuntBooleanParam(params, "autoAttack", false);
        String attackMode = normalizeHuntAttackMode(params != null && params.has("attackMode")
                ? params.get("attackMode").getAsString()
                : KillAuraHandler.ATTACK_MODE_NORMAL);
        String attackSequenceName = params != null && params.has("attackSequenceName")
                ? params.get("attackSequenceName").getAsString().trim()
                : "";
        boolean aimLockEnabled = readHuntBooleanParam(params, "huntAimLockEnabled", true);
        double trackingDistance = readHuntDoubleParam(params, "trackingDistance", 1.0D);
        this.huntRadius = Math.max(0.0D, radius);
        this.huntAutoAttack = autoAttack;
        this.huntAttackMode = attackMode;
        this.huntAttackSequenceName = attackSequenceName;
        this.huntAimLockEnabled = aimLockEnabled;
        this.huntPendingCompleteAfterSequence = false;
        this.huntTrackingDistanceSq = trackingDistance * trackingDistance;
        this.huntMode = readHuntModeParam(params, "huntMode", KillAuraHandler.HUNT_MODE_FIXED_DISTANCE);
        this.huntOrbitEnabled = KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equals(this.huntMode)
                && readHuntBooleanParam(params, "huntOrbitEnabled", false);
        this.huntChaseIntervalEnabled = readHuntBooleanParam(params, "huntChaseIntervalEnabled", false);
        this.huntChaseIntervalTicks = Math.max(0,
                (int) Math.round(readHuntDoubleParam(params, "huntChaseIntervalSeconds", 0.0D) * 20.0D));
        int attackCount = readHuntIntParam(params, "attackCount", -1);
        this.huntAttackRemaining = attackCount > 0 ? attackCount : -1;
        this.huntNoTargetSkipCount = Math.max(0, readHuntIntParam(params, "noTargetSkipCount", 0));
        
        // 读取新增参数
        this.huntShowRange = readHuntBooleanParam(params, "showHuntRange", false);
        this.huntIgnoreInvisible = readHuntBooleanParam(params, "ignoreInvisible", false);
        
        // 记录搜怪中心点（玩家当前位置）
        if (mc.player != null) {
            this.huntCenterX = mc.player.posX;
            this.huntCenterY = mc.player.posY;
            this.huntCenterZ = mc.player.posZ;
        }

        boolean hasTargetGroupConfig = params != null
                && (params.has("targetHostile") || params.has("targetPassive") || params.has("targetPlayers"));
        if (hasTargetGroupConfig) {
            this.huntTargetHostile = readHuntBooleanParam(params, "targetHostile", false);
            this.huntTargetPassive = readHuntBooleanParam(params, "targetPassive", false);
            this.huntTargetPlayers = readHuntBooleanParam(params, "targetPlayers", false);
            this.huntRestrictTargetGroups = this.huntTargetHostile || this.huntTargetPassive || this.huntTargetPlayers;
        } else {
            this.huntTargetHostile = true;
            this.huntTargetPassive = false;
            this.huntTargetPlayers = false;
            this.huntRestrictTargetGroups = true;
        }

        this.huntEnableNameWhitelist = readHuntBooleanParam(params, "enableNameWhitelist", false);
        this.huntEnableNameBlacklist = readHuntBooleanParam(params, "enableNameBlacklist", false);
        this.huntNameWhitelist.addAll(readHuntNameList(params, "nameWhitelist", "nameWhitelistText"));
        this.huntNameBlacklist.addAll(readHuntNameList(params, "nameBlacklist", "nameBlacklistText"));

        EmbeddedNavigationHandler.INSTANCE.stop();
        setStatus(getStatus().split(" \\| ")[0] + " | " + I18n.format("status.path.hunting"));
        zszlScriptMod.LOGGER.info(
                "进入中心搜怪击杀: 半径={}, 自动攻击={}, 攻击方式={}, 攻击序列={}, 视角锁定={}, 追击模式={}, 距离={}, 自动绕圈={}, 追怪间隔={}, 攻击次数={}, 无目标跳过={}, 目标类型[敌对={}, 被动={}, 玩家={}], 白名单={}, 黑名单={}, 显示范围={}, 忽略隐身={}",
                this.huntRadius,
                this.huntAutoAttack,
                this.huntAttackMode,
                this.huntAttackSequenceName.isEmpty() ? "未设置" : this.huntAttackSequenceName,
                this.huntAimLockEnabled,
                this.huntMode,
                trackingDistance,
                this.huntOrbitEnabled,
                this.huntChaseIntervalEnabled ? (this.huntChaseIntervalTicks / 20.0D + "s") : "关闭",
                this.huntAttackRemaining > 0 ? this.huntAttackRemaining : "不限",
                this.huntNoTargetSkipCount,
                this.huntRestrictTargetGroups ? this.huntTargetHostile : "不限",
                this.huntRestrictTargetGroups ? this.huntTargetPassive : "不限",
                this.huntRestrictTargetGroups ? this.huntTargetPlayers : "不限",
                this.huntEnableNameWhitelist ? this.huntNameWhitelist : "关闭",
                this.huntEnableNameBlacklist ? this.huntNameBlacklist : "关闭",
                this.huntShowRange,
                this.huntIgnoreInvisible);
    }
    // --- 新增结束 ---

    // --- 新增：启动跟随实体模式的方法 ---
    public void startFollowingEntity(JsonObject params) {
        resetFollowEntityState();
        this.isFollowingEntity = true;
        
        this.followEntityType = params != null && params.has("entityType") 
                ? params.get("entityType").getAsString() : "player";
        this.followTargetName = params != null && params.has("targetName") 
                ? params.get("targetName").getAsString().trim() : "";
        this.followSearchRadius = readHuntDoubleParam(params, "searchRadius", 16.0D);
        this.followDistance = readHuntDoubleParam(params, "followDistance", 3.0D);
        this.followTimeoutSeconds = readHuntIntParam(params, "timeout", 0);
        this.followStopOnLost = !params.has("stopOnLost") || params.get("stopOnLost").getAsBoolean();
        this.followStartTime = System.currentTimeMillis();
        
        EmbeddedNavigationHandler.INSTANCE.stop();
        setStatus(getStatus().split(" \\| ")[0] + " | 跟随实体中");
        zszlScriptMod.LOGGER.info(
                "开始跟随实体: 类型={}, 名称={}, 搜索半径={}, 跟随距离={}, 超时={}秒, 丢失后停止={}",
                this.followEntityType,
                this.followTargetName.isEmpty() ? "最近目标" : this.followTargetName,
                this.followSearchRadius,
                this.followDistance,
                this.followTimeoutSeconds > 0 ? this.followTimeoutSeconds : "无限",
                this.followStopOnLost);
    }
    // --- 新增结束 ---

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // ==================== 核心修复 ====================
        // 在方法最开始添加此检查，确保只处理主玩家的事件。
        if (mc.player == null || event.player != mc.player) {
            return;
        }
        // ==================== 修复结束 ====================

        if (event.phase != TickEvent.Phase.START || event.side != Side.CLIENT) {
            return;
        }

        if (isPaused) {
            return;
        }

        // --- 核心修复 #1：将 event.player 强制转换为 (EntityPlayerSP) ---
        // 我们在下面已经检查了 event.player.equals(mc.player)，所以这个转换是安全的。
        if (isHunting) {
            executeHuntTick((EntityPlayerSP) event.player);
            return; // 在狩猎模式下，不执行后续的路径跟踪逻辑
        }
        
        if (isFollowingEntity) {
            executeFollowEntityTick((EntityPlayerSP) event.player);
            return; // 在跟随实体模式下，不执行后续的路径跟踪逻辑
        }
        // --- 修复结束 ---

        if (!tracking)
            return;
        // 这一行现在是多余的，但保留也无妨
        if (event.player == null || !event.player.equals(mc.player))
            return;

        EntityPlayerSP player = (EntityPlayerSP) event.player;

        if (tickDelay > 0) {
            if (isPerformingExplicitDelay && explicitDelayNormalizeTo20Tps) {
                explicitDelayRemainingBaselineTicks = Math.max(0.0D,
                        explicitDelayRemainingBaselineTicks - (1.0D / ModUtils.getCurrentTimerSpeedMultiplier()));
                tickDelay = Math.max(0, (int) Math.ceil(explicitDelayRemainingBaselineTicks));
            } else {
                tickDelay--;
            }
            if (isPerformingExplicitDelay && ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE)) {
                double secondsLeft = explicitDelayNormalizeTo20Tps
                        ? Math.max(0.0D, explicitDelayRemainingBaselineTicks) / 20.0D
                        : tickDelay / 20.0D;
                String baseStatus = status.split(" \\| ")[0];
                setStatus(I18n.format("status.path.delaying", baseStatus, secondsLeft));
                if (tickDelay > 0 && tickDelay % 20 == 0 && mc.player != null) {
                    String chatMessage = I18n.format("msg.path.debug.delaying_left", secondsLeft);
                    mc.player.sendMessage(new TextComponentString(chatMessage));
                }
            }
            return;
        }

        if (isPerformingExplicitDelay) {
            isPerformingExplicitDelay = false;
            explicitDelayNormalizeTo20Tps = false;
            explicitDelayRemainingBaselineTicks = 0.0D;
            if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE)) {
                setStatus(status.split(" \\| ")[0]);
            }
        }

        List<PathStep> steps = currentSequence.getSteps();

        if (currentStepIndex >= steps.size()) {
            if (shouldStopNavigationOnFinish()) {
                EmbeddedNavigationHandler.INSTANCE.stop();
            }

            if (!backgroundRunner && PathSequenceManager.resumeCallerSequenceAfterAction(player)) {
                markExecutionResult(true, "子序列执行完成并恢复调用方");
                finishExecutionLogSessionIfNeeded();
                return;
            }

            if (remainingLoops != 0) {
                if (remainingLoops > 0) {
                    remainingLoops--;
                }
                if (remainingLoops != 0) {
                    markExecutionResult(true, "单轮执行完成，等待下一轮");
                    finishExecutionLogSessionIfNeeded();
                    status = I18n.format("status.path.wait_next_loop");
                    tracking = false;
                    pendingLoopRestart = true;
                    MinecraftForge.EVENT_BUS.unregister(this);

                    int delay = currentSequence.getLoopDelayTicks();
                    PathSequence loopingSequence = currentSequence;
                    int nextRemainingLoops = remainingLoops;
                    zszlScriptMod.LOGGER.info(I18n.format("log.path.loop_finished_wait_next"), delay);
                    new ModUtils.DelayAction(delay, () -> {
                        if (!pendingLoopRestart) {
                            return;
                        }
                        pendingLoopRestart = false;
                        if (backgroundRunner) {
                            if (loopingSequence != null) {
                                startTracking(loopingSequence, nextRemainingLoops, this.initialSequenceVariables);
                                setStatus(loopingSequence.getName() + " | 后台执行");
                                resume();
                                primeBackgroundNavigation(this, loopingSequence);
                            }
                        } else if (currentSequence != null) {
                            PathSequenceManager.startNextLoopWithVariables(currentSequence.getName(),
                                    this.initialSequenceVariables);
                        }
                    }).accept(player);
                    return;
                } else {
                    status = I18n.format("status.path.completed_times", GuiInventory.loopCounter);
                    markExecutionResult(true, "序列执行完成");
                    stopTracking();
                }
            } else {
                markExecutionResult(true, "序列执行完成");
                stopTracking();
            }
            return;
        }

        PathStep currentStep = steps.get(currentStepIndex);
        double[] target = currentStep.getGotoPoint();

        if (!atTarget) {
            if (!ensureResources(resolveMovementResources(target), "move_to_step_target")) {
                return;
            }
            if (hasReachedGotoTarget(player, target)) {
                if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE) && !atTarget && mc.player != null) {
                    mc.player.sendMessage(new TextComponentString(
                            I18n.format("msg.path.debug.reached_step_target", currentStepIndex)));
                }
                zszlScriptMod.LOGGER.info(I18n.format("log.path.reached_target"), currentStepIndex,
                        currentSequence.getName());
                atTarget = true;
                currentStepRetryUsed = 0;
                resetStepPathRetryMonitor();
                actionIndex = 0;
            } else if (handleCurrentStepPathRetry(player, currentStep)) {
                return;
            }
        } else {
            List<ActionData> actions = currentStep.getActions();
            int immediateBurstCount = 0;

            while (true) {
                if (handleRepeatActionBoundary(actions)) {
                    return;
                }

                if (handleDeferredWaitAction(player)) {
                    return;
                }

                if (actionIndex >= actions.size()) {
                    if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE) && mc.player != null) {
                        mc.player.sendMessage(new TextComponentString(
                                I18n.format("msg.path.debug.step_actions_done", currentStepIndex)));
                    }
                    currentStepIndex++;
                    actionIndex = 0;
                    atTarget = false;
                    currentStepRetryUsed = 0;
                    resetStepPathRetryMonitor();
                    releaseResources();
                    resetAsyncActionState();
                    resetRepeatActionState();
                    runtimeVariables.enterStep(currentStepIndex);
                    if (currentStepIndex < steps.size()) {
                        restartCurrentStepTarget(steps.get(currentStepIndex));
                    } else {
                        if (shouldStopNavigationForStepTransition()) {
                            EmbeddedNavigationHandler.INSTANCE.stop();
                        }
                        resetStepPathRetryMonitor();
                    }
                    consumeDebugProgress("进入下一步骤: " + currentStepIndex);
                    return;
                }

                ActionData actionData = actions.get(actionIndex);
                ActionData resolvedActionData = resolveRuntimeActionData(actionData, player);
                currentDebugActionDescription = resolvedActionData == null ? "" : resolvedActionData.getDescription();
                String breakpointKey = buildBreakpointKey(currentSequence == null ? "" : currentSequence.getName(),
                        currentStepIndex, actionIndex);
                if (!breakpointKey.isEmpty()
                        && !breakpointKey.equals(debugIgnoreBreakpointKey)
                        && hasDebugBreakpoint(currentSequence == null ? "" : currentSequence.getName(),
                                currentStepIndex, actionIndex)) {
                    pauseForDebugInternal("命中断点: step=" + currentStepIndex + ", action=" + actionIndex);
                    return;
                }
                if (!breakpointKey.equals(debugIgnoreBreakpointKey)) {
                    debugIgnoreBreakpointKey = "";
                }

                PathSafetyManager.SafetyDecision safetyDecision = PathSafetyManager
                        .evaluateAction(resolvedActionData == null ? "" : resolvedActionData.type);
                if (safetyDecision.isBlocked()) {
                    recordDebugTrace("安全模式跳过: "
                            + (resolvedActionData == null ? "unknown" : resolvedActionData.type)
                            + " / " + safetyDecision.getReason());
                    actionIndex++;
                    applyBuiltinSequenceDelay();
                    consumeDebugProgress("安全模式已跳过危险动作");
                    return;
                }

                if (!ensureResources(resolveActionResources(resolvedActionData), currentDebugActionDescription)) {
                    return;
                }

                if (handleRuntimeControlAction(player, actions, actionData, resolvedActionData)) {
                    return;
                }

                if ("use_hotbar_item".equalsIgnoreCase(resolvedActionData.type)) {
                    handleUseHotbarItemAction(player, resolvedActionData);
                    return;
                }

                if (handleConditionalOrWaitAction(player, resolvedActionData)) {
                    return;
                }

                Consumer<EntityPlayerSP> action = PathSequenceManager.parseAction(resolvedActionData.type,
                        resolvedActionData.params);

                if (action == null) {
                    handleCurrentStepFailure("action_parse_failed:" + resolvedActionData.type, null);
                    return;
                }

                if (handleAsyncAction(player, resolvedActionData, action)) {
                    return;
                }

                if (action instanceof ModUtils.DelayAction) {
                    ModUtils.DelayAction delayAction = (ModUtils.DelayAction) action;
                    recordDebugTrace("delay action: " + currentDebugActionDescription);
                    try {
                        runWithExecutionContext(() -> delayAction.accept(player));
                    } catch (Exception e) {
                        handleCurrentStepFailure("delay_action_exception:" + resolvedActionData.type, e);
                        return;
                    }
                    tickDelay = Math.max(0, delayAction.getDelayTicks());
                    isPerformingExplicitDelay = tickDelay > 0;
                    explicitDelayNormalizeTo20Tps = isPerformingExplicitDelay
                            && delayAction.shouldNormalizeDelayTo20Tps();
                    explicitDelayRemainingBaselineTicks = explicitDelayNormalizeTo20Tps
                            ? Math.max(0, delayAction.getConfiguredDelayTicks())
                            : 0.0D;
                    if (tickDelay > 0) {
                        zszlScriptMod.LOGGER.info(I18n.format("log.path.delay_ticks"), tickDelay);
                    }
                    releaseResources();
                    actionIndex++;
                    consumeDebugProgress("执行延迟动作");
                    if (!canContinueImmediateActionBurst(resolvedActionData, tickDelay, ++immediateBurstCount)) {
                        return;
                    }
                    continue;
                }

                if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE) && mc.player != null) {
                    mc.player.sendMessage(new TextComponentString(
                            I18n.format("msg.path.debug.execute_action", currentStepIndex, actionIndex,
                                    resolvedActionData.getDescription())));
                }
                recordDebugTrace("action: step=" + currentStepIndex + ", action=" + actionIndex + " -> "
                        + currentDebugActionDescription);
                try {
                    runWithExecutionContext(() -> action.accept(player));
                    zszlScriptMod.LOGGER.info(I18n.format("log.path.execute_action"), actionIndex, currentStepIndex);
                } catch (Exception e) {
                    handleCurrentStepFailure("action_exception:" + resolvedActionData.type, e);
                    return;
                }
                if ("hunt".equalsIgnoreCase(resolvedActionData.type)) {
                    releaseResources();
                    consumeDebugProgress("中心搜怪动作运行中");
                    return;
                }
                if ("follow_entity".equalsIgnoreCase(resolvedActionData.type)) {
                    releaseResources();
                    consumeDebugProgress("跟随实体动作运行中");
                    return;
                }
                releaseResources();
                actionIndex++;
                applyBuiltinSequenceDelay();
                consumeDebugProgress("执行动作完成");
                return;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc == null || mc.player == null || mc.playerController == null || mc.world == null) {
            return;
        }
    }

    private void resetHotbarUseActionState() {
        this.hotbarUseActionRunning = false;
        this.hotbarUseItemName = "";
        this.hotbarUseMatchMode = AutoUseItemRule.MatchMode.CONTAINS;
        this.hotbarUseMode = AutoUseItemRule.UseMode.RIGHT_CLICK;
        this.hotbarUseChangeLocalSlot = false;
        this.hotbarUseSwitchItemDelayTicks = 0;
        this.hotbarUseSwitchDelayTicks = 0;
        this.hotbarUseSwitchBackDelayTicks = 0;
        this.hotbarUseRemainingCount = 0;
        this.hotbarUseIntervalTicks = 0;
        this.hotbarUseWaitTicks = 0;
    }

    private void resetAsyncActionState() {
        this.pendingAsyncActionType = "";
        this.pendingAsyncActionStepIndex = -1;
        this.pendingAsyncActionIndex = -1;
    }

    private boolean canContinueImmediateActionBurst(ActionData actionData, int currentDelayTicks, int burstCount) {
        if (!isZeroDelayAutoChestClick(actionData) || currentDelayTicks > 0) {
            return false;
        }
        if (burstCount < 128) {
            return true;
        }
        zszlScriptMod.LOGGER.warn("连续执行零延迟点击箱子格子动作过多，已切到下一 tick 继续。");
        tickDelay = 1;
        return false;
    }

    private boolean isZeroDelayAutoChestClick(ActionData actionData) {
        if (actionData == null || actionData.type == null || !"autochestclick".equalsIgnoreCase(actionData.type)) {
            return false;
        }
        return !actionData.params.has("delayTicks") || Math.max(0, actionData.params.get("delayTicks").getAsInt()) <= 0;
    }

    private boolean handleAsyncAction(EntityPlayerSP player, ActionData actionData, Consumer<EntityPlayerSP> action) {
        String type = actionData == null || actionData.type == null ? "" : actionData.type.trim().toLowerCase(Locale.ROOT);
        if (!"transferitemstowarehouse".equals(type)
                && !"move_inventory_items_to_chest_slots".equals(type)
                && !"warehouse_auto_deposit".equals(type)) {
            return false;
        }

        boolean samePendingAction = type.equals(pendingAsyncActionType)
                && pendingAsyncActionStepIndex == currentStepIndex
                && pendingAsyncActionIndex == actionIndex;
        if (samePendingAction) {
            if (isAsyncActionInProgress(type)) {
                tickDelay = 2;
                return true;
            }
            resetAsyncActionState();
            releaseResources();
            actionIndex++;
            applyBuiltinSequenceDelay();
            return true;
        }

        if (ModConfig.isDebugFlagEnabled(DebugModule.PATH_SEQUENCE) && mc.player != null) {
            mc.player.sendMessage(new TextComponentString(
                    I18n.format("msg.path.debug.execute_action", currentStepIndex, actionIndex,
                            actionData.getDescription())));
        }
        try {
            runWithExecutionContext(() -> action.accept(player));
            zszlScriptMod.LOGGER.info(I18n.format("log.path.execute_action"), actionIndex, currentStepIndex);
        } catch (Exception e) {
            handleCurrentStepFailure("async_action_exception:" + type, e);
            return true;
        }

        if (isAsyncActionInProgress(type)) {
            pendingAsyncActionType = type;
            pendingAsyncActionStepIndex = currentStepIndex;
            pendingAsyncActionIndex = actionIndex;
            tickDelay = 2;
            return true;
        }

        resetAsyncActionState();
        releaseResources();
        actionIndex++;
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
        if ("warehouse_auto_deposit".equals(actionType)) {
            return WarehouseEventHandler.isAutoDepositRouteRunning();
        }
        return false;
    }

    private void resetWaitConditionState() {
        this.waitConditionRunning = false;
        this.waitConditionElapsedTicks = 0;
        this.waitConditionStartCapturedUpdateVersion = 0L;
        this.waitConditionStartCapturedRecaptureVersion = 0L;
        this.waitConditionStartPacketTextVersion = 0L;
    }

    private void clearDeferredWaitState() {
        this.deferredWaitActionData = null;
        this.deferredWaitResumeActionIndex = -1;
        this.deferredWaitElapsedTicks = 0;
        this.deferredWaitStartCapturedUpdateVersion = 0L;
        this.deferredWaitStartCapturedRecaptureVersion = 0L;
        this.deferredWaitStartPacketTextVersion = 0L;
    }

    private void resetRepeatActionState() {
        this.repeatActionRunning = false;
        this.repeatActionStepIndex = -1;
        this.repeatActionHeaderIndex = -1;
        this.repeatActionBodyStartIndex = -1;
        this.repeatActionBodyEndIndex = -1;
        this.repeatActionRemainingLoops = 0;
        this.repeatActionIteration = 0;
        this.repeatActionLoopVarName = "loop_index";
    }

    private void resetStepPathRetryMonitor() {
        this.currentStepIdleTicks = 0;
        this.currentStepIdleAnnounced = false;
        this.currentStepLastMovementX = Double.NaN;
        this.currentStepLastMovementY = Double.NaN;
        this.currentStepLastMovementZ = Double.NaN;
    }

    private boolean stepHasGotoTarget(PathStep step) {
        return step != null && step.hasGotoTarget();
    }

    private void initializeStepPathRetryMonitor(EntityPlayerSP player) {
        resetStepPathRetryMonitor();
        if (player == null) {
            return;
        }
        this.currentStepLastMovementX = player.posX;
        this.currentStepLastMovementY = player.posY;
        this.currentStepLastMovementZ = player.posZ;
    }

    private void sendPathRetryMessage(String message, net.minecraft.util.text.TextFormatting color) {
        if (mc.player == null || message == null || message.trim().isEmpty()) {
            return;
        }
        mc.player.sendMessage(new TextComponentString(color + "[路径序列] " + message));
    }

    private void ensureSetVarDefaultValue(String varName, JsonObject rawParams, EntityPlayerSP player) {
        if (varName == null || varName.trim().isEmpty() || rawParams == null) {
            return;
        }
        Object existing = LegacyActionRuntime.getRuntimeValue(varName, runtimeVariables, player, currentSequence,
                currentStepIndex, actionIndex);
        if (existing != null) {
            return;
        }
        runtimeVariables.put(varName, LegacyActionRuntime.inferAssignedValueDefault(rawParams));
    }

    private String summarizeSetVarFailure(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return "表达式执行失败";
        }
        String message = error.getMessage().trim();
        int lineBreak = message.indexOf('\n');
        if (lineBreak >= 0) {
            message = message.substring(0, lineBreak).trim();
        }
        return message.isEmpty() ? "表达式执行失败" : message;
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
        if (!status.contains(" | ")) {
            return;
        }
        String base = status.split(" \\| ")[0];
        if (base == null || base.trim().isEmpty()) {
            base = currentSequence.getName();
        }
        setStatus(base + suffix);
    }

    private void resetSequenceRuntimeToInitialVariables() {
        runtimeVariables.clear();
        for (Map.Entry<String, Object> entry : initialSequenceVariables.entrySet()) {
            if (entry != null && entry.getKey() != null && !entry.getKey().trim().isEmpty()) {
                runtimeVariables.putSequence(entry.getKey().trim(), entry.getValue());
            }
        }
    }

    private boolean restartSequenceFromBeginning(String reason) {
        if (currentSequence == null || currentSequence.getSteps().isEmpty()) {
            markExecutionResult(false, reason == null ? "seek_retry_restart_failed" : reason);
            stopTracking();
            return true;
        }

        resetHuntState();
        resetHotbarUseActionState();
        resetAsyncActionState();
        resetWaitConditionState();
        clearDeferredWaitState();
        resetRepeatActionState();
        releaseResources();

        this.currentStepIndex = 0;
        this.actionIndex = 0;
        this.atTarget = false;
        this.tickDelay = 1;
        this.isPerformingExplicitDelay = false;
        this.explicitDelayNormalizeTo20Tps = false;
        this.explicitDelayRemainingBaselineTicks = 0.0D;
        this.currentStepRetryUsed = 0;
        this.currentDebugActionDescription = "";
        resetSequenceRuntimeToInitialVariables();
        runtimeVariables.enterStep(this.currentStepIndex);
        recordDebugTrace("序列从头重试: " + (reason == null ? "unknown" : reason));
        restartCurrentStepTarget(currentSequence.getSteps().get(this.currentStepIndex));
        setStatus(currentSequence.getName() + (backgroundRunner ? " | 后台执行" : "") + " | 寻路重试已回到开头");
        return true;
    }

    private boolean handleCurrentStepPathRetry(EntityPlayerSP player, PathStep step) {
        if (player == null || step == null || !stepHasGotoTarget(step)) {
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

        double dx = player.posX - currentStepLastMovementX;
        double dy = player.posY - currentStepLastMovementY;
        double dz = player.posZ - currentStepLastMovementZ;
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
            int remainingRetries = Math.max(0, retryCount - currentStepRetryUsed);
            sendPathRetryMessage("步骤 " + (currentStepIndex + 1) + " 检测到人物原地不动，"
                    + timeoutSeconds + " 秒后将自动重发寻路命令，剩余重试 " + remainingRetries + " 次。",
                    net.minecraft.util.text.TextFormatting.YELLOW);
            currentStepIdleAnnounced = true;
            setStatus(currentSequence.getName() + (backgroundRunner ? " | 后台执行" : "") + " | 寻路停留检测中");
        }

        if (currentStepIdleTicks < timeoutSeconds * 20) {
            return false;
        }

        resetHotbarUseActionState();
        resetAsyncActionState();
        resetWaitConditionState();
        clearDeferredWaitState();
        resetRepeatActionState();
        releaseResources();

        if (currentStepRetryUsed < retryCount) {
            currentStepRetryUsed++;
            this.actionIndex = 0;
            this.currentDebugActionDescription = "";
            sendPathRetryMessage("步骤 " + (currentStepIndex + 1) + " 寻路停留超时，重新发送寻路命令。剩余重试 "
                    + Math.max(0, retryCount - currentStepRetryUsed) + " 次。",
                    net.minecraft.util.text.TextFormatting.GOLD);
            recordDebugTrace("步骤寻路重试: step=" + currentStepIndex + ", retry=" + currentStepRetryUsed + "/"
                    + retryCount);
            restartCurrentStepTarget(step);
            setStatus(currentSequence.getName() + (backgroundRunner ? " | 后台执行" : "") + " | 寻路重试 "
                    + currentStepRetryUsed + "/" + retryCount);
            tickDelay = 2;
            return true;
        }

        currentStepRetryUsed = 0;
        recordDebugTrace("步骤寻路重试耗尽: step=" + currentStepIndex);
        if ("RESTART_SEQUENCE".equalsIgnoreCase(step.getRetryExhaustedPolicy())) {
            sendPathRetryMessage("步骤 " + (currentStepIndex + 1) + " 寻路重试已耗尽，按设置从序列开头重新执行。",
                    net.minecraft.util.text.TextFormatting.RED);
            return restartSequenceFromBeginning("path_retry_exhausted_restart_sequence");
        }

        sendPathRetryMessage("步骤 " + (currentStepIndex + 1) + " 寻路重试已耗尽，序列已停止。",
                net.minecraft.util.text.TextFormatting.RED);
        markExecutionResult(false, "path_retry_exhausted");
        stopTracking();
        return true;
    }

    private boolean inventoryHasMatchingItem(EntityPlayerSP player, JsonObject params,
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
        String expected = itemName == null ? "" : itemName.trim().toLowerCase(java.util.Locale.ROOT);
        boolean hasExpressionCondition = itemFilterExpressions != null && !itemFilterExpressions.isEmpty();
        boolean hasNameCondition = !expected.isEmpty();
        boolean hasNbtCondition = requiredNbtTags != null && !requiredNbtTags.isEmpty();
        if (!hasExpressionCondition && !hasNameCondition && !hasNbtCondition) {
            return false;
        }
        int totalCount = 0;
        boolean exact = "EXACT".equalsIgnoreCase(matchMode);
        List<ItemStack> mainInventory = player.inventory.mainInventory;
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
                String actual = net.minecraft.util.text.TextFormatting
                        .getTextWithoutFormattingCodes(stack.getDisplayName());
                if (actual == null) {
                    actual = stack.getDisplayName();
                }
                if (actual == null) {
                    continue;
                }
                actual = actual.trim().toLowerCase(java.util.Locale.ROOT);
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
        LinkedHashSet<Integer> result = new LinkedHashSet<>();
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
        }
        return result;
    }

    private String getCurrentGuiTitle() {
        GuiScreen screen = mc.currentScreen;
        if (screen == null) {
            return "";
        }
        if (screen instanceof GuiChest && mc.player != null && mc.player.openContainer instanceof ContainerChest) {
            try {
                IInventory inv = ((ContainerChest) mc.player.openContainer).getLowerChestInventory();
                if (inv != null && inv.getDisplayName() != null) {
                    return inv.getDisplayName().getUnformattedText();
                }
            } catch (Exception ignored) {
            }
        }
        if (screen instanceof GuiMerchant) {
            return "Merchant";
        }
        return screen.getClass().getSimpleName();
    }

    private boolean isPlayerInArea(EntityPlayerSP player, JsonObject params) {
        if (player == null || params == null || !params.has("center")) {
            return false;
        }
        try {
            com.google.gson.JsonArray center = params.getAsJsonArray("center");
            BlockPos centerPos = new BlockPos(center.get(0).getAsDouble(), center.get(1).getAsDouble(),
                    center.get(2).getAsDouble());
            double radius = params.has("radius") ? params.get("radius").getAsDouble() : 3.0D;
            return player.getDistanceSq(centerPos.getX() + 0.5D, centerPos.getY() + 0.5D, centerPos.getZ() + 0.5D) <= radius
                    * radius;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasNearbyMatchingEntity(EntityPlayerSP player, JsonObject params) {
        if (player == null || mc.world == null || params == null) {
            return false;
        }
        String entityName = params.has("entityName") ? params.get("entityName").getAsString().trim() : "";
        double radius = params.has("radius") ? params.get("radius").getAsDouble() : 6.0D;
        if (entityName.isEmpty()) {
            return false;
        }
        String expected = entityName.toLowerCase(java.util.Locale.ROOT);
        List<EntityLivingBase> entities = mc.world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(player.getPosition()).grow(radius));
        for (EntityLivingBase entity : entities) {
            if (entity == null || entity == player || !entity.isEntityAlive()) {
                continue;
            }
            String actual = entity.getName() == null ? "" : entity.getName().toLowerCase(java.util.Locale.ROOT);
            if (actual.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMatchingHudText(JsonObject params) {
        String contains = params.has("contains") ? params.get("contains").getAsString() : "";
        boolean matchBlock = params.has("matchBlock") && params.get("matchBlock").getAsBoolean();
        String separator = params.has("separator") ? params.get("separator").getAsString() : " | ";
        if (contains == null) {
            contains = "";
        }
        contains = contains.trim();
        if (matchBlock) {
            for (com.zszl.zszlScriptMod.utils.HudTextScanner.TextBlock block : com.zszl.zszlScriptMod.utils.HudTextScanner.INSTANCE
                    .getProcessedTextBlocks()) {
                String text = block.getJoinedText(separator);
                if (contains.isEmpty() || text.contains(contains)) {
                    return true;
                }
            }
            return false;
        }
        for (com.zszl.zszlScriptMod.utils.HudTextScanner.CapturedText text : com.zszl.zszlScriptMod.utils.HudTextScanner.INSTANCE
                .getCurrentHudText()) {
            if (contains.isEmpty() || text.text.contains(contains)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateConditionAction(EntityPlayerSP player, ActionData actionData) {
        if (actionData == null || actionData.type == null) {
            return false;
        }
        switch (actionData.type.toLowerCase(java.util.Locale.ROOT)) {
            case "condition_inventory_item":
            case "wait_until_inventory_item":
                return inventoryHasMatchingItem(player, actionData.params, readMainInventorySlotSelection(actionData.params));
            case "condition_gui_title":
            case "wait_until_gui_title":
                String title = actionData.params.has("title") ? actionData.params.get("title").getAsString() : "";
                String currentTitle = getCurrentGuiTitle();
                return !title.trim().isEmpty() && currentTitle != null && currentTitle.contains(title.trim());
            case "condition_player_in_area":
            case "wait_until_player_in_area":
                return isPlayerInArea(player, actionData.params);
            case "condition_entity_nearby":
            case "wait_until_entity_nearby":
                return hasNearbyMatchingEntity(player, actionData.params);
            case "wait_until_hud_text":
                return hasMatchingHudText(actionData.params);
            case "condition_expression":
            case "wait_until_expression":
                List<String> booleanExpressions = readBooleanExpressionList(actionData.params, "expressions", "expression");
                if (booleanExpressions.isEmpty()) {
                    return false;
                }
                for (String expression : booleanExpressions) {
                    try {
                        if (!LegacyActionRuntime.evaluateExpression(
                                expression,
                                actionData.params,
                                runtimeVariables,
                                player,
                                currentSequence,
                                currentStepIndex,
                                actionIndex)) {
                            return false;
                        }
                    } catch (Exception e) {
                        zszlScriptMod.LOGGER.warn("[legacy_path] 表达式条件解析失败: {}", expression, e);
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private void captureWaitStartVersions(ActionData actionData, boolean deferred) {
        String type = actionData == null || actionData.type == null ? ""
                : actionData.type.toLowerCase(java.util.Locale.ROOT);
        String capturedIdKey = actionData != null && actionData.params != null && actionData.params.has("capturedId")
                ? actionData.params.get("capturedId").getAsString().trim()
                : "";

        if (deferred) {
            deferredWaitStartCapturedUpdateVersion = 0L;
            deferredWaitStartCapturedRecaptureVersion = 0L;
            deferredWaitStartPacketTextVersion = 0L;
            if ("wait_until_captured_id".equals(type) && !capturedIdKey.isEmpty()) {
                deferredWaitStartCapturedUpdateVersion = CapturedIdRuleManager.getCapturedUpdateVersion(capturedIdKey);
                deferredWaitStartCapturedRecaptureVersion = CapturedIdRuleManager
                        .getCapturedRecaptureVersion(capturedIdKey);
            } else if ("wait_until_packet_text".equals(type)) {
                deferredWaitStartPacketTextVersion = PacketCaptureHandler.getRecentPacketTextVersion();
            }
            return;
        }

        waitConditionStartCapturedUpdateVersion = 0L;
        waitConditionStartCapturedRecaptureVersion = 0L;
        waitConditionStartPacketTextVersion = 0L;
        if ("wait_until_captured_id".equals(type) && !capturedIdKey.isEmpty()) {
            waitConditionStartCapturedUpdateVersion = CapturedIdRuleManager.getCapturedUpdateVersion(capturedIdKey);
            waitConditionStartCapturedRecaptureVersion = CapturedIdRuleManager.getCapturedRecaptureVersion(capturedIdKey);
        } else if ("wait_until_packet_text".equals(type)) {
            waitConditionStartPacketTextVersion = PacketCaptureHandler.getRecentPacketTextVersion();
        }
    }

    private boolean evaluateCapturedIdWait(ActionData actionData, long startUpdateVersion, long startRecaptureVersion) {
        if (actionData == null || actionData.params == null || !actionData.params.has("capturedId")) {
            return false;
        }
        String capturedIdKey = actionData.params.get("capturedId").getAsString().trim();
        if (capturedIdKey.isEmpty()) {
            return false;
        }
        String waitMode = actionData.params.has("waitMode")
                ? actionData.params.get("waitMode").getAsString()
                : "update";
        if ("recapture".equalsIgnoreCase(waitMode)) {
            return CapturedIdRuleManager.getCapturedRecaptureVersion(capturedIdKey) > startRecaptureVersion;
        }
        return CapturedIdRuleManager.getCapturedUpdateVersion(capturedIdKey) > startUpdateVersion;
    }

    private boolean evaluatePacketTextWait(ActionData actionData, long startPacketTextVersion) {
        if (actionData == null || actionData.params == null || !actionData.params.has("packetText")) {
            return false;
        }
        String packetText = actionData.params.get("packetText").getAsString();
        if (packetText == null || packetText.trim().isEmpty()) {
            return false;
        }
        if (PacketCaptureHandler.getRecentPacketTextVersion() <= startPacketTextVersion) {
            return false;
        }
        String expected = packetText.trim().toLowerCase(java.util.Locale.ROOT);
        for (String text : PacketCaptureHandler.getRecentPacketTextsSnapshot()) {
            if (text != null && text.toLowerCase(java.util.Locale.ROOT).contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean evaluateCombinedWait(ActionData actionData, EntityPlayerSP player) {
        if (actionData == null || actionData.params == null) {
            return false;
        }
        String rawConditions = actionData.params.has("conditionsText")
                ? actionData.params.get("conditionsText").getAsString()
                : "";
        List<String> expressions = splitCombinedExpressions(rawConditions);
        if (expressions.isEmpty()) {
            return false;
        }

        boolean allMode = actionData.params.has("combinedMode")
                && "ALL".equalsIgnoreCase(actionData.params.get("combinedMode").getAsString());
        boolean matchedAny = false;
        for (String expression : expressions) {
            boolean result = false;
            try {
                result = LegacyActionRuntime.evaluateExpression(
                        expression,
                        actionData.params,
                        runtimeVariables,
                        player,
                        currentSequence,
                        currentStepIndex,
                        actionIndex);
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("[legacy_path] 组合等待表达式解析失败: {}", expression, e);
            }
            if (allMode && !result) {
                return false;
            }
            if (!allMode && result) {
                return true;
            }
            matchedAny |= result;
        }
        return allMode || matchedAny;
    }

    private boolean evaluateWaitCancelExpression(ActionData actionData, EntityPlayerSP player) {
        if (actionData == null || actionData.params == null || !actionData.params.has("cancelExpression")) {
            return false;
        }
        String expression = actionData.params.get("cancelExpression").getAsString();
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        try {
            return LegacyActionRuntime.evaluateExpression(
                    expression,
                    actionData.params,
                    runtimeVariables,
                    player,
                    currentSequence,
                    currentStepIndex,
                    actionIndex);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("[legacy_path] 等待取消表达式解析失败: {}", expression, e);
            return false;
        }
    }

    private List<String> splitCombinedExpressions(String rawText) {
        List<String> expressions = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            return expressions;
        }
        for (String part : rawText.split("\\r?\\n|;|；")) {
            if (part != null && !part.trim().isEmpty()) {
                expressions.add(part.trim());
            }
        }
        return expressions;
    }

    private List<String> readBooleanExpressionList(JsonObject params, String arrayKey, String legacyKey) {
        List<String> expressions = new ArrayList<>();
        if (params == null) {
            return expressions;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            JsonArray array = params.getAsJsonArray(arrayKey);
            for (JsonElement element : array) {
                if (element != null && element.isJsonPrimitive()) {
                    String expression = element.getAsString();
                    if (expression != null && !expression.trim().isEmpty()) {
                        expressions.add(expression.trim());
                    }
                }
            }
            if (!expressions.isEmpty()) {
                return expressions;
            }
        }
        if (params.has(legacyKey) && params.get(legacyKey).isJsonPrimitive()) {
            String expression = params.get(legacyKey).getAsString();
            if (expression != null && !expression.trim().isEmpty()) {
                expressions.add(expression.trim());
            }
        }
        return expressions;
    }

    private boolean evaluateWaitAction(EntityPlayerSP player, ActionData actionData,
            long startUpdateVersion, long startRecaptureVersion, long startPacketTextVersion) {
        if (actionData == null || actionData.type == null) {
            return false;
        }
        String type = actionData.type.toLowerCase(java.util.Locale.ROOT);
        if ("wait_until_captured_id".equals(type)) {
            return evaluateCapturedIdWait(actionData, startUpdateVersion, startRecaptureVersion);
        }
        if ("wait_until_packet_text".equals(type)) {
            return evaluatePacketTextWait(actionData, startPacketTextVersion);
        }
        if ("wait_until_screen_region".equals(type)) {
            return evaluateScreenRegionWait(actionData);
        }
        if ("wait_combined".equals(type)) {
            return evaluateCombinedWait(actionData, player);
        }
        return evaluateConditionAction(player, actionData);
    }

    private int getWaitTimeoutTicks(ActionData actionData) {
        return actionData.params.has("timeoutTicks")
                ? Math.max(0, actionData.params.get("timeoutTicks").getAsInt())
                : 200;
    }

    private int getWaitTimeoutSkipCount(ActionData actionData) {
        return actionData.params.has("timeoutSkipCount")
                ? Math.max(0, actionData.params.get("timeoutSkipCount").getAsInt())
                : 0;
    }

    private boolean handleDeferredWaitAction(EntityPlayerSP player) {
        if (deferredWaitActionData == null) {
            return false;
        }

        int timeoutTicks = getWaitTimeoutTicks(deferredWaitActionData);
        boolean matched = evaluateWaitAction(player, deferredWaitActionData,
                deferredWaitStartCapturedUpdateVersion,
                deferredWaitStartCapturedRecaptureVersion,
                deferredWaitStartPacketTextVersion);
        boolean cancelled = evaluateWaitCancelExpression(deferredWaitActionData, player);
        boolean timedOut = timeoutTicks > 0 && deferredWaitElapsedTicks >= timeoutTicks;

        if (matched || timedOut || cancelled) {
            if (timedOut || cancelled) {
                actionIndex += getWaitTimeoutSkipCount(deferredWaitActionData);
            }
            clearDeferredWaitState();
            releaseResources();
            consumeDebugProgress("延后等待结束");
            return false;
        }

        deferredWaitElapsedTicks++;
        if (actionIndex >= deferredWaitResumeActionIndex) {
            tickDelay = 1;
            consumeDebugProgress("延后等待中 tick=" + deferredWaitElapsedTicks);
            return true;
        }
        return false;
    }

    private boolean handleConditionalOrWaitAction(EntityPlayerSP player, ActionData actionData) {
        String type = actionData.type == null ? "" : actionData.type.toLowerCase(java.util.Locale.ROOT);
        boolean matched = evaluateConditionAction(player, actionData);

        if (type.startsWith("condition_")) {
            int skipCount = actionData.params.has("skipCount") ? Math.max(0, actionData.params.get("skipCount").getAsInt()) : 1;
            currentDebugActionDescription = actionData.getDescription();
            recordDebugTrace("condition: " + currentDebugActionDescription + " -> " + (matched ? "true" : "false"));
            if (!matched) {
                actionIndex += skipCount + 1;
            } else {
                actionIndex++;
            }
            releaseResources();
            tickDelay = 2;
            resetWaitConditionState();
            consumeDebugProgress("条件判定完成");
            return true;
        }

        if (type.startsWith("wait_until_")) {
            int preExecuteCount = actionData.params.has("preExecuteCount")
                    ? Math.max(0, actionData.params.get("preExecuteCount").getAsInt())
                    : 0;
            currentDebugActionDescription = actionData.getDescription();

            if (preExecuteCount > 0 && deferredWaitActionData == null) {
                deferredWaitActionData = actionData;
                deferredWaitResumeActionIndex = actionIndex + 1 + preExecuteCount;
                deferredWaitElapsedTicks = 0;
                captureWaitStartVersions(actionData, true);
                recordDebugTrace("wait defer: " + currentDebugActionDescription + " / pre=" + preExecuteCount);
                actionIndex++;
                releaseResources();
                tickDelay = 1;
                resetWaitConditionState();
                consumeDebugProgress("进入延后等待");
                return true;
            }

            if (!waitConditionRunning) {
                waitConditionRunning = true;
                waitConditionElapsedTicks = 0;
                captureWaitStartVersions(actionData, false);
                recordDebugTrace("wait start: " + currentDebugActionDescription);
            }
            int timeoutTicks = getWaitTimeoutTicks(actionData);
            matched = evaluateWaitAction(player, actionData,
                    waitConditionStartCapturedUpdateVersion,
                    waitConditionStartCapturedRecaptureVersion,
                    waitConditionStartPacketTextVersion);
            boolean cancelled = evaluateWaitCancelExpression(actionData, player);
            boolean timedOut = timeoutTicks > 0 && waitConditionElapsedTicks >= timeoutTicks;

            if (matched || timedOut || cancelled) {
                recordDebugTrace("wait finish: " + currentDebugActionDescription + " -> "
                        + (matched ? "matched" : (cancelled ? "cancel" : "timeout")));
                if (timedOut || cancelled) {
                    actionIndex += getWaitTimeoutSkipCount(actionData) + 1;
                } else {
                    actionIndex++;
                }
                releaseResources();
                tickDelay = 2;
                resetWaitConditionState();
                clearDeferredWaitState();
                consumeDebugProgress("等待动作结束");
                return true;
            }

            waitConditionElapsedTicks++;
            tickDelay = 1;
            consumeDebugProgress("等待中 tick=" + waitConditionElapsedTicks);
            return true;
        }

        return false;
    }

    private ActionData resolveRuntimeActionData(ActionData actionData, EntityPlayerSP player) {
        if (actionData == null) {
            return null;
        }
        runtimeVariables.beginAction(currentStepIndex, actionIndex);
        JsonObject resolvedParams = LegacyActionRuntime.resolveParams(
                actionData.params,
                runtimeVariables,
                player,
                currentSequence,
                currentStepIndex,
                actionIndex);
        return new ActionData(actionData.type, resolvedParams);
    }

    private boolean handleRuntimeControlAction(EntityPlayerSP player,
            List<ActionData> actions,
            ActionData rawActionData,
            ActionData resolvedActionData) {
        if (resolvedActionData == null || resolvedActionData.type == null) {
            return false;
        }

        String type = resolvedActionData.type.toLowerCase(Locale.ROOT);

        if ("set_var".equals(type)) {
            String varName = resolvedActionData.params.has("name") ? resolvedActionData.params.get("name").getAsString().trim()
                    : "";
            if (!varName.isEmpty()) {
                JsonObject rawParams = rawActionData == null || rawActionData.params == null
                        ? new JsonObject()
                        : rawActionData.params;
                ensureSetVarDefaultValue(varName, rawParams, player);
                try {
                    Object value = LegacyActionRuntime.resolveAssignedValue(
                            rawParams,
                            runtimeVariables,
                            player,
                            currentSequence,
                            currentStepIndex,
                            actionIndex);
                    runtimeVariables.put(varName, value);
                    recordDebugTrace("set_var: " + varName + " = " + LegacyActionRuntime.stringifyValue(value));
                } catch (Exception e) {
                    Object fallbackValue = LegacyActionRuntime.inferAssignedValueDefault(rawParams);
                    runtimeVariables.put(varName, fallbackValue);
                    String detail = summarizeSetVarFailure(e);
                    sendPathRetryMessage("设置变量失败: " + varName + "，已使用默认值 "
                            + LegacyActionRuntime.stringifyValue(fallbackValue) + "；" + detail,
                            net.minecraft.util.text.TextFormatting.RED);
                    recordDebugTrace("set_var 失败: " + varName + " -> " + detail
                            + " ; fallback=" + LegacyActionRuntime.stringifyValue(fallbackValue));
                    zszlScriptMod.LOGGER.warn("[legacy_path] set_var failed: seq={}, step={}, action={}, var={}",
                            currentSequence == null ? "" : currentSequence.getName(),
                            currentStepIndex, actionIndex, varName, e);
                }
            }
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("控制流推进: set_var");
            return true;
        }

        if ("goto_action".equals(type)) {
            int targetActionIndex = resolvedActionData.params.has("targetActionIndex")
                    ? Math.max(0, resolvedActionData.params.get("targetActionIndex").getAsInt())
                    : actionIndex + 1;
            actionIndex = Math.min(Math.max(0, targetActionIndex), Math.max(0, actions.size()));
            tickDelay = 2;
            resetWaitConditionState();
            consumeDebugProgress("控制流跳转: goto_action -> " + actionIndex);
            return true;
        }

        if ("skip_actions".equals(type)) {
            int skipCount = resolvedActionData.params.has("count")
                    ? Math.max(0, resolvedActionData.params.get("count").getAsInt())
                    : 1;
            actionIndex = Math.min(Math.max(0, actionIndex + skipCount + 1), Math.max(0, actions.size()));
            tickDelay = 2;
            resetWaitConditionState();
            consumeDebugProgress("控制流跳过动作: skip_actions -> " + actionIndex);
            return true;
        }

        if ("skip_steps".equals(type)) {
            int skipCount = resolvedActionData.params.has("count")
                    ? Math.max(0, resolvedActionData.params.get("count").getAsInt())
                    : 0;
            List<PathStep> sequenceSteps = currentSequence == null
                    ? java.util.Collections.emptyList()
                    : currentSequence.getSteps();
            int targetStepIndex = Math.min(Math.max(0, currentStepIndex + skipCount + 1), sequenceSteps.size());
            currentStepIndex = targetStepIndex;
            actionIndex = 0;
            atTarget = false;
            currentStepRetryUsed = 0;
            resetStepPathRetryMonitor();
            releaseResources();
            resetAsyncActionState();
            resetRepeatActionState();
            resetWaitConditionState();
            clearDeferredWaitState();
            runtimeVariables.enterStep(currentStepIndex);
            if (currentStepIndex < sequenceSteps.size()) {
                restartCurrentStepTarget(sequenceSteps.get(currentStepIndex));
            } else {
                if (shouldStopNavigationForStepTransition()) {
                    EmbeddedNavigationHandler.INSTANCE.stop();
                }
                resetStepPathRetryMonitor();
            }
            tickDelay = 2;
            consumeDebugProgress("控制流跳过步骤: skip_steps -> " + currentStepIndex);
            return true;
        }

        if ("repeat_actions".equals(type)) {
            int bodyCount = resolvedActionData.params.has("bodyCount")
                    ? Math.max(0, resolvedActionData.params.get("bodyCount").getAsInt())
                    : 0;
            int loopCount = resolvedActionData.params.has("count")
                    ? Math.max(0, resolvedActionData.params.get("count").getAsInt())
                    : 0;
            String loopVarName = resolvedActionData.params.has("loopVar")
                    ? resolvedActionData.params.get("loopVar").getAsString().trim()
                    : "loop_index";

            if (bodyCount <= 0 || loopCount <= 0 || actionIndex + 1 >= actions.size()) {
                actionIndex++;
                tickDelay = 2;
                return true;
            }

            repeatActionRunning = true;
            repeatActionStepIndex = currentStepIndex;
            repeatActionHeaderIndex = actionIndex;
            repeatActionBodyStartIndex = actionIndex + 1;
            repeatActionBodyEndIndex = Math.min(actions.size() - 1, repeatActionBodyStartIndex + bodyCount - 1);
            repeatActionRemainingLoops = loopCount;
            repeatActionIteration = 0;
            repeatActionLoopVarName = loopVarName.isEmpty() ? "loop_index" : loopVarName;
            runtimeVariables.putLocal(repeatActionLoopVarName, repeatActionIteration);
            runtimeVariables.putLocal(repeatActionLoopVarName + "_remaining", repeatActionRemainingLoops);
            actionIndex = repeatActionBodyStartIndex;
            tickDelay = 1;
            consumeDebugProgress("进入循环体: repeat_actions");
            return true;
        }

        if ("capture_nearby_entity".equals(type)) {
            captureNearbyEntity(player, resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_nearby_entity");
            return true;
        }

        if ("capture_gui_title".equals(type)) {
            String varName = resolvedActionData.params.has("varName")
                    ? resolvedActionData.params.get("varName").getAsString().trim()
                    : "gui_title";
            if (!varName.isEmpty()) {
                runtimeVariables.put(varName, getCurrentGuiTitle());
                recordDebugTrace("capture_gui_title -> " + varName);
            }
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_gui_title");
            return true;
        }

        if ("capture_inventory_slot".equals(type)) {
            captureInventorySlot(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_inventory_slot");
            return true;
        }

        if ("capture_hotbar".equals(type)) {
            captureHotbar(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_hotbar");
            return true;
        }

        if ("capture_entity_list".equals(type)) {
            captureEntityList(player, resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_entity_list");
            return true;
        }

        if ("capture_packet_field".equals(type)) {
            capturePacketField(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_packet_field");
            return true;
        }

        if ("capture_scoreboard".equals(type)) {
            captureScoreboard(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_scoreboard");
            return true;
        }

        if ("capture_screen_region".equals(type)) {
            captureScreenRegion(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_screen_region");
            return true;
        }

        if ("capture_gui_element".equals(type)) {
            captureGuiElement(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_gui_element");
            return true;
        }

        if ("capture_block_at".equals(type)) {
            captureBlockAt(resolvedActionData.params);
            actionIndex++;
            tickDelay = 2;
            consumeDebugProgress("采集完成: capture_block_at");
            return true;
        }

        return false;
    }

    private boolean handleRepeatActionBoundary(List<ActionData> actions) {
        if (!repeatActionRunning || repeatActionStepIndex != currentStepIndex) {
            return false;
        }

        if (actionIndex <= repeatActionBodyEndIndex) {
            return false;
        }

        if (repeatActionRemainingLoops > 1) {
            repeatActionRemainingLoops--;
            repeatActionIteration++;
            runtimeVariables.putLocal(repeatActionLoopVarName, repeatActionIteration);
            runtimeVariables.putLocal(repeatActionLoopVarName + "_remaining", repeatActionRemainingLoops);
            actionIndex = repeatActionBodyStartIndex;
            tickDelay = 1;
            consumeDebugProgress("循环下一轮: " + repeatActionIteration);
            return true;
        }

        runtimeVariables.remove(repeatActionLoopVarName + "_remaining");
        resetRepeatActionState();
        return false;
    }

    private void captureNearbyEntity(EntityPlayerSP player, JsonObject params) {
        String varName = params.has("varName") ? params.get("varName").getAsString().trim() : "entity";
        String entityName = params.has("entityName") ? params.get("entityName").getAsString().trim() : "";
        double radius = params.has("radius") ? params.get("radius").getAsDouble() : 6.0D;

        if (varName.isEmpty()) {
            return;
        }

        runtimeVariables.put(varName + "_found", false);
        if (player == null || mc.world == null) {
            return;
        }

        String expected = entityName.toLowerCase(Locale.ROOT);
        EntityLivingBase best = null;
        double bestDistSq = Double.MAX_VALUE;
        List<EntityLivingBase> entities = mc.world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(player.getPosition()).grow(radius));
        for (EntityLivingBase entity : entities) {
            if (entity == null || entity == player || !entity.isEntityAlive()) {
                continue;
            }
            if (!expected.isEmpty()) {
                String actual = entity.getName() == null ? "" : entity.getName().toLowerCase(Locale.ROOT);
                if (!actual.contains(expected)) {
                    continue;
                }
            }
            double distSq = player.getDistanceSq(entity);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = entity;
            }
        }

        if (best == null) {
            recordDebugTrace("capture_nearby_entity -> " + varName + " 未找到");
            return;
        }

        runtimeVariables.put(varName + "_found", true);
        runtimeVariables.put(varName + "_name", best.getName());
        runtimeVariables.put(varName + "_id", best.getEntityId());
        runtimeVariables.put(varName + "_x", best.posX);
        runtimeVariables.put(varName + "_y", best.posY);
        runtimeVariables.put(varName + "_z", best.posZ);
        runtimeVariables.put(varName + "_block_x", best.getPosition().getX());
        runtimeVariables.put(varName + "_block_y", best.getPosition().getY());
        runtimeVariables.put(varName + "_block_z", best.getPosition().getZ());
        recordDebugTrace("capture_nearby_entity -> " + varName + " = " + best.getName());
    }

    private void captureBlockAt(JsonObject params) {
        String varName = params.has("varName") ? params.get("varName").getAsString().trim() : "block";
        if (varName.isEmpty()) {
            return;
        }

        runtimeVariables.put(varName + "_found", false);
        if (mc.world == null || params == null || !params.has("pos") || !params.get("pos").isJsonArray()) {
            return;
        }

        try {
            com.google.gson.JsonArray pos = params.getAsJsonArray("pos");
            BlockPos blockPos = new BlockPos(pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble());
            IBlockState state = mc.world.getBlockState(blockPos);
            Block block = state == null ? null : state.getBlock();
            if (block == null) {
                return;
            }

            runtimeVariables.put(varName + "_found", true);
            runtimeVariables.put(varName + "_name", block.getLocalizedName());
            runtimeVariables.put(varName + "_registry", String.valueOf(block.getRegistryName()));
            runtimeVariables.put(varName + "_x", blockPos.getX());
            runtimeVariables.put(varName + "_y", blockPos.getY());
            runtimeVariables.put(varName + "_z", blockPos.getZ());
            recordDebugTrace("capture_block_at -> " + varName + " = " + block.getLocalizedName());
        } catch (Exception ignored) {
        }
    }

    private void captureInventorySlot(JsonObject params) {
        String varName = params != null && params.has("varName") ? params.get("varName").getAsString().trim() : "slot";
        String area = params != null && params.has("slotArea") ? params.get("slotArea").getAsString() : "MAIN";
        int slotIndex = params != null && params.has("slotIndex") ? Math.max(0, params.get("slotIndex").getAsInt()) : 0;
        if (varName.isEmpty() || mc.player == null || mc.player.inventory == null) {
            return;
        }

        ItemStack stack = resolveInventoryStack(area, slotIndex);
        writeCapturedStack(varName, stack);
        runtimeVariables.put(varName + "_slot_index", slotIndex);
        runtimeVariables.put(varName + "_slot_area", area == null ? "" : area.toUpperCase(Locale.ROOT));
        recordDebugTrace("capture_inventory_slot -> " + varName + " = "
                + (stack == null || stack.isEmpty() ? "(empty)" : stack.getDisplayName()));
    }

    private void captureHotbar(JsonObject params) {
        String varName = params != null && params.has("varName") ? params.get("varName").getAsString().trim() : "hotbar";
        if (varName.isEmpty() || mc.player == null || mc.player.inventory == null) {
            return;
        }

        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<String> registries = new ArrayList<>();
        List<String> nbts = new ArrayList<>();
        int filled = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            writeCapturedStack(varName + "_" + i, stack);
            if (stack != null && !stack.isEmpty()) {
                filled++;
                names.add(stack.getDisplayName());
                counts.add(stack.getCount());
                registries.add(String.valueOf(stack.getItem().getRegistryName()));
                nbts.add(stack.hasTagCompound() ? stack.getTagCompound().toString() : "");
            } else {
                names.add("");
                counts.add(0);
                registries.add("");
                nbts.add("");
            }
        }
        int selectedIndex = Math.max(0, Math.min(8, mc.player.inventory.currentItem));
        runtimeVariables.put(varName + "_selected_index", selectedIndex);
        runtimeVariables.put(varName + "_selected_slot", selectedIndex + 1);
        writeCapturedStack(varName + "_selected", mc.player.inventory.getStackInSlot(selectedIndex));
        runtimeVariables.put(varName + "_filled_count", filled);
        runtimeVariables.put(varName + "_names", names);
        runtimeVariables.put(varName + "_counts", counts);
        runtimeVariables.put(varName + "_registries", registries);
        runtimeVariables.put(varName + "_nbts", nbts);
        recordDebugTrace("capture_hotbar -> " + varName + " filled=" + filled);
    }

    private void captureEntityList(EntityPlayerSP player, JsonObject params) {
        String varName = params != null && params.has("varName")
                ? params.get("varName").getAsString().trim()
                : "entities";
        String entityName = params != null && params.has("entityName")
                ? params.get("entityName").getAsString().trim()
                : "";
        double radius = params != null && params.has("radius") ? params.get("radius").getAsDouble() : 8.0D;
        int maxCount = params != null && params.has("maxCount") ? Math.max(1, params.get("maxCount").getAsInt()) : 16;
        if (varName.isEmpty()) {
            return;
        }

        runtimeVariables.put(varName + "_found", false);
        runtimeVariables.put(varName + "_count", 0);
        runtimeVariables.put(varName + "_list", new ArrayList<>());
        runtimeVariables.put(varName + "_names", new ArrayList<>());
        runtimeVariables.put(varName + "_distances", new ArrayList<>());
        runtimeVariables.put(varName + "_categories", new ArrayList<>());
        if (player == null || mc.world == null) {
            return;
        }

        String expected = entityName.toLowerCase(Locale.ROOT);
        List<EntityLivingBase> entities = new ArrayList<>(mc.world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(player.getPosition()).grow(radius)));
        entities.removeIf(entity -> entity == null || entity == player || !entity.isEntityAlive());
        entities.sort(Comparator.comparingDouble(player::getDistanceSq));

        List<Map<String, Object>> list = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<Double> distances = new ArrayList<>();
        for (EntityLivingBase entity : entities) {
            String actualName = entity.getName() == null ? "" : entity.getName();
            if (!expected.isEmpty() && !actualName.toLowerCase(Locale.ROOT).contains(expected)) {
                continue;
            }
            double distance = Math.sqrt(player.getDistanceSq(entity));
            String category = describeEntityCategory(entity);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", actualName);
            entry.put("id", entity.getEntityId());
            entry.put("type", entity.getClass().getSimpleName());
            entry.put("category", category);
            entry.put("distance", distance);
            entry.put("x", entity.posX);
            entry.put("y", entity.posY);
            entry.put("z", entity.posZ);
            list.add(entry);

            names.add(actualName);
            ids.add(entity.getEntityId());
            types.add(entity.getClass().getSimpleName());
            categories.add(category);
            distances.add(distance);
            if (list.size() >= maxCount) {
                break;
            }
        }

        runtimeVariables.put(varName + "_found", !list.isEmpty());
        runtimeVariables.put(varName + "_count", list.size());
        runtimeVariables.put(varName + "_list", list);
        runtimeVariables.put(varName + "_names", names);
        runtimeVariables.put(varName + "_ids", ids);
        runtimeVariables.put(varName + "_types", types);
        runtimeVariables.put(varName + "_categories", categories);
        runtimeVariables.put(varName + "_distances", distances);
        runtimeVariables.put(varName + "_radius", radius);
        runtimeVariables.put(varName + "_max_count", maxCount);
        if (!list.isEmpty()) {
            Map<String, Object> first = list.get(0);
            runtimeVariables.put(varName + "_nearest_name", first.get("name"));
            runtimeVariables.put(varName + "_nearest_id", first.get("id"));
            runtimeVariables.put(varName + "_nearest_type", first.get("type"));
            runtimeVariables.put(varName + "_nearest_category", first.get("category"));
            runtimeVariables.put(varName + "_nearest_distance", first.get("distance"));
        } else {
            runtimeVariables.put(varName + "_nearest_name", "");
            runtimeVariables.put(varName + "_nearest_id", 0);
            runtimeVariables.put(varName + "_nearest_type", "");
            runtimeVariables.put(varName + "_nearest_category", "");
            runtimeVariables.put(varName + "_nearest_distance", 0D);
        }
        recordDebugTrace("capture_entity_list -> " + varName + " count=" + list.size());
    }

    private void capturePacketField(JsonObject params) {
        String varName = params != null && params.has("varName")
                ? params.get("varName").getAsString().trim()
                : "packet_field";
        String lookupMode = params != null && params.has("lookupMode")
                ? params.get("lookupMode").getAsString()
                : "LATEST_CAPTURE";
        String fieldKey = params != null && params.has("fieldKey")
                ? params.get("fieldKey").getAsString().trim()
                : "";
        String fallbackValue = params != null && params.has("fallbackValue")
                ? params.get("fallbackValue").getAsString()
                : "";
        if (varName.isEmpty()) {
            return;
        }

        PacketFieldRuleManager.CapturedFieldSnapshot snapshot = null;
        Object value = null;
        if ("VARIABLE".equalsIgnoreCase(lookupMode)) {
            value = runtimeVariables.get(fieldKey);
            if (value == null && fieldKey != null && !fieldKey.trim().isEmpty()) {
                value = ScopedRuntimeVariables.getGlobalValue(fieldKey.trim());
            }
        } else {
            snapshot = PacketFieldRuleManager.getLatestCapturedField(fieldKey);
            if (snapshot != null) {
                value = snapshot.getValue();
            }
        }

        boolean found = value != null || snapshot != null;
        if (!found && fallbackValue != null && !fallbackValue.trim().isEmpty()) {
            value = fallbackValue;
        }

        runtimeVariables.put(varName + "_found", found);
        runtimeVariables.put(varName + "_lookup_mode", lookupMode == null ? "" : lookupMode.toUpperCase(Locale.ROOT));
        runtimeVariables.put(varName + "_field_key", fieldKey);
        runtimeVariables.put(varName + "_value", value);
        runtimeVariables.put(varName, value);
        runtimeVariables.put(varName + "_value_text", value == null ? "" : LegacyActionRuntime.stringifyValue(value));
        if (snapshot == null) {
            runtimeVariables.put(varName + "_rule_name", "");
            runtimeVariables.put(varName + "_variable_name", "");
            runtimeVariables.put(varName + "_scope", "");
            runtimeVariables.put(varName + "_channel", "");
            runtimeVariables.put(varName + "_direction", "");
            runtimeVariables.put(varName + "_source", "");
            runtimeVariables.put(varName + "_packet_class", "");
            runtimeVariables.put(varName + "_raw", "");
            runtimeVariables.put(varName + "_timestamp", 0L);
        } else {
            runtimeVariables.put(varName + "_rule_name", snapshot.getRuleName());
            runtimeVariables.put(varName + "_variable_name", snapshot.getVariableName());
            runtimeVariables.put(varName + "_scope", snapshot.getScope());
            runtimeVariables.put(varName + "_channel", snapshot.getChannel());
            runtimeVariables.put(varName + "_direction", snapshot.getDirection());
            runtimeVariables.put(varName + "_source", snapshot.getSource());
            runtimeVariables.put(varName + "_packet_class", snapshot.getPacketClassName());
            runtimeVariables.put(varName + "_raw", snapshot.getRawValue());
            runtimeVariables.put(varName + "_timestamp", snapshot.getTimestamp());
            runtimeVariables.put(varName + "_snapshot", snapshot.toMap());
        }
        recordDebugTrace("capture_packet_field -> " + varName + " = "
                + (value == null ? "(null)" : LegacyActionRuntime.stringifyValue(value)));
    }

    private void captureScoreboard(JsonObject params) {
        String varName = params != null && params.has("varName")
                ? params.get("varName").getAsString().trim()
                : "scoreboard";
        int lineIndex = params != null && params.has("lineIndex") ? params.get("lineIndex").getAsInt() : -1;
        if (varName.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }

        runtimeVariables.put(varName + "_found", false);
        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) {
            return;
        }
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return;
        }

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<String> lines = new ArrayList<>();
        for (Score score : scores) {
            if (score == null || score.getPlayerName() == null || score.getPlayerName().startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(line);
            if (lines.size() >= 15) {
                break;
            }
        }

        runtimeVariables.put(varName + "_found", true);
        runtimeVariables.put(varName + "_title", objective.getDisplayName());
        runtimeVariables.put(varName + "_lines", lines);
        runtimeVariables.put(varName + "_line_count", lines.size());
        runtimeVariables.put(varName + "_joined", String.join("\n", lines));
        runtimeVariables.put(varName + "_selected_index", lineIndex);
        boolean selectedFound = lineIndex >= 0 && lineIndex < lines.size();
        runtimeVariables.put(varName + "_selected_found", selectedFound);
        runtimeVariables.put(varName + "_selected_line", selectedFound ? lines.get(lineIndex) : "");
        recordDebugTrace("capture_scoreboard -> " + varName + " lines=" + lines.size());
    }

    private void captureScreenRegion(JsonObject params) {
        String varName = params != null && params.has("varName")
                ? params.get("varName").getAsString().trim()
                : "vision_region";
        int[] rect = parseVisionRegionRect(params);
        if (varName.isEmpty() || rect == null) {
            return;
        }

        ScreenVisionUtils.RegionMetrics metrics = ScreenVisionUtils.analyzeRegion(rect[0], rect[1], rect[2], rect[3]);
        runtimeVariables.put(varName + "_found", metrics.isFound());
        runtimeVariables.put(varName + "_x", rect[0]);
        runtimeVariables.put(varName + "_y", rect[1]);
        runtimeVariables.put(varName + "_width", rect[2]);
        runtimeVariables.put(varName + "_height", rect[3]);
        runtimeVariables.put(varName + "_avg_r", metrics.getAverageR());
        runtimeVariables.put(varName + "_avg_g", metrics.getAverageG());
        runtimeVariables.put(varName + "_avg_b", metrics.getAverageB());
        runtimeVariables.put(varName + "_avg_hex", metrics.getAverageHex());
        runtimeVariables.put(varName + "_center_r", metrics.getCenterR());
        runtimeVariables.put(varName + "_center_g", metrics.getCenterG());
        runtimeVariables.put(varName + "_center_b", metrics.getCenterB());
        runtimeVariables.put(varName + "_center_hex", metrics.getCenterHex());
        runtimeVariables.put(varName + "_brightness", metrics.getBrightness());
        runtimeVariables.put(varName + "_edge_density", metrics.getEdgeDensity());
        recordDebugTrace("capture_screen_region -> " + varName + " avg=" + metrics.getAverageHex());
    }

    private boolean evaluateScreenRegionWait(ActionData actionData) {
        if (actionData == null || actionData.params == null) {
            return false;
        }
        int[] rect = parseVisionRegionRect(actionData.params);
        if (rect == null) {
            return false;
        }
        String compareMode = actionData.params.has("visionCompareMode")
                ? actionData.params.get("visionCompareMode").getAsString()
                : "AVERAGE_COLOR";
        if ("TEMPLATE".equalsIgnoreCase(compareMode)) {
            String imagePath = actionData.params.has("imagePath") ? actionData.params.get("imagePath").getAsString() : "";
            double threshold = actionData.params.has("similarityThreshold")
                    ? actionData.params.get("similarityThreshold").getAsDouble()
                    : 0.92D;
            ScreenVisionUtils.TemplateMatchResult result = ScreenVisionUtils.compareRegionToTemplate(rect[0], rect[1],
                    rect[2], rect[3], imagePath);
            return result.isFound() && result.getSimilarity() >= threshold;
        }

        ScreenVisionUtils.RegionMetrics metrics = ScreenVisionUtils.analyzeRegion(rect[0], rect[1], rect[2], rect[3]);
        if (!metrics.isFound()) {
            return false;
        }
        if ("EDGE_DENSITY".equalsIgnoreCase(compareMode)) {
            double edgeThreshold = actionData.params.has("edgeThreshold")
                    ? actionData.params.get("edgeThreshold").getAsDouble()
                    : 0.12D;
            return metrics.getEdgeDensity() >= edgeThreshold;
        }

        int targetColor = ScreenVisionUtils.parseColor(
                actionData.params.has("targetColor") ? actionData.params.get("targetColor").getAsString() : "",
                -1);
        if (targetColor < 0) {
            return false;
        }
        double tolerance = actionData.params.has("colorTolerance")
                ? actionData.params.get("colorTolerance").getAsDouble()
                : 48D;
        int averageColor = ((metrics.getAverageR() & 0xFF) << 16)
                | ((metrics.getAverageG() & 0xFF) << 8)
                | (metrics.getAverageB() & 0xFF);
        return ScreenVisionUtils.colorDistance(targetColor, averageColor) <= tolerance;
    }

    private int[] parseVisionRegionRect(JsonObject params) {
        if (params == null || !params.has("regionRect")) {
            return null;
        }
        try {
            if (params.get("regionRect").isJsonArray()) {
                com.google.gson.JsonArray array = params.getAsJsonArray("regionRect");
                if (array.size() < 4) {
                    return null;
                }
                return new int[] {
                        Math.max(0, (int) Math.round(array.get(0).getAsDouble())),
                        Math.max(0, (int) Math.round(array.get(1).getAsDouble())),
                        Math.max(1, (int) Math.round(array.get(2).getAsDouble())),
                        Math.max(1, (int) Math.round(array.get(3).getAsDouble()))
                };
            }
            String text = params.get("regionRect").getAsString();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            String normalized = text.replace("[", "").replace("]", "");
            String[] parts = normalized.split(",");
            if (parts.length < 4) {
                return null;
            }
            return new int[] {
                    Math.max(0, (int) Math.round(Double.parseDouble(parts[0].trim()))),
                    Math.max(0, (int) Math.round(Double.parseDouble(parts[1].trim()))),
                    Math.max(1, (int) Math.round(Double.parseDouble(parts[2].trim()))),
                    Math.max(1, (int) Math.round(Double.parseDouble(parts[3].trim())))
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private ItemStack resolveInventoryStack(String area, int slotIndex) {
        String normalized = area == null ? "MAIN" : area.trim().toUpperCase(Locale.ROOT);
        if ("HOTBAR".equals(normalized)) {
            return slotIndex >= 0 && slotIndex < 9 ? mc.player.inventory.getStackInSlot(slotIndex) : ItemStack.EMPTY;
        }
        if ("ARMOR".equals(normalized)) {
            return slotIndex >= 0 && slotIndex < mc.player.inventory.armorInventory.size()
                    ? mc.player.inventory.armorInventory.get(slotIndex)
                    : ItemStack.EMPTY;
        }
        if ("OFFHAND".equals(normalized)) {
            return slotIndex >= 0 && slotIndex < mc.player.inventory.offHandInventory.size()
                    ? mc.player.inventory.offHandInventory.get(slotIndex)
                    : ItemStack.EMPTY;
        }
        return slotIndex >= 0 && slotIndex < mc.player.inventory.mainInventory.size()
                ? mc.player.inventory.mainInventory.get(slotIndex)
                : ItemStack.EMPTY;
    }

    private void writeCapturedStack(String varName, ItemStack stack) {
        if (varName == null || varName.trim().isEmpty()) {
            return;
        }
        runtimeVariables.put(varName + "_found", stack != null && !stack.isEmpty());
        if (stack == null || stack.isEmpty()) {
            runtimeVariables.put(varName + "_name", "");
            runtimeVariables.put(varName + "_count", 0);
            runtimeVariables.put(varName + "_registry", "");
            runtimeVariables.put(varName + "_damage", 0);
            runtimeVariables.put(varName + "_has_nbt", false);
            runtimeVariables.put(varName + "_nbt", "");
            return;
        }
        runtimeVariables.put(varName + "_name", stack.getDisplayName());
        runtimeVariables.put(varName + "_count", stack.getCount());
        runtimeVariables.put(varName + "_registry", String.valueOf(stack.getItem().getRegistryName()));
        runtimeVariables.put(varName + "_damage", stack.getItemDamage());
        runtimeVariables.put(varName + "_has_nbt", stack.hasTagCompound());
        runtimeVariables.put(varName + "_nbt", stack.hasTagCompound() ? stack.getTagCompound().toString() : "");
    }

    private String describeEntityCategory(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            return "PLAYER";
        }
        if (entity instanceof EntityDragon) {
            return "BOSS";
        }
        if (entity instanceof IMob || entity.isCreatureType(EnumCreatureType.MONSTER, false)) {
            return "HOSTILE";
        }
        if (entity instanceof EntityAnimal
                || entity instanceof EntityVillager
                || entity instanceof EntityAmbientCreature
                || entity instanceof EntityWaterMob
                || entity instanceof EntityGolem
                || entity instanceof EntityCreature) {
            return "PASSIVE";
        }
        return "OTHER";
    }

    private void captureGuiElement(JsonObject params) {
        String varName = params != null && params.has("varName")
                ? params.get("varName").getAsString().trim()
                : "gui_element";
        if (varName.isEmpty()) {
            return;
        }

        String elementType = params != null && params.has("elementType")
                ? params.get("elementType").getAsString()
                : "ANY";
        String locatorMode = params != null && params.has("guiElementLocatorMode")
                ? params.get("guiElementLocatorMode").getAsString()
                : "TEXT";
        String locatorText = params != null && params.has("locatorText")
                ? params.get("locatorText").getAsString()
                : "";
        String matchMode = params != null && params.has("locatorMatchMode")
                ? params.get("locatorMatchMode").getAsString()
                : "CONTAINS";

        GuiElementInspector.GuiSnapshot snapshot = GuiElementInspector.captureCurrentSnapshot();
        runtimeVariables.put(varName + "_found", false);
        runtimeVariables.put(varName + "_screen", snapshot.getScreenSimpleName());
        runtimeVariables.put(varName + "_screen_class", snapshot.getScreenClassName());
        runtimeVariables.put(varName + "_title", snapshot.getTitle());

        GuiElementInspector.GuiElementInfo best = null;
        if ("PATH".equalsIgnoreCase(locatorMode)) {
            best = GuiElementInspector.findFirstByPath(locatorText, matchMode, resolveGuiElementTypes(elementType));
        } else {
            best = findGuiElementByText(snapshot, locatorText, matchMode, elementType);
        }

        if (best == null && "TITLE".equalsIgnoreCase(elementType)) {
            for (GuiElementInspector.GuiElementInfo element : snapshot.getElements()) {
                if (element != null && element.getType() == GuiElementInspector.ElementType.TITLE) {
                    best = element;
                    break;
                }
            }
        }

        if (best == null) {
            recordDebugTrace("capture_gui_element -> " + varName + " 未找到");
            return;
        }

        runtimeVariables.put(varName + "_found", true);
        runtimeVariables.put(varName + "_type", best.getType().name());
        runtimeVariables.put(varName + "_path", best.getPath());
        runtimeVariables.put(varName + "_text", best.getText());
        runtimeVariables.put(varName + "_x", best.getX());
        runtimeVariables.put(varName + "_y", best.getY());
        runtimeVariables.put(varName + "_width", best.getWidth());
        runtimeVariables.put(varName + "_height", best.getHeight());
        if (best.getSlotIndex() >= 0) {
            runtimeVariables.put(varName + "_slot", best.getSlotIndex());
        }
        if (best.getButtonId() != Integer.MIN_VALUE) {
            runtimeVariables.put(varName + "_button_id", best.getButtonId());
        }
        recordDebugTrace("capture_gui_element -> " + varName + " = " + best.getPath());
    }

    private GuiElementInspector.GuiElementInfo findGuiElementByText(GuiElementInspector.GuiSnapshot snapshot,
            String locatorText, String matchMode, String elementType) {
        if (snapshot == null || snapshot.getElements().isEmpty()) {
            return null;
        }

        String expected = locatorText == null ? "" : locatorText.trim().toLowerCase(Locale.ROOT);
        for (GuiElementInspector.GuiElementInfo element : snapshot.getElements()) {
            if (element == null || !matchesGuiElementType(element.getType(), elementType)) {
                continue;
            }
            if (expected.isEmpty()) {
                return element;
            }
            String actual = element.getText() == null ? "" : element.getText().trim().toLowerCase(Locale.ROOT);
            if ("EXACT".equalsIgnoreCase(matchMode) ? actual.equals(expected) : actual.contains(expected)) {
                return element;
            }
        }
        return null;
    }

    private GuiElementInspector.ElementType[] resolveGuiElementTypes(String elementType) {
        if ("TITLE".equalsIgnoreCase(elementType)) {
            return new GuiElementInspector.ElementType[] { GuiElementInspector.ElementType.TITLE };
        }
        if ("BUTTON".equalsIgnoreCase(elementType)) {
            return new GuiElementInspector.ElementType[] { GuiElementInspector.ElementType.BUTTON };
        }
        if ("SLOT".equalsIgnoreCase(elementType)) {
            return new GuiElementInspector.ElementType[] { GuiElementInspector.ElementType.SLOT };
        }
        return new GuiElementInspector.ElementType[] {
                GuiElementInspector.ElementType.TITLE,
                GuiElementInspector.ElementType.BUTTON,
                GuiElementInspector.ElementType.SLOT
        };
    }

    private boolean matchesGuiElementType(GuiElementInspector.ElementType type, String expectedType) {
        if (type == null) {
            return false;
        }
        if (expectedType == null || expectedType.trim().isEmpty() || "ANY".equalsIgnoreCase(expectedType)) {
            return true;
        }
        return type.name().equalsIgnoreCase(expectedType);
    }

    private boolean handleCurrentStepFailure(String reason, Throwable error) {
        if (currentSequence == null || currentStepIndex < 0 || currentStepIndex >= currentSequence.getSteps().size()) {
            markExecutionResult(false, reason);
            stopTracking();
            return true;
        }

        String safeReason = reason == null ? "unknown" : reason;
        if (error != null) {
            zszlScriptMod.LOGGER.error("[legacy_path] 步骤失败: seq={}, step={}, action={}, reason={}",
                    currentSequence.getName(), currentStepIndex, actionIndex, safeReason, error);
        } else {
            zszlScriptMod.LOGGER.warn("[legacy_path] 步骤失败: seq={}, step={}, action={}, reason={}",
                    currentSequence.getName(), currentStepIndex, actionIndex, safeReason);
        }
        recordDebugTrace("步骤失败: step=" + currentStepIndex + ", action=" + actionIndex + ", reason=" + safeReason);

        resetHotbarUseActionState();
        resetAsyncActionState();
        resetWaitConditionState();
        clearDeferredWaitState();
        resetRepeatActionState();
        resetStepPathRetryMonitor();
        releaseResources();
        markExecutionResult(false, "步骤动作异常停止: " + safeReason);
        stopTracking();
        return true;
    }

    private void restartCurrentStepTarget(PathStep step) {
        resetStepPathRetryMonitor();
        if (step == null) {
            atTarget = true;
            if (shouldStopNavigationForStepTransition()) {
                EmbeddedNavigationHandler.INSTANCE.stop();
            }
            return;
        }
        double[] target = step.getGotoPoint();
        if (target != null && target.length >= 3 && !Double.isNaN(target[0])) {
            atTarget = false;
            if (shouldStopNavigationForStepTransition()) {
                EmbeddedNavigationHandler.INSTANCE.stop();
            }
            EmbeddedNavigationHandler.INSTANCE.startGoto(target[0], target[1], target[2]);
            initializeStepPathRetryMonitor(mc.player);
        } else {
            atTarget = true;
            if (shouldStopNavigationForStepTransition()) {
                EmbeddedNavigationHandler.INSTANCE.stop();
            }
            resetStepPathRetryMonitor();
        }
    }

    private void moveToStepTargetOrFinish() {
        resetStepPathRetryMonitor();
        if (currentSequence == null) {
            return;
        }
        if (currentStepIndex >= currentSequence.getSteps().size()) {
            atTarget = false;
            return;
        }
        restartCurrentStepTarget(currentSequence.getSteps().get(currentStepIndex));
    }

    private boolean hasReachedGotoTarget(EntityPlayerSP player, double[] target) {
        if (player == null || target == null || target.length < 3) {
            return true;
        }

        if (Double.isNaN(target[0])) {
            return true;
        }

        int playerBlockX = MathHelper.floor(player.posX);
        int playerBlockY = MathHelper.floor(player.posY);
        int playerBlockZ = MathHelper.floor(player.posZ);

        int targetBlockX = MathHelper.floor(target[0]);
        int targetBlockZ = MathHelper.floor(target[2]);

        if (Double.isNaN(target[1])) {
            return playerBlockX == targetBlockX && playerBlockZ == targetBlockZ;
        }

        int targetBlockY = MathHelper.floor(target[1]);
        return playerBlockX == targetBlockX
                && playerBlockY == targetBlockY
                && playerBlockZ == targetBlockZ;
    }

    private void handleUseHotbarItemAction(EntityPlayerSP player, ActionData actionData) {
        if (!hotbarUseActionRunning) {
            this.hotbarUseItemName = actionData.params.has("itemName")
                    ? actionData.params.get("itemName").getAsString().trim()
                    : "";
            this.hotbarUseMatchMode = actionData.params.has("matchMode")
                    && "EXACT".equalsIgnoreCase(actionData.params.get("matchMode").getAsString())
                            ? AutoUseItemRule.MatchMode.EXACT
                            : AutoUseItemRule.MatchMode.CONTAINS;
            this.hotbarUseMode = actionData.params.has("useMode")
                    && "LEFT_CLICK".equalsIgnoreCase(actionData.params.get("useMode").getAsString())
                            ? AutoUseItemRule.UseMode.LEFT_CLICK
                            : AutoUseItemRule.UseMode.RIGHT_CLICK;
            this.hotbarUseChangeLocalSlot = actionData.params.has("changeLocalSlot")
                    && actionData.params.get("changeLocalSlot").getAsBoolean();
            this.hotbarUseSwitchItemDelayTicks = Math.max(0,
                    actionData.params.has("switchItemDelayTicks")
                            ? actionData.params.get("switchItemDelayTicks").getAsInt()
                            : 0);
            this.hotbarUseSwitchDelayTicks = Math.max(0,
                    actionData.params.has("switchDelayTicks")
                            ? actionData.params.get("switchDelayTicks").getAsInt()
                            : 0);
            this.hotbarUseSwitchBackDelayTicks = Math.max(0,
                    actionData.params.has("switchBackDelayTicks")
                            ? actionData.params.get("switchBackDelayTicks").getAsInt()
                            : 0);
            this.hotbarUseRemainingCount = Math.max(1,
                    actionData.params.has("count") ? actionData.params.get("count").getAsInt() : 1);
            this.hotbarUseIntervalTicks = Math.max(0,
                    actionData.params.has("intervalTicks") ? actionData.params.get("intervalTicks").getAsInt() : 0);
            this.hotbarUseWaitTicks = 0;
            this.hotbarUseActionRunning = true;
        }

        if (hotbarUseItemName.isEmpty()) {
            resetHotbarUseActionState();
            releaseResources();
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        if (hotbarUseIntervalTicks <= 0) {
            while (hotbarUseRemainingCount > 0) {
                boolean ok = AutoUseItemHandler.INSTANCE.useMatchingHotbarItem(player, hotbarUseItemName,
                        hotbarUseMatchMode, hotbarUseMode, hotbarUseChangeLocalSlot,
                        hotbarUseSwitchItemDelayTicks, hotbarUseSwitchDelayTicks, hotbarUseSwitchBackDelayTicks);
                hotbarUseRemainingCount--;
                if (!ok) {
                    break;
                }
            }
            resetHotbarUseActionState();
            releaseResources();
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        if (hotbarUseWaitTicks > 0) {
            hotbarUseWaitTicks--;
            return;
        }

        boolean ok = AutoUseItemHandler.INSTANCE.useMatchingHotbarItem(player, hotbarUseItemName,
                hotbarUseMatchMode, hotbarUseMode, hotbarUseChangeLocalSlot,
                hotbarUseSwitchItemDelayTicks, hotbarUseSwitchDelayTicks, hotbarUseSwitchBackDelayTicks);
        hotbarUseRemainingCount--;

        if (!ok || hotbarUseRemainingCount <= 0) {
            resetHotbarUseActionState();
            releaseResources();
            actionIndex++;
            applyBuiltinSequenceDelay();
            return;
        }

        hotbarUseWaitTicks = hotbarUseIntervalTicks;
    }

    // --- 新增：狩猎模式的核心逻辑 ---
    private void executeHuntTick(EntityPlayerSP player) {
        if (mc.world == null || player == null) {
            completeHuntAction();
            return;
        }

        if (huntAttackCooldownTicks > 0) {
            huntAttackCooldownTicks--;
        }
        if (huntChaseCooldownTicks > 0) {
            huntChaseCooldownTicks--;
        }

        // 1. 检查当前目标是否仍然有效
        if (huntTargetEntity != null && (!(huntTargetEntity instanceof EntityLivingBase)
                || !isLockedHuntTargetStillTrackable(player, (EntityLivingBase) huntTargetEntity))) {
            if (!huntPendingCompleteAfterSequence && huntAttackSequenceExecutor.isRunning()) {
                huntAttackSequenceExecutor.stop();
            }
            huntTargetEntity = null; // 目标死亡或失效，清空目标
            lastHuntGotoTargetEntityId = -1L;
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

        // 2. 如果没有目标，则搜索新目标
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
            Optional<EntityLivingBase> nextTarget = mc.world.loadedEntityList.stream()
                    .filter(entity -> entity instanceof EntityLivingBase)
                    .map(entity -> (EntityLivingBase) entity)
                    .filter(entity -> isValidHuntCandidate(player, entity))
                    .min((left, right) -> compareHuntTargets(player, left, right));

            if (nextTarget.isPresent()) {
                huntTargetEntity = nextTarget.get();
                lastHuntGotoTargetEntityId = -1L;
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
                zszlScriptMod.LOGGER.info(I18n.format("log.path.hunt_new_target") + getHuntFilterableEntityName(huntTargetEntity));
            } else {
                zszlScriptMod.LOGGER.info(I18n.format("log.path.hunt_no_monster_end"));
                completeHuntAction(huntNoTargetSkipCount);
                return;
            }
        }

        // 4. 如果有目标，则执行移动和攻击
        if (huntTargetEntity != null) {
            EntityLivingBase livingTarget = (EntityLivingBase) huntTargetEntity;
            double distanceSq = player.getDistanceSq(huntTargetEntity);
            double distance = Math.sqrt(distanceSq);

            // 只有开启自动攻击时才控制视角
            if (huntAutoAttack) {
                tickHuntAttackSequenceExecutor(player);
                if (huntPendingCompleteAfterSequence && !huntAttackSequenceExecutor.isRunning()) {
                    completeHuntAction();
                    return;
                }
                if (huntAimLockEnabled && !isHuntOrbitMode()) {
                    rotatePlayerTowardHuntTarget(player, livingTarget);
                }
                if (tryHuntAttack(player, livingTarget)) {
                    if (huntAttackRemaining > 0) {
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
            } else if (shouldRunMovement) {
                huntMovementStopped = false;
                if (isHuntOrbitMode()) {
                    if (shouldUseContinuousHuntOrbit(player, livingTarget, distance)) {
                        stopHuntEmbeddedNavigation();
                        stopHuntOrbitProcess();
                        driveContinuousHuntOrbit(player, livingTarget);
                        return;
                    } else {
                        huntOrbitController.stop();
                        stopHuntOrbitProcess();
                        navigateHuntTowardsTarget(player, livingTarget);
                    }
                } else {
                    huntOrbitController.stop();
                    stopHuntOrbitProcess();
                    navigateHuntTowardsTarget(player, livingTarget);
                }
            } else {
                stopHuntNavigationMode();
            }
        }
    }

    private void resetHuntState() {
        this.huntAttackSequenceExecutor.stop();
        this.isHunting = false;
        this.huntRadius = 0.0D;
        this.huntAutoAttack = false;
        this.huntAttackMode = KillAuraHandler.ATTACK_MODE_NORMAL;
        this.huntAttackSequenceName = "";
        this.huntAimLockEnabled = true;
        this.huntTrackingDistanceSq = 0.0D;
        this.huntTargetEntity = null;
        this.lastHuntGotoTargetEntityId = -1L;
        this.huntMovementStopped = false;
        this.huntMode = KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
        this.huntOrbitEnabled = false;
        this.huntChaseIntervalEnabled = false;
        this.huntChaseIntervalTicks = 0;
        this.huntChaseCooldownTicks = 0;
        this.huntWasWithinDesiredDistance = false;
        this.huntAttackCooldownTicks = 0;
        this.lastHuntGotoTargetX = Double.NaN;
        this.lastHuntGotoTargetY = Double.NaN;
        this.lastHuntGotoTargetZ = Double.NaN;
        this.huntAttackRemaining = -1;
        this.huntNoTargetSkipCount = 0;
        this.huntRestrictTargetGroups = true;
        this.huntTargetHostile = true;
        this.huntTargetPassive = false;
        this.huntTargetPlayers = false;
        this.huntEnableNameWhitelist = false;
        this.huntEnableNameBlacklist = false;
        this.huntNameWhitelist.clear();
        this.huntNameBlacklist.clear();
        this.huntShowRange = false;
        this.huntIgnoreInvisible = false;
        this.huntCenterX = 0.0D;
        this.huntCenterY = 0.0D;
        this.huntCenterZ = 0.0D;
        this.huntOrbitLoopNodeIndex = -1;
        this.huntLastOrbitGotoTick = -99999;
        this.huntOrbitStuckTicks = 0;
        this.huntLastOrbitPlayerX = Double.NaN;
        this.huntLastOrbitPlayerZ = Double.NaN;
        this.huntPendingCompleteAfterSequence = false;
        this.huntOrbitController.stop();
    }

    private void completeHuntAction() {
        completeHuntAction(0);
    }

    private void completeHuntAction(int additionalSkipCount) {
        stopHuntNavigationMode();
        if (shouldStopNavigationOnFinish()) {
            EmbeddedNavigationHandler.INSTANCE.stop();
        }
        resetHuntState();
        actionIndex += Math.max(0, additionalSkipCount) + 1;
        applyBuiltinSequenceDelay();
        setStatus(getStatus().split(" \\| ")[0]);
    }

    private boolean isHuntFixedDistanceMode() {
        return KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equalsIgnoreCase(huntMode);
    }

    private boolean isHuntOrbitMode() {
        return isHuntFixedDistanceMode() && huntOrbitEnabled;
    }

    private double getHuntDesiredDistance() {
        return Math.max(0.5D, Math.sqrt(Math.max(0.0D, huntTrackingDistanceSq)));
    }

    private String normalizeHuntAttackMode(String mode) {
        return KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(mode)
                ? KillAuraHandler.ATTACK_MODE_SEQUENCE
                : KillAuraHandler.ATTACK_MODE_NORMAL;
    }

    private boolean isHuntSequenceAttackMode() {
        return KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(huntAttackMode);
    }

    private String getConfiguredHuntAttackSequenceName() {
        return huntAttackSequenceName == null ? "" : huntAttackSequenceName.trim();
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

    private void rotatePlayerTowardHuntTarget(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return;
        }
        ModUtils.setPlayerViewAngles(player,
                (float) Math.toDegrees(Math.atan2(target.posZ - player.posZ, target.posX - player.posX)) - 90.0F,
                (float) -Math.toDegrees(Math.atan2(target.posY + target.getEyeHeight() * 0.85D
                        - (player.posY + player.getEyeHeight()), Math.max(0.001D, player.getDistance(target)))));
    }

    private void tickHuntAttackSequenceExecutor(EntityPlayerSP player) {
        if (!isHuntSequenceAttackMode()) {
            if (huntAttackSequenceExecutor.isRunning()) {
                huntAttackSequenceExecutor.stop();
            }
            return;
        }
        huntAttackSequenceExecutor.tick(player);
    }

    private boolean tryTriggerHuntAttackSequence(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return false;
        }
        if (huntAttackCooldownTicks > 0 || huntAttackSequenceExecutor.isRunning()) {
            return false;
        }
        String sequenceName = getConfiguredHuntAttackSequenceName();
        if (sequenceName.isEmpty()) {
            return false;
        }
        PathSequence configuredSequence = PathSequenceManager.getSequence(sequenceName);
        if (configuredSequence == null || configuredSequence.getSteps().isEmpty()) {
            return false;
        }
        huntAttackSequenceExecutor.start(configuredSequence, player, target);
        if (!huntAttackSequenceExecutor.isRunning()) {
            return false;
        }
        huntAttackCooldownTicks = Math.max(0, KillAuraHandler.attackSequenceDelayTicks);
        return true;
    }

    private boolean tryHuntAttack(EntityPlayerSP player, EntityLivingBase target) {
        if (isHuntSequenceAttackMode()) {
            return tryTriggerHuntAttackSequence(player, target);
        }
        if (player == null || target == null || mc.playerController == null) {
            return false;
        }
        if (huntAttackCooldownTicks > 0) {
            return false;
        }
        if (player.getDistance(target) > KillAuraHandler.attackRange) {
            return false;
        }
        if (KillAuraHandler.requireLineOfSight && !player.canEntityBeSeen(target)) {
            return false;
        }
        if (player.getCooledAttackStrength(0.0F) < KillAuraHandler.minAttackStrength) {
            return false;
        }
        mc.playerController.attackEntity(player, target);
        player.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        huntAttackCooldownTicks = Math.max(0, KillAuraHandler.minAttackIntervalTicks);
        return true;
    }

    private void driveContinuousHuntOrbit(EntityPlayerSP player, EntityLivingBase target) {
        huntOrbitController.tick(player, target,
                new HuntOrbitController.OrbitConfig(getHuntDesiredDistance(), HUNT_FIXED_DISTANCE_TOLERANCE,
                        true, true, true));
    }

    private boolean shouldUseContinuousHuntOrbit(EntityPlayerSP player, EntityLivingBase target, double distance) {
        if (!isHuntOrbitMode() || player == null || target == null) {
            return false;
        }
        if (!KillAuraHandler.isHuntOrbitSampleCountAtMaximum()) {
            return false;
        }
        if (player.isElytraFlying()
                || (player.capabilities != null && player.capabilities.isFlying)
                || Math.abs(player.posY - target.posY) > HUNT_CONTINUOUS_ORBIT_MAX_VERTICAL_DELTA) {
            return false;
        }

        double entryDistance = Math.max(getHuntDesiredDistance() + HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER,
                KillAuraHandler.attackRange + 0.9D);
        double maxDistance = huntOrbitController.isActive()
                ? entryDistance + HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER
                : entryDistance;
        return distance <= maxDistance;
    }

    private KillAuraOrbitProcess getHuntOrbitProcess() {
        try {
            Object primary = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (primary instanceof Baritone) {
                return ((Baritone) primary).getKillAuraOrbitProcess();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean requestHuntOrbitProcess(EntityLivingBase target) {
        if (mc.player == null || mc.player.isElytraFlying()
                || (mc.player.capabilities != null && mc.player.capabilities.isFlying)
                || !mc.player.onGround) {
            return false;
        }
        KillAuraOrbitProcess process = getHuntOrbitProcess();
        return process != null && process.requestOrbit(target, getHuntDesiredDistance());
    }

    private boolean driveHuntOrbitMovement(EntityPlayerSP player, EntityLivingBase target, boolean orbitPlanned) {
        if (player == null || target == null || !orbitPlanned) {
            return false;
        }
        List<BetterBlockPos> orbitNodes = getHuntOrbitLoopNodes();
        if (orbitNodes.size() < 2) {
            return false;
        }

        updateHuntOrbitStuckState(player);

        int nextIndex = chooseHuntOrbitLoopNodeIndex(player, orbitNodes);
        if (nextIndex < 0 || nextIndex >= orbitNodes.size()) {
            return false;
        }

        BetterBlockPos goal = orbitNodes.get(nextIndex);
        double goalX = goal.x + 0.5D;
        double goalY = goal.y;
        double goalZ = goal.z + 0.5D;
        double dx = player.posX - goalX;
        double dz = player.posZ - goalZ;
        boolean targetChanged = lastHuntGotoTargetEntityId != target.getEntityId();
        boolean nodeChanged = nextIndex != huntOrbitLoopNodeIndex;
        boolean gotoExpired = player.ticksExisted - huntLastOrbitGotoTick >= 8;
        boolean orbitStuck = huntOrbitStuckTicks >= 4;
        boolean farFromGoal = dx * dx + dz * dz > 3.0D;

        if (targetChanged || nodeChanged || gotoExpired || orbitStuck || farFromGoal) {
            EmbeddedNavigationHandler.INSTANCE.startGoto(goalX, goalY, goalZ, true);
            huntOrbitLoopNodeIndex = nextIndex;
            huntLastOrbitGotoTick = player.ticksExisted;
            lastHuntGotoTargetEntityId = target.getEntityId();
            lastHuntGotoTargetX = goalX;
            lastHuntGotoTargetY = goalY;
            lastHuntGotoTargetZ = goalZ;
            huntMovementStopped = false;
        }
        return true;
    }

    private List<BetterBlockPos> getHuntOrbitLoopNodes() {
        KillAuraOrbitProcess process = getHuntOrbitProcess();
        if (process == null) {
            return Collections.emptyList();
        }
        return process.getNavigationLoopSnapshot();
    }

    private void updateHuntOrbitStuckState(EntityPlayerSP player) {
        if (player == null) {
            huntOrbitStuckTicks = 0;
            huntLastOrbitPlayerX = Double.NaN;
            huntLastOrbitPlayerZ = Double.NaN;
            return;
        }
        if (Double.isNaN(huntLastOrbitPlayerX) || Double.isNaN(huntLastOrbitPlayerZ)) {
            huntLastOrbitPlayerX = player.posX;
            huntLastOrbitPlayerZ = player.posZ;
            huntOrbitStuckTicks = 0;
            return;
        }
        double dx = player.posX - huntLastOrbitPlayerX;
        double dz = player.posZ - huntLastOrbitPlayerZ;
        if (dx * dx + dz * dz <= 0.0036D) {
            huntOrbitStuckTicks++;
        } else {
            huntOrbitStuckTicks = 0;
        }
        huntLastOrbitPlayerX = player.posX;
        huntLastOrbitPlayerZ = player.posZ;
    }

    private int chooseHuntOrbitLoopNodeIndex(EntityPlayerSP player, List<BetterBlockPos> orbitNodes) {
        if (player == null || orbitNodes == null || orbitNodes.isEmpty()) {
            return -1;
        }
        int nearestIndex = -1;
        double nearestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < orbitNodes.size(); i++) {
            BetterBlockPos node = orbitNodes.get(i);
            double dx = player.posX - (node.x + 0.5D);
            double dz = player.posZ - (node.z + 0.5D);
            double distSq = dx * dx + dz * dz;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearestIndex = i;
            }
        }
        if (nearestIndex < 0) {
            return -1;
        }
        if (nearestDistSq <= 2.25D && orbitNodes.size() > 1) {
            return (nearestIndex + 1) % orbitNodes.size();
        }
        return nearestIndex;
    }

    private void stopHuntOrbitProcess() {
        KillAuraOrbitProcess process = getHuntOrbitProcess();
        if (process != null) {
            process.requestStop();
        }
    }

    private void stopHuntNavigationMode() {
        huntOrbitController.stop();
        stopHuntOrbitProcess();
        stopHuntEmbeddedNavigation();
        huntOrbitLoopNodeIndex = -1;
        huntLastOrbitGotoTick = -99999;
        huntOrbitStuckTicks = 0;
        huntLastOrbitPlayerX = Double.NaN;
        huntLastOrbitPlayerZ = Double.NaN;
    }

    private void stopHuntEmbeddedNavigation() {
        boolean hadEmbeddedGoal = lastHuntGotoTargetEntityId != -1L
                || !Double.isNaN(lastHuntGotoTargetX)
                || !Double.isNaN(lastHuntGotoTargetY)
                || !Double.isNaN(lastHuntGotoTargetZ);
        if (hadEmbeddedGoal) {
            EmbeddedNavigationHandler.INSTANCE.stop();
        }
        huntMovementStopped = true;
        lastHuntGotoTargetEntityId = -1L;
        lastHuntGotoTargetX = Double.NaN;
        lastHuntGotoTargetY = Double.NaN;
        lastHuntGotoTargetZ = Double.NaN;
    }

    private void navigateHuntTowardsTarget(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return;
        }
        if (isHuntFixedDistanceMode()) {
            double[] destination = computeFixedDistanceHuntDestination(player, target, getHuntDesiredDistance());
            double[] safeDestination = findSafeHuntNavigationDestination(destination[0], destination[1], destination[2]);
            if (safeDestination != null) {
                EmbeddedNavigationHandler.INSTANCE.startGoto(safeDestination[0], safeDestination[1], safeDestination[2]);
            } else {
                EmbeddedNavigationHandler.INSTANCE.startGoto(destination[0], destination[1], destination[2]);
            }
            lastHuntGotoTargetEntityId = target.getEntityId();
            lastHuntGotoTargetX = destination[0];
            lastHuntGotoTargetY = destination[1];
            lastHuntGotoTargetZ = destination[2];
        } else if (shouldRefreshHuntGoto(target)) {
            EmbeddedNavigationHandler.INSTANCE.startGoto(target.posX, target.posY, target.posZ);
            lastHuntGotoTargetEntityId = target.getEntityId();
            lastHuntGotoTargetX = target.posX;
            lastHuntGotoTargetY = target.posY;
            lastHuntGotoTargetZ = target.posZ;
        }
        huntMovementStopped = false;
    }

    private double[] computeFixedDistanceHuntDestination(EntityPlayerSP player, EntityLivingBase target, double desiredDistance) {
        double dx = player.posX - target.posX;
        double dy = player.posY - target.posY;
        double dz = player.posZ - target.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= 1.0E-4D) {
            double yawRadians = Math.toRadians(player.rotationYaw);
            dx = -Math.sin(yawRadians);
            dy = 0.0D;
            dz = Math.cos(yawRadians);
            distance = Math.sqrt(dx * dx + dz * dz);
        }

        double scale = desiredDistance / Math.max(distance, 1.0E-4D);
        double destinationX = target.posX + dx * scale;
        double destinationY = target.posY + dy * scale;
        double destinationZ = target.posZ + dz * scale;
        return clipHuntDestinationXZ(target.posX, target.posZ, destinationX, destinationY, destinationZ);
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

        double clipped[] = getClippedHuntPoint(centerX, centerZ, distance, Math.atan2(dz, dx));
        return new double[] { clipped[0], destinationY, clipped[1] };
    }

    private double[] findSafeHuntNavigationDestination(double desiredX, double desiredY, double desiredZ) {
        if (mc.world == null || mc.player == null) {
            return null;
        }

        int baseX = MathHelper.floor(desiredX);
        int baseY = MathHelper.floor(desiredY);
        int baseZ = MathHelper.floor(desiredZ);
        BlockPos bestStandPos = null;
        double bestScore = Double.MAX_VALUE;
        int maxFeetY = MathHelper.floor(mc.player.getEntityBoundingBox().minY + 0.001D) + 1;

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
        if (mc.world == null || standPos == null) {
            return false;
        }
        if (standPos.getY() > maxFeetY) {
            return false;
        }
        IBlockState feetState = mc.world.getBlockState(standPos);
        IBlockState headState = mc.world.getBlockState(standPos.up());
        IBlockState belowState = mc.world.getBlockState(standPos.down());

        boolean feetPassable = !feetState.getMaterial().blocksMovement();
        boolean headPassable = !headState.getMaterial().blocksMovement();
        boolean hasGround = belowState.getMaterial().blocksMovement();
        return feetPassable && headPassable && hasGround;
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

    private boolean isValidHuntCandidate(EntityPlayerSP player, EntityLivingBase entity) {
        if (player == null || entity == null || entity == player || !entity.isEntityAlive()) {
            return false;
        }
        if (getDistanceSqToHuntCenter(entity) > getHuntRadiusSq()) {
            return false;
        }
        // 检查是否忽略隐身目标
        if (huntIgnoreInvisible && entity.isInvisible()) {
            return false;
        }
        if (!matchesHuntTargetGroup(entity)) {
            return false;
        }
        String filterName = getHuntFilterableEntityName(entity);
        if (huntEnableNameBlacklist
                && KillAuraHandler.getNameListMatchIndex(filterName, huntNameBlacklist) != Integer.MAX_VALUE) {
            return false;
        }
        if (huntEnableNameWhitelist) {
            return KillAuraHandler.getNameListMatchIndex(filterName, huntNameWhitelist) != Integer.MAX_VALUE;
        }
        return true;
    }

    private boolean isLockedHuntTargetStillTrackable(EntityPlayerSP player, EntityLivingBase entity) {
        return player != null
                && entity != null
                && entity != player
                && entity.isEntityAlive()
                && !entity.isDead
                && entity.world == mc.world;
    }

    private boolean matchesHuntTargetGroup(EntityLivingBase entity) {
        if (!huntRestrictTargetGroups) {
            return true;
        }
        if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
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

    private boolean isHostileHuntTarget(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        return entity instanceof IMob
                || entity instanceof EntityDragon
                || entity.isCreatureType(EnumCreatureType.MONSTER, false);
    }

    private boolean isPassiveHuntTarget(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        return entity instanceof EntityAnimal
                || entity instanceof EntityAmbientCreature
                || entity instanceof EntityWaterMob
                || entity instanceof EntityVillager
                || entity instanceof EntityGolem
                || entity.isCreatureType(EnumCreatureType.CREATURE, false)
                || entity.isCreatureType(EnumCreatureType.AMBIENT, false)
                || entity.isCreatureType(EnumCreatureType.WATER_CREATURE, false);
    }

    private int compareHuntTargets(EntityPlayerSP player, EntityLivingBase left, EntityLivingBase right) {
        if (huntEnableNameWhitelist && !huntNameWhitelist.isEmpty()) {
            int leftPriority = KillAuraHandler.getNameListMatchIndex(getHuntFilterableEntityName(left), huntNameWhitelist);
            int rightPriority = KillAuraHandler.getNameListMatchIndex(getHuntFilterableEntityName(right), huntNameWhitelist);
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }
        }
        int compareByCenter = Double.compare(getDistanceSqToHuntCenter(left), getDistanceSqToHuntCenter(right));
        if (compareByCenter != 0) {
            return compareByCenter;
        }
        return Double.compare(player.getDistanceSq(left), player.getDistanceSq(right));
    }

    private AxisAlignedBB getHuntSearchBounds() {
        return new AxisAlignedBB(
                huntCenterX - huntRadius, huntCenterY - huntRadius, huntCenterZ - huntRadius,
                huntCenterX + huntRadius, huntCenterY + huntRadius, huntCenterZ + huntRadius);
    }

    private double getDistanceSqToHuntCenter(Entity entity) {
        if (entity == null) {
            return Double.MAX_VALUE;
        }
        double dx = entity.posX - huntCenterX;
        double dy = entity.posY - huntCenterY;
        double dz = entity.posZ - huntCenterZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private double getHuntRadiusSq() {
        return huntRadius * huntRadius;
    }

    private boolean shouldRefreshHuntGoto(Entity targetEntity) {
        if (targetEntity == null) {
            return false;
        }
        if (lastHuntGotoTargetEntityId != targetEntity.getEntityId()) {
            return true;
        }
        if (Double.isNaN(lastHuntGotoTargetX) || Double.isNaN(lastHuntGotoTargetY) || Double.isNaN(lastHuntGotoTargetZ)) {
            return true;
        }
        double dx = targetEntity.posX - lastHuntGotoTargetX;
        double dy = targetEntity.posY - lastHuntGotoTargetY;
        double dz = targetEntity.posZ - lastHuntGotoTargetZ;
        return dx * dx + dy * dy + dz * dz >= 1.0D;
    }

    private String getHuntFilterableEntityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        String displayName = entity.getDisplayName() == null ? "" : entity.getDisplayName().getUnformattedText();
        String normalized = KillAuraHandler.normalizeFilterName(displayName);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return KillAuraHandler.normalizeFilterName(entity.getName());
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

    private boolean readHuntBooleanParam(JsonObject params, String key, boolean defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private int readHuntIntParam(JsonObject params, String key, int defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsInt();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double readHuntDoubleParam(JsonObject params, String key, double defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            return params.get(key).getAsDouble();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String readHuntModeParam(JsonObject params, String key, String defaultValue) {
        if (params == null || key == null || !params.has(key)) {
            return defaultValue;
        }
        try {
            String mode = params.get(key).getAsString();
            if (KillAuraHandler.HUNT_MODE_APPROACH.equalsIgnoreCase(mode)) {
                return KillAuraHandler.HUNT_MODE_APPROACH;
            }
            if (KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equalsIgnoreCase(mode)) {
                return KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }
    
    // --- 渲染搜怪范围可视化 ---
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isHunting) {
            return;
        }

        EntityPlayerSP player = mc.player;
        Entity viewer = mc.getRenderViewEntity();
        if (player == null || viewer == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        if (huntShowRange) {
            drawHuntRangeVisualization(huntCenterX, huntCenterY, huntCenterZ, viewerX, viewerY, viewerZ, huntRadius);
        }
        renderHuntOrbitLoop();
    }

    private void drawHuntRangeVisualization(double centerX, double centerY, double centerZ,
            double viewerX, double viewerY, double viewerZ, double radius) {
        double safeRadius = Math.max(0.5D, radius);
        int segments = Math.max(36, (int) Math.round(safeRadius * 10.0D));

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(centerX - viewerX, centerY + 0.05D - viewerY, centerZ - viewerZ)
                .color(0.15F, 0.75F, 1.0F, 0.10F).endVertex();
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double x = centerX + Math.cos(angle) * safeRadius;
            double z = centerZ + Math.sin(angle) * safeRadius;
            buffer.pos(x - viewerX, centerY + 0.05D - viewerY, z - viewerZ)
                    .color(0.15F, 0.75F, 1.0F, 0.02F).endVertex();
        }
        tessellator.draw();

        GlStateManager.glLineWidth(4.0F);
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double x = centerX + Math.cos(angle) * safeRadius;
            double z = centerZ + Math.sin(angle) * safeRadius;
            buffer.pos(x - viewerX, centerY + 0.05D - viewerY, z - viewerZ)
                    .color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
        }
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderHuntOrbitLoop() {
        if (!isHuntOrbitMode() || !(huntTargetEntity instanceof EntityLivingBase) || !huntTargetEntity.isEntityAlive()) {
            return;
        }
        List<Vec3d> renderLoop = null;
        KillAuraOrbitProcess process = getHuntOrbitProcess();
        if (process != null) {
            List<Vec3d> processLoop = process.getRenderedLoopView();
            if (processLoop != null && processLoop.size() >= 2) {
                renderLoop = processLoop;
            }
        }
        if (renderLoop == null || renderLoop.size() < 2) {
            renderLoop = HuntOrbitController.buildPreviewLoop((EntityLivingBase) huntTargetEntity,
                    getHuntDesiredDistance(), KillAuraHandler.getConfiguredHuntOrbitSamplePoints());
        }
        if (renderLoop.size() < 2) {
            return;
        }
        PathRenderer.drawPolyline(renderLoop, new Color(0xFF3B30), 0.95F, 3.0F, true);
    }
    // --- 新增结束 ---

    // --- 新增：跟随实体模式的核心逻辑 ---
    private void executeFollowEntityTick(EntityPlayerSP player) {
        if (mc.world == null || player == null) {
            completeFollowEntityAction();
            return;
        }

        // 检查超时
        if (followTimeoutSeconds > 0) {
            long elapsedSeconds = (System.currentTimeMillis() - followStartTime) / 1000L;
            if (elapsedSeconds >= followTimeoutSeconds) {
                zszlScriptMod.LOGGER.info("跟随实体超时，动作结束");
                completeFollowEntityAction();
                return;
            }
        }

        // 查找或更新目标实体
        if (followTargetEntity == null || !followTargetEntity.isEntityAlive()) {
            followTargetEntity = findFollowTarget(player);
            if (followTargetEntity == null) {
                if (followStopOnLost) {
                    zszlScriptMod.LOGGER.info("未找到跟随目标，动作结束");
                    completeFollowEntityAction();
                }
                return;
            }
            zszlScriptMod.LOGGER.info("找到跟随目标: {}", followTargetEntity.getName());
        }

        // 计算与目标的距离
        double distanceToTarget = player.getDistance(followTargetEntity);
        
        // 如果距离大于跟随距离，则导航到目标
        if (distanceToTarget > followDistance + 0.5D) {
            EmbeddedNavigationHandler.INSTANCE.startGoto(
                    followTargetEntity.posX,
                    followTargetEntity.posY,
                    followTargetEntity.posZ);
        } else if (distanceToTarget < followDistance - 0.5D) {
            // 如果太近，停止移动
            EmbeddedNavigationHandler.INSTANCE.stop();
        }
    }

    private Entity findFollowTarget(EntityPlayerSP player) {
        if (player == null || mc.world == null) {
            return null;
        }

        List<Entity> candidates = new ArrayList<>();
        AxisAlignedBB searchBox = player.getEntityBoundingBox().grow(followSearchRadius);

        for (Entity entity : mc.world.getLoadedEntityList()) {
            if (entity == null || entity == player || !entity.isEntityAlive()) {
                continue;
            }
            if (!searchBox.intersects(entity.getEntityBoundingBox())) {
                continue;
            }
            if (!isValidFollowTarget(entity)) {
                continue;
            }
            candidates.add(entity);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // 如果指定了名称，优先匹配名称
        if (!followTargetName.isEmpty()) {
            for (Entity entity : candidates) {
                String entityName = entity.getName().toLowerCase(Locale.ROOT);
                if (entityName.contains(followTargetName.toLowerCase(Locale.ROOT))) {
                    return entity;
                }
            }
        }

        // 否则返回最近的目标
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity entity : candidates) {
            double dist = player.getDistanceSq(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        return nearest;
    }

    private boolean isValidFollowTarget(Entity entity) {
        if (entity == null) {
            return false;
        }

        String type = followEntityType.toLowerCase(Locale.ROOT);
        
        if ("player".equals(type)) {
            return entity instanceof EntityPlayer;
        }
        
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }

        EntityLivingBase living = (EntityLivingBase) entity;
        
        if ("hostile".equals(type) || "monster".equals(type)) {
            return isHostileHuntTarget(living);
        }
        
        if ("passive".equals(type) || "animal".equals(type)) {
            return isPassiveHuntTarget(living);
        }
        
        // "all" 或其他值：接受所有生物
        return true;
    }

    private void resetFollowEntityState() {
        this.isFollowingEntity = false;
        this.followEntityType = "player";
        this.followTargetName = "";
        this.followSearchRadius = 16.0D;
        this.followDistance = 3.0D;
        this.followTimeoutSeconds = 0;
        this.followStopOnLost = true;
        this.followTargetEntity = null;
        this.followStartTime = 0L;
    }

    private void completeFollowEntityAction() {
        resetFollowEntityState();
        if (shouldStopNavigationOnFinish()) {
            EmbeddedNavigationHandler.INSTANCE.stop();
        }
        actionIndex++;
        applyBuiltinSequenceDelay();
        consumeDebugProgress("跟随实体动作完成");
    }

    private static final class HuntAttackSequenceExecutor {
        private static final int POST_ACTION_DELAY_TICKS = 5;

        private PathSequence sequence;
        private int stepIndex = 0;
        private int actionIndex = 0;
        private int tickDelay = 0;
        private int targetEntityId = Integer.MIN_VALUE;
        private final ScopedRuntimeVariables runtimeVariables = new ScopedRuntimeVariables();
        private final Map<String, String> heldKeys = new LinkedHashMap<>();

        boolean isRunning() {
            return this.sequence != null;
        }

        void start(PathSequence sourceSequence, EntityPlayerSP player, EntityLivingBase target) {
            stop();
            if (sourceSequence == null || sourceSequence.getSteps().isEmpty()) {
                return;
            }

            this.sequence = new PathSequence(sourceSequence);
            this.stepIndex = 0;
            this.actionIndex = 0;
            this.tickDelay = 0;
            this.targetEntityId = target == null ? Integer.MIN_VALUE : target.getEntityId();
            this.runtimeVariables.clear();
            populateTargetVariables(player, target);
            this.runtimeVariables.enterStep(this.stepIndex);
        }

        void stop() {
            releaseHeldKeys();
            this.sequence = null;
            this.stepIndex = 0;
            this.actionIndex = 0;
            this.tickDelay = 0;
            this.targetEntityId = Integer.MIN_VALUE;
            this.runtimeVariables.clear();
            this.heldKeys.clear();
        }

        void tick(EntityPlayerSP player) {
            if (!isRunning()) {
                return;
            }
            if (player == null) {
                stop();
                return;
            }
            refreshTargetVariables(player);
            if (this.tickDelay > 0) {
                this.tickDelay--;
                return;
            }

            int guard = 0;
            while (isRunning() && guard++ < 128) {
                if (this.sequence == null || this.stepIndex >= this.sequence.getSteps().size()) {
                    stop();
                    return;
                }

                PathStep currentStep = this.sequence.getSteps().get(this.stepIndex);
                List<ActionData> actions = currentStep == null ? null : currentStep.getActions();
                if (actions == null || this.actionIndex >= actions.size()) {
                    this.stepIndex++;
                    this.actionIndex = 0;
                    this.runtimeVariables.enterStep(this.stepIndex);
                    continue;
                }

                ActionData rawAction = actions.get(this.actionIndex);
                ActionData resolvedAction = resolveActionData(rawAction, player);
                if (resolvedAction == null || resolvedAction.type == null) {
                    this.actionIndex++;
                    continue;
                }

                String actionType = resolvedAction.type.trim().toLowerCase(Locale.ROOT);
                if (actionType.isEmpty() || shouldSkipAction(actionType)) {
                    this.actionIndex++;
                    continue;
                }

                Consumer<EntityPlayerSP> action = PathSequenceManager.parseAction(resolvedAction.type,
                        resolvedAction.params);
                if (action == null) {
                    this.actionIndex++;
                    continue;
                }

                if (action instanceof ModUtils.DelayAction) {
                    this.tickDelay = ((ModUtils.DelayAction) action).getDelayTicks();
                    this.actionIndex++;
                    return;
                }

                try {
                    action.accept(player);
                } catch (Exception e) {
                    zszlScriptMod.LOGGER.error("[hunt_attack_sequence] 执行动作失败: {}", resolvedAction.getDescription(), e);
                }

                updateHeldKeyState(resolvedAction);
                this.actionIndex++;
                this.tickDelay = POST_ACTION_DELAY_TICKS;
                return;
            }
        }

        private ActionData resolveActionData(ActionData actionData, EntityPlayerSP player) {
            if (actionData == null) {
                return null;
            }
            this.runtimeVariables.beginAction(this.stepIndex, this.actionIndex);

            JsonObject resolvedParams = LegacyActionRuntime.resolveParams(actionData.params, this.runtimeVariables,
                    player, this.sequence, this.stepIndex, this.actionIndex);
            return new ActionData(actionData.type, resolvedParams);
        }

        private boolean shouldSkipAction(String actionType) {
            return "run_sequence".equals(actionType) || "hunt".equals(actionType) || "set_var".equals(actionType)
                    || "goto_action".equals(actionType) || "repeat_actions".equals(actionType)
                    || "capture_nearby_entity".equals(actionType) || "capture_gui_title".equals(actionType)
                    || "capture_block_at".equals(actionType) || actionType.startsWith("condition_")
                    || actionType.startsWith("wait_until_");
        }

        private void populateTargetVariables(EntityPlayerSP player, EntityLivingBase target) {
            this.runtimeVariables.put("target_found", target != null);
            if (target == null) {
                this.runtimeVariables.remove("target_name");
                this.runtimeVariables.remove("target_id");
                this.runtimeVariables.remove("target_x");
                this.runtimeVariables.remove("target_y");
                this.runtimeVariables.remove("target_z");
                this.runtimeVariables.remove("target_block_x");
                this.runtimeVariables.remove("target_block_y");
                this.runtimeVariables.remove("target_block_z");
                this.runtimeVariables.remove("target_health");
                this.runtimeVariables.remove("target_distance");
                return;
            }

            this.runtimeVariables.put("target_name", target.getName());
            this.runtimeVariables.put("target_id", target.getEntityId());
            this.runtimeVariables.put("target_x", target.posX);
            this.runtimeVariables.put("target_y", target.posY);
            this.runtimeVariables.put("target_z", target.posZ);
            this.runtimeVariables.put("target_block_x", target.getPosition().getX());
            this.runtimeVariables.put("target_block_y", target.getPosition().getY());
            this.runtimeVariables.put("target_block_z", target.getPosition().getZ());
            this.runtimeVariables.put("target_health", target.getHealth());
            if (player != null) {
                this.runtimeVariables.put("target_distance", player.getDistance(target));
            }
        }

        private void refreshTargetVariables(EntityPlayerSP player) {
            if (player == null || player.world == null) {
                populateTargetVariables(player, null);
                return;
            }
            Entity targetEntity = this.targetEntityId == Integer.MIN_VALUE
                    ? null
                    : player.world.getEntityByID(this.targetEntityId);
            EntityLivingBase target = targetEntity instanceof EntityLivingBase ? (EntityLivingBase) targetEntity : null;
            if (target != null && (target.isDead || target.getHealth() <= 0.0F)) {
                target = null;
            }
            populateTargetVariables(player, target);
        }

        private void updateHeldKeyState(ActionData actionData) {
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
                this.heldKeys.put(key, "Up");
            } else if ("up".equals(normalizedState) || "robotup".equals(normalizedState)) {
                this.heldKeys.remove(key);
            }
        }

        private void releaseHeldKeys() {
            if (this.heldKeys.isEmpty()) {
                return;
            }

            for (Map.Entry<String, String> entry : this.heldKeys.entrySet()) {
                try {
                    ModUtils.simulateKey(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    zszlScriptMod.LOGGER.warn("[hunt_attack_sequence] 释放按键失败: {}", entry.getKey(), e);
                }
            }
        }
    }
    // --- 新增结束 ---
}

