package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class GuiUtilRenderComponents {

    private GuiUtilRenderComponents() {
    }

    public static List<Component> splitText(Component text, int width, FontRenderer font, boolean keepWords,
            boolean allowNewLines) {
        List<Component> result = new ArrayList<>();
        if (text == null) {
            result.add(Component.empty());
            return result;
        }
        List<String> lines = font.listFormattedStringToWidth(text.getString(), Math.max(1, width));
        for (String line : lines) {
            result.add(Component.literal(line == null ? "" : line));
        }
        if (result.isEmpty()) {
            result.add(Component.empty());
        }
        return result;
    }
}

