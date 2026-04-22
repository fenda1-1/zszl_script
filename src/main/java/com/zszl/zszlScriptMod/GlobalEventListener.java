package com.zszl.zszlScriptMod;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent;
import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.handlers.GuiBlockerHandler;
import com.zszl.zszlScriptMod.listenersupport.PlayerIdleTriggerTracker;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GlobalEventListener {
    public static final GlobalEventListener instance = new GlobalEventListener();
    public static int timedMessageTickCounter = 0;

    private static final Random RANDOM = new Random();

    private final PlayerIdleTriggerTracker playerIdleTriggerTracker = new PlayerIdleTriggerTracker();
    private final Set<Integer> activeTriggerKeys = new HashSet<>();
    private int clientTickCounter = 0;
    private int timedMessageIndex = 0;

    private GlobalEventListener() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            timedMessageTickCounter = 0;
            timedMessageIndex = 0;
            clientTickCounter = 0;
            playerIdleTriggerTracker.reset();
            activeTriggerKeys.clear();
            return;
        }

        if (ModConfig.isMouseDetached && mc.mouseHandler != null && mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }

        clientTickCounter++;
        if (LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_PLAYER_IDLE)) {
            boolean playerDeadNow = mc.player.isDeadOrDying() || mc.player.getHealth() <= 0.0F;
            playerIdleTriggerTracker.update(mc, playerDeadNow, clientTickCounter);
        } else {
            playerIdleTriggerTracker.reset();
        }

        ChatOptimizationConfig config = ChatOptimizationConfig.INSTANCE;
        if (!config.enableTimedMessage || config.timedMessages == null || config.timedMessages.isEmpty()) {
            timedMessageTickCounter = 0;
            return;
        }

        timedMessageTickCounter++;
        if (timedMessageTickCounter < Math.max(1, config.timedMessageIntervalSeconds) * 20) {
            return;
        }

        List<String> validMessages = new ArrayList<>();
        for (String message : config.timedMessages) {
            if (message != null && !message.trim().isEmpty()) {
                validMessages.add(message);
            }
        }

        if (!validMessages.isEmpty()) {
            String messageToSend;
            if (config.timedMessageMode == ChatOptimizationConfig.TimedMessageMode.SEQUENTIAL) {
                if (timedMessageIndex >= validMessages.size()) {
                    timedMessageIndex = 0;
                }
                messageToSend = validMessages.get(timedMessageIndex);
                timedMessageIndex++;
            } else {
                messageToSend = validMessages.get(RANDOM.nextInt(validMessages.size()));
            }

            if (messageToSend != null && !messageToSend.trim().isEmpty()) {
                mc.player.connection.sendChat(messageToSend);
            }
        }

        timedMessageTickCounter = 0;
    }

    @SubscribeEvent
    public void onLegacyKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            activeTriggerKeys.clear();
            return;
        }

        int keyCode = Keyboard.getEventKey();
        boolean pressed = Keyboard.getEventKeyState();
        if (keyCode == Keyboard.KEY_NONE) {
            return;
        }

        if (!pressed) {
            activeTriggerKeys.remove(keyCode);
            return;
        }

        if (!activeTriggerKeys.add(keyCode)) {
            return;
        }

        if (!LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_KEY_INPUT)) {
            return;
        }

        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("keyCode", keyCode);
        triggerData.addProperty("keyName", Keyboard.getKeyName(keyCode));
        triggerData.addProperty("screen", mc.screen == null ? "" : mc.screen.getClass().getSimpleName());
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_KEY_INPUT, triggerData);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event != null && event.getGui() != null && GuiBlockerHandler.shouldBlockAndConsume(event.getGui())) {
            event.setCanceled(true);
        }
    }
}
