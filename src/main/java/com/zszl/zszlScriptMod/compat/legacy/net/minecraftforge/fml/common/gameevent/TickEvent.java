package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.relauncher.Side;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;

public class TickEvent extends MutableEvent {

    public enum Phase {
        START,
        END
    }

    public enum Type {
        IN,
        OUT
    }

    public final Phase phase;
    public final Side side;

    protected TickEvent(Phase phase, Side side) {
        this.phase = phase;
        this.side = side;
    }

    public static class ClientTickEvent extends TickEvent {
        public static final EventBus<ClientTickEvent> BUS = EventBus.create(ClientTickEvent.class);

        public ClientTickEvent(Phase phase) {
            super(phase, Side.CLIENT);
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public static final EventBus<PlayerTickEvent> BUS = EventBus.create(PlayerTickEvent.class);

        public final Player player;

        public PlayerTickEvent(Phase phase, Player player, Side side) {
            super(phase, side);
            this.player = player;
        }
    }
}
