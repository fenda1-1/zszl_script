package com.zszl.zszlScriptMod.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.MouseEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.packet.InputTimelineManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class OverlayGuiHandler {

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || mc.player == null) {
            GuiInventory.updateMasterStatusHudEditorBounds(null, null);
            return;
        }

        if (mc.screen == null && ModConfig.showMouseCoordinates) {
            renderMouseCoordinates(mc);
        }

        if (mc.screen == null && !zszlScriptMod.isGuiVisible) {
            drawMasterStatusHud(false);
        }
    }

    public static void renderMasterStatusHudPreview() {
        drawMasterStatusHud(true);
    }

    private static void drawMasterStatusHud(boolean editingPreview) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) {
            GuiInventory.updateMasterStatusHudEditorBounds(null, null);
            return;
        }

        List<String> lines = buildMasterStatusHudLines(editingPreview);
        if (lines.isEmpty()) {
            GuiInventory.updateMasterStatusHudEditorBounds(null, null);
            return;
        }

        int baseX = Math.max(0, MovementFeatureManager.getMasterStatusHudX());
        int baseY = Math.max(0, MovementFeatureManager.getMasterStatusHudY());
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, mc.font.width(line));
        }

        int lineHeight = 10;
        int panelX = Math.max(0, baseX - 4);
        int panelY = Math.max(0, baseY - 4);
        int panelWidth = Math.max(120, maxWidth + 8);
        int panelHeight = lines.size() * lineHeight + 8;
        Rectangle hudBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        Rectangle exitBounds = null;

        GuiGraphics graphics = currentGuiGraphics(mc);

        if (editingPreview) {
            panelHeight += 22;
            hudBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x7A0F1720);
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF63C7FF);
            graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF35536C);
            graphics.drawString(mc.font, "拖动此区域调整 HUD 位置", panelX + 5, panelY + panelHeight - 18, 0xFFEAF6FF, true);

            int exitWidth = 44;
            int exitHeight = 14;
            int exitX = panelX + panelWidth - exitWidth - 4;
            int exitY = panelY + panelHeight - exitHeight - 4;
            exitBounds = new Rectangle(exitX, exitY, exitWidth, exitHeight);

            int hoverMouseX = (int) Math.round(Mouse.getX() * mc.getWindow().getGuiScaledWidth()
                    / (double) Math.max(1, mc.getWindow().getScreenWidth()));
            int hoverMouseY = (int) Math.round(Mouse.getY() * mc.getWindow().getGuiScaledHeight()
                    / (double) Math.max(1, mc.getWindow().getScreenHeight()));
            boolean hovered = exitBounds.contains(hoverMouseX, hoverMouseY);
            graphics.fill(exitX - 1, exitY - 1, exitX + exitWidth + 1, exitY + exitHeight + 1,
                    hovered ? 0xFF63C7FF : 0xFF35536C);
            graphics.fill(exitX, exitY, exitX + exitWidth, exitY + exitHeight,
                    hovered ? 0xAA244B64 : 0xAA162431);
            graphics.drawString(mc.font, "退出编辑", exitX + 6, exitY + 3, 0xFFFFFFFF, true);
        }

        int drawY = baseY;
        for (String line : lines) {
            graphics.drawString(mc.font, line, baseX, drawY, 0xFFFFFF, true);
            drawY += lineHeight;
        }
        graphics.flush();

        GuiInventory.updateMasterStatusHudEditorBounds(hudBounds, exitBounds);
    }

    private static GuiGraphics currentGuiGraphics(Minecraft mc) {
        return new GuiGraphics(mc, mc.renderBuffers().bufferSource());
    }

    private static void renderMouseCoordinates(Minecraft mc) {
        if (mc == null || mc.font == null || mc.getWindow() == null) {
            return;
        }
        int rawMouseX = Mouse.getX();
        int rawMouseY = Mouse.getY();
        int scaledMouseX = (int) Math.round(rawMouseX * mc.getWindow().getGuiScaledWidth()
                / (double) Math.max(1, mc.getWindow().getScreenWidth()));
        int scaledMouseY = (int) Math.round(rawMouseY * mc.getWindow().getGuiScaledHeight()
                / (double) Math.max(1, mc.getWindow().getScreenHeight()));
        String coordText = I18n.get("gui.overlay.debug.coords", rawMouseX, rawMouseY, scaledMouseX, scaledMouseY);
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth = mc.font.width(coordText);
        GuiGraphics graphics = currentGuiGraphics(mc);
        graphics.drawString(mc.font, coordText, (screenWidth - textWidth) / 2, 5, 0xFFFFFF, true);
        graphics.flush();
    }

    private static List<String> buildMasterStatusHudLines(boolean editingPreview) {
        List<String> lines = new ArrayList<>();
        lines.addAll(editingPreview ? SpeedHandler.getStatusLines(true) : SpeedHandler.getStatusLines());
        lines.addAll(editingPreview ? MovementFeatureManager.getStatusLines(true) : MovementFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? BlockFeatureManager.getStatusLines(true) : BlockFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? WorldFeatureManager.getStatusLines(true) : WorldFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? ItemFeatureManager.getStatusLines(true) : ItemFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? MiscFeatureManager.getStatusLines(true) : MiscFeatureManager.getStatusLines());
        if (!editingPreview || !lines.isEmpty()) {
            return lines;
        }
        lines.add("§a[总状态HUD] §f位置预览");
        lines.add("§7当前没有可显示的状态行");
        lines.add("§7拖动后将保存新的 HUD 位置");
        return lines;
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event != null && event.getGui() != null && mc != null && ModConfig.showMouseCoordinates) {
            renderMouseCoordinates(mc);
        }
    }

    @SubscribeEvent
    public void onMouseInput(MouseEvent event) {
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() >= 0 && Mouse.getEventButton() <= 2) {
            InputTimelineManager.recordMouseClick(Mouse.getEventButton());
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
    }

    @SubscribeEvent
    public void onKeyboardInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() != Keyboard.KEY_NONE) {
            InputTimelineManager.recordKeyPress(Keyboard.getEventKey());
        }
    }
}
