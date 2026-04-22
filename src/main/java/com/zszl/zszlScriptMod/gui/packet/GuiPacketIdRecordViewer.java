package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuiPacketIdRecordViewer extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    private final List<PacketIdRecordManager.PacketIdRecord> allRecords = new ArrayList<>();
    private final List<PacketIdRecordManager.PacketIdRecord> filteredRecords = new ArrayList<>();

    private GuiTextField filterField;
    private GuiButton btnReload;
    private GuiButton btnClear;
    private GuiButton btnCopySelected;

    private final Set<Integer> selectedIndices = new HashSet<>();
    private int lastSelectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public GuiPacketIdRecordViewer(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelWidth = Math.min(720, this.width - 30);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 300) / 2;

        String oldFilter = filterField == null ? "" : filterField.getText();
        filterField = new GuiTextField(10, fontRenderer, panelX + 10, panelY + 12, panelWidth - 20, 18);
        filterField.setMaxStringLength(Integer.MAX_VALUE);
        filterField.setFocused(true);
        filterField.setText(oldFilter);

        btnReload = new ThemedButton(1, panelX + 10, panelY + 265, 120, 20, "重新加载");
        btnClear = new ThemedButton(2, panelX + 140, panelY + 265, 120, 20, "清空记录");
        btnCopySelected = new ThemedButton(3, panelX + 270, panelY + 265, 140, 20, "复制所选(0)");
        this.buttonList.add(btnReload);
        this.buttonList.add(btnClear);
        this.buttonList.add(btnCopySelected);
        this.buttonList.add(new ThemedButton(0, panelX + panelWidth - 120, panelY + 265, 110, 20, "返回"));

        reloadRecords();
        updateButtonStates();
    }

    private void reloadRecords() {
        allRecords.clear();
        allRecords.addAll(PacketIdRecordManager.listRecords());
        applyFilter();
    }

    private void applyFilter() {
        String keyword = PinyinSearchHelper.normalizeQuery(filterField == null ? "" : safe(filterField.getText()));
        filteredRecords.clear();

        if (keyword.isEmpty()) {
            filteredRecords.addAll(allRecords);
        } else {
            for (PacketIdRecordManager.PacketIdRecord record : allRecords) {
                String idText = record.packetId == null ? "" : String.format("0x%02x", record.packetId);
                String idDec = record.packetId == null ? "" : String.valueOf(record.packetId);
                String searchText = safe(record.packetClassName) + " "
                        + safe(record.direction) + " "
                        + safe(record.channel) + " "
                        + idText + " " + idDec;
                if (PinyinSearchHelper.matchesNormalized(searchText, keyword)) {
                    filteredRecords.add(record);
                }
            }
        }

        selectedIndices.clear();
        lastSelectedIndex = -1;
        scrollOffset = 0;
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (btnCopySelected != null) {
            btnCopySelected.enabled = !selectedIndices.isEmpty();
            btnCopySelected.displayString = "复制所选(" + selectedIndices.size() + ")";
        }
    }

    private void copySelectedRecords() {
        if (selectedIndices.isEmpty()) {
            return;
        }

        List<Integer> sorted = new ArrayList<>(selectedIndices);
        Collections.sort(sorted);

        StringBuilder sb = new StringBuilder();
        for (int index : sorted) {
            if (index < 0 || index >= filteredRecords.size()) {
                continue;
            }
            PacketIdRecordManager.PacketIdRecord record = filteredRecords.get(index);
            sb.append(record.getDisplayDirection())
                    .append(" | ")
                    .append(safe(record.packetClassName))
                    .append(" | ")
                    .append(record.getIdOrChannelText())
                    .append(" | 次数=")
                    .append(Math.max(1, record.occurrenceCount))
                    .append(" | 首次=")
                    .append(formatFullTime(record.firstSeenAt))
                    .append(" | 最近=")
                    .append(formatFullTime(record.lastSeenAt))
                    .append("\n");
        }

        setClipboardString(sb.toString().trim());
        if (mc.player != null) {
            mc.player.sendSystemMessage(new TextComponentString(
                    ChatFormatting.GREEN + "已复制 " + selectedIndices.size() + " 条数据包ID记录"));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                mc.setScreen(parentScreen);
                break;
            case 1:
                reloadRecords();
                break;
            case 2:
                if (PacketIdRecordManager.clearRecords()) {
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.GREEN + "已清空数据包ID记录"));
                    }
                    selectedIndices.clear();
                    lastSelectedIndex = -1;
                    reloadRecords();
                }
                break;
            case 3:
                copySelectedRecords();
                break;
            default:
                break;
        }
        updateButtonStates();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (filterField.textboxKeyTyped(typedChar, keyCode)) {
            applyFilter();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        filterField.mouseClicked(mouseX, mouseY, mouseButton);

        int panelWidth = Math.min(720, this.width - 30);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 300) / 2;
        int listX = panelX + 10;
        int listY = panelY + 40;
        int listWidth = panelWidth - 20;
        int listHeight = 215;
        int itemHeight = 24;

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int clicked = (mouseY - listY) / itemHeight + scrollOffset;
            if (clicked >= 0 && clicked < filteredRecords.size()) {
                if (mouseButton == 0) {
                    applySelectionByModifier(clicked);
                }
            }
        }

        updateButtonStates();
    }

    private void applySelectionByModifier(int actualIndex) {
        if (isShiftKeyDown() && lastSelectedIndex != -1) {
            selectedIndices.clear();
            int start = Math.min(lastSelectedIndex, actualIndex);
            int end = Math.max(lastSelectedIndex, actualIndex);
            for (int i = start; i <= end; i++) {
                selectedIndices.add(i);
            }
        } else if (isCtrlKeyDown()) {
            if (selectedIndices.contains(actualIndex)) {
                selectedIndices.remove(actualIndex);
            } else {
                selectedIndices.add(actualIndex);
            }
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
        int dWheel = Mouse.getDWheel();
        if (dWheel == 0) {
            return;
        }
        if (dWheel > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = Math.min(720, this.width - 30);
        int panelHeight = 300;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "数据包ID自动记录", this.fontRenderer);

        drawThemedTextField(filterField);
        if (filterField.getText().isEmpty() && !filterField.isFocused()) {
            drawString(fontRenderer, "§8按包名 / ID / 频道 / 方向过滤", panelX + 14, panelY + 17, 0xFFFFFF);
        }

        int listX = panelX + 10;
        int listY = panelY + 40;
        int listWidth = panelWidth - 20;
        int listHeight = 215;
        int itemHeight = 24;
        int visible = Math.max(1, listHeight / itemHeight);
        maxScroll = Math.max(0, filteredRecords.size() - visible);

        drawString(fontRenderer, "§7方向", listX + 8, listY - 10, 0xFFFFFF);
        drawString(fontRenderer, "§7包名", listX + 60, listY - 10, 0xFFFFFF);
        drawString(fontRenderer, "§7ID/频道", listX + 300, listY - 10, 0xFFFFFF);
        drawString(fontRenderer, "§7次数", listX + 470, listY - 10, 0xFFFFFF);
        drawString(fontRenderer, "§7最近时间", listX + 540, listY - 10, 0xFFFFFF);

        if (filteredRecords.isEmpty()) {
            drawCenteredString(this.fontRenderer, "§7暂无自动记录", this.width / 2, listY + 90, 0xAAAAAA);
        } else {
            for (int i = 0; i < visible; i++) {
                int index = i + scrollOffset;
                if (index >= filteredRecords.size()) {
                    break;
                }
                PacketIdRecordManager.PacketIdRecord record = filteredRecords.get(index);
                int y = listY + i * itemHeight;
                int bg = selectedIndices.contains(index) ? 0xFF0066AA : 0x80444444;
                drawRect(listX, y, listX + listWidth, y + itemHeight - 1, bg);

                String directionText = "C2S".equalsIgnoreCase(record.getDisplayDirection()) ? "§aC2S"
                        : ("S2C".equalsIgnoreCase(record.getDisplayDirection()) ? "§bS2C" : "§7" + record.getDisplayDirection());

                drawString(fontRenderer, directionText, listX + 8, y + 8, 0xFFFFFF);
                drawString(fontRenderer, trimToWidth(safe(record.packetClassName), 220), listX + 60, y + 8, 0xFFFFFF);
                drawString(fontRenderer, trimToWidth(record.getIdOrChannelText(), 155), listX + 300, y + 8, 0xFFFFFF);
                drawString(fontRenderer, String.valueOf(Math.max(1, record.occurrenceCount)), listX + 470, y + 8, 0xFFFFFF);
                drawString(fontRenderer, formatTime(record.lastSeenAt), listX + 540, y + 8, 0xFFFFFF);
            }
        }

        if (maxScroll > 0) {
            int scrollbarX = listX + listWidth - 6;
            drawRect(scrollbarX, listY, scrollbarX + 5, listY + listHeight, 0xFF202020);
            int thumbHeight = Math.max(10, (int) ((float) visible / filteredRecords.size() * listHeight));
            int thumbY = listY + (int) ((float) scrollOffset / maxScroll * (listHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF888888);
        }

        drawString(fontRenderer, "§7支持 Ctrl 多选、Shift 区间多选，然后点“复制所选”", listX, panelY + 287, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawIdRecordTooltip(mouseX, mouseY, panelX, panelY, panelWidth, listX, listY, listWidth, listHeight);
    }

    private void drawIdRecordTooltip(int mouseX, int mouseY, int panelX, int panelY, int panelWidth,
            int listX, int listY, int listWidth, int listHeight) {
        String tooltip = null;
        if (isMouseOverField(mouseX, mouseY, filterField)) {
            tooltip = "按包名、方向、频道或 Packet ID 过滤自动记录结果。";
        } else if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            tooltip = "自动记录到的 Packet ID 列表。\n支持 Ctrl 多选、Shift 连选，再点“复制所选”导出。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case 0:
                        tooltip = "返回上一页。";
                        break;
                    case 1:
                        tooltip = "重新加载 Packet ID 自动记录结果。";
                        break;
                    case 2:
                        tooltip = "清空当前所有 Packet ID 自动记录。";
                        break;
                    case 3:
                        tooltip = "复制当前选中的记录行，方便做规则整理或外部备份。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0L) {
            return "N/A";
        }
        return new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private String formatFullTime(long timestamp) {
        if (timestamp <= 0L) {
            return "N/A";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && fontRenderer.getStringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }
}








