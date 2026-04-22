package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.Input;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

public final class GuiMoveFeatureHandler {

    private static final float LOOK_STEP = 5.0F;

    private static boolean overridingMovementKeys;

    private GuiMoveFeatureHandler() {
    }

    public static void apply(Minecraft mc) {
        if (mc == null) {
            return;
        }

        if (!shouldAllowMovementDuringGui(mc)) {
            if (overridingMovementKeys) {
                restoreMovementKeyStates(mc);
                overridingMovementKeys = false;
            }
            return;
        }

        syncMovementKeyStates(mc);
        applyMovementInput(mc, mc.player == null ? null : mc.player.input);
        overridingMovementKeys = true;
        applyArrowKeyLook(mc.player);
    }

    public static void onClientDisconnect() {
        overridingMovementKeys = false;
    }

    public static boolean shouldAllowMovementDuringGui(Minecraft mc) {
        if (!MovementFeatureManager.isEnabled("gui_move")) {
            return false;
        }
        if (mc == null || mc.player == null || mc.level == null) {
            return false;
        }
        if (mc.screen instanceof ChatScreen) {
            return false;
        }
        return mc.screen != null || zszlScriptMod.isGuiVisible;
    }

    public static void applyMovementInput(Minecraft mc, Input movementInput) {
        if (!shouldAllowMovementDuringGui(mc) || movementInput == null || mc == null || mc.options == null) {
            return;
        }

        boolean forwardDown = isMovementKeyDown(mc.options.keyUp);
        boolean backDown = isMovementKeyDown(mc.options.keyDown);
        boolean leftDown = isMovementKeyDown(mc.options.keyLeft);
        boolean rightDown = isMovementKeyDown(mc.options.keyRight);
        boolean jumpDown = isMovementKeyDown(mc.options.keyJump);
        boolean sneakDown = isMovementKeyDown(mc.options.keyShift);

        movementInput.up = forwardDown;
        movementInput.down = backDown;
        movementInput.left = leftDown;
        movementInput.right = rightDown;
        movementInput.forwardImpulse = forwardDown == backDown ? 0.0F : (forwardDown ? 1.0F : -1.0F);
        movementInput.leftImpulse = leftDown == rightDown ? 0.0F : (leftDown ? 1.0F : -1.0F);
        movementInput.jumping = jumpDown;
        movementInput.shiftKeyDown = sneakDown;
        if (movementInput.shiftKeyDown) {
            movementInput.leftImpulse *= 0.3F;
            movementInput.forwardImpulse *= 0.3F;
        }

        LocalPlayer player = mc.player;
        if (player != null && isMovementKeyDown(mc.options.keySprint)
                && movementInput.forwardImpulse > 0.0F
                && !movementInput.shiftKeyDown) {
            player.setSprinting(true);
        }
    }

    private static void syncMovementKeyStates(Minecraft mc) {
        if (mc.options == null) {
            return;
        }

        syncKeyState(mc.options.keyUp, true);
        syncKeyState(mc.options.keyDown, true);
        syncKeyState(mc.options.keyLeft, true);
        syncKeyState(mc.options.keyRight, true);
        syncKeyState(mc.options.keyJump, true);
        syncKeyState(mc.options.keyShift, true);
        syncKeyState(mc.options.keySprint, true);
    }

    private static void restoreMovementKeyStates(Minecraft mc) {
        if (mc.options == null) {
            return;
        }

        boolean keepPhysicalState = mc.screen == null;
        syncKeyState(mc.options.keyUp, keepPhysicalState);
        syncKeyState(mc.options.keyDown, keepPhysicalState);
        syncKeyState(mc.options.keyLeft, keepPhysicalState);
        syncKeyState(mc.options.keyRight, keepPhysicalState);
        syncKeyState(mc.options.keyJump, keepPhysicalState);
        syncKeyState(mc.options.keyShift, keepPhysicalState);
        syncKeyState(mc.options.keySprint, keepPhysicalState);
    }

    private static void syncKeyState(KeyMapping keyBinding, boolean mirrorPhysicalState) {
        if (keyBinding == null) {
            return;
        }

        InputConstants.Key key = keyBinding.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return;
        }

        KeyMapping.set(key, mirrorPhysicalState && isMovementKeyDown(keyBinding));
    }

    private static boolean isMovementKeyDown(KeyMapping keyBinding) {
        if (keyBinding == null) {
            return false;
        }

        InputConstants.Key key = keyBinding.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            int legacyKeyCode = Keyboard.fromGlfwKey(key.getValue());
            if (legacyKeyCode != Keyboard.KEY_NONE) {
                return SimulatedKeyInputManager.isKeyDown(legacyKeyCode);
            }
            if (Minecraft.getInstance() != null && Minecraft.getInstance().getWindow() != null) {
                return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
            }
            return false;
        }
        return keyBinding.isDown();
    }

    private static void applyArrowKeyLook(LocalPlayer player) {
        if (player == null) {
            return;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            player.setXRot(player.getXRot() - LOOK_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            player.setXRot(player.getXRot() + LOOK_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            player.setYRot(player.getYRot() + LOOK_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            player.setYRot(player.getYRot() - LOOK_STEP);
        }

        if (player.getXRot() > 90.0F) {
            player.setXRot(90.0F);
        }
        if (player.getXRot() < -90.0F) {
            player.setXRot(-90.0F);
        }
    }
}







