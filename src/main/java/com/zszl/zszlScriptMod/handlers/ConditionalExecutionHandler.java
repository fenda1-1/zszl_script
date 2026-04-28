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
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.system.ConditionalRule;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
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
import java.util.concurrent.CopyOnWriteArrayList;

public class ConditionalExecutionHandler {

    public static final class ForegroundSequenceStartDecision {
        private final boolean allowed;
        private final String source;
        private final String reason;

        private ForegroundSequenceStartDecision(boolean allowed, String source, String reason) {
            this.allowed = allowed;
            this.source = source == null ? "" : source;
            this.reason = reason == null ? "" : reason;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getSource() {
            return source;
        }

        public String getReason() {
            return reason;
        }
    }

    public static final ConditionalExecutionHandler INSTANCE = new ConditionalExecutionHandler();

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEBUG_LINES_LIMIT = 5;

    public static boolean globalEnabled = true;
    public static final List<ConditionalRule> rules = new CopyOnWriteArrayList<>();
    private static final List<String> categories = new CopyOnWriteArrayList<>();

    private static final String CATEGORY_DEFAULT = "默认";
    private static ConditionalRule activeRule = null;
    private static final List<String> debugLines = new ArrayList<>();
    private static ConditionalRule antiStuckMonitorRule = null;
    private static int antiStuckStationaryTicks = 0;
    private static boolean antiStuckWarnSent = false;
    private static double antiStuckLastPosX = Double.NaN;
    private static double antiStuckLastPosY = Double.NaN;
    private static double antiStuckLastPosZ = Double.NaN;
    private static ConditionalRule antiStuckEntryGraceRule = null;
    private static int antiStuckEntryGraceTicks = 0;

    private ConditionalExecutionHandler() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("conditional_rules.json");
    }

    public static synchronized void loadConfig() {
        Path configFile = getConfigFile();
        rules.clear();
        categories.clear();
        globalEnabled = true;
        activeRule = null;
        clearAntiStuckState();
        updateDebugLines(Collections.emptyList());

        if (!Files.exists(configFile)) {
            ensureCategoriesSynced();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            List<ConditionalRule> loadedRules = null;
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
                    Type listType = new TypeToken<ArrayList<ConditionalRule>>() {
                    }.getType();
                    loadedRules = GSON.fromJson(root.get("rules"), listType);
                }
            } else if (parsed != null && parsed.isJsonArray()) {
                Type listType = new TypeToken<ArrayList<ConditionalRule>>() {
                }.getType();
                loadedRules = GSON.fromJson(parsed, listType);
            }

            if (loadedRules != null) {
                rules.addAll(loadedRules);
            }
            categories.addAll(loadedCategories);

            pruneLegacyInitializationRules();
            ensureCategoriesSynced();
            sanitizeRules();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法加载条件执行规则", e);
            rules.clear();
            categories.clear();
            ensureCategoriesSynced();
            sanitizeRules();
        }
    }

    public static synchronized void saveConfig() {
        try {
            pruneLegacyInitializationRules();
            ensureCategoriesSynced();
            sanitizeRules();

            Path configFile = getConfigFile();
            Files.createDirectories(configFile.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("globalEnabled", globalEnabled);
            root.add("categories", GSON.toJsonTree(new ArrayList<>(categories)));
            root.add("rules", GSON.toJsonTree(snapshotRules()));

            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法保存条件执行规则", e);
        }
    }

    private static void sanitizeRules() {
        for (ConditionalRule rule : rules) {
            if (rule != null) {
                rule.normalize();
            }
        }
    }

    private static void pruneLegacyInitializationRules() {
        boolean removedActiveRule = isLegacyInitializationRule(activeRule);
        for (int i = rules.size() - 1; i >= 0; i--) {
            if (isLegacyInitializationRule(rules.get(i))) {
                rules.remove(i);
            }
        }
        if (removedActiveRule) {
            activeRule = null;
        }
    }

    private static boolean isLegacyInitializationRule(ConditionalRule rule) {
        return rule != null
                && "初始化界面".equals(rule.name)
                && "初始化界面".equals(rule.sequenceName)
                && Math.abs(rule.centerX - 185.51D) < 0.0001D
                && Math.abs(rule.centerY - 14.0D) < 0.0001D
                && Math.abs(rule.centerZ - (-587.42D)) < 0.0001D
                && Math.abs(rule.range - 10.0D) < 0.0001D
                && !rule.stopOnExit
                && rule.loopCount == 1
                && rule.cooldownSeconds == 30
                && rule.runOncePerEntry
                && !rule.antiStuckEnabled
                && !rule.visualizeRange
                && ConditionalRule.DEFAULT_VISUALIZE_BORDER_COLOR
                        .equals(ConditionalRule.normalizeColor(rule.visualizeBorderColor));
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
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

    private static void ensureCategoriesSynced() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        for (ConditionalRule rule : rules) {
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
        categories.clear();
        categories.addAll(normalized);
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
        for (ConditionalRule rule : rules) {
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
        for (ConditionalRule rule : rules) {
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

    public static List<String> getDebugLinesSnapshot() {
        synchronized (debugLines) {
            return new ArrayList<>(debugLines);
        }
    }

    public static boolean shouldRenderDebugOverlay() {
        return ModConfig.isDebugFlagEnabled(DebugModule.CONDITIONAL_EXECUTION);
    }

    public static boolean isGloballyEnabled() {
        return globalEnabled;
    }

    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
        if (!enabled) {
            INSTANCE.stopRuntimeActivity("条件执行总开关关闭");
        }
        saveConfig();
    }

    public static ForegroundSequenceStartDecision assessForegroundSequenceStart(String sequenceName, LocalPlayer player) {
        return allowStartDecision();
    }

    private static void updateDebugLines(List<String> lines) {
        synchronized (debugLines) {
            debugLines.clear();
            if (lines == null || lines.isEmpty()) {
                return;
            }
            int count = Math.min(DEBUG_LINES_LIMIT, lines.size());
            for (int i = 0; i < count; i++) {
                debugLines.add(lines.get(i));
            }
        }
    }

    private static String formatRuleStatus(ConditionalRule rule, boolean inRange, boolean enteredThisTick,
            boolean onCooldown) {
        StringBuilder sb = new StringBuilder();
        sb.append(rule == activeRule ? "§a> " : "§7- ");
        sb.append(rule.name == null || rule.name.trim().isEmpty() ? "<未命名规则>" : rule.name);
        sb.append(" §7[");
        sb.append(inRange ? "§a区域内" : "§c区域外");
        if (enteredThisTick) {
            sb.append("§7, §b刚进入");
        }
        if (rule.runOncePerEntry && rule.hasBeenTriggered) {
            sb.append("§7, §e本次已触发");
        }
        if (onCooldown) {
            sb.append("§7, §6冷却 ").append(String.format("%.1fs", rule.getCooldownRemainingSeconds()));
        } else {
            sb.append("§7, §a就绪");
        }
        sb.append("§7]");
        return sb.toString();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event == null
                || event.phase != TickEvent.Phase.START
                || MC.player == null
                || MC.level == null
                || event.player != MC.player
                || !(event.player instanceof LocalPlayer player)) {
            return;
        }

        if (!globalEnabled) {
            stopRuntimeActivity("条件执行总开关关闭");
            updateDebugLines(Collections.singletonList("§b[条件执行] §7总开关关闭"));
            return;
        }

        if (AutoEscapeHandler.isEmergencyLockActive()) {
            clearAntiStuckState();
            updateDebugLines(Collections.singletonList("§b[条件执行] §e已被自动逃离临时接管"));
            return;
        }

        pruneLegacyInitializationRules();
        sanitizeRules();
        List<ConditionalRule> rulesSnapshot = snapshotRules();
        List<String> runtimeDebugLines = new ArrayList<>();
        runtimeDebugLines.add("§b[条件执行] §f当前活动: "
                + (activeRule != null ? "§a" + activeRule.name : "§7无"));

        ConditionalRule highestPriorityRuleInRange = null;
        ConditionalRule highestPriorityFreshEntryRule = null;
        boolean playerIsInRangeOfAnyEnabledRule = false;

        for (ConditionalRule rule : rulesSnapshot) {
            if (rule == null) {
                continue;
            }
            boolean inRange = rule.enabled && rule.isInRange(player.getX(), player.getY(), player.getZ());
            boolean enteredThisTick = inRange && !rule.wasPlayerInRangeLastTick;
            boolean onCooldown = rule.isOnCooldown();

            if (rule.enabled) {
                runtimeDebugLines.add(formatRuleStatus(rule, inRange, enteredThisTick, onCooldown));
            }

            if (inRange) {
                playerIsInRangeOfAnyEnabledRule = true;
                if (highestPriorityRuleInRange == null && !onCooldown) {
                    highestPriorityRuleInRange = rule;
                }
                if (enteredThisTick && highestPriorityFreshEntryRule == null && !onCooldown) {
                    highestPriorityFreshEntryRule = rule;
                }
            } else if (rule.wasPlayerInRangeLastTick) {
                rule.resetTrigger();
            }

            rule.wasPlayerInRangeLastTick = inRange;
        }

        updateDebugLines(runtimeDebugLines);

        if (highestPriorityFreshEntryRule != null && highestPriorityFreshEntryRule.antiStuckEnabled) {
            primeAntiStuckEntryGrace(highestPriorityFreshEntryRule);
        }

        if (activeRule != null) {
            if (!activeRule.enabled) {
                stopActiveSequence("条件执行", "当前激活规则已被关闭");
                activeRule = null;
            } else if (!PathSequenceEventListener.instance.isTracking()
                    && !activeRule.isInRange(player.getX(), player.getY(), player.getZ())) {
                stopActiveSequence("条件执行", "当前激活规则已离开区域且前台序列未运行");
                activeRule = null;
            }
        }

        boolean isSequenceActive = PathSequenceEventListener.instance.isTracking() && activeRule != null;

        if (highestPriorityRuleInRange == null && activeRule == null) {
            updateAntiStuckState(false, null, player);
            return;
        }

        if (isSequenceActive && activeRule.stopOnExit && !activeRule.isInRange(player.getX(), player.getY(), player.getZ())) {
            zszlScriptMod.LOGGER.info("[条件执行] 玩家离开规则 '{}' 的区域，终止序列。", activeRule.name);
            stopActiveSequence("条件执行",
                    "玩家离开规则 '" + describeRule(activeRule) + "' 的区域，stopOnExit=true");
            activeRule.startCooldown();
            activeRule = null;
            clearAntiStuckState();
            return;
        }

        if (highestPriorityRuleInRange != null) {
            if (!isSequenceActive || (activeRule != null
                    && getRulePriorityIndex(rulesSnapshot, highestPriorityRuleInRange) < getRulePriorityIndex(
                            rulesSnapshot, activeRule))) {
                if (isSequenceActive) {
                    zszlScriptMod.LOGGER.info("[条件执行] 发现更高优先级的规则 '{}'，停止当前规则 '{}'.",
                            highestPriorityRuleInRange.name, activeRule.name);
                    stopActiveSequence("条件执行",
                            "更高优先级规则 '" + describeRule(highestPriorityRuleInRange)
                                    + "' 抢占当前规则 '" + describeRule(activeRule) + "'");
                }

                ConditionalRule candidateRule = highestPriorityFreshEntryRule != null
                        ? highestPriorityFreshEntryRule
                        : highestPriorityRuleInRange;
                if (candidateRule.runOncePerEntry && candidateRule.hasBeenTriggered) {
                    return;
                }

                zszlScriptMod.LOGGER.info("[条件执行] 玩家进入规则 '{}' 的区域，开始执行序列: {}",
                        candidateRule.name, candidateRule.sequenceName);
                activeRule = candidateRule;
                activeRule.hasBeenTriggered = true;
                runRuleSequence(activeRule, false);
            }
        }

        if (!playerIsInRangeOfAnyEnabledRule) {
            for (ConditionalRule rule : rulesSnapshot) {
                rule.resetTrigger();
            }
            if (!PathSequenceEventListener.instance.isTracking()) {
                activeRule = null;
            }
        }

        updateAntiStuckState(PathSequenceEventListener.instance.isTracking() && activeRule != null,
                highestPriorityRuleInRange, player);
    }

    private static List<ConditionalRule> snapshotRules() {
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(rules);
    }

    private static int getRulePriorityIndex(List<ConditionalRule> snapshot, ConditionalRule rule) {
        int index = snapshot.indexOf(rule);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private void stopActiveSequence(String source, String reason) {
        if (PathSequenceEventListener.instance.isTracking()) {
            PathSequenceEventListener.instance.stopTrackingAndStopNavigation(source, reason);
        }
        GuiInventory.isLooping = false;
    }

    private void stopRuntimeActivity(String reason) {
        if (activeRule != null) {
            stopActiveSequence("条件执行", reason);
        }
        activeRule = null;
        clearAntiStuckState();
    }

    private void primeAntiStuckEntryGrace(ConditionalRule rule) {
        if (rule == null) {
            return;
        }
        antiStuckEntryGraceRule = rule;
        antiStuckEntryGraceTicks = Math.max(1, rule.getAntiStuckTimeoutTicks());
        resetAntiStuckTracking(rule);
    }

    private void updateAntiStuckState(boolean isSequenceActive, ConditionalRule highestPriorityRuleInRange,
            LocalPlayer player) {
        if (player == null) {
            clearAntiStuckState();
            return;
        }

        if (antiStuckEntryGraceRule != null) {
            if (antiStuckEntryGraceTicks <= 0
                    || !antiStuckEntryGraceRule.enabled
                    || !antiStuckEntryGraceRule.antiStuckEnabled
                    || !antiStuckEntryGraceRule.isInRange(player.getX(), player.getY(), player.getZ())) {
                antiStuckEntryGraceRule = null;
                antiStuckEntryGraceTicks = 0;
            } else {
                antiStuckEntryGraceTicks--;
            }
        }

        ConditionalRule monitorRule = null;
        if (activeRule != null
                && activeRule.enabled
                && activeRule.antiStuckEnabled
                && activeRule.isInRange(player.getX(), player.getY(), player.getZ())
                && (isSequenceActive || (antiStuckEntryGraceRule == activeRule && antiStuckEntryGraceTicks > 0))) {
            monitorRule = activeRule;
        }
        if (monitorRule == null
                && antiStuckEntryGraceRule != null
                && antiStuckEntryGraceTicks > 0
                && antiStuckEntryGraceRule.enabled
                && antiStuckEntryGraceRule.antiStuckEnabled
                && antiStuckEntryGraceRule.isInRange(player.getX(), player.getY(), player.getZ())) {
            monitorRule = antiStuckEntryGraceRule;
        }
        if (monitorRule == null
                && highestPriorityRuleInRange != null
                && highestPriorityRuleInRange == activeRule
                && highestPriorityRuleInRange.antiStuckEnabled
                && highestPriorityRuleInRange.isInRange(player.getX(), player.getY(), player.getZ())
                && isSequenceActive) {
            monitorRule = highestPriorityRuleInRange;
        }

        if (monitorRule == null) {
            clearAntiStuckState();
            return;
        }

        if (antiStuckMonitorRule != monitorRule) {
            resetAntiStuckTracking(monitorRule);
            return;
        }

        if (Double.isNaN(antiStuckLastPosX) || hasPlayerMoved(player)) {
            antiStuckStationaryTicks = 0;
            antiStuckWarnSent = false;
            captureAntiStuckPosition(player);
            return;
        }

        antiStuckStationaryTicks++;
        captureAntiStuckPosition(player);

        int timeoutTicks = Math.max(20, monitorRule.getAntiStuckTimeoutTicks());
        int warningTicks = timeoutTicks - 20;
        if (warningTicks >= 20 && !antiStuckWarnSent && antiStuckStationaryTicks >= warningTicks) {
            sendAntiStuckWarning(monitorRule, warningTicks / 20, 1);
            antiStuckWarnSent = true;
        }
        if (antiStuckStationaryTicks < timeoutTicks) {
            return;
        }

        sendAntiStuckRestartMessage(monitorRule, timeoutTicks / 20);
        restartRuleSequence(monitorRule);
        primeAntiStuckEntryGrace(monitorRule);
    }

    private void restartRuleSequence(ConditionalRule rule) {
        if (rule == null || MC.player == null) {
            return;
        }
        String sequenceName = rule.sequenceName == null ? "" : rule.sequenceName.trim();
        if (sequenceName.isEmpty()) {
            return;
        }
        if (!PathSequenceManager.hasSequence(sequenceName)) {
            MC.player.displayClientMessage(new TextComponentString("§c[条件执行] 未找到序列: " + sequenceName), false);
            return;
        }

        runOnClientThread(() -> {
            stopActiveSequence("条件执行",
                    "条件执行防卡死重启规则 '" + describeRule(rule) + "' 前清理当前前台序列");
            activeRule = rule;
            activeRule.hasBeenTriggered = true;
            runRuleSequence(activeRule, true);
        });
    }

    private void runRuleSequence(ConditionalRule rule, boolean forceSingleExecution) {
        if (rule == null) {
            return;
        }
        String sequenceName = rule.sequenceName == null ? "" : rule.sequenceName.trim();
        if (sequenceName.isEmpty()) {
            return;
        }
        runOnClientThread(() -> {
            if (MC.player == null || MC.level == null) {
                return;
            }
            if (forceSingleExecution) {
                PathSequenceManager.runPathSequenceOnce(sequenceName);
                return;
            }

            int explicitLoopCount = rule.loopCount;
            if (explicitLoopCount == 0) {
                return;
            }
            PathSequenceManager.runPathSequenceWithLoopCount(sequenceName, explicitLoopCount);
        });
    }

    private void runOnClientThread(Runnable task) {
        if (task == null) {
            return;
        }
        if (MC.isSameThread()) {
            task.run();
            return;
        }
        MC.execute(task);
    }

    private static ForegroundSequenceStartDecision allowStartDecision() {
        return new ForegroundSequenceStartDecision(true, "", "");
    }

    private static ForegroundSequenceStartDecision blockedStartDecision(String source, String reason) {
        return new ForegroundSequenceStartDecision(false, source, reason);
    }

    private static String describeRule(ConditionalRule rule) {
        if (rule == null || rule.name == null || rule.name.trim().isEmpty()) {
            return "<未命名规则>";
        }
        return rule.name.trim();
    }

    private void sendAntiStuckWarning(ConditionalRule rule, int elapsedSeconds, int remainSeconds) {
        if (MC.player == null || rule == null) {
            return;
        }
        String sequenceName = rule.sequenceName == null ? "" : rule.sequenceName.trim();
        MC.player.displayClientMessage(new TextComponentString(
                "§e[条件执行] 已在" + sequenceName + "序列中停留超过" + Math.max(1, elapsedSeconds)
                        + "s，将在" + Math.max(1, remainSeconds) + "s后重启序列。"), false);
    }

    private void sendAntiStuckRestartMessage(ConditionalRule rule, int elapsedSeconds) {
        if (MC.player == null || rule == null) {
            return;
        }
        String sequenceName = rule.sequenceName == null ? "" : rule.sequenceName.trim();
        MC.player.displayClientMessage(new TextComponentString(
                "§c[条件执行] 已在" + sequenceName + "序列中停留超过" + Math.max(1, elapsedSeconds)
                        + "s，正在重启序列。"), false);
    }

    private boolean hasPlayerMoved(LocalPlayer player) {
        double dx = player.getX() - antiStuckLastPosX;
        double dy = player.getY() - antiStuckLastPosY;
        double dz = player.getZ() - antiStuckLastPosZ;
        return dx * dx + dy * dy + dz * dz > 0.0025D;
    }

    private static void captureAntiStuckPosition(LocalPlayer player) {
        antiStuckLastPosX = player.getX();
        antiStuckLastPosY = player.getY();
        antiStuckLastPosZ = player.getZ();
    }

    private static void resetAntiStuckTracking(ConditionalRule rule) {
        antiStuckMonitorRule = rule;
        antiStuckStationaryTicks = 0;
        antiStuckWarnSent = false;
        if (MC.player != null) {
            captureAntiStuckPosition(MC.player);
        } else {
            antiStuckLastPosX = Double.NaN;
            antiStuckLastPosY = Double.NaN;
            antiStuckLastPosZ = Double.NaN;
        }
    }

    private static void clearAntiStuckState() {
        antiStuckMonitorRule = null;
        antiStuckStationaryTicks = 0;
        antiStuckWarnSent = false;
        antiStuckLastPosX = Double.NaN;
        antiStuckLastPosY = Double.NaN;
        antiStuckLastPosZ = Double.NaN;
        antiStuckEntryGraceRule = null;
        antiStuckEntryGraceTicks = 0;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (MC.level == null || MC.player == null || !globalEnabled) {
            return;
        }

        List<ConditionalRule> rulesSnapshot = snapshotRules();
        if (rulesSnapshot.isEmpty()) {
            return;
        }

        RenderWorldLastEvent.WorldRenderContext renderContext = event.getWorldRenderContext();
        if (renderContext == null) {
            return;
        }
        PoseStack poseStack = event.createWorldPoseStack();
        if (poseStack == null) {
            return;
        }
        for (ConditionalRule rule : rulesSnapshot) {
            if (rule == null || !rule.visualizeRange || rule.range <= 0.05D) {
                continue;
            }
            drawRuleRange(rule, renderContext, poseStack);
        }
    }

    private void drawRuleRange(ConditionalRule rule, RenderWorldLastEvent.WorldRenderContext renderContext, PoseStack poseStack) {
        int rgb = rule.getVisualizeBorderColorRgb();
        Color color = new Color(rgb);
        WorldGizmoRenderer.verticalRingWall(rule.centerX, rule.centerY + 0.05D, rule.centerZ, rule.range,
                0.0D, 1.5D, color, 0.72F, 2.0F, true);
    }
}
