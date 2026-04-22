package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class RenderGameOverlayEvent extends Event {

    public enum ElementType {
        ALL,
        CROSSHAIRS
    }

    private final ElementType type;
    private final float partialTicks;

    protected RenderGameOverlayEvent(ElementType type, float partialTicks) {
        this.type = type;
        this.partialTicks = partialTicks;
    }

    public ElementType getType() {
        return type;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    @Cancelable
    public static class Pre extends RenderGameOverlayEvent {
        public Pre(ElementType type, float partialTicks) {
            super(type, partialTicks);
        }
    }

    public static class Post extends RenderGameOverlayEvent {
        public Post(ElementType type, float partialTicks) {
            super(type, partialTicks);
        }
    }
}

