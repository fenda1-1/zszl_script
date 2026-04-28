package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.ConditionalExecutionHandler;
import com.zszl.zszlScriptMod.system.ConditionalRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiConditionalExecutionManager extends ThemedGuiScreen {

    private static final String CATEGORY_ALL = "__all__";
    private static final String CATEGORY_DEFAULT = "默认";

    private static final int BTN_NEW = 100;
    private static final int BTN_DELETE = 101;
    private static final int BTN_MOVE_UP = 102;
    private static final int BTN_MOVE_DOWN = 103;
    private static final int BTN_SAVE = 104;
    private static final int BTN_DONE = 105;
    private static final int BTN_SELECT_SEQUENCE = 106;
    private static final int BTN_GET_COORDS = 107;
    private static final int BTN_TOGGLE_ENABLED = 108;
    private static final int BTN_TOGGLE_STOP = 109;
    private static final int BTN_TOGGLE_RUN_ONCE = 110;
    private static final int BTN_TOGGLE_VISUALIZE = 111;
    private static final int BTN_TOGGLE_ANTI_STUCK = 112;

    private static final int TREE_ROW_HEIGHT = 18;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 6;
    private static final long CARD_DOUBLE_CLICK_WINDOW_MS = 300L;
    private static final int EDITOR_ROW_HEIGHT = 24;

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    private final GuiScreen parentScreen;

    private final List<ConditionalRule> allRules = new ArrayList<>();
    private final List<ConditionalRule> visibleRules = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<TreeRow> visibleTreeRows = new ArrayList<>();
    private final Set<String> expandedCategories = new LinkedHashSet<>();

    private String selectedCategory = CATEGORY_ALL;
    private int selectedVisibleIndex = -1;
    private int selectedTreeIndex = 0;
    private int treeScrollOffset = 0;
    private int listScrollOffset = 0;
    private int editorScrollOffset = 0;
    private boolean creatingNew = false;
    private boolean treeCollapsed = false;
    private boolean listCollapsed = false;
    private int lastClickedCardIndex = -1;
    private long lastClickedCardTimeMs = 0L;

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

    private int editorX;
    private int editorY;
    private int editorWidth;
    private int editorHeight;
    private int editorLabelX;
    private int editorFieldX;
    private int editorRowStartY;
    private int editorVisibleRows;

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField sequenceField;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;
    private GuiTextField rangeField;
    private GuiTextField loopCountField;
    private GuiTextField cooldownField;
    private GuiTextField visualizeColorField;
    private GuiTextField antiStuckTimeoutField;

    private GuiButton btnNew;
    private GuiButton btnDelete;
    private GuiButton btnMoveUp;
    private GuiButton btnMoveDown;
    private GuiButton btnSave;
    private GuiButton btnDone;
    private GuiButton btnSelectSequence;
    private GuiButton btnGetCoords;
    private GuiButton btnToggleEnabled;
    private GuiButton btnToggleStop;
    private GuiButton btnToggleRunOnce;
    private GuiButton btnToggleVisualize;
    private GuiButton btnToggleAntiStuck;

    private boolean editorEnabled = true;
    private boolean editorStopOnExit = true;
    private boolean editorRunOnce = false;
    private boolean editorVisualizeRange = false;
    private boolean editorAntiStuckEnabled = false;

    private ConditionalRule pendingEditorRestoreModel = null;
    private boolean pendingEditorRestoreCreateMode = false;
    private String pendingEditorRestoreCategory = CATEGORY_ALL;
    private String pendingEditorRestoreRuleName = "";

    private final List<ContextMenuItem> contextMenuItems = new ArrayList<>();
    private boolean contextMenuVisible = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private int contextMenuWidth = 170;
    private int contextMenuItemHeight = 18;
    private int contextMenuTargetIndex = -1;
    private String contextMenuTargetType = "card";
    private String contextMenuTargetCategory = "";

    private String statusMessage = "§7使用指南：左键筛选/折叠，右键分组或卡片打开菜单，滚轮可滚动右侧规则编辑器";
    private int statusColor = 0xFFB8C7D9;

    public GuiConditionalExecutionManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        recalcLayout();
        initEditorFields();
        initButtons();
        refreshData(false);
        restorePendingEditorStateIfNeeded();
        layoutEditorFields();
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
                ? getCollapsedSectionWidth("规则")
                : Math.max(122, Math.min(170, availableWidth / 6));

        listX = treeX + treeWidth + columnGap;
        listY = treeY;
        listHeight = treeHeight;
        listWidth = listCollapsed
                ? getCollapsedSectionWidth("卡片")
                : Math.max(190, Math.min(250, availableWidth / 4));

        int rightX = listX + listWidth + columnGap;
        editorX = rightX;
        editorY = treeY;
        editorWidth = panelX + panelWidth - sidePadding - rightX;
        editorHeight = treeHeight;

        editorLabelX = editorX + 10;
        editorFieldX = editorX + 92;
        editorRowStartY = editorY + 28;
        editorVisibleRows = Math.max(1, (editorHeight - 38) / EDITOR_ROW_HEIGHT);
    }

    private void initEditorFields() {
        nameField = createField(2001);
        categoryField = createField(2002);
        sequenceField = createField(2003);
        xField = createField(2004);
        yField = createField(2005);
        zField = createField(2006);
        rangeField = createField(2007);
        loopCountField = createField(2008);
        cooldownField = createField(2009);
        visualizeColorField = createField(2010);
        antiStuckTimeoutField = createField(2011);
        sequenceField.setEnabled(false);
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private void initButtons() {
        int bottomY = panelY + panelHeight - 28;

        btnNew = new ThemedButton(BTN_NEW, treeX, bottomY, 72, 20, "新增");
        btnDelete = new ThemedButton(BTN_DELETE, treeX + 80, bottomY, 72, 20, "删除");
        btnMoveUp = new ThemedButton(BTN_MOVE_UP, treeX + 160, bottomY, 52, 20, "上移");
        btnMoveDown = new ThemedButton(BTN_MOVE_DOWN, treeX + 220, bottomY, 52, 20, "下移");

        btnSave = new ThemedButton(BTN_SAVE, panelX + panelWidth - 186, bottomY, 84, 20,
                I18n.format("gui.common.save"));
        btnDone = new ThemedButton(BTN_DONE, panelX + panelWidth - 94, bottomY, 84, 20,
                I18n.format("gui.common.done"));

        btnToggleEnabled = new ThemedButton(BTN_TOGGLE_ENABLED, 0, 0, 100, 20, "");
        btnSelectSequence = new ThemedButton(BTN_SELECT_SEQUENCE, 0, 0, 64, 20, "选择");
        btnGetCoords = new ThemedButton(BTN_GET_COORDS, 0, 0, 84, 20, "获取坐标");
        btnToggleVisualize = new ThemedButton(BTN_TOGGLE_VISUALIZE, 0, 0, 120, 20, "");
        btnToggleAntiStuck = new ThemedButton(BTN_TOGGLE_ANTI_STUCK, 0, 0, 120, 20, "");
        btnToggleStop = new ThemedButton(BTN_TOGGLE_STOP, 0, 0, 120, 20, "");
        btnToggleRunOnce = new ThemedButton(BTN_TOGGLE_RUN_ONCE, 0, 0, 120, 20, "");

        this.buttonList.add(btnNew);
        this.buttonList.add(btnDelete);
        this.buttonList.add(btnMoveUp);
        this.buttonList.add(btnMoveDown);
        this.buttonList.add(btnSave);
        this.buttonList.add(btnDone);
        this.buttonList.add(btnToggleEnabled);
        this.buttonList.add(btnSelectSequence);
        this.buttonList.add(btnGetCoords);
        this.buttonList.add(btnToggleVisualize);
        this.buttonList.add(btnToggleAntiStuck);
        this.buttonList.add(btnToggleStop);
        this.buttonList.add(btnToggleRunOnce);
    }

    private void layoutEditorFields() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);
        int coordButtonWidth = Math.min(100, fullFieldWidth);
        int colorFieldWidth = Math.max(82, fullFieldWidth - 30);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);

        placeField(sequenceField, 2, editorFieldX, Math.max(60, fullFieldWidth - 70));
        placeButton(btnSelectSequence, 2, sequenceField.x + sequenceField.width + 6, 64, 20);

        placeField(xField, 3, editorFieldX, halfWidth);
        placeField(yField, 3, editorFieldX + halfWidth + 10, halfWidth);

        placeField(zField, 4, editorFieldX, halfWidth);
        placeField(rangeField, 4, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetCoords, 5, editorFieldX, coordButtonWidth, 20);

        placeField(loopCountField, 6, editorFieldX, halfWidth);
        placeField(cooldownField, 6, editorFieldX + halfWidth + 10, halfWidth);

        placeButton(btnToggleVisualize, 7, editorFieldX, fullFieldWidth, 20);
        placeField(visualizeColorField, 8, editorFieldX, colorFieldWidth);
        placeButton(btnToggleAntiStuck, 9, editorFieldX, fullFieldWidth, 20);
        placeField(antiStuckTimeoutField, 10, editorFieldX, fullFieldWidth);
        placeButton(btnToggleEnabled, 11, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleStop, 12, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleRunOnce, 13, editorFieldX, fullFieldWidth, 20);

        clampEditorScroll();
        updateButtonStates();
    }

    private void placeField(GuiTextField field, int row, int x, int width) {
        if (field == null) {
            return;
        }
        int y = getEditorRowY(row);
        field.x = x;
        field.width = width;
        field.height = 16;
        field.y = y >= 0 ? y : -2000;
    }

    private void placeButton(GuiButton button, int row, int x, int width, int height) {
        if (button == null) {
            return;
        }
        int y = getEditorRowY(row);
        boolean visible = y >= 0;
        button.visible = visible;
        button.x = x;
        button.y = visible ? y - 2 : -2000;
        button.width = width;
        button.height = height;
        if (!visible) {
            button.enabled = false;
        }
    }

    private int getEditorRowY(int row) {
        int visibleIndex = row - editorScrollOffset;
        if (visibleIndex < 0 || visibleIndex >= editorVisibleRows) {
            return -1;
        }
        return editorRowStartY + visibleIndex * EDITOR_ROW_HEIGHT;
    }

    private int getEffectiveEditorTotalRows() {
        return 14;
    }

    private int getMaxEditorScroll() {
        return Math.max(0, getEffectiveEditorTotalRows() - editorVisibleRows);
    }

    private void clampEditorScroll() {
        editorScrollOffset = Math.max(0, Math.min(editorScrollOffset, getMaxEditorScroll()));
    }

    private void refreshData(boolean keepSelection) {
        String keepRuleName = keepSelection ? getSelectedRuleName() : "";

        allRules.clear();
        allRules.addAll(ConditionalExecutionHandler.rules);

        rebuildCategories();
        rebuildTreeRows();
        rebuildVisibleRules();

        if (!isBlank(keepRuleName) && selectByRuleName(keepRuleName)) {
            return;
        }

        loadSelectionOrNew();
    }

    private void rebuildCategories() {
        categories.clear();
        LinkedHashSet<String> seen = new LinkedHashSet<>(ConditionalExecutionHandler.getCategoriesSnapshot());
        for (ConditionalRule rule : allRules) {
            seen.add(getRuleCategory(rule));
        }
        if (seen.isEmpty()) {
            seen.add(CATEGORY_DEFAULT);
        }
        categories.addAll(seen);
    }

    private void rebuildTreeRows() {
        visibleTreeRows.clear();
        visibleTreeRows.add(TreeRow.allRow("全部规则"));
        for (String category : categories) {
            visibleTreeRows.add(TreeRow.categoryRow(category));
            if (expandedCategories.contains(category)) {
                for (ConditionalRule rule : allRules) {
                    if (category.equals(getRuleCategory(rule))) {
                        visibleTreeRows.add(TreeRow.ruleRow(category, safe(rule.name), safe(rule.name)));
                    }
                }
            }
        }
        selectedTreeIndex = Math.max(0, Math.min(selectedTreeIndex, Math.max(0, visibleTreeRows.size() - 1)));
    }

    private void rebuildVisibleRules() {
        visibleRules.clear();
        for (ConditionalRule rule : allRules) {
            if (CATEGORY_ALL.equals(selectedCategory) || selectedCategory.equals(getRuleCategory(rule))) {
                visibleRules.add(rule);
            }
        }

        int max = Math.max(0, visibleRules.size() - getVisibleCardCount());
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));
        if (selectedVisibleIndex >= visibleRules.size()) {
            selectedVisibleIndex = visibleRules.isEmpty() ? -1 : 0;
        }
    }

    private void loadSelectionOrNew() {
        if (visibleRules.isEmpty()) {
            selectedVisibleIndex = -1;
            creatingNew = true;
            clearEditorForNew();
            return;
        }
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            selectedVisibleIndex = 0;
        }
        creatingNew = false;
        loadEditor(visibleRules.get(selectedVisibleIndex));
    }

    private void clearEditorForNew() {
        ConditionalRule draft = new ConditionalRule();
        draft.category = isConcreteCategory(selectedCategory) ? selectedCategory : CATEGORY_DEFAULT;
        loadEditor(draft);
        creatingNew = true;
        editorScrollOffset = 0;
        layoutEditorFields();
    }

    private void loadEditor(ConditionalRule rule) {
        ConditionalRule model = rule == null ? new ConditionalRule() : rule;

        setText(nameField, safe(model.name));
        setText(categoryField, getRuleCategory(model));
        setText(sequenceField, safe(model.sequenceName));
        setText(xField, formatDouble(model.centerX));
        setText(yField, formatDouble(model.centerY));
        setText(zField, formatDouble(model.centerZ));
        setText(rangeField, formatDouble(model.range));
        setText(loopCountField, String.valueOf(model.loopCount));
        setText(cooldownField, String.valueOf(model.cooldownSeconds));
        setText(visualizeColorField, ConditionalRule.normalizeColor(model.visualizeBorderColor));
        setText(antiStuckTimeoutField, String.valueOf(Math.max(1, model.antiStuckTimeoutSeconds)));

        editorEnabled = model.enabled;
        editorStopOnExit = model.stopOnExit;
        editorRunOnce = model.runOncePerEntry;
        editorVisualizeRange = model.visualizeRange;
        editorAntiStuckEnabled = model.antiStuckEnabled;

        clampEditorScroll();
        layoutEditorFields();
    }

    private void updateButtonStates() {
        if (btnToggleVisualize != null) {
            btnToggleVisualize.displayString = "可视化范围: " + yesNo(editorVisualizeRange);
        }
        if (btnToggleAntiStuck != null) {
            btnToggleAntiStuck.displayString = "防卡重启: " + yesNo(editorAntiStuckEnabled);
        }
        if (btnToggleEnabled != null) {
            btnToggleEnabled.displayString = "启用: " + boolText(editorEnabled);
        }
        if (btnToggleStop != null) {
            btnToggleStop.displayString = "离开区域停止: " + yesNo(editorStopOnExit);
        }
        if (btnToggleRunOnce != null) {
            btnToggleRunOnce.displayString = "每次进入只触发一次: " + yesNo(editorRunOnce);
        }

        boolean hasSelectedRule = !creatingNew && selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleRules.size();

        if (btnDelete != null) {
            btnDelete.enabled = hasSelectedRule;
        }
        if (btnMoveUp != null) {
            btnMoveUp.enabled = hasSelectedRule && getSelectedRuleGlobalIndex() > 0;
        }
        if (btnMoveDown != null) {
            int globalIndex = getSelectedRuleGlobalIndex();
            btnMoveDown.enabled = hasSelectedRule
                    && globalIndex >= 0
                    && globalIndex < ConditionalExecutionHandler.rules.size() - 1;
        }
        if (btnSave != null) {
            btnSave.enabled = true;
        }

        nameField.setEnabled(true);
        categoryField.setEnabled(true);
        xField.setEnabled(true);
        yField.setEnabled(true);
        zField.setEnabled(true);
        rangeField.setEnabled(true);
        loopCountField.setEnabled(true);
        cooldownField.setEnabled(true);
        visualizeColorField.setEnabled(editorVisualizeRange);
        antiStuckTimeoutField.setEnabled(editorAntiStuckEnabled);
        btnSelectSequence.enabled = btnSelectSequence.visible;
        btnGetCoords.enabled = btnGetCoords.visible;
        btnToggleVisualize.enabled = btnToggleVisualize.visible;
        btnToggleAntiStuck.enabled = btnToggleAntiStuck.visible;
        btnToggleStop.enabled = btnToggleStop.visible;
        btnToggleRunOnce.enabled = btnToggleRunOnce.visible;
        btnToggleEnabled.enabled = btnToggleEnabled.visible;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DONE) {
            ConditionalExecutionHandler.saveConfig();
            mc.setScreen(parentScreen);
            return;
        }
        if (button.id == BTN_NEW) {
            creatingNew = true;
            selectedVisibleIndex = -1;
            clearEditorForNew();
            setStatus("§a已进入新建规则模式", 0xFF8CFF9E);
            return;
        }
        if (button.id == BTN_DELETE) {
            deleteSelectedRule();
            return;
        }
        if (button.id == BTN_MOVE_UP) {
            moveSelectedRule(-1);
            return;
        }
        if (button.id == BTN_MOVE_DOWN) {
            moveSelectedRule(1);
            return;
        }
        if (button.id == BTN_SAVE) {
            saveCurrentRule();
            return;
        }
        if (button.id == BTN_SELECT_SEQUENCE) {
            pendingEditorRestoreModel = buildRuleFromEditor();
            pendingEditorRestoreCreateMode = creatingNew;
            pendingEditorRestoreCategory = selectedCategory;
            pendingEditorRestoreRuleName = getSelectedRuleName();
            mc.setScreen(new GuiSequenceSelector(this, selected -> {
                if (selected != null) {
                    if (pendingEditorRestoreModel == null) {
                        pendingEditorRestoreModel = buildRuleFromEditor();
                    }
                    pendingEditorRestoreModel.sequenceName = selected;
                }
                mc.setScreen(this);
            }));
            return;
        }
        if (button.id == BTN_GET_COORDS) {
            if (mc.player != null) {
                setText(xField, formatDouble(mc.player.getX()));
                setText(yField, formatDouble(mc.player.getY()));
                setText(zField, formatDouble(mc.player.getZ()));
            }
            return;
        }
        if (button.id == BTN_TOGGLE_VISUALIZE) {
            editorVisualizeRange = !editorVisualizeRange;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_ANTI_STUCK) {
            editorAntiStuckEnabled = !editorAntiStuckEnabled;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_STOP) {
            editorStopOnExit = !editorStopOnExit;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_RUN_ONCE) {
            editorRunOnce = !editorRunOnce;
            updateButtonStates();
            return;
        }

        super.actionPerformed(button);
    }

    private void saveCurrentRule() {
        try {
            ConditionalRule model = buildRuleFromEditor();

            if (isBlank(model.name)) {
                setStatus("§c规则名称不能为空", 0xFFFF8E8E);
                return;
            }

            if (creatingNew) {
                ConditionalExecutionHandler.rules.add(model);
                creatingNew = false;
                ConditionalExecutionHandler.saveConfig();
                refreshData(true);
                selectByRuleName(model.name);
                setStatus("§a已新增规则: " + model.name, 0xFF8CFF9E);
                return;
            }

            if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
                setStatus("§c请先选择一条规则", 0xFFFF8E8E);
                return;
            }

            ConditionalRule target = visibleRules.get(selectedVisibleIndex);
            applyRuleValues(target, model);

            ConditionalExecutionHandler.saveConfig();
            refreshData(true);
            selectByRuleName(target.name);
            setStatus("§a规则已保存", 0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c保存失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void deleteSelectedRule() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先选择一条规则", 0xFFFF8E8E);
            return;
        }

        ConditionalRule rule = visibleRules.get(selectedVisibleIndex);
        ConditionalExecutionHandler.rules.remove(rule);
        ConditionalExecutionHandler.saveConfig();
        refreshData(false);
        setStatus("§a已删除规则: " + safe(rule.name), 0xFF8CFF9E);
    }

    private void duplicateSelectedRule() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }

        ConditionalRule source = visibleRules.get(selectedVisibleIndex);
        ConditionalRule copy = copyRule(source);
        copy.name = safe(source.name).trim().isEmpty() ? "规则副本" : safe(source.name).trim() + " 副本";
        if (!isConcreteCategory(copy.category)) {
            copy.category = isConcreteCategory(selectedCategory) ? selectedCategory : CATEGORY_DEFAULT;
        }
        copy.enabled = false;

        ConditionalExecutionHandler.rules.add(copy);
        ConditionalExecutionHandler.saveConfig();
        refreshData(true);
        selectByRuleName(copy.name);
        setStatus("§a已复制规则: " + copy.name, 0xFF8CFF9E);
    }

    private void copySelectedRuleName() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }
        ConditionalRule rule = visibleRules.get(selectedVisibleIndex);
        setClipboardString(safe(rule.name));
        setStatus("§a已复制规则名: " + safe(rule.name), 0xFF8CFF9E);
    }

    private void moveSelectedRuleToCurrentCategory() {
        if (!isConcreteCategory(selectedCategory)) {
            setStatus("§c请先在左侧选择一个具体分组", 0xFFFF8E8E);
            return;
        }
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }

        ConditionalRule rule = visibleRules.get(selectedVisibleIndex);
        rule.category = selectedCategory;
        ConditionalExecutionHandler.saveConfig();
        refreshData(true);
        selectByRuleName(rule.name);
        setStatus("§a已移动规则到分组: " + selectedCategory, 0xFF8CFF9E);
    }

    private void reloadAllRules() {
        ConditionalExecutionHandler.loadConfig();
        refreshData(false);
        setStatus("§a已重载条件执行规则", 0xFF8CFF9E);
    }

    private void moveSelectedRule(int offset) {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            return;
        }

        ConditionalRule rule = visibleRules.get(selectedVisibleIndex);
        int currentIndex = ConditionalExecutionHandler.rules.indexOf(rule);
        int targetIndex = currentIndex + offset;
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= ConditionalExecutionHandler.rules.size()) {
            return;
        }

        Collections.swap(ConditionalExecutionHandler.rules, currentIndex, targetIndex);
        ConditionalExecutionHandler.saveConfig();
        refreshData(true);
        selectByRuleName(rule.name);
    }

    private void toggleRuleEnabledFromCard(ConditionalRule rule) {
        if (rule == null) {
            return;
        }

        String ruleName = safe(rule.name);
        rule.enabled = !rule.enabled;
        ConditionalExecutionHandler.saveConfig();
        refreshData(true);
        selectByRuleName(ruleName);

        if (rule.enabled) {
            setStatus("§a已快速启用规则: " + ruleName, 0xFF8CFF9E);
        } else {
            setStatus("§e已快速关闭规则: " + ruleName, 0xFFFFD27F);
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

        if (!listCollapsed && mouseButton == 1 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleRules.size()) {
                selectedVisibleIndex = actual;
                creatingNew = false;
                loadEditor(visibleRules.get(actual));
                openContextMenu(mouseX, mouseY, actual);
                return;
            }
            openContextMenu(mouseX, mouseY, -1);
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            if (mouseButton == 0) {
                handleTreeClick(mouseY);
                return;
            }
            if (mouseButton == 1) {
                handleTreeRightClick(mouseX, mouseY);
                return;
            }
        }

        if (!listCollapsed && mouseButton == 0 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleRules.size()) {
                long now = System.currentTimeMillis();
                boolean isDoubleClick = actual == lastClickedCardIndex
                        && (now - lastClickedCardTimeMs) <= CARD_DOUBLE_CLICK_WINDOW_MS;

                selectedVisibleIndex = actual;
                creatingNew = false;
                ConditionalRule selected = visibleRules.get(actual);
                loadEditor(selected);

                if (isDoubleClick) {
                    toggleRuleEnabledFromCard(selected);
                    lastClickedCardIndex = -1;
                    lastClickedCardTimeMs = 0L;
                } else {
                    lastClickedCardIndex = actual;
                    lastClickedCardTimeMs = now;
                }
            } else {
                lastClickedCardIndex = -1;
                lastClickedCardTimeMs = 0L;
            }
            return;
        }

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
    }

    private void handleTreeClick(int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }

        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (row.type == TreeRow.TYPE_ALL) {
            selectedCategory = CATEGORY_ALL;
            rebuildVisibleRules();
            loadSelectionOrNew();
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
            rebuildVisibleRules();
            loadSelectionOrNew();
            return;
        }

        if (row.type == TreeRow.TYPE_RULE) {
            selectedCategory = row.category;
            rebuildVisibleRules();
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
            rebuildVisibleRules();
        }

        openTreeContextMenu(mouseX, mouseY, row);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (isInEditor(mouseX, mouseY)) {
            if (wheel < 0) {
                editorScrollOffset = Math.min(getMaxEditorScroll(), editorScrollOffset + 1);
            } else {
                editorScrollOffset = Math.max(0, editorScrollOffset - 1);
            }
            layoutEditorFields();
            return;
        }

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            int max = Math.max(0, visibleTreeRows.size() - getVisibleTreeRowCount());
            treeScrollOffset = wheel < 0 ? Math.min(max, treeScrollOffset + 1) : Math.max(0, treeScrollOffset - 1);
            return;
        }

        if (!listCollapsed && isInCardList(mouseX, mouseY)) {
            int max = Math.max(0, visibleRules.size() - getVisibleCardCount());
            listScrollOffset = wheel < 0 ? Math.min(max, listScrollOffset + 1) : Math.max(0, listScrollOffset - 1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (contextMenuVisible) {
                closeContextMenu();
                return;
            }
            ConditionalExecutionHandler.loadConfig();
            mc.setScreen(parentScreen);
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
        drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.conditional.manager.title"),
                this.fontRenderer);

        GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 22, panelWidth - 20, 18, false, true);
        drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 27, statusColor);

        drawTreePanel(mouseX, mouseY);
        drawCardPanel(mouseX, mouseY);
        drawEditorPanel();

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (contextMenuVisible) {
            drawContextMenu(mouseX, mouseY);
        }
    }

    private void drawTreePanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(treeX, treeY, treeWidth, treeHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(treeX + 8, treeY + 8, treeCollapsed ? "规则" : "规则树（右键打开菜单）", this.fontRenderer);
        drawCollapseButton(treeX, treeY, treeWidth, treeCollapsed);
        if (treeCollapsed) {
            return;
        }

        int contentY = treeY + 22;
        int visibleRows = getVisibleTreeRowCount();
        int max = Math.max(0, visibleTreeRows.size() - visibleRows);
        treeScrollOffset = Math.max(0, Math.min(treeScrollOffset, max));

        if (visibleTreeRows.isEmpty()) {
            GuiTheme.drawEmptyState(treeX + treeWidth / 2, treeY + treeHeight / 2, "暂无规则", this.fontRenderer);
            return;
        }

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
            int maxTextWidth = Math.max(32, treeWidth - (textX - treeX) - 18);

            if (row.type == TreeRow.TYPE_CATEGORY) {
                String arrow = expandedCategories.contains(row.category) ? "▼" : "▶";
                drawString(fontRenderer, arrow, textX, rowY + 5, 0xFF9FDFFF);
                drawString(fontRenderer, trimToWidth(row.label, maxTextWidth - 12), textX + 10, rowY + 5,
                        0xFFE8F1FA);
            } else if (row.type == TreeRow.TYPE_RULE) {
                drawString(fontRenderer, "•", textX, rowY + 5, 0xFFB8C7D9);
                drawString(fontRenderer, trimToWidth(row.label, maxTextWidth - 8), textX + 8, rowY + 5,
                        0xFFD9E6F2);
            } else {
                drawString(fontRenderer, trimToWidth(row.label, maxTextWidth), textX, rowY + 5, 0xFFE8F1FA);
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
        int max = Math.max(0, visibleRules.size() - visible);
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));

        if (visibleRules.isEmpty()) {
            GuiTheme.drawEmptyState(listX + listWidth / 2, listY + listHeight / 2, "该分组下暂无规则", this.fontRenderer);
            return;
        }

        for (int i = 0; i < visible; i++) {
            int actual = listScrollOffset + i;
            if (actual >= visibleRules.size()) {
                break;
            }

            ConditionalRule rule = visibleRules.get(actual);
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

            String status = rule.enabled ? "§a✔" : "§c✘";
            drawString(fontRenderer, trimToWidth(status + " " + safe(rule.name), listWidth - 18),
                    listX + 8, cardTop + 5, 0xFFFFFFFF);
            drawString(fontRenderer, trimToWidth("序列: " + safe(rule.sequenceName), listWidth - 18),
                    listX + 8, cardTop + 19, 0xFFDDDDDD);
            drawString(fontRenderer,
                    trimToWidth("分类: " + getRuleCategory(rule) + " | 半径: " + formatDouble(rule.range), listWidth - 18),
                    listX + 8, cardTop + 31, 0xFFBDBDBD);
            drawString(fontRenderer,
                    trimToWidth("循环: " + rule.loopCount + " | 冷却: " + rule.cooldownSeconds + "s", listWidth - 18),
                    listX + 8, cardTop + 43, 0xFFB8C7D9);
        }

        if (visibleRules.size() > visible) {
            int thumbHeight = Math.max(18,
                    (int) ((visible / (float) Math.max(visible, visibleRules.size())) * (listHeight - 28)));
            int track = Math.max(1, (listHeight - 28) - thumbHeight);
            int thumbY = contentY + (int) ((listScrollOffset / (float) Math.max(1, max)) * track);
            GuiTheme.drawScrollbar(listX + listWidth - 8, contentY, 4, listHeight - 28, thumbY, thumbHeight);
        }
    }

    private void drawEditorPanel() {
        GuiTheme.drawPanelSegment(editorX, editorY, editorWidth, editorHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(editorX + 8, editorY + 8,
                creatingNew ? "规则编辑器 - 新建" : "规则编辑器 - 编辑", this.fontRenderer);

        layoutEditorFields();

        int visibleStart = editorScrollOffset;
        int visibleEnd = Math.min(getEffectiveEditorTotalRows(), visibleStart + editorVisibleRows);
        for (int row = visibleStart; row < visibleEnd; row++) {
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }
            String label = getRowLabel(row);
            if (!isBlank(label)) {
                drawLabel(editorLabelX, y + 4, label);
            }
        }

        for (GuiTextField field : visibleFields()) {
            drawThemedTextField(field);
        }
        drawVisualizeColorPreview();

        if (getEffectiveEditorTotalRows() > editorVisibleRows) {
            int thumbHeight = Math.max(18,
                    (int) ((editorVisibleRows / (float) getEffectiveEditorTotalRows()) * (editorHeight - 12)));
            int track = Math.max(1, (editorHeight - 12) - thumbHeight);
            int thumbY = editorY + 6 + (int) ((editorScrollOffset / (float) Math.max(1, getMaxEditorScroll())) * track);
            GuiTheme.drawScrollbar(editorX + editorWidth - 8, editorY + 6, 4, editorHeight - 12, thumbY, thumbHeight);
        }
    }

    private void drawLabel(int x, int y, String text) {
        drawString(fontRenderer, text, x, y, GuiTheme.SUB_TEXT);
    }

    private void drawVisualizeColorPreview() {
        if (visualizeColorField == null || visualizeColorField.y < editorRowStartY) {
            return;
        }
        int previewSize = Math.max(10, visualizeColorField.height - 4);
        int previewX = visualizeColorField.x + visualizeColorField.width + 8;
        int previewY = visualizeColorField.y + 2;
        int rgb = 0x4AA3FF;
        try {
            rgb = Integer.parseInt(ConditionalRule.normalizeColor(visualizeColorField.getText()).substring(1), 16);
        } catch (Exception ignored) {
        }
        drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFF101820);
        drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000 | rgb);
    }

    private String getRowLabel(int row) {
        switch (row) {
            case 0:
                return "规则名";
            case 1:
                return "所属分组";
            case 2:
                return "触发序列";
            case 3:
                return "中心点 XY";
            case 4:
                return "中心点 Z / 半径";
            case 5:
                return "坐标操作";
            case 6:
                return "循环 / 冷却";
            case 8:
                return "边框颜色";
            case 10:
                return "停留秒数";
            default:
                return "";
        }
    }

    private List<GuiTextField> allFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(sequenceField);
        fields.add(xField);
        fields.add(yField);
        fields.add(zField);
        fields.add(rangeField);
        fields.add(loopCountField);
        fields.add(cooldownField);
        fields.add(visualizeColorField);
        fields.add(antiStuckTimeoutField);
        return fields;
    }

    private List<GuiTextField> visibleFields() {
        List<GuiTextField> fields = new ArrayList<>();
        for (GuiTextField field : allFields()) {
            if (field != null && field.y >= editorRowStartY
                    && field.y < editorRowStartY + editorVisibleRows * EDITOR_ROW_HEIGHT) {
                fields.add(field);
            }
        }
        return fields;
    }

    private ConditionalRule buildRuleFromEditor() {
        ConditionalRule base = creatingNew ? new ConditionalRule()
                : (selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleRules.size()
                        ? copyRule(visibleRules.get(selectedVisibleIndex))
                        : new ConditionalRule());

        base.name = safe(nameField.getText()).trim();
        base.category = safe(categoryField.getText()).trim();
        if (base.category.isEmpty()) {
            base.category = CATEGORY_DEFAULT;
        }
        base.sequenceName = safe(sequenceField.getText()).trim();
        base.enabled = editorEnabled;
        base.stopOnExit = editorStopOnExit;
        base.runOncePerEntry = editorRunOnce;
        base.visualizeRange = editorVisualizeRange;
        base.visualizeBorderColor = ConditionalRule.normalizeColor(visualizeColorField.getText());
        base.antiStuckEnabled = editorAntiStuckEnabled;
        base.antiStuckTimeoutSeconds = Math.max(1,
                parseInt(antiStuckTimeoutField.getText(), base.antiStuckTimeoutSeconds));
        base.centerX = parseDouble(xField.getText(), base.centerX);
        base.centerY = parseDouble(yField.getText(), base.centerY);
        base.centerZ = parseDouble(zField.getText(), base.centerZ);
        base.range = parseDouble(rangeField.getText(), base.range);
        base.loopCount = parseInt(loopCountField.getText(), base.loopCount);
        base.cooldownSeconds = parseInt(cooldownField.getText(), base.cooldownSeconds);
        base.normalize();
        return base;
    }

    private ConditionalRule copyRule(ConditionalRule src) {
        ConditionalRule copy = new ConditionalRule();
        if (src == null) {
            return copy;
        }
        copy.name = src.name;
        copy.category = src.category;
        copy.enabled = src.enabled;
        copy.centerX = src.centerX;
        copy.centerY = src.centerY;
        copy.centerZ = src.centerZ;
        copy.range = src.range;
        copy.sequenceName = src.sequenceName;
        copy.stopOnExit = src.stopOnExit;
        copy.loopCount = src.loopCount;
        copy.cooldownSeconds = src.cooldownSeconds;
        copy.runOncePerEntry = src.runOncePerEntry;
        copy.antiStuckEnabled = src.antiStuckEnabled;
        copy.antiStuckTimeoutSeconds = src.antiStuckTimeoutSeconds;
        copy.visualizeRange = src.visualizeRange;
        copy.visualizeBorderColor = src.visualizeBorderColor;
        copy.hasBeenTriggered = src.hasBeenTriggered;
        copy.cooldownUntil = src.cooldownUntil;
        copy.wasPlayerInRangeLastTick = src.wasPlayerInRangeLastTick;
        return copy;
    }

    private void applyRuleValues(ConditionalRule target, ConditionalRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = source.category;
        target.enabled = source.enabled;
        target.centerX = source.centerX;
        target.centerY = source.centerY;
        target.centerZ = source.centerZ;
        target.range = source.range;
        target.sequenceName = source.sequenceName;
        target.stopOnExit = source.stopOnExit;
        target.loopCount = source.loopCount;
        target.cooldownSeconds = source.cooldownSeconds;
        target.runOncePerEntry = source.runOncePerEntry;
        target.antiStuckEnabled = source.antiStuckEnabled;
        target.antiStuckTimeoutSeconds = source.antiStuckTimeoutSeconds;
        target.visualizeRange = source.visualizeRange;
        target.visualizeBorderColor = source.visualizeBorderColor;
        target.normalize();
    }

    private void restorePendingEditorStateIfNeeded() {
        if (pendingEditorRestoreModel == null) {
            return;
        }

        ConditionalRule restore = pendingEditorRestoreModel;
        boolean restoreCreate = pendingEditorRestoreCreateMode;
        String restoreCategory = pendingEditorRestoreCategory;
        String restoreRuleName = pendingEditorRestoreRuleName;

        pendingEditorRestoreModel = null;
        pendingEditorRestoreCreateMode = false;
        pendingEditorRestoreCategory = CATEGORY_ALL;
        pendingEditorRestoreRuleName = "";

        selectedCategory = isBlank(restoreCategory) ? CATEGORY_ALL : restoreCategory;
        rebuildTreeRows();
        rebuildVisibleRules();

        creatingNew = restoreCreate;
        if (!restoreCreate && !isBlank(restoreRuleName) && selectByRuleName(restoreRuleName)) {
            loadEditor(restore);
            return;
        }

        loadEditor(restore);
    }

    private boolean selectByRuleName(String name) {
        if (isBlank(name)) {
            return false;
        }

        for (ConditionalRule rule : allRules) {
            if (name.equalsIgnoreCase(safe(rule.name))) {
                selectedCategory = getRuleCategory(rule);
                rebuildTreeRows();
                rebuildVisibleRules();

                for (int i = 0; i < visibleRules.size(); i++) {
                    if (name.equalsIgnoreCase(safe(visibleRules.get(i).name))) {
                        selectedVisibleIndex = i;
                        creatingNew = false;
                        loadEditor(visibleRules.get(i));
                        ensureCardSelectionVisible();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getSelectedRuleName() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            return "";
        }
        return safe(visibleRules.get(selectedVisibleIndex).name);
    }

    private int getSelectedRuleGlobalIndex() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            return -1;
        }
        return ConditionalExecutionHandler.rules.indexOf(visibleRules.get(selectedVisibleIndex));
    }

    private String getRuleCategory(ConditionalRule rule) {
        if (rule == null) {
            return CATEGORY_DEFAULT;
        }
        String category = safe(rule.category).trim();
        return category.isEmpty() ? CATEGORY_DEFAULT : category;
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
            if (hovered) {
                drawRect(x + 2, itemY, x + contextMenuWidth - 2, itemY + contextMenuItemHeight - 1, 0xCC2B5A7C);
            }
            drawString(fontRenderer, item.label, x + 8, itemY + 5, item.enabled ? 0xFFFFFFFF : 0xFF777777);
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
            creatingNew = true;
            clearEditorForNew();
        } else if ("duplicate".equals(item.key)) {
            duplicateSelectedRule();
        } else if ("copy_name".equals(item.key)) {
            copySelectedRuleName();
        } else if ("delete".equals(item.key)) {
            deleteSelectedRule();
        } else if ("move".equals(item.key)) {
            moveSelectedRuleToCurrentCategory();
        } else if ("reload".equals(item.key)) {
            reloadAllRules();
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

        ConditionalRule rule = null;
        if (targetIndex >= 0 && targetIndex < visibleRules.size()) {
            rule = visibleRules.get(targetIndex);
        }

        contextMenuItems.add(new ContextMenuItem("new", "新增规则", true));
        contextMenuItems.add(new ContextMenuItem("reload", "重载规则", true));

        if (rule != null) {
            contextMenuItems.add(new ContextMenuItem("duplicate", "复制当前规则", true));
            contextMenuItems.add(new ContextMenuItem("copy_name", "复制规则名", !isBlank(rule.name)));
            contextMenuItems.add(new ContextMenuItem("delete", "删除规则", true));
            boolean canMove = isConcreteCategory(selectedCategory)
                    && !selectedCategory.equalsIgnoreCase(getRuleCategory(rule));
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

    private void openAddCategoryDialog() {
        mc.setScreen(new GuiTextInput(this, "输入新分组名称", value -> {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) {
                setStatus("§7已取消创建分组", 0xFFB8C7D9);
                return;
            }
            boolean ok = ConditionalExecutionHandler.addCategory(normalized);
            if (ok) {
                selectedCategory = normalized;
                expandedCategories.add(normalized);
                refreshData(true);
                setStatus("§a已创建分组: " + normalized, 0xFF8CFF9E);
            } else {
                setStatus("§c创建分组失败，名称可能重复或无效", 0xFFFF8E8E);
            }
        }));
    }

    private void pasteCategoryFromClipboard() {
        String text = getClipboardString();
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            setStatus("§c剪贴板为空，无法新建分组", 0xFFFF8E8E);
            return;
        }
        boolean ok = ConditionalExecutionHandler.addCategory(normalized);
        if (ok) {
            selectedCategory = normalized;
            expandedCategories.add(normalized);
            refreshData(true);
            setStatus("§a已从剪贴板新建分组: " + normalized, 0xFF8CFF9E);
        } else {
            setStatus("§c新建分组失败，名称可能重复或无效", 0xFFFF8E8E);
        }
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
            boolean ok = ConditionalExecutionHandler.renameCategory(normalized, newName);
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

    private void deleteCategoryByName(String category) {
        String normalized = safe(category).trim();
        if (!isConcreteCategory(normalized)) {
            setStatus("§c该分组不能删除", 0xFFFF8E8E);
            return;
        }
        if (ConditionalExecutionHandler.deleteCategory(normalized)) {
            expandedCategories.remove(normalized);
            selectedCategory = CATEGORY_ALL;
            refreshData(false);
            setStatus("§a已删除分组: " + normalized + "（原分组规则已转为默认分组）", 0xFF8CFF9E);
        } else {
            setStatus("§c删除分组失败", 0xFFFF8E8E);
        }
    }

    private boolean isConcreteCategory(String category) {
        return !isBlank(category)
                && !CATEGORY_ALL.equals(category);
    }

    private int getVisibleTreeRowCount() {
        return Math.max(1, (treeHeight - 26) / TREE_ROW_HEIGHT);
    }

    private int getVisibleCardCount() {
        return Math.max(1, (listHeight - 28) / (CARD_HEIGHT + CARD_GAP));
    }

    private int getTreeRowIndexAt(int mouseY) {
        int contentY = treeY + 22;
        int localY = mouseY - contentY;
        if (localY < 0) {
            return -1;
        }
        int localIndex = localY / TREE_ROW_HEIGHT;
        int actualIndex = treeScrollOffset + localIndex;
        return actualIndex >= 0 && actualIndex < visibleTreeRows.size() ? actualIndex : -1;
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

    private boolean isInTree(int mouseX, int mouseY) {
        return mouseX >= treeX && mouseX <= treeX + treeWidth && mouseY >= treeY && mouseY <= treeY + treeHeight;
    }

    private boolean isInCardList(int mouseX, int mouseY) {
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private boolean isInEditor(int mouseX, int mouseY) {
        return mouseX >= editorX && mouseX <= editorX + editorWidth
                && mouseY >= editorY && mouseY <= editorY + editorHeight;
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
        drawString(this.fontRenderer, collapsed ? ">" : "<", btnX + 4, btnY + 3, 0xFFEAF7FF);
    }

    private int getCollapsedSectionWidth(String shortTitle) {
        int len = shortTitle == null ? 0 : Math.min(2, shortTitle.length());
        int textWidth = this.fontRenderer == null ? 16
                : this.fontRenderer.getStringWidth(shortTitle == null ? "" : shortTitle.substring(0, len));
        return Math.max(34, textWidth + 24);
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void setText(GuiTextField field, String value) {
        if (field != null) {
            field.setText(value == null ? "" : value);
        }
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(safe(text).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(safe(text).trim().replace(',', '.'));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String formatDouble(double value) {
        return COORD_FORMAT.format(value);
    }

    private String boolText(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }

    private String yesNo(boolean yes) {
        return I18n.format(yes ? "gui.common.yes" : "gui.common.no");
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0 || this.fontRenderer == null) {
            return "";
        }
        if (this.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && this.fontRenderer.getStringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
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
}








