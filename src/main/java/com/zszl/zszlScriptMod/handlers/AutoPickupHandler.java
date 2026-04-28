package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.zszl.zszlScriptMod.compat.ItemComponentCompat;
import com.zszl.zszlScriptMod.compat.render.WorldGizmoRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.system.AutoPickupRule;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoPickupHandler {

    public static final AutoPickupHandler INSTANCE = new AutoPickupHandler();

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CATEGORY_DEFAULT = "默认";

    private static final int SEARCH_INTERVAL_TICKS = 3;
    private static final int GOTO_INTERVAL_TICKS = 5;
    private static final int TARGET_LOST_GRACE_TICKS = 8;
    private static final double NAVIGATION_MOVEMENT_THRESHOLD_SQ = 0.16D;

    public static boolean globalEnabled = false;
    public static final List<AutoPickupRule> rules = new CopyOnWriteArrayList<>();
    private static final List<String> categories = new CopyOnWriteArrayList<>();

    private enum State {
        IDLE,
        SEARCHING,
        MOVING_TO_ITEM,
        WAITING_POST_PICKUP
    }

    private static final class PendingPickupSequenceAction {
        private final String sequenceName;
        private int remainingTicks;

        private PendingPickupSequenceAction(String sequenceName, int remainingTicks) {
            this.sequenceName = sequenceName == null ? "" : sequenceName.trim();
            this.remainingTicks = Math.max(0, remainingTicks);
        }
    }

    private static final class ItemLocationKey {
        private final int x;
        private final int y;
        private final int z;

        private ItemLocationKey(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ItemLocationKey other)) {
                return false;
            }
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    private State currentState = State.IDLE;
    private AutoPickupRule activeRule;
    private ItemEntity currentTargetItem;
    private ItemStack currentTargetSnapshot = ItemStack.EMPTY;
    private int postPickupDelayTicks = 0;
    private int lastGotoTick = -99999;
    private int lastTargetSearchTick = -99999;
    private int lastTargetSeenTick = -99999;
    private boolean hasPickedUpAtLeastOneItem = false;
    private double lastIssuedTargetX = Double.NaN;
    private double lastIssuedTargetY = Double.NaN;
    private double lastIssuedTargetZ = Double.NaN;
    private boolean navigationIssuedByAutoPickup = false;
    private int antiStuckStationaryTicks = 0;
    private double antiStuckLastPosX = Double.NaN;
    private double antiStuckLastPosY = Double.NaN;
    private double antiStuckLastPosZ = Double.NaN;

    private final List<PendingPickupSequenceAction> pendingPickupActions = new ArrayList<>();
    private final Map<String, Map<ItemLocationKey, Integer>> failedPickupAttemptsByRule = new HashMap<>();
    private final Map<String, Integer> lastInventoryCounts = new HashMap<>();
    private ItemStack pendingInventoryValidationStack = ItemStack.EMPTY;
    private int pendingInventoryValidationTicks = 0;

    private AutoPickupHandler() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_pickup_rules.json");
    }

    public static synchronized void saveConfig() {
        ensureCategoriesSynced();
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.addProperty("globalEnabled", globalEnabled);
                root.add("categories", GSON.toJsonTree(new ArrayList<>(categories)));
                root.add("rules", GSON.toJsonTree(new ArrayList<>(rules)));
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法保存自动拾取规则", e);
        }
    }

    public static synchronized void loadConfig() {
        globalEnabled = false;
        rules.clear();
        categories.clear();

        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            ensureCategoriesSynced();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("globalEnabled")) {
                globalEnabled = root.get("globalEnabled").getAsBoolean();
            }
            if (root.has("categories") && root.get("categories").isJsonArray()) {
                root.getAsJsonArray("categories")
                        .forEach(element -> categories.add(normalizeCategory(element.getAsString())));
            }
            if (root.has("rules")) {
                Type listType = new TypeToken<ArrayList<AutoPickupRule>>() {
                }.getType();
                List<AutoPickupRule> loaded = GSON.fromJson(root.get("rules"), listType);
                if (loaded != null) {
                    for (AutoPickupRule rule : loaded) {
                        if (rule == null) {
                            continue;
                        }
                        normalizeRule(rule);
                        rules.add(rule);
                    }
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法加载自动拾取规则", e);
            rules.clear();
        }

        ensureCategoriesSynced();
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        ensureCategoriesSynced();
        return new ArrayList<>(categories);
    }

    public static synchronized void replaceCategoryOrder(List<String> orderedCategories) {
        ensureCategoriesSynced();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (orderedCategories != null) {
            for (String category : orderedCategories) {
                normalized.add(normalizeCategory(category));
            }
        }
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        categories.clear();
        categories.addAll(normalized);
        saveConfig();
    }

    public static synchronized boolean addCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        if (containsCategoryIgnoreCase(normalized)) {
            return false;
        }
        categories.add(normalized);
        saveConfig();
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
                break;
            }
        }
        for (AutoPickupRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalizedOld)) {
                rule.category = normalizedNew;
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        ensureCategoriesSynced();
        saveConfig();
        return true;
    }

    public static synchronized boolean deleteCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();

        boolean changed = removeCategoryIgnoreCase(normalized);
        for (AutoPickupRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalized)) {
                rule.category = CATEGORY_DEFAULT;
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        ensureCategoriesSynced();
        saveConfig();
        return true;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LocalPlayer player)) {
            return;
        }

        if (player.level() == null || !globalEnabled) {
            clearFailedAttemptsForRule(activeRule);
            resetStateIfNeeded(true);
            return;
        }

        AutoPickupRule rule = resolveRuleForPlayer(player);
        if (rule == null || !rule.enabled) {
            clearFailedAttemptsForRule(activeRule);
            resetStateIfNeeded(true);
            return;
        }

        if (activeRule != rule) {
            clearFailedAttemptsForRule(activeRule);
            resetState(false);
            activeRule = rule;
            refreshInventorySnapshot(player, rule);
        } else {
            activeRule = rule;
        }

        syncInventoryPickupValidation(player, rule);
        processPendingPickupActions();

        int nowTick = player.tickCount;
        updateAntiStuckState(player, rule);

        if (currentState == State.WAITING_POST_PICKUP) {
            handlePostPickupDelay(player, rule);
            return;
        }

        if (currentTargetItem != null) {
            boolean targetValid = isTargetStillValid(currentTargetItem, rule);
            if (targetValid) {
                currentState = State.MOVING_TO_ITEM;
                lastTargetSeenTick = nowTick;
                if (hasReachedTarget(player, rule, currentTargetItem)) {
                    currentTargetSnapshot = currentTargetItem.getItem().copy();
                } else {
                    ensureNavigationToCurrentTarget(player, rule, nowTick);
                    return;
                }
            }

            if (!targetValid || !currentTargetItem.isAlive()) {
                handlePickupFinished(player, rule, currentTargetSnapshot);
                return;
            }
        }

        if (currentTargetItem != null && (nowTick - lastTargetSeenTick) <= TARGET_LOST_GRACE_TICKS) {
            return;
        }

        currentTargetItem = null;
        currentTargetSnapshot = ItemStack.EMPTY;
        currentState = State.SEARCHING;

        if (nowTick - lastTargetSearchTick < SEARCH_INTERVAL_TICKS) {
            return;
        }
        lastTargetSearchTick = nowTick;

        ItemEntity nextTarget = findNearestItemInRule(rule, player);
        if (nextTarget == null) {
            if (hasPickedUpAtLeastOneItem && !isBlank(rule.postPickupSequence)) {
                postPickupDelayTicks = Math.max(0, rule.postPickupDelaySeconds) * 20;
                currentState = State.WAITING_POST_PICKUP;
            } else {
                currentState = State.IDLE;
            }
            stopNavigation("当前规则范围内未找到可拾取物品");
            return;
        }

        currentTargetItem = nextTarget;
        currentTargetSnapshot = nextTarget.getItem().copy();
        lastTargetSeenTick = nowTick;
        currentState = State.MOVING_TO_ITEM;
        ensureNavigationToCurrentTarget(player, rule, nowTick);
    }

    public boolean shouldPrioritizeNavigation(LocalPlayer player) {
        if (player == null || !globalEnabled || activeRule == null || !activeRule.enabled) {
            return false;
        }
        return currentState == State.MOVING_TO_ITEM && currentTargetItem != null && currentTargetItem.isAlive();
    }

    public boolean isPlayerInsideEnabledRule(LocalPlayer player) {
        return player != null && resolveRuleForPlayer(player) != null;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        RenderWorldLastEvent.WorldRenderContext renderContext = event.getWorldRenderContext();
        if (MC.player == null || renderContext == null || rules.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.createWorldPoseStack();
        if (poseStack == null) {
            return;
        }
        for (AutoPickupRule rule : rules) {
            if (rule == null || !rule.enabled || !rule.visualizeRange || rule.radius <= 0.05D) {
                continue;
            }
            drawPickupRadiusAura(rule.centerX, rule.centerY, rule.centerZ, rule.radius, poseStack, renderContext);
        }
    }

    private void handlePostPickupDelay(LocalPlayer player, AutoPickupRule rule) {
        if (rule == null) {
            resetState(false);
            return;
        }
        if (rule.stopOnExit && !rule.isPlayerInside(player.getX(), player.getY(), player.getZ())) {
            resetState(false);
            return;
        }
        if (postPickupDelayTicks > 0) {
            postPickupDelayTicks--;
            return;
        }
        if (!isBlank(rule.postPickupSequence)) {
            startSequence(rule.postPickupSequence);
        }
        hasPickedUpAtLeastOneItem = false;
        currentState = State.IDLE;
    }

    private void handlePickupFinished(LocalPlayer player, AutoPickupRule rule, ItemStack pickedStack) {
        stopNavigation("当前物品已完成拾取或目标失效");
        if (pickedStack != null && !pickedStack.isEmpty()) {
            clearFailedAttemptsForTarget(rule, currentTargetItem);
            if (hasRestrictedInventoryDetectionSlots(rule)) {
                pendingInventoryValidationStack = pickedStack.copy();
                pendingInventoryValidationTicks = 4;
            } else {
                hasPickedUpAtLeastOneItem = true;
                queuePickupActions(rule, pickedStack);
            }
        }
        currentTargetItem = null;
        currentTargetSnapshot = ItemStack.EMPTY;
        currentState = State.SEARCHING;
        antiStuckStationaryTicks = 0;
        captureAntiStuckPosition(player);
    }

    private AutoPickupRule resolveRuleForPlayer(LocalPlayer player) {
        AutoPickupRule firstEnabled = null;
        for (AutoPickupRule rule : rules) {
            if (rule == null || !rule.enabled) {
                continue;
            }
            normalizeRule(rule);
            if (firstEnabled == null) {
                firstEnabled = rule;
            }
            if (rule.isPlayerInside(player.getX(), player.getY(), player.getZ())) {
                return rule;
            }
        }
        return firstEnabled != null && !firstEnabled.stopOnExit ? firstEnabled : null;
    }

    private ItemEntity findNearestItemInRule(AutoPickupRule rule, LocalPlayer player) {
        double radius = Math.max(0.5D, rule.radius);
        AABB searchBox = player.getBoundingBox().inflate(radius, radius, radius);
        ItemEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (!isTargetStillValid(item, rule)) {
                continue;
            }
            if (hasReachedMaxPickupAttempts(item, rule)) {
                continue;
            }
            double distanceSq = player.distanceToSqr(item);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = item;
            }
        }
        return best;
    }

    private boolean isTargetStillValid(ItemEntity item, AutoPickupRule rule) {
        if (item == null || !item.isAlive() || item.getItem().isEmpty() || rule == null) {
            return false;
        }
        Vec3 center = new Vec3(rule.centerX, rule.centerY, rule.centerZ);
        if (item.position().distanceTo(center) > Math.max(0.0D, rule.radius) + 1.0D) {
            return false;
        }
        return isItemEligibleForRule(item, rule);
    }

    private boolean isItemEligibleForRule(ItemEntity item, AutoPickupRule rule) {
        ItemStack stack = item.getItem();
        String itemName = safeLower(getFilterableItemName(stack));
        String searchableText = safeLower(ItemFilterHandler.buildItemSearchableText(stack));

        if (rule.enableItemWhitelist) {
            if (hasEntryRules(rule.itemWhitelistEntries)) {
                if (!matchesRuleEntryList(itemName, searchableText, stack, rule.itemWhitelistEntries)) {
                    return false;
                }
            } else if (!matchesLegacyKeywordList(itemName, rule.itemWhitelist)) {
                return false;
            }
        }

        if (rule.enableItemBlacklist) {
            if (hasEntryRules(rule.itemBlacklistEntries)) {
                if (matchesRuleEntryList(itemName, searchableText, stack, rule.itemBlacklistEntries)) {
                    return false;
                }
            } else if (matchesLegacyKeywordList(itemName, rule.itemBlacklist)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasReachedTarget(LocalPlayer player, AutoPickupRule rule, ItemEntity item) {
        double distance = Math.max(0.25D, rule.targetReachDistance);
        return player.distanceToSqr(item) <= distance * distance;
    }

    private void ensureNavigationToCurrentTarget(LocalPlayer player, AutoPickupRule rule, int nowTick) {
        if (currentTargetItem == null) {
            return;
        }
        double targetX = currentTargetItem.getX();
        double targetY = currentTargetItem.getY();
        double targetZ = currentTargetItem.getZ();

        boolean targetMoved = Double.isNaN(lastIssuedTargetX)
                || square(targetX - lastIssuedTargetX) + square(targetY - lastIssuedTargetY)
                        + square(targetZ - lastIssuedTargetZ) >= NAVIGATION_MOVEMENT_THRESHOLD_SQ;

        if (!targetMoved && (nowTick - lastGotoTick) < GOTO_INTERVAL_TICKS) {
            return;
        }

        lastGotoTick = nowTick;
        lastIssuedTargetX = targetX;
        lastIssuedTargetY = targetY;
        lastIssuedTargetZ = targetZ;
        EmbeddedNavigationHandler.INSTANCE.startGoto(EmbeddedNavigationHandler.NavigationOwner.AUTO_PICKUP,
                targetX, targetY, targetZ, true, "锁定物品目标并刷新自动拾取导航");
        navigationIssuedByAutoPickup = true;
    }

    private void updateAntiStuckState(LocalPlayer player, AutoPickupRule rule) {
        if (!rule.antiStuckEnabled || currentState != State.MOVING_TO_ITEM || currentTargetItem == null) {
            antiStuckStationaryTicks = 0;
            captureAntiStuckPosition(player);
            return;
        }

        if (Double.isNaN(antiStuckLastPosX)) {
            captureAntiStuckPosition(player);
            return;
        }

        double movedSq = square(player.getX() - antiStuckLastPosX)
                + square(player.getY() - antiStuckLastPosY)
                + square(player.getZ() - antiStuckLastPosZ);
        if (movedSq > 0.04D) {
            antiStuckStationaryTicks = 0;
            captureAntiStuckPosition(player);
            return;
        }

        antiStuckStationaryTicks++;
        int limit = Math.max(20, rule.antiStuckTimeoutSeconds * 20);
        if (antiStuckStationaryTicks < limit) {
            return;
        }

        incrementFailedAttempts(rule, buildItemLocationKey(currentTargetItem));
        if (!isBlank(rule.antiStuckRestartSequence)) {
            startSequence(rule.antiStuckRestartSequence);
        }
        resetState(false);
    }

    private void queuePickupActions(AutoPickupRule rule, ItemStack stack) {
        AutoPickupRule.PickupActionEntry matched = findMatchingPickupActionEntry(rule, stack);
        if (matched == null || isBlank(matched.sequenceName)) {
            return;
        }
        pendingPickupActions.add(new PendingPickupSequenceAction(matched.sequenceName,
                Math.max(0, matched.executeDelaySeconds) * 20));
    }

    private AutoPickupRule.PickupActionEntry findMatchingPickupActionEntry(AutoPickupRule rule, ItemStack stack) {
        if (rule == null || stack == null || stack.isEmpty() || rule.pickupActionEntries == null) {
            return null;
        }
        String itemName = safeLower(getFilterableItemName(stack));
        String searchableText = safeLower(ItemFilterHandler.buildItemSearchableText(stack));
        for (AutoPickupRule.PickupActionEntry entry : rule.pickupActionEntries) {
            if (entry == null || isBlank(entry.keyword)) {
                continue;
            }
            String keyword = safeLower(entry.keyword);
            if (!itemName.contains(keyword) && !searchableText.contains(keyword)) {
                continue;
            }
            if (!ItemFilterHandler.matchesRequiredNbtTags(stack, entry.requiredNbtTags,
                    ItemFilterHandler.NBT_TAG_MATCH_MODE_CONTAINS)) {
                continue;
            }
            return entry;
        }
        return null;
    }

    private void processPendingPickupActions() {
        for (int i = pendingPickupActions.size() - 1; i >= 0; i--) {
            PendingPickupSequenceAction action = pendingPickupActions.get(i);
            if (action == null) {
                pendingPickupActions.remove(i);
                continue;
            }
            if (action.remainingTicks > 0) {
                action.remainingTicks--;
                continue;
            }
            if (!isBlank(action.sequenceName)) {
                startSequence(action.sequenceName);
            }
            pendingPickupActions.remove(i);
        }
    }

    private void startSequence(String sequenceName) {
        if (isBlank(sequenceName) || MC.player == null) {
            return;
        }
        PathSequenceManager.executeSequenceByConfiguredMode(sequenceName.trim(), MC.player, "", null);
    }

    private void resetStateIfNeeded(boolean clearPendingActions) {
        if (!hasRuntimeState(clearPendingActions)) {
            return;
        }
        resetState(clearPendingActions);
    }

    private void resetState(boolean clearPendingActions) {
        stopNavigation("重置自动拾取运行状态");
        activeRule = null;
        currentTargetItem = null;
        currentTargetSnapshot = ItemStack.EMPTY;
        pendingInventoryValidationStack = ItemStack.EMPTY;
        pendingInventoryValidationTicks = 0;
        lastInventoryCounts.clear();
        currentState = State.IDLE;
        postPickupDelayTicks = 0;
        lastGotoTick = -99999;
        lastTargetSearchTick = -99999;
        lastTargetSeenTick = -99999;
        lastIssuedTargetX = Double.NaN;
        lastIssuedTargetY = Double.NaN;
        lastIssuedTargetZ = Double.NaN;
        navigationIssuedByAutoPickup = false;
        antiStuckStationaryTicks = 0;
        antiStuckLastPosX = Double.NaN;
        antiStuckLastPosY = Double.NaN;
        antiStuckLastPosZ = Double.NaN;
        hasPickedUpAtLeastOneItem = false;
        if (clearPendingActions) {
            pendingPickupActions.clear();
        }
    }

    private boolean hasRuntimeState(boolean clearPendingActions) {
        if (activeRule != null
                || currentTargetItem != null
                || currentState != State.IDLE
                || postPickupDelayTicks > 0
                || navigationIssuedByAutoPickup
                || hasPickedUpAtLeastOneItem
                || !Double.isNaN(lastIssuedTargetX)
                || !Double.isNaN(lastIssuedTargetY)
                || !Double.isNaN(lastIssuedTargetZ)
                || antiStuckStationaryTicks > 0) {
            return true;
        }
        return clearPendingActions && !pendingPickupActions.isEmpty();
    }

    private void stopNavigation(String reason) {
        if (!navigationIssuedByAutoPickup) {
            return;
        }
        EmbeddedNavigationHandler.INSTANCE.stopOwned(EmbeddedNavigationHandler.NavigationOwner.AUTO_PICKUP, reason);
        navigationIssuedByAutoPickup = false;
    }

    private int getMaxPickupAttempts(AutoPickupRule rule) {
        return rule == null ? AutoPickupRule.DEFAULT_MAX_PICKUP_ATTEMPTS
                : Math.max(1, rule.maxPickupAttempts);
    }

    private void syncInventoryPickupValidation(LocalPlayer player, AutoPickupRule rule) {
        Map<String, Integer> currentCounts = captureInventoryCounts(player, rule);
        if (pendingInventoryValidationTicks > 0 && pendingInventoryValidationStack != null
                && !pendingInventoryValidationStack.isEmpty()) {
            String key = buildInventoryCountKey(pendingInventoryValidationStack);
            int previous = lastInventoryCounts.getOrDefault(key, 0);
            int current = currentCounts.getOrDefault(key, 0);
            if (current > previous) {
                hasPickedUpAtLeastOneItem = true;
                queuePickupActions(rule, pendingInventoryValidationStack);
                pendingInventoryValidationStack = ItemStack.EMPTY;
                pendingInventoryValidationTicks = 0;
            } else {
                pendingInventoryValidationTicks--;
                if (pendingInventoryValidationTicks <= 0) {
                    pendingInventoryValidationStack = ItemStack.EMPTY;
                }
            }
        }
        lastInventoryCounts.clear();
        lastInventoryCounts.putAll(currentCounts);
    }

    private void refreshInventorySnapshot(LocalPlayer player, AutoPickupRule rule) {
        lastInventoryCounts.clear();
        lastInventoryCounts.putAll(captureInventoryCounts(player, rule));
        pendingInventoryValidationStack = ItemStack.EMPTY;
        pendingInventoryValidationTicks = 0;
    }

    private Map<String, Integer> captureInventoryCounts(LocalPlayer player, AutoPickupRule rule) {
        Map<String, Integer> counts = new HashMap<>();
        if (player == null) {
            return counts;
        }
        List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        for (int rawSlotIndex = 0; rawSlotIndex < items.size(); rawSlotIndex++) {
            if (!isInventoryDetectionSlotIncluded(rule, rawSlotIndex)) {
                continue;
            }
            ItemStack stack = items.get(rawSlotIndex);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String key = buildInventoryCountKey(stack);
            counts.put(key, counts.getOrDefault(key, 0) + stack.getCount());
        }
        return counts;
    }

    private String buildInventoryCountKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String tag = ItemComponentCompat.getComponentKey(stack);
        return ItemComponentCompat.getDescriptionId(stack) + "|" + stack.getHoverName().getString() + "|" + tag;
    }

    private boolean hasRestrictedInventoryDetectionSlots(AutoPickupRule rule) {
        return rule != null && rule.inventoryDetectionSlots != null && !rule.inventoryDetectionSlots.isEmpty();
    }

    private boolean isInventoryDetectionSlotIncluded(AutoPickupRule rule, int rawSlotIndex) {
        if (!hasRestrictedInventoryDetectionSlots(rule)) {
            return true;
        }
        int visibleSlotIndex = toVisibleInventorySlotIndex(rawSlotIndex);
        return visibleSlotIndex >= 0 && rule.inventoryDetectionSlots.contains(visibleSlotIndex);
    }

    private int toVisibleInventorySlotIndex(int rawSlotIndex) {
        if (rawSlotIndex < 0 || rawSlotIndex >= AutoPickupRule.INVENTORY_SLOT_COUNT) {
            return -1;
        }
        if (rawSlotIndex < 9) {
            return 27 + rawSlotIndex;
        }
        return rawSlotIndex - 9;
    }

    private boolean hasReachedMaxPickupAttempts(ItemEntity item, AutoPickupRule rule) {
        if (item == null || rule == null) {
            return false;
        }
        Map<ItemLocationKey, Integer> attempts = failedPickupAttemptsByRule.get(buildRuleAttemptKey(rule));
        if (attempts == null) {
            return false;
        }
        return attempts.getOrDefault(buildItemLocationKey(item), 0) >= getMaxPickupAttempts(rule);
    }

    private void clearFailedAttemptsForTarget(AutoPickupRule rule, ItemEntity item) {
        if (rule == null || item == null) {
            return;
        }
        Map<ItemLocationKey, Integer> attempts = failedPickupAttemptsByRule.get(buildRuleAttemptKey(rule));
        if (attempts != null) {
            attempts.remove(buildItemLocationKey(item));
        }
    }

    private void clearFailedAttemptsForRule(AutoPickupRule rule) {
        if (rule == null) {
            return;
        }
        failedPickupAttemptsByRule.remove(buildRuleAttemptKey(rule));
    }

    private int incrementFailedAttempts(AutoPickupRule rule, ItemLocationKey key) {
        if (rule == null || key == null) {
            return 0;
        }
        Map<ItemLocationKey, Integer> attempts = failedPickupAttemptsByRule.computeIfAbsent(
                buildRuleAttemptKey(rule), unused -> new HashMap<>());
        int next = attempts.getOrDefault(key, 0) + 1;
        attempts.put(key, next);
        return next;
    }

    private ItemLocationKey buildItemLocationKey(ItemEntity item) {
        return new ItemLocationKey(Mth.floor(item.getX() * 4.0D), Mth.floor(item.getY() * 4.0D),
                Mth.floor(item.getZ() * 4.0D));
    }

    private String buildRuleAttemptKey(AutoPickupRule rule) {
        return safeLower(rule == null ? "" : rule.name);
    }

    private static boolean hasEntryRules(List<AutoPickupRule.ItemMatchEntry> entries) {
        return entries != null && !entries.isEmpty();
    }

    private static boolean matchesRuleEntryList(String itemName, String searchableText, ItemStack stack,
            List<AutoPickupRule.ItemMatchEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (AutoPickupRule.ItemMatchEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            String keyword = safeLower(entry.keyword);
            if (keyword.isEmpty()) {
                continue;
            }
            if (!itemName.contains(keyword) && !searchableText.contains(keyword)) {
                continue;
            }
            if (ItemFilterHandler.matchesRequiredNbtTags(stack, entry.requiredNbtTags,
                    ItemFilterHandler.NBT_TAG_MATCH_MODE_CONTAINS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesLegacyKeywordList(String itemName, List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return false;
        }
        for (String filter : filters) {
            String keyword = safeLower(filter);
            if (!keyword.isEmpty() && itemName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String getFilterableItemName(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private void drawPickupRadiusAura(double centerX, double centerY, double centerZ, double radius,
            PoseStack poseStack, RenderWorldLastEvent.WorldRenderContext renderContext) {
        if (radius <= 0.05D) {
            return;
        }
        WorldGizmoRenderer.verticalRingWall(centerX, centerY + 0.05D, centerZ, radius,
                0.0D, 1.2D, new Color(0x55FF9A), 0.72F, 2.0F, true);
    }

    private static void normalizeRule(AutoPickupRule rule) {
        if (rule == null) {
            return;
        }
        if (rule.name == null || rule.name.trim().isEmpty()) {
            rule.name = "规则";
        }
        rule.category = normalizeCategory(rule.category);
        rule.targetReachDistance = Math.max(0.25D, rule.targetReachDistance <= 0.0D
                ? AutoPickupRule.DEFAULT_TARGET_REACH_DISTANCE
                : rule.targetReachDistance);
        rule.maxPickupAttempts = Math.max(1, rule.maxPickupAttempts <= 0
                ? AutoPickupRule.DEFAULT_MAX_PICKUP_ATTEMPTS
                : rule.maxPickupAttempts);
        if (rule.itemWhitelist == null) {
            rule.itemWhitelist = new ArrayList<>();
        }
        if (rule.itemBlacklist == null) {
            rule.itemBlacklist = new ArrayList<>();
        }
        if (rule.itemWhitelistEntries == null) {
            rule.itemWhitelistEntries = new ArrayList<>();
        }
        if (rule.itemBlacklistEntries == null) {
            rule.itemBlacklistEntries = new ArrayList<>();
        }
        if (rule.pickupActionEntries == null) {
            rule.pickupActionEntries = new ArrayList<>();
        }
        if (rule.inventoryDetectionSlots == null) {
            rule.inventoryDetectionSlots = new ArrayList<>();
        } else {
            rule.inventoryDetectionSlots = normalizeInventoryDetectionSlots(rule.inventoryDetectionSlots);
        }
        if (rule.antiStuckRestartSequence == null) {
            rule.antiStuckRestartSequence = "";
        }
    }

    private static List<Integer> normalizeInventoryDetectionSlots(List<Integer> source) {
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        if (source != null) {
            for (Integer slot : source) {
                if (slot == null) {
                    continue;
                }
                int slotIndex = slot.intValue();
                if (slotIndex >= 0 && slotIndex < AutoPickupRule.INVENTORY_SLOT_COUNT) {
                    normalized.add(slotIndex);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private static void ensureCategoriesSynced() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        for (AutoPickupRule rule : rules) {
            if (rule == null) {
                continue;
            }
            normalizeRule(rule);
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

    private static boolean removeCategoryIgnoreCase(String category) {
        for (int i = 0; i < categories.size(); i++) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalizeCategory(category))) {
                categories.remove(i);
                return true;
            }
        }
        return false;
    }

    private void captureAntiStuckPosition(LocalPlayer player) {
        antiStuckLastPosX = player.getX();
        antiStuckLastPosY = player.getY();
        antiStuckLastPosZ = player.getZ();
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static double square(double value) {
        return value * value;
    }
}
