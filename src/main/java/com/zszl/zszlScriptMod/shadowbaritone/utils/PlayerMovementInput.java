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

import com.zszl.zszlScriptMod.baritone.compat.HumanLikeMovementController;
import net.minecraft.world.phys.Vec2;

public class PlayerMovementInput extends net.minecraft.client.player.ClientInput {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tick() {
        HumanLikeMovementController.MovementState movementState = handler.getMovementState();
        float desiredForward = movementState.moveForward;
        float desiredStrafe = movementState.moveStrafe;

        if (movementState.sneak) {
            desiredStrafe *= 0.3F;
            desiredForward *= 0.3F;
        }

        boolean forward = desiredForward > 0.12F;
        boolean backward = desiredForward < -0.12F;
        boolean left = desiredStrafe > 0.12F;
        boolean right = desiredStrafe < -0.12F;

        this.moveVector = new Vec2(desiredStrafe, desiredForward);
        this.keyPresses = new net.minecraft.world.entity.player.Input(
                forward,
                backward,
                left,
                right,
                movementState.jump,
                movementState.sneak,
                false);
    }
}

