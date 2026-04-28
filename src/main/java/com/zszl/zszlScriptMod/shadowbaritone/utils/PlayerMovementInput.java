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
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.input.Input;
import net.minecraft.util.Mth;

public class PlayerMovementInput extends net.minecraft.client.player.Input {

    private final InputOverrideHandler handler;

    PlayerMovementInput(InputOverrideHandler handler) {
        this.handler = handler;
    }

    @Override
    public void tick(boolean p_225607_1_, float f) {
        this.leftImpulse = 0.0F;
        this.forwardImpulse = 0.0F;

        boolean desiredJump = handler.isInputForcedDown(Input.JUMP);
        boolean desiredSneak = handler.isInputForcedDown(Input.SNEAK);
        float desiredForward = 0.0F;
        float desiredStrafe = 0.0F;

        if (this.up = handler.isInputForcedDown(Input.MOVE_FORWARD)) {
            desiredForward++;
        }

        if (this.down = handler.isInputForcedDown(Input.MOVE_BACK)) {
            desiredForward--;
        }

        if (this.left = handler.isInputForcedDown(Input.MOVE_LEFT)) {
            desiredStrafe++;
        }

        if (this.right = handler.isInputForcedDown(Input.MOVE_RIGHT)) {
            desiredStrafe--;
        }

        float yawDifferenceDeg = 0.0F;
        boolean decoupleMovementFromVisualYaw = handler.baritone.getLookBehavior().shouldDecoupleMovementFromVisualYaw();
        if (decoupleMovementFromVisualYaw && (desiredForward != 0.0F || desiredStrafe != 0.0F)) {
            yawDifferenceDeg = Mth.wrapDegrees(handler.getMovementYaw() - handler.getPlayerYaw());
            float yawDelta = (float) Math.toRadians(yawDifferenceDeg);
            float sin = Mth.sin(yawDelta);
            float cos = Mth.cos(yawDelta);
            float rawStrafe = desiredStrafe;
            float rawForward = desiredForward;

            desiredStrafe = rawStrafe * cos - rawForward * sin;
            desiredForward = rawStrafe * sin + rawForward * cos;
        }

        HumanLikeMovementController controller = HumanLikeMovementController.INSTANCE;
        HumanLikeMovementController.MovementState humanMovement = controller.applyMovement(desiredForward,
                desiredStrafe, desiredJump, desiredSneak, yawDifferenceDeg, handler.ctx.player().position().x,
                handler.ctx.player().position().y, handler.ctx.player().position().z, handler.ctx.player().onGround(),
                controller.getFinalApproachProgress(), controller.getNarrowPassageFactor(),
                controller.getStraightPathFactor(), controller.getObstacleEdgeBias());
        desiredForward = humanMovement.moveForward;
        desiredStrafe = humanMovement.moveStrafe;
        desiredJump = humanMovement.jump;
        desiredSneak = humanMovement.sneak;

        this.leftImpulse = desiredStrafe;
        this.forwardImpulse = desiredForward;
        this.jumping = desiredJump;

        if (this.shiftKeyDown = desiredSneak) {
            this.leftImpulse *= 0.3D;
            this.forwardImpulse *= 0.3D;
        }
    }
}

