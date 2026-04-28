package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

final class BlockPhaseFeatureHandler {

    private BlockPhaseFeatureHandler() {
    }

    static void apply(MovementFeatureManager manager, LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("block_phase")
                || player == null
                || player.level() == null
                || player.getAbilities().flying
                || player.isInWater()
                || player.isInLava()
                || player.isPassenger()) {
            manager.blockPhaseStuckTicks = 0;
            return;
        }

        boolean moving = MovementFeatureSupport.isMoving(player);
        boolean insideOpaque = player.isInWall();
        boolean horizontallyBlocked = moving && player.horizontalCollision;
        if (insideOpaque || horizontallyBlocked) {
            manager.blockPhaseStuckTicks++;
        } else {
            manager.blockPhaseStuckTicks = 0;
            return;
        }

        if (!insideOpaque && manager.blockPhaseStuckTicks < 4) {
            return;
        }

        double configuredStrength = MovementFeatureManager.getConfiguredValue("block_phase", 0.12F);
        double searchRadius = 0.55D + configuredStrength * 2.4D;
        Vec3 preferred = horizontallyBlocked ? MovementFeatureSupport.getMovementHeading(player) : null;
        Vec3 safePos = MovementFeatureSupport.findNearestSafePosition(player, searchRadius, preferred);
        if (safePos == null) {
            return;
        }

        double originX = player.getX();
        double originY = player.getY();
        double originZ = player.getZ();
        Vec3 escapeDir = new Vec3(safePos.x - originX, 0.0D, safePos.z - originZ);
        double blend = insideOpaque ? 1.0D : Math.min(0.85D, 0.32D + configuredStrength * 1.4D);
        double targetX = player.getX() + (safePos.x - player.getX()) * blend;
        double targetY = player.getY() + (safePos.y - player.getY()) * blend;
        double targetZ = player.getZ() + (safePos.z - player.getZ()) * blend;
        if (!MovementFeatureSupport.canOccupy(player, targetX, targetY, targetZ)) {
            targetX = safePos.x;
            targetY = safePos.y;
            targetZ = safePos.z;
        }

        player.setPos(targetX, targetY, targetZ);
        if (player.connection != null) {
            player.connection.send(new ServerboundMovePlayerPacket.Pos(new Vec3(targetX, targetY, targetZ), player.onGround(), false));
        }

        if (escapeDir.lengthSqr() > 1.0E-4D) {
            Vec3 horizontal = MovementFeatureSupport.blendHorizontal(MovementFeatureSupport.getMovementHeading(player), 0.35D,
                    escapeDir, 0.85D);
            double speed = Math.max(0.12D, MovementFeatureSupport.getHorizontalSpeed(player));
            player.setDeltaMovement(horizontal.x * speed, player.getDeltaMovement().y, horizontal.z * speed);
        } else {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.5D, motion.y, motion.z * 0.5D);
        }
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
        manager.blockPhaseStuckTicks = 0;
    }
}



