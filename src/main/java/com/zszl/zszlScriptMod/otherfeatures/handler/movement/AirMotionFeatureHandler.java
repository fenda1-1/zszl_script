package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

final class AirMotionFeatureHandler {

    private AirMotionFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (player == null) {
            return;
        }
        applyHoverOrGravity(player);
        applyFallCushion(player);
        LavaWalkFeatureHandler.apply(player);
    }

    private static void applyHoverOrGravity(LocalPlayer player) {
        if (MovementFeatureManager.isEnabled("hover_mode")
                && !player.onGround()
                && !player.getAbilities().flying
                && player.input != null
                && !player.input.keyPresses.jump()
                && !player.input.keyPresses.shift()
                && !player.isInWater()
                && !player.isInLava()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, MovementFeatureManager.getConfiguredValue("hover_mode", 0.0F), motion.z);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
            return;
        }

        if (MovementFeatureManager.isEnabled("low_gravity")
                && !player.onGround()
                && player.getDeltaMovement().y < 0.0D
                && !player.getAbilities().flying
                && !player.isInWater()
                && !player.isInLava()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x,
                    motion.y * MovementFeatureManager.getConfiguredValue("low_gravity", 0.72F),
                    motion.z);
            player.hurtMarked = true;
        }
    }

    private static void applyFallCushion(LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("fall_cushion")
                || player.getDeltaMovement().y >= -0.12D
                || player.fallDistance <= 2.0F
                || !MovementFeatureSupport.hasGroundBelow(player, 1.75D)) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        double buffer = MovementFeatureManager.getConfiguredValue("fall_cushion", 0.24F);
        player.setDeltaMovement(motion.x, Math.max(motion.y, -buffer), motion.z);
        player.fallDistance *= 0.4F;
        player.hurtMarked = true;
    }
}


