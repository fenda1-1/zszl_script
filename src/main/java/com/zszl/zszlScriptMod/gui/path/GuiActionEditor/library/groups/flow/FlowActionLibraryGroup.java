package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.library.groups.flow;

import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ActionLibraryNode;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.util.function.Function;

public final class FlowActionLibraryGroup {
    private FlowActionLibraryGroup() {
    }

    public static ActionLibraryNode buildRoot(Function<String, ActionLibraryNode> itemFactory) {
        return ActionLibraryNode.group("group_flow",
                I18n.format("gui.path.action_editor.group.flow"),
                ActionLibraryNode.group("group_flow_control",
                        I18n.format("gui.path.action_editor.group.flow.control"),
                        itemFactory.apply("delay"),
                        itemFactory.apply("set_var"),
                        itemFactory.apply("goto_action"),
                        itemFactory.apply("skip_actions"),
                        itemFactory.apply("skip_steps"),
                        itemFactory.apply("repeat_actions")),
                ActionLibraryNode.group("group_flow_capture",
                        I18n.format("gui.path.action_editor.group.flow.capture"),
                        itemFactory.apply("capture_gui_title"),
                        itemFactory.apply("capture_inventory_slot"),
                        itemFactory.apply("capture_hotbar"),
                        itemFactory.apply("capture_entity_list"),
                        itemFactory.apply("capture_packet_field"),
                        itemFactory.apply("capture_gui_element"),
                        itemFactory.apply("capture_scoreboard"),
                        itemFactory.apply("capture_screen_region"),
                        itemFactory.apply("capture_block_at"),
                        itemFactory.apply("capture_nearby_entity")),
                ActionLibraryNode.group("group_flow_condition",
                        I18n.format("gui.path.action_editor.group.flow.condition"),
                        itemFactory.apply("condition_inventory_item"),
                        itemFactory.apply("condition_gui_title"),
                        itemFactory.apply("condition_player_in_area"),
                        itemFactory.apply("condition_entity_nearby"),
                        itemFactory.apply("condition_expression")),
                ActionLibraryNode.group("group_flow_wait",
                        I18n.format("gui.path.action_editor.group.flow.wait"),
                        itemFactory.apply("wait_until_inventory_item"),
                        itemFactory.apply("wait_until_gui_title"),
                        itemFactory.apply("wait_until_player_in_area"),
                        itemFactory.apply("wait_until_entity_nearby"),
                        itemFactory.apply("wait_until_hud_text"),
                        itemFactory.apply("wait_until_expression"),
                        itemFactory.apply("wait_combined"),
                        itemFactory.apply("wait_until_captured_id"),
                        itemFactory.apply("wait_until_packet_text"),
                        itemFactory.apply("wait_until_screen_region")),
                ActionLibraryNode.group("group_flow_network",
                        I18n.format("gui.path.action_editor.group.flow.network"),
                        itemFactory.apply("run_sequence"),
                        itemFactory.apply("stop_current_sequence"),
                        itemFactory.apply("sequence_control"),
                        itemFactory.apply("send_packet")));
    }
}
