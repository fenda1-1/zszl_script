package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.trigger.PlayerListTriggerSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GuiOnlinePlayerSelector extends ThemedGuiScreen {
    private static final int BTN_CANCEL = 1;
    private static final int SEARCH_FIELD_ID = 6201;
    private static final int ROW_HEIGHT = 24;

    private final GuiScreen parentScreen;
    private final Consumer<String> onSelect;
    private final List<PlayerListTriggerSupport.PlayerRecord> players = new ArrayList<>();

    private GuiTextField searchField;
    private String searchQuery = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int searchX;
    private int searchY;
    private int searchW;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public GuiOnlinePlayerSelector(GuiScreen parentScreen, Consumer<String> onSelect) {
        this.parentScreen = parentScreen;
        this.onSelect = onSelect;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        computeLayout();
        reloadPlayers();
        if (searchField == null) {
            searchField = new GuiTextField(SEARCH_FIELD_ID, this.fontRenderer, searchX, searchY, searchW, 18);
            searchField.setMaxStringLength(128);
        }
        searchField.x = searchX;
        searchField.y = searchY;
        searchField.width = searchW;
        searchField.setText(searchQuery);
        searchField.setFocused(false);
        buttonList.add(new ThemedButton(BTN_CANCEL, panelX + (panelW - 120) / 2, panelY + panelH - 30, 120, 20, "返回"));
    }

    private void computeLayout() {
        panelW = Math.min(430, this.width - 24);
        panelH = Math.min(360, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        searchX = panelX + 14;
        searchY = panelY + 32;
        searchW = panelW - 28;
        listX = panelX + 14;
        listY = searchY + 28;
        listW = panelW - 28;
        listH = panelH - 102;
    }

    private void reloadPlayers() {
        players.clear();
        players.addAll(PlayerListTriggerSupport.captureSnapshot(Minecraft.getMinecraft()).players);
        scrollOffset = 0;
        maxScroll = 0;
    }

    private List<PlayerListTriggerSupport.PlayerRecord> filteredPlayers() {
        List<PlayerListTriggerSupport.PlayerRecord> filtered = new ArrayList<>();
        String filter = normalize(searchQuery).toLowerCase(Locale.ROOT);
        for (PlayerListTriggerSupport.PlayerRecord player : players) {
            if (player == null) {
                continue;
            }
            if (filter.isEmpty()) {
                filtered.add(player);
                continue;
            }
            String text = (player.getLabel() + " " + player.profileName + " " + player.displayName)
                    .toLowerCase(Locale.ROOT);
            if (text.contains(filter)) {
                filtered.add(player);
            }
        }
        return filtered;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CANCEL) {
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "选择玩家列表名称", this.fontRenderer);

        drawString(fontRenderer, "§7点击下方任意玩家即可回填到上一个编辑框。", panelX + 14, panelY + 21, 0xFFB8C7D9);
        drawThemedTextField(searchField);
        if (normalize(searchField.getText()).isEmpty() && !searchField.isFocused()) {
            drawString(fontRenderer, "§7搜索当前玩家...", searchX + 4, searchY + 6, 0xFF7D8C9C);
        }

        GuiTheme.drawInputFrameSafe(listX, listY, listW, listH, false, true);
        List<PlayerListTriggerSupport.PlayerRecord> filtered = filteredPlayers();
        int visible = Math.max(1, (listH - 8) / ROW_HEIGHT);
        maxScroll = Math.max(0, filtered.size() - visible);
        scrollOffset = clamp(scrollOffset, 0, maxScroll);

        if (filtered.isEmpty()) {
            GuiTheme.drawEmptyState(panelX + panelW / 2, listY + listH / 2 - 6, "当前玩家列表为空，可返回手动输入。", this.fontRenderer);
        } else {
            int rowY = listY + 4;
            for (int i = 0; i < visible; i++) {
                int actualIndex = scrollOffset + i;
                if (actualIndex >= filtered.size()) {
                    break;
                }
                PlayerListTriggerSupport.PlayerRecord player = filtered.get(actualIndex);
                boolean hovered = isHoverRegion(mouseX, mouseY, listX + 4, rowY, listW - 12, ROW_HEIGHT - 2);
                GuiTheme.drawButtonFrameSafe(listX + 4, rowY, listW - 12, ROW_HEIGHT - 2,
                        hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawString(fontRenderer, trim(player.getLabel(), listW - 24), listX + 10, rowY + 8, 0xFFFFFFFF);
                rowY += ROW_HEIGHT;
            }
        }

        if (filtered.size() > visible) {
            int barH = listH - 8;
            int thumbH = Math.max(18, (int) ((visible / (float) Math.max(visible, filtered.size())) * barH));
            int track = Math.max(1, barH - thumbH);
            int thumbY = listY + 4 + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
            GuiTheme.drawScrollbar(listX + listW - 6, listY + 4, 3, barH, thumbY, thumbH);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (mouseButton != 0) {
            return;
        }
        List<PlayerListTriggerSupport.PlayerRecord> filtered = filteredPlayers();
        int visible = Math.max(1, (listH - 8) / ROW_HEIGHT);
        int rowY = listY + 4;
        for (int i = 0; i < visible; i++) {
            int actualIndex = scrollOffset + i;
            if (actualIndex >= filtered.size()) {
                break;
            }
            if (isHoverRegion(mouseX, mouseY, listX + 4, rowY, listW - 12, ROW_HEIGHT - 2)) {
                selectPlayer(filtered.get(actualIndex));
                return;
            }
            rowY += ROW_HEIGHT;
        }
    }

    private void selectPlayer(PlayerListTriggerSupport.PlayerRecord player) {
        if (player == null) {
            return;
        }
        if (onSelect != null) {
            onSelect.accept(player.getSuggestedEntryName());
        }
        mc.displayGuiScreen(parentScreen);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            searchQuery = searchField.getText();
            scrollOffset = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (!isHoverRegion(mouseX, mouseY, listX, listY, listW, listH)) {
            return;
        }
        List<PlayerListTriggerSupport.PlayerRecord> filtered = filteredPlayers();
        int visible = Math.max(1, (listH - 8) / ROW_HEIGHT);
        maxScroll = Math.max(0, filtered.size() - visible);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
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
