package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

public class GuiTextField {

    private final EditBox delegate;
    public int x;
    public int y;
    public int width;
    public int height;
    private boolean backgroundDrawing = true;
    private boolean enabled = true;
    private boolean canLoseFocus = true;

    public GuiTextField(int componentId, FontRenderer fontRenderer, int x, int y, int width, int height) {
        this.delegate = new EditBox(fontRenderer.unwrap(), x, y, width, height, Component.empty());
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.delegate.setEditable(true);
        this.delegate.setVisible(true);
        this.delegate.setCanLoseFocus(true);
    }

    public GuiTextField(int componentId, net.minecraft.client.gui.Font fontRenderer, int x, int y, int width,
            int height) {
        this(componentId, new FontRenderer(fontRenderer), x, y, width, height);
    }

    private void syncBounds() {
        delegate.setX(x);
        delegate.setY(y);
        delegate.setWidth(width);
        delegate.setHeight(height);
    }

    public void setText(String text) {
        delegate.setValue(text == null ? "" : text);
    }

    public String getText() {
        return delegate.getValue();
    }

    public void writeText(String textToWrite) {
        delegate.insertText(textToWrite == null ? "" : textToWrite);
    }

    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (typedChar != 0 && typedChar != '\u0000') {
            return delegate.charTyped(new CharacterEvent(typedChar, 0));
        }
        return delegate.keyPressed(new KeyEvent(Keyboard.toGlfwKey(keyCode), 0, 0));
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        syncBounds();
        boolean inside = delegate.isMouseOver(mouseX, mouseY);
        if (mouseButton == 0) {
            if (inside && enabled) {
                delegate.setFocused(true);
                delegate.onClick(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(mouseButton, 0)), false);
            } else if (!inside && canLoseFocus) {
                delegate.setFocused(false);
            }
            return;
        }
        if (!inside && canLoseFocus) {
            delegate.setFocused(false);
        }
    }

    public void drawTextBox() {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null || !delegate.isVisible()) {
            return;
        }
        syncBounds();
        delegate.setBordered(backgroundDrawing);
        delegate.render(graphics, 0, 0, 0.0F);
    }

    public void updateCursorCounter() {
    }

    public void setMaxStringLength(int length) {
        delegate.setMaxLength(length);
    }

    public void setFocused(boolean focused) {
        if (focused && !enabled) {
            return;
        }
        delegate.setFocused(focused);
    }

    public boolean isFocused() {
        return delegate.isFocused();
    }

    public void setCursorPosition(int pos) {
        delegate.setCursorPosition(pos);
    }

    public void setCursorPositionEnd() {
        delegate.moveCursorToEnd(false);
    }

    public int getCursorPosition() {
        return delegate.getCursorPosition();
    }

    public String getSelectedText() {
        return delegate.getHighlighted();
    }

    public void setEnableBackgroundDrawing(boolean backgroundDrawing) {
        this.backgroundDrawing = backgroundDrawing;
    }

    public void setVisible(boolean visible) {
        delegate.setVisible(visible);
    }

    public boolean getVisible() {
        return delegate.isVisible();
    }

    public void setCanLoseFocus(boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
        delegate.setCanLoseFocus(canLoseFocus);
    }

    public void setValidator(Predicate<String> validator) {
        delegate.setFilter(validator == null ? s -> true : validator);
    }

    public void setTextColor(int color) {
        delegate.setTextColor(color);
    }

    public void setDisabledTextColour(int color) {
        delegate.setTextColorUneditable(color);
    }

    public void setSelectionPos(int pos) {
        delegate.setHighlightPos(pos);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        delegate.setEditable(enabled);
        if (!enabled) {
            delegate.setFocused(false);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}

