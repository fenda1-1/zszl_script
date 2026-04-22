package com.zszl.zszlScriptMod.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AutoEatHandler {

    public static boolean autoEatEnabled = false;
    public static int foodLevelThreshold = 12;
    public static boolean autoMoveFoodEnabled = true;
    public static boolean eatWithLookDown = false;
    public static int targetHotbarSlot = 9;
    public static final List<String> DEFAULT_FOOD_KEYWORDS = Arrays.asList("牛排", "面包", "苹果", "曲奇饼");
    public static List<String> foodKeywords = new ArrayList<>(DEFAULT_FOOD_KEYWORDS);

    public static boolean isEating = false;
    public static int originalHotbarSlot = -1;
    public static ItemStack swappedItem = ItemStack.EMPTY;

    private static int eatTimeoutTicks = 0;
    private static int eatStartFoodLevel = 20;
    private static boolean pitchAdjusted = false;
    private static float originalPitch = 0.0F;

    private AutoEatHandler() {
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_autoeat.json").toFile();
    }

    public static void loadAutoEatConfig() {
        try {
            File configFile = getConfigFile();
            if (configFile.exists()) {
                JsonObject json = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
                autoEatEnabled = json.has("enabled") && json.get("enabled").getAsBoolean();
                foodLevelThreshold = json.has("foodLevelThreshold") ? json.get("foodLevelThreshold").getAsInt() : 12;
                autoMoveFoodEnabled = !json.has("autoMoveFoodEnabled")
                        || json.get("autoMoveFoodEnabled").getAsBoolean();
                eatWithLookDown = json.has("eatWithLookDown") && json.get("eatWithLookDown").getAsBoolean();
                targetHotbarSlot = json.has("targetHotbarSlot") ? json.get("targetHotbarSlot").getAsInt() : 9;
                foodKeywords = new ArrayList<>();
                if (json.has("foodKeywords") && json.get("foodKeywords").isJsonArray()) {
                    json.getAsJsonArray("foodKeywords").forEach(element -> {
                        if (element != null && element.isJsonPrimitive()) {
                            String keyword = element.getAsString().trim();
                            if (!keyword.isEmpty()) {
                                foodKeywords.add(keyword);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载自动进食配置失败", e);
        }
        normalizeConfigValues();
    }

    public static void saveAutoEatConfig() {
        try {
            File configFile = getConfigFile();
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject json = new JsonObject();
            json.addProperty("enabled", autoEatEnabled);
            json.addProperty("foodLevelThreshold", foodLevelThreshold);
            json.addProperty("autoMoveFoodEnabled", autoMoveFoodEnabled);
            json.addProperty("eatWithLookDown", eatWithLookDown);
            json.addProperty("targetHotbarSlot", targetHotbarSlot);
            com.google.gson.JsonArray keywords = new com.google.gson.JsonArray();
            for (String keyword : foodKeywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    keywords.add(keyword.trim());
                }
            }
            json.add("foodKeywords", keywords);
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(json.toString());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动进食配置失败", e);
        }
    }

    public static void checkAutoEat(LocalPlayer player) {
        if (player == null || !autoEatEnabled) {
            return;
        }
        if (isEating) {
            continueEating(player);
            return;
        }
        if (ModConfig.isGuiOpen() || player.getFoodData().getFoodLevel() > foodLevelThreshold) {
            return;
        }

        int hotbarFoodSlot = findBestFoodInHotbar(player);
        if (hotbarFoodSlot < 0) {
            return;
        }

        originalHotbarSlot = player.getInventory().selected;
        eatStartFoodLevel = player.getFoodData().getFoodLevel();
        eatTimeoutTicks = 80;
        isEating = true;
        pitchAdjusted = false;

        EmbeddedNavigationHandler.INSTANCE.pause("自动进食开始时暂停当前导航");

        if (eatWithLookDown) {
            originalPitch = player.getXRot();
            player.setXRot(80.0F);
            pitchAdjusted = true;
        }

        player.getInventory().selected = hotbarFoodSlot;
        useCurrentFood(player);

        if (ModConfig.isDebugFlagEnabled(DebugModule.AUTO_EAT)) {
            ItemStack stack = player.getInventory().getItem(hotbarFoodSlot);
            player.sendSystemMessage(new TextComponentString("§d[调试] §7开始自动进食 [" + stack.getHoverName().getString() + "]。"));
        }
    }

    private static void continueEating(LocalPlayer player) {
        if (player == null) {
            resetEatingState();
            EmbeddedNavigationHandler.INSTANCE.resume("自动进食因玩家状态缺失而恢复导航");
            return;
        }
        if (ModConfig.isGuiOpen()) {
            finalizeEating(player, true);
            return;
        }

        if (!player.isUsingItem()) {
            useCurrentFood(player);
        }

        eatTimeoutTicks--;
        boolean hungerRecovered = player.getFoodData().getFoodLevel() > eatStartFoodLevel;
        boolean timedOut = eatTimeoutTicks <= 0;
        if (hungerRecovered || timedOut) {
            finalizeEating(player, false);
        }
    }

    private static void useCurrentFood(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private static void finalizeEating(LocalPlayer player, boolean interrupted) {
        if (player != null && player.isUsingItem()) {
            player.stopUsingItem();
        }
        if (player != null && pitchAdjusted) {
            player.setXRot(originalPitch);
        }
        if (player != null && originalHotbarSlot >= 0) {
            player.getInventory().selected = originalHotbarSlot;
        }

        EmbeddedNavigationHandler.INSTANCE.resume(interrupted ? "自动进食被打断后恢复导航" : "自动进食完成后恢复导航");
        resetEatingState();

        if (interrupted && player != null && ModConfig.isDebugFlagEnabled(DebugModule.AUTO_EAT)) {
            player.sendSystemMessage(new TextComponentString("§d[调试] §7自动进食被中断。"));
        }
    }

    private static int findBestFoodInHotbar(LocalPlayer player) {
        int bestSlot = -1;
        int bestPriority = Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isPreferredFoodItem(stack)) {
                continue;
            }
            int priority = getFoodPriority(stack);
            if (priority < bestPriority) {
                bestPriority = priority;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isPreferredFoodItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isEdible() && isMatchingFoodKeyword(stack);
    }

    private static boolean isMatchingFoodKeyword(ItemStack stack) {
        if (foodKeywords == null || foodKeywords.isEmpty()) {
            return true;
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        for (String keyword : foodKeywords) {
            if (keyword == null) {
                continue;
            }
            String trimmed = keyword.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty() && name.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private static int getFoodPriority(ItemStack stack) {
        if (foodKeywords == null || foodKeywords.isEmpty()) {
            return 0;
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        for (int i = 0; i < foodKeywords.size(); i++) {
            String keyword = foodKeywords.get(i);
            if (keyword == null) {
                continue;
            }
            String trimmed = keyword.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty() && name.contains(trimmed)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static void normalizeConfigValues() {
        foodLevelThreshold = Math.max(0, Math.min(20, foodLevelThreshold));
        targetHotbarSlot = Math.max(1, Math.min(9, targetHotbarSlot));
        if (foodKeywords == null || foodKeywords.isEmpty()) {
            foodKeywords = new ArrayList<>(DEFAULT_FOOD_KEYWORDS);
        }
    }

    private static void resetEatingState() {
        isEating = false;
        originalHotbarSlot = -1;
        swappedItem = ItemStack.EMPTY;
        eatTimeoutTicks = 0;
        eatStartFoodLevel = 20;
        pitchAdjusted = false;
    }
}

