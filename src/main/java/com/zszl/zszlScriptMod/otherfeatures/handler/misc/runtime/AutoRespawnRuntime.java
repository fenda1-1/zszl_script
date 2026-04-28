package com.zszl.zszlScriptMod.otherfeatures.handler.misc.runtime;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

public final class AutoRespawnRuntime {

    private int cooldownTicks;

    public void onClientDisconnect() {
        this.cooldownTicks = 0;
    }

    public void tick(Minecraft mc, boolean featureEnabled, int respawnDelayTicks) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
        if (!featureEnabled || this.cooldownTicks > 0 || mc == null || mc.player == null) {
            return;
        }
        if (!(mc.screen instanceof DeathScreen)) {
            return;
        }
        try {
            mc.player.respawn();
            mc.setScreen(null);
            this.cooldownTicks = Math.max(1, respawnDelayTicks);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.debug("自动复活执行失败", e);
        }
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }
}
