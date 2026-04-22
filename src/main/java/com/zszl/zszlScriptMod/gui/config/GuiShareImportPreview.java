package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.ProfileShareCodeManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiShareImportPreview extends ThemedGuiScreen {

    private final GuiProfileManager parentScreen;
    private final String targetProfileName;
    private final ProfileShareCodeManager.ImportPreview preview;
    private final List<ProfileShareCodeManager.ImportPreviewEntry> entries;
    private final Set<String> selectedPaths = new LinkedHashSet<>();

    private int listScroll = 0;
    private int listMaxScroll = 0;
    private int detailScroll = 0;
    private int detailMaxScroll = 0;
    private int selectedIndex = 0;

    private GuiButton btnImport;
    private GuiButton btnSelectAll;
    private GuiButton btnClearAll;

    private String statusMessage = "§7请确认每个文件的导入策略：右侧显示本地 → 导入后的差异预览（红删绿增）";
    private int statusColor = 0xFFB8C7D9;

    public GuiShareImportPreview(GuiProfileManager parentScreen, String targetProfileName,
            ProfileShareCodeManager.ImportPreview preview) {
        this.parentScreen = parentScreen;
        this.targetProfileName = targetProfileName == null ? "" : targetProfileName;
        this.preview = preview;
        this.entries = preview == null
                ? new ArrayList<ProfileShareCodeManager.ImportPreviewEntry>()
                : new ArrayList<>(preview.getEntries());

        for (ProfileShareCodeManager.ImportPreviewEntry entry : this.entries) {
            if (entry != null) {
                selectedPaths.add(entry.getRelativePath());
            }
        }
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int bottomY = panelY + getPanelHeight() - 28;

        this.buttonList.add(new ThemedButton(0, panelX + 10, bottomY, 110, 20, "§a确认导入所选"));
        this.buttonList.add(new ThemedButton(1, panelX + 128, bottomY, 90, 20, "§a全选"));
        this.buttonList.add(new ThemedButton(2, panelX + 226, bottomY, 90, 20, "§7清空"));
        this.buttonList.add(new ThemedButton(3, panelX + getPanelWidth() - 100, bottomY, 90, 20, "§c返回"));

        btnImport = this.buttonList.get(0);
        btnSelectAll = this.buttonList.get(1);
        btnClearAll = this.buttonList.get(2);

        clampSelection();
        updateButtonStates();
    }

    private void updateButtonStates() {
        btnImport.enabled = !selectedPaths.isEmpty();
        btnSelectAll.enabled = !entries.isEmpty() && selectedPaths.size() < entries.size();
        btnClearAll.enabled = !selectedPaths.isEmpty();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                applySelectedImport();
                break;
            case 1:
                selectedPaths.clear();
                for (ProfileShareCodeManager.ImportPreviewEntry entry : entries) {
                    selectedPaths.add(entry.getRelativePath());
                }
                updateButtonStates();
                setStatus("§a已全选所有导入项", 0xFF8CFF9E);
                break;
            case 2:
                selectedPaths.clear();
                updateButtonStates();
                setStatus("§7已取消全部导入勾选", 0xFFB8C7D9);
                break;
            case 3:
                this.mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    private void applySelectedImport() {
        try {
            ProfileShareCodeManager.ImportResult result = ProfileShareCodeManager.applyImportPreview(preview,
                    selectedPaths);
            parentScreen.handleImportApplied(result);
            this.mc.setScreen(parentScreen);
        } catch (Exception e) {
            setStatus("§c导入失败: " + e.getMessage(), 0xFFFF8E8E);
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

        if (isInside(mouseX, mouseY, getListX(), getContentY(), getListWidth(), getContentHeight())) {
            if (dWheel > 0) {
                listScroll = Math.max(0, listScroll - 1);
            } else {
                listScroll = Math.min(listMaxScroll, listScroll + 1);
            }
            return;
        }

        if (isInside(mouseX, mouseY, getDetailX(), getContentY(), getDetailWidth(), getContentHeight())) {
            if (dWheel > 0) {
                detailScroll = Math.max(0, detailScroll - 2);
            } else {
                detailScroll = Math.min(detailMaxScroll, detailScroll + 2);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        handleListClick(mouseX, mouseY);
    }

    private void handleListClick(int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY, getListX(), getContentY(), getListWidth(), getContentHeight())) {
            return;
        }

        int localIndex = (mouseY - getContentY()) / getRowHeight();
        int actualIndex = listScroll + localIndex;
        if (actualIndex < 0 || actualIndex >= entries.size()) {
            return;
        }

        ProfileShareCodeManager.ImportPreviewEntry entry = entries.get(actualIndex);
        selectedIndex = actualIndex;
        detailScroll = 0;

        int checkboxX = getListX() + 10;
        if (mouseX >= checkboxX && mouseX <= checkboxX + 12) {
            toggleSelection(entry.getRelativePath());
        } else {
            if (GuiScreen.isCtrlKeyDown()) {
                toggleSelection(entry.getRelativePath());
            }
        }

        updateButtonStates();
    }

    private void toggleSelection(String relativePath) {
        if (relativePath == null) {
            return;
        }
        if (selectedPaths.contains(relativePath)) {
            selectedPaths.remove(relativePath);
        } else {
            selectedPaths.add(relativePath);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        switch (keyCode) {
            case 1:
                this.mc.setScreen(parentScreen);
                return;
            case 200:
                if (selectedIndex > 0) {
                    selectedIndex--;
                    ensureSelectionVisible();
                }
                return;
            case 208:
                if (selectedIndex < entries.size() - 1) {
                    selectedIndex++;
                    ensureSelectionVisible();
                }
                return;
            case 28:
            case 156:
                if (btnImport.enabled) {
                    applySelectedImport();
                }
                return;
            case 57:
                if (selectedIndex >= 0 && selectedIndex < entries.size()) {
                    toggleSelection(entries.get(selectedIndex).getRelativePath());
                    updateButtonStates();
                }
                return;
            default:
                break;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelW = getPanelWidth();
        int panelH = getPanelHeight();

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "导入分享码预览 - " + targetProfileName, this.fontRenderer);

        GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 26, panelW - 20, 18, false, true);
        this.drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 31, statusColor);

        drawListPanel(mouseX, mouseY);
        drawDetailPanel();

        this.drawString(this.fontRenderer,
                "§7共 " + entries.size() + " 项 | 已勾选 " + selectedPaths.size() + " 项 | 有变化 "
                        + (preview == null ? 0 : preview.getChangedCount()) + " 项",
                panelX + 10, panelY + panelH - 42, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawListPanel(int mouseX, int mouseY) {
        int x = getListX();
        int y = getTopY();
        int width = getListWidth();
        int height = getPanelHeight() - 102;

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "导入项目（勾选=实际导入）", this.fontRenderer);

        int visibleRows = Math.max(1, getContentHeight() / getRowHeight());
        listMaxScroll = Math.max(0, entries.size() - visibleRows);
        listScroll = Math.max(0, Math.min(listScroll, listMaxScroll));

        if (entries.isEmpty()) {
            GuiTheme.drawEmptyState(x + width / 2, y + height / 2 - 6, "分享码中没有可导入的配置", this.fontRenderer);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = listScroll + i;
            if (index >= entries.size()) {
                break;
            }

            ProfileShareCodeManager.ImportPreviewEntry entry = entries.get(index);
            boolean selected = index == selectedIndex;
            boolean hovered = isInside(mouseX, mouseY, x + 6, getContentY() + i * getRowHeight(), width - 16,
                    getRowHeight() - 2);
            boolean checked = selectedPaths.contains(entry.getRelativePath());

            GuiTheme.drawButtonFrameSafe(x + 6, getContentY() + i * getRowHeight(), width - 18, getRowHeight() - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER
                                    : (checked ? GuiTheme.UiState.SUCCESS : GuiTheme.UiState.NORMAL)));

            int rowY = getContentY() + i * getRowHeight();
            drawRect(x + 10, rowY + 5, x + 22, rowY + 17, checked ? 0xFF3A9F5B : 0xFF2B3440);
            drawRect(x + 11, rowY + 6, x + 21, rowY + 16, checked ? 0xFF67D58B : 0xFF17202A);
            if (checked) {
                this.drawString(this.fontRenderer, "√", x + 13, rowY + 6, 0xFFFFFFFF);
            }

            String strategy = entry.getStrategy() == ProfileShareCodeManager.ImportStrategy.MERGE ? "§a合并" : "§e替换";
            String display = this.fontRenderer.trimStringToWidth(getDisplayNameForFile(entry.getRelativePath()),
                    width - 78);
            this.drawString(this.fontRenderer, display, x + 28, rowY + 4, 0xFFE8F1FA);
            this.drawString(this.fontRenderer, strategy, x + width - 48, rowY + 4, 0xFFE8F1FA);

            String summary = entry.hasChanges() ? entry.getSummary() : "无变化，导入后不会修改";
            this.drawString(this.fontRenderer,
                    this.fontRenderer.trimStringToWidth("§7" + summary, width - 40),
                    x + 28, rowY + 13, 0xFF9EB1C5);
        }

        if (listMaxScroll > 0) {
            int thumbHeight = Math.max(18, (int) ((visibleRows / (float) Math.max(visibleRows, entries.size()))
                    * getContentHeight()));
            int track = Math.max(1, getContentHeight() - thumbHeight);
            int thumbY = getContentY() + (int) ((listScroll / (float) Math.max(1, listMaxScroll)) * track);
            GuiTheme.drawScrollbar(x + width - 10, getContentY(), 6, getContentHeight(), thumbY, thumbHeight);
        }
    }

    private void drawDetailPanel() {
        int x = getDetailX();
        int y = getTopY();
        int width = getDetailWidth();
        int height = getPanelHeight() - 102;

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "导入策略详情", this.fontRenderer);

        if (entries.isEmpty()) {
            GuiTheme.drawEmptyState(x + width / 2, y + height / 2 - 6, "没有可显示的详情", this.fontRenderer);
            return;
        }

        clampSelection();
        ProfileShareCodeManager.ImportPreviewEntry entry = entries.get(selectedIndex);

        this.drawString(this.fontRenderer,
                "§7文件: " + getDisplayNameForFile(entry.getRelativePath()) + " (" + entry.getRelativePath() + ")",
                x + 8, y + 22, 0xFFB8C7D9);
        this.drawString(this.fontRenderer,
                "§7模式: " + entry.getStrategy().getDisplayName() + " | "
                        + (entry.hasChanges() ? "§a有变化" : "§7无变化"),
                x + 8, y + 34, 0xFFB8C7D9);

        int contentX = x + 8;
        int contentY = y + 48;
        int contentW = width - 18;
        int contentH = height - 56;

        GuiTheme.drawInputFrameSafe(contentX, contentY, contentW, contentH, false, true);

        List<String> lines = buildDetailLines(entry);

        int visibleLines = Math.max(1, (contentH - 8) / 10);
        detailMaxScroll = Math.max(0, lines.size() - visibleLines);
        detailScroll = Math.max(0, Math.min(detailScroll, detailMaxScroll));

        for (int i = 0; i < visibleLines; i++) {
            int index = detailScroll + i;
            if (index >= lines.size()) {
                break;
            }
            String line = this.fontRenderer.trimStringToWidth(lines.get(index), contentW - 10);
            this.drawString(this.fontRenderer, line, contentX + 4, contentY + 4 + i * 10, 0xFFE8F1FA);
        }

        if (detailMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleLines / (float) Math.max(visibleLines, lines.size())) * contentH));
            int track = Math.max(1, contentH - thumbHeight);
            int thumbY = contentY + (int) ((detailScroll / (float) Math.max(1, detailMaxScroll)) * track);
            GuiTheme.drawScrollbar(contentX + contentW - 6, contentY, 4, contentH, thumbY, thumbHeight);
        }
    }

    private void ensureSelectionVisible() {
        clampSelection();
        int visibleRows = Math.max(1, getContentHeight() / getRowHeight());
        if (selectedIndex < listScroll) {
            listScroll = selectedIndex;
        } else if (selectedIndex >= listScroll + visibleRows) {
            listScroll = selectedIndex - visibleRows + 1;
        }
        listScroll = Math.max(0, Math.min(listScroll, listMaxScroll));
        detailScroll = 0;
    }

    private void clampSelection() {
        if (entries.isEmpty()) {
            selectedIndex = -1;
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, entries.size() - 1));
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private List<String> buildDetailLines(ProfileShareCodeManager.ImportPreviewEntry entry) {
        List<String> lines = new ArrayList<>();
        if (entry == null) {
            lines.add("§7没有可显示的详情");
            return lines;
        }

        lines.add("§f" + entry.getSummary());
        lines.add("");
        lines.add("§7策略: " + entry.getStrategy().getDisplayName()
                + " | 状态: " + (entry.hasChanges() ? "§a有变化" : "§7无变化"));
        lines.add("§7本地行数: " + splitContentLines(entry.getExistingContent()).size()
                + " | 分享码行数: " + splitContentLines(entry.getImportedContent()).size()
                + " | 导入后行数: " + splitContentLines(entry.getFinalContent()).size());

        for (String detail : entry.getDetailLines()) {
            lines.add("§7" + detail);
        }

        lines.add("");
        if (!normalizeContent(entry.getImportedContent()).equals(normalizeContent(entry.getFinalContent()))) {
            lines.add("§d提示: 当前为智能合并结果，右侧预览的是“最终导入后”的内容，而不是分享码原始内容");
            lines.add("");
        }

        lines.add("§b差异预览（本地 -> 导入后）");
        lines.add("§8红色删除线 = 将被删除；绿色 = 将新增；灰色 = 保留");
        lines.addAll(buildDiffLines(entry.getExistingContent(), entry.getFinalContent()));

        return lines;
    }

    private List<String> buildDiffLines(String beforeContent, String afterContent) {
        List<String> beforeLines = splitContentLines(beforeContent);
        List<String> afterLines = splitContentLines(afterContent);

        long complexity = (long) beforeLines.size() * (long) afterLines.size();
        if (complexity > 120000L) {
            return buildSimpleDiffLines(beforeLines, afterLines);
        }

        int n = beforeLines.size();
        int m = afterLines.size();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (beforeLines.get(i).equals(afterLines.get(j))) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        List<String> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            String before = beforeLines.get(i);
            String after = afterLines.get(j);
            if (before.equals(after)) {
                result.add("§7  " + formatDiffText(before));
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                result.add("§c§m- " + formatDiffText(before));
                i++;
            } else {
                result.add("§a+ " + formatDiffText(after));
                j++;
            }
        }

        while (i < n) {
            result.add("§c§m- " + formatDiffText(beforeLines.get(i++)));
        }
        while (j < m) {
            result.add("§a+ " + formatDiffText(afterLines.get(j++)));
        }

        if (result.isEmpty()) {
            result.add("§7（无可视差异）");
        }
        return result;
    }

    private List<String> buildSimpleDiffLines(List<String> beforeLines, List<String> afterLines) {
        List<String> result = new ArrayList<>();
        int max = Math.max(beforeLines.size(), afterLines.size());
        for (int i = 0; i < max; i++) {
            String before = i < beforeLines.size() ? beforeLines.get(i) : null;
            String after = i < afterLines.size() ? afterLines.get(i) : null;
            if (before != null && after != null && before.equals(after)) {
                result.add("§7  " + formatDiffText(before));
            } else {
                if (before != null) {
                    result.add("§c§m- " + formatDiffText(before));
                }
                if (after != null) {
                    result.add("§a+ " + formatDiffText(after));
                }
            }
        }
        if (result.isEmpty()) {
            result.add("§7（无可视差异）");
        }
        return result;
    }

    private List<String> splitContentLines(String content) {
        List<String> result = new ArrayList<>();
        String normalized = normalizeContent(content);
        String[] lines = normalized.split("\n", -1);
        Collections.addAll(result, lines);
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String formatDiffText(String text) {
        return text == null || text.isEmpty() ? "§8<空行>" : text;
    }

    private String getDisplayNameForFile(String path) {
        return ProfileShareCodeManager.getDisplayNameForPath(path);
    }

    private int getPanelWidth() {
        return Math.min(980, this.width - 16);
    }

    private int getPanelHeight() {
        return Math.min(620, this.height - 16);
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getTopY() {
        return getPanelY() + 50;
    }

    private int getContentY() {
        return getTopY() + 24;
    }

    private int getContentHeight() {
        return getPanelHeight() - 126;
    }

    private int getListX() {
        return getPanelX() + 10;
    }

    private int getListWidth() {
        return 340;
    }

    private int getDetailX() {
        return getListX() + getListWidth() + 10;
    }

    private int getDetailWidth() {
        return getPanelWidth() - 30 - getListWidth();
    }

    private int getRowHeight() {
        return 24;
    }
}





