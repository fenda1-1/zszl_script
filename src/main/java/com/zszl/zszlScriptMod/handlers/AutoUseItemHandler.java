package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.system.AutoUseItemRule;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoUseItemHandler {

    public static final AutoUseItemHandler INSTANCE = new AutoUseItemHandler();

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> categories = new CopyOnWriteArrayList<>();
    private static final String CATEGORY_DEFAULT = "默认";

    public static boolean globalEnabled = false;
    public static final List<AutoUseItemRule> rules = new CopyOnWriteArrayList<>();

    private long nextCheckAtMs = 0L;

    private AutoUseItemHandler() {
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
                Type listType = new TypeToken<ArrayList<AutoUseItemRule>>() {
                }.getType();
                List<AutoUseItemRule> loaded = GSON.fromJson(root.get("rules"), listType);
                if (loaded != null) {
                    for (AutoUseItemRule rule : loaded) {
                        if (rule == null) {
                            continue;
                        }
                        normalizeRule(rule);
                        rules.add(rule);
                    }
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法加载静默使用物品配置", e);
            rules.clear();
        }

        ensureCategoriesSynced();
        INSTANCE.resetSchedule();
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
            zszlScriptMod.LOGGER.error("无法保存静默使用物品配置", e);
        }
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        ensureCategoriesSynced();
        return new ArrayList<>(categories);
    }

    public static synchronized boolean addCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        if (categories.contains(normalized)) {
            return false;
        }
        categories.add(normalized);
        saveConfig();
        return true;
    }

    public static synchronized boolean renameCategory(String oldCategory, String newCategory) {
        String normalizedOld = normalizeCategory(oldCategory);
        String normalizedNew = normalizeCategory(newCategory);
        boolean changed = false;
        for (int i = 0; i < categories.size(); i++) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalizedOld)) {
                categories.set(i, normalizedNew);
                changed = true;
            }
        }
        for (AutoUseItemRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalizedOld)) {
                rule.category = normalizedNew;
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
        return changed;
    }

    public static synchronized boolean deleteCategory(String category) {
        String normalized = normalizeCategory(category);
        boolean changed = categories.removeIf(existing -> normalizeCategory(existing).equalsIgnoreCase(normalized));
        for (AutoUseItemRule rule : rules) {
            if (rule != null && normalizeCategory(rule.category).equalsIgnoreCase(normalized)) {
                rule.category = CATEGORY_DEFAULT;
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
        return changed;
    }

    public static synchronized void replaceCategoryOrder(List<String> orderedCategories) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (orderedCategories != null) {
            for (String category : orderedCategories) {
                normalized.add(normalizeCategory(category));
            }
        }
        for (AutoUseItemRule rule : rules) {
            if (rule != null) {
                normalized.add(normalizeCategory(rule.category));
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        categories.clear();
        categories.addAll(normalized);
        saveConfig();
    }

    public void tick() {
        if (!globalEnabled || MC.player == null || MC.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextCheckAtMs) {
            return;
        }
        nextCheckAtMs = now + 50L;

        for (AutoUseItemRule rule : rules) {
            if (rule == null || !rule.enabled || rule.name == null || rule.name.trim().isEmpty()) {
                continue;
            }
            int interval = Math.max(10, rule.intervalMs);
            if (now - rule.lastUseAtMs < interval) {
                continue;
            }
            boolean used = useMatchingHotbarItem(MC.player, rule.name, rule.matchMode, rule.useMode,
                    rule.changeLocalSlot, rule.switchItemDelayTicks, rule.switchDelayTicks, rule.restoreDelayTicks);
            if (used) {
                rule.lastUseAtMs = now;
            }
        }
    }

    public boolean useMatchingHotbarItem(LocalPlayer player, String itemName, AutoUseItemRule.MatchMode matchMode,
            AutoUseItemRule.UseMode useMode) {
        return useMatchingHotbarItem(player, itemName, matchMode, useMode, false, 0, 0, 0);
    }

    public boolean useMatchingHotbarItem(LocalPlayer player, String itemName, AutoUseItemRule.MatchMode matchMode,
            AutoUseItemRule.UseMode useMode, boolean changeLocalSlot, int switchItemDelayTicks, int switchDelayTicks,
            int switchBackDelayTicks) {
        if (player == null || itemName == null || itemName.trim().isEmpty()) {
            return false;
        }

        int matchedHotbarSlot = findMatchedHotbarSlotByName(itemName, matchMode);
        if (matchedHotbarSlot < 0) {
            return false;
        }

        int originalHotbarSlot = player.getInventory().getSelectedSlot();
        Runnable useAction = () -> {
            if (useMode == AutoUseItemRule.UseMode.LEFT_CLICK) {
                try {
                    java.lang.reflect.Method method = Minecraft.class.getDeclaredMethod("startAttack");
                    method.setAccessible(true);
                    method.invoke(MC);
                } catch (Exception ignored) {
                }
            } else {
                ModUtils.useHeldItem(player, 0);
            }
        };

        Runnable switchBack = () -> {
            if (changeLocalSlot && originalHotbarSlot != matchedHotbarSlot) {
                ModUtils.switchToHotbarSlot(originalHotbarSlot + 1);
            }
        };

        Runnable beginUse = () -> {
            if (changeLocalSlot && originalHotbarSlot != matchedHotbarSlot) {
                ModUtils.switchToHotbarSlot(matchedHotbarSlot + 1);
            }
            schedule(() -> {
                useAction.run();
                schedule(switchBack, switchBackDelayTicks);
            }, switchDelayTicks);
        };

        schedule(beginUse, switchItemDelayTicks);
        return true;
    }

    public void resetSchedule() {
        nextCheckAtMs = 0L;
        long now = System.currentTimeMillis();
        for (AutoUseItemRule rule : rules) {
            if (rule != null) {
                rule.lastUseAtMs = Math.min(rule.lastUseAtMs, now);
            }
        }
    }

    public int findMatchedHotbarSlotByName(String itemName, AutoUseItemRule.MatchMode matchMode) {
        if (MC.player == null) {
            return -1;
        }
        String target = normalizeName(itemName);
        if (target.isEmpty()) {
            return -1;
        }

        AutoUseItemRule.MatchMode safeMode = matchMode == null ? AutoUseItemRule.MatchMode.CONTAINS : matchMode;
        for (int slot = 0; slot < 9 && slot < MC.player.getInventory().getNonEquipmentItems().size(); slot++) {
            ItemStack stack = MC.player.getInventory().getNonEquipmentItems().get(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String currentName = normalizeName(stack.getHoverName().getString());
            boolean matched = safeMode == AutoUseItemRule.MatchMode.EXACT
                    ? currentName.equals(target)
                    : currentName.contains(target);
            if (matched) {
                return slot;
            }
        }
        return -1;
    }

    private static void schedule(Runnable runnable, int delayTicks) {
        ModUtils.DelayScheduler.init();
        if (delayTicks <= 0 || ModUtils.DelayScheduler.instance == null) {
            Minecraft.getInstance().execute(runnable);
            return;
        }
        ModUtils.DelayScheduler.instance.schedule(() -> Minecraft.getInstance().execute(runnable),
                Math.max(0, delayTicks));
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_use_item_rules.json");
    }

    private static void normalizeRule(AutoUseItemRule rule) {
        if (rule == null) {
            return;
        }
        rule.category = normalizeCategory(rule.category);
        if (rule.intervalMs <= 0) {
            rule.intervalMs = 250;
        }
        if (rule.matchMode == null) {
            rule.matchMode = AutoUseItemRule.MatchMode.CONTAINS;
        }
        if (rule.useMode == null) {
            rule.useMode = AutoUseItemRule.UseMode.RIGHT_CLICK;
        }
        rule.switchItemDelayTicks = Math.max(0, rule.switchItemDelayTicks);
        rule.switchDelayTicks = Math.max(0, rule.switchDelayTicks);
        rule.restoreDelayTicks = Math.max(0, rule.restoreDelayTicks);
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
        for (AutoUseItemRule rule : rules) {
            if (rule != null) {
                normalizeRule(rule);
                normalized.add(rule.category);
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        categories.clear();
        categories.addAll(normalized);
    }

    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
