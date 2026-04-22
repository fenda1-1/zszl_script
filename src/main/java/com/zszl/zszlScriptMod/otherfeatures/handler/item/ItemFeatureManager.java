package com.zszl.zszlScriptMod.otherfeatures.handler.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.inventory.ItemStackHelper;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemFeatureManager {

    public static final ItemFeatureManager INSTANCE = new ItemFeatureManager();

    private static final Map<String, FeatureState> FEATURES = new LinkedHashMap<>();
    private static final int DEFAULT_CHEST_STEAL_DELAY_TICKS = 2;
    private static final int DEFAULT_AUTO_EQUIP_INTERVAL_TICKS = 4;
    private static final int DEFAULT_DROP_ALL_DELAY_TICKS = 6;
    private static final int DEFAULT_SHULKER_PREVIEW_BG = 0xFF101822;
    private static final int DEFAULT_SHULKER_PREVIEW_HEADER_BG = 0xFF172331;
    private static final int DEFAULT_SHULKER_PREVIEW_BORDER = 0xFF3A556E;
    private static final int DEFAULT_SHULKER_PREVIEW_SLOT_BG = 0xFF1A2532;
    private static final int DEFAULT_SHULKER_PREVIEW_SLOT_BORDER = 0xFF31475D;

    private static int chestStealDelayTicks = DEFAULT_CHEST_STEAL_DELAY_TICKS;
    private static int autoEquipIntervalTicks = DEFAULT_AUTO_EQUIP_INTERVAL_TICKS;
    private static int dropAllDelayTicks = DEFAULT_DROP_ALL_DELAY_TICKS;
    private static String dropAllKeywordsText = "";

    private int inventorySortCooldownTicks;
    private int chestStealCooldownTicks;
    private int autoEquipCooldownTicks;
    private int dropAllCooldownTicks;

    static {
        register(new FeatureState("inventory_sort", "自动整理",
                "打开背包时自动整理主背包（不动热栏），将装备、工具、食物、方块等按类别排序。", true));
        register(new FeatureState("chest_steal", "箱子窃取", "打开箱子后按间隔自动把物品快速搬到背包。", true));
        register(new FeatureState("auto_equip", "自动装备", "如果身上没穿或有更差的装备，会自动从背包里穿上更好的装备。", true));
        register(new FeatureState("force_no_hunger", "强制不饥饿",
                "持续锁定客户端饥饿值、饱和度和消耗值，尽量让角色保持满饱食状态。", true));
        register(new FeatureState("drop_all", "丢弃所有", "按关键词自动丢弃指定物品，支持多个关键词。", true));
        register(new FeatureState("shulker_preview", "潜影盒预览", "鼠标悬停在潜影盒物品上时，直接显示其内部内容预览。", true));
        loadConfig();
    }

    private ItemFeatureManager() {
    }

    public static final class FeatureState {
        public final String id;
        public final String name;
        public final String description;
        public final String valueLabel = "";
        public final float defaultValue = 0.0F;
        public final float minValue = 0.0F;
        public final float maxValue = 0.0F;
        public final boolean behaviorImplemented;

        private boolean enabled;
        private boolean statusHudEnabled = true;

        private FeatureState(String id, String name, String description, boolean behaviorImplemented) {
            this.id = safe(id);
            this.name = safe(name);
            this.description = safe(description);
            this.behaviorImplemented = behaviorImplemented;
        }

        public boolean supportsValue() {
            return false;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public float getValue() {
            return 0.0F;
        }

        public boolean isStatusHudEnabled() {
            return statusHudEnabled;
        }
    }

    public static List<FeatureState> getFeatures() {
        return new ArrayList<>(FEATURES.values());
    }

    public static FeatureState getFeature(String featureId) {
        return FEATURES.get(normalizeId(featureId));
    }

    public static boolean isManagedFeature(String featureId) {
        return FEATURES.containsKey(normalizeId(featureId));
    }

    public static boolean isEnabled(String featureId) {
        FeatureState state = getFeature(featureId);
        return state != null && state.enabled;
    }

    public static void toggleFeature(String featureId) {
        setEnabled(featureId, !isEnabled(featureId));
    }

    public static void setEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.enabled = enabled;
        saveConfig();
    }

    public static void setFeatureStatusHudEnabled(String featureId, boolean enabled) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.statusHudEnabled = enabled;
        saveConfig();
    }

    public static void resetFeature(String featureId) {
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return;
        }
        state.enabled = false;
        state.statusHudEnabled = true;
        if ("chest_steal".equals(state.id)) {
            chestStealDelayTicks = DEFAULT_CHEST_STEAL_DELAY_TICKS;
        } else if ("auto_equip".equals(state.id)) {
            autoEquipIntervalTicks = DEFAULT_AUTO_EQUIP_INTERVAL_TICKS;
        } else if ("drop_all".equals(state.id)) {
            dropAllDelayTicks = DEFAULT_DROP_ALL_DELAY_TICKS;
            dropAllKeywordsText = "";
        }
        saveConfig();
    }

    public static int getChestStealDelayTicks() {
        return chestStealDelayTicks;
    }

    public static void setChestStealDelayTicks(int ticks) {
        chestStealDelayTicks = clampInt(ticks, 0, 20);
        saveConfig();
    }

    public static int getAutoEquipIntervalTicks() {
        return autoEquipIntervalTicks;
    }

    public static void setAutoEquipIntervalTicks(int ticks) {
        autoEquipIntervalTicks = clampInt(ticks, 1, 40);
        saveConfig();
    }

    public static int getDropAllDelayTicks() {
        return dropAllDelayTicks;
    }

    public static void setDropAllDelayTicks(int ticks) {
        dropAllDelayTicks = clampInt(ticks, 0, 20);
        saveConfig();
    }

    public static String getDropAllKeywordsText() {
        return dropAllKeywordsText == null ? "" : dropAllKeywordsText;
    }

    public static void setDropAllKeywordsText(String text) {
        dropAllKeywordsText = text == null ? "" : text.trim();
        saveConfig();
    }

    public void tick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            resetRuntimeState();
            return;
        }

        tickCooldowns();
        handleForceNoHunger(mc.player);
        handleAutoEquip(mc, mc.player);
        handleChestSteal(mc, mc.player);
        handleInventorySort(mc, mc.player);
        handleDropAll(mc, mc.player);
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!isEnabled("shulker_preview") || event.getItemStack().isEmpty()) {
            return;
        }
        if (!hasShulkerPreviewContents(event.getItemStack())) {
            return;
        }
        event.getToolTip().add(Component.literal("§8[潜影盒预览]"));
    }

    @SubscribeEvent
    public void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled("shulker_preview")
                || event == null
                || mc == null
                || !(mc.screen instanceof AbstractContainerScreen<?>)
                || !hasShulkerPreviewContents(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled("shulker_preview")
                || mc == null
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        Slot hoveredSlot = resolveHoveredSlot(screen);
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        NonNullList<ItemStack> items = getShulkerPreviewItems(stack);
        if (items == null) {
            return;
        }
        renderShulkerPreview(event.getGuiGraphics(), screen, stack, items, event.getMouseX(), event.getMouseY());
    }

    public static String getFeatureRuntimeSummary(String featureId) {
        return INSTANCE.buildFeatureRuntimeSummary(normalizeId(featureId));
    }

    public static List<String> getStatusLines() {
        return getStatusLines(false);
    }

    public static List<String> getStatusLines(boolean forcePreview) {
        if (!forcePreview && !MovementFeatureManager.isMasterStatusHudEnabled()) {
            return new ArrayList<>();
        }

        List<String> activeNames = new ArrayList<>();
        for (FeatureState state : FEATURES.values()) {
            if (state != null && state.enabled && state.statusHudEnabled) {
                activeNames.add(state.name);
            }
        }

        List<String> lines = new ArrayList<>();
        if (activeNames.isEmpty()) {
            return lines;
        }

        lines.add("§a[物品] §f" + activeNames.size() + " 项开启");
        StringBuilder builder = new StringBuilder("§7");
        for (int i = 0; i < activeNames.size() && i < 4; i++) {
            if (i > 0) {
                builder.append("、");
            }
            builder.append(activeNames.get(i));
        }
        if (activeNames.size() > 4) {
            builder.append(" §8+").append(activeNames.size() - 4);
        }
        lines.add(builder.toString());

        String runtime = INSTANCE.getRuntimeHudLine();
        if (!runtime.isEmpty()) {
            lines.add(runtime);
        }
        return lines;
    }

    public static void loadConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_item.json");
            if (!Files.exists(file)) {
                return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject features = root.has("features") && root.get("features").isJsonObject()
                    ? root.getAsJsonObject("features")
                    : root;

            chestStealDelayTicks = root.has("chestStealDelayTicks")
                    ? clampInt(root.get("chestStealDelayTicks").getAsInt(), 0, 20)
                    : DEFAULT_CHEST_STEAL_DELAY_TICKS;
            autoEquipIntervalTicks = root.has("autoEquipIntervalTicks")
                    ? clampInt(root.get("autoEquipIntervalTicks").getAsInt(), 1, 40)
                    : DEFAULT_AUTO_EQUIP_INTERVAL_TICKS;
            dropAllDelayTicks = root.has("dropAllDelayTicks")
                    ? clampInt(root.get("dropAllDelayTicks").getAsInt(), 0, 20)
                    : DEFAULT_DROP_ALL_DELAY_TICKS;
            dropAllKeywordsText = root.has("dropAllKeywordsText") ? root.get("dropAllKeywordsText").getAsString() : "";

            for (FeatureState state : FEATURES.values()) {
                if (!features.has(state.id) || !features.get(state.id).isJsonObject()) {
                    continue;
                }
                JsonObject json = features.getAsJsonObject(state.id);
                if (json.has("enabled")) {
                    state.enabled = json.get("enabled").getAsBoolean();
                }
                if (json.has("statusHudEnabled")) {
                    state.statusHudEnabled = json.get("statusHudEnabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载物品功能配置失败", e);
        }
    }

    public static void saveConfig() {
        try {
            Path file = ProfileManager.getCurrentProfileDir().resolve("other_features_item.json");
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject features = new JsonObject();
            root.addProperty("chestStealDelayTicks", chestStealDelayTicks);
            root.addProperty("autoEquipIntervalTicks", autoEquipIntervalTicks);
            root.addProperty("dropAllDelayTicks", dropAllDelayTicks);
            root.addProperty("dropAllKeywordsText", getDropAllKeywordsText());
            for (FeatureState state : FEATURES.values()) {
                JsonObject json = new JsonObject();
                json.addProperty("enabled", state.enabled);
                json.addProperty("statusHudEnabled", state.statusHudEnabled);
                features.add(state.id, json);
            }
            root.add("features", features);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存物品功能配置失败", e);
        }
    }

    public void onClientDisconnect() {
        resetRuntimeState();
    }

    private void tickCooldowns() {
        if (inventorySortCooldownTicks > 0) {
            inventorySortCooldownTicks--;
        }
        if (chestStealCooldownTicks > 0) {
            chestStealCooldownTicks--;
        }
        if (autoEquipCooldownTicks > 0) {
            autoEquipCooldownTicks--;
        }
        if (dropAllCooldownTicks > 0) {
            dropAllCooldownTicks--;
        }
    }

    private void handleForceNoHunger(LocalPlayer player) {
        if (!isEnabled("force_no_hunger") || player == null) {
            return;
        }

        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(20.0F);
        foodData.setExhaustion(0.0F);
    }

    private void handleAutoEquip(Minecraft mc, LocalPlayer player) {
        if (!isEnabled("auto_equip") || autoEquipCooldownTicks > 0 || mc.gameMode == null) {
            return;
        }
        if (mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof InventoryScreen)) {
            return;
        }
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        Inventory inventory = player.getInventory();
        int bestMenuSlot = -1;
        double bestGain = 0.0D;

        for (int invIndex = 0; invIndex < inventory.items.size(); invIndex++) {
            ItemStack stack = inventory.items.get(invIndex);
            if (!(stack.getItem() instanceof ArmorItem armorItem)) {
                continue;
            }

            EquipmentSlot slot = armorItem.getEquipmentSlot();
            ItemStack equipped = player.getItemBySlot(slot);
            double gain = getArmorScore(stack) - getArmorScore(equipped);
            if (gain > bestGain + 0.05D) {
                bestGain = gain;
                bestMenuSlot = inventoryIndexToMenuSlot(invIndex);
            }
        }

        if (bestMenuSlot >= 0) {
            mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, bestMenuSlot, 0,
                    ClickType.QUICK_MOVE, player);
            autoEquipCooldownTicks = autoEquipIntervalTicks;
        }
    }

    private void handleChestSteal(Minecraft mc, LocalPlayer player) {
        if (!isEnabled("chest_steal") || chestStealCooldownTicks > 0 || !(player.containerMenu instanceof ChestMenu chestMenu)) {
            return;
        }
        int containerSlots = chestMenu.getRowCount() * 9;
        for (int i = 0; i < containerSlots; i++) {
            if (chestMenu.slots.get(i).hasItem()) {
                mc.gameMode.handleInventoryMouseClick(chestMenu.containerId, i, 0, ClickType.QUICK_MOVE, player);
                chestStealCooldownTicks = chestStealDelayTicks;
                return;
            }
        }
    }

    private void handleInventorySort(Minecraft mc, LocalPlayer player) {
        if (!isEnabled("inventory_sort") || inventorySortCooldownTicks > 0
                || !(mc.screen instanceof InventoryScreen)
                || player.containerMenu != player.inventoryMenu) {
            return;
        }

        List<ItemStack> sorted = new ArrayList<>();
        for (int slot = 9; slot <= 35; slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (!stack.isEmpty()) {
                sorted.add(stack.copy());
            }
        }
        sorted.sort(INVENTORY_SORT_COMPARATOR);
        while (sorted.size() < 27) {
            sorted.add(ItemStack.EMPTY);
        }

        for (int index = 0; index < 27; index++) {
            int targetInvSlot = 9 + index;
            ItemStack desired = sorted.get(index);
            ItemStack current = player.getInventory().items.get(targetInvSlot);
            if (sameStackIdentity(current, desired)) {
                continue;
            }

            if (desired.isEmpty()) {
                int emptySlot = findEmptyInventorySlot(player, targetInvSlot + 1, 35);
                if (emptySlot >= 0) {
                    moveInventorySlot(mc, player, targetInvSlot, emptySlot);
                    inventorySortCooldownTicks = 4;
                }
                return;
            }

            int sourceInvSlot = findMatchingInventorySlot(player, desired, targetInvSlot + 1, 35);
            if (sourceInvSlot >= 0) {
                moveInventorySlot(mc, player, sourceInvSlot, targetInvSlot);
                inventorySortCooldownTicks = 4;
            }
            return;
        }

        inventorySortCooldownTicks = 10;
    }

    private void handleDropAll(Minecraft mc, LocalPlayer player) {
        if (!isEnabled("drop_all") || dropAllCooldownTicks > 0 || getDropAllKeywordsText().trim().isEmpty()) {
            return;
        }
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        List<String> keywords = getDropAllKeywords();
        for (int invIndex = 0; invIndex < player.getInventory().items.size(); invIndex++) {
            ItemStack stack = player.getInventory().items.get(invIndex);
            if (stack.isEmpty()) {
                continue;
            }

            String display = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            boolean matched = false;
            for (String keyword : keywords) {
                if (!keyword.isEmpty() && display.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }

            mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                    inventoryIndexToMenuSlot(invIndex), 1, ClickType.THROW, player);
            dropAllCooldownTicks = dropAllDelayTicks;
            return;
        }
    }

    private void moveInventorySlot(Minecraft mc, LocalPlayer player, int sourceInvSlot, int targetInvSlot) {
        if (sourceInvSlot == targetInvSlot) {
            return;
        }

        int sourceMenuSlot = inventoryIndexToMenuSlot(sourceInvSlot);
        int targetMenuSlot = inventoryIndexToMenuSlot(targetInvSlot);
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, targetMenuSlot, 0, ClickType.PICKUP, player);
        if (!player.inventoryMenu.getCarried().isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, player);
        }
    }

    private int findEmptyInventorySlot(LocalPlayer player, int start, int end) {
        for (int slot = Math.max(9, start); slot <= Math.min(35, end); slot++) {
            if (player.getInventory().items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private int findMatchingInventorySlot(LocalPlayer player, ItemStack target, int start, int end) {
        for (int slot = Math.max(9, start); slot <= Math.min(35, end); slot++) {
            if (sameStackIdentity(player.getInventory().items.get(slot), target)) {
                return slot;
            }
        }
        return -1;
    }

    private static int inventoryIndexToMenuSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    private static double getArmorScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
            return -1.0D;
        }
        double score = armorItem.getDefense() * 10.0D;
        score += armorItem.getToughness() * 1.5D;
        score += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack) * 3.0D;
        score += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, stack) * 1.5D;
        score += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack) * 1.2D;
        score += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, stack) * 1.2D;
        return score;
    }

    private static int categoryWeight(ItemStack stack) {
        if (stack.isEmpty()) {
            return 99;
        }
        if (stack.getItem() instanceof ArmorItem) {
            return 0;
        }
        if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem || stack.getItem() instanceof BowItem) {
            return 1;
        }
        if (stack.isEdible()) {
            return 2;
        }
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
            return 3;
        }
        if (stack.isDamageableItem()) {
            return 4;
        }
        return 5;
    }

    private static boolean sameStackIdentity(ItemStack a, ItemStack b) {
        if (a == null || a.isEmpty()) {
            return b == null || b.isEmpty();
        }
        if (b == null || b.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameTags(a, b) && a.getCount() == b.getCount();
    }

    private List<String> getDropAllKeywords() {
        List<String> keywords = new ArrayList<>();
        if (dropAllKeywordsText == null || dropAllKeywordsText.trim().isEmpty()) {
            return keywords;
        }
        String normalized = dropAllKeywordsText.replace('，', ',').replace('\n', ',').replace(';', ',');
        for (String part : normalized.split(",")) {
            String keyword = safe(part);
            if (!keyword.isEmpty()) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    private boolean hasShulkerPreviewContents(ItemStack stack) {
        return getShulkerPreviewItems(stack) != null;
    }

    private NonNullList<ItemStack> getShulkerPreviewItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null || !blockEntityTag.contains("Items", 9)) {
            return null;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(blockEntityTag, items);
        return items;
    }

    private void renderShulkerPreview(GuiGraphics graphics, AbstractContainerScreen<?> screen, ItemStack shulkerStack,
            NonNullList<ItemStack> items, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int panelWidth = 162;
        int panelHeight = 78;
        int panelX = Math.min(mouseX + 12, screen.width - panelWidth - 6);
        int panelY = Math.min(mouseY + 12, screen.height - panelHeight - 6);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, DEFAULT_SHULKER_PREVIEW_BG);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 15, DEFAULT_SHULKER_PREVIEW_HEADER_BG);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF5FB8FF);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, DEFAULT_SHULKER_PREVIEW_BORDER);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, DEFAULT_SHULKER_PREVIEW_BORDER);
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, DEFAULT_SHULKER_PREVIEW_BORDER);
        graphics.drawString(mc.font, shulkerStack.getHoverName(), panelX + 6, panelY + 4, 0xFFEAF6FF, false);

        int startX = panelX + 6;
        int startY = panelY + 20;
        for (int i = 0; i < items.size(); i++) {
            int slotX = startX + (i % 9) * 17;
            int slotY = startY + (i / 9) * 17;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, DEFAULT_SHULKER_PREVIEW_SLOT_BG);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 1, DEFAULT_SHULKER_PREVIEW_SLOT_BORDER);
            graphics.fill(slotX, slotY + 15, slotX + 16, slotY + 16, DEFAULT_SHULKER_PREVIEW_SLOT_BORDER);
            graphics.fill(slotX, slotY, slotX + 1, slotY + 16, DEFAULT_SHULKER_PREVIEW_SLOT_BORDER);
            graphics.fill(slotX + 15, slotY, slotX + 16, slotY + 16, DEFAULT_SHULKER_PREVIEW_SLOT_BORDER);

            ItemStack item = items.get(i);
            if (item.isEmpty()) {
                continue;
            }
            graphics.renderItem(item, slotX, slotY);
            graphics.renderItemDecorations(mc.font, item, slotX, slotY);
        }
    }

    private Slot resolveHoveredSlot(AbstractContainerScreen<?> screen) {
        try {
            for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Slot.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object value = field.get(screen);
                        if (value instanceof Slot slot) {
                            return slot;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void resetRuntimeState() {
        inventorySortCooldownTicks = 0;
        chestStealCooldownTicks = 0;
        autoEquipCooldownTicks = 0;
        dropAllCooldownTicks = 0;
    }

    private String getRuntimeHudLine() {
        List<String> parts = new ArrayList<>();
        if (isEnabled("auto_equip") && getFeature("auto_equip").statusHudEnabled) {
            parts.add("§b自动装备");
        }
        if (isEnabled("chest_steal") && getFeature("chest_steal").statusHudEnabled) {
            parts.add("§e箱窃:" + chestStealDelayTicks + "t");
        }
        if (isEnabled("drop_all") && getFeature("drop_all").statusHudEnabled) {
            parts.add("§c丢弃词:" + getDropAllKeywords().size());
        }
        if (isEnabled("force_no_hunger") && getFeature("force_no_hunger").statusHudEnabled) {
            parts.add("§a锁饥饿");
        }
        if (parts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("§7");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append("  ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String buildFeatureRuntimeSummary(String featureId) {
        if (featureId == null || featureId.isEmpty()) {
            return "待机";
        }
        FeatureState state = getFeature(featureId);
        if (state == null) {
            return "未找到功能";
        }

        switch (featureId) {
        case "auto_equip":
            return isEnabled(featureId) ? "自动装备扫描间隔: " + autoEquipIntervalTicks + " tick" : "未启用";
        case "force_no_hunger":
            return isEnabled(featureId) ? "持续锁定客户端饥饿值与饱和度" : "未启用";
        case "inventory_sort":
            return isEnabled(featureId) ? "打开背包界面时自动整理主背包" : "未启用";
        case "chest_steal":
            return isEnabled(featureId) ? "箱子搬运间隔: " + chestStealDelayTicks + " tick" : "未启用";
        case "drop_all":
            if (!isEnabled(featureId)) {
                return "未启用";
            }
            List<String> keywords = getDropAllKeywords();
            return keywords.isEmpty() ? "已启用，但尚未配置丢弃关键词" : "丢弃关键词: " + String.join(" / ", keywords);
        case "shulker_preview":
            return isEnabled(featureId) ? "悬停潜影盒时显示内容预览" : "未启用";
        default:
            return state.enabled ? "已启用" : "未启用";
        }
    }

    private static void register(FeatureState state) {
        FEATURES.put(state.id, state);
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static String normalizeId(String featureId) {
        return safe(featureId).toLowerCase(Locale.ROOT);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final Comparator<ItemStack> INVENTORY_SORT_COMPARATOR = Comparator
            .comparingInt(ItemFeatureManager::categoryWeight)
            .thenComparing(stack -> stack.isEmpty() ? "" : safe(stack.getHoverName().getString()), String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(stack -> stack.isEmpty() ? 0 : -stack.getCount());
}
