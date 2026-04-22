package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class AutoObstacleAvoidFeatureHandler {

    private AutoObstacleAvoidFeatureHandler() {
    }

    static void apply(MovementFeatureManager manager, LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("auto_obstacle_avoid")
                || player == null
                || player.level() == null
                || player.getAbilities().flying
                || player.isInWater()
                || player.isInLava()
                || player.isPassenger()
                || !MovementFeatureSupport.isMoving(player)) {
            manager.obstacleAvoidDirection = 0;
            manager.obstacleAvoidTicks = 0;
            return;
        }

        Vec3 heading = MovementFeatureSupport.getMovementHeading(player);
        if (heading.lengthSqr() < 1.0E-4D) {
            return;
        }

        double detectDistance = Math.max(0.50D, MovementFeatureManager.getConfiguredValue("auto_obstacle_avoid", 1.50F));
        boolean blockedAhead = player.horizontalCollision || MovementFeatureSupport.hasObstacleAhead(player, heading, detectDistance);
        if (blockedAhead && MovementFeatureSupport.canStepUp(player, heading)) {
            blockedAhead = false;
        }
        if (!blockedAhead) {
            return;
        }

        int preferredDirection = manager.obstacleAvoidTicks > 0 ? manager.obstacleAvoidDirection : 0;
        int chosenDirection = chooseDirection(player, heading, detectDistance, preferredDirection);
        if (chosenDirection == 0) {
            return;
        }

        Vec3 side = chosenDirection > 0 ? MovementFeatureSupport.rotateRight(heading) : MovementFeatureSupport.rotateLeft(heading);
        Vec3 desired = MovementFeatureSupport.blendHorizontal(heading, 0.60D, side, 0.95D);
        double speed = Math.max(MovementFeatureSupport.getBaseMoveSpeed() * 0.92D,
                MovementFeatureSupport.getHorizontalSpeed(player) * 0.96D);
        if (desired.lengthSqr() > 1.0E-4D) {
            player.setDeltaMovement(desired.x * speed, player.getDeltaMovement().y, desired.z * speed);
            player.hurtMarked = true;
            manager.obstacleAvoidDirection = chosenDirection;
            manager.obstacleAvoidTicks = 6;
        }
    }

    private static int chooseDirection(LocalPlayer player, Vec3 heading, double detectDistance, int preferredDirection) {
        int[] directions = preferredDirection > 0 ? new int[] { preferredDirection, -preferredDirection }
                : new int[] { 1, -1 };
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestDirection = 0;

        for (int direction : directions) {
            Vec3 side = direction > 0 ? MovementFeatureSupport.rotateRight(heading) : MovementFeatureSupport.rotateLeft(heading);
            Vec3 candidate = MovementFeatureSupport.blendHorizontal(heading, 0.55D, side, 0.90D);
            double score = scoreDirection(player, candidate, detectDistance);
            if (direction == preferredDirection) {
                score += 0.35D;
            }
            if (score > bestScore) {
                bestScore = score;
                bestDirection = direction;
            }
        }

        return bestScore > 0.25D ? bestDirection : 0;
    }

    private static double scoreDirection(LocalPlayer player, Vec3 direction, double detectDistance) {
        double score = 0.0D;
        for (double step = 0.35D; step <= detectDistance; step += 0.35D) {
            AABB moved = player.getBoundingBox().move(direction.x * step, 0.0D, direction.z * step)
                    .deflate(0.04D, 0.0D, 0.04D);
            boolean collides = !player.level().noCollision(player, moved);
            boolean supported = player.level().getBlockCollisions(player, moved.move(0.0D, -0.55D, 0.0D)).iterator().hasNext();
            if (collides) {
                score -= 1.4D;
                break;
            }
            score += supported ? 0.85D : -0.55D;
        }
        return score;
    }
}


