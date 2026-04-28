package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class ConditionExpressionEditorBinding {
    public final GroupedVariableSelectorBinding variableBinding;
    public final String operatorDropdownKey;
    public final String valueFieldKey;
    public final String secondValueFieldKey;

    public ConditionExpressionEditorBinding(GroupedVariableSelectorBinding variableBinding,
            String operatorDropdownKey,
            String valueFieldKey,
            String secondValueFieldKey) {
        this.variableBinding = variableBinding;
        this.operatorDropdownKey = operatorDropdownKey;
        this.valueFieldKey = valueFieldKey;
        this.secondValueFieldKey = secondValueFieldKey;
    }
}
