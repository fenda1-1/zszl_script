package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager.ExecutionEvent;
import com.zszl.zszlScriptMod.path.runtime.log.ExecutionLogManager.SessionSnapshot;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuiExecutionLogViewer extends ThemedGuiScreen {

    private static final int SESSION_ROW_HEIGHT = 50;
    private static final int SESSION_CARD_HEIGHT = 42;
    private static final int VARIABLE_CARD_GAP = 6;
    private static final int EVENT_CARD_GAP = 8;
    private static final int DETAIL_SCROLL_STEP = 18;
    private static final SimpleDateFormat DISPLAY_TIME = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);
    private static final SimpleDateFormat DETAIL_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final SimpleDateFormat CARD_TIME = new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT);

    private static final class VariableCard {
        private final String name;
        private final List<String> valueLines;
        private final int offsetY;
        private final int height;

        private VariableCard(String name, List<String> valueLines, int offsetY, int height) {
            this.name = name;
            this.valueLines = valueLines;
            this.offsetY = offsetY;
            this.height = height;
        }
    }

    private static final class EventCard {
        private final String header;
        private final String subHeader;
        private final List<String> messageLines;
        private final List<String> statusLines;
        private final List<String> variableLines;
        private final int offsetY;
        private final int height;
        private final int accentColor;

        private EventCard(String header, String subHeader, List<String> messageLines, List<String> statusLines,
                List<String> variableLines, int offsetY, int height, int accentColor) {
            this.header = header;
            this.subHeader = subHeader;
            this.messageLines = messageLines;
            this.statusLines = statusLines;
            this.variableLines = variableLines;
            this.offsetY = offsetY;
            this.height = height;
            this.accentColor = accentColor;
        }
    }

    private final GuiScreen parentScreen;
    private final List<SessionSnapshot> sessions = new ArrayList<>();
    private final List<VariableCard> variableCards = new ArrayList<>();
    private final List<EventCard> eventCards = new ArrayList<>();

    private int selectedIndex = -1;
    private int sessionScroll = 0;
    private int variableScroll = 0;
    private int eventScroll = 0;
    private int maxSessionScroll = 0;
    private int maxVariableScroll = 0;
    private int maxEventScroll = 0;
    private int sessionListX;
    private int sessionListY;
    private int sessionListW;
    private int sessionListH;
    private int detailX;
    private int detailY;
    private int detailW;
    private int detailH;
    private int variableViewportX;
    private int variableViewportY;
    private int variableViewportW;
    private int variableViewportH;
    private int eventViewportX;
    private int eventViewportY;
    private int eventViewportW;
    private int eventViewportH;
    private int variableToggleX;
    private int variableToggleY;
    private int variableToggleW;
    private int variableToggleH;
    private int variableContentHeight = 0;
    private int eventContentHeight = 0;
    private String detailCacheSessionId = "";
    private int detailCacheVariableWidth = -1;
    private int detailCacheEventWidth = -1;
    private String footerMessage = "";
    private long footerMessageUntil = 0L;
    private boolean variableSectionCollapsed = false;

    public GuiExecutionLogViewer(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelX = 10;
        int panelY = 10;
        int panelW = this.width - 20;
        int panelH = this.height - 20;

        sessionListX = panelX + 10;
        sessionListY = panelY + 34;
        sessionListW = Math.max(180, panelW / 3);
        sessionListH = panelH - 84;

        detailX = sessionListX + sessionListW + 10;
        detailY = sessionListY;
        detailW = panelW - (detailX - panelX) - 10;
        detailH = sessionListH;

        int buttonY = panelY + panelH - 34;
        int buttonSpacing = 8;
        int buttonW = Math.max(1, (panelW - 20 - buttonSpacing * 4) / 5);
        int buttonX = panelX + 10;
        this.buttonList.add(new ThemedButton(0, buttonX, buttonY, buttonW, 20, "返回"));
        buttonX += buttonW + buttonSpacing;
        this.buttonList.add(new ThemedButton(1, buttonX, buttonY, buttonW, 20, "刷新"));
        buttonX += buttonW + buttonSpacing;
        this.buttonList.add(new ThemedButton(2, buttonX, buttonY, buttonW, 20, "复制选中"));
        buttonX += buttonW + buttonSpacing;
        this.buttonList.add(new ThemedButton(3, buttonX, buttonY, buttonW, 20, "导出选中"));
        buttonX += buttonW + buttonSpacing;
        this.buttonList.add(new ThemedButton(4, buttonX, buttonY, buttonW, 20, "清空日志"));

        reloadSessions();
    }

    private void reloadSessions() {
        List<SessionSnapshot> snapshot = ExecutionLogManager.getSessionsSnapshot();
        sessions.clear();
        sessions.addAll(snapshot);
        if (sessions.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex < 0 || selectedIndex >= sessions.size()) {
            selectedIndex = 0;
        }
        sessionScroll = clamp(sessionScroll, 0, Math.max(0, sessions.size() - visibleSessionRows()));
        recalcEventScroll();
        updateButtonStates();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
            switch (button.id) {
            case 0:
                this.mc.setScreen(parentScreen);
                return;
            case 1:
                reloadSessions();
                setFooter("已刷新执行日志");
                return;
            case 2:
                copySelectedSession();
                return;
            case 3:
                SessionSnapshot selected = getSelectedSession();
                if (selected == null) {
                    setFooter("请先选择一条执行日志");
                    return;
                }
                Path exported = ExecutionLogManager.exportSession(selected.getSessionId());
                setFooter(exported == null ? "导出失败" : "已导出到: " + exported.toAbsolutePath());
                return;
            case 4:
                ExecutionLogManager.clearSessions();
                reloadSessions();
                setFooter("已清空执行日志");
                return;
            default:
                break;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (isInside(mouseX, mouseY, sessionListX, sessionListY, sessionListW, sessionListH)) {
            sessionScroll = clamp(sessionScroll - Integer.signum(dWheel), 0, maxSessionScroll);
        } else if (isInside(mouseX, mouseY, variableViewportX, variableViewportY, variableViewportW,
                variableViewportH)) {
            variableScroll = clamp(variableScroll - Integer.signum(dWheel) * DETAIL_SCROLL_STEP, 0, maxVariableScroll);
        } else if (isInside(mouseX, mouseY, eventViewportX, eventViewportY, eventViewportW, eventViewportH)) {
            eventScroll = clamp(eventScroll - Integer.signum(dWheel) * DETAIL_SCROLL_STEP, 0, maxEventScroll);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && isInside(mouseX, mouseY, variableToggleX, variableToggleY, variableToggleW, variableToggleH)) {
            variableSectionCollapsed = !variableSectionCollapsed;
            variableScroll = 0;
            return;
        }
        if (!isInside(mouseX, mouseY, sessionListX, sessionListY, sessionListW, sessionListH)) {
            return;
        }
        int index = getSessionIndexAt(mouseX, mouseY);
        if (index >= 0) {
            selectedIndex = index;
            recalcEventScroll();
            updateButtonStates();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelX = 10;
        int panelY = 10;
        int panelW = this.width - 20;
        int panelH = this.height - 20;
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "执行日志", this.fontRenderer);

        GuiTheme.drawSectionTitle(sessionListX, sessionListY - 12, "最近会话", this.fontRenderer);
        GuiTheme.drawSectionTitle(detailX, detailY - 12, "日志详情", this.fontRenderer);

        drawRect(sessionListX, sessionListY, sessionListX + sessionListW, sessionListY + sessionListH, 0x55203038);
        GuiTheme.drawInputFrameSafe(sessionListX, sessionListY, sessionListW, sessionListH, false, true);
        drawRect(detailX, detailY, detailX + detailW, detailY + detailH, 0x55203038);
        GuiTheme.drawInputFrameSafe(detailX, detailY, detailW, detailH, false, true);

        drawSessionList(mouseX, mouseY);
        drawDetailPanel(mouseX, mouseY);

        if (!footerMessage.isEmpty() && System.currentTimeMillis() <= footerMessageUntil) {
            this.drawString(this.fontRenderer, "§b" + trim(footerMessage, panelW - 24), panelX + 10,
                    panelY + panelH - 48, 0xFFFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSessionList(int mouseX, int mouseY) {
        int visibleRows = visibleSessionRows();
        maxSessionScroll = Math.max(0, sessions.size() - visibleRows);
        sessionScroll = clamp(sessionScroll, 0, maxSessionScroll);
        if (sessions.isEmpty()) {
            GuiTheme.drawEmptyState(sessionListX + sessionListW / 2, sessionListY + sessionListH / 2 - 4,
                    "暂无执行会话", this.fontRenderer);
            return;
        }

        int end = Math.min(sessions.size(), sessionScroll + visibleRows);
        int cardX = sessionListX + 6;
        int cardW = sessionListW - 18;
        int y = sessionListY + 6;
        for (int i = sessionScroll; i < end; i++) {
            SessionSnapshot session = sessions.get(i);
            boolean selected = i == selectedIndex;
            boolean hovered = isInside(mouseX, mouseY, cardX, y, cardW, SESSION_CARD_HEIGHT);
            int accentColor = getSessionAccentColor(session);
            int borderColor = selected ? 0xFF83D7FF : (hovered ? 0xFF597C9A : 0xFF314559);
            int bodyColor = selected ? 0xCC223041 : 0xAA18222D;
            drawRect(cardX - 1, y - 1, cardX + cardW + 1, y + SESSION_CARD_HEIGHT + 1, borderColor);
            drawRect(cardX, y, cardX + cardW, y + SESSION_CARD_HEIGHT, bodyColor);
            drawRect(cardX, y, cardX + cardW, y + 2, accentColor);
            drawRect(cardX, y, cardX + 3, y + SESSION_CARD_HEIGHT, accentColor);
            GuiTheme.drawCardHighlight(cardX, y, cardW, SESSION_CARD_HEIGHT, hovered);

            String modeText = session.isBackground() ? "后台" : "前台";
            int modeWidth = this.fontRenderer.getStringWidth(modeText);
            this.drawString(this.fontRenderer,
                    getSessionTitleColor(session) + trim(session.getSequenceName(), cardW - modeWidth - 28),
                    cardX + 10, y + 5, 0xFFFFFFFF);
            this.drawString(this.fontRenderer, "§7" + modeText, cardX + cardW - modeWidth - 8, y + 5, 0xFFCAD6E3);
            this.drawString(this.fontRenderer,
                    trim(CARD_TIME.format(new Date(session.getStartTime())) + "  ·  "
                            + getSessionResultText(session) + "  ·  " + session.getEvents().size() + " 条",
                            cardW - 18),
                    cardX + 10, y + 18, 0xFFCFDAE5);
            this.drawString(this.fontRenderer, trim(buildSessionCardDetail(session), cardW - 18),
                    cardX + 10, y + 30, 0xFFAAB8C7);
            y += SESSION_ROW_HEIGHT;
        }

        if (maxSessionScroll > 0) {
            int thumbHeight = Math.max(18, (int) ((float) visibleRows / Math.max(visibleRows, sessions.size())
                    * sessionListH));
            int thumbY = sessionListY + (int) ((float) sessionScroll / maxSessionScroll
                    * Math.max(1, sessionListH - thumbHeight));
            GuiTheme.drawScrollbar(sessionListX + sessionListW - 8, sessionListY, 6, sessionListH, thumbY, thumbHeight);
        }
    }

    private void drawDetailPanel(int mouseX, int mouseY) {
        SessionSnapshot session = getSelectedSession();
        if (session == null) {
            this.drawString(this.fontRenderer, "§7暂无执行日志", detailX + 8, detailY + 8, 0xFFFFFFFF);
            variableToggleX = 0;
            variableToggleY = 0;
            variableToggleW = 0;
            variableToggleH = 0;
            variableViewportX = 0;
            variableViewportY = 0;
            variableViewportW = 0;
            variableViewportH = 0;
            eventViewportX = 0;
            eventViewportY = 0;
            eventViewportW = 0;
            eventViewportH = 0;
            return;
        }

        int contentX = detailX + 8;
        int contentW = detailW - 16;
        int contentTop = detailY + 8;
        int contentBottom = detailY + detailH - 8;
        int titleSpacing = 12;
        int sectionGap = 10;
        variableToggleW = 16;
        variableToggleH = 14;
        variableToggleX = contentX + contentW - variableToggleW;
        variableToggleY = contentTop - 2;

        GuiTheme.drawSectionTitle(contentX, contentTop, "初始变量卡片", this.fontRenderer);
        drawVariableSectionToggle(mouseX, mouseY);

        if (variableSectionCollapsed) {
            variableViewportX = 0;
            variableViewportY = 0;
            variableViewportW = 0;
            variableViewportH = 0;
        } else {
            variableViewportX = contentX;
            variableViewportY = contentTop + titleSpacing;
            variableViewportW = contentW;
            variableViewportH = computeExpandedVariableViewportHeight(contentTop, contentBottom, titleSpacing, sectionGap);
        }

        int eventTitleY = variableSectionCollapsed
                ? contentTop + titleSpacing + sectionGap
                : variableViewportY + variableViewportH + sectionGap;
        GuiTheme.drawSectionTitle(contentX, eventTitleY, "执行步骤卡片", this.fontRenderer);
        eventViewportX = contentX;
        eventViewportY = eventTitleY + titleSpacing;
        eventViewportW = contentW;
        eventViewportH = Math.max(42, contentBottom - eventViewportY);

        ensureDetailCache(session, contentW - 28, eventViewportW - 28);

        if (!variableSectionCollapsed) {
            drawRect(variableViewportX, variableViewportY, variableViewportX + variableViewportW,
                    variableViewportY + variableViewportH, 0x44202E3B);
            GuiTheme.drawInputFrameSafe(variableViewportX, variableViewportY, variableViewportW, variableViewportH, false,
                    true);
        }

        drawRect(eventViewportX, eventViewportY, eventViewportX + eventViewportW, eventViewportY + eventViewportH,
                0x44202E3B);
        GuiTheme.drawInputFrameSafe(eventViewportX, eventViewportY, eventViewportW, eventViewportH, false, true);

        if (!variableSectionCollapsed) {
            drawVariableCards(mouseX, mouseY);
        }
        drawEventCards(mouseX, mouseY);
    }

    private void drawSummaryCard(SessionSnapshot session, int x, int y, int w, int h, List<String> reasonLines) {
        int accent = getSessionAccentColor(session);
        drawRect(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF3D5770);
        drawRect(x, y, x + w, y + h, 0xAA16212B);
        drawRect(x, y, x + w, y + 2, accent);
        drawRect(x, y, x + 3, y + h, accent);

        int textX = x + 10;
        int lineY = y + 7;
        this.drawString(this.fontRenderer,
                trim("序列: " + session.getSequenceName() + " / " + (session.isBackground() ? "后台" : "前台"), w - 18),
                textX, lineY, 0xFFFFFFFF);
        lineY += 10;
        this.drawString(this.fontRenderer, trim("会话ID: " + session.getSessionId(), w - 18),
                textX, lineY, 0xFFD7E4F1);
        lineY += 10;
        this.drawString(this.fontRenderer,
                trim("开始: " + DETAIL_TIME.format(new Date(session.getStartTime())) + " / 时长: "
                        + session.getDurationMs() + " ms", w - 18),
                textX, lineY, 0xFFC8D7E8);
        lineY += 10;
        lineY = drawWrappedLines(reasonLines, textX, lineY, 0xFFC8D7E8);
        this.drawString(this.fontRenderer, trim("状态: " + safe(session.getFinalStatus()), w - 18),
                textX, lineY, 0xFFB6CAE0);
    }

    private int computeExpandedVariableViewportHeight(int contentTop, int contentBottom, int titleSpacing, int sectionGap) {
        int minViewportHeight = 42;
        int maxPreferredHeight = 128;
        int preferredHeight = variableCards.isEmpty() ? 54 : Math.min(maxPreferredHeight, variableContentHeight + 8);
        int contentAreaHeight = Math.max(0, contentBottom - contentTop);
        int softCap = Math.max(minViewportHeight, (contentAreaHeight - sectionGap - titleSpacing * 2) / 3);
        int maxAllowed = Math.max(minViewportHeight,
                contentBottom - (contentTop + titleSpacing) - sectionGap - titleSpacing - minViewportHeight);
        return Math.max(minViewportHeight, Math.min(Math.min(preferredHeight, softCap), maxAllowed));
    }

    private void drawVariableSectionToggle(int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, variableToggleX, variableToggleY, variableToggleW, variableToggleH);
        GuiTheme.UiState state = hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
        GuiTheme.drawButtonFrameSafe(variableToggleX, variableToggleY, variableToggleW, variableToggleH, state);
        drawCenteredString(this.fontRenderer, variableSectionCollapsed ? "v" : "^",
                variableToggleX + variableToggleW / 2,
                variableToggleY + 3,
                GuiTheme.getStateTextColor(state));
    }

    private void drawVariableCards(int mouseX, int mouseY) {
        int visibleHeight = Math.max(1, variableViewportH - 8);
        maxVariableScroll = Math.max(0, variableContentHeight - visibleHeight);
        variableScroll = clamp(variableScroll, 0, maxVariableScroll);

        if (variableCards.isEmpty()) {
            GuiTheme.drawEmptyState(variableViewportX + variableViewportW / 2,
                    variableViewportY + variableViewportH / 2 - 4,
                    "没有初始变量", this.fontRenderer);
            return;
        }

        beginScissor(variableViewportX + 1, variableViewportY + 1, variableViewportW - 2, variableViewportH - 2);
        int cardX = variableViewportX + 5;
        int cardW = variableViewportW - 12;
        int baseY = variableViewportY + 4 - variableScroll;
        for (VariableCard card : variableCards) {
            int cardY = baseY + card.offsetY;
            if (cardY + card.height < variableViewportY + 1 || cardY > variableViewportY + variableViewportH - 1) {
                continue;
            }
            boolean hovered = isInside(mouseX, mouseY, cardX, cardY, cardW, card.height);
            drawVariableCard(card, cardX, cardY, cardW, hovered);
        }
        endScissor();

        if (maxVariableScroll > 0) {
            drawPixelScrollbar(variableViewportX + variableViewportW - 8, variableViewportY, 6, variableViewportH,
                    variableScroll, maxVariableScroll, variableContentHeight);
        }
    }

    private void drawVariableCard(VariableCard card, int x, int y, int w, boolean hovered) {
        drawRect(x - 1, y - 1, x + w + 1, y + card.height + 1, hovered ? 0xFF6286A2 : 0xFF34526A);
        drawRect(x, y, x + w, y + card.height, 0xAA17232E);
        drawRect(x, y, x + w, y + 2, 0xFF56B6E8);
        drawRect(x, y, x + 2, y + card.height, 0xFF56B6E8);

        int textX = x + 10;
        int lineY = y + 6;
        this.drawString(this.fontRenderer, trim(card.name, w - 20), textX, lineY, 0xFFFFFFFF);
        lineY += 12;
        drawWrappedLines(card.valueLines, textX, lineY, 0xFFCEDAE6);
    }

    private void drawEventCards(int mouseX, int mouseY) {
        int visibleHeight = Math.max(1, eventViewportH - 8);
        maxEventScroll = Math.max(0, eventContentHeight - visibleHeight);
        eventScroll = clamp(eventScroll, 0, maxEventScroll);

        if (eventCards.isEmpty()) {
            GuiTheme.drawEmptyState(eventViewportX + eventViewportW / 2, eventViewportY + eventViewportH / 2 - 4,
                    "没有执行步骤", this.fontRenderer);
            return;
        }

        beginScissor(eventViewportX + 1, eventViewportY + 1, eventViewportW - 2, eventViewportH - 2);
        int cardX = eventViewportX + 5;
        int cardW = eventViewportW - 12;
        int baseY = eventViewportY + 4 - eventScroll;
        for (EventCard card : eventCards) {
            int cardY = baseY + card.offsetY;
            if (cardY + card.height < eventViewportY + 1 || cardY > eventViewportY + eventViewportH - 1) {
                continue;
            }
            boolean hovered = isInside(mouseX, mouseY, cardX, cardY, cardW, card.height);
            drawEventCard(card, cardX, cardY, cardW, hovered);
        }
        endScissor();

        if (maxEventScroll > 0) {
            drawPixelScrollbar(eventViewportX + eventViewportW - 8, eventViewportY, 6, eventViewportH,
                    eventScroll, maxEventScroll, eventContentHeight);
        }
    }

    private void drawEventCard(EventCard card, int x, int y, int w, boolean hovered) {
        int border = hovered ? 0xFF6C8AA7 : 0xFF39556C;
        drawRect(x - 1, y - 1, x + w + 1, y + card.height + 1, border);
        drawRect(x, y, x + w, y + card.height, 0xAA16212B);
        drawRect(x, y, x + w, y + 2, card.accentColor);
        drawRect(x, y, x + 3, y + card.height, card.accentColor);

        int textX = x + 10;
        int lineY = y + 6;
        this.drawString(this.fontRenderer, trim(card.header, w - 20), textX, lineY, 0xFFFFFFFF);
        lineY += 12;
        this.drawString(this.fontRenderer, trim(card.subHeader, w - 20), textX, lineY, 0xFFB6CAE0);
        lineY += 12;
        lineY = drawWrappedLines(card.messageLines, textX, lineY, 0xFFD6E5F2);
        if (!card.statusLines.isEmpty()) {
            lineY += 2;
            lineY = drawWrappedLines(card.statusLines, textX, lineY, 0xFFC1DDB6);
        }
        if (!card.variableLines.isEmpty()) {
            lineY += 2;
            drawWrappedLines(card.variableLines, textX, lineY, 0xFFC8D1DB);
        }
    }

    private void ensureDetailCache(SessionSnapshot session, int variableWrapWidth, int eventWrapWidth) {
        String sessionId = safe(session.getSessionId());
        if (sessionId.equals(detailCacheSessionId)
                && detailCacheVariableWidth == variableWrapWidth
                && detailCacheEventWidth == eventWrapWidth) {
            return;
        }

        detailCacheSessionId = sessionId;
        detailCacheVariableWidth = variableWrapWidth;
        detailCacheEventWidth = eventWrapWidth;
        variableCards.clear();
        eventCards.clear();

        int variableOffset = 0;
        for (Map.Entry<String, String> entry : session.getInitialVariables().entrySet()) {
            List<String> valueLines = buildWrappedLines(safe(entry.getValue()).isEmpty() ? "(空)" : entry.getValue(),
                    variableWrapWidth);
            int height = Math.max(38, 18 + valueLines.size() * 10 + 10);
            variableCards.add(new VariableCard(entry.getKey(), valueLines, variableOffset, height));
            variableOffset += height + VARIABLE_CARD_GAP;
        }
        variableContentHeight = Math.max(0, variableOffset - VARIABLE_CARD_GAP);

        int eventOffset = 0;
        for (ExecutionEvent event : session.getEvents()) {
            List<String> messageLines = buildWrappedLines("消息: " + safe(event.getMessage()), eventWrapWidth);
            List<String> statusLines = safe(event.getStatus()).isEmpty()
                    ? new ArrayList<String>()
                    : buildWrappedLines("状态: " + event.getStatus(), eventWrapWidth);
            List<String> variableLines = event.getVariablePreview().isEmpty()
                    ? new ArrayList<String>()
                    : buildWrappedLines("变量: " + joinVariables(event.getVariablePreview()), eventWrapWidth);
            int height = 18 + 10 + 12 + messageLines.size() * 10;
            if (!statusLines.isEmpty()) {
                height += 2 + statusLines.size() * 10;
            }
            if (!variableLines.isEmpty()) {
                height += 2 + variableLines.size() * 10;
            }
            height += 10;
            eventCards.add(new EventCard(
                    DISPLAY_TIME.format(new Date(event.getTimestamp())) + "  ·  "
                            + event.getType().toUpperCase(Locale.ROOT),
                    buildEventSubHeader(event),
                    messageLines,
                    statusLines,
                    variableLines,
                    eventOffset,
                    Math.max(54, height),
                    getEventAccentColor(event)));
            eventOffset += Math.max(54, height) + EVENT_CARD_GAP;
        }
        eventContentHeight = Math.max(0, eventOffset - EVENT_CARD_GAP);
    }

    private String buildEventSubHeader(ExecutionEvent event) {
        String stepText = event.getStepIndex() >= 0 ? ("步骤 " + event.getStepIndex()) : "全局";
        String actionText = event.getActionIndex() >= 0 ? ("动作 " + event.getActionIndex()) : "无动作";
        return stepText + "  ·  " + actionText;
    }

    private int getEventAccentColor(ExecutionEvent event) {
        String type = safe(event.getType()).toUpperCase(Locale.ROOT);
        if (type.contains("FINISH")) {
            return 0xFF2FAF5E;
        }
        if (type.contains("START")) {
            return 0xFF4D9DE0;
        }
        if (type.contains("ERROR") || type.contains("FAIL")) {
            return 0xFFB64A4A;
        }
        if (type.contains("WARN")) {
            return 0xFFE0A100;
        }
        return 0xFF56B6E8;
    }

    private void drawPixelScrollbar(int x, int y, int width, int height, int scroll, int maxScroll, int contentHeight) {
        if (maxScroll <= 0 || contentHeight <= 0) {
            return;
        }
        int thumbHeight = Math.max(18, (int) ((float) Math.max(1, height - 8) / Math.max(height - 8, contentHeight)
                * height));
        int thumbY = y + (int) ((float) scroll / maxScroll * Math.max(1, height - thumbHeight));
        GuiTheme.drawScrollbar(x, y, width, height, thumbY, thumbHeight);
    }

    private int drawWrappedLines(List<String> lines, int x, int startY, int color) {
        int y = startY;
        for (String line : lines) {
            this.drawString(this.fontRenderer, line, x, y, color);
            y += 10;
        }
        return y;
    }

    private List<String> buildWrappedLines(String text, int width) {
        List<String> wrapped = this.fontRenderer.listFormattedStringToWidth(safe(text), Math.max(40, width));
        if (wrapped == null || wrapped.isEmpty()) {
            wrapped = new ArrayList<>();
            wrapped.add(safe(text));
        }
        return wrapped;
    }

    private void beginScissor(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int scale = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + h)) * scale, w * scale, h * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int visibleSessionRows() {
        return Math.max(1, sessionListH / SESSION_ROW_HEIGHT);
    }

    private void recalcEventScroll() {
        variableScroll = 0;
        eventScroll = 0;
        maxVariableScroll = 0;
        maxEventScroll = 0;
        variableContentHeight = 0;
        eventContentHeight = 0;
        detailCacheSessionId = "";
        detailCacheVariableWidth = -1;
        detailCacheEventWidth = -1;
        variableCards.clear();
        eventCards.clear();
    }

    private SessionSnapshot getSelectedSession() {
        return selectedIndex >= 0 && selectedIndex < sessions.size() ? sessions.get(selectedIndex) : null;
    }

    private void copySelectedSession() {
        SessionSnapshot selected = getSelectedSession();
        if (selected == null) {
            setFooter("请先选择一条执行日志");
            return;
        }
        String content = ExecutionLogManager.getSessionText(selected.getSessionId());
        if (content.isEmpty()) {
            setFooter("复制失败，未找到选中的日志内容");
            return;
        }
        setClipboardString(content);
        setFooter("已复制当前选中的执行日志");
    }

    private void updateButtonStates() {
        boolean hasSelected = getSelectedSession() != null;
        boolean hasSessions = !sessions.isEmpty();
        setButtonEnabled(2, hasSelected);
        setButtonEnabled(3, hasSelected);
        setButtonEnabled(4, hasSessions);
    }

    private void setButtonEnabled(int buttonId, boolean enabled) {
        for (GuiButton button : this.buttonList) {
            if (button.id == buttonId) {
                button.enabled = enabled;
                return;
            }
        }
    }

    private int getSessionIndexAt(int mouseX, int mouseY) {
        int visibleRows = visibleSessionRows();
        int end = Math.min(sessions.size(), sessionScroll + visibleRows);
        int cardX = sessionListX + 6;
        int cardW = sessionListW - 18;
        int y = sessionListY + 6;
        for (int i = sessionScroll; i < end; i++) {
            if (isInside(mouseX, mouseY, cardX, y, cardW, SESSION_CARD_HEIGHT)) {
                return i;
            }
            y += SESSION_ROW_HEIGHT;
        }
        return -1;
    }

    private int getSessionAccentColor(SessionSnapshot session) {
        if (!session.isFinished()) {
            return 0xFFE0A100;
        }
        return session.isSuccess() ? 0xFF2FAF5E : 0xFFB64A4A;
    }

    private String getSessionTitleColor(SessionSnapshot session) {
        if (!session.isFinished()) {
            return "§e";
        }
        return session.isSuccess() ? "§a" : "§c";
    }

    private String getSessionResultText(SessionSnapshot session) {
        if (session == null || !session.isFinished()) {
            return "运行中";
        }
        return session.isSuccess() ? "成功" : "停止";
    }

    private String buildSessionCardDetail(SessionSnapshot session) {
        String statusText = safe(session.getFinishReason());
        if (statusText.isEmpty()) {
            statusText = safe(session.getFinalStatus());
        }
        if (statusText.isEmpty()) {
            statusText = session.isFinished()
                    ? ("耗时 " + session.getDurationMs() + " ms")
                    : ("已运行 " + session.getDurationMs() + " ms");
        } else {
            statusText = "耗时 " + session.getDurationMs() + " ms  ·  " + statusText;
        }
        return "ID " + shortSessionId(session.getSessionId()) + "  ·  " + statusText;
    }

    private String shortSessionId(String sessionId) {
        String safeId = safe(sessionId);
        return safeId.length() <= 8 ? safeId : safeId.substring(0, 8);
    }

    private void setFooter(String message) {
        this.footerMessage = message == null ? "" : message;
        this.footerMessageUntil = System.currentTimeMillis() + 4000L;
    }

    private String joinVariables(Map<String, String> values) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(" ; ", parts);
    }

    private String trim(String value, int width) {
        return this.fontRenderer.trimStringToWidth(value == null ? "" : value, Math.max(20, width));
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
