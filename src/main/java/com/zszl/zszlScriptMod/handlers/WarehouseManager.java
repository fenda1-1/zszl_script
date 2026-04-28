package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.dungeon.ChestData;
import com.zszl.zszlScriptMod.system.dungeon.Warehouse;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WarehouseManager {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CATEGORY_DEFAULT = "默认";
    private static final List<String> categories = new CopyOnWriteArrayList<>();

    public static List<Warehouse> warehouses = new CopyOnWriteArrayList<>();
    public static Warehouse currentWarehouse;

    private WarehouseManager() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("warehouses.json");
    }

    public static synchronized void loadWarehouses() {
        categories.clear();
        warehouses = new CopyOnWriteArrayList<>();
        currentWarehouse = null;

        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            ensureCategoriesSynced();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            com.google.gson.JsonElement parsed = new com.google.gson.JsonParser().parse(reader);
            if (parsed != null && parsed.isJsonObject()) {
                com.google.gson.JsonObject root = parsed.getAsJsonObject();
                if (root.has("categories") && root.get("categories").isJsonArray()) {
                    for (com.google.gson.JsonElement element : root.getAsJsonArray("categories")) {
                        if (element != null && element.isJsonPrimitive()) {
                            categories.add(normalizeCategory(element.getAsString()));
                        }
                    }
                }
                if (root.has("warehouses") && root.get("warehouses").isJsonArray()) {
                    Type listType = new TypeToken<CopyOnWriteArrayList<Warehouse>>() {
                    }.getType();
                    CopyOnWriteArrayList<Warehouse> loaded = GSON.fromJson(root.get("warehouses"), listType);
                    if (loaded != null) {
                        warehouses = loaded;
                    }
                }
            } else if (parsed != null && parsed.isJsonArray()) {
                Type listType = new TypeToken<CopyOnWriteArrayList<Warehouse>>() {
                }.getType();
                CopyOnWriteArrayList<Warehouse> loaded = GSON.fromJson(parsed, listType);
                if (loaded != null) {
                    warehouses = loaded;
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载仓库数据失败", e);
            warehouses = new CopyOnWriteArrayList<>();
        }

        for (Warehouse warehouse : warehouses) {
            if (warehouse != null) {
                warehouse.category = normalizeCategory(warehouse.category);
                warehouse.updateBounds();
                if (warehouse.chests == null) {
                    warehouse.chests = new CopyOnWriteArrayList<>();
                }
            }
        }
        ensureCategoriesSynced();
    }

    public static synchronized void saveWarehouses() {
        ensureCategoriesSynced();
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                com.google.gson.JsonObject root = new com.google.gson.JsonObject();
                root.add("categories", GSON.toJsonTree(new ArrayList<>(categories)));
                root.add("warehouses", GSON.toJsonTree(warehouses));
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存仓库数据失败", e);
        }
    }

    public static synchronized List<String> getCategoriesSnapshot() {
        ensureCategoriesSynced();
        return new ArrayList<>(categories);
    }

    public static synchronized boolean addCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        if (containsCategoryIgnoreCase(normalized)) {
            return false;
        }
        categories.add(normalized);
        saveWarehouses();
        return true;
    }

    public static synchronized boolean renameCategory(String oldCategory, String newCategory) {
        String normalizedOld = normalizeCategory(oldCategory);
        String normalizedNew = normalizeCategory(newCategory);
        ensureCategoriesSynced();
        if (normalizedOld.equalsIgnoreCase(normalizedNew)) {
            return true;
        }
        if (containsCategoryIgnoreCase(normalizedNew)) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < categories.size(); i++) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalizedOld)) {
                categories.set(i, normalizedNew);
                changed = true;
            }
        }
        for (Warehouse warehouse : warehouses) {
            if (warehouse != null && normalizeCategory(warehouse.category).equalsIgnoreCase(normalizedOld)) {
                warehouse.category = normalizedNew;
                changed = true;
            }
        }

        if (changed) {
            ensureCategoriesSynced();
            saveWarehouses();
        }
        return changed;
    }

    public static synchronized boolean deleteCategory(String category) {
        String normalized = normalizeCategory(category);
        ensureCategoriesSynced();
        boolean changed = removeCategoryIgnoreCase(normalized);
        for (Warehouse warehouse : warehouses) {
            if (warehouse != null && normalizeCategory(warehouse.category).equalsIgnoreCase(normalized)) {
                warehouse.category = CATEGORY_DEFAULT;
                changed = true;
            }
        }
        if (changed) {
            ensureCategoriesSynced();
            saveWarehouses();
        }
        return changed;
    }

    public static void updateCurrentWarehouse() {
        if (MC.player == null) {
            currentWarehouse = null;
            return;
        }
        for (Warehouse warehouse : warehouses) {
            if (warehouse != null && warehouse.isActive && warehouse.isPlayerInside(MC.player.getX(), MC.player.getZ())) {
                if (currentWarehouse != warehouse) {
                    currentWarehouse = warehouse;
                    checkBrokenChests(warehouse);
                }
                return;
            }
        }
        currentWarehouse = null;
    }

    public static Warehouse findWarehouseForPos(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        for (Warehouse warehouse : warehouses) {
            if (warehouse != null && warehouse.isPosInside(pos)) {
                return warehouse;
            }
        }
        return null;
    }

    public static void scanChest(ChestMenu chestMenu, BlockPos pos) {
        if (chestMenu == null || pos == null) {
            return;
        }
        Warehouse targetWarehouse = findWarehouseForPos(pos);
        if (targetWarehouse == null) {
            return;
        }
        ChestData chestData = targetWarehouse.getChestAt(pos);
        if (chestData == null) {
            chestData = new ChestData(pos);
            targetWarehouse.chests.add(chestData);
        }
        int containerSlots = chestMenu.getRowCount() * 9;
        NonNullList<ItemStack> items = NonNullList.withSize(containerSlots, ItemStack.EMPTY);
        for (int i = 0; i < containerSlots; i++) {
            items.set(i, chestMenu.slots.get(i).getItem().copy());
        }
        chestData.snapshotContents(items);
        saveWarehouses();
    }

    public static void scanForChestsInWarehouse(Warehouse warehouse) {
        if (warehouse == null || MC.level == null || MC.player == null) {
            return;
        }

        Set<BlockPos> found = new HashSet<>();
        int playerY = MC.player.blockPosition().getY();
        int minY = Math.max(MC.level.getMinY(), playerY - 30);
        int maxY = Math.min(MC.level.getMaxY(), playerY + 30);

        for (int y = minY; y <= maxY; y++) {
            for (int x = (int) warehouse.minX; x <= (int) warehouse.maxX; x++) {
                for (int z = (int) warehouse.minZ; z <= (int) warehouse.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = MC.level.getBlockEntity(pos);
                    if (blockEntity instanceof ChestBlockEntity) {
                        found.add(pos.immutable());
                    }
                }
            }
        }

        int newCount = 0;
        for (BlockPos pos : found) {
            if (warehouse.getChestAt(pos) == null) {
                warehouse.chests.add(new ChestData(pos));
                newCount++;
            }
        }
        saveWarehouses();
        if (MC.player != null) {
            MC.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§b[仓库] §a扫描完成！共发现 " + found.size() + " 个箱子，其中 " + newCount + " 个是新记录的。"), false);
        }
    }

    public static void checkBrokenChests(Warehouse warehouse) {
        if (warehouse == null || warehouse.chests == null || MC.level == null) {
            return;
        }
        boolean changed = warehouse.chests.removeIf(chest -> chest == null
                || chest.pos == null
                || !(MC.level.getBlockEntity(chest.pos) instanceof ChestBlockEntity));
        if (changed) {
            saveWarehouses();
        }
    }

    private static String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim();
        return normalized.isEmpty() ? CATEGORY_DEFAULT : normalized;
    }

    private static void ensureCategoriesSynced() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String category : categories) {
            normalized.add(normalizeCategory(category));
        }
        for (Warehouse warehouse : warehouses) {
            if (warehouse != null) {
                warehouse.category = normalizeCategory(warehouse.category);
                normalized.add(warehouse.category);
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(CATEGORY_DEFAULT);
        }
        categories.clear();
        categories.addAll(normalized);
    }

    private static boolean containsCategoryIgnoreCase(String category) {
        for (String existing : categories) {
            if (normalizeCategory(existing).equalsIgnoreCase(normalizeCategory(category))) {
                return true;
            }
        }
        return false;
    }

    private static boolean removeCategoryIgnoreCase(String category) {
        for (int i = 0; i < categories.size(); i++) {
            if (normalizeCategory(categories.get(i)).equalsIgnoreCase(normalizeCategory(category))) {
                categories.remove(i);
                return true;
            }
        }
        return false;
    }
}
