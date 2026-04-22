package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.system.AutoEscapeRule;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoEscapeHandler {

    public static final AutoEscapeHandler INSTANCE = new AutoEscapeHandler();

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<AutoEscapeRule> RULES = new CopyOnWriteArrayList<>();
    private static final List<String> CATEGORIES = new CopyOnWriteArrayList<>();
    private static final String CATEGORY_DEFAULT = "默认";
    private static boolean globalEnabled = true;

    private enum RuntimeState {
        IDLE,
        ESCAPING,
        WAITING_RESTART
    }

    private static RuntimeState runtimeState = RuntimeState.IDLE;
    private static AutoEscapeRule activeRule = null;
    private static String pendingRestartSequenceName = "";
    private static long restartExecuteAtMs = 0L;
    private static long lastNotifyAtMs = 0L;

    private AutoEscapeHandler() {
    }

    public static synchronized void loadConfig() {
        RULES.clear();
        CATEGORIES.clear();
        globalEnabled = true;

        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            ensureCategoriesSynced();
            resetRuntimeState();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            List<AutoEscapeRule> loadedRules = null;
            List<String> loadedCategories = new ArrayList<>();

            if (parsed != null && parsed.isJsonObject()) {
                JsonObject root = parsed.getAsJsonObject();
                if (root.has("globalEnabled")) {
                    globalEnabled = root.get("globalEnabled").getAsBoolean();
                }
                if (root.has("categories") && root.get("categories").isJsonArray()) {
                    JsonArray categoryArray = root.getAsJsonArray("categories");
                    for (JsonElement element : categoryArray) {
                        if (element != null && element.isJsonPrimitive()) {
                            loadedCategories.add(element.getAsString());
                        }
                    }
                }
                if (root.has("rules") && root.get("rules").isJsonArray()) {
                    Type listType = new TypeToken<ArrayList<AutoEscapeRule>>() {
                    }.getType();
                    loadedRules = GSON.fromJson(root.get("rules"), listType);
                }
            } else if (parsed != null && parsed.isJsonArray()) {
                Type listType = new TypeToken<ArrayList<AutoEscapeRule>>() {
                }.getType();
                loadedRules = GSON.fromJson(parsed, listType);
            }

            CATEGORIES.addAll(loadedCategories);
            if (loadedRules != null) {
                for (AutoEscapeRule rule : loadedRules) {
                    if (rule == null) {
                        continue;
                    }
                    rule.normalize();
                    rule.resetRuntimeState();
                    RULES.add(rule);
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载自动逃离规则失败", e);
            RULES.clear();
            CATEGORIES.clear();
        }

        ensureCategoriesSynced();
        resetRuntimeState();
    }

    public static synchronized void saveConfig() {
        ensureCategoriesSynced();
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.addProperty("globalEnabled", globalEnabled);
                root.add("categories", GSON.toJsonTree(new ArrayList<>(CATEGORIES)));
                root.add("rules", GSON.toJsonTree(new ArrayList<>(RULES)));
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动逃离规则失败", e);
        }
    }

    public static synchronized List<AutoEscapeRule> getRulesSnapshot() {
        return new ArrayList<>(RULES);
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        ensureCategoriesSynced();
        return new ArrayList<>(CATEGORIES);
    }

    public static synchronized boolean isGloballyEnabled() {
        return globalEnabled;
    }

    public static synchronized void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
        if (!globalEnabled) {
            deactivateRuntimeBecauseDisabled();
        }
        saveConfig();
    }

    public static synchronized boolean addCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        if (containsCategoryIgnoreCase(normalized)) {
            return false;
        }
        CATEGORIES.add(normalized);
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
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (normalizeCategory(CATEGORIES.get(i)).equalsIgnoreCase(normalizedOld)) {
                CATEGORIES.set(i, normalizedNew);
                changed = true;
                break;
            }
        }

        for (AutoEscapeRule rule : RULES) {
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
        for (AutoEscapeRule rule : RULES) {
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

    public static synchronized void replaceAllRules(List<AutoEscapeRule> newRules) {
        RULES.clear();
        if (newRules != null) {
            for (AutoEscapeRule rule : newRules) {
                if (rule == null) {
                    continue;
                }
                rule.normalize();
                rule.resetRuntimeState();
                RULES.add(rule);
            }
        }
        ensureCategoriesSynced();
        saveConfig();
        resetRuntimeState();
    }

    public static boolean isEmergencyLockActive() {
        return runtimeState != RuntimeState.IDLE;
    }

    public static boolean isEscapeSequenceRunning() {
        return runtimeState == RuntimeState.ESCAPING;
    }

    public static void resetRuntimeState() {
        runtimeState = RuntimeState.IDLE;
        activeRule = null;
        pendingRestartSequenceName = "";
        restartExecuteAtMs = 0L;
        lastNotifyAtMs = 0L;
        for (AutoEscapeRule rule : getRulesSnapshot()) {
            rule.resetRuntimeState();
        }
    }

    private static void deactivateRuntimeBecauseDisabled() {
        if (runtimeState == RuntimeState.ESCAPING) {
            stopCurrentSequenceImmediately();
        }
        resetRuntimeState();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof LocalPlayer player) || MC.level == null) {
            return;
        }

        if (!globalEnabled) {
            if (runtimeState != RuntimeState.IDLE) {
                deactivateRuntimeBecauseDisabled();
            }
            return;
        }

        List<AutoEscapeRule> snapshot = getRulesSnapshot();
        AutoEscapeRule candidateRule = null;
        boolean anyThreatPresent = false;

        for (AutoEscapeRule rule : snapshot) {
            if (rule == null) {
                continue;
            }
            rule.normalize();
            boolean matched = rule.enabled && hasMatchingThreatNearby(player, rule);
            if (matched) {
                anyThreatPresent = true;
                if (candidateRule == null && !rule.triggerLatched && hasValidEscapeSequence(rule)) {
                    candidateRule = rule;
                }
                rule.triggerLatched = true;
            } else {
                rule.triggerLatched = false;
            }
        }

        if (runtimeState == RuntimeState.ESCAPING) {
            if (!PathSequenceEventListener.instance.isTracking()) {
                onEscapeSequenceCompleted();
            }
            return;
        }

        if (candidateRule != null) {
            triggerEscape(candidateRule);
            return;
        }

        if (runtimeState == RuntimeState.WAITING_RESTART) {
            if (pendingRestartSequenceName == null || pendingRestartSequenceName.trim().isEmpty()) {
                resetRuntimeState();
                return;
            }

            if (System.currentTimeMillis() >= restartExecuteAtMs && !anyThreatPresent) {
                runPendingRestartSequence();
            }
        }
    }

    private static void onEscapeSequenceCompleted() {
        if (activeRule == null) {
            resetRuntimeState();
            return;
        }

        if (activeRule.restartEnabled
                && activeRule.restartSequenceName != null
                && !activeRule.restartSequenceName.trim().isEmpty()) {
            pendingRestartSequenceName = activeRule.restartSequenceName.trim();
            restartExecuteAtMs = System.currentTimeMillis() + Math.max(0, activeRule.restartDelaySeconds) * 1000L;
            runtimeState = RuntimeState.WAITING_RESTART;
            notifyPlayer("§b[自动逃离] §a逃离序列已完成，将在 §e"
                    + Math.max(0, activeRule.restartDelaySeconds)
                    + " §a秒后执行后续序列。");
            return;
        }

        notifyPlayer("§b[自动逃离] §a逃离序列已完成。");
        resetRuntimeState();
    }

    private static void runPendingRestartSequence() {
        String sequenceName = pendingRestartSequenceName == null ? "" : pendingRestartSequenceName.trim();
        if (sequenceName.isEmpty()) {
            resetRuntimeState();
            return;
        }

        if (!PathSequenceManager.hasSequence(sequenceName)) {
            notifyPlayer("§b[自动逃离] §c后续序列不存在，无法执行: §f" + sequenceName);
            resetRuntimeState();
            return;
        }

        stopCurrentSequenceImmediately();
        PathSequenceManager.runPathSequenceOnce(sequenceName);
        notifyPlayer("§b[自动逃离] §a开始执行后续序列: §f" + sequenceName);
        resetRuntimeState();
    }

    private static void triggerEscape(AutoEscapeRule rule) {
        if (rule == null) {
            return;
        }

        String sequenceName = rule.escapeSequenceName == null ? "" : rule.escapeSequenceName.trim();
        if (sequenceName.isEmpty()) {
            return;
        }
        if (!PathSequenceManager.hasSequence(sequenceName)) {
            notifyPlayer("§b[自动逃离] §c逃离序列不存在，无法执行: §f" + sequenceName);
            return;
        }

        activeRule = rule;
        pendingRestartSequenceName = "";
        restartExecuteAtMs = 0L;
        runtimeState = RuntimeState.ESCAPING;

        stopCurrentSequenceImmediately();
        PathSequenceManager.runPathSequenceOnce(sequenceName);

        notifyPlayer("§b[自动逃离] §e检测到附近目标，开始执行逃离序列: §f" + sequenceName);
    }

    private static void stopCurrentSequenceImmediately() {
        EmbeddedNavigationHandler.INSTANCE.forceStop("自动逃离触发时强制打断当前导航");
        PathSequenceManager.clearRunSequenceCallStack();
        if (PathSequenceEventListener.instance.isTracking()) {
            PathSequenceEventListener.instance.stopTracking();
        }
        GuiInventory.isLooping = false;
    }

    private static boolean hasMatchingThreatNearby(LocalPlayer player, AutoEscapeRule rule) {
        if (player == null || player.level() == null || rule == null) {
            return false;
        }

        double range = rule.detectionRange <= 0 ? AutoEscapeRule.DEFAULT_DETECTION_RANGE : rule.detectionRange;
        AABB box = player.getBoundingBox().inflate(range, range, range);
        List<Entity> entities = player.level().getEntities(player, box, entity -> entity != null && entity != player);
        for (Entity entity : entities) {
            if (!entity.isAlive()) {
                continue;
            }
            if (player.distanceToSqr(entity) > range * range) {
                continue;
            }
            if (!matchesEntityType(entity, rule)) {
                continue;
            }
            if (!matchesNameFilters(entity, rule)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean matchesNameFilters(Entity entity, AutoEscapeRule rule) {
        String name = entity == null ? "" : entity.getName().getString().trim();
        String lowered = name.toLowerCase(Locale.ROOT);

        if (rule.enableNameWhitelist && rule.nameWhitelist != null && !rule.nameWhitelist.isEmpty()) {
            boolean matchedWhitelist = false;
            for (String keyword : rule.nameWhitelist) {
                if (keyword != null && !keyword.trim().isEmpty()
                        && lowered.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                    matchedWhitelist = true;
                    break;
                }
            }
            if (!matchedWhitelist) {
                return false;
            }
        }

        if (rule.enableNameBlacklist && rule.nameBlacklist != null && !rule.nameBlacklist.isEmpty()) {
            for (String keyword : rule.nameBlacklist) {
                if (keyword != null && !keyword.trim().isEmpty()
                        && lowered.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean matchesEntityType(Entity entity, AutoEscapeRule rule) {
        if (entity == null) {
            return false;
        }

        List<String> types = rule.entityTypes;
        if (types == null || types.isEmpty()) {
            return entity instanceof LivingEntity;
        }

        for (String rawType : types) {
            if (matchesEntityTypeToken(entity, rawType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEntityTypeToken(Entity entity, String rawType) {
        String token = rawType == null ? "" : rawType.trim().toLowerCase(Locale.ROOT);
        if (token.isEmpty()) {
            return false;
        }

        switch (token) {
            case "任意":
            case "any":
                return true;
            case "生物":
            case "living":
                return entity instanceof LivingEntity;
            case "玩家":
            case "player":
                return entity instanceof Player;
            case "怪物":
            case "monster":
            case "mob":
            case "hostile":
                return entity instanceof Enemy;
            case "中立":
            case "中立生物":
            case "neutral":
                return entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof Enemy);
            case "动物":
            case "animal":
            case "passive":
                return entity instanceof Animal || entity instanceof AgeableMob || entity instanceof AbstractHorse;
            case "水生":
            case "water":
                return entity instanceof WaterAnimal;
            case "环境":
            case "ambient":
                return entity instanceof AmbientCreature;
            case "村民":
            case "villager":
            case "npc":
                return entity instanceof Villager;
            case "傀儡":
            case "golem":
                return entity instanceof AbstractGolem;
            case "驯服":
            case "宠物":
            case "tameable":
                return entity instanceof TamableAnimal;
            case "首领":
            case "boss":
                return entity instanceof EnderDragon || entity instanceof WitherBoss;
            default:
                return false;
        }
    }

    private static boolean hasValidEscapeSequence(AutoEscapeRule rule) {
        return rule != null
                && rule.escapeSequenceName != null
                && !rule.escapeSequenceName.trim().isEmpty()
                && PathSequenceManager.hasSequence(rule.escapeSequenceName.trim());
    }

    private static void notifyPlayer(String message) {
        long now = System.currentTimeMillis();
        if (now - lastNotifyAtMs < 300L) {
            return;
        }
        lastNotifyAtMs = now;
        if (MC.player != null) {
            MC.player.sendSystemMessage(Component.literal(message));
        }
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_escape_rules.json");
    }

    private static void ensureCategoriesSynced() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String category : CATEGORIES) {
            normalized.add(normalizeCategory(category));
        }
        for (AutoEscapeRule rule : RULES) {
            if (rule == null) {
                continue;
            }
            rule.normalize();
            rule.category = normalizeCategory(rule.category);
            normalized.add(rule.category);
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        CATEGORIES.clear();
        CATEGORIES.addAll(normalized);
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private static boolean containsCategoryIgnoreCase(String category) {
        for (String existing : CATEGORIES) {
            if (normalizeCategory(existing).equalsIgnoreCase(normalizeCategory(category))) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeCategoryIgnoreCase(String category) {
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (normalizeCategory(CATEGORIES.get(i)).equalsIgnoreCase(normalizeCategory(category))) {
                CATEGORIES.remove(i);
                return true;
            }
        }
        return false;
    }
}
