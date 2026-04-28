package com.zszl.zszlScriptMod.baritone.compat;

import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;

import java.util.Random;

public final class HumanLikeMovementController {

    public static final HumanLikeMovementController INSTANCE = new HumanLikeMovementController();

    private final Random random = new Random();

    private float smoothedForward;
    private float smoothedStrafe;
    private float finalApproachProgress;
    private float narrowPassageFactor;
    private float straightPathFactor;
    private float obstacleEdgeBias;
    private float corridorBias;
    private float recoveryStrafeSign = 1.0F;
    private int microPauseTicks;
    private int lightHopCooldownTicks;
    private int strafeJitterTicks;
    private int corridorBiasTicks;
    private int stuckTicks;
    private int stuckRecoveryTicks;
    private int blockBreakHesitationTicks;
    private int rhythmTicks;
    private boolean hasLastPosition;
    private double lastX;
    private double lastY;
    private double lastZ;

    private HumanLikeMovementController() {
    }

    public boolean isEnabled() {
        return HumanLikeMovementConfig.INSTANCE != null && HumanLikeMovementConfig.INSTANCE.enabled;
    }

    public void reset() {
        this.smoothedForward = 0.0F;
        this.smoothedStrafe = 0.0F;
        this.finalApproachProgress = 0.0F;
        this.narrowPassageFactor = 0.0F;
        this.straightPathFactor = 0.0F;
        this.obstacleEdgeBias = 0.0F;
        this.corridorBias = 0.0F;
        this.recoveryStrafeSign = 1.0F;
        this.microPauseTicks = 0;
        this.lightHopCooldownTicks = 0;
        this.strafeJitterTicks = 0;
        this.corridorBiasTicks = 0;
        this.stuckTicks = 0;
        this.stuckRecoveryTicks = 0;
        this.blockBreakHesitationTicks = 0;
        this.rhythmTicks = 0;
        this.hasLastPosition = false;
    }

    public void observePathContext(int pathPosition, int pathLength, double distanceToMovementDest,
            double distanceToPathEnd, float narrowPassageFactor, float straightPathFactor,
            float obstacleEdgeBias) {
        if (!isEnabled()) {
            return;
        }

        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        config.normalize();

        float distanceProgress = config.finalApproachDistance <= 0.0F ? 0.0F
                : 1.0F - clamp((float) (distanceToPathEnd / Math.max(0.1F, config.finalApproachDistance)), 0.0F,
                        1.0F);
        float pathProgress = pathLength <= 1 ? 1.0F
                : 1.0F - clamp((float) (pathLength - pathPosition - 1) / 6.0F, 0.0F, 1.0F);
        float movementProgress = 1.0F - clamp((float) distanceToMovementDest, 0.0F, 1.0F);

        this.finalApproachProgress = clamp(Math.max(distanceProgress, Math.max(pathProgress * 0.6F,
                movementProgress * 0.35F)), 0.0F, 1.0F);
        this.narrowPassageFactor = clamp(narrowPassageFactor, 0.0F, 1.0F);
        this.straightPathFactor = clamp(straightPathFactor, 0.0F, 1.0F);
        this.obstacleEdgeBias = clamp(obstacleEdgeBias, -1.0F, 1.0F);
    }

    public MovementState applyMovement(float desiredForward, float desiredStrafe, boolean jump, boolean sneak,
            float yawDifferenceDeg, double playerX, double playerY, double playerZ, boolean onGround,
            float finalApproachProgress, float narrowPassageFactor, float straightPathFactor,
            float obstacleEdgeBias) {
        if (!isEnabled()) {
            return new MovementState(desiredForward, desiredStrafe, jump, sneak);
        }

        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        config.normalize();

        this.rhythmTicks++;
        if (this.lightHopCooldownTicks > 0) {
            this.lightHopCooldownTicks--;
        }

        float effectiveFinalApproach = Math.max(this.finalApproachProgress, clamp(finalApproachProgress, 0.0F, 1.0F));
        float effectiveNarrow = Math.max(this.narrowPassageFactor, clamp(narrowPassageFactor, 0.0F, 1.0F));
        float effectiveStraight = Math.max(this.straightPathFactor, clamp(straightPathFactor, 0.0F, 1.0F));
        float effectiveEdgeBias = clamp(this.obstacleEdgeBias + obstacleEdgeBias, -1.0F, 1.0F);
        boolean wantsMovement = Math.abs(desiredForward) > 0.01F || Math.abs(desiredStrafe) > 0.01F;

        updateStuckState(config, wantsMovement, playerX, playerY, playerZ);

        if (this.microPauseTicks > 0) {
            this.microPauseTicks--;
            approachMovement(0.0F, 0.0F, config.deceleration * 1.35F, config.deceleration * 1.35F);
            return new MovementState(this.smoothedForward, this.smoothedStrafe, false, sneak);
        }

        if (wantsMovement && onGround && !jump && !sneak && this.stuckRecoveryTicks <= 0) {
            float pauseChance = config.microPauseChance * (0.75F + effectiveStraight * 0.45F)
                    * (1.0F - effectiveFinalApproach * 0.55F);
            if (this.random.nextFloat() < pauseChance) {
                int span = Math.max(0, config.microPauseMaxTicks - config.microPauseMinTicks);
                this.microPauseTicks = config.microPauseMinTicks + (span == 0 ? 0 : this.random.nextInt(span + 1));
            }
        }

        float targetForward = desiredForward;
        float targetStrafe = desiredStrafe;

        float turnAmount = clamp((Math.abs(yawDifferenceDeg) - config.startTurnThreshold) / 90.0F, 0.0F, 1.0F);
        float turnScale = 1.0F - config.turnSlowdown * turnAmount;
        float narrowScale = 1.0F - config.narrowSlowdown * effectiveNarrow;
        float finalScale = 1.0F - config.finalApproachSlowdown * effectiveFinalApproach;
        float rhythmScale = 1.0F + (float) Math.sin(this.rhythmTicks * 0.23D) * config.rhythmVariation
                * effectiveStraight * (1.0F - effectiveFinalApproach * 0.5F);

        targetForward *= turnScale * narrowScale * finalScale * rhythmScale;
        targetStrafe *= Math.max(0.35F, narrowScale * finalScale);

        if (wantsMovement) {
            updateCorridorBias(config, effectiveStraight, effectiveFinalApproach, effectiveEdgeBias);
            targetStrafe += this.corridorBias;
        } else {
            this.corridorBias *= 0.85F;
        }

        if (wantsMovement && this.strafeJitterTicks <= 0
                && this.random.nextFloat() < config.strafeJitterChance * (0.35F + effectiveStraight * 0.35F)) {
            this.strafeJitterTicks = 2 + this.random.nextInt(4);
            this.recoveryStrafeSign = this.random.nextBoolean() ? 1.0F : -1.0F;
        }
        if (this.strafeJitterTicks > 0) {
            this.strafeJitterTicks--;
            targetStrafe += this.recoveryStrafeSign * config.strafeJitterStrength
                    * (0.18F + this.random.nextFloat() * 0.28F)
                    * (1.0F - effectiveFinalApproach * 0.45F);
        }

        if (this.stuckRecoveryTicks > 0) {
            this.stuckRecoveryTicks--;
            targetForward = Math.max(targetForward, 0.42F);
            targetStrafe += this.recoveryStrafeSign * config.stuckRecoveryStrafeStrength;
            if (onGround && this.random.nextFloat() < 0.18F) {
                jump = true;
            }
        }

        if (wantsMovement && onGround && !jump && this.lightHopCooldownTicks <= 0
                && effectiveStraight > 0.55F && effectiveFinalApproach < 0.75F
                && this.random.nextFloat() < config.lightHopChance * 0.018F) {
            jump = true;
            this.lightHopCooldownTicks = config.lightHopCooldownTicks + this.random.nextInt(8);
        }

        approachMovement(targetForward, targetStrafe, config.acceleration, config.deceleration);
        return new MovementState(clamp(this.smoothedForward, -1.0F, 1.0F),
                clamp(this.smoothedStrafe, -1.0F, 1.0F), jump, sneak);
    }

    public RotationState smoothRotation(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        if (!isEnabled()) {
            return new RotationState(targetYaw, targetPitch);
        }

        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        config.normalize();

        float yawDelta = wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;
        float largestDelta = Math.max(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (largestDelta < 0.001F) {
            return new RotationState(targetYaw, targetPitch);
        }

        float maxStep = randomRange(config.minTurnSpeed, config.maxTurnSpeed);
        float factor = clamp(maxStep / Math.max(1.0F, largestDelta), 0.12F, 0.86F);
        float eased = factor * factor * (3.0F - 2.0F * factor);

        float yaw = currentYaw + yawDelta * eased;
        float pitch = currentPitch + pitchDelta * eased;

        if (largestDelta > 8.0F && config.turnOvershoot > 0.0F && this.random.nextFloat() < 0.18F) {
            yaw += Math.signum(yawDelta) * config.turnOvershoot * this.random.nextFloat();
            pitch += Math.signum(pitchDelta) * config.turnOvershoot * 0.45F * this.random.nextFloat();
        }

        float jitter = config.viewJitter * 0.035F;
        yaw += (float) this.random.nextGaussian() * jitter;
        pitch += (float) this.random.nextGaussian() * jitter * 0.65F;

        return new RotationState(yaw, clamp(pitch, -90.0F, 90.0F));
    }

    public boolean shouldDelayBlockBreakTick(boolean alreadyBreaking) {
        if (!isEnabled() || alreadyBreaking) {
            return false;
        }

        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        config.normalize();

        if (this.blockBreakHesitationTicks > 0) {
            this.blockBreakHesitationTicks--;
            return true;
        }

        if (this.random.nextFloat() < config.microPauseChance * 0.32F) {
            this.blockBreakHesitationTicks = 1 + this.random.nextInt(2);
            return true;
        }
        return false;
    }

    public float getFinalApproachProgress() {
        return finalApproachProgress;
    }

    public float getNarrowPassageFactor() {
        return narrowPassageFactor;
    }

    public float getStraightPathFactor() {
        return straightPathFactor;
    }

    public float getObstacleEdgeBias() {
        return obstacleEdgeBias;
    }

    private void updateStuckState(HumanLikeMovementConfig config, boolean wantsMovement,
            double playerX, double playerY, double playerZ) {
        if (!wantsMovement || !config.enableStuckRecovery) {
            this.stuckTicks = 0;
            this.hasLastPosition = true;
            this.lastX = playerX;
            this.lastY = playerY;
            this.lastZ = playerZ;
            return;
        }

        if (this.hasLastPosition) {
            double dx = playerX - this.lastX;
            double dy = playerY - this.lastY;
            double dz = playerZ - this.lastZ;
            double movedSq = dx * dx + dy * dy + dz * dz;
            if (movedSq < 1.0E-4D) {
                this.stuckTicks++;
            } else {
                this.stuckTicks = Math.max(0, this.stuckTicks - 2);
            }
        }

        this.hasLastPosition = true;
        this.lastX = playerX;
        this.lastY = playerY;
        this.lastZ = playerZ;

        if (this.stuckTicks >= config.stuckRecoveryTicks && this.stuckRecoveryTicks <= 0) {
            int span = Math.max(0, config.stuckRecoveryMaxTicks - config.stuckRecoveryMinTicks);
            this.stuckRecoveryTicks = config.stuckRecoveryMinTicks
                    + (span == 0 ? 0 : this.random.nextInt(span + 1));
            this.recoveryStrafeSign = this.random.nextBoolean() ? 1.0F : -1.0F;
            this.stuckTicks = 0;
        }
    }

    private void updateCorridorBias(HumanLikeMovementConfig config, float straightFactor,
            float finalApproach, float edgeBias) {
        if (this.corridorBiasTicks <= 0) {
            this.corridorBiasTicks = 25 + this.random.nextInt(56);
            float randomBias = (this.random.nextBoolean() ? 1.0F : -1.0F) * config.corridorBiasStrength
                    * (0.35F + this.random.nextFloat() * 0.65F);
            this.corridorBias = randomBias;
        } else {
            this.corridorBiasTicks--;
        }

        float targetBias = (this.corridorBias + edgeBias * config.corridorBiasStrength * 0.45F)
                * straightFactor * (1.0F - finalApproach * 0.7F);
        this.corridorBias += (targetBias - this.corridorBias) * 0.08F;
    }

    private void approachMovement(float targetForward, float targetStrafe, float acceleration, float deceleration) {
        float forwardRate = Math.abs(targetForward) > Math.abs(this.smoothedForward) ? acceleration : deceleration;
        float strafeRate = Math.abs(targetStrafe) > Math.abs(this.smoothedStrafe) ? acceleration : deceleration;
        this.smoothedForward += (targetForward - this.smoothedForward) * clamp(forwardRate, 0.01F, 1.0F);
        this.smoothedStrafe += (targetStrafe - this.smoothedStrafe) * clamp(strafeRate, 0.01F, 1.0F);

        if (Math.abs(targetForward) < 0.01F && Math.abs(this.smoothedForward) < 0.035F) {
            this.smoothedForward = 0.0F;
        }
        if (Math.abs(targetStrafe) < 0.01F && Math.abs(this.smoothedStrafe) < 0.035F) {
            this.smoothedStrafe = 0.0F;
        }
    }

    private float randomRange(float min, float max) {
        if (max <= min) {
            return min;
        }
        return min + this.random.nextFloat() * (max - min);
    }

    private static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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

