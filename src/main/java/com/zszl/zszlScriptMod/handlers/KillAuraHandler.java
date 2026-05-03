package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.path.ActionVariableRegistry;
import com.zszl.zszlScriptMod.path.LegacyActionRuntime;
import com.zszl.zszlScriptMod.path.ActionVariableRegistry;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.PathEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.PacketEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.type.EventState;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.listener.AbstractGameEventListener;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.listener.IEventBus;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.Rotation;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.RotationUtils;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    public static final int MIN_HUNT_ORBIT_SAMPLE_POINTS = 3;
    public static final int MAX_HUNT_ORBIT_SAMPLE_POINTS = 360;
    public static final int DEFAULT_HUNT_ORBIT_SAMPLE_POINTS = MAX_HUNT_ORBIT_SAMPLE_POINTS;

    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private static final Type PRESET_LIST_TYPE = new TypeToken<List<KillAuraPreset>>() {
    }.getType();
    private static Field collisionReductionField;

    private static final int HUNT_GOTO_INTERVAL_TICKS = 6;
    private static final double HUNT_GOTO_MOVE_THRESHOLD_SQ = 1.0D;
    private static final double HUNT_FIXED_DISTANCE_TOLERANCE = 0.30D;
    private static final int HUNT_UNREACHABLE_MAX_TRACKED_TARGETS = 256;
    private static final int HUNT_UNREACHABLE_MAX_FAILED_GOALS_PER_TARGET = 16;
    private static final int HUNT_UNREACHABLE_FAILS_BEFORE_TEMP_EXCLUDE = 3;
    private static final int HUNT_UNREACHABLE_TEMP_EXCLUDE_TICKS = 100;
    private static final double HUNT_UNREACHABLE_TARGET_RESET_DISTANCE_SQ = 4.0D;
    private static final int HUNT_PICKUP_GOTO_INTERVAL_TICKS = 5;
    private static final int HUNT_PICKUP_SEARCH_INTERVAL_TICKS = 3;
    private static final double HUNT_PICKUP_OVERLAP_GROWTH = 0.05D;
    private static final double HUNT_APPROACH_MIN_STAND_RADIUS = 0.85D;
    private static final double HUNT_APPROACH_TARGET_BUFFER = 0.35D;
    private static final double HUNT_NAVIGATION_RADIUS_SAMPLE_STEP = 0.75D;
    private static final double HUNT_NAVIGATION_ANGLE_SAMPLE_STEP_RADIANS = Math.toRadians(18.0D);
    private static final int HUNT_NAVIGATION_ANGLE_SAMPLE_PAIRS = 10;
    private static final double HUNT_ORBIT_MAX_ENTRY_VERTICAL_DELTA = 3.5D;
    private static final double HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER = 1.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER = 2.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_LOOP_ENTRY_MAX_DISTANCE = 0.90D;
    private static final double HUNT_ORBIT_ENTRY_RADIUS_BAND = 0.45D;
    private static final int HUNT_ORBIT_ENTRY_SAFE_SEARCH_RADIUS = 1;
    private static final double HUNT_ORBIT_ENTRY_POINT_TOLERANCE = 0.85D;

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

    public static boolean enabled = false;
    public static boolean rotateToTarget = true;
    public static boolean smoothRotation = true;
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
    public static int attackSequenceDelayTicks = 2;
    public static float aimYawOffset = 0.0F;
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

    private int attackCooldownTicks = 0;
    private int sequenceCooldownTicks = 0;
    private int currentTargetEntityId = -1;
    private boolean huntNavigationActive = false;
    private int lastHuntGotoTick = -99999;
    private int lastHuntTargetEntityId = Integer.MIN_VALUE;
    private double lastHuntTargetX = 0.0D;
    private double lastHuntTargetZ = 0.0D;
    private double lastHuntGoalX = Double.NaN;
    private double lastHuntGoalY = Double.NaN;
    private double lastHuntGoalZ = Double.NaN;
    private boolean huntPickupNavigationActive = false;
    private int lastHuntPickupGotoTick = -99999;
    private int lastHuntPickupTargetEntityId = Integer.MIN_VALUE;
    private int lastHuntPickupSearchTick = -99999;
    private int lastHuntPickupSearchTargetEntityId = Integer.MIN_VALUE;
    private boolean lastHuntPickupSearchFound = false;
    private double lastSafeMotionX = 0.0D;
    private double lastSafeMotionY = 0.0D;
    private double lastSafeMotionZ = 0.0D;
    private IEventBus registeredBaritoneEventBus = null;
    private TeleportAttackPlan activeTeleportAttackPlan = null;
    private int pendingTeleportReturnTicks = 0;
    private int lastTeleportCorrectionTick = Integer.MIN_VALUE;
    private final AttackSequenceExecutor attackSequenceExecutor = new AttackSequenceExecutor();
    private final HuntOrbitController huntOrbitController = new HuntOrbitController();
    private final Map<Integer, HuntUnreachableTracker> huntUnreachableTrackers = new LinkedHashMap<>();
    private String lastOrbitDebugState = "";

    private static final class HuntUnreachableTracker {
        private double lastTargetX;
        private double lastTargetY;
        private double lastTargetZ;
        private int failedGoalCount;
        private int excludeUntilTick;
        private final LinkedHashSet<Long> failedGoalKeys = new LinkedHashSet<>();

        private HuntUnreachableTracker(LivingEntity target) {
            refreshTargetPosition(target);
        }

        private void refreshTargetPosition(LivingEntity target) {
            if (target == null) {
                return;
            }
            this.lastTargetX = target.getX();
            this.lastTargetY = target.getY();
            this.lastTargetZ = target.getZ();
        }

        private void resetFailures(LivingEntity target) {
            refreshTargetPosition(target);
            this.failedGoalCount = 0;
            this.excludeUntilTick = 0;
            this.failedGoalKeys.clear();
        }
    }

    public static class KillAuraPreset {
        public String name = "";
        public boolean rotateToTarget = true;
        public boolean smoothRotation = true;
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
        public int attackSequenceDelayTicks = 2;
        public float aimYawOffset = 0.0F;
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
            this.attackMode = other.attackMode;
            this.attackSequenceName = other.attackSequenceName;
            this.attackSequenceDelayTicks = other.attackSequenceDelayTicks;
            this.aimYawOffset = other.aimYawOffset;
            this.huntMode = other.huntMode;
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
            this.nameWhitelist = new ArrayList<>(other.nameWhitelist);
            this.nameBlacklist = new ArrayList<>(other.nameBlacklist);
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
        private final Predicate<LivingEntity> targetFilter;

        public AreaHuntOptions(double centerX, double centerY, double centerZ, double radius,
                double upRange, double downRange, Predicate<LivingEntity> targetFilter) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = Math.max(0.0D, radius);
            this.radiusSq = this.radius * this.radius;
            this.upRange = Math.max(0.0D, upRange);
            this.downRange = Math.max(0.0D, downRange);
            this.targetFilter = targetFilter;
        }

        private boolean allows(LivingEntity target) {
            return targetFilter == null || targetFilter.test(target);
        }

        private boolean contains(Entity target) {
            if (target == null) {
                return false;
            }
            double dx = target.getX() - centerX;
            double dz = target.getZ() - centerZ;
            if (dx * dx + dz * dz > radiusSq) {
                return false;
            }
            double dy = target.getY() - centerY;
            return dy <= upRange + 1.0E-6D && -dy <= downRange + 1.0E-6D;
        }
    }

    public static final class AreaHuntTickResult {
        private final boolean hasTarget;
        private final LivingEntity target;
        private final int attackedCount;
        private final boolean sequenceRunning;

        private AreaHuntTickResult(boolean hasTarget, LivingEntity target, int attackedCount,
                boolean sequenceRunning) {
            this.hasTarget = hasTarget;
            this.target = target;
            this.attackedCount = attackedCount;
            this.sequenceRunning = sequenceRunning;
        }

        public boolean hasTarget() {
            return hasTarget;
        }

        public LivingEntity getTarget() {
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

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_killaura.json");
    }

    public static void loadConfig() {
        resetDefaults();
        Path file = getConfigFile();
        if (!Files.exists(file)) {
            normalizeConfig();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            enabled = readBoolean(json, "enabled", enabled);
            rotateToTarget = readBoolean(json, "rotateToTarget", rotateToTarget);
            smoothRotation = readBoolean(json, "smoothRotation", smoothRotation);
            onlyAttackWhenLookingAtTarget = readBoolean(json, "onlyAttackWhenLookingAtTarget",
                    onlyAttackWhenLookingAtTarget);
            requireLineOfSight = readBoolean(json, "requireLineOfSight", requireLineOfSight);
            targetHostile = readBoolean(json, "targetHostile", targetHostile);
            targetPassive = readBoolean(json, "targetPassive", targetPassive);
            targetPlayers = readBoolean(json, "targetPlayers", targetPlayers);
            onlyWeapon = readBoolean(json, "onlyWeapon", onlyWeapon);
            aimOnlyMode = readBoolean(json, "aimOnlyMode", aimOnlyMode);
            focusSingleTarget = readBoolean(json, "focusSingleTarget", focusSingleTarget);
            ignoreInvisible = readBoolean(json, "ignoreInvisible", ignoreInvisible);
            enableNoCollision = readBoolean(json, "enableNoCollision", enableNoCollision);
            enableAntiKnockback = readBoolean(json, "enableAntiKnockback", enableAntiKnockback);
            enableFullBrightVision = readBoolean(json, "enableFullBrightVision", enableFullBrightVision);
            fullBrightGamma = readFloat(json, "fullBrightGamma", fullBrightGamma);
            attackMode = readString(json, "attackMode", attackMode);
            attackSequenceName = readString(json, "attackSequenceName", attackSequenceName);
            attackSequenceDelayTicks = readInt(json, "attackSequenceDelayTicks", attackSequenceDelayTicks);
            aimYawOffset = readFloat(json, "aimYawOffset", aimYawOffset);
            huntMode = json.has("huntMode")
                    ? readString(json, "huntMode", huntMode)
                    : (readBoolean(json, "huntEnabled", true) ? HUNT_MODE_APPROACH : HUNT_MODE_OFF);
            huntPickupItemsEnabled = readBoolean(json, "huntPickupItemsEnabled", huntPickupItemsEnabled);
            visualizeHuntRadius = readBoolean(json, "visualizeHuntRadius", visualizeHuntRadius);
            huntRadius = readFloat(json, "huntRadius", huntRadius);
            huntFixedDistance = readFloat(json, "huntFixedDistance", huntFixedDistance);
            huntUpRange = readFloat(json, "huntUpRange", huntUpRange);
            huntDownRange = readFloat(json, "huntDownRange", huntDownRange);
            huntOrbitEnabled = readBoolean(json, "huntOrbitEnabled", huntOrbitEnabled);
            huntJumpOrbitEnabled = readBoolean(json, "huntJumpOrbitEnabled", huntJumpOrbitEnabled);
            huntOrbitSamplePoints = readInt(json, "huntOrbitSamplePoints", huntOrbitSamplePoints);
            enableNameWhitelist = readBoolean(json, "enableNameWhitelist", enableNameWhitelist);
            enableNameBlacklist = readBoolean(json, "enableNameBlacklist", enableNameBlacklist);
            nearbyEntityScanRange = readFloat(json, "nearbyEntityScanRange", nearbyEntityScanRange);
            attackRange = readFloat(json, "attackRange", attackRange);
            minAttackStrength = readFloat(json, "minAttackStrength", minAttackStrength);
            minTurnSpeed = readFloat(json, "minTurnSpeed", minTurnSpeed);
            maxTurnSpeed = readFloat(json, "maxTurnSpeed", maxTurnSpeed);
            minAttackIntervalTicks = readInt(json, "minAttackIntervalTicks", minAttackIntervalTicks);
            targetsPerAttack = readInt(json, "targetsPerAttack", targetsPerAttack);
            noDamageAttackLimit = readInt(json, "noDamageAttackLimit", noDamageAttackLimit);

            if (json.has("nameWhitelist") && json.get("nameWhitelist").isJsonArray()) {
                nameWhitelist = normalizeNameList(GSON.fromJson(json.get("nameWhitelist"), STRING_LIST_TYPE));
            }
            if (json.has("nameBlacklist") && json.get("nameBlacklist").isJsonArray()) {
                nameBlacklist = normalizeNameList(GSON.fromJson(json.get("nameBlacklist"), STRING_LIST_TYPE));
            }
            if (json.has("presets") && json.get("presets").isJsonArray()) {
                List<KillAuraPreset> loadedPresets = GSON.fromJson(json.get("presets"), PRESET_LIST_TYPE);
                presets.clear();
                if (loadedPresets != null) {
                    for (KillAuraPreset preset : loadedPresets) {
                        KillAuraPreset normalized = normalizePreset(preset);
                        if (normalized != null) {
                            presets.add(normalized);
                        }
                    }
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载杀戮光环配置失败", e);
        }
        normalizeConfig();
    }

    public static void saveConfig() {
        normalizeConfig();
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("enabled", enabled);
            json.addProperty("rotateToTarget", rotateToTarget);
            json.addProperty("smoothRotation", smoothRotation);
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
            json.addProperty("attackSequenceDelayTicks", attackSequenceDelayTicks);
            json.addProperty("aimYawOffset", aimYawOffset);
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

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存杀戮光环配置失败", e);
        }
    }

    public void toggleEnabled() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean targetEnabled) {
        normalizeConfig();
        if (enabled == targetEnabled) {
            saveConfig();
            return;
        }
        enabled = targetEnabled;
        resetRuntimeState();
        saveConfig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(targetEnabled ? "杀戮光环已启用" : "杀戮光环已关闭"));
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

    public static boolean isHuntOrbitEnabled() {
        return isHuntFixedDistanceMode() && huntOrbitEnabled;
    }

    public static int getConfiguredHuntOrbitSamplePoints() {
        return Mth.clamp(huntOrbitSamplePoints, MIN_HUNT_ORBIT_SAMPLE_POINTS, MAX_HUNT_ORBIT_SAMPLE_POINTS);
    }

    public static boolean isHuntOrbitSampleCountAtMaximum() {
        return getConfiguredHuntOrbitSamplePoints() >= MAX_HUNT_ORBIT_SAMPLE_POINTS;
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
            KillAuraPreset normalized = normalizePreset(preset);
            if (normalized != null) {
                snapshots.add(new KillAuraPreset(normalized));
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
        String normalizedNewName = normalizePresetName(newName);
        if (index < 0 || normalizedNewName.isEmpty()) {
            return false;
        }
        int duplicateIndex = findPresetIndex(normalizedNewName);
        if (duplicateIndex >= 0 && duplicateIndex != index) {
            return false;
        }
        presets.get(index).name = normalizedNewName;
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

    public void resetRuntimeState() {
        stopHuntPickupNavigation();
        stopHuntNavigation();
        clearOrbitDebugState();
        this.attackCooldownTicks = 0;
        this.sequenceCooldownTicks = 0;
        this.currentTargetEntityId = -1;
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
        this.lastSafeMotionX = 0.0D;
        this.lastSafeMotionY = 0.0D;
        this.lastSafeMotionZ = 0.0D;
        this.activeTeleportAttackPlan = null;
        this.pendingTeleportReturnTicks = 0;
        this.lastTeleportCorrectionTick = Integer.MIN_VALUE;
        this.attackSequenceExecutor.stop();
    }

    public boolean hasActiveTarget(LocalPlayer player) {
        if (!enabled || player == null || player.level() == null || this.currentTargetEntityId == -1) {
            return false;
        }
        Entity target = player.level().getEntity(this.currentTargetEntityId);
        return target instanceof LivingEntity livingTarget && isValidTarget(player, livingTarget);
    }

    public Optional<Rotation> getVisualTargetRotation(LocalPlayer player) {
        if (player == null || player.level() == null || !shouldRotateToTarget() || this.currentTargetEntityId == -1) {
            return Optional.empty();
        }
        Entity target = player.level().getEntity(this.currentTargetEntityId);
        if (!(target instanceof LivingEntity livingTarget) || !isValidTarget(player, livingTarget)) {
            return Optional.empty();
        }
        return Optional.of(getDesiredAimRotation(player, livingTarget));
    }

    public boolean shouldKeepRunningDuringGui(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || !enabled || !isHuntOrbitEnabled()) {
            return false;
        }
        return this.huntOrbitController.isActive() && hasActiveTarget(mc.player);
    }

    public static boolean isBrightnessOverrideActive() {
        return enabled && enableFullBrightVision;
    }

    public static float getEffectiveBrightnessGammaOverride() {
        return Math.max(1.0F, fullBrightGamma);
    }

    private void ensureBaritonePacketListenerRegistered() {
        try {
            IEventBus eventBus = BaritoneAPI.getProvider().getPrimaryBaritone() == null
                    ? null
                    : BaritoneAPI.getProvider().getPrimaryBaritone().getGameEventHandler();
            if (eventBus == null || eventBus == this.registeredBaritoneEventBus) {
                return;
            }
            eventBus.registerEventListener(this);
            this.registeredBaritoneEventBus = eventBus;
        } catch (Throwable ignored) {
        }
    }

    public void applyMovementProtection(LocalPlayer player, boolean active, boolean applyNoCollision,
            boolean applyAntiKnockback) {
        applyKillAuraOwnMovementProtection(player, active, applyNoCollision, applyAntiKnockback);
    }

    private void applyKillAuraOwnMovementProtection(LocalPlayer player, boolean active, boolean applyNoCollision,
            boolean applyAntiKnockback) {
        if (player == null) {
            return;
        }
        if (!active || (!applyNoCollision && !applyAntiKnockback)) {
            setCollisionReduction(player, 0.0F);
            this.lastSafeMotionX = 0.0D;
            this.lastSafeMotionY = 0.0D;
            this.lastSafeMotionZ = 0.0D;
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        setCollisionReduction(player, applyNoCollision ? 1.0F : 0.0F);

        if (applyAntiKnockback && player.hurtTime > 0) {
            double preservedSpeed = Math.sqrt(this.lastSafeMotionX * this.lastSafeMotionX
                    + this.lastSafeMotionZ * this.lastSafeMotionZ);
            if (preservedSpeed <= 1.0E-4D) {
                preservedSpeed = Math.max(horizontalSpeed(player), 0.21D);
            }
            double[] preservedMotion = resolveProtectionMotion(player, preservedSpeed);
            player.setDeltaMovement(preservedMotion[0], Math.min(0.0D, motion.y),
                    preservedMotion[1]);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            return;
        }

        this.lastSafeMotionX = motion.x;
        this.lastSafeMotionY = motion.y;
        this.lastSafeMotionZ = motion.z;
    }

    private static void setCollisionReduction(LocalPlayer player, float value) {
        if (player == null) {
            return;
        }
        try {
            if (collisionReductionField == null) {
                for (Class<?> type = player.getClass(); type != null && collisionReductionField == null; type = type.getSuperclass()) {
                    try {
                        collisionReductionField = type.getDeclaredField("entityCollisionReduction");
                        collisionReductionField.setAccessible(true);
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
            if (collisionReductionField != null) {
                collisionReductionField.setFloat(player, value);
            }
        } catch (Throwable ignored) {
        }
    }

    private double[] resolveProtectionMotion(LocalPlayer player, double speed) {
        Vec3 heading = getMovementHeading(player);
        if (heading.lengthSqr() < 1.0E-4D) {
            return new double[] { this.lastSafeMotionX, this.lastSafeMotionZ };
        }
        return new double[] { heading.x * speed, heading.z * speed };
    }

    public static List<String> getNearbyEntityNames(float scanRange) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.level() == null) {
            return new ArrayList<>();
        }

        double radiusSq = Math.max(1.0F, scanRange) * Math.max(1.0F, scanRange);
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(scanRange, scanRange * 0.75D, scanRange))) {
            if (entity == player || !entity.isAlive()) {
                continue;
            }
            if (player.distanceToSqr(entity) > radiusSq) {
                continue;
            }
            String name = normalizeFilterName(getFilterableEntityName(entity));
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }

    public static String normalizeFilterName(String rawName) {
        String stripped = ChatFormatting.stripFormatting(rawName == null ? "" : rawName);
        return trimUnicodeWhitespace(stripped == null ? "" : stripped);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!enabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        RenderWorldLastEvent.WorldRenderContext renderContext = event.getWorldRenderContext();
        PoseStack poseStack = event.createWorldPoseStack();
        if (player == null || mc.level == null || renderContext == null || poseStack == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        double worldCenterX = Mth.lerp(partialTicks, player.xOld, player.getX());
        double worldCenterY = Mth.lerp(partialTicks, player.yOld, player.getY()) + 0.05D;
        double worldCenterZ = Mth.lerp(partialTicks, player.zOld, player.getZ());
        if (isHuntEnabled() && visualizeHuntRadius) {
            drawHuntRadiusAura(worldCenterX, worldCenterY, worldCenterZ, huntRadius, poseStack, renderContext);
        }
        renderHuntOrbitLoop(poseStack, renderContext);
    }

    private void drawHuntRadiusAura(double worldCenterX, double worldCenterY, double worldCenterZ, double radius,
            PoseStack poseStack, RenderWorldLastEvent.WorldRenderContext renderContext) {
        double drawRadius = Math.max(0.5D, radius);
        int segments = Math.max(36, (int) Math.round(drawRadius * 10.0D));
        List<Vec3> ringLoop = buildHuntRadiusLoop(worldCenterX, worldCenterY, worldCenterZ, drawRadius, segments);
        if (ringLoop.size() < 2) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        drawLoopRingWall(buffer, poseStack, renderContext, ringLoop,
                -0.04D, 0.56D,
                0.18F, 0.78F, 1.0F, 0.12F,
                1.0F, 0.95F, 0.22F, 0.98F,
                1.0F, 0.62F, 0.10F, 0.78F,
                12);

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private List<Vec3> buildHuntRadiusLoop(double worldCenterX, double worldCenterY, double worldCenterZ,
            double radius, int segments) {
        List<Vec3> loop = new ArrayList<>();
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double[] point = getClippedHuntPoint(worldCenterX, worldCenterZ, radius, angle);
            loop.add(new Vec3(point[0], worldCenterY, point[1]));
        }
        return loop;
    }

    private void drawLoopRingWall(BufferBuilder buffer, PoseStack poseStack,
            RenderWorldLastEvent.WorldRenderContext renderContext, List<Vec3> loop,
            double bottomOffset, double topOffset,
            float fillRed, float fillGreen, float fillBlue, float fillAlpha,
            float edgeRed, float edgeGreen, float edgeBlue, float edgeAlpha,
            float accentRed, float accentGreen, float accentBlue, float accentAlpha,
            int connectorCount) {
        if (loop == null || loop.size() < 2) {
            return;
        }

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (Vec3 point : loop) {
            Vec3 lower = renderContext.toCameraSpace(new Vec3(point.x, point.y + bottomOffset, point.z));
            Vec3 upper = renderContext.toCameraSpace(new Vec3(point.x, point.y + topOffset, point.z));
            if (lower == null || upper == null) {
                continue;
            }
            buffer.vertex(poseStack.last().pose(), (float) lower.x, (float) lower.y, (float) lower.z)
                    .color(fillRed, fillGreen, fillBlue, fillAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float) upper.x, (float) upper.y, (float) upper.z)
                    .color(fillRed, fillGreen, fillBlue, Math.min(1.0F, fillAlpha + 0.06F)).endVertex();
        }
        Tesselator.getInstance().end();

        RenderSystem.lineWidth(4.0F);
        drawLoopOutline(buffer, poseStack, renderContext, loop, bottomOffset,
                edgeRed, edgeGreen, edgeBlue, edgeAlpha);
        drawLoopOutline(buffer, poseStack, renderContext, loop, topOffset,
                edgeRed, edgeGreen, edgeBlue, edgeAlpha);
        drawLoopOutline(buffer, poseStack, renderContext, loop, topOffset + 0.02D,
                accentRed, accentGreen, accentBlue, accentAlpha);
        drawLoopConnectors(buffer, poseStack, renderContext, loop, bottomOffset, topOffset,
                accentRed, accentGreen, accentBlue, Math.max(edgeAlpha, accentAlpha), connectorCount);
        RenderSystem.lineWidth(1.0F);
    }

    private void drawLoopOutline(BufferBuilder buffer, PoseStack poseStack,
            RenderWorldLastEvent.WorldRenderContext renderContext, List<Vec3> loop, double yOffset,
            float red, float green, float blue, float alpha) {
        buffer.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (Vec3 point : loop) {
            Vec3 cameraPoint = renderContext.toCameraSpace(new Vec3(point.x, point.y + yOffset, point.z));
            if (cameraPoint == null) {
                continue;
            }
            buffer.vertex(poseStack.last().pose(), (float) cameraPoint.x, (float) cameraPoint.y, (float) cameraPoint.z)
                    .color(red, green, blue, alpha).endVertex();
        }
        Tesselator.getInstance().end();
    }

    private void drawLoopConnectors(BufferBuilder buffer, PoseStack poseStack,
            RenderWorldLastEvent.WorldRenderContext renderContext, List<Vec3> loop,
            double bottomOffset, double topOffset,
            float red, float green, float blue, float alpha, int connectorCount) {
        int uniquePointCount = Math.max(1, isLoopClosed(loop) ? loop.size() - 1 : loop.size());
        int step = Math.max(1, uniquePointCount / Math.max(1, connectorCount));

        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < uniquePointCount; i += step) {
            Vec3 point = loop.get(i);
            Vec3 lower = renderContext.toCameraSpace(new Vec3(point.x, point.y + bottomOffset, point.z));
            Vec3 upper = renderContext.toCameraSpace(new Vec3(point.x, point.y + topOffset, point.z));
            if (lower == null || upper == null) {
                continue;
            }
            buffer.vertex(poseStack.last().pose(), (float) lower.x, (float) lower.y, (float) lower.z)
                    .color(red, green, blue, alpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float) upper.x, (float) upper.y, (float) upper.z)
                    .color(red, green, blue, Math.max(0.55F, alpha - 0.15F)).endVertex();
        }
        Tesselator.getInstance().end();
    }

    private boolean isLoopClosed(List<Vec3> loop) {
        if (loop == null || loop.size() < 2) {
            return false;
        }
        return loop.get(0).distanceToSqr(loop.get(loop.size() - 1)) <= 1.0E-4D;
    }

    private void renderHuntOrbitLoop(PoseStack poseStack, RenderWorldLastEvent.WorldRenderContext renderContext) {
        if (!isHuntOrbitEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || this.currentTargetEntityId == -1) {
            return;
        }

        Entity entity = mc.level.getEntity(this.currentTargetEntityId);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        List<Vec3> renderLoop = getHuntOrbitRenderLoop(target);
        if (renderLoop.size() < 2) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        drawLoopRingWall(buffer, poseStack, renderContext, renderLoop,
                -0.03D, 0.38D,
                0.18F, 1.0F, 0.55F, 0.12F,
                0.95F, 1.0F, 0.22F, 0.98F,
                1.0F, 0.62F, 0.15F, 0.82F,
                10);

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || event.player != mc.player) {
            return;
        }

        boolean flyEnabled = FlyHandler.enabled;
        boolean movementProtectionActive = enabled || flyEnabled;
        boolean useNoCollision = (enabled && enableNoCollision)
                || (flyEnabled && FlyHandler.enableNoCollision);
        boolean useAntiKnockback = (enabled && enableAntiKnockback)
                || (flyEnabled && FlyHandler.enableAntiKnockback);
        applyKillAuraOwnMovementProtection(mc.player, movementProtectionActive, useNoCollision, useAntiKnockback);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ensureBaritonePacketListenerRegistered();

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        tickHuntUnreachableTrackers(player);

        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
        }
        if (this.sequenceCooldownTicks > 0) {
            this.sequenceCooldownTicks--;
        }
        tickTeleportAttackRecovery(player);

        boolean flyEnabled = FlyHandler.enabled;
        boolean movementProtectionActive = enabled || flyEnabled;
        boolean useNoCollision = (enabled && enableNoCollision)
                || (flyEnabled && FlyHandler.enableNoCollision);
        boolean useAntiKnockback = (enabled && enableAntiKnockback)
                || (flyEnabled && FlyHandler.enableAntiKnockback);
        applyKillAuraOwnMovementProtection(player, movementProtectionActive, useNoCollision, useAntiKnockback);

        if (!enabled) {
            this.attackCooldownTicks = 0;
            this.sequenceCooldownTicks = 0;
            this.currentTargetEntityId = -1;
            stopHuntPickupNavigation();
            stopHuntNavigation();
            this.attackSequenceExecutor.stop();
            if (!movementProtectionActive) {
                this.lastSafeMotionX = 0.0D;
                this.lastSafeMotionY = 0.0D;
                this.lastSafeMotionZ = 0.0D;
            }
            clearHuntUnreachableTracking();
            return;
        }

        if (player.isDeadOrDying() || player.isSpectator()) {
            this.currentTargetEntityId = -1;
            stopHuntPickupNavigation();
            stopHuntNavigation();
            this.attackSequenceExecutor.stop();
            clearHuntUnreachableTracking();
            return;
        }

        boolean sequenceAttackMode = isSequenceAttackMode();
        if (!sequenceAttackMode && this.attackSequenceExecutor.isRunning()) {
            this.attackSequenceExecutor.stop();
        }

        if (!aimOnlyMode && !sequenceAttackMode && onlyWeapon && getPreferredAttackHotbarSlot(player) < 0) {
            this.currentTargetEntityId = -1;
            stopHuntPickupNavigation();
            stopHuntNavigation();
            return;
        }

        ItemEntity huntPriorityPickupItem = isHuntEnabled() && huntPickupItemsEnabled
                ? findHuntPriorityPickupItem(player)
                : null;
        List<LivingEntity> targets = findTargets(player);
        if (targets.isEmpty()) {
            this.currentTargetEntityId = -1;
            this.attackSequenceExecutor.stop();
            if (huntPriorityPickupItem != null) {
                stopHuntNavigation();
                handleHuntPickupMovement(player, huntPriorityPickupItem);
                return;
            }
            stopHuntPickupNavigation();
            stopHuntNavigation();
            return;
        }

        LivingEntity primaryTarget = targets.get(0);
        boolean orbitFacingActive = shouldForceOrbitFacing(player, primaryTarget);
        if (shouldRotateToTarget() || orbitFacingActive) {
            applyRotation(player, primaryTarget, orbitFacingActive);
        }

        if (huntPriorityPickupItem != null) {
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
            if (canTriggerAttackSequence(player, primaryTarget) && triggerAttackSequence(player, primaryTarget)) {
                this.sequenceCooldownTicks = attackSequenceDelayTicks;
            }
            return;
        }

        if (aimOnlyMode) {
            return;
        }

        if (canStartAttack(player) && mc.gameMode != null) {
            int attackedCount = attackTargets(mc, player, targets);
            if (attackedCount > 0) {
                player.swing(InteractionHand.MAIN_HAND);
                this.attackCooldownTicks = minAttackIntervalTicks;
                if (!isHuntOrbitEnabled()) {
                    stopHuntNavigation();
                }
            }
        }
    }

    @Override
    public void onReceivePacket(PacketEvent event) {
        if (event == null
                || event.getState() != EventState.PRE
                || !(event.getPacket() instanceof ClientboundPlayerPositionPacket packet)) {
            return;
        }

        TeleportAttackPlan plan = this.activeTeleportAttackPlan;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc == null ? null : mc.player;
        if (plan == null || this.pendingTeleportReturnTicks <= 0 || player == null) {
            return;
        }

        double correctedX = packet.getX();
        double correctedY = packet.getY();
        double correctedZ = packet.getZ();
        mc.execute(() -> handleTeleportCorrection(new double[] { correctedX, correctedY, correctedZ }));
    }

    @Override
    public void onPathEvent(PathEvent event) {
        if (event == null) {
            return;
        }
        if (event == PathEvent.CALC_FINISHED_NOW_EXECUTING) {
            clearCurrentHuntUnreachableFailureCount();
            return;
        }
        if (event != PathEvent.CALC_FAILED) {
            return;
        }
        handleHuntPathCalculationFailed();
    }

    private List<LivingEntity> findTargets(LocalPlayer player) {
        List<LivingEntity> targets = new ArrayList<>();
        LivingEntity lockedTarget = null;
        double searchRadius = getTargetSearchRadius();
        double searchRadiusSq = searchRadius * searchRadius;
        boolean useWhitelistPriority = enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty();
        boolean preferStableTarget = isHuntOrbitEnabled();
        int previousTargetEntityId = this.currentTargetEntityId;

        if (focusSingleTarget && this.currentTargetEntityId != -1) {
            Entity existing = player.level().getEntity(this.currentTargetEntityId);
            if (existing instanceof LivingEntity livingExisting
                    && isTrackableTarget(player, livingExisting, searchRadiusSq, useWhitelistPriority)) {
                lockedTarget = livingExisting;
                targets.add(lockedTarget);
            }
        }

        AABB searchBox = player.getBoundingBox().inflate(searchRadius, searchRadius * 0.75D, searchRadius);
        List<TargetCandidate> nearbyTargets = new ArrayList<>();
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (candidate == lockedTarget) {
                continue;
            }
            TargetCandidate targetCandidate = buildTargetCandidate(player, candidate, searchRadiusSq,
                    useWhitelistPriority, candidate.getId() == previousTargetEntityId,
                    shouldAllowHuntTrackingWithoutLineOfSight());
            if (targetCandidate != null) {
                nearbyTargets.add(targetCandidate);
            }
        }

        nearbyTargets.sort((left, right) -> {
            int whitelistCompare = Integer.compare(left.whitelistPriority, right.whitelistPriority);
            if (whitelistCompare != 0) {
                return whitelistCompare;
            }
            if (preferStableTarget) {
                int continuityCompare = Integer.compare(left.currentTargetPriority, right.currentTargetPriority);
                if (continuityCompare != 0) {
                    return continuityCompare;
                }
                int yawCompare = Float.compare(left.yawDeltaAbs, right.yawDeltaAbs);
                if (yawCompare != 0) {
                    return yawCompare;
                }
            }
            int distanceCompare = Double.compare(left.distanceSq, right.distanceSq);
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return Integer.compare(left.entity.getId(), right.entity.getId());
        });

        for (TargetCandidate nearbyTarget : nearbyTargets) {
            targets.add(nearbyTarget.entity);
        }
        this.currentTargetEntityId = targets.isEmpty() ? -1 : targets.get(0).getId();
        return targets;
    }

    private boolean isValidTarget(LocalPlayer player, LivingEntity target) {
        double targetSearchRadius = getTargetSearchRadius();
        return buildTargetCandidate(player, target, targetSearchRadius * targetSearchRadius,
                enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty(), false, false) != null;
    }

    private boolean isTrackableTarget(LocalPlayer player, LivingEntity target, double targetSearchRadiusSq,
            boolean useWhitelistPriority) {
        return buildTargetCandidate(player, target, targetSearchRadiusSq, useWhitelistPriority, false,
                shouldAllowHuntTrackingWithoutLineOfSight()) != null;
    }

    private TargetCandidate buildTargetCandidate(LocalPlayer player, LivingEntity target, double targetSearchRadiusSq,
            boolean useWhitelistPriority, boolean isCurrentTarget, boolean ignoreLineOfSightRequirement) {
        if (player == null || target == null || target == player) {
            return null;
        }
        if (!target.isAlive() || target.isRemoved() || target instanceof ArmorStand) {
            return null;
        }
        if (isHuntUnreachableExcludedTarget(player, target)) {
            return null;
        }
        if (ignoreInvisible && target.isInvisible()) {
            return null;
        }
        if (isHuntEnabled() && !isWithinConfiguredHuntVerticalRange(player, target)) {
            return null;
        }
        double distanceSq = player.distanceToSqr(target);
        if (distanceSq > targetSearchRadiusSq) {
            return null;
        }
        if (AutoFollowHandler.hasActiveLockChaseRestriction()
                && !AutoFollowHandler.isPositionWithinActiveLockChaseBounds(target.getX(), target.getZ())) {
            return null;
        }
        if (!ignoreLineOfSightRequirement && requireLineOfSight && !player.hasLineOfSight(target)) {
            return null;
        }

        String targetName = getFilterableEntityName(target);
        if (enableNameBlacklist && matchesNameList(targetName, nameBlacklist)) {
            return null;
        }
        int whitelistPriority = Integer.MAX_VALUE;
        if (enableNameWhitelist) {
            whitelistPriority = getNormalizedNameListMatchIndex(targetName, nameWhitelist);
            if (whitelistPriority == Integer.MAX_VALUE) {
                return null;
            }
        }
        if (!matchesEnabledTargetGroup(target)) {
            return null;
        }

        float yawDeltaAbs = Math.abs(Mth.wrapDegrees(getDesiredAimRotation(player, target).getYaw() - player.getYRot()));
        return new TargetCandidate(target, distanceSq, useWhitelistPriority ? whitelistPriority : 0,
                isCurrentTarget ? 0 : 1, yawDeltaAbs);
    }

    private boolean isHuntUnreachableExcludedTarget(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return false;
        }
        HuntUnreachableTracker tracker = huntUnreachableTrackers.get(target.getId());
        if (tracker == null) {
            return false;
        }
        if (hasHuntTargetRelocated(tracker, target)) {
            tracker.resetFailures(target);
            if (tracker.failedGoalKeys.isEmpty()) {
                huntUnreachableTrackers.remove(target.getId());
            }
            return false;
        }
        return tracker.excludeUntilTick > player.tickCount;
    }

    private void tickHuntUnreachableTrackers(LocalPlayer player) {
        if (player == null || player.level() == null || huntUnreachableTrackers.isEmpty()) {
            return;
        }
        List<Integer> trackedIds = new ArrayList<>(huntUnreachableTrackers.keySet());
        for (Integer entityId : trackedIds) {
            if (entityId == null) {
                continue;
            }
            Entity entity = player.level().getEntity(entityId);
            HuntUnreachableTracker tracker = huntUnreachableTrackers.get(entityId);
            if (tracker == null) {
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                huntUnreachableTrackers.remove(entityId);
                continue;
            }
            if (!living.isAlive() || living.isRemoved()) {
                huntUnreachableTrackers.remove(entityId);
                continue;
            }
            if (hasHuntTargetRelocated(tracker, living)) {
                tracker.resetFailures(living);
                if (tracker.failedGoalKeys.isEmpty()) {
                    huntUnreachableTrackers.remove(entityId);
                }
            }
        }
        pruneHuntUnreachableTrackingSize();
    }

    private void clearHuntUnreachableTracking() {
        huntUnreachableTrackers.clear();
    }

    private boolean hasHuntTargetRelocated(HuntUnreachableTracker tracker, LivingEntity target) {
        if (tracker == null || target == null) {
            return false;
        }
        double dx = target.getX() - tracker.lastTargetX;
        double dz = target.getZ() - tracker.lastTargetZ;
        return dx * dx + dz * dz >= HUNT_UNREACHABLE_TARGET_RESET_DISTANCE_SQ;
    }

    private boolean isBlockedHuntNavigationDestination(LivingEntity target, double goalX, double goalY, double goalZ) {
        if (target == null) {
            return false;
        }
        HuntUnreachableTracker tracker = huntUnreachableTrackers.get(target.getId());
        if (tracker == null || tracker.failedGoalKeys.isEmpty()) {
            return false;
        }
        return tracker.failedGoalKeys.contains(toHuntGoalKey(goalX, goalY, goalZ));
    }

    private long toHuntGoalKey(double goalX, double goalY, double goalZ) {
        return BlockPos.containing(goalX, goalY, goalZ).asLong();
    }

    private void clearCurrentHuntUnreachableFailureCount() {
        if (!huntNavigationActive || lastHuntTargetEntityId == Integer.MIN_VALUE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(lastHuntTargetEntityId);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        HuntUnreachableTracker tracker = huntUnreachableTrackers.get(lastHuntTargetEntityId);
        if (tracker == null) {
            return;
        }
        tracker.failedGoalCount = 0;
        tracker.excludeUntilTick = 0;
        tracker.refreshTargetPosition(living);
    }

    private void handleHuntPathCalculationFailed() {
        if (!huntNavigationActive || lastHuntTargetEntityId == Integer.MIN_VALUE
                || Double.isNaN(lastHuntGoalX) || Double.isNaN(lastHuntGoalY) || Double.isNaN(lastHuntGoalZ)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc == null ? null : mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(lastHuntTargetEntityId);
        if (!(entity instanceof LivingEntity target)) {
            stopHuntNavigation();
            return;
        }
        HuntUnreachableTracker tracker = huntUnreachableTrackers.get(target.getId());
        if (tracker == null) {
            tracker = new HuntUnreachableTracker(target);
            huntUnreachableTrackers.put(target.getId(), tracker);
        } else if (hasHuntTargetRelocated(tracker, target)) {
            tracker.resetFailures(target);
        } else {
            tracker.refreshTargetPosition(target);
        }

        tracker.failedGoalCount++;
        tracker.failedGoalKeys.add(toHuntGoalKey(lastHuntGoalX, lastHuntGoalY, lastHuntGoalZ));
        while (tracker.failedGoalKeys.size() > HUNT_UNREACHABLE_MAX_FAILED_GOALS_PER_TARGET) {
            Long first = tracker.failedGoalKeys.iterator().next();
            tracker.failedGoalKeys.remove(first);
        }
        pruneHuntUnreachableTrackingSize();

        boolean temporarilyExcluded = tracker.failedGoalCount >= HUNT_UNREACHABLE_FAILS_BEFORE_TEMP_EXCLUDE;
        if (temporarilyExcluded) {
            tracker.excludeUntilTick = player.tickCount + HUNT_UNREACHABLE_TEMP_EXCLUDE_TICKS;
            tracker.failedGoalCount = 0;
        }

        zszlScriptMod.LOGGER.info(
                "杀戮光环追击寻路不可达: id={}, name={}, goal=({}, {}, {}), failedGoals={}, tempExcluded={}",
                target.getId(),
                getFilterableEntityName(target),
                Mth.floor(lastHuntGoalX),
                Mth.floor(lastHuntGoalY),
                Mth.floor(lastHuntGoalZ),
                tracker.failedGoalKeys.size(),
                temporarilyExcluded);

        stopHuntNavigation();
        if (temporarilyExcluded && this.currentTargetEntityId == target.getId()) {
            this.currentTargetEntityId = -1;
            this.attackSequenceExecutor.stop();
        }
    }

    private void pruneHuntUnreachableTrackingSize() {
        while (huntUnreachableTrackers.size() > HUNT_UNREACHABLE_MAX_TRACKED_TARGETS) {
            Integer first = huntUnreachableTrackers.keySet().iterator().next();
            huntUnreachableTrackers.remove(first);
        }
    }

    private boolean shouldAllowHuntTrackingWithoutLineOfSight() {
        return isHuntEnabled();
    }

    private boolean canStartAttack(LocalPlayer player) {
        if (player == null || aimOnlyMode || isSequenceAttackMode() || this.attackCooldownTicks > 0) {
            return false;
        }
        if (onlyWeapon && getPreferredAttackHotbarSlot(player) < 0) {
            return false;
        }
        return player.getAttackStrengthScale(0.0F) >= minAttackStrength;
    }

    private int attackTargets(Minecraft mc, LocalPlayer player, List<LivingEntity> targets) {
        if (mc == null || player == null || targets == null || targets.isEmpty()) {
            return 0;
        }

        int attackLimit = isMouseClickAttackMode() ? 1 : Math.max(1, targetsPerAttack);
        int attackedCount = 0;
        for (LivingEntity target : targets) {
            if (attackedCount >= attackLimit) {
                break;
            }
            boolean teleportAttack = shouldUseTeleportAttack(player, target);
            if (!canAttackTarget(player, target, shouldRequireCrosshairHitForAttack(teleportAttack))) {
                continue;
            }

            boolean attacked = false;
            if (teleportAttack) {
                attacked = performTeleportAttack(player, target);
            } else if (isMouseClickAttackMode()) {
                attacked = performMouseClickAttack(mc);
            } else if (isPacketAttackMode()) {
                if (player.connection != null) {
                    player.connection.send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
                    attacked = true;
                }
            } else if (mc.gameMode != null) {
                mc.gameMode.attack(player, target);
                attacked = true;
            }

            if (attacked) {
                attackedCount++;
            }
        }
        return attackedCount;
    }

    private boolean canAttackTarget(LocalPlayer player, LivingEntity target) {
        return canAttackTarget(player, target, false);
    }

    private boolean canAttackTarget(LocalPlayer player, LivingEntity target, boolean requireCrosshairHit) {
        if (player == null || target == null || !target.isAlive() || !isValidTarget(player, target)) {
            return false;
        }
        if (requireLineOfSight && !player.hasLineOfSight(target)) {
            return false;
        }
        if (player.distanceToSqr(target) > attackRange * attackRange) {
            return false;
        }
        float yawDiff = Math.abs(Mth.wrapDegrees(getDesiredAimRotation(player, target).getYaw() - player.getYRot()));
        if (shouldRotateToTarget() && yawDiff > 100.0F) {
            return false;
        }
        return !requireCrosshairHit || isViewRayHittingAttackableTarget(player, target);
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

    private boolean isViewRayHittingAttackableTarget(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null || !target.isAlive()) {
            return false;
        }
        Vec3 eyePos = player.getEyePosition();
        AABB hitBox = target.getBoundingBox().inflate(Math.max(0.1D, target.getPickRadius()));
        if (hitBox.contains(eyePos)) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0F);
        double reach = Math.max(attackRange, player.distanceTo(target) + target.getBbWidth() + 0.25D);
        Vec3 endPos = eyePos.add(look.x * reach, look.y * reach, look.z * reach);
        Optional<Vec3> hit = hitBox.clip(eyePos, endPos);
        return hit.isPresent() && eyePos.distanceToSqr(hit.get()) <= reach * reach;
    }

    private boolean performMouseClickAttack(Minecraft mc) {
        if (mc == null || mc.screen != null) {
            return false;
        }
        int screenWidth = Math.max(1, mc.getWindow().getScreenWidth());
        int screenHeight = Math.max(1, mc.getWindow().getScreenHeight());
        ModUtils.simulateMouseClick(screenWidth / 2, screenHeight / 2, true, screenWidth, screenHeight);
        return true;
    }

    private boolean shouldUseTeleportAttack(LocalPlayer player, LivingEntity target) {
        return isTeleportAttackMode()
                && attackRange > TELEPORT_ATTACK_MIN_RANGE
                && player != null
                && target != null
                && !isTeleportAttackRecoveryActive()
                && player.distanceTo(target) > TELEPORT_ATTACK_MIN_RANGE;
    }

    private boolean performTeleportAttack(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null || player.connection == null || isTeleportAttackRecoveryActive()) {
            return false;
        }

        TeleportAttackPlan plan = buildTeleportAttackPlan(player, target);
        if (plan == null) {
            return false;
        }

        sendTeleportWaypoints(player, plan.outboundWaypoints, plan.originOnGround);
        if (shouldRotateToTarget()) {
            player.connection.send(new ServerboundMovePlayerPacket.PosRot(plan.assaultX, plan.assaultY, plan.assaultZ,
                    plan.attackYaw, plan.attackPitch, plan.originOnGround));
        } else {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(plan.assaultX, plan.assaultY, plan.assaultZ,
                    plan.originOnGround));
        }
        player.connection.send(ServerboundInteractPacket.createAttackPacket(target, player.isShiftKeyDown()));
        sendTeleportReturnToOrigin(player, plan, plan.assaultX, plan.assaultY, plan.assaultZ, false);
        this.activeTeleportAttackPlan = plan;
        this.pendingTeleportReturnTicks = TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS;
        return true;
    }

    private void handleHuntMovement(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            debugOrbit("绕圈停止", "玩家或目标为空");
            stopHuntNavigation();
            return;
        }
        if (isHuntOrbitEnabled() && shouldBlockOrbitNavigationWhileAirborne(player)) {
            debugOrbit("绕圈阻止", "角色处于空中，暂不启动绕圈");
            stopHuntNavigation();
            return;
        }

        if (isHuntOrbitEnabled()) {
            List<Vec3> orbitRenderLoop = getHuntOrbitRenderLoop(target);
            boolean shouldUseLocalOrbit = (this.huntOrbitController.isActive() && canStartOrbitHunt(player, target))
                    || shouldUseContinuousOrbitController(player, target, orbitRenderLoop);
            if (shouldUseLocalOrbit) {
                debugOrbit("绕圈运行",
                        "目标=" + (target.getDisplayName() == null ? "" : target.getDisplayName().getString())
                                + " 距离=" + String.format(Locale.ROOT, "%.2f", player.distanceTo(target))
                                + " 半径=" + String.format(Locale.ROOT, "%.2f", getEffectiveHuntFixedDistance())
                                + " 采样点=" + getConfiguredHuntOrbitSamplePoints());
                stopEmbeddedHuntNavigation();
                driveContinuousHuntOrbit(player, target);
                return;
            }
            debugOrbit("绕圈待机",
                    "尚未满足起绕条件，当前距离="
                            + String.format(Locale.ROOT, "%.2f", player.distanceTo(target)));
            this.huntOrbitController.stop();
        } else {
            this.huntOrbitController.stop();
        }

        int nowTick = player.tickCount;
        int targetId = target.getId();
        double dx = target.getX() - this.lastHuntTargetX;
        double dz = target.getZ() - this.lastHuntTargetZ;
        double movedSq = dx * dx + dz * dz;
        boolean shouldSendGoto = !huntNavigationActive
                || targetId != this.lastHuntTargetEntityId
                || movedSq >= HUNT_GOTO_MOVE_THRESHOLD_SQ
                || (nowTick - this.lastHuntGotoTick) >= HUNT_GOTO_INTERVAL_TICKS;
        if (!shouldSendGoto) {
            return;
        }

        if (isHuntFixedDistanceMode()) {
            double[] safeDestination = findFixedDistanceHuntNavigationDestination(player, target);
            if (safeDestination != null) {
                EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_HUNT,
                        safeDestination[0], safeDestination[1],
                        safeDestination[2], true, "固定距离追击：采用安全导航落点");
                recordHuntNavigationDispatch(target, safeDestination[0], safeDestination[1], safeDestination[2], nowTick);
            } else {
                double[] destination = computeFixedDistanceHuntDestination(player, target);
                if (isBlockedHuntNavigationDestination(target, destination[0], target.getY(), destination[2])) {
                    temporarilyExcludeUnreachableHuntTarget(target, nowTick);
                    return;
                }
                EmbeddedNavigationHandler.INSTANCE.startGotoXZ(
                        EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_HUNT,
                        destination[0], destination[2], true, "固定距离追击：直接导航到计算落点");
                recordHuntNavigationDispatch(target, destination[0], target.getY(), destination[2], nowTick);
            }
        } else {
            double[] safeDestination = findApproachHuntNavigationDestination(player, target);
            if (safeDestination != null) {
                EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_HUNT,
                        safeDestination[0], safeDestination[1],
                        safeDestination[2], true, "接近追击：采用安全导航落点");
                recordHuntNavigationDispatch(target, safeDestination[0], safeDestination[1], safeDestination[2], nowTick);
            } else {
                if (isBlockedHuntNavigationDestination(target, target.getX(), target.getY(), target.getZ())) {
                    temporarilyExcludeUnreachableHuntTarget(target, nowTick);
                    return;
                }
                EmbeddedNavigationHandler.INSTANCE.startGotoXZ(
                        EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_HUNT,
                        target.getX(), target.getZ(), true, "接近追击：直接贴近目标XZ");
                recordHuntNavigationDispatch(target, target.getX(), target.getY(), target.getZ(), nowTick);
            }
        }
    }

    private void recordHuntNavigationDispatch(LivingEntity target, double goalX, double goalY, double goalZ, int nowTick) {
        if (target == null) {
            return;
        }
        this.huntNavigationActive = true;
        this.lastHuntGotoTick = nowTick;
        this.lastHuntTargetEntityId = target.getId();
        this.lastHuntTargetX = target.getX();
        this.lastHuntTargetZ = target.getZ();
        this.lastHuntGoalX = goalX;
        this.lastHuntGoalY = goalY;
        this.lastHuntGoalZ = goalZ;
    }

    private void temporarilyExcludeUnreachableHuntTarget(LivingEntity target, int nowTick) {
        if (target == null) {
            return;
        }
        HuntUnreachableTracker tracker = huntUnreachableTrackers.get(target.getId());
        if (tracker == null) {
            tracker = new HuntUnreachableTracker(target);
            huntUnreachableTrackers.put(target.getId(), tracker);
        }
        tracker.refreshTargetPosition(target);
        tracker.failedGoalCount = 0;
        tracker.excludeUntilTick = nowTick + HUNT_UNREACHABLE_TEMP_EXCLUDE_TICKS;
        pruneHuntUnreachableTrackingSize();
        zszlScriptMod.LOGGER.info("杀戮光环追击目标暂时排除: id={}, name={}, untilTick={}",
                target.getId(), getFilterableEntityName(target), tracker.excludeUntilTick);
        stopHuntNavigation();
        if (this.currentTargetEntityId == target.getId()) {
            this.currentTargetEntityId = -1;
            this.attackSequenceExecutor.stop();
        }
    }

    private ItemEntity findHuntPriorityPickupItem(LocalPlayer player) {
        if (player == null || player.level() == null || !isHuntEnabled() || huntRadius <= 0.05F) {
            return null;
        }

        int nowTick = player.tickCount;
        double radiusSq = huntRadius * huntRadius;
        if (nowTick - lastHuntPickupSearchTick < HUNT_PICKUP_SEARCH_INTERVAL_TICKS) {
            ItemEntity cached = resolveCachedHuntPickupItem(player, radiusSq);
            if (cached != null) {
                return cached;
            }
            if (!lastHuntPickupSearchFound) {
                return null;
            }
        }

        ItemEntity nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        AABB searchBox = player.getBoundingBox().inflate(huntRadius, 1.5D, huntRadius);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (!item.isAlive() || !item.onGround()) {
                continue;
            }
            double playerDistSq = player.distanceToSqr(item);
            if (playerDistSq > radiusSq) {
                continue;
            }
            if (playerDistSq < bestDistSq) {
                bestDistSq = playerDistSq;
                nearest = item;
            }
        }

        lastHuntPickupSearchTick = nowTick;
        lastHuntPickupSearchTargetEntityId = nearest == null ? Integer.MIN_VALUE : nearest.getId();
        lastHuntPickupSearchFound = nearest != null;
        return nearest;
    }

    private ItemEntity resolveCachedHuntPickupItem(LocalPlayer player, double radiusSq) {
        if (player == null || player.level() == null || lastHuntPickupSearchTargetEntityId == Integer.MIN_VALUE) {
            return null;
        }
        Entity entity = player.level().getEntity(lastHuntPickupSearchTargetEntityId);
        if (!(entity instanceof ItemEntity item)) {
            return null;
        }
        return !item.isAlive() || !item.onGround() || player.distanceToSqr(item) > radiusSq ? null : item;
    }

    private void handleHuntPickupMovement(LocalPlayer player, ItemEntity item) {
        if (player == null || item == null || !item.isAlive()) {
            stopHuntPickupNavigation();
            return;
        }
        if (hasReachedHuntPickupItem(player, item)) {
            stopHuntPickupNavigation();
            return;
        }

        int nowTick = player.tickCount;
        int itemId = item.getId();
        boolean shouldSendGoto = !huntPickupNavigationActive
                || itemId != this.lastHuntPickupTargetEntityId
                || (nowTick - this.lastHuntPickupGotoTick) >= HUNT_PICKUP_GOTO_INTERVAL_TICKS;
        if (!shouldSendGoto) {
            return;
        }

        EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_PICKUP,
                item.getX(), item.getY(), item.getZ(), false, "追击拾取：锁定掉落物并开始接近");
        this.huntPickupNavigationActive = true;
        this.lastHuntPickupGotoTick = nowTick;
        this.lastHuntPickupTargetEntityId = itemId;
    }

    private boolean hasReachedHuntPickupItem(LocalPlayer player, ItemEntity item) {
        return player != null
                && item != null
                && item.isAlive()
                && player.getBoundingBox().inflate(HUNT_PICKUP_OVERLAP_GROWTH, 0.0D, HUNT_PICKUP_OVERLAP_GROWTH)
                        .intersects(item.getBoundingBox());
    }

    private void stopHuntNavigation() {
        this.huntOrbitController.stop();
        stopEmbeddedHuntNavigation();
        clearOrbitDebugState();
    }

    private void stopEmbeddedHuntNavigation() {
        if (!this.huntNavigationActive) {
            return;
        }
        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_HUNT,
                "追击导航结束或目标切换，停止杀戮光环追击导航");
        this.huntNavigationActive = false;
        this.lastHuntGotoTick = -99999;
        this.lastHuntTargetEntityId = Integer.MIN_VALUE;
        this.lastHuntTargetX = 0.0D;
        this.lastHuntTargetZ = 0.0D;
        this.lastHuntGoalX = Double.NaN;
        this.lastHuntGoalY = Double.NaN;
        this.lastHuntGoalZ = Double.NaN;
    }

    private void debugOrbit(String status, String detail) {
        if (!ModConfig.isDebugFlagEnabled(DebugModule.KILL_AURA_ORBIT)) {
            return;
        }
        String safeStatus = status == null ? "" : status;
        String safeDetail = detail == null ? "" : detail;
        String state = safeStatus + "|" + safeDetail;
        if (state.equals(this.lastOrbitDebugState)) {
            return;
        }
        this.lastOrbitDebugState = state;
        ModConfig.debugPrint(DebugModule.KILL_AURA_ORBIT,
                safeStatus + (safeDetail.trim().isEmpty() ? "" : " | " + safeDetail));
    }

    private void clearOrbitDebugState() {
        this.lastOrbitDebugState = "";
    }

    private void stopHuntPickupNavigation() {
        if (!this.huntPickupNavigationActive) {
            return;
        }
        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.KILL_AURA_PICKUP,
                "追击拾取完成或目标失效，停止掉落物导航");
        this.huntPickupNavigationActive = false;
        this.lastHuntPickupGotoTick = -99999;
        this.lastHuntPickupTargetEntityId = Integer.MIN_VALUE;
    }

    private boolean shouldRunHuntMovement(LocalPlayer player, LivingEntity target) {
        if (!isHuntEnabled() || player == null || target == null) {
            return false;
        }
        if (isHuntOrbitEnabled() && shouldBlockOrbitNavigationWhileAirborne(player)) {
            return false;
        }

        double distance = player.distanceTo(target);
        boolean missingAttackLineOfSight = requireLineOfSight && !player.hasLineOfSight(target);
        if (isHuntFixedDistanceMode()) {
            if (canStartOrbitHunt(player, target)) {
                return true;
            }
            return missingAttackLineOfSight
                    || Math.abs(distance - getEffectiveHuntFixedDistance()) > HUNT_FIXED_DISTANCE_TOLERANCE;
        }
        return missingAttackLineOfSight || distance > attackRange;
    }

    private boolean canStartOrbitHunt(LocalPlayer player, LivingEntity target) {
        if (!isHuntOrbitEnabled() || player == null || target == null) {
            return false;
        }
        if (shouldBlockOrbitNavigationWhileAirborne(player)) {
            return false;
        }
        if (Math.abs(player.getY() - target.getY()) > HUNT_ORBIT_MAX_ENTRY_VERTICAL_DELTA) {
            return false;
        }
        double maxEntryDistance = Math.max(getEffectiveHuntFixedDistance() + HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER, attackRange + 0.9D);
        double allowedDistance = this.huntOrbitController.isActive()
                ? maxEntryDistance + HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER
                : maxEntryDistance;
        return player.distanceToSqr(target) <= allowedDistance * allowedDistance;
    }

    private boolean shouldUseContinuousOrbitController(LocalPlayer player, LivingEntity target, List<Vec3> renderLoop) {
        if (!canStartOrbitHunt(player, target)) {
            return false;
        }
        return getHorizontalDistanceToOrbitLoop(player.getX(), player.getZ(), renderLoop)
                <= HUNT_CONTINUOUS_ORBIT_LOOP_ENTRY_MAX_DISTANCE;
    }

    private double getHorizontalDistanceToOrbitLoop(double playerX, double playerZ, List<Vec3> renderLoop) {
        if (renderLoop == null || renderLoop.size() < 2) {
            return Double.POSITIVE_INFINITY;
        }

        Vec3 playerPos = new Vec3(playerX, 0.0D, playerZ);
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (int i = 0; i < renderLoop.size() - 1; i++) {
            Vec3 start = flattenToHorizontal(renderLoop.get(i));
            Vec3 end = flattenToHorizontal(renderLoop.get(i + 1));
            Vec3 nearest = nearestPointOnHorizontalSegment(playerPos, start, end);
            bestDistanceSq = Math.min(bestDistanceSq, playerPos.distanceToSqr(nearest));
        }
        return bestDistanceSq == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : Math.sqrt(bestDistanceSq);
    }

    private Vec3 nearestPointOnHorizontalSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSq = segment.lengthSqr();
        if (lengthSq <= 1.0E-6D) {
            return start;
        }
        double t = point.subtract(start).dot(segment) / lengthSq;
        t = Mth.clamp(t, 0.0D, 1.0D);
        return start.add(segment.scale(t));
    }

    private Vec3 flattenToHorizontal(Vec3 vec) {
        return vec == null ? Vec3.ZERO : new Vec3(vec.x, 0.0D, vec.z);
    }

    private boolean shouldBlockOrbitNavigationWhileAirborne(LocalPlayer player) {
        return player != null && (player.getAbilities().flying || player.isFallFlying());
    }

    private void driveContinuousHuntOrbit(LocalPlayer player, LivingEntity target) {
        this.huntOrbitController.tick(player, target,
                new HuntOrbitController.OrbitConfig(getEffectiveHuntFixedDistance(),
                        HUNT_FIXED_DISTANCE_TOLERANCE, huntJumpOrbitEnabled, true, true));
    }

    private double[] computeFixedDistanceHuntDestination(LocalPlayer player, LivingEntity target) {
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

        double desiredDistance = getEffectiveHuntFixedDistance();
        double scale = desiredDistance / Math.max(distance, 1.0E-4D);
        double destinationX = target.getX() + dx * scale;
        double destinationY = target.getY() + dy * scale;
        double destinationZ = target.getZ() + dz * scale;
        double[] clippedDestination = clipHuntDestinationXZ(target.getX(), target.getZ(), destinationX, destinationZ);
        return new double[] { clippedDestination[0], destinationY, clippedDestination[1] };
    }

    private double[] findApproachHuntNavigationDestination(LocalPlayer player, LivingEntity target) {
        double maxStandRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, attackRange - HUNT_APPROACH_TARGET_BUFFER);
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS,
                Math.min(maxStandRadius, attackRange - HUNT_APPROACH_TARGET_BUFFER * 2.0D));
        return findHuntNavigationDestinationAroundTarget(player, target, preferredRadius,
                HUNT_APPROACH_MIN_STAND_RADIUS, maxStandRadius);
    }

    private double[] findFixedDistanceHuntNavigationDestination(LocalPlayer player, LivingEntity target) {
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
        double[] destination = findHuntNavigationDestinationAroundTarget(player, target, preferredRadius, minRadius, maxRadius);
        if (destination != null) {
            return destination;
        }
        double[] fallback = computeFixedDistanceHuntDestination(player, target);
        return findSafeHuntNavigationDestination(player, target, fallback[0], fallback[1], fallback[2]);
    }

    private double[] findOrbitAlignedHuntNavigationDestination(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return null;
        }

        double[] previewEntry = findOrbitEntryFromRenderLoop(player, target, getHuntOrbitRenderLoop(target));
        if (previewEntry != null) {
            debugOrbit("绕圈进场",
                    "来源=preview_render 落点=" + formatVec3(previewEntry)
                            + " 半径=" + String.format(Locale.ROOT, "%.2f", getHorizontalRadiusToTarget(target, previewEntry)));
            return previewEntry;
        }

        double[] exactOrbitPoint = computeFixedDistanceHuntDestination(player, target);
        double[] directSafeDestination = findSafeHuntNavigationDestination(player, target, exactOrbitPoint[0],
                exactOrbitPoint[1], exactOrbitPoint[2], HUNT_ORBIT_ENTRY_SAFE_SEARCH_RADIUS);
        double preferredRadius = Math.max(HUNT_APPROACH_MIN_STAND_RADIUS, getEffectiveHuntFixedDistance());
        double orbitBand = Math.max(HUNT_FIXED_DISTANCE_TOLERANCE, HUNT_ORBIT_ENTRY_RADIUS_BAND);
        if (directSafeDestination != null
                && isDestinationNearOrbitBand(target, directSafeDestination, preferredRadius, orbitBand)
                && centerDistSq(directSafeDestination[0], directSafeDestination[2], exactOrbitPoint[0], exactOrbitPoint[2]) <= 0.65D * 0.65D) {
            debugOrbit("绕圈进场",
                    "来源=exact_safe 落点=" + formatVec3(directSafeDestination)
                            + " 半径=" + String.format(Locale.ROOT, "%.2f",
                                    getHorizontalRadiusToTarget(target, directSafeDestination))
                            + " 基准=" + formatVec3(exactOrbitPoint));
            return directSafeDestination;
        }

        debugOrbit("绕圈进场",
                "来源=exact_xz 落点=" + formatVec3(exactOrbitPoint)
                        + " 半径=" + String.format(Locale.ROOT, "%.2f", getHorizontalRadiusToTarget(target, exactOrbitPoint)));
        return null;
    }

    private double[] findOrbitEntryFromRenderLoop(LocalPlayer player, LivingEntity target, List<Vec3> renderLoop) {
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
            Vec3 point = renderLoop.get(i);
            if (point == null) {
                continue;
            }

            double[] safeDestination = findSafeHuntNavigationDestination(player, target, point.x, target.getY(), point.z,
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

            BlockPos standPos = BlockPos.containing(safeDestination[0], safeDestination[1], safeDestination[2]);
            boolean hasLineOfSight = hasHuntLineOfSightFromStandPos(player, standPos, target);
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

    private int getOrbitRenderCandidateCount(List<Vec3> renderLoop) {
        if (renderLoop == null || renderLoop.isEmpty()) {
            return 0;
        }
        if (renderLoop.size() >= 2
                && renderLoop.get(0) != null
                && renderLoop.get(renderLoop.size() - 1) != null
                && renderLoop.get(0).distanceToSqr(renderLoop.get(renderLoop.size() - 1)) <= 1.0E-4D) {
            return renderLoop.size() - 1;
        }
        return renderLoop.size();
    }

    private double[] findHuntNavigationDestinationAroundTarget(LocalPlayer player, LivingEntity target,
            double preferredRadius, double minRadius, double maxRadius) {
        if (player == null || player.level() == null || target == null) {
            return null;
        }

        double clampedMinRadius = Math.max(0.0D, minRadius);
        double clampedPreferredRadius = Math.max(clampedMinRadius, preferredRadius);
        double clampedMaxRadius = Math.max(clampedPreferredRadius, maxRadius);
        double[] bestVisibleDestination = null;
        double bestVisibleScore = Double.POSITIVE_INFINITY;
        double[] bestFallbackDestination = null;
        double bestFallbackScore = Double.POSITIVE_INFINITY;
        double baseAngle = Math.atan2(player.getZ() - target.getZ(), player.getX() - target.getX());

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

                double desiredX = target.getX() + Math.cos(baseAngle + angleOffset) * radius;
                double desiredZ = target.getZ() + Math.sin(baseAngle + angleOffset) * radius;
                double[] clippedDestination = clipHuntDestinationXZ(target.getX(), target.getZ(), desiredX, desiredZ);
                double[] safeDestination = findSafeHuntNavigationDestination(player, target, clippedDestination[0],
                        target.getY(), clippedDestination[1]);
                if (safeDestination == null) {
                    continue;
                }

                BlockPos standPos = BlockPos.containing(safeDestination[0], safeDestination[1], safeDestination[2]);
                boolean hasLineOfSight = hasHuntLineOfSightFromStandPos(player, standPos, target);
                double score = scoreHuntNavigationDestination(player, target, safeDestination,
                        clampedPreferredRadius, hasLineOfSight);
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
        return findSafeHuntNavigationDestination(player, target, target.getX(), target.getY(), target.getZ());
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
        double clamped = Mth.clamp(radius, minRadius, maxRadius);
        for (Double existing : samples) {
            if (existing != null && Math.abs(existing - clamped) <= 1.0E-4D) {
                return;
            }
        }
        samples.add(clamped);
    }

    private double scoreHuntNavigationDestination(LocalPlayer player, LivingEntity target, double[] destination,
            double preferredRadius, boolean hasLineOfSight) {
        if (player == null || target == null || destination == null || destination.length < 3) {
            return Double.POSITIVE_INFINITY;
        }
        double targetDx = destination[0] - target.getX();
        double targetDz = destination[2] - target.getZ();
        double actualRadius = Math.sqrt(targetDx * targetDx + targetDz * targetDz);
        double radiusPenalty = Math.abs(actualRadius - preferredRadius);
        double playerDx = destination[0] - player.getX();
        double playerDy = destination[1] - player.getY();
        double playerDz = destination[2] - player.getZ();
        double playerDistancePenalty = playerDx * playerDx + playerDz * playerDz + playerDy * playerDy * 0.35D;
        double verticalPenalty = Math.abs(destination[1] - target.getY());
        double visibilityPenalty = hasLineOfSight ? 0.0D : 4.0D;
        return radiusPenalty * 4.0D + playerDistancePenalty * 0.18D + verticalPenalty * 0.7D + visibilityPenalty;
    }

    private double scoreOrbitEntryDestination(LocalPlayer player, LivingEntity target, double[] destination,
            Vec3 desiredPoint, double preferredRadius, boolean hasLineOfSight) {
        double score = scoreHuntNavigationDestination(player, target, destination, preferredRadius, hasLineOfSight);
        if (destination == null || desiredPoint == null) {
            return score;
        }
        return score + centerDistSq(destination[0], destination[2], desiredPoint.x, desiredPoint.z) * 4.5D;
    }

    private boolean isDestinationNearOrbitBand(LivingEntity target, double[] destination, double preferredRadius,
            double orbitBand) {
        if (target == null || destination == null || destination.length < 3) {
            return false;
        }
        double actualRadius = Math.sqrt(centerDistSq(destination[0], destination[2], target.getX(), target.getZ()));
        return Math.abs(actualRadius - preferredRadius) <= Math.max(0.1D, orbitBand);
    }

    private double getHorizontalRadiusToTarget(LivingEntity target, double[] destination) {
        if (target == null || destination == null || destination.length < 3) {
            return 0.0D;
        }
        return Math.sqrt(centerDistSq(destination[0], destination[2], target.getX(), target.getZ()));
    }

    private double[] clipHuntDestinationXZ(double centerX, double centerZ, double destinationX, double destinationZ) {
        if (!AutoFollowHandler.hasActiveLockChaseRestriction()
                || AutoFollowHandler.isPositionWithinActiveLockChaseBounds(destinationX, destinationZ)) {
            return new double[] { destinationX, destinationZ };
        }

        double dirX = destinationX - centerX;
        double dirZ = destinationZ - centerZ;
        double distance = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (distance <= 1.0E-6D) {
            return new double[] { centerX, centerZ };
        }

        double low = 0.0D;
        double high = distance;
        for (int i = 0; i < 14; i++) {
            double mid = (low + high) * 0.5D;
            double testX = centerX + dirX * (mid / distance);
            double testZ = centerZ + dirZ * (mid / distance);
            if (AutoFollowHandler.isPositionWithinActiveLockChaseBounds(testX, testZ)) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return new double[] { centerX + dirX * (low / distance), centerZ + dirZ * (low / distance) };
    }

    private double[] findSafeHuntNavigationDestination(LocalPlayer player, double desiredX, double desiredY,
            double desiredZ) {
        return findSafeHuntNavigationDestination(player, null, desiredX, desiredY, desiredZ, 2);
    }

    private double[] findSafeHuntNavigationDestination(LocalPlayer player, double desiredX, double desiredY,
            double desiredZ, int safeSearchRadius) {
        return findSafeHuntNavigationDestination(player, null, desiredX, desiredY, desiredZ, safeSearchRadius);
    }

    private double[] findSafeHuntNavigationDestination(LocalPlayer player, LivingEntity target, double desiredX,
            double desiredY, double desiredZ) {
        return findSafeHuntNavigationDestination(player, target, desiredX, desiredY, desiredZ, 2);
    }

    private double[] findSafeHuntNavigationDestination(LocalPlayer player, LivingEntity target, double desiredX,
            double desiredY, double desiredZ, int safeSearchRadius) {
        if (player == null || player.level() == null) {
            return null;
        }
        BlockPos desiredFeet = BlockPos.containing(desiredX, desiredY, desiredZ);
        double bestScore = Double.POSITIVE_INFINITY;
        double[] best = null;
        int searchRadius = Math.max(0, safeSearchRadius);

        for (int yOffset = -searchRadius; yOffset <= searchRadius; yOffset++) {
            for (int xOffset = -searchRadius; xOffset <= searchRadius; xOffset++) {
                for (int zOffset = -searchRadius; zOffset <= searchRadius; zOffset++) {
                    BlockPos candidateFeet = desiredFeet.offset(xOffset, yOffset, zOffset);
                    if (!isStandableHuntFeetPos(player, candidateFeet)) {
                        continue;
                    }
                    double centerX = candidateFeet.getX() + 0.5D;
                    double centerY = candidateFeet.getY();
                    double centerZ = candidateFeet.getZ() + 0.5D;
                    if (isBlockedHuntNavigationDestination(target, centerX, centerY, centerZ)) {
                        continue;
                    }
                    double score = player.distanceToSqr(centerX, centerY, centerZ)
                            + centerDistSq(centerX, centerZ, desiredX, desiredZ) * 2.25D
                            + Math.abs(centerY - desiredY) * 1.35D;
                    if (score < bestScore) {
                        bestScore = score;
                        best = new double[] { centerX, centerY, centerZ };
                    }
                }
            }
        }
        return best;
    }

    private boolean isStandableHuntFeetPos(LocalPlayer player, BlockPos standPos) {
        if (player == null || standPos == null) {
            return false;
        }
        if (AutoFollowHandler.hasActiveLockChaseRestriction()
                && !AutoFollowHandler.isPositionWithinActiveLockChaseBounds(standPos.getX() + 0.5D,
                        standPos.getZ() + 0.5D)) {
            return false;
        }

        BlockState feetState = player.level().getBlockState(standPos);
        BlockState headState = player.level().getBlockState(standPos.above());
        BlockState belowState = player.level().getBlockState(standPos.below());
        if (!feetState.getCollisionShape(player.level(), standPos).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(player.level(), standPos.above()).isEmpty()) {
            return false;
        }
        if (belowState.getCollisionShape(player.level(), standPos.below()).isEmpty()) {
            return false;
        }

        AABB box = player.getDimensions(player.getPose()).makeBoundingBox(
                standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D);
        return player.level().noCollision(player, box);
    }

    private boolean hasHuntLineOfSightFromStandPos(LocalPlayer player, BlockPos standPos, LivingEntity target) {
        if (player == null || standPos == null || target == null) {
            return false;
        }
        Vec3 from = new Vec3(standPos.getX() + 0.5D, standPos.getY() + player.getEyeHeight(), standPos.getZ() + 0.5D);
        Vec3 to = new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.85D, target.getZ());
        HitResult result = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return result.getType() == HitResult.Type.MISS;
    }

    private double centerDistSq(double leftX, double leftZ, double rightX, double rightZ) {
        double dx = leftX - rightX;
        double dz = leftZ - rightZ;
        return dx * dx + dz * dz;
    }

    private double[] getClippedHuntPoint(double centerX, double centerZ, double radius, double angle) {
        double desiredX = centerX + Math.cos(angle) * radius;
        double desiredZ = centerZ + Math.sin(angle) * radius;
        return clipHuntDestinationXZ(centerX, centerZ, desiredX, desiredZ);
    }

    private List<Vec3> getHuntOrbitRenderLoop(LivingEntity target) {
        if (target == null || !isHuntOrbitEnabled()) {
            return java.util.Collections.emptyList();
        }
        return HuntOrbitController.buildPreviewLoop(target, getEffectiveHuntFixedDistance(),
                getConfiguredHuntOrbitSamplePoints());
    }

    private String formatVec3(double[] pos) {
        if (pos == null || pos.length < 3) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", pos[0], pos[1], pos[2]);
    }

    private double getEffectiveHuntFixedDistance() {
        return Math.max(0.5D, huntFixedDistance);
    }

    private void applyRotation(LocalPlayer player, LivingEntity target, boolean forceSmoothRotation) {
        Rotation desiredAim = getDesiredAimRotation(player, target);
        float targetYaw = desiredAim.getYaw();
        float targetPitch = desiredAim.getPitch();
        if (!forceSmoothRotation && !smoothRotation) {
            player.setYRot(targetYaw);
            player.setXRot(targetPitch);
            player.setYHeadRot(targetYaw);
            player.setYBodyRot(targetYaw);
            return;
        }

        float yawDelta = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDelta = targetPitch - player.getXRot();
        float yawSpeed = Math.max(computeTurnSpeed(Math.abs(yawDelta)), computeTrackingYawSpeedFloor(player, target));
        float pitchSpeed = Math.max(1.5F, yawSpeed * 0.75F);
        float nextYaw = player.getYRot() + clampSigned(yawDelta, yawSpeed);
        float nextPitch = Mth.clamp(player.getXRot() + clampSigned(pitchDelta, pitchSpeed), -90.0F, 90.0F);
        player.setYRot(nextYaw);
        player.setXRot(nextPitch);
        player.setYHeadRot(nextYaw);
        player.setYBodyRot(nextYaw);
    }

    private boolean shouldForceOrbitFacing(LocalPlayer player, LivingEntity target) {
        return isHuntOrbitEnabled() && canStartOrbitHunt(player, target);
    }

    private float computeTrackingYawSpeedFloor(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return minTurnSpeed;
        }
        double radiusX = target.getX() - player.getX();
        double radiusZ = target.getZ() - player.getZ();
        double horizontalDistance = Math.sqrt(radiusX * radiusX + radiusZ * radiusZ);
        if (horizontalDistance <= 1.0E-4D) {
            return minTurnSpeed;
        }
        double playerDeltaX = player.getX() - player.xo;
        double playerDeltaZ = player.getZ() - player.zo;
        double targetDeltaX = target.getX() - target.xo;
        double targetDeltaZ = target.getZ() - target.zo;
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
        return Mth.clamp((float) speedFloor, minTurnSpeed, Math.max(maxTurnSpeed, 60.0F));
    }

    private Rotation getDesiredAimRotation(LocalPlayer player, LivingEntity target) {
        Rotation desired = RotationUtils.calcRotationFromVec3d(player.getEyePosition(),
                new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.85D, target.getZ()),
                new Rotation(player.getYRot(), player.getXRot()));
        return new Rotation(applyAimYawOffset(desired.getYaw()), Mth.clamp(desired.getPitch(), -90.0F, 90.0F));
    }

    private float applyAimYawOffset(float yaw) {
        return Mth.wrapDegrees(yaw + aimYawOffset);
    }

    private float computeTurnSpeed(float yawDeltaAbs) {
        float normalized = Mth.clamp(yawDeltaAbs / 90.0F, 0.0F, 1.0F);
        return minTurnSpeed + (maxTurnSpeed - minTurnSpeed) * normalized;
    }

    private float clampSigned(float value, float maxMagnitude) {
        return Math.copySign(Math.min(Math.abs(value), Math.max(0.1F, maxMagnitude)), value);
    }

    private float getTargetSearchRadius() {
        return isHuntEnabled() ? Math.max(attackRange, huntRadius) : attackRange;
    }

    private boolean matchesEnabledTargetGroup(LivingEntity target) {
        if (target instanceof Player) {
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

    private boolean isHostileTargetType(LivingEntity target) {
        EntityType<?> type = target.getType();
        return target instanceof Enemy || target instanceof EnderDragon || type.getCategory() == MobCategory.MONSTER;
    }

    private boolean isPassiveTargetType(LivingEntity target) {
        EntityType<?> type = target.getType();
        return target instanceof Animal
                || target instanceof AmbientCreature
                || target instanceof WaterAnimal
                || target instanceof AbstractVillager
                || target instanceof IronGolem
                || target instanceof SnowGolem
                || type.getCategory() == MobCategory.CREATURE
                || type.getCategory() == MobCategory.AMBIENT
                || type.getCategory() == MobCategory.WATER_CREATURE;
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

    private boolean canTriggerAttackSequence(LocalPlayer player, LivingEntity target) {
        if (player == null || target == null) {
            return false;
        }
        if (this.sequenceCooldownTicks > 0 || this.attackSequenceExecutor.isRunning()) {
            return false;
        }
        return hasConfiguredAttackSequence() && isValidTarget(player, target);
    }

    private boolean triggerAttackSequence(LocalPlayer player, LivingEntity target) {
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

    private static String getConfiguredAttackSequenceName() {
        return attackSequenceName == null ? "" : attackSequenceName.trim();
    }

    private boolean isTeleportAttackRecoveryActive() {
        return this.activeTeleportAttackPlan != null && this.pendingTeleportReturnTicks > 0;
    }

    private void tickTeleportAttackRecovery(LocalPlayer player) {
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
                && player.tickCount != this.lastTeleportCorrectionTick) {
            this.lastTeleportCorrectionTick = player.tickCount;
            sendTeleportReturnToOrigin(player, this.activeTeleportAttackPlan,
                    player.getX(), player.getY(), player.getZ(), true);
            this.pendingTeleportReturnTicks = TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS;
            return;
        }
        clearTeleportAttackState();
    }

    private void handleTeleportCorrection(double[] correctedPosition) {
        if (correctedPosition == null || correctedPosition.length < 3) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc == null ? null : mc.player;
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
        if (plan.correctionCount >= TELEPORT_ATTACK_MAX_CORRECTIONS || player.tickCount == this.lastTeleportCorrectionTick) {
            return;
        }
        this.lastTeleportCorrectionTick = player.tickCount;
        sendTeleportReturnToOrigin(player, plan, correctedPosition[0], correctedPosition[1], correctedPosition[2], true);
        this.pendingTeleportReturnTicks = TELEPORT_ATTACK_CORRECTION_WINDOW_TICKS;
    }

    private void clearTeleportAttackState() {
        this.activeTeleportAttackPlan = null;
        this.pendingTeleportReturnTicks = 0;
        this.lastTeleportCorrectionTick = Integer.MIN_VALUE;
    }

    private boolean isPlayerNearTeleportOrigin(LocalPlayer player, TeleportAttackPlan plan) {
        return player.distanceToSqr(plan.originX, plan.originY, plan.originZ) <= TELEPORT_ATTACK_ORIGIN_TOLERANCE_SQ;
    }

    private boolean isSamePosition(double leftX, double leftY, double leftZ, double rightX, double rightY, double rightZ) {
        double dx = leftX - rightX;
        double dy = leftY - rightY;
        double dz = leftZ - rightZ;
        return dx * dx + dy * dy + dz * dz <= TELEPORT_ATTACK_ORIGIN_TOLERANCE_SQ;
    }

    private TeleportAttackPlan buildTeleportAttackPlan(LocalPlayer player, LivingEntity target) {
        TeleportAssaultCandidate assaultCandidate = findBestTeleportAssaultCandidate(player, target);
        if (assaultCandidate == null) {
            return null;
        }

        List<Vec3> outboundWaypoints = buildTeleportPathWaypoints(player,
                player.getX(), player.getY(), player.getZ(),
                assaultCandidate.x, assaultCandidate.y, assaultCandidate.z);
        List<Vec3> returnWaypoints = buildTeleportPathWaypoints(player,
                assaultCandidate.x, assaultCandidate.y, assaultCandidate.z,
                player.getX(), player.getY(), player.getZ());
        float attackYaw = shouldRotateToTarget()
                ? (float) (Math.toDegrees(Math.atan2(target.getZ() - assaultCandidate.z, target.getX() - assaultCandidate.x)) - 90.0D)
                : player.getYRot();
        double dx = target.getX() - assaultCandidate.x;
        double dz = target.getZ() - assaultCandidate.z;
        double dy = target.getY() + target.getEyeHeight() * 0.85D - (assaultCandidate.y + player.getEyeHeight());
        float attackPitch = shouldRotateToTarget()
                ? (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))))
                : player.getXRot();
        return new TeleportAttackPlan(player, target, assaultCandidate, outboundWaypoints, returnWaypoints,
                attackYaw, attackPitch);
    }

    private TeleportAssaultCandidate findBestTeleportAssaultCandidate(LocalPlayer player, LivingEntity target) {
        double preferredRadius = Math.max(1.8D, TELEPORT_ATTACK_REACH + target.getBbWidth() * 0.5D);
        double minRadius = Math.max(1.2D, preferredRadius - TELEPORT_ATTACK_MAX_RADIUS_ADJUST);
        double maxRadius = preferredRadius + TELEPORT_ATTACK_MAX_RADIUS_ADJUST;
        double preferredAngle = Math.atan2(player.getZ() - target.getZ(), player.getX() - target.getX());
        TeleportAssaultCandidate best = null;

        for (int angleStep = 0; angleStep <= TELEPORT_ATTACK_SAFE_ANGLE_STEPS; angleStep++) {
            double angle = angleStep == 0 ? preferredAngle
                    : preferredAngle + angleStep * TELEPORT_ATTACK_SAFE_ANGLE_STEP_RADIANS;
            best = findTeleportAssaultCandidateForAngle(player, target, angle, preferredRadius, minRadius, maxRadius, best);
            if (angleStep > 0) {
                best = findTeleportAssaultCandidateForAngle(player, target,
                        preferredAngle - angleStep * TELEPORT_ATTACK_SAFE_ANGLE_STEP_RADIANS,
                        preferredRadius, minRadius, maxRadius, best);
            }
        }
        return best;
    }

    private TeleportAssaultCandidate findTeleportAssaultCandidateForAngle(LocalPlayer player, LivingEntity target,
            double angle, double preferredRadius, double minRadius, double maxRadius,
            TeleportAssaultCandidate currentBest) {
        int radiusSteps = Math.max(1,
                (int) Math.ceil((maxRadius - minRadius) / Math.max(0.1D, TELEPORT_ATTACK_SAFE_RADIUS_STEP)));
        TeleportAssaultCandidate best = currentBest;
        for (int radiusStep = 0; radiusStep <= radiusSteps; radiusStep++) {
            double radius = radiusStep == 0
                    ? preferredRadius
                    : Math.min(maxRadius, preferredRadius + radiusStep * TELEPORT_ATTACK_SAFE_RADIUS_STEP);
            best = evaluateTeleportAssaultCandidate(player, target, angle, radius, preferredRadius, best);
            if (radiusStep > 0) {
                double smallerRadius = Math.max(minRadius, preferredRadius - radiusStep * TELEPORT_ATTACK_SAFE_RADIUS_STEP);
                best = evaluateTeleportAssaultCandidate(player, target, angle, smallerRadius, preferredRadius, best);
            }
        }
        return best;
    }

    private TeleportAssaultCandidate evaluateTeleportAssaultCandidate(LocalPlayer player, LivingEntity target,
            double preferredAngle, double radius, double preferredRadius, TeleportAssaultCandidate currentBest) {
        double desiredX = target.getX() + Math.cos(preferredAngle) * radius;
        double desiredZ = target.getZ() + Math.sin(preferredAngle) * radius;
        double[] safeAssaultPos = findSafeHuntNavigationDestination(player, desiredX, target.getY(), desiredZ);
        if (safeAssaultPos == null) {
            return currentBest;
        }
        BlockPos standPos = BlockPos.containing(safeAssaultPos[0], safeAssaultPos[1], safeAssaultPos[2]);
        if (!hasHuntLineOfSightFromStandPos(player, standPos, target)) {
            return currentBest;
        }

        double attackDx = target.getX() - safeAssaultPos[0];
        double attackDy = target.getY() + target.getEyeHeight() * 0.85D - (safeAssaultPos[1] + player.getEyeHeight());
        double attackDz = target.getZ() - safeAssaultPos[2];
        double attackDistance = Math.sqrt(attackDx * attackDx + attackDy * attackDy + attackDz * attackDz);
        double maxAttackDistance = Math.max(2.85D, TELEPORT_ATTACK_REACH + target.getBbWidth() * 0.8D + 0.55D);
        if (attackDistance > maxAttackDistance) {
            return currentBest;
        }

        double radiusPenalty = Math.abs(Math.sqrt((safeAssaultPos[0] - target.getX()) * (safeAssaultPos[0] - target.getX())
                + (safeAssaultPos[2] - target.getZ()) * (safeAssaultPos[2] - target.getZ())) - preferredRadius) * 4.5D;
        double score = player.distanceToSqr(safeAssaultPos[0], safeAssaultPos[1], safeAssaultPos[2]) * 0.04D
                + Math.abs(safeAssaultPos[1] - player.getY()) * 0.6D
                + radiusPenalty;
        if (currentBest == null || score < currentBest.score) {
            return new TeleportAssaultCandidate(safeAssaultPos[0], safeAssaultPos[1], safeAssaultPos[2], true, score);
        }
        return currentBest;
    }

    private List<Vec3> buildTeleportPathWaypoints(LocalPlayer player, double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ) {
        List<Vec3> waypoints = new ArrayList<>();
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
            Vec3 waypoint = new Vec3(desiredX, desiredY, desiredZ);
            if (waypoints.isEmpty() || waypoints.get(waypoints.size() - 1).distanceToSqr(waypoint) > TELEPORT_ATTACK_WAYPOINT_EPSILON_SQ) {
                waypoints.add(waypoint);
            }
        }
        return waypoints;
    }

    private void sendTeleportWaypoints(LocalPlayer player, List<Vec3> waypoints, boolean onGround) {
        if (player == null || player.connection == null || waypoints == null) {
            return;
        }
        for (Vec3 waypoint : waypoints) {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(waypoint.x, waypoint.y, waypoint.z, onGround));
        }
    }

    private void sendTeleportReturnToOrigin(LocalPlayer player, TeleportAttackPlan plan, double startX, double startY,
            double startZ, boolean correctionTriggered) {
        if (player == null || player.connection == null || plan == null) {
            return;
        }
        sendTeleportWaypoints(player, plan.returnWaypoints, plan.originOnGround);
        player.connection.send(new ServerboundMovePlayerPacket.Pos(plan.originX, plan.originY, plan.originZ, plan.originOnGround));
        player.connection.send(new ServerboundMovePlayerPacket.PosRot(plan.originX, plan.originY, plan.originZ,
                plan.originYaw, plan.originPitch, plan.originOnGround));
        if (correctionTriggered) {
            plan.correctedByServer = true;
            plan.correctionCount++;
        }
        plan.returnCompleted = false;
    }

    private int getPreferredAttackHotbarSlot(LocalPlayer player) {
        return isHoldingWeapon(player) ? player.getInventory().selected : -1;
    }

    private boolean isHoldingWeapon(LocalPlayer player) {
        ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        return held.getItem() instanceof SwordItem
                || held.getItem() instanceof AxeItem
                || held.getItem() instanceof TridentItem;
    }

    private static String getFilterableEntityName(Entity entity) {
        return entity == null ? "" : normalizeFilterName(entity.getDisplayName().getString());
    }

    private static String trimUnicodeWhitespace(String text) {
        return text == null ? "" : text.trim();
    }

    private static boolean matchesNameList(String entityName, List<String> filters) {
        return getNormalizedNameListMatchIndex(entityName, filters) != Integer.MAX_VALUE;
    }

    public static int getNameListMatchIndex(String entityName, List<String> filters) {
        return getNormalizedNameListMatchIndex(normalizeFilterName(entityName).toLowerCase(Locale.ROOT), filters);
    }

    private static int getNormalizedNameListMatchIndex(String loweredName, List<String> filters) {
        if (loweredName == null || loweredName.isEmpty() || filters == null) {
            return Integer.MAX_VALUE;
        }
        for (int i = 0; i < filters.size(); i++) {
            String entry = normalizeFilterName(filters.get(i)).toLowerCase(Locale.ROOT);
            if (!entry.isEmpty() && loweredName.contains(entry)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static List<String> normalizeNameList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String entry : source) {
            String keyword = normalizeFilterName(entry).toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty()) {
                normalized.add(keyword);
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String normalizeHuntModeValue(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (HUNT_MODE_FIXED_DISTANCE.equals(normalized)) {
            return HUNT_MODE_FIXED_DISTANCE;
        }
        if (HUNT_MODE_OFF.equals(normalized)) {
            return HUNT_MODE_OFF;
        }
        return HUNT_MODE_APPROACH;
    }

    private static String normalizePresetName(String name) {
        return trimUnicodeWhitespace(name);
    }

    private static int findPresetIndex(String name) {
        String normalizedName = normalizePresetName(name);
        if (normalizedName.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < presets.size(); i++) {
            if (normalizedName.equalsIgnoreCase(normalizePresetName(presets.get(i).name))) {
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
        preset.aimYawOffset = aimYawOffset;
        preset.huntMode = huntMode;
        preset.huntPickupItemsEnabled = huntPickupItemsEnabled;
        preset.visualizeHuntRadius = visualizeHuntRadius;
        preset.huntRadius = huntRadius;
        preset.huntFixedDistance = huntFixedDistance;
        preset.huntOrbitEnabled = huntOrbitEnabled;
        preset.huntJumpOrbitEnabled = huntJumpOrbitEnabled;
        preset.huntOrbitSamplePoints = huntOrbitSamplePoints;
        preset.enableNameWhitelist = enableNameWhitelist;
        preset.enableNameBlacklist = enableNameBlacklist;
        preset.nameWhitelist = new ArrayList<>(nameWhitelist);
        preset.nameBlacklist = new ArrayList<>(nameBlacklist);
        preset.nearbyEntityScanRange = nearbyEntityScanRange;
        preset.attackRange = attackRange;
        preset.minAttackStrength = minAttackStrength;
        preset.minTurnSpeed = minTurnSpeed;
        preset.maxTurnSpeed = maxTurnSpeed;
        preset.minAttackIntervalTicks = minAttackIntervalTicks;
        preset.targetsPerAttack = targetsPerAttack;
        return normalizePreset(preset);
    }

    private static KillAuraPreset normalizePreset(KillAuraPreset preset) {
        if (preset == null) {
            return null;
        }
        KillAuraPreset normalized = new KillAuraPreset(preset);
        normalized.name = normalizePresetName(normalized.name);
        if (normalized.name.isEmpty()) {
            return null;
        }
        normalized.attackMode = normalizeAttackModeValue(normalized.attackMode);
        normalized.attackSequenceName = normalized.attackSequenceName == null ? "" : normalized.attackSequenceName.trim();
        normalized.attackSequenceDelayTicks = Mth.clamp(normalized.attackSequenceDelayTicks, 0, 200);
        normalized.aimYawOffset = Mth.clamp(normalized.aimYawOffset, -30.0F, 30.0F);
        normalized.huntMode = normalizeHuntModeValue(normalized.huntMode);
        normalized.huntRadius = Math.max(Math.max(1.0F, normalized.attackRange), normalized.huntRadius);
        normalized.huntFixedDistance = Mth.clamp(normalized.huntFixedDistance, 0.5F, 100.0F);
        normalized.huntOrbitSamplePoints = Mth.clamp(normalized.huntOrbitSamplePoints,
                MIN_HUNT_ORBIT_SAMPLE_POINTS, MAX_HUNT_ORBIT_SAMPLE_POINTS);
        normalized.nameWhitelist = normalizeNameList(normalized.nameWhitelist);
        normalized.nameBlacklist = normalizeNameList(normalized.nameBlacklist);
        normalized.nearbyEntityScanRange = Mth.clamp(normalized.nearbyEntityScanRange, 1.0F, 64.0F);
        normalized.attackRange = Mth.clamp(normalized.attackRange, 1.0F, 100.0F);
        normalized.minAttackStrength = Mth.clamp(normalized.minAttackStrength, 0.0F, 1.0F);
        normalized.minTurnSpeed = Mth.clamp(normalized.minTurnSpeed, 1.0F, 40.0F);
        normalized.maxTurnSpeed = Mth.clamp(normalized.maxTurnSpeed, normalized.minTurnSpeed, 60.0F);
        normalized.minAttackIntervalTicks = Mth.clamp(normalized.minAttackIntervalTicks, 0, 20);
        normalized.targetsPerAttack = Mth.clamp(normalized.targetsPerAttack, 1, 50);
        normalized.fullBrightGamma = Mth.clamp(normalized.fullBrightGamma, 1.0F, 1000.0F);
        if (!normalized.targetHostile && !normalized.targetPassive && !normalized.targetPlayers) {
            normalized.targetHostile = true;
        }
        return normalized;
    }

    private static void applyPreset(KillAuraPreset preset) {
        KillAuraPreset safePreset = normalizePreset(preset);
        if (safePreset == null) {
            return;
        }
        rotateToTarget = safePreset.rotateToTarget;
        smoothRotation = safePreset.smoothRotation;
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
        attackSequenceDelayTicks = safePreset.attackSequenceDelayTicks;
        aimYawOffset = safePreset.aimYawOffset;
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

    private static void normalizeConfig() {
        attackMode = normalizeAttackModeValue(attackMode);
        attackSequenceName = getConfiguredAttackSequenceName();
        attackSequenceDelayTicks = Mth.clamp(attackSequenceDelayTicks, 0, 200);
        aimYawOffset = Mth.clamp(aimYawOffset, -30.0F, 30.0F);
        attackRange = Mth.clamp(attackRange, 1.0F, 100.0F);
        minAttackStrength = Mth.clamp(minAttackStrength, 0.0F, 1.0F);
        minTurnSpeed = Mth.clamp(minTurnSpeed, 1.0F, 40.0F);
        maxTurnSpeed = Mth.clamp(maxTurnSpeed, minTurnSpeed, 60.0F);
        minAttackIntervalTicks = Mth.clamp(minAttackIntervalTicks, 0, 20);
        targetsPerAttack = Mth.clamp(targetsPerAttack, 1, 50);
        fullBrightGamma = Mth.clamp(fullBrightGamma, 1.0F, 1000.0F);
        huntMode = normalizeHuntModeValue(huntMode);
        huntEnabled = !HUNT_MODE_OFF.equals(huntMode);
        huntRadius = Mth.clamp(Math.max(huntRadius, attackRange), attackRange, 100.0F);
        huntFixedDistance = Mth.clamp(huntFixedDistance, 0.5F, 100.0F);
        huntUpRange = Mth.clamp(huntUpRange, 0.0F, 100.0F);
        huntDownRange = Mth.clamp(huntDownRange, 0.0F, 100.0F);
        huntOrbitSamplePoints = Mth.clamp(huntOrbitSamplePoints,
                MIN_HUNT_ORBIT_SAMPLE_POINTS, MAX_HUNT_ORBIT_SAMPLE_POINTS);
        noDamageAttackLimit = Mth.clamp(noDamageAttackLimit, 0, MAX_NO_DAMAGE_ATTACK_LIMIT);
        nearbyEntityScanRange = Mth.clamp(nearbyEntityScanRange, 1.0F, 64.0F);
        nameWhitelist = normalizeNameList(nameWhitelist);
        nameBlacklist = normalizeNameList(nameBlacklist);
        if (!targetHostile && !targetPassive && !targetPlayers) {
            targetHostile = true;
        }
    }

    private static String normalizeAttackModeValue(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (ATTACK_MODE_PACKET.equals(normalized)) {
            return ATTACK_MODE_PACKET;
        }
        if (ATTACK_MODE_TELEPORT.equals(normalized)) {
            return ATTACK_MODE_TELEPORT;
        }
        if (ATTACK_MODE_SEQUENCE.equals(normalized)) {
            return ATTACK_MODE_SEQUENCE;
        }
        if (ATTACK_MODE_MOUSE_CLICK.equals(normalized)) {
            return ATTACK_MODE_MOUSE_CLICK;
        }
        return ATTACK_MODE_NORMAL;
    }

    private static void resetDefaults() {
        enabled = false;
        rotateToTarget = true;
        smoothRotation = true;
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
        attackSequenceDelayTicks = 2;
        aimYawOffset = 0.0F;
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
    }

    private static boolean readBoolean(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }

    private static int readInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    private static float readFloat(JsonObject json, String key, float defaultValue) {
        return json.has(key) ? json.get(key).getAsFloat() : defaultValue;
    }

    private static String readString(JsonObject json, String key, String defaultValue) {
        return json.has(key) ? json.get(key).getAsString() : defaultValue;
    }

    public static int getNoDamageAttackLimit() {
        return Mth.clamp(noDamageAttackLimit, 0, MAX_NO_DAMAGE_ATTACK_LIMIT);
    }

    public static void setNoDamageAttackLimit(int value) {
        noDamageAttackLimit = Mth.clamp(value, 0, MAX_NO_DAMAGE_ATTACK_LIMIT);
    }

    private static boolean isWithinConfiguredHuntVerticalRange(LocalPlayer player, Entity target) {
        if (player == null || target == null) {
            return true;
        }
        double dy = target.getY() - player.getY();
        return dy <= huntUpRange + 1.0E-6D && -dy <= huntDownRange + 1.0E-6D;
    }

    private static Vec3 getMovementHeading(LocalPlayer player) {
        if (player == null) {
            return Vec3.ZERO;
        }
        Vec3 motion = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontalSpeed > 0.05D) {
            return normalizeHorizontal(new Vec3(motion.x, 0.0D, motion.z));
        }
        return normalizeHorizontal(getInputVector(player));
    }

    private static Vec3 getInputVector(LocalPlayer player) {
        if (player == null || player.input == null) {
            return Vec3.ZERO;
        }
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        if (Math.abs(forward) < 0.01F && Math.abs(strafe) < 0.01F) {
            return Vec3.ZERO;
        }
        double rad = Math.toRadians(player.getYRot());
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(strafe * cos - forward * sin, 0.0D, forward * cos + strafe * sin);
    }

    private static Vec3 normalizeHorizontal(Vec3 vector) {
        if (vector == null) {
            return Vec3.ZERO;
        }
        double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        if (length < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return new Vec3(vector.x / length, 0.0D, vector.z / length);
    }

    private static double horizontalSpeed(LocalPlayer player) {
        Vec3 motion = player == null ? Vec3.ZERO : player.getDeltaMovement();
        return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
    }

    private static final class TargetCandidate {
        private final LivingEntity entity;
        private final double distanceSq;
        private final int whitelistPriority;
        private final int currentTargetPriority;
        private final float yawDeltaAbs;

        private TargetCandidate(LivingEntity entity, double distanceSq, int whitelistPriority,
                int currentTargetPriority, float yawDeltaAbs) {
            this.entity = entity;
            this.distanceSq = distanceSq;
            this.whitelistPriority = whitelistPriority;
            this.currentTargetPriority = currentTargetPriority;
            this.yawDeltaAbs = yawDeltaAbs;
        }
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
        private final List<Vec3> outboundWaypoints;
        private final List<Vec3> returnWaypoints;
        private boolean correctedByServer;
        private boolean returnCompleted;
        private int correctionCount;

        private TeleportAttackPlan(LocalPlayer player, LivingEntity target, TeleportAssaultCandidate assaultCandidate,
                List<Vec3> outboundWaypoints, List<Vec3> returnWaypoints, float attackYaw, float attackPitch) {
            this.originX = player.getX();
            this.originY = player.getY();
            this.originZ = player.getZ();
            this.originYaw = player.getYRot();
            this.originPitch = player.getXRot();
            this.originOnGround = player.onGround();
            this.assaultX = assaultCandidate.x;
            this.assaultY = assaultCandidate.y;
            this.assaultZ = assaultCandidate.z;
            this.attackYaw = attackYaw;
            this.attackPitch = attackPitch;
            this.outboundWaypoints = outboundWaypoints == null ? new ArrayList<>() : new ArrayList<>(outboundWaypoints);
            this.returnWaypoints = returnWaypoints == null ? new ArrayList<>() : new ArrayList<>(returnWaypoints);
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

        void start(PathSequence sourceSequence, LocalPlayer player, LivingEntity target) {
            stop();
            if (sourceSequence == null || sourceSequence.getSteps().isEmpty()) {
                return;
            }
            this.sequence = new PathSequence(sourceSequence);
            this.targetEntityId = target == null ? Integer.MIN_VALUE : target.getId();
            populateTargetVariables(player, target);
            this.runtimeVariables.enterStep(0);
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

        void tick(LocalPlayer player) {
            if (!isRunning() || player == null) {
                if (player == null) {
                    stop();
                }
                return;
            }
            refreshTargetVariables(player);
            if (this.tickDelay > 0) {
                this.tickDelay--;
                return;
            }
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
                return;
            }
            ActionData rawAction = actions.get(this.actionIndex);
            this.runtimeVariables.beginAction(this.stepIndex, this.actionIndex);
            JsonObject resolvedParams = LegacyActionRuntime.resolveParams(rawAction.params, this.runtimeVariables,
                    player, this.sequence, this.stepIndex, this.actionIndex,
                    resolveLiteralParamKeys(rawAction.type));
            String actionType = rawAction.type == null ? "" : rawAction.type.trim().toLowerCase(Locale.ROOT);
            if (shouldSkipAction(actionType)) {
                this.actionIndex++;
                return;
            }
            Consumer<LocalPlayer> action = PathSequenceManager.parseAction(rawAction.type, resolvedParams);
            this.actionIndex++;
            if (action == null) {
                return;
            }
            if (action instanceof ModUtils.DelayAction delayAction) {
                this.tickDelay = delayAction.getDelayTicks();
                return;
            }
            try {
                action.accept(player);
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("[kill_aura_sequence] 执行动作失败: {}", rawAction.getDescription(), e);
            }
            updateHeldKeyState(rawAction);
            this.tickDelay = POST_ACTION_DELAY_TICKS;
        }

        private java.util.Set<String> resolveLiteralParamKeys(String actionType) {
            String literalKey = ActionVariableRegistry.resolveVariableParamKey(actionType);
            return literalKey == null || literalKey.trim().isEmpty()
                    ? java.util.Collections.emptySet()
                    : java.util.Collections.singleton(literalKey);
        }

        private void populateTargetVariables(LocalPlayer player, LivingEntity target) {
            this.runtimeVariables.put("target_found", target != null);
            if (target != null) {
                this.runtimeVariables.put("target_name", target.getDisplayName().getString());
                this.runtimeVariables.put("target_id", target.getId());
                this.runtimeVariables.put("target_x", target.getX());
                this.runtimeVariables.put("target_y", target.getY());
                this.runtimeVariables.put("target_z", target.getZ());
                if (player != null) {
                    this.runtimeVariables.put("target_distance", player.distanceTo(target));
                }
            }
        }

        private void refreshTargetVariables(LocalPlayer player) {
            if (player == null || player.level() == null || this.targetEntityId == Integer.MIN_VALUE) {
                return;
            }
            Entity targetEntity = player.level().getEntity(this.targetEntityId);
            populateTargetVariables(player, targetEntity instanceof LivingEntity living ? living : null);
        }

        private boolean shouldSkipAction(String actionType) {
            return "run_sequence".equals(actionType)
                    || "hunt".equals(actionType)
                    || "set_var".equals(actionType)
                    || "sequence_control".equals(actionType)
                    || "goto_action".equals(actionType)
                    || "repeat_actions".equals(actionType)
                    || "capture_nearby_entity".equals(actionType)
                    || "capture_gui_title".equals(actionType)
                    || "capture_block_at".equals(actionType)
                    || actionType.startsWith("condition_")
                    || actionType.startsWith("wait_until_");
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
