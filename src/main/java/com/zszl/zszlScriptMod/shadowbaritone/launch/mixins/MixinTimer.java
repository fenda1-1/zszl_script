package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public class MixinTimer {

    @Shadow
    private float deltaTicks;

    @Shadow
    private float deltaTickResidual;

    @Inject(
            method = "advanceTime",
            at = @At("RETURN"),
            cancellable = true
    )
    private void zszl$applyTimerMultiplier(long nowMs, boolean advanceGameTime, CallbackInfoReturnable<Integer> cir) {
        float multiplier = SpeedHandler.getAppliedTimerSpeedMultiplier();
        if (!advanceGameTime || multiplier <= 1.0001F) {
            return;
        }

        float originalTickDelta = this.deltaTicks;
        int originalWholeTicks = cir.getReturnValue();
        float previousPartialTick = this.deltaTickResidual + originalWholeTicks - originalTickDelta;
        float desiredTickDelta = originalTickDelta * multiplier;
        float desiredTotalTick = previousPartialTick + desiredTickDelta;
        int desiredWholeTicks = (int) desiredTotalTick;
        float desiredPartialTick = desiredTotalTick - desiredWholeTicks;

        this.deltaTicks = desiredTickDelta;
        this.deltaTickResidual = desiredPartialTick;
        cir.setReturnValue(desiredWholeTicks);
    }
}
