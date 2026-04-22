package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    @Redirect(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 1
            )
    )
    private Object zszl$overrideGammaOption(OptionInstance<?> instance) {
        double gamma = instance.get() instanceof Number ? ((Number) instance.get()).doubleValue() : 1.0D;
        if (RenderFeatureManager.isBrightnessOverrideActive()) {
            gamma = Math.max(gamma, RenderFeatureManager.getEffectiveBrightnessGammaOverride());
        }
        if (KillAuraHandler.isBrightnessOverrideActive()) {
            gamma = Math.max(gamma, KillAuraHandler.getEffectiveBrightnessGammaOverride());
        }
        return Double.valueOf(gamma);
    }
}
