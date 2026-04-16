package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiKillAuraConfig extends ThemedGuiScreen {

    private static final int BTN_ROTATE = 1;
    private static final int BTN_SMOOTH_ROTATE = 2;
    private static final int BTN_LINE_OF_SIGHT = 3;
    private static final int BTN_ONLY_WEAPON = 4;
    private static final int BTN_HOSTILE = 5;
    private static final int BTN_PASSIVE = 6;
    private static final int BTN_PLAYERS = 7;
    private static final int BTN_FOCUS = 8;
    private static final int BTN_IGNORE_INVISIBLE = 9;
    private static final int BTN_AIM_ONLY = 10;
    private static final int BTN_NO_COLLISION = 13;
    private static final int BTN_ANTI_KNOCKBACK = 14;
    private static final int BTN_ATTACK_MODE = 15;
    private static final int BTN_HUNT_ENABLED = 16;
    private static final int BTN_HUNT_VISUALIZE = 17;
    private static final int BTN_FULL_BRIGHT = 18;
    private static final int BTN_HUNT_PICKUP = 19;

    private static final int BTN_RANGE = 20;
    private static final int BTN_MIN_STRENGTH = 21;
    private static final int BTN_MIN_TURN_SPEED = 22;
    private static final int BTN_MAX_TURN_SPEED = 23;
    private static final int BTN_INTERVAL = 24;
    private static final int BTN_HUNT_RADIUS = 25;
    private static final int BTN_FULL_BRIGHT_GAMMA = 26;
    private static final int BTN_NAME_WHITELIST = 27;
    private static final int BTN_NAME_BLACKLIST = 28;
    private static final int BTN_SCAN_RANGE = 29;
    private static final int BTN_SCAN_NEARBY = 30;
    private static final int BTN_ADD_SELECTED_WHITELIST = 31;
    private static final int BTN_ADD_SELECTED_BLACKLIST = 32;
    private static final int BTN_ADD_MANUAL_WHITELIST = 33;
    private static final int BTN_ADD_MANUAL_BLACKLIST = 34;
    private static final int BTN_TARGETS_PER_ATTACK = 35;
    private static final int BTN_ATTACK_SEQUENCE = 36;
    private static final int BTN_ATTACK_SEQUENCE_DELAY = 37;
    private static final int BTN_HUNT_FIXED_DISTANCE = 38;
    private static final int BTN_HUNT_ORBIT = 39;
    private static final int BTN_AIM_YAW_OFFSET = 40;
    private static final int BTN_HUNT_JUMP_ORBIT = 41;
    private static final int BTN_HUNT_ORBIT_SAMPLE_POINTS = 42;

    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;
    private static final int BTN_GROUP_PREV = 103;
    private static final int BTN_GROUP_NEXT = 104;
    private static final int BTN_PRESET_SAVE_NEW = 105;
    private static final int BTN_PRESET_APPLY = 106;
    private static final int BTN_PRESET_OVERWRITE = 107;
    private static final int BTN_PRESET_RENAME = 108;
    private static final int BTN_PRESET_DELETE = 109;

    private static final int NAME_LIST_CARD_HEIGHT = 18;
    private static final int NAME_LIST_CARD_GAP = 4;
    private static final int NAME_LIST_HEADER_HEIGHT = 14;
    private static final int NAME_LIST_INNER_PADDING = 4;
    private static final int NAME_LIST_BOX_HEIGHT = 108;
    private static final int NAME_LIST_HEADER_BUTTON_GAP = 2;
    private static final int GROUP_TAB_HEIGHT = 20;
    private static final int GROUP_TAB_GAP = 6;
    private static final int GROUP_SCROLLBAR_HEIGHT = 4;
    private static final int PRESET_CARD_HEIGHT = 24;
    private static final int PRESET_CARD_GAP = 4;

    private final GuiScreen parentScreen;
    private final List<String> instructionLines = new ArrayList<>();
    private final List<KillAuraHandler.KillAuraPreset> presetCards = new ArrayList<>();

    private ToggleGuiButton rotateButton;
    private ToggleGuiButton smoothRotateButton;
    private ToggleGuiButton lineOfSightButton;
    private ToggleGuiButton hostileButton;
    private ToggleGuiButton passiveButton;
    private ToggleGuiButton playersButton;
    private ToggleGuiButton onlyWeaponButton;
    private ToggleGuiButton focusButton;
    private ToggleGuiButton ignoreInvisibleButton;
    private ToggleGuiButton aimOnlyButton;
    private ToggleGuiButton noCollisionButton;
    private ToggleGuiButton antiKnockbackButton;
    private ToggleGuiButton fullBrightButton;
    private ToggleGuiButton huntVisualizeButton;
    private ToggleGuiButton huntPickupButton;
    private ToggleGuiButton huntOrbitButton;
    private ToggleGuiButton huntJumpOrbitButton;
    private ToggleGuiButton nameWhitelistButton;
    private ToggleGuiButton nameBlacklistButton;

    private GuiButton attackModeButton;
    private GuiButton aimYawOffsetButton;
    private GuiButton rangeButton;
    private GuiButton minStrengthButton;
    private GuiButton minTurnSpeedButton;
    private GuiButton maxTurnSpeedButton;
    private GuiButton intervalButton;
    private GuiButton targetsPerAttackButton;
    private GuiButton attackSequenceButton;
    private GuiButton attackSequenceDelayButton;
    private GuiButton huntRadiusButton;
    private GuiButton huntFixedDistanceButton;
    private GuiButton huntOrbitSamplePointsButton;
    private GuiButton fullBrightGammaButton;
    private GuiButton scanRangeButton;
    private GuiButton scanNearbyButton;
    private GuiButton addSelectedWhitelistButton;
    private GuiButton addSelectedBlacklistButton;
    private GuiButton addManualWhitelistButton;
    private GuiButton addManualBlacklistButton;

    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private GuiButton groupPrevButton;
    private GuiButton groupNextButton;
    private GuiButton presetSaveNewButton;
    private GuiButton presetApplyButton;
    private GuiButton presetOverwriteButton;
    private GuiButton presetRenameButton;
    private GuiButton presetDeleteButton;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int contentTop;
    private int contentBottom;
    private int contentScroll;
    private int contentMaxScroll;
    private int contentFrameX;
    private int contentFrameY;
    private int contentFrameW;
    private int contentFrameH;

    private int groupBarX;
    private int groupBarY;
    private int groupBarW;
    private int groupBarH;
    private int groupTabsX;
    private int groupTabsY;
    private int groupTabsW;
    private int groupTabsH;
    private int groupTabScroll = 0;
    private int groupTabMaxScroll = 0;
    private int groupTabContentWidth = 0;
    private int presetPanelX;
    private int presetPanelY;
    private int presetPanelW;
    private int presetPanelH;
    private int presetToolbarX;
    private int presetToolbarY;
    private int presetToolbarW;
    private int presetListX;
    private int presetListY;
    private int presetListW;
    private int presetListH;
    private int presetScrollOffset = 0;
    private int presetMaxScroll = 0;
    private int selectedPresetIndex = -1;

    private int nameFilterSectionX;
    private int nameFilterSectionY;
    private int nameFilterSectionW;
    private int nameFilterSectionH;

    private int nearbyDropdownX;
    private int nearbyDropdownY;
    private int nearbyDropdownW;
    private int nearbyDropdownH;

    private int whitelistBoxX;
    private int whitelistBoxY;
    private int whitelistBoxW;
    private int whitelistBoxH;

    private int blacklistBoxX;
    private int blacklistBoxY;
    private int blacklistBoxW;
    private int blacklistBoxH;

    private int whitelistListScroll = 0;
    private int blacklistListScroll = 0;
    private int selectedWhitelistIndex = -1;
    private boolean whitelistDragActive = false;
    private int whitelistDragIndex = -1;

    private final NearbyNameDropdown nearbyNameDropdown = new NearbyNameDropdown();
    private final AttackModeDropdown attackModeDropdown = new AttackModeDropdown();
    private final HuntModeDropdown huntModeDropdown = new HuntModeDropdown();
    private ConfigGroup selectedGroup = ConfigGroup.ATTACK;

    public GuiKillAuraConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        recalcLayout();
        initButtons();
        refreshNearbyEntityOptions(false);
        relayoutButtons();
    }

    private void recalcLayout() {
        int maxWidth = Math.min(468, Math.max(260, this.width - 12));
        int maxHeight = Math.min(560, Math.max(300, this.height - 12));

        this.panelWidth = Math.min(maxWidth, Math.max(240, this.width - 8));
        this.panelHeight = Math.min(maxHeight, Math.max(300, this.height - 8));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.instructionLines.clear();
        String instruction = "顶部可用滚轮或左右方向键切换分组；下方只显示当前分组内容，方便集中修改。";
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(instruction, instructionWidth));

        int instructionHeight = this.instructionLines.size() * 10;
        int footerTop = this.panelY + this.panelHeight - 28;
        this.groupBarX = this.panelX + 10;
        this.groupBarY = this.panelY + 24 + instructionHeight + 6;
        this.groupBarW = this.panelWidth - 20;
        this.groupBarH = GROUP_TAB_HEIGHT + GROUP_SCROLLBAR_HEIGHT + 14;
        this.groupTabsX = this.groupBarX + 32;
        this.groupTabsY = this.groupBarY + 5;
        this.groupTabsW = this.groupBarW - 64;
        this.groupTabsH = GROUP_TAB_HEIGHT;

        this.contentFrameX = this.panelX + 10;
        this.contentFrameY = this.groupBarY + this.groupBarH + 8;
        this.contentFrameW = this.panelWidth - 20;
        this.contentFrameH = Math.max(80, footerTop - 8 - this.contentFrameY);

        this.contentTop = this.contentFrameY + 22;
        this.contentBottom = this.contentFrameY + this.contentFrameH - 8;

        if (this.contentBottom < this.contentTop + 24) {
            this.contentBottom = this.contentTop + 24;
        }
    }

    private void initButtons() {
        rotateButton = new ToggleGuiButton(BTN_ROTATE, 0, 0, 100, 20, "", KillAuraHandler.rotateToTarget);
        smoothRotateButton = new ToggleGuiButton(BTN_SMOOTH_ROTATE, 0, 0, 100, 20, "", KillAuraHandler.smoothRotation);
        lineOfSightButton = new ToggleGuiButton(BTN_LINE_OF_SIGHT, 0, 0, 100, 20, "",
                KillAuraHandler.requireLineOfSight);
        onlyWeaponButton = new ToggleGuiButton(BTN_ONLY_WEAPON, 0, 0, 100, 20, "", KillAuraHandler.onlyWeapon);
        hostileButton = new ToggleGuiButton(BTN_HOSTILE, 0, 0, 100, 20, "", KillAuraHandler.targetHostile);
        passiveButton = new ToggleGuiButton(BTN_PASSIVE, 0, 0, 100, 20, "", KillAuraHandler.targetPassive);
        playersButton = new ToggleGuiButton(BTN_PLAYERS, 0, 0, 100, 20, "", KillAuraHandler.targetPlayers);
        focusButton = new ToggleGuiButton(BTN_FOCUS, 0, 0, 100, 20, "", KillAuraHandler.focusSingleTarget);
        ignoreInvisibleButton = new ToggleGuiButton(BTN_IGNORE_INVISIBLE, 0, 0, 100, 20, "",
                KillAuraHandler.ignoreInvisible);
        aimOnlyButton = new ToggleGuiButton(BTN_AIM_ONLY, 0, 0, 100, 20, "", KillAuraHandler.aimOnlyMode);
        noCollisionButton = new ToggleGuiButton(BTN_NO_COLLISION, 0, 0, 100, 20, "", KillAuraHandler.enableNoCollision);
        antiKnockbackButton = new ToggleGuiButton(BTN_ANTI_KNOCKBACK, 0, 0, 100, 20, "",
                KillAuraHandler.enableAntiKnockback);
        fullBrightButton = new ToggleGuiButton(BTN_FULL_BRIGHT, 0, 0, 100, 20, "",
                KillAuraHandler.enableFullBrightVision);
        huntPickupButton = new ToggleGuiButton(BTN_HUNT_PICKUP, 0, 0, 100, 20, "",
                KillAuraHandler.huntPickupItemsEnabled);
        huntVisualizeButton = new ToggleGuiButton(BTN_HUNT_VISUALIZE, 0, 0, 100, 20, "",
                KillAuraHandler.visualizeHuntRadius);
        huntOrbitButton = new ToggleGuiButton(BTN_HUNT_ORBIT, 0, 0, 100, 20, "",
                KillAuraHandler.huntOrbitEnabled);
        huntJumpOrbitButton = new ToggleGuiButton(BTN_HUNT_JUMP_ORBIT, 0, 0, 100, 20, "",
                KillAuraHandler.huntJumpOrbitEnabled);
        nameWhitelistButton = new ToggleGuiButton(BTN_NAME_WHITELIST, 0, 0, 100, 20, "",
                KillAuraHandler.enableNameWhitelist);
        nameBlacklistButton = new ToggleGuiButton(BTN_NAME_BLACKLIST, 0, 0, 100, 20, "",
                KillAuraHandler.enableNameBlacklist);

        attackModeButton = new ThemedButton(BTN_ATTACK_MODE, 0, 0, 100, 20, "");
        aimYawOffsetButton = new ThemedButton(BTN_AIM_YAW_OFFSET, 0, 0, 100, 20, "");
        rangeButton = new ThemedButton(BTN_RANGE, 0, 0, 100, 20, "");
        minStrengthButton = new ThemedButton(BTN_MIN_STRENGTH, 0, 0, 100, 20, "");
        minTurnSpeedButton = new ThemedButton(BTN_MIN_TURN_SPEED, 0, 0, 100, 20, "");
        maxTurnSpeedButton = new ThemedButton(BTN_MAX_TURN_SPEED, 0, 0, 100, 20, "");
        intervalButton = new ThemedButton(BTN_INTERVAL, 0, 0, 100, 20, "");
        targetsPerAttackButton = new ThemedButton(BTN_TARGETS_PER_ATTACK, 0, 0, 100, 20, "");
        attackSequenceButton = new ThemedButton(BTN_ATTACK_SEQUENCE, 0, 0, 100, 20, "");
        attackSequenceDelayButton = new ThemedButton(BTN_ATTACK_SEQUENCE_DELAY, 0, 0, 100, 20, "");
        huntRadiusButton = new ThemedButton(BTN_HUNT_RADIUS, 0, 0, 100, 20, "");
        huntFixedDistanceButton = new ThemedButton(BTN_HUNT_FIXED_DISTANCE, 0, 0, 100, 20, "");
        huntOrbitSamplePointsButton = new ThemedButton(BTN_HUNT_ORBIT_SAMPLE_POINTS, 0, 0, 100, 20, "");
        fullBrightGammaButton = new ThemedButton(BTN_FULL_BRIGHT_GAMMA, 0, 0, 100, 20, "");
        scanRangeButton = new ThemedButton(BTN_SCAN_RANGE, 0, 0, 100, 20, "");
        scanNearbyButton = new ThemedButton(BTN_SCAN_NEARBY, 0, 0, 100, 20, "");
        addSelectedWhitelistButton = new ThemedButton(BTN_ADD_SELECTED_WHITELIST, 0, 0, 100, 20, "");
        addSelectedBlacklistButton = new ThemedButton(BTN_ADD_SELECTED_BLACKLIST, 0, 0, 100, 20, "");
        addManualWhitelistButton = new ThemedButton(BTN_ADD_MANUAL_WHITELIST, 0, 0, 100, 20, "");
        addManualBlacklistButton = new ThemedButton(BTN_ADD_MANUAL_BLACKLIST, 0, 0, 100, 20, "");

        saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, 20, "§a保存并关闭");
        defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, 20, "§e恢复默认");
        cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, 20, "取消");
        groupPrevButton = new ThemedButton(BTN_GROUP_PREV, 0, 0, 24, 20, "<");
        groupNextButton = new ThemedButton(BTN_GROUP_NEXT, 0, 0, 24, 20, ">");
        presetSaveNewButton = new ThemedButton(BTN_PRESET_SAVE_NEW, 0, 0, 100, 20, "新增预设");
        presetApplyButton = new ThemedButton(BTN_PRESET_APPLY, 0, 0, 100, 20, "使用当前预设");
        presetOverwriteButton = new ThemedButton(BTN_PRESET_OVERWRITE, 0, 0, 100, 20, "保存当前预设");
        presetRenameButton = new ThemedButton(BTN_PRESET_RENAME, 0, 0, 100, 20, "重命名");
        presetDeleteButton = new ThemedButton(BTN_PRESET_DELETE, 0, 0, 100, 20, "删除");

        this.buttonList.add(rotateButton);
        this.buttonList.add(smoothRotateButton);
        this.buttonList.add(lineOfSightButton);
        this.buttonList.add(onlyWeaponButton);
        this.buttonList.add(hostileButton);
        this.buttonList.add(passiveButton);
        this.buttonList.add(playersButton);
        this.buttonList.add(focusButton);
        this.buttonList.add(ignoreInvisibleButton);
        this.buttonList.add(aimOnlyButton);
        this.buttonList.add(noCollisionButton);
        this.buttonList.add(antiKnockbackButton);
        this.buttonList.add(fullBrightButton);
        this.buttonList.add(huntPickupButton);
        this.buttonList.add(huntVisualizeButton);
        this.buttonList.add(huntOrbitButton);
        this.buttonList.add(huntJumpOrbitButton);
        this.buttonList.add(nameWhitelistButton);
        this.buttonList.add(nameBlacklistButton);
        this.buttonList.add(attackModeButton);
        this.buttonList.add(aimYawOffsetButton);
        this.buttonList.add(rangeButton);
        this.buttonList.add(minStrengthButton);
        this.buttonList.add(minTurnSpeedButton);
        this.buttonList.add(maxTurnSpeedButton);
        this.buttonList.add(intervalButton);
        this.buttonList.add(targetsPerAttackButton);
        this.buttonList.add(attackSequenceButton);
        this.buttonList.add(attackSequenceDelayButton);
        this.buttonList.add(huntRadiusButton);
        this.buttonList.add(huntFixedDistanceButton);
        this.buttonList.add(huntOrbitSamplePointsButton);
        this.buttonList.add(fullBrightGammaButton);
        this.buttonList.add(scanRangeButton);
        this.buttonList.add(scanNearbyButton);
        this.buttonList.add(addSelectedWhitelistButton);
        this.buttonList.add(addSelectedBlacklistButton);
        this.buttonList.add(addManualWhitelistButton);
        this.buttonList.add(addManualBlacklistButton);
        this.buttonList.add(groupPrevButton);
        this.buttonList.add(groupNextButton);
        this.buttonList.add(presetSaveNewButton);
        this.buttonList.add(presetApplyButton);
        this.buttonList.add(presetOverwriteButton);
        this.buttonList.add(presetRenameButton);
        this.buttonList.add(presetDeleteButton);
        this.buttonList.add(saveButton);
        this.buttonList.add(defaultButton);
        this.buttonList.add(cancelButton);

        refreshPresetCards();
        refreshButtonTexts();
    }

    private void refreshButtonTexts() {
        syncWhitelistSelectionState();
        refreshPresetCards();

        boolean packetMode = KillAuraHandler.ATTACK_MODE_PACKET.equalsIgnoreCase(KillAuraHandler.attackMode);
        boolean teleportMode = KillAuraHandler.ATTACK_MODE_TELEPORT.equalsIgnoreCase(KillAuraHandler.attackMode);
        boolean sequenceMode = KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(KillAuraHandler.attackMode);
        boolean aimOnly = KillAuraHandler.aimOnlyMode;
        boolean huntEnabled = KillAuraHandler.isHuntEnabled();

        rotateButton.setEnabledState(aimOnly || KillAuraHandler.rotateToTarget);
        rotateButton.displayString = aimOnly ? "攻击时转向目标: §b强制锁定"
                : "攻击时转向目标: " + stateText(KillAuraHandler.rotateToTarget);
        rotateButton.enabled = !aimOnly && !packetMode;

        smoothRotateButton.setEnabledState(KillAuraHandler.smoothRotation);
        smoothRotateButton.displayString = "平滑转向: " + stateText(KillAuraHandler.smoothRotation);
        smoothRotateButton.enabled = !packetMode || aimOnly;
        aimYawOffsetButton.displayString = "索敌视角偏移: " + formatSignedPreciseFloat(KillAuraHandler.aimYawOffset) + "°";
        aimYawOffsetButton.enabled = !packetMode || aimOnly;

        lineOfSightButton.setEnabledState(KillAuraHandler.requireLineOfSight);
        lineOfSightButton.displayString = "必须可见: " + stateText(KillAuraHandler.requireLineOfSight);

        onlyWeaponButton.setEnabledState(KillAuraHandler.onlyWeapon);
        onlyWeaponButton.displayString = "仅持武器生效: " + stateText(KillAuraHandler.onlyWeapon);
        onlyWeaponButton.enabled = !aimOnly && !sequenceMode;

        aimOnlyButton.setEnabledState(aimOnly);
        aimOnlyButton.displayString = "只瞄准不攻击: " + stateText(aimOnly);

        hostileButton.setEnabledState(KillAuraHandler.targetHostile);
        hostileButton.displayString = "攻击敌对生物: " + stateText(KillAuraHandler.targetHostile);

        passiveButton.setEnabledState(KillAuraHandler.targetPassive);
        passiveButton.displayString = "攻击被动生物: " + stateText(KillAuraHandler.targetPassive);

        playersButton.setEnabledState(KillAuraHandler.targetPlayers);
        playersButton.displayString = "攻击玩家: " + stateText(KillAuraHandler.targetPlayers);

        focusButton.setEnabledState(KillAuraHandler.focusSingleTarget);
        focusButton.displayString = "锁定单一目标: " + stateText(KillAuraHandler.focusSingleTarget);

        ignoreInvisibleButton.setEnabledState(KillAuraHandler.ignoreInvisible);
        ignoreInvisibleButton.displayString = "忽略隐身目标: " + stateText(KillAuraHandler.ignoreInvisible);

        noCollisionButton.setEnabledState(KillAuraHandler.enableNoCollision);
        noCollisionButton.displayString = "无碰撞: " + stateText(KillAuraHandler.enableNoCollision);

        antiKnockbackButton.setEnabledState(KillAuraHandler.enableAntiKnockback);
        antiKnockbackButton.displayString = "防击退: " + stateText(KillAuraHandler.enableAntiKnockback);

        fullBrightButton.setEnabledState(KillAuraHandler.enableFullBrightVision);
        fullBrightButton.displayString = "夜视提亮: " + stateText(KillAuraHandler.enableFullBrightVision);

        fullBrightGammaButton.displayString = "提亮强度: " + formatFloat(KillAuraHandler.fullBrightGamma);
        fullBrightGammaButton.enabled = true;

        huntPickupButton.setEnabledState(KillAuraHandler.huntPickupItemsEnabled);
        huntPickupButton.displayString = "优先拾取掉落物: " + stateText(KillAuraHandler.huntPickupItemsEnabled);

        huntVisualizeButton.setEnabledState(KillAuraHandler.visualizeHuntRadius);
        huntVisualizeButton.displayString = "显示追击半径光环: " + stateText(KillAuraHandler.visualizeHuntRadius);
        huntOrbitButton.setEnabledState(KillAuraHandler.huntOrbitEnabled);
        huntOrbitButton.displayString = "自动绕圈攻击: " + stateText(KillAuraHandler.huntOrbitEnabled);
        huntJumpOrbitButton.setEnabledState(KillAuraHandler.huntJumpOrbitEnabled);
        huntJumpOrbitButton.displayString = "跳跃绕圈: " + stateText(KillAuraHandler.huntJumpOrbitEnabled);
        huntOrbitSamplePointsButton.displayString = "轨道采样点: "
                + KillAuraHandler.getConfiguredHuntOrbitSamplePoints() + " 点";

        nameWhitelistButton.setEnabledState(KillAuraHandler.enableNameWhitelist);
        nameWhitelistButton.displayString = "启用名称白名单: " + stateText(KillAuraHandler.enableNameWhitelist);

        nameBlacklistButton.setEnabledState(KillAuraHandler.enableNameBlacklist);
        nameBlacklistButton.displayString = "启用名称黑名单: " + stateText(KillAuraHandler.enableNameBlacklist);

        String attackModeName = sequenceMode ? "执行序列"
                : (packetMode ? "数据包攻击" : (teleportMode ? "TP攻击" : "普通攻击"));
        attackModeButton.displayString = aimOnly ? "攻击模式: 执行序列(只瞄准默认)" : "攻击模式: " + attackModeName;
        attackModeButton.enabled = !aimOnly;
        attackModeDropdown.syncFromCurrentMode();
        attackModeDropdown.setEnabled(!aimOnly);
        huntModeDropdown.syncFromCurrentMode();
        huntModeDropdown.setEnabled(true);

        String configuredSequenceName = safe(KillAuraHandler.attackSequenceName).trim();
        boolean sequenceExists = !configuredSequenceName.isEmpty()
                && PathSequenceManager.hasSequence(configuredSequenceName);
        String sequenceText;
        if (configuredSequenceName.isEmpty()) {
            sequenceText = "攻击序列: 点击选择序列";
        } else if (sequenceExists) {
            sequenceText = "攻击序列: §f" + configuredSequenceName;
        } else {
            sequenceText = "攻击序列: §c序列不存在";
        }
        attackSequenceButton.displayString = trimToWidth(sequenceText, Math.max(40, attackSequenceButton.width - 10));
        attackSequenceButton.enabled = sequenceMode;
        attackSequenceDelayButton.displayString = "执行延迟: " + KillAuraHandler.attackSequenceDelayTicks + " Tick";
        attackSequenceDelayButton.enabled = sequenceMode;

        huntRadiusButton.displayString = "追击半径: " + formatFloat(KillAuraHandler.huntRadius) + " 格";
        huntFixedDistanceButton.displayString = "固定距离: " + formatFloat(KillAuraHandler.huntFixedDistance) + " 格";
        huntRadiusButton.enabled = huntEnabled;
        huntFixedDistanceButton.enabled = huntEnabled && KillAuraHandler.isHuntFixedDistanceMode();
        huntOrbitButton.enabled = huntEnabled && KillAuraHandler.isHuntFixedDistanceMode();
        huntJumpOrbitButton.enabled = huntEnabled
                && KillAuraHandler.isHuntFixedDistanceMode()
                && KillAuraHandler.huntOrbitEnabled;
        huntOrbitSamplePointsButton.enabled = huntEnabled
                && KillAuraHandler.isHuntFixedDistanceMode()
                && KillAuraHandler.huntOrbitEnabled;
        huntPickupButton.enabled = huntEnabled;
        huntVisualizeButton.enabled = huntEnabled;

        rangeButton.displayString = "攻击范围: " + formatFloat(KillAuraHandler.attackRange) + " 格";
        minStrengthButton.displayString = "最小攻击蓄力: " + formatFloat(KillAuraHandler.minAttackStrength);
        minTurnSpeedButton.displayString = "最小转速: " + formatFloat(KillAuraHandler.minTurnSpeed);
        maxTurnSpeedButton.displayString = "最大转速: " + formatFloat(KillAuraHandler.maxTurnSpeed);
        intervalButton.displayString = "最小攻击间隔: " + KillAuraHandler.minAttackIntervalTicks + " Tick";
        targetsPerAttackButton.displayString = "单次攻击目标数: " + KillAuraHandler.targetsPerAttack + " 个";
        minStrengthButton.enabled = !aimOnly && !sequenceMode;
        intervalButton.enabled = !aimOnly && !sequenceMode;
        targetsPerAttackButton.enabled = !aimOnly && !sequenceMode;

        scanRangeButton.displayString = "获取范围: " + formatFloat(KillAuraHandler.nearbyEntityScanRange) + " 格";
        scanNearbyButton.displayString = "获取周围实体名称";

        String selectedName = nearbyNameDropdown.getSelectedValue();
        boolean hasSelectedNearby = !isBlank(selectedName);
        addSelectedWhitelistButton.displayString = hasSelectedNearby ? "选中加入白名单" : "先获取并选择实体";
        addSelectedBlacklistButton.displayString = hasSelectedNearby ? "选中加入黑名单" : "先获取并选择实体";
        addSelectedWhitelistButton.enabled = hasSelectedNearby;
        addSelectedBlacklistButton.enabled = hasSelectedNearby;

        addManualWhitelistButton.displayString = "手动添加白名单项";
        addManualBlacklistButton.displayString = "手动添加黑名单项";

        groupPrevButton.enabled = this.selectedGroup.ordinal() > 0;
        groupNextButton.enabled = this.selectedGroup.ordinal() < ConfigGroup.values().length - 1;

        boolean hasSelectedPreset = selectedPresetIndex >= 0 && selectedPresetIndex < presetCards.size();
        presetApplyButton.enabled = hasSelectedPreset;
        presetOverwriteButton.enabled = hasSelectedPreset;
        presetRenameButton.enabled = hasSelectedPreset;
        presetDeleteButton.enabled = hasSelectedPreset;
    }

    private void relayoutButtons() {
        recalcLayout();
        refreshPresetCards();
        recalcGroupTabs();
        hideAllContentButtons();

        int innerPadding = 12;
        int columnGap = 8;
        int buttonHeight = 20;
        int rowGap = 6;
        int rowStep = buttonHeight + rowGap;
        int fullWidth = Math.max(120, this.contentFrameW - innerPadding * 2);
        int buttonW = Math.max(92, (fullWidth - columnGap) / 2);
        int leftX = this.contentFrameX + innerPadding;
        int rightX = leftX + buttonW + columnGap;
        boolean packetMode = KillAuraHandler.ATTACK_MODE_PACKET.equalsIgnoreCase(KillAuraHandler.attackMode);
        boolean sequenceMode = KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(KillAuraHandler.attackMode);

        int totalContentHeight = layoutSelectedGroup(leftX, rightX, fullWidth, buttonW, buttonHeight, rowStep,
                packetMode, sequenceMode, false);
        int visibleContentHeight = Math.max(24, this.contentBottom - this.contentTop);
        this.contentMaxScroll = Math.max(0, totalContentHeight - visibleContentHeight);
        this.contentScroll = clampInt(this.contentScroll, 0, this.contentMaxScroll);

        hideAllContentButtons();
        layoutSelectedGroup(leftX, rightX, fullWidth, buttonW, buttonHeight, rowStep, packetMode, sequenceMode, true);

        int footerY = this.panelY + this.panelHeight - 28;
        int footerGap = 6;
        int footerButtonW = Math.max(64, (this.panelWidth - innerPadding * 2 - footerGap * 2) / 3);
        int footerTotalW = footerButtonW * 3 + footerGap * 2;
        int footerStartX = this.panelX + (this.panelWidth - footerTotalW) / 2;

        layoutFixedButton(groupPrevButton, this.groupBarX + 6, this.groupTabsY, 20, this.groupTabsH);
        layoutFixedButton(groupNextButton, this.groupBarX + this.groupBarW - 26, this.groupTabsY, 20, this.groupTabsH);

        layoutFixedButton(saveButton, footerStartX, footerY, footerButtonW, 20);
        layoutFixedButton(defaultButton, footerStartX + footerButtonW + footerGap, footerY, footerButtonW, 20);
        layoutFixedButton(cancelButton, footerStartX + (footerButtonW + footerGap) * 2, footerY, footerButtonW, 20);

        this.whitelistListScroll = clampInt(this.whitelistListScroll, 0,
                getNameListMaxScroll(KillAuraHandler.nameWhitelist, this.whitelistBoxH));
        this.blacklistListScroll = clampInt(this.blacklistListScroll, 0,
                getNameListMaxScroll(KillAuraHandler.nameBlacklist, this.blacklistBoxH));
        syncWhitelistSelectionState();
    }

    private int layoutSelectedGroup(int leftX, int rightX, int fullWidth, int buttonW, int buttonHeight, int rowStep,
            boolean packetMode, boolean sequenceMode, boolean layout) {
        switch (this.selectedGroup) {
        case PRESET:
            return layoutPresetGroup(leftX, fullWidth, buttonHeight, rowStep, layout);
        case ATTACK:
            return layoutAttackGroup(leftX, rightX, buttonW, buttonHeight, rowStep, packetMode, sequenceMode, layout);
        case TARGET:
            return layoutTargetGroup(leftX, rightX, buttonW, buttonHeight, rowStep, layout);
        case NAME_FILTER:
            return layoutNameFilterGroup(leftX, rightX, fullWidth, buttonW, buttonHeight, rowStep, layout);
        case HUNT:
            return layoutHuntGroup(leftX, rightX, fullWidth, buttonW, buttonHeight, rowStep, layout);
        case PROTECTION:
            return layoutProtectionGroup(leftX, rightX, buttonW, buttonHeight, rowStep, layout);
        case STATS:
        default:
            return layoutStatsGroup(leftX, rightX, buttonW, buttonHeight, rowStep, layout);
        }
    }

    private int layoutPresetGroup(int leftX, int fullWidth, int buttonHeight, int rowStep, boolean layout) {
        int currentY = 0;
        int gap = 4;
        int row1W = Math.max(70, (fullWidth - gap * 2) / 3);
        int row2W = Math.max(90, (fullWidth - gap) / 2);

        if (layout) {
            layoutScrollableButton(presetSaveNewButton, leftX, currentY, row1W, buttonHeight);
            layoutScrollableButton(presetApplyButton, leftX + row1W + gap, currentY, row1W, buttonHeight);
            layoutScrollableButton(presetOverwriteButton, leftX + (row1W + gap) * 2, currentY, row1W, buttonHeight);
        }
        currentY += rowStep;

        if (layout) {
            layoutScrollableButton(presetRenameButton, leftX, currentY, row2W, buttonHeight);
            layoutScrollableButton(presetDeleteButton, leftX + row2W + gap, currentY, row2W, buttonHeight);
        }
        currentY += rowStep;

        this.presetListX = leftX;
        this.presetListY = this.contentTop + currentY - this.contentScroll;
        this.presetListW = fullWidth;
        this.presetListH = Math.max(80, this.contentBottom - this.presetListY - 4);
        return currentY + this.presetListH + 4;
    }

    private int layoutAttackGroup(int leftX, int rightX, int buttonW, int buttonHeight, int rowStep, boolean packetMode,
            boolean sequenceMode, boolean layout) {
        boolean showRotationControls = !packetMode || KillAuraHandler.aimOnlyMode;
        int currentY = 0;
        int fullButtonWidth = rightX + buttonW - leftX;

        if (layout) {
            this.attackModeDropdown.setBounds(leftX, this.contentTop + currentY - this.contentScroll, fullButtonWidth,
                    buttonHeight);
            this.attackModeDropdown.syncFromCurrentMode();
            this.attackModeDropdown.setEnabled(!KillAuraHandler.aimOnlyMode);
        }

        currentY += rowStep;
        if (sequenceMode) {
            placeContentButton(attackSequenceButton, leftX, currentY, fullButtonWidth, buttonHeight, layout);
            currentY += rowStep;
            placeContentButton(attackSequenceDelayButton, leftX, currentY, buttonW, buttonHeight, layout);
            placeContentButton(aimOnlyButton, rightX, currentY, buttonW, buttonHeight, layout);
        } else {
            placeContentButton(onlyWeaponButton, leftX, currentY, buttonW, buttonHeight, layout);
            placeContentButton(aimOnlyButton, rightX, currentY, buttonW, buttonHeight, layout);
        }

        currentY += rowStep;
        placeContentButton(lineOfSightButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(focusButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        if (showRotationControls) {
            placeContentButton(rotateButton, leftX, currentY, buttonW, buttonHeight, layout);
            placeContentButton(smoothRotateButton, rightX, currentY, buttonW, buttonHeight, layout);
            currentY += rowStep;
            placeContentButton(aimYawOffsetButton, leftX, currentY, fullButtonWidth, buttonHeight, layout);
        } else {
            if (layout) {
                hideButton(rotateButton);
                hideButton(smoothRotateButton);
                hideButton(aimYawOffsetButton);
            }
            currentY -= rowStep;
        }

        return currentY + buttonHeight + 4;
    }

    private int layoutTargetGroup(int leftX, int rightX, int buttonW, int buttonHeight, int rowStep, boolean layout) {
        int currentY = 0;
        placeContentButton(hostileButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(passiveButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(playersButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(ignoreInvisibleButton, rightX, currentY, buttonW, buttonHeight, layout);
        return currentY + buttonHeight + 4;
    }

    private int layoutNameFilterGroup(int leftX, int rightX, int fullWidth, int buttonW, int buttonHeight, int rowStep,
            boolean layout) {
        int currentY = 0;
        placeContentButton(nameWhitelistButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(nameBlacklistButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(scanRangeButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(scanNearbyButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        if (layout) {
            this.nearbyDropdownX = leftX;
            this.nearbyDropdownY = this.contentTop + currentY - this.contentScroll;
            this.nearbyDropdownW = fullWidth;
            this.nearbyDropdownH = buttonHeight;
            this.nearbyNameDropdown.setBounds(nearbyDropdownX, nearbyDropdownY, nearbyDropdownW, nearbyDropdownH);
        }

        currentY += rowStep;
        placeContentButton(addSelectedWhitelistButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(addSelectedBlacklistButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(addManualWhitelistButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(addManualBlacklistButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        if (layout) {
            this.whitelistBoxX = leftX;
            this.whitelistBoxY = this.contentTop + currentY - this.contentScroll;
            this.whitelistBoxW = buttonW;
            this.whitelistBoxH = NAME_LIST_BOX_HEIGHT;

            this.blacklistBoxX = rightX;
            this.blacklistBoxY = this.contentTop + currentY - this.contentScroll;
            this.blacklistBoxW = buttonW;
            this.blacklistBoxH = NAME_LIST_BOX_HEIGHT;
        }

        return currentY + NAME_LIST_BOX_HEIGHT + 4;
    }

    private int layoutHuntGroup(int leftX, int rightX, int fullWidth, int buttonW, int buttonHeight, int rowStep,
            boolean layout) {
        int currentY = 0;
        if (layout) {
            this.huntModeDropdown.setBounds(leftX, this.contentTop + currentY - this.contentScroll, fullWidth,
                    buttonHeight);
            this.huntModeDropdown.syncFromCurrentMode();
            this.huntModeDropdown.setEnabled(true);
        }

        currentY += rowStep;
        if (KillAuraHandler.isHuntFixedDistanceMode()) {
            placeContentButton(huntFixedDistanceButton, leftX, currentY, fullWidth, buttonHeight, layout);
            currentY += rowStep;
            placeContentButton(huntOrbitButton, leftX, currentY, fullWidth, buttonHeight, layout);
            if (KillAuraHandler.huntOrbitEnabled) {
                currentY += rowStep;
                placeContentButton(huntJumpOrbitButton, leftX, currentY, fullWidth, buttonHeight, layout);
                currentY += rowStep;
                placeContentButton(huntOrbitSamplePointsButton, leftX, currentY, fullWidth, buttonHeight, layout);
            } else if (layout) {
                hideButton(huntJumpOrbitButton);
                hideButton(huntOrbitSamplePointsButton);
            }
            currentY += rowStep;
        } else if (layout) {
            hideButton(huntFixedDistanceButton);
            hideButton(huntOrbitButton);
            hideButton(huntJumpOrbitButton);
            hideButton(huntOrbitSamplePointsButton);
        }

        placeContentButton(huntRadiusButton, leftX, currentY, fullWidth, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(huntPickupButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(huntVisualizeButton, rightX, currentY, buttonW, buttonHeight, layout);
        return currentY + buttonHeight + 4;
    }

    private int layoutProtectionGroup(int leftX, int rightX, int buttonW, int buttonHeight, int rowStep,
            boolean layout) {
        int currentY = 0;
        placeContentButton(noCollisionButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(antiKnockbackButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(fullBrightButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(fullBrightGammaButton, rightX, currentY, buttonW, buttonHeight, layout);
        return currentY + buttonHeight + 4;
    }

    private int layoutStatsGroup(int leftX, int rightX, int buttonW, int buttonHeight, int rowStep, boolean layout) {
        int currentY = 0;
        placeContentButton(rangeButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(minStrengthButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(minTurnSpeedButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(maxTurnSpeedButton, rightX, currentY, buttonW, buttonHeight, layout);

        currentY += rowStep;
        placeContentButton(intervalButton, leftX, currentY, buttonW, buttonHeight, layout);
        placeContentButton(targetsPerAttackButton, rightX, currentY, buttonW, buttonHeight, layout);
        return currentY + buttonHeight + 4;
    }

    private void placeContentButton(GuiButton button, int x, int baseY, int width, int height, boolean layout) {
        if (!layout || button == null) {
            return;
        }
        layoutScrollableButton(button, x, baseY, width, height);
    }

    private void hideAllContentButtons() {
        hideButton(rotateButton);
        hideButton(smoothRotateButton);
        hideButton(lineOfSightButton);
        hideButton(onlyWeaponButton);
        hideButton(hostileButton);
        hideButton(passiveButton);
        hideButton(playersButton);
        hideButton(focusButton);
        hideButton(ignoreInvisibleButton);
        hideButton(aimOnlyButton);
        hideButton(noCollisionButton);
        hideButton(antiKnockbackButton);
        hideButton(fullBrightButton);
        hideButton(huntPickupButton);
        hideButton(huntVisualizeButton);
        hideButton(huntOrbitButton);
        hideButton(huntJumpOrbitButton);
        hideButton(nameWhitelistButton);
        hideButton(nameBlacklistButton);
        hideButton(attackModeButton);
        hideButton(aimYawOffsetButton);
        hideButton(rangeButton);
        hideButton(minStrengthButton);
        hideButton(minTurnSpeedButton);
        hideButton(maxTurnSpeedButton);
        hideButton(intervalButton);
        hideButton(targetsPerAttackButton);
        hideButton(attackSequenceButton);
        hideButton(attackSequenceDelayButton);
        hideButton(huntRadiusButton);
        hideButton(huntFixedDistanceButton);
        hideButton(huntOrbitSamplePointsButton);
        hideButton(fullBrightGammaButton);
        hideButton(scanRangeButton);
        hideButton(scanNearbyButton);
        hideButton(addSelectedWhitelistButton);
        hideButton(addSelectedBlacklistButton);
        hideButton(addManualWhitelistButton);
        hideButton(addManualBlacklistButton);
        hideButton(presetSaveNewButton);
        hideButton(presetApplyButton);
        hideButton(presetOverwriteButton);
        hideButton(presetRenameButton);
        hideButton(presetDeleteButton);
        hideButton(saveButton);
        hideButton(defaultButton);
        hideButton(cancelButton);
    }

    private void refreshPresetCards() {
        this.presetCards.clear();
        this.presetCards.addAll(KillAuraHandler.getPresetSnapshots());
        if (this.presetCards.isEmpty()) {
            this.selectedPresetIndex = -1;
            this.presetScrollOffset = 0;
            this.presetMaxScroll = 0;
            return;
        }
        this.selectedPresetIndex = clampInt(this.selectedPresetIndex, 0, this.presetCards.size() - 1);
        this.presetMaxScroll = Math.max(0, this.presetCards.size() - getPresetVisibleRows());
        this.presetScrollOffset = clampInt(this.presetScrollOffset, 0, this.presetMaxScroll);
        ensureSelectedPresetVisible();
    }

    private int getPresetVisibleRows() {
        int usable = Math.max(1, this.presetListH - 8);
        return Math.max(1, usable / (PRESET_CARD_HEIGHT + PRESET_CARD_GAP));
    }

    private void ensureSelectedPresetVisible() {
        if (this.selectedPresetIndex < 0) {
            return;
        }
        int visibleRows = getPresetVisibleRows();
        if (this.selectedPresetIndex < this.presetScrollOffset) {
            this.presetScrollOffset = this.selectedPresetIndex;
        } else if (this.selectedPresetIndex >= this.presetScrollOffset + visibleRows) {
            this.presetScrollOffset = this.selectedPresetIndex - visibleRows + 1;
        }
        this.presetScrollOffset = clampInt(this.presetScrollOffset, 0, this.presetMaxScroll);
    }

    private int getPresetCardIndexAt(int mouseX, int mouseY) {
        if (!isMouseInside(mouseX, mouseY, this.presetListX, this.presetListY, this.presetListW, this.presetListH)) {
            return -1;
        }
        int visibleRows = getPresetVisibleRows();
        int localY = mouseY - (this.presetListY + 4);
        if (localY < 0) {
            return -1;
        }
        int row = localY / (PRESET_CARD_HEIGHT + PRESET_CARD_GAP);
        if (row < 0 || row >= visibleRows) {
            return -1;
        }
        int presetIndex = this.presetScrollOffset + row;
        return presetIndex >= 0 && presetIndex < this.presetCards.size() ? presetIndex : -1;
    }

    private String getSelectedPresetName() {
        if (this.selectedPresetIndex < 0 || this.selectedPresetIndex >= this.presetCards.size()) {
            return "";
        }
        KillAuraHandler.KillAuraPreset preset = this.presetCards.get(this.selectedPresetIndex);
        return preset == null ? "" : safe(preset.name).trim();
    }

    private void recalcGroupTabs() {
        this.groupTabContentWidth = 0;
        ConfigGroup[] groups = ConfigGroup.values();
        for (int i = 0; i < groups.length; i++) {
            this.groupTabContentWidth += getGroupTabWidth(groups[i]);
            if (i < groups.length - 1) {
                this.groupTabContentWidth += GROUP_TAB_GAP;
            }
        }
        this.groupTabMaxScroll = Math.max(0, this.groupTabContentWidth - this.groupTabsW);
        ensureSelectedGroupVisible();
    }

    private int getGroupTabWidth(ConfigGroup group) {
        int textWidth = this.fontRenderer == null ? 48 : this.fontRenderer.getStringWidth(group.tabLabel);
        return Math.max(62, textWidth + 24);
    }

    private void ensureSelectedGroupVisible() {
        int startX = 0;
        ConfigGroup[] groups = ConfigGroup.values();
        for (ConfigGroup group : groups) {
            int tabWidth = getGroupTabWidth(group);
            if (group == this.selectedGroup) {
                int endX = startX + tabWidth;
                if (startX < this.groupTabScroll) {
                    this.groupTabScroll = startX;
                } else if (endX > this.groupTabScroll + this.groupTabsW) {
                    this.groupTabScroll = endX - this.groupTabsW;
                }
                this.groupTabScroll = clampInt(this.groupTabScroll, 0, this.groupTabMaxScroll);
                return;
            }
            startX += tabWidth + GROUP_TAB_GAP;
        }
        this.groupTabScroll = clampInt(this.groupTabScroll, 0, this.groupTabMaxScroll);
    }

    private boolean handleGroupWheel(int wheel, int mouseX, int mouseY) {
        if (!isMouseInside(mouseX, mouseY, this.groupBarX, this.groupBarY, this.groupBarW, this.groupBarH)) {
            return false;
        }
        selectGroupByOffset(wheel < 0 ? 1 : -1);
        return true;
    }

    private boolean handlePresetWheel(int wheel, int mouseX, int mouseY) {
        if (this.selectedGroup != ConfigGroup.PRESET) {
            return false;
        }
        if (!isMouseInside(mouseX, mouseY, this.presetListX, this.presetListY, this.presetListW, this.presetListH)
                || this.presetMaxScroll <= 0) {
            return false;
        }
        if (wheel < 0) {
            this.presetScrollOffset = clampInt(this.presetScrollOffset + 1, 0, this.presetMaxScroll);
        } else {
            this.presetScrollOffset = clampInt(this.presetScrollOffset - 1, 0, this.presetMaxScroll);
        }
        return true;
    }

    private boolean handleGroupClick(int mouseX, int mouseY) {
        if (!isMouseInside(mouseX, mouseY, this.groupTabsX, this.groupTabsY, this.groupTabsW, this.groupTabsH)) {
            return false;
        }

        int currentX = this.groupTabsX - this.groupTabScroll;
        for (ConfigGroup group : ConfigGroup.values()) {
            int tabWidth = getGroupTabWidth(group);
            if (isMouseInside(mouseX, mouseY, currentX, this.groupTabsY, tabWidth, this.groupTabsH)) {
                setSelectedGroup(group);
                return true;
            }
            currentX += tabWidth + GROUP_TAB_GAP;
        }
        return false;
    }

    private void selectGroupByOffset(int delta) {
        ConfigGroup[] groups = ConfigGroup.values();
        int currentIndex = this.selectedGroup.ordinal();
        int nextIndex = clampInt(currentIndex + delta, 0, groups.length - 1);
        setSelectedGroup(groups[nextIndex]);
    }

    private void setSelectedGroup(ConfigGroup group) {
        if (group == null || group == this.selectedGroup) {
            return;
        }
        this.selectedGroup = group;
        this.contentScroll = 0;
        this.nearbyNameDropdown.collapse();
        this.attackModeDropdown.collapse();
        this.huntModeDropdown.collapse();
        relayoutButtons();
    }

    private void layoutScrollableButton(GuiButton button, int x, int baseY, int width, int height) {
        int y = this.contentTop + baseY - this.contentScroll;
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = y >= this.contentTop && y + height <= this.contentBottom;
    }

    private void layoutFixedButton(GuiButton button, int x, int y, int width, int height) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = true;
    }

    private void hideButton(GuiButton button) {
        button.visible = false;
        button.x = -2000;
        button.y = -2000;
    }

    private boolean isInScrollableContent(int mouseX, int mouseY) {
        return mouseX >= this.contentFrameX + 4 && mouseX <= this.contentFrameX + this.contentFrameW - 4
                && mouseY >= this.contentTop && mouseY <= this.contentBottom;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (handlePresetWheel(wheel, mouseX, mouseY)) {
            return;
        }

        if (handleGroupWheel(wheel, mouseX, mouseY)) {
            return;
        }

        if (this.selectedGroup == ConfigGroup.NAME_FILTER
                && this.nearbyNameDropdown.handleWheel(wheel, mouseX, mouseY)) {
            return;
        }

        if (handleNameListWheel(wheel, mouseX, mouseY)) {
            return;
        }

        if (this.contentMaxScroll <= 0 || !isInScrollableContent(mouseX, mouseY)) {
            return;
        }

        if (wheel < 0) {
            this.contentScroll = clampInt(this.contentScroll + 18, 0, this.contentMaxScroll);
        } else {
            this.contentScroll = clampInt(this.contentScroll - 18, 0, this.contentMaxScroll);
        }
        relayoutButtons();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && this.selectedGroup == ConfigGroup.PRESET) {
            int presetIndex = getPresetCardIndexAt(mouseX, mouseY);
            if (presetIndex >= 0) {
                this.selectedPresetIndex = presetIndex;
                refreshButtonTexts();
                return;
            }
        }
        if (mouseButton == 0 && handleGroupClick(mouseX, mouseY)) {
            return;
        }

        if (this.selectedGroup == ConfigGroup.NAME_FILTER && mouseButton == 0
                && this.nearbyNameDropdown.handleClick(mouseX, mouseY)) {
            refreshButtonTexts();
            return;
        }

        if (this.selectedGroup == ConfigGroup.ATTACK && mouseButton == 0
                && this.attackModeDropdown.handleClick(mouseX, mouseY)) {
            refreshButtonTexts();
            relayoutButtons();
            return;
        }

        if (this.selectedGroup == ConfigGroup.HUNT && mouseButton == 0
                && this.huntModeDropdown.handleClick(mouseX, mouseY)) {
            refreshButtonTexts();
            relayoutButtons();
            return;
        }

        if (mouseButton == 0 && handleNameListClick(mouseX, mouseY)) {
            refreshButtonTexts();
            return;
        }

        if (this.selectedGroup == ConfigGroup.NAME_FILTER && this.nearbyNameDropdown.isExpanded()
                && !this.nearbyNameDropdown.isHoveringAnyPart(mouseX, mouseY)) {
            this.nearbyNameDropdown.collapse();
        }
        if (this.selectedGroup == ConfigGroup.ATTACK && this.attackModeDropdown.isExpanded()
                && !this.attackModeDropdown.isHoveringAnyPart(mouseX, mouseY)) {
            this.attackModeDropdown.collapse();
        }
        if (this.selectedGroup == ConfigGroup.HUNT && this.huntModeDropdown.isExpanded()
                && !this.huntModeDropdown.isHoveringAnyPart(mouseX, mouseY)) {
            this.huntModeDropdown.collapse();
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0 && this.whitelistDragActive) {
            updateWhitelistDrag(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            stopWhitelistDrag();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_LEFT) {
            selectGroupByOffset(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            selectGroupByOffset(1);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean handleNameListWheel(int wheel, int mouseX, int mouseY) {
        if (this.selectedGroup != ConfigGroup.NAME_FILTER) {
            return false;
        }
        if (isMouseInside(mouseX, mouseY, whitelistBoxX, whitelistBoxY, whitelistBoxW, whitelistBoxH)) {
            this.whitelistListScroll = adjustNameListScroll(this.whitelistListScroll, KillAuraHandler.nameWhitelist,
                    whitelistBoxH, wheel);
            return true;
        }
        if (isMouseInside(mouseX, mouseY, blacklistBoxX, blacklistBoxY, blacklistBoxW, blacklistBoxH)) {
            this.blacklistListScroll = adjustNameListScroll(this.blacklistListScroll, KillAuraHandler.nameBlacklist,
                    blacklistBoxH, wheel);
            return true;
        }
        return false;
    }

    private int adjustNameListScroll(int currentScroll, List<String> names, int boxHeight, int wheel) {
        int maxScroll = getNameListMaxScroll(names, boxHeight);
        if (wheel < 0) {
            return clampInt(currentScroll + 1, 0, maxScroll);
        }
        return clampInt(currentScroll - 1, 0, maxScroll);
    }

    private int getNameListVisibleRows(int boxHeight) {
        int usableHeight = boxHeight - NAME_LIST_HEADER_HEIGHT - NAME_LIST_INNER_PADDING * 2;
        return Math.max(1, usableHeight / (NAME_LIST_CARD_HEIGHT + NAME_LIST_CARD_GAP));
    }

    private int getNameListMaxScroll(List<String> names, int boxHeight) {
        int visibleRows = getNameListVisibleRows(boxHeight);
        int size = names == null ? 0 : names.size();
        return Math.max(0, size - visibleRows);
    }

    private boolean handleNameListClick(int mouseX, int mouseY) {
        if (this.selectedGroup != ConfigGroup.NAME_FILTER) {
            return false;
        }
        if (handleSingleNameListClick(mouseX, mouseY, KillAuraHandler.nameWhitelist, true)) {
            return true;
        }
        return handleSingleNameListClick(mouseX, mouseY, KillAuraHandler.nameBlacklist, false);
    }

    private boolean handleSingleNameListClick(int mouseX, int mouseY, List<String> names, boolean whitelist) {
        int boxX = whitelist ? whitelistBoxX : blacklistBoxX;
        int boxY = whitelist ? whitelistBoxY : blacklistBoxY;
        int boxW = whitelist ? whitelistBoxW : blacklistBoxW;
        int boxH = whitelist ? whitelistBoxH : blacklistBoxH;
        int scroll = whitelist ? whitelistListScroll : blacklistListScroll;

        if (!isMouseInside(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
            return false;
        }

        if (whitelist) {
            if (isMouseInside(mouseX, mouseY, getWhitelistMoveUpButtonX(), getWhitelistHeaderButtonY(),
                    getNameListHeaderButtonSize(), getNameListHeaderButtonSize())) {
                moveSelectedWhitelistBy(-1);
                return true;
            }
            if (isMouseInside(mouseX, mouseY, getWhitelistMoveDownButtonX(), getWhitelistHeaderButtonY(),
                    getNameListHeaderButtonSize(), getNameListHeaderButtonSize())) {
                moveSelectedWhitelistBy(1);
                return true;
            }
        }

        int contentX = boxX + NAME_LIST_INNER_PADDING;
        int contentY = boxY + NAME_LIST_HEADER_HEIGHT + NAME_LIST_INNER_PADDING;
        int cardW = boxW - NAME_LIST_INNER_PADDING * 2;
        int visibleRows = getNameListVisibleRows(boxH);

        for (int i = 0; i < visibleRows; i++) {
            int actualIndex = scroll + i;
            if (names == null || actualIndex >= names.size()) {
                break;
            }

            int cardY = contentY + i * (NAME_LIST_CARD_HEIGHT + NAME_LIST_CARD_GAP);
            int removeX = contentX + cardW - 18;
            if (isMouseInside(mouseX, mouseY, removeX, cardY + 1, 16, NAME_LIST_CARD_HEIGHT - 2)) {
                names.remove(actualIndex);
                if (whitelist) {
                    if (this.selectedWhitelistIndex == actualIndex) {
                        this.selectedWhitelistIndex = actualIndex >= names.size() ? names.size() - 1 : actualIndex;
                    } else if (actualIndex < this.selectedWhitelistIndex) {
                        this.selectedWhitelistIndex--;
                    }
                    stopWhitelistDrag();
                    this.whitelistListScroll = clampInt(this.whitelistListScroll, 0,
                            getNameListMaxScroll(KillAuraHandler.nameWhitelist, whitelistBoxH));
                } else {
                    this.blacklistListScroll = clampInt(this.blacklistListScroll, 0,
                            getNameListMaxScroll(KillAuraHandler.nameBlacklist, blacklistBoxH));
                }
                relayoutButtons();
                return true;
            }

            if (whitelist && isMouseInside(mouseX, mouseY, contentX, cardY, cardW, NAME_LIST_CARD_HEIGHT)) {
                this.selectedWhitelistIndex = actualIndex;
                startWhitelistDrag(actualIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_GROUP_PREV:
            selectGroupByOffset(-1);
            return;
        case BTN_GROUP_NEXT:
            selectGroupByOffset(1);
            return;
        case BTN_PRESET_SAVE_NEW:
            openPresetNameInput("输入新的预设名称", "", name -> {
                if (KillAuraHandler.saveCurrentAsPreset(name)) {
                    refreshPresetCards();
                    selectPresetByName(name);
                    refreshButtonTexts();
                    relayoutButtons();
                }
                mc.displayGuiScreen(this);
            });
            return;
        case BTN_PRESET_APPLY:
            if (KillAuraHandler.applyPresetByName(getSelectedPresetName())) {
                clearWhitelistSelectionState();
                refreshNearbyEntityOptions(false);
                refreshPresetCards();
                refreshButtonTexts();
                relayoutButtons();
            }
            return;
        case BTN_PRESET_OVERWRITE:
            if (KillAuraHandler.overwritePreset(getSelectedPresetName())) {
                refreshPresetCards();
                refreshButtonTexts();
                relayoutButtons();
            }
            return;
        case BTN_PRESET_RENAME:
            openPresetNameInput("重命名预设", getSelectedPresetName(), name -> {
                String oldName = getSelectedPresetName();
                if (KillAuraHandler.renamePreset(oldName, name)) {
                    refreshPresetCards();
                    selectPresetByName(name);
                    refreshButtonTexts();
                    relayoutButtons();
                }
                mc.displayGuiScreen(this);
            });
            return;
        case BTN_PRESET_DELETE:
            if (KillAuraHandler.deletePreset(getSelectedPresetName())) {
                refreshPresetCards();
                refreshButtonTexts();
                relayoutButtons();
            }
            return;
        case BTN_ROTATE:
            KillAuraHandler.rotateToTarget = !KillAuraHandler.rotateToTarget;
            break;
        case BTN_SMOOTH_ROTATE:
            KillAuraHandler.smoothRotation = !KillAuraHandler.smoothRotation;
            break;
        case BTN_AIM_YAW_OFFSET:
            openPreciseFloatInput("输入索敌视角偏移 (-30.0000 - 30.0000，正数向右，负数向左)",
                    KillAuraHandler.aimYawOffset, -30.0F, 30.0F, value -> {
                        KillAuraHandler.aimYawOffset = value;
                        refreshButtonTexts();
                    });
            return;
        case BTN_LINE_OF_SIGHT:
            KillAuraHandler.requireLineOfSight = !KillAuraHandler.requireLineOfSight;
            break;
        case BTN_ONLY_WEAPON:
            KillAuraHandler.onlyWeapon = !KillAuraHandler.onlyWeapon;
            break;
        case BTN_AIM_ONLY:
            KillAuraHandler.aimOnlyMode = !KillAuraHandler.aimOnlyMode;
            if (KillAuraHandler.aimOnlyMode) {
                KillAuraHandler.attackMode = KillAuraHandler.ATTACK_MODE_SEQUENCE;
            }
            break;
        case BTN_HOSTILE:
            KillAuraHandler.targetHostile = !KillAuraHandler.targetHostile;
            enforceAtLeastOneTargetType();
            break;
        case BTN_PASSIVE:
            KillAuraHandler.targetPassive = !KillAuraHandler.targetPassive;
            enforceAtLeastOneTargetType();
            break;
        case BTN_PLAYERS:
            KillAuraHandler.targetPlayers = !KillAuraHandler.targetPlayers;
            enforceAtLeastOneTargetType();
            break;
        case BTN_FOCUS:
            KillAuraHandler.focusSingleTarget = !KillAuraHandler.focusSingleTarget;
            break;
        case BTN_IGNORE_INVISIBLE:
            KillAuraHandler.ignoreInvisible = !KillAuraHandler.ignoreInvisible;
            break;
        case BTN_NO_COLLISION:
            KillAuraHandler.enableNoCollision = !KillAuraHandler.enableNoCollision;
            break;
        case BTN_ANTI_KNOCKBACK:
            KillAuraHandler.enableAntiKnockback = !KillAuraHandler.enableAntiKnockback;
            break;
        case BTN_FULL_BRIGHT:
            KillAuraHandler.enableFullBrightVision = !KillAuraHandler.enableFullBrightVision;
            break;
        case BTN_FULL_BRIGHT_GAMMA:
            openFloatInput("输入夜视提亮强度 (1.0 - 1000.0)", KillAuraHandler.fullBrightGamma, 1.0F, 1000.0F, value -> {
                KillAuraHandler.fullBrightGamma = value;
                refreshButtonTexts();
            });
            return;
        case BTN_ATTACK_MODE:
            if (KillAuraHandler.ATTACK_MODE_NORMAL.equalsIgnoreCase(KillAuraHandler.attackMode)) {
                applyAttackMode(KillAuraHandler.ATTACK_MODE_PACKET);
            } else if (KillAuraHandler.ATTACK_MODE_PACKET.equalsIgnoreCase(KillAuraHandler.attackMode)) {
                applyAttackMode(KillAuraHandler.ATTACK_MODE_TELEPORT);
            } else if (KillAuraHandler.ATTACK_MODE_TELEPORT.equalsIgnoreCase(KillAuraHandler.attackMode)) {
                applyAttackMode(KillAuraHandler.ATTACK_MODE_SEQUENCE);
            } else {
                applyAttackMode(KillAuraHandler.ATTACK_MODE_NORMAL);
            }
            break;
        case BTN_ATTACK_SEQUENCE:
            PathSequenceManager.initializePathSequences();
            mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                KillAuraHandler.attackSequenceName = safe(seq).trim();
                refreshButtonTexts();
                relayoutButtons();
                mc.displayGuiScreen(this);
            }));
            return;
        case BTN_ATTACK_SEQUENCE_DELAY:
            mc.displayGuiScreen(new GuiTextInput(this, "输入执行序列延迟 Tick (0 - 200)",
                    String.valueOf(KillAuraHandler.attackSequenceDelayTicks), value -> {
                        int parsed = KillAuraHandler.attackSequenceDelayTicks;
                        try {
                            parsed = Integer.parseInt(value.trim());
                        } catch (Exception ignored) {
                        }
                        KillAuraHandler.attackSequenceDelayTicks = clampInt(parsed, 0, 200);
                        refreshButtonTexts();
                        mc.displayGuiScreen(this);
                    }));
            return;
        case BTN_HUNT_PICKUP:
            KillAuraHandler.huntPickupItemsEnabled = !KillAuraHandler.huntPickupItemsEnabled;
            break;
        case BTN_HUNT_VISUALIZE:
            KillAuraHandler.visualizeHuntRadius = !KillAuraHandler.visualizeHuntRadius;
            break;
        case BTN_RANGE:
            openFloatInput("输入攻击范围 (1.0 - 100.0)", KillAuraHandler.attackRange, 1.0F, 100.0F, value -> {
                KillAuraHandler.attackRange = value;
                if (KillAuraHandler.huntRadius < KillAuraHandler.attackRange) {
                    KillAuraHandler.huntRadius = KillAuraHandler.attackRange;
                }
                refreshButtonTexts();
            });
            return;
        case BTN_MIN_STRENGTH:
            openFloatInput("输入最小攻击蓄力 (0.0 - 1.0)", KillAuraHandler.minAttackStrength, 0.0F, 1.0F, value -> {
                KillAuraHandler.minAttackStrength = value;
                refreshButtonTexts();
            });
            return;
        case BTN_MIN_TURN_SPEED:
            openFloatInput("输入最小转速 (1.0 - 40.0)", KillAuraHandler.minTurnSpeed, 1.0F, 40.0F, value -> {
                KillAuraHandler.minTurnSpeed = value;
                if (KillAuraHandler.maxTurnSpeed < KillAuraHandler.minTurnSpeed) {
                    KillAuraHandler.maxTurnSpeed = KillAuraHandler.minTurnSpeed;
                }
                refreshButtonTexts();
            });
            return;
        case BTN_MAX_TURN_SPEED:
            openFloatInput("输入最大转速 (>= 最小转速, <= 60.0)", KillAuraHandler.maxTurnSpeed, KillAuraHandler.minTurnSpeed,
                    60.0F, value -> {
                        KillAuraHandler.maxTurnSpeed = value;
                        refreshButtonTexts();
                    });
            return;
        case BTN_INTERVAL:
            mc.displayGuiScreen(new GuiTextInput(this, "输入最小攻击间隔 Tick (0 - 20)",
                    String.valueOf(KillAuraHandler.minAttackIntervalTicks), value -> {
                        int parsed = KillAuraHandler.minAttackIntervalTicks;
                        try {
                            parsed = Integer.parseInt(value.trim());
                        } catch (Exception ignored) {
                        }
                        KillAuraHandler.minAttackIntervalTicks = clampInt(parsed, 0, 20);
                        refreshButtonTexts();
                        mc.displayGuiScreen(this);
                    }));
            return;
        case BTN_TARGETS_PER_ATTACK:
            mc.displayGuiScreen(new GuiTextInput(this, "输入单次攻击目标数 (1 - 50)",
                    String.valueOf(KillAuraHandler.targetsPerAttack), value -> {
                        int parsed = KillAuraHandler.targetsPerAttack;
                        try {
                            parsed = Integer.parseInt(value.trim());
                        } catch (Exception ignored) {
                        }
                        KillAuraHandler.targetsPerAttack = clampInt(parsed, 1, 50);
                        refreshButtonTexts();
                        mc.displayGuiScreen(this);
                    }));
            return;
        case BTN_HUNT_RADIUS:
            openFloatInput("输入追击半径 (>= 攻击范围, <= 100.0)", KillAuraHandler.huntRadius, KillAuraHandler.attackRange, 100.0F,
                    value -> {
                        KillAuraHandler.huntRadius = value;
                        refreshButtonTexts();
                    });
            return;
        case BTN_HUNT_FIXED_DISTANCE:
            openFloatInput("输入固定距离 (0.5 - 100.0)", KillAuraHandler.huntFixedDistance, 0.5F, 100.0F,
                    value -> {
                        KillAuraHandler.huntFixedDistance = value;
                        refreshButtonTexts();
                    });
            return;
        case BTN_HUNT_ORBIT:
            KillAuraHandler.huntOrbitEnabled = !KillAuraHandler.huntOrbitEnabled;
            break;
        case BTN_HUNT_JUMP_ORBIT:
            KillAuraHandler.huntJumpOrbitEnabled = !KillAuraHandler.huntJumpOrbitEnabled;
            break;
        case BTN_HUNT_ORBIT_SAMPLE_POINTS:
            mc.displayGuiScreen(new GuiTextInput(this,
                    "输入绕圈轨道采样点个数 (" + KillAuraHandler.MIN_HUNT_ORBIT_SAMPLE_POINTS
                            + " - " + KillAuraHandler.MAX_HUNT_ORBIT_SAMPLE_POINTS + ")",
                    String.valueOf(KillAuraHandler.getConfiguredHuntOrbitSamplePoints()), value -> {
                        int parsed = KillAuraHandler.getConfiguredHuntOrbitSamplePoints();
                        try {
                            parsed = Integer.parseInt(value.trim());
                        } catch (Exception ignored) {
                        }
                        KillAuraHandler.huntOrbitSamplePoints = clampInt(parsed,
                                KillAuraHandler.MIN_HUNT_ORBIT_SAMPLE_POINTS,
                                KillAuraHandler.MAX_HUNT_ORBIT_SAMPLE_POINTS);
                        refreshButtonTexts();
                        relayoutButtons();
                        mc.displayGuiScreen(this);
                    }));
            return;
        case BTN_NAME_WHITELIST:
            KillAuraHandler.enableNameWhitelist = !KillAuraHandler.enableNameWhitelist;
            break;
        case BTN_NAME_BLACKLIST:
            KillAuraHandler.enableNameBlacklist = !KillAuraHandler.enableNameBlacklist;
            break;
        case BTN_SCAN_RANGE:
            openFloatInput("输入附近实体获取范围 (1.0 - 64.0)", KillAuraHandler.nearbyEntityScanRange, 1.0F, 64.0F, value -> {
                KillAuraHandler.nearbyEntityScanRange = value;
                refreshButtonTexts();
            });
            return;
        case BTN_SCAN_NEARBY:
            refreshNearbyEntityOptions(true);
            break;
        case BTN_ADD_SELECTED_WHITELIST:
            addSelectedDropdownName(true);
            break;
        case BTN_ADD_SELECTED_BLACKLIST:
            addSelectedDropdownName(false);
            break;
        case BTN_ADD_MANUAL_WHITELIST:
            openNameInput("输入要加入白名单的实体名称关键字", true);
            return;
        case BTN_ADD_MANUAL_BLACKLIST:
            openNameInput("输入要加入黑名单的实体名称关键字", false);
            return;
        case BTN_SAVE:
            KillAuraHandler.saveConfig();
            mc.displayGuiScreen(parentScreen);
            return;
        case BTN_DEFAULT:
            applyDefaultValues();
            refreshNearbyEntityOptions(false);
            refreshButtonTexts();
            relayoutButtons();
            return;
        case BTN_CANCEL:
            KillAuraHandler.loadConfig();
            clearWhitelistSelectionState();
            refreshNearbyEntityOptions(false);
            mc.displayGuiScreen(parentScreen);
            return;
        default:
            break;
        }

        refreshButtonTexts();
        relayoutButtons();
    }

    private void refreshNearbyEntityOptions(boolean rescan) {
        List<String> names = rescan ? KillAuraHandler.getNearbyEntityNames(KillAuraHandler.nearbyEntityScanRange)
                : KillAuraHandler.getNearbyEntityNames(KillAuraHandler.nearbyEntityScanRange);
        this.nearbyNameDropdown.setOptions(names);
        refreshButtonTexts();
    }

    private void addSelectedDropdownName(boolean toWhitelist) {
        String selected = KillAuraHandler.normalizeFilterName(this.nearbyNameDropdown.getSelectedValue());
        if (isBlank(selected)) {
            return;
        }
        int addedIndex = addNameToList(toWhitelist ? KillAuraHandler.nameWhitelist : KillAuraHandler.nameBlacklist,
                selected);
        if (toWhitelist) {
            this.selectedWhitelistIndex = addedIndex;
            this.whitelistListScroll = clampInt(this.whitelistListScroll, 0,
                    getNameListMaxScroll(KillAuraHandler.nameWhitelist, whitelistBoxH));
            ensureWhitelistSelectionVisible();
        } else {
            this.blacklistListScroll = clampInt(this.blacklistListScroll, 0,
                    getNameListMaxScroll(KillAuraHandler.nameBlacklist, blacklistBoxH));
        }
        refreshButtonTexts();
        relayoutButtons();
    }

    private void openNameInput(String title, boolean toWhitelist) {
        mc.displayGuiScreen(new GuiTextInput(this, title, "", value -> {
            String trimmed = safe(value).trim();
            if (!trimmed.isEmpty()) {
                int addedIndex = addNameToList(
                        toWhitelist ? KillAuraHandler.nameWhitelist : KillAuraHandler.nameBlacklist,
                        trimmed);
                if (toWhitelist) {
                    this.selectedWhitelistIndex = addedIndex;
                    ensureWhitelistSelectionVisible();
                }
            }
            refreshButtonTexts();
            relayoutButtons();
            mc.displayGuiScreen(this);
        }));
    }

    private void openPresetNameInput(String title, String initialValue, java.util.function.Consumer<String> onDone) {
        mc.displayGuiScreen(new GuiTextInput(this, title, safe(initialValue), value -> {
            if (onDone != null) {
                onDone.accept(safe(value).trim());
            }
        }));
    }

    private void selectPresetByName(String presetName) {
        String normalizedPresetName = safe(presetName).trim();
        if (normalizedPresetName.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.presetCards.size(); i++) {
            KillAuraHandler.KillAuraPreset preset = this.presetCards.get(i);
            if (preset != null && normalizedPresetName.equalsIgnoreCase(safe(preset.name).trim())) {
                this.selectedPresetIndex = i;
                ensureSelectedPresetVisible();
                return;
            }
        }
    }

    private int addNameToList(List<String> targetList, String value) {
        String normalized = KillAuraHandler.normalizeFilterName(value);
        if (normalized.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < targetList.size(); i++) {
            String existing = targetList.get(i);
            if (existing != null && existing.equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        targetList.add(normalized);
        return targetList.size() - 1;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "杀戮光环设置", this.fontRenderer);

        int textY = panelY + 22;
        for (String line : instructionLines) {
            this.drawString(this.fontRenderer, line, panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += 10;
        }

        drawGroupTabs(mouseX, mouseY);
        drawCustomSectionFrame(this.selectedGroup.contentTitle, this.contentFrameX, this.contentFrameY,
                this.contentFrameW, this.contentFrameH);

        if (contentMaxScroll > 0) {
            drawRect(this.contentFrameX + 4, contentTop - 2, this.contentFrameX + this.contentFrameW - 4,
                    contentBottom + 2, 0x221A2533);

            int trackX = this.contentFrameX + this.contentFrameW - 8;
            int trackY = contentTop;
            int trackHeight = contentBottom - contentTop;
            int thumbHeight = Math.max(18,
                    (int) ((trackHeight / (float) Math.max(trackHeight, trackHeight + contentMaxScroll))
                            * trackHeight));
            int thumbTrack = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) ((contentScroll / (float) Math.max(1, contentMaxScroll)) * thumbTrack);
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackHeight, thumbY, thumbHeight);
        }

        if (this.selectedGroup == ConfigGroup.ATTACK) {
            attackModeDropdown.drawMain(mouseX, mouseY, this.fontRenderer);
        }
        if (this.selectedGroup == ConfigGroup.HUNT) {
            huntModeDropdown.drawMain(mouseX, mouseY, this.fontRenderer);
        }
        if (this.selectedGroup == ConfigGroup.NAME_FILTER) {
            nearbyNameDropdown.drawMain(mouseX, mouseY, this.fontRenderer);
            drawNameListBox("白名单", KillAuraHandler.nameWhitelist, whitelistBoxX, whitelistBoxY, whitelistBoxW,
                    whitelistBoxH, whitelistListScroll, mouseX, mouseY, 0xFF4FA6D9, true);
            drawNameListBox("黑名单", KillAuraHandler.nameBlacklist, blacklistBoxX, blacklistBoxY, blacklistBoxW,
                    blacklistBoxH, blacklistListScroll, mouseX, mouseY, 0xFFE57C7C, false);
        } else if (this.selectedGroup == ConfigGroup.PRESET) {
            drawPresetPanel(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.selectedGroup == ConfigGroup.ATTACK) {
            attackModeDropdown.drawExpanded(mouseX, mouseY, this.fontRenderer);
        }
        if (this.selectedGroup == ConfigGroup.HUNT) {
            huntModeDropdown.drawExpanded(mouseX, mouseY, this.fontRenderer);
        }
        if (this.selectedGroup == ConfigGroup.NAME_FILTER) {
            nearbyNameDropdown.drawExpanded(mouseX, mouseY, this.fontRenderer);
        }

        drawHoverTooltips(mouseX, mouseY);
    }

    private void drawHoverTooltips(int mouseX, int mouseY) {
        if (this.selectedGroup == ConfigGroup.PRESET
                && isMouseInside(mouseX, mouseY, this.contentFrameX, this.contentFrameY, this.contentFrameW,
                        this.contentFrameH)) {
            if (isMouseOver(mouseX, mouseY, presetSaveNewButton)) {
                drawHoveringText(Arrays.asList("§e新增预设", "§7把当前杀戮光环配置保存为新的自定义预设。"), mouseX, mouseY);
            } else if (isMouseOver(mouseX, mouseY, presetApplyButton)) {
                drawHoveringText(Arrays.asList("§e使用当前预设", "§7把当前选中的预设完整应用到杀戮光环配置。"), mouseX, mouseY);
            } else if (isMouseOver(mouseX, mouseY, presetOverwriteButton)) {
                drawHoveringText(Arrays.asList("§e保存当前预设", "§7用当前界面的配置覆盖更新选中的预设。"), mouseX, mouseY);
            } else if (isMouseOver(mouseX, mouseY, presetRenameButton)) {
                drawHoveringText(Arrays.asList("§e重命名预设", "§7给当前选中的预设改一个新名字。"), mouseX, mouseY);
            } else if (isMouseOver(mouseX, mouseY, presetDeleteButton)) {
                drawHoveringText(Arrays.asList("§e删除预设", "§7删除当前选中的自定义预设。"), mouseX, mouseY);
            } else if (isMouseInside(mouseX, mouseY, this.presetListX, this.presetListY, this.presetListW, this.presetListH)) {
                drawHoveringText(Arrays.asList("§e预设卡片列表", "§7点击卡片可选中预设。", "§7在这里滚轮滚动时，每次刚好移动一张卡片。"), mouseX, mouseY);
            } else {
                drawHoveringText(Arrays.asList("§e预设分组", "§7这里集中管理杀戮光环预设。", "§7可保存、应用、覆盖、重命名、删除杀戮光环预设。"), mouseX,
                        mouseY);
            }
        } else if (isMouseInside(mouseX, mouseY, this.groupBarX, this.groupBarY, this.groupBarW, this.groupBarH)) {
            drawHoveringText(Arrays.asList("§e顶部功能分组", "§7点击分组可切换当前功能页。", "§7把鼠标放在这里滚轮滚动，或按键盘左右方向键，也能快速切组。"), mouseX,
                    mouseY);
        } else if (this.selectedGroup == ConfigGroup.ATTACK && attackModeDropdown.isHoveringAnyPart(mouseX, mouseY)) {
            drawHoveringText(
                    Arrays.asList("§e攻击模式", "§7普通攻击：使用原版控制器攻击，可配合转向。",
                            "§7数据包攻击：直接发送攻击包，不移动视角。",
                            "§7TP攻击：当攻击范围大于 6 格时，分段发送位置包贴近目标攻击后返回原位。",
                            "§7执行序列：命中触发条件后，独立执行你选定的攻击序列。",
                            "§7开启只瞄准后，会默认切到执行序列模式。", "§7切到数据包模式后会自动关闭并隐藏转向相关选项。"),
                    mouseX, mouseY);
        } else if (attackSequenceButton.visible && isMouseOver(mouseX, mouseY, attackSequenceButton)) {
            drawHoveringText(Arrays.asList("§e攻击序列", "§7选择一个已有路径序列，作为杀戮光环的自定义攻击动作。", "§7该序列由杀戮光环独立执行，不会打断当前正在跑的其他主序列。",
                    "§7为避免互相抢控制，带导航、狩猎、再调序列等动作会被自动忽略。"), mouseX, mouseY);
        } else if (attackSequenceDelayButton.visible && isMouseOver(mouseX, mouseY, attackSequenceDelayButton)) {
            drawHoveringText(Arrays.asList("§e执行延迟", "§7控制杀戮光环两次触发攻击序列之间的独立冷却时间。", "§7这个延迟只影响杀戮光环自己的序列触发，",
                    "§7不会改动其他路径序列本身的循环或等待设置。"), mouseX, mouseY);
        } else if (aimOnlyButton.visible && isMouseOver(mouseX, mouseY, aimOnlyButton)) {
            drawHoveringText(
                    Arrays.asList("§e只瞄准不攻击", "§7开启后只会锁定杀戮范围内的目标并转向，", "§7不会进行普通攻击或数据包攻击。",
                            "§7攻击模式会默认切到执行序列，可继续触发自定义攻击序列。", "§7如果同时开启追击(Hunt)，仍会正常追击目标。", "§7黑白名单、目标类型和可见性过滤依然生效。"),
                    mouseX, mouseY);
        } else if (this.selectedGroup == ConfigGroup.HUNT && huntModeDropdown.isHoveringAnyPart(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e追击模式", "§7靠近目标：旧 Hunt 行为，超出攻击距离时自动靠近目标。",
                    "§7固定距离：持续尝试和目标保持你单独设置的固定距离。", "§7关闭：不主动追击，也会禁用相关追击联动。"), mouseX, mouseY);
        } else if (huntFixedDistanceButton.visible && isMouseOver(mouseX, mouseY, huntFixedDistanceButton)) {
            drawHoveringText(Arrays.asList("§e固定距离", "§7仅在追击模式=固定距离时生效。", "§7它和攻击范围完全独立，不会再跟随攻击范围变化。"),
                    mouseX, mouseY);
        } else if (huntOrbitButton.visible && isMouseOver(mouseX, mouseY, huntOrbitButton)) {
            drawHoveringText(Arrays.asList("§e自动绕圈攻击", "§7仅在追击模式=固定距离时生效。", "§7开启后会尝试以目标为圆心，围绕固定距离持续绕圈。",
                    "§7遇到障碍会优先微调半径并改选可站立的平地方块。", "§7候选位置不会高于你当前脚底一格。"), mouseX, mouseY);
        } else if (huntJumpOrbitButton.visible && isMouseOver(mouseX, mouseY, huntJumpOrbitButton)) {
            drawHoveringText(Arrays.asList("§e跳跃绕圈", "§7默认开启。", "§7开启后，进入绕圈轨道后会切换到内置的本地跳跃绕圈。",
                    "§7关闭后，仍然使用同一套本地绕圈接管逻辑，只是不再持续按跳。",
                    "§7现在所有轨道采样点配置都会允许本地绕圈接管，不再只限定最大值。"), mouseX, mouseY);
        } else if (huntOrbitSamplePointsButton.visible && isMouseOver(mouseX, mouseY, huntOrbitSamplePointsButton)) {
            drawHoveringText(Arrays.asList("§e轨道采样点", "§7仅在自动绕圈攻击开启时生效。", "§7决定绕圈轨道一圈采多少个等间隔点。",
                    "§7例如设为 3 时，会按三角形轨道渲染并沿三角形节点绕圈。", "§7默认最大值会尽量保持接近当前的圆形平滑效果。",
                    "§7最小值为 3。"), mouseX, mouseY);
        } else if (huntRadiusButton.visible && isMouseOver(mouseX, mouseY, huntRadiusButton)) {
            drawHoveringText(Arrays.asList("§e追击半径", "§7用于决定在多大范围内主动搜怪并追击。", "§7该值不能小于攻击范围。"), mouseX, mouseY);
        } else if (huntPickupButton.visible && isMouseOver(mouseX, mouseY, huntPickupButton)) {
            drawHoveringText(Arrays.asList("§e优先拾取掉落物", "§7默认关闭。", "§7开启后，Hunt 在追击半径内发现掉落物时会先去捡，再继续追怪。",
                    "§7如果你当前就在自动拾取规则范围内，会优先让自动拾取规则管理器接管，不会互相抢导航。"), mouseX, mouseY);
        } else if (huntVisualizeButton.visible && isMouseOver(mouseX, mouseY, huntVisualizeButton)) {
            drawHoveringText(Arrays.asList("§e显示追击半径光环", "§7开启后会在玩家脚底绘制一个追击半径的可视化光环。", "§7方便直观看到 Hunt 搜怪范围。"), mouseX,
                    mouseY);
        } else if (rangeButton.visible && isMouseOver(mouseX, mouseY, rangeButton)) {
            drawHoveringText(Arrays.asList("§e攻击范围", "§7现已支持 1 ~ 100 格。", "§7超过 6 格时，建议切换到 TP攻击 模式。",
                    "§7普通攻击 / 数据包攻击在超远距离下通常无法稳定命中。"), mouseX, mouseY);
        } else if (minStrengthButton.visible && isMouseOver(mouseX, mouseY, minStrengthButton)) {
            drawHoveringText(Arrays.asList("§e最小攻击蓄力", "§70.0 表示见到就打。", "§71.0 表示完全冷却后再打。"), mouseX, mouseY);
        } else if (targetsPerAttackButton.visible && isMouseOver(mouseX, mouseY, targetsPerAttackButton)) {
            drawHoveringText(Arrays.asList("§e单次攻击目标数", "§71 表示每次只攻击一个目标。", "§7大于 1 时，会在一次出手里依次攻击多个符合条件的目标。",
                    "§7同样会受黑白名单、攻击范围和可见性限制。", "§7执行序列模式不会使用这个选项。"), mouseX, mouseY);
        } else if (onlyWeaponButton.visible && isMouseOver(mouseX, mouseY, onlyWeaponButton)) {
            drawHoveringText(Arrays.asList("§e仅持武器生效", "§7开启后必须主手拿剑或斧头才会自动攻击。", "§7可避免误拿工具、食物或其他物品时乱打。"), mouseX,
                    mouseY);
        } else if (noCollisionButton.visible && isMouseOver(mouseX, mouseY, noCollisionButton)) {
            drawHoveringText(Arrays.asList("§e无碰撞", "§7开启后会提高实体碰撞减免，", "§7减少被怪物或实体挤开、顶开。"), mouseX, mouseY);
        } else if (antiKnockbackButton.visible && isMouseOver(mouseX, mouseY, antiKnockbackButton)) {
            drawHoveringText(Arrays.asList("§e防击退", "§7开启后在受击时尽量抵消水平击退，", "§7并尽量保留自己的移动手感。"), mouseX, mouseY);
        } else if (fullBrightButton.visible && isMouseOver(mouseX, mouseY, fullBrightButton)) {
            drawHoveringText(Arrays.asList("§e夜视提亮", "§7开启后会把游戏亮度临时拉高，", "§7让整个视角明显变亮，方便夜间或暗处挂机。",
                    "§7这是独立开关，不受杀戮光环总开关影响。", "§7关闭此项后会自动恢复原亮度。"), mouseX, mouseY);
        } else if (fullBrightGammaButton.visible && isMouseOver(mouseX, mouseY, fullBrightGammaButton)) {
            drawHoveringText(
                    Arrays.asList("§e提亮强度", "§7用于自由调整夜视提亮的亮度值。", "§7数值越高，整个视角越亮。", "§71.0 接近原版较亮环境，1000.0 为强提亮。"),
                    mouseX, mouseY);
        } else if (focusButton.visible && isMouseOver(mouseX, mouseY, focusButton)) {
            drawHoveringText(Arrays.asList("§e锁定单一目标", "§7开启后优先持续攻击当前目标。", "§7关闭后会按评分重新挑选更优目标。"), mouseX, mouseY);
        } else if (nameWhitelistButton.visible && isMouseOver(mouseX, mouseY, nameWhitelistButton)) {
            drawHoveringText(Arrays.asList("§e名称白名单", "§7启用后，只会攻击名称包含白名单任意关键字的实体。",
                    "§7不管该实体是玩家、怪物还是中立生物，只要名称命中白名单就会攻击。", "§7白名单顺序就是瞄准优先级，越靠前越优先。",
                    "§7可选中后上移/下移，或直接拖动重排。", "§7匹配规则：包含即可。"), mouseX, mouseY);
        } else if (nameBlacklistButton.visible && isMouseOver(mouseX, mouseY, nameBlacklistButton)) {
            drawHoveringText(Arrays.asList("§e名称黑名单", "§7启用后，名称包含黑名单任意关键字的实体会被忽略，不进行攻击。", "§7匹配规则：包含即可。"), mouseX,
                    mouseY);
        } else if (scanRangeButton.visible && isMouseOver(mouseX, mouseY, scanRangeButton)) {
            drawHoveringText(Arrays.asList("§e获取范围", "§7设置一键获取周围实体名称时的扫描范围。", "§7默认 10 格，可自行修改。"), mouseX, mouseY);
        } else if (scanNearbyButton.visible && isMouseOver(mouseX, mouseY, scanNearbyButton)) {
            drawHoveringText(Arrays.asList("§e获取周围实体名称", "§7按当前获取范围扫描附近实体名称。", "§7扫描结果会出现在下方下拉框中，方便快速加入黑白名单。"), mouseX,
                    mouseY);
        } else if (this.selectedGroup == ConfigGroup.NAME_FILTER && nearbyNameDropdown.isHoveringMain(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e附近实体名称下拉框", "§7这里显示最近一次扫描得到的附近实体名称。", "§7点击展开后可滚动选择，再快速加入黑名单或白名单。"),
                    mouseX, mouseY);
        } else if (addSelectedWhitelistButton.visible && isMouseOver(mouseX, mouseY, addSelectedWhitelistButton)) {
            drawHoveringText(Arrays.asList("§e选中加入白名单", "§7把当前下拉框选中的实体名称加入白名单。"), mouseX, mouseY);
        } else if (addSelectedBlacklistButton.visible && isMouseOver(mouseX, mouseY, addSelectedBlacklistButton)) {
            drawHoveringText(Arrays.asList("§e选中加入黑名单", "§7把当前下拉框选中的实体名称加入黑名单。"), mouseX, mouseY);
        } else if (addManualWhitelistButton.visible && isMouseOver(mouseX, mouseY, addManualWhitelistButton)) {
            drawHoveringText(Arrays.asList("§e手动添加白名单项", "§7手动输入一个实体名称关键字并加入白名单。", "§7匹配规则：包含即可。"), mouseX, mouseY);
        } else if (addManualBlacklistButton.visible && isMouseOver(mouseX, mouseY, addManualBlacklistButton)) {
            drawHoveringText(Arrays.asList("§e手动添加黑名单项", "§7手动输入一个实体名称关键字并加入黑名单。", "§7匹配规则：包含即可。"), mouseX, mouseY);
        } else if (this.selectedGroup == ConfigGroup.NAME_FILTER
                && isMouseInside(mouseX, mouseY, whitelistBoxX, whitelistBoxY, whitelistBoxW, whitelistBoxH)) {
            drawHoveringText(Arrays.asList("§e白名单卡片列表", "§7这里展示所有白名单关键字，序号越小优先级越高。", "§7点击卡片可选中该项。",
                    "§7标题栏的 ^ / v 可上移或下移选中项。", "§7按住卡片主体上下拖动也可以直接重排。", "§7右侧 §cX §7可删除该项。"), mouseX, mouseY);
        } else if (this.selectedGroup == ConfigGroup.NAME_FILTER
                && isMouseInside(mouseX, mouseY, blacklistBoxX, blacklistBoxY, blacklistBoxW, blacklistBoxH)) {
            drawHoveringText(Arrays.asList("§e黑名单卡片列表", "§7这里展示所有黑名单关键字。", "§7右侧 §cX §7可删除该项。"), mouseX, mouseY);
        }
    }

    private void drawPresetPanel(int mouseX, int mouseY) {
        int visibleRows = getPresetVisibleRows();
        int startIndex = this.presetScrollOffset;
        int contentX = this.presetListX;
        int contentY = this.presetListY + 4;
        int cardWidth = this.presetListW;

        drawRect(this.presetListX, this.presetListY, this.presetListX + this.presetListW, this.presetListY + this.presetListH,
                0x44202A36);
        drawHorizontalLine(this.presetListX, this.presetListX + this.presetListW, this.presetListY, 0xFF4FA6D9);
        drawHorizontalLine(this.presetListX, this.presetListX + this.presetListW, this.presetListY + this.presetListH,
                0xFF35536C);
        drawVerticalLine(this.presetListX, this.presetListY, this.presetListY + this.presetListH, 0xFF35536C);
        drawVerticalLine(this.presetListX + this.presetListW, this.presetListY, this.presetListY + this.presetListH,
                0xFF35536C);

        if (this.presetCards.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, "§7暂无预设，点击上方“新增预设”保存当前配置",
                    this.presetListX + this.presetListW / 2,
                    this.presetListY + this.presetListH / 2 - this.fontRenderer.FONT_HEIGHT / 2, 0xFFAAAAAA);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int presetIndex = startIndex + i;
            if (presetIndex >= this.presetCards.size()) {
                break;
            }
            KillAuraHandler.KillAuraPreset preset = this.presetCards.get(presetIndex);
            int cardY = contentY + i * (PRESET_CARD_HEIGHT + PRESET_CARD_GAP);
            boolean hovered = isMouseInside(mouseX, mouseY, contentX, cardY, cardWidth, PRESET_CARD_HEIGHT);
            boolean selected = presetIndex == this.selectedPresetIndex;
            UiState state = selected ? UiState.SELECTED : (hovered ? UiState.HOVER : UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(contentX, cardY, cardWidth, PRESET_CARD_HEIGHT, state);
            this.drawString(this.fontRenderer, trimToWidth("§f" + safe(preset.name), cardWidth - 12),
                    contentX + 6, cardY + 4, 0xFFFFFFFF);
            String summary = getAttackModeDisplayName(preset.attackMode) + " | 范围 " + formatFloat(preset.attackRange)
                    + " | Hunt " + getHuntModeDisplayName(preset.huntMode);
            this.drawString(this.fontRenderer, trimToWidth("§7" + summary, cardWidth - 12), contentX + 6, cardY + 14,
                    0xFFB8C7D9);
        }

        if (this.presetMaxScroll > 0) {
            int scrollbarX = this.presetListX + this.presetListW - 5;
            int scrollbarY = this.presetListY + 4;
            int scrollbarHeight = Math.max(8, this.presetListH - 8);
            int thumbHeight = Math.max(12,
                    (int) ((visibleRows / (float) this.presetCards.size()) * scrollbarHeight));
            int thumbTravel = Math.max(1, scrollbarHeight - thumbHeight);
            int thumbY = scrollbarY + (int) ((this.presetScrollOffset / (float) this.presetMaxScroll) * thumbTravel);
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 3, scrollbarHeight, thumbY, thumbHeight);
        }
    }

    private void drawGroupTabs(int mouseX, int mouseY) {
        drawCustomSectionFrame("功能分组", this.groupBarX, this.groupBarY, this.groupBarW, this.groupBarH);

        int currentX = this.groupTabsX - this.groupTabScroll;
        for (ConfigGroup group : ConfigGroup.values()) {
            int tabWidth = getGroupTabWidth(group);
            if (currentX + tabWidth >= this.groupTabsX && currentX <= this.groupTabsX + this.groupTabsW) {
                boolean hovered = isMouseInside(mouseX, mouseY, currentX, this.groupTabsY, tabWidth, this.groupTabsH);
                boolean selected = group == this.selectedGroup;
                UiState state = selected ? UiState.SUCCESS : (hovered ? UiState.HOVER : UiState.NORMAL);
                GuiTheme.drawButtonFrameSafe(currentX, this.groupTabsY, tabWidth, this.groupTabsH, state);
                if (selected) {
                    drawRect(currentX + 3, this.groupTabsY + this.groupTabsH - 2, currentX + tabWidth - 3,
                            this.groupTabsY + this.groupTabsH, 0xFF8EE2FF);
                }
                int textColor = selected ? 0xFFEAFBFF : GuiTheme.getStateTextColor(state);
                this.drawCenteredString(this.fontRenderer, group.tabLabel, currentX + tabWidth / 2, this.groupTabsY + 6,
                        textColor);
            }
            currentX += tabWidth + GROUP_TAB_GAP;
        }

        if (this.groupTabMaxScroll > 0) {
            int trackX = this.groupTabsX;
            int trackY = this.groupBarY + this.groupBarH - GROUP_SCROLLBAR_HEIGHT - 4;
            int trackW = this.groupTabsW;
            int thumbW = Math.max(18, (int) ((this.groupTabsW / (float) this.groupTabContentWidth) * trackW));
            int thumbTravel = Math.max(1, trackW - thumbW);
            int thumbX = trackX + (int) ((this.groupTabScroll / (float) this.groupTabMaxScroll) * thumbTravel);
            drawRect(trackX, trackY, trackX + trackW, trackY + GROUP_SCROLLBAR_HEIGHT, 0xAA1B2733);
            drawRect(thumbX, trackY, thumbX + thumbW, trackY + GROUP_SCROLLBAR_HEIGHT, 0xFF4FA6D9);
        }
    }

    private void drawCustomSectionFrame(String title, int boxX, int boxY, int boxW, int boxH) {
        if (boxW <= 0 || boxH <= 0) {
            return;
        }
        drawRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x44202A36);
        drawHorizontalLine(boxX, boxX + boxW, boxY, 0xFF4FA6D9);
        drawHorizontalLine(boxX, boxX + boxW, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX, boxY, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX + boxW, boxY, boxY + boxH, 0xFF35536C);
        this.drawString(this.fontRenderer, "§b" + title, boxX + 6, boxY + 5, 0xFFE8F6FF);
    }

    private void drawNameListBox(String title, List<String> names, int x, int y, int width, int height,
            int scrollOffset, int mouseX, int mouseY, int accentColor, boolean whitelistList) {
        if (width <= 0 || height <= 0) {
            return;
        }

        drawRect(x, y, x + width, y + height, 0x55202A36);
        drawHorizontalLine(x, x + width, y, accentColor);
        drawHorizontalLine(x, x + width, y + height, 0xFF35536C);
        drawVerticalLine(x, y, y + height, 0xFF35536C);
        drawVerticalLine(x + width, y, y + height, 0xFF35536C);

        String header = title + " (" + (names == null ? 0 : names.size()) + ")";
        this.drawString(this.fontRenderer, "§f" + header, x + 5, y + 3, 0xFFFFFFFF);
        if (whitelistList) {
            boolean canMoveUp = this.selectedWhitelistIndex > 0;
            boolean canMoveDown = this.selectedWhitelistIndex >= 0
                    && names != null
                    && this.selectedWhitelistIndex < names.size() - 1;
            drawNameListHeaderButton(getWhitelistMoveUpButtonX(), getWhitelistHeaderButtonY(),
                    getNameListHeaderButtonSize(), "^", canMoveUp,
                    isMouseInside(mouseX, mouseY, getWhitelistMoveUpButtonX(), getWhitelistHeaderButtonY(),
                            getNameListHeaderButtonSize(), getNameListHeaderButtonSize()));
            drawNameListHeaderButton(getWhitelistMoveDownButtonX(), getWhitelistHeaderButtonY(),
                    getNameListHeaderButtonSize(), "v", canMoveDown,
                    isMouseInside(mouseX, mouseY, getWhitelistMoveDownButtonX(), getWhitelistHeaderButtonY(),
                            getNameListHeaderButtonSize(), getNameListHeaderButtonSize()));
        }

        int contentX = x + NAME_LIST_INNER_PADDING;
        int contentY = y + NAME_LIST_HEADER_HEIGHT + NAME_LIST_INNER_PADDING;
        int cardW = width - NAME_LIST_INNER_PADDING * 2;
        int visibleRows = getNameListVisibleRows(height);

        if (names == null || names.isEmpty()) {
            this.drawCenteredString(this.fontRenderer, "§7暂无条目", x + width / 2,
                    y + height / 2 - this.fontRenderer.FONT_HEIGHT / 2, 0xFFAAAAAA);
            return;
        }

        int maxScroll = getNameListMaxScroll(names, height);
        int actualScroll = clampInt(scrollOffset, 0, maxScroll);

        for (int i = 0; i < visibleRows; i++) {
            int actualIndex = actualScroll + i;
            if (actualIndex >= names.size()) {
                break;
            }

            String name = safe(names.get(actualIndex));
            int cardY = contentY + i * (NAME_LIST_CARD_HEIGHT + NAME_LIST_CARD_GAP);
            boolean hovered = isMouseInside(mouseX, mouseY, contentX, cardY, cardW, NAME_LIST_CARD_HEIGHT);
            boolean selected = whitelistList && actualIndex == this.selectedWhitelistIndex;
            boolean dragging = whitelistList && this.whitelistDragActive && actualIndex == this.whitelistDragIndex;

            int bg = selected ? 0xAA315B79 : (hovered ? 0xAA314A63 : 0x99222D39);
            if (dragging) {
                bg = 0xCC3E6E90;
            }
            int border = selected ? 0xFF9FE5FF : (hovered ? accentColor : 0xFF455A70);
            drawRect(contentX, cardY, contentX + cardW, cardY + NAME_LIST_CARD_HEIGHT, bg);
            drawHorizontalLine(contentX, contentX + cardW, cardY, border);
            drawHorizontalLine(contentX, contentX + cardW, cardY + NAME_LIST_CARD_HEIGHT, border);
            drawVerticalLine(contentX, cardY, cardY + NAME_LIST_CARD_HEIGHT, border);
            drawVerticalLine(contentX + cardW, cardY, cardY + NAME_LIST_CARD_HEIGHT, border);

            int removeX = contentX + cardW - 18;
            drawRect(removeX, cardY + 1, removeX + 16, cardY + NAME_LIST_CARD_HEIGHT - 1,
                    hovered ? 0xAA6C2B2B : 0x88522222);
            this.drawCenteredString(this.fontRenderer, "§cX", removeX + 8, cardY + 5, 0xFFFFFFFF);

            String display = whitelistList ? (actualIndex + 1) + ". " + name : name;
            display = trimToWidth(display, cardW - 24);
            this.drawString(this.fontRenderer, display, contentX + 4, cardY + 5, 0xFFFFFFFF);
        }

        if (maxScroll > 0) {
            int scrollbarX = x + width - 5;
            int scrollbarY = contentY;
            int scrollbarHeight = visibleRows * (NAME_LIST_CARD_HEIGHT + NAME_LIST_CARD_GAP) - NAME_LIST_CARD_GAP;
            int thumbHeight = Math.max(10, (int) ((visibleRows / (float) names.size()) * scrollbarHeight));
            int thumbY = scrollbarY
                    + (int) ((actualScroll / (float) maxScroll) * Math.max(1, scrollbarHeight - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 3, scrollbarHeight, thumbY, thumbHeight);
        }
    }

    private void drawSectionBox(String title, GuiButton... buttons) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (GuiButton button : buttons) {
            if (button == null || !button.visible) {
                continue;
            }
            minX = Math.min(minX, button.x);
            minY = Math.min(minY, button.y);
            maxX = Math.max(maxX, button.x + button.width);
            maxY = Math.max(maxY, button.y + button.height);
        }

        if (minX == Integer.MAX_VALUE) {
            return;
        }

        int boxX = minX - 6;
        int boxY = minY - 18;
        int boxW = (maxX - minX) + 12;
        int boxH = (maxY - minY) + 24;

        drawRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x44202A36);
        drawHorizontalLine(boxX, boxX + boxW, boxY, 0xFF4FA6D9);
        drawHorizontalLine(boxX, boxX + boxW, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX, boxY, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX + boxW, boxY, boxY + boxH, 0xFF35536C);
        this.drawString(this.fontRenderer, "§b" + title, boxX + 6, boxY + 5, 0xFFE8F6FF);
    }

    private boolean isMouseOver(int mouseX, int mouseY, GuiButton button) {
        return button != null && button.visible && mouseX >= button.x && mouseX <= button.x + button.width
                && mouseY >= button.y && mouseY <= button.y + button.height;
    }

    private void drawNameListHeaderButton(int x, int y, int size, String label, boolean enabled, boolean hovered) {
        int bg = enabled ? (hovered ? 0xAA33597A : 0x99304864) : 0x66313D4A;
        int border = enabled ? (hovered ? 0xFF9FE5FF : 0xFF5A89AA) : 0xFF4A5866;
        drawRect(x, y, x + size, y + size, bg);
        drawHorizontalLine(x, x + size, y, border);
        drawHorizontalLine(x, x + size, y + size, border);
        drawVerticalLine(x, y, y + size, border);
        drawVerticalLine(x + size, y, y + size, border);
        this.drawCenteredString(this.fontRenderer, enabled ? label : "§7" + label, x + size / 2, y + 3, 0xFFFFFFFF);
    }

    private int getNameListHeaderButtonSize() {
        return Math.max(10, NAME_LIST_HEADER_HEIGHT - 2);
    }

    private int getWhitelistHeaderButtonY() {
        return this.whitelistBoxY + 1;
    }

    private int getWhitelistMoveDownButtonX() {
        return this.whitelistBoxX + this.whitelistBoxW - NAME_LIST_INNER_PADDING - getNameListHeaderButtonSize();
    }

    private int getWhitelistMoveUpButtonX() {
        return getWhitelistMoveDownButtonX() - getNameListHeaderButtonSize() - NAME_LIST_HEADER_BUTTON_GAP;
    }

    private void moveSelectedWhitelistBy(int offset) {
        syncWhitelistSelectionState();
        if (this.selectedWhitelistIndex < 0 || KillAuraHandler.nameWhitelist == null
                || KillAuraHandler.nameWhitelist.isEmpty()) {
            return;
        }

        int targetIndex = clampInt(this.selectedWhitelistIndex + offset, 0, KillAuraHandler.nameWhitelist.size() - 1);
        if (targetIndex == this.selectedWhitelistIndex) {
            return;
        }

        moveNameListEntry(KillAuraHandler.nameWhitelist, this.selectedWhitelistIndex, targetIndex);
        this.selectedWhitelistIndex = targetIndex;
        this.whitelistDragIndex = targetIndex;
        ensureWhitelistSelectionVisible();
    }

    private void moveNameListEntry(List<String> names, int fromIndex, int toIndex) {
        if (names == null || fromIndex < 0 || toIndex < 0 || fromIndex >= names.size() || toIndex >= names.size()
                || fromIndex == toIndex) {
            return;
        }

        String moved = names.remove(fromIndex);
        names.add(toIndex, moved);
    }

    private void startWhitelistDrag(int index) {
        if (KillAuraHandler.nameWhitelist == null || index < 0 || index >= KillAuraHandler.nameWhitelist.size()) {
            stopWhitelistDrag();
            return;
        }
        this.whitelistDragActive = true;
        this.whitelistDragIndex = index;
    }

    private void updateWhitelistDrag(int mouseX, int mouseY) {
        if (!this.whitelistDragActive || this.selectedGroup != ConfigGroup.NAME_FILTER
                || KillAuraHandler.nameWhitelist == null || KillAuraHandler.nameWhitelist.size() <= 1) {
            return;
        }

        int targetIndex = getWhitelistDragTargetIndex(mouseX, mouseY, KillAuraHandler.nameWhitelist.size());
        if (targetIndex < 0 || targetIndex == this.whitelistDragIndex) {
            return;
        }

        moveNameListEntry(KillAuraHandler.nameWhitelist, this.whitelistDragIndex, targetIndex);
        this.whitelistDragIndex = targetIndex;
        this.selectedWhitelistIndex = targetIndex;
        ensureWhitelistSelectionVisible();
    }

    private int getWhitelistDragTargetIndex(int mouseX, int mouseY, int size) {
        if (size <= 0) {
            return -1;
        }

        int visibleRows = getNameListVisibleRows(this.whitelistBoxH);
        int contentY = this.whitelistBoxY + NAME_LIST_HEADER_HEIGHT + NAME_LIST_INNER_PADDING;
        int contentHeight = visibleRows * (NAME_LIST_CARD_HEIGHT + NAME_LIST_CARD_GAP) - NAME_LIST_CARD_GAP;
        int maxScroll = getNameListMaxScroll(KillAuraHandler.nameWhitelist, this.whitelistBoxH);

        if (mouseY < contentY && this.whitelistListScroll > 0) {
            this.whitelistListScroll = clampInt(this.whitelistListScroll - 1, 0, maxScroll);
        } else if (mouseY > contentY + contentHeight && this.whitelistListScroll < maxScroll) {
            this.whitelistListScroll = clampInt(this.whitelistListScroll + 1, 0, maxScroll);
        }

        if (mouseY <= contentY) {
            return clampInt(this.whitelistListScroll, 0, size - 1);
        }
        if (mouseY >= contentY + contentHeight) {
            return clampInt(this.whitelistListScroll + visibleRows - 1, 0, size - 1);
        }

        int row = clampInt((mouseY - contentY) / (NAME_LIST_CARD_HEIGHT + NAME_LIST_CARD_GAP), 0, visibleRows - 1);
        return clampInt(this.whitelistListScroll + row, 0, size - 1);
    }

    private void stopWhitelistDrag() {
        this.whitelistDragActive = false;
        this.whitelistDragIndex = this.selectedWhitelistIndex;
    }

    private void ensureWhitelistSelectionVisible() {
        if (this.selectedWhitelistIndex < 0) {
            return;
        }
        int visibleRows = getNameListVisibleRows(this.whitelistBoxH);
        if (this.selectedWhitelistIndex < this.whitelistListScroll) {
            this.whitelistListScroll = this.selectedWhitelistIndex;
        } else if (this.selectedWhitelistIndex >= this.whitelistListScroll + visibleRows) {
            this.whitelistListScroll = this.selectedWhitelistIndex - visibleRows + 1;
        }
        this.whitelistListScroll = clampInt(this.whitelistListScroll, 0,
                getNameListMaxScroll(KillAuraHandler.nameWhitelist, this.whitelistBoxH));
    }

    private void syncWhitelistSelectionState() {
        int size = KillAuraHandler.nameWhitelist == null ? 0 : KillAuraHandler.nameWhitelist.size();
        if (size <= 0) {
            clearWhitelistSelectionState();
            return;
        }
        if (this.selectedWhitelistIndex >= size) {
            this.selectedWhitelistIndex = size - 1;
        }
        if (this.whitelistDragIndex >= size) {
            this.whitelistDragIndex = size - 1;
        }
        if (this.whitelistDragIndex < 0 && this.whitelistDragActive) {
            this.whitelistDragIndex = Math.max(0, this.selectedWhitelistIndex);
        }
    }

    private void clearWhitelistSelectionState() {
        this.selectedWhitelistIndex = -1;
        this.whitelistDragActive = false;
        this.whitelistDragIndex = -1;
    }

    private boolean isMouseInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void applyAttackMode(String mode) {
        if (isBlank(mode)) {
            mode = KillAuraHandler.ATTACK_MODE_NORMAL;
        }
        KillAuraHandler.attackMode = mode;
        if (KillAuraHandler.ATTACK_MODE_PACKET.equalsIgnoreCase(KillAuraHandler.attackMode)) {
            KillAuraHandler.rotateToTarget = false;
            KillAuraHandler.smoothRotation = false;
        }
    }

    private void applyHuntMode(String mode) {
        if (isBlank(mode)) {
            mode = KillAuraHandler.HUNT_MODE_APPROACH;
        }
        KillAuraHandler.setHuntMode(mode);
    }

    private String getAttackModeDisplayName(String mode) {
        if (KillAuraHandler.ATTACK_MODE_PACKET.equalsIgnoreCase(mode)) {
            return "数据包攻击";
        }
        if (KillAuraHandler.ATTACK_MODE_TELEPORT.equalsIgnoreCase(mode)) {
            return "TP攻击";
        }
        if (KillAuraHandler.ATTACK_MODE_SEQUENCE.equalsIgnoreCase(mode)) {
            return "执行序列";
        }
        return "普通攻击";
    }

    private String getHuntModeDisplayName(String mode) {
        if (KillAuraHandler.HUNT_MODE_FIXED_DISTANCE.equalsIgnoreCase(mode)) {
            return "固定距离";
        }
        if (KillAuraHandler.HUNT_MODE_OFF.equalsIgnoreCase(mode)) {
            return "关闭";
        }
        return "靠近目标";
    }

    private void applyDefaultValues() {
        KillAuraHandler.rotateToTarget = true;
        KillAuraHandler.smoothRotation = true;
        KillAuraHandler.requireLineOfSight = true;
        KillAuraHandler.targetHostile = true;
        KillAuraHandler.targetPassive = false;
        KillAuraHandler.targetPlayers = false;
        KillAuraHandler.onlyWeapon = false;
        KillAuraHandler.aimOnlyMode = false;
        KillAuraHandler.focusSingleTarget = true;
        KillAuraHandler.ignoreInvisible = true;
        KillAuraHandler.enableNoCollision = true;
        KillAuraHandler.enableAntiKnockback = true;
        KillAuraHandler.enableFullBrightVision = false;
        KillAuraHandler.fullBrightGamma = 1000.0F;
        KillAuraHandler.attackMode = KillAuraHandler.ATTACK_MODE_NORMAL;
        KillAuraHandler.aimYawOffset = 0.0F;
        KillAuraHandler.huntEnabled = true;
        KillAuraHandler.huntMode = KillAuraHandler.HUNT_MODE_APPROACH;
        KillAuraHandler.huntPickupItemsEnabled = false;
        KillAuraHandler.visualizeHuntRadius = false;
        KillAuraHandler.huntRadius = 8.0F;
        KillAuraHandler.huntFixedDistance = 4.2F;
        KillAuraHandler.huntOrbitEnabled = false;
        KillAuraHandler.huntJumpOrbitEnabled = true;
        KillAuraHandler.huntOrbitSamplePoints = KillAuraHandler.DEFAULT_HUNT_ORBIT_SAMPLE_POINTS;
        KillAuraHandler.enableNameWhitelist = false;
        KillAuraHandler.enableNameBlacklist = false;
        KillAuraHandler.nameWhitelist.clear();
        KillAuraHandler.nameBlacklist.clear();
        clearWhitelistSelectionState();
        KillAuraHandler.nearbyEntityScanRange = 10.0F;
        KillAuraHandler.attackRange = 4.2F;
        KillAuraHandler.minAttackStrength = 0.92F;
        KillAuraHandler.minTurnSpeed = 4.0F;
        KillAuraHandler.maxTurnSpeed = 18.0F;
        KillAuraHandler.minAttackIntervalTicks = 2;
        KillAuraHandler.targetsPerAttack = 1;
        KillAuraHandler.attackSequenceName = "";
        KillAuraHandler.attackSequenceDelayTicks = 2;
        this.whitelistListScroll = 0;
        this.blacklistListScroll = 0;
        this.attackModeDropdown.collapse();
        this.huntModeDropdown.collapse();
    }

    private void enforceAtLeastOneTargetType() {
        if (!KillAuraHandler.targetHostile && !KillAuraHandler.targetPassive && !KillAuraHandler.targetPlayers) {
            KillAuraHandler.targetHostile = true;
        }
    }

    private void openFloatInput(String title, float current, float min, float max, FloatConsumer consumer) {
        mc.displayGuiScreen(new GuiTextInput(this, title, formatFloat(current), value -> {
            float parsed = current;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            parsed = clampFloat(parsed, min, max);
            consumer.accept(parsed);
            mc.displayGuiScreen(this);
        }));
    }

    private void openPreciseFloatInput(String title, float current, float min, float max, FloatConsumer consumer) {
        mc.displayGuiScreen(new GuiTextInput(this, title, formatPreciseFloat(current), value -> {
            float parsed = current;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            parsed = clampFloat(parsed, min, max);
            consumer.accept(parsed);
            mc.displayGuiScreen(this);
        }));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatPreciseFloat(float value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private String formatSignedPreciseFloat(float value) {
        return String.format(Locale.ROOT, "%+.4f", value);
    }

    private String stateText(boolean enabled) {
        return enabled ? "§a开启" : "§c关闭";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private boolean isBlank(String text) {
        return safe(text).trim().isEmpty();
    }

    private String trimToWidth(String text, int width) {
        if (this.fontRenderer == null) {
            return safe(text);
        }
        String safeText = safe(text);
        if (this.fontRenderer.getStringWidth(safeText) <= width) {
            return safeText;
        }
        return this.fontRenderer.trimStringToWidth(safeText,
                Math.max(0, width - this.fontRenderer.getStringWidth("..."))) + "...";
    }

    private enum ConfigGroup {
        PRESET("预设", "预设"),
        ATTACK("攻击方式", "攻击方式"), TARGET("目标筛选", "目标筛选"), NAME_FILTER("名称名单", "目标名称黑白名单"), HUNT("追击 / Hunt", "追击 / Hunt"),
        PROTECTION("运动保护", "运动保护"), STATS("攻击参数", "攻击参数");

        private final String tabLabel;
        private final String contentTitle;

        ConfigGroup(String tabLabel, String contentTitle) {
            this.tabLabel = tabLabel;
            this.contentTitle = contentTitle;
        }
    }

    private interface FloatConsumer {
        void accept(float value);
    }

    private class AttackModeDropdown {
        private final List<String> options = Arrays.asList(
                KillAuraHandler.ATTACK_MODE_NORMAL,
                KillAuraHandler.ATTACK_MODE_PACKET,
                KillAuraHandler.ATTACK_MODE_TELEPORT,
                KillAuraHandler.ATTACK_MODE_SEQUENCE);
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean expanded = false;
        private boolean enabled = true;

        void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (!enabled) {
                this.expanded = false;
            }
        }

        void syncFromCurrentMode() {
            if (!this.options.contains(KillAuraHandler.attackMode)) {
                KillAuraHandler.attackMode = KillAuraHandler.ATTACK_MODE_NORMAL;
            }
        }

        boolean isExpanded() {
            return this.expanded;
        }

        void collapse() {
            this.expanded = false;
        }

        boolean isHoveringMain(int mouseX, int mouseY) {
            return isMouseInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }

        boolean isHoveringAnyPart(int mouseX, int mouseY) {
            if (isHoveringMain(mouseX, mouseY)) {
                return true;
            }
            if (!this.expanded) {
                return false;
            }
            return isMouseInside(mouseX, mouseY, this.x, this.y + this.height, this.width,
                    this.options.size() * this.height);
        }

        void drawMain(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
            boolean hover = isHoveringMain(mouseX, mouseY);
            int bg = this.enabled ? (hover ? 0xCC203146 : 0xCC152433) : 0xAA18212D;
            int border = !this.enabled ? 0xFF4E5F73 : (this.expanded ? 0xFF76D1FF : (hover ? 0xFF4FA6D9 : 0xFF3F6A8C));

            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, bg);
            drawHorizontalLine(this.x, this.x + this.width, this.y, border);
            drawHorizontalLine(this.x, this.x + this.width, this.y + this.height, border);
            drawVerticalLine(this.x, this.y, this.y + this.height, border);
            drawVerticalLine(this.x + this.width, this.y, this.y + this.height, border);

            String prefix = "攻击模式: ";
            String modeText = KillAuraHandler.aimOnlyMode ? "执行序列(只瞄准默认)"
                    : getAttackModeDisplayName(KillAuraHandler.attackMode);
            int color = this.enabled ? 0xFFEAF7FF : 0xFF96A7B8;
            drawString(fontRenderer, trimToWidth(prefix + modeText, this.width - 22), this.x + 5, this.y + 6, color);
            drawString(fontRenderer, this.enabled ? (this.expanded ? "▲" : "▼") : "-", this.x + this.width - 10,
                    this.y + 6, 0xFF9FDFFF);
        }

        void drawExpanded(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
            if (!this.expanded || !this.enabled) {
                return;
            }
            int startY = this.y + this.height;
            for (int i = 0; i < this.options.size(); i++) {
                String mode = this.options.get(i);
                int itemY = startY + i * this.height;
                boolean hover = isMouseInside(mouseX, mouseY, this.x, itemY, this.width, this.height);
                boolean selected = mode.equalsIgnoreCase(KillAuraHandler.attackMode);
                int bg = selected ? 0xEE2B5A7C : (hover ? 0xCC29455E : 0xCC1B2D3D);
                int border = selected ? 0xFF76D1FF : 0xFF35536C;

                drawRect(this.x, itemY, this.x + this.width, itemY + this.height, bg);
                drawHorizontalLine(this.x, this.x + this.width, itemY, border);
                drawHorizontalLine(this.x, this.x + this.width, itemY + this.height, border);
                drawVerticalLine(this.x, itemY, itemY + this.height, border);
                drawVerticalLine(this.x + this.width, itemY, itemY + this.height, border);

                drawString(fontRenderer, trimToWidth(getAttackModeDisplayName(mode), this.width - 10), this.x + 5,
                        itemY + 6, 0xFFFFFFFF);
            }
        }

        boolean handleClick(int mouseX, int mouseY) {
            if (isHoveringMain(mouseX, mouseY)) {
                if (this.enabled) {
                    this.expanded = !this.expanded;
                }
                return true;
            }
            if (!this.expanded || !this.enabled) {
                return false;
            }
            int startY = this.y + this.height;
            for (int i = 0; i < this.options.size(); i++) {
                int itemY = startY + i * this.height;
                if (isMouseInside(mouseX, mouseY, this.x, itemY, this.width, this.height)) {
                    applyAttackMode(this.options.get(i));
                    this.expanded = false;
                    return true;
                }
            }
            return false;
        }
    }

    private class HuntModeDropdown {
        private final List<String> options = Arrays.asList(
                KillAuraHandler.HUNT_MODE_APPROACH,
                KillAuraHandler.HUNT_MODE_FIXED_DISTANCE,
                KillAuraHandler.HUNT_MODE_OFF);
        private int x;
        private int y;
        private int width;
        private int height;
        private boolean expanded = false;
        private boolean enabled = true;

        void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (!enabled) {
                this.expanded = false;
            }
        }

        void syncFromCurrentMode() {
            if (!this.options.contains(KillAuraHandler.huntMode)) {
                KillAuraHandler.setHuntMode(KillAuraHandler.HUNT_MODE_APPROACH);
            }
        }

        boolean isExpanded() {
            return this.expanded;
        }

        void collapse() {
            this.expanded = false;
        }

        boolean isHoveringMain(int mouseX, int mouseY) {
            return isMouseInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }

        boolean isHoveringAnyPart(int mouseX, int mouseY) {
            if (isHoveringMain(mouseX, mouseY)) {
                return true;
            }
            if (!this.expanded) {
                return false;
            }
            return isMouseInside(mouseX, mouseY, this.x, this.y + this.height, this.width,
                    this.options.size() * this.height);
        }

        void drawMain(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
            boolean hover = isHoveringMain(mouseX, mouseY);
            int bg = this.enabled ? (hover ? 0xCC203146 : 0xCC152433) : 0xAA18212D;
            int border = !this.enabled ? 0xFF4E5F73 : (this.expanded ? 0xFF76D1FF : (hover ? 0xFF4FA6D9 : 0xFF3F6A8C));

            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, bg);
            drawHorizontalLine(this.x, this.x + this.width, this.y, border);
            drawHorizontalLine(this.x, this.x + this.width, this.y + this.height, border);
            drawVerticalLine(this.x, this.y, this.y + this.height, border);
            drawVerticalLine(this.x + this.width, this.y, this.y + this.height, border);

            String prefix = "追击模式: ";
            int color = this.enabled ? 0xFFEAF7FF : 0xFF96A7B8;
            drawString(fontRenderer, trimToWidth(prefix + getHuntModeDisplayName(KillAuraHandler.huntMode),
                    this.width - 22), this.x + 5, this.y + 6, color);
            drawString(fontRenderer, this.enabled ? (this.expanded ? "▲" : "▼") : "-", this.x + this.width - 10,
                    this.y + 6, 0xFF9FDFFF);
        }

        void drawExpanded(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
            if (!this.expanded || !this.enabled) {
                return;
            }

            int startY = this.y + this.height;
            for (int i = 0; i < this.options.size(); i++) {
                String mode = this.options.get(i);
                int itemY = startY + i * this.height;
                boolean hover = isMouseInside(mouseX, mouseY, this.x, itemY, this.width, this.height);
                boolean selected = mode.equalsIgnoreCase(KillAuraHandler.huntMode);
                int bg = selected ? 0xEE2B5A7C : (hover ? 0xCC29455E : 0xCC1B2D3D);
                int border = selected ? 0xFF76D1FF : 0xFF35536C;

                drawRect(this.x, itemY, this.x + this.width, itemY + this.height, bg);
                drawHorizontalLine(this.x, this.x + this.width, itemY, border);
                drawHorizontalLine(this.x, this.x + this.width, itemY + this.height, border);
                drawVerticalLine(this.x, itemY, itemY + this.height, border);
                drawVerticalLine(this.x + this.width, itemY, itemY + this.height, border);

                drawString(fontRenderer, trimToWidth(getHuntModeDisplayName(mode), this.width - 10), this.x + 5,
                        itemY + 6, 0xFFFFFFFF);
            }
        }

        boolean handleClick(int mouseX, int mouseY) {
            if (isHoveringMain(mouseX, mouseY)) {
                if (this.enabled) {
                    this.expanded = !this.expanded;
                }
                return true;
            }
            if (!this.expanded || !this.enabled) {
                return false;
            }

            int startY = this.y + this.height;
            for (int i = 0; i < this.options.size(); i++) {
                int itemY = startY + i * this.height;
                if (isMouseInside(mouseX, mouseY, this.x, itemY, this.width, this.height)) {
                    applyHuntMode(this.options.get(i));
                    this.expanded = false;
                    return true;
                }
            }
            return false;
        }
    }

    private class NearbyNameDropdown {
        private final List<String> options = new ArrayList<>();
        private int x;
        private int y;
        private int width;
        private int height;
        private int selectedIndex = -1;
        private boolean expanded = false;
        private int scrollOffset = 0;
        private final int maxVisibleItems = 6;

        void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void setOptions(List<String> newOptions) {
            this.options.clear();
            if (newOptions != null) {
                for (String option : newOptions) {
                    String normalized = KillAuraHandler.normalizeFilterName(option);
                    if (!normalized.isEmpty() && !containsIgnoreCase(this.options, normalized)) {
                        this.options.add(normalized);
                    }
                }
            }
            if (this.options.isEmpty()) {
                this.selectedIndex = -1;
                this.scrollOffset = 0;
                this.expanded = false;
            } else {
                if (this.selectedIndex < 0 || this.selectedIndex >= this.options.size()) {
                    this.selectedIndex = 0;
                }
                this.scrollOffset = clampInt(this.scrollOffset, 0, getMaxScroll());
            }
        }

        String getSelectedValue() {
            if (this.selectedIndex < 0 || this.selectedIndex >= this.options.size()) {
                return "";
            }
            return safe(this.options.get(this.selectedIndex));
        }

        boolean isExpanded() {
            return this.expanded;
        }

        void collapse() {
            this.expanded = false;
        }

        boolean isHoveringMain(int mouseX, int mouseY) {
            return isMouseInside(mouseX, mouseY, this.x, this.y, this.width, this.height);
        }

        boolean isHoveringAnyPart(int mouseX, int mouseY) {
            if (isHoveringMain(mouseX, mouseY)) {
                return true;
            }
            if (!this.expanded) {
                return false;
            }
            int listHeight = Math.min(this.options.size(), this.maxVisibleItems) * this.height;
            return isMouseInside(mouseX, mouseY, this.x, this.y + this.height, this.width, listHeight);
        }

        void drawMain(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
            boolean hover = isHoveringMain(mouseX, mouseY);
            int bg = hover ? 0xCC203146 : 0xCC152433;
            int border = this.expanded ? 0xFF76D1FF : (hover ? 0xFF4FA6D9 : 0xFF3F6A8C);

            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, bg);
            drawHorizontalLine(this.x, this.x + this.width, this.y, border);
            drawHorizontalLine(this.x, this.x + this.width, this.y + this.height, border);
            drawVerticalLine(this.x, this.y, this.y + this.height, border);
            drawVerticalLine(this.x + this.width, this.y, this.y + this.height, border);

            String mainText = this.options.isEmpty() ? "§7暂无扫描结果" : trimToWidth(getSelectedValue(), this.width - 22);
            drawString(fontRenderer, mainText, this.x + 5, this.y + 6, 0xFFEAF7FF);
            drawString(fontRenderer, this.expanded ? "▲" : "▼", this.x + this.width - 10, this.y + 6, 0xFF9FDFFF);
        }

        void drawExpanded(int mouseX, int mouseY, net.minecraft.client.gui.FontRenderer fontRenderer) {
            if (!this.expanded || this.options.isEmpty()) {
                return;
            }

            int visible = Math.min(this.options.size(), this.maxVisibleItems);
            int startY = this.y + this.height;
            for (int i = 0; i < visible; i++) {
                int actualIndex = this.scrollOffset + i;
                if (actualIndex >= this.options.size()) {
                    break;
                }

                int itemY = startY + i * this.height;
                boolean hover = isMouseInside(mouseX, mouseY, this.x, itemY, this.width, this.height);
                boolean selected = actualIndex == this.selectedIndex;
                int bg = selected ? 0xEE2B5A7C : (hover ? 0xCC29455E : 0xCC1B2D3D);
                int border = selected ? 0xFF76D1FF : 0xFF35536C;

                drawRect(this.x, itemY, this.x + this.width, itemY + this.height, bg);
                drawHorizontalLine(this.x, this.x + this.width, itemY, border);
                drawHorizontalLine(this.x, this.x + this.width, itemY + this.height, border);
                drawVerticalLine(this.x, itemY, itemY + this.height, border);
                drawVerticalLine(this.x + this.width, itemY, itemY + this.height, border);

                drawString(fontRenderer, trimToWidth(this.options.get(actualIndex), this.width - 10), this.x + 5,
                        itemY + 6, 0xFFFFFFFF);
            }

            if (getMaxScroll() > 0) {
                int listHeight = visible * this.height;
                int thumbHeight = Math.max(10, (int) ((visible / (float) this.options.size()) * listHeight));
                int thumbY = startY
                        + (int) ((this.scrollOffset / (float) getMaxScroll()) * Math.max(1, listHeight - thumbHeight));
                GuiTheme.drawScrollbar(this.x + this.width - 4, startY, 3, listHeight, thumbY, thumbHeight);
            }
        }

        boolean handleClick(int mouseX, int mouseY) {
            if (isHoveringMain(mouseX, mouseY)) {
                if (!this.options.isEmpty()) {
                    this.expanded = !this.expanded;
                }
                return true;
            }

            if (!this.expanded) {
                return false;
            }

            int visible = Math.min(this.options.size(), this.maxVisibleItems);
            int startY = this.y + this.height;
            for (int i = 0; i < visible; i++) {
                int actualIndex = this.scrollOffset + i;
                if (actualIndex >= this.options.size()) {
                    break;
                }
                int itemY = startY + i * this.height;
                if (isMouseInside(mouseX, mouseY, this.x, itemY, this.width, this.height)) {
                    this.selectedIndex = actualIndex;
                    this.expanded = false;
                    return true;
                }
            }
            return false;
        }

        boolean handleWheel(int wheel, int mouseX, int mouseY) {
            if (!this.expanded || getMaxScroll() <= 0) {
                return false;
            }
            int listHeight = Math.min(this.options.size(), this.maxVisibleItems) * this.height;
            if (!isMouseInside(mouseX, mouseY, this.x, this.y + this.height, this.width, listHeight)
                    && !isHoveringMain(mouseX, mouseY)) {
                return false;
            }

            if (wheel < 0) {
                this.scrollOffset = clampInt(this.scrollOffset + 1, 0, getMaxScroll());
            } else {
                this.scrollOffset = clampInt(this.scrollOffset - 1, 0, getMaxScroll());
            }
            return true;
        }

        private int getMaxScroll() {
            return Math.max(0, this.options.size() - this.maxVisibleItems);
        }

        private boolean containsIgnoreCase(List<String> values, String target) {
            for (String value : values) {
                if (value != null && value.equalsIgnoreCase(target)) {
                    return true;
                }
            }
            return false;
        }
    }
}
