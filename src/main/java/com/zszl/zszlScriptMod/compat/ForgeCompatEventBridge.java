package com.zszl.zszlScriptMod.compat;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiChat;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiMerchant;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.inventory.GuiChest;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.network.FMLNetworkEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher.Side;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class ForgeCompatEventBridge {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent(mapPhase(event.phase)));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof LocalPlayer)) {
            return;
        }
        MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                mapPhase(event.phase), event.player, Side.CLIENT));
    }

    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent compat = new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent(wrap(event.getNewScreen()));
        if (MinecraftForge.EVENT_BUS.post(compat)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        GuiScreen wrapped = wrap(event.getScreen());
        if (wrapped != null) {
            MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post(
                    wrapped, wrapped.buttonList));
        }
    }

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        GuiScreen wrapped = wrap(event.getScreen());
        if (wrapped != null) {
            MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post(
                    wrapped, event.getMouseX(), event.getMouseY(), event.getPartialTick()));
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Keyboard.setEvent(mapGlfwKey(event.getKey()), '\0', event.getAction() != 0);
        MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent());
    }

    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Pre event) {
        int x = Mouse.getX();
        int y = Mouse.getY();
        Mouse.setButtonEvent(x, y, event.getButton(), event.getAction() != 0);
        GuiScreen wrapped = wrap(Minecraft.getInstance().screen);
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre compatGui =
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre(wrapped);
        if (wrapped != null) {
            MinecraftForge.EVENT_BUS.post(compatGui);
        }
        MouseInputEvent compat = new MouseInputEvent(wrapped, event.getButton());
        MinecraftForge.EVENT_BUS.post(compat);
        if (compatGui.isCanceled() || compat.isCanceled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouseButtonPost(InputEvent.MouseButton.Post event) {
        GuiScreen wrapped = wrap(Minecraft.getInstance().screen);
        if (wrapped != null) {
            MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Post(wrapped));
        }
        Mouse.clearEventState();
    }

    @SubscribeEvent
    public void onNetworkLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        MinecraftForge.EVENT_BUS.post(new FMLNetworkEvent.ClientConnectedToServerEvent());
    }

    @SubscribeEvent
    public void onNetworkLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MinecraftForge.EVENT_BUS.post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent());
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent.Pre compat =
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent.Pre(mapOverlay(event), event.getPartialTick());
        if (MinecraftForge.EVENT_BUS.post(compat)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent.Post(
                mapOverlay(event), event.getPartialTick()));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            MinecraftForge.EVENT_BUS.post(new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent(
                    event.getPartialTick(),
                    event.getPoseStack(),
                    event.getProjectionMatrix(),
                    event.getCamera()));
        }
    }

    private static com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase mapPhase(TickEvent.Phase phase) {
        return phase == TickEvent.Phase.START
                ? com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START
                : com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END;
    }

    private static com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType mapOverlay(RenderGuiOverlayEvent event) {
        String path = event.getOverlay().id().getPath();
        if ("crosshair".equals(path) || "crosshairs".equals(path)) {
            return com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.CROSSHAIRS;
        }
        return com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL;
    }

    private static int mapGlfwKey(int key) {
        return Keyboard.fromGlfwKey(key);
    }

    private static GuiScreen wrap(Screen screen) {
        if (screen == null) {
            return null;
        }
        if (screen instanceof ChatScreen) {
            return new WrappedGuiChat((ChatScreen) screen);
        }
        if (screen instanceof MerchantScreen) {
            return new WrappedGuiMerchant((MerchantScreen) screen);
        }
        if (screen instanceof ContainerScreen) {
            return new WrappedGuiChest((AbstractContainerScreen<?>) screen);
        }
        if (screen instanceof AbstractContainerScreen) {
            return new WrappedGuiContainer((AbstractContainerScreen<?>) screen);
        }
        return new WrappedGuiScreen(screen);
    }

    private static void initWrapped(GuiScreen gui, Screen delegate) {
        gui.mc = Minecraft.getInstance();
        gui.fontRenderer = new FontRenderer(gui.mc.font);
        gui.width = delegate.width;
        gui.height = delegate.height;
    }

    private static class WrappedGuiScreen extends GuiScreen {
        protected final Screen delegate;

        WrappedGuiScreen(Screen delegate) {
            this.delegate = delegate;
            initWrapped(this, delegate);
        }
    }

    private static class WrappedGuiContainer extends GuiContainer {
        protected final AbstractContainerScreen<?> delegate;

        WrappedGuiContainer(AbstractContainerScreen<?> delegate) {
            this.delegate = delegate;
            initWrapped(this, delegate);
            this.inventorySlots = delegate.getMenu();
            this.guiLeft = delegate.getGuiLeft();
            this.guiTop = delegate.getGuiTop();
            this.xSize = delegate.getXSize();
            this.ySize = delegate.getYSize();
            this.hoveredSlot = resolveHoveredSlot(delegate);
        }
    }

    private static class WrappedGuiChest extends GuiChest {
        WrappedGuiChest(AbstractContainerScreen<?> delegate) {
            initWrapped(this, delegate);
            this.inventorySlots = delegate.getMenu();
            this.guiLeft = delegate.getGuiLeft();
            this.guiTop = delegate.getGuiTop();
            this.xSize = delegate.getXSize();
            this.ySize = delegate.getYSize();
            this.hoveredSlot = resolveHoveredSlot(delegate);
        }
    }

    private static class WrappedGuiMerchant extends GuiMerchant {
        WrappedGuiMerchant(MerchantScreen delegate) {
            initWrapped(this, delegate);
        }
    }

    private static class WrappedGuiChat extends GuiChat {
        WrappedGuiChat(ChatScreen delegate) {
            initWrapped(this, delegate);
            this.field_146415_a = resolveChatInput(delegate);
        }
    }

    private static Slot resolveHoveredSlot(AbstractContainerScreen<?> screen) {
        try {
            for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Slot.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object value = field.get(screen);
                        if (value instanceof Slot) {
                            return (Slot) value;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static GuiTextField resolveChatInput(ChatScreen screen) {
        try {
            for (Field field : ChatScreen.class.getDeclaredFields()) {
                if (net.minecraft.client.gui.components.EditBox.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    net.minecraft.client.gui.components.EditBox editBox =
                            (net.minecraft.client.gui.components.EditBox) field.get(screen);
                    if (editBox != null) {
                        GuiTextField wrapped = new GuiTextField(0, new FontRenderer(Minecraft.getInstance().font),
                                editBox.getX(), editBox.getY(), editBox.getWidth(), editBox.getHeight());
                        wrapped.setText(editBox.getValue());
                        return wrapped;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}



