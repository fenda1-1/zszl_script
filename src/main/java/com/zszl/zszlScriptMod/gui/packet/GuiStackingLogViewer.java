// File path: src/main/java/com/zszl/zszlScriptMod/gui/packet/GuiStackingLogViewer.java
package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.handlers.ShulkerBoxStackingHandler.StackingLogEntry;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class GuiStackingLogViewer extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final List<StackingLogEntry> logs;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final int itemHeight = 55;

    public GuiStackingLogViewer(GuiScreen parent, List<StackingLogEntry> logs) {
        this.parentScreen = parent;
        this.logs = logs;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int buttonWidth = 120;
        int spacing = 10;
        int totalButtonWidth = buttonWidth * 2 + spacing;
        int startX = (this.width - totalButtonWidth) / 2;

        this.buttonList
                .add(new GuiButton(0, startX, this.height - 30, buttonWidth, 20, I18n.format("gui.common.back")));
        this.buttonList.add(new GuiButton(1, startX + buttonWidth + spacing, this.height - 30, buttonWidth, 20,
                "§a" + I18n.format("gui.packet.stacking_log.copy_all")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.setScreen(parentScreen);
        } else if (button.id == 1) {
            String fullLog = logs.stream()
                    .map(log -> String.format("Move From Slot %d -> To Slot %d\n  Pickup HEX: %s\n  Place HEX: %s\n",
                            log.sourceSlot, log.destinationSlot, log.pickupHexPayload, log.placeHexPayload))
                    .collect(Collectors.joining("\n"));
            setClipboardString(fullLog);
            if (this.mc.player != null) {
                this.mc.player.sendSystemMessage(new TextComponentString(
                        ChatFormatting.GREEN + I18n.format("msg.packet.stacking_log.copied_all")));
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = this.width - 40;
        int panelHeight = this.height - 60;
        int panelX = 20;
        int panelY = 20;

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.packet.stacking_log.title", logs.size()),
                this.width / 2, panelY + 5, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "§7" + I18n.format("gui.packet.stacking_log.tip"), this.width / 2,
                panelY + 15, 0xAAAAAA);

        int listX = panelX + 5;
        int listY = panelY + 30;
        int listWidth = panelWidth - 15;
        int listHeight = panelHeight - 35;
        int visibleLines = listHeight / itemHeight;
        maxScroll = Math.max(0, logs.size() - visibleLines);

        for (int i = 0; i < visibleLines; i++) {
            int index = i + scrollOffset;
            if (index >= logs.size())
                break;

            StackingLogEntry log = logs.get(index);
            int itemY = listY + i * itemHeight;

            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth && mouseY >= itemY
                    && mouseY < itemY + itemHeight;
            drawRect(listX, itemY, listX + listWidth, itemY + itemHeight - 2, isHovered ? 0x40FFFFFF : 0x20FFFFFF);

            String sessionIdHex = (log.sessionID != null) ? bytesToHex(log.sessionID)
                    : "§c" + I18n.format("gui.common.none");
            drawString(fontRenderer,
                    I18n.format("gui.packet.stacking_log.row_title", index + 1, log.sourceSlot, log.destinationSlot,
                            sessionIdHex),
                    listX + 2, itemY + 5, 0xFFFFFF);

            String pickupHex = log.pickupHexPayload;
            String placeHex = log.placeHexPayload;

            if (log.sessionID != null) {
                String idHex = bytesToHex(log.sessionID);
                pickupHex = pickupHex.replaceFirst(idHex, ChatFormatting.YELLOW + idHex + ChatFormatting.WHITE);
                placeHex = placeHex.replaceFirst(idHex, ChatFormatting.YELLOW + idHex + ChatFormatting.WHITE);
            }

            fontRenderer.drawSplitString("§a" + I18n.format("gui.packet.stacking_log.pickup_hex") + ": §f" + pickupHex,
                    listX + 4, itemY + 18, listWidth - 8, 0xFFFFFF);
            fontRenderer.drawSplitString("§c" + I18n.format("gui.packet.stacking_log.place_hex") + ": §f" + placeHex,
                    listX + 4, itemY + 31, listWidth - 8, 0xFFFFFF);
        }

        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 8;
            int scrollbarY = listY;
            int scrollbarHeight = listHeight;

            drawRect(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFF202020);

            int thumbHeight = Math.max(10, (int) ((float) visibleLines / logs.size() * scrollbarHeight));
            int thumbY = scrollbarY + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b));
        }
        return hex.toString().trim();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int panelX = 20;
        int panelY = 20;
        int listX = panelX + 5;
        int listY = panelY + 30;
        int listWidth = this.width - 40 - 15;
        int listHeight = this.height - 60 - 35;

        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < logs.size()) {
                StackingLogEntry log = logs.get(clickedIndex);
                int yInCard = (mouseY - listY) % itemHeight;

                if (yInCard >= 18 && yInCard < 31) {
                    setClipboardString(log.pickupHexPayload);
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(new TextComponentString(
                                ChatFormatting.GREEN + I18n.format("msg.packet.stacking_log.copied_pickup",
                                        clickedIndex + 1)));
                    }
                } else if (yInCard >= 31 && yInCard < 44) {
                    setClipboardString(log.placeHexPayload);
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(new TextComponentString(
                                ChatFormatting.GREEN + I18n.format("msg.packet.stacking_log.copied_place",
                                        clickedIndex + 1)));
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}









