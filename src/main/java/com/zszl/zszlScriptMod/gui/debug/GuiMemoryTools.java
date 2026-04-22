// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/debug/GuiMemoryTools.java
package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiMemoryTools extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    public GuiMemoryTools(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int centerX = this.width / 2;
        int panelWidth = 280;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 80;

        this.buttonList
                .add(new GuiButton(0, panelX, panelY + 20, panelWidth, 20, I18n.format("gui.memory.tools.force_gc")));
        this.buttonList.add(
                new GuiButton(1, panelX, panelY + 50, panelWidth, 20, I18n.format("gui.memory.tools.reload_chunks")));
        this.buttonList.add(new GuiButton(2, panelX, panelY + 100, panelWidth, 20, I18n.format("gui.common.back")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (mc.player == null) {
            if (button.id == 2) {
                mc.setScreen(parentScreen);
            }
            return;
        }

        switch (button.id) {
            case 0: // Force GC
                long memBefore = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                System.gc();
                long memAfter = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                mc.player.sendSystemMessage(new TextComponentString(ChatFormatting.GREEN
                        + I18n.format("msg.memory.tools.gc_done", memBefore, memAfter)));
                break;
            case 1: // Reload Chunks
                mc.player.sendSystemMessage(new TextComponentString(
                        ChatFormatting.YELLOW + I18n.format("msg.memory.tools.reloading_chunks")));
                if (mc.levelRenderer != null) {
                    mc.levelRenderer.allChanged();
                }
                break;
            case 2: // 返回
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int panelWidth = 280;
        int panelHeight = 140;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 80;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawCenteredString(fontRenderer, I18n.format("gui.memory.tools.title"), centerX, panelY + 5, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}







