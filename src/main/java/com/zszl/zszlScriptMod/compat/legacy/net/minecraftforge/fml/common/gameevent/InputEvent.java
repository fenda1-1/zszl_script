package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public class InputEvent extends MutableEvent implements Cancellable {
    private boolean canceled;

    public static class KeyInputEvent extends InputEvent {
        public static final CancellableEventBus<KeyInputEvent> BUS = CancellableEventBus.create(KeyInputEvent.class);
    }

    public static class MouseInputEvent extends InputEvent {
        public static final CancellableEventBus<MouseInputEvent> BUS = CancellableEventBus.create(MouseInputEvent.class);

        private final GuiScreen gui;
        private final int button;

        public MouseInputEvent(GuiScreen gui, int button) {
            this.gui = gui;
            this.button = button;
        }

        public GuiScreen getGui() {
            return gui;
        }

        public int getButton() {
            return button;
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
