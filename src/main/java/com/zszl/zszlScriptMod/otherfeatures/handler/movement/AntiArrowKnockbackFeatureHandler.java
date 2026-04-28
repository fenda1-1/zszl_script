package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

final class AntiArrowKnockbackFeatureHandler {

    private AntiArrowKnockbackFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("anti_arrow_knockback") || player.hurtTime <= 0) {
            return;
        }
        double keepRatio = 1.0D - MovementFeatureManager.getConfiguredValue("anti_arrow_knockback", 0.72F);
        player.setDeltaMovement(player.getDeltaMovement().multiply(keepRatio, 1.0D, keepRatio));
        player.hurtMarked = true;
    }
}


