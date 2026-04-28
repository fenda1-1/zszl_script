package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {

    @Shadow
    private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Shadow
    private boolean ignoreFirstMove;

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    private boolean mouseGrabbed;

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void zszl$keepMouseDetachedDuringGrab(CallbackInfo ci) {
        if (!ModConfig.isMouseDetached) {
            return;
        }
        this.mouseGrabbed = false;
        this.ignoreFirstMove = true;
        this.accumulatedDX = 0.0D;
        this.accumulatedDY = 0.0D;
        if (this.minecraft != null && this.minecraft.getWindow() != null) {
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(),
                    InputConstants.CURSOR_NORMAL,
                    this.xpos,
                    this.ypos);
        }
        ci.cancel();
    }

    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    private void zszl$preserveCursorPositionWhenDetached(CallbackInfo ci) {
        if (!ModConfig.isMouseDetached) {
            return;
        }
        if (this.mouseGrabbed) {
            this.mouseGrabbed = false;
            this.ignoreFirstMove = true;
            this.accumulatedDX = 0.0D;
            this.accumulatedDY = 0.0D;
            if (this.minecraft != null && this.minecraft.getWindow() != null) {
                InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(),
                        InputConstants.CURSOR_NORMAL,
                        this.xpos,
                        this.ypos);
            }
        }
        ci.cancel();
    }
}
