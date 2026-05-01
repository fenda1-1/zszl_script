package com.zszl.zszlScriptMod.handlers;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.compat.ItemComponentCompat;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.JsonConfigCharsetCompat;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class AutoEatHandler {

    public static boolean autoEatEnabled = false;
    public static int foodLevelThreshold = 12;
    public static boolean autoMoveFoodEnabled = true;
    public static boolean eatWithLookDown = false;
    public static boolean smoothLookDown = true;
    public static boolean onlyDuringSequenceStepPathing = true;
    public static int targetHotbarSlot = 9;
    public static final List<String> DEFAULT_FOOD_KEYWORDS = Arrays.asList("牛排", "面包", "苹果", "曲奇饼");
    public static List<String> foodKeywords = new ArrayList<>(DEFAULT_FOOD_KEYWORDS);

    public static boolean isEating = false;
    public static int originalHotbarSlot = -1;
    public static ItemStack swappedItem = ItemStack.EMPTY;

    private static int eatTimeoutTicks = 0;
    private static int eatStartFoodLevel = 20;
    private static boolean eatStartedUsingHand = false;
    private static boolean pitchAdjusted = false;
    private static float originalPitch = 0.0F;
    private static int eatRetryAttempts = 0;
    private static int eatActiveHotbarSlot = -1;
    private static int eatStartStackCount = 0;
    private static final int EAT_ATTEMPT_TIMEOUT_TICKS = 100;
    private static final int MAX_EAT_RETRY_ATTEMPTS = 4;
    private static final float EAT_LOOKDOWN_TARGET_PITCH = 80.0F;
    private static final float EAT_LOOKDOWN_MIN_STEP = 3.0F;
    private static final float EAT_LOOKDOWN_MAX_STEP = 14.0F;

    private AutoEatHandler() {
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_autoeat.json").toFile();
    }

    public static void loadAutoEatConfig() {
        boolean needsRewrite = false;
        try {
            File configFile = getConfigFile();
            if (configFile.exists()) {
                JsonConfigCharsetCompat.ReadResult readResult = JsonConfigCharsetCompat
                        .readJsonObject(configFile.toPath());
                JsonObject json = readResult.getRoot();
                needsRewrite = readResult.usedLegacyCharset();
                autoEatEnabled = json.has("enabled") && json.get("enabled").getAsBoolean();
                foodLevelThreshold = json.has("foodLevelThreshold") ? json.get("foodLevelThreshold").getAsInt() : 12;
                autoMoveFoodEnabled = !json.has("autoMoveFoodEnabled")
                        || json.get("autoMoveFoodEnabled").getAsBoolean();
                eatWithLookDown = json.has("eatWithLookDown") && json.get("eatWithLookDown").getAsBoolean();
                smoothLookDown = !json.has("smoothLookDown") || json.get("smoothLookDown").getAsBoolean();
                onlyDuringSequenceStepPathing = !json.has("onlyDuringSequenceStepPathing")
                        || json.get("onlyDuringSequenceStepPathing").getAsBoolean();
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

                if (shouldResetToDefaultKeywords(foodKeywords)) {
                    foodKeywords = new ArrayList<>(DEFAULT_FOOD_KEYWORDS);
                    needsRewrite = true;
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载自动进食配置失败", e);
        }
        normalizeConfigValues();
        if (needsRewrite) {
            saveAutoEatConfig();
        }
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
            json.addProperty("smoothLookDown", smoothLookDown);
            json.addProperty("onlyDuringSequenceStepPathing", onlyDuringSequenceStepPathing);
            json.addProperty("targetHotbarSlot", targetHotbarSlot);
            com.google.gson.JsonArray keywords = new com.google.gson.JsonArray();
            for (String keyword : foodKeywords) {
                if (keyword != null && !keyword.trim().isEmpty()) {
                    keywords.add(keyword.trim());
                }
            }
            json.add("foodKeywords", keywords);
            JsonConfigCharsetCompat.writeJsonObject(configFile.toPath(), json);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动进食配置失败", e);
        }
    }

    public static void checkAutoEat(LocalPlayer player) {
        if (player == null) {
            if (isEating) {
                resetEatingState();
                EmbeddedNavigationHandler.INSTANCE.resume("自动进食因玩家状态缺失而恢复导航");
            }
            return;
        }
        if (!autoEatEnabled) {
            if (isEating) {
                finalizeEating(player, true);
            }
            return;
        }
        if (isEating) {
            continueEating(player);
            return;
        }
        if (ModConfig.isGuiOpen() || player.getFoodData().getFoodLevel() > foodLevelThreshold) {
            return;
        }
        if (onlyDuringSequenceStepPathing && !PathSequenceEventListener.instance.isAutoEatStepPathingActive()) {
            return;
        }

        int hotbarFoodSlot = findBestFoodInHotbar(player);
        if (hotbarFoodSlot < 0) {
            return;
        }

        originalHotbarSlot = player.getInventory().getSelectedSlot();
        eatStartFoodLevel = player.getFoodData().getFoodLevel();
        eatTimeoutTicks = EAT_ATTEMPT_TIMEOUT_TICKS;
        eatStartedUsingHand = false;
        eatRetryAttempts = 0;
        eatActiveHotbarSlot = hotbarFoodSlot;
        ItemStack startingStack = player.getInventory().getItem(hotbarFoodSlot);
        eatStartStackCount = startingStack.isEmpty() ? 0 : startingStack.getCount();
        isEating = true;
        pitchAdjusted = false;

        EmbeddedNavigationHandler.INSTANCE.pause("自动进食开始时暂停当前导航");

        if (eatWithLookDown) {
            originalPitch = player.getXRot();
            pitchAdjusted = true;
            applyLookDownPitch(player);
        }

        ModUtils.switchToHotbarSlot(hotbarFoodSlot + 1);
        tryStartEatingUse(player);

        if (ModConfig.isDebugFlagEnabled(DebugModule.AUTO_EAT)) {
            ItemStack stack = player.getInventory().getItem(hotbarFoodSlot);
            player.displayClientMessage(new TextComponentString("§d[调试] §7开始自动进食 [" + stack.getHoverName().getString() + "]。"), false);
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

        if (eatActiveHotbarSlot >= 0 && player.getInventory().getSelectedSlot() != eatActiveHotbarSlot) {
            ModUtils.switchToHotbarSlot(eatActiveHotbarSlot + 1);
        }

        applyLookDownPitch(player);
        setUseKeyState(true);
        tryStartEatingUse(player);
        if (player.isUsingItem()) {
            eatStartedUsingHand = true;
        }

        eatTimeoutTicks--;
        boolean hungerRecovered = player.getFoodData().getFoodLevel() > eatStartFoodLevel;
        boolean stackConsumed = hasConsumedFoodStack(player);
        boolean finishedUseWithoutSuccess = eatStartedUsingHand && !player.isUsingItem() && !hungerRecovered
                && !stackConsumed;
        boolean timedOut = eatTimeoutTicks <= 0 && !hungerRecovered && !stackConsumed;
        if (hungerRecovered || stackConsumed) {
            finalizeEating(player, false);
        } else if (finishedUseWithoutSuccess || timedOut) {
            retryEatingUse(player);
        }
    }

    private static void tryStartEatingUse(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.gameMode == null) {
            return;
        }
        if (!player.isUsingItem()) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
        setUseKeyState(true);
    }

    private static void finalizeEating(LocalPlayer player, boolean interrupted) {
        if (player != null && player.isUsingItem()) {
            player.stopUsingItem();
        }
        if (player != null && pitchAdjusted) {
            player.setXRot(originalPitch);
        }
        setUseKeyState(false);
        if (player != null && originalHotbarSlot >= 0) {
            ModUtils.switchToHotbarSlot(originalHotbarSlot + 1);
        }

        EmbeddedNavigationHandler.INSTANCE.resume(interrupted ? "自动进食被打断后恢复导航" : "自动进食完成后恢复导航");
        resetEatingState();

        if (interrupted && player != null && ModConfig.isDebugFlagEnabled(DebugModule.AUTO_EAT)) {
            player.displayClientMessage(new TextComponentString("§d[调试] §7自动进食被中断。"), false);
        }
    }

    private static void setUseKeyState(boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.options.keyUse == null) {
            return;
        }
        InputConstants.Key key = mc.options.keyUse.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return;
        }
        KeyMapping.set(key, pressed || isPhysicalUseKeyDown(mc, key));
    }

    private static boolean isPhysicalUseKeyDown(Minecraft mc, InputConstants.Key key) {
        if (mc == null || mc.getWindow() == null || key == null || key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(mc.getWindow(), key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse.isButtonDown(key.getValue());
        }
        return false;
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
        return ItemComponentCompat.isEdible(stack) && isMatchingFoodKeyword(stack);
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

    private static boolean shouldResetToDefaultKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (!JsonConfigCharsetCompat.looksLikeMojibake(keyword)) {
                return false;
            }
        }
        return true;
    }

    private static void resetEatingState() {
        setUseKeyState(false);
        isEating = false;
        originalHotbarSlot = -1;
        swappedItem = ItemStack.EMPTY;
        eatTimeoutTicks = 0;
        eatStartFoodLevel = 20;
        eatStartedUsingHand = false;
        pitchAdjusted = false;
        eatRetryAttempts = 0;
        eatActiveHotbarSlot = -1;
        eatStartStackCount = 0;
    }

    private static boolean hasConsumedFoodStack(LocalPlayer player) {
        if (player == null || eatActiveHotbarSlot < 0 || eatActiveHotbarSlot > 8) {
            return false;
        }
        ItemStack current = player.getInventory().getItem(eatActiveHotbarSlot);
        if (current == null || current.isEmpty()) {
            return eatStartedUsingHand;
        }
        return current.getCount() < eatStartStackCount;
    }

    private static void retryEatingUse(LocalPlayer player) {
        if (player == null) {
            finalizeEating(null, true);
            return;
        }
        if (eatRetryAttempts >= MAX_EAT_RETRY_ATTEMPTS) {
            finalizeEating(player, true);
            return;
        }
        eatRetryAttempts++;
        eatTimeoutTicks = EAT_ATTEMPT_TIMEOUT_TICKS;
        eatStartedUsingHand = false;
        ItemStack current = eatActiveHotbarSlot >= 0 && eatActiveHotbarSlot <= 8
                ? player.getInventory().getItem(eatActiveHotbarSlot)
                : ItemStack.EMPTY;
        eatStartStackCount = current.isEmpty() ? 0 : current.getCount();
        tryStartEatingUse(player);
    }

    private static void applyLookDownPitch(LocalPlayer player) {
        if (player == null || !eatWithLookDown || !pitchAdjusted) {
            return;
        }
        if (!smoothLookDown) {
            player.setXRot(EAT_LOOKDOWN_TARGET_PITCH);
            return;
        }
        float delta = EAT_LOOKDOWN_TARGET_PITCH - player.getXRot();
        float absDelta = Math.abs(delta);
        if (absDelta <= 0.001F) {
            player.setXRot(EAT_LOOKDOWN_TARGET_PITCH);
            return;
        }
        float stepLimit = Math.min(EAT_LOOKDOWN_MAX_STEP,
                Math.max(EAT_LOOKDOWN_MIN_STEP, EAT_LOOKDOWN_MIN_STEP + absDelta * 0.35F));
        float step = clampSigned(delta, stepLimit);
        float gcdStep = getMouseGcdStep();
        step = quantizeRotationStepForGcd(step, stepLimit, gcdStep);
        if (Math.abs(step) <= 0.001F) {
            step = Math.copySign(Math.min(stepLimit, absDelta), delta);
        }
        player.setXRot(clampPitch(player.getXRot() + step));
    }

    private static float clampSigned(float value, float limit) {
        float safeLimit = Math.max(0.0F, Math.abs(limit));
        return Math.copySign(Math.min(Math.abs(value), safeLimit), value);
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    private static float getMouseGcdStep() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return 0.0F;
        }
        float sensitivity = (float) Math.max(0.0D, Math.min(1.0D, mc.options.sensitivity().get()));
        float f = sensitivity * 0.6F + 0.2F;
        return f * f * f * 8.0F * 0.15F;
    }

    private static float quantizeRotationStepForGcd(float step, float maxMagnitude, float gcdStep) {
        if (step == 0.0F || gcdStep <= 1.0E-5F || maxMagnitude <= 0.0F) {
            return step == 0.0F ? 0.0F : clampSigned(step, maxMagnitude);
        }
        float maxStep = Math.abs(maxMagnitude);
        float absStep = Math.min(Math.abs(step), maxStep);
        if (absStep <= 1.0E-5F) {
            return 0.0F;
        }
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
}

