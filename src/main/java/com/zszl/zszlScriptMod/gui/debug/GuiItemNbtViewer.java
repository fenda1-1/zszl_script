package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.compat.ItemComponentCompat;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiItemNbtViewer extends ThemedGuiScreen {

    private static final int BTN_COPY = 1;
    private static final int BTN_CLOSE = 2;
    private static final int MOUSE_WHEEL_NOTCH = 120;

    private final GuiScreen parentScreen;
    private final ItemStack stack;
    private final List<String> wrappedLines = new ArrayList<>();

    private String rawNbtText = "";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int scrollOffset;
    private int maxScrollOffset;

    public GuiItemNbtViewer(GuiScreen parentScreen, ItemStack stack) {
        this.parentScreen = parentScreen;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        this.panelWidth = Math.min(520, Math.max(300, this.width - 20));
        this.panelHeight = Math.min(360, Math.max(220, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.contentTop = this.panelY + 42;
        this.contentBottom = this.panelY + this.panelHeight - 34;

        this.buttonList.add(new ThemedButton(BTN_COPY, this.panelX + 12, this.panelY + this.panelHeight - 26, 110, 20, "复制原始NBT"));
        this.buttonList.add(new ThemedButton(BTN_CLOSE, this.panelX + this.panelWidth - 92, this.panelY + this.panelHeight - 26, 80, 20, I18n.format("gui.common.done")));

        rebuildContent();
    }

    private void rebuildContent() {
        this.wrappedLines.clear();

        this.rawNbtText = ItemComponentCompat.getDebugDataText(this.stack);

        String itemName = this.stack.isEmpty() ? "(空物品)" : this.stack.getHoverName().getString();
        Identifier itemId = this.stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(this.stack.getItem());
        int wrapWidth = Math.max(120, this.panelWidth - 32);

        appendWrappedLine("物品名称: " + itemName, wrapWidth);
        appendWrappedLine("物品ID: " + (itemId == null ? "minecraft:air" : itemId.toString()), wrapWidth);
        appendWrappedLine("数量: " + this.stack.getCount(), wrapWidth);
        appendWrappedLine("提示: 这里只做稳定的 1.20.x 原始 NBT 查看与复制。", wrapWidth);
        appendWrappedLine("", wrapWidth);
        appendWrappedLine("原始NBT:", wrapWidth);
        appendWrappedLine(this.rawNbtText.isEmpty() ? "{}" : this.rawNbtText, wrapWidth);

        int visibleLines = Math.max(1, (this.contentBottom - this.contentTop - 8) / 10);
        this.maxScrollOffset = Math.max(0, this.wrappedLines.size() - visibleLines);
        this.scrollOffset = clampInt(this.scrollOffset, 0, this.maxScrollOffset);
    }

    private void appendWrappedLine(String text, int wrapWidth) {
        if (text == null || text.isEmpty()) {
            this.wrappedLines.add("");
            return;
        }
        this.wrappedLines.addAll(this.fontRenderer.listFormattedStringToWidth(text, wrapWidth));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || this.maxScrollOffset <= 0) {
            return;
        }

        int mouseX = 0;
        int mouseY = 0;
        if (this.mc != null && this.width > 0 && this.height > 0) {
            mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getWidth());
            mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getHeight()) - 1;
        }

        if (mouseX < this.panelX + 8 || mouseX > this.panelX + this.panelWidth - 8
                || mouseY < this.contentTop || mouseY > this.contentBottom) {
            return;
        }

        int steps = Math.max(1, Math.abs(wheel) / MOUSE_WHEEL_NOTCH);
        if (wheel < 0) {
            this.scrollOffset = clampInt(this.scrollOffset + steps, 0, this.maxScrollOffset);
        } else {
            this.scrollOffset = clampInt(this.scrollOffset - steps, 0, this.maxScrollOffset);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_COPY) {
            setClipboardString(this.rawNbtText);
            notifyCopied();
            return;
        }
        if (button.id == BTN_CLOSE) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E || keyCode == Keyboard.KEY_F) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        if (isCtrlKeyDown() && keyCode == Keyboard.KEY_C) {
            setClipboardString(this.rawNbtText);
            notifyCopied();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "物品NBT查看", this.fontRenderer);

        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null && !this.stack.isEmpty()) {
            graphics.renderItem(this.stack, this.panelX + 12, this.panelY + 14);
            graphics.renderItemDecorations(this.mc.font, this.stack, this.panelX + 12, this.panelY + 14);
        }

        drawString(this.fontRenderer, "按 Ctrl+C 或点击下方按钮可复制原始 NBT。", this.panelX + 34, this.panelY + 18, GuiTheme.SUB_TEXT);

        drawRect(this.panelX + 8, this.contentTop - 2, this.panelX + this.panelWidth - 8, this.contentBottom + 2, 0x221A2533);

        int y = this.contentTop + 4;
        for (int i = this.scrollOffset; i < this.wrappedLines.size(); i++) {
            if (y > this.contentBottom - 10) {
                break;
            }
            drawString(this.fontRenderer, this.wrappedLines.get(i), this.panelX + 12, y, 0xD8E6F4);
            y += 10;
        }

        if (this.maxScrollOffset > 0) {
            int trackX = this.panelX + this.panelWidth - 10;
            int trackY = this.contentTop;
            int trackHeight = this.contentBottom - this.contentTop;
            int thumbHeight = Math.max(18,
                    (int) ((trackHeight / (float) Math.max(trackHeight, trackHeight + this.maxScrollOffset * 10)) * trackHeight));
            int thumbTrack = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) ((this.scrollOffset / (float) Math.max(1, this.maxScrollOffset)) * thumbTrack);
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void notifyCopied() {
        if (this.mc != null && this.mc.player != null) {
            this.mc.player.displayClientMessage(Component.literal("[物品查看器] 已复制原始NBT到剪贴板。")
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }
}
