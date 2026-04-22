package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.path.InventoryItemFilterExpressionEngine;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.player.LocalPlayer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class ItemFilterHandler {

    public static final class MoveChestFilterRule {
        private final String itemName;
        private final List<String> requiredNbtTags;

        private MoveChestFilterRule(String itemName, List<String> requiredNbtTags) {
            this.itemName = itemName == null ? "" : itemName.trim();
            this.requiredNbtTags = requiredNbtTags == null ? new ArrayList<String>() : new ArrayList<String>(requiredNbtTags);
        }

        public String getItemName() {
            return itemName;
        }

        public List<String> getRequiredNbtTags() {
            return new ArrayList<String>(requiredNbtTags);
        }

        private boolean isEmpty() {
            return itemName.isEmpty() && requiredNbtTags.isEmpty();
        }
    }

    public static final File FILTER_CONFIG_FILE = new File(ModConfig.CONFIG_DIR, "filter_config.json");
    public static final String NBT_TAG_MATCH_MODE_CONTAINS = "CONTAINS";
    public static final String NBT_TAG_MATCH_MODE_NOT_CONTAINS = "NOT_CONTAINS";
    public static final String MOVE_DIRECTION_INVENTORY_TO_CHEST = "INVENTORY_TO_CHEST";
    public static final String MOVE_DIRECTION_CHEST_TO_INVENTORY = "CHEST_TO_INVENTORY";

    public static List<String> blacklistFilters = new ArrayList<>();
    public static List<String> whitelistFilters = new ArrayList<>();

    private static volatile boolean warehouseTransferInProgress = false;

    private ItemFilterHandler() {
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("filter_config.json").toFile();
    }

    public static void saveFilterConfig() {
        try {
            File configFile = getConfigFile();
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject json = new JsonObject();
            json.add("blacklist", new Gson().toJsonTree(blacklistFilters));
            json.add("whitelist", new Gson().toJsonTree(whitelistFilters));
            Files.write(configFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存过滤配置失败", e);
        }
    }

    public static void loadFilterConfig() {
        blacklistFilters = new ArrayList<>();
        whitelistFilters = new ArrayList<>();
        try {
            File configFile = getConfigFile();
            if (!configFile.exists()) {
                return;
            }
            JsonObject json = JsonParser.parseString(Files.readString(configFile.toPath(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (json.has("blacklist") && json.get("blacklist").isJsonArray()) {
                for (var element : json.getAsJsonArray("blacklist")) {
                    blacklistFilters.add(element.getAsString());
                }
            }
            if (json.has("whitelist") && json.get("whitelist").isJsonArray()) {
                for (var element : json.getAsJsonArray("whitelist")) {
                    whitelistFilters.add(element.getAsString());
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载过滤配置失败", e);
        }
    }

    public static void dropItemsByNameFilter() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu == null ? player.inventoryMenu : player.containerMenu;
        if (menu == null) {
            return;
        }

        List<Integer> menuSlotsToDrop = new ArrayList<>();
        for (int inventoryIndex = 0; inventoryIndex < player.getInventory().items.size(); inventoryIndex++) {
            ItemStack stack = player.getInventory().items.get(inventoryIndex);
            if (stack == null || stack.isEmpty() || !shouldDropByNameFilter(stack)) {
                continue;
            }
            int menuSlotIndex = findInventoryMenuSlotIndex(menu, player, inventoryIndex);
            if (menuSlotIndex >= 0) {
                menuSlotsToDrop.add(menuSlotIndex);
            }
        }

        for (Integer menuSlotIndex : menuSlotsToDrop) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, menuSlotIndex, 1, ClickType.THROW, player);
        }
    }

    public static void transferItemsToWarehouse() {
        WarehouseEventHandler.startAutoDepositByHighlights();
        warehouseTransferInProgress = WarehouseEventHandler.isAutoDepositRouteRunning();
    }

    public static boolean isWarehouseTransferInProgress() {
        return warehouseTransferInProgress || WarehouseEventHandler.isAutoDepositRouteRunning();
    }

    public static void moveInventoryItemsToChestSlots(JsonObject params) {
        warehouseTransferInProgress = false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || player.containerMenu == null) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        int expectedContainerId = menu.containerId;
        String moveDirection = readMoveChestDirection(params);
        boolean chestToInventory = MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(moveDirection);
        int delayTicks = params != null && params.has("delayTicks")
                ? Math.max(0, params.get("delayTicks").getAsInt())
                : 2;
        List<String> expressions = InventoryItemFilterExpressionEngine.readExpressions(params);
        List<MoveChestFilterRule> legacyRules = expressions.isEmpty()
                ? readMoveChestFilterRules(params)
                : new ArrayList<MoveChestFilterRule>();
        if (expressions.isEmpty() && legacyRules.isEmpty()) {
            return;
        }

        int chestContainerSlotCount = resolveChestContainerSlotCount(menu, player);
        List<Integer> selectedChestSlots = readSlotSelection(params, "chestSlots", "chestSlotsText",
                chestContainerSlotCount);
        List<Integer> selectedInventorySlots = readSlotSelection(params, "inventorySlots", "inventorySlotsText",
                player.getInventory().items.size());
        boolean explicitChestTargetSelection = hasExplicitSlotSelection(params, "chestSlots", "chestSlotsText");
        boolean explicitInventoryTargetSelection = hasExplicitSlotSelection(params, "inventorySlots", "inventorySlotsText");

        List<Integer> sourceMenuSlots = chestToInventory
                ? buildChestMenuSlotList(chestContainerSlotCount, selectedChestSlots)
                : buildInventoryMenuSlotList(menu, player, selectedInventorySlots);
        List<Integer> targetMenuSlots = chestToInventory
                ? buildInventoryMenuSlotList(menu, player, selectedInventorySlots)
                : buildChestMenuSlotList(chestContainerSlotCount, selectedChestSlots);
        boolean explicitTargetSelection = chestToInventory ? explicitInventoryTargetSelection : explicitChestTargetSelection;

        ModUtils.DelayScheduler.init();
        if (ModUtils.DelayScheduler.instance != null) {
            ModUtils.DelayScheduler.instance.cancelTasks(task -> "move_chest_batch".equals(task.getTag()));
        }

        int scheduledCount = 0;
        int scheduleDelay = 0;
        for (Integer sourceMenuSlot : sourceMenuSlots) {
            if (sourceMenuSlot == null) {
                continue;
            }
            final int finalSourceMenuSlot = sourceMenuSlot;
            final int finalVisibleSourceSlotIndex = chestToInventory
                    ? finalSourceMenuSlot
                    : resolveInventoryIndexByMenuSlot(menu, player, finalSourceMenuSlot);
            if (finalVisibleSourceSlotIndex < 0) {
                continue;
            }
            final List<Integer> finalTargetMenuSlots = new ArrayList<>(targetMenuSlots);
            final boolean finalExplicitTargetSelection = explicitTargetSelection;
            ModUtils.DelayScheduler.instance.schedule(() -> moveSingleMatchingStack(expectedContainerId,
                    finalSourceMenuSlot, finalVisibleSourceSlotIndex, expressions, legacyRules,
                    finalTargetMenuSlots, finalExplicitTargetSelection, chestToInventory), scheduleDelay,
                    "move_chest_batch");
            scheduledCount++;
            scheduleDelay += Math.max(1, delayTicks);
        }

        if (scheduledCount <= 0) {
            warehouseTransferInProgress = false;
            return;
        }

        warehouseTransferInProgress = true;
        ModUtils.DelayScheduler.instance.schedule(() -> warehouseTransferInProgress = false, scheduleDelay + 1,
                "move_chest_batch");
    }

    public static String buildItemSearchableText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String name = stack.getHoverName().getString();
        CompoundTag tag = stack.getTag();
        return (name == null ? "" : name) + " " + (tag == null ? "" : tag.toString());
    }

    private static boolean shouldDropByNameFilter(ItemStack stack) {
        String searchable = buildItemSearchableText(stack).toLowerCase(Locale.ROOT);
        boolean whitelistMatch = whitelistFilters == null || whitelistFilters.isEmpty()
                || matchesAnyFilter(searchable, whitelistFilters);
        boolean blacklistMatch = blacklistFilters != null && !blacklistFilters.isEmpty()
                && matchesAnyFilter(searchable, blacklistFilters);

        if (whitelistFilters != null && !whitelistFilters.isEmpty()) {
            return whitelistMatch && !blacklistMatch;
        }
        return blacklistMatch;
    }

    private static boolean matchesAnyFilter(String searchable, List<String> filters) {
        if (searchable == null || searchable.isEmpty() || filters == null || filters.isEmpty()) {
            return false;
        }
        for (String filter : filters) {
            String normalized = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && searchable.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesRequiredNbtTags(ItemStack stack, List<String> requiredNbtTags, String matchMode) {
        if (requiredNbtTags == null || requiredNbtTags.isEmpty()) {
            return true;
        }
        String searchable = buildItemSearchableText(stack).toLowerCase(Locale.ROOT);
        boolean containsMode = !NBT_TAG_MATCH_MODE_NOT_CONTAINS.equalsIgnoreCase(matchMode);
        for (String tag : requiredNbtTags) {
            String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            boolean contains = searchable.contains(normalized);
            if (containsMode && !contains) {
                return false;
            }
            if (!containsMode && contains) {
                return false;
            }
        }
        return true;
    }

    public static List<String> readTagFilters(JsonObject params, String arrayKey, String textKey) {
        List<String> tags = new ArrayList<>();
        if (params == null) {
            return tags;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            JsonArray array = params.getAsJsonArray(arrayKey);
            array.forEach(element -> {
                String text = element.getAsString().trim();
                if (!text.isEmpty()) {
                    tags.add(text);
                }
            });
            return tags;
        }
        if (params.has(textKey)) {
            String raw = params.get(textKey).getAsString();
            for (String part : raw.split("[,\\n]")) {
                String text = part.trim();
                if (!text.isEmpty()) {
                    tags.add(text);
                }
            }
        }
        return tags;
    }

    public static String readRequiredNbtTagMatchMode(JsonObject params) {
        if (params == null || !params.has("requiredNbtTagMatchMode")) {
            return NBT_TAG_MATCH_MODE_CONTAINS;
        }
        return params.get("requiredNbtTagMatchMode").getAsString();
    }

    public static String readMoveChestDirection(JsonObject params) {
        if (params == null || !params.has("moveDirection")) {
            return MOVE_DIRECTION_INVENTORY_TO_CHEST;
        }
        return params.get("moveDirection").getAsString();
    }

    public static String readMoveChestItemName(JsonObject params) {
        if (params == null || !params.has("itemName")) {
            return "";
        }
        try {
            String value = params.get("itemName").getAsString();
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static List<MoveChestFilterRule> readMoveChestFilterRules(JsonObject params) {
        List<MoveChestFilterRule> rules = new ArrayList<>();
        if (params == null) {
            return rules;
        }

        if (params.has("moveChestRules") && params.get("moveChestRules").isJsonArray()) {
            JsonArray array = params.getAsJsonArray("moveChestRules");
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject ruleObject = element.getAsJsonObject();
                String itemName = "";
                if (ruleObject.has("itemName") && ruleObject.get("itemName").isJsonPrimitive()) {
                    itemName = ruleObject.get("itemName").getAsString();
                }
                List<String> requiredNbtTags = readTagFilters(ruleObject, "requiredNbtTags", "requiredNbtTagsText");
                MoveChestFilterRule rule = new MoveChestFilterRule(itemName, requiredNbtTags);
                if (!rule.isEmpty()) {
                    rules.add(rule);
                }
            }
            if (!rules.isEmpty()) {
                return rules;
            }
        }

        String itemName = readMoveChestItemName(params);
        List<String> requiredNbtTags = readTagFilters(params, "requiredNbtTags", "requiredNbtTagsText");
        MoveChestFilterRule legacyRule = new MoveChestFilterRule(itemName, requiredNbtTags);
        if (!legacyRule.isEmpty()) {
            rules.add(legacyRule);
        }
        return rules;
    }

    private static void moveSingleMatchingStack(int expectedContainerId, int sourceMenuSlot,
            int visibleSourceSlotIndex, List<String> expressions, List<MoveChestFilterRule> legacyRules,
            List<Integer> targetMenuSlots, boolean explicitTargetSelection, boolean chestToInventory) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || player.containerMenu == null
                || player.containerMenu.containerId != expectedContainerId) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (sourceMenuSlot < 0 || sourceMenuSlot >= menu.slots.size()) {
            return;
        }

        Slot sourceSlot = menu.getSlot(sourceMenuSlot);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return;
        }

        ItemStack sourceStack = sourceSlot.getItem().copy();
        if (!matchesMoveChestFilters(sourceStack, visibleSourceSlotIndex, expressions, legacyRules, chestToInventory)) {
            return;
        }

        if (!explicitTargetSelection || targetMenuSlots.isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, sourceMenuSlot, 0, ClickType.QUICK_MOVE, player);
            return;
        }

        mc.gameMode.handleInventoryMouseClick(menu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, player);
        if (menu.getCarried() == null || menu.getCarried().isEmpty()) {
            return;
        }

        for (Integer targetMenuSlot : targetMenuSlots) {
            if (targetMenuSlot == null || targetMenuSlot.intValue() == sourceMenuSlot
                    || targetMenuSlot < 0 || targetMenuSlot >= menu.slots.size()) {
                continue;
            }
            ItemStack carried = menu.getCarried();
            if (carried == null || carried.isEmpty()) {
                break;
            }
            Slot targetSlot = menu.getSlot(targetMenuSlot);
            if (!canPlaceIntoTarget(carried, targetSlot)) {
                continue;
            }
            mc.gameMode.handleInventoryMouseClick(menu.containerId, targetMenuSlot, 0, ClickType.PICKUP, player);
        }

        if (menu.getCarried() != null && !menu.getCarried().isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, player);
        }
    }

    private static boolean matchesMoveChestFilters(ItemStack stack, int visibleSourceSlotIndex,
            List<String> expressions, List<MoveChestFilterRule> legacyRules, boolean chestToInventory) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (expressions != null && !expressions.isEmpty()) {
            for (String expression : expressions) {
                if (expression == null || expression.trim().isEmpty()) {
                    continue;
                }
                try {
                    if (InventoryItemFilterExpressionEngine.matches(stack, visibleSourceSlotIndex, expression)) {
                        return true;
                    }
                } catch (Exception e) {
                    zszlScriptMod.LOGGER.warn("[move_chest] 物品过滤表达式匹配失败: {}", expression, e);
                }
            }
            return false;
        }
        if (legacyRules == null || legacyRules.isEmpty()) {
            return false;
        }
        String name = stack.getHoverName() == null ? "" : stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);
        String searchable = buildItemSearchableText(stack).toLowerCase(Locale.ROOT);
        for (MoveChestFilterRule rule : legacyRules) {
            if (rule == null || rule.isEmpty()) {
                continue;
            }
            String itemName = rule.getItemName().trim().toLowerCase(Locale.ROOT);
            if (!itemName.isEmpty() && !name.contains(itemName) && !searchable.contains(itemName)) {
                continue;
            }
            if (!matchesRequiredNbtTags(stack, rule.getRequiredNbtTags(), NBT_TAG_MATCH_MODE_CONTAINS)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean canPlaceIntoTarget(ItemStack carried, Slot targetSlot) {
        if (carried == null || carried.isEmpty() || targetSlot == null) {
            return false;
        }
        if (!targetSlot.hasItem()) {
            return true;
        }
        ItemStack targetStack = targetSlot.getItem();
        if (targetStack.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameTags(targetStack, carried)) {
            return false;
        }
        return targetStack.getCount() < targetStack.getMaxStackSize();
    }

    private static int resolveChestContainerSlotCount(AbstractContainerMenu menu, LocalPlayer player) {
        if (menu == null || player == null) {
            return 0;
        }
        int count = 0;
        Inventory inventory = player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot != null && slot.container != inventory) {
                count++;
            }
        }
        return count;
    }

    private static List<Integer> buildChestMenuSlotList(int chestContainerSlotCount, List<Integer> selectedSlots) {
        List<Integer> result = new ArrayList<>();
        if (selectedSlots == null || selectedSlots.isEmpty()) {
            for (int i = 0; i < chestContainerSlotCount; i++) {
                result.add(i);
            }
            return result;
        }
        for (Integer slot : selectedSlots) {
            if (slot != null && slot >= 0 && slot < chestContainerSlotCount) {
                result.add(slot);
            }
        }
        return result;
    }

    private static List<Integer> buildInventoryMenuSlotList(AbstractContainerMenu menu, LocalPlayer player,
            List<Integer> selectedInventorySlots) {
        List<Integer> result = new ArrayList<>();
        int inventorySize = player == null ? 0 : player.getInventory().items.size();
        if (selectedInventorySlots == null || selectedInventorySlots.isEmpty()) {
            for (int inventoryIndex = 0; inventoryIndex < inventorySize; inventoryIndex++) {
                int menuIndex = findInventoryMenuSlotIndex(menu, player, inventoryIndex);
                if (menuIndex >= 0) {
                    result.add(menuIndex);
                }
            }
            return result;
        }
        for (Integer inventoryIndex : selectedInventorySlots) {
            if (inventoryIndex == null || inventoryIndex < 0 || inventoryIndex >= inventorySize) {
                continue;
            }
            int menuIndex = findInventoryMenuSlotIndex(menu, player, inventoryIndex);
            if (menuIndex >= 0) {
                result.add(menuIndex);
            }
        }
        return result;
    }

    private static int findInventoryMenuSlotIndex(AbstractContainerMenu menu, LocalPlayer player, int inventoryIndex) {
        if (menu == null || player == null) {
            return -1;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot != null && slot.container == inventory && slot.getSlotIndex() == inventoryIndex) {
                return i;
            }
        }
        return -1;
    }

    private static int resolveInventoryIndexByMenuSlot(AbstractContainerMenu menu, LocalPlayer player, int menuSlot) {
        if (menu == null || player == null || menuSlot < 0 || menuSlot >= menu.slots.size()) {
            return -1;
        }
        Slot slot = menu.slots.get(menuSlot);
        if (slot == null || slot.container != player.getInventory()) {
            return -1;
        }
        return slot.getSlotIndex();
    }

    private static boolean hasExplicitSlotSelection(JsonObject params, String arrayKey, String textKey) {
        if (params == null) {
            return false;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray() && params.getAsJsonArray(arrayKey).size() > 0) {
            return true;
        }
        if (params.has(textKey) && params.get(textKey).isJsonPrimitive()
                && !params.get(textKey).getAsString().trim().isEmpty()) {
            return true;
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonPrimitive()
                && !params.get(arrayKey).getAsString().trim().isEmpty()) {
            return true;
        }
        return false;
    }

    private static List<Integer> readSlotSelection(JsonObject params, String arrayKey, String textKey, int maxSlots) {
        LinkedHashSet<Integer> selected = new LinkedHashSet<>();
        if (params == null) {
            return new ArrayList<>();
        }
        if (params.has(arrayKey) && params.get(arrayKey).isJsonArray()) {
            for (JsonElement element : params.getAsJsonArray(arrayKey)) {
                try {
                    int value = element.getAsInt();
                    if (value >= 0 && value < maxSlots) {
                        selected.add(value);
                    }
                } catch (Exception ignored) {
                }
            }
        } else {
            String text = "";
            if (params.has(textKey) && params.get(textKey).isJsonPrimitive()) {
                text = params.get(textKey).getAsString();
            } else if (params.has(arrayKey) && params.get(arrayKey).isJsonPrimitive()) {
                text = params.get(arrayKey).getAsString();
            }
            for (String token : text.split("[,\\r\\n\\s]+")) {
                if (token == null || token.trim().isEmpty()) {
                    continue;
                }
                try {
                    int value = Integer.parseInt(token.trim());
                    if (value >= 0 && value < maxSlots) {
                        selected.add(value);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return new ArrayList<>(selected);
    }
}
