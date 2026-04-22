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
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditBlockReplacementRule extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private final BlockReplacementRule rule;
    private final Consumer<BlockReplacementRule> onSave;

    private GuiTextField nameField;
    private GuiTextField corner1Field;
    private GuiTextField corner2Field;
    private GuiButton btnEnabled;
    private GuiButton btnHighlight;
    private GuiButton btnSolidCollision;

    private int selectedReplacementIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final List<GuiTextField> allFields = new ArrayList<>();

    public GuiEditBlockReplacementRule(GuiScreen parentScreen, BlockReplacementRule rule,
            Consumer<BlockReplacementRule> onSave) {
        this.parentScreen = parentScreen;
        this.rule = rule == null ? new BlockReplacementRule() : rule;
        if (this.rule.replacements == null) {
            this.rule.replacements = new ArrayList<>();
        }
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allFields.clear();

        int panelWidth = 560;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 320) / 2;
        int y = panelY + 26;

        nameField = new GuiTextField(1, fontRenderer, panelX + 10, y + 12, panelWidth - 20, 20);
        nameField.setText(rule.name == null ? "" : rule.name);
        nameField.setMaxStringLength(Integer.MAX_VALUE);
        allFields.add(nameField);
        y += 40;

        btnEnabled = new ThemedButton(2, panelX + 10, y, 120, 20,
                I18n.format("gui.blockreplace.edit.enabled", onOff(rule.enabled)));
        btnHighlight = new ThemedButton(3, panelX + 140, y, 160, 20,
                I18n.format("gui.blockreplace.edit.highlight", onOff(rule.highlightReplacedBlocks)));
        btnSolidCollision = new ThemedButton(13, panelX + 310, y, 240, 20,
                I18n.format("gui.blockreplace.edit.solid_collision", onOff(rule.useSolidCollision)));
        this.buttonList.add(btnEnabled);
        this.buttonList.add(btnHighlight);
        this.buttonList.add(btnSolidCollision);
        y += 28;

        corner1Field = new GuiTextField(4, fontRenderer, panelX + 10, y + 12, 240, 20);
        corner1Field.setText(formatCorner1());
        corner1Field.setMaxStringLength(Integer.MAX_VALUE);
        allFields.add(corner1Field);
        corner2Field = new GuiTextField(5, fontRenderer, panelX + 260, y + 12, 240, 20);
        corner2Field.setText(formatCorner2());
        corner2Field.setMaxStringLength(Integer.MAX_VALUE);
        allFields.add(corner2Field);
        this.buttonList.add(new ThemedButton(6, panelX + 510, y + 12, 40, 20, "选区"));
        y += 46;

        this.buttonList.add(new ThemedButton(7, panelX + 10, y, 150, 20,
                I18n.format("gui.blockreplace.edit.scan_blocks")));
        this.buttonList.add(new ThemedButton(8, panelX + 170, y, 150, 20,
                I18n.format("gui.blockreplace.edit.add_replacement")));
        this.buttonList.add(new ThemedButton(9, panelX + 330, y, 110, 20,
                I18n.format("gui.blockreplace.manager.edit")));
        this.buttonList.add(new ThemedButton(10, panelX + 450, y, 100, 20,
                I18n.format("gui.blockreplace.manager.delete")));
        y += 30;

        this.buttonList.add(new ThemedButton(11, panelX + 10, panelY + 290, (panelWidth - 30) / 2, 20,
                "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(new ThemedButton(12, panelX + 20 + (panelWidth - 30) / 2, panelY + 290,
                (panelWidth - 30) / 2, 20, I18n.format("gui.common.cancel")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedReplacementIndex >= 0 && selectedReplacementIndex < rule.replacements.size();
        for (GuiButton button : this.buttonList) {
            if (button.id == 9 || button.id == 10) {
                button.enabled = hasSelection;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 2:
                rule.enabled = !rule.enabled;
                btnEnabled.displayString = I18n.format("gui.blockreplace.edit.enabled", onOff(rule.enabled));
                break;
            case 3:
                rule.highlightReplacedBlocks = !rule.highlightReplacedBlocks;
                btnHighlight.displayString = I18n.format("gui.blockreplace.edit.highlight",
                        onOff(rule.highlightReplacedBlocks));
                break;
            case 13:
                rule.useSolidCollision = !rule.useSolidCollision;
                btnSolidCollision.displayString = I18n.format("gui.blockreplace.edit.solid_collision",
                        onOff(rule.useSolidCollision));
                break;
            case 6:
                syncFieldsToRule();
                BlockReplacementHandler.startRegionSelection(rule, this);
                break;
            case 7:
                syncFieldsToRule();
                BlockReplacementHandler.markRuleDirty(rule);
                break;
            case 8:
                rule.replacements.add(new BlockReplacementRule.BlockReplacementEntry());
                selectedReplacementIndex = rule.replacements.size() - 1;
                updateButtonStates();
                break;
            case 9:
                if (selectedReplacementIndex >= 0 && selectedReplacementIndex < rule.replacements.size()) {
                    mc.setScreen(
                            new GuiEditBlockReplacementEntry(this, rule.replacements.get(selectedReplacementIndex),
                                    edited -> {
                                        rule.replacements.set(selectedReplacementIndex, edited);
                                        BlockReplacementHandler.markRuleDirty(rule);
                                        mc.setScreen(this);
                                    }));
                }
                break;
            case 10:
                if (selectedReplacementIndex >= 0 && selectedReplacementIndex < rule.replacements.size()) {
                    rule.replacements.remove(selectedReplacementIndex);
                    selectedReplacementIndex = -1;
                    updateButtonStates();
                }
                break;
            case 11:
                syncFieldsToRule();
                onSave.accept(rule);
                BlockReplacementHandler.saveConfig();
                mc.setScreen(parentScreen);
                break;
            case 12:
                mc.setScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    private void syncFieldsToRule() {
        rule.name = nameField.getText() == null ? "" : nameField.getText().trim();
        parseCorner(corner1Field.getText(), true);
        parseCorner(corner2Field.getText(), false);
        BlockReplacementHandler.markRuleDirty(rule);
    }

    private void parseCorner(String text, boolean first) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return;
        }
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            if (first) {
                rule.setCorner1(x, y, z);
            } else {
                rule.setCorner2(x, y, z);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 560;
        int panelHeight = 360;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.blockreplace.edit.title"),
                this.fontRenderer);

        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.name"), nameField.x, nameField.y - 10,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(nameField);
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.corner1"), corner1Field.x, corner1Field.y - 10,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(corner1Field);
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.corner2"), corner2Field.x, corner2Field.y - 10,
                GuiTheme.SUB_TEXT);
        drawThemedTextField(corner2Field);
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.selection_hint"), panelX + 10, panelY + 108,
                GuiTheme.SUB_TEXT);
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.region_info", rule.getRegionBlockCount()),
                panelX + 10, panelY + 124, GuiTheme.SUB_TEXT);

        int listY = panelY + 145;
        int itemHeight = 20;
        int visibleItems = 6;
        maxScroll = Math.max(0, rule.replacements.size() - visibleItems);
        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= rule.replacements.size()) {
                break;
            }
            BlockReplacementRule.BlockReplacementEntry entry = rule.replacements.get(index);
            int itemY = listY + i * itemHeight;
            int bgColor = index == selectedReplacementIndex ? 0xFF0066AA : 0xFF444444;
            boolean hover = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 18 && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            if (hover && index != selectedReplacementIndex) {
                bgColor = 0xFF666666;
            }
            drawRect(panelX + 10, itemY, panelX + panelWidth - 18, itemY + itemHeight, bgColor);
            String line = I18n.format("gui.blockreplace.edit.entry_line", entry.enabled ? "§a✔" : "§c✘",
                    index + 1,
                    entry.sourceBlockId == null || entry.sourceBlockId.isEmpty() ? "?" : entry.sourceBlockId,
                    entry.targetBlockId == null || entry.targetBlockId.isEmpty() ? "?" : entry.targetBlockId);
            drawString(fontRenderer, line, panelX + 15, itemY + 6, 0xFFFFFF);
        }

        List<BlockReplacementHandler.BlockCountEntry> availableBlocks = BlockReplacementHandler
                .getAvailableBlocks(rule);
        int infoY = panelY + 274;
        drawString(fontRenderer, I18n.format("gui.blockreplace.edit.available_blocks"), panelX + 10, infoY,
                GuiTheme.SUB_TEXT);
        if (availableBlocks.isEmpty()) {
            drawString(fontRenderer, I18n.format("gui.blockreplace.edit.available_blocks_empty"), panelX + 10,
                    infoY + 14, GuiTheme.SUB_TEXT);
        } else {
            int count = Math.min(4, availableBlocks.size());
            for (int i = 0; i < count; i++) {
                BlockReplacementHandler.BlockCountEntry entry = availableBlocks.get(i);
                drawString(fontRenderer,
                        I18n.format("gui.blockreplace.edit.available_blocks_line", entry.blockId, entry.count),
                        panelX + 10, infoY + 14 + i * 12, 0xFFFFFF);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : allFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        int panelWidth = 560;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 320) / 2;
        int listY = panelY + 145;
        int itemHeight = 20;
        int visibleHeight = itemHeight * 6;
        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 18 && mouseY >= listY
                && mouseY <= listY + visibleHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < rule.replacements.size()) {
                selectedReplacementIndex = clickedIndex;
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

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        for (GuiTextField field : allFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        corner1Field.setText(formatCorner1());
        corner2Field.setText(formatCorner2());
        for (GuiTextField field : allFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String formatCorner1() {
        return rule.hasCorner1() ? rule.corner1X + ", " + rule.corner1Y + ", " + rule.corner1Z : "";
    }

    private String formatCorner2() {
        return rule.hasCorner2() ? rule.corner2X + ", " + rule.corner2Y + ", " + rule.corner2Z : "";
    }

    private String onOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }
}






