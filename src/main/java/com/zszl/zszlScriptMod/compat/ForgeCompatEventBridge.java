package com.zszl.zszlScriptMod.compat;

import com.mojang.blaze3d.framegraph.FramePass;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiChat;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiMerchant;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.inventory.GuiChest;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.inventory.GuiContainer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent.MouseInputEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.network.FMLNetworkEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher.Side;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

public class ForgeCompatEventBridge {
    private static final Identifier LEGACY_RENDER_WORLD_PASS_ID =
            Identifier.fromNamespaceAndPath(zszlScriptMod.MODID, "legacy_render_world_last");
    private static final Identifier LEGACY_RENDER_GAME_OVERLAY_LAYER_ID =
            Identifier.fromNamespaceAndPath(zszlScriptMod.MODID, "legacy_render_game_overlay");

    @SubscribeEvent
    public void onClientTickPre(TickEvent.ClientTickEvent.Pre event) {
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent.BUS.post(
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent(
                        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START));
    }

    @SubscribeEvent
    public void onClientTickPost(TickEvent.ClientTickEvent.Post event) {
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent.BUS.post(
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent(
                        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END));
    }

    @SubscribeEvent
    public void onPlayerTickPre(TickEvent.PlayerTickEvent.Pre event) {
        if (!(event.player() instanceof LocalPlayer)) {
            return;
        }
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent.BUS.post(
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START,
                        event.player(), Side.CLIENT));
    }

    @SubscribeEvent
    public void onPlayerTickPost(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof LocalPlayer)) {
            return;
        }
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent.BUS.post(
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent(
                        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END,
                        event.player(), Side.CLIENT));
    }

    @SubscribeEvent
    public boolean onScreenOpening(ScreenEvent.Opening event) {
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent compat = new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent(wrap(event.getNewScreen()));
        return com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent.BUS.post(compat) || compat.isCanceled();
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        GuiScreen wrapped = wrap(event.getScreen());
        if (wrapped != null) {
            com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post.BUS.post(
                    new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post(
                            wrapped, wrapped.buttonList));
        }
    }

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        GuiScreen wrapped = wrap(event.getScreen());
        if (wrapped != null) {
            GuiCompatContext.push(event.getGuiGraphics());
            try {
                com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post.BUS.post(
                        new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post(
                                wrapped, event.getMouseX(), event.getMouseY(), event.getPartialTick()));
            } finally {
                GuiCompatContext.clear();
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Keyboard.setEvent(mapGlfwKey(event.getKey()), '\0', event.getAction() != 0);
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent.BUS.post(
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent());
    }

    @SubscribeEvent
    public boolean onMouseButton(InputEvent.MouseButton.Pre event) {
        int x = Mouse.getX();
        int y = Mouse.getY();
        Mouse.setButtonEvent(x, y, event.getButton(), event.getAction() != 0);
        GuiScreen wrapped = wrap(Minecraft.getInstance().screen);
        com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre compatGui =
                new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre(wrapped);
        boolean canceled = false;
        if (wrapped != null) {
            canceled |= com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre.BUS.post(compatGui);
        }
        MouseInputEvent compat = new MouseInputEvent(wrapped, event.getButton());
        canceled |= MouseInputEvent.BUS.post(compat);
        return canceled || compatGui.isCanceled() || compat.isCanceled();
    }

    @SubscribeEvent
    public void onMouseButtonPost(InputEvent.MouseButton.Post event) {
        GuiScreen wrapped = wrap(Minecraft.getInstance().screen);
        if (wrapped != null) {
            com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Post.BUS.post(
                    new com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Post(wrapped));
        }
        Mouse.clearEventState();
    }

    @SubscribeEvent
    public void onNetworkLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        FMLNetworkEvent.ClientConnectedToServerEvent.BUS.post(new FMLNetworkEvent.ClientConnectedToServerEvent());
    }

    @SubscribeEvent
    public void onNetworkLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FMLNetworkEvent.ClientDisconnectionFromServerEvent.BUS.post(new FMLNetworkEvent.ClientDisconnectionFromServerEvent());
    }

    public void onAddFramePass(AddFramePassEvent event) {
        if (event == null) {
            return;
        }
        event.addPass(LEGACY_RENDER_WORLD_PASS_ID, new FramePassManager.PassDefinition() {
            @Override
            public void extracts(LevelTargetBundle bundle, FramePass pass) {
                bundle.main = pass.readsAndWrites(bundle.main);
            }

            @Override
            public void executes(LevelRenderState state) {
                Minecraft mc = Minecraft.getInstance();
                if (mc == null || mc.level == null || mc.player == null || mc.gameRenderer == null) {
                    return;
                }
                float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                RenderWorldLastEvent.BUS.post(new RenderWorldLastEvent(partialTicks, null, null,
                        mc.gameRenderer.getMainCamera()));
            }
        });
    }

    public void onAddGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
        if (event == null || event.getLayeredDraw() == null) {
            return;
        }
        event.getLayeredDraw().addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, LEGACY_RENDER_GAME_OVERLAY_LAYER_ID,
                ForgeLayeredDraw.HOTBAR_AND_DECOS, (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc == null || mc.font == null || graphics == null) {
                        return;
                    }
                    float partialTicks = mc.getDeltaTracker() == null
                            ? 0.0F
                            : mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                    GuiCompatContext.push(graphics);
                    try {
                        RenderGameOverlayEvent.Pre pre = new RenderGameOverlayEvent.Pre(
                                RenderGameOverlayEvent.ElementType.ALL, partialTicks);
                        boolean canceled = RenderGameOverlayEvent.Pre.BUS.post(pre) || pre.isCanceled();
                        if (!canceled) {
                            RenderGameOverlayEvent.Post.BUS.post(new RenderGameOverlayEvent.Post(
                                    RenderGameOverlayEvent.ElementType.ALL, partialTicks));
                        }
                    } finally {
                        GuiCompatContext.clear();
                    }
                });
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



