package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.network.chat.Component;

public class ChatLine {

    private final int updatedCounter;
    private final Component chatComponent;
    private final int chatLineID;

    public ChatLine(int updatedCounter, Component chatComponent, int chatLineID) {
        this.updatedCounter = updatedCounter;
        this.chatComponent = chatComponent;
        this.chatLineID = chatLineID;
    }

    public int getUpdatedCounter() {
        return updatedCounter;
    }

    public Component getChatComponent() {
        return chatComponent;
    }

    public int getChatLineID() {
        return chatLineID;
    }
}

