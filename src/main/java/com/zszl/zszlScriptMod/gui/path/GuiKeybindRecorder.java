// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/path/GuiKeybindRecorder.java
// (这是一个全新的文件)
package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import com.zszl.zszlScriptMod.system.KeybindManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

/**
 * 一个用于录制单个或组合按键的GUI界面。
 */
public class GuiKeybindRecorder extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final Consumer<KeybindManager.Keybind> onRecordComplete;
    private KeybindManager.Keybind currentKeybind;
    private boolean isRecording = true; // 默认进入时就是录制状态

    public GuiKeybindRecorder(GuiScreen parent, KeybindManager.Keybind existingKeybind,
            Consumer<KeybindManager.Keybind> callback) {
        this.parentScreen = parent;
        this.currentKeybind = existingKeybind != null ? existingKeybind : new KeybindManager.Keybind();
        this.onRecordComplete = callback;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int centerX = this.width / 2;
        int panelWidth = 260;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 60;

        this.buttonList.add(new GuiButton(0, panelX + 10, panelY + 80, (panelWidth - 40) / 3, 20,
                "§a" + I18n.format("gui.loop.confirm")));
        this.buttonList.add(new GuiButton(1, panelX + 20 + (panelWidth - 40) / 3, panelY + 80, (panelWidth - 40) / 3,
                20, "§e" + I18n.format("gui.evac_adv.reset")));
        this.buttonList.add(new GuiButton(2, panelX + 30 + 2 * ((panelWidth - 40) / 3), panelY + 80,
                (panelWidth - 40) / 3, 20, "§c" + I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // 确认
                onRecordComplete.accept(this.currentKeybind);
                mc.setScreen(parentScreen);
                break;
            case 1: // 重置
                this.currentKeybind = new KeybindManager.Keybind();
                this.isRecording = true; // 重置后回到录制状态
                break;
            case 2: // 取消
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // 按下 ESC 总是可以取消
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }

        if (isRecording) {
            if (keyCode == Keyboard.KEY_NONE) {
                return;
            }

            // 单独按下修饰键时，不立即结束录制，继续等待主键
            if (isModifierKey(keyCode)) {
                return;
            }

            this.currentKeybind = new KeybindManager.Keybind(keyCode, getActiveModifiers());
            isRecording = false; // 录制完成
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int centerX = this.width / 2;
        int panelWidth = 260;
        int panelHeight = 120;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 60;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.path.record_keybind"), centerX, panelY + 15,
                0xFFFFFFFF);

        String displayText = isRecording ? getRecordingDisplayText() : "§a" + this.currentKeybind.toString();
        this.drawCenteredString(this.fontRenderer, displayText, centerX, panelY + 45, 0xFFFFFFFF);
        this.drawCenteredString(this.fontRenderer, "§7支持 Ctrl / Shift / Alt 组合键", centerX, panelY + 60,
                0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private boolean isModifierKey(int keyCode) {
        return keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LMENU || keyCode == Keyboard.KEY_RMENU;
    }

    private Set<Integer> getActiveModifiers() {
        Set<Integer> modifiers = new HashSet<>();
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers.add(Keyboard.KEY_LCONTROL);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers.add(Keyboard.KEY_LSHIFT);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers.add(Keyboard.KEY_LMENU);
        }
        return modifiers;
    }

    private String getRecordingDisplayText() {
        Set<Integer> modifiers = getActiveModifiers();
        if (modifiers.isEmpty()) {
            return I18n.format("gui.path.press_key");
        }

        List<String> modifierNames = new java.util.ArrayList<>();
        if (modifiers.contains(Keyboard.KEY_LCONTROL)) {
            modifierNames.add("Ctrl");
        }
        if (modifiers.contains(Keyboard.KEY_LSHIFT)) {
            modifierNames.add("Shift");
        }
        if (modifiers.contains(Keyboard.KEY_LMENU)) {
            modifierNames.add("Alt");
        }
        return "§e[" + String.join(" + ", modifierNames) + " + ...]";
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}





