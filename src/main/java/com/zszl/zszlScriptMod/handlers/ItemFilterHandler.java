package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ItemFilterHandler {
    public static final File FILTER_CONFIG_FILE = new File(ModConfig.CONFIG_DIR, "filter_config.json");
    private static final int DEFAULT_WAREHOUSE_TRANSFER_DELAY_MS = 100;
    private static final int DEFAULT_MOVE_TO_CHEST_DELAY_TICKS = 2;
    public static final String NBT_TAG_MATCH_MODE_CONTAINS = "CONTAINS";
    public static final String NBT_TAG_MATCH_MODE_NOT_CONTAINS = "NOT_CONTAINS";
    public static final String MOVE_DIRECTION_INVENTORY_TO_CHEST = "INVENTORY_TO_CHEST";
    public static final String MOVE_DIRECTION_CHEST_TO_INVENTORY = "CHEST_TO_INVENTORY";

    private static final class ContainerSlotGroups {
        private final List<Integer> containerSlots;
        private final List<Integer> playerInventorySlots;

        private ContainerSlotGroups(List<Integer> containerSlots, List<Integer> playerInventorySlots) {
            this.containerSlots = containerSlots;
            this.playerInventorySlots = playerInventorySlots;
        }
    }

    public static final class MoveChestFilterRule {
        private final String itemName;
        private final List<String> requiredNbtTags;

        private MoveChestFilterRule(String itemName, List<String> requiredNbtTags) {
            this.itemName = itemName == null ? "" : itemName.trim();
            this.requiredNbtTags = requiredNbtTags == null ? Collections.<String>emptyList() : requiredNbtTags;
        }

        public String getItemName() {
            return itemName;
        }

        public List<String> getRequiredNbtTags() {
            return requiredNbtTags;
        }

        public boolean isEmpty() {
            return itemName.isEmpty() && (requiredNbtTags == null || requiredNbtTags.isEmpty());
        }
    }

    private static volatile int pendingWarehouseTransferClicks = 0;
    private static volatile boolean warehouseTransferInProgress = false;

    public static List<String> blacklistFilters = new ArrayList<>();
    public static List<String> whitelistFilters = new ArrayList<>();

    static {
        loadFilterConfig();
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("filter_config.json").toFile();
    }

    /**
     * 丢弃配置保存方法
     */
    public static void saveFilterConfig() {
        try {
            File configFile = getConfigFile();
            JsonObject json = new JsonObject();
            json.add("blacklist", new Gson().toJsonTree(blacklistFilters));
            json.add("whitelist", new Gson().toJsonTree(whitelistFilters));

            // 确保父目录存在
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            Files.write(configFile.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存过滤配置失败", e);
        }
    }

    /**
     * 丢弃配置加载方法
     */
    public static void loadFilterConfig() {
        try {
            File configFile = getConfigFile();
            if (configFile.exists()) {
                String jsonContent = new String(
                        Files.readAllBytes(configFile.toPath()),
                        StandardCharsets.UTF_8);
                JsonObject json = new JsonParser().parse(jsonContent).getAsJsonObject();

                blacklistFilters = new Gson().fromJson(
                        json.get("blacklist"), new TypeToken<List<String>>() {
                        }.getType());
                whitelistFilters = new Gson().fromJson(
                        json.get("whitelist"), new TypeToken<List<String>>() {
                        }.getType());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载过滤配置失败", e);
        }
    }

    /**
     * 根据名称过滤丢弃物品（黑名单+白名单机制）
     */
    public static void dropItemsByNameFilter() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return;

        // 捕获当前容器和其窗口ID，以便在延迟任务中进行验证
        Container initialContainer = player.openContainer;
        if (initialContainer == null) {
            System.err.println("警告: 没有打开的容器，无法执行丢弃操作。");
            return;
        }
        int initialWindowId = initialContainer.windowId;

        List<Integer> slotsToDrop = new ArrayList<>();

        // 增强安全性的容器操作
        try {
            int containerSize = initialContainer.inventorySlots.size();
            // 检查容器是否为空或无效
            if (containerSize <= 0) {
                System.err.println("警告: 容器大小无效 " + containerSize);
                return;
            }

            // 遍历当前容器的所有槽位
            for (int i = 0; i < containerSize; i++) {
                try {
                    Slot slot = initialContainer.getSlot(i);
                    if (slot == null || !slot.canTakeStack(player)) {
                        continue;
                    }

                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty() || stack.getCount() <= 0) {
                        continue;
                    }

                    String itemName = stack.getDisplayName().trim();
                    if (itemName.isEmpty()) {
                        continue;
                    }

                    boolean shouldDrop = blacklistFilters.stream().anyMatch(itemName::contains);
                    boolean shouldKeep = whitelistFilters.stream().anyMatch(itemName::contains);

                    if (shouldDrop && !shouldKeep) {
                        slotsToDrop.add(i);
                        if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)
                                && Minecraft.getMinecraft().player != null) {
                            Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                                    String.format("§d[调试] §7物品 [%s] 被标记为待丢弃 (黑名单匹配)。", itemName)));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("警告: 处理槽位 " + i + " 时出错: " + e.getMessage());
                    // 跳过问题槽位，继续处理其他槽位
                }
            }
        } catch (Exception e) {
            System.err.println("严重错误: 处理物品栏时出错: " + e.getMessage());
            e.printStackTrace();
            return; // 如果在初始扫描时出现严重错误，则停止
        }

        // 如果没有物品需要丢弃，则直接返回
        if (slotsToDrop.isEmpty()) {
            return;
        }

        // 优化后的并行丢弃逻辑
        Minecraft.getMinecraft().addScheduledTask(() -> {
            // 在执行任务前再次检查容器是否仍然打开且未改变
            if (player.openContainer == null || player.openContainer.windowId != initialWindowId) {
                System.err.println("警告: 容器已更改或关闭，取消丢弃操作。");
                return; // 如果容器已更改或关闭，则中止操作
            }

            // 将槽位分成3组并行处理
            int batchSize = 3;
            for (int batch = 0; batch < slotsToDrop.size(); batch += batchSize) {
                final int currentBatch = batch;
                new ModUtils.DelayAction(batch * 3, () -> {
                    // 在每个延迟批次执行前再次检查容器状态
                    if (Minecraft.getMinecraft().player == null)
                        return;
                    if (Minecraft.getMinecraft().player.openContainer == null
                            || Minecraft.getMinecraft().player.openContainer.windowId != initialWindowId) {
                        System.err.println("警告: 容器在延迟操作中途更改或关闭，取消剩余丢弃操作。");
                        return; // 如果容器已更改或关闭，则中止剩余操作
                    }

                    // 并行处理当前批次
                    for (int i = 0; i < batchSize; i++) {
                        int globalIndex = currentBatch + i;
                        if (globalIndex >= slotsToDrop.size()) {
                            return; // 当前批次所有槽位已处理
                        }

                        int containerSlot = slotsToDrop.get(globalIndex);

                        // 关键检查：确保槽位索引对于当前打开的容器仍然有效
                        if (containerSlot < 0 || containerSlot >= player.openContainer.inventorySlots.size()) {
                            System.err.println(
                                    "警告: 尝试丢弃的槽位 " + containerSlot + " 对于当前容器 (ID: " + player.openContainer.windowId
                                            + ", Size: " + player.openContainer.inventorySlots.size() + ") 无效，跳过。");
                            continue; // 跳过无效槽位
                        }

                        // 执行丢弃流程
                        Minecraft.getMinecraft().playerController.windowClick(
                                player.openContainer.windowId,
                                containerSlot, 0, ClickType.PICKUP, player);
                        // 丢弃到地面（点击容器外）
                        Minecraft.getMinecraft().playerController.windowClick(
                                player.openContainer.windowId,
                                -999, 0, ClickType.PICKUP, player);
                    }
                }).accept(player);
            }
        });
    }

    /**
     * 根据名称过滤点击物品（黑名单+白名单机制）
     */
    public static void ClickItemsByNameFilter() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return;

        if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
            player.sendMessage(new TextComponentString("§d[调试] §7开始执行 ClickItemsByNameFilter..."));
        }

        Container initialContainer = player.openContainer;
        if (initialContainer == null) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §c错误: 容器未打开，操作取消。"));
            }
            return;
        }
        int initialWindowId = initialContainer.windowId;

        List<Integer> slotsToKeep = new ArrayList<>();

        try {
            int totalSlots = initialContainer.inventorySlots.size();
            if (totalSlots <= 36)
                return; // 如果没有容器部分，则直接返回

            // --- 核心修复: 只扫描玩家背包的槽位 ---
            // 玩家的背包槽位总是在容器的最后36个。
            int playerInventoryStartIndex = totalSlots - 36;

            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString(String.format("§d[调试] §7(旧方法安全修复) 扫描玩家背包槽位 %d -> %d...",
                        playerInventoryStartIndex, totalSlots - 1)));
            }

            for (int i = playerInventoryStartIndex; i < totalSlots; i++) {
                Slot slot = initialContainer.getSlot(i);
                if (slot == null || !slot.canTakeStack(player) || slot.getStack().isEmpty()) {
                    continue;
                }

                String itemName = slot.getStack().getDisplayName().trim();
                boolean shouldKeep = whitelistFilters.stream().anyMatch(itemName::contains);

                if (shouldKeep) {
                    slotsToKeep.add(i);
                    if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                        player.sendMessage(new TextComponentString(
                                String.format("§d[调试] §a匹配成功: §f物品 [%s] 在槽位 %d, 将被点击。", itemName, i)));
                    }
                }
            }
            // --- 修复结束 ---

        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("处理物品栏时出错", e);
            return;
        }

        if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
            if (slotsToKeep.isEmpty()) {
                player.sendMessage(new TextComponentString("§d[调试] §e总结: 未找到任何符合白名单的物品进行点击。"));
            } else {
                player.sendMessage(
                        new TextComponentString(String.format("§d[调试] §a总结: 共找到 %d 个物品将被点击。", slotsToKeep.size())));
            }
        }

        if (slotsToKeep.isEmpty()) {
            return;
        }

        // 点击逻辑保持不变，但现在只点击背包里的物品
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (player.openContainer == null || player.openContainer.windowId != initialWindowId) {
                return;
            }
            int batchSize = 3;
            for (int batch = 0; batch < slotsToKeep.size(); batch += batchSize) {
                final int currentBatch = batch;
                new ModUtils.DelayAction(batch * 3, () -> {
                    if (Minecraft.getMinecraft().player == null || Minecraft.getMinecraft().player.openContainer == null
                            || Minecraft.getMinecraft().player.openContainer.windowId != initialWindowId) {
                        return;
                    }
                    for (int i = 0; i < batchSize; i++) {
                        int globalIndex = currentBatch + i;
                        if (globalIndex >= slotsToKeep.size()) {
                            return;
                        }
                        int containerSlot = slotsToKeep.get(globalIndex);
                        if (containerSlot >= 0 && containerSlot < player.openContainer.inventorySlots.size()) {
                            Minecraft.getMinecraft().playerController.windowClick(
                                    player.openContainer.windowId,
                                    containerSlot, 0, ClickType.PICKUP, player);
                        }
                    }
                }).accept(player);
            }
        });
    }

    /**
     * [新方法] 自动将背包中的物品转移到仓库。
     * <p>
     * 此方法专门用于在打开仓库（或其他类似箱子的GUI）时，
     * 根据当前设置的模式（黑白名单或只拿模式），
     * 点击玩家背包中的对应物品，以将其存入仓库。
     * 它会使用竞技场设置中的“拿取设置”来控制点击速度。
     */
    public static void transferItemsToWarehouse() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null)
            return;

        if (warehouseTransferInProgress) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e仓库转移仍在进行中，本次请求已忽略。"));
            }
            return;
        }

        warehouseTransferInProgress = false;
        pendingWarehouseTransferClicks = 0;

        if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
            player.sendMessage(new TextComponentString("§d[调试] §7开始执行仓库转移逻辑..."));
        }

        Container container = player.openContainer;
        // 确保当前打开的是一个箱子类型的GUI
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §c错误: 未检测到箱子GUI，操作取消。"));
            }
            return;
        }
        int windowId = container.windowId;

        List<Integer> slotsToClick = new ArrayList<>();

        // 1. 识别并过滤需要点击的物品槽位
        try {
            int totalSlots = container.inventorySlots.size();
            // 在箱子GUI中，玩家的背包槽位总是在最后36个
            int playerInventoryStartIndex = totalSlots - 36;

            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString(String.format("§d[调试] §7当前模式: §b%s§7. 扫描玩家背包槽位 %d -> %d",
                        ArenaItemHandler.currentMode.getDisplayName(), playerInventoryStartIndex, totalSlots - 1)));
            }

            for (int i = playerInventoryStartIndex; i < totalSlots; i++) {
                Slot slot = container.getSlot(i);
                if (slot != null && slot.getHasStack()) {
                    ItemStack stack = slot.getStack();
                    String itemName = stack.getDisplayName().trim();
                    boolean shouldClick = false;

                    switch (ArenaItemHandler.currentMode) {
                        case BLACKLIST_WHITELIST:
                            boolean onWhitelist = whitelistFilters.stream().anyMatch(itemName::contains);
                            boolean onBlacklist = blacklistFilters.stream().anyMatch(itemName::contains);
                            // 只有在白名单上且不在黑名单上的物品才会被存入
                            if (onWhitelist && !onBlacklist) {
                                shouldClick = true;
                            }
                            break;
                        case PICK_ONLY:
                            // 只拿模式下，只存入预设的贵重物品
                            if (ArenaItemHandler.selectedPresetItems.stream().anyMatch(itemName::contains)) {
                                shouldClick = true;
                            }
                            break;
                    }

                    if (shouldClick) {
                        slotsToClick.add(i);
                        if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                            player.sendMessage(new TextComponentString(
                                    String.format("§d[调试] §a匹配成功: §f物品 [%s] 在槽位 %d, 将被点击转移。", itemName, i)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("扫描背包以转移物品时出错", e);
            return;
        }

        if (slotsToClick.isEmpty()) {
            warehouseTransferInProgress = false;
            pendingWarehouseTransferClicks = 0;
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e总结: 未在背包中找到需要转移到仓库的物品。"));
            }
            return;
        }

        warehouseTransferInProgress = true;
        pendingWarehouseTransferClicks = slotsToClick.size();

        int delayMs = DEFAULT_WAREHOUSE_TRANSFER_DELAY_MS;
        int delayTicks = Math.max(1, delayMs / 50); // 将毫秒转换为tick，50ms = 1tick，最小1tick

        if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
            player.sendMessage(new TextComponentString(
                    String.format("§d[调试] §7将使用转移仓库延迟进行点击: 延迟=%dms (约%d tick), 共%d个物品",
                            delayMs, delayTicks, slotsToClick.size())));
        }

        // 2. 按顺序逐一点击每个物品，每次点击之间有固定延迟
        for (int i = 0; i < slotsToClick.size(); i++) {
            final int slotIndex = slotsToClick.get(i);
            int delay = i * delayTicks; // 每个物品依次延迟

            ModUtils.DelayScheduler.instance.schedule(new Runnable() {
                @Override
                public void run() {
                    // 每次执行前都检查GUI和窗口ID是否仍然有效
                    if (Minecraft.getMinecraft().player == null ||
                            !(Minecraft.getMinecraft().currentScreen instanceof GuiChest) ||
                            Minecraft.getMinecraft().player.openContainer == null ||
                            Minecraft.getMinecraft().player.openContainer.windowId != windowId) {
                        markWarehouseTransferTaskFinished();
                        return;
                    }

                    Minecraft.getMinecraft().playerController.windowClick(
                            windowId,
                            slotIndex,
                            0,
                            ClickType.PICKUP,
                            Minecraft.getMinecraft().player);

                    if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                        Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                                String.format("§d[调试] §7已点击槽位 %d", slotIndex)));
                    }

                    markWarehouseTransferTaskFinished();
                }
            }, delay);

        }
        // --- 修改结束 ---
    }

    public static boolean isWarehouseTransferInProgress() {
        return warehouseTransferInProgress;
    }

    private static void markWarehouseTransferTaskFinished() {
        int remain = pendingWarehouseTransferClicks - 1;
        pendingWarehouseTransferClicks = Math.max(0, remain);
        if (pendingWarehouseTransferClicks <= 0) {
            warehouseTransferInProgress = false;
        }
    }

    public static void moveInventoryItemsToChestSlots(JsonObject params) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }

        if (warehouseTransferInProgress) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e物品槽位转移仍在进行中，本次请求已忽略。"));
            }
            return;
        }

        warehouseTransferInProgress = false;
        pendingWarehouseTransferClicks = 0;

        Container container = player.openContainer;
        if (Minecraft.getMinecraft().currentScreen == null || container == null) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §c错误: 未检测到已打开的容器界面，槽位转移取消。"));
            }
            return;
        }
        if (player.inventory.getItemStack() != null && !player.inventory.getItemStack().isEmpty()) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e鼠标光标上已有物品，槽位转移取消。"));
            }
            return;
        }

        ContainerSlotGroups slotGroups = resolveContainerSlotGroups(container, player);
        int containerSlotCount = slotGroups.containerSlots.size();
        int playerInventoryVisibleSlots = slotGroups.playerInventorySlots.size();
        if (containerSlotCount <= 0 || playerInventoryVisibleSlots <= 0) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e当前容器未识别出可写入容器槽位或可用背包槽位，操作取消。"));
            }
            return;
        }

        List<Integer> selectedContainerSlots = readSlotList(params, "chestSlots", "chestSlotsText", containerSlotCount);
        List<Integer> selectedInventorySlots = readSlotList(params, "inventorySlots", "inventorySlotsText",
                playerInventoryVisibleSlots);
        List<MoveChestFilterRule> filterRules = readMoveChestFilterRules(params);
        String requiredNbtTagMatchMode = readRequiredNbtTagMatchMode(params);
        String moveDirection = readMoveChestDirection(params);

        if (filterRules.isEmpty()) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e至少需要添加一条有效的物品转移规则，槽位转移取消。"));
            }
            return;
        }

        if (selectedContainerSlots.isEmpty() || selectedInventorySlots.isEmpty()) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e未选择有效的容器槽位或背包槽位，操作取消。"));
            }
            return;
        }

        List<Integer> sourceSlotIndices = MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(moveDirection)
                ? slotGroups.containerSlots
                : slotGroups.playerInventorySlots;
        List<Integer> selectedSourceSlots = MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(moveDirection)
                ? selectedContainerSlots
                : selectedInventorySlots;
        List<Integer> targetSlotIndices = MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(moveDirection)
                ? slotGroups.playerInventorySlots
                : slotGroups.containerSlots;
        List<Integer> selectedTargetSlots = MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(moveDirection)
                ? selectedInventorySlots
                : selectedContainerSlots;

        int windowId = container.windowId;
        List<Integer> clickPlan = buildMoveBetweenSlotGroupsClickPlan(container,
                sourceSlotIndices,
                selectedSourceSlots,
                targetSlotIndices,
                selectedTargetSlots,
                filterRules,
                requiredNbtTagMatchMode);
        if (clickPlan.isEmpty()) {
            if (ModConfig.isDebugFlagEnabled(DebugModule.ITEM_FILTER)) {
                player.sendMessage(new TextComponentString("§d[调试] §e未找到符合任意转移规则且可放入目标槽位的物品。"));
            }
            return;
        }

        warehouseTransferInProgress = true;
        pendingWarehouseTransferClicks = clickPlan.size();
        int moveDelayTicks = DEFAULT_MOVE_TO_CHEST_DELAY_TICKS;
        boolean normalizeDelayTo20Tps = true;
        if (params != null && params.has("delayTicks")) {
            try {
                moveDelayTicks = Math.max(0, params.get("delayTicks").getAsInt());
            } catch (Exception ignored) {
                moveDelayTicks = DEFAULT_MOVE_TO_CHEST_DELAY_TICKS;
            }
        }
        if (params != null && params.has("normalizeDelayTo20Tps")) {
            try {
                normalizeDelayTo20Tps = params.get("normalizeDelayTo20Tps").getAsBoolean();
            } catch (Exception ignored) {
                normalizeDelayTo20Tps = true;
            }
        }

        for (int i = 0; i < clickPlan.size(); i++) {
            final int slotIndex = clickPlan.get(i);
            final int delayTicks = i * moveDelayTicks;
            ModUtils.DelayScheduler.instance.schedule(new Runnable() {
                @Override
                public void run() {
                    if (Minecraft.getMinecraft().player == null
                            || Minecraft.getMinecraft().currentScreen == null
                            || Minecraft.getMinecraft().player.openContainer == null
                            || Minecraft.getMinecraft().player.openContainer.windowId != windowId) {
                        markWarehouseTransferTaskFinished();
                        return;
                    }

                    Minecraft.getMinecraft().playerController.windowClick(
                            windowId,
                            slotIndex,
                            0,
                            ClickType.PICKUP,
                            Minecraft.getMinecraft().player);
                    markWarehouseTransferTaskFinished();
                }
            }, delayTicks, normalizeDelayTo20Tps);
        }
    }

    private static ContainerSlotGroups resolveContainerSlotGroups(Container container, EntityPlayerSP player) {
        if (container == null || player == null) {
            return new ContainerSlotGroups(Collections.<Integer>emptyList(), Collections.<Integer>emptyList());
        }

        List<Integer> containerSlots = new ArrayList<>();
        List<Integer> playerInventorySlots = new ArrayList<>();
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);
            if (slot == null) {
                continue;
            }
            if (slot.inventory == player.inventory && slot.getSlotIndex() >= 0 && slot.getSlotIndex() < 36) {
                playerInventorySlots.add(i);
            } else if (slot.inventory != player.inventory) {
                containerSlots.add(i);
            }
        }
        return new ContainerSlotGroups(containerSlots, playerInventorySlots);
    }

    private static List<Integer> buildMoveBetweenSlotGroupsClickPlan(Container container,
            List<Integer> sourceSlotIndices,
            List<Integer> selectedSourceSlots,
            List<Integer> targetSlotIndices,
            List<Integer> selectedTargetSlots,
            List<MoveChestFilterRule> filterRules,
            String requiredNbtTagMatchMode) {
        if (container == null) {
            return Collections.emptyList();
        }

        List<Integer> clickPlan = new ArrayList<>();
        ItemStack[] simulatedStacks = new ItemStack[container.inventorySlots.size()];
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            simulatedStacks[i] = slot == null || !slot.getHasStack() ? ItemStack.EMPTY : slot.getStack().copy();
        }

        for (int sourceIndex : selectedSourceSlots) {
            if (sourceIndex < 0 || sourceIndex >= sourceSlotIndices.size()) {
                continue;
            }

            int sourceContainerSlot = sourceSlotIndices.get(sourceIndex);
            if (sourceContainerSlot < 0 || sourceContainerSlot >= simulatedStacks.length) {
                continue;
            }

            ItemStack sourceStack = simulatedStacks[sourceContainerSlot];
            if (sourceStack == null || sourceStack.isEmpty()) {
                continue;
            }
            if (!matchesAnyMoveChestRule(sourceStack, filterRules, requiredNbtTagMatchMode)) {
                continue;
            }

            int remaining = sourceStack.getCount();
            List<Integer> targetChain = new ArrayList<>();
            Integer firstEmptyTargetSlot = null;

            for (int selectedTargetIndex : selectedTargetSlots) {
                if (selectedTargetIndex < 0 || selectedTargetIndex >= targetSlotIndices.size()) {
                    continue;
                }

                int targetSlot = targetSlotIndices.get(selectedTargetIndex);
                if (targetSlot < 0 || targetSlot >= simulatedStacks.length) {
                    continue;
                }

                ItemStack targetStack = simulatedStacks[targetSlot];
                if (targetStack == null || targetStack.isEmpty()) {
                    if (firstEmptyTargetSlot == null) {
                        firstEmptyTargetSlot = targetSlot;
                    }
                    continue;
                }

                if (!canMergeItemStacks(sourceStack, targetStack)) {
                    continue;
                }

                if (remaining <= 0) {
                    break;
                }

                int space = Math.max(0, targetStack.getMaxStackSize() - targetStack.getCount());
                if (space <= 0) {
                    continue;
                }

                int moved = Math.min(remaining, space);
                targetStack.setCount(targetStack.getCount() + moved);
                remaining -= moved;
                targetChain.add(targetSlot);
            }

            if (remaining > 0 && firstEmptyTargetSlot != null) {
                ItemStack placed = sourceStack.copy();
                placed.setCount(remaining);
                simulatedStacks[firstEmptyTargetSlot] = placed;
                remaining = 0;
                targetChain.add(firstEmptyTargetSlot);
            }

            if (targetChain.isEmpty()) {
                continue;
            }

            clickPlan.add(sourceContainerSlot);
            clickPlan.addAll(targetChain);

            if (remaining > 0) {
                simulatedStacks[sourceContainerSlot].setCount(remaining);
                clickPlan.add(sourceContainerSlot);
            } else {
                simulatedStacks[sourceContainerSlot] = ItemStack.EMPTY;
            }
        }

        return clickPlan;
    }

    private static boolean canMergeItemStacks(ItemStack sourceStack, ItemStack targetStack) {
        if (sourceStack == null || targetStack == null || sourceStack.isEmpty() || targetStack.isEmpty()) {
            return false;
        }
        return ItemStack.areItemsEqual(sourceStack, targetStack)
                && ItemStack.areItemStackTagsEqual(sourceStack, targetStack)
                && targetStack.isStackable();
    }

    public static boolean matchesRequiredNbtTags(ItemStack stack, List<String> requiredNbtTags,
            String requiredNbtTagMatchMode) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (requiredNbtTags == null || requiredNbtTags.isEmpty()) {
            return true;
        }

        boolean excludeMatches = NBT_TAG_MATCH_MODE_NOT_CONTAINS.equalsIgnoreCase(requiredNbtTagMatchMode);

        String searchableText = buildMoveChestMatchText(stack);
        if (searchableText.isEmpty()) {
            return excludeMatches;
        }

        for (String filter : requiredNbtTags) {
            String normalized = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty() && searchableText.contains(normalized)) {
                return !excludeMatches;
            }
        }
        return excludeMatches;
    }

    private static boolean matchesMoveChestItemName(ItemStack stack, String requiredItemName) {
        String expected = normalizeMoveChestItemName(requiredItemName);
        if (expected.isEmpty()) {
            return true;
        }
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return normalizeMoveChestItemName(stack.getDisplayName()).contains(expected);
    }

    private static boolean matchesAnyMoveChestRule(ItemStack stack, List<MoveChestFilterRule> rules,
            String requiredNbtTagMatchMode) {
        if (rules == null || rules.isEmpty()) {
            return false;
        }
        for (MoveChestFilterRule rule : rules) {
            if (rule == null || rule.isEmpty()) {
                continue;
            }
            if (!matchesMoveChestItemName(stack, rule.getItemName())) {
                continue;
            }
            if (!matchesRequiredNbtTags(stack, rule.getRequiredNbtTags(), requiredNbtTagMatchMode)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static String buildItemSearchableText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(128);
        appendMoveChestMatchText(builder, stack.getDisplayName());

        Item item = stack.getItem();
        if (item != null) {
            appendMoveChestMatchText(builder, item.getItemStackDisplayName(stack));
            ResourceLocation registryName = item.getRegistryName();
            if (registryName != null) {
                appendMoveChestMatchText(builder, registryName.toString());
            }
        }

        try {
            List<String> tooltip = stack.getTooltip(Minecraft.getMinecraft().player, ITooltipFlag.TooltipFlags.NORMAL);
            for (String line : tooltip) {
                appendMoveChestMatchText(builder, line);
            }
        } catch (Exception ignored) {
        }

        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound != null) {
            appendMoveChestMatchText(builder, tagCompound.toString());
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static String buildMoveChestMatchText(ItemStack stack) {
        return buildItemSearchableText(stack);
    }

    private static void appendMoveChestMatchText(StringBuilder builder, String text) {
        if (builder == null || text == null) {
            return;
        }

        String normalized = TextFormatting.getTextWithoutFormattingCodes(text);
        if (normalized == null) {
            normalized = text;
        }
        normalized = normalized.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.isEmpty()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(normalized);
    }

    private static String normalizeMoveChestItemName(String text) {
        if (text == null) {
            return "";
        }
        String normalized = TextFormatting.getTextWithoutFormattingCodes(text);
        if (normalized == null) {
            normalized = text;
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private static List<Integer> readSlotList(JsonObject params, String arrayKey, String textKey, int maxSlots) {
        Set<Integer> unique = new LinkedHashSet<>();
        if (params == null) {
            return new ArrayList<>();
        }

        if (params.has(arrayKey)) {
            JsonElement arrayElement = params.get(arrayKey);
            if (arrayElement != null && arrayElement.isJsonArray()) {
                JsonArray array = arrayElement.getAsJsonArray();
                for (JsonElement element : array) {
                    if (element == null || !element.isJsonPrimitive()) {
                        continue;
                    }
                    try {
                        int value = element.getAsInt();
                        if (value >= 0 && value < maxSlots) {
                            unique.add(value);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } else if (arrayElement != null && arrayElement.isJsonPrimitive()) {
                unique.addAll(parseIntegerText(arrayElement.getAsString(), maxSlots));
            }
        }

        if (unique.isEmpty() && params.has(textKey) && params.get(textKey).isJsonPrimitive()) {
            unique.addAll(parseIntegerText(params.get(textKey).getAsString(), maxSlots));
        }

        List<Integer> result = new ArrayList<>(unique);
        Collections.sort(result);
        return result;
    }

    private static List<Integer> parseIntegerText(String text, int maxSlots) {
        Set<Integer> unique = new LinkedHashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] tokens = text.split("[,\\r\\n\\s]+");
        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            try {
                int value = Integer.parseInt(token.trim());
                if (value >= 0 && value < maxSlots) {
                    unique.add(value);
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(unique);
    }

    public static List<String> readTagFilters(JsonObject params, String arrayKey, String textKey) {
        List<String> values = new ArrayList<>();
        if (params == null) {
            return values;
        }

        if (params.has(arrayKey)) {
            JsonElement arrayElement = params.get(arrayKey);
            if (arrayElement != null && arrayElement.isJsonArray()) {
                for (JsonElement element : arrayElement.getAsJsonArray()) {
                    if (element != null && element.isJsonPrimitive()) {
                        addTagFilter(values, element.getAsString());
                    }
                }
            } else if (arrayElement != null && arrayElement.isJsonPrimitive()) {
                for (String token : arrayElement.getAsString().split("\\r?\\n|,")) {
                    addTagFilter(values, token);
                }
            }
        }

        if (values.isEmpty() && params.has(textKey) && params.get(textKey).isJsonPrimitive()) {
            for (String token : params.get(textKey).getAsString().split("\\r?\\n|,")) {
                addTagFilter(values, token);
            }
        }

        return values;
    }

    public static String readRequiredNbtTagMatchMode(JsonObject params) {
        if (params == null || !params.has("requiredNbtTagsMode") || !params.get("requiredNbtTagsMode").isJsonPrimitive()) {
            return NBT_TAG_MATCH_MODE_CONTAINS;
        }

        try {
            String mode = params.get("requiredNbtTagsMode").getAsString();
            return NBT_TAG_MATCH_MODE_NOT_CONTAINS.equalsIgnoreCase(mode)
                    ? NBT_TAG_MATCH_MODE_NOT_CONTAINS
                    : NBT_TAG_MATCH_MODE_CONTAINS;
        } catch (Exception ignored) {
            return NBT_TAG_MATCH_MODE_CONTAINS;
        }
    }

    public static String readMoveChestDirection(JsonObject params) {
        if (params == null || !params.has("moveDirection") || !params.get("moveDirection").isJsonPrimitive()) {
            return MOVE_DIRECTION_INVENTORY_TO_CHEST;
        }

        try {
            String direction = params.get("moveDirection").getAsString();
            return MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(direction)
                    ? MOVE_DIRECTION_CHEST_TO_INVENTORY
                    : MOVE_DIRECTION_INVENTORY_TO_CHEST;
        } catch (Exception ignored) {
            return MOVE_DIRECTION_INVENTORY_TO_CHEST;
        }
    }

    public static String readMoveChestItemName(JsonObject params) {
        if (params == null || !params.has("itemName") || !params.get("itemName").isJsonPrimitive()) {
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

    private static void addTagFilter(List<String> values, String raw) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isEmpty()) {
            return;
        }
        for (String existing : values) {
            if (existing.equalsIgnoreCase(normalized)) {
                return;
            }
        }
        values.add(normalized);
    }
}

