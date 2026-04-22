package com.zszl.zszlScriptMod;

import com.mojang.blaze3d.platform.InputConstants;
import com.zszl.zszlScriptMod.baritone.compat.pathing.HumanLikeRouteTemplateManager;
import com.zszl.zszlScriptMod.compat.ForgeCompatEventBridge;
import com.zszl.zszlScriptMod.config.BaritoneSettingsConfig;
import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import com.zszl.zszlScriptMod.config.LoopExecutionConfig;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.config.GuiBaritoneCommandTable;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.gui.OverlayGuiHandler;
import com.zszl.zszlScriptMod.gui.packet.PacketFilterConfig;
import com.zszl.zszlScriptMod.gui.packet.PacketInterceptConfig;
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler;
import com.zszl.zszlScriptMod.handlers.AutoUseItemHandler;
import com.zszl.zszlScriptMod.handlers.AutoEscapeHandler;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;
import com.zszl.zszlScriptMod.handlers.ChatEventHandler;
import com.zszl.zszlScriptMod.handlers.ConditionalExecutionHandler;
import com.zszl.zszlScriptMod.handlers.FlyHandler;
import com.zszl.zszlScriptMod.handlers.GuiBlockerHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.KillTimerHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.handlers.BlockReplacementHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.system.GlobalKeybindListener;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.utils.ClientPerformanceWarmupManager;
import com.zszl.zszlScriptMod.utils.HttpsCompat;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiInspectionManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(zszlScriptMod.MODID)
public class zszlScriptMod {
    public static final String MODID = "zszl_script";
    public static final String NAME = "我的世界脚本";
    public static final String VERSION = "v1.0.6";

    public static final Logger LOGGER = LogManager.getLogger(zszlScriptMod.class);
    public static zszlScriptMod instance;
    public static boolean isGuiVisible = false;

    private static final KeyMapping GUI_KEY = new KeyMapping("key.zszl_script.open_menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F, "key.categories.zszl_script");

    public zszlScriptMod() {
        instance = this;
        HttpsCompat.install();

        ProfileManager.initialize();
        GuiBaritoneCommandTable.cleanupLegacyStateDirectory();
        ModConfig.loadAllConfigs();
        LoopExecutionConfig.load();
        GuiInventory.loopCount = LoopExecutionConfig.INSTANCE.loopCount;
        ChatOptimizationConfig.load();
        BaritoneSettingsConfig.load();
        HumanLikeMovementConfig.load();
        HumanLikeRouteTemplateManager.load();
        PacketFilterConfig.load();
        PacketInterceptConfig.load();
        ClientPerformanceWarmupManager.warmupCurrentProfileAsync("startup");

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ForgeCompatEventBridge());
        MinecraftForge.EVENT_BUS.register(GlobalEventListener.instance);
        MinecraftForge.EVENT_BUS.register(new ChatEventHandler());
        MinecraftForge.EVENT_BUS.register(new OverlayGuiHandler());
        MinecraftForge.EVENT_BUS.register(new GlobalKeybindListener());
        MinecraftForge.EVENT_BUS.register(SimulatedKeyInputManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(SpeedHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(KillAuraHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(KillTimerHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AutoFollowHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AutoPickupHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AutoFishingHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AutoEscapeHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ConditionalExecutionHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(FlyHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(BlockReplacementHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WarehouseEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MovementFeatureManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(BlockFeatureManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldFeatureManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(RenderFeatureManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ItemFeatureManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MiscFeatureManager.INSTANCE);

        AutoEscapeHandler.loadConfig();
        SpeedHandler.loadConfig();
        KillTimerHandler.loadConfig();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterKeyMappings);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GUI_KEY);
    }

    public static int getGuiToggleKeyCode() {
        return GUI_KEY.getKey().getValue();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MiscFeatureManager.INSTANCE.tick(mc);
        PacketCaptureHandler.onClientTick();
        GuiInspectionManager.onClientTick();
        RenderFeatureManager.INSTANCE.tick(mc);
        if (mc.player == null) {
            return;
        }
        AutoEatHandler.checkAutoEat(mc.player);
        AutoUseItemHandler.INSTANCE.tick();
        SpeedHandler.INSTANCE.tick(mc);
        MovementFeatureManager.tickClientPlayerFeatures(mc.player);
        WorldFeatureManager.INSTANCE.tick(mc);
        ItemFeatureManager.INSTANCE.tick(mc);
        if (GUI_KEY.consumeClick()) {
            GuiInventory.toggleOverlayScreen();
        }
    }

    @SubscribeEvent
    public void onNetworkLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        MiscFeatureManager.INSTANCE.onClientConnected();
        ClientPerformanceWarmupManager.warmupCurrentProfileAsync("network-login");
        PacketCaptureHandler.injectIntoCurrentConnection();
    }

    @SubscribeEvent
    public void onNetworkLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MiscFeatureManager.INSTANCE.onClientDisconnect();
        MovementFeatureManager.INSTANCE.onClientDisconnect();
        RenderFeatureManager.INSTANCE.onClientDisconnect();
        WorldFeatureManager.INSTANCE.onClientDisconnect();
        ItemFeatureManager.INSTANCE.onClientDisconnect();
        BlockFeatureManager.INSTANCE.onClientDisconnect();
        SpeedHandler.INSTANCE.onClientDisconnect();
        FlyHandler.INSTANCE.onClientDisconnect();
        GuiBlockerHandler.reset();
        AutoEscapeHandler.resetRuntimeState();
        AutoFishingHandler.INSTANCE.onClientDisconnect();
        KillTimerHandler.clearRuntimeState();
        KillAuraHandler.INSTANCE.onClientDisconnect();
    }
}
