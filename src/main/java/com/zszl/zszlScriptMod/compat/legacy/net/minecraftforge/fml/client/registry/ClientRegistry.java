package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.registry;

import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientRegistry {

    private static final List<KeyMapping> REGISTERED_KEYS = new ArrayList<>();

    private ClientRegistry() {
    }

    public static void registerKeyBinding(KeyMapping keyMapping) {
        if (keyMapping != null) {
            REGISTERED_KEYS.add(keyMapping);
        }
    }

    public static List<KeyMapping> getRegisteredKeys() {
        return Collections.unmodifiableList(REGISTERED_KEYS);
    }
}


