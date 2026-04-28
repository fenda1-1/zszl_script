package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.network.internal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;

public class FMLProxyPacket {

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

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(payload.copy());
    }

    public void handle(Object listener) {
    }
}

