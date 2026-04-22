package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import java.io.IOException;

public class GuiYesNo extends GuiScreen {

    private final GuiYesNoCallback callback;
    private final String title;
    private final String message;
    private final int parentButtonClickedId;

    public GuiYesNo(GuiYesNoCallback callback, String title, String message, int id) {
        this.callback = callback;
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.parentButtonClickedId = id;
    }

    public GuiYesNo(GuiYesNoCallback callback, String title, String message, String yesText, String noText, int id) {
        this(callback, title, message, id);
    }

    public GuiYesNo(GuiScreen parentScreen, String title, String message, int id) {
        this((GuiYesNoCallback) null, title, message, id);
    }

    public GuiYesNo(GuiScreen parentScreen, String title, String message, String yesText, String noText, int id) {
        this((GuiYesNoCallback) null, title, message, id);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 20;
        buttonList.add(new GuiButton(0, centerX - 105, buttonY, 100, 20, "是"));
        buttonList.add(new GuiButton(1, centerX + 5, buttonY, 100, 20, "否"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (callback != null) {
            callback.confirmClicked(button.id == 0, parentButtonClickedId);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(this.fontRenderer, title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        drawCenteredString(this.fontRenderer, message, this.width / 2, this.height / 2 - 10, 0xCCCCCC);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

