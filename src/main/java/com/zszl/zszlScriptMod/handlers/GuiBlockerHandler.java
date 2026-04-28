package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;

public class GuiBlockerHandler {

    private static int remainingBlockCount = 0;

    public static void blockNextGui(int count) {
        blockGui(count, false);
    }

    public static void blockGui(int count, boolean blockCurrentGui) {
        int effectiveCount = Math.max(1, count);
        remainingBlockCount += effectiveCount;

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    new TextComponentString(I18n.format("msg.gui_blocker.enabled", remainingBlockCount)), false);
        }
        zszlScriptMod.LOGGER.info(I18n.format("log.gui_blocker.enabled"), effectiveCount, remainingBlockCount);

        if (blockCurrentGui) {
            blockCurrentGuiNow();
        }
    }

    public static boolean shouldBlockAndConsume(GuiScreen gui) {
        if (gui == null || remainingBlockCount <= 0) {
            return false;
        }

        remainingBlockCount--;
        zszlScriptMod.LOGGER.info(I18n.format("log.gui_blocker.blocked"), gui.getClass().getName(),
                remainingBlockCount);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    new TextComponentString(I18n.format("msg.gui_blocker.blocked", remainingBlockCount)), false);
        }

        return true;
    }

    public static int getRemainingBlockCount() {
        return remainingBlockCount;
    }

    private static void blockCurrentGuiNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && remainingBlockCount > 0) {
            remainingBlockCount--;
            zszlScriptMod.LOGGER.info(I18n.format("log.gui_blocker.blocked"), mc.screen.getClass().getName(),
                    remainingBlockCount);
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new TextComponentString(I18n.format("msg.gui_blocker.blocked", remainingBlockCount)), false);
            }
            mc.setScreen(null);
        }
    }

    public static void reset() {
        remainingBlockCount = 0;
    }
}
