package com.zszl.zszlScriptMod.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BaritoneParkourSettingsHelper {

    private static final Set<String> PARKOUR_SETTING_KEYS = new LinkedHashSet<>(Arrays.asList(
            "parkourMode",
            "parkourProfile",
            "parkourDebugRender",
            "allowParkour",
            "allowParkourPlace",
            "allowParkourAscend"));

    private BaritoneParkourSettingsHelper() {
    }

    public static boolean isParkourSettingKey(String key) {
        if (key == null) {
            return false;
        }
        for (String parkourKey : PARKOUR_SETTING_KEYS) {
            if (parkourKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
}
