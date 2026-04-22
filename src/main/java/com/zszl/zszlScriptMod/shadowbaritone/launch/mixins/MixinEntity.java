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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {

    @Unique
    private RotationMoveEvent motionUpdateRotationEvent;

    @Inject(
            method = "moveRelative",
            at = @At("HEAD")
    )
    private void moveRelativeHead(CallbackInfo info) {
        if (!LocalPlayer.class.isInstance(this)) {
            this.motionUpdateRotationEvent = null;
            return;
        }
        LocalPlayer player = (LocalPlayer) (Object) this;
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(player);
        if (!shouldOverrideMovementRotation(baritone)) {
            this.motionUpdateRotationEvent = null;
            return;
        }
        Entity self = (Entity) (Object) this;
        this.motionUpdateRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, self.getYRot(), self.getXRot());
        baritone.getGameEventHandler().onPlayerRotationMove(motionUpdateRotationEvent);
        self.setYRot(this.motionUpdateRotationEvent.getYaw());
        self.setXRot(this.motionUpdateRotationEvent.getPitch());
    }

    @Inject(
            method = "moveRelative",
            at = @At("RETURN")
    )
    private void moveRelativeReturn(CallbackInfo info) {
        if (this.motionUpdateRotationEvent != null) {
            Entity self = (Entity) (Object) this;
            self.setYRot(this.motionUpdateRotationEvent.getOriginal().getYaw());
            self.setXRot(this.motionUpdateRotationEvent.getOriginal().getPitch());
            this.motionUpdateRotationEvent = null;
        }
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zszl$cancelEntityPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (zszl$isNoCollisionLocalPlayer(self) || zszl$isNoCollisionLocalPlayer(other)) {
            ci.cancel();
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

    @Unique
    private boolean zszl$isNoCollisionLocalPlayer(Entity entity) {
        return entity instanceof LocalPlayer
                && entity == Minecraft.getInstance().player
                && MovementFeatureManager.isEnabled("no_collision");
    }
}

