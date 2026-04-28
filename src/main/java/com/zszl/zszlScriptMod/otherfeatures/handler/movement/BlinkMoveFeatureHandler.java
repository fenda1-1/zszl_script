package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

final class BlinkMoveFeatureHandler {

    private BlinkMoveFeatureHandler() {
    }

    static void apply(MovementFeatureManager manager, LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        boolean sneakDown = player != null && player.input != null && player.input.keyPresses.shift();
        boolean sprintTrigger = mc != null && mc.options != null && mc.options.keySprint.isDown();
        boolean triggerDown = sneakDown && sprintTrigger;

        if (!MovementFeatureManager.isEnabled("blink_move")
                || player == null
                || player.level() == null
                || player.getAbilities().flying
                || player.isInWater()
                || player.isInLava()
                || player.isPassenger()
                || mc == null
                || mc.screen != null) {
            manager.wasBlinkTriggerDown = triggerDown;
            return;
        }

        if (!triggerDown || manager.wasBlinkTriggerDown || manager.blinkCooldownTicks > 0 || !player.onGround()
                || !MovementFeatureSupport.isMoving(player)) {
            manager.wasBlinkTriggerDown = triggerDown;
            return;
        }

        double maxDistance = MovementFeatureManager.getConfiguredValue("blink_move", 3.00F);
        Vec3 destination = MovementFeatureSupport.findBestBlinkDestination(player, maxDistance);
        if (destination == null) {
            manager.wasBlinkTriggerDown = triggerDown;
            manager.blinkCooldownTicks = 6;
            return;
        }

        Vec3 direction = MovementFeatureSupport.getMovementHeading(player);
        double carrySpeed = Math.max(MovementFeatureSupport.getBaseMoveSpeed() * 0.70D,
                MovementFeatureSupport.getHorizontalSpeed(player) * 0.60D);

        player.setPos(destination.x, destination.y, destination.z);
        if (player.connection != null) {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(destination, player.onGround(), false));
        }

        if (direction.lengthSqr() > 1.0E-4D) {
            player.setDeltaMovement(direction.x * carrySpeed, player.getDeltaMovement().y, direction.z * carrySpeed);
        } else {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.3D, motion.y, motion.z * 0.3D);
        }
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        manager.blinkCooldownTicks = 16;
        manager.longJumpChargeTicks = 0;
        manager.wasLongJumpSneakDown = false;
        manager.wasBlinkTriggerDown = triggerDown;
    }
}




