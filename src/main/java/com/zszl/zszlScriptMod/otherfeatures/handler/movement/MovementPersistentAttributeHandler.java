package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

import java.lang.reflect.Field;

final class MovementPersistentAttributeHandler {

    private static Field collisionReductionField;
    private static boolean collisionReductionLookupAttempted;

    private MovementPersistentAttributeHandler() {
    }

    static void apply(LocalPlayer player) {
        if (player == null) {
            return;
        }
        player.setMaxUpStep(MovementFeatureManager.isEnabled("auto_step")
                ? MovementFeatureManager.getConfiguredValue("auto_step", MovementFeatureManager.DEFAULT_STEP_HEIGHT)
                : MovementFeatureManager.DEFAULT_STEP_HEIGHT);
        setCollisionReduction(player, MovementFeatureManager.isEnabled("no_collision") ? 1.0F
                : MovementFeatureManager.DEFAULT_COLLISION_REDUCTION);
    }

    static void reset(LocalPlayer player) {
        if (player == null) {
            return;
        }
        player.setMaxUpStep(MovementFeatureManager.DEFAULT_STEP_HEIGHT);
        setCollisionReduction(player, MovementFeatureManager.DEFAULT_COLLISION_REDUCTION);
    }

    private static void setCollisionReduction(LocalPlayer player, float value) {
        try {
            if (!collisionReductionLookupAttempted) {
                collisionReductionLookupAttempted = true;
                for (Class<?> type = player.getClass(); type != null && collisionReductionField == null; type = type.getSuperclass()) {
                    try {
                        collisionReductionField = type.getDeclaredField("entityCollisionReduction");
                        collisionReductionField.setAccessible(true);
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
            if (collisionReductionField != null) {
                collisionReductionField.setFloat(player, value);
            }
        } catch (Throwable ignored) {
        }
    }
}


