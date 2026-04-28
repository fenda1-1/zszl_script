package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GuiCapturedIdSelector extends ThemedGuiScreen {

    private static final String CATEGORY_UNGROUPED = "未分组";

    private final GuiScreen parentScreen;
    private final Consumer<CapturedIdRuleManager.RuleCard> onSelect;

    private final List<String> categories = new ArrayList<>();
    private final List<CapturedIdRuleManager.RuleCard> allCards = new ArrayList<>();
    private final List<CapturedIdRuleManager.RuleCard> visibleCards = new ArrayList<>();

    private String selectedCategory = "";
    private int categoryScrollOffset = 0;
    private int cardScrollOffset = 0;
    private int maxCategoryScroll = 0;
    private int maxCardScroll = 0;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listY;
    private int listHeight;
    private int categoryListX;
    private int categoryListWidth;
    private int cardListX;
    private int cardListWidth;
    private int itemHeight;
    private int bottomButtonY;
    private int bottomButtonW;
    private int bottomButtonH;

    public GuiCapturedIdSelector(GuiScreen parentScreen, Consumer<CapturedIdRuleManager.RuleCard> onSelect) {
        this.parentScreen = parentScreen;
        this.onSelect = onSelect;
    }

    private float uiScale() {
        float sx = this.width / 420.0f;
        float sy = this.height / 280.0f;
        return Math.max(0.72f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    private void computeLayout() {
        panelWidth = Math.max(320, Math.min(s(400), this.width - s(20)));
        panelHeight = Math.max(220, Math.min(s(250), this.height - s(20)));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int pad = s(10);
        itemHeight = s(20);
        listY = panelY + s(30);
        bottomButtonH = s(20);
        bottomButtonW = s(150);
        bottomButtonY = panelY + panelHeight - bottomButtonH - s(10);
        listHeight = Math.max(s(100), bottomButtonY - listY - s(10));

        categoryListX = panelX + pad;
        categoryListWidth = Math.max(s(100), Math.min(s(130), panelWidth / 3));

        int gap = s(10);
        cardListX = categoryListX + categoryListWidth + gap;
        cardListWidth = panelX + panelWidth - pad - cardListX;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        computeLayout();
        reloadData();

        int centerX = this.width / 2;
        this.buttonList.add(new GuiButton(0, centerX - bottomButtonW / 2, bottomButtonY, bottomButtonW, bottomButtonH,
                I18n.format("gui.common.cancel")));
    }

    private void reloadData() {
        allCards.clear();
        allCards.addAll(CapturedIdRuleManager.getRuleCards());

        categories.clear();
        categories.addAll(CapturedIdRuleManager.getAllCategories());

        if (selectedCategory == null || selectedCategory.trim().isEmpty() || !categories.contains(selectedCategory)) {
            selectedCategory = categories.isEmpty() ? "" : categories.get(0);
        }
        filterCards();
    }

    private void filterCards() {
        visibleCards.clear();
        for (CapturedIdRuleManager.RuleCard card : allCards) {
            if (card == null || card.model == null || card.model.name == null || card.model.name.trim().isEmpty()) {
                continue;
            }
            if (selectedCategory.equalsIgnoreCase(getCardCategory(card))) {
                visibleCards.add(card);
            }
        }
        cardScrollOffset = 0;
    }

    private String getCardCategory(CapturedIdRuleManager.RuleCard card) {
        if (card == null || card.model == null || card.model.category == null || card.model.category.trim().isEmpty()) {
            return CATEGORY_UNGROUPED;
        }
        return card.model.category.trim();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = this.width / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawCenteredString(fontRenderer, "选择捕获ID", centerX, panelY + 10, 0xFFFFFF);

        drawCategoryList(mouseX, mouseY);
        drawCardList(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCategoryList(int mouseX, int mouseY) {
        int listX = this.categoryListX;
        int listWidth = this.categoryListWidth;

        drawRect(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);

        int visibleItems = Math.max(1, listHeight / itemHeight);
        maxCategoryScroll = Math.max(0, categories.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + categoryScrollOffset;
            if (index >= categories.size()) {
                break;
            }

            String category = categories.get(index);
            int itemY = listY + i * itemHeight;
            boolean isSelected = category.equals(selectedCategory);
            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;

            int bgColor = isSelected ? 0xFF0066AA : (isHovered ? 0xFF666666 : 0xFF444444);
            drawRect(listX + 1, itemY + 1, listX + listWidth - 1, itemY + itemHeight, bgColor);
            drawCenteredString(fontRenderer, trimToWidth(category, listWidth - 10), listX + listWidth / 2,
                    itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }
    }

    private void drawCardList(int mouseX, int mouseY) {
        int listX = this.cardListX;
        int listWidth = this.cardListWidth;

        drawRect(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);

        if (visibleCards.isEmpty()) {
            drawCenteredString(fontRenderer, "该分组暂无捕获ID", listX + listWidth / 2, listY + listHeight / 2 - 4,
                    0xFFBBBBBB);
            return;
        }

        int visibleItems = Math.max(1, listHeight / itemHeight);
        maxCardScroll = Math.max(0, visibleCards.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + cardScrollOffset;
            if (index >= visibleCards.size()) {
                break;
            }

            CapturedIdRuleManager.RuleCard card = visibleCards.get(index);
            int itemY = listY + i * itemHeight;
            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;

            int bgColor = isHovered ? 0xFF666666 : 0xFF444444;
            drawRect(listX + 1, itemY + 1, listX + listWidth - 1, itemY + itemHeight, bgColor);

            String displayName = card.model.displayName == null || card.model.displayName.trim().isEmpty()
                    ? card.model.name
                    : card.model.displayName;
            String value = card.capturedHex == null || card.capturedHex.trim().isEmpty()
                    ? "未捕获"
                    : card.capturedHex;
            String line = displayName + " | " + card.model.name + " | " + value;
            drawCenteredString(fontRenderer, trimToWidth(line, listWidth - 10), listX + listWidth / 2,
                    itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }
    }

    private String trimToWidth(String text, int width) {
        if (text == null) {
            return "";
        }
        String result = text;
        while (!result.isEmpty() && fontRenderer.getStringWidth(result) > width) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.equals(text) && result.length() >= 3) {
            result = result.substring(0, Math.max(0, result.length() - 3)) + "...";
        }
        return result;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        if (mouseX >= categoryListX && mouseX <= categoryListX + categoryListWidth && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + categoryScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < categories.size()) {
                selectedCategory = categories.get(clickedIndex);
                filterCards();
                return;
            }
        }

        if (mouseX >= cardListX && mouseX <= cardListX + cardListWidth && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + cardScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < visibleCards.size()) {
                onSelect.accept(visibleCards.get(clickedIndex));
            }
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

        if (mouseX >= categoryListX && mouseX <= categoryListX + categoryListWidth && mouseY >= listY
                && mouseY <= listY + listHeight) {
            if (dWheel > 0) {
                categoryScrollOffset = Math.max(0, categoryScrollOffset - 1);
            } else {
                categoryScrollOffset = Math.min(maxCategoryScroll, categoryScrollOffset + 1);
            }
            return;
        }

        if (mouseX >= cardListX && mouseX <= cardListX + cardListWidth && mouseY >= listY
                && mouseY <= listY + listHeight) {
            if (dWheel > 0) {
                cardScrollOffset = Math.max(0, cardScrollOffset - 1);
            } else {
                cardScrollOffset = Math.min(maxCardScroll, cardScrollOffset + 1);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}






