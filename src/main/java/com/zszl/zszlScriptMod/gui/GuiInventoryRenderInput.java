package com.zszl.zszlScriptMod.gui;

import java.io.IOException;

/**
 * 参考版的渲染/输入桥接层在 1.20.1 中直接委托给 {@link GuiInventory} 的统一实现。
 */
final class GuiInventoryRenderInput extends GuiInventoryFeatureScreens {

    GuiInventoryRenderInput() {
    }

    static void drawOverlay(int screenWidth, int screenHeight) {
        GuiInventory.drawOverlay(screenWidth, screenHeight);
    }

    static void drawOverlay(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        GuiInventory.drawOverlay(screenWidth, screenHeight, mouseX, mouseY);
    }

    static void handleMouseClick(int mouseX, int mouseY, int mouseButton) throws IOException {
        GuiInventory.handleMouseClick(mouseX, mouseY, mouseButton);
    }

    static void handleMouseRelease(int mouseX, int mouseY, int mouseButton) {
        GuiInventory.handleMouseRelease(mouseX, mouseY, mouseButton);
    }

    static void handleMouseDrag(int mouseX, int mouseY) {
        GuiInventory.handleMouseDrag(mouseX, mouseY);
    }

    static void handleMouseWheel(int wheelDelta, int rawMouseX, int rawMouseY) {
        GuiInventory.handleMouseWheel(wheelDelta, rawMouseX, rawMouseY);
    }

    static boolean handleKeyTyped(char typedChar, int keyCode) throws IOException {
        return GuiInventory.handleKeyTyped(typedChar, keyCode);
    }
}
