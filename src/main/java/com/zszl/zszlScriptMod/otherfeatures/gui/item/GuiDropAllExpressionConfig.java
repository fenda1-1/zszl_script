package com.zszl.zszlScriptMod.otherfeatures.gui.item;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.path.InventoryItemFilterExpressionEngine;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuiDropAllExpressionConfig extends ThemedGuiScreen {
    private static final int BTN_ADD = 1;
    private static final int BTN_EDIT = 2;
    private static final int BTN_DELETE = 3;
    private static final int BTN_MOVE_UP = 4;
    private static final int BTN_MOVE_DOWN = 5;
    private static final int BTN_SAVE = 6;
    private static final int BTN_BACK = 7;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 4;

    private final GuiScreen parentScreen;
    private final List<String> expressions = new ArrayList<>();

    private GuiButton addButton;
    private GuiButton editButton;
    private GuiButton deleteButton;
    private GuiButton moveUpButton;
    private GuiButton moveDownButton;
    private GuiButton saveButton;
    private GuiButton backButton;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private String statusMessage = "";
    private int statusColor = 0xFFB6C5D6;
    private int statusTicks = 0;

    public GuiDropAllExpressionConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.expressions.addAll(ItemFeatureManager.getDropAllItemFilterExpressions());
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        this.panelWidth = Math.min(560, Math.max(300, this.width - 12));
        this.panelHeight = Math.min(420, Math.max(240, this.height - 8));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        int contentX = this.panelX + 14;
        int contentWidth = this.panelWidth - 28;
        int toolbarY = this.panelY + 52;
        int footerY = this.panelY + this.panelHeight - 28;
        int gap = 6;

        this.addButton = new ThemedButton(BTN_ADD, 0, 0, 72, 20, "新增");
        this.editButton = new ThemedButton(BTN_EDIT, 0, 0, 72, 20, "编辑");
        this.deleteButton = new ThemedButton(BTN_DELETE, 0, 0, 72, 20, "删除");
        this.moveUpButton = new ThemedButton(BTN_MOVE_UP, 0, 0, 72, 20, "上移");
        this.moveDownButton = new ThemedButton(BTN_MOVE_DOWN, 0, 0, 72, 20, "下移");
        this.saveButton = new ThemedButton(BTN_SAVE, 0, footerY, 92, 20, "§a保存");
        this.backButton = new ThemedButton(BTN_BACK, 0, footerY, 92, 20, "返回");

        if (contentWidth >= 390) {
            int buttonWidth = Math.max(58, (contentWidth - gap * 4) / 5);
            layoutButton(this.addButton, contentX, toolbarY, buttonWidth);
            layoutButton(this.editButton, contentX + (buttonWidth + gap), toolbarY, buttonWidth);
            layoutButton(this.deleteButton, contentX + (buttonWidth + gap) * 2, toolbarY, buttonWidth);
            layoutButton(this.moveUpButton, contentX + (buttonWidth + gap) * 3, toolbarY, buttonWidth);
            layoutButton(this.moveDownButton, contentX + (buttonWidth + gap) * 4, toolbarY,
                    Math.max(58, contentWidth - buttonWidth * 4 - gap * 4));
            this.listY = toolbarY + 30;
        } else {
            int topWidth = Math.max(64, (contentWidth - gap * 2) / 3);
            int bottomWidth = Math.max(88, (contentWidth - gap) / 2);
            layoutButton(this.addButton, contentX, toolbarY, topWidth);
            layoutButton(this.editButton, contentX + topWidth + gap, toolbarY, topWidth);
            layoutButton(this.deleteButton, contentX + (topWidth + gap) * 2, toolbarY,
                    Math.max(64, contentWidth - topWidth * 2 - gap * 2));
            layoutButton(this.moveUpButton, contentX, toolbarY + 26, bottomWidth);
            layoutButton(this.moveDownButton, contentX + bottomWidth + gap, toolbarY + 26,
                    Math.max(88, contentWidth - bottomWidth - gap));
            this.listY = toolbarY + 56;
        }

        int footerButtonWidth = Math.max(80, (contentWidth - gap) / 2);
        this.saveButton.x = contentX;
        this.saveButton.width = footerButtonWidth;
        this.backButton.x = contentX + footerButtonWidth + gap;
        this.backButton.width = Math.max(80, contentWidth - footerButtonWidth - gap);

        this.listX = contentX;
        this.listWidth = contentWidth;
        this.listHeight = Math.max(90, footerY - this.listY - 10);

        this.buttonList.add(this.addButton);
        this.buttonList.add(this.editButton);
        this.buttonList.add(this.deleteButton);
        this.buttonList.add(this.moveUpButton);
        this.buttonList.add(this.moveDownButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.backButton);

        clampSelectionAndScroll();
        updateButtonState();
    }

    private void layoutButton(GuiButton button, int x, int y, int width) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = 20;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        if (this.statusTicks > 0) {
            this.statusTicks--;
            if (this.statusTicks == 0) {
                this.statusMessage = "";
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) {
            return;
        }
        switch (button.id) {
        case BTN_ADD:
            openExpressionInput(-1);
            return;
        case BTN_EDIT:
            if (hasSelection()) {
                openExpressionInput(this.selectedIndex);
            }
            return;
        case BTN_DELETE:
            if (hasSelection()) {
                this.expressions.remove(this.selectedIndex);
                if (this.selectedIndex >= this.expressions.size()) {
                    this.selectedIndex = this.expressions.size() - 1;
                }
                setInfo("已删除表达式，记得保存。");
                clampSelectionAndScroll();
                updateButtonState();
            }
            return;
        case BTN_MOVE_UP:
            if (this.selectedIndex > 0 && this.selectedIndex < this.expressions.size()) {
                Collections.swap(this.expressions, this.selectedIndex, this.selectedIndex - 1);
                this.selectedIndex--;
                clampSelectionAndScroll();
                updateButtonState();
            }
            return;
        case BTN_MOVE_DOWN:
            if (this.selectedIndex >= 0 && this.selectedIndex < this.expressions.size() - 1) {
                Collections.swap(this.expressions, this.selectedIndex, this.selectedIndex + 1);
                this.selectedIndex++;
                clampSelectionAndScroll();
                updateButtonState();
            }
            return;
        case BTN_SAVE:
            if (validateAllExpressions()) {
                ItemFeatureManager.setDropAllItemFilterExpressions(this.expressions);
                this.mc.setScreen(this.parentScreen);
            }
            return;
        case BTN_BACK:
            this.mc.setScreen(this.parentScreen);
            return;
        default:
            break;
        }
    }

    private void openExpressionInput(final int editIndex) {
        final boolean editing = editIndex >= 0 && editIndex < this.expressions.size();
        String title = editing ? "编辑物品过滤表达式" : "新增物品过滤表达式";
        String initial = editing ? this.expressions.get(editIndex) : "";
        this.mc.setScreen(new GuiItemFilterExpressionEditor(this, title, initial, value -> {
            String expression = value == null ? "" : value.trim();
            if (expression.isEmpty()) {
                setError("表达式不能为空。");
                this.mc.setScreen(this);
                return;
            }
            try {
                InventoryItemFilterExpressionEngine.validate(expression);
            } catch (RuntimeException e) {
                setError("表达式无效: " + safeMessage(e));
                this.mc.setScreen(this);
                return;
            }

            if (editing) {
                this.expressions.set(editIndex, expression);
                this.selectedIndex = editIndex;
            } else {
                this.expressions.add(expression);
                this.selectedIndex = this.expressions.size() - 1;
            }
            setInfo("已更新表达式，记得保存。");
            clampSelectionAndScroll();
            updateButtonState();
            this.mc.setScreen(this);
        }));
    }

    private boolean validateAllExpressions() {
        for (int i = 0; i < this.expressions.size(); i++) {
            String expression = this.expressions.get(i);
            try {
                InventoryItemFilterExpressionEngine.validate(expression);
            } catch (RuntimeException e) {
                this.selectedIndex = i;
                clampSelectionAndScroll();
                updateButtonState();
                setError("第 " + (i + 1) + " 条表达式无效: " + safeMessage(e));
                return false;
            }
        }
        return true;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }
        if (mouseButton == 0 && isInsideList(mouseX, mouseY)) {
            int index = getExpressionIndexAt(mouseY);
            this.selectedIndex = index >= 0 && index < this.expressions.size() ? index : -1;
            updateButtonState();
            return true;
        }
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && isInsideList(mouseX, mouseY)) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                this.scrollOffset = dWheel > 0
                        ? Math.max(0, this.scrollOffset - 1)
                        : Math.min(maxScroll, this.scrollOffset + 1);
            }
            return;
        }

        int button = Mouse.getEventButton();
        if (button == -1) {
            return;
        }

        if (Mouse.getEventButtonState()) {
            handleMousePressed(mouseX, mouseY, button);
        } else {
            mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "丢弃所有表达式", this.fontRenderer);

        String summary = "§7表达式: §f" + this.expressions.size()
                + " §7| 扔出间隔: §f" + ItemFeatureManager.getDropAllDelayTicks() + " tick";
        drawString(this.fontRenderer, summary, this.panelX + 14, this.panelY + 28, 0xFFFFFFFF);
        if (!this.statusMessage.isEmpty()) {
            drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(this.statusMessage, this.panelWidth - 28),
                    this.panelX + 14, this.panelY + 40, this.statusColor);
        } else {
            drawString(this.fontRenderer, "§7语法与动作库“条件执行-背包物品”一致。", this.panelX + 14,
                    this.panelY + 40, 0xFFB6C5D6);
        }

        drawExpressionList(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    private void drawExpressionList(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(this.listX, this.listY, this.listWidth, this.listHeight, false, true);
        Gui.drawRect(this.listX + 1, this.listY + 1, this.listX + this.listWidth - 1,
                this.listY + this.listHeight - 1, 0x2A101820);

        if (this.expressions.isEmpty()) {
            GuiTheme.drawEmptyState(this.listX + this.listWidth / 2, this.listY + this.listHeight / 2 - 8,
                    "尚未配置丢弃表达式", this.fontRenderer);
            return;
        }

        int visibleRows = getVisibleRowCount();
        int cardX = this.listX + 5;
        int cardWidth = this.listWidth - 16;
        int rowStride = ROW_HEIGHT + ROW_GAP;
        for (int row = 0; row < visibleRows; row++) {
            int expressionIndex = this.scrollOffset + row;
            if (expressionIndex >= this.expressions.size()) {
                break;
            }
            int cardY = this.listY + 5 + row * rowStride;
            boolean selected = expressionIndex == this.selectedIndex;
            boolean hovered = isInside(mouseX, mouseY, cardX, cardY, cardWidth, ROW_HEIGHT);
            int border = selected ? 0xFF7AD9FF : (hovered ? 0xFF5F8FAE : 0xFF3D586B);
            int fill = selected ? 0xAA244053 : (hovered ? 0x8A22313E : 0x6A18232C);
            Gui.drawRect(cardX - 1, cardY - 1, cardX + cardWidth + 1, cardY + ROW_HEIGHT + 1, border);
            Gui.drawRect(cardX, cardY, cardX + cardWidth, cardY + ROW_HEIGHT, fill);
            Gui.drawRect(cardX, cardY, cardX + 3, cardY + ROW_HEIGHT, selected ? 0xFF56B6E8 : 0xFF4C6E84);
            drawString(this.fontRenderer, "#" + (expressionIndex + 1), cardX + 8, cardY + 6,
                    selected ? 0xFFFFFFFF : 0xFFD8E5F1);

            List<String> lines = this.fontRenderer.listFormattedStringToWidth(this.expressions.get(expressionIndex),
                    Math.max(60, cardWidth - 42));
            int lineY = cardY + 6;
            for (int i = 0; i < lines.size() && i < 2; i++) {
                drawString(this.fontRenderer, lines.get(i), cardX + 34, lineY, 0xFFF1F7FC);
                lineY += 10;
            }
        }

        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int visibleRowsForThumb = Math.max(1, visibleRows);
            int thumbHeight = Math.max(12,
                    (int) ((visibleRowsForThumb / (float) this.expressions.size()) * (this.listHeight - 4)));
            int trackHeight = Math.max(1, (this.listHeight - 4) - thumbHeight);
            int thumbY = this.listY + 2 + (int) ((this.scrollOffset / (float) maxScroll) * trackHeight);
            GuiTheme.drawScrollbar(this.listX + this.listWidth - 7, this.listY + 2, 4,
                    this.listHeight - 4, thumbY, thumbHeight);
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        if (isMouseOverButton(mouseX, mouseY, this.addButton)) {
            drawHoveringText(Arrays.asList("§e新增表达式",
                    "§7示例: nameContains(\"腐肉\")",
                    "§7示例: registry == \"minecraft:stone\"",
                    "§7示例: count >= 32",
                    "§7示例: NBT(\"display\")"), mouseX, mouseY);
        } else if (isMouseOverButton(mouseX, mouseY, this.saveButton)) {
            drawHoveringText(Arrays.asList("§a保存",
                    "§7保存后，背包中命中任意表达式的物品会按间隔丢弃。"), mouseX, mouseY);
        } else {
            int index = getExpressionIndexAt(mouseY);
            if (isInsideList(mouseX, mouseY) && index >= 0 && index < this.expressions.size()) {
                drawHoveringText(Arrays.asList("§e表达式 #" + (index + 1),
                        "§f" + this.expressions.get(index),
                        "§7左键选择，再使用编辑/删除/上下移动。"), mouseX, mouseY);
            }
        }
    }

    private void updateButtonState() {
        boolean hasSelection = hasSelection();
        if (this.editButton != null) {
            this.editButton.enabled = hasSelection;
        }
        if (this.deleteButton != null) {
            this.deleteButton.enabled = hasSelection;
        }
        if (this.moveUpButton != null) {
            this.moveUpButton.enabled = hasSelection && this.selectedIndex > 0;
        }
        if (this.moveDownButton != null) {
            this.moveDownButton.enabled = hasSelection && this.selectedIndex < this.expressions.size() - 1;
        }
    }

    private boolean hasSelection() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.expressions.size();
    }

    private int getExpressionIndexAt(int mouseY) {
        if (this.expressions.isEmpty()) {
            return -1;
        }
        int localY = mouseY - this.listY - 5;
        if (localY < 0) {
            return -1;
        }
        int rowStride = ROW_HEIGHT + ROW_GAP;
        int row = localY / rowStride;
        int rowY = localY % rowStride;
        if (rowY >= ROW_HEIGHT || row >= getVisibleRowCount()) {
            return -1;
        }
        return this.scrollOffset + row;
    }

    private int getVisibleRowCount() {
        return Math.max(1, (this.listHeight - 8) / (ROW_HEIGHT + ROW_GAP));
    }

    private int getMaxScroll() {
        return Math.max(0, this.expressions.size() - getVisibleRowCount());
    }

    private void clampSelectionAndScroll() {
        if (this.expressions.isEmpty()) {
            this.selectedIndex = -1;
            this.scrollOffset = 0;
            return;
        }
        if (this.selectedIndex >= this.expressions.size()) {
            this.selectedIndex = this.expressions.size() - 1;
        }
        if (this.selectedIndex < -1) {
            this.selectedIndex = -1;
        }
        int maxScroll = getMaxScroll();
        this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset));
        if (this.selectedIndex >= 0 && this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        }
        int visibleRows = getVisibleRowCount();
        if (this.selectedIndex >= 0 && this.selectedIndex >= this.scrollOffset + visibleRows) {
            this.scrollOffset = Math.min(maxScroll, this.selectedIndex - visibleRows + 1);
        }
    }

    private boolean isInsideList(int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, this.listX, this.listY, this.listWidth, this.listHeight);
    }

    private boolean handleButtonActivation(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : this.buttonList) {
            if (button == null || !button.visible || !button.enabled) {
                continue;
            }
            if (!isInside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
                continue;
            }
            button.playPressSound(this.mc.getSoundManager());
            actionPerformed(button);
            return true;
        }
        return false;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void setInfo(String message) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = 0xFFB6C5D6;
        this.statusTicks = 120;
    }

    private void setError(String message) {
        this.statusMessage = message == null ? "" : "§c" + message;
        this.statusColor = 0xFFFFB7B7;
        this.statusTicks = 200;
    }

    private static String safeMessage(RuntimeException e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "未知错误";
        }
        return e.getMessage().trim();
    }
}
