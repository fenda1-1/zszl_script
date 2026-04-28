package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;

public class GuiButton extends Gui {

    public static final net.minecraft.resources.Identifier BUTTON_TEXTURES =
            net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/widgets.png");

    public final int id;
    public int x;
    public int y;
    public int width;
    public int height;
    public String displayString;
    public boolean enabled = true;
    public boolean visible = true;
    public boolean hovered;
    public int packedFGColour;

    public GuiButton(int buttonId, int x, int y, String buttonText) {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        this.id = buttonId;
        this.x = x;
        this.y = y;
        this.width = widthIn;
        this.height = heightIn;
        this.displayString = buttonText == null ? "" : buttonText;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        hovered = isMouseOver(mouseX, mouseY);
        UiState state = getRenderState();
        GuiTheme.drawButtonFrame(x, y, width, height, state);
        FontRenderer fr = new FontRenderer(mc.font);
        int color = packedFGColour != 0 ? packedFGColour : GuiTheme.getStateTextColor(state);
        if (state == UiState.NORMAL && enabled) {
            color = GuiTheme.LABEL_TEXT;
        }
        color = GuiTheme.resolveTextColor(displayString, color);
        drawCenteredString(fr, displayString, x + width / 2, y + (height - fr.FONT_HEIGHT) / 2, color);
    }

    protected UiState getRenderState() {
        if (!enabled) {
            return UiState.DISABLED;
        }
        if (hovered && Mouse.isButtonDown(0)) {
            return UiState.PRESSED;
        }
        return hovered ? UiState.HOVER : UiState.NORMAL;
    }

    protected void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.drawCenteredString(fontRenderer.unwrap(), text == null ? "" : text, x, y,
                    Gui.withDefaultTextAlpha(color));
        }
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return enabled && visible && isMouseOver(mouseX, mouseY);
    }

    public void mouseReleased(int mouseX, int mouseY) {
    }

    public void playPressSound(net.minecraft.client.sounds.SoundManager soundHandlerIn) {
        soundHandlerIn.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public boolean isMouseOver() {
        return hovered;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public int getButtonWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

