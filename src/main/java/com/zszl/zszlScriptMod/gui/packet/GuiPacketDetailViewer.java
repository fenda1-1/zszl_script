package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PacketPayloadDecoder;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

public class GuiPacketDetailViewer extends ThemedGuiScreen {
    private static final int BUTTON_BACK = 0;
    private static final int BUTTON_COPY_SELECTION = 1;
    private static final int BUTTON_COPY_FULL = 2;
    private static final int BUTTON_COPY_DECODE_CHUNKS = 3;

    private final GuiScreen parentScreen;
    private final PacketCaptureHandler.CapturedPacketData packet;
    private final int packetSequence;
    private final String directionLabel;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final List<String> lines = new ArrayList<>();
    private PacketPayloadDecoder.AnnotatedDecodeReport annotatedDecodeReport;
    private List<PacketPayloadDecoder.DecodedChunk> decodedChunks = new ArrayList<>();

    private ViewSection activeSection;
    private String activeContent = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int cursorLine = 0;
    private int cursorColumn = 0;
    private int anchorLine = 0;
    private int anchorColumn = 0;

    private boolean draggingSelection = false;
    private boolean draggingScrollbar = false;
    private boolean draggingHexSelection = false;
    private int selectedHexStart = -1;
    private int selectedHexEnd = -1;
    private int hoveredChunkIndex = -1;
    private int decodedChunkScrollOffset = 0;

    private static final class HexPanelLayout {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int bytesPerRow;
        private final int rowHeight;
        private final int visibleRows;
        private final int totalRows;
        private final int offsetWidth;
        private final int cellWidth;
        private final int bytesX;
        private final int asciiX;
        private final int asciiCharWidth;
        private final boolean showAscii;

        private HexPanelLayout(int x, int y, int width, int height, int bytesPerRow, int rowHeight, int visibleRows,
                int totalRows, int offsetWidth, int cellWidth, int bytesX, int asciiX, int asciiCharWidth,
                boolean showAscii) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.bytesPerRow = bytesPerRow;
            this.rowHeight = rowHeight;
            this.visibleRows = visibleRows;
            this.totalRows = totalRows;
            this.offsetWidth = offsetWidth;
            this.cellWidth = cellWidth;
            this.bytesX = bytesX;
            this.asciiX = asciiX;
            this.asciiCharWidth = asciiCharWidth;
            this.showAscii = showAscii;
        }
    }

    private String statusMessage = "§7左侧切换字段，拖拽选择文本，Ctrl+C复制，Ctrl+A全选";
    private int statusColor = 0xFFB8C7D9;

    public GuiPacketDetailViewer(GuiScreen parentScreen, PacketCaptureHandler.CapturedPacketData packet, int packetSequence,
            String directionLabel, ViewSection initialSection) {
        this.parentScreen = parentScreen;
        this.packet = packet;
        this.packetSequence = packetSequence;
        this.directionLabel = directionLabel == null ? "" : directionLabel;
        this.activeSection = initialSection == null ? ViewSection.DECODE_DETAIL : initialSection;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int buttonY = getPanelY() + getPanelHeight() - 28;
        int centerX = getPanelX() + getPanelWidth() / 2;
        this.buttonList.add(new ThemedButton(BUTTON_BACK, getPanelX() + 10, buttonY, 90, 20, "§c返回"));
        this.buttonList.add(new ThemedButton(BUTTON_COPY_SELECTION, centerX - 150, buttonY, 90, 20, "§b复制选中"));
        this.buttonList.add(new ThemedButton(BUTTON_COPY_FULL, centerX - 50, buttonY, 90, 20, "§a复制全文"));
        this.buttonList.add(new ThemedButton(BUTTON_COPY_DECODE_CHUNKS, centerX + 50, buttonY, 110, 20,
                "§d复制通用解码块"));

        setActiveSection(activeSection);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BUTTON_BACK:
                this.mc.setScreen(parentScreen);
                break;
            case BUTTON_COPY_SELECTION:
                copySelectionOrCurrent();
                break;
            case BUTTON_COPY_FULL:
                copyCurrentSection();
                break;
            case BUTTON_COPY_DECODE_CHUNKS:
                copyDecodedChunks();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth,
                "数据包详情 - #" + packetSequence + " " + buildDirectionTitle(), this.fontRenderer);

        int metaY = panelY + 26;
        this.drawString(this.fontRenderer,
                "§7类名: §f" + safe(packet.packetClassName) + "  §7大小: §f" + packet.getPayloadSize() + "B",
                panelX + 10, metaY, 0xFFE5EEF8);
        this.drawString(this.fontRenderer,
                "§7时间: §f" + timeFormat.format(new Date(packet.getLastTimestamp())) + "  §7聚合: §f"
                        + packet.getOccurrenceCount(),
                panelX + 10, metaY + 12, 0xFFB8C7D9);

        int statusY = panelY + 52;
        GuiTheme.drawInputFrameSafe(panelX + 10, statusY, panelWidth - 20, 18, false, true);
        this.drawString(this.fontRenderer, statusMessage, panelX + 14, statusY + 5, statusColor);

        drawSidebar(mouseX, mouseY);
        if (activeSection == ViewSection.HEX) {
            drawHexInspector(mouseX, mouseY);
        } else {
            drawEditor(mouseX, mouseY);
        }

        String footer;
        if (activeSection == ViewSection.HEX) {
            footer = "§7字段: HEX  选中字节: " + getSelectedHexByteCount()
                    + "  解码块: " + (decodedChunks == null ? 0 : decodedChunks.size());
        } else {
            footer = "§7字段: " + activeSection.label + "  行: " + (cursorLine + 1) + "  列: "
                    + (cursorColumn + 1) + "  选中: " + getSelectionLength() + " 字符";
        }
        this.drawString(this.fontRenderer, footer, panelX + 10, panelY + panelHeight - 42, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawDetailTooltip(mouseX, mouseY);
    }

    private void drawSidebar(int mouseX, int mouseY) {
        int sidebarX = getSidebarX();
        int sidebarY = getSidebarY();
        int sidebarW = getSidebarWidth();
        int sidebarH = getSidebarHeight();

        GuiTheme.drawInputFrameSafe(sidebarX, sidebarY, sidebarW, sidebarH, false, true);
        this.drawString(this.fontRenderer, "§l字段", sidebarX + 8, sidebarY + 6, 0xFFE5EEF8);

        int rowY = sidebarY + 22;
        for (ViewSection section : ViewSection.values()) {
            int rowH = 20;
            boolean active = section == activeSection;
            boolean hovered = mouseX >= sidebarX + 4 && mouseX <= sidebarX + sidebarW - 4
                    && mouseY >= rowY && mouseY <= rowY + rowH;
            int bg = active ? 0xAA36658F : hovered ? 0x6634485C : 0x44202B36;
            drawRect(sidebarX + 4, rowY, sidebarX + sidebarW - 4, rowY + rowH, bg);
            if (active) {
                drawRect(sidebarX + 4, rowY, sidebarX + 6, rowY + rowH, 0xFF7ED0FF);
            }
            this.drawString(this.fontRenderer, (active ? "§f" : "§7") + section.label, sidebarX + 10, rowY + 6,
                    0xFFFFFFFF);
            rowY += rowH + 4;
        }
    }

    private void drawEditor(int mouseX, int mouseY) {
        int editorX = getEditorX();
        int editorY = getEditorY();
        int editorW = getEditorWidth();
        int editorH = getEditorHeight();
        boolean focused = isInsideEditor(mouseX, mouseY) || hasSelection();

        GuiTheme.drawInputFrameSafe(editorX, editorY, editorW, editorH, focused, true);
        int lineNumberWidth = 48;
        drawRect(editorX + lineNumberWidth, editorY + 1, editorX + lineNumberWidth + 1, editorY + editorH - 1,
                0x884A5F75);

        int visibleCount = getVisibleLineCount();
        int textX = editorX + lineNumberWidth + 6;
        int maxTextWidth = Math.max(16, editorW - lineNumberWidth - 14);
        int lineHeight = getLineHeight();

        for (int local = 0; local < visibleCount; local++) {
            int lineIndex = scrollOffset + local;
            if (lineIndex >= lines.size()) {
                break;
            }

            int drawY = editorY + 4 + local * lineHeight;
            String lineNo = String.valueOf(lineIndex + 1);
            String lineText = lines.get(lineIndex);
            String clipped = this.fontRenderer.trimStringToWidth(lineText, maxTextWidth);

            drawSelectionHighlight(lineIndex, clipped, drawY, textX);

            this.drawString(this.fontRenderer, "§7" + lineNo,
                    editorX + lineNumberWidth - 4 - this.fontRenderer.getStringWidth(lineNo), drawY, 0xFF9EB1C5);
            this.drawString(this.fontRenderer, clipped, textX, drawY, 0xFFE8F1FA);

            if (!hasSelection() && lineIndex == cursorLine && blinkCursor()) {
                int cursorX = textX + this.fontRenderer
                        .getStringWidth(clampVisiblePrefix(clipped, Math.min(cursorColumn, clipped.length())));
                drawRect(cursorX, drawY - 1, cursorX + 1, drawY + 9, 0xFFFFFFFF);
            }
        }

        if (maxScroll > 0) {
            int scrollbarX = editorX + editorW - 8;
            int thumbHeight = Math.max(18,
                    (int) ((visibleCount / (float) Math.max(visibleCount, lines.size())) * editorH));
            int track = Math.max(1, editorH - thumbHeight);
            int thumbY = editorY + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(scrollbarX, editorY, 6, editorH, thumbY, thumbHeight);
        }
    }

    private void drawHexInspector(int mouseX, int mouseY) {
        int editorX = getEditorX();
        int editorY = getEditorY();
        int editorW = getEditorWidth();
        int editorH = getEditorHeight();
        int gap = 10;
        int leftW = Math.max(320, (int) (editorW * 0.57f));
        leftW = Math.min(leftW, editorW - 220);
        int rightW = editorW - leftW - gap;
        int leftX = editorX;
        int rightX = leftX + leftW + gap;

        GuiTheme.drawInputFrameSafe(leftX, editorY, leftW, editorH, true, true);
        GuiTheme.drawInputFrameSafe(rightX, editorY, rightW, editorH, true, true);
        drawString(this.fontRenderer, "§lHEX 字节视图", leftX + 8, editorY + 6, 0xFFE5EEF8);
        drawString(this.fontRenderer, "§l通用解码块", rightX + 8, editorY + 6, 0xFFE5EEF8);

        drawHexBytePanel(mouseX, mouseY, leftX + 6, editorY + 22, leftW - 12, editorH - 28);
        drawDecodedChunkPanel(mouseX, mouseY, rightX + 6, editorY + 22, rightW - 12, editorH - 28);
    }

    private void drawHexBytePanel(int mouseX, int mouseY, int x, int y, int width, int height) {
        byte[] raw = packet == null || packet.rawData == null ? new byte[0] : packet.rawData;
        if (raw.length == 0) {
            drawString(this.fontRenderer, "§7(empty)", x + 6, y + 6, 0xFF9EB1C5);
            return;
        }

        HexPanelLayout layout = buildHexPanelLayout(x, y, width, height, raw.length);
        maxScroll = Math.max(0, layout.totalRows - layout.visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int localRow = 0; localRow < layout.visibleRows; localRow++) {
            int rowIndex = scrollOffset + localRow;
            int baseIndex = rowIndex * layout.bytesPerRow;
            if (baseIndex >= raw.length) {
                break;
            }
            int drawY = y + 4 + localRow * layout.rowHeight;
            String offsetText = String.format("%04X", baseIndex);
            drawString(this.fontRenderer, "§7" + offsetText, x + 2, drawY + 2, 0xFF9EB1C5);

            for (int column = 0; column < layout.bytesPerRow; column++) {
                int byteIndex = baseIndex + column;
                if (byteIndex >= raw.length) {
                    break;
                }
                int cellX = layout.bytesX + column * layout.cellWidth;
                boolean selected = isHexByteSelected(byteIndex);
                if (selected) {
                    drawRect(cellX - 1, drawY, cellX + layout.cellWidth - 2, drawY + 11, 0xAA4D7FB0);
                }
                String hex = String.format("%02X", raw[byteIndex] & 0xFF);
                drawString(this.fontRenderer, selected ? "§f" + hex : "§e" + hex, cellX, drawY + 2, 0xFFFFFFFF);
            }

            if (!layout.showAscii) {
                continue;
            }
            for (int column = 0; column < layout.bytesPerRow; column++) {
                int byteIndex = baseIndex + column;
                if (byteIndex >= raw.length) {
                    break;
                }
                int value = raw[byteIndex] & 0xFF;
                char display = (value >= 32 && value <= 126) ? (char) value : '.';
                boolean selected = isHexByteSelected(byteIndex);
                int charX = layout.asciiX + column * layout.asciiCharWidth;
                if (selected) {
                    drawRect(charX - 1, drawY, charX + layout.asciiCharWidth, drawY + 11, 0x665C87B6);
                }
                drawString(this.fontRenderer, String.valueOf(display), charX, drawY + 2,
                        selected ? 0xFFFFFFFF : 0xFFB8C7D9);
            }
        }

        if (maxScroll > 0) {
            int scrollbarX = x + width - 7;
            int thumbHeight = Math.max(18, (int) ((layout.visibleRows / (float) layout.totalRows) * height));
            int track = Math.max(1, height - thumbHeight);
            int thumbY = y + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(scrollbarX, y, 6, height, thumbY, thumbHeight);
        }
    }

    private void drawDecodedChunkPanel(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (decodedChunks == null || decodedChunks.isEmpty()) {
            drawString(this.fontRenderer, "§7未识别到可映射的解码块", x + 6, y + 6, 0xFF9EB1C5);
            String fallback = packet == null ? "" : emptyIfBlank(packet.getDecodedDetailData(), "");
            if (!fallback.isEmpty()) {
                this.fontRenderer.drawSplitString(fallback, x + 6, y + 22, width - 12, 0xFFB8C7D9);
            }
            return;
        }

        int rowHeight = 30;
        int visibleRows = Math.max(1, (height - 4) / rowHeight);
        int maxChunkScroll = Math.max(0, decodedChunks.size() - visibleRows);
        decodedChunkScrollOffset = Math.max(0, Math.min(decodedChunkScrollOffset, maxChunkScroll));
        hoveredChunkIndex = -1;
        int lineWidth = Math.max(36, width - 18);

        for (int localRow = 0; localRow < visibleRows; localRow++) {
            int chunkIndex = decodedChunkScrollOffset + localRow;
            if (chunkIndex >= decodedChunks.size()) {
                break;
            }
            PacketPayloadDecoder.DecodedChunk chunk = decodedChunks.get(chunkIndex);
            int rowY = y + 4 + localRow * rowHeight;
            boolean hovered = mouseX >= x + 2 && mouseX <= x + width - 8 && mouseY >= rowY && mouseY <= rowY + rowHeight - 2;
            boolean selected = isChunkIntersectingSelection(chunk);
            int bg = selected ? 0xAA7A2F36 : (hovered ? 0xAA2E4258 : 0x55222A33);
            int border = selected ? 0xFFFF7B86 : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);
            drawRect(x + 2, rowY, x + width - 8, rowY + rowHeight - 2, bg);
            drawHorizontalLine(x + 2, x + width - 8, rowY, border);
            drawHorizontalLine(x + 2, x + width - 8, rowY + rowHeight - 2, border);

            String rangeText = String.format("[%04X-%04X]", chunk.startOffset, Math.max(chunk.startOffset, chunk.endOffset - 1));
            String labelLine = this.fontRenderer.trimStringToWidth("§7" + rangeText + " §f" + emptyIfBlank(chunk.label, "解码块"),
                    lineWidth);
            String previewLine = this.fontRenderer.trimStringToWidth("§b" + buildChunkPreview(chunk.text, 160), lineWidth);
            drawString(this.fontRenderer, labelLine, x + 6, rowY + 4, 0xFFFFFFFF);
            drawString(this.fontRenderer, previewLine, x + 6, rowY + 17, 0xFFFFFFFF);

            if (hovered) {
                hoveredChunkIndex = chunkIndex;
            }
        }

        if (maxChunkScroll > 0) {
            int scrollbarX = x + width - 7;
            int thumbHeight = Math.max(18, (int) ((visibleRows / (float) decodedChunks.size()) * height));
            int track = Math.max(1, height - thumbHeight);
            int thumbY = y + (int) ((decodedChunkScrollOffset / (float) Math.max(1, maxChunkScroll)) * track);
            GuiTheme.drawScrollbar(scrollbarX, y, 6, height, thumbY, thumbHeight);
        }
    }

    private void drawSelectionHighlight(int lineIndex, String clippedLine, int drawY, int textX) {
        if (!hasSelection()) {
            return;
        }

        SelectionRange range = getSelectionRange();
        if (range == null || lineIndex < range.startLine || lineIndex > range.endLine) {
            return;
        }

        int startCol = lineIndex == range.startLine ? range.startColumn : 0;
        int endCol = lineIndex == range.endLine ? range.endColumn : lines.get(lineIndex).length();
        startCol = Math.max(0, Math.min(startCol, clippedLine.length()));
        endCol = Math.max(0, Math.min(endCol, clippedLine.length()));
        if (endCol <= startCol) {
            if (lineIndex == range.endLine && range.endColumn > clippedLine.length() && clippedLine.length() > 0
                    && lineIndex > range.startLine) {
                startCol = 0;
                endCol = clippedLine.length();
            } else {
                return;
            }
        }

        int startX = textX + this.fontRenderer.getStringWidth(clampVisiblePrefix(clippedLine, startCol));
        int endX = textX + this.fontRenderer.getStringWidth(clampVisiblePrefix(clippedLine, endCol));
        drawRect(startX, drawY - 1, Math.max(startX + 1, endX), drawY + 9, 0xAA4D7FB0);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;
        if (activeSection == ViewSection.HEX) {
            if (isInsideHexBytePanel(mouseX, mouseY)) {
                if (wheel > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 2);
                } else {
                    scrollOffset = Math.min(maxScroll, scrollOffset + 2);
                }
                return;
            }
            if (isInsideDecodedChunkPanel(mouseX, mouseY)) {
                int maxChunkScroll = Math.max(0, (decodedChunks == null ? 0 : decodedChunks.size()) - getVisibleChunkCount());
                if (wheel > 0) {
                    decodedChunkScrollOffset = Math.max(0, decodedChunkScrollOffset - 1);
                } else {
                    decodedChunkScrollOffset = Math.min(maxChunkScroll, decodedChunkScrollOffset + 1);
                }
                return;
            }
        }
        if (isInsideEditor(mouseX, mouseY)) {
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 3);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 3);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }

        ViewSection clickedSection = getSectionAt(mouseX, mouseY);
        if (clickedSection != null) {
            setActiveSection(clickedSection);
            return;
        }

        if (isOnScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            return;
        }

        if (activeSection == ViewSection.HEX) {
            Integer clickedByte = getHexByteIndexAt(mouseX, mouseY);
            if (clickedByte != null) {
                selectedHexStart = clickedByte;
                selectedHexEnd = clickedByte;
                draggingHexSelection = true;
                return;
            }
            Integer clickedChunk = getDecodedChunkIndexAt(mouseX, mouseY);
            if (clickedChunk != null && clickedChunk >= 0 && clickedChunk < decodedChunks.size()) {
                PacketPayloadDecoder.DecodedChunk chunk = decodedChunks.get(clickedChunk);
                selectedHexStart = chunk.startOffset;
                selectedHexEnd = Math.max(chunk.startOffset, chunk.endOffset - 1);
                ensureHexSelectionVisible();
                return;
            }
            return;
        }

        if (isInsideEditor(mouseX, mouseY)) {
            moveCursorFromMouse(mouseX, mouseY);
            anchorLine = cursorLine;
            anchorColumn = cursorColumn;
            draggingSelection = true;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingScrollbar) {
            int editorY = activeSection == ViewSection.HEX ? getHexBytePanelRect()[1] : getEditorY();
            int editorH = activeSection == ViewSection.HEX ? getHexBytePanelRect()[3] : getEditorHeight();
            float percent = (float) (mouseY - editorY) / (float) Math.max(1, editorH);
            scrollOffset = (int) (percent * (maxScroll + 1));
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            return;
        }

        if (draggingHexSelection) {
            Integer currentByte = getHexByteIndexAt(mouseX, mouseY);
            if (currentByte != null) {
                selectedHexEnd = currentByte;
            }
            return;
        }

        if (draggingSelection) {
            moveCursorFromMouse(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingSelection = false;
        draggingScrollbar = false;
        draggingHexSelection = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean shift = GuiScreen.isShiftKeyDown();
        if (GuiScreen.isCtrlKeyDown()) {
            if (keyCode == Keyboard.KEY_C) {
                copySelectionOrCurrent();
                return;
            }
            if (keyCode == Keyboard.KEY_A) {
                selectAll();
                return;
            }
        }

        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                this.mc.setScreen(parentScreen);
                return;
            case Keyboard.KEY_LEFT:
                moveLeft(shift);
                return;
            case Keyboard.KEY_RIGHT:
                moveRight(shift);
                return;
            case Keyboard.KEY_UP:
                moveUp(shift);
                return;
            case Keyboard.KEY_DOWN:
                moveDown(shift);
                return;
            case Keyboard.KEY_HOME:
                moveToLineBoundary(true, shift);
                return;
            case Keyboard.KEY_END:
                moveToLineBoundary(false, shift);
                return;
            case Keyboard.KEY_PRIOR:
                pageMove(-1, shift);
                return;
            case Keyboard.KEY_NEXT:
                pageMove(1, shift);
                return;
            default:
                super.keyTyped(typedChar, keyCode);
                break;
        }
    }

    private void setActiveSection(ViewSection section) {
        activeSection = section == null ? ViewSection.DECODE_DETAIL : section;
        if (activeSection == ViewSection.HEX) {
            annotatedDecodeReport = PacketPayloadDecoder.inspectAnnotated(packet == null ? null : packet.rawData);
            decodedChunks = annotatedDecodeReport == null ? new ArrayList<>() : new ArrayList<>(annotatedDecodeReport.chunks);
            selectedHexStart = -1;
            selectedHexEnd = -1;
            hoveredChunkIndex = -1;
        }
        setContent(getSectionContent(activeSection));
        setStatus("§a已切换到字段: " + activeSection.label, 0xFF8CFF9E);
    }

    private String getSectionContent(ViewSection section) {
        if (packet == null || section == null) {
            return "";
        }

        switch (section) {
            case OVERVIEW:
                return buildOverviewText();
            case CLASS_NAME:
                return safe(packet.packetClassName);
            case CHANNEL_OR_ID:
                return buildChannelText();
            case HEX:
                return buildHexDump(packet.rawData);
            case DECODED:
                return emptyIfBlank(packet.getDecodedData(), "未识别到可显示的概要解码结果。");
            case DECODE_DETAIL:
            default:
                return emptyIfBlank(packet.getDecodedDetailData(), "未识别到可显示的详细解码结果。");
        }
    }

    private String buildOverviewText() {
        StringBuilder builder = new StringBuilder();
        builder.append("序号: ").append(packetSequence).append('\n');
        builder.append("方向: ").append(buildDirectionTitle()).append('\n');
        builder.append("类名: ").append(safe(packet.packetClassName)).append('\n');
        builder.append("频道/ID: ").append(buildChannelText()).append('\n');
        builder.append("首次时间: ").append(timeFormat.format(new Date(packet.timestamp))).append('\n');
        builder.append("最后时间: ").append(timeFormat.format(new Date(packet.getLastTimestamp()))).append('\n');
        builder.append("负载大小: ").append(packet.getPayloadSize()).append(" B").append('\n');
        builder.append("聚合次数: ").append(packet.getOccurrenceCount()).append('\n');
        builder.append("累计字节: ").append(packet.getTotalPayloadBytes()).append(" B");
        return builder.toString();
    }

    private String buildChannelText() {
        if (packet.isFmlPacket) {
            return emptyIfBlank(packet.channel, "N/A");
        }
        return packet.packetId == null ? "N/A" : String.format("0x%02X (%d)", packet.packetId, packet.packetId);
    }

    private String buildHexDump(byte[] data) {
        if (data == null || data.length == 0) {
            return "(empty)";
        }

        StringBuilder builder = new StringBuilder(data.length * 5);
        for (int offset = 0; offset < data.length; offset += 16) {
            int end = Math.min(data.length, offset + 16);
            builder.append(String.format("%04X  ", offset));
            for (int i = offset; i < offset + 16; i++) {
                if (i < end) {
                    builder.append(String.format("%02X ", data[i] & 0xFF));
                } else {
                    builder.append("   ");
                }
            }
            builder.append(" |");
            for (int i = offset; i < end; i++) {
                int value = data[i] & 0xFF;
                builder.append(value >= 32 && value <= 126 ? (char) value : '.');
            }
            builder.append('|');
            if (end < data.length) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private void setContent(String content) {
        this.activeContent = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        this.lines.clear();
        this.lines.addAll(Arrays.asList(this.activeContent.split("\n", -1)));
        if (this.lines.isEmpty()) {
            this.lines.add("");
        }
        this.scrollOffset = 0;
        this.cursorLine = 0;
        this.cursorColumn = 0;
        this.anchorLine = 0;
        this.anchorColumn = 0;
        recalcScrollBounds();
    }

    private void copySelectionOrCurrent() {
        if (activeSection == ViewSection.HEX && hasHexSelection()) {
            String text = getSelectedHexText();
            if (text == null || text.isEmpty()) {
                setStatus("§e当前没有可复制的 HEX 字节", 0xFFFFD36B);
                return;
            }
            setClipboardString(text);
            toast("已复制选中 HEX 字节");
            setStatus("§a已复制选中 HEX 字节", 0xFF8CFF9E);
            return;
        }
        String text = hasSelection() ? getSelectedText() : activeContent;
        if (text == null || text.isEmpty()) {
            setStatus("§e当前字段没有可复制内容", 0xFFFFD36B);
            return;
        }
        setClipboardString(text);
        toast("已复制" + (hasSelection() ? "选中内容" : "当前字段全文"));
        setStatus("§a已复制到剪贴板", 0xFF8CFF9E);
    }

    private void copyCurrentSection() {
        if (activeSection == ViewSection.HEX) {
            String hex = packet == null ? "" : buildHexDump(packet.rawData);
            if (hex.isEmpty()) {
                setStatus("§e当前字段没有可复制内容", 0xFFFFD36B);
                return;
            }
            setClipboardString(hex);
            toast("已复制当前 HEX 视图全文");
            setStatus("§a已复制当前 HEX 视图全文到剪贴板", 0xFF8CFF9E);
            return;
        }
        if (activeContent == null || activeContent.isEmpty()) {
            setStatus("§e当前字段没有可复制内容", 0xFFFFD36B);
            return;
        }
        setClipboardString(activeContent);
        toast("已复制当前字段全文");
        setStatus("§a已复制当前字段全文到剪贴板", 0xFF8CFF9E);
    }

    private void copyDecodedChunks() {
        String text = buildDecodedChunksText();
        if (text.isEmpty()) {
            setStatus("§e当前没有可复制的通用解码块", 0xFFFFD36B);
            return;
        }
        setClipboardString(text);
        toast("已复制通用解码块");
        setStatus("§a已复制通用解码块到剪贴板", 0xFF8CFF9E);
    }

    private String buildDecodedChunksText() {
        if (annotatedDecodeReport == null && packet != null) {
            annotatedDecodeReport = PacketPayloadDecoder.inspectAnnotated(packet.rawData);
            decodedChunks = annotatedDecodeReport == null ? new ArrayList<>() : new ArrayList<>(annotatedDecodeReport.chunks);
        }
        if (decodedChunks == null || decodedChunks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < decodedChunks.size(); i++) {
            PacketPayloadDecoder.DecodedChunk chunk = decodedChunks.get(i);
            if (chunk == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append('#').append(i + 1).append(' ');
            builder.append(String.format("[%04X-%04X]", chunk.startOffset,
                    Math.max(chunk.startOffset, chunk.endOffset - 1)));
            if (chunk.label != null && !chunk.label.trim().isEmpty()) {
                builder.append(' ').append(chunk.label.trim());
            }
            if (chunk.text != null && !chunk.text.trim().isEmpty()) {
                builder.append('\n').append(chunk.text.trim());
            }
        }
        return builder.toString().trim();
    }

    private void toast(String text) {
        if (this.mc.player != null) {
            this.mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.GREEN + text));
        }
    }

    private void selectAll() {
        if (activeSection == ViewSection.HEX && packet != null && packet.rawData != null && packet.rawData.length > 0) {
            selectedHexStart = 0;
            selectedHexEnd = packet.rawData.length - 1;
            setStatus("§a已全选当前 HEX 字节", 0xFF8CFF9E);
            return;
        }
        this.anchorLine = 0;
        this.anchorColumn = 0;
        this.cursorLine = lines.size() - 1;
        this.cursorColumn = lines.get(this.cursorLine).length();
        ensureCursorVisible();
        setStatus("§a已全选当前字段", 0xFF8CFF9E);
    }

    private void moveLeft(boolean extendSelection) {
        if (cursorColumn > 0) {
            cursorColumn--;
        } else if (cursorLine > 0) {
            cursorLine--;
            cursorColumn = lines.get(cursorLine).length();
        }
        finishCursorMove(extendSelection);
    }

    private void moveRight(boolean extendSelection) {
        if (cursorColumn < currentLine().length()) {
            cursorColumn++;
        } else if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorColumn = 0;
        }
        finishCursorMove(extendSelection);
    }

    private void moveUp(boolean extendSelection) {
        if (cursorLine > 0) {
            cursorLine--;
            cursorColumn = Math.min(cursorColumn, currentLine().length());
        }
        finishCursorMove(extendSelection);
    }

    private void moveDown(boolean extendSelection) {
        if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorColumn = Math.min(cursorColumn, currentLine().length());
        }
        finishCursorMove(extendSelection);
    }

    private void moveToLineBoundary(boolean home, boolean extendSelection) {
        cursorColumn = home ? 0 : currentLine().length();
        finishCursorMove(extendSelection);
    }

    private void pageMove(int direction, boolean extendSelection) {
        int delta = getVisibleLineCount();
        cursorLine = Math.max(0, Math.min(lines.size() - 1, cursorLine + direction * delta));
        cursorColumn = Math.min(cursorColumn, currentLine().length());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + direction * delta));
        finishCursorMove(extendSelection);
    }

    private void finishCursorMove(boolean extendSelection) {
        clampCursor();
        ensureCursorVisible();
        if (!extendSelection) {
            anchorLine = cursorLine;
            anchorColumn = cursorColumn;
        }
    }

    private void moveCursorFromMouse(int mouseX, int mouseY) {
        int localY = mouseY - getEditorY() - 4;
        int lineIndex = scrollOffset + Math.max(0, localY / getLineHeight());
        lineIndex = Math.max(0, Math.min(lines.size() - 1, lineIndex));
        cursorLine = lineIndex;

        String line = lines.get(cursorLine);
        int textX = getEditorX() + 54;
        int relativeX = Math.max(0, mouseX - textX);
        int bestColumn = 0;
        for (int i = 1; i <= line.length(); i++) {
            if (this.fontRenderer.getStringWidth(line.substring(0, i)) > relativeX) {
                break;
            }
            bestColumn = i;
        }
        cursorColumn = bestColumn;
        ensureCursorVisible();
    }

    private void recalcScrollBounds() {
        maxScroll = Math.max(0, lines.size() - getVisibleLineCount());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void ensureCursorVisible() {
        clampCursor();
        if (cursorLine < scrollOffset) {
            scrollOffset = cursorLine;
        }
        int visible = getVisibleLineCount();
        if (cursorLine >= scrollOffset + visible) {
            scrollOffset = cursorLine - visible + 1;
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void clampCursor() {
        if (lines.isEmpty()) {
            lines.add("");
        }
        cursorLine = Math.max(0, Math.min(cursorLine, lines.size() - 1));
        cursorColumn = Math.max(0, Math.min(cursorColumn, currentLine().length()));
        anchorLine = Math.max(0, Math.min(anchorLine, lines.size() - 1));
        anchorColumn = Math.max(0, Math.min(anchorColumn, lines.get(anchorLine).length()));
    }

    private String currentLine() {
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines.get(Math.max(0, Math.min(cursorLine, lines.size() - 1)));
    }

    private boolean hasSelection() {
        return cursorLine != anchorLine || cursorColumn != anchorColumn;
    }

    private SelectionRange getSelectionRange() {
        if (!hasSelection()) {
            return null;
        }
        if (isBefore(anchorLine, anchorColumn, cursorLine, cursorColumn)) {
            return new SelectionRange(anchorLine, anchorColumn, cursorLine, cursorColumn);
        }
        return new SelectionRange(cursorLine, cursorColumn, anchorLine, anchorColumn);
    }

    private String getSelectedText() {
        SelectionRange range = getSelectionRange();
        if (range == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int lineIndex = range.startLine; lineIndex <= range.endLine; lineIndex++) {
            String line = lines.get(lineIndex);
            int start = lineIndex == range.startLine ? range.startColumn : 0;
            int end = lineIndex == range.endLine ? range.endColumn : line.length();
            start = Math.max(0, Math.min(start, line.length()));
            end = Math.max(0, Math.min(end, line.length()));
            if (end > start) {
                builder.append(line.substring(start, end));
            }
            if (lineIndex < range.endLine) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private int getSelectionLength() {
        String selected = getSelectedText();
        return selected == null ? 0 : selected.length();
    }

    private boolean isBefore(int lineA, int columnA, int lineB, int columnB) {
        return lineA < lineB || (lineA == lineB && columnA <= columnB);
    }

    private String clampVisiblePrefix(String text, int column) {
        int safeColumn = Math.max(0, Math.min(column, text.length()));
        return text.substring(0, safeColumn);
    }

    private boolean blinkCursor() {
        return (System.currentTimeMillis() / 500L) % 2L == 0L;
    }

    private ViewSection getSectionAt(int mouseX, int mouseY) {
        int sidebarX = getSidebarX();
        int sidebarW = getSidebarWidth();
        int rowY = getSidebarY() + 22;
        for (ViewSection section : ViewSection.values()) {
            if (mouseX >= sidebarX + 4 && mouseX <= sidebarX + sidebarW - 4 && mouseY >= rowY
                    && mouseY <= rowY + 20) {
                return section;
            }
            rowY += 24;
        }
        return null;
    }

    private boolean isInsideEditor(int mouseX, int mouseY) {
        return mouseX >= getEditorX() && mouseX <= getEditorX() + getEditorWidth() && mouseY >= getEditorY()
                && mouseY <= getEditorY() + getEditorHeight();
    }

    private boolean isOnScrollbar(int mouseX, int mouseY) {
        if (activeSection == ViewSection.HEX) {
            int editorX = getEditorX();
            int editorY = getEditorY();
            int editorW = getEditorWidth();
            int editorH = getEditorHeight();
            int gap = 10;
            int leftW = Math.max(320, (int) (editorW * 0.57f));
            leftW = Math.min(leftW, editorW - 220);
            int hexScrollX = editorX + 6 + leftW - 12 - 7;
            return maxScroll > 0 && mouseX >= hexScrollX && mouseX <= hexScrollX + 6
                    && mouseY >= editorY + 22 && mouseY <= editorY + editorH - 6;
        }
        int scrollbarX = getEditorX() + getEditorWidth() - 8;
        return maxScroll > 0 && mouseX >= scrollbarX && mouseX <= scrollbarX + 6 && mouseY >= getEditorY()
                && mouseY <= getEditorY() + getEditorHeight();
    }

    private boolean hasHexSelection() {
        return selectedHexStart >= 0 && selectedHexEnd >= 0;
    }

    private int getSelectedHexByteCount() {
        if (!hasHexSelection()) {
            return 0;
        }
        return Math.abs(selectedHexEnd - selectedHexStart) + 1;
    }

    private String getSelectedHexText() {
        if (!hasHexSelection() || packet == null || packet.rawData == null || packet.rawData.length == 0) {
            return "";
        }
        int start = Math.max(0, Math.min(selectedHexStart, selectedHexEnd));
        int end = Math.min(packet.rawData.length - 1, Math.max(selectedHexStart, selectedHexEnd));
        StringBuilder builder = new StringBuilder((end - start + 1) * 3);
        for (int i = start; i <= end; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", packet.rawData[i] & 0xFF));
        }
        return builder.toString();
    }

    private boolean isHexByteSelected(int byteIndex) {
        if (!hasHexSelection()) {
            return false;
        }
        int start = Math.min(selectedHexStart, selectedHexEnd);
        int end = Math.max(selectedHexStart, selectedHexEnd);
        return byteIndex >= start && byteIndex <= end;
    }

    private boolean isChunkIntersectingSelection(PacketPayloadDecoder.DecodedChunk chunk) {
        if (chunk == null || !hasHexSelection()) {
            return false;
        }
        int start = Math.min(selectedHexStart, selectedHexEnd);
        int end = Math.max(selectedHexStart, selectedHexEnd);
        return chunk.endOffset > start && chunk.startOffset <= end;
    }

    private String buildChunkPreview(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private HexPanelLayout buildHexPanelLayout(int x, int y, int width, int height, int byteCount) {
        int rowHeight = 14;
        int offsetWidth = Math.max(38, this.fontRenderer.getStringWidth("0000") + 10);
        int hexWidth = Math.max(10, this.fontRenderer.getStringWidth("00"));
        int asciiCharWidth = Math.max(5, this.fontRenderer.getStringWidth("A"));
        int bestBytesPerRow = 16;
        boolean showAscii = true;
        int bestCellWidth = hexWidth + 2;

        for (int candidate = 16; candidate >= 8; candidate--) {
            int candidateAsciiWidth = candidate * asciiCharWidth + 8;
            int candidateCellWidth = Math.max(hexWidth + 2, (width - offsetWidth - candidateAsciiWidth - 10) / candidate);
            int totalWidth = offsetWidth + candidate * candidateCellWidth + candidateAsciiWidth + 6;
            if (totalWidth <= width) {
                bestBytesPerRow = candidate;
                bestCellWidth = candidateCellWidth;
                showAscii = true;
                break;
            }

            candidateCellWidth = Math.max(hexWidth + 2, (width - offsetWidth - 6) / candidate);
            totalWidth = offsetWidth + candidate * candidateCellWidth + 6;
            if (totalWidth <= width) {
                bestBytesPerRow = candidate;
                bestCellWidth = candidateCellWidth;
                showAscii = false;
                break;
            }
        }

        if (!showAscii) {
            int maxCandidate = Math.max(6, Math.min(16, (width - offsetWidth - 6) / Math.max(1, hexWidth + 2)));
            bestBytesPerRow = Math.max(6, Math.min(bestBytesPerRow, maxCandidate));
            bestCellWidth = Math.max(hexWidth + 2, (width - offsetWidth - 6) / Math.max(1, bestBytesPerRow));
        }

        int visibleRows = Math.max(1, (height - 4) / rowHeight);
        int totalRows = Math.max(1, (byteCount + bestBytesPerRow - 1) / bestBytesPerRow);
        int bytesX = x + offsetWidth;
        int asciiX = bytesX + bestBytesPerRow * bestCellWidth + 8;
        return new HexPanelLayout(x, y, width, height, bestBytesPerRow, rowHeight, visibleRows, totalRows, offsetWidth,
                bestCellWidth, bytesX, asciiX, asciiCharWidth, showAscii);
    }

    private void ensureHexSelectionVisible() {
        if (!hasHexSelection() || packet == null || packet.rawData == null) {
            return;
        }
        int[] rect = getHexBytePanelRect();
        HexPanelLayout layout = buildHexPanelLayout(rect[0], rect[1], rect[2], rect[3], packet.rawData.length);
        int visibleRows = layout.visibleRows;
        int targetRow = Math.max(0, Math.min(selectedHexStart, selectedHexEnd)) / layout.bytesPerRow;
        maxScroll = Math.max(0, layout.totalRows - visibleRows);
        if (targetRow < scrollOffset) {
            scrollOffset = targetRow;
        } else if (targetRow >= scrollOffset + visibleRows) {
            scrollOffset = Math.max(0, targetRow - visibleRows / 2);
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private String buildDirectionTitle() {
        return emptyIfBlank(directionLabel, packet.isFmlPacket ? "FML" : "标准包");
    }

    private String emptyIfBlank(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private int getPanelWidth() {
        return Math.min(1120, this.width - 20);
    }

    private int getPanelHeight() {
        return Math.min(660, this.height - 20);
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getSidebarX() {
        return getPanelX() + 10;
    }

    private int getSidebarY() {
        return getPanelY() + 78;
    }

    private int getSidebarWidth() {
        return 150;
    }

    private int getSidebarHeight() {
        return getPanelHeight() - 116;
    }

    private int getEditorX() {
        return getSidebarX() + getSidebarWidth() + 12;
    }

    private int getEditorY() {
        return getSidebarY();
    }

    private int getEditorWidth() {
        return getPanelX() + getPanelWidth() - getEditorX() - 10;
    }

    private int getEditorHeight() {
        return getSidebarHeight();
    }

    private int getVisibleLineCount() {
        return Math.max(1, (getEditorHeight() - 8) / getLineHeight());
    }

    private int getLineHeight() {
        return 10;
    }

    private boolean isInsideHexBytePanel(int mouseX, int mouseY) {
        int[] rect = getHexBytePanelRect();
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] && mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }

    private boolean isInsideDecodedChunkPanel(int mouseX, int mouseY) {
        int[] rect = getDecodedChunkPanelRect();
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] && mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }

    private int[] getHexBytePanelRect() {
        int editorX = getEditorX();
        int editorY = getEditorY();
        int editorW = getEditorWidth();
        int editorH = getEditorHeight();
        int gap = 10;
        int leftW = Math.max(320, (int) (editorW * 0.57f));
        leftW = Math.min(leftW, editorW - 220);
        return new int[] { editorX + 6, editorY + 22, leftW - 12, editorH - 28 };
    }

    private int[] getDecodedChunkPanelRect() {
        int editorX = getEditorX();
        int editorY = getEditorY();
        int editorW = getEditorWidth();
        int editorH = getEditorHeight();
        int gap = 10;
        int leftW = Math.max(320, (int) (editorW * 0.57f));
        leftW = Math.min(leftW, editorW - 220);
        int rightW = editorW - leftW - gap;
        int rightX = editorX + leftW + gap + 6;
        return new int[] { rightX, editorY + 22, rightW - 12, editorH - 28 };
    }

    private Integer getHexByteIndexAt(int mouseX, int mouseY) {
        if (packet == null || packet.rawData == null || packet.rawData.length == 0) {
            return null;
        }
        int[] rect = getHexBytePanelRect();
        if (mouseX < rect[0] || mouseX > rect[0] + rect[2] || mouseY < rect[1] || mouseY > rect[1] + rect[3]) {
            return null;
        }

        HexPanelLayout layout = buildHexPanelLayout(rect[0], rect[1], rect[2], rect[3], packet.rawData.length);
        int relativeY = mouseY - (rect[1] + 4);
        if (relativeY < 0) {
            return null;
        }
        int row = relativeY / layout.rowHeight;
        int col = (mouseX - layout.bytesX) / layout.cellWidth;
        if (col < 0 || col >= layout.bytesPerRow) {
            return null;
        }
        int byteIndex = (scrollOffset + row) * layout.bytesPerRow + col;
        return byteIndex >= 0 && byteIndex < packet.rawData.length ? byteIndex : null;
    }

    private Integer getDecodedChunkIndexAt(int mouseX, int mouseY) {
        int[] rect = getDecodedChunkPanelRect();
        if (mouseX < rect[0] || mouseX > rect[0] + rect[2] || mouseY < rect[1] || mouseY > rect[1] + rect[3]) {
            return null;
        }
        int rowHeight = 26;
        int localY = mouseY - (rect[1] + 4);
        if (localY < 0) {
            return null;
        }
        int row = localY / rowHeight;
        int index = decodedChunkScrollOffset + row;
        return decodedChunks != null && index >= 0 && index < decodedChunks.size() ? index : null;
    }

    private int getVisibleChunkCount() {
        int[] rect = getDecodedChunkPanelRect();
        return Math.max(1, (rect[3] - 4) / 30);
    }

    public enum ViewSection {
        OVERVIEW("概览"),
        CLASS_NAME("类名"),
        CHANNEL_OR_ID("频道/ID"),
        HEX("HEX"),
        DECODED("解码"),
        DECODE_DETAIL("解码详情");

        private final String label;

        ViewSection(String label) {
            this.label = label;
        }
    }

    private void drawDetailTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (mouseX >= getSidebarX() && mouseX <= getSidebarX() + getSidebarWidth()
                && mouseY >= getSidebarY() && mouseY <= getSidebarY() + getSidebarHeight()) {
            tooltip = "左侧字段导航。\n点击可在类名、频道/ID、HEX、解码详情等不同视图之间切换。";
        } else if (activeSection == ViewSection.HEX && isInsideHexBytePanel(mouseX, mouseY)) {
            tooltip = "HEX 字节面板。\n可逐字节查看、框选复制，并和右侧解码块对照。";
        } else if (activeSection != ViewSection.HEX && isInsideEditor(mouseX, mouseY)) {
            tooltip = "文本详情区。\n可查看当前字段完整内容，支持选中复制和光标定位。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case BUTTON_BACK:
                        tooltip = "返回抓包查看器。";
                        break;
                    case BUTTON_COPY_SELECTION:
                        tooltip = "复制当前选中的 HEX 或文本内容。";
                        break;
                    case BUTTON_COPY_FULL:
                        tooltip = "复制当前字段的完整内容。";
                        break;
                    case BUTTON_COPY_DECODE_CHUNKS:
                        tooltip = "复制当前包的解码块列表，适合做结构分析。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private static final class SelectionRange {
        private final int startLine;
        private final int startColumn;
        private final int endLine;
        private final int endColumn;

        private SelectionRange(int startLine, int startColumn, int endLine, int endColumn) {
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
        }
    }
}


