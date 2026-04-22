package com.zszl.zszlScriptMod.otherfeatures.gui.misc;

import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public final class MiscFeatureGuiFactory {

    private MiscFeatureGuiFactory() {
    }

    public static GuiScreen create(GuiScreen parent, String featureId) {
        MiscFeatureManager.FeatureState feature = MiscFeatureManager.getFeature(featureId);
        if (feature == null) {
            return null;
        }
        if ("auto_reconnect".equalsIgnoreCase(feature.id)) {
            return new GuiAutoReconnectConfig(parent);
        }
        if ("auto_respawn".equalsIgnoreCase(feature.id)) {
            return new GuiAutoRespawnConfig(parent);
        }
        return new SingleMiscFeatureConfigScreen(parent, feature.id, feature.name + "设置");
    }
}



