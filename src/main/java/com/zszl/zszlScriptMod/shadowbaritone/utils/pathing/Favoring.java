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

package com.zszl.zszlScriptMod.shadowbaritone.utils.pathing;

import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.calc.IPath;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BetterBlockPos;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.Helper;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.IPlayerContext;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.interfaces.IGoalRenderPos;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.CalculationContext;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;

public final class Favoring {

    private final Long2DoubleOpenHashMap favorings;
    private final double routeNoiseStrength;
    private final int routeNoiseScale;
    private final long routeNoiseSeed;
    private final boolean useRouteNoise;
    private final boolean useRouteAnchor;
    private final double routeStartX;
    private final double routeStartZ;
    private final double routeGoalX;
    private final double routeGoalZ;
    private final double routeAnchorX;
    private final double routeAnchorZ;
    private final double routeAnchorInfluenceRadius;
    private final double routeAnchorStrength;

    public Favoring(IPlayerContext ctx, IPath previous, CalculationContext context, Goal goal) {
        favorings = new Long2DoubleOpenHashMap();
        favorings.defaultReturnValue(1.0D);
        double coeff = context.backtrackCostFavoringCoefficient;
        if (coeff != 1D && previous != null) {
            previous.positions().forEach(pos -> favorings.put(BetterBlockPos.longHash(pos), coeff));
        }
        for (Avoidance avoid : Avoidance.create(ctx)) {
            avoid.applySpherical(favorings);
        }
        BlockPos goalPos = goal instanceof IGoalRenderPos ? ((IGoalRenderPos) goal).getGoalPos() : null;
        HumanLikeMovementConfig humanLikeConfig = HumanLikeMovementConfig.INSTANCE;
        boolean humanLikeEnabled = humanLikeConfig != null && humanLikeConfig.enabled;
        this.routeNoiseStrength = humanLikeEnabled ? humanLikeConfig.routeNoiseStrength : 0.0D;
        this.routeNoiseScale = humanLikeEnabled ? Math.max(2, humanLikeConfig.routeNoiseScale) : 2;
        this.useRouteNoise = humanLikeEnabled && this.routeNoiseStrength > 0.0D;

        BetterBlockPos start = ctx.playerFeet();
        this.routeStartX = start == null ? 0.0D : start.x + 0.5D;
        this.routeStartZ = start == null ? 0.0D : start.z + 0.5D;
        this.routeGoalX = goalPos == null ? 0.0D : goalPos.getX() + 0.5D;
        this.routeGoalZ = goalPos == null ? 0.0D : goalPos.getZ() + 0.5D;

        long seed = mix64(Double.doubleToLongBits(routeStartX))
                ^ mix64(Double.doubleToLongBits(routeStartZ))
                ^ mix64(Double.doubleToLongBits(routeGoalX))
                ^ mix64(Double.doubleToLongBits(routeGoalZ))
                ^ mix64(this.routeNoiseScale);
        this.routeNoiseSeed = seed;

        double anchorRadius = humanLikeEnabled ? humanLikeConfig.routeAnchorRadius : 0.0D;
        double routeLength = goalPos == null ? 0.0D
                : Math.hypot(this.routeGoalX - this.routeStartX, this.routeGoalZ - this.routeStartZ);
        if (goalPos != null && anchorRadius > 0.0D && routeLength > 6.0D) {
            double midpointX = (this.routeStartX + this.routeGoalX) * 0.5D;
            double midpointZ = (this.routeStartZ + this.routeGoalZ) * 0.5D;
            double normalX = -(this.routeGoalZ - this.routeStartZ) / routeLength;
            double normalZ = (this.routeGoalX - this.routeStartX) / routeLength;
            double signedOffset = signedUnit(seed ^ 0x9E3779B97F4A7C15L) * anchorRadius;
            this.routeAnchorX = midpointX + normalX * signedOffset;
            this.routeAnchorZ = midpointZ + normalZ * signedOffset;
            this.routeAnchorInfluenceRadius = Math.max(1.5D, anchorRadius + 1.25D);
            this.routeAnchorStrength = 0.08D + this.routeNoiseStrength * 0.40D;
            this.useRouteAnchor = true;
        } else {
            this.routeAnchorX = 0.0D;
            this.routeAnchorZ = 0.0D;
            this.routeAnchorInfluenceRadius = 0.0D;
            this.routeAnchorStrength = 0.0D;
            this.useRouteAnchor = false;
        }
        Helper.HELPER.logDebug("Favoring size: " + favorings.size());
    }

    public Favoring(IPath previous, CalculationContext context) { // create one just from previous path, no mob avoidances
        favorings = new Long2DoubleOpenHashMap();
        favorings.defaultReturnValue(1.0D);
        this.routeNoiseStrength = 0.0D;
        this.routeNoiseScale = 2;
        this.routeNoiseSeed = 0L;
        this.useRouteNoise = false;
        this.useRouteAnchor = false;
        this.routeStartX = 0.0D;
        this.routeStartZ = 0.0D;
        this.routeGoalX = 0.0D;
        this.routeGoalZ = 0.0D;
        this.routeAnchorX = 0.0D;
        this.routeAnchorZ = 0.0D;
        this.routeAnchorInfluenceRadius = 0.0D;
        this.routeAnchorStrength = 0.0D;
        double coeff = context.backtrackCostFavoringCoefficient;
        if (coeff != 1D && previous != null) {
            previous.positions().forEach(pos -> favorings.put(BetterBlockPos.longHash(pos), coeff));
        }
    }

    public boolean isEmpty() {
        return favorings.isEmpty() && !useRouteNoise && !useRouteAnchor;
    }

    public double calculate(long hash, int x, int y, int z) {
        double result = favorings.get(hash);
        if (useRouteNoise) {
            result *= computeNoiseMultiplier(x, z);
        }
        if (useRouteAnchor) {
            result *= computeAnchorMultiplier(x, z);
        }
        return clamp(result, 0.65D, 1.45D);
    }

    private double computeNoiseMultiplier(int x, int z) {
        double scaledX = x / (double) routeNoiseScale;
        double scaledZ = z / (double) routeNoiseScale;
        int floorX = floor(scaledX);
        int floorZ = floor(scaledZ);
        double fracX = fade(scaledX - floorX);
        double fracZ = fade(scaledZ - floorZ);

        double n00 = latticeNoise(floorX, floorZ);
        double n10 = latticeNoise(floorX + 1, floorZ);
        double n01 = latticeNoise(floorX, floorZ + 1);
        double n11 = latticeNoise(floorX + 1, floorZ + 1);
        double nx0 = lerp(n00, n10, fracX);
        double nx1 = lerp(n01, n11, fracX);
        double noise = lerp(nx0, nx1, fracZ);
        double amplitude = routeNoiseStrength * 0.22D;
        return clamp(1.0D + noise * amplitude, 0.78D, 1.22D);
    }

    private double computeAnchorMultiplier(int x, int z) {
        double px = x + 0.5D;
        double pz = z + 0.5D;
        double distance = Math.min(
                distanceToSegment(px, pz, routeStartX, routeStartZ, routeAnchorX, routeAnchorZ),
                distanceToSegment(px, pz, routeAnchorX, routeAnchorZ, routeGoalX, routeGoalZ));
        double normalized = clamp(distance / routeAnchorInfluenceRadius, 0.0D, 1.75D);
        double reward = 1.0D - clamp(normalized, 0.0D, 1.0D);
        return clamp(1.0D - routeAnchorStrength * reward * reward, 0.72D, 1.0D);
    }

    private double latticeNoise(int x, int z) {
        long mixed = mix64(routeNoiseSeed ^ mix64(x * 0x9E3779B97F4A7C15L) ^ mix64(z * 0xC2B2AE3D27D4EB4FL));
        return ((mixed >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSq = dx * dx + dz * dz;
        if (lengthSq <= 1.0E-6D) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = ((px - ax) * dx + (pz - az) * dz) / lengthSq;
        t = clamp(t, 0.0D, 1.0D);
        double nearestX = ax + dx * t;
        double nearestZ = az + dz * t;
        return Math.hypot(px - nearestX, pz - nearestZ);
    }

    private double fade(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    private int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private double signedUnit(long value) {
        return ((value >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private long mix64(double value) {
        return mix64(Double.doubleToLongBits(value));
    }

    private long mix64(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

