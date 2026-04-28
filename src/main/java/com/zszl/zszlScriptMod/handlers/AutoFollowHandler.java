package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zszl.zszlScriptMod.compat.render.WorldGizmoRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.system.AutoFollowRule;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoFollowHandler {

    public static final AutoFollowHandler INSTANCE = new AutoFollowHandler();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Minecraft MC = Minecraft.getInstance();
    private static final String CATEGORY_DEFAULT = "默认";
    private static final Random RANDOM = new Random();

    public static final double DETECTION_RADIUS = 5.0D;

    private static final int HUNT_GOTO_INTERVAL_TICKS = 20;
    private static final double HUNT_GOTO_MOVE_THRESHOLD_SQ = 1.0D;
    private static final double HUNT_FIXED_DISTANCE_TOLERANCE = 0.35D;
    private static final int MONSTER_SCORE_SCAN_INTERVAL_TICKS = 4;

    private static final int STUCK_CHECK_INTERVAL = 40;
    private static final double STUCK_DISTANCE_THRESHOLD_SQ = 0.01D;
    private static final double AVOIDANCE_SIDESTEP_SAFETY_MARGIN = 1.5D;
    private static final double AVOIDANCE_FORWARD_SAFETY_MARGIN = 1.5D;
    private static final double ESCAPE_DISTANCE = 3.5D;
    private static final long AVOIDANCE_COOLDOWN_MS = 2500L;

    private static final int COMMAND_DELAY_TICKS = 3;
    private static final int RETURN_STUCK_RESTART_TICKS = 60;
    private static final long OUT_OF_BOUNDS_NOTIFY_COOLDOWN_MS = 2000L;

    public static final List<AutoFollowRule> rules = new CopyOnWriteArrayList<>();
    private static final List<String> categories = new CopyOnWriteArrayList<>();
    private static final List<ScoredMonsterInfo> lastScoredMonsters = new CopyOnWriteArrayList<>();

    public static boolean antiStuckEnabled = false;
    public static boolean avoidVinesProactively = false;
    public static double vineAvoidanceDistance = 2.0D;
    public static boolean timeoutReloadEnabled = false;
    public static int timeoutReloadSeconds = 60;
    public static boolean isMovingToPoint = false;

    private static AutoFollowRule activeRule;
    private static String lastQuickToggleRuleName = "";

    private static Entity huntTargetEntity;
    private static int lastHuntGotoTick = -99999;
    private static int lastHuntTargetEntityId = Integer.MIN_VALUE;
    private static Vec3 lastHuntTargetPos = null;
    private static boolean huntMovementStopped = false;

    private static int lastMonsterScoreScanTick = -99999;

    private static int timeoutTicksCounter = 0;
    private static Vec3 lastTimeoutCheckPosition = null;

    private static int stuckCheckCounter = 0;
    private static Vec3 lastStuckCheckPosition = null;

    private static Vec3 lastTickPlayerPos = null;
    private static long lastAvoidanceTime = 0L;
    private static Vec3 escapeDestination = null;
    private static boolean centerReturnCommandIssued = false;
    private static boolean navigationIssuedByAutoFollow = false;
    private static boolean returningToCenterFromOutOfBounds = false;
    private static boolean isSuspendedDueToDistance = false;
    private static boolean outOfRecoveryRangeSequenceTriggered = false;
    private static int returnStuckTicks = 0;
    private static Vec3 lastReturnProgressCheckPosition = null;
    private static long lastOutOfBoundsNotifyMs = 0L;

    private static int currentReturnPointIndex = 0;
    private static boolean patrolWaitingAtPoint = false;
    private static long patrolWaitStartMs = 0L;
    private static Point randomPatrolPoint = null;

    private AutoFollowHandler() {
    }

    public static class Point {
        public double x;
        public double z;

        public Point(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    public static class ScoredMonsterInfo {
        public int entityId;
        public String name = "";
        public double totalScore;
        public double distanceScore;
        public double visibilityScore;
        public double reachabilityScore;
        public double verticalScore;
        public double lockBonusScore;
        public double distance;
        public double verticalDiff;
        public boolean visible;
        public boolean reachable;
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("autofollow_rules.json");
    }

    public static synchronized void loadFollowConfig() {
        rules.clear();
        categories.clear();
        replaceLastScoredMonsters(Collections.emptyList());
        activeRule = null;

        antiStuckEnabled = false;
        avoidVinesProactively = false;
        vineAvoidanceDistance = 2.0D;
        timeoutReloadEnabled = false;
        timeoutReloadSeconds = 60;
        isMovingToPoint = false;
        lastQuickToggleRuleName = "";

        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonElement parsed = new JsonParser().parse(reader);
                JsonArray ruleArray = null;

                if (parsed != null && parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    antiStuckEnabled = readBoolean(root, "antiStuckEnabled", antiStuckEnabled);
                    avoidVinesProactively = readBoolean(root, "avoidVinesProactively", avoidVinesProactively);
                    vineAvoidanceDistance = readDouble(root, "vineAvoidanceDistance", vineAvoidanceDistance);
                    timeoutReloadEnabled = readBoolean(root, "timeoutReloadEnabled", timeoutReloadEnabled);
                    timeoutReloadSeconds = readInt(root, "timeoutReloadSeconds", timeoutReloadSeconds);

                    if (root.has("categories") && root.get("categories").isJsonArray()) {
                        for (JsonElement element : root.getAsJsonArray("categories")) {
                            if (element != null && element.isJsonPrimitive()) {
                                categories.add(normalizeCategory(element.getAsString()));
                            }
                        }
                    }
                    if (root.has("rules") && root.get("rules").isJsonArray()) {
                        ruleArray = root.getAsJsonArray("rules");
                    }
                } else if (parsed != null && parsed.isJsonArray()) {
                    ruleArray = parsed.getAsJsonArray();
                }

                if (ruleArray != null) {
                    Type listType = new TypeToken<List<AutoFollowRule>>() {
                    }.getType();
                    List<AutoFollowRule> loadedRules = GSON.fromJson(ruleArray, listType);
                    if (loadedRules != null) {
                        for (AutoFollowRule rule : loadedRules) {
                            if (rule == null) {
                                continue;
                            }
                            sanitizeRule(rule);
                            rules.add(rule);
                            if (rule.enabled && activeRule == null) {
                                activeRule = rule;
                                lastQuickToggleRuleName = safe(rule.name).trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("加载自动追怪规则失败", e);
            }
        }

        ensureCategoriesSynced();
        resetPatrolState();
    }

    public static synchronized void saveFollowConfig() {
        ensureCategoriesSynced();

        JsonObject root = new JsonObject();
        root.addProperty("antiStuckEnabled", antiStuckEnabled);
        root.addProperty("avoidVinesProactively", avoidVinesProactively);
        root.addProperty("vineAvoidanceDistance", vineAvoidanceDistance);
        root.addProperty("timeoutReloadEnabled", timeoutReloadEnabled);
        root.addProperty("timeoutReloadSeconds", timeoutReloadSeconds);
        root.add("categories", GSON.toJsonTree(new ArrayList<>(categories)));
        root.add("rules", GSON.toJsonTree(snapshotRules()));

        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动追怪规则失败", e);
        }
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        ensureCategoriesSynced();
        return new ArrayList<>(categories);
    }

    public static synchronized boolean addCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        if (containsCategoryIgnoreCase(normalized)) {
            return false;
        }
        categories.add(normalized);
        saveFollowConfig();
        return true;
    }

    public static synchronized boolean renameCategory(String oldCategory, String newCategory) {
        String normalizedOld = normalizeCategory(oldCategory);
        String normalizedNew = normalizeCategory(newCategory);
        ensureCategoriesSynced();

        if (normalizedOld.equalsIgnoreCase(normalizedNew)) {
            return true;
        }
        if (containsCategoryIgnoreCase(normalizedNew)) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < categories.size(); i++) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalizedOld)) {
                categories.set(i, normalizedNew);
                changed = true;
            }
        }
        for (AutoFollowRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalizedOld)) {
                rule.category = normalizedNew;
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        ensureCategoriesSynced();
        saveFollowConfig();
        return true;
    }

    public static synchronized boolean deleteCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();

        boolean changed = false;
        for (int i = categories.size() - 1; i >= 0; i--) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalized)) {
                categories.remove(i);
                changed = true;
            }
        }
        for (AutoFollowRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalized)) {
                rule.category = CATEGORY_DEFAULT;
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        ensureCategoriesSynced();
        saveFollowConfig();
        return true;
    }

    public static synchronized void replaceCategoryOrder(List<String> orderedCategories) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (orderedCategories != null) {
            for (String category : orderedCategories) {
                normalized.add(normalizeCategory(category));
            }
        }
        for (AutoFollowRule rule : rules) {
            if (rule != null) {
                normalized.add(normalizeCategory(rule.category));
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        categories.clear();
        categories.addAll(normalized);
        saveFollowConfig();
    }

    public static synchronized AutoFollowRule getActiveRule() {
        return activeRule;
    }

    public static synchronized boolean hasAnyRuleConfigured() {
        return !rules.isEmpty();
    }

    public static synchronized AutoFollowRule toggleEnabledFromQuickSwitch() {
        if (activeRule != null && activeRule.enabled) {
            lastQuickToggleRuleName = safe(activeRule.name).trim();
            setActiveRule(null);
            return null;
        }

        AutoFollowRule preferred = findRuleByName(lastQuickToggleRuleName);
        if (preferred == null) {
            List<AutoFollowRule> snapshot = snapshotRules();
            preferred = snapshot.isEmpty() ? null : snapshot.get(0);
        }
        if (preferred == null) {
            return null;
        }

        setActiveRule(preferred);
        return preferred;
    }

    public static synchronized void setActiveRule(AutoFollowRule ruleToActivate) {
        for (AutoFollowRule rule : rules) {
            if (rule != null) {
                rule.enabled = false;
            }
        }

        if (ruleToActivate != null && rules.contains(ruleToActivate)) {
            sanitizeRule(ruleToActivate);
            ruleToActivate.enabled = true;
            activeRule = ruleToActivate;
            lastQuickToggleRuleName = safe(ruleToActivate.name).trim();
            reloadState(false);
        } else {
            activeRule = null;
            resetPatrolState();
        }

        saveFollowConfig();
    }

    public static synchronized boolean hasActiveLockChaseRestriction() {
        return activeRule != null && activeRule.enabled;
    }

    public static synchronized boolean isPositionWithinActiveLockChaseBounds(double x, double z) {
        if (!hasActiveLockChaseRestriction()) {
            return true;
        }
        activeRule.updateBounds();
        return isPositionWithinLockChaseBounds(activeRule, x, z);
    }

    public static synchronized List<ScoredMonsterInfo> getLastScoredMonstersSnapshot() {
        if (lastScoredMonsters.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(lastScoredMonsters);
    }

    public static synchronized void reloadState(boolean showMessage) {
        if (activeRule == null) {
            return;
        }

        sanitizeRule(activeRule);
        isMovingToPoint = true;
        returningToCenterFromOutOfBounds = false;
        escapeDestination = null;
        isSuspendedDueToDistance = false;
        stuckCheckCounter = 0;
        lastStuckCheckPosition = null;
        timeoutTicksCounter = 0;
        lastTimeoutCheckPosition = null;
        huntTargetEntity = null;
        lastHuntGotoTick = -99999;
        lastHuntTargetEntityId = Integer.MIN_VALUE;
        lastHuntTargetPos = null;
        huntMovementStopped = false;
        centerReturnCommandIssued = false;
        outOfRecoveryRangeSequenceTriggered = false;
        returnStuckTicks = 0;
        lastReturnProgressCheckPosition = null;
        patrolWaitingAtPoint = false;
        patrolWaitStartMs = 0L;
        randomPatrolPoint = null;
        lastMonsterScoreScanTick = -99999;
        replaceLastScoredMonsters(Collections.emptyList());

        if (MC.player != null) {
            currentReturnPointIndex = getNearestReturnPointIndex(MC.player.getX(), MC.player.getZ());
        } else {
            currentReturnPointIndex = selectInitialReturnPointIndex();
        }

        stopNavigationStatic("重载自动追怪状态前停止旧导航");
        startMoveToCurrentReturnPoint("重载后重新前往当前回归点");

        if (showMessage && MC.player != null) {
            MC.player.displayClientMessage(new TextComponentString("§b[自动追怪] §a已重载！正在返回回归点并重新开始追怪..."), false);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LocalPlayer player) || event.player != MC.player) {
            return;
        }
        if (player.level() == null) {
            clearRuntimeStateIfNeeded(true, false);
            return;
        }

        AutoFollowRule rule = activeRule;
        if (rule == null || !rule.enabled) {
            clearRuntimeStateIfNeeded(true, false);
            return;
        }

        sanitizeRule(rule);
        Vec3 currentPos = player.position();

        if (PathSequenceEventListener.instance.isTracking() || AutoEscapeHandler.isEmergencyLockActive()) {
            clearRuntimeState(false, false);
            lastTickPlayerPos = currentPos;
            return;
        }

        double playerX = player.getX();
        double playerZ = player.getZ();

        if (isMovingToPoint) {
            handleMoveToPointState(player, currentPos, rule);
            lastTickPlayerPos = currentPos;
            return;
        }

        if (isPlayerWithinRuleBounds(player, rule)) {
            handleInBoundsState(player, currentPos, rule);
        } else {
            handleOutOfBoundsState(player, currentPos, rule);
        }

        lastTickPlayerPos = currentPos;
    }

    private void handleMoveToPointState(LocalPlayer player, Vec3 currentPos, AutoFollowRule rule) {
        timeoutTicksCounter = 0;
        lastTimeoutCheckPosition = null;

        if (returningToCenterFromOutOfBounds && escapeDestination == null) {
            Point returnPoint = getCurrentReturnPoint();
            double distToCenter = distanceToPoint(player.getX(), player.getZ(), returnPoint);
            if (distToCenter <= rule.maxRecoveryDistance) {
                if (lastReturnProgressCheckPosition != null
                        && currentPos.distanceToSqr(lastReturnProgressCheckPosition) < STUCK_DISTANCE_THRESHOLD_SQ) {
                    returnStuckTicks++;
                    if (returnStuckTicks >= RETURN_STUCK_RESTART_TICKS) {
                        player.displayClientMessage(new TextComponentString("§b[自动追怪] §e在回归过程中超过3秒未动，正在重载状态。"), false);
                        reloadState(false);
                        return;
                    }
                } else {
                    returnStuckTicks = 0;
                }
                lastReturnProgressCheckPosition = currentPos;
            } else {
                returnStuckTicks = 0;
                lastReturnProgressCheckPosition = null;
            }
        } else {
            returnStuckTicks = 0;
            lastReturnProgressCheckPosition = null;
        }

        if (escapeDestination == null && !shouldYieldChaseToKillAura(player)) {
            Entity candidate = findNearestMonsterInRule(player);
            if (candidate != null) {
                isMovingToPoint = false;
                returningToCenterFromOutOfBounds = false;
                centerReturnCommandIssued = false;
                patrolWaitingAtPoint = false;
                patrolWaitStartMs = 0L;
                huntTargetEntity = candidate;
                lastHuntTargetEntityId = Integer.MIN_VALUE;
                lastHuntTargetPos = null;
                huntMovementStopped = false;
                returnStuckTicks = 0;
                lastReturnProgressCheckPosition = null;
                handleBoundedMonsterChase(player, rule);
                return;
            }
        }

        if (escapeDestination != null) {
            if (arriveAt(player, escapeDestination.x, Double.NaN, escapeDestination.z, 1.5D)) {
                isMovingToPoint = false;
                returningToCenterFromOutOfBounds = false;
                escapeDestination = null;
                stopNavigationStatic("已抵达规避落点，停止当前回避导航");
                huntTargetEntity = null;
                lastHuntTargetEntityId = Integer.MIN_VALUE;
                lastHuntTargetPos = null;
                huntMovementStopped = false;
                centerReturnCommandIssued = false;
                returnStuckTicks = 0;
                lastReturnProgressCheckPosition = null;
            }
            return;
        }

        Point returnPoint = getCurrentReturnPoint();
        if (arriveAt(player, returnPoint.x, Double.NaN, returnPoint.z, getReturnArriveDistance(rule))) {
            isMovingToPoint = false;
            returningToCenterFromOutOfBounds = false;
            stopNavigationStatic("已抵达回归点，停止返回导航并进入停留阶段");
            huntTargetEntity = null;
            lastHuntTargetEntityId = Integer.MIN_VALUE;
            lastHuntTargetPos = null;
            huntMovementStopped = false;
            centerReturnCommandIssued = false;
            returnStuckTicks = 0;
            lastReturnProgressCheckPosition = null;
            patrolWaitingAtPoint = true;
            patrolWaitStartMs = System.currentTimeMillis();
            player.displayClientMessage(new TextComponentString("§b[自动追怪] §a已到达回归点，开始停留。"), false);
        }
    }

    private void handleInBoundsState(LocalPlayer player, Vec3 currentPos, AutoFollowRule rule) {
        if (handleTimeoutReload(player)) {
            return;
        }

        Point returnPoint = getCurrentReturnPoint();
        double distToCenter = distanceToPoint(player.getX(), player.getZ(), returnPoint);
        if (distToCenter > rule.maxRecoveryDistance) {
            if (!isSuspendedDueToDistance) {
                isSuspendedDueToDistance = true;
                stopNavigationStatic("距离当前回归点过远，暂停自动追怪导航");
                player.displayClientMessage(new TextComponentString(
                        "§b[自动追怪] §e距离当前回归点过远，逻辑已暂停。进入 "
                                + (int) rule.maxRecoveryDistance + " 格范围后将自动恢复。"), false);
            }
            return;
        }

        if (isSuspendedDueToDistance) {
            isSuspendedDueToDistance = false;
            player.displayClientMessage(new TextComponentString("§b[自动追怪] §a已返回有效范围，恢复追怪。"), false);
            huntTargetEntity = null;
            lastHuntTargetEntityId = Integer.MIN_VALUE;
            lastHuntTargetPos = null;
            huntMovementStopped = false;
        }

        if (antiStuckEnabled && System.currentTimeMillis() - lastAvoidanceTime > AVOIDANCE_COOLDOWN_MS) {
            if (handleGeneralStuckCondition(player, rule)) {
                return;
            }

            if (avoidVinesProactively) {
                Vec3 travelVec = getHorizontalTravelVector(currentPos);
                if (travelVec != null && isNearHazard(player, player.blockPosition(), vineAvoidanceDistance) != null) {
                    resolveHazardZoneAvoidance(player, travelVec);
                    return;
                }
            }
        }

        handleBoundedMonsterChase(player, rule);
    }

    private void handleOutOfBoundsState(LocalPlayer player, Vec3 currentPos, AutoFollowRule rule) {
        if (huntTargetEntity != null && huntTargetEntity.isAlive() && isEntityWithinLockChaseBounds(huntTargetEntity)) {
            centerReturnCommandIssued = false;
            patrolWaitingAtPoint = false;
            patrolWaitStartMs = 0L;
            handleBoundedMonsterChase(player, rule);
            return;
        }

        currentReturnPointIndex = getNearestReturnPointIndex(player.getX(), player.getZ());
        Point returnPoint = getCurrentReturnPoint();
        double distToCenter = distanceToPoint(player.getX(), player.getZ(), returnPoint);

        huntTargetEntity = null;
        lastHuntTargetEntityId = Integer.MIN_VALUE;
        lastHuntTargetPos = null;
        huntMovementStopped = false;
        escapeDestination = null;
        patrolWaitingAtPoint = false;
        patrolWaitStartMs = 0L;

        if (distToCenter > rule.maxRecoveryDistance) {
            isMovingToPoint = false;
            returningToCenterFromOutOfBounds = false;
            centerReturnCommandIssued = false;
            stopNavigationStatic("超出最大恢复距离，停止自动返回导航");

            boolean shouldRunSequence = rule.runSequenceWhenOutOfRecoveryRange
                    && !isBlank(rule.outOfRangeSequenceName);
            if (shouldRunSequence && !outOfRecoveryRangeSequenceTriggered) {
                String sequenceName = rule.outOfRangeSequenceName.trim();
                if (PathSequenceManager.hasSequence(sequenceName)) {
                    outOfRecoveryRangeSequenceTriggered = true;
                    PathSequenceManager.runPathSequence(sequenceName);
                    notifyOutOfBounds(player, "§b[自动追怪] §e超出巡逻范围且超过最大恢复距离，正在执行序列: §f" + sequenceName);
                } else {
                    notifyOutOfBounds(player, "§b[自动追怪] §c超距序列不存在，无法执行: §f" + sequenceName);
                }
            } else {
                notifyOutOfBounds(player, "§b[自动追怪] §e超出巡逻范围，且距离回归点超过最大恢复距离，已停止自动返回。");
            }
            return;
        }

        outOfRecoveryRangeSequenceTriggered = false;
        returnStuckTicks = 0;
        lastReturnProgressCheckPosition = null;
        returningToCenterFromOutOfBounds = true;
        stopNavigationStatic("超出巡逻范围，切换为返回回归点导航");
        startMoveToCurrentReturnPoint("超出巡逻范围后返回当前回归点");
        notifyOutOfBounds(player, "§b[自动追怪] §e超出巡逻范围，正在返回回归点...");
    }

    private void handleBoundedMonsterChase(LocalPlayer player, AutoFollowRule rule) {
        if (player == null || rule == null || player.level() == null) {
            return;
        }
        if (shouldYieldChaseToKillAura(player)) {
            huntTargetEntity = null;
            lastHuntTargetEntityId = Integer.MIN_VALUE;
            lastHuntTargetPos = null;
            huntMovementStopped = false;
            return;
        }

        if (huntTargetEntity != null) {
            boolean invalid = !huntTargetEntity.isAlive() || !isEntityWithinLockChaseBounds(huntTargetEntity);
            if (invalid) {
                huntTargetEntity = null;
                lastHuntTargetEntityId = Integer.MIN_VALUE;
                lastHuntTargetPos = null;
                huntMovementStopped = false;
            }
        }

        Entity latestBestCandidate = findNearestMonsterInRule(player);
        if (huntTargetEntity == null) {
            huntTargetEntity = latestBestCandidate;
            if (huntTargetEntity == null) {
                handlePatrolWhenNoMonster(player, rule);
                return;
            }
            centerReturnCommandIssued = false;
            patrolWaitingAtPoint = false;
            patrolWaitStartMs = 0L;
            lastHuntTargetEntityId = Integer.MIN_VALUE;
            lastHuntTargetPos = null;
            huntMovementStopped = false;
        }

        boolean fixedDistanceMode = isFixedDistanceHuntMode(rule);
        double keepDistance = getHuntKeepDistance(rule);
        double keepDistSq = keepDistance * keepDistance;
        double distanceSq = player.distanceToSqr(huntTargetEntity);
        double distance = Math.sqrt(distanceSq);
        boolean withinDesiredDistance = fixedDistanceMode
                ? Math.abs(distance - keepDistance) <= HUNT_FIXED_DISTANCE_TOLERANCE
                : distanceSq <= keepDistSq;
        if (withinDesiredDistance) {
            if (!huntMovementStopped) {
                stopNavigationStatic("已进入怪物理想距离，停止追击导航");
                huntMovementStopped = true;
            }
            return;
        }

        huntMovementStopped = false;

        Vec3 targetPos = huntTargetEntity.position();
        int nowTick = player.tickCount;
        int targetId = huntTargetEntity.getId();
        boolean needSendGoto = targetId != lastHuntTargetEntityId
                || lastHuntTargetPos == null
                || targetPos.distanceToSqr(lastHuntTargetPos) >= HUNT_GOTO_MOVE_THRESHOLD_SQ
                || (nowTick - lastHuntGotoTick) >= HUNT_GOTO_INTERVAL_TICKS;

        double minGotoDistance = keepDistance + 0.5D;
        boolean shouldSendGoto = needSendGoto && (fixedDistanceMode
                ? Math.abs(distance - keepDistance) > HUNT_FIXED_DISTANCE_TOLERANCE
                : distanceSq > minGotoDistance * minGotoDistance);
        if (!shouldSendGoto) {
            return;
        }

        if (fixedDistanceMode) {
            Vec3 destination = computeFixedDistanceHuntDestination(player, huntTargetEntity, keepDistance);
            EmbeddedNavigationHandler.INSTANCE.startGotoXZ(
                    EmbeddedNavigationHandler.NavigationOwner.AUTO_FOLLOW, destination.x, destination.z, false,
                    "固定距离追怪：前往计算出的保持距离落点");
            navigationIssuedByAutoFollow = true;
        } else {
            EmbeddedNavigationHandler.INSTANCE.startGotoXZ(
                    EmbeddedNavigationHandler.NavigationOwner.AUTO_FOLLOW, targetPos.x, targetPos.z, false,
                    "普通追怪：直接追击当前目标XZ");
            navigationIssuedByAutoFollow = true;
        }

        lastHuntGotoTick = nowTick;
        lastHuntTargetEntityId = targetId;
        lastHuntTargetPos = targetPos;
    }

    private void handlePatrolWhenNoMonster(LocalPlayer player, AutoFollowRule rule) {
        rule.ensureReturnPoints();
        if (rule.returnPoints == null || rule.returnPoints.isEmpty()) {
            handleRandomPatrolWithoutReturnPoints(player, rule);
            return;
        }

        Point returnPoint = getCurrentReturnPoint();
        if (patrolWaitingAtPoint) {
            stopNavigationStatic("回归点停留阶段保持静止，停止自动追怪导航");
            int stayMillis = Math.max(1, rule.returnStayMillis);
            if (System.currentTimeMillis() - patrolWaitStartMs >= stayMillis) {
                if (rule.returnPoints.size() > 1) {
                    patrolWaitingAtPoint = false;
                    patrolWaitStartMs = 0L;
                    advanceToNextReturnPoint();
                    startMoveToCurrentReturnPoint("回归点停留结束，切换到下一个回归点");
                } else {
                    patrolWaitStartMs = System.currentTimeMillis();
                }
            }
            return;
        }

        boolean atPoint = arriveAt(player, returnPoint.x, Double.NaN, returnPoint.z, getReturnArriveDistance(rule));
        if (!atPoint) {
            if (!isMovingToPoint || !centerReturnCommandIssued) {
                startMoveToCurrentReturnPoint("未在回归点范围内，继续返回当前回归点");
            }
            return;
        }

        centerReturnCommandIssued = false;
        patrolWaitingAtPoint = true;
        patrolWaitStartMs = System.currentTimeMillis();
    }

    private void handleRandomPatrolWithoutReturnPoints(LocalPlayer player, AutoFollowRule rule) {
        if (patrolWaitingAtPoint) {
            stopNavigationStatic("随机巡逻停留阶段保持静止，停止自动追怪导航");
            int stayMillis = Math.max(1, rule.returnStayMillis);
            if (System.currentTimeMillis() - patrolWaitStartMs >= stayMillis) {
                patrolWaitingAtPoint = false;
                patrolWaitStartMs = 0L;
                randomPatrolPoint = getRandomPatrolPointWithinBounds();
                startMoveToPoint(randomPatrolPoint, "随机巡逻停留结束，前往新的随机巡逻点");
            }
            return;
        }

        if (randomPatrolPoint == null) {
            randomPatrolPoint = getRandomPatrolPointWithinBounds();
            startMoveToPoint(randomPatrolPoint, "未配置回归点，初始化随机巡逻目标");
            return;
        }

        boolean atPoint = arriveAt(player, randomPatrolPoint.x, Double.NaN, randomPatrolPoint.z, getReturnArriveDistance(rule));
        if (!atPoint) {
            if (!isMovingToPoint || !centerReturnCommandIssued) {
                startMoveToPoint(randomPatrolPoint, "未到达随机巡逻点，继续导航前往");
            }
            return;
        }

        centerReturnCommandIssued = false;
        patrolWaitingAtPoint = true;
        patrolWaitStartMs = System.currentTimeMillis();
    }

    private static void startMoveToCurrentReturnPoint(String reason) {
        if (activeRule == null) {
            return;
        }
        startMoveToPoint(getCurrentReturnPoint(), reason);
    }

    private static void startMoveToPoint(Point targetPoint, String reason) {
        if (targetPoint == null) {
            return;
        }
        isMovingToPoint = true;
        patrolWaitingAtPoint = false;
        patrolWaitStartMs = 0L;
        centerReturnCommandIssued = true;
        scheduleGotoXZ(targetPoint.x, targetPoint.z, reason);
    }

    private static void scheduleGotoXZ(double x, double z, String reason) {
        ModUtils.DelayScheduler.init();
        if (ModUtils.DelayScheduler.instance != null) {
            ModUtils.DelayScheduler.instance.schedule(() -> {
                EmbeddedNavigationHandler.INSTANCE.startGotoXZ(
                        EmbeddedNavigationHandler.NavigationOwner.AUTO_FOLLOW, x, z, false, reason);
                navigationIssuedByAutoFollow = true;
            },
                    COMMAND_DELAY_TICKS);
        } else {
            EmbeddedNavigationHandler.INSTANCE.startGotoXZ(
                    EmbeddedNavigationHandler.NavigationOwner.AUTO_FOLLOW, x, z, false, reason);
            navigationIssuedByAutoFollow = true;
        }
    }

    private static Point getCurrentReturnPoint() {
        if (activeRule == null) {
            return new Point(0, 0);
        }
        activeRule.ensureReturnPoints();
        List<Point> returnPoints = activeRule.returnPoints;
        if (returnPoints == null || returnPoints.isEmpty()) {
            if (randomPatrolPoint != null) {
                return new Point(randomPatrolPoint.x, randomPatrolPoint.z);
            }
            return new Point((activeRule.minX + activeRule.maxX) * 0.5D,
                    (activeRule.minZ + activeRule.maxZ) * 0.5D);
        }
        if (currentReturnPointIndex < 0 || currentReturnPointIndex >= returnPoints.size()) {
            currentReturnPointIndex = 0;
        }
        Point point = returnPoints.get(currentReturnPointIndex);
        return point == null ? new Point(0, 0) : new Point(point.x, point.z);
    }

    private static Point getRandomPatrolPointWithinBounds() {
        if (activeRule == null) {
            return new Point(0, 0);
        }
        double spanX = Math.max(0.0D, activeRule.maxX - activeRule.minX);
        double spanZ = Math.max(0.0D, activeRule.maxZ - activeRule.minZ);
        double x = activeRule.minX + (spanX <= 0.01D ? 0.0D : RANDOM.nextDouble() * spanX);
        double z = activeRule.minZ + (spanZ <= 0.01D ? 0.0D : RANDOM.nextDouble() * spanZ);
        return new Point(x, z);
    }

    private static int selectInitialReturnPointIndex() {
        if (activeRule == null) {
            return 0;
        }
        activeRule.ensureReturnPoints();
        List<Point> returnPoints = activeRule.returnPoints;
        if (returnPoints == null || returnPoints.isEmpty()) {
            return 0;
        }
        if (AutoFollowRule.PATROL_MODE_RANDOM.equalsIgnoreCase(activeRule.patrolMode)) {
            return RANDOM.nextInt(returnPoints.size());
        }
        return 0;
    }

    private static int getNearestReturnPointIndex(double x, double z) {
        if (activeRule == null) {
            return 0;
        }
        activeRule.ensureReturnPoints();
        List<Point> returnPoints = activeRule.returnPoints;
        if (returnPoints == null || returnPoints.isEmpty()) {
            return 0;
        }

        int bestIndex = 0;
        double bestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < returnPoints.size(); i++) {
            Point point = returnPoints.get(i);
            if (point == null) {
                continue;
            }
            double dx = x - point.x;
            double dz = z - point.z;
            double distSq = dx * dx + dz * dz;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void advanceToNextReturnPoint() {
        if (activeRule == null) {
            currentReturnPointIndex = 0;
            return;
        }
        activeRule.ensureReturnPoints();
        List<Point> returnPoints = activeRule.returnPoints;
        if (returnPoints == null || returnPoints.size() <= 1) {
            currentReturnPointIndex = 0;
            return;
        }

        if (AutoFollowRule.PATROL_MODE_RANDOM.equalsIgnoreCase(activeRule.patrolMode)) {
            int next = currentReturnPointIndex;
            while (next == currentReturnPointIndex && returnPoints.size() > 1) {
                next = RANDOM.nextInt(returnPoints.size());
            }
            currentReturnPointIndex = next;
        } else {
            currentReturnPointIndex = (currentReturnPointIndex + 1) % returnPoints.size();
        }
    }

    private Entity findNearestMonsterInRule(LocalPlayer player) {
        if (player == null || player.level() == null || activeRule == null) {
            replaceLastScoredMonsters(Collections.emptyList());
            return null;
        }

        if (player.tickCount - lastMonsterScoreScanTick < MONSTER_SCORE_SCAN_INTERVAL_TICKS) {
            Entity cached = resolveBestCachedMonster(player);
            if (cached != null) {
                return cached;
            }
        }

        List<ScoredMonsterInfo> snapshots = new ArrayList<>();
        Entity bestEntity = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double playerCenterY = getPlayerCenterY(player);
        Vec3 playerPosition = player.position();

        double minY = player.getY() - Math.max(activeRule.monsterDownwardRange, activeRule.monsterVerticalRange) - 4.0D;
        double maxY = player.getY() + Math.max(activeRule.monsterUpwardRange, activeRule.monsterVerticalRange) + 4.0D;
        AABB searchBox = new AABB(activeRule.minX, minY, activeRule.minZ, activeRule.maxX, maxY, activeRule.maxZ);

        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
            ScoredMonsterInfo info = scoreMonsterCandidate(player, playerPosition, playerCenterY, entity);
            if (info == null) {
                continue;
            }
            snapshots.add(info);
            if (bestEntity == null
                    || info.totalScore > bestScore
                    || (info.totalScore == bestScore && entity.getId() < bestEntity.getId())) {
                bestEntity = entity;
                bestScore = info.totalScore;
            }
        }

        if (snapshots.isEmpty()) {
            lastMonsterScoreScanTick = player.tickCount;
            replaceLastScoredMonsters(Collections.emptyList());
            return null;
        }

        snapshots.sort((left, right) -> Double.compare(right.totalScore, left.totalScore));
        lastMonsterScoreScanTick = player.tickCount;
        replaceLastScoredMonsters(snapshots);
        return bestEntity;
    }

    private Entity resolveBestCachedMonster(LocalPlayer player) {
        if (player == null || player.level() == null || lastScoredMonsters.isEmpty()) {
            return null;
        }
        for (ScoredMonsterInfo info : lastScoredMonsters) {
            Entity entity = player.level().getEntity(info.entityId);
            if (entity != null && entity.isAlive() && isEntityInActiveRuleBounds(entity)) {
                return entity;
            }
        }
        return null;
    }

    private ScoredMonsterInfo scoreMonsterCandidate(LocalPlayer player, Vec3 playerPosition, double playerCenterY,
            LivingEntity entity) {
        if (entity == null || entity == player || !entity.isAlive() || entity.isDeadOrDying() || entity instanceof ArmorStand) {
            return null;
        }
        if (!isEntityInActiveRuleBounds(entity)) {
            return null;
        }

        double entityCenterY = getEntityCenterY(entity);
        double verticalDelta = entityCenterY - playerCenterY;
        double verticalDiff = Math.abs(verticalDelta);
        double distance = player.distanceTo(entity);
        boolean visible = player.hasLineOfSight(entity);
        boolean reachable = isPathClear(player, playerPosition, new Vec3(entity.getX(), entityCenterY, entity.getZ()));
        boolean floating = isEntitySuspended(entity);

        double upwardRange = getAllowedUpwardRange(player, entity, activeRule);
        double downwardRange = getAllowedDownwardRange(player, entity, activeRule);
        double allowedVertical = verticalDelta >= 0.0D ? upwardRange : downwardRange;

        ScoredMonsterInfo info = new ScoredMonsterInfo();
        info.entityId = entity.getId();
        info.name = getFilterableEntityName(entity);
        info.distance = distance;
        info.verticalDiff = verticalDiff;
        info.visible = visible;
        info.reachable = reachable;
        info.distanceScore = Math.max(0.0D, 120.0D - distance * 12.0D);
        info.visibilityScore = visible ? 35.0D : -25.0D;
        info.reachabilityScore = reachable ? 30.0D : -40.0D;
        info.verticalScore = Math.max(-35.0D, 30.0D - (verticalDiff / Math.max(0.1D, allowedVertical)) * 30.0D);
        if (floating) {
            info.verticalScore -= 15.0D;
        }
        info.lockBonusScore = huntTargetEntity != null && huntTargetEntity.getId() == entity.getId() ? 18.0D : 0.0D;
        info.totalScore = info.distanceScore + info.visibilityScore + info.reachabilityScore
                + info.verticalScore + info.lockBonusScore;
        return info;
    }

    private boolean isEntityInActiveRuleBounds(Entity entity) {
        if (activeRule == null || entity == null) {
            return false;
        }
        if (!isEntityWithinRuleBounds(entity, activeRule)) {
            return false;
        }
        if (!passesMonsterTargetFilters(entity, activeRule)) {
            return false;
        }
        return passesMonsterVerticalRange(entity, activeRule);
    }

    private boolean isEntityWithinLockChaseBounds(Entity entity) {
        if (activeRule == null || entity == null) {
            return false;
        }
        if (!passesMonsterTargetFilters(entity, activeRule) || !passesMonsterVerticalRange(entity, activeRule)) {
            return false;
        }
        return isPositionWithinLockChaseBounds(activeRule, entity.getX(), entity.getZ());
    }

    private static boolean isPositionWithinLockChaseBounds(AutoFollowRule rule, double x, double z) {
        if (rule == null) {
            return false;
        }

        double dx = 0.0D;
        if (x < rule.minX) {
            dx = rule.minX - x;
        } else if (x > rule.maxX) {
            dx = x - rule.maxX;
        }

        double dz = 0.0D;
        if (z < rule.minZ) {
            dz = rule.minZ - z;
        } else if (z > rule.maxZ) {
            dz = z - rule.maxZ;
        }

        double outDistance = Math.sqrt(dx * dx + dz * dz);
        double allowed = rule.lockChaseOutOfBoundsDistance > 0
                ? rule.lockChaseOutOfBoundsDistance
                : AutoFollowRule.DEFAULT_LOCK_CHASE_OUT_OF_BOUNDS_DISTANCE;
        return outDistance <= allowed;
    }

    private boolean passesMonsterTargetFilters(Entity entity, AutoFollowRule rule) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isDeadOrDying()) {
            return false;
        }
        if (!matchesConfiguredEntityType(entity, rule)) {
            return false;
        }
        if (!rule.targetInvisibleMonsters && entity.isInvisible()) {
            return false;
        }
        if (!rule.enableMonsterNameList) {
            return true;
        }

        String name = getFilterableEntityName(entity);
        boolean hasWhitelist = rule.monsterWhitelistNames != null && !rule.monsterWhitelistNames.isEmpty();
        boolean matchedWhitelist = !hasWhitelist
                || KillAuraHandler.getNameListMatchIndex(name, rule.monsterWhitelistNames) != Integer.MAX_VALUE;
        boolean matchedBlacklist = KillAuraHandler.getNameListMatchIndex(name, rule.monsterBlacklistNames) != Integer.MAX_VALUE;
        return matchedWhitelist && !matchedBlacklist;
    }

    private boolean matchesConfiguredEntityType(Entity entity, AutoFollowRule rule) {
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand) {
            return false;
        }
        List<String> types = rule == null ? null : rule.entityTypes;
        if (types == null || types.isEmpty()) {
            return entity instanceof Enemy;
        }
        for (String rawType : types) {
            if (matchesEntityTypeToken(entity, rawType)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesEntityTypeToken(Entity entity, String rawType) {
        String token = normalizeEntityTypeAlias(rawType);
        if (token.isEmpty()) {
            return false;
        }

        switch (token) {
            case AutoFollowRule.ENTITY_TYPE_ANY:
            case AutoFollowRule.ENTITY_TYPE_LIVING:
                return entity instanceof LivingEntity && entity != MC.player;
            case AutoFollowRule.ENTITY_TYPE_PLAYER:
                return entity instanceof Player && entity != MC.player;
            case AutoFollowRule.ENTITY_TYPE_MONSTER:
                return entity instanceof Enemy;
            case AutoFollowRule.ENTITY_TYPE_NEUTRAL:
                return entity instanceof LivingEntity
                        && !(entity instanceof Player)
                        && !(entity instanceof Enemy)
                        && !(entity instanceof Animal)
                        && !(entity instanceof AbstractHorse)
                        && !(entity instanceof WaterAnimal)
                        && !(entity instanceof AmbientCreature)
                        && !(entity instanceof AbstractVillager)
                        && !(entity instanceof AbstractGolem)
                        && !(entity instanceof TamableAnimal);
            case AutoFollowRule.ENTITY_TYPE_ANIMAL:
                return entity instanceof Animal || entity instanceof AbstractHorse;
            case AutoFollowRule.ENTITY_TYPE_WATER:
                return entity instanceof WaterAnimal;
            case AutoFollowRule.ENTITY_TYPE_AMBIENT:
                return entity instanceof AmbientCreature;
            case AutoFollowRule.ENTITY_TYPE_VILLAGER:
                return entity instanceof AbstractVillager;
            case AutoFollowRule.ENTITY_TYPE_GOLEM:
                return entity instanceof AbstractGolem
                        || entity.getType().toString().toLowerCase(Locale.ROOT).contains("golem");
            case AutoFollowRule.ENTITY_TYPE_TAMEABLE:
                return entity instanceof TamableAnimal;
            case AutoFollowRule.ENTITY_TYPE_BOSS:
                return entity instanceof EnderDragon
                        || entity instanceof WitherBoss
                        || (entity instanceof LivingEntity living && living.getMaxHealth() >= 80.0F);
            default:
                return false;
        }
    }

    private String normalizeEntityTypeAlias(String rawType) {
        String token = safe(rawType).trim().toLowerCase(Locale.ROOT);
        switch (token) {
            case "任意":
                return AutoFollowRule.ENTITY_TYPE_ANY;
            case "生物":
                return AutoFollowRule.ENTITY_TYPE_LIVING;
            case "玩家":
                return AutoFollowRule.ENTITY_TYPE_PLAYER;
            case "怪物":
            case "mob":
            case "hostile":
                return AutoFollowRule.ENTITY_TYPE_MONSTER;
            case "中立":
            case "中立生物":
                return AutoFollowRule.ENTITY_TYPE_NEUTRAL;
            case "动物":
            case "passive":
                return AutoFollowRule.ENTITY_TYPE_ANIMAL;
            case "水生":
                return AutoFollowRule.ENTITY_TYPE_WATER;
            case "环境":
                return AutoFollowRule.ENTITY_TYPE_AMBIENT;
            case "村民":
            case "npc":
                return AutoFollowRule.ENTITY_TYPE_VILLAGER;
            case "傀儡":
                return AutoFollowRule.ENTITY_TYPE_GOLEM;
            case "驯服":
            case "宠物":
                return AutoFollowRule.ENTITY_TYPE_TAMEABLE;
            case "首领":
                return AutoFollowRule.ENTITY_TYPE_BOSS;
            default:
                return token;
        }
    }

    private String getFilterableEntityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        String displayName = entity.getDisplayName() == null ? "" : entity.getDisplayName().getString();
        String normalized = KillAuraHandler.normalizeFilterName(displayName);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return KillAuraHandler.normalizeFilterName(entity.getName().getString());
    }

    private boolean passesMonsterVerticalRange(Entity entity, AutoFollowRule rule) {
        if (entity == null || MC.player == null || rule == null) {
            return false;
        }
        double delta = getEntityCenterY(entity) - getPlayerCenterY(MC.player);
        return delta <= getAllowedUpwardRange(MC.player, entity, rule)
                && delta >= -getAllowedDownwardRange(MC.player, entity, rule);
    }

    private double getEntityCenterY(Entity entity) {
        AABB box = entity == null ? null : entity.getBoundingBox();
        return box == null ? 0.0D : (box.minY + box.maxY) * 0.5D;
    }

    private double getPlayerCenterY(LocalPlayer player) {
        AABB box = player == null ? null : player.getBoundingBox();
        return box == null ? 0.0D : (box.minY + box.maxY) * 0.5D;
    }

    private double getAllowedUpwardRange(LocalPlayer player, Entity entity, AutoFollowRule rule) {
        double range = rule != null && rule.monsterUpwardRange > 0
                ? rule.monsterUpwardRange
                : AutoFollowRule.DEFAULT_MONSTER_UPWARD_RANGE;
        if (isStairOrSlopeContext(player, entity)) {
            range += 1.0D;
        }
        return range;
    }

    private double getAllowedDownwardRange(LocalPlayer player, Entity entity, AutoFollowRule rule) {
        double range = rule != null && rule.monsterDownwardRange > 0
                ? rule.monsterDownwardRange
                : AutoFollowRule.DEFAULT_MONSTER_DOWNWARD_RANGE;
        if (isStairOrSlopeContext(player, entity)) {
            range += 1.0D;
        }
        return range;
    }

    private boolean isStairOrSlopeContext(LocalPlayer player, Entity entity) {
        BlockPos playerPos = player == null ? null : player.blockPosition();
        BlockPos entityPos = entity == null ? null : BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        return (playerPos != null && isOnSlabOrStair(playerPos))
                || (entityPos != null && isOnSlabOrStair(entityPos));
    }

    private boolean isOnSlabOrStair(BlockPos pos) {
        if (MC.level == null || pos == null) {
            return false;
        }
        BlockState below = MC.level.getBlockState(pos.below());
        BlockState feet = MC.level.getBlockState(pos);
        Block belowBlock = below.getBlock();
        Block feetBlock = feet.getBlock();
        return belowBlock instanceof SlabBlock || belowBlock instanceof StairBlock
                || feetBlock instanceof SlabBlock || feetBlock instanceof StairBlock;
    }

    private boolean isEntitySuspended(Entity entity) {
        if (entity == null || MC.level == null) {
            return false;
        }
        BlockPos under = BlockPos.containing(entity.getX(), entity.getY() - 0.1D, entity.getZ()).below();
        return MC.level.getBlockState(under).canBeReplaced();
    }

    private boolean handleTimeoutReload(LocalPlayer player) {
        if (!timeoutReloadEnabled) {
            timeoutTicksCounter = 0;
            lastTimeoutCheckPosition = null;
            return false;
        }

        timeoutTicksCounter++;
        if (timeoutTicksCounter >= 20) {
            if (lastTimeoutCheckPosition != null
                    && player.position().distanceToSqr(lastTimeoutCheckPosition) >= STUCK_DISTANCE_THRESHOLD_SQ) {
                timeoutTicksCounter = 0;
            }
            lastTimeoutCheckPosition = player.position();
        }

        if (timeoutTicksCounter >= Math.max(20, timeoutReloadSeconds * 20)) {
            player.displayClientMessage(new TextComponentString("§b[自动追怪] §c检测到长时间未移动，触发超时重载。"), false);
            reloadState(true);
            return true;
        }
        return false;
    }

    private boolean handleGeneralStuckCondition(LocalPlayer player, AutoFollowRule rule) {
        stuckCheckCounter++;
        if (stuckCheckCounter < STUCK_CHECK_INTERVAL) {
            return false;
        }

        List<Entity> nearbyEnemies = player.level().getEntities(player,
                player.getBoundingBox().inflate(3.5D),
                entity -> entity != null
                        && entity != player
                        && entity.isAlive()
                        && passesMonsterTargetFilters(entity, rule));

        if (!nearbyEnemies.isEmpty()) {
            stuckCheckCounter = 0;
            lastStuckCheckPosition = player.position();
            return false;
        }

        if (lastStuckCheckPosition != null
                && player.position().distanceToSqr(lastStuckCheckPosition) < STUCK_DISTANCE_THRESHOLD_SQ) {
            player.displayClientMessage(new TextComponentString("§b[自动追怪] §c检测到卡住，执行智能逃逸。"), false);
            if (findAndExecuteEscape(player)) {
                stuckCheckCounter = 0;
                lastStuckCheckPosition = null;
                return true;
            }
        }

        lastStuckCheckPosition = player.position();
        stuckCheckCounter = 0;
        return false;
    }

    private boolean findAndExecuteEscape(LocalPlayer player) {
        Vec3 escapeVec = findEscapeVector(player);
        if (escapeVec == null) {
            player.displayClientMessage(new TextComponentString("§b[自动追怪] §4无法找到安全的逃逸路径。"), false);
            reloadState(true);
            return false;
        }

        escapeDestination = player.position().add(escapeVec.scale(ESCAPE_DISTANCE));
        isMovingToPoint = true;
        returningToCenterFromOutOfBounds = false;
        stopNavigationStatic("开始执行逃逸路径前停止当前追怪导航");
        scheduleGotoXZ(escapeDestination.x, escapeDestination.z, "危险规避：执行逃逸路径导航");
        lastAvoidanceTime = System.currentTimeMillis();
        return true;
    }

    private Vec3 findEscapeVector(LocalPlayer player) {
        Vec3 bestVector = null;
        double bestScore = -1.0D;

        Vec3 look = player.getViewVector(1.0F);
        Vec3 forwardVec = new Vec3(look.x, 0.0D, look.z);
        if (forwardVec.lengthSqr() < 1.0E-4D) {
            forwardVec = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            forwardVec = forwardVec.normalize();
        }

        Vec3[] directions = {
                new Vec3(0, 0, -1),
                new Vec3(1, 0, -1).normalize(),
                new Vec3(1, 0, 0),
                new Vec3(1, 0, 1).normalize(),
                new Vec3(0, 0, 1),
                new Vec3(-1, 0, 1).normalize(),
                new Vec3(-1, 0, 0),
                new Vec3(-1, 0, -1).normalize()
        };

        for (Vec3 dir : directions) {
            Vec3 escapePointVec = player.position().add(dir.scale(ESCAPE_DISTANCE));
            BlockPos escapePointPos = BlockPos.containing(escapePointVec);
            if (!isWalkable(player, escapePointPos) || !isPathClear(player, player.position(), escapePointVec)) {
                continue;
            }

            BlockPos nearestHazard = isNearHazard(player, escapePointPos, 10.0D);
            double hazardScore = nearestHazard != null
                    ? Math.sqrt(escapePointPos.distSqr(nearestHazard))
                    : 100.0D;

            double directionScore = forwardVec.dot(dir);
            double intentBonus = (directionScore + 1.0D) * 10.0D;
            double finalScore = hazardScore + intentBonus;
            if (finalScore > bestScore) {
                bestScore = finalScore;
                bestVector = dir;
            }
        }
        return bestVector;
    }

    private boolean isWalkable(LocalPlayer player, BlockPos pos) {
        if (MC.level == null || player == null || pos == null) {
            return false;
        }

        BlockState belowState = MC.level.getBlockState(pos.below());
        BlockState feetState = MC.level.getBlockState(pos);
        BlockState headState = MC.level.getBlockState(pos.above());
        if (belowState.getCollisionShape(MC.level, pos.below()).isEmpty()) {
            return false;
        }
        if (!feetState.canBeReplaced() || !headState.canBeReplaced()) {
            return false;
        }
        if (Math.abs(pos.getY() - player.getY()) > 2.0D) {
            return false;
        }

        AABB box = player.getDimensions(player.getPose()).makeBoundingBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return MC.level.noCollision(player, box);
    }

    private boolean isPathClear(LocalPlayer player, Vec3 start, Vec3 end) {
        if (player == null || player.level() == null) {
            return false;
        }

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        if (distance <= 1.0E-4D) {
            return true;
        }
        direction = direction.normalize();

        for (double step = 0.5D; step < distance; step += 0.5D) {
            Vec3 intermediatePoint = start.add(direction.scale(step));
            AABB box = player.getDimensions(player.getPose()).makeBoundingBox(intermediatePoint.x, intermediatePoint.y,
                    intermediatePoint.z);
            if (!player.level().noCollision(player, box)) {
                return false;
            }
        }
        return true;
    }

    private void resolveHazardZoneAvoidance(LocalPlayer player, Vec3 travelVec) {
        player.displayClientMessage(new TextComponentString("§b[自动追怪] §6感知到前方危险区域，正在计算规避路径。"), false);

        List<BlockPos> obstacles = scanForwardArea(player, travelVec);
        if (obstacles.isEmpty()) {
            forcefulSidestep(player, travelVec);
            return;
        }

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        double maxDepth = 0.0D;

        for (BlockPos pos : obstacles) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());

            Vec3 obstacleVec = new Vec3(pos.getX() - player.getX(), 0.0D, pos.getZ() - player.getZ());
            double depth = obstacleVec.dot(new Vec3(travelVec.x, 0.0D, travelVec.z));
            maxDepth = Math.max(maxDepth, depth);
        }

        Vec3 hazardCenter = new Vec3((minX + maxX) * 0.5D, player.getY(), (minZ + maxZ) * 0.5D);
        Vec3 playerToHazardVec = hazardCenter.subtract(player.position());
        double crossProduct = travelVec.x * playerToHazardVec.z - travelVec.z * playerToHazardVec.x;

        Vec3 sidestepVec = crossProduct > 0.0D
                ? new Vec3(travelVec.z, 0.0D, -travelVec.x).normalize()
                : new Vec3(-travelVec.z, 0.0D, travelVec.x).normalize();
        double hazardWidth = Math.sqrt(square(maxX - minX) + square(maxZ - minZ));
        double sidestepDistance = (hazardWidth * 0.5D) + AVOIDANCE_SIDESTEP_SAFETY_MARGIN;
        double forwardDistance = maxDepth + AVOIDANCE_FORWARD_SAFETY_MARGIN;

        Vec3 avoidancePoint = player.position()
                .add(sidestepVec.scale(sidestepDistance))
                .add(travelVec.scale(forwardDistance));
        if (!isWalkable(player, BlockPos.containing(avoidancePoint))
                || !isPathClear(player, player.position(), avoidancePoint)) {
            forcefulSidestep(player, travelVec);
            return;
        }

        escapeDestination = avoidancePoint;
        isMovingToPoint = true;
        returningToCenterFromOutOfBounds = false;
        stopNavigationStatic("开始执行危险规避前停止当前追怪导航");
        scheduleGotoXZ(avoidancePoint.x, avoidancePoint.z, "危险规避：导航到侧移绕行点");
        lastAvoidanceTime = System.currentTimeMillis();
    }

    private List<BlockPos> scanForwardArea(LocalPlayer player, Vec3 travelVec) {
        List<BlockPos> obstacles = new ArrayList<>();
        BlockPos center = player.blockPosition();
        int scanRadiusXZ = 2;
        int scanRadiusY = 1;

        for (int dx = -scanRadiusXZ; dx <= scanRadiusXZ; dx++) {
            for (int dz = -scanRadiusXZ; dz <= scanRadiusXZ; dz++) {
                for (int dy = -scanRadiusY; dy <= scanRadiusY; dy++) {
                    BlockPos currentPos = center.offset(dx, dy, dz);
                    Vec3 blockVec = new Vec3(currentPos.getX() - player.getX(), 0.0D, currentPos.getZ() - player.getZ());
                    if (blockVec.dot(new Vec3(travelVec.x, 0.0D, travelVec.z)) < 0.0D) {
                        continue;
                    }

                    if (!MC.level.getBlockState(currentPos).isAir()) {
                        obstacles.add(currentPos);
                    }
                }
            }
        }
        return obstacles;
    }

    private void forcefulSidestep(LocalPlayer player, Vec3 travelVec) {
        player.displayClientMessage(new TextComponentString("§b[自动追怪] §e未扫描到明确障碍，执行预防性侧移。"), false);

        Vec3 sidestepVecRight = new Vec3(-travelVec.z, 0.0D, travelVec.x).normalize();
        Vec3 avoidancePointRight = player.position().add(sidestepVecRight.scale(AVOIDANCE_SIDESTEP_SAFETY_MARGIN + 1.0D));
        if (isWalkable(player, BlockPos.containing(avoidancePointRight))
                && isPathClear(player, player.position(), avoidancePointRight)) {
            escapeDestination = avoidancePointRight;
        } else {
            Vec3 sidestepVecLeft = new Vec3(travelVec.z, 0.0D, -travelVec.x).normalize();
            Vec3 avoidancePointLeft = player.position().add(sidestepVecLeft.scale(AVOIDANCE_SIDESTEP_SAFETY_MARGIN + 1.0D));
            if (isWalkable(player, BlockPos.containing(avoidancePointLeft))
                    && isPathClear(player, player.position(), avoidancePointLeft)) {
                escapeDestination = avoidancePointLeft;
            } else {
                player.displayClientMessage(new TextComponentString("§b[自动追怪] §c两侧均无安全侧移路径，取消规避。"), false);
                return;
            }
        }

        isMovingToPoint = true;
        returningToCenterFromOutOfBounds = false;
        stopNavigationStatic("开始执行强制侧移前停止当前追怪导航");
        scheduleGotoXZ(escapeDestination.x, escapeDestination.z, "危险规避：执行预防性强制侧移");
        lastAvoidanceTime = System.currentTimeMillis();
    }

    private BlockPos isNearHazard(LocalPlayer player, BlockPos origin, double distance) {
        if (player == null || player.level() == null || origin == null) {
            return null;
        }

        int checkRadius = Mth.ceil(distance);
        double distanceSq = distance * distance;
        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int z = -checkRadius; z <= checkRadius; z++) {
                for (int y = -1; y <= 2; y++) {
                    BlockPos checkPos = origin.offset(x, y, z);
                    if (player.distanceToSqr(checkPos.getX() + 0.5D, checkPos.getY() + 0.5D, checkPos.getZ() + 0.5D) > distanceSq) {
                        continue;
                    }
                    Block block = player.level().getBlockState(checkPos).getBlock();
                    if (block instanceof VineBlock || block == Blocks.COBWEB) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    private boolean shouldYieldChaseToKillAura(LocalPlayer player) {
        return player != null
                && KillAuraHandler.enabled
                && KillAuraHandler.isHuntEnabled()
                && KillAuraHandler.INSTANCE.hasActiveTarget(player);
    }

    private static boolean arriveAt(LocalPlayer player, double x, double y, double z, double tolerance) {
        if (player == null) {
            return false;
        }
        double dx = player.getX() - x;
        double dz = player.getZ() - z;
        if (Double.isNaN(y)) {
            return dx * dx + dz * dz <= tolerance * tolerance;
        }
        double dy = player.getY() - y;
        return dx * dx + dy * dy + dz * dz <= tolerance * tolerance;
    }

    private Vec3 getHorizontalTravelVector(Vec3 currentPos) {
        if (lastTickPlayerPos == null || currentPos.distanceToSqr(lastTickPlayerPos) <= 0.001D) {
            return null;
        }
        Vec3 delta = currentPos.subtract(lastTickPlayerPos);
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        return horizontal.lengthSqr() < 1.0E-4D ? null : horizontal.normalize();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        LocalPlayer player = MC.player;
        AutoFollowRule rule = activeRule;
        if (player == null || MC.level == null || rule == null || !rule.enabled) {
            return;
        }
        rule.updateBounds();
        double y = player.getY() + 0.05D;
        if (rule.visualizeRange) {
            AABB box = new AABB(rule.minX, y, rule.minZ, rule.maxX, y + 1.6D, rule.maxZ).inflate(0.01D);
            WorldGizmoRenderer.boxWorld(box, new Color(0x4AA3FF), 0.78F, 2.0F, true);
        }
        if (rule.visualizeLockChaseRadius) {
            Vec3 center = huntTargetEntity != null && huntTargetEntity.isAlive()
                    ? huntTargetEntity.position()
                    : (lastHuntTargetPos != null ? lastHuntTargetPos : player.position());
            WorldGizmoRenderer.verticalRingWall(center.x, y, center.z,
                    Math.max(0.5D, rule.lockChaseOutOfBoundsDistance),
                    0.0D, 1.4D, new Color(0xFFAA44), 0.72F, 2.0F, true);
        }
    }

    private void drawLockChaseBoundary(BufferBuilder buffer, PoseStack poseStack, AutoFollowRule rule,
            RenderWorldLastEvent.WorldRenderContext renderContext, double y) {
    }

    private void drawLockChaseBoundaryStrip(BufferBuilder buffer, PoseStack poseStack, AutoFollowRule rule,
            RenderWorldLastEvent.WorldRenderContext renderContext, double y, double radius,
            float red, float green, float blue, float alpha) {
    }

    private void drawLockChaseBoundaryMarkers(BufferBuilder buffer, PoseStack poseStack, AutoFollowRule rule,
            RenderWorldLastEvent.WorldRenderContext renderContext, double y, double radius,
            float red, float green, float blue, float alpha) {
    }

    private void appendArc(BufferBuilder buffer, PoseStack poseStack, double centerX, double y, double centerZ, double radius,
            double startDeg, double endDeg, int segments, RenderWorldLastEvent.WorldRenderContext renderContext,
            float red, float green, float blue, float alpha) {
    }

    private void renderWallShell(PoseStack poseStack, BufferBuilder buffer, AABB outer,
            RenderWorldLastEvent.WorldRenderContext renderContext,
            float r, float g, float b, float alpha) {
    }

    private void drawFilledAndOutline(PoseStack poseStack, BufferBuilder buffer, AABB box,
            RenderWorldLastEvent.WorldRenderContext renderContext,
            float r, float g, float b, float alpha) {
    }

    private double getHuntKeepDistance(AutoFollowRule rule) {
        if (rule == null) {
            return AutoFollowRule.DEFAULT_MONSTER_STOP_DISTANCE;
        }
        if (isFixedDistanceHuntMode(rule) && rule.monsterFixedDistance > 0) {
            return rule.monsterFixedDistance;
        }
        if (rule.monsterStopDistance > 0) {
            return rule.monsterStopDistance;
        }
        return AutoFollowRule.DEFAULT_MONSTER_STOP_DISTANCE;
    }

    private boolean isFixedDistanceHuntMode(AutoFollowRule rule) {
        return rule != null
                && AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE.equalsIgnoreCase(rule.monsterChaseMode);
    }

    private Vec3 computeFixedDistanceHuntDestination(LocalPlayer player, Entity target, double desiredDistance) {
        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 1.0E-4D) {
            dx = 1.0D;
            dz = 0.0D;
            distance = 1.0D;
        }

        double scale = desiredDistance / distance;
        double destinationX = target.getX() + dx * scale;
        double destinationZ = target.getZ() + dz * scale;
        return clipFixedDistanceDestination(activeRule, target.getX(), target.getZ(), destinationX, destinationZ);
    }

    private Vec3 clipFixedDistanceDestination(AutoFollowRule rule, double centerX, double centerZ,
            double destinationX, double destinationZ) {
        if (rule == null || isPositionWithinLockChaseBounds(rule, destinationX, destinationZ)) {
            return new Vec3(destinationX, Double.NaN, destinationZ);
        }

        double dx = destinationX - centerX;
        double dz = destinationZ - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 1.0E-4D) {
            return new Vec3(centerX, Double.NaN, centerZ);
        }

        double dirX = dx / distance;
        double dirZ = dz / distance;
        double low = 0.0D;
        double high = distance;
        for (int i = 0; i < 14; i++) {
            double mid = (low + high) * 0.5D;
            double testX = centerX + dirX * mid;
            double testZ = centerZ + dirZ * mid;
            if (isPositionWithinLockChaseBounds(rule, testX, testZ)) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return new Vec3(centerX + dirX * low, Double.NaN, centerZ + dirZ * low);
    }

    private static synchronized void replaceLastScoredMonsters(List<ScoredMonsterInfo> scoredMonsters) {
        lastScoredMonsters.clear();
        if (scoredMonsters != null) {
            lastScoredMonsters.addAll(scoredMonsters);
        }
    }

    private static void clearRuntimeStateIfNeeded(boolean stopNavigation, boolean save) {
        if (!hasRuntimeState()) {
            return;
        }
        clearRuntimeState(stopNavigation, save);
    }

    private static void clearRuntimeState(boolean stopNavigation, boolean save) {
        if (stopNavigation) {
            stopNavigationStatic("清理自动追怪运行状态");
        }
        isMovingToPoint = false;
        escapeDestination = null;
        returningToCenterFromOutOfBounds = false;
        isSuspendedDueToDistance = false;
        huntTargetEntity = null;
        lastHuntGotoTick = -99999;
        lastHuntTargetEntityId = Integer.MIN_VALUE;
        lastHuntTargetPos = null;
        huntMovementStopped = false;
        centerReturnCommandIssued = false;
        navigationIssuedByAutoFollow = false;
        patrolWaitingAtPoint = false;
        patrolWaitStartMs = 0L;
        randomPatrolPoint = null;
        lastMonsterScoreScanTick = -99999;
        timeoutTicksCounter = 0;
        lastTimeoutCheckPosition = null;
        stuckCheckCounter = 0;
        lastStuckCheckPosition = null;
        lastTickPlayerPos = null;
        lastAvoidanceTime = 0L;
        outOfRecoveryRangeSequenceTriggered = false;
        returnStuckTicks = 0;
        lastReturnProgressCheckPosition = null;
        lastOutOfBoundsNotifyMs = 0L;
        replaceLastScoredMonsters(Collections.emptyList());
        if (save) {
            saveFollowConfig();
        }
    }

    private static boolean hasRuntimeState() {
        return activeRule != null
                || isMovingToPoint
                || huntTargetEntity != null
                || lastHuntTargetPos != null
                || escapeDestination != null
                || centerReturnCommandIssued
                || navigationIssuedByAutoFollow
                || returningToCenterFromOutOfBounds
                || isSuspendedDueToDistance
                || outOfRecoveryRangeSequenceTriggered
                || returnStuckTicks > 0
                || patrolWaitingAtPoint
                || patrolWaitStartMs != 0L
                || randomPatrolPoint != null
                || lastMonsterScoreScanTick != -99999
                || timeoutTicksCounter > 0
                || lastTimeoutCheckPosition != null
                || stuckCheckCounter > 0
                || lastStuckCheckPosition != null
                || lastTickPlayerPos != null
                || lastReturnProgressCheckPosition != null;
    }

    private static void resetPatrolState() {
        clearRuntimeState(true, false);
        currentReturnPointIndex = activeRule == null ? 0 : selectInitialReturnPointIndex();
    }

    private static void stopNavigationStatic(String reason) {
        if (!navigationIssuedByAutoFollow) {
            return;
        }
        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.AUTO_FOLLOW, reason);
        navigationIssuedByAutoFollow = false;
    }

    private static List<AutoFollowRule> snapshotRules() {
        List<AutoFollowRule> snapshot = new ArrayList<>();
        for (AutoFollowRule rule : rules) {
            if (rule == null) {
                continue;
            }
            sanitizeRule(rule);
            snapshot.add(rule);
        }
        return snapshot;
    }

    private static AutoFollowRule findRuleByName(String name) {
        String normalized = safe(name).trim();
        if (normalized.isEmpty()) {
            return null;
        }
        for (AutoFollowRule rule : rules) {
            if (rule != null && normalized.equalsIgnoreCase(safe(rule.name).trim())) {
                return rule;
            }
        }
        return null;
    }

    private static void sanitizeRule(AutoFollowRule rule) {
        if (rule == null) {
            return;
        }
        if (isBlank(rule.name)) {
            rule.name = "规则";
        } else {
            rule.name = rule.name.trim();
        }
        rule.category = normalizeCategory(rule.category);
        if (rule.maxRecoveryDistance <= 0.0D) {
            rule.maxRecoveryDistance = 500.0D;
        }
        if (rule.outOfRangeSequenceName == null) {
            rule.outOfRangeSequenceName = "";
        }
        rule.updateBounds();
        rule.ensureReturnPoints();
    }

    private static void ensureCategoriesSynced() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        for (AutoFollowRule rule : rules) {
            if (rule == null) {
                continue;
            }
            sanitizeRule(rule);
            normalized.add(rule.category);
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        categories.clear();
        categories.addAll(normalized);
    }

    private static boolean containsCategoryIgnoreCase(String category) {
        for (String existing : categories) {
            if (normalizeCategory(existing).equalsIgnoreCase(normalizeCategory(category))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private void notifyOutOfBounds(LocalPlayer player, String message) {
        long now = System.currentTimeMillis();
        if (now - lastOutOfBoundsNotifyMs < OUT_OF_BOUNDS_NOTIFY_COOLDOWN_MS) {
            return;
        }
        lastOutOfBoundsNotifyMs = now;
        if (player != null) {
            player.displayClientMessage(new TextComponentString(message), false);
        }
    }

    private boolean isPlayerWithinRuleBounds(LocalPlayer player, AutoFollowRule rule) {
        return player.getX() >= rule.minX && player.getX() <= rule.maxX
                && player.getZ() >= rule.minZ && player.getZ() <= rule.maxZ;
    }

    private boolean isEntityWithinRuleBounds(Entity entity, AutoFollowRule rule) {
        return entity.getX() >= rule.minX && entity.getX() <= rule.maxX
                && entity.getZ() >= rule.minZ && entity.getZ() <= rule.maxZ;
    }

    private double getReturnArriveDistance(AutoFollowRule rule) {
        return rule == null ? AutoFollowRule.DEFAULT_RETURN_ARRIVE_DISTANCE
                : Math.max(0.25D, rule.returnArriveDistance);
    }

    private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }

    private static double readDouble(JsonObject root, String key, double fallback) {
        return root.has(key) ? root.get(key).getAsDouble() : fallback;
    }

    private static int readInt(JsonObject root, String key, int fallback) {
        return root.has(key) ? root.get(key).getAsInt() : fallback;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static double square(double value) {
        return value * value;
    }

    private static double distanceToPoint(double x, double z, Point point) {
        if (point == null) {
            return Double.MAX_VALUE;
        }
        double dx = x - point.x;
        double dz = z - point.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
