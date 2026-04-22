package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.handlers.DeathAutoRejoinHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Arrays;

public class GuiDeathAutoRejoinConfig extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    private GuiButton btnResumeLastPath;
    private GuiButton btnResumeMode;
    private GuiButton btnLineMode;
    private GuiButton btnLineModeNormal;
    private GuiButton btnLineModeHidden;
    private GuiTextField teleportDetectMsField;
    private GuiTextField deathRespawnXField;
    private GuiTextField deathRespawnYField;
    private GuiTextField deathRespawnZField;
    private GuiTextField deathRespawnRadiusField;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public GuiDeathAutoRejoinConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        panelWidth = 340;
        panelHeight = 340;
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        int y = panelY + 30;

        btnResumeLastPath = new ThemedButton(0, panelX, y, panelWidth, 20,
                I18n.format("gui.death_rejoin.resume_last_path",
                        stateText(DeathAutoRejoinHandler.deathAutoResumeLastPath)));
        this.buttonList.add(btnResumeLastPath);
        y += 28;

        btnResumeMode = new ThemedButton(1, panelX, y, panelWidth, 20,
                I18n.format("gui.death_rejoin.resume_mode", resumeModeText()));
        this.buttonList.add(btnResumeMode);
        y += 28;

        btnLineMode = new ThemedButton(5, panelX, y, panelWidth, 20,
                I18n.format("gui.death_rejoin.line_mode", rejoinLineModeText()));
        this.buttonList.add(btnLineMode);
        y += 24;

        btnLineModeNormal = new ThemedButton(6, panelX, y, panelWidth, 20,
                "· " + I18n.format("gui.death_rejoin.line_mode.normal"));
        btnLineModeNormal.visible = false;
        this.buttonList.add(btnLineModeNormal);

        btnLineModeHidden = new ThemedButton(7, panelX, y + 20, panelWidth, 20,
                "· " + I18n.format("gui.death_rejoin.line_mode.hidden"));
        btnLineModeHidden.visible = false;
        this.buttonList.add(btnLineModeHidden);

        y += 8;

        teleportDetectMsField = new GuiTextField(100, this.fontRenderer, panelX, y, panelWidth, 20);
        teleportDetectMsField.setMaxStringLength(10);
        teleportDetectMsField.setText(String.valueOf(DeathAutoRejoinHandler.deathAutoTeleportDetectMs));
        y += 36;

        deathRespawnXField = new GuiTextField(101, this.fontRenderer, panelX, y, panelWidth, 20);
        deathRespawnXField.setMaxStringLength(16);
        deathRespawnXField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnCenterX));
        y += 32;

        deathRespawnYField = new GuiTextField(102, this.fontRenderer, panelX, y, panelWidth, 20);
        deathRespawnYField.setMaxStringLength(16);
        deathRespawnYField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnCenterY));
        y += 32;

        deathRespawnZField = new GuiTextField(103, this.fontRenderer, panelX, y, panelWidth, 20);
        deathRespawnZField.setMaxStringLength(16);
        deathRespawnZField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnCenterZ));
        y += 32;

        deathRespawnRadiusField = new GuiTextField(104, this.fontRenderer, panelX, y, panelWidth, 20);
        deathRespawnRadiusField.setMaxStringLength(16);
        deathRespawnRadiusField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnRadius));
        y += 30;

        int btnWidth = (panelWidth - 12) / 3;
        this.buttonList.add(new ThemedButton(2, panelX, y, btnWidth, 20, I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(3, panelX + btnWidth + 6, y, btnWidth, 20,
                I18n.format("gui.common.reset_default")));
        this.buttonList.add(new ThemedButton(4, panelX + (btnWidth + 6) * 2, y, btnWidth, 20,
                I18n.format("gui.common.back")));

        updateButtonsEnabled();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            collapseLineModeDropdown();
            DeathAutoRejoinHandler.deathAutoResumeLastPath = !DeathAutoRejoinHandler.deathAutoResumeLastPath;
            btnResumeLastPath.displayString = I18n.format("gui.death_rejoin.resume_last_path",
                    stateText(DeathAutoRejoinHandler.deathAutoResumeLastPath));
            updateButtonsEnabled();
        } else if (button.id == 1) {
            collapseLineModeDropdown();
            DeathAutoRejoinHandler.deathAutoResumeMode = DeathAutoRejoinHandler.deathAutoResumeMode == 0 ? 1 : 0;
            btnResumeMode.displayString = I18n.format("gui.death_rejoin.resume_mode", resumeModeText());
        } else if (button.id == 5) {
            boolean expand = !btnLineModeNormal.visible;
            btnLineModeNormal.visible = expand;
            btnLineModeHidden.visible = expand;
        } else if (button.id == 6) {
            DeathAutoRejoinHandler.deathAutoRejoinLineMode = 0;
            btnLineMode.displayString = I18n.format("gui.death_rejoin.line_mode", rejoinLineModeText());
            collapseLineModeDropdown();
        } else if (button.id == 7) {
            DeathAutoRejoinHandler.deathAutoRejoinLineMode = 1;
            btnLineMode.displayString = I18n.format("gui.death_rejoin.line_mode", rejoinLineModeText());
            collapseLineModeDropdown();
        } else if (button.id == 2) {
            collapseLineModeDropdown();
            applyAllFields();
            DeathAutoRejoinHandler.saveConfig();
            this.mc.setScreen(this.parentScreen);
        } else if (button.id == 3) {
            collapseLineModeDropdown();
            resetDeathRespawnDefaultsToFields();
        } else if (button.id == 4) {
            collapseLineModeDropdown();
            this.mc.setScreen(this.parentScreen);
        }
    }

    private void collapseLineModeDropdown() {
        if (btnLineModeNormal != null) {
            btnLineModeNormal.visible = false;
        }
        if (btnLineModeHidden != null) {
            btnLineModeHidden.visible = false;
        }
    }

    private void updateButtonsEnabled() {
        if (btnResumeMode != null) {
            btnResumeMode.enabled = DeathAutoRejoinHandler.deathAutoResumeLastPath;
        }
    }

    private void applyAllFields() {
        applyDetectMsFromField();
        applyDeathRespawnFromFields();
    }

    private void applyDetectMsFromField() {
        if (teleportDetectMsField == null) {
            return;
        }
        try {
            int ms = Integer.parseInt(teleportDetectMsField.getText().trim());
            DeathAutoRejoinHandler.deathAutoTeleportDetectMs = Math.max(200, ms);
            teleportDetectMsField.setText(String.valueOf(DeathAutoRejoinHandler.deathAutoTeleportDetectMs));
        } catch (Exception ignored) {
            DeathAutoRejoinHandler.deathAutoTeleportDetectMs = 2000;
            teleportDetectMsField.setText("2000");
        }
    }

    private void applyDeathRespawnFromFields() {
        try {
            DeathAutoRejoinHandler.deathRespawnCenterX = Double.parseDouble(deathRespawnXField.getText().trim());
            DeathAutoRejoinHandler.deathRespawnCenterY = Double.parseDouble(deathRespawnYField.getText().trim());
            DeathAutoRejoinHandler.deathRespawnCenterZ = Double.parseDouble(deathRespawnZField.getText().trim());
            DeathAutoRejoinHandler.deathRespawnRadius = Math.max(1.0,
                    Double.parseDouble(deathRespawnRadiusField.getText().trim()));
            deathRespawnRadiusField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnRadius));
        } catch (Exception ignored) {
            resetDeathRespawnDefaultsToFields();
        }
    }

    private void resetDeathRespawnDefaultsToFields() {
        DeathAutoRejoinHandler.resetRespawnDefaults();
        deathRespawnXField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnCenterX));
        deathRespawnYField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnCenterY));
        deathRespawnZField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnCenterZ));
        deathRespawnRadiusField.setText(String.valueOf(DeathAutoRejoinHandler.deathRespawnRadius));
    }

    private String stateText(boolean enabled) {
        return I18n.format(enabled ? "gui.common.enabled" : "gui.common.disabled");
    }

    private String resumeModeText() {
        return DeathAutoRejoinHandler.deathAutoResumeMode == 0
                ? I18n.format("gui.death_rejoin.resume_mode.continue")
                : I18n.format("gui.death_rejoin.resume_mode.restart");
    }

    private String rejoinLineModeText() {
        return DeathAutoRejoinHandler.deathAutoRejoinLineMode == 1
                ? I18n.format("gui.death_rejoin.line_mode.hidden")
                : I18n.format("gui.death_rejoin.line_mode.normal");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (teleportDetectMsField != null && teleportDetectMsField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (deathRespawnXField != null && deathRespawnXField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (deathRespawnYField != null && deathRespawnYField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (deathRespawnZField != null && deathRespawnZField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (deathRespawnRadiusField != null && deathRespawnRadiusField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        boolean clickedInLineDropdownArea = btnLineMode != null &&
                mouseX >= btnLineMode.x && mouseX <= btnLineMode.x + btnLineMode.width &&
                mouseY >= btnLineMode.y && mouseY <= btnLineMode.y + 60;
        if (!clickedInLineDropdownArea) {
            collapseLineModeDropdown();
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (teleportDetectMsField != null) {
            teleportDetectMsField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (deathRespawnXField != null) {
            deathRespawnXField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (deathRespawnYField != null) {
            deathRespawnYField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (deathRespawnZField != null) {
            deathRespawnZField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (deathRespawnRadiusField != null) {
            deathRespawnRadiusField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (teleportDetectMsField != null) {
            teleportDetectMsField.updateCursorCounter();
        }
        if (deathRespawnXField != null) {
            deathRespawnXField.updateCursorCounter();
        }
        if (deathRespawnYField != null) {
            deathRespawnYField.updateCursorCounter();
        }
        if (deathRespawnZField != null) {
            deathRespawnZField.updateCursorCounter();
        }
        if (deathRespawnRadiusField != null) {
            deathRespawnRadiusField.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.death_rejoin.config.title"),
                this.fontRenderer);

        if (teleportDetectMsField != null) {
            drawString(this.fontRenderer, I18n.format("gui.death_rejoin.teleport_detect_ms"), panelX,
                    teleportDetectMsField.y - 11, 0xC0C0C0);
            drawThemedTextField(teleportDetectMsField);
        }

        if (deathRespawnXField != null) {
            drawString(this.fontRenderer, I18n.format("gui.death_rejoin.death_respawn_x"), panelX,
                    deathRespawnXField.y - 11, 0xC0C0C0);
            drawThemedTextField(deathRespawnXField);
        }
        if (deathRespawnYField != null) {
            drawString(this.fontRenderer, I18n.format("gui.death_rejoin.death_respawn_y"), panelX,
                    deathRespawnYField.y - 11, 0xC0C0C0);
            drawThemedTextField(deathRespawnYField);
        }
        if (deathRespawnZField != null) {
            drawString(this.fontRenderer, I18n.format("gui.death_rejoin.death_respawn_z"), panelX,
                    deathRespawnZField.y - 11, 0xC0C0C0);
            drawThemedTextField(deathRespawnZField);
        }
        if (deathRespawnRadiusField != null) {
            drawString(this.fontRenderer, I18n.format("gui.death_rejoin.death_respawn_radius"), panelX,
                    deathRespawnRadiusField.y - 11, 0xC0C0C0);
            drawThemedTextField(deathRespawnRadiusField);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (btnResumeLastPath != null && btnResumeLastPath.isMouseOver()) {
            String tooltip = I18n.format("gui.death_rejoin.resume_last_path.tooltip").replace("\\n", "\n");
            drawHoveringText(Arrays.asList(tooltip.split("\n")), mouseX, mouseY);
        } else if (btnResumeMode != null && btnResumeMode.isMouseOver()) {
            String tooltip = I18n.format("gui.death_rejoin.resume_mode.tooltip").replace("\\n", "\n");
            drawHoveringText(Arrays.asList(tooltip.split("\n")), mouseX, mouseY);
        } else if (btnLineMode != null && btnLineMode.isMouseOver()) {
            String tooltip = I18n.format("gui.death_rejoin.line_mode.tooltip").replace("\\n", "\n");
            drawHoveringText(Arrays.asList(tooltip.split("\n")), mouseX, mouseY);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}





