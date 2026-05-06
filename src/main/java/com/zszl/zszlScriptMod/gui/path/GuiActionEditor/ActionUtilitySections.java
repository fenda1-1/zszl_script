package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;

import net.minecraft.client.resources.I18n;

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
        currentY += 40;
        editor.addSectionTitle("§7兼容旧脚本保留；新脚本更建议使用“标签 + 跳转到标签”", x, currentY);
    }

    static void buildLabelSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.label_name"), "labelName",
                I18n.format("gui.path.action_editor.help.label_name"), fieldWidth, x, currentY);
    }

    static void buildGotoLabelSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_label"), "targetLabel",
                I18n.format("gui.path.action_editor.help.target_label"), fieldWidth, x, currentY);
    }

    static void buildIfElseSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("条件表达式", "conditionsText",
                "每行一条表达式；全部为真时执行 true 块，否则执行 false 块。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("True块动作数", "thenCount",
                "条件为真时，从下一条动作开始执行多少条。", fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField("False块动作数", "elseCount",
                "条件为假时，true 块后面作为 else 的动作数。可填 0。", fieldWidth, x, currentY, "0");
    }

    static void buildSwitchVarSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("变量名", "sourceVar",
                "按这个运行时变量的文本值选择分支，例如 sequence.state。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("分支表", "casesText",
                "每行格式: 值=动作数，例如 boss=2。分支块按书写顺序紧跟在当前动作后面。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("默认分支动作数", "defaultCount",
                "没有命中任何值时执行的默认分支动作数。可填 0。", fieldWidth, x, currentY, "0");
    }

    static void buildBranchTableSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("分支键表达式", "keyExpression",
                "先计算这个表达式的结果，再按分支表匹配，例如 target.type。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("分支表", "casesText",
                "每行格式: 值=动作数，例如 hostile=3。分支块按书写顺序紧跟在当前动作后面。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("默认分支动作数", "defaultCount",
                "没有命中任何值时执行的默认分支动作数。可填 0。", fieldWidth, x, currentY, "0");
    }

    static void buildWhileConditionSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("循环条件表达式", "conditionsText",
                "每行一条表达式；全部为真才进入下一轮。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("循环体动作数", "bodyCount",
                "从下一条动作开始算起，作为循环体的动作数量。", fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField("最大循环次数", "maxLoops",
                "0 表示不限制；大于 0 时达到上限后退出循环。", fieldWidth, x, currentY, "0");
        currentY += 40;
        editor.addTextField("循环索引变量", "loopVar",
                "每轮写入的局部变量名，默认 while_index。", fieldWidth, x, currentY, "while_index");
    }

    static void buildForEachListSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("列表变量名", "sourceVar",
                "填写要遍历的运行时变量名，例如 sequence.entities。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("循环体动作数", "bodyCount",
                "从下一条动作开始算起，作为每个元素循环体的动作数量。", fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField("当前元素变量", "itemVar",
                "每轮写入的当前元素变量名，默认 item。", fieldWidth, x, currentY, "item");
        currentY += 40;
        editor.addTextField("索引变量", "indexVar",
                "每轮写入的当前索引变量名，默认 item_index。", fieldWidth, x, currentY, "item_index");
    }

    static void buildForEachPointSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("点列表", "pointsText",
                "每行一个点，支持 [x,y,z] 或 x,y,z。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("循环体动作数", "bodyCount",
                "从下一条动作开始算起，作为每个点循环体的动作数量。", fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField("当前点变量", "pointVar",
                "每轮写入的当前点变量名前缀，默认 point。", fieldWidth, x, currentY, "point");
        currentY += 40;
        editor.addTextField("索引变量", "indexVar",
                "每轮写入的当前索引变量名，默认 point_index。", fieldWidth, x, currentY, "point_index");
    }

    static void buildRetryBlockSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("成功条件表达式", "conditionsText",
                "每行一条表达式；动作块执行完后全部为真则停止重试。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("动作块数量", "bodyCount",
                "从下一条动作开始算起，作为重试体的动作数量。", fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField("最大重试次数", "retryCount",
                "失败后最多再重试多少次；0 表示只执行一次。", fieldWidth, x, currentY, "3");
        currentY += 40;
        editor.addTextField("重试延迟Tick", "retryDelayTicks",
                "每次失败后重新进入动作块前等待的 Tick。", fieldWidth, x, currentY, "0");
        currentY += 40;
        editor.addTextField("重试变量前缀", "attemptVar",
                "输出 attempt/success/exhausted/remaining 等变量，默认 retry_block。", fieldWidth, x, currentY,
                "retry_block");
    }

    static void buildDebugPrintVarSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField("变量名", "varName",
                "填写要打印的运行时变量名，例如 sequence.target_count、global.money。", fieldWidth, x, currentY);
    }

    static void buildDebugPrintNearbyEntitiesSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addDropdown("实体类型", "entityType",
                "选择要打印的实体类型；默认打印所有附近实体。", fieldWidth, x, currentY,
                new String[] { "所有实体", "玩家", "敌对生物", "被动生物" },
                entityTypeToDisplay(editor.currentParams.has("entityType")
                        ? editor.currentParams.get("entityType").getAsString()
                        : "all"));
        currentY += 40;
        editor.addTextField("实体名称", "entityName",
                "可选；按名称关键字过滤后再打印。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.range"), "radius",
                I18n.format("gui.path.action_editor.help.range"), fieldWidth, x, currentY, "8");
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
