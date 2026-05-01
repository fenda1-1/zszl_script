package com.zszl.zszlScriptMod.otherfeatures.gui.item;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateCard;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateLayoutEntry;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.template.ExpressionTemplateCatalog;
import com.zszl.zszlScriptMod.path.InventoryItemFilterExpressionEngine;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GuiItemFilterExpressionEditor extends ThemedGuiScreen {
    private static final int SCROLL_STEP = 24;

    private final GuiScreen parentScreen;
    private final String title;
    private final String initialText;
    private final Consumer<String> callback;

    private GuiTextField searchField;
    private GuiTextField inputField;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int inputX;
    private int inputY;
    private int inputWidth;
    private int buttonY;
    private int confirmX;
    private int cancelX;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean draggingScrollbar = false;
    private String statusMessage = "";
    private int statusTicks = 0;

    public GuiItemFilterExpressionEditor(GuiScreen parentScreen, String title, String initialText,
            Consumer<String> callback) {
        this.parentScreen = parentScreen;
        this.title = safe(title).trim().isEmpty() ? "编辑物品过滤表达式" : title;
        this.initialText = safe(initialText);
        this.callback = callback;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        updateLayout();

        this.searchField = new GuiTextField(7101, this.fontRenderer, this.panelX + 12, this.panelY + 34,
                this.panelWidth - 24, 18);
        this.searchField.setMaxStringLength(80);
        this.searchField.setFocused(false);

        this.inputField = new GuiTextField(7102, this.fontRenderer, this.inputX, this.inputY, this.inputWidth, 18);
        this.inputField.setMaxStringLength(Integer.MAX_VALUE);
        this.inputField.setText(this.initialText);
        this.inputField.setFocused(true);

        this.scrollOffset = 0;
        this.maxScroll = 0;
        this.draggingScrollbar = false;
    }

    private void updateLayout() {
        this.panelWidth = Math.max(320, Math.min(this.width - 12, 660));
        this.panelHeight = Math.max(270, Math.min(this.height - 12, 430));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        int buttonWidth = 54;
        int buttonGap = 6;
        this.buttonY = this.panelY + this.panelHeight - 30;
        this.cancelX = this.panelX + this.panelWidth - 12 - buttonWidth;
        this.confirmX = this.cancelX - buttonGap - buttonWidth;
        this.inputX = this.panelX + 12;
        this.inputY = this.buttonY;
        this.inputWidth = Math.max(120, this.confirmX - this.inputX - 8);

        this.viewportX = this.panelX + 12;
        this.viewportY = this.panelY + 64;
        this.viewportWidth = this.panelWidth - 24;
        this.viewportHeight = Math.max(70, this.inputY - this.viewportY - 18);

        if (this.searchField != null) {
            this.searchField.x = this.panelX + 12;
            this.searchField.y = this.panelY + 34;
            this.searchField.width = this.panelWidth - 24;
            this.searchField.height = 18;
        }
        if (this.inputField != null) {
            this.inputField.x = this.inputX;
            this.inputField.y = this.inputY;
            this.inputField.width = this.inputWidth;
            this.inputField.height = 18;
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        if (this.searchField != null) {
            this.searchField.updateCursorCounter();
        }
        if (this.inputField != null) {
            this.inputField.updateCursorCounter();
        }
        if (this.statusTicks > 0) {
            this.statusTicks--;
            if (this.statusTicks == 0) {
                this.statusMessage = "";
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancel();
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            boolean searchFocused = this.searchField != null && this.searchField.isFocused();
            if (this.searchField != null) {
                this.searchField.setFocused(!searchFocused);
            }
            if (this.inputField != null) {
                this.inputField.setFocused(searchFocused);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            commit();
            return;
        }

        if (this.searchField != null && this.searchField.isFocused()) {
            if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
                this.scrollOffset = 0;
            }
            return;
        }
        if (this.inputField != null) {
            this.inputField.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && isInside(mouseX, mouseY, this.viewportX, this.viewportY,
                this.viewportWidth, this.viewportHeight)) {
            handleScroll(dWheel);
            return;
        }

        int button = Mouse.getEventButton();
        if (button == -1) {
            if (this.draggingScrollbar && Mouse.isButtonDown(0)) {
                updateScrollFromMouse(mouseY, computeContentHeight(buildLayoutEntries(getFilteredCards())));
            }
            return;
        }

        if (Mouse.getEventButtonState()) {
            handleMousePressed(mouseX, mouseY, button);
        } else {
            if (button == 0) {
                this.draggingScrollbar = false;
            }
            mouseReleased(mouseX, mouseY, button);
        }
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        updateLayout();
        List<ExpressionTemplateLayoutEntry> entries = buildLayoutEntries(getFilteredCards());
        int contentHeight = computeContentHeight(entries);
        updateScrollLimit(contentHeight);

        if (this.searchField != null) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (this.inputField != null) {
            this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton != 0) {
            return false;
        }

        if (isInside(mouseX, mouseY, this.confirmX, this.buttonY, getButtonWidth(), getButtonHeight())) {
            commit();
            return true;
        }
        if (isInside(mouseX, mouseY, this.cancelX, this.buttonY, getButtonWidth(), getButtonHeight())) {
            cancel();
            return true;
        }
        if (this.maxScroll > 0 && isInside(mouseX, mouseY, getScrollbarX(), getScrollbarY(),
                getScrollbarWidth(), getScrollbarHeight())) {
            this.draggingScrollbar = true;
            updateScrollFromMouse(mouseY, contentHeight);
            return true;
        }

        ExpressionTemplateLayoutEntry hoveredEntry = getHoveredEntry(entries, mouseX, mouseY);
        if (hoveredEntry != null && this.inputField != null) {
            this.inputField.setText(hoveredEntry.card.example);
            this.inputField.setFocused(true);
            if (this.searchField != null) {
                this.searchField.setFocused(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();
        drawDefaultBackground();

        List<ExpressionTemplateCard> cards = getFilteredCards();
        List<ExpressionTemplateLayoutEntry> entries = buildLayoutEntries(cards);
        int contentHeight = computeContentHeight(entries);
        updateScrollLimit(contentHeight);

        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "编辑表达式", this.fontRenderer);
        drawString(this.fontRenderer, this.title, this.panelX + 12, this.panelY + 18, 0xFFE8F2FB);

        GuiTheme.drawInputFrameSafe(this.searchField.x - 2, this.searchField.y - 2,
                this.searchField.width + 4, this.searchField.height + 4, this.searchField.isFocused(), true);
        drawThemedTextField(this.searchField);
        if (this.searchField.getText().trim().isEmpty() && !this.searchField.isFocused()) {
            drawString(this.fontRenderer, "§7搜索模板名称 / 示例 / 关键字",
                    this.searchField.x + 4, this.searchField.y + 6, 0xFF7F8FA4);
        }

        drawTemplateViewport(entries, mouseX, mouseY, contentHeight);

        drawString(this.fontRenderer, "最终表达式", this.inputX, this.inputY - 12, 0xFFDDDDDD);
        GuiTheme.drawInputFrameSafe(this.inputField.x - 2, this.inputField.y - 2,
                this.inputField.width + 4, this.inputField.height + 4, this.inputField.isFocused(), true);
        drawThemedTextField(this.inputField);

        drawActionButton("确定", this.confirmX, this.buttonY, getButtonWidth(), getButtonHeight(),
                isInside(mouseX, mouseY, this.confirmX, this.buttonY, getButtonWidth(), getButtonHeight()),
                GuiTheme.UiState.SUCCESS);
        drawActionButton("取消", this.cancelX, this.buttonY, getButtonWidth(), getButtonHeight(),
                isInside(mouseX, mouseY, this.cancelX, this.buttonY, getButtonWidth(), getButtonHeight()),
                GuiTheme.UiState.NORMAL);

        if (!this.statusMessage.isEmpty()) {
            drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(this.statusMessage,
                    this.panelWidth - 24), this.panelX + 12, this.panelY + 54, 0xFFFFB7B7);
        }

        ExpressionTemplateLayoutEntry hoveredEntry = getHoveredEntry(entries, mouseX, mouseY);
        if (hoveredEntry != null) {
            drawHoveringText(buildTooltip(hoveredEntry.card), mouseX, mouseY);
        }
    }

    private void drawTemplateViewport(List<ExpressionTemplateLayoutEntry> entries, int mouseX, int mouseY,
            int contentHeight) {
        GuiTheme.drawInputFrameSafe(this.viewportX, this.viewportY, this.viewportWidth, this.viewportHeight,
                false, true);
        Gui.drawRect(this.viewportX + 1, this.viewportY + 1, this.viewportX + this.viewportWidth - 1,
                this.viewportY + this.viewportHeight - 1, 0x33151E28);

        beginScissor(this.viewportX + 1, this.viewportY + 1, this.viewportWidth - 2, this.viewportHeight - 2);
        for (ExpressionTemplateLayoutEntry entry : entries) {
            int drawY = entry.y - this.scrollOffset;
            if (drawY + entry.height < this.viewportY + 1 || drawY > this.viewportY + this.viewportHeight - 1) {
                continue;
            }
            drawCard(entry, drawY, mouseX, mouseY);
        }
        endScissor();

        if (entries.isEmpty()) {
            GuiTheme.drawEmptyState(this.viewportX + this.viewportWidth / 2,
                    this.viewportY + this.viewportHeight / 2 - 4, "没有匹配的物品过滤表达式模板",
                    this.fontRenderer);
        } else if (this.maxScroll > 0) {
            GuiTheme.drawScrollbar(getScrollbarX(), getScrollbarY(), getScrollbarWidth(), getScrollbarHeight(),
                    getScrollbarThumbY(contentHeight), getScrollbarThumbHeight(contentHeight));
        }
    }

    private void drawCard(ExpressionTemplateLayoutEntry entry, int drawY, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, entry.x, drawY, entry.width, entry.height);
        int border = hovered ? 0xFF7AD9FF : 0xFF446278;
        int fill = hovered ? 0xCC1E3344 : 0xB31A2632;
        Gui.drawRect(entry.x - 1, drawY - 1, entry.x + entry.width + 1, drawY + entry.height + 1, border);
        Gui.drawRect(entry.x, drawY, entry.x + entry.width, drawY + entry.height, fill);
        Gui.drawRect(entry.x, drawY, entry.x + entry.width, drawY + 2, 0xFF56B6E8);
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(entry.card.name,
                Math.max(20, entry.width - 12)), entry.x + 6, drawY + 6, 0xFFFFFFFF);
        int lineY = drawY + 20;
        for (String line : entry.exampleLines) {
            drawString(this.fontRenderer, line, entry.x + 6, lineY, 0xFFD8E6F2);
            lineY += 10;
        }
    }

    private void drawActionButton(String text, int x, int y, int width, int height, boolean hovered,
            GuiTheme.UiState baseState) {
        GuiTheme.UiState state = hovered ? GuiTheme.UiState.HOVER : baseState;
        GuiTheme.drawButtonFrameSafe(x, y, width, height, state);
        int textX = x + (width - this.fontRenderer.getStringWidth(text)) / 2;
        int textY = y + (height - this.fontRenderer.FONT_HEIGHT) / 2 + 1;
        drawString(this.fontRenderer, text, textX, textY, GuiTheme.getStateTextColor(state));
    }

    private List<ExpressionTemplateCard> getFilteredCards() {
        List<ExpressionTemplateCard> allCards = ExpressionTemplateCatalog.buildItemFilterCards();
        String keyword = safe(this.searchField == null ? "" : this.searchField.getText()).trim()
                .toLowerCase(Locale.ROOT);
        if (keyword.isEmpty()) {
            return allCards;
        }
        List<ExpressionTemplateCard> filtered = new ArrayList<ExpressionTemplateCard>();
        for (ExpressionTemplateCard card : allCards) {
            if (matchesCard(card, keyword)) {
                filtered.add(card);
            }
        }
        return filtered;
    }

    private List<ExpressionTemplateLayoutEntry> buildLayoutEntries(List<ExpressionTemplateCard> cards) {
        List<ExpressionTemplateLayoutEntry> entries = new ArrayList<ExpressionTemplateLayoutEntry>();
        int contentWidth = getTemplateContentWidth();
        int gap = 8;
        int cols = contentWidth >= 540 ? 3 : (contentWidth >= 360 ? 2 : 1);
        int cardWidth = Math.max(120, (contentWidth - gap * (cols - 1) - 8) / cols);
        int currentY = this.viewportY + 4;
        int col = 0;
        int rowMaxHeight = 0;

        for (ExpressionTemplateCard card : cards) {
            List<String> exampleLines = wrapCardText(card.example, Math.max(60, cardWidth - 12), 2);
            int cardHeight = Math.max(54, 24 + exampleLines.size() * 10 + 10);
            int cardX = this.viewportX + 4 + col * (cardWidth + gap);
            entries.add(new ExpressionTemplateLayoutEntry(card, cardX, currentY, cardWidth, cardHeight, exampleLines));
            rowMaxHeight = Math.max(rowMaxHeight, cardHeight);
            col++;
            if (col >= cols) {
                col = 0;
                currentY += rowMaxHeight + gap;
                rowMaxHeight = 0;
            }
        }
        return entries;
    }

    private int computeContentHeight(List<ExpressionTemplateLayoutEntry> entries) {
        int maxBottom = 0;
        for (ExpressionTemplateLayoutEntry entry : entries) {
            maxBottom = Math.max(maxBottom, entry.y + entry.height);
        }
        return Math.max(0, maxBottom - this.viewportY);
    }

    private ExpressionTemplateLayoutEntry getHoveredEntry(List<ExpressionTemplateLayoutEntry> entries, int mouseX,
            int mouseY) {
        if (!isInside(mouseX, mouseY, this.viewportX, this.viewportY, this.viewportWidth, this.viewportHeight)) {
            return null;
        }
        for (ExpressionTemplateLayoutEntry entry : entries) {
            int drawY = entry.y - this.scrollOffset;
            if (isInside(mouseX, mouseY, entry.x, drawY, entry.width, entry.height)) {
                return new ExpressionTemplateLayoutEntry(entry.card, entry.x, drawY, entry.width, entry.height,
                        entry.exampleLines);
            }
        }
        return null;
    }

    private void commit() {
        String expression = safe(this.inputField == null ? "" : this.inputField.getText()).trim();
        if (expression.isEmpty()) {
            setError("表达式不能为空。");
            return;
        }
        try {
            InventoryItemFilterExpressionEngine.validate(expression);
        } catch (RuntimeException e) {
            setError("表达式无效: " + safeMessage(e));
            return;
        }

        net.minecraft.client.gui.screens.Screen screenBeforeCallback = this.mc.screen;
        if (this.callback != null) {
            this.callback.accept(expression);
        }
        if (this.mc.screen == null || this.mc.screen == screenBeforeCallback
                || this.mc.screen == this) {
            this.mc.setScreen(this.parentScreen);
        }
    }

    private void cancel() {
        this.mc.setScreen(this.parentScreen);
    }

    private void handleScroll(int dWheel) {
        if (this.maxScroll <= 0) {
            this.scrollOffset = 0;
            return;
        }
        if (dWheel > 0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - SCROLL_STEP);
        } else {
            this.scrollOffset = Math.min(this.maxScroll, this.scrollOffset + SCROLL_STEP);
        }
    }

    private void updateScrollLimit(int contentHeight) {
        this.maxScroll = Math.max(0, contentHeight - Math.max(1, this.viewportHeight - 8));
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
    }

    private int getTemplateContentWidth() {
        return Math.max(120, this.viewportWidth - 8 - getScrollbarGap() - getScrollbarWidth());
    }

    private int getScrollbarWidth() {
        return 8;
    }

    private int getScrollbarGap() {
        return 4;
    }

    private int getScrollbarX() {
        return this.viewportX + this.viewportWidth - getScrollbarWidth() - 2;
    }

    private int getScrollbarY() {
        return this.viewportY + 1;
    }

    private int getScrollbarHeight() {
        return Math.max(8, this.viewportHeight - 2);
    }

    private int getScrollbarThumbHeight(int contentHeight) {
        int scrollbarHeight = Math.max(1, getScrollbarHeight());
        return Math.max(18, (int) ((float) scrollbarHeight / Math.max(scrollbarHeight, contentHeight)
                * scrollbarHeight));
    }

    private int getScrollbarThumbY(int contentHeight) {
        int scrollbarY = getScrollbarY();
        int scrollbarHeight = getScrollbarHeight();
        int thumbHeight = getScrollbarThumbHeight(contentHeight);
        if (this.maxScroll <= 0) {
            return scrollbarY;
        }
        return scrollbarY + (int) ((float) this.scrollOffset / this.maxScroll
                * Math.max(1, scrollbarHeight - thumbHeight));
    }

    private void updateScrollFromMouse(int mouseY, int contentHeight) {
        if (this.maxScroll <= 0) {
            this.scrollOffset = 0;
            return;
        }
        int scrollbarY = getScrollbarY();
        int scrollbarHeight = getScrollbarHeight();
        int thumbHeight = getScrollbarThumbHeight(contentHeight);
        int trackHeight = scrollbarHeight - thumbHeight;
        if (trackHeight <= 0) {
            this.scrollOffset = 0;
            return;
        }
        float ratio = (float) (mouseY - scrollbarY - thumbHeight / 2) / trackHeight;
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll, (int) (ratio * this.maxScroll)));
    }

    private int getButtonWidth() {
        return 54;
    }

    private int getButtonHeight() {
        return 20;
    }

    private boolean matchesCard(ExpressionTemplateCard card, String keyword) {
        if (card == null) {
            return false;
        }
        if (safe(card.name).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(card.example).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(card.description).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(card.format).toLowerCase(Locale.ROOT).contains(keyword)
                || safe(card.outputExample).toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        for (String alias : card.keywords) {
            if (safe(alias).toLowerCase(Locale.ROOT).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildTooltip(ExpressionTemplateCard card) {
        List<String> lines = new ArrayList<String>();
        if (card == null) {
            return lines;
        }
        lines.add("§b" + card.name);
        lines.add("§7示例: §f" + card.example);
        lines.add("§7作用: §f" + card.description);
        lines.add("§7格式: §f" + card.format);
        lines.add("§7输出示例: §f" + card.outputExample);
        if (card.keywords.length > 0) {
            lines.add("§8关键字: " + String.join(" / ", card.keywords));
        }
        return lines;
    }

    private List<String> wrapCardText(String text, int width, int maxLines) {
        List<String> wrapped = this.fontRenderer.listFormattedStringToWidth(safe(text), Math.max(20, width));
        if (wrapped == null || wrapped.isEmpty()) {
            wrapped = new ArrayList<String>();
            wrapped.add("");
        }
        List<String> result = new ArrayList<String>();
        int limit = Math.max(1, maxLines);
        for (int i = 0; i < wrapped.size() && i < limit; i++) {
            String line = wrapped.get(i);
            if (i == limit - 1 && wrapped.size() > limit) {
                line = this.fontRenderer.trimStringToWidth(line, Math.max(20, width - 6)) + "...";
            }
            result.add(line);
        }
        return result;
    }

    private void beginScissor(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int scale = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + height)) * scale, width * scale, height * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void setError(String message) {
        this.statusMessage = message == null ? "" : "§c" + message;
        this.statusTicks = 180;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeMessage(RuntimeException e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "未知错误";
        }
        return e.getMessage().trim();
    }
}
