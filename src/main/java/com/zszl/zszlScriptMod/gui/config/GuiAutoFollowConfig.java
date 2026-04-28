// !! REFACTORED FILE (Editor Mode) !!
package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import net.minecraft.world.entity.Entity;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.system.AutoFollowRule;
import com.zszl.zszlScriptMod.utils.CapturingFontRenderer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiAutoFollowConfig extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private final AutoFollowRule rule;
    private final Consumer<AutoFollowRule> onSave;

    private GuiTextField nameField, point1XField, point1ZField, point2XField, point2ZField, point3XField, point3ZField,
            maxRecoveryDistanceField;
    private GuiButton visualizeRangeButton;
    private List<GuiTextField> allTextFields = new ArrayList<>();
    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    public GuiAutoFollowConfig(GuiScreen parent, AutoFollowRule rule, Consumer<AutoFollowRule> onSaveCallback) {
        this.parentScreen = parent;
        this.rule = rule;
        this.onSave = onSaveCallback;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allTextFields.clear();

        int centerX = this.width / 2;
        int panelWidth = 300;
        int panelY = this.height / 2 - 140;

        int currentY = panelY;

        // 规则名称
        nameField = new GuiTextField(10, this.fontRenderer, centerX - 140, currentY, 280, 20);
        nameField.setText(rule.name);
        allTextFields.add(nameField);
        currentY += 35;

        int fieldX = centerX - 45;
        int inputWidth = 80;

        // 点1
        point1XField = new GuiTextField(0, this.fontRenderer, fieldX, currentY, inputWidth, 20);
        point1ZField = new GuiTextField(1, this.fontRenderer, fieldX + inputWidth + 10, currentY, inputWidth, 20);
        allTextFields.add(point1XField);
        allTextFields.add(point1ZField);
        currentY += 40;

        // 点2
        point2XField = new GuiTextField(2, this.fontRenderer, fieldX, currentY, inputWidth, 20);
        point2ZField = new GuiTextField(3, this.fontRenderer, fieldX + inputWidth + 10, currentY, inputWidth, 20);
        allTextFields.add(point2XField);
        allTextFields.add(point2ZField);
        currentY += 40;

        // 回归点
        point3XField = new GuiTextField(4, this.fontRenderer, fieldX, currentY, inputWidth, 20);
        point3ZField = new GuiTextField(5, this.fontRenderer, fieldX + inputWidth + 10, currentY, inputWidth, 20);
        allTextFields.add(point3XField);
        allTextFields.add(point3ZField);
        currentY += 40;

        // 恢复距离
        maxRecoveryDistanceField = new GuiTextField(6, this.fontRenderer, fieldX, currentY, inputWidth, 20);
        allTextFields.add(maxRecoveryDistanceField);

        setupCoordinateField(point1XField, rule.point1.x);
        setupCoordinateField(point1ZField, rule.point1.z);
        setupCoordinateField(point2XField, rule.point2.x);
        setupCoordinateField(point2ZField, rule.point2.z);
        setupCoordinateField(point3XField, rule.point3.x);
        setupCoordinateField(point3ZField, rule.point3.z);
        setupCoordinateField(maxRecoveryDistanceField, rule.maxRecoveryDistance);

        currentY += 30;
        int smallButtonWidth = (panelWidth - 40) / 3;
        this.buttonList.add(new ThemedButton(10, centerX - 140, currentY, smallButtonWidth, 20,
                I18n.format("gui.autofollow.btn.get_p1")));
        this.buttonList.add(new ThemedButton(11, centerX - 140 + smallButtonWidth + 10, currentY, smallButtonWidth, 20,
                I18n.format("gui.autofollow.btn.get_p2")));
        this.buttonList
                .add(new ThemedButton(12, centerX - 140 + 2 * (smallButtonWidth + 10), currentY, smallButtonWidth, 20,
                        I18n.format("gui.autofollow.btn.get_return")));
        currentY += 25;

        visualizeRangeButton = new ThemedButton(16, centerX - 140, currentY, 280, 20, getVisualizeRangeText());
        this.buttonList.add(visualizeRangeButton);
        currentY += 25;

        this.buttonList
                .add(new ThemedButton(14, centerX - 140, currentY, 135, 20, "§a" + I18n.format("gui.common.save")));
        this.buttonList
                .add(new ThemedButton(15, centerX + 5, currentY, 135, 20, "§c" + I18n.format("gui.common.cancel")));
    }

    private void setupCoordinateField(GuiTextField field, double value) {
        field.setText(COORD_FORMAT.format(value));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = this.width / 2;
        int panelX = centerX - 150;
        int panelY = this.height / 2 - 140;
        int panelW = 300;
        int panelH = 285;

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.autofollow.edit.title"), this.fontRenderer);

        drawString(fontRenderer, I18n.format("gui.autofollow.name"), nameField.x, nameField.y - 12, GuiTheme.SUB_TEXT);
        drawString(fontRenderer, I18n.format("gui.autofollow.point1"), point1XField.x - 90, point1XField.y + 5,
                GuiTheme.LABEL_TEXT);
        drawString(fontRenderer, I18n.format("gui.autofollow.point2"), point2XField.x - 90, point2XField.y + 5,
                GuiTheme.LABEL_TEXT);
        drawString(fontRenderer, I18n.format("gui.autofollow.return_point"), point3XField.x - 90, point3XField.y + 5,
                GuiTheme.LABEL_TEXT);
        drawString(fontRenderer, I18n.format("gui.autofollow.max_recovery"), maxRecoveryDistanceField.x - 90,
                maxRecoveryDistanceField.y + 5, GuiTheme.LABEL_TEXT);

        for (GuiTextField field : allTextFields) {
            drawThemedTextField(field);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 10:
                updatePointFromPlayer(point1XField, point1ZField);
                break;
            case 11:
                updatePointFromPlayer(point2XField, point2ZField);
                break;
            case 12:
                updatePointFromPlayer(point3XField, point3ZField);
                break;
            case 16:
                rule.visualizeRange = !rule.visualizeRange;
                visualizeRangeButton.displayString = getVisualizeRangeText();
                break;
            case 14: // 保存
                try {
                    rule.name = nameField.getText();
                    rule.point1.x = Double.parseDouble(point1XField.getText().replace(',', '.'));
                    rule.point1.z = Double.parseDouble(point1ZField.getText().replace(',', '.'));
                    rule.point2.x = Double.parseDouble(point2XField.getText().replace(',', '.'));
                    rule.point2.z = Double.parseDouble(point2ZField.getText().replace(',', '.'));
                    rule.point3.x = Double.parseDouble(point3XField.getText().replace(',', '.'));
                    rule.point3.z = Double.parseDouble(point3ZField.getText().replace(',', '.'));
                    rule.maxRecoveryDistance = Double.parseDouble(maxRecoveryDistanceField.getText().replace(',', '.'));
                    rule.updateBounds();
                    onSave.accept(rule);
                } catch (NumberFormatException e) {
                    // 可以添加一个状态消息来提示用户
                }
                break;
            case 15: // 取消
                mc.setScreen(parentScreen);
                break;
        }
    }

    private void updatePointFromPlayer(GuiTextField xField, GuiTextField zField) {
        Entity viewEntity = mc.getCameraEntity();
        if (viewEntity == null) {
            viewEntity = mc.player;
        }
        if (viewEntity != null) {
            xField.setText(COORD_FORMAT.format(viewEntity.getX()));
            zField.setText(COORD_FORMAT.format(viewEntity.getZ()));
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
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

    private String getVisualizeRangeText() {
        return I18n.format("gui.autofollow.visualize",
                I18n.format(rule.visualizeRange ? "gui.autofollow.reenable.on" : "gui.autofollow.reenable.off"));
    }
}





