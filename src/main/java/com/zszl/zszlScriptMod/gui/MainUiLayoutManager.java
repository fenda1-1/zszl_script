package com.zszl.zszlScriptMod.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

public final class MainUiLayoutManager {

    public static final String SORT_DEFAULT = "default";
    public static final String SORT_ALPHABETICAL = "alphabetical";
    public static final String SORT_LAST_OPENED = "last_opened";
    public static final String SORT_OPEN_COUNT = "open_count";

    public static final String LAYOUT_TILE = "tile";
    public static final String LAYOUT_LIST = "list";
    public static final String LAYOUT_COMPACT = "compact";
    public static final String LAYOUT_WIDE = "wide";

    public static final String ICON_DEFAULT = "default";
    public static final String ICON_XL = "xl";
    public static final String ICON_LARGE = "large";
    public static final String ICON_MEDIUM = "medium";
    public static final String ICON_SMALL = "small";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class LayoutData {
        int categoryPanelBaseWidth = -1;
        Map<String, GroupMeta> groupMeta = new LinkedHashMap<>();
        Map<String, List<String>> subCategories = new LinkedHashMap<>();
        Map<String, SequenceOpenStats> sequenceOpenStats = new LinkedHashMap<>();
    }

    public static final class GroupMeta {
        public boolean pinned;
        public boolean collapsed;
        public String sortMode = SORT_DEFAULT;
        public String layoutMode = LAYOUT_TILE;
        public String iconSize = ICON_DEFAULT;

        public GroupMeta copy() {
            GroupMeta copy = new GroupMeta();
            copy.pinned = pinned;
            copy.collapsed = collapsed;
            copy.sortMode = sortMode;
            copy.layoutMode = layoutMode;
            copy.iconSize = iconSize;
            return copy;
        }
    }

    public static final class SequenceOpenStats {
        public long lastOpenedAt;
        public int openCount;

        public SequenceOpenStats copy() {
            SequenceOpenStats copy = new SequenceOpenStats();
            copy.lastOpenedAt = lastOpenedAt;
            copy.openCount = openCount;
            return copy;
        }
    }

    private static LayoutData data = new LayoutData();
    private static boolean loaded = false;

    private MainUiLayoutManager() {
    }

    private static Path getLayoutFile() {
        return ProfileManager.getCurrentProfileDir().resolve("inventory_ui_layout.json");
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            synchronizeWithSequences(false);
            return;
        }

        loaded = true;
        data = new LayoutData();
        Path file = getLayoutFile();
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement root = new JsonParser().parse(reader);
                if (root != null && root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();

                    if (obj.has("groupMeta") && obj.get("groupMeta").isJsonObject()) {
                        Type mapType = new TypeToken<LinkedHashMap<String, GroupMeta>>() {
                        }.getType();
                        Map<String, GroupMeta> loadedMap = GSON.fromJson(obj.get("groupMeta"), mapType);
                        if (loadedMap != null) {
                            data.groupMeta.putAll(loadedMap);
                        }
                    }

                    if (obj.has("subCategories") && obj.get("subCategories").isJsonObject()) {
                        Type mapType = new TypeToken<LinkedHashMap<String, List<String>>>() {
                        }.getType();
                        Map<String, List<String>> loadedMap = GSON.fromJson(obj.get("subCategories"), mapType);
                        if (loadedMap != null) {
                            data.subCategories.putAll(loadedMap);
                        }
                    }

                    if (obj.has("categoryPanelBaseWidth")) {
                        try {
                            data.categoryPanelBaseWidth = obj.get("categoryPanelBaseWidth").getAsInt();
                        } catch (Exception ignored) {
                        }
                    }

                    if (obj.has("sequenceOpenStats") && obj.get("sequenceOpenStats").isJsonObject()) {
                        Type mapType = new TypeToken<LinkedHashMap<String, SequenceOpenStats>>() {
                        }.getType();
                        Map<String, SequenceOpenStats> loadedMap = GSON.fromJson(obj.get("sequenceOpenStats"), mapType);
                        if (loadedMap != null) {
                            data.sequenceOpenStats.putAll(loadedMap);
                        }
                    }
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.warn("[main_ui_layout] 读取主界面布局失败: {}", file, e);
            }
        }

        synchronizeWithSequences(true);
    }

    public static synchronized void reload() {
        loaded = false;
        ensureLoaded();
    }

    private static synchronized void synchronizeWithSequences(boolean saveAfterSync) {
        boolean changed = false;
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (sequence == null || !sequence.isCustom()) {
                continue;
            }

            String category = normalize(sequence.getCategory());
            if (category.isEmpty()) {
                continue;
            }

            GroupMeta meta = data.groupMeta.get(category);
            if (meta == null) {
                data.groupMeta.put(category, new GroupMeta());
                changed = true;
            } else {
                changed |= sanitizeMeta(meta);
            }

            String subCategory = normalize(sequence.getSubCategory());
            if (!subCategory.isEmpty()) {
                changed |= addSubCategoryInternal(category, subCategory, false);
            }
        }

        if (saveAfterSync && changed) {
            save();
        }
    }

    private static boolean sanitizeMeta(GroupMeta meta) {
        boolean changed = false;
        if (!isSupportedSortMode(meta.sortMode)) {
            meta.sortMode = SORT_DEFAULT;
            changed = true;
        }
        if (!isSupportedLayoutMode(meta.layoutMode)) {
            meta.layoutMode = LAYOUT_TILE;
            changed = true;
        }
        if (!isSupportedIconSize(meta.iconSize)) {
            meta.iconSize = ICON_DEFAULT;
            changed = true;
        }
        return changed;
    }

    private static boolean isSupportedSortMode(String sortMode) {
        return SORT_DEFAULT.equals(sortMode)
                || SORT_ALPHABETICAL.equals(sortMode)
                || SORT_LAST_OPENED.equals(sortMode)
                || SORT_OPEN_COUNT.equals(sortMode);
    }

    private static boolean isSupportedLayoutMode(String layoutMode) {
        return LAYOUT_TILE.equals(layoutMode)
                || LAYOUT_LIST.equals(layoutMode)
                || LAYOUT_COMPACT.equals(layoutMode)
                || LAYOUT_WIDE.equals(layoutMode);
    }

    private static boolean isSupportedIconSize(String iconSize) {
        return ICON_DEFAULT.equals(iconSize)
                || ICON_XL.equals(iconSize)
                || ICON_LARGE.equals(iconSize)
                || ICON_MEDIUM.equals(iconSize)
                || ICON_SMALL.equals(iconSize);
    }

    public static synchronized void save() {
        ensureLoaded();
        Path file = getLayoutFile();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.addProperty("categoryPanelBaseWidth", data.categoryPanelBaseWidth);
                root.add("groupMeta", GSON.toJsonTree(data.groupMeta));
                root.add("subCategories", GSON.toJsonTree(data.subCategories));
                root.add("sequenceOpenStats", GSON.toJsonTree(data.sequenceOpenStats));
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            zszlScriptMod.LOGGER.warn("[main_ui_layout] 保存主界面布局失败: {}", file, e);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static synchronized GroupMeta getGroupMeta(String category) {
        ensureLoaded();
        String normalized = normalize(category);
        GroupMeta meta = data.groupMeta.get(normalized);
        if (meta == null) {
            meta = new GroupMeta();
            data.groupMeta.put(normalized, meta);
            save();
        } else {
            sanitizeMeta(meta);
        }
        return meta.copy();
    }

    private static synchronized GroupMeta getMutableGroupMeta(String category) {
        ensureLoaded();
        String normalized = normalize(category);
        GroupMeta meta = data.groupMeta.get(normalized);
        if (meta == null) {
            meta = new GroupMeta();
            data.groupMeta.put(normalized, meta);
        } else {
            sanitizeMeta(meta);
        }
        return meta;
    }

    public static synchronized boolean isPinned(String category) {
        return getGroupMeta(category).pinned;
    }

    public static synchronized boolean isCollapsed(String category) {
        return getGroupMeta(category).collapsed;
    }

    public static synchronized void setPinned(String category, boolean pinned) {
        GroupMeta meta = getMutableGroupMeta(category);
        if (meta.pinned != pinned) {
            meta.pinned = pinned;
            save();
        }
    }

    public static synchronized void togglePinned(String category) {
        GroupMeta meta = getMutableGroupMeta(category);
        meta.pinned = !meta.pinned;
        save();
    }

    public static synchronized void setCollapsed(String category, boolean collapsed) {
        GroupMeta meta = getMutableGroupMeta(category);
        if (meta.collapsed != collapsed) {
            meta.collapsed = collapsed;
            save();
        }
    }

    public static synchronized void toggleCollapsed(String category) {
        GroupMeta meta = getMutableGroupMeta(category);
        meta.collapsed = !meta.collapsed;
        save();
    }

    public static synchronized void setCollapsedForCategories(Iterable<String> categories, boolean collapsed) {
        ensureLoaded();
        boolean changed = false;
        if (categories == null) {
            return;
        }
        for (String category : categories) {
            String normalized = normalize(category);
            if (normalized.isEmpty()) {
                continue;
            }
            GroupMeta meta = getMutableGroupMeta(normalized);
            if (meta.collapsed != collapsed) {
                meta.collapsed = collapsed;
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    public static synchronized String getSortMode(String category) {
        return getGroupMeta(category).sortMode;
    }

    public static synchronized void setSortMode(String category, String sortMode) {
        if (!isSupportedSortMode(sortMode)) {
            return;
        }
        GroupMeta meta = getMutableGroupMeta(category);
        if (!sortMode.equals(meta.sortMode)) {
            meta.sortMode = sortMode;
            save();
        }
    }

    public static synchronized String getLayoutMode(String category) {
        return getGroupMeta(category).layoutMode;
    }

    public static synchronized void setLayoutMode(String category, String layoutMode) {
        if (!isSupportedLayoutMode(layoutMode)) {
            return;
        }
        GroupMeta meta = getMutableGroupMeta(category);
        if (!layoutMode.equals(meta.layoutMode)) {
            meta.layoutMode = layoutMode;
            save();
        }
    }

    public static synchronized String getIconSize(String category) {
        return getGroupMeta(category).iconSize;
    }

    public static synchronized void setIconSize(String category, String iconSize) {
        if (!isSupportedIconSize(iconSize)) {
            return;
        }
        GroupMeta meta = getMutableGroupMeta(category);
        if (!iconSize.equals(meta.iconSize)) {
            meta.iconSize = iconSize;
            save();
        }
    }

    public static synchronized List<String> getSubCategories(String category) {
        ensureLoaded();
        String normalizedCategory = normalize(category);
        List<String> subCategories = data.subCategories.get(normalizedCategory);
        if (subCategories == null) {
            return new ArrayList<>();
        }

        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (String subCategory : subCategories) {
            String normalized = normalize(subCategory);
            if (!normalized.isEmpty()) {
                dedup.add(normalized);
            }
        }
        return new ArrayList<>(dedup);
    }

    private static boolean addSubCategoryInternal(String category, String subCategory, boolean saveAfter) {
        String normalizedCategory = normalize(category);
        String normalizedSubCategory = normalize(subCategory);
        if (normalizedCategory.isEmpty() || normalizedSubCategory.isEmpty()) {
            return false;
        }

        List<String> list = data.subCategories.get(normalizedCategory);
        if (list == null) {
            list = new ArrayList<>();
            data.subCategories.put(normalizedCategory, list);
        }

        for (String existing : list) {
            if (normalizedSubCategory.equalsIgnoreCase(normalize(existing))) {
                return false;
            }
        }

        list.add(normalizedSubCategory);
        if (saveAfter) {
            save();
        }
        return true;
    }

    public static synchronized boolean addSubCategory(String category, String subCategory) {
        ensureLoaded();
        boolean changed = addSubCategoryInternal(category, subCategory, false);
        if (changed) {
            save();
        }
        return changed;
    }

    public static synchronized boolean moveSubCategory(String category, String subCategoryToMove, String anchorSubCategory,
            boolean placeAfter) {
        ensureLoaded();
        String normalizedCategory = normalize(category);
        String normalizedMove = normalize(subCategoryToMove);
        String normalizedAnchor = normalize(anchorSubCategory);
        if (normalizedCategory.isEmpty() || normalizedMove.isEmpty() || normalizedAnchor.isEmpty()
                || normalizedMove.equalsIgnoreCase(normalizedAnchor)) {
            return false;
        }

        List<String> list = data.subCategories.get(normalizedCategory);
        if (list == null || list.size() < 2) {
            return false;
        }

        int moveIndex = -1;
        int anchorIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            String item = normalize(list.get(i));
            if (normalizedMove.equalsIgnoreCase(item)) {
                moveIndex = i;
            }
            if (normalizedAnchor.equalsIgnoreCase(item)) {
                anchorIndex = i;
            }
        }

        if (moveIndex < 0 || anchorIndex < 0 || moveIndex == anchorIndex) {
            return false;
        }

        String movingItem = list.remove(moveIndex);
        if (moveIndex < anchorIndex) {
            anchorIndex--;
        }
        int insertIndex = placeAfter ? anchorIndex + 1 : anchorIndex;
        insertIndex = Math.max(0, Math.min(insertIndex, list.size()));
        list.add(insertIndex, movingItem);
        save();
        return true;
    }

    public static synchronized boolean renameSubCategory(String category, String oldSubCategory, String newSubCategory) {
        ensureLoaded();
        String normalizedCategory = normalize(category);
        String normalizedOld = normalize(oldSubCategory);
        String normalizedNew = normalize(newSubCategory);
        if (normalizedCategory.isEmpty() || normalizedOld.isEmpty() || normalizedNew.isEmpty()
                || normalizedOld.equalsIgnoreCase(normalizedNew)) {
            return false;
        }

        List<String> list = data.subCategories.get(normalizedCategory);
        if (list == null) {
            return false;
        }

        boolean exists = false;
        for (String item : list) {
            if (normalizedOld.equalsIgnoreCase(normalize(item))) {
                exists = true;
            }
            if (normalizedNew.equalsIgnoreCase(normalize(item))) {
                return false;
            }
        }
        if (!exists) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            if (normalizedOld.equalsIgnoreCase(normalize(list.get(i)))) {
                list.set(i, normalizedNew);
                break;
            }
        }

        List<PathSequence> allSequences = PathSequenceManager.getAllSequences();
        for (PathSequence sequence : allSequences) {
            if (sequence != null
                    && sequence.isCustom()
                    && normalizedCategory.equals(normalize(sequence.getCategory()))
                    && normalizedOld.equalsIgnoreCase(normalize(sequence.getSubCategory()))) {
                sequence.setSubCategory(normalizedNew);
            }
        }

        PathSequenceManager.saveAllSequences(allSequences);
        save();
        return true;
    }

    public static synchronized int deleteSubCategory(String category, String subCategory, boolean deleteSequences) {
        ensureLoaded();
        String normalizedCategory = normalize(category);
        String normalizedSubCategory = normalize(subCategory);
        if (normalizedCategory.isEmpty() || normalizedSubCategory.isEmpty()) {
            return 0;
        }

        List<String> list = data.subCategories.get(normalizedCategory);
        if (list != null) {
            list.removeIf(item -> normalizedSubCategory.equalsIgnoreCase(normalize(item)));
            if (list.isEmpty()) {
                data.subCategories.remove(normalizedCategory);
            }
        }

        int removedCount = 0;
        if (deleteSequences) {
            removedCount = PathSequenceManager.deleteCustomSequencesInSubCategory(normalizedCategory, normalizedSubCategory);
        }

        save();
        return removedCount;
    }

    public static synchronized void renameCategory(String oldCategory, String newCategory) {
        ensureLoaded();
        String normalizedOld = normalize(oldCategory);
        String normalizedNew = normalize(newCategory);
        if (normalizedOld.isEmpty() || normalizedNew.isEmpty() || normalizedOld.equals(normalizedNew)) {
            return;
        }

        GroupMeta meta = data.groupMeta.remove(normalizedOld);
        if (meta != null) {
            data.groupMeta.put(normalizedNew, meta);
        }

        List<String> subCategories = data.subCategories.remove(normalizedOld);
        if (subCategories != null) {
            data.subCategories.put(normalizedNew, subCategories);
        }

        save();
    }

    public static synchronized void removeCategory(String category) {
        ensureLoaded();
        String normalized = normalize(category);
        data.groupMeta.remove(normalized);
        data.subCategories.remove(normalized);
        save();
    }

    public static synchronized void recordSequenceOpened(String sequenceName) {
        ensureLoaded();
        String normalized = normalize(sequenceName);
        if (normalized.isEmpty()) {
            return;
        }

        SequenceOpenStats stats = data.sequenceOpenStats.get(normalized);
        if (stats == null) {
            stats = new SequenceOpenStats();
            data.sequenceOpenStats.put(normalized, stats);
        }
        stats.openCount++;
        stats.lastOpenedAt = System.currentTimeMillis();
        save();
    }

    public static synchronized SequenceOpenStats getSequenceStats(String sequenceName) {
        ensureLoaded();
        SequenceOpenStats stats = data.sequenceOpenStats.get(normalize(sequenceName));
        return stats == null ? new SequenceOpenStats() : stats.copy();
    }

    public static synchronized void removeSequenceStats(String sequenceName) {
        ensureLoaded();
        if (data.sequenceOpenStats.remove(normalize(sequenceName)) != null) {
            save();
        }
    }

    public static synchronized void renameSequenceStats(String oldName, String newName) {
        ensureLoaded();
        String normalizedOld = normalize(oldName);
        String normalizedNew = normalize(newName);
        if (normalizedOld.isEmpty() || normalizedNew.isEmpty() || normalizedOld.equals(normalizedNew)) {
            return;
        }

        SequenceOpenStats stats = data.sequenceOpenStats.remove(normalizedOld);
        if (stats != null) {
            data.sequenceOpenStats.put(normalizedNew, stats);
            save();
        }
    }

    public static synchronized Set<String> getKnownCategories() {
        ensureLoaded();
        return new LinkedHashSet<>(data.groupMeta.keySet());
    }

    public static synchronized int getCategoryPanelBaseWidth() {
        ensureLoaded();
        return data.categoryPanelBaseWidth;
    }

    public static synchronized void setCategoryPanelBaseWidth(int width) {
        ensureLoaded();
        int normalized = width <= 0 ? -1 : width;
        if (data.categoryPanelBaseWidth != normalized) {
            data.categoryPanelBaseWidth = normalized;
            save();
        }
    }
}

