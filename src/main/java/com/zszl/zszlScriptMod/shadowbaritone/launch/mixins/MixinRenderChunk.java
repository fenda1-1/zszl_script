package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public class MixinRenderChunk {

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void zszl$applyXray(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && !RenderFeatureManager.shouldRenderBlockInXray(state)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
