package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.settings;

import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import net.minecraft.client.KeyMapping;

public class KeyBinding extends KeyMapping {

    public KeyBinding(String description, int keyCode, String category) {
        super(description, InputConstants.Type.KEYSYM, keyCode, category);
    }

    public static void setKeyBindState(int keyCode, boolean pressed) {
        KeyMapping.set(InputConstants.Type.KEYSYM.getOrCreate(Keyboard.toGlfwKey(keyCode)), pressed);
    }

    public static void onTick(int keyCode) {
        KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(Keyboard.toGlfwKey(keyCode)));
    }

    public int getKeyCode() {
        return this.getKey().getValue();
    }

    public boolean isKeyDown() {
        return this.isDown();
    }
}


