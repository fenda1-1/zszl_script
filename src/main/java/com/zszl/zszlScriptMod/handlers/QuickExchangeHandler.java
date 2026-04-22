package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.inventory.Slot;

public final class QuickExchangeHandler {

    public static final QuickExchangeHandler INSTANCE = new QuickExchangeHandler();
    public static final Settings settings = new Settings();

    public static final class Settings {
        public boolean shiftClickEnabled = false;
        public boolean ctrlClickEnabled = false;
        public int clickIntervalMs = 100;
    }

    private QuickExchangeHandler() {
    }

    public static void loadConfig() {
    }

    public static void saveConfig() {
    }

    public static boolean isQuickExchangeGui(GuiScreen gui) {
        return false;
    }

    public static void handleClick(Slot slot, boolean shift, boolean ctrl, int amount) {
    }
}


