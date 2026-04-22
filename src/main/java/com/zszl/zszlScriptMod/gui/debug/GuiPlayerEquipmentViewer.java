package com.zszl.zszlScriptMod.gui.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.nbt.GuiNBTAdvanced;
import com.zszl.zszlScriptMod.inventory.InventoryViewerManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public class GuiPlayerEquipmentViewer extends ThemedGuiScreen {

    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final int INVENTORY_ROWS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_RENDER_SIZE = 16;
    private static final int VIEWER_SLOT_COUNT = 54;
    private static final int PLAYER_SLOT_COUNT = 36;
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 114 + INVENTORY_ROWS * 18;

    private final NonNullList<ItemStack> viewerSlots = NonNullList.withSize(VIEWER_SLOT_COUNT, ItemStack.EMPTY);
    private final NonNullList<ItemStack> playerPreviewSlots = NonNullList.withSize(PLAYER_SLOT_COUNT, ItemStack.EMPTY);

    private ItemStack cursorStack = ItemStack.EMPTY;
    private DisplaySlot hoveredSlot;
    private int guiLeft;
    private int guiTop;
    private String targetName = "";
    private String playerInventoryName = "";
    private boolean hasCopiedInventoryData;

    private static final class DisplaySlot {
        private final boolean viewerSlot;
        private final int index;
        private final int x;
        private final int y;

        private DisplaySlot(boolean viewerSlot, int index, int x, int y) {
            this.viewerSlot = viewerSlot;
            this.index = index;
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public void initGui() {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        reloadSnapshots();
    }

    private void reloadSnapshots() {
        NonNullList<ItemStack> copied = InventoryViewerManager.snapshotCopiedInventory();
        for (int i = 0; i < VIEWER_SLOT_COUNT; i++) {
            this.viewerSlots.set(i, normalize(copyOrEmpty(copied.get(i))));
        }

        this.targetName = InventoryViewerManager.getLastCopiedPlayerName();
        this.hasCopiedInventoryData = InventoryViewerManager.hasCopiedInventoryData();
        this.playerInventoryName = this.mc != null && this.mc.player != null
                ? this.mc.player.getInventory().getDisplayName().getString()
                : I18n.format("container.inventory");

        if (this.mc == null || this.mc.player == null) {
            for (int i = 0; i < PLAYER_SLOT_COUNT; i++) {
                this.playerPreviewSlots.set(i, ItemStack.EMPTY);
            }
            return;
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int previewIndex = row * 9 + col;
                int inventoryIndex = col + row * 9 + 9;
                this.playerPreviewSlots.set(previewIndex,
                        copyOrEmpty(this.mc.player.getInventory().items.get(inventoryIndex)));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.playerPreviewSlots.set(27 + col, copyOrEmpty(this.mc.player.getInventory().items.get(col)));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_E) {
            this.mc.setScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_F && this.hoveredSlot != null) {
            ItemStack hoveredStack = getSlotStack(this.hoveredSlot);
            if (!hoveredStack.isEmpty()) {
                this.mc.setScreen(new GuiNBTAdvanced(this, hoveredStack));
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        DisplaySlot clickedSlot = findSlot(mouseX, mouseY);
        if (clickedSlot != null) {
            handleSlotClick(clickedSlot, mouseButton);
            return;
        }

        if (mouseButton == 0 && !this.cursorStack.isEmpty()) {
            this.cursorStack = ItemStack.EMPTY;
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleSlotClick(DisplaySlot slot, int mouseButton) {
        if (slot == null) {
            return;
        }

        if (mouseButton == 2) {
            ItemStack stack = getSlotStack(slot);
            if (!stack.isEmpty()) {
                this.cursorStack = stack.copy();
            }
            return;
        }

        if (isShiftKeyDown() && mouseButton == 0) {
            quickMove(slot);
            return;
        }

        if (mouseButton == 1) {
            rightClickSlot(slot);
            return;
        }

        leftClickSlot(slot);
    }

    private void leftClickSlot(DisplaySlot slot) {
        ItemStack slotStack = getSlotStack(slot);
        if (this.cursorStack.isEmpty()) {
            if (!slotStack.isEmpty()) {
                this.cursorStack = slotStack.copy();
                setSlotStack(slot, ItemStack.EMPTY);
            }
            return;
        }

        if (slotStack.isEmpty()) {
            setSlotStack(slot, this.cursorStack.copy());
            this.cursorStack = ItemStack.EMPTY;
            return;
        }

        if (canStack(this.cursorStack, slotStack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
            int moveAmount = Math.min(this.cursorStack.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
            ItemStack merged = slotStack.copy();
            merged.grow(moveAmount);
            setSlotStack(slot, merged);
            this.cursorStack.shrink(moveAmount);
            this.cursorStack = normalize(this.cursorStack);
            return;
        }

        setSlotStack(slot, this.cursorStack.copy());
        this.cursorStack = slotStack.copy();
    }

    private void rightClickSlot(DisplaySlot slot) {
        ItemStack slotStack = getSlotStack(slot);

        if (this.cursorStack.isEmpty()) {
            if (slotStack.isEmpty()) {
                return;
            }
            int pickupAmount = (slotStack.getCount() + 1) / 2;
            ItemStack taken = slotStack.copy();
            taken.setCount(pickupAmount);
            ItemStack remaining = slotStack.copy();
            remaining.shrink(pickupAmount);
            this.cursorStack = normalize(taken);
            setSlotStack(slot, normalize(remaining));
            return;
        }

        if (slotStack.isEmpty()) {
            ItemStack placed = this.cursorStack.copy();
            placed.setCount(1);
            setSlotStack(slot, placed);
            this.cursorStack.shrink(1);
            this.cursorStack = normalize(this.cursorStack);
            return;
        }

        if (canStack(this.cursorStack, slotStack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
            ItemStack merged = slotStack.copy();
            merged.grow(1);
            setSlotStack(slot, merged);
            this.cursorStack.shrink(1);
            this.cursorStack = normalize(this.cursorStack);
            return;
        }

        setSlotStack(slot, this.cursorStack.copy());
        this.cursorStack = slotStack.copy();
    }

    private void quickMove(DisplaySlot slot) {
        ItemStack moving = getSlotStack(slot);
        if (moving.isEmpty()) {
            return;
        }

        ItemStack remaining = moving.copy();
        boolean moved = slot.viewerSlot ? mergeInto(this.playerPreviewSlots, remaining) : mergeInto(this.viewerSlots, remaining);
        if (!moved) {
            return;
        }
        setSlotStack(slot, normalize(remaining));
    }

    private boolean mergeInto(NonNullList<ItemStack> targetSlots, ItemStack moving) {
        if (moving.isEmpty()) {
            return false;
        }

        int originalCount = moving.getCount();

        for (int i = 0; i < targetSlots.size(); i++) {
            ItemStack target = targetSlots.get(i);
            if (target.isEmpty() || !canStack(moving, target) || target.getCount() >= target.getMaxStackSize()) {
                continue;
            }

            int moveAmount = Math.min(moving.getCount(), target.getMaxStackSize() - target.getCount());
            if (moveAmount <= 0) {
                continue;
            }
            ItemStack merged = target.copy();
            merged.grow(moveAmount);
            targetSlots.set(i, merged);
            moving.shrink(moveAmount);
            if (moving.isEmpty()) {
                return true;
            }
        }

        for (int i = 0; i < targetSlots.size(); i++) {
            if (!targetSlots.get(i).isEmpty()) {
                continue;
            }
            targetSlots.set(i, moving.copy());
            moving.setCount(0);
            return true;
        }

        return moving.getCount() != originalCount;
    }

    private ItemStack getSlotStack(DisplaySlot slot) {
        if (slot == null) {
            return ItemStack.EMPTY;
        }
        return slot.viewerSlot ? this.viewerSlots.get(slot.index) : this.playerPreviewSlots.get(slot.index);
    }

    private void setSlotStack(DisplaySlot slot, ItemStack stack) {
        ItemStack normalized = normalize(stack);
        if (slot.viewerSlot) {
            this.viewerSlots.set(slot.index, normalized);
        } else {
            this.playerPreviewSlots.set(slot.index, normalized);
        }
    }

    private DisplaySlot findSlot(int mouseX, int mouseY) {
        for (int i = 0; i < VIEWER_SLOT_COUNT; i++) {
            int slotX = this.guiLeft + 8 + (i % 9) * SLOT_SIZE;
            int slotY = this.guiTop + 18 + (i / 9) * SLOT_SIZE;
            if (isPointInsideSlot(mouseX, mouseY, slotX, slotY)) {
                return new DisplaySlot(true, i, slotX, slotY);
            }
        }

        int playerYOffset = (INVENTORY_ROWS - 4) * 18;
        for (int i = 0; i < 27; i++) {
            int slotX = this.guiLeft + 8 + (i % 9) * SLOT_SIZE;
            int slotY = this.guiTop + 103 + (i / 9) * SLOT_SIZE + playerYOffset;
            if (isPointInsideSlot(mouseX, mouseY, slotX, slotY)) {
                return new DisplaySlot(false, i, slotX, slotY);
            }
        }
        for (int i = 0; i < 9; i++) {
            int slotX = this.guiLeft + 8 + i * SLOT_SIZE;
            int slotY = this.guiTop + 161 + playerYOffset;
            if (isPointInsideSlot(mouseX, mouseY, slotX, slotY)) {
                return new DisplaySlot(false, 27 + i, slotX, slotY);
            }
        }
        return null;
    }

    private boolean isPointInsideSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_RENDER_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_RENDER_SIZE;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;
        this.hoveredSlot = findSlot(mouseX, mouseY);

        drawDefaultBackground();

        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(CHEST_GUI_TEXTURE, this.guiLeft, this.guiTop, 0, 0.0F, 0.0F, GUI_WIDTH, INVENTORY_ROWS * 18 + 17, 256, 256);
        graphics.blit(CHEST_GUI_TEXTURE, this.guiLeft, this.guiTop + INVENTORY_ROWS * 18 + 17, 0, 0.0F, 126.0F, GUI_WIDTH, 96, 256, 256);

        String title = I18n.format("gui.inv_viewer.title");
        drawString(this.fontRenderer, title, this.guiLeft + 8, this.guiTop + 6, GuiTheme.resolveTextColor(title, 0x404040));

        if (this.targetName != null && !this.targetName.trim().isEmpty()) {
            String targetLabel = "目标: " + this.targetName.trim();
            int maxWidth = 82;
            String displayText = this.fontRenderer.trimStringToWidth(targetLabel, maxWidth);
            drawString(this.fontRenderer, displayText,
                    this.guiLeft + GUI_WIDTH - 8 - this.fontRenderer.getStringWidth(displayText),
                    this.guiTop + 6,
                    0x506880);
        }

        drawString(this.fontRenderer, this.playerInventoryName, this.guiLeft + 8, this.guiTop + GUI_HEIGHT - 94,
                GuiTheme.resolveTextColor(this.playerInventoryName, 0x404040));

        renderSlots(graphics, this.viewerSlots, true);
        renderSlots(graphics, this.playerPreviewSlots, false);

        if (!this.hasCopiedInventoryData) {
            String hint = "暂无已复制玩家装备";
            drawCenteredString(this.fontRenderer, hint, this.guiLeft + GUI_WIDTH / 2, this.guiTop + 60, 0xB0B0B0);
        }

        if (this.hoveredSlot != null) {
            drawRect(this.hoveredSlot.x, this.hoveredSlot.y, this.hoveredSlot.x + SLOT_RENDER_SIZE,
                    this.hoveredSlot.y + SLOT_RENDER_SIZE, 0x80FFFFFF);
        }

        if (!this.cursorStack.isEmpty()) {
            int renderX = mouseX - 8;
            int renderY = mouseY - 8;
            graphics.renderItem(this.cursorStack, renderX, renderY);
            graphics.renderItemDecorations(this.mc.font, this.cursorStack, renderX, renderY);
        }

        String footer = "左键移动 | 右键拆分 | Shift 快速转移 | F 查看 NBT | E/ESC 关闭";
        drawCenteredString(this.fontRenderer, footer, this.width / 2, this.guiTop + GUI_HEIGHT + 8, 0x8FA1B5);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.hoveredSlot != null && this.cursorStack.isEmpty()) {
            ItemStack hoveredStack = getSlotStack(this.hoveredSlot);
            if (!hoveredStack.isEmpty()) {
                graphics.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
            }
        }
    }

    private void renderSlots(GuiGraphics graphics, NonNullList<ItemStack> slots, boolean viewerArea) {
        int baseY = viewerArea ? this.guiTop + 18 : this.guiTop + 139;
        int startIndex = viewerArea ? 0 : 0;
        int slotCount = viewerArea ? VIEWER_SLOT_COUNT : 27;

        for (int i = 0; i < slotCount; i++) {
            int slotX = this.guiLeft + 8 + (i % 9) * SLOT_SIZE;
            int slotY = baseY + (i / 9) * SLOT_SIZE;
            ItemStack stack = slots.get(startIndex + i);
            if (stack.isEmpty()) {
                continue;
            }
            graphics.renderItem(stack, slotX, slotY);
            graphics.renderItemDecorations(this.mc.font, stack, slotX, slotY);
        }

        if (!viewerArea) {
            int hotbarY = this.guiTop + 197;
            for (int i = 0; i < 9; i++) {
                int slotX = this.guiLeft + 8 + i * SLOT_SIZE;
                ItemStack stack = slots.get(27 + i);
                if (stack.isEmpty()) {
                    continue;
                }
                graphics.renderItem(stack, slotX, hotbarY);
                graphics.renderItemDecorations(this.mc.font, stack, slotX, hotbarY);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean canStack(ItemStack a, ItemStack b) {
        return !a.isEmpty()
                && !b.isEmpty()
                && ItemStack.isSameItemSameTags(a, b);
    }

    private ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
            return ItemStack.EMPTY;
        }
        return stack;
    }
}
