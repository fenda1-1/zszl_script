package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiAutoEquipManager extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private List<String> setNames;
    private int selectedIndex = -1;
    private GuiButton editButton, deleteButton, setActiveButton, smartActivateButton;
    private GuiTextField rangeField;
    private GuiTextField equipIntervalField;

    private int listTop, listBottom, listLeft, listRight, slotHeight;
    private float scrollAmount;
    private boolean isScrolling;

    public GuiAutoEquipManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.setNames = new ArrayList<>(AutoEquipHandler.getAllSetNames());
        this.selectedIndex = -1;

        int centerX = this.width / 2;
        int startY = this.height / 2 - 80;

        this.buttonList
                .add(new ThemedButton(1, centerX - 155, startY + 165, 100, 20, I18n.format("gui.auto_equip.add")));
        this.editButton = new ThemedButton(2, centerX - 50, startY + 165, 100, 20, I18n.format("gui.auto_equip.edit"));
        this.deleteButton = new ThemedButton(3, centerX + 55, startY + 165, 100, 20,
                I18n.format("gui.auto_equip.delete"));

        // --- 核心修改：按钮文本固定 ---
        this.setActiveButton = new ThemedButton(4, centerX - 155, startY + 140, 100, 20,
                I18n.format("gui.auto_equip.set_active"));
        this.smartActivateButton = new ThemedButton(5, centerX - 50, startY + 140, 100, 20,
                I18n.format("gui.auto_equip.set_smart_active"));

        this.buttonList.add(this.editButton);
        this.buttonList.add(this.deleteButton);
        this.buttonList.add(this.setActiveButton);
        this.buttonList.add(this.smartActivateButton);

        this.rangeField = new GuiTextField(6, fontRenderer, centerX + 55, startY + 140, 40, 20);
        this.rangeField.setText(String.valueOf(AutoEquipHandler.smartActivationRange));
        this.rangeField.setMaxStringLength(Integer.MAX_VALUE);

        this.equipIntervalField = new GuiTextField(7, fontRenderer, centerX + 55, startY + 115, 40, 20);
        this.equipIntervalField.setText(String.valueOf(AutoEquipHandler.equipIntervalTicks));
        this.equipIntervalField.setMaxStringLength(Integer.MAX_VALUE);

        this.buttonList
                .add(new ThemedButton(100, centerX - 100, startY + 190, 200, 20, I18n.format("gui.common.done")));

        this.listLeft = centerX - 155;
        this.listRight = centerX + 155;
        this.listTop = startY;
        this.listBottom = startY + 135;
        this.slotHeight = 20;

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean isSelected = selectedIndex >= 0 && selectedIndex < setNames.size();
        this.editButton.enabled = isSelected;
        this.deleteButton.enabled = isSelected;
        this.setActiveButton.enabled = isSelected;
        this.smartActivateButton.enabled = isSelected;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled)
            return;

        String selectedName = (selectedIndex != -1) ? setNames.get(selectedIndex) : null;

        switch (button.id) {
            case 1: // Add New
                mc.setScreen(new GuiTextInput(this, I18n.format("gui.auto_equip.input_new_set"), "", name -> {
                    if (name != null && !name.trim().isEmpty() && !setNames.contains(name)) {
                        AutoEquipHandler.addSet(name);
                        this.setNames = new ArrayList<>(AutoEquipHandler.getAllSetNames());
                        this.selectedIndex = this.setNames.indexOf(name);
                        updateButtonStates();
                    }
                }));
                break;
            case 2: // Edit
                if (selectedName != null)
                    mc.setScreen(new GuiAutoEquipConfig(this, selectedName));
                break;
            case 3: // Delete
                if (selectedName != null) {
                    AutoEquipHandler.deleteSet(selectedName);
                    this.setNames.remove(selectedIndex);
                    this.selectedIndex = -1;
                    updateButtonStates();
                }
                break;
            case 4: // Set Active (Manual)
                if (selectedName != null)
                    AutoEquipHandler.setActiveSet(selectedName, false);
                break;
            case 5: // Smart Activate
                if (selectedName != null)
                    AutoEquipHandler.setActiveSet(selectedName, true);
                break;
            case 100: // Done
                mc.setScreen(parentScreen);
                break;
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        try {
            int range = Integer.parseInt(rangeField.getText());
            AutoEquipHandler.smartActivationRange = Math.max(1, range);
        } catch (NumberFormatException e) {
            AutoEquipHandler.smartActivationRange = 5; // Reset to default on invalid input
        }
        try {
            int interval = Integer.parseInt(equipIntervalField.getText());
            AutoEquipHandler.equipIntervalTicks = Math.max(3, interval);
        } catch (NumberFormatException e) {
            AutoEquipHandler.equipIntervalTicks = 15;
        }
        AutoEquipHandler.saveConfig();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.rangeField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                int range = Integer.parseInt(rangeField.getText());
                AutoEquipHandler.smartActivationRange = Math.max(1, range);
            } catch (NumberFormatException e) {
                // Do nothing, wait for valid input
            }
        } else if (this.equipIntervalField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                int interval = Integer.parseInt(equipIntervalField.getText());
                AutoEquipHandler.equipIntervalTicks = Math.max(3, interval);
            } catch (NumberFormatException e) {
                // Do nothing, wait for valid input
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int panelX = this.width / 2 - 165;
        int panelY = this.height / 2 - 90;
        int panelW = 330;
        int panelH = 230;
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.auto_equip.title"), this.fontRenderer);

        drawRect(listLeft, listTop, listRight, listBottom, 0x66324458);

        int contentHeight = setNames.size() * slotHeight;
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        if (maxScroll > 0) {
            int scrollBarHeight = getScrollBarHeight();
            int scrollBarTop = listTop + (int) (scrollAmount * (listBottom - listTop - scrollBarHeight));
            int scrollBarBottom = scrollBarTop + scrollBarHeight;
            GuiTheme.drawScrollbar(listRight - 5, listTop, 5, listBottom - listTop, scrollBarTop,
                    scrollBarBottom - scrollBarTop);
        }

        int startY = listTop - (int) (scrollAmount * maxScroll);
        for (int i = 0; i < setNames.size(); i++) {
            int entryTop = startY + i * slotHeight;
            int entryBottom = entryTop + slotHeight;
            if (entryBottom < listTop || entryTop > listBottom)
                continue;

            // --- 核心修改：动态生成显示名称 ---
            String rawName = setNames.get(i);
            String displayName = rawName;
            boolean isTheActiveOne = rawName.equals(AutoEquipHandler.activeSetName);

            if (isTheActiveOne) {
                if (AutoEquipHandler.smartActivationEnabled) {
                    displayName += " " + I18n.format("gui.auto_equip.state_smart_active");
                } else if (AutoEquipHandler.enabled) {
                    displayName += " " + I18n.format("gui.auto_equip.state_active");
                }
            }

            if (i == selectedIndex) {
                GuiTheme.drawButtonFrame(listLeft, entryTop, (listRight - (maxScroll > 0 ? 5 : 0)) - listLeft,
                        entryBottom - entryTop, GuiTheme.UiState.SELECTED);
            }
            drawString(fontRenderer, displayName, listLeft + 5, entryTop + 6, 0xFFFFFF);
        }

        drawThemedTextField(this.rangeField);
        drawString(fontRenderer, I18n.format("gui.auto_equip.range"), rangeField.x + 45, rangeField.y + 6,
                GuiTheme.SUB_TEXT);

        drawThemedTextField(this.equipIntervalField);
        drawString(fontRenderer, I18n.format("gui.auto_equip.interval_ticks"), equipIntervalField.x + 45,
                equipIntervalField.y + 6, GuiTheme.SUB_TEXT);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (smartActivateButton.isMouseOver()) {
            drawHoveringText(Arrays.asList(I18n.format("gui.auto_equip.tip.smart_active.title"),
                    I18n.format("gui.auto_equip.tip.smart_active.line1"),
                    I18n.format("gui.auto_equip.tip.smart_active.line2"),
                    I18n.format("gui.auto_equip.tip.smart_active.line3")), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.rangeField.mouseClicked(mouseX, mouseY, mouseButton);
        this.equipIntervalField.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (mouseX >= listLeft && mouseX <= listRight && mouseY >= listTop && mouseY <= listBottom) {
                int contentHeight = setNames.size() * slotHeight;
                int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
                int relativeY = mouseY - listTop + (int) (scrollAmount * maxScroll);
                int clickedIndex = relativeY / slotHeight;
                if (clickedIndex >= 0 && clickedIndex < setNames.size()) {
                    this.selectedIndex = clickedIndex;
                    updateButtonStates();
                }
            }
            int maxScroll = Math.max(0, setNames.size() * slotHeight - (listBottom - listTop));
            if (maxScroll > 0 && mouseX >= listRight - 5 && mouseX <= listRight && mouseY >= listTop
                    && mouseY <= listBottom) {
                isScrolling = true;
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0) {
            isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int contentHeight = setNames.size() * slotHeight;
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        if (delta != 0.0D && maxScroll > 0) {
            scrollAmount -= (float) delta * slotHeight / (float) maxScroll;
            scrollAmount = Math.max(0.0F, Math.min(1.0F, scrollAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int getScrollBarHeight() {
        int listHeight = listBottom - listTop;
        int contentHeight = setNames.size() * slotHeight;
        return contentHeight > listHeight ? Math.max(20, listHeight * listHeight / contentHeight) : 0;
    }
}






