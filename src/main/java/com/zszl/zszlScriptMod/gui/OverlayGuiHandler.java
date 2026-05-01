package com.zszl.zszlScriptMod.gui;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.MouseEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
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
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

public class OverlayGuiHandler {

    private static final OverlayGuiHandler INSTANCE = new OverlayGuiHandler();
    private static boolean registered;
    private static Field gameRendererGuiRenderStateField;

    private static final class GraphicsContext {
        final GuiGraphics graphics;

        GraphicsContext(GuiGraphics graphics) {
            this.graphics = graphics;
        }
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        RenderGameOverlayEvent.Post.BUS.addListener(INSTANCE::onRenderOverlay);
        GuiScreenEvent.DrawScreenEvent.Post.BUS.addListener(INSTANCE::onDrawScreenPost);
        MouseEvent.BUS.addListener((java.util.function.Consumer<MouseEvent>) INSTANCE::onMouseInput);
        InputEvent.MouseInputEvent.BUS.addListener(
                (java.util.function.Consumer<InputEvent.MouseInputEvent>) INSTANCE::onMouseInput);
        InputEvent.KeyInputEvent.BUS.addListener(
                (java.util.function.Consumer<InputEvent.KeyInputEvent>) INSTANCE::onKeyInput);
        GuiScreenEvent.KeyboardInputEvent.Pre.BUS.addListener(INSTANCE::onKeyboardInputPre);
    }

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

        renderInGameHud(mc);
    }

    private static void renderInGameHud(Minecraft mc) {
        if (mc.screen == null && ModConfig.showMouseCoordinates) {
            renderMouseCoordinates(mc);
        }

        if (mc.screen == null && zszlScriptMod.isGuiVisible) {
            zszlScriptMod.isGuiVisible = false;
        }

        if (mc.screen == null) {
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

        GraphicsContext context = currentGuiGraphics(mc);
        if (context == null || context.graphics == null) {
            GuiInventory.updateMasterStatusHudEditorBounds(hudBounds, exitBounds);
            return;
        }
        GuiGraphics graphics = context.graphics;

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
            graphics.drawString(mc.font, line, baseX, drawY, Gui.withDefaultTextAlpha(0xFFFFFF), true);
            drawY += lineHeight;
        }
        GuiInventory.updateMasterStatusHudEditorBounds(hudBounds, exitBounds);
    }

    private static GraphicsContext currentGuiGraphics(Minecraft mc) {
        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics != null) {
            return new GraphicsContext(graphics);
        }
        GuiRenderState renderState = currentGuiRenderState(mc);
        if (mc == null || renderState == null || mc.getWindow() == null) {
            return null;
        }
        int scaledMouseX = (int) Math.round(Mouse.getX() * mc.getWindow().getGuiScaledWidth()
                / (double) Math.max(1, mc.getWindow().getScreenWidth()));
        int scaledMouseY = (int) Math.round(Mouse.getY() * mc.getWindow().getGuiScaledHeight()
                / (double) Math.max(1, mc.getWindow().getScreenHeight()));
        return new GraphicsContext(new GuiGraphics(mc, renderState, scaledMouseX, scaledMouseY));
    }

    private static GuiRenderState currentGuiRenderState(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) {
            return null;
        }
        try {
            if (gameRendererGuiRenderStateField == null) {
                gameRendererGuiRenderStateField = mc.gameRenderer.getClass().getDeclaredField("guiRenderState");
                gameRendererGuiRenderStateField.setAccessible(true);
            }
            Object value = gameRendererGuiRenderStateField.get(mc.gameRenderer);
            return value instanceof GuiRenderState ? (GuiRenderState) value : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
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
        GraphicsContext context = currentGuiGraphics(mc);
        if (context == null || context.graphics == null) {
            return;
        }
        GuiGraphics graphics = context.graphics;
        graphics.drawString(mc.font, coordText, (screenWidth - textWidth) / 2, 5,
                Gui.withDefaultTextAlpha(0xFFFFFF), true);
    }

    private static List<String> buildMasterStatusHudLines(boolean editingPreview) {
        List<String> lines = new ArrayList<>();
        lines.addAll(editingPreview ? SpeedHandler.getStatusLines(true) : SpeedHandler.getStatusLines());
        lines.addAll(editingPreview ? MovementFeatureManager.getStatusLines(true) : MovementFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? BlockFeatureManager.getStatusLines(true) : BlockFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? WorldFeatureManager.getStatusLines(true) : WorldFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? ItemFeatureManager.getStatusLines(true) : ItemFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? MiscFeatureManager.getStatusLines(true) : MiscFeatureManager.getStatusLines());
        lines.addAll(editingPreview ? RenderFeatureManager.getStatusLines(true) : RenderFeatureManager.getStatusLines());
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
