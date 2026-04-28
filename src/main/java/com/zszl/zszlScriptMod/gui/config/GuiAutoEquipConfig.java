package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler.ArmorSlot;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler.EquipmentSet;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler.SlotConfig;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.*;

public class GuiAutoEquipConfig extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final String setName;
    private EquipmentSet currentSet; // 使用新的 EquipmentSet

    private List<GuiTextField> allTextFields = new ArrayList<>();
    private Map<ArmorSlot, GuiTextField> itemNameFields = new EnumMap<>(ArmorSlot.class);
    private Map<ArmorSlot, ToggleGuiButton> toggleButtons = new EnumMap<>(ArmorSlot.class);
    private Map<ArmorSlot, ToggleGuiButton> leaveOneButtons = new EnumMap<>(ArmorSlot.class);
    private ToggleGuiButton sequentialButton; // 新的顺序穿戴按钮
    private final Map<GuiButton, String> tooltips = new HashMap<>();

    public GuiAutoEquipConfig(GuiScreen parent, String setName) {
        this.parentScreen = parent;
        this.setName = setName;
        // 深拷贝一份配置进行编辑
        EquipmentSet originalSet = AutoEquipHandler.getSet(setName);
        if (originalSet != null) {
            this.currentSet = new EquipmentSet();
            this.currentSet.sequentialEquip = originalSet.sequentialEquip;
            for (Map.Entry<ArmorSlot, SlotConfig> entry : originalSet.slots.entrySet()) {
                SlotConfig original = entry.getValue();
                SlotConfig copy = new SlotConfig();
                copy.enabled = original.enabled;
                copy.itemName = original.itemName;
                copy.leaveOne = original.leaveOne;
                this.currentSet.slots.put(entry.getKey(), copy);
            }
        } else {
            this.currentSet = new EquipmentSet(); // Fallback
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allTextFields.clear();
        this.itemNameFields.clear();
        this.toggleButtons.clear();
        this.leaveOneButtons.clear();
        this.tooltips.clear();

        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 100;
        int currentY = panelY + 35;

        for (ArmorSlot slot : ArmorSlot.values()) {
            SlotConfig config = currentSet.slots.get(slot);
            if (config == null)
                continue;

            int controlX = panelX + 10;

            ToggleGuiButton toggle = new ToggleGuiButton(slot.ordinal(), controlX, currentY, 50, 20,
                    enabledText(config.enabled), config.enabled);
            this.buttonList.add(toggle);
            toggleButtons.put(slot, toggle);
            controlX += 55;

            ToggleGuiButton leaveOneBtn = new ToggleGuiButton(slot.ordinal() + 8, controlX, currentY, 50, 20,
                    leaveOneText(config.leaveOne), config.leaveOne);
            this.buttonList.add(leaveOneBtn);
            leaveOneButtons.put(slot, leaveOneBtn);
            tooltips.put(leaveOneBtn, I18n.format("gui.auto_equip_config.tip.keep_one"));
            controlX += 55;

            GuiTextField nameField = new GuiTextField(slot.ordinal() + 12, fontRenderer, controlX, currentY, 260, 20);
            nameField.setText(config.itemName);
            nameField.setMaxStringLength(Integer.MAX_VALUE);
            itemNameFields.put(slot, nameField);
            allTextFields.add(nameField);

            currentY += 28;
        }

        currentY += 10;
        // --- 核心修改：添加顺序穿戴按钮 ---
        this.sequentialButton = new ToggleGuiButton(20, panelX + 10, currentY, 120, 20,
                equipModeText(currentSet.sequentialEquip), currentSet.sequentialEquip);
        this.buttonList.add(sequentialButton);
        tooltips.put(sequentialButton, I18n.format("gui.auto_equip_config.tip.mode"));

        this.buttonList.add(new GuiButton(100, panelX + panelWidth - 130, currentY, 120, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new GuiButton(101, panelX + panelWidth - 260, currentY, 120, 20,
                I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 0 && button.id < 4) { // 开关
            ToggleGuiButton toggle = (ToggleGuiButton) button;
            toggle.setEnabledState(!toggle.getEnabledState());
            toggle.displayString = enabledText(toggle.getEnabledState());
        } else if (button.id >= 8 && button.id < 12) { // 留一件
            ToggleGuiButton toggle = (ToggleGuiButton) button;
            toggle.setEnabledState(!toggle.getEnabledState());
            toggle.displayString = leaveOneText(toggle.getEnabledState());
        } else if (button.id == 20) { // 顺序穿戴开关
            sequentialButton.setEnabledState(!sequentialButton.getEnabledState());
            sequentialButton.displayString = equipModeText(sequentialButton.getEnabledState());
        } else if (button.id == 100) { // 保存
            EquipmentSet setToSave = AutoEquipHandler.getSet(this.setName);
            if (setToSave != null) {
                setToSave.sequentialEquip = sequentialButton.getEnabledState();
                for (ArmorSlot slot : ArmorSlot.values()) {
                    SlotConfig config = setToSave.slots.get(slot);
                    if (config == null)
                        continue;
                    config.enabled = toggleButtons.get(slot).getEnabledState();
                    config.leaveOne = leaveOneButtons.get(slot).getEnabledState();
                    config.itemName = itemNameFields.get(slot).getText();
                }
                AutoEquipHandler.saveConfig();
            }
            mc.setScreen(parentScreen);
        } else if (button.id == 101) { // 取消
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 400;
        int panelHeight = 180; // 增加高度以容纳新按钮
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 100;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.auto_equip_config.title", this.setName),
                this.width / 2, panelY + 5, 0xFFFFFF);

        int headerY = panelY + 20;
        drawString(fontRenderer, I18n.format("gui.auto_equip_config.header.toggle"), panelX + 22, headerY, 0xAAAAAA);
        drawString(fontRenderer, I18n.format("gui.auto_equip_config.header.keep_one"), panelX + 77, headerY, 0xAAAAAA);
        drawString(fontRenderer, I18n.format("gui.auto_equip_config.header.item_name"), panelX + 125, headerY,
                0xAAAAAA);

        for (GuiTextField field : allTextFields)
            drawThemedTextField(field);
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiButton button : this.buttonList) {
            if (button.isMouseOver() && tooltips.containsKey(button)) {
                drawHoveringText(Arrays.asList(tooltips.get(button).split("\n")), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        for (GuiTextField field : allTextFields)
            field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : allTextFields)
            field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String enabledText(boolean enabled) {
        return I18n.format(enabled ? "gui.auto_equip_config.enabled_on" : "gui.auto_equip_config.enabled_off");
    }

    private String leaveOneText(boolean enabled) {
        return I18n.format(enabled ? "gui.auto_equip_config.keep_one_on" : "gui.auto_equip_config.keep_one_off");
    }

    private String equipModeText(boolean sequential) {
        return I18n.format(sequential ? "gui.auto_equip_config.mode.sequential" : "gui.auto_equip_config.mode.smart");
    }
}






