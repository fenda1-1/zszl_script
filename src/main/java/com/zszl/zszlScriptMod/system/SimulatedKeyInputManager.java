package com.zszl.zszlScriptMod.system;

import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.settings.KeyBinding;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimulatedKeyInputManager {
    public static final SimulatedKeyInputManager INSTANCE = new SimulatedKeyInputManager();
    private static final int PRESS_HOLD_TICKS = 2;
    private static volatile Method keyboardKeyPressMethod;

    public static final class SimulatedPressEvent {
        private final int keyCode;
        private final Set<Integer> modifiers;

        private SimulatedPressEvent(int keyCode, Set<Integer> modifiers) {
            this.keyCode = keyCode;
            this.modifiers = modifiers;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public Set<Integer> getModifiers() {
            return modifiers;
        }
    }

    private final Set<Integer> heldKeys = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private final Set<Integer> managedKeys = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    private final Map<Integer, Integer> pendingReleaseTicks = new ConcurrentHashMap<>();
    private final Queue<SimulatedPressEvent> pendingPressEvents = new ConcurrentLinkedQueue<>();

    private SimulatedKeyInputManager() {
    }

    public static void register() {
        TickEvent.ClientTickEvent.BUS.addListener(INSTANCE::onClientTick);
    }

    public static void simulateKey(String keyName, String state) {
        int keyCode = ModUtils.resolveLwjglKeyCode(keyName);
        if (keyCode == Keyboard.KEY_NONE) {
            zszlScriptMod.LOGGER.warn("忽略未知模拟按键: {}", keyName);
            return;
        }

        String normalizedState = ModUtils.normalizeSimulatedKeyState(state);
        runOnClientThread(() -> INSTANCE.applyStateChange(keyCode, normalizedState));
    }

    public static void simulateKeyCode(int keyCode, String state) {
        if (keyCode == Keyboard.KEY_NONE) {
            return;
        }
        String normalizedState = ModUtils.normalizeSimulatedKeyState(state);
        runOnClientThread(() -> INSTANCE.applyStateChange(keyCode, normalizedState));
    }

    public static boolean isKeyDown(int keyCode) {
        return Keyboard.isKeyDown(keyCode) || INSTANCE.isSimulatedKeyDown(keyCode);
    }

    public static boolean isEitherKeyDown(int primaryKeyCode, int secondaryKeyCode) {
        return isKeyDown(primaryKeyCode) || isKeyDown(secondaryKeyCode);
    }

    public SimulatedPressEvent pollPressedKey() {
        return pendingPressEvents.poll();
    }

    public void reset() {
        runOnClientThread(this::resetInternal);
    }

    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        syncManagedKeyStates();
        advancePendingReleases();
    }

    private void applyStateChange(int keyCode, String state) {
        managedKeys.add(keyCode);
        switch (state) {
            case "Down":
                pressKey(keyCode, false);
                break;
            case "Up":
                releaseKey(keyCode);
                break;
            default:
                tapKey(keyCode);
                break;
        }
        zszlScriptMod.LOGGER.info("成功模拟实例内按键: {} ({})", Keyboard.getKeyName(keyCode), state);
    }

    private void pressKey(int keyCode, boolean syntheticTap) {
        boolean wasHeld = heldKeys.contains(keyCode);
        heldKeys.add(keyCode);
        if (syntheticTap) {
            pendingReleaseTicks.put(keyCode, PRESS_HOLD_TICKS);
        } else {
            pendingReleaseTicks.remove(keyCode);
        }

        syncKeyBindingState(keyCode);
        if (!wasHeld || syntheticTap) {
            if (!dispatchGameKeyEvent(keyCode, true, snapshotActiveModifiers())) {
                KeyBinding.onTick(keyCode);
            }
            pendingPressEvents.add(new SimulatedPressEvent(keyCode, snapshotActiveModifiers()));
        }
    }

    private void tapKey(int keyCode) {
        if (heldKeys.contains(keyCode) && !pendingReleaseTicks.containsKey(keyCode)) {
            syncKeyBindingState(keyCode);
            if (!dispatchGameKeyEvent(keyCode, true, snapshotActiveModifiers())) {
                KeyBinding.onTick(keyCode);
            }
            pendingPressEvents.add(new SimulatedPressEvent(keyCode, snapshotActiveModifiers()));
            return;
        }
        pressKey(keyCode, true);
    }

    private void releaseKey(int keyCode) {
        Set<Integer> modifiersBeforeRelease = snapshotActiveModifiers();
        heldKeys.remove(keyCode);
        pendingReleaseTicks.remove(keyCode);
        syncKeyBindingState(keyCode);
        dispatchGameKeyEvent(keyCode, false, modifiersBeforeRelease);
    }

    private void syncManagedKeyStates() {
        if (managedKeys.isEmpty()) {
            return;
        }

        for (Integer keyCode : new HashSet<>(managedKeys)) {
            syncKeyBindingState(keyCode);
        }
    }

    private void advancePendingReleases() {
        if (pendingReleaseTicks.isEmpty()) {
            return;
        }

        Set<Integer> releaseKeys = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : pendingReleaseTicks.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                releaseKeys.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        for (Integer keyCode : releaseKeys) {
            releaseKey(keyCode);
        }
    }

    private void resetInternal() {
        Set<Integer> keysToReset = new HashSet<>(managedKeys);
        keysToReset.addAll(heldKeys);
        keysToReset.addAll(pendingReleaseTicks.keySet());

        pendingPressEvents.clear();
        heldKeys.clear();
        pendingReleaseTicks.clear();
        managedKeys.clear();

        for (Integer keyCode : keysToReset) {
            KeyBinding.setKeyBindState(keyCode, Keyboard.isKeyDown(keyCode));
        }
    }

    private void syncKeyBindingState(int keyCode) {
        KeyBinding.setKeyBindState(keyCode, Keyboard.isKeyDown(keyCode) || isSimulatedKeyDown(keyCode));
    }

    private boolean dispatchGameKeyEvent(int keyCode, boolean pressed, Set<Integer> modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.keyboardHandler == null) {
            return false;
        }

        int glfwKey = Keyboard.toGlfwKey(keyCode);
        if (glfwKey <= GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        }

        int glfwModifiers = toGlfwModifiers(modifiers);
        int action = pressed ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
        try {
            Method keyPressMethod = resolveKeyboardKeyPressMethod(mc);
            keyPressMethod.invoke(mc.keyboardHandler, mc.getWindow().handle(), action,
                    new KeyEvent(glfwKey, 0, glfwModifiers));
            return true;
        } catch (Throwable throwable) {
            zszlScriptMod.LOGGER.debug("Dispatching synthetic game key event failed: keyCode={} glfwKey={} pressed={}",
                    keyCode, glfwKey, pressed, throwable);
            return false;
        }
    }

    private Method resolveKeyboardKeyPressMethod(Minecraft mc) throws NoSuchMethodException {
        Method method = keyboardKeyPressMethod;
        if (method == null) {
            method = mc.keyboardHandler.getClass().getDeclaredMethod("keyPress", long.class, int.class,
                    KeyEvent.class);
            method.setAccessible(true);
            keyboardKeyPressMethod = method;
        }
        return method;
    }

    private int toGlfwModifiers(Set<Integer> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) {
            return 0;
        }
        int glfwModifiers = 0;
        if (modifiers.contains(Keyboard.KEY_LCONTROL) || modifiers.contains(Keyboard.KEY_RCONTROL)) {
            glfwModifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (modifiers.contains(Keyboard.KEY_LSHIFT) || modifiers.contains(Keyboard.KEY_RSHIFT)) {
            glfwModifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (modifiers.contains(Keyboard.KEY_LMENU) || modifiers.contains(Keyboard.KEY_RMENU)) {
            glfwModifiers |= GLFW.GLFW_MOD_ALT;
        }
        return glfwModifiers;
    }

    private boolean isSimulatedKeyDown(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) {
            return false;
        }
        if (heldKeys.contains(keyCode)) {
            return true;
        }

        switch (keyCode) {
            case Keyboard.KEY_LCONTROL:
                return heldKeys.contains(Keyboard.KEY_RCONTROL);
            case Keyboard.KEY_RCONTROL:
                return heldKeys.contains(Keyboard.KEY_LCONTROL);
            case Keyboard.KEY_LSHIFT:
                return heldKeys.contains(Keyboard.KEY_RSHIFT);
            case Keyboard.KEY_RSHIFT:
                return heldKeys.contains(Keyboard.KEY_LSHIFT);
            case Keyboard.KEY_LMENU:
                return heldKeys.contains(Keyboard.KEY_RMENU);
            case Keyboard.KEY_RMENU:
                return heldKeys.contains(Keyboard.KEY_LMENU);
            default:
                return false;
        }
    }

    private Set<Integer> snapshotActiveModifiers() {
        Set<Integer> modifiers = new HashSet<>();
        if (isKeyDown(Keyboard.KEY_LCONTROL) || isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers.add(Keyboard.KEY_LCONTROL);
        }
        if (isKeyDown(Keyboard.KEY_LSHIFT) || isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers.add(Keyboard.KEY_LSHIFT);
        }
        if (isKeyDown(Keyboard.KEY_LMENU) || isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers.add(Keyboard.KEY_LMENU);
        }
        return modifiers;
    }

    private static void runOnClientThread(Runnable task) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (mc.isSameThread()) {
            task.run();
            return;
        }
        mc.execute(task);
    }
}





