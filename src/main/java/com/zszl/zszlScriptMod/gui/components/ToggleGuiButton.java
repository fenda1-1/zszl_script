package com.zszl.zszlScriptMod.gui.components;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;

public class ToggleGuiButton extends GuiButton {
    private boolean enabledState;

    public ToggleGuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText,
            boolean initialState) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.enabledState = initialState;
    }

    public void setEnabledState(boolean state) {
        this.enabledState = state;
    }

    public boolean getEnabledState() {
        return enabledState;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
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

            GuiTheme.drawToggleFrame(this.x, this.y, this.width, this.height, enabledState, state);
            int j = 0xFFFFFFFF; // White text color

            if (!this.enabled) {
                j = GuiTheme.getStateTextColor(UiState.DISABLED);
            } else if (this.hovered) {
                j = GuiTheme.getStateTextColor(UiState.HOVER);
            } else if (enabledState) {
                j = GuiTheme.getStateTextColor(UiState.SUCCESS);
            } else {
                j = GuiTheme.getStateTextColor(UiState.NORMAL);
            }

            this.drawCenteredString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font), this.displayString, this.x + this.width / 2,
                    this.y + (this.height - 8) / 2, j);
        }
    }
}







