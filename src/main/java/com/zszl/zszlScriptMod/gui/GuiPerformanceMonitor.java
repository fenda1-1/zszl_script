package com.zszl.zszlScriptMod.gui;

import com.zszl.zszlScriptMod.PerformanceMonitor;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 主题化性能监控面板
 */
public class GuiPerformanceMonitor extends ThemedGuiScreen {

    private static final int BTN_RESET_ALL = 10000;
    private static final int BTN_BACK = 10001;
    private static final int BTN_SPIKE_GUARD = 10002;
    private static final int BTN_APPLY_GUARD = 10003;

    private static final int PANEL_MARGIN = 15;
    private static final int PANEL_TOP = 10;
    private static final int CARD_HEIGHT = 132;
    private static final int CARD_MARGIN = 10;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int CONTENT_PADDING = 16;
    private static final int CONTENT_TOP = 82;
    private static final int MIN_CARD_WIDTH = 260;

    private static final List<String> FEATURE_ORDER = Arrays.asList(
            "auto_equip",
            "auto_pickup",
            "auto_follow",
            "path_sequence",
            "conditional_execution",
            "debuff_detector",
            "freecam",
            "goto_open",
            "warehouse",
            "packet_capture_inbound",
            "packet_capture_outbound",
            "packet_intercept",
            "packet_send_fml",
            "packet_send_standard");

    private final Minecraft mc = Minecraft.getInstance();
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private GuiTextField thresholdField;
    private GuiTextField disableDurationField;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();

        int panelWidth = getPanelWidth();
        int panelX = getPanelX();
        int bottomY = getButtonY();
        int settingsY = PANEL_TOP + 38;

        this.buttonList.add(new GuiButton(BTN_RESET_ALL, panelX, bottomY, 120, 20,
                I18n.format("gui.performance.reset_all")));
        this.buttonList.add(new GuiButton(BTN_BACK, panelX + panelWidth - 120, bottomY, 120, 20,
                I18n.format("gui.common.back")));
        this.buttonList.add(new GuiButton(BTN_SPIKE_GUARD, panelX, settingsY, 110, 20,
                I18n.format("gui.performance.guard_toggle",
                        PerformanceMonitor.isSpikeGuardEnabled() ? "§aON" : "§cOFF")));
        this.buttonList.add(new GuiButton(BTN_APPLY_GUARD, panelX + panelWidth - 80, settingsY, 80, 20,
                I18n.format("gui.theme.apply_selected")));

        thresholdField = new GuiTextField(20000, this.fontRenderer, panelX + 150, settingsY, 70, 20);
        thresholdField.setMaxStringLength(Integer.MAX_VALUE);
        thresholdField.setText(String.format("%.1f", PerformanceMonitor.getSpikeThresholdMillis()));

        disableDurationField = new GuiTextField(20001, this.fontRenderer, panelX + 290, settingsY, 70, 20);
        disableDurationField.setMaxStringLength(Integer.MAX_VALUE);
        disableDurationField.setText(String.valueOf(PerformanceMonitor.getSpikeDisableDurationMs()));

        updateScrollLimits();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateScrollLimits();
        drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = PANEL_TOP;
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.performance.title"), this.fontRenderer);
        drawCenteredString(this.fontRenderer, I18n.format("gui.performance.subtitle"), this.width / 2, panelY + 24,
                GuiTheme.SUB_TEXT);
        drawGuardControls(panelX, panelWidth);

        startScissor(getContentAreaX(), getContentAreaY(), getContentAreaWidth(), getVisibleContentHeight());
        drawFeatureCards(mouseX, mouseY);
        endScissor();

        if (maxScrollOffset > 0) {
            drawScrollbar();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawFeatureCards(int mouseX, int mouseY) {
        List<String> featureNames = getFeatureNames();

        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.get(i);
            CardBounds bounds = getCardBounds(i);

            if (bounds.y + CARD_HEIGHT >= getContentAreaY()
                    && bounds.y <= getContentAreaY() + getVisibleContentHeight()) {
                drawFeatureCard(bounds.x, bounds.y, bounds.width, featureName, mouseX, mouseY);
            }
        }
    }

    private void drawFeatureCard(int x, int y, int cardWidth, String featureName, int mouseX, int mouseY) {
        boolean isEnabled = PerformanceMonitor.isFeatureEnabled(featureName);
        boolean manuallyEnabled = PerformanceMonitor.isFeatureManuallyEnabled(featureName);
        long tempDisabledRemaining = PerformanceMonitor.getTemporaryDisableRemainingMs(featureName);
        PerformanceMonitor.PerformanceStats stats = PerformanceMonitor.getPerformanceStats(featureName);

        GuiTheme.drawPanel(x, y, cardWidth, CARD_HEIGHT);
        GuiTheme.drawCardHighlight(x, y, cardWidth, CARD_HEIGHT,
                mouseX >= x && mouseX <= x + cardWidth && mouseY >= y && mouseY <= y + CARD_HEIGHT);

        drawString(this.fontRenderer, getDisplayName(featureName), x + 10, y + 10, GuiTheme.LABEL_TEXT);

        String status;
        int statusColor;
        if (!manuallyEnabled) {
            status = I18n.format("gui.common.disabled");
            statusColor = GuiTheme.STATE_DANGER;
        } else if (tempDisabledRemaining > 0L) {
            status = I18n.format("gui.performance.temp_disabled", tempDisabledRemaining);
            statusColor = GuiTheme.STATE_WARNING;
        } else {
            status = I18n.format("gui.common.enabled");
            statusColor = GuiTheme.STATE_SUCCESS;
        }
        drawString(this.fontRenderer, I18n.format("gui.performance.status", status), x + 10, y + 25, statusColor);

        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonX = x + cardWidth - buttonWidth - 10;
        int buttonY = y + 8;
        boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        GuiTheme.drawButtonFrameSafe(buttonX, buttonY, buttonWidth, buttonHeight,
                buttonHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);

        String buttonText = manuallyEnabled ? I18n.format("gui.performance.disable")
                : I18n.format("gui.performance.enable");
        int textX = buttonX + (buttonWidth - this.fontRenderer.getStringWidth(buttonText)) / 2;
        int textY = buttonY + (buttonHeight - 8) / 2;
        drawString(this.fontRenderer, buttonText, textX, textY, GuiTheme.LABEL_TEXT);

        if (stats != null && stats.getMeasurementCount() > 0) {
            drawString(this.fontRenderer, I18n.format("gui.performance.stats"), x + 10, y + 46, GuiTheme.TITLE_RIGHT);
            drawString(this.fontRenderer,
                    I18n.format("gui.performance.avg", stats.getAverageTimeMillis()),
                    x + 10, y + 61, GuiTheme.LABEL_TEXT);
            drawString(this.fontRenderer, I18n.format("gui.performance.max", stats.getMaxTimeMillis()),
                    x + 10, y + 76, GuiTheme.LABEL_TEXT);
            drawString(this.fontRenderer, I18n.format("gui.performance.min", stats.getMinTimeMillis()),
                    x + 10, y + 91, GuiTheme.LABEL_TEXT);
            drawString(this.fontRenderer,
                    I18n.format("gui.performance.count", stats.getMeasurementCount()),
                    x + 10, y + 106, GuiTheme.SUB_TEXT);

            if (featureName.startsWith("packet_")) {
                PacketCaptureHandler.PacketCaptureUiSnapshot snapshot = PacketCaptureHandler.getUiSnapshot();
                String extra = "队列:" + snapshot.queueSize + " 丢弃:" + snapshot.droppedCount
                        + " 采样:1/" + Math.max(1, snapshot.samplingModulo);
                drawString(this.fontRenderer, extra, x + 10, y + 118, GuiTheme.SUB_TEXT);
            }
        } else {
            drawString(this.fontRenderer, I18n.format("gui.performance.no_data"), x + 10, y + 68, GuiTheme.SUB_TEXT);
            if (featureName.startsWith("packet_")) {
                PacketCaptureHandler.PacketCaptureUiSnapshot snapshot = PacketCaptureHandler.getUiSnapshot();
                String extra = "队列:" + snapshot.queueSize + " 丢弃:" + snapshot.droppedCount
                        + " 采样:1/" + Math.max(1, snapshot.samplingModulo);
                drawString(this.fontRenderer, extra, x + 10, y + 83, GuiTheme.SUB_TEXT);
            }
        }
    }

    private void drawScrollbar() {
        GuiTheme.drawScrollbar(getScrollbarX(), getContentAreaY(), SCROLLBAR_WIDTH, getVisibleContentHeight(),
                getScrollbarHandleY(), getScrollbarHandleHeight());
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) {
            return;
        }
        if (button.id == BTN_RESET_ALL) {
            for (String feature : getFeatureNames()) {
                PerformanceMonitor.resetPerformanceStats(feature);
            }
        } else if (button.id == BTN_BACK) {
            mc.setScreen(null);
        } else if (button.id == BTN_SPIKE_GUARD) {
            PerformanceMonitor.setSpikeGuardEnabled(!PerformanceMonitor.isSpikeGuardEnabled());
            initGui();
        } else if (button.id == BTN_APPLY_GUARD) {
            applyGuardSettings();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingScrollbar = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int scrollAmount = scroll > 0 ? -20 : 20;
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + scrollAmount));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (maxScrollOffset > 0) {
            int scrollbarX = getScrollbarX();
            int scrollbarY = getContentAreaY();
            int scrollbarHeight = getVisibleContentHeight();
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
                    && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                isDraggingScrollbar = true;
                updateScrollFromMouse(mouseY);
                return;
            }
        }

        if (thresholdField != null) {
            thresholdField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (disableDurationField != null) {
            disableDurationField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        List<String> featureNames = getFeatureNames();

        for (int i = 0; i < featureNames.size(); i++) {
            String featureName = featureNames.get(i);
            CardBounds bounds = getCardBounds(i);
            if (bounds.y + CARD_HEIGHT < getContentAreaY()
                    || bounds.y > getContentAreaY() + getVisibleContentHeight()) {
                continue;
            }
            int buttonX = bounds.x + bounds.width - 70;
            int buttonY = bounds.y + 8;

            if (mouseX >= buttonX && mouseX <= buttonX + 60 && mouseY >= buttonY && mouseY <= buttonY + 20) {
                PerformanceMonitor.setFeatureEnabled(featureName,
                        !PerformanceMonitor.isFeatureManuallyEnabled(featureName));
                return;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (isDraggingScrollbar && clickedMouseButton == 0) {
            updateScrollFromMouse(mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.setScreen(null);
            return;
        }
        if (thresholdField != null && thresholdField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (disableDurationField != null && disableDurationField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (thresholdField != null) {
            thresholdField.updateCursorCounter();
        }
        if (disableDurationField != null) {
            disableDurationField.updateCursorCounter();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int getPanelWidth() {
        return Math.min(760, this.width - PANEL_MARGIN * 2);
    }

    private int getPanelHeight() {
        return this.height - PANEL_TOP - PANEL_MARGIN;
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getButtonY() {
        return PANEL_TOP + getPanelHeight() - 30;
    }

    private int getContentAreaX() {
        return getPanelX() + CONTENT_PADDING;
    }

    private int getContentAreaY() {
        return PANEL_TOP + CONTENT_TOP;
    }

    private int getScrollbarX() {
        return getPanelX() + getPanelWidth() - CONTENT_PADDING - SCROLLBAR_WIDTH;
    }

    private int getContentAreaWidth() {
        return getScrollbarX() - getContentAreaX() - 8;
    }

    private int getVisibleContentHeight() {
        return getButtonY() - getContentAreaY() - 8;
    }

    private int getColumnCount() {
        return getContentAreaWidth() >= MIN_CARD_WIDTH * 2 + CARD_MARGIN ? 2 : 1;
    }

    private int getCardWidth() {
        int columns = getColumnCount();
        return (getContentAreaWidth() - CARD_MARGIN * (columns - 1)) / columns;
    }

    private int getTotalRows() {
        int totalCards = getFeatureNames().size();
        int columns = getColumnCount();
        return (int) Math.ceil(totalCards / (double) columns);
    }

    private void updateScrollLimits() {
        int rows = getTotalRows();
        int totalHeight = rows <= 0 ? 0 : rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_MARGIN;
        maxScrollOffset = Math.max(0, totalHeight - getVisibleContentHeight());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }

    private int getScrollbarHandleHeight() {
        int visibleHeight = getVisibleContentHeight();
        int rows = getTotalRows();
        int totalHeight = rows <= 0 ? visibleHeight : rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_MARGIN;
        if (totalHeight <= 0) {
            return visibleHeight;
        }
        return Math.max(20, (int) ((visibleHeight / (float) totalHeight) * visibleHeight));
    }

    private int getScrollbarHandleY() {
        if (maxScrollOffset <= 0) {
            return getContentAreaY();
        }
        int trackHeight = getVisibleContentHeight() - getScrollbarHandleHeight();
        return getContentAreaY() + Math.round((scrollOffset / (float) maxScrollOffset) * trackHeight);
    }

    private void updateScrollFromMouse(int mouseY) {
        int handleHeight = getScrollbarHandleHeight();
        int travel = Math.max(1, getVisibleContentHeight() - handleHeight);
        int target = mouseY - getContentAreaY() - handleHeight / 2;
        target = Math.max(0, Math.min(travel, target));
        scrollOffset = Math.max(0, Math.min(maxScrollOffset,
                Math.round((target / (float) travel) * maxScrollOffset)));
    }

    private CardBounds getCardBounds(int index) {
        int columns = getColumnCount();
        int cardWidth = getCardWidth();
        int row = index / columns;
        int col = index % columns;
        int x = getContentAreaX() + col * (cardWidth + CARD_MARGIN);
        int y = getContentAreaY() + row * (CARD_HEIGHT + CARD_MARGIN) - scrollOffset;
        return new CardBounds(x, y, cardWidth);
    }

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(this.mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (this.height - (y + height)) * scale, width * scale, height * scale);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawGuardControls(int panelX, int panelWidth) {
        int settingsY = PANEL_TOP + 38;
        drawString(this.fontRenderer, I18n.format("gui.performance.guard_threshold"), panelX + 118, settingsY + 6,
                GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer, I18n.format("gui.performance.guard_disable_ms"), panelX + 228, settingsY + 6,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(thresholdField);
        drawThemedTextField(disableDurationField);
        drawString(this.fontRenderer, I18n.format("gui.performance.guard_hint"), panelX, settingsY + 26,
                GuiTheme.SUB_TEXT);
    }

    private void applyGuardSettings() {
        try {
            double thresholdMillis = Double.parseDouble(thresholdField.getText().trim());
            long disableMillis = Long.parseLong(disableDurationField.getText().trim());
            PerformanceMonitor.setSpikeThresholdMillis(thresholdMillis);
            PerformanceMonitor.setSpikeDisableDurationMs(disableMillis);
            initGui();
        } catch (NumberFormatException ignored) {
        }
    }

    private static class CardBounds {
        private final int x;
        private final int y;
        private final int width;

        private CardBounds(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    private List<String> getFeatureNames() {
        Map<String, Boolean> features = PerformanceMonitor.getAllFeatureStates();
        List<String> ordered = new ArrayList<>();
        for (String feature : FEATURE_ORDER) {
            if (features.containsKey(feature)) {
                ordered.add(feature);
            }
        }
        for (String feature : features.keySet()) {
            if (!ordered.contains(feature)) {
                ordered.add(feature);
            }
        }
        return ordered;
    }

    private String getDisplayName(String featureName) {
        switch (featureName) {
            case "auto_equip":
                return "自动穿戴 - Auto Equip";
            case "auto_pickup":
                return "自动拾取 - Auto Pickup";
            case "auto_follow":
                return "自动追怪 - Auto Follow";
            case "path_sequence":
                return "路径序列 - Path Sequence";
            case "conditional_execution":
                return "条件执行 - Conditional Exec";
            case "debuff_detector":
                return "Debuff检测器 - Debuff Detector";
            case "freecam":
                return "自由视角 - Freecam";
            case "goto_open":
                return "前往并打开 - Go To & Open";
            case "warehouse":
                return "仓库系统 - Warehouse";
            case "packet_capture_inbound":
                return "入站捕获 - Inbound Capture";
            case "packet_capture_outbound":
                return "出站捕获 - Outbound Capture";
            case "packet_intercept":
                return "数据包拦截 - Packet Intercept";
            case "packet_send_fml":
                return "FML包发送 - FML Packet Send";
            case "packet_send_standard":
                return "标准包发送 - Standard Packet Send";
            default:
                return featureName;
        }
    }
}






