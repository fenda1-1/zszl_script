package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.runtime.ScopedRuntimeVariables;
import com.zszl.zszlScriptMod.utils.PacketFieldRuleManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiPacketFieldRules extends ThemedGuiScreen {

    private static final String[] DIRECTION_OPTIONS = new String[] { "both", "inbound", "outbound" };
    private static final String[] SOURCE_OPTIONS = new String[] { "decoded", "hex", "channel", "class" };
    private static final String[] EXTRACT_MODE_OPTIONS = new String[] { "regex", "offset", "key", "length_prefixed" };
    private static final String[] VALUE_TYPE_OPTIONS = new String[] { "auto", "string", "number", "boolean", "hex" };
    private static final String[] SCOPE_OPTIONS = new String[] { "global", "sequence", "local", "temp" };

    private final GuiScreen parent;
    private final List<PacketFieldRuleManager.RuleEditModel> rules = new ArrayList<>();

    private int selected = -1;
    private int scroll = 0;

    private GuiTextField nameField;
    private GuiTextField channelField;
    private GuiTextField patternField;
    private GuiTextField groupField;
    private GuiTextField variableField;
    private GuiTextField defaultValueField;
    private GuiTextField noteField;

    private GuiButton toggleEnabledBtn;
    private GuiButton directionBtn;
    private GuiButton sourceBtn;
    private GuiButton extractModeBtn;
    private GuiButton valueTypeBtn;
    private GuiButton scopeBtn;
    private GuiButton writeDefaultBtn;

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private static final int ROW_H = 28;

    private boolean editingEnabled = true;
    private int editingDirectionIndex = 0;
    private int editingSourceIndex = 0;
    private int editingExtractModeIndex = 0;
    private int editingValueTypeIndex = 0;
    private int editingScopeIndex = 0;
    private boolean editingWriteDefault = false;
    private int editorX;
    private int editorY;
    private int editorW;
    private int editorH;
    private int editorScroll = 0;
    private int maxEditorScroll = 0;
    private int nameBaseY;
    private int channelBaseY;
    private int enabledBaseY;
    private int directionBaseY;
    private int extractBaseY;
    private int scopeBaseY;
    private int patternBaseY;
    private int groupBaseY;
    private int variableBaseY;
    private int defaultBaseY;
    private int noteBaseY;
    private int previewBaseY;

    public GuiPacketFieldRules(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        reloadWorkingCopy();

        int panelW = Math.min(980, this.width - 30);
        int panelX = (this.width - panelW) / 2;
        int panelY = 30;

        listX = panelX + 10;
        listY = panelY + 36;
        listW = 320;
        listH = this.height - panelY - 92;

        int rightX = listX + listW + 12;
        int rightW = panelX + panelW - rightX - 10;
        editorX = rightX - 6;
        editorY = listY;
        editorW = rightW + 12;
        editorH = listH;

        int y = listY;
        nameBaseY = y;
        nameField = createField(2001, rightX, y, rightW, 18);
        y += 24;
        channelBaseY = y;
        channelField = createField(2002, rightX, y, rightW, 18);
        y += 24;
        enabledBaseY = y;
        toggleEnabledBtn = new ThemedButton(20, rightX, y, rightW, 20, "");
        buttonList.add(toggleEnabledBtn);
        y += 26;

        int half = (rightW - 6) / 2;
        directionBaseY = y;
        directionBtn = new ThemedButton(21, rightX, y, half, 20, "");
        sourceBtn = new ThemedButton(22, rightX + half + 6, y, half, 20, "");
        buttonList.add(directionBtn);
        buttonList.add(sourceBtn);
        y += 26;

        extractBaseY = y;
        extractModeBtn = new ThemedButton(23, rightX, y, half, 20, "");
        valueTypeBtn = new ThemedButton(24, rightX + half + 6, y, half, 20, "");
        buttonList.add(extractModeBtn);
        buttonList.add(valueTypeBtn);
        y += 26;

        scopeBaseY = y;
        scopeBtn = new ThemedButton(25, rightX, y, half, 20, "");
        writeDefaultBtn = new ThemedButton(26, rightX + half + 6, y, half, 20, "");
        buttonList.add(scopeBtn);
        buttonList.add(writeDefaultBtn);
        y += 26;

        patternBaseY = y;
        patternField = createField(2003, rightX, y, rightW, 18);
        patternField.setMaxStringLength(Integer.MAX_VALUE);
        y += 24;
        groupBaseY = y;
        groupField = createField(2004, rightX, y, rightW, 18);
        y += 24;
        variableBaseY = y;
        variableField = createField(2005, rightX, y, rightW, 18);
        y += 24;
        defaultBaseY = y;
        defaultValueField = createField(2006, rightX, y, rightW, 18);
        y += 24;
        noteBaseY = y;
        noteField = createField(2007, rightX, y, rightW, 18);
        noteField.setMaxStringLength(Integer.MAX_VALUE);
        previewBaseY = y + 36;

        int btnY = this.height - 32;
        buttonList.add(new ThemedButton(1, listX, btnY, 72, 20, "新增"));
        buttonList.add(new ThemedButton(2, listX + 78, btnY, 72, 20, "删除"));
        buttonList.add(new ThemedButton(3, listX + 156, btnY, 72, 20, "重载"));
        buttonList.add(new ThemedButton(4, panelX + panelW - 258, btnY, 78, 20, "校验"));
        buttonList.add(new ThemedButton(5, panelX + panelW - 174, btnY, 78, 20, "保存"));
        buttonList.add(new ThemedButton(6, panelX + panelW - 90, btnY, 78, 20, "返回"));

        if (selected >= rules.size()) {
            selected = rules.isEmpty() ? -1 : 0;
        }
        if (selected >= 0) {
            loadFromRule(rules.get(selected));
        } else {
            clearEditor();
        }
        updateEditorLayout();
        refreshToggleText();
    }

    private GuiTextField createField(int id, int x, int y, int width, int height) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, width, height);
        field.setMaxStringLength(256);
        return field;
    }

    private void reloadWorkingCopy() {
        rules.clear();
        rules.addAll(PacketFieldRuleManager.getRuleModels());
    }

    private void loadFromRule(PacketFieldRuleManager.RuleEditModel rule) {
        if (rule == null) {
            clearEditor();
            return;
        }
        nameField.setText(safe(rule.name));
        channelField.setText(safe(rule.channel));
        patternField.setText(safe(rule.pattern));
        groupField.setText(String.valueOf(Math.max(1, rule.group)));
        variableField.setText(safe(rule.variableName));
        defaultValueField.setText(safe(rule.defaultValue));
        noteField.setText(safe(rule.note));
        editingEnabled = rule.enabled;
        editingDirectionIndex = indexOf(DIRECTION_OPTIONS, rule.direction, 0);
        editingSourceIndex = indexOf(SOURCE_OPTIONS, rule.source, 0);
        editingExtractModeIndex = indexOf(EXTRACT_MODE_OPTIONS, rule.extractMode, 0);
        editingValueTypeIndex = indexOf(VALUE_TYPE_OPTIONS, rule.valueType, 0);
        editingScopeIndex = indexOf(SCOPE_OPTIONS, rule.scope, 0);
        editingWriteDefault = rule.writeDefaultOnFailure;
    }

    private void clearEditor() {
        nameField.setText("");
        channelField.setText("");
        patternField.setText("");
        groupField.setText("1");
        variableField.setText("");
        defaultValueField.setText("");
        noteField.setText("");
        editingEnabled = true;
        editingDirectionIndex = 0;
        editingSourceIndex = 0;
        editingExtractModeIndex = 0;
        editingValueTypeIndex = 0;
        editingScopeIndex = 0;
        editingWriteDefault = false;
    }

    private void refreshToggleText() {
        if (toggleEnabledBtn != null) {
            toggleEnabledBtn.displayString = "启用规则: " + formatBool(editingEnabled);
        }
        if (directionBtn != null) {
            directionBtn.displayString = "方向: " + DIRECTION_OPTIONS[editingDirectionIndex];
        }
        if (sourceBtn != null) {
            sourceBtn.displayString = "来源: " + SOURCE_OPTIONS[editingSourceIndex];
        }
        if (extractModeBtn != null) {
            extractModeBtn.displayString = "提取模式: " + EXTRACT_MODE_OPTIONS[editingExtractModeIndex];
        }
        if (valueTypeBtn != null) {
            valueTypeBtn.displayString = "类型: " + VALUE_TYPE_OPTIONS[editingValueTypeIndex];
        }
        if (scopeBtn != null) {
            scopeBtn.displayString = "作用域: " + SCOPE_OPTIONS[editingScopeIndex];
        }
        if (writeDefaultBtn != null) {
            writeDefaultBtn.displayString = "失败写默认值: " + formatBool(editingWriteDefault);
        }
    }

    private String formatBool(boolean value) {
        return value ? "§aON" : "§cOFF";
    }

    private void clampEditorScroll() {
        int contentBottom = previewBaseY + 86;
        maxEditorScroll = Math.max(0, contentBottom - editorY - editorH + 6);
        editorScroll = Math.max(0, Math.min(editorScroll, maxEditorScroll));
    }

    private void updateEditorLayout() {
        clampEditorScroll();
        nameField.y = nameBaseY - editorScroll;
        channelField.y = channelBaseY - editorScroll;
        patternField.y = patternBaseY - editorScroll;
        groupField.y = groupBaseY - editorScroll;
        variableField.y = variableBaseY - editorScroll;
        defaultValueField.y = defaultBaseY - editorScroll;
        noteField.y = noteBaseY - editorScroll;
        toggleEnabledBtn.y = enabledBaseY - editorScroll;
        directionBtn.y = directionBaseY - editorScroll;
        sourceBtn.y = directionBaseY - editorScroll;
        extractModeBtn.y = extractBaseY - editorScroll;
        valueTypeBtn.y = extractBaseY - editorScroll;
        scopeBtn.y = scopeBaseY - editorScroll;
        writeDefaultBtn.y = scopeBaseY - editorScroll;

        toggleEnabledBtn.visible = isVisibleInEditor(toggleEnabledBtn.y, toggleEnabledBtn.height);
        directionBtn.visible = isVisibleInEditor(directionBtn.y, directionBtn.height);
        sourceBtn.visible = isVisibleInEditor(sourceBtn.y, sourceBtn.height);
        extractModeBtn.visible = isVisibleInEditor(extractModeBtn.y, extractModeBtn.height);
        valueTypeBtn.visible = isVisibleInEditor(valueTypeBtn.y, valueTypeBtn.height);
        scopeBtn.visible = isVisibleInEditor(scopeBtn.y, scopeBtn.height);
        writeDefaultBtn.visible = isVisibleInEditor(writeDefaultBtn.y, writeDefaultBtn.height);
    }

    private boolean isVisibleInEditor(int y, int height) {
        return y + height >= editorY && y <= editorY + editorH;
    }

    private boolean isVisibleField(GuiTextField field) {
        return field != null && isVisibleInEditor(field.y, field.height);
    }

    private boolean isInsideEditor(int mouseX, int mouseY) {
        return mouseX >= editorX && mouseX <= editorX + editorW && mouseY >= editorY && mouseY <= editorY + editorH;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                PacketFieldRuleManager.RuleEditModel created = new PacketFieldRuleManager.RuleEditModel();
                created.name = "field_rule_" + (rules.size() + 1);
                created.group = 1;
                rules.add(created);
                selected = rules.size() - 1;
                loadFromRule(created);
                ensureVisible();
                break;
            case 2:
                if (selected >= 0 && selected < rules.size()) {
                    rules.remove(selected);
                    if (selected >= rules.size()) {
                        selected = rules.size() - 1;
                    }
                    if (selected >= 0) {
                        loadFromRule(rules.get(selected));
                    } else {
                        clearEditor();
                    }
                }
                break;
            case 3:
                reloadWorkingCopy();
                selected = rules.isEmpty() ? -1 : 0;
                if (selected >= 0) {
                    loadFromRule(rules.get(selected));
                } else {
                    clearEditor();
                }
                break;
            case 4:
                flushEditorToSelected();
                String error = validateRules();
                toast(error == null ? ChatFormatting.GREEN + "字段提取规则校验通过"
                        : ChatFormatting.RED + "校验失败: " + error);
                break;
            case 5:
                flushEditorToSelected();
                String validationError = validateRules();
                if (validationError != null) {
                    toast(ChatFormatting.RED + "保存失败: " + validationError);
                    break;
                }
                PacketFieldRuleManager.saveRuleModels(rules);
                toast(ChatFormatting.GREEN + "字段提取规则已保存");
                break;
            case 6:
                mc.setScreen(parent);
                break;
            case 20:
                editingEnabled = !editingEnabled;
                refreshToggleText();
                break;
            case 21:
                editingDirectionIndex = (editingDirectionIndex + 1) % DIRECTION_OPTIONS.length;
                refreshToggleText();
                break;
            case 22:
                editingSourceIndex = (editingSourceIndex + 1) % SOURCE_OPTIONS.length;
                refreshToggleText();
                break;
            case 23:
                editingExtractModeIndex = (editingExtractModeIndex + 1) % EXTRACT_MODE_OPTIONS.length;
                refreshToggleText();
                break;
            case 24:
                editingValueTypeIndex = (editingValueTypeIndex + 1) % VALUE_TYPE_OPTIONS.length;
                refreshToggleText();
                break;
            case 25:
                editingScopeIndex = (editingScopeIndex + 1) % SCOPE_OPTIONS.length;
                refreshToggleText();
                break;
            case 26:
                editingWriteDefault = !editingWriteDefault;
                refreshToggleText();
                break;
            default:
                break;
        }
    }

    private String validateRules() {
        for (PacketFieldRuleManager.RuleEditModel rule : rules) {
            if (rule == null) {
                continue;
            }
            if (safe(rule.pattern).trim().isEmpty()) {
                return "存在空正则";
            }
            if (safe(rule.variableName).trim().isEmpty()) {
                return "存在空变量名";
            }
            if (safe(rule.pattern).trim().isEmpty()) {
                return "存在空规则值";
            }
            if ("regex".equalsIgnoreCase(rule.extractMode) && rule.group <= 0) {
                return "存在非法 group";
            }
        }
        return null;
    }

    private void flushEditorToSelected() {
        if (selected < 0 || selected >= rules.size()) {
            return;
        }
        PacketFieldRuleManager.RuleEditModel rule = rules.get(selected);
        rule.name = safe(nameField.getText()).trim();
        rule.channel = safe(channelField.getText()).trim();
        rule.pattern = safe(patternField.getText()).trim();
        rule.variableName = safe(variableField.getText()).trim();
        rule.note = safe(noteField.getText()).trim();
        rule.enabled = editingEnabled;
        rule.direction = DIRECTION_OPTIONS[editingDirectionIndex];
        rule.source = SOURCE_OPTIONS[editingSourceIndex];
        rule.extractMode = EXTRACT_MODE_OPTIONS[editingExtractModeIndex];
        rule.valueType = VALUE_TYPE_OPTIONS[editingValueTypeIndex];
        rule.scope = SCOPE_OPTIONS[editingScopeIndex];
        rule.writeDefaultOnFailure = editingWriteDefault;
        rule.defaultValue = safe(defaultValueField.getText()).trim();
        try {
            rule.group = Math.max(1, Integer.parseInt(safe(groupField.getText()).trim()));
        } catch (Exception ignored) {
            rule.group = 1;
        }
    }

    private int indexOf(String[] options, String value, int fallback) {
        if (options == null || value == null) {
            return fallback;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return fallback;
    }

    private void ensureVisible() {
        int visible = Math.max(1, listH / ROW_H);
        if (selected < scroll) {
            scroll = selected;
        } else if (selected >= scroll + visible) {
            scroll = selected - visible + 1;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelW = Math.min(980, this.width - 30);
        int panelX = (this.width - panelW) / 2;
        int panelY = 30;
        GuiTheme.drawPanel(panelX, panelY, panelW, this.height - 60);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "字段提取规则", this.fontRenderer);

        drawRect(listX, listY, listX + listW, listY + listH, 0x66324458);
        drawString(fontRenderer, "规则列表", listX + 6, listY - 12, 0xFFFFFF);
        GuiTheme.drawInputFrameSafe(editorX, editorY, editorW, editorH, false, true);

        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, rules.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < visible; i++) {
            int actual = i + scroll;
            if (actual >= rules.size()) {
                break;
            }
            int rowY = listY + i * ROW_H;
            PacketFieldRuleManager.RuleEditModel rule = rules.get(actual);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + ROW_H - 2;
            int bg = actual == selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x55222A33);
            drawRect(listX + 2, rowY, listX + listW - 2, rowY + ROW_H - 2, bg);
            String title = (rule.enabled ? "§a✔ " : "§c✘ ") + safe(rule.name);
            drawString(fontRenderer, trimToWidth(title, listW - 14), listX + 6, rowY + 4, 0xFFFFFFFF);
            String sub = rule.source + " | " + rule.variableName + " | g" + Math.max(1, rule.group);
            drawString(fontRenderer, trimToWidth("§7" + sub, listW - 14), listX + 6, rowY + 15, 0xFFFFFFFF);
        }

        updateEditorLayout();
        drawEditorLabels();
        if (isVisibleField(nameField)) drawThemedTextField(nameField);
        if (isVisibleField(channelField)) drawThemedTextField(channelField);
        if (isVisibleField(patternField)) drawThemedTextField(patternField);
        if (isVisibleField(groupField)) drawThemedTextField(groupField);
        if (isVisibleField(variableField)) drawThemedTextField(variableField);
        if (isVisibleField(defaultValueField)) drawThemedTextField(defaultValueField);
        if (isVisibleField(noteField)) drawThemedTextField(noteField);

        drawGlobalPreview(nameField.x, previewBaseY - editorScroll, Math.min(240, editorW - 20), 82);

        if (maxEditorScroll > 0) {
            int thumbHeight = Math.max(18, (int) ((editorH / (float) Math.max(editorH, editorH + maxEditorScroll)) * editorH));
            int track = Math.max(1, editorH - thumbHeight);
            int thumbY = editorY + (int) ((editorScroll / (float) Math.max(1, maxEditorScroll)) * track);
            GuiTheme.drawScrollbar(editorX + editorW - 8, editorY, 4, editorH, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawFieldRuleTooltip(mouseX, mouseY);
    }

    private void drawEditorLabels() {
        if (isVisibleField(nameField)) drawString(fontRenderer, "规则名", nameField.x, nameField.y - 10, 0xFFFFFF);
        if (isVisibleField(channelField)) drawString(fontRenderer, "频道(可空)", channelField.x, channelField.y - 10, 0xFFFFFF);
        if (isVisibleField(patternField)) drawString(fontRenderer, "参数 / 规则值", patternField.x, patternField.y - 10, 0xFFFFFF);
        if (isVisibleField(groupField)) drawString(fontRenderer, "分组 / 辅助值", groupField.x, groupField.y - 10, 0xFFFFFF);
        if (isVisibleField(variableField)) drawString(fontRenderer, "变量名", variableField.x, variableField.y - 10, 0xFFFFFF);
        if (isVisibleField(defaultValueField)) drawString(fontRenderer, "默认值", defaultValueField.x, defaultValueField.y - 10, 0xFFFFFF);
        if (isVisibleField(noteField)) drawString(fontRenderer, "备注", noteField.x, noteField.y - 10, 0xFFFFFF);
    }

    private void drawGlobalPreview(int x, int y, int width, int height) {
        GuiTheme.drawInputFrameSafe(x, y, width, height, false, true);
        drawString(fontRenderer, "全局变量预览", x + 6, y + 6, 0xFFFFFF);
        Map<String, Object> globals = ScopedRuntimeVariables.getGlobalScopeSnapshot();
        if (globals.isEmpty()) {
            drawString(fontRenderer, "§7(空)", x + 6, y + 20, 0xFFB8C7D9);
            return;
        }
        int line = 0;
        for (Map.Entry<String, Object> entry : globals.entrySet()) {
            drawString(fontRenderer,
                    trimToWidth("§f" + entry.getKey() + " §7= §b" + String.valueOf(entry.getValue()), width - 12),
                    x + 6, y + 20 + line * 12, 0xFFFFFFFF);
            if (++line >= 4) {
                break;
            }
        }
    }

    private String trimToWidth(String text, int width) {
        return fontRenderer.trimStringToWidth(text == null ? "" : text, Math.max(20, width));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parent);
            return;
        }
        nameField.textboxKeyTyped(typedChar, keyCode);
        channelField.textboxKeyTyped(typedChar, keyCode);
        patternField.textboxKeyTyped(typedChar, keyCode);
        groupField.textboxKeyTyped(typedChar, keyCode);
        variableField.textboxKeyTyped(typedChar, keyCode);
        defaultValueField.textboxKeyTyped(typedChar, keyCode);
        noteField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(nameField)) nameField.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(channelField)) channelField.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(patternField)) patternField.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(groupField)) groupField.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(variableField)) variableField.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(defaultValueField)) defaultValueField.mouseClicked(mouseX, mouseY, mouseButton);
        if (isVisibleField(noteField)) noteField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int actual = (mouseY - listY) / ROW_H + scroll;
            if (actual >= 0 && actual < rules.size()) {
                flushEditorToSelected();
                selected = actual;
                loadFromRule(rules.get(selected));
                updateEditorLayout();
            }
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
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int visible = Math.max(1, listH / ROW_H);
            int maxScroll = Math.max(0, rules.size() - visible);
            if (dWheel < 0) {
                scroll = Math.min(maxScroll, scroll + 1);
            } else {
                scroll = Math.max(0, scroll - 1);
            }
            return;
        }
        if (isInsideEditor(mouseX, mouseY)) {
            if (dWheel < 0) {
                editorScroll = Math.min(maxEditorScroll, editorScroll + 14);
            } else {
                editorScroll = Math.max(0, editorScroll - 14);
            }
            updateEditorLayout();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private void toast(String text) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(new TextComponentString(text));
        }
    }

    private void drawFieldRuleTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            tooltip = "字段提取规则列表。\n左键切换当前编辑项，保存后会在抓包业务链里执行提取。";
        } else if (isMouseOverField(mouseX, mouseY, nameField)) {
            tooltip = "规则名称，仅用于区分和管理这条字段提取规则。";
        } else if (isMouseOverField(mouseX, mouseY, channelField)) {
            tooltip = "频道过滤。\n可限制只处理某个频道的数据包，留空表示任意频道。";
        } else if (isMouseOverField(mouseX, mouseY, patternField)) {
            tooltip = "提取参数。\n不同提取模式含义不同：正则模式填表达式，偏移模式填起始位置等。";
        } else if (isMouseOverField(mouseX, mouseY, groupField)) {
            tooltip = "正则分组或辅助数值。\n正则模式下一般填 group 编号。";
        } else if (isMouseOverField(mouseX, mouseY, variableField)) {
            tooltip = "提取结果写入的变量名。\n后续动作、条件或其它规则可以直接读取。";
        } else if (isMouseOverField(mouseX, mouseY, defaultValueField)) {
            tooltip = "提取失败时可写入的默认值。";
        } else if (isMouseOverField(mouseX, mouseY, noteField)) {
            tooltip = "备注说明，方便你记录这条规则的用途。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case 1:
                        tooltip = "新增一条字段提取规则。";
                        break;
                    case 2:
                        tooltip = "删除当前选中的字段提取规则。";
                        break;
                    case 3:
                        tooltip = "重新从配置文件加载字段提取规则。";
                        break;
                    case 4:
                        tooltip = "校验当前字段提取规则是否存在空值或明显配置错误。";
                        break;
                    case 5:
                        tooltip = "保存当前字段提取规则。";
                        break;
                    case 6:
                        tooltip = "返回上一页。";
                        break;
                    case 20:
                        tooltip = "启用/禁用当前规则。";
                        break;
                    case 21:
                        tooltip = "切换方向：双向 / 入站 / 出站。";
                        break;
                    case 22:
                        tooltip = "切换来源：从解码文本、HEX、频道或类名中提取。";
                        break;
                    case 23:
                        tooltip = "切换提取模式：正则、偏移、键值、长度前缀等。";
                        break;
                    case 24:
                        tooltip = "切换提取结果类型：自动、字符串、数字、布尔或 HEX。";
                        break;
                    case 25:
                        tooltip = "切换变量作用域：全局、序列、局部或临时。";
                        break;
                    case 26:
                        tooltip = "失败写默认值开关。\n开启后提取失败会把默认值写入变量。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}









