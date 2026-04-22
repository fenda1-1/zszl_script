package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

final class PrecisionControlFeatureHandler {

    private PrecisionControlFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("precision_control")
                || !player.isShiftKeyDown()
                || !MovementFeatureSupport.isMoving(player)) {
            return;
        }
        double limited = MovementFeatureSupport.getBaseMoveSpeed()
                * MovementFeatureManager.getConfiguredValue("precision_control", 0.35F);
        MovementFeatureSupport.capHorizontalSpeed(player, limited);
    }
}


