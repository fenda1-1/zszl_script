/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.TickEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.WorldEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.IInputOverrideHandler;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.input.Input;
import com.zszl.zszlScriptMod.shadowbaritone.behavior.Behavior;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.KeyboardInput;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface with the game's control system allowing the ability to
 * force down certain controls, having the same effect as if we were actually
 * physically forcing down the assigned key.
 *
 * @author Brady
 * @since 7/31/2018
 */
public final class InputOverrideHandler extends Behavior implements IInputOverrideHandler {
    private static final float COMPATIBILITY_KEY_THRESHOLD = 0.3F;

    /**
     * Maps inputs to whether or not we are forcing their state down.
     */
    private final Map<Input, Boolean> inputForceStateMap = new HashMap<>();
    private final Map<Integer, Boolean> compatibilityMovementStateMap = new HashMap<>();

    private final BlockBreakHelper blockBreakHelper;
    private final BlockPlaceHelper blockPlaceHelper;

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
        this.blockBreakHelper = new BlockBreakHelper(baritone.getPlayerContext());
        this.blockPlaceHelper = new BlockPlaceHelper(baritone.getPlayerContext());
    }

    /**
     * Returns whether or not we are forcing down the specified {@link Input}.
     *
     * @param input The input
     * @return Whether or not it is being forced down
     */
    @Override
    public final boolean isInputForcedDown(Input input) {
        return input == null ? false : this.inputForceStateMap.getOrDefault(input, false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    @Override
    public final void setInputForceState(Input input, boolean forced) {
        this.inputForceStateMap.put(input, forced);
    }

    /**
     * Clears the override state for all keys
     */
    @Override
    public final void clearAllKeys() {
        this.inputForceStateMap.clear();
        releaseCompatibilityMovementKeys();
    }

    @Override
    public final void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (ctx.minecraft() == null || ctx.minecraft().options == null) {
            return;
        }
        if (ctx.player() == null) {
            releaseCompatibilityMovementKeys();
            return;
        }
        if (isInputForcedDown(Input.CLICK_LEFT)) {
            setInputForceState(Input.CLICK_RIGHT, false);
        }
        blockBreakHelper.tick(isInputForcedDown(Input.CLICK_LEFT));
        blockPlaceHelper.tick(isInputForcedDown(Input.CLICK_RIGHT));

        if (shouldUseCompatibilityWalkMode()) {
            if (inControl()) {
                ensureKeyboardInput(true);
                syncCompatibilityMovementKeys();
            } else {
                releaseCompatibilityMovementKeys();
                ensureKeyboardInput(false);
            }
        } else {
            releaseCompatibilityMovementKeys();
            if (inControl()) {
                if (ctx.player().input.getClass() != PlayerMovementInput.class) {
                    ctx.player().input = new PlayerMovementInput(this);
                }
            } else {
                if (ctx.player().input.getClass() == PlayerMovementInput.class) { // allow other movement inputs that aren't this one, e.g. for a freecam
                    ctx.player().input = new KeyboardInput(ctx.minecraft().options);
                }
            }
        }
        // only set it if it was previously incorrect
        // gotta do it this way, or else it constantly thinks you're beginning a double tap W sprint lol
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        releaseCompatibilityMovementKeys();
    }

    private boolean inControl() {
        for (Input input : new Input[]{Input.MOVE_FORWARD, Input.MOVE_BACK, Input.MOVE_LEFT, Input.MOVE_RIGHT, Input.SNEAK, Input.JUMP}) {
            if (isInputForcedDown(input)) {
                return true;
            }
        }
        // if we are not primary (a bot) we should set the movementinput even when idle (not pathing)
        return baritone.getPathingBehavior().isPathing() || baritone != BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    public BlockBreakHelper getBlockBreakHelper() {
        return blockBreakHelper;
    }

    float getPlayerYaw() {
        return ctx.player().getYRot();
    }

    float getMovementYaw() {
        return baritone.getLookBehavior()
                .getEffectiveRotation()
                .map(rotation -> rotation.getYaw())
                .orElseGet(this::getPlayerYaw);
    }

    private boolean shouldUseCompatibilityWalkMode() {
        return Baritone.settings().compatibilityWalkMode.value;
    }

    private void ensureKeyboardInput(boolean forceReplaceCustomInput) {
        if (ctx.player().input.getClass() == KeyboardInput.class) {
            return;
        }
        if (!forceReplaceCustomInput && ctx.player().input.getClass() != PlayerMovementInput.class) {
            return;
        }
        if (ctx.minecraft() != null && ctx.minecraft().options != null) {
            ctx.player().input = new KeyboardInput(ctx.minecraft().options);
        }
    }

    private void syncCompatibilityMovementKeys() {
        if (ctx.minecraft() == null || ctx.minecraft().options == null) {
            return;
        }
        float desiredForward = 0.0F;
        float desiredStrafe = 0.0F;

        boolean jump = isInputForcedDown(Input.JUMP);
        boolean sneak = isInputForcedDown(Input.SNEAK);

        if (isInputForcedDown(Input.MOVE_FORWARD)) {
            desiredForward += 1.0F;
        }
        if (isInputForcedDown(Input.MOVE_BACK)) {
            desiredForward -= 1.0F;
        }
        if (isInputForcedDown(Input.MOVE_LEFT)) {
            desiredStrafe += 1.0F;
        }
        if (isInputForcedDown(Input.MOVE_RIGHT)) {
            desiredStrafe -= 1.0F;
        }

        setCompatibilityMovementKey(ctx.minecraft().options.keyUp, desiredForward > COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyDown, desiredForward < -COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyLeft, desiredStrafe > COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyRight, desiredStrafe < -COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyJump, jump);
        setCompatibilityMovementKey(ctx.minecraft().options.keyShift, sneak);
    }

    private void releaseCompatibilityMovementKeys() {
        if (ctx.minecraft() == null || ctx.minecraft().options == null) {
            return;
        }
        setCompatibilityMovementKey(ctx.minecraft().options.keyUp, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyDown, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyLeft, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyRight, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyJump, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyShift, false);
    }

    private void setCompatibilityMovementKey(KeyMapping keyMapping, boolean down) {
        if (keyMapping == null) {
            return;
        }
        InputConstants.Key key = keyMapping.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN) || key.getType() != InputConstants.Type.KEYSYM) {
            return;
        }
        int legacyKeyCode = Keyboard.fromGlfwKey(key.getValue());
        if (legacyKeyCode == Keyboard.KEY_NONE) {
            return;
        }
        Boolean previous = compatibilityMovementStateMap.get(legacyKeyCode);
        if (previous != null && previous == down) {
            return;
        }
        compatibilityMovementStateMap.put(legacyKeyCode, down);
        SimulatedKeyInputManager.simulateKeyCode(legacyKeyCode, down ? "Down" : "Up");
    }
}

