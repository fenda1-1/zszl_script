package com.zszl.zszlScriptMod.otherfeatures.gui.common;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SimpleFeatureConfigScreen extends ThemedGuiScreen {

    protected static final int CONTROL_HEIGHT = 20;
    private static final int SCROLLBAR_TRACK_WIDTH = 4;
    private static final int SCROLLBAR_GUTTER = 10;
    private static final int SCROLL_BOTTOM_PADDING = 18;
    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;
    protected static final int BTN_PRIMARY_OPTION = 103;
    protected static final int BTN_SECONDARY_OPTION = 104;
    protected static final int BTN_TERTIARY_OPTION = 105;
    private static final String[] ON_OFF_OPTIONS = { "开启", "关闭" };

    protected final GuiScreen parentScreen;
    protected final String featureId;
    protected final String title;

    private final List<String> instructionLines = new ArrayList<>();
    private final List<String> descriptionLines = new ArrayList<>();

    private ConfigDropdown stateDropdown;
    private ConfigDropdown hudDropdown;
    private GuiButton primaryOptionButton;
    private GuiButton secondaryOptionButton;
    private GuiButton tertiaryOptionButton;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;

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
    private int optionSectionY;
    private int optionHintY;
    private int optionPrimaryY;
    private int optionSecondaryY;
    private int optionTertiaryY;
    private int optionEmptyY;
    private int optionSectionBottom;
    private int infoSectionY;
    private int infoRuntimeY;
    private int infoSectionBottom;

    private boolean draftInitialized;
    protected boolean draftEnabled;
    protected boolean draftStatusHudEnabled;

    protected SimpleFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        this.parentScreen = parentScreen;
        this.featureId = featureId == null ? "" : featureId.trim();
        this.title = title == null ? getDefaultTitle() : title.trim();
    }

    protected static final class FeatureView {
        public final String name;
        public final String description;
        public final boolean enabled;
        public final boolean statusHudEnabled;
        public final boolean behaviorImplemented;
        public final String runtimeSummary;

        public FeatureView(String name, String description, boolean enabled, boolean statusHudEnabled,
                boolean behaviorImplemented, String runtimeSummary) {
            this.name = safe(name);
            this.description = safe(description);
            this.enabled = enabled;
            this.statusHudEnabled = statusHudEnabled;
            this.behaviorImplemented = behaviorImplemented;
            this.runtimeSummary = safe(runtimeSummary);
        }

        private static String safe(String text) {
            return text == null ? "" : text.trim();
        }
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

    private void loadDraftFromFeature() {
        FeatureView view = getFeatureView();
        this.draftEnabled = view != null && view.enabled;
        this.draftStatusHudEnabled = view == null || view.statusHudEnabled;
    }

    private void computeLayout() {
        FeatureView view = getFeatureView();

        int maxWidth = Math.min(540, Math.max(360, this.width - 12));
        int maxUsableWidth = Math.max(240, this.width - 8);
        this.panelWidth = Math.min(maxWidth, maxUsableWidth);

        rebuildText(view);

        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int instructionHeight = this.instructionLines.size() * textStep;
        int optionButtonCount = 0;
        if (hasPrimaryOptionButton()) {
            optionButtonCount++;
        }
        if (hasSecondaryOptionButton()) {
            optionButtonCount++;
        }
        if (hasTertiaryOptionButton()) {
            optionButtonCount++;
        }
        int optionBodyHeight;
        if (optionButtonCount <= 0) {
            optionBodyHeight = textStep + 2;
        } else if (optionButtonCount == 1) {
            optionBodyHeight = textStep + 4 + CONTROL_HEIGHT;
        } else if (optionButtonCount == 2) {
            optionBodyHeight = textStep + 4 + CONTROL_HEIGHT + 6 + CONTROL_HEIGHT;
        } else {
            optionBodyHeight = textStep + 4 + CONTROL_HEIGHT + 6 + CONTROL_HEIGHT + 6 + CONTROL_HEIGHT;
        }
        int infoBodyHeight = Math.max(36,
                this.descriptionLines.size() * textStep + 6 + this.fontRenderer.FONT_HEIGHT + 2);
        int requiredHeight = 24 + instructionHeight + 8 + 18 + 10
                + 18 + textStep + 4 + CONTROL_HEIGHT + ConfigDropdown.MAX_MENU_HEIGHT
                + 8 + textStep + 4 + CONTROL_HEIGHT + ConfigDropdown.MAX_MENU_HEIGHT
                + 12 + 18 + optionBodyHeight
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

    private void rebuildText(FeatureView view) {
        this.instructionLines.clear();
        this.descriptionLines.clear();

        String instruction = view == null
                ? "这里可以调整当前功能的草稿状态与 HUD 显示，修改后通过底部按钮保存并返回。"
                : "左键其他功能页中的“" + safe(view.name, "当前功能")
                        + "”为总开关；这里可切换状态、配置单项状态 HUD，并查看运行状态与附加参数。修改后通过底部按钮保存。";
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(instruction, instructionWidth));

        String description = view == null ? "§7当前功能信息不可用，无法读取说明。"
                : "§7" + safe(view.description, "暂无说明。");
        int descriptionWidth = Math.max(120, this.panelWidth - 36);
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(description, descriptionWidth));
    }

    private void initControls() {
        this.primaryOptionButton = new ThemedButton(BTN_PRIMARY_OPTION, 0, 0, 120, CONTROL_HEIGHT, "");
        this.secondaryOptionButton = new ThemedButton(BTN_SECONDARY_OPTION, 0, 0, 120, CONTROL_HEIGHT, "");
        this.tertiaryOptionButton = new ThemedButton(BTN_TERTIARY_OPTION, 0, 0, 120, CONTROL_HEIGHT, "");
        this.saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, CONTROL_HEIGHT, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, CONTROL_HEIGHT, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, CONTROL_HEIGHT, "取消");

        this.buttonList.add(this.primaryOptionButton);
        this.buttonList.add(this.secondaryOptionButton);
        this.buttonList.add(this.tertiaryOptionButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);

        this.stateDropdown = new ConfigDropdown(this, "功能状态", ON_OFF_OPTIONS);
        this.hudDropdown = new ConfigDropdown(this, "状态HUD", ON_OFF_OPTIONS);
    }

    private void relayoutControls() {
        computeLayout();

        FeatureView view = getFeatureView();
        boolean featureAvailable = isFeatureAvailable(view);
        this.stateDropdown.setEnabled(featureAvailable);
        this.hudDropdown.setEnabled(featureAvailable);

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
            int stateDropdownY = this.stateHintY + textStep + 4;
            this.stateDropdown.setBounds(fieldX, stateDropdownY, fieldWidth, CONTROL_HEIGHT);

            this.hudHintY = stateDropdownY + CONTROL_HEIGHT + this.stateDropdown.getVisibleMenuHeight() + 8;
            int hudDropdownY = this.hudHintY + textStep + 4;
            this.hudDropdown.setBounds(fieldX, hudDropdownY, fieldWidth, CONTROL_HEIGHT);
            this.stateSectionBottom = hudDropdownY + CONTROL_HEIGHT + this.hudDropdown.getVisibleMenuHeight();

            this.optionSectionY = this.stateSectionBottom + sectionGap;
            this.optionHintY = this.optionSectionY + sectionHeaderHeight;
            this.optionEmptyY = this.optionHintY + textStep + 4;
            int currentOptionY = this.optionEmptyY;
            this.optionPrimaryY = currentOptionY;
            this.optionSecondaryY = currentOptionY;
            this.optionTertiaryY = currentOptionY;

            int optionBottom = this.optionHintY + textStep + 2;
            if (hasPrimaryOptionButton()) {
                this.optionPrimaryY = currentOptionY;
                layoutScrollableButton(this.primaryOptionButton, fieldX, this.optionPrimaryY, fieldWidth,
                        isPrimaryOptionButtonEnabled());
                optionBottom = this.optionPrimaryY + CONTROL_HEIGHT;
                currentOptionY = this.optionPrimaryY + CONTROL_HEIGHT + 6;
            } else {
                hideButton(this.primaryOptionButton);
            }
            if (hasSecondaryOptionButton()) {
                this.optionSecondaryY = currentOptionY;
                layoutScrollableButton(this.secondaryOptionButton, fieldX, this.optionSecondaryY, fieldWidth,
                        isSecondaryOptionButtonEnabled());
                optionBottom = this.optionSecondaryY + CONTROL_HEIGHT;
                currentOptionY = this.optionSecondaryY + CONTROL_HEIGHT + 6;
            } else {
                hideButton(this.secondaryOptionButton);
            }
            if (hasTertiaryOptionButton()) {
                this.optionTertiaryY = currentOptionY;
                layoutScrollableButton(this.tertiaryOptionButton, fieldX, this.optionTertiaryY, fieldWidth,
                        isTertiaryOptionButtonEnabled());
                optionBottom = this.optionTertiaryY + CONTROL_HEIGHT;
            } else {
                hideButton(this.tertiaryOptionButton);
            }
            this.optionSectionBottom = Math.max(optionBottom, this.optionEmptyY + this.fontRenderer.FONT_HEIGHT);

            this.infoSectionY = this.optionSectionBottom + sectionGap;
            int currentY = this.infoSectionY + sectionHeaderHeight;
            currentY += this.descriptionLines.size() * textStep;
            currentY += 6;
            this.infoRuntimeY = currentY;
            currentY += this.fontRenderer.FONT_HEIGHT + 2;
            this.infoSectionBottom = currentY;

            int totalContentHeight = this.infoSectionBottom - baseY + SCROLL_BOTTOM_PADDING;
            this.maxContentScroll = Math.max(0, totalContentHeight - visibleContentHeight);
            int clampedScroll = clampInt(this.contentScrollOffset, 0, this.maxContentScroll);
            if (clampedScroll == this.contentScrollOffset) {
                break;
            }
            this.contentScrollOffset = clampedScroll;
        }

        layoutFooterButtons();
    }

    private void layoutScrollableButton(GuiButton button, int x, int y, int width, boolean enabled) {
        if (button == null) {
            return;
        }
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = CONTROL_HEIGHT;
        button.visible = isVisibleInScrollableViewport(y, CONTROL_HEIGHT);
        button.enabled = button.visible && enabled;
    }

    private void hideButton(GuiButton button) {
        if (button == null) {
            return;
        }
        button.visible = false;
        button.enabled = false;
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
        button.enabled = true;
    }

    private void refreshControlTexts() {
        this.stateDropdown.setSelectedIndex(this.draftEnabled ? 0 : 1);
        this.hudDropdown.setSelectedIndex(this.draftStatusHudEnabled ? 0 : 1);

        this.primaryOptionButton.visible = hasPrimaryOptionButton() && this.primaryOptionButton.visible;
        this.primaryOptionButton.enabled = this.primaryOptionButton.visible && isPrimaryOptionButtonEnabled();
        this.primaryOptionButton.displayString = safeOptionLabel(getPrimaryOptionButtonLabel(), "无额外设置");

        this.secondaryOptionButton.visible = hasSecondaryOptionButton() && this.secondaryOptionButton.visible;
        this.secondaryOptionButton.enabled = this.secondaryOptionButton.visible && isSecondaryOptionButtonEnabled();
        this.secondaryOptionButton.displayString = safeOptionLabel(getSecondaryOptionButtonLabel(), "无额外设置");

        this.tertiaryOptionButton.visible = hasTertiaryOptionButton() && this.tertiaryOptionButton.visible;
        this.tertiaryOptionButton.enabled = this.tertiaryOptionButton.visible && isTertiaryOptionButtonEnabled();
        this.tertiaryOptionButton.displayString = safeOptionLabel(getTertiaryOptionButtonLabel(), "无额外设置");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_PRIMARY_OPTION:
                onPrimaryOptionButtonPressed();
                return;
            case BTN_SECONDARY_OPTION:
                onSecondaryOptionButtonPressed();
                return;
            case BTN_TERTIARY_OPTION:
                onTertiaryOptionButtonPressed();
                refreshControlTexts();
                relayoutControls();
                return;
            case BTN_SAVE:
                syncDraftFromControls();
                saveDraftState(this.draftEnabled, this.draftStatusHudEnabled);
                this.mc.displayGuiScreen(this.parentScreen);
                return;
            case BTN_DEFAULT:
                resetFeatureState();
                this.draftInitialized = false;
                loadDraftFromFeature();
                this.draftInitialized = true;
                refreshControlTexts();
                relayoutControls();
                return;
            case BTN_CANCEL:
                this.mc.displayGuiScreen(this.parentScreen);
                return;
            default:
                break;
        }
    }

    private void syncDraftFromControls() {
        this.draftEnabled = this.stateDropdown.getSelectedIndex() == 0;
        this.draftStatusHudEnabled = this.hudDropdown.getSelectedIndex() == 0;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (isPointInsideScrollableViewport(mouseX, mouseY)) {
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

        if (isPointInsideScrollableViewport(mouseX, mouseY)
                && this.stateDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            this.hudDropdown.collapse();
            syncDraftFromControls();
            refreshControlTexts();
            relayoutControls();
            return true;
        }
        if (isPointInsideScrollableViewport(mouseX, mouseY)
                && this.hudDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            this.stateDropdown.collapse();
            syncDraftFromControls();
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

        FeatureView view = getFeatureView();
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, resolveTitle(view), this.fontRenderer);
        drawInstructionText();
        drawStatusStrip(view);
        drawScrollableContent(mouseX, mouseY, partialTicks, view);
        drawContentScrollbar();
        drawFixedButtons(mouseX, mouseY, partialTicks);
        drawTopLevelTooltips(mouseX, mouseY, view);
    }

    private void drawInstructionText() {
        int textY = this.panelY + 22;
        int lineStep = this.fontRenderer.FONT_HEIGHT + 2;
        for (String line : this.instructionLines) {
            drawString(this.fontRenderer, line, this.panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += lineStep;
        }
    }

    private void drawStatusStrip(FeatureView view) {
        int stripX = this.panelX + 14;
        int stripY = this.contentTop;
        int stripW = this.panelWidth - 28;
        int stripH = 18;
        drawRect(stripX, stripY, stripX + stripW, stripY + stripH, 0x33202A36);
        drawHorizontalLine(stripX, stripX + stripW, stripY, 0x664FA6D9);
        drawHorizontalLine(stripX, stripX + stripW, stripY + stripH, 0x4435536C);
        String status = "状态: " + (this.draftEnabled ? "§a开启" : "§c关闭")
                + (view == null ? " §7| 功能: §c未找到" : " §7| 功能: §f" + safe(view.name, "未命名功能"))
                + " §7| HUD: " + (this.draftStatusHudEnabled ? "§a开" : "§c关")
                + (MovementFeatureManager.isMasterStatusHudEnabled() ? " §7/总开" : " §c/总关")
                + " §7| 逻辑: " + (view != null && view.behaviorImplemented ? "§a已接入" : "§6占位");
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(status, stripW - 12), stripX + 6, stripY + 5,
                0xFFFFFFFF);
    }

    private void drawScrollableContent(int mouseX, int mouseY, float partialTicks, FeatureView view) {
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
            drawSectionBox("功能参数", rowX, this.optionHintY, rowRight, this.optionSectionBottom);
            drawSectionBox("状态提示", rowX, this.infoSectionY + 18, rowRight, this.infoSectionBottom);

            if (isVisibleInScrollableViewport(this.stateHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7切换后需用底部“保存并关闭”提交。", rowX, this.stateHintY, 0xFFB6C5D6);
            }
            this.stateDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.hudHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7单项 HUD 仍会受到顶部“总状态HUD”控制。", rowX, this.hudHintY, 0xFFB6C5D6);
            }
            this.hudDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.optionHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, safe(getOptionSectionHintText(), "§7这里可单独调整当前功能的附加参数。"),
                        rowX, this.optionHintY, 0xFFB6C5D6);
            }

            if (this.primaryOptionButton.visible) {
                this.primaryOptionButton.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
            }
            if (this.secondaryOptionButton.visible) {
                this.secondaryOptionButton.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
            }
            if (this.tertiaryOptionButton.visible) {
                this.tertiaryOptionButton.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
            }
            if (!hasPrimaryOptionButton() && !hasSecondaryOptionButton() && !hasTertiaryOptionButton()
                    && isVisibleInScrollableViewport(this.optionEmptyY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7当前没有额外参数。", rowX, this.optionEmptyY, 0xFFB6C5D6);
            }

            int currentY = this.infoSectionY + 18;
            for (String line : this.descriptionLines) {
                if (isVisibleInScrollableViewport(currentY, this.fontRenderer.FONT_HEIGHT + 2)) {
                    drawString(this.fontRenderer, line, rowX, currentY, 0xFFE2F2FF);
                }
                currentY += this.fontRenderer.FONT_HEIGHT + 2;
            }

            if (isVisibleInScrollableViewport(this.infoRuntimeY, this.fontRenderer.FONT_HEIGHT + 2)) {
                String runtime = view == null
                        ? "§6运行状态: 未找到对应功能"
                        : "§e运行状态: §f" + safe(view.runtimeSummary, "待机");
                drawString(this.fontRenderer, runtime, rowX, this.infoRuntimeY, 0xFFFFFFFF);
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

    private void drawTopLevelTooltips(int mouseX, int mouseY, FeatureView view) {
        if (isMouseOverStateDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e功能状态",
                    "§7当前草稿: " + (this.draftEnabled ? "§a开启" : "§c关闭"),
                    "§7使用下拉框切换状态。"), mouseX, mouseY);
        } else if (isMouseOverHudDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e状态HUD",
                    "§7当前草稿: " + (this.draftStatusHudEnabled ? "§a开启" : "§c关闭"),
                    "§7控制这个功能是否在 HUD 中显示状态。",
                    "§7即使这里开启，也仍受顶部“总状态HUD”总开关控制。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, this.primaryOptionButton) && !getPrimaryOptionTooltipLines().isEmpty()) {
            drawHoveringText(getPrimaryOptionTooltipLines(), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, this.secondaryOptionButton)
                && !getSecondaryOptionTooltipLines().isEmpty()) {
            drawHoveringText(getSecondaryOptionTooltipLines(), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, this.tertiaryOptionButton)
                && !getTertiaryOptionTooltipLines().isEmpty()) {
            drawHoveringText(getTertiaryOptionTooltipLines(), mouseX, mouseY);
        } else if (view != null && !isFeatureAvailable(view) && isPointInsideScrollableViewport(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§6当前功能信息不可用", "§7请检查功能是否仍在管理器中注册。"), mouseX, mouseY);
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
            button.playPressSound(this.mc.getSoundHandler());
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

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            this.draggingScrollbar = false;
        }
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

    private String resolveTitle(FeatureView view) {
        return view == null || view.name.isEmpty() ? this.title : view.name + "设置";
    }

    private boolean isFeatureAvailable(FeatureView view) {
        return view != null && !view.name.isEmpty();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safeOptionLabel(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    protected abstract FeatureView getFeatureView();

    protected abstract void saveDraftState(boolean enabled, boolean statusHudEnabled);

    protected abstract void resetFeatureState();

    protected abstract String getDefaultTitle();

    protected boolean hasPrimaryOptionButton() {
        return false;
    }

    protected boolean hasSecondaryOptionButton() {
        return false;
    }

    protected boolean isPrimaryOptionButtonEnabled() {
        return true;
    }

    protected boolean isSecondaryOptionButtonEnabled() {
        return true;
    }

    protected boolean hasTertiaryOptionButton() {
        return false;
    }

    protected boolean isTertiaryOptionButtonEnabled() {
        return true;
    }

    protected String getPrimaryOptionButtonLabel() {
        return "";
    }

    protected String getSecondaryOptionButtonLabel() {
        return "";
    }

    protected String getTertiaryOptionButtonLabel() {
        return "";
    }

    protected String getOptionSectionHintText() {
        return "§7这里可单独调整当前功能的附加参数。";
    }

    protected List<String> getPrimaryOptionTooltipLines() {
        return new ArrayList<>();
    }

    protected List<String> getSecondaryOptionTooltipLines() {
        return new ArrayList<>();
    }

    protected List<String> getTertiaryOptionTooltipLines() {
        return new ArrayList<>();
    }

    protected void onPrimaryOptionButtonPressed() throws IOException {
    }

    protected void onSecondaryOptionButtonPressed() throws IOException {
    }

    protected void onTertiaryOptionButtonPressed() throws IOException {
    }

    private static final class ConfigDropdown {

        private static final int ITEM_HEIGHT = 18;
        private static final int MAX_MENU_HEIGHT = ITEM_HEIGHT * 2;

        private final ThemedGuiScreen owner;
        private final String label;
        private final String[] options;

        private int x;
        private int y;
        private int width;
        private int height;
        private boolean expanded;
        private int selectedIndex;
        private boolean enabled = true;

        private ConfigDropdown(ThemedGuiScreen owner, String label, String[] options) {
            this.owner = owner;
            this.label = label == null ? "" : label;
            this.options = options == null ? new String[0] : options;
        }

        private void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (!enabled) {
                this.expanded = false;
            }
        }

        private void setSelectedIndex(int selectedIndex) {
            if (this.options.length == 0) {
                this.selectedIndex = 0;
                return;
            }
            this.selectedIndex = Math.max(0, Math.min(selectedIndex, this.options.length - 1));
        }

        private int getSelectedIndex() {
            return this.selectedIndex;
        }

        private int getVisibleMenuHeight() {
            return this.expanded ? this.options.length * ITEM_HEIGHT + 2 : 0;
        }

        private void collapse() {
            this.expanded = false;
        }

        private boolean isMouseOver(int mouseX, int mouseY) {
            if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
                return true;
            }
            return this.expanded && isInside(mouseX, mouseY, this.x, this.y + this.height + 2, this.width,
                    this.options.length * ITEM_HEIGHT);
        }

        private void draw(int mouseX, int mouseY) {
            GuiTheme.UiState state = !this.enabled
                    ? GuiTheme.UiState.DISABLED
                    : (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)
                            ? GuiTheme.UiState.HOVER
                            : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(this.x, this.y, this.width, this.height, state);
            String optionText = this.options.length == 0 ? "" : this.options[this.selectedIndex];
            String text = this.label + ": " + optionText;
            int textColor = this.enabled ? 0xFFFFFFFF : 0xFF8C98A6;
            this.owner.drawString(this.owner.mc.fontRenderer,
                    this.owner.mc.fontRenderer.trimStringToWidth(text, Math.max(10, this.width - 18)), this.x + 6,
                    this.y + 6, textColor);
            this.owner.drawString(this.owner.mc.fontRenderer, this.expanded ? "▲" : "▼", this.x + this.width - 10,
                    this.y + 6, this.enabled ? 0xFF9FDFFF : 0xFF6A7480);

            if (!this.expanded) {
                return;
            }

            int menuY = this.y + this.height + 2;
            int totalHeight = this.options.length * ITEM_HEIGHT;
            Gui.drawRect(this.x, menuY, this.x + this.width, menuY + totalHeight, 0xEE111A22);
            Gui.drawRect(this.x, menuY, this.x + this.width, menuY + 1, 0xFF6FB8FF);
            Gui.drawRect(this.x, menuY + totalHeight - 1, this.x + this.width, menuY + totalHeight, 0xFF35536C);
            Gui.drawRect(this.x, menuY, this.x + 1, menuY + totalHeight, 0xFF35536C);
            Gui.drawRect(this.x + this.width - 1, menuY, this.x + this.width, menuY + totalHeight, 0xFF35536C);

            for (int i = 0; i < this.options.length; i++) {
                int itemY = menuY + i * ITEM_HEIGHT;
                boolean hovered = isInside(mouseX, mouseY, this.x, itemY, this.width, ITEM_HEIGHT);
                boolean selected = i == this.selectedIndex;
                if (selected || hovered) {
                    Gui.drawRect(this.x + 1, itemY, this.x + this.width - 1, itemY + ITEM_HEIGHT,
                            selected ? 0xCC2B5A7C : 0xAA2E4258);
                }
                this.owner.drawString(this.owner.mc.fontRenderer, this.options[i], this.x + 6, itemY + 5, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY, int mouseButton) {
            if (!this.enabled || mouseButton != 0) {
                return false;
            }
            if (isInside(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
                this.expanded = !this.expanded;
                return true;
            }
            if (!this.expanded) {
                return false;
            }

            int menuY = this.y + this.height + 2;
            int totalHeight = this.options.length * ITEM_HEIGHT;
            if (isInside(mouseX, mouseY, this.x, menuY, this.width, totalHeight)) {
                int index = (mouseY - menuY) / ITEM_HEIGHT;
                if (index >= 0 && index < this.options.length) {
                    this.selectedIndex = index;
                }
                this.expanded = false;
                return true;
            }

            this.expanded = false;
            return false;
        }

        private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
