package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.mojang.blaze3d.platform.GlStateManager; // 导入 GlStateManager
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;

import java.io.IOException;

import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.config.LoopExecutionConfig;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;
import com.zszl.zszlScriptMod.utils.CapturingFontRenderer;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiLoopCountInput extends ThemedGuiScreen {
    private final GuiScreen parent;
    private String inputText = "";
    private GuiTextField numberField;
    private String statusMessage = ""; // 添加状态消息
    private int messageDisplayTicks = 0; // 消息显示计时器

    public GuiLoopCountInput(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 主面板位置和尺寸
        int panelWidth = 220;
        int panelHeight = 150; // 减少高度
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        // 创建输入框
        numberField = new GuiTextField(0, fontRenderer,
                panelX + 10, panelY + 60, panelWidth - 20, 20);
        numberField.setFocused(true);
        numberField.setCanLoseFocus(false);
        numberField.setMaxStringLength(Integer.MAX_VALUE);
        numberField.setText(inputText);

        // 确认按钮
        this.buttonList.add(new GuiButton(0, panelX + 10, panelY + 90, (panelWidth - 30) / 2, 20,
                I18n.format("gui.loop.confirm")));
        // 取消按钮
        this.buttonList.add(
                new GuiButton(1, panelX + 20 + (panelWidth - 30) / 2, panelY + 90, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.common.cancel")));
        // 无限循环按钮
        this.buttonList.add(new GuiButton(2, panelX + 10, panelY + 120, panelWidth - 20, 20,
                I18n.format("gui.loop.set_infinite")));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (numberField.textboxKeyTyped(typedChar, keyCode)) {
            inputText = numberField.getText();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        numberField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font) instanceof CapturingFontRenderer) {
            ((CapturingFontRenderer) new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font)).disableCapture();
        }

        try {
            this.drawDefaultBackground();

            int centerX = this.width / 2;
            int centerY = this.height / 2;

            int panelWidth = 220;
            int panelHeight = 150; // 减少高度
            int panelX = centerX - panelWidth / 2;
            int panelY = centerY - panelHeight / 2;

            GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);

            drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY, 0xFF888888);
            drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF888888);
            drawVerticalLine(panelX, panelY, panelY + panelHeight - 1, 0xFF888888);
            drawVerticalLine(panelX + panelWidth - 1, panelY, panelY + panelHeight - 1, 0xFF888888);

            drawCenteredString(fontRenderer, I18n.format("gui.loop.title"), centerX, panelY + 15, 0xFFFFFFFF);
            drawString(fontRenderer, I18n.format("gui.loop.input_tip"),
                    panelX + 10, panelY + 45, 0xFFFFFFFF);

            drawThemedTextField(numberField);

            if (messageDisplayTicks > 0 && !statusMessage.isEmpty()) {
                drawCenteredString(fontRenderer, statusMessage, centerX, panelY + panelHeight - 45, 0xFFFFFFFF);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        } finally {
            // !! 关键：绘制完成后，无论是否发生异常，都要重新启用文本捕获 !!
            if (new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font) instanceof CapturingFontRenderer) {
                ((CapturingFontRenderer) new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font)).enableCapture();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // 确认按钮
            setLoopCount();
            mc.setScreen(parent);
        } else if (button.id == 1) { // 取消按钮
            mc.setScreen(parent);
        } else if (button.id == 2) { // 无限循环按钮
            GuiInventory.loopCount = -1;
            LoopExecutionConfig.INSTANCE.loopCount = GuiInventory.loopCount;
            LoopExecutionConfig.save();
            statusMessage = I18n.format("gui.loop.status_infinite");
            messageDisplayTicks = 60;
            mc.setScreen(parent);
        }
    }

    private void setLoopCount() {
        try {
            GuiInventory.loopCount = Integer.parseInt(inputText.trim());
            LoopExecutionConfig.INSTANCE.loopCount = GuiInventory.loopCount;
            LoopExecutionConfig.save();
            GuiInventory.loopCounter = 0;
            statusMessage = I18n.format("gui.loop.status_set", GuiInventory.loopCount);
            messageDisplayTicks = 60;
        } catch (NumberFormatException e) {
            GuiInventory.loopCount = 1;
            LoopExecutionConfig.INSTANCE.loopCount = GuiInventory.loopCount;
            LoopExecutionConfig.save();
            statusMessage = I18n.format("gui.loop.status_invalid");
            messageDisplayTicks = 60;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (messageDisplayTicks > 0) {
            messageDisplayTicks--;
            if (messageDisplayTicks == 0) {
                statusMessage = "";
            }
        }
    }
}






