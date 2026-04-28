package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.handlers.BlockReplacementHandler;
import com.zszl.zszlScriptMod.system.BlockReplacementRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditBlockReplacementEntry extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private final BlockReplacementRule.BlockReplacementEntry entry;
    private final Consumer<BlockReplacementRule.BlockReplacementEntry> onSave;

    private GuiTextField sourceField;
    private GuiTextField targetField;
    private final List<GuiTextField> fields = new ArrayList<>();
    private GuiButton btnEnabled;

    public GuiEditBlockReplacementEntry(GuiScreen parentScreen, BlockReplacementRule.BlockReplacementEntry entry,
            Consumer<BlockReplacementRule.BlockReplacementEntry> onSave) {
        this.parentScreen = parentScreen;
        this.entry = entry == null ? new BlockReplacementRule.BlockReplacementEntry() : entry;
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.fields.clear();

        int panelWidth = 360;
        int panelX = (this.width - panelWidth) / 2;
        int startY = this.height / 2 - 80;

        sourceField = new GuiTextField(1, fontRenderer, panelX + 10, startY + 24, panelWidth - 100, 20);
        sourceField.setText(entry.sourceBlockId == null ? "" : entry.sourceBlockId);
        sourceField.setMaxStringLength(Integer.MAX_VALUE);
        fields.add(sourceField);
        this.buttonList.add(new ThemedButton(2, panelX + panelWidth - 80, startY + 24, 70, 20,
                I18n.format("gui.blockreplace.edit.pick_source")));

        targetField = new GuiTextField(3, fontRenderer, panelX + 10, startY + 74, panelWidth - 100, 20);
        targetField.setText(entry.targetBlockId == null ? "" : entry.targetBlockId);
        targetField.setMaxStringLength(Integer.MAX_VALUE);
        fields.add(targetField);
        this.buttonList.add(new ThemedButton(4, panelX + panelWidth - 80, startY + 74, 70, 20,
                I18n.format("gui.blockreplace.edit.pick_target")));

        btnEnabled = new ThemedButton(5, panelX + 10, startY + 110, panelWidth - 20, 20,
                I18n.format("gui.blockreplace.edit.entry_enabled", onOff(entry.enabled)));
        this.buttonList.add(btnEnabled);

        int half = (panelWidth - 30) / 2;
        this.buttonList.add(new ThemedButton(10, panelX + 10, startY + 145, half, 20,
                "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(11, panelX + 20 + half, startY + 145, half, 20,
                I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 2:
                syncFields();
                BlockReplacementHandler.startSourceBlockSelection(entry, this);
                break;
            case 4:
                syncFields();
                BlockReplacementHandler.startTargetBlockSelection(entry, this);
                break;
            case 5:
                entry.enabled = !entry.enabled;
                btnEnabled.displayString = I18n.format("gui.blockreplace.edit.entry_enabled", onOff(entry.enabled));
                break;
            case 10:
                syncFields();
                onSave.accept(entry);
                mc.setScreen(parentScreen);
                break;
            case 11:
                mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    private void syncFields() {
        entry.sourceBlockId = sourceField.getText() == null ? "" : sourceField.getText().trim();
        entry.targetBlockId = targetField.getText() == null ? "" : targetField.getText().trim();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 380;
        int panelHeight = 210;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.blockreplace.edit.entry_title"),
                this.fontRenderer);

        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.source_block"), sourceField.x, sourceField.y - 10,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(sourceField);
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.target_block"), targetField.x, targetField.y - 10,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(targetField);
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.pick_hint"), panelX + 10, panelY + 110,
                GuiTheme.SUB_TEXT);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : fields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        for (GuiTextField field : fields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : fields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String onOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }
}






