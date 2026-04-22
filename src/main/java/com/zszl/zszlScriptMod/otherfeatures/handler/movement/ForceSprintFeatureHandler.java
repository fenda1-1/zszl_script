package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

final class ForceSprintFeatureHandler {

    private ForceSprintFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (MovementFeatureManager.isEnabled("force_sprint")
                && MovementFeatureSupport.isMoving(player)
                && !player.isShiftKeyDown()
                && !player.horizontalCollision) {
            player.setSprinting(true);
        }
    }
}


