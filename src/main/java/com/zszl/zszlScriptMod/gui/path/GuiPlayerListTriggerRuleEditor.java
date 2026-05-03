package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.trigger.PlayerListTriggerSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiPlayerListTriggerRuleEditor extends ThemedGuiScreen {
    private static final int BTN_MODE = 1;
    private static final int BTN_PICK_PLAYER = 2;
    private static final int BTN_ADD_OR_UPDATE = 3;
    private static final int BTN_REMOVE = 4;
    private static final int BTN_CLEAR = 5;
    private static final int BTN_CANCEL = 6;
    private static final int BTN_DONE = 7;
    private static final int NAME_FIELD_ID = 6301;
    private static final int CARD_HEIGHT = 46;

    private final GuiScreen parentScreen;
    private final Consumer<List<PlayerListTriggerSupport.RuleEntry>> onSave;
    private final List<PlayerListTriggerSupport.RuleEntry> entries = new ArrayList<>();

    private GuiTextField nameField;
    private GuiButton modeBtn;
    private GuiButton pickPlayerBtn;
    private GuiButton addOrUpdateBtn;
    private GuiButton removeBtn;
    private GuiButton clearBtn;

    private String draftName = "";
    private String draftMode = PlayerListTriggerSupport.MODE_EXACT;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String statusMessage = "§7可手动输入名称，或从当前玩家列表回填；列表中的卡片满足任意一条即触发。";

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int nameFieldX;
    private int nameFieldY;
    private int nameFieldW;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public GuiPlayerListTriggerRuleEditor(GuiScreen parentScreen,
            List<PlayerListTriggerSupport.RuleEntry> currentEntries,
            Consumer<List<PlayerListTriggerSupport.RuleEntry>> onSave) {
        this.parentScreen = parentScreen;
        this.onSave = onSave;
        this.entries.addAll(PlayerListTriggerSupport.copyEntries(currentEntries));
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        computeLayout();
        if (nameField == null) {
            nameField = new GuiTextField(NAME_FIELD_ID, this.fontRenderer, nameFieldX, nameFieldY, nameFieldW, 18);
            nameField.setMaxStringLength(128);
        }
        nameField.x = nameFieldX;
        nameField.y = nameFieldY;
        nameField.width = nameFieldW;
        nameField.setText(draftName);
        nameField.setFocused(false);

        int row1Y = nameFieldY + 26;
        int row2Y = row1Y + 26;
        int gap = 6;
        int halfW = (nameFieldW - gap) / 2;
        int thirdW = (nameFieldW - gap * 2) / 3;

        modeBtn = new ThemedButton(BTN_MODE, nameFieldX, row1Y, halfW, 20, "");
        pickPlayerBtn = new ThemedButton(BTN_PICK_PLAYER, nameFieldX + halfW + gap, row1Y,
                Math.max(1, nameFieldW - halfW - gap), 20, "从玩家列表选择");
        addOrUpdateBtn = new ThemedButton(BTN_ADD_OR_UPDATE, nameFieldX, row2Y, thirdW, 20, "");
        removeBtn = new ThemedButton(BTN_REMOVE, nameFieldX + thirdW + gap, row2Y, thirdW, 20, "删除选中");
        clearBtn = new ThemedButton(BTN_CLEAR, nameFieldX + (thirdW + gap) * 2, row2Y,
                Math.max(1, nameFieldW - thirdW * 2 - gap * 2), 20, "清空卡片");

        buttonList.add(modeBtn);
        buttonList.add(pickPlayerBtn);
        buttonList.add(addOrUpdateBtn);
        buttonList.add(removeBtn);
        buttonList.add(clearBtn);
        buttonList.add(new ThemedButton(BTN_CANCEL, panelX + panelW / 2 - 126, panelY + panelH - 30, 120, 20, "取消"));
        buttonList.add(new ThemedButton(BTN_DONE, panelX + panelW / 2 + 6, panelY + panelH - 30, 120, 20, "完成"));
        refreshButtons();
    }

    private void computeLayout() {
        panelW = Math.min(700, this.width - 24);
        panelH = Math.min(500, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        nameFieldX = panelX + 16;
        nameFieldY = panelY + 38;
        nameFieldW = panelW - 32;
        listX = panelX + 16;
        listY = nameFieldY + 88;
        listW = panelW - 32;
        listH = Math.max(120, panelY + panelH - 60 - listY);
    }

    private void refreshButtons() {
        if (modeBtn != null) {
            modeBtn.displayString = "匹配模式: " + displayMode(draftMode);
        }
        if (addOrUpdateBtn != null) {
            addOrUpdateBtn.displayString = selectedIndex >= 0 ? "更新卡片" : "新增卡片";
        }
        if (removeBtn != null) {
            removeBtn.enabled = selectedIndex >= 0 && selectedIndex < entries.size();
        }
        if (clearBtn != null) {
            clearBtn.enabled = !entries.isEmpty();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) {
            return;
        }
        switch (button.id) {
            case BTN_MODE:
                draftMode = PlayerListTriggerSupport.MODE_CONTAINS.equals(draftMode)
                        ? PlayerListTriggerSupport.MODE_EXACT
                        : PlayerListTriggerSupport.MODE_CONTAINS;
                refreshButtons();
                break;
            case BTN_PICK_PLAYER:
                syncDraftFromField();
                mc.setScreen(new GuiOnlinePlayerSelector(this, selectedName -> {
                    setDraftName(selectedName);
                    statusMessage = "§a已回填玩家名，可直接新增卡片或修改匹配模式。";
                }));
                return;
            case BTN_ADD_OR_UPDATE:
                addOrUpdateEntry();
                break;
            case BTN_REMOVE:
                removeSelectedEntry();
                break;
            case BTN_CLEAR:
                entries.clear();
                clearDraft();
                statusMessage = "§e已清空所有玩家名称卡片。";
                refreshButtons();
                break;
            case BTN_CANCEL:
                mc.setScreen(parentScreen);
                return;
            case BTN_DONE:
                if (onSave != null) {
                    onSave.accept(PlayerListTriggerSupport.copyEntries(entries));
                }
                mc.setScreen(parentScreen);
                return;
            default:
                break;
        }
    }

    private void addOrUpdateEntry() {
        syncDraftFromField();
        String normalizedName = PlayerListTriggerSupport.normalizeName(draftName);
        if (normalizedName.isEmpty()) {
            statusMessage = "§c请先输入玩家名称，或从当前玩家列表选择一个名称。";
            return;
        }
        PlayerListTriggerSupport.RuleEntry updated = new PlayerListTriggerSupport.RuleEntry(normalizedName, draftMode);
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            entries.set(selectedIndex, updated);
            statusMessage = "§a已更新玩家名称卡片。";
        } else {
            entries.add(updated);
            statusMessage = "§a已新增玩家名称卡片。";
        }
        normalizeEntriesAndSelect(updated.name, updated.mode);
        refreshButtons();
    }

    private void removeSelectedEntry() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) {
            statusMessage = "§e请先点击一张卡片，再执行删除。";
            return;
        }
        entries.remove(selectedIndex);
        clearDraft();
        statusMessage = "§e已删除选中的玩家名称卡片。";
        refreshButtons();
    }

    private void normalizeEntriesAndSelect(String name, String mode) {
        List<PlayerListTriggerSupport.RuleEntry> normalized = PlayerListTriggerSupport.copyEntries(entries);
        entries.clear();
        entries.addAll(normalized);
        String targetName = PlayerListTriggerSupport.normalizeName(name);
        String targetMode = PlayerListTriggerSupport.normalizeMode(mode);
        selectedIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            PlayerListTriggerSupport.RuleEntry entry = entries.get(i);
            if (entry.name.equalsIgnoreCase(targetName)
                    && PlayerListTriggerSupport.normalizeMode(entry.mode).equals(targetMode)) {
                selectedIndex = i;
                break;
            }
        }
        draftName = targetName;
        draftMode = targetMode;
        if (nameField != null) {
            nameField.setText(draftName);
        }
        ensureSelectionVisible();
    }

    private void ensureSelectionVisible() {
        if (selectedIndex < 0) {
            return;
        }
        int visible = Math.max(1, (listH - 8) / CARD_HEIGHT);
        maxScroll = Math.max(0, entries.size() - visible);
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visible) {
            scrollOffset = Math.max(0, selectedIndex - visible + 1);
        }
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private void loadEntryToDraft(int index) {
        if (index < 0 || index >= entries.size()) {
            return;
        }
        PlayerListTriggerSupport.RuleEntry entry = entries.get(index);
        selectedIndex = index;
        draftName = entry.name;
        draftMode = entry.mode;
        if (nameField != null) {
            nameField.setText(draftName);
            nameField.setFocused(false);
        }
        statusMessage = "§7已载入卡片，可直接修改名称或匹配模式，再点“更新卡片”。";
        refreshButtons();
    }

    private void clearDraft() {
        selectedIndex = -1;
        draftName = "";
        draftMode = PlayerListTriggerSupport.MODE_EXACT;
        if (nameField != null) {
            nameField.setText("");
            nameField.setFocused(false);
        }
        refreshButtons();
    }

    private void setDraftName(String value) {
        draftName = PlayerListTriggerSupport.normalizeName(value);
        if (nameField != null) {
            nameField.setText(draftName);
        }
    }

    private void syncDraftFromField() {
        if (nameField != null) {
            draftName = nameField.getText();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "编辑玩家列表触发规则", this.fontRenderer);

        drawString(fontRenderer, "§f玩家名称", panelX + 16, panelY + 25, 0xFFFFFFFF);
        drawThemedTextField(nameField);
        if (PlayerListTriggerSupport.normalizeName(nameField.getText()).isEmpty() && !nameField.isFocused()) {
            drawString(fontRenderer, "§7输入名称，或从当前玩家列表选择...", nameFieldX + 4, nameFieldY + 6, 0xFF7D8C9C);
        }
        drawString(fontRenderer, "§7点击卡片可编辑；多个卡片是“或”关系，任意命中即触发。", panelX + 16, panelY + 118, 0xFFB8C7D9);

        drawCardList(mouseX, mouseY);
        drawString(fontRenderer, trim(statusMessage, panelW - 32), panelX + 16, panelY + panelH - 42, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCardList(int mouseX, int mouseY) {
        GuiTheme.drawInputFrameSafe(listX, listY, listW, listH, false, true);
        int visible = Math.max(1, (listH - 8) / CARD_HEIGHT);
        maxScroll = Math.max(0, entries.size() - visible);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        if (entries.isEmpty()) {
            GuiTheme.drawEmptyState(panelX + panelW / 2, listY + listH / 2 - 6, "还没有任何玩家名称卡片。", this.fontRenderer);
            return;
        }

        int rowY = listY + 4;
        for (int i = 0; i < visible; i++) {
            int actualIndex = scrollOffset + i;
            if (actualIndex >= entries.size()) {
                break;
            }
            PlayerListTriggerSupport.RuleEntry entry = entries.get(actualIndex);
            boolean hovered = isHoverRegion(mouseX, mouseY, listX + 4, rowY, listW - 12, CARD_HEIGHT - 4);
            GuiTheme.UiState state = actualIndex == selectedIndex
                    ? GuiTheme.UiState.SELECTED
                    : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrameSafe(listX + 4, rowY, listW - 12, CARD_HEIGHT - 4, state);
            drawString(fontRenderer, "§f" + trim(entry.name, listW - 28), listX + 12, rowY + 7, 0xFFFFFFFF);
            drawString(fontRenderer, "§7匹配方式: §f" + displayMode(entry.mode), listX + 12, rowY + 22, 0xFFB8C7D9);
            rowY += CARD_HEIGHT;
        }

        if (entries.size() > visible) {
            int barH = listH - 8;
            int thumbH = Math.max(18, (int) ((visible / (float) Math.max(visible, entries.size())) * barH));
            int track = Math.max(1, barH - thumbH);
            int thumbY = listY + 4 + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(listX + listW - 6, listY + 4, 3, barH, thumbY, thumbH);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (nameField != null) {
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton != 0) {
            return;
        }
        int visible = Math.max(1, (listH - 8) / CARD_HEIGHT);
        int rowY = listY + 4;
        for (int i = 0; i < visible; i++) {
            int actualIndex = scrollOffset + i;
            if (actualIndex >= entries.size()) {
                break;
            }
            if (isHoverRegion(mouseX, mouseY, listX + 4, rowY, listW - 12, CARD_HEIGHT - 4)) {
                loadEntryToDraft(actualIndex);
                return;
            }
            rowY += CARD_HEIGHT;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            addOrUpdateEntry();
            return;
        }
        if (nameField != null && nameField.textboxKeyTyped(typedChar, keyCode)) {
            draftName = nameField.getText();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (nameField != null) {
            nameField.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (!isHoverRegion(mouseX, mouseY, listX, listY, listW, listH)) {
            return;
        }
        int visible = Math.max(1, (listH - 8) / CARD_HEIGHT);
        maxScroll = Math.max(0, entries.size() - visible);
        scrollOffset = clamp(scrollOffset + (dWheel < 0 ? 1 : -1), 0, maxScroll);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private String displayMode(String mode) {
        return PlayerListTriggerSupport.MODE_CONTAINS.equals(PlayerListTriggerSupport.normalizeMode(mode))
                ? "包含"
                : "全匹配";
    }

    private String trim(String text, int width) {
        String safeText = text == null ? "" : text;
        if (fontRenderer == null || fontRenderer.getStringWidth(safeText) <= width) {
            return safeText;
        }
        return fontRenderer.trimStringToWidth(safeText, Math.max(0, width - fontRenderer.getStringWidth("..."))) + "...";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
