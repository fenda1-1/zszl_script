package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiDebugConfig extends ThemedGuiScreen {

    private static final int BTN_DONE = 101;
    private static final int SEARCH_FIELD_ID = 102;

    private static final int PANEL_MARGIN = 16;
    private static final int PANEL_TOP = 16;
    private static final int PANEL_MIN_WIDTH = 360;
    private static final int PANEL_MAX_WIDTH = 860;
    private static final int TITLE_BAR_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 68;
    private static final int FOOTER_HEIGHT = 40;
    private static final int CONTENT_PADDING = 14;
    private static final int SECTION_HEADER_HEIGHT = 28;
    private static final int SECTION_GAP = 12;
    private static final int CARD_HEIGHT = 86;
    private static final int CARD_GAP = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SEARCH_FIELD_HEIGHT = 18;
    private static final int MIN_TWO_COLUMN_WIDTH = 560;
    private static final String SEARCH_PLACEHOLDER = "搜索调试项、说明或模块名...";

    private static final int MODULE_ACCENT = 0xFF56CCF2;
    private static final int UTILITY_ACCENT = 0xFF7ED087;

    private final List<DebugSection> sections = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int footerY;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int visibleContentHeight;
    private int scrollOffset;
    private int maxScroll;
    private boolean draggingScrollbar;
    private GuiTextField searchField;
    private String searchQuery = "";
    private String hoveredTooltip;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        Keyboard.enableRepeatEvents(true);

        normalizeLegacyDebugState();
        updateLayout();
        initSearchField();
        rebuildSections();

        this.buttonList.add(new ThemedButton(BTN_DONE, 0, 0, 150, 20, I18n.format("gui.common.done")));
        layoutButtons();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button != null && button.id == BTN_DONE) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();
        layoutButtons();
        hoveredTooltip = null;

        this.drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.debug.title"), this.fontRenderer);

        drawHeaderArea();
        drawFooterArea();

        startScissor(contentX, contentY, contentWidth, visibleContentHeight);
        drawScrollableSections(mouseX, mouseY);
        endScissor();

        if (maxScroll > 0) {
            GuiTheme.drawScrollbar(getScrollbarX(), contentY, SCROLLBAR_WIDTH, visibleContentHeight,
                    getScrollbarHandleY(), getScrollbarHandleHeight());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (hoveredTooltip != null) {
            drawSimpleTooltip(hoveredTooltip, mouseX, mouseY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && maxScroll > 0) {
            int delta = wheel > 0 ? -24 : 24;
            scrollOffset = clamp(scrollOffset + delta, 0, maxScroll);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        if (maxScroll > 0 && isHoverRegion(mouseX, mouseY, getScrollbarX(), contentY, SCROLLBAR_WIDTH,
                visibleContentHeight)) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return;
        }

        if (!isHoverRegion(mouseX, mouseY, contentX, contentY, contentWidth, visibleContentHeight)) {
            return;
        }

        for (CardBounds bounds : buildCardBounds()) {
            if (isHoverRegion(mouseX, mouseY, bounds.x, bounds.y, bounds.width, bounds.height)) {
                toggleCard(bounds.card);
                rebuildSections();
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingScrollbar && clickedMouseButton == 0) {
            updateScrollFromMouse(mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen(null);
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

    private void normalizeLegacyDebugState() {
        boolean anyModuleEnabled = hasEnabledDebugModules();
        boolean allModuleEnabled = areAllDebugModulesEnabled();

        // 兼容旧界面的默认值：以前依赖总开关时，模块位默认全开但实际并不生效。
        if (!ModConfig.isDebugModeEnabled && allModuleEnabled) {
            for (DebugModule module : DebugModule.values()) {
                ModConfig.debugFlags.put(module, false);
            }
            anyModuleEnabled = false;
        }

        ModConfig.isDebugModeEnabled = anyModuleEnabled;
    }

    private void rebuildSections() {
        sections.clear();
        String normalizedQuery = normalizeSearchQuery(searchQuery);

        List<DebugCard> moduleCards = new ArrayList<>();
        for (DebugModule module : DebugModule.values()) {
            DebugCard card = new DebugCard(
                    module.getDisplayName(),
                    getModuleDescription(module),
                    getTooltipText(getModuleTooltipKey(module), getModuleDescription(module)),
                    "日志模块",
                    MODULE_ACCENT,
                    module,
                    null,
                    buildSearchText(module.getDisplayName(), getModuleDescription(module),
                            getTooltipText(getModuleTooltipKey(module), getModuleDescription(module)), "日志模块",
                            module == null ? "" : module.name(), ""));
            if (matchesSearch(card, normalizedQuery)) {
                moduleCards.add(card);
            }
        }
        if (!moduleCards.isEmpty()) {
            sections.add(new DebugSection("日志模块", "聊天框 / 日志输出类调试项", MODULE_ACCENT, moduleCards));
        }

        List<DebugCard> utilityCards = new ArrayList<>();
        for (ExtraToggle toggle : ExtraToggle.values()) {
            String title = getExtraTitle(toggle);
            String description = getExtraDescription(toggle);
            String tooltip = getTooltipText(getExtraTooltipKey(toggle), description);
            DebugCard card = new DebugCard(
                    getExtraTitle(toggle),
                    description,
                    tooltip,
                    "界面辅助",
                    UTILITY_ACCENT,
                    null,
                    toggle,
                    buildSearchText(title, description, tooltip, "界面辅助", "", toggle.name()));
            if (matchesSearch(card, normalizedQuery)) {
                utilityCards.add(card);
            }
        }
        if (!utilityCards.isEmpty()) {
            sections.add(new DebugSection("界面辅助", "显示、监听与开发辅助项", UTILITY_ACCENT, utilityCards));
        }
    }

    private void updateLayout() {
        panelWidth = Math.max(PANEL_MIN_WIDTH, Math.min(PANEL_MAX_WIDTH, this.width - PANEL_MARGIN * 2));
        panelHeight = Math.max(220, this.height - PANEL_TOP - PANEL_MARGIN);
        panelX = (this.width - panelWidth) / 2;
        panelY = PANEL_TOP;
        footerY = panelY + panelHeight - FOOTER_HEIGHT;
        contentX = panelX + CONTENT_PADDING;
        contentY = panelY + TITLE_BAR_HEIGHT + HEADER_HEIGHT;
        contentWidth = panelWidth - CONTENT_PADDING * 2 - SCROLLBAR_WIDTH - 8;
        visibleContentHeight = Math.max(48, footerY - contentY - CONTENT_PADDING);
        maxScroll = Math.max(0, getContentHeight() - visibleContentHeight);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        if (searchField != null) {
            searchField.x = contentX;
            searchField.y = panelY + TITLE_BAR_HEIGHT + 40;
            searchField.width = contentWidth;
            searchField.height = SEARCH_FIELD_HEIGHT;
        }
    }

    private void layoutButtons() {
        for (GuiButton button : this.buttonList) {
            if (button.id == BTN_DONE) {
                button.width = Math.min(160, panelWidth - CONTENT_PADDING * 2);
                button.height = 20;
                button.x = panelX + (panelWidth - button.width) / 2;
                button.y = footerY + 10;
            }
        }
    }

    private void drawHeaderArea() {
        int textX = contentX;
        int textY = panelY + TITLE_BAR_HEIGHT + 10;
        int totalEnabled = getEnabledCardCount();
        int totalCards = getTotalCardCount();
        int enabledModules = getEnabledModuleCount();
        int allCards = getAvailableCardCount();
        boolean searchActive = !normalizeSearchQuery(searchQuery).isEmpty();

        drawText("点击卡片即可开关，不再显示调试总开关。", textX, textY, GuiTheme.LABEL_TEXT);

        String statusLine;
        if (searchActive) {
            statusLine = "当前搜索匹配 " + totalCards + " 项，已开启 " + totalEnabled + " 项，支持中文、拼音和英文过滤。";
        } else {
            statusLine = enabledModules > 0
                    ? "当前有 " + enabledModules + " 个日志模块正在生效，界面辅助项可单独启用。"
                    : "当前没有启用日志模块，界面辅助项仍可独立工作。";
        }
        drawText(statusLine, textX, textY + 14, GuiTheme.SUB_TEXT);

        String primaryChip = searchActive ? "匹配 " + totalCards : "已开启 " + totalEnabled;
        String secondaryChip = searchActive ? "全部 " + allCards : "总项 " + totalCards;
        int primaryWidth = getChipWidth(primaryChip);
        int secondaryWidth = getChipWidth(secondaryChip);
        int chipsX = panelX + panelWidth - CONTENT_PADDING - primaryWidth - secondaryWidth - 8;
        int chipY = textY + 2;

        if (chipsX >= textX + 190) {
            drawHeaderChip(chipsX, chipY, primaryChip,
                    searchActive ? UTILITY_ACCENT : (totalEnabled > 0 ? MODULE_ACCENT : GuiTheme.STATE_DISABLED),
                    searchActive || totalEnabled > 0);
            drawHeaderChip(chipsX + primaryWidth + 8, chipY, secondaryChip, MODULE_ACCENT, false);
        } else {
            drawHeaderChip(panelX + panelWidth - CONTENT_PADDING - primaryWidth, chipY, primaryChip,
                    searchActive ? UTILITY_ACCENT : (totalEnabled > 0 ? MODULE_ACCENT : GuiTheme.STATE_DISABLED),
                    searchActive || totalEnabled > 0);
        }

        drawThemedTextField(searchField);
        if (searchField != null && normalizeSearchQuery(searchField.getText()).isEmpty() && !searchField.isFocused()) {
            drawText(SEARCH_PLACEHOLDER, searchField.x + 6,
                    searchField.y + (searchField.height - this.fontRenderer.FONT_HEIGHT) / 2, 0xFF8A96A8);
        }
    }

    private void drawFooterArea() {
        Gui.drawRect(panelX + CONTENT_PADDING, footerY, panelX + panelWidth - CONTENT_PADDING, footerY + 1, 0x4454697D);
    }

    private void drawScrollableSections(int mouseX, int mouseY) {
        if (sections.isEmpty()) {
            GuiTheme.drawEmptyState(contentX + contentWidth / 2, contentY + visibleContentHeight / 2 - 6,
                    "没有匹配的调试项", this.fontRenderer);
            return;
        }

        int currentY = contentY - scrollOffset;
        int columns = getColumnCount();
        int cardWidth = getCardWidth(columns);

        for (DebugSection section : sections) {
            drawSectionHeader(section, currentY);
            currentY += SECTION_HEADER_HEIGHT;

            for (int i = 0; i < section.cards.size(); i++) {
                int row = i / columns;
                int col = i % columns;
                int cardX = contentX + col * (cardWidth + CARD_GAP);
                int cardY = currentY + row * (CARD_HEIGHT + CARD_GAP);

                if (cardY + CARD_HEIGHT < contentY - 2 || cardY > contentY + visibleContentHeight + 2) {
                    continue;
                }

                DebugCard card = section.cards.get(i);
                boolean hovered = isHoverRegion(mouseX, mouseY, cardX, cardY, cardWidth, CARD_HEIGHT);
                drawCard(card, cardX, cardY, cardWidth, CARD_HEIGHT, hovered);

                if (hovered) {
                    hoveredTooltip = card.tooltip;
                }
            }

            int rows = getRowCount(section.cards.size(), columns);
            if (rows > 0) {
                currentY += rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
            }
            currentY += SECTION_GAP;
        }
    }

    private void drawSectionHeader(DebugSection section, int y) {
        int titleX = contentX + 2;
        int titleY = y + 5;
        int countWidth = getChipWidth(section.cards.size() + " 项");
        int countX = contentX + contentWidth - countWidth;
        int summaryX = titleX + this.fontRenderer.getStringWidth(section.title) + 10;

        drawText(section.title, titleX, titleY, GuiTheme.LABEL_TEXT);

        if (summaryX + this.fontRenderer.getStringWidth(section.summary) < countX - 8) {
            drawText(section.summary, summaryX, titleY + 1, GuiTheme.SUB_TEXT);
        }

        drawHeaderChip(countX, y + 3, section.cards.size() + " 项", section.accentColor, false);
        Gui.drawRect(contentX, y + SECTION_HEADER_HEIGHT - 2, contentX + contentWidth, y + SECTION_HEADER_HEIGHT - 1,
                0x334B6075);
        Gui.drawRect(contentX, y + SECTION_HEADER_HEIGHT - 2, contentX + 54, y + SECTION_HEADER_HEIGHT - 1,
                section.accentColor);
    }

    private void drawCard(DebugCard card, int x, int y, int width, int height, boolean hovered) {
        boolean enabled = isCardEnabled(card);
        GuiTheme.UiState state = enabled ? GuiTheme.UiState.SUCCESS
                : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);

        GuiTheme.drawButtonFrameSafe(x, y, width, height, state);
        Gui.drawRect(x, y, x + width, y + 22, enabled ? 0x5531B86B : hovered ? 0x334A86B0 : 0x22384A5D);
        Gui.drawRect(x, y, x + 4, y + height, enabled ? card.accentColor : 0xAA607488);
        Gui.drawRect(x + 10, y + height - 18, x + width - 10, y + height - 17, 0x22485C70);
        GuiTheme.drawCardHighlight(x, y, width, height, hovered);

        String statusText = enabled ? "已开启" : "未开启";
        int pillWidth = Math.max(50, this.fontRenderer.getStringWidth(statusText) + 18);
        int pillX = x + width - pillWidth - 10;
        int pillY = y + 7;
        GuiTheme.drawButtonFrameSafe(pillX, pillY, pillWidth, 16,
                enabled ? GuiTheme.UiState.SUCCESS : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
        drawCenteredText(statusText, pillX + pillWidth / 2, pillY + 4,
                enabled ? GuiTheme.getStateTextColor(GuiTheme.UiState.SUCCESS) : GuiTheme.SUB_TEXT);

        String title = this.fontRenderer.trimStringToWidth(card.title, Math.max(80, pillX - x - 24));
        drawText(title, x + 12, y + 9, GuiTheme.LABEL_TEXT);

        int descWidth = Math.max(120, width - 24);
        List<String> lines = getLimitedWrappedLines(card.description, descWidth, 3);
        for (int i = 0; i < lines.size(); i++) {
            drawText(lines.get(i), x + 12, y + 29 + i * 11, GuiTheme.SUB_TEXT);
        }

        drawText(card.badge, x + 12, y + height - 12, card.accentColor);
        String clickHint = enabled ? "点击关闭" : "点击开启";
        drawText(clickHint, x + width - 12 - this.fontRenderer.getStringWidth(clickHint), y + height - 12,
                GuiTheme.SUB_TEXT);
    }

    private List<CardBounds> buildCardBounds() {
        List<CardBounds> bounds = new ArrayList<>();
        int currentY = contentY - scrollOffset;
        int columns = getColumnCount();
        int cardWidth = getCardWidth(columns);

        for (DebugSection section : sections) {
            currentY += SECTION_HEADER_HEIGHT;
            for (int i = 0; i < section.cards.size(); i++) {
                int row = i / columns;
                int col = i % columns;
                int x = contentX + col * (cardWidth + CARD_GAP);
                int y = currentY + row * (CARD_HEIGHT + CARD_GAP);
                bounds.add(new CardBounds(section.cards.get(i), x, y, cardWidth, CARD_HEIGHT));
            }

            int rows = getRowCount(section.cards.size(), columns);
            if (rows > 0) {
                currentY += rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
            }
            currentY += SECTION_GAP;
        }
        return bounds;
    }

    private void toggleCard(DebugCard card) {
        if (card.module != null) {
            boolean next = !ModConfig.debugFlags.getOrDefault(card.module, false);
            ModConfig.debugFlags.put(card.module, next);
            ModConfig.isDebugModeEnabled = hasEnabledDebugModules();
            return;
        }

        if (card.extra == null) {
            return;
        }

        switch (card.extra) {
            case SHOW_MOUSE_COORDINATES:
                ModConfig.showMouseCoordinates = !ModConfig.showMouseCoordinates;
                break;
            case AHK_MOVE_MOUSE_MODE:
                ModConfig.ahkMoveMouseMode = !ModConfig.ahkMoveMouseMode;
                break;
            case SHOW_HOVER_INFO:
                ModConfig.showHoverInfo = !ModConfig.showHoverInfo;
                break;
            case ENABLE_GUI_LISTENER:
                ModConfig.enableGuiListener = !ModConfig.enableGuiListener;
                break;
            case ENABLE_GHOST_ITEM_COPY:
                ModConfig.enableGhostItemCopy = !ModConfig.enableGhostItemCopy;
                break;
        }
    }

    private boolean isCardEnabled(DebugCard card) {
        if (card.module != null) {
            return ModConfig.debugFlags.getOrDefault(card.module, false);
        }
        if (card.extra == null) {
            return false;
        }
        switch (card.extra) {
            case SHOW_MOUSE_COORDINATES:
                return ModConfig.showMouseCoordinates;
            case AHK_MOVE_MOUSE_MODE:
                return ModConfig.ahkMoveMouseMode;
            case SHOW_HOVER_INFO:
                return ModConfig.showHoverInfo;
            case ENABLE_GUI_LISTENER:
                return ModConfig.enableGuiListener;
            case ENABLE_GHOST_ITEM_COPY:
                return ModConfig.enableGhostItemCopy;
            default:
                return false;
        }
    }

    private boolean hasEnabledDebugModules() {
        for (DebugModule module : DebugModule.values()) {
            if (ModConfig.debugFlags.getOrDefault(module, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean areAllDebugModulesEnabled() {
        for (DebugModule module : DebugModule.values()) {
            if (!ModConfig.debugFlags.getOrDefault(module, false)) {
                return false;
            }
        }
        return true;
    }

    private int getEnabledModuleCount() {
        int count = 0;
        for (DebugModule module : DebugModule.values()) {
            if (ModConfig.debugFlags.getOrDefault(module, false)) {
                count++;
            }
        }
        return count;
    }

    private int getEnabledCardCount() {
        int count = 0;
        for (DebugSection section : sections) {
            for (DebugCard card : section.cards) {
                if (isCardEnabled(card)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int getTotalCardCount() {
        int count = 0;
        for (DebugSection section : sections) {
            count += section.cards.size();
        }
        return count;
    }

    private int getAvailableCardCount() {
        return DebugModule.values().length + ExtraToggle.values().length;
    }

    private int getContentHeight() {
        int height = 0;
        int columns = getColumnCount();
        for (DebugSection section : sections) {
            height += SECTION_HEADER_HEIGHT;
            int rows = getRowCount(section.cards.size(), columns);
            if (rows > 0) {
                height += rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
            }
            height += SECTION_GAP;
        }
        return height;
    }

    private int getColumnCount() {
        return contentWidth >= MIN_TWO_COLUMN_WIDTH ? 2 : 1;
    }

    private int getCardWidth(int columns) {
        return (contentWidth - Math.max(0, columns - 1) * CARD_GAP) / columns;
    }

    private int getRowCount(int itemCount, int columns) {
        if (itemCount <= 0) {
            return 0;
        }
        return (itemCount + columns - 1) / columns;
    }

    private int getScrollbarX() {
        return panelX + panelWidth - CONTENT_PADDING - SCROLLBAR_WIDTH;
    }

    private int getScrollbarHandleHeight() {
        if (maxScroll <= 0) {
            return visibleContentHeight;
        }
        int totalHeight = Math.max(visibleContentHeight, getContentHeight());
        return Math.max(20, Math.round((visibleContentHeight / (float) totalHeight) * visibleContentHeight));
    }

    private int getScrollbarHandleY() {
        if (maxScroll <= 0) {
            return contentY;
        }
        int travel = visibleContentHeight - getScrollbarHandleHeight();
        return contentY + Math.round((scrollOffset / (float) maxScroll) * travel);
    }

    private void updateScrollFromMouse(int mouseY) {
        int handleHeight = getScrollbarHandleHeight();
        int travel = Math.max(1, visibleContentHeight - handleHeight);
        int target = clamp(mouseY - contentY - handleHeight / 2, 0, travel);
        scrollOffset = clamp(Math.round((target / (float) travel) * maxScroll), 0, maxScroll);
    }

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(this.mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + height)) * scale, width * scale, height * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawText(String text, int x, int y, int color) {
        this.fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    private void drawCenteredText(String text, int centerX, int y, int color) {
        this.fontRenderer.drawStringWithShadow(text, centerX - this.fontRenderer.getStringWidth(text) / 2, y, color);
    }

    private void drawHeaderChip(int x, int y, String text, int accentColor, boolean active) {
        int width = getChipWidth(text);
        int border = active ? accentColor : 0xAA4C6279;
        int fill = active ? 0xCC1C3347 : 0xAA17222D;

        Gui.drawRect(x - 1, y - 1, x + width + 1, y + 17, border);
        Gui.drawRect(x, y, x + width, y + 16, fill);
        Gui.drawRect(x, y, x + 3, y + 16, accentColor);
        drawText(text, x + 8, y + 4, active ? GuiTheme.LABEL_TEXT : GuiTheme.SUB_TEXT);
    }

    private int getChipWidth(String text) {
        return this.fontRenderer.getStringWidth(text) + 18;
    }

    private void initSearchField() {
        if (searchField == null) {
            searchField = new GuiTextField(SEARCH_FIELD_ID, this.fontRenderer, contentX, panelY + TITLE_BAR_HEIGHT + 40,
                    contentWidth, SEARCH_FIELD_HEIGHT);
            searchField.setMaxStringLength(128);
        } else {
            searchField.x = contentX;
            searchField.y = panelY + TITLE_BAR_HEIGHT + 40;
            searchField.width = contentWidth;
            searchField.height = SEARCH_FIELD_HEIGHT;
        }
        searchField.setText(searchQuery == null ? "" : searchQuery);
        searchField.setFocused(false);
    }

    private void updateSearchQueryFromField() {
        String newQuery = searchField == null ? "" : searchField.getText().trim();
        if (!newQuery.equals(searchQuery)) {
            searchQuery = newQuery;
            scrollOffset = 0;
            rebuildSections();
            updateLayout();
        } else {
            searchQuery = newQuery;
        }
    }

    private List<String> getLimitedWrappedLines(String text, int width, int maxLines) {
        List<String> wrapped = this.fontRenderer.listFormattedStringToWidth(text, Math.max(60, width));
        if (wrapped.size() <= maxLines) {
            return wrapped;
        }

        List<String> limited = new ArrayList<>();
        for (int i = 0; i < maxLines; i++) {
            limited.add(wrapped.get(i));
        }

        String ellipsis = "...";
        int lastIndex = maxLines - 1;
        String lastLine = limited.get(lastIndex);
        String trimmed = this.fontRenderer.trimStringToWidth(lastLine, Math.max(24, width - this.fontRenderer.getStringWidth(ellipsis)));
        limited.set(lastIndex, trimmed + ellipsis);
        return limited;
    }

    private String getTooltipText(String baseKey, String fallback) {
        if (baseKey == null) {
            return fallback;
        }

        List<String> lines = new ArrayList<>();
        addTranslatedLine(lines, baseKey);
        for (int i = 1; i <= 4; i++) {
            String line = translateOrNull(baseKey + "." + i);
            if (line == null) {
                break;
            }
            lines.add(line);
        }

        if (lines.isEmpty()) {
            return fallback;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private void addTranslatedLine(List<String> lines, String key) {
        String translated = translateOrNull(key);
        if (translated != null) {
            lines.add(translated);
        }
    }

    private String translateOrNull(String key) {
        String translated = I18n.format(key);
        return key.equals(translated) ? null : translated;
    }

    private String getExtraTitle(ExtraToggle toggle) {
        switch (toggle) {
            case SHOW_MOUSE_COORDINATES:
                return I18n.format("gui.debug.show_mouse");
            case AHK_MOVE_MOUSE_MODE:
                return I18n.format("gui.debug.ahk_mouse");
            case SHOW_HOVER_INFO:
                return I18n.format("gui.debug.show_nbt");
            case ENABLE_GUI_LISTENER:
                return I18n.format("gui.debug.gui_listener");
            case ENABLE_GHOST_ITEM_COPY:
                return I18n.format("gui.debug.ghost_copy");
            default:
                return toggle.name();
        }
    }

    private String getExtraDescription(ExtraToggle toggle) {
        switch (toggle) {
            case SHOW_MOUSE_COORDINATES:
                return "在无界面时显示原始鼠标坐标与缩放后的屏幕坐标。";
            case AHK_MOVE_MOUSE_MODE:
                return "切换调用 AHK 点击时是否真实移动系统鼠标。";
            case SHOW_HOVER_INFO:
                return "在 GUI 中悬停时展示更详细的物品与控件调试信息。";
            case ENABLE_GUI_LISTENER:
                return "监听当前打开的游戏界面，并输出对应 GUI 的类名变化。";
            case ENABLE_GHOST_ITEM_COPY:
                return "允许在支持场景中复制本地幽灵物品用于预览与调试。";
            default:
                return "界面辅助调试项。";
        }
    }

    private String getExtraTooltipKey(ExtraToggle toggle) {
        switch (toggle) {
            case SHOW_MOUSE_COORDINATES:
                return "gui.debug.tip.show_mouse";
            case AHK_MOVE_MOUSE_MODE:
                return "gui.debug.tip.ahk_mouse";
            case SHOW_HOVER_INFO:
                return "gui.debug.tip.show_nbt";
            case ENABLE_GUI_LISTENER:
                return "gui.debug.tip.gui_listener";
            case ENABLE_GHOST_ITEM_COPY:
                return "gui.debug.tip.ghost_copy";
            default:
                return null;
        }
    }

    private String getModuleTooltipKey(DebugModule module) {
        switch (module) {
            case PATH_SEQUENCE:
                return "gui.debug.tip.path";
            case EVACUATION:
                return "gui.debug.tip.evac";
            case AUTO_EAT:
                return "gui.debug.tip.eat";
            case AUTO_SKILL:
                return "gui.debug.tip.skill";
            case ITEM_FILTER:
                return "gui.debug.tip.filter";
            case AHK_EXECUTION:
                return "gui.debug.tip.ahk";
            case ARENA_HANDLER:
                return "gui.debug.tip.arena";
            case CHEST_ANALYSIS:
                return "gui.debug.tip.chest";
            case KILL_TIMER:
                return "gui.debug.tip.killtimer";
            case WAREHOUSE_ANALYSIS:
                return "gui.debug.tip.warehouse";
            case AUTO_PICKUP:
                return "gui.debug.tip.autopickup";
            case SHULKER_STACKING:
                return "gui.debug.tip.shulker";
            case AUTO_EQUIP:
                return "gui.debug.tip.autoequip";
            case TRIGGER_RULES:
                return "gui.debug.tip.trigger";
            default:
                return null;
        }
    }

    private String getModuleDescription(DebugModule module) {
        switch (module) {
            case PATH_SEQUENCE:
                return "输出脚本路径序列的执行步骤、前往逻辑与动作链。";
            case EVACUATION:
                return "记录撤离状态机切换、条件判断与触发原因。";
            case AUTO_EAT:
                return "查看自动进食的触发时机、食物选择与过程日志。";
            case AUTO_SKILL:
                return "跟踪技能冷却、施放选择和按键模拟过程。";
            case ITEM_FILTER:
                return "查看旧版物品过滤保留与丢弃的判断细节。";
            case AHK_EXECUTION:
                return "记录每次 AHK 脚本调用的命令和执行情况。";
            case ARENA_HANDLER:
                return "输出竞技场处理中的物品决策和状态变化。";
            case CHEST_ANALYSIS:
                return "打开箱子时记录标题、槽位数据和识别结果。";
            case KILL_TIMER:
                return "记录攻击事件、击杀判定与计时器变化。";
            case WAREHOUSE_ANALYSIS:
                return "输出仓库识别、坐标匹配与扫描统计信息。";
            case CONDITIONAL_EXECUTION:
                return "记录条件执行规则的命中过程、变量与最终结果。";
            case AUTO_PICKUP:
                return "查看自动拾取的目标选择、状态流转和失败原因。";
            case SHULKER_STACKING:
                return "跟踪潜影盒叠加、搬运流程与关键数据包细节。";
            case REFINE:
                return "输出精炼配置命中、材料检查和执行步骤。";
            case AUTO_EQUIP:
                return "记录装备比较、刷新判断与自动穿戴流程。";
            case TRIGGER_RULES:
                return "输出触发器事件到达、规则匹配过程、冷却与执行结果。";
            case MAIL_GUI:
                return "跟踪邮件 GUI 的识别结果、控件状态与交互信息。";
            case PASSWORD_MANAGER:
                return "输出密码管理的匹配、填充与保存调试日志。";
            case BARITONE:
                return "查看 Baritone 指令派发、路径反馈与状态切换。";
            case KILL_AURA_ORBIT:
                return "记录杀戮绕圈的性能摘要、重建原因与接管状态。";
            case KILL_AURA_ORBIT_TRACE:
                return "输出杀戮绕圈的 nodePlan、arcPlan 与 routeTraverse 深度跟踪日志。";
            default:
                return "该模块会输出对应功能的详细调试信息。";
        }
    }

    private String normalizeSearchQuery(String value) {
        return PinyinSearchHelper.normalizeQuery(value);
    }

    private boolean matchesSearch(DebugCard card, String normalizedQuery) {
        return normalizedQuery.isEmpty()
                || (card != null && PinyinSearchHelper.matchesNormalized(card.searchText, normalizedQuery));
    }

    private String buildSearchText(String title, String description, String tooltip, String badge, String moduleName,
            String extraName) {
        StringBuilder builder = new StringBuilder();
        appendSearchPart(builder, title);
        appendSearchPart(builder, description);
        appendSearchPart(builder, tooltip);
        appendSearchPart(builder, badge);
        appendSearchPart(builder, moduleName);
        appendSearchPart(builder, extraName);
        return builder.toString();
    }

    private void appendSearchPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ExtraToggle {
        SHOW_MOUSE_COORDINATES,
        AHK_MOVE_MOUSE_MODE,
        SHOW_HOVER_INFO,
        ENABLE_GUI_LISTENER,
        ENABLE_GHOST_ITEM_COPY
    }

    private static class DebugSection {
        private final String title;
        private final String summary;
        private final int accentColor;
        private final List<DebugCard> cards;

        private DebugSection(String title, String summary, int accentColor, List<DebugCard> cards) {
            this.title = title;
            this.summary = summary;
            this.accentColor = accentColor;
            this.cards = cards;
        }
    }

    private static class DebugCard {
        private final String title;
        private final String description;
        private final String tooltip;
        private final String badge;
        private final int accentColor;
        private final DebugModule module;
        private final ExtraToggle extra;
        private final String searchText;

        private DebugCard(String title, String description, String tooltip, String badge, int accentColor,
                DebugModule module, ExtraToggle extra, String searchText) {
            this.title = title;
            this.description = description;
            this.tooltip = tooltip;
            this.badge = badge;
            this.accentColor = accentColor;
            this.module = module;
            this.extra = extra;
            this.searchText = searchText == null ? "" : searchText;
        }
    }

    private static class CardBounds {
        private final DebugCard card;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private CardBounds(DebugCard card, int x, int y, int width, int height) {
            this.card = card;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
