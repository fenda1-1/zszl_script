package com.zszl.zszlScriptMod.gui.loot;

import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.handlers.LootHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiLootIdViewer extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private List<String> lines = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final int itemHeight = 18;

    public GuiLootIdViewer(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 360;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 240) / 2;
        this.buttonList.add(new GuiButton(0, panelX + panelWidth - 95, panelY + 210, 85, 20, I18n.format("gui.common.done")));
        refreshLines();
    }

    private void refreshLines() {
        lines = LootHelper.INSTANCE.getViewerLines();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 360;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);
        this.drawCenteredString(this.fontRenderer, "战利品ID查看", panelX + panelWidth / 2, panelY + 10, 0xFFFFFF);

        int listX = panelX + 10;
        int listY = panelY + 30;
        int listW = panelWidth - 20;
        int listH = 170;
        drawRect(listX, listY, listX + listW, listY + listH, 0x66000000);

        int visible = Math.max(1, listH / itemHeight);
        maxScroll = Math.max(0, lines.size() - visible);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        for (int i = 0; i < visible; i++) {
            int idx = scrollOffset + i;
            if (idx >= lines.size()) {
                break;
            }
            int y = listY + i * itemHeight + 4;
            this.drawString(this.fontRenderer, lines.get(idx), listX + 6, y, 0xFFE0E0E0);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0 || maxScroll <= 0) {
            return;
        }
        if (dWheel > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
