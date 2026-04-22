package com.zszl.zszlScriptMod.utils;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;

public class CapturingFontRenderer extends FontRenderer {

    private boolean captureEnabled;

    public CapturingFontRenderer() {
        super(Minecraft.getInstance().font);
    }

    public void disableCapture() {
        this.captureEnabled = false;
    }

    public void enableCapture() {
        this.captureEnabled = true;
    }

    public boolean isCaptureEnabled() {
        return captureEnabled;
    }
}

