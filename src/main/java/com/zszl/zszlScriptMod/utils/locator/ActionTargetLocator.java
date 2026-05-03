package com.zszl.zszlScriptMod.utils.locator;

import com.zszl.zszlScriptMod.utils.guiinspect.GuiElementInspector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ActionTargetLocator {
    public static final String LEGACY_TARGET_MODE_POSITION_TYPO = "PISITION";
    public static final String CLICK_MODE_COORDINATE = "COORDINATE";
    public static final String CLICK_MODE_BUTTON_TEXT = "BUTTON_TEXT";
    public static final String CLICK_MODE_SLOT_TEXT = "SLOT_TEXT";
    public static final String CLICK_MODE_ELEMENT_PATH = "ELEMENT_PATH";
    public static final String SLOT_MODE_DIRECT = "DIRECT_SLOT";
    public static final String SLOT_MODE_ITEM_TEXT = "ITEM_TEXT";
    public static final String SLOT_MODE_EMPTY = "EMPTY_SLOT";
    public static final String SLOT_MODE_PATH = "SLOT_PATH";
    public static final String TARGET_MODE_POSITION = "POSITION";
    public static final String TARGET_MODE_NAME = "NAME";
    public static final String MATCH_MODE_CONTAINS = "CONTAINS";
    public static final String MATCH_MODE_EXACT = "EXACT";

    private ActionTargetLocator() {
    }

    public static String normalizeClickLocatorMode(String locatorMode) {
        String mode = safe(locatorMode).trim().toUpperCase(Locale.ROOT);
        if (mode.isEmpty()
                || TARGET_MODE_POSITION.equals(mode)
                || LEGACY_TARGET_MODE_POSITION_TYPO.equalsIgnoreCase(mode)) {
            return CLICK_MODE_COORDINATE;
        }
        return mode;
    }

    public static String normalizeWorldLocatorMode(String locatorMode) {
        String mode = safe(locatorMode).trim().toUpperCase(Locale.ROOT);
        if (mode.isEmpty() || LEGACY_TARGET_MODE_POSITION_TYPO.equalsIgnoreCase(mode)) {
            return TARGET_MODE_POSITION;
        }
        return mode;
    }

    public static final class ClickPoint {
        private final int x;
        private final int y;
        private final String description;

        public ClickPoint(int x, int y, String description) {
            this.x = x;
            this.y = y;
            this.description = description == null ? "" : description;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class SlotResult {
        private final int slotIndex;
        private final String description;

        public SlotResult(int slotIndex, String description) {
            this.slotIndex = slotIndex;
            this.description = description == null ? "" : description;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final class BlockMatch {
        private final BlockPos pos;
        private final double distSq;
        private final String description;

        private BlockMatch(BlockPos pos, double distSq, String description) {
            this.pos = pos;
            this.distSq = distSq;
            this.description = description;
        }
    }

    private static final class EntityMatch {
        private final Entity entity;
        private final double distSq;

        private EntityMatch(Entity entity, double distSq) {
            this.entity = entity;
            this.distSq = distSq;
        }
    }

    public static ClickPoint resolveScreenClickPoint(String locatorMode, String locatorText, String matchMode) {
        String mode = normalizeClickLocatorMode(locatorMode);
        if (mode.isEmpty() || CLICK_MODE_COORDINATE.equals(mode)) {
            return null;
        }
        if (CLICK_MODE_BUTTON_TEXT.equals(mode)) {
            return findClickPointByText(locatorText, matchMode, GuiElementInspector.ElementType.BUTTON,
                    GuiElementInspector.ElementType.CUSTOM);
        }
        if (CLICK_MODE_SLOT_TEXT.equals(mode)) {
            return findClickPointByText(locatorText, matchMode, GuiElementInspector.ElementType.SLOT);
        }
        if (CLICK_MODE_ELEMENT_PATH.equals(mode)) {
            GuiElementInspector.GuiElementInfo info = GuiElementInspector.findFirstByPath(locatorText, matchMode);
            if (info != null) {
                return toClickPoint(info);
            }
        }
        return null;
    }

    public static boolean tryInvokeCurrentScreenClick(String locatorMode, String locatorText, String matchMode,
            boolean isLeftClick) {
        ClickPoint point = resolveScreenClickPoint(locatorMode, locatorText, matchMode);
        return point != null && tryInvokeCurrentScreenClick(point.getX(), point.getY(), isLeftClick);
    }

    public static boolean tryInvokeCurrentScreenClick(String locatorMode, String locatorText, String matchMode,
            String mouseButton) {
        ClickPoint point = resolveScreenClickPoint(locatorMode, locatorText, matchMode);
        return point != null && tryInvokeCurrentScreenClick(point.getX(), point.getY(), mouseButton);
    }

    public static boolean tryInvokeCurrentScreenClick(int x, int y, boolean isLeftClick) {
        return tryInvokeCurrentScreenClick(x, y, isLeftClick ? "left" : "right");
    }

    public static boolean tryInvokeCurrentScreenClick(int x, int y, String mouseButton) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return false;
        }
        int button = "middle".equalsIgnoreCase(mouseButton)
                ? 2
                : ("right".equalsIgnoreCase(mouseButton) ? 1 : 0);
        boolean handled = screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
        return handled;
    }

    public static SlotResult resolveContainerSlot(String locatorMode, String locatorText, String matchMode) {
        String mode = safe(locatorMode).trim().toUpperCase(Locale.ROOT);
        if (mode.isEmpty() || SLOT_MODE_DIRECT.equals(mode)) {
            return null;
        }
        if (SLOT_MODE_ITEM_TEXT.equals(mode)) {
            return findContainerSlotByItemText(locatorText, matchMode);
        }
        if (SLOT_MODE_EMPTY.equals(mode)) {
            return findContainerEmptySlot();
        }
        if (SLOT_MODE_PATH.equals(mode)) {
            GuiElementInspector.GuiElementInfo info = GuiElementInspector.findFirstByPath(locatorText, matchMode,
                    GuiElementInspector.ElementType.SLOT);
            if (info != null && info.getSlotIndex() >= 0) {
                return new SlotResult(info.getSlotIndex(), info.getPath());
            }
        }
        return null;
    }

    public static BlockPos findNearbyInteractableBlock(String locatorText, String matchMode, double range) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }

        String query = normalize(locatorText);
        if (query.isEmpty()) {
            return null;
        }

        int radius = Math.max(1, (int) Math.ceil(Math.max(1.0D, range)));
        double maxDistSq = Math.max(1.0D, range) * Math.max(1.0D, range);
        BlockPos center = player.blockPosition();
        List<BlockMatch> matches = new ArrayList<>();

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq > maxDistSq) {
                        continue;
                    }
                    BlockPos current = center.offset(x, y, z);
                    String searchText = buildBlockSearchText(current);
                    if (!matches(searchText, query, matchMode)) {
                        continue;
                    }
                    matches.add(new BlockMatch(current, distSq, searchText));
                }
            }
        }

        matches.sort(Comparator.comparingDouble(match -> match.distSq));
        return matches.isEmpty() ? null : matches.get(0).pos;
    }

    public static Entity findNearbyEntity(String locatorText, String matchMode, double range) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }

        String query = normalize(locatorText);
        if (query.isEmpty()) {
            return null;
        }

        double radius = Math.max(1.0D, range);
        AABB bounds = player.getBoundingBox().inflate(radius);
        List<EntityMatch> matches = new ArrayList<>();
        for (Entity entity : mc.level.getEntities(player, bounds)) {
            if (entity == null || entity == player) {
                continue;
            }
            String searchText = buildEntitySearchText(entity);
            if (!matches(searchText, query, matchMode)) {
                continue;
            }
            matches.add(new EntityMatch(entity, entity.distanceToSqr(player)));
        }
        matches.sort(Comparator.comparingDouble(match -> match.distSq));
        return matches.isEmpty() ? null : matches.get(0).entity;
    }

    private static ClickPoint findClickPointByText(String locatorText, String matchMode,
            GuiElementInspector.ElementType... allowedTypes) {
        String query = normalize(locatorText);
        if (query.isEmpty()) {
            return null;
        }
        for (GuiElementInspector.GuiElementInfo info : GuiElementInspector.captureCurrentSnapshot().getElements()) {
            if (info == null || !isAllowed(info.getType(), allowedTypes)) {
                continue;
            }
            if (matches(normalize(info.getText()), query, matchMode)) {
                return toClickPoint(info);
            }
        }
        return null;
    }

    private static ClickPoint toClickPoint(GuiElementInspector.GuiElementInfo info) {
        return new ClickPoint(info.getX() + Math.max(1, info.getWidth()) / 2,
                info.getY() + Math.max(1, info.getHeight()) / 2,
                info.getPath());
    }

    private static SlotResult findContainerSlotByItemText(String locatorText, String matchMode) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return null;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return null;
        }
        String query = normalize(locatorText);
        if (query.isEmpty()) {
            return null;
        }
        for (int index : buildPreferredSlotOrder(menu, player)) {
            if (index < 0 || index >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(index);
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            String searchText = normalize(slot.getItem().getHoverName().getString());
            if (matches(searchText, query, matchMode)) {
                return new SlotResult(index, slot.getItem().getHoverName().getString());
            }
        }
        return null;
    }

    private static SlotResult findContainerEmptySlot() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return null;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return null;
        }
        for (int index : buildPreferredSlotOrder(menu, player)) {
            if (index < 0 || index >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.slots.get(index);
            if (slot != null && !slot.hasItem()) {
                return new SlotResult(index, "empty");
            }
        }
        return null;
    }

    private static List<Integer> buildPreferredSlotOrder(AbstractContainerMenu menu, LocalPlayer player) {
        List<Integer> order = new ArrayList<>();
        if (menu == null || menu.slots == null) {
            return order;
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot != null && slot.container != player.getInventory()) {
                order.add(i);
            }
        }
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot != null && slot.container == player.getInventory()) {
                order.add(i);
            }
        }
        return order;
    }

    private static String buildBlockSearchText(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        Block block = mc.level.getBlockState(pos).getBlock();
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        StringBuilder builder = new StringBuilder();
        if (block != null) {
            builder.append(block.getName().getString()).append(' ');
            builder.append(String.valueOf(block)).append(' ');
        }
        if (blockEntity != null) {
            builder.append(blockEntity.getType()).append(' ');
        }
        return normalize(builder.toString());
    }

    private static String buildEntitySearchText(Entity entity) {
        StringBuilder builder = new StringBuilder();
        builder.append(entity.getName().getString()).append(' ');
        builder.append(entity.getType().toString()).append(' ');
        builder.append(entity.getEncodeId() == null ? "" : entity.getEncodeId());
        return normalize(builder.toString());
    }

    private static boolean isAllowed(GuiElementInspector.ElementType type,
            GuiElementInspector.ElementType... allowedTypes) {
        if (allowedTypes == null || allowedTypes.length == 0) {
            return true;
        }
        for (GuiElementInspector.ElementType allowedType : allowedTypes) {
            if (allowedType == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String source, String query, String matchMode) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        if ("EXACT".equalsIgnoreCase(matchMode)) {
            return source.equals(query);
        }
        return source.contains(query);
    }

    private static String normalize(String value) {
        String stripped = ChatFormatting.stripFormatting(value);
        return (stripped == null ? safe(value) : stripped).trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
