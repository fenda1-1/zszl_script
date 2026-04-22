// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/path/GuiSequenceSelector.java
package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.MainUiLayoutManager;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

public class GuiSequenceSelector extends ThemedGuiScreen {
    private static final int BTN_CANCEL = 0;
    private static final int SEARCH_FIELD_ID = 9100;
    private static final String BUILTIN_SUFFIX = "内置";
    private static final String UNGROUPED_TITLE = "未分类";
    private static final String SEARCH_PLACEHOLDER = "搜索序列...";
    private static final String UNGROUPED_KEY = "__ungrouped__";

    private final GuiScreen parentScreen;
    private final Consumer<String> onSelect;

    private final Set<String> collapsedSequenceGroups = new LinkedHashSet<>();

    private List<String> categories = new ArrayList<>();
    private List<PathSequence> sequencesInCategory = new ArrayList<>();
    private List<CategoryTreeRow> visibleCategoryRows = new ArrayList<>();
    private List<SequenceListRow> sequenceRows = new ArrayList<>();

    private String selectedCategory = "";
    private String selectedSubCategory = "";
    private String searchQuery = "";

    private GuiTextField searchField;

    private int categoryScrollOffset = 0;
    private int sequenceScrollOffset = 0;
    private int maxCategoryScroll = 0;
    private int maxSequenceScroll = 0;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int titleY;
    private int searchFieldX;
    private int searchFieldY;
    private int searchFieldWidth;
    private int searchFieldHeight;
    private int listY;
    private int listHeight;
    private int categoryListX;
    private int categoryListWidth;
    private int sequenceListX;
    private int sequenceListWidth;
    private int itemHeight;
    private int bottomButtonY;
    private int bottomButtonW;
    private int bottomButtonH;

    private static final class CategoryTreeRow {
        final String category;
        final String subCategory;
        final boolean customCategory;
        Rectangle bounds;
        Rectangle toggleBounds;

        private CategoryTreeRow(String category, String subCategory, boolean customCategory) {
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.customCategory = customCategory;
        }

        boolean isSubCategory() {
            return !subCategory.isEmpty();
        }

        boolean isCustomRoot() {
            return customCategory && subCategory.isEmpty();
        }
    }

    private static final class SequenceListRow {
        final boolean header;
        final String title;
        final String subCategory;
        final String groupKey;
        final boolean collapsed;
        final PathSequence sequence;
        Rectangle bounds;

        private SequenceListRow(boolean header, String title, String subCategory, String groupKey, boolean collapsed,
                PathSequence sequence) {
            this.header = header;
            this.title = title == null ? "" : title;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.groupKey = groupKey == null ? "" : groupKey;
            this.collapsed = collapsed;
            this.sequence = sequence;
        }

        static SequenceListRow header(String title, String subCategory, String groupKey, boolean collapsed) {
            return new SequenceListRow(true, title, subCategory, groupKey, collapsed, null);
        }

        static SequenceListRow sequence(PathSequence sequence) {
            return new SequenceListRow(false, "", "", "", false, sequence);
        }
    }

    public GuiSequenceSelector(GuiScreen parent, Consumer<String> onSelectCallback) {
        this.parentScreen = parent;
        this.onSelect = onSelectCallback;
    }

    private float uiScale() {
        float sx = this.width / 430.0f;
        float sy = this.height / 290.0f;
        return Math.max(0.72f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    private void computeLayout() {
        panelWidth = Math.max(320, Math.min(s(420), this.width - s(20)));
        panelHeight = Math.max(220, Math.min(s(260), this.height - s(20)));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int pad = s(10);
        int gap = s(10);
        itemHeight = s(20);

        titleY = panelY + s(11);

        searchFieldWidth = Math.max(s(110), Math.min(s(150), panelWidth / 3));
        searchFieldHeight = s(18);
        searchFieldX = panelX + panelWidth - pad - searchFieldWidth;
        searchFieldY = panelY + s(8);

        listY = searchFieldY + searchFieldHeight + s(8);
        bottomButtonH = s(20);
        bottomButtonW = s(150);
        bottomButtonY = panelY + panelHeight - bottomButtonH - s(10);
        listHeight = Math.max(s(100), bottomButtonY - listY - s(10));

        categoryListX = panelX + pad;
        categoryListWidth = Math.max(s(100), Math.min(s(140), panelWidth / 3));

        sequenceListX = categoryListX + categoryListWidth + gap;
        sequenceListWidth = panelX + panelWidth - pad - sequenceListX;

        if (searchField != null) {
            searchField.x = searchFieldX;
            searchField.y = searchFieldY;
            searchField.width = searchFieldWidth;
            searchField.height = searchFieldHeight;
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        computeLayout();

        MainUiLayoutManager.ensureLoaded();
        categories = new ArrayList<>(PathSequenceManager.getVisibleCategories());

        if (selectedCategory == null || selectedCategory.trim().isEmpty() || !categories.contains(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? "" : categories.get(0);
            selectedSubCategory = "";
        }

        if (searchField == null) {
            searchField = new GuiTextField(SEARCH_FIELD_ID, this.fontRenderer, searchFieldX, searchFieldY,
                    searchFieldWidth, searchFieldHeight);
            searchField.setMaxStringLength(128);
        } else {
            searchField.x = searchFieldX;
            searchField.y = searchFieldY;
            searchField.width = searchFieldWidth;
            searchField.height = searchFieldHeight;
        }
        searchField.setText(searchQuery == null ? "" : searchQuery);
        searchField.setFocused(false);

        syncSelectedCategoryState();
        rebuildLists(true, true);

        int centerX = this.width / 2;
        this.buttonList.add(new ThemedButton(BTN_CANCEL, centerX - bottomButtonW / 2, bottomButtonY, bottomButtonW,
                bottomButtonH, I18n.format("gui.common.cancel")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeKey(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    private boolean isCustomCategory(String category) {
        String normalizedCategory = normalize(category);
        if (normalizedCategory.isEmpty()) {
            return false;
        }
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (sequence != null && sequence.isCustom()
                    && normalizedCategory.equalsIgnoreCase(normalize(sequence.getCategory()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRootCategoryMode() {
        return isCustomCategory(selectedCategory) && normalize(selectedSubCategory).isEmpty();
    }

    private String buildGroupKey(String category, String subCategory) {
        String normalizedCategory = normalizeKey(category);
        String normalizedSubCategory = normalize(subCategory).isEmpty() ? UNGROUPED_KEY : normalizeKey(subCategory);
        return normalizedCategory + "::" + normalizedSubCategory;
    }

    private boolean isGroupCollapsed(String groupKey) {
        return groupKey != null && !groupKey.isEmpty() && collapsedSequenceGroups.contains(groupKey);
    }

    private void toggleGroupCollapsed(String groupKey) {
        if (groupKey == null || groupKey.isEmpty()) {
            return;
        }
        if (!collapsedSequenceGroups.add(groupKey)) {
            collapsedSequenceGroups.remove(groupKey);
        }
    }

    private List<PathSequence> getSequencesForCategory(String category) {
        String normalizedCategory = normalize(category);
        if (normalizedCategory.isEmpty()) {
            return Collections.emptyList();
        }
        return PathSequenceManager.getAllSequences().stream()
                .filter(sequence -> sequence != null
                        && normalizedCategory.equalsIgnoreCase(normalize(sequence.getCategory())))
                .collect(Collectors.toList());
    }

    private String getSequenceDisplayName(PathSequence sequence) {
        if (sequence == null) {
            return "";
        }
        return sequence.isCustom() ? normalize(sequence.getName())
                : normalize(I18n.format("gui.path.builtin_name", sequence.getName(), BUILTIN_SUFFIX));
    }

    private boolean matchesSearch(PathSequence sequence, String normalizedQuery) {
        if (sequence == null) {
            return false;
        }
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String searchText = normalize(sequence.getName()) + " "
                + getSequenceDisplayName(sequence) + " "
                + normalize(sequence.getCategory()) + " "
                + normalize(sequence.getSubCategory());
        return PinyinSearchHelper.matchesNormalized(searchText, normalizedQuery);
    }

    private List<PathSequence> applySearchFilter(List<PathSequence> sequences) {
        String normalizedQuery = PinyinSearchHelper.normalizeQuery(searchQuery);
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>(sequences);
        }

        List<PathSequence> filtered = new ArrayList<>();
        for (PathSequence sequence : sequences) {
            if (matchesSearch(sequence, normalizedQuery)) {
                filtered.add(sequence);
            }
        }
        return filtered;
    }

    private void syncSelectedCategoryState() {
        if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
            selectedSubCategory = "";
            return;
        }
        if (!isCustomCategory(selectedCategory)) {
            selectedSubCategory = "";
            return;
        }
        List<String> subCategories = MainUiLayoutManager.getSubCategories(selectedCategory);
        String normalizedSelectedSubCategory = normalize(selectedSubCategory);
        if (!normalizedSelectedSubCategory.isEmpty()) {
            boolean exists = false;
            for (String subCategory : subCategories) {
                if (normalizedSelectedSubCategory.equalsIgnoreCase(normalize(subCategory))) {
                    selectedSubCategory = subCategory;
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                selectedSubCategory = "";
            }
        }
    }

    private List<CategoryTreeRow> buildVisibleCategoryRows() {
        List<CategoryTreeRow> rows = new ArrayList<>();
        for (String category : categories) {
            boolean customCategory = isCustomCategory(category);
            rows.add(new CategoryTreeRow(category, "", customCategory));
            if (customCategory && !MainUiLayoutManager.isCollapsed(category)) {
                for (String subCategory : MainUiLayoutManager.getSubCategories(category)) {
                    rows.add(new CategoryTreeRow(category, subCategory, true));
                }
            }
        }
        return rows;
    }

    private void addGroupRows(List<SequenceListRow> rows, String category, String title, String subCategory,
            List<PathSequence> sequences, boolean includeWhenEmpty) {
        if (!includeWhenEmpty && (sequences == null || sequences.isEmpty())) {
            return;
        }

        String groupKey = buildGroupKey(category, subCategory);
        boolean collapsed = isGroupCollapsed(groupKey);
        rows.add(SequenceListRow.header(title, subCategory, groupKey, collapsed));
        if (!collapsed && sequences != null) {
            for (PathSequence sequence : sequences) {
                rows.add(SequenceListRow.sequence(sequence));
            }
        }
    }

    private List<SequenceListRow> buildSequenceRows() {
        if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
            sequencesInCategory = Collections.emptyList();
            return Collections.emptyList();
        }

        List<PathSequence> allSequencesInCategory = getSequencesForCategory(selectedCategory);
        List<PathSequence> filteredSequences = applySearchFilter(allSequencesInCategory);
        sequencesInCategory = new ArrayList<>(filteredSequences);

        if (!isCustomCategory(selectedCategory)) {
            List<SequenceListRow> rows = new ArrayList<>();
            for (PathSequence sequence : filteredSequences) {
                rows.add(SequenceListRow.sequence(sequence));
            }
            return rows;
        }

        String normalizedSelectedSubCategory = normalize(selectedSubCategory);
        List<SequenceListRow> rows = new ArrayList<>();

        if (!normalizedSelectedSubCategory.isEmpty()) {
            List<PathSequence> matched = new ArrayList<>();
            for (PathSequence sequence : filteredSequences) {
                if (normalizedSelectedSubCategory.equalsIgnoreCase(normalize(sequence.getSubCategory()))) {
                    matched.add(sequence);
                }
            }
            if (!matched.isEmpty() || normalize(searchQuery).isEmpty()) {
                rows.add(SequenceListRow.header(selectedSubCategory, selectedSubCategory,
                        buildGroupKey(selectedCategory, selectedSubCategory), false));
            }
            for (PathSequence sequence : matched) {
                rows.add(SequenceListRow.sequence(sequence));
            }
            return rows;
        }

        Set<String> addedSubCategories = new LinkedHashSet<>();
        for (String subCategory : MainUiLayoutManager.getSubCategories(selectedCategory)) {
            List<PathSequence> sectionSequences = new ArrayList<>();
            for (PathSequence sequence : filteredSequences) {
                if (normalize(subCategory).equalsIgnoreCase(normalize(sequence.getSubCategory()))) {
                    sectionSequences.add(sequence);
                }
            }
            addGroupRows(rows, selectedCategory, subCategory, subCategory, sectionSequences,
                    normalize(searchQuery).isEmpty());
            addedSubCategories.add(normalizeKey(subCategory));
        }

        Map<String, List<PathSequence>> extraGroups = new LinkedHashMap<>();
        List<PathSequence> ungroupedSequences = new ArrayList<>();
        for (PathSequence sequence : filteredSequences) {
            String sequenceSubCategory = normalize(sequence.getSubCategory());
            if (sequenceSubCategory.isEmpty()) {
                ungroupedSequences.add(sequence);
                continue;
            }

            String normalizedSubCategory = normalizeKey(sequenceSubCategory);
            if (!addedSubCategories.contains(normalizedSubCategory)) {
                List<PathSequence> section = extraGroups.get(sequenceSubCategory);
                if (section == null) {
                    section = new ArrayList<>();
                    extraGroups.put(sequenceSubCategory, section);
                }
                section.add(sequence);
            }
        }

        for (Map.Entry<String, List<PathSequence>> entry : extraGroups.entrySet()) {
            addGroupRows(rows, selectedCategory, entry.getKey(), entry.getKey(), entry.getValue(), true);
            addedSubCategories.add(normalizeKey(entry.getKey()));
        }

        if (!ungroupedSequences.isEmpty()) {
            addGroupRows(rows, selectedCategory, UNGROUPED_TITLE, "", ungroupedSequences, true);
        }

        return rows;
    }

    private void rebuildLists(boolean resetCategoryScroll, boolean resetSequenceScroll) {
        syncSelectedCategoryState();
        visibleCategoryRows = buildVisibleCategoryRows();
        sequenceRows = buildSequenceRows();
        if (resetCategoryScroll) {
            categoryScrollOffset = 0;
        }
        if (resetSequenceScroll) {
            sequenceScrollOffset = 0;
        }
    }

    private void finishSelection(String sequenceName) {
        String finalSequenceName = sequenceName == null ? "" : sequenceName;
        if (onSelect != null) {
            onSelect.accept(finalSequenceName);
        }
        if (mc.screen == this) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CANCEL) {
            finishSelection("");
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        computeLayout();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawString(fontRenderer, I18n.format("gui.path.select_sequence"), panelX + s(12), titleY, 0xFFFFFFFF);

        drawThemedTextField(searchField);
        if (searchField != null && normalize(searchField.getText()).isEmpty() && !searchField.isFocused()) {
            drawString(fontRenderer, SEARCH_PLACEHOLDER, searchField.x + s(5),
                    searchField.y + (searchField.height - fontRenderer.FONT_HEIGHT) / 2, 0xFF8A96A8);
        }

        drawCategoryList(mouseX, mouseY);
        drawSequenceList(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCategoryList(int mouseX, int mouseY) {
        int listX = categoryListX;
        int listY = this.listY;
        int listWidth = categoryListWidth;
        int listHeight = this.listHeight;

        GuiTheme.drawInputFrameSafe(listX, listY, listWidth, listHeight, false, true);

        int visibleItems = Math.max(1, listHeight / itemHeight);
        maxCategoryScroll = Math.max(0, visibleCategoryRows.size() - visibleItems);
        categoryScrollOffset = MathHelper.clamp(categoryScrollOffset, 0, maxCategoryScroll);

        for (int local = 0; local < visibleItems; local++) {
            int index = local + categoryScrollOffset;
            if (index >= visibleCategoryRows.size()) {
                break;
            }

            CategoryTreeRow row = visibleCategoryRows.get(index);
            int itemY = listY + local * itemHeight;
            int indent = row.isSubCategory() ? s(12) : 0;
            int buttonX = listX + 2 + indent;
            int buttonWidth = listWidth - 4 - indent;
            Rectangle bounds = new Rectangle(buttonX, itemY + 1, buttonWidth, itemHeight - 2);
            row.bounds = bounds;
            row.toggleBounds = null;

            boolean selected = row.category.equals(selectedCategory)
                    && (row.isSubCategory()
                            ? normalize(row.subCategory).equalsIgnoreCase(normalize(selectedSubCategory))
                            : normalize(selectedSubCategory).isEmpty());
            boolean hovered = bounds.contains(mouseX, mouseY);

            GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                    resolveListState(selected, hovered));

            int labelX = bounds.x + 6;
            if (row.isCustomRoot()) {
                row.toggleBounds = new Rectangle(bounds.x + 3, bounds.y + 2, s(12), Math.max(8, bounds.height - 4));
                String arrow = MainUiLayoutManager.isCollapsed(row.category) ? ">" : "v";
                drawString(fontRenderer, arrow, row.toggleBounds.x + 1,
                        itemY + (itemHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFB8CCE0);
                labelX += s(10);
            }

            String label = row.isSubCategory() ? row.subCategory : row.category;
            String trimmed = fontRenderer.trimStringToWidth(label, Math.max(10, bounds.width - (labelX - bounds.x) - 4));
            drawString(fontRenderer, trimmed, labelX,
                    itemY + (itemHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }
    }

    private void drawSequenceList(int mouseX, int mouseY) {
        int listX = sequenceListX;
        int listY = this.listY;
        int listWidth = sequenceListWidth;
        int listHeight = this.listHeight;

        GuiTheme.drawInputFrameSafe(listX, listY, listWidth, listHeight, false, true);

        int visibleItems = Math.max(1, listHeight / itemHeight);
        maxSequenceScroll = Math.max(0, sequenceRows.size() - visibleItems);
        sequenceScrollOffset = MathHelper.clamp(sequenceScrollOffset, 0, maxSequenceScroll);

        if (sequenceRows.isEmpty()) {
            drawCenteredString(fontRenderer, "当前筛选下没有序列", listX + listWidth / 2,
                    listY + listHeight / 2 - fontRenderer.FONT_HEIGHT / 2, 0xFFBBBBBB);
            return;
        }

        for (int local = 0; local < visibleItems; local++) {
            int index = local + sequenceScrollOffset;
            if (index >= sequenceRows.size()) {
                break;
            }

            SequenceListRow row = sequenceRows.get(index);
            int itemY = listY + local * itemHeight;
            Rectangle bounds = new Rectangle(listX + 2, itemY + 1, listWidth - 4, itemHeight - 2);
            row.bounds = bounds;
            boolean hovered = bounds.contains(mouseX, mouseY);

            if (row.header) {
                drawHeaderRow(bounds, row, hovered);
            } else if (row.sequence != null) {
                drawSequenceRow(bounds, row.sequence, hovered);
            }
        }
    }

    private void drawHeaderRow(Rectangle bounds, SequenceListRow row, boolean hovered) {
        int bgColor = hovered ? 0x55355872 : 0x44324458;
        GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawRect(bounds.x + 1, bounds.y + 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1, bgColor);

        String arrow = row.collapsed ? ">" : "v";
        String prefix = isRootCategoryMode() ? arrow + " " : "▼ ";
        String displayTitle = fontRenderer.trimStringToWidth(prefix + row.title, bounds.width - s(10));
        drawString(fontRenderer, displayTitle, bounds.x + 6,
                bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFB8D6F0);
    }

    private void drawSequenceRow(Rectangle bounds, PathSequence sequence, boolean hovered) {
        GuiTheme.drawButtonFrameSafe(bounds.x, bounds.y, bounds.width, bounds.height,
                hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);

        String trimmed = fontRenderer.trimStringToWidth(getSequenceDisplayName(sequence), bounds.width - s(12));
        drawString(fontRenderer, trimmed, bounds.x + 6,
                bounds.y + (bounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
    }

    private GuiTheme.UiState resolveListState(boolean selected, boolean hovered) {
        if (selected) {
            return GuiTheme.UiState.SELECTED;
        }
        return hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private CategoryTreeRow findCategoryRowAt(int mouseX, int mouseY) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private SequenceListRow findSequenceRowAt(int mouseX, int mouseY) {
        for (SequenceListRow row : sequenceRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private void updateSearchQueryFromField() {
        String newQuery = searchField == null ? "" : normalize(searchField.getText());
        if (!normalize(searchQuery).equals(newQuery)) {
            searchQuery = newQuery;
            rebuildLists(false, true);
        } else {
            searchQuery = newQuery;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton != 0) {
            return;
        }

        CategoryTreeRow clickedCategoryRow = findCategoryRowAt(mouseX, mouseY);
        if (clickedCategoryRow != null) {
            boolean clickArrow = clickedCategoryRow.isCustomRoot()
                    && clickedCategoryRow.toggleBounds != null
                    && clickedCategoryRow.toggleBounds.contains(mouseX, mouseY);
            if (clickArrow) {
                MainUiLayoutManager.toggleCollapsed(clickedCategoryRow.category);
                rebuildLists(false, true);
                return;
            }

            selectedCategory = clickedCategoryRow.category;
            selectedSubCategory = clickedCategoryRow.isSubCategory() ? clickedCategoryRow.subCategory : "";
            rebuildLists(false, true);
            return;
        }

        SequenceListRow clickedSequenceRow = findSequenceRowAt(mouseX, mouseY);
        if (clickedSequenceRow != null) {
            if (clickedSequenceRow.header) {
                if (isRootCategoryMode() && !clickedSequenceRow.groupKey.isEmpty()) {
                    toggleGroupCollapsed(clickedSequenceRow.groupKey);
                    rebuildLists(false, false);
                }
                return;
            }
            if (clickedSequenceRow.sequence != null) {
                finishSelection(clickedSequenceRow.sequence.getName());
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            finishSelection("");
            return;
        }

        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateSearchQueryFromField();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (isInside(mouseX, mouseY, categoryListX, listY, categoryListWidth, listHeight)) {
            categoryScrollOffset = dWheel > 0
                    ? Math.max(0, categoryScrollOffset - 1)
                    : Math.min(maxCategoryScroll, categoryScrollOffset + 1);
            return;
        }

        if (isInside(mouseX, mouseY, sequenceListX, listY, sequenceListWidth, listHeight)) {
            sequenceScrollOffset = dWheel > 0
                    ? Math.max(0, sequenceScrollOffset - 1)
                    : Math.min(maxSequenceScroll, sequenceScrollOffset + 1);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}







