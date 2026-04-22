package com.zszl.zszlScriptMod.handlers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.dungeon.ChestData;
import com.zszl.zszlScriptMod.system.dungeon.Warehouse;
import com.zszl.zszlScriptMod.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WarehouseEventHandler {

    public static final WarehouseEventHandler INSTANCE = new WarehouseEventHandler();
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int AUTO_DEPOSIT_INTERVAL_TICKS = 2;
    private static final int CHEST_PANEL_WIDTH = 120;

    public static boolean oneClickDepositMode = false;

    private static final List<BlockPos> chestsToHighlight = new CopyOnWriteArrayList<>();
    private static final Set<String> playerItemKeys = new HashSet<>();
    private static final Deque<BlockPos> autoDepositRouteQueue = new ArrayDeque<>();

    private static boolean autoDepositRouteRunning = false;
    private static BlockPos autoDepositCurrentTarget;
    private static int autoDepositOpenWaitTicks;
    private static int autoDepositCooldown;

    private static ChestData currentOpenChestData;
    private static BlockPos currentOpenChestPos;

    private static int designatedItemScrollOffset;
    private static int maxDesignatedItemScroll;
    private static boolean isDraggingDesignatedScrollbar;

    private static GuiButton autoDepositToggleButton;

    private WarehouseEventHandler() {
    }

    public static void startAutoDepositByHighlights() {
        if (MC.player == null || MC.level == null) {
            return;
        }
        if (autoDepositRouteRunning) {
            MC.player.sendSystemMessage(Component.literal("§e[仓库] 自动存入流程已在运行中。"));
            return;
        }

        WarehouseManager.updateCurrentWarehouse();
        if (WarehouseManager.currentWarehouse == null) {
            MC.player.sendSystemMessage(Component.literal("§c[仓库] 当前不在激活仓库区域内。"));
            return;
        }

        oneClickDepositMode = true;
        INSTANCE.updatePlayerItemKeys();
        INSTANCE.updateHighlightList();

        if (playerItemKeys.isEmpty()) {
            MC.player.sendSystemMessage(Component.literal("§e[仓库] 背包中没有可自动存入的目标物品。"));
            return;
        }
        if (chestsToHighlight.isEmpty()) {
            MC.player.sendSystemMessage(Component.literal("§e[仓库] 没有可用的高亮箱子。"));
            return;
        }

        List<BlockPos> sorted = new ArrayList<>(chestsToHighlight);
        sorted.sort((a, b) -> Double.compare(
                MC.player.distanceToSqr(a.getX() + 0.5D, a.getY() + 0.5D, a.getZ() + 0.5D),
                MC.player.distanceToSqr(b.getX() + 0.5D, b.getY() + 0.5D, b.getZ() + 0.5D)));

        autoDepositRouteQueue.clear();
        autoDepositRouteQueue.addAll(sorted);
        autoDepositRouteRunning = true;
        autoDepositCurrentTarget = null;
        autoDepositOpenWaitTicks = 0;

        MC.player.sendSystemMessage(Component.literal("§a[仓库] 已启动自动存入流程，目标箱子数: " + sorted.size()));
        INSTANCE.startNextAutoDepositTarget();
    }

    public static boolean isAutoDepositRouteRunning() {
        return autoDepositRouteRunning;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MC.player == null || MC.level == null) {
            return;
        }

        if (MC.player.tickCount % 20 == 0) {
            WarehouseManager.updateCurrentWarehouse();
        }

        if (oneClickDepositMode && WarehouseManager.currentWarehouse != null) {
            if (MC.player.tickCount % 5 == 0) {
                updatePlayerItemKeys();
                updateHighlightList();
            }
        } else {
            chestsToHighlight.clear();
        }

        if (autoDepositCooldown > 0) {
            autoDepositCooldown--;
        }

        if (currentOpenChestData != null && currentOpenChestData.autoDepositEnabled
                && MC.player.containerMenu instanceof ChestMenu chestMenu) {
            if (autoDepositCooldown <= 0) {
                executeAutoDeposit(chestMenu);
                autoDepositCooldown = Math.max(0, AUTO_DEPOSIT_INTERVAL_TICKS - 1);
            }
        }

        if (!autoDepositRouteRunning) {
            return;
        }

        if (autoDepositCurrentTarget != null && !(MC.player.containerMenu instanceof ChestMenu)) {
            autoDepositOpenWaitTicks++;
            if (autoDepositOpenWaitTicks > 200) {
                if (MC.player != null) {
                    MC.player.sendSystemMessage(Component.literal("§e[仓库] 打开箱子超时，尝试下一个目标。"));
                }
                startNextAutoDepositTarget();
            }
        }

        if (autoDepositCurrentTarget != null && MC.player.containerMenu instanceof ChestMenu chestMenu) {
            if (currentOpenChestData != null && !hasDepositableItemsInOpenContainer(chestMenu)) {
                MC.setScreen(null);
                autoDepositCurrentTarget = null;
                autoDepositOpenWaitTicks = 0;
                ModUtils.DelayScheduler.init();
                ModUtils.DelayScheduler.instance.schedule(this::startNextAutoDepositTarget, 6);
            }
        }
    }

    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        currentOpenChestData = null;
        currentOpenChestPos = null;
        designatedItemScrollOffset = 0;
        isDraggingDesignatedScrollbar = false;

        if (event == null || !(event.getNewScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }

        BlockPos chestPos = resolveOpenedChestPos();
        if (chestPos == null) {
            return;
        }

        BlockPos finalChestPos = chestPos.immutable();
        ModUtils.DelayScheduler.init();
        ModUtils.DelayScheduler.instance.schedule(() -> initializeOpenChest(finalChestPos), 4);
    }

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!isWarehouseChestScreen(event == null ? null : event.getScreen()) || currentOpenChestData == null) {
            return;
        }

        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }

        if (isDraggingDesignatedScrollbar) {
            if (!com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse.isButtonDown(0)) {
                isDraggingDesignatedScrollbar = false;
            } else {
                updateScrollbarFromMouse(screen, event.getMouseY());
            }
        }

        GuiGraphics graphics = event.getGuiGraphics();
        GuiCompatContext.push(graphics);
        try {
            renderChestSidePanel(screen, graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        } finally {
            GuiCompatContext.clear();
        }
    }

    @SubscribeEvent
    public void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        Screen screen = MC.screen;
        if (!isWarehouseChestScreen(screen) || currentOpenChestData == null || event == null || event.getButton() != 0) {
            return;
        }

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();

        if (event.getAction() != 0) {
            if (autoDepositToggleButton != null && autoDepositToggleButton.mousePressed(MC, mouseX, mouseY)) {
                currentOpenChestData.autoDepositEnabled = !currentOpenChestData.autoDepositEnabled;
                WarehouseManager.saveWarehouses();
                event.setCanceled(true);
                return;
            }

            if (isInsideDesignatedScrollbar(screen, mouseX, mouseY)) {
                isDraggingDesignatedScrollbar = true;
                updateScrollbarFromMouse((AbstractContainerScreen<?>) screen, mouseY);
                event.setCanceled(true);
            }
            return;
        }

        isDraggingDesignatedScrollbar = false;
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Screen screen = MC.screen;
        if (!isWarehouseChestScreen(screen) || currentOpenChestData == null || event == null) {
            return;
        }

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();
        if (!isInsideDesignatedList(screen, mouseX, mouseY)) {
            return;
        }

        double delta = event.getScrollDelta();
        if (Math.abs(delta) < 0.0001D) {
            return;
        }

        if (delta > 0.0D) {
            designatedItemScrollOffset = Math.max(0, designatedItemScrollOffset - 1);
        } else {
            designatedItemScrollOffset = Math.min(maxDesignatedItemScroll, designatedItemScrollOffset + 1);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        RenderWorldLastEvent.WorldRenderContext renderContext = event.getWorldRenderContext();
        if (chestsToHighlight.isEmpty() || renderContext == null) {
            return;
        }

        PoseStack poseStack = event.createWorldPoseStack();
        if (poseStack == null) {
            return;
        }

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        for (BlockPos pos : chestsToHighlight) {
            drawChestOutline(buffer, poseStack, renderContext, pos);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void initializeOpenChest(BlockPos chestPos) {
        if (chestPos == null || MC.player == null || !(MC.player.containerMenu instanceof ChestMenu chestMenu)
                || !(MC.screen instanceof AbstractContainerScreen<?>)) {
            return;
        }

        Warehouse targetWarehouse = WarehouseManager.findWarehouseForPos(chestPos);
        if (targetWarehouse == null) {
            WarehouseManager.updateCurrentWarehouse();
            targetWarehouse = WarehouseManager.currentWarehouse;
        }
        if (targetWarehouse == null) {
            return;
        }

        currentOpenChestPos = chestPos.immutable();
        WarehouseManager.scanChest(chestMenu, chestPos);
        currentOpenChestData = targetWarehouse.getChestAt(chestPos);
        if (currentOpenChestData == null) {
            return;
        }

        updateDesignatedItems(currentOpenChestData, chestMenu);
        if (oneClickDepositMode || autoDepositRouteRunning) {
            currentOpenChestData.autoDepositEnabled = true;
            WarehouseManager.saveWarehouses();
        }
    }

    private void renderChestSidePanel(AbstractContainerScreen<?> screen, GuiGraphics graphics,
            int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = new FontRenderer(MC.font);
        int panelX = screen.getGuiLeft() - CHEST_PANEL_WIDTH - 5;
        int panelY = 0;
        int panelHeight = screen.height;

        Gui.drawRect(panelX, panelY, panelX + CHEST_PANEL_WIDTH, panelY + panelHeight, 0xC0000000);

        int itemHeight = 15;
        int designatedListY = panelY + 5;
        int designatedListHeight = panelHeight / 2 - 10;
        font.drawString("§e指定存放物品:", panelX + 5, designatedListY, 0xFFFFFF);
        Gui.drawRect(panelX + 5, designatedListY + 15, panelX + CHEST_PANEL_WIDTH - 5,
                designatedListY + 15 + designatedListHeight, 0x80000000);

        List<String> items = new ArrayList<>(currentOpenChestData.designatedItems);
        int visibleDesignatedItems = Math.max(1, designatedListHeight / itemHeight);
        maxDesignatedItemScroll = Math.max(0, items.size() - visibleDesignatedItems);
        designatedItemScrollOffset = Math.max(0, Math.min(designatedItemScrollOffset, maxDesignatedItemScroll));

        for (int i = 0; i < visibleDesignatedItems; i++) {
            int index = i + designatedItemScrollOffset;
            if (index >= items.size()) {
                break;
            }
            font.drawString("§f- " + items.get(index), panelX + 8, designatedListY + 17 + i * itemHeight, 0xFFFFFF);
        }

        if (maxDesignatedItemScroll > 0) {
            int scrollbarX = panelX + CHEST_PANEL_WIDTH - 9;
            int listTop = designatedListY + 15;
            Gui.drawRect(scrollbarX, listTop, scrollbarX + 4, listTop + designatedListHeight, 0xFF101010);
            int thumbHeight = Math.max(10,
                    (int) ((float) visibleDesignatedItems / Math.max(1, items.size()) * designatedListHeight));
            int thumbY = listTop + (int) ((float) designatedItemScrollOffset / Math.max(1, maxDesignatedItemScroll)
                    * (designatedListHeight - thumbHeight));
            Gui.drawRect(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFF888888);
        }

        int functionPanelY = designatedListY + designatedListHeight + 20;
        int functionPanelHeight = panelHeight - functionPanelY - 5;
        font.drawString("§e自动存入功能:", panelX + 5, functionPanelY, 0xFFFFFF);

        int functionListY = functionPanelY + 15;
        Gui.drawRect(panelX + 5, functionListY, panelX + CHEST_PANEL_WIDTH - 5,
                functionListY + functionPanelHeight - 20, 0x80000000);

        int buttonY = functionListY + 45;
        autoDepositToggleButton = new GuiButton(9010, panelX + 5, buttonY, CHEST_PANEL_WIDTH - 10, 20,
                "自动存入: " + (currentOpenChestData.autoDepositEnabled ? "§a开" : "§c关"));
        autoDepositToggleButton.drawButton(MC, mouseX, mouseY, partialTicks);

        String status = currentOpenChestData.autoDepositEnabled ? "§a开" : "§c关";
        String route = autoDepositRouteRunning ? "§a运行中" : "§7未运行";
        font.drawString("§f状态: 自动存入 " + status, panelX + 8, panelY + panelHeight - 22, 0xFFFFFF);
        font.drawString("§f流程: " + route, panelX + 8, panelY + panelHeight - 10, 0xFFFFFF);

        if (autoDepositToggleButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§7打开此箱子后自动存入"));
            tooltip.add(Component.literal("§7匹配“指定存放物品”的背包物品。"));
            graphics.renderComponentTooltip(MC.font, tooltip, mouseX, mouseY);
        }
    }

    private void startNextAutoDepositTarget() {
        if (!autoDepositRouteRunning || MC.player == null) {
            return;
        }

        updatePlayerItemKeys();
        if (playerItemKeys.isEmpty()) {
            stopAutoDepositRoute("§a[仓库] 背包目标物品已全部存入，流程结束。");
            return;
        }

        while (!autoDepositRouteQueue.isEmpty()) {
            BlockPos next = autoDepositRouteQueue.pollFirst();
            Warehouse warehouse = WarehouseManager.findWarehouseForPos(next);
            ChestData chestData = warehouse == null ? null : warehouse.getChestAt(next);
            if (chestData == null || !chestData.hasBeenScanned) {
                continue;
            }
            if (!hasAnyDepositableForChest(chestData)) {
                continue;
            }

            autoDepositCurrentTarget = next.immutable();
            autoDepositOpenWaitTicks = 0;
            GoToAndOpenHandler.start(next);
            MC.player.sendSystemMessage(Component.literal("§b[仓库] 前往箱子: " + next));
            return;
        }

        stopAutoDepositRoute("§a[仓库] 已遍历所有高亮箱子，自动存入流程结束。");
    }

    private void stopAutoDepositRoute(String reason) {
        if (MC.player != null && reason != null && !reason.isEmpty()) {
            MC.player.sendSystemMessage(Component.literal(reason));
        }
        autoDepositRouteRunning = false;
        autoDepositCurrentTarget = null;
        autoDepositOpenWaitTicks = 0;
        autoDepositRouteQueue.clear();
    }

    private void updatePlayerItemKeys() {
        playerItemKeys.clear();
        if (MC.player == null) {
            return;
        }
        for (ItemStack stack : MC.player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            collectStackKeys(stack, playerItemKeys);
        }
    }

    private void updateHighlightList() {
        if (ModConfig.isDebugFlagEnabled(DebugModule.WAREHOUSE_ANALYSIS)) {
            if (WarehouseManager.currentWarehouse == null) {
                return;
            }
            if (playerItemKeys.isEmpty()) {
                return;
            }
        }

        chestsToHighlight.clear();
        Warehouse warehouse = WarehouseManager.currentWarehouse;
        if (warehouse == null || warehouse.chests == null || playerItemKeys.isEmpty()) {
            return;
        }

        for (ChestData chest : warehouse.chests) {
            if (chest == null || !chest.hasBeenScanned || chest.pos == null) {
                continue;
            }
            if (hasAnyDepositableForChest(chest)) {
                chestsToHighlight.add(chest.pos.immutable());
            }
        }
    }

    private boolean hasAnyDepositableForChest(ChestData chestData) {
        if (chestData == null || playerItemKeys.isEmpty()) {
            return false;
        }

        for (ItemStack chestItem : chestData.getSnapshotContents(54)) {
            if (chestItem.isEmpty()) {
                continue;
            }
            if (playerItemKeys.contains(getUniqueItemKey(chestItem))) {
                return true;
            }
        }
        return false;
    }

    private void updateDesignatedItems(ChestData chest, ChestMenu chestMenu) {
        if (chest == null || chestMenu == null) {
            return;
        }

        Set<String> foundItems = new HashSet<>();
        int containerSlots = chestMenu.getRowCount() * 9;
        for (int i = 0; i < containerSlots && i < chestMenu.slots.size(); i++) {
            ItemStack stack = chestMenu.slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }

            NonNullList<ItemStack> shulkerItems = getShulkerItems(stack);
            if (shulkerItems != null) {
                for (ItemStack shulkerItem : shulkerItems) {
                    if (!shulkerItem.isEmpty()) {
                        foundItems.add(shulkerItem.getHoverName().getString());
                    }
                }
            } else {
                foundItems.add(stack.getHoverName().getString());
            }
        }

        chest.designatedItems.clear();
        chest.designatedItems.addAll(foundItems);
        WarehouseManager.saveWarehouses();
    }

    private boolean hasDepositableItemsInOpenContainer(ChestMenu chestMenu) {
        if (MC.player == null || currentOpenChestData == null || currentOpenChestData.designatedItems.isEmpty()) {
            return false;
        }

        int containerSlots = chestMenu.getRowCount() * 9;
        for (int slotIndex = containerSlots; slotIndex < chestMenu.slots.size(); slotIndex++) {
            ItemStack playerStack = chestMenu.slots.get(slotIndex).getItem();
            if (playerStack.isEmpty()) {
                continue;
            }
            if (shouldDepositStack(playerStack)) {
                return true;
            }
        }
        return false;
    }

    private void executeAutoDeposit(ChestMenu chestMenu) {
        if (MC.player == null || MC.gameMode == null || currentOpenChestData == null
                || currentOpenChestData.designatedItems.isEmpty()) {
            return;
        }

        int containerSlots = chestMenu.getRowCount() * 9;
        for (int slotIndex = containerSlots; slotIndex < chestMenu.slots.size(); slotIndex++) {
            ItemStack playerStack = chestMenu.slots.get(slotIndex).getItem();
            if (playerStack.isEmpty()) {
                continue;
            }
            if (!shouldDepositStack(playerStack)) {
                continue;
            }

            MC.gameMode.handleInventoryMouseClick(chestMenu.containerId, slotIndex, 0, ClickType.QUICK_MOVE, MC.player);
            autoDepositCooldown = Math.max(0, AUTO_DEPOSIT_INTERVAL_TICKS - 1);
            return;
        }
    }

    private boolean shouldDepositStack(ItemStack stack) {
        if (stack.isEmpty() || currentOpenChestData == null || currentOpenChestData.designatedItems.isEmpty()) {
            return false;
        }

        NonNullList<ItemStack> shulkerItems = getShulkerItems(stack);
        if (shulkerItems != null) {
            for (ItemStack shulkerItem : shulkerItems) {
                if (!shulkerItem.isEmpty()
                        && currentOpenChestData.designatedItems.contains(shulkerItem.getHoverName().getString())) {
                    return true;
                }
            }
            return false;
        }

        return currentOpenChestData.designatedItems.contains(stack.getHoverName().getString());
    }

    private void collectStackKeys(ItemStack stack, Set<String> keys) {
        if (stack.isEmpty() || keys == null) {
            return;
        }

        NonNullList<ItemStack> shulkerItems = getShulkerItems(stack);
        if (shulkerItems != null) {
            for (ItemStack shulkerItem : shulkerItems) {
                if (!shulkerItem.isEmpty()) {
                    keys.add(getUniqueItemKey(shulkerItem));
                }
            }
            return;
        }

        keys.add(getUniqueItemKey(stack));
    }

    private NonNullList<ItemStack> getShulkerItems(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }

        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null || !blockEntityTag.contains("Items", 9)) {
            return null;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        com.zszl.zszlScriptMod.compat.legacy.net.minecraft.inventory.ItemStackHelper.loadAllItems(blockEntityTag, items);
        return items;
    }

    private BlockPos resolveOpenedChestPos() {
        BlockPos target = GoToAndOpenHandler.getTargetChestPos();
        if (target != null) {
            return target;
        }

        HitResult hit = MC.hitResult;
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity blockEntity = MC.level == null ? null : MC.level.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity) {
                return pos;
            }
        }
        return null;
    }

    private void drawChestOutline(BufferBuilder buffer, PoseStack poseStack,
            RenderWorldLastEvent.WorldRenderContext renderContext, BlockPos pos) {
        Vec3 min = renderContext.toCameraSpace(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        float x1 = (float) min.x;
        float y1 = (float) min.y;
        float z1 = (float) min.z;
        float x2 = x1 + 1.0F;
        float y2 = y1 + 1.0F;
        float z2 = z1 + 1.0F;

        buffer.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(poseStack.last().pose(), x1, y1, z1).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x2, y1, z1).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x2, y1, z2).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x1, y1, z2).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x1, y1, z1).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x1, y2, z1).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x2, y2, z1).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x2, y2, z2).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x1, y2, z2).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        buffer.vertex(poseStack.last().pose(), x1, y2, z1).color(1.0F, 0.85F, 0.25F, 0.95F).endVertex();
        Tesselator.getInstance().end();
    }

    private String getUniqueItemKey(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                + "@"
                + stack.getDamageValue()
                + ":"
                + (stack.hasTag() ? stack.getTag() : "");
    }

    private boolean isWarehouseChestScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>
                && MC.player != null
                && MC.player.containerMenu instanceof ChestMenu
                && currentOpenChestPos != null;
    }

    private boolean isInsideDesignatedList(Screen screen, int mouseX, int mouseY) {
        if (!(screen instanceof AbstractContainerScreen<?> chestScreen)) {
            return false;
        }

        int panelX = chestScreen.getGuiLeft() - CHEST_PANEL_WIDTH - 5;
        int designatedListY = 5;
        int designatedListHeight = chestScreen.height / 2 - 10;
        int designatedListTop = designatedListY + 15;
        return mouseX >= panelX
                && mouseX < panelX + CHEST_PANEL_WIDTH
                && mouseY >= designatedListTop
                && mouseY < designatedListTop + designatedListHeight;
    }

    private boolean isInsideDesignatedScrollbar(Screen screen, int mouseX, int mouseY) {
        if (!(screen instanceof AbstractContainerScreen<?> chestScreen)) {
            return false;
        }

        int panelX = chestScreen.getGuiLeft() - CHEST_PANEL_WIDTH - 5;
        int designatedListY = 5;
        int designatedListHeight = chestScreen.height / 2 - 10;
        int designatedListTop = designatedListY + 15;
        int scrollbarX = panelX + CHEST_PANEL_WIDTH - 9;
        return mouseX >= scrollbarX
                && mouseX < scrollbarX + 4
                && mouseY >= designatedListTop
                && mouseY < designatedListTop + designatedListHeight;
    }

    private void updateScrollbarFromMouse(AbstractContainerScreen<?> screen, int mouseY) {
        int designatedListHeight = screen.height / 2 - 10;
        int listTop = 20;
        float percent = (float) (mouseY - listTop) / Math.max(1.0F, designatedListHeight);
        designatedItemScrollOffset = (int) (percent * (maxDesignatedItemScroll + 1));
        designatedItemScrollOffset = Math.max(0, Math.min(maxDesignatedItemScroll, designatedItemScrollOffset));
    }

    private int getScaledMouseX() {
        ScaledResolution resolution = new ScaledResolution(MC);
        return (int) Math.round(com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse.getX()
                * resolution.getScaledWidth()
                / (double) Math.max(1, MC.getWindow().getScreenWidth()));
    }

    private int getScaledMouseY() {
        ScaledResolution resolution = new ScaledResolution(MC);
        return (int) Math.round(com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse.getY()
                * resolution.getScaledHeight()
                / (double) Math.max(1, MC.getWindow().getScreenHeight()));
    }
}
