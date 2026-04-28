package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.system.ConditionalRule;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditConditionalRule extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final ConditionalRule rule;
    private final Consumer<ConditionalRule> onSave;

    private GuiTextField nameField, xField, yField, zField, rangeField, loopCountField, cooldownField;
    private GuiButton btnEnabled, btnStopOnExit, btnRunOnce, btnSelectSequence, btnGetCoords;
    private List<GuiTextField> allTextFields = new ArrayList<>();

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    public GuiEditConditionalRule(GuiScreen parent, ConditionalRule rule, Consumer<ConditionalRule> onSave) {
        this.parentScreen = parent;
        this.rule = rule;
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allTextFields.clear();

        int panelWidth = 240;
        int panelX = (this.width - panelWidth) / 2;
        int startY = this.height / 2 - 120;
        int currentY = startY;

        nameField = new GuiTextField(0, fontRenderer, panelX, currentY, panelWidth, 20);
        nameField.setText(rule.name);
        allTextFields.add(nameField);
        currentY += 25;

        btnEnabled = new ThemedButton(1, panelX, currentY, panelWidth, 20,
                I18n.format("gui.conditional.btn.enabled", stateOnOff(rule.enabled)));
        this.buttonList.add(btnEnabled);
        currentY += 25;

        String seqButtonText = (rule.sequenceName == null || rule.sequenceName.isEmpty())
                ? I18n.format("gui.conditional.btn.select_seq")
                : "§f" + rule.sequenceName;
        btnSelectSequence = new ThemedButton(2, panelX, currentY, panelWidth, 20, seqButtonText);
        this.buttonList.add(btnSelectSequence);
        currentY += 30;

        int fieldWidth = (panelWidth - 10) / 2;
        xField = new GuiTextField(3, fontRenderer, panelX, currentY, fieldWidth, 20);
        yField = new GuiTextField(4, fontRenderer, panelX + fieldWidth + 10, currentY, fieldWidth, 20);
        allTextFields.add(xField);
        allTextFields.add(yField);
        currentY += 25;

        zField = new GuiTextField(5, fontRenderer, panelX, currentY, fieldWidth, 20);
        rangeField = new GuiTextField(6, fontRenderer, panelX + fieldWidth + 10, currentY, fieldWidth, 20);
        allTextFields.add(zField);
        allTextFields.add(rangeField);
        currentY += 25;

        btnGetCoords = new ThemedButton(7, panelX, currentY, panelWidth, 20,
                I18n.format("gui.conditional.btn.get_coords"));
        this.buttonList.add(btnGetCoords);
        currentY += 30;

        loopCountField = new GuiTextField(8, fontRenderer, panelX, currentY, fieldWidth, 20);
        cooldownField = new GuiTextField(9, fontRenderer, panelX + fieldWidth + 10, currentY, fieldWidth, 20);
        allTextFields.add(loopCountField);
        allTextFields.add(cooldownField);
        currentY += 25;

        btnStopOnExit = new ThemedButton(10, panelX, currentY, panelWidth, 20,
                I18n.format("gui.conditional.btn.stop_on_exit", yesNo(rule.stopOnExit)));
        this.buttonList.add(btnStopOnExit);
        currentY += 25;

        btnRunOnce = new ThemedButton(11, panelX, currentY, panelWidth, 20,
                I18n.format("gui.conditional.btn.run_once", yesNo(rule.runOncePerEntry)));
        this.buttonList.add(btnRunOnce);
        currentY += 25;

        this.buttonList.add(
                new ThemedButton(12, panelX, currentY, (panelWidth - 10) / 2, 20,
                        "§a" + I18n.format("gui.common.save")));
        this.buttonList
                .add(new ThemedButton(13, panelX + (panelWidth - 10) / 2 + 10, currentY, (panelWidth - 10) / 2, 20,
                        I18n.format("gui.common.cancel")));

        xField.setText(COORD_FORMAT.format(rule.centerX));
        yField.setText(COORD_FORMAT.format(rule.centerY));
        zField.setText(COORD_FORMAT.format(rule.centerZ));
        rangeField.setText(COORD_FORMAT.format(rule.range));
        loopCountField.setText(String.valueOf(rule.loopCount));
        cooldownField.setText(String.valueOf(rule.cooldownSeconds));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                rule.enabled = !rule.enabled;
                btnEnabled.displayString = I18n.format("gui.conditional.btn.enabled", stateOnOff(rule.enabled));
                break;
            case 2:
                mc.setScreen(new GuiSequenceSelector(this, seq -> {
                    rule.sequenceName = seq;
                    mc.setScreen(this);
                }));
                break;
            case 7:
                if (mc.player != null) {
                    xField.setText(COORD_FORMAT.format(mc.player.getX()));
                    yField.setText(COORD_FORMAT.format(mc.player.getY()));
                    zField.setText(COORD_FORMAT.format(mc.player.getZ()));
                }
                break;
            case 10:
                rule.stopOnExit = !rule.stopOnExit;
                btnStopOnExit.displayString = I18n.format("gui.conditional.btn.stop_on_exit", yesNo(rule.stopOnExit));
                break;
            case 11:
                rule.runOncePerEntry = !rule.runOncePerEntry;
                btnRunOnce.displayString = I18n.format("gui.conditional.btn.run_once", yesNo(rule.runOncePerEntry));
                break;
            case 12: // 保存
                try {
                    rule.name = nameField.getText();
                    rule.centerX = Double.parseDouble(xField.getText().replace(',', '.'));
                    rule.centerY = Double.parseDouble(yField.getText().replace(',', '.'));
                    rule.centerZ = Double.parseDouble(zField.getText().replace(',', '.'));
                    rule.range = Double.parseDouble(rangeField.getText().replace(',', '.'));
                    rule.loopCount = Integer.parseInt(loopCountField.getText());
                    rule.cooldownSeconds = Integer.parseInt(cooldownField.getText());
                    onSave.accept(rule);
                } catch (NumberFormatException e) {
                    // 可以在这里加一个错误提示
                }
                break;
            case 13: // 取消
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 260;
        int panelHeight = 310;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 142;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.conditional.edit.title"), this.fontRenderer);

        drawString(fontRenderer, I18n.format("gui.conditional.rule_name"), nameField.x, nameField.y - 12,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(nameField);

        drawString(fontRenderer, I18n.format("gui.conditional.center_xy"), xField.x, xField.y - 12, GuiTheme.SUB_TEXT);
        drawThemedTextField(xField);
        drawThemedTextField(yField);

        drawString(fontRenderer, I18n.format("gui.conditional.center_zr"), zField.x, zField.y - 12, GuiTheme.SUB_TEXT);
        drawThemedTextField(zField);
        drawThemedTextField(rangeField);

        drawString(fontRenderer, I18n.format("gui.conditional.loop_cooldown"), loopCountField.x, loopCountField.y - 12,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(loopCountField);
        drawThemedTextField(cooldownField);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        for (GuiTextField field : allTextFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : allTextFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : allTextFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String stateOnOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }

    private String yesNo(boolean yes) {
        return I18n.format(yes ? "gui.common.yes" : "gui.common.no");
    }
}







