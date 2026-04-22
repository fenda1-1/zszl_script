package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GuiBaritoneBlockMapEditor extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_EDIT = 2;
    private static final int BTN_REMOVE = 3;
    private static final int BTN_CLEAR = 4;
    private static final int BTN_DEFAULT = 5;
    private static final int BTN_CANCEL = 6;
    private static final int BTN_DONE = 7;

    private static final int LIST_ROW_HEIGHT = 34;
    private static final long DOUBLE_CLICK_WINDOW_MS = 260L;

    private final GuiScreen parentScreen;
    private final String settingKey;
    private final String settingLabel;
    private final LinkedHashMap<String, List<String>> defaultMappings;
    private final LinkedHashMap<String, List<String>> workingMappings;
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

    private GuiTextField sourceBlockField;
    private GuiButton addButton;
    private GuiButton editButton;
    private GuiButton removeButton;
    private GuiButton clearButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private GuiButton doneButton;

    private Block sourceBlockToAdd;
    private String sourceBlockIdToAdd = "";

    public GuiBaritoneBlockMapEditor(GuiScreen parentScreen, String settingKey, String settingLabel,
            Map<String, List<String>> currentMappings,
            Map<String, List<String>> defaultMappings,
            Consumer<String> onSave) {
        this.parentScreen = parentScreen;
        this.settingKey = settingKey == null ? "" : settingKey;
        this.settingLabel = settingLabel == null ? "" : settingLabel;
        this.defaultMappings = BaritoneBlockSettingEditorSupport.copyNormalizedBlockMap(defaultMappings);
        this.workingMappings = BaritoneBlockSettingEditorSupport.copyNormalizedBlockMap(currentMappings);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();

        this.panelWidth = Math.min(660, Math.max(500, this.width - 24));
        this.panelHeight = Math.min(410, Math.max(332, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.listX = this.panelX + 12;
        this.listWidth = this.panelWidth - 24;
        this.listTop = this.panelY + 48;
        this.listBottom = this.panelY + this.panelHeight - 86;

        if (getSourceKeys().isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex < 0 || this.selectedIndex >= getSourceKeys().size()) {
            this.selectedIndex = 0;
        }
        clampListScroll();

        int fieldX = this.panelX + 56;
        int fieldY = this.panelY + this.panelHeight - 74;
        this.sourceBlockField = new GuiTextField(10, this.fontRenderer, fieldX, fieldY, this.panelWidth - 170, 18);
        this.sourceBlockField.setMaxStringLength(96);
        this.sourceBlockField.setCanLoseFocus(true);
        this.sourceBlockField.setFocused(true);

        int buttonY = this.panelY + this.panelHeight - 44;
        int gap = 6;
        int buttonWidth = (this.panelWidth - 20 - gap * 5) / 6;
        int startX = this.panelX + 10;

        this.addButton = new ThemedButton(BTN_ADD, this.panelX + this.panelWidth - 104, fieldY - 1, 94, 20, "新增条目");
        this.editButton = new ThemedButton(BTN_EDIT, startX, buttonY, buttonWidth, 20, "编辑选中");
        this.removeButton = new ThemedButton(BTN_REMOVE, startX + (buttonWidth + gap), buttonY, buttonWidth, 20, "移除选中");
        this.clearButton = new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20, "清空映射");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, startX + (buttonWidth + gap) * 3, buttonY, buttonWidth, 20, "恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, startX + (buttonWidth + gap) * 4, buttonY, buttonWidth, 20, "取消");
        this.doneButton = new ThemedButton(BTN_DONE, startX + (buttonWidth + gap) * 5, buttonY, buttonWidth, 20, "完成");

        this.buttonList.add(this.addButton);
        this.buttonList.add(this.editButton);
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
        if (this.sourceBlockField != null) {
            this.sourceBlockField.updateCursorCounter();
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
                addCurrentSource();
                return;
            case BTN_EDIT:
                openSelectedMappingEditor();
                return;
            case BTN_REMOVE:
                removeSelectedMapping();
                return;
            case BTN_CLEAR:
                this.workingMappings.clear();
                this.selectedIndex = -1;
                clampListScroll();
                updateInputPreview();
                return;
            case BTN_DEFAULT:
                this.workingMappings.clear();
                this.workingMappings.putAll(BaritoneBlockSettingEditorSupport.copyNormalizedBlockMap(this.defaultMappings));
                this.selectedIndex = getSourceKeys().isEmpty() ? -1 : 0;
                clampListScroll();
                updateInputPreview();
                return;
            case BTN_CANCEL:
                this.mc.setScreen(this.parentScreen);
                return;
            case BTN_DONE:
                if (this.onSave != null) {
                    this.onSave.accept(BaritoneBlockSettingEditorSupport.serializeBlockMapValue(this.workingMappings));
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
        if (this.sourceBlockField != null) {
            this.sourceBlockField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton == 0) {
            handleListClick(mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.sourceBlockField != null && this.sourceBlockField.textboxKeyTyped(typedChar, keyCode)) {
            updateInputPreview();
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (this.sourceBlockField != null && this.sourceBlockField.isFocused()
                    && this.sourceBlockField.getText() != null
                    && !this.sourceBlockField.getText().trim().isEmpty()) {
                if (this.addButton != null && this.addButton.enabled) {
                    actionPerformed(this.addButton);
                }
            } else if (this.editButton != null && this.editButton.enabled) {
                actionPerformed(this.editButton);
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
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "Baritone方块映射编辑", this.fontRenderer);

        drawString(this.fontRenderer, "§7设置项: §f" + this.settingKey, this.panelX + 12, this.panelY + 22, GuiTheme.SUB_TEXT);
        String desc = this.settingLabel.isEmpty() ? "§7可视化编辑 源方块 -> 替代方块列表。"
                : "§7说明: §f" + this.settingLabel;
        drawString(this.fontRenderer, trimToWidth(desc, this.panelWidth - 24), this.panelX + 12, this.panelY + 32, GuiTheme.SUB_TEXT);

        drawList(mouseX, mouseY);

        drawString(this.fontRenderer, "§b新增源方块", this.panelX + 12, this.panelY + this.panelHeight - 84, 0xFFFFFFFF);
        drawBlockPreview(this.panelX + 14, this.panelY + this.panelHeight - 66);
        drawThemedTextField(this.sourceBlockField);
        if (this.sourceBlockField != null
                && (this.sourceBlockField.getText() == null || this.sourceBlockField.getText().isEmpty())
                && !this.sourceBlockField.isFocused()) {
            drawString(this.fontRenderer, "输入要配置替代方块的源方块名称或 ID", this.sourceBlockField.x + 4,
                    this.sourceBlockField.y + 5, 0xFF7B8A99);
        }

        drawStatusLine();
        super.drawScreen(mouseX, mouseY, partialTicks);

        drawTooltips(mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void addCurrentSource() {
        if (this.sourceBlockIdToAdd.isEmpty() || this.workingMappings.containsKey(this.sourceBlockIdToAdd)) {
            return;
        }
        this.workingMappings.put(this.sourceBlockIdToAdd, new ArrayList<String>());
        this.selectedIndex = getSourceKeys().size() - 1;
        ensureSelectionVisible();
        if (this.sourceBlockField != null) {
            this.sourceBlockField.setText("");
        }
        updateInputPreview();
    }

    private void removeSelectedMapping() {
        String selectedId = getSelectedSourceId();
        if (selectedId == null) {
            return;
        }
        this.workingMappings.remove(selectedId);
        if (getSourceKeys().isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex >= getSourceKeys().size()) {
            this.selectedIndex = getSourceKeys().size() - 1;
        }
        clampListScroll();
        updateInputPreview();
    }

    private void moveSelection(int delta) {
        List<String> keys = getSourceKeys();
        if (keys.isEmpty()) {
            this.selectedIndex = -1;
            updateInputPreview();
            return;
        }
        if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
        } else {
            this.selectedIndex = Math.max(0, Math.min(keys.size() - 1, this.selectedIndex + delta));
        }
        ensureSelectionVisible();
        updateInputPreview();
    }

    private void handleListClick(int mouseX, int mouseY) {
        int clickedIndex = getListIndexAt(mouseX, mouseY);
        List<String> keys = getSourceKeys();
        if (clickedIndex < 0 || clickedIndex >= keys.size()) {
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
            openSelectedMappingEditor();
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
        return Math.max(0, getSourceKeys().size() - getVisibleRowCount());
    }

    private void clampListScroll() {
        List<String> keys = getSourceKeys();
        this.listScroll = Math.max(0, Math.min(this.listScroll, getMaxListScroll()));
        if (keys.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex < 0 || this.selectedIndex >= keys.size()) {
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
        return index >= 0 && index < getSourceKeys().size() ? index : -1;
    }

    private List<String> getSourceKeys() {
        return new ArrayList<String>(this.workingMappings.keySet());
    }

    private String getSelectedSourceId() {
        List<String> keys = getSourceKeys();
        if (this.selectedIndex < 0 || this.selectedIndex >= keys.size()) {
            return null;
        }
        return keys.get(this.selectedIndex);
    }

    private void updateInputPreview() {
        String typed = this.sourceBlockField == null ? "" : this.sourceBlockField.getText();
        this.sourceBlockIdToAdd = BaritoneBlockSettingEditorSupport.normalizeBlockId(typed);
        this.sourceBlockToAdd = this.sourceBlockIdToAdd.isEmpty() ? null
                : BaritoneBlockSettingEditorSupport.resolveBlock(this.sourceBlockIdToAdd);

        if (this.addButton != null) {
            this.addButton.enabled = this.sourceBlockToAdd != null && !this.workingMappings.containsKey(this.sourceBlockIdToAdd);
        }
        if (this.editButton != null) {
            this.editButton.enabled = getSelectedSourceId() != null;
        }
        if (this.removeButton != null) {
            this.removeButton.enabled = getSelectedSourceId() != null;
        }
        if (this.clearButton != null) {
            this.clearButton.enabled = !this.workingMappings.isEmpty();
        }
        if (this.defaultButton != null) {
            this.defaultButton.enabled = !this.workingMappings.equals(this.defaultMappings);
        }
    }

    private void openSelectedMappingEditor() {
        final String selectedId = getSelectedSourceId();
        if (selectedId == null) {
            return;
        }

        List<String> currentTargets = this.workingMappings.containsKey(selectedId)
                ? this.workingMappings.get(selectedId)
                : new ArrayList<String>();
        List<String> defaultTargets = this.defaultMappings.containsKey(selectedId)
                ? this.defaultMappings.get(selectedId)
                : new ArrayList<String>();
        this.mc.setScreen(new GuiBaritoneBlockListEditor(this, selectedId,
                "编辑 " + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId) + " 的替代方块列表",
                currentTargets, defaultTargets,
                new Consumer<String>() {
                    @Override
                    public void accept(String value) {
                        List<String> parsedTargets = BaritoneBlockSettingEditorSupport.parseBlockListValue(value);
                        if (parsedTargets.isEmpty()) {
                            GuiBaritoneBlockMapEditor.this.workingMappings.remove(selectedId);
                        } else {
                            GuiBaritoneBlockMapEditor.this.workingMappings.put(selectedId, parsedTargets);
                        }
                        List<String> keys = getSourceKeys();
                        GuiBaritoneBlockMapEditor.this.selectedIndex = keys.isEmpty() ? -1 : Math.max(0, keys.indexOf(selectedId));
                        clampListScroll();
                        updateInputPreview();
                    }
                }));
    }

    private void drawList(int mouseX, int mouseY) {
        drawRect(this.listX, this.listTop - 4, this.listX + this.listWidth, this.listBottom + 4, 0x44101822);
        drawRect(this.listX, this.listTop - 4, this.listX + this.listWidth, this.listTop - 3, 0xFF5FB8FF);
        drawRect(this.listX, this.listBottom + 3, this.listX + this.listWidth, this.listBottom + 4, 0xFF35536C);

        List<String> keys = getSourceKeys();
        int visibleRows = getVisibleRowCount();
        int rowX = this.listX + 2;
        int rowWidth = this.listWidth - 12;

        if (keys.isEmpty()) {
            drawCenteredString(this.fontRenderer, "映射为空，输入源方块后点击 新增条目", this.listX + this.listWidth / 2,
                    this.listTop + (this.listBottom - this.listTop) / 2 - 4, GuiTheme.SUB_TEXT);
            return;
        }

        clampListScroll();
        for (int i = 0; i < visibleRows; i++) {
            int index = this.listScroll + i;
            if (index >= keys.size()) {
                break;
            }

            int rowY = this.listTop + i * LIST_ROW_HEIGHT;
            boolean selected = index == this.selectedIndex;
            boolean hovered = mouseX >= rowX && mouseX <= rowX + rowWidth
                    && mouseY >= rowY && mouseY <= rowY + LIST_ROW_HEIGHT - 2;

            GuiTheme.drawButtonFrameSafe(rowX, rowY, rowWidth, LIST_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String sourceId = keys.get(index);
            List<String> targets = this.workingMappings.get(sourceId);
            drawString(this.fontRenderer, trimToWidth(BaritoneBlockSettingEditorSupport.getDisplayName(sourceId), rowWidth - 56),
                    rowX + 26, rowY + 3, 0xFFEAF6FF);
            drawString(this.fontRenderer, "§8" + trimToWidth(sourceId, rowWidth - 56), rowX + 26, rowY + 13, 0xFF9FB2C8);
            drawString(this.fontRenderer, "§7" + trimToWidth(buildTargetsSummary(targets, rowWidth - 56), rowWidth - 56),
                    rowX + 26, rowY + 23, 0xFFB7CCDD);
            drawString(this.fontRenderer, "§7" + (index + 1), rowX + 6, rowY + 12, 0xFF91A7BC);
        }

        if (keys.size() > visibleRows) {
            int trackHeight = this.listBottom - this.listTop;
            int maxScroll = getMaxListScroll();
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, keys.size())) * trackHeight));
            int track = Math.max(1, trackHeight - thumbHeight);
            int thumbY = this.listTop + (int) ((this.listScroll / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(this.listX + this.listWidth - 8, this.listTop, 4, trackHeight, thumbY, thumbHeight);
        }
    }

    private void drawStatusLine() {
        String input = this.sourceBlockField == null ? "" : this.sourceBlockField.getText();
        String selectedId = getSelectedSourceId();
        List<String> selectedTargets = selectedId == null ? null : this.workingMappings.get(selectedId);

        String statusText;
        int statusColor;
        if (input != null && !input.trim().isEmpty()) {
            if (this.sourceBlockToAdd == null) {
                statusText = "§c未找到这个方块，请检查中文名称或方块 ID。";
                statusColor = 0xFFFF8A8A;
            } else if (this.workingMappings.containsKey(this.sourceBlockIdToAdd)) {
                statusText = "§e该源方块已存在，可直接选中后编辑替代列表。";
                statusColor = 0xFFFFE08A;
            } else {
                statusText = "§a将新增条目: §f" + BaritoneBlockSettingEditorSupport.getDisplayName(this.sourceBlockIdToAdd);
                statusColor = 0xFF9CFFB2;
            }
        } else if (selectedId != null) {
            int targetCount = selectedTargets == null ? 0 : selectedTargets.size();
            statusText = "§7当前选中 §f" + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId)
                    + " §7，替代方块数: §f" + targetCount;
            statusColor = GuiTheme.SUB_TEXT;
        } else {
            statusText = "§7当前共配置 §f" + this.workingMappings.size() + " §7组方块映射。";
            statusColor = GuiTheme.SUB_TEXT;
        }

        drawString(this.fontRenderer, trimToWidth(statusText, this.panelWidth - 24),
                this.panelX + 12, this.panelY + this.panelHeight - 20, statusColor);
    }

    private void drawTooltips(int mouseX, int mouseY) {
        if (isMouseOverField(mouseX, mouseY, this.sourceBlockField)) {
            drawHoveringText(Arrays.asList(
                    "§e源方块输入",
                    "§7支持中文名称、原版或模组方块 ID。",
                    "§7示例: §fminecraft:stone"), mouseX, mouseY);
            return;
        }

        String selectedId = getSelectedSourceId();
        if (isMouseOverButton(mouseX, mouseY, this.editButton) && this.editButton.enabled && selectedId != null) {
            drawHoveringText(Arrays.asList(
                    "§e编辑替代方块列表",
                    "§7当前选中: §f" + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId),
                    "§8" + selectedId), mouseX, mouseY);
            return;
        }

        int hoverIndex = getListIndexAt(mouseX, mouseY);
        List<String> keys = getSourceKeys();
        if (hoverIndex >= 0 && hoverIndex < keys.size()) {
            String sourceId = keys.get(hoverIndex);
            List<String> targets = this.workingMappings.get(sourceId);
            List<String> tooltip = new ArrayList<String>();
            tooltip.add("§e" + BaritoneBlockSettingEditorSupport.getDisplayName(sourceId));
            tooltip.add("§8" + sourceId);
            tooltip.add("§7替代方块数: §f" + (targets == null ? 0 : targets.size()));
            tooltip.add("§7双击可快速打开替代列表");
            drawHoveringText(tooltip, mouseX, mouseY);
        }
    }

    private void drawBlockPreview(int x, int y) {
        drawRect(x - 2, y - 2, x + 18, y + 18, 0xFF31475D);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF16212D);
        ItemStack stack = BaritoneBlockSettingEditorSupport.getBlockStack(this.sourceBlockToAdd);
        if (stack.isEmpty()) {
            drawString(this.fontRenderer, "?", x + 6, y + 5, 0xFF86A4C0);
            return;
        }

        try {
            GuiGraphics graphics = new GuiGraphics(this.mc, this.mc.renderBuffers().bufferSource());
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.mc.font, stack, x, y);
            graphics.flush();
            return;
        } catch (Throwable ignored) {
            String symbol = BaritoneBlockSettingEditorSupport.getDisplayName(this.sourceBlockIdToAdd);
            if (symbol == null || symbol.trim().isEmpty()) {
                symbol = "#";
            } else {
                symbol = symbol.trim().substring(0, 1);
            }
            drawString(this.fontRenderer, symbol, x + 6, y + 5, 0xFFEAF6FF);
        }
    }

    private String buildTargetsSummary(List<String> blockIds, int maxWidth) {
        if (blockIds == null || blockIds.isEmpty()) {
            return "未配置替代方块";
        }
        StringBuilder summary = new StringBuilder("共 ").append(blockIds.size()).append(" 项: ");
        int appended = 0;
        for (String blockId : blockIds) {
            if (appended > 0) {
                summary.append(", ");
            }
            summary.append(BaritoneBlockSettingEditorSupport.getDisplayName(blockId));
            appended++;
            String candidate = summary.toString();
            if (this.fontRenderer.getStringWidth(candidate) > maxWidth) {
                return trimToWidth(candidate, maxWidth);
            }
            if (appended >= 3 && blockIds.size() > appended) {
                summary.append("...");
                return trimToWidth(summary.toString(), maxWidth);
            }
        }
        return trimToWidth(summary.toString(), maxWidth);
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
