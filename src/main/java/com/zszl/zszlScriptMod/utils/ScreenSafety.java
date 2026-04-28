package com.zszl.zszlScriptMod.utils;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenSafety {

    private ScreenSafety() {
    }

    public static boolean isLoadingOrTransitionScreen(Screen screen) {
        return screen instanceof LevelLoadingScreen
                || screen instanceof ProgressScreen
                || screen instanceof GenericMessageScreen
                || screen instanceof GenericWaitingScreen
                || screen instanceof ConnectScreen;
    }
}
