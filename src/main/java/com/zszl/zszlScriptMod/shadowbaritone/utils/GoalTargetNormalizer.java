package com.zszl.zszlScriptMod.shadowbaritone.utils;

import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalBlock;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalComposite;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.MovementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public final class GoalTargetNormalizer {

    private GoalTargetNormalizer() {
    }

    public static Goal normalize(IBaritone baritone, Goal goal) {
        if (goal == null) {
            return null;
        }
        if (goal instanceof GoalComposite) {
            return normalizeGoalComposite(baritone, (GoalComposite) goal);
        }
        if (!(goal instanceof GoalBlock)) {
            return goal;
        }
        return normalizeGoalBlock(baritone, (GoalBlock) goal);
    }

    private static Goal normalizeGoalComposite(IBaritone baritone, GoalComposite goal) {
        Goal[] children = goal.goals();
        Goal[] normalizedChildren = new Goal[children.length];
        boolean changed = false;
        for (int i = 0; i < children.length; i++) {
            normalizedChildren[i] = normalize(baritone, children[i]);
            changed |= normalizedChildren[i] != children[i];
        }
        return changed ? new GoalComposite(normalizedChildren) : goal;
    }

    public static GoalBlock normalizeGoalBlock(IBaritone baritone, GoalBlock goal) {
        if (baritone == null || goal == null) {
            return goal;
        }

        BlockPos pos = goal.getGoalPos();
        BlockStateInterface bsi = new BlockStateInterface(baritone.getPlayerContext());
        int minY = bsi.world.dimensionType().minY();
        int maxYExclusive = minY + bsi.world.dimensionType().height();
        if (pos.getY() < minY || pos.getY() >= maxYExclusive) {
            return goal;
        }
        if (!bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ())) {
            return goal;
        }
        if (BaritoneAPI.getSettings().allowBreak.value) {
            return goal;
        }
        if (canStandAt(bsi, pos)) {
            return goal;
        }

        GoalBlock standingAbove = normalizeToStandingAboveSurface(bsi, pos);
        if (standingAbove != null) {
            return standingAbove;
        }

        GoalBlock corrected = findClosestStandableGoal(bsi, pos);
        return corrected != null ? corrected : goal;
    }

    private static GoalBlock normalizeToStandingAboveSurface(BlockStateInterface bsi, BlockPos pos) {
        BlockState targetState = bsi.get0(pos);
        if (!MovementHelper.canWalkOn(bsi, pos.getX(), pos.getY(), pos.getZ(), targetState)) {
            return null;
        }
        BlockPos standPos = pos.above();
        return canStandAt(bsi, standPos) ? new GoalBlock(standPos) : null;
    }

    private static GoalBlock findClosestStandableGoal(BlockStateInterface bsi, BlockPos origin) {
        int minStandY = bsi.world.dimensionType().minY() + 1;
        int maxStandY = bsi.world.dimensionType().minY() + bsi.world.dimensionType().height() - 2;
        if (minStandY > maxStandY) {
            return null;
        }

        int startY = Mth.clamp(origin.getY(), minStandY, maxStandY);
        int maxDistance = Math.max(startY - minStandY, maxStandY - startY);
        for (int offset = 0; offset <= maxDistance; offset++) {
            int upY = startY + offset;
            GoalBlock upward = toStandGoalIfValid(bsi, origin.getX(), upY, origin.getZ());
            if (upward != null) {
                return upward;
            }
            if (offset == 0) {
                continue;
            }
            int downY = startY - offset;
            GoalBlock downward = toStandGoalIfValid(bsi, origin.getX(), downY, origin.getZ());
            if (downward != null) {
                return downward;
            }
        }
        return null;
    }

    private static GoalBlock toStandGoalIfValid(BlockStateInterface bsi, int x, int y, int z) {
        BlockPos standPos = new BlockPos(x, y, z);
        return canStandAt(bsi, standPos) ? new GoalBlock(standPos) : null;
    }

    private static boolean canStandAt(BlockStateInterface bsi, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return MovementHelper.canWalkThrough(bsi, x, y, z)
                && MovementHelper.canWalkThrough(bsi, x, y + 1, z)
                && MovementHelper.canWalkOn(bsi, x, y - 1, z);
    }
}
