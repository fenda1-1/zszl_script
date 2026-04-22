// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/packet/GuiPacketSequenceSelector.java
package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiPacketSequenceSelector extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final Consumer<String> onSelect;
    private List<String> sequenceFiles;

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public GuiPacketSequenceSelector(GuiScreen parent, Consumer<String> onSelectCallback) {
        this.parentScreen = parent;
        this.onSelect = onSelectCallback;
    }

    @Override
    public void initGui() {
        this.sequenceFiles = PacketSequenceManager.getAllSequenceNames();
        this.buttonList.clear();
        this.buttonList.add(
                new GuiButton(0, (this.width - 100) / 2, this.height - 30, 100, 20, I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 250;
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.packet.select_sequence"), this.fontRenderer);

        int listY = panelY + 40;
        int listHeight = 160;
        int itemHeight = 20;
        int visibleItems = listHeight / itemHeight;
        maxScroll = Math.max(0, sequenceFiles.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= sequenceFiles.size())
                break;

            String fileName = sequenceFiles.get(index);
            int itemY = listY + i * itemHeight;
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            drawRect(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight,
                    isHovered ? 0xFF666666 : 0xFF444444);
            this.drawCenteredString(fontRenderer, fileName, this.width / 2, itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawSelectorTooltip(mouseX, mouseY, panelX, panelY, panelWidth, listY, listHeight);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int panelWidth = 250;
        int panelX = (this.width - panelWidth) / 2;
        int listY = (this.height - 220) / 2 + 40;
        int listHeight = 160;
        int itemHeight = 20;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < sequenceFiles.size()) {
                // --- 核心修改：调用回调函数并返回父界面 ---
                onSelect.accept(sequenceFiles.get(clickedIndex));
                mc.setScreen(parentScreen);
                // --- 修改结束 ---
            }
        }
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
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void drawSelectorTooltip(int mouseX, int mouseY, int panelX, int panelY, int panelWidth, int listY,
            int listHeight) {
        String tooltip = null;
        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            tooltip = "可用的数据包序列列表。\n点击一条序列后会回填到上一页对应输入框。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (isMouseOverButton(mouseX, mouseY, button) && button.id == 0) {
                    tooltip = "取消选择并返回上一页。";
                    break;
                }
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }
}







