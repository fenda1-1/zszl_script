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

package com.zszl.zszlScriptMod.shadowbaritone.api.utils;

import com.zszl.zszlScriptMod.shadowbaritone.api.cache.IWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IPlayerContext {

    Minecraft minecraft();

    LocalPlayer player();

    IPlayerController playerController();

    Level world();

    default Iterable<Entity> entities() {
        return ((ClientLevel) world()).entitiesForRendering();
    }

    default Stream<Entity> entitiesStream() {
        return StreamSupport.stream(entities().spliterator(), false);
    }


    IWorldData worldData();

    HitResult objectMouseOver();

    default BetterBlockPos playerFeet() {
        // TODO find a better way to deal with soul sand!!!!!
        BetterBlockPos feet = new BetterBlockPos(player().position().x, player().position().y + 0.1251, player().position().z);
        BetterBlockPos liquidFeet = resolveLiquidFootprintPos();
        if (liquidFeet != null) {
            feet = liquidFeet;
        } else {
            BetterBlockPos footprintFeet = resolveFootprintStandingPos(feet);
            if (footprintFeet != null) {
                feet = footprintFeet;
            }
        }

        // sometimes when calling this from another thread or while world is null, it'll throw a NullPointerException
        // that causes the game to immediately crash
        //
        // so of course crashing on 2b is horribly bad due to queue times and logout spot
        // catch the NPE and ignore it if it does happen
        //
        // this does not impact performance at all since we're not null checking constantly
        // if there is an exception, the only overhead is Java generating the exception object... so we can ignore it
        try {
            if (world().getBlockState(feet).getBlock() instanceof SlabBlock) {
                return feet.above();
            }
        } catch (NullPointerException ignored) {}

        return feet;
    }

    default BetterBlockPos resolveLiquidFootprintPos() {
        if (player() == null || world() == null) {
            return null;
        }
        AABB boundingBox = player().getBoundingBox();
        if (boundingBox == null) {
            return null;
        }

        double epsilon = 1.0E-4D;
        double minX = boundingBox.minX + epsilon;
        double maxX = boundingBox.maxX - epsilon;
        double minZ = boundingBox.minZ + epsilon;
        double maxZ = boundingBox.maxZ - epsilon;
        if (maxX < minX) {
            minX = maxX = player().position().x;
        }
        if (maxZ < minZ) {
            minZ = maxZ = player().position().z;
        }

        int minBlockX = Mth.floor(minX);
        int maxBlockX = Mth.floor(maxX);
        int minBlockZ = Mth.floor(minZ);
        int maxBlockZ = Mth.floor(maxZ);
        int liquidY = Mth.floor(boundingBox.minY + epsilon);

        BetterBlockPos best = null;
        double bestOverlap = -1.0D;
        double bestDistanceSq = Double.POSITIVE_INFINITY;

        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                BetterBlockPos candidate = new BetterBlockPos(x, liquidY, z);
                if (!isLiquidFootCandidate(candidate)) {
                    continue;
                }

                double overlapX = overlapLength(minX, maxX, x, x + 1.0D);
                double overlapZ = overlapLength(minZ, maxZ, z, z + 1.0D);
                double overlapArea = overlapX * overlapZ;
                double dx = (candidate.x + 0.5D) - player().position().x;
                double dz = (candidate.z + 0.5D) - player().position().z;
                double distanceSq = dx * dx + dz * dz;

                if (best == null
                        || overlapArea > bestOverlap + 1.0E-6D
                        || (Math.abs(overlapArea - bestOverlap) <= 1.0E-6D
                                && distanceSq < bestDistanceSq - 1.0E-6D)) {
                    best = candidate;
                    bestOverlap = overlapArea;
                    bestDistanceSq = distanceSq;
                }
            }
        }

        return best;
    }

    default BetterBlockPos resolveFootprintStandingPos(BetterBlockPos defaultFeet) {
        if (player() == null) {
            return defaultFeet;
        }
        AABB boundingBox = player().getBoundingBox();
        if (boundingBox == null) {
            return defaultFeet;
        }

        double epsilon = 1.0E-4D;
        double minX = boundingBox.minX + epsilon;
        double maxX = boundingBox.maxX - epsilon;
        double minZ = boundingBox.minZ + epsilon;
        double maxZ = boundingBox.maxZ - epsilon;
        if (maxX < minX) {
            minX = maxX = player().position().x;
        }
        if (maxZ < minZ) {
            minZ = maxZ = player().position().z;
        }

        int minBlockX = Mth.floor(minX);
        int maxBlockX = Mth.floor(maxX);
        int minBlockZ = Mth.floor(minZ);
        int maxBlockZ = Mth.floor(maxZ);

        BetterBlockPos best = null;
        double bestOverlap = -1.0D;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        boolean bestMatchesDefault = false;

        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                BetterBlockPos candidate = adjustStandingCandidate(new BetterBlockPos(x, defaultFeet.y, z));
                if (!isUsableStandingCandidate(candidate)) {
                    continue;
                }

                double overlapX = overlapLength(minX, maxX, x, x + 1.0D);
                double overlapZ = overlapLength(minZ, maxZ, z, z + 1.0D);
                double overlapArea = overlapX * overlapZ;
                double dx = (candidate.x + 0.5D) - player().position().x;
                double dz = (candidate.z + 0.5D) - player().position().z;
                double distanceSq = dx * dx + dz * dz;
                boolean matchesDefault = candidate.equals(defaultFeet);

                if (best == null
                        || overlapArea > bestOverlap + 1.0E-6D
                        || (Math.abs(overlapArea - bestOverlap) <= 1.0E-6D
                                && (matchesDefault && !bestMatchesDefault
                                        || (matchesDefault == bestMatchesDefault
                                                && distanceSq < bestDistanceSq - 1.0E-6D)))) {
                    best = candidate;
                    bestOverlap = overlapArea;
                    bestDistanceSq = distanceSq;
                    bestMatchesDefault = matchesDefault;
                }
            }
        }

        return best;
    }

    static double overlapLength(double minA, double maxA, double minB, double maxB) {
        return Math.max(0.0D, Math.min(maxA, maxB) - Math.max(minA, minB));
    }

    default BetterBlockPos adjustStandingCandidate(BetterBlockPos candidate) {
        if (candidate == null) {
            return null;
        }
        try {
            BlockState standingState = world().getBlockState(candidate);
            if (standingState.getBlock() instanceof SlabBlock
                    || standingState.getBlock() instanceof StairBlock) {
                return candidate.above();
            }
        } catch (NullPointerException ignored) {
        }
        return candidate;
    }

    default boolean isUsableStandingCandidate(BetterBlockPos candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            BlockState feetState = world().getBlockState(candidate);
            BlockState headState = world().getBlockState(candidate.above());
            BlockState groundState = world().getBlockState(candidate.below());
            boolean feetPassable = !feetState.blocksMotion();
            boolean headPassable = !headState.blocksMotion();
            boolean hasGround = groundState.blocksMotion()
                    || groundState.getBlock() instanceof SlabBlock
                    || groundState.getBlock() instanceof StairBlock;
            return feetPassable && headPassable && hasGround;
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    default boolean isLiquidFootCandidate(BetterBlockPos candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            return world().getBlockState(candidate).getBlock() instanceof LiquidBlock;
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    default Vec3 playerFeetAsVec() {
        return new Vec3(player().position().x, player().position().y, player().position().z);
    }

    default Vec3 playerHead() {
        return new Vec3(player().position().x, player().position().y + player().getEyeHeight(), player().position().z);
    }

    default Vec3 playerMotion() {
        return player().getDeltaMovement();
    }

    BetterBlockPos viewerPos();

    default Rotation playerRotations() {
        return new Rotation(player().getYRot(), player().getXRot());
    }

    /**
     * Returns the player's eye height, taking into account whether or not the player is sneaking.
     *
     * @param ifSneaking Whether or not the player is sneaking
     * @return The player's eye height
     * @deprecated Use entity.getEyeHeight(Pose.CROUCHING) instead
     */
    @Deprecated
    static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.27 : 1.62;
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    default Optional<BlockPos> getSelectedBlock() {
        HitResult result = objectMouseOver();
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            return Optional.of(((BlockHitResult) result).getBlockPos());
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}

