package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class ScopedVariableEditorBinding {
    public final String actualParamKey;
    public final String scopeDropdownKey;
    public final String nameFieldKey;

    public ScopedVariableEditorBinding(String actualParamKey, String scopeDropdownKey, String nameFieldKey) {
        this.actualParamKey = actualParamKey;
        this.scopeDropdownKey = scopeDropdownKey;
        this.nameFieldKey = nameFieldKey;
    }
}
