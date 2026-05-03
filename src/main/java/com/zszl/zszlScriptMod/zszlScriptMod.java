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
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderWorldLastEvent;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.system.GlobalKeybindListener;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.utils.ClientPerformanceWarmupManager;
import com.zszl.zszlScriptMod.utils.HudTextScanner;
import com.zszl.zszlScriptMod.utils.HttpsCompat;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.WorldLoadSafety;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiInspectionManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
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
    private static boolean legacyRenderListenersRegistered;
    private static boolean modernRenderListenersRegistered;

    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MODID, "zszl_script"));
    private static final KeyMapping GUI_KEY = new KeyMapping("key.zszl_script.open_menu", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F, KEY_CATEGORY);

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
        ForgeCompatEventBridge forgeCompatEventBridge = new ForgeCompatEventBridge();
        MinecraftForge.EVENT_BUS.register(forgeCompatEventBridge);
        MinecraftForge.EVENT_BUS.register(GlobalEventListener.instance);
        ChatEventHandler.register();
        OverlayGuiHandler.register();
        MinecraftForge.EVENT_BUS.register(new GlobalKeybindListener());
        SimulatedKeyInputManager.register();
        MinecraftForge.EVENT_BUS.register(KillAuraHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(KillTimerHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AutoFollowHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AutoPickupHandler.INSTANCE);
        AutoFishingHandler.register();
        AutoEscapeHandler.register();
        MinecraftForge.EVENT_BUS.register(ConditionalExecutionHandler.INSTANCE);
        FlyHandler.register();
        MinecraftForge.EVENT_BUS.register(BlockReplacementHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WarehouseEventHandler.INSTANCE);
        MovementFeatureManager.register();
        BlockFeatureManager.register();
        MinecraftForge.EVENT_BUS.register(RenderFeatureManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ItemFeatureManager.INSTANCE);
        ItemFeatureManager.registerModernListeners();
        registerModernRenderListeners(forgeCompatEventBridge);
        registerLegacyRenderListeners();

        AutoEscapeHandler.loadConfig();
        SpeedHandler.loadConfig();
        KillTimerHandler.loadConfig();

        RegisterKeyMappingsEvent.getBus(FMLJavaModLoadingContext.get().getModBusGroup()).addListener(this::onRegisterKeyMappings);
    }

    private static void registerModernRenderListeners(ForgeCompatEventBridge forgeCompatEventBridge) {
        if (modernRenderListenersRegistered) {
            return;
        }
        modernRenderListenersRegistered = true;

        AddFramePassEvent.BUS.addListener(forgeCompatEventBridge::onAddFramePass);
        AddGuiOverlayLayersEvent.BUS.addListener(forgeCompatEventBridge::onAddGuiOverlayLayers);
        AddFramePassEvent.BUS.addListener(RenderFeatureManager.INSTANCE::onAddFramePass);
        AddGuiOverlayLayersEvent.BUS.addListener(RenderFeatureManager.INSTANCE::onAddGuiOverlayLayers);
    }

    private static void registerLegacyRenderListeners() {
        if (legacyRenderListenersRegistered) {
            return;
        }
        legacyRenderListenersRegistered = true;

        RenderGameOverlayEvent.Post.BUS.addListener(KillTimerHandler.INSTANCE::onRenderOverlay);
        RenderGameOverlayEvent.Pre.BUS.addListener(
                (java.util.function.Consumer<RenderGameOverlayEvent.Pre>) HudTextScanner.INSTANCE::onRenderOverlayPre);

        RenderWorldLastEvent.BUS.addListener(KillAuraHandler.INSTANCE::onRenderWorldLast);
        RenderWorldLastEvent.BUS.addListener(AutoFollowHandler.INSTANCE::onRenderWorldLast);
        RenderWorldLastEvent.BUS.addListener(AutoPickupHandler.INSTANCE::onRenderWorldLast);
        RenderWorldLastEvent.BUS.addListener(ConditionalExecutionHandler.INSTANCE::onRenderWorldLast);
        RenderWorldLastEvent.BUS.addListener(BlockReplacementHandler.INSTANCE::onRenderWorldLast);
        RenderWorldLastEvent.BUS.addListener(WarehouseEventHandler.INSTANCE::onRenderWorldLast);
        RenderWorldLastEvent.BUS.addListener(PathSequenceEventListener.instance::onRenderWorldLast);
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
        boolean automationDeferred = WorldLoadSafety.shouldDeferAutomation(mc);
        if (GUI_KEY.consumeClick()) {
            GuiInventory.toggleOverlayScreen();
        }
        if (mc.player == null) {
            return;
        }
        if (automationDeferred) {
            return;
        }
        AutoEatHandler.checkAutoEat(mc.player);
        AutoUseItemHandler.INSTANCE.tick();
        SpeedHandler.INSTANCE.tick(mc);
        MovementFeatureManager.tickClientPlayerFeatures(mc.player);
        WorldFeatureManager.INSTANCE.tick(mc);
        ItemFeatureManager.INSTANCE.tick(mc);
    }

    @SubscribeEvent
    public void onNetworkLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        WorldLoadSafety.onNetworkLogin();
        MiscFeatureManager.INSTANCE.onClientConnected();
        ClientPerformanceWarmupManager.warmupCurrentProfileAsync("network-login");
    }

    @SubscribeEvent
    public void onNetworkLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        WorldLoadSafety.onNetworkLogout();
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
