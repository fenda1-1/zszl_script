package com.zszl.zszlScriptMod.gui.nbt;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.compat.ItemComponentCompat;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiNBTEditor extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final ItemStack itemStack;
    private CompoundTag nbt;

    private final List<NBTEntry> nbtEntries = new ArrayList<>();
    private final List<GuiTextField> keyFields = new ArrayList<>();
    private final List<GuiTextField> valueFields = new ArrayList<>();
    private final List<GuiButton> editButtons = new ArrayList<>();

    private GuiTextField itemNameField;
    private GuiTextField itemCountField;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static class NBTEntry {
        String key;
        String value;

        private NBTEntry(String key, Tag tag) {
            this.key = key == null ? "" : key;
            this.value = GuiNBTAdvanced.tagToString(tag);
        }
    }

    public GuiNBTEditor(GuiScreen parent, ItemStack stack) {
        this.parentScreen = parent;
        this.itemStack = stack == null ? ItemStack.EMPTY : stack;
        this.nbt = ItemComponentCompat.getCustomDataTag(this.itemStack);
        parseNbtToList();
    }

    private void parseNbtToList() {
        this.nbtEntries.clear();
        for (String key : this.nbt.keySet()) {
            this.nbtEntries.add(new NBTEntry(key, this.nbt.get(key)));
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.keyFields.clear();
        this.valueFields.clear();
        this.editButtons.clear();

        int panelWidth = 450;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 30;
        int panelHeight = this.height - 60;

        this.itemNameField = new GuiTextField(0, this.fontRenderer, panelX + 70, panelY + 25, 180, 20);
        this.itemNameField.setText(this.itemStack.getHoverName().getString());

        this.itemCountField = new GuiTextField(1, this.fontRenderer, panelX + panelWidth - 80, panelY + 25, 60, 20);
        this.itemCountField.setText(String.valueOf(this.itemStack.getCount()));

        this.buttonList.add(new GuiButton(100, panelX + 10, panelY + panelHeight - 25, 120, 20,
                I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new GuiButton(101, panelX + 140, panelY + panelHeight - 25, 120, 20,
                I18n.format("gui.common.cancel")));
        this.buttonList.add(new GuiButton(102, panelX + panelWidth - 90, panelY + panelHeight - 25, 80, 20,
                I18n.format("gui.nbt.editor.add_tag")));

        int listY = panelY + 60;
        int listHeight = panelHeight - 95;
        int itemHeight = 25;
        int visibleRows = Math.max(1, listHeight / itemHeight);
        this.maxScroll = Math.max(0, this.nbtEntries.size() - visibleRows);
        this.scrollOffset = Math.min(this.scrollOffset, this.maxScroll);

        for (int i = 0; i < visibleRows; i++) {
            int index = i + this.scrollOffset;
            if (index >= this.nbtEntries.size()) {
                break;
            }

            NBTEntry entry = this.nbtEntries.get(index);
            int currentY = listY + i * itemHeight;

            this.buttonList.add(new GuiButton(400 + i, panelX + 10, currentY, 20, 20, "§cX"));

            GuiTextField keyField = new GuiTextField(200 + i, this.fontRenderer, panelX + 35, currentY, 120, 20);
            keyField.setText(entry.key);
            this.keyFields.add(keyField);

            int valueFieldWidth = 210;
            GuiTextField valueField = new GuiTextField(300 + i, this.fontRenderer, panelX + 160, currentY,
                    valueFieldWidth, 20);
            String displayValue = entry.value == null ? "" : entry.value;
            boolean isLongText = this.fontRenderer.getStringWidth(displayValue) > valueFieldWidth - 10;
            if (isLongText) {
                displayValue = this.fontRenderer.trimStringToWidth(displayValue, valueFieldWidth - 10) + "...";
            }
            valueField.setText(displayValue);
            valueField.setEnabled(!isLongText);
            this.valueFields.add(valueField);

            GuiButton editButton = new GuiButton(500 + i, panelX + 160 + valueFieldWidth + 5, currentY, 20, 20, "...");
            editButton.visible = isLongText;
            this.buttonList.add(editButton);
            this.editButtons.add(editButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) {
            syncChangesFromFields();
            rebuildNbtFromList();
            ItemComponentCompat.setCustomDataTag(this.itemStack, this.nbt);

            String customName = this.itemNameField.getText() == null ? "" : this.itemNameField.getText().trim();
            ItemComponentCompat.setCustomName(this.itemStack, customName);

            try {
                int count = Integer.parseInt(this.itemCountField.getText().trim());
                this.itemStack.setCount(Math.max(1, Math.min(this.itemStack.getMaxStackSize(), count)));
            } catch (NumberFormatException ignored) {
                this.itemStack.setCount(Math.max(1, this.itemStack.getCount()));
            }

            this.mc.setScreen(this.parentScreen);
            return;
        }

        if (button.id == 101) {
            this.mc.setScreen(this.parentScreen);
            return;
        }

        if (button.id == 102) {
            syncChangesFromFields();
            this.nbtEntries.add(new NBTEntry("new_key", StringTag.valueOf("new_value")));
            initGui();
            return;
        }

        if (button.id >= 400 && button.id < 500) {
            syncChangesFromFields();
            int indexToRemove = button.id - 400 + this.scrollOffset;
            if (indexToRemove >= 0 && indexToRemove < this.nbtEntries.size()) {
                this.nbtEntries.remove(indexToRemove);
                initGui();
            }
            return;
        }

        if (button.id >= 500) {
            int index = button.id - 500 + this.scrollOffset;
            if (index < this.nbtEntries.size()) {
                NBTEntry entry = this.nbtEntries.get(index);
                this.mc.setScreen(new GuiTextInput(this,
                        I18n.format("gui.nbt.editor.edit_value", entry.key),
                        entry.value,
                        (newValue) -> {
                            entry.value = newValue == null ? "" : newValue;
                            this.mc.setScreen(this);
                        }));
            }
        }
    }

    private void syncChangesFromFields() {
        for (int i = 0; i < this.keyFields.size(); i++) {
            int entryIndex = i + this.scrollOffset;
            if (entryIndex >= this.nbtEntries.size()) {
                continue;
            }
            NBTEntry entry = this.nbtEntries.get(entryIndex);
            entry.key = this.keyFields.get(i).getText();
            if (this.valueFields.get(i).isEnabled()) {
                entry.value = this.valueFields.get(i).getText();
            }
        }
    }

    private void rebuildNbtFromList() {
        for (String key : new ArrayList<>(this.nbt.keySet())) {
            this.nbt.remove(key);
        }

        for (NBTEntry entry : this.nbtEntries) {
            if (entry.key == null || entry.key.trim().isEmpty()) {
                continue;
            }
            try {
                Tag parsedTag = GuiNBTAdvanced.parseTagValue(entry.value);
                this.nbt.put(entry.key.trim(), parsedTag);
            } catch (Exception e) {
                this.nbt.putString(entry.key.trim(), entry.value == null ? "" : entry.value);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 450;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 30;
        int panelHeight = this.height - 60;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawCenteredString(this.fontRenderer, I18n.format("gui.nbt.editor.title"), this.width / 2, panelY + 10,
                0xFFFFFF);

        drawString(this.fontRenderer, I18n.format("gui.nbt.editor.item_name"), panelX + 10, panelY + 30, 0xFFFFFF);
        drawThemedTextField(this.itemNameField);
        drawString(this.fontRenderer, I18n.format("gui.nbt.editor.count"), panelX + panelWidth - 120, panelY + 30,
                0xFFFFFF);
        drawThemedTextField(this.itemCountField);

        for (GuiTextField field : this.keyFields) {
            drawThemedTextField(field);
        }
        for (GuiTextField field : this.valueFields) {
            drawThemedTextField(field);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }

        this.itemNameField.textboxKeyTyped(typedChar, keyCode);
        this.itemCountField.textboxKeyTyped(typedChar, keyCode);
        for (GuiTextField field : this.keyFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
        for (GuiTextField field : this.valueFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            this.actionPerformed(this.buttonList.get(0));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.itemNameField.mouseClicked(mouseX, mouseY, mouseButton);
        this.itemCountField.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : this.keyFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (GuiTextField field : this.valueFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        this.itemNameField.updateCursorCounter();
        this.itemCountField.updateCursorCounter();
        for (GuiTextField field : this.keyFields) {
            field.updateCursorCounter();
        }
        for (GuiTextField field : this.valueFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0 || this.maxScroll <= 0) {
            return;
        }

        syncChangesFromFields();
        if (dWheel > 0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
        } else {
            this.scrollOffset = Math.min(this.maxScroll, this.scrollOffset + 1);
        }
        initGui();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
