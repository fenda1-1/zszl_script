package com.zszl.zszlScriptMod.handlers;

public final class AdExpPanelHandler {

    public static final AdExpPanelHandler INSTANCE = new AdExpPanelHandler();

    public static boolean enabled = false;
    public static boolean showCurrentValue = true;
    public static boolean showCurrentTotalExp = true;
    public static boolean currentValueUseTotalExp = false;
    public static boolean showCurrentLevel = true;
    public static boolean showNextLevel = true;
    public static boolean showProgress = true;
    public static int progressDecimalPlaces = 1;

    private AdExpPanelHandler() {
    }

    public static void toggleEnabled() {
        enabled = !enabled;
    }

    public static void saveConfig() {
    }

    public static void loadConfig() {
    }

    public static void onAdventureExpUpdated(int exp) {
    }
}
