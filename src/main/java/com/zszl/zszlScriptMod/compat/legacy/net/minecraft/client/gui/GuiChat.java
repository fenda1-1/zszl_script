package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

public class GuiChat extends GuiScreen {

    public GuiTextField field_146415_a;

    @Override
    public void initGui() {
        super.initGui();
        field_146415_a = new GuiTextField(0, this.fontRenderer, 4, this.height - 14, this.width - 8, 12);
    }
}

