package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionEditorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.GroupedVariableSelectorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ScopedVariableEditorBinding;
import com.zszl.zszlScriptMod.path.ActionVariableRegistry;
import com.zszl.zszlScriptMod.path.PathSequenceManager;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util.ActionEditorDisplayConverters.*;

final class ActionVariableBindingSupport {
    private ActionVariableBindingSupport() {
    }

    static int addScopedVariableEditor(GuiActionEditor editor, String label, String actualParamKey, String helpText,
            int width, int x, int y, String defaultBaseName) {
        String currentValue = editor.currentParams.has(actualParamKey)
                ? editor.currentParams.get(actualParamKey).getAsString()
                : "";
        String scopeKey = ActionVariableRegistry.extractScopeKey(currentValue);
        String baseName = ActionVariableRegistry.extractBaseName(currentValue);
        if (baseName.isEmpty()) {
            baseName = defaultBaseName == null ? "" : defaultBaseName;
        }

        int safeWidth = Math.max(1, width);
        int scopeWidth = Math.max(96, Math.min(118, safeWidth / 3));
        int nameWidth = Math.max(1, safeWidth - scopeWidth - GuiActionEditor.PARAM_INLINE_GAP);
        String scopeDropdownKey = "__ui_scope_" + actualParamKey;
        String nameFieldKey = "__ui_name_" + actualParamKey;
        boolean wrap = editor.isCompactParamLayout(safeWidth);

        if (wrap) {
            editor.addDropdown("作用域", scopeDropdownKey,
                    I18n.format("gui.path.action_editor.help.variable_scope"),
                    safeWidth, x, y,
                    GuiActionEditor.VARIABLE_SCOPE_DISPLAY_OPTIONS, scopeKeyToDisplay(scopeKey));
            editor.addTextField(label, nameFieldKey,
                    I18n.format("gui.path.action_editor.help.variable_name_scoped"),
                    safeWidth, x, y + 40, baseName);
        } else {
            editor.addDropdown("作用域", scopeDropdownKey,
                    I18n.format("gui.path.action_editor.help.variable_scope"),
                    scopeWidth, x, y,
                    GuiActionEditor.VARIABLE_SCOPE_DISPLAY_OPTIONS, scopeKeyToDisplay(scopeKey));
            editor.addTextField(label, nameFieldKey,
                    I18n.format("gui.path.action_editor.help.variable_name_scoped"),
                    nameWidth, x + scopeWidth + GuiActionEditor.PARAM_INLINE_GAP, y, baseName);
        }

        editor.scopedVariableBindings.put(actualParamKey,
                new ScopedVariableEditorBinding(actualParamKey, scopeDropdownKey, nameFieldKey));
        return wrap ? 80 : 40;
    }

    static int addGroupedRuntimeVariableSelector(GuiActionEditor editor, String label, String actualParamKey,
            String helpText, int width, int x, int y) {
        String currentValue = editor.currentParams.has(actualParamKey)
                ? editor.currentParams.get(actualParamKey).getAsString()
                : "";
        GroupedVariableSelectorBinding binding = createGroupedVariableBinding(editor, actualParamKey, currentValue);

        int safeWidth = Math.max(1, width);
        int groupWidth = Math.max(106, Math.min(126, safeWidth / 3));
        int valueWidth = Math.max(1, safeWidth - groupWidth - GuiActionEditor.PARAM_INLINE_GAP);
        String[] groups = binding.groupValueMap.keySet().toArray(new String[0]);
        boolean wrap = editor.isCompactParamLayout(safeWidth);

        if (wrap) {
            editor.addDropdown("变量分组", binding.groupDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_group"),
                    safeWidth, x, y, groups,
                    findGroupDisplayForActualValue(binding, currentValue));
            editor.addDropdown(label, binding.valueDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_value"),
                    safeWidth, x, y + 40,
                    buildValueOptions(binding, findGroupDisplayForActualValue(binding, currentValue)),
                    findValueDisplayForActualValue(binding, currentValue));
        } else {
            editor.addDropdown("变量分组", binding.groupDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_group"),
                    groupWidth, x, y, groups,
                    findGroupDisplayForActualValue(binding, currentValue));
            editor.addDropdown(label, binding.valueDropdownKey,
                    I18n.format("gui.path.action_editor.help.source_var_value"),
                    valueWidth, x + groupWidth + GuiActionEditor.PARAM_INLINE_GAP, y,
                    buildValueOptions(binding, findGroupDisplayForActualValue(binding, currentValue)),
                    findValueDisplayForActualValue(binding, currentValue));
        }
        editor.groupedVariableBindings.put(actualParamKey, binding);
        syncGroupedVariableValueOptions(editor, binding, currentValue);
        return wrap ? 80 : 40;
    }

    static int addExpressionTemplateEditor(GuiActionEditor editor, String label, String actualParamKey, int width,
            int x, int y) {
        String currentValue = editor.currentParams.has(actualParamKey)
                ? editor.currentParams.get(actualParamKey).getAsString()
                : "";
        editor.addTextField(label, actualParamKey,
                I18n.format("gui.path.action_editor.help.set_var_expression"),
                Math.max(1, width), x, y, editor.safe(currentValue).trim());
        editor.expressionEditorBindings.put(actualParamKey,
                new ExpressionEditorBinding(actualParamKey, label,
                        I18n.format("gui.path.action_editor.help.set_var_expression")));
        return 40;
    }

    static boolean isExpressionEditorFieldKey(GuiActionEditor editor, String key) {
        return key != null && editor.expressionEditorBindings.containsKey(key);
    }

    static void drawExpressionEditorButton(GuiActionEditor editor, GuiTextField field, int mouseX, int mouseY) {
        if (field == null) {
            return;
        }
        boolean hovered = editor.isPointInside(mouseX, mouseY, field.x, field.y, field.width, field.height);
        GuiTheme.drawInputFrameSafe(field.x - 1, field.y - 1, field.width + 2, field.height + 2, hovered, true);
        Gui.drawRect(field.x, field.y, field.x + field.width, field.y + field.height,
                hovered ? 0x55203038 : 0x44202A34);
        String value = editor.safe(field.getText()).trim();
        String display = value.isEmpty() ? "点击打开表达式编辑器，搜索模板后再编辑" : value;
        int color = value.isEmpty() ? 0xFF8FA5BA : 0xFFE7F1FB;
        editor.getEditorFontRenderer().drawString(
                editor.getEditorFontRenderer().trimStringToWidth(display, Math.max(20, field.width - 40)),
                field.x + 6, field.y + 6, color);
        editor.getEditorFontRenderer().drawString("编辑", field.x + field.width - 22, field.y + 6, 0xFF7AD9FF);
    }

    static ExpressionEditorBinding getExpressionEditorBindingAt(GuiActionEditor editor, int mouseX, int mouseY) {
        for (ExpressionEditorBinding binding : editor.expressionEditorBindings.values()) {
            GuiTextField field = editor.getFieldByKey(binding.paramKey);
            if (field != null && editor.isPointInside(mouseX, mouseY, field.x, field.y, field.width, field.height)) {
                return binding;
            }
        }
        return null;
    }

    static GroupedVariableSelectorBinding createGroupedVariableBinding(GuiActionEditor editor, String actualParamKey,
            String currentValue) {
        boolean allowEmptySelection = "fromVar".equals(actualParamKey);
        LinkedHashMap<String, LinkedHashMap<String, String>> groups = new LinkedHashMap<>();
        groups.put("内置变量(builtin)", new LinkedHashMap<String, String>());
        groups.put(ActionVariableRegistry.scopeKeyToDisplay("global"), new LinkedHashMap<String, String>());
        groups.put(ActionVariableRegistry.scopeKeyToDisplay("sequence"), new LinkedHashMap<String, String>());
        groups.put(ActionVariableRegistry.scopeKeyToDisplay("local"), new LinkedHashMap<String, String>());
        groups.put(ActionVariableRegistry.scopeKeyToDisplay("temp"), new LinkedHashMap<String, String>());
        groups.put("数据采集变量(capture)", new LinkedHashMap<String, String>());

        for (String option : editor.availableRuntimeVariableOptions) {
            String value = editor.safe(option).trim();
            if (value.isEmpty() || "(选择变量)".equals(value)) {
                continue;
            }
            if ("trigger".equalsIgnoreCase(value) || "triggerType".equalsIgnoreCase(value)) {
                groups.get("内置变量(builtin)").put(value, value);
            }
        }

        for (ActionVariableRegistry.VariableEntry entry : ActionVariableRegistry
                .collectVariables(PathSequenceManager.getAllSequences())) {
            if (entry == null) {
                continue;
            }
            String actualValue = editor.safe(entry.getVariableName()).trim();
            if (actualValue.isEmpty()) {
                continue;
            }
            String scopeGroup = ActionVariableRegistry.scopeKeyToDisplay(entry.getScopeKey());
            addVariableOption(editor, groups.get(scopeGroup), entry.getBaseVariableName(), actualValue);
            if (ActionVariableRegistry.isCaptureVariable(entry)) {
                addVariableOption(editor, groups.get("数据采集变量(capture)"),
                        entry.getBaseVariableName() + " ["
                                + ActionVariableRegistry.normalizeScopeKey(entry.getScopeKey()) + "]",
                        actualValue);
            }
        }

        if (currentValue != null && !currentValue.trim().isEmpty()
                && !containsActualValue(groups, currentValue.trim())) {
            LinkedHashMap<String, String> currentGroup = groups.get(ActionVariableRegistry.scopeKeyToDisplay("sequence"));
            if (currentGroup == null) {
                currentGroup = new LinkedHashMap<String, String>();
                groups.put(ActionVariableRegistry.scopeKeyToDisplay("sequence"), currentGroup);
            }
            currentGroup.put(ActionVariableRegistry.extractBaseName(currentValue.trim()) + " (当前值)",
                    currentValue.trim());
        }

        removeEmptyVariableGroups(groups);
        if (groups.isEmpty()) {
            groups.put("内置变量(builtin)", new LinkedHashMap<String, String>());
        }

        if (allowEmptySelection) {
            for (Map.Entry<String, LinkedHashMap<String, String>> entry : groups.entrySet()) {
                LinkedHashMap<String, String> values = entry.getValue();
                LinkedHashMap<String, String> withClear = new LinkedHashMap<String, String>();
                withClear.put(GuiActionEditor.VARIABLE_SELECTION_CLEAR, "");
                if (values != null) {
                    withClear.putAll(values);
                }
                entry.setValue(withClear);
            }
        }

        return new GroupedVariableSelectorBinding(actualParamKey,
                "__ui_group_" + actualParamKey,
                "__ui_value_" + actualParamKey,
                groups,
                allowEmptySelection);
    }

    static void syncGroupedVariableValueOptions(GuiActionEditor editor, GroupedVariableSelectorBinding binding,
            String preferredActualValue) {
        if (binding == null) {
            return;
        }
        EnumDropdown groupDropdown = editor.getDropdownByKey(binding.groupDropdownKey);
        EnumDropdown valueDropdown = editor.getDropdownByKey(binding.valueDropdownKey);
        if (groupDropdown == null || valueDropdown == null) {
            return;
        }
        String groupDisplay = groupDropdown.getValue();
        String[] options = buildValueOptions(binding, groupDisplay);
        valueDropdown.setOptions(options);
        valueDropdown.setValue(findValueDisplayForActualValue(binding, preferredActualValue));
    }

    static String[] buildValueOptions(GroupedVariableSelectorBinding binding, String groupDisplay) {
        LinkedHashMap<String, String> values = binding == null ? null : binding.groupValueMap.get(groupDisplay);
        if (values == null || values.isEmpty()) {
            return new String[] { "(无可用变量)" };
        }
        return values.keySet().toArray(new String[0]);
    }

    static String findGroupDisplayForActualValue(GroupedVariableSelectorBinding binding, String actualValue) {
        if (binding == null || binding.groupValueMap.isEmpty()) {
            return "";
        }
        String target = safe(actualValue).trim();
        for (Map.Entry<String, LinkedHashMap<String, String>> entry : binding.groupValueMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().containsValue(target)) {
                return entry.getKey();
            }
        }
        return binding.groupValueMap.keySet().iterator().next();
    }

    static String findValueDisplayForActualValue(GroupedVariableSelectorBinding binding, String actualValue) {
        if (binding == null) {
            return "";
        }
        String target = safe(actualValue).trim();
        if (target.isEmpty() && binding.allowEmptySelection) {
            return GuiActionEditor.VARIABLE_SELECTION_CLEAR;
        }
        String groupDisplay = findGroupDisplayForActualValue(binding, target);
        LinkedHashMap<String, String> values = binding.groupValueMap.get(groupDisplay);
        if (values == null || values.isEmpty()) {
            return binding.allowEmptySelection ? GuiActionEditor.VARIABLE_SELECTION_CLEAR : "(无可用变量)";
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (target.equalsIgnoreCase(safe(entry.getValue()).trim())) {
                return entry.getKey();
            }
        }
        return values.keySet().iterator().next();
    }

    static String resolveGroupedVariableActualValue(GuiActionEditor editor, GroupedVariableSelectorBinding binding) {
        if (binding == null) {
            return "";
        }
        EnumDropdown groupDropdown = editor.getDropdownByKey(binding.groupDropdownKey);
        EnumDropdown valueDropdown = editor.getDropdownByKey(binding.valueDropdownKey);
        if (groupDropdown == null || valueDropdown == null) {
            return "";
        }
        LinkedHashMap<String, String> values = binding.groupValueMap.get(groupDropdown.getValue());
        if (values == null || values.isEmpty()) {
            return "";
        }
        String actual = values.get(valueDropdown.getValue());
        return actual == null ? "" : actual.trim();
    }

    static String resolveScopedVariableActualValue(GuiActionEditor editor, ScopedVariableEditorBinding binding) {
        if (binding == null) {
            return "";
        }
        EnumDropdown scopeDropdown = editor.getDropdownByKey(binding.scopeDropdownKey);
        GuiTextField nameField = editor.getFieldByKey(binding.nameFieldKey);
        String baseName = nameField == null ? "" : editor.safe(nameField.getText()).trim();
        if (baseName.isEmpty()) {
            return "";
        }
        String scopeKey = displayToScopeKey(scopeDropdown == null ? "" : scopeDropdown.getValue());
        return ActionVariableRegistry.buildScopedVariableName(scopeKey, baseName);
    }

    static LinkedHashMap<String, String> buildSetVarExpressionTemplates() {
        LinkedHashMap<String, String> templates = new LinkedHashMap<String, String>();
        templates.put(GuiActionEditor.SET_VAR_EXPRESSION_TEMPLATE_CUSTOM, "");
        templates.put("当前变量 += 1", "+= 1");
        templates.put("当前变量 -= 1", "-= 1");
        templates.put("当前变量 *= 2", "*= 2");
        templates.put("当前变量 /= 2", "/= 2");
        templates.put("随机小数 0~1", "random()");
        templates.put("随机整数 1~100", "random(1, 100)");
        templates.put("随机浮点 1.5~3.0", "randomFloat(1.5, 3.0)");
        templates.put("25% 概率布尔", "random() < 0.25");
        templates.put("数值大于阈值", "global.money > 100");
        templates.put("普通加法", "global.money + 1");
        templates.put("普通减法", "global.money - 1");
        templates.put("普通乘法", "global.money * 2");
        templates.put("普通除法", "global.money / 2");
        templates.put("布尔状态判断", "sequence.step_done == true");
        templates.put("区间判断", "local.loop_index >= 1 && local.loop_index <= 3");
        templates.put("三元条件", "temp.current_match == \"boss\" ? 1 : 0");
        templates.put("空值回退", "global.money ?? 0");
        templates.put("列表包含", "contains(sequence.targets, \"boss\")");
        templates.put("求和统计", "sum(sequence.damage_list)");
        templates.put("文本包含", "contains(temp.current_match, \"目标文本\")");
        templates.put("数据存在", "exists(global.money)");
        templates.put("列表非空", "len(sequence.packet_list) > 0");
        templates.put("忽略大小写相等", "equalsignorecase(temp.current_match, \"boss\")");
        return templates;
    }

    static String matchExpressionTemplateDisplay(LinkedHashMap<String, String> templates, String actualValue) {
        String target = safe(actualValue).trim();
        if (target.isEmpty()) {
            return GuiActionEditor.SET_VAR_EXPRESSION_TEMPLATE_CUSTOM;
        }
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            if (target.equalsIgnoreCase(safe(entry.getValue()).trim())) {
                return entry.getKey();
            }
        }
        return GuiActionEditor.SET_VAR_EXPRESSION_TEMPLATE_CUSTOM;
    }

    static String scopeKeyToDisplay(String scopeKey) {
        return ActionVariableRegistry.scopeKeyToDisplay(scopeKey);
    }

    static String displayToScopeKey(String display) {
        String text = safe(display).trim();
        if (text.startsWith("全局变量")) {
            return "global";
        }
        if (text.startsWith("局部变量")) {
            return "local";
        }
        if (text.startsWith("临时变量")) {
            return "temp";
        }
        return "sequence";
    }

    static void onVariableSelectorDropdownChanged(GuiActionEditor editor, String dropdownKey, String oldValue,
            String newValue) {
        if (dropdownKey == null || java.util.Objects.equals(oldValue, newValue)) {
            return;
        }
        LinkedHashMap<String, String> templates = editor.expressionTemplateBindings.get(dropdownKey);
        if (templates != null) {
            if (!GuiActionEditor.SET_VAR_EXPRESSION_TEMPLATE_CUSTOM.equals(newValue)) {
                GuiTextField expressionField = editor.getFieldByKey("expression");
                String sample = templates.get(newValue);
                if (expressionField != null && sample != null && !sample.trim().isEmpty()) {
                    expressionField.setText(sample);
                }
            }
            return;
        }
        for (GroupedVariableSelectorBinding binding : editor.groupedVariableBindings.values()) {
            if (binding != null && dropdownKey.equals(binding.groupDropdownKey)) {
                syncGroupedVariableValueOptions(editor, binding, "");
                break;
            }
        }
        if (editor.conditionExpressionBinding != null
                && editor.conditionExpressionBinding.variableBinding != null
                && dropdownKey.equals(editor.conditionExpressionBinding.variableBinding.groupDropdownKey)) {
            syncGroupedVariableValueOptions(editor, editor.conditionExpressionBinding.variableBinding, "");
        }
    }

    static void applyResolvedBindingsToParams(GuiActionEditor editor, JsonObject target) {
        for (ScopedVariableEditorBinding binding : editor.scopedVariableBindings.values()) {
            target.remove(binding.actualParamKey);
            String actual = resolveScopedVariableActualValue(editor, binding);
            if (!actual.isEmpty()) {
                target.addProperty(binding.actualParamKey, actual);
            }
        }
        for (GroupedVariableSelectorBinding binding : editor.groupedVariableBindings.values()) {
            target.remove(binding.actualParamKey);
            String actual = resolveGroupedVariableActualValue(editor, binding);
            if (!actual.isEmpty()) {
                target.addProperty(binding.actualParamKey, actual);
            }
        }
    }

    static void captureCurrentEditorDraftParams(GuiActionEditor editor) {
        JsonObject draft = editor.currentParams == null
                ? new JsonObject()
                : new JsonParser().parse(editor.currentParams.toString()).getAsJsonObject();

        for (int i = 0; i < editor.paramFields.size(); i++) {
            String key = editor.paramFieldKeys.get(i);
            if (key != null && key.startsWith("__ui_")) {
                continue;
            }
            String value = editor.paramFields.get(i).getText();
            if (value == null || value.trim().isEmpty()) {
                draft.remove(key);
                continue;
            }
            draft.addProperty(key, value);
        }

        for (int i = 0; i < editor.toggleButtons.size(); i++) {
            draft.addProperty(editor.toggleKeys.get(i), editor.toggleButtons.get(i).getValue());
        }

        for (int i = 0; i < editor.paramDropdowns.size(); i++) {
            String key = editor.paramDropdownKeys.get(i);
            if (key != null && key.startsWith("__ui_")) {
                continue;
            }
            String value = editor.paramDropdowns.get(i).getValue();
            if ("slotBase".equals(key)) {
                boolean isHex = I18n.format("gui.path.action_editor.option.hex").equals(value);
                draft.addProperty(key, isHex ? "HEX" : "DEC");
            } else if ("clickType".equals(key)) {
                draft.addProperty(key, displayToClickType(value));
            } else if ("onlyOnSlotChange".equals(key)) {
                draft.addProperty(key, displayOnOffToBool(value));
            } else if ("locatorMode".equals(key)) {
                draft.addProperty(key, displayToLocatorMode(value));
            } else if ("locatorMatchMode".equals(key)) {
                draft.addProperty(key, displayToMatchMode(value));
            } else if ("huntMode".equals(key)) {
                draft.addProperty(key, displayToHuntMode(value));
            } else if ("attackMode".equals(key) && "hunt".equalsIgnoreCase(editor.getSelectedActionType())) {
                draft.addProperty(key, displayToHuntAttackMode(value));
            } else {
                draft.addProperty(key, value);
            }
        }

        editor.applyHuntActionDraftParams(draft);
        applyResolvedBindingsToParams(editor, draft);
        editor.currentParams = draft;
    }

    private static void addVariableOption(GuiActionEditor editor, LinkedHashMap<String, String> options, String display,
            String actual) {
        if (options == null) {
            return;
        }
        String label = editor.safe(display).trim();
        String value = editor.safe(actual).trim();
        if (label.isEmpty() || value.isEmpty()) {
            return;
        }
        String uniqueLabel = label;
        int duplicateIndex = 2;
        while (options.containsKey(uniqueLabel)
                && !value.equalsIgnoreCase(editor.safe(options.get(uniqueLabel)).trim())) {
            uniqueLabel = label + " (" + duplicateIndex + ")";
            duplicateIndex++;
        }
        options.put(uniqueLabel, value);
    }

    private static boolean containsActualValue(LinkedHashMap<String, LinkedHashMap<String, String>> groups,
            String actualValue) {
        if (groups == null) {
            return false;
        }
        for (LinkedHashMap<String, String> values : groups.values()) {
            if (values != null && values.containsValue(actualValue)) {
                return true;
            }
        }
        return false;
    }

    private static void removeEmptyVariableGroups(LinkedHashMap<String, LinkedHashMap<String, String>> groups) {
        if (groups == null) {
            return;
        }
        List<String> emptyKeys = new ArrayList<String>();
        for (Map.Entry<String, LinkedHashMap<String, String>> entry : groups.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                emptyKeys.add(entry.getKey());
            }
        }
        for (String key : emptyKeys) {
            groups.remove(key);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
