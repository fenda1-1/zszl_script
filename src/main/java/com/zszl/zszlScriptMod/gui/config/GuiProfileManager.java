package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.ProfileShareCodeManager;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GuiProfileManager extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    private List<String> profiles = new ArrayList<>();
    private List<String> shareableFiles = new ArrayList<>();
    private List<String> filteredShareableFiles = new ArrayList<>();
    private final Set<String> checkedFiles = new LinkedHashSet<>();

    private int selectedProfileIndex = -1;
    private int selectedFileIndex = -1;
    private int lastFileAnchorIndex = -1;

    private int profileScroll = 0;
    private int profileMaxScroll = 0;
    private int fileScroll = 0;
    private int fileMaxScroll = 0;
    private int previewScroll = 0;
    private int previewMaxScroll = 0;

    private String previewContent = "";
    private String statusMessage = "§7支持查看配置、单独编辑保存、Ctrl/Shift 多选并生成分享码";
    private int statusColor = 0xFFB8C7D9;
    private String fileSearchQuery = "";

    private GuiButton btnSelect;
    private GuiButton btnDelete;
    private GuiButton btnOpenEditor;
    private GuiButton btnExportShare;
    private GuiButton btnImportShare;
    private GuiButton btnSelectAllFiles;
    private GuiButton btnClearFileSelection;
    private GuiButton btnRefresh;
    private GuiTextField fileSearchField;

    public GuiProfileManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        refreshProfiles();
        refreshFilesForSelectedProfile();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();

        int actionX = panelX + 10;
        int actionWidth = panelWidth - 20;
        int gap = 8;
        int cols = 5;
        int buttonWidth = Math.max(48, (actionWidth - gap * (cols - 1)) / cols);
        int row1Y = panelY + getPanelHeight() - 52;
        int row2Y = panelY + getPanelHeight() - 28;

        btnSelect = new ThemedButton(0, actionX, row1Y, buttonWidth, 20, I18n.format("gui.profile.select"));
        this.buttonList.add(btnSelect);
        this.buttonList.add(new ThemedButton(1, actionX + (buttonWidth + gap), row1Y, buttonWidth, 20,
                I18n.format("gui.profile.create")));
        btnDelete = new ThemedButton(2, actionX + 2 * (buttonWidth + gap), row1Y, buttonWidth, 20,
                I18n.format("gui.profile.delete"));
        this.buttonList.add(btnDelete);
        btnOpenEditor = new ThemedButton(3, actionX + 3 * (buttonWidth + gap), row1Y, buttonWidth, 20, "§b编辑当前");
        this.buttonList.add(btnOpenEditor);
        btnRefresh = new ThemedButton(4, actionX + 4 * (buttonWidth + gap), row1Y, buttonWidth, 20, "§e刷新");
        this.buttonList.add(btnRefresh);

        btnExportShare = new ThemedButton(5, actionX, row2Y, buttonWidth, 20, "§a复制分享码");
        this.buttonList.add(btnExportShare);
        btnImportShare = new ThemedButton(6, actionX + (buttonWidth + gap), row2Y, buttonWidth, 20, "§d导入分享码");
        this.buttonList.add(btnImportShare);
        btnSelectAllFiles = new ThemedButton(7, actionX + 2 * (buttonWidth + gap), row2Y, buttonWidth, 20, "§a全选文件");
        this.buttonList.add(btnSelectAllFiles);
        btnClearFileSelection = new ThemedButton(8, actionX + 3 * (buttonWidth + gap), row2Y, buttonWidth, 20,
                "§7清空勾选");
        this.buttonList.add(btnClearFileSelection);
        this.buttonList.add(new ThemedButton(9, actionX + 4 * (buttonWidth + gap), row2Y, buttonWidth, 20,
                I18n.format("gui.common.done")));

        fileSearchField = new GuiTextField(100, this.fontRenderer, getFileSearchX() + 4, getFileSearchY() + 5,
                getFileSearchWidth() - 8, 10);
        fileSearchField.setMaxStringLength(128);
        fileSearchField.setCanLoseFocus(true);
        fileSearchField.setEnableBackgroundDrawing(false);
        fileSearchField.setText(fileSearchQuery == null ? "" : fileSearchQuery);

        applyFileFilter(getSelectedFilePath());
        updateButtonStates();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private void refreshProfiles() {
        this.profiles = ProfileManager.getAllProfileNames();
        if (profiles.isEmpty()) {
            selectedProfileIndex = -1;
            profileScroll = 0;
            return;
        }

        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            selectedProfileIndex = profiles.indexOf(ProfileManager.getActiveProfileName());
            if (selectedProfileIndex < 0) {
                selectedProfileIndex = 0;
            }
        }
        recalcProfileScroll();
    }

    private void refreshFilesForSelectedProfile() {
        String preferredPath = getSelectedFilePath();
        String profileName = getSelectedProfileName();
        if (profileName == null) {
            shareableFiles = new ArrayList<>();
            filteredShareableFiles = new ArrayList<>();
            selectedFileIndex = -1;
            previewContent = "";
            checkedFiles.clear();
            lastFileAnchorIndex = -1;
            recalcFileScroll();
            recalcPreviewScroll();
            return;
        }

        shareableFiles = new ArrayList<>(ProfileShareCodeManager.listShareableFiles(profileName));
        checkedFiles.retainAll(shareableFiles);
        applyFileFilter(preferredPath);

        recalcFileScroll();
        recalcPreviewScroll();
    }

    private void applyFileFilter(String preferredPath) {
        filteredShareableFiles = new ArrayList<>();
        for (String path : shareableFiles) {
            if (matchesFileFilter(path)) {
                filteredShareableFiles.add(path);
            }
        }

        if (filteredShareableFiles.isEmpty()) {
            selectedFileIndex = -1;
            lastFileAnchorIndex = -1;
            previewContent = "";
            previewScroll = 0;
            recalcFileScroll();
            recalcPreviewScroll();
            return;
        }

        int newIndex = preferredPath == null ? -1 : filteredShareableFiles.indexOf(preferredPath);
        if (newIndex < 0 && selectedFileIndex >= 0) {
            newIndex = Math.min(selectedFileIndex, filteredShareableFiles.size() - 1);
        }
        if (newIndex < 0) {
            newIndex = 0;
        }

        selectedFileIndex = newIndex;
        if (lastFileAnchorIndex < 0 || lastFileAnchorIndex >= filteredShareableFiles.size()) {
            lastFileAnchorIndex = selectedFileIndex;
        }
        loadPreviewForSelectedFile();
        recalcFileScroll();
        recalcPreviewScroll();
    }

    private boolean matchesFileFilter(String path) {
        String keyword = normalizeSearchText(fileSearchQuery);
        if (keyword.isEmpty()) {
            return true;
        }
        return PinyinSearchHelper.matchesNormalized(path, keyword)
                || PinyinSearchHelper.matchesNormalized(getDisplayNameForFile(path), keyword)
                || PinyinSearchHelper.matchesNormalized(getDisplayLabelForFile(path), keyword);
    }

    private String normalizeSearchText(String text) {
        return PinyinSearchHelper.normalizeQuery(text == null ? "" : text.replace('\\', '/'));
    }

    private void loadPreviewForSelectedFile() {
        String profileName = getSelectedProfileName();
        String relativePath = getSelectedFilePath();
        if (profileName == null || relativePath == null) {
            previewContent = "";
            recalcPreviewScroll();
            return;
        }

        try {
            previewContent = ProfileShareCodeManager.loadProfileFileContent(profileName, relativePath);
        } catch (Exception e) {
            previewContent = "读取失败: " + e.getMessage();
        }
        previewScroll = 0;
        recalcPreviewScroll();
    }

    private void updateButtonStates() {
        boolean hasProfile = getSelectedProfileName() != null;
        boolean isActive = hasProfile && getSelectedProfileName().equals(ProfileManager.getActiveProfileName());
        boolean isDefault = hasProfile && ProfileManager.DEFAULT_PROFILE_NAME.equals(getSelectedProfileName());
        boolean hasFile = getSelectedFilePath() != null;
        boolean hasChecked = !checkedFiles.isEmpty() || hasFile;
        boolean hasVisibleFiles = !filteredShareableFiles.isEmpty();

        btnSelect.enabled = hasProfile && !isActive;
        btnDelete.enabled = hasProfile && !isDefault;
        btnOpenEditor.enabled = hasProfile && hasFile;
        btnExportShare.enabled = hasProfile && hasChecked;
        btnImportShare.enabled = hasProfile;
        btnSelectAllFiles.enabled = hasProfile && hasVisibleFiles;
        btnClearFileSelection.enabled = !checkedFiles.isEmpty();
        btnRefresh.enabled = hasProfile;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                if (btnSelect.enabled) {
                    ProfileManager.setActiveProfile(getSelectedProfileName());
                    setStatus("§a已切换到配置: " + getSelectedProfileName(), 0xFF8CFF9E);
                    refreshProfiles();
                    refreshFilesForSelectedProfile();
                    updateButtonStates();
                }
                break;
            case 1:
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.profile.input_new"), newName -> {
                    if (newName != null && !newName.trim().isEmpty()) {
                        if (ProfileManager.createProfile(newName.trim())) {
                            profiles = ProfileManager.getAllProfileNames();
                            selectedProfileIndex = profiles.indexOf(newName.trim());
                            checkedFiles.clear();
                            selectedFileIndex = 0;
                            lastFileAnchorIndex = 0;
                            refreshFilesForSelectedProfile();
                            setStatus("§a已创建配置: " + newName.trim(), 0xFF8CFF9E);
                        } else {
                            setStatus("§c创建失败，名称可能重复或无效", 0xFFFF8E8E);
                        }
                        updateButtonStates();
                    }
                }));
                break;
            case 2:
                if (btnDelete.enabled) {
                    String profile = getSelectedProfileName();
                    if (ProfileManager.deleteProfile(profile)) {
                        setStatus("§a已删除配置: " + profile, 0xFF8CFF9E);
                        refreshProfiles();
                        checkedFiles.clear();
                        selectedFileIndex = 0;
                        lastFileAnchorIndex = 0;
                        refreshFilesForSelectedProfile();
                        updateButtonStates();
                    } else {
                        setStatus("§c删除失败: " + profile, 0xFFFF8E8E);
                    }
                }
                break;
            case 3:
                openEditorForSelectedFile();
                break;
            case 4:
                refreshProfiles();
                refreshFilesForSelectedProfile();
                updateButtonStates();
                setStatus("§a已刷新配置视图", 0xFF8CFF9E);
                break;
            case 5:
                exportShareCode();
                break;
            case 6:
                openImportDialog();
                break;
            case 7:
                checkedFiles.clear();
                checkedFiles.addAll(filteredShareableFiles);
                updateButtonStates();
                setStatus("§a已选择当前列表中的配置文件，共 " + checkedFiles.size() + " 项", 0xFF8CFF9E);
                break;
            case 8:
                checkedFiles.clear();
                updateButtonStates();
                setStatus("§7已清空分享勾选", 0xFFB8C7D9);
                break;
            case 9:
                mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    private void openEditorForSelectedFile() {
        String profileName = getSelectedProfileName();
        String relativePath = getSelectedFilePath();
        if (profileName == null || relativePath == null) {
            return;
        }
        try {
            String content = ProfileShareCodeManager.loadProfileFileContent(profileName, relativePath);
            mc.setScreen(new GuiProfileConfigEditor(this, profileName, relativePath, content));
        } catch (Exception e) {
            setStatus("§c打开编辑器失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void exportShareCode() {
        String profileName = getSelectedProfileName();
        if (profileName == null) {
            return;
        }

        List<String> exportFiles = new ArrayList<>();
        if (!checkedFiles.isEmpty()) {
            exportFiles.addAll(checkedFiles);
        } else if (getSelectedFilePath() != null) {
            exportFiles.add(getSelectedFilePath());
        }

        try {
            String shareCode = ProfileShareCodeManager.generateShareCode(profileName, exportFiles);
            setClipboardString(shareCode);
            setStatus("§a分享码已复制到剪贴板，长度: " + shareCode.length() + "，文件数: " + exportFiles.size(),
                    0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c生成分享码失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void openImportDialog() {
        final String targetProfile = getSelectedProfileName();
        if (targetProfile == null) {
            return;
        }
        mc.setScreen(new GuiTextInput(this, "粘贴分享码到当前配置: " + targetProfile, code -> {
            if (code == null || code.trim().isEmpty()) {
                setStatus("§7已取消导入", 0xFFB8C7D9);
                return;
            }
            try {
                ProfileShareCodeManager.ImportPreview preview = ProfileShareCodeManager.previewImport(code,
                        targetProfile);
                mc.setScreen(new GuiShareImportPreview(this, targetProfile, preview));
            } catch (Exception e) {
                setStatus("§c导入失败: " + e.getMessage(), 0xFFFF8E8E);
            }
        }));
    }

    public void handleImportApplied(ProfileShareCodeManager.ImportResult result) {
        refreshFilesForSelectedProfile();
        updateButtonStates();
        if (result == null) {
            setStatus("§a导入已完成", 0xFF8CFF9E);
            return;
        }
        setStatus("§a导入完成：写入 " + result.getImportedCount()
                + " 项，替换 " + result.getReplacedFileCount()
                + " 项，合并 " + result.getMergedFileCount()
                + " 项，跳过 " + result.getUnchangedFileCount() + " 项",
                0xFF8CFF9E);
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

        if (isInside(mouseX, mouseY, getProfileListX(), getListY(), getProfileListWidth(), getListHeight())) {
            if (dWheel > 0) {
                profileScroll = Math.max(0, profileScroll - 1);
            } else {
                profileScroll = Math.min(profileMaxScroll, profileScroll + 1);
            }
            return;
        }

        if (isInside(mouseX, mouseY, getFileListX(), getListY(), getFileListWidth(), getListHeight())) {
            if (dWheel > 0) {
                fileScroll = Math.max(0, fileScroll - 1);
            } else {
                fileScroll = Math.min(fileMaxScroll, fileScroll + 1);
            }
            return;
        }

        if (isInside(mouseX, mouseY, getPreviewX(), getPreviewY(), getPreviewWidth(), getPreviewHeight())) {
            if (dWheel > 0) {
                previewScroll = Math.max(0, previewScroll - 3);
            } else {
                previewScroll = Math.min(previewMaxScroll, previewScroll + 3);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (fileSearchField != null) {
            fileSearchField.mouseClicked(mouseX, mouseY, mouseButton);
            if (isInside(mouseX, mouseY, getFileSearchX(), getFileSearchY(), getFileSearchWidth(), getFileSearchHeight())) {
                return;
            }
        }

        if (mouseButton != 0) {
            return;
        }

        handleProfileListClick(mouseX, mouseY);
        handleFileListClick(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (fileSearchField != null) {
            if (keyCode == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown()) {
                fileSearchField.setFocused(true);
                return;
            }

            if (fileSearchField.isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    fileSearchField.setFocused(false);
                    return;
                }

                String preferredPath = getSelectedFilePath();
                String before = fileSearchField.getText();
                if (fileSearchField.textboxKeyTyped(typedChar, keyCode)) {
                    fileSearchQuery = fileSearchField.getText();
                    if (!before.equals(fileSearchQuery)) {
                        applyFileFilter(preferredPath);
                        updateButtonStates();
                    }
                    return;
                }
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void handleProfileListClick(int mouseX, int mouseY) {
        int contentY = getListContentY();
        if (!isInside(mouseX, mouseY, getProfileListX(), contentY, getProfileListWidth(), getListHeight() - 24)) {
            return;
        }

        int localIndex = (mouseY - contentY) / getRowHeight();
        int actualIndex = profileScroll + localIndex;
        if (actualIndex >= 0 && actualIndex < profiles.size()) {
            selectedProfileIndex = actualIndex;
            checkedFiles.clear();
            selectedFileIndex = 0;
            lastFileAnchorIndex = 0;
            refreshFilesForSelectedProfile();
            updateButtonStates();
        }
    }

    private void handleFileListClick(int mouseX, int mouseY) {
        int contentY = getFileListContentY();
        if (!isInside(mouseX, mouseY, getFileListX(), contentY, getFileListWidth(), getFileListContentHeight())) {
            return;
        }

        int localIndex = (mouseY - contentY) / getRowHeight();
        int actualIndex = fileScroll + localIndex;
        if (actualIndex < 0 || actualIndex >= filteredShareableFiles.size()) {
            return;
        }

        String path = filteredShareableFiles.get(actualIndex);
        boolean shiftDown = GuiScreen.isShiftKeyDown();
        boolean ctrlDown = GuiScreen.isCtrlKeyDown();
        int anchorBeforeClick = lastFileAnchorIndex;

        int checkboxX = getFileListX() + 10;
        if (mouseX >= checkboxX && mouseX <= checkboxX + 12) {
            if (checkedFiles.contains(path)) {
                checkedFiles.remove(path);
            } else {
                checkedFiles.add(path);
            }
            selectedFileIndex = actualIndex;
            lastFileAnchorIndex = actualIndex;
            loadPreviewForSelectedFile();
            updateButtonStates();
            return;
        }

        selectedFileIndex = actualIndex;

        if (shiftDown && !filteredShareableFiles.isEmpty()) {
            int anchor = anchorBeforeClick < 0 ? actualIndex : anchorBeforeClick;
            int min = Math.min(anchor, actualIndex);
            int max = Math.max(anchor, actualIndex);
            checkedFiles.clear();
            for (int i = min; i <= max; i++) {
                checkedFiles.add(filteredShareableFiles.get(i));
            }
        } else if (ctrlDown) {
            lastFileAnchorIndex = actualIndex;
            if (checkedFiles.contains(path)) {
                checkedFiles.remove(path);
            } else {
                checkedFiles.add(path);
            }
        } else {
            checkedFiles.clear();
            checkedFiles.add(path);
            lastFileAnchorIndex = actualIndex;
        }

        if (shiftDown && anchorBeforeClick < 0) {
            lastFileAnchorIndex = actualIndex;
        }

        loadPreviewForSelectedFile();
        updateButtonStates();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.profile.title"), this.fontRenderer);

        GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 26, panelWidth - 20, 18, false, true);
        this.drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 31, statusColor);

        drawProfileList(mouseX, mouseY);
        drawFileList(mouseX, mouseY);
        drawPreviewPanel();

        String filterHint = fileSearchQuery == null || fileSearchQuery.trim().isEmpty()
                ? "§7未过滤"
                : "§b筛选: " + filteredShareableFiles.size() + "/" + shareableFiles.size();
        this.drawString(this.fontRenderer,
                "§7已勾选分享文件: " + checkedFiles.size()
                        + " | 当前配置: " + (getSelectedProfileName() == null ? "-" : getSelectedProfileName())
                        + " | " + filterHint,
                panelX + 10, panelY + panelHeight - 70, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawProfileList(int mouseX, int mouseY) {
        int x = getProfileListX();
        int y = getListY();
        int width = getProfileListWidth();
        int height = getListHeight();

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "配置方案", this.fontRenderer);

        int contentY = getListContentY();
        int visibleRows = Math.max(1, (height - 30) / getRowHeight());
        profileMaxScroll = Math.max(0, profiles.size() - visibleRows);
        profileScroll = Math.max(0, Math.min(profileScroll, profileMaxScroll));

        if (profiles.isEmpty()) {
            GuiTheme.drawEmptyState(x + width / 2, y + height / 2 - 6, I18n.format("gui.profile.empty"),
                    this.fontRenderer);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = profileScroll + i;
            if (index >= profiles.size()) {
                break;
            }

            String profile = profiles.get(index);
            int rowY = contentY + i * getRowHeight();
            boolean isActive = profile.equals(ProfileManager.getActiveProfileName());
            boolean isSelected = index == selectedProfileIndex;
            boolean hovered = isInside(mouseX, mouseY, x + 6, rowY, width - 16, getRowHeight() - 2);

            GuiTheme.drawButtonFrameSafe(x + 6, rowY, width - 18, getRowHeight() - 2,
                    isSelected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String displayName = profile;
            if (isActive) {
                displayName = "§a" + profile + " " + I18n.format("gui.profile.current");
            } else if (profile.equals(ProfileManager.DEFAULT_PROFILE_NAME)) {
                displayName = "§e" + profile + " " + I18n.format("gui.profile.default");
            }
            displayName = this.fontRenderer.trimStringToWidth(displayName, width - 28);
            this.drawString(this.fontRenderer, displayName, x + 12, rowY + 6, 0xFFE8F1FA);
        }

        if (profileMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, profiles.size())) * (height - 30)));
            int track = Math.max(1, (height - 30) - thumbHeight);
            int thumbY = contentY + (int) ((profileScroll / (float) Math.max(1, profileMaxScroll)) * track);
            GuiTheme.drawScrollbar(x + width - 10, contentY, 6, height - 30, thumbY, thumbHeight);
        }
    }

    private void drawFileList(int mouseX, int mouseY) {
        int x = getFileListX();
        int y = getListY();
        int width = getFileListWidth();
        int height = getListHeight();

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "配置文件（Ctrl/Shift 多选，点击复选框分享）", this.fontRenderer);

        GuiTheme.drawInputFrameSafe(getFileSearchX(), getFileSearchY(), getFileSearchWidth(), getFileSearchHeight(),
                fileSearchField != null && fileSearchField.isFocused(), true);
        if (fileSearchField != null) {
            fileSearchField.drawTextBox();
            if ((fileSearchField.getText() == null || fileSearchField.getText().isEmpty()) && !fileSearchField.isFocused()) {
                this.drawString(this.fontRenderer, "§7搜索文件名 / 中文名 / 路径，Ctrl+F 快速聚焦",
                        getFileSearchX() + 5, getFileSearchY() + 5, 0xFF7D8A9A);
            }
        }

        this.drawString(this.fontRenderer,
                "§7显示 " + filteredShareableFiles.size() + " / " + shareableFiles.size(),
                x + width - 84, y + 8, 0xFF9EB3C9);

        int contentY = getFileListContentY();
        int contentHeight = getFileListContentHeight();
        int visibleRows = Math.max(1, contentHeight / getRowHeight());
        fileMaxScroll = Math.max(0, filteredShareableFiles.size() - visibleRows);
        fileScroll = Math.max(0, Math.min(fileScroll, fileMaxScroll));

        if (filteredShareableFiles.isEmpty()) {
            String emptyText = shareableFiles.isEmpty() ? "该配置下暂无可分享文件" : "没有匹配当前搜索条件的配置文件";
            GuiTheme.drawEmptyState(x + width / 2, y + height / 2 - 6, emptyText, this.fontRenderer);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = fileScroll + i;
            if (index >= filteredShareableFiles.size()) {
                break;
            }

            String path = filteredShareableFiles.get(index);
            int rowY = contentY + i * getRowHeight();
            boolean isSelected = index == selectedFileIndex;
            boolean isChecked = checkedFiles.contains(path);
            boolean hovered = isInside(mouseX, mouseY, x + 6, rowY, width - 16, getRowHeight() - 2);

            GuiTheme.drawButtonFrameSafe(x + 6, rowY, width - 18, getRowHeight() - 2,
                    isSelected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER
                                    : (isChecked ? GuiTheme.UiState.SUCCESS : GuiTheme.UiState.NORMAL)));

            drawRect(x + 10, rowY + 5, x + 22, rowY + 17, isChecked ? 0xFF3A9F5B : 0xFF2B3440);
            drawRect(x + 11, rowY + 6, x + 21, rowY + 16, isChecked ? 0xFF67D58B : 0xFF17202A);
            if (isChecked) {
                this.drawString(this.fontRenderer, "√", x + 13, rowY + 6, 0xFFFFFFFF);
            }

            String clipped = this.fontRenderer.trimStringToWidth(getDisplayLabelForFile(path), width - 40);
            this.drawString(this.fontRenderer, clipped, x + 28, rowY + 6, 0xFFE8F1FA);
        }

        if (fileMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, filteredShareableFiles.size())) * contentHeight));
            int track = Math.max(1, contentHeight - thumbHeight);
            int thumbY = contentY + (int) ((fileScroll / (float) Math.max(1, fileMaxScroll)) * track);
            GuiTheme.drawScrollbar(x + width - 10, contentY, 6, contentHeight, thumbY, thumbHeight);
        }
    }

    private void drawPreviewPanel() {
        int x = getPreviewX();
        int y = getPreviewY();
        int width = getPreviewWidth();
        int height = getPreviewHeight();

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "内容预览 / 单文件编辑保存", this.fontRenderer);

        if (getSelectedFilePath() != null) {
            this.drawString(this.fontRenderer,
                    "§7当前文件: " + getDisplayNameForFile(getSelectedFilePath()) + " (" + getSelectedFilePath() + ")",
                    x + 8, y + 20, 0xFFB8C7D9);
        } else {
            this.drawString(this.fontRenderer, "§7请先在中间列表选择一个配置文件", x + 8, y + 20, 0xFFB8C7D9);
        }

        int contentX = x + 8;
        int contentY = y + 36;
        int contentW = width - 18;
        int contentH = height - 44;

        GuiTheme.drawInputFrameSafe(contentX, contentY, contentW, contentH, false, true);

        List<String> lines = splitPreviewLines(previewContent);
        int visibleLines = Math.max(1, (contentH - 8) / 10);
        previewMaxScroll = Math.max(0, lines.size() - visibleLines);
        previewScroll = Math.max(0, Math.min(previewScroll, previewMaxScroll));

        for (int i = 0; i < visibleLines; i++) {
            int index = previewScroll + i;
            if (index >= lines.size()) {
                break;
            }
            String line = this.fontRenderer.trimStringToWidth(lines.get(index), contentW - 10);
            this.drawString(this.fontRenderer, line, contentX + 4, contentY + 4 + i * 10, 0xFFE8F1FA);
        }

        if (previewMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleLines / (float) Math.max(visibleLines, lines.size())) * contentH));
            int track = Math.max(1, contentH - thumbHeight);
            int thumbY = contentY + (int) ((previewScroll / (float) Math.max(1, previewMaxScroll)) * track);
            GuiTheme.drawScrollbar(contentX + contentW - 6, contentY, 4, contentH, thumbY, thumbHeight);
        }
    }

    private List<String> splitPreviewLines(String content) {
        List<String> result = new ArrayList<>();
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        String[] split = normalized.split("\n", -1);
        for (String line : split) {
            result.add(line);
        }
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    private String getDisplayLabelForFile(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        String displayName = getDisplayNameForFile(path);
        if (displayName.equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return displayName + " §7(" + normalized + ")";
    }

    private String getDisplayNameForFile(String path) {
        return ProfileShareCodeManager.getDisplayNameForPath(path);
    }

    private void recalcProfileScroll() {
        profileScroll = Math.max(0, profileScroll);
    }

    private void recalcFileScroll() {
        fileScroll = Math.max(0, fileScroll);
    }

    private void recalcPreviewScroll() {
        previewScroll = Math.max(0, previewScroll);
    }

    private String getSelectedProfileName() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            return null;
        }
        return profiles.get(selectedProfileIndex);
    }

    private String getSelectedFilePath() {
        if (selectedFileIndex < 0 || selectedFileIndex >= filteredShareableFiles.size()) {
            return null;
        }
        return filteredShareableFiles.get(selectedFileIndex);
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int[] getColumnWidths() {
        int total = getPanelWidth() - 40;
        int profileWidth = Math.max(160, Math.min(220, total / 4));
        int preferredFileWidth = getPreferredFileListWidth();
        int fileWidth = Math.max(190, Math.min(320, preferredFileWidth));
        int previewWidth = total - profileWidth - fileWidth;

        if (previewWidth < 360) {
            int need = 360 - previewWidth;
            int reducible = Math.max(0, fileWidth - 190);
            int reduce = Math.min(need, reducible);
            fileWidth -= reduce;
            previewWidth += reduce;
        }
        if (previewWidth < 300) {
            int need = 300 - previewWidth;
            int reducible = Math.max(0, profileWidth - 150);
            int reduce = Math.min(need, reducible);
            profileWidth -= reduce;
            previewWidth += reduce;
        }
        if (previewWidth < 260) {
            previewWidth = 260;
            fileWidth = Math.max(170, total - profileWidth - previewWidth);
        }
        return new int[] { profileWidth, fileWidth, total - profileWidth - fileWidth };
    }

    private int getPreferredFileListWidth() {
        int preferred = 220;
        if (this.fontRenderer != null) {
            for (String path : shareableFiles) {
                preferred = Math.max(preferred, this.fontRenderer.getStringWidth(getDisplayLabelForFile(path)) + 42);
            }
        }
        return preferred;
    }

    private int getPanelWidth() {
        return Math.min(1180, this.width - 12);
    }

    private int getPanelHeight() {
        return Math.min(680, this.height - 12);
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getListY() {
        return getPanelY() + 50;
    }

    private int getListContentY() {
        return getListY() + 24;
    }

    private int getListHeight() {
        return getPanelHeight() - 132;
    }

    private int getProfileListX() {
        return getPanelX() + 10;
    }

    private int getProfileListWidth() {
        return getColumnWidths()[0];
    }

    private int getFileListX() {
        return getProfileListX() + getProfileListWidth() + 10;
    }

    private int getFileListWidth() {
        return getColumnWidths()[1];
    }

    private int getPreviewX() {
        return getFileListX() + getFileListWidth() + 10;
    }

    private int getPreviewY() {
        return getListY();
    }

    private int getPreviewWidth() {
        return getColumnWidths()[2];
    }

    private int getPreviewHeight() {
        return getListHeight();
    }

    private int getRowHeight() {
        return 20;
    }

    private int getFileSearchX() {
        return getFileListX() + 8;
    }

    private int getFileSearchY() {
        return getListY() + 24;
    }

    private int getFileSearchWidth() {
        return getFileListWidth() - 18;
    }

    private int getFileSearchHeight() {
        return 18;
    }

    private int getFileListContentY() {
        return getFileSearchY() + 22;
    }

    private int getFileListContentHeight() {
        return getListHeight() - 52;
    }
}







