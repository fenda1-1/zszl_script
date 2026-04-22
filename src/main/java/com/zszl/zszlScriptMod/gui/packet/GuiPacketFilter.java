// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/packet/GuiPacketFilter.java
package com.zszl.zszlScriptMod.gui.packet;

import java.io.IOException;

import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;

import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;

public class GuiPacketFilter extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private GuiTextField whitelistField;
    private GuiTextField blacklistField;
    private GuiTextField maxCapturedField;

    public GuiPacketFilter(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int panelWidth = 320;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = this.height / 2 - 100;
        int currentY = panelY + 20;

        whitelistField = new GuiTextField(0, fontRenderer, panelX, currentY + 15, panelWidth, 20);
        whitelistField.setMaxStringLength(Integer.MAX_VALUE);
        whitelistField.setText(String.join(", ", PacketFilterConfig.INSTANCE.whitelistFilters));
        currentY += 50;

        blacklistField = new GuiTextField(1, fontRenderer, panelX, currentY + 15, panelWidth, 20);
        blacklistField.setMaxStringLength(Integer.MAX_VALUE);
        blacklistField.setText(String.join(", ", PacketFilterConfig.INSTANCE.blacklistFilters));
        currentY += 50;

        maxCapturedField = new GuiTextField(2, fontRenderer, panelX, currentY + 15, panelWidth, 20);
        maxCapturedField.setMaxStringLength(6);
        maxCapturedField.setText(String.valueOf(PacketFilterConfig.INSTANCE.maxCapturedPackets));
        currentY += 60;

        this.buttonList.add(new GuiButton(100, panelX, currentY, (panelWidth - 10) / 2, 20,
                "§a" + I18n.format("gui.common.save")));
        this.buttonList
                .add(new GuiButton(101, panelX + (panelWidth + 10) / 2, currentY, (panelWidth - 10) / 2, 20,
                        I18n.format("gui.common.cancel")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) { // save
            PacketFilterConfig config = PacketFilterConfig.INSTANCE;

            config.whitelistFilters.clear();
            String[] whitelistKeywords = whitelistField.getText().split(",");
            for (String keyword : whitelistKeywords) {
                if (!keyword.trim().isEmpty()) {
                    config.whitelistFilters.add(keyword.trim());
                }
            }

            config.blacklistFilters.clear();
            String[] blacklistKeywords = blacklistField.getText().split(",");
            for (String keyword : blacklistKeywords) {
                if (!keyword.trim().isEmpty()) {
                    config.blacklistFilters.add(keyword.trim());
                }
            }

            int maxCaptured = 3000;
            try {
                maxCaptured = Integer.parseInt(maxCapturedField.getText().trim());
            } catch (NumberFormatException ignored) {
            }
            config.maxCapturedPackets = MathHelper.clamp(maxCaptured, 100, 50000);

            PacketFilterConfig.save();

            mc.setScreen(parentScreen);
        } else if (button.id == 101) { // cancel
            mc.setScreen(parentScreen);
        }
    }

    // keyTyped, mouseClicked, drawScreen
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (whitelistField.textboxKeyTyped(typedChar, keyCode)
                || blacklistField.textboxKeyTyped(typedChar, keyCode)
                || maxCapturedField.textboxKeyTyped(typedChar, keyCode)) {
            // Handled
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        whitelistField.mouseClicked(mouseX, mouseY, mouseButton);
        blacklistField.mouseClicked(mouseX, mouseY, mouseButton);
        maxCapturedField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRenderer, I18n.format("gui.packet.filter.title"), this.width / 2, 20, 0xFFFFFF);

        int panelX = (this.width - 320) / 2;

        drawString(fontRenderer, I18n.format("gui.packet.filter.whitelist_label"), panelX, whitelistField.y - 12,
                0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.filter.blacklist_label"), panelX, blacklistField.y - 12,
                0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.packet.filter.max_captured_label"), panelX, maxCapturedField.y - 12,
                0xFFFFFF);

        drawThemedTextField(whitelistField);
        drawThemedTextField(blacklistField);
        drawThemedTextField(maxCapturedField);

        drawString(fontRenderer, I18n.format("gui.packet.filter.hint"), panelX, maxCapturedField.y + 26, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawScreenTooltip(mouseX, mouseY);
    }

    private void drawScreenTooltip(int mouseX, int mouseY) {
        String tooltip = null;
        if (isMouseOverField(mouseX, mouseY, whitelistField)) {
            tooltip = "白名单关键字，多个用逗号分隔。\n启用白名单模式时，只有命中的包才会进入捕获列表。";
        } else if (isMouseOverField(mouseX, mouseY, blacklistField)) {
            tooltip = "黑名单关键字，多个用逗号分隔。\n命中的包会被排除，不写则不过滤。";
        } else if (isMouseOverField(mouseX, mouseY, maxCapturedField)) {
            tooltip = "允许在内存里保留的最大抓包数量。\n越大越方便回溯，但也会占用更多内存。";
        } else {
            for (GuiButton button : this.buttonList) {
                if (!isMouseOverButton(mouseX, mouseY, button)) {
                    continue;
                }
                if (button.id == 100) {
                    tooltip = "保存当前过滤设置并立即生效。";
                } else if (button.id == 101) {
                    tooltip = "放弃本次修改并返回上一页。";
                }
                break;
            }
        }
        drawSimpleTooltip(tooltip, mouseX, mouseY);
    }
}







