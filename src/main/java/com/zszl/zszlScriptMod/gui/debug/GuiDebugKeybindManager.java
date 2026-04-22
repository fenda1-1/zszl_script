// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/debug/GuiDebugKeybindManager.java
// (这是一个新文件)
package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.gui.path.GuiKeybindRecorder;
import com.zszl.zszlScriptMod.system.BindableDebugAction;
import com.zszl.zszlScriptMod.system.DebugKeybindManager;
import com.zszl.zszlScriptMod.system.KeybindManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * 调试功能快捷键的管理界面。
 */
public class GuiDebugKeybindManager extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    public GuiDebugKeybindManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 120;
        int panelHeight = 240;
        int titleHeight = 24;
        int footerHeight = 30;
        int listPadding = 10;
        int itemHeight = 25;
        int listTop = panelY + titleHeight + listPadding;
        int listBottom = panelY + panelHeight - footerHeight - listPadding;

        // 遍历所有可绑定的调试动作
        BindableDebugAction[] actions = BindableDebugAction.values();
        for (int i = 0; i < actions.length; i++) {
            int buttonY = listTop + i * itemHeight;
            if (buttonY + 20 > listBottom) {
                break;
            }
            // 添加“更改”按钮
            this.buttonList.add(new GuiButton(i, panelX + panelWidth - 90, buttonY, 80, 20,
                    I18n.format("gui.debug_keybind.change")));
        }

        this.buttonList.add(new GuiButton(100, panelX, panelY + panelHeight - 30, (panelWidth - 10) / 2, 20,
                I18n.format("gui.debug_keybind.save_close")));
        this.buttonList.add(new GuiButton(101, panelX + (panelWidth + 10) / 2, panelY + panelHeight - 30,
                (panelWidth - 10) / 2, 20, I18n.format("gui.debug_keybind.back_no_save")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 0 && button.id < BindableDebugAction.values().length) {
            final BindableDebugAction action = BindableDebugAction.values()[button.id];
            mc.setScreen(new GuiKeybindRecorder(this, DebugKeybindManager.keybinds.get(action), newKeybind -> {
                if (newKeybind != null && newKeybind.getKeyCode() != Keyboard.KEY_NONE) {
                    DebugKeybindManager.keybinds.put(action, newKeybind);
                } else {
                    DebugKeybindManager.keybinds.remove(action);
                }
                mc.setScreen(this);
            }));
        } else if (button.id == 100) { // 保存
            DebugKeybindManager.saveConfig();
            this.mc.setScreen(this.parentScreen);
        } else if (button.id == 101) { // 返回
            DebugKeybindManager.loadConfig(); // 重新加载以放弃更改
            this.mc.setScreen(this.parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 120;
        int panelHeight = 240;
        int titleHeight = 24;
        int footerHeight = 30;
        int listPadding = 10;
        int itemHeight = 25;
        int listTop = panelY + titleHeight + listPadding;
        int listBottom = panelY + panelHeight - footerHeight - listPadding;

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + titleHeight, 0xA0000000);
        drawRect(panelX + 5, listTop - 5, panelX + panelWidth - 5, listBottom, 0x50000000);

        drawCenteredString(fontRenderer, I18n.format("gui.debug_keybind.title"), this.width / 2, panelY + 8,
                0xFFFFFF);

        BindableDebugAction[] actions = BindableDebugAction.values();

        for (int i = 0; i < actions.length; i++) {
            int rowTop = listTop + i * itemHeight;
            if (rowTop + itemHeight > listBottom) {
                break;
            }

            BindableDebugAction action = actions[i];
            int currentY = rowTop + (itemHeight - 8) / 2;

            drawString(fontRenderer, action.getDisplayName(), panelX + 10, currentY, 0xFFFFFF);

            KeybindManager.Keybind keybind = DebugKeybindManager.keybinds.get(action);
            String keyName = (keybind == null || keybind.getKeyCode() == Keyboard.KEY_NONE)
                    ? I18n.format("gui.debug_keybind.unbound")
                    : "§e" + keybind.toString();
            drawString(fontRenderer, keyName, panelX + 150, currentY, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}





