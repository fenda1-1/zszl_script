package com.zszl.zszlScriptMod.gui.mail;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.handlers.MailHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNo;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiYesNoCallback;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiMailIdViewer extends ThemedGuiScreen implements GuiYesNoCallback {

    private static final int BTN_DONE = 0;
    private static final int BTN_DELETE_SELECTED = 1;
    private static final int BTN_CLEAR_ALL = 2;
    private static final int CONFIRM_DELETE_SELECTED = 1001;
    private static final int CONFIRM_CLEAR_ALL = 1002;

    private final GuiScreen parentScreen;
    private final List<MailHelper.MailInfo> mailList = new ArrayList<>();

    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private final int rowHeight = 24;

    public GuiMailIdViewer(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        refreshMailList();

        int panelWidth = 360;
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        listX = panelX + 10;
        listY = panelY + 38;
        listWidth = panelWidth - 20;
        listHeight = 150;

        int buttonY = panelY + panelHeight - 28;
        this.buttonList.add(new ThemedButton(BTN_DELETE_SELECTED, panelX + 10, buttonY, 112, 20,
                I18n.format("gui.mail.viewer.delete_selected")));
        this.buttonList.add(new ThemedButton(BTN_CLEAR_ALL, panelX + 124, buttonY, 112, 20,
                I18n.format("gui.mail.viewer.clear_all")));
        this.buttonList.add(new ThemedButton(BTN_DONE, panelX + 238, buttonY, 112, 20, I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void refreshMailList() {
        this.mailList.clear();
        synchronized (MailHelper.INSTANCE.mailInfoList) {
            this.mailList.addAll(MailHelper.INSTANCE.mailInfoList);
        }
        if (selectedIndex >= mailList.size()) {
            selectedIndex = mailList.isEmpty() ? -1 : mailList.size() - 1;
        }
    }

    private void updateButtonStates() {
        GuiButton delete = this.buttonList.stream().filter(button -> button.id == BTN_DELETE_SELECTED).findFirst().orElse(null);
        GuiButton clearAll = this.buttonList.stream().filter(button -> button.id == BTN_CLEAR_ALL).findFirst().orElse(null);
        if (delete != null) {
            delete.enabled = selectedIndex >= 0 && selectedIndex < mailList.size();
        }
        if (clearAll != null) {
            clearAll.enabled = !mailList.isEmpty();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DONE) {
            mc.setScreen(parentScreen);
        } else if (button.id == BTN_DELETE_SELECTED && selectedIndex >= 0 && selectedIndex < mailList.size()) {
            mc.setScreen(new GuiYesNo((GuiYesNoCallback) this,
                    I18n.format("gui.mail.viewer.confirm_delete.title"),
                    I18n.format("gui.mail.viewer.confirm_delete.message", 1),
                    CONFIRM_DELETE_SELECTED));
        } else if (button.id == BTN_CLEAR_ALL && !mailList.isEmpty()) {
            mc.setScreen(new GuiYesNo((GuiYesNoCallback) this,
                    I18n.format("gui.mail.viewer.confirm_clear.title"),
                    I18n.format("gui.mail.viewer.confirm_clear.message", mailList.size()),
                    CONFIRM_CLEAR_ALL));
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            if (id == CONFIRM_DELETE_SELECTED && selectedIndex >= 0 && selectedIndex < mailList.size()) {
                MailHelper.INSTANCE.removeMailById(mailList.get(selectedIndex).mailId);
            } else if (id == CONFIRM_CLEAR_ALL) {
                MailHelper.INSTANCE.clearAllMails();
            }
        }
        refreshMailList();
        updateButtonStates();
        mc.setScreen(this);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        if (mouseX < listX || mouseX > listX + listWidth || mouseY < listY || mouseY > listY + listHeight) {
            return;
        }
        int clickedIndex = scrollOffset + (mouseY - listY) / rowHeight;
        if (clickedIndex >= 0 && clickedIndex < mailList.size()) {
            selectedIndex = clickedIndex;
            updateButtonStates();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visibleRows = Math.max(1, listHeight / rowHeight);
        int maxScroll = Math.max(0, mailList.size() - visibleRows);
        if (maxScroll > 0 && delta != 0.0D) {
            scrollOffset += delta > 0 ? -1 : 1;
            if (scrollOffset < 0) {
                scrollOffset = 0;
            } else if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 360;
        int panelHeight = 250;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.mail.viewer.title"), this.fontRenderer);

        drawRect(listX, listY, listX + listWidth, listY + listHeight, 0x66324458);

        int visibleRows = Math.max(1, listHeight / rowHeight);
        for (int i = 0; i < visibleRows; i++) {
            int actualIndex = scrollOffset + i;
            if (actualIndex >= mailList.size()) {
                break;
            }
            MailHelper.MailInfo info = mailList.get(actualIndex);
            int top = listY + i * rowHeight;
            if (actualIndex == selectedIndex) {
                GuiTheme.drawButtonFrame(listX + 2, top + 1, listWidth - 4, rowHeight - 2, GuiTheme.UiState.SELECTED);
            }
            drawString(this.fontRenderer,
                    (info.mailId == null ? "" : info.mailId) + "  " + (info.title == null ? "" : info.title),
                    listX + 8, top + 7, 0xFFFFFFFF);
        }

        if (mailList.isEmpty()) {
            drawCenteredString(this.fontRenderer, I18n.format("gui.mail.viewer.empty"),
                    listX + listWidth / 2, listY + listHeight / 2 - 4, 0xFFAAAAAA);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}


