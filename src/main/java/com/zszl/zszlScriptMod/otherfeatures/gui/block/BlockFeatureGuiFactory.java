package com.zszl.zszlScriptMod.otherfeatures.gui.block;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public final class BlockFeatureGuiFactory {

    private BlockFeatureGuiFactory() {
    }

    public static GuiScreen create(GuiScreen parent, String featureId) {
        String normalized = featureId == null ? "" : featureId.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
        case "auto_tool":
            return new SingleBlockFeatureConfigScreen(parent, "auto_tool", "自动切工具设置");
        case "fast_place":
            return new SingleBlockFeatureConfigScreen(parent, "fast_place", "快速放置设置");
        case "place_assist":
            return new SingleBlockFeatureConfigScreen(parent, "place_assist", "精准放置辅助设置");
        case "fast_break":
            return new SingleBlockFeatureConfigScreen(parent, "fast_break", "基础快速挖掘设置");
        case "block_swap_lock":
            return new SingleBlockFeatureConfigScreen(parent, "block_swap_lock", "方块热栏锁定设置");
        case "auto_light":
            return new SingleBlockFeatureConfigScreen(parent, "auto_light", "自动补光设置");
        case "block_refill":
            return new SingleBlockFeatureConfigScreen(parent, "block_refill", "方块自动补栏设置");
        case "ghost_hand_block":
            return new SingleBlockFeatureConfigScreen(parent, "ghost_hand_block", "穿墙方块交互设置");
        case "surround":
            return new SingleBlockFeatureConfigScreen(parent, "surround", "自动围身设置");
        default:
            return null;
        }
    }
}



