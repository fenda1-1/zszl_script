package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.path.LegacyActionRuntime;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.PacketEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.type.EventState;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.listener.AbstractGameEventListener;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.listener.IEventBus;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BetterBlockPos;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.Rotation;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.RotationUtils;
import com.zszl.zszlScriptMod.shadowbaritone.process.KillAuraOrbitProcess;
import com.zszl.zszlScriptMod.shadowbaritone.utils.PathRenderer;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.TickRangeSpec;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KillAuraHandler implements AbstractGameEventListener {

    public static final KillAuraHandler INSTANCE = new KillAuraHandler();
    public static final String ATTACK_MODE_NORMAL = "NORMAL";
    public static final String ATTACK_MODE_PACKET = "PACKET";
    public static final String ATTACK_MODE_TELEPORT = "TELEPORT";
    public static final String ATTACK_MODE_SEQUENCE = "SEQUENCE";
    public static final String ATTACK_MODE_MOUSE_CLICK = "MOUSE_CLICK";
    public static final String DEFAULT_ATTACK_MODE = ATTACK_MODE_MOUSE_CLICK;
    public static final String HUNT_MODE_OFF = "OFF";
    public static final String HUNT_MODE_APPROACH = "APPROACH";
    public static final String HUNT_MODE_FIXED_DISTANCE = "FIXED_DISTANCE";
    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final Type PRESET_LIST_TYPE = new TypeToken<List<KillAuraPreset>>() {
    }.getType();

    public static boolean enabled = false;
    public static boolean rotateToTarget = true;
    public static boolean smoothRotation = true;
    public static final float MIN_SMOOTH_MAX_TURN_STEP = 0.5F;
    public static final float MAX_SMOOTH_MAX_TURN_STEP = 60.0F;
    public static final float DEFAULT_SMOOTH_MAX_TURN_STEP = 24.0F;
    public static final String DEFAULT_SMOOTH_MAX_TURN_STEP_SPEC = "24-60";
    public static float smoothMaxTurnStep = DEFAULT_SMOOTH_MAX_TURN_STEP;
    public static String smoothMaxTurnStepSpec = DEFAULT_SMOOTH_MAX_TURN_STEP_SPEC;
    public static boolean rotateOnlyOnAttack = true;
    public static boolean relockOnlyWhenNoCrosshairTarget = true;
    public static boolean onlyAttackWhenLookingAtTarget = true;
    public static boolean requireLineOfSight = true;
    public static boolean targetHostile = true;
    public static boolean targetPassive = false;
    public static boolean targetPlayers = false;
    public static boolean onlyWeapon = false;
    public static boolean aimOnlyMode = false;
    public static boolean focusSingleTarget = true;
    public static boolean ignoreInvisible = true;
    public static boolean enableNoCollision = true;
    public static boolean enableAntiKnockback = true;
    public static boolean enableFullBrightVision = false;
    public static float fullBrightGamma = 1000.0F;
    public static String attackMode = DEFAULT_ATTACK_MODE;
    public static String attackSequenceName = "";
    public static final int MIN_ATTACK_SEQUENCE_DELAY_TICKS = 0;
    public static final int MAX_ATTACK_SEQUENCE_DELAY_TICKS = 200;
    public static final int DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS = 1;
    public static final String DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS_SPEC = "1-6";
    public static int attackSequenceDelayTicks = DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS;
    public static String attackSequenceDelayTicksSpec = DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS_SPEC;
    public static final float MIN_AIM_OFFSET = -30.0F;
    public static final float MAX_AIM_OFFSET = 30.0F;
    public static final float DEFAULT_AIM_OFFSET = 0.0F;
    public static final float DEFAULT_AIM_YAW_OFFSET = -3.0F;
    public static final String DEFAULT_AIM_YAW_OFFSET_SPEC = "-3-3";
    public static final float DEFAULT_AIM_PITCH_OFFSET = -11.0F;
    public static final String DEFAULT_AIM_PITCH_OFFSET_SPEC = "-11-11";
    public static float aimYawOffset = DEFAULT_AIM_YAW_OFFSET;
    public static String aimYawOffsetSpec = DEFAULT_AIM_YAW_OFFSET_SPEC;
    public static float aimPitchOffset = DEFAULT_AIM_PITCH_OFFSET;
    public static String aimPitchOffsetSpec = DEFAULT_AIM_PITCH_OFFSET_SPEC;
    public static boolean huntEnabled = true;
    public static String huntMode = HUNT_MODE_APPROACH;
    public static boolean huntPickupItemsEnabled = false;
    public static boolean visualizeHuntRadius = false;
    public static float huntRadius = 8.0F;
    public static float huntFixedDistance = 4.2F;
    public static final float DEFAULT_HUNT_UP_RANGE = 1.0F;
    public static final float DEFAULT_HUNT_DOWN_RANGE = 1.0F;
    public static float huntUpRange = DEFAULT_HUNT_UP_RANGE;
    public static float huntDownRange = DEFAULT_HUNT_DOWN_RANGE;
    public static boolean huntOrbitEnabled = false;
    public static boolean huntJumpOrbitEnabled = true;
    public static final int MIN_HUNT_ORBIT_SAMPLE_POINTS = 3;
    public static final int MAX_HUNT_ORBIT_SAMPLE_POINTS = 360;
    public static final int DEFAULT_HUNT_ORBIT_SAMPLE_POINTS = MAX_HUNT_ORBIT_SAMPLE_POINTS;
    public static int huntOrbitSamplePoints = DEFAULT_HUNT_ORBIT_SAMPLE_POINTS;
    public static boolean enableNameWhitelist = false;
    public static boolean enableNameBlacklist = false;
    public static List<String> nameWhitelist = new ArrayList<>();
    public static List<String> nameBlacklist = new ArrayList<>();
    public static float nearbyEntityScanRange = 10.0F;
    public static final List<KillAuraPreset> presets = new ArrayList<>();

    public static float attackRange = 4.2F;
    public static float minAttackStrength = 0.92F;
    public static float minTurnSpeed = 4.0F;
    public static float maxTurnSpeed = 18.0F;
    public static int minAttackIntervalTicks = 2;
    public static int targetsPerAttack = 1;
    public static final int DEFAULT_NO_DAMAGE_ATTACK_LIMIT = 5;
    public static final int MAX_NO_DAMAGE_ATTACK_LIMIT = 100;
    public static int noDamageAttackLimit = DEFAULT_NO_DAMAGE_ATTACK_LIMIT;

    private static final int HUNT_GOTO_INTERVAL_TICKS = 6;
    private static final double HUNT_GOTO_MOVE_THRESHOLD_SQ = 1.0D;
    private static final double TARGET_SWITCH_DISTANCE_HYSTERESIS_SQ = 1.0D;
    private static final double HUNT_FIXED_DISTANCE_TOLERANCE = 0.30D;
    private static final int HUNT_ORBIT_PROCESS_REQUEST_INTERVAL_TICKS = 2;
    private static final double HUNT_ORBIT_MAX_ENTRY_DISTANCE_BUFFER = 4.0D;
    private static final double HUNT_ORBIT_MAX_ENTRY_VERTICAL_DELTA = 3.5D;
    private static final double HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER = 1.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER = 2.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_LOOP_ENTRY_MAX_DISTANCE = 0.90D;
    private static final double HUNT_ORBIT_ENTRY_RADIUS_BAND = 0.45D;
    private static final int HUNT_ORBIT_ENTRY_SAFE_SEARCH_RADIUS = 1;
    private static final double HUNT_ORBIT_ENTRY_POINT_TOLERANCE = 0.85D;
    private static final int HUNT_PICKUP_GOTO_INTERVAL_TICKS = 5;
    private static final int HUNT_PICKUP_SEARCH_INTERVAL_TICKS = 3;
    private static final double HUNT_PICKUP_OVERLAP_GROWTH = 0.05D;
    private static final double HUNT_APPROACH_MIN_STAND_RADIUS = 0.85D;
    private static final double HUNT_APPROACH_TARGET_BUFFER = 0.35D;
    private static final double HUNT_NAVIGATION_RADIUS_SAMPLE_STEP = 0.75D;
    private static final double HUNT_NAVIGATION_ANGLE_SAMPLE_STEP_RADIANS = Math.toRadians(18.0D);
    private static final int HUNT_NAVIGATION_ANGLE_SAMPLE_PAIRS = 10;
    private static final double TELEPORT_ATTACK_STEP_DISTANCE = 8.0D;
    private static final double TELEPORT_ATTACK_REACH = 2.85D;
    private static final float TELEPORT_ATTACK_MIN_RANGE = 6.0F;
    private static final int TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS = 4;
    private static final int TELEPORT_ATTACK_MAX_CORRECTIONS = 2;
    private static final double TELEPORT_ATTACK_SAFE_ANGLE_STEP_RADIANS = Math.toRadians(12.0D);
    private static final int TELEPORT_ATTACK_SAFE_ANGLE_STEPS = 10;
    private static final double TELEPORT_ATTACK_SAFE_RADIUS_STEP = 0.4D;
    private static final double TELEPORT_ATTACK_MAX_RADIUS_ADJUST = 1.4D;
    private static final double TELEPORT_ATTACK_ORIGIN_TOLERANCE_SQ = 0.09D;
    private static final double TELEPORT_ATTACK_WAYPOINT_EPSILON_SQ = 0.04D;
    private static final int TARGET_SWITCH_SMOOTH_TICKS = 12;
    private static final float SMOOTH_ATTACK_READY_YAW_DEGREES = 28.0F;
    private static final float SMOOTH_ATTACK_READY_PITCH_DEGREES = 40.0F;
    private static final double ADVANCED_AIM_MIN_LEAD_TICKS = 0.25D;
    private static final double ADVANCED_AIM_MAX_LEAD_TICKS = 2.45D;
    private static final double ADVANCED_AIM_MAX_PLAYER_LEAD_TICKS = 0.85D;
    private static final float ADVANCED_OVERSHOOT_MAX_YAW = 2.15F;
    private static final float ADVANCED_OVERSHOOT_MAX_ATTACK_YAW = 1.25F;
    private static final float ADVANCED_OVERSHOOT_MAX_PITCH = 0.95F;
    private static final float NO_DAMAGE_HEALTH_EPSILON = 0.001F;
    private static final int NO_DAMAGE_OBSERVATION_DELAY_TICKS = 1;
    private static final int NO_DAMAGE_MAX_TRACKED_TARGETS = 256;
    private static final int NO_DAMAGE_MAX_EXCLUDED_TARGETS = 512;

    private int attackCooldownTicks = 0;
    private int sequenceCooldownTicks = 0;
    private int currentTargetEntityId = -1;
    private int lastAimTargetEntityId = Integer.MIN_VALUE;
    private int targetSwitchSmoothTicks = 0;
    private int lastSmoothTurnLimitTick = Integer.MIN_VALUE;
    private float smoothYawTurnUsedThisTick = 0.0F;
    private float smoothPitchTurnUsedThisTick = 0.0F;
    private int visualRotationCacheTick = Integer.MIN_VALUE;
    private int visualRotationCacheTargetEntityId = Integer.MIN_VALUE;
    private float visualRotationCacheSourceYaw = 0.0F;
    private float visualRotationCacheSourcePitch = 0.0F;
    private Rotation visualRotationCache = null;
    private int aimOffsetSampleTick = Integer.MIN_VALUE;
    private int aimOffsetSampleTargetEntityId = Integer.MIN_VALUE;
    private String aimOffsetSampleYawSpec = "";
    private String aimOffsetSamplePitchSpec = "";
    private float sampledAimYawOffset = 0.0F;
    private float sampledAimPitchOffset = 0.0F;
    private boolean huntNavigationActive = false;
    private int lastHuntGotoTick = -99999;
    private int lastHuntTargetEntityId = Integer.MIN_VALUE;
    private double lastHuntTargetX = 0.0D;
    private double lastHuntTargetZ = 0.0D;
    private boolean huntPickupNavigationActive = false;
    private int lastHuntPickupGotoTick = -99999;
    private int lastHuntPickupTargetEntityId = Integer.MIN_VALUE;
    private int lastHuntPickupSearchTick = -99999;
    private int lastHuntPickupSearchTargetEntityId = Integer.MIN_VALUE;
    private boolean lastHuntPickupSearchFound = false;
    private int lastOrbitProcessRequestTick = -99999;
    private int lastOrbitProcessTargetEntityId = Integer.MIN_VALUE;
    private double lastOrbitProcessRequestedRadius = Double.NaN;
    private double lastSafeMotionX = 0.0D;
    private double lastSafeMotionY = 0.0D;
    private double lastSafeMotionZ = 0.0D;
    private boolean fullBrightApplied = false;
    private float previousGammaSetting = 1.0F;
    private IEventBus registeredBaritoneEventBus = null;
    private TeleportAttackPlan activeTeleportAttackPlan = null;
    private int pendingTeleportReturnTicks = 0;
    private int lastTeleportCorrectionTick = Integer.MIN_VALUE;
    private final AttackSequenceExecutor attackSequenceExecutor = new AttackSequenceExecutor();
    private final HuntOrbitController huntOrbitController = new HuntOrbitController();
    private final Map<Integer, NoDamageAttackTracker> noDamageAttackTrackers = new LinkedHashMap<>();
    private final Set<Integer> noDamageExcludedEntityIds = new LinkedHashSet<>();
    private int areaHuntControlTicks = 0;

    public static class KillAuraPreset {
        public String name = "";
        public boolean rotateToTarget = true;
        public boolean smoothRotation = true;
        public float smoothMaxTurnStep = DEFAULT_SMOOTH_MAX_TURN_STEP;
        public String smoothMaxTurnStepSpec = DEFAULT_SMOOTH_MAX_TURN_STEP_SPEC;
        public boolean rotateOnlyOnAttack = true;
        public boolean relockOnlyWhenNoCrosshairTarget = true;
        public boolean onlyAttackWhenLookingAtTarget = true;
        public boolean requireLineOfSight = true;
        public boolean targetHostile = true;
        public boolean targetPassive = false;
        public boolean targetPlayers = false;
        public boolean onlyWeapon = false;
        public boolean aimOnlyMode = false;
        public boolean focusSingleTarget = true;
        public boolean ignoreInvisible = true;
        public boolean enableNoCollision = true;
        public boolean enableAntiKnockback = true;
        public boolean enableFullBrightVision = false;
        public float fullBrightGamma = 1000.0F;
        public String attackMode = DEFAULT_ATTACK_MODE;
        public String attackSequenceName = "";
        public int attackSequenceDelayTicks = DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS;
        public String attackSequenceDelayTicksSpec = DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS_SPEC;
        public float aimYawOffset = DEFAULT_AIM_YAW_OFFSET;
        public String aimYawOffsetSpec = DEFAULT_AIM_YAW_OFFSET_SPEC;
        public float aimPitchOffset = DEFAULT_AIM_PITCH_OFFSET;
        public String aimPitchOffsetSpec = DEFAULT_AIM_PITCH_OFFSET_SPEC;
        public String huntMode = HUNT_MODE_APPROACH;
        public boolean huntPickupItemsEnabled = false;
        public boolean visualizeHuntRadius = false;
        public float huntRadius = 8.0F;
        public float huntFixedDistance = 4.2F;
        public float huntUpRange = DEFAULT_HUNT_UP_RANGE;
        public float huntDownRange = DEFAULT_HUNT_DOWN_RANGE;
        public boolean huntOrbitEnabled = false;
        public boolean huntJumpOrbitEnabled = true;
        public int huntOrbitSamplePoints = DEFAULT_HUNT_ORBIT_SAMPLE_POINTS;
        public boolean enableNameWhitelist = false;
        public boolean enableNameBlacklist = false;
        public List<String> nameWhitelist = new ArrayList<>();
        public List<String> nameBlacklist = new ArrayList<>();
        public float nearbyEntityScanRange = 10.0F;
        public float attackRange = 4.2F;
        public float minAttackStrength = 0.92F;
        public float minTurnSpeed = 4.0F;
        public float maxTurnSpeed = 18.0F;
        public int minAttackIntervalTicks = 2;
        public int targetsPerAttack = 1;
        public int noDamageAttackLimit = DEFAULT_NO_DAMAGE_ATTACK_LIMIT;

        public KillAuraPreset() {
        }

        public KillAuraPreset(KillAuraPreset other) {
            if (other == null) {
                return;
            }
            this.name = other.name == null ? "" : other.name;
            this.rotateToTarget = other.rotateToTarget;
            this.smoothRotation = other.smoothRotation;
            this.smoothMaxTurnStep = other.smoothMaxTurnStep;
            this.smoothMaxTurnStepSpec = other.smoothMaxTurnStepSpec == null || other.smoothMaxTurnStepSpec.trim().isEmpty()
                    ? formatSmoothMaxTurnStepValue(other.smoothMaxTurnStep)
                    : other.smoothMaxTurnStepSpec;
            this.rotateOnlyOnAttack = other.rotateOnlyOnAttack;
            this.relockOnlyWhenNoCrosshairTarget = other.relockOnlyWhenNoCrosshairTarget;
            this.onlyAttackWhenLookingAtTarget = other.onlyAttackWhenLookingAtTarget;
            this.requireLineOfSight = other.requireLineOfSight;
            this.targetHostile = other.targetHostile;
            this.targetPassive = other.targetPassive;
            this.targetPlayers = other.targetPlayers;
            this.onlyWeapon = other.onlyWeapon;
            this.aimOnlyMode = other.aimOnlyMode;
            this.focusSingleTarget = other.focusSingleTarget;
            this.ignoreInvisible = other.ignoreInvisible;
            this.enableNoCollision = other.enableNoCollision;
            this.enableAntiKnockback = other.enableAntiKnockback;
            this.enableFullBrightVision = other.enableFullBrightVision;
            this.fullBrightGamma = other.fullBrightGamma;
            this.attackMode = other.attackMode == null ? ATTACK_MODE_NORMAL : other.attackMode;
            this.attackSequenceName = other.attackSequenceName == null ? "" : other.attackSequenceName;
            this.attackSequenceDelayTicks = other.attackSequenceDelayTicks;
            this.attackSequenceDelayTicksSpec = other.attackSequenceDelayTicksSpec == null
                    || other.attackSequenceDelayTicksSpec.trim().isEmpty()
                            ? String.valueOf(other.attackSequenceDelayTicks)
                            : other.attackSequenceDelayTicksSpec;
            this.aimYawOffset = other.aimYawOffset;
            this.aimYawOffsetSpec = other.aimYawOffsetSpec == null || other.aimYawOffsetSpec.trim().isEmpty()
                    ? formatAimOffsetValue(other.aimYawOffset)
                    : other.aimYawOffsetSpec;
            this.aimPitchOffset = other.aimPitchOffset;
            this.aimPitchOffsetSpec = other.aimPitchOffsetSpec == null || other.aimPitchOffsetSpec.trim().isEmpty()
                    ? formatAimOffsetValue(other.aimPitchOffset)
                    : other.aimPitchOffsetSpec;
            this.huntMode = other.huntMode == null ? HUNT_MODE_APPROACH : other.huntMode;
            this.huntPickupItemsEnabled = other.huntPickupItemsEnabled;
            this.visualizeHuntRadius = other.visualizeHuntRadius;
            this.huntRadius = other.huntRadius;
            this.huntFixedDistance = other.huntFixedDistance;
            this.huntUpRange = other.huntUpRange;
            this.huntDownRange = other.huntDownRange;
            this.huntOrbitEnabled = other.huntOrbitEnabled;
            this.huntJumpOrbitEnabled = other.huntJumpOrbitEnabled;
            this.huntOrbitSamplePoints = other.huntOrbitSamplePoints;
            this.enableNameWhitelist = other.enableNameWhitelist;
            this.enableNameBlacklist = other.enableNameBlacklist;
            this.nameWhitelist = new ArrayList<>(other.nameWhitelist == null ? new ArrayList<>() : other.nameWhitelist);
            this.nameBlacklist = new ArrayList<>(other.nameBlacklist == null ? new ArrayList<>() : other.nameBlacklist);
            this.nearbyEntityScanRange = other.nearbyEntityScanRange;
            this.attackRange = other.attackRange;
            this.minAttackStrength = other.minAttackStrength;
            this.minTurnSpeed = other.minTurnSpeed;
            this.maxTurnSpeed = other.maxTurnSpeed;
            this.minAttackIntervalTicks = other.minAttackIntervalTicks;
            this.targetsPerAttack = other.targetsPerAttack;
            this.noDamageAttackLimit = other.noDamageAttackLimit;
        }
    }

    private static final class NoDamageAttackTracker {
        private float baselineHealth;
        private int pendingAttempts;
        private int observationTicks;
        private int confirmedNoDamageAttempts;

        private NoDamageAttackTracker(float baselineHealth) {
            this.baselineHealth = baselineHealth;
        }
    }

    public static final class AreaHuntOptions {
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radius;
        private final double radiusSq;
        private final double upRange;
        private final double downRange;
        private final Predicate<EntityLivingBase> targetFilter;
        private final boolean whitelistOverridesTargetGroup;

        public AreaHuntOptions(double centerX, double centerY, double centerZ, double radius,
                double upRange, double downRange, Predicate<EntityLivingBase> targetFilter,
                boolean whitelistOverridesTargetGroup) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = Math.max(0.0D, radius);
            this.radiusSq = this.radius * this.radius;
            this.upRange = Math.max(0.0D, upRange);
            this.downRange = Math.max(0.0D, downRange);
            this.targetFilter = targetFilter;
            this.whitelistOverridesTargetGroup = whitelistOverridesTargetGroup;
        }

        private boolean allows(EntityLivingBase target) {
            return targetFilter == null || targetFilter.test(target);
        }

        private boolean shouldTreatAllowedTargetAsWhitelistMatched() {
            return whitelistOverridesTargetGroup;
        }

        private boolean contains(Entity target) {
            if (target == null) {
                return false;
            }
            double dx = target.posX - centerX;
            double dz = target.posZ - centerZ;
            if (dx * dx + dz * dz > radiusSq) {
                return false;
            }
            double dy = target.posY - centerY;
            return dy <= upRange + 1.0E-6D && -dy <= downRange + 1.0E-6D;
        }
    }

    public static final class AreaHuntTickResult {
        private final boolean hasTarget;
        private final EntityLivingBase target;
        private final int attackedCount;
        private final boolean sequenceRunning;

        private AreaHuntTickResult(boolean hasTarget, EntityLivingBase target, int attackedCount,
                boolean sequenceRunning) {
            this.hasTarget = hasTarget;
            this.target = target;
            this.attackedCount = attackedCount;
            this.sequenceRunning = sequenceRunning;
        }

        public boolean hasTarget() {
            return hasTarget;
        }

        public EntityLivingBase getTarget() {
            return target;
        }

        public int getAttackedCount() {
            return attackedCount;
        }

        public boolean isSequenceRunning() {
            return sequenceRunning;
        }
    }

    private KillAuraHandler() {
    }

    static {
        loadConfig();
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_killaura.json").toFile();
    }

    public static void loadConfig() {
        enabled = false;
        rotateToTarget = true;
        smoothRotation = true;
        smoothMaxTurnStep = DEFAULT_SMOOTH_MAX_TURN_STEP;
        smoothMaxTurnStepSpec = DEFAULT_SMOOTH_MAX_TURN_STEP_SPEC;
        rotateOnlyOnAttack = true;
        relockOnlyWhenNoCrosshairTarget = true;
        onlyAttackWhenLookingAtTarget = true;
        requireLineOfSight = true;
        targetHostile = true;
        targetPassive = false;
        targetPlayers = false;
        onlyWeapon = false;
        aimOnlyMode = false;
        focusSingleTarget = true;
        ignoreInvisible = true;
        enableNoCollision = true;
        enableAntiKnockback = true;
        enableFullBrightVision = false;
        fullBrightGamma = 1000.0F;
        attackMode = DEFAULT_ATTACK_MODE;
        attackSequenceName = "";
        attackSequenceDelayTicks = DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS;
        attackSequenceDelayTicksSpec = DEFAULT_ATTACK_SEQUENCE_DELAY_TICKS_SPEC;
        aimYawOffset = DEFAULT_AIM_YAW_OFFSET;
        aimYawOffsetSpec = DEFAULT_AIM_YAW_OFFSET_SPEC;
        aimPitchOffset = DEFAULT_AIM_PITCH_OFFSET;
        aimPitchOffsetSpec = DEFAULT_AIM_PITCH_OFFSET_SPEC;
        huntEnabled = true;
        huntMode = HUNT_MODE_APPROACH;
        huntPickupItemsEnabled = false;
        visualizeHuntRadius = false;
        huntRadius = 8.0F;
        huntFixedDistance = 4.2F;
        huntUpRange = DEFAULT_HUNT_UP_RANGE;
        huntDownRange = DEFAULT_HUNT_DOWN_RANGE;
        huntOrbitEnabled = false;
        huntJumpOrbitEnabled = true;
        huntOrbitSamplePoints = DEFAULT_HUNT_ORBIT_SAMPLE_POINTS;
        enableNameWhitelist = false;
        enableNameBlacklist = false;
        nameWhitelist = new ArrayList<>();
        nameBlacklist = new ArrayList<>();
        nearbyEntityScanRange = 10.0F;
        presets.clear();

        attackRange = 4.2F;
        minAttackStrength = 0.92F;
        minTurnSpeed = 4.0F;
        maxTurnSpeed = 18.0F;
        minAttackIntervalTicks = 2;
        targetsPerAttack = 1;
        noDamageAttackLimit = DEFAULT_NO_DAMAGE_ATTACK_LIMIT;

        try {
            File configFile = getConfigFile();
            if (!configFile.exists()) {
                normalizeConfig();
                return;
            }

            JsonObject json = new JsonParser().parse(new FileReader(configFile)).getAsJsonObject();
            if (json.has("enabled")) {
                enabled = json.get("enabled").getAsBoolean();
            }
            if (json.has("rotateToTarget")) {
                rotateToTarget = json.get("rotateToTarget").getAsBoolean();
            }
            if (json.has("smoothRotation")) {
                smoothRotation = json.get("smoothRotation").getAsBoolean();
            }
            if (json.has("smoothMaxTurnStep")) {
                setSmoothMaxTurnStepSpec(json.get("smoothMaxTurnStep").getAsString());
            }
            if (json.has("smoothMaxTurnStepSpec")) {
                setSmoothMaxTurnStepSpec(json.get("smoothMaxTurnStepSpec").getAsString());
            }
            if (json.has("rotateOnlyOnAttack")) {
                rotateOnlyOnAttack = json.get("rotateOnlyOnAttack").getAsBoolean();
            }
            if (json.has("relockOnlyWhenNoCrosshairTarget")) {
                relockOnlyWhenNoCrosshairTarget = json.get("relockOnlyWhenNoCrosshairTarget").getAsBoolean();
            }
            if (json.has("onlyAttackWhenLookingAtTarget")) {
                onlyAttackWhenLookingAtTarget = json.get("onlyAttackWhenLookingAtTarget").getAsBoolean();
            }
            if (json.has("requireLineOfSight")) {
                requireLineOfSight = json.get("requireLineOfSight").getAsBoolean();
            }
            if (json.has("targetHostile")) {
                targetHostile = json.get("targetHostile").getAsBoolean();
            }
            if (json.has("targetPassive")) {
                targetPassive = json.get("targetPassive").getAsBoolean();
            }
            if (json.has("targetPlayers")) {
                targetPlayers = json.get("targetPlayers").getAsBoolean();
            }
            if (json.has("onlyWeapon")) {
                onlyWeapon = json.get("onlyWeapon").getAsBoolean();
            }
            if (json.has("aimOnlyMode")) {
                aimOnlyMode = json.get("aimOnlyMode").getAsBoolean();
            }
            if (json.has("focusSingleTarget")) {
                focusSingleTarget = json.get("focusSingleTarget").getAsBoolean();
            }
            if (json.has("ignoreInvisible")) {
                ignoreInvisible = json.get("ignoreInvisible").getAsBoolean();
            }
            if (json.has("enableNoCollision")) {
                enableNoCollision = json.get("enableNoCollision").getAsBoolean();
            }
            if (json.has("enableAntiKnockback")) {
                enableAntiKnockback = json.get("enableAntiKnockback").getAsBoolean();
            }
            if (json.has("enableFullBrightVision")) {
                enableFullBrightVision = json.get("enableFullBrightVision").getAsBoolean();
            }
            if (json.has("fullBrightGamma")) {
                fullBrightGamma = json.get("fullBrightGamma").getAsFloat();
            }
            if (json.has("attackMode")) {
                attackMode = json.get("attackMode").getAsString();
            }
            if (json.has("attackSequenceName")) {
                attackSequenceName = json.get("attackSequenceName").getAsString();
            }
            if (json.has("attackSequenceDelayTicks")) {
                setAttackSequenceDelayTicksSpec(json.get("attackSequenceDelayTicks").getAsString());
            }
            if (json.has("attackSequenceDelayTicksSpec")) {
                setAttackSequenceDelayTicksSpec(json.get("attackSequenceDelayTicksSpec").getAsString());
            }
            if (json.has("aimYawOffset")) {
                setAimYawOffsetSpec(json.get("aimYawOffset").getAsString());
            }
            if (json.has("aimYawOffsetSpec")) {
                setAimYawOffsetSpec(json.get("aimYawOffsetSpec").getAsString());
            }
            if (json.has("aimPitchOffset")) {
                setAimPitchOffsetSpec(json.get("aimPitchOffset").getAsString());
            }
            if (json.has("aimPitchOffsetSpec")) {
                setAimPitchOffsetSpec(json.get("aimPitchOffsetSpec").getAsString());
            }
            boolean migratedHuntVerticalDefaults = false;
            if (json.has("huntMode")) {
                huntMode = json.get("huntMode").getAsString();
            } else if (json.has("huntEnabled")) {
                huntMode = json.get("huntEnabled").getAsBoolean() ? HUNT_MODE_APPROACH : HUNT_MODE_OFF;
            }
            boolean hasHuntFixedDistance = json.has("huntFixedDistance");
            if (json.has("huntPickupItemsEnabled")) {
                huntPickupItemsEnabled = json.get("huntPickupItemsEnabled").getAsBoolean();
            }
            if (json.has("visualizeHuntRadius")) {
                visualizeHuntRadius = json.get("visualizeHuntRadius").getAsBoolean();
            }
            if (json.has("huntRadius")) {
                huntRadius = json.get("huntRadius").getAsFloat();
            }
            if (hasHuntFixedDistance) {
                huntFixedDistance = json.get("huntFixedDistance").getAsFloat();
            }
            if (json.has("huntUpRange") && !json.get("huntUpRange").isJsonNull()) {
                huntUpRange = json.get("huntUpRange").getAsFloat();
            } else {
                migratedHuntVerticalDefaults = true;
            }
            if (json.has("huntDownRange") && !json.get("huntDownRange").isJsonNull()) {
                huntDownRange = json.get("huntDownRange").getAsFloat();
            } else {
                migratedHuntVerticalDefaults = true;
            }
            if (json.has("huntOrbitEnabled")) {
                huntOrbitEnabled = json.get("huntOrbitEnabled").getAsBoolean();
            }
            if (json.has("huntJumpOrbitEnabled")) {
                huntJumpOrbitEnabled = json.get("huntJumpOrbitEnabled").getAsBoolean();
            }
            if (json.has("huntOrbitSamplePoints")) {
                huntOrbitSamplePoints = json.get("huntOrbitSamplePoints").getAsInt();
            }
            if (json.has("enableNameWhitelist")) {
                enableNameWhitelist = json.get("enableNameWhitelist").getAsBoolean();
            }
            if (json.has("enableNameBlacklist")) {
                enableNameBlacklist = json.get("enableNameBlacklist").getAsBoolean();
            }
            if (json.has("nameWhitelist") && json.get("nameWhitelist").isJsonArray()) {
                List<String> loaded = GSON.fromJson(json.get("nameWhitelist"), STRING_LIST_TYPE);
                nameWhitelist = normalizeNameList(loaded);
            }
            if (json.has("nameBlacklist") && json.get("nameBlacklist").isJsonArray()) {
                List<String> loaded = GSON.fromJson(json.get("nameBlacklist"), STRING_LIST_TYPE);
                nameBlacklist = normalizeNameList(loaded);
            }
            if (json.has("nearbyEntityScanRange")) {
                nearbyEntityScanRange = json.get("nearbyEntityScanRange").getAsFloat();
            }
            if (json.has("presets") && json.get("presets").isJsonArray()) {
                List<KillAuraPreset> loadedPresets = GSON.fromJson(json.get("presets"), PRESET_LIST_TYPE);
                presets.clear();
                if (loadedPresets != null) {
                    for (KillAuraPreset preset : loadedPresets) {
                        KillAuraPreset normalizedPreset = normalizePreset(preset);
                        if (normalizedPreset != null) {
                            presets.add(normalizedPreset);
                        }
                    }
                }
            }

            if (json.has("attackRange")) {
                attackRange = json.get("attackRange").getAsFloat();
            }
            if (!hasHuntFixedDistance) {
                huntFixedDistance = attackRange;
            }
            if (json.has("minAttackStrength")) {
                minAttackStrength = json.get("minAttackStrength").getAsFloat();
            }
            if (json.has("minTurnSpeed")) {
                minTurnSpeed = json.get("minTurnSpeed").getAsFloat();
            }
            if (json.has("maxTurnSpeed")) {
                maxTurnSpeed = json.get("maxTurnSpeed").getAsFloat();
            }
            if (json.has("minAttackIntervalTicks")) {
                minAttackIntervalTicks = json.get("minAttackIntervalTicks").getAsInt();
            }
            if (json.has("targetsPerAttack")) {
                targetsPerAttack = json.get("targetsPerAttack").getAsInt();
            }
            if (json.has("noDamageAttackLimit")) {
                noDamageAttackLimit = json.get("noDamageAttackLimit").getAsInt();
            }

            normalizeConfig();
            if (migratedHuntVerticalDefaults) {
                saveConfig();
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.kill_aura.load_failed"), e);
        }
    }

    public static void saveConfig() {
        normalizeConfig();
        try {
            File configFile = getConfigFile();
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("rotateToTarget", rotateToTarget);
            json.addProperty("smoothRotation", smoothRotation);
            json.addProperty("smoothMaxTurnStep", smoothMaxTurnStepSpec);
            json.addProperty("smoothMaxTurnStepValue", smoothMaxTurnStep);
            json.addProperty("rotateOnlyOnAttack", rotateOnlyOnAttack);
            json.addProperty("relockOnlyWhenNoCrosshairTarget", relockOnlyWhenNoCrosshairTarget);
            json.addProperty("onlyAttackWhenLookingAtTarget", onlyAttackWhenLookingAtTarget);
            json.addProperty("requireLineOfSight", requireLineOfSight);
            json.addProperty("targetHostile", targetHostile);
            json.addProperty("targetPassive", targetPassive);
            json.addProperty("targetPlayers", targetPlayers);
            json.addProperty("onlyWeapon", onlyWeapon);
            json.addProperty("aimOnlyMode", aimOnlyMode);
            json.addProperty("focusSingleTarget", focusSingleTarget);
            json.addProperty("ignoreInvisible", ignoreInvisible);
            json.addProperty("enableNoCollision", enableNoCollision);
            json.addProperty("enableAntiKnockback", enableAntiKnockback);
            json.addProperty("enableFullBrightVision", enableFullBrightVision);
            json.addProperty("fullBrightGamma", fullBrightGamma);
            json.addProperty("attackMode", attackMode);
            json.addProperty("attackSequenceName", attackSequenceName);
            json.addProperty("attackSequenceDelayTicks", attackSequenceDelayTicksSpec);
            json.addProperty("attackSequenceDelayTicksValue", attackSequenceDelayTicks);
            json.addProperty("aimYawOffset", aimYawOffsetSpec);
            json.addProperty("aimYawOffsetValue", aimYawOffset);
            json.addProperty("aimYawOffsetSpec", aimYawOffsetSpec);
            json.addProperty("aimPitchOffset", aimPitchOffsetSpec);
            json.addProperty("aimPitchOffsetValue", aimPitchOffset);
            json.addProperty("aimPitchOffsetSpec", aimPitchOffsetSpec);
            json.addProperty("huntMode", huntMode);
            json.addProperty("huntEnabled", isHuntEnabled());
            json.addProperty("huntPickupItemsEnabled", huntPickupItemsEnabled);
            json.addProperty("visualizeHuntRadius", visualizeHuntRadius);
            json.addProperty("huntRadius", huntRadius);
            json.addProperty("huntFixedDistance", huntFixedDistance);
            json.addProperty("huntUpRange", huntUpRange);
            json.addProperty("huntDownRange", huntDownRange);
            json.addProperty("huntOrbitEnabled", huntOrbitEnabled);
            json.addProperty("huntJumpOrbitEnabled", huntJumpOrbitEnabled);
            json.addProperty("huntOrbitSamplePoints", huntOrbitSamplePoints);
            json.addProperty("enableNameWhitelist", enableNameWhitelist);
            json.addProperty("enableNameBlacklist", enableNameBlacklist);
            json.add("nameWhitelist", GSON.toJsonTree(normalizeNameList(nameWhitelist), STRING_LIST_TYPE));
            json.add("nameBlacklist", GSON.toJsonTree(normalizeNameList(nameBlacklist), STRING_LIST_TYPE));
            json.addProperty("nearbyEntityScanRange", nearbyEntityScanRange);
            json.add("presets", GSON.toJsonTree(getPresetSnapshots(), PRESET_LIST_TYPE));
            json.addProperty("attackRange", attackRange);
            json.addProperty("minAttackStrength", minAttackStrength);
            json.addProperty("minTurnSpeed", minTurnSpeed);
            json.addProperty("maxTurnSpeed", maxTurnSpeed);
            json.addProperty("minAttackIntervalTicks", minAttackIntervalTicks);
            json.addProperty("targetsPerAttack", targetsPerAttack);
            json.addProperty("noDamageAttackLimit", noDamageAttackLimit);

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.kill_aura.save_failed"), e);
        }
    }

    public void toggleEnabled() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean targetEnabled) {
        Minecraft mc = Minecraft.getMinecraft();
        normalizeConfig();
        if (enabled == targetEnabled) {
            saveConfig();
            return;
        }

        enabled = targetEnabled;
        resetRuntimeState();
        saveConfig();

        if (mc.player != null) {
            mc.player.sendMessage(
                    new TextComponentString(I18n.format(enabled ? "msg.kill_aura.enabled" : "msg.kill_aura.disabled")));
        }
    }

    public void onClientDisconnect() {
        resetRuntimeState();
    }

    public static boolean isHuntEnabled() {
        return !HUNT_MODE_OFF.equals(normalizeHuntModeValue(huntMode));
    }

    public static boolean isHuntApproachMode() {
        return HUNT_MODE_APPROACH.equals(normalizeHuntModeValue(huntMode));
    }

    public static boolean isHuntFixedDistanceMode() {
        return HUNT_MODE_FIXED_DISTANCE.equals(normalizeHuntModeValue(huntMode));
    }

    public static void setHuntMode(String mode) {
        huntMode = normalizeHuntModeValue(mode);
        huntEnabled = !HUNT_MODE_OFF.equals(huntMode);
        huntRadius = Math.max(huntRadius, attackRange);
        if (!huntEnabled) {
            visualizeHuntRadius = false;
        }
    }

    public static synchronized List<KillAuraPreset> getPresetSnapshots() {
        List<KillAuraPreset> snapshots = new ArrayList<>();
        for (KillAuraPreset preset : presets) {
            KillAuraPreset normalizedPreset = normalizePreset(preset);
            if (normalizedPreset != null) {
                snapshots.add(new KillAuraPreset(normalizedPreset));
            }
        }
        return snapshots;
    }

    public static synchronized boolean hasPreset(String name) {
        return findPresetIndex(name) >= 0;
    }

    public static synchronized boolean saveCurrentAsPreset(String name) {
        String normalizedName = normalizePresetName(name);
        if (normalizedName.isEmpty()) {
            return false;
        }
        KillAuraPreset preset = captureCurrentAsPreset(normalizedName);
        int existingIndex = findPresetIndex(normalizedName);
        if (existingIndex >= 0) {
            presets.set(existingIndex, preset);
        } else {
            presets.add(preset);
        }
        saveConfig();
        return true;
    }

    public static synchronized boolean overwritePreset(String name) {
        int index = findPresetIndex(name);
        if (index < 0) {
            return false;
        }
        presets.set(index, captureCurrentAsPreset(presets.get(index).name));
        saveConfig();
        return true;
    }

    public static synchronized boolean applyPresetByName(String name) {
        int index = findPresetIndex(name);
        if (index < 0) {
            return false;
        }
        applyPreset(presets.get(index));
        return true;
    }

    public static synchronized boolean renamePreset(String oldName, String newName) {
        int index = findPresetIndex(oldName);
        String normalizedNew = normalizePresetName(newName);
        if (index < 0 || normalizedNew.isEmpty()) {
            return false;
        }
        int duplicateIndex = findPresetIndex(normalizedNew);
        if (duplicateIndex >= 0 && duplicateIndex != index) {
            return false;
        }
        presets.get(index).name = normalizedNew;
        saveConfig();
        return true;
    }

    public static synchronized boolean deletePreset(String name) {
        int index = findPresetIndex(name);
        if (index < 0) {
            return false;
        }
        presets.remove(index);
        saveConfig();
        return true;
    }

    private static void applyPreset(KillAuraPreset preset) {
        KillAuraPreset safePreset = normalizePreset(preset);
        if (safePreset == null) {
            return;
        }
        rotateToTarget = safePreset.rotateToTarget;
        smoothRotation = safePreset.smoothRotation;
        setSmoothMaxTurnStepSpec(safePreset.smoothMaxTurnStepSpec);
        rotateOnlyOnAttack = safePreset.rotateOnlyOnAttack;
        relockOnlyWhenNoCrosshairTarget = safePreset.relockOnlyWhenNoCrosshairTarget;
        onlyAttackWhenLookingAtTarget = safePreset.onlyAttackWhenLookingAtTarget;
        requireLineOfSight = safePreset.requireLineOfSight;
        targetHostile = safePreset.targetHostile;
        targetPassive = safePreset.targetPassive;
        targetPlayers = safePreset.targetPlayers;
        onlyWeapon = safePreset.onlyWeapon;
        aimOnlyMode = safePreset.aimOnlyMode;
        focusSingleTarget = safePreset.focusSingleTarget;
        ignoreInvisible = safePreset.ignoreInvisible;
        enableNoCollision = safePreset.enableNoCollision;
        enableAntiKnockback = safePreset.enableAntiKnockback;
        enableFullBrightVision = safePreset.enableFullBrightVision;
        fullBrightGamma = safePreset.fullBrightGamma;
        attackMode = safePreset.attackMode;
        attackSequenceName = safePreset.attackSequenceName;
        setAttackSequenceDelayTicksSpec(safePreset.attackSequenceDelayTicksSpec);
        setAimYawOffsetSpec(safePreset.aimYawOffsetSpec);
        setAimPitchOffsetSpec(safePreset.aimPitchOffsetSpec);
        huntMode = safePreset.huntMode;
        huntPickupItemsEnabled = safePreset.huntPickupItemsEnabled;
        visualizeHuntRadius = safePreset.visualizeHuntRadius;
        huntRadius = safePreset.huntRadius;
        huntFixedDistance = safePreset.huntFixedDistance;
        huntUpRange = safePreset.huntUpRange;
        huntDownRange = safePreset.huntDownRange;
        huntOrbitEnabled = safePreset.huntOrbitEnabled;
        huntJumpOrbitEnabled = safePreset.huntJumpOrbitEnabled;
        huntOrbitSamplePoints = safePreset.huntOrbitSamplePoints;
        enableNameWhitelist = safePreset.enableNameWhitelist;
        enableNameBlacklist = safePreset.enableNameBlacklist;
        nameWhitelist = new ArrayList<>(safePreset.nameWhitelist);
        nameBlacklist = new ArrayList<>(safePreset.nameBlacklist);
        nearbyEntityScanRange = safePreset.nearbyEntityScanRange;
        attackRange = safePreset.attackRange;
        minAttackStrength = safePreset.minAttackStrength;
        minTurnSpeed = safePreset.minTurnSpeed;
        maxTurnSpeed = safePreset.maxTurnSpeed;
        minAttackIntervalTicks = safePreset.minAttackIntervalTicks;
        targetsPerAttack = safePreset.targetsPerAttack;
        noDamageAttackLimit = safePreset.noDamageAttackLimit;
        normalizeConfig();
        INSTANCE.resetRuntimeState();
        saveConfig();
    }

    public void resetRuntimeState() {
        stopHuntPickupNavigation();
        stopHuntNavigation();
        this.attackCooldownTicks = 0;
        this.sequenceCooldownTicks = 0;
        this.currentTargetEntityId = -1;
        clearAimTargetTransition();
        resetSmoothTurnLimitBudget();
        clearVisualRotationCache();
        this.huntNavigationActive = false;
        this.lastHuntGotoTick = -99999;
        this.lastHuntTargetEntityId = Integer.MIN_VALUE;
        this.lastHuntTargetX = 0.0D;
        this.lastHuntTargetZ = 0.0D;
        this.huntPickupNavigationActive = false;
        this.lastHuntPickupGotoTick = -99999;
        this.lastHuntPickupTargetEntityId = Integer.MIN_VALUE;
        this.lastHuntPickupSearchTick = -99999;
        this.lastHuntPickupSearchTargetEntityId = Integer.MIN_VALUE;
        this.lastHuntPickupSearchFound = false;
        this.lastOrbitProcessRequestTick = -99999;
        this.lastOrbitProcessTargetEntityId = Integer.MIN_VALUE;
        this.lastOrbitProcessRequestedRadius = Double.NaN;
        this.lastSafeMotionX = 0.0D;
        this.lastSafeMotionY = 0.0D;
        this.lastSafeMotionZ = 0.0D;
        this.activeTeleportAttackPlan = null;
        this.pendingTeleportReturnTicks = 0;
        this.lastTeleportCorrectionTick = Integer.MIN_VALUE;
        this.attackSequenceExecutor.stop();
        clearNoDamageAttackTracking();
        this.areaHuntControlTicks = 0;
    }

    public boolean hasActiveTarget(EntityPlayerSP player) {
        return getActiveTarget(player).isPresent();
    }

    public Optional<EntityLivingBase> getActiveTarget(EntityPlayerSP player) {
        if (!enabled || player == null || player.world == null || this.currentTargetEntityId == -1) {
            return Optional.empty();
        }

        Entity target = player.world.getEntityByID(this.currentTargetEntityId);
        double targetSearchRadius = getTargetSearchRadius();
        boolean useWhitelistPriority = enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty();
        if (target instanceof EntityLivingBase
                && isTrackableTarget(player, (EntityLivingBase) target, targetSearchRadius * targetSearchRadius,
                        useWhitelistPriority)) {
            return Optional.of((EntityLivingBase) target);
        }
        return Optional.empty();
    }

    public Optional<Rotation> getVisualTargetRotation(EntityPlayerSP player) {
        if (player == null || player.world == null || !shouldRotateToTarget() || this.currentTargetEntityId == -1) {
            return Optional.empty();
        }
        if (!shouldApplyContinuousRotation(false)) {
            return Optional.empty();
        }
        Entity target = player.world.getEntityByID(this.currentTargetEntityId);
        if (!(target instanceof EntityLivingBase)) {
            return Optional.empty();
        }
        EntityLivingBase livingTarget = (EntityLivingBase) target;
        if (!isValidTarget(player, livingTarget)) {
            return Optional.empty();
        }
        if (isRelockSuppressedByCrosshairTarget(player, livingTarget)) {
            return Optional.empty();
        }

        Rotation cachedRotation = getCachedVisualRotation(player, livingTarget);
        if (cachedRotation != null) {
            return Optional.of(cachedRotation);
        }

        Rotation nextRotation = computeNextAimRotation(player, livingTarget, false, false);
        if (nextRotation == null) {
            return Optional.empty();
        }
        cacheVisualRotation(player, livingTarget, nextRotation);
        return Optional.of(nextRotation);
    }

    public boolean tryAttackUsingCurrentConfigForHunt(EntityPlayerSP player, EntityLivingBase target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || player == null || target == null || mc.playerController == null) {
            return false;
        }
        if (aimOnlyMode || isSequenceAttackMode()) {
            return false;
        }
        if (onlyWeapon && getPreferredAttackHotbarSlot(player) < 0) {
            return false;
        }
        if ((isPacketAttackMode() || isTeleportAttackMode()) && player.connection == null) {
            return false;
        }
        if (player.getCooledAttackStrength(0.0F) < minAttackStrength) {
            return false;
        }
        if (!canAttackTargetBeforeRotation(player, target)) {
            return false;
        }

        this.currentTargetEntityId = target.getEntityId();
        updateAimTargetTransition(target);
        EntityLivingBase crosshairLockedTarget = isRelockSuppressedByCrosshairTarget(player, target) ? target : null;
        if (!prepareRotationForAttack(player, target, crosshairLockedTarget)) {
            decayTargetSwitchSmoothTicks();
            return false;
        }

        boolean teleportAttack = shouldUseTeleportAttack(player, target);
        if (!canAttackTarget(player, target, shouldRequireCrosshairHitForAttack(teleportAttack))) {
            decayTargetSwitchSmoothTicks();
            return false;
        }

        boolean attacked = false;
        if (teleportAttack) {
            attacked = performTeleportAttack(player, target);
        } else if (isMouseClickAttackMode()) {
            attacked = performMouseClickAttack(mc);
        } else if (isPacketAttackMode()) {
            player.connection.sendPacket(new CPacketUseEntity(target));
            attacked = true;
        } else {
            mc.playerController.attackEntity(player, target);
            attacked = true;
        }
        if (attacked && !isMouseClickAttackMode()) {
            player.swingArm(EnumHand.MAIN_HAND);
        }
        if (attacked) {
            recordNoDamageAttackAttempt(target);
        }
        decayTargetSwitchSmoothTicks();
        return attacked;
    }

    public boolean prepareCurrentConfigSequenceAttackForHunt(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null || !isSequenceAttackMode() || !hasConfiguredAttackSequence()) {
            return false;
        }
        if (!isValidTarget(player, target)) {
            return false;
        }
        this.currentTargetEntityId = target.getEntityId();
        updateAimTargetTransition(target);
        EntityLivingBase crosshairLockedTarget = isRelockSuppressedByCrosshairTarget(player, target) ? target : null;
        boolean prepared = prepareRotationForAttack(player, target, crosshairLockedTarget);
        decayTargetSwitchSmoothTicks();
        return prepared;
    }

    public static boolean isConfiguredSequenceAttackMode() {
        return ATTACK_MODE_SEQUENCE.equalsIgnoreCase(attackMode);
    }

    public static int sampleCurrentConfigAttackCooldownTicks() {
        return ATTACK_MODE_MOUSE_CLICK.equalsIgnoreCase(attackMode)
                ? sampleAttackSequenceDelayTicks()
                : minAttackIntervalTicks;
    }

    public static int sampleCurrentConfigSequenceDelayTicks() {
        return sampleAttackSequenceDelayTicks();
    }

    public AreaHuntTickResult tickAreaHunt(EntityPlayerSP player, AreaHuntOptions options) {
        this.areaHuntControlTicks = 2;
        ensureBaritonePacketListenerRegistered();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || player == null || mc.world == null) {
            return new AreaHuntTickResult(false, null, 0, this.attackSequenceExecutor.isRunning());
        }

        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
        }
        if (this.sequenceCooldownTicks > 0) {
            this.sequenceCooldownTicks--;
        }
        tickTeleportAttackRecovery(player);
        applyFullBright(enableFullBrightVision);

        if (player.isDead || player.getHealth() <= 0.0F || player.isSpectator()) {
            clearAreaHuntRuntimeState(true);
            return new AreaHuntTickResult(false, null, 0, false);
        }

        tickNoDamageAttackTrackers(player);

        boolean sequenceAttackMode = isSequenceAttackMode();
        if (!sequenceAttackMode && this.attackSequenceExecutor.isRunning()) {
            this.attackSequenceExecutor.stop();
        }

        List<EntityLivingBase> targets = findTargets(player, options);
        if (targets.isEmpty()) {
            clearAreaHuntRuntimeState(false);
            return new AreaHuntTickResult(false, null, 0, this.attackSequenceExecutor.isRunning());
        }

        EntityLivingBase primaryTarget = targets.get(0);
        if (!aimOnlyMode && !sequenceAttackMode && onlyWeapon && getPreferredAttackHotbarSlot(player) < 0) {
            return new AreaHuntTickResult(true, primaryTarget, 0, this.attackSequenceExecutor.isRunning());
        }

        updateAimTargetTransition(primaryTarget);
        boolean orbitFacingActive = shouldForceOrbitFacing(player, primaryTarget);
        boolean crosshairTargetLocked = isRelockSuppressedByCrosshairTarget(player, primaryTarget, options);

        if (shouldApplyContinuousRotation(orbitFacingActive) && !crosshairTargetLocked) {
            applyRotation(player, primaryTarget, orbitFacingActive);
        }

        if (shouldRunHuntMovement(player, primaryTarget)) {
            stopHuntPickupNavigation();
            handleHuntMovement(player, primaryTarget);
        } else {
            stopHuntPickupNavigation();
            stopHuntNavigation();
        }

        int attackedCount = 0;
        if (sequenceAttackMode) {
            this.attackSequenceExecutor.tick(player);
            if (canTriggerAttackSequence(player, primaryTarget, options)
                    && prepareRotationForAttack(player, primaryTarget, crosshairTargetLocked ? primaryTarget : null)
                    && triggerAttackSequence(player, primaryTarget)) {
                recordNoDamageAttackAttempt(primaryTarget);
                this.sequenceCooldownTicks = sampleAttackSequenceDelayTicks();
            }
            decayTargetSwitchSmoothTicks();
            return new AreaHuntTickResult(true, primaryTarget, 0, this.attackSequenceExecutor.isRunning());
        }

        if (aimOnlyMode) {
            decayTargetSwitchSmoothTicks();
            return new AreaHuntTickResult(true, primaryTarget, 0, false);
        }

        if (canStartAttack(player) && mc.playerController != null) {
            attackedCount = attackTargets(mc, player, targets, crosshairTargetLocked ? primaryTarget : null, options);
            if (attackedCount > 0) {
                if (!isMouseClickAttackMode()) {
                    player.swingArm(EnumHand.MAIN_HAND);
                }
                this.attackCooldownTicks = isMouseClickAttackMode()
                        ? sampleAttackSequenceDelayTicks()
                        : minAttackIntervalTicks;
                if (!isHuntOrbitEnabled()) {
                    stopHuntNavigation();
                }
            }
        }
        decayTargetSwitchSmoothTicks();
        return new AreaHuntTickResult(true, primaryTarget, attackedCount, this.attackSequenceExecutor.isRunning());
    }

    public void stopAreaHuntAction() {
        clearAreaHuntRuntimeState(true);
        this.areaHuntControlTicks = 0;
        clearNoDamageAttackTracking();
    }

    public boolean isAreaHuntSequenceRunning() {
        return this.attackSequenceExecutor.isRunning();
    }

    public void tickAreaHuntSequenceOnly(EntityPlayerSP player) {
        this.areaHuntControlTicks = 2;
        if (player != null && this.attackSequenceExecutor.isRunning()) {
            this.attackSequenceExecutor.tick(player);
        }
    }

    private void clearAreaHuntRuntimeState(boolean stopSequence) {
        this.currentTargetEntityId = -1;
        clearAimTargetTransition();
        stopHuntPickupNavigation();
        stopHuntNavigation();
        if (stopSequence) {
            this.attackSequenceExecutor.stop();
        }
    }

    private void ensureBaritonePacketListenerRegistered() {
        try {
            IBaritone primaryBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            IEventBus eventBus = primaryBaritone == null ? null : primaryBaritone.getGameEventHandler();
            if (eventBus == null || eventBus == this.registeredBaritoneEventBus) {
                return;
            }
            eventBus.registerEventListener(this);
            this.registeredBaritoneEventBus = eventBus;
        } catch (Throwable ignored) {
        }
    }

    private boolean isTeleportAttackRecoveryActive() {
        return this.activeTeleportAttackPlan != null && this.pendingTeleportReturnTicks > 0;
    }

    private void tickTeleportAttackRecovery(EntityPlayerSP player) {
        if (this.pendingTeleportReturnTicks > 0) {
            this.pendingTeleportReturnTicks--;
        }
        if (this.activeTeleportAttackPlan == null) {
            return;
        }
        if (player == null || player.connection == null) {
            clearTeleportAttackState();
            return;
        }

        if (isPlayerNearTeleportOrigin(player, this.activeTeleportAttackPlan)) {
            this.activeTeleportAttackPlan.returnCompleted = true;
            if (this.pendingTeleportReturnTicks <= 0) {
                clearTeleportAttackState();
            }
            return;
        }

        if (this.pendingTeleportReturnTicks > 0) {
            return;
        }

        if (this.activeTeleportAttackPlan.correctedByServer
                && this.activeTeleportAttackPlan.correctionCount < TELEPORT_ATTACK_MAX_CORRECTIONS
                && player.ticksExisted != this.lastTeleportCorrectionTick) {
            this.lastTeleportCorrectionTick = player.ticksExisted;
            sendTeleportReturnToOrigin(player, this.activeTeleportAttackPlan, player.posX, player.posY, player.posZ, true);
            this.pendingTeleportReturnTicks = TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS;
            return;
        }

        clearTeleportAttackState();
    }

    private void clearTeleportAttackState() {
        this.activeTeleportAttackPlan = null;
        this.pendingTeleportReturnTicks = 0;
        this.lastTeleportCorrectionTick = Integer.MIN_VALUE;
    }

    private boolean isPlayerNearTeleportOrigin(EntityPlayerSP player, TeleportAttackPlan plan) {
        return player != null && plan != null
                && player.getDistanceSq(plan.originX, plan.originY, plan.originZ) <= TELEPORT_ATTACK_ORIGIN_TOLERANCE_SQ;
    }

    private boolean isSamePosition(double leftX, double leftY, double leftZ, double rightX, double rightY, double rightZ) {
        double dx = leftX - rightX;
        double dy = leftY - rightY;
        double dz = leftZ - rightZ;
        return dx * dx + dy * dy + dz * dz <= TELEPORT_ATTACK_ORIGIN_TOLERANCE_SQ;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (event.player != mc.player || mc.player == null || mc.world == null) {
            return;
        }

        boolean fastAttackEnabled = FreecamHandler.INSTANCE.isFastAttackEnabled;
        boolean flyEnabled = FlyHandler.enabled;
        boolean areaHuntActive = this.areaHuntControlTicks > 0;
        boolean movementProtectionActive = enabled || areaHuntActive || fastAttackEnabled || flyEnabled;
        boolean useNoCollision = ((enabled || areaHuntActive) && enableNoCollision)
                || (fastAttackEnabled && FreecamHandler.enableNoCollision)
                || (flyEnabled && FlyHandler.enableNoCollision);
        boolean useAntiKnockback = ((enabled || areaHuntActive) && enableAntiKnockback)
                || (fastAttackEnabled && FreecamHandler.enableAntiKnockback)
                || (flyEnabled && FlyHandler.enableAntiKnockback);
        applyKillAuraOwnMovementProtection(mc.player, movementProtectionActive, useNoCollision, useAntiKnockback);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ensureBaritonePacketListenerRegistered();

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }

        if (this.areaHuntControlTicks > 0) {
            this.areaHuntControlTicks--;
            return;
        }

        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
        }
        if (this.sequenceCooldownTicks > 0) {
            this.sequenceCooldownTicks--;
        }
        tickTeleportAttackRecovery(player);

        boolean fastAttackEnabled = FreecamHandler.INSTANCE.isFastAttackEnabled;
        boolean flyEnabled = FlyHandler.enabled;
        boolean movementProtectionActive = enabled || fastAttackEnabled || flyEnabled;
        boolean useNoCollision = (enabled && enableNoCollision)
                || (fastAttackEnabled && FreecamHandler.enableNoCollision)
                || (flyEnabled && FlyHandler.enableNoCollision);
        boolean useAntiKnockback = (enabled && enableAntiKnockback)
                || (fastAttackEnabled && FreecamHandler.enableAntiKnockback)
                || (flyEnabled && FlyHandler.enableAntiKnockback);
        applyKillAuraOwnMovementProtection(player, movementProtectionActive, useNoCollision, useAntiKnockback);
        applyFullBright(enableFullBrightVision);

        if (!enabled) {
            this.attackCooldownTicks = 0;
            this.sequenceCooldownTicks = 0;
            this.currentTargetEntityId = -1;
            clearAimTargetTransition();
            stopHuntPickupNavigation();
            if (!PathSequenceEventListener.isAnyHuntOrbitActionRunning()) {
                stopHuntNavigation();
            }
            this.attackSequenceExecutor.stop();
            if (!movementProtectionActive) {
                this.lastSafeMotionX = 0.0D;
                this.lastSafeMotionY = 0.0D;
                this.lastSafeMotionZ = 0.0D;
            }
            clearNoDamageAttackTracking();
            return;
        }

        if (player.isDead || player.getHealth() <= 0.0F || player.isSpectator()) {
            this.currentTargetEntityId = -1;
            clearAimTargetTransition();
            stopHuntPickupNavigation();
            stopHuntNavigation();
            this.attackSequenceExecutor.stop();
            clearNoDamageAttackTracking();
            return;
        }

        tickNoDamageAttackTrackers(player);

        boolean sequenceAttackMode = isSequenceAttackMode();
        if (!sequenceAttackMode && this.attackSequenceExecutor.isRunning()) {
            this.attackSequenceExecutor.stop();
        }

        if (!aimOnlyMode && !sequenceAttackMode && onlyWeapon && getPreferredAttackHotbarSlot(player) < 0) {
            this.currentTargetEntityId = -1;
            clearAimTargetTransition();
            stopHuntPickupNavigation();
            stopHuntNavigation();
            return;
        }

        boolean autoPickupRulePriority = AutoPickupHandler.INSTANCE.shouldPrioritizeNavigation(player);
        boolean autoPickupRuleAreaActive = AutoPickupHandler.INSTANCE.isPlayerInsideEnabledRule(player);
        EntityItem huntPriorityPickupItem = (!autoPickupRuleAreaActive && isHuntEnabled() && huntPickupItemsEnabled)
                ? findHuntPriorityPickupItem(player)
                : null;

        List<EntityLivingBase> targets = findTargets(player);
        if (targets.isEmpty()) {
            this.currentTargetEntityId = -1;
            clearAimTargetTransition();
            this.attackSequenceExecutor.stop();
            if (autoPickupRulePriority) {
                stopHuntPickupNavigation();
                stopHuntNavigation();
                return;
            }
            if (huntPriorityPickupItem != null) {
                stopHuntNavigation();
                handleHuntPickupMovement(player, huntPriorityPickupItem);
                return;
            }
            stopHuntPickupNavigation();
            stopHuntNavigation();
            return;
        }

        EntityLivingBase primaryTarget = targets.get(0);
        updateAimTargetTransition(primaryTarget);
        boolean orbitFacingActive = shouldForceOrbitFacing(player, primaryTarget);
        boolean crosshairTargetLocked = isRelockSuppressedByCrosshairTarget(player, primaryTarget);

        if (shouldApplyContinuousRotation(orbitFacingActive) && !crosshairTargetLocked) {
            applyRotation(player, primaryTarget, orbitFacingActive);
        }

        if (autoPickupRulePriority) {
            stopHuntPickupNavigation();
            stopHuntNavigation();
        } else if (huntPriorityPickupItem != null) {
            stopHuntNavigation();
            handleHuntPickupMovement(player, huntPriorityPickupItem);
        } else if (shouldRunHuntMovement(player, primaryTarget)) {
            stopHuntPickupNavigation();
            handleHuntMovement(player, primaryTarget);
        } else {
            stopHuntPickupNavigation();
            stopHuntNavigation();
        }

        if (sequenceAttackMode) {
            this.attackSequenceExecutor.tick(player);
            if (canTriggerAttackSequence(player, primaryTarget)
                    && prepareRotationForAttack(player, primaryTarget, crosshairTargetLocked ? primaryTarget : null)
                    && triggerAttackSequence(player, primaryTarget)) {
                recordNoDamageAttackAttempt(primaryTarget);
                this.sequenceCooldownTicks = sampleAttackSequenceDelayTicks();
            }
            decayTargetSwitchSmoothTicks();
            return;
        }

        if (aimOnlyMode) {
            decayTargetSwitchSmoothTicks();
            return;
        }

        if (canStartAttack(player) && mc.playerController != null) {
            int attackedCount = attackTargets(mc, player, targets, crosshairTargetLocked ? primaryTarget : null);
            if (attackedCount > 0) {
                if (!isMouseClickAttackMode()) {
                    player.swingArm(EnumHand.MAIN_HAND);
                }
                this.attackCooldownTicks = isMouseClickAttackMode()
                        ? sampleAttackSequenceDelayTicks()
                        : minAttackIntervalTicks;
                if (!isHuntOrbitEnabled()) {
                    stopHuntNavigation();
                }
            }
        }
        decayTargetSwitchSmoothTicks();
    }

    @Override
    public void onReceivePacket(PacketEvent event) {
        if (event == null || event.getState() != EventState.PRE || !(event.getPacket() instanceof SPacketPlayerPosLook)) {
            return;
        }

        final TeleportAttackPlan plan = this.activeTeleportAttackPlan;
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayerSP player = mc == null ? null : mc.player;
        if (plan == null || this.pendingTeleportReturnTicks <= 0 || player == null) {
            return;
        }

        final double[] correctedPosition = resolveTeleportCorrectionPosition((SPacketPlayerPosLook) event.getPacket(), player);
        mc.addScheduledTask(() -> handleTeleportCorrection(correctedPosition));
    }

    private double[] resolveTeleportCorrectionPosition(SPacketPlayerPosLook packet, EntityPlayerSP player) {
        double correctedX = packet.getX();
        double correctedY = packet.getY();
        double correctedZ = packet.getZ();
        if (packet.getFlags().contains(SPacketPlayerPosLook.EnumFlags.X)) {
            correctedX += player.posX;
        }
        if (packet.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Y)) {
            correctedY += player.posY;
        }
        if (packet.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Z)) {
            correctedZ += player.posZ;
        }
        return new double[] { correctedX, correctedY, correctedZ };
    }

    private void handleTeleportCorrection(double[] correctedPosition) {
        if (correctedPosition == null || correctedPosition.length < 3) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.player;
        TeleportAttackPlan plan = this.activeTeleportAttackPlan;
        if (player == null || player.connection == null || plan == null) {
            return;
        }

        if (isSamePosition(correctedPosition[0], correctedPosition[1], correctedPosition[2],
                plan.originX, plan.originY, plan.originZ)) {
            plan.returnCompleted = true;
            clearTeleportAttackState();
            return;
        }

        if (plan.correctionCount >= TELEPORT_ATTACK_MAX_CORRECTIONS || player.ticksExisted == this.lastTeleportCorrectionTick) {
            return;
        }

        this.lastTeleportCorrectionTick = player.ticksExisted;
        sendTeleportReturnToOrigin(player, plan, correctedPosition[0], correctedPosition[1], correctedPosition[2], true);
        this.pendingTeleportReturnTicks = TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled && this.areaHuntControlTicks <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        Entity viewer = mc.getRenderViewEntity();
        if (player == null || viewer == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        double worldCenterX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double worldCenterY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + 0.05D;
        double worldCenterZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        if (isHuntEnabled() && visualizeHuntRadius) {
            drawHuntRadiusAura(worldCenterX, worldCenterY, worldCenterZ, viewerX, viewerY, viewerZ, huntRadius);
        }
        renderHuntOrbitLoop();
    }

    private void drawHuntRadiusAura(double worldCenterX, double worldCenterY, double worldCenterZ, double viewerX,
            double viewerY, double viewerZ, double radius) {
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
        buffer.pos(worldCenterX - viewerX, worldCenterY - viewerY, worldCenterZ - viewerZ)
                .color(0.15F, 0.75F, 1.0F, 0.10F).endVertex();
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double[] point = getClippedHuntPoint(worldCenterX, worldCenterZ, safeRadius, angle);
            buffer.pos(point[0] - viewerX, worldCenterY - viewerY, point[1] - viewerZ).color(0.15F, 0.75F, 1.0F, 0.02F)
                    .endVertex();
        }
        tessellator.draw();

        GlStateManager.glLineWidth(4.0F);
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double[] point = getClippedHuntPoint(worldCenterX, worldCenterZ, safeRadius, angle);
            buffer.pos(point[0] - viewerX, worldCenterY - viewerY, point[1] - viewerZ).color(1.0F, 1.0F, 0.0F, 1.0F)
                    .endVertex();
        }
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
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

    private List<EntityLivingBase> findTargets(EntityPlayerSP player) {
        return findTargets(player, null);
    }

    private List<EntityLivingBase> findTargets(EntityPlayerSP player, AreaHuntOptions areaOptions) {
        List<EntityLivingBase> targets = new ArrayList<>();
        EntityLivingBase lockedTarget = null;
        double targetSearchRadius = areaOptions == null ? getTargetSearchRadius() : Double.MAX_VALUE;
        double targetSearchRadiusSq = targetSearchRadius * targetSearchRadius;
        boolean useWhitelistPriority = areaOptions == null
                && enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty();
        boolean preferStableOrbitTarget = isHuntOrbitEnabled();
        int previousTargetEntityId = this.currentTargetEntityId;

        if (focusSingleTarget && this.currentTargetEntityId != -1) {
            Entity existing = player.world.getEntityByID(this.currentTargetEntityId);
            if (existing instanceof EntityLivingBase
                    && isTrackableTarget(player, (EntityLivingBase) existing, targetSearchRadiusSq, useWhitelistPriority,
                            areaOptions)) {
                lockedTarget = (EntityLivingBase) existing;
                targets.add(lockedTarget);
            }
        }

        List<TargetCandidate> nearbyTargets = new ArrayList<>();

        for (Entity entity : player.world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase candidate = (EntityLivingBase) entity;
            if (candidate == lockedTarget) {
                continue;
            }
            TargetCandidate targetCandidate = buildTargetCandidate(player, candidate, targetSearchRadiusSq,
                    useWhitelistPriority, candidate.getEntityId() == previousTargetEntityId,
                    shouldAllowHuntTrackingWithoutLineOfSight(), areaOptions);
            if (targetCandidate != null) {
                nearbyTargets.add(targetCandidate);
            }
        }

        nearbyTargets.sort((left, right) -> {
            int whitelistPriorityCompare = Integer.compare(left.whitelistPriority, right.whitelistPriority);
            if (whitelistPriorityCompare != 0) {
                return whitelistPriorityCompare;
            }
            if (preferStableOrbitTarget) {
                int continuityCompare = Integer.compare(left.currentTargetPriority, right.currentTargetPriority);
                if (continuityCompare != 0) {
                    return continuityCompare;
                }
                int yawCompare = Float.compare(left.yawDeltaAbs, right.yawDeltaAbs);
                if (yawCompare != 0) {
                    return yawCompare;
                }
            }
            int stickyDistanceCompare = compareCurrentTargetWithinDistanceHysteresis(left, right);
            if (stickyDistanceCompare != 0) {
                return stickyDistanceCompare;
            }
            int distanceCompare = Double.compare(left.distanceSq, right.distanceSq);
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return Integer.compare(left.entity.getEntityId(), right.entity.getEntityId());
        });

        for (TargetCandidate nearbyTarget : nearbyTargets) {
            targets.add(nearbyTarget.entity);
        }
        if (lockedTarget == null) {
            promoteCrosshairLockedTarget(player, targets, areaOptions);
        }
        int nextTargetEntityId = targets.isEmpty() ? -1 : targets.get(0).getEntityId();
        if (nextTargetEntityId != this.currentTargetEntityId) {
            clearVisualRotationCache();
        }
        this.currentTargetEntityId = nextTargetEntityId;
        return targets;
    }

    private int compareCurrentTargetWithinDistanceHysteresis(TargetCandidate left, TargetCandidate right) {
        if (left == null || right == null || left.currentTargetPriority == right.currentTargetPriority) {
            return 0;
        }
        TargetCandidate current = left.currentTargetPriority < right.currentTargetPriority ? left : right;
        TargetCandidate challenger = current == left ? right : left;
        if (current.distanceSq <= challenger.distanceSq + TARGET_SWITCH_DISTANCE_HYSTERESIS_SQ) {
            return current == left ? -1 : 1;
        }
        return 0;
    }

    private void promoteCrosshairLockedTarget(EntityPlayerSP player, List<EntityLivingBase> targets) {
        promoteCrosshairLockedTarget(player, targets, null);
    }

    private void promoteCrosshairLockedTarget(EntityPlayerSP player, List<EntityLivingBase> targets,
            AreaHuntOptions areaOptions) {
        EntityLivingBase crosshairTarget = getCrosshairLockedTarget(player, targets, areaOptions);
        if (crosshairTarget == null || targets == null || targets.isEmpty() || targets.get(0) == crosshairTarget) {
            return;
        }
        targets.remove(crosshairTarget);
        targets.add(0, crosshairTarget);
    }

    private EntityLivingBase getCrosshairLockedTarget(EntityPlayerSP player, List<EntityLivingBase> targets) {
        return getCrosshairLockedTarget(player, targets, null);
    }

    private EntityLivingBase getCrosshairLockedTarget(EntityPlayerSP player, List<EntityLivingBase> targets,
            AreaHuntOptions areaOptions) {
        if (!shouldUseRelockOnlyWhenNoCrosshairTarget() || player == null || targets == null || targets.isEmpty()) {
            return null;
        }

        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        double searchDistance = Math.max(0.1D, attackRange);
        Vec3d endPos = eyePos.addVector(lookVec.x * searchDistance, lookVec.y * searchDistance,
                lookVec.z * searchDistance);

        EntityLivingBase bestTarget = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (EntityLivingBase target : targets) {
            if (!canAttackTargetBeforeRotation(player, target, areaOptions)) {
                continue;
            }
            double hitDistanceSq = getCrosshairHitDistanceSq(player, target, eyePos, endPos);
            if (hitDistanceSq >= 0.0D && hitDistanceSq < bestDistanceSq) {
                bestDistanceSq = hitDistanceSq;
                bestTarget = target;
            }
        }
        return bestTarget;
    }

    private boolean isRelockSuppressedByCrosshairTarget(EntityPlayerSP player, EntityLivingBase target) {
        return isRelockSuppressedByCrosshairTarget(player, target, null);
    }

    private boolean isRelockSuppressedByCrosshairTarget(EntityPlayerSP player, EntityLivingBase target,
            AreaHuntOptions areaOptions) {
        return shouldUseRelockOnlyWhenNoCrosshairTarget()
                && target != null
                && canAttackTargetBeforeRotation(player, target, areaOptions)
                && getCrosshairHitDistanceSq(player, target) >= 0.0D;
    }

    private double getCrosshairHitDistanceSq(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null) {
            return -1.0D;
        }
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        double searchDistance = Math.max(0.1D, attackRange);
        Vec3d endPos = eyePos.addVector(lookVec.x * searchDistance, lookVec.y * searchDistance,
                lookVec.z * searchDistance);
        return getCrosshairHitDistanceSq(player, target, eyePos, endPos);
    }

    private double getCrosshairHitDistanceSq(EntityPlayerSP player, EntityLivingBase target, Vec3d eyePos,
            Vec3d endPos) {
        if (player == null || target == null || eyePos == null || endPos == null) {
            return -1.0D;
        }
        AxisAlignedBB boundingBox = target.getEntityBoundingBox();
        if (boundingBox == null) {
            return -1.0D;
        }
        double border = Math.max(0.0D, target.getCollisionBorderSize());
        AxisAlignedBB hitBox = border > 0.0D ? boundingBox.grow(border) : boundingBox;
        Vec3d hitVec;
        if (containsPoint(hitBox, eyePos)) {
            hitVec = eyePos;
        } else {
            RayTraceResult intercept = hitBox.calculateIntercept(eyePos, endPos);
            if (intercept == null || intercept.hitVec == null) {
                return -1.0D;
            }
            hitVec = intercept.hitVec;
        }
        if (requireLineOfSight && isCrosshairHitBlockedByWorld(eyePos, hitVec)) {
            return -1.0D;
        }
        return eyePos.squareDistanceTo(hitVec);
    }

    private boolean containsPoint(AxisAlignedBB box, Vec3d point) {
        return box != null
                && point != null
                && point.x >= box.minX
                && point.x <= box.maxX
                && point.y >= box.minY
                && point.y <= box.maxY
                && point.z >= box.minZ
                && point.z <= box.maxZ;
    }

    private boolean isCrosshairHitBlockedByWorld(Vec3d eyePos, Vec3d hitVec) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || eyePos == null || hitVec == null) {
            return false;
        }
        RayTraceResult blockRay = mc.world.rayTraceBlocks(eyePos, hitVec, false, true, false);
        return blockRay != null && blockRay.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    private boolean isValidTarget(EntityPlayerSP player, EntityLivingBase target) {
        return isValidTarget(player, target, null);
    }

    private boolean isValidTarget(EntityPlayerSP player, EntityLivingBase target, AreaHuntOptions areaOptions) {
        double targetSearchRadius = getTargetSearchRadius();
        return buildTargetCandidate(player, target,
                areaOptions == null ? targetSearchRadius * targetSearchRadius : Double.MAX_VALUE,
                areaOptions == null && enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty(),
                false, false, areaOptions) != null;
    }

    private boolean isTrackableTarget(EntityPlayerSP player, EntityLivingBase target, double targetSearchRadiusSq,
            boolean useWhitelistPriority) {
        return isTrackableTarget(player, target, targetSearchRadiusSq, useWhitelistPriority, null);
    }

    private boolean isTrackableTarget(EntityPlayerSP player, EntityLivingBase target, double targetSearchRadiusSq,
            boolean useWhitelistPriority, AreaHuntOptions areaOptions) {
        return buildTargetCandidate(player, target, targetSearchRadiusSq, useWhitelistPriority, false,
                shouldAllowHuntTrackingWithoutLineOfSight(), areaOptions) != null;
    }

    private TargetCandidate buildTargetCandidate(EntityPlayerSP player, EntityLivingBase target, double targetSearchRadiusSq,
            boolean useWhitelistPriority, boolean isCurrentTarget, boolean ignoreLineOfSightRequirement) {
        return buildTargetCandidate(player, target, targetSearchRadiusSq, useWhitelistPriority, isCurrentTarget,
                ignoreLineOfSightRequirement, null);
    }

    private TargetCandidate buildTargetCandidate(EntityPlayerSP player, EntityLivingBase target,
            double targetSearchRadiusSq, boolean useWhitelistPriority, boolean isCurrentTarget,
            boolean ignoreLineOfSightRequirement, AreaHuntOptions areaOptions) {
        if (player == null || target == null || target == player) {
            return null;
        }
        if (target.isDead || target.getHealth() <= 0.0F) {
            return null;
        }
        if (isNoDamageExcludedTarget(target)) {
            return null;
        }
        if (target instanceof EntityArmorStand) {
            return null;
        }
        if (ignoreInvisible && target.isInvisible()) {
            return null;
        }
        if (areaOptions != null && (!areaOptions.contains(target) || !areaOptions.allows(target))) {
            return null;
        }
        boolean whitelistMatched = areaOptions != null && areaOptions.shouldTreatAllowedTargetAsWhitelistMatched();
        if (areaOptions == null && isHuntEnabled() && !isWithinConfiguredHuntVerticalRange(player, target)) {
            return null;
        }
        double distanceSq = getTargetSearchDistanceSq(player, target);
        if (distanceSq > targetSearchRadiusSq) {
            return null;
        }
        if (AutoFollowHandler.hasActiveLockChaseRestriction()
                && !AutoFollowHandler.isPositionWithinActiveLockChaseBounds(target.posX, target.posZ)) {
            return null;
        }
        if (!ignoreLineOfSightRequirement && requireLineOfSight && !player.canEntityBeSeen(target)) {
            return null;
        }

        String targetName = getFilterableEntityName(target);
        int whitelistPriority = whitelistMatched ? 0 : Integer.MAX_VALUE;
        if (areaOptions == null) {
            if (enableNameBlacklist && matchesNameList(targetName, nameBlacklist)) {
                return null;
            }
            if (enableNameWhitelist) {
                whitelistPriority = getNormalizedNameListMatchIndex(targetName, nameWhitelist);
                if (whitelistPriority == Integer.MAX_VALUE) {
                    return null;
                }
                whitelistMatched = true;
            }
        }

        if (!whitelistMatched && !matchesEnabledTargetGroup(target)) {
            return null;
        }
        float yawDeltaAbs = Math.abs(MathHelper.wrapDegrees(getDesiredAimRotation(player, target).getYaw() - player.rotationYaw));
        return new TargetCandidate(target, distanceSq, useWhitelistPriority ? whitelistPriority : 0,
                isCurrentTarget ? 0 : 1, yawDeltaAbs);
    }

    private static boolean isWithinConfiguredHuntVerticalRange(EntityPlayerSP player, Entity target) {
        if (player == null || target == null) {
            return false;
        }
        double dy = target.posY - player.posY;
        return dy <= huntUpRange + 1.0E-6D && -dy <= huntDownRange + 1.0E-6D;
    }

    private double getTargetSearchDistanceSq(EntityPlayerSP player, Entity target) {
        if (player == null || target == null) {
            return Double.MAX_VALUE;
        }
        if (isHuntEnabled()) {
            double dx = player.posX - target.posX;
            double dz = player.posZ - target.posZ;
            return dx * dx + dz * dz;
        }
        return player.getDistanceSq(target);
    }

    private boolean shouldAllowHuntTrackingWithoutLineOfSight() {
        return isHuntEnabled();
    }

    private boolean canStartAttack(EntityPlayerSP player) {
        if (player == null) {
            return false;
        }
        if (aimOnlyMode) {
            return false;
        }
        if (isSequenceAttackMode()) {
            return false;
        }
        if (this.attackCooldownTicks > 0) {
            return false;
        }
        if (onlyWeapon && getPreferredAttackHotbarSlot(player) < 0) {
            return false;
        }
        return player.getCooledAttackStrength(0.0F) >= minAttackStrength;
    }

    private int attackTargets(Minecraft mc, EntityPlayerSP player, List<EntityLivingBase> targets,
            EntityLivingBase crosshairLockedTarget) {
        return attackTargets(mc, player, targets, crosshairLockedTarget, null);
    }

    private int attackTargets(Minecraft mc, EntityPlayerSP player, List<EntityLivingBase> targets,
            EntityLivingBase crosshairLockedTarget, AreaHuntOptions areaOptions) {
        if (mc == null || player == null || targets == null || targets.isEmpty()) {
            return 0;
        }

        int attackLimit = isMouseClickAttackMode() ? 1 : Math.max(1, targetsPerAttack);
        int candidateLimit = shouldAttackPrimaryTargetOnly(crosshairLockedTarget)
                ? 1
                : targets.size();
        int attackedCount = 0;
        for (int i = 0; i < candidateLimit; i++) {
            if (attackedCount >= attackLimit) {
                break;
            }
            EntityLivingBase target = targets.get(i);
            if (!canAttackTargetBeforeRotation(player, target, areaOptions)) {
                continue;
            }
            if (!prepareRotationForAttack(player, target, crosshairLockedTarget)) {
                continue;
            }
            boolean teleportAttack = shouldUseTeleportAttack(player, target);
            if (!canAttackTarget(player, target, shouldRequireCrosshairHitForAttack(teleportAttack), areaOptions)) {
                continue;
            }

            if (teleportAttack) {
                if (!performTeleportAttack(player, target)) {
                    continue;
                }
            } else if (isMouseClickAttackMode()) {
                if (!performMouseClickAttack(mc)) {
                    continue;
                }
            } else if (isPacketAttackMode()) {
                player.connection.sendPacket(new CPacketUseEntity(target));
            } else {
                mc.playerController.attackEntity(player, target);
            }
            recordNoDamageAttackAttempt(target);
            attackedCount++;
        }
        return attackedCount;
    }

    private boolean shouldAttackPrimaryTargetOnly(EntityLivingBase crosshairLockedTarget) {
        return focusSingleTarget
                || crosshairLockedTarget != null
                || rotateOnlyOnAttack
                || isTargetSwitchSmoothingActive();
    }

    private boolean canAttackTargetBeforeRotation(EntityPlayerSP player, EntityLivingBase target) {
        return canAttackTargetBeforeRotation(player, target, null);
    }

    private boolean canAttackTargetBeforeRotation(EntityPlayerSP player, EntityLivingBase target,
            AreaHuntOptions areaOptions) {
        if (target == null || target.isDead || target.getHealth() <= 0.0F) {
            return false;
        }
        if (!isValidTarget(player, target, areaOptions)) {
            return false;
        }
        if (requireLineOfSight && !player.canEntityBeSeen(target)) {
            return false;
        }
        if (player.getDistanceSq(target) > attackRange * attackRange) {
            return false;
        }
        return true;
    }

    private boolean canAttackTarget(EntityPlayerSP player, EntityLivingBase target, boolean requireCrosshairHit) {
        return canAttackTarget(player, target, requireCrosshairHit, null);
    }

    private boolean canAttackTarget(EntityPlayerSP player, EntityLivingBase target, boolean requireCrosshairHit,
            AreaHuntOptions areaOptions) {
        if (!canAttackTargetBeforeRotation(player, target, areaOptions)) {
            return false;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees(getDesiredAimRotation(player, target).getYaw() - player.rotationYaw));
        if (shouldRotateToTarget() && yawDiff > 100.0F) {
            return false;
        }
        if (requireCrosshairHit && !isViewRayHittingAttackableTarget(player, target, areaOptions)) {
            return false;
        }
        return true;
    }

    private boolean shouldRequireCrosshairHitForAttack(boolean teleportAttack) {
        if (teleportAttack) {
            return false;
        }
        if (isMouseClickAttackMode()) {
            return true;
        }
        return onlyAttackWhenLookingAtTarget && shouldRotateToTarget();
    }

    private boolean isViewRayHittingAttackableTarget(EntityPlayerSP player, EntityLivingBase target) {
        return isViewRayHittingAttackableTarget(player, target, null);
    }

    private boolean isViewRayHittingAttackableTarget(EntityPlayerSP player, EntityLivingBase target,
            AreaHuntOptions areaOptions) {
        return player != null
                && target != null
                && canAttackTargetBeforeRotation(player, target, areaOptions)
                && getCrosshairHitDistanceSq(player, target) >= 0.0D;
    }

    private boolean shouldUseTeleportAttack(EntityPlayerSP player, EntityLivingBase target) {
        return isTeleportAttackMode()
                && attackRange > TELEPORT_ATTACK_MIN_RANGE
                && player != null
                && target != null
                && !isTeleportAttackRecoveryActive()
                && player.getDistance(target) > TELEPORT_ATTACK_MIN_RANGE;
    }

    private static final class TargetCandidate {
        private final EntityLivingBase entity;
        private final double distanceSq;
        private final int whitelistPriority;
        private final int currentTargetPriority;
        private final float yawDeltaAbs;

        private TargetCandidate(EntityLivingBase entity, double distanceSq, int whitelistPriority,
                int currentTargetPriority, float yawDeltaAbs) {
            this.entity = entity;
            this.distanceSq = distanceSq;
            this.whitelistPriority = whitelistPriority;
            this.currentTargetPriority = currentTargetPriority;
            this.yawDeltaAbs = yawDeltaAbs;
        }
    }

    public boolean isNoDamageExcludedTarget(Entity target) {
        return isNoDamageExclusionEnabled()
                && target != null
                && noDamageExcludedEntityIds.contains(target.getEntityId());
    }

    public static boolean isNoDamageExclusionEnabled() {
        return getNoDamageAttackLimit() > 0;
    }

    public static int getNoDamageAttackLimit() {
        return MathHelper.clamp(noDamageAttackLimit, 0, MAX_NO_DAMAGE_ATTACK_LIMIT);
    }

    private void tickNoDamageAttackTrackers(EntityPlayerSP player) {
        if (!isNoDamageExclusionEnabled() || player == null || player.world == null) {
            clearNoDamageAttackTracking();
            return;
        }

        int limit = getNoDamageAttackLimit();
        List<Integer> trackerIds = new ArrayList<>(noDamageAttackTrackers.keySet());
        for (Integer entityId : trackerIds) {
            if (entityId == null) {
                continue;
            }
            Entity entity = player.world.getEntityByID(entityId);
            if (!(entity instanceof EntityLivingBase)) {
                noDamageAttackTrackers.remove(entityId);
                noDamageExcludedEntityIds.remove(entityId);
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isDead || !living.isEntityAlive() || living.getHealth() <= 0.0F) {
                noDamageAttackTrackers.remove(entityId);
                noDamageExcludedEntityIds.remove(entityId);
                continue;
            }
            NoDamageAttackTracker tracker = noDamageAttackTrackers.get(entityId);
            if (tracker != null) {
                updateNoDamageAttackTracker(entityId, living, tracker, limit);
            }
        }

        pruneNoDamageExcludedTargets(player);
    }

    private void updateNoDamageAttackTracker(int entityId, EntityLivingBase target,
            NoDamageAttackTracker tracker, int limit) {
        float currentHealth = target.getHealth();
        if (currentHealth + NO_DAMAGE_HEALTH_EPSILON < tracker.baselineHealth) {
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
        if (tracker.confirmedNoDamageAttempts >= limit) {
            excludeNoDamageTarget(entityId, target);
        }
    }

    private void recordNoDamageAttackAttempt(EntityLivingBase target) {
        if (!isNoDamageExclusionEnabled() || target == null || target.getHealth() <= 0.0F) {
            return;
        }
        int entityId = target.getEntityId();
        if (noDamageExcludedEntityIds.contains(entityId)) {
            return;
        }

        NoDamageAttackTracker tracker = noDamageAttackTrackers.get(entityId);
        float currentHealth = target.getHealth();
        if (tracker == null) {
            tracker = new NoDamageAttackTracker(currentHealth);
            noDamageAttackTrackers.put(entityId, tracker);
        } else if (currentHealth + NO_DAMAGE_HEALTH_EPSILON < tracker.baselineHealth) {
            tracker.baselineHealth = currentHealth;
            tracker.pendingAttempts = 0;
            tracker.observationTicks = 0;
            tracker.confirmedNoDamageAttempts = 0;
        }
        if (tracker.pendingAttempts <= 0) {
            tracker.baselineHealth = currentHealth;
            tracker.observationTicks = NO_DAMAGE_OBSERVATION_DELAY_TICKS;
        }
        tracker.pendingAttempts++;
        pruneNoDamageTrackingSize();
    }

    private void excludeNoDamageTarget(int entityId, EntityLivingBase target) {
        if (!noDamageExcludedEntityIds.add(entityId)) {
            return;
        }
        noDamageAttackTrackers.remove(entityId);
        pruneNoDamageTrackingSize();
        if (this.currentTargetEntityId == entityId) {
            this.currentTargetEntityId = -1;
            clearAimTargetTransition();
            clearVisualRotationCache();
            stopHuntNavigation();
            this.attackSequenceExecutor.stop();
        }
        String targetName = target == null ? "" : getFilterableEntityName(target);
        zszlScriptMod.LOGGER.info("杀戮光环无掉血排除目标: id={}, name={}, limit={}",
                entityId, targetName, getNoDamageAttackLimit());
    }

    private void pruneNoDamageExcludedTargets(EntityPlayerSP player) {
        if (player == null || player.world == null || noDamageExcludedEntityIds.isEmpty()) {
            return;
        }
        List<Integer> excludedIds = new ArrayList<>(noDamageExcludedEntityIds);
        for (Integer entityId : excludedIds) {
            if (entityId == null) {
                continue;
            }
            Entity entity = player.world.getEntityByID(entityId);
            if (!(entity instanceof EntityLivingBase)) {
                noDamageExcludedEntityIds.remove(entityId);
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isDead || !living.isEntityAlive() || living.getHealth() <= 0.0F) {
                noDamageExcludedEntityIds.remove(entityId);
            }
        }
    }

    private void pruneNoDamageTrackingSize() {
        while (noDamageAttackTrackers.size() > NO_DAMAGE_MAX_TRACKED_TARGETS) {
            Integer first = noDamageAttackTrackers.keySet().iterator().next();
            noDamageAttackTrackers.remove(first);
        }
        while (noDamageExcludedEntityIds.size() > NO_DAMAGE_MAX_EXCLUDED_TARGETS) {
            Integer first = noDamageExcludedEntityIds.iterator().next();
            noDamageExcludedEntityIds.remove(first);
        }
    }

    private void clearNoDamageAttackTracking() {
        noDamageAttackTrackers.clear();
        noDamageExcludedEntityIds.clear();
    }

    private boolean performTeleportAttack(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null || player.connection == null || isTeleportAttackRecoveryActive()) {
            return false;
        }

        boolean preserveCurrentLook = isRelockSuppressedByCrosshairTarget(player, target);
        TeleportAttackPlan plan = buildTeleportAttackPlan(player, target, preserveCurrentLook);
        if (plan == null) {
            return false;
        }

        sendTeleportWaypoints(player, plan.outboundWaypoints, plan.originOnGround);
        if (shouldRotateToTarget() && !preserveCurrentLook) {
            player.connection.sendPacket(new CPacketPlayer.PositionRotation(plan.assaultX, plan.assaultY, plan.assaultZ,
                    plan.attackYaw, plan.attackPitch, plan.originOnGround));
        } else {
            player.connection.sendPacket(new CPacketPlayer.Position(plan.assaultX, plan.assaultY, plan.assaultZ,
                    plan.originOnGround));
        }
        player.connection.sendPacket(new CPacketUseEntity(target));
        sendTeleportReturnToOrigin(player, plan, plan.assaultX, plan.assaultY, plan.assaultZ, false);
        this.activeTeleportAttackPlan = plan;
        this.pendingTeleportReturnTicks = TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS;
        return true;
    }

    private boolean performMouseClickAttack(Minecraft mc) {
        if (mc == null || mc.currentScreen != null) {
            return false;
        }

        int screenWidth = Math.max(1, mc.displayWidth);
        int screenHeight = Math.max(1, mc.displayHeight);
        ModUtils.simulateMouseClick(screenWidth / 2, screenHeight / 2, true, screenWidth, screenHeight);
        return true;
    }

    private TeleportAttackPlan buildTeleportAttackPlan(EntityPlayerSP player, EntityLivingBase target,
            boolean preserveCurrentLook) {
        if (player == null || target == null) {
            return null;
        }

        TeleportAssaultCandidate assaultCandidate = findBestTeleportAssaultCandidate(player, target);
        if (assaultCandidate == null) {
            return null;
        }

        List<Vec3d> outboundWaypoints = buildTeleportPathWaypoints(player,
                player.posX, player.posY, player.posZ,
                assaultCandidate.x, assaultCandidate.y, assaultCandidate.z);
        List<Vec3d> returnWaypoints = buildTeleportPathWaypoints(player,
                assaultCandidate.x, assaultCandidate.y, assaultCandidate.z,
                player.posX, player.posY, player.posZ);

        Rotation attackRotation = shouldRotateToTarget() && !preserveCurrentLook
                ? getAdvancedAimRotationFromPosition(player, target, assaultCandidate.x, assaultCandidate.y,
                        assaultCandidate.z, new Rotation(player.rotationYaw, player.rotationPitch), true)
                : new Rotation(player.rotationYaw, player.rotationPitch);
        attackRotation = applyMouseGcdCompensation(player, attackRotation, Float.MAX_VALUE, Float.MAX_VALUE);

        return new TeleportAttackPlan(player, target, assaultCandidate, outboundWaypoints, returnWaypoints,
                attackRotation.getYaw(), attackRotation.getPitch());
    }

    private TeleportAssaultCandidate findBestTeleportAssaultCandidate(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return null;
        }

        double preferredRadius = Math.max(1.8D, TELEPORT_ATTACK_REACH + target.width * 0.5D);
        double minRadius = Math.max(0.9D, preferredRadius - TELEPORT_ATTACK_MAX_RADIUS_ADJUST);
        double maxRadius = Math.max(preferredRadius, preferredRadius + TELEPORT_ATTACK_MAX_RADIUS_ADJUST);
        double preferredAngle = Math.atan2(player.posZ - target.posZ, player.posX - target.posX);
        TeleportAssaultCandidate best = null;

        for (int angleStep = 0; angleStep <= TELEPORT_ATTACK_SAFE_ANGLE_STEPS; angleStep++) {
            if (angleStep == 0) {
                best = findTeleportAssaultCandidateForAngle(player, target, preferredAngle, preferredRadius, minRadius,
                        maxRadius, best);
                continue;
            }

            double angleOffset = angleStep * TELEPORT_ATTACK_SAFE_ANGLE_STEP_RADIANS;
            best = findTeleportAssaultCandidateForAngle(player, target, wrapOrbitAngle(preferredAngle + angleOffset),
                    preferredRadius, minRadius, maxRadius, best);
            best = findTeleportAssaultCandidateForAngle(player, target, wrapOrbitAngle(preferredAngle - angleOffset),
                    preferredRadius, minRadius, maxRadius, best);
        }

        if (best != null) {
            return best;
        }

        double[] unsafeAssaultPos = computeUnsafeTeleportAttackPosition(player, target);
        if (unsafeAssaultPos == null) {
            return null;
        }
        return new TeleportAssaultCandidate(unsafeAssaultPos[0], unsafeAssaultPos[1], unsafeAssaultPos[2], false,
                Double.MAX_VALUE);
    }

    private TeleportAssaultCandidate findTeleportAssaultCandidateForAngle(EntityPlayerSP player, EntityLivingBase target,
            double angle, double preferredRadius, double minRadius, double maxRadius, TeleportAssaultCandidate currentBest) {
        int radiusSteps = Math.max(1,
                (int) Math.ceil((maxRadius - minRadius) / Math.max(0.1D, TELEPORT_ATTACK_SAFE_RADIUS_STEP)));
        TeleportAssaultCandidate best = currentBest;

        for (int radiusStep = 0; radiusStep <= radiusSteps; radiusStep++) {
            if (radiusStep == 0) {
                best = evaluateTeleportAssaultCandidate(player, target, angle, preferredRadius, preferredRadius, best);
                continue;
            }

            double largerRadius = Math.min(maxRadius, preferredRadius + radiusStep * TELEPORT_ATTACK_SAFE_RADIUS_STEP);
            best = evaluateTeleportAssaultCandidate(player, target, angle, largerRadius, preferredRadius, best);

            double smallerRadius = Math.max(minRadius, preferredRadius - radiusStep * TELEPORT_ATTACK_SAFE_RADIUS_STEP);
            if (smallerRadius < largerRadius - 1.0E-4D) {
                best = evaluateTeleportAssaultCandidate(player, target, angle, smallerRadius, preferredRadius, best);
            }
        }

        return best;
    }

    private TeleportAssaultCandidate evaluateTeleportAssaultCandidate(EntityPlayerSP player, EntityLivingBase target,
            double preferredAngle, double radius, double preferredRadius, TeleportAssaultCandidate currentBest) {
        double desiredX = target.posX + Math.cos(preferredAngle) * radius;
        double desiredZ = target.posZ + Math.sin(preferredAngle) * radius;
        double[] clippedDesired = clipHuntDestinationXZ(target.posX, target.posZ, desiredX, desiredZ);
        double[] safeAssaultPos = findSafeHuntNavigationDestination(player, clippedDesired[0], target.posY, clippedDesired[1]);
        if (safeAssaultPos == null) {
            return currentBest;
        }

        BlockPos standPos = new BlockPos(safeAssaultPos[0], safeAssaultPos[1], safeAssaultPos[2]);
        if (!hasHuntLineOfSightFromStandPos(standPos, target)) {
            return currentBest;
        }

        double attackDx = target.posX - safeAssaultPos[0];
        double attackDy = target.posY + target.getEyeHeight() * 0.85D - (safeAssaultPos[1] + player.getEyeHeight());
        double attackDz = target.posZ - safeAssaultPos[2];
        double attackDistance = Math.sqrt(attackDx * attackDx + attackDy * attackDy + attackDz * attackDz);
        double maxAttackDistance = Math.max(2.85D, TELEPORT_ATTACK_REACH + target.width * 0.8D + 0.55D);
        if (attackDistance > maxAttackDistance) {
            return currentBest;
        }

        double actualAngle = Math.atan2(safeAssaultPos[2] - target.posZ, safeAssaultPos[0] - target.posX);
        double desiredPenalty = centerDistSq(safeAssaultPos[0], safeAssaultPos[2], clippedDesired[0], clippedDesired[1]) * 3.0D;
        double anglePenalty = Math.abs(wrapOrbitAngle(actualAngle - preferredAngle)) * 6.0D;
        double radiusPenalty = Math.abs(Math.sqrt((safeAssaultPos[0] - target.posX) * (safeAssaultPos[0] - target.posX)
                + (safeAssaultPos[2] - target.posZ) * (safeAssaultPos[2] - target.posZ)) - preferredRadius) * 4.5D;
        double heightPenalty = Math.abs(safeAssaultPos[1] - player.posY) * 0.6D;
        double approachPenalty = player.getDistanceSq(safeAssaultPos[0], safeAssaultPos[1], safeAssaultPos[2]) * 0.04D;
        double score = desiredPenalty + anglePenalty + radiusPenalty + heightPenalty + approachPenalty;

        if (currentBest == null || score < currentBest.score) {
            return new TeleportAssaultCandidate(safeAssaultPos[0], safeAssaultPos[1], safeAssaultPos[2], true, score);
        }
        return currentBest;
    }

    private double[] computeUnsafeTeleportAttackPosition(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return null;
        }

        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= 0.001D) {
            return new double[] { player.posX, target.posY, player.posZ };
        }

        double reach = Math.max(1.8D, TELEPORT_ATTACK_REACH + target.width * 0.5D);
        double ratio = Math.max(0.0D, (horizontalDistance - reach) / horizontalDistance);
        double assaultX = player.posX + dx * ratio;
        double assaultZ = player.posZ + dz * ratio;
        double assaultY = target.posY;
        return new double[] { assaultX, assaultY, assaultZ };
    }

    private List<Vec3d> buildTeleportPathWaypoints(EntityPlayerSP player, double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ) {
        List<Vec3d> waypoints = new ArrayList<>();
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(distance / TELEPORT_ATTACK_STEP_DISTANCE));

        for (int i = 1; i < steps; i++) {
            double progress = i / (double) steps;
            double desiredX = fromX + dx * progress;
            double desiredY = fromY + dy * progress;
            double desiredZ = fromZ + dz * progress;
            addTeleportWaypoint(waypoints, findTeleportWaypoint(player, desiredX, desiredY, desiredZ));
        }
        return waypoints;
    }

    private Vec3d findTeleportWaypoint(EntityPlayerSP player, double desiredX, double desiredY, double desiredZ) {
        double[] safePoint = findSafeHuntNavigationDestination(player, desiredX, desiredY, desiredZ);
        if (safePoint != null) {
            return new Vec3d(safePoint[0], safePoint[1], safePoint[2]);
        }
        return new Vec3d(desiredX, desiredY, desiredZ);
    }

    private void addTeleportWaypoint(List<Vec3d> waypoints, Vec3d waypoint) {
        if (waypoint == null) {
            return;
        }
        if (waypoints.isEmpty()) {
            waypoints.add(waypoint);
            return;
        }
        Vec3d last = waypoints.get(waypoints.size() - 1);
        if (last.squareDistanceTo(waypoint) > TELEPORT_ATTACK_WAYPOINT_EPSILON_SQ) {
            waypoints.add(waypoint);
        }
    }

    private void sendTeleportWaypoints(EntityPlayerSP player, List<Vec3d> waypoints, boolean onGround) {
        if (player == null || player.connection == null || waypoints == null) {
            return;
        }
        for (Vec3d waypoint : waypoints) {
            if (waypoint == null) {
                continue;
            }
            player.connection.sendPacket(new CPacketPlayer.Position(waypoint.x, waypoint.y, waypoint.z, onGround));
        }
    }

    private void sendTeleportReturnToOrigin(EntityPlayerSP player, TeleportAttackPlan plan, double startX, double startY,
            double startZ, boolean correctionTriggered) {
        if (player == null || player.connection == null || plan == null) {
            return;
        }

        List<Vec3d> returnWaypoints = isSamePosition(startX, startY, startZ, plan.assaultX, plan.assaultY, plan.assaultZ)
                ? plan.returnWaypoints
                : buildTeleportPathWaypoints(player, startX, startY, startZ, plan.originX, plan.originY, plan.originZ);
        sendTeleportWaypoints(player, returnWaypoints, plan.originOnGround);
        player.connection.sendPacket(new CPacketPlayer.Position(plan.originX, plan.originY, plan.originZ, plan.originOnGround));
        player.connection.sendPacket(new CPacketPlayer.PositionRotation(plan.originX, plan.originY, plan.originZ,
                plan.originYaw, plan.originPitch, plan.originOnGround));
        player.connection.sendPacket(new CPacketPlayer.PositionRotation(plan.originX, plan.originY, plan.originZ,
                plan.originYaw, plan.originPitch, plan.originOnGround));
        if (correctionTriggered) {
            plan.correctedByServer = true;
            plan.correctionCount++;
        }
        plan.returnCompleted = false;
    }

    private void applyRotation(EntityPlayerSP player, EntityLivingBase target) {
        applyRotation(player, target, false);
    }

    private void applyRotation(EntityPlayerSP player, EntityLivingBase target, boolean forceSmoothRotation) {
        applyRotation(player, target, forceSmoothRotation, false);
    }

    private boolean prepareRotationForAttack(EntityPlayerSP player, EntityLivingBase target,
            EntityLivingBase crosshairLockedTarget) {
        if (crosshairLockedTarget != null) {
            return isSameEntity(target, crosshairLockedTarget);
        }
        boolean targetSwitchSmoothing = isTargetSwitchSmoothingActive();
        boolean rotateOnlyForAttack = shouldRotateOnlyOnAttack();
        if (!rotateOnlyForAttack && !targetSwitchSmoothing) {
            return true;
        }
        if (rotateOnlyForAttack && !targetSwitchSmoothing) {
            applyRotation(player, target, false, true);
        }
        return targetSwitchSmoothing ? isRotationReadyForSmoothAttack(player, target)
                : isRotationReadyForAttack(player, target);
    }

    private void applyRotation(EntityPlayerSP player, EntityLivingBase target, boolean forceSmoothRotation,
            boolean attackRotation) {
        Rotation nextRotation = computeNextAimRotation(player, target, forceSmoothRotation, attackRotation);
        if (nextRotation == null) {
            return;
        }

        player.rotationYaw = nextRotation.getYaw();
        player.rotationPitch = nextRotation.getPitch();
        player.rotationYawHead = nextRotation.getYaw();
        player.renderYawOffset = nextRotation.getYaw();
    }

    private Rotation computeNextAimRotation(EntityPlayerSP player, EntityLivingBase target, boolean forceSmoothRotation,
            boolean attackRotation) {
        if (player == null || target == null || isRelockSuppressedByCrosshairTarget(player, target)) {
            return null;
        }

        Rotation desiredAim = getDesiredAimRotation(player, target, attackRotation);
        boolean useSmoothRotation = forceSmoothRotation || smoothRotation;
        if (!useSmoothRotation) {
            return applyMouseGcdCompensation(player, desiredAim, Float.MAX_VALUE, Float.MAX_VALUE);
        }

        desiredAim = applyAdvancedOvershootCorrection(player, target, desiredAim, attackRotation);
        float yawDelta = MathHelper.wrapDegrees(desiredAim.getYaw() - player.rotationYaw);
        float pitchDelta = desiredAim.getPitch() - player.rotationPitch;
        float yawSpeed = Math.max(computeTurnSpeed(Math.abs(yawDelta), attackRotation),
                computeTrackingYawSpeedFloor(player, target) * (attackRotation ? 0.85F : 0.65F));
        float pitchSpeed = Math.max(0.6F, yawSpeed * 0.62F);

        float yawStep = clampSigned(yawDelta, yawSpeed);
        float pitchStep = clampSigned(pitchDelta, pitchSpeed);
        updateSmoothTurnLimitBudget(player);
        float turnLimit = sampleSmoothMaxTurnStepForTurn();
        float remainingYawTurn = getRemainingSmoothYawTurnStep(turnLimit);
        float remainingPitchTurn = getRemainingSmoothPitchTurnStep(turnLimit);
        yawStep = clampSigned(yawStep, remainingYawTurn);
        pitchStep = clampSigned(pitchStep, remainingPitchTurn);
        float gcdStep = getMouseGcdStep();
        yawStep = quantizeRotationStepForGcd(yawStep, remainingYawTurn, gcdStep);
        pitchStep = quantizeRotationStepForGcd(pitchStep, remainingPitchTurn, gcdStep);
        smoothYawTurnUsedThisTick += Math.abs(yawStep);
        smoothPitchTurnUsedThisTick += Math.abs(pitchStep);

        float nextYaw = player.rotationYaw + yawStep;
        float nextPitch = player.rotationPitch + pitchStep;
        return new Rotation(nextYaw, MathHelper.clamp(nextPitch, -90.0F, 90.0F));
    }

    private void updateSmoothTurnLimitBudget(EntityPlayerSP player) {
        int currentTick = player == null ? Integer.MIN_VALUE : player.ticksExisted;
        if (currentTick != this.lastSmoothTurnLimitTick) {
            this.lastSmoothTurnLimitTick = currentTick;
            this.smoothYawTurnUsedThisTick = 0.0F;
            this.smoothPitchTurnUsedThisTick = 0.0F;
        }
    }

    private void resetSmoothTurnLimitBudget() {
        this.lastSmoothTurnLimitTick = Integer.MIN_VALUE;
        this.smoothYawTurnUsedThisTick = 0.0F;
        this.smoothPitchTurnUsedThisTick = 0.0F;
        clearVisualRotationCache();
    }

    private Rotation getCachedVisualRotation(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null || this.visualRotationCache == null) {
            return null;
        }
        if (this.visualRotationCacheTick != player.ticksExisted
                || this.visualRotationCacheTargetEntityId != target.getEntityId()
                || Float.compare(this.visualRotationCacheSourceYaw, player.rotationYaw) != 0
                || Float.compare(this.visualRotationCacheSourcePitch, player.rotationPitch) != 0) {
            return null;
        }
        return this.visualRotationCache;
    }

    private void cacheVisualRotation(EntityPlayerSP player, EntityLivingBase target, Rotation rotation) {
        if (player == null || target == null || rotation == null) {
            clearVisualRotationCache();
            return;
        }
        this.visualRotationCacheTick = player.ticksExisted;
        this.visualRotationCacheTargetEntityId = target.getEntityId();
        this.visualRotationCacheSourceYaw = player.rotationYaw;
        this.visualRotationCacheSourcePitch = player.rotationPitch;
        this.visualRotationCache = rotation;
    }

    private void clearVisualRotationCache() {
        this.visualRotationCacheTick = Integer.MIN_VALUE;
        this.visualRotationCacheTargetEntityId = Integer.MIN_VALUE;
        this.visualRotationCacheSourceYaw = 0.0F;
        this.visualRotationCacheSourcePitch = 0.0F;
        this.visualRotationCache = null;
    }

    private float getRemainingSmoothYawTurnStep(float turnLimit) {
        return Math.max(0.0F, turnLimit - this.smoothYawTurnUsedThisTick);
    }

    private float getRemainingSmoothPitchTurnStep(float turnLimit) {
        return Math.max(0.0F, turnLimit - this.smoothPitchTurnUsedThisTick);
    }

    private Rotation applyMouseGcdCompensation(EntityPlayerSP player, Rotation desiredRotation,
            float maxYawStep, float maxPitchStep) {
        if (player == null || desiredRotation == null) {
            return desiredRotation;
        }
        float yawStep = MathHelper.wrapDegrees(desiredRotation.getYaw() - player.rotationYaw);
        float pitchStep = desiredRotation.getPitch() - player.rotationPitch;
        float gcdStep = getMouseGcdStep();
        yawStep = quantizeRotationStepForGcd(yawStep, maxYawStep, gcdStep);
        pitchStep = quantizeRotationStepForGcd(pitchStep, maxPitchStep, gcdStep);
        return new Rotation(player.rotationYaw + yawStep,
                MathHelper.clamp(player.rotationPitch + pitchStep, -90.0F, 90.0F));
    }

    private float getMouseGcdStep() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return 0.0F;
        }
        float sensitivity = MathHelper.clamp(mc.gameSettings.mouseSensitivity, 0.0F, 1.0F);
        float f = sensitivity * 0.6F + 0.2F;
        return f * f * f * 8.0F * 0.15F;
    }

    private float quantizeRotationStepForGcd(float step, float maxMagnitude, float gcdStep) {
        if (step == 0.0F || gcdStep <= 1.0E-5F || maxMagnitude <= 0.0F) {
            return step == 0.0F ? 0.0F : clampSigned(step, maxMagnitude);
        }
        float maxStep = Math.abs(maxMagnitude);
        float absStep = Math.min(Math.abs(step), maxStep);
        if (absStep <= 1.0E-5F) {
            return 0.0F;
        }

        // When the user's smooth cap is below one mouse quantum, preserving the cap matters more than quantizing.
        if (maxStep + 1.0E-5F < gcdStep) {
            return Math.copySign(absStep, step);
        }

        float quantized = Math.round(absStep / gcdStep) * gcdStep;
        if (quantized <= 1.0E-5F && absStep >= gcdStep * 0.45F) {
            quantized = gcdStep;
        }
        if (quantized > maxStep) {
            quantized = (float) Math.floor(maxStep / gcdStep) * gcdStep;
        }
        if (quantized <= 1.0E-5F) {
            return 0.0F;
        }
        return Math.copySign(Math.min(quantized, maxStep), step);
    }

    private boolean isRotationReadyForAttack(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return false;
        }
        Rotation desiredAim = getDesiredAimRotation(player, target);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(desiredAim.getYaw() - player.rotationYaw));
        float pitchDiff = Math.abs(desiredAim.getPitch() - player.rotationPitch);
        return yawDiff <= 100.0F && pitchDiff <= 80.0F;
    }

    private boolean isRotationReadyForSmoothAttack(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return false;
        }
        Rotation desiredAim = getDesiredAimRotation(player, target);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(desiredAim.getYaw() - player.rotationYaw));
        float pitchDiff = Math.abs(desiredAim.getPitch() - player.rotationPitch);
        return yawDiff <= SMOOTH_ATTACK_READY_YAW_DEGREES && pitchDiff <= SMOOTH_ATTACK_READY_PITCH_DEGREES;
    }

    private boolean shouldApplyContinuousRotation(boolean orbitFacingActive) {
        if (!shouldRotateToTarget() && !orbitFacingActive) {
            return false;
        }
        return !rotateOnlyOnAttack || aimOnlyMode || isTargetSwitchSmoothingActive();
    }

    private void updateAimTargetTransition(EntityLivingBase target) {
        if (target == null) {
            clearAimTargetTransition();
            return;
        }

        int targetEntityId = target.getEntityId();
        if (targetEntityId == this.lastAimTargetEntityId) {
            return;
        }

        boolean hadPreviousTarget = this.lastAimTargetEntityId != Integer.MIN_VALUE && this.lastAimTargetEntityId != -1;
        this.lastAimTargetEntityId = targetEntityId;
        if (hadPreviousTarget && smoothRotation && shouldRotateToTarget()) {
            this.targetSwitchSmoothTicks = TARGET_SWITCH_SMOOTH_TICKS;
        } else {
            this.targetSwitchSmoothTicks = 0;
        }
    }

    private boolean isTargetSwitchSmoothingActive() {
        return this.targetSwitchSmoothTicks > 0 && smoothRotation && shouldRotateToTarget();
    }

    private void decayTargetSwitchSmoothTicks() {
        if (this.targetSwitchSmoothTicks > 0) {
            this.targetSwitchSmoothTicks--;
        }
    }

    private void clearAimTargetTransition() {
        this.lastAimTargetEntityId = Integer.MIN_VALUE;
        this.targetSwitchSmoothTicks = 0;
        clearVisualRotationCache();
    }

    private boolean shouldForceOrbitFacing(EntityPlayerSP player, EntityLivingBase target) {
        return isHuntOrbitEnabled() && canStartOrbitHunt(player, target);
    }

    private float computeTrackingYawSpeedFloor(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return minTurnSpeed;
        }

        double radiusX = target.posX - player.posX;
        double radiusZ = target.posZ - player.posZ;
        double horizontalDistance = Math.sqrt(radiusX * radiusX + radiusZ * radiusZ);
        if (horizontalDistance <= 1.0E-4D) {
            return minTurnSpeed;
        }

        double playerDeltaX = player.posX - player.lastTickPosX;
        double playerDeltaZ = player.posZ - player.lastTickPosZ;
        double targetDeltaX = target.posX - target.lastTickPosX;
        double targetDeltaZ = target.posZ - target.lastTickPosZ;
        double relativeDeltaX = targetDeltaX - playerDeltaX;
        double relativeDeltaZ = targetDeltaZ - playerDeltaZ;

        double tangentX = -radiusZ / horizontalDistance;
        double tangentZ = radiusX / horizontalDistance;
        double tangentialSpeed = Math.abs(relativeDeltaX * tangentX + relativeDeltaZ * tangentZ);
        double angularVelocityDeg = Math.toDegrees(Math.atan2(tangentialSpeed, horizontalDistance));
        double speedFloor = angularVelocityDeg * 1.18D + 1.35D;

        if (isHuntOrbitEnabled() && canStartOrbitHunt(player, target)) {
            speedFloor += 2.25D;
        }
        if (SpeedHandler.enabled) {
            speedFloor += Math.max(0.0D, (SpeedHandler.getCurrentTimerSpeedMultiplier() - 1.0F) * 8.0D);
        }

        return MathHelper.clamp((float) speedFloor, minTurnSpeed, Math.max(maxTurnSpeed, 60.0F));
    }

    private Rotation getDesiredAimRotation(EntityPlayerSP player, EntityLivingBase target) {
        return getDesiredAimRotation(player, target, false);
    }

    private Rotation getDesiredAimRotation(EntityPlayerSP player, EntityLivingBase target, boolean attackRotation) {
        if (player == null || target == null) {
            return new Rotation(0.0F, 0.0F);
        }
        return getAdvancedAimRotation(player, target, attackRotation);
    }

    private Rotation getAdvancedAimRotation(EntityPlayerSP player, EntityLivingBase target, boolean attackRotation) {
        float partialTicks = getCurrentAimPartialTicks();
        Vec3d eyePos = player.getPositionEyes(partialTicks);
        return getAdvancedAimRotationFromEye(player, target, eyePos, true,
                new Rotation(player.rotationYaw, player.rotationPitch), attackRotation);
    }

    private Rotation getAdvancedAimRotationFromPosition(EntityPlayerSP player, EntityLivingBase target,
            double fromX, double fromY, double fromZ, Rotation currentRotation, boolean attackRotation) {
        double eyeHeight = player == null ? 1.62D : player.getEyeHeight();
        return getAdvancedAimRotationFromEye(player, target, new Vec3d(fromX, fromY + eyeHeight, fromZ), false,
                currentRotation == null ? new Rotation(0.0F, 0.0F) : currentRotation, attackRotation);
    }

    private Rotation getAdvancedAimRotationFromEye(EntityPlayerSP player, EntityLivingBase target, Vec3d baseEyePos,
            boolean predictEye, Rotation currentRotation) {
        return getAdvancedAimRotationFromEye(player, target, baseEyePos, predictEye, currentRotation, false);
    }

    private Rotation getAdvancedAimRotationFromEye(EntityPlayerSP player, EntityLivingBase target, Vec3d baseEyePos,
            boolean predictEye, Rotation currentRotation, boolean attackRotation) {
        if (target == null || baseEyePos == null) {
            return currentRotation == null ? new Rotation(0.0F, 0.0F) : currentRotation;
        }

        float partialTicks = getCurrentAimPartialTicks();
        double targetX = interpolateAimCoordinate(target.lastTickPosX, target.posX, partialTicks);
        double targetY = interpolateAimCoordinate(target.lastTickPosY, target.posY, partialTicks)
                + getAdvancedAimHeightOffset(player, target, baseEyePos, attackRotation);
        double targetZ = interpolateAimCoordinate(target.lastTickPosZ, target.posZ, partialTicks);

        double leadTicks = computeAdvancedAimLeadTicks(player, target, baseEyePos, targetX, targetY, targetZ,
                attackRotation);
        double targetMotionX = clampPredictionMotion(target.posX - target.lastTickPosX);
        double targetMotionY = clampPredictionMotion(target.posY - target.lastTickPosY);
        double targetMotionZ = clampPredictionMotion(target.posZ - target.lastTickPosZ);
        targetX += targetMotionX * leadTicks;
        targetY += targetMotionY * leadTicks * 0.7D;
        targetZ += targetMotionZ * leadTicks;

        Vec3d eyePos = baseEyePos;
        if (predictEye && player != null) {
            double playerLead = Math.min(ADVANCED_AIM_MAX_PLAYER_LEAD_TICKS, leadTicks * 0.45D);
            eyePos = eyePos.addVector(
                    clampPredictionMotion(player.posX - player.lastTickPosX) * playerLead,
                    clampPredictionMotion(player.posY - player.lastTickPosY) * playerLead * 0.45D,
                    clampPredictionMotion(player.posZ - player.lastTickPosZ) * playerLead);
        }

        Rotation desired = RotationUtils.calcRotationFromVec3d(eyePos, new Vec3d(targetX, targetY, targetZ),
                currentRotation == null ? new Rotation(0.0F, 0.0F) : currentRotation);
        AimOffsetSample offset = sampleAimOffsets(player, target);
        return new Rotation(MathHelper.wrapDegrees(desired.getYaw() + offset.yaw),
                MathHelper.clamp(desired.getPitch() + offset.pitch, -90.0F, 90.0F));
    }

    private double getAdvancedAimHeightOffset(EntityPlayerSP player, EntityLivingBase target, Vec3d eyePos,
            boolean attackRotation) {
        double distance = 4.0D;
        if (target != null && eyePos != null) {
            double dx = target.posX - eyePos.x;
            double dy = target.posY - eyePos.y;
            double dz = target.posZ - eyePos.z;
            distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        double factor = attackRotation ? 0.80D : 0.84D;
        if (distance < 3.0D) {
            factor -= (3.0D - distance) * 0.035D;
        }
        if (target != null && target.height > 2.35F) {
            factor -= 0.08D;
        }
        double eyeHeight = target == null ? 1.5D : target.getEyeHeight();
        double maxOffset = target == null ? eyeHeight : Math.max(0.32D, target.height - 0.08D);
        return MathHelper.clamp(eyeHeight * factor, 0.28D, maxOffset);
    }

    private double computeAdvancedAimLeadTicks(EntityPlayerSP player, EntityLivingBase target, Vec3d eyePos,
            double targetX, double targetY, double targetZ, boolean attackRotation) {
        if (target == null || eyePos == null) {
            return ADVANCED_AIM_MIN_LEAD_TICKS;
        }
        double dx = targetX - eyePos.x;
        double dy = targetY - eyePos.y;
        double dz = targetZ - eyePos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double targetMotionX = target.posX - target.lastTickPosX;
        double targetMotionZ = target.posZ - target.lastTickPosZ;
        double playerMotionX = player == null ? 0.0D : player.posX - player.lastTickPosX;
        double playerMotionZ = player == null ? 0.0D : player.posZ - player.lastTickPosZ;
        double relativeSpeed = Math.sqrt((targetMotionX - playerMotionX) * (targetMotionX - playerMotionX)
                + (targetMotionZ - playerMotionZ) * (targetMotionZ - playerMotionZ));

        double lead = 0.30D
                + Math.min(1.05D, distance / 9.0D)
                + Math.min(0.80D, relativeSpeed * 2.45D);
        if (attackRotation) {
            lead += 0.18D;
        }
        if (SpeedHandler.enabled) {
            lead += MathHelper.clamp((double) (SpeedHandler.getCurrentTimerSpeedMultiplier() - 1.0F) * 0.65D,
                    0.0D, 0.75D);
        }
        if (distance < 2.4D) {
            lead *= 0.58D;
        }
        return MathHelper.clamp(lead, ADVANCED_AIM_MIN_LEAD_TICKS, ADVANCED_AIM_MAX_LEAD_TICKS);
    }

    private double clampPredictionMotion(double motion) {
        return MathHelper.clamp(motion, -1.15D, 1.15D);
    }

    private Rotation applyAdvancedOvershootCorrection(EntityPlayerSP player, EntityLivingBase target,
            Rotation desiredAim, boolean attackRotation) {
        if (player == null || target == null || desiredAim == null) {
            return desiredAim;
        }
        float yawDelta = MathHelper.wrapDegrees(desiredAim.getYaw() - player.rotationYaw);
        float pitchDelta = desiredAim.getPitch() - player.rotationPitch;
        float absYaw = Math.abs(yawDelta);
        float absPitch = Math.abs(pitchDelta);

        float yawFade = MathHelper.clamp((absYaw - 1.35F) / 24.0F, 0.0F, 1.0F);
        float pitchFade = MathHelper.clamp((absPitch - 1.0F) / 22.0F, 0.0F, 1.0F);
        float angularVelocity = Math.abs(computeRelativeYawVelocity(player, target));
        float yawMax = attackRotation ? ADVANCED_OVERSHOOT_MAX_ATTACK_YAW : ADVANCED_OVERSHOOT_MAX_YAW;
        float yawOvershoot = MathHelper.clamp(absYaw * 0.045F + angularVelocity * 0.24F, 0.0F, yawMax) * yawFade;
        float pitchOvershoot = MathHelper.clamp(absPitch * 0.030F, 0.0F, ADVANCED_OVERSHOOT_MAX_PITCH) * pitchFade;

        float correctedYaw = desiredAim.getYaw() + Math.copySign(yawOvershoot, yawDelta);
        float correctedPitch = desiredAim.getPitch() + Math.copySign(pitchOvershoot, pitchDelta);
        return new Rotation(MathHelper.wrapDegrees(correctedYaw), MathHelper.clamp(correctedPitch, -90.0F, 90.0F));
    }

    private float computeRelativeYawVelocity(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return 0.0F;
        }
        double currentDx = target.posX - player.posX;
        double currentDz = target.posZ - player.posZ;
        double previousDx = target.lastTickPosX - player.lastTickPosX;
        double previousDz = target.lastTickPosZ - player.lastTickPosZ;
        if ((currentDx * currentDx + currentDz * currentDz) < 1.0E-5D
                || (previousDx * previousDx + previousDz * previousDz) < 1.0E-5D) {
            return 0.0F;
        }
        float currentYaw = (float) (Math.toDegrees(Math.atan2(currentDz, currentDx)) - 90.0D);
        float previousYaw = (float) (Math.toDegrees(Math.atan2(previousDz, previousDx)) - 90.0D);
        return MathHelper.wrapDegrees(currentYaw - previousYaw);
    }

    private AimOffsetSample sampleAimOffsets(EntityPlayerSP player, EntityLivingBase target) {
        int tick = player == null ? Integer.MIN_VALUE : player.ticksExisted;
        int targetEntityId = target == null ? Integer.MIN_VALUE : target.getEntityId();
        String yawSpec = getAimYawOffsetSpec();
        String pitchSpec = getAimPitchOffsetSpec();
        if (this.aimOffsetSampleTick == tick
                && this.aimOffsetSampleTargetEntityId == targetEntityId
                && yawSpec.equals(this.aimOffsetSampleYawSpec)
                && pitchSpec.equals(this.aimOffsetSamplePitchSpec)) {
            return new AimOffsetSample(this.sampledAimYawOffset, this.sampledAimPitchOffset);
        }

        this.aimOffsetSampleTick = tick;
        this.aimOffsetSampleTargetEntityId = targetEntityId;
        this.aimOffsetSampleYawSpec = yawSpec;
        this.aimOffsetSamplePitchSpec = pitchSpec;
        this.sampledAimYawOffset = sampleAimOffset(aimYawOffsetSpec, aimYawOffset);
        this.sampledAimPitchOffset = sampleAimOffset(aimPitchOffsetSpec, aimPitchOffset);
        return new AimOffsetSample(this.sampledAimYawOffset, this.sampledAimPitchOffset);
    }

    private boolean shouldUseMotionCompensatedVisualAim(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null || !SpeedHandler.enabled) {
            return false;
        }
        double horizontalSpeed = getHorizontalPlayerMotion(player);
        return horizontalSpeed > 0.32D || SpeedHandler.getCurrentTimerSpeedMultiplier() > 1.02F;
    }

    private float getCurrentAimPartialTicks() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return 1.0F;
        }
        return MathHelper.clamp(mc.getRenderPartialTicks(), 0.0F, 1.0F);
    }

    private double interpolateAimCoordinate(double previous, double current, float progress) {
        return previous + (current - previous) * progress;
    }

    private double getHorizontalPlayerMotion(EntityPlayerSP player) {
        if (player == null) {
            return 0.0D;
        }
        return Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
    }

    private float getTargetYaw(EntityPlayerSP player, EntityLivingBase target) {
        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    private float getTargetYawFromPosition(double fromX, double fromZ, EntityLivingBase target) {
        double dx = target.posX - fromX;
        double dz = target.posZ - fromZ;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
    }

    private float getTargetPitch(EntityPlayerSP player, EntityLivingBase target) {
        double dx = target.posX - player.posX;
        double dz = target.posZ - player.posZ;
        double dy = target.posY + target.getEyeHeight() * 0.85D - (player.posY + player.getEyeHeight());
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
    }

    private float getTargetPitchFromPosition(double fromX, double fromY, double fromZ, EntityLivingBase target) {
        double dx = target.posX - fromX;
        double dz = target.posZ - fromZ;
        EntityPlayerSP currentPlayer = Minecraft.getMinecraft().player;
        double eyeHeight = currentPlayer == null ? 1.62D : currentPlayer.getEyeHeight();
        double dy = target.posY + target.getEyeHeight() * 0.85D - (fromY + eyeHeight);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
    }

    private float computeTurnSpeed(float yawDeltaAbs, boolean attackRotation) {
        float normalized = MathHelper.clamp(yawDeltaAbs / 120.0F, 0.0F, 1.0F);
        float eased = normalized * normalized;
        float effectiveMin = Math.max(0.65F, minTurnSpeed * (attackRotation ? 0.75F : 0.58F));
        float effectiveMax = Math.max(effectiveMin, maxTurnSpeed * (attackRotation ? 0.86F : 0.72F));
        return effectiveMin + (effectiveMax - effectiveMin) * eased;
    }

    private float clampSigned(float value, float maxMagnitude) {
        if (maxMagnitude <= 0.0F) {
            return 0.0F;
        }
        return Math.copySign(Math.min(Math.abs(value), maxMagnitude), value);
    }

    private static float sampleSmoothMaxTurnStepForTurn() {
        SmoothTurnStepRange range = parseSmoothMaxTurnStepSpec(smoothMaxTurnStepSpec, smoothMaxTurnStep);
        if (!range.isRandom()) {
            return range.min;
        }
        return (float) ThreadLocalRandom.current().nextDouble(range.min, range.max);
    }

    public static void setSmoothMaxTurnStepSpec(String spec) {
        SmoothTurnStepRange range = parseSmoothMaxTurnStepSpec(spec, smoothMaxTurnStep);
        smoothMaxTurnStep = range.min;
        smoothMaxTurnStepSpec = range.toSpec();
    }

    public static String getSmoothMaxTurnStepDisplayText() {
        return parseSmoothMaxTurnStepSpec(smoothMaxTurnStepSpec, smoothMaxTurnStep).toDisplayText();
    }

    public static String getSmoothMaxTurnStepSpec() {
        return parseSmoothMaxTurnStepSpec(smoothMaxTurnStepSpec, smoothMaxTurnStep).toSpec();
    }

    private static SmoothTurnStepRange parseSmoothMaxTurnStepSpec(String spec, float fallback) {
        float safeFallback = MathHelper.clamp(Float.isNaN(fallback) ? DEFAULT_SMOOTH_MAX_TURN_STEP : fallback,
                MIN_SMOOTH_MAX_TURN_STEP, MAX_SMOOTH_MAX_TURN_STEP);
        String normalized = spec == null ? "" : spec.trim();
        if (normalized.isEmpty()) {
            return new SmoothTurnStepRange(safeFallback, safeFallback);
        }

        normalized = normalized.replace("°", "")
                .replace("，", ",")
                .replace("－", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replace("~", "-")
                .replace("～", "-")
                .replace("至", "-")
                .replace("到", "-")
                .replaceAll("\\s+", "");
        String[] parts = normalized.split("-", -1);
        try {
            if (parts.length == 1) {
                float value = MathHelper.clamp(Float.parseFloat(parts[0]), MIN_SMOOTH_MAX_TURN_STEP,
                        MAX_SMOOTH_MAX_TURN_STEP);
                return new SmoothTurnStepRange(value, value);
            }
            if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                float first = Float.parseFloat(parts[0]);
                float second = Float.parseFloat(parts[1]);
                float min = MathHelper.clamp(Math.min(first, second), MIN_SMOOTH_MAX_TURN_STEP,
                        MAX_SMOOTH_MAX_TURN_STEP);
                float max = MathHelper.clamp(Math.max(first, second), MIN_SMOOTH_MAX_TURN_STEP,
                        MAX_SMOOTH_MAX_TURN_STEP);
                return new SmoothTurnStepRange(min, Math.max(min, max));
            }
        } catch (Exception ignored) {
        }
        return new SmoothTurnStepRange(safeFallback, safeFallback);
    }

    private static String formatSmoothMaxTurnStepValue(float value) {
        float clamped = MathHelper.clamp(value, MIN_SMOOTH_MAX_TURN_STEP, MAX_SMOOTH_MAX_TURN_STEP);
        if (Math.abs(clamped - Math.round(clamped)) < 0.0001F) {
            return String.valueOf(Math.round(clamped));
        }
        return String.format(Locale.ROOT, "%.2f", clamped).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static final class SmoothTurnStepRange {
        private final float min;
        private final float max;

        private SmoothTurnStepRange(float min, float max) {
            this.min = MathHelper.clamp(min, MIN_SMOOTH_MAX_TURN_STEP, MAX_SMOOTH_MAX_TURN_STEP);
            this.max = MathHelper.clamp(Math.max(this.min, max), MIN_SMOOTH_MAX_TURN_STEP,
                    MAX_SMOOTH_MAX_TURN_STEP);
        }

        private boolean isRandom() {
            return this.max - this.min > 0.0001F;
        }

        private String toSpec() {
            if (!isRandom()) {
                return formatSmoothMaxTurnStepValue(this.min);
            }
            return formatSmoothMaxTurnStepValue(this.min) + "-" + formatSmoothMaxTurnStepValue(this.max);
        }

        private String toDisplayText() {
            return toSpec();
        }
    }

    private static int sampleAttackSequenceDelayTicks() {
        return TickRangeSpec.sample(attackSequenceDelayTicksSpec, attackSequenceDelayTicks,
                MIN_ATTACK_SEQUENCE_DELAY_TICKS, MAX_ATTACK_SEQUENCE_DELAY_TICKS);
    }

    public static void setAttackSequenceDelayTicksSpec(String spec) {
        TickRangeSpec.Range range = TickRangeSpec.parse(spec, attackSequenceDelayTicks,
                MIN_ATTACK_SEQUENCE_DELAY_TICKS, MAX_ATTACK_SEQUENCE_DELAY_TICKS);
        attackSequenceDelayTicks = range.getMin();
        attackSequenceDelayTicksSpec = range.toSpec();
    }

    public static String getAttackSequenceDelayTicksDisplayText() {
        return TickRangeSpec.parse(attackSequenceDelayTicksSpec, attackSequenceDelayTicks,
                MIN_ATTACK_SEQUENCE_DELAY_TICKS, MAX_ATTACK_SEQUENCE_DELAY_TICKS).toDisplayText();
    }

    public static String getAttackSequenceDelayTicksSpec() {
        return TickRangeSpec.parse(attackSequenceDelayTicksSpec, attackSequenceDelayTicks,
                MIN_ATTACK_SEQUENCE_DELAY_TICKS, MAX_ATTACK_SEQUENCE_DELAY_TICKS).toSpec();
    }

    private static float sampleAimOffset(String spec, float fallback) {
        AimOffsetRange range = parseAimOffsetSpec(spec, fallback);
        if (!range.isRandom()) {
            return range.min;
        }
        return (float) ThreadLocalRandom.current().nextDouble(range.min, range.max);
    }

    public static void setAimYawOffsetSpec(String spec) {
        AimOffsetRange range = parseAimOffsetSpec(spec, aimYawOffset);
        aimYawOffset = range.min;
        aimYawOffsetSpec = range.toSpec();
    }

    public static void setAimPitchOffsetSpec(String spec) {
        AimOffsetRange range = parseAimOffsetSpec(spec, aimPitchOffset);
        aimPitchOffset = range.min;
        aimPitchOffsetSpec = range.toSpec();
    }

    public static String getAimYawOffsetDisplayText() {
        return parseAimOffsetSpec(aimYawOffsetSpec, aimYawOffset).toDisplayText();
    }

    public static String getAimPitchOffsetDisplayText() {
        return parseAimOffsetSpec(aimPitchOffsetSpec, aimPitchOffset).toDisplayText();
    }

    public static String getAimYawOffsetSpec() {
        return parseAimOffsetSpec(aimYawOffsetSpec, aimYawOffset).toSpec();
    }

    public static String getAimPitchOffsetSpec() {
        return parseAimOffsetSpec(aimPitchOffsetSpec, aimPitchOffset).toSpec();
    }

    private static AimOffsetRange parseAimOffsetSpec(String spec, float fallback) {
        float safeFallback = MathHelper.clamp(Float.isNaN(fallback) ? DEFAULT_AIM_OFFSET : fallback,
                MIN_AIM_OFFSET, MAX_AIM_OFFSET);
        String normalized = spec == null ? "" : spec.trim();
        if (normalized.isEmpty()) {
            return new AimOffsetRange(safeFallback, safeFallback);
        }

        normalized = normalized.replace("°", "")
                .replace("，", ",")
                .replace("－", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replace("～", "~")
                .replace("至", "~")
                .replace("到", "~")
                .trim();

        Matcher matcher = Pattern.compile("^([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*(?:[-~]\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)))?$")
                .matcher(normalized);
        try {
            if (matcher.matches()) {
                float first = Float.parseFloat(matcher.group(1));
                String secondText = matcher.group(2);
                if (secondText == null || secondText.trim().isEmpty()) {
                    float value = MathHelper.clamp(first, MIN_AIM_OFFSET, MAX_AIM_OFFSET);
                    return new AimOffsetRange(value, value);
                }
                float second = Float.parseFloat(secondText);
                float min = MathHelper.clamp(Math.min(first, second), MIN_AIM_OFFSET, MAX_AIM_OFFSET);
                float max = MathHelper.clamp(Math.max(first, second), MIN_AIM_OFFSET, MAX_AIM_OFFSET);
                return new AimOffsetRange(min, Math.max(min, max));
            }
        } catch (Exception ignored) {
        }
        return new AimOffsetRange(safeFallback, safeFallback);
    }

    private static String formatAimOffsetValue(float value) {
        float clamped = MathHelper.clamp(value, MIN_AIM_OFFSET, MAX_AIM_OFFSET);
        if (Math.abs(clamped - Math.round(clamped)) < 0.00005F) {
            return String.valueOf(Math.round(clamped));
        }
        return String.format(Locale.ROOT, "%.4f", clamped).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static final class AimOffsetRange {
        private final float min;
        private final float max;

        private AimOffsetRange(float min, float max) {
            this.min = MathHelper.clamp(min, MIN_AIM_OFFSET, MAX_AIM_OFFSET);
            this.max = MathHelper.clamp(Math.max(this.min, max), MIN_AIM_OFFSET, MAX_AIM_OFFSET);
        }

        private boolean isRandom() {
            return this.max - this.min > 0.000001F;
        }

        private String toSpec() {
            if (!isRandom()) {
                return formatAimOffsetValue(this.min);
            }
            return formatAimOffsetValue(this.min) + "-" + formatAimOffsetValue(this.max);
        }

        private String toDisplayText() {
            return toSpec();
        }
    }

    private static final class AimOffsetSample {
        private final float yaw;
        private final float pitch;

        private AimOffsetSample(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private float getTargetSearchRadius() {
        return isHuntEnabled() ? Math.max(attackRange, huntRadius) : attackRange;
    }

    private boolean matchesEnabledTargetGroup(EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            return targetPlayers;
        }
        if (isHostileTargetType(target)) {
            return targetHostile;
        }
        if (isPassiveTargetType(target)) {
            return targetPassive;
        }
        return false;
    }

    private boolean isHostileTargetType(EntityLivingBase target) {
        if (target == null) {
            return false;
        }
        return target instanceof IMob || target instanceof EntityDragon
                || target.isCreatureType(EnumCreatureType.MONSTER, false);
    }

    private boolean isPassiveTargetType(EntityLivingBase target) {
        if (target == null) {
            return false;
        }
        return target instanceof EntityAnimal || target instanceof EntityAmbientCreature
                || target instanceof EntityWaterMob || target instanceof EntityVillager || target instanceof EntityGolem
                || target.isCreatureType(EnumCreatureType.CREATURE, false)
                || target.isCreatureType(EnumCreatureType.AMBIENT, false)
                || target.isCreatureType(EnumCreatureType.WATER_CREATURE, false);
    }

    private boolean isPacketAttackMode() {
        return ATTACK_MODE_PACKET.equalsIgnoreCase(attackMode);
    }

    private boolean isTeleportAttackMode() {
        return ATTACK_MODE_TELEPORT.equalsIgnoreCase(attackMode);
    }

    private boolean isSequenceAttackMode() {
        return ATTACK_MODE_SEQUENCE.equalsIgnoreCase(attackMode);
    }

    private boolean isMouseClickAttackMode() {
        return ATTACK_MODE_MOUSE_CLICK.equalsIgnoreCase(attackMode);
    }

    private boolean shouldRotateToTarget() {
        return aimOnlyMode || (!isPacketAttackMode() && rotateToTarget);
    }

    private boolean shouldRotateOnlyOnAttack() {
        return rotateOnlyOnAttack && !aimOnlyMode && shouldRotateToTarget();
    }

    private boolean shouldUseRelockOnlyWhenNoCrosshairTarget() {
        return relockOnlyWhenNoCrosshairTarget && shouldRotateToTarget();
    }

    private boolean isSameEntity(EntityLivingBase left, EntityLivingBase right) {
        return left != null && right != null && left.getEntityId() == right.getEntityId();
    }

    private boolean canTriggerAttackSequence(EntityPlayerSP player, EntityLivingBase target) {
        return canTriggerAttackSequence(player, target, null);
    }

    private boolean canTriggerAttackSequence(EntityPlayerSP player, EntityLivingBase target,
            AreaHuntOptions areaOptions) {
        if (player == null || target == null) {
            return false;
        }
        if (this.sequenceCooldownTicks > 0 || this.attackSequenceExecutor.isRunning()) {
            return false;
        }
        if (!hasConfiguredAttackSequence()) {
            return false;
        }
        if (shouldRequireCrosshairHitForAttack(false)
                && !isViewRayHittingAttackableTarget(player, target, areaOptions)) {
            return false;
        }
        return isValidTarget(player, target, areaOptions);
    }

    private boolean triggerAttackSequence(EntityPlayerSP player, EntityLivingBase target) {
        String sequenceName = getConfiguredAttackSequenceName();
        if (sequenceName.isEmpty()) {
            return false;
        }

        PathSequence configuredSequence = PathSequenceManager.getSequence(sequenceName);
        if (configuredSequence == null || configuredSequence.getSteps().isEmpty()) {
            return false;
        }

        this.attackSequenceExecutor.start(configuredSequence, player, target);
        return this.attackSequenceExecutor.isRunning();
    }

    private static boolean hasConfiguredAttackSequence() {
        String sequenceName = getConfiguredAttackSequenceName();
        return !sequenceName.isEmpty() && PathSequenceManager.hasSequence(sequenceName);
    }

    public static String getConfiguredAttackSequenceName() {
        return attackSequenceName == null ? "" : attackSequenceName.trim();
    }

    private KillAuraOrbitProcess getKillAuraOrbitProcess() {
        try {
            Object primary = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (primary instanceof Baritone) {
                return ((Baritone) primary).getKillAuraOrbitProcess();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean requestHuntOrbitProcess(EntityLivingBase target, int nowTick) {
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        if (orbitProcess == null || target == null) {
            return false;
        }
        double radius = getEffectiveHuntFixedDistance();
        boolean sameTarget = target.getEntityId() == this.lastOrbitProcessTargetEntityId;
        boolean sameRadius = !Double.isNaN(this.lastOrbitProcessRequestedRadius)
                && Math.abs(this.lastOrbitProcessRequestedRadius - radius) <= 0.001D;
        boolean shouldRefreshRequest = !orbitProcess.isActive()
                || !sameTarget
                || !sameRadius
                || nowTick - this.lastOrbitProcessRequestTick >= HUNT_ORBIT_PROCESS_REQUEST_INTERVAL_TICKS;
        if (shouldRefreshRequest) {
            this.lastOrbitProcessRequestTick = nowTick;
            this.lastOrbitProcessTargetEntityId = target.getEntityId();
            this.lastOrbitProcessRequestedRadius = radius;
            return orbitProcess.requestOrbit(target, radius);
        }
        return orbitProcess.hasUsableLoop();
    }

    private boolean isHuntOrbitProcessActive() {
        if (!isHuntOrbitEnabled()) {
            return false;
        }
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        return orbitProcess != null && orbitProcess.hasUsableLoop();
    }

    private void stopHuntOrbitProcess() {
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        if (orbitProcess != null) {
            orbitProcess.requestStop();
        }
        this.lastOrbitProcessRequestTick = -99999;
        this.lastOrbitProcessTargetEntityId = Integer.MIN_VALUE;
        this.lastOrbitProcessRequestedRadius = Double.NaN;
    }

    private void renderHuntOrbitLoop() {
        if (!isHuntOrbitEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || this.currentTargetEntityId == -1) {
            return;
        }
        Entity entity = mc.world.getEntityByID(this.currentTargetEntityId);
        if (!(entity instanceof EntityLivingBase) || !entity.isEntityAlive()) {
            return;
        }
        List<Vec3d> renderLoop = null;
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        if (orbitProcess != null) {
            List<Vec3d> processLoop = orbitProcess.getRenderedLoopView();
            if (processLoop != null && processLoop.size() >= 2) {
                renderLoop = processLoop;
            }
        }
        if (renderLoop == null || renderLoop.size() < 2) {
            renderLoop = HuntOrbitController.buildPreviewLoop((EntityLivingBase) entity,
                    getEffectiveHuntFixedDistance(), getConfiguredHuntOrbitSamplePoints());
        }
        if (renderLoop.size() < 2) {
            return;
        }
        PathRenderer.drawPolyline(renderLoop, new Color(0xFF3B30), 0.95F, 3.0F, true);
    }

    private void handleHuntMovement(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            stopHuntNavigation();
            return;
        }
        if (isHuntOrbitEnabled() && shouldBlockOrbitNavigationWhileAirborne(player)) {
            stopHuntNavigation();
            return;
        }

        int nowTick = player.ticksExisted;
        if (isHuntOrbitEnabled()) {
            if (this.huntOrbitController.isActive() && canStartOrbitHunt(player, target)) {
                stopEmbeddedHuntNavigation();
                stopHuntOrbitProcess();
                driveContinuousHuntOrbit(player, target);
                return;
            }

            boolean orbitProcessActive = requestHuntOrbitProcess(target, nowTick);
            if (orbitProcessActive) {
                stopEmbeddedHuntNavigation();
                if (shouldUseContinuousOrbitController(player, target)) {
                    stopHuntOrbitProcess();
                    driveContinuousHuntOrbit(player, target);
                } else {
                    this.huntOrbitController.stop();
                }
                return;
            }

            this.huntOrbitController.stop();
        }

        int targetId = target.getEntityId();
        double dx = target.posX - this.lastHuntTargetX;
        double dz = target.posZ - this.lastHuntTargetZ;
        double movedSq = dx * dx + dz * dz;

        boolean shouldSendGoto = !huntNavigationActive || targetId != this.lastHuntTargetEntityId
                || movedSq >= HUNT_GOTO_MOVE_THRESHOLD_SQ
                || (nowTick - this.lastHuntGotoTick) >= HUNT_GOTO_INTERVAL_TICKS;

        if (shouldSendGoto) {
            if (isHuntFixedDistanceMode()) {
                double[] safeDestination = findFixedDistanceHuntNavigationDestination(player, target);
                if (safeDestination != null) {
                    EmbeddedNavigationHandler.INSTANCE.startGoto(safeDestination[0], safeDestination[1],
                            safeDestination[2], true);
                } else {
                    // If the orbit process failed to produce a usable loop, do not keep
                    // simulating a fake orbit point with the legacy fallback. That causes
                    // "no red loop, but the goal point still jumps around the circle".
                    // In this case we should fall back to a plain fixed-distance anchor.
                    double[] destination = computeFixedDistanceHuntDestination(player, target);
                    EmbeddedNavigationHandler.INSTANCE.startGotoXZ(destination[0], destination[2], true);
                }
            } else {
                double[] safeDestination = findApproachHuntNavigationDestination(player, target);
                if (safeDestination != null) {
                    EmbeddedNavigationHandler.INSTANCE.startGoto(safeDestination[0], safeDestination[1],
                            safeDestination[2], true);
                } else {
                    EmbeddedNavigationHandler.INSTANCE.startGotoXZ(target.posX, target.posZ, true);
                }
            }
            this.huntNavigationActive = true;
            this.lastHuntGotoTick = nowTick;
            this.lastHuntTargetEntityId = targetId;
            this.lastHuntTargetX = target.posX;
            this.lastHuntTargetZ = target.posZ;
        }
    }

    private EntityItem findHuntPriorityPickupItem(EntityPlayerSP player) {
        if (player == null || player.world == null || !isHuntEnabled() || huntRadius <= 0.05F) {
            return null;
        }

        int nowTick = player.ticksExisted;
        double radiusSq = huntRadius * huntRadius;
        if (nowTick - lastHuntPickupSearchTick < HUNT_PICKUP_SEARCH_INTERVAL_TICKS) {
            EntityItem cached = resolveCachedHuntPickupItem(player, radiusSq);
            if (cached != null) {
                return cached;
            }
            if (!lastHuntPickupSearchFound) {
                return null;
            }
        }

        EntityItem nearest = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Entity entity : player.world.loadedEntityList) {
            if (!(entity instanceof EntityItem)) {
                continue;
            }

            EntityItem item = (EntityItem) entity;
            if (item.isDead || !item.onGround) {
                continue;
            }

            double playerDistSq = player.getDistanceSq(item);
            if (playerDistSq > radiusSq) {
                continue;
            }

            if (playerDistSq < bestDistSq) {
                bestDistSq = playerDistSq;
                nearest = item;
            }
        }

        lastHuntPickupSearchTick = nowTick;
        lastHuntPickupSearchTargetEntityId = nearest == null ? Integer.MIN_VALUE : nearest.getEntityId();
        lastHuntPickupSearchFound = nearest != null;
        return nearest;
    }

    private EntityItem resolveCachedHuntPickupItem(EntityPlayerSP player, double radiusSq) {
        if (player == null || player.world == null || lastHuntPickupSearchTargetEntityId == Integer.MIN_VALUE) {
            return null;
        }
        Entity entity = player.world.getEntityByID(lastHuntPickupSearchTargetEntityId);
        if (!(entity instanceof EntityItem)) {
            return null;
        }
        EntityItem item = (EntityItem) entity;
        return item.isDead || !item.onGround || player.getDistanceSq(item) > radiusSq ? null : item;
    }

    private void handleHuntPickupMovement(EntityPlayerSP player, EntityItem item) {
        if (player == null || item == null || item.isDead) {
            stopHuntPickupNavigation();
            return;
        }

        if (hasReachedHuntPickupItem(player, item)) {
            stopHuntPickupNavigation();
            return;
        }

        int nowTick = player.ticksExisted;
        int itemId = item.getEntityId();
        boolean shouldSendGoto = !huntPickupNavigationActive
                || itemId != this.lastHuntPickupTargetEntityId
                || (nowTick - this.lastHuntPickupGotoTick) >= HUNT_PICKUP_GOTO_INTERVAL_TICKS;
        if (!shouldSendGoto) {
            return;
        }

        EmbeddedNavigationHandler.INSTANCE.startGoto(item.posX, item.posY, item.posZ);
        this.huntPickupNavigationActive = true;
        this.lastHuntPickupGotoTick = nowTick;
        this.lastHuntPickupTargetEntityId = itemId;
    }

    private boolean hasReachedHuntPickupItem(EntityPlayerSP player, EntityItem item) {
        if (player == null || item == null || item.isDead) {
            return false;
        }

        // Hunt 拾取必须真正踩到掉落物实体上，不能只是在附近 1 格就停下。
        return player.getEntityBoundingBox()
                .grow(HUNT_PICKUP_OVERLAP_GROWTH, 0.0D, HUNT_PICKUP_OVERLAP_GROWTH)
                .intersects(item.getEntityBoundingBox());
    }

    private void stopHuntNavigation() {
        this.huntOrbitController.stop();
        stopHuntOrbitProcess();
        stopEmbeddedHuntNavigation();
    }

    private void stopEmbeddedHuntNavigation() {
        if (!this.huntNavigationActive) {
            return;
        }
        EmbeddedNavigationHandler.INSTANCE.stop();
        this.huntNavigationActive = false;
        this.lastHuntGotoTick = -99999;
        this.lastHuntTargetEntityId = Integer.MIN_VALUE;
        this.lastHuntTargetX = 0.0D;
        this.lastHuntTargetZ = 0.0D;
    }

    private void stopHuntPickupNavigation() {
        if (!this.huntPickupNavigationActive) {
            return;
        }
        EmbeddedNavigationHandler.INSTANCE.stop();
        this.huntPickupNavigationActive = false;
        this.lastHuntPickupGotoTick = -99999;
        this.lastHuntPickupTargetEntityId = Integer.MIN_VALUE;
    }

    private boolean shouldRunHuntMovement(EntityPlayerSP player, EntityLivingBase target) {
        if (!isHuntEnabled() || player == null || target == null) {
            return false;
        }
        if (isHuntOrbitEnabled() && shouldBlockOrbitNavigationWhileAirborne(player)) {
            return false;
        }

        double distance = player.getDistance(target);
        boolean missingAttackLineOfSight = requireLineOfSight && !player.canEntityBeSeen(target);
        if (isHuntFixedDistanceMode()) {
            if (canStartOrbitHunt(player, target)) {
                return true;
            }
            return missingAttackLineOfSight
                    || Math.abs(distance - getEffectiveHuntFixedDistance()) > HUNT_FIXED_DISTANCE_TOLERANCE;
        }
        return missingAttackLineOfSight || distance > attackRange;
    }

    private boolean canStartOrbitHunt(EntityPlayerSP player, EntityLivingBase target) {
        if (!isHuntOrbitEnabled() || player == null || target == null) {
            return false;
        }
        if (shouldBlockOrbitNavigationWhileAirborne(player)) {
            return false;
        }
        if (Math.abs(player.posY - target.posY) > HUNT_ORBIT_MAX_ENTRY_VERTICAL_DELTA) {
            return false;
        }
        double maxEntryDistance = getEffectiveHuntFixedDistance() + HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER;
        double allowedDistance = this.huntOrbitController.isActive()
                ? maxEntryDistance + HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER
                : maxEntryDistance;
        return player.getDistanceSq(target) <= allowedDistance * allowedDistance;
    }

    private boolean shouldUseContinuousOrbitController(EntityPlayerSP player, EntityLivingBase target) {
        if (!canStartOrbitHunt(player, target)) {
            return false;
        }
        return isPlayerOnHuntOrbitLoop(player);
    }

    private boolean isPlayerOnHuntOrbitLoop(EntityPlayerSP player) {
        if (player == null) {
            return false;
        }
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        if (orbitProcess == null || !orbitProcess.isActive()) {
            return false;
        }
        List<Vec3d> renderLoop = orbitProcess.getRenderedLoopView();
        if (renderLoop == null || renderLoop.size() < 2) {
            return false;
        }
        double distanceToLoop = getHorizontalDistanceToOrbitLoop(player.posX, player.posZ, renderLoop);
        return distanceToLoop <= HUNT_CONTINUOUS_ORBIT_LOOP_ENTRY_MAX_DISTANCE;
    }

    private double getHorizontalDistanceToOrbitLoop(double playerX, double playerZ, List<Vec3d> renderLoop) {
        if (renderLoop == null || renderLoop.size() < 2) {
            return Double.POSITIVE_INFINITY;
        }
        Vec3d playerPos = new Vec3d(playerX, 0.0D, playerZ);
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int i = 0; i < renderLoop.size() - 1; i++) {
            Vec3d start = flattenToHorizontal(renderLoop.get(i));
            Vec3d end = flattenToHorizontal(renderLoop.get(i + 1));
            Vec3d nearest = nearestPointOnHorizontalSegment(playerPos, start, end);
            bestDistanceSq = Math.min(bestDistanceSq, playerPos.squareDistanceTo(nearest));
        }
        return bestDistanceSq == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : Math.sqrt(bestDistanceSq);
    }

    private Vec3d nearestPointOnHorizontalSegment(Vec3d point, Vec3d start, Vec3d end) {
        Vec3d segment = end.subtract(start);
        double lengthSq = segment.lengthSquared();
        if (lengthSq <= 1.0E-6D) {
            return start;
        }
        double t = point.subtract(start).dotProduct(segment) / lengthSq;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return start.add(segment.scale(t));
    }

    private Vec3d flattenToHorizontal(Vec3d vec) {
        return vec == null ? Vec3d.ZERO : new Vec3d(vec.x, 0.0D, vec.z);
    }

    private boolean shouldBlockOrbitNavigationWhileAirborne(EntityPlayerSP player) {
        if (player == null) {
            return false;
        }
        return (player.capabilities != null && player.capabilities.isFlying) || player.isElytraFlying();
    }

    private void driveContinuousHuntOrbit(EntityPlayerSP player, EntityLivingBase target) {
        this.huntOrbitController.tick(player, target,
                new HuntOrbitController.OrbitConfig(getEffectiveHuntFixedDistance(), HUNT_FIXED_DISTANCE_TOLERANCE,
                        huntJumpOrbitEnabled, true, true));
    }

    private double[] computeFixedDistanceHuntDestination(EntityPlayerSP player, EntityLivingBase target) {
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

        double desiredDistance = getEffectiveHuntFixedDistance();
        double scale = desiredDistance / Math.max(distance, 1.0E-4D);
        double destinationX = target.posX + dx * scale;
        double destinationY = target.posY + dy * scale;
        double destinationZ = target.posZ + dz * scale;
        double[] clippedDestination = clipHuntDestinationXZ(target.posX, target.posZ, destinationX, destinationZ);
        return new double[] { clippedDestination[0], destinationY, clippedDestination[1] };
    }

    private double[] findApproachHuntNavigationDestination(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return null;
        }
        double maxStandRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, attackRange - HUNT_APPROACH_TARGET_BUFFER);
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS,
                Math.min(maxStandRadius, attackRange - HUNT_APPROACH_TARGET_BUFFER * 2.0D));
        return findHuntNavigationDestinationAroundTarget(player, target, preferredRadius,
                HUNT_APPROACH_MIN_STAND_RADIUS, maxStandRadius);
    }

    private double[] findFixedDistanceHuntNavigationDestination(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return null;
        }
        if (isHuntOrbitEnabled()) {
            double[] orbitAligned = findOrbitAlignedHuntNavigationDestination(player, target);
            if (orbitAligned != null) {
                return orbitAligned;
            }
        }
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, getEffectiveHuntFixedDistance());
        double minRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, preferredRadius - 1.0D);
        double maxRadius = Math.max(minRadius, preferredRadius + 1.0D);
        double[] destination = findHuntNavigationDestinationAroundTarget(player, target, preferredRadius,
                minRadius, maxRadius);
        if (destination != null) {
            return destination;
        }
        double[] fallback = computeFixedDistanceHuntDestination(player, target);
        return findSafeHuntNavigationDestination(player, fallback[0], fallback[1], fallback[2]);
    }

    private double[] findOrbitAlignedHuntNavigationDestination(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return null;
        }
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        if (orbitProcess != null) {
            double[] plannedEntry = findOrbitEntryFromLoopNodes(player, target, orbitProcess.getNavigationLoopSnapshot());
            if (plannedEntry != null) {
                orbitDebug("huntOrbitEntry source=planned_nodes dest=%s radius=%.3f", formatVec3(plannedEntry),
                        getHorizontalRadiusToTarget(target, plannedEntry));
                return plannedEntry;
            }
            double[] renderedEntry = findOrbitEntryFromRenderLoop(player, target, orbitProcess.getRenderedLoopView());
            if (renderedEntry != null) {
                orbitDebug("huntOrbitEntry source=process_render dest=%s radius=%.3f", formatVec3(renderedEntry),
                        getHorizontalRadiusToTarget(target, renderedEntry));
                return renderedEntry;
            }
        }

        double[] previewEntry = findOrbitEntryFromRenderLoop(player, target,
                HuntOrbitController.buildPreviewLoop(target, getEffectiveHuntFixedDistance(),
                        getConfiguredHuntOrbitSamplePoints()));
        if (previewEntry != null) {
            orbitDebug("huntOrbitEntry source=preview_render dest=%s radius=%.3f", formatVec3(previewEntry),
                    getHorizontalRadiusToTarget(target, previewEntry));
            return previewEntry;
        }

        double[] exactOrbitPoint = computeFixedDistanceHuntDestination(player, target);
        double[] directSafeDestination = findSafeHuntNavigationDestination(player, exactOrbitPoint[0], exactOrbitPoint[1],
                exactOrbitPoint[2], HUNT_ORBIT_ENTRY_SAFE_SEARCH_RADIUS);
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, getEffectiveHuntFixedDistance());
        double orbitBand = Math.max(HUNT_FIXED_DISTANCE_TOLERANCE, HUNT_ORBIT_ENTRY_RADIUS_BAND);
        if (directSafeDestination != null
                && isDestinationNearOrbitBand(target, directSafeDestination, preferredRadius, orbitBand)
                && centerDistSq(directSafeDestination[0], directSafeDestination[2], exactOrbitPoint[0], exactOrbitPoint[2]) <= 0.65D * 0.65D) {
            orbitDebug("huntOrbitEntry source=exact_safe dest=%s radius=%.3f exact=%s", formatVec3(directSafeDestination),
                    getHorizontalRadiusToTarget(target, directSafeDestination), formatVec3(exactOrbitPoint));
            return directSafeDestination;
        }

        orbitDebug("huntOrbitEntry source=exact_xz dest=%s radius=%.3f", formatVec3(exactOrbitPoint),
                getHorizontalRadiusToTarget(target, exactOrbitPoint));
        return null;
    }

    private double[] findOrbitEntryFromLoopNodes(EntityPlayerSP player, EntityLivingBase target,
            List<BetterBlockPos> loopNodes) {
        if (player == null || target == null || loopNodes == null || loopNodes.isEmpty()) {
            return null;
        }
        double[] bestVisibleDestination = null;
        double bestVisibleScore = Double.POSITIVE_INFINITY;
        double[] bestFallbackDestination = null;
        double bestFallbackScore = Double.POSITIVE_INFINITY;
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, getEffectiveHuntFixedDistance());
        double orbitBand = Math.max(HUNT_FIXED_DISTANCE_TOLERANCE, HUNT_ORBIT_ENTRY_RADIUS_BAND);

        for (BetterBlockPos node : loopNodes) {
            if (node == null) {
                continue;
            }
            double[] destination = new double[] { node.x + 0.5D, node.y, node.z + 0.5D };
            if (!isDestinationNearOrbitBand(target, destination, preferredRadius, orbitBand + 0.35D)) {
                continue;
            }
            BlockPos standPos = new BlockPos(node.x, node.y, node.z);
            boolean hasLineOfSight = hasHuntLineOfSightFromStandPos(standPos, target);
            double score = scoreHuntNavigationDestination(player, target, destination, preferredRadius, hasLineOfSight);
            if (hasLineOfSight && score < bestVisibleScore) {
                bestVisibleScore = score;
                bestVisibleDestination = destination;
            }
            if (score < bestFallbackScore) {
                bestFallbackScore = score;
                bestFallbackDestination = destination;
            }
        }

        return bestVisibleDestination != null ? bestVisibleDestination : bestFallbackDestination;
    }

    private double[] findOrbitEntryFromRenderLoop(EntityPlayerSP player, EntityLivingBase target, List<Vec3d> renderLoop) {
        if (player == null || target == null || renderLoop == null || renderLoop.isEmpty()) {
            return null;
        }
        double[] bestVisibleDestination = null;
        double bestVisibleScore = Double.POSITIVE_INFINITY;
        double[] bestFallbackDestination = null;
        double bestFallbackScore = Double.POSITIVE_INFINITY;
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, getEffectiveHuntFixedDistance());
        double orbitBand = Math.max(HUNT_FIXED_DISTANCE_TOLERANCE, HUNT_ORBIT_ENTRY_RADIUS_BAND);
        int candidateCount = getOrbitRenderCandidateCount(renderLoop);

        for (int i = 0; i < candidateCount; i++) {
            Vec3d point = renderLoop.get(i);
            if (point == null) {
                continue;
            }
            double[] safeDestination = findSafeHuntNavigationDestination(player, point.x, target.posY, point.z,
                    HUNT_ORBIT_ENTRY_SAFE_SEARCH_RADIUS);
            if (safeDestination == null) {
                continue;
            }
            if (!isDestinationNearOrbitBand(target, safeDestination, preferredRadius, orbitBand)) {
                continue;
            }
            if (centerDistSq(safeDestination[0], safeDestination[2], point.x, point.z)
                    > HUNT_ORBIT_ENTRY_POINT_TOLERANCE * HUNT_ORBIT_ENTRY_POINT_TOLERANCE) {
                continue;
            }

            BlockPos standPos = new BlockPos(safeDestination[0], safeDestination[1], safeDestination[2]);
            boolean hasLineOfSight = hasHuntLineOfSightFromStandPos(standPos, target);
            double score = scoreOrbitEntryDestination(player, target, safeDestination, point, preferredRadius, hasLineOfSight);
            if (hasLineOfSight && score < bestVisibleScore) {
                bestVisibleScore = score;
                bestVisibleDestination = safeDestination;
            }
            if (score < bestFallbackScore) {
                bestFallbackScore = score;
                bestFallbackDestination = safeDestination;
            }
        }

        return bestVisibleDestination != null ? bestVisibleDestination : bestFallbackDestination;
    }

    private int getOrbitRenderCandidateCount(List<Vec3d> renderLoop) {
        if (renderLoop == null || renderLoop.isEmpty()) {
            return 0;
        }
        if (renderLoop.size() >= 2 && renderLoop.get(0) != null && renderLoop.get(renderLoop.size() - 1) != null
                && renderLoop.get(0).squareDistanceTo(renderLoop.get(renderLoop.size() - 1)) <= 1.0E-4D) {
            return renderLoop.size() - 1;
        }
        return renderLoop.size();
    }

    private double[] findHuntNavigationDestinationAroundTarget(EntityPlayerSP player, EntityLivingBase target,
            double preferredRadius, double minRadius, double maxRadius) {
        return findHuntNavigationDestinationAroundTarget(player, target, preferredRadius, minRadius, maxRadius, true, 2);
    }

    private double[] findHuntNavigationDestinationAroundTarget(EntityPlayerSP player, EntityLivingBase target,
            double preferredRadius, double minRadius, double maxRadius, boolean allowCenterFallback,
            int safeSearchRadius) {
        if (player == null || player.world == null || target == null) {
            return null;
        }

        double clampedMinRadius = Math.max(0.0D, minRadius);
        double clampedPreferredRadius = Math.max(clampedMinRadius, preferredRadius);
        double clampedMaxRadius = Math.max(clampedPreferredRadius, maxRadius);
        double[] bestVisibleDestination = null;
        double bestVisibleScore = Double.POSITIVE_INFINITY;
        double[] bestFallbackDestination = null;
        double bestFallbackScore = Double.POSITIVE_INFINITY;
        double baseAngle = Math.atan2(player.posZ - target.posZ, player.posX - target.posX);

        for (double radius : buildHuntRadiusSamples(clampedPreferredRadius, clampedMinRadius, clampedMaxRadius)) {
            for (int angleIndex = 0; angleIndex <= HUNT_NAVIGATION_ANGLE_SAMPLE_PAIRS * 2; angleIndex++) {
                double angleOffset;
                if (angleIndex == 0) {
                    angleOffset = 0.0D;
                } else {
                    int ringIndex = (angleIndex + 1) / 2;
                    angleOffset = ringIndex * HUNT_NAVIGATION_ANGLE_SAMPLE_STEP_RADIANS;
                    if ((angleIndex & 1) == 0) {
                        angleOffset = -angleOffset;
                    }
                }

                double desiredX = target.posX + Math.cos(baseAngle + angleOffset) * radius;
                double desiredZ = target.posZ + Math.sin(baseAngle + angleOffset) * radius;
                double[] clippedDestination = clipHuntDestinationXZ(target.posX, target.posZ, desiredX, desiredZ);
                double[] safeDestination = findSafeHuntNavigationDestination(player, clippedDestination[0], target.posY,
                        clippedDestination[1], safeSearchRadius);
                if (safeDestination == null) {
                    continue;
                }

                BlockPos standPos = new BlockPos(safeDestination[0], safeDestination[1], safeDestination[2]);
                boolean hasLineOfSight = hasHuntLineOfSightFromStandPos(standPos, target);
                double score = scoreHuntNavigationDestination(player, target, safeDestination, clampedPreferredRadius,
                        hasLineOfSight);
                if (hasLineOfSight && score < bestVisibleScore) {
                    bestVisibleScore = score;
                    bestVisibleDestination = safeDestination;
                }
                if (score < bestFallbackScore) {
                    bestFallbackScore = score;
                    bestFallbackDestination = safeDestination;
                }
            }
        }

        if (bestVisibleDestination != null) {
            return bestVisibleDestination;
        }
        if (bestFallbackDestination != null) {
            return bestFallbackDestination;
        }
        if (!allowCenterFallback) {
            return null;
        }
        return findSafeHuntNavigationDestination(player, target.posX, target.posY, target.posZ);
    }

    private List<Double> buildHuntRadiusSamples(double preferredRadius, double minRadius, double maxRadius) {
        List<Double> samples = new ArrayList<>();
        addHuntRadiusSample(samples, preferredRadius, minRadius, maxRadius);
        double maxOffset = Math.max(preferredRadius - minRadius, maxRadius - preferredRadius);
        for (double offset = HUNT_NAVIGATION_RADIUS_SAMPLE_STEP; offset <= maxOffset + 1.0E-4D;
                offset += HUNT_NAVIGATION_RADIUS_SAMPLE_STEP) {
            addHuntRadiusSample(samples, preferredRadius - offset, minRadius, maxRadius);
            addHuntRadiusSample(samples, preferredRadius + offset, minRadius, maxRadius);
        }
        addHuntRadiusSample(samples, minRadius, minRadius, maxRadius);
        addHuntRadiusSample(samples, maxRadius, minRadius, maxRadius);
        return samples;
    }

    private void addHuntRadiusSample(List<Double> samples, double radius, double minRadius, double maxRadius) {
        if (samples == null) {
            return;
        }
        double clamped = MathHelper.clamp(radius, minRadius, maxRadius);
        for (Double existing : samples) {
            if (existing != null && Math.abs(existing - clamped) <= 1.0E-4D) {
                return;
            }
        }
        samples.add(clamped);
    }

    private double scoreHuntNavigationDestination(EntityPlayerSP player, EntityLivingBase target, double[] destination,
            double preferredRadius, boolean hasLineOfSight) {
        if (player == null || target == null || destination == null || destination.length < 3) {
            return Double.POSITIVE_INFINITY;
        }

        double targetDx = destination[0] - target.posX;
        double targetDz = destination[2] - target.posZ;
        double actualRadius = Math.sqrt(targetDx * targetDx + targetDz * targetDz);
        double radiusPenalty = Math.abs(actualRadius - preferredRadius);
        double playerDx = destination[0] - player.posX;
        double playerDy = destination[1] - player.posY;
        double playerDz = destination[2] - player.posZ;
        double playerDistancePenalty = playerDx * playerDx + playerDz * playerDz + playerDy * playerDy * 0.35D;
        double verticalPenalty = Math.abs(destination[1] - target.posY);
        double visibilityPenalty = hasLineOfSight ? 0.0D : 4.0D;
        return radiusPenalty * 4.0D + playerDistancePenalty * 0.18D + verticalPenalty * 0.7D + visibilityPenalty;
    }

    private double scoreOrbitEntryDestination(EntityPlayerSP player, EntityLivingBase target, double[] destination,
            Vec3d desiredPoint, double preferredRadius, boolean hasLineOfSight) {
        double score = scoreHuntNavigationDestination(player, target, destination, preferredRadius, hasLineOfSight);
        if (destination == null || desiredPoint == null) {
            return score;
        }
        return score + centerDistSq(destination[0], destination[2], desiredPoint.x, desiredPoint.z) * 4.5D;
    }

    private boolean isDestinationNearOrbitBand(EntityLivingBase target, double[] destination, double preferredRadius,
            double orbitBand) {
        if (target == null || destination == null || destination.length < 3) {
            return false;
        }
        double actualRadius = Math.sqrt(centerDistSq(destination[0], destination[2], target.posX, target.posZ));
        return Math.abs(actualRadius - preferredRadius) <= Math.max(0.1D, orbitBand);
    }

    private double getHorizontalRadiusToTarget(EntityLivingBase target, double[] destination) {
        if (target == null || destination == null || destination.length < 3) {
            return 0.0D;
        }
        return Math.sqrt(centerDistSq(destination[0], destination[2], target.posX, target.posZ));
    }

    private boolean isOrbitDebugEnabled() {
        return ModConfig.isDebugFlagEnabled(DebugModule.KILL_AURA_ORBIT);
    }

    private void orbitDebug(String format, Object... args) {
        if (!isOrbitDebugEnabled()) {
            return;
        }
        ModConfig.debugLog(DebugModule.KILL_AURA_ORBIT, String.format(Locale.ROOT, format, args));
    }

    private String formatVec3(double[] pos) {
        if (pos == null || pos.length < 3) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", pos[0], pos[1], pos[2]);
    }

    private double[] clipHuntDestinationXZ(double centerX, double centerZ, double destinationX, double destinationZ) {
        if (!AutoFollowHandler.hasActiveLockChaseRestriction()
                || AutoFollowHandler.isPositionWithinActiveLockChaseBounds(destinationX, destinationZ)) {
            return new double[] { destinationX, destinationZ };
        }

        double dx = destinationX - centerX;
        double dz = destinationZ - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 1.0E-4D) {
            return new double[] { centerX, centerZ };
        }

        return getClippedHuntPoint(centerX, centerZ, distance, Math.atan2(dz, dx));
    }

    private double[] findSafeHuntNavigationDestination(EntityPlayerSP player, double desiredX, double desiredY,
            double desiredZ) {
        return findSafeHuntNavigationDestination(player, desiredX, desiredY, desiredZ, 2);
    }

    private double[] findSafeHuntNavigationDestination(EntityPlayerSP player, double desiredX, double desiredY,
            double desiredZ, int horizontalSearchRadius) {
        if (player == null || player.world == null) {
            return null;
        }

        int baseX = MathHelper.floor(desiredX);
        int baseY = MathHelper.floor(desiredY);
        int baseZ = MathHelper.floor(desiredZ);
        BlockPos bestStandPos = null;
        double bestScore = Double.MAX_VALUE;
        int maxHorizontalSearchRadius = Math.max(0, horizontalSearchRadius);

        for (int radius = 0; radius <= maxHorizontalSearchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 3; dy >= -4; dy--) {
                        BlockPos candidate = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                        if (!isStandableHuntFeetPos(candidate)) {
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

    private boolean hasHuntLineOfSightFromStandPos(BlockPos standPos, EntityLivingBase target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || standPos == null || target == null) {
            return false;
        }

        Vec3d eyePos = new Vec3d(standPos).addVector(0.5D, 1.62D, 0.5D);
        Vec3d targetEye = new Vec3d(target.posX, target.posY + target.getEyeHeight() * 0.85D, target.posZ);
        RayTraceResult ray = mc.world.rayTraceBlocks(eyePos, targetEye, false, true, false);
        return ray == null || ray.typeOfHit != RayTraceResult.Type.BLOCK;
    }

    private double centerDistSq(double leftX, double leftZ, double rightX, double rightZ) {
        double dx = leftX - rightX;
        double dz = leftZ - rightZ;
        return dx * dx + dz * dz;
    }

    private double wrapOrbitAngle(double angle) {
        double wrapped = angle;
        while (wrapped <= -Math.PI) {
            wrapped += Math.PI * 2.0D;
        }
        while (wrapped > Math.PI) {
            wrapped -= Math.PI * 2.0D;
        }
        return wrapped;
    }

    private boolean isStandableHuntFeetPos(BlockPos standPos) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || standPos == null) {
            return false;
        }

        IBlockState feetState = mc.world.getBlockState(standPos);
        IBlockState headState = mc.world.getBlockState(standPos.up());
        IBlockState belowState = mc.world.getBlockState(standPos.down());

        boolean feetPassable = !feetState.getMaterial().blocksMovement();
        boolean headPassable = !headState.getMaterial().blocksMovement();
        boolean hasGround = hasStandableTopSurface(mc.world, standPos.down(), belowState);
        return feetPassable && headPassable && hasGround;
    }

    private boolean hasStandableTopSurface(net.minecraft.world.World world, BlockPos supportPos, IBlockState supportState) {
        if (world == null || supportPos == null || supportState == null || !supportState.getMaterial().blocksMovement()) {
            return false;
        }
        try {
            if (supportState.isSideSolid(world, supportPos, EnumFacing.UP)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        net.minecraft.util.math.AxisAlignedBB collisionBox = supportState.getCollisionBoundingBox(world, supportPos);
        return collisionBox != null
                && collisionBox != net.minecraft.block.Block.NULL_AABB
                && collisionBox.maxY >= 1.0D - 1.0E-4D;
    }

    private int getPreferredAttackHotbarSlot(EntityPlayerSP player) {
        if (player == null) {
            return -1;
        }
        return isHoldingWeapon(player) ? player.inventory.currentItem : -1;
    }

    private boolean isHoldingWeapon(EntityPlayerSP player) {
        if (player == null || player.getHeldItemMainhand().isEmpty()) {
            return false;
        }
        return player.getHeldItemMainhand().getItem() instanceof ItemSword
                || player.getHeldItemMainhand().getItem() instanceof ItemAxe;
    }

    private void applyFullBright(boolean active) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null) {
            return;
        }

        if (active) {
            if (!this.fullBrightApplied) {
                this.previousGammaSetting = mc.gameSettings.gammaSetting;
                this.fullBrightApplied = true;
            }
            float targetGamma = Math.max(1.0F, fullBrightGamma);
            if (mc.gameSettings.gammaSetting != targetGamma) {
                mc.gameSettings.gammaSetting = targetGamma;
            }
        } else {
            restoreFullBright();
        }
    }

    private void restoreFullBright() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!this.fullBrightApplied) {
            return;
        }
        if (mc != null && mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = this.previousGammaSetting;
        }
        this.fullBrightApplied = false;
    }

    public void applyMovementProtection(EntityPlayerSP player, boolean active, boolean applyNoCollision,
            boolean applyAntiKnockback) {
        if (player == null) {
            return;
        }

        if (!active) {
            player.entityCollisionReduction = 0.0F;
            player.noClip = false;
            this.lastSafeMotionX = 0.0D;
            this.lastSafeMotionY = 0.0D;
            this.lastSafeMotionZ = 0.0D;
            return;
        }

        if (applyNoCollision) {
            player.entityCollisionReduction = 1.0F;
            player.noClip = false;
        } else {
            player.entityCollisionReduction = 0.0F;
            player.noClip = false;
        }

        if (applyAntiKnockback && player.hurtTime > 0) {
            boolean hasMoveInput = player.movementInput != null && (Math.abs(player.movementInput.moveForward) > 0.01F
                    || Math.abs(player.movementInput.moveStrafe) > 0.01F || player.movementInput.jump
                    || player.movementInput.sneak);
            boolean jumpPressed = player.movementInput != null && player.movementInput.jump;

            if (!hasMoveInput) {
                player.motionX = 0.0D;
                player.motionZ = 0.0D;
                player.velocityChanged = true;
            } else {
                double preservedSpeed = Math.sqrt(this.lastSafeMotionX * this.lastSafeMotionX
                        + this.lastSafeMotionZ * this.lastSafeMotionZ);
                double[] preservedMotion = resolveProtectionMotion(player, preservedSpeed);
                player.motionX = preservedMotion[0];
                player.motionZ = preservedMotion[1];
                player.velocityChanged = true;
            }

            if (!jumpPressed && player.motionY > 0.0D) {
                player.motionY = Math.min(0.0D, this.lastSafeMotionY);
                player.velocityChanged = true;
            }
        } else {
            this.lastSafeMotionX = player.motionX;
            this.lastSafeMotionY = player.motionY;
            this.lastSafeMotionZ = player.motionZ;
        }
    }

    private void applyKillAuraOwnMovementProtection(EntityPlayerSP player, boolean active, boolean applyNoCollision,
            boolean applyAntiKnockback) {
        if (player == null) {
            return;
        }

        if (!active) {
            player.entityCollisionReduction = 0.0F;
            player.noClip = false;
            this.lastSafeMotionX = 0.0D;
            this.lastSafeMotionY = 0.0D;
            this.lastSafeMotionZ = 0.0D;
            return;
        }

        if (applyNoCollision) {
            player.entityCollisionReduction = 1.0F;
            player.noClip = false;
        } else {
            player.entityCollisionReduction = 0.0F;
            player.noClip = false;
        }

        if (applyAntiKnockback && player.hurtTime > 0) {
            boolean hasMoveInput = player.movementInput != null && (Math.abs(player.movementInput.moveForward) > 0.01F
                    || Math.abs(player.movementInput.moveStrafe) > 0.01F || player.movementInput.jump
                    || player.movementInput.sneak);
            boolean jumpPressed = player.movementInput != null && player.movementInput.jump;

            if (!hasMoveInput) {
                player.motionX = 0.0D;
                player.motionZ = 0.0D;
                player.velocityChanged = true;
            } else {
                double preservedSpeed = Math.sqrt(this.lastSafeMotionX * this.lastSafeMotionX
                        + this.lastSafeMotionZ * this.lastSafeMotionZ);
                double[] preservedMotion = resolveProtectionMotion(player, preservedSpeed);
                player.motionX = preservedMotion[0];
                player.motionZ = preservedMotion[1];
                player.velocityChanged = true;
            }

            if (!jumpPressed && player.motionY > 0.0D) {
                player.motionY = Math.min(0.0D, this.lastSafeMotionY);
                player.velocityChanged = true;
            }
        } else {
            this.lastSafeMotionX = player.motionX;
            this.lastSafeMotionY = player.motionY;
            this.lastSafeMotionZ = player.motionZ;
        }
    }

    private double[] resolveProtectionMotion(EntityPlayerSP player, double speed) {
        if (player == null) {
            return new double[] { 0.0D, 0.0D };
        }
        if (speed <= 1.0E-4D) {
            return new double[] { 0.0D, 0.0D };
        }

        float forward = player.movementInput == null ? 0.0F : player.movementInput.moveForward;
        float strafe = player.movementInput == null ? 0.0F : player.movementInput.moveStrafe;
        float yaw = player.rotationYaw;

        if (Math.abs(forward) < 0.01F && Math.abs(strafe) < 0.01F) {
            return new double[] { this.lastSafeMotionX, this.lastSafeMotionZ };
        }

        if (forward != 0.0F) {
            if (strafe > 0.0F) {
                yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (strafe < 0.0F) {
                yaw += forward > 0.0F ? 45.0F : -45.0F;
            }
            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        if (strafe > 0.0F) {
            strafe = 1.0F;
        } else if (strafe < 0.0F) {
            strafe = -1.0F;
        }

        double rad = Math.toRadians(yaw + 90.0F);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double motionX = (forward * cos + strafe * sin) * speed;
        double motionZ = (forward * sin - strafe * cos) * speed;
        return new double[] { motionX, motionZ };
    }

    public static List<String> getNearbyEntityNames(float scanRange) {
        List<String> result = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.player;
        if (player == null || mc.world == null) {
            return result;
        }

        float actualRange = MathHelper.clamp(scanRange, 1.0F, 64.0F);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (Entity entity : mc.world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == player || entity instanceof EntityArmorStand) {
                continue;
            }
            if (player.getDistance(entity) > actualRange) {
                continue;
            }
            String name = getFilterableEntityName(entity);
            if (!name.isEmpty()) {
                unique.add(name);
            }
        }

        result.addAll(unique);
        result.sort((a, b) -> a.compareToIgnoreCase(b));
        return result;
    }

    public static String normalizeFilterName(String rawName) {
        String stripped = TextFormatting.getTextWithoutFormattingCodes(rawName);
        String source = stripped == null ? (rawName == null ? "" : rawName) : stripped;
        if (source.isEmpty()) {
            return "";
        }

        StringBuilder visible = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (Character.isISOControl(ch) || Character.getType(ch) == Character.FORMAT) {
                continue;
            }
            visible.append(ch);
        }
        return trimUnicodeWhitespace(visible.toString());
    }

    private static String getFilterableEntityName(Entity entity) {
        if (entity == null) {
            return "";
        }

        String displayName = entity.getDisplayName() == null ? "" : entity.getDisplayName().getUnformattedText();
        String normalized = normalizeFilterName(displayName);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return normalizeFilterName(entity.getName());
    }

    private static String trimUnicodeWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int start = 0;
        int end = text.length();
        while (start < end && isIgnorableNameBoundary(text.charAt(start))) {
            start++;
        }
        while (end > start && isIgnorableNameBoundary(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    private static boolean isIgnorableNameBoundary(char ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch) || Character.isISOControl(ch)
                || Character.getType(ch) == Character.FORMAT;
    }

    private static boolean matchesNameList(String entityName, List<String> filters) {
        return getNameListMatchIndex(entityName, filters) != Integer.MAX_VALUE;
    }

    public static int getNameListMatchIndex(String entityName, List<String> filters) {
        String loweredName = normalizeFilterName(entityName).toLowerCase(Locale.ROOT);
        if (loweredName.isEmpty() || filters == null || filters.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return getNormalizedNameListMatchIndex(loweredName, filters);
    }

    private static int getNormalizedNameListMatchIndex(String loweredName, List<String> filters) {
        if (loweredName == null || loweredName.isEmpty() || filters == null || filters.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        for (int i = 0; i < filters.size(); i++) {
            String keyword = filters.get(i);
            if (keyword == null || keyword.isEmpty()) {
                continue;
            }
            if (!isFilterKeywordNormalized(keyword)) {
                keyword = normalizeNameFilterKeyword(keyword);
            }
            if (!keyword.isEmpty() && loweredName.contains(keyword)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static List<String> normalizeNameList(List<String> source) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (source != null) {
            for (String entry : source) {
                String normalized = normalizeNameFilterKeyword(entry);
                if (!normalized.isEmpty()) {
                    unique.add(normalized);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static String normalizeNameFilterKeyword(String entry) {
        return normalizeFilterName(entry).toLowerCase(Locale.ROOT);
    }

    private static boolean isFilterKeywordNormalized(String keyword) {
        for (int i = 0; i < keyword.length(); i++) {
            char ch = keyword.charAt(i);
            if (Character.isUpperCase(ch)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeHuntModeValue(String mode) {
        String normalizedMode = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (HUNT_MODE_FIXED_DISTANCE.equals(normalizedMode)) {
            return HUNT_MODE_FIXED_DISTANCE;
        }
        if (HUNT_MODE_OFF.equals(normalizedMode)) {
            return HUNT_MODE_OFF;
        }
        return HUNT_MODE_APPROACH;
    }

    private static String normalizePresetName(String name) {
        return name == null ? "" : name.trim();
    }

    private static int findPresetIndex(String name) {
        String normalizedName = normalizePresetName(name);
        if (normalizedName.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < presets.size(); i++) {
            KillAuraPreset preset = presets.get(i);
            if (preset != null && normalizedName.equalsIgnoreCase(normalizePresetName(preset.name))) {
                return i;
            }
        }
        return -1;
    }

    private static KillAuraPreset captureCurrentAsPreset(String name) {
        KillAuraPreset preset = new KillAuraPreset();
        preset.name = normalizePresetName(name);
        preset.rotateToTarget = rotateToTarget;
        preset.smoothRotation = smoothRotation;
        preset.smoothMaxTurnStep = smoothMaxTurnStep;
        preset.smoothMaxTurnStepSpec = smoothMaxTurnStepSpec;
        preset.rotateOnlyOnAttack = rotateOnlyOnAttack;
        preset.relockOnlyWhenNoCrosshairTarget = relockOnlyWhenNoCrosshairTarget;
        preset.onlyAttackWhenLookingAtTarget = onlyAttackWhenLookingAtTarget;
        preset.requireLineOfSight = requireLineOfSight;
        preset.targetHostile = targetHostile;
        preset.targetPassive = targetPassive;
        preset.targetPlayers = targetPlayers;
        preset.onlyWeapon = onlyWeapon;
        preset.aimOnlyMode = aimOnlyMode;
        preset.focusSingleTarget = focusSingleTarget;
        preset.ignoreInvisible = ignoreInvisible;
        preset.enableNoCollision = enableNoCollision;
        preset.enableAntiKnockback = enableAntiKnockback;
        preset.enableFullBrightVision = enableFullBrightVision;
        preset.fullBrightGamma = fullBrightGamma;
        preset.attackMode = attackMode;
        preset.attackSequenceName = attackSequenceName;
        preset.attackSequenceDelayTicks = attackSequenceDelayTicks;
        preset.attackSequenceDelayTicksSpec = attackSequenceDelayTicksSpec;
        preset.aimYawOffset = aimYawOffset;
        preset.aimYawOffsetSpec = aimYawOffsetSpec;
        preset.aimPitchOffset = aimPitchOffset;
        preset.aimPitchOffsetSpec = aimPitchOffsetSpec;
        preset.huntMode = huntMode;
        preset.huntPickupItemsEnabled = huntPickupItemsEnabled;
        preset.visualizeHuntRadius = visualizeHuntRadius;
        preset.huntRadius = huntRadius;
        preset.huntFixedDistance = huntFixedDistance;
        preset.huntUpRange = huntUpRange;
        preset.huntDownRange = huntDownRange;
        preset.huntOrbitEnabled = huntOrbitEnabled;
        preset.huntJumpOrbitEnabled = huntJumpOrbitEnabled;
        preset.huntOrbitSamplePoints = huntOrbitSamplePoints;
        preset.enableNameWhitelist = enableNameWhitelist;
        preset.enableNameBlacklist = enableNameBlacklist;
        preset.nameWhitelist = new ArrayList<>(nameWhitelist == null ? new ArrayList<>() : nameWhitelist);
        preset.nameBlacklist = new ArrayList<>(nameBlacklist == null ? new ArrayList<>() : nameBlacklist);
        preset.nearbyEntityScanRange = nearbyEntityScanRange;
        preset.attackRange = attackRange;
        preset.minAttackStrength = minAttackStrength;
        preset.minTurnSpeed = minTurnSpeed;
        preset.maxTurnSpeed = maxTurnSpeed;
        preset.minAttackIntervalTicks = minAttackIntervalTicks;
        preset.targetsPerAttack = targetsPerAttack;
        preset.noDamageAttackLimit = noDamageAttackLimit;
        return normalizePreset(preset);
    }

    private static KillAuraPreset normalizePreset(KillAuraPreset preset) {
        if (preset == null) {
            return null;
        }
        String normalizedName = normalizePresetName(preset.name);
        if (normalizedName.isEmpty()) {
            return null;
        }
        KillAuraPreset normalizedPreset = new KillAuraPreset(preset);
        normalizedPreset.name = normalizedName;
        normalizedPreset.rotateOnlyOnAttack = normalizedPreset.rotateOnlyOnAttack && normalizedPreset.rotateToTarget;
        normalizedPreset.relockOnlyWhenNoCrosshairTarget = normalizedPreset.relockOnlyWhenNoCrosshairTarget
                && (normalizedPreset.rotateToTarget || normalizedPreset.aimOnlyMode);
        normalizedPreset.nameWhitelist = normalizeNameList(normalizedPreset.nameWhitelist);
        normalizedPreset.nameBlacklist = normalizeNameList(normalizedPreset.nameBlacklist);
        normalizedPreset.attackSequenceName = normalizedPreset.attackSequenceName == null
                ? ""
                : normalizedPreset.attackSequenceName.trim();
        normalizedPreset.fullBrightGamma = MathHelper.clamp(normalizedPreset.fullBrightGamma, 1.0F, 1000.0F);
        normalizedPreset.attackRange = MathHelper.clamp(normalizedPreset.attackRange, 1.0F, 100.0F);
        normalizedPreset.minAttackStrength = MathHelper.clamp(normalizedPreset.minAttackStrength, 0.0F, 1.0F);
        SmoothTurnStepRange presetTurnRange = parseSmoothMaxTurnStepSpec(normalizedPreset.smoothMaxTurnStepSpec,
                normalizedPreset.smoothMaxTurnStep);
        normalizedPreset.smoothMaxTurnStep = presetTurnRange.min;
        normalizedPreset.smoothMaxTurnStepSpec = presetTurnRange.toSpec();
        normalizedPreset.minTurnSpeed = MathHelper.clamp(normalizedPreset.minTurnSpeed, 1.0F, 40.0F);
        normalizedPreset.maxTurnSpeed = MathHelper.clamp(normalizedPreset.maxTurnSpeed,
                normalizedPreset.minTurnSpeed, 60.0F);
        normalizedPreset.minAttackIntervalTicks = MathHelper.clamp(normalizedPreset.minAttackIntervalTicks, 0, 20);
        normalizedPreset.targetsPerAttack = MathHelper.clamp(normalizedPreset.targetsPerAttack, 1, 50);
        normalizedPreset.noDamageAttackLimit = MathHelper.clamp(normalizedPreset.noDamageAttackLimit, 0,
                MAX_NO_DAMAGE_ATTACK_LIMIT);
        TickRangeSpec.Range attackSequenceDelayRange = TickRangeSpec.parse(normalizedPreset.attackSequenceDelayTicksSpec,
                normalizedPreset.attackSequenceDelayTicks, MIN_ATTACK_SEQUENCE_DELAY_TICKS,
                MAX_ATTACK_SEQUENCE_DELAY_TICKS);
        normalizedPreset.attackSequenceDelayTicks = attackSequenceDelayRange.getMin();
        normalizedPreset.attackSequenceDelayTicksSpec = attackSequenceDelayRange.toSpec();
        AimOffsetRange presetYawOffsetRange = parseAimOffsetSpec(normalizedPreset.aimYawOffsetSpec,
                normalizedPreset.aimYawOffset);
        normalizedPreset.aimYawOffset = presetYawOffsetRange.min;
        normalizedPreset.aimYawOffsetSpec = presetYawOffsetRange.toSpec();
        AimOffsetRange presetPitchOffsetRange = parseAimOffsetSpec(normalizedPreset.aimPitchOffsetSpec,
                normalizedPreset.aimPitchOffset);
        normalizedPreset.aimPitchOffset = presetPitchOffsetRange.min;
        normalizedPreset.aimPitchOffsetSpec = presetPitchOffsetRange.toSpec();
        normalizedPreset.huntRadius = MathHelper.clamp(normalizedPreset.huntRadius, normalizedPreset.attackRange, 100.0F);
        normalizedPreset.huntFixedDistance = MathHelper.clamp(normalizedPreset.huntFixedDistance, 0.5F, 100.0F);
        normalizedPreset.huntUpRange = MathHelper.clamp(normalizedPreset.huntUpRange, 0.0F, 100.0F);
        normalizedPreset.huntDownRange = MathHelper.clamp(normalizedPreset.huntDownRange, 0.0F, 100.0F);
        normalizedPreset.huntOrbitSamplePoints = MathHelper.clamp(normalizedPreset.huntOrbitSamplePoints,
                MIN_HUNT_ORBIT_SAMPLE_POINTS, MAX_HUNT_ORBIT_SAMPLE_POINTS);
        normalizedPreset.nearbyEntityScanRange = MathHelper.clamp(normalizedPreset.nearbyEntityScanRange, 1.0F, 64.0F);

        String normalizedAttackMode = normalizedPreset.attackMode == null ? "" : normalizedPreset.attackMode.trim().toUpperCase(Locale.ROOT);
        if (ATTACK_MODE_PACKET.equals(normalizedAttackMode)) {
            normalizedPreset.attackMode = ATTACK_MODE_PACKET;
        } else if (ATTACK_MODE_TELEPORT.equals(normalizedAttackMode)) {
            normalizedPreset.attackMode = ATTACK_MODE_TELEPORT;
        } else if (ATTACK_MODE_SEQUENCE.equals(normalizedAttackMode)) {
            normalizedPreset.attackMode = ATTACK_MODE_SEQUENCE;
        } else if (ATTACK_MODE_MOUSE_CLICK.equals(normalizedAttackMode)) {
            normalizedPreset.attackMode = ATTACK_MODE_MOUSE_CLICK;
        } else {
            normalizedPreset.attackMode = ATTACK_MODE_NORMAL;
        }
        normalizedPreset.huntMode = normalizeHuntModeValue(normalizedPreset.huntMode);
        if (normalizedPreset.aimOnlyMode) {
            normalizedPreset.attackMode = ATTACK_MODE_SEQUENCE;
            normalizedPreset.rotateOnlyOnAttack = false;
        } else if (ATTACK_MODE_PACKET.equals(normalizedPreset.attackMode)) {
            normalizedPreset.rotateToTarget = false;
            normalizedPreset.smoothRotation = false;
            normalizedPreset.rotateOnlyOnAttack = false;
            normalizedPreset.relockOnlyWhenNoCrosshairTarget = false;
        }
        if (!normalizedPreset.targetHostile && !normalizedPreset.targetPassive && !normalizedPreset.targetPlayers) {
            normalizedPreset.targetHostile = true;
        }
        if (HUNT_MODE_OFF.equals(normalizedPreset.huntMode)) {
            normalizedPreset.visualizeHuntRadius = false;
        }
        return normalizedPreset;
    }

    private static void normalizeConfig() {
        attackRange = MathHelper.clamp(attackRange, 1.0F, 100.0F);
        minAttackStrength = MathHelper.clamp(minAttackStrength, 0.0F, 1.0F);
        SmoothTurnStepRange turnRange = parseSmoothMaxTurnStepSpec(smoothMaxTurnStepSpec, smoothMaxTurnStep);
        smoothMaxTurnStep = turnRange.min;
        smoothMaxTurnStepSpec = turnRange.toSpec();
        minTurnSpeed = MathHelper.clamp(minTurnSpeed, 1.0F, 40.0F);
        maxTurnSpeed = MathHelper.clamp(maxTurnSpeed, minTurnSpeed, 60.0F);
        minAttackIntervalTicks = MathHelper.clamp(minAttackIntervalTicks, 0, 20);
        targetsPerAttack = MathHelper.clamp(targetsPerAttack, 1, 50);
        noDamageAttackLimit = MathHelper.clamp(noDamageAttackLimit, 0, MAX_NO_DAMAGE_ATTACK_LIMIT);
        TickRangeSpec.Range attackSequenceDelayRange = TickRangeSpec.parse(attackSequenceDelayTicksSpec,
                attackSequenceDelayTicks, MIN_ATTACK_SEQUENCE_DELAY_TICKS, MAX_ATTACK_SEQUENCE_DELAY_TICKS);
        attackSequenceDelayTicks = attackSequenceDelayRange.getMin();
        attackSequenceDelayTicksSpec = attackSequenceDelayRange.toSpec();
        AimOffsetRange yawOffsetRange = parseAimOffsetSpec(aimYawOffsetSpec, aimYawOffset);
        aimYawOffset = yawOffsetRange.min;
        aimYawOffsetSpec = yawOffsetRange.toSpec();
        AimOffsetRange pitchOffsetRange = parseAimOffsetSpec(aimPitchOffsetSpec, aimPitchOffset);
        aimPitchOffset = pitchOffsetRange.min;
        aimPitchOffsetSpec = pitchOffsetRange.toSpec();
        huntRadius = MathHelper.clamp(huntRadius, attackRange, 100.0F);
        huntFixedDistance = MathHelper.clamp(huntFixedDistance, 0.5F, 100.0F);
        huntUpRange = MathHelper.clamp(huntUpRange, 0.0F, 100.0F);
        huntDownRange = MathHelper.clamp(huntDownRange, 0.0F, 100.0F);
        huntOrbitSamplePoints = MathHelper.clamp(huntOrbitSamplePoints,
                MIN_HUNT_ORBIT_SAMPLE_POINTS, MAX_HUNT_ORBIT_SAMPLE_POINTS);
        fullBrightGamma = MathHelper.clamp(fullBrightGamma, 1.0F, 1000.0F);
        nearbyEntityScanRange = MathHelper.clamp(nearbyEntityScanRange, 1.0F, 64.0F);
        nameWhitelist = normalizeNameList(nameWhitelist);
        nameBlacklist = normalizeNameList(nameBlacklist);
        attackSequenceName = getConfiguredAttackSequenceName();

        String normalizedAttackMode = attackMode == null ? "" : attackMode.trim().toUpperCase(Locale.ROOT);
        if (ATTACK_MODE_PACKET.equals(normalizedAttackMode)) {
            attackMode = ATTACK_MODE_PACKET;
        } else if (ATTACK_MODE_TELEPORT.equals(normalizedAttackMode)) {
            attackMode = ATTACK_MODE_TELEPORT;
        } else if (ATTACK_MODE_SEQUENCE.equals(normalizedAttackMode)) {
            attackMode = ATTACK_MODE_SEQUENCE;
        } else if (ATTACK_MODE_MOUSE_CLICK.equals(normalizedAttackMode)) {
            attackMode = ATTACK_MODE_MOUSE_CLICK;
        } else {
            attackMode = ATTACK_MODE_NORMAL;
        }

        if (aimOnlyMode) {
            attackMode = ATTACK_MODE_SEQUENCE;
            rotateOnlyOnAttack = false;
        } else if (ATTACK_MODE_PACKET.equals(attackMode)) {
            rotateToTarget = false;
            smoothRotation = false;
            rotateOnlyOnAttack = false;
            relockOnlyWhenNoCrosshairTarget = false;
        }
        rotateOnlyOnAttack = rotateOnlyOnAttack && rotateToTarget;
        relockOnlyWhenNoCrosshairTarget = relockOnlyWhenNoCrosshairTarget && (aimOnlyMode || rotateToTarget);

        huntMode = normalizeHuntModeValue(huntMode);
        huntEnabled = !HUNT_MODE_OFF.equals(huntMode);
        if (!huntEnabled) {
            visualizeHuntRadius = false;
        }

        if (!targetHostile && !targetPassive && !targetPlayers) {
            targetHostile = true;
        }
    }

    private double getEffectiveHuntFixedDistance() {
        return Math.max(0.5D, huntFixedDistance);
    }

    public static boolean isHuntOrbitEnabled() {
        return isHuntFixedDistanceMode() && huntOrbitEnabled;
    }

    public static int getConfiguredHuntOrbitSamplePoints() {
        return MathHelper.clamp(huntOrbitSamplePoints, MIN_HUNT_ORBIT_SAMPLE_POINTS, MAX_HUNT_ORBIT_SAMPLE_POINTS);
    }

    public static boolean isHuntOrbitSampleCountAtMaximum() {
        return getConfiguredHuntOrbitSamplePoints() >= MAX_HUNT_ORBIT_SAMPLE_POINTS;
    }

    public boolean shouldKeepRunningDuringGui(Minecraft mc) {
        if (mc == null || mc.player == null || mc.world == null || !enabled || !isHuntOrbitEnabled()) {
            return false;
        }
        return this.huntOrbitController.isActive() && hasActiveTarget(mc.player);
    }

    private static final class TeleportAssaultCandidate {
        private final double x;
        private final double y;
        private final double z;
        private final boolean usedSafeStandPos;
        private final double score;

        private TeleportAssaultCandidate(double x, double y, double z, boolean usedSafeStandPos, double score) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.usedSafeStandPos = usedSafeStandPos;
            this.score = score;
        }
    }

    private static final class TeleportAttackPlan {
        private final int targetEntityId;
        private final int createdTick;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final float originYaw;
        private final float originPitch;
        private final boolean originOnGround;
        private final double assaultX;
        private final double assaultY;
        private final double assaultZ;
        private final float attackYaw;
        private final float attackPitch;
        private final boolean usedSafeAssaultPos;
        private final List<Vec3d> outboundWaypoints;
        private final List<Vec3d> returnWaypoints;
        private boolean correctedByServer;
        private boolean returnCompleted;
        private int correctionCount;

        private TeleportAttackPlan(EntityPlayerSP player, EntityLivingBase target, TeleportAssaultCandidate assaultCandidate,
                List<Vec3d> outboundWaypoints, List<Vec3d> returnWaypoints, float attackYaw, float attackPitch) {
            this.targetEntityId = target == null ? Integer.MIN_VALUE : target.getEntityId();
            this.createdTick = player == null ? -1 : player.ticksExisted;
            this.originX = player == null ? 0.0D : player.posX;
            this.originY = player == null ? 0.0D : player.posY;
            this.originZ = player == null ? 0.0D : player.posZ;
            this.originYaw = player == null ? 0.0F : player.rotationYaw;
            this.originPitch = player == null ? 0.0F : player.rotationPitch;
            this.originOnGround = player != null && player.onGround;
            this.assaultX = assaultCandidate == null ? this.originX : assaultCandidate.x;
            this.assaultY = assaultCandidate == null ? this.originY : assaultCandidate.y;
            this.assaultZ = assaultCandidate == null ? this.originZ : assaultCandidate.z;
            this.attackYaw = attackYaw;
            this.attackPitch = attackPitch;
            this.usedSafeAssaultPos = assaultCandidate != null && assaultCandidate.usedSafeStandPos;
            this.outboundWaypoints = outboundWaypoints == null ? new ArrayList<>() : new ArrayList<>(outboundWaypoints);
            this.returnWaypoints = returnWaypoints == null ? new ArrayList<>() : new ArrayList<>(returnWaypoints);
            this.correctedByServer = false;
            this.returnCompleted = false;
            this.correctionCount = 0;
        }
    }

    private static final class AttackSequenceExecutor {
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
                    zszlScriptMod.LOGGER.error("[kill_aura_sequence] 执行动作失败: {}", resolvedAction.getDescription(), e);
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
                    || "restart_sequence".equals(actionType)
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
                    zszlScriptMod.LOGGER.warn("[kill_aura_sequence] 释放按键失败: {}", entry.getKey(), e);
                }
            }
        }
    }
}
