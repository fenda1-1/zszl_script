package com.zszl.zszlScriptMod.handlers;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.zszl.zszlScriptMod.path.node.NodeTriggerManager;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

import static com.zszl.zszlScriptMod.shadowbaritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class ChatEventHandler {

    public static void register() {
        ClientChatEvent.BUS.addListener(Priority.HIGHEST, ChatEventHandler::onClientChat);
    }

    public static void onClientChat(ClientChatEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }
        String raw = event.getMessage().trim();
        if (!raw.startsWith("!") && !raw.startsWith(FORCE_COMMAND_PREFIX)) {
            return;
        }

        boolean executed = InternalBaritoneBridge.executeRawChatLikeCommand(raw);
        event.setMessage("");

        Minecraft mc = Minecraft.getInstance();
        if (!executed && mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§c[Baritone] 命令未执行: §f" + raw), false);
        }
    }

    public static void triggerDisplayedChatMessage(Component originalComponent, Component displayedComponent, boolean system) {
        String rawMessage = normalizeTriggerMessage(originalComponent == null ? null : originalComponent.getString());
        String displayedMessage = normalizeTriggerMessage(displayedComponent == null ? null : displayedComponent.getString());
        if (rawMessage.isEmpty() && displayedMessage.isEmpty()) {
            return;
        }

        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("message", rawMessage);
        triggerData.addProperty("displayedMessage", displayedMessage);
        triggerData.addProperty("formatted", displayedComponent == null ? "" : safeSerialized(displayedComponent));
        triggerData.addProperty("source", system ? "system" : "chat");
        triggerData.addProperty("chatLineId", system ? 1 : 0);

        NodeTriggerManager.trigger(NodeTriggerManager.TRIGGER_CHAT, triggerData);
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_CHAT, triggerData);
    }

    private static String normalizeTriggerMessage(String text) {
        return text == null ? "" : text.trim();
    }

    private static String safeSerialized(Component component) {
        try {
            return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component)
                    .result()
                    .map(Object::toString)
                    .orElse(component == null ? "" : component.getString());
        } catch (Exception ignored) {
            return component == null ? "" : component.getString();
        }
    }
}
