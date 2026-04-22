package com.zszl.zszlScriptMod.handlers;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.path.node.NodeTriggerManager;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.zszl.zszlScriptMod.shadowbaritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class ChatEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientChat(ClientChatEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }
        String raw = event.getMessage().trim();
        if (!raw.startsWith("!") && !raw.startsWith(FORCE_COMMAND_PREFIX)) {
            return;
        }

        boolean executed = InternalBaritoneBridge.executeRawChatLikeCommand(raw);
        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        if (!executed && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§c[Baritone] 命令未执行: §f" + raw));
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
            return Component.Serializer.toJson(component);
        } catch (Exception ignored) {
            return component == null ? "" : component.getString();
        }
    }
}
