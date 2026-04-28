package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

final class IceBoostFeatureHandler {

    private IceBoostFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("ice_boost")
                || !MovementFeatureSupport.isMoving(player)
                || !MovementFeatureSupport.isStandingOnIce(player)) {
            return;
        }
        double boosted = MovementFeatureSupport.getBaseMoveSpeed()
                * MovementFeatureManager.getConfiguredValue("ice_boost", 1.25F);
        MovementFeatureSupport.ensureHorizontalSpeed(player, boosted);
    }
}


