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

package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.RotationMoveEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.input.Input;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * @author Brady
 * @since 9/10/2018
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {

    /**
     * Event called to override the movement direction when jumping
     */
    @Unique
    private RotationMoveEvent jumpRotationEvent;

    @Unique
    private RotationMoveEvent elytraRotationEvent;

    private MixinLivingEntity(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(
            method = "jumpFromGround",
            at = @At("HEAD")
    )
    private void preMoveRelative(CallbackInfo ci) {
        Optional<IBaritone> baritone = this.getBaritone();
        if (!baritone.isPresent() || !shouldOverrideMovementRotation(baritone.get())) {
            this.jumpRotationEvent = null;
            return;
        }
        this.jumpRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.JUMP, this.getYRot(), this.getXRot());
        baritone.get().getGameEventHandler().onPlayerRotationMove(this.jumpRotationEvent);
    }

    @Redirect(
            method = "jumpFromGround",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.getYRot()F"
            )
    )
    private float overrideYaw(LivingEntity self) {
        if (self instanceof LocalPlayer
                && this.jumpRotationEvent != null
                && BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this) != null) {
            return this.jumpRotationEvent.getYaw();
        }
        return self.getYRot();
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.getLookAngle()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private void onPreElytraMove(Vec3 direction, CallbackInfo ci) {
        this.getBaritone().ifPresent(baritone -> {
            this.elytraRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.getYRot(), this.getXRot());
            baritone.getGameEventHandler().onPlayerRotationMove(this.elytraRotationEvent);
            this.setYRot(this.elytraRotationEvent.getYaw());
            this.setXRot(this.elytraRotationEvent.getPitch());
        });
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/entity/LivingEntity.move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPostElytraMove(Vec3 direction, CallbackInfo ci) {
        if (this.elytraRotationEvent != null) {
            this.setYRot(this.elytraRotationEvent.getOriginal().getYaw());
            this.setXRot(this.elytraRotationEvent.getOriginal().getPitch());
            this.elytraRotationEvent = null;
        }
    }

    @Inject(
            method = "isPushable",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zszl$disablePushableWhileNoCollision(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LocalPlayer
                && (Object) this == Minecraft.getInstance().player
                && MovementFeatureManager.isEnabled("no_collision")) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private Optional<IBaritone> getBaritone() {
        // noinspection ConstantConditions
        if (LocalPlayer.class.isInstance(this)) {
            return Optional.ofNullable(BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this));
        } else {
            return Optional.empty();
        }
    }

    @Unique
    private boolean shouldOverrideMovementRotation(IBaritone baritone) {
        if (baritone == null || baritone.getInputOverrideHandler() == null || baritone.getLookBehavior() == null) {
            return false;
        }
        if (!isBaritoneActivelyControllingMovement(baritone)) {
            return false;
        }
        if (baritone.getLookBehavior().shouldDecoupleMovementFromVisualYaw()) {
            return false;
        }
        return baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_FORWARD)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_BACK)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_LEFT)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.MOVE_RIGHT)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.JUMP)
                || baritone.getInputOverrideHandler().isInputForcedDown(Input.SNEAK);
    }

    @Unique
    private boolean isBaritoneActivelyControllingMovement(IBaritone baritone) {
        if (baritone.getPathingBehavior() != null
                && baritone.getPathingBehavior().isPathing()
                && baritone.getPathingBehavior().getCurrent() != null) {
            return true;
        }
        return baritone.getElytraProcess() != null && baritone.getElytraProcess().isActive();
    }
}

