package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class ExpressionEditorBinding {
    public final String paramKey;
    public final String title;
    public final String helpText;

    public ExpressionEditorBinding(String paramKey, String title, String helpText) {
        this.paramKey = paramKey == null ? "" : paramKey;
        this.title = title == null ? "" : title;
        this.helpText = helpText == null ? "" : helpText;
    }
}
