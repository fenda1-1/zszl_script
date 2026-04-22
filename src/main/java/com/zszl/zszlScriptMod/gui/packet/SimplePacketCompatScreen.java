package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

abstract class SimplePacketCompatScreen extends ThemedGuiScreen {

    protected final GuiScreen parentScreen;
    private final String title;

    protected SimplePacketCompatScreen(GuiScreen parentScreen, String title) {
        this.parentScreen = parentScreen;
        this.title = title == null ? "数据包工具" : title;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        this.buttonList.add(new ThemedButton(0, panelX + panelWidth - 92, panelY + panelHeight - 28, 80, 20, "返回"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, title, this.fontRenderer);
        drawString(this.fontRenderer, "该工具已迁移为 1.20.1 兼容占位界面。", panelX + 12, panelY + 42, GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer, "如需继续扩展，可在当前版本上补回具体业务逻辑。", panelX + 12, panelY + 64, 0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}



