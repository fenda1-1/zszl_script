package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Arrays;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;

public class GuiAutoEatConfig extends ThemedGuiScreen {
    private final GuiScreen parentScreen;

    private ToggleGuiButton autoMoveButton;
    private ToggleGuiButton lookDownButton;
    private ToggleGuiButton smoothLookDownButton;
    private ToggleGuiButton onlyDuringPathingButton;
    private GuiTextField thresholdField;
    private GuiTextField hotbarSlotField;
    private GuiTextField foodKeywordsField;

    public GuiAutoEatConfig(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int panelWidth = 340;
        int panelHeight = 310;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int currentY = panelY + 25;

        // 1) 食物阈值输入框
        thresholdField = new GuiTextField(2, this.fontRenderer, panelX + 132, currentY, 42, 20);
        thresholdField.setMaxStringLength(Integer.MAX_VALUE);
        thresholdField.setText(String.valueOf(AutoEatHandler.foodLevelThreshold));
        currentY += 30;

        // 2) 自动移动食物到快捷栏开关
        autoMoveButton = new ToggleGuiButton(1, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.autoeat.toggle_move", stateText(AutoEatHandler.autoMoveFoodEnabled)),
                AutoEatHandler.autoMoveFoodEnabled);
        this.buttonList.add(autoMoveButton);
        currentY += 30;

        lookDownButton = new ToggleGuiButton(5, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.autoeat.toggle_lookdown", stateText(AutoEatHandler.eatWithLookDown)),
                AutoEatHandler.eatWithLookDown);
        this.buttonList.add(lookDownButton);
        currentY += 30;

        smoothLookDownButton = new ToggleGuiButton(6, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.autoeat.toggle_smooth_lookdown", stateText(AutoEatHandler.smoothLookDown)),
                AutoEatHandler.smoothLookDown);
        this.buttonList.add(smoothLookDownButton);
        currentY += 30;

        onlyDuringPathingButton = new ToggleGuiButton(7, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.autoeat.toggle_only_during_pathing",
                        stateText(AutoEatHandler.onlyDuringSequenceStepPathing)),
                AutoEatHandler.onlyDuringSequenceStepPathing);
        this.buttonList.add(onlyDuringPathingButton);
        currentY += 30;

        // 3) 目标快捷栏数字框
        hotbarSlotField = new GuiTextField(3, this.fontRenderer, panelX + 140, currentY, 42, 20);
        hotbarSlotField.setMaxStringLength(Integer.MAX_VALUE);
        hotbarSlotField.setText(String.valueOf(AutoEatHandler.targetHotbarSlot));
        currentY += 30;

        // 4) 食物关键词输入框
        foodKeywordsField = new GuiTextField(4, this.fontRenderer, panelX + 10, currentY + 12, panelWidth - 20, 20);
        foodKeywordsField.setMaxStringLength(Integer.MAX_VALUE);
        foodKeywordsField.setText(String.join(", ", AutoEatHandler.foodKeywords));
        currentY += 45;

        this.buttonList.add(new ThemedButton(100, panelX + 10, currentY, (panelWidth - 30) / 2, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList.add(
                new ThemedButton(101, panelX + 20 + (panelWidth - 30) / 2, currentY, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            AutoEatHandler.autoMoveFoodEnabled = !AutoEatHandler.autoMoveFoodEnabled;
            autoMoveButton.displayString = I18n.format("gui.autoeat.toggle_move",
                    stateText(AutoEatHandler.autoMoveFoodEnabled));
            autoMoveButton.setEnabledState(AutoEatHandler.autoMoveFoodEnabled);
        } else if (button.id == 5) {
            AutoEatHandler.eatWithLookDown = !AutoEatHandler.eatWithLookDown;
            lookDownButton.displayString = I18n.format("gui.autoeat.toggle_lookdown",
                    stateText(AutoEatHandler.eatWithLookDown));
            lookDownButton.setEnabledState(AutoEatHandler.eatWithLookDown);
        } else if (button.id == 6) {
            AutoEatHandler.smoothLookDown = !AutoEatHandler.smoothLookDown;
            smoothLookDownButton.displayString = I18n.format("gui.autoeat.toggle_smooth_lookdown",
                    stateText(AutoEatHandler.smoothLookDown));
            smoothLookDownButton.setEnabledState(AutoEatHandler.smoothLookDown);
        } else if (button.id == 7) {
            AutoEatHandler.onlyDuringSequenceStepPathing = !AutoEatHandler.onlyDuringSequenceStepPathing;
            onlyDuringPathingButton.displayString = I18n.format("gui.autoeat.toggle_only_during_pathing",
                    stateText(AutoEatHandler.onlyDuringSequenceStepPathing));
            onlyDuringPathingButton.setEnabledState(AutoEatHandler.onlyDuringSequenceStepPathing);
        } else if (button.id == 100) {
            try {
                int threshold = Integer.parseInt(thresholdField.getText());
                AutoEatHandler.foodLevelThreshold = Math.max(0, Math.min(20, threshold));
            } catch (NumberFormatException e) {
                AutoEatHandler.foodLevelThreshold = 12;
            }
            thresholdField.setText(String.valueOf(AutoEatHandler.foodLevelThreshold));

            try {
                int slot = Integer.parseInt(hotbarSlotField.getText());
                AutoEatHandler.targetHotbarSlot = (slot >= 1 && slot <= 9) ? slot : 9;
            } catch (NumberFormatException e) {
                AutoEatHandler.targetHotbarSlot = 9;
            }
            hotbarSlotField.setText(String.valueOf(AutoEatHandler.targetHotbarSlot));

            AutoEatHandler.foodKeywords.clear();
            String[] names = foodKeywordsField.getText().replace("，", ",").split(",");
            for (String name : names) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    AutoEatHandler.foodKeywords.add(trimmed);
                }
            }
            if (AutoEatHandler.foodKeywords.isEmpty()) {
                AutoEatHandler.foodKeywords.addAll(AutoEatHandler.DEFAULT_FOOD_KEYWORDS);
            }

            AutoEatHandler.saveAutoEatConfig();
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == 101) {
            AutoEatHandler.loadAutoEatConfig();
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 340;
        int panelHeight = 310;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.autoeat.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.autoeat.threshold"), panelX + 10, thresholdField.y + 6,
                GuiTheme.LABEL_TEXT);
        this.drawString(this.fontRenderer, I18n.format("gui.autoeat.hotbar_slot"), panelX + 10, hotbarSlotField.y + 6,
                GuiTheme.LABEL_TEXT);
        this.drawString(this.fontRenderer, I18n.format("gui.autoeat.keywords"), panelX + 10, foodKeywordsField.y - 12,
                GuiTheme.LABEL_TEXT);

        drawThemedTextField(thresholdField);
        drawThemedTextField(hotbarSlotField);
        drawThemedTextField(foodKeywordsField);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (thresholdField.isFocused()) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.autoeat.tip.validate.title"),
                    I18n.format("gui.autoeat.tip.threshold.range"),
                    I18n.format("gui.autoeat.tip.validate.common")), mouseX, mouseY);
        } else if (hotbarSlotField.isFocused()) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.autoeat.tip.validate.title"),
                    I18n.format("gui.autoeat.tip.hotbar.range"),
                    I18n.format("gui.autoeat.tip.validate.common")), mouseX, mouseY);
        }

        if (mouseX >= foodKeywordsField.x && mouseX < foodKeywordsField.x + foodKeywordsField.width
                && mouseY >= foodKeywordsField.y && mouseY < foodKeywordsField.y + foodKeywordsField.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.autoeat.tip.keyword.title"),
                    I18n.format("gui.autoeat.tip.keyword.line1"),
                    I18n.format("gui.autoeat.tip.keyword.line2"),
                    I18n.format("gui.autoeat.tip.keyword.line3")), mouseX, mouseY);
        } else if (lookDownButton != null && mouseX >= lookDownButton.x
                && mouseX <= lookDownButton.x + lookDownButton.width
                && mouseY >= lookDownButton.y && mouseY <= lookDownButton.y + lookDownButton.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.autoeat.lookdown.tip.title"),
                    I18n.format("gui.autoeat.lookdown.tip.line1"),
                    I18n.format("gui.autoeat.lookdown.tip.line2")), mouseX, mouseY);
        } else if (smoothLookDownButton != null && mouseX >= smoothLookDownButton.x
                && mouseX <= smoothLookDownButton.x + smoothLookDownButton.width
                && mouseY >= smoothLookDownButton.y && mouseY <= smoothLookDownButton.y + smoothLookDownButton.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.autoeat.smooth.tip.title"),
                    I18n.format("gui.autoeat.smooth.tip.line1"),
                    I18n.format("gui.autoeat.smooth.tip.line2")), mouseX, mouseY);
        } else if (onlyDuringPathingButton != null && mouseX >= onlyDuringPathingButton.x
                && mouseX <= onlyDuringPathingButton.x + onlyDuringPathingButton.width
                && mouseY >= onlyDuringPathingButton.y && mouseY <= onlyDuringPathingButton.y + onlyDuringPathingButton.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.autoeat.pathing.tip.title"),
                    I18n.format("gui.autoeat.pathing.tip.line1"),
                    I18n.format("gui.autoeat.pathing.tip.line2"),
                    I18n.format("gui.autoeat.pathing.tip.line3")), mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (thresholdField.textboxKeyTyped(typedChar, keyCode)) {
            // handled
        } else if (hotbarSlotField.textboxKeyTyped(typedChar, keyCode)) {
            // handled
        } else if (foodKeywordsField.textboxKeyTyped(typedChar, keyCode)) {
            // handled
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        thresholdField.mouseClicked(mouseX, mouseY, mouseButton);
        hotbarSlotField.mouseClicked(mouseX, mouseY, mouseButton);
        foodKeywordsField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String stateText(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }
}
