package com.zszl.zszlScriptMod.handlers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HuntOrbitController {

    private static final double STUCK_MOVEMENT_EPSILON_SQ = 0.0036D;
    private static final int STUCK_TICKS_THRESHOLD = 6;
    private static final int RECOVERY_TICKS = 8;
    private static final int PREVIEW_SEGMENTS = 32;

    private final Minecraft mc = Minecraft.getInstance();
    private final Map<KeyMapping, Boolean> heldKeyStates = new LinkedHashMap<>();

    private int activeTargetEntityId = Integer.MIN_VALUE;
    private boolean counterClockwise = true;
    private boolean active = false;
    private double lastPlayerX = Double.NaN;
    private double lastPlayerZ = Double.NaN;
    private int stuckTicks = 0;
    private int recoveryTicksRemaining = 0;

    public static final class OrbitConfig {
        private final double desiredDistance;
        private final double distanceTolerance;
        private final boolean holdJump;
        private final boolean holdSprint;
        private final boolean counterClockwise;

        public OrbitConfig(double desiredDistance, double distanceTolerance, boolean holdJump,
                boolean holdSprint, boolean counterClockwise) {
            this.desiredDistance = desiredDistance;
            this.distanceTolerance = distanceTolerance;
            this.holdJump = holdJump;
            this.holdSprint = holdSprint;
            this.counterClockwise = counterClockwise;
        }
    }

    public void tick(LocalPlayer player, LivingEntity target, OrbitConfig config) {
        if (player == null || target == null || config == null) {
            stop();
            return;
        }

        int targetEntityId = target.getId();
        if (!this.active || targetEntityId != this.activeTargetEntityId) {
            releaseHeldKeys();
            resetMovementTracking();
            this.activeTargetEntityId = targetEntityId;
            this.counterClockwise = config.counterClockwise;
        }

        this.active = true;
        updateStuckState(player);
        applyMovement(player, target, config);
    }

    public boolean isActive() {
        return this.active;
    }

    public void stop() {
        releaseHeldKeys();
        this.active = false;
        this.activeTargetEntityId = Integer.MIN_VALUE;
        resetMovementTracking();
    }

    public static List<Vec3> buildPreviewLoop(LivingEntity target, double radius) {
        return buildPreviewLoop(target, radius, PREVIEW_SEGMENTS);
    }

    public static List<Vec3> buildPreviewLoop(LivingEntity target, double radius, int samplePoints) {
        List<Vec3> points = new ArrayList<>();
        if (target == null || radius <= 0.0D) {
            return points;
        }

        int segments = Math.max(3, samplePoints);
        double centerY = target.getY() + 0.08D;
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0D * i) / segments;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            points.add(new Vec3(x, centerY, z));
        }
        return points;
    }

    private void applyMovement(LocalPlayer player, LivingEntity target, OrbitConfig config) {
        if (mc.options == null) {
            stop();
            return;
        }

        double desiredDistance = Math.max(0.5D, config.desiredDistance);
        double tolerance = Math.max(0.15D, config.distanceTolerance);
        double distance = player.distanceTo(target);
        boolean moveForward = false;
        boolean moveBackward = false;

        double farBand = desiredDistance + tolerance;
        double closeBand = Math.max(0.45D, desiredDistance - tolerance);
        if (distance > farBand) {
            moveForward = true;
        } else if (distance < closeBand) {
            moveBackward = true;
        }

        if (distance > desiredDistance + Math.max(1.10D, tolerance * 2.0D)) {
            moveForward = true;
            moveBackward = false;
        }
        if (distance < Math.max(0.45D, desiredDistance - Math.max(0.75D, tolerance * 2.2D))) {
            moveBackward = true;
            moveForward = false;
        }

        if (this.recoveryTicksRemaining > 0) {
            this.recoveryTicksRemaining--;
            if (!moveBackward) {
                moveForward = true;
            }
        }

        boolean shouldHoldSprint = config.holdSprint && !moveBackward && canPrimeSprint(player);

        setKeyState(mc.options.keyLeft, this.counterClockwise);
        setKeyState(mc.options.keyRight, !this.counterClockwise);
        setKeyState(mc.options.keyUp, moveForward);
        setKeyState(mc.options.keyDown, moveBackward);
        setKeyState(mc.options.keyJump, config.holdJump);
        setKeyState(mc.options.keySprint, shouldHoldSprint);
        if (shouldHoldSprint) {
            player.setSprinting(true);
        }
    }

    private void updateStuckState(LocalPlayer player) {
        if (player == null) {
            resetMovementTracking();
            return;
        }

        if (Double.isNaN(this.lastPlayerX) || Double.isNaN(this.lastPlayerZ)) {
            this.lastPlayerX = player.getX();
            this.lastPlayerZ = player.getZ();
            this.stuckTicks = 0;
            return;
        }

        double dx = player.getX() - this.lastPlayerX;
        double dz = player.getZ() - this.lastPlayerZ;
        if (dx * dx + dz * dz <= STUCK_MOVEMENT_EPSILON_SQ) {
            this.stuckTicks++;
            if (this.stuckTicks >= STUCK_TICKS_THRESHOLD) {
                this.recoveryTicksRemaining = Math.max(this.recoveryTicksRemaining, RECOVERY_TICKS);
                this.stuckTicks = 0;
            }
        } else {
            this.stuckTicks = 0;
        }

        this.lastPlayerX = player.getX();
        this.lastPlayerZ = player.getZ();
    }

    private void resetMovementTracking() {
        this.lastPlayerX = Double.NaN;
        this.lastPlayerZ = Double.NaN;
        this.stuckTicks = 0;
        this.recoveryTicksRemaining = 0;
    }

    private boolean canPrimeSprint(LocalPlayer player) {
        return player != null
                && player.getFoodData().getFoodLevel() > 6
                && !player.isShiftKeyDown()
                && !player.horizontalCollision
                && !player.isUsingItem();
    }

    private void setKeyState(KeyMapping keyBinding, boolean pressed) {
        if (keyBinding == null) {
            return;
        }
        InputConstants.Key key = keyBinding.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return;
        }

        if (pressed) {
            this.heldKeyStates.put(keyBinding, Boolean.TRUE);
        } else {
            this.heldKeyStates.remove(keyBinding);
        }
        KeyMapping.set(key, pressed || isPhysicalKeyDown(keyBinding));
    }

    private void releaseHeldKeys() {
        if (this.heldKeyStates.isEmpty()) {
            return;
        }

        for (KeyMapping keyBinding : new ArrayList<>(this.heldKeyStates.keySet())) {
            if (keyBinding == null) {
                continue;
            }
            InputConstants.Key key = keyBinding.getKey();
            if (key == null || key.equals(InputConstants.UNKNOWN)) {
                continue;
            }
            KeyMapping.set(key, isPhysicalKeyDown(keyBinding));
        }
        this.heldKeyStates.clear();
    }

    private boolean isPhysicalKeyDown(KeyMapping keyBinding) {
        if (keyBinding == null || mc == null || mc.getWindow() == null) {
            return false;
        }
        InputConstants.Key key = keyBinding.getKey();
        if (key == null || key.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        if (key.getType() != InputConstants.Type.KEYSYM) {
            return false;
        }
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
    }
}
