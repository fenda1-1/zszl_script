package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class BlockEntry {
    public final String label;
    public final String pos;
    public final double distSq;

    public BlockEntry(String label, String pos, double distSq) {
        this.label = label;
        this.pos = pos;
        this.distSq = distSq;
    }
}
