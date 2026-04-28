package com.zszl.zszlScriptMod.gui.halloffame;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public class GuiHallOfFame extends GuiScreen {

    public GuiHallOfFame(GuiScreen parent, String content) {
    }

    public static GuiHallOfFame createTitleCompendiumView(GuiScreen parent) {
        return new GuiHallOfFame(parent, "");
    }

    public static GuiHallOfFame createEnhancementAttrView(GuiScreen parent) {
        return new GuiHallOfFame(parent, "");
    }

    public static GuiHallOfFame createAdExpListView(GuiScreen parent) {
        return new GuiHallOfFame(parent, "");
    }
}


