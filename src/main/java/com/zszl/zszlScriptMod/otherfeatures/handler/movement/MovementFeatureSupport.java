package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import com.zszl.zszlScriptMod.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class MovementFeatureSupport {

    private static final double SAFE_WALK_CLIP_STEP = 0.05D;
    private static final Direction[] PLACE_SEARCH_ORDER = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
    };

    static final class PlacementTarget {
        final BlockPos placePos;
        final BlockPos supportPos;
        final Direction supportFace;
        final Vec3 hitVec;
        final int hotbarSlot;

        private PlacementTarget(BlockPos placePos, BlockPos supportPos, Direction supportFace, Vec3 hitVec,
                int hotbarSlot) {
            this.placePos = placePos;
            this.supportPos = supportPos;
            this.supportFace = supportFace;
            this.hitVec = hitVec;
            this.hotbarSlot = hotbarSlot;
        }
    }

    private MovementFeatureSupport() {
    }

    static boolean isMoving(LocalPlayer player) {
        ClientInput input = player == null ? null : player.input;
        return input != null && (Math.abs(input.moveVector.y) > 0.01F || Math.abs(input.moveVector.x) > 0.01F);
    }

    static double getBaseMoveSpeed() {
        return 0.2873D;
    }

    static double getHorizontalSpeed(LocalPlayer player) {
        if (player == null) {
            return 0.0D;
        }
        Vec3 motion = player.getDeltaMovement();
        return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
    }

    static void ensureHorizontalSpeed(LocalPlayer player, double minimumSpeed) {
        if (player == null || !isMoving(player)) {
            return;
        }
        if (getHorizontalSpeed(player) + 1.0E-4D >= minimumSpeed) {
            return;
        }
        setHorizontalSpeed(player, minimumSpeed);
    }

    static void capHorizontalSpeed(LocalPlayer player, double maxSpeed) {
        if (player == null || !isMoving(player)) {
            return;
        }
        double current = getHorizontalSpeed(player);
        if (current <= maxSpeed) {
            return;
        }
        setHorizontalSpeed(player, maxSpeed);
    }

    static void setHorizontalSpeed(LocalPlayer player, double speed) {
        if (player == null) {
            return;
        }
        double[] dir = forward(player, speed);
        player.setDeltaMovement(dir[0], player.getDeltaMovement().y, dir[1]);
        player.hurtMarked = true;
    }

    static boolean hasGroundBelow(LocalPlayer player, double distance) {
        return player != null
                && player.level() != null
                && hasBlockCollision(player.level(), player, player.getBoundingBox().move(0.0D, -distance, 0.0D));
    }

    static boolean isStandingOnIce(LocalPlayer player) {
        if (player == null || player.level() == null) {
            return false;
        }
        BlockPos underPos = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.05D, player.getZ());
        Block block = player.level().getBlockState(underPos).getBlock();
        return block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.FROSTED_ICE
                || block == Blocks.BLUE_ICE;
    }

    static Vec3 getMovementHeading(LocalPlayer player) {
        if (player == null) {
            return Vec3.ZERO;
        }

        double horizontalSpeed = getHorizontalSpeed(player);
        if (horizontalSpeed > 0.05D) {
            Vec3 motion = player.getDeltaMovement();
            return normalizeHorizontal(new Vec3(motion.x, 0.0D, motion.z));
        }

        return normalizeHorizontal(getInputVector(player));
    }

    static Vec3 rotateLeft(Vec3 direction) {
        return normalizeHorizontal(new Vec3(-direction.z, 0.0D, direction.x));
    }

    static Vec3 rotateRight(Vec3 direction) {
        return normalizeHorizontal(new Vec3(direction.z, 0.0D, -direction.x));
    }

    static Vec3 blendHorizontal(Vec3 primary, double primaryWeight, Vec3 secondary, double secondaryWeight) {
        return normalizeHorizontal(primary.scale(primaryWeight).add(secondary.scale(secondaryWeight)));
    }

    static boolean canOccupy(LocalPlayer player, double x, double y, double z) {
        if (player == null || player.level() == null) {
            return false;
        }
        AABB bb = getBoundingBoxAt(player, x, y, z).deflate(0.001D, 0.0D, 0.001D);
        return player.level().noCollision(player, bb);
    }

    static boolean canStandAt(LocalPlayer player, double x, double y, double z) {
        if (player == null || player.level() == null) {
            return false;
        }
        AABB bb = getBoundingBoxAt(player, x, y, z);
        if (!player.level().noCollision(player, bb.deflate(0.001D, 0.0D, 0.001D))) {
            return false;
        }
        return hasSupportBelow(player, shrinkHorizontal(bb, 0.08D), 0.60D) && !isHazardousBelow(player, bb);
    }

    static boolean canStepUp(LocalPlayer player, Vec3 direction) {
        if (player == null
                || player.level() == null
                || direction == null
                || direction.lengthSqr() < 1.0E-4D
                || player.maxUpStep() < 0.9F) {
            return false;
        }

        double probe = 0.55D;
        return canStandAt(player,
                player.getX() + direction.x * probe,
                player.getY() + 1.0D,
                player.getZ() + direction.z * probe);
    }

    static boolean hasObstacleAhead(LocalPlayer player, Vec3 direction, double distance) {
        if (player == null || player.level() == null || direction == null || direction.lengthSqr() < 1.0E-4D) {
            return false;
        }

        Vec3 normalized = normalizeHorizontal(direction);
        for (double step = 0.25D; step <= distance; step += 0.25D) {
            AABB moved = player.getBoundingBox().move(normalized.x * step, 0.0D, normalized.z * step)
                    .deflate(0.02D, 0.0D, 0.02D);
            if (hasBlockCollision(player.level(), player, moved)) {
                return true;
            }
        }
        return false;
    }

    static void clampHorizontalMotionToSafeWalk(LocalPlayer player, double edgeMargin) {
        if (player == null || player.level() == null) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        double motionX = motion.x;
        double motionZ = motion.z;
        if (Math.abs(motionX) < 1.0E-4D && Math.abs(motionZ) < 1.0E-4D) {
            return;
        }

        double clippedX = clipSafeWalkAxis(player, motionX, 0.0D, edgeMargin);
        double clippedZ = clipSafeWalkAxis(player, 0.0D, motionZ, edgeMargin);
        double diagonalX = clippedX;
        double diagonalZ = clippedZ;

        while (Math.abs(diagonalX) > 1.0E-4D
                && Math.abs(diagonalZ) > 1.0E-4D
                && !hasSafeWalkSupport(player, diagonalX, diagonalZ, edgeMargin)) {
            diagonalX = approachZero(diagonalX, SAFE_WALK_CLIP_STEP);
            diagonalZ = approachZero(diagonalZ, SAFE_WALK_CLIP_STEP);
        }

        if (Math.abs(diagonalX - motionX) < 1.0E-4D && Math.abs(diagonalZ - motionZ) < 1.0E-4D) {
            return;
        }

        player.setDeltaMovement(diagonalX, motion.y, diagonalZ);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    static Vec3 findNearestSafePosition(LocalPlayer player, double maxRadius, Vec3 preferredDirection) {
        if (player == null || player.level() == null) {
            return null;
        }

        Vec3 preferred = normalizeHorizontal(preferredDirection);
        Vec3 best = null;
        double bestScore = Double.MAX_VALUE;

        for (double radius = 0.15D; radius <= maxRadius + 1.0E-4D; radius += 0.15D) {
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2.0D * i) / 16.0D;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                double score = radius;

                if (preferred.lengthSqr() > 0.0D) {
                    Vec3 candidateDir = normalizeHorizontal(new Vec3(offsetX, 0.0D, offsetZ));
                    score -= preferred.dot(candidateDir) * 0.18D;
                }

                for (double yOffset : new double[] { 0.0D, 1.0D, -1.0D, 0.5D, -0.5D }) {
                    double targetX = player.getX() + offsetX;
                    double targetY = player.getY() + yOffset;
                    double targetZ = player.getZ() + offsetZ;
                    if (!canStandAt(player, targetX, targetY, targetZ)) {
                        continue;
                    }
                    double adjustedScore = score + Math.abs(yOffset) * 0.12D;
                    if (best == null || adjustedScore < bestScore) {
                        best = new Vec3(targetX, targetY, targetZ);
                        bestScore = adjustedScore;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }

        return best;
    }

    static Vec3 findBestBlinkDestination(LocalPlayer player, double maxDistance) {
        if (player == null || player.level() == null) {
            return null;
        }

        Vec3 heading = getMovementHeading(player);
        if (heading.lengthSqr() < 1.0E-4D) {
            return null;
        }

        Vec3 best = null;
        for (double step = 0.50D; step <= maxDistance + 1.0E-4D; step += 0.25D) {
            double targetX = player.getX() + heading.x * step;
            double targetY = player.getY();
            double targetZ = player.getZ() + heading.z * step;
            if (canStandAt(player, targetX, targetY, targetZ)) {
                best = new Vec3(targetX, targetY, targetZ);
            } else if (canStandAt(player, targetX, targetY + 1.0D, targetZ)) {
                best = new Vec3(targetX, targetY + 1.0D, targetZ);
            } else if (canStandAt(player, targetX, targetY - 1.0D, targetZ)) {
                best = new Vec3(targetX, targetY - 1.0D, targetZ);
            } else {
                break;
            }
        }
        return best;
    }

    static PlacementTarget findScaffoldPlacement(LocalPlayer player, BlockPos targetPos) {
        if (player == null || player.level() == null || targetPos == null || !isReplaceable(player.level(), targetPos)) {
            return null;
        }

        int hotbarSlot = findHotbarPlaceableBlockSlot(player);
        if (hotbarSlot < 0) {
            return null;
        }

        for (Direction facing : PLACE_SEARCH_ORDER) {
            BlockPos supportPos = targetPos.relative(facing);
            if (!canPlaceAgainst(player.level(), supportPos)) {
                continue;
            }
            Direction supportFace = facing.getOpposite();
            Vec3 hitVec = Vec3.atCenterOf(supportPos).add(
                    supportFace.getStepX() * 0.5D,
                    supportFace.getStepY() * 0.5D,
                    supportFace.getStepZ() * 0.5D);
            return new PlacementTarget(targetPos, supportPos, supportFace, hitVec, hotbarSlot);
        }

        return null;
    }

    static boolean placeFromScaffold(LocalPlayer player, PlacementTarget target) {
        if (player == null || target == null || player.level() == null || player.connection == null) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameMode == null || mc.level == null) {
            return false;
        }

        int originalSlot = player.getInventory().getSelectedSlot();
        if (!ModUtils.switchToHotbarSlot(target.hotbarSlot + 1)) {
            return false;
        }

        BlockHitResult hitResult = new BlockHitResult(target.hitVec, target.supportFace, target.supportPos, false);
        InteractionResult result = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
        player.swing(InteractionHand.MAIN_HAND);

        if (originalSlot != target.hotbarSlot) {
            ModUtils.switchToHotbarSlot(originalSlot + 1);
        }

        return result.consumesAction();
    }

    private static int findHotbarPlaceableBlockSlot(LocalPlayer player) {
        Level level = player.level();
        BlockPos contextPos = player.blockPosition();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }

            Block block = blockItem.getBlock();
            BlockState state = block.defaultBlockState();
            if (block == Blocks.AIR
                    || block instanceof FallingBlock
                    || state.hasBlockEntity()
                    || !state.getFluidState().isEmpty()
                    || !state.isCollisionShapeFullBlock(level, contextPos)
                    || state.canBeReplaced()) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private static boolean canPlaceAgainst(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir()
                && !state.canBeReplaced()
                && !state.getCollisionShape(level, pos).isEmpty()
                && level.getBlockEntity(pos) == null;
    }

    private static boolean isReplaceable(Level level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced();
    }

    private static AABB getBoundingBoxAt(LocalPlayer player, double x, double y, double z) {
        return player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
    }

    private static boolean hasSupportBelow(LocalPlayer player, AABB box, double depth) {
        return hasBlockCollision(player.level(), player, box.move(0.0D, -depth, 0.0D));
    }

    private static boolean isHazardousBelow(LocalPlayer player, AABB box) {
        Level level = player.level();
        int minX = Mth.floor(box.minX + 0.01D);
        int maxX = Mth.floor(box.maxX - 0.01D);
        int minZ = Mth.floor(box.minZ + 0.01D);
        int maxZ = Mth.floor(box.maxZ - 0.01D);
        int y = Mth.floor(box.minY - 0.08D);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block == Blocks.LAVA
                        || block == Blocks.FIRE
                        || block == Blocks.CACTUS
                        || block == Blocks.MAGMA_BLOCK
                        || block == Blocks.CAMPFIRE
                        || block == Blocks.SOUL_CAMPFIRE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AABB shrinkHorizontal(AABB box, double amount) {
        double maxShrinkX = Math.min(amount, Math.max(0.0D, (box.maxX - box.minX) * 0.49D));
        double maxShrinkZ = Math.min(amount, Math.max(0.0D, (box.maxZ - box.minZ) * 0.49D));
        return new AABB(box.minX + maxShrinkX, box.minY, box.minZ + maxShrinkZ,
                box.maxX - maxShrinkX, box.maxY, box.maxZ - maxShrinkZ);
    }

    private static double clipSafeWalkAxis(LocalPlayer player, double motionX, double motionZ, double edgeMargin) {
        double clippedX = motionX;
        double clippedZ = motionZ;
        while ((Math.abs(clippedX) > 1.0E-4D || Math.abs(clippedZ) > 1.0E-4D)
                && !hasSafeWalkSupport(player, clippedX, clippedZ, edgeMargin)) {
            clippedX = approachZero(clippedX, SAFE_WALK_CLIP_STEP);
            clippedZ = approachZero(clippedZ, SAFE_WALK_CLIP_STEP);
        }
        return Math.abs(motionX) > Math.abs(motionZ) ? clippedX : clippedZ;
    }

    private static boolean hasSafeWalkSupport(LocalPlayer player, double motionX, double motionZ, double edgeMargin) {
        AABB moved = player.getBoundingBox().move(motionX, 0.0D, motionZ);
        double supportInset = Mth.clamp((float) (edgeMargin * 0.18D), 0.0F, 0.18F);
        AABB supportBox = supportInset <= 1.0E-4D ? moved : shrinkHorizontal(moved, supportInset);
        return hasSupportBelow(player, supportBox, 1.0D);
    }

    private static double approachZero(double value, double amount) {
        if (value > 0.0D) {
            return Math.max(0.0D, value - amount);
        }
        if (value < 0.0D) {
            return Math.min(0.0D, value + amount);
        }
        return 0.0D;
    }

    private static boolean hasBlockCollision(Level level, Entity entity, AABB box) {
        return level.getBlockCollisions(entity, box).iterator().hasNext();
    }

    private static Vec3 getInputVector(LocalPlayer player) {
        net.minecraft.client.player.ClientInput input = player == null ? null : player.input;
        float forward = input == null ? 0.0F : input.moveVector.y;
        float strafe = input == null ? 0.0F : input.moveVector.x;
        if (Math.abs(forward) < 0.01F && Math.abs(strafe) < 0.01F) {
            return Vec3.ZERO;
        }

        double rad = Math.toRadians(player.getYRot());
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return new Vec3(strafe * cos - forward * sin, 0.0D, forward * cos + strafe * sin);
    }

    private static Vec3 normalizeHorizontal(Vec3 vector) {
        if (vector == null) {
            return Vec3.ZERO;
        }
        double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        if (length < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return new Vec3(vector.x / length, 0.0D, vector.z / length);
    }

    private static double[] forward(LocalPlayer player, double speed) {
        Vec3 direction = normalizeHorizontal(getInputVector(player));
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = getMovementHeading(player);
        }
        return new double[] { direction.x * speed, direction.z * speed };
    }
}
