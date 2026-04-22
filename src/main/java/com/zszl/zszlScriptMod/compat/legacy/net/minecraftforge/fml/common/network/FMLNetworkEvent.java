package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.network;

import net.minecraftforge.eventbus.api.Event;

public final class FMLNetworkEvent {

    private FMLNetworkEvent() {
    }

    public static class ClientConnectedToServerEvent extends Event {
    }

    public static class ClientDisconnectionFromServerEvent extends Event {
    }
}

