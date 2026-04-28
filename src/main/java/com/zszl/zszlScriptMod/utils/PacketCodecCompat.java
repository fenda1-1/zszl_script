package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.zszlScriptMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ForgePayload;

import java.util.Arrays;

final class PacketCodecCompat {

    private PacketCodecCompat() {
    }

    static EncodedPacket encode(Packet<?> packet, boolean outbound) {
        ProtocolInfo<?> protocol = currentProtocolInfo(outbound);
        if (protocol == null) {
            throw new IllegalStateException("当前连接没有可用的" + (outbound ? "出站" : "入站") + "协议 codec");
        }

        ByteBuf buffer = Unpooled.buffer();
        try {
            encode(protocol, buffer, packet);

            ByteBuf fullView = buffer.duplicate();
            fullView.readerIndex(0);
            byte[] fullBytes = readRemaining(fullView);

            ByteBuf payloadView = Unpooled.wrappedBuffer(fullBytes);
            try {
                int packetId = readVarInt(payloadView);
                byte[] payloadBytes = readRemaining(payloadView);
                return new EncodedPacket(packetId, payloadBytes, fullBytes);
            } finally {
                payloadView.release();
            }
        } finally {
            buffer.release();
        }
    }

    static Packet<?> decodeStandardPacket(int packetId, byte[] payload, boolean outbound) {
        ProtocolInfo<?> protocol = currentProtocolInfo(outbound);
        if (protocol == null) {
            throw new IllegalStateException("当前连接没有可用的" + (outbound ? "出站" : "入站") + "协议 codec");
        }
        ByteBuf buffer = Unpooled.buffer();
        try {
            writeVarInt(buffer, packetId);
            buffer.writeBytes(payload == null ? new byte[0] : payload);
            Packet<?> packet = decode(protocol, buffer);
            if (buffer.isReadable()) {
                zszlScriptMod.LOGGER.debug("标准包 0x{} 解码后仍有 {} 字节未读取",
                        Integer.toHexString(packetId).toUpperCase(), buffer.readableBytes());
            }
            return packet;
        } finally {
            buffer.release();
        }
    }

    static ServerboundCustomPayloadPacket serverboundCustomPayload(Identifier identifier, byte[] payload) {
        return new ServerboundCustomPayloadPacket(forgePayload(identifier, payload));
    }

    static ClientboundCustomPayloadPacket clientboundCustomPayload(Identifier identifier, byte[] payload) {
        return new ClientboundCustomPayloadPacket(forgePayload(identifier, payload));
    }

    static byte[] extractCustomPayloadBytes(CustomPacketPayload payload, byte[] encodedPacketPayload) {
        byte[] forgeBytes = extractForgePayloadBytes(payload);
        if (forgeBytes != null) {
            return forgeBytes;
        }
        return stripCustomPayloadIdentifier(encodedPacketPayload);
    }

    private static byte[] extractForgePayloadBytes(CustomPacketPayload payload) {
        if (!(payload instanceof ForgePayload)) {
            return null;
        }
        ForgePayload forgePayload = (ForgePayload) payload;
        FriendlyByteBuf data = forgePayload.data();
        if (data != null) {
            ByteBuf copy = data.copy();
            try {
                return readRemaining(copy);
            } finally {
                copy.release();
            }
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            forgePayload.encoder().accept(buffer);
            return readRemaining(buffer.duplicate());
        } finally {
            buffer.release();
        }
    }

    private static CustomPacketPayload forgePayload(Identifier identifier, byte[] payload) {
        byte[] safePayload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        return ForgePayload.create(identifier, new FriendlyByteBuf(Unpooled.wrappedBuffer(safePayload)));
    }

    private static byte[] stripCustomPayloadIdentifier(byte[] encodedPacketPayload) {
        if (encodedPacketPayload == null || encodedPacketPayload.length == 0) {
            return new byte[0];
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(encodedPacketPayload));
        try {
            buffer.readIdentifier();
            return readRemaining(buffer);
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.debug("解析 CustomPayload 原始内容失败，回退为空 payload", t);
            return new byte[0];
        } finally {
            buffer.release();
        }
    }

    private static ProtocolInfo<?> currentProtocolInfo(boolean outbound) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            return null;
        }
        Connection connection = listener.getConnection();
        if (connection == null) {
            return null;
        }
        return outbound ? connection.getOutputboundProtocolInfo() : connection.getInboundProtocolInfo();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void encode(ProtocolInfo<?> protocol, ByteBuf buffer, Packet<?> packet) {
        StreamCodec codec = protocol.codec();
        codec.encode(buffer, packet);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Packet<?> decode(ProtocolInfo<?> protocol, ByteBuf buffer) {
        StreamCodec codec = protocol.codec();
        return (Packet<?>) codec.decode(buffer);
    }

    private static int readVarInt(ByteBuf buffer) {
        int value = 0;
        int byteCount = 0;
        byte currentByte;
        do {
            if (!buffer.isReadable()) {
                throw new IllegalArgumentException("VarInt 数据不完整");
            }
            currentByte = buffer.readByte();
            value |= (currentByte & 0x7F) << (byteCount * 7);
            byteCount++;
            if (byteCount > 5) {
                throw new IllegalArgumentException("VarInt 过长");
            }
        } while ((currentByte & 0x80) == 0x80);
        return value;
    }

    private static void writeVarInt(ByteBuf buffer, int value) {
        int remaining = value;
        while ((remaining & -128) != 0) {
            buffer.writeByte(remaining & 127 | 128);
            remaining >>>= 7;
        }
        buffer.writeByte(remaining);
    }

    private static byte[] readRemaining(ByteBuf buffer) {
        if (buffer == null || !buffer.isReadable()) {
            return new byte[0];
        }
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    static final class EncodedPacket {
        final int packetId;
        final byte[] payloadBytes;
        final byte[] fullPacketBytes;

        EncodedPacket(int packetId, byte[] payloadBytes, byte[] fullPacketBytes) {
            this.packetId = packetId;
            this.payloadBytes = payloadBytes == null ? new byte[0] : payloadBytes;
            this.fullPacketBytes = fullPacketBytes == null ? new byte[0] : fullPacketBytes;
        }
    }
}
