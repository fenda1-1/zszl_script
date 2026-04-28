package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model;

public class ConditionExpressionParts {
    public final String variable;
    public final String operator;
    public final String value1;
    public final String value2;
    public final boolean custom;

    public ConditionExpressionParts(String variable, String operator, String value1, String value2,
            boolean custom) {
        this.variable = variable == null ? "" : variable;
        this.operator = operator == null ? "" : operator;
        this.value1 = value1 == null ? "" : value1;
        this.value2 = value2 == null ? "" : value2;
        this.custom = custom;
    }
}
