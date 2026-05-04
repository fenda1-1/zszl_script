package com.zszl.zszlScriptMod.otherfeatures.gui.world;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiCoordDisplayConfigScreen extends ThemedGuiScreen {

    private static final int BTN_SAVE = 1;
    private static final int BTN_DEFAULT = 2;
    private static final int BTN_CANCEL = 3;
    private static final int BTN_ENABLED = 100;
    private static final int BTN_HUD = 101;
    private static final int BTN_SHOW_X = 200;
    private static final int BTN_SHOW_Y = 201;
    private static final int BTN_SHOW_Z = 202;
    private static final int BTN_SHOW_FACING = 203;
    private static final int BTN_SHOW_BIOME = 204;

    private final GuiScreen parentScreen;
    private ToggleGuiButton enabledButton;
    private ToggleGuiButton hudButton;
    private ToggleGuiButton showXButton;
    private ToggleGuiButton showYButton;
    private ToggleGuiButton showZButton;
    private ToggleGuiButton showFacingButton;
    private ToggleGuiButton showBiomeButton;

    private boolean draftEnabled;
    private boolean draftHudEnabled;
    private boolean draftShowX;
    private boolean draftShowY;
    private boolean draftShowZ;
    private boolean draftShowFacing;
    private boolean draftShowBiome;

    public GuiCoordDisplayConfigScreen(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        loadDraft();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int contentX = panelX + 12;
        int fullWidth = panelWidth - 24;
        int halfWidth = (fullWidth - 6) / 2;
        int y = panelY + 74;

        enabledButton = new ToggleGuiButton(BTN_ENABLED, contentX, y, halfWidth, 20, "", draftEnabled);
        hudButton = new ToggleGuiButton(BTN_HUD, contentX + halfWidth + 6, y, halfWidth, 20, "", draftHudEnabled);
        y += 28;
        showXButton = new ToggleGuiButton(BTN_SHOW_X, contentX, y, halfWidth, 20, "", draftShowX);
        showYButton = new ToggleGuiButton(BTN_SHOW_Y, contentX + halfWidth + 6, y, halfWidth, 20, "", draftShowY);
        y += 28;
        showZButton = new ToggleGuiButton(BTN_SHOW_Z, contentX, y, halfWidth, 20, "", draftShowZ);
        showFacingButton = new ToggleGuiButton(BTN_SHOW_FACING, contentX + halfWidth + 6, y, halfWidth, 20, "",
                draftShowFacing);
        y += 28;
        showBiomeButton = new ToggleGuiButton(BTN_SHOW_BIOME, contentX, y, fullWidth, 20, "", draftShowBiome);

        int footerY = panelY + getPanelHeight() - 28;
        int footerGap = 6;
        int footerButtonW = Math.max(64, (panelWidth - 24 - footerGap * 2) / 3);
        int footerStartX = panelX + (panelWidth - (footerButtonW * 3 + footerGap * 2)) / 2;

        this.buttonList.add(enabledButton);
        this.buttonList.add(hudButton);
        this.buttonList.add(showXButton);
        this.buttonList.add(showYButton);
        this.buttonList.add(showZButton);
        this.buttonList.add(showFacingButton);
        this.buttonList.add(showBiomeButton);
        this.buttonList.add(new ThemedButton(BTN_SAVE, footerStartX, footerY, footerButtonW, 20, "§a保存并关闭"));
        this.buttonList.add(
                new ThemedButton(BTN_DEFAULT, footerStartX + footerButtonW + footerGap, footerY, footerButtonW, 20,
                        "§e恢复默认"));
        this.buttonList.add(new ThemedButton(BTN_CANCEL, footerStartX + (footerButtonW + footerGap) * 2, footerY,
                footerButtonW, 20, "取消"));
        refreshButtonTexts();
    }

    private void loadDraft() {
        draftEnabled = WorldFeatureManager.isEnabled("coord_display");
        draftHudEnabled = WorldFeatureManager.isFeatureStatusHudEnabled("coord_display");
        draftShowX = WorldFeatureManager.coordDisplayShowX;
        draftShowY = WorldFeatureManager.coordDisplayShowY;
        draftShowZ = WorldFeatureManager.coordDisplayShowZ;
        draftShowFacing = WorldFeatureManager.coordDisplayShowFacing;
        draftShowBiome = WorldFeatureManager.coordDisplayShowBiome;
    }

    private void refreshButtonTexts() {
        enabledButton.setEnabledState(draftEnabled);
        enabledButton.displayString = "坐标显示总开关: " + boolText(draftEnabled);
        hudButton.setEnabledState(draftHudEnabled);
        hudButton.displayString = "状态HUD: " + boolText(draftHudEnabled);

        showXButton.setEnabledState(draftShowX);
        showXButton.displayString = "显示 X: " + boolText(draftShowX);
        showYButton.setEnabledState(draftShowY);
        showYButton.displayString = "显示 Y: " + boolText(draftShowY);
        showZButton.setEnabledState(draftShowZ);
        showZButton.displayString = "显示 Z: " + boolText(draftShowZ);
        showFacingButton.setEnabledState(draftShowFacing);
        showFacingButton.displayString = "显示朝向: " + boolText(draftShowFacing);
        showBiomeButton.setEnabledState(draftShowBiome);
        showBiomeButton.displayString = "显示生物群系: " + boolText(draftShowBiome);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
        case BTN_SAVE:
            WorldFeatureManager.setEnabled("coord_display", draftEnabled);
            WorldFeatureManager.setFeatureStatusHudEnabled("coord_display", draftHudEnabled);
            WorldFeatureManager.coordDisplayShowX = draftShowX;
            WorldFeatureManager.coordDisplayShowY = draftShowY;
            WorldFeatureManager.coordDisplayShowZ = draftShowZ;
            WorldFeatureManager.coordDisplayShowFacing = draftShowFacing;
            WorldFeatureManager.coordDisplayShowBiome = draftShowBiome;
            WorldFeatureManager.saveConfig();
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        case BTN_DEFAULT:
            draftEnabled = false;
            draftHudEnabled = true;
            draftShowX = true;
            draftShowY = true;
            draftShowZ = true;
            draftShowFacing = true;
            draftShowBiome = true;
            break;
        case BTN_CANCEL:
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        case BTN_ENABLED:
            draftEnabled = !draftEnabled;
            break;
        case BTN_HUD:
            draftHudEnabled = !draftHudEnabled;
            break;
        case BTN_SHOW_X:
            draftShowX = !draftShowX;
            break;
        case BTN_SHOW_Y:
            draftShowY = !draftShowY;
            break;
        case BTN_SHOW_Z:
            draftShowZ = !draftShowZ;
            break;
        case BTN_SHOW_FACING:
            draftShowFacing = !draftShowFacing;
            break;
        case BTN_SHOW_BIOME:
            draftShowBiome = !draftShowBiome;
            break;
        default:
            break;
        }
        refreshButtonTexts();
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "坐标显示设置", this.fontRenderer);

        List<String> introLines = new ArrayList<>();
        introLines.add("左键其他功能页中的“坐标显示”为总开关；这里可以把 HUD 里的字段拆开控制。");
        introLines.add("保存后会同时影响世界状态 HUD 和坐标显示运行摘要。");
        int introY = panelY + 26;
        for (String line : introLines) {
            drawString(this.fontRenderer, line, panelX + 14, introY, GuiTheme.SUB_TEXT);
            introY += 10;
        }

        drawRect(panelX + 12, panelY + 52, panelX + panelWidth - 12, panelY + 68, 0x66324E67);
        drawString(this.fontRenderer, "草稿预览: " + buildPreviewText(), panelX + 16, panelY + 57, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String buildPreviewText() {
        List<String> parts = new ArrayList<>();
        if (draftShowX) {
            parts.add("X");
        }
        if (draftShowY) {
            parts.add("Y");
        }
        if (draftShowZ) {
            parts.add("Z");
        }
        if (draftShowFacing) {
            parts.add("朝向");
        }
        if (draftShowBiome) {
            parts.add("生物群系");
        }
        if (parts.isEmpty()) {
            return "未选择任何字段";
        }
        return String.join(" / ", parts);
    }

    private int getPanelWidth() {
        return Math.min(520, this.width - 20);
    }

    private int getPanelHeight() {
        return Math.min(260, this.height - 20);
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private String boolText(boolean enabled) {
        return enabled ? "§a开启" : "§c关闭";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
