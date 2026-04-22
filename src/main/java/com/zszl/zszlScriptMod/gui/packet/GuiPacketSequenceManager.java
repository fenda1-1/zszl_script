// File path: src/main/java/com/keycommand2/zszlScriptMod/gui/packet/GuiPacketSequenceManager.java
package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNoCallback;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNo;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;

import java.io.IOException;
import java.util.List;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiPacketSequenceManager extends ThemedGuiScreen implements GuiYesNoCallback {
    private final GuiScreen parentScreen;
    private List<String> sequenceFiles;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private GuiButton btnLoad, btnSend, btnRename, btnDelete;

    public GuiPacketSequenceManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.sequenceFiles = PacketSequenceManager.getAllSequenceNames();
        this.buttonList.clear();
        int panelWidth = 350;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 240) / 2;

        btnLoad = new ThemedButton(0, panelX + 10, panelY + 185, 70, 20, "§a" + I18n.format("gui.packet.seq_mgr.load"));
        btnSend = new ThemedButton(1, panelX + 90, panelY + 185, 70, 20,
                "§b" + I18n.format("gui.packet.seq_mgr.send_now"));
        btnRename = new ThemedButton(2, panelX + 170, panelY + 185, 70, 20,
                "§e" + I18n.format("gui.path.rename"));
        btnDelete = new ThemedButton(3, panelX + 250, panelY + 185, 70, 20, "§c" + I18n.format("gui.common.delete"));
        this.buttonList.add(btnLoad);
        this.buttonList.add(btnSend);
        this.buttonList.add(btnRename);
        this.buttonList.add(btnDelete);

        this.buttonList
                .add(new ThemedButton(4, panelX + 10, panelY + 215, panelWidth - 20, 20,
                        I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean isSelected = selectedIndex != -1;
        btnLoad.enabled = isSelected;
        btnSend.enabled = isSelected;
        btnRename.enabled = isSelected;
        btnDelete.enabled = isSelected;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (selectedIndex == -1 && button.id < 4)
            return;
        String selectedName = (selectedIndex != -1) ? sequenceFiles.get(selectedIndex) : null;

        switch (button.id) {
            case 0: // Load
                PacketSequence seqToLoad = PacketSequenceManager.loadSequence(selectedName);
                if (seqToLoad != null) {
                    mc.setScreen(new GuiPacketSequenceEditor(this, seqToLoad));
                }
                break;
            case 1: // Send
                PacketSequence seqToSend = PacketSequenceManager.loadSequence(selectedName);
                if (seqToSend != null) {
                    // Create a temporary editor instance to reuse sending logic
                    GuiPacketSequenceEditor sender = new GuiPacketSequenceEditor(this, seqToSend);
                    sender.mc = this.mc;
                    sender.sendSequence();
                }
                break;
            case 2: // Rename
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.path.manager.input_new_name"), selectedName,
                        newName -> {
                            if (newName != null && !newName.trim().isEmpty() && !newName.equals(selectedName)) {
                                if (PacketSequenceManager.renameSequence(selectedName, newName.trim())) {
                                    this.initGui();
                                }
                            }
                            mc.setScreen(this);
                        }));
                break;
            case 3: // Delete
                mc.setScreen(new GuiYesNo((GuiYesNoCallback) this, I18n.format("gui.common.confirm_delete"),
                        I18n.format("gui.packet.seq_mgr.delete_confirm", selectedName), 0));
                break;
            case 4: // Done
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && id == 0 && selectedIndex != -1) {
            PacketSequenceManager.deleteSequence(sequenceFiles.get(selectedIndex));
            selectedIndex = -1;
            this.initGui();
        }
        mc.setScreen(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 350;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.packet.seq_mgr.title"), this.fontRenderer);

        int listY = panelY + 40;
        int listHeight = 140;
        int itemHeight = 20;
        int visibleItems = listHeight / itemHeight;
        maxScroll = Math.max(0, sequenceFiles.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= sequenceFiles.size())
                break;

            String fileName = sequenceFiles.get(index);
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
        drawManagerTooltip(mouseX, mouseY, panelX, panelY, panelWidth);
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
            if (clickedIndex >= 0 && clickedIndex < sequenceFiles.size()) {
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

    private void drawManagerTooltip(int mouseX, int mouseY, int panelX, int panelY, int panelWidth) {
        String tooltip = null;
        int listY = panelY + 40;
        int listHeight = 140;
        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            tooltip = "已保存的数据包序列列表。\n左键选中一条序列，再用下方按钮执行加载、发送、重命名或删除。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                switch (button.id) {
                    case 0:
                        tooltip = "加载选中的数据包序列到编辑器中继续修改。";
                        break;
                    case 1:
                        tooltip = "立即发送选中的已保存序列。\n会按序列中保存的方向和延迟逐条发送。";
                        break;
                    case 2:
                        tooltip = "重命名当前选中的数据包序列文件。";
                        break;
                    case 3:
                        tooltip = "删除当前选中的数据包序列。";
                        break;
                    case 4:
                        tooltip = "返回上一页。";
                        break;
                    default:
                        break;
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }
}


