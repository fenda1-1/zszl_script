// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/config/GuiKeybindManager.java
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.packet.GuiPacketSequenceSelector;
import com.zszl.zszlScriptMod.gui.path.GuiKeybindRecorder;
import com.zszl.zszlScriptMod.system.BindableAction;
import com.zszl.zszlScriptMod.system.KeybindManager;
import com.zszl.zszlScriptMod.system.KeybindManager.Keybind;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GuiKeybindManager extends ThemedGuiScreen {

    private static final int BUTTON_SAVE = 100;
    private static final int BUTTON_BACK = 101;
    private static final int TITLE_HEIGHT = 20;
    private static final int TABLE_HEADER_HEIGHT = 22;
    private static final int ITEM_HEIGHT = 28;

    private static class RowEntry {
        final boolean actionRow;
        final BindableAction action;
        final String sequenceName;

        RowEntry(BindableAction action) {
            this.actionRow = true;
            this.action = action;
            this.sequenceName = null;
        }

        RowEntry(String sequenceName) {
            this.actionRow = false;
            this.action = null;
            this.sequenceName = sequenceName;
        }

        boolean isMovementAction() {
            return this.actionRow && this.action.isMovementFeatureToggle();
        }

        boolean isPacketAction() {
            return this.actionRow && this.action == BindableAction.EXECUTE_SPECIFIC_PACKET_SEQUENCE;
        }

        String getGroupLabel() {
            if (!this.actionRow) {
                return I18n.format("gui.keybind.group.scripts");
            }
            if (isPacketAction()) {
                return I18n.format("gui.keybind.group.packet");
            }
            if (this.action.getFeatureGroup() != null) {
                return I18n.format(this.action.getFeatureGroup().getTranslationKey());
            }
            return I18n.format("gui.keybind.group.actions");
        }

        String getName() {
            return this.actionRow ? this.action.getDisplayName() : this.sequenceName;
        }

        String getDescription() {
            if (this.actionRow) {
                return this.action.getDescription();
            }
            return I18n.format("gui.keybind.script_desc");
        }
    }

    private final GuiScreen parentScreen;
    private final List<RowEntry> filteredRows = new ArrayList<>();

    private GuiTextField searchField;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int searchX;
    private int searchY;
    private int searchWidth;
    private int statsX;
    private int statsY;
    private int statsWidth;
    private int helperY;
    private int tableX;
    private int tableY;
    private int tableWidth;
    private int tableHeight;
    private int listTop;
    private int listBottom;
    private int listHeight;
    private int footerHintY;
    private int buttonY;

    private String hoveredTooltip;

    public GuiKeybindManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        KeybindManager.syncPathSequenceKeybinds();
        layoutScreen();

        if (this.searchField == null) {
            this.searchField = new GuiTextField(9000, this.fontRenderer, this.searchX, this.searchY, this.searchWidth, 20);
            this.searchField.setMaxStringLength(100);
        } else {
            this.searchField.x = this.searchX;
            this.searchField.y = this.searchY;
            this.searchField.width = this.searchWidth;
            this.searchField.height = 20;
        }

        int buttonWidth = Math.max(90, (this.panelWidth - 26) / 2);
        this.buttonList.add(new ThemedButton(BUTTON_SAVE, this.panelX + 10, this.buttonY, buttonWidth, 20,
                I18n.format("gui.keybind.save_close")));
        this.buttonList.add(new ThemedButton(BUTTON_BACK, this.panelX + this.panelWidth - 10 - buttonWidth, this.buttonY,
                buttonWidth, 20, I18n.format("gui.keybind.back_nosave")));

        rebuildFilteredRows();
        refreshScrollBounds();
    }

    private void layoutScreen() {
        this.panelWidth = Math.min(780, this.width - 24);
        this.panelWidth = Math.max(420, this.panelWidth);
        if (this.panelWidth > this.width - 24) {
            this.panelWidth = this.width - 24;
        }

        this.panelHeight = Math.min(420, this.height - 24);
        this.panelHeight = Math.max(280, this.panelHeight);
        if (this.panelHeight > this.height - 24) {
            this.panelHeight = this.height - 24;
        }

        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        int contentX = this.panelX + 10;
        int contentWidth = this.panelWidth - 20;
        this.statsWidth = Math.min(150, Math.max(112, contentWidth / 4));
        this.searchX = contentX + 48;
        this.searchY = this.panelY + TITLE_HEIGHT + 8;
        this.searchWidth = contentWidth - 48 - this.statsWidth - 8;
        this.statsX = this.searchX + this.searchWidth + 8;
        this.statsY = this.searchY;
        this.helperY = this.searchY + 28;
        this.buttonY = this.panelY + this.panelHeight - 30;
        this.footerHintY = this.buttonY - 16;
        this.tableX = contentX;
        this.tableY = this.helperY + 22;
        this.tableWidth = contentWidth;
        this.tableHeight = this.footerHintY - 8 - this.tableY;
        this.listTop = this.tableY + TABLE_HEADER_HEIGHT;
        this.listBottom = this.tableY + this.tableHeight - 6;
        this.listHeight = Math.max(ITEM_HEIGHT, this.listBottom - this.listTop);
    }

    private void refreshScrollBounds() {
        int visibleItems = getVisibleItems();
        this.maxScroll = Math.max(0, this.filteredRows.size() - visibleItems);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
    }

    private int getVisibleItems() {
        return Math.max(1, this.listHeight / ITEM_HEIGHT);
    }

    private void rebuildFilteredRows() {
        String keyword = PinyinSearchHelper.normalizeQuery(this.searchField == null ? "" : this.searchField.getText());
        this.filteredRows.clear();

        for (BindableAction action : BindableAction.values()) {
            RowEntry row = new RowEntry(action);
            if (keyword.isEmpty() || PinyinSearchHelper.matchesNormalized(buildSearchText(row), keyword)) {
                this.filteredRows.add(row);
            }
        }

        List<String> sequenceNames = new ArrayList<>(KeybindManager.pathSequenceKeybinds.keySet());
        sequenceNames.sort(Comparator.naturalOrder());
        for (String sequenceName : sequenceNames) {
            RowEntry row = new RowEntry(sequenceName);
            if (keyword.isEmpty() || PinyinSearchHelper.matchesNormalized(buildSearchText(row), keyword)) {
                this.filteredRows.add(row);
            }
        }
    }

    private String buildSearchText(RowEntry row) {
        StringBuilder builder = new StringBuilder();
        builder.append(row.getGroupLabel()).append(' ');
        builder.append(row.getName()).append(' ');
        builder.append(row.getDescription()).append(' ');

        Keybind keybind = getRowKeybind(row);
        String bindingText = getBindingText(row, keybind, false);
        if (!bindingText.isEmpty()) {
            builder.append(bindingText).append(' ');
        }

        if (row.isPacketAction() && keybind != null && keybind.getParameter() != null) {
            builder.append(keybind.getParameter()).append(' ');
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private int getTotalRowCount() {
        return BindableAction.values().length + KeybindManager.pathSequenceKeybinds.size();
    }

    private Keybind getRowKeybind(RowEntry row) {
        if (row.actionRow) {
            return KeybindManager.keybinds.get(row.action);
        }
        return KeybindManager.pathSequenceKeybinds.get(row.sequenceName);
    }

    private String getBindingText(RowEntry row, Keybind keybind, boolean fullText) {
        String keyName = (keybind == null || keybind.getKeyCode() == Keyboard.KEY_NONE)
                ? I18n.format("gui.keybind.unbound")
                : keybind.toString();
        if (!row.isPacketAction()) {
            return keyName;
        }

        String parameter = (keybind != null && keybind.getParameter() != null && !keybind.getParameter().trim().isEmpty())
                ? keybind.getParameter()
                : I18n.format("gui.keybind.unselected");
        String pattern = fullText ? "gui.keybind.packet_target_full" : "gui.keybind.packet_target_short";
        return keyName + " · " + I18n.format(pattern, parameter);
    }

    private String getRowDetailText(RowEntry row, Keybind keybind) {
        if (row.isPacketAction()) {
            String parameter = (keybind != null && keybind.getParameter() != null && !keybind.getParameter().trim().isEmpty())
                    ? keybind.getParameter()
                    : I18n.format("gui.keybind.unselected");
            return I18n.format("gui.keybind.packet_detail", parameter);
        }
        if (!row.actionRow) {
            return I18n.format("gui.keybind.script_detail");
        }
        return row.getDescription();
    }

    private int getGroupFillColor(RowEntry row) {
        if (!row.actionRow) {
            return 0xAA27473A;
        }
        if (row.isPacketAction()) {
            return 0xAA5A4424;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.MOVEMENT) {
            return 0xAA23485E;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.BLOCK) {
            return 0xAA56422A;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.ITEM) {
            return 0xAA47335E;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.RENDER) {
            return 0xAA1E5660;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.WORLD) {
            return 0xAA2A5A3C;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.MISC) {
            return 0xAA5A3150;
        }
        return 0xAA283E59;
    }

    private int getGroupBorderColor(RowEntry row) {
        if (!row.actionRow) {
            return 0xFF7FD9A8;
        }
        if (row.isPacketAction()) {
            return 0xFFF0C674;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.MOVEMENT) {
            return 0xFF7ED8FF;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.BLOCK) {
            return 0xFFF0C27B;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.ITEM) {
            return 0xFFD1A8FF;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.RENDER) {
            return 0xFF79EEFF;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.WORLD) {
            return 0xFF93E2A7;
        }
        if (row.action.getFeatureGroup() == BindableAction.FeatureGroup.MISC) {
            return 0xFFFFA6DF;
        }
        return 0xFF8FB4FF;
    }

    private int getScrollbarX() {
        return this.tableX + this.tableWidth - 8;
    }

    private int getScrollbarThumbHeight() {
        int visibleItems = getVisibleItems();
        return Math.max(18, (int) ((visibleItems / (float) Math.max(visibleItems, this.filteredRows.size())) * this.listHeight));
    }

    private void updateScrollFromMouse(int mouseY) {
        if (this.maxScroll <= 0) {
            return;
        }
        int thumbHeight = getScrollbarThumbHeight();
        int trackHeight = Math.max(1, this.listHeight - thumbHeight);
        float percent = (float) (mouseY - this.listTop - thumbHeight / 2) / trackHeight;
        percent = Math.max(0.0f, Math.min(1.0f, percent));
        this.scrollOffset = Math.round(percent * this.maxScroll);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (this.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.fontRenderer.getStringWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return this.fontRenderer.trimStringToWidth(text, maxWidth);
        }
        return this.fontRenderer.trimStringToWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    private void setTooltipIfTrimmed(String fullText, String drawnText, int mouseX, int mouseY, int x, int y, int width,
            int height) {
        if (!isHoverRegion(mouseX, mouseY, x, y, width, height)) {
            return;
        }
        if (fullText != null && !fullText.equals(drawnText)) {
            this.hoveredTooltip = fullText;
        }
    }

    private void drawPlaceholderText() {
        if (this.searchField == null || this.searchField.isFocused() || !this.searchField.getText().isEmpty()) {
            return;
        }
        drawString(this.fontRenderer, I18n.format("gui.keybind.search_placeholder"), this.searchField.x + 4,
                this.searchField.y + 6, GuiTheme.SUB_TEXT);
    }

    private void drawStatsBox() {
        GuiTheme.drawButtonFrameSafe(this.statsX, this.statsY, this.statsWidth, 20, GuiTheme.UiState.NORMAL);
        String summary = I18n.format("gui.keybind.summary", this.filteredRows.size(), getTotalRowCount());
        drawCenteredString(this.fontRenderer, summary, this.statsX + this.statsWidth / 2, this.statsY + 6, 0xFFD9F2FF);
    }

    private void drawHelperBar() {
        drawRect(this.panelX + 10, this.helperY, this.panelX + this.panelWidth - 10, this.helperY + 16, 0x88324E67);
        drawRect(this.panelX + 10, this.helperY, this.panelX + this.panelWidth - 10, this.helperY + 1, 0xAA77CFFF);
        drawString(this.fontRenderer, I18n.format("gui.keybind.tip.table"), this.panelX + 14, this.helperY + 4,
                0xFFCAE6F7);
    }

    private void drawTable(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(this.tableX, this.tableY, this.tableWidth, this.tableHeight,
                this.panelX, this.panelY, this.panelWidth, this.panelHeight);

        int rowLeft = this.tableX + 6;
        int rowRight = this.tableX + this.tableWidth - 12;
        int availableWidth = rowRight - rowLeft;

        int groupWidth = Math.max(52, Math.min(76, availableWidth / 7));
        int actionWidth = Math.max(52, Math.min(64, availableWidth / 8));
        int keyWidth = Math.max(88, Math.min(150, availableWidth / 5));
        int detailWidth = Math.max(96, availableWidth / 4);
        int nameWidth = availableWidth - groupWidth - keyWidth - detailWidth - actionWidth;

        if (nameWidth < 120) {
            int shortage = 120 - nameWidth;

            int reducible = Math.max(0, detailWidth - 72);
            int reduction = Math.min(shortage, reducible);
            detailWidth -= reduction;
            shortage -= reduction;

            reducible = Math.max(0, keyWidth - 82);
            reduction = Math.min(shortage, reducible);
            keyWidth -= reduction;
            shortage -= reduction;

            reducible = Math.max(0, groupWidth - 48);
            reduction = Math.min(shortage, reducible);
            groupWidth -= reduction;

            nameWidth = availableWidth - groupWidth - keyWidth - detailWidth - actionWidth;
        }

        int groupX = rowLeft;
        int nameX = groupX + groupWidth;
        int keyX = nameX + nameWidth;
        int detailX = keyX + keyWidth;
        int actionX = detailX + detailWidth;

        drawRect(this.tableX + 1, this.tableY + 1, this.tableX + this.tableWidth - 1, this.tableY + TABLE_HEADER_HEIGHT,
                0xCC21384B);
        drawRect(this.tableX + 1, this.tableY + TABLE_HEADER_HEIGHT - 1, this.tableX + this.tableWidth - 1,
                this.tableY + TABLE_HEADER_HEIGHT, 0xAA6FCBFF);

        int headerY = this.tableY + 7;
        drawString(this.fontRenderer, I18n.format("gui.keybind.column.group"), groupX + 6, headerY, 0xFFEAF4FF);
        drawString(this.fontRenderer, I18n.format("gui.keybind.column.name"), nameX + 6, headerY, 0xFFEAF4FF);
        drawString(this.fontRenderer, I18n.format("gui.keybind.column.binding"), keyX + 6, headerY, 0xFFEAF4FF);
        drawString(this.fontRenderer, I18n.format("gui.keybind.column.detail"), detailX + 6, headerY, 0xFFEAF4FF);
        drawCenteredString(this.fontRenderer, I18n.format("gui.keybind.column.action"), actionX + actionWidth / 2, headerY,
                0xFFEAF4FF);

        if (this.filteredRows.isEmpty()) {
            GuiTheme.drawEmptyState(this.tableX + this.tableWidth / 2, this.tableY + this.tableHeight / 2,
                    I18n.format("gui.keybind.empty"), this.fontRenderer);
            return;
        }

        int visibleItems = getVisibleItems();
        for (int i = 0; i < visibleItems; i++) {
            int index = i + this.scrollOffset;
            if (index >= this.filteredRows.size()) {
                break;
            }

            RowEntry row = this.filteredRows.get(index);
            Keybind keybind = getRowKeybind(row);
            int rowY = this.listTop + i * ITEM_HEIGHT;
            int rowBottom = rowY + ITEM_HEIGHT - 1;
            boolean hoveredRow = isHoverRegion(mouseX, mouseY, rowLeft, rowY, rowRight - rowLeft, ITEM_HEIGHT - 1);
            int rowBg = (index % 2 == 0) ? 0x5520303E : 0x66304150;
            if (!row.actionRow) {
                rowBg = (index % 2 == 0) ? 0x5530343D : 0x6641454C;
            }

            drawRect(this.tableX + 1, rowY, this.tableX + this.tableWidth - 1, rowBottom, rowBg);
            if (hoveredRow) {
                drawRect(this.tableX + 1, rowY, this.tableX + this.tableWidth - 1, rowBottom, 0x3349B1E5);
                drawRect(this.tableX + 1, rowY, this.tableX + this.tableWidth - 1, rowY + 1, 0xAA79D4FF);
            } else {
                drawRect(this.tableX + 1, rowBottom - 1, this.tableX + this.tableWidth - 1, rowBottom, 0x55283B4D);
            }

            drawRect(nameX, rowY + 2, nameX + 1, rowBottom - 2, 0x553F5D73);
            drawRect(keyX, rowY + 2, keyX + 1, rowBottom - 2, 0x553F5D73);
            drawRect(detailX, rowY + 2, detailX + 1, rowBottom - 2, 0x553F5D73);
            drawRect(actionX, rowY + 2, actionX + 1, rowBottom - 2, 0x553F5D73);

            String groupLabel = row.getGroupLabel();
            int groupBadgeX = groupX + 6;
            int groupBadgeY = rowY + 6;
            int groupBadgeWidth = Math.min(groupWidth - 12, this.fontRenderer.getStringWidth(groupLabel) + 12);
            drawRect(groupBadgeX, groupBadgeY, groupBadgeX + groupBadgeWidth, groupBadgeY + 14, getGroupFillColor(row));
            drawRect(groupBadgeX, groupBadgeY, groupBadgeX + groupBadgeWidth, groupBadgeY + 1, getGroupBorderColor(row));
            drawCenteredString(this.fontRenderer, groupLabel, groupBadgeX + groupBadgeWidth / 2, groupBadgeY + 3,
                    getGroupBorderColor(row));

            String rowName = row.getName();
            String drawnName = trimToWidth(rowName, nameWidth - 12);
            drawString(this.fontRenderer, drawnName, nameX + 6, rowY + 10, 0xFFEAF4FF);
            setTooltipIfTrimmed(rowName, drawnName, mouseX, mouseY, nameX + 4, rowY + 4, nameWidth - 8, 18);

            String bindingText = getBindingText(row, keybind, false);
            String bindingTooltip = getBindingText(row, keybind, true);
            String drawnBinding = trimToWidth(bindingText, keyWidth - 12);
            int bindingColor = (keybind == null || keybind.getKeyCode() == Keyboard.KEY_NONE) ? GuiTheme.SUB_TEXT : 0xFFF6D27B;
            drawString(this.fontRenderer, drawnBinding, keyX + 6, rowY + 10, bindingColor);
            setTooltipIfTrimmed(bindingTooltip, drawnBinding, mouseX, mouseY, keyX + 4, rowY + 4, keyWidth - 8, 18);

            String detailText = getRowDetailText(row, keybind);
            String drawnDetail = trimToWidth(detailText, detailWidth - 12);
            drawString(this.fontRenderer, drawnDetail, detailX + 6, rowY + 10, 0xFFC0D2E2);
            setTooltipIfTrimmed(detailText, drawnDetail, mouseX, mouseY, detailX + 4, rowY + 4, detailWidth - 8, 18);

            int buttonWidth = actionWidth - 12;
            int buttonX = actionX + 6;
            int buttonY = rowY + 4;
            boolean hoveredButton = isHoverRegion(mouseX, mouseY, buttonX, buttonY, buttonWidth, ITEM_HEIGHT - 8);
            GuiTheme.drawButtonFrameSafe(buttonX, buttonY, buttonWidth, ITEM_HEIGHT - 8,
                    hoveredButton ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(this.fontRenderer, I18n.format("gui.keybind.change"), buttonX + buttonWidth / 2, buttonY + 5,
                    0xFFF4FAFF);

            if (hoveredButton && row.isPacketAction()) {
                this.hoveredTooltip = I18n.format("gui.keybind.packet_selector_hint");
            }
        }

        if (this.maxScroll > 0) {
            int thumbHeight = getScrollbarThumbHeight();
            int thumbY = this.listTop
                    + (int) ((this.scrollOffset / (float) Math.max(1, this.maxScroll)) * (this.listHeight - thumbHeight));
            GuiTheme.drawScrollbar(getScrollbarX(), this.listTop, 6, this.listHeight, thumbY, thumbHeight);
        }
    }

    private void drawFooterHint() {
        drawString(this.fontRenderer, I18n.format("gui.keybind.footer_hint"), this.panelX + 12, this.footerHintY,
                GuiTheme.SUB_TEXT);
    }

    private void openPacketSelector(final RowEntry row) {
        this.mc.setScreen(new GuiPacketSequenceSelector(this, sequenceName -> {
            Keybind keybind = KeybindManager.keybinds.computeIfAbsent(row.action, ignored -> new Keybind());
            keybind.setParameter(sequenceName);
            this.mc.setScreen(this);
        }));
    }

    private void openKeyRecorder(final RowEntry row) {
        this.mc.setScreen(new GuiKeybindRecorder(this, getRowKeybind(row), newKeybind -> {
            if (newKeybind != null && newKeybind.getKeyCode() != Keyboard.KEY_NONE) {
                if (row.actionRow) {
                    Keybind oldKeybind = KeybindManager.keybinds.get(row.action);
                    if (oldKeybind != null && oldKeybind.getParameter() != null) {
                        newKeybind.setParameter(oldKeybind.getParameter());
                    }
                    KeybindManager.keybinds.put(row.action, newKeybind);
                } else {
                    KeybindManager.pathSequenceKeybinds.put(row.sequenceName, newKeybind);
                }
            } else {
                if (row.actionRow) {
                    KeybindManager.keybinds.remove(row.action);
                } else {
                    KeybindManager.pathSequenceKeybinds.put(row.sequenceName, new Keybind());
                }
            }
            this.mc.setScreen(this);
        }));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BUTTON_SAVE) {
            KeybindManager.syncPathSequenceKeybinds();
            KeybindManager.saveConfig();
            this.mc.setScreen(this.parentScreen);
        } else if (button.id == BUTTON_BACK) {
            KeybindManager.loadConfig();
            this.mc.setScreen(this.parentScreen);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.searchField != null) {
            this.searchField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.hoveredTooltip = null;

        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, I18n.format("gui.keybind.title"), this.fontRenderer);

        drawString(this.fontRenderer, I18n.format("gui.keybind.search"), this.panelX + 12, this.searchY + 6,
                GuiTheme.LABEL_TEXT);
        drawThemedTextField(this.searchField);
        drawPlaceholderText();
        drawStatsBox();
        drawHelperBar();
        drawTable(mouseX, mouseY);
        drawFooterHint();

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.hoveredTooltip != null) {
            drawSimpleTooltip(this.hoveredTooltip, mouseX, mouseY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0 || this.maxScroll <= 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        boolean overList = isHoverRegion(mouseX, mouseY, this.tableX, this.listTop, this.tableWidth, this.listHeight)
                || isHoverRegion(mouseX, mouseY, getScrollbarX(), this.listTop, 6, this.listHeight);
        if (!overList) {
            return;
        }

        if (dWheel > 0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
        } else {
            this.scrollOffset = Math.min(this.maxScroll, this.scrollOffset + 1);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.searchField != null) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (this.maxScroll > 0 && isHoverRegion(mouseX, mouseY, getScrollbarX(), this.listTop, 6, this.listHeight)) {
            if (mouseButton == 0) {
                this.isDraggingScrollbar = true;
                updateScrollFromMouse(mouseY);
                return;
            }
        }

        int rowLeft = this.tableX + 6;
        int rowRight = this.tableX + this.tableWidth - 12;
        int availableWidth = rowRight - rowLeft;
        int groupWidth = Math.max(52, Math.min(76, availableWidth / 7));
        int actionWidth = Math.max(52, Math.min(64, availableWidth / 8));
        int keyWidth = Math.max(88, Math.min(150, availableWidth / 5));
        int detailWidth = Math.max(96, availableWidth / 4);
        int nameWidth = availableWidth - groupWidth - keyWidth - detailWidth - actionWidth;

        if (nameWidth < 120) {
            int shortage = 120 - nameWidth;

            int reducible = Math.max(0, detailWidth - 72);
            int reduction = Math.min(shortage, reducible);
            detailWidth -= reduction;
            shortage -= reduction;

            reducible = Math.max(0, keyWidth - 82);
            reduction = Math.min(shortage, reducible);
            keyWidth -= reduction;
            shortage -= reduction;

            reducible = Math.max(0, groupWidth - 48);
            reduction = Math.min(shortage, reducible);
            groupWidth -= reduction;

            nameWidth = availableWidth - groupWidth - keyWidth - detailWidth - actionWidth;
        }

        int actionX = rowLeft + groupWidth + nameWidth + keyWidth + detailWidth + 6;
        int actionButtonWidth = actionWidth - 12;
        int visibleItems = getVisibleItems();
        for (int i = 0; i < visibleItems; i++) {
            int index = i + this.scrollOffset;
            if (index >= this.filteredRows.size()) {
                break;
            }

            int rowY = this.listTop + i * ITEM_HEIGHT;
            if (!isHoverRegion(mouseX, mouseY, actionX, rowY + 4, actionButtonWidth, ITEM_HEIGHT - 8)) {
                continue;
            }

            RowEntry row = this.filteredRows.get(index);
            if (row.isPacketAction() && mouseButton == 1) {
                openPacketSelector(row);
            } else if (mouseButton == 0) {
                openKeyRecorder(row);
            }
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.isDraggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            this.isDraggingScrollbar = false;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField != null && this.searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.searchField.setFocused(false);
                return;
            }
            this.searchField.textboxKeyTyped(typedChar, keyCode);
            this.scrollOffset = 0;
            rebuildFilteredRows();
            refreshScrollBounds();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}







