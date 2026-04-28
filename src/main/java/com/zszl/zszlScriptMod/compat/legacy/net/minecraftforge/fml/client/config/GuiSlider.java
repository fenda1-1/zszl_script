package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;

public class GuiSlider extends GuiButton {

    private final double minValue;
    private final double maxValue;
    private final String prefix;
    private final String suffix;
    private final boolean showDecimal;
    private double sliderValue;
    private boolean dragging;

    public GuiSlider(int id, int x, int y, int width, int height, String prefix, String suffix,
            double minValue, double maxValue, double currentValue, boolean showDecimal, boolean drawString) {
        super(id, x, y, width, height, "");
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.showDecimal = showDecimal;
        this.sliderValue = clamp((currentValue - minValue) / Math.max(0.0001D, maxValue - minValue));
        updateDisplayString();
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private void updateDisplayString() {
        double value = getValue();
        String rendered = showDecimal ? String.format("%.2f", value) : Integer.toString((int) Math.round(value));
        this.displayString = prefix + rendered + suffix;
    }

    public double getValue() {
        return minValue + sliderValue * (maxValue - minValue);
    }

    public int getValueInt() {
        return (int) Math.round(getValue());
    }

    public void setValue(double value) {
        this.sliderValue = clamp((value - minValue) / Math.max(0.0001D, maxValue - minValue));
        updateDisplayString();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) {
            return false;
        }
        this.dragging = true;
        updateFromMouse(mouseX);
        return true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            updateFromMouse(mouseX);
        }
        if (!this.visible) {
            return;
        }

        this.hovered = isMouseOver(mouseX, mouseY);
        UiState state = getRenderState();
        GuiTheme.drawButtonFrame(this.x, this.y, this.width, this.height, state);

        int trackLeft = this.x + 5;
        int trackRight = this.x + this.width - 5;
        int trackY = this.y + this.height - 5;
        int filledRight = trackLeft + (int) Math.round((trackRight - trackLeft) * sliderValue);
        Gui.drawRect(trackLeft, trackY, trackRight, trackY + 2, 0xAA1A2632);
        Gui.drawRect(trackLeft, trackY, filledRight, trackY + 2, 0xCC78C5FF);

        int handleX = Math.max(trackLeft, Math.min(trackRight, filledRight));
        Gui.drawRect(handleX - 2, this.y + 3, handleX + 2, this.y + this.height - 3,
                this.enabled ? 0xFFE6F7FF : 0xFF97A3B2);

        FontRenderer fr = new FontRenderer(mc.font);
        int textColor = GuiTheme.resolveTextColor(this.displayString, GuiTheme.getStateTextColor(state));
        drawCenteredString(fr, this.displayString, this.x + this.width / 2,
                this.y + (this.height - fr.FONT_HEIGHT) / 2 - 1, textColor);
    }

    private void updateFromMouse(int mouseX) {
        this.sliderValue = clamp((mouseX - (this.x + 4.0D)) / Math.max(1.0D, this.width - 8.0D));
        updateDisplayString();
    }
}



