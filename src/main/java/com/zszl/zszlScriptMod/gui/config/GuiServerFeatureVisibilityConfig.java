package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager.ServerFeatureRule;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiServerFeatureVisibilityConfig extends ThemedGuiScreen {
    private final GuiScreen parentScreen;

    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private GuiButton toggleRuleBtn;

    public GuiServerFeatureVisibilityConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 420;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 220) / 2;

        toggleRuleBtn = new ThemedButton(1, panelX + 10, panelY + 160, panelWidth - 20, 20,
                I18n.format("gui.server_feature_visibility.toggle"));
        this.buttonList.add(toggleRuleBtn);

        this.buttonList.add(new ThemedButton(2, panelX + 10, panelY + 185, panelWidth - 20, 20,
                I18n.format("gui.common.save_and_close")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean selected = selectedIndex >= 0 && selectedIndex < ServerFeatureVisibilityManager.rules.size();
        toggleRuleBtn.enabled = selected;
        if (selected) {
            ServerFeatureRule rule = ServerFeatureVisibilityManager.rules.get(selectedIndex);
            toggleRuleBtn.displayString = rule.enabled
                    ? I18n.format("gui.server_feature_visibility.disable_selected")
                    : I18n.format("gui.server_feature_visibility.enable_selected");
        } else {
            toggleRuleBtn.displayString = I18n.format("gui.server_feature_visibility.toggle");
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            if (selectedIndex >= 0 && selectedIndex < ServerFeatureVisibilityManager.rules.size()) {
                ServerFeatureRule rule = ServerFeatureVisibilityManager.rules.get(selectedIndex);
                rule.enabled = !rule.enabled;
                updateButtonStates();
            }
        } else if (button.id == 2) {
            ServerFeatureVisibilityManager.saveConfig();
            this.mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 420;
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.server_feature_visibility.title"),
                this.fontRenderer);

        int listY = panelY + 35;
        int listHeight = 120;
        int itemHeight = 20;
        int visible = listHeight / itemHeight;
        maxScroll = Math.max(0, ServerFeatureVisibilityManager.rules.size() - visible);

        if (ServerFeatureVisibilityManager.rules.isEmpty()) {
            GuiTheme.drawEmptyState(this.width / 2, listY + listHeight / 2 - 4,
                    I18n.format("gui.server_feature_visibility.empty"), this.fontRenderer);
        }

        for (int i = 0; i < visible; i++) {
            int index = i + scrollOffset;
            if (index >= ServerFeatureVisibilityManager.rules.size()) {
                break;
            }
            ServerFeatureRule rule = ServerFeatureVisibilityManager.rules.get(index);
            int itemY = listY + i * itemHeight;

            int bgColor = (index == selectedIndex) ? 0xFF0066AA : 0xFF444444;
            boolean hover = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10
                    && mouseY >= itemY && mouseY <= itemY + itemHeight;
            if (hover && index != selectedIndex) {
                bgColor = 0xFF666666;
            }

            drawRect(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight, bgColor);

            String state = rule.enabled ? "§aON" : "§cOFF";
            drawString(fontRenderer, String.format("%s §f%s", state, rule.name), panelX + 15, itemY + 6, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int panelWidth = 420;
        int panelX = (this.width - panelWidth) / 2;
        int listY = (this.height - 220) / 2 + 35;
        int listHeight = 120;
        int itemHeight = 20;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10
                && mouseY >= listY && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < ServerFeatureVisibilityManager.rules.size()) {
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
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }
}






