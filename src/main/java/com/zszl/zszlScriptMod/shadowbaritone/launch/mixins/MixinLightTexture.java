package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    @Redirect(
            method = "updateLightTexture",
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;"
                    )
            ),
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object zszl$overrideGammaOption(OptionInstance<?> instance) {
        Object configuredGamma = instance.get();
        double gamma = configuredGamma instanceof Number ? ((Number) configuredGamma).doubleValue() : 1.0D;
        if (RenderFeatureManager.isBrightnessOverrideActive()) {
            gamma = Math.max(gamma, RenderFeatureManager.getEffectiveBrightnessGammaOverride());
        }
        if (KillAuraHandler.isBrightnessOverrideActive()) {
            gamma = Math.max(gamma, KillAuraHandler.getEffectiveBrightnessGammaOverride());
        }
        return Double.valueOf(gamma);
    }
}
