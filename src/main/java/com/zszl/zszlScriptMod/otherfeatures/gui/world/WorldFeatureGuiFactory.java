package com.zszl.zszlScriptMod.otherfeatures.gui.world;

import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public final class WorldFeatureGuiFactory {

    private WorldFeatureGuiFactory() {
    }

    public static GuiScreen create(GuiScreen parent, String featureId) {
        WorldFeatureManager.FeatureState feature = WorldFeatureManager.getFeature(featureId);
        if (feature == null) {
            return null;
        }
        return new SingleWorldFeatureConfigScreen(parent, feature.id, feature.name + "设置");
    }
}



