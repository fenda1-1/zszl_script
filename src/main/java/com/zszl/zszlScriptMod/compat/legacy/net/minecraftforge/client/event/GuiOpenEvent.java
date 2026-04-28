package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public class GuiOpenEvent extends MutableEvent implements Cancellable {
    public static final CancellableEventBus<GuiOpenEvent> BUS = CancellableEventBus.create(GuiOpenEvent.class);

    private GuiScreen gui;
    private boolean canceled;

    public GuiOpenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public GuiScreen getGui() {
        return gui;
    }

    public void setGui(GuiScreen gui) {
        this.gui = gui;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}



