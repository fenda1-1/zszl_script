package com.zszl.zszlScriptMod.otherfeatures.handler.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;

final class AntiKnockbackFeatureHandler {

    private AntiKnockbackFeatureHandler() {
    }

    static void onKnockback(LivingKnockBackEvent event) {
        if (event == null || !MovementFeatureManager.isEnabled("anti_knockback")) {
            return;
        }
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (player != Minecraft.getInstance().player) {
            return;
        }
        event.setCanceled(true);
    }

    static void apply(LocalPlayer player) {
        if (player == null || !MovementFeatureManager.isEnabled("anti_knockback") || player.hurtTime <= 0) {
            return;
        }
        if (MovementFeatureSupport.isMoving(player)) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        player.hasImpulse = false;
        player.fallDistance = 0.0F;
    }
}
