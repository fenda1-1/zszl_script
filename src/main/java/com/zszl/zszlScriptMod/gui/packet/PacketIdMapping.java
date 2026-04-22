package com.zszl.zszlScriptMod.gui.packet;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketIdMapping {

    private static final Map<Integer, Class<? extends Packet<?>>> SERVERBOUND_PACKETS = new ConcurrentHashMap<>();
    private static final Map<Integer, Class<? extends Packet<?>>> CLIENTBOUND_PACKETS = new ConcurrentHashMap<>();

    static {
        reloadMappings();
    }

    private PacketIdMapping() {
    }

    public static synchronized void reloadMappings() {
        SERVERBOUND_PACKETS.clear();
        CLIENTBOUND_PACKETS.clear();
        SERVERBOUND_PACKETS.putAll(ConnectionProtocol.PLAY.getPacketsByIds(PacketFlow.SERVERBOUND));
        CLIENTBOUND_PACKETS.putAll(ConnectionProtocol.PLAY.getPacketsByIds(PacketFlow.CLIENTBOUND));
    }

    public static Class<? extends Packet<?>> getClassById(int id) {
        return SERVERBOUND_PACKETS.get(id);
    }

    public static Class<? extends Packet<?>> getClientboundClassById(int id) {
        return CLIENTBOUND_PACKETS.get(id);
    }
}

