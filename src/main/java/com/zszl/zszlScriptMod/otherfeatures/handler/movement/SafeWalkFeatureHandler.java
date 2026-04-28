package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

final class SafeWalkFeatureHandler {

    private SafeWalkFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (!MovementFeatureManager.isEnabled("safe_walk")
                || player == null
                || player.level() == null
                || player.noPhysics
                || player.getAbilities().flying
                || !player.onGround()
                || player.isPassenger()
                || (player.input != null && player.input.keyPresses.jump())) {
            return;
        }

        double edgeMargin = MovementFeatureManager.getConfiguredValue("safe_walk", 0.35F);
        MovementFeatureSupport.clampHorizontalMotionToSafeWalk(player, edgeMargin);
    }
}


