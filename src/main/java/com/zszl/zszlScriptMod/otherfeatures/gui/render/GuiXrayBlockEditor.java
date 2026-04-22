package com.zszl.zszlScriptMod.otherfeatures.gui.render;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.BlockDisplayLookup;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiXrayBlockEditor extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_REMOVE = 2;
    private static final int BTN_CLEAR = 3;
    private static final int BTN_DEFAULT = 4;
    private static final int BTN_DONE = 5;
    private static final int ROW_HEIGHT = 22;

    private final GuiScreen parentScreen;
    private final List<String> entries = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int scrollOffset;
    private int selectedIndex = -1;

    private GuiTextField blockIdField;
    private GuiButton addButton;
    private GuiButton removeButton;
    private GuiButton clearButton;
    private GuiButton defaultButton;
    private GuiButton doneButton;

    private Block blockToAdd;
    private String blockIdToAdd = "";

    public GuiXrayBlockEditor(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.panelWidth = Math.min(560, Math.max(420, this.width - 24));
        this.panelHeight = Math.min(360, Math.max(300, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.listX = this.panelX + 12;
        this.listY = this.panelY + 52;
        this.listWidth = this.panelWidth - 24;
        this.listHeight = this.panelHeight - 138;

        int inputY = this.panelY + this.panelHeight - 72;
        this.blockIdField = new GuiTextField(10, this.fontRenderer, this.panelX + 42, inputY, this.panelWidth - 146, 20);
        this.blockIdField.setMaxStringLength(256);
        this.blockIdField.setFocused(true);
        this.blockIdField.setCanLoseFocus(true);
        this.blockIdField.setTextColor(0xFFEAF6FF);
        this.blockIdField.setDisabledTextColour(0xFF9FB2C8);

        int bottomY = this.panelY + this.panelHeight - 40;
        int gap = 6;
        int leftButtonWidth = 90;
        this.addButton = new ThemedButton(BTN_ADD, this.panelX + this.panelWidth - 96, inputY, 84, 20, "添加方块");
        this.removeButton = new ThemedButton(BTN_REMOVE, this.panelX + 12, bottomY, leftButtonWidth, 20, "移除选中");
        this.clearButton = new ThemedButton(BTN_CLEAR, this.panelX + 12 + (leftButtonWidth + gap), bottomY, leftButtonWidth, 20, "清空列表");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, this.panelX + 12 + (leftButtonWidth + gap) * 2, bottomY, leftButtonWidth, 20, "恢复默认");
        this.doneButton = new ThemedButton(BTN_DONE, this.panelX + this.panelWidth - 96, bottomY, 84, 20, "完成");
        this.buttonList.add(this.addButton);
        this.buttonList.add(this.removeButton);
        this.buttonList.add(this.clearButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.doneButton);

        refreshEntries();
        updateInputPreview();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.blockIdField != null) {
            this.blockIdField.updateCursorCounter();
        }
        updateInputPreview();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
        case BTN_ADD:
            if (!this.blockIdToAdd.isEmpty()) {
                RenderFeatureManager.setXrayBlockVisible(this.blockIdToAdd, true);
                this.blockIdField.setText("");
                refreshEntries();
                updateInputPreview();
            }
            return;
        case BTN_REMOVE:
            String selectedId = getSelectedBlockId();
            if (selectedId != null) {
                RenderFeatureManager.setXrayBlockVisible(selectedId, false);
                refreshEntries();
            }
            return;
        case BTN_CLEAR:
            RenderFeatureManager.clearXrayVisibleBlocksWithoutSave();
            refreshEntries();
            return;
        case BTN_DEFAULT:
            RenderFeatureManager.resetXrayVisibleBlocksWithoutSave();
            refreshEntries();
            return;
        case BTN_DONE:
            this.mc.setScreen(this.parentScreen);
            return;
        default:
            break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.blockIdField != null && this.blockIdField.isFocused() && this.blockIdField.textboxKeyTyped(typedChar, keyCode)) {
            updateInputPreview();
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            actionPerformed(this.addButton);
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE && getSelectedBlockId() != null) {
            actionPerformed(this.removeButton);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && isPointInList(mouseX, mouseY)) {
            int clickedIndex = getEntryIndexAt(mouseY);
            if (clickedIndex >= 0 && clickedIndex < this.entries.size()) {
                this.selectedIndex = clickedIndex;
                updateInputPreview();
                return;
            }
        }
        if (this.blockIdField != null) {
            this.blockIdField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (isPointInList(mouseX, mouseY)) {
            this.scrollOffset = clampInt(this.scrollOffset + (dWheel < 0 ? 1 : -1), 0, getMaxScroll());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "X光透视方块编辑", this.fontRenderer);

        drawString(this.fontRenderer, "§7这里只管理 X 光模式下允许保留渲染的方块列表。", this.panelX + 12, this.panelY + 22, GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer,
                "§7支持输入中文名或方块 ID，当前共 §f" + this.entries.size() + " §7项。",
                this.panelX + 12, this.panelY + 32, GuiTheme.SUB_TEXT);

        drawList(mouseX, mouseY);

        drawString(this.fontRenderer, "§b添加方块", this.panelX + 12, this.panelY + this.panelHeight - 90, 0xFFFFFFFF);
        drawBlockPreview(this.panelX + 12, this.panelY + this.panelHeight - 72);
        drawThemedTextField(this.blockIdField);

        String statusText;
        int statusColor;
        if (this.blockIdField.getText().trim().isEmpty()) {
            statusText = "§7输入方块名或 ID 后即可添加到透视列表。";
            statusColor = GuiTheme.SUB_TEXT;
        } else if (this.blockToAdd == null) {
            statusText = "§c未找到这个方块，请检查中文名称或方块 ID。";
            statusColor = 0xFFFF8A8A;
        } else if (RenderFeatureManager.isXrayBlockIdVisible(this.blockIdToAdd)) {
            statusText = "§e该方块已在透视列表中。";
            statusColor = 0xFFFFE08A;
        } else {
            statusText = "§a将添加: §f" + getDisplayName(this.blockIdToAdd);
            statusColor = 0xFF9CFFB2;
        }
        drawString(this.fontRenderer, statusText, this.panelX + 12, this.panelY + this.panelHeight - 16, statusColor);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isMouseOverField(mouseX, mouseY, this.blockIdField)) {
            drawHoveringText(java.util.Arrays.asList("§e方块输入", "§7支持中文名称、原版或模组方块 ID。", "§7示例: §fminecraft:diamond_ore"), mouseX, mouseY);
            return;
        }
        if (isPointInList(mouseX, mouseY)) {
            int hoveredIndex = getEntryIndexAt(mouseY);
            if (hoveredIndex >= 0 && hoveredIndex < this.entries.size()) {
                String blockId = this.entries.get(hoveredIndex);
                drawHoveringText(java.util.Arrays.asList("§e" + getDisplayName(blockId), "§8" + blockId), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void refreshEntries() {
        String selectedId = getSelectedBlockId();
        this.entries.clear();
        this.entries.addAll(RenderFeatureManager.getXrayVisibleBlockIds());
        this.selectedIndex = selectedId == null ? -1 : this.entries.indexOf(selectedId);
        if (this.selectedIndex < 0 && !this.entries.isEmpty()) {
            this.selectedIndex = 0;
        }
        this.scrollOffset = clampInt(this.scrollOffset, 0, getMaxScroll());
        updateInputPreview();
    }

    private void updateInputPreview() {
        String input = this.blockIdField == null ? "" : this.blockIdField.getText().trim();
        this.blockToAdd = BlockDisplayLookup.findBlockByUserInput(input);
        this.blockIdToAdd = this.blockToAdd == null ? "" : RenderFeatureManager.normalizeXrayBlockId(input);
        if (this.addButton != null) {
            this.addButton.enabled = !this.blockIdToAdd.isEmpty() && !RenderFeatureManager.isXrayBlockIdVisible(this.blockIdToAdd);
        }
        if (this.removeButton != null) {
            this.removeButton.enabled = getSelectedBlockId() != null;
        }
        if (this.clearButton != null) {
            this.clearButton.enabled = !this.entries.isEmpty();
        }
    }

    private void drawList(int mouseX, int mouseY) {
        drawRect(this.listX, this.listY - 4, this.listX + this.listWidth, this.listY + this.listHeight + 4, 0x44101822);
        drawRect(this.listX, this.listY - 4, this.listX + this.listWidth, this.listY - 3, 0xFF5FB8FF);
        drawRect(this.listX, this.listY + this.listHeight + 3, this.listX + this.listWidth, this.listY + this.listHeight + 4, 0xFF35536C);

        int visibleRows = Math.max(1, this.listHeight / ROW_HEIGHT);
        int start = this.scrollOffset;
        int end = Math.min(this.entries.size(), start + visibleRows);
        for (int i = start; i < end; i++) {
            int rowTop = this.listY + (i - start) * ROW_HEIGHT;
            boolean hovered = isPointInList(mouseX, mouseY) && getEntryIndexAt(mouseY) == i;
            boolean selected = i == this.selectedIndex;
            int bg = selected ? 0x664FA6D9 : (hovered ? 0x332C4258 : 0x22131A22);
            drawRect(this.listX + 2, rowTop, this.listX + this.listWidth - 2, rowTop + ROW_HEIGHT - 2, bg);

            String blockId = this.entries.get(i);
            drawBlockIcon(getBlock(blockId), this.listX + 6, rowTop + 3);
            drawString(this.fontRenderer, getDisplayName(blockId), this.listX + 28, rowTop + 3, 0xFFEAF6FF);
            drawString(this.fontRenderer,
                    this.fontRenderer.trimStringToWidth("§8" + blockId, this.listWidth - 36),
                    this.listX + 28, rowTop + 13, 0xFF9FB2C8);
        }

        if (this.entries.isEmpty()) {
            drawCenteredString(this.fontRenderer, "§7透视列表为空", this.listX + this.listWidth / 2, this.listY + this.listHeight / 2 - 4, 0xFFB6C5D6);
        }
    }

    private void drawBlockPreview(int x, int y) {
        drawRect(x, y, x + 20, y + 20, 0xFF31475D);
        drawRect(x + 1, y + 1, x + 19, y + 19, 0xFF16212D);
        drawBlockIcon(this.blockToAdd, x + 2, y + 2);
    }

    private void drawBlockIcon(Block block, int x, int y) {
        if (block == null || block == Blocks.AIR) {
            return;
        }
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return;
        }
        ItemStack stack = new ItemStack(block);
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(this.font, stack, x, y);
    }

    private String getSelectedBlockId() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.entries.size() ? this.entries.get(this.selectedIndex) : null;
    }

    private boolean isPointInList(int mouseX, int mouseY) {
        return mouseX >= this.listX && mouseX <= this.listX + this.listWidth
                && mouseY >= this.listY && mouseY <= this.listY + this.listHeight;
    }

    private int getEntryIndexAt(int mouseY) {
        int row = (mouseY - this.listY) / ROW_HEIGHT;
        int index = this.scrollOffset + row;
        return row < 0 || row >= Math.max(1, this.listHeight / ROW_HEIGHT) ? -1 : index;
    }

    private int getMaxScroll() {
        int visibleRows = Math.max(1, this.listHeight / ROW_HEIGHT);
        return Math.max(0, this.entries.size() - visibleRows);
    }

    private static Block getBlock(String blockId) {
        return RenderFeatureManager.resolveBlock(blockId);
    }

    private static String getDisplayName(String blockId) {
        Block block = getBlock(blockId);
        ItemStack stack = block == null ? ItemStack.EMPTY : new ItemStack(block);
        return stack.isEmpty() ? blockId : stack.getHoverName().getString();
    }

    private static int clampInt(int value, int minValue, int maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }
}
