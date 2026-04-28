package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

import java.util.ArrayList;
import java.util.List;

public class ExpressionTemplateLayoutEntry {
    public final ExpressionTemplateCard card;
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final List<String> exampleLines;

    public ExpressionTemplateLayoutEntry(ExpressionTemplateCard card, int x, int y, int width, int height,
            List<String> exampleLines) {
        this.card = card;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.exampleLines = exampleLines == null ? new ArrayList<String>() : exampleLines;
    }
}
