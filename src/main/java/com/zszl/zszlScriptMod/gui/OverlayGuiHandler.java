// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/OverlayGuiHandler.java
package com.zszl.zszlScriptMod.gui;

import java.awt.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui; // 确保导入 Gui 类
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.packet.InputTimelineManager;
import com.zszl.zszlScriptMod.handlers.DungeonWarehouseHandler;
import com.zszl.zszlScriptMod.handlers.QuickExchangeHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class OverlayGuiHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    // --- 快速兑换控件 ---
    private static ToggleGuiButton quickExchangeShiftClickButton;
    private static ToggleGuiButton quickExchangeCtrlClickButton;
    private static GuiTextField quickExchangeIntervalField;
    private static GuiTextField quickExchangeAmountField;
    private static int exchangeAmount = 1;

    // --- 副本仓库控件 ---
    private static ToggleGuiButton dungeonWarehouseShiftClickButton;
    private static ToggleGuiButton dungeonWarehouseCtrlClickButton;
    private static GuiTextField dungeonWarehouseIntervalField;
    private static GuiTextField dungeonWarehouseAmountField;
    private static int dungeonWarehouseAmount = 1;

    private static IInventory lastCheckedChestInventory = null;
    private static ItemStack ghostClipboardStack = ItemStack.EMPTY;

    public static void resetLastCheckedChest() {
        lastCheckedChestInventory = null;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (mc.currentScreen == null && !zszlScriptMod.isGuiVisible) {
            drawMasterStatusHud(false);
        }

        if (zszlScriptMod.isGuiVisible && mc.currentScreen == null) {
            if (GuiInventory.isMasterStatusHudEditMode()) {
                drawMasterStatusHud(true);
            }
            // 鼠标拖动逻辑也需要在这里处理，因为它依赖于每一帧的鼠标位置
            if (GuiInventory.isAnyDragActive() && Mouse.isButtonDown(0)) {
                ScaledResolution res = new ScaledResolution(mc);
                int mouseX = Mouse.getX() * res.getScaledWidth() / mc.displayWidth;
                int mouseY = res.getScaledHeight() - Mouse.getY() * res.getScaledHeight() / mc.displayHeight - 1;
                GuiInventory.handleMouseDrag(mouseX, mouseY);
            } else if (GuiInventory.isAnyDragActive()) {
                // 如果鼠标松开，但我们还在拖动状态，则释放它
                GuiInventory.handleMouseRelease(Mouse.getX(), Mouse.getY(), 0);
            }

            // 绘制主UI覆盖层
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            GuiInventory.drawOverlay(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        }

        if (mc.currentScreen == null) {
            drawMouseCoordinateDebugHud();
        }
    }

    public static void renderMasterStatusHudPreview() {
        drawMasterStatusHud(true);
    }

    private static void drawMasterStatusHud(boolean editingPreview) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRenderer == null) {
            GuiInventory.updateMasterStatusHudEditorBounds(null, null);
            return;
        }
        List<String> lines = buildMasterStatusHudLines(editingPreview);
        if (lines.isEmpty()) {
            GuiInventory.updateMasterStatusHudEditorBounds(null, null);
            return;
        }

        int baseX = Math.max(0, MovementFeatureManager.getMasterStatusHudX());
        int baseY = Math.max(0, MovementFeatureManager.getMasterStatusHudY());
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, mc.fontRenderer.getStringWidth(line));
        }
        int lineHeight = 10;
        int panelX = Math.max(0, baseX - 4);
        int panelY = Math.max(0, baseY - 4);
        int panelWidth = Math.max(120, maxWidth + 8);
        int panelHeight = lines.size() * lineHeight + 8;
        Rectangle hudBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        Rectangle exitBounds = null;

        if (editingPreview) {
            panelHeight += 22;
            hudBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);
            Gui.drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x7A0F1720);
            Gui.drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF63C7FF);
            Gui.drawRect(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF35536C);
            mc.fontRenderer.drawStringWithShadow("拖动此区域调整 HUD 位置", panelX + 5, panelY + panelHeight - 18,
                    0xFFEAF6FF);
            int exitWidth = 44;
            int exitHeight = 14;
            int exitX = panelX + panelWidth - exitWidth - 4;
            int exitY = panelY + panelHeight - exitHeight - 4;
            exitBounds = new Rectangle(exitX, exitY, exitWidth, exitHeight);
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            int hoverMouseX = Mouse.getX() * scaledResolution.getScaledWidth() / mc.displayWidth;
            int hoverMouseY = scaledResolution.getScaledHeight()
                    - Mouse.getY() * scaledResolution.getScaledHeight() / mc.displayHeight - 1;
            boolean hovered = exitBounds.contains(hoverMouseX, hoverMouseY);
            GuiTheme.drawButtonFrame(exitX, exitY, exitWidth, exitHeight,
                    hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            mc.fontRenderer.drawStringWithShadow("退出编辑", exitX + 6, exitY + 3, 0xFFFFFFFF);
        }

        int drawY = baseY;
        for (String line : lines) {
            mc.fontRenderer.drawStringWithShadow(line, baseX, drawY, 0xFFFFFF);
            drawY += lineHeight;
        }

        GuiInventory.updateMasterStatusHudEditorBounds(hudBounds, exitBounds);
    }

    private static List<String> buildMasterStatusHudLines(boolean editingPreview) {
        List<String> lines = new ArrayList<>();
        lines.addAll(editingPreview ? SpeedHandler.getStatusLines(true) : SpeedHandler.getStatusLines());
        lines.addAll(editingPreview ? MovementFeatureManager.getStatusLines(true) : MovementFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? BlockFeatureManager.getStatusLines(true) : BlockFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? WorldFeatureManager.getStatusLines(true) : WorldFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? ItemFeatureManager.getStatusLines(true) : ItemFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? MiscFeatureManager.getStatusLines(true) : MiscFeatureManager.getStatusLines());
        if (!editingPreview || !lines.isEmpty()) {
            return lines;
        }
        lines.add("§a[总状态HUD] §f位置预览");
        lines.add("§7当前没有可显示的状态行");
        lines.add("§7拖动后将保存新的 HUD 位置");
        return lines;
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (ModConfig.showHoverInfo && !zszlScriptMod.isGuiVisible) {
            GuiScreen gui = event.getGui();
            if (gui == null) {
                return;
            }
            int mouseX = event.getMouseX();
            int mouseY = event.getMouseY();
            List<String> tooltipLines = new ArrayList<>();
            boolean infoFound = false;

            if (gui instanceof GuiContainer) {
                GuiContainer guiContainer = (GuiContainer) gui;
                Slot slot = guiContainer.getSlotUnderMouse();
                if (slot != null && slot.getHasStack()) {
                    ItemStack stack = slot.getStack();
                    tooltipLines.add(stack.getDisplayName());
                    Object registryName = Item.REGISTRY.getNameForObject(stack.getItem());
                    tooltipLines.add(TextFormatting.DARK_GRAY
                            + (registryName == null ? "unknown:item" : registryName.toString()));
                    tooltipLines.add("---");
                    tooltipLines.add(TextFormatting.AQUA + I18n.format("gui.overlay.container_slot_id")
                            + TextFormatting.WHITE + slot.slotNumber);
                    tooltipLines
                            .add(TextFormatting.YELLOW + I18n.format("gui.overlay.inventory_slot_id")
                                    + TextFormatting.WHITE + slot.getSlotIndex());
                    if (stack.hasTagCompound()) {
                        tooltipLines.add(TextFormatting.GOLD + "NBT: " + TextFormatting.WHITE
                                + String.valueOf(stack.getTagCompound()));
                    }
                    infoFound = true;
                }
            }

            if (!infoFound) {
                GuiButton hoveredButton = findHoveredButton(gui);
                if (hoveredButton != null) {
                    tooltipLines.add(I18n.format("gui.overlay.button_label", hoveredButton.displayString));
                    tooltipLines.add("---");
                    tooltipLines.add(I18n.format("gui.overlay.button_id", hoveredButton.id));
                    infoFound = true;
                }
            }

            if (!infoFound) {
                tooltipLines.add(I18n.format("gui.overlay.none"));
            }
            gui.drawHoveringText(tooltipLines, mouseX, mouseY);
        }

        if (event.getGui() instanceof GuiChest) {
            GuiChest gui = (GuiChest) event.getGui();

            int guiYSize = 166;
            try {
                if (gui.inventorySlots instanceof ContainerChest) {
                    ContainerChest containerChest = (ContainerChest) gui.inventorySlots;
                    IInventory lower = containerChest.getLowerChestInventory();
                    int rows = Math.max(1, lower.getSizeInventory() / 9);
                    guiYSize = 114 + rows * 18;
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("Failed to calc chest guiYSize, using default", e);
            }
            int guiLeft = (gui.width - 176) / 2;
            int guiTop = (gui.height - guiYSize) / 2;

            if (QuickExchangeHandler.isQuickExchangeGui(gui)) {
                int panelWidth = 130;
                int panelX = guiLeft - panelWidth - 5;
                int panelY = guiTop;
                int panelHeight = 140;

                // !! 修复：使用 Gui.drawRect 静态方法
                Gui.drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);
                mc.fontRenderer.drawStringWithShadow(I18n.format("gui.quick_exchange.title"), panelX + 7, panelY + 8,
                        0xFFFFFF);

                int currentY = panelY + 25;
                int controlWidth = panelWidth - 14;
                int controlX = panelX + 7;

                if (quickExchangeShiftClickButton == null) {
                    quickExchangeShiftClickButton = new ToggleGuiButton(9001, 0, 0, 0, 0, "",
                            QuickExchangeHandler.settings.shiftClickEnabled);
                }
                quickExchangeShiftClickButton.x = controlX;
                quickExchangeShiftClickButton.y = currentY;
                quickExchangeShiftClickButton.width = controlWidth;
                quickExchangeShiftClickButton.height = 20;
                quickExchangeShiftClickButton.displayString = I18n.format("gui.quick_exchange.shift_click") + ": "
                        + (QuickExchangeHandler.settings.shiftClickEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"));
                quickExchangeShiftClickButton.setEnabledState(QuickExchangeHandler.settings.shiftClickEnabled);
                quickExchangeShiftClickButton.drawButton(mc, event.getMouseX(), event.getMouseY(),
                        event.getRenderPartialTicks());
                currentY += 22;

                if (quickExchangeCtrlClickButton == null) {
                    quickExchangeCtrlClickButton = new ToggleGuiButton(9002, 0, 0, 0, 0, "",
                            QuickExchangeHandler.settings.ctrlClickEnabled);
                }
                quickExchangeCtrlClickButton.x = controlX;
                quickExchangeCtrlClickButton.y = currentY;
                quickExchangeCtrlClickButton.width = controlWidth;
                quickExchangeCtrlClickButton.height = 20;
                quickExchangeCtrlClickButton.displayString = I18n.format("gui.quick_exchange.ctrl_click") + ": "
                        + (QuickExchangeHandler.settings.ctrlClickEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"));
                quickExchangeCtrlClickButton.setEnabledState(QuickExchangeHandler.settings.ctrlClickEnabled);
                quickExchangeCtrlClickButton.drawButton(mc, event.getMouseX(), event.getMouseY(),
                        event.getRenderPartialTicks());
                currentY += 22;

                mc.fontRenderer.drawStringWithShadow(I18n.format("gui.quick_exchange.click_interval"), controlX,
                        currentY + 6, 0xFFFFFF);
                if (quickExchangeIntervalField == null) {
                    quickExchangeIntervalField = new GuiTextField(9003, mc.fontRenderer, 0, 0, 50, 20);
                    quickExchangeIntervalField.setText(String.valueOf(QuickExchangeHandler.settings.clickIntervalMs));
                    quickExchangeIntervalField.setEnableBackgroundDrawing(false);
                }
                quickExchangeIntervalField.x = controlX + controlWidth - 50;
                quickExchangeIntervalField.y = currentY;
                GuiTheme.drawInputFrame(quickExchangeIntervalField.x - 1, quickExchangeIntervalField.y - 1,
                        quickExchangeIntervalField.width + 2, quickExchangeIntervalField.height + 2, true,
                        quickExchangeIntervalField.isFocused());
                quickExchangeIntervalField.drawTextBox();
                currentY += 22;

                mc.fontRenderer.drawStringWithShadow(I18n.format("gui.quick_exchange.ctrl_click"), controlX,
                        currentY + 6, 0xFFFFFF);
                if (quickExchangeAmountField == null) {
                    quickExchangeAmountField = new GuiTextField(9004, mc.fontRenderer, 0, 0, 50, 20);
                    quickExchangeAmountField.setText(String.valueOf(exchangeAmount));
                    quickExchangeAmountField.setEnableBackgroundDrawing(false);
                }
                quickExchangeAmountField.x = controlX + controlWidth - 50;
                quickExchangeAmountField.y = currentY;
                GuiTheme.drawInputFrame(quickExchangeAmountField.x - 1, quickExchangeAmountField.y - 1,
                        quickExchangeAmountField.width + 2, quickExchangeAmountField.height + 2, true,
                        quickExchangeAmountField.isFocused());
                quickExchangeAmountField.drawTextBox();

            } else if (DungeonWarehouseHandler.isDungeonWarehouseGui(gui)) {
                int panelWidth = 130;
                int panelX = guiLeft - panelWidth - 5;
                int panelY = guiTop;
                int panelHeight = 140;

                // !! 修复：使用 Gui.drawRect 静态方法
                Gui.drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);
                mc.fontRenderer.drawStringWithShadow(I18n.format("gui.dungeon_warehouse.title"), panelX + 7,
                        panelY + 8, 0xFFFFFF);

                int currentY = panelY + 25;
                int controlWidth = panelWidth - 14;
                int controlX = panelX + 7;

                if (dungeonWarehouseShiftClickButton == null) {
                    dungeonWarehouseShiftClickButton = new ToggleGuiButton(9005, 0, 0, 0, 0, "",
                            DungeonWarehouseHandler.settings.shiftClickEnabled);
                }
                dungeonWarehouseShiftClickButton.x = controlX;
                dungeonWarehouseShiftClickButton.y = currentY;
                dungeonWarehouseShiftClickButton.width = controlWidth;
                dungeonWarehouseShiftClickButton.height = 20;
                dungeonWarehouseShiftClickButton.displayString = I18n.format("gui.dungeon_warehouse.shift_click")
                        + ": "
                        + (DungeonWarehouseHandler.settings.shiftClickEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"));
                dungeonWarehouseShiftClickButton.setEnabledState(DungeonWarehouseHandler.settings.shiftClickEnabled);
                dungeonWarehouseShiftClickButton.drawButton(mc, event.getMouseX(), event.getMouseY(),
                        event.getRenderPartialTicks());
                currentY += 22;

                if (dungeonWarehouseCtrlClickButton == null) {
                    dungeonWarehouseCtrlClickButton = new ToggleGuiButton(9006, 0, 0, 0, 0, "",
                            DungeonWarehouseHandler.settings.ctrlClickEnabled);
                }
                dungeonWarehouseCtrlClickButton.x = controlX;
                dungeonWarehouseCtrlClickButton.y = currentY;
                dungeonWarehouseCtrlClickButton.width = controlWidth;
                dungeonWarehouseCtrlClickButton.height = 20;
                dungeonWarehouseCtrlClickButton.displayString = I18n.format("gui.dungeon_warehouse.ctrl_click")
                        + ": "
                        + (DungeonWarehouseHandler.settings.ctrlClickEnabled ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"));
                dungeonWarehouseCtrlClickButton.setEnabledState(DungeonWarehouseHandler.settings.ctrlClickEnabled);
                dungeonWarehouseCtrlClickButton.drawButton(mc, event.getMouseX(), event.getMouseY(),
                        event.getRenderPartialTicks());
                currentY += 22;

                mc.fontRenderer.drawStringWithShadow(I18n.format("gui.dungeon_warehouse.click_interval"), controlX,
                        currentY + 6, 0xFFFFFF);
                if (dungeonWarehouseIntervalField == null) {
                    dungeonWarehouseIntervalField = new GuiTextField(9007, mc.fontRenderer, 0, 0, 50, 20);
                    dungeonWarehouseIntervalField
                            .setText(String.valueOf(DungeonWarehouseHandler.settings.clickIntervalMs));
                    dungeonWarehouseIntervalField.setEnableBackgroundDrawing(false);
                }
                dungeonWarehouseIntervalField.x = controlX + controlWidth - 50;
                dungeonWarehouseIntervalField.y = currentY;
                GuiTheme.drawInputFrame(dungeonWarehouseIntervalField.x - 1, dungeonWarehouseIntervalField.y - 1,
                        dungeonWarehouseIntervalField.width + 2, dungeonWarehouseIntervalField.height + 2, true,
                        dungeonWarehouseIntervalField.isFocused());
                dungeonWarehouseIntervalField.drawTextBox();
                currentY += 22;

                mc.fontRenderer.drawStringWithShadow(I18n.format("gui.dungeon_warehouse.ctrl_click_number"), controlX,
                        currentY + 6, 0xFFFFFF);
                if (dungeonWarehouseAmountField == null) {
                    dungeonWarehouseAmountField = new GuiTextField(9008, mc.fontRenderer, 0, 0, 50, 20);
                    dungeonWarehouseAmountField.setText(String.valueOf(dungeonWarehouseAmount));
                    dungeonWarehouseAmountField.setEnableBackgroundDrawing(false);
                }
                dungeonWarehouseAmountField.x = controlX + controlWidth - 50;
                dungeonWarehouseAmountField.y = currentY;
                GuiTheme.drawInputFrame(dungeonWarehouseAmountField.x - 1, dungeonWarehouseAmountField.y - 1,
                        dungeonWarehouseAmountField.width + 2, dungeonWarehouseAmountField.height + 2, true,
                        dungeonWarehouseAmountField.isFocused());
                dungeonWarehouseAmountField.drawTextBox();
            }
        }

        if (ModConfig.isDebugFlagEnabled(DebugModule.CHEST_ANALYSIS) && event.getGui() instanceof GuiChest) {
            GuiChest gui = (GuiChest) event.getGui();
            if (gui.inventorySlots instanceof ContainerChest) {
                ContainerChest container = (ContainerChest) gui.inventorySlots;
                IInventory currentChestInventory = container.getLowerChestInventory();

                if (currentChestInventory != lastCheckedChestInventory) {
                    performChestAnalysis(gui);
                    lastCheckedChestInventory = currentChestInventory;
                }
            }
        }

        drawMouseCoordinateDebugHud();
    }

    private void drawMouseCoordinateDebugHud() {
        if (!ModConfig.showMouseCoordinates || mc == null || mc.fontRenderer == null) {
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int rawMouseX = Mouse.getX();
        int rawMouseY = mc.displayHeight - Mouse.getY() - 1;
        int scaledMouseX = rawMouseX * scaledResolution.getScaledWidth() / mc.displayWidth;
        int scaledMouseY = rawMouseY * scaledResolution.getScaledHeight() / mc.displayHeight;
        String coordText = I18n.format("gui.overlay.debug.coords",
                rawMouseX, rawMouseY, scaledMouseX, scaledMouseY);
        int screenWidth = scaledResolution.getScaledWidth();
        int textWidth = mc.fontRenderer.getStringWidth(coordText);
        mc.fontRenderer.drawStringWithShadow(coordText, (screenWidth - textWidth) / 2, 5, 0xFFFFFF);
    }

    private void performChestAnalysis(GuiChest gui) {
        if (mc.player == null)
            return;

        ContainerChest container = (ContainerChest) gui.inventorySlots;
        IInventory inventory = container.getLowerChestInventory();
        String title = inventory.getDisplayName().getUnformattedText();

        mc.player.sendMessage(new TextComponentString(TextFormatting.GOLD + "--- Chest Check Debug ---"));
        mc.player.sendMessage(
                new TextComponentString(
                        TextFormatting.YELLOW + "Detected chest title: " + TextFormatting.WHITE + title));

        boolean isQuickExchange = QuickExchangeHandler.isQuickExchangeGui(gui);
        String resultText = isQuickExchange ? TextFormatting.GREEN + "MATCH" : TextFormatting.RED + "NO MATCH";
        mc.player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Is quick-exchange GUI? " + resultText));

        String expectedNameSlot0 = I18n.format("gui.overlay.coin_coupon_60");
        String expectedNameSlot1 = I18n.format("gui.overlay.coin_coupon_360");

        ItemStack stackSlot0 = container.getSlot(0).getStack();
        ItemStack stackSlot1 = container.getSlot(1).getStack();
        String actualNameSlot0 = stackSlot0.isEmpty() ? I18n.format("gui.overlay.empty_slot")
                : stackSlot0.getDisplayName();
        String actualNameSlot1 = stackSlot1.isEmpty() ? I18n.format("gui.overlay.empty_slot")
                : stackSlot1.getDisplayName();

        mc.player.sendMessage(new TextComponentString(TextFormatting.GRAY + "--- Slot 0 Check ---"));
        mc.player.sendMessage(
                new TextComponentString(
                        TextFormatting.AQUA + "  Expected name contains: " + TextFormatting.WHITE + expectedNameSlot0));
        mc.player.sendMessage(
                new TextComponentString(
                        TextFormatting.AQUA + "  Actual item name: " + TextFormatting.WHITE + actualNameSlot0));
        boolean match0 = !stackSlot0.isEmpty() && actualNameSlot0.contains(expectedNameSlot0);
        mc.player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "  Match: "
                        + (match0 ? TextFormatting.GREEN + "YES" : TextFormatting.RED + "NO")));

        mc.player.sendMessage(new TextComponentString(TextFormatting.GRAY + "--- Slot 1 Check ---"));
        mc.player.sendMessage(
                new TextComponentString(
                        TextFormatting.AQUA + "  Expected name contains: " + TextFormatting.WHITE + expectedNameSlot1));
        mc.player.sendMessage(
                new TextComponentString(
                        TextFormatting.AQUA + "  Actual item name: " + TextFormatting.WHITE + actualNameSlot1));
        boolean match1 = !stackSlot1.isEmpty() && actualNameSlot1.contains(expectedNameSlot1);
        mc.player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "  Match: "
                        + (match1 ? TextFormatting.GREEN + "YES" : TextFormatting.RED + "NO")));

        mc.player.sendMessage(new TextComponentString(TextFormatting.GOLD + "--------------------"));
    }

    @SubscribeEvent
    public void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        GuiScreen currentGui = event.getGui();
        if (currentGui == null) {
            return;
        }

        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0 && Mouse.getEventButton() <= 2) {
            InputTimelineManager.recordMouseClick(Mouse.getEventButton());
        }

        if (ModConfig.enableGhostItemCopy && currentGui instanceof GuiContainer
                && Mouse.getEventButton() == 2 && Mouse.getEventButtonState()) {
            handleGuiGhostCopy((GuiContainer) currentGui);
            event.setCanceled(true);
            return;
        }

        if (currentGui instanceof GuiChest && Mouse.getEventButtonState()) {
            GuiChest gui = (GuiChest) currentGui;
            int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
            int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;

            if (QuickExchangeHandler.isQuickExchangeGui(gui)) {
                if (quickExchangeShiftClickButton != null
                        && quickExchangeShiftClickButton.mousePressed(mc, mouseX, mouseY)) {
                    QuickExchangeHandler.settings.shiftClickEnabled = !QuickExchangeHandler.settings.shiftClickEnabled;
                    QuickExchangeHandler.saveConfig();
                    event.setCanceled(true);
                    return;
                }
                if (quickExchangeCtrlClickButton != null
                        && quickExchangeCtrlClickButton.mousePressed(mc, mouseX, mouseY)) {
                    QuickExchangeHandler.settings.ctrlClickEnabled = !QuickExchangeHandler.settings.ctrlClickEnabled;
                    QuickExchangeHandler.saveConfig();
                    event.setCanceled(true);
                    return;
                }
                if (quickExchangeIntervalField != null)
                    quickExchangeIntervalField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                if (quickExchangeAmountField != null)
                    quickExchangeAmountField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());

                Slot slot = gui.getSlotUnderMouse();
                if (slot != null && slot.getHasStack() && slot.inventory != mc.player.inventory) {
                    boolean isShift = GuiScreen.isShiftKeyDown();
                    boolean isCtrl = GuiScreen.isCtrlKeyDown();
                    if (isShift || isCtrl) {
                        QuickExchangeHandler.handleClick(slot, isShift, isCtrl, exchangeAmount);
                        event.setCanceled(true);
                    }
                }
                return;
            }

            if (DungeonWarehouseHandler.isDungeonWarehouseGui(gui)) {
                if (dungeonWarehouseShiftClickButton != null
                        && dungeonWarehouseShiftClickButton.mousePressed(mc, mouseX, mouseY)) {
                    DungeonWarehouseHandler.settings.shiftClickEnabled = !DungeonWarehouseHandler.settings.shiftClickEnabled;
                    DungeonWarehouseHandler.saveConfig();
                    event.setCanceled(true);
                    return;
                }
                if (dungeonWarehouseCtrlClickButton != null
                        && dungeonWarehouseCtrlClickButton.mousePressed(mc, mouseX, mouseY)) {
                    DungeonWarehouseHandler.settings.ctrlClickEnabled = !DungeonWarehouseHandler.settings.ctrlClickEnabled;
                    DungeonWarehouseHandler.saveConfig();
                    event.setCanceled(true);
                    return;
                }
                if (dungeonWarehouseIntervalField != null)
                    dungeonWarehouseIntervalField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
                if (dungeonWarehouseAmountField != null)
                    dungeonWarehouseAmountField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());

                Slot slot = gui.getSlotUnderMouse();
                if (slot != null && slot.getHasStack()) {
                    boolean isShift = GuiScreen.isShiftKeyDown();
                    boolean isCtrl = GuiScreen.isCtrlKeyDown();

                    boolean shouldHandle = (isCtrl && DungeonWarehouseHandler.settings.ctrlClickEnabled) ||
                            (isShift && DungeonWarehouseHandler.settings.shiftClickEnabled
                                    && slot.inventory == mc.player.inventory);

                    if (shouldHandle) {
                        DungeonWarehouseHandler.handleClick(slot, isShift, isCtrl, dungeonWarehouseAmount);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onKeyboardInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() != Keyboard.KEY_NONE) {
            InputTimelineManager.recordKeyPress(Keyboard.getEventKey());
        }
        if (mc.currentScreen instanceof GuiChest) {
            GuiChest gui = (GuiChest) mc.currentScreen;
            char typedChar = Keyboard.getEventCharacter();
            int keyCode = Keyboard.getEventKey();

            if (QuickExchangeHandler.isQuickExchangeGui(gui)) {
                if (quickExchangeIntervalField != null && quickExchangeIntervalField.isFocused()) {
                    if (quickExchangeIntervalField.textboxKeyTyped(typedChar, keyCode)) {
                        try {
                            int interval = Integer.parseInt(quickExchangeIntervalField.getText());
                            QuickExchangeHandler.settings.clickIntervalMs = Math.max(10, interval); // 最小10ms
                            QuickExchangeHandler.saveConfig();
                        } catch (NumberFormatException ignored) {
                        }
                        event.setCanceled(true);
                    }
                }
                if (quickExchangeAmountField != null && quickExchangeAmountField.isFocused()) {
                    if (quickExchangeAmountField.textboxKeyTyped(typedChar, keyCode)) {
                        try {
                            exchangeAmount = Integer.parseInt(quickExchangeAmountField.getText());
                        } catch (NumberFormatException ignored) {
                        }
                        event.setCanceled(true);
                    }
                }
            } else if (DungeonWarehouseHandler.isDungeonWarehouseGui(gui)) {
                if (dungeonWarehouseIntervalField != null && dungeonWarehouseIntervalField.isFocused()) {
                    if (dungeonWarehouseIntervalField.textboxKeyTyped(typedChar, keyCode)) {
                        try {
                            int interval = Integer.parseInt(dungeonWarehouseIntervalField.getText());
                            DungeonWarehouseHandler.settings.clickIntervalMs = Math.max(10, interval);
                            DungeonWarehouseHandler.saveConfig();
                        } catch (NumberFormatException ignored) {
                        }
                        event.setCanceled(true);
                    }
                }
                if (dungeonWarehouseAmountField != null && dungeonWarehouseAmountField.isFocused()) {
                    if (dungeonWarehouseAmountField.textboxKeyTyped(typedChar, keyCode)) {
                        try {
                            dungeonWarehouseAmount = Integer.parseInt(dungeonWarehouseAmountField.getText());
                        } catch (NumberFormatException ignored) {
                        }
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    private GuiButton findHoveredButton(GuiScreen gui) {
        try {
            for (Class<?> clazz = gui.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(gui);
                    if (fieldValue == null)
                        continue;

                    if (fieldValue instanceof GuiButton) {
                        GuiButton button = (GuiButton) fieldValue;
                        if (button.visible && button.isMouseOver())
                            return button;
                    } else if (fieldValue instanceof List) {
                        for (Object element : (List<?>) fieldValue) {
                            if (element instanceof GuiButton) {
                                GuiButton button = (GuiButton) element;
                                if (button.visible && button.isMouseOver())
                                    return button;
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | SecurityException e) {
            // 静默处理反射异常
        }
        return null;
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (mc.currentScreen == null && Mouse.getEventButtonState() && Mouse.getEventButton() >= 0 && Mouse.getEventButton() <= 2) {
            InputTimelineManager.recordMouseClick(Mouse.getEventButton());
        }
        if (ModConfig.enableGhostItemCopy && mc.currentScreen == null
                && Mouse.getEventButton() == 2 && Mouse.getEventButtonState()) {
            handleWorldGhostCopy();
        }
    }

    /**
     * 当自定义菜单显示时，拦截原始鼠标事件：
     * - 左键/右键仅作用于菜单
     * - 滚轮仅作用于菜单，不再切换快捷栏
     */
    @SubscribeEvent
    public void onRawMouseEvent(MouseEvent event) {
        if (!zszlScriptMod.isGuiVisible) {
            return;
        }

        // 关键修复：仅在“游戏内悬浮菜单”场景拦截原始鼠标。
        // 若当前已有其它GUI（箱子/自定义界面等）打开，不应在这里吞掉事件，
        // 否则会导致按钮“点不动/无响应”。
        if (mc.currentScreen != null) {
            return;
        }

        boolean handledByOverlay = false;

        int dWheel = event.getDwheel();
        if (dWheel != 0) {
            GuiInventory.handleMouseWheel(dWheel, Mouse.getX(), Mouse.getY());
            handledByOverlay = true;
        }

        int button = event.getButton();
        if (button != -1) {
            if (event.isButtonstate()) {
                try {
                    GuiInventory.handleMouseClick(Mouse.getX(), Mouse.getY(), button);
                    handledByOverlay = true;
                } catch (IOException e) {
                    zszlScriptMod.LOGGER.error("Error handling overlay mouse click", e);
                }
            } else {
                GuiInventory.handleMouseRelease(Mouse.getX(), Mouse.getY(), button);
                handledByOverlay = true;
            }
        }

        // 仅当确实被悬浮菜单消费时再阻止事件继续传递。
        if (handledByOverlay) {
            event.setCanceled(true);
        }
    }

    private void handleGuiGhostCopy(GuiContainer gui) {
        if (mc.player == null)
            return;

        Slot slot = gui.getSlotUnderMouse();
        if (slot == null) {
            sendGhostCopyMessage("未指向有效槽位");
            return;
        }

        boolean isPlayerSlot = slot.inventory == mc.player.inventory;
        boolean forcePaste = GuiScreen.isCtrlKeyDown();

        if (!forcePaste && slot.getHasStack()) {
            ghostClipboardStack = slot.getStack().copy();
            sendGhostCopyMessage(
                    "已复制: " + ghostClipboardStack.getDisplayName() + " x" + ghostClipboardStack.getCount());
            return;
        }

        if (!isPlayerSlot) {
            sendGhostCopyMessage("仅可粘贴到玩家背包槽位");
            return;
        }

        if (ghostClipboardStack.isEmpty()) {
            sendGhostCopyMessage("幽灵剪贴板为空，请先中键复制物品");
            return;
        }

        ItemStack copy = ghostClipboardStack.copy();
        int stackLimit = Math.min(copy.getMaxStackSize(), slot.getItemStackLimit(copy));
        if (stackLimit <= 0 || !slot.isItemValid(copy)) {
            sendGhostCopyMessage("该槽位不接受此物品");
            return;
        }
        copy.setCount(Math.min(copy.getCount(), stackLimit));

        slot.putStack(copy);
        slot.onSlotChanged();
        mc.player.inventory.markDirty();
        sendGhostCopyMessage("已粘贴到槽位 " + slot.slotNumber + "（本地幽灵物品）");
    }

    private void handleWorldGhostCopy() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit == RayTraceResult.Type.MISS) {
            sendGhostCopyMessage("未命中可复制目标");
            return;
        }

        ItemStack picked = ItemStack.EMPTY;
        if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            if (pos != null && !mc.world.isAirBlock(pos)) {
                IBlockState state = mc.world.getBlockState(pos);
                picked = state.getBlock().getPickBlock(state, hit, mc.world, pos, mc.player);
            }
        } else if (hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit != null) {
            picked = hit.entityHit.getPickedResult(hit);
        }

        if (picked == null || picked.isEmpty()) {
            sendGhostCopyMessage("该目标不可复制");
            return;
        }

        ghostClipboardStack = picked.copy();
        int currentSlot = mc.player.inventory.currentItem;
        mc.player.inventory.setInventorySlotContents(currentSlot, ghostClipboardStack.copy());
        mc.player.inventory.markDirty();

        sendGhostCopyMessage("已复制到当前快捷栏（本地幽灵物品）: " + ghostClipboardStack.getDisplayName());
    }

    private void sendGhostCopyMessage(String text) {
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString("§d[复制幽灵] §f" + text));
        }
    }
}
