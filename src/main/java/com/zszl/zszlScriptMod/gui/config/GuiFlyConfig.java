package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.handlers.FlyHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiFlyConfig extends ThemedGuiScreen {

    private static final int BTN_MODE = 1;
    private static final int BTN_AUTO_TAKEOFF = 2;
    private static final int BTN_STOP_ON_DISABLE = 3;
    private static final int BTN_NO_COLLISION = 4;
    private static final int BTN_ANTI_KNOCKBACK = 5;
    private static final int BTN_ANTI_KICK = 6;

    private static final int BTN_HORIZONTAL_SPEED = 20;
    private static final int BTN_VERTICAL_SPEED = 21;
    private static final int BTN_SPRINT_MULTIPLIER = 22;
    private static final int BTN_GLIDE_FALL = 23;
    private static final int BTN_PULSE_BOOST = 24;
    private static final int BTN_PULSE_INTERVAL = 25;
    private static final int BTN_ANTI_KICK_INTERVAL = 26;
    private static final int BTN_ANTI_KICK_DISTANCE = 27;

    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;

    private final GuiScreen parentScreen;
    private final List<String> instructionLines = new ArrayList<>();

    private ToggleGuiButton autoTakeoffButton;
    private ToggleGuiButton stopOnDisableButton;
    private ToggleGuiButton noCollisionButton;
    private ToggleGuiButton antiKnockbackButton;
    private ToggleGuiButton antiKickButton;

    private GuiButton modeButton;
    private GuiButton horizontalSpeedButton;
    private GuiButton verticalSpeedButton;
    private GuiButton sprintMultiplierButton;
    private GuiButton glideFallButton;
    private GuiButton pulseBoostButton;
    private GuiButton pulseIntervalButton;
    private GuiButton antiKickIntervalButton;
    private GuiButton antiKickDistanceButton;

    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int contentTop;
    private int contentBottom;
    private int contentScroll;
    private int contentMaxScroll;

    public GuiFlyConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        recalcLayout();
        initButtons();
        relayoutButtons();
    }

    private void recalcLayout() {
        int maxWidth = Math.min(440, Math.max(300, this.width - 12));
        int maxHeight = Math.min(420, Math.max(240, this.height - 12));

        this.panelWidth = Math.min(maxWidth, Math.max(300, this.width - 8));
        this.panelHeight = Math.min(maxHeight, Math.max(240, this.height - 8));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.instructionLines.clear();
        String instruction = "左键主界面按钮为总开关；这里可自由切换不同飞行方式，并按分组细调速度与兼容参数。";
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(instruction, instructionWidth));

        int instructionHeight = this.instructionLines.size() * 10;
        this.contentTop = this.panelY + 24 + instructionHeight + 8;
        int footerTop = this.panelY + this.panelHeight - 28;
        this.contentBottom = footerTop - 8;

        if (this.contentBottom < this.contentTop + 24) {
            this.contentBottom = this.contentTop + 24;
        }
    }

    private void initButtons() {
        autoTakeoffButton = new ToggleGuiButton(BTN_AUTO_TAKEOFF, 0, 0, 100, 20, "", FlyHandler.autoTakeoff);
        stopOnDisableButton = new ToggleGuiButton(BTN_STOP_ON_DISABLE, 0, 0, 100, 20, "",
                FlyHandler.stopMotionOnDisable);
        noCollisionButton = new ToggleGuiButton(BTN_NO_COLLISION, 0, 0, 100, 20, "", FlyHandler.enableNoCollision);
        antiKnockbackButton = new ToggleGuiButton(BTN_ANTI_KNOCKBACK, 0, 0, 100, 20, "",
                FlyHandler.enableAntiKnockback);
        antiKickButton = new ToggleGuiButton(BTN_ANTI_KICK, 0, 0, 100, 20, "", FlyHandler.enableAntiKick);

        modeButton = new ThemedButton(BTN_MODE, 0, 0, 100, 20, "");
        horizontalSpeedButton = new ThemedButton(BTN_HORIZONTAL_SPEED, 0, 0, 100, 20, "");
        verticalSpeedButton = new ThemedButton(BTN_VERTICAL_SPEED, 0, 0, 100, 20, "");
        sprintMultiplierButton = new ThemedButton(BTN_SPRINT_MULTIPLIER, 0, 0, 100, 20, "");
        glideFallButton = new ThemedButton(BTN_GLIDE_FALL, 0, 0, 100, 20, "");
        pulseBoostButton = new ThemedButton(BTN_PULSE_BOOST, 0, 0, 100, 20, "");
        pulseIntervalButton = new ThemedButton(BTN_PULSE_INTERVAL, 0, 0, 100, 20, "");
        antiKickIntervalButton = new ThemedButton(BTN_ANTI_KICK_INTERVAL, 0, 0, 100, 20, "");
        antiKickDistanceButton = new ThemedButton(BTN_ANTI_KICK_DISTANCE, 0, 0, 100, 20, "");

        saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, 20, "§a保存并关闭");
        defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, 20, "§e恢复默认");
        cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, 20, "取消");

        this.buttonList.add(modeButton);
        this.buttonList.add(autoTakeoffButton);
        this.buttonList.add(stopOnDisableButton);
        this.buttonList.add(horizontalSpeedButton);
        this.buttonList.add(verticalSpeedButton);
        this.buttonList.add(sprintMultiplierButton);
        this.buttonList.add(glideFallButton);
        this.buttonList.add(pulseBoostButton);
        this.buttonList.add(pulseIntervalButton);
        this.buttonList.add(noCollisionButton);
        this.buttonList.add(antiKnockbackButton);
        this.buttonList.add(antiKickButton);
        this.buttonList.add(antiKickIntervalButton);
        this.buttonList.add(antiKickDistanceButton);
        this.buttonList.add(saveButton);
        this.buttonList.add(defaultButton);
        this.buttonList.add(cancelButton);

        refreshButtonTexts();
    }

    private void refreshButtonTexts() {
        autoTakeoffButton.setEnabledState(FlyHandler.autoTakeoff);
        autoTakeoffButton.displayString = "自动起飞: " + stateText(FlyHandler.autoTakeoff);

        stopOnDisableButton.setEnabledState(FlyHandler.stopMotionOnDisable);
        stopOnDisableButton.displayString = "关闭时清空速度: " + stateText(FlyHandler.stopMotionOnDisable);

        noCollisionButton.setEnabledState(FlyHandler.enableNoCollision);
        noCollisionButton.displayString = "无碰撞: " + stateText(FlyHandler.enableNoCollision);

        antiKnockbackButton.setEnabledState(FlyHandler.enableAntiKnockback);
        antiKnockbackButton.displayString = "防击退: " + stateText(FlyHandler.enableAntiKnockback);

        antiKickButton.setEnabledState(FlyHandler.enableAntiKick);
        antiKickButton.displayString = "防卡空踢: " + stateText(FlyHandler.enableAntiKick);

        modeButton.displayString = "飞行方式: " + getModeDisplayName(FlyHandler.flightMode);
        horizontalSpeedButton.displayString = "水平速度: " + formatFloat(FlyHandler.horizontalSpeed);
        verticalSpeedButton.displayString = "垂直速度: " + formatFloat(FlyHandler.verticalSpeed);
        sprintMultiplierButton.displayString = "冲刺倍率: " + formatFloat(FlyHandler.sprintMultiplier);
        glideFallButton.displayString = "滑翔下坠: " + formatFloat(FlyHandler.glideFallSpeed);
        pulseBoostButton.displayString = "脉冲升力: " + formatFloat(FlyHandler.pulseBoost);
        pulseIntervalButton.displayString = "脉冲间隔: " + FlyHandler.pulseIntervalTicks + " Tick";
        antiKickIntervalButton.displayString = "防踢间隔: " + FlyHandler.antiKickIntervalTicks + " Tick";
        antiKickDistanceButton.displayString = "防踢下压: " + formatFloat(FlyHandler.antiKickDistance);

        boolean pulseMode = FlyHandler.MODE_PULSE.equalsIgnoreCase(FlyHandler.flightMode);
        pulseBoostButton.enabled = pulseMode;
        pulseIntervalButton.enabled = pulseMode;

        antiKickIntervalButton.enabled = FlyHandler.enableAntiKick;
        antiKickDistanceButton.enabled = FlyHandler.enableAntiKick;
    }

    private void relayoutButtons() {
        recalcLayout();

        int innerPadding = 12;
        int columnGap = 8;
        int buttonHeight = 20;
        int rowGap = 6;
        int rowStep = buttonHeight + rowGap;
        int sectionGap = 12;
        int sectionHeaderHeight = 18;

        int leftX = this.panelX + innerPadding;
        int buttonW = Math.max(92, (this.panelWidth - innerPadding * 2 - columnGap) / 2);
        int rightX = this.panelX + this.panelWidth - innerPadding - buttonW;

        int baseY = 0;

        int modeTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(modeButton, leftX, modeTop, buttonW, buttonHeight);
        layoutScrollableButton(autoTakeoffButton, rightX, modeTop, buttonW, buttonHeight);
        layoutScrollableButton(stopOnDisableButton, leftX, modeTop + rowStep, buttonW, buttonHeight);
        baseY = modeTop + rowStep * 2 + sectionGap;

        int speedTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(horizontalSpeedButton, leftX, speedTop, buttonW, buttonHeight);
        layoutScrollableButton(verticalSpeedButton, rightX, speedTop, buttonW, buttonHeight);
        layoutScrollableButton(sprintMultiplierButton, leftX, speedTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(glideFallButton, rightX, speedTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(pulseBoostButton, leftX, speedTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(pulseIntervalButton, rightX, speedTop + rowStep * 2, buttonW, buttonHeight);
        baseY = speedTop + rowStep * 3 + sectionGap;

        int protectTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(noCollisionButton, leftX, protectTop, buttonW, buttonHeight);
        layoutScrollableButton(antiKnockbackButton, rightX, protectTop, buttonW, buttonHeight);
        layoutScrollableButton(antiKickButton, leftX, protectTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(antiKickIntervalButton, rightX, protectTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(antiKickDistanceButton, leftX, protectTop + rowStep * 2, buttonW, buttonHeight);
        baseY = protectTop + rowStep * 3;

        int totalContentHeight = baseY + buttonHeight;
        int visibleContentHeight = Math.max(24, this.contentBottom - this.contentTop);
        this.contentMaxScroll = Math.max(0, totalContentHeight - visibleContentHeight);
        this.contentScroll = clampInt(this.contentScroll, 0, this.contentMaxScroll);

        modeTop = sectionHeaderHeight;
        layoutScrollableButton(modeButton, leftX, modeTop, buttonW, buttonHeight);
        layoutScrollableButton(autoTakeoffButton, rightX, modeTop, buttonW, buttonHeight);
        layoutScrollableButton(stopOnDisableButton, leftX, modeTop + rowStep, buttonW, buttonHeight);
        baseY = modeTop + rowStep * 2 + sectionGap;

        speedTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(horizontalSpeedButton, leftX, speedTop, buttonW, buttonHeight);
        layoutScrollableButton(verticalSpeedButton, rightX, speedTop, buttonW, buttonHeight);
        layoutScrollableButton(sprintMultiplierButton, leftX, speedTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(glideFallButton, rightX, speedTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(pulseBoostButton, leftX, speedTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(pulseIntervalButton, rightX, speedTop + rowStep * 2, buttonW, buttonHeight);
        baseY = speedTop + rowStep * 3 + sectionGap;

        protectTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(noCollisionButton, leftX, protectTop, buttonW, buttonHeight);
        layoutScrollableButton(antiKnockbackButton, rightX, protectTop, buttonW, buttonHeight);
        layoutScrollableButton(antiKickButton, leftX, protectTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(antiKickIntervalButton, rightX, protectTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(antiKickDistanceButton, leftX, protectTop + rowStep * 2, buttonW, buttonHeight);

        int footerY = this.panelY + this.panelHeight - 28;
        int footerGap = 6;
        int footerButtonW = Math.max(64, (this.panelWidth - innerPadding * 2 - footerGap * 2) / 3);
        int footerTotalW = footerButtonW * 3 + footerGap * 2;
        int footerStartX = this.panelX + (this.panelWidth - footerTotalW) / 2;

        layoutFixedButton(saveButton, footerStartX, footerY, footerButtonW, 20);
        layoutFixedButton(defaultButton, footerStartX + footerButtonW + footerGap, footerY, footerButtonW, 20);
        layoutFixedButton(cancelButton, footerStartX + (footerButtonW + footerGap) * 2, footerY, footerButtonW, 20);
    }

    private void layoutScrollableButton(GuiButton button, int x, int baseY, int width, int height) {
        int y = this.contentTop + baseY - this.contentScroll;
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = y >= this.contentTop && y + height <= this.contentBottom;
    }

    private void layoutFixedButton(GuiButton button, int x, int y, int width, int height) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = true;
    }

    private boolean isInScrollableContent(int mouseX, int mouseY) {
        return mouseX >= this.panelX + 8 && mouseX <= this.panelX + this.panelWidth - 8 && mouseY >= this.contentTop
                && mouseY <= this.contentBottom;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (this.contentMaxScroll <= 0 || !isInScrollableContent(mouseX, mouseY)) {
            return;
        }

        if (wheel < 0) {
            this.contentScroll = clampInt(this.contentScroll + 18, 0, this.contentMaxScroll);
        } else {
            this.contentScroll = clampInt(this.contentScroll - 18, 0, this.contentMaxScroll);
        }
        relayoutButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_MODE:
            FlyHandler.flightMode = nextMode(FlyHandler.flightMode);
            break;
        case BTN_AUTO_TAKEOFF:
            FlyHandler.autoTakeoff = !FlyHandler.autoTakeoff;
            break;
        case BTN_STOP_ON_DISABLE:
            FlyHandler.stopMotionOnDisable = !FlyHandler.stopMotionOnDisable;
            break;
        case BTN_NO_COLLISION:
            FlyHandler.enableNoCollision = !FlyHandler.enableNoCollision;
            break;
        case BTN_ANTI_KNOCKBACK:
            FlyHandler.enableAntiKnockback = !FlyHandler.enableAntiKnockback;
            break;
        case BTN_ANTI_KICK:
            FlyHandler.enableAntiKick = !FlyHandler.enableAntiKick;
            break;
        case BTN_HORIZONTAL_SPEED:
            openFloatInput("输入水平飞行速度 (0.05 - 3.00)", FlyHandler.horizontalSpeed, 0.05F, 3.00F,
                    value -> FlyHandler.horizontalSpeed = value);
            return;
        case BTN_VERTICAL_SPEED:
            openFloatInput("输入垂直飞行速度 (0.05 - 1.50)", FlyHandler.verticalSpeed, 0.05F, 1.50F,
                    value -> FlyHandler.verticalSpeed = value);
            return;
        case BTN_SPRINT_MULTIPLIER:
            openFloatInput("输入冲刺倍率 (1.00 - 3.00)", FlyHandler.sprintMultiplier, 1.00F, 3.00F,
                    value -> FlyHandler.sprintMultiplier = value);
            return;
        case BTN_GLIDE_FALL:
            openFloatInput("输入滑翔模式自然下坠速度 (0.00 - 0.50)", FlyHandler.glideFallSpeed, 0.00F, 0.50F,
                    value -> FlyHandler.glideFallSpeed = value);
            return;
        case BTN_PULSE_BOOST:
            if (pulseBoostButton.enabled) {
                openFloatInput("输入脉冲模式单次升力 (0.05 - 1.50)", FlyHandler.pulseBoost, 0.05F, 1.50F,
                        value -> FlyHandler.pulseBoost = value);
            }
            return;
        case BTN_PULSE_INTERVAL:
            if (pulseIntervalButton.enabled) {
                openIntInput("输入脉冲模式升力间隔 Tick (1 - 40)", FlyHandler.pulseIntervalTicks, 1, 40,
                        value -> FlyHandler.pulseIntervalTicks = value);
            }
            return;
        case BTN_ANTI_KICK_INTERVAL:
            if (antiKickIntervalButton.enabled) {
                openIntInput("输入防卡空踢间隔 Tick (4 - 80)", FlyHandler.antiKickIntervalTicks, 4, 80,
                        value -> FlyHandler.antiKickIntervalTicks = value);
            }
            return;
        case BTN_ANTI_KICK_DISTANCE:
            if (antiKickDistanceButton.enabled) {
                openFloatInput("输入防卡空踢向下微调值 (0.01 - 0.20)", FlyHandler.antiKickDistance, 0.01F, 0.20F,
                        value -> FlyHandler.antiKickDistance = value);
            }
            return;
        case BTN_SAVE:
            FlyHandler.saveConfig();
            mc.setScreen(parentScreen);
            return;
        case BTN_DEFAULT:
            applyDefaultValues();
            break;
        case BTN_CANCEL:
            FlyHandler.loadConfig();
            mc.setScreen(parentScreen);
            return;
        default:
            break;
        }

        refreshButtonTexts();
        relayoutButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "飞行设置", this.fontRenderer);

        int textY = panelY + 22;
        for (String line : instructionLines) {
            this.drawString(this.fontRenderer, line, panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += 10;
        }

        if (contentMaxScroll > 0) {
            fillRect(panelX + 8, contentTop - 2, panelX + panelWidth - 8, contentBottom + 2, 0x221A2533);

            int trackX = panelX + panelWidth - 10;
            int trackY = contentTop;
            int trackHeight = contentBottom - contentTop;
            int thumbHeight = Math.max(18,
                    (int) ((trackHeight / (float) Math.max(trackHeight, trackHeight + contentMaxScroll))
                            * trackHeight));
            int thumbTrack = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) ((contentScroll / (float) Math.max(1, contentMaxScroll)) * thumbTrack);
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackHeight, thumbY, thumbHeight);
        }

        drawSectionBox("飞行方式", modeButton, autoTakeoffButton, stopOnDisableButton);
        drawSectionBox("速度参数", horizontalSpeedButton, verticalSpeedButton, sprintMultiplierButton, glideFallButton,
                pulseBoostButton, pulseIntervalButton);
        drawSectionBox("保护与兼容", noCollisionButton, antiKnockbackButton, antiKickButton, antiKickIntervalButton,
                antiKickDistanceButton);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isMouseOver(mouseX, mouseY, modeButton)) {
            drawHoveringText(Arrays.asList("§e飞行方式", "§7动量飞行：直接锁定水平/垂直速度，适合精准悬停。", "§7滑翔飞行：默认缓慢下坠，更像可控滑翔。",
                    "§7脉冲飞行：周期性抬升，适合尝试不同兼容方式。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, autoTakeoffButton)) {
            drawHoveringText(Arrays.asList("§e自动起飞", "§7开启后，总开关打开时若角色站在地上，", "§7会自动给一个轻微上升量，方便直接进入飞行。"), mouseX,
                    mouseY);
        } else if (isMouseOver(mouseX, mouseY, stopOnDisableButton)) {
            drawHoveringText(Arrays.asList("§e关闭时清空速度", "§7关闭飞行时把当前 motion 清零，", "§7可减少因为残余速度导致的飘移或摔落。"), mouseX,
                    mouseY);
        } else if (isMouseOver(mouseX, mouseY, horizontalSpeedButton)) {
            drawHoveringText(Arrays.asList("§e水平速度", "§7控制前后左右飞行时的基础平移速度。", "§7推荐从 0.60 到 1.20 之间慢慢测试。"), mouseX,
                    mouseY);
        } else if (isMouseOver(mouseX, mouseY, verticalSpeedButton)) {
            drawHoveringText(Arrays.asList("§e垂直速度", "§7按跳跃上升、按潜行下降时的速度。", "§7动量飞行/滑翔飞行/脉冲飞行都会使用这个值。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, sprintMultiplierButton)) {
            drawHoveringText(Arrays.asList("§e冲刺倍率", "§7飞行时如果角色处于冲刺状态，", "§7水平速度会额外乘上这个倍率。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, glideFallButton)) {
            drawHoveringText(Arrays.asList("§e滑翔下坠", "§7仅在滑翔飞行与脉冲飞行中体现明显效果。", "§7数值越大，不按键时掉落越快。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, pulseBoostButton)) {
            drawHoveringText(Arrays.asList("§e脉冲升力", "§7仅脉冲飞行模式可调。", "§7每次到达脉冲间隔时，会给角色一个向上的升力。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, pulseIntervalButton)) {
            drawHoveringText(Arrays.asList("§e脉冲间隔", "§7仅脉冲飞行模式可调。", "§7数值越小，向上抬升越频繁。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, noCollisionButton)) {
            drawHoveringText(Arrays.asList("§e无碰撞", "§7复用现有运动保护逻辑，", "§7减少飞行途中被实体推挤、卡位。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, antiKnockbackButton)) {
            drawHoveringText(Arrays.asList("§e防击退", "§7飞行中受击时尽量抵消水平击退，", "§7减少被怪物打偏飞行轨迹。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, antiKickButton)) {
            drawHoveringText(Arrays.asList("§e防卡空踢", "§7定期给一个很小的向下微调，", "§7用于尝试规避部分服务器对长时间悬空的检测。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, antiKickIntervalButton)) {
            drawHoveringText(Arrays.asList("§e防踢间隔", "§7仅在开启防卡空踢后生效。", "§7表示每隔多少 Tick 执行一次微下压。"), mouseX, mouseY);
        } else if (isMouseOver(mouseX, mouseY, antiKickDistanceButton)) {
            drawHoveringText(Arrays.asList("§e防踢下压", "§7仅在开启防卡空踢后生效。", "§7表示每次微调时向下施加的 motionY 值。"), mouseX, mouseY);
        }
    }

    private void drawSectionBox(String title, GuiButton... buttons) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (GuiButton button : buttons) {
            if (button == null || !button.visible) {
                continue;
            }
            minX = Math.min(minX, button.x);
            minY = Math.min(minY, button.y);
            maxX = Math.max(maxX, button.x + button.width);
            maxY = Math.max(maxY, button.y + button.height);
        }

        if (minX == Integer.MAX_VALUE) {
            return;
        }

        int boxX = minX - 6;
        int boxY = minY - 18;
        int boxW = (maxX - minX) + 12;
        int boxH = (maxY - minY) + 24;

        fillRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x44202A36);
        drawLineH(boxX, boxX + boxW, boxY, 0xFF4FA6D9);
        drawLineH(boxX, boxX + boxW, boxY + boxH, 0xFF35536C);
        drawLineV(boxX, boxY, boxY + boxH, 0xFF35536C);
        drawLineV(boxX + boxW, boxY, boxY + boxH, 0xFF35536C);
        this.drawString(this.fontRenderer, "§b" + title, boxX + 6, boxY + 5, 0xFFE8F6FF);
    }

    private void openFloatInput(String title, float currentValue, float min, float max, FloatConsumer consumer) {
        mc.setScreen(new GuiTextInput(this, title, formatFloat(currentValue), value -> {
            float parsed = currentValue;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            consumer.accept(clampFloat(parsed, min, max));
            refreshButtonTexts();
            relayoutButtons();
            mc.setScreen(this);
        }));
    }

    private void openIntInput(String title, int currentValue, int min, int max, IntConsumer consumer) {
        mc.setScreen(new GuiTextInput(this, title, String.valueOf(currentValue), value -> {
            int parsed = currentValue;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            consumer.accept(clampInt(parsed, min, max));
            refreshButtonTexts();
            relayoutButtons();
            mc.setScreen(this);
        }));
    }

    private void applyDefaultValues() {
        FlyHandler.flightMode = FlyHandler.MODE_MOTION;
        FlyHandler.autoTakeoff = true;
        FlyHandler.stopMotionOnDisable = true;
        FlyHandler.enableNoCollision = true;
        FlyHandler.enableAntiKnockback = true;
        FlyHandler.enableAntiKick = false;
        FlyHandler.horizontalSpeed = 0.85F;
        FlyHandler.verticalSpeed = 0.42F;
        FlyHandler.glideFallSpeed = 0.04F;
        FlyHandler.sprintMultiplier = 1.25F;
        FlyHandler.pulseBoost = 0.28F;
        FlyHandler.pulseIntervalTicks = 4;
        FlyHandler.antiKickIntervalTicks = 16;
        FlyHandler.antiKickDistance = 0.04F;
        refreshButtonTexts();
        relayoutButtons();
    }

    private String nextMode(String currentMode) {
        if (FlyHandler.MODE_MOTION.equalsIgnoreCase(currentMode)) {
            return FlyHandler.MODE_GLIDE;
        }
        if (FlyHandler.MODE_GLIDE.equalsIgnoreCase(currentMode)) {
            return FlyHandler.MODE_PULSE;
        }
        return FlyHandler.MODE_MOTION;
    }

    private String getModeDisplayName(String mode) {
        if (FlyHandler.MODE_GLIDE.equalsIgnoreCase(mode)) {
            return "滑翔飞行";
        }
        if (FlyHandler.MODE_PULSE.equalsIgnoreCase(mode)) {
            return "脉冲飞行";
        }
        return "动量飞行";
    }

    private boolean isMouseOver(int mouseX, int mouseY, GuiButton button) {
        return button != null && button.visible && mouseX >= button.x && mouseX <= button.x + button.width
                && mouseY >= button.y && mouseY <= button.y + button.height;
    }

    private void fillRect(int left, int top, int right, int bottom, int color) {
        com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }

    private void drawLineH(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int tmp = startX;
            startX = endX;
            endX = tmp;
        }
        fillRect(startX, y, endX + 1, y + 1, color);
    }

    private void drawLineV(int x, int startY, int endY, int color) {
        if (endY < startY) {
            int tmp = startY;
            startY = endY;
            endY = tmp;
        }
        fillRect(x, startY + 1, x + 1, endY, color);
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

    private interface IntConsumer {
        void accept(int value);
    }
}





