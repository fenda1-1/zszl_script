// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/config/GuiEditAutoPickupRule.java
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.system.AutoPickupRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditAutoPickupRule extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final AutoPickupRule rule;
    private final Consumer<AutoPickupRule> onSave;

    private GuiTextField nameField, xField, yField, zField, radiusField, delayField;
    private GuiButton btnEnabled, btnStopOnExit, btnSelectSequence, btnGetCoords;
    private List<GuiTextField> allTextFields = new ArrayList<>();

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    public GuiEditAutoPickupRule(GuiScreen parent, AutoPickupRule rule, Consumer<AutoPickupRule> onSave) {
        this.parentScreen = parent;
        this.rule = (rule == null) ? new AutoPickupRule() : rule;
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allTextFields.clear();

        int panelWidth = 240;
        int panelX = (this.width - panelWidth) / 2;
        int startY = this.height / 2 - 140;
        int currentY = startY;

        nameField = new GuiTextField(0, fontRenderer, panelX, currentY, panelWidth, 20);
        nameField.setText(rule.name);
        allTextFields.add(nameField);
        currentY += 25;

        btnEnabled = new ThemedButton(1, panelX, currentY, panelWidth, 20,
                I18n.format("gui.autopickup.btn.enabled", stateOnOff(rule.enabled)));
        this.buttonList.add(btnEnabled);
        currentY += 25;

        String seqButtonText = (rule.postPickupSequence == null || rule.postPickupSequence.isEmpty())
                ? I18n.format("gui.autopickup.btn.select_seq")
                : "§f" + rule.postPickupSequence;
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
        radiusField = new GuiTextField(6, fontRenderer, panelX + fieldWidth + 10, currentY, fieldWidth, 20);
        allTextFields.add(zField);
        allTextFields.add(radiusField);
        currentY += 25;

        btnGetCoords = new ThemedButton(7, panelX, currentY, panelWidth, 20,
                I18n.format("gui.autopickup.btn.get_coords"));
        this.buttonList.add(btnGetCoords);
        currentY += 30;

        delayField = new GuiTextField(8, fontRenderer, panelX, currentY, panelWidth, 20);
        allTextFields.add(delayField);
        currentY += 25;

        btnStopOnExit = new ThemedButton(9, panelX, currentY, panelWidth, 20,
                I18n.format("gui.autopickup.btn.stop_on_exit", yesNo(rule.stopOnExit)));
        this.buttonList.add(btnStopOnExit);
        currentY += 25;

        this.buttonList.add(
                new ThemedButton(10, panelX, currentY, (panelWidth - 10) / 2, 20,
                        "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(11, panelX + (panelWidth - 10) / 2 + 10, currentY, (panelWidth - 10) / 2,
                20,
                I18n.format("gui.common.cancel")));

        xField.setText(COORD_FORMAT.format(rule.centerX));
        yField.setText(COORD_FORMAT.format(rule.centerY));
        zField.setText(COORD_FORMAT.format(rule.centerZ));
        radiusField.setText(COORD_FORMAT.format(rule.radius));
        delayField.setText(String.valueOf(rule.postPickupDelaySeconds));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                rule.enabled = !rule.enabled;
                btnEnabled.displayString = I18n.format("gui.autopickup.btn.enabled", stateOnOff(rule.enabled));
                break;
            case 2:
                mc.setScreen(new GuiSequenceSelector(this, seq -> {
                    rule.postPickupSequence = seq;
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
            case 9:
                rule.stopOnExit = !rule.stopOnExit;
                btnStopOnExit.displayString = I18n.format("gui.autopickup.btn.stop_on_exit", yesNo(rule.stopOnExit));
                break;
            case 10: // 保存
                try {
                    rule.name = nameField.getText();
                    rule.centerX = Double.parseDouble(xField.getText().replace(',', '.'));
                    rule.centerY = Double.parseDouble(yField.getText().replace(',', '.'));
                    rule.centerZ = Double.parseDouble(zField.getText().replace(',', '.'));
                    rule.radius = Double.parseDouble(radiusField.getText().replace(',', '.'));
                    rule.postPickupDelaySeconds = Integer.parseInt(delayField.getText());
                    onSave.accept(rule);
                } catch (NumberFormatException e) {
                    // 可以在这里加一个错误提示
                }
                mc.setScreen(parentScreen);
                break;
            case 11: // 取消
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 260;
        int panelHeight = 318;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 160;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.autopickup.edit.title"), this.fontRenderer);

        drawString(fontRenderer, I18n.format("gui.autopickup.rule_name"), nameField.x, nameField.y - 12,
                GuiTheme.SUB_TEXT);
        drawString(fontRenderer, I18n.format("gui.autopickup.center_xy"), xField.x, xField.y - 12, GuiTheme.SUB_TEXT);

        // --- 核心修复：使用 zField 的坐标来绘制 Z/半径 标签 ---
        drawString(fontRenderer, I18n.format("gui.autopickup.center_zr"), zField.x, zField.y - 12, GuiTheme.SUB_TEXT);
        // --- 修复结束 ---

        drawString(fontRenderer, I18n.format("gui.autopickup.delay"), delayField.x, delayField.y - 12,
                GuiTheme.SUB_TEXT);

        for (GuiTextField field : allTextFields) {
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







