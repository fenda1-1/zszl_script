package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractThreePaneRuleManager<T> extends ThemedGuiScreen {

    protected static final String CATEGORY_ALL = "__all__";
    protected static final String CATEGORY_DEFAULT = "默认";

    protected static final int BTN_NEW = 100;
    protected static final int BTN_DELETE = 101;
    protected static final int BTN_SAVE = 102;
    protected static final int BTN_DONE = 103;

    protected static final int TREE_ROW_HEIGHT = 18;
    protected static final int EDITOR_ROW_HEIGHT = 24;

    protected final GuiScreen parentScreen;

    protected final List<T> allItems = new ArrayList<>();
    protected final List<T> visibleItems = new ArrayList<>();
    protected final List<String> categories = new ArrayList<>();
    protected final List<TreeRow> visibleTreeRows = new ArrayList<>();
    protected final Set<String> expandedCategories = new LinkedHashSet<>();

    protected String selectedCategory = CATEGORY_ALL;
    protected int selectedVisibleIndex = -1;
    protected int selectedTreeIndex = 0;
    protected int treeScrollOffset = 0;
    protected int listScrollOffset = 0;
    protected int editorScrollOffset = 0;
    protected boolean creatingNew = false;
    protected boolean treeCollapsed = false;
    protected boolean listCollapsed = false;

    protected int panelX;
    protected int panelY;
    protected int panelWidth;
    protected int panelHeight;

    protected int treeX;
    protected int treeY;
    protected int treeWidth;
    protected int treeHeight;

    protected int listX;
    protected int listY;
    protected int listWidth;
    protected int listHeight;

    protected int editorX;
    protected int editorY;
    protected int editorWidth;
    protected int editorHeight;
    protected int editorLabelX;
    protected int editorFieldX;
    protected int editorRowStartY;
    protected int editorVisibleRows;

    protected GuiButton btnNew;
    protected GuiButton btnDelete;
    protected GuiButton btnSave;
    protected GuiButton btnDone;

    protected String statusMessage = "";
    protected int statusColor = 0xFFB8C7D9;

    private final List<ContextMenuItem> contextMenuItems = new ArrayList<>();
    private boolean contextMenuVisible = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private int contextMenuWidth = 180;
    private int contextMenuItemHeight = 18;
    private int contextMenuTargetIndex = -1;
    private String contextMenuTargetType = "card";
    private String contextMenuTargetCategory = "";

    protected AbstractThreePaneRuleManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.statusMessage = safe(getGuideText());
        applyReadableUiScaleForLargeScreen(1180, 660);

        recalcLayout();
        initCoreButtons();
        initEditorWidgets();
        initAdditionalButtons();
        refreshData(false);
        layoutAllWidgets();
    }

    protected void recalcLayout() {
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
                ? getCollapsedSectionWidth(getTreeCollapsedTitle())
                : Math.max(122, Math.min(170, availableWidth / 6));

        listX = treeX + treeWidth + columnGap;
        listY = treeY;
        listHeight = treeHeight;
        listWidth = listCollapsed
                ? getCollapsedSectionWidth(getListCollapsedTitle())
                : Math.max(190, Math.min(250, availableWidth / 4));

        int rightX = listX + listWidth + columnGap;
        editorX = rightX;
        editorY = treeY;
        editorWidth = panelX + panelWidth - sidePadding - rightX;
        editorHeight = treeHeight;

        editorLabelX = editorX + 10;
        editorFieldX = editorX + getEditorLabelWidth();
        editorRowStartY = editorY + 28;
        editorVisibleRows = Math.max(1, (editorHeight - 38) / EDITOR_ROW_HEIGHT);
    }

    private void initCoreButtons() {
        int bottomY = panelY + panelHeight - 28;

        btnNew = new ThemedButton(BTN_NEW, treeX, bottomY, 72, 20, "新增");
        btnDelete = new ThemedButton(BTN_DELETE, treeX + 80, bottomY, 72, 20, "删除");

        btnSave = new ThemedButton(BTN_SAVE, panelX + panelWidth - 186, bottomY, 84, 20, "§a保存");
        btnDone = new ThemedButton(BTN_DONE, panelX + panelWidth - 94, bottomY, 84, 20, "完成");

        this.buttonList.add(btnNew);
        this.buttonList.add(btnDelete);
        this.buttonList.add(btnSave);
        this.buttonList.add(btnDone);
    }

    protected void layoutAllWidgets() {
        layoutCoreButtons();
        clampEditorScroll();
        layoutEditorWidgets();
        layoutAdditionalButtons();
        updateBaseButtonStates();
        updateEditorButtonStates();
    }

    protected void layoutCoreButtons() {
        int bottomY = panelY + panelHeight - 28;
        if (btnNew != null) {
            btnNew.x = treeX;
            btnNew.y = bottomY;
        }
        if (btnDelete != null) {
            btnDelete.x = treeX + 80;
            btnDelete.y = bottomY;
        }
        if (btnSave != null) {
            btnSave.x = panelX + panelWidth - 186;
            btnSave.y = bottomY;
        }
        if (btnDone != null) {
            btnDone.x = panelX + panelWidth - 94;
            btnDone.y = bottomY;
        }
    }

    protected void updateBaseButtonStates() {
        boolean hasSelected = !creatingNew && selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleItems.size();
        if (btnDelete != null) {
            btnDelete.enabled = hasSelected;
        }
        if (btnSave != null) {
            btnSave.enabled = true;
        }
    }

    protected void refreshData(boolean keepSelection) {
        String keepItemName = keepSelection ? getSelectedItemName() : "";

        allItems.clear();
        List<T> sourceItems = getSourceItems();
        if (sourceItems != null) {
            allItems.addAll(sourceItems);
        }

        rebuildCategories();
        rebuildTreeRows();
        rebuildVisibleItems();

        if (!isBlank(keepItemName) && selectByItemName(keepItemName)) {
            return;
        }

        if (visibleItems.isEmpty()) {
            selectedVisibleIndex = -1;
            creatingNew = true;
            clearEditorForNew();
            return;
        }

        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleItems.size()) {
            selectedVisibleIndex = 0;
        }
        creatingNew = false;
        loadEditor(visibleItems.get(selectedVisibleIndex));
        onSelectionChanged(visibleItems.get(selectedVisibleIndex));
    }

    protected void rebuildCategories() {
        categories.clear();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        List<String> sourceCategories = getSourceCategories();
        if (sourceCategories != null) {
            for (String category : sourceCategories) {
                seen.add(normalizeCategory(category));
            }
        }

        for (T item : allItems) {
            seen.add(normalizeCategory(getItemCategory(item)));
        }

        if (seen.isEmpty()) {
            seen.add(CATEGORY_DEFAULT);
        }
        categories.addAll(seen);
    }

    protected void rebuildTreeRows() {
        visibleTreeRows.clear();
        visibleTreeRows.add(TreeRow.allRow(getAllItemsLabel()));
        for (String category : categories) {
            visibleTreeRows.add(TreeRow.categoryRow(category));
            if (expandedCategories.contains(category)) {
                for (T item : allItems) {
                    if (category.equals(normalizeCategory(getItemCategory(item)))) {
                        String name = safe(getItemName(item));
                        visibleTreeRows.add(TreeRow.itemRow(category, name, name));
                    }
                }
            }
        }
        selectedTreeIndex = Math.max(0, Math.min(selectedTreeIndex, Math.max(0, visibleTreeRows.size() - 1)));
    }

    protected void rebuildVisibleItems() {
        visibleItems.clear();
        for (T item : allItems) {
            if (CATEGORY_ALL.equals(selectedCategory)
                    || normalizeCategory(getItemCategory(item)).equals(selectedCategory)) {
                visibleItems.add(item);
            }
        }

        int max = Math.max(0, visibleItems.size() - getVisibleCardCount());
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));
        if (selectedVisibleIndex >= visibleItems.size()) {
            selectedVisibleIndex = visibleItems.isEmpty() ? -1 : 0;
        }
    }

    protected void clearEditorForNew() {
        T draft = createNewItem();
        if (draft != null && isConcreteCategory(selectedCategory)) {
            setItemCategory(draft, selectedCategory);
        }
        loadEditor(draft);
        creatingNew = true;
        editorScrollOffset = 0;
        layoutAllWidgets();
        onSelectionChanged(null);
    }

    protected void saveCurrentItem() {
        try {
            T current = getSelectedItemOrNull();
            T built = buildItemFromEditor(creatingNew, current);
            String validationMessage = validateItem(built);
            if (!isBlank(validationMessage)) {
                setStatus("§c" + validationMessage, 0xFFFF8E8E);
                return;
            }

            if (creatingNew) {
                addItemToSource(built);
                creatingNew = false;
                afterItemSaved(built, true);
                refreshData(true);
                selectByItemName(getItemName(built));
                setStatus("§a已新增" + getEntityDisplayName() + ": " + safe(getItemName(built)), 0xFF8CFF9E);
                return;
            }

            if (current == null) {
                setStatus("§c请先选择一条" + getEntityDisplayName(), 0xFFFF8E8E);
                return;
            }

            applyItemValues(current, built);
            afterItemSaved(current, false);
            refreshData(true);
            selectByItemName(getItemName(current));
            setStatus("§a" + getEntityDisplayName() + "已保存", 0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c保存失败: " + safe(e.getMessage()), 0xFFFF8E8E);
        }
    }

    protected void deleteSelectedItem() {
        T target = getSelectedItemOrNull();
        if (target == null) {
            setStatus("§c请先选择一条" + getEntityDisplayName(), 0xFFFF8E8E);
            return;
        }
        String name = safe(getItemName(target));
        removeItemFromSource(target);
        afterItemDeleted(target);
        refreshData(false);
        setStatus("§a已删除" + getEntityDisplayName() + ": " + name, 0xFF8CFF9E);
    }

    protected void duplicateSelectedItem() {
        T source = getSelectedItemOrNull();
        if (source == null) {
            setStatus("§c请先右键选择一条" + getEntityDisplayName() + "卡片", 0xFFFF8E8E);
            return;
        }

        T copy = copyItem(source);
        if (copy == null) {
            setStatus("§c复制失败", 0xFFFF8E8E);
            return;
        }

        String baseName = safe(getItemName(source)).trim();
        setItemName(copy, baseName.isEmpty() ? getEntityDisplayName() + "副本" : baseName + " 副本");
        if (!isConcreteCategory(getItemCategory(copy))) {
            setItemCategory(copy, isConcreteCategory(selectedCategory) ? selectedCategory : CATEGORY_DEFAULT);
        }

        addItemToSource(copy);
        afterItemDuplicated(copy);
        refreshData(true);
        selectByItemName(getItemName(copy));
        setStatus("§a已复制" + getEntityDisplayName() + ": " + safe(getItemName(copy)), 0xFF8CFF9E);
    }

    protected void copySelectedItemName() {
        T selected = getSelectedItemOrNull();
        if (selected == null) {
            setStatus("§c请先右键选择一条" + getEntityDisplayName() + "卡片", 0xFFFF8E8E);
            return;
        }
        String name = safe(getItemName(selected));
        setClipboardString(name);
        setStatus("§a已复制名称: " + name, 0xFF8CFF9E);
    }

    protected void moveSelectedItemToCurrentCategory() {
        if (!isConcreteCategory(selectedCategory)) {
            setStatus("§c请先在左侧选择一个具体分组", 0xFFFF8E8E);
            return;
        }
        T selected = getSelectedItemOrNull();
        if (selected == null) {
            setStatus("§c请先右键选择一条" + getEntityDisplayName() + "卡片", 0xFFFF8E8E);
            return;
        }

        setItemCategory(selected, selectedCategory);
        persistChanges();
        refreshData(true);
        selectByItemName(getItemName(selected));
        setStatus("§a已移动到分组: " + selectedCategory, 0xFF8CFF9E);
    }

    protected void reloadAllItems() {
        reloadSource();
        refreshData(false);
        setStatus("§a已重载" + getScreenTitle(), 0xFF8CFF9E);
    }

    protected boolean selectByItemName(String name) {
        if (isBlank(name)) {
            return false;
        }

        for (T item : allItems) {
            if (name.equalsIgnoreCase(safe(getItemName(item)))) {
                selectedCategory = normalizeCategory(getItemCategory(item));
                rebuildTreeRows();
                rebuildVisibleItems();

                for (int i = 0; i < visibleItems.size(); i++) {
                    if (name.equalsIgnoreCase(safe(getItemName(visibleItems.get(i))))) {
                        selectedVisibleIndex = i;
                        creatingNew = false;
                        loadEditor(visibleItems.get(i));
                        ensureCardSelectionVisible();
                        onSelectionChanged(visibleItems.get(i));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected String getSelectedItemName() {
        T selected = getSelectedItemOrNull();
        return selected == null ? "" : safe(getItemName(selected));
    }

    protected T getSelectedItemOrNull() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleItems.size()) {
            return null;
        }
        return visibleItems.get(selectedVisibleIndex);
    }

    protected void refreshSelectionAfterFilter() {
        if (visibleItems.isEmpty()) {
            creatingNew = true;
            clearEditorForNew();
            return;
        }
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleItems.size()) {
            selectedVisibleIndex = 0;
        }
        creatingNew = false;
        loadEditor(visibleItems.get(selectedVisibleIndex));
        onSelectionChanged(visibleItems.get(selectedVisibleIndex));
    }

    protected void placeField(GuiTextField field, int row, int x, int width) {
        if (field == null) {
            return;
        }
        int y = getEditorRowY(row);
        field.x = x;
        field.width = width;
        field.height = 16;
        field.y = y >= 0 ? y : -2000;
    }

    protected void placeButton(GuiButton button, int row, int x, int width, int height) {
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

    protected int getEditorRowY(int row) {
        int visibleIndex = row - editorScrollOffset;
        if (visibleIndex < 0 || visibleIndex >= editorVisibleRows) {
            return -1;
        }
        return editorRowStartY + visibleIndex * EDITOR_ROW_HEIGHT;
    }

    protected int getMaxEditorScroll() {
        return Math.max(0, getEditorTotalRows() - editorVisibleRows);
    }

    protected void clampEditorScroll() {
        editorScrollOffset = Math.max(0, Math.min(editorScrollOffset, getMaxEditorScroll()));
    }

    protected int getVisibleTreeRowCount() {
        return Math.max(1, (treeHeight - 26) / TREE_ROW_HEIGHT);
    }

    protected int getVisibleCardCount() {
        return Math.max(1, (listHeight - 28) / getCardStep());
    }

    protected int getCardStep() {
        return getCardHeight() + getCardGap();
    }

    protected int getTreeRowIndexAt(int mouseY) {
        int contentY = treeY + 22;
        int localY = mouseY - contentY;
        if (localY < 0) {
            return -1;
        }
        int localIndex = localY / TREE_ROW_HEIGHT;
        int actualIndex = treeScrollOffset + localIndex;
        return actualIndex >= 0 && actualIndex < visibleTreeRows.size() ? actualIndex : -1;
    }

    protected int getCardIndexAt(int mouseY) {
        int contentY = listY + 22;
        int local = mouseY - contentY;
        if (local < 0) {
            return -1;
        }
        int step = getCardStep();
        int localIndex = local / step;
        int yInCard = local % step;
        if (yInCard >= getCardHeight()) {
            return -1;
        }
        return listScrollOffset + localIndex;
    }

    protected void ensureCardSelectionVisible() {
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

    protected boolean isInTree(int mouseX, int mouseY) {
        return mouseX >= treeX && mouseX <= treeX + treeWidth && mouseY >= treeY && mouseY <= treeY + treeHeight;
    }

    protected boolean isInCardList(int mouseX, int mouseY) {
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    protected boolean isInEditor(int mouseX, int mouseY) {
        return mouseX >= editorX && mouseX <= editorX + editorWidth
                && mouseY >= editorY && mouseY <= editorY + editorHeight;
    }

    protected boolean isInTreeCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, treeX, treeY, treeWidth);
    }

    protected boolean isInListCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, listX, listY, listWidth);
    }

    protected boolean isInCollapseButton(int mouseX, int mouseY, int boxX, int boxY, int boxWidth) {
        int btnSize = 14;
        int btnX = boxX + boxWidth - btnSize - 6;
        int btnY = boxY + 5;
        return mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;
    }

    protected void drawCollapseButton(int boxX, int boxY, int boxWidth, boolean collapsed) {
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

    protected int getCollapsedSectionWidth(String shortTitle) {
        int len = shortTitle == null ? 0 : Math.min(2, shortTitle.length());
        int textWidth = this.fontRenderer == null ? 16
                : this.fontRenderer.getStringWidth(shortTitle == null ? "" : shortTitle.substring(0, len));
        return Math.max(34, textWidth + 24);
    }

    protected void setStatus(String message, int color) {
        this.statusMessage = safe(message);
        this.statusColor = color;
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }

    protected boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected String normalizeCategory(String category) {
        String normalized = safe(category).trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    protected boolean isConcreteCategory(String category) {
        return !isBlank(category) && !CATEGORY_ALL.equals(category);
    }

    protected void setText(GuiTextField field, String value) {
        if (field != null) {
            field.setText(value == null ? "" : value);
        }
    }

    protected List<GuiTextField> getVisibleFields() {
        List<GuiTextField> fields = new ArrayList<>();
        for (GuiTextField field : getEditorFields()) {
            if (field != null && field.y >= editorRowStartY
                    && field.y < editorRowStartY + editorVisibleRows * EDITOR_ROW_HEIGHT) {
                fields.add(field);
            }
        }
        return fields;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DONE) {
            persistChanges();
            onDone();
            mc.setScreen(parentScreen);
            return;
        }
        if (button.id == BTN_NEW) {
            creatingNew = true;
            selectedVisibleIndex = -1;
            clearEditorForNew();
            setStatus("§a已进入新建模式", 0xFF8CFF9E);
            return;
        }
        if (button.id == BTN_DELETE) {
            deleteSelectedItem();
            return;
        }
        if (button.id == BTN_SAVE) {
            saveCurrentItem();
            return;
        }
        if (handleAdditionalAction(button)) {
            return;
        }
        super.actionPerformed(button);
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
                layoutAllWidgets();
                return;
            }
            if (isInListCollapseButton(mouseX, mouseY)) {
                listCollapsed = !listCollapsed;
                recalcLayout();
                layoutAllWidgets();
                return;
            }
        }

        if (!listCollapsed && mouseButton == 1 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleItems.size()) {
                selectedVisibleIndex = actual;
                creatingNew = false;
                T selected = visibleItems.get(actual);
                loadEditor(selected);
                onSelectionChanged(selected);
                openCardContextMenu(mouseX, mouseY, actual);
                return;
            }
            openCardContextMenu(mouseX, mouseY, -1);
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
            if (actual >= 0 && actual < visibleItems.size()) {
                selectedVisibleIndex = actual;
                creatingNew = false;
                T selected = visibleItems.get(actual);
                loadEditor(selected);
                onSelectionChanged(selected);
            }
            return;
        }

        GuiTextField focusedField = null;
        for (GuiTextField field : getVisibleFields()) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
            if (mouseX >= field.x && mouseX < field.x + field.width
                    && mouseY >= field.y && mouseY < field.y + field.height) {
                focusedField = field;
            }
        }

        for (GuiTextField field : getEditorFields()) {
            if (field != null) {
                field.setFocused(field == focusedField);
            }
        }

        onAfterEditorMouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void handleTreeClick(int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }

        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (row.type == TreeRow.TYPE_ALL) {
            selectedCategory = CATEGORY_ALL;
            rebuildVisibleItems();
            refreshSelectionAfterFilter();
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
            rebuildVisibleItems();
            refreshSelectionAfterFilter();
            return;
        }

        if (row.type == TreeRow.TYPE_ITEM) {
            selectedCategory = row.category;
            rebuildVisibleItems();
            selectByItemName(row.itemName);
        }
    }

    protected void handleTreeRightClick(int mouseX, int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }

        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (row.type == TreeRow.TYPE_ITEM) {
            selectedCategory = row.category;
            rebuildVisibleItems();
            selectByItemName(row.itemName);
            openCardContextMenu(mouseX, mouseY, selectedVisibleIndex);
            return;
        }

        if (row.type == TreeRow.TYPE_CATEGORY) {
            selectedCategory = safe(row.category);
            rebuildVisibleItems();
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
            if (handleEditorMouseWheel(mouseX, mouseY, wheel)) {
                layoutAllWidgets();
                return;
            }
            if (wheel < 0) {
                editorScrollOffset = Math.min(getMaxEditorScroll(), editorScrollOffset + 1);
            } else {
                editorScrollOffset = Math.max(0, editorScrollOffset - 1);
            }
            layoutAllWidgets();
            return;
        }

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            int max = Math.max(0, visibleTreeRows.size() - getVisibleTreeRowCount());
            treeScrollOffset = wheel < 0 ? Math.min(max, treeScrollOffset + 1) : Math.max(0, treeScrollOffset - 1);
            return;
        }

        if (!listCollapsed && isInCardList(mouseX, mouseY)) {
            int max = Math.max(0, visibleItems.size() - getVisibleCardCount());
            listScrollOffset = wheel < 0 ? Math.min(max, listScrollOffset + 1) : Math.max(0, listScrollOffset - 1);
            return;
        }

        onMouseWheelOutsidePanels(mouseX, mouseY, wheel);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (contextMenuVisible) {
                closeContextMenu();
                return;
            }
            onEscape();
            mc.setScreen(parentScreen);
            return;
        }

        boolean consumed = false;
        for (GuiTextField field : getEditorFields()) {
            if (field != null && field.isFocused() && field.textboxKeyTyped(typedChar, keyCode)) {
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
        for (GuiTextField field : getEditorFields()) {
            if (field != null) {
                field.updateCursorCounter();
            }
        }
        onUpdateEditor();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mouseX = toReadableMouseX(mouseX);
        mouseY = toReadableMouseY(mouseY);
        pushReadableUiScale();
        try {
            drawDefaultBackground();

            GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
            GuiTheme.drawTitleBar(panelX, panelY, panelWidth, getScreenTitle(), this.fontRenderer);

            GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 22, panelWidth - 20, 18, false, true);
            drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 27, statusColor);

            drawTreePanel(mouseX, mouseY);
            drawCardPanel(mouseX, mouseY);
            drawEditorPanel(mouseX, mouseY, partialTicks);

            super.drawScreen(mouseX, mouseY, partialTicks);

            if (contextMenuVisible) {
                drawContextMenu(mouseX, mouseY);
            }
        } finally {
            popReadableUiScale();
        }
    }

    protected void drawTreePanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(treeX, treeY, treeWidth, treeHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(treeX + 8, treeY + 8,
                treeCollapsed ? getTreeCollapsedTitle() : getTreeExpandedTitle(),
                this.fontRenderer);
        drawCollapseButton(treeX, treeY, treeWidth, treeCollapsed);
        if (treeCollapsed) {
            return;
        }

        int contentY = treeY + 22;
        int visibleRows = getVisibleTreeRowCount();
        int max = Math.max(0, visibleTreeRows.size() - visibleRows);
        treeScrollOffset = Math.max(0, Math.min(treeScrollOffset, max));

        if (visibleTreeRows.isEmpty()) {
            GuiTheme.drawEmptyState(treeX + treeWidth / 2, treeY + treeHeight / 2, getEmptyTreeText(), this.fontRenderer);
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
            } else if (row.type == TreeRow.TYPE_ITEM) {
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

    protected void drawCardPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(listX, listY, listWidth, listHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(listX + 8, listY + 8,
                listCollapsed ? getListCollapsedTitle() : getListExpandedTitle(),
                this.fontRenderer);
        drawCollapseButton(listX, listY, listWidth, listCollapsed);
        if (listCollapsed) {
            return;
        }

        int contentY = listY + 22;
        int visible = getVisibleCardCount();
        int max = Math.max(0, visibleItems.size() - visible);
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));

        if (visibleItems.isEmpty()) {
            GuiTheme.drawEmptyState(listX + listWidth / 2, listY + listHeight / 2, getEmptyListText(), this.fontRenderer);
            return;
        }

        for (int i = 0; i < visible; i++) {
            int actual = listScrollOffset + i;
            if (actual >= visibleItems.size()) {
                break;
            }

            T item = visibleItems.get(actual);
            int cardTop = contentY + i * getCardStep();
            boolean selected = actual == selectedVisibleIndex;
            boolean hovered = mouseX >= listX + 2 && mouseX <= listX + listWidth - 2
                    && mouseY >= cardTop && mouseY <= cardTop + getCardHeight();

            drawCard(item, actual, listX + 2, cardTop, listWidth - 4, getCardHeight(), selected, hovered);
        }

        if (visibleItems.size() > visible) {
            int thumbHeight = Math.max(18,
                    (int) ((visible / (float) Math.max(visible, visibleItems.size())) * (listHeight - 28)));
            int track = Math.max(1, (listHeight - 28) - thumbHeight);
            int thumbY = contentY + (int) ((listScrollOffset / (float) Math.max(1, max)) * track);
            GuiTheme.drawScrollbar(listX + listWidth - 8, contentY, 4, listHeight - 28, thumbY, thumbHeight);
        }
    }

    protected void drawEditorPanel(int mouseX, int mouseY, float partialTicks) {
        GuiTheme.drawPanelSegment(editorX, editorY, editorWidth, editorHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(editorX + 8, editorY + 8,
                creatingNew ? getEditorPanelTitle() + " - 新建" : getEditorPanelTitle() + " - 编辑",
                this.fontRenderer);

        layoutAllWidgets();

        int visibleStart = editorScrollOffset;
        int visibleEnd = Math.min(getEditorTotalRows(), visibleStart + editorVisibleRows);
        for (int row = visibleStart; row < visibleEnd; row++) {
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }
            String label = getEditorRowLabel(row);
            if (!isBlank(label)) {
                drawString(fontRenderer, label, editorLabelX, y + 4, GuiTheme.SUB_TEXT);
            }
        }

        drawEditorContents(mouseX, mouseY, partialTicks);

        if (getEditorTotalRows() > editorVisibleRows) {
            int thumbHeight = Math.max(18,
                    (int) ((editorVisibleRows / (float) getEditorTotalRows()) * (editorHeight - 12)));
            int track = Math.max(1, (editorHeight - 12) - thumbHeight);
            int thumbY = editorY + 6 + (int) ((editorScrollOffset / (float) Math.max(1, getMaxEditorScroll())) * track);
            GuiTheme.drawScrollbar(editorX + editorWidth - 8, editorY + 6, 4, editorHeight - 12, thumbY, thumbHeight);
        }
    }

    protected void drawEditorFields() {
        for (GuiTextField field : getVisibleFields()) {
            drawThemedTextField(field);
        }
    }

    protected void drawContextMenu(int mouseX, int mouseY) {
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

    protected boolean handleContextMenuClick(int mouseX, int mouseY, int mouseButton) {
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
            duplicateSelectedItem();
        } else if ("copy_name".equals(item.key)) {
            copySelectedItemName();
        } else if ("delete".equals(item.key)) {
            deleteSelectedItem();
        } else if ("move".equals(item.key)) {
            moveSelectedItemToCurrentCategory();
        } else if ("reload".equals(item.key)) {
            reloadAllItems();
        } else {
            return handleCustomContextMenuAction(item.key);
        }
        return true;
    }

    protected void openCardContextMenu(int mouseX, int mouseY, int targetIndex) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetIndex = targetIndex;
        contextMenuTargetType = "card";
        contextMenuTargetCategory = "";
        contextMenuItems.clear();

        T item = null;
        if (targetIndex >= 0 && targetIndex < visibleItems.size()) {
            item = visibleItems.get(targetIndex);
        }

        contextMenuItems.add(new ContextMenuItem("new", "新增" + getEntityDisplayName(), true));
        contextMenuItems.add(new ContextMenuItem("reload", "重载" + getScreenTitle(), true));

        if (item != null) {
            contextMenuItems.add(new ContextMenuItem("duplicate", "复制当前" + getEntityDisplayName(), true));
            contextMenuItems.add(new ContextMenuItem("copy_name", "复制名称", !isBlank(getItemName(item))));
            contextMenuItems.add(new ContextMenuItem("delete", "删除" + getEntityDisplayName(), true));
            boolean canMove = isConcreteCategory(selectedCategory)
                    && !selectedCategory.equalsIgnoreCase(normalizeCategory(getItemCategory(item)));
            contextMenuItems.add(new ContextMenuItem("move", "移动到当前分组", canMove));
        }

        appendCustomCardContextMenuItems(contextMenuItems, item);
    }

    protected void openTreeContextMenu(int mouseX, int mouseY, TreeRow row) {
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
            contextMenuItems.add(new ContextMenuItem("tree_reload", "重载" + getScreenTitle(), true));
        }

        appendCustomTreeContextMenuItems(contextMenuItems, row);
    }

    protected void closeContextMenu() {
        contextMenuVisible = false;
        contextMenuItems.clear();
        contextMenuTargetIndex = -1;
        contextMenuTargetType = "card";
        contextMenuTargetCategory = "";
    }

    protected void handleTreeContextMenuAction(String key, String targetCategory) {
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
            reloadAllItems();
            return;
        }
        handleCustomTreeContextMenuAction(key, targetCategory);
    }

    protected void openAddCategoryDialog() {
        mc.setScreen(new GuiTextInput(this, "输入新分组名称", value -> {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) {
                setStatus("§7已取消创建分组", 0xFFB8C7D9);
                return;
            }
            boolean ok = addCategoryToSource(normalized);
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

    protected void pasteCategoryFromClipboard() {
        String text = getClipboardString();
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            setStatus("§c剪贴板为空，无法新建分组", 0xFFFF8E8E);
            return;
        }
        boolean ok = addCategoryToSource(normalized);
        if (ok) {
            selectedCategory = normalized;
            expandedCategories.add(normalized);
            refreshData(true);
            setStatus("§a已从剪贴板新建分组: " + normalized, 0xFF8CFF9E);
        } else {
            setStatus("§c新建分组失败，名称可能重复或无效", 0xFFFF8E8E);
        }
    }

    protected void copyCategoryName(String category) {
        String normalized = safe(category).trim();
        if (normalized.isEmpty()) {
            setStatus("§c没有可复制的分组名", 0xFFFF8E8E);
            return;
        }
        setClipboardString(normalized);
        setStatus("§a已复制分组名: " + normalized, 0xFF8CFF9E);
    }

    protected void openRenameCategoryDialog(String category) {
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
            boolean ok = renameCategoryInSource(normalized, newName);
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

    protected void deleteCategoryByName(String category) {
        String normalized = safe(category).trim();
        if (!isConcreteCategory(normalized)) {
            setStatus("§c该分组不能删除", 0xFFFF8E8E);
            return;
        }
        if (deleteCategoryInSource(normalized)) {
            expandedCategories.remove(normalized);
            selectedCategory = CATEGORY_ALL;
            refreshData(false);
            setStatus("§a已删除分组: " + normalized + "（原分组条目已转为默认分组）", 0xFF8CFF9E);
        } else {
            setStatus("§c删除分组失败", 0xFFFF8E8E);
        }
    }

    protected String trimToWidth(String text, int maxWidth) {
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

    protected int getCardHeight() {
        return 56;
    }

    protected int getCardGap() {
        return 6;
    }

    protected int getEditorLabelWidth() {
        return 92;
    }

    protected String getTreeCollapsedTitle() {
        return "规则";
    }

    protected String getListCollapsedTitle() {
        return "卡片";
    }

    protected String getTreeExpandedTitle() {
        return "规则树（右键打开菜单）";
    }

    protected String getListExpandedTitle() {
        return "规则卡片（右键打开菜单）";
    }

    protected String getEditorPanelTitle() {
        return getEntityDisplayName() + "编辑器";
    }

    protected String getAllItemsLabel() {
        return "全部" + getEntityDisplayName() + "s";
    }

    protected String getEmptyTreeText() {
        return "暂无" + getEntityDisplayName();
    }

    protected String getEmptyListText() {
        return "该分组下暂无" + getEntityDisplayName();
    }

    protected void onSelectionChanged(T selected) {
    }

    protected void updateEditorButtonStates() {
    }

    protected void onAfterEditorMouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    }

    protected boolean handleEditorMouseWheel(int mouseX, int mouseY, int wheel) {
        return false;
    }

    protected void onMouseWheelOutsidePanels(int mouseX, int mouseY, int wheel) {
    }

    protected void onUpdateEditor() {
    }

    protected void onEscape() {
    }

    protected void onDone() {
    }

    protected void initAdditionalButtons() {
    }

    protected void layoutAdditionalButtons() {
    }

    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        return false;
    }

    protected boolean handleCustomContextMenuAction(String key) {
        return false;
    }

    protected void handleCustomTreeContextMenuAction(String key, String targetCategory) {
    }

    protected void appendCustomCardContextMenuItems(List<ContextMenuItem> items, T selectedItem) {
    }

    protected void appendCustomTreeContextMenuItems(List<ContextMenuItem> items, TreeRow row) {
    }

    protected String validateItem(T item) {
        return null;
    }

    protected abstract String getScreenTitle();

    protected abstract String getGuideText();

    protected abstract String getEntityDisplayName();

    protected abstract List<T> getSourceItems();

    protected abstract List<String> getSourceCategories();

    protected abstract boolean addCategoryToSource(String category);

    protected abstract boolean renameCategoryInSource(String oldCategory, String newCategory);

    protected abstract boolean deleteCategoryInSource(String category);

    protected abstract void persistChanges();

    protected abstract void reloadSource();

    protected abstract T createNewItem();

    protected abstract T copyItem(T source);

    protected abstract void addItemToSource(T item);

    protected abstract void removeItemFromSource(T item);

    protected abstract String getItemName(T item);

    protected abstract void setItemName(T item, String name);

    protected abstract String getItemCategory(T item);

    protected abstract void setItemCategory(T item, String category);

    protected abstract void loadEditor(T item);

    protected abstract T buildItemFromEditor(boolean creatingNew, T selectedItem);

    protected abstract void applyItemValues(T target, T source);

    protected abstract void initEditorWidgets();

    protected abstract void layoutEditorWidgets();

    protected abstract int getEditorTotalRows();

    protected abstract String getEditorRowLabel(int row);

    protected abstract List<GuiTextField> getEditorFields();

    protected abstract void drawEditorContents(int mouseX, int mouseY, float partialTicks);

    protected abstract void drawCard(T item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered);

    protected void afterItemSaved(T item, boolean createdNow) {
        persistChanges();
    }

    protected void afterItemDeleted(T item) {
        persistChanges();
    }

    protected void afterItemDuplicated(T item) {
        persistChanges();
    }

    protected static final class TreeRow {
        private static final int TYPE_ALL = 0;
        private static final int TYPE_CATEGORY = 1;
        private static final int TYPE_ITEM = 2;

        private final int type;
        private final String label;
        private final String category;
        private final String itemName;
        private final int indent;

        private TreeRow(int type, String label, String category, String itemName, int indent) {
            this.type = type;
            this.label = label == null ? "" : label;
            this.category = category == null ? "" : category;
            this.itemName = itemName == null ? "" : itemName;
            this.indent = indent;
        }

        private static TreeRow allRow(String label) {
            return new TreeRow(TYPE_ALL, label, CATEGORY_ALL, "", 0);
        }

        private static TreeRow categoryRow(String category) {
            return new TreeRow(TYPE_CATEGORY, category, category, "", 0);
        }

        private static TreeRow itemRow(String category, String itemName, String displayName) {
            return new TreeRow(TYPE_ITEM, displayName, category, itemName, 1);
        }
    }

    protected static class ContextMenuItem {
        private final String key;
        private final String label;
        private final boolean enabled;

        protected ContextMenuItem(String key, String label, boolean enabled) {
            this.key = key;
            this.label = label;
            this.enabled = enabled;
        }
    }
}




