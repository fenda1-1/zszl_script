package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

final class ScaffoldFeatureHandler {

    private ScaffoldFeatureHandler() {
    }

    static void apply(MovementFeatureManager manager, LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (!MovementFeatureManager.isEnabled("scaffold")
                || player == null
                || player.level() == null
                || player.getAbilities().flying
                || player.isInWater()
                || player.isInLava()
                || player.isPassenger()
                || player.isUsingItem()
                || mc == null
                || mc.screen != null
                || manager.scaffoldPlaceCooldownTicks > 0
                || (!player.onGround() && (player.fallDistance > 2.0F || player.getDeltaMovement().y > 0.35D))
                || !MovementFeatureSupport.isMoving(player)) {
            return;
        }

        Vec3 heading = MovementFeatureSupport.getMovementHeading(player);
        if (heading.lengthSqr() < 1.0E-4D) {
            return;
        }

        double probeDistance = Math.min(3.0D, Math.max(0.50D, MovementFeatureManager.getConfiguredValue("scaffold", 1.00F)));
        for (double step = 0.0D; step <= probeDistance + 1.0E-4D; step += 0.45D) {
            AABB futureBox = player.getBoundingBox().move(heading.x * step, 0.0D, heading.z * step);
            AABB footBox = futureBox.deflate(0.10D, 0.0D, 0.10D);
            if (MovementFeatureSupport.hasGroundBelow(player, 0.55D)
                    && player.level().getBlockCollisions(player, footBox.move(0.0D, -0.55D, 0.0D)).iterator().hasNext()) {
                continue;
            }

            BlockPos targetPos = BlockPos.containing((futureBox.minX + futureBox.maxX) * 0.5D,
                    futureBox.minY - 0.80D,
                    (futureBox.minZ + futureBox.maxZ) * 0.5D);
            MovementFeatureSupport.PlacementTarget placement = MovementFeatureSupport.findScaffoldPlacement(player, targetPos);
            if (placement == null) {
                continue;
            }

            if (MovementFeatureSupport.placeFromScaffold(player, placement)) {
                player.fallDistance = 0.0F;
                manager.scaffoldPlaceCooldownTicks = 2;
                return;
            }
        }
    }
}




