package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.AutoEscapeHandler;
import com.zszl.zszlScriptMod.system.AutoEscapeRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiAutoEscapeManager extends AbstractThreePaneRuleManager<AutoEscapeRule> {
    private static final int BTN_SELECT_ESCAPE_SEQUENCE = 1000;
    private static final int BTN_TOGGLE_WHITELIST = 1001;
    private static final int BTN_TOGGLE_BLACKLIST = 1002;
    private static final int BTN_TOGGLE_RESTART = 1003;
    private static final int BTN_SELECT_RESTART_SEQUENCE = 1004;
    private static final int BTN_TOGGLE_ENABLED = 1005;
    private static final int BTN_ENTITY_TYPE_BASE = 1100;
    private static final long CARD_DOUBLE_CLICK_WINDOW_MS = 300L;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final String[][] ENTITY_TYPE_OPTIONS = new String[][] {
            { "player", "玩家" },
            { "monster", "怪物" },
            { "neutral", "中立" },
            { "animal", "动物" },
            { "water", "水生" },
            { "ambient", "环境" },
            { "villager", "村民" },
            { "golem", "傀儡" },
            { "tameable", "宠物" },
            { "boss", "首领" },
            { "living", "生物" },
            { "any", "任意" }
    };

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField entityTypesField;
    private GuiTextField detectionRangeField;
    private GuiTextField escapeSequenceField;
    private GuiTextField whitelistField;
    private GuiTextField blacklistField;
    private GuiTextField restartDelayField;
    private GuiTextField restartSequenceField;

    private GuiButton btnSelectEscapeSequence;
    private GuiButton btnToggleWhitelist;
    private GuiButton btnToggleBlacklist;
    private GuiButton btnToggleRestart;
    private GuiButton btnSelectRestartSequence;
    private GuiButton btnToggleEnabled;
    private final List<ToggleGuiButton> entityTypeButtons = new ArrayList<>();
    private final Set<String> editorEntityTypes = new LinkedHashSet<>();

    private boolean editorEnabled = true;
    private boolean editorWhitelistEnabled = false;
    private boolean editorBlacklistEnabled = false;
    private boolean editorRestartEnabled = false;
    private EditorStateSnapshot pendingRestoreState = null;
    private int lastClickedCardIndex = -1;
    private long lastClickedCardTimeMs = 0L;

    public GuiAutoEscapeManager(GuiScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void initGui() {
        EditorStateSnapshot restoreState = pendingRestoreState;
        pendingRestoreState = null;
        super.initGui();
        if (restoreState != null) {
            applyEditorStateSnapshot(restoreState);
        }
    }

    @Override
    protected String getScreenTitle() {
        return "自动逃离规则";
    }

    @Override
    protected String getGuideText() {
        return "§7自动逃离优先级最高：检测到指定实体后会立即打断当前序列并执行逃离序列。";
    }

    @Override
    protected String getEntityDisplayName() {
        return "规则";
    }

    @Override
    protected List<AutoEscapeRule> getSourceItems() {
        return AutoEscapeHandler.getRulesSnapshot();
    }

    @Override
    protected List<String> getSourceCategories() {
        return AutoEscapeHandler.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return AutoEscapeHandler.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return AutoEscapeHandler.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return AutoEscapeHandler.deleteCategory(category);
    }

    @Override
    protected void persistChanges() {
        List<AutoEscapeRule> newRules = new ArrayList<>();
        for (AutoEscapeRule rule : allItems) {
            if (rule == null) {
                continue;
            }
            AutoEscapeRule copy = rule.copy();
            copy.normalize();
            newRules.add(copy);
        }
        AutoEscapeHandler.replaceAllRules(newRules);
    }

    @Override
    protected void reloadSource() {
        AutoEscapeHandler.loadConfig();
    }

    @Override
    protected AutoEscapeRule createNewItem() {
        return new AutoEscapeRule();
    }

    @Override
    protected AutoEscapeRule copyItem(AutoEscapeRule source) {
        return source == null ? new AutoEscapeRule() : source.copy();
    }

    @Override
    protected void addItemToSource(AutoEscapeRule item) {
        if (item != null) {
            allItems.add(item);
        }
    }

    @Override
    protected void removeItemFromSource(AutoEscapeRule item) {
        allItems.remove(item);
    }

    @Override
    protected String getItemName(AutoEscapeRule item) {
        return item == null ? "" : safe(item.name);
    }

    @Override
    protected void setItemName(AutoEscapeRule item, String name) {
        if (item != null) {
            item.name = safe(name);
        }
    }

    @Override
    protected String getItemCategory(AutoEscapeRule item) {
        return item == null ? CATEGORY_DEFAULT : normalizeCategory(item.category);
    }

    @Override
    protected void setItemCategory(AutoEscapeRule item, String category) {
        if (item != null) {
            item.category = normalizeCategory(category);
        }
    }

    @Override
    protected void loadEditor(AutoEscapeRule item) {
        AutoEscapeRule model = item == null ? new AutoEscapeRule() : item.copy();
        model.normalize();

        setText(nameField, safe(model.name));
        setText(categoryField, normalizeCategory(model.category));
        setEditorEntityTypes(model.entityTypes);
        setText(entityTypesField, buildEntityTypeSummary());
        setText(detectionRangeField, formatDouble(model.detectionRange));
        setText(escapeSequenceField, safe(model.escapeSequenceName));
        setText(whitelistField, joinList(model.nameWhitelist));
        setText(blacklistField, joinList(model.nameBlacklist));
        setText(restartDelayField, String.valueOf(Math.max(0, model.restartDelaySeconds)));
        setText(restartSequenceField, safe(model.restartSequenceName));

        editorEnabled = model.enabled;
        editorWhitelistEnabled = model.enableNameWhitelist;
        editorBlacklistEnabled = model.enableNameBlacklist;
        editorRestartEnabled = model.restartEnabled;

        clampEditorScroll();
        layoutAllWidgets();
    }

    @Override
    protected AutoEscapeRule buildItemFromEditor(boolean creatingNew, AutoEscapeRule selectedItem) {
        AutoEscapeRule base = creatingNew || selectedItem == null ? new AutoEscapeRule() : selectedItem.copy();
        base.name = safe(nameField.getText()).trim();
        base.category = normalizeCategory(categoryField.getText());
        base.entityTypes = new ArrayList<>(editorEntityTypes);
        base.detectionRange = parseDouble(detectionRangeField.getText(), base.detectionRange);
        base.escapeSequenceName = safe(escapeSequenceField.getText()).trim();

        base.enableNameWhitelist = editorWhitelistEnabled;
        base.nameWhitelist = parseList(whitelistField.getText());

        base.enableNameBlacklist = editorBlacklistEnabled;
        base.nameBlacklist = parseList(blacklistField.getText());

        base.restartEnabled = editorRestartEnabled;
        base.restartDelaySeconds = parseInt(restartDelayField.getText(), base.restartDelaySeconds);
        base.restartSequenceName = safe(restartSequenceField.getText()).trim();

        base.enabled = editorEnabled;
        base.normalize();
        return base;
    }

    @Override
    protected void applyItemValues(AutoEscapeRule target, AutoEscapeRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = source.category;
        target.enabled = source.enabled;
        target.entityTypes = new ArrayList<>(
                source.entityTypes == null ? Collections.<String>emptyList() : source.entityTypes);
        target.detectionRange = source.detectionRange;
        target.enableNameWhitelist = source.enableNameWhitelist;
        target.nameWhitelist = new ArrayList<>(
                source.nameWhitelist == null ? Collections.<String>emptyList() : source.nameWhitelist);
        target.enableNameBlacklist = source.enableNameBlacklist;
        target.nameBlacklist = new ArrayList<>(
                source.nameBlacklist == null ? Collections.<String>emptyList() : source.nameBlacklist);
        target.escapeSequenceName = source.escapeSequenceName;
        target.restartEnabled = source.restartEnabled;
        target.restartDelaySeconds = source.restartDelaySeconds;
        target.restartSequenceName = source.restartSequenceName;
        target.normalize();
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(2100);
        categoryField = createField(2101);
        entityTypesField = createField(2102);
        detectionRangeField = createField(2103);
        escapeSequenceField = createField(2104);
        whitelistField = createField(2105);
        blacklistField = createField(2106);
        restartDelayField = createField(2107);
        restartSequenceField = createField(2108);

        entityTypesField.setEnabled(false);
        escapeSequenceField.setEnabled(false);
        restartSequenceField.setEnabled(false);

        btnSelectEscapeSequence = createButton(BTN_SELECT_ESCAPE_SEQUENCE, "选择");
        btnToggleWhitelist = createButton(BTN_TOGGLE_WHITELIST, "");
        btnToggleBlacklist = createButton(BTN_TOGGLE_BLACKLIST, "");
        btnToggleRestart = createButton(BTN_TOGGLE_RESTART, "");
        btnSelectRestartSequence = createButton(BTN_SELECT_RESTART_SEQUENCE, "选择");
        btnToggleEnabled = createButton(BTN_TOGGLE_ENABLED, "");

        this.buttonList.add(btnSelectEscapeSequence);
        this.buttonList.add(btnToggleWhitelist);
        this.buttonList.add(btnToggleBlacklist);
        this.buttonList.add(btnToggleRestart);
        this.buttonList.add(btnSelectRestartSequence);
        this.buttonList.add(btnToggleEnabled);

        entityTypeButtons.clear();
        for (int i = 0; i < ENTITY_TYPE_OPTIONS.length; i++) {
            ToggleGuiButton button = new ToggleGuiButton(BTN_ENTITY_TYPE_BASE + i, 0, 0, 80, 20, "", false);
            entityTypeButtons.add(button);
            this.buttonList.add(button);
        }
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);
        placeField(entityTypesField, 2, editorFieldX, fullFieldWidth);

        int typeColumns = getEntityTypeGridColumns(fullFieldWidth);
        int typeGap = 6;
        int typeButtonWidth = Math.max(64, (fullFieldWidth - typeGap * (typeColumns - 1)) / typeColumns);
        int typeIndex = 0;
        for (int row = 0; row < getEntityTypeGridRowCount(); row++) {
            int editorRow = getEntityTypeStartRow() + row;
            for (int col = 0; col < typeColumns; col++) {
                if (typeIndex >= entityTypeButtons.size()) {
                    break;
                }
                int buttonX = editorFieldX + col * (typeButtonWidth + typeGap);
                placeButton(entityTypeButtons.get(typeIndex), editorRow, buttonX, typeButtonWidth, 20);
                typeIndex++;
            }
        }

        placeField(detectionRangeField, getDetectionRangeRow(), editorFieldX, halfWidth);

        placeField(escapeSequenceField, getEscapeSequenceRow(), editorFieldX, Math.max(60, fullFieldWidth - 70));
        placeButton(btnSelectEscapeSequence, getEscapeSequenceRow(),
                escapeSequenceField.x + escapeSequenceField.width + 6, 64, 20);

        placeButton(btnToggleWhitelist, getWhitelistToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(whitelistField, getWhitelistFieldRow(), editorFieldX, fullFieldWidth);

        placeButton(btnToggleBlacklist, getBlacklistToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(blacklistField, getBlacklistFieldRow(), editorFieldX, fullFieldWidth);

        placeButton(btnToggleRestart, getRestartToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(restartDelayField, getRestartDelayRow(), editorFieldX, halfWidth);

        placeField(restartSequenceField, getRestartSequenceRow(), editorFieldX, Math.max(60, fullFieldWidth - 70));
        placeButton(btnSelectRestartSequence, getRestartSequenceRow(),
                restartSequenceField.x + restartSequenceField.width + 6, 64, 20);

        placeButton(btnToggleEnabled, getEnabledRow(), editorFieldX, fullFieldWidth, 20);
    }

    @Override
    protected int getEditorTotalRows() {
        return getEnabledRow() + 1;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        if (row == 0) {
            return "规则名";
        }
        if (row == 1) {
            return "所属分组";
        }
        if (row == 2) {
            return "已选实体类型";
        }
        if (row == getEntityTypeStartRow()) {
            return "选择实体类型";
        }
        if (row > getEntityTypeStartRow() && row <= getEntityTypeEndRow()) {
            return "";
        }
        if (row == getDetectionRangeRow()) {
            return "检测范围";
        }
        if (row == getEscapeSequenceRow()) {
            return "逃离序列";
        }
        if (row == getWhitelistToggleRow()) {
            return "名称白名单";
        }
        if (row == getWhitelistFieldRow()) {
            return "白名单关键字";
        }
        if (row == getBlacklistToggleRow()) {
            return "名称黑名单";
        }
        if (row == getBlacklistFieldRow()) {
            return "黑名单关键字";
        }
        if (row == getRestartToggleRow()) {
            return "重启功能";
        }
        if (row == getRestartDelayRow()) {
            return "重启计时";
        }
        if (row == getRestartSequenceRow()) {
            return "后续序列";
        }
        if (row == getEnabledRow()) {
            return "启用";
        }
        return "";
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(entityTypesField);
        fields.add(detectionRangeField);
        fields.add(escapeSequenceField);
        fields.add(whitelistField);
        fields.add(blacklistField);
        fields.add(restartDelayField);
        fields.add(restartSequenceField);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorFields();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawEditorTooltip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!listCollapsed && mouseButton == 0 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleItems.size()) {
                long now = System.currentTimeMillis();
                boolean isDoubleClick = actual == lastClickedCardIndex
                        && (now - lastClickedCardTimeMs) <= CARD_DOUBLE_CLICK_WINDOW_MS;

                selectedVisibleIndex = actual;
                creatingNew = false;
                AutoEscapeRule selected = visibleItems.get(actual);
                loadEditor(selected);
                onSelectionChanged(selected);

                if (isDoubleClick) {
                    toggleRuleEnabledFromCard(selected);
                    lastClickedCardIndex = -1;
                    lastClickedCardTimeMs = 0L;
                } else {
                    lastClickedCardIndex = actual;
                    lastClickedCardTimeMs = now;
                }
                return;
            } else {
                lastClickedCardIndex = -1;
                lastClickedCardTimeMs = 0L;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawCard(AutoEscapeRule item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered) {
        int cardBottom = y + height;
        int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
        int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

        drawRect(x, y, x + width, cardBottom, bg);
        drawHorizontalLine(x, x + width, y, border);
        drawHorizontalLine(x, x + width, cardBottom, border);
        drawVerticalLine(x, y, cardBottom, border);
        drawVerticalLine(x + width, y, cardBottom, border);

        String status = item.enabled ? "§a✔" : "§7○";
        drawString(fontRenderer, trimToWidth(status + " " + safe(item.name), width - 16), x + 8, y + 5, 0xFFFFFFFF);
        drawString(fontRenderer,
                trimToWidth("分类: " + normalizeCategory(item.category) + " | 范围: " + formatDouble(item.detectionRange),
                        width - 16),
                x + 8, y + 19, 0xFFDDDDDD);
        drawString(fontRenderer,
                trimToWidth("实体: " + joinEntityTypeLabels(item.entityTypes), width - 16),
                x + 8, y + 31, 0xFFBDBDBD);

        String restartText = item.restartEnabled
                ? "重启: " + Math.max(0, item.restartDelaySeconds) + "s -> " + safe(item.restartSequenceName)
                : "重启: 关闭";
        drawString(fontRenderer,
                trimToWidth("逃离: " + safe(item.escapeSequenceName) + " | " + restartText, width - 16),
                x + 8, y + 43, 0xFFB8C7D9);
    }

    @Override
    protected void updateEditorButtonStates() {
        setText(entityTypesField, buildEntityTypeSummary());

        if (btnToggleWhitelist != null) {
            btnToggleWhitelist.displayString = "启用名称白名单(包含即可): " + yesNo(editorWhitelistEnabled);
            btnToggleWhitelist.enabled = btnToggleWhitelist.visible;
        }
        if (btnToggleBlacklist != null) {
            btnToggleBlacklist.displayString = "启用名称黑名单(包含即可): " + yesNo(editorBlacklistEnabled);
            btnToggleBlacklist.enabled = btnToggleBlacklist.visible;
        }
        if (btnToggleRestart != null) {
            btnToggleRestart.displayString = "启用重启功能: " + yesNo(editorRestartEnabled);
            btnToggleRestart.enabled = btnToggleRestart.visible;
        }
        if (btnToggleEnabled != null) {
            btnToggleEnabled.displayString = "启用: " + boolText(editorEnabled);
            btnToggleEnabled.enabled = btnToggleEnabled.visible;
        }

        for (int i = 0; i < entityTypeButtons.size(); i++) {
            ToggleGuiButton button = entityTypeButtons.get(i);
            String token = ENTITY_TYPE_OPTIONS[i][0];
            String label = ENTITY_TYPE_OPTIONS[i][1];
            boolean selected = editorEntityTypes.contains(token);
            button.setEnabledState(selected);
            button.displayString = selected ? "§a" + label : "§7" + label;
            button.enabled = button.visible;
        }

        boolean hasSelected = !creatingNew && selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleItems.size();
        if (btnDelete != null) {
            btnDelete.enabled = hasSelected;
        }

        if (btnSelectEscapeSequence != null) {
            btnSelectEscapeSequence.enabled = btnSelectEscapeSequence.visible;
        }
        if (btnSelectRestartSequence != null) {
            btnSelectRestartSequence.enabled = btnSelectRestartSequence.visible && editorRestartEnabled;
        }

        if (whitelistField != null) {
            whitelistField.setEnabled(editorWhitelistEnabled);
        }
        if (blacklistField != null) {
            blacklistField.setEnabled(editorBlacklistEnabled);
        }
        if (restartDelayField != null) {
            restartDelayField.setEnabled(editorRestartEnabled);
        }
        if (entityTypesField != null) {
            entityTypesField.setEnabled(false);
        }
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button.id >= BTN_ENTITY_TYPE_BASE && button.id < BTN_ENTITY_TYPE_BASE + ENTITY_TYPE_OPTIONS.length) {
            int index = button.id - BTN_ENTITY_TYPE_BASE;
            String token = ENTITY_TYPE_OPTIONS[index][0];
            if (editorEntityTypes.contains(token)) {
                editorEntityTypes.remove(token);
            } else {
                editorEntityTypes.add(token);
            }
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_SELECT_ESCAPE_SEQUENCE) {
            EditorStateSnapshot snapshot = captureEditorStateSnapshot();
            mc.setScreen(new GuiSequenceSelector(this, selected -> {
                snapshot.escapeSequenceName = safe(selected);
                pendingRestoreState = snapshot;
                mc.setScreen(this);
            }));
            return true;
        }
        if (button.id == BTN_TOGGLE_WHITELIST) {
            editorWhitelistEnabled = !editorWhitelistEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_BLACKLIST) {
            editorBlacklistEnabled = !editorBlacklistEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_RESTART) {
            editorRestartEnabled = !editorRestartEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_SELECT_RESTART_SEQUENCE) {
            EditorStateSnapshot snapshot = captureEditorStateSnapshot();
            mc.setScreen(new GuiSequenceSelector(this, selected -> {
                snapshot.restartSequenceName = safe(selected);
                pendingRestoreState = snapshot;
                mc.setScreen(this);
            }));
            return true;
        }
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            layoutAllWidgets();
            return true;
        }
        return false;
    }

    @Override
    protected String validateItem(AutoEscapeRule item) {
        if (item == null) {
            return "规则数据无效";
        }
        if (isBlank(item.name)) {
            return "规则名称不能为空";
        }
        if (item.entityTypes == null || item.entityTypes.isEmpty()) {
            return "请至少选择一种实体类型";
        }
        if (item.detectionRange <= 0) {
            return "检测范围必须大于 0";
        }
        if (isBlank(item.escapeSequenceName)) {
            return "逃离序列不能为空";
        }
        if (item.restartEnabled && isBlank(item.restartSequenceName)) {
            return "已启用重启功能时，后续序列不能为空";
        }
        return null;
    }

    private void toggleRuleEnabledFromCard(AutoEscapeRule rule) {
        if (rule == null) {
            return;
        }

        String ruleName = safe(rule.name);
        rule.enabled = !rule.enabled;
        persistChanges();
        refreshData(true);
        selectByItemName(ruleName);

        if (rule.enabled) {
            setStatus("§a已快速启用规则: " + ruleName, 0xFF8CFF9E);
        } else {
            setStatus("§e已快速关闭规则: " + ruleName, 0xFFFFD27F);
        }
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private GuiButton createButton(int id, String text) {
        return new com.zszl.zszlScriptMod.gui.components.ThemedButton(id, 0, 0, 100, 20, text);
    }

    private void drawEditorTooltip(int mouseX, int mouseY) {
        if (!isInEditor(mouseX, mouseY)) {
            return;
        }

        String text = null;
        for (int row = 0; row < getEditorTotalRows(); row++) {
            int y = getEditorRowY(row);
            if (y >= 0 && mouseY >= y - 2 && mouseY <= y + 18) {
                text = getRowDescription(row);
                break;
            }
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }
        drawHoveringText(this.fontRenderer.listFormattedStringToWidth(text, 260), mouseX, mouseY);
    }

    private String getRowDescription(int row) {
        if (row == 0) {
            return "规则名：用于区分不同自动逃离方案。";
        }
        if (row == 1) {
            return "所属分组：用于左侧规则树分类管理。";
        }
        if (row == 2) {
            return "已选实体类型：这里会汇总当前勾选的实体类型。";
        }
        if (row >= getEntityTypeStartRow() && row <= getEntityTypeEndRow()) {
            return "实体类型：直接点击按钮进行多选，不再需要手动输入。界面会自动换行，确保不会超出编辑器宽度。";
        }
        if (row == getDetectionRangeRow()) {
            return "检测范围：当匹配实体进入当前玩家多少格范围内时，立即触发自动逃离。";
        }
        if (row == getEscapeSequenceRow()) {
            return "逃离序列：检测到目标后优先执行的序列，会立刻终止当前正在执行的序列。选择后会保留当前编辑位置。";
        }
        if (row == getWhitelistToggleRow()) {
            return "名称白名单：单独启用后，仅当实体名称包含这里的任意关键字时才触发。";
        }
        if (row == getWhitelistFieldRow()) {
            return "白名单关键字：多个关键字用逗号分隔，匹配规则为“包含即可”。";
        }
        if (row == getBlacklistToggleRow()) {
            return "名称黑名单：单独启用后，实体名称只要包含这里的任意关键字就不会触发。";
        }
        if (row == getBlacklistFieldRow()) {
            return "黑名单关键字：多个关键字用逗号分隔，匹配规则为“包含即可”。";
        }
        if (row == getRestartToggleRow()) {
            return "重启功能：逃离序列执行完成后，等待指定秒数，再执行一个新的后续序列。";
        }
        if (row == getRestartDelayRow()) {
            return "重启计时：逃离完成后等待多久再执行后续序列；不是恢复原序列，而是执行你指定的新序列。";
        }
        if (row == getRestartSequenceRow()) {
            return "后续序列：重启功能启用后要执行的新序列。选择后会保留当前滚动和编辑位置。";
        }
        if (row == getEnabledRow()) {
            return "启用：关闭后该规则不会参与检测。";
        }
        return "";
    }

    private void setEditorEntityTypes(List<String> types) {
        editorEntityTypes.clear();
        if (types != null) {
            for (String type : types) {
                String normalized = normalizeTypeToken(type);
                if (!normalized.isEmpty()) {
                    editorEntityTypes.add(normalized);
                }
            }
        }
    }

    private String normalizeTypeToken(String type) {
        String normalized = safe(type).trim().toLowerCase();
        for (String[] option : ENTITY_TYPE_OPTIONS) {
            if (option[0].equalsIgnoreCase(normalized)) {
                return option[0];
            }
        }
        return "";
    }

    private String buildEntityTypeSummary() {
        if (editorEntityTypes.isEmpty()) {
            return "未选择";
        }
        List<String> labels = new ArrayList<>();
        for (String[] option : ENTITY_TYPE_OPTIONS) {
            if (editorEntityTypes.contains(option[0])) {
                labels.add(option[1]);
            }
        }
        return joinList(labels);
    }

    private String joinEntityTypeLabels(List<String> types) {
        if (types == null || types.isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String type : types) {
            String label = getEntityTypeLabel(type);
            labels.add(label.isEmpty() ? safe(type) : label);
        }
        return joinList(labels);
    }

    private String getEntityTypeLabel(String token) {
        String normalized = normalizeTypeToken(token);
        for (String[] option : ENTITY_TYPE_OPTIONS) {
            if (option[0].equalsIgnoreCase(normalized)) {
                return option[1];
            }
        }
        return "";
    }

    private List<String> parseList(String text) {
        List<String> result = new ArrayList<>();
        String normalized = safe(text).replace('，', ',').replace('\n', ',').replace('\r', ',');
        for (String part : normalized.split(",")) {
            String item = safe(part).trim();
            if (!item.isEmpty() && !containsIgnoreCase(result, item)) {
                result.add(item);
            }
        }
        return result;
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(safe(text).trim().replace(',', '.'));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(safe(text).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String formatDouble(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    private String yesNo(boolean yes) {
        return yes ? "是" : "否";
    }

    private String boolText(boolean enabled) {
        return enabled ? "开" : "关";
    }

    private int getEntityTypeGridColumns(int fullFieldWidth) {
        if (fullFieldWidth >= 300) {
            return 3;
        }
        if (fullFieldWidth >= 190) {
            return 2;
        }
        return 1;
    }

    private int getEntityTypeGridRowCount() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int columns = getEntityTypeGridColumns(fullFieldWidth);
        return Math.max(1, (entityTypeButtons.size() + columns - 1) / columns);
    }

    private int getEntityTypeStartRow() {
        return 3;
    }

    private int getEntityTypeEndRow() {
        return getEntityTypeStartRow() + getEntityTypeGridRowCount() - 1;
    }

    private int getDetectionRangeRow() {
        return getEntityTypeEndRow() + 1;
    }

    private int getEscapeSequenceRow() {
        return getDetectionRangeRow() + 1;
    }

    private int getWhitelistToggleRow() {
        return getEscapeSequenceRow() + 1;
    }

    private int getWhitelistFieldRow() {
        return getWhitelistToggleRow() + 1;
    }

    private int getBlacklistToggleRow() {
        return getWhitelistFieldRow() + 1;
    }

    private int getBlacklistFieldRow() {
        return getBlacklistToggleRow() + 1;
    }

    private int getRestartToggleRow() {
        return getBlacklistFieldRow() + 1;
    }

    private int getRestartDelayRow() {
        return getRestartToggleRow() + 1;
    }

    private int getRestartSequenceRow() {
        return getRestartDelayRow() + 1;
    }

    private int getEnabledRow() {
        return getRestartSequenceRow() + 1;
    }

    private EditorStateSnapshot captureEditorStateSnapshot() {
        EditorStateSnapshot snapshot = new EditorStateSnapshot();
        snapshot.selectedItemName = getSelectedItemName();
        snapshot.creatingNew = creatingNew;
        snapshot.editorScrollOffset = editorScrollOffset;
        snapshot.name = nameField == null ? "" : safe(nameField.getText());
        snapshot.category = categoryField == null ? "" : safe(categoryField.getText());
        snapshot.entityTypes.addAll(editorEntityTypes);
        snapshot.detectionRange = detectionRangeField == null ? "" : safe(detectionRangeField.getText());
        snapshot.escapeSequenceName = escapeSequenceField == null ? "" : safe(escapeSequenceField.getText());
        snapshot.whitelistEnabled = editorWhitelistEnabled;
        snapshot.whitelist = whitelistField == null ? "" : safe(whitelistField.getText());
        snapshot.blacklistEnabled = editorBlacklistEnabled;
        snapshot.blacklist = blacklistField == null ? "" : safe(blacklistField.getText());
        snapshot.restartEnabled = editorRestartEnabled;
        snapshot.restartDelay = restartDelayField == null ? "" : safe(restartDelayField.getText());
        snapshot.restartSequenceName = restartSequenceField == null ? "" : safe(restartSequenceField.getText());
        snapshot.enabled = editorEnabled;
        return snapshot;
    }

    private void applyEditorStateSnapshot(EditorStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (snapshot.creatingNew) {
            creatingNew = true;
            selectedVisibleIndex = -1;
            clearEditorForNew();
        } else if (!isBlank(snapshot.selectedItemName)) {
            selectByItemName(snapshot.selectedItemName);
        }

        setText(nameField, snapshot.name);
        setText(categoryField, snapshot.category);
        editorEntityTypes.clear();
        editorEntityTypes.addAll(snapshot.entityTypes);
        setText(entityTypesField, buildEntityTypeSummary());
        setText(detectionRangeField, snapshot.detectionRange);
        setText(escapeSequenceField, snapshot.escapeSequenceName);
        setText(whitelistField, snapshot.whitelist);
        setText(blacklistField, snapshot.blacklist);
        setText(restartDelayField, snapshot.restartDelay);
        setText(restartSequenceField, snapshot.restartSequenceName);

        editorEnabled = snapshot.enabled;
        editorWhitelistEnabled = snapshot.whitelistEnabled;
        editorBlacklistEnabled = snapshot.blacklistEnabled;
        editorRestartEnabled = snapshot.restartEnabled;
        editorScrollOffset = snapshot.editorScrollOffset;
        clampEditorScroll();
        layoutAllWidgets();
    }

    private static class EditorStateSnapshot {
        private String selectedItemName = "";
        private boolean creatingNew = false;
        private int editorScrollOffset = 0;
        private String name = "";
        private String category = "";
        private final Set<String> entityTypes = new LinkedHashSet<>();
        private String detectionRange = "";
        private String escapeSequenceName = "";
        private boolean whitelistEnabled = false;
        private String whitelist = "";
        private boolean blacklistEnabled = false;
        private String blacklist = "";
        private boolean restartEnabled = false;
        private String restartDelay = "";
        private String restartSequenceName = "";
        private boolean enabled = true;
    }
}



