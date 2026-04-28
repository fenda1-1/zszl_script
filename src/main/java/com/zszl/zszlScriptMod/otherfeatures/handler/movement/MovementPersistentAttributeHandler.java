package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
        setStepHeight(player, MovementFeatureManager.isEnabled("auto_step")
                ? MovementFeatureManager.getConfiguredValue("auto_step", MovementFeatureManager.DEFAULT_STEP_HEIGHT)
                : MovementFeatureManager.DEFAULT_STEP_HEIGHT);
        setCollisionReduction(player, MovementFeatureManager.isEnabled("no_collision") ? 1.0F
                : MovementFeatureManager.DEFAULT_COLLISION_REDUCTION);
    }

    static void reset(LocalPlayer player) {
        if (player == null) {
            return;
        }
        setStepHeight(player, MovementFeatureManager.DEFAULT_STEP_HEIGHT);
        setCollisionReduction(player, MovementFeatureManager.DEFAULT_COLLISION_REDUCTION);
    }

    private static void setStepHeight(LocalPlayer player, float value) {
        AttributeInstance attribute = player.getAttribute(Attributes.STEP_HEIGHT);
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
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


