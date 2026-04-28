package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;

class EnumDropdown extends Gui {
    private static final int MAX_VISIBLE_OPTIONS = 8;

    int x;
    int y;
    int width;
    int height;
    String[] options;
    int selectedIndex = 0;
    boolean expanded = false;
    private int scrollOffset = 0;
    private boolean enabled = true;
    private int viewportTop = 0;
    private int viewportBottom = Integer.MAX_VALUE;

    EnumDropdown(int x, int y, int width, int height, String[] options) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.options = options == null ? new String[0] : options;
    }

    void setViewportBounds(int top, int bottom) {
        this.viewportTop = top;
        this.viewportBottom = bottom;
    }

    void drawMain(int mouseX, int mouseY) {
        FontRenderer fontRenderer = new FontRenderer(Minecraft.getInstance().font);
        boolean hoverMain = enabled && isMouseInside(mouseX, mouseY, x, y, width, height);
        int bg = !enabled ? 0xAA111920 : (hoverMain ? 0xCC203146 : 0xCC152433);
        int border = !enabled ? 0xFF465566 : (expanded ? 0xFF76D1FF : (hoverMain ? 0xFF4FA6D9 : 0xFF3F6A8C));

        Gui.drawRect(x, y, x + width, y + height, bg);
        Gui.drawRect(x, y, x + width + 1, y + 1, border);
        Gui.drawRect(x, y + height, x + width + 1, y + height + 1, border);
        Gui.drawRect(x, y, x + 1, y + height + 1, border);
        Gui.drawRect(x + width, y, x + width + 1, y + height + 1, border);

        String value = getValue();
        int textWidth = Math.max(16, width - 18);
        String mainText = fontRenderer.trimStringToWidth(value, textWidth);
        fontRenderer.drawString(mainText, x + 5, y + 4, enabled ? 0xFFEAF7FF : 0xFF9AA7B6);
        fontRenderer.drawString(expanded ? "▲" : "▼", x + width - 10, y + 4, enabled ? 0xFF9FDFFF : 0xFF758596);
    }

    void drawExpanded(int mouseX, int mouseY) {
        FontRenderer fontRenderer = new FontRenderer(Minecraft.getInstance().font);
        if (!enabled || !expanded || options == null || options.length == 0) {
            return;
        }

        int visibleCount = getVisibleOptionCount();
        int startIndex = getClampedScrollOffset();
        int topY = getExpandedTopY();
        boolean expandUp = shouldExpandUpward();
        int textWidth = Math.max(16, width - (needsScrollbar(visibleCount) ? 14 : 8));

        for (int row = 0; row < visibleCount; row++) {
            int optionIndex = startIndex + row;
            if (optionIndex >= options.length) {
                break;
            }

            int oy = topY + row * height;
            boolean hoverItem = isMouseInside(mouseX, mouseY, x, oy, width, height);
            boolean selected = optionIndex == selectedIndex;
            int itemBg = selected ? 0xEE2B5A7C : (hoverItem ? 0xCC29455E : 0xCC1B2D3D);
            int itemBorder = hoverItem ? 0xFF7ED0FF : 0xFF3B6B8A;

            Gui.drawRect(x, oy, x + width, oy + height, itemBg);
            Gui.drawRect(x, oy, x + width + 1, oy + 1, itemBorder);
            Gui.drawRect(x, oy + height, x + width + 1, oy + height + 1, itemBorder);
            Gui.drawRect(x, oy, x + 1, oy + height + 1, itemBorder);
            Gui.drawRect(x + width, oy, x + width + 1, oy + height + 1, itemBorder);
            String optionText = options[optionIndex] == null ? "" : options[optionIndex];
            fontRenderer.drawString(fontRenderer.trimStringToWidth(optionText, textWidth), x + 5, oy + 4,
                    0xFFFFFFFF);
        }

        if (visibleCount > 0 && needsScrollbar(visibleCount)) {
            int totalHeight = visibleCount * height;
            int barX = x + width - 6;
            int thumbHeight = Math.max(10, (int) ((float) visibleCount / options.length * totalHeight));
            int maxScroll = Math.max(1, options.length - visibleCount);
            int thumbTravel = Math.max(1, totalHeight - thumbHeight);
            int thumbY = topY + (int) ((float) getClampedScrollOffset() / maxScroll * thumbTravel);

            Gui.drawRect(barX, topY, barX + 4, topY + totalHeight, 0xAA0E1822);
            Gui.drawRect(barX, thumbY, barX + 4, thumbY + thumbHeight, 0xFF7ED0FF);
        }

        if (expandUp) {
            Gui.drawRect(x, y, x + width + 1, y + 1, 0xFF76D1FF);
        } else {
            Gui.drawRect(x, y + height, x + width + 1, y + height + 1, 0xFF76D1FF);
        }
    }

    boolean handleClick(int mouseX, int mouseY, int mouseButton) {
        if (!enabled) {
            expanded = false;
            return false;
        }
        if (mouseButton != 0) {
            return false;
        }

        if (isMouseInside(mouseX, mouseY, x, y, width, height)) {
            expanded = !expanded;
            if (expanded) {
                ensureSelectionVisible();
            }
            return true;
        }

        if (!expanded) {
            return false;
        }

        int visibleCount = getVisibleOptionCount();
        int startIndex = getClampedScrollOffset();
        int topY = getExpandedTopY();
        for (int row = 0; row < visibleCount; row++) {
            int optionIndex = startIndex + row;
            int oy = topY + row * height;
            if (optionIndex < options.length && isMouseInside(mouseX, mouseY, x, oy, width, height)) {
                selectedIndex = optionIndex;
                expanded = false;
                ensureSelectionVisible();
                return true;
            }
        }
        return false;
    }

    boolean handleMouseWheel(int mouseX, int mouseY, int dWheel) {
        if (!enabled || !expanded || options == null || options.length == 0) {
            return false;
        }
        int visibleCount = getVisibleOptionCount();
        if (!needsScrollbar(visibleCount) || !isMouseInsideExpanded(mouseX, mouseY)) {
            return false;
        }

        if (dWheel > 0) {
            scrollOffset = Math.max(0, getClampedScrollOffset() - 1);
        } else if (dWheel < 0) {
            scrollOffset = Math.min(getMaxScrollOffset(visibleCount), getClampedScrollOffset() + 1);
        }
        return true;
    }

    String getValue() {
        if (options == null || options.length == 0) {
            return "";
        }
        if (selectedIndex < 0 || selectedIndex >= options.length) {
            selectedIndex = 0;
        }
        return options[selectedIndex] == null ? "" : options[selectedIndex];
    }

    void setOptions(String[] newOptions) {
        this.options = newOptions == null ? new String[0] : newOptions;
        selectedIndex = 0;
        scrollOffset = 0;
        expanded = false;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.expanded = false;
        }
    }

    void setValue(String value) {
        String normalized = value == null ? "" : value.trim();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(normalized)) {
                selectedIndex = i;
                ensureSelectionVisible();
                return;
            }
        }
        selectedIndex = 0;
        ensureSelectionVisible();
    }

    void collapse() {
        expanded = false;
    }

    private int getVisibleOptionCount() {
        if (options == null || options.length == 0) {
            return 0;
        }
        int maxBelow = Math.max(1, (viewportBottom - (y + height)) / height);
        int maxAbove = Math.max(1, (y - viewportTop) / height);
        int availableRows = shouldExpandUpward() ? maxAbove : maxBelow;
        return Math.max(1, Math.min(options.length, Math.min(MAX_VISIBLE_OPTIONS, availableRows)));
    }

    private boolean shouldExpandUpward() {
        int belowSpace = Math.max(0, viewportBottom - (y + height));
        int aboveSpace = Math.max(0, y - viewportTop);
        int desiredHeight = Math.min(options == null ? 0 : options.length, MAX_VISIBLE_OPTIONS) * height;
        return belowSpace < desiredHeight && aboveSpace > belowSpace;
    }

    private int getExpandedTopY() {
        int visibleCount = getVisibleOptionCount();
        return shouldExpandUpward() ? y - visibleCount * height : y + height;
    }

    private int getClampedScrollOffset() {
        return Math.max(0, Math.min(scrollOffset, getMaxScrollOffset(getVisibleOptionCount())));
    }

    private int getMaxScrollOffset(int visibleCount) {
        return Math.max(0, (options == null ? 0 : options.length) - visibleCount);
    }

    private boolean needsScrollbar(int visibleCount) {
        return options != null && options.length > visibleCount;
    }

    private void ensureSelectionVisible() {
        int visibleCount = getVisibleOptionCount();
        if (visibleCount <= 0) {
            scrollOffset = 0;
            return;
        }
        if (selectedIndex < getClampedScrollOffset()) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= getClampedScrollOffset() + visibleCount) {
            scrollOffset = selectedIndex - visibleCount + 1;
        } else {
            scrollOffset = getClampedScrollOffset();
        }
    }

    boolean isMouseInsideExpanded(int mx, int my) {
        if (!expanded) {
            return false;
        }
        int visibleCount = getVisibleOptionCount();
        int topY = getExpandedTopY();
        return isMouseInside(mx, my, x, topY, width, visibleCount * height);
    }

    private boolean isMouseInside(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }
}

