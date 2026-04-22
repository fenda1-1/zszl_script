package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

final class LongJumpFeatureHandler {

    private static final int MIN_RELEASE_TICKS = 3;

    private LongJumpFeatureHandler() {
    }

    static void apply(MovementFeatureManager manager, LocalPlayer player) {
        boolean enabled = MovementFeatureManager.isEnabled("long_jump");
        boolean moving = MovementFeatureSupport.isMoving(player);
        boolean sneakDown = player != null && player.input != null && player.input.shiftKeyDown;

        if (!enabled || player == null || player.level() == null || player.getAbilities().flying
                || player.isInWater() || player.isInLava() || player.isPassenger()) {
            manager.longJumpChargeTicks = 0;
            manager.longJumpBoostTicks = 0;
            manager.longJumpBoostSpeed = 0.0D;
            manager.wasLongJumpSneakDown = sneakDown;
            return;
        }

        if (manager.longJumpBoostTicks > 0) {
            manager.longJumpBoostSpeed = Math.max(MovementFeatureSupport.getBaseMoveSpeed(),
                    manager.longJumpBoostSpeed * 0.94D);
            MovementFeatureSupport.ensureHorizontalSpeed(player, manager.longJumpBoostSpeed);
        }

        int fullChargeTicks = Math.max(4, Math.round(MovementFeatureManager.getConfiguredValue("long_jump", 1.20F) * 20.0F));
        boolean released = manager.wasLongJumpSneakDown && !sneakDown;

        if (player.onGround() && moving && sneakDown && manager.longJumpCooldownTicks <= 0) {
            manager.longJumpChargeTicks = Math.min(fullChargeTicks, manager.longJumpChargeTicks + 1);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.35D, motion.y, motion.z * 0.35D);
            player.setSprinting(false);
            player.hurtMarked = true;
        } else if (!sneakDown) {
            manager.longJumpChargeTicks = Math.max(0, manager.longJumpChargeTicks - 2);
        }

        if (released
                && player.onGround()
                && moving
                && manager.longJumpChargeTicks >= MIN_RELEASE_TICKS
                && manager.longJumpCooldownTicks <= 0) {
            double chargeRatio = Math.max(0.25D, Math.min(1.0D, manager.longJumpChargeTicks / (double) fullChargeTicks));
            double jumpBoost = 0.42D + chargeRatio * 0.28D;
            double launchSpeed = MovementFeatureSupport.getBaseMoveSpeed() * (1.55D + chargeRatio * 2.05D);

            player.setDeltaMovement(player.getDeltaMovement().x,
                    Math.max(player.getDeltaMovement().y, jumpBoost),
                    player.getDeltaMovement().z);
            MovementFeatureSupport.setHorizontalSpeed(player, launchSpeed);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;

            manager.longJumpBoostTicks = 7 + (int) Math.round(chargeRatio * 5.0D);
            manager.longJumpBoostSpeed = launchSpeed * 0.94D;
            manager.longJumpCooldownTicks = Math.max(12, Math.round(fullChargeTicks * 0.60F));
            manager.longJumpChargeTicks = 0;
        }

        if (!player.onGround() && manager.longJumpChargeTicks > 0 && !sneakDown) {
            manager.longJumpChargeTicks = 0;
        }

        manager.wasLongJumpSneakDown = sneakDown;
    }
}


