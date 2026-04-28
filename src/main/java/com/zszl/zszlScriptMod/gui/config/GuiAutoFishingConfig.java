package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiAutoFishingConfig extends ThemedGuiScreen {

    private static final int BTN_REQUIRE_ROD = 1;
    private static final int BTN_AUTO_SWITCH_ROD = 2;
    private static final int BTN_PREFERRED_SLOT = 3;
    private static final int BTN_DISABLE_WHEN_GUI = 4;
    private static final int BTN_ALLOW_MOVING = 5;
    private static final int BTN_STATUS_MESSAGE = 6;

    private static final int BTN_AUTO_CAST_ON_START = 20;
    private static final int BTN_INITIAL_CAST_DELAY = 21;
    private static final int BTN_AUTO_RECAST = 22;
    private static final int BTN_RECAST_DELAY_MIN = 23;
    private static final int BTN_RECAST_DELAY_MAX = 24;
    private static final int BTN_RETRY_BOBBER_MISSING = 25;
    private static final int BTN_RETRY_CAST_DELAY = 26;
    private static final int BTN_TIMEOUT_RECAST = 27;
    private static final int BTN_MAX_WAIT = 28;

    private static final int BTN_BITE_MODE = 40;
    private static final int BTN_IGNORE_SETTLE = 41;
    private static final int BTN_REEL_DELAY = 42;
    private static final int BTN_VERTICAL_THRESHOLD = 43;
    private static final int BTN_HORIZONTAL_THRESHOLD = 44;
    private static final int BTN_CONFIRM_BITE = 45;
    private static final int BTN_DEBUG_BITE = 46;

    private static final int BTN_POST_REEL_PAUSE = 60;
    private static final int BTN_PREVENT_DOUBLE_REEL = 61;
    private static final int BTN_RECAST_ONLY_SUCCESS = 62;
    private static final int BTN_RESET_HOOK_GONE = 63;
    private static final int BTN_AUTO_RECOVER = 64;

    private static final int BTN_STOP_LOW_DURA = 80;
    private static final int BTN_MIN_DURA = 81;
    private static final int BTN_STOP_NO_ROD = 82;
    private static final int BTN_PAUSE_HOOK_ENTITY = 83;
    private static final int BTN_STOP_WORLD_CHANGE = 84;

    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;

    private static final int MOUSE_WHEEL_NOTCH = 120;

    private static final class SectionBox {
        private final String title;
        private final int virtualTop;
        private final int virtualHeight;

        private SectionBox(String title, int virtualTop, int virtualHeight) {
            this.title = title;
            this.virtualTop = virtualTop;
            this.virtualHeight = virtualHeight;
        }
    }

    private static final class ButtonPlacement {
        private final GuiButton button;
        private final int x;
        private final int virtualY;

        private ButtonPlacement(GuiButton button, int x, int virtualY) {
            this.button = button;
            this.x = x;
            this.virtualY = virtualY;
        }
    }

    private final GuiScreen parentScreen;
    private final List<String> instructionLines = new ArrayList<>();
    private final List<SectionBox> sectionBoxes = new ArrayList<>();
    private final List<ButtonPlacement> placements = new ArrayList<>();

    private ToggleGuiButton requireRodButton;
    private ToggleGuiButton autoSwitchRodButton;
    private GuiButton preferredSlotButton;
    private ToggleGuiButton disableWhenGuiButton;
    private ToggleGuiButton allowMovingButton;
    private ToggleGuiButton statusMessageButton;

    private ToggleGuiButton autoCastOnStartButton;
    private GuiButton initialCastDelayButton;
    private ToggleGuiButton autoRecastButton;
    private GuiButton recastDelayMinButton;
    private GuiButton recastDelayMaxButton;
    private ToggleGuiButton retryBobberMissingButton;
    private GuiButton retryCastDelayButton;
    private ToggleGuiButton timeoutRecastButton;
    private GuiButton maxWaitButton;

    private GuiButton biteModeButton;
    private GuiButton ignoreSettleButton;
    private GuiButton reelDelayButton;
    private GuiButton verticalThresholdButton;
    private GuiButton horizontalThresholdButton;
    private GuiButton confirmBiteButton;
    private ToggleGuiButton debugBiteButton;

    private GuiButton postReelPauseButton;
    private GuiButton preventDoubleReelButton;
    private ToggleGuiButton recastOnlySuccessButton;
    private ToggleGuiButton resetHookGoneButton;
    private ToggleGuiButton autoRecoverButton;

    private ToggleGuiButton stopLowDuraButton;
    private GuiButton minDuraButton;
    private ToggleGuiButton stopNoRodButton;
    private ToggleGuiButton pauseHookEntityButton;
    private ToggleGuiButton stopWorldChangeButton;

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

    public GuiAutoFishingConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        recalcLayout();
        initButtons();
        layoutButtons();
    }

    private void recalcLayout() {
        this.panelWidth = Math.min(440, Math.max(280, this.width - 16));
        this.panelHeight = Math.min(380, Math.max(240, this.height - 16));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.instructionLines.clear();
        String intro = "完整自动钓鱼设置：覆盖出杆、鱼漂判定、收杆补杆、异常恢复与安全限制。";
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(intro, this.panelWidth - 32));

        int introHeight = this.instructionLines.size() * 10;
        this.contentTop = this.panelY + 26 + introHeight + 8;
        this.contentBottom = this.panelY + this.panelHeight - 36;
        if (this.contentBottom < this.contentTop + 24) {
            this.contentBottom = this.contentTop + 24;
        }
    }

    private void initButtons() {
        requireRodButton = new ToggleGuiButton(BTN_REQUIRE_ROD, 0, 0, 100, 20, "", AutoFishingHandler.requireFishingRod);
        autoSwitchRodButton = new ToggleGuiButton(BTN_AUTO_SWITCH_ROD, 0, 0, 100, 20, "",
                AutoFishingHandler.autoSwitchToRod);
        preferredSlotButton = new ThemedButton(BTN_PREFERRED_SLOT, 0, 0, 100, 20, "");
        disableWhenGuiButton = new ToggleGuiButton(BTN_DISABLE_WHEN_GUI, 0, 0, 100, 20, "",
                AutoFishingHandler.disableWhenGuiOpen);
        allowMovingButton = new ToggleGuiButton(BTN_ALLOW_MOVING, 0, 0, 100, 20, "",
                AutoFishingHandler.allowWhilePlayerMoving);
        statusMessageButton = new ToggleGuiButton(BTN_STATUS_MESSAGE, 0, 0, 100, 20, "",
                AutoFishingHandler.sendStatusMessage);

        autoCastOnStartButton = new ToggleGuiButton(BTN_AUTO_CAST_ON_START, 0, 0, 100, 20, "",
                AutoFishingHandler.enableAutoCastOnStart);
        initialCastDelayButton = new ThemedButton(BTN_INITIAL_CAST_DELAY, 0, 0, 100, 20, "");
        autoRecastButton = new ToggleGuiButton(BTN_AUTO_RECAST, 0, 0, 100, 20, "",
                AutoFishingHandler.autoRecastAfterCatch);
        recastDelayMinButton = new ThemedButton(BTN_RECAST_DELAY_MIN, 0, 0, 100, 20, "");
        recastDelayMaxButton = new ThemedButton(BTN_RECAST_DELAY_MAX, 0, 0, 100, 20, "");
        retryBobberMissingButton = new ToggleGuiButton(BTN_RETRY_BOBBER_MISSING, 0, 0, 100, 20, "",
                AutoFishingHandler.retryCastWhenBobberMissing);
        retryCastDelayButton = new ThemedButton(BTN_RETRY_CAST_DELAY, 0, 0, 100, 20, "");
        timeoutRecastButton = new ToggleGuiButton(BTN_TIMEOUT_RECAST, 0, 0, 100, 20, "",
                AutoFishingHandler.timeoutRecastEnabled);
        maxWaitButton = new ThemedButton(BTN_MAX_WAIT, 0, 0, 100, 20, "");

        biteModeButton = new ThemedButton(BTN_BITE_MODE, 0, 0, 100, 20, "");
        ignoreSettleButton = new ThemedButton(BTN_IGNORE_SETTLE, 0, 0, 100, 20, "");
        reelDelayButton = new ThemedButton(BTN_REEL_DELAY, 0, 0, 100, 20, "");
        verticalThresholdButton = new ThemedButton(BTN_VERTICAL_THRESHOLD, 0, 0, 100, 20, "");
        horizontalThresholdButton = new ThemedButton(BTN_HORIZONTAL_THRESHOLD, 0, 0, 100, 20, "");
        confirmBiteButton = new ThemedButton(BTN_CONFIRM_BITE, 0, 0, 100, 20, "");
        debugBiteButton = new ToggleGuiButton(BTN_DEBUG_BITE, 0, 0, 100, 20, "",
                AutoFishingHandler.debugBiteInfo);

        postReelPauseButton = new ThemedButton(BTN_POST_REEL_PAUSE, 0, 0, 100, 20, "");
        preventDoubleReelButton = new ThemedButton(BTN_PREVENT_DOUBLE_REEL, 0, 0, 100, 20, "");
        recastOnlySuccessButton = new ToggleGuiButton(BTN_RECAST_ONLY_SUCCESS, 0, 0, 100, 20, "",
                AutoFishingHandler.recastOnlyIfLootSuccess);
        resetHookGoneButton = new ToggleGuiButton(BTN_RESET_HOOK_GONE, 0, 0, 100, 20, "",
                AutoFishingHandler.resetStateWhenHookGone);
        autoRecoverButton = new ToggleGuiButton(BTN_AUTO_RECOVER, 0, 0, 100, 20, "",
                AutoFishingHandler.autoRecoverFromInterruptedCast);

        stopLowDuraButton = new ToggleGuiButton(BTN_STOP_LOW_DURA, 0, 0, 100, 20, "",
                AutoFishingHandler.stopWhenRodDurabilityLow);
        minDuraButton = new ThemedButton(BTN_MIN_DURA, 0, 0, 100, 20, "");
        stopNoRodButton = new ToggleGuiButton(BTN_STOP_NO_ROD, 0, 0, 100, 20, "",
                AutoFishingHandler.stopWhenNoRodFound);
        pauseHookEntityButton = new ToggleGuiButton(BTN_PAUSE_HOOK_ENTITY, 0, 0, 100, 20, "",
                AutoFishingHandler.pauseWhenHookedEntity);
        stopWorldChangeButton = new ToggleGuiButton(BTN_STOP_WORLD_CHANGE, 0, 0, 100, 20, "",
                AutoFishingHandler.stopOnWorldChange);

        saveButton = new ThemedButton(BTN_SAVE, 0, 0, 110, 20, "保存并关闭");
        defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, 20, "恢复默认");
        cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, 20, "取消");

        this.buttonList.add(requireRodButton);
        this.buttonList.add(autoSwitchRodButton);
        this.buttonList.add(preferredSlotButton);
        this.buttonList.add(disableWhenGuiButton);
        this.buttonList.add(allowMovingButton);
        this.buttonList.add(statusMessageButton);

        this.buttonList.add(autoCastOnStartButton);
        this.buttonList.add(initialCastDelayButton);
        this.buttonList.add(autoRecastButton);
        this.buttonList.add(recastDelayMinButton);
        this.buttonList.add(recastDelayMaxButton);
        this.buttonList.add(retryBobberMissingButton);
        this.buttonList.add(retryCastDelayButton);
        this.buttonList.add(timeoutRecastButton);
        this.buttonList.add(maxWaitButton);

        this.buttonList.add(biteModeButton);
        this.buttonList.add(ignoreSettleButton);
        this.buttonList.add(reelDelayButton);
        this.buttonList.add(verticalThresholdButton);
        this.buttonList.add(horizontalThresholdButton);
        this.buttonList.add(confirmBiteButton);
        this.buttonList.add(debugBiteButton);

        this.buttonList.add(postReelPauseButton);
        this.buttonList.add(preventDoubleReelButton);
        this.buttonList.add(recastOnlySuccessButton);
        this.buttonList.add(resetHookGoneButton);
        this.buttonList.add(autoRecoverButton);

        this.buttonList.add(stopLowDuraButton);
        this.buttonList.add(minDuraButton);
        this.buttonList.add(stopNoRodButton);
        this.buttonList.add(pauseHookEntityButton);
        this.buttonList.add(stopWorldChangeButton);

        this.buttonList.add(saveButton);
        this.buttonList.add(defaultButton);
        this.buttonList.add(cancelButton);

        refreshButtonTexts();
    }

    private void refreshButtonTexts() {
        requireRodButton.setEnabledState(AutoFishingHandler.requireFishingRod);
        requireRodButton.displayString = "必须手持鱼竿: " + stateText(AutoFishingHandler.requireFishingRod);

        autoSwitchRodButton.setEnabledState(AutoFishingHandler.autoSwitchToRod);
        autoSwitchRodButton.displayString = "自动切换鱼竿: " + stateText(AutoFishingHandler.autoSwitchToRod);

        preferredSlotButton.displayString = "优先鱼竿槽位: "
                + (AutoFishingHandler.preferredRodSlot <= 0 ? "自动" : AutoFishingHandler.preferredRodSlot);

        disableWhenGuiButton.setEnabledState(AutoFishingHandler.disableWhenGuiOpen);
        disableWhenGuiButton.displayString = "打开模组界面时暂停: " + stateText(AutoFishingHandler.disableWhenGuiOpen);

        allowMovingButton.setEnabledState(AutoFishingHandler.allowWhilePlayerMoving);
        allowMovingButton.displayString = "移动时继续工作: " + stateText(AutoFishingHandler.allowWhilePlayerMoving);

        statusMessageButton.setEnabledState(AutoFishingHandler.sendStatusMessage);
        statusMessageButton.displayString = "发送状态提示: " + stateText(AutoFishingHandler.sendStatusMessage);

        autoCastOnStartButton.setEnabledState(AutoFishingHandler.enableAutoCastOnStart);
        autoCastOnStartButton.displayString = "开启后自动出杆: " + stateText(AutoFishingHandler.enableAutoCastOnStart);

        initialCastDelayButton.displayString = "首次出杆延迟: " + AutoFishingHandler.initialCastDelayTicks + " Tick";

        autoRecastButton.setEnabledState(AutoFishingHandler.autoRecastAfterCatch);
        autoRecastButton.displayString = "钓完后自动出杆: " + stateText(AutoFishingHandler.autoRecastAfterCatch);

        recastDelayMinButton.displayString = "补杆最小延迟: " + AutoFishingHandler.recastDelayMinTicks + " Tick";
        recastDelayMaxButton.displayString = "补杆最大延迟: " + AutoFishingHandler.recastDelayMaxTicks + " Tick";

        retryBobberMissingButton.setEnabledState(AutoFishingHandler.retryCastWhenBobberMissing);
        retryBobberMissingButton.displayString = "鱼漂缺失时重试: "
                + stateText(AutoFishingHandler.retryCastWhenBobberMissing);

        retryCastDelayButton.displayString = "重试出杆延迟: " + AutoFishingHandler.retryCastDelayTicks + " Tick";

        timeoutRecastButton.setEnabledState(AutoFishingHandler.timeoutRecastEnabled);
        timeoutRecastButton.displayString = "超时自动补杆: " + stateText(AutoFishingHandler.timeoutRecastEnabled);

        maxWaitButton.displayString = "最长等待咬钩: " + AutoFishingHandler.maxFishingWaitTicks + " Tick";

        biteModeButton.displayString = "咬钩判定模式: " + getBiteModeText(AutoFishingHandler.biteDetectMode);
        ignoreSettleButton.displayString = "忽略入水扰动: " + AutoFishingHandler.ignoreInitialBobberSettleTicks + " Tick";
        reelDelayButton.displayString = "咬钩后收杆延迟: " + AutoFishingHandler.reelDelayTicks + " Tick";
        verticalThresholdButton.displayString = "下沉阈值: " + formatFloat(AutoFishingHandler.minVerticalDropThreshold);
        horizontalThresholdButton.displayString = "水平位移阈值: "
                + formatFloat(AutoFishingHandler.minHorizontalMoveThreshold);
        confirmBiteButton.displayString = "咬钩确认 Tick: " + AutoFishingHandler.confirmBiteTicks;

        debugBiteButton.setEnabledState(AutoFishingHandler.debugBiteInfo);
        debugBiteButton.displayString = "显示咬钩调试: " + stateText(AutoFishingHandler.debugBiteInfo);

        postReelPauseButton.displayString = "收杆后等待: " + AutoFishingHandler.postReelPauseTicks + " Tick";
        preventDoubleReelButton.displayString = "防重复收杆: " + AutoFishingHandler.preventDoubleReelTicks + " Tick";

        recastOnlySuccessButton.setEnabledState(AutoFishingHandler.recastOnlyIfLootSuccess);
        recastOnlySuccessButton.displayString = "仅成功收杆后补杆: "
                + stateText(AutoFishingHandler.recastOnlyIfLootSuccess);

        resetHookGoneButton.setEnabledState(AutoFishingHandler.resetStateWhenHookGone);
        resetHookGoneButton.displayString = "鱼漂消失时重置: "
                + stateText(AutoFishingHandler.resetStateWhenHookGone);

        autoRecoverButton.setEnabledState(AutoFishingHandler.autoRecoverFromInterruptedCast);
        autoRecoverButton.displayString = "异常断杆后恢复: "
                + stateText(AutoFishingHandler.autoRecoverFromInterruptedCast);

        stopLowDuraButton.setEnabledState(AutoFishingHandler.stopWhenRodDurabilityLow);
        stopLowDuraButton.displayString = "耐久低时停止: "
                + stateText(AutoFishingHandler.stopWhenRodDurabilityLow);

        minDuraButton.displayString = "最低鱼竿耐久: " + AutoFishingHandler.minRodDurability;

        stopNoRodButton.setEnabledState(AutoFishingHandler.stopWhenNoRodFound);
        stopNoRodButton.displayString = "未找到鱼竿时停止: " + stateText(AutoFishingHandler.stopWhenNoRodFound);

        pauseHookEntityButton.setEnabledState(AutoFishingHandler.pauseWhenHookedEntity);
        pauseHookEntityButton.displayString = "钩到实体时暂停: " + stateText(AutoFishingHandler.pauseWhenHookedEntity);

        stopWorldChangeButton.setEnabledState(AutoFishingHandler.stopOnWorldChange);
        stopWorldChangeButton.displayString = "切图/换服时停止: " + stateText(AutoFishingHandler.stopOnWorldChange);

        recastDelayMinButton.enabled = AutoFishingHandler.autoRecastAfterCatch;
        recastDelayMaxButton.enabled = AutoFishingHandler.autoRecastAfterCatch;
        retryCastDelayButton.enabled = AutoFishingHandler.retryCastWhenBobberMissing;
        maxWaitButton.enabled = AutoFishingHandler.timeoutRecastEnabled;
        minDuraButton.enabled = AutoFishingHandler.stopWhenRodDurabilityLow;
    }

    private void layoutButtons() {
        recalcLayout();
        refreshButtonTexts();

        int innerPadding = 12;
        int columnGap = 8;
        int buttonWidth = Math.max(96, (this.panelWidth - innerPadding * 2 - columnGap) / 2);
        int buttonHeight = 20;
        int rowGap = 6;
        int rowStep = buttonHeight + rowGap;
        int sectionGap = 12;
        int headerHeight = 18;

        int leftX = this.panelX + innerPadding;
        int rightX = leftX + buttonWidth + columnGap;

        this.sectionBoxes.clear();
        this.placements.clear();

        int currentY = 0;
        currentY = addSection("基础控制", currentY, leftX, rightX, headerHeight, rowStep, sectionGap,
                new GuiButton[][] {
                        { requireRodButton, autoSwitchRodButton },
                        { preferredSlotButton, disableWhenGuiButton },
                        { allowMovingButton, statusMessageButton }
                });
        currentY = addSection("出杆设置", currentY, leftX, rightX, headerHeight, rowStep, sectionGap,
                new GuiButton[][] {
                        { autoCastOnStartButton, initialCastDelayButton },
                        { autoRecastButton, recastDelayMinButton },
                        { recastDelayMaxButton, retryBobberMissingButton },
                        { retryCastDelayButton, timeoutRecastButton },
                        { maxWaitButton, null }
                });
        currentY = addSection("咬钩判定", currentY, leftX, rightX, headerHeight, rowStep, sectionGap,
                new GuiButton[][] {
                        { biteModeButton, ignoreSettleButton },
                        { reelDelayButton, verticalThresholdButton },
                        { horizontalThresholdButton, confirmBiteButton },
                        { debugBiteButton, null }
                });
        currentY = addSection("收杆 / 补杆", currentY, leftX, rightX, headerHeight, rowStep, sectionGap,
                new GuiButton[][] {
                        { postReelPauseButton, preventDoubleReelButton },
                        { recastOnlySuccessButton, resetHookGoneButton },
                        { autoRecoverButton, null }
                });
        currentY = addSection("安全限制", currentY, leftX, rightX, headerHeight, rowStep, 0,
                new GuiButton[][] {
                        { stopLowDuraButton, minDuraButton },
                        { stopNoRodButton, pauseHookEntityButton },
                        { stopWorldChangeButton, null }
                });

        int viewportHeight = Math.max(1, this.contentBottom - this.contentTop);
        this.contentMaxScroll = Math.max(0, currentY - viewportHeight);
        this.contentScroll = clampInt(this.contentScroll, 0, this.contentMaxScroll);

        for (ButtonPlacement placement : this.placements) {
            applyPlacement(placement.button, placement.x, placement.virtualY, buttonWidth, buttonHeight);
        }

        saveButton.x = this.panelX + 12;
        saveButton.y = this.panelY + this.panelHeight - 28;
        defaultButton.x = this.panelX + 130;
        defaultButton.y = this.panelY + this.panelHeight - 28;
        cancelButton.x = this.panelX + this.panelWidth - 102;
        cancelButton.y = this.panelY + this.panelHeight - 28;
    }

    private int addSection(String title, int startY, int leftX, int rightX, int headerHeight, int rowStep,
            int sectionGap, GuiButton[][] rows) {
        int currentY = startY + headerHeight;
        for (GuiButton[] row : rows) {
            if (row.length > 0 && row[0] != null) {
                this.placements.add(new ButtonPlacement(row[0], leftX, currentY));
            }
            if (row.length > 1 && row[1] != null) {
                this.placements.add(new ButtonPlacement(row[1], rightX, currentY));
            }
            currentY += rowStep;
        }
        this.sectionBoxes.add(new SectionBox(title, startY, currentY - startY));
        return currentY + sectionGap;
    }

    private void applyPlacement(GuiButton button, int x, int virtualY, int width, int height) {
        int y = this.contentTop + virtualY - this.contentScroll;
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = y + height > this.contentTop && y < this.contentBottom;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || this.contentMaxScroll <= 0) {
            return;
        }

        int mouseX = 0;
        int mouseY = 0;
        if (this.mc != null && this.width > 0 && this.height > 0) {
            mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
            mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;
        }

        if (!isInScrollableContent(mouseX, mouseY)) {
            return;
        }

        int steps = Math.max(1, Math.abs(wheel) / MOUSE_WHEEL_NOTCH);
        if (wheel < 0) {
            this.contentScroll = clampInt(this.contentScroll + steps * 18, 0, this.contentMaxScroll);
        } else {
            this.contentScroll = clampInt(this.contentScroll - steps * 18, 0, this.contentMaxScroll);
        }
        layoutButtons();
    }

    private boolean isInScrollableContent(int mouseX, int mouseY) {
        return mouseX >= this.panelX + 8
                && mouseX <= this.panelX + this.panelWidth - 8
                && mouseY >= this.contentTop
                && mouseY <= this.contentBottom;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_REQUIRE_ROD:
                AutoFishingHandler.requireFishingRod = !AutoFishingHandler.requireFishingRod;
                break;
            case BTN_AUTO_SWITCH_ROD:
                AutoFishingHandler.autoSwitchToRod = !AutoFishingHandler.autoSwitchToRod;
                break;
            case BTN_PREFERRED_SLOT:
                openIntInput("输入优先鱼竿槽位 (0=自动, 1-9)", AutoFishingHandler.preferredRodSlot, 0, 9, value -> {
                    AutoFishingHandler.preferredRodSlot = value;
                    refreshButtonTexts();
                });
                return;
            case BTN_DISABLE_WHEN_GUI:
                AutoFishingHandler.disableWhenGuiOpen = !AutoFishingHandler.disableWhenGuiOpen;
                break;
            case BTN_ALLOW_MOVING:
                AutoFishingHandler.allowWhilePlayerMoving = !AutoFishingHandler.allowWhilePlayerMoving;
                break;
            case BTN_STATUS_MESSAGE:
                AutoFishingHandler.sendStatusMessage = !AutoFishingHandler.sendStatusMessage;
                break;

            case BTN_AUTO_CAST_ON_START:
                AutoFishingHandler.enableAutoCastOnStart = !AutoFishingHandler.enableAutoCastOnStart;
                break;
            case BTN_INITIAL_CAST_DELAY:
                openIntInput("输入首次出杆延迟 Tick (0 - 100)", AutoFishingHandler.initialCastDelayTicks, 0, 100,
                        value -> {
                            AutoFishingHandler.initialCastDelayTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_AUTO_RECAST:
                AutoFishingHandler.autoRecastAfterCatch = !AutoFishingHandler.autoRecastAfterCatch;
                break;
            case BTN_RECAST_DELAY_MIN:
                openIntInput("输入补杆最小延迟 Tick (0 - 100)", AutoFishingHandler.recastDelayMinTicks, 0, 100, value -> {
                    AutoFishingHandler.recastDelayMinTicks = value;
                    if (AutoFishingHandler.recastDelayMaxTicks < value) {
                        AutoFishingHandler.recastDelayMaxTicks = value;
                    }
                    refreshButtonTexts();
                });
                return;
            case BTN_RECAST_DELAY_MAX:
                openIntInput("输入补杆最大延迟 Tick", AutoFishingHandler.recastDelayMaxTicks,
                        AutoFishingHandler.recastDelayMinTicks, 100, value -> {
                            AutoFishingHandler.recastDelayMaxTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_RETRY_BOBBER_MISSING:
                AutoFishingHandler.retryCastWhenBobberMissing = !AutoFishingHandler.retryCastWhenBobberMissing;
                break;
            case BTN_RETRY_CAST_DELAY:
                openIntInput("输入重试出杆延迟 Tick (5 - 100)", AutoFishingHandler.retryCastDelayTicks, 5, 100,
                        value -> {
                            AutoFishingHandler.retryCastDelayTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_TIMEOUT_RECAST:
                AutoFishingHandler.timeoutRecastEnabled = !AutoFishingHandler.timeoutRecastEnabled;
                break;
            case BTN_MAX_WAIT:
                openIntInput("输入最长等待咬钩时间 Tick (40 - 2400)", AutoFishingHandler.maxFishingWaitTicks,
                        40, 2400, value -> {
                            AutoFishingHandler.maxFishingWaitTicks = value;
                            refreshButtonTexts();
                        });
                return;

            case BTN_BITE_MODE:
                cycleBiteMode();
                break;
            case BTN_IGNORE_SETTLE:
                openIntInput("输入忽略入水扰动 Tick (0 - 40)", AutoFishingHandler.ignoreInitialBobberSettleTicks, 0, 40,
                        value -> {
                            AutoFishingHandler.ignoreInitialBobberSettleTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_REEL_DELAY:
                openIntInput("输入咬钩后收杆延迟 Tick (0 - 20)", AutoFishingHandler.reelDelayTicks, 0, 20, value -> {
                    AutoFishingHandler.reelDelayTicks = value;
                    refreshButtonTexts();
                });
                return;
            case BTN_VERTICAL_THRESHOLD:
                openFloatInput("输入鱼漂下沉阈值 (0.01 - 1.0)", AutoFishingHandler.minVerticalDropThreshold,
                        0.01F, 1.0F, value -> {
                            AutoFishingHandler.minVerticalDropThreshold = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_HORIZONTAL_THRESHOLD:
                openFloatInput("输入鱼漂水平位移阈值 (0.0 - 1.0)", AutoFishingHandler.minHorizontalMoveThreshold,
                        0.0F, 1.0F, value -> {
                            AutoFishingHandler.minHorizontalMoveThreshold = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_CONFIRM_BITE:
                openIntInput("输入咬钩确认 Tick (1 - 5)", AutoFishingHandler.confirmBiteTicks, 1, 5, value -> {
                    AutoFishingHandler.confirmBiteTicks = value;
                    refreshButtonTexts();
                });
                return;
            case BTN_DEBUG_BITE:
                AutoFishingHandler.debugBiteInfo = !AutoFishingHandler.debugBiteInfo;
                break;

            case BTN_POST_REEL_PAUSE:
                openIntInput("输入收杆后等待 Tick (0 - 40)", AutoFishingHandler.postReelPauseTicks, 0, 40, value -> {
                    AutoFishingHandler.postReelPauseTicks = value;
                    refreshButtonTexts();
                });
                return;
            case BTN_PREVENT_DOUBLE_REEL:
                openIntInput("输入防重复收杆间隔 Tick (0 - 20)", AutoFishingHandler.preventDoubleReelTicks, 0, 20,
                        value -> {
                            AutoFishingHandler.preventDoubleReelTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_RECAST_ONLY_SUCCESS:
                AutoFishingHandler.recastOnlyIfLootSuccess = !AutoFishingHandler.recastOnlyIfLootSuccess;
                break;
            case BTN_RESET_HOOK_GONE:
                AutoFishingHandler.resetStateWhenHookGone = !AutoFishingHandler.resetStateWhenHookGone;
                break;
            case BTN_AUTO_RECOVER:
                AutoFishingHandler.autoRecoverFromInterruptedCast = !AutoFishingHandler.autoRecoverFromInterruptedCast;
                break;

            case BTN_STOP_LOW_DURA:
                AutoFishingHandler.stopWhenRodDurabilityLow = !AutoFishingHandler.stopWhenRodDurabilityLow;
                break;
            case BTN_MIN_DURA:
                openIntInput("输入最低鱼竿耐久 (1 - 64)", AutoFishingHandler.minRodDurability, 1, 64, value -> {
                    AutoFishingHandler.minRodDurability = value;
                    refreshButtonTexts();
                });
                return;
            case BTN_STOP_NO_ROD:
                AutoFishingHandler.stopWhenNoRodFound = !AutoFishingHandler.stopWhenNoRodFound;
                break;
            case BTN_PAUSE_HOOK_ENTITY:
                AutoFishingHandler.pauseWhenHookedEntity = !AutoFishingHandler.pauseWhenHookedEntity;
                break;
            case BTN_STOP_WORLD_CHANGE:
                AutoFishingHandler.stopOnWorldChange = !AutoFishingHandler.stopOnWorldChange;
                break;

            case BTN_SAVE:
                AutoFishingHandler.saveConfig();
                this.mc.setScreen(parentScreen);
                return;
            case BTN_DEFAULT:
                AutoFishingHandler.resetToDefaults();
                break;
            case BTN_CANCEL:
                AutoFishingHandler.loadConfig();
                this.mc.setScreen(parentScreen);
                return;
            default:
                break;
        }

        refreshButtonTexts();
        layoutButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "自动钓鱼设置", this.fontRenderer);

        int textY = panelY + 24;
        for (String line : instructionLines) {
            this.drawString(this.fontRenderer, line, panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += 10;
        }

        drawRect(panelX + 8, contentTop - 2, panelX + panelWidth - 8, contentBottom + 2, 0x221A2533);
        drawSectionBoxes();

        if (contentMaxScroll > 0) {
            int trackX = panelX + panelWidth - 10;
            int trackY = contentTop;
            int trackHeight = contentBottom - contentTop;
            int thumbHeight = Math.max(18,
                    (int) ((trackHeight / (float) Math.max(trackHeight, trackHeight + contentMaxScroll)) * trackHeight));
            int thumbTrack = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) ((contentScroll / (float) Math.max(1, contentMaxScroll)) * thumbTrack);
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawTooltips(mouseX, mouseY);
    }

    private void drawSectionBoxes() {
        int boxX = panelX + 8;
        int boxWidth = panelWidth - 20;
        for (SectionBox section : sectionBoxes) {
            int top = contentTop + section.virtualTop - contentScroll;
            int bottom = top + section.virtualHeight;
            if (bottom <= contentTop || top >= contentBottom) {
                continue;
            }

            int clippedTop = Math.max(contentTop, top);
            int clippedBottom = Math.min(contentBottom, bottom);
            drawRect(boxX, clippedTop, boxX + boxWidth, clippedBottom, 0x44202A36);
            drawHorizontalLine(boxX, boxX + boxWidth, clippedTop, 0xFF4FA6D9);
            drawHorizontalLine(boxX, boxX + boxWidth, clippedBottom, 0xFF35536C);
            drawVerticalLine(boxX, clippedTop, clippedBottom, 0xFF35536C);
            drawVerticalLine(boxX + boxWidth, clippedTop, clippedBottom, 0xFF35536C);

            int titleY = Math.max(top + 5, clippedTop + 5);
            if (titleY + this.fontRenderer.FONT_HEIGHT < clippedBottom) {
                this.drawString(this.fontRenderer, "§b" + section.title, boxX + 6, titleY, 0xFFE8F6FF);
            }
        }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        if (biteModeButton.visible && isMouseOverButton(mouseX, mouseY, biteModeButton)) {
            drawHoveringText(Arrays.asList(
                    "§e咬钩判定模式",
                    "§7SMART: 综合下沉和位移判断，适合大多数服务器。",
                    "§7MOTION: 只看鱼漂运动，简单直接。",
                    "§7STRICT: 要求更严格，误判更少但更保守。"), mouseX, mouseY);
            return;
        }
        if (autoRecastButton.visible && isMouseOverButton(mouseX, mouseY, autoRecastButton)) {
            drawHoveringText(Arrays.asList(
                    "§e钓完后自动出杆",
                    "§7收杆完成后自动等待一段随机延迟，",
                    "§7然后重新甩杆，形成完整挂机循环。"), mouseX, mouseY);
            return;
        }
        if (retryBobberMissingButton.visible && isMouseOverButton(mouseX, mouseY, retryBobberMissingButton)) {
            drawHoveringText(Arrays.asList(
                    "§e鱼漂缺失时重试",
                    "§7用于处理甩杆后鱼漂实体未正常出现的情况，",
                    "§7开启后会按设定延迟重复尝试出杆。"), mouseX, mouseY);
            return;
        }
        if (timeoutRecastButton.visible && isMouseOverButton(mouseX, mouseY, timeoutRecastButton)) {
            drawHoveringText(Arrays.asList(
                    "§e超时自动补杆",
                    "§7若长时间没有检测到咬钩，",
                    "§7会自动收杆并重新进入补杆流程。"), mouseX, mouseY);
            return;
        }
        if (stopLowDuraButton.visible && isMouseOverButton(mouseX, mouseY, stopLowDuraButton)) {
            drawHoveringText(Arrays.asList(
                    "§e耐久低时停止",
                    "§7避免挂机时把鱼竿用坏。",
                    "§7当剩余耐久小于等于下方阈值时自动关闭。"), mouseX, mouseY);
            return;
        }
        if (pauseHookEntityButton.visible && isMouseOverButton(mouseX, mouseY, pauseHookEntityButton)) {
            drawHoveringText(Arrays.asList(
                    "§e钩到实体时暂停",
                    "§7如果鱼钩意外勾到了实体，",
                    "§7开启后会暂停自动收杆，减少误操作。"), mouseX, mouseY);
        }
    }

    private void cycleBiteMode() {
        if (AutoFishingHandler.BITE_MODE_SMART.equalsIgnoreCase(AutoFishingHandler.biteDetectMode)) {
            AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_MOTION_ONLY;
        } else if (AutoFishingHandler.BITE_MODE_MOTION_ONLY.equalsIgnoreCase(AutoFishingHandler.biteDetectMode)) {
            AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_STRICT;
        } else {
            AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_SMART;
        }
    }

    private String getBiteModeText(String mode) {
        if (AutoFishingHandler.BITE_MODE_MOTION_ONLY.equalsIgnoreCase(mode)) {
            return "仅运动";
        }
        if (AutoFishingHandler.BITE_MODE_STRICT.equalsIgnoreCase(mode)) {
            return "严格";
        }
        return "智能";
    }

    private void openIntInput(String title, int currentValue, int min, int max, IntConsumer consumer) {
        this.mc.setScreen(new GuiTextInput(this, title, String.valueOf(currentValue), value -> {
            int parsed = currentValue;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            consumer.accept(clampInt(parsed, min, max));
            layoutButtons();
        }));
    }

    private void openFloatInput(String title, float currentValue, float min, float max, FloatConsumer consumer) {
        this.mc.setScreen(new GuiTextInput(this, title, formatFloat(currentValue), value -> {
            float parsed = currentValue;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            consumer.accept(clampFloat(parsed, min, max));
            layoutButtons();
        }));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String stateText(boolean enabled) {
        return enabled ? "§a开启" : "§c关闭";
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private interface FloatConsumer {
        void accept(float value);
    }
}
