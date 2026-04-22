package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.handlers.KillTimerHandler;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;

public class GuiKillTimerConfig extends ThemedGuiScreen {

    private final GuiScreen parent;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField wField;
    private GuiTextField hField;
    private GuiTextField alphaField;
    private GuiTextField deathHoldSecField;
    private GuiButton deathModeButton;

    public GuiKillTimerConfig(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelW = 320;
        int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int fx = panelX + 120;
        int y = panelY + 34;

        xField = new GuiTextField(1, this.fontRenderer, fx, y, 160, 20);
        xField.setText(String.valueOf(KillTimerHandler.panelX));
        y += 28;

        yField = new GuiTextField(2, this.fontRenderer, fx, y, 160, 20);
        yField.setText(String.valueOf(KillTimerHandler.panelY));
        y += 28;

        wField = new GuiTextField(3, this.fontRenderer, fx, y, 160, 20);
        wField.setText(String.valueOf(KillTimerHandler.panelWidth));
        y += 28;

        hField = new GuiTextField(4, this.fontRenderer, fx, y, 160, 20);
        hField.setText(String.valueOf(KillTimerHandler.panelHeight));
        y += 28;

        alphaField = new GuiTextField(5, this.fontRenderer, fx, y, 160, 20);
        alphaField.setText(String.valueOf(KillTimerHandler.panelAlpha));
        y += 28;

        deathModeButton = new ThemedButton(20, fx, y, 160, 20, getDeathModeDisplay());
        this.buttonList.add(deathModeButton);
        y += 28;

        deathHoldSecField = new GuiTextField(6, this.fontRenderer, fx, y, 160, 20);
        deathHoldSecField.setText(String.valueOf(KillTimerHandler.deathPanelHoldSeconds));

        this.buttonList.add(new ThemedButton(10, panelX + 10, panelY + panelH - 28, 95, 20,
                I18n.format("gui.killtimer.btn.free_edit")));
        this.buttonList.add(new ThemedButton(11, panelX + 112, panelY + panelH - 28, 95, 20,
                I18n.format("gui.killtimer.btn.reset")));
        this.buttonList.add(new ThemedButton(100, panelX + 214, panelY + panelH - 28, 95, 20,
                I18n.format("gui.killtimer.btn.save")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 10) {
            this.mc.setScreen(null);
            KillTimerHandler.enterFreeEditMode();
            if (this.mc.player != null) {
                this.mc.player.sendSystemMessage(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString(
                        I18n.format("msg.killtimer.enter_free_edit")));
            }
            return;
        }
        if (button.id == 11) {
            KillTimerHandler.panelX = 0;
            KillTimerHandler.panelY = 22;
            KillTimerHandler.panelWidth = 219;
            KillTimerHandler.panelHeight = 100;
            KillTimerHandler.panelAlpha = 100;
            KillTimerHandler.deathDataMode = KillTimerHandler.DEATH_MODE_CHAT;
            KillTimerHandler.deathPanelHoldSeconds = 3;
            syncFromHandler();
            return;
        }
        if (button.id == 20) {
            if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_CHAT) {
                KillTimerHandler.deathDataMode = KillTimerHandler.DEATH_MODE_PANEL_HOLD;
            } else {
                KillTimerHandler.deathDataMode = KillTimerHandler.DEATH_MODE_CHAT;
            }
            deathModeButton.displayString = getDeathModeDisplay();
            return;
        }
        if (button.id == 100) {
            KillTimerHandler.panelX = Math.max(0, parseIntOrDefault(xField.getText(), KillTimerHandler.panelX));
            KillTimerHandler.panelY = Math.max(0, parseIntOrDefault(yField.getText(), KillTimerHandler.panelY));
            KillTimerHandler.panelWidth = Math.max(120,
                    parseIntOrDefault(wField.getText(), KillTimerHandler.panelWidth));
            KillTimerHandler.panelHeight = Math.max(60,
                    parseIntOrDefault(hField.getText(), KillTimerHandler.panelHeight));
            KillTimerHandler.panelAlpha = Math.max(30, Math.min(240,
                    parseIntOrDefault(alphaField.getText(), KillTimerHandler.panelAlpha)));
            KillTimerHandler.deathPanelHoldSeconds = Math.max(1,
                    parseIntOrDefault(deathHoldSecField.getText(), KillTimerHandler.deathPanelHoldSeconds));
            KillTimerHandler.saveConfig();
            this.mc.setScreen(parent);
        }
    }

    private void syncFromHandler() {
        xField.setText(String.valueOf(KillTimerHandler.panelX));
        yField.setText(String.valueOf(KillTimerHandler.panelY));
        wField.setText(String.valueOf(KillTimerHandler.panelWidth));
        hField.setText(String.valueOf(KillTimerHandler.panelHeight));
        alphaField.setText(String.valueOf(KillTimerHandler.panelAlpha));
        deathHoldSecField.setText(String.valueOf(KillTimerHandler.deathPanelHoldSeconds));
        if (deathModeButton != null) {
            deathModeButton.displayString = getDeathModeDisplay();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelW = 320;
        int panelH = 280;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.killtimer.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.x"), panelX + 12, panelY + 40, 0xFFFFFF);
        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.y"), panelX + 12, panelY + 68, 0xFFFFFF);
        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.w"), panelX + 12, panelY + 96, 0xFFFFFF);
        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.h"), panelX + 12, panelY + 124, 0xFFFFFF);
        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.alpha"), panelX + 12, panelY + 152, 0xFFFFFF);
        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.death_mode"), panelX + 12, panelY + 180,
                0xFFFFFF);

        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD) {
            this.drawString(this.fontRenderer, I18n.format("gui.killtimer.hold_sec"), panelX + 12, panelY + 208,
                    0xFFFFFF);
            drawThemedTextField(deathHoldSecField);
        }

        this.drawString(this.fontRenderer, I18n.format("gui.killtimer.tip"), panelX + 12, panelY + 236,
                0xFFBBBBBB);

        drawThemedTextField(xField);
        drawThemedTextField(yField);
        drawThemedTextField(wField);
        drawThemedTextField(hField);
        drawThemedTextField(alphaField);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (xField.textboxKeyTyped(typedChar, keyCode))
            return;
        if (yField.textboxKeyTyped(typedChar, keyCode))
            return;
        if (wField.textboxKeyTyped(typedChar, keyCode))
            return;
        if (hField.textboxKeyTyped(typedChar, keyCode))
            return;
        if (alphaField.textboxKeyTyped(typedChar, keyCode))
            return;
        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD
                && deathHoldSecField.textboxKeyTyped(typedChar, keyCode))
            return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        xField.mouseClicked(mouseX, mouseY, mouseButton);
        yField.mouseClicked(mouseX, mouseY, mouseButton);
        wField.mouseClicked(mouseX, mouseY, mouseButton);
        hField.mouseClicked(mouseX, mouseY, mouseButton);
        alphaField.mouseClicked(mouseX, mouseY, mouseButton);
        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD) {
            deathHoldSecField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private int parseIntOrDefault(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String getDeathModeDisplay() {
        return KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD
                ? I18n.format("gui.killtimer.mode.hold")
                : I18n.format("gui.killtimer.mode.chat");
    }
}






