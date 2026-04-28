package com.zszl.zszlScriptMod.otherfeatures.gui.item;

import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public final class ItemFeatureGuiFactory {

    private ItemFeatureGuiFactory() {
    }

    public static GuiScreen create(GuiScreen parent, String featureId) {
        ItemFeatureManager.FeatureState feature = ItemFeatureManager.getFeature(featureId);
        if (feature == null) {
            return null;
        }
        return new SingleItemFeatureConfigScreen(parent, feature.id, feature.name + "设置");
    }
}



