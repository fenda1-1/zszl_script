package com.zszl.zszlScriptMod.compat.legacy.net.minecraft.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class ItemStackHelper {

    private ItemStackHelper() {
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag), items);
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(output, items);
        tag.merge(output.buildResult());
        return tag;
    }
}

