package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import static com.zszl.zszlScriptMod.gui.path.GuiActionEditor.util.ActionEditorDisplayConverters.*;

final class ActionParameterSections {
    private ActionParameterSections() {
    }

    static void buildHuntSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.initializeHuntWhitelistCardState();
        editor.addSectionTitle("§d§l━━━ 常用预设 ━━━", x, currentY);
        currentY += 25;
        currentY += editor.addPresetButtons(GuiActionEditor.BTN_ID_APPLY_HUNT_PRESET_BASE,
                new String[] { "小范围", "中范围", "大范围" }, x, currentY, fieldWidth);

        editor.addSectionTitle("§b§l━━━ 基础参数 ━━━", x, currentY);
        currentY += 25;

        editor.addTextField(I18n.format("gui.path.action_editor.label.search_radius"), "radius",
                I18n.format("gui.path.action_editor.help.search_radius"), fieldWidth, x, currentY, "3.0");
        currentY += 40;
        if (editor.shouldShowAdvancedOptions()) {
            editor.addTextField("无目标时跳过动作数", "noTargetSkipCount",
                    "搜不到任何目标时，额外跳过后续几个动作。0 表示不跳过。", fieldWidth, x, currentY, "0");
            currentY += 40;
        }
        editor.addTextField("向上追击范围", "huntUpRange",
                "只追击比触发坐标高出该范围内的目标；旧动作默认 1 格。", fieldWidth, x, currentY, "1.0");
        currentY += 40;
        editor.addTextField("向下追击范围", "huntDownRange",
                "只追击比触发坐标低出该范围内的目标；旧动作默认 1 格。", fieldWidth, x, currentY, "1.0");
        currentY += 40;
        editor.addSectionTitle("§7战斗/索敌/转向/追击使用杀戮光环当前配置", x, currentY);
        currentY += 30;

        editor.addSectionTitle("§b§l━━━ 动作名单过滤 ━━━", x, currentY);
        currentY += 25;

        editor.addToggle(I18n.format("gui.path.action_editor.label.hunt_enable_whitelist"), "enableNameWhitelist",
                I18n.format("gui.path.action_editor.help.hunt_enable_whitelist"), fieldWidth, x, currentY,
                editor.currentParams.has("enableNameWhitelist")
                        && editor.currentParams.get("enableNameWhitelist").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField("白名单目标", "__ui_hunt_whitelist_name",
                "实体名称关键字，按包含匹配；可手动输入，也可用下方扫描后加入。", fieldWidth, x, currentY, "");
        currentY += 40;
        editor.addTextField("击杀数量", "__ui_hunt_whitelist_kill_count",
                "可空；大于 0 时该目标击杀到数量后计入完成。空或 0 表示清完所有匹配目标。", fieldWidth, x, currentY, "");
        currentY += 40;
        int whitelistButtonGap = 6;
        int whitelistButtonWidth = Math.max(80, (fieldWidth - whitelistButtonGap) / 2);
        editor.btnAddHuntWhitelistCard = new ThemedButton(GuiActionEditor.BTN_ID_ADD_HUNT_WHITELIST_CARD,
                x, currentY, whitelistButtonWidth, 20, "添加/更新白名单");
        editor.btnDeleteHuntWhitelistCard = new ThemedButton(GuiActionEditor.BTN_ID_DELETE_HUNT_WHITELIST_CARD,
                x + whitelistButtonWidth + whitelistButtonGap, currentY,
                Math.max(80, fieldWidth - whitelistButtonWidth - whitelistButtonGap), 20, "删除选中");
        editor.addEditorButton(editor.btnAddHuntWhitelistCard);
        editor.addEditorButton(editor.btnDeleteHuntWhitelistCard);
        editor.registerScrollableButton(editor.btnAddHuntWhitelistCard, currentY);
        editor.registerScrollableButton(editor.btnDeleteHuntWhitelistCard, currentY);
        currentY += 32;
        currentY += editor.addHuntWhitelistCardList(fieldWidth, x, currentY);
        currentY += 16;
        editor.addToggle(I18n.format("gui.path.action_editor.label.hunt_enable_blacklist"), "enableNameBlacklist",
                I18n.format("gui.path.action_editor.help.hunt_enable_blacklist"), fieldWidth, x, currentY,
                editor.currentParams.has("enableNameBlacklist")
                        && editor.currentParams.get("enableNameBlacklist").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.hunt_name_blacklist"), "nameBlacklistText",
                I18n.format("gui.path.action_editor.help.hunt_name_blacklist"), fieldWidth, x, currentY,
                editor.getJoinedStringParam(editor.currentParams, "nameBlacklist", "nameBlacklistText"));
        currentY += 50;

        editor.addSectionTitle("§b§l━━━ 辅助工具 ━━━", x, currentY);
        currentY += 25;

        editor.addTextField(I18n.format("gui.path.action_editor.label.scan_radius"), "scanRadius",
                I18n.format("gui.path.action_editor.help.scan_radius"), fieldWidth, x, currentY, "10");
        currentY += 40;

        editor.btnScanNearbyEntities = new ThemedButton(GuiActionEditor.BTN_ID_SCAN_NEARBY_ENTITIES, x, currentY,
                fieldWidth, 20, I18n.format("gui.path.action_editor.button.hunt_scan_nearby"));
        editor.addEditorButton(editor.btnScanNearbyEntities);
        editor.registerScrollableButton(editor.btnScanNearbyEntities, currentY);
        currentY += 40;
        editor.nearbyEntityDropdown = new EnumDropdown(x, currentY, fieldWidth, 20,
                new String[] { "未找到范围内生物" });
        editor.nearbyEntityDropdownBaseY = currentY;
        currentY += 40;
        int huntButtonGap = 6;
        int leftButtonWidth = Math.max(80, (fieldWidth - huntButtonGap) / 2);
        int rightButtonWidth = Math.max(80, fieldWidth - leftButtonWidth - huntButtonGap);
        editor.btnAddSelectedHuntWhitelist = new ThemedButton(GuiActionEditor.BTN_ID_ADD_HUNT_WHITELIST, x,
                currentY, leftButtonWidth, 20, I18n.format("gui.path.action_editor.button.hunt_add_whitelist"));
        editor.btnAddSelectedHuntBlacklist = new ThemedButton(GuiActionEditor.BTN_ID_ADD_HUNT_BLACKLIST,
                x + leftButtonWidth + huntButtonGap, currentY, rightButtonWidth, 20,
                I18n.format("gui.path.action_editor.button.hunt_add_blacklist"));
        editor.addEditorButton(editor.btnAddSelectedHuntWhitelist);
        editor.addEditorButton(editor.btnAddSelectedHuntBlacklist);
        editor.registerScrollableButton(editor.btnAddSelectedHuntWhitelist, currentY);
        editor.registerScrollableButton(editor.btnAddSelectedHuntBlacklist, currentY);
        currentY += 50;

        editor.addSectionTitle("§b§l━━━ 可视化选项 ━━━", x, currentY);
        currentY += 25;

        editor.addToggle("显示搜怪范围", "showHuntRange",
                "开启后执行动作时会像杀戮光环一样显示以触发坐标中心为圆心的半径光环", fieldWidth, x, currentY,
                editor.currentParams.has("showHuntRange") && editor.currentParams.get("showHuntRange").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
    }

    static void buildFollowEntitySection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addSectionTitle("§b§l━━━ 实体类型 ━━━", x, currentY);
        currentY += 25;

        editor.addDropdown("实体类型", "entityType",
                "选择要跟随的实体类型：玩家、敌对生物、被动生物或所有实体", fieldWidth, x, currentY,
                new String[] { "玩家", "敌对生物", "被动生物", "所有实体" },
                entityTypeToDisplay(editor.currentParams.has("entityType")
                        ? editor.currentParams.get("entityType").getAsString()
                        : "player"));
        currentY += 50;

        editor.addSectionTitle("§b§l━━━ 目标筛选 ━━━", x, currentY);
        currentY += 25;

        editor.addTextField("实体/玩家名称", "targetName",
                "填写要跟随的实体或玩家名称（支持部分匹配），留空则跟随最近的目标", fieldWidth, x, currentY, "");
        currentY += 40;

        editor.addTextField("搜索半径", "searchRadius",
                "搜索目标实体的范围（格）", fieldWidth, x, currentY, "16.0");
        currentY += 50;

        editor.addSectionTitle("§b§l━━━ 跟随参数 ━━━", x, currentY);
        currentY += 25;

        editor.addTextField("跟随距离", "followDistance",
                "与目标保持的距离（格），设置为0则尽可能靠近", fieldWidth, x, currentY, "3.0");
        currentY += 40;

        editor.addTextField("超时时间", "timeout",
                "跟随超时时间（秒），0表示无限跟随直到找不到目标", fieldWidth, x, currentY, "0");
        currentY += 40;

        editor.addToggle("丢失目标后停止", "stopOnLost",
                "开启后，如果目标消失或超出范围，动作将结束", fieldWidth, x, currentY,
                !editor.currentParams.has("stopOnLost") || editor.currentParams.get("stopOnLost").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 50;

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

        ThemedButton btnFillFromScan = new ThemedButton(GuiActionEditor.BTN_ID_FILL_FOLLOW_ENTITY_NAME, x, currentY,
                fieldWidth, 20, "填充选中实体名称");
        editor.addEditorButton(btnFillFromScan);
        editor.registerScrollableButton(btnFillFromScan, currentY);
    }

    static void buildUseHotbarItemSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.item_name"), "itemName",
                I18n.format("gui.path.action_editor.help.item_name"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.match_mode"), "matchMode",
                I18n.format("gui.path.action_editor.help.match_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.autouseitem.match.contains"),
                        I18n.format("gui.autouseitem.match.exact")
                },
                matchModeToDisplay(editor.currentParams.has("matchMode")
                        ? editor.currentParams.get("matchMode").getAsString()
                        : "CONTAINS"));
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.use_mode"), "useMode",
                I18n.format("gui.path.action_editor.help.use_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.autouseitem.mode.right"),
                        I18n.format("gui.autouseitem.mode.left")
                },
                useModeToDisplay(editor.currentParams.has("useMode")
                        ? editor.currentParams.get("useMode").getAsString()
                        : "RIGHT_CLICK"));
        currentY += 40;
        editor.addToggle(I18n.format("gui.path.action_editor.label.change_local_slot"), "changeLocalSlot",
                I18n.format("gui.path.action_editor.help.change_local_slot"), fieldWidth, x, currentY,
                editor.currentParams.has("changeLocalSlot") && editor.currentParams.get("changeLocalSlot").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.use_count"), "count",
                I18n.format("gui.path.action_editor.help.use_count"), fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.switch_item_delay_ticks"), "switchItemDelayTicks",
                I18n.format("gui.path.action_editor.help.switch_item_delay_ticks"), fieldWidth, x, currentY, "0");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.use_item_delay_ticks"), "switchDelayTicks",
                I18n.format("gui.path.action_editor.help.use_item_delay_ticks"), fieldWidth, x, currentY, "0");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.switch_back_delay_ticks"),
                "switchBackDelayTicks",
                I18n.format("gui.path.action_editor.help.switch_back_delay_ticks"), fieldWidth, x, currentY, "0");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.use_interval_ticks"), "intervalTicks",
                I18n.format("gui.path.action_editor.help.use_interval_ticks"), fieldWidth, x, currentY, "0");
    }

    static void buildMoveInventoryItemToHotbarSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.item_name"), "itemName",
                I18n.format("gui.path.action_editor.help.item_name"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addDropdown(I18n.format("gui.path.action_editor.label.match_mode"), "matchMode",
                I18n.format("gui.path.action_editor.help.match_mode"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.autouseitem.match.contains"),
                        I18n.format("gui.autouseitem.match.exact")
                },
                matchModeToDisplay(editor.currentParams.has("matchMode")
                        ? editor.currentParams.get("matchMode").getAsString()
                        : "CONTAINS"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_hotbar_slot"), "targetHotbarSlot",
                I18n.format("gui.path.action_editor.help.target_hotbar_slot"), fieldWidth, x, currentY, "1");
    }

    static void buildSilentUseSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.item_name"), "item",
                I18n.format("gui.path.action_editor.help.item_name"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.temp_hotbar_slot"), "tempslot",
                I18n.format("gui.path.action_editor.help.temp_hotbar_slot"), fieldWidth, x, currentY, "0");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.switch_item_delay_ticks"), "switchDelayTicks",
                I18n.format("gui.path.action_editor.help.switch_item_delay_ticks"), fieldWidth, x, currentY,
                editor.currentParams.has("switchDelayTicks")
                        ? editor.currentParams.get("switchDelayTicks").getAsString()
                        : "0");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.use_item_delay_ticks"), "useDelayTicks",
                I18n.format("gui.path.action_editor.help.use_item_delay_ticks"), fieldWidth, x, currentY,
                editor.currentParams.has("useDelayTicks")
                        ? editor.currentParams.get("useDelayTicks").getAsString()
                        : "1");
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.switch_back_delay_ticks"),
                "switchBackDelayTicks",
                I18n.format("gui.path.action_editor.help.switch_back_delay_ticks"), fieldWidth, x, currentY,
                editor.currentParams.has("switchBackDelayTicks")
                        ? editor.currentParams.get("switchBackDelayTicks").getAsString()
                        : "0");
    }

    static void buildSwitchHotbarSlotSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.target_hotbar_slot"), "targetHotbarSlot",
                I18n.format("gui.path.action_editor.help.target_hotbar_slot"), fieldWidth, x, currentY, "1");
        currentY += 40;
        editor.addToggle(I18n.format("gui.path.action_editor.label.use_after_switch"), "useAfterSwitch",
                I18n.format("gui.path.action_editor.help.use_after_switch"), fieldWidth, x, currentY,
                editor.currentParams.has("useAfterSwitch") && editor.currentParams.get("useAfterSwitch").getAsBoolean(),
                I18n.format("path.common.on"), I18n.format("path.common.off"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.use_after_switch_delay_ticks"),
                "useAfterSwitchDelayTicks",
                I18n.format("gui.path.action_editor.help.use_after_switch_delay_ticks"), fieldWidth, x, currentY,
                editor.currentParams.has("useAfterSwitchDelayTicks")
                        ? editor.currentParams.get("useAfterSwitchDelayTicks").getAsString()
                        : "0");
    }

    static void buildUseHeldItemSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addTextField(I18n.format("gui.path.action_editor.label.delay_ticks"), "delayTicks",
                "使用当前主手物品前的延迟。0 表示立即使用。", fieldWidth, x, currentY,
                editor.currentParams.has("delayTicks") ? editor.currentParams.get("delayTicks").getAsString() : "0");
    }

    static void buildSendPacketSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addDropdown(I18n.format("gui.path.action_editor.label.direction"), "direction",
                I18n.format("gui.path.action_editor.help.direction"), fieldWidth, x, currentY,
                new String[] {
                        I18n.format("path.action.desc.send_packet.direction.c2s"),
                        I18n.format("path.action.desc.send_packet.direction.s2c")
                },
                directionToDisplay(
                        editor.currentParams.has("direction") ? editor.currentParams.get("direction").getAsString()
                                : "C2S"));
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.channel"), "channel",
                I18n.format("gui.path.action_editor.help.channel"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.packet_id"), "packetId",
                I18n.format("gui.path.action_editor.help.packet_id"), fieldWidth, x, currentY);
        currentY += 40;
        editor.addTextField(I18n.format("gui.path.action_editor.label.hex_data"), "hex",
                I18n.format("gui.path.action_editor.help.hex_data"), fieldWidth, x, currentY);
    }

    static void buildRunSequenceSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addSectionTitle("§d§l━━━ 常用预设 ━━━", x, currentY);
        currentY += 25;
        currentY += editor.addPresetButtons(GuiActionEditor.BTN_ID_APPLY_RUN_SEQUENCE_PRESET_BASE,
                new String[] { "每次执行", "间隔执行", "后台执行" }, x, currentY, fieldWidth);
        if (editor.currentParams.has("sequenceName")) {
            editor.selectedRunSequenceName = editor.currentParams.get("sequenceName").getAsString();
        }
        if (!editor.currentParams.has("uuid")) {
            editor.currentParams.addProperty("uuid", java.util.UUID.randomUUID().toString());
        }
        editor.btnSelectRunSequence = new ThemedButton(GuiActionEditor.BTN_ID_SELECT_RUN_SEQUENCE, x, currentY,
                fieldWidth, 20, editor.getRunSequenceButtonText());
        editor.addEditorButton(editor.btnSelectRunSequence);
        editor.registerScrollableButton(editor.btnSelectRunSequence, currentY);
        currentY += 40;
        if (editor.shouldShowAdvancedOptions()) {
            editor.addDropdown(I18n.format("gui.path.action_editor.label.run_sequence_execute_mode"), "executeMode",
                    I18n.format("gui.path.action_editor.help.run_sequence_execute_mode"), fieldWidth, x, currentY,
                    new String[] {
                            I18n.format("gui.path.action_editor.option.run_sequence_execute_always"),
                            I18n.format("gui.path.action_editor.option.run_sequence_execute_interval")
                    },
                    runSequenceExecuteModeToDisplay(editor.currentParams.has("executeMode")
                            ? editor.currentParams.get("executeMode").getAsString()
                            : "always"));
            currentY += 40;
            editor.addTextField(I18n.format("gui.path.action_editor.label.run_sequence_execute_every_count"),
                    "executeEveryCount",
                    I18n.format("gui.path.action_editor.help.run_sequence_execute_every_count"),
                    fieldWidth, x, currentY, "1");
            currentY += 40;
            editor.addDropdown(I18n.format("gui.path.action_editor.label.run_sequence_background_execution"),
                    "backgroundExecution",
                    I18n.format("gui.path.action_editor.help.run_sequence_background_execution"),
                    fieldWidth, x, currentY,
                    new String[] { "否", "是" },
                    boolToDisplayYesNo(editor.currentParams.has("backgroundExecution")
                            && editor.currentParams.get("backgroundExecution").getAsBoolean()));
            currentY += 40;
        }
        editor.runSequenceStatusLabelY = currentY;
        editor.runSequenceStatusLabelBaseY = currentY;
    }

    static void buildStopCurrentSequenceSection(GuiActionEditor editor, int x, int currentY, int fieldWidth) {
        editor.addDropdown(I18n.format("gui.path.action_editor.label.stop_sequence_target_scope"),
                "targetScope",
                I18n.format("gui.path.action_editor.help.stop_sequence_target_scope"),
                fieldWidth, x, currentY,
                new String[] {
                        I18n.format("gui.path.action_editor.option.stop_sequence_scope_foreground"),
                        I18n.format("gui.path.action_editor.option.stop_sequence_scope_background")
                },
                stopCurrentSequenceScopeToDisplay(editor.currentParams.has("targetScope")
                        ? editor.currentParams.get("targetScope").getAsString()
                        : "foreground"));
    }
}

