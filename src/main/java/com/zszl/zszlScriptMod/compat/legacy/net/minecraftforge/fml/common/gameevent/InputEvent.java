package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;

public class InputEvent extends Event {

    @Cancelable
    public static class KeyInputEvent extends InputEvent {
    }

    @Cancelable
    public static class MouseInputEvent extends InputEvent {
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
}



