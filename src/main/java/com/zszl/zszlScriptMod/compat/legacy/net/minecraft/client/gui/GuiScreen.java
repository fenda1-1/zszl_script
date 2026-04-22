package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiScreen extends Screen {

    public final List<GuiButton> buttonList = new ArrayList<>();
    public Minecraft mc;
    public FontRenderer fontRenderer;
    public boolean allowUserInput;

    public GuiScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        this.mc = Minecraft.getInstance();
        this.fontRenderer = new FontRenderer(this.minecraft.font);
        initGui();
    }

    public void initGui() {
    }

    public void updateScreen() {
    }

    public void onGuiClosed() {
    }

    public boolean doesGuiPauseGame() {
        return isPauseScreen();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : buttonList) {
            button.drawButton(mc, mouseX, mouseY, partialTicks);
        }
    }

    public void drawDefaultBackground() {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            renderBackground(graphics);
        }
    }

    public void drawString(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        (fontRendererIn == null ? fontRenderer : fontRendererIn).drawString(text, x, y, color);
    }

    public void drawCenteredString(FontRenderer fontRendererIn, String text, int x, int y, int color) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            graphics.drawCenteredString((fontRendererIn == null ? fontRenderer : fontRendererIn).unwrap(),
                    text == null ? "" : text, x, y, color);
        }
    }

    public void drawHoveringText(List<String> textLines, int x, int y) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null || textLines == null) {
            return;
        }
        List<Component> components = new ArrayList<>();
        for (String line : textLines) {
            components.add(Component.literal(line == null ? "" : line));
        }
        graphics.renderComponentTooltip(this.font, components, x, y);
    }

    public void drawHoveringText(List<String> textLines, int x, int y, FontRenderer ignored) {
        drawHoveringText(textLines, x, y);
    }

    protected boolean handleComponentClick(Component component) {
        return false;
    }

    protected void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    protected void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        Gui.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }

    protected void drawHorizontalLine(int startX, int endX, int y, int color) {
        Gui.drawRect(Math.min(startX, endX), y, Math.max(startX, endX) + 1, y + 1, color);
    }

    protected void drawVerticalLine(int x, int startY, int endY, int color) {
        Gui.drawRect(x, Math.min(startY, endY), x + 1, Math.max(startY, endY) + 1, color);
    }

    public static void setClipboardString(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }

    public static String getClipboardString() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mc = Minecraft.getInstance();
        this.fontRenderer = new FontRenderer(this.minecraft.font);
        GuiCompatContext.push(graphics);
        try {
            drawScreen(mouseX, mouseY, partialTick);
        } finally {
            GuiCompatContext.clear();
        }
    }

    @Override
    public void tick() {
        updateScreen();
    }

    @Override
    public void removed() {
        onGuiClosed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int legacyKeyCode = Keyboard.fromGlfwKey(keyCode);
        if (legacyKeyCode == Keyboard.KEY_NONE) {
            legacyKeyCode = keyCode;
        }
        try {
            keyTyped('\0', legacyKeyCode);
        } catch (IOException ignored) {
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        try {
            keyTyped(codePoint, 0);
        } catch (IOException ignored) {
        }
        return super.charTyped(codePoint, modifiers);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        try {
            mouseClicked((int) mouseX, (int) mouseY, mouseButton);
        } catch (IOException ignored) {
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        List<GuiButton> snapshot = new ArrayList<>(buttonList);
        for (GuiButton button : snapshot) {
            if (button == null || !buttonList.contains(button)) {
                continue;
            }
            if (button.mousePressed(mc, mouseX, mouseY)) {
                button.playPressSound(mc.getSoundManager());
                actionPerformed(button);
                return;
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GuiButton guiButton : buttonList) {
            guiButton.mouseReleased((int) mouseX, (int) mouseY);
        }
        mouseReleased((int) mouseX, (int) mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        mouseClickMove((int) mouseX, (int) mouseY, button, 0L);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    public void handleMouseInput() throws IOException {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int wheel = delta > 0.0D ? 120 : (delta < 0.0D ? -120 : 0);
        int rawX = 0;
        int rawY = 0;
        if (this.mc != null && this.width > 0 && this.height > 0) {
            rawX = (int) Math.round(mouseX * this.mc.getWindow().getWidth() / (double) this.width);
            rawY = (int) Math.round((this.height - mouseY - 1.0D) * this.mc.getWindow().getHeight()
                    / (double) this.height);
        }
        Mouse.setScrollEvent(rawX, rawY, wheel);
        try {
            handleMouseInput();
            return wheel != 0 || super.mouseScrolled(mouseX, mouseY, delta);
        } catch (IOException e) {
            return false;
        } finally {
            Mouse.clearEventState();
        }
    }

    protected void actionPerformed(GuiButton button) throws IOException {
    }

    public static boolean isCtrlKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    public static boolean isShiftKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    public static boolean isAltKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }
}


