package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
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

public class GuiEditAutoPickupFilterEntry extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final AutoPickupRule.ItemMatchEntry entry;
    private final Consumer<AutoPickupRule.ItemMatchEntry> onSave;

    private GuiTextField keywordField;
    private GuiTextField nbtInputField;
    private final List<GuiTextField> fields = new ArrayList<>();
    private final List<String> nbtTags = new ArrayList<>();
    private final List<TagRemoveRegion> removeRegions = new ArrayList<>();
    private int tagScrollOffset = 0;

    public GuiEditAutoPickupFilterEntry(GuiScreen parentScreen, AutoPickupRule.ItemMatchEntry entry,
            Consumer<AutoPickupRule.ItemMatchEntry> onSave) {
        this.parentScreen = parentScreen;
        this.entry = entry == null ? new AutoPickupRule.ItemMatchEntry() : new AutoPickupRule.ItemMatchEntry(entry);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.fields.clear();
        this.removeRegions.clear();
        this.nbtTags.clear();

        int panelWidth = getPanelWidth();
        int panelX = getPanelX();
        int panelY = getPanelY();
        int startY = panelY + 24;
        int keywordY = startY + 24;
        int nbtY = keywordY + 50;
        int buttonY = panelY + getPanelHeight() - 30;
        int addButtonWidth = Math.max(64, Math.min(90, panelWidth / 4));

        keywordField = new GuiTextField(1, fontRenderer, panelX + 10, keywordY, panelWidth - 20, 20);
        keywordField.setText(entry.keyword == null ? "" : entry.keyword);
        keywordField.setMaxStringLength(Integer.MAX_VALUE);
        fields.add(keywordField);

        nbtInputField = new GuiTextField(2, fontRenderer, panelX + 10, nbtY, panelWidth - addButtonWidth - 20, 20);
        nbtInputField.setMaxStringLength(Integer.MAX_VALUE);
        fields.add(nbtInputField);

        this.buttonList
                .add(new ThemedButton(3, panelX + panelWidth - addButtonWidth - 10, nbtY, addButtonWidth, 20, "添加NBT"));

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
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 3) {
            addCurrentNbtTag();
            return;
        }
        if (button.id == 10) {
            syncEntry();
            if (onSave != null) {
                onSave.accept(new AutoPickupRule.ItemMatchEntry(entry));
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
        this.drawDefaultBackground();

        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        int panelX = getPanelX();
        int panelY = getPanelY();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "编辑拾取匹配卡片", this.fontRenderer);

        drawString(fontRenderer, "物品关键字(包含即可)", keywordField.x, keywordField.y - 10, GuiTheme.SUB_TEXT);
        drawThemedTextField(keywordField);
        drawString(fontRenderer, "留空则只按 NBT 标签匹配", keywordField.x, keywordField.y + 24, 0xFF8FA3B8);

        drawString(fontRenderer, "NBT 标签", nbtInputField.x, nbtInputField.y - 10, GuiTheme.SUB_TEXT);
        drawThemedTextField(nbtInputField);
        drawString(fontRenderer, "匹配物品提示、附魔描述、NBT 文本和物品 ID", nbtInputField.x, nbtInputField.y + 24, 0xFF8FA3B8);

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
        int panelX = getPanelX();
        int panelY = getPanelY();
        int listX = panelX + 10;
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

    private void syncEntry() {
        entry.keyword = normalizeToken(keywordField.getText());
        entry.requiredNbtTags.clear();
        entry.requiredNbtTags.addAll(nbtTags);
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
        return Math.max(260, Math.min(520, this.width - 20));
    }

    private int getPanelHeight() {
        return Math.max(220, Math.min(360, this.height - 20));
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getTagListHeight() {
        int buttonY = getPanelY() + getPanelHeight() - 30;
        return Math.max(56, buttonY - getTagListY() - 8);
    }

    private int getTagListY() {
        return getPanelY() + 124;
    }

    private String normalizeToken(String text) {
        return KillAuraHandler.normalizeFilterName(text);
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
}







