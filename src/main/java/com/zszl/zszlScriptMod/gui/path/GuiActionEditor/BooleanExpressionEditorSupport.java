package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.IndexedHitRegion;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BooleanExpressionEditorSupport {
    private BooleanExpressionEditorSupport() {
    }

    static void initializeState(GuiActionEditor editor) {
        editor.booleanExpressionCardRegions.clear();
        clampSelectionAndScroll(editor, readExpressionList(editor.currentParams, "expressions", "expression").size());
    }

    static List<String> getExpressionList(GuiActionEditor editor) {
        return readExpressionList(editor.currentParams, "expressions", "expression");
    }

    static void applyExpressionListToCurrentParams(GuiActionEditor editor, List<String> expressions) {
        if (editor.currentParams == null) {
            editor.currentParams = new JsonObject();
        }
        writeExpressionsToParams(editor.currentParams, expressions);
        clampSelectionAndScroll(editor, getExpressionList(editor).size());
    }

    static String buildLegacyExpression(List<String> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return "";
        }
        if (expressions.size() == 1) {
            return safe(expressions.get(0)).trim();
        }
        StringBuilder builder = new StringBuilder();
        for (String expression : expressions) {
            String value = safe(expression).trim();
            if (value.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" && ");
            }
            builder.append("(").append(value).append(")");
        }
        return builder.toString();
    }

    static void writeExpressionsToParams(JsonObject target, List<String> expressions) {
        if (target == null) {
            return;
        }
        JsonArray array = new JsonArray();
        List<String> normalized = normalizeExpressions(expressions);
        if (normalized.isEmpty()) {
            target.remove("expressions");
            target.remove("expression");
            return;
        }
        for (String expression : normalized) {
            array.add(expression);
        }
        target.add("expressions", array);
        target.addProperty("expression", buildLegacyExpression(normalized));
    }

    static int addCardEditor(GuiActionEditor editor, int width, int x, int y) {
        editor.booleanExpressionToolbarBaseY = y;
        editor.booleanExpressionCardListBaseY = y + getToolbarHeight(width) + 8;
        editor.btnAddBooleanExpression = new ThemedButton(GuiActionEditor.BTN_ID_ADD_BOOLEAN_EXPRESSION, x, y, 72, 20,
                I18n.format("gui.path.manager.add"));
        editor.btnEditBooleanExpression = new ThemedButton(GuiActionEditor.BTN_ID_EDIT_BOOLEAN_EXPRESSION, x, y, 72, 20,
                I18n.format("gui.common.edit"));
        editor.btnDeleteBooleanExpression = new ThemedButton(GuiActionEditor.BTN_ID_DELETE_BOOLEAN_EXPRESSION, x, y, 72,
                20, I18n.format("gui.common.delete"));
        editor.btnMoveBooleanExpressionUp = new ThemedButton(GuiActionEditor.BTN_ID_MOVE_BOOLEAN_EXPRESSION_UP, x, y,
                72,
                20, I18n.format("gui.path.manager.move_up"));
        editor.btnMoveBooleanExpressionDown = new ThemedButton(GuiActionEditor.BTN_ID_MOVE_BOOLEAN_EXPRESSION_DOWN, x,
                y,
                72, 20, I18n.format("gui.path.manager.move_down"));
        editor.addEditorButton(editor.btnAddBooleanExpression);
        editor.addEditorButton(editor.btnEditBooleanExpression);
        editor.addEditorButton(editor.btnDeleteBooleanExpression);
        editor.addEditorButton(editor.btnMoveBooleanExpressionUp);
        editor.addEditorButton(editor.btnMoveBooleanExpressionDown);
        updateControlLayout(editor);
        return getToolbarHeight(width) + 8 + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT;
    }

    static void updateControlLayout(GuiActionEditor editor) {
        if (!editor.isBooleanExpressionActionSelected() || editor.booleanExpressionToolbarBaseY < 0) {
            return;
        }
        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int toolbarY = editor.booleanExpressionToolbarBaseY - editor.paramScrollOffset;
        int gap = 6;
        boolean wrap = shouldWrapToolbar(fieldWidth);
        if (!wrap) {
            int buttonWidth = Math.max(52, (fieldWidth - gap * 4) / 5);
            layoutButton(editor, editor.btnAddBooleanExpression, x, toolbarY, buttonWidth,
                    editor.booleanExpressionToolbarBaseY);
            layoutButton(editor, editor.btnEditBooleanExpression, x + (buttonWidth + gap), toolbarY, buttonWidth,
                    editor.booleanExpressionToolbarBaseY);
            layoutButton(editor, editor.btnDeleteBooleanExpression, x + (buttonWidth + gap) * 2, toolbarY, buttonWidth,
                    editor.booleanExpressionToolbarBaseY);
            layoutButton(editor, editor.btnMoveBooleanExpressionUp, x + (buttonWidth + gap) * 3, toolbarY, buttonWidth,
                    editor.booleanExpressionToolbarBaseY);
            layoutButton(editor, editor.btnMoveBooleanExpressionDown, x + (buttonWidth + gap) * 4, toolbarY,
                    Math.max(52, fieldWidth - buttonWidth * 4 - gap * 4), editor.booleanExpressionToolbarBaseY);
        } else {
            int topWidth = Math.max(64, (fieldWidth - gap * 2) / 3);
            int bottomWidth = Math.max(80, (fieldWidth - gap) / 2);
            layoutButton(editor, editor.btnAddBooleanExpression, x, toolbarY, topWidth,
                    editor.booleanExpressionToolbarBaseY);
            layoutButton(editor, editor.btnEditBooleanExpression, x + topWidth + gap, toolbarY, topWidth,
                    editor.booleanExpressionToolbarBaseY);
            layoutButton(editor, editor.btnDeleteBooleanExpression, x + (topWidth + gap) * 2, toolbarY,
                    Math.max(64, fieldWidth - topWidth * 2 - gap * 2), editor.booleanExpressionToolbarBaseY);
            int secondRowBaseY = editor.booleanExpressionToolbarBaseY + 26;
            int secondRowY = secondRowBaseY - editor.paramScrollOffset;
            layoutButton(editor, editor.btnMoveBooleanExpressionUp, x, secondRowY, bottomWidth, secondRowBaseY);
            layoutButton(editor, editor.btnMoveBooleanExpressionDown, x + bottomWidth + gap, secondRowY,
                    Math.max(80, fieldWidth - bottomWidth - gap), secondRowBaseY);
        }
        updateButtonState(editor);
    }

    static int getCustomBottomBaseY(GuiActionEditor editor) {
        if (!editor.isBooleanExpressionActionSelected() || editor.booleanExpressionCardListBaseY < 0) {
            return 0;
        }
        return editor.booleanExpressionCardListBaseY + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT + 8;
    }

    static void drawCustomSection(GuiActionEditor editor, int mouseX, int mouseY) {
        editor.booleanExpressionCardRegions.clear();
        if (!editor.isBooleanExpressionActionSelected() || editor.booleanExpressionCardListBaseY < 0) {
            return;
        }

        List<String> expressions = getExpressionList(editor);
        clampSelectionAndScroll(editor, expressions.size());
        updateButtonState(editor);

        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int listY = getCardListY(editor);
        int listHeight = GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT;
        int titleY = editor.booleanExpressionToolbarBaseY - editor.paramScrollOffset - 12;
        String title = I18n.format("gui.path.action_editor.label.boolean_expression_cards");
        String suffix = " §7(" + I18n.format("gui.path.action_editor.option.wait_combined_mode.all") + ")";
        if (titleY + 10 >= editor.paramViewTop && titleY <= editor.paramViewBottom) {
            editor.getEditorFontRenderer().drawString(title + suffix, x, titleY, 0xFFDDDDDD);
        }

        if (listY + listHeight < editor.paramViewTop || listY > editor.paramViewBottom) {
            return;
        }

        GuiTheme.drawInputFrameSafe(x, listY, fieldWidth, listHeight, false, true);
        Gui.drawRect(x + 1, listY + 1, x + fieldWidth - 1, listY + listHeight - 1, 0x2A101820);

        if (expressions.isEmpty()) {
            GuiTheme.drawEmptyState(x + fieldWidth / 2, listY + listHeight / 2 - 10,
                    I18n.format("gui.path.action_editor.empty.boolean_expression_cards"),
                    editor.getEditorFontRenderer());
            editor.getEditorFontRenderer().drawString(
                    I18n.format("gui.path.action_editor.help.boolean_expression_cards_short"),
                    x + 8, listY + listHeight - 14, 0xFF8EA4B8);
            return;
        }

        int visibleRows = getVisibleRowCount();
        int maxScroll = getMaxScroll(expressions.size());
        int cardX = x + 4;
        int cardWidth = fieldWidth - 14;
        int rowStride = GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT
                + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_GAP;
        for (int row = 0; row < visibleRows; row++) {
            int expressionIndex = row + editor.booleanExpressionCardScrollOffset;
            if (expressionIndex >= expressions.size()) {
                break;
            }
            int cardY = listY + 4 + row * rowStride;
            boolean selected = expressionIndex == editor.selectedBooleanExpressionIndex;
            boolean hovered = editor.isPointInside(mouseX, mouseY, cardX, cardY, cardWidth,
                    GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT);
            int border = selected ? 0xFF7AD9FF : (hovered ? 0xFF5F8FAE : 0xFF3D586B);
            int fill = selected ? 0xAA244053 : (hovered ? 0x8A22313E : 0x6A18232C);
            Gui.drawRect(cardX - 1, cardY - 1, cardX + cardWidth + 1,
                    cardY + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT + 1, border);
            Gui.drawRect(cardX, cardY, cardX + cardWidth, cardY + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT,
                    fill);
            Gui.drawRect(cardX, cardY, cardX + 3, cardY + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT,
                    selected ? 0xFF56B6E8 : 0xFF4C6E84);

            String indexText = "#" + (expressionIndex + 1);
            editor.getEditorFontRenderer().drawString(indexText, cardX + 8, cardY + 6,
                    selected ? 0xFFFFFFFF : 0xFFD8E5F1);
            List<String> lines = editor.wrapExpressionCardText(expressions.get(expressionIndex),
                    Math.max(60, cardWidth - 42), 2);
            int lineY = cardY + 6;
            for (String line : lines) {
                editor.getEditorFontRenderer().drawString(line, cardX + 30, lineY, 0xFFF1F7FC);
                lineY += 10;
            }
            editor.booleanExpressionCardRegions.add(new IndexedHitRegion(cardX, cardY, cardWidth,
                    GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT, expressionIndex));
        }

        if (maxScroll > 0) {
            int thumbHeight = Math.max(12, (int) ((visibleRows / (float) expressions.size()) * (listHeight - 4)));
            int trackHeight = Math.max(1, (listHeight - 4) - thumbHeight);
            int thumbY = listY + 2
                    + (int) ((editor.booleanExpressionCardScrollOffset / (float) maxScroll) * trackHeight);
            GuiTheme.drawScrollbar(x + fieldWidth - 6, listY + 2, 4, listHeight - 4, thumbY, thumbHeight);
        }
    }

    static boolean handleButtonAction(GuiActionEditor editor, GuiButton button) {
        if (button.id == GuiActionEditor.BTN_ID_ADD_BOOLEAN_EXPRESSION) {
            editor.openBooleanExpressionPopup(GuiActionEditor.BOOLEAN_EXPRESSION_EDIT_NEW);
            return true;
        }

        if (button.id == GuiActionEditor.BTN_ID_EDIT_BOOLEAN_EXPRESSION) {
            if (editor.selectedBooleanExpressionIndex >= 0
                    && editor.selectedBooleanExpressionIndex < getExpressionList(editor).size()) {
                editor.openBooleanExpressionPopup(editor.selectedBooleanExpressionIndex);
            }
            return true;
        }

        if (button.id == GuiActionEditor.BTN_ID_DELETE_BOOLEAN_EXPRESSION) {
            List<String> expressions = getExpressionList(editor);
            if (editor.selectedBooleanExpressionIndex >= 0
                    && editor.selectedBooleanExpressionIndex < expressions.size()) {
                expressions.remove(editor.selectedBooleanExpressionIndex);
                if (editor.selectedBooleanExpressionIndex >= expressions.size()) {
                    editor.selectedBooleanExpressionIndex = expressions.isEmpty() ? -1 : expressions.size() - 1;
                }
                applyExpressionListToCurrentParams(editor, expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
            return true;
        }

        if (button.id == GuiActionEditor.BTN_ID_MOVE_BOOLEAN_EXPRESSION_UP) {
            List<String> expressions = getExpressionList(editor);
            if (editor.selectedBooleanExpressionIndex > 0
                    && editor.selectedBooleanExpressionIndex < expressions.size()) {
                Collections.swap(expressions, editor.selectedBooleanExpressionIndex,
                        editor.selectedBooleanExpressionIndex - 1);
                editor.selectedBooleanExpressionIndex--;
                applyExpressionListToCurrentParams(editor, expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
            return true;
        }

        if (button.id == GuiActionEditor.BTN_ID_MOVE_BOOLEAN_EXPRESSION_DOWN) {
            List<String> expressions = getExpressionList(editor);
            if (editor.selectedBooleanExpressionIndex >= 0
                    && editor.selectedBooleanExpressionIndex < expressions.size() - 1) {
                Collections.swap(expressions, editor.selectedBooleanExpressionIndex,
                        editor.selectedBooleanExpressionIndex + 1);
                editor.selectedBooleanExpressionIndex++;
                applyExpressionListToCurrentParams(editor, expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
            return true;
        }

        return false;
    }

    static boolean handleCustomClick(GuiActionEditor editor, int mouseX, int mouseY) {
        if (!editor.isBooleanExpressionActionSelected() || editor.booleanExpressionCardListBaseY < 0) {
            return false;
        }
        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int listY = getCardListY(editor);
        if (!editor.isPointInside(mouseX, mouseY, x, listY, fieldWidth,
                GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT)) {
            return false;
        }
        for (IndexedHitRegion region : editor.booleanExpressionCardRegions) {
            if (region.contains(mouseX, mouseY)) {
                editor.selectedBooleanExpressionIndex = region.index;
                clampSelectionAndScroll(editor, getExpressionList(editor).size());
                return true;
            }
        }
        editor.selectedBooleanExpressionIndex = -1;
        return true;
    }

    static boolean handleCustomWheel(GuiActionEditor editor, int mouseX, int mouseY, int dWheel) {
        List<String> expressions = getExpressionList(editor);
        if (!editor.isBooleanExpressionActionSelected() || expressions.isEmpty() || dWheel == 0) {
            return false;
        }
        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int listY = getCardListY(editor);
        if (!editor.isPointInside(mouseX, mouseY, x, listY, fieldWidth,
                GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT)) {
            return false;
        }
        int maxScroll = getMaxScroll(expressions.size());
        if (maxScroll <= 0) {
            return true;
        }
        if (dWheel > 0) {
            editor.booleanExpressionCardScrollOffset = Math.max(0, editor.booleanExpressionCardScrollOffset - 1);
        } else {
            editor.booleanExpressionCardScrollOffset = Math.min(maxScroll,
                    editor.booleanExpressionCardScrollOffset + 1);
        }
        return true;
    }

    private static List<String> readExpressionList(JsonObject source, String arrayKey, String legacyKey) {
        List<String> expressions = new ArrayList<String>();
        if (source == null) {
            return expressions;
        }
        if (source.has(arrayKey) && source.get(arrayKey).isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray(arrayKey)) {
                if (element != null && element.isJsonPrimitive()) {
                    String expression = safe(element.getAsString()).trim();
                    if (!expression.isEmpty()) {
                        expressions.add(expression);
                    }
                }
            }
            if (!expressions.isEmpty()) {
                return expressions;
            }
        }
        String legacyExpression = safe(source.has(legacyKey) && source.get(legacyKey).isJsonPrimitive()
                ? source.get(legacyKey).getAsString()
                : "").trim();
        if (!legacyExpression.isEmpty()) {
            expressions.add(legacyExpression);
        }
        return expressions;
    }

    private static List<String> normalizeExpressions(List<String> expressions) {
        List<String> normalized = new ArrayList<String>();
        if (expressions == null) {
            return normalized;
        }
        for (String expression : expressions) {
            String value = safe(expression).trim();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static boolean shouldWrapToolbar(int fieldWidth) {
        return fieldWidth < 360;
    }

    private static int getToolbarHeight(int fieldWidth) {
        return shouldWrapToolbar(fieldWidth) ? 46 : 20;
    }

    private static int getCardListY(GuiActionEditor editor) {
        return editor.booleanExpressionCardListBaseY - editor.paramScrollOffset;
    }

    private static int getVisibleRowCount() {
        return Math.max(1, Math.max(0, GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT - 4)
                / (GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT
                        + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_GAP));
    }

    private static int getMaxScroll(int expressionCount) {
        return Math.max(0, expressionCount - getVisibleRowCount());
    }

    private static void clampSelectionAndScroll(GuiActionEditor editor, int expressionCount) {
        if (expressionCount <= 0) {
            editor.selectedBooleanExpressionIndex = -1;
            editor.booleanExpressionCardScrollOffset = 0;
            return;
        }
        if (editor.selectedBooleanExpressionIndex < 0) {
            editor.selectedBooleanExpressionIndex = 0;
        } else if (editor.selectedBooleanExpressionIndex >= expressionCount) {
            editor.selectedBooleanExpressionIndex = expressionCount - 1;
        }
        int maxScroll = getMaxScroll(expressionCount);
        editor.booleanExpressionCardScrollOffset = MathHelper.clamp(editor.booleanExpressionCardScrollOffset, 0,
                maxScroll);
        if (editor.selectedBooleanExpressionIndex < editor.booleanExpressionCardScrollOffset) {
            editor.booleanExpressionCardScrollOffset = editor.selectedBooleanExpressionIndex;
        } else if (editor.selectedBooleanExpressionIndex >= editor.booleanExpressionCardScrollOffset
                + getVisibleRowCount()) {
            editor.booleanExpressionCardScrollOffset = editor.selectedBooleanExpressionIndex - getVisibleRowCount() + 1;
        }
    }

    private static void layoutButton(GuiActionEditor editor, GuiButton button, int x, int y, int width, int baseY) {
        if (button == null) {
            return;
        }
        button.x = x;
        button.y = y;
        button.width = Math.max(52, width);
        editor.registerScrollableButton(button, baseY);
    }

    private static void updateButtonState(GuiActionEditor editor) {
        List<String> expressions = getExpressionList(editor);
        clampSelectionAndScroll(editor, expressions.size());
        boolean hasSelection = editor.selectedBooleanExpressionIndex >= 0
                && editor.selectedBooleanExpressionIndex < expressions.size();
        if (editor.btnEditBooleanExpression != null) {
            editor.btnEditBooleanExpression.enabled = hasSelection;
        }
        if (editor.btnDeleteBooleanExpression != null) {
            editor.btnDeleteBooleanExpression.enabled = hasSelection;
        }
        if (editor.btnMoveBooleanExpressionUp != null) {
            editor.btnMoveBooleanExpressionUp.enabled = hasSelection && editor.selectedBooleanExpressionIndex > 0;
        }
        if (editor.btnMoveBooleanExpressionDown != null) {
            editor.btnMoveBooleanExpressionDown.enabled = hasSelection
                    && editor.selectedBooleanExpressionIndex >= 0
                    && editor.selectedBooleanExpressionIndex < expressions.size() - 1;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

