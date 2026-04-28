package com.zszl.zszlScriptMod.gui;

import java.io.IOException;
import java.util.List;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public class GuiInventoryConfirmScreen extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final String title;
    private final String message;
    private final Runnable onConfirm;

    public GuiInventoryConfirmScreen(GuiScreen parentScreen, String title, String message, Runnable onConfirm) {
        this.parentScreen = parentScreen;
        this.title = title == null ? "确认操作" : title;
        this.message = message == null ? "" : message;
        this.onConfirm = onConfirm;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 300;
        int panelHeight = 150;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int buttonWidth = 110;
        int buttonY = panelY + panelHeight - 34;

        this.buttonList.add(new ThemedButton(0, panelX + 16, buttonY, buttonWidth, 20, "§c确定删除"));
        this.buttonList.add(new ThemedButton(1, panelX + panelWidth - 16 - buttonWidth, buttonY, buttonWidth, 20, "取消"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            Object screenBeforeCallback = this.mc.screen;
            if (onConfirm != null) {
                onConfirm.run();
            }
            if (this.mc.screen == null || this.mc.screen == this || this.mc.screen == screenBeforeCallback) {
                this.mc.setScreen(parentScreen);
            }
        } else if (button.id == 1) {
            this.mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 300;
        int panelHeight = 150;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, title, this.fontRenderer);

        List<String> lines = this.fontRenderer.listFormattedStringToWidth(message, panelWidth - 28);
        int textY = panelY + 34;
        for (String line : lines) {
            this.drawString(this.fontRenderer, line, panelX + 14, textY, 0xFFFFFFFF);
            textY += this.fontRenderer.FONT_HEIGHT + 3;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}





