package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.network.internal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class FMLProxyPacket implements Packet<PacketListener> {

    private final FriendlyByteBuf payload;
    private final String channel;

    public FMLProxyPacket(FriendlyByteBuf payload, String channel) {
        this.payload = payload;
        this.channel = channel;
    }

    public String channel() {
        return channel;
    }

    public ByteBuf payload() {
        return payload;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(payload.copy());
    }

    @Override
    public void handle(PacketListener listener) {
    }
}

