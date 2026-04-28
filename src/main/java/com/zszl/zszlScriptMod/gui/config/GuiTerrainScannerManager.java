// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/config/GuiTerrainScannerManager.java
// (这是修复了刷新问题的最终版本)
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiConfirmOpenLink;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNo;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNoCallback;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.utils.TerrainScanManager;
import com.zszl.zszlScriptMod.utils.TerrainScannerHandler;

import java.io.IOException;
import java.util.List;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiTerrainScannerManager extends ThemedGuiScreen implements GuiYesNoCallback {

    private final GuiScreen parentScreen;
    private List<String> scanFiles;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private GuiButton btnView, btnRename, btnDelete;

    public GuiTerrainScannerManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        // 每次initGui都会重新从磁盘加载，确保数据最新
        this.scanFiles = TerrainScanManager.getAllScanNames();

        this.buttonList.clear();
        int panelWidth = 350;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 240) / 2;

        // 列表下方的操作按钮
        this.buttonList
                .add(new ThemedButton(0, panelX + 10, panelY + 185, 70, 20, I18n.format("gui.scan_manager.create")));
        btnView = new ThemedButton(1, panelX + 90, panelY + 185, 70, 20, I18n.format("gui.scan_manager.view"));
        btnRename = new ThemedButton(2, panelX + 170, panelY + 185, 70, 20, I18n.format("gui.scan_manager.rename"));
        btnDelete = new ThemedButton(3, panelX + 250, panelY + 185, 70, 20, I18n.format("gui.scan_manager.delete"));
        this.buttonList.add(btnView);
        this.buttonList.add(btnRename);
        this.buttonList.add(btnDelete);

        // 底部按钮
        this.buttonList
                .add(new ThemedButton(4, panelX + 10, panelY + 215, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.scan_manager.clear_all")));
        this.buttonList
                .add(new ThemedButton(5, panelX + 20 + (panelWidth - 30) / 2, panelY + 215, (panelWidth - 30) / 2,
                        20, I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean isSelected = selectedIndex != -1;
        btnView.enabled = isSelected;
        btnRename.enabled = isSelected;
        btnDelete.enabled = isSelected;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // 新建扫描
                // 新建扫描后会关闭GUI，下次打开时会自动刷新，所以这里不需要特殊处理
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.scan_manager.input_radius"),
                        String.valueOf(TerrainScanManager.loadLastRadius()), radiusStr -> {
                            try {
                                int radius = Integer.parseInt(radiusStr);
                                if (radius > 0 && radius <= 50) {
                                    TerrainScanManager.saveLastRadius(radius);
                                    TerrainScannerHandler.scanAndSaveTerrain(radius);
                                    mc.setScreen(null); // 关闭所有GUI开始扫描
                                } else {
                                    // 半径无效，返回管理器界面
                                    mc.setScreen(this);
                                }
                            } catch (NumberFormatException e) {
                                mc.setScreen(this);
                            }
                        }));
                break;
            case 1: // 查看
                if (selectedIndex != -1) {
                    String fileName = scanFiles.get(selectedIndex);
                    String content = TerrainScanManager.readScanContent(fileName);
                    mc.setScreen(new GuiScanViewer(this, fileName, content));
                }
                break;
            case 2: // 重命名
                if (selectedIndex != -1) {
                    String oldName = scanFiles.get(selectedIndex);
                    mc.setScreen(new GuiTextInput(this, I18n.format("gui.scan_manager.input_new_name"), oldName,
                            newName -> {
                                if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
                                    TerrainScanManager.renameScan(oldName, newName.trim());
                                }
                                // !! 核心修复：返回管理器界面后，强制刷新 !!
                                mc.setScreen(this);
                                this.initGui();
                            }));
                }
                break;
            case 3: // 删除
                if (selectedIndex != -1) {
                    String fileName = scanFiles.get(selectedIndex);
                    mc.setScreen(new GuiYesNo((GuiYesNoCallback) this, I18n.format("gui.scan_manager.confirm_delete.title"),
                            I18n.format("gui.scan_manager.confirm_delete.message", fileName),
                            I18n.format("gui.scan_manager.delete"), I18n.format("gui.common.cancel"), 0));
                }
                break;
            case 4: // 全部清除
                mc.setScreen(new GuiYesNo((GuiYesNoCallback) this, I18n.format("gui.scan_manager.confirm_clear_all.title"),
                        I18n.format("gui.scan_manager.confirm_clear_all.message"),
                        I18n.format("gui.scan_manager.clear_all"),
                        I18n.format("gui.common.cancel"), 1));
                break;
            case 5: // 完成
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            if (id == 0) { // 删除单个
                if (selectedIndex != -1) {
                    TerrainScanManager.deleteScan(scanFiles.get(selectedIndex));
                    selectedIndex = -1;
                }
            } else if (id == 1) { // 全部清除
                TerrainScanManager.deleteAllScans();
                selectedIndex = -1;
            }
        }
        mc.setScreen(this);
        // !! 核心修复：从确认框返回后，强制刷新 !!
        this.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 350;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.scan_manager.title"), this.fontRenderer);

        int listY = panelY + 40;
        int listHeight = 140;
        int itemHeight = 20;
        int visibleItems = listHeight / itemHeight;
        maxScroll = Math.max(0, scanFiles.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= scanFiles.size())
                break;

            String fileName = scanFiles.get(index);
            int itemY = listY + i * itemHeight;
            int bgColor = (index == selectedIndex) ? 0xFF0066AA : 0xFF444444;
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            if (isHovered && index != selectedIndex)
                bgColor = 0xFF666666;

            drawRect(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight, bgColor);
            this.drawString(fontRenderer, fileName, panelX + 15, itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int panelWidth = 350;
        int panelX = (this.width - panelWidth) / 2;
        int listY = (this.height - 240) / 2 + 40;
        int listHeight = 140;
        int itemHeight = 20;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < scanFiles.size()) {
                selectedIndex = clickedIndex;
                updateButtonStates();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}






