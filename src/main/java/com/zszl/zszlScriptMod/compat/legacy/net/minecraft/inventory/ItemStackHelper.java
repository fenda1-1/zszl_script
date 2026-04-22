package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public final class ItemStackHelper {

    private ItemStackHelper() {
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(tag, items);
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        return ContainerHelper.saveAllItems(tag, items);
    }
}

