package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class IndexedHitRegion {
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final int index;

    public IndexedHitRegion(int x, int y, int width, int height, int index) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.index = index;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
