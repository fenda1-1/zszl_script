package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

public class GuiConfirmOpenLink extends GuiYesNo {
    public GuiConfirmOpenLink(GuiYesNoCallback callback, String url, int id, boolean trusted) {
        super(callback, "打开链接", url, id);
    }
}

