package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.AutoUseItemRule;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditAutoUseItemRule extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final AutoUseItemRule rule;
    private final Consumer<AutoUseItemRule> onSave;

    private GuiTextField nameField;
    private GuiTextField intervalField;
    private GuiButton btnEnabled;
    private GuiButton btnUseMode;
    private GuiButton btnMatchMode;
    private final List<GuiTextField> allFields = new ArrayList<>();

    public GuiEditAutoUseItemRule(GuiScreen parent, AutoUseItemRule rule, Consumer<AutoUseItemRule> onSave) {
        this.parentScreen = parent;
        this.rule = (rule == null) ? new AutoUseItemRule() : rule;
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allFields.clear();

        int panelWidth = 280;
        int panelX = (this.width - panelWidth) / 2;
        int startY = this.height / 2 - 90;
        int y = startY;

        nameField = new GuiTextField(1, fontRenderer, panelX + 10, y + 14, panelWidth - 20, 20);
        nameField.setText(rule.name == null ? "" : rule.name);
        nameField.setMaxStringLength(Integer.MAX_VALUE);
        allFields.add(nameField);
        y += 45;

        btnEnabled = new ThemedButton(2, panelX + 10, y, panelWidth - 20, 20,
                I18n.format("gui.autouseitem.edit.enabled", onOff(rule.enabled)));
        this.buttonList.add(btnEnabled);
        y += 25;

        btnUseMode = new ThemedButton(3, panelX + 10, y, panelWidth - 20, 20,
                I18n.format("gui.autouseitem.edit.use_mode", useModeText(rule.useMode)));
        this.buttonList.add(btnUseMode);
        y += 25;

        btnMatchMode = new ThemedButton(4, panelX + 10, y, panelWidth - 20, 20,
                I18n.format("gui.autouseitem.edit.match_mode", matchModeText(rule.matchMode)));
        this.buttonList.add(btnMatchMode);
        y += 30;

        intervalField = new GuiTextField(5, fontRenderer, panelX + 10, y + 14, panelWidth - 20, 20);
        intervalField.setText(String.valueOf(Math.max(10, rule.intervalMs)));
        intervalField.setMaxStringLength(Integer.MAX_VALUE);
        allFields.add(intervalField);
        y += 45;

        int half = (panelWidth - 30) / 2;
        this.buttonList.add(new ThemedButton(10, panelX + 10, y, half, 20,
                "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(11, panelX + 20 + half, y, half, 20,
                I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 2:
                rule.enabled = !rule.enabled;
                btnEnabled.displayString = I18n.format("gui.autouseitem.edit.enabled", onOff(rule.enabled));
                break;
            case 3:
                rule.useMode = (rule.useMode == AutoUseItemRule.UseMode.LEFT_CLICK)
                        ? AutoUseItemRule.UseMode.RIGHT_CLICK
                        : AutoUseItemRule.UseMode.LEFT_CLICK;
                btnUseMode.displayString = I18n.format("gui.autouseitem.edit.use_mode", useModeText(rule.useMode));
                break;
            case 4:
                rule.matchMode = (rule.matchMode == AutoUseItemRule.MatchMode.EXACT)
                        ? AutoUseItemRule.MatchMode.CONTAINS
                        : AutoUseItemRule.MatchMode.EXACT;
                btnMatchMode.displayString = I18n.format("gui.autouseitem.edit.match_mode",
                        matchModeText(rule.matchMode));
                break;
            case 10:
                rule.name = nameField.getText() == null ? "" : nameField.getText().trim();
                try {
                    rule.intervalMs = Math.max(10, Integer.parseInt(intervalField.getText().trim()));
                } catch (Exception ignored) {
                    rule.intervalMs = 250;
                }
                onSave.accept(rule);
                mc.setScreen(parentScreen);
                break;
            case 11:
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 300;
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.autouseitem.edit.title"), this.fontRenderer);

        drawString(fontRenderer, I18n.format("gui.autouseitem.edit.name"), nameField.x, nameField.y - 10,
                GuiTheme.SUB_TEXT);
        drawString(fontRenderer, I18n.format("gui.autouseitem.edit.interval"), intervalField.x, intervalField.y - 10,
                GuiTheme.SUB_TEXT);

        for (GuiTextField field : allFields) {
            drawThemedTextField(field);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        for (GuiTextField field : allFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : allFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : allFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String onOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }

    private String useModeText(AutoUseItemRule.UseMode mode) {
        return mode == AutoUseItemRule.UseMode.LEFT_CLICK
                ? I18n.format("gui.autouseitem.mode.left")
                : I18n.format("gui.autouseitem.mode.right");
    }

    private String matchModeText(AutoUseItemRule.MatchMode mode) {
        return mode == AutoUseItemRule.MatchMode.EXACT
                ? I18n.format("gui.autouseitem.match.exact")
                : I18n.format("gui.autouseitem.match.contains");
    }
}






