package com.zszl.zszlScriptMod.otherfeatures.gui.world;

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

    void setSelectedIndex(int selectedIndex) {
        if (this.options.length <= 0) {
            this.selectedIndex = 0;
            return;
        }
        this.selectedIndex = Math.max(0, Math.min(selectedIndex, this.options.length - 1));
    }

    int getSelectedIndex() {
        return this.selectedIndex;
    }

    String getSelectedText() {
        return this.options.length <= 0 ? "" : this.options[this.selectedIndex];
    }

    int getVisibleMenuHeight() {
        return this.expanded ? getExpandedHeight() : 0;
    }

    void collapse() {
        this.expanded = false;
    }

    void draw(int mouseX, int mouseY) {
        GuiTheme.UiState state;
        if (!this.enabled) {
            state = GuiTheme.UiState.DISABLED;
        } else if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            state = GuiTheme.UiState.HOVER;
        } else {
            state = GuiTheme.UiState.NORMAL;
        }
        GuiTheme.drawButtonFrameSafe(this.x, this.y, this.width, this.height, state);
        this.owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font),
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font).trimStringToWidth(this.label + ": " + getSelectedText(), Math.max(10, this.width - 18)),
                this.x + 6, this.y + 6, 0xFFFFFFFF);
        this.owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font), this.expanded ? "▲" : "▼", this.x + this.width - 10, this.y + 6,
                0xFF9FDFFF);

        if (!this.expanded) {
            return;
        }

        int menuY = this.y + this.height + 2;
        int totalHeight = getExpandedHeight();
        Gui.drawRect(this.x, menuY, this.x + this.width, menuY + totalHeight, 0xEE111A22);
        Gui.drawRect(this.x, menuY, this.x + this.width, menuY + 1, 0xFF6FB8FF);
        Gui.drawRect(this.x, menuY + totalHeight - 1, this.x + this.width, menuY + totalHeight, 0xFF35536C);
        Gui.drawRect(this.x, menuY, this.x + 1, menuY + totalHeight, 0xFF35536C);
        Gui.drawRect(this.x + this.width - 1, menuY, this.x + this.width, menuY + totalHeight, 0xFF35536C);

        for (int i = 0; i < this.options.length; i++) {
            int itemY = menuY + i * ITEM_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, this.x, itemY, this.width, ITEM_HEIGHT);
            boolean selected = i == this.selectedIndex;
            if (selected || hovered) {
                Gui.drawRect(this.x + 1, itemY, this.x + this.width - 1, itemY + ITEM_HEIGHT,
                        selected ? 0xCC2B5A7C : 0xAA2E4258);
            }
            this.owner.drawString(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(net.minecraft.client.Minecraft.getInstance().font), this.options[i], this.x + 6, itemY + 5, 0xFFFFFFFF);
        }
    }

    boolean handleClick(int mouseX, int mouseY, int mouseButton) {
        if (!this.enabled || mouseButton != 0) {
            return false;
        }
        if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            this.expanded = !this.expanded;
            return true;
        }
        if (!this.expanded) {
            return false;
        }

        int menuY = this.y + this.height + 2;
        int totalHeight = getExpandedHeight();
        if (isInside(mouseX, mouseY, this.x, menuY, this.width, totalHeight)) {
            int index = (mouseY - menuY) / ITEM_HEIGHT;
            if (index >= 0 && index < this.options.length) {
                this.selectedIndex = index;
            }
            this.expanded = false;
            return true;
        }

        this.expanded = false;
        return false;
    }

    boolean isMouseOver(int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return true;
        }
        return this.expanded && isInside(mouseX, mouseY, this.x, this.y + this.height + 2, this.width, getExpandedHeight());
    }

    private int getExpandedHeight() {
        return this.options.length * ITEM_HEIGHT;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}





