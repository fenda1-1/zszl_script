package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class FontRenderer {

    private final net.minecraft.client.gui.Font delegate;
    public final int FONT_HEIGHT;

    public FontRenderer(net.minecraft.client.gui.Font delegate) {
        this.delegate = delegate;
        this.FONT_HEIGHT = delegate.lineHeight;
    }

    public int getStringWidth(String text) {
        return delegate.width(text == null ? "" : text);
    }

    public String trimStringToWidth(String text, int width) {
        return delegate.plainSubstrByWidth(text == null ? "" : text, Math.max(0, width));
    }

    public List<String> listFormattedStringToWidth(String text, int width) {
        String safeText = text == null ? "" : text;
        List<String> result = new ArrayList<>();
        List<FormattedText> split = delegate.getSplitter().splitLines(safeText, Math.max(0, width), Style.EMPTY);
        for (FormattedText line : split) {
            result.add(line == null ? "" : line.getString());
        }
        if (result.isEmpty()) {
            result.add(safeText.isEmpty() ? "" : trimStringToWidth(safeText, width));
        }
        return result;
    }

    public int drawString(String text, int x, int y, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return 0;
        }
        return graphics.drawString(delegate, text == null ? "" : text, x, y, color, false);
    }

    public int drawStringWithShadow(String text, float x, float y, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return 0;
        }
        return graphics.drawString(delegate, text == null ? "" : text, x, y, color, true);
    }

    public void drawSplitString(String text, int x, int y, int width, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return;
        }
        graphics.drawWordWrap(delegate, Component.literal(text == null ? "" : text), x, y, width, color);
    }

    public net.minecraft.client.gui.Font unwrap() {
        return delegate;
    }
}

