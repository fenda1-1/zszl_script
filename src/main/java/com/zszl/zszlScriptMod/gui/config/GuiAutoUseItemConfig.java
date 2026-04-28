package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.AutoUseItemHandler;
import com.zszl.zszlScriptMod.system.AutoUseItemRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiAutoUseItemConfig extends AbstractThreePaneRuleManager<AutoUseItemRule> {

    private static final int BTN_TOGGLE_ENABLED = 2101;
    private static final int BTN_TOGGLE_USE_MODE = 2102;
    private static final int BTN_TOGGLE_MATCH_MODE = 2103;
    private static final int BTN_TOGGLE_CHANGE_LOCAL_SLOT = 2104;

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField intervalField;
    private GuiTextField switchItemDelayField;
    private GuiTextField switchDelayField;
    private GuiTextField restoreDelayField;

    private GuiButton btnToggleEnabled;
    private GuiButton btnToggleUseMode;
    private GuiButton btnToggleMatchMode;
    private GuiButton btnToggleChangeLocalSlot;

    private boolean editorEnabled = true;
    private boolean editorChangeLocalSlot = false;
    private AutoUseItemRule.UseMode editorUseMode = AutoUseItemRule.UseMode.RIGHT_CLICK;
    private AutoUseItemRule.MatchMode editorMatchMode = AutoUseItemRule.MatchMode.CONTAINS;

    public GuiAutoUseItemConfig(GuiScreen parent) {
        super(parent);
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("gui.autouseitem.manager.title");
    }

    @Override
    protected String getGuideText() {
        return "§7使用指南：左键筛选/折叠分组，右键分组或卡片打开菜单，滚轮可滚动右侧编辑器";
    }

    @Override
    protected String getEntityDisplayName() {
        return "规则";
    }

    @Override
    protected String getAllItemsLabel() {
        return "全部规则";
    }

    @Override
    protected String getEmptyTreeText() {
        return I18n.format("gui.autouseitem.manager.empty");
    }

    @Override
    protected String getEmptyListText() {
        return "该分组下暂无静默使用物品规则";
    }

    @Override
    protected List<AutoUseItemRule> getSourceItems() {
        return AutoUseItemHandler.rules;
    }

    @Override
    protected List<String> getSourceCategories() {
        return AutoUseItemHandler.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return AutoUseItemHandler.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return AutoUseItemHandler.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return AutoUseItemHandler.deleteCategory(category);
    }

    @Override
    protected void persistChanges() {
        AutoUseItemHandler.saveConfig();
    }

    @Override
    protected void reloadSource() {
        AutoUseItemHandler.loadConfig();
    }

    @Override
    protected AutoUseItemRule createNewItem() {
        return new AutoUseItemRule();
    }

    @Override
    protected AutoUseItemRule copyItem(AutoUseItemRule source) {
        AutoUseItemRule copy = new AutoUseItemRule();
        if (source == null) {
            return copy;
        }
        copy.name = source.name;
        copy.category = source.category;
        copy.enabled = source.enabled;
        copy.changeLocalSlot = source.changeLocalSlot;
        copy.useMode = source.useMode;
        copy.matchMode = source.matchMode;
        copy.intervalMs = source.intervalMs;
        copy.switchItemDelayTicks = source.switchItemDelayTicks;
        copy.switchDelayTicks = source.switchDelayTicks;
        copy.restoreDelayTicks = source.restoreDelayTicks;
        copy.lastUseAtMs = 0L;
        return copy;
    }

    @Override
    protected void addItemToSource(AutoUseItemRule item) {
        AutoUseItemHandler.rules.add(item);
    }

    @Override
    protected void removeItemFromSource(AutoUseItemRule item) {
        AutoUseItemHandler.rules.remove(item);
    }

    @Override
    protected String getItemName(AutoUseItemRule item) {
        return item == null ? "" : item.name;
    }

    @Override
    protected void setItemName(AutoUseItemRule item, String name) {
        if (item != null) {
            item.name = name;
        }
    }

    @Override
    protected String getItemCategory(AutoUseItemRule item) {
        return item == null ? CATEGORY_DEFAULT : item.category;
    }

    @Override
    protected void setItemCategory(AutoUseItemRule item, String category) {
        if (item != null) {
            item.category = normalizeCategory(category);
        }
    }

    @Override
    protected void loadEditor(AutoUseItemRule item) {
        AutoUseItemRule rule = item == null ? new AutoUseItemRule() : item;

        setText(nameField, safe(rule.name));
        setText(categoryField, normalizeCategory(rule.category));
        setText(intervalField, String.valueOf(Math.max(10, rule.intervalMs)));
        setText(switchItemDelayField, String.valueOf(Math.max(0, rule.switchItemDelayTicks)));
        setText(switchDelayField, String.valueOf(Math.max(0, rule.switchDelayTicks)));
        setText(restoreDelayField, String.valueOf(Math.max(0, rule.restoreDelayTicks)));

        editorEnabled = rule.enabled;
        editorChangeLocalSlot = rule.changeLocalSlot;
        editorUseMode = rule.useMode == null ? AutoUseItemRule.UseMode.RIGHT_CLICK : rule.useMode;
        editorMatchMode = rule.matchMode == null ? AutoUseItemRule.MatchMode.CONTAINS : rule.matchMode;

        layoutAllWidgets();
    }

    @Override
    protected AutoUseItemRule buildItemFromEditor(boolean creatingNew, AutoUseItemRule selectedItem) {
        AutoUseItemRule base = creatingNew ? new AutoUseItemRule() : copyItem(selectedItem);
        base.name = safe(nameField.getText()).trim();
        base.category = normalizeCategory(categoryField.getText());
        base.enabled = editorEnabled;
        base.changeLocalSlot = editorChangeLocalSlot;
        base.useMode = editorUseMode == null ? AutoUseItemRule.UseMode.RIGHT_CLICK : editorUseMode;
        base.matchMode = editorMatchMode == null ? AutoUseItemRule.MatchMode.CONTAINS : editorMatchMode;
        base.intervalMs = Math.max(10, parseInt(intervalField.getText(), base.intervalMs));
        base.switchItemDelayTicks = Math.max(0, parseInt(switchItemDelayField.getText(), base.switchItemDelayTicks));
        base.switchDelayTicks = Math.max(0, parseInt(switchDelayField.getText(), base.switchDelayTicks));
        base.restoreDelayTicks = Math.max(0, parseInt(restoreDelayField.getText(), base.restoreDelayTicks));
        base.lastUseAtMs = 0L;
        return base;
    }

    @Override
    protected void applyItemValues(AutoUseItemRule target, AutoUseItemRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = normalizeCategory(source.category);
        target.enabled = source.enabled;
        target.changeLocalSlot = source.changeLocalSlot;
        target.useMode = source.useMode;
        target.matchMode = source.matchMode;
        target.intervalMs = Math.max(10, source.intervalMs);
        target.switchItemDelayTicks = Math.max(0, source.switchItemDelayTicks);
        target.switchDelayTicks = Math.max(0, source.switchDelayTicks);
        target.restoreDelayTicks = Math.max(0, source.restoreDelayTicks);
        target.lastUseAtMs = 0L;
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(3101);
        categoryField = createField(3102);
        intervalField = createField(3103);
        switchItemDelayField = createField(3104);
        switchDelayField = createField(3105);
        restoreDelayField = createField(3106);
    }

    @Override
    protected void initAdditionalButtons() {
        btnToggleEnabled = new ThemedButton(BTN_TOGGLE_ENABLED, 0, 0, 100, 20, "");
        btnToggleUseMode = new ThemedButton(BTN_TOGGLE_USE_MODE, 0, 0, 100, 20, "");
        btnToggleMatchMode = new ThemedButton(BTN_TOGGLE_MATCH_MODE, 0, 0, 100, 20, "");
        btnToggleChangeLocalSlot = new ThemedButton(BTN_TOGGLE_CHANGE_LOCAL_SLOT, 0, 0, 100, 20, "");

        this.buttonList.add(btnToggleEnabled);
        this.buttonList.add(btnToggleUseMode);
        this.buttonList.add(btnToggleMatchMode);
        this.buttonList.add(btnToggleChangeLocalSlot);
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(120, right - editorFieldX);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);
        placeButton(btnToggleEnabled, 2, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleUseMode, 3, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleMatchMode, 4, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleChangeLocalSlot, 5, editorFieldX, fullFieldWidth, 20);
        placeField(intervalField, 6, editorFieldX, Math.max(100, fullFieldWidth / 2));
        placeField(switchItemDelayField, 7, editorFieldX, Math.max(100, fullFieldWidth / 2));
        placeField(switchDelayField, 8, editorFieldX, Math.max(100, fullFieldWidth / 2));
        placeField(restoreDelayField, 9, editorFieldX, Math.max(100, fullFieldWidth / 2));
    }

    @Override
    protected void layoutAdditionalButtons() {
        updateEditorButtonStates();
    }

    @Override
    protected int getEditorTotalRows() {
        return 10;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        switch (row) {
            case 0:
                return I18n.format("gui.autouseitem.edit.name");
            case 1:
                return "所属分组";
            case 2:
                return "规则状态";
            case 3:
                return "使用方式";
            case 4:
                return "名称匹配";
            case 5:
                return "更改本地物品槽位";
            case 6:
                return I18n.format("gui.autouseitem.edit.interval");
            case 7:
                return "切换物品延迟 Tick";
            case 8:
                return "切换后延迟 Tick";
            case 9:
                return "右键切回延迟 Tick";
            default:
                return "";
        }
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(intervalField);
        fields.add(switchItemDelayField);
        fields.add(switchDelayField);
        fields.add(restoreDelayField);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorFields();
    }

    @Override
    protected void drawCard(AutoUseItemRule item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered) {
        int cardBottom = y + height;
        int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
        int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

        drawRect(x, y, x + width, cardBottom, bg);
        drawHorizontalLine(x, x + width, y, border);
        drawHorizontalLine(x, x + width, cardBottom, border);
        drawVerticalLine(x, y, cardBottom, border);
        drawVerticalLine(x + width, y, cardBottom, border);

        String status = item.enabled ? "§a✔" : "§c✘";
        drawString(fontRenderer, trimToWidth(status + " " + safe(item.name), width - 12),
                x + 6, y + 5, 0xFFFFFFFF);
        drawString(fontRenderer, trimToWidth("分类: " + normalizeCategory(item.category), width - 12),
                x + 6, y + 18, 0xFFDDDDDD);
        drawString(fontRenderer,
                trimToWidth("使用: " + useModeText(item.useMode) + "  匹配: " + matchModeText(item.matchMode)
                        + "  本地槽: " + onOff(item.changeLocalSlot), width - 12),
                x + 6, y + 31, 0xFFBDBDBD);
        drawString(fontRenderer, trimToWidth("间隔: " + Math.max(10, item.intervalMs) + " ms  切物: "
                        + Math.max(0, item.switchItemDelayTicks) + "t  切后: "
                        + Math.max(0, item.switchDelayTicks) + "t  切回: "
                        + Math.max(0, item.restoreDelayTicks) + "t", width - 12),
                x + 6, y + 44, 0xFFB8C7D9);
    }

    @Override
    protected String validateItem(AutoUseItemRule item) {
        if (item == null) {
            return "规则为空";
        }
        if (isBlank(item.name)) {
            return "规则名称不能为空";
        }
        if (item.intervalMs < 10) {
            return "间隔不能小于 10ms";
        }
        if (item.switchItemDelayTicks < 0) {
            return "切换物品延迟不能小于 0 tick";
        }
        if (item.switchDelayTicks < 0) {
            return "切换后延迟不能小于 0 tick";
        }
        if (item.restoreDelayTicks < 0) {
            return "右键切回延迟不能小于 0 tick";
        }
        return null;
    }

    @Override
    protected void updateEditorButtonStates() {
        if (btnToggleEnabled != null) {
            btnToggleEnabled.displayString = I18n.format("gui.autouseitem.edit.enabled", onOff(editorEnabled));
            btnToggleEnabled.enabled = btnToggleEnabled.visible;
        }
        if (btnToggleUseMode != null) {
            btnToggleUseMode.displayString = I18n.format("gui.autouseitem.edit.use_mode", useModeText(editorUseMode));
            btnToggleUseMode.enabled = btnToggleUseMode.visible;
        }
        if (btnToggleMatchMode != null) {
            btnToggleMatchMode.displayString = I18n.format("gui.autouseitem.edit.match_mode",
                    matchModeText(editorMatchMode));
            btnToggleMatchMode.enabled = btnToggleMatchMode.visible;
        }
        if (btnToggleChangeLocalSlot != null) {
            btnToggleChangeLocalSlot.displayString = "更改本地物品槽位: " + onOff(editorChangeLocalSlot);
            btnToggleChangeLocalSlot.enabled = btnToggleChangeLocalSlot.visible;
        }
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_USE_MODE) {
            editorUseMode = (editorUseMode == AutoUseItemRule.UseMode.LEFT_CLICK)
                    ? AutoUseItemRule.UseMode.RIGHT_CLICK
                    : AutoUseItemRule.UseMode.LEFT_CLICK;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_MATCH_MODE) {
            editorMatchMode = (editorMatchMode == AutoUseItemRule.MatchMode.EXACT)
                    ? AutoUseItemRule.MatchMode.CONTAINS
                    : AutoUseItemRule.MatchMode.EXACT;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_CHANGE_LOCAL_SLOT) {
            editorChangeLocalSlot = !editorChangeLocalSlot;
            updateEditorButtonStates();
            return true;
        }
        return false;
    }

    @Override
    protected int getEditorLabelWidth() {
        return 102;
    }

    @Override
    protected String getListCollapsedTitle() {
        return "列表";
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(safe(text).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String onOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }

    private String useModeText(AutoUseItemRule.UseMode mode) {
        return mode == AutoUseItemRule.UseMode.LEFT_CLICK
                ? I18n.format("gui.autouseitem.mode.left")
                : I18n.format("gui.autouseitem.mode.right");
    }

    private String matchModeText(AutoUseItemRule.MatchMode mode) {
        return mode == AutoUseItemRule.MatchMode.EXACT
                ? I18n.format("gui.autouseitem.match.exact")
                : I18n.format("gui.autouseitem.match.contains");
    }
}



