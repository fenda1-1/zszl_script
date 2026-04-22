package com.zszl.zszlScriptMod.otherfeatures.gui.misc;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager.FeatureState;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SingleMiscFeatureConfigScreen extends ThemedGuiScreen {

    private static final int BTN_ENABLED = 1;
    private static final int BTN_HUD = 2;
    private static final int BTN_PRIMARY_OPTION = 3;
    private static final int BTN_SECONDARY_OPTION = 4;
    private static final int BTN_TERTIARY_OPTION = 5;
    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;

    private final GuiScreen parentScreen;
    private final String featureId;
    private final String title;
    private final List<String> descriptionLines = new ArrayList<>();

    private ToggleGuiButton enabledButton;
    private ToggleGuiButton hudButton;
    private GuiButton primaryOptionButton;
    private GuiButton secondaryOptionButton;
    private GuiButton tertiaryOptionButton;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;

    private boolean draftEnabled;
    private boolean draftStatusHudEnabled;

    public SingleMiscFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        this.parentScreen = parentScreen;
        this.featureId = featureId == null ? "" : featureId.trim();
        this.title = title == null ? "杂项功能设置" : title.trim();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        FeatureState state = getFeature();
        this.draftEnabled = state != null && state.isEnabled();
        this.draftStatusHudEnabled = state == null || state.isStatusHudEnabled();
        rebuildDescription(state);

        int panelWidth = Math.min(520, Math.max(360, this.width - 16));
        int panelHeight = 272;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int fieldX = panelX + 18;
        int fieldWidth = panelWidth - 36;

        this.enabledButton = new ToggleGuiButton(BTN_ENABLED, fieldX, panelY + 84, fieldWidth, 20, "", this.draftEnabled);
        this.hudButton = new ToggleGuiButton(BTN_HUD, fieldX, panelY + 108, fieldWidth, 20, "", this.draftStatusHudEnabled);
        this.primaryOptionButton = new ThemedButton(BTN_PRIMARY_OPTION, fieldX, panelY + 140, fieldWidth, 20, "");
        this.secondaryOptionButton = new ThemedButton(BTN_SECONDARY_OPTION, fieldX, panelY + 164, fieldWidth, 20, "");
        this.tertiaryOptionButton = new ThemedButton(BTN_TERTIARY_OPTION, fieldX, panelY + 188, fieldWidth, 20, "");
        int footerY = panelY + panelHeight - 30;
        int footerW = (fieldWidth - 12) / 3;
        this.saveButton = new ThemedButton(BTN_SAVE, fieldX, footerY, footerW, 20, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, fieldX + footerW + 6, footerY, footerW, 20, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, fieldX + (footerW + 6) * 2, footerY, footerW, 20, "取消");

        this.buttonList.add(this.enabledButton);
        this.buttonList.add(this.hudButton);
        this.buttonList.add(this.primaryOptionButton);
        this.buttonList.add(this.secondaryOptionButton);
        this.buttonList.add(this.tertiaryOptionButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);

        refreshButtonTexts();
    }

    private FeatureState getFeature() {
        return MiscFeatureManager.getFeature(this.featureId);
    }

    private void rebuildDescription(FeatureState state) {
        this.descriptionLines.clear();
        String text = state == null ? "当前功能不存在。" : "§7" + state.description;
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(text, Math.max(120, this.width - 80)));
    }

    private void refreshButtonTexts() {
        this.enabledButton.setEnabledState(this.draftEnabled);
        this.enabledButton.displayString = "功能状态 : " + (this.draftEnabled ? "开启" : "关闭");
        this.hudButton.setEnabledState(this.draftStatusHudEnabled);
        this.hudButton.displayString = "状态HUD : " + (this.draftStatusHudEnabled ? "开启" : "关闭");

        if ("auto_reconnect".equals(this.featureId)) {
            this.primaryOptionButton.displayString = "重连延迟 : " + MiscFeatureManager.getAutoReconnectDelayTicks() + " tick";
            this.secondaryOptionButton.visible = true;
            this.secondaryOptionButton.enabled = !MiscFeatureManager.isAutoReconnectInfiniteAttempts();
            this.secondaryOptionButton.displayString = MiscFeatureManager.isAutoReconnectInfiniteAttempts()
                    ? "最大尝试次数 : 当前已忽略"
                    : "最大尝试次数 : " + MiscFeatureManager.getAutoReconnectMaxAttempts();
            this.tertiaryOptionButton.visible = true;
            this.tertiaryOptionButton.enabled = true;
            this.tertiaryOptionButton.displayString = "无限重试 : "
                    + (MiscFeatureManager.isAutoReconnectInfiniteAttempts() ? "开启" : "关闭");
        } else {
            this.primaryOptionButton.displayString = "复活冷却 : " + MiscFeatureManager.getAutoRespawnDelayTicks() + " tick";
            this.secondaryOptionButton.visible = false;
            this.secondaryOptionButton.enabled = false;
            this.tertiaryOptionButton.visible = false;
            this.tertiaryOptionButton.enabled = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_ENABLED:
            this.draftEnabled = !this.draftEnabled;
            refreshButtonTexts();
            return;
        case BTN_HUD:
            this.draftStatusHudEnabled = !this.draftStatusHudEnabled;
            refreshButtonTexts();
            return;
        case BTN_PRIMARY_OPTION:
            if ("auto_reconnect".equals(this.featureId)) {
                openIntegerInput("输入自动重连延迟 (5 - 200 tick)", MiscFeatureManager.getAutoReconnectDelayTicks(), 5, 200,
                        MiscFeatureManager::setAutoReconnectDelayTicks);
            } else {
                openIntegerInput("输入自动复活冷却 (1 - 100 tick)", MiscFeatureManager.getAutoRespawnDelayTicks(), 1, 100,
                        MiscFeatureManager::setAutoRespawnDelayTicks);
            }
            return;
        case BTN_SECONDARY_OPTION:
            openIntegerInput("输入最大重连次数 (1 - 10)", MiscFeatureManager.getAutoReconnectMaxAttempts(), 1, 10,
                    MiscFeatureManager::setAutoReconnectMaxAttempts);
            return;
        case BTN_TERTIARY_OPTION:
            MiscFeatureManager.setAutoReconnectInfiniteAttempts(!MiscFeatureManager.isAutoReconnectInfiniteAttempts());
            refreshButtonTexts();
            return;
        case BTN_SAVE:
            MiscFeatureManager.setEnabled(this.featureId, this.draftEnabled);
            MiscFeatureManager.setFeatureStatusHudEnabled(this.featureId, this.draftStatusHudEnabled);
            this.mc.setScreen(this.parentScreen);
            return;
        case BTN_DEFAULT:
            MiscFeatureManager.resetFeature(this.featureId);
            FeatureState state = getFeature();
            this.draftEnabled = state != null && state.isEnabled();
            this.draftStatusHudEnabled = state == null || state.isStatusHudEnabled();
            refreshButtonTexts();
            return;
        case BTN_CANCEL:
            this.mc.setScreen(this.parentScreen);
            return;
        default:
            break;
        }
    }

    private void openIntegerInput(String titleText, int currentValue, int min, int max, java.util.function.IntConsumer setter) {
        this.mc.setScreen(new GuiTextInput(this, titleText, String.valueOf(currentValue), value -> {
            int parsed = currentValue;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            setter.accept(Math.max(min, Math.min(max, parsed)));
            refreshButtonTexts();
            this.mc.setScreen(this);
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int panelWidth = Math.min(520, Math.max(360, this.width - 16));
        int panelHeight = 272;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int fieldX = panelX + 18;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, this.title, this.fontRenderer);

        drawString(this.fontRenderer, "§7这里使用独立的杂项功能配置页。", fieldX, panelY + 24, 0xFFFFFFFF);
        int descY = panelY + 42;
        for (String line : this.descriptionLines) {
            drawString(this.fontRenderer, line, fieldX, descY, 0xFFE2F2FF);
            descY += this.fontRenderer.FONT_HEIGHT + 2;
        }
        drawString(this.fontRenderer, "§7运行状态: §f" + MiscFeatureManager.getFeatureRuntimeSummary(this.featureId), fieldX,
                panelY + 220, 0xFFFFFFFF);
        drawString(this.fontRenderer, "§7当前草稿: " + (this.draftEnabled ? "§a开启" : "§c关闭") + " §7| HUD: "
                + (this.draftStatusHudEnabled ? "§a开启" : "§c关闭"), fieldX, panelY + 234, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}





