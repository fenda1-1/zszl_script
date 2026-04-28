// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/debug/GuiMemoryComparison.java
package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.system.MemoryManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet; // !! 修复：添加缺失的导入 !!
import java.util.List;
import java.util.Map;
import java.util.Set; // !! 修复：添加缺失的导入 !!

public class GuiMemoryComparison extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final MemoryManager.ComparisonResult result;

    private List<String> displayLines = new ArrayList<>();

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;
    private int listTop, listBottom, listHeight;

    public GuiMemoryComparison(GuiScreen parent, MemoryManager.ComparisonResult result) {
        this.parentScreen = parent;
        this.result = result;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int buttonWidth = 120;
        int spacing = 10;
        int totalButtonWidth = buttonWidth * 2 + spacing;
        int startX = (this.width - totalButtonWidth) / 2;

        this.buttonList
                .add(new GuiButton(0, startX, this.height - 30, buttonWidth, 20, I18n.format("gui.common.back")));
        this.buttonList.add(new GuiButton(1, startX + buttonWidth + spacing, this.height - 30, buttonWidth, 20,
                I18n.format("gui.memory.compare.copy_report")));

        listTop = 45;
        listBottom = this.height - 40;
        listHeight = listBottom - listTop;

        displayLines.clear();

        // 实体
        displayLines.add(I18n.format("gui.memory.compare.section.entities"));
        addDeltaLines(displayLines, result.before.entityMemoryUsage, result.after.entityMemoryUsage,
                result.before.entityCounts, result.after.entityCounts);

        // 方块实体
        displayLines.add(" ");
        displayLines.add(I18n.format("gui.memory.compare.section.tile_entities"));
        addDeltaLines(displayLines, result.before.tileEntityMemoryUsage, result.after.tileEntityMemoryUsage,
                result.before.tileEntityCounts, result.after.tileEntityCounts);

        // 区块
        displayLines.add(" ");
        displayLines.add(I18n.format("gui.memory.compare.section.chunks"));
        addDeltaLines(displayLines, result.before.chunkMemoryUsage, result.after.chunkMemoryUsage,
                result.before.chunkCounts, result.after.chunkCounts);

        // 渲染区块
        displayLines.add(" ");
        displayLines.add(I18n.format("gui.memory.compare.section.render_chunks"));
        addDeltaLines(displayLines, result.before.renderChunkMemoryUsage, result.after.renderChunkMemoryUsage,
                result.before.renderChunkCounts, result.after.renderChunkCounts);
    }

    private void addDeltaLines(List<String> lines, Map<String, Long> beforeMem, Map<String, Long> afterMem,
            Map<String, Integer> beforeCount, Map<String, Integer> afterCount) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(beforeMem.keySet());
        allKeys.addAll(afterMem.keySet());

        List<String> changedItems = new ArrayList<>();
        for (String key : allKeys) {
            long memDelta = afterMem.getOrDefault(key, 0L) - beforeMem.getOrDefault(key, 0L);
            int countDelta = afterCount.getOrDefault(key, 0) - beforeCount.getOrDefault(key, 0);

            if (memDelta != 0 || countDelta != 0) {
                String memStr = (memDelta == 0) ? "§7±0 B"
                        : String.format("%s%s", memDelta > 0 ? "§a+" : "§c", formatBytes(memDelta));
                String countStr = (countDelta == 0) ? "§7±0"
                        : String.format("%s%d", countDelta > 0 ? "§a+" : "§c", countDelta);
                changedItems.add(String.format("%s (%s) §f%s", memStr, countStr, key));
            }
        }

        if (changedItems.isEmpty()) {
            lines.add(I18n.format("gui.memory.compare.no_changes"));
        } else {
            changedItems.sort((s1, s2) -> {
                long delta1 = parseDeltaFromFormattedString(s1);
                long delta2 = parseDeltaFromFormattedString(s2);
                return Long.compare(Math.abs(delta2), Math.abs(delta1));
            });
            lines.addAll(changedItems);
        }
    }

    private long parseDeltaFromFormattedString(String formatted) {
        try {
            String memPart = formatted.split(" ")[0];
            memPart = memPart.replaceAll("§[a-z0-9]", "").replace("+", "").replace("B", "");
            double value;
            long multiplier = 1;
            if (memPart.endsWith("K")) {
                multiplier = 1024;
                value = Double.parseDouble(memPart.substring(0, memPart.length() - 1));
            } else if (memPart.endsWith("M")) {
                multiplier = 1024 * 1024;
                value = Double.parseDouble(memPart.substring(0, memPart.length() - 1));
            } else if (memPart.endsWith("G")) {
                multiplier = 1024 * 1024 * 1024;
                value = Double.parseDouble(memPart.substring(0, memPart.length() - 1));
            } else {
                value = Double.parseDouble(memPart);
            }
            return (long) (value * multiplier);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        } else if (button.id == 1) {
            setClipboardString(generateReportString());
            if (mc.player != null) {
                mc.player.displayClientMessage(new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString(
                        net.minecraft.ChatFormatting.GREEN
                                + I18n.format("msg.memory.compare.report_copied")), false);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int panelWidth = this.width - 40;
        int panelHeight = this.height - 60;
        int panelX = 20;
        int panelY = 20;

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        String title = I18n.format("gui.memory.compare.title",
            result.before.name, result.before.getFormattedTimestamp(),
            result.after.name, result.after.getFormattedTimestamp());
        drawCenteredString(fontRenderer, title, centerX, panelY + 10, 0xFFFFFF);

        long usedBefore = result.before.usedMemory / 1024 / 1024;
        long usedAfter = result.after.usedMemory / 1024 / 1024;
        long delta = usedAfter - usedBefore;
        String color = delta > 0 ? "§c+" : (delta < 0 ? "§a" : "§7");
        String memoryInfo = I18n.format("gui.memory.compare.jvm_usage", usedBefore, usedAfter, color,
            delta);
        drawCenteredString(fontRenderer, memoryInfo, centerX, panelY + 25, 0xFFFFFF);

        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int visibleLines = listHeight / lineHeight;
        maxScroll = Math.max(0, displayLines.size() - visibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < visibleLines; i++) {
            int index = i + scrollOffset;
            if (index >= displayLines.size())
                break;
            drawString(fontRenderer, displayLines.get(index), panelX + 10, listTop + i * lineHeight, 0xFFFFFF);
        }

        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 8;
            drawRect(scrollbarX, listTop, scrollbarX + 6, listBottom, 0xFF101010);
            int thumbHeight = Math.max(10, (int) ((float) visibleLines / displayLines.size() * listHeight));
            int thumbY = listTop + (int) ((float) scrollOffset / maxScroll * (listHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            int panelWidth = this.width - 40;
            int panelX = 20;
            int scrollbarX = panelX + panelWidth - 8;
            if (maxScroll > 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 6 && mouseY >= listTop
                    && mouseY < listBottom) {
                isDraggingScrollbar = true;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingScrollbar) {
            int visibleLines = listHeight / (fontRenderer.FONT_HEIGHT + 2);
            int thumbHeight = Math.max(10, (int) ((float) visibleLines / displayLines.size() * listHeight));
            float scrollPercent = (float) (mouseY - listTop - thumbHeight / 2) / (listHeight - thumbHeight);
            scrollPercent = Math.max(0.0f, Math.min(1.0f, scrollPercent));
            scrollOffset = (int) (scrollPercent * maxScroll);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            isDraggingScrollbar = false;
        }
    }

    private String generateReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.format("gui.memory.compare.report_header",
            result.before.name, result.before.getFormattedTimestamp(),
            result.after.name, result.after.getFormattedTimestamp())).append("\n");
        sb.append("========================================\n");
        long usedBefore = result.before.usedMemory / 1024 / 1024;
        long usedAfter = result.after.usedMemory / 1024 / 1024;
        long delta = usedAfter - usedBefore;
        sb.append(I18n.format("gui.memory.compare.jvm_usage_plain", usedBefore, usedAfter,
            delta >= 0 ? "+" : "", delta)).append("\n\n");

        for (String line : displayLines) {
            sb.append(line.replaceAll("§[a-z0-9]", "")).append("\n");
        }

        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes == 0)
            return "0 B";
        String sign = bytes < 0 ? "-" : "";
        bytes = Math.abs(bytes);
        if (bytes < 1024)
            return sign + bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%s%.2f %sB", sign, bytes / Math.pow(1024, exp), pre);
    }
}







