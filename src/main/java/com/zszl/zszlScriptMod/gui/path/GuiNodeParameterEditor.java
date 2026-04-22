package com.zszl.zszlScriptMod.gui.path;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class GuiNodeParameterEditor extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final Consumer<JsonObject> onSave;
    private final JsonObject currentData;

    public GuiNodeParameterEditor(GuiScreen parent, String nodeType, JsonObject currentData, List<String> graphNames,
            Consumer<JsonObject> onSave) {
        this.parentScreen = parent;
        this.onSave = onSave;
        this.currentData = currentData == null ? new JsonObject() : currentData;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        this.buttonList.add(new ThemedButton(0, panelX + 12, panelY + panelHeight - 28, 80, 20, "保存"));
        this.buttonList.add(new ThemedButton(1, panelX + panelWidth - 92, panelY + panelHeight - 28, 80, 20, "取消"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            if (onSave != null) {
                onSave.accept(currentData);
            }
            mc.setScreen(parentScreen);
        } else if (button.id == 1) {
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
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "节点参数", this.fontRenderer);
        drawString(this.fontRenderer, "兼容版参数编辑器占位界面。", panelX + 12, panelY + 44, GuiTheme.SUB_TEXT);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}


