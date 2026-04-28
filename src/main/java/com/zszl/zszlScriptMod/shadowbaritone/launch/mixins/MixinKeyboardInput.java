package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.otherfeatures.handler.movement.GuiMoveFeatureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardInput.class, priority = 1200)
public class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("RETURN"))
    private void zszlScript$applyGuiMovementAfterVanilla(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        GuiMoveFeatureHandler.applyMovementInput(mc, (ClientInput) (Object) this);
    }
}
