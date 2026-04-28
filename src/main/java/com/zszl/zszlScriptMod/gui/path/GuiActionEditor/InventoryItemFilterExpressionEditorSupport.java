package com.zszl.zszlScriptMod.gui.path.GuiActionEditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.IndexedHitRegion;
import com.zszl.zszlScriptMod.handlers.ItemFilterHandler;
import com.zszl.zszlScriptMod.path.InventoryItemFilterExpressionEngine;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class InventoryItemFilterExpressionEditorSupport {
    private InventoryItemFilterExpressionEditorSupport() {
    }

    static void initializeState(GuiActionEditor editor) {
        editor.inventoryItemFilterExpressionCardRegions.clear();
        clampSelectionAndScroll(editor, getExpressionList(editor).size());
    }

    static List<String> getExpressionList(GuiActionEditor editor) {
        List<String> expressions = InventoryItemFilterExpressionEngine.readExpressions(editor.currentParams);
        if (!expressions.isEmpty()) {
            return expressions;
        }
        String legacy = buildLegacyCompatibleExpression(editor.currentParams);
        if (legacy.isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> converted = new ArrayList<String>();
        converted.add(legacy);
        return converted;
    }

    static void applyExpressionListToCurrentParams(GuiActionEditor editor, List<String> expressions) {
        if (editor.currentParams == null) {
            editor.currentParams = new JsonObject();
        }
        InventoryItemFilterExpressionEngine.writeExpressions(editor.currentParams, expressions);
        clearLegacyFields(editor.currentParams);
        clampSelectionAndScroll(editor, getExpressionList(editor).size());
    }

    static int addCardEditor(GuiActionEditor editor, int width, int x, int y) {
        editor.inventoryItemFilterExpressionToolbarBaseY = y;
        editor.inventoryItemFilterExpressionCardListBaseY = y + getToolbarHeight(width) + 8;
        editor.btnAddInventoryItemFilterExpression = new ThemedButton(
                GuiActionEditor.BTN_ID_ADD_INVENTORY_ITEM_FILTER_EXPRESSION, x, y, 72, 20,
                I18n.format("gui.path.manager.add"));
        editor.btnEditInventoryItemFilterExpression = new ThemedButton(
                GuiActionEditor.BTN_ID_EDIT_INVENTORY_ITEM_FILTER_EXPRESSION, x, y, 72, 20,
                I18n.format("gui.common.edit"));
        editor.btnDeleteInventoryItemFilterExpression = new ThemedButton(
                GuiActionEditor.BTN_ID_DELETE_INVENTORY_ITEM_FILTER_EXPRESSION, x, y, 72, 20,
                I18n.format("gui.common.delete"));
        editor.btnMoveInventoryItemFilterExpressionUp = new ThemedButton(
                GuiActionEditor.BTN_ID_MOVE_INVENTORY_ITEM_FILTER_EXPRESSION_UP, x, y, 72, 20,
                I18n.format("gui.path.manager.move_up"));
        editor.btnMoveInventoryItemFilterExpressionDown = new ThemedButton(
                GuiActionEditor.BTN_ID_MOVE_INVENTORY_ITEM_FILTER_EXPRESSION_DOWN, x, y, 72, 20,
                I18n.format("gui.path.manager.move_down"));
        editor.addEditorButton(editor.btnAddInventoryItemFilterExpression);
        editor.addEditorButton(editor.btnEditInventoryItemFilterExpression);
        editor.addEditorButton(editor.btnDeleteInventoryItemFilterExpression);
        editor.addEditorButton(editor.btnMoveInventoryItemFilterExpressionUp);
        editor.addEditorButton(editor.btnMoveInventoryItemFilterExpressionDown);
        updateControlLayout(editor);
        return getToolbarHeight(width) + 8 + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT;
    }

    static void updateControlLayout(GuiActionEditor editor) {
        if (!editor.supportsInventoryItemFilterExpressionEditor() || editor.inventoryItemFilterExpressionToolbarBaseY < 0) {
            return;
        }
        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int toolbarY = editor.inventoryItemFilterExpressionToolbarBaseY - editor.paramScrollOffset;
        int gap = 6;
        boolean wrap = shouldWrapToolbar(fieldWidth);
        if (!wrap) {
            int buttonWidth = Math.max(52, (fieldWidth - gap * 4) / 5);
            layoutButton(editor, editor.btnAddInventoryItemFilterExpression, x, toolbarY, buttonWidth,
                    editor.inventoryItemFilterExpressionToolbarBaseY);
            layoutButton(editor, editor.btnEditInventoryItemFilterExpression, x + (buttonWidth + gap), toolbarY,
                    buttonWidth, editor.inventoryItemFilterExpressionToolbarBaseY);
            layoutButton(editor, editor.btnDeleteInventoryItemFilterExpression, x + (buttonWidth + gap) * 2, toolbarY,
                    buttonWidth, editor.inventoryItemFilterExpressionToolbarBaseY);
            layoutButton(editor, editor.btnMoveInventoryItemFilterExpressionUp, x + (buttonWidth + gap) * 3, toolbarY,
                    buttonWidth, editor.inventoryItemFilterExpressionToolbarBaseY);
            layoutButton(editor, editor.btnMoveInventoryItemFilterExpressionDown, x + (buttonWidth + gap) * 4, toolbarY,
                    Math.max(52, fieldWidth - buttonWidth * 4 - gap * 4),
                    editor.inventoryItemFilterExpressionToolbarBaseY);
        } else {
            int topWidth = Math.max(64, (fieldWidth - gap * 2) / 3);
            int bottomWidth = Math.max(80, (fieldWidth - gap) / 2);
            layoutButton(editor, editor.btnAddInventoryItemFilterExpression, x, toolbarY, topWidth,
                    editor.inventoryItemFilterExpressionToolbarBaseY);
            layoutButton(editor, editor.btnEditInventoryItemFilterExpression, x + topWidth + gap, toolbarY, topWidth,
                    editor.inventoryItemFilterExpressionToolbarBaseY);
            layoutButton(editor, editor.btnDeleteInventoryItemFilterExpression, x + (topWidth + gap) * 2, toolbarY,
                    Math.max(64, fieldWidth - topWidth * 2 - gap * 2),
                    editor.inventoryItemFilterExpressionToolbarBaseY);
            int secondRowBaseY = editor.inventoryItemFilterExpressionToolbarBaseY + 26;
            int secondRowY = secondRowBaseY - editor.paramScrollOffset;
            layoutButton(editor, editor.btnMoveInventoryItemFilterExpressionUp, x, secondRowY, bottomWidth,
                    secondRowBaseY);
            layoutButton(editor, editor.btnMoveInventoryItemFilterExpressionDown, x + bottomWidth + gap, secondRowY,
                    Math.max(80, fieldWidth - bottomWidth - gap), secondRowBaseY);
        }
        updateButtonState(editor);
    }

    static int getCustomBottomBaseY(GuiActionEditor editor) {
        if (!editor.supportsInventoryItemFilterExpressionEditor() || editor.inventoryItemFilterExpressionCardListBaseY < 0) {
            return 0;
        }
        return editor.inventoryItemFilterExpressionCardListBaseY + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT + 8;
    }

    static void drawCustomSection(GuiActionEditor editor, int mouseX, int mouseY) {
        editor.inventoryItemFilterExpressionCardRegions.clear();
        if (!editor.supportsInventoryItemFilterExpressionEditor() || editor.inventoryItemFilterExpressionCardListBaseY < 0) {
            return;
        }

        List<String> expressions = getExpressionList(editor);
        clampSelectionAndScroll(editor, expressions.size());
        updateButtonState(editor);

        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int listY = getCardListY(editor);
        int listHeight = GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT;
        int titleY = editor.inventoryItemFilterExpressionToolbarBaseY - editor.paramScrollOffset - 12;
        if (titleY + 10 >= editor.paramViewTop && titleY <= editor.paramViewBottom) {
            editor.getEditorFontRenderer().drawString(
                    I18n.format("gui.path.action_editor.label.item_filter_expression_cards"), x, titleY, 0xFFDDDDDD);
        }

        if (listY + listHeight < editor.paramViewTop || listY > editor.paramViewBottom) {
            return;
        }

        GuiTheme.drawInputFrameSafe(x, listY, fieldWidth, listHeight, false, true);
        Gui.drawRect(x + 1, listY + 1, x + fieldWidth - 1, listY + listHeight - 1, 0x2A101820);

        if (expressions.isEmpty()) {
            GuiTheme.drawEmptyState(x + fieldWidth / 2, listY + listHeight / 2 - 10,
                    I18n.format("gui.path.action_editor.empty.item_filter_expression_cards"),
                    editor.getEditorFontRenderer());
            editor.getEditorFontRenderer().drawString(
                    I18n.format("gui.path.action_editor.help.item_filter_expression_cards_short"),
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
            int expressionIndex = row + editor.inventoryItemFilterExpressionCardScrollOffset;
            if (expressionIndex >= expressions.size()) {
                break;
            }
            int cardY = listY + 4 + row * rowStride;
            boolean selected = expressionIndex == editor.selectedInventoryItemFilterExpressionIndex;
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

            editor.getEditorFontRenderer().drawString("#" + (expressionIndex + 1), cardX + 8, cardY + 6,
                    selected ? 0xFFFFFFFF : 0xFFD8E5F1);
            List<String> lines = editor.wrapExpressionCardText(expressions.get(expressionIndex),
                    Math.max(60, cardWidth - 42), 2);
            int lineY = cardY + 6;
            for (String line : lines) {
                editor.getEditorFontRenderer().drawString(line, cardX + 30, lineY, 0xFFF1F7FC);
                lineY += 10;
            }
            editor.inventoryItemFilterExpressionCardRegions.add(new IndexedHitRegion(cardX, cardY, cardWidth,
                    GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT, expressionIndex));
        }

        if (maxScroll > 0) {
            int thumbHeight = Math.max(12, (int) ((visibleRows / (float) expressions.size()) * (listHeight - 4)));
            int trackHeight = Math.max(1, (listHeight - 4) - thumbHeight);
            int thumbY = listY + 2
                    + (int) ((editor.inventoryItemFilterExpressionCardScrollOffset / (float) maxScroll) * trackHeight);
            GuiTheme.drawScrollbar(x + fieldWidth - 6, listY + 2, 4, listHeight - 4, thumbY, thumbHeight);
        }
    }

    static boolean handleButtonAction(GuiActionEditor editor, GuiButton button) {
        if (button.id == GuiActionEditor.BTN_ID_ADD_INVENTORY_ITEM_FILTER_EXPRESSION) {
            editor.openInventoryItemFilterExpressionPopup(GuiActionEditor.ITEM_FILTER_EXPRESSION_EDIT_NEW);
            return true;
        }
        if (button.id == GuiActionEditor.BTN_ID_EDIT_INVENTORY_ITEM_FILTER_EXPRESSION) {
            if (editor.selectedInventoryItemFilterExpressionIndex >= 0
                    && editor.selectedInventoryItemFilterExpressionIndex < getExpressionList(editor).size()) {
                editor.openInventoryItemFilterExpressionPopup(editor.selectedInventoryItemFilterExpressionIndex);
            }
            return true;
        }
        if (button.id == GuiActionEditor.BTN_ID_DELETE_INVENTORY_ITEM_FILTER_EXPRESSION) {
            List<String> expressions = getExpressionList(editor);
            if (editor.selectedInventoryItemFilterExpressionIndex >= 0
                    && editor.selectedInventoryItemFilterExpressionIndex < expressions.size()) {
                expressions.remove(editor.selectedInventoryItemFilterExpressionIndex);
                if (editor.selectedInventoryItemFilterExpressionIndex >= expressions.size()) {
                    editor.selectedInventoryItemFilterExpressionIndex = expressions.isEmpty()
                            ? -1
                            : expressions.size() - 1;
                }
                applyExpressionListToCurrentParams(editor, expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
            return true;
        }
        if (button.id == GuiActionEditor.BTN_ID_MOVE_INVENTORY_ITEM_FILTER_EXPRESSION_UP) {
            List<String> expressions = getExpressionList(editor);
            if (editor.selectedInventoryItemFilterExpressionIndex > 0
                    && editor.selectedInventoryItemFilterExpressionIndex < expressions.size()) {
                Collections.swap(expressions, editor.selectedInventoryItemFilterExpressionIndex,
                        editor.selectedInventoryItemFilterExpressionIndex - 1);
                editor.selectedInventoryItemFilterExpressionIndex--;
                applyExpressionListToCurrentParams(editor, expressions);
                editor.hasUnsavedChanges = true;
                editor.pendingSwitchActionType = null;
                editor.refreshDynamicParamLayout();
            }
            return true;
        }
        if (button.id == GuiActionEditor.BTN_ID_MOVE_INVENTORY_ITEM_FILTER_EXPRESSION_DOWN) {
            List<String> expressions = getExpressionList(editor);
            if (editor.selectedInventoryItemFilterExpressionIndex >= 0
                    && editor.selectedInventoryItemFilterExpressionIndex < expressions.size() - 1) {
                Collections.swap(expressions, editor.selectedInventoryItemFilterExpressionIndex,
                        editor.selectedInventoryItemFilterExpressionIndex + 1);
                editor.selectedInventoryItemFilterExpressionIndex++;
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
        if (!editor.supportsInventoryItemFilterExpressionEditor() || editor.inventoryItemFilterExpressionCardListBaseY < 0) {
            return false;
        }
        int x = editor.getParamContentX();
        int fieldWidth = editor.getParamFieldWidth();
        int listY = getCardListY(editor);
        if (!editor.isPointInside(mouseX, mouseY, x, listY, fieldWidth,
                GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT)) {
            return false;
        }
        for (IndexedHitRegion region : editor.inventoryItemFilterExpressionCardRegions) {
            if (region.contains(mouseX, mouseY)) {
                editor.selectedInventoryItemFilterExpressionIndex = region.index;
                clampSelectionAndScroll(editor, getExpressionList(editor).size());
                return true;
            }
        }
        editor.selectedInventoryItemFilterExpressionIndex = -1;
        return true;
    }

    static boolean handleCustomWheel(GuiActionEditor editor, int mouseX, int mouseY, int dWheel) {
        List<String> expressions = getExpressionList(editor);
        if (!editor.supportsInventoryItemFilterExpressionEditor() || expressions.isEmpty() || dWheel == 0) {
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
            editor.inventoryItemFilterExpressionCardScrollOffset = Math.max(0,
                    editor.inventoryItemFilterExpressionCardScrollOffset - 1);
        } else {
            editor.inventoryItemFilterExpressionCardScrollOffset = Math.min(maxScroll,
                    editor.inventoryItemFilterExpressionCardScrollOffset + 1);
        }
        return true;
    }

    private static void clearLegacyFields(JsonObject target) {
        if (target == null) {
            return;
        }
        target.remove("itemName");
        target.remove("matchMode");
        target.remove("requiredNbtTags");
        target.remove("requiredNbtTagsText");
        target.remove("requiredNbtTagsMode");
        target.remove("moveChestRules");
    }

    private static String buildLegacyCompatibleExpression(JsonObject params) {
        if (params == null) {
            return "";
        }
        String itemName = params.has("itemName") ? params.get("itemName").getAsString() : "";
        String matchMode = params.has("matchMode") ? params.get("matchMode").getAsString() : "CONTAINS";
        List<String> requiredNbtTags = ItemFilterHandler.readTagFilters(params, "requiredNbtTags", "requiredNbtTagsText");
        String requiredNbtTagMatchMode = ItemFilterHandler.readRequiredNbtTagMatchMode(params);
        return InventoryItemFilterExpressionEngine.buildLegacyCompatibleExpression(itemName, matchMode, requiredNbtTags,
                requiredNbtTagMatchMode);
    }

    private static int getToolbarHeight(int width) {
        return shouldWrapToolbar(width) ? 46 : 20;
    }

    private static boolean shouldWrapToolbar(int width) {
        return width < 340;
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
        int size = getExpressionList(editor).size();
        boolean hasSelection = editor.selectedInventoryItemFilterExpressionIndex >= 0
                && editor.selectedInventoryItemFilterExpressionIndex < size;
        if (editor.btnEditInventoryItemFilterExpression != null) {
            editor.btnEditInventoryItemFilterExpression.enabled = hasSelection;
        }
        if (editor.btnDeleteInventoryItemFilterExpression != null) {
            editor.btnDeleteInventoryItemFilterExpression.enabled = hasSelection;
        }
        if (editor.btnMoveInventoryItemFilterExpressionUp != null) {
            editor.btnMoveInventoryItemFilterExpressionUp.enabled = hasSelection
                    && editor.selectedInventoryItemFilterExpressionIndex > 0;
        }
        if (editor.btnMoveInventoryItemFilterExpressionDown != null) {
            editor.btnMoveInventoryItemFilterExpressionDown.enabled = hasSelection
                    && editor.selectedInventoryItemFilterExpressionIndex < size - 1;
        }
    }

    private static int getCardListY(GuiActionEditor editor) {
        return editor.inventoryItemFilterExpressionCardListBaseY - editor.paramScrollOffset;
    }

    private static int getVisibleRowCount() {
        int rowStride = GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_HEIGHT + GuiActionEditor.BOOLEAN_EXPRESSION_CARD_ROW_GAP;
        return Math.max(1, (GuiActionEditor.BOOLEAN_EXPRESSION_CARD_LIST_HEIGHT - 4) / rowStride);
    }

    private static int getMaxScroll(int size) {
        return Math.max(0, size - getVisibleRowCount());
    }

    private static void clampSelectionAndScroll(GuiActionEditor editor, int size) {
        if (size <= 0) {
            editor.selectedInventoryItemFilterExpressionIndex = -1;
            editor.inventoryItemFilterExpressionCardScrollOffset = 0;
            return;
        }
        editor.selectedInventoryItemFilterExpressionIndex = MathHelper.clamp(
                editor.selectedInventoryItemFilterExpressionIndex, -1, size - 1);
        editor.inventoryItemFilterExpressionCardScrollOffset = MathHelper.clamp(
                editor.inventoryItemFilterExpressionCardScrollOffset, 0, getMaxScroll(size));
    }
}
