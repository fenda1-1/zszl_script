// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/config/GuiAutoStackingConfig.java
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.handlers.ShulkerBoxStackingHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;

public class GuiAutoStackingConfig extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private GuiTextField keywordsField;

    public GuiAutoStackingConfig(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int panelWidth = 320;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 80;
        int currentY = panelY + 40;

        keywordsField = new GuiTextField(0, this.fontRenderer, panelX + 10, currentY, panelWidth - 20, 20);
        keywordsField.setMaxStringLength(Integer.MAX_VALUE);
        keywordsField.setText(String.join(", ", ShulkerBoxStackingHandler.stackableItemKeywords));
        currentY += 40;

        this.buttonList.add(new ThemedButton(100, panelX + 10, currentY, (panelWidth - 30) / 2, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList
                .add(new ThemedButton(101, panelX + 20 + (panelWidth - 30) / 2, currentY, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) { // 保存
            ShulkerBoxStackingHandler.stackableItemKeywords = Arrays.stream(keywordsField.getText().split("[,，]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            ShulkerBoxStackingHandler.saveConfig();
            mc.setScreen(parentScreen);
        } else if (button.id == 101) { // 取消
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 320;
        int panelHeight = 130;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 80;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.autostacking.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.autostacking.keywords"), keywordsField.x,
                keywordsField.y - 12,
                GuiTheme.LABEL_TEXT);
        drawThemedTextField(keywordsField);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keywordsField.textboxKeyTyped(typedChar, keyCode)) {
            // Handled
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        keywordsField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}






