package com.zszl.zszlScriptMod.gui.components;

import java.util.Arrays;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiGraphics;

public class ThemedGuiScreen extends GuiScreen {
    protected float readableUiScale = 1.0F;
    protected int rawScreenWidth = 0;
    protected int rawScreenHeight = 0;

    protected void applyReadableUiScaleForLargeScreen(int targetWidth, int targetHeight) {
        this.rawScreenWidth = this.width;
        this.rawScreenHeight = this.height;
        int safeTargetWidth = Math.max(640, targetWidth);
        int safeTargetHeight = Math.max(360, targetHeight);
        float widthScale = this.width / (float) safeTargetWidth;
        float heightScale = this.height / (float) safeTargetHeight;
        this.readableUiScale = Math.max(1.0F, Math.min(1.8F, Math.min(widthScale, heightScale)));
        if (this.readableUiScale <= 1.02F) {
            this.readableUiScale = 1.0F;
            return;
        }
        this.width = Math.max(1, Math.round(this.rawScreenWidth / this.readableUiScale));
        this.height = Math.max(1, Math.round(this.rawScreenHeight / this.readableUiScale));
    }

    protected int toReadableMouseX(int mouseX) {
        return this.readableUiScale <= 1.0F ? mouseX : Math.round(mouseX / this.readableUiScale);
    }

    protected int toReadableMouseY(int mouseY) {
        return this.readableUiScale <= 1.0F ? mouseY : Math.round(mouseY / this.readableUiScale);
    }

    protected void pushReadableUiScale() {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.pose().pushPose();
            if (this.readableUiScale > 1.0F) {
                graphics.pose().scale(this.readableUiScale, this.readableUiScale, 1.0F);
            }
        }
    }

    protected void popReadableUiScale() {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.pose().popPose();
        }
    }

    protected void drawThemedTextField(GuiTextField field) {
        if (field == null) {
            return;
        }
        // 仅在无选中内容时同步光标，避免破坏 Ctrl+A 选区
        if (field.isFocused() && (field.getSelectedText() == null || field.getSelectedText().isEmpty())) {
            field.setCursorPosition(field.getCursorPosition());
        }
        field.setEnableBackgroundDrawing(false);
        GuiTheme.drawInputFrame(field.x - 1, field.y - 1, field.width + 2, field.height + 2,
                field.isFocused(), field.isEnabled());
        field.drawTextBox();
    }

    @Override
    public void drawString(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        FontRenderer renderer = fontRendererIn != null ? fontRendererIn : this.fontRenderer;
        if (renderer == null) {
            return;
        }
        String safeText = text == null ? "" : text;
        super.drawString(renderer, safeText, x, y, GuiTheme.resolveTextColor(safeText, color));
    }

    @Override
    public void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        FontRenderer renderer = fontRendererIn != null ? fontRendererIn : this.fontRenderer;
        if (renderer == null) {
            return;
        }
        String safeText = text == null ? "" : text;
        super.drawCenteredString(renderer, safeText, x, y, GuiTheme.resolveTextColor(safeText, color));
    }

    protected boolean isMouseOverButton(int mouseX, int mouseY, GuiButton button) {
        return button != null && button.visible
                && mouseX >= button.x && mouseX <= button.x + button.width
                && mouseY >= button.y && mouseY <= button.y + button.height;
    }

    protected boolean isMouseOverField(int mouseX, int mouseY, GuiTextField field) {
        return field != null && mouseX >= field.x && mouseX <= field.x + field.width
                && mouseY >= field.y && mouseY <= field.y + field.height;
    }

    protected boolean isHoverRegion(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    protected void drawSimpleTooltip(String text, int mouseX, int mouseY) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        drawHoveringText(Arrays.asList(text.split("\n")), mouseX, mouseY);
    }
}



