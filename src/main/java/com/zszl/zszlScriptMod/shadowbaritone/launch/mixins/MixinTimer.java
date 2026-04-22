package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import net.minecraft.client.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Timer.class)
public class MixinTimer {

    @Shadow
    public float partialTick;

    @Shadow
    public float tickDelta;

    @Inject(
            method = "advanceTime",
            at = @At("RETURN"),
            cancellable = true
    )
    private void zszl$applyTimerMultiplier(long nowMs, CallbackInfoReturnable<Integer> cir) {
        float multiplier = SpeedHandler.getAppliedTimerSpeedMultiplier();
        if (multiplier <= 1.0001F) {
            return;
        }

        float originalTickDelta = this.tickDelta;
        int originalWholeTicks = cir.getReturnValue();
        float previousPartialTick = this.partialTick + originalWholeTicks - originalTickDelta;
        float desiredTickDelta = originalTickDelta * multiplier;
        float desiredTotalTick = previousPartialTick + desiredTickDelta;
        int desiredWholeTicks = (int) desiredTotalTick;
        float desiredPartialTick = desiredTotalTick - desiredWholeTicks;

        this.tickDelta = desiredTickDelta;
        this.partialTick = desiredPartialTick;
        cir.setReturnValue(desiredWholeTicks);
    }
}
