package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.util.List;

@SideOnly(Side.CLIENT)
public class LootModGuiHandler {

    private static Class<? extends GuiScreen> mainLootGuiClass = null;
    private GuiScreen overlayBoundGui;
    private GuiButton overlayAutoAssignButton;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen newGui = event.getGui();
        if (newGui == null) {
            overlayBoundGui = null;
        }
        if (newGui == null && !LootHelper.INSTANCE.isLootMainGuiOpen) {
            mainLootGuiClass = null;
        }
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (LootHelper.INSTANCE.isLootTicketValid) {
            LootHelper.INSTANCE.isLootContextActive = true;
            LootHelper.INSTANCE.isLootTicketValid = false;
            mainLootGuiClass = event.getGui().getClass();
        }

        if (!shouldHandleCurrentGui(event.getGui())) {
            return;
        }

        ensureButtonsPresent(event.getButtonList(), event.getGui());
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() == null || !LootHelper.INSTANCE.shouldInjectButtons()) {
            return;
        }
        if (mainLootGuiClass == null) {
            mainLootGuiClass = event.getGui().getClass();
            LootHelper.INSTANCE.isLootContextActive = true;
            LootHelper.INSTANCE.isLootTicketValid = false;
        }
        if (!shouldHandleCurrentGui(event.getGui())) {
            return;
        }
        ensureButtonsPresent(getButtonList(event.getGui()), event.getGui());
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!shouldHandleCurrentGui(event.getGui())) {
            return;
        }
        ensureOverlayButtons(event.getGui());
        overlayAutoAssignButton.drawButton(Minecraft.getMinecraft(), event.getMouseX(), event.getMouseY(),
                event.getRenderPartialTicks());
    }

    @SubscribeEvent
    public void onActionPerformedPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton() == null || event.getGui() == null) {
            return;
        }
        int id = event.getButton().id;
        if (id != LootHelper.BTN_AUTO_ASSIGN) {
            return;
        }
        if (!shouldHandleCurrentGui(event.getGui())) {
            return;
        }
        try {
            LootHelper.INSTANCE.startAutoAssign();
            event.setCanceled(true);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LootModGuiHandler] 处理战利品按钮点击时出错。", e);
        }
    }

    @SubscribeEvent
    public void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!shouldHandleCurrentGui(event.getGui())) {
            return;
        }
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) {
            return;
        }

        GuiScreen gui = event.getGui();
        ensureOverlayButtons(gui);
        int mouseX = Mouse.getEventX() * gui.width / gui.mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / gui.mc.displayHeight - 1;

        if (overlayAutoAssignButton.mousePressed(gui.mc, mouseX, mouseY)) {
            LootHelper.INSTANCE.startAutoAssign();
            event.setCanceled(true);
        }
    }

    private boolean shouldHandleCurrentGui(GuiScreen gui) {
        return gui != null
                && LootHelper.INSTANCE.shouldInjectButtons()
                && mainLootGuiClass != null
                && gui.getClass() == mainLootGuiClass;
    }

    private void ensureButtonsPresent(List<GuiButton> buttonList, GuiScreen gui) {
        if (buttonList == null || gui == null) {
            return;
        }
        if (buttonList.stream().anyMatch(b -> b.id == LootHelper.BTN_AUTO_ASSIGN)) {
            return;
        }

        int panelX = 10;
        int panelY = gui.height / 2 - 88;
        int buttonWidth = 80;
        int buttonHeight = 20;

        buttonList.add(new GuiButton(LootHelper.BTN_AUTO_ASSIGN, panelX, panelY, buttonWidth, buttonHeight, "§a一键分配"));
    }

    private void ensureOverlayButtons(GuiScreen gui) {
        if (overlayBoundGui != gui) {
            overlayAutoAssignButton = null;
            overlayBoundGui = gui;
        }
        int panelX = 10;
        int panelY = gui.height / 2 - 88;
        int buttonWidth = 80;
        int buttonHeight = 20;

        if (overlayAutoAssignButton == null) {
            overlayAutoAssignButton = new GuiButton(LootHelper.BTN_AUTO_ASSIGN, panelX, panelY, buttonWidth,
                    buttonHeight, "§a一键分配");
            return;
        }

        overlayAutoAssignButton.x = panelX;
        overlayAutoAssignButton.y = panelY;
        overlayAutoAssignButton.width = buttonWidth;
        overlayAutoAssignButton.height = buttonHeight;
        overlayAutoAssignButton.displayString = "§a一键分配";
    }

    @SuppressWarnings("unchecked")
    private List<GuiButton> getButtonList(GuiScreen gui) {
        if (gui == null) {
            return null;
        }
        try {
            Field field = GuiScreen.class.getDeclaredField("buttonList");
            field.setAccessible(true);
            Object value = field.get(gui);
            if (value instanceof List) {
                return (List<GuiButton>) value;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LootModGuiHandler] 读取 GuiScreen.buttonList 失败。", e);
        }
        return null;
    }
}
