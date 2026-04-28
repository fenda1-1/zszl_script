package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;

class ToggleOptionButton extends ThemedButton {
    final String key;
    private final String trueLabel;
    private final String falseLabel;
    private boolean value;

    ToggleOptionButton(int id, int x, int y, int width, int height,
            String key, String trueLabel, String falseLabel, boolean initialValue) {
        super(id, x, y, width, height, "");
        this.key = key;
        this.trueLabel = trueLabel;
        this.falseLabel = falseLabel;
        this.value = initialValue;
        refreshText();
    }

    void toggle() {
        this.value = !this.value;
        refreshText();
    }

    boolean getValue() {
        return value;
    }

    void setValue(boolean value) {
        this.value = value;
        refreshText();
    }

    private void refreshText() {
        this.displayString = value ? "开启: " + trueLabel : "关闭: " + falseLabel;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
                && mouseY < this.y + this.height;

        GuiTheme.UiState state;
        if (!this.enabled) {
            state = GuiTheme.UiState.DISABLED;
        } else if (this.hovered) {
            state = value ? GuiTheme.UiState.SUCCESS : GuiTheme.UiState.DANGER;
        } else {
            state = value ? GuiTheme.UiState.SUCCESS : GuiTheme.UiState.NORMAL;
        }

        GuiTheme.drawButtonFrame(this.x, this.y, this.width, this.height, state);
        int textColor = GuiTheme.resolveTextColor(this.displayString, GuiTheme.getStateTextColor(state));
        drawCenteredString(new FontRenderer(mc.font), this.displayString, this.x + this.width / 2,
                this.y + (this.height - 8) / 2, textColor);
    }
}
