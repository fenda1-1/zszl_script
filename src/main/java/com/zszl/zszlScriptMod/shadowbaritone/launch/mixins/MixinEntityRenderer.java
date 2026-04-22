package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void zszl$disableViewBob(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (RenderFeatureManager.shouldSuppressViewBobbing()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void zszl$disableHurtBob(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (RenderFeatureManager.shouldSuppressHurtCamera()) {
            ci.cancel();
        }
    }
}
