package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
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
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.Rotation;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.RotationUtils;
import com.zszl.zszlScriptMod.shadowbaritone.process.KillAuraOrbitProcess;
import com.zszl.zszlScriptMod.shadowbaritone.utils.PathRenderer;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
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
import net.minecraft.util.EnumHand;
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
import java.util.function.Consumer;

public class KillAuraHandler implements AbstractGameEventListener {

    public static final KillAuraHandler INSTANCE = new KillAuraHandler();
    public static final String ATTACK_MODE_NORMAL = "NORMAL";
    public static final String ATTACK_MODE_PACKET = "PACKET";
    public static final String ATTACK_MODE_TELEPORT = "TELEPORT";
    public static final String ATTACK_MODE_SEQUENCE = "SEQUENCE";
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
    public static String attackMode = ATTACK_MODE_NORMAL;
    public static String attackSequenceName = "";
    public static int attackSequenceDelayTicks = 2;
    public static float aimYawOffset = 0.0F;
    public static boolean huntEnabled = true;
    public static String huntMode = HUNT_MODE_APPROACH;
    public static boolean huntPickupItemsEnabled = false;
    public static boolean visualizeHuntRadius = false;
    public static float huntRadius = 8.0F;
    public static float huntFixedDistance = 4.2F;
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

    private static final int HUNT_GOTO_INTERVAL_TICKS = 6;
    private static final double HUNT_GOTO_MOVE_THRESHOLD_SQ = 1.0D;
    private static final double HUNT_FIXED_DISTANCE_TOLERANCE = 0.30D;
    private static final int HUNT_ORBIT_PROCESS_REQUEST_INTERVAL_TICKS = 2;
    private static final double HUNT_ORBIT_MAX_ENTRY_DISTANCE_BUFFER = 4.0D;
    private static final double HUNT_ORBIT_MAX_ENTRY_VERTICAL_DELTA = 3.5D;
    private static final double HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER = 1.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER = 2.25D;
    private static final double HUNT_CONTINUOUS_ORBIT_LOOP_ENTRY_MAX_DISTANCE = 0.90D;
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

    private int attackCooldownTicks = 0;
    private int sequenceCooldownTicks = 0;
    private int currentTargetEntityId = -1;
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

    public static class KillAuraPreset {
        public String name = "";
        public boolean rotateToTarget = true;
        public boolean smoothRotation = true;
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
        public String attackMode = ATTACK_MODE_NORMAL;
        public String attackSequenceName = "";
        public int attackSequenceDelayTicks = 2;
        public float aimYawOffset = 0.0F;
        public String huntMode = HUNT_MODE_APPROACH;
        public boolean huntPickupItemsEnabled = false;
        public boolean visualizeHuntRadius = false;
        public float huntRadius = 8.0F;
        public float huntFixedDistance = 4.2F;
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

        public KillAuraPreset() {
        }

        public KillAuraPreset(KillAuraPreset other) {
            if (other == null) {
                return;
            }
            this.name = other.name == null ? "" : other.name;
            this.rotateToTarget = other.rotateToTarget;
            this.smoothRotation = other.smoothRotation;
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
            this.aimYawOffset = other.aimYawOffset;
            this.huntMode = other.huntMode == null ? HUNT_MODE_APPROACH : other.huntMode;
            this.huntPickupItemsEnabled = other.huntPickupItemsEnabled;
            this.visualizeHuntRadius = other.visualizeHuntRadius;
            this.huntRadius = other.huntRadius;
            this.huntFixedDistance = other.huntFixedDistance;
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
        attackMode = ATTACK_MODE_NORMAL;
        attackSequenceName = "";
        attackSequenceDelayTicks = 2;
        aimYawOffset = 0.0F;
        huntEnabled = true;
        huntMode = HUNT_MODE_APPROACH;
        huntPickupItemsEnabled = false;
        visualizeHuntRadius = false;
        huntRadius = 8.0F;
        huntFixedDistance = 4.2F;
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
                attackSequenceDelayTicks = json.get("attackSequenceDelayTicks").getAsInt();
            }
            if (json.has("aimYawOffset")) {
                aimYawOffset = json.get("aimYawOffset").getAsFloat();
            }
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

            normalizeConfig();
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
    }

    public boolean hasActiveTarget(EntityPlayerSP player) {
        if (!enabled || player == null || player.world == null || this.currentTargetEntityId == -1) {
            return false;
        }

        Entity target = player.world.getEntityByID(this.currentTargetEntityId);
        return target instanceof EntityLivingBase && isValidTarget(player, (EntityLivingBase) target);
    }

    public Optional<Rotation> getVisualTargetRotation(EntityPlayerSP player) {
        if (player == null || player.world == null || !shouldRotateToTarget() || this.currentTargetEntityId == -1) {
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
        return Optional.of(getDesiredAimRotation(player, livingTarget));
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

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null) {
            return;
        }

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
        applyFullBright(enableFullBrightVision);

        if (!enabled) {
            this.attackCooldownTicks = 0;
            this.sequenceCooldownTicks = 0;
            this.currentTargetEntityId = -1;
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
            return;
        }

        if (player.isDead || player.getHealth() <= 0.0F || player.isSpectator()) {
            this.currentTargetEntityId = -1;
            stopHuntPickupNavigation();
            stopHuntNavigation();
            this.attackSequenceExecutor.stop();
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

        boolean autoPickupRulePriority = AutoPickupHandler.INSTANCE.shouldPrioritizeNavigation(player);
        boolean autoPickupRuleAreaActive = AutoPickupHandler.INSTANCE.isPlayerInsideEnabledRule(player);
        EntityItem huntPriorityPickupItem = (!autoPickupRuleAreaActive && isHuntEnabled() && huntPickupItemsEnabled)
                ? findHuntPriorityPickupItem(player)
                : null;

        List<EntityLivingBase> targets = findTargets(player);
        if (targets.isEmpty()) {
            this.currentTargetEntityId = -1;
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
        boolean orbitFacingActive = shouldForceOrbitFacing(player, primaryTarget);

        if (shouldRotateToTarget() || orbitFacingActive) {
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
            if (canTriggerAttackSequence(player, primaryTarget) && triggerAttackSequence(player, primaryTarget)) {
                this.sequenceCooldownTicks = attackSequenceDelayTicks;
            }
            return;
        }

        if (aimOnlyMode) {
            return;
        }

        if (canStartAttack(player) && mc.playerController != null) {
            int attackedCount = attackTargets(mc, player, targets);
            if (attackedCount > 0) {
                player.swingArm(EnumHand.MAIN_HAND);
                this.attackCooldownTicks = minAttackIntervalTicks;
                if (!isHuntOrbitEnabled()) {
                    stopHuntNavigation();
                }
            }
        }
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
        if (!enabled) {
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
        List<EntityLivingBase> targets = new ArrayList<>();
        EntityLivingBase lockedTarget = null;
        double targetSearchRadius = getTargetSearchRadius();
        double targetSearchRadiusSq = targetSearchRadius * targetSearchRadius;
        boolean useWhitelistPriority = enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty();
        boolean preferStableOrbitTarget = isHuntOrbitEnabled();
        int previousTargetEntityId = this.currentTargetEntityId;

        if (focusSingleTarget && this.currentTargetEntityId != -1) {
            Entity existing = player.world.getEntityByID(this.currentTargetEntityId);
            if (existing instanceof EntityLivingBase
                    && isTrackableTarget(player, (EntityLivingBase) existing, targetSearchRadiusSq, useWhitelistPriority)) {
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
                    shouldAllowHuntTrackingWithoutLineOfSight());
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
            int distanceCompare = Double.compare(left.distanceSq, right.distanceSq);
            if (distanceCompare != 0) {
                return distanceCompare;
            }
            return Integer.compare(left.entity.getEntityId(), right.entity.getEntityId());
        });

        for (TargetCandidate nearbyTarget : nearbyTargets) {
            targets.add(nearbyTarget.entity);
        }
        this.currentTargetEntityId = targets.isEmpty() ? -1 : targets.get(0).getEntityId();
        return targets;
    }

    private boolean isValidTarget(EntityPlayerSP player, EntityLivingBase target) {
        double targetSearchRadius = getTargetSearchRadius();
        return buildTargetCandidate(player, target, targetSearchRadius * targetSearchRadius,
                enableNameWhitelist && nameWhitelist != null && !nameWhitelist.isEmpty(), false, false) != null;
    }

    private boolean isTrackableTarget(EntityPlayerSP player, EntityLivingBase target, double targetSearchRadiusSq,
            boolean useWhitelistPriority) {
        return buildTargetCandidate(player, target, targetSearchRadiusSq, useWhitelistPriority, false,
                shouldAllowHuntTrackingWithoutLineOfSight()) != null;
    }

    private TargetCandidate buildTargetCandidate(EntityPlayerSP player, EntityLivingBase target, double targetSearchRadiusSq,
            boolean useWhitelistPriority, boolean isCurrentTarget, boolean ignoreLineOfSightRequirement) {
        if (target == null || target == player) {
            return null;
        }
        if (target.isDead || target.getHealth() <= 0.0F) {
            return null;
        }
        if (target instanceof EntityArmorStand) {
            return null;
        }
        if (ignoreInvisible && target.isInvisible()) {
            return null;
        }
        double distanceSq = player.getDistanceSq(target);
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
        float yawDeltaAbs = Math.abs(MathHelper.wrapDegrees(getDesiredAimRotation(player, target).getYaw() - player.rotationYaw));
        return new TargetCandidate(target, distanceSq, useWhitelistPriority ? whitelistPriority : 0,
                isCurrentTarget ? 0 : 1, yawDeltaAbs);
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

    private int attackTargets(Minecraft mc, EntityPlayerSP player, List<EntityLivingBase> targets) {
        if (mc == null || player == null || targets == null || targets.isEmpty()) {
            return 0;
        }

        int attackLimit = Math.max(1, targetsPerAttack);
        int attackedCount = 0;
        for (EntityLivingBase target : targets) {
            if (attackedCount >= attackLimit) {
                break;
            }
            if (!canAttackTarget(player, target)) {
                continue;
            }

            if (shouldUseTeleportAttack(player, target)) {
                if (!performTeleportAttack(player, target)) {
                    continue;
                }
            } else if (isPacketAttackMode()) {
                player.connection.sendPacket(new CPacketUseEntity(target));
            } else {
                mc.playerController.attackEntity(player, target);
            }
            attackedCount++;
        }
        return attackedCount;
    }

    private boolean canAttackTarget(EntityPlayerSP player, EntityLivingBase target) {
        if (target == null || target.isDead || target.getHealth() <= 0.0F) {
            return false;
        }
        if (!isValidTarget(player, target)) {
            return false;
        }
        if (requireLineOfSight && !player.canEntityBeSeen(target)) {
            return false;
        }
        if (player.getDistanceSq(target) > attackRange * attackRange) {
            return false;
        }

        float yawDiff = Math.abs(MathHelper.wrapDegrees(getDesiredAimRotation(player, target).getYaw() - player.rotationYaw));
        if (shouldRotateToTarget() && yawDiff > 100.0F) {
            return false;
        }
        return true;
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

    private boolean performTeleportAttack(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null || player.connection == null || isTeleportAttackRecoveryActive()) {
            return false;
        }

        TeleportAttackPlan plan = buildTeleportAttackPlan(player, target);
        if (plan == null) {
            return false;
        }

        sendTeleportWaypoints(player, plan.outboundWaypoints, plan.originOnGround);
        if (shouldRotateToTarget()) {
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

    private TeleportAttackPlan buildTeleportAttackPlan(EntityPlayerSP player, EntityLivingBase target) {
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

        float attackYaw = shouldRotateToTarget()
                ? getTargetYawFromPosition(assaultCandidate.x, assaultCandidate.z, target)
                : player.rotationYaw;
        float attackPitch = shouldRotateToTarget()
                ? getTargetPitchFromPosition(assaultCandidate.x, assaultCandidate.y, assaultCandidate.z, target)
                : player.rotationPitch;

        return new TeleportAttackPlan(player, target, assaultCandidate, outboundWaypoints, returnWaypoints,
                attackYaw, attackPitch);
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
        Rotation desiredAim = getDesiredAimRotation(player, target);
        float targetYaw = desiredAim.getYaw();
        float targetPitch = desiredAim.getPitch();

        if (!forceSmoothRotation && !smoothRotation) {
            player.rotationYaw = targetYaw;
            player.rotationPitch = targetPitch;
            player.rotationYawHead = targetYaw;
            player.renderYawOffset = targetYaw;
            return;
        }

        float yawDelta = MathHelper.wrapDegrees(targetYaw - player.rotationYaw);
        float pitchDelta = targetPitch - player.rotationPitch;
        float yawSpeed = Math.max(computeTurnSpeed(Math.abs(yawDelta)), computeTrackingYawSpeedFloor(player, target));
        float pitchSpeed = Math.max(1.5F, yawSpeed * 0.75F);

        float nextYaw = player.rotationYaw + clampSigned(yawDelta, yawSpeed);
        float nextPitch = player.rotationPitch + clampSigned(pitchDelta, pitchSpeed);
        nextPitch = MathHelper.clamp(nextPitch, -90.0F, 90.0F);

        player.rotationYaw = nextYaw;
        player.rotationPitch = nextPitch;
        player.rotationYawHead = nextYaw;
        player.renderYawOffset = nextYaw;
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
        if (player == null || target == null) {
            return new Rotation(0.0F, 0.0F);
        }
        if (!shouldUseMotionCompensatedVisualAim(player, target)) {
            return new Rotation(applyAimYawOffset(getTargetYaw(player, target)), getTargetPitch(player, target));
        }

        float partialTicks = getCurrentAimPartialTicks();
        Vec3d eyePos = player.getPositionEyes(partialTicks);
        double targetX = interpolateAimCoordinate(target.lastTickPosX, target.posX, partialTicks);
        double targetY = interpolateAimCoordinate(target.lastTickPosY, target.posY, partialTicks)
                + target.getEyeHeight() * 0.85D;
        double targetZ = interpolateAimCoordinate(target.lastTickPosZ, target.posZ, partialTicks);
        Rotation desired = RotationUtils.calcRotationFromVec3d(eyePos, new Vec3d(targetX, targetY, targetZ),
                new Rotation(player.rotationYaw, player.rotationPitch));
        return new Rotation(applyAimYawOffset(desired.getYaw()), MathHelper.clamp(desired.getPitch(), -90.0F, 90.0F));
    }

    private float applyAimYawOffset(float yaw) {
        return MathHelper.wrapDegrees(yaw + aimYawOffset);
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

    private float computeTurnSpeed(float yawDeltaAbs) {
        float normalized = MathHelper.clamp(yawDeltaAbs / 90.0F, 0.0F, 1.0F);
        return minTurnSpeed + (maxTurnSpeed - minTurnSpeed) * normalized;
    }

    private float clampSigned(float value, float maxMagnitude) {
        return Math.copySign(Math.min(Math.abs(value), Math.max(0.1F, maxMagnitude)), value);
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

    private boolean shouldRotateToTarget() {
        return aimOnlyMode || (!isPacketAttackMode() && rotateToTarget);
    }

    private boolean canTriggerAttackSequence(EntityPlayerSP player, EntityLivingBase target) {
        if (player == null || target == null) {
            return false;
        }
        if (this.sequenceCooldownTicks > 0 || this.attackSequenceExecutor.isRunning()) {
            return false;
        }
        if (!hasConfiguredAttackSequence()) {
            return false;
        }
        return isValidTarget(player, target);
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

    private static String getConfiguredAttackSequenceName() {
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
        return orbitProcess.isActive();
    }

    private boolean isHuntOrbitProcessActive() {
        if (!isHuntOrbitEnabled()) {
            return false;
        }
        KillAuraOrbitProcess orbitProcess = getKillAuraOrbitProcess();
        return orbitProcess != null && orbitProcess.isActive();
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
            stopHuntOrbitProcess();
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
        double maxEntryDistance = Math.max(getEffectiveHuntFixedDistance() + HUNT_CONTINUOUS_ORBIT_ENTRY_BUFFER,
                attackRange + 0.9D);
        double allowedDistance = this.huntOrbitController.isActive()
                ? maxEntryDistance + HUNT_CONTINUOUS_ORBIT_EXIT_BUFFER
                : maxEntryDistance;
        return player.getDistanceSq(target) <= allowedDistance * allowedDistance;
    }

    private boolean shouldUseContinuousOrbitController(EntityPlayerSP player, EntityLivingBase target) {
        if (!huntJumpOrbitEnabled) {
            return false;
        }
        if (!isHuntOrbitSampleCountAtMaximum()) {
            return false;
        }
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

    private double[] findHuntNavigationDestinationAroundTarget(EntityPlayerSP player, EntityLivingBase target,
            double preferredRadius, double minRadius, double maxRadius) {
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
                        clippedDestination[1]);
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
        if (player == null || player.world == null) {
            return null;
        }

        int baseX = MathHelper.floor(desiredX);
        int baseY = MathHelper.floor(desiredY);
        int baseZ = MathHelper.floor(desiredZ);
        BlockPos bestStandPos = null;
        double bestScore = Double.MAX_VALUE;

        for (int radius = 0; radius <= 2; radius++) {
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
        boolean hasGround = belowState.getMaterial().blocksMovement();
        return feetPassable && headPassable && hasGround;
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
        preset.nameWhitelist = new ArrayList<>(nameWhitelist == null ? new ArrayList<>() : nameWhitelist);
        preset.nameBlacklist = new ArrayList<>(nameBlacklist == null ? new ArrayList<>() : nameBlacklist);
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
        String normalizedName = normalizePresetName(preset.name);
        if (normalizedName.isEmpty()) {
            return null;
        }
        KillAuraPreset normalizedPreset = new KillAuraPreset(preset);
        normalizedPreset.name = normalizedName;
        normalizedPreset.nameWhitelist = normalizeNameList(normalizedPreset.nameWhitelist);
        normalizedPreset.nameBlacklist = normalizeNameList(normalizedPreset.nameBlacklist);
        normalizedPreset.attackSequenceName = normalizedPreset.attackSequenceName == null
                ? ""
                : normalizedPreset.attackSequenceName.trim();
        normalizedPreset.fullBrightGamma = MathHelper.clamp(normalizedPreset.fullBrightGamma, 1.0F, 1000.0F);
        normalizedPreset.attackRange = MathHelper.clamp(normalizedPreset.attackRange, 1.0F, 100.0F);
        normalizedPreset.minAttackStrength = MathHelper.clamp(normalizedPreset.minAttackStrength, 0.0F, 1.0F);
        normalizedPreset.minTurnSpeed = MathHelper.clamp(normalizedPreset.minTurnSpeed, 1.0F, 40.0F);
        normalizedPreset.maxTurnSpeed = MathHelper.clamp(normalizedPreset.maxTurnSpeed,
                normalizedPreset.minTurnSpeed, 60.0F);
        normalizedPreset.minAttackIntervalTicks = MathHelper.clamp(normalizedPreset.minAttackIntervalTicks, 0, 20);
        normalizedPreset.targetsPerAttack = MathHelper.clamp(normalizedPreset.targetsPerAttack, 1, 50);
        normalizedPreset.attackSequenceDelayTicks = MathHelper.clamp(normalizedPreset.attackSequenceDelayTicks, 0, 200);
        normalizedPreset.aimYawOffset = MathHelper.clamp(normalizedPreset.aimYawOffset, -30.0F, 30.0F);
        normalizedPreset.huntRadius = MathHelper.clamp(normalizedPreset.huntRadius, normalizedPreset.attackRange, 100.0F);
        normalizedPreset.huntFixedDistance = MathHelper.clamp(normalizedPreset.huntFixedDistance, 0.5F, 100.0F);
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
        } else {
            normalizedPreset.attackMode = ATTACK_MODE_NORMAL;
        }
        normalizedPreset.huntMode = normalizeHuntModeValue(normalizedPreset.huntMode);
        if (normalizedPreset.aimOnlyMode) {
            normalizedPreset.attackMode = ATTACK_MODE_SEQUENCE;
        } else if (ATTACK_MODE_PACKET.equals(normalizedPreset.attackMode)) {
            normalizedPreset.rotateToTarget = false;
            normalizedPreset.smoothRotation = false;
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
        minTurnSpeed = MathHelper.clamp(minTurnSpeed, 1.0F, 40.0F);
        maxTurnSpeed = MathHelper.clamp(maxTurnSpeed, minTurnSpeed, 60.0F);
        minAttackIntervalTicks = MathHelper.clamp(minAttackIntervalTicks, 0, 20);
        targetsPerAttack = MathHelper.clamp(targetsPerAttack, 1, 50);
        attackSequenceDelayTicks = MathHelper.clamp(attackSequenceDelayTicks, 0, 200);
        aimYawOffset = MathHelper.clamp(aimYawOffset, -30.0F, 30.0F);
        huntRadius = MathHelper.clamp(huntRadius, attackRange, 100.0F);
        huntFixedDistance = MathHelper.clamp(huntFixedDistance, 0.5F, 100.0F);
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
        } else {
            attackMode = ATTACK_MODE_NORMAL;
        }

        if (aimOnlyMode) {
            attackMode = ATTACK_MODE_SEQUENCE;
        } else if (ATTACK_MODE_PACKET.equals(attackMode)) {
            rotateToTarget = false;
            smoothRotation = false;
        }

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
