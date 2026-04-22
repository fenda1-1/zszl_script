package com.zszl.zszlScriptMod.gui.path;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.function.Consumer;

public class GuiActionEditorCompat extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final ActionData actionToEdit;
    private final Consumer<ActionData> onSave;
    private final String currentSequenceName;

    public GuiActionEditorCompat(GuiScreen parent, ActionData action, Consumer<ActionData> onSaveCallback) {
        this(parent, action, onSaveCallback, null);
    }

    public GuiActionEditorCompat(GuiScreen parent, ActionData action, Consumer<ActionData> onSaveCallback, String currentSequenceName) {
        this.parentScreen = parent;
        this.onSave = onSaveCallback;
        this.currentSequenceName = currentSequenceName == null ? "" : currentSequenceName;
        this.actionToEdit = action == null ? new ActionData("delay", new JsonObject()) : action;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 360;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        this.buttonList.add(new ThemedButton(0, panelX + 12, panelY + panelHeight - 28, 110, 20, "保存"));
        this.buttonList.add(new ThemedButton(1, panelX + panelWidth - 122, panelY + panelHeight - 28, 110, 20, "取消"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            if (onSave != null) {
                onSave.accept(actionToEdit);
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
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "动作编辑器", this.fontRenderer);
        drawString(this.fontRenderer, "兼容版动作编辑器已启用。", panelX + 12, panelY + 34, GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer, "当前动作类型: " + actionToEdit.type, panelX + 12, panelY + 56, 0xFFFFFFFF);
        if (!currentSequenceName.isEmpty()) {
            drawString(this.fontRenderer, "所属序列: " + currentSequenceName, panelX + 12, panelY + 78, 0xFFFFFFFF);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}


