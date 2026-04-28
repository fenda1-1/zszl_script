package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ConditionExpressionEditorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ConditionExpressionParts;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.GroupedVariableSelectorBinding;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ConditionExpressionEditorSupport {
    private ConditionExpressionEditorSupport() {
    }

    static int addEditor(GuiActionEditor editor, int width, int x, int y) {
        ConditionExpressionParts parts = getDraftParts(editor);
        GroupedVariableSelectorBinding variableBinding = ActionVariableBindingSupport
                .createGroupedVariableBinding(editor, "__condition_expression_var", parts.variable);
        String groupDisplay = ActionVariableBindingSupport.findGroupDisplayForActualValue(variableBinding, parts.variable);
        int safeWidth = Math.max(1, width);
        int rowY = y;
        boolean wrap = editor.isCompactParamLayout(safeWidth);
        int groupWidth = Math.max(106, Math.min(126, safeWidth / 3));
        int variableWidth = Math.max(1, safeWidth - groupWidth - GuiActionEditor.PARAM_INLINE_GAP);

        if (wrap) {
            editor.addDropdown("变量分组", variableBinding.groupDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_group"),
                    safeWidth, x, rowY, variableBinding.groupValueMap.keySet().toArray(new String[0]), groupDisplay);
            rowY += 40;
            editor.addDropdown("变量", variableBinding.valueDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_value"),
                    safeWidth, x, rowY,
                    ActionVariableBindingSupport.buildValueOptions(variableBinding, groupDisplay),
                    ActionVariableBindingSupport.findValueDisplayForActualValue(variableBinding, parts.variable));
            rowY += 40;
            editor.addDropdown("运算符", "__ui_condition_expression_operator",
                    I18n.format("gui.path.action_editor.help.expression"),
                    safeWidth, x, rowY, GuiActionEditor.SIMPLE_EXPRESSION_OPERATORS,
                    normalizeOperator(parts.operator));
            rowY += 40;
            editor.addTextField(parts.custom ? "表达式" : "值", "__ui_condition_expression_value1",
                    parts.custom ? I18n.format("gui.path.action_editor.help.expression")
                            : I18n.format("gui.path.action_editor.help.value"),
                    safeWidth, x, rowY, parts.value1);
            rowY += 40;
            if (isRangeOperator(parts.operator)) {
                editor.addTextField("结束值", "__ui_condition_expression_value2",
                        I18n.format("gui.path.action_editor.help.value"),
                        safeWidth, x, rowY, parts.value2);
                rowY += 40;
            }
        } else {
            editor.addDropdown("变量分组", variableBinding.groupDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_group"),
                    groupWidth, x, rowY, variableBinding.groupValueMap.keySet().toArray(new String[0]), groupDisplay);
            editor.addDropdown("变量", variableBinding.valueDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_value"),
                    variableWidth, x + groupWidth + GuiActionEditor.PARAM_INLINE_GAP, rowY,
                    ActionVariableBindingSupport.buildValueOptions(variableBinding, groupDisplay),
                    ActionVariableBindingSupport.findValueDisplayForActualValue(variableBinding, parts.variable));
            rowY += 40;

            int operatorWidth = Math.max(132, Math.min(166, safeWidth / 2));
            int valueWidth = Math.max(1, safeWidth - operatorWidth - GuiActionEditor.PARAM_INLINE_GAP);
            editor.addDropdown("运算符", "__ui_condition_expression_operator",
                    I18n.format("gui.path.action_editor.help.expression"),
                    operatorWidth, x, rowY, GuiActionEditor.SIMPLE_EXPRESSION_OPERATORS,
                    normalizeOperator(parts.operator));
            if (isRangeOperator(parts.operator)) {
                int firstWidth = Math.max(72, (valueWidth - GuiActionEditor.PARAM_INLINE_GAP) / 2);
                int secondWidth = Math.max(72, valueWidth - firstWidth - GuiActionEditor.PARAM_INLINE_GAP);
                editor.addTextField("起始值", "__ui_condition_expression_value1",
                        I18n.format("gui.path.action_editor.help.value"),
                        firstWidth, x + operatorWidth + GuiActionEditor.PARAM_INLINE_GAP, rowY, parts.value1);
                editor.addTextField("结束值", "__ui_condition_expression_value2",
                        I18n.format("gui.path.action_editor.help.value"),
                        secondWidth, x + operatorWidth + GuiActionEditor.PARAM_INLINE_GAP + firstWidth
                                + GuiActionEditor.PARAM_INLINE_GAP,
                        rowY, parts.value2);
            } else {
                editor.addTextField(parts.custom ? "表达式" : "值", "__ui_condition_expression_value1",
                        parts.custom ? I18n.format("gui.path.action_editor.help.expression")
                                : I18n.format("gui.path.action_editor.help.value"),
                        valueWidth, x + operatorWidth + GuiActionEditor.PARAM_INLINE_GAP, rowY, parts.value1);
            }
            rowY += 40;
        }

        editor.conditionExpressionBinding = new ConditionExpressionEditorBinding(variableBinding,
                "__ui_condition_expression_operator",
                "__ui_condition_expression_value1",
                "__ui_condition_expression_value2");
        ActionVariableBindingSupport.syncGroupedVariableValueOptions(editor, variableBinding, parts.variable);
        return rowY - y;
    }

    static ConditionExpressionParts getDraftParts(GuiActionEditor editor) {
        if (editor.currentParams != null && editor.currentParams.has("__draft_condition_expression_operator")) {
            return new ConditionExpressionParts(
                    editor.currentParams.has("__draft_condition_expression_var")
                            ? editor.currentParams.get("__draft_condition_expression_var").getAsString()
                            : "",
                    editor.currentParams.get("__draft_condition_expression_operator").getAsString(),
                    editor.currentParams.has("__draft_condition_expression_value1")
                            ? editor.currentParams.get("__draft_condition_expression_value1").getAsString()
                            : "",
                    editor.currentParams.has("__draft_condition_expression_value2")
                            ? editor.currentParams.get("__draft_condition_expression_value2").getAsString()
                            : "",
                    GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_CUSTOM.equals(
                            editor.currentParams.get("__draft_condition_expression_operator").getAsString()));
        }
        return parseParts(editor.currentParams.has("expression")
                ? editor.currentParams.get("expression").getAsString()
                : "");
    }

    static boolean shouldRefreshLayout(GuiActionEditor editor, String dropdownKey, String oldValue, String newValue) {
        if (dropdownKey == null || Objects.equals(oldValue, newValue) || editor.conditionExpressionBinding == null) {
            return false;
        }
        return dropdownKey.equals(editor.conditionExpressionBinding.operatorDropdownKey);
    }

    static String buildExpressionFromEditor(GuiActionEditor editor) {
        if (editor.conditionExpressionBinding == null) {
            return "";
        }
        String operator = normalizeOperator(
                editor.getDropdownByKey(editor.conditionExpressionBinding.operatorDropdownKey) == null
                        ? ""
                        : editor.getDropdownByKey(editor.conditionExpressionBinding.operatorDropdownKey).getValue());
        String value1 = editor.safe(editor.getFieldByKey(editor.conditionExpressionBinding.valueFieldKey) == null
                ? ""
                : editor.getFieldByKey(editor.conditionExpressionBinding.valueFieldKey).getText()).trim();
        String value2 = editor.safe(editor.getFieldByKey(editor.conditionExpressionBinding.secondValueFieldKey) == null
                ? ""
                : editor.getFieldByKey(editor.conditionExpressionBinding.secondValueFieldKey).getText()).trim();
        if (GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_CUSTOM.equals(operator)) {
            return value1;
        }
        String variable = ActionVariableBindingSupport.resolveGroupedVariableActualValue(editor,
                editor.conditionExpressionBinding.variableBinding);
        if (variable.isEmpty() || value1.isEmpty()) {
            return "";
        }
        String reference = "$" + variable;
        if (isRangeOperator(operator)) {
            if (value2.isEmpty()) {
                return "";
            }
            if (GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_CLOSED.equals(operator)) {
                return reference + " >= " + value1 + " && " + reference + " <= " + value2;
            }
            if (GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_OPEN.equals(operator)) {
                return reference + " > " + value1 + " && " + reference + " < " + value2;
            }
            if (GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_LEFT_CLOSED.equals(operator)) {
                return reference + " >= " + value1 + " && " + reference + " < " + value2;
            }
            return reference + " > " + value1 + " && " + reference + " <= " + value2;
        }
        return reference + " " + operator + " " + value1;
    }

    static boolean isRangeOperator(String operatorDisplay) {
        return GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_CLOSED.equals(operatorDisplay)
                || GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_OPEN.equals(operatorDisplay)
                || GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_LEFT_CLOSED.equals(operatorDisplay)
                || GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_RIGHT_CLOSED.equals(operatorDisplay);
    }

    static String normalizeOperator(String operatorDisplay) {
        String normalized = safe(operatorDisplay).trim();
        if (normalized.isEmpty()) {
            return "==";
        }
        for (String option : GuiActionEditor.SIMPLE_EXPRESSION_OPERATORS) {
            if (option.equals(normalized)) {
                return option;
            }
        }
        return GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_CUSTOM;
    }

    private static ConditionExpressionParts parseParts(String expression) {
        String text = safe(expression).trim();
        if (text.isEmpty()) {
            return new ConditionExpressionParts("", "==", "", "", false);
        }

        String variablePattern = "\\$?([A-Za-z0-9_:.]+)";
        ConditionExpressionParts parts = tryParseRangeExpression(text, variablePattern,
                GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_CLOSED, ">=", "<=");
        if (parts != null) {
            return parts;
        }
        parts = tryParseRangeExpression(text, variablePattern,
                GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_OPEN, ">", "<");
        if (parts != null) {
            return parts;
        }
        parts = tryParseRangeExpression(text, variablePattern,
                GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_LEFT_CLOSED, ">=", "<");
        if (parts != null) {
            return parts;
        }
        parts = tryParseRangeExpression(text, variablePattern,
                GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_RANGE_RIGHT_CLOSED, ">", "<=");
        if (parts != null) {
            return parts;
        }

        Matcher simpleMatcher = Pattern.compile("^" + variablePattern + "\\s*(==|!=|>=|<=|>|<)\\s*(.+)$")
                .matcher(text);
        if (simpleMatcher.matches()) {
            return new ConditionExpressionParts(simpleMatcher.group(1).trim(),
                    simpleMatcher.group(2).trim(),
                    simpleMatcher.group(3).trim(), "", false);
        }

        return new ConditionExpressionParts("", GuiActionEditor.CONDITION_EXPRESSION_OPERATOR_CUSTOM, text, "", true);
    }

    private static ConditionExpressionParts tryParseRangeExpression(String text, String variablePattern,
            String displayOperator, String leftOperator, String rightOperator) {
        Matcher matcher = Pattern.compile("^" + variablePattern + "\\s*" + Pattern.quote(leftOperator)
                + "\\s*(.+?)\\s*&&\\s*\\$?\\1\\s*" + Pattern.quote(rightOperator) + "\\s*(.+)$")
                .matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        return new ConditionExpressionParts(matcher.group(1).trim(), displayOperator,
                matcher.group(2).trim(), matcher.group(3).trim(), false);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

