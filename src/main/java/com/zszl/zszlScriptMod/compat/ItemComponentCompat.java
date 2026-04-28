package com.zszl.zszlScriptMod.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

public final class ItemComponentCompat {

    private ItemComponentCompat() {
    }

    public static boolean isSword(ItemStack stack) {
        return isUsable(stack) && stack.is(ItemTags.SWORDS);
    }

    public static boolean isAxe(ItemStack stack) {
        return isUsable(stack) && stack.is(ItemTags.AXES);
    }

    public static boolean isPickaxe(ItemStack stack) {
        return isUsable(stack) && stack.is(ItemTags.PICKAXES);
    }

    public static boolean isDiggerTool(ItemStack stack) {
        return isUsable(stack)
                && (stack.has(DataComponents.TOOL)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES));
    }

    public static boolean isWeapon(ItemStack stack) {
        return isUsable(stack) && (stack.has(DataComponents.WEAPON) || isSword(stack) || isAxe(stack));
    }

    public static boolean isEdible(ItemStack stack) {
        return isUsable(stack) && stack.has(DataComponents.FOOD);
    }

    public static boolean isArmor(ItemStack stack) {
        if (!isUsable(stack)) {
            return false;
        }
        return stack.is(ItemTags.HEAD_ARMOR)
                || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR)
                || stack.is(ItemTags.FOOT_ARMOR);
    }

    public static EquipmentSlot getEquipmentSlot(ItemStack stack) {
        if (!isUsable(stack)) {
            return null;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null ? null : equippable.slot();
    }

    public static double getArmorScore(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlot(stack);
        if (slot == null || !isArmor(stack)) {
            return -1.0D;
        }
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double armor = modifiers.compute(Attributes.ARMOR, 0.0D, slot);
        double toughness = modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0D, slot);
        return armor * 10.0D + toughness * 1.5D;
    }

    public static int getMaterialCost(ItemStack stack) {
        if (!isUsable(stack)) {
            return -1;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id == null ? "" : id.getPath();
        if (path.startsWith("wooden_") || path.startsWith("golden_")) {
            return 0;
        }
        if (path.startsWith("stone_")) {
            return 1;
        }
        if (path.startsWith("copper_")) {
            return 2;
        }
        if (path.startsWith("iron_")) {
            return 3;
        }
        if (path.startsWith("diamond_")) {
            return 4;
        }
        if (path.startsWith("netherite_")) {
            return 5;
        }
        return stack.has(DataComponents.TOOL) ? 1 : -1;
    }

    public static String getDescriptionId(ItemStack stack) {
        if (!isUsable(stack)) {
            return "minecraft:air";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    public static CompoundTag getCustomDataTag(ItemStack stack) {
        if (!isUsable(stack)) {
            return new CompoundTag();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static boolean hasCustomDataTag(ItemStack stack) {
        return !getCustomDataTag(stack).isEmpty();
    }

    public static void setCustomDataTag(ItemStack stack, CompoundTag tag) {
        if (!isUsable(stack)) {
            return;
        }
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }

    public static String getComponentKey(ItemStack stack) {
        return isUsable(stack) ? String.valueOf(stack.getComponents()) : "";
    }

    public static NonNullList<ItemStack> copyContainerItems(ItemStack stack, int size) {
        NonNullList<ItemStack> items = NonNullList.withSize(Math.max(0, size), ItemStack.EMPTY);
        if (!isUsable(stack)) {
            return items;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(items);
        }
        return items;
    }

    public static String getDebugDataText(ItemStack stack) {
        if (!isUsable(stack)) {
            return "{}";
        }
        CompoundTag customData = getCustomDataTag(stack);
        String componentText = getComponentKey(stack);
        return "customData=" + (customData.isEmpty() ? "{}" : customData.toString())
                + ", components=" + componentText;
    }

    public static void setCustomName(ItemStack stack, String name) {
        if (!isUsable(stack)) {
            return;
        }
        String safeName = name == null ? "" : name.trim();
        if (safeName.isEmpty()) {
            resetCustomName(stack);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(safeName));
        }
    }

    public static void resetCustomName(ItemStack stack) {
        if (isUsable(stack)) {
            stack.remove(DataComponents.CUSTOM_NAME);
        }
    }

    private static boolean isUsable(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }
}
