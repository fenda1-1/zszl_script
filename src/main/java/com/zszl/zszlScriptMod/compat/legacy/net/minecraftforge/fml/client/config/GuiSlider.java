package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;

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
        super.drawButton(mc, mouseX, mouseY, partialTicks);
    }

    private void updateFromMouse(int mouseX) {
        this.sliderValue = clamp((mouseX - (this.x + 4.0D)) / Math.max(1.0D, this.width - 8.0D));
        updateDisplayString();
    }
}



