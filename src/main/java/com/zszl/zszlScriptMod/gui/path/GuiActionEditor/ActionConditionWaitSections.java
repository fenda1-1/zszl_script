package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import static com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util.ActionEditorDisplayConverters.*;

final class ActionConditionWaitSections {
    private ActionConditionWaitSections() {
    }

    static void buildInventoryConditionSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        editor.initializeConditionInventoryNbtState();
        editor.initializeInventoryItemFilterExpressionEditorState();
        editor.addSectionTitle("§b§l━━━ 物品过滤表达式 ━━━", x, currentY);
        currentY += 25;
        currentY += editor.addInventoryItemFilterExpressionCardEditor(fieldWidth, x, currentY);
        editor.addSectionTitle("§b§l━━━ 计数条件 ━━━", x, currentY);
        currentY += 25;
        editor.addTextField(I18n.format("gui.path.action_editor.label.condition_item_count"), "count",
                I18n.format("gui.path.action_editor.help.condition_item_count"), fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addSectionTitle("§b§l━━━ 动作结果 ━━━", x, currentY);
        currentY += 25;
        if ("condition_inventory_item".equalsIgnoreCase(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
                currentY += 40;
            }
        }
        currentY += 40;
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            editor.addSectionTitle("§b§l━━━ 槽位范围 ━━━", x, currentY);
            currentY += 25;
            editor.addTextField("背包行数", "inventoryRows",
                    "用于绘制下方背包槽位网格，默认 4 行。", fieldWidth, x, currentY, "4");
            currentY += 40;
            editor.addTextField("背包列数", "inventoryCols",
                    "用于绘制下方背包槽位网格，默认 9 列。", fieldWidth, x, currentY, "9");
        }
    }

    static void buildGuiTitleSection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.gui_title_contains"), "title",
                I18n.format("gui.path.action_editor.help.gui_title_contains"), fieldWidth, x, currentY);
        currentY += 40;
        if ("condition_gui_title".equalsIgnoreCase(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildTextConditionWaitSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth, String label, String key, String helpText) {
        editor.addTextField(label, key, helpText, fieldWidth, x, currentY);
        currentY += 40;
        if (selectedType != null && selectedType.startsWith("condition_")) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildPacketFieldConditionWaitSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        editor.addDropdown("包字段读取方式", "lookupMode",
                "最近命中字段=读取字段提取器最近一次命中的结果；运行时变量=按变量键读取已有变量。", fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.latest"),
                        I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.variable")
                },
                packetFieldLookupModeToDisplay(editor.currentParams.has("lookupMode")
                        ? editor.currentParams.get("lookupMode").getAsString()
                        : "LATEST_CAPTURE"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.capture_packet_field_key"), "fieldKey",
                I18n.format("gui.path.action_editor.help.capture_packet_field_key"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField("期望字段值", "expectedValue",
                "留空时只判断目标字段是否存在；填写后按下方匹配方式比较字段值文本。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.match_mode"), "matchMode",
                "包含=字段值包含期望文本；完全相同=字段值与期望文本完全一致。", fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.autouseitem.match.contains"),
                        I18n.format("gui.autouseitem.match.exact")
                },
                matchModeToDisplay(editor.currentParams.has("matchMode")
                        ? editor.currentParams.get("matchMode").getAsString()
                        : "CONTAINS"));
        currentY += 40;
        if (selectedType != null && selectedType.startsWith("condition_")) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField("先执行下面N个动作", "preExecuteCount", "先继续执行后续 N 个动作，再在必要时真正进入等待", fieldWidth,
                    x, currentY, "0");
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildPlayerAreaSection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_pos"), "center",
                I18n.format("gui.path.action_editor.help.area_center"), fieldWidth, x, currentY,
                "[0,0,0]");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.area_radius"), "radius",
                I18n.format("gui.path.action_editor.help.area_radius"), fieldWidth, x, currentY, "3");
        currentY += 40;
        if ("condition_player_in_area".equalsIgnoreCase(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildPlayerListSection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.btnEditPlayerListRules = new ThemedButton(GuiActionEditor.BTN_ID_EDIT_PLAYER_LIST_RULES, x, currentY,
                fieldWidth, 20, editor.getPlayerListRuleButtonText());
        editor.addEditorButton(editor.btnEditPlayerListRules);
        editor.registerScrollableButton(editor.btnEditPlayerListRules, currentY);
        currentY += 40;
        if ("condition_player_list".equalsIgnoreCase(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildEntityNearbySection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.addDropdown("实体类型", "entityType",
                "选择要判断的实体类型；留空名称时会按该类型统计附近实体。", fieldWidth, x, currentY,
                new String[] { "玩家", "敌对生物", "被动生物", "所有实体" },
                entityTypeToDisplay(editor.currentParams.has("entityType")
                        ? editor.currentParams.get("entityType").getAsString()
                        : "all"));
        currentY += 50;
        editor.addTextField(I18n.format("gui.path.action_editor.label.entity_name"), "entityName",
                "支持部分匹配；留空时可仅按实体类型与数量判断。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.range"), "radius",
                I18n.format("gui.path.action_editor.help.range"), fieldWidth, x, currentY, "6");
        currentY += 40;
        editor.addTextField("最少数量", "minCount",
                "匹配实体数量至少达到该值才算成功，默认 1。", fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addSectionTitle("§b§l━━━ 辅助工具 ━━━", x, currentY);
        currentY += 25;
        editor.btnScanNearbyEntities = new ThemedButton(GuiActionEditor.BTN_ID_SCAN_NEARBY_ENTITIES, x, currentY,
                fieldWidth, 20, "扫描附近实体");
        editor.addEditorButton(editor.btnScanNearbyEntities);
        editor.registerScrollableButton(editor.btnScanNearbyEntities, currentY);
        currentY += 40;
        editor.nearbyEntityDropdown = new EnumDropdown(x, currentY, fieldWidth, 20,
                new String[] { "未找到范围内实体" });
        editor.nearbyEntityDropdownBaseY = currentY;
        currentY += 40;
        ThemedButton btnFillNearbyEntityName = new ThemedButton(GuiActionEditor.BTN_ID_FILL_NEARBY_ENTITY_NAME, x,
                currentY, fieldWidth, 20, "填充选中实体名称");
        editor.addEditorButton(btnFillNearbyEntityName);
        editor.registerScrollableButton(btnFillNearbyEntityName, currentY);
        currentY += 40;
        ThemedButton btnFillNearbyEntityRadius = new ThemedButton(GuiActionEditor.BTN_ID_FILL_NEARBY_ENTITY_RADIUS, x,
                currentY, fieldWidth, 20, "按选中实体距离填充范围");
        editor.addEditorButton(btnFillNearbyEntityRadius);
        editor.registerScrollableButton(btnFillNearbyEntityRadius, currentY);
        currentY += 40;
        if ("condition_entity_nearby".equalsIgnoreCase(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildWaitHudTextSection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.contains_text"), "contains",
                I18n.format("gui.path.action_editor.help.contains_text"), fieldWidth, x, currentY);
        currentY += 40;
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            editor.addToggle(I18n.format("gui.path.action_editor.label.match_block"), "matchBlock",
                    I18n.format("gui.path.action_editor.help.match_block"), fieldWidth, x, currentY,
                    editor.currentParams.has("matchBlock")
                            && editor.currentParams.get("matchBlock").getAsBoolean(),
                    I18n.format("path.common.on"), I18n.format("path.common.off"));
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.separator"), "separator",
                    I18n.format("gui.path.action_editor.help.separator"), fieldWidth, x, currentY, " | ");
            currentY += 40;
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                    I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
        }
    }

    static void buildExpressionSection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.initializeBooleanExpressionEditorState();
        currentY += editor.addBooleanExpressionCardEditor(fieldWidth, x, currentY);
        if ("condition_expression".equalsIgnoreCase(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }

    static void buildWaitCombinedSection(GuiActionEditor editor, String selectedType, int x, int currentY, int fieldWidth) {
        editor.addDropdown(I18n.format("gui.path.action_editor.label.wait_combined_mode"), "combinedMode",
                I18n.format("gui.path.action_editor.help.wait_combined_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.wait_combined_mode.any"),
                        I18n.format("gui.path.action_editor.option.wait_combined_mode.all")
                },
                waitCombinedModeToDisplay(editor.currentParams.has("combinedMode")
                        ? editor.currentParams.get("combinedMode").getAsString()
                        : "ANY"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.wait_combined_expressions"), "conditionsText",
                I18n.format("gui.path.action_editor.help.wait_combined_expressions"), fieldWidth, x, currentY);
        currentY += 40;
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.wait_combined_cancel_expression"),
                    "cancelExpression",
                    I18n.format("gui.path.action_editor.help.wait_combined_cancel_expression"), fieldWidth, x,
                    currentY);
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.wait_combined_pre_execute"),
                    "preExecuteCount",
                    I18n.format("gui.path.action_editor.help.wait_combined_pre_execute"), fieldWidth, x, currentY,
                    "0");
            currentY += 40;
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                    I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
        }
    }

    static void buildWaitCapturedIdSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        if (editor.currentParams.has("capturedId")) {
            editor.selectedCapturedIdName = editor.currentParams.get("capturedId").getAsString();
        }
        editor.btnSelectCapturedId = new ThemedButton(GuiActionEditor.BTN_ID_SELECT_CAPTURED_ID, x, currentY, fieldWidth,
                20, editor.getCapturedIdButtonText());
        editor.addEditorButton(editor.btnSelectCapturedId);
        editor.registerScrollableButton(editor.btnSelectCapturedId, currentY);
        currentY += 40;
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            editor.addDropdown("等待模式", "waitMode", "选择等待捕获ID更新还是重新捕获", fieldWidth, x, currentY,
                    new String[] { "等待更新", "等待重新捕获" },
                    capturedIdWaitModeToDisplay(editor.currentParams.has("waitMode")
                            ? editor.currentParams.get("waitMode").getAsString()
                            : "update"));
            currentY += 40;
            editor.addTextField("先执行下面N个动作", "preExecuteCount", "先继续执行后续N个动作，再在必要时真正进入等待", fieldWidth, x,
                    currentY, "0");
            currentY += 40;
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                    I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
        }
    }

    static void buildWaitPacketTextSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        editor.addTextField("数据包文本片段", "packetText",
                "填写中文或英文片段，最近数据包文本中包含该片段即视为完成", fieldWidth, x, currentY);
        currentY += 40;
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            editor.addTextField("先执行下面N个动作", "preExecuteCount", "先继续执行后续N个动作，再在必要时真正进入等待", fieldWidth, x,
                    currentY, "0");
            currentY += 40;
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                    I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
        }
    }

    static void buildWaitScreenRegionSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.vision_region_rect"), "regionRect",
                I18n.format("gui.path.action_editor.help.vision_region_rect"), fieldWidth, x, currentY,
                "[0,0,50,50]");
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.vision_compare_mode"), "visionCompareMode",
                I18n.format("gui.path.action_editor.help.vision_compare_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.vision_compare_mode.color"),
                        I18n.format("gui.path.action_editor.option.vision_compare_mode.template"),
                        I18n.format("gui.path.action_editor.option.vision_compare_mode.edge")
                },
                visionCompareModeToDisplay(editor.currentParams.has("visionCompareMode")
                        ? editor.currentParams.get("visionCompareMode").getAsString()
                        : "AVERAGE_COLOR"));
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.vision_target_color"), "targetColor",
                    I18n.format("gui.path.action_editor.help.vision_target_color"), fieldWidth, x, currentY,
                    "#FFFFFF");
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.vision_color_tolerance"), "colorTolerance",
                    I18n.format("gui.path.action_editor.help.vision_color_tolerance"), fieldWidth, x, currentY,
                    "48");
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.vision_template_path"), "imagePath",
                    I18n.format("gui.path.action_editor.help.vision_template_path"), fieldWidth, x, currentY);
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.vision_similarity_threshold"),
                    "similarityThreshold",
                    I18n.format("gui.path.action_editor.help.vision_similarity_threshold"), fieldWidth, x,
                    currentY, "0.92");
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.vision_edge_threshold"), "edgeThreshold",
                    I18n.format("gui.path.action_editor.help.vision_edge_threshold"), fieldWidth, x, currentY,
                    "0.12");
            currentY += 40;
        } else {
            currentY += 40;
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
        if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                    I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
        }
    }

    static void buildGuiElementConditionWaitSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        editor.addDropdown(I18n.format("gui.path.action_editor.label.gui_element_type"), "elementType",
                I18n.format("gui.path.action_editor.help.gui_element_type"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.gui_element_type.any"),
                        I18n.format("gui.path.action_editor.option.gui_element_type.title"),
                        I18n.format("gui.path.action_editor.option.gui_element_type.button"),
                        I18n.format("gui.path.action_editor.option.gui_element_type.slot")
                },
                guiElementTypeToDisplay(editor.currentParams.has("elementType")
                        ? editor.currentParams.get("elementType").getAsString()
                        : "ANY"));
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.gui_element_locator_mode"),
                "guiElementLocatorMode",
                I18n.format("gui.path.action_editor.help.gui_element_locator_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.gui_element_locator_mode.text"),
                        I18n.format("gui.path.action_editor.option.gui_element_locator_mode.path")
                },
                guiElementLocatorModeToDisplay(editor.currentParams.has("guiElementLocatorMode")
                        ? editor.currentParams.get("guiElementLocatorMode").getAsString()
                        : "TEXT"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                I18n.format("gui.path.action_editor.help.capture_gui_element_locator_text"), fieldWidth, x,
                currentY);
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.autouseitem.match.contains"),
                        I18n.format("gui.autouseitem.match.exact")
                },
                matchModeToDisplay(editor.currentParams.has("locatorMatchMode")
                        ? editor.currentParams.get("locatorMatchMode").getAsString()
                        : "CONTAINS"));
        currentY += 40;
        if (selectedType != null && selectedType.startsWith("condition_")) {
            editor.addTextField(I18n.format("gui.path.action_editor.label.skip_count"), "skipCount",
                    I18n.format("gui.path.action_editor.help.skip_count"), fieldWidth, x, currentY, "1");
        } else {
            editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_ticks"), "timeoutTicks",
                    I18n.format("gui.path.action_editor.help.timeout_ticks"), fieldWidth, x, currentY, "200");
            if (editor.shouldShowAdvancedWaitOptions(selectedType)) {
                currentY += 40;
                editor.addTextField(I18n.format("gui.path.action_editor.label.timeout_skip_count"), "timeoutSkipCount",
                        I18n.format("gui.path.action_editor.help.timeout_skip_count"), fieldWidth, x, currentY, "0");
            }
        }
    }
}
