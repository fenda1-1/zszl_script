package com.zszl.zszlScriptMod.system;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.gui.packet.InputTimelineManager;
import com.zszl.zszlScriptMod.system.KeybindManager.Keybind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GlobalKeybindListener {

    private final Map<Integer, Boolean> lastPhysicalKeyStates = new HashMap<>();

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) {
            return;
        }

        int keyCode = Keyboard.getEventKey();
        if (keyCode == Keyboard.KEY_NONE) {
            return;
        }

        InputTimelineManager.recordKeyPress(keyCode);
        this.lastPhysicalKeyStates.put(keyCode, Keyboard.isKeyDown(keyCode));
        handleTriggeredKey(keyCode, getActivePhysicalModifiers());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        SimulatedKeyInputManager.SimulatedPressEvent pressEvent;
        while ((pressEvent = SimulatedKeyInputManager.INSTANCE.pollPressedKey()) != null) {
            handleTriggeredKey(pressEvent.getKeyCode(), pressEvent.getModifiers());
        }

        pollPhysicalKeybindPresses();
    }

    private void handleTriggeredKey(int keyCode, Set<Integer> providedModifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (mc.screen instanceof ChatScreen || isAnyTextFieldFocused()) {
            return;
        }

        Set<Integer> currentModifiers = providedModifiers == null ? getActiveModifiers() : providedModifiers;

        for (Map.Entry<BindableAction, Keybind> entry : KeybindManager.keybinds.entrySet()) {
            Keybind keybind = entry.getValue();
            if (keybind != null
                    && keybind.getKeyCode() == keyCode
                    && areModifierSetsEqual(keybind.getModifiers(), currentModifiers)) {
                KeybindManager.executeAction(entry.getKey());
                return;
            }
        }

        for (Map.Entry<BindableDebugAction, Keybind> entry : DebugKeybindManager.keybinds.entrySet()) {
            Keybind keybind = entry.getValue();
            if (keybind != null
                    && keybind.getKeyCode() == keyCode
                    && areModifierSetsEqual(keybind.getModifiers(), currentModifiers)) {
                DebugKeybindManager.executeAction(entry.getKey());
                return;
            }
        }

        for (Map.Entry<String, Keybind> entry : new ArrayList<>(KeybindManager.pathSequenceKeybinds.entrySet())) {
            Keybind keybind = entry.getValue();
            if (keybind == null) {
                continue;
            }
            if (keybind.getKeyCode() == keyCode
                    && areModifierSetsEqual(keybind.getModifiers(), currentModifiers)) {
                KeybindManager.executePathSequenceByName(entry.getKey());
                return;
            }
        }
    }

    private void pollPhysicalKeybindPresses() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.lastPhysicalKeyStates.clear();
            return;
        }

        Set<Integer> watchedKeys = collectWatchedPhysicalKeys();
        if (watchedKeys.isEmpty()) {
            this.lastPhysicalKeyStates.clear();
            return;
        }

        Set<Integer> currentModifiers = getActivePhysicalModifiers();
        for (Integer keyCode : watchedKeys) {
            boolean downNow = Keyboard.isKeyDown(keyCode);
            boolean downBefore = this.lastPhysicalKeyStates.getOrDefault(keyCode, Boolean.FALSE);
            if (downNow && !downBefore) {
                handleTriggeredKey(keyCode, currentModifiers);
            }
            this.lastPhysicalKeyStates.put(keyCode, downNow);
        }
        this.lastPhysicalKeyStates.keySet().retainAll(watchedKeys);
    }

    private Set<Integer> collectWatchedPhysicalKeys() {
        Set<Integer> watchedKeys = new HashSet<>();
        for (Keybind keybind : KeybindManager.keybinds.values()) {
            addWatchedPhysicalKey(watchedKeys, keybind);
        }
        for (Keybind keybind : DebugKeybindManager.keybinds.values()) {
            addWatchedPhysicalKey(watchedKeys, keybind);
        }
        for (Keybind keybind : KeybindManager.pathSequenceKeybinds.values()) {
            addWatchedPhysicalKey(watchedKeys, keybind);
        }
        return watchedKeys;
    }

    private void addWatchedPhysicalKey(Set<Integer> watchedKeys, Keybind keybind) {
        if (keybind == null) {
            return;
        }
        int keyCode = keybind.getKeyCode();
        if (keyCode > Keyboard.KEY_NONE) {
            watchedKeys.add(keyCode);
        }
    }

    private Set<Integer> getActiveModifiers() {
        Set<Integer> modifiers = new HashSet<>();
        if (SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL)) {
            modifiers.add(Keyboard.KEY_LCONTROL);
        }
        if (SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT)) {
            modifiers.add(Keyboard.KEY_LSHIFT);
        }
        if (SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LMENU, Keyboard.KEY_RMENU)) {
            modifiers.add(Keyboard.KEY_LMENU);
        }
        return modifiers;
    }

    private Set<Integer> getActivePhysicalModifiers() {
        Set<Integer> modifiers = new HashSet<>();
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers.add(Keyboard.KEY_LCONTROL);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers.add(Keyboard.KEY_LSHIFT);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers.add(Keyboard.KEY_LMENU);
        }
        return modifiers;
    }

    private boolean areModifierSetsEqual(Set<Integer> required, Set<Integer> actual) {
        return actual.equals(required);
    }

    private boolean isAnyTextFieldFocused() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) {
            return false;
        }
        try {
            for (Class<?> clazz = screen.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(screen);
                    if (value instanceof GuiTextField && ((GuiTextField) value).isFocused()) {
                        return true;
                    }
                    if (value instanceof EditBox && ((EditBox) value).isFocused()) {
                        return true;
                    }
                }
            }
        } catch (IllegalAccessException ignored) {
        }

        for (Object child : screen.children()) {
            if (child instanceof EditBox && ((EditBox) child).isFocused()) {
                return true;
            }
        }
        return false;
    }
}
