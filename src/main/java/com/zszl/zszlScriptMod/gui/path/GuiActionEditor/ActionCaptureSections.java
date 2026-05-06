package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.utils.locator.ActionTargetLocator;

import net.minecraft.client.resources.I18n;

import static com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util.ActionEditorDisplayConverters.*;

final class ActionCaptureSections {
    private ActionCaptureSections() {
    }

    static void buildCaptureNearbyEntitySection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY, "entity");
        editor.addDropdown("实体类型", "entityType",
                "选择要采集的实体类型；留空名称时会采集最近的该类型实体。", fieldWidth, x, currentY,
                new String[] { "玩家", "敌对生物", "被动生物", "所有实体" },
                entityTypeToDisplay(editor.currentParams.has("entityType")
                        ? editor.currentParams.get("entityType").getAsString()
                        : "all"));
        currentY += 50;
        editor.addTextField(I18n.format("gui.path.action_editor.label.entity_name"), "entityName",
                "支持部分匹配；留空时按实体类型采集最近目标。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.range"), "radius",
                I18n.format("gui.path.action_editor.help.range"), fieldWidth, x, currentY, "6");
    }

    static void buildCaptureGuiTitleSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "gui_title");
    }

    static void buildCaptureInventorySlotSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY, "slot");
        editor.addDropdown(I18n.format("gui.path.action_editor.label.capture_slot_area"), "slotArea",
                I18n.format("gui.path.action_editor.help.capture_slot_area"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.capture_slot_area.main"),
                        I18n.format("gui.path.action_editor.option.capture_slot_area.hotbar"),
                        I18n.format("gui.path.action_editor.option.capture_slot_area.armor"),
                        I18n.format("gui.path.action_editor.option.capture_slot_area.offhand")
                },
                captureSlotAreaToDisplay(editor.currentParams.has("slotArea")
                        ? editor.currentParams.get("slotArea").getAsString()
                        : "MAIN"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.capture_slot_index"), "slotIndex",
                I18n.format("gui.path.action_editor.help.capture_slot_index"), fieldWidth, x, currentY, "0");
    }

    static void buildCaptureHotbarSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "hotbar");
    }

    static void buildCaptureEntityListSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "entities");
        editor.addDropdown("实体类型", "entityType",
                "选择要采集的实体类型；留空名称时采集该类型的附近实体列表。", fieldWidth, x, currentY,
                new String[] { "玩家", "敌对生物", "被动生物", "所有实体" },
                entityTypeToDisplay(editor.currentParams.has("entityType")
                        ? editor.currentParams.get("entityType").getAsString()
                        : "all"));
        currentY += 50;
        editor.addTextField(I18n.format("gui.path.action_editor.label.entity_name"), "entityName",
                "支持部分匹配；留空时按实体类型采集列表。", fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.range"), "radius",
                I18n.format("gui.path.action_editor.help.range"), fieldWidth, x, currentY, "8");
        if (editor.shouldShowAdvancedCaptureOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.capture_max_count"), "maxCount",
                    I18n.format("gui.path.action_editor.help.capture_max_count"), fieldWidth, x, currentY, "16");
        }
    }

    static void buildCapturePacketFieldSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "packet_field");
        if (editor.shouldShowAdvancedCaptureOptions(selectedType)) {
            editor.addDropdown(I18n.format("gui.path.action_editor.label.capture_packet_field_lookup_mode"), "lookupMode",
                    I18n.format("gui.path.action_editor.help.capture_packet_field_lookup_mode"), fieldWidth, x,
                    currentY,
                    new String[] {
                            I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.latest"),
                            I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.variable")
                    },
                    packetFieldLookupModeToDisplay(editor.currentParams.has("lookupMode")
                            ? editor.currentParams.get("lookupMode").getAsString()
                            : "LATEST_CAPTURE"));
            currentY += 40;
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.capture_packet_field_key"), "fieldKey",
                I18n.format("gui.path.action_editor.help.capture_packet_field_key"), fieldWidth, x, currentY);
        if (editor.shouldShowAdvancedCaptureOptions(selectedType)) {
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.capture_packet_field_fallback"),
                    "fallbackValue",
                    I18n.format("gui.path.action_editor.help.capture_packet_field_fallback"), fieldWidth, x,
                    currentY);
        }
    }

    static void buildCaptureGuiElementSection(GuiActionEditor editor, String selectedType, int x, int currentY,
            int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "gui_element");
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
        if (editor.shouldShowAdvancedCaptureOptions(selectedType)) {
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
        }
        editor.addTextField(I18n.format("gui.path.action_editor.label.locator_text"), "locatorText",
                I18n.format("gui.path.action_editor.help.capture_gui_element_locator_text"), fieldWidth, x,
                currentY);
        if (editor.shouldShowAdvancedCaptureOptions(selectedType)) {
            currentY += 40;
            editor.addDropdown(I18n.format("gui.path.action_editor.label.locator_match_mode"), "locatorMatchMode",
                    I18n.format("gui.path.action_editor.help.locator_match_mode"), fieldWidth, x, currentY,
                    new String[] {
                            I18n.format("gui.autouseitem.match.contains"),
                            I18n.format("gui.autouseitem.match.exact")
                    },
                    matchModeToDisplay(editor.currentParams.has("locatorMatchMode")
                            ? editor.currentParams.get("locatorMatchMode").getAsString()
                            : ActionTargetLocator.MATCH_MODE_CONTAINS));
        }
    }

    static void buildCaptureBlockAtSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY, "block");
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_pos"), "pos",
                I18n.format("gui.path.action_editor.help.capture_block_pos"), fieldWidth, x, currentY,
                "[0,0,0]");
    }

    static void buildCaptureScoreboardSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "scoreboard");
        editor.addTextField(I18n.format("gui.path.action_editor.label.capture_scoreboard_line_index"), "lineIndex",
                I18n.format("gui.path.action_editor.help.capture_scoreboard_line_index"), fieldWidth, x,
                currentY, "-1");
    }

    static void buildCaptureScreenRegionSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        currentY += editor.addScopedVariableEditor(I18n.format("gui.path.action_editor.label.variable_name"), "varName",
                I18n.format("gui.path.action_editor.help.capture_var_name"), fieldWidth, x, currentY,
                "vision_region");
        editor.addTextField(I18n.format("gui.path.action_editor.label.vision_region_rect"), "regionRect",
                I18n.format("gui.path.action_editor.help.vision_region_rect"), fieldWidth, x, currentY,
                "[0,0,50,50]");
    }
}
