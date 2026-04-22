package com.zszl.zszlScriptMod.otherfeatures.gui.block;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager.FeatureState;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SingleBlockFeatureConfigScreen extends ThemedGuiScreen {

    private static final int CONTROL_HEIGHT = 20;
    private static final int SCROLLBAR_TRACK_WIDTH = 4;
    private static final int SCROLLBAR_GUTTER = 10;
    private static final int BTN_VALUE_INPUT = 99;
    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;
    private static final int DROPDOWN_ITEM_HEIGHT = 18;
    private static final int MAX_STATE_MENU_HEIGHT = DROPDOWN_ITEM_HEIGHT * 2;
    private static final String[] ON_OFF_OPTIONS = { "开启", "关闭" };

    private final GuiScreen parentScreen;
    private final String featureId;
    private final String title;
    private final List<String> instructionLines = new ArrayList<>();
    private final List<String> descriptionLines = new ArrayList<>();

    private GuiButton valueButton;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private ConfigDropdown stateDropdown;
    private ConfigDropdown hudDropdown;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int footerY;
    private int contentScrollOffset;
    private int maxContentScroll;
    private boolean draggingScrollbar;
    private int scrollbarDragOffsetY;

    private int stateHintY;
    private int hudHintY;
    private int stateSectionBottom;
    private int valueSectionY;
    private int valueLabelY;
    private int valueSectionBottom;
    private int valueRangeY;
    private int infoSectionY;
    private int infoRuntimeY;
    private int infoDraftY;
    private int infoSectionBottom;

    private boolean draftInitialized;
    private boolean draftEnabled;
    private boolean draftStatusHudEnabled;
    private float draftValue;

    public SingleBlockFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        this.parentScreen = parentScreen;
        this.featureId = featureId == null ? "" : featureId.trim();
        this.title = title == null ? "方块功能设置" : title.trim();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.draggingScrollbar = false;
        if (!this.draftInitialized) {
            this.contentScrollOffset = 0;
        }
        this.maxContentScroll = 0;
        computeLayout();
        initControls();
        if (!this.draftInitialized) {
            loadDraftFromFeature();
            this.draftInitialized = true;
        }
        relayoutControls();
        refreshControlTexts();
    }

    private void computeLayout() {
        FeatureState state = getFeature();

        int maxWidth = Math.min(540, Math.max(360, this.width - 12));
        int maxUsableWidth = Math.max(240, this.width - 8);
        this.panelWidth = Math.min(maxWidth, maxUsableWidth);

        rebuildInstructionLines(state);
        rebuildDescriptionLines(state);

        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int instructionHeight = this.instructionLines.size() * textStep;
        boolean supportsValue = state != null && state.supportsValue();
        int valueBodyHeight = supportsValue
                ? textStep + 4 + CONTROL_HEIGHT + 4 + this.fontRenderer.FONT_HEIGHT
                : this.fontRenderer.FONT_HEIGHT + 2;
        int infoBodyHeight = Math.max(36, this.descriptionLines.size() * textStep + 6 + this.fontRenderer.FONT_HEIGHT + 6
                + (supportsValue ? this.fontRenderer.FONT_HEIGHT + 2 : 0));
        int requiredHeight = 24 + instructionHeight + 8 + 18 + 10
                + 18 + textStep + 4 + CONTROL_HEIGHT + MAX_STATE_MENU_HEIGHT
                + 8 + textStep + 4 + CONTROL_HEIGHT + MAX_STATE_MENU_HEIGHT
                + 12 + 18 + valueBodyHeight
                + 12 + 18 + infoBodyHeight
                + 8 + 28;
        int maxHeight = Math.max(240, this.height - 8);

        this.panelHeight = Math.min(Math.max(360, requiredHeight), maxHeight);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.contentTop = this.panelY + 24 + instructionHeight + 8;
        this.footerY = this.panelY + this.panelHeight - 28;
        this.contentBottom = this.footerY - 8;
    }

    private void rebuildInstructionLines(FeatureState state) {
        this.instructionLines.clear();
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(buildInstructionText(state), instructionWidth));
    }

    private void rebuildDescriptionLines(FeatureState state) {
        this.descriptionLines.clear();
        String description = state == null ? "当前功能信息不可用，无法读取说明与参数。" : "§7" + state.description;
        int descriptionWidth = Math.max(120, this.panelWidth - 36);
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(description, descriptionWidth));
    }

    private String buildInstructionText(FeatureState state) {
        if (state == null) {
            return "这里可以调整当前方块功能的草稿状态与参数，修改后通过底部按钮保存并返回。";
        }
        if (state.supportsValue()) {
            return "左键其他功能页中的“" + state.name + "”为总开关；这里可切换状态，并以点击输入框的方式调整"
                    + state.valueLabel + "，也可以单独控制这个功能的状态 HUD 是否显示，修改后通过底部按钮保存。";
        }
        return "左键其他功能页中的“" + state.name + "”为总开关；这里可切换状态、配置单项状态 HUD，并查看说明，修改后通过底部按钮保存。";
    }

    private void initControls() {
        this.valueButton = new ThemedButton(BTN_VALUE_INPUT, 0, 0, 120, CONTROL_HEIGHT, "");
        this.saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, 20, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, 20, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, 20, "取消");

        this.buttonList.add(this.valueButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);

        this.stateDropdown = new ConfigDropdown(this, "功能状态", ON_OFF_OPTIONS);
        this.hudDropdown = new ConfigDropdown(this, "状态HUD", ON_OFF_OPTIONS);
    }

    private void loadDraftFromFeature() {
        FeatureState state = getFeature();
        if (state == null) {
            this.draftEnabled = false;
            this.draftStatusHudEnabled = true;
            this.draftValue = 0.0F;
            return;
        }
        this.draftEnabled = state.isEnabled();
        this.draftStatusHudEnabled = state.isStatusHudEnabled();
        this.draftValue = state.getValue();
    }

    private void relayoutControls() {
        computeLayout();

        FeatureState state = getFeature();
        boolean supportsValue = state != null && state.supportsValue();
        this.stateDropdown.setEnabled(state != null);
        this.hudDropdown.setEnabled(state != null);

        int innerPadding = 12;
        int sectionGap = 12;
        int sectionHeaderHeight = 18;
        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int fieldX = this.panelX + innerPadding;
        int fieldWidth = this.panelWidth - innerPadding * 2 - SCROLLBAR_GUTTER;
        int layoutBaseY = getScrollableViewportTop();
        int visibleContentHeight = getScrollableViewportHeight();

        for (int pass = 0; pass < 2; pass++) {
            int baseY = layoutBaseY - this.contentScrollOffset;

            this.stateHintY = baseY + sectionHeaderHeight;
            int dropdownY = this.stateHintY + textStep + 4;
            this.stateDropdown.setBounds(fieldX, dropdownY, fieldWidth, CONTROL_HEIGHT);
            this.hudHintY = dropdownY + CONTROL_HEIGHT + this.stateDropdown.getVisibleMenuHeight() + 8;
            int hudDropdownY = this.hudHintY + textStep + 4;
            this.hudDropdown.setBounds(fieldX, hudDropdownY, fieldWidth, CONTROL_HEIGHT);
            this.stateSectionBottom = hudDropdownY + CONTROL_HEIGHT + this.hudDropdown.getVisibleMenuHeight();

            this.valueSectionY = this.stateSectionBottom + sectionGap;
            this.valueLabelY = this.valueSectionY + sectionHeaderHeight;
            if (supportsValue) {
                this.valueButton.x = fieldX;
                this.valueButton.y = this.valueLabelY + textStep + 4;
                this.valueButton.width = fieldWidth;
                this.valueButton.height = CONTROL_HEIGHT;
                this.valueRangeY = this.valueButton.y + CONTROL_HEIGHT + 4;
                this.valueSectionBottom = this.valueRangeY + this.fontRenderer.FONT_HEIGHT;
            } else {
                this.valueRangeY = this.valueLabelY;
                this.valueSectionBottom = this.valueLabelY + this.fontRenderer.FONT_HEIGHT + 2;
            }

            this.infoSectionY = this.valueSectionBottom + sectionGap;
            int currentY = this.infoSectionY + sectionHeaderHeight;
            currentY += this.descriptionLines.size() * textStep;
            currentY += 6;
            this.infoRuntimeY = currentY;
            currentY += this.fontRenderer.FONT_HEIGHT + 6;
            if (supportsValue) {
                this.infoDraftY = currentY;
                currentY += this.fontRenderer.FONT_HEIGHT + 2;
            } else {
                this.infoDraftY = Integer.MIN_VALUE;
            }
            this.infoSectionBottom = currentY;

            int totalContentHeight = this.infoSectionBottom - layoutBaseY + 18;
            this.maxContentScroll = Math.max(0, totalContentHeight - visibleContentHeight);
            int clampedScroll = clampInt(this.contentScrollOffset, 0, this.maxContentScroll);
            if (clampedScroll == this.contentScrollOffset) {
                break;
            }
            this.contentScrollOffset = clampedScroll;
        }

        this.valueButton.visible = supportsValue
                && isVisibleInScrollableViewport(this.valueButton.y, this.valueButton.height);
        layoutFooterButtons();
    }

    private void layoutFooterButtons() {
        int gap = 6;
        int footerButtonW = Math.max(64, (this.panelWidth - 24 - gap * 2) / 3);
        int totalW = footerButtonW * 3 + gap * 2;
        int startX = this.panelX + (this.panelWidth - totalW) / 2;
        layoutFooterButton(this.saveButton, startX, this.footerY, footerButtonW);
        layoutFooterButton(this.defaultButton, startX + footerButtonW + gap, this.footerY, footerButtonW);
        layoutFooterButton(this.cancelButton, startX + (footerButtonW + gap) * 2, this.footerY, footerButtonW);
    }

    private void layoutFooterButton(GuiButton button, int x, int y, int width) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = CONTROL_HEIGHT;
        button.visible = true;
    }

    private void refreshControlTexts() {
        FeatureState state = getFeature();
        this.stateDropdown.setEnabled(state != null);
        this.hudDropdown.setEnabled(state != null);
        this.stateDropdown.setSelectedIndex(this.draftEnabled ? 0 : 1);
        this.hudDropdown.setSelectedIndex(this.draftStatusHudEnabled ? 0 : 1);
        this.valueButton.enabled = state != null && state.supportsValue();
        this.valueButton.displayString = state != null && state.supportsValue() ? formatFloat(this.draftValue) : "";
    }

    private FeatureState getFeature() {
        return BlockFeatureManager.getFeature(this.featureId);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        FeatureState state = getFeature();
        switch (button.id) {
        case BTN_VALUE_INPUT:
            if (state != null && state.supportsValue()) {
                openValueInput(state);
            }
            return;
        case BTN_SAVE:
            saveDraft(state);
            this.mc.setScreen(this.parentScreen);
            return;
        case BTN_DEFAULT:
            applyDefaults(state);
            break;
        case BTN_CANCEL:
            this.mc.setScreen(this.parentScreen);
            return;
        default:
            break;
        }

        refreshControlTexts();
        relayoutControls();
    }

    private void saveDraft(FeatureState state) {
        if (state == null) {
            return;
        }
        syncDraftFromControls(state);
        BlockFeatureManager.setEnabled(this.featureId, this.draftEnabled);
        BlockFeatureManager.setFeatureStatusHudEnabled(this.featureId, this.draftStatusHudEnabled);
        if (state.supportsValue()) {
            BlockFeatureManager.setValue(this.featureId, this.draftValue);
        }
    }

    private void applyDefaults(FeatureState state) {
        if (state == null) {
            return;
        }
        this.draftEnabled = false;
        this.draftStatusHudEnabled = true;
        this.draftValue = state.defaultValue;
        refreshControlTexts();
    }

    private void syncDraftFromControls(FeatureState state) {
        this.draftEnabled = this.stateDropdown.getSelectedIndex() == 0;
        this.draftStatusHudEnabled = this.hudDropdown.getSelectedIndex() == 0;
        if (state == null || !state.supportsValue()) {
            return;
        }
        this.draftValue = clampFloat(this.draftValue, state.minValue, state.maxValue);
    }

    private void openValueInput(FeatureState state) {
        String title = "输入 " + state.valueLabel + " (" + formatFloat(state.minValue) + " - "
                + formatFloat(state.maxValue) + ")";
        mc.setScreen(new GuiTextInput(this, title, formatFloat(this.draftValue), value -> {
            float parsed = this.draftValue;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            this.draftValue = clampFloat(parsed, state.minValue, state.maxValue);
            refreshControlTexts();
            relayoutControls();
            mc.setScreen(this);
        }));
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (isInside(mouseX, mouseY, this.panelX + 8, this.contentTop + 20, this.panelWidth - 16,
                    Math.max(20, this.contentBottom - this.contentTop - 12))) {
                this.contentScrollOffset = dWheel > 0
                        ? Math.max(0, this.contentScrollOffset - 14)
                        : Math.min(this.maxContentScroll, this.contentScrollOffset + 14);
                relayoutControls();
            }
            return;
        }

        int button = Mouse.getEventButton();
        if (button == -1) {
            return;
        }

        if (Mouse.getEventButtonState()) {
            if (handleMousePressed(mouseX, mouseY, button)) {
                return;
            }
        } else {
            mouseReleased(mouseX, mouseY, button);
        }
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && isMouseOverScrollbarTrack(mouseX, mouseY)) {
            this.stateDropdown.collapse();
            this.hudDropdown.collapse();
            beginScrollbarDrag(mouseY);
            return true;
        }

        if (isPointInsideScrollableViewport(mouseX, mouseY) && this.stateDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            this.hudDropdown.collapse();
            this.draftEnabled = this.stateDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }
        if (isPointInsideScrollableViewport(mouseX, mouseY) && this.hudDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            this.stateDropdown.collapse();
            this.draftStatusHudEnabled = this.hudDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }

        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }

        if (!isMouseOverAnyDropdown(mouseX, mouseY)) {
            this.stateDropdown.collapse();
            this.hudDropdown.collapse();
        }

        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.draggingScrollbar) {
            if (Mouse.isButtonDown(0)) {
                updateScrollFromScrollbar(mouseY);
            } else {
                this.draggingScrollbar = false;
            }
        }

        FeatureState state = getFeature();
        if (state != null) {
            syncDraftFromControls(state);
        }

        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, this.title, this.fontRenderer);
        drawInstructionText();
        drawStatusStrip(state);
        drawScrollableContent(mouseX, mouseY, partialTicks, state);
        drawContentScrollbar();
        drawFixedButtons(mouseX, mouseY, partialTicks);
        drawTopLevelTooltips(mouseX, mouseY, state);
    }

    private void drawInstructionText() {
        int textY = this.panelY + 22;
        int lineStep = this.fontRenderer.FONT_HEIGHT + 2;
        for (String line : this.instructionLines) {
            drawString(this.fontRenderer, line, this.panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += lineStep;
        }
    }

    private void drawStatusStrip(FeatureState state) {
        int stripX = this.panelX + 14;
        int stripY = this.contentTop;
        int stripW = this.panelWidth - 28;
        int stripH = 18;
        drawRect(stripX, stripY, stripX + stripW, stripY + stripH, 0x33202A36);
        drawHorizontalLine(stripX, stripX + stripW, stripY, 0x664FA6D9);
        drawHorizontalLine(stripX, stripX + stripW, stripY + stripH, 0x4435536C);
        String status = "状态: " + (this.draftEnabled ? "§a开启" : "§c关闭")
                + (state == null ? " §7| 功能: §c未找到" : " §7| 功能: §f" + state.name)
                + " §7| HUD: " + (this.draftStatusHudEnabled ? "§a开" : "§c关")
                + (MovementFeatureManager.isMasterStatusHudEnabled() ? " §7/总开" : " §c/总关")
                + " §7| 逻辑: "
                + (state != null && state.behaviorImplemented ? "§a已接入" : "§6占位");
        if (state != null && state.supportsValue()) {
            status += " §7| 草稿: §f" + formatFloat(this.draftValue);
        }
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(status, stripW - 12), stripX + 6, stripY + 5,
                0xFFFFFFFF);
    }

    private void drawScrollableContent(int mouseX, int mouseY, float partialTicks, FeatureState state) {
        int viewportX = getScrollableViewportLeft();
        int viewportY = getScrollableViewportTop();
        int viewportW = getScrollableViewportWidth();
        int viewportH = getScrollableViewportHeight();
        if (viewportW <= 0 || viewportH <= 0) {
            return;
        }

        int rowX = this.panelX + 12;
        int rowRight = getScrollableViewportRight() - 6;
        boolean mouseInViewport = isPointInsideScrollableViewport(mouseX, mouseY);
        int clippedMouseX = mouseInViewport ? mouseX : Integer.MIN_VALUE;
        int clippedMouseY = mouseInViewport ? mouseY : Integer.MIN_VALUE;

        startScissor(viewportX, viewportY, viewportW, viewportH);
        try {
            drawSectionBox("基础状态", rowX, this.stateHintY, rowRight, this.stateSectionBottom);
            drawSectionBox("功能参数", rowX, this.valueLabelY, rowRight, this.valueSectionBottom);
            drawSectionBox("状态提示", rowX, this.infoSectionY + 18, rowRight, this.infoSectionBottom);

            if (isVisibleInScrollableViewport(this.stateHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7切换后需用底部“保存并关闭”提交。", rowX, this.stateHintY, 0xFFB6C5D6);
            }
            this.stateDropdown.draw(clippedMouseX, clippedMouseY);
            if (isVisibleInScrollableViewport(this.hudHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7单项 HUD 默认为开启，仍会受到顶部“总状态HUD”控制。", rowX, this.hudHintY,
                        0xFFB6C5D6);
            }
            this.hudDropdown.draw(clippedMouseX, clippedMouseY);

            if (state != null && state.supportsValue()) {
                if (isVisibleInScrollableViewport(this.valueLabelY, this.fontRenderer.FONT_HEIGHT + 2)) {
                    drawString(this.fontRenderer, state.valueLabel + ":", rowX, this.valueLabelY, 0xFFE8F6FF);
                }
                if (this.valueButton.visible) {
                    this.valueButton.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
                }
                if (isVisibleInScrollableViewport(this.valueRangeY, this.fontRenderer.FONT_HEIGHT)) {
                    drawString(this.fontRenderer,
                            "§8范围 " + formatFloat(state.minValue) + " - " + formatFloat(state.maxValue),
                            rowX, this.valueRangeY, 0xFF9FB2C8);
                }
            } else if (isVisibleInScrollableViewport(this.valueLabelY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7当前没有可调参数。", rowX, this.valueLabelY, 0xFFB6C5D6);
            }

            int currentY = this.infoSectionY + 18;
            for (String line : this.descriptionLines) {
                if (isVisibleInScrollableViewport(currentY, this.fontRenderer.FONT_HEIGHT + 2)) {
                    drawString(this.fontRenderer, line, rowX, currentY, 0xFFE2F2FF);
                }
                currentY += this.fontRenderer.FONT_HEIGHT + 2;
            }

            if (isVisibleInScrollableViewport(this.infoRuntimeY, this.fontRenderer.FONT_HEIGHT + 2)) {
                String runtime = state == null
                        ? "§6运行状态: 未找到对应功能"
                        : "§e运行状态: §f" + BlockFeatureManager.getFeatureRuntimeSummary(this.featureId);
                drawString(this.fontRenderer, runtime, rowX, this.infoRuntimeY, 0xFFFFFFFF);
            }

            if (state != null && state.supportsValue()
                    && isVisibleInScrollableViewport(this.infoDraftY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7当前草稿值: §f" + formatFloat(this.draftValue), rowX, this.infoDraftY,
                        0xFFFFFFFF);
            }
        } finally {
            endScissor();
        }
    }

    private void drawFixedButtons(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : this.buttonList) {
            if (isFooterButton(button) && button != null && button.visible) {
                button.drawButton(this.mc, mouseX, mouseY, partialTicks);
            }
        }
    }

    private void drawSectionBox(String title, int minX, int minY, int maxX, int maxY) {
        int viewportLeft = getScrollableViewportLeft();
        int viewportTop = getScrollableViewportTop();
        int viewportRight = getScrollableViewportRight();
        int viewportBottom = getScrollableViewportBottom();

        int boxX = minX - 6;
        int boxY = minY - 18;
        int boxW = (maxX - minX) + 12;
        int boxH = (maxY - minY) + 24;
        int boxRight = boxX + boxW;
        int boxBottom = boxY + boxH;

        if (boxRight <= viewportLeft || boxX >= viewportRight || boxBottom <= viewportTop || boxY >= viewportBottom) {
            return;
        }

        int drawLeft = Math.max(boxX, viewportLeft);
        int drawTop = Math.max(boxY, viewportTop);
        int drawRight = Math.min(boxRight, viewportRight);
        int drawBottom = Math.min(boxBottom, viewportBottom);

        drawRect(drawLeft, drawTop, drawRight, drawBottom, 0x44202A36);
        if (boxY >= viewportTop && boxY < viewportBottom) {
            drawHorizontalLine(drawLeft, drawRight, boxY, 0xFF4FA6D9);
        }
        if (boxBottom > viewportTop && boxBottom <= viewportBottom) {
            drawHorizontalLine(drawLeft, drawRight, boxBottom, 0xFF35536C);
        }
        if (boxX >= viewportLeft && boxX < viewportRight) {
            drawVerticalLine(boxX, drawTop, drawBottom, 0xFF35536C);
        }
        if (boxRight > viewportLeft && boxRight <= viewportRight) {
            drawVerticalLine(boxRight, drawTop, drawBottom, 0xFF35536C);
        }

        int titleY = boxY + 5;
        if (titleY + this.fontRenderer.FONT_HEIGHT > viewportTop && titleY < viewportBottom) {
            drawString(this.fontRenderer, "§b" + title, boxX + 6, titleY, 0xFFE8F6FF);
        }
    }

    private void drawTopLevelTooltips(int mouseX, int mouseY, FeatureState state) {
        if (state != null && isMouseOverStateDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e功能状态",
                    "§7当前草稿: " + (this.draftEnabled ? "§a开启" : "§c关闭"),
                    "§7使用下拉框切换状态。"), mouseX, mouseY);
        } else if (state != null && isMouseOverHudDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e状态HUD",
                    "§7当前草稿: " + (this.draftStatusHudEnabled ? "§a开启" : "§c关闭"),
                    "§7控制这个功能是否在 HUD 中显示状态。",
                    "§7即使这里开启，也仍受顶部“总状态HUD”总开关控制。"), mouseX, mouseY);
        } else if (state != null && state.supportsValue() && isMouseOver(mouseX, mouseY, this.valueButton)) {
            List<String> lines = new ArrayList<>();
            lines.add("§e" + state.valueLabel);
            lines.add("§7当前草稿: §f" + formatFloat(this.draftValue));
            lines.add("§7范围: §f" + formatFloat(state.minValue) + " - " + formatFloat(state.maxValue));
            lines.add("§7点击后弹出输入框进行数值输入。");
            lines.add("§7保存后才会真正生效。");
            drawHoveringText(lines, mouseX, mouseY);
        }
    }

    private void drawContentScrollbar() {
        if (this.maxContentScroll <= 0) {
            return;
        }
        int sbX = getScrollbarTrackX();
        int sbY = getScrollbarTrackY();
        int sbH = getScrollbarTrackHeight();
        int thumbY = getScrollbarThumbY();
        int thumbH = getScrollbarThumbHeight();
        drawRect(sbX, sbY, sbX + SCROLLBAR_TRACK_WIDTH, sbY + sbH, 0x55141C24);
        drawRect(sbX, thumbY, sbX + SCROLLBAR_TRACK_WIDTH, thumbY + thumbH, 0xCC7CCBFF);
    }

    private boolean handleButtonActivation(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : this.buttonList) {
            if (button == null || !button.visible || !button.enabled) {
                continue;
            }
            if (!isPointInsideButtonArea(button, mouseX, mouseY)) {
                continue;
            }
            button.playPressSound(this.mc.getSoundManager());
            actionPerformed(button);
            return true;
        }
        return false;
    }

    private boolean isFooterButton(GuiButton button) {
        return button == this.saveButton || button == this.defaultButton || button == this.cancelButton;
    }

    private boolean isScrollableButton(GuiButton button) {
        return button != null && !isFooterButton(button);
    }

    private boolean isMouseOver(int mouseX, int mouseY, GuiButton button) {
        return button != null && button.visible && isPointInsideButtonArea(button, mouseX, mouseY);
    }

    private boolean isPointInsideButtonArea(GuiButton button, int mouseX, int mouseY) {
        if (button == null || !isInside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
            return false;
        }
        return !isScrollableButton(button) || isPointInsideScrollableViewport(mouseX, mouseY);
    }

    private int getScrollableViewportLeft() {
        return this.panelX + 6;
    }

    private int getScrollableViewportTop() {
        return this.contentTop + 28;
    }

    private int getScrollableViewportWidth() {
        return Math.max(1, this.panelWidth - 12 - SCROLLBAR_GUTTER);
    }

    private int getScrollableViewportHeight() {
        return Math.max(1, this.contentBottom - getScrollableViewportTop());
    }

    private int getScrollableViewportRight() {
        return getScrollableViewportLeft() + getScrollableViewportWidth();
    }

    private int getScrollableViewportBottom() {
        return getScrollableViewportTop() + getScrollableViewportHeight();
    }

    private boolean isPointInsideScrollableViewport(int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, getScrollableViewportLeft(), getScrollableViewportTop(),
                getScrollableViewportWidth(), getScrollableViewportHeight());
    }

    private boolean isVisibleInScrollableViewport(int y, int height) {
        return y + height >= getScrollableViewportTop() && y <= getScrollableViewportBottom();
    }

    private boolean isMouseOverStateDropdown(int mouseX, int mouseY) {
        return isPointInsideScrollableViewport(mouseX, mouseY) && this.stateDropdown.isMouseOver(mouseX, mouseY);
    }

    private boolean isMouseOverHudDropdown(int mouseX, int mouseY) {
        return isPointInsideScrollableViewport(mouseX, mouseY) && this.hudDropdown.isMouseOver(mouseX, mouseY);
    }

    private boolean isMouseOverAnyDropdown(int mouseX, int mouseY) {
        return isMouseOverStateDropdown(mouseX, mouseY) || isMouseOverHudDropdown(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            this.draggingScrollbar = false;
        }
    }

    private int getScrollbarTrackX() {
        return this.panelX + this.panelWidth - 10;
    }

    private int getScrollbarTrackY() {
        return getScrollableViewportTop();
    }

    private int getScrollbarTrackHeight() {
        return Math.max(20, getScrollableViewportHeight());
    }

    private int getScrollbarThumbHeight() {
        int trackHeight = getScrollbarTrackHeight();
        return Math.max(16, (int) ((float) trackHeight * trackHeight
                / Math.max(trackHeight, trackHeight + this.maxContentScroll)));
    }

    private int getScrollbarThumbY() {
        int trackHeight = getScrollbarTrackHeight();
        int thumbHeight = getScrollbarThumbHeight();
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        return getScrollbarTrackY()
                + (int) ((this.contentScrollOffset / (float) Math.max(1, this.maxContentScroll)) * thumbTravel);
    }

    private boolean isMouseOverScrollbarTrack(int mouseX, int mouseY) {
        return this.maxContentScroll > 0
                && isInside(mouseX, mouseY, getScrollbarTrackX(), getScrollbarTrackY(), SCROLLBAR_TRACK_WIDTH,
                        getScrollbarTrackHeight());
    }

    private void beginScrollbarDrag(int mouseY) {
        this.draggingScrollbar = true;
        int thumbY = getScrollbarThumbY();
        int thumbHeight = getScrollbarThumbHeight();
        this.scrollbarDragOffsetY = mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                ? mouseY - thumbY
                : thumbHeight / 2;
        updateScrollFromScrollbar(mouseY);
    }

    private void updateScrollFromScrollbar(int mouseY) {
        if (this.maxContentScroll <= 0) {
            return;
        }
        int trackY = getScrollbarTrackY();
        int trackHeight = getScrollbarTrackHeight();
        int thumbHeight = getScrollbarThumbHeight();
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = clampInt(mouseY - this.scrollbarDragOffsetY, trackY, trackY + trackHeight - thumbHeight);
        float ratio = (thumbY - trackY) / (float) thumbTravel;
        this.contentScrollOffset = clampInt(Math.round(ratio * this.maxContentScroll), 0, this.maxContentScroll);
        relayoutControls();
    }

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int scaleFactor = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scaleFactor, (this.height - (y + height)) * scaleFactor, width * scaleFactor,
                height * scaleFactor);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}







