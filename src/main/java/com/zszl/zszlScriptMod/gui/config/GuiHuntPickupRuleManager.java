package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler.HuntPickupRule;
import com.zszl.zszlScriptMod.otherfeatures.gui.item.GuiItemFilterExpressionEditor;
import com.zszl.zszlScriptMod.path.InventoryItemFilterExpressionEngine;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class GuiHuntPickupRuleManager extends AbstractThreePaneRuleManager<HuntPickupRule> {

    private static final int BTN_TOGGLE_ENABLED = 1300;
    private static final int BTN_TOGGLE_MODE = 1301;
    private static final int BTN_ADD_EXPRESSION = 1302;
    private static final int BTN_EDIT_EXPRESSION = 1303;
    private static final int BTN_DELETE_EXPRESSION = 1304;
    private static final int BTN_MOVE_EXPRESSION_UP = 1305;
    private static final int BTN_MOVE_EXPRESSION_DOWN = 1306;

    private static final int ROW_NAME = 0;
    private static final int ROW_CATEGORY = 1;
    private static final int ROW_STATE = 2;
    private static final int ROW_PRIORITY_DISTANCE = 3;
    private static final int ROW_EXPRESSION_TOOLBAR_TOP = 4;
    private static final int ROW_EXPRESSION_TOOLBAR_BOTTOM = 5;
    private static final int ROW_EXPRESSION_CARD_BOX = 6;
    private static final int EXPRESSION_BOX_ROWS = 4;
    private static final int EXPRESSION_CARD_HEIGHT = 30;
    private static final int EXPRESSION_CARD_GAP = 4;
    private static final int EXPRESSION_BOX_HEIGHT = EDITOR_ROW_HEIGHT * EXPRESSION_BOX_ROWS - 6;

    private final List<String> transientCategories = new ArrayList<>();
    private final List<String> editorExpressions = new ArrayList<>();

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField maxDistanceField;
    private GuiTextField priorityField;

    private GuiButton btnToggleEnabled;
    private GuiButton btnToggleMode;
    private GuiButton btnAddExpression;
    private GuiButton btnEditExpression;
    private GuiButton btnDeleteExpression;
    private GuiButton btnMoveExpressionUp;
    private GuiButton btnMoveExpressionDown;

    private boolean editorEnabled = true;
    private String editorMode = KillAuraHandler.HUNT_PICKUP_RULE_MODE_ALLOW;
    private int selectedExpressionIndex = -1;
    private int expressionScrollOffset = 0;

    public GuiHuntPickupRuleManager(GuiScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    protected String getScreenTitle() {
        return "Hunt 掉落过滤规则";
    }

    @Override
    protected String getGuideText() {
        return "§7左侧按分组管理规则；右侧用表达式卡片描述要拾取或屏蔽的掉落物。支持 item/name/registry/NBT/tooltip/lore，并额外支持 rarity 与 distance 字段。";
    }

    @Override
    protected String getEntityDisplayName() {
        return "规则";
    }

    @Override
    protected List<HuntPickupRule> getSourceItems() {
        return KillAuraHandler.getHuntPickupRuleSnapshots();
    }

    @Override
    protected List<String> getSourceCategories() {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        categories.addAll(this.transientCategories);
        for (HuntPickupRule rule : allItems) {
            if (rule != null) {
                categories.add(normalizeRuleCategory(rule.category));
            }
        }
        return new ArrayList<>(categories);
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        String normalized = normalizeRuleCategory(category);
        if (normalized.isEmpty() || containsCategory(normalized)) {
            return false;
        }
        this.transientCategories.add(normalized);
        return true;
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        String normalizedOld = normalizeRuleCategory(oldCategory);
        String normalizedNew = normalizeRuleCategory(newCategory);
        if (normalizedNew.isEmpty() || normalizedOld.equalsIgnoreCase(normalizedNew) || containsCategory(normalizedNew)) {
            return false;
        }
        for (int i = 0; i < this.transientCategories.size(); i++) {
            if (normalizeRuleCategory(this.transientCategories.get(i)).equalsIgnoreCase(normalizedOld)) {
                this.transientCategories.set(i, normalizedNew);
            }
        }
        for (HuntPickupRule rule : allItems) {
            if (rule != null && normalizeRuleCategory(rule.category).equalsIgnoreCase(normalizedOld)) {
                rule.category = normalizedNew;
            }
        }
        return true;
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        String normalized = normalizeRuleCategory(category);
        if (normalized.isEmpty()) {
            return false;
        }
        this.transientCategories.removeIf(entry -> normalizeRuleCategory(entry).equalsIgnoreCase(normalized));
        for (HuntPickupRule rule : allItems) {
            if (rule != null && normalizeRuleCategory(rule.category).equalsIgnoreCase(normalized)) {
                rule.category = CATEGORY_DEFAULT;
            }
        }
        return true;
    }

    @Override
    protected void persistChanges() {
        List<HuntPickupRule> rules = new ArrayList<>();
        for (HuntPickupRule rule : allItems) {
            if (rule != null) {
                rules.add(new HuntPickupRule(rule));
            }
        }
        KillAuraHandler.replaceHuntPickupRules(rules);
    }

    @Override
    protected void reloadSource() {
        KillAuraHandler.loadConfig();
    }

    @Override
    protected HuntPickupRule createNewItem() {
        HuntPickupRule rule = new HuntPickupRule();
        rule.name = "新掉落过滤规则";
        rule.category = CATEGORY_DEFAULT;
        rule.enabled = true;
        rule.mode = KillAuraHandler.HUNT_PICKUP_RULE_MODE_ALLOW;
        rule.maxDistance = 0.0F;
        rule.priority = 0;
        return rule;
    }

    @Override
    protected HuntPickupRule copyItem(HuntPickupRule source) {
        return source == null ? createNewItem() : new HuntPickupRule(source);
    }

    @Override
    protected void addItemToSource(HuntPickupRule item) {
        if (item != null) {
            allItems.add(item);
        }
    }

    @Override
    protected void removeItemFromSource(HuntPickupRule item) {
        allItems.remove(item);
    }

    @Override
    protected String getItemName(HuntPickupRule item) {
        return item == null ? "" : safe(item.name);
    }

    @Override
    protected void setItemName(HuntPickupRule item, String name) {
        if (item != null) {
            item.name = safe(name);
        }
    }

    @Override
    protected String getItemCategory(HuntPickupRule item) {
        return item == null ? CATEGORY_DEFAULT : normalizeRuleCategory(item.category);
    }

    @Override
    protected void setItemCategory(HuntPickupRule item, String category) {
        if (item != null) {
            item.category = normalizeRuleCategory(category);
        }
    }

    @Override
    protected void loadEditor(HuntPickupRule item) {
        HuntPickupRule rule = item == null ? createNewItem() : new HuntPickupRule(item);
        setText(nameField, rule.name);
        setText(categoryField, normalizeRuleCategory(rule.category));
        setText(maxDistanceField, formatFloat(rule.maxDistance));
        setText(priorityField, String.valueOf(rule.priority));
        editorEnabled = rule.enabled;
        editorMode = KillAuraHandler.HUNT_PICKUP_RULE_MODE_BLOCK.equalsIgnoreCase(rule.mode)
                ? KillAuraHandler.HUNT_PICKUP_RULE_MODE_BLOCK
                : KillAuraHandler.HUNT_PICKUP_RULE_MODE_ALLOW;
        editorExpressions.clear();
        editorExpressions.addAll(normalizeExpressions(rule.itemFilterExpressions));
        selectedExpressionIndex = editorExpressions.isEmpty() ? -1 : 0;
        expressionScrollOffset = 0;
        clampExpressionSelectionAndScroll();
        layoutAllWidgets();
    }

    @Override
    protected HuntPickupRule buildItemFromEditor(boolean creatingNew, HuntPickupRule selectedItem) {
        HuntPickupRule base = creatingNew || selectedItem == null ? createNewItem() : new HuntPickupRule(selectedItem);
        base.name = safe(nameField.getText()).trim();
        base.category = normalizeRuleCategory(categoryField.getText());
        base.enabled = editorEnabled;
        base.mode = editorMode;
        base.maxDistance = clampFloat(parseFloat(maxDistanceField.getText(), base.maxDistance), 0.0F, 100.0F);
        base.priority = clampInt(parseInt(priorityField.getText(), base.priority), -999, 999);
        base.itemFilterExpressions = normalizeExpressions(editorExpressions);
        base.nameKeyword = "";
        base.itemIdKeyword = "";
        base.requiredNbtTags = new ArrayList<>();
        base.rarityFilters = new ArrayList<>();
        return new HuntPickupRule(base);
    }

    @Override
    protected void applyItemValues(HuntPickupRule target, HuntPickupRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = source.category;
        target.enabled = source.enabled;
        target.mode = source.mode;
        target.maxDistance = source.maxDistance;
        target.priority = source.priority;
        target.itemFilterExpressions = new ArrayList<>(
                source.itemFilterExpressions == null ? new ArrayList<String>() : source.itemFilterExpressions);
        target.nameKeyword = source.nameKeyword;
        target.itemIdKeyword = source.itemIdKeyword;
        target.requiredNbtTags = new ArrayList<>(
                source.requiredNbtTags == null ? new ArrayList<String>() : source.requiredNbtTags);
        target.rarityFilters = new ArrayList<>(
                source.rarityFilters == null ? new ArrayList<String>() : source.rarityFilters);
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(2200);
        categoryField = createField(2201);
        maxDistanceField = createField(2202);
        priorityField = createField(2203);

        btnToggleEnabled = new ToggleGuiButton(BTN_TOGGLE_ENABLED, 0, 0, 100, 20, "", true);
        btnToggleMode = new ThemedButton(BTN_TOGGLE_MODE, 0, 0, 100, 20, "");
        btnAddExpression = new ThemedButton(BTN_ADD_EXPRESSION, 0, 0, 72, 20, "新增");
        btnEditExpression = new ThemedButton(BTN_EDIT_EXPRESSION, 0, 0, 72, 20, "编辑");
        btnDeleteExpression = new ThemedButton(BTN_DELETE_EXPRESSION, 0, 0, 72, 20, "删除");
        btnMoveExpressionUp = new ThemedButton(BTN_MOVE_EXPRESSION_UP, 0, 0, 72, 20, "上移");
        btnMoveExpressionDown = new ThemedButton(BTN_MOVE_EXPRESSION_DOWN, 0, 0, 72, 20, "下移");

        this.buttonList.add(btnToggleEnabled);
        this.buttonList.add(btnToggleMode);
        this.buttonList.add(btnAddExpression);
        this.buttonList.add(btnEditExpression);
        this.buttonList.add(btnDeleteExpression);
        this.buttonList.add(btnMoveExpressionUp);
        this.buttonList.add(btnMoveExpressionDown);
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);

        placeField(nameField, ROW_NAME, editorFieldX, fullFieldWidth);
        placeField(categoryField, ROW_CATEGORY, editorFieldX, fullFieldWidth);
        placeButton(btnToggleEnabled, ROW_STATE, editorFieldX, halfWidth, 20);
        placeButton(btnToggleMode, ROW_STATE, editorFieldX + halfWidth + 10, halfWidth, 20);
        placeField(maxDistanceField, ROW_PRIORITY_DISTANCE, editorFieldX, halfWidth);
        placeField(priorityField, ROW_PRIORITY_DISTANCE, editorFieldX + halfWidth + 10, halfWidth);

        int gap = 6;
        if (fullFieldWidth >= 390) {
            int buttonWidth = Math.max(58, (fullFieldWidth - gap * 4) / 5);
            placeButton(btnAddExpression, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX, buttonWidth, 20);
            placeButton(btnEditExpression, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX + (buttonWidth + gap), buttonWidth,
                    20);
            placeButton(btnDeleteExpression, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX + (buttonWidth + gap) * 2,
                    buttonWidth, 20);
            placeButton(btnMoveExpressionUp, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX + (buttonWidth + gap) * 3,
                    buttonWidth, 20);
            placeButton(btnMoveExpressionDown, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX + (buttonWidth + gap) * 4,
                    Math.max(58, fullFieldWidth - buttonWidth * 4 - gap * 4), 20);
            hideButtonForRow(btnMoveExpressionUp, ROW_EXPRESSION_TOOLBAR_BOTTOM);
            hideButtonForRow(btnMoveExpressionDown, ROW_EXPRESSION_TOOLBAR_BOTTOM);
        } else {
            int topWidth = Math.max(64, (fullFieldWidth - gap * 2) / 3);
            int bottomWidth = Math.max(88, (fullFieldWidth - gap) / 2);
            placeButton(btnAddExpression, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX, topWidth, 20);
            placeButton(btnEditExpression, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX + topWidth + gap, topWidth, 20);
            placeButton(btnDeleteExpression, ROW_EXPRESSION_TOOLBAR_TOP, editorFieldX + (topWidth + gap) * 2,
                    Math.max(64, fullFieldWidth - topWidth * 2 - gap * 2), 20);
            placeButton(btnMoveExpressionUp, ROW_EXPRESSION_TOOLBAR_BOTTOM, editorFieldX, bottomWidth, 20);
            placeButton(btnMoveExpressionDown, ROW_EXPRESSION_TOOLBAR_BOTTOM, editorFieldX + bottomWidth + gap,
                    Math.max(88, fullFieldWidth - bottomWidth - gap), 20);
        }
    }

    @Override
    protected int getEditorTotalRows() {
        return ROW_EXPRESSION_CARD_BOX + EXPRESSION_BOX_ROWS;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        switch (row) {
        case ROW_NAME:
            return "规则名";
        case ROW_CATEGORY:
            return "所属分组";
        case ROW_STATE:
            return "规则状态";
        case ROW_PRIORITY_DISTANCE:
            return "距离 / 优先级";
        case ROW_EXPRESSION_TOOLBAR_TOP:
            return "表达式卡片";
        case ROW_EXPRESSION_TOOLBAR_BOTTOM:
            return "";
        case ROW_EXPRESSION_CARD_BOX:
            return "表达式列表";
        default:
            return "";
        }
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(maxDistanceField);
        fields.add(priorityField);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawField(nameField);
        drawField(categoryField);
        drawField(maxDistanceField);
        drawField(priorityField);
        drawExpressionCardList(mouseX, mouseY);
        drawEditorHintLines();
    }

    @Override
    protected void onAfterEditorMouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) {
            return;
        }
        int expressionIndex = getExpressionIndexAt(mouseX, mouseY);
        if (expressionIndex >= 0) {
            selectedExpressionIndex = expressionIndex;
            updateEditorButtonStates();
        }
    }

    @Override
    protected boolean handleEditorMouseWheel(int mouseX, int mouseY, int wheel) {
        if (!isMouseInsideExpressionList(mouseX, mouseY) || editorExpressions.isEmpty()) {
            return false;
        }
        int maxScroll = getExpressionMaxScroll();
        if (maxScroll <= 0) {
            return false;
        }
        expressionScrollOffset = wheel < 0
                ? Math.min(maxScroll, expressionScrollOffset + 1)
                : Math.max(0, expressionScrollOffset - 1);
        updateEditorButtonStates();
        return true;
    }

    @Override
    protected void drawCard(HuntPickupRule item, int actualIndex, int x, int y, int width, int height,
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
        String modeText = KillAuraHandler.HUNT_PICKUP_RULE_MODE_BLOCK.equalsIgnoreCase(item.mode) ? "§c屏蔽" : "§a允许";
        drawString(fontRenderer, trimToWidth(status + " " + safe(item.name), width - 16), x + 8, y + 5, 0xFFFFFFFF);
        drawString(fontRenderer,
                trimToWidth("类型: " + modeText + "  |  分组: " + normalizeRuleCategory(item.category), width - 16),
                x + 8, y + 19, 0xFFE2F0FF);
        drawString(fontRenderer,
                trimToWidth("表达式: " + buildExpressionSummary(item.itemFilterExpressions), width - 16),
                x + 8, y + 31, 0xFFCFDCE8);
        drawString(fontRenderer,
                trimToWidth("距离≤" + formatFloat(item.maxDistance) + "  |  优先级 " + item.priority, width - 16),
                x + 8, y + 43, 0xFFB8C7D9);
    }

    @Override
    protected void updateEditorButtonStates() {
        if (btnToggleEnabled instanceof ToggleGuiButton) {
            ((ToggleGuiButton) btnToggleEnabled).setEnabledState(editorEnabled);
        }
        btnToggleEnabled.displayString = "启用规则: " + boolText(editorEnabled);
        btnToggleMode.displayString = "规则类型: "
                + (KillAuraHandler.HUNT_PICKUP_RULE_MODE_BLOCK.equals(editorMode) ? "屏蔽/黑名单" : "允许/白名单");

        boolean hasSelection = selectedExpressionIndex >= 0 && selectedExpressionIndex < editorExpressions.size();
        btnEditExpression.enabled = hasSelection;
        btnDeleteExpression.enabled = hasSelection;
        btnMoveExpressionUp.enabled = hasSelection && selectedExpressionIndex > 0;
        btnMoveExpressionDown.enabled = hasSelection && selectedExpressionIndex < editorExpressions.size() - 1;
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_MODE) {
            editorMode = KillAuraHandler.HUNT_PICKUP_RULE_MODE_BLOCK.equals(editorMode)
                    ? KillAuraHandler.HUNT_PICKUP_RULE_MODE_ALLOW
                    : KillAuraHandler.HUNT_PICKUP_RULE_MODE_BLOCK;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_ADD_EXPRESSION) {
            openExpressionEditor(-1);
            return true;
        }
        if (button.id == BTN_EDIT_EXPRESSION) {
            if (selectedExpressionIndex >= 0 && selectedExpressionIndex < editorExpressions.size()) {
                openExpressionEditor(selectedExpressionIndex);
            }
            return true;
        }
        if (button.id == BTN_DELETE_EXPRESSION) {
            if (selectedExpressionIndex >= 0 && selectedExpressionIndex < editorExpressions.size()) {
                editorExpressions.remove(selectedExpressionIndex);
                if (selectedExpressionIndex >= editorExpressions.size()) {
                    selectedExpressionIndex = editorExpressions.isEmpty() ? -1 : editorExpressions.size() - 1;
                }
                clampExpressionSelectionAndScroll();
                updateEditorButtonStates();
            }
            return true;
        }
        if (button.id == BTN_MOVE_EXPRESSION_UP) {
            if (selectedExpressionIndex > 0 && selectedExpressionIndex < editorExpressions.size()) {
                Collections.swap(editorExpressions, selectedExpressionIndex, selectedExpressionIndex - 1);
                selectedExpressionIndex--;
                clampExpressionSelectionAndScroll();
                updateEditorButtonStates();
            }
            return true;
        }
        if (button.id == BTN_MOVE_EXPRESSION_DOWN) {
            if (selectedExpressionIndex >= 0 && selectedExpressionIndex < editorExpressions.size() - 1) {
                Collections.swap(editorExpressions, selectedExpressionIndex, selectedExpressionIndex + 1);
                selectedExpressionIndex++;
                clampExpressionSelectionAndScroll();
                updateEditorButtonStates();
            }
            return true;
        }
        return false;
    }

    @Override
    protected String validateItem(HuntPickupRule item) {
        if (item == null) {
            return "规则不能为空";
        }
        String normalizedName = safe(item.name).trim();
        if (normalizedName.isEmpty()) {
            return "规则名不能为空";
        }
        for (HuntPickupRule existing : allItems) {
            if (existing == null || existing == getSelectedItemOrNull()) {
                continue;
            }
            if (normalizedName.equalsIgnoreCase(safe(existing.name).trim())) {
                return "规则名重复: " + normalizedName;
            }
        }
        if ((item.itemFilterExpressions == null || item.itemFilterExpressions.isEmpty()) && item.maxDistance <= 0.0F) {
            return "至少添加一条表达式，或设置大于 0 的规则距离";
        }
        return null;
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 120, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private void drawField(GuiTextField field) {
        if (field == null) {
            return;
        }
        field.setVisible(field.y >= editorRowStartY && field.y < editorRowStartY + editorVisibleRows * EDITOR_ROW_HEIGHT);
        if (field.getVisible()) {
            field.drawTextBox();
        }
    }

    private void drawExpressionCardList(int mouseX, int mouseY) {
        int boxY = getEditorRowY(ROW_EXPRESSION_CARD_BOX);
        if (boxY < 0) {
            return;
        }
        int boxX = editorFieldX;
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);

        drawRect(boxX, boxY, boxX + boxWidth, boxY + EXPRESSION_BOX_HEIGHT, 0x5520222A);
        drawHorizontalLine(boxX, boxX + boxWidth, boxY, 0xFF4B4B4B);
        drawHorizontalLine(boxX, boxX + boxWidth, boxY + EXPRESSION_BOX_HEIGHT, 0xFF4B4B4B);
        drawVerticalLine(boxX, boxY, boxY + EXPRESSION_BOX_HEIGHT, 0xFF4B4B4B);
        drawVerticalLine(boxX + boxWidth, boxY, boxY + EXPRESSION_BOX_HEIGHT, 0xFF4B4B4B);

        if (editorExpressions.isEmpty()) {
            int textY = boxY + 8;
            drawString(fontRenderer, trimToWidth("§7暂无表达式卡片，点击上方“新增”添加。", boxWidth - 18),
                    boxX + 6, textY, 0xFFB8C7D9);
            textY += 10;
            drawString(fontRenderer, trimToWidth("§7支持字段: name / registry / tooltip / lore / rarity / distance", boxWidth - 18),
                    boxX + 6, textY, 0xFF95A9BF);
            textY += 10;
            drawString(fontRenderer, trimToWidth("§7示例: registryContains(\"diamond\") && distance <= 6", boxWidth - 18),
                    boxX + 6, textY, 0xFF95A9BF);
            return;
        }

        int visibleCount = EXPRESSION_BOX_ROWS;
        int visibleStart = Math.max(0,
                Math.min(expressionScrollOffset, Math.max(0, editorExpressions.size() - visibleCount)));
        for (int i = 0; i < visibleCount; i++) {
            int expressionIndex = visibleStart + i;
            if (expressionIndex >= editorExpressions.size()) {
                break;
            }

            int rowY = boxY + 4 + i * (EXPRESSION_CARD_HEIGHT + EXPRESSION_CARD_GAP);
            boolean hovered = mouseX >= boxX + 3 && mouseX <= boxX + boxWidth - 10
                    && mouseY >= rowY && mouseY <= rowY + EXPRESSION_CARD_HEIGHT;
            boolean selected = expressionIndex == selectedExpressionIndex;
            int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x88222222);
            int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);
            drawRect(boxX + 3, rowY, boxX + boxWidth - 10, rowY + EXPRESSION_CARD_HEIGHT, bg);
            drawHorizontalLine(boxX + 3, boxX + boxWidth - 10, rowY, border);
            drawHorizontalLine(boxX + 3, boxX + boxWidth - 10, rowY + EXPRESSION_CARD_HEIGHT, border);

            String expression = editorExpressions.get(expressionIndex);
            drawString(fontRenderer, "#" + (expressionIndex + 1), boxX + 8, rowY + 6,
                    selected ? 0xFFFFFFFF : 0xFFD8E5F1);
            List<String> wrapped = fontRenderer.listFormattedStringToWidth(expression, Math.max(60, boxWidth - 42));
            int lineY = rowY + 6;
            for (int line = 0; line < wrapped.size() && line < 2; line++) {
                drawString(fontRenderer, wrapped.get(line), boxX + 34, lineY, 0xFFF1F7FC);
                lineY += 10;
            }
        }

        if (editorExpressions.size() > visibleCount) {
            int thumbHeight = Math.max(16,
                    (int) ((visibleCount / (float) editorExpressions.size()) * (EXPRESSION_BOX_HEIGHT - 8)));
            int track = Math.max(1, (EXPRESSION_BOX_HEIGHT - 8) - thumbHeight);
            int maxScroll = Math.max(1, editorExpressions.size() - visibleCount);
            int thumbY = boxY + 4 + (int) ((visibleStart / (float) maxScroll) * track);
            GuiTheme.drawScrollbar(boxX + boxWidth - 7, boxY + 4, 4, EXPRESSION_BOX_HEIGHT - 8, thumbY, thumbHeight);
        }
    }

    private void drawEditorHintLines() {
        int topRowY = getEditorRowY(ROW_EXPRESSION_TOOLBAR_TOP);
        if (topRowY >= 0) {
            int hintY = topRowY - 10;
            drawString(fontRenderer, trimToWidth("§8表达式与“动作库 -> 条件分支 -> 背包物品”语法一致；新增支持 rarity 与 distance。", Math.max(40, editorWidth - 24)),
                    editorFieldX, hintY, 0xFF9FB2C8);
        }
    }

    private void openExpressionEditor(final int editIndex) {
        final boolean editing = editIndex >= 0 && editIndex < editorExpressions.size();
        String title = editing ? "编辑 Hunt 掉落过滤表达式" : "新增 Hunt 掉落过滤表达式";
        String initial = editing ? editorExpressions.get(editIndex) : "";
        this.mc.displayGuiScreen(new GuiItemFilterExpressionEditor(this, title, initial, value -> {
            String expression = value == null ? "" : value.trim();
            if (expression.isEmpty()) {
                setStatus("§c表达式不能为空", 0xFFFF8E8E);
                this.mc.displayGuiScreen(this);
                return;
            }
            try {
                InventoryItemFilterExpressionEngine.validate(expression);
            } catch (RuntimeException e) {
                setStatus("§c表达式无效: " + safe(e.getMessage()), 0xFFFF8E8E);
                this.mc.displayGuiScreen(this);
                return;
            }

            if (editing) {
                editorExpressions.set(editIndex, expression);
                selectedExpressionIndex = editIndex;
            } else {
                editorExpressions.add(expression);
                selectedExpressionIndex = editorExpressions.size() - 1;
            }
            clampExpressionSelectionAndScroll();
            updateEditorButtonStates();
            setStatus("§a已更新表达式卡片", 0xFF8CFF9E);
            this.mc.displayGuiScreen(this);
        }));
    }

    private int getExpressionIndexAt(int mouseX, int mouseY) {
        int boxY = getEditorRowY(ROW_EXPRESSION_CARD_BOX);
        if (boxY < 0) {
            return -1;
        }
        int boxX = editorFieldX;
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + EXPRESSION_BOX_HEIGHT) {
            return -1;
        }
        int localY = mouseY - (boxY + 4);
        if (localY < 0) {
            return -1;
        }
        int slot = localY / (EXPRESSION_CARD_HEIGHT + EXPRESSION_CARD_GAP);
        if (slot < 0 || slot >= EXPRESSION_BOX_ROWS) {
            return -1;
        }
        int index = expressionScrollOffset + slot;
        return index >= 0 && index < editorExpressions.size() ? index : -1;
    }

    private boolean isMouseInsideExpressionList(int mouseX, int mouseY) {
        int boxY = getEditorRowY(ROW_EXPRESSION_CARD_BOX);
        if (boxY < 0) {
            return false;
        }
        int boxX = editorFieldX;
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        return mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + EXPRESSION_BOX_HEIGHT;
    }

    private int getExpressionMaxScroll() {
        return Math.max(0, editorExpressions.size() - EXPRESSION_BOX_ROWS);
    }

    private void clampExpressionSelectionAndScroll() {
        if (editorExpressions.isEmpty()) {
            selectedExpressionIndex = -1;
            expressionScrollOffset = 0;
            return;
        }
        selectedExpressionIndex = Math.max(-1, Math.min(selectedExpressionIndex, editorExpressions.size() - 1));
        expressionScrollOffset = Math.max(0, Math.min(expressionScrollOffset, getExpressionMaxScroll()));
        if (selectedExpressionIndex >= 0 && selectedExpressionIndex < expressionScrollOffset) {
            expressionScrollOffset = selectedExpressionIndex;
        }
        if (selectedExpressionIndex >= 0 && selectedExpressionIndex >= expressionScrollOffset + EXPRESSION_BOX_ROWS) {
            expressionScrollOffset = Math.min(getExpressionMaxScroll(), selectedExpressionIndex - EXPRESSION_BOX_ROWS + 1);
        }
    }

    private void hideButtonForRow(GuiButton button, int row) {
        if (button == null) {
            return;
        }
        button.visible = false;
        button.enabled = false;
        button.x = -2000;
        button.y = -2000;
    }

    private boolean containsCategory(String category) {
        String normalized = normalizeRuleCategory(category);
        for (String existing : getSourceCategories()) {
            if (normalized.equalsIgnoreCase(normalizeRuleCategory(existing))) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizeExpressions(List<String> expressions) {
        List<String> normalized = new ArrayList<>();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (expressions != null) {
            for (String expression : expressions) {
                String text = expression == null ? "" : expression.trim();
                if (text.isEmpty()) {
                    continue;
                }
                try {
                    InventoryItemFilterExpressionEngine.validate(text);
                    unique.add(text);
                } catch (RuntimeException ignored) {
                }
            }
        }
        normalized.addAll(unique);
        return normalized;
    }

    private String normalizeRuleCategory(String value) {
        String normalized = safe(value).trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private String buildExpressionSummary(List<String> expressions) {
        List<String> normalized = normalizeExpressions(expressions);
        return normalized.isEmpty() ? "无" : InventoryItemFilterExpressionEngine.summarizeExpressions(normalized);
    }

    private float parseFloat(String text, float fallback) {
        try {
            return Float.parseFloat(safe(text).trim());
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

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String boolText(boolean enabled) {
        return enabled ? "§a开启" : "§c关闭";
    }
}
