package com.zszl.zszlScriptMod.shadowbaritone.utils;

import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import net.minecraft.client.Minecraft;

public final class GuiPathingPolicy {

    private GuiPathingPolicy() {
    }

    public static boolean shouldKeepPathingDuringGui(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            return false;
        }
        IBaritone primary = BaritoneAPI.getProvider().getPrimaryBaritone();
        return (primary != null && primary.getPathingBehavior().isPathing())
                || KillAuraHandler.INSTANCE.shouldKeepRunningDuringGui(mc);
    }
}
