package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.utils.CapturedIdSmartRuleGenerator;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.mojang.blaze3d.platform.GlStateManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiCapturedIdSmartGenerator extends ThemedGuiScreen {

    private static final int BTN_BACK = 1;
    private static final int BTN_ADD_SAMPLE = 2;
    private static final int BTN_CLEAR_SAMPLES = 3;
    private static final int BTN_IMPORT_ALL = 4;
    private static final int BTN_COPY_ALL = 5;
    private static final int BTN_CYCLE_DIRECTION = 6;
    private static final int BTN_LOAD_SELECTED = 7;
    private static final int BTN_IMPORT_SELECTED = 8;

    private static final int SAMPLE_CARD_HEIGHT = 26;
    private static final int SAMPLE_CARD_GAP = 4;
    private static final int PROPOSAL_CARD_HEIGHT = 76;
    private static final int PROPOSAL_CARD_GAP = 6;
    private static final int SECTION_PADDING = 10;
    private static final int SAMPLE_LIST_MIN_HEIGHT = SAMPLE_CARD_HEIGHT * 3 + SAMPLE_CARD_GAP * 2 + 10;

    private static final String[] DIRECTION_OPTIONS = new String[] { "both", "inbound", "outbound" };
    private static final String[] DIRECTION_LABELS = new String[] { "双向", "仅入站", "仅出站" };

    private final GuiScreen parentScreen;
    private final String initialCategory;

    private static final class DraftState {
        String rulePrefix = "smart_id";
        String displayPrefix = "智能捕获";
        String channel = "";
        String category = "";
        int directionIndex = 0;
        final List<String> samples = new ArrayList<>();
    }

    private static final DraftState DRAFT = new DraftState();

    private GuiTextField rulePrefixField;
    private GuiTextField displayPrefixField;
    private GuiTextField channelField;
    private GuiTextField categoryField;
    private GuiTextField sampleInputField;
    private GuiButton addSampleButton;
    private GuiButton clearSamplesButton;
    private GuiButton loadSelectedButton;
    private GuiButton importSelectedButton;

    private final List<String> sampleEntries = new ArrayList<>();
    private final List<Rectangle> sampleRemoveBounds = new ArrayList<>();
    private final List<Rectangle> sampleCardBounds = new ArrayList<>();
    private final List<Rectangle> proposalCardBounds = new ArrayList<>();
    private final List<String> initialSamples = new ArrayList<>();

    private int directionIndex = 0;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int leftPanelX;
    private int leftPanelY;
    private int leftPanelWidth;
    private int leftPanelHeight;
    private int rightPanelX;
    private int rightPanelY;
    private int rightPanelWidth;
    private int rightPanelHeight;
    private Rectangle sampleListBounds = new Rectangle();
    private Rectangle leftContentViewportBounds = new Rectangle();
    private Rectangle resultListBounds = new Rectangle();
    private Rectangle resultScrollbarBounds = new Rectangle();
    private int leftContentScrollOffset = 0;
    private int leftContentMaxScroll = 0;
    private int sampleScrollOffset = 0;
    private int sampleMaxScroll = 0;
    private int resultScrollOffset = 0;
    private int resultMaxScroll = 0;
    private int editingSampleIndex = -1;
    private int pressedSampleIndex = -1;
    private int draggingSampleIndex = -1;
    private int sampleDropIndex = -1;
    private int selectedProposalIndex = -1;
    private boolean draggingResultScrollbar = false;
    private int leftContentBaseY = 0;
    private int sampleListBaseY = 0;
    private int sampleListHeight = SAMPLE_LIST_MIN_HEIGHT;

    private CapturedIdSmartRuleGenerator.AnalysisResult currentAnalysis = CapturedIdSmartRuleGenerator
            .analyze(new ArrayList<String>());
    private String lastAnalysisSignature = "";
    private String statusMessage = "§7默认样本为空。先在左侧输入 HEX 并点击“添加样本”，至少两组后才会生成规则";
    private int statusColor = 0xFFB8C7D9;

    public GuiCapturedIdSmartGenerator(GuiScreen parentScreen, String initialCategory) {
        this.parentScreen = parentScreen;
        this.initialCategory = initialCategory == null ? "" : initialCategory;
    }

    public GuiCapturedIdSmartGenerator(GuiScreen parentScreen, String initialCategory, List<String> initialSamples) {
        this(parentScreen, initialCategory);
        if (initialSamples != null) {
            for (String sample : initialSamples) {
                String normalized = normalizeHex(sample);
                if (!normalized.isEmpty()) {
                    this.initialSamples.add(normalized);
                }
            }
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        loadDraft();

        String existingRulePrefix = safeText(rulePrefixField, DRAFT.rulePrefix);
        String existingDisplayPrefix = safeText(displayPrefixField, DRAFT.displayPrefix);
        String existingChannel = safeText(channelField, DRAFT.channel);
        String existingCategory = safeText(categoryField, DRAFT.category.isEmpty() ? initialCategory : DRAFT.category);
        String existingSampleInput = safeText(sampleInputField, "");

        this.buttonList.clear();
        recalcLayout();

        rulePrefixField = createField(2001, leftPanelX + SECTION_PADDING, leftPanelY + 44, leftPanelWidth / 2 - 16, 18,
                existingRulePrefix);
        displayPrefixField = createField(2002, leftPanelX + leftPanelWidth / 2 + 6, leftPanelY + 44,
                leftPanelWidth / 2 - 16, 18, existingDisplayPrefix);
        channelField = createField(2003, leftPanelX + SECTION_PADDING, leftPanelY + 88, leftPanelWidth / 2 - 16, 18,
                existingChannel);
        categoryField = createField(2004, leftPanelX + leftPanelWidth / 2 + 6, leftPanelY + 88, leftPanelWidth / 2 - 16,
                18, existingCategory);

        sampleInputField = createField(2100, leftPanelX + SECTION_PADDING, leftPanelY + 152, leftPanelWidth - 120, 18,
                existingSampleInput);

        this.buttonList.add(new GuiButton(BTN_BACK, panelX + 10, panelY + panelHeight - 26, 84, 20, "返回"));
        this.buttonList.add(new GuiButton(BTN_COPY_ALL, panelX + panelWidth - 340, panelY + panelHeight - 26, 96, 20,
                "复制结果"));
        importSelectedButton = new GuiButton(BTN_IMPORT_SELECTED, panelX + panelWidth - 442, panelY + panelHeight - 26, 96,
                20, "导入选中");
        this.buttonList.add(new GuiButton(BTN_IMPORT_ALL, panelX + panelWidth - 238, panelY + panelHeight - 26, 106, 20,
                "批量导入"));
        this.buttonList.add(new GuiButton(BTN_CYCLE_DIRECTION, panelX + panelWidth - 126, panelY + panelHeight - 26, 116,
                20, buildDirectionButtonText()));
        this.buttonList.add(importSelectedButton);

        addSampleButton = new GuiButton(BTN_ADD_SAMPLE, 0, 0, 56, 20, getAddOrUpdateButtonText());
        clearSamplesButton = new GuiButton(BTN_CLEAR_SAMPLES, 0, 0, 56, 20, "清空");
        loadSelectedButton = new GuiButton(BTN_LOAD_SELECTED, 0, 0, 72, 20, "载入选中");
        this.buttonList.add(addSampleButton);
        this.buttonList.add(clearSamplesButton);
        this.buttonList.add(loadSelectedButton);

        sampleEntries.clear();
        sampleEntries.addAll(DRAFT.samples);
        if (!this.initialSamples.isEmpty()) {
            for (String sample : this.initialSamples) {
                if (!sampleEntries.contains(sample)) {
                    sampleEntries.add(sample);
                }
            }
        }
        directionIndex = Math.max(0, Math.min(DIRECTION_OPTIONS.length - 1, DRAFT.directionIndex));

        layoutLeftControls();
        updateSampleActionButtons();
        analyzeIfNeeded(true);
    }

    private void recalcLayout() {
        panelWidth = Math.min(1180, this.width - 20);
        panelHeight = Math.min(680, this.height - 20);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        contentTop = panelY + 48;

        int columnGap = 12;
        int availableWidth = panelWidth - 24;
        leftPanelWidth = Math.max(320, availableWidth * 44 / 100);
        rightPanelWidth = availableWidth - leftPanelWidth - columnGap;

        leftPanelX = panelX + 12;
        leftPanelY = contentTop;
        leftPanelHeight = panelHeight - 86;
        leftContentBaseY = leftPanelY + 44;
        sampleListBaseY = leftPanelY + 234;
        sampleListHeight = Math.max(SAMPLE_LIST_MIN_HEIGHT, leftPanelHeight / 3);

        rightPanelX = leftPanelX + leftPanelWidth + columnGap;
        rightPanelY = contentTop;
        rightPanelHeight = leftPanelHeight;
    }

    private void layoutLeftControls() {
        int halfWidth = leftPanelWidth / 2 - 16;
        int currentY = leftContentBaseY - leftContentScrollOffset;
        if (rulePrefixField != null) {
            rulePrefixField.x = leftPanelX + SECTION_PADDING;
            rulePrefixField.y = currentY;
            rulePrefixField.width = halfWidth;
        }
        if (displayPrefixField != null) {
            displayPrefixField.x = leftPanelX + leftPanelWidth / 2 + 6;
            displayPrefixField.y = currentY;
            displayPrefixField.width = halfWidth;
        }
        currentY += 44;
        if (channelField != null) {
            channelField.x = leftPanelX + SECTION_PADDING;
            channelField.y = currentY;
            channelField.width = halfWidth;
        }
        if (categoryField != null) {
            categoryField.x = leftPanelX + leftPanelWidth / 2 + 6;
            categoryField.y = currentY;
            categoryField.width = halfWidth;
        }

        int inputY = leftPanelY + 172 - leftContentScrollOffset;
        if (sampleInputField != null) {
            sampleInputField.x = leftPanelX + SECTION_PADDING;
            sampleInputField.y = inputY;
            sampleInputField.width = leftPanelWidth - SECTION_PADDING * 2;
        }
        int buttonY = inputY + 26;
        if (addSampleButton != null) {
            addSampleButton.x = leftPanelX + SECTION_PADDING;
            addSampleButton.y = buttonY;
        }
        if (clearSamplesButton != null) {
            clearSamplesButton.x = leftPanelX + SECTION_PADDING + 62;
            clearSamplesButton.y = buttonY;
        }
        if (loadSelectedButton != null) {
            loadSelectedButton.x = leftPanelX + SECTION_PADDING + 124;
            loadSelectedButton.y = buttonY;
        }

        leftContentViewportBounds = new Rectangle(leftPanelX + 4, leftPanelY + 36, leftPanelWidth - 10,
                Math.max(40, leftPanelHeight - 40));

        sampleListBounds = new Rectangle(leftPanelX + 10, sampleListBaseY - leftContentScrollOffset, leftPanelWidth - 20,
                sampleListHeight);

        int contentBottom = sampleListBaseY + sampleListHeight + 12;
        leftContentMaxScroll = Math.max(0, contentBottom - (leftPanelY + leftPanelHeight));
        leftContentScrollOffset = Math.max(0, Math.min(leftContentScrollOffset, leftContentMaxScroll));
        updateScrollableLeftButtonVisibility();
    }

    private GuiTextField createField(int id, int x, int y, int width, int height, String text) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, width, height);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setText(text == null ? "" : text);
        return field;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_BACK:
                mc.setScreen(parentScreen);
                return;
            case BTN_ADD_SAMPLE:
                addSampleEntry();
                return;
            case BTN_CLEAR_SAMPLES:
                sampleEntries.clear();
                sampleInputField.setText("");
                sampleScrollOffset = 0;
                analyzeIfNeeded(true);
                return;
            case BTN_COPY_ALL:
                copyAllGeneratedRules();
                return;
            case BTN_IMPORT_ALL:
                importAllGeneratedRules();
                return;
            case BTN_IMPORT_SELECTED:
                importSelectedGeneratedRule();
                return;
            case BTN_CYCLE_DIRECTION:
                directionIndex = (directionIndex + 1) % DIRECTION_OPTIONS.length;
                button.displayString = buildDirectionButtonText();
                analyzeIfNeeded(true);
                return;
            case BTN_LOAD_SELECTED:
                if (editingSampleIndex >= 0 && editingSampleIndex < sampleEntries.size()) {
                    sampleInputField.setText(sampleEntries.get(editingSampleIndex));
                }
                return;
            default:
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            if (resultScrollbarBounds.contains(mouseX, mouseY) && resultMaxScroll > 0) {
                draggingResultScrollbar = true;
                return;
            }
            for (int i = 0; i < sampleRemoveBounds.size(); i++) {
                Rectangle bounds = sampleRemoveBounds.get(i);
                if (bounds != null && bounds.contains(mouseX, mouseY) && i < sampleEntries.size()) {
                    sampleEntries.remove(i);
                    if (editingSampleIndex == i) {
                        editingSampleIndex = -1;
                        if (sampleInputField != null) {
                            sampleInputField.setText("");
                        }
                    } else if (editingSampleIndex > i) {
                        editingSampleIndex--;
                    }
                    analyzeIfNeeded(true);
                    return;
                }
            }
            for (int i = 0; i < sampleCardBounds.size(); i++) {
                Rectangle bounds = sampleCardBounds.get(i);
                if (bounds != null && bounds.contains(mouseX, mouseY) && i < sampleEntries.size()) {
                    editingSampleIndex = i;
                    pressedSampleIndex = i;
                    draggingSampleIndex = -1;
                    sampleDropIndex = i;
                    if (sampleInputField != null) {
                        sampleInputField.setText(sampleEntries.get(i));
                        sampleInputField.setFocused(true);
                    }
                    updateSampleActionButtons();
                    return;
                }
            }
            for (int i = 0; i < proposalCardBounds.size(); i++) {
                Rectangle bounds = proposalCardBounds.get(i);
                if (bounds != null && bounds.contains(mouseX, mouseY)) {
                    selectedProposalIndex = i;
                    updateSampleActionButtons();
                    return;
                }
            }
        }

        GuiTextField focusedField = null;
        for (GuiTextField field : getAllFields()) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
            if (mouseX >= field.x && mouseX < field.x + field.width && mouseY >= field.y && mouseY < field.y + field.height) {
                focusedField = field;
            }
        }
        for (GuiTextField field : getAllFields()) {
            field.setFocused(field == focusedField);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (sampleInputField != null && sampleInputField.isFocused()) {
                addSampleEntry();
                return;
            }
        }

        boolean consumed = false;
        for (GuiTextField field : getAllFields()) {
            if (field.isFocused() && field.textboxKeyTyped(typedChar, keyCode)) {
                consumed = true;
                break;
            }
        }
        if (!consumed) {
            super.keyTyped(typedChar, keyCode);
        }
        analyzeIfNeeded(false);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;

        if (sampleListBounds.contains(mouseX, mouseY)) {
            if (wheel < 0) {
                sampleScrollOffset = Math.min(sampleMaxScroll, sampleScrollOffset + 1);
            } else {
                sampleScrollOffset = Math.max(0, sampleScrollOffset - 1);
            }
            return;
        }

        if (mouseX >= leftPanelX && mouseX <= leftPanelX + leftPanelWidth
                && mouseY >= leftPanelY && mouseY <= leftPanelY + leftPanelHeight) {
            if (wheel < 0) {
                leftContentScrollOffset = Math.min(leftContentMaxScroll, leftContentScrollOffset + 12);
            } else {
                leftContentScrollOffset = Math.max(0, leftContentScrollOffset - 12);
            }
            layoutLeftControls();
            return;
        }

        if (resultListBounds.contains(mouseX, mouseY)) {
            if (wheel < 0) {
                resultScrollOffset = Math.min(resultMaxScroll,
                        resultScrollOffset + PROPOSAL_CARD_HEIGHT + PROPOSAL_CARD_GAP);
            } else {
                resultScrollOffset = Math.max(0,
                        resultScrollOffset - (PROPOSAL_CARD_HEIGHT + PROPOSAL_CARD_GAP));
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : getAllFields()) {
            field.updateCursorCounter();
        }
        analyzeIfNeeded(false);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (parentScreen instanceof GuiCapturedIdViewer) {
            ((GuiCapturedIdViewer) parentScreen).initGui();
        }
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        layoutLeftControls();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "捕获ID智能生成器", this.fontRenderer);
        GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 22, panelWidth - 20, 18, false, true);
        drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 27, statusColor);

        drawLeftPanel(mouseX, mouseY);
        drawRightPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);

        beginScissor(leftContentViewportBounds);
        for (GuiTextField field : getAllFields()) {
            if (field != null && intersectsViewport(field.x, field.y, field.width, field.height, leftContentViewportBounds)) {
                drawThemedTextField(field);
            }
        }
        endScissor();
        drawSmartGeneratorTooltip(mouseX, mouseY);
    }

    private void drawLeftPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight, panelX, panelY, panelWidth,
                panelHeight);
        GuiTheme.drawSectionTitle(leftPanelX + 8, leftPanelY + 8, "样本输入", this.fontRenderer);
        drawString(this.fontRenderer, "默认空白。先输入一组 HEX，再点右侧“添加”变成样本卡片", leftPanelX + 8, leftPanelY + 20,
                0xFF9FB2C8);

        beginScissor(leftContentViewportBounds);
        drawFieldLabel("规则名前缀", rulePrefixField.x, rulePrefixField.y - 10);
        drawFieldLabel("显示名前缀", displayPrefixField.x, displayPrefixField.y - 10);
        drawFieldLabel("频道", channelField.x, channelField.y - 10);
        drawFieldLabel("分组", categoryField.x, categoryField.y - 10);

        int sampleTitleY = leftPanelY + 124 - leftContentScrollOffset;
        drawString(this.fontRenderer, "输入框", leftPanelX + 10, sampleTitleY, 0xFFD7E6F5);
        drawString(this.fontRenderer, "至少两组样本才会生成；回车也可直接添加", leftPanelX + 10, sampleTitleY + 12,
                0xFF9FB2C8);

        int listTitleY = sampleInputField.y + 50;
        drawString(this.fontRenderer, "样本列表", leftPanelX + 10, listTitleY, 0xFFD7E6F5);
        GuiTheme.drawInputFrameSafe(sampleListBounds.x - 1, sampleListBounds.y - 1, sampleListBounds.width + 2,
                sampleListBounds.height + 2, false, true);

        sampleRemoveBounds.clear();
        sampleCardBounds.clear();
        int visibleHeight = sampleListBounds.height;
        int totalHeight = sampleEntries.isEmpty() ? 0 : sampleEntries.size() * (SAMPLE_CARD_HEIGHT + SAMPLE_CARD_GAP) - SAMPLE_CARD_GAP;
        sampleMaxScroll = Math.max(0, totalHeight - visibleHeight);
        sampleScrollOffset = Math.max(0, Math.min(sampleScrollOffset, sampleMaxScroll));

        if (sampleEntries.isEmpty()) {
            GuiTheme.drawEmptyState(sampleListBounds.x + sampleListBounds.width / 2,
                    sampleListBounds.y + sampleListBounds.height / 2, "还没有样本，先添加两组 HEX", this.fontRenderer);
            return;
        }

        int cardY = sampleListBounds.y - sampleScrollOffset;
        for (int i = 0; i < sampleEntries.size(); i++) {
            int top = cardY + i * (SAMPLE_CARD_HEIGHT + SAMPLE_CARD_GAP);
            int bottom = top + SAMPLE_CARD_HEIGHT;
            Rectangle removeBounds = new Rectangle(sampleListBounds.x + sampleListBounds.width - 28, top + 4, 18, 18);
            Rectangle cardBounds = new Rectangle(sampleListBounds.x + 4, top, sampleListBounds.width - 8, SAMPLE_CARD_HEIGHT);
            sampleRemoveBounds.add(removeBounds);
            sampleCardBounds.add(cardBounds);
            if (bottom < sampleListBounds.y || top > sampleListBounds.y + sampleListBounds.height) {
                continue;
            }

            int cardBg = i == editingSampleIndex ? 0x3A2B5A7C : (i == sampleDropIndex && draggingSampleIndex >= 0 ? 0x3A365E2B : 0x2E202A36);
            drawRect(sampleListBounds.x + 4, top, sampleListBounds.x + sampleListBounds.width - 4, bottom, cardBg);
            drawHorizontalLine(sampleListBounds.x + 4, sampleListBounds.x + sampleListBounds.width - 4, top, 0xFF4A7AA0);
            drawHorizontalLine(sampleListBounds.x + 4, sampleListBounds.x + sampleListBounds.width - 4, bottom, 0x883B5872);
            drawString(this.fontRenderer, "#" + (i + 1), sampleListBounds.x + 10, top + 9, 0xFFFFFFFF);
            drawString(this.fontRenderer, trimToWidth(sampleEntries.get(i), sampleListBounds.width - 58),
                    sampleListBounds.x + 34, top + 9, 0xFFEAF7FF);

            boolean hoveredRemove = removeBounds.contains(mouseX, mouseY);
            GuiTheme.drawButtonFrameSafe(removeBounds.x, removeBounds.y, removeBounds.width, removeBounds.height,
                    hoveredRemove ? GuiTheme.UiState.DANGER : GuiTheme.UiState.NORMAL);
            drawCenteredString(this.fontRenderer, "x", removeBounds.x + removeBounds.width / 2, removeBounds.y + 5,
                    0xFFFFFFFF);
        }

        if (draggingSampleIndex >= 0 && sampleDropIndex >= 0) {
            int lineY = sampleListBounds.y + Math.max(0, Math.min(sampleDropIndex, sampleEntries.size()))
                    * (SAMPLE_CARD_HEIGHT + SAMPLE_CARD_GAP) - sampleScrollOffset;
            drawRect(sampleListBounds.x + 6, lineY - 1, sampleListBounds.x + sampleListBounds.width - 6, lineY + 1,
                    0xFF7EC8FF);
        }

        if (sampleMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleHeight / (float) Math.max(visibleHeight, visibleHeight + sampleMaxScroll)) * visibleHeight));
            int track = Math.max(1, visibleHeight - thumbHeight);
            int thumbY = sampleListBounds.y + (int) ((sampleScrollOffset / (float) Math.max(1, sampleMaxScroll)) * track);
            GuiTheme.drawScrollbar(sampleListBounds.x + sampleListBounds.width - 6, sampleListBounds.y, 4, visibleHeight,
                    thumbY, thumbHeight);
        }

        if (leftContentMaxScroll > 0) {
            int visibleHeightPanel = leftPanelHeight - 8;
            int thumbHeight = Math.max(18,
                    (int) ((visibleHeightPanel / (float) Math.max(visibleHeightPanel, visibleHeightPanel + leftContentMaxScroll))
                            * visibleHeightPanel));
            int track = Math.max(1, visibleHeightPanel - thumbHeight);
            int thumbY = leftPanelY + 4
                    + (int) ((leftContentScrollOffset / (float) Math.max(1, leftContentMaxScroll)) * track);
            GuiTheme.drawScrollbar(leftPanelX + leftPanelWidth - 6, leftPanelY + 4, 4, visibleHeightPanel, thumbY,
                    thumbHeight);
        }
        endScissor();
    }

    private void drawRightPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight, panelX, panelY, panelWidth,
                panelHeight);
        GuiTheme.drawSectionTitle(rightPanelX + 8, rightPanelY + 8, "自动生成结果", this.fontRenderer);
        drawString(this.fontRenderer, "至少两组样本后，按变化区段自动生成多条规则", rightPanelX + 8, rightPanelY + 20, 0xFF9FB2C8);

        resultListBounds = new Rectangle(rightPanelX + 8, rightPanelY + 36, rightPanelWidth - 16, rightPanelHeight - 48);
        GuiTheme.drawInputFrameSafe(resultListBounds.x - 1, resultListBounds.y - 1, resultListBounds.width + 2,
                resultListBounds.height + 2, false, true);

        List<CapturedIdSmartRuleGenerator.Proposal> proposals = currentAnalysis.getProposals();
        proposalCardBounds.clear();
        if (proposals.isEmpty()) {
            GuiTheme.drawEmptyState(resultListBounds.x + resultListBounds.width / 2,
                    resultListBounds.y + resultListBounds.height / 2, currentAnalysis.getMessage(), this.fontRenderer);
            resultMaxScroll = 0;
            resultScrollOffset = 0;
            return;
        }

        int visibleHeight = resultListBounds.height;
        int totalHeight = proposals.size() * (PROPOSAL_CARD_HEIGHT + PROPOSAL_CARD_GAP) - PROPOSAL_CARD_GAP;
        resultMaxScroll = Math.max(0, totalHeight - visibleHeight);
        resultScrollOffset = Math.max(0, Math.min(resultScrollOffset, resultMaxScroll));

        int cardY = resultListBounds.y - resultScrollOffset;
        for (int i = 0; i < proposals.size(); i++) {
            CapturedIdSmartRuleGenerator.Proposal proposal = proposals.get(i);
            int top = cardY + i * (PROPOSAL_CARD_HEIGHT + PROPOSAL_CARD_GAP);
            int bottom = top + PROPOSAL_CARD_HEIGHT;
            proposalCardBounds.add(new Rectangle(resultListBounds.x + 4, top, resultListBounds.width - 8,
                    PROPOSAL_CARD_HEIGHT));
            if (bottom < resultListBounds.y || top > resultListBounds.y + resultListBounds.height) {
                continue;
            }

            int bg = i == selectedProposalIndex ? 0x3A2B5A7C : 0x33202A36;
            drawRect(resultListBounds.x + 4, top, resultListBounds.x + resultListBounds.width - 4, bottom, bg);
            drawHorizontalLine(resultListBounds.x + 4, resultListBounds.x + resultListBounds.width - 4, top, 0xFF4A7AA0);
            drawHorizontalLine(resultListBounds.x + 4, resultListBounds.x + resultListBounds.width - 4, bottom, 0x883B5872);
            drawVerticalLine(resultListBounds.x + 4, top, bottom, 0x883B5872);
            drawVerticalLine(resultListBounds.x + resultListBounds.width - 4, top, bottom, 0x883B5872);

            int textX = resultListBounds.x + 10;
            int textWidth = resultListBounds.width - 20;
            drawString(this.fontRenderer,
                    trimToWidth("规则 " + proposal.getIndex() + " | 捕获长度 " + proposal.getMinBytes() + "-"
                            + proposal.getMaxBytes() + " 字节", textWidth),
                    textX, top + 6, 0xFFFFFFFF);
            drawString(this.fontRenderer, trimToWidth("样本值: " + joinSampleValues(proposal.getSampleValues()), textWidth),
                    textX, top + 20, 0xFFB8CCE0);
            drawString(this.fontRenderer, trimToWidth("pattern: " + proposal.getPattern(), textWidth), textX, top + 34,
                    0xFF8CFF9E);
            drawString(this.fontRenderer, trimToWidth(proposal.getSummary(), textWidth), textX, top + 48, 0xFF9FB2C8);
        }

        if (resultMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleHeight / (float) Math.max(visibleHeight, visibleHeight + resultMaxScroll)) * visibleHeight));
            int track = Math.max(1, visibleHeight - thumbHeight);
            int thumbY = resultListBounds.y
                    + (int) ((resultScrollOffset / (float) Math.max(1, resultMaxScroll)) * track);
            resultScrollbarBounds = new Rectangle(resultListBounds.x + resultListBounds.width - 10, resultListBounds.y, 8,
                    visibleHeight);
            GuiTheme.drawScrollbar(resultScrollbarBounds.x, resultScrollbarBounds.y, resultScrollbarBounds.width, visibleHeight,
                    thumbY, thumbHeight);
        } else {
            resultScrollbarBounds = new Rectangle();
        }
    }

    private void addSampleEntry() {
        String normalized = normalizeHex(sampleInputField == null ? "" : sampleInputField.getText());
        if (normalized.isEmpty()) {
            setStatus("§c请先输入一组有效 HEX", 0xFFFF8E8E);
            return;
        }
        if (editingSampleIndex >= 0 && editingSampleIndex < sampleEntries.size()) {
            sampleEntries.set(editingSampleIndex, normalized);
            setStatus("§a已更新样本 " + (editingSampleIndex + 1), 0xFF8CFF9E);
        } else {
            sampleEntries.add(normalized);
            editingSampleIndex = sampleEntries.size() - 1;
            setStatus("§a已添加样本 " + sampleEntries.size(), 0xFF8CFF9E);
        }
        sampleInputField.setText("");
        editingSampleIndex = -1;
        updateSampleActionButtons();
        analyzeIfNeeded(true);
    }

    private void analyzeIfNeeded(boolean force) {
        String signature = buildAnalysisSignature();
        if (!force && signature.equals(lastAnalysisSignature)) {
            return;
        }
        lastAnalysisSignature = signature;
        currentAnalysis = CapturedIdSmartRuleGenerator.analyze(new ArrayList<>(sampleEntries));
        if (currentAnalysis.getProposals().isEmpty()) {
            selectedProposalIndex = -1;
        } else if (selectedProposalIndex < 0 || selectedProposalIndex >= currentAnalysis.getProposals().size()) {
            selectedProposalIndex = 0;
        }
        saveDraft();
        if (sampleEntries.size() < 2) {
            setStatus("§e至少输入两组有效 HEX 文本后才会自动生成规则", 0xFFFFD47A);
        } else if (currentAnalysis.getProposals().isEmpty()) {
            setStatus("§e" + currentAnalysis.getMessage(), 0xFFFFD47A);
        } else {
            setStatus("§a" + currentAnalysis.getMessage(), 0xFF8CFF9E);
        }
        updateSampleActionButtons();
    }

    private void importAllGeneratedRules() {
        List<CapturedIdSmartRuleGenerator.Proposal> proposals = currentAnalysis.getProposals();
        String prefix = safeText(rulePrefixField, "").trim();
        if (proposals.isEmpty()) {
            setStatus("§c当前没有可导入的生成规则", 0xFFFF8E8E);
            return;
        }
        if (prefix.isEmpty()) {
            setStatus("§c请先填写规则名前缀", 0xFFFF8E8E);
            return;
        }

        Set<String> existingNames = new LinkedHashSet<>();
        for (CapturedIdRuleManager.RuleCard card : CapturedIdRuleManager.getRuleCards()) {
            if (card != null && card.model != null && card.model.name != null) {
                existingNames.add(card.model.name.trim().toLowerCase());
            }
        }

        String category = safeText(categoryField, "").trim();
        String channel = safeText(channelField, "").trim();
        String displayPrefix = safeText(displayPrefixField, "").trim();
        String direction = DIRECTION_OPTIONS[Math.max(0, Math.min(directionIndex, DIRECTION_OPTIONS.length - 1))];
        int importedCount = 0;

        for (int i = 0; i < proposals.size(); i++) {
            CapturedIdSmartRuleGenerator.Proposal proposal = proposals.get(i);
            String ruleName = uniqueRuleName(existingNames, prefix + "_" + (i + 1));
            String displayName = displayPrefix.isEmpty() ? ruleName : (displayPrefix + " " + (i + 1));
            CapturedIdRuleManager.RuleEditModel model = CapturedIdSmartRuleGenerator.buildRuleModel(proposal, ruleName,
                    displayName, category, channel, direction);
            if (CapturedIdRuleManager.addRule(model)) {
                existingNames.add(ruleName.trim().toLowerCase());
                importedCount++;
            }
        }

        if (importedCount <= 0) {
            setStatus("§c导入失败，没有规则被创建", 0xFFFF8E8E);
            return;
        }

        setStatus("§a已导入 " + importedCount + " 条规则", 0xFF8CFF9E);
        if (parentScreen instanceof GuiCapturedIdViewer) {
            ((GuiCapturedIdViewer) parentScreen).initGui();
        }
        mc.setScreen(parentScreen);
    }

    private void importSelectedGeneratedRule() {
        List<CapturedIdSmartRuleGenerator.Proposal> proposals = currentAnalysis.getProposals();
        if (selectedProposalIndex < 0 || selectedProposalIndex >= proposals.size()) {
            setStatus("§c请先在右侧选择一条自动生成结果", 0xFFFF8E8E);
            return;
        }

        String prefix = safeText(rulePrefixField, "").trim();
        if (prefix.isEmpty()) {
            setStatus("§c请先填写规则名前缀", 0xFFFF8E8E);
            return;
        }

        Set<String> existingNames = new LinkedHashSet<>();
        for (CapturedIdRuleManager.RuleCard card : CapturedIdRuleManager.getRuleCards()) {
            if (card != null && card.model != null && card.model.name != null) {
                existingNames.add(card.model.name.trim().toLowerCase());
            }
        }

        CapturedIdSmartRuleGenerator.Proposal proposal = proposals.get(selectedProposalIndex);
        String category = safeText(categoryField, "").trim();
        String channel = safeText(channelField, "").trim();
        String displayPrefix = safeText(displayPrefixField, "").trim();
        String direction = DIRECTION_OPTIONS[Math.max(0, Math.min(directionIndex, DIRECTION_OPTIONS.length - 1))];
        String ruleName = uniqueRuleName(existingNames, prefix + "_" + proposal.getIndex());
        String displayName = displayPrefix.isEmpty() ? ruleName : (displayPrefix + " " + proposal.getIndex());
        CapturedIdRuleManager.RuleEditModel model = CapturedIdSmartRuleGenerator.buildRuleModel(proposal, ruleName,
                displayName, category, channel, direction);
        if (!CapturedIdRuleManager.addRule(model)) {
            setStatus("§c导入选中结果失败", 0xFFFF8E8E);
            return;
        }

        setStatus("§a已导入选中结果: " + ruleName, 0xFF8CFF9E);
        if (parentScreen instanceof GuiCapturedIdViewer) {
            ((GuiCapturedIdViewer) parentScreen).initGui();
        }
        mc.setScreen(parentScreen);
    }

    private void copyAllGeneratedRules() {
        List<CapturedIdSmartRuleGenerator.Proposal> proposals = currentAnalysis.getProposals();
        String prefix = safeText(rulePrefixField, "smart_id").trim();
        if (proposals.isEmpty()) {
            setStatus("§c当前没有可复制的生成结果", 0xFFFF8E8E);
            return;
        }

        StringBuilder builder = new StringBuilder();
        String channel = safeText(channelField, "").trim();
        String category = safeText(categoryField, "").trim();
        String direction = DIRECTION_OPTIONS[Math.max(0, Math.min(directionIndex, DIRECTION_OPTIONS.length - 1))];
        for (int i = 0; i < proposals.size(); i++) {
            CapturedIdSmartRuleGenerator.Proposal proposal = proposals.get(i);
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("# Rule ").append(i + 1).append("\n");
            builder.append("name=").append(prefix).append("_").append(i + 1).append("\n");
            builder.append("channel=").append(channel).append("\n");
            builder.append("direction=").append(direction).append("\n");
            builder.append("category=").append(category).append("\n");
            builder.append("group=1\n");
            builder.append("valueType=hex\n");
            builder.append("byteLength=").append(Math.max(1, proposal.getMaxBytes())).append("\n");
            builder.append("pattern=").append(proposal.getPattern()).append("\n");
            builder.append("samples=").append(joinSampleValues(proposal.getSampleValues())).append("\n");
        }
        setClipboardString(builder.toString());
        setStatus("§a已复制 " + proposals.size() + " 条生成规则", 0xFF8CFF9E);
    }

    private List<GuiTextField> getAllFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(rulePrefixField);
        fields.add(displayPrefixField);
        fields.add(channelField);
        fields.add(categoryField);
        fields.add(sampleInputField);
        return fields;
    }

    private String buildAnalysisSignature() {
        StringBuilder builder = new StringBuilder();
        builder.append(safeText(rulePrefixField, "")).append('|')
                .append(safeText(displayPrefixField, "")).append('|')
                .append(safeText(channelField, "")).append('|')
                .append(safeText(categoryField, "")).append('|')
                .append(directionIndex);
        for (String sample : sampleEntries) {
            builder.append('|').append(sample);
        }
        return builder.toString();
    }

    private void drawFieldLabel(String label, int x, int y) {
        drawString(this.fontRenderer, label, x, y, 0xFFD7E6F5);
    }

    private String buildDirectionButtonText() {
        return "方向: " + DIRECTION_LABELS[Math.max(0, Math.min(directionIndex, DIRECTION_LABELS.length - 1))];
    }

    private String joinSampleValues(List<String> sampleValues) {
        if (sampleValues == null || sampleValues.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sampleValues.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(sampleValues.get(i));
        }
        return builder.toString();
    }

    private String uniqueRuleName(Set<String> existingNames, String preferred) {
        String base = preferred == null ? "smart_id" : preferred.trim();
        if (base.isEmpty()) {
            base = "smart_id";
        }
        String candidate = base;
        int index = 2;
        while (existingNames.contains(candidate.toLowerCase())) {
            candidate = base + "_" + index;
            index++;
        }
        return candidate;
    }

    private String trimToWidth(String text, int maxWidth) {
        String safe = text == null ? "" : text;
        return this.fontRenderer == null ? safe : this.fontRenderer.trimStringToWidth(safe, Math.max(24, maxWidth));
    }

    private void updateScrollableLeftButtonVisibility() {
        updateButtonVisibility(addSampleButton);
        updateButtonVisibility(clearSamplesButton);
        updateButtonVisibility(loadSelectedButton);
    }

    private void updateButtonVisibility(GuiButton button) {
        if (button == null) {
            return;
        }
        button.visible = intersectsViewport(button.x, button.y, button.width, button.height, leftContentViewportBounds);
    }

    private boolean intersectsViewport(int x, int y, int width, int height, Rectangle viewport) {
        if (viewport == null || width <= 0 || height <= 0) {
            return false;
        }
        return x + width > viewport.x && x < viewport.x + viewport.width
                && y + height > viewport.y && y < viewport.y + viewport.height;
    }

    private void beginScissor(Rectangle viewport) {
        if (viewport == null || viewport.width <= 0 || viewport.height <= 0) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(mc);
        int scaleFactor = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(viewport.x * scaleFactor,
                mc.getWindow().getScreenHeight() - ((viewport.y + viewport.height) * scaleFactor),
                viewport.width * scaleFactor,
                viewport.height * scaleFactor);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private String safeText(GuiTextField field, String fallback) {
        return field == null ? fallback : (field.getText() == null ? fallback : field.getText());
    }

    private String normalizeHex(String text) {
        String normalized = text == null ? "" : text.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        if (normalized.isEmpty()) {
            return "";
        }
        if ((normalized.length() & 1) != 0) {
            normalized = "0" + normalized;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length(); i += 2) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(normalized.substring(i, i + 2));
        }
        return builder.toString();
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private void drawSmartGeneratorTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (isMouseOverField(mouseX, mouseY, rulePrefixField)) {
            tooltip = "生成规则时使用的规则名前缀。\n导入后会按这个前缀自动编号。";
        } else if (isMouseOverField(mouseX, mouseY, displayPrefixField)) {
            tooltip = "生成规则时使用的显示名前缀。\n只影响界面展示名称。";
        } else if (isMouseOverField(mouseX, mouseY, channelField)) {
            tooltip = "批量导入时写入规则的频道过滤。\n留空表示不限制频道。";
        } else if (isMouseOverField(mouseX, mouseY, categoryField)) {
            tooltip = "批量导入时写入规则的分组。";
        } else if (isMouseOverField(mouseX, mouseY, sampleInputField)) {
            tooltip = "在这里输入一组 HEX 样本。\n支持空格、换行和无分隔粘贴，回车可直接添加。";
        } else if (sampleListBounds.contains(mouseX, mouseY)) {
            tooltip = "样本卡片列表。\n左键可选中并回填编辑，拖拽可排序，点 x 可删除。";
        } else if (resultListBounds.contains(mouseX, mouseY)) {
            tooltip = "自动生成结果列表。\n左键选中一条规则结果，滚轮一次滚动一张规则卡片。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case BTN_BACK:
                        tooltip = "返回捕获ID规则管理器。";
                        break;
                    case BTN_COPY_ALL:
                        tooltip = "把当前所有自动生成结果复制成文本，方便外部保存或人工微调。";
                        break;
                    case BTN_IMPORT_ALL:
                        tooltip = "把当前全部自动生成结果批量导入到捕获ID规则管理器。";
                        break;
                    case BTN_IMPORT_SELECTED:
                        tooltip = "只导入当前选中的那条自动生成结果。";
                        break;
                    case BTN_CYCLE_DIRECTION:
                        tooltip = "切换导入规则的方向过滤：双向 / 仅入站 / 仅出站。";
                        break;
                    case BTN_ADD_SAMPLE:
                        tooltip = editingSampleIndex >= 0
                                ? "把输入框中的 HEX 覆盖更新到当前选中的样本卡片。"
                                : "把输入框中的 HEX 添加为新的样本卡片。";
                        break;
                    case BTN_CLEAR_SAMPLES:
                        tooltip = "清空当前样本卡片列表。\n只有手动点击这里才会删除已有样本。";
                        break;
                    case BTN_LOAD_SELECTED:
                        tooltip = "把当前选中的样本卡片内容重新载入输入框。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private void loadDraft() {
        if (rulePrefixField != null || displayPrefixField != null || channelField != null || categoryField != null) {
            return;
        }
        directionIndex = Math.max(0, Math.min(DIRECTION_OPTIONS.length - 1, DRAFT.directionIndex));
    }

    private void saveDraft() {
        DRAFT.rulePrefix = safeText(rulePrefixField, DRAFT.rulePrefix);
        DRAFT.displayPrefix = safeText(displayPrefixField, DRAFT.displayPrefix);
        DRAFT.channel = safeText(channelField, DRAFT.channel);
        DRAFT.category = safeText(categoryField, DRAFT.category);
        DRAFT.directionIndex = directionIndex;
        DRAFT.samples.clear();
        DRAFT.samples.addAll(sampleEntries);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0 && draggingSampleIndex >= 0 && draggingSampleIndex < sampleEntries.size()
                && sampleDropIndex >= 0) {
            int targetIndex = sampleDropIndex;
            if (targetIndex > draggingSampleIndex) {
                targetIndex--;
            }
            targetIndex = Math.max(0, Math.min(sampleEntries.size() - 1, targetIndex));
            if (targetIndex != draggingSampleIndex) {
                String moved = sampleEntries.remove(draggingSampleIndex);
                sampleEntries.add(targetIndex, moved);
                if (editingSampleIndex == draggingSampleIndex) {
                    editingSampleIndex = targetIndex;
                } else if (editingSampleIndex >= 0) {
                    if (editingSampleIndex > draggingSampleIndex && editingSampleIndex <= targetIndex) {
                        editingSampleIndex--;
                    } else if (editingSampleIndex < draggingSampleIndex && editingSampleIndex >= targetIndex) {
                        editingSampleIndex++;
                    }
                }
                analyzeIfNeeded(true);
            }
        }
        pressedSampleIndex = -1;
        draggingSampleIndex = -1;
        sampleDropIndex = -1;
        draggingResultScrollbar = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0 && draggingResultScrollbar && resultMaxScroll > 0 && resultScrollbarBounds.height > 0) {
            int visibleHeight = resultListBounds.height;
            int thumbHeight = Math.max(18,
                    (int) ((visibleHeight / (float) Math.max(visibleHeight, visibleHeight + resultMaxScroll)) * visibleHeight));
            int track = Math.max(1, visibleHeight - thumbHeight);
            int relative = Math.max(0, Math.min(track, mouseY - resultListBounds.y - thumbHeight / 2));
            resultScrollOffset = (int) Math.round((relative / (double) Math.max(1, track)) * resultMaxScroll);
            return;
        }
        if (clickedMouseButton != 0 || pressedSampleIndex < 0 || pressedSampleIndex >= sampleEntries.size()
                || !sampleListBounds.contains(mouseX, mouseY)) {
            return;
        }
        draggingSampleIndex = pressedSampleIndex;
        int relativeY = mouseY - sampleListBounds.y + sampleScrollOffset;
        int step = SAMPLE_CARD_HEIGHT + SAMPLE_CARD_GAP;
        sampleDropIndex = Math.max(0, Math.min(sampleEntries.size(), relativeY / Math.max(1, step)));
    }

    private void updateSampleActionButtons() {
        for (GuiButton button : this.buttonList) {
            if (button == null) {
                continue;
            }
            if (button.id == BTN_ADD_SAMPLE) {
                button.displayString = getAddOrUpdateButtonText();
            } else if (button.id == BTN_LOAD_SELECTED) {
                button.enabled = editingSampleIndex >= 0 && editingSampleIndex < sampleEntries.size();
            } else if (button.id == BTN_IMPORT_SELECTED) {
                button.enabled = selectedProposalIndex >= 0
                        && selectedProposalIndex < currentAnalysis.getProposals().size();
            }
        }
    }

    private String getAddOrUpdateButtonText() {
        return editingSampleIndex >= 0 ? "更新" : "添加";
    }
}


