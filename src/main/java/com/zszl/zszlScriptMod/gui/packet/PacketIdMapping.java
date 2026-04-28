package com.zszl.zszlScriptMod.gui.packet;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.ping.PingPacketTypes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
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

        Map<PacketType<?>, Integer> serverboundIds = collectPacketIds(GameProtocols.SERVERBOUND_TEMPLATE.details());
        Map<PacketType<?>, Integer> clientboundIds = collectPacketIds(GameProtocols.CLIENTBOUND_TEMPLATE.details());

        loadPacketTypes(GamePacketTypes.class, serverboundIds, clientboundIds);
        loadPacketTypes(CommonPacketTypes.class, serverboundIds, clientboundIds);
        loadPacketTypes(CookiePacketTypes.class, serverboundIds, clientboundIds);
        loadPacketTypes(PingPacketTypes.class, serverboundIds, clientboundIds);
    }

    public static Class<? extends Packet<?>> getClassById(int id) {
        return SERVERBOUND_PACKETS.get(id);
    }

    public static Class<? extends Packet<?>> getClientboundClassById(int id) {
        return CLIENTBOUND_PACKETS.get(id);
    }

    private static Map<PacketType<?>, Integer> collectPacketIds(ProtocolInfo.Details details) {
        Map<PacketType<?>, Integer> ids = new HashMap<>();
        if (details != null) {
            details.listPackets((packetType, packetId) -> ids.put(packetType, packetId));
        }
        return ids;
    }

    private static void loadPacketTypes(Class<?> holder, Map<PacketType<?>, Integer> serverboundIds,
            Map<PacketType<?>, Integer> clientboundIds) {
        for (Field field : holder.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !PacketType.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                PacketType<?> packetType = (PacketType<?>) field.get(null);
                Class<? extends Packet<?>> packetClass = resolvePacketClass(field);
                if (packetType == null || packetClass == null) {
                    continue;
                }
                Integer packetId = packetType.flow() == PacketFlow.SERVERBOUND
                        ? serverboundIds.get(packetType)
                        : clientboundIds.get(packetType);
                if (packetId == null) {
                    continue;
                }
                if (packetType.flow() == PacketFlow.SERVERBOUND) {
                    SERVERBOUND_PACKETS.put(packetId, packetClass);
                } else {
                    CLIENTBOUND_PACKETS.put(packetId, packetClass);
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.debug("读取 1.21.11 数据包 ID 映射失败: {}.{}", holder.getName(),
                        field.getName(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Packet<?>> resolvePacketClass(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        Type packetArg = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        Class<?> rawClass = null;
        if (packetArg instanceof Class<?>) {
            rawClass = (Class<?>) packetArg;
        } else if (packetArg instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) packetArg).getRawType();
            if (rawType instanceof Class<?>) {
                rawClass = (Class<?>) rawType;
            }
        }
        if (rawClass == null || !Packet.class.isAssignableFrom(rawClass)) {
            return null;
        }
        return (Class<? extends Packet<?>>) rawClass;
    }
}

