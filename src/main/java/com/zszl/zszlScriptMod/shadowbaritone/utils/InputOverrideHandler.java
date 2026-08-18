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

package com.zszl.zszlScriptMod.shadowbaritone.utils;

import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.baritone.compat.HumanLikeMovementController;
import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import com.zszl.zszlScriptMod.shadowbaritone.Baritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.TickEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.WorldEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.calc.IPath;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BetterBlockPos;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.IInputOverrideHandler;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.interfaces.IGoalRenderPos;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.input.Input;
import com.zszl.zszlScriptMod.shadowbaritone.behavior.Behavior;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.MovementHelper;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.path.PathExecutor;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface with the game's control system allowing the ability to
 * force down certain controls, having the same effect as if we were actually
 * physically forcing down the assigned key.
 *
 * @author Brady
 * @since 7/31/2018
 */
public final class InputOverrideHandler extends Behavior implements IInputOverrideHandler {
    private static final float COMPATIBILITY_KEY_THRESHOLD = 0.3F;

    /**
     * Maps inputs to whether or not we are forcing their state down.
     */
    private final Map<Input, Boolean> inputForceStateMap = new HashMap<>();
    private final Map<Integer, Boolean> compatibilityMovementStateMap = new HashMap<>();

    private final BlockBreakHelper blockBreakHelper;
    private final BlockPlaceHelper blockPlaceHelper;

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
        this.blockBreakHelper = new BlockBreakHelper(baritone.getPlayerContext());
        this.blockPlaceHelper = new BlockPlaceHelper(baritone.getPlayerContext());
    }

    /**
     * Returns whether or not we are forcing down the specified {@link Input}.
     *
     * @param input The input
     * @return Whether or not it is being forced down
     */
    @Override
    public final boolean isInputForcedDown(Input input) {
        return input == null ? false : this.inputForceStateMap.getOrDefault(input, false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    @Override
    public final void setInputForceState(Input input, boolean forced) {
        this.inputForceStateMap.put(input, forced);
    }

    /**
     * Clears the override state for all keys
     */
    @Override
    public final void clearAllKeys() {
        this.inputForceStateMap.clear();
    }

    @Override
    public final void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            return;
        }
        if (ctx.minecraft() == null || ctx.minecraft().options == null) {
            return;
        }
        if (ctx.player() == null) {
            HumanLikeMovementController.INSTANCE.reset();
            releaseCompatibilityMovementKeys();
            return;
        }
        if (isInputForcedDown(Input.CLICK_LEFT)) {
            setInputForceState(Input.CLICK_RIGHT, false);
        }
        blockBreakHelper.tick(isInputForcedDown(Input.CLICK_LEFT));
        blockPlaceHelper.tick(isInputForcedDown(Input.CLICK_RIGHT));

        if (shouldUseCompatibilityWalkMode()) {
            if (inControl()) {
                ensureKeyboardInput(true);
                syncCompatibilityMovementKeys();
            } else {
                HumanLikeMovementController.INSTANCE.reset();
                releaseCompatibilityMovementKeys();
                ensureKeyboardInput(false);
            }
        } else {
            releaseCompatibilityMovementKeys();
            if (inControl()) {
                if (ctx.player().input.getClass() != PlayerMovementInput.class) {
                    ctx.player().input = new PlayerMovementInput(this);
                }
            } else {
                if (ctx.player().input.getClass() == PlayerMovementInput.class) { // allow other movement inputs that aren't this one, e.g. for a freecam
                    ctx.player().input = new KeyboardInput(ctx.minecraft().options);
                }
            }
        }
        if (!inControl()) {
            HumanLikeMovementController.INSTANCE.reset();
        }
        // only set it if it was previously incorrect
        // gotta do it this way, or else it constantly thinks you're beginning a double tap W sprint lol
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        HumanLikeMovementController.INSTANCE.reset();
        releaseCompatibilityMovementKeys();
    }

    private boolean inControl() {
        for (Input input : new Input[]{Input.MOVE_FORWARD, Input.MOVE_BACK, Input.MOVE_LEFT, Input.MOVE_RIGHT, Input.SNEAK, Input.JUMP}) {
            if (isInputForcedDown(input)) {
                return true;
            }
        }
        // if we are not primary (a bot) we should set the movementinput even when idle (not pathing)
        return baritone.getPathingBehavior().isPathing() || baritone != BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    public BlockBreakHelper getBlockBreakHelper() {
        return blockBreakHelper;
    }

    float getPlayerYaw() {
        return ctx.player().getYRot();
    }

    float getMovementYaw() {
        return baritone.getLookBehavior()
                .getEffectiveRotation()
                .map(rotation -> rotation.getYaw())
                .orElseGet(this::getPlayerYaw);
    }

    private boolean shouldUseCompatibilityWalkMode() {
        return Baritone.settings().compatibilityWalkMode.value;
    }

    private void ensureKeyboardInput(boolean forceReplaceCustomInput) {
        if (ctx.player().input.getClass() == KeyboardInput.class) {
            return;
        }
        if (!forceReplaceCustomInput && ctx.player().input.getClass() != PlayerMovementInput.class) {
            return;
        }
        if (ctx.minecraft() != null && ctx.minecraft().options != null) {
            ctx.player().input = new KeyboardInput(ctx.minecraft().options);
        }
    }

    private void syncCompatibilityMovementKeys() {
        if (ctx.minecraft() == null || ctx.minecraft().options == null) {
            return;
        }
        HumanLikeMovementController.MovementState movementState = getMovementState();
        setCompatibilityMovementKey(ctx.minecraft().options.keyUp,
                movementState.moveForward > COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyDown,
                movementState.moveForward < -COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyLeft,
                movementState.moveStrafe > COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyRight,
                movementState.moveStrafe < -COMPATIBILITY_KEY_THRESHOLD);
        setCompatibilityMovementKey(ctx.minecraft().options.keyJump, movementState.jump);
        setCompatibilityMovementKey(ctx.minecraft().options.keyShift, movementState.sneak);
    }

    private void releaseCompatibilityMovementKeys() {
        if (ctx.minecraft() == null || ctx.minecraft().options == null) {
            return;
        }
        setCompatibilityMovementKey(ctx.minecraft().options.keyUp, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyDown, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyLeft, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyRight, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyJump, false);
        setCompatibilityMovementKey(ctx.minecraft().options.keyShift, false);
    }

    private void setCompatibilityMovementKey(KeyMapping keyMapping, boolean down) {
        if (keyMapping == null) {
            return;
        }
        InputConstants.Key key = keyMapping.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN) || key.getType() != InputConstants.Type.KEYSYM) {
            return;
        }
        int legacyKeyCode = Keyboard.fromGlfwKey(key.getValue());
        if (legacyKeyCode == Keyboard.KEY_NONE) {
            return;
        }
        Boolean previous = compatibilityMovementStateMap.get(legacyKeyCode);
        if (previous != null && previous == down) {
            return;
        }
        compatibilityMovementStateMap.put(legacyKeyCode, down);
        SimulatedKeyInputManager.simulateKeyCode(legacyKeyCode, down ? "Down" : "Up");
    }

    HumanLikeMovementController.MovementState getMovementState() {
        float desiredForward = 0.0F;
        float desiredStrafe = 0.0F;

        boolean desiredJump = isInputForcedDown(Input.JUMP);
        boolean desiredSneak = isInputForcedDown(Input.SNEAK);

        if (isInputForcedDown(Input.MOVE_FORWARD)) {
            desiredForward += 1.0F;
        }
        if (isInputForcedDown(Input.MOVE_BACK)) {
            desiredForward -= 1.0F;
        }
        if (isInputForcedDown(Input.MOVE_LEFT)) {
            desiredStrafe += 1.0F;
        }
        if (isInputForcedDown(Input.MOVE_RIGHT)) {
            desiredStrafe -= 1.0F;
        }

        float yawDifferenceDeg = 0.0F;
        boolean decoupleMovementFromVisualYaw = handlerUsesDecoupledMovementYaw();
        if (decoupleMovementFromVisualYaw && (desiredForward != 0.0F || desiredStrafe != 0.0F)) {
            yawDifferenceDeg = Mth.wrapDegrees(getMovementYaw() - getPlayerYaw());
            float yawDelta = (float) Math.toRadians(yawDifferenceDeg);
            float sin = Mth.sin(yawDelta);
            float cos = Mth.cos(yawDelta);
            float rawStrafe = desiredStrafe;
            float rawForward = desiredForward;

            desiredStrafe = rawStrafe * cos - rawForward * sin;
            desiredForward = rawStrafe * sin + rawForward * cos;
        }

        if (!shouldApplyHumanLikeMovement()) {
            return new HumanLikeMovementController.MovementState(desiredForward, desiredStrafe, desiredJump,
                    desiredSneak);
        }

        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        if (config == null) {
            return new HumanLikeMovementController.MovementState(desiredForward, desiredStrafe, desiredJump,
                    desiredSneak);
        }

        float straightPathFactor = computeStraightPathFactor();
        if (desiredForward < 0.0F || Math.abs(desiredStrafe) > 0.12F || desiredJump) {
            straightPathFactor *= 0.45F;
        }
        PassageMetrics passageMetrics = computePassageMetrics();
        float finalApproachProgress = computeFinalApproachProgress(config);

        return HumanLikeMovementController.INSTANCE.applyMovement(
                desiredForward,
                desiredStrafe,
                desiredJump,
                desiredSneak,
                yawDifferenceDeg,
                ctx.player().getX(),
                ctx.player().getY(),
                ctx.player().getZ(),
                ctx.player().onGround(),
                finalApproachProgress,
                passageMetrics.narrowPassageFactor,
                straightPathFactor,
                passageMetrics.obstacleEdgeBias);
    }

    private boolean handlerUsesDecoupledMovementYaw() {
        return baritone.getLookBehavior().shouldDecoupleMovementFromVisualYaw();
    }

    private boolean shouldApplyHumanLikeMovement() {
        return ctx.player() != null
                && HumanLikeMovementController.INSTANCE.isEnabled()
                && baritone.getPathingBehavior().isPathing();
    }

    private float computeFinalApproachProgress(HumanLikeMovementConfig config) {
        if (ctx.player() == null || config.finalApproachDistance <= 0.0F) {
            return 0.0F;
        }
        BlockPos goalPos = resolveGoalPos();
        if (goalPos == null) {
            return 0.0F;
        }
        double dx = ctx.player().getX() - (goalPos.getX() + 0.5D);
        double dy = ctx.player().getY() - goalPos.getY();
        double dz = ctx.player().getZ() - (goalPos.getZ() + 0.5D);
        double distance = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.35D);
        return Mth.clamp((float) (1.0D - distance / config.finalApproachDistance), 0.0F, 1.0F);
    }

    private BlockPos resolveGoalPos() {
        Goal goal = baritone.getPathingBehavior().getGoal();
        if (goal instanceof IGoalRenderPos) {
            return ((IGoalRenderPos) goal).getGoalPos();
        }
        PathExecutor current = baritone.getPathingBehavior().getCurrent();
        if (current != null) {
            return current.getPath().getDest();
        }
        return null;
    }

    private float computeStraightPathFactor() {
        PathExecutor executor = baritone.getPathingBehavior().getCurrent();
        if (executor == null) {
            return 0.0F;
        }
        IPath path = executor.getPath();
        if (path == null || path.positions().size() < 2) {
            return 0.0F;
        }
        int currentIndex = Mth.clamp(executor.getPosition(), 0, Math.max(0, path.positions().size() - 2));
        BetterBlockPos baseStart = path.positions().get(currentIndex);
        BetterBlockPos baseEnd = path.positions().get(currentIndex + 1);
        double baseDx = baseEnd.x - baseStart.x;
        double baseDz = baseEnd.z - baseStart.z;
        double baseLen = Math.sqrt(baseDx * baseDx + baseDz * baseDz);
        if (baseLen <= 1.0E-6D) {
            return 0.0F;
        }

        int lookahead = Math.min(4, path.positions().size() - 1 - currentIndex);
        float weightedScore = 0.0F;
        float totalWeight = 0.0F;
        for (int step = 1; step <= lookahead; step++) {
            BetterBlockPos from = path.positions().get(currentIndex + step - 1);
            BetterBlockPos to = path.positions().get(currentIndex + step);
            double dx = to.x - from.x;
            double dz = to.z - from.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len <= 1.0E-6D) {
                continue;
            }
            float alignment = (float) ((dx * baseDx + dz * baseDz) / (len * baseLen));
            alignment = Mth.clamp(alignment, 0.0F, 1.0F);
            if (to.y != from.y) {
                alignment *= 0.7F;
            }
            float weight = 1.0F / step;
            weightedScore += alignment * weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0.0F) {
            return 0.0F;
        }
        return Mth.clamp(weightedScore / totalWeight, 0.0F, 1.0F);
    }

    private PassageMetrics computePassageMetrics() {
        float[] travelDirection = getTravelDirection();
        if (travelDirection == null) {
            return PassageMetrics.EMPTY;
        }
        float leftX = -travelDirection[1];
        float leftZ = travelDirection[0];
        float rightX = travelDirection[1];
        float rightZ = -travelDirection[0];

        double leftOpenness = sampleSideOpenness(leftX, leftZ);
        double rightOpenness = sampleSideOpenness(rightX, rightZ);

        float narrowFactor = 1.0F - Mth.clamp((float) Math.min(leftOpenness, rightOpenness), 0.0F, 1.0F);
        BetterBlockPos feet = ctx.playerFeet();
        if (!MovementHelper.canWalkThrough(ctx, feet.above()) || !MovementHelper.canWalkThrough(ctx, feet.above(2))) {
            narrowFactor = Math.max(narrowFactor, 0.7F);
        }

        float obstacleEdgeBias = Mth.clamp((float) (rightOpenness - leftOpenness), -1.0F, 1.0F);
        return new PassageMetrics(narrowFactor, obstacleEdgeBias);
    }

    private float[] getTravelDirection() {
        PathExecutor executor = baritone.getPathingBehavior().getCurrent();
        if (executor != null) {
            IPath path = executor.getPath();
            if (path != null && path.positions().size() >= 2) {
                int currentIndex = Mth.clamp(executor.getPosition(), 0, Math.max(0, path.positions().size() - 2));
                BetterBlockPos from = path.positions().get(currentIndex);
                BetterBlockPos to = path.positions().get(currentIndex + 1);
                double dx = to.x - from.x;
                double dz = to.z - from.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1.0E-6D) {
                    return new float[]{(float) (dx / len), (float) (dz / len)};
                }
            }
        }

        float yawRadians = getMovementYaw() * ((float) Math.PI / 180.0F);
        return new float[]{-Mth.sin(yawRadians), Mth.cos(yawRadians)};
    }

    private double sampleSideOpenness(float directionX, float directionZ) {
        BetterBlockPos feet = ctx.playerFeet();
        double openness = 0.0D;
        for (int distance = 1; distance <= 2; distance++) {
            float weight = distance == 1 ? 0.6F : 0.4F;
            double sampleX = ctx.player().getX() + directionX * distance;
            double sampleZ = ctx.player().getZ() + directionZ * distance;
            BetterBlockPos sample = new BetterBlockPos(BlockPos.containing(sampleX, feet.getY(), sampleZ));
            if (MovementHelper.canWalkThrough(ctx, sample) && MovementHelper.canWalkThrough(ctx, sample.above())) {
                openness += weight;
            }
        }
        return openness;
    }

    private static final class PassageMetrics {
        private static final PassageMetrics EMPTY = new PassageMetrics(0.0F, 0.0F);

        private final float narrowPassageFactor;
        private final float obstacleEdgeBias;

        private PassageMetrics(float narrowPassageFactor, float obstacleEdgeBias) {
            this.narrowPassageFactor = narrowPassageFactor;
            this.obstacleEdgeBias = obstacleEdgeBias;
        }
    }
}

