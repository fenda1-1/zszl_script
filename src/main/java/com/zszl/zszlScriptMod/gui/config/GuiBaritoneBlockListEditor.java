package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class GuiBaritoneBlockListEditor extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_REMOVE = 2;
    private static final int BTN_CLEAR = 3;
    private static final int BTN_DEFAULT = 4;
    private static final int BTN_CANCEL = 5;
    private static final int BTN_DONE = 6;

    private static final int LIST_ROW_HEIGHT = 22;
    private static final long DOUBLE_CLICK_WINDOW_MS = 260L;

    private final GuiScreen parentScreen;
    private final String settingKey;
    private final String settingLabel;
    private final List<String> defaultBlockIds;
    private final List<String> workingBlockIds;
    private final Consumer<String> onSave;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int listX;
    private int listTop;
    private int listBottom;
    private int listWidth;
    private int listScroll;
    private int selectedIndex = -1;

    private long lastListClickTime;
    private int lastListClickIndex = -1;

    private GuiTextField blockIdField;
    private GuiButton addButton;
    private GuiButton removeButton;
    private GuiButton clearButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private GuiButton doneButton;

    private Block blockToAdd;
    private String blockIdToAdd = "";

    public GuiBaritoneBlockListEditor(GuiScreen parentScreen, String settingKey, String settingLabel,
            List<String> currentBlockIds, List<String> defaultBlockIds, Consumer<String> onSave) {
        this.parentScreen = parentScreen;
        this.settingKey = settingKey == null ? "" : settingKey;
        this.settingLabel = settingLabel == null ? "" : settingLabel;
        this.defaultBlockIds = BaritoneBlockSettingEditorSupport.copyNormalizedBlockIds(defaultBlockIds);
        this.workingBlockIds = BaritoneBlockSettingEditorSupport.copyNormalizedBlockIds(currentBlockIds);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        this.panelWidth = Math.min(580, Math.max(440, this.width - 24));
        this.panelHeight = Math.min(380, Math.max(316, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.listX = this.panelX + 12;
        this.listWidth = this.panelWidth - 24;
        this.listTop = this.panelY + 48;
        this.listBottom = this.panelY + this.panelHeight - 86;

        if (this.workingBlockIds.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex < 0 || this.selectedIndex >= this.workingBlockIds.size()) {
            this.selectedIndex = 0;
        }
        clampListScroll();

        int fieldX = this.panelX + 56;
        int fieldY = this.panelY + this.panelHeight - 74;
        this.blockIdField = new GuiTextField(10, this.fontRenderer, fieldX, fieldY, this.panelWidth - 170, 18);
        this.blockIdField.setMaxStringLength(96);
        this.blockIdField.setCanLoseFocus(true);
        this.blockIdField.setFocused(true);

        int buttonY = this.panelY + this.panelHeight - 44;
        int gap = 6;
        int buttonWidth = (this.panelWidth - 20 - gap * 4) / 5;
        int startX = this.panelX + 10;

        this.addButton = new ThemedButton(BTN_ADD, this.panelX + this.panelWidth - 104, fieldY - 1, 94, 20, "添加方块");
        this.removeButton = new ThemedButton(BTN_REMOVE, startX, buttonY, buttonWidth, 20, "移除选中");
        this.clearButton = new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap), buttonY, buttonWidth, 20, "清空列表");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20,
                "恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, startX + (buttonWidth + gap) * 3, buttonY, buttonWidth, 20, "取消");
        this.doneButton = new ThemedButton(BTN_DONE, startX + (buttonWidth + gap) * 4, buttonY, buttonWidth, 20, "完成");

        this.buttonList.add(this.addButton);
        this.buttonList.add(this.removeButton);
        this.buttonList.add(this.clearButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);
        this.buttonList.add(this.doneButton);

        updateInputPreview();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
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
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
            case BTN_ADD:
                addCurrentBlock();
                return;
            case BTN_REMOVE:
                removeSelectedBlock();
                return;
            case BTN_CLEAR:
                this.workingBlockIds.clear();
                this.selectedIndex = -1;
                clampListScroll();
                updateInputPreview();
                return;
            case BTN_DEFAULT:
                this.workingBlockIds.clear();
                this.workingBlockIds.addAll(this.defaultBlockIds);
                this.selectedIndex = this.workingBlockIds.isEmpty() ? -1 : 0;
                clampListScroll();
                updateInputPreview();
                return;
            case BTN_CANCEL:
                this.mc.setScreen(this.parentScreen);
                return;
            case BTN_DONE:
                if (this.onSave != null) {
                    this.onSave.accept(BaritoneBlockSettingEditorSupport.serializeBlockListValue(this.workingBlockIds));
                }
                this.mc.setScreen(this.parentScreen);
                return;
            default:
                return;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (isInsideList(mouseX, mouseY)) {
            scrollListBy(wheel < 0 ? 1 : -1);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0D && isInsideList((int) mouseX, (int) mouseY)) {
            scrollListBy(delta < 0.0D ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.blockIdField != null) {
            this.blockIdField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 0) {
            handleListClick(mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.blockIdField != null && this.blockIdField.textboxKeyTyped(typedChar, keyCode)) {
            updateInputPreview();
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (this.addButton != null && this.addButton.enabled) {
                actionPerformed(this.addButton);
            } else if (this.removeButton != null && this.removeButton.enabled) {
                actionPerformed(this.removeButton);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            if (this.removeButton != null && this.removeButton.enabled) {
                actionPerformed(this.removeButton);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_UP) {
            moveSelection(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            moveSelection(1);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "Baritone方块列表编辑", this.fontRenderer);

        drawString(this.fontRenderer, "§7设置项: §f" + this.settingKey, this.panelX + 12, this.panelY + 22, GuiTheme.SUB_TEXT);
        String desc = this.settingLabel.isEmpty() ? "§7可视化编辑当前方块列表。"
                : "§7说明: §f" + this.settingLabel;
        drawString(this.fontRenderer, trimToWidth(desc, this.panelWidth - 24), this.panelX + 12, this.panelY + 32, GuiTheme.SUB_TEXT);

        drawList(mouseX, mouseY);

        int inputLabelY = this.panelY + this.panelHeight - 84;
        drawString(this.fontRenderer, "§b添加方块", this.panelX + 12, inputLabelY, 0xFFFFFFFF);
        drawBlockPreview(this.panelX + 14, this.panelY + this.panelHeight - 66);
        drawThemedTextField(this.blockIdField);
        if (this.blockIdField != null
                && (this.blockIdField.getText() == null || this.blockIdField.getText().isEmpty())
                && !this.blockIdField.isFocused()) {
            drawString(this.fontRenderer, "输入方块名或 ID", this.blockIdField.x + 4, this.blockIdField.y + 5, 0xFF7B8A99);
        }

        drawStatusLine();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawTooltips(mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void addCurrentBlock() {
        if (this.blockIdToAdd.isEmpty() || this.workingBlockIds.contains(this.blockIdToAdd)) {
            return;
        }
        this.workingBlockIds.add(this.blockIdToAdd);
        this.selectedIndex = this.workingBlockIds.size() - 1;
        ensureSelectionVisible();
        if (this.blockIdField != null) {
            this.blockIdField.setText("");
        }
        updateInputPreview();
    }

    private void removeSelectedBlock() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.workingBlockIds.size()) {
            return;
        }
        this.workingBlockIds.remove(this.selectedIndex);
        if (this.workingBlockIds.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex >= this.workingBlockIds.size()) {
            this.selectedIndex = this.workingBlockIds.size() - 1;
        }
        clampListScroll();
        updateInputPreview();
    }

    private void moveSelection(int delta) {
        if (this.workingBlockIds.isEmpty()) {
            this.selectedIndex = -1;
            updateInputPreview();
            return;
        }
        if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
        } else {
            this.selectedIndex = Math.max(0, Math.min(this.workingBlockIds.size() - 1, this.selectedIndex + delta));
        }
        ensureSelectionVisible();
        updateInputPreview();
    }

    private void handleListClick(int mouseX, int mouseY) {
        int clickedIndex = getListIndexAt(mouseX, mouseY);
        if (clickedIndex < 0 || clickedIndex >= this.workingBlockIds.size()) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean doubleClick = clickedIndex == this.lastListClickIndex
                && now - this.lastListClickTime <= DOUBLE_CLICK_WINDOW_MS;

        this.selectedIndex = clickedIndex;
        ensureSelectionVisible();
        this.lastListClickIndex = clickedIndex;
        this.lastListClickTime = now;
        updateInputPreview();

        if (doubleClick) {
            removeSelectedBlock();
            this.lastListClickIndex = -1;
            this.lastListClickTime = 0L;
        }
    }

    private void scrollListBy(int amount) {
        if (amount == 0) {
            return;
        }
        int max = getMaxListScroll();
        if (max <= 0) {
            this.listScroll = 0;
            return;
        }
        this.listScroll = Math.max(0, Math.min(max, this.listScroll + amount));
    }

    private void ensureSelectionVisible() {
        if (this.selectedIndex < 0) {
            return;
        }
        int visible = getVisibleRowCount();
        if (visible <= 0) {
            return;
        }
        if (this.selectedIndex < this.listScroll) {
            this.listScroll = this.selectedIndex;
        } else if (this.selectedIndex >= this.listScroll + visible) {
            this.listScroll = this.selectedIndex - visible + 1;
        }
        clampListScroll();
    }

    private int getVisibleRowCount() {
        return Math.max(1, (this.listBottom - this.listTop) / LIST_ROW_HEIGHT);
    }

    private int getMaxListScroll() {
        return Math.max(0, this.workingBlockIds.size() - getVisibleRowCount());
    }

    private void clampListScroll() {
        this.listScroll = Math.max(0, Math.min(this.listScroll, getMaxListScroll()));
        if (this.workingBlockIds.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex < 0 || this.selectedIndex >= this.workingBlockIds.size()) {
            this.selectedIndex = 0;
        }
    }

    private boolean isInsideList(int mouseX, int mouseY) {
        return mouseX >= this.listX && mouseX <= this.listX + this.listWidth
                && mouseY >= this.listTop && mouseY <= this.listBottom;
    }

    private int getListIndexAt(int mouseX, int mouseY) {
        if (!isInsideList(mouseX, mouseY)) {
            return -1;
        }
        int localY = mouseY - this.listTop;
        int row = localY / LIST_ROW_HEIGHT;
        int index = this.listScroll + row;
        return index >= 0 && index < this.workingBlockIds.size() ? index : -1;
    }

    private String getSelectedBlockId() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.workingBlockIds.size()) {
            return null;
        }
        return this.workingBlockIds.get(this.selectedIndex);
    }

    private void updateInputPreview() {
        String typed = this.blockIdField == null ? "" : this.blockIdField.getText();
        this.blockIdToAdd = BaritoneBlockSettingEditorSupport.normalizeBlockId(typed);
        this.blockToAdd = this.blockIdToAdd.isEmpty() ? null : BaritoneBlockSettingEditorSupport.resolveBlock(this.blockIdToAdd);

        if (this.addButton != null) {
            this.addButton.enabled = this.blockToAdd != null && !this.workingBlockIds.contains(this.blockIdToAdd);
        }
        if (this.removeButton != null) {
            this.removeButton.enabled = getSelectedBlockId() != null;
        }
        if (this.clearButton != null) {
            this.clearButton.enabled = !this.workingBlockIds.isEmpty();
        }
        if (this.defaultButton != null) {
            this.defaultButton.enabled = !this.workingBlockIds.equals(this.defaultBlockIds);
        }
    }

    private void drawList(int mouseX, int mouseY) {
        drawRect(this.listX, this.listTop - 4, this.listX + this.listWidth, this.listBottom + 4, 0x44101822);
        drawRect(this.listX, this.listTop - 4, this.listX + this.listWidth, this.listTop - 3, 0xFF5FB8FF);
        drawRect(this.listX, this.listBottom + 3, this.listX + this.listWidth, this.listBottom + 4, 0xFF35536C);

        int visibleRows = getVisibleRowCount();
        int rowX = this.listX + 2;
        int rowWidth = this.listWidth - 12;

        if (this.workingBlockIds.isEmpty()) {
            drawCenteredString(this.fontRenderer, "列表为空，输入方块后点击 添加方块", this.listX + this.listWidth / 2,
                    this.listTop + (this.listBottom - this.listTop) / 2 - 4, GuiTheme.SUB_TEXT);
            return;
        }

        clampListScroll();
        for (int i = 0; i < visibleRows; i++) {
            int index = this.listScroll + i;
            if (index >= this.workingBlockIds.size()) {
                break;
            }

            int rowY = this.listTop + i * LIST_ROW_HEIGHT;
            boolean selected = index == this.selectedIndex;
            boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth
                    && mouseY >= rowY && mouseY <= rowY + LIST_ROW_HEIGHT - 2;

            GuiTheme.drawButtonFrameSafe(rowX, rowY, rowWidth, LIST_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String blockId = this.workingBlockIds.get(index);
            String displayName = BaritoneBlockSettingEditorSupport.getDisplayName(blockId);
            drawString(this.fontRenderer, trimToWidth(displayName, rowWidth - 48), rowX + 26, rowY + 3, 0xFFEAF6FF);
            drawString(this.fontRenderer, "§8" + trimToWidth(blockId, rowWidth - 48), rowX + 26, rowY + 13, 0xFF9FB2C8);
            drawString(this.fontRenderer, "§7" + (index + 1), rowX + 6, rowY + 7, 0xFF91A7BC);
        }

        if (this.workingBlockIds.size() > visibleRows) {
            int trackHeight = this.listBottom - this.listTop;
            int maxScroll = getMaxListScroll();
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, this.workingBlockIds.size())) * trackHeight));
            int track = Math.max(1, trackHeight - thumbHeight);
            int thumbY = this.listTop + (int) ((this.listScroll / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(this.listX + this.listWidth - 8, this.listTop, 4, trackHeight, thumbY, thumbHeight);
        }
    }

    private void drawStatusLine() {
        String input = this.blockIdField == null ? "" : this.blockIdField.getText();
        String statusText;
        int statusColor;
        if (input == null || input.trim().isEmpty()) {
            statusText = "§7当前已选择 §f" + this.workingBlockIds.size() + " §7个方块。";
            statusColor = GuiTheme.SUB_TEXT;
        } else if (this.blockToAdd == null) {
            statusText = "§c未找到这个方块，请检查中文名称或方块 ID。";
            statusColor = 0xFFFF8A8A;
        } else if (this.workingBlockIds.contains(this.blockIdToAdd)) {
            statusText = "§e该方块已在列表中，可直接从列表里选中后移除。";
            statusColor = 0xFFFFE08A;
        } else {
            statusText = "§a将添加: §f" + BaritoneBlockSettingEditorSupport.getDisplayName(this.blockIdToAdd);
            statusColor = 0xFF9CFFB2;
        }
        drawString(this.fontRenderer, trimToWidth(statusText, this.panelWidth - 24),
                this.panelX + 12, this.panelY + this.panelHeight - 20, statusColor);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        if (isMouseOverField(mouseX, mouseY, this.blockIdField)) {
            List<String> tooltip = new ArrayList<String>();
            tooltip.add("§e方块输入");
            tooltip.add("§7支持中文名称、原版或模组方块 ID。");
            tooltip.add("§7示例: §fminecraft:diamond_ore");
            drawHoveringText(tooltip, mouseX, mouseY);
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, this.removeButton) && this.removeButton.enabled) {
            String selectedId = getSelectedBlockId();
            if (selectedId != null) {
                drawHoveringText(Arrays.asList("§e移除选中", "§7当前选中: §f"
                        + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId), "§8" + selectedId), mouseX, mouseY);
                return;
            }
        }

        int hoverIndex = getListIndexAt(mouseX, mouseY);
        if (hoverIndex >= 0 && hoverIndex < this.workingBlockIds.size()) {
            String blockId = this.workingBlockIds.get(hoverIndex);
            drawHoveringText(Arrays.asList(
                    "§e" + BaritoneBlockSettingEditorSupport.getDisplayName(blockId),
                    "§8" + blockId,
                    "§7双击可快速移除该方块"), mouseX, mouseY);
        }
    }

    private void drawBlockPreview(int x, int y) {
        drawRect(x - 2, y - 2, x + 18, y + 18, 0xFF31475D);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF16212D);
        ItemStack stack = BaritoneBlockSettingEditorSupport.getBlockStack(this.blockToAdd);
        if (stack.isEmpty()) {
            drawString(this.fontRenderer, "?", x + 6, y + 5, 0xFF86A4C0);
            return;
        }

        try {
            GuiGraphics graphics = GuiCompatContext.current();
            if (graphics != null) {
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(this.mc.font, stack, x, y);
                return;
            }
        } catch (Throwable ignored) {
            String symbol = BaritoneBlockSettingEditorSupport.getDisplayName(this.blockIdToAdd);
            if (symbol == null || symbol.trim().isEmpty()) {
                symbol = "#";
            } else {
                symbol = symbol.trim().substring(0, 1);
            }
            drawString(this.fontRenderer, symbol, x + 6, y + 5, 0xFFEAF6FF);
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return this.fontRenderer.trimStringToWidth(text,
                Math.max(0, maxWidth - this.fontRenderer.getStringWidth(".."))) + "..";
    }
}
