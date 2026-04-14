package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionEditorBinding;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateCard;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ExpressionTemplateLayoutEntry;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.template.ExpressionTemplateCatalog;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class ExpressionPopupSupport {
    private ExpressionPopupSupport() {
    }

    static void openBinding(GuiActionEditor editor, ExpressionEditorBinding binding) {
        if (binding == null) {
            return;
        }
        GuiTextField targetField = editor.getFieldByKey(binding.paramKey);
        open(editor, editor.safe(targetField == null ? "" : targetField.getText()),
                binding.title.isEmpty() ? "表达式" : binding.title,
                GuiActionEditor.EXPRESSION_TEMPLATE_MODE_SET_VAR);
        editor.activeExpressionEditorBinding = binding;
    }

    static void openBoolean(GuiActionEditor editor, int editIndex) {
        List<String> expressions = editor.getBooleanExpressionList();
        String initialValue = "";
        if (editIndex >= 0 && editIndex < expressions.size()) {
            initialValue = expressions.get(editIndex);
        }
        open(editor, initialValue,
                editIndex == GuiActionEditor.BOOLEAN_EXPRESSION_EDIT_NEW
                        ? net.minecraft.client.resources.I18n
                                .format("gui.path.action_editor.popup.add_boolean_expression")
                        : net.minecraft.client.resources.I18n
                                .format("gui.path.action_editor.popup.edit_boolean_expression"),
                GuiActionEditor.EXPRESSION_TEMPLATE_MODE_BOOLEAN);
        editor.activeBooleanExpressionEditIndex = editIndex;
    }

    static void openItemFilter(GuiActionEditor editor, int editIndex) {
        List<String> expressions = editor.getInventoryItemFilterExpressionList();
        String initialValue = "";
        if (editIndex >= 0 && editIndex < expressions.size()) {
            initialValue = expressions.get(editIndex);
        }
        open(editor, initialValue,
                editIndex == GuiActionEditor.ITEM_FILTER_EXPRESSION_EDIT_NEW
                        ? net.minecraft.client.resources.I18n
                                .format("gui.path.action_editor.popup.add_item_filter_expression")
                        : net.minecraft.client.resources.I18n
                                .format("gui.path.action_editor.popup.edit_item_filter_expression"),
                GuiActionEditor.EXPRESSION_TEMPLATE_MODE_ITEM_FILTER);
        editor.activeInventoryItemFilterExpressionEditIndex = editIndex;
    }

    static void open(GuiActionEditor editor, String initialValue, String title, String templateMode) {
        editor.collapseAllDropdowns();
        if (editor.actionSearchField != null) {
            editor.actionSearchField.setFocused(false);
        }
        for (GuiTextField field : editor.paramFields) {
            field.setFocused(false);
        }
        editor.activeExpressionEditorBinding = null;
        editor.activeBooleanExpressionEditIndex = GuiActionEditor.BOOLEAN_EXPRESSION_EDIT_NONE;
        editor.activeInventoryItemFilterExpressionEditIndex = GuiActionEditor.ITEM_FILTER_EXPRESSION_EDIT_NONE;
        editor.activeExpressionPopupTitle = title == null ? "" : title;
        editor.activeExpressionPopupTemplateMode = templateMode == null
                ? GuiActionEditor.EXPRESSION_TEMPLATE_MODE_SET_VAR
                : templateMode;
        editor.activeExpressionPopupBooleanOnly = GuiActionEditor.EXPRESSION_TEMPLATE_MODE_BOOLEAN
                .equals(editor.activeExpressionPopupTemplateMode);
        editor.expressionPopupSearchField = new GuiTextField(7001, editor.getEditorFontRenderer(), 0, 0, 120, 18);
        editor.expressionPopupSearchField.setMaxStringLength(80);
        editor.expressionPopupSearchField.setFocused(false);
        editor.expressionPopupInputField = new GuiTextField(7002, editor.getEditorFontRenderer(), 0, 0, 120, 18);
        editor.expressionPopupInputField.setMaxStringLength(Integer.MAX_VALUE);
        editor.expressionPopupOriginalValue = editor.safe(initialValue);
        editor.expressionPopupInputField.setText(editor.expressionPopupOriginalValue);
        editor.expressionPopupInputField.setFocused(true);
        editor.expressionPopupScrollOffset = 0;
        editor.expressionPopupMaxScroll = 0;
    }

    static void close(GuiActionEditor editor) {
        editor.activeExpressionEditorBinding = null;
        editor.activeBooleanExpressionEditIndex = GuiActionEditor.BOOLEAN_EXPRESSION_EDIT_NONE;
        editor.activeInventoryItemFilterExpressionEditIndex = GuiActionEditor.ITEM_FILTER_EXPRESSION_EDIT_NONE;
        editor.activeExpressionPopupTitle = "";
        editor.activeExpressionPopupBooleanOnly = false;
        editor.activeExpressionPopupTemplateMode = GuiActionEditor.EXPRESSION_TEMPLATE_MODE_SET_VAR;
        editor.expressionPopupSearchField = null;
        editor.expressionPopupInputField = null;
        editor.expressionPopupOriginalValue = "";
        editor.expressionPopupScrollOffset = 0;
        editor.expressionPopupMaxScroll = 0;
        editor.isDraggingExpressionPopupScrollbar = false;
    }

    static void commit(GuiActionEditor editor) {
        if (!editor.isExpressionEditorPopupOpen()) {
            return;
        }
        String nextValue = editor.safe(editor.expressionPopupInputField.getText()).trim();
        if (editor.activeBooleanExpressionEditIndex != GuiActionEditor.BOOLEAN_EXPRESSION_EDIT_NONE) {
            List<String> expressions = editor.getBooleanExpressionList();
            if (!nextValue.isEmpty()) {
                if (editor.activeBooleanExpressionEditIndex == GuiActionEditor.BOOLEAN_EXPRESSION_EDIT_NEW) {
                    expressions.add(nextValue);
                    editor.selectedBooleanExpressionIndex = expressions.size() - 1;
                } else if (editor.activeBooleanExpressionEditIndex >= 0
                        && editor.activeBooleanExpressionEditIndex < expressions.size()) {
                    expressions.set(editor.activeBooleanExpressionEditIndex, nextValue);
                    editor.selectedBooleanExpressionIndex = editor.activeBooleanExpressionEditIndex;
                }
                editor.applyBooleanExpressionListToCurrentParams(expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
        } else if (editor.activeInventoryItemFilterExpressionEditIndex != GuiActionEditor.ITEM_FILTER_EXPRESSION_EDIT_NONE) {
            List<String> expressions = editor.getInventoryItemFilterExpressionList();
            if (!nextValue.isEmpty()) {
                if (editor.activeInventoryItemFilterExpressionEditIndex == GuiActionEditor.ITEM_FILTER_EXPRESSION_EDIT_NEW) {
                    expressions.add(nextValue);
                    editor.selectedInventoryItemFilterExpressionIndex = expressions.size() - 1;
                } else if (editor.activeInventoryItemFilterExpressionEditIndex >= 0
                        && editor.activeInventoryItemFilterExpressionEditIndex < expressions.size()) {
                    expressions.set(editor.activeInventoryItemFilterExpressionEditIndex, nextValue);
                    editor.selectedInventoryItemFilterExpressionIndex = editor.activeInventoryItemFilterExpressionEditIndex;
                }
                editor.applyInventoryItemFilterExpressionListToCurrentParams(expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
        } else if (editor.activeExpressionEditorBinding != null) {
            GuiTextField targetField = editor.getFieldByKey(editor.activeExpressionEditorBinding.paramKey);
            if (targetField != null && !Objects.equals(targetField.getText(), nextValue)) {
                targetField.setText(nextValue);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
            }
        }
        close(editor);
    }

    static void cancel(GuiActionEditor editor) {
        if (editor.isExpressionEditorPopupOpen() && editor.activeExpressionEditorBinding != null) {
            GuiTextField targetField = editor.getFieldByKey(editor.activeExpressionEditorBinding.paramKey);
            if (targetField != null) {
                targetField.setText(editor.expressionPopupOriginalValue);
            }
        }
        close(editor);
    }

    static void syncBounds(GuiActionEditor editor) {
        if (!editor.isExpressionEditorPopupOpen()) {
            return;
        }
        editor.expressionPopupSearchField.x = editor.getExpressionPopupSearchX();
        editor.expressionPopupSearchField.y = editor.getExpressionPopupSearchY();
        editor.expressionPopupSearchField.width = editor.getExpressionPopupSearchWidth();
        editor.expressionPopupSearchField.height = 18;

        editor.expressionPopupInputField.x = editor.getExpressionPopupInputX();
        editor.expressionPopupInputField.y = editor.getExpressionPopupInputY();
        editor.expressionPopupInputField.width = editor.getExpressionPopupInputWidth();
        editor.expressionPopupInputField.height = 18;
    }

    static void draw(GuiActionEditor editor, int mouseX, int mouseY) {
        if (!editor.isExpressionEditorPopupOpen()) {
            return;
        }
        syncBounds(editor);

        int popupX = editor.getExpressionPopupX();
        int popupY = editor.getExpressionPopupY();
        int popupW = editor.getExpressionPopupWidth();
        int popupH = editor.getExpressionPopupHeight();
        int viewportX = editor.getExpressionPopupViewportX();
        int viewportY = editor.getExpressionPopupViewportY();
        int viewportW = editor.getExpressionPopupViewportWidth();
        int viewportH = editor.getExpressionPopupViewportHeight();

        Gui.drawRect(0, 0, editor.getScreenWidth(), editor.getScreenHeight(), 0xA0000000);
        GuiTheme.drawPanel(popupX, popupY, popupW, popupH);
        GuiTheme.drawTitleBar(popupX, popupY, popupW, "编辑表达式", editor.getEditorFontRenderer());

        String popupTitle = editor.activeExpressionPopupTitle;
        if ((popupTitle == null || popupTitle.trim().isEmpty()) && editor.activeExpressionEditorBinding != null) {
            popupTitle = editor.activeExpressionEditorBinding.title;
        }
        editor.getEditorFontRenderer().drawString(editor.safe(popupTitle).trim().isEmpty() ? "表达式" : popupTitle,
                popupX + 12, popupY + 18, 0xFFE8F2FB);

        GuiTheme.drawInputFrameSafe(editor.expressionPopupSearchField.x - 2, editor.expressionPopupSearchField.y - 2,
                editor.expressionPopupSearchField.width + 4, editor.expressionPopupSearchField.height + 4,
                editor.expressionPopupSearchField.isFocused(), true);
        editor.drawEditorTextField(editor.expressionPopupSearchField);
        if (editor.expressionPopupSearchField.getText().trim().isEmpty() && !editor.expressionPopupSearchField.isFocused()) {
            editor.getEditorFontRenderer().drawString(
                    editor.activeExpressionPopupBooleanOnly ? "§7搜索布尔模板 / 示例 / 关键字" : "§7搜索模板名称 / 示例 / 关键字",
                    editor.expressionPopupSearchField.x + 4, editor.expressionPopupSearchField.y + 6, 0xFF7F8FA4);
        }

        GuiTheme.drawInputFrameSafe(viewportX, viewportY, viewportW, viewportH, false, true);

        List<ExpressionTemplateCard> filteredCards = getFilteredCards(editor,
                editor.expressionPopupSearchField.getText());
        List<ExpressionTemplateLayoutEntry> entries = buildLayoutEntries(editor, filteredCards);
        int contentHeight = computeContentHeight(editor, entries);
        editor.expressionPopupMaxScroll = Math.max(0, contentHeight - Math.max(1, viewportH - 8));
        editor.expressionPopupScrollOffset = Math.max(0,
                Math.min(editor.expressionPopupScrollOffset, editor.expressionPopupMaxScroll));

        Gui.drawRect(viewportX + 1, viewportY + 1, viewportX + viewportW - 1, viewportY + viewportH - 1, 0x33151E28);
        editor.beginScissor(viewportX + 1, viewportY + 1, viewportW - 2, viewportH - 2);
        for (ExpressionTemplateLayoutEntry entry : entries) {
            int drawY = entry.y - editor.expressionPopupScrollOffset;
            if (drawY + entry.height < viewportY + 1 || drawY > viewportY + viewportH - 1) {
                continue;
            }
            drawCard(editor, entry, drawY, mouseX, mouseY);
        }
        editor.endScissor();

        if (entries.isEmpty()) {
            GuiTheme.drawEmptyState(viewportX + viewportW / 2, viewportY + viewportH / 2 - 4,
                    editor.activeExpressionPopupBooleanOnly ? "没有匹配的布尔表达式模板" : "没有匹配的表达式模板",
                    editor.getEditorFontRenderer());
        } else if (editor.expressionPopupMaxScroll > 0) {
            GuiTheme.drawScrollbar(editor.getExpressionPopupScrollbarX(), editor.getExpressionPopupScrollbarY(),
                    editor.getExpressionPopupScrollbarWidth(), editor.getExpressionPopupScrollbarHeight(),
                    editor.getExpressionPopupScrollbarThumbY(contentHeight),
                    editor.getExpressionPopupScrollbarThumbHeight(contentHeight));
        }

        editor.getEditorFontRenderer().drawString("最终表达式", editor.expressionPopupInputField.x,
                editor.expressionPopupInputField.y - 12, 0xFFDDDDDD);
        GuiTheme.drawInputFrameSafe(editor.expressionPopupInputField.x - 2, editor.expressionPopupInputField.y - 2,
                editor.expressionPopupInputField.width + 4, editor.expressionPopupInputField.height + 4,
                editor.expressionPopupInputField.isFocused(), true);
        editor.drawEditorTextField(editor.expressionPopupInputField);
        drawActionButton(editor, "确定", editor.getExpressionPopupConfirmButtonX(), editor.getExpressionPopupButtonY(),
                editor.getExpressionPopupButtonWidth(), editor.getExpressionPopupButtonHeight(),
                editor.isPointInside(mouseX, mouseY, editor.getExpressionPopupConfirmButtonX(),
                        editor.getExpressionPopupButtonY(), editor.getExpressionPopupButtonWidth(),
                        editor.getExpressionPopupButtonHeight()),
                GuiTheme.UiState.SUCCESS);
        drawActionButton(editor, "取消", editor.getExpressionPopupCancelButtonX(), editor.getExpressionPopupButtonY(),
                editor.getExpressionPopupButtonWidth(), editor.getExpressionPopupButtonHeight(),
                editor.isPointInside(mouseX, mouseY, editor.getExpressionPopupCancelButtonX(),
                        editor.getExpressionPopupButtonY(), editor.getExpressionPopupButtonWidth(),
                        editor.getExpressionPopupButtonHeight()),
                GuiTheme.UiState.NORMAL);

        editor.getEditorFontRenderer().drawString(
                GuiActionEditor.EXPRESSION_TEMPLATE_MODE_ITEM_FILTER.equals(editor.activeExpressionPopupTemplateMode)
                        ? "当前提供物品过滤表达式模板；一条表达式就是一条过滤规则，点击卡片可直接填入示例"
                        : (editor.activeExpressionPopupBooleanOnly
                                ? "当前只提供输出为布尔值的模板；点击卡片填入示例，确定保存"
                                : "点击卡片可填入示例，悬浮查看详细说明；确定保存，取消 / Esc 放弃修改"),
                popupX + 12, popupY + popupH - 10, 0xFF9FB0C0);

        ExpressionTemplateLayoutEntry hoveredEntry = getHoveredEntry(editor, entries, mouseX, mouseY);
        if (hoveredEntry != null) {
            editor.showEditorHoveringText(buildTooltip(hoveredEntry.card), mouseX, mouseY);
        }
    }

    static void handleClick(GuiActionEditor editor, int mouseX, int mouseY, int mouseButton) {
        if (!editor.isExpressionEditorPopupOpen()) {
            return;
        }
        syncBounds(editor);
        int popupX = editor.getExpressionPopupX();
        int popupY = editor.getExpressionPopupY();
        int popupW = editor.getExpressionPopupWidth();
        int popupH = editor.getExpressionPopupHeight();
        if (!editor.isPointInside(mouseX, mouseY, popupX, popupY, popupW, popupH)) {
            if (mouseButton == 0 || mouseButton == 1) {
                cancel(editor);
            }
            return;
        }

        List<ExpressionTemplateLayoutEntry> entries = buildLayoutEntries(editor,
                getFilteredCards(editor, editor.expressionPopupSearchField.getText()));
        int contentHeight = computeContentHeight(editor, entries);
        editor.expressionPopupMaxScroll = Math.max(0,
                contentHeight - Math.max(1, editor.getExpressionPopupViewportHeight() - 8));
        editor.expressionPopupScrollOffset = Math.max(0,
                Math.min(editor.expressionPopupScrollOffset, editor.expressionPopupMaxScroll));

        if (mouseButton == 0 && editor.expressionPopupMaxScroll > 0
                && editor.isPointInside(mouseX, mouseY, editor.getExpressionPopupScrollbarX(),
                        editor.getExpressionPopupScrollbarY(),
                        editor.getExpressionPopupScrollbarWidth(), editor.getExpressionPopupScrollbarHeight())) {
            editor.isDraggingExpressionPopupScrollbar = true;
            editor.updateExpressionPopupScrollFromMouse(mouseY, contentHeight);
            return;
        }

        editor.expressionPopupSearchField.mouseClicked(mouseX, mouseY, mouseButton);
        editor.expressionPopupInputField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        if (editor.isPointInside(mouseX, mouseY, editor.getExpressionPopupConfirmButtonX(),
                editor.getExpressionPopupButtonY(), editor.getExpressionPopupButtonWidth(),
                editor.getExpressionPopupButtonHeight())) {
            commit(editor);
            return;
        }

        if (editor.isPointInside(mouseX, mouseY, editor.getExpressionPopupCancelButtonX(),
                editor.getExpressionPopupButtonY(), editor.getExpressionPopupButtonWidth(),
                editor.getExpressionPopupButtonHeight())) {
            cancel(editor);
            return;
        }

        ExpressionTemplateLayoutEntry hoveredEntry = getHoveredEntry(editor, entries, mouseX, mouseY);
        if (hoveredEntry != null) {
            editor.expressionPopupInputField.setText(hoveredEntry.card.example);
            editor.expressionPopupInputField.setFocused(true);
            editor.expressionPopupSearchField.setFocused(false);
        }
    }

    static void handleKeyTyped(GuiActionEditor editor, char typedChar, int keyCode) throws IOException {
        if (!editor.isExpressionEditorPopupOpen()) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancel(editor);
            return;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            boolean searchFocused = editor.expressionPopupSearchField.isFocused();
            editor.expressionPopupSearchField.setFocused(!searchFocused);
            editor.expressionPopupInputField.setFocused(searchFocused);
            return;
        }

        if (editor.expressionPopupSearchField.isFocused()) {
            if (editor.expressionPopupSearchField.textboxKeyTyped(typedChar, keyCode)) {
                editor.expressionPopupScrollOffset = 0;
            }
            return;
        }

        editor.expressionPopupInputField.textboxKeyTyped(typedChar, keyCode);
    }

    static void handleScroll(GuiActionEditor editor, int dWheel) {
        if (!editor.isExpressionEditorPopupOpen() || editor.expressionPopupMaxScroll <= 0) {
            return;
        }
        if (dWheel > 0) {
            editor.expressionPopupScrollOffset = Math.max(0, editor.expressionPopupScrollOffset
                    - GuiActionEditor.EXPRESSION_POPUP_SCROLL_STEP);
        } else {
            editor.expressionPopupScrollOffset = Math.min(editor.expressionPopupMaxScroll,
                    editor.expressionPopupScrollOffset + GuiActionEditor.EXPRESSION_POPUP_SCROLL_STEP);
        }
    }

    static List<ExpressionTemplateCard> getFilteredCards(GuiActionEditor editor, String searchText) {
        List<ExpressionTemplateCard> allCards;
        if (GuiActionEditor.EXPRESSION_TEMPLATE_MODE_BOOLEAN.equals(editor.activeExpressionPopupTemplateMode)) {
            allCards = ExpressionTemplateCatalog.buildBooleanCards();
        } else if (GuiActionEditor.EXPRESSION_TEMPLATE_MODE_ITEM_FILTER.equals(editor.activeExpressionPopupTemplateMode)) {
            allCards = ExpressionTemplateCatalog.buildItemFilterCards();
        } else {
            allCards = ExpressionTemplateCatalog.buildSetVarCards();
        }
        String keyword = editor.safe(searchText).trim().toLowerCase(Locale.ROOT);
        if (keyword.isEmpty()) {
            return allCards;
        }
        List<ExpressionTemplateCard> filtered = new ArrayList<ExpressionTemplateCard>();
        for (ExpressionTemplateCard card : allCards) {
            if (matchesCard(editor, card, keyword)) {
                filtered.add(card);
            }
        }
        return filtered;
    }

    static List<ExpressionTemplateLayoutEntry> buildLayoutEntries(GuiActionEditor editor,
            List<ExpressionTemplateCard> cards) {
        List<ExpressionTemplateLayoutEntry> entries = new ArrayList<ExpressionTemplateLayoutEntry>();
        int viewportX = editor.getExpressionPopupViewportX();
        int viewportY = editor.getExpressionPopupViewportY() + 4;
        int viewportW = editor.getExpressionPopupTemplateContentWidth();
        int gap = 8;
        int cols = viewportW >= 540 ? 3 : (viewportW >= 360 ? 2 : 1);
        int cardWidth = Math.max(120, (viewportW - gap * (cols - 1) - 8) / cols);
        int currentY = viewportY;
        int col = 0;
        int rowMaxHeight = 0;

        for (ExpressionTemplateCard card : cards) {
            List<String> exampleLines = wrapCardText(editor, card.example, Math.max(60, cardWidth - 12), 2);
            int cardHeight = Math.max(54, 24 + exampleLines.size() * 10 + 10);
            int cardX = viewportX + 4 + col * (cardWidth + gap);
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

    static int computeContentHeight(GuiActionEditor editor, List<ExpressionTemplateLayoutEntry> entries) {
        int maxBottom = 0;
        for (ExpressionTemplateLayoutEntry entry : entries) {
            maxBottom = Math.max(maxBottom, entry.y + entry.height);
        }
        return Math.max(0, maxBottom - editor.getExpressionPopupViewportY());
    }

    static ExpressionTemplateLayoutEntry getHoveredEntry(GuiActionEditor editor,
            List<ExpressionTemplateLayoutEntry> entries, int mouseX, int mouseY) {
        int viewportX = editor.getExpressionPopupViewportX();
        int viewportY = editor.getExpressionPopupViewportY();
        int viewportW = editor.getExpressionPopupViewportWidth();
        int viewportH = editor.getExpressionPopupViewportHeight();
        if (!editor.isPointInside(mouseX, mouseY, viewportX, viewportY, viewportW, viewportH)) {
            return null;
        }
        for (ExpressionTemplateLayoutEntry entry : entries) {
            int drawY = entry.y - editor.expressionPopupScrollOffset;
            if (editor.isPointInside(mouseX, mouseY, entry.x, drawY, entry.width, entry.height)) {
                return new ExpressionTemplateLayoutEntry(entry.card, entry.x, drawY, entry.width, entry.height,
                        entry.exampleLines);
            }
        }
        return null;
    }

    private static void drawCard(GuiActionEditor editor, ExpressionTemplateLayoutEntry entry, int drawY, int mouseX,
            int mouseY) {
        boolean hovered = editor.isPointInside(mouseX, mouseY, entry.x, drawY, entry.width, entry.height);
        int border = hovered ? 0xFF7AD9FF : 0xFF446278;
        int fill = hovered ? 0xCC1E3344 : 0xB31A2632;
        Gui.drawRect(entry.x - 1, drawY - 1, entry.x + entry.width + 1, drawY + entry.height + 1, border);
        Gui.drawRect(entry.x, drawY, entry.x + entry.width, drawY + entry.height, fill);
        Gui.drawRect(entry.x, drawY, entry.x + entry.width, drawY + 2, 0xFF56B6E8);
        editor.getEditorFontRenderer().drawString(
                editor.getEditorFontRenderer().trimStringToWidth(entry.card.name, Math.max(20, entry.width - 12)),
                entry.x + 6, drawY + 6, 0xFFFFFFFF);
        int lineY = drawY + 20;
        for (String line : entry.exampleLines) {
            editor.getEditorFontRenderer().drawString(line, entry.x + 6, lineY, 0xFFD8E6F2);
            lineY += 10;
        }
    }

    private static void drawActionButton(GuiActionEditor editor, String text, int x, int y, int width, int height,
            boolean hovered, GuiTheme.UiState baseState) {
        GuiTheme.UiState state = hovered ? GuiTheme.UiState.HOVER : baseState;
        GuiTheme.drawButtonFrameSafe(x, y, width, height, state);
        int textX = x + (width - editor.getEditorFontRenderer().getStringWidth(text)) / 2;
        int textY = y + (height - editor.getEditorFontRenderer().FONT_HEIGHT) / 2 + 1;
        editor.getEditorFontRenderer().drawString(text, textX, textY, GuiTheme.getStateTextColor(state));
    }

    private static boolean matchesCard(GuiActionEditor editor, ExpressionTemplateCard card, String keyword) {
        if (card == null) {
            return false;
        }
        if (editor.safe(card.name).toLowerCase(Locale.ROOT).contains(keyword)
                || editor.safe(card.example).toLowerCase(Locale.ROOT).contains(keyword)
                || editor.safe(card.description).toLowerCase(Locale.ROOT).contains(keyword)
                || editor.safe(card.format).toLowerCase(Locale.ROOT).contains(keyword)
                || editor.safe(card.outputExample).toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        for (String alias : card.keywords) {
            if (editor.safe(alias).toLowerCase(Locale.ROOT).contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> buildTooltip(ExpressionTemplateCard card) {
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

    private static List<String> wrapCardText(GuiActionEditor editor, String text, int width, int maxLines) {
        List<String> wrapped = editor.getEditorFontRenderer().listFormattedStringToWidth(editor.safe(text), Math.max(20, width));
        if (wrapped == null || wrapped.isEmpty()) {
            wrapped = new ArrayList<String>();
            wrapped.add("");
        }
        List<String> result = new ArrayList<String>();
        int limit = Math.max(1, maxLines);
        for (int i = 0; i < wrapped.size() && i < limit; i++) {
            String line = wrapped.get(i);
            if (i == limit - 1 && wrapped.size() > limit) {
                line = editor.getEditorFontRenderer().trimStringToWidth(line, Math.max(20, width - 6)) + "...";
            }
            result.add(line);
        }
        return result;
    }
}
