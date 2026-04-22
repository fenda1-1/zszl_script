package com.zszl.zszlScriptMod.gui.path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.debug.GuiGuiInspectorManager;
import com.zszl.zszlScriptMod.gui.path.trigger.LegacyTriggerEventItem;
import com.zszl.zszlScriptMod.gui.path.trigger.LegacyTriggerEventLibrary;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiInspectionManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GuiLegacySequenceTriggerRules extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_DELETE = 2;
    private static final int BTN_RELOAD = 3;
    private static final int BTN_VALIDATE = 4;
    private static final int BTN_SAVE = 5;
    private static final int BTN_BACK = 6;
    private static final int BTN_SELECT_SEQUENCE = 20;
    private static final int BTN_TOGGLE_ENABLED = 21;
    private static final int BTN_TOGGLE_BACKGROUND = 22;
    private static final int BTN_PACKET_DIRECTION = 23;
    private static final int BTN_IMPORT_GUI = 24;
    private static final int BTN_OPEN_GUI_INSPECTOR = 25;
    private static final int BTN_IMPORT_PACKET = 26;
    private static final int BTN_IDLE_EXCLUDE_PATH = 27;
    private static final int BTN_IDLE_IGNORE_DAMAGE = 28;

    private static final int RULE_CARD_H = 36;
    private static final int RULE_CARD_GAP = 4;
    private static final int LIB_ROW_H = 28;
    private static final int TAB_BAR_H = 22;
    private static final int TAB_BAR_GAP = 4;
    private static final int TAB_ARROW_W = 18;
    private static final int TAB_MIN_W = 72;
    private static final int TAB_BASE = 0;
    private static final int TAB_EVENT = 1;
    private static final int TAB_DEBUG = 2;

    private static final class EditorTabLayout {
        int barX;
        int barY;
        int barW;
        int barH;
        int arrowW;
        int leftArrowX;
        int rightArrowX;
        int arrowY;
        int arrowH;
        int tabsX;
        int tabsW;
        int tabW;
        int visibleCount;
        int startIndex;
    }

    private static final class TooltipRegion {
        final int x;
        final int y;
        final int w;
        final int h;
        final List<String> lines;

        TooltipRegion(int x, int y, int w, int h, List<String> lines) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.lines = lines;
        }
    }

    private static final List<LegacyTriggerEventItem> EVENT_LIBRARY = LegacyTriggerEventLibrary.createDefaultItems();

    private final GuiScreen parent;
    private final List<LegacySequenceTriggerManager.RuleEditModel> rules = new ArrayList<>();

    private int selectedRuleIndex = -1;
    private int listScroll = 0;
    private int libraryScroll = 0;
    private int maxListScroll = 0;
    private int maxLibraryScroll = 0;
    private boolean listCollapsed = false;
    private boolean libraryCollapsed = false;
    private int editorScroll = 0;
    private int maxEditorScroll = 0;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int libraryX;
    private int libraryY;
    private int libraryW;
    private int libraryH;
    private int editorX;
    private int editorY;
    private int editorW;
    private int editorH;
    private int fieldX;
    private int fieldW;
    private int activeEditorTab = TAB_BASE;

    private GuiTextField nameField;
    private GuiTextField sequenceField;
    private GuiTextField cooldownField;
    private GuiTextField containsField;
    private GuiTextField noteField;
    private GuiTextField librarySearchField;
    private GuiTextField eventTextField;
    private GuiTextField keyNameField;
    private GuiTextField idleDurationMsField;
    private GuiTextField damageSourceField;
    private GuiTextField damageMinField;
    private GuiTextField guiTitleField;
    private GuiTextField guiClassField;
    private GuiTextField chatTextField;
    private GuiTextField packetTextField;
    private GuiTextField packetChannelField;
    private GuiTextField timerIntervalField;
    private GuiTextField hpThresholdField;
    private GuiTextField areaFromField;
    private GuiTextField areaToField;
    private GuiTextField inventoryTextField;
    private GuiTextField inventoryFullSlotsField;
    private GuiTextField itemPickupTextField;
    private GuiTextField itemPickupCountField;
    private GuiTextField entityTextField;
    private GuiTextField entityMinCountField;

    private GuiButton selectSequenceBtn;
    private GuiButton toggleEnabledBtn;
    private GuiButton toggleBackgroundBtn;
    private GuiButton packetDirectionBtn;
    private GuiButton importGuiBtn;
    private GuiButton openGuiInspectorBtn;
    private GuiButton importPacketBtn;
    private GuiButton idleExcludePathBtn;
    private GuiButton idleIgnoreDamageBtn;

    private boolean editingEnabled = true;
    private boolean editingBackground = false;
    private String editingPacketDirection = "";
    private boolean editingIdleExcludePath = true;
    private boolean editingIdleIgnoreDamage = false;
    private String statusMessage = "§7左侧选规则，中间选事件，右侧编辑该事件专属参数。";
    private boolean workingCopyInitialized = false;
    private final List<TooltipRegion> tooltipRegions = new ArrayList<>();
    private final Set<String> collapsedLibraryGroups = new LinkedHashSet<>();
    private final List<LegacyTriggerEventItem> visibleLibraryRows = new ArrayList<>();

    public GuiLegacySequenceTriggerRules(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        if (!workingCopyInitialized) {
            reloadWorkingCopy();
            workingCopyInitialized = true;
        }
        recalcLayout();
        initFields();
        initButtons();
        if (!rules.isEmpty()) {
            selectedRuleIndex = Math.max(0, Math.min(selectedRuleIndex, rules.size() - 1));
            loadFromRule(rules.get(selectedRuleIndex));
        } else {
            clearEditor();
        }
        refreshButtons();
    }

    private void recalcLayout() {
        panelW = Math.min(1120, this.width - 24);
        panelH = Math.min(660, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int contentY = panelY + 40;
        int contentH = panelH - 76;
        int preferredLibraryW = libraryCollapsed ? getCollapsedSectionWidth("事件") : 190;
        listX = panelX + 10;
        listY = contentY;
        listW = calculateRuleListWidth(preferredLibraryW);
        listH = contentH;
        libraryX = listX + listW + 10;
        libraryY = contentY;
        libraryW = preferredLibraryW;
        libraryH = contentH;
        editorX = libraryX + libraryW + 10;
        editorY = contentY;
        editorW = panelX + panelW - 10 - editorX;
        editorH = contentH;
        int contentLeft = editorX + 10;
        int contentRight = editorX + editorW - 14;
        int labelGap = 8;
        int labelWidth = Math.min(104, Math.max(56, editorW / 5));
        int maxLabelWidth = Math.max(44, contentRight - contentLeft - 96 - labelGap);
        labelWidth = Math.min(labelWidth, maxLabelWidth);
        fieldX = contentLeft + labelWidth + labelGap;
        fieldW = Math.max(1, contentRight - fieldX);
    }

    private void initFields() {
        nameField = createField(3001);
        sequenceField = createField(3002);
        cooldownField = createField(3003);
        containsField = createField(3004);
        noteField = createField(3005);
        librarySearchField = createField(3006);
        eventTextField = createField(3007);
        keyNameField = createField(3008);
        idleDurationMsField = createField(3016);
        damageSourceField = createField(3009);
        damageMinField = createField(3010);
        guiTitleField = createField(3101);
        guiClassField = createField(3102);
        chatTextField = createField(3103);
        packetTextField = createField(3104);
        packetChannelField = createField(3105);
        timerIntervalField = createField(3106);
        hpThresholdField = createField(3107);
        areaFromField = createField(3108);
        areaToField = createField(3109);
        inventoryTextField = createField(3110);
        inventoryFullSlotsField = createField(3111);
        itemPickupTextField = createField(3112);
        itemPickupCountField = createField(3113);
        entityTextField = createField(3114);
        entityMinCountField = createField(3115);
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 120, 18);
        field.setEnableBackgroundDrawing(false);
        field.setMaxStringLength(Integer.MAX_VALUE);
        return field;
    }

    private void initButtons() {
        selectSequenceBtn = new ThemedButton(BTN_SELECT_SEQUENCE, 0, 0, 96, 20, "选择序列");
        toggleEnabledBtn = new ThemedButton(BTN_TOGGLE_ENABLED, 0, 0, 120, 20, "");
        toggleBackgroundBtn = new ThemedButton(BTN_TOGGLE_BACKGROUND, 0, 0, 120, 20, "");
        packetDirectionBtn = new ThemedButton(BTN_PACKET_DIRECTION, 0, 0, 120, 20, "");
        importGuiBtn = new ThemedButton(BTN_IMPORT_GUI, 0, 0, 96, 20, "填入最近GUI");
        openGuiInspectorBtn = new ThemedButton(BTN_OPEN_GUI_INSPECTOR, 0, 0, 96, 20, "打开识别器");
        importPacketBtn = new ThemedButton(BTN_IMPORT_PACKET, 0, 0, 96, 20, "填入最近包");
        idleExcludePathBtn = new ThemedButton(BTN_IDLE_EXCLUDE_PATH, 0, 0, 120, 20, "");
        idleIgnoreDamageBtn = new ThemedButton(BTN_IDLE_IGNORE_DAMAGE, 0, 0, 120, 20, "");
        buttonList.add(selectSequenceBtn);
        buttonList.add(toggleEnabledBtn);
        buttonList.add(toggleBackgroundBtn);
        buttonList.add(packetDirectionBtn);
        buttonList.add(importGuiBtn);
        buttonList.add(openGuiInspectorBtn);
        buttonList.add(importPacketBtn);
        buttonList.add(idleExcludePathBtn);
        buttonList.add(idleIgnoreDamageBtn);

        int footerY = panelY + panelH - 28;
        buttonList.add(new ThemedButton(BTN_ADD, panelX + 10, footerY, 76, 20, "新增"));
        buttonList.add(new ThemedButton(BTN_DELETE, panelX + 92, footerY, 76, 20, "删除"));
        buttonList.add(new ThemedButton(BTN_RELOAD, panelX + 174, footerY, 76, 20, "重载"));
        buttonList.add(new ThemedButton(BTN_VALIDATE, panelX + panelW - 268, footerY, 80, 20, "校验"));
        buttonList.add(new ThemedButton(BTN_SAVE, panelX + panelW - 182, footerY, 80, 20, "保存"));
        buttonList.add(new ThemedButton(BTN_BACK, panelX + panelW - 96, footerY, 80, 20, "返回"));
    }

    private void reloadWorkingCopy() {
        rules.clear();
        rules.addAll(LegacySequenceTriggerManager.getRuleModels());
        if (rules.isEmpty()) {
            selectedRuleIndex = -1;
        } else {
            selectedRuleIndex = Math.max(0, Math.min(selectedRuleIndex, rules.size() - 1));
        }
    }

    private void loadFromRule(LegacySequenceTriggerManager.RuleEditModel rule) {
        JsonObject params = copyJson(rule.params);
        nameField.setText(safe(rule.name));
        sequenceField.setText(safe(rule.sequenceName));
        cooldownField.setText(String.valueOf(Math.max(0, rule.cooldownMs)));
        containsField.setText(safe(rule.contains));
        noteField.setText(safe(rule.note));
        editingEnabled = rule.enabled;
        editingBackground = rule.backgroundExecution;
        eventTextField.setText(stringParam(params, "text"));
        keyNameField.setText(stringParam(params, "keyName"));
        idleDurationMsField.setText(String.valueOf(intParam(params, "idleMs", 1000)));
        damageSourceField.setText(stringParam(params, "damageSource"));
        damageMinField.setText(floatText(doubleParam(params, "minDamage", 0.0D)));
        guiTitleField.setText(stringParam(params, "guiTitle"));
        guiClassField.setText(stringParam(params, "guiClass"));
        chatTextField.setText(stringParam(params, "chatText"));
        packetTextField.setText(stringParam(params, "packetText"));
        packetChannelField.setText(stringParam(params, "channel"));
        editingPacketDirection = stringParam(params, "direction");
        timerIntervalField.setText(String.valueOf(intParam(params, "intervalSeconds", 1)));
        hpThresholdField.setText(floatText(doubleParam(params, "hpThreshold", 6.0D)));
        areaFromField.setText(stringParam(params, "fromText"));
        areaToField.setText(stringParam(params, "toText"));
        inventoryTextField.setText(stringParam(params, "inventoryText"));
        inventoryFullSlotsField.setText(String.valueOf(intParam(params, "minFilledSlots", 0)));
        itemPickupTextField.setText(stringParam(params, "itemText"));
        itemPickupCountField.setText(String.valueOf(intParam(params, "minCount", 1)));
        entityTextField.setText(stringParam(params, "entityText"));
        entityMinCountField.setText(String.valueOf(intParam(params, "minCount", 1)));
        editingIdleExcludePath = boolParam(params, "excludePathTracking", true);
        editingIdleIgnoreDamage = boolParam(params, "ignoreDamageReset", false);
    }

    private void clearEditor() {
        for (GuiTextField field : allFields()) {
            field.setText("");
        }
        cooldownField.setText("1000");
        idleDurationMsField.setText("1000");
        timerIntervalField.setText("1");
        hpThresholdField.setText("6");
        damageMinField.setText("0");
        inventoryFullSlotsField.setText("0");
        itemPickupCountField.setText("1");
        entityMinCountField.setText("1");
        editingEnabled = true;
        editingBackground = false;
        editingPacketDirection = "";
        editingIdleExcludePath = true;
        editingIdleIgnoreDamage = false;
    }

    private List<GuiTextField> allFields() {
        return Arrays.asList(nameField, sequenceField, cooldownField, containsField, noteField, guiTitleField,
                guiClassField, chatTextField, packetTextField, packetChannelField, eventTextField, keyNameField,
                idleDurationMsField, damageSourceField, damageMinField, timerIntervalField, hpThresholdField,
                areaFromField, areaToField, inventoryTextField, inventoryFullSlotsField, itemPickupTextField,
                itemPickupCountField, entityTextField, entityMinCountField);
    }

    private void refreshButtons() {
        toggleEnabledBtn.displayString = "启用规则: " + (editingEnabled ? "§aON" : "§cOFF");
        toggleBackgroundBtn.displayString = "后台执行: " + (editingBackground ? "§a是" : "§c否");
        packetDirectionBtn.displayString = "方向: " + packetDirectionLabel(editingPacketDirection);
        idleExcludePathBtn.displayString = buildIdleExcludePathButtonLabel(idleExcludePathBtn.width);
        idleIgnoreDamageBtn.displayString = buildIdleIgnoreDamageButtonLabel(idleIgnoreDamageBtn.width);
        boolean hasRule = selectedRuleIndex >= 0 && selectedRuleIndex < rules.size();
        boolean guiTrigger = hasRule && (LegacySequenceTriggerManager.TRIGGER_GUI_OPEN.equals(selectedTriggerType())
                || LegacySequenceTriggerManager.TRIGGER_GUI_CLOSE.equals(selectedTriggerType()));
        boolean packetTrigger = hasRule && LegacySequenceTriggerManager.TRIGGER_PACKET.equals(selectedTriggerType());
        boolean idleTrigger = hasRule && LegacySequenceTriggerManager.TRIGGER_PLAYER_IDLE.equals(selectedTriggerType());
        selectSequenceBtn.enabled = hasRule;
        toggleEnabledBtn.enabled = hasRule;
        toggleBackgroundBtn.enabled = hasRule;
        packetDirectionBtn.enabled = packetTrigger;
        importGuiBtn.enabled = guiTrigger;
        openGuiInspectorBtn.enabled = guiTrigger;
        importPacketBtn.enabled = packetTrigger;
        idleExcludePathBtn.enabled = idleTrigger;
        idleIgnoreDamageBtn.enabled = idleTrigger;
        if (!idleTrigger) {
            idleExcludePathBtn.visible = false;
            idleIgnoreDamageBtn.visible = false;
        }
    }

    private void flushEditorToSelected() {
        if (selectedRuleIndex < 0 || selectedRuleIndex >= rules.size()) {
            return;
        }
        LegacySequenceTriggerManager.RuleEditModel rule = rules.get(selectedRuleIndex);
        rule.name = safe(nameField.getText()).trim();
        rule.sequenceName = safe(sequenceField.getText()).trim();
        rule.contains = safe(containsField.getText()).trim();
        rule.note = safe(noteField.getText()).trim();
        rule.enabled = editingEnabled;
        rule.backgroundExecution = editingBackground;
        rule.cooldownMs = parseInt(cooldownField.getText(), 1000, 0);

        JsonObject params = new JsonObject();
        String type = selectedTriggerType();
        if (LegacySequenceTriggerManager.TRIGGER_GUI_OPEN.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_GUI_CLOSE.equals(type)) {
            putTrimmed(params, "guiTitle", guiTitleField.getText());
            putTrimmed(params, "guiClass", guiClassField.getText());
        } else if (LegacySequenceTriggerManager.TRIGGER_CHAT.equals(type)) {
            putTrimmed(params, "chatText", chatTextField.getText());
        } else if (LegacySequenceTriggerManager.TRIGGER_PACKET.equals(type)) {
            putTrimmed(params, "packetText", packetTextField.getText());
            putTrimmed(params, "channel", packetChannelField.getText());
            putTrimmed(params, "direction", editingPacketDirection);
        } else if (LegacySequenceTriggerManager.TRIGGER_TITLE.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_ACTIONBAR.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_SCOREBOARD_CHANGED.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_BOSSBAR.equals(type)) {
            putTrimmed(params, "text", eventTextField.getText());
        } else if (LegacySequenceTriggerManager.TRIGGER_KEY_INPUT.equals(type)) {
            putTrimmed(params, "keyName", keyNameField.getText());
        } else if (LegacySequenceTriggerManager.TRIGGER_PLAYER_IDLE.equals(type)) {
            params.addProperty("idleMs", parseInt(idleDurationMsField.getText(), 1000, 0));
            params.addProperty("excludePathTracking", editingIdleExcludePath);
            params.addProperty("ignoreDamageReset", editingIdleIgnoreDamage);
        } else if (LegacySequenceTriggerManager.TRIGGER_TIMER.equals(type)) {
            params.addProperty("intervalSeconds", parseInt(timerIntervalField.getText(), 1, 1));
        } else if (LegacySequenceTriggerManager.TRIGGER_HP_LOW.equals(type)) {
            params.addProperty("hpThreshold", parseDouble(hpThresholdField.getText(), 6.0D, 0.0D));
        } else if (LegacySequenceTriggerManager.TRIGGER_PLAYER_HURT.equals(type)) {
            putTrimmed(params, "damageSource", damageSourceField.getText());
            params.addProperty("minDamage", parseDouble(damageMinField.getText(), 0.0D, 0.0D));
        } else if (LegacySequenceTriggerManager.TRIGGER_WORLD_CHANGED.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_AREA_CHANGED.equals(type)) {
            putTrimmed(params, "fromText", areaFromField.getText());
            putTrimmed(params, "toText", areaToField.getText());
        } else if (LegacySequenceTriggerManager.TRIGGER_INVENTORY_CHANGED.equals(type)) {
            putTrimmed(params, "inventoryText", inventoryTextField.getText());
        } else if (LegacySequenceTriggerManager.TRIGGER_INVENTORY_FULL.equals(type)) {
            params.addProperty("minFilledSlots", parseInt(inventoryFullSlotsField.getText(), 0, 0));
        } else if (LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP.equals(type)) {
            putTrimmed(params, "itemText", itemPickupTextField.getText());
            params.addProperty("minCount", parseInt(itemPickupCountField.getText(), 1, 0));
        } else if (LegacySequenceTriggerManager.TRIGGER_ENTITY_NEARBY.equals(type)) {
            putTrimmed(params, "entityText", entityTextField.getText());
            params.addProperty("minCount", parseInt(entityMinCountField.getText(), 1, 0));
        } else if (LegacySequenceTriggerManager.TRIGGER_ATTACK_ENTITY.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_TARGET_KILL.equals(type)) {
            putTrimmed(params, "entityText", entityTextField.getText());
        }
        rule.params = params;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_ADD:
            flushEditorToSelected();
            LegacySequenceTriggerManager.RuleEditModel created = new LegacySequenceTriggerManager.RuleEditModel();
            created.name = "trigger_rule_" + (rules.size() + 1);
            rules.add(created);
            selectedRuleIndex = rules.size() - 1;
            recalcLayout();
            loadFromRule(created);
            statusMessage = "§a已新增触发器规则";
            break;
        case BTN_DELETE:
            if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
                rules.remove(selectedRuleIndex);
                selectedRuleIndex = rules.isEmpty() ? -1 : Math.min(selectedRuleIndex, rules.size() - 1);
                recalcLayout();
                if (selectedRuleIndex >= 0) {
                    loadFromRule(rules.get(selectedRuleIndex));
                } else {
                    clearEditor();
                }
                statusMessage = "§e已删除触发器规则";
            }
            break;
        case BTN_RELOAD:
            reloadWorkingCopy();
            recalcLayout();
            if (selectedRuleIndex >= 0) {
                loadFromRule(rules.get(selectedRuleIndex));
            } else {
                clearEditor();
            }
            statusMessage = "§7已重载触发器规则";
            break;
        case BTN_VALIDATE:
            flushEditorToSelected();
            String validateMessage = validateRules();
            statusMessage = validateMessage == null ? "§a触发器规则校验通过" : "§c校验失败: " + validateMessage;
            break;
        case BTN_SAVE:
            flushEditorToSelected();
            String validationError = validateRules();
            if (validationError != null) {
                statusMessage = "§c保存失败: " + validationError;
                break;
            }
            LegacySequenceTriggerManager.saveRuleModels(rules);
            statusMessage = "§a触发器规则已保存";
            break;
        case BTN_BACK:
            mc.setScreen(parent);
            return;
        case BTN_SELECT_SEQUENCE:
            flushEditorToSelected();
            mc.setScreen(new GuiSequenceSelector(this, seq -> {
                String selectedSequence = seq == null ? "" : seq;
                sequenceField.setText(selectedSequence);
                if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
                    rules.get(selectedRuleIndex).sequenceName = selectedSequence;
                }
                mc.setScreen(this);
            }));
            return;
        case BTN_TOGGLE_ENABLED:
            editingEnabled = !editingEnabled;
            break;
        case BTN_TOGGLE_BACKGROUND:
            editingBackground = !editingBackground;
            break;
        case BTN_PACKET_DIRECTION:
            if (editingPacketDirection.isEmpty()) {
                editingPacketDirection = "inbound";
            } else if ("inbound".equalsIgnoreCase(editingPacketDirection)) {
                editingPacketDirection = "outbound";
            } else {
                editingPacketDirection = "";
            }
            break;
        case BTN_IMPORT_GUI:
            fillFromLatestGuiSnapshot();
            break;
        case BTN_OPEN_GUI_INSPECTOR:
            flushEditorToSelected();
            mc.setScreen(new GuiGuiInspectorManager(this));
            return;
        case BTN_IMPORT_PACKET:
            fillFromLatestPacketText();
            break;
        case BTN_IDLE_EXCLUDE_PATH:
            editingIdleExcludePath = !editingIdleExcludePath;
            break;
        case BTN_IDLE_IGNORE_DAMAGE:
            editingIdleIgnoreDamage = !editingIdleIgnoreDamage;
            break;
        default:
            break;
        }
        refreshButtons();
    }

    private String validateRules() {
        for (LegacySequenceTriggerManager.RuleEditModel rule : rules) {
            if (rule == null) {
                continue;
            }
            if (safe(rule.sequenceName).trim().isEmpty()) {
                return "存在未填写序列名的规则";
            }
            if (PathSequenceManager.getSequence(rule.sequenceName) == null) {
                return "序列不存在: " + rule.sequenceName;
            }
        }
        return null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        tooltipRegions.clear();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "路径序列触发器规则", this.fontRenderer);
        drawPane(listCollapsed ? "规则" : "规则列表", listX, listY, listW, listH);
        drawPane(libraryCollapsed ? "事件" : "事件库", libraryX, libraryY, libraryW, libraryH);
        drawPane("事件编辑器", editorX, editorY, editorW, editorH);
        drawCollapseButton(listX, listY, listW, listCollapsed);
        drawCollapseButton(libraryX, libraryY, libraryW, libraryCollapsed);
        drawRuleList(mouseX, mouseY);
        drawLibrary(mouseX, mouseY);
        drawEditor(mouseX, mouseY);
        drawString(fontRenderer, statusMessage, panelX + 12, panelY + panelH - 38, 0xFFB8C7D9);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawHoverTooltips(mouseX, mouseY);
    }

    private void drawRuleList(int mouseX, int mouseY) {
        if (listCollapsed) {
            return;
        }
        int visible = Math.max(1, (listH - 30) / (RULE_CARD_H + RULE_CARD_GAP));
        maxListScroll = Math.max(0, rules.size() - visible);
        listScroll = Math.max(0, Math.min(listScroll, maxListScroll));
        int rowY = listY + 24;
        for (int i = 0; i < visible; i++) {
            int idx = listScroll + i;
            if (idx >= rules.size()) {
                break;
            }
            LegacySequenceTriggerManager.RuleEditModel rule = rules.get(idx);
            boolean hovered = isHoverRegion(mouseX, mouseY, listX + 6, rowY, listW - 14, RULE_CARD_H);
            GuiTheme.UiState state = idx == selectedRuleIndex ? GuiTheme.UiState.SELECTED
                    : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(listX + 6, rowY, listW - 14, RULE_CARD_H, state);
            drawString(fontRenderer, trim((rule.enabled ? "§a✔ " : "§c✘ ") + displayRuleName(rule), listW - 28),
                    listX + 12, rowY + 6, 0xFFFFFFFF);
            drawString(fontRenderer,
                    trim("§7" + displayTriggerType(rule.triggerType) + " | " + safe(rule.sequenceName), listW - 28),
                    listX + 12, rowY + 20, 0xFF9FB0C4);
            rowY += RULE_CARD_H + RULE_CARD_GAP;
        }

        if (rules.size() > visible) {
            int barY = listY + 24;
            int barH = Math.max(20, listH - 30);
            int thumbH = Math.max(18, (int) ((visible / (float) Math.max(visible, rules.size())) * barH));
            int track = Math.max(1, barH - thumbH);
            int thumbY = barY + (int) ((listScroll / (float) Math.max(1, maxListScroll)) * track);
            GuiTheme.drawScrollbar(listX + listW - 8, barY, 4, barH, thumbY, thumbH);
        }
    }

    private void drawLibrary(int mouseX, int mouseY) {
        if (libraryCollapsed) {
            return;
        }
        int searchX = libraryX + 6;
        int searchY = libraryY + 24;
        int searchW = Math.max(60, libraryW - 14);
        librarySearchField.x = searchX;
        librarySearchField.y = searchY;
        librarySearchField.width = searchW;
        drawThemedTextField(librarySearchField);
        if (safe(librarySearchField.getText()).trim().isEmpty() && !librarySearchField.isFocused()) {
            drawString(fontRenderer, "§7搜索事件...", searchX + 4, searchY + 6, 0xFF808080);
        }
        registerTooltip(searchX, searchY, searchW, librarySearchField.height,
                buildTooltipLines("§e搜索事件", "§7输入关键字过滤事件名称和事件类型。"));

        rebuildVisibleLibraryRows();
        int listStartY = searchY + 24;
        int visible = Math.max(1, (libraryH - 54) / LIB_ROW_H);
        maxLibraryScroll = Math.max(0, visibleLibraryRows.size() - visible);
        libraryScroll = Math.max(0, Math.min(libraryScroll, maxLibraryScroll));
        int rowY = listStartY;
        for (int i = 0; i < visible; i++) {
            int idx = libraryScroll + i;
            if (idx >= visibleLibraryRows.size()) {
                break;
            }
            LegacyTriggerEventItem item = visibleLibraryRows.get(idx);
            if (item.header) {
                drawString(fontRenderer, "§b" + item.label, libraryX + 8, rowY + 5, 0xFFEAF7FF);
                drawRect(libraryX + 8, rowY + LIB_ROW_H - 4, libraryX + libraryW - 12, rowY + LIB_ROW_H - 3,
                        0x553D6F91);
                String arrow = isLibraryGroupCollapsed(item.label) ? "§7[+]" : "§7[-]";
                drawString(fontRenderer, arrow, libraryX + libraryW - 28, rowY + 5, 0xFFEAF7FF);
                registerTooltip(libraryX + 6, rowY, libraryW - 14, LIB_ROW_H - 1,
                        buildTooltipLines("§e" + item.label, "§7左键展开/收起这一组事件。"));
            } else {
                boolean hovered = isHoverRegion(mouseX, mouseY, libraryX + 6, rowY, libraryW - 14, LIB_ROW_H - 1);
                GuiTheme.UiState state = item.type.equals(selectedTriggerType()) ? GuiTheme.UiState.SELECTED
                        : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                GuiTheme.drawButtonFrameSafe(libraryX + 6, rowY, libraryW - 14, LIB_ROW_H - 1, state);
                drawString(fontRenderer, trim("§f" + item.label, libraryW - 28), libraryX + 12, rowY + 4, 0xFFFFFFFF);
                drawString(fontRenderer, trim("§7" + item.type, libraryW - 28), libraryX + 12, rowY + 16, 0xFF9FB0C4);
                registerTooltip(libraryX + 6, rowY, libraryW - 14, LIB_ROW_H - 1,
                        buildTooltipLines("§e" + item.label, "§7事件类型: §f" + item.type, "§7" + item.help, "§7左键切换到该事件。"));
            }
            rowY += LIB_ROW_H;
        }

        if (visibleLibraryRows.size() > visible) {
            int barY = listStartY;
            int barH = Math.max(20, libraryY + libraryH - 6 - listStartY);
            int thumbH = Math.max(18, (int) ((visible / (float) Math.max(visible, visibleLibraryRows.size())) * barH));
            int track = Math.max(1, barH - thumbH);
            int thumbY = barY + (int) ((libraryScroll / (float) Math.max(1, maxLibraryScroll)) * track);
            GuiTheme.drawScrollbar(libraryX + libraryW - 8, barY, 4, barH, thumbY, thumbH);
        }
    }

    private void drawEditor(int mouseX, int mouseY) {
        hideTransientEditorButtons();
        drawEditorTabs(mouseX, mouseY);
        int viewportX = getEditorContentX();
        int viewportY = getEditorContentY();
        int viewportW = getEditorContentWidth();
        int viewportH = getEditorContentHeight();
        GuiTheme.drawInputFrameSafe(viewportX, viewportY, viewportW, viewportH, false, true);

        beginScissor(viewportX + 1, viewportY + 1, viewportW - 2, viewportH - 2);
        int y = viewportY + 4 - editorScroll;
        if (selectedRuleIndex < 0 || selectedRuleIndex >= rules.size()) {
            y = drawInfoBox(y, "暂无规则", "请先点击下方“新增”，然后从中间事件库选择触发类型。", 0xFFB8C7D9);
        } else if (activeEditorTab == TAB_BASE) {
            y = drawInfoBox(y, "基础设置 · " + displayTriggerType(selectedTriggerType()),
                    "这里配置规则名称、目标序列、开关、冷却和通用过滤。事件类型请在中间事件库切换。", 0xFFB8C7D9);
            drawField("规则名", nameField, y,
                    "§e规则名",
                    "§7只是这条触发器规则的显示名称。",
                    "§7用于在左侧列表区分不同规则，不参与触发判定。");
            y += 24;
            y = drawSequenceRow(y);
            y = drawToggleRow(y);
            drawField("冷却(ms)", cooldownField, y,
                    "§e冷却(ms)",
                    "§7同一条规则两次成功触发之间的最短间隔，单位毫秒。",
                    "§70 表示不额外限流。");
            y += 24;
            drawField("通用关键字过滤", containsField, y,
                    "§e通用关键字过滤",
                    "§7这是所有事件共用的一层模糊过滤。",
                    "§7程序会把当前事件数据拼成一段文本，只要其中包含你填写的关键字才允许触发。",
                    "§7留空表示不过滤。适合先做粗筛，再配合事件专属参数细筛。");
            y += 24;
            drawField("备注", noteField, y,
                    "§e备注",
                    "§7仅用于记录说明，不参与触发和执行。");
            y += 24;
        } else if (activeEditorTab == TAB_EVENT) {
            y = drawSpecificFields(y);
        } else {
            String latestGuiSummary = getLatestGuiSnapshotSummary();
            String latestPacketSummary = getLatestPacketTextSummary();
            y = drawInfoBox(y, "调试数据源", "最近 GUI: " + latestGuiSummary + "\n最近包文本: " + latestPacketSummary, 0xFFB8C7D9);
            y = drawTextPreviewBox(y, "当前事件说明", currentHelp(), 0xFFB8C7D9);
            Object trigger = ScopedRuntimeVariables.getGlobalValue("trigger");
            String preview = trigger instanceof Map ? trigger.toString() : String.valueOf(trigger);
            y = drawTextPreviewBox(y, "最近事件上下文(global.trigger)", safe(preview), 0xFF9FB0C4);
        }
        endScissor();

        updateEditorScrollMetrics(y + editorScroll + 4);
        drawEditorScrollbar();
    }

    private int drawSpecificFields(int y) {
        String type = selectedTriggerType();
        String summary = "当前触发类型: " + displayTriggerType(type) + " (" + type + ")";
        int nextY = drawInfoBox(y, "事件参数", summary + "\n" + currentHelp(), 0xFFB8C7D9);
        if (LegacySequenceTriggerManager.TRIGGER_GUI_OPEN.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_GUI_CLOSE.equals(type)) {
            drawField("界面标题包含", guiTitleField, nextY,
                    "§e界面标题包含",
                    "§7匹配 GUI 标题文本。",
                    "§7例如只想在某个箱子或菜单标题打开时触发。");
            nextY += 24;
            drawField("GUI类名包含", guiClassField, nextY,
                    "§eGUI类名包含",
                    "§7匹配界面类名。",
                    "§7适合标题不稳定、但界面类固定的情况。");
            nextY += 24;
            nextY = drawDualEditorButtons(nextY, importGuiBtn, openGuiInspectorBtn);
        } else if (LegacySequenceTriggerManager.TRIGGER_CHAT.equals(type)) {
            drawField("聊天消息包含", chatTextField, nextY,
                    "§e聊天消息包含",
                    "§7聊天框里只要显示出包含该文本的消息就会触发。",
                    "§7包括玩家消息、系统消息、服务器提示；留空表示任意显示消息都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_PACKET.equals(type)) {
            drawField("数据包文本包含", packetTextField, nextY,
                    "§e数据包文本包含",
                    "§7在抓到的数据包文本、解码文本、包类名里做包含匹配。",
                    "§7留空表示不按包文本过滤。");
            nextY += 24;
            drawField("频道包含", packetChannelField, nextY,
                    "§e频道包含",
                    "§7按网络频道名进一步过滤数据包。",
                    "§7适用于 FML 自定义频道或特定业务频道。");
            nextY += 24;
            drawLabel("方向筛选", nextY);
            registerTooltip(getLabelX(), nextY, getLabelWidth(), 18,
                    buildTooltipLines("§e方向筛选", "§7限制只匹配入站、出站，或全部数据包。"));
            packetDirectionBtn.x = fieldX;
            packetDirectionBtn.y = nextY - 1;
            packetDirectionBtn.width = fieldW;
            packetDirectionBtn.displayString = fieldW < 128 ? "方向: " + packetDirectionLabel(editingPacketDirection)
                    : "方向筛选: " + packetDirectionLabel(editingPacketDirection);
            packetDirectionBtn.visible = isFullyVisibleInEditorContent(packetDirectionBtn.y, packetDirectionBtn.height);
            registerButtonTooltip(packetDirectionBtn,
                    "§e方向筛选",
                    "§7点击切换：全部 → 入站 → 出站。",
                    "§7当你只关心服务端下发或客户端上报时很有用。");
            nextY += 24;
            importPacketBtn.x = fieldX;
            importPacketBtn.y = nextY - 1;
            importPacketBtn.width = fieldW;
            importPacketBtn.displayString = fieldW < 128 ? "填最近包" : "填入最近包";
            importPacketBtn.visible = isFullyVisibleInEditorContent(importPacketBtn.y, importPacketBtn.height);
            registerButtonTooltip(importPacketBtn,
                    "§e填入最近包",
                    "§7把最近一次抓到的数据包文本回填到“数据包文本包含”里。",
                    "§7适合先抓包，再快速生成触发条件。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_TITLE.equals(type)) {
            drawField("标题文本包含", eventTextField, nextY,
                    "§e标题文本包含",
                    "§7匹配大标题文本。",
                    "§7留空表示任意标题都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_ACTIONBAR.equals(type)) {
            drawField("ActionBar文本包含", eventTextField, nextY,
                    "§eActionBar文本包含",
                    "§7匹配 ActionBar 文本。",
                    "§7留空表示任意 ActionBar 都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_SCOREBOARD_CHANGED.equals(type)) {
            drawField("记分板文本包含", eventTextField, nextY,
                    "§e记分板文本包含",
                    "§7匹配侧边栏记分板标题和行文本。",
                    "§7留空表示任意记分板变化都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_BOSSBAR.equals(type)) {
            drawField("BossBar文本包含", eventTextField, nextY,
                    "§eBossBar文本包含",
                    "§7匹配 BossBar 标题文本。",
                    "§7留空表示任意 BossBar 变化都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_KEY_INPUT.equals(type)) {
            drawField("按键名称包含", keyNameField, nextY,
                    "§e按键名称包含",
                    "§7匹配按下的按键名称，例如 F、R、NUMPAD1。",
                    "§7留空表示任意按键按下都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_PLAYER_IDLE.equals(type)) {
            drawField("静止时长(毫秒)", idleDurationMsField, nextY,
                    "§e静止时长(毫秒)",
                    "§7当角色持续站立不动达到这个时长后触发。",
                    "§7填 1000 表示连续静止 1 秒。",
                    "§7如果想过滤掉路径执行造成的短暂停顿，可开启下面的“排除正在执行路径”。");
            nextY += 24;
            drawLabel("路径执行过滤", nextY);
            registerTooltip(getLabelX(), nextY, getLabelWidth(), 18,
                    buildTooltipLines("§e路径执行过滤", "§7控制站立不动检测是否排除路径正在执行时的停顿。"));
            idleExcludePathBtn.x = fieldX;
            idleExcludePathBtn.y = nextY - 1;
            idleExcludePathBtn.width = fieldW;
            idleExcludePathBtn.displayString = buildIdleExcludePathButtonLabel(idleExcludePathBtn.width);
            idleExcludePathBtn.visible = isFullyVisibleInEditorContent(idleExcludePathBtn.y,
                    idleExcludePathBtn.height);
            registerButtonTooltip(idleExcludePathBtn,
                    "§e排除正在执行路径",
                    "§7开启后，路径执行过程中的静止不会累计到站立不动触发器。");
            nextY += 24;
            drawLabel("受伤重置规则", nextY);
            registerTooltip(getLabelX(), nextY, getLabelWidth(), 18,
                    buildTooltipLines("§e受伤重置规则", "§7控制受击击退时是否把静止计时清零。"));
            idleIgnoreDamageBtn.x = fieldX;
            idleIgnoreDamageBtn.y = nextY - 1;
            idleIgnoreDamageBtn.width = fieldW;
            idleIgnoreDamageBtn.displayString = buildIdleIgnoreDamageButtonLabel(idleIgnoreDamageBtn.width);
            idleIgnoreDamageBtn.visible = isFullyVisibleInEditorContent(idleIgnoreDamageBtn.y,
                    idleIgnoreDamageBtn.height);
            registerButtonTooltip(idleIgnoreDamageBtn,
                    "§e受击不重置空闲",
                    "§7开启后，受击后的短暂击退不会把站立不动计时立刻清零。",
                    "§7适合把挂机防打断、站桩监控这类规则调得更稳。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_TIMER.equals(type)) {
            drawField("间隔秒数", timerIntervalField, nextY,
                    "§e间隔秒数",
                    "§7每隔多少秒触发一次。",
                    "§71 表示每秒检查一次。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_HP_LOW.equals(type)) {
            drawField("阈值血量", hpThresholdField, nextY,
                    "§e阈值血量",
                    "§7当当前血量小于等于这个值时触发。",
                    "§7例如 6 代表 3 颗心。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_PLAYER_HURT.equals(type)) {
            drawField("伤害来源包含", damageSourceField, nextY,
                    "§e伤害来源包含",
                    "§7匹配伤害来源类型，例如 player、mob、magic、fall。",
                    "§7留空表示不按伤害来源过滤。");
            nextY += 24;
            drawField("最小伤害值", damageMinField, nextY,
                    "§e最小伤害值",
                    "§7本次受到的伤害至少达到该值才触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_WORLD_CHANGED.equals(type)) {
            drawField("离开世界包含", areaFromField, nextY,
                    "§e离开世界包含",
                    "§7匹配切换前的世界文本，例如 dim:0。");
            nextY += 24;
            drawField("进入世界包含", areaToField, nextY,
                    "§e进入世界包含",
                    "§7匹配切换后的世界文本，例如 dim:-1。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_AREA_CHANGED.equals(type)) {
            drawField("离开区域包含", areaFromField, nextY,
                    "§e离开区域包含",
                    "§7只在离开的区域文本包含该关键字时触发。",
                    "§7留空表示不限制来源区域。");
            nextY += 24;
            drawField("进入区域包含", areaToField, nextY,
                    "§e进入区域包含",
                    "§7只在进入的区域文本包含该关键字时触发。",
                    "§7留空表示不限制目标区域。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_INVENTORY_CHANGED.equals(type)) {
            drawField("背包变化文本包含", inventoryTextField, nextY,
                    "§e背包变化文本包含",
                    "§7在背包变化后的签名文本里做包含匹配。",
                    "§7留空表示任意背包变化都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_INVENTORY_FULL.equals(type)) {
            drawField("最少占满槽位", inventoryFullSlotsField, nextY,
                    "§e最少占满槽位",
                    "§7主背包至少占满多少格时触发。",
                    "§7可用来做背包接近满时的收尾动作。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP.equals(type)) {
            drawField("物品文本包含", itemPickupTextField, nextY,
                    "§e物品文本包含",
                    "§7按拾取到的物品名称或注册名做包含匹配。",
                    "§7留空表示任意拾取物品都可触发。");
            nextY += 24;
            drawField("最少拾取数量", itemPickupCountField, nextY,
                    "§e最少拾取数量",
                    "§7本次拾取数量达到该值才触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_ENTITY_NEARBY.equals(type)) {
            drawField("实体文本包含", entityTextField, nextY,
                    "§e实体文本包含",
                    "§7在附近实体列表文本中做包含匹配。",
                    "§7留空表示不按实体名称过滤。");
            nextY += 24;
            drawField("最少实体数量", entityMinCountField, nextY,
                    "§e最少实体数量",
                    "§7附近实体数量达到该值才触发。",
                    "§7可配合实体文本一起筛选。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_ATTACK_ENTITY.equals(type)) {
            drawField("目标实体文本包含", entityTextField, nextY,
                    "§e目标实体文本包含",
                    "§7匹配你攻击时的目标实体名称或类型文本。",
                    "§7留空表示任意攻击实体都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_TARGET_KILL.equals(type)) {
            drawField("被击杀实体文本包含", entityTextField, nextY,
                    "§e被击杀实体文本包含",
                    "§7匹配被你击杀的实体名称或类型文本。",
                    "§7留空表示任意击杀都可触发。");
            nextY += 24;
        } else if (LegacySequenceTriggerManager.TRIGGER_SERVER_CONNECT.equals(type)) {
            drawInfoBox(nextY, "连接服务器",
                    "当客户端成功连接到服务器时触发一次。可在基础设置页配置冷却和目标序列。", 0xFFB8C7D9);
        } else if (LegacySequenceTriggerManager.TRIGGER_SERVER_DISCONNECT.equals(type)) {
            drawInfoBox(nextY, "断开服务器",
                    "当客户端与服务器断开连接时触发一次。可在基础设置页配置冷却和目标序列。", 0xFFB8C7D9);
        } else {
            drawInfoBox(nextY, "无需额外参数", "该事件只要触发即可执行，你仍然可以切到“基础设置”配置冷却、目标序列和关键字过滤。", 0xFFB8C7D9);
        }
        return nextY;
    }

    private int drawSequenceRow(int y) {
        int buttonW = Math.max(96, Math.min(130, fieldW / 3));
        int gap = 6;
        boolean stacked = fieldW < buttonW + gap + 110;
        if (stacked) {
            drawField("目标序列", sequenceField, y, fieldW,
                    "§e目标序列",
                    "§7触发成功后实际要执行的路径序列名称。",
                    "§7必须选择一个已经存在的序列。");
            y += 24;
            selectSequenceBtn.x = fieldX;
            selectSequenceBtn.y = y - 1;
            selectSequenceBtn.width = fieldW;
            selectSequenceBtn.displayString = fieldW < 108 ? "选择" : "选择序列";
            selectSequenceBtn.visible = isFullyVisibleInEditorContent(selectSequenceBtn.y, selectSequenceBtn.height);
            registerButtonTooltip(selectSequenceBtn,
                    "§e选择序列",
                    "§7打开序列选择器，从已有路径序列里选一个作为触发后的执行目标。");
            return y + 24;
        }

        int inputW = Math.max(1, fieldW - buttonW - gap);
        drawField("目标序列", sequenceField, y, inputW,
                "§e目标序列",
                "§7触发成功后实际要执行的路径序列名称。",
                "§7必须选择一个已经存在的序列。");
        selectSequenceBtn.x = fieldX + inputW + gap;
        selectSequenceBtn.y = y - 1;
        selectSequenceBtn.width = buttonW;
        selectSequenceBtn.displayString = buttonW < 108 ? "选择" : "选择序列";
        selectSequenceBtn.visible = isFullyVisibleInEditorContent(selectSequenceBtn.y, selectSequenceBtn.height);
        registerButtonTooltip(selectSequenceBtn,
                "§e选择序列",
                "§7打开序列选择器，从已有路径序列里选一个作为触发后的执行目标。");
        return y + 24;
    }

    private int drawToggleRow(int y) {
        drawLabel("启用 / 后台", y);
        registerTooltip(getLabelX(), y, getLabelWidth(), 18,
                buildTooltipLines("§e启用 / 后台", "§7这里控制规则是否生效，以及触发后是否后台并行执行。"));
        int gap = 6;
        boolean stacked = fieldW < 80 * 2 + gap;
        if (stacked) {
            toggleEnabledBtn.x = fieldX;
            toggleEnabledBtn.y = y - 1;
            toggleEnabledBtn.width = fieldW;
            toggleEnabledBtn.displayString = buildEnabledButtonLabel(toggleEnabledBtn.width);
            toggleEnabledBtn.visible = isFullyVisibleInEditorContent(toggleEnabledBtn.y, toggleEnabledBtn.height);
            registerButtonTooltip(toggleEnabledBtn,
                    "§e启用规则",
                    "§7关闭后这条规则不会参与触发判断。");

            int secondY = y + 24;
            toggleBackgroundBtn.x = fieldX;
            toggleBackgroundBtn.y = secondY - 1;
            toggleBackgroundBtn.width = fieldW;
            toggleBackgroundBtn.displayString = buildBackgroundButtonLabel(toggleBackgroundBtn.width);
            toggleBackgroundBtn.visible = isFullyVisibleInEditorContent(toggleBackgroundBtn.y,
                    toggleBackgroundBtn.height);
            registerButtonTooltip(toggleBackgroundBtn,
                    "§e后台执行",
                    "§7开启后不等待目标序列结束，当前流程会立即继续。",
                    "§7关闭时会前台执行并等待目标序列完成。");
            return secondY + 24;
        }

        toggleEnabledBtn.x = fieldX;
        toggleEnabledBtn.y = y - 1;
        toggleEnabledBtn.width = (fieldW - gap) / 2;
        toggleEnabledBtn.displayString = buildEnabledButtonLabel(toggleEnabledBtn.width);
        toggleEnabledBtn.visible = isFullyVisibleInEditorContent(toggleEnabledBtn.y, toggleEnabledBtn.height);
        registerButtonTooltip(toggleEnabledBtn,
                "§e启用规则",
                "§7关闭后这条规则不会参与触发判断。");
        toggleBackgroundBtn.x = fieldX + toggleEnabledBtn.width + gap;
        toggleBackgroundBtn.y = y - 1;
        toggleBackgroundBtn.width = Math.max(1, fieldW - toggleEnabledBtn.width - gap);
        toggleBackgroundBtn.displayString = buildBackgroundButtonLabel(toggleBackgroundBtn.width);
        toggleBackgroundBtn.visible = isFullyVisibleInEditorContent(toggleBackgroundBtn.y, toggleBackgroundBtn.height);
        registerButtonTooltip(toggleBackgroundBtn,
                "§e后台执行",
                "§7开启后不等待目标序列结束，当前流程会立即继续。",
                "§7关闭时会前台执行并等待目标序列完成。");
        return y + 24;
    }

    private int drawDualEditorButtons(int y, GuiButton left, GuiButton right) {
        int gap = 6;
        boolean stacked = fieldW < 80 * 2 + gap;
        left.x = fieldX;
        left.y = y - 1;
        if (stacked) {
            left.width = fieldW;
            right.x = fieldX;
            right.y = y + 23;
            right.width = fieldW;
        } else {
            int leftW = (fieldW - gap) / 2;
            left.width = leftW;
            right.x = fieldX + leftW + gap;
            right.y = y - 1;
            right.width = Math.max(1, fieldW - leftW - gap);
        }
        left.visible = isFullyVisibleInEditorContent(left.y, left.height);
        right.visible = isFullyVisibleInEditorContent(right.y, right.height);
        if (left == importGuiBtn) {
            left.displayString = left.width < 108 ? "填GUI" : "填入最近GUI";
            registerButtonTooltip(left,
                    "§e填入最近GUI",
                    "§7把最近一次捕获到的 GUI 标题和类名回填到条件里。");
        }
        if (right == openGuiInspectorBtn) {
            right.displayString = right.width < 108 ? "识别器" : "打开识别器";
            registerButtonTooltip(right,
                    "§e打开识别器",
                    "§7打开 GUI 识别管理器，先捕获界面信息，再回填到规则。");
        }
        return y + (stacked ? 48 : 24);
    }

    private int drawInfoBox(int y, String title, String body, int textColor) {
        return drawTextPreviewBox(y, title, body, textColor);
    }

    private int drawTextPreviewBox(int y, String title, String body, int textColor) {
        int boxX = getEditorContentX();
        int boxW = getEditorContentWidth();
        int wrapW = Math.max(20, boxW - 12);
        List<String> lines = fontRenderer.listFormattedStringToWidth(safe(body), wrapW);
        int boxH = Math.max(38, 18 + lines.size() * 10 + 8);
        GuiTheme.drawButtonFrameSafe(boxX, y, boxW, boxH, GuiTheme.UiState.NORMAL);
        drawString(fontRenderer, "§b" + title, boxX + 6, y + 6, 0xFFEAF7FF);
        int textY = y + 18;
        for (String line : lines) {
            drawString(fontRenderer, line, boxX + 6, textY, textColor);
            textY += 10;
        }
        return y + boxH + 10;
    }

    private void drawEditorTabs(int mouseX, int mouseY) {
        String[] labels = getEditorTabLabels();
        EditorTabLayout layout = buildEditorTabLayout(labels);
        GuiTheme.drawButtonFrameSafe(layout.barX, layout.barY, layout.barW, layout.barH, GuiTheme.UiState.NORMAL);

        boolean leftEnabled = activeEditorTab > 0;
        boolean rightEnabled = activeEditorTab < labels.length - 1;
        boolean leftHovered = isHoverRegion(mouseX, mouseY, layout.leftArrowX, layout.arrowY, layout.arrowW,
                layout.arrowH);
        boolean rightHovered = isHoverRegion(mouseX, mouseY, layout.rightArrowX, layout.arrowY, layout.arrowW,
                layout.arrowH);
        GuiTheme.drawButtonFrameSafe(layout.leftArrowX, layout.arrowY, layout.arrowW, layout.arrowH,
                leftEnabled && leftHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        GuiTheme.drawButtonFrameSafe(layout.rightArrowX, layout.arrowY, layout.arrowW, layout.arrowH,
                rightEnabled && rightHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawCenteredString(fontRenderer, "<", layout.leftArrowX + layout.arrowW / 2, layout.arrowY + 5,
                leftEnabled ? 0xFFFFFFFF : 0x668FA3B8);
        drawCenteredString(fontRenderer, ">", layout.rightArrowX + layout.arrowW / 2, layout.arrowY + 5,
                rightEnabled ? 0xFFFFFFFF : 0x668FA3B8);
        registerTooltip(layout.leftArrowX, layout.arrowY, layout.arrowW, layout.arrowH,
                buildTooltipLines("§e上一标签", "§7切换到左侧标签页。"));
        registerTooltip(layout.rightArrowX, layout.arrowY, layout.arrowW, layout.arrowH,
                buildTooltipLines("§e下一标签", "§7切换到右侧标签页。"));

        beginScissor(layout.tabsX, layout.barY, layout.tabsW, layout.barH);
        for (int i = 0; i < layout.visibleCount; i++) {
            int actualIndex = layout.startIndex + i;
            if (actualIndex >= labels.length) {
                break;
            }
            int tabX = layout.tabsX + i * (layout.tabW + TAB_BAR_GAP);
            boolean hovered = isHoverRegion(mouseX, mouseY, tabX, layout.barY, layout.tabW, layout.barH);
            GuiTheme.UiState state = actualIndex == activeEditorTab ? GuiTheme.UiState.SELECTED
                    : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(tabX, layout.barY, layout.tabW, layout.barH, state);
            drawCenteredString(fontRenderer, trim(labels[actualIndex], Math.max(18, layout.tabW - 8)),
                    tabX + layout.tabW / 2, layout.barY + 6, 0xFFFFFFFF);
            registerTooltip(tabX, layout.barY, layout.tabW, layout.barH, editorTabTooltip(actualIndex));
        }
        endScissor();
    }

    private void drawField(String label, GuiTextField field, int y) {
        drawField(label, field, y, fieldW, (String[]) null);
    }

    private void drawField(String label, GuiTextField field, int y, int width) {
        drawField(label, field, y, width, (String[]) null);
    }

    private void drawField(String label, GuiTextField field, int y, String... tooltipLines) {
        drawField(label, field, y, fieldW, tooltipLines);
    }

    private void drawField(String label, GuiTextField field, int y, int width, String... tooltipLines) {
        field.x = fieldX;
        field.y = y;
        field.width = Math.max(1, width);
        if (isVisibleInEditorContent(field.y, field.height)) {
            drawLabel(label, y);
            drawThemedTextField(field);
            if (tooltipLines != null && tooltipLines.length > 0) {
                List<String> builtLines = buildTooltipLines(tooltipLines);
                registerTooltip(getLabelX(), y, getLabelWidth(), 18, builtLines);
                registerTooltip(field.x, field.y, field.width, field.height, builtLines);
            }
        }
    }

    private void drawLabel(String label, int y) {
        drawString(fontRenderer, trim(label, getLabelWidth()), getLabelX(), y + 5, 0xFFFFFFFF);
    }

    private int getLabelX() {
        return editorX + 10;
    }

    private int getLabelWidth() {
        return Math.max(20, fieldX - getLabelX() - 8);
    }

    private void drawPane(String title, int x, int y, int w, int h) {
        GuiTheme.drawPanelSegment(x, y, w, h, panelX, panelY, panelW, panelH);
        drawString(fontRenderer, "§b" + title, x + 8, y + 8, 0xFFEAF7FF);
    }

    private int getEditorContentX() {
        return editorX + 8;
    }

    private int getEditorContentY() {
        return editorY + 24 + TAB_BAR_H + 8;
    }

    private int getEditorContentWidth() {
        return Math.max(1, editorW - 16);
    }

    private int getEditorContentHeight() {
        return Math.max(1, editorY + editorH - 8 - getEditorContentY());
    }

    private boolean isVisibleInEditorContent(int y, int height) {
        int top = getEditorContentY();
        int bottom = top + getEditorContentHeight();
        return y + height >= top && y <= bottom;
    }

    private boolean isFullyVisibleInEditorContent(int y, int height) {
        int top = getEditorContentY();
        int bottom = top + getEditorContentHeight();
        return y >= top && y + height <= bottom;
    }

    private boolean isInsideEditorContent(int mouseX, int mouseY) {
        return isHoverRegion(mouseX, mouseY, getEditorContentX(), getEditorContentY(), getEditorContentWidth(),
                getEditorContentHeight());
    }

    private boolean isInsideLibraryCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, libraryX, libraryY, libraryW);
    }

    private void updateEditorScrollMetrics(int contentBottomY) {
        int visibleBottom = getEditorContentY() + getEditorContentHeight();
        maxEditorScroll = Math.max(0, contentBottomY - visibleBottom);
        editorScroll = clamp(editorScroll, 0, maxEditorScroll);
    }

    private void drawEditorScrollbar() {
        if (maxEditorScroll <= 0) {
            return;
        }
        int barX = editorX + editorW - 8;
        int barY = getEditorContentY();
        int barH = getEditorContentHeight();
        int visibleH = barH;
        int contentH = visibleH + maxEditorScroll;
        int thumbH = Math.max(18, (int) ((visibleH / (float) Math.max(visibleH, contentH)) * barH));
        int track = Math.max(1, barH - thumbH);
        int thumbY = barY + (int) ((editorScroll / (float) Math.max(1, maxEditorScroll)) * track);
        GuiTheme.drawScrollbar(barX, barY, 4, barH, thumbY, thumbH);
    }

    private void beginScissor(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0 || this.mc == null) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(this.mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + h)) * scale, w * scale, h * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void registerButtonTooltip(GuiButton button, String... lines) {
        if (button == null || !button.visible || lines == null || lines.length == 0) {
            return;
        }
        registerTooltip(button.x, button.y, button.width, button.height, buildTooltipLines(lines));
    }

    private void registerTooltip(int x, int y, int w, int h, List<String> lines) {
        if (w <= 0 || h <= 0 || lines == null || lines.isEmpty()) {
            return;
        }
        tooltipRegions.add(new TooltipRegion(x, y, w, h, lines));
    }

    private List<String> buildTooltipLines(String... lines) {
        List<String> result = new ArrayList<>();
        if (lines == null) {
            return result;
        }
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

    private List<String> editorTabTooltip(int tabIndex) {
        switch (tabIndex) {
            case TAB_BASE:
                return buildTooltipLines("§e基础设置", "§7配置规则名、目标序列、开关、冷却和通用过滤。");
            case TAB_EVENT:
                return buildTooltipLines("§e事件参数", "§7配置当前事件类型专属的触发条件。");
            case TAB_DEBUG:
            default:
                return buildTooltipLines("§e调试预览", "§7查看最近 GUI / 包文本和最近一次触发上下文。");
        }
    }

    private void drawHoverTooltips(int mouseX, int mouseY) {
        for (int i = tooltipRegions.size() - 1; i >= 0; i--) {
            TooltipRegion region = tooltipRegions.get(i);
            if (region == null || region.lines == null || region.lines.isEmpty()) {
                continue;
            }
            if (mouseX >= region.x && mouseX <= region.x + region.w
                    && mouseY >= region.y && mouseY <= region.y + region.h) {
                drawHoveringText(region.lines, mouseX, mouseY);
                return;
            }
        }
    }

    private String buildEnabledButtonLabel(int width) {
        String state = editingEnabled ? "§aON" : "§cOFF";
        return width < 112 ? "启用: " + state : "启用规则: " + state;
    }

    private String buildBackgroundButtonLabel(int width) {
        String state = editingBackground ? "§a是" : "§c否";
        return width < 112 ? "后台: " + state : "后台执行: " + state;
    }

    private String buildIdleExcludePathButtonLabel(int width) {
        String state = editingIdleExcludePath ? "§aON" : "§cOFF";
        return width < 150 ? "排除执行路径: " + state : "排除正在执行路径: " + state;
    }

    private String buildIdleIgnoreDamageButtonLabel(int width) {
        String state = editingIdleIgnoreDamage ? "§aON" : "§cOFF";
        return width < 150 ? "受击不重置: " + state : "受击不重置空闲: " + state;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            if (isInListCollapseButton(mouseX, mouseY)) {
                listCollapsed = !listCollapsed;
                recalcLayout();
                refreshButtons();
                return;
            }
            if (isInsideLibraryCollapseButton(mouseX, mouseY)) {
                libraryCollapsed = !libraryCollapsed;
                recalcLayout();
                refreshButtons();
                return;
            }
            int ruleIndex = clickedRuleIndex(mouseX, mouseY);
            if (ruleIndex >= 0) {
                flushEditorToSelected();
                recalcLayout();
                selectedRuleIndex = ruleIndex;
                loadFromRule(rules.get(selectedRuleIndex));
                clearFieldFocus();
                refreshButtons();
                return;
            }
            int libIndex = clickedLibraryIndex(mouseX, mouseY);
            if (libIndex >= 0) {
                flushEditorToSelected();
                LegacyTriggerEventItem item = visibleLibraryRows.get(libIndex);
                if (item.header) {
                    toggleLibraryGroup(item.label);
                    rebuildVisibleLibraryRows();
                    libraryScroll = clamp(libraryScroll, 0, maxLibraryScroll);
                } else if (selectedRuleIndex >= 0 && selectedRuleIndex < rules.size()) {
                    rules.get(selectedRuleIndex).triggerType = item.type;
                    loadFromRule(rules.get(selectedRuleIndex));
                    activeEditorTab = TAB_EVENT;
                    clearFieldFocus();
                    refreshButtons();
                    statusMessage = "§a已切换触发类型: " + item.label;
                }
                return;
            }
            if (isInEditorTabLeftArrow(mouseX, mouseY)) {
                activeEditorTab = Math.max(TAB_BASE, activeEditorTab - 1);
                clearFieldFocus();
                refreshButtons();
                return;
            }
            if (isInEditorTabRightArrow(mouseX, mouseY)) {
                activeEditorTab = Math.min(TAB_DEBUG, activeEditorTab + 1);
                clearFieldFocus();
                refreshButtons();
                return;
            }
            int tabIndex = clickedEditorTab(mouseX, mouseY);
            if (tabIndex >= 0) {
                activeEditorTab = tabIndex;
                clearFieldFocus();
                refreshButtons();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!libraryCollapsed && librarySearchField != null) {
            librarySearchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (GuiTextField field : activeFields()) {
            if (!isVisibleInEditorContent(field.y, field.height)) {
                continue;
            }
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        if (!libraryCollapsed && librarySearchField != null && librarySearchField.textboxKeyTyped(typedChar, keyCode)) {
            libraryScroll = 0;
            rebuildVisibleLibraryRows();
            return;
        }
        for (GuiTextField field : activeFields()) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (librarySearchField != null) {
            librarySearchField.updateCursorCounter();
        }
        for (GuiTextField field : activeFields()) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (!listCollapsed && isHoverRegion(mouseX, mouseY, listX, listY, listW, listH)) {
            int visible = Math.max(1, (listH - 30) / (RULE_CARD_H + RULE_CARD_GAP));
            maxListScroll = Math.max(0, rules.size() - visible);
            listScroll = clamp(listScroll + (dWheel < 0 ? 1 : -1), 0, maxListScroll);
        } else if (!libraryCollapsed && isHoverRegion(mouseX, mouseY, libraryX, libraryY, libraryW, libraryH)) {
            rebuildVisibleLibraryRows();
            int visible = Math.max(1, (libraryH - 54) / LIB_ROW_H);
            maxLibraryScroll = Math.max(0, visibleLibraryRows.size() - visible);
            libraryScroll = clamp(libraryScroll + (dWheel < 0 ? 1 : -1), 0, maxLibraryScroll);
        } else if (isInsideEditorContent(mouseX, mouseY)) {
            int oldScroll = editorScroll;
            editorScroll = clamp(editorScroll + (dWheel < 0 ? 20 : -20), 0, maxEditorScroll);
            if (oldScroll != editorScroll) {
                clearFieldFocus();
            }
        }
    }

    private int clickedRuleIndex(int mouseX, int mouseY) {
        if (listCollapsed) {
            return -1;
        }
        int visible = Math.max(1, (listH - 30) / (RULE_CARD_H + RULE_CARD_GAP));
        int rowY = listY + 24;
        for (int i = 0; i < visible; i++) {
            int idx = listScroll + i;
            if (idx >= rules.size()) {
                break;
            }
            if (isHoverRegion(mouseX, mouseY, listX + 6, rowY, listW - 14, RULE_CARD_H)) {
                return idx;
            }
            rowY += RULE_CARD_H + RULE_CARD_GAP;
        }
        return -1;
    }

    private int clickedLibraryIndex(int mouseX, int mouseY) {
        if (libraryCollapsed) {
            return -1;
        }
        int visible = Math.max(1, (libraryH - 54) / LIB_ROW_H);
        int rowY = libraryY + 48;
        for (int i = 0; i < visible; i++) {
            int idx = libraryScroll + i;
            if (idx >= visibleLibraryRows.size()) {
                break;
            }
            LegacyTriggerEventItem item = visibleLibraryRows.get(idx);
            if (isHoverRegion(mouseX, mouseY, libraryX + 6, rowY, libraryW - 14, LIB_ROW_H - 1)) {
                return idx;
            }
            rowY += LIB_ROW_H;
        }
        return -1;
    }

    private void rebuildVisibleLibraryRows() {
        visibleLibraryRows.clear();
        String filter = PinyinSearchHelper.normalizeQuery(
                safe(librarySearchField == null ? "" : librarySearchField.getText()));
        LegacyTriggerEventItem currentHeader = null;
        List<LegacyTriggerEventItem> pendingChildren = new ArrayList<>();
        for (LegacyTriggerEventItem item : EVENT_LIBRARY) {
            if (item.header) {
                appendVisibleLibraryGroup(currentHeader, pendingChildren, filter);
                currentHeader = item;
                pendingChildren = new ArrayList<>();
                continue;
            }
            String searchText = safe(item.label) + " " + safe(item.type) + " " + safe(item.help);
            boolean matches = filter.isEmpty() || PinyinSearchHelper.matchesNormalized(searchText, filter);
            if (matches) {
                pendingChildren.add(item);
            }
        }
        appendVisibleLibraryGroup(currentHeader, pendingChildren, filter);
    }

    private void appendVisibleLibraryGroup(LegacyTriggerEventItem header, List<LegacyTriggerEventItem> children, String filter) {
        if (header == null) {
            return;
        }
        if (children == null) {
            children = java.util.Collections.emptyList();
        }
        if (children.isEmpty() && !filter.isEmpty()) {
            return;
        }
        visibleLibraryRows.add(header);
        if (!isLibraryGroupCollapsed(header.label) || !filter.isEmpty()) {
            visibleLibraryRows.addAll(children);
        }
    }

    private boolean isLibraryGroupCollapsed(String label) {
        return label != null && collapsedLibraryGroups.contains(label);
    }

    private void toggleLibraryGroup(String label) {
        if (label == null || label.trim().isEmpty()) {
            return;
        }
        if (collapsedLibraryGroups.contains(label)) {
            collapsedLibraryGroups.remove(label);
        } else {
            collapsedLibraryGroups.add(label);
        }
    }

    private int clickedEditorTab(int mouseX, int mouseY) {
        String[] labels = getEditorTabLabels();
        EditorTabLayout layout = buildEditorTabLayout(labels);
        for (int i = 0; i < layout.visibleCount; i++) {
            int actualIndex = layout.startIndex + i;
            if (actualIndex >= labels.length) {
                break;
            }
            int tabX = layout.tabsX + i * (layout.tabW + TAB_BAR_GAP);
            if (isHoverRegion(mouseX, mouseY, tabX, layout.barY, layout.tabW, layout.barH)) {
                return actualIndex;
            }
        }
        return -1;
    }

    private boolean isInEditorTabLeftArrow(int mouseX, int mouseY) {
        EditorTabLayout layout = buildEditorTabLayout(getEditorTabLabels());
        return activeEditorTab > 0
                && isHoverRegion(mouseX, mouseY, layout.leftArrowX, layout.arrowY, layout.arrowW, layout.arrowH);
    }

    private boolean isInEditorTabRightArrow(int mouseX, int mouseY) {
        String[] labels = getEditorTabLabels();
        EditorTabLayout layout = buildEditorTabLayout(labels);
        return activeEditorTab < labels.length - 1
                && isHoverRegion(mouseX, mouseY, layout.rightArrowX, layout.arrowY, layout.arrowW, layout.arrowH);
    }

    private String[] getEditorTabLabels() {
        return editorW < 520 ? new String[] { "基础", "参数", "预览" } : new String[] { "基础设置", "事件参数", "调试预览" };
    }

    private EditorTabLayout buildEditorTabLayout(String[] labels) {
        EditorTabLayout layout = new EditorTabLayout();
        layout.barX = editorX + 8;
        layout.barY = editorY + 24;
        layout.barW = Math.max(1, editorW - 16);
        layout.barH = TAB_BAR_H;
        layout.arrowW = Math.min(TAB_ARROW_W, Math.max(10, layout.barW / 6));
        layout.arrowW = Math.min(layout.arrowW, Math.max(8, (layout.barW - TAB_BAR_GAP * 2 - 4) / 2));
        layout.arrowY = layout.barY + 1;
        layout.arrowH = Math.max(18, layout.barH - 2);
        layout.leftArrowX = layout.barX + 2;
        layout.rightArrowX = layout.barX + layout.barW - layout.arrowW - 2;
        layout.tabsX = layout.leftArrowX + layout.arrowW + TAB_BAR_GAP;
        layout.tabsW = Math.max(1, layout.rightArrowX - TAB_BAR_GAP - layout.tabsX);
        layout.visibleCount = Math.min(labels.length,
                Math.max(1, (layout.tabsW + TAB_BAR_GAP) / (TAB_MIN_W + TAB_BAR_GAP)));
        while (layout.visibleCount > 1) {
            int candidateW = (layout.tabsW - TAB_BAR_GAP * (layout.visibleCount - 1)) / layout.visibleCount;
            if (candidateW >= TAB_MIN_W) {
                break;
            }
            layout.visibleCount--;
        }
        layout.tabW = Math.max(1, (layout.tabsW - TAB_BAR_GAP * (layout.visibleCount - 1)) / layout.visibleCount);
        int maxStart = Math.max(0, labels.length - layout.visibleCount);
        layout.startIndex = clamp(activeEditorTab - layout.visibleCount / 2, 0, maxStart);
        return layout;
    }

    private List<GuiTextField> activeFields() {
        if (activeEditorTab == TAB_BASE) {
            return Arrays.asList(nameField, sequenceField, cooldownField, containsField, noteField);
        }
        if (activeEditorTab == TAB_EVENT) {
            return eventFieldsForSelectedType();
        }
        return java.util.Collections.emptyList();
    }

    private List<GuiTextField> eventFieldsForSelectedType() {
        String type = selectedTriggerType();
        if (LegacySequenceTriggerManager.TRIGGER_GUI_OPEN.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_GUI_CLOSE.equals(type)) {
            return Arrays.asList(guiTitleField, guiClassField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_CHAT.equals(type)) {
            return Arrays.asList(chatTextField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_PACKET.equals(type)) {
            return Arrays.asList(packetTextField, packetChannelField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_TITLE.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_ACTIONBAR.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_SCOREBOARD_CHANGED.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_BOSSBAR.equals(type)) {
            return Arrays.asList(eventTextField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_KEY_INPUT.equals(type)) {
            return Arrays.asList(keyNameField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_PLAYER_IDLE.equals(type)) {
            return Arrays.asList(idleDurationMsField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_TIMER.equals(type)) {
            return Arrays.asList(timerIntervalField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_HP_LOW.equals(type)) {
            return Arrays.asList(hpThresholdField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_PLAYER_HURT.equals(type)) {
            return Arrays.asList(damageSourceField, damageMinField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_WORLD_CHANGED.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_AREA_CHANGED.equals(type)) {
            return Arrays.asList(areaFromField, areaToField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_INVENTORY_CHANGED.equals(type)) {
            return Arrays.asList(inventoryTextField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_INVENTORY_FULL.equals(type)) {
            return Arrays.asList(inventoryFullSlotsField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP.equals(type)) {
            return Arrays.asList(itemPickupTextField, itemPickupCountField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_ENTITY_NEARBY.equals(type)) {
            return Arrays.asList(entityTextField, entityMinCountField);
        }
        if (LegacySequenceTriggerManager.TRIGGER_ATTACK_ENTITY.equals(type)
                || LegacySequenceTriggerManager.TRIGGER_TARGET_KILL.equals(type)) {
            return Arrays.asList(entityTextField);
        }
        return java.util.Collections.emptyList();
    }

    private void clearFieldFocus() {
        if (librarySearchField != null) {
            librarySearchField.setFocused(false);
        }
        for (GuiTextField field : allFields()) {
            field.setFocused(false);
        }
    }

    private String selectedTriggerType() {
        if (selectedRuleIndex < 0 || selectedRuleIndex >= rules.size()) {
            return LegacySequenceTriggerManager.TRIGGER_GUI_OPEN;
        }
        return safe(rules.get(selectedRuleIndex).triggerType).trim().toLowerCase(Locale.ROOT);
    }

    private String currentHelp() {
        String type = selectedTriggerType();
        for (LegacyTriggerEventItem item : EVENT_LIBRARY) {
            if (!item.header && item.type.equals(type)) {
                return item.help;
            }
        }
        return "当前事件没有额外说明。";
    }

    private String displayTriggerType(String type) {
        for (LegacyTriggerEventItem item : EVENT_LIBRARY) {
            if (!item.header && item.type.equals(type)) {
                return item.label;
            }
        }
        return safe(type);
    }

    private String packetDirectionLabel(String direction) {
        if ("inbound".equalsIgnoreCase(direction)) {
            return "入站";
        }
        if ("outbound".equalsIgnoreCase(direction)) {
            return "出站";
        }
        return "全部";
    }

    private String displayRuleName(LegacySequenceTriggerManager.RuleEditModel rule) {
        String name = safe(rule == null ? "" : rule.name).trim();
        return name.isEmpty() ? "(未命名规则)" : name;
    }

    private int calculateRuleListWidth(int preferredLibraryW) {
        if (listCollapsed) {
            return getCollapsedSectionWidth("规则");
        }
        int widest = fontRenderer == null ? 120 : fontRenderer.getStringWidth("规则列表");
        for (LegacySequenceTriggerManager.RuleEditModel rule : rules) {
            if (rule == null) {
                continue;
            }
            String line1 = (rule.enabled ? "✔ " : "✘ ") + displayRuleName(rule);
            String line2 = displayTriggerType(rule.triggerType) + " | " + safe(rule.sequenceName);
            widest = Math.max(widest, fontRenderer == null ? widest : fontRenderer.getStringWidth(line1));
            widest = Math.max(widest, fontRenderer == null ? widest : fontRenderer.getStringWidth(line2));
        }

        int minWidth = 170;
        int preferred = widest + 30;
        int maxAllowed = Math.max(minWidth, panelW - (10 + preferredLibraryW + 10 + 380 + 20));
        return Math.max(minWidth, Math.min(preferred, maxAllowed));
    }

    private boolean isInListCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, listX, listY, listW);
    }

    private boolean isInCollapseButton(int mouseX, int mouseY, int boxX, int boxY, int boxWidth) {
        int btnSize = 14;
        int btnX = boxX + boxWidth - btnSize - 6;
        int btnY = boxY + 5;
        return mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;
    }

    private void drawCollapseButton(int boxX, int boxY, int boxWidth, boolean collapsed) {
        int btnSize = 14;
        int btnX = boxX + boxWidth - btnSize - 6;
        int btnY = boxY + 5;
        drawRect(btnX, btnY, btnX + btnSize, btnY + btnSize, 0xAA203146);
        drawHorizontalLine(btnX, btnX + btnSize, btnY, 0xFF4FA6D9);
        drawHorizontalLine(btnX, btnX + btnSize, btnY + btnSize, 0xFF3F6A8C);
        drawVerticalLine(btnX, btnY, btnY + btnSize, 0xFF3F6A8C);
        drawVerticalLine(btnX + btnSize, btnY, btnY + btnSize, 0xFF3F6A8C);
        drawString(this.fontRenderer, collapsed ? ">" : "<", btnX + 4, btnY + 3, 0xFFEAF7FF);
    }

    private int getCollapsedSectionWidth(String shortTitle) {
        int len = shortTitle == null ? 0 : Math.min(2, shortTitle.length());
        int textWidth = this.fontRenderer == null ? 16
                : this.fontRenderer.getStringWidth(shortTitle == null ? "" : shortTitle.substring(0, len));
        return Math.max(34, textWidth + 24);
    }

    private void hideEditorButton(GuiButton button) {
        if (button != null) {
            button.visible = false;
            button.x = -2000;
            button.y = -2000;
        }
    }

    private void hideTransientEditorButtons() {
        hideEditorButton(selectSequenceBtn);
        hideEditorButton(toggleEnabledBtn);
        hideEditorButton(toggleBackgroundBtn);
        hideEditorButton(packetDirectionBtn);
        hideEditorButton(importGuiBtn);
        hideEditorButton(openGuiInspectorBtn);
        hideEditorButton(importPacketBtn);
        hideEditorButton(idleExcludePathBtn);
        hideEditorButton(idleIgnoreDamageBtn);
    }

    private void fillFromLatestGuiSnapshot() {
        List<GuiInspectionManager.CapturedGuiSnapshot> history = GuiInspectionManager.getHistory();
        if (history.isEmpty()) {
            statusMessage = "§eGUI识别历史为空，请先到调试-GUI识别管理器开启捕获并打开目标界面";
            return;
        }
        GuiInspectionManager.CapturedGuiSnapshot snapshot = history.get(0);
        guiTitleField.setText(safe(snapshot.getTitle()));
        guiClassField.setText(safe(snapshot.getScreenClassName()));
        activeEditorTab = TAB_EVENT;
        statusMessage = "§a已填入最近GUI: " + safe(snapshot.getScreenSimpleName());
    }

    private void fillFromLatestPacketText() {
        List<String> texts = PacketCaptureHandler.getRecentPacketTextsSnapshot();
        if (texts.isEmpty()) {
            statusMessage = "§e最近包文本为空，请先触发数据包或开启相关抓包调试";
            return;
        }
        packetTextField.setText(safe(texts.get(texts.size() - 1)));
        activeEditorTab = TAB_EVENT;
        statusMessage = "§a已填入最近数据包文本";
    }

    private String getLatestGuiSnapshotSummary() {
        List<GuiInspectionManager.CapturedGuiSnapshot> history = GuiInspectionManager.getHistory();
        if (history.isEmpty()) {
            return "(无)";
        }
        GuiInspectionManager.CapturedGuiSnapshot snapshot = history.get(0);
        String title = safe(snapshot.getTitle()).trim();
        String screen = safe(snapshot.getScreenSimpleName()).trim();
        if (!title.isEmpty()) {
            return title + (screen.isEmpty() ? "" : " | " + screen);
        }
        return screen.isEmpty() ? "(无标题)" : screen;
    }

    private String getLatestPacketTextSummary() {
        List<String> texts = PacketCaptureHandler.getRecentPacketTextsSnapshot();
        if (texts.isEmpty()) {
            return "(无)";
        }
        return trim(texts.get(texts.size() - 1), Math.max(120, editorW - 48));
    }

    private String stringParam(JsonObject params, String key) {
        if (params == null || !params.has(key)) {
            return "";
        }
        try {
            return safe(params.get(key).getAsString()).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private int intParam(JsonObject params, String key, int fallback) {
        if (params == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean boolParam(JsonObject params, String key, boolean fallback) {
        if (params == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double doubleParam(JsonObject params, String key, double fallback) {
        if (params == null || !params.has(key)) {
            return fallback;
        }
        try {
            return params.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void putTrimmed(JsonObject params, String key, String value) {
        String text = safe(value).trim();
        if (!text.isEmpty()) {
            params.addProperty(key, text);
        }
    }

    private JsonObject copyJson(JsonObject source) {
        if (source == null) {
            return new JsonObject();
        }
        try {
            return new JsonParser().parse(source.toString()).getAsJsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    private int parseInt(String text, int fallback, int min) {
        try {
            return Math.max(min, Integer.parseInt(safe(text).trim()));
        } catch (Exception ignored) {
            return Math.max(min, fallback);
        }
    }

    private double parseDouble(String text, double fallback, double min) {
        try {
            return Math.max(min, Double.parseDouble(safe(text).trim()));
        } catch (Exception ignored) {
            return Math.max(min, fallback);
        }
    }

    private String floatText(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        if (text.endsWith(".00")) {
            return text.substring(0, text.length() - 3);
        }
        if (text.endsWith("0")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    private String trim(String text, int width) {
        String safeText = safe(text);
        if (fontRenderer == null || fontRenderer.getStringWidth(safeText) <= width) {
            return safeText;
        }
        return fontRenderer.trimStringToWidth(safeText, Math.max(0, width - fontRenderer.getStringWidth("...")))
                + "...";
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}






