package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class GuiChestViewer extends ThemedGuiScreen {

    private final ChestViewerContainer viewer;
    private final Component playerTitle;
    private final Component chestTitle;
    private final int xSize;
    private final int ySize;

    public GuiChestViewer(Container playerInv, Container chestInv) {
        this.viewer = new ChestViewerContainer(playerInv, chestInv);
        this.playerTitle = Component.literal("玩家背包");
        this.chestTitle = Component.literal("箱子内容");
        this.xSize = 176;
        this.ySize = 114 + this.viewer.getInventoryRows() * 18;
        this.allowUserInput = false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;

        GuiTheme.drawPanel(guiLeft - 8, guiTop - 8, this.xSize + 16, this.ySize + 16);
        drawString(fontRenderer, chestTitle.getString(), guiLeft + 8, guiTop + 6, 0xFFFFFF);
        drawString(fontRenderer, playerTitle.getString(), guiLeft + 8, guiTop + this.ySize - 96 + 2, 0xFFFFFF);

        for (ChestViewerContainer.ReadOnlySlot slot : viewer.getSlots()) {
            int x = guiLeft + slot.getX() - 1;
            int y = guiTop + slot.getY() - 1;
            drawRect(x, y, x + 18, y + 18, 0xAA1A1F26);
            drawRect(x, y, x + 18, y + 1, 0xFF5D768F);
            drawRect(x, y, x + 1, y + 18, 0xFF5D768F);
            drawRect(x + 17, y, x + 18, y + 18, 0xFF243343);
            drawRect(x, y + 17, x + 18, y + 18, 0xFF243343);

            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && GuiCompatContext.current() != null) {
                GuiCompatContext.current().renderItem(stack, guiLeft + slot.getX(), guiTop + slot.getY());
            }
        }

        ChestViewerContainer.ReadOnlySlot hovered = viewer.getHoveredSlot(mouseX - guiLeft, mouseY - guiTop);
        if (hovered != null) {
            int x = guiLeft + hovered.getX() - 1;
            int y = guiTop + hovered.getY() - 1;
            drawRect(x, y, x + 18, y + 18, 0x55B7D9FF);

            ItemStack hoveredStack = hovered.getItem();
            if (!hoveredStack.isEmpty() && GuiCompatContext.current() != null) {
                GuiCompatContext.current().setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
