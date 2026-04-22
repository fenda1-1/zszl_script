package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiElementInspector;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiInspectionManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GuiGuiInspectorManager extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int historyX;
    private int historyY;
    private int historyW;
    private int historyH;
    private int detailX;
    private int detailY;
    private int detailW;
    private int detailH;
    private int detailInfoH;
    private int elementListY;
    private int elementListH;

    private int historyScroll = 0;
    private int historyMaxScroll = 0;
    private int elementScroll = 0;
    private int elementMaxScroll = 0;
    private int selectedSnapshotIndex = -1;
    private int selectedElementIndex = -1;

    private static final int BTN_TOGGLE_CAPTURE = 1;
    private static final int BTN_CLEAR = 2;
    private static final int BTN_COPY_PATH = 3;
    private static final int BTN_BACK = 4;

    private static final int HISTORY_ROW_HEIGHT = 34;
    private static final int ELEMENT_ROW_HEIGHT = 24;

    private float uiScale() {
        float sx = this.width / 980.0f;
        float sy = this.height / 620.0f;
        return Math.max(0.68f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    public GuiGuiInspectorManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        recalcLayout();

        int buttonY = panelY + panelH - s(26);
        int gap = s(8);
        int buttonCount = 4;
        int availableWidth = panelW - s(20);
        int buttonWidth = Math.max(s(56), Math.min(s(92), (availableWidth - gap * (buttonCount - 1)) / buttonCount));
        int totalButtonWidth = buttonWidth * buttonCount + gap * (buttonCount - 1);
        int startX = panelX + Math.max(s(10), (panelW - totalButtonWidth) / 2);

        this.buttonList.add(new ThemedButton(BTN_TOGGLE_CAPTURE, startX, buttonY, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap), buttonY, buttonWidth, 20,
                "清空历史"));
        this.buttonList.add(new ThemedButton(BTN_COPY_PATH, startX + 2 * (buttonWidth + gap), buttonY, buttonWidth, 20,
                "复制路径"));
        this.buttonList.add(new ThemedButton(BTN_BACK, startX + 3 * (buttonWidth + gap), buttonY, buttonWidth, 20,
                "返回"));

        syncSelectionBounds();
        refreshButtons();
    }

    private void recalcLayout() {
        int margin = s(10);
        panelW = Math.max(s(340), Math.min(s(980), this.width - margin * 2));
        panelH = Math.max(s(250), Math.min(s(620), this.height - margin * 2));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int innerPadding = s(10);
        int columnGap = s(10);
        historyX = panelX + innerPadding;
        historyY = panelY + s(30);
        historyW = Math.max(s(150), Math.min(s(250), panelW / 3));
        historyH = Math.max(s(120), panelH - s(66));

        detailX = historyX + historyW + columnGap;
        detailY = historyY;
        detailW = Math.max(s(150), panelX + panelW - detailX - innerPadding);
        detailH = historyH;
        detailInfoH = Math.max(s(78), Math.min(s(118), detailH / 3));
        elementListY = detailY + detailInfoH + s(8);
        elementListH = Math.max(s(54), detailH - detailInfoH - s(8));
    }

    private List<GuiInspectionManager.CapturedGuiSnapshot> getSnapshots() {
        return GuiInspectionManager.getHistory();
    }

    private GuiInspectionManager.CapturedGuiSnapshot getSelectedSnapshot() {
        List<GuiInspectionManager.CapturedGuiSnapshot> snapshots = getSnapshots();
        if (selectedSnapshotIndex < 0 || selectedSnapshotIndex >= snapshots.size()) {
            return null;
        }
        return snapshots.get(selectedSnapshotIndex);
    }

    private List<GuiElementInspector.GuiElementInfo> getSelectedElements() {
        GuiInspectionManager.CapturedGuiSnapshot snapshot = getSelectedSnapshot();
        return snapshot == null ? Collections.<GuiElementInspector.GuiElementInfo>emptyList() : snapshot.getElements();
    }

    private GuiElementInspector.GuiElementInfo getSelectedElement() {
        List<GuiElementInspector.GuiElementInfo> elements = getSelectedElements();
        if (selectedElementIndex < 0 || selectedElementIndex >= elements.size()) {
            return null;
        }
        return elements.get(selectedElementIndex);
    }

    private void syncSelectionBounds() {
        List<GuiInspectionManager.CapturedGuiSnapshot> snapshots = getSnapshots();
        if (snapshots.isEmpty()) {
            selectedSnapshotIndex = -1;
            selectedElementIndex = -1;
            historyScroll = 0;
            elementScroll = 0;
            return;
        }
        if (selectedSnapshotIndex < 0 || selectedSnapshotIndex >= snapshots.size()) {
            selectedSnapshotIndex = 0;
            selectedElementIndex = -1;
        }

        List<GuiElementInspector.GuiElementInfo> elements = getSelectedElements();
        if (elements.isEmpty()) {
            selectedElementIndex = -1;
            elementScroll = 0;
        } else if (selectedElementIndex < 0 || selectedElementIndex >= elements.size()) {
            selectedElementIndex = 0;
        }
        clampScrolls();
    }

    private void clampScrolls() {
        historyMaxScroll = Math.max(0, getSnapshots().size() - Math.max(1, historyH / HISTORY_ROW_HEIGHT));
        historyScroll = Math.max(0, Math.min(historyScroll, historyMaxScroll));
        elementMaxScroll = Math.max(0, getSelectedElements().size() - Math.max(1, elementListH / ELEMENT_ROW_HEIGHT));
        elementScroll = Math.max(0, Math.min(elementScroll, elementMaxScroll));
    }

    private void refreshButtons() {
        GuiButton captureButton = this.buttonList.stream()
                .filter(button -> button.id == BTN_TOGGLE_CAPTURE)
                .findFirst()
                .orElse(null);
        if (captureButton != null) {
            captureButton.displayString = GuiInspectionManager.isCaptureEnabled() ? "停止捕获" : "开启捕获";
        }

        GuiButton copyButton = this.buttonList.stream()
                .filter(button -> button.id == BTN_COPY_PATH)
                .findFirst()
                .orElse(null);
        if (copyButton != null) {
            copyButton.enabled = getSelectedElement() != null;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_TOGGLE_CAPTURE:
                GuiInspectionManager.toggleCaptureEnabled();
                refreshButtons();
                break;
            case BTN_CLEAR:
                GuiInspectionManager.clearHistory();
                syncSelectionBounds();
                refreshButtons();
                break;
            case BTN_COPY_PATH:
                GuiElementInspector.GuiElementInfo element = getSelectedElement();
                if (element != null) {
                    setClipboardString(element.getPath());
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.GREEN + "已复制路径: " + element.getPath()));
                    }
                }
                break;
            case BTN_BACK:
                mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "GUI识别管理器", this.fontRenderer);

        syncSelectionBounds();
        refreshButtons();

        drawHistoryPanel(mouseX, mouseY);
        drawDetailPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHistoryPanel(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(historyX, historyY, historyW, historyH, false, true);
        drawString(fontRenderer, "捕获历史", historyX + 6, historyY - 10, 0xFFEAF7FF);

        List<GuiInspectionManager.CapturedGuiSnapshot> snapshots = getSnapshots();
        int visibleRows = Math.max(1, historyH / HISTORY_ROW_HEIGHT);
        clampScrolls();

        for (int i = 0; i < visibleRows; i++) {
            int index = i + historyScroll;
            if (index >= snapshots.size()) {
                break;
            }
            int rowY = historyY + i * HISTORY_ROW_HEIGHT;
            GuiInspectionManager.CapturedGuiSnapshot snapshot = snapshots.get(index);
            boolean hovered = isInside(mouseX, mouseY, historyX, rowY, historyW, HISTORY_ROW_HEIGHT);
            boolean selected = index == selectedSnapshotIndex;
            GuiTheme.drawButtonFrameSafe(historyX + 2, rowY + 1, historyW - 10, HISTORY_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            String line1 = snapshot.getTimestampText() + "  " + snapshot.getScreenSimpleName();
            String line2 = (snapshot.getTitle().trim().isEmpty() ? "(无标题)" : snapshot.getTitle())
                    + " | wnd=" + snapshot.getWindowId();
            drawString(fontRenderer, fontRenderer.trimStringToWidth(line1, historyW - 18), historyX + 8, rowY + 6,
                    0xFFFFFFFF);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(line2, historyW - 18), historyX + 8, rowY + 18,
                    0xFF9FB0C0);
        }

        if (snapshots.isEmpty()) {
            drawCenteredString(fontRenderer, "暂无记录", historyX + historyW / 2, historyY + 18, 0xFF9FB0C0);
            drawCenteredString(fontRenderer, "先开启捕获，再打开任意容器/GUI", historyX + historyW / 2, historyY + 32, 0xFF7F8FA4);
        }

        drawScrollbarIfNeeded(historyX + historyW - 6, historyY, historyH, historyScroll, historyMaxScroll,
                snapshots.size(), visibleRows, HISTORY_ROW_HEIGHT);
    }

    private void drawDetailPanel(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(detailX, detailY, detailW, detailInfoH, false, true);
        drawString(fontRenderer, "快照详情", detailX + 6, detailY - 10, 0xFFEAF7FF);

        GuiInspectionManager.CapturedGuiSnapshot snapshot = getSelectedSnapshot();
        if (snapshot == null) {
            drawString(fontRenderer, "未选择任何快照", detailX + 8, detailY + 8, 0xFF9FB0C0);
            return;
        }

        drawString(fontRenderer, "屏幕: " + snapshot.getScreenSimpleName(), detailX + 8, detailY + 8, 0xFFFFFFFF);
        drawString(fontRenderer, "类名: " + fontRenderer.trimStringToWidth(snapshot.getScreenClassName(), detailW - 56),
                detailX + 8, detailY + 22, 0xFFB9C7D4);
        drawString(fontRenderer, "标题: " + (snapshot.getTitle().trim().isEmpty() ? "(无)" : snapshot.getTitle()),
                detailX + 8, detailY + 36, 0xFFB9C7D4);
        drawString(fontRenderer,
                "windowId=" + snapshot.getWindowId()
                        + "  total=" + snapshot.getTotalSlots()
                        + "  container=" + snapshot.getContainerSlots()
                        + "  player=" + snapshot.getPlayerInventorySlots(),
                detailX + 8, detailY + 50, 0xFFB9C7D4);
        drawString(fontRenderer, "元素数: " + snapshot.getElements().size(), detailX + 8, detailY + 64, 0xFFB9C7D4);

        GuiElementInspector.GuiElementInfo selectedElement = getSelectedElement();
        if (selectedElement != null) {
            drawString(fontRenderer,
                    fontRenderer.trimStringToWidth("当前路径: " + selectedElement.getPath(), detailW - 16),
                    detailX + 8, detailY + 82, 0xFFEAF7FF);
            drawString(fontRenderer,
                    fontRenderer.trimStringToWidth(
                            "文本: " + (selectedElement.getText().trim().isEmpty() ? "(空)" : selectedElement.getText()),
                            detailW - 16),
                    detailX + 8, detailY + 96, 0xFF9FDFFF);
        } else {
            drawString(fontRenderer, "当前路径: (未选择元素)", detailX + 8, detailY + 82, 0xFFEAF7FF);
        }

        GuiTheme.drawInputFrameSafe(detailX, elementListY, detailW, elementListH, false, true);
        drawString(fontRenderer, "元素列表", detailX + 6, elementListY - 10, 0xFFEAF7FF);
        drawElementList(mouseX, mouseY, snapshot.getElements());
    }

    private void drawElementList(int mouseX, int mouseY, List<GuiElementInspector.GuiElementInfo> elements) {
        int visibleRows = Math.max(1, elementListH / ELEMENT_ROW_HEIGHT);
        clampScrolls();

        for (int i = 0; i < visibleRows; i++) {
            int index = i + elementScroll;
            if (index >= elements.size()) {
                break;
            }
            int rowY = elementListY + i * ELEMENT_ROW_HEIGHT;
            GuiElementInspector.GuiElementInfo element = elements.get(index);
            boolean hovered = isInside(mouseX, mouseY, detailX, rowY, detailW, ELEMENT_ROW_HEIGHT);
            boolean selected = index == selectedElementIndex;
            GuiTheme.drawButtonFrameSafe(detailX + 2, rowY + 1, detailW - 10, ELEMENT_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String text = element.getText().trim().isEmpty() ? "(空)" : element.getText();
            String line = "[" + element.getType().name() + "] "
                    + element.getPath()
                    + " | text=" + text;
            drawString(fontRenderer, fontRenderer.trimStringToWidth(line, detailW - 18), detailX + 8, rowY + 8,
                    0xFFFFFFFF);
        }

        if (elements.isEmpty()) {
            drawCenteredString(fontRenderer, "当前快照没有可展示元素", detailX + detailW / 2, elementListY + 18, 0xFF9FB0C0);
        }

        drawScrollbarIfNeeded(detailX + detailW - 6, elementListY, elementListH, elementScroll, elementMaxScroll,
                elements.size(), visibleRows, ELEMENT_ROW_HEIGHT);
    }

    private void drawScrollbarIfNeeded(int x, int y, int height, int scrollOffset, int maxScroll,
            int totalItems, int visibleItems, int rowHeight) {
        if (maxScroll <= 0 || totalItems <= 0) {
            return;
        }
        int totalHeight = totalItems * rowHeight;
        int thumbHeight = Math.max(16, (int) ((height / (float) Math.max(height, totalHeight)) * height));
        int track = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
        GuiTheme.drawScrollbar(x, y, 4, height, thumbY, thumbHeight);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        int historyVisible = Math.max(1, historyH / HISTORY_ROW_HEIGHT);
        for (int i = 0; i < historyVisible; i++) {
            int index = i + historyScroll;
            if (index >= getSnapshots().size()) {
                break;
            }
            int rowY = historyY + i * HISTORY_ROW_HEIGHT;
            if (isInside(mouseX, mouseY, historyX, rowY, historyW, HISTORY_ROW_HEIGHT)) {
                selectedSnapshotIndex = index;
                selectedElementIndex = getSelectedElements().isEmpty() ? -1 : 0;
                elementScroll = 0;
                refreshButtons();
                return;
            }
        }

        int elementVisible = Math.max(1, elementListH / ELEMENT_ROW_HEIGHT);
        for (int i = 0; i < elementVisible; i++) {
            int index = i + elementScroll;
            if (index >= getSelectedElements().size()) {
                break;
            }
            int rowY = elementListY + i * ELEMENT_ROW_HEIGHT;
            if (isInside(mouseX, mouseY, detailX, rowY, detailW, ELEMENT_ROW_HEIGHT)) {
                selectedElementIndex = index;
                refreshButtons();
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (isInside(mouseX, mouseY, historyX, historyY, historyW, historyH) && historyMaxScroll > 0) {
            historyScroll = dWheel > 0 ? Math.max(0, historyScroll - 1) : Math.min(historyMaxScroll, historyScroll + 1);
            return;
        }
        if (isInside(mouseX, mouseY, detailX, elementListY, detailW, elementListH) && elementMaxScroll > 0) {
            elementScroll = dWheel > 0 ? Math.max(0, elementScroll - 1) : Math.min(elementMaxScroll, elementScroll + 1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}








