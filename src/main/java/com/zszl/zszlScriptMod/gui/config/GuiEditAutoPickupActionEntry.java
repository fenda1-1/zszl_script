package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.system.AutoPickupRule;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditAutoPickupActionEntry extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final AutoPickupRule.PickupActionEntry entry;
    private final Consumer<AutoPickupRule.PickupActionEntry> onSave;

    private GuiTextField keywordField;
    private GuiTextField nbtInputField;
    private GuiTextField delayField;
    private final List<GuiTextField> fields = new ArrayList<>();
    private final List<String> nbtTags = new ArrayList<>();
    private final List<TagRemoveRegion> removeRegions = new ArrayList<>();
    private int tagScrollOffset = 0;
    private String selectedSequence = "";
    private String validationMessage = "";
    private GuiButton btnSelectSequence;
    private DraftState pendingRestoreState = null;

    public GuiEditAutoPickupActionEntry(GuiScreen parentScreen, AutoPickupRule.PickupActionEntry entry,
            Consumer<AutoPickupRule.PickupActionEntry> onSave) {
        this.parentScreen = parentScreen;
        this.entry = entry == null ? new AutoPickupRule.PickupActionEntry() : new AutoPickupRule.PickupActionEntry(entry);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        DraftState restoreState = pendingRestoreState;
        pendingRestoreState = null;
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.fields.clear();
        this.removeRegions.clear();
        this.nbtTags.clear();
        this.validationMessage = "";

        selectedSequence = entry.sequenceName == null ? "" : entry.sequenceName;

        int panelWidth = getPanelWidth();
        int panelX = getPanelX();
        int panelY = getPanelY();
        int keywordY = panelY + 48;
        int nbtY = keywordY + 50;
        int delayY = nbtY + 50;
        int sequenceY = delayY + 40;
        int buttonY = panelY + getPanelHeight() - 30;
        int addButtonWidth = Math.max(64, Math.min(90, panelWidth / 4));

        keywordField = new GuiTextField(1, fontRenderer, panelX + 10, keywordY, panelWidth - 20, 20);
        keywordField.setText(entry.keyword == null ? "" : entry.keyword);
        keywordField.setMaxStringLength(Integer.MAX_VALUE);
        fields.add(keywordField);

        nbtInputField = new GuiTextField(2, fontRenderer, panelX + 10, nbtY, panelWidth - addButtonWidth - 20, 20);
        nbtInputField.setMaxStringLength(Integer.MAX_VALUE);
        fields.add(nbtInputField);

        delayField = new GuiTextField(9, fontRenderer, panelX + 10, delayY, Math.max(100, panelWidth / 2), 20);
        delayField.setMaxStringLength(6);
        delayField.setText(String.valueOf(Math.max(0, entry.executeDelaySeconds)));
        fields.add(delayField);

        this.buttonList.add(new ThemedButton(3, panelX + panelWidth - addButtonWidth - 10, nbtY, addButtonWidth, 20,
                "添加NBT"));
        btnSelectSequence = new ThemedButton(4, panelX + 10, sequenceY, panelWidth - 20, 20, "");
        this.buttonList.add(btnSelectSequence);

        int half = (panelWidth - 30) / 2;
        this.buttonList.add(new ThemedButton(10, panelX + 10, buttonY, half, 20, "§a保存"));
        this.buttonList.add(new ThemedButton(11, panelX + 20 + half, buttonY, half, 20, "取消"));

        if (entry.requiredNbtTags != null) {
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String tag : entry.requiredNbtTags) {
                String normalized = normalizeToken(tag);
                if (!normalized.isEmpty()) {
                    unique.add(normalized);
                }
            }
            this.nbtTags.addAll(unique);
        }
        refreshSequenceButton();
        if (restoreState != null) {
            applyDraftState(restoreState);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 3) {
            addCurrentNbtTag();
            return;
        }
        if (button.id == 4) {
            DraftState draftState = captureDraftState();
            mc.setScreen(new GuiSequenceSelector(this, seq -> {
                draftState.selectedSequence = seq == null ? "" : seq;
                draftState.validationMessage = "";
                pendingRestoreState = draftState;
                mc.setScreen(this);
            }));
            return;
        }
        if (button.id == 10) {
            syncEntry();
            if (entry.sequenceName == null || entry.sequenceName.trim().isEmpty()) {
                validationMessage = "请选择执行序列";
                return;
            }
            validationMessage = "";
            if (onSave != null) {
                onSave.accept(new AutoPickupRule.PickupActionEntry(entry));
            }
            if (mc.screen == this) {
                mc.setScreen(parentScreen);
            }
            return;
        }
        if (button.id == 11) {
            mc.setScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        int panelX = getPanelX();
        int panelY = getPanelY();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "编辑拾取执行卡片", this.fontRenderer);

        drawString(fontRenderer, "物品关键字", keywordField.x, keywordField.y - 10, GuiTheme.SUB_TEXT);
        drawThemedTextField(keywordField);
        drawWrappedText(keywordField.x, keywordField.y + 24, panelWidth - 20, 0xFF8FA3B8,
                "可留空；关键字与 NBT 都留空时，表示任意拾取。");

        drawString(fontRenderer, "NBT 标签", nbtInputField.x, nbtInputField.y - 10, GuiTheme.SUB_TEXT);
        drawThemedTextField(nbtInputField);
        drawWrappedText(nbtInputField.x, nbtInputField.y + 24, panelWidth - 20, 0xFF8FA3B8,
                "匹配物品提示、附魔描述、NBT 文本和物品 ID。");

        drawString(fontRenderer, "拾取后延迟执行(秒)", delayField.x, delayField.y - 10, GuiTheme.SUB_TEXT);
        drawThemedTextField(delayField);
        drawWrappedText(delayField.x, delayField.y + 24, panelWidth - 20, 0xFF8FA3B8,
                "延迟期间仍会继续拾取白名单中的物品；0 表示立刻执行。");

        drawString(fontRenderer, "执行序列", btnSelectSequence.x, btnSelectSequence.y - 10, GuiTheme.SUB_TEXT);
        if (!validationMessage.isEmpty()) {
            drawString(fontRenderer, "§c" + validationMessage, panelX + 10, panelY + panelHeight - 44, 0xFFFF8E8E);
        }
        drawTagList(panelX + 10, getTagListY(), panelWidth - 20, getTagListHeight(), mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : fields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton != 0) {
            return;
        }
        for (TagRemoveRegion region : removeRegions) {
            if (region.contains(mouseX, mouseY) && region.index >= 0 && region.index < nbtTags.size()) {
                nbtTags.remove(region.index);
                tagScrollOffset = Math.max(0, Math.min(tagScrollOffset, getMaxTagScroll()));
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.setScreen(parentScreen);
            return;
        }
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && nbtInputField.isFocused()) {
            addCurrentNbtTag();
            return;
        }
        for (GuiTextField field : fields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0 || nbtTags.isEmpty()) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        int listX = getPanelX() + 10;
        int listY = getTagListY();
        int listW = getPanelWidth() - 20;
        int listH = getTagListHeight();
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            return;
        }

        if (dWheel > 0) {
            tagScrollOffset = Math.max(0, tagScrollOffset - 1);
        } else {
            tagScrollOffset = Math.min(getMaxTagScroll(), tagScrollOffset + 1);
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
        super.onGuiClosed();
    }

    private void refreshSequenceButton() {
        if (btnSelectSequence != null) {
            btnSelectSequence.displayString = selectedSequence == null || selectedSequence.trim().isEmpty()
                    ? "点击选择序列"
                    : "§f" + selectedSequence;
        }
    }

    private void addCurrentNbtTag() {
        String normalized = normalizeToken(nbtInputField.getText());
        if (normalized.isEmpty()) {
            return;
        }
        for (String existing : nbtTags) {
            if (existing.equalsIgnoreCase(normalized)) {
                nbtInputField.setText("");
                return;
            }
        }
        nbtTags.add(normalized);
        nbtInputField.setText("");
        tagScrollOffset = getMaxTagScroll();
    }

    private void drawWrappedText(int x, int y, int maxWidth, int color, String text) {
        if (fontRenderer == null || text == null || text.isEmpty() || maxWidth <= 0) {
            return;
        }
        List<String> lines = fontRenderer.listFormattedStringToWidth(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            drawString(fontRenderer, lines.get(i), x, y + i * 10, color);
        }
    }

    private void syncEntry() {
        entry.keyword = normalizeToken(keywordField.getText());
        entry.requiredNbtTags.clear();
        entry.requiredNbtTags.addAll(nbtTags);
        entry.sequenceName = selectedSequence == null ? "" : selectedSequence.trim();
        entry.executeDelaySeconds = Math.max(0, parseInt(delayField == null ? "" : delayField.getText(), entry.executeDelaySeconds));
    }

    private DraftState captureDraftState() {
        DraftState state = new DraftState();
        state.keyword = keywordField == null ? (entry.keyword == null ? "" : entry.keyword) : keywordField.getText();
        state.nbtInput = nbtInputField == null ? "" : nbtInputField.getText();
        state.delay = delayField == null ? String.valueOf(Math.max(0, entry.executeDelaySeconds)) : delayField.getText();
        state.nbtTags.addAll(nbtTags);
        state.selectedSequence = selectedSequence == null ? "" : selectedSequence;
        state.validationMessage = validationMessage == null ? "" : validationMessage;
        state.tagScrollOffset = tagScrollOffset;
        return state;
    }

    private void applyDraftState(DraftState state) {
        if (state == null) {
            return;
        }
        if (keywordField != null) {
            keywordField.setText(state.keyword == null ? "" : state.keyword);
        }
        if (nbtInputField != null) {
            nbtInputField.setText(state.nbtInput == null ? "" : state.nbtInput);
        }
        if (delayField != null) {
            delayField.setText(state.delay == null ? "0" : state.delay);
        }
        nbtTags.clear();
        nbtTags.addAll(state.nbtTags);
        selectedSequence = state.selectedSequence == null ? "" : state.selectedSequence;
        validationMessage = state.validationMessage == null ? "" : state.validationMessage;
        tagScrollOffset = Math.max(0, Math.min(state.tagScrollOffset, getMaxTagScroll()));
        refreshSequenceButton();
    }

    private void drawTagList(int x, int y, int width, int height, int mouseX, int mouseY) {
        removeRegions.clear();

        drawRect(x, y, x + width, y + height, 0x5520222A);
        drawHorizontalLine(x, x + width, y, 0xFF4B4B4B);
        drawHorizontalLine(x, x + width, y + height, 0xFF4B4B4B);
        drawVerticalLine(x, y, y + height, 0xFF4B4B4B);
        drawVerticalLine(x + width, y, y + height, 0xFF4B4B4B);

        if (nbtTags.isEmpty()) {
            drawString(fontRenderer, "§7暂无 NBT 标签，留空时只按物品关键字匹配", x + 6, y + 8, 0xFF9FB0C4);
            return;
        }

        int rowHeight = 18;
        int visibleRows = Math.max(1, (height - 8) / rowHeight);
        tagScrollOffset = Math.max(0, Math.min(tagScrollOffset, getMaxTagScroll()));
        for (int i = 0; i < visibleRows; i++) {
            int tagIndex = tagScrollOffset + i;
            if (tagIndex >= nbtTags.size()) {
                break;
            }
            int rowY = y + 4 + i * rowHeight;
            boolean hovered = mouseX >= x + 2 && mouseX <= x + width - 10 && mouseY >= rowY
                    && mouseY <= rowY + rowHeight - 1;
            int bg = hovered ? 0xAA2E4258 : 0x88222222;
            int border = hovered ? 0xFF7EC8FF : 0xFF4B4B4B;
            drawRect(x + 2, rowY, x + width - 10, rowY + rowHeight - 1, bg);
            drawHorizontalLine(x + 2, x + width - 10, rowY, border);
            drawHorizontalLine(x + 2, x + width - 10, rowY + rowHeight - 1, border);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(nbtTags.get(tagIndex), width - 32), x + 6, rowY + 5,
                    0xFFFFFFFF);
            int removeX = x + width - 22;
            drawString(fontRenderer, "§c✕", removeX, rowY + 5, 0xFFFF8080);
            removeRegions.add(new TagRemoveRegion(removeX - 2, rowY + 1, 16, rowHeight - 2, tagIndex));
        }

        if (getMaxTagScroll() > 0) {
            int thumbHeight = Math.max(14, (int) ((visibleRows / (float) nbtTags.size()) * (height - 8)));
            int trackHeight = Math.max(1, (height - 8) - thumbHeight);
            int thumbY = y + 4 + (int) ((tagScrollOffset / (float) getMaxTagScroll()) * trackHeight);
            GuiTheme.drawScrollbar(x + width - 7, y + 4, 4, height - 8, thumbY, thumbHeight);
        }
    }

    private int getMaxTagScroll() {
        return Math.max(0, nbtTags.size() - getVisibleTagRows());
    }

    private int getVisibleTagRows() {
        return Math.max(1, (getTagListHeight() - 8) / 18);
    }

    private int getPanelWidth() {
        return Math.max(280, Math.min(560, this.width - 20));
    }

    private int getPanelHeight() {
        return Math.max(240, Math.min(400, this.height - 20));
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getTagListY() {
        return getPanelY() + 212;
    }

    private int getTagListHeight() {
        int buttonY = getPanelY() + getPanelHeight() - 30;
        return Math.max(56, buttonY - getTagListY() - 8);
    }

    private String normalizeToken(String text) {
        return KillAuraHandler.normalizeFilterName(text);
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static final class TagRemoveRegion {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int index;

        private TagRemoveRegion(int x, int y, int width, int height, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.index = index;
        }

        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private static final class DraftState {
        private String keyword = "";
        private String nbtInput = "";
        private String delay = "0";
        private final List<String> nbtTags = new ArrayList<>();
        private String selectedSequence = "";
        private String validationMessage = "";
        private int tagScrollOffset = 0;
    }
}







