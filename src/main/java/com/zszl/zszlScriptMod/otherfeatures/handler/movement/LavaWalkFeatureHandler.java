package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class LavaWalkFeatureHandler {

    private static final double COLLISION_TRIGGER_OFFSET = 0.45D;
    private static final double SURFACE_SNAP_RANGE = 0.35D;
    private static final double FOOTPRINT_SHRINK = 0.12D;

    private LavaWalkFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (!isAvailable(player)) {
            return;
        }

        double surfaceY = findSupportingSurfaceY(player);
        if (Double.isNaN(surfaceY)) {
            return;
        }

        boolean pressingJump = player.input != null && player.input.keyPresses.jump();
        double liftStrength = 0.06D + 0.05D * MovementFeatureManager.getConfiguredValue("lava_walk", 0.90F);
        double distanceToSurface = surfaceY - player.getY();

        if (isInsideWalkableLiquid(player)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, liftStrength), motion.z);
            if (!pressingJump && distanceToSurface >= 0.0D && distanceToSurface <= SURFACE_SNAP_RANGE) {
                player.setPos(player.getX(), surfaceY, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, Math.max(player.getDeltaMovement().y, 0.0D),
                        player.getDeltaMovement().z);
            }
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
            return;
        }

        if (!pressingJump && player.getDeltaMovement().y < 0.0D && distanceToSurface >= -0.05D && distanceToSurface <= 0.20D) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, Math.max(motion.y, -0.02D), motion.z);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
        }
    }

    private static boolean isAvailable(LocalPlayer player) {
        return MovementFeatureManager.isEnabled("lava_walk")
                && player != null
                && player.level() != null
                && !player.getAbilities().flying
                && !player.isPassenger()
                && !isSneakBypassActive(player);
    }

    private static double findSupportingSurfaceY(LocalPlayer player) {
        AABB footprint = player.getBoundingBox().deflate(FOOTPRINT_SHRINK, 0.0D, FOOTPRINT_SHRINK);
        int minX = Mth.floor(footprint.minX);
        int maxX = Mth.floor(footprint.maxX + 0.999D);
        int minY = Mth.floor(footprint.minY - 1.20D);
        int maxY = Mth.floor(footprint.minY + 0.20D);
        int minZ = Mth.floor(footprint.minZ);
        int maxZ = Mth.floor(footprint.maxZ + 0.999D);

        double bestSurfaceY = Double.NaN;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos cursor = new BlockPos(x, y, z);
                    if (!isWalkableLiquid(player.level().getBlockState(cursor))) {
                        continue;
                    }
                    double candidateSurfaceY = cursor.getY() + 1.0D;
                    if (Double.isNaN(bestSurfaceY) || candidateSurfaceY > bestSurfaceY) {
                        bestSurfaceY = candidateSurfaceY;
                    }
                }
            }
        }
        return bestSurfaceY;
    }

    private static boolean isWalkableLiquid(BlockState state) {
        if (state == null) {
            return false;
        }
        if (state.getFluidState().isEmpty()) {
            return false;
        }
        if (MovementFeatureManager.isLiquidWalkDangerousOnly()) {
            return state.getFluidState().is(FluidTags.LAVA);
        }
        if (state.getFluidState().is(FluidTags.WATER) && !MovementFeatureManager.shouldLiquidWalkOnWater()) {
            return false;
        }
        return state.getFluidState().is(FluidTags.WATER) || state.getFluidState().is(FluidTags.LAVA);
    }

    private static boolean isInsideWalkableLiquid(LocalPlayer player) {
        if (player == null || player.level() == null) {
            return false;
        }

        AABB bounds = player.getBoundingBox().deflate(0.001D, 0.0D, 0.001D);
        int minX = Mth.floor(bounds.minX);
        int maxX = Mth.floor(bounds.maxX + 0.999D);
        int minY = Mth.floor(bounds.minY);
        int maxY = Mth.floor(bounds.maxY + 0.999D);
        int minZ = Mth.floor(bounds.minZ);
        int maxZ = Mth.floor(bounds.maxZ + 0.999D);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isWalkableLiquid(player.level().getBlockState(new BlockPos(x, y, z)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isSneakBypassActive(LocalPlayer player) {
        return player != null
                && MovementFeatureManager.shouldLiquidWalkSneakToDescend()
                && player.input != null
                && player.input.keyPresses.shift();
    }
}




