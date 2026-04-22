package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.handlers.AdExpPanelHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAdExpPanelConfig extends ThemedGuiScreen {

    private final GuiScreen parent;
    private GuiButton currentValueBtn;
    private GuiButton currentValueModeBtn;
    private GuiButton currentTotalBtn;
    private GuiButton currentLevelBtn;
    private GuiButton nextLevelBtn;
    private GuiButton progressBtn;
    private GuiTextField progressDecimalField;

    public GuiAdExpPanelConfig(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelW = 360;
        int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int bx = panelX + 20;
        int by = panelY + 36;
        int bw = 320;
        int bh = 20;
        int gap = 24;

        currentValueBtn = new ThemedButton(10, bx, by, bw, bh, textForToggle("gui.ad_exp_panel.cfg.current_value", AdExpPanelHandler.showCurrentValue));
        by += gap;
        currentValueModeBtn = new ThemedButton(15, bx, by, bw, bh, currentValueModeText());
        by += gap;
        currentTotalBtn = new ThemedButton(11, bx, by, bw, bh, textForToggle("gui.ad_exp_panel.cfg.current_total", AdExpPanelHandler.showCurrentTotalExp));
        by += gap;
        currentLevelBtn = new ThemedButton(12, bx, by, bw, bh, textForToggle("gui.ad_exp_panel.cfg.current_level", AdExpPanelHandler.showCurrentLevel));
        by += gap;
        nextLevelBtn = new ThemedButton(13, bx, by, bw, bh, textForToggle("gui.ad_exp_panel.cfg.next_level", AdExpPanelHandler.showNextLevel));
        by += gap;
        progressBtn = new ThemedButton(14, bx, by, bw, bh, textForToggle("gui.ad_exp_panel.cfg.progress", AdExpPanelHandler.showProgress));

        this.buttonList.add(currentValueBtn);
        this.buttonList.add(currentValueModeBtn);
        this.buttonList.add(currentTotalBtn);
        this.buttonList.add(currentLevelBtn);
        this.buttonList.add(nextLevelBtn);
        this.buttonList.add(progressBtn);

        progressDecimalField = new GuiTextField(20, this.fontRenderer, panelX + 180, panelY + 198, 50, 20);
        progressDecimalField.setText(String.valueOf(AdExpPanelHandler.progressDecimalPlaces));

        this.buttonList.add(new ThemedButton(100, panelX + panelW - 90, panelY + panelH - 28, 70, 20, I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(101, panelX + 20, panelY + panelH - 28, 70, 20, I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 10:
                AdExpPanelHandler.showCurrentValue = !AdExpPanelHandler.showCurrentValue;
                currentValueBtn.displayString = textForToggle("gui.ad_exp_panel.cfg.current_value", AdExpPanelHandler.showCurrentValue);
                return;
            case 11:
                AdExpPanelHandler.showCurrentTotalExp = !AdExpPanelHandler.showCurrentTotalExp;
                currentTotalBtn.displayString = textForToggle("gui.ad_exp_panel.cfg.current_total", AdExpPanelHandler.showCurrentTotalExp);
                return;
            case 15:
                AdExpPanelHandler.currentValueUseTotalExp = !AdExpPanelHandler.currentValueUseTotalExp;
                currentValueModeBtn.displayString = currentValueModeText();
                return;
            case 12:
                AdExpPanelHandler.showCurrentLevel = !AdExpPanelHandler.showCurrentLevel;
                currentLevelBtn.displayString = textForToggle("gui.ad_exp_panel.cfg.current_level", AdExpPanelHandler.showCurrentLevel);
                return;
            case 13:
                AdExpPanelHandler.showNextLevel = !AdExpPanelHandler.showNextLevel;
                nextLevelBtn.displayString = textForToggle("gui.ad_exp_panel.cfg.next_level", AdExpPanelHandler.showNextLevel);
                return;
            case 14:
                AdExpPanelHandler.showProgress = !AdExpPanelHandler.showProgress;
                progressBtn.displayString = textForToggle("gui.ad_exp_panel.cfg.progress", AdExpPanelHandler.showProgress);
                return;
            case 100:
                AdExpPanelHandler.progressDecimalPlaces = Math.max(0,
                        Math.min(4, parseIntOrDefault(progressDecimalField.getText(), AdExpPanelHandler.progressDecimalPlaces)));
                AdExpPanelHandler.saveConfig();
                this.mc.setScreen(parent);
                return;
            case 101:
                this.mc.setScreen(parent);
                return;
            default:
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelW = 360;
        int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.ad_exp_panel.cfg.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.ad_exp_panel.cfg.progress_decimal"), panelX + 20, panelY + 204, 0xFFFFFF);
        this.drawString(this.fontRenderer, I18n.format("gui.ad_exp_panel.cfg.tip"), panelX + 20, panelY + 232, 0xFFBBBBBB);

        drawThemedTextField(progressDecimalField);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (progressDecimalField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        progressDecimalField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String textForToggle(String key, boolean on) {
        return I18n.format(key) + ": " + I18n.format(on ? "gui.common.enabled" : "gui.common.disabled");
    }

    private int parseIntOrDefault(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String currentValueModeText() {
        return I18n.format("gui.ad_exp_panel.cfg.current_value_mode") + ": "
                + I18n.format(AdExpPanelHandler.currentValueUseTotalExp
                        ? "gui.ad_exp_panel.cfg.current_value_mode.total"
                        : "gui.ad_exp_panel.cfg.current_value_mode.level");
    }
}






