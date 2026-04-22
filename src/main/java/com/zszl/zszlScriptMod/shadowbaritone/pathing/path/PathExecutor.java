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

package com.zszl.zszlScriptMod.shadowbaritone.pathing.path;

import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.calc.IPath;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.movement.ActionCosts;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.movement.IMovement;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.movement.MovementStatus;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.path.IPathExecutor;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.*;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.input.Input;
import com.zszl.zszlScriptMod.shadowbaritone.behavior.PathingBehavior;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.calc.AbstractNodeCostSearch;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.CalculationContext;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.Movement;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.MovementHelper;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.movements.*;
import com.zszl.zszlScriptMod.shadowbaritone.utils.BlockStateInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import java.util.*;

import static com.zszl.zszlScriptMod.shadowbaritone.api.pathing.movement.MovementStatus.*;

/**
 * Behavior to execute a precomputed path
 *
 * @author leijurv
 */
public class PathExecutor implements IPathExecutor, Helper {

    private static final double MAX_MAX_DIST_FROM_PATH = 3;
    private static final double MAX_DIST_FROM_PATH = 2;
    private static final double ROUTE_PROGRESS_LENIENCY = 0.9D;
    private static final double ROUTE_PAUSE_LENIENCY = 0.85D;
    private static final int MAX_PATH_REEVALUATIONS_PER_TICK = 96;
    private static final int MAX_ROUTE_PROGRESS_LOOKAHEAD = 4;

    /**
     * Default value is equal to 10 seconds. It's find to decrease it, but it must be at least 5.5s (110 ticks).
     * For more information, see issue #102.
     *
     * @see <a href="https://github.com/cabaletta/baritone/issues/102">Issue #102</a>
     * @see <a href="https://i.imgur.com/5s5GLnI.png">Anime</a>
     */
    private static final double MAX_TICKS_AWAY = 200;

    private final IPath path;
    private int pathPosition;
    private int ticksAway;
    private int ticksOnCurrent;
    private Double currentMovementOriginalCostEstimate;
    private Integer costEstimateIndex;
    private boolean failed;
    private boolean recalcBP = true;
    private HashSet<BlockPos> toBreak = new HashSet<>();
    private HashSet<BlockPos> toPlace = new HashSet<>();
    private HashSet<BlockPos> toWalkInto = new HashSet<>();

    private final PathingBehavior behavior;
    private final IPlayerContext ctx;

    private boolean sprintNextTick;
    private boolean restartRequested;

    public PathExecutor(PathingBehavior behavior, IPath path) {
        this.behavior = behavior;
        this.ctx = behavior.ctx;
        this.path = path;
        this.pathPosition = 0;
    }

    /**
     * Tick this executor
     *
     * @return True if a movement just finished (and the player is therefore in a "stable" state, like,
     * not sneaking out over lava), false otherwise
     */
    public boolean onTick() {
        for (int reevaluations = 0; reevaluations < MAX_PATH_REEVALUATIONS_PER_TICK; reevaluations++) {
            this.restartRequested = false;

            if (pathPosition == path.length() - 1) {
                pathPosition++;
            }
            if (pathPosition >= path.length()) {
                return true;
            }

            Movement movement = (Movement) path.movements().get(pathPosition);
            BetterBlockPos whereAmI = ctx.playerFeet();

            if (tryAdvanceToCurrentPathPosition(whereAmI, movement)) {
                continue;
            }

            if (!movement.getValidPositions().contains(whereAmI)) {
                for (int i = 0; i < pathPosition && i < path.length(); i++) {
                    if (((Movement) path.movements().get(i)).getValidPositions().contains(whereAmI)) {
                        int previousPos = pathPosition;
                        pathPosition = i;
                        for (int j = pathPosition; j <= previousPos; j++) {
                            path.movements().get(j).reset();
                        }
                        onChangeInPathPosition();
                        this.restartRequested = true;
                        break;
                    }
                }
                if (this.restartRequested) {
                    continue;
                }
                for (int i = pathPosition + 3; i < path.length() - 1; i++) {
                    if (((Movement) path.movements().get(i)).getValidPositions().contains(whereAmI)) {
                        if (i - pathPosition > 2) {
                            logDebug("Skipping forward " + (i - pathPosition) + " steps, to " + i);
                        }
                        pathPosition = i - 1;
                        onChangeInPathPosition();
                        this.restartRequested = true;
                        break;
                    }
                }
                if (this.restartRequested) {
                    continue;
                }
            }

            Tuple<Double, BlockPos> status = closestPathPos(path);
            if (possiblyOffPath(status, MAX_DIST_FROM_PATH)) {
                ticksAway++;
                System.out.println("FAR AWAY FROM PATH FOR " + ticksAway + " TICKS. Current distance: " + status.getA() + ". Threshold: " + MAX_DIST_FROM_PATH);
                if (ticksAway > MAX_TICKS_AWAY) {
                    logDebug("Too far away from path for too long, cancelling path");
                    cancel();
                    return false;
                }
            } else {
                ticksAway = 0;
            }
            if (possiblyOffPath(status, MAX_MAX_DIST_FROM_PATH)) {
                logDebug("too far from path");
                cancel();
                return false;
            }

            BlockStateInterface bsi = new BlockStateInterface(ctx);
            for (int i = pathPosition - 10; i < pathPosition + 10; i++) {
                if (i < 0 || i >= path.movements().size()) {
                    continue;
                }
                Movement m = (Movement) path.movements().get(i);
                List<BlockPos> prevBreak = m.toBreak(bsi);
                List<BlockPos> prevPlace = m.toPlace(bsi);
                List<BlockPos> prevWalkInto = m.toWalkInto(bsi);
                m.resetBlockCache();
                if (!prevBreak.equals(m.toBreak(bsi))) {
                    recalcBP = true;
                }
                if (!prevPlace.equals(m.toPlace(bsi))) {
                    recalcBP = true;
                }
                if (!prevWalkInto.equals(m.toWalkInto(bsi))) {
                    recalcBP = true;
                }
            }
            if (recalcBP) {
                HashSet<BlockPos> newBreak = new HashSet<>();
                HashSet<BlockPos> newPlace = new HashSet<>();
                HashSet<BlockPos> newWalkInto = new HashSet<>();
                for (int i = pathPosition; i < path.movements().size(); i++) {
                    Movement m = (Movement) path.movements().get(i);
                    newBreak.addAll(m.toBreak(bsi));
                    newPlace.addAll(m.toPlace(bsi));
                    newWalkInto.addAll(m.toWalkInto(bsi));
                }
                toBreak = newBreak;
                toPlace = newPlace;
                toWalkInto = newWalkInto;
                recalcBP = false;
            }

            if (pathPosition < path.movements().size() - 1) {
                IMovement next = path.movements().get(pathPosition + 1);
                if (!behavior.baritone.bsi.worldContainsLoadedChunk(next.getDest().x, next.getDest().z)) {
                    logDebug("Pausing since destination is at edge of loaded chunks");
                    clearKeys();
                    return true;
                }
            }

            boolean canCancel = movement.safeToCancel();
            if (costEstimateIndex == null || costEstimateIndex != pathPosition) {
                costEstimateIndex = pathPosition;
                currentMovementOriginalCostEstimate = movement.getCost();
                for (int i = 1; i < Baritone.settings().costVerificationLookahead.value && pathPosition + i < path.length() - 1; i++) {
                    if (((Movement) path.movements().get(pathPosition + i)).calculateCost(behavior.secretInternalGetCalculationContext()) >= ActionCosts.COST_INF && canCancel) {
                        logDebug("Something has changed in the world and a future movement has become impossible. Cancelling.");
                        cancel();
                        return true;
                    }
                }
            }

            double currentCost = movement.recalculateCost(behavior.secretInternalGetCalculationContext());
            if (currentCost >= ActionCosts.COST_INF && canCancel) {
                logDebug("Something has changed in the world and this movement has become impossible. Cancelling.");
                cancel();
                return true;
            }
            if (!movement.calculatedWhileLoaded() && currentCost - currentMovementOriginalCostEstimate > Baritone.settings().maxCostIncrease.value && canCancel) {
                logDebug("Original cost " + currentMovementOriginalCostEstimate + " current cost " + currentCost + ". Cancelling.");
                cancel();
                return true;
            }
            if (shouldPause()) {
                logDebug("Pausing since current best path is a backtrack");
                clearKeys();
                return true;
            }

            MovementStatus movementStatus = movement.update();
            if (movementStatus == UNREACHABLE || movementStatus == FAILED) {
                logDebug("Movement returns status " + movementStatus);
                cancel();
                return true;
            }
            if (movementStatus == SUCCESS) {
                pathPosition++;
                onChangeInPathPosition();
                if (movement instanceof MovementParkour) {
                    return true;
                }
                continue;
            }

            sprintNextTick = shouldSprintNextTick();
            if (this.restartRequested) {
                continue;
            }
            ctx.player().setSprinting(sprintNextTick);
            if (!sprintNextTick) {
                ctx.player().setSprinting(false);
            }
            ticksOnCurrent++;
            if (ticksOnCurrent > currentMovementOriginalCostEstimate + Baritone.settings().movementTimeoutTicks.value) {
                logDebug("This movement has taken too long (" + ticksOnCurrent + " ticks, expected " + currentMovementOriginalCostEstimate + "). Cancelling.");
                cancel();
                return true;
            }
            return canCancel;
        }

        logDebug("Exceeded per-tick path reevaluation limit while resolving high-speed path progress. Cancelling current path.");
        cancel();
        return false;
    }

    private boolean tryAdvanceToCurrentPathPosition(BetterBlockPos whereAmI, Movement currentMovement) {
        if (whereAmI == null || currentMovement == null) {
            return false;
        }

        AdvanceResolution advance = resolveAdvanceIndex(whereAmI);
        if (!advance.hasAdvance() || advance.index <= pathPosition) {
            return false;
        }
        if (advance.isRouteMatch() && isCoveredByCurrentOrIntermediateMovements(whereAmI, pathPosition, advance.index)) {
            return false;
        }
        if (!currentMovement.safeToCancel()) {
            return false;
        }
        if (!ctx.player().onGround() && ctx.world().getFluidState(ctx.playerFeet()).isEmpty() && ctx.player().getDeltaMovement().y < -0.1D) {
            return false;
        }

        int previous = pathPosition;
        pathPosition = advance.index;
        onChangeInPathPosition();
        if (advance.index - previous > 0) {
            logDebug("Auto-advanced path position from " + previous + " to " + advance.index + " due to high-speed progression / overshoot.");
        }
        return true;
    }

    private boolean isCoveredByCurrentOrIntermediateMovements(BetterBlockPos whereAmI, int fromInclusive, int toExclusive) {
        int endExclusive = Math.min(path.movements().size(), Math.max(fromInclusive + 1, toExclusive));
        for (int i = Math.max(0, fromInclusive); i < endExclusive; i++) {
            IMovement movement = path.movements().get(i);
            if (movement instanceof Movement && ((Movement) movement).getValidPositions().contains(whereAmI)) {
                return true;
            }
        }
        return false;
    }

    private AdvanceResolution resolveAdvanceIndex(BetterBlockPos whereAmI) {
        int exactIndex = path.positions().indexOf(whereAmI);
        if (exactIndex > pathPosition) {
            return AdvanceResolution.exact(exactIndex);
        }

        PathProgressMatch match = matchPathProgress(
                path,
                ROUTE_PROGRESS_LENIENCY,
                pathPosition + 1,
                Math.min(path.movements().size(), pathPosition + 1 + MAX_ROUTE_PROGRESS_LOOKAHEAD)
        );
        if (match.hasMovementMatch() && match.getMovementIndex() > pathPosition) {
            return AdvanceResolution.route(match.getMovementIndex(), match.getExactPositionIndex(), match.getDistance());
        }
        return AdvanceResolution.none();
    }

    private Tuple<Double, BlockPos> closestPathPos(IPath path) {
        double best = -1.0D;
        BlockPos bestPos = null;

        List<BetterBlockPos> positions = path.positions();
        Vec3 playerPos = ctx.player().position();
        for (int i = 0; i + 1 < positions.size(); i++) {
            Vec3 nearest = nearestPointOnSegment(playerPos, blockCenter(positions.get(i)), blockCenter(positions.get(i + 1)));
            double dist = playerPos.distanceTo(nearest);
            if (dist < best || best < 0.0D) {
                best = dist;
                bestPos = BlockPos.containing(nearest);
            }
        }

        for (IMovement movement : path.movements()) {
            for (BlockPos pos : ((Movement) movement).getValidPositions()) {
                double dist = VecUtils.entityDistanceToCenter(ctx.player(), pos);
                if (dist < best || best < 0.0D) {
                    best = dist;
                    bestPos = pos;
                }
            }
        }
        return new Tuple<>(best, bestPos);
    }

    private static Vec3 nearestPointOnSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSq = segment.lengthSqr();
        if (lengthSq <= 1.0E-6D) {
            return start;
        }
        double t = point.subtract(start).dot(segment) / lengthSq;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return start.add(segment.scale(t));
    }

    private static Vec3 blockCenter(BetterBlockPos pos) {
        return new Vec3(pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D);
    }

    private boolean shouldPause() {
        Optional<AbstractNodeCostSearch> current = behavior.getInProgress();
        if (!current.isPresent()) {
            return false;
        }
        if (!ctx.player().onGround()) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx, ctx.playerFeet().below())) {
            // we're in some kind of sketchy situation, maybe parkouring
            return false;
        }
        if (!MovementHelper.canWalkThrough(ctx, ctx.playerFeet()) || !MovementHelper.canWalkThrough(ctx, ctx.playerFeet().above())) {
            // suffocating?
            return false;
        }
        if (!path.movements().get(pathPosition).safeToCancel()) {
            return false;
        }
        Optional<IPath> currentBest = current.get().bestPathSoFar();
        if (!currentBest.isPresent()) {
            return false;
        }
        IPath bestPath = currentBest.get();
        try {
            bestPath = bestPath.postProcess();
        } catch (Throwable ignored) {
        }
        if (bestPath.length() < 3) {
            return false;
        }
        PathProgressMatch match = matchPathProgress(bestPath, ROUTE_PAUSE_LENIENCY, 1);
        if (match.hasExactPositionMatch()) {
            return match.getExactPositionIndex() > 1;
        }
        return match.hasMovementMatch() && match.getMovementIndex() > 0;
    }

    private boolean possiblyOffPath(Tuple<Double, BlockPos> status, double leniency) {
        double distanceFromPath = status.getA();
        if (distanceFromPath > leniency) {
            // when we're midair in the middle of a fall, we're very far from both the beginning and the end, but we aren't actually off path
            if (path.movements().get(pathPosition) instanceof MovementFall) {
                BlockPos fallDest = path.positions().get(pathPosition + 1); // .get(pathPosition) is the block we fell off of
                return VecUtils.entityFlatDistanceToCenter(ctx.player(), fallDest) >= leniency; // ignore Y by using flat distance
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Regardless of current path position, snap to the current player feet if possible
     *
     * @return Whether or not it was possible to snap to the current player feet
     */
    public boolean snipsnapifpossible() {
        if (!ctx.player().onGround() && ctx.world().getFluidState(ctx.playerFeet()).isEmpty()) {
            // if we're falling in the air, and not in water, don't splice
            return false;
        } else {
            // we are either onGround or in liquid
            if (ctx.player().getDeltaMovement().y < -0.1) {
                // if we are strictly moving downwards (not stationary)
                // we could be falling through water, which could be unsafe to splice
                return false; // so don't
            }
        }
        int index = resolveSnipIndex(ctx.playerFeet());
        if (index == -1) {
            return false;
        }
        pathPosition = index; // jump directly to current position
        clearKeys();
        return true;
    }

    private int resolveSnipIndex(BetterBlockPos whereAmI) {
        int exactIndex = path.positions().indexOf(whereAmI);
        if (exactIndex != -1) {
            return exactIndex;
        }
        PathProgressMatch match = matchPathProgress(path, ROUTE_PROGRESS_LENIENCY, 0);
        if (match.hasMovementMatch()) {
            return match.getMovementIndex();
        }
        return -1;
    }

    private PathProgressMatch matchPathProgress(IPath candidatePath, double leniency, int movementStartIndex) {
        return matchPathProgress(candidatePath, leniency, movementStartIndex, Integer.MAX_VALUE);
    }

    private PathProgressMatch matchPathProgress(IPath candidatePath, double leniency, int movementStartIndex, int movementEndExclusive) {
        int exactPositionIndex = candidatePath.positions().indexOf(ctx.playerFeet());
        List<IMovement> candidateMovements;
        try {
            candidateMovements = candidatePath.movements();
        } catch (IllegalStateException ignored) {
            return new PathProgressMatch(exactPositionIndex, -1, Double.POSITIVE_INFINITY);
        }

        int endExclusive = Math.min(candidateMovements.size(), Math.max(0, movementEndExclusive));
        List<BetterBlockPos> positions = candidatePath.positions();
        Vec3 playerPos = ctx.player().position();

        for (int i = Math.max(0, movementStartIndex); i < endExclusive; i++) {
            IMovement movement = candidateMovements.get(i);
            if (movement instanceof Movement && ((Movement) movement).getValidPositions().contains(ctx.playerFeet())) {
                return new PathProgressMatch(exactPositionIndex, i, 0.0D);
            }
            if (i + 1 < positions.size()) {
                double distance = distanceToPathSegment(positions.get(i), positions.get(i + 1), playerPos);
                if (distance <= leniency) {
                    return new PathProgressMatch(exactPositionIndex, i, distance);
                }
            }
        }
        return new PathProgressMatch(exactPositionIndex, -1, Double.POSITIVE_INFINITY);
    }

    private double distanceToPathSegment(BetterBlockPos start, BetterBlockPos end, Vec3 playerPos) {
        return playerPos.distanceTo(nearestPointOnSegment(playerPos, blockCenter(start), blockCenter(end)));
    }

    private static final class PathProgressMatch {

        private final int exactPositionIndex;
        private final int movementIndex;
        private final double distance;

        private PathProgressMatch(int exactPositionIndex, int movementIndex, double distance) {
            this.exactPositionIndex = exactPositionIndex;
            this.movementIndex = movementIndex;
            this.distance = distance;
        }

        private boolean hasExactPositionMatch() {
            return exactPositionIndex != -1;
        }

        private int getExactPositionIndex() {
            return exactPositionIndex;
        }

        private boolean hasMovementMatch() {
            return movementIndex != -1 && distance < Double.POSITIVE_INFINITY;
        }

        private int getMovementIndex() {
            return movementIndex;
        }

        private double getDistance() {
            return distance;
        }
    }

    private static final class AdvanceResolution {
        private static final AdvanceResolution NONE = new AdvanceResolution(-1, -1, -1, Double.POSITIVE_INFINITY);

        private final int index;
        private final int exactIndex;
        private final int movementIndex;
        private final double distance;

        private AdvanceResolution(int index, int exactIndex, int movementIndex, double distance) {
            this.index = index;
            this.exactIndex = exactIndex;
            this.movementIndex = movementIndex;
            this.distance = distance;
        }

        private boolean hasAdvance() {
            return index >= 0;
        }

        private static AdvanceResolution none() {
            return NONE;
        }

        private static AdvanceResolution exact(int index) {
            return new AdvanceResolution(index, index, -1, 0.0D);
        }

        private static AdvanceResolution route(int index, int exactIndex, double distance) {
            return new AdvanceResolution(index, exactIndex, index, distance);
        }

        private boolean isRouteMatch() {
            return movementIndex >= 0;
        }
    }

    private boolean shouldSprintNextTick() {
        boolean requested = behavior.baritone.getInputOverrideHandler().isInputForcedDown(Input.SPRINT);

        // we'll take it from here, no need for minecraft to see we're holding down control and sprint for us
        behavior.baritone.getInputOverrideHandler().setInputForceState(Input.SPRINT, false);

        // first and foremost, if allowSprint is off, or if we don't have enough hunger, don't try and sprint
        if (!new CalculationContext(behavior.baritone, false).canSprint) {
            return false;
        }
        IMovement current = path.movements().get(pathPosition);

        // traverse requests sprinting, so we need to do this check first
        if (current instanceof MovementTraverse && pathPosition < path.length() - 3) {
            IMovement next = path.movements().get(pathPosition + 1);
            if (next instanceof MovementAscend && sprintableAscend(ctx, (MovementTraverse) current, (MovementAscend) next, path.movements().get(pathPosition + 2))) {
                if (skipNow(ctx, current)) {
                    logDebug("Skipping traverse to straight ascend");
                    pathPosition++;
                    onChangeInPathPosition();
                    this.restartRequested = true;
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    return true;
                } else {
                    logDebug("Too far to the side to safely sprint ascend");
                }
            }
        }

        // if the movement requested sprinting, then we're done
        if (requested) {
            return true;
        }

        // however, descend and ascend don't request sprinting, because they don't know the context of what movement comes after it
        if (current instanceof MovementDescend) {

            if (pathPosition < path.length() - 2) {
                // keep this out of onTick, even if that means a tick of delay before it has an effect
                IMovement next = path.movements().get(pathPosition + 1);
                if (MovementHelper.canUseFrostWalker(ctx, next.getDest().below())) {
                    // frostwalker only works if you cross the edge of the block on ground so in some cases we may not overshoot
                    // Since MovementDescend can't know the next movement we have to tell it
                    if (next instanceof MovementTraverse || next instanceof MovementParkour) {
                        boolean couldPlaceInstead = Baritone.settings().allowPlace.value && behavior.baritone.getInventoryBehavior().hasGenericThrowaway() && next instanceof MovementParkour; // traverse doesn't react fast enough
                        // this is true if the next movement does not ascend or descends and goes into the same cardinal direction (N-NE-E-SE-S-SW-W-NW) as the descend
                        // in that case current.getDirection() is e.g. (0, -1, 1) and next.getDirection() is e.g. (0, 0, 3) so the cross product of (0, 0, 1) and (0, 0, 3) is taken, which is (0, 0, 0) because the vectors are colinear (don't form a plane)
                        // since movements in exactly the opposite direction (e.g. descend (0, -1, 1) and traverse (0, 0, -1)) would also pass this check we also have to rule out that case
                        // we can do that by adding the directions because traverse is always 1 long like descend and parkour can't jump through current.getSrc().down()
                        boolean sameFlatDirection = !current.getDirection().above().offset(next.getDirection()).equals(BlockPos.ZERO)
                                && current.getDirection().above().cross(next.getDirection()).equals(BlockPos.ZERO); // here's why you learn maths in school
                        if (sameFlatDirection && !couldPlaceInstead) {
                            ((MovementDescend) current).forceSafeMode();
                        }
                    }
                }
            }
            if (((MovementDescend) current).safeMode() && !((MovementDescend) current).skipToAscend()) {
                logDebug("Sprinting would be unsafe");
                return false;
            }

            if (pathPosition < path.length() - 2) {
                IMovement next = path.movements().get(pathPosition + 1);
                if (next instanceof MovementAscend && current.getDirection().above().equals(next.getDirection().below())) {
                    // a descend then an ascend in the same direction
                    pathPosition++;
                    onChangeInPathPosition();
                    this.restartRequested = true;
                    // okay to skip clearKeys and / or onChangeInPathPosition here since this isn't possible to repeat, since it's asymmetric
                    logDebug("Skipping descend to straight ascend");
                    return true;
                }
                if (canSprintFromDescendInto(ctx, current, next)) {

                    if (next instanceof MovementDescend && pathPosition < path.length() - 3) {
                        IMovement next_next = path.movements().get(pathPosition + 2);
                        if (next_next instanceof MovementDescend && !canSprintFromDescendInto(ctx, next, next_next)) {
                            return false;
                        }

                    }
                    if (ctx.playerFeet().equals(current.getDest())) {
                        pathPosition++;
                        onChangeInPathPosition();
                        this.restartRequested = true;
                    }

                    return true;
                }
                //logDebug("Turning off sprinting " + movement + " " + next + " " + movement.getDirection() + " " + next.getDirection().down() + " " + next.getDirection().down().equals(movement.getDirection()));
            }
        }
        if (current instanceof MovementAscend && pathPosition != 0) {
            IMovement prev = path.movements().get(pathPosition - 1);
            if (prev instanceof MovementDescend && prev.getDirection().above().equals(current.getDirection().below())) {
                BlockPos center = current.getSrc().above();
                // playerFeet adds 0.1251 to account for soul sand
                // farmland is 0.9375
                // 0.07 is to account for farmland
                if (ctx.player().position().y >= center.getY() - 0.07) {
                    behavior.baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                    return true;
                }
            }
            if (pathPosition < path.length() - 2 && prev instanceof MovementTraverse && sprintableAscend(ctx, (MovementTraverse) prev, (MovementAscend) current, path.movements().get(pathPosition + 1))) {
                return true;
            }
        }
        if (current instanceof MovementFall) {
            Tuple<Vec3, BlockPos> data = overrideFall((MovementFall) current);
            if (data != null) {
                BetterBlockPos fallDest = new BetterBlockPos(data.getB());
                if (!path.positions().contains(fallDest)) {
                    throw new IllegalStateException(String.format(
                            "Fall override at %s %s %s returned illegal destination %s %s %s",
                            current.getSrc(), fallDest));
                }
                if (ctx.playerFeet().equals(fallDest)) {
                    pathPosition = path.positions().indexOf(fallDest);
                    onChangeInPathPosition();
                    this.restartRequested = true;
                    return true;
                }
                clearKeys();
                behavior.baritone.getLookBehavior().updateTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), data.getA(), ctx.playerRotations()), false);
                behavior.baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                return true;
            }
        }
        return false;
    }

    private Tuple<Vec3, BlockPos> overrideFall(MovementFall movement) {
        Vec3i dir = movement.getDirection();
        if (dir.getY() < -3) {
            return null;
        }
        if (!movement.toBreakCached.isEmpty()) {
            return null; // it's breaking
        }
        Vec3i flatDir = new Vec3i(dir.getX(), 0, dir.getZ());
        int i;
        outer:
        for (i = pathPosition + 1; i < path.length() - 1 && i < pathPosition + 3; i++) {
            IMovement next = path.movements().get(i);
            if (!(next instanceof MovementTraverse)) {
                break;
            }
            if (!flatDir.equals(next.getDirection())) {
                break;
            }
            for (int y = next.getDest().y; y <= movement.getSrc().y + 1; y++) {
                BlockPos chk = new BlockPos(next.getDest().x, y, next.getDest().z);
                if (!MovementHelper.fullyPassable(ctx, chk)) {
                    break outer;
                }
            }
            if (!MovementHelper.canWalkOn(ctx, next.getDest().below())) {
                break;
            }
        }
        i--;
        if (i == pathPosition) {
            return null; // no valid extension exists
        }
        double len = i - pathPosition - 0.4;
        return new Tuple<>(
                new Vec3(flatDir.getX() * len + movement.getDest().x + 0.5, movement.getDest().y, flatDir.getZ() * len + movement.getDest().z + 0.5),
                movement.getDest().offset(flatDir.getX() * (i - pathPosition), 0, flatDir.getZ() * (i - pathPosition)));
    }

    private static boolean skipNow(IPlayerContext ctx, IMovement current) {
        double offTarget = Math.abs(current.getDirection().getX() * (current.getSrc().z + 0.5D - ctx.player().position().z)) + Math.abs(current.getDirection().getZ() * (current.getSrc().x + 0.5D - ctx.player().position().x));
        if (offTarget > 0.1) {
            return false;
        }
        // we are centered
        BlockPos headBonk = current.getSrc().subtract(current.getDirection()).above(2);
        if (MovementHelper.fullyPassable(ctx, headBonk)) {
            return true;
        }
        // wait 0.3
        double flatDist = Math.abs(current.getDirection().getX() * (headBonk.getX() + 0.5D - ctx.player().position().x)) + Math.abs(current.getDirection().getZ() * (headBonk.getZ() + 0.5 - ctx.player().position().z));
        return flatDist > 0.8;
    }

    private static boolean sprintableAscend(IPlayerContext ctx, MovementTraverse current, MovementAscend next, IMovement nextnext) {
        if (!Baritone.settings().sprintAscends.value) {
            return false;
        }
        if (!current.getDirection().equals(next.getDirection().below())) {
            return false;
        }
        if (nextnext.getDirection().getX() != next.getDirection().getX() || nextnext.getDirection().getZ() != next.getDirection().getZ()) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx, current.getDest().below())) {
            return false;
        }
        if (!MovementHelper.canWalkOn(ctx, next.getDest().below())) {
            return false;
        }
        if (!next.toBreakCached.isEmpty()) {
            return false; // it's breaking
        }
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                BlockPos chk = current.getSrc().above(y);
                if (x == 1) {
                    chk = chk.offset(current.getDirection());
                }
                if (!MovementHelper.fullyPassable(ctx, chk)) {
                    return false;
                }
            }
        }
        if (MovementHelper.avoidWalkingInto(ctx.world().getBlockState(current.getSrc().above(3)))) {
            return false;
        }
        return !MovementHelper.avoidWalkingInto(ctx.world().getBlockState(next.getDest().above(2))); // codacy smh my head
    }

    private static boolean canSprintFromDescendInto(IPlayerContext ctx, IMovement current, IMovement next) {
        if (next instanceof MovementDescend && next.getDirection().equals(current.getDirection())) {
            return true;
        }
        if (!MovementHelper.canWalkOn(ctx, current.getDest().offset(current.getDirection()))) {
            return false;
        }
        if (next instanceof MovementTraverse && next.getDirection().equals(current.getDirection())) {
            return true;
        }
        return next instanceof MovementDiagonal && Baritone.settings().allowOvershootDiagonalDescend.value;
    }

    private void onChangeInPathPosition() {
        clearKeys();
        ticksOnCurrent = 0;
    }

    private void clearKeys() {
        // i'm just sick and tired of this snippet being everywhere lol
        behavior.baritone.getInputOverrideHandler().clearAllKeys();
    }

    private void cancel() {
        clearKeys();
        behavior.baritone.getInputOverrideHandler().getBlockBreakHelper().stopBreakingBlock();
        pathPosition = path.length() + 3;
        failed = true;
    }

    @Override
    public int getPosition() {
        return pathPosition;
    }

    public PathExecutor trySplice(PathExecutor next) {
        if (next == null) {
            return cutIfTooLong();
        }
        return SplicedPath.trySplice(path, next.path, false).map(path -> {
            if (!path.getDest().equals(next.getPath().getDest())) {
                throw new IllegalStateException(String.format(
                        "Path has end %s instead of %s after splicing",
                        path.getDest(), next.getPath().getDest()));
            }
            PathExecutor ret = new PathExecutor(behavior, path);
            ret.pathPosition = pathPosition;
            ret.currentMovementOriginalCostEstimate = currentMovementOriginalCostEstimate;
            ret.costEstimateIndex = costEstimateIndex;
            ret.ticksOnCurrent = ticksOnCurrent;
            return ret;
        }).orElseGet(this::cutIfTooLong); // dont actually call cutIfTooLong every tick if we won't actually use it, use a method reference
    }

    private PathExecutor cutIfTooLong() {
        if (pathPosition > Baritone.settings().maxPathHistoryLength.value) {
            int cutoffAmt = Baritone.settings().pathHistoryCutoffAmount.value;
            CutoffPath newPath = new CutoffPath(path, cutoffAmt, path.length() - 1);
            if (!newPath.getDest().equals(path.getDest())) {
                throw new IllegalStateException(String.format(
                        "Path has end %s instead of %s after trimming its start",
                        newPath.getDest(), path.getDest()));
            }
            logDebug("Discarding earliest segment movements, length cut from " + path.length() + " to " + newPath.length());
            PathExecutor ret = new PathExecutor(behavior, newPath);
            ret.pathPosition = pathPosition - cutoffAmt;
            ret.currentMovementOriginalCostEstimate = currentMovementOriginalCostEstimate;
            if (costEstimateIndex != null) {
                ret.costEstimateIndex = costEstimateIndex - cutoffAmt;
            }
            ret.ticksOnCurrent = ticksOnCurrent;
            return ret;
        }
        return this;
    }

    @Override
    public IPath getPath() {
        return path;
    }

    public boolean failed() {
        return failed;
    }

    public boolean finished() {
        return pathPosition >= path.length();
    }

    public Set<BlockPos> toBreak() {
        return Collections.unmodifiableSet(toBreak);
    }

    public Set<BlockPos> toPlace() {
        return Collections.unmodifiableSet(toPlace);
    }

    public Set<BlockPos> toWalkInto() {
        return Collections.unmodifiableSet(toWalkInto);
    }

    public boolean isSprinting() {
        return sprintNextTick;
    }
}

