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

import com.zszl.zszlScriptMod.shadowbaritone.api.utils.input.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public class PlayerMovementInput extends net.minecraft.client.player.ClientInput {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tick() {
        boolean desiredJump = handler.isInputForcedDown(Input.JUMP);
        boolean desiredSneak = handler.isInputForcedDown(Input.SNEAK);
        float desiredForward = 0.0F;
        float desiredStrafe = 0.0F;

        boolean forward = handler.isInputForcedDown(Input.MOVE_FORWARD);
        boolean backward = handler.isInputForcedDown(Input.MOVE_BACK);
        boolean left = handler.isInputForcedDown(Input.MOVE_LEFT);
        boolean right = handler.isInputForcedDown(Input.MOVE_RIGHT);

        if (forward) {
            desiredForward++;
        }

        if (backward) {
            desiredForward--;
        }

        if (left) {
            desiredStrafe++;
        }

        if (right) {
            desiredStrafe--;
        }

        boolean decoupleMovementFromVisualYaw = handler.baritone.getLookBehavior().shouldDecoupleMovementFromVisualYaw();
        if (decoupleMovementFromVisualYaw && (desiredForward != 0.0F || desiredStrafe != 0.0F)) {
            float yawDifferenceDeg = Mth.wrapDegrees(handler.getMovementYaw() - handler.getPlayerYaw());
            float yawDelta = (float) Math.toRadians(yawDifferenceDeg);
            float sin = Mth.sin(yawDelta);
            float cos = Mth.cos(yawDelta);
            float rawStrafe = desiredStrafe;
            float rawForward = desiredForward;

            desiredStrafe = rawStrafe * cos - rawForward * sin;
            desiredForward = rawStrafe * sin + rawForward * cos;
        }

        if (desiredSneak) {
            desiredStrafe *= 0.3F;
            desiredForward *= 0.3F;
        }

        this.moveVector = new Vec2(desiredStrafe, desiredForward);
        this.keyPresses = new net.minecraft.world.entity.player.Input(forward, backward, left, right, desiredJump, desiredSneak, false);
    }
}

