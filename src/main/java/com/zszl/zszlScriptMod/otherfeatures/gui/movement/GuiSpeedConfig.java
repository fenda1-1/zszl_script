package com.zszl.zszlScriptMod.otherfeatures.gui.movement;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiSpeedConfig extends ThemedGuiScreen {

    private static final int CONTROL_HEIGHT = 20;
    private static final int DROPDOWN_ITEM_HEIGHT = 18;
    private static final int MAX_DROPDOWN_MENU_HEIGHT = 2 + DROPDOWN_ITEM_HEIGHT * 4;
    private static final int SCROLL_BOTTOM_PADDING = 18;

    private static final int BTN_TIMER_TOGGLE = 10;
    private static final int BTN_STATUS_HUD = 13;

    private static final int BTN_TIMER_SPEED = 20;
    private static final int BTN_JUMP_HEIGHT = 21;
    private static final int BTN_VANILLA_SPEED = 22;

    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;

    private final GuiScreen parentScreen;
    private final List<String> instructionLines = new ArrayList<>();

    private ToggleGuiButton timerToggleButton;
    private ToggleGuiButton statusHudButton;
    private GuiButton timerSpeedButton;
    private GuiButton jumpHeightButton;
    private GuiButton vanillaSpeedButton;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private SimpleDropdown modeDropdown;
    private SimpleDropdown presetDropdown;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int contentScrollOffset;
    private int contentMaxScroll;

    private final class SimpleDropdown {
        private int x;
        private int y;
        private int width;
        private int height;
        private final String label;
        private final String[] options;
        private boolean expanded;
        private int selectedIndex;

        private SimpleDropdown(String label, String[] options, int selectedIndex) {
            this.label = label;
            this.options = options == null ? new String[0] : options;
            this.selectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, this.options.length - 1)));
        }

        private void draw(int mouseX, int mouseY) {
            boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
            GuiTheme.drawButtonFrameSafe(x, y, width, height,
                    hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(label + ": " + getSelectedText(), width - 14),
                    x + 6, y + 6, 0xFFFFFFFF);
            drawString(fontRenderer, expanded ? "▲" : "▼", x + width - 10, y + 6, 0xFF9FDFFF);

            if (!expanded) {
                return;
            }

            int totalHeight = getExpandedHeight() - 2;
            drawRect(x, y + height + 2, x + width, y + height + 2 + totalHeight, 0xEE111A22);
            drawHorizontalLine(x, x + width, y + height + 2, 0xFF6FB8FF);
            drawHorizontalLine(x, x + width, y + height + 2 + totalHeight, 0xFF35536C);
            drawVerticalLine(x, y + height + 2, y + height + 2 + totalHeight, 0xFF35536C);
            drawVerticalLine(x + width, y + height + 2, y + height + 2 + totalHeight, 0xFF35536C);
            for (int i = 0; i < options.length; i++) {
                int itemY = y + height + 2 + i * DROPDOWN_ITEM_HEIGHT;
                boolean itemHovered = isInside(mouseX, mouseY, x, itemY, width, DROPDOWN_ITEM_HEIGHT);
                boolean selected = i == selectedIndex;
                if (selected || itemHovered) {
                    drawRect(x + 1, itemY, x + width - 1, itemY + DROPDOWN_ITEM_HEIGHT,
                            selected ? 0xCC2B5A7C : 0xAA2E4258);
                }
                drawString(fontRenderer, options[i], x + 6, itemY + 5, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY, int mouseButton) {
            if (mouseButton != 0) {
                return false;
            }
            boolean insideViewport = isPointInsideScrollableViewport(mouseX, mouseY);
            if (insideViewport && isInside(mouseX, mouseY, x, y, width, height)) {
                expanded = !expanded;
                return true;
            }
            if (!expanded) {
                return false;
            }

            int totalHeight = getExpandedHeight() - 2;
            if (insideViewport && isInside(mouseX, mouseY, x, y + height + 2, width, totalHeight)) {
                int index = (mouseY - (y + height + 2)) / DROPDOWN_ITEM_HEIGHT;
                if (index >= 0 && index < options.length) {
                    selectedIndex = index;
                    onDropdownValueChanged(this, index);
                }
                expanded = false;
                return true;
            }

            expanded = false;
            return false;
        }

        private void collapse() {
            expanded = false;
        }

        private int getExpandedHeight() {
            return options.length <= 0 ? 0 : 2 + DROPDOWN_ITEM_HEIGHT * options.length;
        }

        private int getVisibleMenuHeight() {
            return expanded ? getExpandedHeight() : 0;
        }

        private String getSelectedText() {
            return options.length == 0 ? "" : options[selectedIndex];
        }
    }

    public GuiSpeedConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.contentScrollOffset = 0;
        this.contentMaxScroll = 0;
        recalcLayout();
        initControls();
        relayoutControls();
    }

    private void recalcLayout() {
        int maxWidth = Math.min(540, Math.max(360, this.width - 12));
        int maxUsableWidth = Math.max(240, this.width - 8);
        this.panelWidth = Math.min(maxWidth, maxUsableWidth);

        this.instructionLines.clear();
        String instruction = "左键其他功能页中的“加速”为总开关；这里可切换 Ground/Air/Bhop/LowHop/OnGround 模式、选择预设，并单独控制 Timer 与单项状态 HUD。单项 HUD 仍会受到顶部“总状态HUD”控制。";
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(instruction, instructionWidth));

        int instructionHeight = this.instructionLines.size() * (this.fontRenderer.FONT_HEIGHT + 2);
        int requiredHeight = 24 + instructionHeight + 8 + 18 + 10
                + 18 + CONTROL_HEIGHT + MAX_DROPDOWN_MENU_HEIGHT
                + 12 + 18 + (CONTROL_HEIGHT + 6) + CONTROL_HEIGHT
                + 12 + 18 + CONTROL_HEIGHT
                + 8 + 28;
        int maxHeight = Math.max(240, this.height - 8);
        this.panelHeight = Math.min(Math.max(360, requiredHeight), maxHeight);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.contentTop = this.panelY + 24 + instructionHeight + 8;
        int footerTop = this.panelY + this.panelHeight - 28;
        this.contentBottom = footerTop - 8;
    }

    private void initControls() {
        modeDropdown = new SimpleDropdown("模式",
                new String[] { "Ground", "Air", "Bhop", "LowHop", "OnGround" },
                getModeIndex(SpeedHandler.speedMode));
        presetDropdown = new SimpleDropdown("预设", new String[] { "稳妥", "平衡", "激进", "自定义" },
                getPresetIndex(SpeedHandler.presetId));

        timerToggleButton = new ToggleGuiButton(BTN_TIMER_TOGGLE, 0, 0, 100, 20, "", SpeedHandler.useTimerBoost);
        statusHudButton = new ToggleGuiButton(BTN_STATUS_HUD, 0, 0, 100, 20, "", SpeedHandler.showStatusHud);

        timerSpeedButton = new ThemedButton(BTN_TIMER_SPEED, 0, 0, 100, 20, "");
        jumpHeightButton = new ThemedButton(BTN_JUMP_HEIGHT, 0, 0, 100, 20, "");
        vanillaSpeedButton = new ThemedButton(BTN_VANILLA_SPEED, 0, 0, 100, 20, "");
        saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, 20, "§a保存并关闭");
        defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, 20, "§e恢复默认");
        cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, 20, "取消");

        this.buttonList.add(timerToggleButton);
        this.buttonList.add(statusHudButton);
        this.buttonList.add(timerSpeedButton);
        this.buttonList.add(jumpHeightButton);
        this.buttonList.add(vanillaSpeedButton);
        this.buttonList.add(saveButton);
        this.buttonList.add(defaultButton);
        this.buttonList.add(cancelButton);

        refreshControlTexts();
    }

    private void relayoutControls() {
        recalcLayout();

        int innerPadding = 12;
        int columnGap = 8;
        int sectionGap = 12;
        int sectionHeaderHeight = 18;
        int rowGap = 6;
        int rowStep = CONTROL_HEIGHT + rowGap;
        int buttonW = Math.max(110, (this.panelWidth - innerPadding * 2 - columnGap) / 2);
        int leftX = this.panelX + innerPadding;
        int rightX = this.panelX + this.panelWidth - innerPadding - buttonW;
        int layoutBaseY = getScrollableViewportTop();
        int visibleTop = layoutBaseY;
        int visibleBottom = this.contentBottom;
        int baseY = layoutBaseY - this.contentScrollOffset;

        int modeSectionY = baseY;
        int modeBodyY = modeSectionY + sectionHeaderHeight;
        modeDropdown.x = leftX;
        modeDropdown.y = modeBodyY;
        modeDropdown.width = buttonW;
        modeDropdown.height = CONTROL_HEIGHT;

        presetDropdown.x = rightX;
        presetDropdown.y = modeBodyY;
        presetDropdown.width = buttonW;
        presetDropdown.height = CONTROL_HEIGHT;

        int dropdownReserve = Math.max(modeDropdown.getVisibleMenuHeight(), presetDropdown.getVisibleMenuHeight());
        int modeSectionBottom = modeBodyY + CONTROL_HEIGHT + dropdownReserve;

        int speedSectionY = modeSectionBottom + sectionGap;
        int speedBodyY = speedSectionY + sectionHeaderHeight;
        timerToggleButton.x = leftX;
        timerToggleButton.y = speedBodyY;
        timerToggleButton.width = buttonW;
        timerToggleButton.height = CONTROL_HEIGHT;

        timerSpeedButton.x = rightX;
        timerSpeedButton.y = speedBodyY;
        timerSpeedButton.width = buttonW;
        timerSpeedButton.height = CONTROL_HEIGHT;

        jumpHeightButton.x = leftX;
        jumpHeightButton.y = speedBodyY + rowStep;
        jumpHeightButton.width = buttonW;
        jumpHeightButton.height = CONTROL_HEIGHT;

        vanillaSpeedButton.x = rightX;
        vanillaSpeedButton.y = speedBodyY + rowStep;
        vanillaSpeedButton.width = buttonW;
        vanillaSpeedButton.height = CONTROL_HEIGHT;

        int speedSectionBottom = speedBodyY + rowStep + CONTROL_HEIGHT;
        int safetySectionY = speedSectionBottom + sectionGap;
        int safetyBodyY = safetySectionY + sectionHeaderHeight;
        statusHudButton.x = leftX;
        statusHudButton.y = safetyBodyY;
        statusHudButton.width = buttonW;
        statusHudButton.height = CONTROL_HEIGHT;

        int totalContentHeight = (safetyBodyY + CONTROL_HEIGHT) - baseY + SCROLL_BOTTOM_PADDING;
        int visibleContentHeight = Math.max(24, visibleBottom - visibleTop);
        this.contentMaxScroll = Math.max(0, totalContentHeight - visibleContentHeight);
        this.contentScrollOffset = clampInt(this.contentScrollOffset, 0, this.contentMaxScroll);

        if (this.contentScrollOffset != 0) {
            baseY = layoutBaseY - this.contentScrollOffset;
            modeSectionY = baseY;
            modeBodyY = modeSectionY + sectionHeaderHeight;
            modeDropdown.y = modeBodyY;
            presetDropdown.y = modeBodyY;
            int adjustedModeSectionBottom = modeBodyY + CONTROL_HEIGHT
                    + Math.max(modeDropdown.getVisibleMenuHeight(), presetDropdown.getVisibleMenuHeight());
            speedSectionY = adjustedModeSectionBottom + sectionGap;
            speedBodyY = speedSectionY + sectionHeaderHeight;
            timerToggleButton.y = speedBodyY;
            timerSpeedButton.y = speedBodyY;
            jumpHeightButton.y = speedBodyY + rowStep;
            vanillaSpeedButton.y = speedBodyY + rowStep;
            int adjustedSpeedSectionBottom = speedBodyY + rowStep + CONTROL_HEIGHT;
            safetySectionY = adjustedSpeedSectionBottom + sectionGap;
            safetyBodyY = safetySectionY + sectionHeaderHeight;
            statusHudButton.y = safetyBodyY;
        }

        int footerGap = 6;
        int footerButtonW = Math.max(64, (this.panelWidth - innerPadding * 2 - footerGap * 2) / 3);
        int footerTotalW = footerButtonW * 3 + footerGap * 2;
        int footerStartX = this.panelX + (this.panelWidth - footerTotalW) / 2;
        int footerY = this.panelY + this.panelHeight - 28;

        layoutFooterButton(saveButton, footerStartX, footerY, footerButtonW);
        layoutFooterButton(defaultButton, footerStartX + footerButtonW + footerGap, footerY, footerButtonW);
        layoutFooterButton(cancelButton, footerStartX + (footerButtonW + footerGap) * 2, footerY, footerButtonW);

        timerToggleButton.visible = isWithinContent(timerToggleButton.y, timerToggleButton.height, visibleTop, visibleBottom);
        statusHudButton.visible = isWithinContent(statusHudButton.y, statusHudButton.height, visibleTop, visibleBottom);
        timerSpeedButton.visible = isWithinContent(timerSpeedButton.y, timerSpeedButton.height, visibleTop, visibleBottom);
        jumpHeightButton.visible = isWithinContent(jumpHeightButton.y, jumpHeightButton.height, visibleTop, visibleBottom);
        vanillaSpeedButton.visible = isWithinContent(vanillaSpeedButton.y, vanillaSpeedButton.height, visibleTop, visibleBottom);
    }

    private void layoutFooterButton(GuiButton button, int x, int y, int width) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = 20;
        button.visible = true;
    }

    private void refreshControlTexts() {
        modeDropdown.selectedIndex = getModeIndex(SpeedHandler.speedMode);
        presetDropdown.selectedIndex = getPresetIndex(SpeedHandler.presetId);

        timerToggleButton.setEnabledState(SpeedHandler.useTimerBoost);
        timerToggleButton.displayString = "Timer加速: " + stateText(SpeedHandler.useTimerBoost);

        statusHudButton.setEnabledState(SpeedHandler.showStatusHud);
        statusHudButton.displayString = "单项状态HUD: " + stateText(SpeedHandler.showStatusHud);

        timerSpeedButton.displayString = "Timer倍率: " + formatFloat(SpeedHandler.timerSpeed);
        jumpHeightButton.displayString = "跳跃高度: " + formatFloat(SpeedHandler.jumpHeight);
        vanillaSpeedButton.displayString = "水平速度: " + formatFloat(SpeedHandler.vanillaSpeed);

        jumpHeightButton.enabled = SpeedHandler.usesJumpHeight();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_TIMER_TOGGLE:
            SpeedHandler.useTimerBoost = !SpeedHandler.useTimerBoost;
            SpeedHandler.markCustomPreset();
            break;
        case BTN_STATUS_HUD:
            SpeedHandler.showStatusHud = !SpeedHandler.showStatusHud;
            break;
        case BTN_TIMER_SPEED:
            openFloatInput("输入 Timer 倍率 (1.00 - 2.50)", SpeedHandler.timerSpeed, 1.00F, 2.50F,
                    value -> {
                        SpeedHandler.timerSpeed = value;
                        SpeedHandler.markCustomPreset();
                    });
            return;
        case BTN_JUMP_HEIGHT:
            if (jumpHeightButton.enabled) {
                openFloatInput("输入起跳高度 (0.00 - 1.00)", SpeedHandler.jumpHeight, 0.00F, 1.00F,
                        value -> {
                            SpeedHandler.jumpHeight = value;
                            SpeedHandler.markCustomPreset();
                        });
            }
            return;
        case BTN_VANILLA_SPEED:
            openFloatInput("输入水平速度 (0.10 - 3.00)", SpeedHandler.vanillaSpeed, 0.10F, 3.00F,
                    value -> {
                        SpeedHandler.vanillaSpeed = value;
                        SpeedHandler.markCustomPreset();
                    });
            return;
        case BTN_SAVE:
            SpeedHandler.saveConfig();
            mc.displayGuiScreen(parentScreen);
            return;
        case BTN_DEFAULT:
            SpeedHandler.applyPreset(SpeedHandler.PRESET_BALANCED);
            SpeedHandler.showStatusHud = true;
            break;
        case BTN_CANCEL:
            SpeedHandler.loadConfig();
            mc.displayGuiScreen(parentScreen);
            return;
        default:
            break;
        }

        refreshControlTexts();
        relayoutControls();
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
                        : Math.min(this.contentMaxScroll, this.contentScrollOffset + 14);
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
        if (modeDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            presetDropdown.collapse();
            relayoutControls();
            return true;
        }
        if (presetDropdown.handleClick(mouseX, mouseY, mouseButton)) {
            modeDropdown.collapse();
            relayoutControls();
            return true;
        }
        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }
        if (modeDropdown.expanded && !isMouseOverDropdown(modeDropdown, mouseX, mouseY)) {
            modeDropdown.collapse();
        }
        if (presetDropdown.expanded && !isMouseOverDropdown(presetDropdown, mouseX, mouseY)) {
            presetDropdown.collapse();
        }
        return false;
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

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "加速设置", this.fontRenderer);

        int textY = panelY + 22;
        int lineStep = this.fontRenderer.FONT_HEIGHT + 2;
        for (String line : instructionLines) {
            this.drawString(this.fontRenderer, line, panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += lineStep;
        }

        drawStatusStrip();
        drawScrollableContent(mouseX, mouseY, partialTicks);
        drawContentScrollbar();
        drawFixedButtons(mouseX, mouseY, partialTicks);

        if (isMouseOverDropdown(modeDropdown, mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e模式", "§7当前: " + SpeedHandler.getModeDisplayName(),
                    "§7Ground：纯地面贴地加速，不主动起跳。",
                    "§7Air：自动起跳后在空中持续加速。",
                    "§7Bhop：自动连跳加速，节奏最明显。",
                    "§7LowHop：低跳加速，弹跳更低更贴地。",
                    "§7OnGround：强制贴地加速，尽量维持地面判定。"), mouseX, mouseY);
        } else if (isMouseOverDropdown(presetDropdown, mouseX, mouseY)) {
            drawHoveringText(Arrays.asList("§e预设", "§7当前: " + SpeedHandler.getPresetDisplayName(),
                    "§7稳妥：更保守，优先稳定。", "§7平衡：默认通用。", "§7激进：更高节奏。", "§7自定义：手动参数。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, timerToggleButton)) {
            drawHoveringText(Arrays.asList("§eTimer加速", "§7当前: " + stateText(SpeedHandler.useTimerBoost),
                    "§7关闭后只保留移动速度逻辑，不修改客户端 tickLength。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, timerSpeedButton)) {
            drawHoveringText(Arrays.asList("§eTimer倍率", "§7当前: " + formatFloat(SpeedHandler.timerSpeed),
                    "§7仅在开启 Timer 加速时生效。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, jumpHeightButton)) {
            drawHoveringText(Arrays.asList("§e跳跃高度", "§7当前: " + formatFloat(SpeedHandler.jumpHeight),
                    "§7Air / Bhop / LowHop 模式会使用这个值。",
                    "§7Ground / OnGround 模式不会使用跳跃高度。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, vanillaSpeedButton)) {
            drawHoveringText(Arrays.asList("§e水平速度", "§7当前: " + formatFloat(SpeedHandler.vanillaSpeed),
                    "§7所有模式都会把它作为水平速度基准。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, statusHudButton)) {
            drawHoveringText(Arrays.asList("§e单项状态HUD", "§7当前: " + stateText(SpeedHandler.showStatusHud),
                    "§7开启后，在主界面左上角显示当前模式和参数状态。",
                    "§7仍受顶部“总状态HUD”总开关控制。"), mouseX, mouseY);
        }
    }

    private void drawStatusStrip() {
        int stripX = panelX + 14;
        int stripY = contentTop;
        int stripW = panelWidth - 28;
        int stripH = 18;
        drawRect(stripX, stripY, stripX + stripW, stripY + stripH, 0x33202A36);
        drawHorizontalLine(stripX, stripX + stripW, stripY, 0x664FA6D9);
        drawHorizontalLine(stripX, stripX + stripW, stripY + stripH, 0x4435536C);
        String status = "状态: " + (SpeedHandler.enabled ? "§a开启" : "§c关闭")
                + " §7| 模式: §f" + SpeedHandler.getModeDisplayName()
                + " §7| 预设: §f" + SpeedHandler.getPresetDisplayName()
                + " §7| Timer: " + (SpeedHandler.useTimerBoost ? "§a开" : "§c关")
                + " §7| HUD: " + (SpeedHandler.showStatusHud ? "§a单开" : "§c单关")
                + (MovementFeatureManager.isMasterStatusHudEnabled() ? " §7/总开" : " §c/总关");
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(status, stripW - 12), stripX + 6, stripY + 5,
                0xFFFFFFFF);
    }

    private void drawScrollableContent(int mouseX, int mouseY, float partialTicks) {
        int viewportX = getScrollableViewportLeft();
        int viewportY = getScrollableViewportTop();
        int viewportW = getScrollableViewportWidth();
        int viewportH = getScrollableViewportHeight();
        if (viewportW <= 0 || viewportH <= 0) {
            return;
        }

        boolean mouseInViewport = isPointInsideScrollableViewport(mouseX, mouseY);
        int clippedMouseX = mouseInViewport ? mouseX : Integer.MIN_VALUE;
        int clippedMouseY = mouseInViewport ? mouseY : Integer.MIN_VALUE;

        startScissor(viewportX, viewportY, viewportW, viewportH);
        try {
            drawSectionBox("模式与预设", modeDropdown.x, modeDropdown.y, presetDropdown.x + presetDropdown.width,
                    presetDropdown.y + presetDropdown.height
                            + Math.max(modeDropdown.getVisibleMenuHeight(), presetDropdown.getVisibleMenuHeight()));
            drawSectionBox("速度参数", timerToggleButton.x, timerToggleButton.y,
                    vanillaSpeedButton.x + vanillaSpeedButton.width,
                    vanillaSpeedButton.y + vanillaSpeedButton.height);
            drawSectionBox("状态提示", statusHudButton.x, statusHudButton.y,
                    statusHudButton.x + statusHudButton.width,
                    statusHudButton.y + statusHudButton.height);

            for (GuiButton button : this.buttonList) {
                if (isScrollableButton(button)) {
                    button.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
                }
            }

            modeDropdown.draw(clippedMouseX, clippedMouseY);
            presetDropdown.draw(clippedMouseX, clippedMouseY);
        } finally {
            endScissor();
        }
    }

    private void drawFixedButtons(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : this.buttonList) {
            if (isFooterButton(button)) {
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
            this.drawString(this.fontRenderer, "§b" + title, boxX + 6, titleY, 0xFFE8F6FF);
        }
    }

    private void drawContentScrollbar() {
        if (this.contentMaxScroll <= 0) {
            return;
        }
        int sbX = this.panelX + this.panelWidth - 12;
        int sbY = this.contentTop + 22;
        int sbH = Math.max(20, this.contentBottom - sbY);
        int thumbH = Math.max(16, (int) ((float) sbH * sbH / Math.max(sbH, sbH + this.contentMaxScroll)));
        int thumbTravel = Math.max(0, sbH - thumbH);
        int thumbY = sbY + (int) ((this.contentScrollOffset / (float) Math.max(1, this.contentMaxScroll)) * thumbTravel);
        drawRect(sbX, sbY, sbX + 4, sbY + sbH, 0x55141C24);
        drawRect(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xCC7CCBFF);
    }

    private void openFloatInput(String title, float currentValue, float min, float max, FloatConsumer consumer) {
        mc.displayGuiScreen(new GuiTextInput(this, title, formatFloat(currentValue), value -> {
            float parsed = currentValue;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            consumer.accept(clampFloat(parsed, min, max));
            refreshControlTexts();
            relayoutControls();
            mc.displayGuiScreen(this);
        }));
    }

    private void onDropdownValueChanged(SimpleDropdown dropdown, int selectedIndex) {
        if (dropdown == modeDropdown) {
            SpeedHandler.speedMode = getModeIdByIndex(selectedIndex);
        } else if (dropdown == presetDropdown) {
            SpeedHandler.applyPreset(getPresetIdByIndex(selectedIndex));
        }
        refreshControlTexts();
        relayoutControls();
    }

    private int getModeIndex(String mode) {
        String normalizedMode = mode == null ? "" : mode.trim();
        if (SpeedHandler.MODE_GROUND.equalsIgnoreCase(normalizedMode) || "VANILLA".equalsIgnoreCase(normalizedMode)) {
            return 0;
        }
        if (SpeedHandler.MODE_AIR.equalsIgnoreCase(normalizedMode)) {
            return 1;
        }
        if (SpeedHandler.MODE_LOWHOP.equalsIgnoreCase(normalizedMode)) {
            return 3;
        }
        if (SpeedHandler.MODE_ONGROUND.equalsIgnoreCase(normalizedMode)) {
            return 4;
        }
        return 2;
    }

    private String getModeIdByIndex(int index) {
        switch (index) {
        case 0:
            return SpeedHandler.MODE_GROUND;
        case 1:
            return SpeedHandler.MODE_AIR;
        case 3:
            return SpeedHandler.MODE_LOWHOP;
        case 4:
            return SpeedHandler.MODE_ONGROUND;
        case 2:
        default:
            return SpeedHandler.MODE_BHOP;
        }
    }

    private int getPresetIndex(String presetId) {
        if (SpeedHandler.PRESET_SAFE.equalsIgnoreCase(presetId)) {
            return 0;
        }
        if (SpeedHandler.PRESET_AGGRESSIVE.equalsIgnoreCase(presetId)) {
            return 2;
        }
        if (SpeedHandler.PRESET_CUSTOM.equalsIgnoreCase(presetId)) {
            return 3;
        }
        return 1;
    }

    private String getPresetIdByIndex(int index) {
        switch (index) {
        case 0:
            return SpeedHandler.PRESET_SAFE;
        case 2:
            return SpeedHandler.PRESET_AGGRESSIVE;
        case 3:
            return SpeedHandler.PRESET_CUSTOM;
        default:
            return SpeedHandler.PRESET_BALANCED;
        }
    }

    private boolean isMouseOver(int mouseX, int mouseY, GuiButton button) {
        return button != null && button.visible
                && isPointInsideButtonArea(button, mouseX, mouseY);
    }

    private boolean isMouseOverDropdown(SimpleDropdown dropdown, int mouseX, int mouseY) {
        if (dropdown == null || !isPointInsideScrollableViewport(mouseX, mouseY)) {
            return false;
        }
        if (isInside(mouseX, mouseY, dropdown.x, dropdown.y, dropdown.width, dropdown.height)) {
            return true;
        }
        if (!dropdown.expanded) {
            return false;
        }
        return isInside(mouseX, mouseY, dropdown.x, dropdown.y + dropdown.height + 2, dropdown.width,
                dropdown.getExpandedHeight() - 2);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isPointInsideButtonArea(GuiButton button, int mouseX, int mouseY) {
        if (button == null || !isInside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
            return false;
        }
        return !isScrollableButton(button) || isPointInsideScrollableViewport(mouseX, mouseY);
    }

    private boolean isWithinContent(int y, int height, int visibleTop, int visibleBottom) {
        return y + height >= visibleTop && y <= visibleBottom;
    }

    private boolean isFooterButton(GuiButton button) {
        return button == saveButton || button == defaultButton || button == cancelButton;
    }

    private boolean isScrollableButton(GuiButton button) {
        return button != null && !isFooterButton(button);
    }

    private int getScrollableViewportLeft() {
        return this.panelX + 6;
    }

    private int getScrollableViewportTop() {
        return this.contentTop + 28;
    }

    private int getScrollableViewportWidth() {
        return Math.max(1, this.panelWidth - 12);
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

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String stateText(boolean enabled) {
        return enabled ? "§a开启" : "§c关闭";
    }

    private interface FloatConsumer {
        void accept(float value);
    }
}
