package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class Gui {

    protected float zLevel;

    public static int withDefaultTextAlpha(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.fill(left, top, right, bottom, color);
        }
    }

    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.fillGradient(left, top, right, bottom, startColor, endColor);
        }
    }

    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uWidth, int vHeight,
            int width, int height, float tileWidth, float tileHeight) {
    }

    public static void drawScaledCustomSizeModalRect(Identifier texture, int x, int y, float u, float v, int uWidth,
            int vHeight, int width, int height, float tileWidth, float tileHeight, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null || width <= 0 || height <= 0 || tileWidth <= 0.0F || tileHeight <= 0.0F) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, uWidth, vHeight,
                Math.round(tileWidth), Math.round(tileHeight), color);
    }
}

