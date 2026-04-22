package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public abstract class GuiScreenEvent extends Event {

    private final GuiScreen gui;

    protected GuiScreenEvent(GuiScreen gui) {
        this.gui = gui;
    }

    public GuiScreen getGui() {
        return gui;
    }

    public abstract static class InitGuiEvent extends GuiScreenEvent {
        private final List<GuiButton> buttonList;

        protected InitGuiEvent(GuiScreen gui, List<GuiButton> buttonList) {
            super(gui);
            this.buttonList = buttonList;
        }

        public List<GuiButton> getButtonList() {
            return buttonList;
        }

        public static class Post extends InitGuiEvent {
            public Post(GuiScreen gui, List<GuiButton> buttonList) {
                super(gui, buttonList);
            }
        }
    }

    public abstract static class DrawScreenEvent extends GuiScreenEvent {
        private final int mouseX;
        private final int mouseY;
        private final float renderPartialTicks;

        protected DrawScreenEvent(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
            super(gui);
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.renderPartialTicks = renderPartialTicks;
        }

        public int getMouseX() {
            return mouseX;
        }

        public int getMouseY() {
            return mouseY;
        }

        public float getRenderPartialTicks() {
            return renderPartialTicks;
        }

        public static class Pre extends DrawScreenEvent {
            public Pre(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
                super(gui, mouseX, mouseY, renderPartialTicks);
            }
        }

        public static class Post extends DrawScreenEvent {
            public Post(GuiScreen gui, int mouseX, int mouseY, float renderPartialTicks) {
                super(gui, mouseX, mouseY, renderPartialTicks);
            }
        }
    }

    public abstract static class ActionPerformedEvent extends GuiScreenEvent {
        private final GuiButton button;

        protected ActionPerformedEvent(GuiScreen gui, GuiButton button) {
            super(gui);
            this.button = button;
        }

        public GuiButton getButton() {
            return button;
        }

        @Cancelable
        public static class Pre extends ActionPerformedEvent {
            public Pre(GuiScreen gui, GuiButton button) {
                super(gui, button);
            }
        }
    }

    public abstract static class MouseInputEvent extends GuiScreenEvent {
        protected MouseInputEvent(GuiScreen gui) {
            super(gui);
        }

        @Cancelable
        public static class Pre extends MouseInputEvent {
            public Pre(GuiScreen gui) {
                super(gui);
            }
        }

        public static class Post extends MouseInputEvent {
            public Post(GuiScreen gui) {
                super(gui);
            }
        }
    }

    public abstract static class KeyboardInputEvent extends GuiScreenEvent {
        protected KeyboardInputEvent(GuiScreen gui) {
            super(gui);
        }

        @Cancelable
        public static class Pre extends KeyboardInputEvent {
            public Pre(GuiScreen gui) {
                super(gui);
            }
        }
    }
}



