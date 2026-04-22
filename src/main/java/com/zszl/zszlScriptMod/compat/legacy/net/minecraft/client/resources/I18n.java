package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources;

public final class I18n {

    private I18n() {
    }

    public static String format(String key, Object... args) {
        return net.minecraft.client.resources.language.I18n.get(key, args);
    }
}

