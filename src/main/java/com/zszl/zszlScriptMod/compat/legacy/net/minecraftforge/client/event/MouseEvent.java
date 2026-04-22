package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class MouseEvent extends Event {
    private final int button;
    private final boolean buttonstate;
    private final int dwheel;

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
}

