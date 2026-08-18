package com.zszl.zszlScriptMod.baritone.compat;

import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import net.minecraft.util.Mth;

import java.util.concurrent.ThreadLocalRandom;

public final class HumanLikeMovementController {

    public static final HumanLikeMovementController INSTANCE = new HumanLikeMovementController();

    private static final float MIN_INPUT_EPSILON = 0.001F;
    private static final double STUCK_MOVEMENT_EPSILON_SQ = 0.0025D;

    private float smoothedForward;
    private float smoothedStrafe;
    private float rhythmPhase;
    private float activeStrafeJitter;
    private float stuckRecoveryDirection = 1.0F;
    private float corridorBiasDirection = 1.0F;
    private int microPauseTicksRemaining;
    private int lightHopCooldownRemaining;
    private int strafeJitterTicksRemaining;
    private int stuckTicks;
    private int stuckRecoveryTicksRemaining;
    private int corridorBiasRetargetTicks;
    private double lastTrackedX;
    private double lastTrackedY;
    private double lastTrackedZ;
    private boolean lastPositionInitialized;

    private HumanLikeMovementController() {
    }

    public boolean isEnabled() {
        return HumanLikeMovementConfig.INSTANCE != null && HumanLikeMovementConfig.INSTANCE.enabled;
    }

    public void reset() {
        smoothedForward = 0.0F;
        smoothedStrafe = 0.0F;
        rhythmPhase = 0.0F;
        activeStrafeJitter = 0.0F;
        stuckRecoveryDirection = 1.0F;
        corridorBiasDirection = 1.0F;
        microPauseTicksRemaining = 0;
        lightHopCooldownRemaining = 0;
        strafeJitterTicksRemaining = 0;
        stuckTicks = 0;
        stuckRecoveryTicksRemaining = 0;
        corridorBiasRetargetTicks = 0;
        lastTrackedX = 0.0D;
        lastTrackedY = 0.0D;
        lastTrackedZ = 0.0D;
        lastPositionInitialized = false;
    }

    public MovementState applyMovement(float desiredForward, float desiredStrafe, boolean jump, boolean sneak,
            float yawDifferenceDeg, double playerX, double playerY, double playerZ, boolean onGround,
            float finalApproachProgress, float narrowPassageFactor, float straightPathFactor,
            float obstacleEdgeBias) {
        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        if (config == null || !config.enabled) {
            reset();
            return new MovementState(desiredForward, desiredStrafe, jump, sneak);
        }

        if (lightHopCooldownRemaining > 0) {
            lightHopCooldownRemaining--;
        }
        if (corridorBiasRetargetTicks > 0) {
            corridorBiasRetargetTicks--;
        }

        float requestedForward = clampInput(desiredForward);
        float requestedStrafe = clampInput(desiredStrafe);
        boolean hasRequestedMovement = Math.abs(requestedForward) > MIN_INPUT_EPSILON
                || Math.abs(requestedStrafe) > MIN_INPUT_EPSILON;

        updateStuckTracking(playerX, playerY, playerZ, onGround, hasRequestedMovement, config);

        if (!hasRequestedMovement) {
            microPauseTicksRemaining = 0;
            strafeJitterTicksRemaining = 0;
            activeStrafeJitter = 0.0F;
            stuckRecoveryTicksRemaining = 0;
            smoothedForward = approachAxis(smoothedForward, 0.0F, config.deceleration, config.deceleration);
            smoothedStrafe = approachAxis(smoothedStrafe, 0.0F, config.deceleration, config.deceleration);
            return new MovementState(smoothedForward, smoothedStrafe, jump, sneak);
        }

        if (microPauseTicksRemaining > 0) {
            microPauseTicksRemaining--;
            smoothedForward = approachAxis(smoothedForward, 0.0F, config.deceleration, config.deceleration);
            smoothedStrafe = approachAxis(smoothedStrafe, 0.0F, config.deceleration, config.deceleration);
            return new MovementState(smoothedForward, smoothedStrafe, false, sneak);
        }

        if (shouldStartMicroPause(config, onGround, straightPathFactor, finalApproachProgress)) {
            microPauseTicksRemaining = sampleTickRange(config.microPauseMinTicks, config.microPauseMaxTicks);
            smoothedForward = approachAxis(smoothedForward, 0.0F, config.deceleration, config.deceleration);
            smoothedStrafe = approachAxis(smoothedStrafe, 0.0F, config.deceleration, config.deceleration);
            return new MovementState(smoothedForward, smoothedStrafe, false, sneak);
        }

        float adjustedForward = requestedForward;
        float adjustedStrafe = requestedStrafe;

        float absYawDifference = Math.abs(yawDifferenceDeg);
        if (onGround && config.startTurnThreshold > 0.0F && absYawDifference > config.startTurnThreshold
                && requestedForward > 0.0F) {
            float turnSuppression = Mth.clamp(
                    (absYawDifference - config.startTurnThreshold)
                            / Math.max(1.0F, 90.0F - config.startTurnThreshold),
                    0.0F,
                    1.0F);
            adjustedForward *= 1.0F - turnSuppression;
            adjustedStrafe *= 1.0F - turnSuppression * 0.55F;
        }

        float turnSlowdownFactor = 1.0F - config.turnSlowdown
                * Mth.clamp(absYawDifference / 90.0F, 0.0F, 1.0F);
        float narrowSlowdownFactor = 1.0F - config.narrowSlowdown * Mth.clamp(narrowPassageFactor, 0.0F, 1.0F);
        float finalSlowdownFactor = 1.0F - config.finalApproachSlowdown
                * Mth.clamp(finalApproachProgress, 0.0F, 1.0F);
        float rhythmFactor = computeRhythmFactor(config, straightPathFactor, requestedForward);
        float sharedMultiplier = Mth.clamp(
                turnSlowdownFactor * narrowSlowdownFactor * finalSlowdownFactor * rhythmFactor,
                0.08F,
                1.35F);

        adjustedForward *= sharedMultiplier;
        adjustedStrafe *= sharedMultiplier;

        if (shouldRefreshCorridorBias(straightPathFactor)) {
            corridorBiasDirection = ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F;
            corridorBiasRetargetTicks = sampleTickRange(28, 90);
        }
        adjustedStrafe += computeCorridorBias(config, straightPathFactor, obstacleEdgeBias);

        if (shouldRefreshStrafeJitter(config, straightPathFactor, onGround)) {
            activeStrafeJitter = (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F)
                    * config.strafeJitterStrength;
            strafeJitterTicksRemaining = sampleTickRange(4, 9);
        }
        if (strafeJitterTicksRemaining > 0) {
            adjustedStrafe += activeStrafeJitter * (0.25F + 0.75F * Mth.clamp(straightPathFactor, 0.0F, 1.0F));
            strafeJitterTicksRemaining--;
            if (strafeJitterTicksRemaining <= 0) {
                activeStrafeJitter = 0.0F;
            }
        }

        if (stuckRecoveryTicksRemaining > 0) {
            adjustedForward = Math.max(adjustedForward, 0.42F);
            adjustedStrafe += stuckRecoveryDirection * config.stuckRecoveryStrafeStrength;
            if (onGround && !jump && lightHopCooldownRemaining <= 0
                    && ThreadLocalRandom.current().nextFloat() < 0.18F) {
                jump = true;
                lightHopCooldownRemaining = config.lightHopCooldownTicks;
            }
            stuckRecoveryTicksRemaining--;
        }

        smoothedForward = approachAxis(smoothedForward, adjustedForward, config.acceleration, config.deceleration);
        smoothedStrafe = approachAxis(smoothedStrafe, adjustedStrafe, config.acceleration, config.deceleration);

        float vectorLengthSq = smoothedForward * smoothedForward + smoothedStrafe * smoothedStrafe;
        if (vectorLengthSq > 1.0F) {
            float inverseLength = Mth.invSqrt(vectorLengthSq);
            smoothedForward *= inverseLength;
            smoothedStrafe *= inverseLength;
        }

        if (!jump && shouldLightHop(config, onGround, straightPathFactor, absYawDifference, requestedForward,
                finalApproachProgress)) {
            jump = true;
            lightHopCooldownRemaining = config.lightHopCooldownTicks;
        }

        if (Math.abs(smoothedForward) < MIN_INPUT_EPSILON) {
            smoothedForward = 0.0F;
        }
        if (Math.abs(smoothedStrafe) < MIN_INPUT_EPSILON) {
            smoothedStrafe = 0.0F;
        }

        return new MovementState(smoothedForward, smoothedStrafe, jump, sneak);
    }

    public RotationState smoothRotation(float currentYaw, float currentPitch, float targetYaw, float targetPitch) {
        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        if (config == null || !config.enabled) {
            return new RotationState(targetYaw, targetPitch);
        }

        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;
        float absYaw = Math.abs(yawDelta);
        float absPitch = Math.abs(pitchDelta);

        float turnProgress = Mth.clamp(absYaw / 120.0F, 0.0F, 1.0F);
        float turnSpeed = Mth.lerp(turnProgress * turnProgress, config.minTurnSpeed, config.maxTurnSpeed);
        float pitchTurnSpeed = Math.max(0.5F, turnSpeed * 0.75F);

        float overshootYaw = computeOvershoot(config.turnOvershoot, absYaw, 42.0F);
        float overshootPitch = computeOvershoot(config.turnOvershoot * 0.65F, absPitch, 30.0F);
        float jitterWeight = Mth.clamp(Math.max(absYaw, absPitch) / 25.0F, 0.0F, 1.0F);
        float yawJitter = randomSigned(config.viewJitter) * jitterWeight;
        float pitchJitter = randomSigned(config.viewJitter * 0.6F) * jitterWeight;

        float desiredYaw = targetYaw + Math.copySign(overshootYaw, yawDelta) + yawJitter;
        float desiredPitch = targetPitch + Math.copySign(overshootPitch, pitchDelta) + pitchJitter;

        float newYaw = currentYaw + clampSigned(Mth.wrapDegrees(desiredYaw - currentYaw), turnSpeed);
        float newPitch = currentPitch + clampSigned(desiredPitch - currentPitch, pitchTurnSpeed);

        return new RotationState(Mth.wrapDegrees(newYaw), Mth.clamp(newPitch, -90.0F, 90.0F));
    }

    private void updateStuckTracking(double playerX, double playerY, double playerZ, boolean onGround,
            boolean hasRequestedMovement, HumanLikeMovementConfig config) {
        if (!lastPositionInitialized) {
            lastTrackedX = playerX;
            lastTrackedY = playerY;
            lastTrackedZ = playerZ;
            lastPositionInitialized = true;
            stuckTicks = 0;
            return;
        }

        double deltaX = playerX - lastTrackedX;
        double deltaZ = playerZ - lastTrackedZ;
        double horizontalMovementSq = deltaX * deltaX + deltaZ * deltaZ;
        if (hasRequestedMovement && onGround && horizontalMovementSq < STUCK_MOVEMENT_EPSILON_SQ) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }

        lastTrackedX = playerX;
        lastTrackedY = playerY;
        lastTrackedZ = playerZ;

        if (config.enableStuckRecovery && stuckRecoveryTicksRemaining <= 0 && hasRequestedMovement && onGround
                && stuckTicks >= config.stuckRecoveryTicks) {
            stuckTicks = 0;
            stuckRecoveryTicksRemaining = sampleTickRange(config.stuckRecoveryMinTicks, config.stuckRecoveryMaxTicks);
            stuckRecoveryDirection = ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F;
            activeStrafeJitter = 0.0F;
            strafeJitterTicksRemaining = 0;
            microPauseTicksRemaining = 0;
        }
    }

    private boolean shouldStartMicroPause(HumanLikeMovementConfig config, boolean onGround, float straightPathFactor,
            float finalApproachProgress) {
        if (!onGround || config.microPauseChance <= 0.0F) {
            return false;
        }
        if (straightPathFactor < 0.45F || finalApproachProgress > 0.82F) {
            return false;
        }
        return ThreadLocalRandom.current().nextFloat() < config.microPauseChance;
    }

    private float computeRhythmFactor(HumanLikeMovementConfig config, float straightPathFactor, float requestedForward) {
        if (config.rhythmVariation <= 0.0F || Math.abs(requestedForward) <= MIN_INPUT_EPSILON) {
            return 1.0F;
        }
        rhythmPhase += 0.18F + 0.16F * Mth.clamp(straightPathFactor, 0.0F, 1.0F);
        float wave = Mth.sin(rhythmPhase);
        return 1.0F + wave * config.rhythmVariation * Mth.clamp(straightPathFactor, 0.0F, 1.0F);
    }

    private boolean shouldRefreshCorridorBias(float straightPathFactor) {
        return straightPathFactor > 0.45F && corridorBiasRetargetTicks <= 0;
    }

    private float computeCorridorBias(HumanLikeMovementConfig config, float straightPathFactor, float obstacleEdgeBias) {
        if (config.corridorBiasStrength <= 0.0F || straightPathFactor <= 0.2F) {
            return 0.0F;
        }
        float sideAvailability = corridorBiasDirection > 0.0F
                ? Mth.clamp(0.5F + obstacleEdgeBias * 0.5F, 0.1F, 1.0F)
                : Mth.clamp(0.5F - obstacleEdgeBias * 0.5F, 0.1F, 1.0F);
        float straightness = Mth.clamp(straightPathFactor, 0.0F, 1.0F);
        return corridorBiasDirection * config.corridorBiasStrength * straightness * sideAvailability;
    }

    private boolean shouldRefreshStrafeJitter(HumanLikeMovementConfig config, float straightPathFactor,
            boolean onGround) {
        if (!onGround || config.strafeJitterChance <= 0.0F || config.strafeJitterStrength <= 0.0F) {
            return false;
        }
        if (strafeJitterTicksRemaining > 0 || straightPathFactor <= 0.25F) {
            return false;
        }
        return ThreadLocalRandom.current().nextFloat() < config.strafeJitterChance;
    }

    private boolean shouldLightHop(HumanLikeMovementConfig config, boolean onGround, float straightPathFactor,
            float absYawDifference, float requestedForward, float finalApproachProgress) {
        if (!onGround || config.lightHopChance <= 0.0F || lightHopCooldownRemaining > 0) {
            return false;
        }
        if (straightPathFactor < 0.58F || requestedForward <= 0.12F || absYawDifference > 15.0F
                || finalApproachProgress > 0.88F) {
            return false;
        }
        return ThreadLocalRandom.current().nextFloat() < config.lightHopChance;
    }

    private float approachAxis(float current, float target, float acceleration, float deceleration) {
        float delta = target - current;
        if (Math.abs(delta) <= MIN_INPUT_EPSILON) {
            return target;
        }
        boolean accelerating = Math.abs(target) > Math.abs(current)
                || Math.signum(target) == Math.signum(current)
                || Math.abs(target) > MIN_INPUT_EPSILON;
        float step = accelerating ? acceleration : deceleration;
        if (Math.abs(target) < Math.abs(current) || Math.signum(target) != Math.signum(current)) {
            step = deceleration;
        }
        return current + clampSigned(delta, Math.max(0.01F, step));
    }

    private float clampInput(float value) {
        return Mth.clamp(value, -1.0F, 1.0F);
    }

    private float clampSigned(float value, float maxMagnitude) {
        if (maxMagnitude <= 0.0F) {
            return 0.0F;
        }
        return Math.copySign(Math.min(Math.abs(value), maxMagnitude), value);
    }

    private float computeOvershoot(float baseStrength, float deltaAbs, float divisor) {
        if (baseStrength <= 0.0F || deltaAbs <= 4.0F) {
            return 0.0F;
        }
        float normalized = Mth.clamp(deltaAbs / divisor, 0.0F, 1.0F);
        return baseStrength * normalized * normalized;
    }

    private float randomSigned(float magnitude) {
        if (magnitude <= 0.0F) {
            return 0.0F;
        }
        return (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * magnitude;
    }

    private int sampleTickRange(int minTicks, int maxTicks) {
        if (maxTicks <= minTicks) {
            return minTicks;
        }
        return ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1);
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

