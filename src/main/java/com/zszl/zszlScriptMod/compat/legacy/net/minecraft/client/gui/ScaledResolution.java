package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;

public class ScaledResolution {

    private final Minecraft minecraft;

    public ScaledResolution(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public int getScaledWidth() {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    public int getScaledHeight() {
        return minecraft.getWindow().getGuiScaledHeight();
    }

    public int getScaleFactor() {
        return (int) Math.max(1, Math.round(minecraft.getWindow().getGuiScale()));
    }
}

