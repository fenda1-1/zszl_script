package com.zszl.zszlScriptMod.gui.components;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;

/**
 * 安全版主题按钮：不使用纹理，仅使用纯色主题绘制，避免纹理异常导致灰块/无文字。
 */
public class SafeThemedButton extends GuiButton {

    public SafeThemedButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
                && mouseY < this.y + this.height;
        boolean pressed = this.hovered && Mouse.isButtonDown(0);

        UiState state;
        if (!this.enabled) {
            state = UiState.DISABLED;
        } else if (pressed) {
            state = UiState.PRESSED;
        } else if (this.hovered) {
            state = UiState.HOVER;
        } else {
            state = UiState.NORMAL;
        }

        GuiTheme.drawButtonFrameSafe(this.x, this.y, this.width, this.height, state);
        int textColor = GuiTheme.getStateTextColor(state);
        if (state == UiState.NORMAL && this.enabled) {
            textColor = GuiTheme.LABEL_TEXT;
        }
        textColor = GuiTheme.resolveTextColor(this.displayString, textColor);
        drawCenteredString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font), this.displayString, this.x + this.width / 2,
                this.y + (this.height - 8) / 2, textColor);
    }
}







