package com.zszl.zszlScriptMod.gui;

import com.zszl.zszlScriptMod.gui.debug.GuiPlayerEquipmentViewer;
import net.minecraft.client.Minecraft;

public final class GuiHandler {

    public static final int INVENTORY_VIEWER = 1;

    private GuiHandler() {
    }

    public static void openInventoryViewer(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return;
        }
        mc.setScreen(new GuiPlayerEquipmentViewer());
    }
}
