package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.AutoSigninOnlineHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAutoSigninOnlineConfig extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    private ToggleGuiButton totalButton;
    private ToggleGuiButton signinButton;
    private ToggleGuiButton onlineButton;

    public GuiAutoSigninOnlineConfig(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelWidth = 340;
        int panelHeight = 170;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        int y = panelY + 24;

        totalButton = new ToggleGuiButton(1, panelX + 10, y, panelWidth - 20, 20,
                I18n.format("gui.signin_online.master", stateText(AutoSigninOnlineHandler.enabled)),
                AutoSigninOnlineHandler.enabled);
        this.buttonList.add(totalButton);
        y += 28;

        signinButton = new ToggleGuiButton(2, panelX + 10, y, 155, 20,
                I18n.format("gui.signin_online.signin", stateText(AutoSigninOnlineHandler.signinEnabled)),
                AutoSigninOnlineHandler.signinEnabled);
        this.buttonList.add(signinButton);

        onlineButton = new ToggleGuiButton(3, panelX + 175, y, 155, 20,
                I18n.format("gui.signin_online.online", stateText(AutoSigninOnlineHandler.onlineEnabled)),
                AutoSigninOnlineHandler.onlineEnabled);
        this.buttonList.add(onlineButton);
        refreshSubSwitchButtons();

        this.buttonList
                .add(new ThemedButton(100, panelX + 10, panelY + panelHeight - 28, (panelWidth - 30) / 2, 20,
                        "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(101, panelX + 20 + (panelWidth - 30) / 2, panelY + panelHeight - 28,
                (panelWidth - 30) / 2, 20, I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            AutoSigninOnlineHandler.enabled = !AutoSigninOnlineHandler.enabled;
            totalButton.setEnabledState(AutoSigninOnlineHandler.enabled);
            totalButton.displayString = I18n.format("gui.signin_online.master",
                    stateText(AutoSigninOnlineHandler.enabled));
            refreshSubSwitchButtons();
        } else if (button.id == 2) {
            AutoSigninOnlineHandler.signinEnabled = !AutoSigninOnlineHandler.signinEnabled;
            signinButton.setEnabledState(AutoSigninOnlineHandler.signinEnabled);
            signinButton.displayString = I18n.format("gui.signin_online.signin",
                    stateText(AutoSigninOnlineHandler.signinEnabled));
        } else if (button.id == 3) {
            AutoSigninOnlineHandler.onlineEnabled = !AutoSigninOnlineHandler.onlineEnabled;
            onlineButton.setEnabledState(AutoSigninOnlineHandler.onlineEnabled);
            onlineButton.displayString = I18n.format("gui.signin_online.online",
                    stateText(AutoSigninOnlineHandler.onlineEnabled));
        } else if (button.id == 100) {
            AutoSigninOnlineHandler.saveConfig();
            mc.setScreen(parentScreen);
        } else if (button.id == 101) {
            AutoSigninOnlineHandler.loadConfig();
            mc.setScreen(parentScreen);
        }
    }

    private void refreshSubSwitchButtons() {
        boolean totalOn = AutoSigninOnlineHandler.enabled;
        signinButton.enabled = totalOn;
        onlineButton.enabled = totalOn;

        // 总开关关闭时：子开关置灰展示并禁点；开启时恢复各自状态颜色。
        if (totalOn) {
            signinButton.setEnabledState(AutoSigninOnlineHandler.signinEnabled);
            onlineButton.setEnabledState(AutoSigninOnlineHandler.onlineEnabled);
        } else {
            signinButton.setEnabledState(false);
            onlineButton.setEnabledState(false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 340;
        int panelHeight = 170;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.signin_online.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.signin_online.tip1"), panelX + 10, panelY + 86, 0xFFBBBBBB);
        this.drawString(this.fontRenderer, I18n.format("gui.signin_online.tip2"), panelX + 10, panelY + 98, 0xFFBBBBBB);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String stateText(boolean enabled) {
        return I18n.format(enabled ? "gui.common.enabled" : "gui.common.disabled");
    }

}






