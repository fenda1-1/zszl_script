package com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public class RenderGameOverlayEvent extends MutableEvent implements Cancellable {

    public enum ElementType {
        ALL,
        CROSSHAIRS
    }

    private final ElementType type;
    private final float partialTicks;
    private boolean canceled;

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

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public static class Pre extends RenderGameOverlayEvent {
        public static final CancellableEventBus<Pre> BUS = CancellableEventBus.create(Pre.class);

        public Pre(ElementType type, float partialTicks) {
            super(type, partialTicks);
        }
    }

    public static class Post extends RenderGameOverlayEvent {
        public static final EventBus<Post> BUS = EventBus.create(Post.class);

        public Post(ElementType type, float partialTicks) {
            super(type, partialTicks);
        }
    }
}
