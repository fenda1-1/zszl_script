package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import static com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util.ActionEditorDisplayConverters.*;

final class ActionUtilitySections {
    private ActionUtilitySections() {
    }

    static void buildSetVarSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "name",
                I18n.format("gui.path.action_editor.help.variable_name"), fieldWidth, x, currentY, "");
        editor.addTextField(I18n.format("gui.path.action_editor.label.value"), "value",
                I18n.format("gui.path.action_editor.help.value"), fieldWidth, x, currentY);
        currentY += 40;
        currentY += editor.addExpressionTemplateEditor(I18n.format("gui.path.action_editor.label.expression"), "expression",
                fieldWidth, x, currentY);
        currentY += editor.addGroupedRuntimeVariableSelector(I18n.format("gui.path.action_editor.label.source_var"), "fromVar",
                I18n.format("gui.path.action_editor.help.source_var"), fieldWidth, x, currentY);
        editor.addDropdown(I18n.format("gui.path.action_editor.label.value_type"), "valueType",
                I18n.format("gui.path.action_editor.help.value_type"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.auto"),
                        I18n.format("gui.path.action_editor.option.string"),
                        I18n.format("gui.path.action_editor.option.number"),
                        I18n.format("gui.path.action_editor.option.boolean")
                },
                valueTypeToDisplay(editor.currentParams.has("valueType")
                        ? editor.currentParams.get("valueType").getAsString()
                        : ""));
    }

    static void buildGotoActionSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_action_index"), "targetActionIndex",
                I18n.format("gui.path.action_editor.help.target_action_index"), fieldWidth, x, currentY, "0");
    }

    static void buildSkipActionsSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.skip_action_count"), "count",
                I18n.format("gui.path.action_editor.help.skip_action_count"), fieldWidth, x, currentY, "1");
    }

    static void buildSkipStepsSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.skip_step_count"), "count",
                I18n.format("gui.path.action_editor.help.skip_step_count"), fieldWidth, x, currentY, "0");
    }

    static void buildRepeatActionsSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.repeat_count"), "count",
                I18n.format("gui.path.action_editor.help.repeat_count"), fieldWidth, x, currentY, "2");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.body_count"), "bodyCount",
                I18n.format("gui.path.action_editor.help.body_count"), fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.loop_var"), "loopVar",
                I18n.format("gui.path.action_editor.help.loop_var"), fieldWidth, x, currentY, "loop_index");
    }

    static void buildAutoEatSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addToggle(I18n.format("gui.path.action_editor.label.enabled"), "enabled",
                I18n.format("gui.path.action_editor.help.enabled"), fieldWidth, x, currentY,
                !editor.currentParams.has("enabled")
                        || editor.currentParams.get("enabled").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.food_level_threshold"), "foodLevelThreshold",
                I18n.format("gui.path.action_editor.help.food_level_threshold"), fieldWidth, x, currentY, "12");
        currentY += 40;
        editor.addToggle(I18n.format("gui.path.action_editor.label.auto_move_food_enabled"), "autoMoveFoodEnabled",
                I18n.format("gui.path.action_editor.help.auto_move_food_enabled"), fieldWidth, x, currentY,
                !editor.currentParams.has("autoMoveFoodEnabled")
                        || editor.currentParams.get("autoMoveFoodEnabled").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addToggle(I18n.format("gui.path.action_editor.label.eat_with_look_down"), "eatWithLookDown",
                I18n.format("gui.path.action_editor.help.eat_with_look_down"), fieldWidth, x, currentY,
                editor.currentParams.has("eatWithLookDown")
                        && editor.currentParams.get("eatWithLookDown").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_hotbar_slot"), "targetHotbarSlot",
                I18n.format("gui.path.action_editor.help.target_hotbar_slot"), fieldWidth, x, currentY, "9");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.food_keywords_text"), "foodKeywordsText",
                I18n.format("gui.path.action_editor.help.food_keywords_text"), fieldWidth, x, currentY);
    }

    static void buildAutoEquipSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addToggle(I18n.format("gui.path.action_editor.label.enabled"), "enabled",
                I18n.format("gui.path.action_editor.help.enabled"), fieldWidth, x, currentY,
                !editor.currentParams.has("enabled")
                        || editor.currentParams.get("enabled").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.set_name"), "setName",
                I18n.format("gui.path.action_editor.help.set_name"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addToggle(I18n.format("gui.path.action_editor.label.smart_activation"), "smartActivation",
                I18n.format("gui.path.action_editor.help.smart_activation"), fieldWidth, x, currentY,
                editor.currentParams.has("smartActivation")
                        && editor.currentParams.get("smartActivation").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
    }

    static void buildAutoPickupSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addToggle(I18n.format("gui.path.action_editor.label.enabled"), "enabled",
                I18n.format("gui.path.action_editor.help.enabled"), fieldWidth, x, currentY,
                !editor.currentParams.has("enabled")
                        || editor.currentParams.get("enabled").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
    }

    static void buildSimpleToggleSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addDropdown(I18n.format("gui.path.action_editor.label.enabled"), "enabled",
                I18n.format("gui.path.action_editor.help.enabled"), fieldWidth, x, currentY,
                new String[] { I18n.format("path.common.on"), I18n.format("path.common.off") },
                boolToDisplayOnOff(!editor.currentParams.has("enabled")
                        || editor.currentParams.get("enabled").getAsBoolean()));
    }

    static void buildOtherFeatureToggleSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        if (editor.currentParams.has("featureId")) {
            editor.selectedOtherFeatureId = editor.currentParams.get("featureId").getAsString();
        }
        editor.btnSelectOtherFeature = new ThemedButton(GuiActionEditor.BTN_ID_SELECT_OTHER_FEATURE, x, currentY,
                fieldWidth, 20, editor.getOtherFeatureButtonText());
        editor.addEditorButton(editor.btnSelectOtherFeature);
        editor.registerScrollableButton(editor.btnSelectOtherFeature, currentY);
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.enabled"), "enabled",
                I18n.format("gui.path.action_editor.help.enabled"), fieldWidth, x, currentY,
                new String[] { I18n.format("path.common.on"), I18n.format("path.common.off") },
                boolToDisplayOnOff(!editor.currentParams.has("enabled")
                        || editor.currentParams.get("enabled").getAsBoolean()));
    }

    static void buildTakeAllItemsSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addToggle(I18n.format("gui.path.action_editor.label.shift_quick_move"), "shiftQuickMove",
                I18n.format("gui.path.action_editor.help.shift_quick_move"), fieldWidth, x, currentY,
                !editor.currentParams.has("shiftQuickMove") || editor.currentParams.get("shiftQuickMove").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
    }
}

