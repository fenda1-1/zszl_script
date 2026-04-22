package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiCapturedIdViewer extends ThemedGuiScreen {

    private static final int BTN_DONE = 100;
    private static final int BTN_SAVE = 102;
    private static final int BTN_NEW_CATEGORY = 106;
    private static final int BTN_SMART_GENERATOR = 107;

    private static final String CATEGORY_ALL = "__all__";
    private static final String CATEGORY_UNGROUPED = "未分组";

    private static final int TREE_ROW_HEIGHT = 18;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 6;
    private static final int EDITOR_ROW_HEIGHT = 20;
    private static final int EDITOR_BASE_ROWS = 16;

    private static final String[] ENABLED_OPTIONS = new String[] { "true", "false" };
    private static final String[] ENABLED_LABEL_KEYS = new String[] {
            "gui.packet.captured_id.option.true",
            "gui.packet.captured_id.option.false"
    };
    private static final String[] DIRECTION_OPTIONS = new String[] { "both", "inbound", "outbound" };
    private static final String[] DIRECTION_LABEL_KEYS = new String[] {
            "gui.packet.captured_id.direction.both",
            "gui.packet.captured_id.direction.inbound",
            "gui.packet.captured_id.direction.outbound"
    };
    private static final String[] TARGET_OPTIONS = new String[] { "hex", "decoded" };
    private static final String[] TARGET_LABEL_KEYS = new String[] {
            "gui.packet.captured_id.target.hex",
            "gui.packet.captured_id.target.decoded"
    };
    private static final String[] VALUE_TYPE_OPTIONS = new String[] { "hex", "decimal-int" };
    private static final String[] VALUE_TYPE_LABEL_KEYS = new String[] {
            "gui.packet.captured_id.value_type.hex",
            "gui.packet.captured_id.value_type.decimal_int"
    };
    private static final String[] UPDATE_SEQUENCE_MODE_OPTIONS = new String[] { "always", "first", "cooldown",
            "recapture" };
    private static final String[] UPDATE_SEQUENCE_MODE_LABEL_KEYS = new String[] {
            "gui.packet.captured_id.update_sequence_mode.always",
            "gui.packet.captured_id.update_sequence_mode.first",
            "gui.packet.captured_id.update_sequence_mode.cooldown",
            "gui.packet.captured_id.update_sequence_mode.recapture"
    };

    private final GuiScreen parentScreen;

    private final List<CapturedIdRuleManager.RuleCard> allCards = new ArrayList<>();
    private final List<CapturedIdRuleManager.RuleCard> visibleCards = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<TreeRow> visibleTreeRows = new ArrayList<>();
    private final Set<String> expandedCategories = new LinkedHashSet<>();

    private String selectedCategory = CATEGORY_ALL;
    private int selectedVisibleIndex = -1;
    private int selectedTreeIndex = 0;
    private int treeScrollOffset = 0;
    private int listScrollOffset = 0;
    private boolean creatingNew = false;
    private boolean treeCollapsed = false;
    private boolean listCollapsed = false;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int treeX;
    private int treeY;
    private int treeWidth;
    private int treeHeight;

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    private int editorBoxX;
    private int editorBoxY;
    private int editorBoxWidth;
    private int editorBoxHeight;
    private int hintBoxX;
    private int hintBoxY;
    private int hintBoxWidth;
    private int hintBoxHeight;

    private int editorLabelX;
    private int editorLabelWidth;
    private int editorRowStartY;
    private int editorVisibleRows = EDITOR_BASE_ROWS;
    private int editorScrollOffset = 0;

    private GuiTextField nameField;
    private GuiTextField displayNameField;
    private GuiTextField noteField;
    private GuiTextField aliasesField;
    private GuiTextField enabledField;
    private GuiTextField channelField;
    private GuiTextField directionField;
    private GuiTextField targetField;
    private GuiTextField patternField;
    private GuiTextField offsetField;
    private GuiTextField groupField;
    private GuiTextField categoryField;
    private GuiTextField valueTypeField;
    private GuiTextField byteLengthField;
    private GuiTextField updateSequenceField;
    private GuiTextField updateSequenceModeField;
    private GuiTextField updateSequenceCooldownField;

    private EnumDropdown enabledDropdown;
    private EnumDropdown directionDropdown;
    private EnumDropdown targetDropdown;
    private EnumDropdown valueTypeDropdown;
    private EnumDropdown updateSequenceModeDropdown;
    private GuiButton selectSequenceButton;

    private boolean editingBuiltinSequenceOnly = false;
    private CapturedIdRuleManager.RuleEditModel pendingEditorRestoreModel = null;
    private boolean pendingEditorRestoreCreateMode = false;
    private String pendingEditorRestoreCategory = CATEGORY_ALL;
    private String pendingEditorRestoreRuleName = "";

    private final List<ContextMenuItem> contextMenuItems = new ArrayList<>();
    private boolean contextMenuVisible = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private int contextMenuWidth = 168;
    private int contextMenuItemHeight = 18;
    private int contextMenuTargetIndex = -1;
    private String contextMenuTargetType = "card";
    private String contextMenuTargetCategory = "";

    private String statusMessage = "§7左侧分组树默认折叠；右键分组/卡片都可打开完整菜单";
    private int statusColor = 0xFFB8C7D9;

    public GuiCapturedIdViewer(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        recalcLayout();
        initEditorFields();
        initButtons();
        layoutEditorFields();

        refreshData(false);
        restorePendingEditorStateIfNeeded();
    }

    private void recalcLayout() {
        panelWidth = Math.min(1140, this.width - 20);
        panelHeight = Math.min(620, this.height - 20);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int sidePadding = 12;
        int columnGap = this.width < 960 ? 8 : 10;
        int availableWidth = panelWidth - sidePadding * 2;

        treeX = panelX + sidePadding;
        treeY = panelY + 46;
        treeHeight = panelHeight - 104;
        treeWidth = treeCollapsed
                ? getCollapsedSectionWidth("分组")
                : Math.max(118, Math.min(160, availableWidth / 6));

        listX = treeX + treeWidth + columnGap;
        listY = treeY;
        listWidth = listCollapsed
                ? getCollapsedSectionWidth("卡片")
                : Math.max(172, Math.min(208, availableWidth / 5));

        int minimumEditorWidth = Math.max(240, Math.min(420, availableWidth / 2));
        int totalNeeded = treeWidth + columnGap + listWidth + columnGap + minimumEditorWidth;
        if (totalNeeded > availableWidth) {
            int overflow = totalNeeded - availableWidth;

            int listReducible = listWidth - 150;
            if (overflow > 0 && listReducible > 0) {
                int reduce = Math.min(overflow, listReducible);
                listWidth -= reduce;
                overflow -= reduce;
            }

            int treeReducible = treeWidth - 108;
            if (overflow > 0 && treeReducible > 0) {
                int reduce = Math.min(overflow, treeReducible);
                treeWidth -= reduce;
            }
        }

        listX = treeX + treeWidth + columnGap;
        listHeight = treeHeight;

        int rightX = listX + listWidth + columnGap;
        int rightWidth = panelX + panelWidth - rightX - sidePadding;

        hintBoxX = rightX;
        hintBoxWidth = rightWidth;
        hintBoxHeight = 56;
        hintBoxY = treeY + treeHeight - hintBoxHeight;

        editorBoxX = rightX;
        editorBoxY = treeY;
        editorBoxWidth = rightWidth;
        editorBoxHeight = Math.max(120, hintBoxY - editorBoxY - 6);

        editorLabelX = editorBoxX + 4;
        editorLabelWidth = Math.max(72, Math.min(92, editorBoxWidth / 5));
        editorRowStartY = editorBoxY + 8;
        editorVisibleRows = Math.max(1, (editorBoxHeight - 18) / EDITOR_ROW_HEIGHT);
    }

    private void initEditorFields() {
        int labelW = editorLabelWidth;
        int fieldW = editorBoxWidth - labelW - 10;
        int x = editorBoxX + labelW;
        int y = editorRowStartY;
        int h = 16;
        int gap = 4;

        nameField = createField(2001, x, y, fieldW, h);
        y += h + gap;
        displayNameField = createField(2002, x, y, fieldW, h);
        y += h + gap;
        noteField = createField(2003, x, y, fieldW, h);
        y += h + gap;
        aliasesField = createField(2004, x, y, fieldW, h);
        y += h + gap;
        enabledField = createField(2005, x, y, fieldW, h);
        y += h + gap;
        channelField = createField(2006, x, y, fieldW, h);
        y += h + gap;
        directionField = createField(2007, x, y, fieldW, h);
        y += h + gap;
        targetField = createField(2008, x, y, fieldW, h);
        y += h + gap;
        patternField = createField(2009, x, y, fieldW, h);
        y += h + gap;
        offsetField = createField(2010, x, y, fieldW, h);
        y += h + gap;
        groupField = createField(2011, x, y, fieldW, h);
        y += h + gap;
        categoryField = createField(2012, x, y, fieldW, h);
        y += h + gap;
        valueTypeField = createField(2013, x, y, fieldW, h);
        y += h + gap;
        byteLengthField = createField(2014, x, y, fieldW, h);
        y += h + gap;
        updateSequenceField = createField(2015, x, y, fieldW - 64, h);
        selectSequenceButton = new GuiButton(2016, editorBoxX + editorBoxWidth - 56, y, 56, 16,
                I18n.format("gui.path.select_sequence"));
        this.buttonList.add(selectSequenceButton);
        y += h + gap;
        updateSequenceModeField = createField(2017, x, y, fieldW, h);
        y += h + gap;
        updateSequenceCooldownField = createField(2018, x, y, fieldW, h);

        enabledField.setEnabled(false);
        directionField.setEnabled(false);
        targetField.setEnabled(false);
        valueTypeField.setEnabled(false);
        updateSequenceModeField.setEnabled(false);

        enabledDropdown = new EnumDropdown(enabledField.x, enabledField.y, enabledField.width, enabledField.height,
                ENABLED_OPTIONS, ENABLED_LABEL_KEYS);
        directionDropdown = new EnumDropdown(directionField.x, directionField.y, directionField.width,
                directionField.height, DIRECTION_OPTIONS, DIRECTION_LABEL_KEYS);
        targetDropdown = new EnumDropdown(targetField.x, targetField.y, targetField.width, targetField.height,
                TARGET_OPTIONS, TARGET_LABEL_KEYS);
        valueTypeDropdown = new EnumDropdown(valueTypeField.x, valueTypeField.y, valueTypeField.width,
                valueTypeField.height, VALUE_TYPE_OPTIONS, VALUE_TYPE_LABEL_KEYS);
        updateSequenceModeDropdown = new EnumDropdown(updateSequenceModeField.x, updateSequenceModeField.y,
                updateSequenceModeField.width, updateSequenceModeField.height, UPDATE_SEQUENCE_MODE_OPTIONS,
                UPDATE_SEQUENCE_MODE_LABEL_KEYS);
    }

    private void initButtons() {
        int btnY = panelY + panelHeight - 28;

        this.buttonList.add(new GuiButton(BTN_NEW_CATEGORY, treeX, btnY, 90, 20, "新建分组"));
        this.buttonList.add(new GuiButton(BTN_SMART_GENERATOR, treeX + 96, btnY, 110, 20, "智能生成器"));
        this.buttonList.add(new GuiButton(BTN_SAVE, panelX + panelWidth - 186, btnY, 84, 20,
                I18n.format("gui.common.save")));
        this.buttonList.add(new GuiButton(BTN_DONE, panelX + panelWidth - 94, btnY, 84, 20,
                I18n.format("gui.common.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DONE) {
            mc.setScreen(parentScreen);
            return;
        }

        if (button.id == BTN_NEW_CATEGORY) {
            openAddCategoryDialog();
            return;
        }

        if (button.id == BTN_SMART_GENERATOR) {
            String category = isConcreteCategory(selectedCategory) ? selectedCategory : "";
            mc.setScreen(new GuiCapturedIdSmartGenerator(this, category));
            return;
        }

        if (button.id == 2016) {
            pendingEditorRestoreModel = buildModelFromEditor();
            pendingEditorRestoreCreateMode = creatingNew;
            pendingEditorRestoreCategory = selectedCategory;
            pendingEditorRestoreRuleName = getSelectedRuleName();
            mc.setScreen(new GuiSequenceSelector(this, selected -> {
                if (selected != null) {
                    if (pendingEditorRestoreModel == null) {
                        pendingEditorRestoreModel = buildModelFromEditor();
                    }
                    pendingEditorRestoreModel.updateSequenceName = selected;
                }
                mc.setScreen(this);
            }));
            return;
        }

        if (button.id == BTN_SAVE) {
            saveCurrentRule();
            return;
        }

        super.actionPerformed(button);
    }

    private void saveCurrentRule() {
        CapturedIdRuleManager.RuleEditModel model = buildModelFromEditor();
        if (creatingNew && isBlank(model.category) && isConcreteCategory(selectedCategory)) {
            model.category = selectedCategory;
        }

        if (!editingBuiltinSequenceOnly && (isBlank(model.name) || isBlank(model.pattern))) {
            setStatus("§c规则名称和匹配表达式不能为空", 0xFFFF8E8E);
            return;
        }

        boolean ok;
        if (creatingNew) {
            ok = CapturedIdRuleManager.addRule(model);
        } else {
            if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
                setStatus("§c请先选择一条规则，或右键卡片新增规则", 0xFFFF8E8E);
                return;
            }
            CapturedIdRuleManager.RuleCard card = visibleCards.get(selectedVisibleIndex);
            if (card.index < 0) {
                ok = CapturedIdRuleManager.updateBuiltinRuleSequenceSettings(card.model.name,
                        model.updateSequenceName, model.updateSequenceMode, model.updateSequenceCooldownMs);
            } else {
                ok = CapturedIdRuleManager.updateRule(card.index, model);
            }
        }

        if (!ok) {
            setStatus("§c保存失败，请检查规则配置", 0xFFFF8E8E);
            return;
        }

        setStatus("§a规则已保存", 0xFF8CFF9E);
        refreshData(true);
        selectByRuleName(model.name);
    }

    private void openAddCategoryDialog() {
        mc.setScreen(new GuiTextInput(this, "输入新分组名称", value -> {
            if (value == null || value.trim().isEmpty()) {
                setStatus("§7已取消创建分组", 0xFFB8C7D9);
                return;
            }
            boolean ok = CapturedIdRuleManager.addCategory(value.trim());
            if (ok) {
                selectedCategory = value.trim();
                setStatus("§a已创建分组: " + value.trim(), 0xFF8CFF9E);
                refreshData(true);
            } else {
                setStatus("§c创建分组失败，可能已存在或名称无效", 0xFFFF8E8E);
            }
        }));
    }

    private void createNewRule() {
        creatingNew = true;
        editingBuiltinSequenceOnly = false;
        selectedVisibleIndex = -1;
        clearEditorForNew();
        closeContextMenu();
        setStatus("§a已进入新建规则模式", 0xFF8CFF9E);
    }

    private void deleteSelectedRule() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            setStatus("§c请先右键一条规则卡片", 0xFFFF8E8E);
            return;
        }
        CapturedIdRuleManager.RuleCard card = visibleCards.get(selectedVisibleIndex);
        if (card.index < 0) {
            setStatus("§c内置规则不能删除", 0xFFFF8E8E);
            return;
        }
        if (CapturedIdRuleManager.deleteRule(card.index)) {
            setStatus("§a已删除规则: " + safe(card.model.name), 0xFF8CFF9E);
            refreshData(false);
        } else {
            setStatus("§c删除规则失败", 0xFFFF8E8E);
        }
    }

    private void reloadAllRules() {
        CapturedIdRuleManager.reloadRules();
        refreshData(false);
        setStatus("§a已重载捕获ID规则", 0xFF8CFF9E);
    }

    private void copySelectedValue() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }
        CapturedIdRuleManager.RuleCard card = visibleCards.get(selectedVisibleIndex);
        if (isBlank(card.capturedHex)) {
            setStatus("§c当前规则还没有捕获值", 0xFFFF8E8E);
            return;
        }
        setClipboardString(card.capturedHex);
        setStatus("§a已复制当前捕获值到剪贴板", 0xFF8CFF9E);
    }

    private void exportSelectedRule() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }
        CapturedIdRuleManager.RuleCard card = visibleCards.get(selectedVisibleIndex);
        try {
            String shareCode = CapturedIdRuleManager.exportRuleShareCode(card.model, card.capturedHex);
            setClipboardString(shareCode);
            setStatus("§a规则分享码已复制，长度: " + shareCode.length(), 0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c导出规则失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void importRuleToCurrentCategory() {
        final String fallbackCategory = isConcreteCategory(selectedCategory) ? selectedCategory : "";
        mc.setScreen(new GuiTextInput(this, "粘贴捕获规则分享码", value -> {
            if (value == null || value.trim().isEmpty()) {
                setStatus("§7已取消导入规则", 0xFFB8C7D9);
                return;
            }
            try {
                String importedRuleName = CapturedIdRuleManager.importRuleShareCode(value, fallbackCategory);
                setStatus("§a已导入规则: " + importedRuleName, 0xFF8CFF9E);
                refreshData(true);
                selectByRuleName(importedRuleName);
            } catch (Exception e) {
                setStatus("§c导入规则失败: " + e.getMessage(), 0xFFFF8E8E);
            }
        }));
    }

    private void moveSelectedRuleToCurrentCategory() {
        if (!isConcreteCategory(selectedCategory)) {
            setStatus("§c请先在左侧选择一个具体分组", 0xFFFF8E8E);
            return;
        }
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }

        CapturedIdRuleManager.RuleCard card = visibleCards.get(selectedVisibleIndex);
        if (card.index < 0) {
            setStatus("§c内置规则使用默认分组，不能直接移动", 0xFFFF8E8E);
            return;
        }

        if (CapturedIdRuleManager.moveCustomRuleToCategory(card.index, selectedCategory)) {
            setStatus("§a已移动规则到分组: " + selectedCategory, 0xFF8CFF9E);
            refreshData(true);
            selectByRuleName(card.model.name);
        } else {
            setStatus("§c移动规则失败", 0xFFFF8E8E);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (contextMenuVisible) {
            if (handleContextMenuClick(mouseX, mouseY, mouseButton)) {
                return;
            }
            closeContextMenu();
        }

        if (mouseButton == 0) {
            if (isInTreeCollapseButton(mouseX, mouseY)) {
                treeCollapsed = !treeCollapsed;
                recalcLayout();
                layoutEditorFields();
                return;
            }
            if (isInListCollapseButton(mouseX, mouseY)) {
                listCollapsed = !listCollapsed;
                recalcLayout();
                layoutEditorFields();
                return;
            }
        }

        if (handleDropdownClick(mouseX, mouseY, mouseButton)) {
            return;
        }

        if (!listCollapsed && mouseButton == 1 && isInCardList(mouseX, mouseY)) {
            int index = getCardIndexAt(mouseY);
            if (index >= 0 && index < visibleCards.size()) {
                selectedVisibleIndex = index;
                creatingNew = false;
                loadEditor(visibleCards.get(index).model);
                openContextMenu(mouseX, mouseY, index);
                return;
            }
            openContextMenu(mouseX, mouseY, -1);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        GuiTextField focusedField = null;
        for (GuiTextField field : visibleFields()) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
            if (mouseX >= field.x && mouseX < field.x + field.width
                    && mouseY >= field.y && mouseY < field.y + field.height) {
                focusedField = field;
            }
        }

        for (GuiTextField field : allFields()) {
            field.setFocused(field == focusedField);
        }

        collapseAllDropdowns();

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            if (mouseButton == 0) {
                handleTreeClick(mouseX, mouseY);
                return;
            }
            if (mouseButton == 1) {
                handleTreeRightClick(mouseX, mouseY);
                return;
            }
        }

        if (!listCollapsed && mouseButton == 0 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleCards.size()) {
                selectedVisibleIndex = actual;
                creatingNew = false;
                loadEditor(visibleCards.get(actual).model);
            }
            return;
        }
    }

    private void handleTreeClick(int mouseX, int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }

        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);

        if (row.type == TreeRow.TYPE_ALL) {
            selectedCategory = CATEGORY_ALL;
            rebuildVisibleCards();
            return;
        }

        if (row.type == TreeRow.TYPE_CATEGORY) {
            if (expandedCategories.contains(row.category)) {
                expandedCategories.remove(row.category);
            } else {
                expandedCategories.add(row.category);
            }
            selectedCategory = row.category;
            rebuildTreeRows();
            rebuildVisibleCards();
            return;
        }

        if (row.type == TreeRow.TYPE_RULE) {
            selectedCategory = row.category;
            rebuildVisibleCards();
            selectByRuleName(row.ruleName);
        }
    }

    private void handleTreeRightClick(int mouseX, int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }

        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (row.type == TreeRow.TYPE_RULE) {
            setStatus("§7左侧规则子项暂不支持独立菜单，请右键中间规则卡片", 0xFFB8C7D9);
            return;
        }

        if (row.type == TreeRow.TYPE_CATEGORY) {
            selectedCategory = safe(row.category);
            rebuildVisibleCards();
        }

        openTreeContextMenu(mouseX, mouseY, row);
    }

    private int getTreeRowIndexAt(int mouseY) {
        int contentY = treeY + 22;
        int localY = mouseY - contentY;
        if (localY < 0) {
            return -1;
        }
        int localIndex = localY / TREE_ROW_HEIGHT;
        int actualIndex = treeScrollOffset + localIndex;
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return -1;
        }
        return actualIndex;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (contextMenuVisible) {
                closeContextMenu();
                return;
            }
            if (isAnyDropdownExpanded()) {
                collapseAllDropdowns();
                return;
            }
        }

        if (keyCode == Keyboard.KEY_UP && selectedVisibleIndex > 0 && !visibleCards.isEmpty()) {
            selectedVisibleIndex--;
            ensureCardSelectionVisible();
            loadEditor(visibleCards.get(selectedVisibleIndex).model);
            return;
        }
        if (keyCode == Keyboard.KEY_DOWN && selectedVisibleIndex < visibleCards.size() - 1 && !visibleCards.isEmpty()) {
            selectedVisibleIndex++;
            ensureCardSelectionVisible();
            loadEditor(visibleCards.get(selectedVisibleIndex).model);
            return;
        }

        boolean consumed = false;
        for (GuiTextField field : allFields()) {
            if (field.isFocused() && field.textboxKeyTyped(typedChar, keyCode)) {
                consumed = true;
                break;
            }
        }
        if (!consumed) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;

        if (isInEditor(mouseX, mouseY)) {
            int max = getMaxEditorScroll();
            if (wheel < 0) {
                editorScrollOffset = Math.min(max, editorScrollOffset + 1);
            } else {
                editorScrollOffset = Math.max(0, editorScrollOffset - 1);
            }
            layoutEditorFields();
            return;
        }

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            int max = Math.max(0, visibleTreeRows.size() - getVisibleTreeRowCount());
            if (wheel < 0) {
                treeScrollOffset = Math.min(max, treeScrollOffset + 1);
            } else {
                treeScrollOffset = Math.max(0, treeScrollOffset - 1);
            }
            return;
        }

        if (!listCollapsed && isInCardList(mouseX, mouseY)) {
            int max = Math.max(0, visibleCards.size() - getVisibleCardCount());
            if (wheel < 0) {
                listScrollOffset = Math.min(max, listScrollOffset + 1);
            } else {
                listScrollOffset = Math.max(0, listScrollOffset - 1);
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : allFields()) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.packet.captured_id.title"),
                this.fontRenderer);

        GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 22, panelWidth - 20, 18, false, true);
        this.drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 27, statusColor);

        drawTreePanel(mouseX, mouseY);
        drawCardPanel(mouseX, mouseY);
        drawEditorPanel(mouseX, mouseY);
        drawHintPanel();

        EnumDropdown expandedDropdown = getExpandedDropdown();
        if (expandedDropdown != null) {
            expandedDropdown.drawExpanded(mouseX, mouseY);
        }

        if (contextMenuVisible) {
            drawContextMenu(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCapturedIdTooltip(mouseX, mouseY);
    }

    private void drawTreePanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(treeX, treeY, treeWidth, treeHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(treeX + 8, treeY + 8, treeCollapsed ? "分组" : "规则分组树（默认折叠）", this.fontRenderer);
        drawCollapseButton(treeX, treeY, treeWidth, treeCollapsed);

        if (treeCollapsed) {
            return;
        }

        int contentY = treeY + 22;
        int visibleRows = getVisibleTreeRowCount();

        if (visibleTreeRows.isEmpty()) {
            GuiTheme.drawEmptyState(treeX + treeWidth / 2, treeY + treeHeight / 2, "暂无分组", this.fontRenderer);
            return;
        }

        int max = Math.max(0, visibleTreeRows.size() - visibleRows);
        treeScrollOffset = Math.max(0, Math.min(treeScrollOffset, max));

        for (int i = 0; i < visibleRows; i++) {
            int index = treeScrollOffset + i;
            if (index >= visibleTreeRows.size()) {
                break;
            }

            TreeRow row = visibleTreeRows.get(index);
            int rowY = contentY + i * TREE_ROW_HEIGHT;
            boolean selected = index == selectedTreeIndex
                    || (row.type == TreeRow.TYPE_CATEGORY && row.category.equals(selectedCategory))
                    || (row.type == TreeRow.TYPE_ALL && CATEGORY_ALL.equals(selectedCategory));
            boolean hovered = mouseX >= treeX + 6 && mouseX <= treeX + treeWidth - 10
                    && mouseY >= rowY && mouseY <= rowY + TREE_ROW_HEIGHT - 2;

            GuiTheme.drawButtonFrameSafe(treeX + 6, rowY, treeWidth - 14, TREE_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            int textX = treeX + 10 + row.indent * 12;
            int maxTextWidth = Math.max(36, treeWidth - (textX - treeX) - 18);
            if (row.type == TreeRow.TYPE_CATEGORY) {
                String arrow = expandedCategories.contains(row.category) ? "▼" : "▶";
                this.drawString(this.fontRenderer, arrow, textX, rowY + 5, 0xFF9FDFFF);
                this.drawString(this.fontRenderer, trimToWidth(row.label, maxTextWidth - 12), textX + 10, rowY + 5,
                        0xFFE8F1FA);
            } else if (row.type == TreeRow.TYPE_RULE) {
                this.drawString(this.fontRenderer, "•", textX, rowY + 5, 0xFFB8C7D9);
                this.drawString(this.fontRenderer, trimToWidth(row.label, maxTextWidth - 8), textX + 8, rowY + 5,
                        0xFFD9E6F2);
            } else {
                this.drawString(this.fontRenderer, trimToWidth(row.label, maxTextWidth), textX, rowY + 5, 0xFFE8F1FA);
            }
        }

        if (visibleTreeRows.size() > visibleRows) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, visibleTreeRows.size())) * (treeHeight - 28)));
            int track = Math.max(1, (treeHeight - 28) - thumbHeight);
            int thumbY = contentY + (int) ((treeScrollOffset / (float) Math.max(1, max)) * track);
            GuiTheme.drawScrollbar(treeX + treeWidth - 8, contentY, 4, treeHeight - 28, thumbY, thumbHeight);
        }
    }

    private void drawCardPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(listX, listY, listWidth, listHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(listX + 8, listY + 8, listCollapsed ? "卡片" : "规则卡片（右键打开菜单）", this.fontRenderer);
        drawCollapseButton(listX, listY, listWidth, listCollapsed);

        if (listCollapsed) {
            return;
        }

        int contentY = listY + 22;
        int visible = getVisibleCardCount();
        int max = Math.max(0, visibleCards.size() - visible);
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));

        if (visibleCards.isEmpty()) {
            GuiTheme.drawEmptyState(listX + listWidth / 2, listY + listHeight / 2, "该分组下暂无规则", this.fontRenderer);
            return;
        }

        for (int i = 0; i < visible; i++) {
            int actual = listScrollOffset + i;
            if (actual >= visibleCards.size()) {
                break;
            }

            int cardTop = contentY + i * (CARD_HEIGHT + CARD_GAP);
            int cardBottom = cardTop + CARD_HEIGHT;
            boolean selected = actual == selectedVisibleIndex;
            boolean hovered = mouseX >= listX + 2 && mouseX <= listX + listWidth - 2
                    && mouseY >= cardTop && mouseY <= cardBottom;

            int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
            int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

            drawRect(listX + 2, cardTop, listX + listWidth - 2, cardBottom, bg);
            drawHorizontalLine(listX + 2, listX + listWidth - 2, cardTop, border);
            drawHorizontalLine(listX + 2, listX + listWidth - 2, cardBottom, border);
            drawVerticalLine(listX + 2, cardTop, cardBottom, border);
            drawVerticalLine(listX + listWidth - 2, cardTop, cardBottom, border);

            CapturedIdRuleManager.RuleCard card = visibleCards.get(actual);
            String title = safe(card.model.displayName);
            if (isBlank(title)) {
                title = safe(card.model.name);
            }
            int textMaxWidth = Math.max(72, listWidth - 18);
            String value = isBlank(card.capturedHex) ? "未捕获" : safe(card.capturedHex);
            int valueColor = isBlank(card.capturedHex) ? 0xFFAAAAAA : 0xFF77FF77;

            drawString(fontRenderer, trimToWidth("#" + (actual + 1) + " " + title, textMaxWidth), listX + 8,
                    cardTop + 5, 0xFFFFFFFF);
            drawString(fontRenderer, trimToWidth("name: " + safe(card.model.name), textMaxWidth), listX + 8,
                    cardTop + 18, 0xFFDDDDDD);
            drawString(fontRenderer, trimToWidth("分类: " + getCardCategory(card), textMaxWidth), listX + 8,
                    cardTop + 30, 0xFFBDBDBD);
            drawString(fontRenderer, trimToWidth("值: " + value, textMaxWidth), listX + 8, cardTop + 42, valueColor);
        }

        if (visibleCards.size() > visible) {
            int thumbHeight = Math.max(18,
                    (int) ((visible / (float) Math.max(visible, visibleCards.size())) * (listHeight - 28)));
            int track = Math.max(1, (listHeight - 28) - thumbHeight);
            int thumbY = contentY + (int) ((listScrollOffset / (float) Math.max(1, max)) * track);
            GuiTheme.drawScrollbar(listX + listWidth - 8, contentY, 4, listHeight - 28, thumbY, thumbHeight);
        }
    }

    private void drawEditorPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(editorBoxX, editorBoxY, editorBoxWidth, editorBoxHeight, panelX, panelY, panelWidth,
                panelHeight);
        GuiTheme.drawSectionTitle(editorBoxX + 8, editorBoxY + 8,
                creatingNew ? "规则编辑器 - 新建" : "规则编辑器 - 编辑", this.fontRenderer);

        layoutEditorFields();

        int visibleStart = editorScrollOffset;
        int visibleEnd = Math.min(getEffectiveEditorTotalRows(), visibleStart + editorVisibleRows);
        for (int row = visibleStart; row < visibleEnd; row++) {
            int ly = editorRowStartY + (row - visibleStart) * EDITOR_ROW_HEIGHT;
            drawString(fontRenderer, getRowLabel(row), editorLabelX, ly + 4, 0xFFFFFFFF);
        }

        for (GuiTextField field : visibleFields()) {
            drawThemedTextField(field);
        }

        enabledDropdown.drawMain(mouseX, mouseY, "启用");
        directionDropdown.drawMain(mouseX, mouseY, "方向");
        targetDropdown.drawMain(mouseX, mouseY, "匹配目标");
        valueTypeDropdown.drawMain(mouseX, mouseY, "值类型");
        updateSequenceModeDropdown.drawMain(mouseX, mouseY, "序列触发模式");

        if (getEffectiveEditorTotalRows() > editorVisibleRows) {
            int thumbHeight = Math.max(18, (int) ((editorVisibleRows / (float) getEffectiveEditorTotalRows())
                    * editorBoxHeight));
            int track = Math.max(1, editorBoxHeight - thumbHeight - 10);
            int thumbY = editorBoxY + 6 + (int) ((editorScrollOffset / (float) Math.max(1, getMaxEditorScroll())) * track);
            GuiTheme.drawScrollbar(editorBoxX + editorBoxWidth - 8, editorBoxY + 6, 4, editorBoxHeight - 12, thumbY,
                    thumbHeight);
        }
    }

    private void drawHintPanel() {
        drawRect(hintBoxX, hintBoxY, hintBoxX + hintBoxWidth, hintBoxY + hintBoxHeight, 0x66161F2B);
        drawString(fontRenderer, "使用说明", hintBoxX + 6, hintBoxY + 4, 0xFFB0D8FF);
        drawString(fontRenderer, "1. 左侧点击分组可筛选，点击 ▶/▼ 可展开或折叠子项", hintBoxX + 6, hintBoxY + 18,
                0xFFBDBDBD);
        drawString(fontRenderer, "2. 右键规则卡片可新增/删除/重载/复制值/导出分享/导入规则", hintBoxX + 6, hintBoxY + 31,
                0xFF9FC3D9);
        drawString(fontRenderer, "3. 内置规则默认分到“再生之路/魔塔之巅”，自定义规则可保存到任意分组", hintBoxX + 6,
                hintBoxY + 44, 0xFF9FC3D9);
    }

    private void drawCapturedIdTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (isMouseOverButton(mouseX, mouseY, selectSequenceButton)) {
            tooltip = "打开序列选择器。\n用于指定这条捕获规则在更新后要自动触发的路径序列。";
        } else if (isMouseOverField(mouseX, mouseY, nameField)) {
            tooltip = "规则唯一名称。\n其它动作通过这个名字读取捕获值。";
        } else if (isMouseOverField(mouseX, mouseY, displayNameField)) {
            tooltip = "显示名称。\n只影响界面展示，便于给规则起更易懂的中文名。";
        } else if (isMouseOverField(mouseX, mouseY, noteField)) {
            tooltip = "备注说明，记录规则用途、来源或注意事项。";
        } else if (isMouseOverField(mouseX, mouseY, aliasesField)) {
            tooltip = "别名列表，多个用逗号分隔。\n旧脚本或其它模块可以通过别名访问同一个捕获值。";
        } else if (isMouseOverField(mouseX, mouseY, channelField)) {
            tooltip = "频道过滤。\n通常填 FML / 自定义频道名，留空表示不限制频道。";
        } else if (isMouseOverField(mouseX, mouseY, patternField)) {
            tooltip = "匹配表达式。\n用于从 HEX 或解码文本里提取目标值，通常填写正则。";
        } else if (isMouseOverField(mouseX, mouseY, offsetField)) {
            tooltip = "偏移修正。\n可在捕获到的 HEX 值基础上再做偏移调整。";
        } else if (isMouseOverField(mouseX, mouseY, groupField)) {
            tooltip = "捕获组号。\n当使用正则时，表示取第几个分组作为最终值。";
        } else if (isMouseOverField(mouseX, mouseY, categoryField)) {
            tooltip = "规则所属分组。\n左侧树和卡片区会按这个字段归类显示。";
        } else if (isMouseOverField(mouseX, mouseY, byteLengthField)) {
            tooltip = "目标字节长度。\n提取十进制整型或自动转换时会按这个长度输出字节。";
        } else if (isMouseOverField(mouseX, mouseY, updateSequenceField)) {
            tooltip = "更新后触发的序列名。\n留空表示只捕获值，不自动执行任何路径序列。";
        } else if (isHoverRegion(mouseX, mouseY, enabledDropdown.x, enabledDropdown.y, enabledDropdown.width, enabledDropdown.height)) {
            tooltip = "启用开关。\n关闭后该规则保留，但不会再参与数据包捕获。";
        } else if (isHoverRegion(mouseX, mouseY, directionDropdown.x, directionDropdown.y, directionDropdown.width, directionDropdown.height)) {
            tooltip = "方向过滤。\n可选择只处理入站、出站，或双向都处理。";
        } else if (isHoverRegion(mouseX, mouseY, targetDropdown.x, targetDropdown.y, targetDropdown.width, targetDropdown.height)) {
            tooltip = "匹配目标。\n决定规则是在原始 HEX 上匹配，还是在解码文本上匹配。";
        } else if (isHoverRegion(mouseX, mouseY, valueTypeDropdown.x, valueTypeDropdown.y, valueTypeDropdown.width, valueTypeDropdown.height)) {
            tooltip = "值类型。\n决定捕获结果按 HEX 还是十进制整型等方式解释。";
        } else if (isHoverRegion(mouseX, mouseY, updateSequenceModeDropdown.x, updateSequenceModeDropdown.y, updateSequenceModeDropdown.width, updateSequenceModeDropdown.height)) {
            tooltip = "序列触发模式。\n控制更新后每次触发、仅首次触发、冷却触发或重新捕获触发。";
        } else if (mouseX >= treeX && mouseX <= treeX + treeWidth && mouseY >= treeY && mouseY <= treeY + treeHeight) {
            tooltip = "左侧规则分组树。\n点击分组筛选，右键分组可管理分类。";
        } else if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            tooltip = "中间规则卡片区。\n左键选中规则，右键打开完整菜单。";
        } else if (mouseX >= editorBoxX && mouseX <= editorBoxX + editorBoxWidth && mouseY >= editorBoxY
                && mouseY <= editorBoxY + editorBoxHeight) {
            tooltip = "右侧规则编辑器。\n修改字段后点击底部“保存”才能写入配置文件。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case BTN_NEW_CATEGORY:
                        tooltip = "新建一个捕获ID分组。";
                        break;
                    case BTN_SMART_GENERATOR:
                        tooltip = "打开智能生成器。\n通过多组 HEX 样本自动分析变化区段并生成捕获规则。";
                        break;
                    case BTN_SAVE:
                        tooltip = "保存当前规则编辑器内容。";
                        break;
                    case BTN_DONE:
                        tooltip = "关闭捕获ID规则管理器并返回上一页。";
                        break;
                    case 2016:
                        tooltip = "选择更新后要自动触发的路径序列。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private void drawContextMenu(int mouseX, int mouseY) {
        int height = contextMenuItems.size() * contextMenuItemHeight + 4;
        int x = Math.min(contextMenuX, this.width - contextMenuWidth - 6);
        int y = Math.min(contextMenuY, this.height - height - 6);

        drawRect(x, y, x + contextMenuWidth, y + height, 0xEE111A22);
        drawHorizontalLine(x, x + contextMenuWidth, y, 0xFF6FB8FF);
        drawHorizontalLine(x, x + contextMenuWidth, y + height, 0xFF35536C);
        drawVerticalLine(x, y, y + height, 0xFF35536C);
        drawVerticalLine(x + contextMenuWidth, y, y + height, 0xFF35536C);

        for (int i = 0; i < contextMenuItems.size(); i++) {
            ContextMenuItem item = contextMenuItems.get(i);
            int itemY = y + 2 + i * contextMenuItemHeight;
            boolean hovered = mouseX >= x + 2 && mouseX <= x + contextMenuWidth - 2
                    && mouseY >= itemY && mouseY <= itemY + contextMenuItemHeight - 1;
            int bg = hovered ? 0xCC2B5A7C : 0x00000000;
            if (bg != 0) {
                drawRect(x + 2, itemY, x + contextMenuWidth - 2, itemY + contextMenuItemHeight - 1, bg);
            }
            int color = item.enabled ? 0xFFFFFFFF : 0xFF777777;
            drawString(fontRenderer, item.label, x + 8, itemY + 5, color);
        }
    }

    private boolean handleContextMenuClick(int mouseX, int mouseY, int mouseButton) {
        if (!contextMenuVisible) {
            return false;
        }

        int height = contextMenuItems.size() * contextMenuItemHeight + 4;
        int x = Math.min(contextMenuX, this.width - contextMenuWidth - 6);
        int y = Math.min(contextMenuY, this.height - height - 6);

        if (mouseX < x || mouseX > x + contextMenuWidth || mouseY < y || mouseY > y + height) {
            return false;
        }

        if (mouseButton != 0) {
            closeContextMenu();
            return true;
        }

        int local = mouseY - (y + 2);
        int index = local / contextMenuItemHeight;
        if (index < 0 || index >= contextMenuItems.size()) {
            closeContextMenu();
            return true;
        }

        ContextMenuItem item = contextMenuItems.get(index);
        if (!item.enabled) {
            closeContextMenu();
            return true;
        }

        int previousTargetIndex = contextMenuTargetIndex;
        String targetType = contextMenuTargetType;
        String targetCategory = contextMenuTargetCategory;
        selectedVisibleIndex = contextMenuTargetIndex;
        closeContextMenu();

        if ("tree".equals(targetType)) {
            handleTreeContextMenuAction(item.key, targetCategory);
            return true;
        }

        selectedVisibleIndex = previousTargetIndex;

        if ("new".equals(item.key)) {
            createNewRule();
        } else if ("delete".equals(item.key)) {
            deleteSelectedRule();
        } else if ("reload".equals(item.key)) {
            reloadAllRules();
        } else if ("copy".equals(item.key)) {
            copySelectedValue();
        } else if ("export".equals(item.key)) {
            exportSelectedRule();
        } else if ("import".equals(item.key)) {
            importRuleToCurrentCategory();
        } else if ("move".equals(item.key)) {
            moveSelectedRuleToCurrentCategory();
        }

        return true;
    }

    private void openContextMenu(int mouseX, int mouseY, int targetIndex) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetIndex = targetIndex;
        contextMenuTargetType = "card";
        contextMenuTargetCategory = "";
        contextMenuItems.clear();

        CapturedIdRuleManager.RuleCard card = null;
        if (targetIndex >= 0 && targetIndex < visibleCards.size()) {
            card = visibleCards.get(targetIndex);
        }

        contextMenuItems.add(new ContextMenuItem("new", "新增规则", true));
        contextMenuItems.add(new ContextMenuItem("reload", "重载规则", true));
        contextMenuItems.add(new ContextMenuItem("import", "导入规则", true));

        if (card != null) {
            contextMenuItems.add(new ContextMenuItem("copy", "复制当前值", !isBlank(card.capturedHex)));
            contextMenuItems.add(new ContextMenuItem("export", "导出选择卡片值", true));
            contextMenuItems.add(new ContextMenuItem("delete", "删除规则", card.index >= 0));
            boolean canMove = card.index >= 0 && isConcreteCategory(selectedCategory)
                    && !selectedCategory.equalsIgnoreCase(getCardCategory(card));
            contextMenuItems.add(new ContextMenuItem("move", "移动到当前分组", canMove));
        }
    }

    private void openTreeContextMenu(int mouseX, int mouseY, TreeRow row) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetIndex = -1;
        contextMenuTargetType = "tree";
        contextMenuTargetCategory = row == null ? "" : safe(row.category);
        contextMenuItems.clear();

        boolean isAllRow = row != null && row.type == TreeRow.TYPE_ALL;
        boolean isCategoryRow = row != null && row.type == TreeRow.TYPE_CATEGORY;
        boolean concreteCategory = isConcreteCategory(contextMenuTargetCategory);

        contextMenuItems.add(new ContextMenuItem("tree_new", "新建分组", true));
        contextMenuItems.add(new ContextMenuItem("tree_paste", "从剪贴板新建分组", true));

        if (isCategoryRow) {
            contextMenuItems.add(new ContextMenuItem("tree_copy_name", "复制分组名", true));
            contextMenuItems.add(new ContextMenuItem("tree_rename", "重命名分组", concreteCategory));
            contextMenuItems.add(new ContextMenuItem("tree_delete", "删除分组", concreteCategory));
        } else if (isAllRow) {
            contextMenuItems.add(new ContextMenuItem("tree_reload", "重载规则", true));
        }
    }

    private void closeContextMenu() {
        contextMenuVisible = false;
        contextMenuItems.clear();
        contextMenuTargetIndex = -1;
        contextMenuTargetType = "card";
        contextMenuTargetCategory = "";
    }

    private void handleTreeContextMenuAction(String key, String targetCategory) {
        if ("tree_new".equals(key)) {
            openAddCategoryDialog();
            return;
        }
        if ("tree_paste".equals(key)) {
            pasteCategoryFromClipboard();
            return;
        }
        if ("tree_copy_name".equals(key)) {
            copyCategoryName(targetCategory);
            return;
        }
        if ("tree_rename".equals(key)) {
            openRenameCategoryDialog(targetCategory);
            return;
        }
        if ("tree_delete".equals(key)) {
            deleteCategoryByName(targetCategory);
            return;
        }
        if ("tree_reload".equals(key)) {
            reloadAllRules();
        }
    }

    private void openRenameCategoryDialog(String category) {
        final String normalized = safe(category).trim();
        if (!isConcreteCategory(normalized)) {
            setStatus("§c该分组不能重命名", 0xFFFF8E8E);
            return;
        }
        mc.setScreen(new GuiTextInput(this, "重命名分组", normalized, value -> {
            String newName = value == null ? "" : value.trim();
            if (newName.isEmpty()) {
                setStatus("§7已取消重命名分组", 0xFFB8C7D9);
                return;
            }
            boolean ok = CapturedIdRuleManager.renameCategory(normalized, newName);
            if (ok) {
                expandedCategories.remove(normalized);
                expandedCategories.add(newName);
                selectedCategory = newName;
                refreshData(true);
                setStatus("§a已重命名分组: " + normalized + " -> " + newName, 0xFF8CFF9E);
            } else {
                setStatus("§c重命名分组失败，名称可能重复或无效", 0xFFFF8E8E);
            }
        }));
    }

    private void copyCategoryName(String category) {
        String normalized = safe(category).trim();
        if (normalized.isEmpty()) {
            setStatus("§c没有可复制的分组名", 0xFFFF8E8E);
            return;
        }
        setClipboardString(normalized);
        setStatus("§a已复制分组名: " + normalized, 0xFF8CFF9E);
    }

    private void pasteCategoryFromClipboard() {
        String text = getClipboardString();
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            setStatus("§c剪贴板为空，无法新建分组", 0xFFFF8E8E);
            return;
        }
        boolean ok = CapturedIdRuleManager.addCategory(normalized);
        if (ok) {
            selectedCategory = normalized;
            expandedCategories.add(normalized);
            refreshData(true);
            setStatus("§a已从剪贴板新建分组: " + normalized, 0xFF8CFF9E);
        } else {
            setStatus("§c新建分组失败，名称可能重复或无效", 0xFFFF8E8E);
        }
    }

    private void deleteCategoryByName(String category) {
        String normalized = safe(category).trim();
        if (!isConcreteCategory(normalized)) {
            setStatus("§c该分组不能删除", 0xFFFF8E8E);
            return;
        }
        if (CapturedIdRuleManager.deleteCategory(normalized)) {
            expandedCategories.remove(normalized);
            selectedCategory = CATEGORY_ALL;
            refreshData(false);
            setStatus("§a已删除分组: " + normalized + "（原分组规则已转为未分组）", 0xFF8CFF9E);
        } else {
            setStatus("§c删除分组失败", 0xFFFF8E8E);
        }
    }

    private void refreshData(boolean keepSelection) {
        String keepRuleName = keepSelection ? getSelectedRuleName() : "";
        allCards.clear();
        allCards.addAll(CapturedIdRuleManager.getRuleCards());

        categories.clear();
        categories.addAll(CapturedIdRuleManager.getAllCategories());

        rebuildTreeRows();
        rebuildVisibleCards();

        if (!isBlank(keepRuleName)) {
            if (selectByRuleName(keepRuleName)) {
                return;
            }
        }

        if (visibleCards.isEmpty()) {
            selectedVisibleIndex = -1;
            creatingNew = true;
            editingBuiltinSequenceOnly = false;
            clearEditorForNew();
            return;
        }

        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            selectedVisibleIndex = 0;
        }
        creatingNew = false;
        loadEditor(visibleCards.get(selectedVisibleIndex).model);
    }

    private void rebuildTreeRows() {
        visibleTreeRows.clear();
        visibleTreeRows.add(TreeRow.allRow("全部规则"));
        for (String category : categories) {
            visibleTreeRows.add(TreeRow.categoryRow(category));
            if (expandedCategories.contains(category)) {
                for (CapturedIdRuleManager.RuleCard card : allCards) {
                    if (category.equalsIgnoreCase(getCardCategory(card))) {
                        visibleTreeRows.add(TreeRow.ruleRow(category, safe(card.model.name),
                                isBlank(card.model.displayName) ? safe(card.model.name) : safe(card.model.displayName)));
                    }
                }
            }
        }
        selectedTreeIndex = Math.max(0, Math.min(selectedTreeIndex, Math.max(0, visibleTreeRows.size() - 1)));
    }

    private void rebuildVisibleCards() {
        visibleCards.clear();
        for (CapturedIdRuleManager.RuleCard card : allCards) {
            if (CATEGORY_ALL.equals(selectedCategory)) {
                visibleCards.add(card);
            } else if (selectedCategory.equalsIgnoreCase(getCardCategory(card))) {
                visibleCards.add(card);
            }
        }

        int max = Math.max(0, visibleCards.size() - getVisibleCardCount());
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));
        if (selectedVisibleIndex >= visibleCards.size()) {
            selectedVisibleIndex = visibleCards.isEmpty() ? -1 : 0;
        }
    }

    private boolean selectByRuleName(String name) {
        if (isBlank(name)) {
            return false;
        }

        for (CapturedIdRuleManager.RuleCard card : allCards) {
            if (name.equalsIgnoreCase(safe(card.model.name))) {
                selectedCategory = getCardCategory(card);
                rebuildTreeRows();
                rebuildVisibleCards();

                for (int i = 0; i < visibleCards.size(); i++) {
                    if (name.equalsIgnoreCase(safe(visibleCards.get(i).model.name))) {
                        selectedVisibleIndex = i;
                        creatingNew = false;
                        loadEditor(visibleCards.get(i).model);
                        ensureCardSelectionVisible();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void restorePendingEditorStateIfNeeded() {
        if (pendingEditorRestoreModel == null) {
            return;
        }

        CapturedIdRuleManager.RuleEditModel restoreModel = pendingEditorRestoreModel;
        boolean restoreCreateMode = pendingEditorRestoreCreateMode;
        String restoreCategory = pendingEditorRestoreCategory;
        String restoreRuleName = pendingEditorRestoreRuleName;

        pendingEditorRestoreModel = null;
        pendingEditorRestoreCreateMode = false;
        pendingEditorRestoreCategory = CATEGORY_ALL;
        pendingEditorRestoreRuleName = "";

        selectedCategory = isBlank(restoreCategory) ? CATEGORY_ALL : restoreCategory;
        rebuildTreeRows();
        rebuildVisibleCards();

        creatingNew = restoreCreateMode;
        if (creatingNew) {
            setBuiltinFieldEditability(true);
            loadEditor(restoreModel);
            return;
        }

        if (!isBlank(restoreRuleName) && selectByRuleName(restoreRuleName)) {
            loadEditor(restoreModel);
            return;
        }

        loadEditor(restoreModel);
    }

    private void clearEditorForNew() {
        setBuiltinFieldEditability(true);
        setText(nameField, "");
        setText(displayNameField, "");
        setText(noteField, "");
        setText(aliasesField, "");
        setText(enabledField, "true");
        setText(channelField, "");
        setText(directionField, "both");
        setText(targetField, "hex");
        setText(patternField, "");
        setText(offsetField, "");
        setText(groupField, "1");
        setText(categoryField, isConcreteCategory(selectedCategory) ? selectedCategory : "");
        setText(byteLengthField, "4");
        setText(updateSequenceField, "");
        setText(updateSequenceModeField, "always");
        setText(updateSequenceCooldownField, "1000");

        enabledDropdown.setValue("true");
        directionDropdown.setValue("both");
        targetDropdown.setValue("hex");
        valueTypeDropdown.setValue("hex");
        updateSequenceModeDropdown.setValue("always");

        syncEnumFieldsFromDropdowns();
    }

    private void loadEditor(CapturedIdRuleManager.RuleEditModel model) {
        if (model == null) {
            clearEditorForNew();
            return;
        }

        setText(nameField, safe(model.name));
        setText(displayNameField, safe(model.displayName));
        setText(noteField, safe(model.note));
        setText(aliasesField, safe(model.aliasesCsv));
        setText(channelField, safe(model.channel));
        setText(patternField, safe(model.pattern));
        setText(offsetField, safe(model.offset));
        setText(groupField, String.valueOf(model.group));
        setText(categoryField, safe(model.category));
        setText(byteLengthField, String.valueOf(model.byteLength));
        setText(updateSequenceField, safe(model.updateSequenceName));
        setText(updateSequenceCooldownField, String.valueOf(model.updateSequenceCooldownMs));

        enabledDropdown.setValue(model.enabled ? "true" : "false");
        directionDropdown.setValue(safe(model.direction));
        targetDropdown.setValue(safe(model.target));
        valueTypeDropdown.setValue(safe(model.valueType));
        updateSequenceModeDropdown.setValue(safe(model.updateSequenceMode));

        syncEnumFieldsFromDropdowns();
        updateBuiltinEditorState(model);
    }

    private void updateBuiltinEditorState(CapturedIdRuleManager.RuleEditModel model) {
        editingBuiltinSequenceOnly = false;
        if (creatingNew || selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            setBuiltinFieldEditability(true);
            return;
        }

        CapturedIdRuleManager.RuleCard card = visibleCards.get(selectedVisibleIndex);
        boolean builtin = card.index < 0;
        editingBuiltinSequenceOnly = builtin;
        setBuiltinFieldEditability(!builtin);

        if (builtin) {
            setText(nameField, safe(card.model.name));
            setText(displayNameField, safe(card.model.displayName));
            setText(noteField, safe(card.model.note));
            setText(aliasesField, safe(card.model.aliasesCsv));
            setText(channelField, safe(card.model.channel));
            setText(patternField, safe(card.model.pattern));
            setText(offsetField, safe(card.model.offset));
            setText(groupField, String.valueOf(card.model.group));
            setText(categoryField, safe(card.model.category));
            setText(byteLengthField, String.valueOf(card.model.byteLength));
            enabledDropdown.setValue(card.model.enabled ? "true" : "false");
            directionDropdown.setValue(safe(card.model.direction));
            targetDropdown.setValue(safe(card.model.target));
            valueTypeDropdown.setValue(safe(card.model.valueType));
            syncEnumFieldsFromDropdowns();
        }
    }

    private void setBuiltinFieldEditability(boolean editable) {
        nameField.setEnabled(editable);
        displayNameField.setEnabled(editable);
        noteField.setEnabled(editable);
        aliasesField.setEnabled(editable);
        channelField.setEnabled(editable);
        patternField.setEnabled(editable);
        offsetField.setEnabled(editable);
        groupField.setEnabled(editable);
        categoryField.setEnabled(editable);
        byteLengthField.setEnabled(editable);

        enabledField.setEnabled(false);
        directionField.setEnabled(false);
        targetField.setEnabled(false);
        valueTypeField.setEnabled(false);
        updateSequenceModeField.setEnabled(false);
    }

    private CapturedIdRuleManager.RuleEditModel buildModelFromEditor() {
        CapturedIdRuleManager.RuleEditModel model = new CapturedIdRuleManager.RuleEditModel();
        model.name = nameField.getText();
        model.displayName = displayNameField.getText();
        model.note = noteField.getText();
        model.aliasesCsv = aliasesField.getText();
        model.enabled = !"false".equalsIgnoreCase(enabledDropdown.getValue());
        model.channel = channelField.getText();
        model.direction = directionDropdown.getValue();
        model.target = targetDropdown.getValue();
        model.pattern = patternField.getText();
        model.offset = offsetField.getText();
        model.group = parseInt(groupField.getText(), 1);
        model.category = categoryField.getText();
        model.valueType = valueTypeDropdown.getValue();
        model.byteLength = parseInt(byteLengthField.getText(), 4);
        model.updateSequenceName = updateSequenceField.getText();
        model.updateSequenceMode = updateSequenceModeDropdown.getValue();
        model.updateSequenceCooldownMs = parseInt(updateSequenceCooldownField.getText(), 1000);
        return model;
    }

    private GuiTextField createField(int id, int x, int y, int w, int h) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, w, h);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private List<GuiTextField> allFields() {
        List<GuiTextField> list = new ArrayList<>();
        list.add(nameField);
        list.add(displayNameField);
        list.add(noteField);
        list.add(aliasesField);
        list.add(channelField);
        list.add(patternField);
        list.add(offsetField);
        list.add(groupField);
        list.add(categoryField);
        list.add(byteLengthField);
        list.add(updateSequenceField);
        list.add(updateSequenceCooldownField);
        return list;
    }

    private List<GuiTextField> visibleFields() {
        List<GuiTextField> result = new ArrayList<>();
        int start = editorScrollOffset;
        int end = Math.min(getEffectiveEditorTotalRows(), start + editorVisibleRows);
        for (int row = start; row < end; row++) {
            GuiTextField field = getFieldByRow(row);
            if (field != null) {
                result.add(field);
            }
        }
        return result;
    }

    private GuiTextField getFieldByRow(int row) {
        switch (row) {
            case 0:
                return nameField;
            case 1:
                return displayNameField;
            case 2:
                return noteField;
            case 3:
                return aliasesField;
            case 4:
                return enabledField;
            case 5:
                return channelField;
            case 6:
                return directionField;
            case 7:
                return targetField;
            case 8:
                return patternField;
            case 9:
                return offsetField;
            case 10:
                return groupField;
            case 11:
                return categoryField;
            case 12:
                return valueTypeField;
            case 13:
                return byteLengthField;
            case 14:
                return updateSequenceField;
            case 15:
                return updateSequenceModeField;
            case 16:
                return isUpdateSequenceCooldownVisible() ? updateSequenceCooldownField : null;
            default:
                return null;
        }
    }

    private boolean handleDropdownClick(int mouseX, int mouseY, int mouseButton) {
        if (editingBuiltinSequenceOnly) {
            if (updateSequenceModeDropdown.handleClick(mouseX, mouseY, mouseButton)) {
                syncEnumFieldsFromDropdowns();
                collapseOtherDropdowns(updateSequenceModeDropdown);
                return true;
            }
            return false;
        }

        if (enabledDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            syncEnumFieldsFromDropdowns();
            collapseOtherDropdowns(enabledDropdown);
            return true;
        }
        if (directionDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            syncEnumFieldsFromDropdowns();
            collapseOtherDropdowns(directionDropdown);
            return true;
        }
        if (targetDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            syncEnumFieldsFromDropdowns();
            collapseOtherDropdowns(targetDropdown);
            return true;
        }
        if (valueTypeDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            syncEnumFieldsFromDropdowns();
            collapseOtherDropdowns(valueTypeDropdown);
            return true;
        }
        if (updateSequenceModeDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            syncEnumFieldsFromDropdowns();
            collapseOtherDropdowns(updateSequenceModeDropdown);
            return true;
        }
        return false;
    }

    private void syncEnumFieldsFromDropdowns() {
        setText(enabledField, enabledDropdown.getValue());
        setText(directionField, directionDropdown.getValue());
        setText(targetField, targetDropdown.getValue());
        setText(valueTypeField, valueTypeDropdown.getValue());
        setText(updateSequenceModeField, updateSequenceModeDropdown.getValue());
    }

    private void collapseOtherDropdowns(EnumDropdown keep) {
        if (enabledDropdown != keep) {
            enabledDropdown.collapse();
        }
        if (directionDropdown != keep) {
            directionDropdown.collapse();
        }
        if (targetDropdown != keep) {
            targetDropdown.collapse();
        }
        if (valueTypeDropdown != keep) {
            valueTypeDropdown.collapse();
        }
        if (updateSequenceModeDropdown != keep) {
            updateSequenceModeDropdown.collapse();
        }
    }

    private boolean isAnyDropdownExpanded() {
        return enabledDropdown.isExpanded() || directionDropdown.isExpanded()
                || targetDropdown.isExpanded() || valueTypeDropdown.isExpanded()
                || updateSequenceModeDropdown.isExpanded();
    }

    private void collapseAllDropdowns() {
        enabledDropdown.collapse();
        directionDropdown.collapse();
        targetDropdown.collapse();
        valueTypeDropdown.collapse();
        updateSequenceModeDropdown.collapse();
    }

    private EnumDropdown getExpandedDropdown() {
        if (enabledDropdown.isExpanded()) {
            return enabledDropdown;
        }
        if (directionDropdown.isExpanded()) {
            return directionDropdown;
        }
        if (targetDropdown.isExpanded()) {
            return targetDropdown;
        }
        if (valueTypeDropdown.isExpanded()) {
            return valueTypeDropdown;
        }
        if (updateSequenceModeDropdown.isExpanded()) {
            return updateSequenceModeDropdown;
        }
        return null;
    }

    private boolean isInTree(int mouseX, int mouseY) {
        return mouseX >= treeX && mouseX <= treeX + treeWidth && mouseY >= treeY && mouseY <= treeY + treeHeight;
    }

    private boolean isInCardList(int mouseX, int mouseY) {
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private boolean isInTreeCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, treeX, treeY, treeWidth);
    }

    private boolean isInListCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, listX, listY, listWidth);
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
        this.drawString(this.fontRenderer, collapsed ? ">" : "<", btnX + 4, btnY + 3, 0xFFEAF7FF);
    }

    private int getCollapsedSectionWidth(String shortTitle) {
        int textWidth = this.fontRenderer == null ? 16 : this.fontRenderer.getStringWidth(shortTitle == null ? "" : shortTitle);
        return Math.max(34, textWidth + 24);
    }

    private boolean isInEditor(int mouseX, int mouseY) {
        return mouseX >= editorBoxX && mouseX <= editorBoxX + editorBoxWidth
                && mouseY >= editorBoxY && mouseY <= editorBoxY + editorBoxHeight;
    }

    private int getVisibleTreeRowCount() {
        return Math.max(1, (treeHeight - 26) / TREE_ROW_HEIGHT);
    }

    private int getVisibleCardCount() {
        return Math.max(1, (listHeight - 28) / (CARD_HEIGHT + CARD_GAP));
    }

    private int getCardIndexAt(int mouseY) {
        int contentY = listY + 22;
        int local = mouseY - contentY;
        if (local < 0) {
            return -1;
        }
        int step = CARD_HEIGHT + CARD_GAP;
        int localIndex = local / step;
        int yInCard = local % step;
        if (yInCard >= CARD_HEIGHT) {
            return -1;
        }
        return listScrollOffset + localIndex;
    }

    private int getMaxEditorScroll() {
        return Math.max(0, getEffectiveEditorTotalRows() - editorVisibleRows);
    }

    private void layoutEditorFields() {
        editorLabelX = editorBoxX + 6;
        editorRowStartY = editorBoxY + 26;

        int fieldX = editorLabelX + editorLabelWidth;
        int baseY = editorRowStartY;

        for (int row = 0; row <= 16; row++) {
            GuiTextField field = getFieldByRow(row);
            if (field == null) {
                continue;
            }
            if (row == 16 && !isUpdateSequenceCooldownVisible()) {
                field.y = -2000;
                continue;
            }
            int visibleIndex = row - editorScrollOffset;
            if (visibleIndex < 0 || visibleIndex >= editorVisibleRows) {
                field.y = -2000;
            } else {
                field.x = fieldX;
                field.y = baseY + visibleIndex * EDITOR_ROW_HEIGHT;
            }
        }

        if (selectSequenceButton != null) {
            boolean visible = updateSequenceField != null && updateSequenceField.y >= editorRowStartY
                    && updateSequenceField.y < editorRowStartY + editorVisibleRows * EDITOR_ROW_HEIGHT;
            selectSequenceButton.visible = visible;
            selectSequenceButton.enabled = visible;
            selectSequenceButton.x = editorBoxX + editorBoxWidth - 56;
            selectSequenceButton.y = visible ? updateSequenceField.y : -2000;
            selectSequenceButton.width = 56;
            selectSequenceButton.height = 16;
        }

        enabledDropdown.updateBounds(enabledField.x, enabledField.y, enabledField.width, enabledField.height);
        directionDropdown.updateBounds(directionField.x, directionField.y, directionField.width, directionField.height);
        targetDropdown.updateBounds(targetField.x, targetField.y, targetField.width, targetField.height);
        valueTypeDropdown.updateBounds(valueTypeField.x, valueTypeField.y, valueTypeField.width, valueTypeField.height);
        updateSequenceModeDropdown.updateBounds(updateSequenceModeField.x, updateSequenceModeField.y,
                updateSequenceModeField.width, updateSequenceModeField.height);
        syncEnumFieldsFromDropdowns();
    }

    private int getEffectiveEditorTotalRows() {
        return EDITOR_BASE_ROWS + (isUpdateSequenceCooldownVisible() ? 1 : 0);
    }

    private boolean isUpdateSequenceCooldownVisible() {
        return "cooldown".equalsIgnoreCase(updateSequenceModeDropdown.getValue());
    }

    private String getRowLabel(int row) {
        switch (row) {
            case 0:
                return "规则名";
            case 1:
                return "显示名";
            case 2:
                return "备注";
            case 3:
                return "别名";
            case 4:
                return "启用";
            case 5:
                return "频道";
            case 6:
                return "方向";
            case 7:
                return "目标";
            case 8:
                return "匹配表达式";
            case 9:
                return "偏移";
            case 10:
                return "捕获组号";
            case 11:
                return "所属分组";
            case 12:
                return "值类型";
            case 13:
                return "字节长度";
            case 14:
                return "触发序列";
            case 15:
                return "触发模式";
            case 16:
                return "冷却(ms)";
            default:
                return "";
        }
    }

    private void ensureCardSelectionVisible() {
        if (selectedVisibleIndex < 0) {
            return;
        }
        int visible = getVisibleCardCount();
        if (selectedVisibleIndex < listScrollOffset) {
            listScrollOffset = selectedVisibleIndex;
        } else if (selectedVisibleIndex >= listScrollOffset + visible) {
            listScrollOffset = selectedVisibleIndex - visible + 1;
        }
    }

    private String getSelectedRuleName() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleCards.size()) {
            return "";
        }
        return safe(visibleCards.get(selectedVisibleIndex).model.name);
    }

    private String getCardCategory(CapturedIdRuleManager.RuleCard card) {
        if (card == null || card.model == null || isBlank(card.model.category)) {
            return CATEGORY_UNGROUPED;
        }
        return card.model.category.trim();
    }

    private boolean isConcreteCategory(String category) {
        return !isBlank(category) && !CATEGORY_ALL.equals(category) && !CATEGORY_UNGROUPED.equals(category);
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
        notifyMsg(message, color == 0xFFFF8E8E);
    }

    private void notifyMsg(String text, boolean err) {
        if (mc != null && mc.player != null) {
            mc.player.sendSystemMessage(new TextComponentString(text));
        } else if (err) {
            System.err.println(text);
        } else {
            System.out.println(text);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void setText(GuiTextField field, String value) {
        if (field != null) {
            field.setText(value == null ? "" : value);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String trimTo(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String trimToWidth(String s, int maxWidth) {
        if (s == null) {
            return "";
        }
        if (maxWidth <= 0 || this.fontRenderer == null) {
            return "";
        }
        if (this.fontRenderer.getStringWidth(s) <= maxWidth) {
            return s;
        }

        String ellipsis = "...";
        int end = s.length();
        while (end > 0 && this.fontRenderer.getStringWidth(s.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        if (end <= 0) {
            return ellipsis;
        }
        return s.substring(0, end) + ellipsis;
    }

    private static final class TreeRow {
        private static final int TYPE_ALL = 0;
        private static final int TYPE_CATEGORY = 1;
        private static final int TYPE_RULE = 2;

        private final int type;
        private final String label;
        private final String category;
        private final String ruleName;
        private final int indent;

        private TreeRow(int type, String label, String category, String ruleName, int indent) {
            this.type = type;
            this.label = label == null ? "" : label;
            this.category = category == null ? "" : category;
            this.ruleName = ruleName == null ? "" : ruleName;
            this.indent = indent;
        }

        private static TreeRow allRow(String label) {
            return new TreeRow(TYPE_ALL, label, CATEGORY_ALL, "", 0);
        }

        private static TreeRow categoryRow(String category) {
            return new TreeRow(TYPE_CATEGORY, category, category, "", 0);
        }

        private static TreeRow ruleRow(String category, String ruleName, String displayName) {
            return new TreeRow(TYPE_RULE, displayName, category, ruleName, 1);
        }
    }

    private static final class ContextMenuItem {
        private final String key;
        private final String label;
        private final boolean enabled;

        private ContextMenuItem(String key, String label, boolean enabled) {
            this.key = key;
            this.label = label;
            this.enabled = enabled;
        }
    }

    private class EnumDropdown {
        private int x;
        private int y;
        private int width;
        private int height;
        private final String[] options;
        private final String[] displayKeys;
        private int selectedIndex = 0;
        private boolean expanded = false;

        private EnumDropdown(int x, int y, int width, int height, String[] options, String[] displayKeys) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.options = options;
            this.displayKeys = displayKeys;
        }

        private void updateBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void drawMain(int mouseX, int mouseY, String label) {
            boolean hoverMain = isMouseInside(mouseX, mouseY, x, y, width, height);
            int bg = hoverMain ? 0xCC203146 : 0xCC152433;
            int border = expanded ? 0xFF76D1FF : (hoverMain ? 0xFF4FA6D9 : 0xFF3F6A8C);
            int text = 0xFFEAF7FF;

            drawRect(x, y, x + width, y + height, bg);
            drawHorizontalLine(x, x + width, y, border);
            drawHorizontalLine(x, x + width, y + height, border);
            drawVerticalLine(x, y, y + height, border);
            drawVerticalLine(x + width, y, y + height, border);

            String display = getDisplayValue();
            String mainText = trimTo(display, 24);
            fontRenderer.drawString(mainText, x + 5, y + 4, GuiTheme.resolveTextColor(mainText, text));
            fontRenderer.drawString(expanded ? "▲" : "▼", x + width - 10, y + 4, 0xFF9FDFFF);

            if (hoverMain) {
                List<String> tips = new ArrayList<>();
                tips.add("§e选择" + label);
                tips.add("当前: " + display);
                drawHoveringText(tips, mouseX, mouseY, fontRenderer);
            }
        }

        private void drawExpanded(int mouseX, int mouseY) {
            if (!expanded) {
                return;
            }
            for (int i = 0; i < options.length; i++) {
                int oy = y + height + i * height;
                boolean hoverItem = isMouseInside(mouseX, mouseY, x, oy, width, height);
                boolean selected = i == selectedIndex;
                int itemBg = selected ? 0xEE2B5A7C : (hoverItem ? 0xCC29455E : 0xCC1B2D3D);
                int itemBorder = hoverItem ? 0xFF7ED0FF : 0xFF3B6B8A;

                drawRect(x, oy, x + width, oy + height, itemBg);
                drawHorizontalLine(x, x + width, oy, itemBorder);
                drawHorizontalLine(x, x + width, oy + height, itemBorder);
                drawVerticalLine(x, oy, oy + height, itemBorder);
                drawVerticalLine(x + width, oy, oy + height, itemBorder);

                String display = getDisplayLabel(i);
                fontRenderer.drawString(display, x + 5, oy + 4, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) {
                return false;
            }

            if (isMouseInside(mouseX, mouseY, x, y, width, height)) {
                expanded = !expanded;
                return true;
            }

            if (!expanded) {
                return false;
            }

            for (int i = 0; i < options.length; i++) {
                int oy = y + height + i * height;
                if (isMouseInside(mouseX, mouseY, x, oy, width, height)) {
                    selectedIndex = i;
                    expanded = false;
                    return true;
                }
            }
            return false;
        }

        private String getValue() {
            if (options == null || options.length == 0) {
                return "";
            }
            if (selectedIndex < 0 || selectedIndex >= options.length) {
                selectedIndex = 0;
            }
            return safe(options[selectedIndex]);
        }

        private String getDisplayValue() {
            return getDisplayLabel(selectedIndex);
        }

        private String getDisplayLabel(int index) {
            if (displayKeys != null && index >= 0 && index < displayKeys.length) {
                return I18n.format(displayKeys[index]);
            }
            if (options == null || index < 0 || index >= options.length) {
                return "";
            }
            return safe(options[index]);
        }

        private void setValue(String value) {
            String normalized = safe(value).trim();
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(normalized)) {
                    selectedIndex = i;
                    return;
                }
            }
            selectedIndex = 0;
        }

        private void collapse() {
            expanded = false;
        }

        private boolean isExpanded() {
            return expanded;
        }

        private boolean isMouseInside(int mx, int my, int rx, int ry, int rw, int rh) {
            return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
        }
    }
}



