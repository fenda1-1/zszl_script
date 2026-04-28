package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GuiNewChat extends Gui {

    protected final Minecraft mc;
    private final List<String> sentMessages = new ArrayList<>();

    public GuiNewChat(Minecraft mc) {
        this.mc = mc;
    }

    public void drawChat(int updateCounter) {
    }

    public void printChatMessage(Component component) {
    }

    public void printChatMessageWithOptionalDeletion(Component component, int chatLineId) {
    }

    public void deleteChatLine(int id) {
    }

    public int getLineCount() {
        return 10;
    }

    public boolean getChatOpen() {
        return false;
    }

    public float getChatScale() {
        return 1.0F;
    }

    public int getChatWidth() {
        return mc.getWindow().getGuiScaledWidth();
    }

    public void addToSentMessages(String message) {
        if (message != null && !message.isEmpty()) {
            sentMessages.add(message);
        }
    }

    public List<String> getSentMessages() {
        return sentMessages;
    }
}

