package com.zszl.zszlScriptMod.otherfeatures.gui.block;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;

final class ConfigDropdown {

    private static final int ITEM_HEIGHT = 18;

    private final ThemedGuiScreen owner;
    private final String label;
    private final String[] options;

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean expanded = false;
    private boolean enabled = true;
    private int selectedIndex = 0;

    ConfigDropdown(ThemedGuiScreen owner, String label, String[] options) {
        this.owner = owner;
        this.label = label == null ? "" : label;
        this.options = options == null ? new String[0] : options;
    }

    void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.expanded = false;
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    void setSelectedIndex(int selectedIndex) {
        if (options.length <= 0) {
            this.selectedIndex = 0;
            return;
        }
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.length - 1));
    }

    int getSelectedIndex() {
        return selectedIndex;
    }

    String getSelectedText() {
        return options.length <= 0 ? "" : options[selectedIndex];
    }

    int getVisibleMenuHeight() {
        return expanded ? getExpandedHeight() : 0;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    void collapse() {
        expanded = false;
    }

    void draw(int mouseX, int mouseY) {
        GuiTheme.UiState state;
        if (!enabled) {
            state = GuiTheme.UiState.DISABLED;
        } else if (isInside(mouseX, mouseY, x, y, width, height)) {
            state = GuiTheme.UiState.HOVER;
        } else {
            state = GuiTheme.UiState.NORMAL;
        }
        GuiTheme.drawButtonFrameSafe(x, y, width, height, state);
        owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font),
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font).trimStringToWidth(label + ": " + getSelectedText(), Math.max(10, width - 18)),
                x + 6, y + 6, 0xFFFFFFFF);
        owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font), expanded ? "▲" : "▼", x + width - 10, y + 6, 0xFF9FDFFF);

        if (!expanded) {
            return;
        }

        int menuY = y + height + 2;
        int totalHeight = getExpandedHeight();
        Gui.drawRect(x, menuY, x + width, menuY + totalHeight, 0xEE111A22);
        Gui.drawRect(x, menuY, x + width, menuY + 1, 0xFF6FB8FF);
        Gui.drawRect(x, menuY + totalHeight - 1, x + width, menuY + totalHeight, 0xFF35536C);
        Gui.drawRect(x, menuY, x + 1, menuY + totalHeight, 0xFF35536C);
        Gui.drawRect(x + width - 1, menuY, x + width, menuY + totalHeight, 0xFF35536C);

        for (int i = 0; i < options.length; i++) {
            int itemY = menuY + i * ITEM_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, x, itemY, width, ITEM_HEIGHT);
            boolean selected = i == selectedIndex;
            if (selected || hovered) {
                Gui.drawRect(x + 1, itemY, x + width - 1, itemY + ITEM_HEIGHT,
                        selected ? 0xCC2B5A7C : 0xAA2E4258);
            }
            owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font), options[i], x + 6, itemY + 5, 0xFFFFFFFF);
        }
    }

    boolean handleClick(int mouseX, int mouseY, int mouseButton) {
        if (!enabled || mouseButton != 0) {
            return false;
        }
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            expanded = !expanded;
            return true;
        }
        if (!expanded) {
            return false;
        }

        int menuY = y + height + 2;
        int totalHeight = getExpandedHeight();
        if (isInside(mouseX, mouseY, x, menuY, width, totalHeight)) {
            int index = (mouseY - menuY) / ITEM_HEIGHT;
            if (index >= 0 && index < options.length) {
                selectedIndex = index;
            }
            expanded = false;
            return true;
        }

        expanded = false;
        return false;
    }

    boolean isMouseOver(int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x, y, width, height)) {
            return true;
        }
        return expanded && isInside(mouseX, mouseY, x, y + height + 2, width, getExpandedHeight());
    }

    private int getExpandedHeight() {
        return options.length * ITEM_HEIGHT;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}




