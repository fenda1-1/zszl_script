package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;

public class InventoryBasic extends SimpleContainer {

    private final Component displayName;

    public InventoryBasic(String title, boolean customName, int slotCount) {
        super(slotCount);
        this.displayName = Component.literal(title == null ? "" : title);
    }

    public Component getDisplayName() {
        return displayName;
    }
}

