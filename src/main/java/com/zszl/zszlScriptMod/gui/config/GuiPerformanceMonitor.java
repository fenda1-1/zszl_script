package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.system.PerformanceMonitor;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import net.minecraft.ChatFormatting;

import java.io.IOException;

/**
 * 性能监控配置界面
 * 卡片式布局显示各个功能模块的开关控制和性能分析
 */
public class GuiPerformanceMonitor extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private int panelX, panelY, panelWidth, panelHeight;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // 卡片布局参数
    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 120;
    private static final int CARD_MARGIN = 10;
    private static final int CARDS_PER_ROW = 2;

    public GuiPerformanceMonitor(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        this.panelWidth = Math.min(680, this.width - 40);
        this.panelHeight = Math.min(400, this.height - 30);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        // 返回按钮
        this.buttonList.add(new GuiButton(0, this.width / 2 - 50, this.panelY + this.panelHeight - 26, 100, 18,
                I18n.format("gui.common.back")));

        // 重置统计数据按钮
        this.buttonList.add(new GuiButton(1, this.panelX + 10, this.panelY + this.panelHeight - 26, 80, 18,
                "重置统计"));

        // 计算最大滚动距离
        int totalCards = PerformanceMonitor.PERFORMANCE_FEATURES.length;
        int rows = (totalCards + CARDS_PER_ROW - 1) / CARDS_PER_ROW;
        int totalHeight = rows * (CARD_HEIGHT + CARD_MARGIN) + CARD_MARGIN;
        maxScroll = Math.max(0, totalHeight - (this.panelHeight - 60));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // 返回
                if (parentScreen != null) {
                    mc.setScreen(parentScreen);
                } else {
                    mc.setScreen(null);
                }
                break;
            case 1: // 重置统计数据
                PerformanceMonitor.resetAllStats();
                break;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) { // 左键点击
            // 检查卡片区域的点击
            int contentY = this.panelY + 30 - scrollOffset;
            int startX = this.panelX + 15;

            for (int i = 0; i < PerformanceMonitor.PERFORMANCE_FEATURES.length; i++) {
                String feature = PerformanceMonitor.PERFORMANCE_FEATURES[i];
                int row = i / CARDS_PER_ROW;
                int col = i % CARDS_PER_ROW;

                int cardX = startX + col * (CARD_WIDTH + CARD_MARGIN);
                int cardY = contentY + row * (CARD_HEIGHT + CARD_MARGIN);

                if (cardX <= mouseX && mouseX <= cardX + CARD_WIDTH &&
                        cardY <= mouseY && mouseY <= cardY + CARD_HEIGHT) {

                    // 检查开关按钮点击
                    int buttonY = cardY + 25;
                    if (mouseX >= cardX + 10 && mouseX <= cardX + 60 && mouseY >= buttonY && mouseY <= buttonY + 15) {
                        // 功能开关
                        boolean current = PerformanceMonitor.isFeatureEnabled(feature);
                        PerformanceMonitor.setFeatureEnabled(feature, !current);
                    } else if (mouseX >= cardX + 70 && mouseX <= cardX + 120 && mouseY >= buttonY
                            && mouseY <= buttonY + 15) {
                        // 性能分析开关
                        boolean current = PerformanceMonitor.isPerformanceAnalysisEnabled(feature);
                        PerformanceMonitor.setPerformanceAnalysisEnabled(feature, !current);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 绘制背景
        drawGradientRect(0, 0, this.width, this.height, 0xA0000000, 0xD0000000);

        // 绘制主面板
        drawRect(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xC0181E28);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY, 0xFF5A6A80);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 1,
                0xFF5A6A80);
        drawVerticalLine(this.panelX, this.panelY, this.panelY + this.panelHeight - 1, 0xFF5A6A80);
        drawVerticalLine(this.panelX + this.panelWidth - 1, this.panelY, this.panelY + this.panelHeight - 1,
                0xFF5A6A80);

        // 绘制标题
        drawCenteredString(this.fontRenderer, "性能监控", this.panelX + this.panelWidth / 2, this.panelY + 8, 0xFFFFFFFF);

        // 绘制卡片
        drawFeatureCards(mouseX, mouseY);

        // 绘制滚动条
        if (maxScroll > 0) {
            int scrollbarX = this.panelX + this.panelWidth - 15;
            int scrollbarY = this.panelY + 30;
            int scrollbarHeight = this.panelHeight - 60;

            drawRect(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFF202732);

            int thumbHeight = Math.max(20,
                    (int) ((float) scrollbarHeight / (maxScroll + scrollbarHeight) * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX - 1, thumbY, scrollbarX + 7, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawFeatureCards(int mouseX, int mouseY) {
        int contentY = this.panelY + 30 - scrollOffset;
        int startX = this.panelX + 15;

        for (int i = 0; i < PerformanceMonitor.PERFORMANCE_FEATURES.length; i++) {
            String feature = PerformanceMonitor.PERFORMANCE_FEATURES[i];
            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;

            int cardX = startX + col * (CARD_WIDTH + CARD_MARGIN);
            int cardY = contentY + row * (CARD_HEIGHT + CARD_MARGIN);

            // 只绘制可见的卡片
            if (cardY + CARD_HEIGHT < this.panelY + 30 || cardY > this.panelY + this.panelHeight - 30) {
                continue;
            }

            drawFeatureCard(cardX, cardY, feature, mouseX, mouseY);
        }
    }

    private void drawFeatureCard(int x, int y, String feature, int mouseX, int mouseY) {
        // 卡片背景
        drawRect(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF2A2E37);

        // 卡片边框
        drawHorizontalLine(x, x + CARD_WIDTH - 1, y, 0xFF5A6A80);
        drawHorizontalLine(x, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, 0xFF5A6A80);
        drawVerticalLine(x, y, y + CARD_HEIGHT - 1, 0xFF5A6A80);
        drawVerticalLine(x + CARD_WIDTH - 1, y, y + CARD_HEIGHT - 1, 0xFF5A6A80);

        // 功能名称
        String displayName = PerformanceMonitor.getFeatureDisplayName(feature);
        drawString(this.fontRenderer, ChatFormatting.BOLD + displayName, x + 10, y + 8, 0xFFFFFFFF);

        // 开关按钮
        int buttonY = y + 25;
        drawFeatureToggleButton(x + 10, buttonY, "启用", PerformanceMonitor.isFeatureEnabled(feature), mouseX, mouseY);
        drawFeatureToggleButton(x + 70, buttonY, "分析", PerformanceMonitor.isPerformanceAnalysisEnabled(feature), mouseX,
                mouseY);

        // 性能统计
        PerformanceMonitor.PerformanceStats stats = PerformanceMonitor.performanceStats.get(feature);
        if (stats != null && PerformanceMonitor.isPerformanceAnalysisEnabled(feature)) {
            drawString(this.fontRenderer, "性能统计:", x + 10, y + 50, 0xFFCCCCCC);
            String statsText = stats.getFormattedStats();
            if (statsText.length() > 35) {
                statsText = statsText.substring(0, 32) + "...";
            }
            drawString(this.fontRenderer, statsText, x + 10, y + 62, 0xFFAAAAAA);
        } else {
            drawString(this.fontRenderer, "性能分析未启用", x + 10, y + 50, 0xFF666666);
        }

        // 功能描述
        String description = PerformanceMonitor.getFeatureDescription(feature);
        if (description.length() > 40) {
            description = description.substring(0, 37) + "...";
        }
        drawString(this.fontRenderer, description, x + 10, y + 85, 0xFF888888);
    }

    private void drawFeatureToggleButton(int x, int y, String label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + 50 && mouseY >= y && mouseY <= y + 15;

        // 按钮背景
        int bgColor = enabled ? 0xFF4CAF50 : 0xFF666666;
        if (hovered) {
            bgColor = enabled ? 0xFF66BB6A : 0xFF888888;
        }
        drawRect(x, y, x + 50, y + 15, bgColor);

        // 按钮边框
        drawHorizontalLine(x, x + 49, y, 0xFFFFFFFF);
        drawHorizontalLine(x, x + 49, y + 14, 0xFFFFFFFF);
        drawVerticalLine(x, y, y + 14, 0xFFFFFFFF);
        drawVerticalLine(x + 49, y, y + 14, 0xFFFFFFFF);

        // 按钮文本
        String text = enabled ? "开" : "关";
        int textColor = enabled ? 0xFFFFFFFF : 0xFFCCCCCC;
        drawCenteredString(this.fontRenderer, text, x + 25, y + 3, textColor);

        // 标签
        drawString(this.fontRenderer, label, x - 35, y + 3, 0xFFCCCCCC);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        // 处理滚动
        if (clickedMouseButton == 0 && maxScroll > 0) {
            int scrollbarX = this.panelX + this.panelWidth - 15;
            int scrollbarY = this.panelY + 30;
            int scrollbarHeight = this.panelHeight - 60;

            if (mouseX >= scrollbarX - 5 && mouseX <= scrollbarX + 11 &&
                    mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                // 点击滚动条
                float ratio = (float) (mouseY - scrollbarY) / scrollbarHeight;
                scrollOffset = (int) (ratio * maxScroll);
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            }
        }
    }
}






