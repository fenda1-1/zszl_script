package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiCompatContext {

    private static final ThreadLocal<GuiGraphics> CURRENT = new ThreadLocal<>();

    private GuiCompatContext() {
    }

    public static void push(GuiGraphics graphics) {
        CURRENT.set(graphics);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static GuiGraphics current() {
        return CURRENT.get();
    }
}

