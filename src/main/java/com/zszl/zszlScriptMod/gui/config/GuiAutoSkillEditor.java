// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/config/GuiAutoSkillEditor.java
package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.AutoSkillHandler;
import com.zszl.zszlScriptMod.handlers.AutoSkillHandler.Skill;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.utils.PacketCaptureHandler; // 导入
import net.minecraft.ChatFormatting; // 导入

import java.io.IOException;
import java.util.Collections;

public class GuiAutoSkillEditor extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private int selectedSkillIndex = 0;
    private int selectedHexIndex = -1;

    // UI Controls
    private GuiTextField cooldownField;
    private ToggleGuiButton skillEnabledButton;
    private GuiButton btnAddHex, btnEditHex, btnRemoveHex, btnMoveUp, btnMoveDown;

    // Scrolling
    private int hexScrollOffset = 0;
    private int maxHexScroll = 0;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listX, listY, listWidth, listHeight;

    public GuiAutoSkillEditor(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        panelWidth = 450;
        panelHeight = Math.min(this.height - 40, 280);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int bottomButtonY = panelY + panelHeight - 30;
        this.buttonList.add(
            new ThemedButton(100, panelX + 10, bottomButtonY, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new ThemedButton(101, panelX + 20 + (panelWidth - 30) / 2, bottomButtonY,
                (panelWidth - 30) / 2, 20, I18n.format("gui.common.cancel")));

        int hexActionY = bottomButtonY - 25;
        int editorX = panelX + 100;
        int editorWidth = panelWidth - 110;
        int hexBtnWidth = (editorWidth - 20) / 5;

        btnAddHex = new ThemedButton(20, editorX, hexActionY, hexBtnWidth, 20, I18n.format("gui.auto_skill.add"));
        btnEditHex = new ThemedButton(21, editorX + hexBtnWidth + 5, hexActionY, hexBtnWidth, 20,
                I18n.format("gui.auto_skill.edit"));
        btnRemoveHex = new ThemedButton(22, editorX + 2 * (hexBtnWidth + 5), hexActionY, hexBtnWidth, 20,
                I18n.format("gui.auto_skill.delete"));
        btnMoveUp = new ThemedButton(23, editorX + 3 * (hexBtnWidth + 5), hexActionY, hexBtnWidth, 20,
                I18n.format("gui.auto_skill.move_up"));
        btnMoveDown = new ThemedButton(24, editorX + 4 * (hexBtnWidth + 5), hexActionY, hexBtnWidth, 20,
                I18n.format("gui.auto_skill.move_down"));
        this.buttonList.add(btnAddHex);
        this.buttonList.add(btnEditHex);
        this.buttonList.add(btnRemoveHex);
        this.buttonList.add(btnMoveUp);
        this.buttonList.add(btnMoveDown);

        int topControlsY = panelY + 40;

        int skillButtonX = panelX + 10;
        for (int i = 0; i < AutoSkillHandler.skills.size(); i++) {
            Skill skill = AutoSkillHandler.skills.get(i);
            GuiButton btn = new ThemedButton(i, skillButtonX, topControlsY + i * 25, 80, 20, skill.name);
            if (i == selectedSkillIndex) {
                btn.enabled = false;
            }
            this.buttonList.add(btn);
        }

        Skill selectedSkill = AutoSkillHandler.skills.get(selectedSkillIndex);
        skillEnabledButton = new ToggleGuiButton(10, editorX, topControlsY, (editorWidth - 110) / 2, 20,
                I18n.format("gui.auto_skill.toggle",
                        I18n.format(selectedSkill.enabled ? "gui.auto_skill.state_on" : "gui.auto_skill.state_off")),
                selectedSkill.enabled);
        this.buttonList.add(skillEnabledButton);

        cooldownField = new GuiTextField(11, fontRenderer, editorX + editorWidth - 100, topControlsY, 100, 20);
        cooldownField.setText(String.valueOf(selectedSkill.cooldownSeconds));
        cooldownField.setValidator(s -> s.matches("\\d*"));

        // --- 核心修复：增加顶部控件和列表之间的间距，为提示文本留出空间 ---
        listX = editorX;
        listY = topControlsY + 40; // 从 25 增加到 40
        listWidth = editorWidth;
        listHeight = hexActionY - listY - 5; // 填充剩余空间
        // --- 修复结束 ---

        updateHexButtonStates();
    }

    private void updateHexButtonStates() {
        boolean hexSelected = selectedHexIndex != -1;
        Skill selectedSkill = AutoSkillHandler.skills.get(selectedSkillIndex);
        boolean isDefaultHex = selectedHexIndex == 0;
        btnEditHex.enabled = hexSelected && !isDefaultHex;
        btnRemoveHex.enabled = hexSelected && !isDefaultHex;
        btnMoveUp.enabled = hexSelected && selectedHexIndex > 0;
        btnMoveDown.enabled = hexSelected && selectedHexIndex < selectedSkill.hexPayloads.size() - 1;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        // ... (此方法保持不变)
        Skill selectedSkill = AutoSkillHandler.skills.get(selectedSkillIndex);

        if (button.id >= 0 && button.id < 4) { // Skill selection
            selectedSkillIndex = button.id;
            selectedHexIndex = -1;
            hexScrollOffset = 0;
            initGui();
        } else if (button.id == 10) { // Skill enabled toggle
            selectedSkill.enabled = !selectedSkill.enabled;
            skillEnabledButton.displayString = I18n.format("gui.auto_skill.toggle",
                    I18n.format(selectedSkill.enabled ? "gui.auto_skill.state_on" : "gui.auto_skill.state_off"));
            skillEnabledButton.setEnabledState(selectedSkill.enabled);
        } else if (button.id == 20) { // Add HEX
            mc.setScreen(new GuiTextInput(this, I18n.format("gui.auto_skill.input_new_hex"), newHex -> {
                if (newHex != null && !newHex.trim().isEmpty()) {
                    selectedSkill.hexPayloads.add(newHex.trim());
                }
                mc.setScreen(this);
            }));
        } else if (button.id == 21 && selectedHexIndex > 0) { // Edit HEX (skip default)
            String oldHex = selectedSkill.hexPayloads.get(selectedHexIndex);
            mc.setScreen(new GuiTextInput(this, I18n.format("gui.auto_skill.edit_hex"), oldHex, newHex -> {
                if (newHex != null && !newHex.trim().isEmpty()) {
                    selectedSkill.hexPayloads.set(selectedHexIndex, newHex.trim());
                }
                mc.setScreen(this);
            }));
        } else if (button.id == 22 && selectedHexIndex > 0) { // Remove HEX (skip default)
            selectedSkill.hexPayloads.remove(selectedHexIndex);
            selectedHexIndex = -1;
            updateHexButtonStates();
        } else if (button.id == 23 && btnMoveUp.enabled) { // Move Up
            Collections.swap(selectedSkill.hexPayloads, selectedHexIndex, selectedHexIndex - 1);
            selectedHexIndex--;
            updateHexButtonStates();
        } else if (button.id == 24 && btnMoveDown.enabled) { // Move Down
            Collections.swap(selectedSkill.hexPayloads, selectedHexIndex, selectedHexIndex + 1);
            selectedHexIndex++;
            updateHexButtonStates();
        } else if (button.id == 100) { // Save
            try {
                selectedSkill.cooldownSeconds = Integer.parseInt(cooldownField.getText());
            } catch (NumberFormatException e) {
                selectedSkill.cooldownSeconds = 1;
            }
            AutoSkillHandler.saveSkillConfig();
            mc.setScreen(parentScreen);
        } else if (button.id == 101) { // Cancel
            AutoSkillHandler.loadSkillConfig(); // Reload to discard changes
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);

        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        String idDisplay;
        if (sessionIdHex != null) {
            idDisplay = I18n.format("gui.auto_skill.id_ready", sessionIdHex);
        } else {
            idDisplay = I18n.format("gui.auto_skill.id_missing");
        }
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.auto_skill.title"), this.fontRenderer);
        this.drawString(fontRenderer, idDisplay, panelX + panelWidth - fontRenderer.getStringWidth(idDisplay) - 10,
                panelY + 10, 0xFFFFFF);

        // Draw editor labels
        drawString(fontRenderer, I18n.format("gui.auto_skill.cooldown"), cooldownField.x - 85, cooldownField.y + 6,
                0xFFFFFF);
        drawThemedTextField(cooldownField);

        // --- 核心修复：将提示文本绘制到独立的空白区域 ---
        drawString(fontRenderer, I18n.format("gui.auto_skill.tip"), listX, listY - 12, 0xFFFFFF);
        // --- 修复结束 ---

        // Draw HEX list
        drawRect(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);

        Skill selectedSkill = AutoSkillHandler.skills.get(selectedSkillIndex);
        int itemHeight = 15;
        int visibleItems = listHeight / itemHeight;
        maxHexScroll = Math.max(0, selectedSkill.hexPayloads.size() - visibleItems);
        hexScrollOffset = Math.max(0, Math.min(hexScrollOffset, maxHexScroll));

        for (int i = 0; i < visibleItems; i++) {
            int index = i + hexScrollOffset;
            if (index >= selectedSkill.hexPayloads.size())
                break;

            String hex = selectedSkill.hexPayloads.get(index);
            int itemY = listY + i * itemHeight;
            int bgColor = (index == selectedHexIndex) ? 0xFF0066AA : 0;
            if (index == 0) {
                bgColor = (index == selectedHexIndex) ? 0xFF2A6C9A : 0x402A6C9A;
            }

            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= itemY
                    && mouseY < itemY + itemHeight;
            if (isHovered && index != selectedHexIndex) {
                bgColor = 0x40FFFFFF;
            }

            drawRect(listX, itemY, listX + listWidth, itemY + itemHeight, bgColor);

            String hexPreview = hex.length() > 50 ? hex.substring(0, 47) + "..." : hex;
            String label = (index == 0) ? I18n.format("gui.auto_skill.default_prefix") : "";
            drawString(fontRenderer, (index + 1) + ". " + label + hexPreview, listX + 5, itemY + 4, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // ... (此方法保持不变)
        super.mouseClicked(mouseX, mouseY, mouseButton);
        cooldownField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / 15 + hexScrollOffset;
            if (clickedIndex >= 0
                    && clickedIndex < AutoSkillHandler.skills.get(selectedSkillIndex).hexPayloads.size()) {
                selectedHexIndex = clickedIndex;
                updateHexButtonStates();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // ... (此方法保持不变)
        super.keyTyped(typedChar, keyCode);
        if (cooldownField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                int cd = Integer.parseInt(cooldownField.getText());
                AutoSkillHandler.skills.get(selectedSkillIndex).cooldownSeconds = cd;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        // ... (此方法保持不变)
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                hexScrollOffset = Math.max(0, hexScrollOffset - 1);
            else
                hexScrollOffset = Math.min(maxHexScroll, hexScrollOffset + 1);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}







