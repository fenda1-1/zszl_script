package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public class MouseEvent extends MutableEvent implements Cancellable {
    public static final CancellableEventBus<MouseEvent> BUS = CancellableEventBus.create(MouseEvent.class);

    private final int button;
    private final boolean buttonstate;
    private final int dwheel;
    private boolean canceled;

    public MouseEvent(int button, boolean buttonstate, int dwheel) {
        this.button = button;
        this.buttonstate = buttonstate;
        this.dwheel = dwheel;
    }

    public int getButton() {
        return button;
    }

    public boolean isButtonstate() {
        return buttonstate;
    }

    public int getDwheel() {
        return dwheel;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
