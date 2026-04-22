package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.BlockReplacementHandler;
import com.zszl.zszlScriptMod.system.BlockReplacementRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiBlockReplacementConfig extends AbstractThreePaneRuleManager<BlockReplacementRule> {

    private static final int BTN_TOGGLE_ENABLED = 2201;
    private static final int BTN_TOGGLE_HIGHLIGHT = 2202;
    private static final int BTN_TOGGLE_SOLID = 2203;
    private static final int BTN_SELECT_REGION = 2204;
    private static final int BTN_SCAN_BLOCKS = 2205;
    private static final int BTN_ADD_REPLACEMENT = 2206;
    private static final int BTN_EDIT_REPLACEMENT = 2207;
    private static final int BTN_DELETE_REPLACEMENT = 2208;

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField corner1Field;
    private GuiTextField corner2Field;

    private GuiButton btnToggleEnabled;
    private GuiButton btnToggleHighlight;
    private GuiButton btnToggleSolid;
    private GuiButton btnSelectRegion;
    private GuiButton btnScanBlocks;
    private GuiButton btnAddReplacement;
    private GuiButton btnEditReplacement;
    private GuiButton btnDeleteReplacement;

    private BlockReplacementRule workingRule;
    private boolean editorEnabled = true;
    private boolean editorHighlightReplacedBlocks = true;
    private boolean editorUseSolidCollision = false;
    private int selectedReplacementIndex = -1;

    public GuiBlockReplacementConfig(GuiScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("gui.blockreplace.manager.title");
    }

    @Override
    protected String getGuideText() {
        return "§7使用指南：左键筛选/折叠分组，右键分组或卡片打开菜单，右侧支持区域设置与替换条目编辑";
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
        return I18n.format("gui.blockreplace.manager.empty");
    }

    @Override
    protected String getEmptyListText() {
        return "该分组下暂无区域方块替换规则";
    }

    @Override
    protected List<BlockReplacementRule> getSourceItems() {
        return BlockReplacementHandler.rules;
    }

    @Override
    protected List<String> getSourceCategories() {
        return BlockReplacementHandler.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return BlockReplacementHandler.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return BlockReplacementHandler.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return BlockReplacementHandler.deleteCategory(category);
    }

    @Override
    protected void persistChanges() {
        BlockReplacementHandler.saveConfig();
    }

    @Override
    protected void reloadSource() {
        BlockReplacementHandler.loadConfig();
    }

    @Override
    protected BlockReplacementRule createNewItem() {
        BlockReplacementRule rule = new BlockReplacementRule();
        rule.category = CATEGORY_DEFAULT;
        return rule;
    }

    @Override
    protected BlockReplacementRule copyItem(BlockReplacementRule source) {
        BlockReplacementRule copy = new BlockReplacementRule();
        if (source == null) {
            copy.category = CATEGORY_DEFAULT;
            return copy;
        }
        copy.name = source.name;
        copy.category = normalizeCategory(source.category);
        copy.enabled = source.enabled;
        copy.highlightReplacedBlocks = source.highlightReplacedBlocks;
        copy.useSolidCollision = source.useSolidCollision;
        copy.corner1X = source.corner1X;
        copy.corner1Y = source.corner1Y;
        copy.corner1Z = source.corner1Z;
        copy.corner2X = source.corner2X;
        copy.corner2Y = source.corner2Y;
        copy.corner2Z = source.corner2Z;
        copy.replacements = deepCopyReplacements(source.replacements);
        copy.dirty = true;
        return copy;
    }

    @Override
    protected void addItemToSource(BlockReplacementRule item) {
        BlockReplacementHandler.rules.add(item);
    }

    @Override
    protected void removeItemFromSource(BlockReplacementRule item) {
        BlockReplacementHandler.rules.remove(item);
    }

    @Override
    protected String getItemName(BlockReplacementRule item) {
        return item == null ? "" : item.name;
    }

    @Override
    protected void setItemName(BlockReplacementRule item, String name) {
        if (item != null) {
            item.name = name;
        }
    }

    @Override
    protected String getItemCategory(BlockReplacementRule item) {
        return item == null ? CATEGORY_DEFAULT : item.category;
    }

    @Override
    protected void setItemCategory(BlockReplacementRule item, String category) {
        if (item != null) {
            item.category = normalizeCategory(category);
        }
    }

    @Override
    protected void loadEditor(BlockReplacementRule item) {
        workingRule = copyItem(item == null ? createNewItem() : item);

        setText(nameField, safe(workingRule.name));
        setText(categoryField, normalizeCategory(workingRule.category));
        setText(corner1Field, formatCorner1(workingRule));
        setText(corner2Field, formatCorner2(workingRule));

        editorEnabled = workingRule.enabled;
        editorHighlightReplacedBlocks = workingRule.highlightReplacedBlocks;
        editorUseSolidCollision = workingRule.useSolidCollision;
        selectedReplacementIndex = workingRule.replacements.isEmpty() ? -1 : Math.min(selectedReplacementIndex, workingRule.replacements.size() - 1);
        if (selectedReplacementIndex < 0 && !workingRule.replacements.isEmpty()) {
            selectedReplacementIndex = 0;
        }

        layoutAllWidgets();
    }

    @Override
    protected BlockReplacementRule buildItemFromEditor(boolean creatingNew, BlockReplacementRule selectedItem) {
        syncFieldsToWorkingRule();
        return copyItem(workingRule);
    }

    @Override
    protected void applyItemValues(BlockReplacementRule target, BlockReplacementRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = normalizeCategory(source.category);
        target.enabled = source.enabled;
        target.highlightReplacedBlocks = source.highlightReplacedBlocks;
        target.useSolidCollision = source.useSolidCollision;
        target.corner1X = source.corner1X;
        target.corner1Y = source.corner1Y;
        target.corner1Z = source.corner1Z;
        target.corner2X = source.corner2X;
        target.corner2Y = source.corner2Y;
        target.corner2Z = source.corner2Z;
        target.replacements = deepCopyReplacements(source.replacements);
        target.dirty = true;
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(3201);
        categoryField = createField(3202);
        corner1Field = createField(3203);
        corner2Field = createField(3204);
    }

    @Override
    protected void initAdditionalButtons() {
        btnToggleEnabled = new ThemedButton(BTN_TOGGLE_ENABLED, 0, 0, 100, 20, "");
        btnToggleHighlight = new ThemedButton(BTN_TOGGLE_HIGHLIGHT, 0, 0, 100, 20, "");
        btnToggleSolid = new ThemedButton(BTN_TOGGLE_SOLID, 0, 0, 100, 20, "");
        btnSelectRegion = new ThemedButton(BTN_SELECT_REGION, 0, 0, 100, 20, "选区");
        btnScanBlocks = new ThemedButton(BTN_SCAN_BLOCKS, 0, 0, 100, 20,
                I18n.format("gui.blockreplace.edit.scan_blocks"));
        btnAddReplacement = new ThemedButton(BTN_ADD_REPLACEMENT, 0, 0, 100, 20,
                I18n.format("gui.blockreplace.edit.add_replacement"));
        btnEditReplacement = new ThemedButton(BTN_EDIT_REPLACEMENT, 0, 0, 100, 20,
                I18n.format("gui.blockreplace.manager.edit"));
        btnDeleteReplacement = new ThemedButton(BTN_DELETE_REPLACEMENT, 0, 0, 100, 20,
                I18n.format("gui.blockreplace.manager.delete"));

        this.buttonList.add(btnToggleEnabled);
        this.buttonList.add(btnToggleHighlight);
        this.buttonList.add(btnToggleSolid);
        this.buttonList.add(btnSelectRegion);
        this.buttonList.add(btnScanBlocks);
        this.buttonList.add(btnAddReplacement);
        this.buttonList.add(btnEditReplacement);
        this.buttonList.add(btnDeleteReplacement);
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(120, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);
        placeButton(btnToggleEnabled, 2, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleHighlight, 3, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleSolid, 4, editorFieldX, fullFieldWidth, 20);
        placeField(corner1Field, 5, editorFieldX, fullFieldWidth);
        placeField(corner2Field, 6, editorFieldX, fullFieldWidth);
        placeButton(btnSelectRegion, 7, editorFieldX, fullFieldWidth, 20);
        placeButton(btnScanBlocks, 8, editorFieldX, halfWidth, 20);
        placeButton(btnAddReplacement, 8, editorFieldX + halfWidth + 10, halfWidth, 20);
        placeButton(btnEditReplacement, 9, editorFieldX, halfWidth, 20);
        placeButton(btnDeleteReplacement, 9, editorFieldX + halfWidth + 10, halfWidth, 20);
    }

    @Override
    protected void layoutAdditionalButtons() {
        updateEditorButtonStates();
    }

    @Override
    protected int getEditorTotalRows() {
        return getAvailableBlocksFirstRow() + 4;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        if (row == 0) {
            return I18n.format("gui.blockreplace.edit.name");
        }
        if (row == 1) {
            return "所属分组";
        }
        if (row == 2) {
            return "规则状态";
        }
        if (row == 3) {
            return "高亮被替换方块";
        }
        if (row == 4) {
            return "使用实体方块(阻挡玩家)";
        }
        if (row == 5) {
            return I18n.format("gui.blockreplace.edit.corner1");
        }
        if (row == 6) {
            return I18n.format("gui.blockreplace.edit.corner2");
        }
        if (row == 7) {
            return "区域选区";
        }
        if (row == 8) {
            return "扫描 / 新增";
        }
        if (row == 9) {
            return "编辑 / 删除";
        }
        if (row == 10) {
            return "替换条目";
        }
        if (row == getRegionInfoHeaderRow()) {
            return "区域信息";
        }
        if (row == getAvailableBlocksHeaderRow()) {
            return I18n.format("gui.blockreplace.edit.available_blocks");
        }
        return "";
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(corner1Field);
        fields.add(corner2Field);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorFields();
        drawReplacementRows(mouseX, mouseY);
        drawRegionInfo();
        drawAvailableBlocks();
    }

    @Override
    protected void drawCard(BlockReplacementRule item, int actualIndex, int x, int y, int width, int height,
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

        String region = item.hasValidRegion()
                ? "[" + item.getMinX() + "," + item.getMinY() + "," + item.getMinZ() + "]~["
                + item.getMaxX() + "," + item.getMaxY() + "," + item.getMaxZ() + "]"
                : I18n.format("gui.blockreplace.manager.no_region");
        drawString(fontRenderer, trimToWidth("区域: " + region, width - 12),
                x + 6, y + 31, 0xFFBDBDBD);

        int replacementCount = item.replacements == null ? 0 : item.replacements.size();
        drawString(fontRenderer,
                trimToWidth("替换数: " + replacementCount + "  高亮: " + onOffText(item.highlightReplacedBlocks)
                                + "  碰撞: " + onOffText(item.useSolidCollision),
                        width - 12),
                x + 6, y + 44, 0xFFB8C7D9);
    }

    @Override
    protected String validateItem(BlockReplacementRule item) {
        if (item == null) {
            return "规则为空";
        }
        if (isBlank(item.name)) {
            return "规则名称不能为空";
        }
        return null;
    }

    @Override
    protected void updateEditorButtonStates() {
        if (btnToggleEnabled != null) {
            btnToggleEnabled.displayString = I18n.format("gui.blockreplace.edit.enabled", onOff(editorEnabled));
            btnToggleEnabled.enabled = btnToggleEnabled.visible;
        }
        if (btnToggleHighlight != null) {
            btnToggleHighlight.displayString = I18n.format("gui.blockreplace.edit.highlight",
                    onOff(editorHighlightReplacedBlocks));
            btnToggleHighlight.enabled = btnToggleHighlight.visible;
        }
        if (btnToggleSolid != null) {
            btnToggleSolid.displayString = I18n.format("gui.blockreplace.edit.solid_collision",
                    onOff(editorUseSolidCollision));
            btnToggleSolid.enabled = btnToggleSolid.visible;
        }
        if (btnSelectRegion != null) {
            btnSelectRegion.enabled = btnSelectRegion.visible;
        }
        if (btnScanBlocks != null) {
            btnScanBlocks.enabled = btnScanBlocks.visible && workingRule != null && workingRule.hasValidRegion();
        }
        if (btnAddReplacement != null) {
            btnAddReplacement.enabled = btnAddReplacement.visible;
        }
        boolean hasSelection = workingRule != null
                && selectedReplacementIndex >= 0
                && selectedReplacementIndex < workingRule.replacements.size();
        if (btnEditReplacement != null) {
            btnEditReplacement.enabled = btnEditReplacement.visible && hasSelection;
        }
        if (btnDeleteReplacement != null) {
            btnDeleteReplacement.enabled = btnDeleteReplacement.visible && hasSelection;
        }
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (workingRule == null) {
            return false;
        }
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_HIGHLIGHT) {
            editorHighlightReplacedBlocks = !editorHighlightReplacedBlocks;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_SOLID) {
            editorUseSolidCollision = !editorUseSolidCollision;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_SELECT_REGION) {
            syncFieldsToWorkingRule();
            BlockReplacementHandler.startRegionSelection(workingRule, this);
            return true;
        }
        if (button.id == BTN_SCAN_BLOCKS) {
            syncFieldsToWorkingRule();
            BlockReplacementHandler.markRuleDirty(workingRule);
            setStatus("§a已刷新区域缓存统计", 0xFF8CFF9E);
            return true;
        }
        if (button.id == BTN_ADD_REPLACEMENT) {
            if (workingRule.replacements == null) {
                workingRule.replacements = new ArrayList<>();
            }
            workingRule.replacements.add(new BlockReplacementRule.BlockReplacementEntry());
            selectedReplacementIndex = workingRule.replacements.size() - 1;
            BlockReplacementHandler.markRuleDirty(workingRule);
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_EDIT_REPLACEMENT) {
            if (selectedReplacementIndex >= 0 && selectedReplacementIndex < workingRule.replacements.size()) {
                final int editIndex = selectedReplacementIndex;
                mc.setScreen(new GuiEditBlockReplacementEntry(this, workingRule.replacements.get(editIndex), edited -> {
                    workingRule.replacements.set(editIndex, copyReplacementEntry(edited));
                    BlockReplacementHandler.markRuleDirty(workingRule);
                    mc.setScreen(this);
                }));
            }
            return true;
        }
        if (button.id == BTN_DELETE_REPLACEMENT) {
            if (selectedReplacementIndex >= 0 && selectedReplacementIndex < workingRule.replacements.size()) {
                workingRule.replacements.remove(selectedReplacementIndex);
                if (workingRule.replacements.isEmpty()) {
                    selectedReplacementIndex = -1;
                } else if (selectedReplacementIndex >= workingRule.replacements.size()) {
                    selectedReplacementIndex = workingRule.replacements.size() - 1;
                }
                BlockReplacementHandler.markRuleDirty(workingRule);
                updateEditorButtonStates();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onAfterEditorMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 || workingRule == null) {
            return;
        }
        int startRow = getReplacementStartRow();
        int displayCount = getReplacementDisplayCount();
        int boxX = editorFieldX;
        int boxW = Math.max(80, editorX + editorWidth - 14 - editorFieldX);

        for (int i = 0; i < displayCount; i++) {
            int row = startRow + i;
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }
            if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= y - 1 && mouseY <= y + 17) {
                if (workingRule.replacements.isEmpty()) {
                    selectedReplacementIndex = -1;
                } else if (i < workingRule.replacements.size()) {
                    selectedReplacementIndex = i;
                }
                updateEditorButtonStates();
                return;
            }
        }
    }

    @Override
    protected void onUpdateEditor() {
        if (workingRule == null) {
            return;
        }
        if (corner1Field != null && !corner1Field.isFocused()) {
            setText(corner1Field, formatCorner1(workingRule));
        }
        if (corner2Field != null && !corner2Field.isFocused()) {
            setText(corner2Field, formatCorner2(workingRule));
        }
    }

    @Override
    protected void afterItemSaved(BlockReplacementRule item, boolean createdNow) {
        BlockReplacementHandler.markRuleDirty(item);
        persistChanges();
    }

    @Override
    protected void afterItemDuplicated(BlockReplacementRule item) {
        BlockReplacementHandler.markRuleDirty(item);
        persistChanges();
    }

    private void syncFieldsToWorkingRule() {
        if (workingRule == null) {
            workingRule = createNewItem();
        }
        workingRule.name = safe(nameField.getText()).trim();
        workingRule.category = normalizeCategory(categoryField.getText());
        workingRule.enabled = editorEnabled;
        workingRule.highlightReplacedBlocks = editorHighlightReplacedBlocks;
        workingRule.useSolidCollision = editorUseSolidCollision;
        parseCorner(corner1Field.getText(), true);
        parseCorner(corner2Field.getText(), false);
        if (workingRule.replacements == null) {
            workingRule.replacements = new ArrayList<>();
        }
        BlockReplacementHandler.markRuleDirty(workingRule);
    }

    private void parseCorner(String text, boolean first) {
        if (isBlank(text)) {
            return;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return;
        }
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            if (first) {
                workingRule.setCorner1(x, y, z);
            } else {
                workingRule.setCorner2(x, y, z);
            }
        } catch (Exception ignored) {
        }
    }

    private void drawReplacementRows(int mouseX, int mouseY) {
        int boxX = editorFieldX;
        int boxW = Math.max(80, editorX + editorWidth - 14 - editorFieldX);
        int startRow = getReplacementStartRow();
        int displayCount = getReplacementDisplayCount();

        for (int i = 0; i < displayCount; i++) {
            int row = startRow + i;
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }

            if (workingRule == null || workingRule.replacements.isEmpty()) {
                int bg = 0x55222222;
                int border = 0xFF4B4B4B;
                drawRect(boxX, y - 1, boxX + boxW, y + 17, bg);
                drawHorizontalLine(boxX, boxX + boxW, y - 1, border);
                drawHorizontalLine(boxX, boxX + boxW, y + 17, border);
                drawString(fontRenderer, I18n.format("gui.blockreplace.manager.empty"), boxX + 6, y + 4, 0xFFB8C7D9);
                return;
            }

            BlockReplacementRule.BlockReplacementEntry entry = workingRule.replacements.get(i);
            boolean selected = i == selectedReplacementIndex;
            boolean hovered = mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= y - 1 && mouseY <= y + 17;

            int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x88222222);
            int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

            drawRect(boxX, y - 1, boxX + boxW, y + 17, bg);
            drawHorizontalLine(boxX, boxX + boxW, y - 1, border);
            drawHorizontalLine(boxX, boxX + boxW, y + 17, border);

            String line = I18n.format("gui.blockreplace.edit.entry_line",
                    entry.enabled ? "§a✔" : "§c✘",
                    i + 1,
                    isBlank(entry.sourceBlockId) ? "?" : entry.sourceBlockId,
                    isBlank(entry.targetBlockId) ? "?" : entry.targetBlockId);
            drawString(fontRenderer, trimToWidth(line, boxW - 10), boxX + 4, y + 4, 0xFFFFFFFF);
        }
    }

    private void drawRegionInfo() {
        if (workingRule == null) {
            return;
        }
        int infoRow = getRegionInfoValueRow();
        int y = getEditorRowY(infoRow);
        if (y < 0) {
            return;
        }

        String text;
        if (workingRule.hasValidRegion()) {
            text = I18n.format("gui.blockreplace.edit.region_info", workingRule.getRegionBlockCount());
        } else {
            text = I18n.format("gui.blockreplace.manager.no_region");
        }
        drawString(fontRenderer, trimToWidth(text, editorWidth - getEditorLabelWidth() - 24), editorFieldX, y + 4, 0xFFB8C7D9);
    }

    private void drawAvailableBlocks() {
        if (workingRule == null) {
            return;
        }
        List<BlockReplacementHandler.BlockCountEntry> availableBlocks = BlockReplacementHandler.getAvailableBlocks(workingRule);
        int firstRow = getAvailableBlocksFirstRow();

        if (availableBlocks.isEmpty()) {
            int y = getEditorRowY(firstRow);
            if (y >= 0) {
                drawString(fontRenderer,
                        I18n.format("gui.blockreplace.edit.available_blocks_empty"),
                        editorFieldX, y + 4, 0xFFB8C7D9);
            }
            return;
        }

        int count = Math.min(4, availableBlocks.size());
        for (int i = 0; i < count; i++) {
            int y = getEditorRowY(firstRow + i);
            if (y < 0) {
                continue;
            }
            BlockReplacementHandler.BlockCountEntry entry = availableBlocks.get(i);
            String line = I18n.format("gui.blockreplace.edit.available_blocks_line", entry.blockId, entry.count);
            drawString(fontRenderer, trimToWidth(line, editorWidth - getEditorLabelWidth() - 24),
                    editorFieldX, y + 4, 0xFFFFFFFF);
        }
    }

    private int getReplacementDisplayCount() {
        if (workingRule == null || workingRule.replacements == null || workingRule.replacements.isEmpty()) {
            return 1;
        }
        return workingRule.replacements.size();
    }

    private int getReplacementStartRow() {
        return 11;
    }

    private int getRegionInfoHeaderRow() {
        return getReplacementStartRow() + getReplacementDisplayCount();
    }

    private int getRegionInfoValueRow() {
        return getRegionInfoHeaderRow() + 1;
    }

    private int getAvailableBlocksHeaderRow() {
        return getRegionInfoValueRow() + 1;
    }

    private int getAvailableBlocksFirstRow() {
        return getAvailableBlocksHeaderRow() + 1;
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private List<BlockReplacementRule.BlockReplacementEntry> deepCopyReplacements(
            List<BlockReplacementRule.BlockReplacementEntry> entries) {
        List<BlockReplacementRule.BlockReplacementEntry> copies = new ArrayList<>();
        if (entries == null) {
            return copies;
        }
        for (BlockReplacementRule.BlockReplacementEntry entry : entries) {
            copies.add(copyReplacementEntry(entry));
        }
        return copies;
    }

    private BlockReplacementRule.BlockReplacementEntry copyReplacementEntry(
            BlockReplacementRule.BlockReplacementEntry source) {
        BlockReplacementRule.BlockReplacementEntry copy = new BlockReplacementRule.BlockReplacementEntry();
        if (source == null) {
            return copy;
        }
        copy.sourceBlockId = source.sourceBlockId;
        copy.targetBlockId = source.targetBlockId;
        copy.enabled = source.enabled;
        return copy;
    }

    private String formatCorner1(BlockReplacementRule rule) {
        return rule != null && rule.hasCorner1()
                ? rule.corner1X + ", " + rule.corner1Y + ", " + rule.corner1Z
                : "";
    }

    private String formatCorner2(BlockReplacementRule rule) {
        return rule != null && rule.hasCorner2()
                ? rule.corner2X + ", " + rule.corner2Y + ", " + rule.corner2Z
                : "";
    }

    private String onOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }

    private String onOffText(boolean enabled) {
        return enabled ? "开" : "关";
    }
}




