package com.zszl.zszlScriptMod.gui;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiNewChat;
import net.minecraft.network.chat.Component;

public class CustomGuiNewChat extends GuiNewChat {

    public boolean configuring = false;

    public CustomGuiNewChat(Minecraft mcIn) {
        super(mcIn);
    }

    public Component getChatComponent(int mouseX, int mouseY) {
        return null;
    }
}


