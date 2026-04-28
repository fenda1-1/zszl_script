package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.template.LegacyActionTemplateManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiLegacyActionTemplateSelector extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final Consumer<String> onSelect;
    private final List<String> templateNames = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int scroll = 0;
    private static final int ROW_H = 22;

    public GuiLegacyActionTemplateSelector(GuiScreen parentScreen, Consumer<String> onSelect) {
        this.parentScreen = parentScreen;
        this.onSelect = onSelect;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        templateNames.clear();
        templateNames.addAll(LegacyActionTemplateManager.getTemplateNames());

        panelW = Math.min(420, this.width - 20);
        panelH = Math.min(300, this.height - 20);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        listX = panelX + 12;
        listY = panelY + 34;
        listW = panelW - 24;
        listH = panelH - 72;

        buttonList.add(new ThemedButton(1, panelX + panelW - 94, panelY + panelH - 28, 80, 20, "返回"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "选择模板", fontRenderer);
        GuiTheme.drawInputFrameSafe(listX, listY, listW, listH, false, true);

        if (templateNames.isEmpty()) {
            GuiTheme.drawEmptyState(listX + listW / 2, listY + 20, "暂无模板", fontRenderer);
        } else {
            int visible = Math.max(1, listH / ROW_H);
            int maxScroll = Math.max(0, templateNames.size() - visible);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            for (int i = 0; i < visible; i++) {
                int actual = i + scroll;
                if (actual >= templateNames.size()) {
                    break;
                }
                int rowY = listY + i * ROW_H;
                boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + ROW_H;
                GuiTheme.drawButtonFrameSafe(listX + 2, rowY + 1, listW - 4, ROW_H - 2,
                        hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawString(fontRenderer, templateNames.get(actual), listX + 8, rowY + 7, 0xFFEAF7FF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int visible = Math.max(1, listH / ROW_H);
        for (int i = 0; i < visible; i++) {
            int actual = i + scroll;
            if (actual >= templateNames.size()) {
                break;
            }
            int rowY = listY + i * ROW_H;
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + ROW_H) {
                if (onSelect != null) {
                    onSelect.accept(templateNames.get(actual));
                }
                mc.setScreen(parentScreen);
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel == 0 || templateNames.isEmpty()) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            return;
        }
        int visible = Math.max(1, listH / ROW_H);
        int maxScroll = Math.max(0, templateNames.size() - visible);
        if (dWheel > 0) {
            scroll = Math.max(0, scroll - 1);
        } else {
            scroll = Math.min(maxScroll, scroll + 1);
        }
    }
}






