package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util;

import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.locator.ActionTargetLocator;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

public final class ActionEditorDisplayConverters {
    private ActionEditorDisplayConverters() {
    }

    public static String stateToDisplay(String state) {
        if ("Up".equalsIgnoreCase(state) || "RobotUp".equalsIgnoreCase(state)) {
            return "抬起";
        }
        if ("Press".equalsIgnoreCase(state)) {
            return "单击";
        }
        return "按下";
    }

    public static String displayToState(String display) {
        if ("抬起".equals(display)) {
            return "Up";
        }
        if ("单击".equals(display)) {
            return "Press";
        }
        return "Down";
    }

    public static String leftToDisplay(String leftRaw) {
        return "false".equalsIgnoreCase(leftRaw) ? "右键" : "左键";
    }

    public static String clickCoordinateModeToDisplay(String mode) {
        if (ModUtils.CLICK_COORDINATE_MODE_SCALED.equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.click_coordinate_mode.scaled");
        }
        return I18n.format("gui.path.action_editor.option.click_coordinate_mode.raw");
    }

    public static String displayToClickCoordinateMode(String display) {
        if (I18n.format("gui.path.action_editor.option.click_coordinate_mode.scaled").equals(display)) {
            return ModUtils.CLICK_COORDINATE_MODE_SCALED;
        }
        return ModUtils.CLICK_COORDINATE_MODE_RAW;
    }

    public static String mouseMoveModeToDisplay(String mode) {
        if (ModUtils.CLICK_MOUSE_MOVE_MODE_MOVE.equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.click_mouse_move_mode.move");
        }
        return I18n.format("gui.path.action_editor.option.click_mouse_move_mode.silent");
    }

    public static String displayToMouseMoveMode(String display) {
        if (I18n.format("gui.path.action_editor.option.click_mouse_move_mode.move").equals(display)) {
            return ModUtils.CLICK_MOUSE_MOVE_MODE_MOVE;
        }
        return ModUtils.CLICK_MOUSE_MOVE_MODE_SILENT;
    }

    public static String clickTypeToDisplay(String clickType) {
        return ModUtils.clickTypeToDisplayName(clickType);
    }

    public static String displayToClickType(String display) {
        return ModUtils.normalizeClickTypeName(display);
    }

    public static String directionToDisplay(String direction) {
        return "S2C".equalsIgnoreCase(direction)
                ? I18n.format("path.action.desc.send_packet.direction.s2c")
                : I18n.format("path.action.desc.send_packet.direction.c2s");
    }

    public static String displayToDirection(String display) {
        return I18n.format("path.action.desc.send_packet.direction.s2c").equals(display) ? "S2C" : "C2S";
    }

    public static String matchModeToDisplay(String mode) {
        return "EXACT".equalsIgnoreCase(mode)
                ? I18n.format("gui.autouseitem.match.exact")
                : I18n.format("gui.autouseitem.match.contains");
    }

    public static String displayToMatchMode(String display) {
        return I18n.format("gui.autouseitem.match.exact").equals(display) ? "EXACT" : "CONTAINS";
    }

    public static String useModeToDisplay(String mode) {
        return "LEFT_CLICK".equalsIgnoreCase(mode)
                ? I18n.format("gui.autouseitem.mode.left")
                : I18n.format("gui.autouseitem.mode.right");
    }

    public static String displayToUseMode(String display) {
        return I18n.format("gui.autouseitem.mode.left").equals(display) ? "LEFT_CLICK" : "RIGHT_CLICK";
    }

    public static String valueTypeToDisplay(String type) {
        if ("STRING".equalsIgnoreCase(type)) {
            return I18n.format("gui.path.action_editor.option.string");
        }
        if ("NUMBER".equalsIgnoreCase(type)) {
            return I18n.format("gui.path.action_editor.option.number");
        }
        if ("BOOLEAN".equalsIgnoreCase(type) || "BOOL".equalsIgnoreCase(type)) {
            return I18n.format("gui.path.action_editor.option.boolean");
        }
        return I18n.format("gui.path.action_editor.option.auto");
    }

    public static String displayToValueType(String display) {
        if (I18n.format("gui.path.action_editor.option.string").equals(display)) {
            return "STRING";
        }
        if (I18n.format("gui.path.action_editor.option.number").equals(display)) {
            return "NUMBER";
        }
        if (I18n.format("gui.path.action_editor.option.boolean").equals(display)) {
            return "BOOLEAN";
        }
        return "AUTO";
    }

    public static String capturedIdWaitModeToDisplay(String mode) {
        return "recapture".equalsIgnoreCase(mode) ? "等待重新捕获" : "等待更新";
    }

    public static String displayToCapturedIdWaitMode(String display) {
        return "等待重新捕获".equals(display) ? "recapture" : "update";
    }

    public static String waitCombinedModeToDisplay(String mode) {
        return "ALL".equalsIgnoreCase(mode)
                ? I18n.format("gui.path.action_editor.option.wait_combined_mode.all")
                : I18n.format("gui.path.action_editor.option.wait_combined_mode.any");
    }

    public static String displayToWaitCombinedMode(String display) {
        return I18n.format("gui.path.action_editor.option.wait_combined_mode.all").equals(display)
                ? "ALL"
                : "ANY";
    }

    public static String runSequenceExecuteModeToDisplay(String mode) {
        return "interval".equalsIgnoreCase(mode)
                ? I18n.format("gui.path.action_editor.option.run_sequence_execute_interval")
                : I18n.format("gui.path.action_editor.option.run_sequence_execute_always");
    }

    public static String displayToRunSequenceExecuteMode(String display) {
        return I18n.format("gui.path.action_editor.option.run_sequence_execute_interval").equals(display)
                ? "interval"
                : "always";
    }

    public static String stopCurrentSequenceScopeToDisplay(String scope) {
        return "background".equalsIgnoreCase(scope)
                ? I18n.format("gui.path.action_editor.option.stop_sequence_scope_background")
                : I18n.format("gui.path.action_editor.option.stop_sequence_scope_foreground");
    }

    public static String displayToStopCurrentSequenceScope(String display) {
        return I18n.format("gui.path.action_editor.option.stop_sequence_scope_background").equals(display)
                ? "background"
                : "foreground";
    }

    public static String moveChestDirectionToDisplay(String direction) {
        return ItemFilterHandler.MOVE_DIRECTION_CHEST_TO_INVENTORY.equalsIgnoreCase(direction)
                ? I18n.format("gui.path.action_editor.option.move_chest_direction.chest_to_inventory")
                : I18n.format("gui.path.action_editor.option.move_chest_direction.inventory_to_chest");
    }

    public static String displayToMoveChestDirection(String display) {
        return I18n.format("gui.path.action_editor.option.move_chest_direction.chest_to_inventory").equals(display)
                ? ItemFilterHandler.MOVE_DIRECTION_CHEST_TO_INVENTORY
                : ItemFilterHandler.MOVE_DIRECTION_INVENTORY_TO_CHEST;
    }

    public static String moveChestNbtModeToDisplay(String mode) {
        return "NOT_CONTAINS".equalsIgnoreCase(mode)
                ? I18n.format("gui.path.action_editor.option.nbt_tag_mode.not_contains")
                : I18n.format("gui.path.action_editor.option.nbt_tag_mode.contains");
    }

    public static String displayToMoveChestNbtMode(String display) {
        return I18n.format("gui.path.action_editor.option.nbt_tag_mode.not_contains").equals(display)
                ? "NOT_CONTAINS"
                : "CONTAINS";
    }

    public static String captureSlotAreaToDisplay(String area) {
        if ("HOTBAR".equalsIgnoreCase(area)) {
            return I18n.format("gui.path.action_editor.option.capture_slot_area.hotbar");
        }
        if ("ARMOR".equalsIgnoreCase(area)) {
            return I18n.format("gui.path.action_editor.option.capture_slot_area.armor");
        }
        if ("OFFHAND".equalsIgnoreCase(area)) {
            return I18n.format("gui.path.action_editor.option.capture_slot_area.offhand");
        }
        return I18n.format("gui.path.action_editor.option.capture_slot_area.main");
    }

    public static String displayToCaptureSlotArea(String display) {
        if (I18n.format("gui.path.action_editor.option.capture_slot_area.hotbar").equals(display)) {
            return "HOTBAR";
        }
        if (I18n.format("gui.path.action_editor.option.capture_slot_area.armor").equals(display)) {
            return "ARMOR";
        }
        if (I18n.format("gui.path.action_editor.option.capture_slot_area.offhand").equals(display)) {
            return "OFFHAND";
        }
        return "MAIN";
    }

    public static String packetFieldLookupModeToDisplay(String mode) {
        if ("VARIABLE".equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.variable");
        }
        return I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.latest");
    }

    public static String displayToPacketFieldLookupMode(String display) {
        return I18n.format("gui.path.action_editor.option.capture_packet_field_lookup_mode.variable").equals(display)
                ? "VARIABLE"
                : "LATEST_CAPTURE";
    }

    public static String visionCompareModeToDisplay(String mode) {
        if ("TEMPLATE".equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.vision_compare_mode.template");
        }
        if ("EDGE_DENSITY".equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.vision_compare_mode.edge");
        }
        return I18n.format("gui.path.action_editor.option.vision_compare_mode.color");
    }

    public static String displayToVisionCompareMode(String display) {
        if (I18n.format("gui.path.action_editor.option.vision_compare_mode.template").equals(display)) {
            return "TEMPLATE";
        }
        if (I18n.format("gui.path.action_editor.option.vision_compare_mode.edge").equals(display)) {
            return "EDGE_DENSITY";
        }
        return "AVERAGE_COLOR";
    }

    public static String clickLocatorModeToDisplay(String mode) {
        String normalized = ActionTargetLocator.normalizeClickLocatorMode(mode);
        if (ActionTargetLocator.CLICK_MODE_BUTTON_TEXT.equalsIgnoreCase(normalized)) {
            return I18n.format("gui.path.action_editor.option.locator.click.button_text");
        }
        if (ActionTargetLocator.CLICK_MODE_SLOT_TEXT.equalsIgnoreCase(normalized)) {
            return I18n.format("gui.path.action_editor.option.locator.click.slot_text");
        }
        if (ActionTargetLocator.CLICK_MODE_ELEMENT_PATH.equalsIgnoreCase(normalized)) {
            return I18n.format("gui.path.action_editor.option.locator.click.element_path");
        }
        return I18n.format("gui.path.action_editor.option.locator.click.coordinate");
    }

    public static String slotLocatorModeToDisplay(String mode) {
        if (ActionTargetLocator.SLOT_MODE_ITEM_TEXT.equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.locator.slot.item_text");
        }
        if (ActionTargetLocator.SLOT_MODE_EMPTY.equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.locator.slot.empty");
        }
        if (ActionTargetLocator.SLOT_MODE_PATH.equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.locator.slot.path");
        }
        return I18n.format("gui.path.action_editor.option.locator.slot.direct");
    }

    public static String worldLocatorModeToDisplay(String mode) {
        String normalized = ActionTargetLocator.normalizeWorldLocatorMode(mode);
        if (ActionTargetLocator.TARGET_MODE_NAME.equalsIgnoreCase(normalized)) {
            return I18n.format("gui.path.action_editor.option.locator.world.name");
        }
        return I18n.format("gui.path.action_editor.option.locator.world.position");
    }

    public static String guiElementTypeToDisplay(String type) {
        if ("TITLE".equalsIgnoreCase(type)) {
            return I18n.format("gui.path.action_editor.option.gui_element_type.title");
        }
        if ("BUTTON".equalsIgnoreCase(type)) {
            return I18n.format("gui.path.action_editor.option.gui_element_type.button");
        }
        if ("SLOT".equalsIgnoreCase(type)) {
            return I18n.format("gui.path.action_editor.option.gui_element_type.slot");
        }
        return I18n.format("gui.path.action_editor.option.gui_element_type.any");
    }

    public static String displayToGuiElementType(String display) {
        if (I18n.format("gui.path.action_editor.option.gui_element_type.title").equals(display)) {
            return "TITLE";
        }
        if (I18n.format("gui.path.action_editor.option.gui_element_type.button").equals(display)) {
            return "BUTTON";
        }
        if (I18n.format("gui.path.action_editor.option.gui_element_type.slot").equals(display)) {
            return "SLOT";
        }
        return "ANY";
    }

    public static String guiElementLocatorModeToDisplay(String mode) {
        if ("PATH".equalsIgnoreCase(mode)) {
            return I18n.format("gui.path.action_editor.option.gui_element_locator_mode.path");
        }
        return I18n.format("gui.path.action_editor.option.gui_element_locator_mode.text");
    }

    public static String displayToGuiElementLocatorMode(String display) {
        return I18n.format("gui.path.action_editor.option.gui_element_locator_mode.path").equals(display)
                ? "PATH"
                : "TEXT";
    }

    public static String entityTypeToDisplay(String type) {
        if ("hostile".equalsIgnoreCase(type) || "monster".equalsIgnoreCase(type)) {
            return "敌对生物";
        }
        if ("passive".equalsIgnoreCase(type) || "animal".equalsIgnoreCase(type)) {
            return "被动生物";
        }
        if ("all".equalsIgnoreCase(type) || "entity".equalsIgnoreCase(type)) {
            return "所有实体";
        }
        return "玩家";
    }

    public static String displayToEntityType(String display) {
        if ("敌对生物".equals(display)) {
            return "hostile";
        }
        if ("被动生物".equals(display)) {
            return "passive";
        }
        if ("所有实体".equals(display)) {
            return "all";
        }
        return "player";
    }

    public static String huntModeToDisplay(String mode) {
        if (KillAuraHandler.HUNT_MODE_APPROACH.equalsIgnoreCase(mode)) {
            return "靠近目标";
        }
        return "固定距离";
    }

    public static String displayToHuntMode(String display) {
        if ("靠近目标".equals(display)) {
            return KillAuraHandler.HUNT_MODE_APPROACH;
        }
        return KillAuraHandler.HUNT_MODE_FIXED_DISTANCE;
    }

    public static String huntAttackModeToDisplay(String mode) {
        if (KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(mode)) {
            return "执行序列攻击";
        }
        return "普通攻击";
    }

    public static String displayToHuntAttackMode(String display) {
        if ("执行序列攻击".equals(display)) {
            return KillAuraHandler.ATTACK_MODE_SEQUENCE;
        }
        return KillAuraHandler.ATTACK_MODE_NORMAL;
    }

    public static String displayToLocatorMode(String display) {
        if (ActionTargetLocator.TARGET_MODE_POSITION.equalsIgnoreCase(display)
                || ActionTargetLocator.LEGACY_TARGET_MODE_POSITION_TYPO.equalsIgnoreCase(display)) {
            return ActionTargetLocator.TARGET_MODE_POSITION;
        }
        if (ActionTargetLocator.TARGET_MODE_NAME.equalsIgnoreCase(display)) {
            return ActionTargetLocator.TARGET_MODE_NAME;
        }
        if (I18n.format("gui.path.action_editor.option.locator.click.button_text").equals(display)) {
            return ActionTargetLocator.CLICK_MODE_BUTTON_TEXT;
        }
        if (I18n.format("gui.path.action_editor.option.locator.click.slot_text").equals(display)) {
            return ActionTargetLocator.CLICK_MODE_SLOT_TEXT;
        }
        if (I18n.format("gui.path.action_editor.option.locator.click.element_path").equals(display)) {
            return ActionTargetLocator.CLICK_MODE_ELEMENT_PATH;
        }
        if (I18n.format("gui.path.action_editor.option.locator.slot.item_text").equals(display)) {
            return ActionTargetLocator.SLOT_MODE_ITEM_TEXT;
        }
        if (I18n.format("gui.path.action_editor.option.locator.slot.empty").equals(display)) {
            return ActionTargetLocator.SLOT_MODE_EMPTY;
        }
        if (I18n.format("gui.path.action_editor.option.locator.slot.path").equals(display)) {
            return ActionTargetLocator.SLOT_MODE_PATH;
        }
        if (I18n.format("gui.path.action_editor.option.locator.world.name").equals(display)) {
            return ActionTargetLocator.TARGET_MODE_NAME;
        }
        if (I18n.format("gui.path.action_editor.option.locator.world.position").equals(display)) {
            return ActionTargetLocator.TARGET_MODE_POSITION;
        }
        if (I18n.format("gui.path.action_editor.option.locator.slot.direct").equals(display)) {
            return ActionTargetLocator.SLOT_MODE_DIRECT;
        }
        return ActionTargetLocator.CLICK_MODE_COORDINATE;
    }

    public static String boolToDisplayYesNo(boolean value) {
        return value ? "是" : "否";
    }

    public static boolean displayYesNoToBool(String display) {
        return "是".equals(display);
    }

    public static String boolToDisplayOnOff(boolean value) {
        return value ? I18n.format("path.common.on") : I18n.format("path.common.off");
    }

    public static boolean displayOnOffToBool(String display) {
        return I18n.format("path.common.on").equals(display)
                || "是".equals(display)
                || "true".equalsIgnoreCase(display);
    }
}

