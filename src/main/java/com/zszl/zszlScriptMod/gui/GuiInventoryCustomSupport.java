// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/GuiInventory.java
package com.zszl.zszlScriptMod.gui;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.config.BaritoneParkourSettingsHelper;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.config.GuiAutoEatConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoFishingConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoEscapeManager;
import com.zszl.zszlScriptMod.gui.config.GuiAutoFollowManager;
import com.zszl.zszlScriptMod.gui.config.GuiAutoPickupConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoUseItemConfig;
import com.zszl.zszlScriptMod.gui.config.GuiBaritoneCommandTable;
import com.zszl.zszlScriptMod.gui.config.GuiBaritoneParkourSettings;
import com.zszl.zszlScriptMod.gui.config.GuiBlockReplacementConfig;
import com.zszl.zszlScriptMod.gui.config.GuiChatOptimization;
import com.zszl.zszlScriptMod.gui.config.GuiConditionalExecutionManager;
import com.zszl.zszlScriptMod.gui.config.GuiFlyConfig;
import com.zszl.zszlScriptMod.gui.config.GuiKeybindManager;
import com.zszl.zszlScriptMod.gui.config.GuiKillAuraConfig;
import com.zszl.zszlScriptMod.gui.config.GuiLoopCountInput;
import com.zszl.zszlScriptMod.gui.config.GuiProfileManager;
import com.zszl.zszlScriptMod.gui.config.GuiServerFeatureVisibilityConfig;
import com.zszl.zszlScriptMod.gui.dungeon.GuiWarehouseManager;
import com.zszl.zszlScriptMod.gui.path.GuiPathManager;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler;
import com.zszl.zszlScriptMod.handlers.AutoUseItemHandler;
import com.zszl.zszlScriptMod.handlers.ConditionalExecutionHandler;
import com.zszl.zszlScriptMod.handlers.FlyHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.system.AutoFollowRule;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;

abstract class GuiInventoryCustomSupport extends GuiInventoryBase {
    protected static boolean isCommonCategory(String category) {
        return I18n.format("gui.inventory.category.common").equals(category);
    }

    protected static void rebuildCommonSections(List<String> availableCommands) {
        commonItemSections.clear();
        LinkedHashSet<String> available = new LinkedHashSet<>(availableCommands);

        addCommonSection("automation", "自动化", available, Arrays.asList("autoeat", "toggle_auto_fishing",
                "toggle_auto_pickup", "toggle_auto_use_item", "followconfig"));
        addCommonSection("combat_execute", "战斗与执行", available,
                Arrays.asList("toggle_kill_aura", "conditional_execution", "auto_escape", "setloop"));
        addCommonSection("config_interaction", "配置与交互", available, Arrays.asList("toggle_mouse_detach",
                "keybind_manager", "profile_manager", "chat_optimization", "toggle_server_feature_visibility"));
        addCommonSection("movement_scene", "移动与场景", available,
                Arrays.asList("toggle_fly", "block_replacement_config", "warehouse_manager", "baritone_settings",
                        "baritone_parkour"));

        for (GroupedItemSection section : commonItemSections) {
            commonSectionExpanded.putIfAbsent(section.key, Boolean.TRUE);
        }
    }

    protected static void addCommonSection(String key, String title, Set<String> availableCommands,
            List<String> commands) {
        List<String> sectionCommands = new ArrayList<>();
        for (String command : commands) {
            if (availableCommands.contains(command)) {
                sectionCommands.add(command);
            }
        }
        if (!sectionCommands.isEmpty()) {
            commonItemSections.add(new GroupedItemSection(key, title, sectionCommands));
        }
    }

    protected static int getCommonSectionItemRows(GroupedItemSection section) {
        if (section == null || section.commands == null || section.commands.isEmpty()) {
            return 0;
        }
        return (section.commands.size() + 2) / 3;
    }

    protected static int getCommonSectionPageUnits(GroupedItemSection section) {
        boolean expanded = section != null && commonSectionExpanded.getOrDefault(section.key, Boolean.TRUE);
        return expanded ? Math.max(1, 1 + getCommonSectionItemRows(section)) : 1;
    }

    protected static List<List<GroupedItemSection>> buildCommonContentPages() {
        List<List<GroupedItemSection>> pages = new ArrayList<>();
        List<GroupedItemSection> currentPageSections = new ArrayList<>();
        int currentUnits = 0;

        for (GroupedItemSection section : commonItemSections) {
            int sectionUnits = getCommonSectionPageUnits(section);

            if (!currentPageSections.isEmpty() && currentUnits + sectionUnits > COMMON_ROWS_PER_PAGE) {
                pages.add(currentPageSections);
                currentPageSections = new ArrayList<>();
                currentUnits = 0;
            }

            currentPageSections.add(section);
            currentUnits += sectionUnits;
        }

        if (!currentPageSections.isEmpty() || pages.isEmpty()) {
            pages.add(currentPageSections);
        }

        return pages;
    }

    protected static boolean isCustomCategorySelection() {
        return isCustomOverlayCategory(currentCategory);
    }

    protected static String normalizeSequenceSubCategory(PathSequence sequence) {
        return sequence == null ? "" : normalizeText(sequence.getSubCategory());
    }

    protected static List<PathSequence> getCustomSequencesForSelection(String category, String subCategory) {
        String normalizedCategory = normalizeText(category);
        String normalizedSubCategory = normalizeText(subCategory);
        List<PathSequence> result = new ArrayList<>();
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (sequence == null || !sequence.isCustom()) {
                continue;
            }
            if (!normalizedCategory.equals(normalizeText(sequence.getCategory()))) {
                continue;
            }
            if (!normalizedSubCategory.isEmpty()
                    && !normalizedSubCategory.equalsIgnoreCase(normalizeSequenceSubCategory(sequence))) {
                continue;
            }
            result.add(sequence);
        }

        String sortMode = MainUiLayoutManager.getSortMode(normalizedCategory);
        if (MainUiLayoutManager.SORT_ALPHABETICAL.equals(sortMode)) {
            result.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        } else if (MainUiLayoutManager.SORT_LAST_OPENED.equals(sortMode)) {
            result.sort((left, right) -> {
                MainUiLayoutManager.SequenceOpenStats leftStats = MainUiLayoutManager.getSequenceStats(left.getName());
                MainUiLayoutManager.SequenceOpenStats rightStats = MainUiLayoutManager
                        .getSequenceStats(right.getName());
                int compare = Long.compare(rightStats.lastOpenedAt, leftStats.lastOpenedAt);
                return compare != 0 ? compare : left.getName().compareToIgnoreCase(right.getName());
            });
        } else if (MainUiLayoutManager.SORT_OPEN_COUNT.equals(sortMode)) {
            result.sort((left, right) -> {
                MainUiLayoutManager.SequenceOpenStats leftStats = MainUiLayoutManager.getSequenceStats(left.getName());
                MainUiLayoutManager.SequenceOpenStats rightStats = MainUiLayoutManager
                        .getSequenceStats(right.getName());
                int compare = Integer.compare(rightStats.openCount, leftStats.openCount);
                if (compare != 0) {
                    return compare;
                }
                compare = Long.compare(rightStats.lastOpenedAt, leftStats.lastOpenedAt);
                return compare != 0 ? compare : left.getName().compareToIgnoreCase(right.getName());
            });
        }

        return result;
    }

    protected static CustomGridMetrics computeCustomGridMetrics(OverlayMetrics m, int contentStartY, String category) {
        CustomGridMetrics grid = new CustomGridMetrics();
        grid.startX = m.contentPanelX + m.padding + 6;
        grid.startY = contentStartY + m.padding + 6;
        grid.width = Math.max(120, m.contentPanelRight - grid.startX - m.padding - 6);
        int bottomReservedY = m.y + m.totalHeight - scaleUi(34, m.scale);
        grid.height = Math.max(40, bottomReservedY - grid.startY);
        grid.gap = Math.max(4, m.gap - 2);

        String layoutMode = MainUiLayoutManager.getLayoutMode(category);
        String iconSize = MainUiLayoutManager.getIconSize(category);
        int baseHeight = scaleUi(38, m.scale);
        int desiredColumns = 4;
        switch (iconSize) {
            case MainUiLayoutManager.ICON_XL:
                desiredColumns = 3;
                baseHeight = scaleUi(52, m.scale);
                break;
            case MainUiLayoutManager.ICON_LARGE:
                desiredColumns = 4;
                baseHeight = scaleUi(46, m.scale);
                break;
            case MainUiLayoutManager.ICON_MEDIUM:
                desiredColumns = 5;
                baseHeight = scaleUi(40, m.scale);
                break;
            case MainUiLayoutManager.ICON_SMALL:
                desiredColumns = 6;
                baseHeight = scaleUi(34, m.scale);
                break;
            default:
                desiredColumns = 4;
                baseHeight = scaleUi(42, m.scale);
                break;
        }

        if (MainUiLayoutManager.LAYOUT_LIST.equals(layoutMode)) {
            grid.columns = 1;
            grid.cardHeight = baseHeight;
        } else if (MainUiLayoutManager.LAYOUT_WIDE.equals(layoutMode)) {
            grid.columns = 2;
            grid.cardHeight = Math.max(scaleUi(34, m.scale), baseHeight);
        } else {
            grid.columns = desiredColumns;
            grid.cardHeight = baseHeight;
        }

        if (grid.columns <= 0) {
            grid.columns = 1;
        }

        grid.cardWidth = (grid.width - grid.gap * Math.max(0, grid.columns - 1)) / grid.columns;
        while (grid.columns > 1 && grid.cardWidth < scaleUi(54, m.scale)) {
            grid.columns--;
            grid.cardWidth = (grid.width - grid.gap * Math.max(0, grid.columns - 1)) / grid.columns;
        }

        grid.rowsPerPage = Math.max(1, grid.height / Math.max(1, grid.cardHeight + grid.gap));
        grid.pageSize = Math.max(1, grid.rowsPerPage * grid.columns);
        return grid;
    }

    protected static String buildCustomSequenceTooltip(PathSequence sequence) {
        StringBuilder builder = new StringBuilder();
        builder.append("序列: ").append(sequence.getName());
        String subCategory = normalizeSequenceSubCategory(sequence);
        if (!subCategory.isEmpty()) {
            builder.append("\n子分类: ").append(subCategory);
        }
        String note = normalizeText(sequence.getNote());
        if (!note.isEmpty()) {
            builder.append("\n备注: ").append(note);
        }
        MainUiLayoutManager.SequenceOpenStats stats = MainUiLayoutManager.getSequenceStats(sequence.getName());
        if (stats.openCount > 0) {
            builder.append("\n打开次数: ").append(stats.openCount);
        }
        return builder.toString();
    }

    protected static String getCustomSectionKey(String category, String subCategory) {
        String normalizedSubCategory = normalizeText(subCategory);
        return normalizeText(category) + "::section::" + (normalizedSubCategory.isEmpty() ? "__uncategorized__"
                : normalizedSubCategory.toLowerCase(Locale.ROOT));
    }

    protected static boolean isCustomSectionExpanded(String key) {
        return customSectionExpanded.getOrDefault(normalizeText(key), Boolean.TRUE);
    }

    protected static void toggleCustomSectionExpanded(String key) {
        String normalizedKey = normalizeText(key);
        customSectionExpanded.put(normalizedKey, !isCustomSectionExpanded(normalizedKey));
    }

    protected static void ensureCustomSequenceSearchField(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        int safeWidth = Math.max(40, width);
        int safeHeight = Math.max(16, height);
        if (customSequenceSearchField == null) {
            customSequenceSearchField = new GuiTextField(8600, mc.fontRenderer, x, y, safeWidth, safeHeight);
            customSequenceSearchField.setMaxStringLength(80);
            customSequenceSearchField.setCanLoseFocus(true);
            customSequenceSearchField.setText(customSequenceSearchQuery);
        } else {
            customSequenceSearchField.x = x;
            customSequenceSearchField.y = y;
            customSequenceSearchField.width = safeWidth;
            customSequenceSearchField.height = safeHeight;
            if (!customSequenceSearchQuery.equals(customSequenceSearchField.getText())) {
                customSequenceSearchField.setText(customSequenceSearchQuery);
            }
        }
        if (customSequenceSearchFocusPending) {
            customSequenceSearchField.setFocused(true);
            customSequenceSearchFocusPending = false;
        }
    }

    protected static void clearCustomSequenceSearch(boolean keepFocus) {
        customSequenceSearchQuery = "";
        if (customSequenceSearchField != null) {
            customSequenceSearchField.setText("");
            customSequenceSearchField.setFocused(keepFocus);
        }
        currentPage = 0;
        CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
    }

    protected static boolean isCustomSearchExpanded() {
        return customSequenceSearchExpanded;
    }

    protected static void setCustomSearchExpanded(boolean expanded, boolean focusField) {
        customSequenceSearchExpanded = expanded;
        if (customSequenceSearchField != null) {
            customSequenceSearchField.setFocused(expanded && focusField);
        }
        customSequenceSearchFocusPending = expanded && focusField && customSequenceSearchField == null;
    }

    protected static String getCompactCustomSearchScopeLabel() {
        String effectiveScope = getEffectiveCustomSearchScope();
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(effectiveScope)) {
            return "子类";
        }
        if (SEARCH_SCOPE_ALL_CATEGORIES.equals(effectiveScope)) {
            return "全分类";
        }
        return "分类";
    }

    protected static int getCustomSearchHeaderReservedWidth(FontRenderer fontRenderer, float scale) {
        return 0;
    }

    protected static void cycleCustomSearchScope() {
        List<String> scopes = new ArrayList<>();
        if (!isBlank(currentCustomSubCategory)) {
            scopes.add(SEARCH_SCOPE_CURRENT_SUBCATEGORY);
        }
        scopes.add(SEARCH_SCOPE_CURRENT_CATEGORY);
        scopes.add(SEARCH_SCOPE_ALL_CATEGORIES);
        if (scopes.isEmpty()) {
            return;
        }

        String effectiveScope = getEffectiveCustomSearchScope();
        int currentIndex = scopes.indexOf(effectiveScope);
        int nextIndex = (currentIndex + 1) % scopes.size();
        applyCustomSearchScope(scopes.get(nextIndex));
    }

    protected static String getEffectiveCustomSearchScope() {
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(customSequenceSearchScope) && isBlank(currentCustomSubCategory)) {
            return SEARCH_SCOPE_CURRENT_CATEGORY;
        }
        return customSequenceSearchScope;
    }

    protected static String getCustomSearchScopeLabel(String scope) {
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(scope)) {
            return "当前子分类";
        }
        if (SEARCH_SCOPE_ALL_CATEGORIES.equals(scope)) {
            return "全分类";
        }
        return "当前分类";
    }

    protected static void applyCustomSearchScope(String scope) {
        String normalizedScope = normalizeText(scope);
        if (!SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(normalizedScope)
                && !SEARCH_SCOPE_CURRENT_CATEGORY.equals(normalizedScope)
                && !SEARCH_SCOPE_ALL_CATEGORIES.equals(normalizedScope)) {
            return;
        }
        customSequenceSearchScope = normalizedScope;
        currentPage = 0;
        CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
        pruneSelectedCustomSequences();
    }

    protected static List<String> getVisibleCustomCategoriesInDisplayOrder() {
        List<String> customCategories = new ArrayList<>();
        for (String category : categories) {
            if (isCustomOverlayCategory(category)) {
                customCategories.add(category);
            }
        }
        customCategories.sort((left, right) -> {
            boolean leftPinned = MainUiLayoutManager.isPinned(left);
            boolean rightPinned = MainUiLayoutManager.isPinned(right);
            if (leftPinned != rightPinned) {
                return Boolean.compare(rightPinned, leftPinned);
            }
            return Integer.compare(categories.indexOf(left), categories.indexOf(right));
        });
        return customCategories;
    }

    protected static boolean matchesCustomSequenceSearchNormalized(PathSequence sequence, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        if (sequence == null) {
            return false;
        }
        return PinyinSearchHelper.matchesNormalized(sequence.getName(), normalizedQuery)
                || PinyinSearchHelper.matchesNormalized(sequence.getNote(), normalizedQuery)
                || PinyinSearchHelper.matchesNormalized(sequence.getSubCategory(), normalizedQuery)
                || PinyinSearchHelper.matchesNormalized(sequence.getCategory(), normalizedQuery);
    }

    protected static String buildCustomSectionDisplayTitle(String category, String subCategory, boolean includeCategory) {
        String normalizedCategory = normalizeText(category);
        String normalizedSubCategory = normalizeText(subCategory);
        String baseTitle = normalizedSubCategory.isEmpty() ? UNCATEGORIZED_SECTION_TITLE : normalizedSubCategory;
        if (!includeCategory) {
            return baseTitle;
        }
        if (normalizedCategory.isEmpty()) {
            return baseTitle;
        }
        return normalizedCategory + " / " + baseTitle;
    }

    protected static long getMostRecentSequenceOpenedAt(List<PathSequence> sequences) {
        long lastOpenedAt = 0L;
        if (sequences == null) {
            return lastOpenedAt;
        }
        for (PathSequence sequence : sequences) {
            if (sequence == null) {
                continue;
            }
            MainUiLayoutManager.SequenceOpenStats stats = MainUiLayoutManager.getSequenceStats(sequence.getName());
            lastOpenedAt = Math.max(lastOpenedAt, stats.lastOpenedAt);
        }
        return lastOpenedAt;
    }

    protected static int getRunningSequenceCount(List<PathSequence> sequences) {
        String runningName = getRunningCustomSequenceName();
        if (runningName.isEmpty() || sequences == null) {
            return 0;
        }
        int count = 0;
        for (PathSequence sequence : sequences) {
            if (sequence != null && runningName.equals(normalizeText(sequence.getName()))) {
                count++;
            }
        }
        return count;
    }

    protected static String buildCustomSectionStatsLabel(List<PathSequence> sequences) {
        int total = sequences == null ? 0 : sequences.size();
        StringBuilder builder = new StringBuilder();
        builder.append(total).append("个");
        long lastOpenedAt = getMostRecentSequenceOpenedAt(sequences);
        if (lastOpenedAt > 0L) {
            builder.append("  最近 ").append(formatRelativeTime(lastOpenedAt));
        }
        int runningCount = getRunningSequenceCount(sequences);
        if (runningCount > 0) {
            builder.append("  运行 ").append(runningCount);
        }
        return builder.toString();
    }

    protected static List<String> buildSequenceCardTitleLines(FontRenderer fontRenderer, String title, int maxWidth,
            int maxLines) {
        List<String> wrapped = fontRenderer.listFormattedStringToWidth(normalizeText(title), Math.max(24, maxWidth));
        if (wrapped.isEmpty()) {
            return Collections.singletonList("");
        }
        if (wrapped.size() <= maxLines) {
            return wrapped;
        }

        List<String> result = new ArrayList<>(wrapped.subList(0, maxLines));
        String lastLine = result.get(maxLines - 1);
        while (!lastLine.isEmpty() && fontRenderer.getStringWidth(lastLine + "...") > maxWidth) {
            lastLine = lastLine.substring(0, lastLine.length() - 1);
        }
        result.set(maxLines - 1, lastLine + "...");
        return result;
    }

    protected static void drawCenteredWrappedText(FontRenderer fontRenderer, List<String> lines, int centerX, int topY,
            int width, int lineHeight, int color) {
        for (int i = 0; i < lines.size(); i++) {
            String line = fontRenderer.trimStringToWidth(lines.get(i), Math.max(24, width));
            drawCenteredString(fontRenderer, line, centerX, topY + i * lineHeight, color);
        }
    }

    protected static List<PathSequence> getCustomSequencesForActiveSearchScope(String category,
            String selectedSubCategory, String normalizedQuery) {
        String normalizedCategory = normalizeText(category);
        String normalizedSelectedSubCategory = normalizeText(selectedSubCategory);
        if (normalizedQuery.isEmpty()) {
            return getCustomSequencesForSelection(normalizedCategory, normalizedSelectedSubCategory);
        }

        String effectiveScope = getEffectiveCustomSearchScope();
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(effectiveScope)) {
            return getCustomSequencesForSelection(normalizedCategory, normalizedSelectedSubCategory);
        }
        if (SEARCH_SCOPE_ALL_CATEGORIES.equals(effectiveScope)) {
            List<PathSequence> result = new ArrayList<>();
            for (String customCategory : getVisibleCustomCategoriesInDisplayOrder()) {
                result.addAll(getCustomSequencesForSelection(customCategory, ""));
            }
            return result;
        }
        return getCustomSequencesForSelection(normalizedCategory, "");
    }

    protected static boolean matchesCustomSequenceSearch(PathSequence sequence, String query) {
        return matchesCustomSequenceSearchNormalized(sequence, PinyinSearchHelper.normalizeQuery(query));
    }

    protected static List<CustomSectionModel> buildCustomSectionModels(String category, String selectedSubCategory) {
        String normalizedCategory = normalizeText(category);
        String normalizedSelectedSubCategory = normalizeText(selectedSubCategory);
        String normalizedQuery = PinyinSearchHelper.normalizeQuery(customSequenceSearchQuery);
        String effectiveScope = normalizedQuery.isEmpty() ? SEARCH_SCOPE_CURRENT_SUBCATEGORY
                : getEffectiveCustomSearchScope();
        boolean includeCategoryPrefix = !normalizedQuery.isEmpty()
                && SEARCH_SCOPE_ALL_CATEGORIES.equals(effectiveScope);
        boolean showEmptySections = normalizedQuery.isEmpty()
                || (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(effectiveScope)
                        && !normalizedSelectedSubCategory.isEmpty());

        List<PathSequence> filteredSequences = getCustomSequencesForActiveSearchScope(normalizedCategory,
                normalizedSelectedSubCategory, normalizedQuery);
        Map<String, List<PathSequence>> sectionMap = new LinkedHashMap<>();
        Map<String, String> sectionCategoryMap = new LinkedHashMap<>();
        Map<String, String> sectionSubCategoryMap = new LinkedHashMap<>();

        if (normalizedQuery.isEmpty()) {
            if (!normalizedSelectedSubCategory.isEmpty()) {
                String sectionKey = getCustomSectionKey(normalizedCategory, normalizedSelectedSubCategory);
                sectionMap.put(sectionKey, new ArrayList<>());
                sectionCategoryMap.put(sectionKey, normalizedCategory);
                sectionSubCategoryMap.put(sectionKey, normalizedSelectedSubCategory);
            } else {
                for (String subCategory : MainUiLayoutManager.getSubCategories(normalizedCategory)) {
                    String sectionKey = getCustomSectionKey(normalizedCategory, subCategory);
                    sectionMap.put(sectionKey, new ArrayList<>());
                    sectionCategoryMap.put(sectionKey, normalizedCategory);
                    sectionSubCategoryMap.put(sectionKey, normalizeText(subCategory));
                }
                String sectionKey = getCustomSectionKey(normalizedCategory, "");
                sectionMap.put(sectionKey, new ArrayList<>());
                sectionCategoryMap.put(sectionKey, normalizedCategory);
                sectionSubCategoryMap.put(sectionKey, "");
            }
        } else if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(effectiveScope)) {
            String sectionKey = getCustomSectionKey(normalizedCategory, normalizedSelectedSubCategory);
            sectionMap.put(sectionKey, new ArrayList<>());
            sectionCategoryMap.put(sectionKey, normalizedCategory);
            sectionSubCategoryMap.put(sectionKey, normalizedSelectedSubCategory);
        } else if (SEARCH_SCOPE_CURRENT_CATEGORY.equals(effectiveScope)) {
            for (String subCategory : MainUiLayoutManager.getSubCategories(normalizedCategory)) {
                String sectionKey = getCustomSectionKey(normalizedCategory, subCategory);
                sectionMap.put(sectionKey, new ArrayList<>());
                sectionCategoryMap.put(sectionKey, normalizedCategory);
                sectionSubCategoryMap.put(sectionKey, normalizeText(subCategory));
            }
            String sectionKey = getCustomSectionKey(normalizedCategory, "");
            sectionMap.put(sectionKey, new ArrayList<>());
            sectionCategoryMap.put(sectionKey, normalizedCategory);
            sectionSubCategoryMap.put(sectionKey, "");
        } else {
            for (String customCategory : getVisibleCustomCategoriesInDisplayOrder()) {
                for (String subCategory : MainUiLayoutManager.getSubCategories(customCategory)) {
                    String sectionKey = getCustomSectionKey(customCategory, subCategory);
                    sectionMap.put(sectionKey, new ArrayList<>());
                    sectionCategoryMap.put(sectionKey, customCategory);
                    sectionSubCategoryMap.put(sectionKey, normalizeText(subCategory));
                }
                String sectionKey = getCustomSectionKey(customCategory, "");
                sectionMap.put(sectionKey, new ArrayList<>());
                sectionCategoryMap.put(sectionKey, customCategory);
                sectionSubCategoryMap.put(sectionKey, "");
            }
        }

        for (PathSequence sequence : filteredSequences) {
            String sequenceSubCategory = normalizeSequenceSubCategory(sequence);
            if (!matchesCustomSequenceSearchNormalized(sequence, normalizedQuery)) {
                continue;
            }
            String sequenceCategory = normalizeText(sequence.getCategory());
            String sectionKey = getCustomSectionKey(sequenceCategory, sequenceSubCategory);
            if (!sectionMap.containsKey(sectionKey)) {
                sectionMap.put(sectionKey, new ArrayList<>());
                sectionCategoryMap.put(sectionKey, sequenceCategory);
                sectionSubCategoryMap.put(sectionKey, sequenceSubCategory);
            }
            sectionMap.get(sectionKey).add(sequence);
        }

        List<CustomSectionModel> models = new ArrayList<>();
        for (Map.Entry<String, List<PathSequence>> entry : sectionMap.entrySet()) {
            String sectionKey = entry.getKey();
            String sectionCategory = sectionCategoryMap.getOrDefault(sectionKey, normalizedCategory);
            String sectionSubCategory = normalizeText(sectionSubCategoryMap.getOrDefault(sectionKey, ""));
            List<PathSequence> sequences = entry.getValue();
            if (sectionSubCategory.isEmpty() && sequences.isEmpty()) {
                continue;
            }
            if (sequences.isEmpty() && !showEmptySections) {
                continue;
            }
            String title = buildCustomSectionDisplayTitle(sectionCategory, sectionSubCategory, includeCategoryPrefix);
            models.add(new CustomSectionModel(sectionKey, sectionCategory, title, sectionSubCategory, sequences));
        }

        return models;
    }

    protected static CustomPageLayout buildCustomPageLayout(OverlayMetrics m, int contentStartY) {
        CustomGridMetrics grid = computeCustomGridMetrics(m, contentStartY, currentCategory);
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        boolean searchExpanded = isCustomSearchExpanded();
        int topButtonWidth = GuiInventory.getTopButtonWidth(m, 5);
        int pathManagerButtonX = m.x + m.totalWidth - topButtonWidth - m.padding;
        int stopForegroundButtonX = pathManagerButtonX - topButtonWidth - m.padding;
        int stopBackgroundButtonX = stopForegroundButtonX - topButtonWidth - m.padding;
        int otherFeaturesButtonX = stopBackgroundButtonX - topButtonWidth - m.padding;
        int searchToggleHeight = m.topButtonHeight;
        int searchToggleWidth = topButtonWidth;
        int searchToggleX = otherFeaturesButtonX - searchToggleWidth - m.padding;
        int searchToggleY = m.y + scaleUi(4, m.scale);
        int secondRowY = searchToggleY + searchToggleHeight + scaleUi(4, m.scale);
        int searchFieldHeight = searchToggleHeight;
        int searchFieldY = secondRowY;
        int searchScopeHeight = searchToggleHeight;
        int searchScopeY = secondRowY;
        int searchScopeWidth = searchExpanded
                ? Math.max(scaleUi(42, m.scale), fontRenderer.getStringWidth(getCompactCustomSearchScopeLabel()) + 16)
                : 0;
        int searchRightX = pathManagerButtonX + topButtonWidth;
        int searchScopeX = searchExpanded ? searchRightX - searchScopeWidth : 0;
        int maxFieldWidth = Math.max(scaleUi(100, m.scale), scaleUi(210, m.scale));
        int minFieldLeft = m.x + scaleUi(96, m.scale);
        int searchFieldWidth = 0;
        int searchFieldX = 0;
        if (searchExpanded) {
            int preferredFieldLeft = searchToggleX;
            int maxAllowedFieldWidth = Math.max(48, searchScopeX - 4 - Math.max(minFieldLeft, preferredFieldLeft));
            searchFieldWidth = Math.min(maxFieldWidth, maxAllowedFieldWidth);
            searchFieldX = searchScopeX - 4 - searchFieldWidth;
        }
        int toolbarHeight = selectedCustomSequenceNames.isEmpty() ? 0
                : Math.max(scaleUi(18, m.scale), m.itemButtonHeight - 2);
        int toolbarY = toolbarHeight > 0 ? contentStartY + m.padding + 6 : 0;
        int contentTopY = contentStartY + m.padding + 6 + (toolbarHeight > 0 ? toolbarHeight + 8 : 0);
        int contentBottomY = m.y + m.totalHeight - scaleUi(34, m.scale);
        int availableHeight = Math.max(scaleUi(40, m.scale), contentBottomY - contentTopY);
        int headerHeight = Math.max(scaleUi(18, m.scale), m.itemButtonHeight - 2);
        int sectionGap = Math.max(5, grid.gap);
        int emptyStateHeight = Math.max(scaleUi(44, m.scale), grid.cardHeight);

        grid.startX = m.contentPanelX + m.padding + 6;
        grid.startY = contentTopY;
        grid.width = Math.max(120, m.contentPanelRight - grid.startX - m.padding - 6);
        grid.height = availableHeight;
        grid.cardWidth = (grid.width - grid.gap * Math.max(0, grid.columns - 1)) / grid.columns;
        while (grid.columns > 1 && grid.cardWidth < scaleUi(54, m.scale)) {
            grid.columns--;
            grid.cardWidth = (grid.width - grid.gap * Math.max(0, grid.columns - 1)) / grid.columns;
        }

        List<CustomSectionModel> models = buildCustomSectionModels(currentCategory, currentCustomSubCategory);
        List<List<CustomSectionChunk>> pages = new ArrayList<>();
        List<CustomSectionChunk> currentPageSections = new ArrayList<>();
        int remainingHeight = availableHeight;

        for (CustomSectionModel model : models) {
            boolean expanded = isCustomSectionExpanded(model.key);
            if (!expanded) {
                int usedHeight = headerHeight + 8;
                if (!currentPageSections.isEmpty() && usedHeight > remainingHeight) {
                    pages.add(currentPageSections);
                    currentPageSections = new ArrayList<>();
                    remainingHeight = availableHeight;
                }
                currentPageSections.add(new CustomSectionChunk(model, Collections.emptyList(), false));
                remainingHeight -= usedHeight + sectionGap;
                continue;
            }

            if (model.sequences.isEmpty()) {
                int usedHeight = headerHeight + 8 + emptyStateHeight;
                if (!currentPageSections.isEmpty() && usedHeight > remainingHeight) {
                    pages.add(currentPageSections);
                    currentPageSections = new ArrayList<>();
                    remainingHeight = availableHeight;
                }
                currentPageSections.add(new CustomSectionChunk(model, Collections.emptyList(), false));
                remainingHeight -= usedHeight + sectionGap;
                continue;
            }

            int offset = 0;
            while (offset < model.sequences.size()) {
                int headerReservedHeight = headerHeight + 8;
                int usableCardHeight = remainingHeight - headerReservedHeight - 6;
                int rowsFit = Math.max(1, (usableCardHeight + grid.gap) / Math.max(1, grid.cardHeight + grid.gap));
                int maxCards = Math.max(1, rowsFit * grid.columns);
                int count = Math.min(model.sequences.size() - offset, maxCards);
                int rowsUsed = Math.max(1, (count + grid.columns - 1) / grid.columns);
                int usedHeight = headerReservedHeight + rowsUsed * grid.cardHeight
                        + Math.max(0, rowsUsed - 1) * grid.gap + 6;

                if (!currentPageSections.isEmpty() && usedHeight > remainingHeight) {
                    pages.add(currentPageSections);
                    currentPageSections = new ArrayList<>();
                    remainingHeight = availableHeight;
                    continue;
                }

                currentPageSections.add(
                        new CustomSectionChunk(model, model.sequences.subList(offset, offset + count), offset > 0));
                remainingHeight -= usedHeight + sectionGap;
                offset += count;

                if (offset < model.sequences.size()) {
                    pages.add(currentPageSections);
                    currentPageSections = new ArrayList<>();
                    remainingHeight = availableHeight;
                }
            }
        }

        if (!currentPageSections.isEmpty() || pages.isEmpty()) {
            pages.add(currentPageSections);
        }

        int totalPages = Math.max(1, pages.size());
        int pageIndex = MathHelper.clamp(currentPage, 0, totalPages - 1);
        List<CustomSectionChunk> pageSections = pages.get(pageIndex);
        return new CustomPageLayout(grid, pageSections, totalPages, searchToggleX, searchToggleY, searchToggleWidth,
                searchToggleHeight, searchFieldX, searchFieldY, searchFieldWidth, searchFieldHeight, searchScopeX,
                searchScopeY, searchScopeWidth, searchScopeHeight, toolbarY, toolbarHeight);
    }

    protected static CustomSectionRenderInfo findCustomSectionHeaderAt(int mouseX, int mouseY) {
        for (CustomSectionRenderInfo info : visibleCustomSectionHeaders) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    protected static String formatRelativeTime(long lastOpenedAt) {
        if (lastOpenedAt <= 0L) {
            return "";
        }
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - lastOpenedAt);
        long seconds = elapsedMillis / 1000L;
        if (seconds < 45L) {
            return "刚刚";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "m";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "h";
        }
        long days = hours / 24L;
        return days + "d";
    }

    protected static boolean isSequenceRecentlyOpened(MainUiLayoutManager.SequenceOpenStats stats) {
        return stats != null && stats.lastOpenedAt > 0L
                && System.currentTimeMillis() - stats.lastOpenedAt <= RECENT_OPEN_HIGHLIGHT_WINDOW_MS;
    }

    protected static String getRunningCustomSequenceName() {
        if (!PathSequenceEventListener.instance.isTracking()
                || PathSequenceEventListener.instance.currentSequence == null
                || !PathSequenceEventListener.instance.currentSequence.isCustom()) {
            return "";
        }
        return normalizeText(PathSequenceEventListener.instance.currentSequence.getName());
    }

    protected static boolean isCustomCategoryRunning(String category) {
        String runningSequenceName = getRunningCustomSequenceName();
        if (runningSequenceName.isEmpty()) {
            return false;
        }
        PathSequence sequence = PathSequenceManager.getSequence(runningSequenceName);
        return sequence != null && sequence.isCustom()
                && normalizeText(category).equals(normalizeText(sequence.getCategory()));
    }

    protected static boolean isCustomCategoryRecentlyOpened(String category) {
        String normalizedCategory = normalizeText(category);
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (sequence != null && sequence.isCustom()
                    && normalizedCategory.equals(normalizeText(sequence.getCategory()))
                    && isSequenceRecentlyOpened(MainUiLayoutManager.getSequenceStats(sequence.getName()))) {
                return true;
            }
        }
        return false;
    }

    protected static int getCurrentTotalPages(OverlayMetrics m, int contentStartY) {
        if (isCustomCategorySelection()) {
            return buildCustomPageLayout(m, contentStartY).totalPages;
        }
        return getTotalPagesForCategory(currentCategory);
    }

    protected static int getTotalPagesForCategory(String category) {
        if (isCommonCategory(category)) {
            return Math.max(1, buildCommonContentPages().size());
        }
        List<String> items = categoryItems.get(category);
        return Math.max(1, items == null ? 1 : (items.size() + 17) / 18);
    }

    protected static void clampCurrentPageToCategoryBounds() {
        int totalPages;
        if (isCustomCategorySelection()) {
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution resolution = new ScaledResolution(mc);
            OverlayMetrics metrics = getCurrentOverlayMetrics(resolution.getScaledWidth(),
                    resolution.getScaledHeight());
            totalPages = Math.max(1, buildCustomPageLayout(metrics, metrics.contentStartY).totalPages);
        } else {
            totalPages = getTotalPagesForCategory(currentCategory);
        }
        currentPage = MathHelper.clamp(currentPage, 0, Math.max(0, totalPages - 1));
        CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
        sLastPage = currentPage;
        sLastCategory = currentCategory;
    }

    protected static String findBaseItemName(String command) {
        for (Map.Entry<String, List<String>> entry : categoryItems.entrySet()) {
            List<String> commands = entry.getValue();
            List<String> names = categoryItemNames.get(entry.getKey());
            if (commands == null || names == null) {
                continue;
            }
            for (int i = 0; i < commands.size() && i < names.size(); i++) {
                if (command.equals(commands.get(i))) {
                    return names.get(i);
                }
            }
        }
        return command;
    }

    protected static String getCommonDisplayName(String command) {
        String baseName = findBaseItemName(command);
        if ("toggle_auto_fishing".equals(command)) {
            return AutoFishingHandler.enabled ? I18n.format("gui.inventory.auto_fishing.on") : baseName;
        }
        if ("toggle_kill_aura".equals(command)) {
            return KillAuraHandler.enabled ? I18n.format("gui.inventory.kill_aura.on") : baseName;
        }
        if ("toggle_fly".equals(command)) {
            return FlyHandler.enabled ? I18n.format("gui.inventory.fly.on") : baseName;
        }
        return baseName;
    }

    protected static GuiTheme.UiState getCommonItemState(String command, boolean isHovering) {
        boolean active = false;
        if ("autoeat".equals(command)) {
            active = AutoEatHandler.autoEatEnabled;
        } else if ("toggle_auto_fishing".equals(command)) {
            active = AutoFishingHandler.enabled;
        } else if ("toggle_mouse_detach".equals(command)) {
            active = ModConfig.isMouseDetached;
        } else if ("followconfig".equals(command)) {
            active = AutoFollowHandler.getActiveRule() != null;
        } else if ("toggle_kill_aura".equals(command)) {
            active = KillAuraHandler.enabled;
        } else if ("toggle_fly".equals(command)) {
            active = FlyHandler.enabled;
        } else if ("toggle_auto_pickup".equals(command)) {
            active = AutoPickupHandler.globalEnabled;
        } else if ("conditional_execution".equals(command)) {
            active = ConditionalExecutionHandler.isGloballyEnabled();
        } else if ("toggle_auto_use_item".equals(command)) {
            active = AutoUseItemHandler.globalEnabled;
        } else if ("toggle_server_feature_visibility".equals(command)) {
            active = ServerFeatureVisibilityManager.isAnyRuleEnabled();
        } else if ("baritone_parkour".equals(command)) {
            active = BaritoneParkourSettingsHelper.isParkourModeEnabled();
        }

        if (active) {
            return GuiTheme.UiState.SUCCESS;
        }
        return isHovering ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    protected static String drawGroupedCommonItems(OverlayMetrics m, int contentStartY, int mouseX, int mouseY,
            FontRenderer fontRenderer) {
        List<List<GroupedItemSection>> pages = buildCommonContentPages();
        int totalPages = Math.max(1, pages.size());
        currentPage = MathHelper.clamp(currentPage, 0, totalPages - 1);

        List<GroupedItemSection> pageSections = pages.get(currentPage);
        int sectionX = m.contentPanelX + m.padding;
        int sectionY = contentStartY + m.padding;
        int sectionWidth = Math.max(140, m.contentPanelRight - sectionX - m.padding);
        int sectionGap = Math.max(4, m.gap - 2);
        int headerHeight = Math.max(18, m.itemButtonHeight - 2);
        int itemGap = Math.max(4, m.gap - 2);
        int innerPadding = 6;
        String hoveredTooltip = null;

        for (GroupedItemSection section : pageSections) {
            boolean expanded = commonSectionExpanded.getOrDefault(section.key, Boolean.TRUE);
            int itemRows = expanded ? getCommonSectionItemRows(section) : 0;
            int sectionHeight = headerHeight + 8;
            if (expanded && itemRows > 0) {
                sectionHeight += itemRows * m.itemButtonHeight + (itemRows - 1) * itemGap + 8;
            }

            int bg = 0x44202A36;
            int border = 0xFF35536C;
            drawRect(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, bg);
            drawHorizontalLine(sectionX, sectionX + sectionWidth, sectionY, 0xFF4FA6D9);
            drawHorizontalLine(sectionX, sectionX + sectionWidth, sectionY + sectionHeight, border);
            drawVerticalLine(sectionX, sectionY, sectionY + sectionHeight, border);
            drawVerticalLine(sectionX + sectionWidth, sectionY, sectionY + sectionHeight, border);

            int headerBottom = sectionY + headerHeight;
            drawHorizontalLine(sectionX, sectionX + sectionWidth, headerBottom, 0x664FA6D9);

            boolean hoveringHeader = isMouseOver(mouseX, mouseY, sectionX, sectionY, sectionWidth, headerHeight + 2);
            drawString(fontRenderer, section.title, sectionX + 8,
                    sectionY + (headerHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
            drawString(fontRenderer, expanded ? "▲" : "▼", sectionX + sectionWidth - 12,
                    sectionY + (headerHeight - fontRenderer.FONT_HEIGHT) / 2, hoveringHeader ? 0xFFFFFFFF : 0xFF9FDFFF);

            if (expanded) {
                int buttonAreaX = sectionX + innerPadding;
                int buttonAreaY = headerBottom + 6;
                int buttonWidth = Math.max(60, (sectionWidth - innerPadding * 2 - itemGap * 2) / 3);

                for (int i = 0; i < section.commands.size(); i++) {
                    String command = section.commands.get(i);
                    int col = i % 3;
                    int row = i / 3;
                    int buttonX = buttonAreaX + col * (buttonWidth + itemGap);
                    int buttonY = buttonAreaY + row * (m.itemButtonHeight + itemGap);
                    boolean isHoveringItem = isMouseOver(mouseX, mouseY, buttonX, buttonY, buttonWidth,
                            m.itemButtonHeight);

                    GuiTheme.drawButtonFrame(buttonX, buttonY, buttonWidth, m.itemButtonHeight,
                            getCommonItemState(command, isHoveringItem));
                    GuiTheme.drawCardHighlight(buttonX, buttonY, buttonWidth, m.itemButtonHeight, isHoveringItem);

                    if (isHoveringItem) {
                        String tooltip = itemTooltips.get(command);
                        if (tooltip != null && !tooltip.isEmpty()) {
                            hoveredTooltip = tooltip;
                        }
                    }

                    if ("setloop".equals(command)) {
                        drawCenteredString(fontRenderer, getCommonDisplayName(command), buttonX + buttonWidth / 2,
                                buttonY + 2, 0xFFFFFFFF);
                        String loopText = (loopCount < 0) ? I18n.format("gui.inventory.loop.infinite")
                                : (loopCount == 0) ? I18n.format("gui.inventory.loop.off")
                                        : I18n.format("gui.inventory.loop.count", loopCount);
                        drawCenteredString(fontRenderer, loopText, buttonX + buttonWidth / 2, buttonY + 11, 0xFFDDDDDD);
                    } else {
                        drawCenteredString(fontRenderer, getCommonDisplayName(command), buttonX + buttonWidth / 2,
                                buttonY + (m.itemButtonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                    }
                }
            }

            sectionY += sectionHeight + sectionGap;
        }

        return hoveredTooltip;
    }

    protected static void drawCategoryTree(OverlayMetrics m, int contentStartY, int mouseX, int mouseY) {
        visibleCategoryRows.clear();

        int categoryPanelX = m.x + m.padding;
        int categoryPanelY = contentStartY;
        int categoryPanelWidth = m.categoryPanelWidth;
        int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;
        int categoryButtonWidth = m.categoryButtonWidth;
        int categoryButtonHeight = m.categoryButtonHeight;
        int categoryItemHeight = m.categoryItemHeight;
        int categoryStartX = categoryPanelX + (categoryPanelWidth - categoryButtonWidth) / 2;
        int categoryStartY = contentStartY + m.padding;

        List<CategoryTreeRow> allRows = buildVisibleCategoryTreeRows();
        int categoryListVisibleHeight = categoryPanelHeight - 18;
        int visibleCategories = Math.max(1, categoryListVisibleHeight / categoryItemHeight);
        maxCategoryScroll = Math.max(0, allRows.size() - visibleCategories);
        categoryScrollOffset = MathHelper.clamp(categoryScrollOffset, 0, maxCategoryScroll);
        categoryDividerBounds = new Rectangle(m.categoryDividerX, categoryPanelY + 2, m.categoryDividerWidth,
                Math.max(12, categoryPanelHeight - 4));

        for (int i = 0; i < visibleCategories; i++) {
            int index = i + categoryScrollOffset;
            if (index >= allRows.size()) {
                break;
            }

            CategoryTreeRow row = allRows.get(index);
            int buttonY = categoryStartY + i * categoryItemHeight;
            int rowIndent = row.isSubCategory() ? scaleUi(8, m.scale) : 0;
            int buttonX = categoryStartX + rowIndent;
            int buttonWidth = categoryButtonWidth - rowIndent;
            Rectangle bounds = new Rectangle(buttonX, buttonY, buttonWidth, categoryButtonHeight);
            row.bounds = bounds;
            visibleCategoryRows.add(row);

            boolean hovered = bounds.contains(mouseX, mouseY);
            boolean selected = row.category.equals(currentCategory) && (row.isSubCategory()
                    ? normalizeText(row.subCategory).equals(normalizeText(currentCustomSubCategory))
                    : normalizeText(currentCustomSubCategory).isEmpty());

            GuiTheme.UiState state = selected ? GuiTheme.UiState.SELECTED
                    : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            GuiTheme.drawButtonFrame(buttonX, buttonY, buttonWidth, categoryButtonHeight, state);
            boolean pinnedCategory = row.isCustomCategoryRoot() && MainUiLayoutManager.isPinned(row.category);
            boolean runningCategory = row.isCustomCategoryRoot() && isCustomCategoryRunning(row.category);
            boolean recentCategory = row.isCustomCategoryRoot() && isCustomCategoryRecentlyOpened(row.category);
            int accentColor = pinnedCategory ? 0xFFF5C15A : getStableAccentColor(getCategoryRowStorageKey(row));
            drawRect(buttonX + 2, buttonY + 2, buttonX + 5, buttonY + categoryButtonHeight - 2, accentColor);
            if (runningCategory) {
                int pulseColor = ((colorChangeTicker / 4) % 2 == 0) ? 0xFF6BD8FF : 0xFF2AA6D9;
                drawRect(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + 3, pulseColor);
            } else if (recentCategory) {
                drawRect(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + 3, 0xCCF0A348);
            }

            if (currentSequenceDropTarget != null
                    && currentSequenceDropTarget.matches(row.category, row.subCategory)) {
                drawRect(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY, 0xFF77D4FF);
                drawRect(buttonX - 1, buttonY + categoryButtonHeight, buttonX + buttonWidth + 1,
                        buttonY + categoryButtonHeight + 1, 0xFF77D4FF);
            }
            if (isDraggingCategoryRow && currentCategorySortDropTarget != null
                    && currentCategorySortDropTarget.category.equals(row.category)
                    && currentCategorySortDropTarget.subCategory.equals(row.subCategory)) {
                drawRect(buttonX, buttonY, buttonX + buttonWidth, buttonY + categoryButtonHeight, 0x223D6A86);
                int lineY = currentCategorySortDropAfter ? buttonY + categoryButtonHeight + 1 : buttonY - 1;
                drawRect(buttonX - 2, lineY, buttonX + buttonWidth + 2, lineY + 2, 0xFF7FD1FF);
            }

            boolean hasChildren = row.isCustomCategoryRoot()
                    && !MainUiLayoutManager.getSubCategories(row.category).isEmpty();
            if (hasChildren) {
                String arrow = MainUiLayoutManager.isCollapsed(row.category) ? ">" : "v";
                drawString(Minecraft.getMinecraft().fontRenderer, arrow, buttonX + 8,
                        buttonY + (categoryButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                        hovered ? 0xFFFFFFFF : 0xFFAEDBFF);
            }

            String label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(getCategoryRowDisplayLabel(row),
                    buttonWidth - (hasChildren ? 30 : 20));
            drawCenteredString(Minecraft.getMinecraft().fontRenderer, label, buttonX + buttonWidth / 2,
                    buttonY + (categoryButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                    0xFFFFFFFF);

            if (runningCategory) {
                drawString(Minecraft.getMinecraft().fontRenderer, "▶", buttonX + buttonWidth - 11,
                        buttonY + (categoryButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                        0xFF7FD1FF);
            } else if (pinnedCategory) {
                drawString(Minecraft.getMinecraft().fontRenderer, "★", buttonX + buttonWidth - 12,
                        buttonY + (categoryButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                        0xFFFFD47A);
            } else if (recentCategory) {
                drawString(Minecraft.getMinecraft().fontRenderer, "●", buttonX + buttonWidth - 11,
                        buttonY + (categoryButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                        0xFFF0A348);
            }
        }

        if (maxCategoryScroll > 0) {
            int scrollbarX = categoryPanelX + categoryPanelWidth - 6;
            int scrollbarY = categoryPanelY + 5;
            int scrollbarHeight = categoryPanelHeight - 10;
            int thumbHeight = Math.max(10, (int) ((float) visibleCategories / allRows.size() * scrollbarHeight));
            int thumbY = scrollbarY
                    + (int) ((float) categoryScrollOffset / maxCategoryScroll * (scrollbarHeight - thumbHeight));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 4, scrollbarHeight, thumbY, thumbHeight);
        }

        boolean dividerHovered = categoryDividerBounds != null && categoryDividerBounds.contains(mouseX, mouseY);
        int dividerColor = isDraggingCategoryDivider ? 0xFF7FD1FF : (dividerHovered ? 0xFF5AAEE5 : 0x664A708E);
        drawRect(categoryDividerBounds.x, categoryDividerBounds.y,
                categoryDividerBounds.x + categoryDividerBounds.width,
                categoryDividerBounds.y + categoryDividerBounds.height, dividerColor);

        drawCenteredString(Minecraft.getMinecraft().fontRenderer, "右键空白处新建分类", categoryPanelX + categoryPanelWidth / 2,
                categoryPanelY + categoryPanelHeight - 12, 0xAA9FB2C8);
    }

    protected static String drawCustomSequenceCards(OverlayMetrics m, int contentStartY, int mouseX, int mouseY,
            FontRenderer fontRenderer) {
        visibleCustomSequenceCards.clear();
        visibleCustomSectionHeaders.clear();
        visibleCustomSectionDropTargets.clear();
        visibleCustomSearchScopeButtons.clear();
        visibleCustomToolbarButtons.clear();
        visibleCustomEmptySectionButtons.clear();
        customSearchClearButtonBounds = null;
        customSearchToggleButtonBounds = null;
        pruneSelectedCustomSequences();

        CustomPageLayout layout = buildCustomPageLayout(m, contentStartY);
        boolean searchExpanded = isCustomSearchExpanded();
        boolean searchActive = !isBlank(customSequenceSearchQuery);
        String toggleLabel = searchExpanded ? "收" : "搜";
        customSearchToggleButtonBounds = new Rectangle(layout.searchToggleX, layout.searchToggleY,
                layout.searchToggleWidth, layout.searchToggleHeight);
        boolean toggleHovered = customSearchToggleButtonBounds.contains(mouseX, mouseY);
        GuiTheme.UiState toggleState = searchExpanded || searchActive ? GuiTheme.UiState.SELECTED
                : (toggleHovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        GuiTheme.drawButtonFrame(customSearchToggleButtonBounds.x, customSearchToggleButtonBounds.y,
                customSearchToggleButtonBounds.width, customSearchToggleButtonBounds.height, toggleState);
        drawCenteredString(fontRenderer, toggleLabel,
                customSearchToggleButtonBounds.x + customSearchToggleButtonBounds.width / 2,
                customSearchToggleButtonBounds.y
                        + (customSearchToggleButtonBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                0xFFFFFFFF);

        if (searchExpanded) {
            ensureCustomSequenceSearchField(layout.searchFieldX, layout.searchFieldY, layout.searchFieldWidth,
                    layout.searchFieldHeight);
            if (customSequenceSearchField != null) {
                customSequenceSearchField.updateCursorCounter();
                customSequenceSearchField.drawTextBox();
                if (customSequenceSearchField.getText().trim().isEmpty() && !customSequenceSearchField.isFocused()) {
                    drawString(fontRenderer, "名称/拼音", layout.searchFieldX + 5, layout.searchFieldY + 5, 0x779FB2C8);
                }
            }
            if (!isBlank(customSequenceSearchQuery) && customSequenceSearchField != null) {
                int clearSize = Math.max(12, layout.searchFieldHeight - 4);
                int clearX = customSequenceSearchField.x + customSequenceSearchField.width - clearSize - 3;
                int clearY = customSequenceSearchField.y + (customSequenceSearchField.height - clearSize) / 2;
                customSearchClearButtonBounds = new Rectangle(clearX, clearY, clearSize, clearSize);
                boolean hoverClear = customSearchClearButtonBounds.contains(mouseX, mouseY);
                drawRect(clearX, clearY, clearX + clearSize, clearY + clearSize, hoverClear ? 0x99556F84 : 0x663C5366);
                drawCenteredString(fontRenderer, "x", clearX + clearSize / 2,
                        clearY + (clearSize - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
            }

            Rectangle scopeBounds = new Rectangle(layout.searchScopeX, layout.searchScopeY, layout.searchScopeWidth,
                    layout.searchScopeHeight);
            boolean hoveredScope = scopeBounds.contains(mouseX, mouseY);
            GuiTheme.UiState scopeState = hoveredScope ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
            GuiTheme.drawButtonFrame(scopeBounds.x, scopeBounds.y, scopeBounds.width, scopeBounds.height, scopeState);
            drawCenteredString(fontRenderer, getCompactCustomSearchScopeLabel(), scopeBounds.x + scopeBounds.width / 2,
                    scopeBounds.y + (scopeBounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
            visibleCustomSearchScopeButtons.add(
                    new CustomButtonRenderInfo("cycle_scope", currentCategory, currentCustomSubCategory, scopeBounds));
        } else if (customSequenceSearchField != null) {
            customSequenceSearchField.setFocused(false);
        }

        if (layout.toolbarHeight > 0) {
            int toolbarX = m.contentPanelX + m.padding + 6;
            int toolbarWidth = Math.max(120, m.contentPanelRight - toolbarX - m.padding - 6);
            int toolbarY = layout.toolbarY;
            drawRect(toolbarX, toolbarY, toolbarX + toolbarWidth, toolbarY + layout.toolbarHeight, 0x332C3E50);
            drawHorizontalLine(toolbarX, toolbarX + toolbarWidth, toolbarY, 0x664FA6D9);
            drawHorizontalLine(toolbarX, toolbarX + toolbarWidth, toolbarY + layout.toolbarHeight, 0x5535536C);

            String selectedText = "已选 " + selectedCustomSequenceNames.size();
            drawString(fontRenderer, selectedText, toolbarX + 6,
                    toolbarY + (layout.toolbarHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);

            String[] actionLabels = new String[] { "全选本页", "清空", "移动到", "复制到", "更多", "删除" };
            String[] actionKeys = new String[] { "select_page", "clear_selection", "batch_move", "batch_copy",
                    "batch_more", "batch_delete" };
            int buttonX = toolbarX + fontRenderer.getStringWidth(selectedText) + 12;
            for (int i = 0; i < actionLabels.length; i++) {
                int buttonWidth = Math.max(36, fontRenderer.getStringWidth(actionLabels[i]) + 14);
                if (buttonX + buttonWidth > toolbarX + toolbarWidth) {
                    break;
                }
                Rectangle buttonBounds = new Rectangle(buttonX, toolbarY + 1, buttonWidth, layout.toolbarHeight - 2);
                boolean hovered = buttonBounds.contains(mouseX, mouseY);
                GuiTheme.UiState state = "batch_delete".equals(actionKeys[i])
                        ? (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.DANGER)
                        : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                GuiTheme.drawButtonFrame(buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height,
                        state);
                drawCenteredString(fontRenderer, actionLabels[i], buttonBounds.x + buttonBounds.width / 2,
                        buttonBounds.y + (buttonBounds.height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                visibleCustomToolbarButtons.add(new CustomButtonRenderInfo(actionKeys[i], currentCategory,
                        currentCustomSubCategory, buttonBounds));
                buttonX += buttonWidth + 4;
            }
        }

        int totalPages = layout.totalPages;
        currentPage = MathHelper.clamp(currentPage, 0, totalPages - 1);
        CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);

        List<CustomSectionModel> allSections = buildCustomSectionModels(currentCategory, currentCustomSubCategory);
        if (allSections.isEmpty()) {
            String emptyText = isBlank(customSequenceSearchQuery)
                    ? (normalizeText(currentCustomSubCategory).isEmpty() ? "当前分类下还没有自定义路径序列" : "当前子分类下还没有路径序列")
                    : "没有匹配当前搜索条件的路径序列";
            GuiTheme.drawEmptyState((m.contentPanelX + m.contentPanelRight) / 2,
                    layout.grid.startY + scaleUi(24, m.scale), emptyText, fontRenderer);
            if (isBlank(customSequenceSearchQuery)) {
                int buttonWidth = Math.max(58, fontRenderer.getStringWidth("新建序列") + 20);
                int buttonHeight = Math.max(scaleUi(18, m.scale), m.itemButtonHeight - 2);
                int buttonGap = 8;
                int subCategoryButtonWidth = Math.max(66, fontRenderer.getStringWidth("新建子分类") + 20);
                int totalButtonsWidth = buttonWidth + buttonGap + subCategoryButtonWidth;
                int buttonX = (m.contentPanelX + m.contentPanelRight - totalButtonsWidth) / 2;
                int buttonY = layout.grid.startY + scaleUi(42, m.scale);
                Rectangle subCategoryButtonBounds = new Rectangle(buttonX, buttonY, subCategoryButtonWidth,
                        buttonHeight);
                boolean hoveredSubCategory = subCategoryButtonBounds.contains(mouseX, mouseY);
                GuiTheme.drawButtonFrame(subCategoryButtonBounds.x, subCategoryButtonBounds.y,
                        subCategoryButtonBounds.width, subCategoryButtonBounds.height,
                        hoveredSubCategory ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawCenteredString(fontRenderer, "新建子分类", subCategoryButtonBounds.x + subCategoryButtonBounds.width / 2,
                        subCategoryButtonBounds.y + (buttonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                visibleCustomEmptySectionButtons.add(new CustomButtonRenderInfo("create_subcategory", currentCategory,
                        currentCustomSubCategory, subCategoryButtonBounds));

                Rectangle buttonBounds = new Rectangle(buttonX + subCategoryButtonWidth + buttonGap, buttonY,
                        buttonWidth, buttonHeight);
                boolean hovered = buttonBounds.contains(mouseX, mouseY);
                GuiTheme.drawButtonFrame(buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height,
                        hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawCenteredString(fontRenderer, "新建序列", buttonBounds.x + buttonBounds.width / 2,
                        buttonBounds.y + (buttonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                visibleCustomEmptySectionButtons.add(new CustomButtonRenderInfo("create_sequence", currentCategory,
                        currentCustomSubCategory, buttonBounds));
            }
            return null;
        }

        String hoveredTooltip = null;
        int sectionX = layout.grid.startX;
        int sectionWidth = layout.grid.width;
        int sectionY = layout.grid.startY;
        int headerHeight = Math.max(scaleUi(18, m.scale), m.itemButtonHeight - 2);
        int sectionGap = Math.max(5, layout.grid.gap);
        int emptyStateHeight = Math.max(scaleUi(44, m.scale), layout.grid.cardHeight);

        for (CustomSectionChunk chunk : layout.sections) {
            boolean expanded = isCustomSectionExpanded(chunk.model.key);
            int headerY = sectionY;
            int headerBottom = headerY + headerHeight;
            visibleCustomSectionHeaders.add(new CustomSectionRenderInfo(chunk.model.key, chunk.model.title,
                    chunk.model.subCategory,
                    new Rectangle(sectionX, headerY, sectionWidth, headerHeight + 2)));

            boolean emptySection = expanded && chunk.pageSequences.isEmpty();
            int totalRows = expanded && !emptySection
                    ? Math.max(1, (chunk.pageSequences.size() + layout.grid.columns - 1) / layout.grid.columns)
                    : 0;
            int sectionHeight = headerHeight + 8
                    + (expanded
                            ? (emptySection ? emptyStateHeight
                                    : totalRows * layout.grid.cardHeight + Math.max(0, totalRows - 1) * layout.grid.gap
                                            + 6)
                            : 0);
            visibleCustomSectionDropTargets.add(new CustomSequenceDropTarget(chunk.model.category,
                    chunk.model.subCategory, new Rectangle(sectionX, sectionY, sectionWidth, sectionHeight)));
            drawRect(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, 0x44202A36);
            drawHorizontalLine(sectionX, sectionX + sectionWidth, sectionY, getStableAccentColor(chunk.model.key));
            drawHorizontalLine(sectionX, sectionX + sectionWidth, sectionY + sectionHeight, 0xFF35536C);
            drawVerticalLine(sectionX, sectionY, sectionY + sectionHeight, 0xFF35536C);
            drawVerticalLine(sectionX + sectionWidth, sectionY, sectionY + sectionHeight, 0xFF35536C);
            drawHorizontalLine(sectionX, sectionX + sectionWidth, headerBottom, 0x664FA6D9);
            if (currentSequenceDropTarget != null
                    && currentSequenceDropTarget.matches(chunk.model.category, chunk.model.subCategory)) {
                drawRect(sectionX - 1, sectionY - 1, sectionX + sectionWidth + 1, sectionY, 0xFF77D4FF);
                drawRect(sectionX - 1, sectionY + sectionHeight, sectionX + sectionWidth + 1,
                        sectionY + sectionHeight + 1, 0xFF77D4FF);
                drawRect(sectionX - 1, sectionY, sectionX, sectionY + sectionHeight, 0xFF77D4FF);
                drawRect(sectionX + sectionWidth, sectionY, sectionX + sectionWidth + 1, sectionY + sectionHeight,
                        0xFF77D4FF);
            }

            boolean hoveringHeader = mouseX >= sectionX && mouseX < sectionX + sectionWidth && mouseY >= headerY
                    && mouseY < headerY + headerHeight + 2;
            int accentColor = getStableAccentColor(chunk.model.key);
            drawRect(sectionX + 6, sectionY + 5, sectionX + 10, headerBottom - 3, accentColor);
            String sectionTitle = chunk.model.title + (chunk.continuation ? "（续）" : "");
            int arrowX = sectionX + sectionWidth - 12;
            int titleMaxWidth = Math.max(48, arrowX - (sectionX + 16) - 8);
            String displayTitle = fontRenderer.trimStringToWidth(sectionTitle, titleMaxWidth);
            drawString(fontRenderer, displayTitle, sectionX + 16,
                    headerY + (headerHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
            drawString(fontRenderer, expanded ? "v" : ">", arrowX,
                    headerY + (headerHeight - fontRenderer.FONT_HEIGHT) / 2, hoveringHeader ? 0xFFFFFFFF : 0xFF9FDFFF);

            if (expanded) {
                int cardAreaY = headerBottom + 6;
                if (emptySection) {
                    String message = isBlank(customSequenceSearchQuery) ? "该子分类暂无序列，可拖入或直接新建" : "该子分类下没有匹配当前搜索条件的序列";
                    drawCenteredString(fontRenderer, message, sectionX + sectionWidth / 2,
                            cardAreaY + scaleUi(6, m.scale), 0xFFB8CCE0);
                    if (isBlank(customSequenceSearchQuery)) {
                        int buttonWidth = Math.max(58, fontRenderer.getStringWidth("新建序列") + 20);
                        int moveButtonWidth = Math.max(76, fontRenderer.getStringWidth("移动现有序列进来") + 20);
                        int buttonHeight = Math.max(scaleUi(18, m.scale), m.itemButtonHeight - 2);
                        int buttonGap = 8;
                        int totalButtonsWidth = buttonWidth + buttonGap + moveButtonWidth;
                        int buttonX = sectionX + (sectionWidth - totalButtonsWidth) / 2;
                        int buttonY = cardAreaY + scaleUi(18, m.scale);
                        Rectangle moveButtonBounds = new Rectangle(buttonX, buttonY, moveButtonWidth, buttonHeight);
                        boolean hoveredMove = moveButtonBounds.contains(mouseX, mouseY);
                        GuiTheme.drawButtonFrame(moveButtonBounds.x, moveButtonBounds.y, moveButtonBounds.width,
                                moveButtonBounds.height,
                                hoveredMove ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                        drawCenteredString(fontRenderer, "移动现有序列进来", moveButtonBounds.x + moveButtonBounds.width / 2,
                                moveButtonBounds.y + (buttonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                        visibleCustomEmptySectionButtons.add(new CustomButtonRenderInfo("move_existing_into_section",
                                currentCategory, chunk.model.subCategory, moveButtonBounds));

                        Rectangle buttonBounds = new Rectangle(buttonX + moveButtonWidth + buttonGap, buttonY,
                                buttonWidth, buttonHeight);
                        boolean hovered = buttonBounds.contains(mouseX, mouseY);
                        GuiTheme.drawButtonFrame(buttonBounds.x, buttonBounds.y, buttonBounds.width,
                                buttonBounds.height, hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                        drawCenteredString(fontRenderer, "新建序列", buttonBounds.x + buttonBounds.width / 2,
                                buttonBounds.y + (buttonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                        visibleCustomEmptySectionButtons.add(new CustomButtonRenderInfo("create_sequence",
                                currentCategory, chunk.model.subCategory, buttonBounds));
                    }
                } else {
                    for (int index = 0; index < chunk.pageSequences.size(); index++) {
                        PathSequence sequence = chunk.pageSequences.get(index);
                        int col = index % layout.grid.columns;
                        int row = index / layout.grid.columns;
                        int cardX = layout.grid.startX + col * (layout.grid.cardWidth + layout.grid.gap);
                        int cardY = cardAreaY + row * (layout.grid.cardHeight + layout.grid.gap);
                        Rectangle bounds = new Rectangle(cardX, cardY, layout.grid.cardWidth, layout.grid.cardHeight);
                        boolean hovered = bounds.contains(mouseX, mouseY);
                        boolean foregroundRunning = PathSequenceEventListener
                                .isSequenceRunningInForeground(sequence.getName());
                        boolean backgroundRunning = PathSequenceEventListener
                                .isSequenceRunningInBackground(sequence.getName());
                        boolean running = foregroundRunning || backgroundRunning;
                        boolean selectedCard = isCustomSequenceSelected(sequence);

                        GuiTheme.UiState state = (running || selectedCard) ? GuiTheme.UiState.SELECTED
                                : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                        GuiTheme.drawButtonFrame(cardX, cardY, layout.grid.cardWidth, layout.grid.cardHeight, state);
                        if (backgroundRunning) {
                            drawSequenceRunStripe(cardX, cardY, layout.grid.cardWidth, true);
                        } else if (foregroundRunning) {
                            drawSequenceRunStripe(cardX, cardY, layout.grid.cardWidth, false);
                        }
                        GuiTheme.drawCardHighlight(cardX, cardY, layout.grid.cardWidth, layout.grid.cardHeight,
                                hovered);
                        if (isDraggingCustomSequenceCard && normalizeText(sequence.getName())
                                .equalsIgnoreCase(normalizeText(currentCustomSequenceSortTargetName))) {
                            drawRect(cardX, cardY, cardX + layout.grid.cardWidth, cardY + layout.grid.cardHeight,
                                    0x223D6A86);
                            if (layout.grid.columns > 1) {
                                int lineX = currentCustomSequenceSortAfter ? cardX + layout.grid.cardWidth - 1 : cardX;
                                drawRect(lineX, cardY - 2, lineX + 2, cardY + layout.grid.cardHeight + 2, 0xFF7FD1FF);
                            } else {
                                int lineY = currentCustomSequenceSortAfter ? cardY + layout.grid.cardHeight - 1 : cardY;
                                drawRect(cardX - 2, lineY, cardX + layout.grid.cardWidth + 2, lineY + 2, 0xFF7FD1FF);
                            }
                        }
                        int textAreaWidth = Math.max(24, layout.grid.cardWidth - 12);
                        int lineHeight = fontRenderer.FONT_HEIGHT + 1;
                        int maxLines = Math.max(1, Math.min(3, (layout.grid.cardHeight - 8) / lineHeight));
                        List<String> titleLines = buildSequenceCardTitleLines(fontRenderer, sequence.getName(),
                                textAreaWidth, maxLines);
                        int textHeight = titleLines.size() * lineHeight;
                        int textTop = cardY + Math.max(4, (layout.grid.cardHeight - textHeight) / 2);
                        drawCenteredWrappedText(fontRenderer, titleLines, cardX + layout.grid.cardWidth / 2, textTop,
                                textAreaWidth, lineHeight, 0xFFFFFFFF);

                        String tooltip = buildCustomSequenceTooltip(sequence);
                        visibleCustomSequenceCards
                                .add(new SequenceCardRenderInfo(sequence, bounds, sequence.getName(), "", tooltip));
                        if (hovered) {
                            hoveredTooltip = tooltip;
                        }
                    }
                }
            }

            sectionY += sectionHeight + sectionGap;
        }

        return hoveredTooltip;
    }

    protected static void trimContextMenuOpenPath(int newSize) {
        while (contextMenuOpenPath.size() > newSize) {
            contextMenuOpenPath.remove(contextMenuOpenPath.size() - 1);
        }
    }

    protected static void drawSequenceRunStripe(int x, int y, int width, boolean background) {
        double phase = (System.currentTimeMillis() % 900L) / 900.0D;
        double pulse = 0.45D + 0.55D * (0.5D + 0.5D * Math.sin(phase * Math.PI * 2.0D));
        int red = background ? (int) Math.round(110 + 145 * pulse) : (int) Math.round(40 + 45 * pulse);
        int green = background ? (int) Math.round(35 + 45 * pulse) : (int) Math.round(120 + 120 * pulse);
        int blue = background ? (int) Math.round(35 + 35 * pulse) : (int) Math.round(55 + 65 * pulse);
        int color = 0xFF000000
                | (MathHelper.clamp(red, 0, 255) << 16)
                | (MathHelper.clamp(green, 0, 255) << 8)
                | MathHelper.clamp(blue, 0, 255);
        drawRect(x + 1, y + 1, x + width - 1, y + 4, color);
    }

    protected static int getContextMenuHorizontalGap() {
        return 4;
    }

    protected static int getContextMenuWidth(List<ContextMenuItem> items, FontRenderer fontRenderer) {
        int width = 44;
        for (ContextMenuItem item : items) {
            String label = item.selected ? "√ " + item.label : item.label;
            int reservedWidth = item.hasChildren() ? 26 : 16;
            width = Math.max(width, fontRenderer.getStringWidth(label) + reservedWidth);
        }
        return width;
    }

    protected static int getContextMenuChainWidth(List<ContextMenuItem> items, FontRenderer fontRenderer) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int menuWidth = getContextMenuWidth(items, fontRenderer);
        int deepestChildChainWidth = 0;
        for (ContextMenuItem item : items) {
            if (item != null && item.hasChildren()) {
                deepestChildChainWidth = Math.max(deepestChildChainWidth,
                        getContextMenuChainWidth(item.children, fontRenderer));
            }
        }

        if (deepestChildChainWidth <= 0) {
            return menuWidth;
        }
        return menuWidth + getContextMenuHorizontalGap() + deepestChildChainWidth;
    }

    protected static int clampContextMenuX(int desiredX, int menuWidth, int screenWidth) {
        int margin = 6;
        return Math.max(margin, Math.min(desiredX, screenWidth - menuWidth - margin));
    }

    protected static int clampContextMenuY(int desiredY, int menuHeight, int screenHeight) {
        int margin = 6;
        return Math.max(margin, Math.min(desiredY, screenHeight - menuHeight - margin));
    }

    protected static boolean intersectsAnyAncestor(Rectangle candidateBounds, List<Rectangle> ancestorBounds) {
        if (candidateBounds == null || ancestorBounds == null || ancestorBounds.isEmpty()) {
            return false;
        }
        for (Rectangle ancestor : ancestorBounds) {
            if (ancestor != null && candidateBounds.intersects(ancestor)) {
                return true;
            }
        }
        return false;
    }

    protected static int resolveSubMenuX(Rectangle anchorRect, List<ContextMenuItem> childItems,
            FontRenderer fontRenderer,
            int screenWidth, List<Rectangle> ancestorBounds, boolean preferLeft) {
        int margin = 6;
        int gap = getContextMenuHorizontalGap();
        int childMenuWidth = getContextMenuWidth(childItems, fontRenderer);
        int childChainWidth = getContextMenuChainWidth(childItems, fontRenderer);
        int childTailWidth = Math.max(0, childChainWidth - childMenuWidth);
        int childMenuHeight = childItems.size() * 20 + 4;

        int openRightX = anchorRect.x + anchorRect.width + gap;
        int openLeftX = anchorRect.x - childMenuWidth - gap;

        Rectangle rightBounds = new Rectangle(openRightX, anchorRect.y, childMenuWidth, childMenuHeight);
        Rectangle leftBounds = new Rectangle(openLeftX, anchorRect.y, childMenuWidth, childMenuHeight);

        boolean canOpenRightFully = openRightX + childChainWidth <= screenWidth - margin;
        boolean canOpenLeftFully = openLeftX - childTailWidth >= margin;
        boolean rightHitsAncestor = intersectsAnyAncestor(rightBounds, ancestorBounds);
        boolean leftHitsAncestor = intersectsAnyAncestor(leftBounds, ancestorBounds);

        if (preferLeft) {
            if (canOpenLeftFully && !leftHitsAncestor) {
                return openLeftX;
            }
            if (canOpenRightFully && !rightHitsAncestor) {
                return openRightX;
            }
        } else {
            if (canOpenRightFully && !rightHitsAncestor) {
                return openRightX;
            }
            if (canOpenLeftFully && !leftHitsAncestor) {
                return openLeftX;
            }
        }

        if (!leftHitsAncestor && rightHitsAncestor) {
            return clampContextMenuX(openLeftX, childMenuWidth, screenWidth);
        }
        if (!rightHitsAncestor && leftHitsAncestor) {
            return clampContextMenuX(openRightX, childMenuWidth, screenWidth);
        }

        if (preferLeft && canOpenLeftFully) {
            return openLeftX;
        }
        if (!preferLeft && canOpenRightFully) {
            return openRightX;
        }
        if (canOpenLeftFully) {
            return openLeftX;
        }
        if (canOpenRightFully) {
            return openRightX;
        }

        int rightOverflow = Math.max(0, openRightX + childChainWidth - (screenWidth - margin));
        int leftOverflow = Math.max(0, margin - (openLeftX - childTailWidth));

        if (preferLeft) {
            if (leftOverflow <= rightOverflow) {
                return clampContextMenuX(openLeftX, childMenuWidth, screenWidth);
            }
            return clampContextMenuX(openRightX, childMenuWidth, screenWidth);
        }

        if (rightOverflow <= leftOverflow) {
            return clampContextMenuX(openRightX, childMenuWidth, screenWidth);
        }
        return clampContextMenuX(openLeftX, childMenuWidth, screenWidth);
    }

    protected static Rectangle getSubMenuBoundsForItem(Rectangle anchorRect, List<ContextMenuItem> childItems,
            FontRenderer fontRenderer, int screenWidth, int screenHeight, List<Rectangle> ancestorBounds,
            boolean preferLeft) {
        if (anchorRect == null || childItems == null || childItems.isEmpty()) {
            return null;
        }
        int childMenuWidth = getContextMenuWidth(childItems, fontRenderer);
        int childMenuHeight = childItems.size() * 20 + 4;
        int childX = resolveSubMenuX(anchorRect, childItems, fontRenderer, screenWidth, ancestorBounds, preferLeft);
        int childY = clampContextMenuY(anchorRect.y, childMenuHeight, screenHeight);
        return new Rectangle(childX, childY, childMenuWidth, childMenuHeight);
    }

    protected static boolean shouldOpenSubMenuToLeft(Rectangle anchorRect, List<ContextMenuItem> childItems,
            FontRenderer fontRenderer, int screenWidth, int screenHeight, List<Rectangle> ancestorBounds,
            boolean preferLeft) {
        Rectangle bounds = getSubMenuBoundsForItem(anchorRect, childItems, fontRenderer, screenWidth, screenHeight,
                ancestorBounds, preferLeft);
        return bounds != null && bounds.x + bounds.width <= anchorRect.x;
    }

    protected static void drawContextMenus(int mouseX, int mouseY, int screenWidth, int screenHeight,
            FontRenderer fontRenderer) {
        contextMenuLayers.clear();
        if (!contextMenuVisible || contextMenuRootItems.isEmpty()) {
            return;
        }

        List<ContextMenuItem> items = contextMenuRootItems;
        int x = clampContextMenuX(contextMenuAnchorX, getContextMenuWidth(items, fontRenderer), screenWidth);
        int y = clampContextMenuY(contextMenuAnchorY, items.size() * 20 + 4, screenHeight);
        int depth = 0;

        while (items != null && !items.isEmpty()) {
            ContextMenuLayer layer = new ContextMenuLayer(items);
            layer.x = x;
            layer.y = y;
            layer.width = getContextMenuWidth(items, fontRenderer);
            int layerHeight = items.size() * 20 + 4;
            layer.bounds = new Rectangle(layer.x, layer.y, layer.width, layerHeight);
            contextMenuLayers.add(layer);

            drawRect(layer.x, layer.y, layer.x + layer.width, layer.y + layerHeight, 0xEE111A22);
            drawHorizontalLine(layer.x, layer.x + layer.width, layer.y, 0xFF6FB8FF);
            drawHorizontalLine(layer.x, layer.x + layer.width, layer.y + layerHeight, 0xFF35536C);
            drawVerticalLine(layer.x, layer.y, layer.y + layerHeight, 0xFF35536C);
            drawVerticalLine(layer.x + layer.width, layer.y, layer.y + layerHeight, 0xFF35536C);

            int hoveredIndex = -1;
            for (int i = 0; i < items.size(); i++) {
                int itemY = layer.y + 2 + i * 20;
                Rectangle itemBounds = new Rectangle(layer.x + 2, itemY, layer.width - 4, 19);
                layer.itemBounds.add(itemBounds);
                if (itemBounds.contains(mouseX, mouseY)) {
                    hoveredIndex = i;
                }
            }

            List<Rectangle> ancestorBounds = new ArrayList<>();
            for (int ancestorIndex = 0; ancestorIndex < contextMenuLayers.size(); ancestorIndex++) {
                Rectangle ancestorBoundsRect = contextMenuLayers.get(ancestorIndex).bounds;
                if (ancestorBoundsRect != null) {
                    ancestorBounds.add(ancestorBoundsRect);
                }
            }

            int existingOpenIndex = depth < contextMenuOpenPath.size() ? contextMenuOpenPath.get(depth) : -1;
            Rectangle existingChildBounds = null;
            int currentLayerIndex = contextMenuLayers.size() - 1;
            boolean preferLeftForChild = currentLayerIndex > 0
                    && layer.bounds != null
                    && contextMenuLayers.get(currentLayerIndex - 1).bounds != null
                    && layer.bounds.x + layer.bounds.width <= contextMenuLayers.get(currentLayerIndex - 1).bounds.x;
            if (existingOpenIndex >= 0 && existingOpenIndex < items.size() && items.get(existingOpenIndex).hasChildren()
                    && existingOpenIndex < layer.itemBounds.size()) {
                existingChildBounds = getSubMenuBoundsForItem(layer.itemBounds.get(existingOpenIndex),
                        items.get(existingOpenIndex).children, fontRenderer, screenWidth, screenHeight, ancestorBounds,
                        preferLeftForChild);
            }
            boolean mouseInsideExistingChild = existingChildBounds != null
                    && existingChildBounds.contains(mouseX, mouseY);

            int keyboardSelectedIndex = getKeyboardMenuSelection(depth, items);
            for (int i = 0; i < items.size(); i++) {
                Rectangle itemBounds = layer.itemBounds.get(i);
                int itemY = itemBounds.y;
                boolean hovered = hoveredIndex == i;
                ContextMenuItem item = items.get(i);
                boolean keyboardSelected = hoveredIndex < 0 && keyboardSelectedIndex == i;
                if (hovered || keyboardSelected) {
                    drawRect(itemBounds.x, itemBounds.y, itemBounds.x + itemBounds.width,
                            itemBounds.y + itemBounds.height, 0xCC2B5A7C);
                }
                String label = item.selected ? "√ " + item.label : item.label;
                drawString(fontRenderer, label, layer.x + 8, itemY + 5, item.enabled ? 0xFFFFFFFF : 0xFF777777);
                if (item.hasChildren()) {
                    boolean openLeft = shouldOpenSubMenuToLeft(itemBounds, item.children, fontRenderer, screenWidth,
                            screenHeight, ancestorBounds, preferLeftForChild);
                    drawString(fontRenderer, openLeft ? "<" : ">", layer.x + layer.width - 10, itemY + 5, 0xFFB8CCE0);
                }
            }

            if (mouseInsideExistingChild) {
                // 鼠标已经进入当前层所展开的子菜单区域时，父层不再根据重叠区域更新 hover/openPath，
                // 否则会出现“子菜单盖在上面，但实际仍响应父菜单”的问题。
            } else if (hoveredIndex >= 0 && items.get(hoveredIndex).hasChildren()) {
                setKeyboardMenuSelection(depth, hoveredIndex);
                while (contextMenuOpenPath.size() <= depth) {
                    contextMenuOpenPath.add(-1);
                }
                contextMenuOpenPath.set(depth, hoveredIndex);
                trimContextMenuOpenPath(depth + 1);
            } else if (hoveredIndex >= 0 && layer.bounds.contains(mouseX, mouseY)) {
                setKeyboardMenuSelection(depth, hoveredIndex);
                trimContextMenuOpenPath(depth);
            } else {
                if (keyboardSelectedIndex >= 0 && keyboardSelectedIndex < items.size()
                        && items.get(keyboardSelectedIndex).hasChildren()) {
                    while (contextMenuOpenPath.size() <= depth) {
                        contextMenuOpenPath.add(-1);
                    }
                    contextMenuOpenPath.set(depth, keyboardSelectedIndex);
                    trimContextMenuOpenPath(depth + 1);
                } else {
                    trimContextMenuOpenPath(depth);
                }
            }

            int openIndex = depth < contextMenuOpenPath.size() ? contextMenuOpenPath.get(depth) : -1;
            if (openIndex < 0 || openIndex >= items.size() || !items.get(openIndex).hasChildren()) {
                break;
            }

            Rectangle anchorRect = layer.itemBounds.get(openIndex);
            items = items.get(openIndex).children;
            int childMenuWidth = getContextMenuWidth(items, fontRenderer);
            int childMenuHeight = items.size() * 20 + 4;
            boolean childPreferLeft = shouldOpenSubMenuToLeft(anchorRect, items, fontRenderer, screenWidth,
                    screenHeight, ancestorBounds, preferLeftForChild);
            x = resolveSubMenuX(anchorRect, items, fontRenderer, screenWidth, ancestorBounds, childPreferLeft);
            y = clampContextMenuY(anchorRect.y, childMenuHeight, screenHeight);
            depth++;
        }
    }

    protected static void drawCustomSequenceDragGhost(int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (!isDraggingCustomSequenceCard || pressedCustomSequence == null) {
            return;
        }

        String text = fontRenderer.trimStringToWidth(pressedCustomSequence.getName(), 120);
        int width = Math.max(96, fontRenderer.getStringWidth(text) + 18);
        String targetText = null;
        if (!normalizeText(currentCustomSequenceSortTargetName).isEmpty()) {
            SequenceCardRenderInfo targetCard = findVisibleCustomSequenceCardByName(
                    currentCustomSequenceSortTargetName);
            String targetName = targetCard != null && targetCard.sequence != null ? targetCard.sequence.getName()
                    : currentCustomSequenceSortTargetName;
            targetText = "排序到: " + targetName + (currentCustomSequenceSortAfter ? " 后" : " 前");
            width = Math.max(width, Math.min(180, fontRenderer.getStringWidth(targetText) + 18));
        } else if (currentSequenceDropTarget != null) {
            targetText = currentSequenceDropTarget.isSubCategory()
                    ? "移动到: " + currentSequenceDropTarget.category + " / " + currentSequenceDropTarget.subCategory
                    : "移动到: " + currentSequenceDropTarget.category;
            width = Math.max(width, Math.min(180, fontRenderer.getStringWidth(targetText) + 18));
        }
        int height = targetText == null ? 22 : 36;
        int x = mouseX + 10;
        int y = mouseY + 10;
        GuiTheme.drawButtonFrame(x, y, width, height, GuiTheme.UiState.HOVER);
        drawRect(x, y, x + width, y + height, 0x442B5A7C);
        drawString(fontRenderer, text, x + 6, y + (height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        if (targetText != null) {
            drawString(fontRenderer, fontRenderer.trimStringToWidth(targetText, width - 12), x + 6, y + 20, 0xFF9FDFFF);
        }
    }

    protected static void drawCategoryTreeDragGhost(int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (!isDraggingCategoryRow || pressedCategoryRow == null) {
            return;
        }
        String label = fontRenderer.trimStringToWidth(getCategoryRowDisplayLabel(pressedCategoryRow), 120);
        String targetText = null;
        if (currentCategorySortDropTarget != null) {
            targetText = "排序到: "
                    + fontRenderer.trimStringToWidth(getCategoryRowDisplayLabel(currentCategorySortDropTarget), 100)
                    + (currentCategorySortDropAfter ? " 后" : " 前");
        }
        int width = Math.max(88, fontRenderer.getStringWidth(label) + 18);
        if (targetText != null) {
            width = Math.max(width, Math.min(180, fontRenderer.getStringWidth(targetText) + 18));
        }
        int height = targetText == null ? 22 : 36;
        int x = mouseX + 10;
        int y = mouseY + 10;
        GuiTheme.drawButtonFrame(x, y, width, height, GuiTheme.UiState.HOVER);
        drawRect(x, y, x + width, y + height, 0x442B5A7C);
        drawString(fontRenderer, label, x + 6, y + (height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        if (targetText != null) {
            drawString(fontRenderer, fontRenderer.trimStringToWidth(targetText, width - 12), x + 6, y + 20, 0xFF9FDFFF);
        }
    }

    protected static boolean isProtectedCustomCategory(String category) {
        return I18n.format("path.category.default").equals(category)
                || I18n.format("path.category.builtin").equals(category);
    }

    protected static void activateSequence(PathSequence sequence) {
        if (sequence == null) {
            return;
        }
        if (sequence.isCustom()) {
            MainUiLayoutManager.recordSequenceOpened(sequence.getName());
        }
        if (sequence.shouldCloseGuiAfterStart()) {
            closeOverlay();
        }
        PathSequenceManager.runPathSequence(sequence.getName());
    }

    protected static void activateSequenceFromStep(PathSequence sequence, int stepIndex) {
        if (sequence == null) {
            return;
        }
        if (sequence.isCustom()) {
            MainUiLayoutManager.recordSequenceOpened(sequence.getName());
        }
        if (sequence.shouldCloseGuiAfterStart()) {
            closeOverlay();
        }
        PathSequenceManager.runPathSequenceFromStep(sequence.getName(), stepIndex);
    }

    protected static void clearPressedCustomSequence() {
        pressedCustomSequence = null;
        pressedCustomSequenceRect = null;
        isDraggingCustomSequenceCard = false;
        currentSequenceDropTarget = null;
        currentCustomSequenceSortTargetName = "";
        currentCustomSequenceSortAfter = false;
        customSequencePageTurnLockUntil = 0L;
    }

    protected static void clearPressedCategoryRow() {
        pressedCategoryRow = null;
        pressedCategoryRowRect = null;
        isDraggingCategoryRow = false;
        currentCategorySortDropTarget = null;
        currentCategorySortDropAfter = false;
    }

    protected static void setAllCustomCategoryCollapsed(boolean collapsed) {
        MainUiLayoutManager.setCollapsedForCategories(getVisibleCustomCategoriesInDisplayOrder(), collapsed);
        refreshGuiLists();
    }

    protected static ContextMenuItem buildCategoryTreeOrganizeMenu() {
        ContextMenuItem organizeMenu = menuItem("分类树整理", null);
        organizeMenu.child(menuItem("恢复默认顺序", () -> {
            PathSequenceManager.restoreCustomCategoryOrder();
            refreshGuiLists();
        }));
        organizeMenu.child(menuItem("按名称整理", () -> {
            PathSequenceManager.sortCustomCategoriesAlphabetically();
            refreshGuiLists();
        }));
        organizeMenu.child(menuItem("展开全部", () -> setAllCustomCategoryCollapsed(false)));
        organizeMenu.child(menuItem("折叠全部", () -> setAllCustomCategoryCollapsed(true)));
        return organizeMenu;
    }

    protected static List<ContextMenuItem> buildCategoryBlankAreaMenu() {
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(menuItem("新建分类", () -> openOverlayTextInput("新建分类", value -> {
            String name = normalizeText(value);
            if (!validateCategoryNameInput(name, "")) {
                return;
            }
            PathSequenceManager.addCategory(name);
            currentCategory = name;
            currentCustomSubCategory = "";
            currentPage = 0;
            refreshGuiLists();
        })));
        items.add(buildCategoryTreeOrganizeMenu());
        return items;
    }

    protected static List<ContextMenuItem> buildCategoryContextMenu(String category) {
        List<ContextMenuItem> items = new ArrayList<>();
        final boolean protectedCategory = isProtectedCustomCategory(category);

        items.add(menuItem("新建子分类", () -> openOverlayTextInput("新建子分类", value -> {
            String name = normalizeText(value);
            if (!validateSubCategoryNameInput(category, name, "")) {
                return;
            }
            MainUiLayoutManager.addSubCategory(category, name);
            currentCategory = category;
            currentCustomSubCategory = name;
            currentPage = 0;
            refreshGuiLists();
        })));

        items.add(menuItem(MainUiLayoutManager.isPinned(category) ? "取消固定分类" : "固定分类", () -> {
            MainUiLayoutManager.togglePinned(category);
            refreshGuiLists();
        }));

        items.add(buildCategoryTreeOrganizeMenu());

        ContextMenuItem sortMenu = menuItem("排序", null);
        sortMenu.child(menuItem("默认", () -> {
            MainUiLayoutManager.setSortMode(category, MainUiLayoutManager.SORT_DEFAULT);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.SORT_DEFAULT.equals(MainUiLayoutManager.getSortMode(category))));
        sortMenu.child(menuItem("按首字母", () -> {
            MainUiLayoutManager.setSortMode(category, MainUiLayoutManager.SORT_ALPHABETICAL);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.SORT_ALPHABETICAL.equals(MainUiLayoutManager.getSortMode(category))));
        sortMenu.child(menuItem("最后打开", () -> {
            MainUiLayoutManager.setSortMode(category, MainUiLayoutManager.SORT_LAST_OPENED);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.SORT_LAST_OPENED.equals(MainUiLayoutManager.getSortMode(category))));
        sortMenu.child(menuItem("按打开次数", () -> {
            MainUiLayoutManager.setSortMode(category, MainUiLayoutManager.SORT_OPEN_COUNT);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.SORT_OPEN_COUNT.equals(MainUiLayoutManager.getSortMode(category))));
        items.add(sortMenu);

        ContextMenuItem layoutMenu = menuItem("布局", null);
        layoutMenu.child(menuItem("平铺", () -> {
            MainUiLayoutManager.setLayoutMode(category, MainUiLayoutManager.LAYOUT_TILE);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.LAYOUT_TILE.equals(MainUiLayoutManager.getLayoutMode(category))));
        layoutMenu.child(menuItem("列表", () -> {
            MainUiLayoutManager.setLayoutMode(category, MainUiLayoutManager.LAYOUT_LIST);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.LAYOUT_LIST.equals(MainUiLayoutManager.getLayoutMode(category))));
        layoutMenu.child(menuItem("紧凑", () -> {
            MainUiLayoutManager.setLayoutMode(category, MainUiLayoutManager.LAYOUT_COMPACT);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.LAYOUT_COMPACT.equals(MainUiLayoutManager.getLayoutMode(category))));
        layoutMenu.child(menuItem("宽卡片", () -> {
            MainUiLayoutManager.setLayoutMode(category, MainUiLayoutManager.LAYOUT_WIDE);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.LAYOUT_WIDE.equals(MainUiLayoutManager.getLayoutMode(category))));
        items.add(layoutMenu);

        ContextMenuItem iconMenu = menuItem("图标", null);
        iconMenu.child(menuItem("默认", () -> {
            MainUiLayoutManager.setIconSize(category, MainUiLayoutManager.ICON_DEFAULT);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.ICON_DEFAULT.equals(MainUiLayoutManager.getIconSize(category))));
        iconMenu.child(menuItem("超大", () -> {
            MainUiLayoutManager.setIconSize(category, MainUiLayoutManager.ICON_XL);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.ICON_XL.equals(MainUiLayoutManager.getIconSize(category))));
        iconMenu.child(menuItem("大", () -> {
            MainUiLayoutManager.setIconSize(category, MainUiLayoutManager.ICON_LARGE);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.ICON_LARGE.equals(MainUiLayoutManager.getIconSize(category))));
        iconMenu.child(menuItem("中", () -> {
            MainUiLayoutManager.setIconSize(category, MainUiLayoutManager.ICON_MEDIUM);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.ICON_MEDIUM.equals(MainUiLayoutManager.getIconSize(category))));
        iconMenu.child(menuItem("小", () -> {
            MainUiLayoutManager.setIconSize(category, MainUiLayoutManager.ICON_SMALL);
            refreshGuiLists();
        }).selected(MainUiLayoutManager.ICON_SMALL.equals(MainUiLayoutManager.getIconSize(category))));
        items.add(iconMenu);

        ContextMenuItem displayMenu = menuItem("显示", null);
        displayMenu.child(menuItem("显示分类", () -> {
            PathSequenceManager.setCategoryHidden(category, false);
            refreshGuiLists();
        }).selected(!PathSequenceManager.isCategoryHidden(category)));
        displayMenu.child(menuItem("隐藏分类", () -> {
            PathSequenceManager.setCategoryHidden(category, true);
            if (category.equals(currentCategory)) {
                currentCategory = I18n.format("gui.inventory.category.common");
                currentCustomSubCategory = "";
            }
            refreshGuiLists();
        }).selected(PathSequenceManager.isCategoryHidden(category)));
        items.add(displayMenu);

        items.add(menuItem("编辑", () -> openOverlayTextInput("编辑分组", category, value -> {
            String newName = normalizeText(value);
            if (newName.equals(category)) {
                return;
            }
            if (!validateCategoryNameInput(newName, category)) {
                return;
            }
            PathSequenceManager.renameCategory(category, newName);
            if (category.equals(currentCategory)) {
                currentCategory = newName;
            }
            refreshGuiLists();
        })).enabled(!protectedCategory));

        items.add(menuItem("删除", () -> openOverlayConfirm("删除分组", "删除后将一并删除分组下所有路径序列。\n是否继续？", () -> {
            PathSequenceManager.deleteCategory(category);
            if (category.equals(currentCategory)) {
                currentCategory = I18n.format("gui.inventory.category.common");
                currentCustomSubCategory = "";
            }
            refreshGuiLists();
        })).enabled(!protectedCategory));

        return items;
    }

    protected static List<ContextMenuItem> buildSubCategoryContextMenu(String category, String subCategory) {
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(menuItem("编辑", () -> openOverlayTextInput("编辑子分类", subCategory, value -> {
            String newName = normalizeText(value);
            if (newName.equals(subCategory)) {
                return;
            }
            if (!validateSubCategoryNameInput(category, newName, subCategory)) {
                return;
            }
            MainUiLayoutManager.renameSubCategory(category, subCategory, newName);
            if (category.equals(currentCategory) && subCategory.equals(currentCustomSubCategory)) {
                currentCustomSubCategory = newName;
            }
            refreshGuiLists();
        })));
        items.add(menuItem("删除", () -> openOverlayConfirm("删除子分类", "删除后将一并删除该子分类下所有路径序列。\n是否继续？", () -> {
            MainUiLayoutManager.deleteSubCategory(category, subCategory, true);
            if (category.equals(currentCategory) && subCategory.equals(currentCustomSubCategory)) {
                currentCustomSubCategory = "";
            }
            refreshGuiLists();
        })));
        return items;
    }

    protected static ContextMenuItem buildSequenceDestinationMenu(String label, PathSequence sequence, boolean copy) {
        ContextMenuItem root = menuItem(label, null);
        List<String> customCategories = new ArrayList<>();
        for (String category : categories) {
            if (isCustomOverlayCategory(category)) {
                customCategories.add(category);
            }
        }
        customCategories.sort((left, right) -> {
            boolean leftPinned = MainUiLayoutManager.isPinned(left);
            boolean rightPinned = MainUiLayoutManager.isPinned(right);
            if (leftPinned != rightPinned) {
                return Boolean.compare(rightPinned, leftPinned);
            }
            return Integer.compare(categories.indexOf(left), categories.indexOf(right));
        });

        for (String destinationCategory : customCategories) {
            ContextMenuItem categoryMenu = menuItem(destinationCategory, null);
            boolean sameRoot = destinationCategory.equals(sequence.getCategory())
                    && normalizeText(sequence.getSubCategory()).isEmpty();
            categoryMenu.child(menuItem("分类根目录", () -> {
                if (copy) {
                    PathSequenceManager.copyCustomSequenceTo(sequence.getName(), destinationCategory, "");
                } else {
                    PathSequenceManager.moveCustomSequenceTo(sequence.getName(), destinationCategory, "");
                }
                currentCategory = destinationCategory;
                currentCustomSubCategory = "";
                currentPage = 0;
                refreshGuiLists();
            }).enabled(copy || !sameRoot));

            for (String destinationSubCategory : MainUiLayoutManager.getSubCategories(destinationCategory)) {
                boolean sameSubCategory = destinationCategory.equals(sequence.getCategory())
                        && destinationSubCategory.equalsIgnoreCase(normalizeText(sequence.getSubCategory()));
                categoryMenu.child(menuItem(destinationSubCategory, () -> {
                    if (copy) {
                        PathSequenceManager.copyCustomSequenceTo(sequence.getName(), destinationCategory,
                                destinationSubCategory);
                    } else {
                        PathSequenceManager.moveCustomSequenceTo(sequence.getName(), destinationCategory,
                                destinationSubCategory);
                    }
                    currentCategory = destinationCategory;
                    currentCustomSubCategory = destinationSubCategory;
                    currentPage = 0;
                    refreshGuiLists();
                }).enabled(copy || !sameSubCategory));
            }

            root.child(categoryMenu);
        }
        return root;
    }

    protected static ContextMenuItem buildBatchSequenceDestinationMenu(String label, List<String> sequenceNames,
            boolean copy) {
        ContextMenuItem root = menuItem(label, null);
        List<String> customCategories = new ArrayList<>();
        for (String category : categories) {
            if (isCustomOverlayCategory(category)) {
                customCategories.add(category);
            }
        }
        customCategories.sort((left, right) -> {
            boolean leftPinned = MainUiLayoutManager.isPinned(left);
            boolean rightPinned = MainUiLayoutManager.isPinned(right);
            if (leftPinned != rightPinned) {
                return Boolean.compare(rightPinned, leftPinned);
            }
            return Integer.compare(categories.indexOf(left), categories.indexOf(right));
        });

        for (String destinationCategory : customCategories) {
            ContextMenuItem categoryMenu = menuItem(destinationCategory, null);
            categoryMenu.child(menuItem("分类根目录", () -> {
                if (copy) {
                    List<String> copiedNames = new ArrayList<>();
                    for (String sequenceName : sequenceNames) {
                        String copied = PathSequenceManager.copyCustomSequenceTo(sequenceName, destinationCategory, "");
                        if (!copied.isEmpty()) {
                            copiedNames.add(copied);
                        }
                    }
                    clearSelectedCustomSequences();
                    selectedCustomSequenceNames.addAll(copiedNames);
                } else {
                    for (String sequenceName : sequenceNames) {
                        PathSequenceManager.moveCustomSequenceTo(sequenceName, destinationCategory, "");
                    }
                    clearSelectedCustomSequences();
                }
                currentCategory = destinationCategory;
                currentCustomSubCategory = "";
                currentPage = 0;
                refreshGuiLists();
            }));

            for (String destinationSubCategory : MainUiLayoutManager.getSubCategories(destinationCategory)) {
                categoryMenu.child(menuItem(destinationSubCategory, () -> {
                    if (copy) {
                        List<String> copiedNames = new ArrayList<>();
                        for (String sequenceName : sequenceNames) {
                            String copied = PathSequenceManager.copyCustomSequenceTo(sequenceName, destinationCategory,
                                    destinationSubCategory);
                            if (!copied.isEmpty()) {
                                copiedNames.add(copied);
                            }
                        }
                        clearSelectedCustomSequences();
                        selectedCustomSequenceNames.addAll(copiedNames);
                    } else {
                        for (String sequenceName : sequenceNames) {
                            PathSequenceManager.moveCustomSequenceTo(sequenceName, destinationCategory,
                                    destinationSubCategory);
                        }
                        clearSelectedCustomSequences();
                    }
                    currentCategory = destinationCategory;
                    currentCustomSubCategory = destinationSubCategory;
                    currentPage = 0;
                    refreshGuiLists();
                }));
            }

            root.child(categoryMenu);
        }
        return root;
    }

    protected static List<ContextMenuItem> buildBatchMoreMenu(List<String> sequenceNames) {
        List<ContextMenuItem> items = new ArrayList<>();
        ContextMenuItem closeGuiMenu = menuItem("批量关闭GUI", null);
        closeGuiMenu.child(menuItem("开启",
                () -> applyBatchSequenceChange(sequenceNames, sequence -> sequence.setCloseGuiAfterStart(true))));
        closeGuiMenu.child(menuItem("关闭",
                () -> applyBatchSequenceChange(sequenceNames, sequence -> sequence.setCloseGuiAfterStart(false))));
        closeGuiMenu.child(menuItem("切换", () -> applyBatchSequenceChange(sequenceNames,
                sequence -> sequence.setCloseGuiAfterStart(!sequence.shouldCloseGuiAfterStart()))));
        items.add(closeGuiMenu);

        ContextMenuItem singleExecutionMenu = menuItem("批量单次执行", null);
        singleExecutionMenu.child(menuItem("开启",
                () -> applyBatchSequenceChange(sequenceNames, sequence -> sequence.setSingleExecution(true))));
        singleExecutionMenu.child(menuItem("关闭",
                () -> applyBatchSequenceChange(sequenceNames, sequence -> sequence.setSingleExecution(false))));
        singleExecutionMenu.child(menuItem("切换", () -> applyBatchSequenceChange(sequenceNames,
                sequence -> sequence.setSingleExecution(!sequence.isSingleExecution()))));
        items.add(singleExecutionMenu);

        items.add(menuItem("批量循环延迟", () -> openOverlayTextInput("批量循环延迟", "20", value -> {
            String normalized = normalizeText(value);
            if (normalized.isEmpty()) {
                showOverlayMessage("§c循环延迟不能为空");
                return;
            }
            int ticks;
            try {
                ticks = Integer.parseInt(normalized);
            } catch (NumberFormatException e) {
                showOverlayMessage("§c循环延迟必须是整数");
                return;
            }
            if (ticks < 0) {
                showOverlayMessage("§c循环延迟不能小于 0");
                return;
            }
            applyBatchSequenceChange(sequenceNames, sequence -> sequence.setLoopDelayTicks(ticks));
        })));

        items.add(menuItem("批量改子分类", () -> promptBatchSubCategoryUpdate(sequenceNames)));
        items.add(menuItem("批量改备注", () -> promptBatchNoteUpdate(sequenceNames)));
        items.add(menuItem("批量导出", () -> {
            Path exportFile = PathSequenceManager.exportCustomSequences(sequenceNames);
            if (exportFile != null) {
                showOverlayMessage("§a已导出到: " + exportFile.toAbsolutePath());
            } else {
                showOverlayMessage("§c批量导出失败");
            }
        }));
        return items;
    }

    protected static List<ContextMenuItem> buildSequenceCardContextMenu(PathSequence sequence) {
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(menuItem("删除", () -> openOverlayConfirm("删除路径序列", "删除后不可恢复，是否继续？", () -> {
            PathSequenceManager.deleteCustomSequence(sequence.getName());
            MainUiLayoutManager.removeSequenceStats(sequence.getName());
            refreshGuiLists();
        })));
        items.add(buildSequenceDestinationMenu("移动到", sequence, false));
        items.add(buildSequenceDestinationMenu("复制到", sequence, true));
        items.add(buildSequenceJumpExecutionMenu(sequence));
        items.add(menuItem("编辑", () -> {
            closeContextMenu();
            closeOverlay();
            Minecraft.getMinecraft()
                    .displayGuiScreen(GuiPathManager.openForSequence(sequence.getCategory(), sequence.getName()));
        }));
        return items;
    }

    protected static ContextMenuItem buildSequenceJumpExecutionMenu(PathSequence sequence) {
        ContextMenuItem root = menuItem("跳转执行", null);
        if (sequence == null || sequence.getSteps() == null || sequence.getSteps().isEmpty()) {
            return root.enabled(false);
        }

        final int groupSize = 25;
        int stepCount = sequence.getSteps().size();
        if (stepCount <= groupSize) {
            appendSequenceJumpStepItems(root, sequence, 0, stepCount);
            return root;
        }

        for (int start = 0; start < stepCount; start += groupSize) {
            int end = Math.min(stepCount, start + groupSize);
            ContextMenuItem group = menuItem((start + 1) + "-" + end, null);
            appendSequenceJumpStepItems(group, sequence, start, end);
            root.child(group);
        }
        return root;
    }

    protected static void appendSequenceJumpStepItems(ContextMenuItem parent, PathSequence sequence, int startIndex,
            int endIndex) {
        List<PathStep> steps = sequence.getSteps();
        for (int i = Math.max(0, startIndex); i < endIndex && i < steps.size(); i++) {
            final int stepIndex = i;
            parent.child(menuItem(buildSequenceJumpStepLabel(steps.get(i), i), () -> {
                closeContextMenu();
                activateSequenceFromStep(sequence, stepIndex);
            }));
        }
    }

    protected static String buildSequenceJumpStepLabel(PathStep step, int stepIndex) {
        String label = "第 " + (stepIndex + 1) + " 步";
        String note = step == null ? "" : sanitizeContextMenuLabelPart(step.getNote());
        if (!note.isEmpty()) {
            return limitContextMenuLabel(label + " - " + note, 48);
        }

        double[] target = step == null ? null : step.getGotoPoint();
        if (target != null && target.length >= 3 && !Double.isNaN(target[0])) {
            String coords = String.format(Locale.ROOT, "%.1f, %.1f, %.1f", target[0], target[1], target[2]);
            return limitContextMenuLabel(label + " - " + coords, 48);
        }
        return label + " - 无坐标";
    }

    protected static String sanitizeContextMenuLabelPart(String text) {
        return normalizeText(text).replace('\r', ' ').replace('\n', ' ');
    }

    protected static String limitContextMenuLabel(String label, int maxLength) {
        String text = label == null ? "" : label;
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(1, maxLength - 1)) + "...";
    }

    protected static void handleCustomToolbarAction(CustomButtonRenderInfo button, int mouseX, int mouseY) {
        if (button == null) {
            return;
        }
        if ("select_page".equals(button.action)) {
            for (SequenceCardRenderInfo info : visibleCustomSequenceCards) {
                selectedCustomSequenceNames.add(info.sequence.getName());
            }
            return;
        }
        if ("clear_selection".equals(button.action)) {
            clearSelectedCustomSequences();
            return;
        }
        if ("batch_move".equals(button.action)) {
            List<String> selectedNames = getSelectedCustomSequenceNames();
            if (!selectedNames.isEmpty()) {
                openContextMenu(mouseX, mouseY,
                        Collections.singletonList(buildBatchSequenceDestinationMenu("批量移动到", selectedNames, false)));
            }
            return;
        }
        if ("batch_copy".equals(button.action)) {
            List<String> selectedNames = getSelectedCustomSequenceNames();
            if (!selectedNames.isEmpty()) {
                openContextMenu(mouseX, mouseY,
                        Collections.singletonList(buildBatchSequenceDestinationMenu("批量复制到", selectedNames, true)));
            }
            return;
        }
        if ("batch_delete".equals(button.action)) {
            List<String> selectedNames = getSelectedCustomSequenceNames();
            if (!selectedNames.isEmpty()) {
                openOverlayConfirm("批量删除路径序列", "将删除已选中的 " + selectedNames.size() + " 个路径序列，是否继续？", () -> {
                    for (String sequenceName : selectedNames) {
                        PathSequenceManager.deleteCustomSequence(sequenceName);
                    }
                    clearSelectedCustomSequences();
                    refreshGuiLists();
                });
            }
            return;
        }
        if ("batch_more".equals(button.action)) {
            List<String> selectedNames = getSelectedCustomSequenceNames();
            if (!selectedNames.isEmpty()) {
                openContextMenu(mouseX, mouseY, buildBatchMoreMenu(selectedNames));
            }
            return;
        }
        if ("create_subcategory".equals(button.action)) {
            promptCreateSubCategory(button.category);
            return;
        }
        if ("move_existing_into_section".equals(button.action)) {
            List<ContextMenuItem> menuItems = buildMoveExistingIntoSectionMenu(button.category, button.subCategory);
            if (menuItems.isEmpty()) {
                showOverlayMessage("§e当前分类下没有可移动到该子分类的序列");
            } else {
                openContextMenu(mouseX, mouseY, menuItems);
            }
            return;
        }
        if ("create_sequence".equals(button.action)) {
            promptCreateCustomSequence(button.category, button.subCategory);
        }
    }

    protected static boolean handleContextMenuClick(int mouseX, int mouseY, int mouseButton) {
        if (!contextMenuVisible) {
            return false;
        }

        for (int layerIndex = contextMenuLayers.size() - 1; layerIndex >= 0; layerIndex--) {
            ContextMenuLayer layer = contextMenuLayers.get(layerIndex);
            if (!layer.bounds.contains(mouseX, mouseY)) {
                continue;
            }

            if (mouseButton != 0) {
                closeContextMenu();
                return true;
            }

            for (int itemIndex = 0; itemIndex < layer.itemBounds.size(); itemIndex++) {
                if (!layer.itemBounds.get(itemIndex).contains(mouseX, mouseY)) {
                    continue;
                }
                ContextMenuItem item = layer.items.get(itemIndex);
                if (!item.enabled) {
                    closeContextMenu();
                    return true;
                }
                if (item.hasChildren()) {
                    while (contextMenuOpenPath.size() <= layerIndex) {
                        contextMenuOpenPath.add(-1);
                    }
                    contextMenuOpenPath.set(layerIndex, itemIndex);
                    trimContextMenuOpenPath(layerIndex + 1);
                    return true;
                }

                closeContextMenu();
                if (item.action != null) {
                    item.action.run();
                }
                return true;
            }

            closeContextMenu();
            return true;
        }

        closeContextMenu();
        return false;
    }

    protected static boolean handleCustomSequenceCategoryClick(int mouseX, int mouseY, int mouseButton, OverlayMetrics m,
            int contentStartY, Minecraft mc) {
        if (mouseButton == 0) {
            if (customSearchToggleButtonBounds != null && customSearchToggleButtonBounds.contains(mouseX, mouseY)) {
                boolean expand = !isCustomSearchExpanded();
                setCustomSearchExpanded(expand, expand);
                return true;
            }

            CustomButtonRenderInfo scopeButton = findCustomSearchScopeButtonAt(mouseX, mouseY);
            if (scopeButton != null) {
                if ("cycle_scope".equals(scopeButton.action)) {
                    cycleCustomSearchScope();
                } else if (!SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(scopeButton.action)
                        || !isBlank(currentCustomSubCategory)) {
                    applyCustomSearchScope(scopeButton.action);
                }
                return true;
            }

            CustomButtonRenderInfo toolbarButton = findCustomToolbarButtonAt(mouseX, mouseY);
            if (toolbarButton != null) {
                handleCustomToolbarAction(toolbarButton, mouseX, mouseY);
                return true;
            }

            CustomButtonRenderInfo emptyButton = findCustomEmptySectionButtonAt(mouseX, mouseY);
            if (emptyButton != null) {
                handleCustomToolbarAction(emptyButton, mouseX, mouseY);
                return true;
            }
        }

        CustomSectionRenderInfo sectionHeader = findCustomSectionHeaderAt(mouseX, mouseY);
        if (sectionHeader != null && mouseButton == 0) {
            toggleCustomSectionExpanded(sectionHeader.key);
            return true;
        }

        SequenceCardRenderInfo card = findCustomSequenceCardAt(mouseX, mouseY);
        if (card == null) {
            return false;
        }

        if (mouseButton == 1) {
            openContextMenu(mouseX, mouseY, buildSequenceCardContextMenu(card.sequence));
            return true;
        }

        if (mouseButton == 0) {
            if (isControlDown()) {
                toggleCustomSequenceSelection(card.sequence);
                return true;
            }
            pressedCustomSequence = card.sequence;
            pressedCustomSequenceRect = card.bounds;
            pressedCustomSequenceMouseX = mouseX;
            pressedCustomSequenceMouseY = mouseY;
            draggingCustomSequenceMouseX = mouseX;
            draggingCustomSequenceMouseY = mouseY;
            isDraggingCustomSequenceCard = false;
            currentSequenceDropTarget = null;
            return true;
        }

        return false;
    }

    protected static boolean handleGroupedCommonCategoryClick(int mouseX, int mouseY, int mouseButton, OverlayMetrics m,
            int contentStartY, Minecraft mc) throws IOException {
        List<List<GroupedItemSection>> pages = buildCommonContentPages();
        int totalPages = Math.max(1, pages.size());
        currentPage = MathHelper.clamp(currentPage, 0, totalPages - 1);

        List<GroupedItemSection> pageSections = pages.get(currentPage);
        int sectionX = m.contentPanelX + m.padding;
        int sectionY = contentStartY + m.padding;
        int sectionWidth = Math.max(140, m.contentPanelRight - sectionX - m.padding);
        int sectionGap = Math.max(4, m.gap - 2);
        int headerHeight = Math.max(18, m.itemButtonHeight - 2);
        int itemGap = Math.max(4, m.gap - 2);
        int innerPadding = 6;

        for (GroupedItemSection section : pageSections) {
            boolean expanded = commonSectionExpanded.getOrDefault(section.key, Boolean.TRUE);
            int itemRows = expanded ? getCommonSectionItemRows(section) : 0;
            int sectionHeight = headerHeight + 8;
            if (expanded && itemRows > 0) {
                sectionHeight += itemRows * m.itemButtonHeight + (itemRows - 1) * itemGap + 8;
            }

            if (mouseButton == 0 && isMouseOver(mouseX, mouseY, sectionX, sectionY, sectionWidth, headerHeight + 2)) {
                commonSectionExpanded.put(section.key, !expanded);
                clampCurrentPageToCategoryBounds();
                return true;
            }

            if (expanded) {
                int buttonAreaX = sectionX + innerPadding;
                int buttonAreaY = sectionY + headerHeight + 6;
                int buttonWidth = Math.max(60, (sectionWidth - innerPadding * 2 - itemGap * 2) / 3);

                for (int i = 0; i < section.commands.size(); i++) {
                    String command = section.commands.get(i);
                    int col = i % 3;
                    int row = i / 3;
                    int buttonX = buttonAreaX + col * (buttonWidth + itemGap);
                    int buttonY = buttonAreaY + row * (m.itemButtonHeight + itemGap);
                    if (isMouseOver(mouseX, mouseY, buttonX, buttonY, buttonWidth, m.itemButtonHeight)) {
                        handleCommonCommandClick(command, mouseButton, mc);
                        return true;
                    }
                }
            }

            sectionY += sectionHeight + sectionGap;
        }

        return false;
    }

    protected static boolean handleCommonCommandClick(String command, int mouseButton, Minecraft mc) throws IOException {
        if ("profile_manager".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiProfileManager(null));
            return true;
        } else if ("chat_optimization".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiChatOptimization(null));
            return true;
        } else if ("toggle_auto_pickup".equals(command)) {
            if (mouseButton == 0) {
                AutoPickupHandler.globalEnabled = !AutoPickupHandler.globalEnabled;
                AutoPickupHandler.saveConfig();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiAutoPickupConfig(null));
            }
            return true;
        } else if ("toggle_auto_use_item".equals(command)) {
            if (mouseButton == 0) {
                AutoUseItemHandler.globalEnabled = !AutoUseItemHandler.globalEnabled;
                AutoUseItemHandler.INSTANCE.resetSchedule();
                AutoUseItemHandler.saveConfig();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiAutoUseItemConfig(null));
            }
            return true;
        } else if ("block_replacement_config".equals(command)) {
            if (mouseButton == 0 || mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiBlockReplacementConfig(null));
            }
            return true;
        } else if ("toggle_server_feature_visibility".equals(command)) {
            if (mouseButton == 0 || mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiServerFeatureVisibilityConfig(null));
            }
            return true;
        } else if ("setloop".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiLoopCountInput(null));
            return true;
        } else if ("autoeat".equals(command)) {
            if (mouseButton == 0) {
                AutoEatHandler.autoEatEnabled = !AutoEatHandler.autoEatEnabled;
                AutoEatHandler.saveAutoEatConfig();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiAutoEatConfig(null));
            }
            return true;
        } else if ("toggle_auto_fishing".equals(command)) {
            if (mouseButton == 0) {
                AutoFishingHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiAutoFishingConfig(null));
            }
            return true;
        } else if ("toggle_mouse_detach".equals(command)) {
            ModConfig.isMouseDetached = !ModConfig.isMouseDetached;
            String mouseStatus = ModConfig.isMouseDetached ? I18n.format("gui.inventory.mouse.detached")
                    : I18n.format("gui.inventory.mouse.reattached");
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.inventory.mouse_toggle", mouseStatus)));
            }
            if (!ModConfig.isMouseDetached && mc.currentScreen == null) {
                mc.mouseHelper.grabMouseCursor();
            }
            refreshGuiLists();
            return true;
        } else if ("followconfig".equals(command)) {
            if (mouseButton == 0) {
                boolean wasActive = AutoFollowHandler.getActiveRule() != null;
                if (wasActive) {
                    AutoFollowHandler.toggleEnabledFromQuickSwitch();
                    if (mc.player != null) {
                        mc.player.sendMessage(new TextComponentString("§b[自动追怪] §c已关闭"));
                    }
                } else {
                    if (!AutoFollowHandler.hasAnyRuleConfigured()) {
                        if (mc.player != null) {
                            mc.player.sendMessage(new TextComponentString("§b[自动追怪] §e未配置任何规则，请右键打开配置界面"));
                        }
                    } else {
                        AutoFollowRule activatedRule = AutoFollowHandler.toggleEnabledFromQuickSwitch();
                        if (mc.player != null) {
                            mc.player.sendMessage(new TextComponentString("§b[自动追怪] §a已开启"
                                    + (activatedRule != null && activatedRule.name != null
                                            && !activatedRule.name.trim().isEmpty()
                                                    ? " §7规则: §f" + activatedRule.name.trim()
                                                    : "")));
                        }
                    }
                }
                refreshGuiLists();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiAutoFollowManager(null));
            }
            return true;
        } else if ("conditional_execution".equals(command)) {
            if (mouseButton == 0) {
                ConditionalExecutionHandler.setGlobalEnabled(!ConditionalExecutionHandler.isGloballyEnabled());
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiConditionalExecutionManager(null));
            }
            return true;
        } else if ("auto_escape".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiAutoEscapeManager(null));
            return true;
        } else if ("keybind_manager".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiKeybindManager(null));
            return true;
        } else if ("warehouse_manager".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiWarehouseManager(null));
            return true;
        } else if ("baritone_settings".equals(command)) {
            closeOverlay();
            mc.displayGuiScreen(new GuiBaritoneCommandTable(null));
            return true;
        } else if ("baritone_parkour".equals(command)) {
            if (mouseButton == 0) {
                BaritoneParkourSettingsHelper.toggleParkourMode();
                refreshGuiLists();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiBaritoneParkourSettings(null));
            }
            return true;
        } else if ("toggle_kill_aura".equals(command)) {
            if (mouseButton == 0) {
                KillAuraHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiKillAuraConfig(null));
            }
            return true;
        } else if ("toggle_fly".equals(command)) {
            if (mouseButton == 0) {
                FlyHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiFlyConfig(null));
            }
            return true;
        }

        return false;
    }


}

