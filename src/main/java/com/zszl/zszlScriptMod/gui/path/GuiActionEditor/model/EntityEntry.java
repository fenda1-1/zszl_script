package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class EntityEntry {
    public final String label;
    public final String pos;
    public final String name;
    public final double distSq;

    public EntityEntry(String label, String pos, String name, double distSq) {
        this.label = label;
        this.pos = pos;
        this.name = name;
        this.distSq = distSq;
    }
}
