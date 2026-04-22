// [V4.7] 新增超时重载UI
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;

public class GuiAntiStuckConfig extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private ToggleGuiButton enableButton;
    private ToggleGuiButton avoidVinesButton;
    private GuiTextField vineDistanceField;

    // [V4.7 新增] UI控件
    private ToggleGuiButton timeoutReloadButton;
    private GuiTextField timeoutSecondsField;

    private final Map<Integer, String> tooltips = new HashMap<>();
    private static final DecimalFormat FORMAT = new DecimalFormat("#.#");

    public GuiAntiStuckConfig(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.tooltips.clear();
        int panelWidth = 320;
        // [V4.7 修改] 增加面板高度以容纳新选项
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        int fieldX = panelX + 180;
        int fieldWidth = 110;

        int currentY = panelY + 30;

        enableButton = new ToggleGuiButton(0, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.antistuck.master"), AutoFollowHandler.antiStuckEnabled);
        this.buttonList.add(enableButton);
        tooltips.put(0, I18n.format("gui.antistuck.tip.master"));
        currentY += 30;

        avoidVinesButton = new ToggleGuiButton(3, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.antistuck.avoid"), AutoFollowHandler.avoidVinesProactively);
        this.buttonList.add(avoidVinesButton);
        tooltips.put(3, I18n.format("gui.antistuck.tip.avoid"));
        currentY += 30;

        vineDistanceField = new GuiTextField(4, this.fontRenderer, fieldX, currentY, fieldWidth, 20);
        vineDistanceField.setText(FORMAT.format(AutoFollowHandler.vineAvoidanceDistance));
        vineDistanceField.setEnabled(AutoFollowHandler.avoidVinesProactively && AutoFollowHandler.antiStuckEnabled);
        currentY += 30;

        // [V4.7 新增] 超时重载UI
        timeoutReloadButton = new ToggleGuiButton(5, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.antistuck.timeout"), AutoFollowHandler.timeoutReloadEnabled);
        this.buttonList.add(timeoutReloadButton);
        tooltips.put(5, I18n.format("gui.antistuck.tip.timeout"));
        currentY += 30;

        timeoutSecondsField = new GuiTextField(6, this.fontRenderer, fieldX, currentY, fieldWidth, 20);
        timeoutSecondsField.setText(String.valueOf(AutoFollowHandler.timeoutReloadSeconds));
        timeoutSecondsField.setEnabled(AutoFollowHandler.timeoutReloadEnabled);
        currentY += 35;

        int buttonWidth = (panelWidth - 40) / 3;
        this.buttonList
                .add(new GuiButton(100, panelX + 10, currentY, buttonWidth, 20, "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(new GuiButton(101, panelX + 20 + buttonWidth, currentY, buttonWidth, 20,
                "§e" + I18n.format("gui.evac_adv.reset")));
        this.buttonList.add(new GuiButton(102, panelX + 30 + 2 * buttonWidth, currentY, buttonWidth, 20,
                I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // 总开关
            AutoFollowHandler.antiStuckEnabled = !AutoFollowHandler.antiStuckEnabled;
            ((ToggleGuiButton) button).setEnabledState(AutoFollowHandler.antiStuckEnabled);
            avoidVinesButton.enabled = AutoFollowHandler.antiStuckEnabled;
            vineDistanceField.setEnabled(AutoFollowHandler.antiStuckEnabled && AutoFollowHandler.avoidVinesProactively);
        } else if (button.id == 3) { // 主动规避开关
            AutoFollowHandler.avoidVinesProactively = !AutoFollowHandler.avoidVinesProactively;
            ((ToggleGuiButton) button).setEnabledState(AutoFollowHandler.avoidVinesProactively);
            vineDistanceField.setEnabled(AutoFollowHandler.avoidVinesProactively);
        } else if (button.id == 5) { // [V4.7 新增] 超时重载开关
            AutoFollowHandler.timeoutReloadEnabled = !AutoFollowHandler.timeoutReloadEnabled;
            ((ToggleGuiButton) button).setEnabledState(AutoFollowHandler.timeoutReloadEnabled);
            timeoutSecondsField.setEnabled(AutoFollowHandler.timeoutReloadEnabled);
        } else if (button.id == 100) { // 保存
            try {
                AutoFollowHandler.vineAvoidanceDistance = Double
                        .parseDouble(vineDistanceField.getText().replace(',', '.'));
                // [V4.7 新增] 保存新字段
                AutoFollowHandler.timeoutReloadSeconds = Integer.parseInt(timeoutSecondsField.getText());

                AutoFollowHandler.saveFollowConfig();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.antistuck.saved")));
                }
                mc.setScreen(parentScreen);
            } catch (NumberFormatException e) {
                if (mc.player != null) {
                    mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.common.invalid_number")));
                }
            }
        } else if (button.id == 101) { // 重置
            // [V4.7 修改] 重置为新的默认值
            AutoFollowHandler.antiStuckEnabled = false;
            AutoFollowHandler.avoidVinesProactively = false;
            AutoFollowHandler.vineAvoidanceDistance = 2.0;
            AutoFollowHandler.timeoutReloadEnabled = false;
            AutoFollowHandler.timeoutReloadSeconds = 60;
            initGui();
        } else if (button.id == 102) { // 取消
            AutoFollowHandler.loadFollowConfig();
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 320;
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.antistuck.title"), this.fontRenderer);

        int labelX = panelX + 20;
        this.drawString(this.fontRenderer, I18n.format("gui.antistuck.vine_distance"), labelX, vineDistanceField.y + 6,
                0xFFFFFF);
        // [V4.7 新增] 绘制新标签
        this.drawString(this.fontRenderer, I18n.format("gui.antistuck.timeout_seconds"), labelX,
                timeoutSecondsField.y + 6, 0xFFFFFF);

        drawThemedTextField(vineDistanceField);
        drawThemedTextField(timeoutSecondsField);

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (GuiButton button : this.buttonList) {
            if (button.isMouseOver()) {
                String tooltip = tooltips.get(button.id);
                if (tooltip != null) {
                    drawHoveringText(Arrays.asList(tooltip.split("\n")), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        vineDistanceField.textboxKeyTyped(typedChar, keyCode);
        // [V4.7 新增] 处理新输入框
        timeoutSecondsField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        vineDistanceField.mouseClicked(mouseX, mouseY, mouseButton);
        // [V4.7 新增] 处理新输入框
        timeoutSecondsField.mouseClicked(mouseX, mouseY, mouseButton);
    }
}





