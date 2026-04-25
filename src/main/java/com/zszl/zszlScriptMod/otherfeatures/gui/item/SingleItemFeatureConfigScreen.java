package com.zszl.zszlScriptMod.otherfeatures.gui.item;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager.FeatureState;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SingleItemFeatureConfigScreen extends ThemedGuiScreen {

    private static final int BTN_ENABLED = 1;
    private static final int BTN_HUD = 2;
    private static final int BTN_PRIMARY_OPTION = 3;
    private static final int BTN_SECONDARY_OPTION = 4;
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
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;

    private boolean draftEnabled;
    private boolean draftStatusHudEnabled;

    public SingleItemFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        this.parentScreen = parentScreen;
        this.featureId = featureId == null ? "" : featureId.trim();
        this.title = title == null ? "物品功能设置" : title.trim();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        FeatureState state = getFeature();
        this.draftEnabled = state != null && state.isEnabled();
        this.draftStatusHudEnabled = state == null || state.isStatusHudEnabled();
        rebuildDescription(state);

        int panelWidth = Math.min(520, Math.max(360, this.width - 16));
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int fieldX = panelX + 18;
        int fieldWidth = panelWidth - 36;

        this.enabledButton = new ToggleGuiButton(BTN_ENABLED, fieldX, panelY + 84, fieldWidth, 20, "", this.draftEnabled);
        this.hudButton = new ToggleGuiButton(BTN_HUD, fieldX, panelY + 108, fieldWidth, 20, "", this.draftStatusHudEnabled);
        this.primaryOptionButton = new ThemedButton(BTN_PRIMARY_OPTION, fieldX, panelY + 140, fieldWidth, 20, "");
        this.secondaryOptionButton = new ThemedButton(BTN_SECONDARY_OPTION, fieldX, panelY + 164, fieldWidth, 20, "");
        int footerY = panelY + panelHeight - 30;
        int footerW = (fieldWidth - 12) / 3;
        this.saveButton = new ThemedButton(BTN_SAVE, fieldX, footerY, footerW, 20, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, fieldX + footerW + 6, footerY, footerW, 20, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, fieldX + (footerW + 6) * 2, footerY, footerW, 20, "取消");

        this.buttonList.add(this.enabledButton);
        this.buttonList.add(this.hudButton);
        this.buttonList.add(this.primaryOptionButton);
        this.buttonList.add(this.secondaryOptionButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);

        refreshButtonTexts();
    }

    private FeatureState getFeature() {
        return ItemFeatureManager.getFeature(this.featureId);
    }

    private void rebuildDescription(FeatureState state) {
        this.descriptionLines.clear();
        String text = state == null ? "当前功能不存在。" : "§7" + state.description;
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(text, Math.max(120, this.width - 80)));
    }

    private void refreshButtonTexts() {
        if (this.enabledButton != null) {
            this.enabledButton.setEnabledState(this.draftEnabled);
            this.enabledButton.displayString = "功能状态 : " + (this.draftEnabled ? "开启" : "关闭");
        }
        if (this.hudButton != null) {
            this.hudButton.setEnabledState(this.draftStatusHudEnabled);
            this.hudButton.displayString = "状态HUD : " + (this.draftStatusHudEnabled ? "开启" : "关闭");
        }
        if (this.primaryOptionButton != null) {
            this.primaryOptionButton.visible = true;
            this.primaryOptionButton.enabled = true;
            this.primaryOptionButton.displayString = buildPrimaryOptionLabel();
        }
        if (this.secondaryOptionButton != null) {
            this.secondaryOptionButton.visible = shouldShowSecondaryOption();
            this.secondaryOptionButton.enabled = this.secondaryOptionButton.visible;
            this.secondaryOptionButton.displayString = buildSecondaryOptionLabel();
        }
    }

    private String buildPrimaryOptionLabel() {
        switch (this.featureId) {
        case "chest_steal":
            return "箱子窃取间隔 : " + ItemFeatureManager.getChestStealDelayTicks() + " tick";
        case "auto_equip":
            return "扫描间隔 : " + ItemFeatureManager.getAutoEquipIntervalTicks() + " tick";
        case "always_critical":
            return "触发方式 : 攻击前补发暴击包";
        case "attack_no_cooldown":
            return "触发方式 : 按住左键自动满蓄力出刀";
        case "drop_all":
            return "丢弃关键词 : " + (ItemFeatureManager.getDropAllKeywordsText().isEmpty()
                    ? "未配置" : ItemFeatureManager.getDropAllKeywordsText());
        case "inventory_sort":
            return "整理范围 : 主背包 9-35 格";
        case "shulker_preview":
            return "预览模式 : 悬停显示";
        default:
            return "无额外设置";
        }
    }

    private boolean shouldShowSecondaryOption() {
        return "drop_all".equals(this.featureId);
    }

    private String buildSecondaryOptionLabel() {
        if ("drop_all".equals(this.featureId)) {
            return "丢弃间隔 : " + ItemFeatureManager.getDropAllDelayTicks() + " tick";
        }
        return "";
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
            handlePrimaryOption();
            return;
        case BTN_SECONDARY_OPTION:
            handleSecondaryOption();
            return;
        case BTN_SAVE:
            ItemFeatureManager.setEnabled(this.featureId, this.draftEnabled);
            ItemFeatureManager.setFeatureStatusHudEnabled(this.featureId, this.draftStatusHudEnabled);
            this.mc.setScreen(this.parentScreen);
            return;
        case BTN_DEFAULT:
            ItemFeatureManager.resetFeature(this.featureId);
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

    private void handlePrimaryOption() {
        switch (this.featureId) {
        case "chest_steal":
            openIntegerInput("输入箱子窃取间隔 (0 - 20 tick)", ItemFeatureManager.getChestStealDelayTicks(), 0, 20,
                    ItemFeatureManager::setChestStealDelayTicks);
            return;
        case "auto_equip":
            openIntegerInput("输入自动装备扫描间隔 (1 - 40 tick)", ItemFeatureManager.getAutoEquipIntervalTicks(), 1, 40,
                    ItemFeatureManager::setAutoEquipIntervalTicks);
            return;
        case "drop_all":
            this.mc.setScreen(new GuiTextInput(this, "输入丢弃关键词（逗号分隔）",
                    ItemFeatureManager.getDropAllKeywordsText(), value -> {
                        ItemFeatureManager.setDropAllKeywordsText(value);
                        refreshButtonTexts();
                        this.mc.setScreen(this);
                    }));
            return;
        default:
            return;
        }
    }

    private void handleSecondaryOption() {
        if (!"drop_all".equals(this.featureId)) {
            return;
        }
        openIntegerInput("输入丢弃间隔 (0 - 20 tick)", ItemFeatureManager.getDropAllDelayTicks(), 0, 20,
                ItemFeatureManager::setDropAllDelayTicks);
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
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int fieldX = panelX + 18;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, this.title, this.fontRenderer);

        FeatureState state = getFeature();
        drawString(this.fontRenderer, "§7左键主界面为总开关；这里可配置物品功能与状态HUD。", fieldX, panelY + 24, 0xFFFFFFFF);
        int descY = panelY + 42;
        for (String line : this.descriptionLines) {
            drawString(this.fontRenderer, line, fieldX, descY, 0xFFE2F2FF);
            descY += this.fontRenderer.FONT_HEIGHT + 2;
        }
        drawString(this.fontRenderer, "§7运行状态: §f" + ItemFeatureManager.getFeatureRuntimeSummary(this.featureId), fieldX,
                panelY + 194, 0xFFFFFFFF);
        drawString(this.fontRenderer, "§7当前草稿: " + (this.draftEnabled ? "§a开启" : "§c关闭") + " §7| HUD: "
                + (this.draftStatusHudEnabled ? "§a开启" : "§c关闭"), fieldX, panelY + 208, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}





