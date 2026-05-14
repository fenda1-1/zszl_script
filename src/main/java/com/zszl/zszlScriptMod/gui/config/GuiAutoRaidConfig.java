package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.loot.GuiLootIdViewer;
import com.zszl.zszlScriptMod.handlers.LootHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiAutoRaidConfig extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private GuiTextField delayField;
    private GuiTextField retryField;
    private GuiTextField playerPositionField;

    public GuiAutoRaidConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        int panelWidth = 360;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 110;
        int labelX = panelX + 14;
        int fieldX = panelX + 170;
        int fieldWidth = 170;
        int rowY = panelY + 38;
        int rowGap = 26;

        delayField = new GuiTextField(0, this.fontRenderer, fieldX, rowY, fieldWidth, 18);
        delayField.setMaxStringLength(3);
        delayField.setText(String.valueOf(LootHelper.playerClickDelayTicks));
        rowY += rowGap;

        retryField = new GuiTextField(1, this.fontRenderer, fieldX, rowY, fieldWidth, 18);
        retryField.setMaxStringLength(3);
        retryField.setText(String.valueOf(LootHelper.maxAssignClickAttempts));
        rowY += rowGap;

        playerPositionField = new GuiTextField(2, this.fontRenderer, fieldX, rowY, fieldWidth, 18);
        playerPositionField.setMaxStringLength(3);
        playerPositionField.setText(String.valueOf(LootHelper.assignPlayerPosition));
        rowY += rowGap + 2;

        this.buttonList.add(new ThemedButton(10, labelX, rowY, panelWidth - 28, 20, autoAssignLabel()));
        rowY += rowGap;
        this.buttonList.add(new ThemedButton(11, labelX, rowY, panelWidth - 28, 20, completionActionLabel()));
        rowY += rowGap;
        this.buttonList.add(new ThemedButton(12, labelX, rowY, panelWidth - 28, 20, "战利品ID查看"));
        rowY += 34;

        this.buttonList.add(new ThemedButton(100, labelX, rowY, (panelWidth - 38) / 2, 20, "§a保存并关闭"));
        this.buttonList.add(new ThemedButton(101, labelX + (panelWidth - 38) / 2 + 10, rowY, (panelWidth - 38) / 2,
                20, "取消"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 10) {
            LootHelper.autoOneKeyAssignEnabled = !LootHelper.autoOneKeyAssignEnabled;
            button.displayString = autoAssignLabel();
            return;
        }
        if (button.id == 11) {
            LootHelper.completionAction = nextCompletionAction(LootHelper.completionAction);
            button.displayString = completionActionLabel();
            return;
        }
        if (button.id == 12) {
            this.mc.displayGuiScreen(new GuiLootIdViewer(this));
            return;
        }
        if (button.id == 100) {
            LootHelper.playerClickDelayTicks = LootHelper.clampPlayerClickDelay(parseInt(delayField, 2));
            LootHelper.maxAssignClickAttempts = LootHelper.clampMaxAssignClickAttempts(parseInt(retryField, 10));
            LootHelper.assignPlayerPosition = LootHelper.clampAssignPlayerPosition(parseInt(playerPositionField, 1));
            LootHelper.saveConfig();
            this.mc.displayGuiScreen(parentScreen);
            return;
        }
        if (button.id == 101) {
            LootHelper.loadConfig();
            this.mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelWidth = 360;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 110;
        int labelX = panelX + 14;
        int rowY = panelY + 43;
        int rowGap = 26;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "全自动团本设置", this.fontRenderer);

        this.drawString(this.fontRenderer, "一键分配延迟(Ticks)", labelX, rowY + 4, GuiTheme.LABEL_TEXT);
        drawThemedTextField(delayField);
        rowY += rowGap;

        this.drawString(this.fontRenderer, "最多重试次数", labelX, rowY + 4, GuiTheme.LABEL_TEXT);
        drawThemedTextField(retryField);
        rowY += rowGap;

        this.drawString(this.fontRenderer, "分配玩家位置", labelX, rowY + 4, GuiTheme.LABEL_TEXT);
        drawThemedTextField(playerPositionField);
        rowY += rowGap;

        this.drawString(this.fontRenderer, "超出已识别玩家数时自动回退到玩家1", labelX, rowY + 8, 0xFFB8B8B8);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (delayField.textboxKeyTyped(typedChar, keyCode)
                || retryField.textboxKeyTyped(typedChar, keyCode)
                || playerPositionField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        delayField.mouseClicked(mouseX, mouseY, mouseButton);
        retryField.mouseClicked(mouseX, mouseY, mouseButton);
        playerPositionField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private int parseInt(GuiTextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String autoAssignLabel() {
        return "一键自动分配: " + (LootHelper.autoOneKeyAssignEnabled ? "§a开启" : "§c关闭");
    }

    private String completionActionLabel() {
        if (LootHelper.completionAction == LootHelper.CompletionAction.SUICIDE) {
            return "分配完成后退出: §e/suicide";
        }
        if (LootHelper.completionAction == LootHelper.CompletionAction.DISCONNECT) {
            return "分配完成后退出: §b退出";
        }
        return "分配完成后退出: §7不处理";
    }

    private LootHelper.CompletionAction nextCompletionAction(LootHelper.CompletionAction current) {
        if (current == LootHelper.CompletionAction.NONE) {
            return LootHelper.CompletionAction.SUICIDE;
        }
        if (current == LootHelper.CompletionAction.SUICIDE) {
            return LootHelper.CompletionAction.DISCONNECT;
        }
        return LootHelper.CompletionAction.NONE;
    }
}
