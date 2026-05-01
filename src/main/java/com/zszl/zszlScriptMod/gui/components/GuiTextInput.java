// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/components/GuiTextInput.java
package com.zszl.zszlScriptMod.gui.components;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.function.Consumer;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

/**
 * 通用文本输入GUI
 */
public class GuiTextInput extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private final String title;
    private final Consumer<String> callback;
    private GuiTextField inputField;
    private String initialText = "";

    public GuiTextInput(GuiScreen parent, String title, Consumer<String> callback) {
        this.parentScreen = parent;
        this.title = title;
        this.callback = callback;
    }

    public GuiTextInput(GuiScreen parent, String title, String initialText, Consumer<String> callback) {
        this(parent, title, callback);
        this.initialText = initialText;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelWidth = 220;
        int panelHeight = 120;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        this.inputField = new GuiTextField(0, this.fontRenderer, panelX + 10, panelY + 50, panelWidth - 20, 20);

        // !! 核心修改：解除输入框的长度限制 !!
        this.inputField.setMaxStringLength(Integer.MAX_VALUE);

        this.inputField.setFocused(true);
        this.inputField.setText(initialText);

        this.buttonList.add(
                new GuiButton(0, panelX + 10, panelY + 80, (panelWidth - 30) / 2, 20, I18n.format("gui.loop.confirm")));
        this.buttonList.add(new GuiButton(1, panelX + 20 + (panelWidth - 30) / 2, panelY + 80, (panelWidth - 30) / 2,
                20, I18n.format("gui.common.cancel")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // 确认
            net.minecraft.client.gui.screens.Screen screenBeforeCallback = this.mc.screen;
            this.callback.accept(this.inputField.getText());
            if (this.mc.screen == null || this.mc.screen == screenBeforeCallback
                    || this.mc.screen == this) {
                this.mc.setScreen(this.parentScreen);
            }
        } else if (button.id == 1) { // 取消
            this.mc.setScreen(this.parentScreen);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.inputField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            this.actionPerformed(this.buttonList.get(0));
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            this.actionPerformed(this.buttonList.get(1));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;

        int button = Mouse.getEventButton();
        if (button == -1) {
            return;
        }

        if (Mouse.getEventButtonState()) {
            handleMousePressed(mouseX, mouseY, button);
        } else {
            mouseReleased(mouseX, mouseY, button);
        }
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }
        this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
        return false;
    }

    private boolean handleButtonActivation(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : this.buttonList) {
            if (button == null || !button.visible || !button.enabled) {
                continue;
            }
            if (mouseX < button.x || mouseX >= button.x + button.width
                    || mouseY < button.y || mouseY >= button.y + button.height) {
                continue;
            }
            button.playPressSound(this.mc.getSoundManager());
            actionPerformed(button);
            return true;
        }
        return false;
    }

    @Override
    public void updateScreen() {
        this.inputField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = 220;
        int panelHeight = 120;
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY, 0xFF888888);
        drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF888888);
        drawVerticalLine(panelX, panelY, panelY + panelHeight - 1, 0xFF888888);
        drawVerticalLine(panelX + panelWidth - 1, panelY, panelY + panelHeight - 1, 0xFF888888);

        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, panelY + 20, 16777215);
        drawThemedTextField(this.inputField);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}







