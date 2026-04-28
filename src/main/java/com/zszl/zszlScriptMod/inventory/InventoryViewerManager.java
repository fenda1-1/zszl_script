package com.zszl.zszlScriptMod.inventory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryViewerManager {

    private static final int VIEWER_SIZE = 54;
    private static final int ARMOR_START_SLOT = 45;
    private static final int OFFHAND_SLOT = 49;
    private static final SimpleContainer COPIED_INVENTORY = new SimpleContainer(VIEWER_SIZE);
    private static String lastCopiedPlayerName = "";
    private static boolean hasCopiedInventoryData = false;

    private InventoryViewerManager() {
    }

    public static Container getCopiedInventory() {
        return COPIED_INVENTORY;
    }

    public static NonNullList<ItemStack> snapshotCopiedInventory() {
        NonNullList<ItemStack> snapshot = NonNullList.withSize(VIEWER_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < VIEWER_SIZE; i++) {
            snapshot.set(i, copyOrEmpty(COPIED_INVENTORY.getItem(i)));
        }
        return snapshot;
    }

    public static String getLastCopiedPlayerName() {
        return lastCopiedPlayerName == null ? "" : lastCopiedPlayerName;
    }

    public static boolean hasCopiedInventoryData() {
        return hasCopiedInventoryData;
    }

    public static void copyInventoryFromTarget() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) {
            return;
        }

        Entity targetEntity = mc.crosshairPickEntity;
        if (!(targetEntity instanceof Player)) {
            localPlayer.displayClientMessage(Component.literal("[物品查看器] 准星未对准任何玩家！")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        Player targetPlayer = (Player) targetEntity;
        clearCopiedInventory();
        lastCopiedPlayerName = targetPlayer.getName().getString();

        for (int i = 0; i < targetPlayer.getInventory().getNonEquipmentItems().size() && i < VIEWER_SIZE; i++) {
            COPIED_INVENTORY.setItem(i, copyOrEmpty(targetPlayer.getInventory().getNonEquipmentItems().get(i)));
        }

        for (int i = 0; i < 4; i++) {
            int targetSlot = ARMOR_START_SLOT + (3 - i);
            if (targetSlot >= 0 && targetSlot < VIEWER_SIZE) {
                COPIED_INVENTORY.setItem(targetSlot, copyOrEmpty(targetPlayer.getInventory().getItem(36 + i)));
            }
        }

        if (OFFHAND_SLOT < VIEWER_SIZE) {
            COPIED_INVENTORY.setItem(OFFHAND_SLOT, copyOrEmpty(targetPlayer.getInventory().getItem(40)));
        }
        hasCopiedInventoryData = true;

        localPlayer.displayClientMessage(Component.literal("[物品查看器] 已成功复制玩家 " + targetPlayer.getName().getString() + " 的物品栏。")
                .withStyle(ChatFormatting.GREEN), false);
    }

    private static void clearCopiedInventory() {
        hasCopiedInventoryData = false;
        for (int i = 0; i < VIEWER_SIZE; i++) {
            COPIED_INVENTORY.setItem(i, ItemStack.EMPTY);
        }
    }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }
}
