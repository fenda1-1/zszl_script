package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.player.LocalPlayer;

final class NoFallFeatureHandler {

    private static final float MIN_PROTECT_FALL_DISTANCE = 2.0F;

    private NoFallFeatureHandler() {
    }

    static void apply(LocalPlayer player) {
        if (player == null
                || !MovementFeatureManager.isEnabled("no_fall")
                || player.connection == null
                || player.getAbilities().flying
                || player.isInWater()
                || player.isInLava()
                || player.fallDistance <= MIN_PROTECT_FALL_DISTANCE) {
            return;
        }
        player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly(true, false));
        player.fallDistance = 0.0F;
    }
}
