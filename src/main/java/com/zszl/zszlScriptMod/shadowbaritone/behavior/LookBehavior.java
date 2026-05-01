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

package com.zszl.zszlScriptMod.shadowbaritone.behavior;

import com.zszl.zszlScriptMod.baritone.compat.HumanLikeMovementController;
import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.Settings;
import com.zszl.zszlScriptMod.shadowbaritone.api.behavior.ILookBehavior;
import com.zszl.zszlScriptMod.shadowbaritone.api.behavior.look.IAimProcessor;
import com.zszl.zszlScriptMod.shadowbaritone.api.behavior.look.ITickableAimProcessor;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.*;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.IPlayerContext;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.Rotation;
import com.zszl.zszlScriptMod.shadowbaritone.behavior.look.ForkableRandom;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class LookBehavior extends Behavior implements ILookBehavior {

    private static final float MIN_SMOOTH_LOOK_MAX_TURN_STEP = 0.5F;
    private static final float MAX_SMOOTH_LOOK_MAX_TURN_STEP = 60.0F;
    private static final float DEFAULT_SMOOTH_LOOK_MAX_TURN_STEP = 24.0F;
    private static final float MIN_SMOOTH_LOOK_PITCH_SPEED = 0.6F;
    private static final float SMOOTH_LOOK_PITCH_SPEED_RATIO = 0.62F;
    private static final float SMOOTH_LOOK_OVERSHOOT_MAX_YAW = 2.15F;
    private static final float SMOOTH_LOOK_OVERSHOOT_MAX_PITCH = 0.95F;

    /**
     * The current look target, may be {@code null}.
     */
    private Target target;

    /**
     * The rotation known to the server. Returned by {@link #getEffectiveRotation()} for use in {@link IPlayerContext}.
     */
    private Rotation serverRotation;

    /**
     * Dedicated movement frame used for input remapping while free look is active.
     */
    private Rotation movementRotation;

    /**
     * The last player rotation. Used to restore the player's angle when using free look.
     *
     * @see Settings#freeLook
     */
    private Rotation prevRotation;
    private Rotation visualTargetThisTick;

    private final AimProcessor processor;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(rotation, Target.Mode.resolve(ctx, blockInteract), blockInteract);
        this.movementRotation = rotation;
    }

    @Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @Override
    public boolean shouldDecoupleMovementFromVisualYaw() {
        if (!shouldUseMovementRotationForGroundMovement()) {
            return false;
        }
        if (this.target != null) {
            return shouldDecoupleMovementFromVisualYaw(this.target.mode, this.target.blockInteract);
        }
        return this.movementRotation != null;
    }

    @Override
    public void onTick(TickEvent event) {
        clearMovementRotationIfDetachedFromPathing();
        if (event.getType() == TickEvent.Type.IN) {
            this.processor.tick();
        }
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        clearMovementRotationIfDetachedFromPathing();

        if (this.target == null) {
            return;
        }

        switch (event.getState()) {
            case PRE: {
                boolean smoothLookVisualControl = isSmoothLookEnabled();
                if (this.target.mode == Target.Mode.NONE && !smoothLookVisualControl) {
                    // Just return for PRE, we still want to set target to null on POST
                    return;
                }

                this.prevRotation = new Rotation(ctx.player().getYRot(), ctx.player().getXRot());
                if (this.target.mode == Target.Mode.NONE) {
                    this.visualTargetThisTick = this.target.rotation.normalizeAndClamp();
                } else {
                    final Rotation actual = this.processor.peekRotation(this.target.rotation);
                    this.visualTargetThisTick = actual;
                    ctx.player().setYRot(actual.getYaw());
                    ctx.player().setXRot(actual.getPitch());
                }
                break;
            }
            case POST: {
                // Reset the player's rotations back to their original values
                if (this.prevRotation != null) {
                    if (isSmoothLookEnabled()) {
                        Rotation visualTarget = this.visualTargetThisTick != null
                                ? this.visualTargetThisTick
                                : this.target.rotation;
                        Rotation smoothedRotation = smoothLookLikeKillAura(this.prevRotation, visualTarget);
                        ctx.player().setYRot(smoothedRotation.getYaw());
                        ctx.player().setXRot(smoothedRotation.getPitch());
                    } else if (this.target.mode == Target.Mode.SERVER) {
                        ctx.player().setYRot(this.prevRotation.getYaw());
                        ctx.player().setXRot(this.prevRotation.getPitch());
                    }
                    //ctx.player().xRotO = prevRotation.getPitch();
                    //ctx.player().yRotO = prevRotation.getYaw();
                    this.prevRotation = null;
                    this.visualTargetThisTick = null;
                }
                // The target is done being used for this game tick, so it can be invalidated
                this.target = null;
                clearMovementRotationIfDetachedFromPathing();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) {
            return;
        }

        final ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket.Rot || packet instanceof ServerboundMovePlayerPacket.PosRot) {
            this.serverRotation = new Rotation(packet.getYRot(0.0f), packet.getXRot(0.0f));
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        this.serverRotation = null;
        this.movementRotation = null;
        this.target = null;
        this.visualTargetThisTick = null;
    }

    public void pig() {
        if (this.target != null) {
            final Rotation actual = this.processor.peekRotation(this.target.rotation);
            ctx.player().setYRot(actual.getYaw());
        }
    }

    public Optional<Rotation> getEffectiveRotation() {
        if (this.movementRotation != null) {
            return Optional.of(this.movementRotation);
        }
        if (Baritone.settings().freeLook.value) {
            return Optional.ofNullable(this.serverRotation);
        }
        // If freeLook isn't on, just defer to the player's actual rotations
        return Optional.empty();
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        clearMovementRotationIfDetachedFromPathing();
        final Rotation movement = this.target != null ? this.target.rotation : this.movementRotation;
        if (movement != null) {
            event.setYaw(movement.getYaw());
            event.setPitch(movement.getPitch());
            this.movementRotation = movement;
            this.serverRotation = movement;
        }
    }

    private boolean shouldUseMovementRotationForGroundMovement() {
        if (!hasActivePathingMovementControl()) {
            return false;
        }
        return !ctx.player().isFallFlying() && Baritone.settings().freeLook.value;
    }

    private boolean hasActivePathingMovementControl() {
        return ctx.player() != null && baritone.getPathingBehavior().isPathing();
    }

    private void clearMovementRotationIfDetachedFromPathing() {
        if (this.target == null && !hasActivePathingMovementControl()) {
            this.movementRotation = null;
        }
    }

    private static boolean shouldDecoupleMovementFromVisualYaw(Target.Mode mode, boolean blockInteract) {
        if (blockInteract) {
            return false;
        }
        return mode != Target.Mode.CLIENT;
    }

    private boolean isSmoothLookEnabled() {
        return ctx.player() != null
                && (ctx.player().isFallFlying() ? Baritone.settings().elytraSmoothLook.value
                        : Baritone.settings().smoothLook.value);
    }

    private Rotation smoothLookLikeKillAura(Rotation currentRotation, Rotation targetRotation) {
        if (currentRotation == null || targetRotation == null) {
            return targetRotation == null ? new Rotation(0.0F, 0.0F) : targetRotation.normalizeAndClamp();
        }

        Rotation desiredRotation = applySmoothLookOvershootCorrection(currentRotation, targetRotation.normalizeAndClamp());
        float yawDelta = Rotation.normalizeYaw(desiredRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = desiredRotation.getPitch() - currentRotation.getPitch();
        float yawSpeed = computeSmoothLookTurnSpeed(Math.abs(yawDelta));
        float pitchSpeed = Math.max(MIN_SMOOTH_LOOK_PITCH_SPEED, yawSpeed * SMOOTH_LOOK_PITCH_SPEED_RATIO);
        float turnLimit = sampleSmoothLookMaxTurnStep();

        float maxYawStep = Math.min(yawSpeed, turnLimit);
        float maxPitchStep = Math.min(pitchSpeed, turnLimit);
        float gcdStep = getMouseGcdStep();
        float yawStep = quantizeRotationStepForGcd(clampSigned(yawDelta, maxYawStep), maxYawStep, gcdStep);
        float pitchStep = quantizeRotationStepForGcd(clampSigned(pitchDelta, maxPitchStep), maxPitchStep, gcdStep);

        return new Rotation(
                Rotation.normalizeYaw(currentRotation.getYaw() + yawStep),
                Rotation.clampPitch(currentRotation.getPitch() + pitchStep));
    }

    private Rotation applySmoothLookOvershootCorrection(Rotation currentRotation, Rotation targetRotation) {
        float yawDelta = Rotation.normalizeYaw(targetRotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();
        float absYaw = Math.abs(yawDelta);
        float absPitch = Math.abs(pitchDelta);

        float yawFade = Mth.clamp((absYaw - 1.35F) / 24.0F, 0.0F, 1.0F);
        float pitchFade = Mth.clamp((absPitch - 1.0F) / 22.0F, 0.0F, 1.0F);
        float yawOvershoot = Mth.clamp(absYaw * 0.045F, 0.0F, SMOOTH_LOOK_OVERSHOOT_MAX_YAW) * yawFade;
        float pitchOvershoot = Mth.clamp(absPitch * 0.030F, 0.0F, SMOOTH_LOOK_OVERSHOOT_MAX_PITCH)
                * pitchFade;

        float correctedYaw = targetRotation.getYaw() + Math.copySign(yawOvershoot, yawDelta);
        float correctedPitch = targetRotation.getPitch() + Math.copySign(pitchOvershoot, pitchDelta);
        return new Rotation(
                Rotation.normalizeYaw(correctedYaw),
                Rotation.clampPitch(correctedPitch));
    }

    private float computeSmoothLookTurnSpeed(float yawDeltaAbs) {
        float configuredMin = Mth.clamp(Baritone.settings().smoothLookMinTurnSpeed.value, 0.05F, 180.0F);
        float configuredMax = Mth.clamp(Baritone.settings().smoothLookMaxTurnSpeed.value, configuredMin, 180.0F);
        float normalized = Mth.clamp(yawDeltaAbs / 120.0F, 0.0F, 1.0F);
        float eased = normalized * normalized;
        float effectiveMin = Math.max(0.65F, configuredMin * 0.58F);
        float effectiveMax = Math.max(effectiveMin, configuredMax * 0.72F);
        return effectiveMin + (effectiveMax - effectiveMin) * eased;
    }

    private float sampleSmoothLookMaxTurnStep() {
        SmoothLookTurnStepRange range = parseSmoothLookMaxTurnStepRange(Baritone.settings().smoothLookMaxTurnStep.value);
        if (!range.isRandom()) {
            return range.min;
        }
        return (float) ThreadLocalRandom.current().nextDouble(range.min, range.max);
    }

    private SmoothLookTurnStepRange parseSmoothLookMaxTurnStepRange(String spec) {
        String normalized = spec == null ? "" : spec.trim();
        if (normalized.isEmpty()) {
            return new SmoothLookTurnStepRange(
                    DEFAULT_SMOOTH_LOOK_MAX_TURN_STEP,
                    DEFAULT_SMOOTH_LOOK_MAX_TURN_STEP);
        }

        normalized = normalized.replace("°", "")
                .replace("，", ",")
                .replace("－", "-")
                .replace("–", "-")
                .replace("—", "-")
                .replace("~", "-")
                .replace("～", "-")
                .replace("至", "-")
                .replace("到", "-")
                .replaceAll("\\s+", "");

        String[] parts = normalized.split("-", -1);
        try {
            if (parts.length == 1) {
                float value = clampSmoothLookTurnStep(Float.parseFloat(parts[0]));
                return new SmoothLookTurnStepRange(value, value);
            }
            if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                float min = clampSmoothLookTurnStep(Float.parseFloat(parts[0]));
                float max = clampSmoothLookTurnStep(Float.parseFloat(parts[1]));
                if (max < min) {
                    float swap = min;
                    min = max;
                    max = swap;
                }
                return new SmoothLookTurnStepRange(min, max);
            }
        } catch (NumberFormatException ignored) {
        }

        return new SmoothLookTurnStepRange(
                DEFAULT_SMOOTH_LOOK_MAX_TURN_STEP,
                DEFAULT_SMOOTH_LOOK_MAX_TURN_STEP);
    }

    private float clampSmoothLookTurnStep(float value) {
        return Mth.clamp(value, MIN_SMOOTH_LOOK_MAX_TURN_STEP, MAX_SMOOTH_LOOK_MAX_TURN_STEP);
    }

    private float getMouseGcdStep() {
        float sensitivity = Mth.clamp(ctx.minecraft().options.sensitivity().get().floatValue(), 0.0F, 1.0F);
        float factor = sensitivity * 0.6F + 0.2F;
        return factor * factor * factor * 8.0F * 0.15F;
    }

    private float quantizeRotationStepForGcd(float step, float maxMagnitude, float gcdStep) {
        if (step == 0.0F || gcdStep <= 1.0E-5F || maxMagnitude <= 0.0F) {
            return step == 0.0F ? 0.0F : clampSigned(step, maxMagnitude);
        }
        float maxStep = Math.abs(maxMagnitude);
        float absStep = Math.min(Math.abs(step), maxStep);
        if (absStep <= 1.0E-5F) {
            return 0.0F;
        }
        if (maxStep + 1.0E-5F < gcdStep) {
            return Math.copySign(absStep, step);
        }

        float quantized = Math.round(absStep / gcdStep) * gcdStep;
        if (quantized <= 1.0E-5F && absStep >= gcdStep * 0.45F) {
            quantized = gcdStep;
        }
        if (quantized > maxStep) {
            quantized = (float) Math.floor(maxStep / gcdStep) * gcdStep;
        }
        if (quantized <= 1.0E-5F) {
            return 0.0F;
        }
        return Math.copySign(Math.min(quantized, maxStep), step);
    }

    private float clampSigned(float value, float maxMagnitude) {
        if (maxMagnitude <= 0.0F) {
            return 0.0F;
        }
        return Math.copySign(Math.min(Math.abs(value), maxMagnitude), value);
    }

    private static final class SmoothLookTurnStepRange {
        private final float min;
        private final float max;

        private SmoothLookTurnStepRange(float min, float max) {
            this.min = min;
            this.max = max;
        }

        private boolean isRandom() {
            return this.max > this.min + 1.0E-5F;
        }
    }

    private static final class AimProcessor extends AbstractAimProcessor {

        public AimProcessor(final IPlayerContext ctx) {
            super(ctx);
        }

        @Override
        protected Rotation getPrevRotation() {
            // Implementation will use LookBehavior.serverRotation
            return ctx.playerRotations();
        }
    }

    private static abstract class AbstractAimProcessor implements ITickableAimProcessor {

        protected final IPlayerContext ctx;
        private final ForkableRandom rand;
        private double randomYawOffset;
        private double randomPitchOffset;
        private float lastYawDelta;
        private int humanTicks;
        private int hesitationTicks;
        private int nextHesitationInterval = 8;
        private boolean humanLikeWasEnabled;
        private boolean hasSpareGaussian;
        private double spareGaussian;

        public AbstractAimProcessor(IPlayerContext ctx) {
            this.ctx = ctx;
            this.rand = new ForkableRandom();
        }

        private AbstractAimProcessor(final AbstractAimProcessor source) {
            this.ctx = source.ctx;
            this.rand = source.rand.fork();
            this.randomYawOffset = source.randomYawOffset;
            this.randomPitchOffset = source.randomPitchOffset;
            this.lastYawDelta = source.lastYawDelta;
            this.humanTicks = source.humanTicks;
            this.hesitationTicks = source.hesitationTicks;
            this.nextHesitationInterval = source.nextHesitationInterval;
            this.humanLikeWasEnabled = source.humanLikeWasEnabled;
            this.hasSpareGaussian = source.hasSpareGaussian;
            this.spareGaussian = source.spareGaussian;
        }

        @Override
        public final Rotation peekRotation(final Rotation rotation) {
            if (!HumanLikeMovementController.INSTANCE.isEnabled()) {
                return peekRotationOriginal(rotation);
            }

            return peekRotationHuman(rotation);
        }

        private Rotation peekRotationOriginal(final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            float desiredYaw = rotation.getYaw();
            float desiredPitch = rotation.getPitch();

            // In other words, the target doesn't care about the pitch, so it used playerRotations().getPitch()
            // and it's safe to adjust it to a normal level
            if (desiredPitch == prev.getPitch()) {
                desiredPitch = nudgeToLevel(desiredPitch);
            }

            desiredYaw += this.randomYawOffset;
            desiredPitch += this.randomPitchOffset;

            return new Rotation(
                    this.calculateMouseMove(prev.getYaw(), desiredYaw),
                    this.calculateMouseMove(prev.getPitch(), desiredPitch)
            ).clamp();
        }

        private Rotation peekRotationHuman(final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            float desiredYaw = rotation.getYaw();
            float desiredPitch = rotation.getPitch();

            if (desiredPitch == prev.getPitch()) {
                desiredPitch = nudgeToLevel(desiredPitch);
            }

            desiredYaw += this.randomYawOffset;
            desiredPitch += this.randomPitchOffset;

            float yawDelta = Rotation.normalizeYaw(desiredYaw - prev.getYaw());
            float pitchDelta = Math.abs(desiredPitch - prev.getPitch());
            boolean pitchIsAtLimit = Math.abs(Rotation.clampPitch(desiredPitch)) == 90.0F;

            if (pitchDelta > 0.0F && pitchDelta < 1.0F && !pitchIsAtLimit) {
                if (Math.abs(yawDelta) < 0.05F) {
                    desiredYaw = prev.getYaw() + yawDelta + randomSignedRange(0.01F, 0.3F);
                }
                if (randomChance(0.22D)) {
                    desiredPitch = prev.getPitch();
                } else if (randomChance(0.34D)) {
                    desiredPitch = prev.getPitch()
                            + Math.copySign(1.0F + nextFloat(0.35F), desiredPitch - prev.getPitch());
                }
            } else if (Math.abs(yawDelta) < 0.015F && Math.abs(this.lastYawDelta) < 0.015F && randomChance(0.45D)) {
                desiredYaw = prev.getYaw() + randomSignedRange(0.01F, 0.18F);
            }

            yawDelta = Rotation.normalizeYaw(desiredYaw - prev.getYaw());
            pitchDelta = Math.abs(desiredPitch - prev.getPitch());

            if (this.hesitationTicks > 0 && Math.abs(yawDelta) + pitchDelta < 35.0F) {
                this.hesitationTicks--;
                desiredYaw = prev.getYaw() + yawDelta * nextFloat(0.04F, 0.12F) + (float) (nextGaussian() * 0.05D);
                desiredPitch = prev.getPitch()
                        + (desiredPitch - prev.getPitch()) * nextFloat(0.04F, 0.12F)
                        + (float) (nextGaussian() * 0.035D);
            } else if (this.humanTicks % this.nextHesitationInterval == 0
                    && Math.abs(yawDelta) + pitchDelta < 40.0F
                    && randomChance(0.35D)) {
                this.hesitationTicks = 1 + nextInt(2);
                this.nextHesitationInterval = 8 + nextInt(13);
            }

            float yawFactor = chooseSmoothFactor(Math.abs(yawDelta), pitchDelta, true);
            float pitchFactor = chooseSmoothFactor(Math.abs(yawDelta), pitchDelta, false);
            float smoothedYaw = smoothEaseAngle(prev.getYaw(), desiredYaw, yawFactor);
            float smoothedPitch = smoothEaseLinear(prev.getPitch(), desiredPitch, pitchFactor);

            HumanLikeMovementController.RotationState controllerRotation = HumanLikeMovementController.INSTANCE
                    .smoothRotation(prev.getYaw(), prev.getPitch(), smoothedYaw, smoothedPitch);

            float actualYaw = this.calculateHumanMouseMove(prev.getYaw(), controllerRotation.yaw, true);
            float actualPitch = this.calculateHumanMouseMove(prev.getPitch(), controllerRotation.pitch, false);

            float actualYawDelta = Rotation.normalizeYaw(actualYaw - prev.getYaw());
            float actualPitchDelta = Math.abs(actualPitch - prev.getPitch());
            if (Math.abs(actualYawDelta) < 0.01F && actualPitchDelta > 0.0F && actualPitchDelta < 1.0F
                    && Math.abs(actualPitch) != 90.0F) {
                actualYaw = prev.getYaw() + randomSignedRange(0.01F, 0.3F);
                actualYawDelta = Rotation.normalizeYaw(actualYaw - prev.getYaw());
            }

            this.lastYawDelta = actualYawDelta;
            return new Rotation(actualYaw, actualPitch).clamp();
        }

        @Override
        public final void tick() {
            if (!HumanLikeMovementController.INSTANCE.isEnabled()) {
                this.humanLikeWasEnabled = false;
                tickOriginal();
                return;
            }

            if (!this.humanLikeWasEnabled) {
                this.hesitationTicks = 0;
                this.nextHesitationInterval = 8 + nextInt(10);
                this.lastYawDelta = 0.0F;
                this.humanLikeWasEnabled = true;
            }
            this.humanTicks++;
            tickHuman();
        }

        private void tickOriginal() {
            // randomLooking
            this.randomYawOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;
            this.randomPitchOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;

            // randomLooking113
            double random = this.rand.nextDouble() - 0.5;
            if (Math.abs(random) < 0.1) {
                random *= 4;
            }
            this.randomYawOffset += random * Baritone.settings().randomLooking113.value;
        }

        private void tickHuman() {
            HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
            config.normalize();

            double speed = 0.0D;
            try {
                speed = this.ctx.player() == null ? 0.0D : this.ctx.player().getDeltaMovement().length();
            } catch (Throwable ignored) {
                speed = 0.0D;
            }

            double fatigue = 0.82D + Math.sin(this.humanTicks * 0.037D) * 0.18D;
            double speedBoost = 1.0D + Math.min(1.25D, speed * 2.75D);
            double baseRandomLooking = Baritone.settings().randomLooking.value;
            double jitterBase = Math.max(baseRandomLooking * 1.5D, config.viewJitter * 0.035D);

            this.randomYawOffset = ((this.rand.nextDouble() - 0.5D) * baseRandomLooking * 1.2D
                    + nextGaussian() * jitterBase * 0.55D) * speedBoost * fatigue;
            this.randomPitchOffset = ((this.rand.nextDouble() - 0.5D) * baseRandomLooking * 0.8D
                    + nextGaussian() * jitterBase * 0.34D) * (0.85D + Math.min(0.7D, speed * 1.8D));

            double random113 = this.rand.nextDouble() - 0.5D;
            if (Math.abs(random113) < 0.15D) {
                random113 *= 3.5D;
            }
            double random113Scale = 0.75D + this.rand.nextDouble() * 0.65D;
            this.randomYawOffset += random113 * Baritone.settings().randomLooking113.value * random113Scale;
            this.randomPitchOffset += nextGaussian() * Baritone.settings().randomLooking113.value * 0.025D;

            if (Math.abs(this.lastYawDelta) < 0.02F && randomChance(0.25D)) {
                this.randomYawOffset += randomSignedRange(0.01F, 0.22F);
            }
        }

        @Override
        public final void advance(int ticks) {
            for (int i = 0; i < ticks; i++) {
                this.tick();
            }
        }

        @Override
        public Rotation nextRotation(final Rotation rotation) {
            final Rotation actual = this.peekRotation(rotation);
            this.tick();
            return actual;
        }

        @Override
        public final ITickableAimProcessor fork() {
            return new AbstractAimProcessor(this) {

                private Rotation prev = AbstractAimProcessor.this.getPrevRotation();

                @Override
                public Rotation nextRotation(final Rotation rotation) {
                    return (this.prev = super.nextRotation(rotation));
                }

                @Override
                protected Rotation getPrevRotation() {
                    return this.prev;
                }
            };
        }

        protected abstract Rotation getPrevRotation();

        /**
         * Nudges the player's pitch to a regular level. (Between {@code -20} and {@code 10}, increments are by {@code 1})
         */
        private float nudgeToLevel(float pitch) {
            if (pitch < -20) {
                return pitch + 1;
            } else if (pitch > 10) {
                return pitch - 1;
            }
            return pitch;
        }

        private float calculateMouseMove(float current, float target) {
            final float delta = target - current;
            final double deltaPx = angleToMouse(delta); // yes, even the mouse movements use double
            return current + mouseToAngle(deltaPx);
        }

        private float calculateHumanMouseMove(float current, float target, boolean yaw) {
            final float delta = yaw ? Rotation.normalizeYaw(target - current) : target - current;
            final double deltaPx = angleToHumanMouse(delta);
            return current + mouseToAngle(deltaPx);
        }

        private double angleToMouse(float angleDelta) {
            final float minAngleChange = mouseToAngle(1);
            return Math.round(angleDelta / minAngleChange);
        }

        private double angleToHumanMouse(float angleDelta) {
            final float minAngleChange = mouseToAngle(1);
            if (Math.abs(minAngleChange) < 1.0E-6F) {
                return 0.0D;
            }

            double px = angleDelta / minAngleChange;
            if (Math.abs(px) > 0.25D) {
                px += nextGaussian() * 0.10D;
            }
            double rounded = Math.round(px);

            if (rounded == 0.0D && Math.abs(angleDelta) > 0.01F && randomChance(0.65D)) {
                rounded = Math.copySign(1.0D, px == 0.0D ? angleDelta : px);
            }
            if (Math.abs(rounded) > 0.0D && randomChance(0.08D)) {
                rounded += nextGaussian() * 0.18D;
            }
            return rounded;
        }

        private float mouseToAngle(double mouseDelta) {
            // casting float literals to double gets us the precise values used by mc
            final double f = ctx.minecraft().options.sensitivity().get() * (double) 0.6f + (double) 0.2f;
            return (float) (mouseDelta * f * f * f * 8.0d) * 0.15f; // yes, one double and one float scaling factor
        }

        private float chooseSmoothFactor(float yawDeltaAbs, float pitchDeltaAbs, boolean yaw) {
            HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
            float turnMagnitude = yawDeltaAbs + pitchDeltaAbs * 0.65F;
            float speedWeight = clamp(config.maxTurnSpeed / 60.0F, 0.08F, 0.8F);
            float base = yaw ? 0.24F : 0.20F;
            float factor = base + Math.min(0.36F, turnMagnitude / 95.0F) + speedWeight * 0.16F;
            factor += nextFloat(-0.06F, 0.08F);
            if (turnMagnitude < 1.25F) {
                factor *= 0.72F + nextFloat(0.18F);
            }
            return clamp(factor, 0.12F, 0.72F);
        }

        private float smoothEaseAngle(float current, float target, float factor) {
            float delta = Rotation.normalizeYaw(target - current);
            float eased = smoothStep(clamp(factor, 0.0F, 1.0F));
            return current + delta * eased;
        }

        private float smoothEaseLinear(float current, float target, float factor) {
            float eased = smoothStep(clamp(factor, 0.0F, 1.0F));
            return current + (target - current) * eased;
        }

        private float smoothStep(float t) {
            return t * t * (3.0F - 2.0F * t);
        }

        private boolean randomChance(double chance) {
            return this.rand.nextDouble() < chance;
        }

        private int nextInt(int bound) {
            if (bound <= 1) {
                return 0;
            }
            return (int) Math.floor(this.rand.nextDouble() * bound);
        }

        private float nextFloat(float maxExclusive) {
            return (float) (this.rand.nextDouble() * maxExclusive);
        }

        private float nextFloat(float minInclusive, float maxExclusive) {
            return minInclusive + nextFloat(maxExclusive - minInclusive);
        }

        private float randomSignedRange(float minAbs, float maxAbs) {
            float value = minAbs + nextFloat(Math.max(0.0F, maxAbs - minAbs));
            return this.rand.nextDouble() < 0.5D ? -value : value;
        }

        private double nextGaussian() {
            if (this.hasSpareGaussian) {
                this.hasSpareGaussian = false;
                return this.spareGaussian;
            }

            double u1 = Math.max(1.0E-12D, this.rand.nextDouble());
            double u2 = this.rand.nextDouble();
            double magnitude = Math.sqrt(-2.0D * Math.log(u1));
            double angle = 2.0D * Math.PI * u2;
            this.spareGaussian = magnitude * Math.sin(angle);
            this.hasSpareGaussian = true;
            return magnitude * Math.cos(angle);
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static class Target {

        public final Rotation rotation;
        public final Mode mode;
        public final boolean blockInteract;

        public Target(Rotation rotation, Mode mode, boolean blockInteract) {
            this.rotation = rotation;
            this.mode = mode;
            this.blockInteract = blockInteract;
        }

        enum Mode {
            /**
             * Rotation will be set client-side and is visual to the player
             */
            CLIENT,

            /**
             * Rotation will be set server-side and is silent to the player
             */
            SERVER,

            /**
             * Rotation will remain unaffected on both the client and server
             */
            NONE;

            static Mode resolve(IPlayerContext ctx, boolean blockInteract) {
                final Settings settings = Baritone.settings();
                final boolean antiCheat = settings.antiCheatCompatibility.value;
                final boolean blockFreeLook = settings.blockFreeLook.value;

                if (ctx.player().isFallFlying()) {
                    // always need to set angles while flying
                    return settings.elytraFreeLook.value ? SERVER : CLIENT;
                } else if (settings.freeLook.value) {
                    // Regardless of if antiCheatCompatibility is enabled, if a blockInteract is requested then the player
                    // rotation needs to be set somehow, otherwise Baritone will halt since objectMouseOver() will just be
                    // whatever the player is mousing over visually. Let's just settle for setting it silently.
                    if (blockInteract) {
                        return blockFreeLook ? SERVER : CLIENT;
                    }
                    return antiCheat ? SERVER : NONE;
                }

                // all freeLook settings are disabled so set the angles
                return CLIENT;
            }
        }
    }
}

