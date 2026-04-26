package com.zszl.zszlScriptMod.otherfeatures.gui.movement;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager.FeatureState;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager.LiquidWalkSettings;
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
import java.util.Locale;

public class GuiLavaWalkConfig extends ThemedGuiScreen {

    private static final int CONTROL_HEIGHT = 20;
    private static final int SCROLLBAR_TRACK_WIDTH = 4;
    private static final int SCROLLBAR_GUTTER = 10;
    private static final int SCROLL_BOTTOM_PADDING = 18;
    private static final int BTN_LIFT_STRENGTH = 3;
    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;
    private static final String[] ON_OFF_OPTIONS = { "开启", "关闭" };

    private final GuiScreen parentScreen;
    private final List<String> instructionLines = new ArrayList<>();
    private final List<String> descriptionLines = new ArrayList<>();

    private GuiButton liftStrengthButton;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private ConfigDropdown stateDropdown;
    private ConfigDropdown hudDropdown;
    private ConfigDropdown waterDropdown;
    private ConfigDropdown dangerousDropdown;
    private ConfigDropdown sneakDropdown;

    private boolean draftInitialized;
    private boolean draftEnabled;
    private boolean draftStatusHudEnabled;
    private float draftLiftStrength;
    private boolean draftWalkOnWater;
    private boolean draftDangerousOnly;
    private boolean draftSneakToDescend;

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
    private int liquidSectionY;
    private int waterHintY;
    private int dangerousHintY;
    private int sneakHintY;
    private int liquidSectionBottom;
    private int valueSectionY;
    private int valueLabelY;
    private int valueRangeY;
    private int valueSectionBottom;
    private int infoSectionY;
    private int infoRuntimeY;
    private int infoDraftY;
    private int infoSectionBottom;

    public GuiLavaWalkConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
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
            loadDraftFromConfig();
            this.draftInitialized = true;
        }
        relayoutControls();
        refreshControlTexts();
    }

    private void computeLayout() {
        FeatureState feature = getFeature();

        int maxWidth = Math.min(540, Math.max(380, this.width - 12));
        int maxUsableWidth = Math.max(260, this.width - 8);
        this.panelWidth = Math.min(maxWidth, maxUsableWidth);

        rebuildInstructionLines();
        rebuildDescriptionLines(feature);

        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int instructionHeight = this.instructionLines.size() * textStep;
        int infoBodyHeight = Math.max(44,
                this.descriptionLines.size() * textStep + 6 + this.fontRenderer.FONT_HEIGHT + 6 + this.fontRenderer.FONT_HEIGHT);
        int requiredHeight = 24 + instructionHeight + 8 + 18 + 10
                + 18 + textStep + 4 + CONTROL_HEIGHT + 8 + textStep + 4 + CONTROL_HEIGHT
                + 12 + 18 + textStep + 4 + CONTROL_HEIGHT + 8 + textStep + 4 + CONTROL_HEIGHT
                + 8 + textStep + 4 + CONTROL_HEIGHT
                + 12 + 18 + textStep + 4 + CONTROL_HEIGHT + 4 + this.fontRenderer.FONT_HEIGHT
                + 12 + 18 + infoBodyHeight
                + 8 + 28;
        int maxHeight = Math.max(280, this.height - 8);

        this.panelHeight = Math.min(Math.max(390, requiredHeight), maxHeight);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.contentTop = this.panelY + 24 + instructionHeight + 8;
        this.footerY = this.panelY + this.panelHeight - 28;
        this.contentBottom = this.footerY - 8;
    }

    private void rebuildInstructionLines() {
        this.instructionLines.clear();
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        String text = "左键其他功能页中的“液体行走”为总开关；这里继续细分水面、危险液体、潜行下沉和托举力度。界面与移动功能页统一，修改后通过底部按钮保存。";
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(text, instructionWidth));
    }

    private void rebuildDescriptionLines(FeatureState feature) {
        this.descriptionLines.clear();
        String text = feature == null
                ? "当前功能信息不可用，无法读取液体行走的说明。"
                : "§7" + feature.description
                        + " 这里的草稿设置会覆盖水面行走、仅危险液体与潜行下沉等细分选项。";
        int descriptionWidth = Math.max(120, this.panelWidth - 36);
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(text, descriptionWidth));
    }

    private void initControls() {
        this.liftStrengthButton = new ThemedButton(BTN_LIFT_STRENGTH, 0, 0, 120, CONTROL_HEIGHT, "");
        this.saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, CONTROL_HEIGHT, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, CONTROL_HEIGHT, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, CONTROL_HEIGHT, "取消");

        this.buttonList.add(this.liftStrengthButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);

        this.stateDropdown = new ConfigDropdown(this, "功能状态", ON_OFF_OPTIONS);
        this.hudDropdown = new ConfigDropdown(this, "状态HUD", ON_OFF_OPTIONS);
        this.waterDropdown = new ConfigDropdown(this, "水面可行走", ON_OFF_OPTIONS);
        this.dangerousDropdown = new ConfigDropdown(this, "仅危险液体", ON_OFF_OPTIONS);
        this.sneakDropdown = new ConfigDropdown(this, "潜行时穿过液体", ON_OFF_OPTIONS);
    }

    private void loadDraftFromConfig() {
        FeatureState feature = getFeature();
        LiquidWalkSettings settings = MovementFeatureManager.getLiquidWalkSettings();

        this.draftEnabled = feature != null && feature.isEnabled();
        this.draftStatusHudEnabled = feature == null || feature.isStatusHudEnabled();
        this.draftLiftStrength = feature == null ? 0.90F : feature.getValue();
        this.draftWalkOnWater = settings.isWalkOnWater();
        this.draftDangerousOnly = settings.isDangerousOnly();
        this.draftSneakToDescend = settings.isSneakToDescend();
    }

    private void relayoutControls() {
        computeLayout();

        FeatureState feature = getFeature();
        boolean featureAvailable = feature != null;
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
            this.stateDropdown.setEnabled(featureAvailable);
            this.stateDropdown.setBounds(fieldX, stateDropdownY, fieldWidth, CONTROL_HEIGHT);

            this.hudHintY = stateDropdownY + CONTROL_HEIGHT + this.stateDropdown.getVisibleMenuHeight() + 8;
            int hudDropdownY = this.hudHintY + textStep + 4;
            this.hudDropdown.setEnabled(featureAvailable);
            this.hudDropdown.setBounds(fieldX, hudDropdownY, fieldWidth, CONTROL_HEIGHT);
            this.stateSectionBottom = hudDropdownY + CONTROL_HEIGHT + this.hudDropdown.getVisibleMenuHeight();

            this.liquidSectionY = this.stateSectionBottom + sectionGap;
            this.waterHintY = this.liquidSectionY + sectionHeaderHeight;
            int waterDropdownY = this.waterHintY + textStep + 4;
            this.waterDropdown.setEnabled(featureAvailable);
            this.waterDropdown.setBounds(fieldX, waterDropdownY, fieldWidth, CONTROL_HEIGHT);

            this.dangerousHintY = waterDropdownY + CONTROL_HEIGHT + this.waterDropdown.getVisibleMenuHeight() + 8;
            int dangerousDropdownY = this.dangerousHintY + textStep + 4;
            this.dangerousDropdown.setEnabled(featureAvailable);
            this.dangerousDropdown.setBounds(fieldX, dangerousDropdownY, fieldWidth, CONTROL_HEIGHT);

            this.sneakHintY = dangerousDropdownY + CONTROL_HEIGHT + this.dangerousDropdown.getVisibleMenuHeight() + 8;
            int sneakDropdownY = this.sneakHintY + textStep + 4;
            this.sneakDropdown.setEnabled(featureAvailable);
            this.sneakDropdown.setBounds(fieldX, sneakDropdownY, fieldWidth, CONTROL_HEIGHT);
            this.liquidSectionBottom = sneakDropdownY + CONTROL_HEIGHT + this.sneakDropdown.getVisibleMenuHeight();

            this.valueSectionY = this.liquidSectionBottom + sectionGap;
            this.valueLabelY = this.valueSectionY + sectionHeaderHeight;
            this.liftStrengthButton.x = fieldX;
            this.liftStrengthButton.y = this.valueLabelY + textStep + 4;
            this.liftStrengthButton.width = fieldWidth;
            this.liftStrengthButton.height = CONTROL_HEIGHT;
            this.valueRangeY = this.liftStrengthButton.y + CONTROL_HEIGHT + 4;
            this.valueSectionBottom = this.valueRangeY + this.fontRenderer.FONT_HEIGHT;

            this.infoSectionY = this.valueSectionBottom + sectionGap;
            int currentY = this.infoSectionY + sectionHeaderHeight;
            currentY += this.descriptionLines.size() * textStep;
            currentY += 6;
            this.infoRuntimeY = currentY;
            currentY += this.fontRenderer.FONT_HEIGHT + 6;
            this.infoDraftY = currentY;
            currentY += this.fontRenderer.FONT_HEIGHT;
            this.infoSectionBottom = currentY;

            int totalContentHeight = this.infoSectionBottom - baseY + SCROLL_BOTTOM_PADDING;
            this.maxContentScroll = Math.max(0, totalContentHeight - visibleContentHeight);
            int clampedScroll = clampInt(this.contentScrollOffset, 0, this.maxContentScroll);
            if (clampedScroll == this.contentScrollOffset) {
                break;
            }
            this.contentScrollOffset = clampedScroll;
        }

        this.liftStrengthButton.visible = featureAvailable
                && isVisibleInScrollableViewport(this.liftStrengthButton.y, this.liftStrengthButton.height);
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

    private FeatureState getFeature() {
        return MovementFeatureManager.getFeature("lava_walk");
    }

    private void refreshControlTexts() {
        FeatureState feature = getFeature();
        boolean featureAvailable = feature != null;

        this.stateDropdown.setEnabled(featureAvailable);
        this.hudDropdown.setEnabled(featureAvailable);
        this.waterDropdown.setEnabled(featureAvailable);
        this.dangerousDropdown.setEnabled(featureAvailable);
        this.sneakDropdown.setEnabled(featureAvailable);

        this.stateDropdown.setSelectedIndex(this.draftEnabled ? 0 : 1);
        this.hudDropdown.setSelectedIndex(this.draftStatusHudEnabled ? 0 : 1);
        this.waterDropdown.setSelectedIndex(this.draftWalkOnWater ? 0 : 1);
        this.dangerousDropdown.setSelectedIndex(this.draftDangerousOnly ? 0 : 1);
        this.sneakDropdown.setSelectedIndex(this.draftSneakToDescend ? 0 : 1);

        this.liftStrengthButton.enabled = featureAvailable;
        this.liftStrengthButton.displayString = "托举力度: " + formatFloat(this.draftLiftStrength);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        FeatureState feature = getFeature();
        switch (button.id) {
        case BTN_LIFT_STRENGTH:
            if (feature != null) {
                openLiftStrengthInput(feature);
            }
            return;
        case BTN_SAVE:
            saveDraft(feature);
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        case BTN_DEFAULT:
            applyDefaults(feature);
            break;
        case BTN_CANCEL:
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        default:
            break;
        }

        refreshControlTexts();
        relayoutControls();
    }

    private void openLiftStrengthInput(FeatureState feature) {
        String title = "输入托举力度 (" + formatFloat(feature.minValue) + " - " + formatFloat(feature.maxValue) + ")";
        this.mc.displayGuiScreen(new GuiTextInput(this, title, formatFloat(this.draftLiftStrength), value -> {
            float parsed = this.draftLiftStrength;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            this.draftLiftStrength = clampFloat(parsed, feature.minValue, feature.maxValue);
            refreshControlTexts();
            relayoutControls();
            this.mc.displayGuiScreen(this);
        }));
    }

    private void saveDraft(FeatureState feature) {
        if (feature == null) {
            return;
        }
        syncDraftFromControls(feature);
        MovementFeatureManager.setEnabled("lava_walk", this.draftEnabled);
        MovementFeatureManager.setFeatureStatusHudEnabled("lava_walk", this.draftStatusHudEnabled);
        MovementFeatureManager.setValue("lava_walk", clampFloat(this.draftLiftStrength, feature.minValue, feature.maxValue));
        MovementFeatureManager.setLiquidWalkSettings(this.draftWalkOnWater, this.draftDangerousOnly, this.draftSneakToDescend);
    }

    private void applyDefaults(FeatureState feature) {
        this.draftEnabled = false;
        this.draftStatusHudEnabled = true;
        this.draftLiftStrength = feature == null ? 0.90F : feature.defaultValue;
        this.draftWalkOnWater = LiquidWalkSettings.DEFAULT_WALK_ON_WATER;
        this.draftDangerousOnly = LiquidWalkSettings.DEFAULT_DANGEROUS_ONLY;
        this.draftSneakToDescend = LiquidWalkSettings.DEFAULT_SNEAK_TO_DESCEND;
        refreshControlTexts();
    }

    private void syncDraftFromControls(FeatureState feature) {
        this.draftEnabled = this.stateDropdown.getSelectedIndex() == 0;
        this.draftStatusHudEnabled = this.hudDropdown.getSelectedIndex() == 0;
        this.draftWalkOnWater = this.waterDropdown.getSelectedIndex() == 0;
        this.draftDangerousOnly = this.dangerousDropdown.getSelectedIndex() == 0;
        this.draftSneakToDescend = this.sneakDropdown.getSelectedIndex() == 0;
        if (feature != null) {
            this.draftLiftStrength = clampFloat(this.draftLiftStrength, feature.minValue, feature.maxValue);
        }
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
            collapseAllDropdowns();
            beginScrollbarDrag(mouseY);
            return true;
        }

        if (isPointInsideScrollableViewport(mouseX, mouseY) && this.stateDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseDropdownsExcept(this.stateDropdown);
            this.draftEnabled = this.stateDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }
        if (isPointInsideScrollableViewport(mouseX, mouseY) && this.hudDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseDropdownsExcept(this.hudDropdown);
            this.draftStatusHudEnabled = this.hudDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }
        if (isPointInsideScrollableViewport(mouseX, mouseY) && this.waterDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseDropdownsExcept(this.waterDropdown);
            this.draftWalkOnWater = this.waterDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }
        if (isPointInsideScrollableViewport(mouseX, mouseY)
                && this.dangerousDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseDropdownsExcept(this.dangerousDropdown);
            this.draftDangerousOnly = this.dangerousDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }
        if (isPointInsideScrollableViewport(mouseX, mouseY) && this.sneakDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            collapseDropdownsExcept(this.sneakDropdown);
            this.draftSneakToDescend = this.sneakDropdown.getSelectedIndex() == 0;
            refreshControlTexts();
            relayoutControls();
            return true;
        }

        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }

        if (!isMouseOverAnyDropdown(mouseX, mouseY)) {
            collapseAllDropdowns();
        }

        return false;
    }

    private void collapseDropdownsExcept(ConfigDropdown keep) {
        if (this.stateDropdown != keep) {
            this.stateDropdown.collapse();
        }
        if (this.hudDropdown != keep) {
            this.hudDropdown.collapse();
        }
        if (this.waterDropdown != keep) {
            this.waterDropdown.collapse();
        }
        if (this.dangerousDropdown != keep) {
            this.dangerousDropdown.collapse();
        }
        if (this.sneakDropdown != keep) {
            this.sneakDropdown.collapse();
        }
    }

    private void collapseAllDropdowns() {
        collapseDropdownsExcept(null);
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

        FeatureState feature = getFeature();
        if (feature != null) {
            syncDraftFromControls(feature);
        }

        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "液体行走设置", this.fontRenderer);
        drawInstructionText();
        drawStatusStrip(feature);
        drawScrollableContent(mouseX, mouseY, partialTicks, feature);
        drawContentScrollbar();
        drawFixedButtons(mouseX, mouseY, partialTicks);
        drawTopLevelTooltips(mouseX, mouseY, feature);
    }

    private void drawInstructionText() {
        int textY = this.panelY + 22;
        int lineStep = this.fontRenderer.FONT_HEIGHT + 2;
        for (String line : this.instructionLines) {
            drawString(this.fontRenderer, line, this.panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += lineStep;
        }
    }

    private void drawStatusStrip(FeatureState feature) {
        int stripX = this.panelX + 14;
        int stripY = this.contentTop;
        int stripW = this.panelWidth - 28;
        int stripH = 18;
        drawRect(stripX, stripY, stripX + stripW, stripY + stripH, 0x33202A36);
        drawHorizontalLine(stripX, stripX + stripW, stripY, 0x664FA6D9);
        drawHorizontalLine(stripX, stripX + stripW, stripY + stripH, 0x4435536C);

        String modeText = this.draftDangerousOnly ? "危险液体" : (this.draftWalkOnWater ? "全部液体" : "排除水面");
        String status = "状态: " + (this.draftEnabled ? "§a开启" : "§c关闭")
                + (feature == null ? " §7| 功能: §c未找到" : " §7| 功能: §f" + feature.name)
                + " §7| HUD: " + (this.draftStatusHudEnabled ? "§a开" : "§c关")
                + (MovementFeatureManager.isMasterStatusHudEnabled() ? " §7/总开" : " §c/总关")
                + " §7| 模式: §f" + modeText
                + " §7| 下沉: " + (this.draftSneakToDescend ? "§a开" : "§c关");
        if (feature != null) {
            status += " §7| 运行: §f" + MovementFeatureManager.getFeatureRuntimeSummary("lava_walk");
        }
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(status, stripW - 12), stripX + 6, stripY + 5,
                0xFFFFFFFF);
    }

    private void drawScrollableContent(int mouseX, int mouseY, float partialTicks, FeatureState feature) {
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
            drawSectionBox("液体策略", rowX, this.waterHintY, rowRight, this.liquidSectionBottom);
            drawSectionBox("液体参数", rowX, this.valueLabelY, rowRight, this.valueSectionBottom);
            drawSectionBox("运行说明", rowX, this.infoSectionY + 18, rowRight, this.infoSectionBottom);

            if (isVisibleInScrollableViewport(this.stateHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7总开关与 HUD 草稿需要通过底部“保存并关闭”提交。", rowX, this.stateHintY,
                        0xFFB6C5D6);
            }
            this.stateDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.hudHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7单项 HUD 默认为开启，但仍会受到顶部“总状态HUD”控制。", rowX, this.hudHintY,
                        0xFFB6C5D6);
            }
            this.hudDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.waterHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7关闭后，普通水面不会被当成平台；危险液体仍可按下方设置处理。", rowX, this.waterHintY,
                        0xFFB6C5D6);
            }
            this.waterDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.dangerousHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7开启后只保留危险液体表面行走，普通水面设置会被忽略。", rowX, this.dangerousHintY,
                        0xFFB6C5D6);
            }
            this.dangerousDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.sneakHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7开启后按住潜行会临时关闭液体平台效果，方便主动下沉。", rowX, this.sneakHintY,
                        0xFFB6C5D6);
            }
            this.sneakDropdown.draw(clippedMouseX, clippedMouseY);

            if (isVisibleInScrollableViewport(this.valueLabelY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "托举力度:", rowX, this.valueLabelY, 0xFFE8F6FF);
            }
            if (this.liftStrengthButton.visible) {
                this.liftStrengthButton.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
            }
            if (feature != null && isVisibleInScrollableViewport(this.valueRangeY, this.fontRenderer.FONT_HEIGHT)) {
                drawString(this.fontRenderer,
                        "§8范围 " + formatFloat(feature.minValue) + " - " + formatFloat(feature.maxValue),
                        rowX, this.valueRangeY, 0xFF9FB2C8);
            }

            int currentY = this.infoSectionY + 18;
            for (String line : this.descriptionLines) {
                if (isVisibleInScrollableViewport(currentY, this.fontRenderer.FONT_HEIGHT + 2)) {
                    drawString(this.fontRenderer, line, rowX, currentY, 0xFFE2F2FF);
                }
                currentY += this.fontRenderer.FONT_HEIGHT + 2;
            }

            if (isVisibleInScrollableViewport(this.infoRuntimeY, this.fontRenderer.FONT_HEIGHT + 2)) {
                String runtime = feature == null
                        ? "§6运行状态: 未找到对应功能"
                        : "§e运行状态: §f" + MovementFeatureManager.getFeatureRuntimeSummary("lava_walk");
                drawString(this.fontRenderer, runtime, rowX, this.infoRuntimeY, 0xFFFFFFFF);
            }

            if (isVisibleInScrollableViewport(this.infoDraftY, this.fontRenderer.FONT_HEIGHT + 2)) {
                String draft = "§7草稿设置: §f托举 "
                        + formatFloat(this.draftLiftStrength)
                        + "，水面 " + (this.draftWalkOnWater ? "开" : "关")
                        + "，危险液体 " + (this.draftDangerousOnly ? "开" : "关")
                        + "，潜行下沉 " + (this.draftSneakToDescend ? "开" : "关");
                drawString(this.fontRenderer, draft, rowX, this.infoDraftY, 0xFFFFFFFF);
            }
        } finally {
            endScissor();
        }
    }

    private void drawFixedButtons(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : this.buttonList) {
            if (isFooterButton(button) && button.visible) {
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

    private void drawTopLevelTooltips(int mouseX, int mouseY, FeatureState feature) {
        if (feature != null && isMouseOverStateDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e功能状态",
                    "§7当前草稿: " + (this.draftEnabled ? "§a开启" : "§c关闭"),
                    "§7控制液体行走功能的总开关。"), mouseX, mouseY);
            return;
        }
        if (feature != null && isMouseOverHudDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e状态HUD",
                    "§7当前草稿: " + (this.draftStatusHudEnabled ? "§a开启" : "§c关闭"),
                    "§7控制液体行走是否在 HUD 中显示状态。",
                    "§7即使这里开启，也仍受顶部“总状态HUD”控制。"), mouseX, mouseY);
            return;
        }
        if (feature != null && isMouseOverWaterDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e水面可行走",
                    "§7当前草稿: " + (this.draftWalkOnWater ? "§a开启" : "§c关闭"),
                    "§7决定普通水面是否也被当成平台。"), mouseX, mouseY);
            return;
        }
        if (feature != null && isMouseOverDangerousDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e仅危险液体",
                    "§7当前草稿: " + (this.draftDangerousOnly ? "§a开启" : "§c关闭"),
                    "§7开启后只保留危险液体表面行走。"), mouseX, mouseY);
            return;
        }
        if (feature != null && isMouseOverSneakDropdown(mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e潜行时穿过液体",
                    "§7当前草稿: " + (this.draftSneakToDescend ? "§a开启" : "§c关闭"),
                    "§7开启后，按住潜行可主动沉入液体。"), mouseX, mouseY);
            return;
        }
        if (feature != null && isMouseOver(mouseX, mouseY, this.liftStrengthButton)) {
            drawHoveringText(Arrays.asList("§e托举力度",
                    "§7当前草稿: §f" + formatFloat(this.draftLiftStrength),
                    "§7范围: §f" + formatFloat(feature.minValue) + " - " + formatFloat(feature.maxValue),
                    "§7点击后会弹出输入框。"), mouseX, mouseY);
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

    private boolean isMouseOverWaterDropdown(int mouseX, int mouseY) {
        return isPointInsideScrollableViewport(mouseX, mouseY) && this.waterDropdown.isMouseOver(mouseX, mouseY);
    }

    private boolean isMouseOverDangerousDropdown(int mouseX, int mouseY) {
        return isPointInsideScrollableViewport(mouseX, mouseY) && this.dangerousDropdown.isMouseOver(mouseX, mouseY);
    }

    private boolean isMouseOverSneakDropdown(int mouseX, int mouseY) {
        return isPointInsideScrollableViewport(mouseX, mouseY) && this.sneakDropdown.isMouseOver(mouseX, mouseY);
    }

    private boolean isMouseOverAnyDropdown(int mouseX, int mouseY) {
        return isMouseOverStateDropdown(mouseX, mouseY)
                || isMouseOverHudDropdown(mouseX, mouseY)
                || isMouseOverWaterDropdown(mouseX, mouseY)
                || isMouseOverDangerousDropdown(mouseX, mouseY)
                || isMouseOverSneakDropdown(mouseX, mouseY);
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
