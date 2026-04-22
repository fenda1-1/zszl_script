// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/mail/GuiMailSettings.java
package com.zszl.zszlScriptMod.gui.mail;

import com.zszl.zszlScriptMod.handlers.MailConfig;
import com.zszl.zszlScriptMod.handlers.MailHelper;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import java.io.IOException;
import java.util.Collections;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

public class GuiMailSettings extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private ToggleGuiButton autoReceiveButton;
    private ToggleGuiButton amountFilterButton;
    private GuiTextField timeoutTicksField;
    private GuiTextField maxGoldField;
    private GuiTextField maxCouponField;

    public GuiMailSettings(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int panelWidth = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 120;
        int currentY = panelY;

        autoReceiveButton = new ToggleGuiButton(0, panelX, currentY, panelWidth, 20,
                I18n.format("gui.mail.settings.auto_receive", getStateText(MailConfig.INSTANCE.autoReceiveEnabled)),
                MailConfig.INSTANCE.autoReceiveEnabled);
        this.buttonList.add(autoReceiveButton);
        currentY += 34;

        amountFilterButton = new ToggleGuiButton(2, panelX, currentY, panelWidth, 20,
                I18n.format("gui.mail.settings.amount_filter", getStateText(MailConfig.INSTANCE.amountFilterEnabled)),
                MailConfig.INSTANCE.amountFilterEnabled);
        this.buttonList.add(amountFilterButton);
        currentY += 34;

        timeoutTicksField = new GuiTextField(100, this.fontRenderer, panelX, currentY, panelWidth, 20);
        timeoutTicksField.setMaxStringLength(Integer.MAX_VALUE);
        timeoutTicksField.setText(String.valueOf(MailConfig.INSTANCE.waitTimeoutTicks));
        currentY += 34;

        maxGoldField = new GuiTextField(101, this.fontRenderer, panelX, currentY, panelWidth, 20);
        maxGoldField.setMaxStringLength(Integer.MAX_VALUE);
        maxGoldField.setText(String.valueOf(MailConfig.INSTANCE.autoReceiveMaxGold));
        currentY += 34;

        maxCouponField = new GuiTextField(102, this.fontRenderer, panelX, currentY, panelWidth, 20);
        maxCouponField.setMaxStringLength(Integer.MAX_VALUE);
        maxCouponField.setText(String.valueOf(MailConfig.INSTANCE.autoReceiveMaxCoupon));
        currentY += 34;

        this.buttonList.add(new GuiButton(1, panelX, currentY, panelWidth, 20, I18n.format("gui.common.done")));
    }

    // --- 核心修复 1: 在GUI关闭时保存配置 ---
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        applyTimeoutTicksFromField();
        MailConfig.save();
    }
    // --- 修复结束 ---

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // 切换自动领取
            MailConfig.INSTANCE.autoReceiveEnabled = !MailConfig.INSTANCE.autoReceiveEnabled;
            autoReceiveButton.setEnabledState(MailConfig.INSTANCE.autoReceiveEnabled);
            autoReceiveButton.displayString = I18n.format("gui.mail.settings.auto_receive",
                    getStateText(MailConfig.INSTANCE.autoReceiveEnabled));

            // 开启自动领取时，立即复用“一键领取”同一套流程处理当前已捕获邮件。
            if (MailConfig.INSTANCE.autoReceiveEnabled) {
                MailHelper.INSTANCE.startAutoReceiveAll();
            }

            // (可选但推荐) 立即保存，这样即使游戏崩溃也能保留设置
            MailConfig.save();

        } else if (button.id == 2) { // 金额过滤总开关
            MailConfig.INSTANCE.amountFilterEnabled = !MailConfig.INSTANCE.amountFilterEnabled;
            amountFilterButton.setEnabledState(MailConfig.INSTANCE.amountFilterEnabled);
            amountFilterButton.displayString = I18n.format("gui.mail.settings.amount_filter",
                    getStateText(MailConfig.INSTANCE.amountFilterEnabled));
            MailConfig.save();

        } else if (button.id == 1) { // 完成按钮
            // --- 核心修复 2: 点击完成时也保存一下 (作为双重保险) ---
            applyTimeoutTicksFromField();
            MailConfig.save();
            // --- 修复结束 ---
            this.mc.setScreen(this.parentScreen);
        }
    }

    private void applyTimeoutTicksFromField() {
        if (timeoutTicksField == null) {
            return;
        }
        try {
            int value = Integer.parseInt(timeoutTicksField.getText().trim());
            MailConfig.INSTANCE.waitTimeoutTicks = Math.max(1, value);
        } catch (Exception ignored) {
            MailConfig.INSTANCE.waitTimeoutTicks = 5;
            timeoutTicksField.setText("5");
        }

        try {
            int value = Integer.parseInt(maxGoldField.getText().trim());
            MailConfig.INSTANCE.autoReceiveMaxGold = Math.max(0, value);
        } catch (Exception ignored) {
            MailConfig.INSTANCE.autoReceiveMaxGold = 0;
            maxGoldField.setText("0");
        }

        try {
            int value = Integer.parseInt(maxCouponField.getText().trim());
            MailConfig.INSTANCE.autoReceiveMaxCoupon = Math.max(0, value);
        } catch (Exception ignored) {
            MailConfig.INSTANCE.autoReceiveMaxCoupon = 0;
            maxCouponField.setText("0");
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (timeoutTicksField != null && timeoutTicksField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (maxGoldField != null && maxGoldField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (maxCouponField != null && maxCouponField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (timeoutTicksField != null) {
            timeoutTicksField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (maxGoldField != null) {
            maxGoldField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (maxCouponField != null) {
            maxCouponField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (timeoutTicksField != null) {
            timeoutTicksField.updateCursorCounter();
        }
        if (maxGoldField != null) {
            maxGoldField.updateCursorCounter();
        }
        if (maxCouponField != null) {
            maxCouponField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRenderer, I18n.format("gui.mail.settings.title"), this.width / 2, 20, 0xFFFFFF);
        if (timeoutTicksField != null) {
            drawCenteredString(this.fontRenderer, I18n.format("gui.mail.settings.timeout"), this.width / 2,
                    timeoutTicksField.y - 12, 0xC0C0C0);
            drawThemedTextField(timeoutTicksField);
        }
        if (maxGoldField != null) {
            drawCenteredString(this.fontRenderer, I18n.format("gui.mail.settings.max_gold"), this.width / 2,
                    maxGoldField.y - 12, 0xC0C0C0);
            drawThemedTextField(maxGoldField);
        }
        if (maxCouponField != null) {
            drawCenteredString(this.fontRenderer, I18n.format("gui.mail.settings.max_coupon"), this.width / 2,
                    maxCouponField.y - 12, 0xC0C0C0);
            drawThemedTextField(maxCouponField);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (autoReceiveButton != null && autoReceiveButton.isMouseOver()) {
            drawHoveringText(Collections.singletonList(I18n.format("gui.mail.settings.auto_receive.tooltip")),
                    this.width / 2 - 90, 36);
        }
        if (amountFilterButton != null && amountFilterButton.isMouseOver()) {
            drawHoveringText(Collections.singletonList(I18n.format("gui.mail.settings.amount_filter.tooltip")),
                    this.width / 2 - 90, 52);
        }
    }

    private String getStateText(boolean enabled) {
        return I18n.format(enabled ? "gui.common.enabled" : "gui.common.disabled");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}






