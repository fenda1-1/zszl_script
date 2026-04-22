/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.launch.mixins;

import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.PacketEvent;
import com.zszl.zszlScriptMod.shadowbaritone.api.event.events.type.EventState;
import com.zszl.zszlScriptMod.shadowbaritone.launch.util.ReflectionHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/6/2018
 */
@Mixin(Connection.class)
public class MixinNetworkManager {

    @Inject(
            method = "sendPacket",
            at = @At("HEAD")
    )
    private void preDispatchPacket(Packet<?> packet, PacketSendListener packetSendListener, CallbackInfo ci) {
        PacketFlow receiving = ReflectionHelper.getField(this, "receiving", "f_129466_");
        if (receiving != PacketFlow.CLIENTBOUND) {
            return;
        }

        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
                ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((Connection) (Object) this, EventState.PRE, packet));
            }
        }
    }

    @Inject(
            method = "sendPacket",
            at = @At("RETURN")
    )
    private void postDispatchPacket(Packet<?> packet, PacketSendListener packetSendListener, CallbackInfo ci) {
        PacketFlow receiving = ReflectionHelper.getField(this, "receiving", "f_129466_");
        if (receiving != PacketFlow.CLIENTBOUND) {
            return;
        }

        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
                ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((Connection) (Object) this, EventState.POST, packet));
            }
        }
    }

    @Inject(
            method = "channelRead0",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/network/Connection.genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
            )
    )
    private void preProcessPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketFlow receiving = ReflectionHelper.getField(this, "receiving", "f_129466_");
        if (receiving != PacketFlow.CLIENTBOUND) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
                ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((Connection) (Object) this, EventState.PRE, packet));
            }
        }
    }

    @Inject(
            method = "channelRead0",
            at = @At("RETURN")
    )
    private void postProcessPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        Channel channel = ReflectionHelper.getField(this, "channel", "f_129468_");
        PacketFlow receiving = ReflectionHelper.getField(this, "receiving", "f_129466_");
        if (!channel.isOpen() || receiving != PacketFlow.CLIENTBOUND) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().connection.getConnection() == (Connection) (Object) this) {
                ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((Connection) (Object) this, EventState.POST, packet));
            }
        }
    }
}

