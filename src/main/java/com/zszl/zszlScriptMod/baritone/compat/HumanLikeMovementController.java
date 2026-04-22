package com.zszl.zszlScriptMod.baritone.compat;

import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;

public final class HumanLikeMovementController {

    public static final HumanLikeMovementController INSTANCE = new HumanLikeMovementController();

    private HumanLikeMovementController() {
    }

    public boolean isEnabled() {
        return HumanLikeMovementConfig.INSTANCE != null && HumanLikeMovementConfig.INSTANCE.enabled;
    }

    public void reset() {
    }

    public MovementState applyMovement(float desiredForward, float desiredStrafe, boolean jump, boolean sneak,
            float yawDifferenceDeg, double playerX, double playerY, double playerZ, boolean onGround,
            float finalApproachProgress, float narrowPassageFactor, float straightPathFactor,
            float obstacleEdgeBias) {
        return new MovementState(desiredForward, desiredStrafe, jump, sneak);
    }

    public RotationState smoothRotation(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        return new RotationState(targetYaw, targetPitch);
    }

    public static final class MovementState {
        public final float moveForward;
        public final float moveStrafe;
        public final boolean jump;
        public final boolean sneak;

        public MovementState(float moveForward, float moveStrafe, boolean jump, boolean sneak) {
            this.moveForward = moveForward;
            this.moveStrafe = moveStrafe;
            this.jump = jump;
            this.sneak = sneak;
        }
    }

    public static final class RotationState {
        public final float yaw;
        public final float pitch;

        public RotationState(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}

