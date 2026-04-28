package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.network;

import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;

public final class FMLNetworkEvent {

    private FMLNetworkEvent() {
    }

    public static class ClientConnectedToServerEvent extends MutableEvent {
        public static final EventBus<ClientConnectedToServerEvent> BUS = EventBus.create(ClientConnectedToServerEvent.class);
    }

    public static class ClientDisconnectionFromServerEvent extends MutableEvent {
        public static final EventBus<ClientDisconnectionFromServerEvent> BUS = EventBus.create(ClientDisconnectionFromServerEvent.class);
    }
}
