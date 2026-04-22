// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/config/GuiScanViewer.java
// (这是增加了“复制”按钮和“可视化滚动条”的最终版本)
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;

public class GuiScanViewer extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final String fileName;
    private final List<String> contentLines;
    private final String fullContent; // !! 新增：存储完整的、未分割的文本内容 !!

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public GuiScanViewer(GuiScreen parent, String fileName, String content) {
        this.parentScreen = parent;
        this.fileName = fileName;
        this.fullContent = content; // !! 新增：保存完整内容 !!
        this.contentLines = Arrays.asList(content.split("\n"));
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int buttonWidth = 120;
        int spacing = 10;
        int totalButtonWidth = buttonWidth * 2 + spacing;
        int startX = (this.width - totalButtonWidth) / 2;

        // !! 核心修改：新增“复制”按钮并重新布局 !!
        this.buttonList
                .add(new ThemedButton(0, startX, this.height - 30, buttonWidth, 20, I18n.format("gui.common.cancel")));
        this.buttonList.add(new ThemedButton(1, startX + buttonWidth + spacing, this.height - 30, buttonWidth, 20,
                I18n.format("gui.scan_viewer.copy")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.setScreen(parentScreen);
        }
        // !! 核心修改：处理“复制”按钮的点击事件 !!
        else if (button.id == 1) {
            setClipboardString(this.fullContent); // 使用存储的完整内容
            if (this.mc.player != null) {
                this.mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.scan_viewer.copy_success")));
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = this.width - 40;
        int panelHeight = this.height - 60;
        int panelX = 20;
        int panelY = 20;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.scan_viewer.title", fileName),
                this.fontRenderer);

        int listX = panelX + 5;
        int listY = panelY + 20;
        int listWidth = panelWidth - 15; // 为滚动条留出空间
        int listHeight = panelHeight - 25;
        int lineHeight = this.fontRenderer.FONT_HEIGHT + 1;
        int visibleLines = listHeight / lineHeight;
        maxScroll = Math.max(0, contentLines.size() - visibleLines);

        // 绘制文本行
        for (int i = 0; i < visibleLines; i++) {
            int index = i + scrollOffset;
            if (index >= contentLines.size())
                break;
            // 使用 listWidth 来限制文本绘制区域
            this.fontRenderer.drawSplitString(contentLines.get(index), listX, listY + i * lineHeight, listWidth,
                    0xFFFFFF);
        }

        // !! 核心修改：绘制可视化滚动条 !!
        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 8;
            int scrollbarY = panelY + 20;
            int scrollbarHeight = listHeight;

            // 绘制滚动条轨道
            // 计算并绘制滑块
            int thumbHeight = Math.max(10, (int) ((float) visibleLines / contentLines.size() * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 6, scrollbarHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}







