package com.zszl.zszlScriptMod.gui.dungeon;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChestViewerContainer {

    public static final class ReadOnlySlot {
        private final Container inventory;
        private final int index;
        private final int x;
        private final int y;

        private ReadOnlySlot(Container inventory, int index, int x, int y) {
            this.inventory = inventory;
            this.index = index;
            this.x = x;
            this.y = y;
        }

        public ItemStack getItem() {
            return inventory == null ? ItemStack.EMPTY : inventory.getItem(index);
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    private final Container playerInventory;
    private final Container chestInventory;
    private final int inventoryRows;
    private final List<ReadOnlySlot> slots = new ArrayList<>();

    public ChestViewerContainer(Container playerInventory, Container chestInventory) {
        this.playerInventory = playerInventory;
        this.chestInventory = chestInventory;
        this.inventoryRows = Math.max(1, chestInventory == null ? 1 : Math.max(1, chestInventory.getContainerSize() / 9));

        for (int row = 0; row < inventoryRows; ++row) {
            for (int col = 0; col < 9; ++col) {
                slots.add(new ReadOnlySlot(chestInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        int yOffset = (inventoryRows - 4) * 18;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                slots.add(new ReadOnlySlot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 103 + row * 18 + yOffset));
            }
        }

        for (int col = 0; col < 9; ++col) {
            slots.add(new ReadOnlySlot(playerInventory, col, 8 + col * 18, 161 + yOffset));
        }
    }

    public Container getPlayerInventory() {
        return playerInventory;
    }

    public Container getChestInventory() {
        return chestInventory;
    }

    public int getInventoryRows() {
        return inventoryRows;
    }

    public List<ReadOnlySlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public ReadOnlySlot getHoveredSlot(int mouseX, int mouseY) {
        for (ReadOnlySlot slot : slots) {
            if (slot.isMouseOver(mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }
}
