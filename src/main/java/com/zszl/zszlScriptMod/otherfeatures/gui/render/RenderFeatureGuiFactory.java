package com.zszl.zszlScriptMod.otherfeatures.gui.render;

import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public final class RenderFeatureGuiFactory {

    private RenderFeatureGuiFactory() {
    }

    public static GuiScreen create(GuiScreen parent, String featureId) {
        return RenderFeatureManager.isManagedFeature(featureId) ? new GuiRenderFeatureConfig(parent, featureId) : null;
    }
}



