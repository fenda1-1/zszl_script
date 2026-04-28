package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;

import java.util.ArrayList;
import java.util.List;

public final class GuiUtils {

    private GuiUtils() {
    }

    public static void drawHoveringText(List<String> textLines, int x, int y, int width, int height, int maxTextWidth,
            com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer font) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null || textLines == null) {
            return;
        }
        List<Component> components = new ArrayList<>();
        for (String line : textLines) {
            components.add(Component.literal(line == null ? "" : line));
        }
        graphics.renderComponentTooltip(Minecraft.getInstance().font, components, x, y, ItemStack.EMPTY);
    }
}


