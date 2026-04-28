package com.zszl.zszlScriptMod.gui;

import java.io.IOException;

import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;

public class GuiInventoryOverlayScreen extends GuiScreen {

    private int toRawMouseX(double scaledMouseX) {
        Minecraft minecraft = Minecraft.getInstance();
        int scaledWidth = Math.max(1, this.width);
        int rawWidth = Math.max(1, minecraft.getWindow().getScreenWidth());
        return (int) Math.round(scaledMouseX * rawWidth / (double) scaledWidth);
    }

    private int toRawMouseY(double scaledMouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int scaledHeight = Math.max(1, this.height);
        int rawHeight = Math.max(1, minecraft.getWindow().getScreenHeight());
        double invertedScaledY = scaledHeight - scaledMouseY - 1.0D;
        return (int) Math.round(invertedScaledY * rawHeight / (double) scaledHeight);
    }

    @Override
    public void initGui() {
        super.initGui();
        com.zszl.zszlScriptMod.zszlScriptMod.isGuiVisible = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiInventory.drawOverlay(this.width, this.height, mouseX, mouseY);
        if (GuiInventory.isMasterStatusHudEditMode()) {
            OverlayGuiHandler.renderMasterStatusHudPreview();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        try {
            GuiInventory.handleMouseClick(toRawMouseX(mouseX), toRawMouseY(mouseY), mouseButton);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        GuiInventory.handleMouseRelease((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 || GuiInventory.isAnyDragActive()) {
            GuiInventory.handleMouseDrag((int) mouseX, (int) mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0.0D) {
            return false;
        }
        int wheel = delta > 0.0D ? 120 : -120;
        GuiInventory.handleMouseWheel(wheel, toRawMouseX(mouseX), toRawMouseY(mouseY));
        return true;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (GuiInventory.handleKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            com.zszl.zszlScriptMod.zszlScriptMod.isGuiVisible = false;
            this.mc.setScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        GuiInventory.handleMouseRelease(0, 0, 0);
        com.zszl.zszlScriptMod.zszlScriptMod.isGuiVisible = false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}





