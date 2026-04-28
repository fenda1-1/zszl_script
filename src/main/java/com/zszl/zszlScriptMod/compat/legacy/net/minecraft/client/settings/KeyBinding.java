package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.settings;

import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class KeyBinding extends KeyMapping {

    private static final Map<String, Category> CATEGORIES = new HashMap<>();

    public KeyBinding(String description, int keyCode, String category) {
        super(description, InputConstants.Type.KEYSYM, keyCode, category(category));
    }

    private static Category category(String legacyCategory) {
        String name = legacyCategory == null || legacyCategory.isBlank() ? "misc" : legacyCategory;
        String normalized = name;
        if (normalized.startsWith("key.categories.")) {
            normalized = normalized.substring("key.categories.".length());
        } else if (normalized.startsWith("key.category.")) {
            normalized = normalized.substring("key.category.".length());
        }
        Category vanilla = switch (normalized) {
            case "movement" -> Category.MOVEMENT;
            case "misc" -> Category.MISC;
            case "multiplayer" -> Category.MULTIPLAYER;
            case "gameplay" -> Category.GAMEPLAY;
            case "inventory" -> Category.INVENTORY;
            case "creative" -> Category.CREATIVE;
            case "spectator" -> Category.SPECTATOR;
            case "debug" -> Category.DEBUG;
            default -> null;
        };
        if (vanilla != null) {
            return vanilla;
        }
        Identifier id = normalized.contains(":")
                ? Identifier.parse(normalized)
                : Identifier.withDefaultNamespace(normalized.replace('.', '_').replace('/', '_'));
        return CATEGORIES.computeIfAbsent(id.toString(), ignored -> Category.register(id));
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


