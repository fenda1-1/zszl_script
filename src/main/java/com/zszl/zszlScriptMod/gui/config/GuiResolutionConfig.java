package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.mojang.blaze3d.opengl.GlStateManager; // 导入 GlStateManager

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.utils.CapturingFontRenderer;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiResolutionConfig extends ThemedGuiScreen {
    private GuiTextField widthField;
    private GuiTextField heightField;
    private GuiTextField guiscaleField;
    private String statusMessage = "";
    private int messageDisplayTicks = 0;

    @Override
    public void initGui() {
        this.buttonList.clear();
        int centerX = width / 2;
        int centerY = height / 2;

        int panelWidth = 240;
        int panelHeight = 200; // 调整高度
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        // 宽度输入框
        widthField = new GuiTextField(1, this.fontRenderer,
                panelX + 10, panelY + 60, panelWidth - 20, 20);
        widthField.setMaxStringLength(Integer.MAX_VALUE);
        widthField.setEnabled(false); // !! 修改: 设置为不可编辑 !!

        // 高度输入框
        heightField = new GuiTextField(2, this.fontRenderer,
                panelX + 10, panelY + 100, panelWidth - 20, 20);
        heightField.setMaxStringLength(Integer.MAX_VALUE);
        heightField.setEnabled(false); // !! 修改: 设置为不可编辑 !!

        // 界面尺寸输入框
        guiscaleField = new GuiTextField(4, this.fontRenderer,
                panelX + 10, panelY + 140, panelWidth - 20, 20);
        guiscaleField.setMaxStringLength(Integer.MAX_VALUE);
        guiscaleField.setEnabled(false); // !! 修改: 设置为不可编辑 !!

        // !! 修改: 将按钮ID 1 改为 0, 按钮ID 0 (保存) 被移除 !!
        this.buttonList.add(new GuiButton(0, panelX + 10, panelY + 170, (panelWidth - 30) / 2, 20,
                I18n.format("gui.resolution.refresh")));
        this.buttonList.add(new GuiButton(1, panelX + 20 + (panelWidth - 30) / 2, panelY + 170, (panelWidth - 30) / 2,
                20, I18n.format("gui.common.done")));

        // !! 新增: 打开界面时立即获取一次当前配置 !!
        updateFieldsWithCurrentConfig();
    }

    private void updateFieldsWithCurrentConfig() {
        int currentWidth = mc.getWindow().getWidth();
        int currentHeight = mc.getWindow().getHeight();
        com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution scaledResolution = new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution(mc);
        int currentGuiScale = scaledResolution.getScaleFactor();

        widthField.setText(String.valueOf(currentWidth));
        heightField.setText(String.valueOf(currentHeight));
        guiscaleField.setText(String.valueOf(currentGuiScale));

        statusMessage = I18n.format("gui.resolution.refresh_ok");
        messageDisplayTicks = 60;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        // !! 修改: 重新定义按钮逻辑 !!
        if (button.id == 0) { // 刷新按钮
            updateFieldsWithCurrentConfig();
        } else if (button.id == 1) { // 完成按钮
            mc.setScreen(null); // 关闭GUI
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font) instanceof CapturingFontRenderer) {
            ((CapturingFontRenderer) new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer(mc.font)).disableCapture();
        }

        try {
            drawDefaultBackground();

            int centerX = width / 2;
            int centerY = height / 2;

            int panelWidth = 240;
            int panelHeight = 200;
            int panelX = centerX - panelWidth / 2;
            int panelY = centerY - panelHeight / 2;

            GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);

            drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY, 0xFF888888);
            drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF888888);
            drawVerticalLine(panelX, panelY, panelY + panelHeight - 1, 0xFF888888);
            drawVerticalLine(panelX + panelWidth - 1, panelY, panelY + panelHeight - 1, 0xFF888888);

            drawCenteredString(fontRenderer, I18n.format("gui.resolution.title"), centerX, panelY + 15, 0xFFFFFFFF);
            drawString(fontRenderer, I18n.format("gui.resolution.width"), panelX + 10, panelY + 45, 0xFFFFFFFF);
            drawString(fontRenderer, I18n.format("gui.resolution.height"), panelX + 10, panelY + 85, 0xFFFFFFFF);
            drawString(fontRenderer, I18n.format("gui.resolution.scale"), panelX + 10, panelY + 125, 0xFFFFFFFF);

            drawThemedTextField(widthField);
            drawThemedTextField(heightField);
            drawThemedTextField(guiscaleField);

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
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // 允许按ESC关闭
        if (keyCode == 1) {
            this.mc.setScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        // 文本框不可编辑，所以不需要它们的mouseClicked
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








