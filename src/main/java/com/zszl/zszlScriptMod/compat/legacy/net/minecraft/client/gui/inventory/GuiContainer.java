package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.inventory;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class GuiContainer extends GuiScreen {
    public AbstractContainerMenu inventorySlots;
    public int guiLeft;
    public int guiTop;
    public int xSize = 176;
    public int ySize = 166;
    protected Slot hoveredSlot;

    public Slot getSlotUnderMouse() {
        return hoveredSlot;
    }
}



