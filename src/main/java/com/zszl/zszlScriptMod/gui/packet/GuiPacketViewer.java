package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.mojang.blaze3d.platform.GlStateManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class GuiPacketViewer extends ThemedGuiScreen {
    private enum DisplayMode { SENT, RECEIVED }
    private enum InputFilterMode {
        ALL("全部"), KEYBOARD("键盘"), MOUSE("鼠标"), LEFT("左键"), RIGHT("右键"), MIDDLE("中键");
        private final String label;
        InputFilterMode(String label) { this.label = label; }
    }
    private enum TimeWindowMode {
        ALL("全时窗", -1L),
        MS50("±50ms", 50L),
        MS100("±100ms", 100L),
        MS200("±200ms", 200L),
        MS500("±500ms", 500L);
        private final String label;
        private final long windowMs;
        TimeWindowMode(String label, long windowMs) { this.label = label; this.windowMs = windowMs; }
    }
    private enum PacketTextScaleMode {
        LARGE("大", 1.0f),
        MEDIUM("中", 0.85f),
        SMALL("小", 0.72f);
        private final String label;
        private final float scale;
        PacketTextScaleMode(String label, float scale) { this.label = label; this.scale = scale; }
    }
    private static final class InputTreeRow {
        private final boolean groupRow;
        private final int sessionId;
        private final String label;
        private final InputTimelineManager.InputEventRecord event;

        private InputTreeRow(boolean groupRow, int sessionId, String label, InputTimelineManager.InputEventRecord event) {
            this.groupRow = groupRow;
            this.sessionId = sessionId;
            this.label = label == null ? "" : label;
            this.event = event;
        }
    }
    private final class SimpleDropdown {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String label;
        private final String[] options;
        private int selectedIndex;
        private boolean expanded;

        private SimpleDropdown(int x, int y, int width, int height, String label, String[] options, int selectedIndex) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.options = options == null ? new String[0] : options;
            this.selectedIndex = Math.max(0, Math.min(selectedIndex, Math.max(0, this.options.length - 1)));
        }

        private void draw(int mouseX, int mouseY) {
            boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
            GuiTheme.drawButtonFrameSafe(x, y, width, height,
                    hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(label + ":" + getSelectedText(), width - 14),
                    x + 6, y + 5, 0xFFFFFFFF);
            drawString(fontRenderer, expanded ? "▲" : "▼", x + width - 10, y + 5, 0xFF9FDFFF);
            if (!expanded) {
                return;
            }
            int itemHeight = 18;
            int totalHeight = itemHeight * options.length;
            drawRect(x, y + height + 2, x + width, y + height + 2 + totalHeight, 0xEE111A22);
            drawHorizontalLine(x, x + width, y + height + 2, 0xFF6FB8FF);
            drawHorizontalLine(x, x + width, y + height + 2 + totalHeight, 0xFF35536C);
            drawVerticalLine(x, y + height + 2, y + height + 2 + totalHeight, 0xFF35536C);
            drawVerticalLine(x + width, y + height + 2, y + height + 2 + totalHeight, 0xFF35536C);
            for (int i = 0; i < options.length; i++) {
                int itemY = y + height + 2 + i * itemHeight;
                boolean itemHovered = isInside(mouseX, mouseY, x, itemY, width, itemHeight);
                boolean selected = i == selectedIndex;
                if (selected || itemHovered) {
                    drawRect(x + 1, itemY, x + width - 1, itemY + itemHeight, selected ? 0xCC2B5A7C : 0xAA2E4258);
                }
                drawString(fontRenderer, options[i], x + 6, itemY + 5, 0xFFFFFFFF);
            }
        }

        private boolean handleClick(int mouseX, int mouseY) {
            if (isInside(mouseX, mouseY, x, y, width, height)) {
                expanded = !expanded;
                return true;
            }
            if (!expanded) {
                return false;
            }
            int itemHeight = 18;
            int totalHeight = itemHeight * options.length;
            if (isInside(mouseX, mouseY, x, y + height + 2, width, totalHeight)) {
                int index = (mouseY - (y + height + 2)) / itemHeight;
                if (index >= 0 && index < options.length) {
                    selectedIndex = index;
                }
                expanded = false;
                return true;
            }
            expanded = false;
            return false;
        }

        private void collapse() { expanded = false; }
        private String getSelectedText() { return options.length == 0 ? "" : options[selectedIndex]; }
        private int getSelectedIndex() { return selectedIndex; }
        private boolean isExpanded() { return expanded; }
    }

    private final GuiScreen parentScreen;
    private final List<PacketCaptureHandler.CapturedPacketData> fixedSourcePackets;
    private final String customTitle;
    private final boolean readOnlyMode;

    private List<PacketCaptureHandler.CapturedPacketData> filteredPackets = new ArrayList<>();
    private List<InputTimelineManager.InputEventRecord> inputEvents = new ArrayList<>();
    private List<InputTimelineManager.InputEventRecord> visibleInputEvents = new ArrayList<>();
    private List<InputTreeRow> inputTreeRows = new ArrayList<>();
    private List<Integer> packetViewIndexes = new ArrayList<>();
    private final Set<Integer> selectedIndices = new HashSet<>();
    private final Set<Integer> collapsedInputSessions = new HashSet<>();
    private int lastSelectedIndex = -1;
    private int selectedInputIndex = -1;
    private int selectedInputSessionId = -1;

    private GuiTextField filterField;
    private GuiButton sendButton, copyButton, btnToggleMode, saveSnapshotButton, historyButton, idRecordButton,
            smartGeneratorButton,
            clearInputButton, inputFilterButton, inputFreezeButton, timeWindowButton,
            packetColumnsButton, packetTextScaleButton, packetHintToggleButton;
    private DisplayMode currentMode = DisplayMode.SENT;
    private InputFilterMode inputFilterMode = InputFilterMode.ALL;
    private TimeWindowMode timeWindowMode = TimeWindowMode.ALL;
    private boolean freezeInputTimeline = false;
    private int packetColumns = 1;
    private PacketTextScaleMode packetTextScaleMode = PacketTextScaleMode.MEDIUM;
    private boolean hidePacketHintLine = false;

    private int panelX, panelY, panelWidth, panelHeight;
    private int filterX, filterY, filterWidth;
    private int leftX, leftY, leftWidth, leftHeight;
    private int rightX, rightY, rightWidth, rightHeight;
    private int inputListY, inputListHeight;
    private int footerY;
    private int inputScrollOffset = 0, inputMaxScroll = 0;
    private int packetScrollOffset = 0, packetMaxScroll = 0;
    private int inputHorizontalScrollOffset = 0, inputHorizontalMaxScroll = 0;
    private String activeFilterText = "";
    private boolean preserveViewStateOnNextInit = false;
    private boolean draggingInputScrollbar = false;
    private boolean draggingPacketScrollbar = false;
    private boolean draggingInputHorizontalScrollbar = false;
    private SimpleDropdown inputFilterDropdown;
    private SimpleDropdown timeWindowDropdown;
    private SimpleDropdown packetColumnsDropdown;
    private SimpleDropdown packetTextScaleDropdown;
    private SimpleDropdown packetHintDropdown;

    private static final int INPUT_ITEM_HEIGHT = 22;
    private static final int PACKET_ITEM_HEIGHT = 76;

    public GuiPacketViewer(GuiScreen parent) {
        this.parentScreen = parent;
        this.fixedSourcePackets = null;
        this.customTitle = null;
        this.readOnlyMode = false;
    }

    public GuiPacketViewer(GuiScreen parent, List<PacketCaptureHandler.CapturedPacketData> packets, String customTitle, boolean readOnlyMode) {
        this.parentScreen = parent;
        this.fixedSourcePackets = packets == null ? new ArrayList<>() : new ArrayList<>(packets);
        this.customTitle = customTitle;
        this.readOnlyMode = readOnlyMode;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        recalcLayout();
        if (fixedSourcePackets == null
                && currentMode == DisplayMode.SENT
                && PacketCaptureHandler.capturedPackets.isEmpty()
                && !PacketCaptureHandler.capturedReceivedPackets.isEmpty()) {
            currentMode = DisplayMode.RECEIVED;
        }
        String oldFilter = filterField == null ? "" : filterField.getText();
        applyFilter(oldFilter, preserveViewStateOnNextInit);
        preserveViewStateOnNextInit = false;

        this.buttonList.clear();
        filterField = new GuiTextField(0, fontRenderer, filterX, filterY, filterWidth, 20);
        filterField.setMaxStringLength(Integer.MAX_VALUE);
        filterField.setFocused(true);
        filterField.setText(oldFilter);

        int gap = 6;
        int buttonCount = 8;
        int btnW = Math.max(52, Math.min(104, (panelWidth - 20 - gap * (buttonCount - 1)) / buttonCount));
        int rowX = panelX + Math.max(10, (panelWidth - (btnW * buttonCount + gap * (buttonCount - 1))) / 2);
        this.buttonList.add(new ThemedButton(1, rowX, footerY, btnW, 20, btnW < 76 ? "返回" : I18n.format("gui.common.back")));
        btnToggleMode = new ThemedButton(3, rowX + 1 * (btnW + gap), footerY, btnW, 20, "");
        copyButton = new ThemedButton(2, rowX + 2 * (btnW + gap), footerY, btnW, 20, btnW < 76 ? "复制" : "复制选中");
        sendButton = new ThemedButton(0, rowX + 3 * (btnW + gap), footerY, btnW, 20, "");
        saveSnapshotButton = new ThemedButton(4, rowX + 4 * (btnW + gap), footerY, btnW, 20, btnW < 76 ? "快照" : "保存快照");
        historyButton = new ThemedButton(5, rowX + 5 * (btnW + gap), footerY, btnW, 20, "历史");
        smartGeneratorButton = new ThemedButton(10, rowX + 6 * (btnW + gap), footerY, btnW, 20,
                btnW < 84 ? "生成器" : "智能生成器");
        clearInputButton = new ThemedButton(7, rowX + 7 * (btnW + gap), footerY, btnW, 20, btnW < 76 ? "清空" : "清空键鼠");

        inputFreezeButton = new ThemedButton(9, leftX + 6, leftY + 4, 56, 18, "");
        inputFilterDropdown = new SimpleDropdown(leftX + 66, leftY + 4, Math.max(86, leftWidth - 72), 18, "筛选",
                buildInputFilterLabels(), inputFilterMode.ordinal());

        int dropdownY = filterY;
        int dropdownGap = 6;
        int rightPad = 6;
        int ddW1 = 72;
        int ddW2 = 72;
        int ddW3 = 72;
        int ddW4 = 72;
        int ddW5 = 72;
        int totalDdWidth = ddW1 + ddW2 + ddW3 + ddW4 + ddW5 + dropdownGap * 4;
        int ddStartX = rightX + Math.max(rightPad, rightWidth - totalDdWidth - rightPad);
        timeWindowDropdown = new SimpleDropdown(ddStartX, dropdownY, ddW1, 18, "时间窗", buildTimeWindowLabels(),
                timeWindowMode.ordinal());
        packetColumnsDropdown = new SimpleDropdown(ddStartX + ddW1 + dropdownGap, dropdownY, ddW2, 18, "列", buildPacketColumnLabels(),
                Math.max(0, Math.min(1, packetColumns - 1)));
        packetTextScaleDropdown = new SimpleDropdown(ddStartX + ddW1 + ddW2 + dropdownGap * 2, dropdownY, ddW3, 18, "字", buildPacketTextScaleLabels(),
                packetTextScaleMode.ordinal());
        packetHintDropdown = new SimpleDropdown(ddStartX + ddW1 + ddW2 + ddW3 + dropdownGap * 3, dropdownY, ddW4, 18, "提示",
                new String[] { "显示", "隐藏" }, hidePacketHintLine ? 1 : 0);
        this.buttonList.add(new ThemedButton(6, ddStartX + ddW1 + ddW2 + ddW3 + ddW4 + dropdownGap * 4, dropdownY, ddW5, 18, "ID记录"));
        idRecordButton = this.buttonList.get(this.buttonList.size() - 1);
        this.buttonList.add(btnToggleMode);
        this.buttonList.add(copyButton);
        this.buttonList.add(sendButton);
        this.buttonList.add(saveSnapshotButton);
        this.buttonList.add(historyButton);
        this.buttonList.add(smartGeneratorButton);
        this.buttonList.add(clearInputButton);
        this.buttonList.add(inputFreezeButton);
        updateButtonStates();
    }

    private void recalcLayout() {
        panelX = 12; panelY = 10; panelWidth = this.width - 24; panelHeight = this.height - 20;
        filterX = panelX + 10; filterY = panelY + 24;
        footerY = panelY + panelHeight - 28;
        leftX = panelX + 10; leftY = filterY + 26; leftWidth = Math.max(180, Math.min(280, panelWidth / 4));
        rightX = leftX + leftWidth + 10; rightY = leftY; rightWidth = panelX + panelWidth - rightX - 10;
        rightHeight = footerY - rightY - 8;
        leftHeight = rightHeight;
        filterWidth = leftWidth;
        inputListY = leftY + 24;
        int footerBoxH = 20;
        int footerBoxGap = 2;
        inputListHeight = Math.max(40, leftHeight - 24 - footerBoxGap - footerBoxH);
    }

    private void updateButtonStates() {
        if (btnToggleMode != null) {
            btnToggleMode.displayString = currentMode == DisplayMode.SENT
                    ? I18n.format("gui.packet.viewer.mode", "§a" + I18n.format("gui.packet.viewer.sent"))
                    : I18n.format("gui.packet.viewer.mode", "§b" + I18n.format("gui.packet.viewer.received"));
            btnToggleMode.enabled = !readOnlyMode;
        }
        boolean hasSelection = !selectedIndices.isEmpty();
        if (sendButton != null) { sendButton.enabled = hasSelection; sendButton.displayString = I18n.format("gui.packet.viewer.send_mock", selectedIndices.size()); }
        if (copyButton != null) copyButton.enabled = hasSelection;
        if (saveSnapshotButton != null) saveSnapshotButton.enabled = hasSelection;
        if (smartGeneratorButton != null) smartGeneratorButton.enabled = hasSelection;
        if (inputFreezeButton != null) inputFreezeButton.displayString = freezeInputTimeline ? "冻结:开" : "冻结:关";
    }

    private void applyFilter(String filterText, boolean preserve) {
        List<PacketCaptureHandler.CapturedPacketData> source = fixedSourcePackets != null
                ? fixedSourcePackets
                : (currentMode == DisplayMode.SENT ? PacketCaptureHandler.capturedPackets : PacketCaptureHandler.capturedReceivedPackets);
        String normalized = filterText == null ? "" : filterText.trim();
        activeFilterText = normalized;
        if (normalized.isEmpty()) {
            filteredPackets = new ArrayList<>(source);
        } else {
            final List<String> keywords = Arrays.stream(normalized.split("[,\\s]+")).filter(s -> !s.trim().isEmpty()).collect(Collectors.toList());
            filteredPackets = source.stream().filter(packet -> {
                for (String keyword : keywords) {
                    if (!packetMatchesFilter(packet, keyword)) return false;
                }
                return true;
            }).collect(Collectors.toList());
        }
        if (!freezeInputTimeline || inputEvents.isEmpty()) {
            inputEvents = InputTimelineManager.getEventsSnapshot();
        }
        visibleInputEvents = filterInputEvents(inputEvents);
        rebuildInputTreeRows();
        rebuildPacketViewIndexes();
        if (!preserve) {
            selectedIndices.clear();
            lastSelectedIndex = -1;
            selectedInputIndex = -1;
            selectedInputSessionId = -1;
            inputScrollOffset = 0;
            packetScrollOffset = 0;
        }
        inputMaxScroll = Math.max(0, inputTreeRows.size() - getVisibleInputCount());
        packetMaxScroll = Math.max(0, packetViewIndexes.size() - getVisiblePacketCount());
        inputScrollOffset = Math.max(0, Math.min(inputScrollOffset, inputMaxScroll));
        packetScrollOffset = Math.max(0, Math.min(packetScrollOffset, packetMaxScroll));
    }

    private List<InputTimelineManager.InputEventRecord> filterInputEvents(List<InputTimelineManager.InputEventRecord> source) {
        List<InputTimelineManager.InputEventRecord> filtered = new ArrayList<>();
        if (source == null) {
            return filtered;
        }
        for (InputTimelineManager.InputEventRecord record : source) {
            if (record == null) {
                continue;
            }
            switch (inputFilterMode) {
                case KEYBOARD:
                    if (!"键盘".equals(record.getCategory())) continue;
                    break;
                case MOUSE:
                    if (!"鼠标".equals(record.getCategory())) continue;
                    break;
                case LEFT:
                    if (!"鼠标".equals(record.getCategory()) || !"左键".equals(record.getDetail())) continue;
                    break;
                case RIGHT:
                    if (!"鼠标".equals(record.getCategory()) || !"右键".equals(record.getDetail())) continue;
                    break;
                case MIDDLE:
                    if (!"鼠标".equals(record.getCategory()) || !"中键".equals(record.getDetail())) continue;
                    break;
                case ALL:
                default:
                    break;
            }
            filtered.add(record);
        }
        return filtered;
    }

    private void rebuildInputTreeRows() {
        inputTreeRows.clear();
        int lastSessionId = Integer.MIN_VALUE;
        for (int i = 0; i < visibleInputEvents.size(); i++) {
            InputTimelineManager.InputEventRecord event = visibleInputEvents.get(i);
            if (event == null) {
                continue;
            }
            if (event.getSessionId() != lastSessionId) {
                lastSessionId = event.getSessionId();
                String groupLabel = event.getGuiTitle() + " §7(" + event.getScreenSimpleName() + " #" + event.getSessionId() + ")";
                inputTreeRows.add(new InputTreeRow(true, event.getSessionId(), groupLabel, null));
            }
            if (!collapsedInputSessions.contains(event.getSessionId())) {
                inputTreeRows.add(new InputTreeRow(false, event.getSessionId(), "", event));
            }
        }
    }

    private String[] buildInputFilterLabels() {
        InputFilterMode[] values = InputFilterMode.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = values[i].label;
        return labels;
    }

    private String[] buildTimeWindowLabels() {
        TimeWindowMode[] values = TimeWindowMode.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = values[i].label;
        return labels;
    }

    private String[] buildPacketColumnLabels() {
        return new String[] { "1列", "2列" };
    }

    private String[] buildPacketTextScaleLabels() {
        PacketTextScaleMode[] values = PacketTextScaleMode.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = values[i].label;
        return labels;
    }

    private void collapseAllDropdowns() {
        if (inputFilterDropdown != null) inputFilterDropdown.collapse();
        if (timeWindowDropdown != null) timeWindowDropdown.collapse();
        if (packetColumnsDropdown != null) packetColumnsDropdown.collapse();
        if (packetTextScaleDropdown != null) packetTextScaleDropdown.collapse();
        if (packetHintDropdown != null) packetHintDropdown.collapse();
    }

    private void collapseOtherDropdowns(SimpleDropdown keep) {
        if (inputFilterDropdown != null && inputFilterDropdown != keep) inputFilterDropdown.collapse();
        if (timeWindowDropdown != null && timeWindowDropdown != keep) timeWindowDropdown.collapse();
        if (packetColumnsDropdown != null && packetColumnsDropdown != keep) packetColumnsDropdown.collapse();
        if (packetTextScaleDropdown != null && packetTextScaleDropdown != keep) packetTextScaleDropdown.collapse();
        if (packetHintDropdown != null && packetHintDropdown != keep) packetHintDropdown.collapse();
    }

    private void rebuildPacketViewIndexes() {
        packetViewIndexes.clear();
        if (filteredPackets.isEmpty()) {
            return;
        }
        if (hasActivePacketSearchFilter()) {
            for (int i = 0; i < filteredPackets.size(); i++) {
                packetViewIndexes.add(i);
            }
            return;
        }
        if (timeWindowMode.windowMs < 0) {
            for (int i = 0; i < filteredPackets.size(); i++) {
                packetViewIndexes.add(i);
            }
            return;
        }

        long groupStart = Long.MIN_VALUE;
        long groupEnd = Long.MAX_VALUE;
        if (selectedInputSessionId >= 0) {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (InputTimelineManager.InputEventRecord event : visibleInputEvents) {
                if (event != null && event.getSessionId() == selectedInputSessionId) {
                    min = Math.min(min, event.getTimestamp());
                    max = Math.max(max, event.getTimestamp());
                }
            }
            if (min != Long.MAX_VALUE && max != Long.MIN_VALUE) {
                groupStart = min;
                groupEnd = max;
            }
        }

        if (selectedInputIndex < 0 || selectedInputIndex >= visibleInputEvents.size()) {
            for (int i = 0; i < filteredPackets.size(); i++) {
                long ts = filteredPackets.get(i).getLastTimestamp();
                if (ts >= groupStart && ts <= groupEnd) packetViewIndexes.add(i);
            }
            return;
        }

        long center = visibleInputEvents.get(selectedInputIndex).getTimestamp();
        long window = timeWindowMode.windowMs;
        for (int i = 0; i < filteredPackets.size(); i++) {
            long ts = filteredPackets.get(i).getLastTimestamp();
            long diff = Math.abs(ts - center);
            if (diff <= window && ts >= groupStart && ts <= groupEnd) {
                packetViewIndexes.add(i);
            }
        }
        if (packetViewIndexes.isEmpty()) {
            int nearest = getNearestPacketIndex(center);
            if (nearest >= 0) {
                packetViewIndexes.add(nearest);
            }
        }
    }

    private boolean packetMatchesFilter(PacketCaptureHandler.CapturedPacketData packet, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        String normalizedKeyword = PinyinSearchHelper.normalizeQuery(keyword);
        String normalizedHexKeyword = normalizeHexText(keyword);
        if (isRegexKeyword(keyword)) return packetMatchesRegex(packet, extractRegexPattern(keyword));
        return PinyinSearchHelper.matchesNormalized(packet.packetClassName, normalizedKeyword)
                || PinyinSearchHelper.matchesNormalized(packet.channel, normalizedKeyword)
                || PinyinSearchHelper.matchesNormalized(packet.getDecodedData(), normalizedKeyword)
                || (!normalizedHexKeyword.isEmpty()
                && normalizeHexText(packet.getHexData()).contains(normalizedHexKeyword));
    }

    private boolean hasActivePacketSearchFilter() {
        return activeFilterText != null && !activeFilterText.trim().isEmpty();
    }

    private boolean isRegexKeyword(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        return text.startsWith("re:") || (text.startsWith("/") && text.endsWith("/") && text.length() > 2);
    }

    private String extractRegexPattern(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        if (text.startsWith("re:")) return text.substring(3);
        if (text.startsWith("/") && text.endsWith("/") && text.length() > 2) return text.substring(1, text.length() - 1);
        return text;
    }

    private boolean packetMatchesRegex(PacketCaptureHandler.CapturedPacketData packet, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(packet.packetClassName).find()
                    || pattern.matcher(packet.channel).find()
                    || pattern.matcher(packet.getHexData()).find()
                    || pattern.matcher(packet.getDecodedData()).find();
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    private String normalizeHexText(String text) { return text == null ? "" : text.toLowerCase().replaceAll("[^0-9a-f]", ""); }
    private String safeLower(String text) { return text == null ? "" : text.toLowerCase(); }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: openSequenceEditorForSelection(); break;
            case 1: mc.setScreen(parentScreen); break;
            case 2: copySelectedPackets(); break;
            case 3: if (!readOnlyMode) { currentMode = currentMode == DisplayMode.SENT ? DisplayMode.RECEIVED : DisplayMode.SENT; initGui(); } break;
            case 4: saveSelectedSnapshot(); break;
            case 5: preserveViewStateOnNextInit = true; mc.setScreen(new GuiPacketSnapshotManager(this)); break;
            case 6: preserveViewStateOnNextInit = true; mc.setScreen(new GuiPacketIdRecordViewer(this)); break;
            case 7:
                InputTimelineManager.clear();
                inputEvents = InputTimelineManager.getEventsSnapshot();
                visibleInputEvents = filterInputEvents(inputEvents);
                selectedInputIndex = -1;
                inputScrollOffset = 0;
                break;
            case 10:
                openSmartGeneratorForSelection();
                break;
            case 9:
                freezeInputTimeline = !freezeInputTimeline;
                updateButtonStates();
                break;
            default: break;
        }
    }

    private void openSequenceEditorForSelection() {
        if (selectedIndices.isEmpty()) return;
        preserveViewStateOnNextInit = true;
        List<PacketCaptureHandler.CapturedPacketData> packetsToSend = new ArrayList<>();
        List<Integer> sortedIndices = new ArrayList<>(selectedIndices);
        Collections.sort(sortedIndices);
        for (int index : sortedIndices) if (index >= 0 && index < filteredPackets.size()) packetsToSend.add(filteredPackets.get(index));
        mc.setScreen(new GuiPacketSequenceEditor(this, packetsToSend));
    }

    private void copySelectedPackets() {
        if (selectedIndices.isEmpty()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        StringBuilder sb = new StringBuilder();
        for (int index : new java.util.TreeSet<>(selectedIndices)) {
            if (index < 0 || index >= filteredPackets.size()) continue;
            PacketCaptureHandler.CapturedPacketData p = filteredPackets.get(index);
            sb.append("--- Packet #").append(index + 1).append(" ---\n");
            sb.append(I18n.format("gui.packet.viewer.copy.timestamp")).append(sdf.format(new Date(p.timestamp))).append('\n');
            sb.append(I18n.format("gui.packet.viewer.copy.class")).append(p.packetClassName).append('\n');
            sb.append("HEX Data: ").append(p.getHexData()).append("\n");
            sb.append(I18n.format("gui.packet.viewer.copy.decoded")).append(p.getDecodedData()).append("\n\n");
        }
        String content = sb.toString();
        if (content.length() <= 32767) {
            setClipboardString(content);
        } else {
            Path exportPath = exportLongCopyContent(content);
            if (exportPath != null) setClipboardString(exportPath.toAbsolutePath().toString());
        }
        if (mc.player != null) mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.GREEN + I18n.format("msg.packet.viewer.copied_all", selectedIndices.size())));
    }

    private void saveSelectedSnapshot() {
        if (selectedIndices.isEmpty()) return;
        String defaultName = "快照-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        preserveViewStateOnNextInit = true;
        mc.setScreen(new GuiTextInput(this, I18n.format("gui.packet.viewer.snapshot_input_name"), defaultName, name -> {
            if (name != null && !name.trim().isEmpty()) {
                List<PacketCaptureHandler.CapturedPacketData> packetsToSave = new ArrayList<>();
                for (int index : new java.util.TreeSet<>(selectedIndices)) if (index >= 0 && index < filteredPackets.size()) packetsToSave.add(filteredPackets.get(index));
                String mode = readOnlyMode ? I18n.format("gui.packet.viewer.mode_snapshot")
                        : (currentMode == DisplayMode.SENT ? I18n.format("gui.packet.viewer.sent") : I18n.format("gui.packet.viewer.received"));
                PacketSnapshotManager.saveSnapshot(name.trim(), packetsToSave, mode);
            }
            preserveViewStateOnNextInit = true;
            mc.setScreen(this);
        }));
    }

    private void openSmartGeneratorForSelection() {
        if (selectedIndices.isEmpty()) {
            return;
        }
        preserveViewStateOnNextInit = true;
        List<String> samples = new ArrayList<>();
        List<Integer> sortedIndices = new ArrayList<>(selectedIndices);
        Collections.sort(sortedIndices);
        for (int index : sortedIndices) {
            if (index >= 0 && index < filteredPackets.size()) {
                samples.add(filteredPackets.get(index).getHexData());
            }
        }
        mc.setScreen(new GuiCapturedIdSmartGenerator(this, "", samples));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        String title = customTitle != null && !customTitle.trim().isEmpty() ? customTitle
                : (currentMode == DisplayMode.SENT ? I18n.format("gui.packet.viewer.title_sent") : I18n.format("gui.packet.viewer.title_received"));
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.packet.viewer.title_with_count", title, filteredPackets.size()), this.fontRenderer);
        drawThemedTextField(filterField);
        if (filterField.getText().isEmpty() && !filterField.isFocused()) drawString(fontRenderer, "§8" + I18n.format("gui.packet.viewer.filter_hint"), filterField.x + 4, filterField.y + 6, 0xFFFFFF);
        GuiTheme.drawInputFrameSafe(leftX, leftY, leftWidth, leftHeight, false, true);
        GuiTheme.drawInputFrameSafe(rightX, rightY, rightWidth, rightHeight, false, true);
        drawString(fontRenderer, "键鼠时间轴", leftX + 6, leftY - 10, 0xFFEAF7FF);
        drawString(fontRenderer, "数据包队列", rightX + 6, rightY - 10, 0xFFEAF7FF);
        String footer = "§7左侧选中键鼠事件后，右侧会跳到最近数据包时间点。";
        if (selectedInputIndex >= 0 && selectedInputIndex < visibleInputEvents.size()) {
            int nearest = getNearestPacketIndex(visibleInputEvents.get(selectedInputIndex).getTimestamp());
            long diff = getNearestPacketDiff(visibleInputEvents.get(selectedInputIndex).getTimestamp());
            if (nearest >= 0) {
                footer = "§7最近包: #"
                        + (nearest + 1)
                        + "  时间差: "
                        + (diff >= 0 ? "+" : "")
                        + diff + "ms";
            }
        }
        drawInputTimeline(mouseX, mouseY);
        drawPacketList(mouseX, mouseY);
        int footerBoxX = leftX;
        int footerBoxH = 20;
        int footerBoxY = leftY + leftHeight - footerBoxH;
        int footerBoxW = leftWidth;
        GuiTheme.drawInputFrameSafe(footerBoxX, footerBoxY, footerBoxW, footerBoxH, false, true);
        drawString(fontRenderer, fontRenderer.trimStringToWidth(footer, footerBoxW - 12), footerBoxX + 6, footerBoxY + 5,
                0xFFB8C7D9);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (inputFilterDropdown != null) inputFilterDropdown.draw(mouseX, mouseY);
        if (timeWindowDropdown != null) timeWindowDropdown.draw(mouseX, mouseY);
        if (packetColumnsDropdown != null) packetColumnsDropdown.draw(mouseX, mouseY);
        if (packetTextScaleDropdown != null) packetTextScaleDropdown.draw(mouseX, mouseY);
        if (packetHintDropdown != null) packetHintDropdown.draw(mouseX, mouseY);
        drawViewerTooltip(mouseX, mouseY);
    }

    private void drawInputTimeline(int mouseX, int mouseY) {
        if (!freezeInputTimeline || inputEvents.isEmpty()) {
            inputEvents = InputTimelineManager.getEventsSnapshot();
        }
        visibleInputEvents = filterInputEvents(inputEvents);
        rebuildInputTreeRows();
        recalcInputHorizontalScroll();
        inputMaxScroll = Math.max(0, inputTreeRows.size() - getVisibleInputCount());
        inputScrollOffset = Math.max(0, Math.min(inputScrollOffset, inputMaxScroll));
        int visible = getVisibleInputCount();
        for (int i = 0; i < visible; i++) {
            int index = i + inputScrollOffset;
            if (index >= inputTreeRows.size()) break;
            InputTreeRow row = inputTreeRows.get(index);
            int rowY = inputListY + i * INPUT_ITEM_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, leftX, rowY, leftWidth, INPUT_ITEM_HEIGHT);
            if (row.groupRow) {
                drawRect(leftX + 2, rowY + 1, leftX + leftWidth - 10, rowY + INPUT_ITEM_HEIGHT - 2, hovered ? 0x883A4D63 : 0x66303F50);
                String prefix = collapsedInputSessions.contains(row.sessionId) ? "▶ " : "▼ ";
                boolean selectedGroup = row.sessionId == selectedInputSessionId;
                if (selectedGroup) {
                    drawRect(leftX + 2, rowY + 1, leftX + leftWidth - 10, rowY + INPUT_ITEM_HEIGHT - 2, 0x88546D3A);
                }
                String groupText = applyInputHorizontalWindow(prefix + row.label, leftWidth - 18);
                drawString(fontRenderer, fontRenderer.trimStringToWidth(groupText, leftWidth - 18), leftX + 6, rowY + 7, 0xFFEAF7FF);
                continue;
            }
            InputTimelineManager.InputEventRecord r = row.event;
            boolean selected = selectedInputIndex >= 0
                    && selectedInputIndex < visibleInputEvents.size()
                    && visibleInputEvents.get(selectedInputIndex) == r;
            GuiTheme.drawButtonFrameSafe(leftX + 12, rowY + 1, leftWidth - 20, INPUT_ITEM_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            String line = buildInputRowText(r, selected);
            line = applyInputHorizontalWindow(line, leftWidth - 30);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(line, leftWidth - 30), leftX + 18, rowY + 7, 0xFFFFFFFF);
        }
        if (visibleInputEvents.isEmpty()) drawCenteredString(fontRenderer, "暂无键鼠事件", leftX + leftWidth / 2, inputListY + 18, 0xFF9FB0C0);
        drawScrollbarIfNeeded(leftX + leftWidth - 6, inputListY, inputListHeight, inputScrollOffset, inputMaxScroll, inputTreeRows.size(), visible, INPUT_ITEM_HEIGHT);
        drawInputHorizontalScrollbar();
    }

    private void drawPacketList(int mouseX, int mouseY) {
        rebuildPacketViewIndexes();
        packetMaxScroll = Math.max(0, getTotalPacketRows() - getVisiblePacketRows());
        packetScrollOffset = Math.max(0, Math.min(packetScrollOffset, packetMaxScroll));
        int visible = getVisiblePacketCount();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        int cols = getPacketColumns();
        int cardGap = 8;
        int cardWidth = Math.max(150, (rightWidth - cardGap * (cols - 1)) / cols);
        int cardHeight = getPacketCardHeight();
        int visibleRows = getVisiblePacketRows();
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < cols; col++) {
                int displayIndex = (row + packetScrollOffset) * cols + col;
                if (displayIndex >= packetViewIndexes.size()) break;
                int index = packetViewIndexes.get(displayIndex);
                PacketCaptureHandler.CapturedPacketData p = filteredPackets.get(index);
                int x = rightX + col * (cardWidth + cardGap);
                int y = rightY + row * cardHeight;
                boolean selected = selectedIndices.contains(index);
                boolean hovered = isInside(mouseX, mouseY, x, y, cardWidth, cardHeight);
            boolean nearLinked = isNearSelectedInputPacket(index);
                GuiTheme.drawButtonFrameSafe(x + 2, y + 1, cardWidth - 6, cardHeight - 2,
                    selected ? GuiTheme.UiState.SELECTED : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
                if (nearLinked && !selected) {
                    drawRect(x + 3, y + 2, x + cardWidth - 8, y + cardHeight - 3, 0x332A7A55);
                }
                int textY = y + 5;
                String titleLine = "§7#" + (index + 1) + " §f" + p.packetClassName + " §7(" + sdf.format(new Date(p.getLastTimestamp())) + ")";
                drawScaledTrimmedString(titleLine, x + 8, textY, cardWidth - 14, 0xFFFFFFFF);
                textY += getScaledLineStep();

                String idLine = p.isFmlPacket ? "§6Channel: §f" + p.channel : "§bPacket ID: §f" + (p.packetId != null ? String.format("0x%02X", p.packetId) : "N/A");
                if (selectedInputIndex >= 0 && selectedInputIndex < visibleInputEvents.size()) {
                    long diff = p.getLastTimestamp() - visibleInputEvents.get(selectedInputIndex).getTimestamp();
                    idLine += "  §7Δ" + (diff >= 0 ? "+" : "") + diff + "ms";
                }
                drawScaledTrimmedString(idLine, x + 10, textY, cardWidth - 18, 0xFFFFFFFF);
                textY += getScaledLineStep();

                String hexPreview = p.getHexData(); if (hexPreview.length() > 120) hexPreview = hexPreview.substring(0, 117) + "...";
                drawScaledTrimmedString("§6Hex: §e" + hexPreview, x + 10, textY, cardWidth - 18, 0xFFFFFFFF);
                textY += getScaledLineStep();

                drawScaledTrimmedString("§a" + I18n.format("gui.packet.viewer.decoded") + ": §f" + buildInlinePreview(p.getDecodedData(), 150),
                        x + 10, textY, cardWidth - 18, 0xFFFFFFFF);
                textY += getScaledLineStep();

                if (!hidePacketHintLine) {
                    drawScaledTrimmedString("§7左键查看详情 / 右键复制字段 / Ctrl 多选 / Shift 连选", x + 8, textY, cardWidth - 14, 0xFFAAAAAA);
                }
            }
        }
        if (filteredPackets.isEmpty()) drawCenteredString(fontRenderer, "暂无数据包", rightX + rightWidth / 2, rightY + 18, 0xFF9FB0C0);
        drawScrollbarIfNeeded(rightX + rightWidth - 10, rightY, rightHeight, packetScrollOffset, packetMaxScroll, getTotalPacketRows(), visibleRows, cardHeight);
    }

    private void drawScrollbarIfNeeded(int x, int y, int height, int scrollOffset, int maxScroll, int totalItems, int visibleItems, int rowHeight) {
        if (maxScroll <= 0 || totalItems <= 0) return;
        int totalHeight = totalItems * rowHeight;
        int thumbHeight = Math.max(18, (int) ((height / (float) Math.max(height, totalHeight)) * height));
        int track = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
        GuiTheme.drawScrollbar(x, y, 8, height, thumbY, thumbHeight);
    }

    private int getPacketColumns() {
        return Math.max(1, Math.min(2, packetColumns));
    }

    private int getPacketCardHeight() {
        int lines = hidePacketHintLine ? 4 : 5;
        return Math.max(54, lines * getScaledLineStep() + 10);
    }

    private int getVisiblePacketRows() {
        return Math.max(1, rightHeight / getPacketCardHeight());
    }

    private int getTotalPacketRows() {
        int cols = getPacketColumns();
        return cols <= 0 ? 0 : (packetViewIndexes.size() + cols - 1) / cols;
    }

    private int getScaledLineStep() {
        return Math.max(9, Math.round(12 * packetTextScaleMode.scale));
    }

    private void drawScaledTrimmedString(String text, int x, int y, int maxWidthPixels, int color) {
        String safeText = text == null ? "" : text;
        float scale = packetTextScaleMode.scale;
        int logicalWidth = Math.max(12, (int) (maxWidthPixels / Math.max(0.01f, scale)));
        String trimmed = fontRenderer.trimStringToWidth(safeText, logicalWidth);
        if (Math.abs(scale - 1.0f) < 0.001f) {
            drawString(fontRenderer, trimmed, x, y, color);
            return;
        }
        GuiCompatContext.current().pose().pushPose();
        GuiCompatContext.current().pose().scale(scale, scale, 1.0F);
        fontRenderer.drawString(trimmed, Math.round(x / scale), Math.round(y / scale), color);
        GuiCompatContext.current().pose().popPose();
    }

    private void recalcInputHorizontalScroll() {
        int maxWidth = 0;
        for (InputTreeRow row : inputTreeRows) {
            if (row == null) {
                continue;
            }
            String text = row.groupRow ? row.label : buildInputRowText(row.event, false);
            maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(text == null ? "" : text));
        }
        int visibleWidth = Math.max(40, leftWidth - 30);
        int avgCharWidth = Math.max(1, fontRenderer.getStringWidth("0123456789") / 10);
        inputHorizontalMaxScroll = Math.max(0, (maxWidth - visibleWidth) / avgCharWidth);
        inputHorizontalScrollOffset = Math.max(0, Math.min(inputHorizontalScrollOffset, inputHorizontalMaxScroll));
    }

    private String applyInputHorizontalWindow(String text, int width) {
        String safeText = text == null ? "" : text;
        if (inputHorizontalScrollOffset <= 0) {
            return safeText;
        }
        int offset = Math.min(inputHorizontalScrollOffset, Math.max(0, safeText.length() - 1));
        return safeText.substring(offset);
    }

    private String buildInputRowText(InputTimelineManager.InputEventRecord record, boolean includeNearest) {
        if (record == null) {
            return "";
        }
        String line = record.getTimestampText() + " | " + record.getCategory() + " | " + record.getDetail();
        if (includeNearest) {
            int nearestIndex = getNearestPacketIndex(record.getTimestamp());
            long diff = getNearestPacketDiff(record.getTimestamp());
            if (nearestIndex >= 0) {
                line += " -> #" + (nearestIndex + 1) + "(" + (diff >= 0 ? "+" : "") + diff + "ms)";
            }
        }
        return line;
    }

    private void drawInputHorizontalScrollbar() {
        if (inputHorizontalMaxScroll <= 0) {
            return;
        }
        int barX = leftX + 2;
        int barY = leftY + leftHeight - 8;
        int barW = leftWidth - 12;
        int barH = 6;
        int thumbWidth = Math.max(18, (int) ((barW / (float) Math.max(barW, barW + inputHorizontalMaxScroll * 6)) * barW));
        int track = Math.max(1, barW - thumbWidth);
        int thumbX = barX + (int) ((inputHorizontalScrollOffset / (float) Math.max(1, inputHorizontalMaxScroll)) * track);
        drawRect(barX, barY, barX + barW, barY + barH, 0x55222A33);
        drawRect(thumbX, barY, thumbX + thumbWidth, barY + barH, 0xFF7EC8FF);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (filterField.textboxKeyTyped(typedChar, keyCode)) {
            applyFilter(filterField.getText(), false);
            updateButtonStates();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        filterField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            if (inputFilterDropdown != null && inputFilterDropdown.handleClick(mouseX, mouseY)) {
                collapseOtherDropdowns(inputFilterDropdown);
                inputFilterMode = InputFilterMode.values()[inputFilterDropdown.getSelectedIndex()];
                applyFilter(filterField == null ? "" : filterField.getText(), true);
                return;
            }
            if (timeWindowDropdown != null && timeWindowDropdown.handleClick(mouseX, mouseY)) {
                collapseOtherDropdowns(timeWindowDropdown);
                timeWindowMode = TimeWindowMode.values()[timeWindowDropdown.getSelectedIndex()];
                applyFilter(filterField == null ? "" : filterField.getText(), true);
                return;
            }
            if (packetColumnsDropdown != null && packetColumnsDropdown.handleClick(mouseX, mouseY)) {
                collapseOtherDropdowns(packetColumnsDropdown);
                packetColumns = packetColumnsDropdown.getSelectedIndex() + 1;
                applyFilter(filterField == null ? "" : filterField.getText(), true);
                return;
            }
            if (packetTextScaleDropdown != null && packetTextScaleDropdown.handleClick(mouseX, mouseY)) {
                collapseOtherDropdowns(packetTextScaleDropdown);
                packetTextScaleMode = PacketTextScaleMode.values()[packetTextScaleDropdown.getSelectedIndex()];
                applyFilter(filterField == null ? "" : filterField.getText(), true);
                return;
            }
            if (packetHintDropdown != null && packetHintDropdown.handleClick(mouseX, mouseY)) {
                collapseOtherDropdowns(packetHintDropdown);
                hidePacketHintLine = packetHintDropdown.getSelectedIndex() == 1;
                applyFilter(filterField == null ? "" : filterField.getText(), true);
                return;
            }
        }

        if (mouseButton == 0 && isInside(mouseX, mouseY, leftX + leftWidth - 10, inputListY, 10, inputListHeight) && inputMaxScroll > 0) {
            draggingInputScrollbar = true;
            return;
        }
        if (mouseButton == 0 && isInside(mouseX, mouseY, rightX + rightWidth - 12, rightY, 12, rightHeight) && packetMaxScroll > 0) {
            draggingPacketScrollbar = true;
            return;
        }

        if (isInside(mouseX, mouseY, leftX, leftY, leftWidth, leftHeight)) {
            if (mouseY < inputListY) {
                return;
            }
            int clicked = (mouseY - inputListY) / INPUT_ITEM_HEIGHT + inputScrollOffset;
            if (clicked >= 0 && clicked < inputTreeRows.size()) {
                InputTreeRow row = inputTreeRows.get(clicked);
                if (row.groupRow) {
                    selectedInputSessionId = row.sessionId;
                    selectedInputIndex = -1;
                    if (collapsedInputSessions.contains(row.sessionId)) {
                        collapsedInputSessions.remove(row.sessionId);
                    } else {
                        collapsedInputSessions.add(row.sessionId);
                    }
                    rebuildInputTreeRows();
                    applyFilter(filterField == null ? "" : filterField.getText(), true);
                } else if (row.event != null) {
                    selectedInputSessionId = row.sessionId;
                    selectedInputIndex = visibleInputEvents.indexOf(row.event);
                    if (selectedInputIndex >= 0) {
                        jumpToNearestPacket(row.event.getTimestamp());
                        updateButtonStates();
                    }
                }
            }
            return;
        }
        if (isInside(mouseX, mouseY, rightX, rightY, rightWidth, rightHeight)) {
            int cols = getPacketColumns();
            int cardGap = 8;
            int cardWidth = Math.max(150, (rightWidth - cardGap * (cols - 1)) / cols);
            int cardHeight = getPacketCardHeight();
            int clickedRow = (mouseY - rightY) / cardHeight;
            int clickedCol = Math.max(0, Math.min(cols - 1, (mouseX - rightX) / Math.max(1, cardWidth + cardGap)));
            int displayIndex = (clickedRow + packetScrollOffset) * cols + clickedCol;
            if (displayIndex < 0 || displayIndex >= packetViewIndexes.size()) return;
            int actual = packetViewIndexes.get(displayIndex);
            PacketCaptureHandler.CapturedPacketData p = filteredPackets.get(actual);
            int yInCard = (mouseY - rightY) % cardHeight;
            if (mouseButton == 0) applySelectionByModifier(actual);
            int step = getScaledLineStep();
            if (yInCard >= 5 && yInCard < 5 + step) {
                if (mouseButton == 1) copyTextWithToast(p.packetClassName, I18n.format("msg.packet.viewer.copied_class"));
                else if (mouseButton == 0) openPacketDetail(p, actual, GuiPacketDetailViewer.ViewSection.CLASS_NAME);
            } else if (yInCard >= 5 + step && yInCard < 5 + step * 2) {
                if (mouseButton == 1) copyTextWithToast(p.channel, I18n.format("msg.packet.viewer.copied_channel"));
                else if (mouseButton == 0) openPacketDetail(p, actual, GuiPacketDetailViewer.ViewSection.CHANNEL_OR_ID);
            } else if (yInCard >= 5 + step * 2 && yInCard < 5 + step * 3) {
                if (mouseButton == 1) copyTextWithToast(p.getHexData(), I18n.format("msg.packet.viewer.copied_hex"));
                else if (mouseButton == 0) openPacketDetail(p, actual, GuiPacketDetailViewer.ViewSection.HEX);
            } else if (yInCard >= 5 + step * 3 && yInCard < 5 + step * 4) {
                if (mouseButton == 1) copyTextWithToast(p.getDecodedData(), I18n.format("msg.packet.viewer.copied_decoded"));
                else if (mouseButton == 0) openPacketDetail(p, actual, GuiPacketDetailViewer.ViewSection.DECODE_DETAIL);
            }
            updateButtonStates();
        }
    }

    private void jumpToNearestPacket(long timestamp) {
        if (filteredPackets.isEmpty()) return;
        int bestIndex = 0;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < filteredPackets.size(); i++) {
            long diff = Math.abs(filteredPackets.get(i).getLastTimestamp() - timestamp);
            if (diff < bestDiff) { bestDiff = diff; bestIndex = i; }
        }
        selectedIndices.clear();
        selectedIndices.add(bestIndex);
        lastSelectedIndex = bestIndex;
        ensurePacketVisible(bestIndex);
    }

    private void ensurePacketVisible(int index) {
        int displayIndex = packetViewIndexes.indexOf(index);
        if (displayIndex < 0) {
            rebuildPacketViewIndexes();
            displayIndex = packetViewIndexes.indexOf(index);
        }
        if (displayIndex < 0) {
            return;
        }
        int visible = getVisiblePacketCount();
        if (displayIndex < packetScrollOffset) packetScrollOffset = displayIndex;
        else if (displayIndex >= packetScrollOffset + visible) packetScrollOffset = displayIndex - visible + 1;
    }

    private void applySelectionByModifier(int actualIndex) {
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        if (shift && lastSelectedIndex != -1) {
            selectedIndices.clear();
            int start = Math.min(lastSelectedIndex, actualIndex), end = Math.max(lastSelectedIndex, actualIndex);
            for (int i = start; i <= end; i++) selectedIndices.add(i);
        } else if (ctrl) {
            if (selectedIndices.contains(actualIndex)) selectedIndices.remove(actualIndex); else selectedIndices.add(actualIndex);
            lastSelectedIndex = actualIndex;
        } else {
            selectedIndices.clear();
            selectedIndices.add(actualIndex);
            lastSelectedIndex = actualIndex;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) return;
        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;
        if (isInside(mouseX, mouseY, leftX, inputListY, leftWidth, inputListHeight) && inputMaxScroll > 0) {
            inputScrollOffset = dWheel > 0 ? Math.max(0, inputScrollOffset - 1) : Math.min(inputMaxScroll, inputScrollOffset + 1);
            return;
        }
        if (isInside(mouseX, mouseY, rightX, rightY, rightWidth, rightHeight) && packetMaxScroll > 0) {
            packetScrollOffset = dWheel > 0 ? Math.max(0, packetScrollOffset - 1) : Math.min(packetMaxScroll, packetScrollOffset + 1);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingInputScrollbar) {
            float percent = (float) (mouseY - inputListY) / Math.max(1, inputListHeight);
            inputScrollOffset = Math.max(0, Math.min(inputMaxScroll, (int) (percent * (inputMaxScroll + 1))));
            return;
        }
        if (draggingPacketScrollbar) {
            float percent = (float) (mouseY - rightY) / Math.max(1, rightHeight);
            packetScrollOffset = Math.max(0, Math.min(packetMaxScroll, (int) (percent * (packetMaxScroll + 1))));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingInputScrollbar = false;
        draggingPacketScrollbar = false;
    }

    private void openPacketDetail(PacketCaptureHandler.CapturedPacketData packet, int actualIndex, GuiPacketDetailViewer.ViewSection section) {
        preserveViewStateOnNextInit = true;
        mc.setScreen(new GuiPacketDetailViewer(this, packet, actualIndex + 1, resolveDirectionLabel(), section));
    }

    private String resolveDirectionLabel() {
        if (readOnlyMode) return customTitle != null && !customTitle.trim().isEmpty() ? customTitle : I18n.format("gui.packet.viewer.mode_snapshot");
        return currentMode == DisplayMode.SENT ? I18n.format("gui.packet.viewer.sent") : I18n.format("gui.packet.viewer.received");
    }

    private void copyTextWithToast(String text, String successMessage) {
        setClipboardString(text == null ? "" : text);
        if (mc.player != null) mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.GREEN + successMessage));
    }

    private Path exportLongCopyContent(String content) {
        try {
            Path exportDir = mc.gameDirectory.toPath().resolve("zszl_script").resolve("packet_exports");
            Files.createDirectories(exportDir);
            String fileName = "packet-copy-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date()) + ".txt";
            Path filePath = exportDir.resolve(fileName);
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            return filePath;
        } catch (IOException ignored) {
            return null;
        }
    }

    private String buildInlinePreview(String text, int maxLength) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private int getVisibleInputCount() { return Math.max(1, inputListHeight / INPUT_ITEM_HEIGHT); }
    private int getVisiblePacketCount() { return Math.max(1, getVisiblePacketRows()); }
    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; }

    private void drawViewerTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (isMouseOverField(mouseX, mouseY, filterField)) {
            tooltip = "抓包过滤框。\n按包名、HEX、解码文本、频道或 ID 组合筛选当前列表。";
        } else if (isInside(mouseX, mouseY, leftX, leftY, leftWidth, leftHeight)) {
            tooltip = "左侧输入时间线。\n用于把键盘/鼠标输入与右侧数据包发生时间做对照。";
        } else if (isInside(mouseX, mouseY, rightX, rightY, rightWidth, rightHeight)) {
            tooltip = "右侧数据包卡片区。\n左键可选中，右键对应行可复制字段内容，双击可打开详情。";
        } else if (inputFilterDropdown != null && isInside(mouseX, mouseY, inputFilterDropdown.x, inputFilterDropdown.y, inputFilterDropdown.width, inputFilterDropdown.height)) {
            tooltip = "输入时间线筛选。\n限制左侧只显示键盘、鼠标、左键、右键等特定输入事件。";
        } else if (timeWindowDropdown != null && isInside(mouseX, mouseY, timeWindowDropdown.x, timeWindowDropdown.y, timeWindowDropdown.width, timeWindowDropdown.height)) {
            tooltip = "时间窗口。\n控制左侧输入事件与右侧数据包的关联时间范围。";
        } else if (packetColumnsDropdown != null && isInside(mouseX, mouseY, packetColumnsDropdown.x, packetColumnsDropdown.y, packetColumnsDropdown.width, packetColumnsDropdown.height)) {
            tooltip = "卡片列数。\n可调整右侧数据包卡片每行显示几列。";
        } else if (packetTextScaleDropdown != null && isInside(mouseX, mouseY, packetTextScaleDropdown.x, packetTextScaleDropdown.y, packetTextScaleDropdown.width, packetTextScaleDropdown.height)) {
            tooltip = "卡片字号。\n调整右侧卡片中文本缩放大小。";
        } else if (packetHintDropdown != null && isInside(mouseX, mouseY, packetHintDropdown.x, packetHintDropdown.y, packetHintDropdown.width, packetHintDropdown.height)) {
            tooltip = "提示行显示开关。\n可控制右侧卡片的提示文字是否显示。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case 0:
                        tooltip = "把当前选中的数据包作为序列发送模拟。";
                        break;
                    case 1:
                        tooltip = "返回上一页。";
                        break;
                    case 2:
                        tooltip = "复制当前选中的数据包文本摘要。";
                        break;
                    case 3:
                        tooltip = "切换查看发送包 / 接收包。";
                        break;
                    case 4:
                        tooltip = "把当前选中的数据包保存成快照。";
                        break;
                    case 5:
                        tooltip = "打开历史快照列表。";
                        break;
                    case 6:
                        tooltip = "打开 Packet ID 自动记录页面。";
                        break;
                    case 7:
                        tooltip = "清空左侧键盘/鼠标输入时间线。";
                        break;
                    case 9:
                        tooltip = "冻结左侧输入时间线。\n开启后不会继续追加新的键鼠输入事件。";
                        break;
                    case 10:
                        tooltip = "把当前选中的数据包 HEX 一键导入捕获ID智能生成器。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private int getNearestPacketIndex(long timestamp) {
        if (filteredPackets.isEmpty()) return -1;
        int bestIndex = 0;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < filteredPackets.size(); i++) {
            long diff = Math.abs(filteredPackets.get(i).getLastTimestamp() - timestamp);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private long getNearestPacketDiff(long timestamp) {
        int index = getNearestPacketIndex(timestamp);
        if (index < 0 || index >= filteredPackets.size()) return -1L;
        return filteredPackets.get(index).getLastTimestamp() - timestamp;
    }

    private boolean isNearSelectedInputPacket(int packetIndex) {
        if (selectedInputIndex < 0 || selectedInputIndex >= visibleInputEvents.size() || packetIndex < 0 || packetIndex >= filteredPackets.size()) {
            return false;
        }
        long ts = visibleInputEvents.get(selectedInputIndex).getTimestamp();
        List<Integer> top = getNearestPacketIndexes(ts, 3);
        return top.contains(packetIndex);
    }

    private List<Integer> getNearestPacketIndexes(long timestamp, int limit) {
        List<Integer> indexes = new ArrayList<>();
        if (filteredPackets.isEmpty() || limit <= 0) return indexes;
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < filteredPackets.size(); i++) all.add(i);
        all.sort((a, b) -> Long.compare(
                Math.abs(filteredPackets.get(a).getLastTimestamp() - timestamp),
                Math.abs(filteredPackets.get(b).getLastTimestamp() - timestamp)));
        for (int i = 0; i < Math.min(limit, all.size()); i++) indexes.add(all.get(i));
        return indexes;
    }
}


