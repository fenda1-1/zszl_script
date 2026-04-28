package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher;

public enum Side {
    CLIENT,
    SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isServer() {
        return this == SERVER;
    }
}

