package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiPacketMain extends ThemedGuiScreen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;
    private static final int SECTION_GAP = 8;
    private static final int SECTION_PADDING = 8;
    private static final int SECTION_HEADER_HEIGHT = 12;
    private static final int SUMMARY_HEIGHT = 44;
    private static final int TITLE_HEIGHT = 20;
    private static final int FOOTER_HEIGHT = 30;

    private final GuiScreen parentScreen;

    private ToggleGuiButton captureToggleButton;
    private GuiButton viewButton;
    private GuiButton modeButton;

    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;
    private int summaryX;
    private int summaryY;
    private int summaryWidth;
    private int summaryHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int footerY;
    private int scrollOffset;
    private int maxScroll;

    private String captureSummary = "";
    private String businessSummary = "";
    private String queueSummary = "";
    private String footerSummary = "";

    private final List<SectionRenderInfo> visibleSections = new ArrayList<>();

    private static final class SectionItem {
        final int buttonId;
        final int span;

        private SectionItem(int buttonId, int span) {
            this.buttonId = buttonId;
            this.span = Math.max(1, Math.min(2, span));
        }
    }

    private static final class SectionDefinition {
        final String title;
        final List<SectionItem> items;

        private SectionDefinition(String title, SectionItem... items) {
            this.title = title == null ? "" : title;
            this.items = items == null ? new ArrayList<SectionItem>() : Arrays.asList(items);
        }
    }

    private static final class SectionRenderInfo {
        final String title;
        final Rectangle bounds;

        private SectionRenderInfo(String title, Rectangle bounds) {
            this.title = title == null ? "" : title;
            this.bounds = bounds;
        }
    }

    public GuiPacketMain(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.visibleSections.clear();
        recalcLayout();

        PacketCaptureHandler.PacketCaptureUiSnapshot snapshot = PacketCaptureHandler.getUiSnapshot();
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;

        String captureState = PacketCaptureHandler.isCapturing
                ? "§a" + I18n.format("gui.packet.main.running")
                : "§c" + I18n.format("gui.packet.main.stopped");
        captureToggleButton = new ToggleGuiButton(0, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.capture", captureState), PacketCaptureHandler.isCapturing);
        this.buttonList.add(captureToggleButton);

        String modeState = config.captureMode == PacketCaptureHandler.CaptureMode.WHITELIST
                ? "§a" + I18n.format("gui.packet.main.whitelist")
                : "§e" + I18n.format("gui.packet.main.blacklist");
        modeButton = new ThemedButton(7, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.mode", modeState));
        this.buttonList.add(modeButton);

        String businessState = snapshot.businessPacketProcessingEnabled ? "§aON" : "§cOFF";
        this.buttonList.add(new ThemedButton(10, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                "业务链 " + businessState));
        this.buttonList.add(new ThemedButton(5, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.filter_settings")));

        String viewButtonText = I18n.format("gui.packet.main.view_packets", snapshot.sentCount, snapshot.receivedCount);
        viewButton = new ThemedButton(1, contentX, contentY, contentWidth, BUTTON_HEIGHT, viewButtonText);
        this.buttonList.add(viewButton);
        this.buttonList.add(new ThemedButton(2, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                "§e" + I18n.format("gui.packet.main.clear_list")));

        this.buttonList.add(new ThemedButton(8, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.view_captured_id")));
        this.buttonList.add(new ThemedButton(11, contentX, contentY, contentWidth, BUTTON_HEIGHT, "字段提取规则"));
        this.buttonList.add(new ThemedButton(9, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.intercept_rules",
                        PacketInterceptConfig.INSTANCE.inboundInterceptEnabled ? "§aON" : "§cOFF")));

        this.buttonList.add(new ThemedButton(3, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.sequence_editor")));
        this.buttonList.add(new ThemedButton(6, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.packet.main.manage_sequences")));
        this.buttonList.add(new ThemedButton(4, contentX, contentY, contentWidth, BUTTON_HEIGHT,
                I18n.format("gui.common.back")));

        String samplingState = snapshot.samplingModulo > 1
                ? "1/" + Math.max(1, snapshot.samplingModulo)
                : "OFF";
        captureSummary = PacketCaptureHandler.isCapturing ? "采集中" : "已停止";
        businessSummary = snapshot.businessPacketProcessingEnabled ? "业务链已启用" : "业务链已暂停";
        queueSummary = "队列 " + snapshot.queueSize + " | 采样 " + samplingState + " | 丢弃 " + snapshot.droppedCount;
        footerSummary = "发送 " + snapshot.sentCount + " / 接收 " + snapshot.receivedCount;

        refreshButtonLayout();
    }

    private void recalcLayout() {
        this.panelWidth = Math.min(560, this.width - 24);
        this.panelHeight = Math.min(Math.max(300, this.height - 28), 420);
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;

        this.summaryX = panelX + 12;
        this.summaryY = panelY + TITLE_HEIGHT + 10;
        this.summaryWidth = panelWidth - 24;
        this.summaryHeight = SUMMARY_HEIGHT;

        this.contentX = panelX + 12;
        this.contentY = summaryY + summaryHeight + 10;
        this.contentWidth = panelWidth - 24;
        this.footerY = panelY + panelHeight - FOOTER_HEIGHT - 10;
        this.contentHeight = footerY - contentY - 8;
    }

    private void refreshButtonLayout() {
        List<SectionDefinition> sections = buildSections();
        this.visibleSections.clear();

        int sectionColumnGap = 8;
        int sectionWidth = Math.max(150, (contentWidth - sectionColumnGap) / 2);
        int totalHeight = computeTotalSectionHeight(sections, sectionWidth, sectionColumnGap);
        this.maxScroll = Math.max(0, totalHeight - contentHeight);
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int currentBaseY = contentY - scrollOffset;

        for (int i = 0; i < sections.size(); i += 2) {
            SectionDefinition leftSection = sections.get(i);
            SectionDefinition rightSection = i + 1 < sections.size() ? sections.get(i + 1) : null;

            int leftHeight = computeSectionHeight(leftSection, sectionWidth);
            int rightHeight = rightSection == null ? 0 : computeSectionHeight(rightSection, sectionWidth);
            int rowHeight = Math.max(leftHeight, rightHeight);

            layoutSection(leftSection, contentX, currentBaseY, sectionWidth, leftHeight);
            if (rightSection != null) {
                layoutSection(rightSection, contentX + sectionWidth + sectionColumnGap, currentBaseY, sectionWidth,
                        rightHeight);
            }

            currentBaseY += rowHeight + SECTION_GAP;
        }

        GuiButton backButton = getButtonById(4);
        if (backButton != null) {
            backButton.x = contentX + 8;
            backButton.y = footerY + 5;
            backButton.width = Math.max(92, Math.min(126, contentWidth / 4));
            backButton.height = BUTTON_HEIGHT;
            backButton.visible = true;
        }
    }

    private List<SectionDefinition> buildSections() {
        List<SectionDefinition> sections = new ArrayList<>();
        sections.add(new SectionDefinition("采集控制",
                new SectionItem(0, 2),
                new SectionItem(7, 1),
                new SectionItem(10, 1)));
        sections.add(new SectionDefinition("浏览与清理",
                new SectionItem(1, 2),
                new SectionItem(5, 1),
                new SectionItem(2, 1)));
        sections.add(new SectionDefinition("规则与提取",
                new SectionItem(8, 1),
                new SectionItem(11, 1),
                new SectionItem(9, 2)));
        sections.add(new SectionDefinition("序列工具",
                new SectionItem(3, 1),
                new SectionItem(6, 1)));
        return sections;
    }

    private int computeTotalSectionHeight(List<SectionDefinition> sections, int sectionWidth, int sectionColumnGap) {
        int total = 0;
        for (int i = 0; i < sections.size(); i += 2) {
            int leftHeight = computeSectionHeight(sections.get(i), sectionWidth);
            int rightHeight = i + 1 < sections.size() ? computeSectionHeight(sections.get(i + 1), sectionWidth) : 0;
            total += Math.max(leftHeight, rightHeight);
            if (i + 2 < sections.size()) {
                total += SECTION_GAP;
            }
        }
        return total;
    }

    private int computeSectionHeight(SectionDefinition section, int sectionWidth) {
        int rows = 0;
        int nextColumn = 0;
        int innerButtonWidth = Math.max(92, (sectionWidth - SECTION_PADDING * 2 - BUTTON_GAP) / 2);
        for (SectionItem item : section.items) {
            if (item.span >= 2 || innerButtonWidth < 108) {
                if (nextColumn != 0) {
                    rows++;
                    nextColumn = 0;
                }
                rows++;
                continue;
            }
            if (nextColumn == 1) {
                rows++;
                nextColumn = 0;
            } else {
                nextColumn = 1;
            }
        }
        if (nextColumn != 0) {
            rows++;
        }
        return SECTION_PADDING * 2 + SECTION_HEADER_HEIGHT + 6
                + rows * BUTTON_HEIGHT + Math.max(0, rows - 1) * BUTTON_GAP;
    }

    private void layoutSection(SectionDefinition section, int sectionX, int sectionY, int sectionWidth, int sectionHeight) {
        if (section == null) {
            return;
        }
        this.visibleSections.add(new SectionRenderInfo(section.title,
                new Rectangle(sectionX, sectionY, sectionWidth, sectionHeight)));

        int columnWidth = Math.max(92, (sectionWidth - SECTION_PADDING * 2 - BUTTON_GAP) / 2);
        int buttonY = sectionY + SECTION_PADDING + SECTION_HEADER_HEIGHT + 6;
        int nextColumn = 0;

        for (SectionItem item : section.items) {
            GuiButton button = getButtonById(item.buttonId);
            if (button == null) {
                continue;
            }

            boolean fullSpan = item.span >= 2 || columnWidth < 108;
            if (fullSpan) {
                if (nextColumn != 0) {
                    buttonY += BUTTON_HEIGHT + BUTTON_GAP;
                    nextColumn = 0;
                }
                button.x = sectionX + SECTION_PADDING;
                button.y = buttonY;
                button.width = sectionWidth - SECTION_PADDING * 2;
                button.height = BUTTON_HEIGHT;
                buttonY += BUTTON_HEIGHT + BUTTON_GAP;
            } else {
                int buttonX = sectionX + SECTION_PADDING + nextColumn * (columnWidth + BUTTON_GAP);
                button.x = buttonX;
                button.y = buttonY;
                button.width = columnWidth;
                button.height = BUTTON_HEIGHT;
                if (nextColumn == 1) {
                    buttonY += BUTTON_HEIGHT + BUTTON_GAP;
                    nextColumn = 0;
                } else {
                    nextColumn = 1;
                }
            }

            button.visible = isButtonVisible(button.y, button.height);
        }
    }

    private GuiButton getButtonById(int id) {
        for (GuiButton button : this.buttonList) {
            if (button != null && button.id == id) {
                return button;
            }
        }
        return null;
    }

    private boolean isButtonVisible(int y, int height) {
        return y + height >= contentY && y <= contentY + contentHeight;
    }

    @Override
    public void onGuiClosed() {
        if (parentScreen instanceof GuiPacketMain) {
            ((GuiPacketMain) parentScreen).initGui();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;

        if (!isInsideContent(mouseX, mouseY) || maxScroll <= 0) {
            return;
        }

        if (dWheel < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 20);
        } else {
            scrollOffset = Math.max(0, scrollOffset - 20);
        }
        refreshButtonLayout();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                PacketCaptureHandler.isCapturing = !PacketCaptureHandler.isCapturing;
                initGui();
                break;
            case 1:
                mc.setScreen(new GuiPacketViewer(this));
                break;
            case 2:
                PacketCaptureHandler.clearAllPackets();
                initGui();
                break;
            case 3:
                mc.setScreen(new GuiPacketSequenceEditor(this, new ArrayList<>()));
                break;
            case 4:
                mc.setScreen(parentScreen);
                break;
            case 5:
                mc.setScreen(new GuiPacketFilter(this));
                break;
            case 6:
                mc.setScreen(new GuiPacketSequenceManager(this));
                break;
            case 7:
                PacketFilterConfig.INSTANCE.captureMode = PacketFilterConfig.INSTANCE.captureMode.next();
                PacketFilterConfig.save();
                initGui();
                break;
            case 8:
                mc.setScreen(new GuiCapturedIdViewer(this));
                break;
            case 9:
                mc.setScreen(new GuiPacketInterceptRules(this));
                break;
            case 10:
                PacketFilterConfig.INSTANCE.enableBusinessPacketProcessing = !PacketFilterConfig.INSTANCE.enableBusinessPacketProcessing;
                PacketFilterConfig.save();
                initGui();
                break;
            case 11:
                mc.setScreen(new GuiPacketFieldRules(this));
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX - 8, panelY - 8, panelWidth + 16, panelHeight + 16);
        GuiTheme.drawTitleBar(panelX - 8, panelY - 8, panelWidth + 16, I18n.format("gui.packet.main.title"),
                this.fontRenderer);

        drawSummaryPanel();
        drawScrollableSections();
        drawFooterBar();

        if (maxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((contentHeight / (float) Math.max(contentHeight, contentHeight + maxScroll)) * contentHeight));
            int track = Math.max(1, contentHeight - thumbHeight);
            int thumbY = contentY + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(contentX + contentWidth + 4, contentY, 4, contentHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawButtonTooltip(mouseX, mouseY);
    }

    private void drawSummaryPanel() {
        GuiTheme.drawInputFrameSafe(summaryX - 1, summaryY - 1, summaryWidth + 2, summaryHeight + 2, false, true);
        int columnGap = 8;
        int columnWidth = (summaryWidth - columnGap * 2) / 3;

        drawSummaryColumn(summaryX, summaryY, columnWidth, "采集状态", captureSummary, footerSummary);
        drawSummaryColumn(summaryX + columnWidth + columnGap, summaryY, columnWidth, "业务链", businessSummary,
                modeButton == null ? "" : modeButton.displayString.replace(I18n.format("gui.packet.main.mode", ""), "").trim());
        drawSummaryColumn(summaryX + (columnWidth + columnGap) * 2, summaryY, columnWidth, "处理压力", queueSummary,
                "滚轮仅在内容区生效");
    }

    private void drawSummaryColumn(int x, int y, int width, String title, String primary, String secondary) {
        drawRect(x, y, x + width, y + summaryHeight, 0x22182430);
        drawString(this.fontRenderer, title, x + 6, y + 5, 0xFFAEDBFF);
        drawString(this.fontRenderer, trimToWidth(primary, width - 12), x + 6, y + 18, 0xFFFFFFFF);
        drawString(this.fontRenderer, trimToWidth(secondary, width - 12), x + 6, y + 31, 0xFF97ACC0);
    }

    private void drawScrollableSections() {
        GuiTheme.drawInputFrameSafe(contentX - 1, contentY - 1, contentWidth + 2, contentHeight + 2, false, true);
        for (SectionRenderInfo section : visibleSections) {
            if (section == null || section.bounds == null) {
                continue;
            }
            Rectangle bounds = section.bounds;
            if (bounds.y + bounds.height < contentY || bounds.y > contentY + contentHeight) {
                continue;
            }
            drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x2A1B2633);
            drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + 1, 0xFF4A7AA0);
            drawRect(bounds.x, bounds.y, bounds.x + 1, bounds.y + bounds.height, 0x883B5872);
            drawRect(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                    0x883B5872);
            drawRect(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width, bounds.y + bounds.height,
                    0x883B5872);
            drawString(this.fontRenderer, section.title, bounds.x + SECTION_PADDING, bounds.y + SECTION_PADDING,
                    0xFFEAF7FF);
        }
    }

    private void drawFooterBar() {
        GuiTheme.drawInputFrameSafe(contentX - 1, footerY - 1, contentWidth + 2, FOOTER_HEIGHT, false, true);
        drawString(this.fontRenderer, "固定操作", contentX + 8, footerY + 7, 0xFF9FB2C8);
    }

    private void drawButtonTooltip(int mouseX, int mouseY) {
        GuiButton hovered = null;
        for (GuiButton button : this.buttonList) {
            if (button != null && button.visible && mouseX >= button.x && mouseX <= button.x + button.width
                    && mouseY >= button.y && mouseY <= button.y + button.height) {
                hovered = button;
                break;
            }
        }
        if (hovered == null) {
            return;
        }

        String tooltip = getTooltipForButton(hovered.id);
        if (tooltip == null || tooltip.trim().isEmpty()) {
            return;
        }
        drawHoveringText(Arrays.asList(tooltip.split("\n")), mouseX, mouseY);
    }

    private String getTooltipForButton(int buttonId) {
        PacketCaptureHandler.PacketCaptureUiSnapshot snapshot = PacketCaptureHandler.getUiSnapshot();
        switch (buttonId) {
            case 0:
                return "开始或停止当前抓包会话。\n"
                        + "开启后会持续记录发送/接收的数据包，供查看、复制、保存快照或生成规则。\n"
                        + "当前状态: " + (PacketCaptureHandler.isCapturing ? "运行中" : "已停止");
            case 1:
                return "打开抓包查看器。\n"
                        + "可浏览已捕获的发送/接收数据包，支持筛选、复制、保存快照、发送模拟和智能生成器。\n"
                        + "当前统计: 发送 " + snapshot.sentCount + " / 接收 " + snapshot.receivedCount;
            case 2:
                return "清空当前已捕获的数据包列表。\n"
                        + "只清除内存中的抓包结果，不会删除规则、快照和已保存的序列。";
            case 3:
                return "打开数据包序列编辑器。\n"
                        + "用于手动组装多个数据包，设置方向、延迟，并作为一组序列发送或保存。";
            case 4:
                return "返回上一级界面。\n"
                        + "这是固定在底部的操作按钮，不会跟随内容滚动。";
            case 5:
                return "打开抓包过滤设置。\n"
                        + "配置黑白名单、频道/类名过滤条件，控制哪些数据包进入捕获列表。";
            case 6:
                return "打开数据包序列管理器。\n"
                        + "查看、编辑、复用、发送已经保存的数据包序列。";
            case 7:
                return "切换抓包模式。\n"
                        + "黑名单: 默认捕获，排除命中的数据包。\n"
                        + "白名单: 默认不捕获，只保留命中的数据包。";
            case 8:
                return "打开捕获ID规则管理器。\n"
                        + "维护正则捕获规则、查看当前捕获值，并可进入智能生成器自动生成规则。";
            case 9:
                return "打开拦截规则管理器。\n"
                        + "可对数据包做匹配、替换、阻断或重写，适合高级调试与协议改写。\n"
                        + "当前状态: " + (PacketInterceptConfig.INSTANCE.inboundInterceptEnabled ? "已启用" : "已关闭");
            case 10:
                return "切换业务链处理中心。\n"
                        + "开启后会让抓包系统继续执行捕获ID、字段提取等后续业务逻辑。\n"
                        + "关闭后只保留基础抓包，不再触发这些业务处理。\n"
                        + "当前状态: " + (snapshot.businessPacketProcessingEnabled ? "已启用" : "已暂停");
            case 11:
                return "打开字段提取规则。\n"
                        + "可从数据包 HEX 或解码文本中提取字段，生成变量或供其它系统联动使用。";
            default:
                return "";
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        String safe = text == null ? "" : text;
        return this.fontRenderer == null ? safe : this.fontRenderer.trimStringToWidth(safe, Math.max(24, maxWidth));
    }

    private boolean isInsideContent(int mouseX, int mouseY) {
        return mouseX >= contentX && mouseX <= contentX + contentWidth
                && mouseY >= contentY && mouseY <= contentY + contentHeight;
    }
}


