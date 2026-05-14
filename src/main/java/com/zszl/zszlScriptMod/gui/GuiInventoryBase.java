// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/GuiInventory.java
package com.zszl.zszlScriptMod.gui;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.path.GuiPathManager;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.FeatureDef;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.utils.AdExpListManager;
import com.zszl.zszlScriptMod.utils.DonationLeaderboardManager;
import com.zszl.zszlScriptMod.utils.EnhancementAttrManager;
import com.zszl.zszlScriptMod.utils.HallOfFameManager;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.TitleCompendiumManager;
import com.zszl.zszlScriptMod.utils.UpdateChecker;
import com.zszl.zszlScriptMod.zszlScriptMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

abstract class GuiInventoryBase {

    protected static final int BASE_SIDE_BUTTON_COLUMN_WIDTH = 80;
    protected static final int BASE_TOTAL_WIDTH = 420;
    protected static final int BASE_TOTAL_HEIGHT = 280;
    protected static final int BASE_GAP = 10;
    protected static final int BASE_PADDING = 5;
    protected static final int BASE_CATEGORY_PANEL_WIDTH = 110;
    protected static final int BASE_CATEGORY_BUTTON_WIDTH = 96;
    protected static final int BASE_CATEGORY_BUTTON_HEIGHT = 22;
    protected static final int BASE_ITEM_BUTTON_WIDTH = 84;
    protected static final int BASE_ITEM_BUTTON_HEIGHT = 22;
    protected static final int BASE_TOP_BUTTON_WIDTH = 60;
    protected static final int BASE_TOP_BUTTON_HEIGHT = 16;
    protected static final int BASE_CONTENT_X_OFFSET = 122;
    protected static final int CATEGORY_PANEL_MIN_BASE_WIDTH = 92;
    protected static final int CATEGORY_PANEL_MAX_BASE_WIDTH = 180;
    protected static final String UNCATEGORIZED_SECTION_TITLE = "未分类";
    protected static final String ILLEGAL_CATEGORY_NAME_CHARS = "\\/:*?\"<>|";
    protected static final long RECENT_OPEN_HIGHLIGHT_WINDOW_MS = 10L * 60L * 1000L;
    protected static final String SEARCH_SCOPE_CURRENT_SUBCATEGORY = "current_subcategory";
    protected static final String SEARCH_SCOPE_CURRENT_CATEGORY = "current_category";
    protected static final String SEARCH_SCOPE_ALL_CATEGORIES = "all_categories";

    protected static class OverlayMetrics {
        float scale;
        int sideButtonColumnWidth;
        int totalWidth;
        int totalHeight;
        int x;
        int y;

        int gap;
        int padding;

        int pathManagerButtonWidth;
        int stopButtonWidth;
        int topButtonHeight;

        int topBarHeight;
        int contentStartY;

        int categoryPanelWidth;
        int categoryButtonWidth;
        int categoryButtonHeight;
        int categoryItemHeight;

        int contentPanelX;
        int contentPanelRight;
        int categoryDividerX;
        int categoryDividerWidth;

        int itemButtonWidth;
        int itemButtonHeight;

        int pageButtonWidth;
        int autoPauseButtonWidth;

        int sideButtonWidth;
        int sideButtonHeight;
    }

    protected static int scaleUi(int base, float scale) {
        return Math.max(1, Math.round(base * scale));
    }

    protected static float computeUiScale(int screenWidth, int screenHeight) {
        float sx = screenWidth / 460.0f;
        float sy = screenHeight / 300.0f;
        float s = Math.min(1.0f, Math.min(sx, sy));
        return MathHelper.clamp(s, 0.72f, 1.0f);
    }

    protected static String buildOverlayTitle() {
        if (merchantScreenActive) {
            return I18n.format("gui.inventory.merchant.title");
        }
        if (otherFeaturesScreenActive) {
            return I18n.format("gui.inventory.other_features.title");
        }
        return "";
    }

    protected static List<String> buildOverlayHeaderLines() {
        List<String> lines = new ArrayList<>();
        if (merchantScreenActive) {
            lines.add(I18n.format("gui.inventory.merchant.title"));
            return lines;
        }
        if (otherFeaturesScreenActive) {
            lines.add(I18n.format("gui.inventory.other_features.title"));
            return lines;
        }

        lines.add(I18n.format("gui.inventory.profile", ProfileManager.getActiveProfileName()));
        lines.add(I18n.format("gui.inventory.loop.progress", Math.max(0, loopCounter), formatLoopTargetForHeader()));
        return lines;
    }

    protected static String formatLoopTargetForHeader() {
        if (loopCount < 0) {
            return "∞";
        }
        return String.valueOf(Math.max(0, loopCount));
    }

    protected static OverlayMetrics computeOverlayMetrics(int screenWidth, int screenHeight, FontRenderer fontRenderer,
            String title) {
        OverlayMetrics m = new OverlayMetrics();
        m.scale = computeUiScale(screenWidth, screenHeight);
        m.gap = scaleUi(BASE_GAP, m.scale);
        m.padding = scaleUi(BASE_PADDING, m.scale);

        m.sideButtonColumnWidth = scaleUi(BASE_SIDE_BUTTON_COLUMN_WIDTH, m.scale);
        m.totalWidth = scaleUi(BASE_TOTAL_WIDTH, m.scale);
        m.totalHeight = scaleUi(BASE_TOTAL_HEIGHT, m.scale);

        int maxUsableWidth = screenWidth - m.padding * 2;
        int maxPanelWidth = maxUsableWidth - m.sideButtonColumnWidth - m.gap;
        m.totalWidth = Math.max(260, Math.min(m.totalWidth, maxPanelWidth));
        m.totalHeight = Math.max(190, Math.min(m.totalHeight, screenHeight - m.padding * 2));

        m.x = (screenWidth - m.totalWidth) / 2 + (m.sideButtonColumnWidth / 2);
        m.y = (screenHeight - m.totalHeight) / 2;

        m.pathManagerButtonWidth = scaleUi(BASE_TOP_BUTTON_WIDTH, m.scale);
        m.stopButtonWidth = scaleUi(BASE_TOP_BUTTON_WIDTH, m.scale);
        m.topButtonHeight = scaleUi(BASE_TOP_BUTTON_HEIGHT, m.scale);

        if (!merchantScreenActive && !otherFeaturesScreenActive) {
            int lineHeight = fontRenderer.FONT_HEIGHT + scaleUi(2, m.scale);
            m.topBarHeight = Math.max(scaleUi(30, m.scale), lineHeight * 2);
            if (GuiInventory.isCustomCategorySelection() && GuiInventory.isCustomSearchExpanded()) {
                m.topBarHeight = Math.max(m.topBarHeight, m.topButtonHeight * 2 + scaleUi(10, m.scale));
            }
        } else {
            int pathManagerButtonX = m.x + m.totalWidth - m.pathManagerButtonWidth - m.padding;
            int stopButtonX = pathManagerButtonX - m.stopButtonWidth - m.padding;
            int titleAreaStartX = m.x + scaleUi(8, m.scale);
            int titleAreaEndX = stopButtonX - scaleUi(8, m.scale)
                    - GuiInventory.getCustomSearchHeaderReservedWidth(fontRenderer, m.scale);
            int titleAreaWidth = Math.max(80, titleAreaEndX - titleAreaStartX);
            List<String> titleLines = fontRenderer.listFormattedStringToWidth(title, titleAreaWidth);
            int titleTotalHeight = titleLines.size() * (fontRenderer.FONT_HEIGHT + scaleUi(2, m.scale));
            m.topBarHeight = Math.max(scaleUi(24, m.scale), titleTotalHeight);
        }
        m.contentStartY = m.y + m.topBarHeight + scaleUi(6, m.scale);

        int storedCategoryBaseWidth = MainUiLayoutManager.getCategoryPanelBaseWidth();
        int categoryBaseWidth = storedCategoryBaseWidth > 0 ? clampCategoryPanelBaseWidth(storedCategoryBaseWidth)
                : computeAutoCategoryPanelBaseWidth(fontRenderer);
        m.categoryPanelWidth = scaleUi(categoryBaseWidth, m.scale);
        int categoryButtonMaxWidth = Math.max(scaleUi(40, m.scale),
                m.categoryPanelWidth - m.padding * 2 - scaleUi(8, m.scale));
        m.categoryButtonWidth = Math.min(Math.max(scaleUi(BASE_CATEGORY_BUTTON_WIDTH, m.scale),
                m.categoryPanelWidth - m.padding * 2 - scaleUi(12, m.scale)), categoryButtonMaxWidth);
        m.categoryButtonHeight = scaleUi(BASE_CATEGORY_BUTTON_HEIGHT, m.scale);
        m.categoryItemHeight = m.categoryButtonHeight + m.padding;

        m.categoryDividerWidth = Math.max(5, scaleUi(6, m.scale));
        m.contentPanelX = m.x + m.padding + m.categoryPanelWidth + m.gap;
        m.categoryDividerX = m.contentPanelX - (m.gap / 2) - (m.categoryDividerWidth / 2);
        m.contentPanelRight = m.x + m.totalWidth - m.padding;

        m.itemButtonWidth = scaleUi(BASE_ITEM_BUTTON_WIDTH, m.scale);
        m.itemButtonHeight = scaleUi(BASE_ITEM_BUTTON_HEIGHT, m.scale);

        m.pageButtonWidth = scaleUi(60, m.scale);
        m.autoPauseButtonWidth = scaleUi(80, m.scale);

        m.sideButtonWidth = scaleUi(70, m.scale);
        m.sideButtonHeight = scaleUi(20, m.scale);

        int sideButtonLeft = m.x - m.sideButtonColumnWidth - m.gap;
        int minX = m.padding + m.sideButtonColumnWidth + m.gap;
        if (sideButtonLeft < m.padding) {
            m.x = Math.max(minX, m.x + (m.padding - sideButtonLeft));
        }
        if (m.x + m.totalWidth > screenWidth - m.padding) {
            m.x = Math.max(minX, screenWidth - m.padding - m.totalWidth);
        }

        return m;
    }

    protected static final List<String> BUILTIN_SECOND_PAGE_GROUP_KEYS = Arrays.asList(
            "gui.inventory.builtin.evac.helipad", "gui.inventory.builtin.evac.desert_palace",
            "gui.inventory.builtin.evac.transfer_station", "gui.inventory.builtin.evac.snow_inn",
            "gui.inventory.builtin.evac.watchtower", "gui.inventory.builtin.evac.shore_house",
            "gui.inventory.builtin.evac.cliff_house", "gui.inventory.builtin.evac.monitor_station",
            "gui.inventory.builtin.evac.rejoin");

    protected static int colorChangeTicker = 0;
    protected static final List<TextFormatting> RAINBOW_COLORS = Arrays.asList(TextFormatting.RED, TextFormatting.GOLD,
            TextFormatting.YELLOW, TextFormatting.GREEN, TextFormatting.AQUA, TextFormatting.BLUE,
            TextFormatting.LIGHT_PURPLE);

    protected static String sLastCategory = "gui.inventory.category.builtin_script";
    protected static int sLastPage = 0;
    protected static final Map<String, Integer> CATEGORY_PAGE_MAP = new HashMap<>();
    protected static int currentPage = sLastPage;
    protected static String currentCategory = sLastCategory;
    protected static List<String> categories = new ArrayList<>();
    protected static final Map<String, List<String>> categoryItems = new HashMap<>();
    protected static final Map<String, List<String>> categoryItemNames = new HashMap<>();
    protected static final Map<String, String> itemTooltips = new HashMap<>();
    protected static final int COMMON_ROWS_PER_PAGE = 6;

    protected static class GroupedItemSection {
        final String key;
        final String title;
        final List<String> commands;

        GroupedItemSection(String key, String title, List<String> commands) {
            this.key = key;
            this.title = title;
            this.commands = new ArrayList<>(commands);
        }
    }

    protected static class CommonContentRow {
        final boolean header;
        final String sectionKey;
        final String title;
        final boolean expanded;
        final List<String> commands;

        protected CommonContentRow(boolean header, String sectionKey, String title, boolean expanded,
                List<String> commands) {
            this.header = header;
            this.sectionKey = sectionKey;
            this.title = title;
            this.expanded = expanded;
            this.commands = commands == null ? Collections.emptyList() : new ArrayList<>(commands);
        }

        static CommonContentRow header(String sectionKey, String title, boolean expanded) {
            return new CommonContentRow(true, sectionKey, title, expanded, Collections.emptyList());
        }

        static CommonContentRow items(String sectionKey, List<String> commands) {
            return new CommonContentRow(false, sectionKey, "", false, commands);
        }
    }

    protected static class CategoryTreeRow {
        final String category;
        final String subCategory;
        final boolean systemCategory;
        Rectangle bounds;

        protected CategoryTreeRow(String category, String subCategory, boolean systemCategory) {
            this.category = category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.systemCategory = systemCategory;
        }

        boolean isSubCategory() {
            return !subCategory.isEmpty();
        }

        boolean isCustomCategoryRoot() {
            return !systemCategory && subCategory.isEmpty();
        }

        boolean isDroppableTarget() {
            return !systemCategory;
        }

        String getPageKey() {
            return isSubCategory() ? category + "::" + subCategory : category;
        }
    }

    protected static class SequenceCardRenderInfo {
        final PathSequence sequence;
        final Rectangle bounds;
        final String displayName;
        final String secondaryText;
        final String tooltip;

        protected SequenceCardRenderInfo(PathSequence sequence, Rectangle bounds, String displayName,
                String secondaryText, String tooltip) {
            this.sequence = sequence;
            this.bounds = bounds;
            this.displayName = displayName;
            this.secondaryText = secondaryText;
            this.tooltip = tooltip;
        }
    }

    protected static class CustomSectionRenderInfo {
        final String key;
        final String title;
        final String subCategory;
        final Rectangle bounds;

        protected CustomSectionRenderInfo(String key, String title, String subCategory, Rectangle bounds) {
            this.key = key;
            this.title = title;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.bounds = bounds;
        }
    }

    protected static class CustomSequenceDropTarget {
        final String category;
        final String subCategory;
        final Rectangle bounds;

        protected CustomSequenceDropTarget(String category, String subCategory, Rectangle bounds) {
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.bounds = bounds;
        }

        boolean isSubCategory() {
            return !normalizeText(subCategory).isEmpty();
        }

        boolean matches(String targetCategory, String targetSubCategory) {
            return normalizeText(category).equals(normalizeText(targetCategory))
                    && normalizeText(subCategory).equalsIgnoreCase(normalizeText(targetSubCategory));
        }
    }

    protected static class CustomButtonRenderInfo {
        final String action;
        final String category;
        final String subCategory;
        final Rectangle bounds;

        protected CustomButtonRenderInfo(String action, String category, String subCategory, Rectangle bounds) {
            this.action = action;
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.bounds = bounds;
        }
    }

    protected static class CustomSectionModel {
        final String key;
        final String category;
        final String title;
        final String subCategory;
        final List<PathSequence> sequences;
        final String statsLabel;

        protected CustomSectionModel(String key, String category, String title, String subCategory,
                List<PathSequence> sequences) {
            this.key = key;
            this.category = category == null ? "" : category;
            this.title = title;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.sequences = sequences == null ? Collections.emptyList() : new ArrayList<>(sequences);
            this.statsLabel = GuiInventory.buildCustomSectionStatsLabel(this.sequences);
        }
    }

    protected static class CustomSectionChunk {
        final CustomSectionModel model;
        final List<PathSequence> pageSequences;
        final boolean continuation;

        protected CustomSectionChunk(CustomSectionModel model, List<PathSequence> pageSequences, boolean continuation) {
            this.model = model;
            this.pageSequences = pageSequences == null ? Collections.emptyList() : new ArrayList<>(pageSequences);
            this.continuation = continuation;
        }
    }

    protected static class CustomGridMetrics {
        int startX;
        int startY;
        int width;
        int height;
        int gap;
        int columns;
        int rowsPerPage;
        int cardWidth;
        int cardHeight;
        int pageSize;
    }

    protected static class CustomPageLayout {
        final CustomGridMetrics grid;
        final List<CustomSectionChunk> sections;
        final int totalPages;
        final int searchToggleX;
        final int searchToggleY;
        final int searchToggleWidth;
        final int searchToggleHeight;
        final int searchFieldX;
        final int searchFieldY;
        final int searchFieldWidth;
        final int searchFieldHeight;
        final int searchScopeX;
        final int searchScopeY;
        final int searchScopeWidth;
        final int searchScopeHeight;
        final int toolbarY;
        final int toolbarHeight;

        protected CustomPageLayout(CustomGridMetrics grid, List<CustomSectionChunk> sections, int totalPages,
                int searchToggleX, int searchToggleY, int searchToggleWidth, int searchToggleHeight, int searchFieldX,
                int searchFieldY, int searchFieldWidth, int searchFieldHeight, int searchScopeX, int searchScopeY,
                int searchScopeWidth, int searchScopeHeight, int toolbarY, int toolbarHeight) {
            this.grid = grid;
            this.sections = sections == null ? Collections.emptyList() : new ArrayList<>(sections);
            this.totalPages = Math.max(1, totalPages);
            this.searchToggleX = searchToggleX;
            this.searchToggleY = searchToggleY;
            this.searchToggleWidth = searchToggleWidth;
            this.searchToggleHeight = searchToggleHeight;
            this.searchFieldX = searchFieldX;
            this.searchFieldY = searchFieldY;
            this.searchFieldWidth = searchFieldWidth;
            this.searchFieldHeight = searchFieldHeight;
            this.searchScopeX = searchScopeX;
            this.searchScopeY = searchScopeY;
            this.searchScopeWidth = searchScopeWidth;
            this.searchScopeHeight = searchScopeHeight;
            this.toolbarY = toolbarY;
            this.toolbarHeight = toolbarHeight;
        }
    }

    protected static class MainPageControlBounds {
        final Rectangle containerBounds;
        final Rectangle prevButtonBounds;
        final Rectangle nextButtonBounds;
        final Rectangle pageInfoBounds;
        final Rectangle pageTrackBounds;
        final Rectangle autoPauseButtonBounds;

        protected MainPageControlBounds(Rectangle containerBounds, Rectangle prevButtonBounds,
                Rectangle nextButtonBounds, Rectangle pageInfoBounds, Rectangle pageTrackBounds,
                Rectangle autoPauseButtonBounds) {
            this.containerBounds = containerBounds;
            this.prevButtonBounds = prevButtonBounds;
            this.nextButtonBounds = nextButtonBounds;
            this.pageInfoBounds = pageInfoBounds;
            this.pageTrackBounds = pageTrackBounds;
            this.autoPauseButtonBounds = autoPauseButtonBounds;
        }
    }

    protected static class ContextMenuItem {
        final String label;
        final Runnable action;
        final List<ContextMenuItem> children = new ArrayList<>();
        boolean enabled = true;
        boolean selected = false;

        protected ContextMenuItem(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        ContextMenuItem child(ContextMenuItem item) {
            if (item != null) {
                children.add(item);
            }
            return this;
        }

        ContextMenuItem enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        ContextMenuItem selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    protected static class ContextMenuLayer {
        final List<ContextMenuItem> items;
        final List<Rectangle> itemBounds = new ArrayList<>();
        Rectangle bounds;
        int x;
        int y;
        int width;

        protected ContextMenuLayer(List<ContextMenuItem> items) {
            this.items = items;
        }
    }

    protected static final List<GroupedItemSection> commonItemSections = new ArrayList<>();
    protected static final Map<String, Boolean> commonSectionExpanded = new HashMap<>();
    protected static final List<CategoryTreeRow> visibleCategoryRows = new ArrayList<>();
    protected static final List<SequenceCardRenderInfo> visibleCustomSequenceCards = new ArrayList<>();
    protected static final List<CustomSectionRenderInfo> visibleCustomSectionHeaders = new ArrayList<>();
    protected static final List<CustomSequenceDropTarget> visibleCustomSectionDropTargets = new ArrayList<>();
    protected static final List<CustomButtonRenderInfo> visibleCustomSearchScopeButtons = new ArrayList<>();
    protected static final List<CustomButtonRenderInfo> visibleCustomToolbarButtons = new ArrayList<>();
    protected static final List<CustomButtonRenderInfo> visibleCustomEmptySectionButtons = new ArrayList<>();
    protected static final Map<String, Boolean> customSectionExpanded = new HashMap<>();
    protected static final String CMD_BUILTIN_PRIMARY_PREFIX = "builtin_primary:";
    protected static final String CMD_BUILTIN_PRIMARY_BACK = "builtin_primary_back";
    protected static final String CMD_BUILTIN_SUBCAT_PREFIX = "builtin_subcat:";
    protected static final String CMD_BUILTIN_SUBCAT_BACK = "builtin_subcat_back";
    protected static String builtinScriptPrimaryCategory = null;
    protected static String builtinScriptSubCategory = null;
    protected static String currentCustomSubCategory = "";

    public static int loopCount = 1;
    public static int loopCounter = 0;
    public static boolean isLooping = false;

    public static boolean lockGameInteraction = false;

    protected static boolean isDebugRecordingMenuVisible = false;
    protected static int debugCategoryRightClickCounter = 0;
    protected static long lastDebugCategoryRightClickTime = 0;

    protected static int categoryScrollOffset = 0;
    protected static int maxCategoryScroll = 0;
    public static boolean isDraggingCategoryScrollbar = false;
    protected static int categoryScrollClickY = 0;
    protected static int initialCategoryScrollOffset = 0;
    protected static boolean isDraggingCategoryRow = false;
    protected static CategoryTreeRow pressedCategoryRow = null;
    protected static Rectangle pressedCategoryRowRect = null;
    protected static int pressedCategoryRowMouseX = 0;
    protected static int pressedCategoryRowMouseY = 0;
    protected static int draggingCategoryRowMouseX = 0;
    protected static int draggingCategoryRowMouseY = 0;
    protected static CategoryTreeRow currentCategorySortDropTarget = null;
    protected static boolean currentCategorySortDropAfter = false;
    protected static boolean isDraggingCustomSequenceCard = false;
    protected static PathSequence pressedCustomSequence = null;
    protected static Rectangle pressedCustomSequenceRect = null;
    protected static int pressedCustomSequenceMouseX = 0;
    protected static int pressedCustomSequenceMouseY = 0;
    protected static int draggingCustomSequenceMouseX = 0;
    protected static int draggingCustomSequenceMouseY = 0;
    protected static CustomSequenceDropTarget currentSequenceDropTarget = null;
    protected static String currentCustomSequenceSortTargetName = "";
    protected static boolean currentCustomSequenceSortAfter = false;
    protected static boolean contextMenuVisible = false;
    protected static int contextMenuAnchorX = 0;
    protected static int contextMenuAnchorY = 0;
    protected static final List<ContextMenuItem> contextMenuRootItems = new ArrayList<>();
    protected static final List<ContextMenuLayer> contextMenuLayers = new ArrayList<>();
    protected static final List<Integer> contextMenuOpenPath = new ArrayList<>();
    protected static final List<Integer> contextMenuKeyboardSelectionPath = new ArrayList<>();
    protected static GuiTextField customSequenceSearchField;
    protected static String customSequenceSearchQuery = "";
    protected static String customSequenceSearchScope = SEARCH_SCOPE_CURRENT_CATEGORY;
    protected static boolean customSequenceSearchExpanded = false;
    protected static boolean customSequenceSearchFocusPending = false;
    protected static final Set<String> selectedCustomSequenceNames = new LinkedHashSet<>();
    protected static Rectangle customSearchClearButtonBounds;
    protected static Rectangle customSearchToggleButtonBounds;
    protected static boolean isDraggingCategoryDivider = false;
    protected static int categoryDividerMouseOffsetX = 0;
    protected static long customSequencePageTurnLockUntil = 0L;
    protected static Rectangle categoryDividerBounds;
    protected static Rectangle versionClickArea;
    protected static Rectangle authorClickArea;

    // --- 新增：左侧功能按钮列表 ---
    protected static final List<GuiButton> sideButtons = new ArrayList<>();
    protected static final int BTN_ID_THEME_CONFIG = 1000;
    protected static final int BTN_ID_UPDATE = 1001;
    protected static final int BTN_ID_HALL_OF_FAME = 1002;
    protected static final int BTN_ID_TITLE_COMPENDIUM = 1003;
    protected static final int BTN_ID_ENHANCEMENT_ATTR = 1004;
    protected static final int BTN_ID_AD_EXP_LIST = 1005;
    protected static final int BTN_ID_MERCHANT = 1007;
    protected static final int BTN_ID_DONATE = 1006;
    protected static final int BTN_ID_PERFORMANCE_MONITOR = 1008;
    // --- 新增结束 ---

    protected static boolean isRslFeaturesHidden() {
        return ServerFeatureVisibilityManager.shouldHideRslFeatures()
                || PathSequenceManager.isCategoryHidden(I18n.format("path.category.builtin"));
    }

    protected static boolean merchantScreenActive = false;
    protected static int merchantScreenPage = 0;
    protected static int selectedMerchantIndex = 0;
    protected static int selectedMerchantCategoryIndex = -1;
    protected static int merchantCategoryScrollOffset = 0;
    protected static int merchantListScrollOffset = 0;
    protected static int maxMerchantListScroll = 0;
    protected static boolean isDraggingMerchantListScrollbar = false;
    protected static boolean otherFeaturesScreenActive = false;
    protected static int selectedOtherFeatureGroupIndex = -1;
    protected static int otherFeatureGroupScrollOffset = 0;
    protected static int maxOtherFeatureGroupScroll = 0;
    protected static boolean isDraggingOtherFeatureGroupScrollbar = false;
    protected static int otherFeatureScreenPage = 0;
    protected static boolean masterStatusHudEditMode = false;
    protected static boolean isDraggingMasterStatusHud = false;
    protected static int masterStatusHudDragOffsetX = 0;
    protected static int masterStatusHudDragOffsetY = 0;
    protected static Rectangle masterStatusHudEditorBounds = null;
    protected static Rectangle masterStatusHudExitButtonBounds = null;
    protected static boolean masterStatusHudEditPreviousMouseDetached = false;

    protected static final class OtherFeatureCardLayout {
        protected final FeatureDef feature;
        protected final Rectangle bounds;

        protected OtherFeatureCardLayout(FeatureDef feature, Rectangle bounds) {
            this.feature = feature;
            this.bounds = bounds;
        }
    }

    protected static final class OtherFeaturePageControlBounds {
        protected final Rectangle containerBounds;
        protected final Rectangle prevButtonBounds;
        protected final Rectangle nextButtonBounds;
        protected final Rectangle pageInfoBounds;

        protected OtherFeaturePageControlBounds(Rectangle containerBounds, Rectangle prevButtonBounds,
                Rectangle nextButtonBounds, Rectangle pageInfoBounds) {
            this.containerBounds = containerBounds;
            this.prevButtonBounds = prevButtonBounds;
            this.nextButtonBounds = nextButtonBounds;
            this.pageInfoBounds = pageInfoBounds;
        }
    }

    protected static final class OtherFeaturePageLayout {
        protected final List<OtherFeatureCardLayout> cards;
        protected final Rectangle cardAreaBounds;
        protected final int currentPage;
        protected final int totalPages;
        protected final OtherFeaturePageControlBounds pageControls;

        protected OtherFeaturePageLayout(List<OtherFeatureCardLayout> cards, Rectangle cardAreaBounds, int currentPage,
                int totalPages, OtherFeaturePageControlBounds pageControls) {
            this.cards = cards;
            this.cardAreaBounds = cardAreaBounds;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.pageControls = pageControls;
        }
    }

    public static void onOpen() {
        merchantScreenActive = false;
        isDraggingMerchantListScrollbar = false;
        maxOtherFeatureGroupScroll = 0;
        isDraggingOtherFeatureGroupScrollbar = false;
        otherFeatureScreenPage = 0;
        masterStatusHudEditMode = false;
        isDraggingMasterStatusHud = false;
        masterStatusHudEditorBounds = null;
        masterStatusHudExitButtonBounds = null;
        isDraggingCategoryDivider = false;
        isDraggingCategoryRow = false;
        pressedCategoryRow = null;
        pressedCategoryRowRect = null;
        currentCategorySortDropTarget = null;
        pressedCustomSequence = null;
        pressedCustomSequenceRect = null;
        isDraggingCustomSequenceCard = false;
        currentSequenceDropTarget = null;
        currentCustomSequenceSortTargetName = "";
        currentCustomSequenceSortAfter = false;
        categoryDividerBounds = null;
        customSequenceSearchQuery = "";
        customSequenceSearchScope = SEARCH_SCOPE_CURRENT_CATEGORY;
        customSequenceSearchExpanded = false;
        customSequenceSearchFocusPending = false;
        customSequenceSearchField = null;
        customSearchClearButtonBounds = null;
        customSearchToggleButtonBounds = null;
        selectedCustomSequenceNames.clear();
        visibleCustomSearchScopeButtons.clear();
        visibleCustomToolbarButtons.clear();
        visibleCustomEmptySectionButtons.clear();
        closeContextMenu();
        normalizeCategoryState();
        PathSequenceManager.initializePathSequences();
        MainUiLayoutManager.ensureLoaded();
        refreshGuiLists();
        isDebugRecordingMenuVisible = false;
        UpdateChecker.fetchVersionAndChangelog();
        UpdateChecker.notifyIfNewVersion();
        PacketCaptureHandler.notifyIfSessionIdMissing();

        rebuildSideButtons();
    }

    public static void openOverlayScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }

        onOpen();
        zszlScriptMod.isGuiVisible = true;
        mc.displayGuiScreen(new GuiInventoryOverlayScreen());
    }

    protected static void rebuildSideButtons() {
        boolean hideRsl = isRslFeaturesHidden();
        sideButtons.clear();
        // 始终显示：主题配置、更新脚本、打赏
        sideButtons.add(new ThemedButton(BTN_ID_THEME_CONFIG, 0, 0, 70, 20, I18n.format("gui.inventory.theme_config")));
        sideButtons.add(new ThemedButton(BTN_ID_UPDATE, 0, 0, 70, 20, I18n.format("gui.inventory.update_script")));
        sideButtons.add(new ThemedButton(BTN_ID_DONATE, 0, 0, 70, 20, I18n.format("gui.inventory.donate")));

        if (!hideRsl) {
            // --- 仅在显示再生之路内容时：预加载并显示其余侧栏按钮 ---
            HallOfFameManager.fetchContent();
            TitleCompendiumManager.fetchContent();
            EnhancementAttrManager.fetchContent();
            AdExpListManager.fetchContent();
            DonationLeaderboardManager.fetchContent();

            sideButtons.add(
                    new ThemedButton(BTN_ID_HALL_OF_FAME, 0, 0, 70, 20, I18n.format("gui.inventory.hall_of_fame")));
            sideButtons.add(new ThemedButton(BTN_ID_TITLE_COMPENDIUM, 0, 0, 70, 20,
                    I18n.format("gui.inventory.title_compendium")));
            sideButtons.add(new ThemedButton(BTN_ID_ENHANCEMENT_ATTR, 0, 0, 70, 20,
                    I18n.format("gui.inventory.enhancement_attr")));
            sideButtons
                    .add(new ThemedButton(BTN_ID_AD_EXP_LIST, 0, 0, 70, 20, I18n.format("gui.inventory.ad_exp_list")));
            sideButtons.add(new ThemedButton(BTN_ID_MERCHANT, 0, 0, 70, 20, I18n.format("gui.inventory.merchant")));
        }
        // --- 新增结束 ---
    }

    protected static void updateButtonPositions(int screenWidth, int screenHeight) {
        if (sideButtons.isEmpty()) {
            return;
        }

        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String title = buildOverlayTitle();
        OverlayMetrics m = computeOverlayMetrics(screenWidth, screenHeight, fontRenderer, title);

        int sideButtonCount = sideButtons.size();
        int sidePanelX = m.x - m.sideButtonColumnWidth - m.gap;
        int sidePanelY = m.y;

        for (GuiButton button : sideButtons) {
            button.width = m.sideButtonWidth;
            button.height = m.sideButtonHeight;
            button.x = sidePanelX + (m.sideButtonColumnWidth - button.width) / 2;
        }

        int topY = sidePanelY + m.padding;
        int bottomY = sidePanelY + m.totalHeight - m.padding - m.sideButtonHeight;
        for (int i = 0; i < sideButtonCount; i++) {
            GuiButton button = sideButtons.get(i);
            if (sideButtonCount == 1) {
                button.y = topY;
            } else {
                float t = (float) i / (float) (sideButtonCount - 1);
                button.y = Math.round(topY + t * (bottomY - topY));
            }
        }

        // 更新版本和作者点击区域
        int topInfoY = m.y - fontRenderer.FONT_HEIGHT - m.padding;
        String versionText = I18n.format("gui.inventory.version", zszlScriptMod.VERSION, UpdateChecker.latestVersion);
        int versionX = m.x;
        versionClickArea = new Rectangle(versionX, topInfoY, fontRenderer.getStringWidth(versionText), 10);

        String authorText = I18n.format("gui.inventory.author");
        int authorX = m.x + m.totalWidth - fontRenderer.getStringWidth(authorText);
        authorClickArea = new Rectangle(authorX, topInfoY, fontRenderer.getStringWidth(authorText), 10);
    }

    protected static OverlayMetrics getCurrentOverlayMetrics(int screenWidth, int screenHeight) {
        return computeOverlayMetrics(screenWidth, screenHeight, Minecraft.getMinecraft().fontRenderer,
                buildOverlayTitle());
    }

    protected static int scaleRawMouseX(int rawMouseX, int screenWidth) {
        return rawMouseX * screenWidth / Minecraft.getMinecraft().displayWidth;
    }

    protected static int scaleRawMouseY(int rawMouseY, int screenHeight) {
        return screenHeight - rawMouseY * screenHeight / Minecraft.getMinecraft().displayHeight - 1;
    }

    protected static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    protected static boolean shouldShowMainPageControls() {
        return !merchantScreenActive && !otherFeaturesScreenActive
                && !(isDebugRecordingMenuVisible
                        && I18n.format("gui.inventory.category.debug").equals(currentCategory));
    }

    protected static Rectangle getMainRightPanelBounds(OverlayMetrics m) {
        int x = m.contentPanelX;
        int y = m.contentStartY;
        int width = Math.max(0, m.contentPanelRight - m.contentPanelX);
        int bottom = m.y + m.totalHeight - m.padding;
        return new Rectangle(x, y, width, Math.max(0, bottom - y));
    }

    protected static MainPageControlBounds getMainPageControlBounds(OverlayMetrics m) {
        int pageButtonHeight = m.itemButtonHeight;
        int pageButtonWidth = m.pageButtonWidth;
        int autoPauseButtonWidth = m.autoPauseButtonWidth;
        int containerPadding = Math.max(4, scaleUi(5, m.scale));
        int buttonGap = Math.max(4, scaleUi(6, m.scale));
        int trackHeight = Math.max(4, scaleUi(4, m.scale));
        int containerHeight = Math.max(scaleUi(46, m.scale), pageButtonHeight + trackHeight + containerPadding * 3);
        int containerX = m.contentPanelX + m.padding;
        int containerWidth = Math.max(140, m.contentPanelRight - containerX - m.padding);
        int containerY = m.y + m.totalHeight - m.padding - containerHeight;

        int innerX = containerX + containerPadding;
        int innerRight = containerX + containerWidth - containerPadding;
        int rowY = containerY + containerPadding;
        int prevButtonX = innerX;
        int nextButtonX = prevButtonX + pageButtonWidth + buttonGap;
        int autoPauseButtonX = innerRight - autoPauseButtonWidth;
        int pageInfoX = nextButtonX + pageButtonWidth + buttonGap;
        int pageInfoWidth = Math.max(scaleUi(40, m.scale), autoPauseButtonX - pageInfoX - buttonGap);
        int trackY = containerY + containerHeight - containerPadding - trackHeight;
        int trackX = innerX;
        int trackWidth = Math.max(24, innerRight - innerX);

        return new MainPageControlBounds(
                new Rectangle(containerX, containerY, containerWidth, containerHeight),
                new Rectangle(prevButtonX, rowY, pageButtonWidth, pageButtonHeight),
                new Rectangle(nextButtonX, rowY, pageButtonWidth, pageButtonHeight),
                new Rectangle(pageInfoX, rowY, pageInfoWidth, pageButtonHeight),
                new Rectangle(trackX, trackY, trackWidth, trackHeight),
                new Rectangle(autoPauseButtonX, rowY, autoPauseButtonWidth, pageButtonHeight));
    }

    protected static Rectangle getMainPageTrackThumbBounds(Rectangle pageTrackBounds, int page, int totalPages) {
        if (pageTrackBounds == null) {
            return new Rectangle();
        }
        int trackWidth = Math.max(1, pageTrackBounds.width);
        int safeTotalPages = Math.max(1, totalPages);
        if (safeTotalPages <= 1) {
            return new Rectangle(pageTrackBounds.x, pageTrackBounds.y, trackWidth, pageTrackBounds.height);
        }

        int thumbWidth = Math.max(18, Math.round(trackWidth / (float) safeTotalPages));
        thumbWidth = Math.min(trackWidth, thumbWidth);
        int travelWidth = Math.max(0, trackWidth - thumbWidth);
        float ratio = MathHelper.clamp(page / (float) (safeTotalPages - 1), 0.0f, 1.0f);
        int thumbX = pageTrackBounds.x + Math.round(travelWidth * ratio);
        return new Rectangle(thumbX, pageTrackBounds.y, thumbWidth, pageTrackBounds.height);
    }

    protected static boolean setCurrentPage(int newPage, int totalPages) {
        int clampedTotalPages = Math.max(1, totalPages);
        int clampedPage = MathHelper.clamp(newPage, 0, clampedTotalPages - 1);
        if (clampedPage == currentPage) {
            return false;
        }
        currentPage = clampedPage;
        CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
        sLastPage = currentPage;
        return true;
    }

    protected static String drawMainPageControls(OverlayMetrics m, int contentStartY, int mouseX, int mouseY,
            FontRenderer fontRenderer) {
        if (!shouldShowMainPageControls()) {
            return null;
        }

        MainPageControlBounds pageControls = getMainPageControlBounds(m);
        setCurrentPage(currentPage, GuiInventory.getCurrentTotalPages(m, contentStartY));
        int totalPages = GuiInventory.getCurrentTotalPages(m, contentStartY);
        boolean canGoPrev = currentPage > 0;
        boolean canGoNext = currentPage < totalPages - 1;
        boolean isHoveringPrev = pageControls.prevButtonBounds.contains(mouseX, mouseY);
        boolean isHoveringNext = pageControls.nextButtonBounds.contains(mouseX, mouseY);
        boolean isHoveringAutoPause = pageControls.autoPauseButtonBounds.contains(mouseX, mouseY);
        boolean isHoveringTrack = pageControls.pageTrackBounds.contains(mouseX, mouseY);

        drawRect(pageControls.containerBounds.x, pageControls.containerBounds.y,
                pageControls.containerBounds.x + pageControls.containerBounds.width,
                pageControls.containerBounds.y + pageControls.containerBounds.height, 0x66324458);
        drawHorizontalLine(pageControls.containerBounds.x,
                pageControls.containerBounds.x + pageControls.containerBounds.width,
                pageControls.containerBounds.y, 0xAA4FA6D9);
        drawHorizontalLine(pageControls.containerBounds.x,
                pageControls.containerBounds.x + pageControls.containerBounds.width,
                pageControls.containerBounds.y + pageControls.containerBounds.height, 0xAA35536C);
        drawRect(pageControls.containerBounds.x, pageControls.containerBounds.y,
                pageControls.containerBounds.x + 1,
                pageControls.containerBounds.y + pageControls.containerBounds.height, 0x7735536C);
        drawRect(pageControls.containerBounds.x + pageControls.containerBounds.width - 1,
                pageControls.containerBounds.y,
                pageControls.containerBounds.x + pageControls.containerBounds.width,
                pageControls.containerBounds.y + pageControls.containerBounds.height, 0x7735536C);

        GuiTheme.drawButtonFrame(pageControls.prevButtonBounds.x, pageControls.prevButtonBounds.y,
                pageControls.prevButtonBounds.width, pageControls.prevButtonBounds.height,
                canGoPrev ? (isHoveringPrev ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL)
                        : GuiTheme.UiState.DISABLED);
        drawCenteredString(fontRenderer, I18n.format("gui.inventory.prev_page"),
                pageControls.prevButtonBounds.x + pageControls.prevButtonBounds.width / 2,
                pageControls.prevButtonBounds.y + (pageControls.prevButtonBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                canGoPrev ? 0xFFFFFFFF : 0xFF8E9AAC);

        GuiTheme.drawButtonFrame(pageControls.nextButtonBounds.x, pageControls.nextButtonBounds.y,
                pageControls.nextButtonBounds.width, pageControls.nextButtonBounds.height,
                canGoNext ? (isHoveringNext ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL)
                        : GuiTheme.UiState.DISABLED);
        drawCenteredString(fontRenderer, I18n.format("gui.inventory.next_page"),
                pageControls.nextButtonBounds.x + pageControls.nextButtonBounds.width / 2,
                pageControls.nextButtonBounds.y + (pageControls.nextButtonBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                canGoNext ? 0xFFFFFFFF : 0xFF8E9AAC);

        String pageInfo = String.format("%d / %d", currentPage + 1, totalPages);
        drawCenteredString(fontRenderer, pageInfo,
                pageControls.pageInfoBounds.x + pageControls.pageInfoBounds.width / 2,
                pageControls.pageInfoBounds.y + (pageControls.pageInfoBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                0xFFCFD9E6);

        Rectangle thumbBounds = getMainPageTrackThumbBounds(pageControls.pageTrackBounds, currentPage, totalPages);
        int thumbColor = isHoveringTrack ? 0xFF8FD8FF : 0xFF4FA6D9;
        drawRect(pageControls.pageTrackBounds.x, pageControls.pageTrackBounds.y,
                pageControls.pageTrackBounds.x + pageControls.pageTrackBounds.width,
                pageControls.pageTrackBounds.y + pageControls.pageTrackBounds.height, 0xAA1B2733);
        drawRect(pageControls.pageTrackBounds.x, pageControls.pageTrackBounds.y,
                thumbBounds.x + thumbBounds.width,
                pageControls.pageTrackBounds.y + pageControls.pageTrackBounds.height, 0x443E85B5);
        drawRect(thumbBounds.x, pageControls.pageTrackBounds.y - 1, thumbBounds.x + thumbBounds.width,
                pageControls.pageTrackBounds.y + pageControls.pageTrackBounds.height + 1, thumbColor);
        drawRect(thumbBounds.x, pageControls.pageTrackBounds.y - 1, thumbBounds.x + thumbBounds.width,
                pageControls.pageTrackBounds.y, 0xFFB6EAFF);

        String autoPauseText;
        GuiTheme.UiState autoPauseState;
        if (ModConfig.autoPauseOnMenuOpen) {
            autoPauseText = I18n.format("gui.inventory.auto_pause.on");
            autoPauseState = GuiTheme.UiState.SUCCESS;
        } else {
            autoPauseText = I18n.format("gui.inventory.auto_pause.off");
            autoPauseState = GuiTheme.UiState.DANGER;
        }
        if (isHoveringAutoPause && autoPauseState != GuiTheme.UiState.SUCCESS
                && autoPauseState != GuiTheme.UiState.DANGER) {
            autoPauseState = GuiTheme.UiState.HOVER;
        }
        GuiTheme.drawButtonFrame(pageControls.autoPauseButtonBounds.x, pageControls.autoPauseButtonBounds.y,
                pageControls.autoPauseButtonBounds.width, pageControls.autoPauseButtonBounds.height, autoPauseState);
        drawCenteredString(fontRenderer, autoPauseText,
                pageControls.autoPauseButtonBounds.x + pageControls.autoPauseButtonBounds.width / 2,
                pageControls.autoPauseButtonBounds.y
                        + (pageControls.autoPauseButtonBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                0xFFFFFFFF);

        if (isHoveringAutoPause) {
            return I18n.format("gui.inventory.tip.auto_pause");
        }
        if (isHoveringTrack || pageControls.pageInfoBounds.contains(mouseX, mouseY)) {
            return "滚轮上下翻页，点击滚动条快速跳页";
        }
        return null;
    }

    protected static boolean handleMainPageControlsClick(int mouseX, int mouseY, OverlayMetrics m, int contentStartY) {
        if (!shouldShowMainPageControls()) {
            return false;
        }

        MainPageControlBounds pageControls = getMainPageControlBounds(m);
        int totalPages = GuiInventory.getCurrentTotalPages(m, contentStartY);
        if (pageControls.prevButtonBounds.contains(mouseX, mouseY)) {
            shiftCurrentPage(-1, totalPages);
            return true;
        }
        if (pageControls.nextButtonBounds.contains(mouseX, mouseY)) {
            shiftCurrentPage(1, totalPages);
            return true;
        }
        if (pageControls.pageTrackBounds.contains(mouseX, mouseY)) {
            float ratio = MathHelper.clamp((mouseX - pageControls.pageTrackBounds.x)
                    / (float) Math.max(1, pageControls.pageTrackBounds.width - 1), 0.0f, 1.0f);
            setCurrentPage(Math.round(ratio * Math.max(0, totalPages - 1)), totalPages);
            return true;
        }
        if (pageControls.autoPauseButtonBounds.contains(mouseX, mouseY)) {
            ModConfig.autoPauseOnMenuOpen = !ModConfig.autoPauseOnMenuOpen;
            return true;
        }
        return false;
    }

    protected static boolean shiftCurrentPage(int delta, int totalPages) {
        return setCurrentPage(currentPage + delta, totalPages);
    }

    public static boolean isAnyScrollbarDragging() {
        return isDraggingCategoryScrollbar || isDraggingMerchantListScrollbar || isDraggingOtherFeatureGroupScrollbar
                || isDraggingCategoryDivider;
    }

    public static boolean isAnyDragActive() {
        return isAnyScrollbarDragging() || pressedCustomSequence != null || isDraggingCustomSequenceCard
                || pressedCategoryRow != null || isDraggingCategoryRow || isDraggingMasterStatusHud;
    }

    public static boolean isMasterStatusHudEditMode() {
        return masterStatusHudEditMode;
    }

    public static void updateMasterStatusHudEditorBounds(Rectangle hudBounds, Rectangle exitButtonBounds) {
        masterStatusHudEditorBounds = hudBounds;
        masterStatusHudExitButtonBounds = exitButtonBounds;
    }

    protected static void setMasterStatusHudEditMode(boolean editing) {
        Minecraft mc = Minecraft.getMinecraft();
        masterStatusHudEditMode = editing;
        if (editing) {
            masterStatusHudEditPreviousMouseDetached = ModConfig.isMouseDetached;
            ModConfig.isMouseDetached = true;
            if (mc != null && zszlScriptMod.isGuiVisible && mc.currentScreen == null) {
                mc.displayGuiScreen(new GuiInventoryOverlayScreen());
            }
        }
        if (!editing) {
            isDraggingMasterStatusHud = false;
            masterStatusHudEditorBounds = null;
            masterStatusHudExitButtonBounds = null;
            ModConfig.isMouseDetached = masterStatusHudEditPreviousMouseDetached;
            if (!ModConfig.isMouseDetached && mc != null && mc.currentScreen == null) {
                mc.mouseHelper.grabMouseCursor();
            }
        }
    }

    protected static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    protected static boolean isBlank(String value) {
        return normalizeText(value).isEmpty();
    }

    protected static boolean containsIllegalNameChars(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (ILLEGAL_CATEGORY_NAME_CHARS.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    protected static void showOverlayMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.player != null && message != null && !message.isEmpty()) {
            mc.player.sendMessage(new TextComponentString(message));
        }
    }

    protected static int clampCategoryPanelBaseWidth(int baseWidth) {
        return MathHelper.clamp(baseWidth, CATEGORY_PANEL_MIN_BASE_WIDTH, CATEGORY_PANEL_MAX_BASE_WIDTH);
    }

    protected static String getCategoryRowDisplayLabel(CategoryTreeRow row) {
        return row == null ? "" : (row.isSubCategory() ? row.subCategory : row.category);
    }

    protected static String getCategoryRowStorageKey(CategoryTreeRow row) {
        if (row == null) {
            return "";
        }
        return row.isSubCategory() ? row.category + "::" + row.subCategory : row.category;
    }

    protected static int getStableAccentColor(String key) {
        int hash = normalizeText(key).hashCode();
        int red = 80 + Math.abs(hash & 0x3F);
        int green = 120 + Math.abs((hash >> 8) & 0x5F);
        int blue = 145 + Math.abs((hash >> 16) & 0x5F);
        return 0xFF000000 | (Math.min(255, red) << 16) | (Math.min(255, green) << 8) | Math.min(255, blue);
    }

    protected static int computeAutoCategoryPanelBaseWidth(FontRenderer fontRenderer) {
        int longestText = fontRenderer.getStringWidth("右键空白处新建分类");
        for (CategoryTreeRow row : buildVisibleCategoryTreeRows()) {
            int labelWidth = fontRenderer.getStringWidth(getCategoryRowDisplayLabel(row));
            int reserved = row.isSubCategory() ? 26 : 34;
            longestText = Math.max(longestText, labelWidth + reserved);
        }
        return clampCategoryPanelBaseWidth(longestText + 12);
    }

    protected static String findCategoryIgnoreCase(String categoryName) {
        String normalizedTarget = normalizeText(categoryName);
        for (String category : categories) {
            if (normalizedTarget.equalsIgnoreCase(normalizeText(category))) {
                return category;
            }
        }
        return "";
    }

    protected static boolean validateCategoryNameInput(String value, String originalName) {
        String normalizedValue = normalizeText(value);
        String normalizedOriginal = normalizeText(originalName);
        if (normalizedValue.isEmpty()) {
            showOverlayMessage("§c名称不能为空");
            return false;
        }
        if (containsIllegalNameChars(normalizedValue)) {
            showOverlayMessage("§c名称不能包含以下字符: " + ILLEGAL_CATEGORY_NAME_CHARS);
            return false;
        }
        String existing = findCategoryIgnoreCase(normalizedValue);
        if (!existing.isEmpty() && !normalizedValue.equalsIgnoreCase(normalizedOriginal)) {
            showOverlayMessage("§c已存在同名分类: " + existing);
            return false;
        }
        return true;
    }

    protected static boolean validateSubCategoryNameInput(String category, String value, String originalName) {
        String normalizedValue = normalizeText(value);
        String normalizedOriginal = normalizeText(originalName);
        if (normalizedValue.isEmpty()) {
            showOverlayMessage("§c子分类名称不能为空");
            return false;
        }
        if (containsIllegalNameChars(normalizedValue)) {
            showOverlayMessage("§c子分类名称不能包含以下字符: " + ILLEGAL_CATEGORY_NAME_CHARS);
            return false;
        }
        for (String subCategory : MainUiLayoutManager.getSubCategories(category)) {
            if (normalizedValue.equalsIgnoreCase(normalizeText(subCategory))
                    && !normalizedValue.equalsIgnoreCase(normalizedOriginal)) {
                showOverlayMessage("§c该分类下已存在同名子分类: " + subCategory);
                return false;
            }
        }
        return true;
    }

    protected static String findSubCategoryIgnoreCase(String category, String subCategoryName) {
        String normalizedTarget = normalizeText(subCategoryName);
        for (String subCategory : MainUiLayoutManager.getSubCategories(category)) {
            if (normalizedTarget.equalsIgnoreCase(normalizeText(subCategory))) {
                return subCategory;
            }
        }
        return "";
    }

    protected static int findFirstEnabledMenuItem(List<ContextMenuItem> items) {
        if (items == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).enabled) {
                return i;
            }
        }
        return items.isEmpty() ? -1 : 0;
    }

    protected static int getKeyboardMenuSelection(int depth, List<ContextMenuItem> items) {
        int fallback = findFirstEnabledMenuItem(items);
        if (depth < 0 || items == null || items.isEmpty()) {
            return fallback;
        }
        if (depth >= contextMenuKeyboardSelectionPath.size()) {
            while (contextMenuKeyboardSelectionPath.size() <= depth) {
                contextMenuKeyboardSelectionPath.add(fallback);
            }
            return fallback;
        }
        int selected = contextMenuKeyboardSelectionPath.get(depth);
        if (selected < 0 || selected >= items.size()) {
            contextMenuKeyboardSelectionPath.set(depth, fallback);
            return fallback;
        }
        return selected;
    }

    protected static void setKeyboardMenuSelection(int depth, int selectedIndex) {
        while (contextMenuKeyboardSelectionPath.size() <= depth) {
            contextMenuKeyboardSelectionPath.add(-1);
        }
        contextMenuKeyboardSelectionPath.set(depth, selectedIndex);
        while (contextMenuKeyboardSelectionPath.size() > depth + 1) {
            contextMenuKeyboardSelectionPath.remove(contextMenuKeyboardSelectionPath.size() - 1);
        }
    }

    protected static int moveKeyboardMenuSelection(List<ContextMenuItem> items, int currentIndex, int step) {
        if (items == null || items.isEmpty()) {
            return -1;
        }
        int size = items.size();
        int index = currentIndex < 0 ? findFirstEnabledMenuItem(items) : currentIndex;
        if (index < 0) {
            return -1;
        }
        for (int i = 0; i < size; i++) {
            index = (index + step + size) % size;
            if (items.get(index).enabled) {
                return index;
            }
        }
        return currentIndex;
    }

    protected static void clearSelectedCustomSequences() {
        selectedCustomSequenceNames.clear();
    }

    protected static void pruneSelectedCustomSequences() {
        Iterator<String> iterator = selectedCustomSequenceNames.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            PathSequence sequence = PathSequenceManager.getSequence(name);
            if (sequence == null || !sequence.isCustom()
                    || !normalizeText(sequence.getCategory()).equals(normalizeText(currentCategory))) {
                iterator.remove();
            }
        }
    }

    protected static List<String> getSelectedCustomSequenceNames() {
        pruneSelectedCustomSequences();
        return new ArrayList<>(selectedCustomSequenceNames);
    }

    protected static boolean isCustomSequenceSelected(PathSequence sequence) {
        return sequence != null && selectedCustomSequenceNames.contains(sequence.getName());
    }

    protected static void toggleCustomSequenceSelection(PathSequence sequence) {
        if (sequence == null) {
            return;
        }
        String name = sequence.getName();
        if (selectedCustomSequenceNames.contains(name)) {
            selectedCustomSequenceNames.remove(name);
        } else {
            selectedCustomSequenceNames.add(name);
        }
    }

    protected static boolean isControlDown() {
        return SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL);
    }

    protected static boolean canReorderCategoryRows(CategoryTreeRow source, CategoryTreeRow target) {
        if (source == null || target == null || source == target || source.systemCategory || target.systemCategory) {
            return false;
        }
        if (source.isCustomCategoryRoot() && target.isCustomCategoryRoot()) {
            return true;
        }
        return source.isSubCategory() && target.isSubCategory()
                && normalizeText(source.category).equals(normalizeText(target.category));
    }

    protected static CategoryTreeRow findSortableCategoryRowAt(int mouseX, int mouseY, CategoryTreeRow source) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY) && canReorderCategoryRows(source, row)) {
                return row;
            }
        }
        return null;
    }

    protected static CustomButtonRenderInfo findCustomToolbarButtonAt(int mouseX, int mouseY) {
        for (CustomButtonRenderInfo info : visibleCustomToolbarButtons) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    protected static CustomButtonRenderInfo findCustomSearchScopeButtonAt(int mouseX, int mouseY) {
        for (CustomButtonRenderInfo info : visibleCustomSearchScopeButtons) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    protected static CustomButtonRenderInfo findCustomEmptySectionButtonAt(int mouseX, int mouseY) {
        for (CustomButtonRenderInfo info : visibleCustomEmptySectionButtons) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    protected static void promptCreateCustomSequence(String category, String subCategory) {
        final String normalizedCategory = normalizeText(category);
        final String normalizedSubCategory = normalizeText(subCategory);
        if (normalizedCategory.isEmpty()) {
            return;
        }
        openOverlayTextInput("新建路径序列", value -> {
            String name = normalizeText(value);
            if (name.isEmpty()) {
                showOverlayMessage("§c路径序列名称不能为空");
                return;
            }
            if (PathSequenceManager.hasSequence(name)) {
                showOverlayMessage("§c已存在同名路径序列: " + name);
                return;
            }
            if (!normalizedSubCategory.isEmpty()) {
                MainUiLayoutManager.addSubCategory(normalizedCategory, normalizedSubCategory);
            }
            if (!PathSequenceManager.createEmptyCustomSequence(name, normalizedCategory, normalizedSubCategory)) {
                showOverlayMessage("§c创建路径序列失败");
                return;
            }
            currentCategory = normalizedCategory;
            currentCustomSubCategory = normalizedSubCategory;
            clearSelectedCustomSequences();
            closeOverlay();
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                mc.displayGuiScreen(GuiPathManager.openForSequence(normalizedCategory, name));
            }
        });
    }

    protected static void promptCreateSubCategory(String category) {
        final String normalizedCategory = normalizeText(category);
        if (normalizedCategory.isEmpty()) {
            return;
        }
        openOverlayTextInput("新建子分类", value -> {
            String name = normalizeText(value);
            if (!validateSubCategoryNameInput(normalizedCategory, name, "")) {
                return;
            }
            MainUiLayoutManager.addSubCategory(normalizedCategory, name);
            currentCategory = normalizedCategory;
            currentCustomSubCategory = name;
            currentPage = 0;
            refreshGuiLists();
        });
    }

    protected static void promptBatchSubCategoryUpdate(List<String> sequenceNames) {
        if (sequenceNames == null || sequenceNames.isEmpty()) {
            return;
        }
        openOverlayTextInput("批量设置子分类", currentCustomSubCategory, value -> {
            String newSubCategory = normalizeText(value);
            if (!newSubCategory.isEmpty() && containsIllegalNameChars(newSubCategory)) {
                showOverlayMessage("§c子分类名称不能包含以下字符: " + ILLEGAL_CATEGORY_NAME_CHARS);
                return;
            }
            String resolvedSubCategory = newSubCategory;
            String existingSubCategory = findSubCategoryIgnoreCase(currentCategory, newSubCategory);
            if (!newSubCategory.isEmpty() && existingSubCategory.isEmpty()) {
                MainUiLayoutManager.addSubCategory(currentCategory, newSubCategory);
            } else if (!existingSubCategory.isEmpty()) {
                resolvedSubCategory = existingSubCategory;
            }
            for (String sequenceName : sequenceNames) {
                PathSequenceManager.moveCustomSequenceTo(sequenceName, currentCategory, resolvedSubCategory);
            }
            clearSelectedCustomSequences();
            currentCustomSubCategory = resolvedSubCategory;
            currentPage = 0;
            refreshGuiLists();
        });
    }

    protected static void promptBatchNoteUpdate(List<String> sequenceNames) {
        if (sequenceNames == null || sequenceNames.isEmpty()) {
            return;
        }
        openOverlayTextInput("批量设置备注", "", value -> {
            String note = value == null ? "" : value.trim();
            boolean changed = false;
            List<PathSequence> allSequences = PathSequenceManager.getAllSequences();
            for (PathSequence sequence : allSequences) {
                if (sequence != null && sequence.isCustom() && sequenceNames.contains(sequence.getName())) {
                    sequence.setNote(note);
                    changed = true;
                }
            }
            if (changed) {
                PathSequenceManager.saveAllSequences(allSequences);
                refreshGuiLists();
            }
        });
    }

    protected static boolean applyBatchSequenceChange(List<String> sequenceNames, Consumer<PathSequence> updater) {
        if (sequenceNames == null || sequenceNames.isEmpty() || updater == null) {
            return false;
        }
        Set<String> targets = new HashSet<>(sequenceNames);
        boolean changed = false;
        List<PathSequence> allSequences = PathSequenceManager.getAllSequences();
        for (PathSequence sequence : allSequences) {
            if (sequence != null && sequence.isCustom() && targets.contains(sequence.getName())) {
                updater.accept(sequence);
                changed = true;
            }
        }
        if (changed) {
            PathSequenceManager.saveAllSequences(allSequences);
            refreshGuiLists();
        }
        return changed;
    }

    protected static void moveCustomSequencesToSubCategory(List<String> sequenceNames, String category,
            String subCategory, boolean keepSelection) {
        if (sequenceNames == null || sequenceNames.isEmpty()) {
            return;
        }
        String normalizedCategory = normalizeText(category);
        String normalizedSubCategory = normalizeText(subCategory);
        if (normalizedCategory.isEmpty()) {
            return;
        }
        if (!normalizedSubCategory.isEmpty()) {
            MainUiLayoutManager.addSubCategory(normalizedCategory, normalizedSubCategory);
        }
        for (String sequenceName : sequenceNames) {
            PathSequenceManager.moveCustomSequenceTo(sequenceName, normalizedCategory, normalizedSubCategory);
        }
        if (!keepSelection) {
            clearSelectedCustomSequences();
        }
        currentCategory = normalizedCategory;
        currentCustomSubCategory = normalizedSubCategory;
        currentPage = 0;
        refreshGuiLists();
    }

    protected static List<PathSequence> getMovableSequencesForSubCategory(String category, String targetSubCategory) {
        String normalizedCategory = normalizeText(category);
        String normalizedTargetSubCategory = normalizeText(targetSubCategory);
        List<PathSequence> result = new ArrayList<>();
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (sequence == null || !sequence.isCustom()) {
                continue;
            }
            if (!normalizedCategory.equals(normalizeText(sequence.getCategory()))) {
                continue;
            }
            if (normalizedTargetSubCategory.equalsIgnoreCase(GuiInventory.normalizeSequenceSubCategory(sequence))) {
                continue;
            }
            result.add(sequence);
        }
        result.sort((left, right) -> {
            int compare = GuiInventory.normalizeSequenceSubCategory(left)
                    .compareToIgnoreCase(GuiInventory.normalizeSequenceSubCategory(right));
            return compare != 0 ? compare : left.getName().compareToIgnoreCase(right.getName());
        });
        return result;
    }

    protected static List<ContextMenuItem> buildMoveExistingIntoSectionMenu(String category, String targetSubCategory) {
        String normalizedCategory = normalizeText(category);
        String normalizedTargetSubCategory = normalizeText(targetSubCategory);
        List<ContextMenuItem> items = new ArrayList<>();
        List<String> selectedNames = getSelectedCustomSequenceNames();
        if (!selectedNames.isEmpty()) {
            List<String> movableSelected = new ArrayList<>();
            for (String sequenceName : selectedNames) {
                PathSequence sequence = PathSequenceManager.getSequence(sequenceName);
                if (sequence != null && sequence.isCustom()
                        && normalizedCategory.equals(normalizeText(sequence.getCategory()))
                        && !normalizedTargetSubCategory.equalsIgnoreCase(
                                GuiInventory.normalizeSequenceSubCategory(sequence))) {
                    movableSelected.add(sequenceName);
                }
            }
            if (!movableSelected.isEmpty()) {
                items.add(menuItem("移动已选中的 " + movableSelected.size() + " 个", () -> {
                    moveCustomSequencesToSubCategory(movableSelected, normalizedCategory, normalizedTargetSubCategory,
                            false);
                }));
            }
        }

        Map<String, List<PathSequence>> grouped = new LinkedHashMap<>();
        for (PathSequence sequence : getMovableSequencesForSubCategory(normalizedCategory,
                normalizedTargetSubCategory)) {
            String sourceSubCategory = GuiInventory.normalizeSequenceSubCategory(sequence);
            String groupKey = sourceSubCategory.isEmpty() ? "分类根目录" : sourceSubCategory;
            grouped.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(sequence);
        }

        for (Map.Entry<String, List<PathSequence>> entry : grouped.entrySet()) {
            ContextMenuItem sourceMenu = menuItem(entry.getKey(), null);
            for (PathSequence sequence : entry.getValue()) {
                sourceMenu.child(menuItem(sequence.getName(), () -> {
                    moveCustomSequencesToSubCategory(Collections.singletonList(sequence.getName()), normalizedCategory,
                            normalizedTargetSubCategory, false);
                }));
            }
            items.add(sourceMenu);
        }

        return items;
    }

    protected static boolean isBuiltinScriptCategory(String category) {
        return I18n.format("gui.inventory.category.builtin_script").equals(category);
    }

    protected static boolean isSystemOverlayCategory(String category) {
        return I18n.format("gui.inventory.category.common").equals(category)
                || I18n.format("gui.inventory.category.rsl").equals(category)
                || I18n.format("gui.inventory.category.debug").equals(category) || isBuiltinScriptCategory(category);
    }

    protected static boolean isCustomOverlayCategory(String category) {
        return category != null && categories.contains(category) && !isSystemOverlayCategory(category);
    }

    protected static String getPageKeyForSelection(String category, String subCategory) {
        String normalizedSubCategory = normalizeText(subCategory);
        return normalizedSubCategory.isEmpty() ? category : category + "::" + normalizedSubCategory;
    }

    protected static String getCurrentPageKey() {
        if (isCustomOverlayCategory(currentCategory)) {
            return getPageKeyForSelection(currentCategory, currentCustomSubCategory);
        }
        return currentCategory;
    }

    protected static void syncCurrentCustomCategoryState() {
        if (!isCustomOverlayCategory(currentCategory)) {
            currentCustomSubCategory = "";
            return;
        }

        List<String> subCategories = MainUiLayoutManager.getSubCategories(currentCategory);
        if (!normalizeText(currentCustomSubCategory).isEmpty() && !subCategories.contains(currentCustomSubCategory)) {
            currentCustomSubCategory = "";
        }
    }

    protected static void closeContextMenu() {
        contextMenuVisible = false;
        contextMenuRootItems.clear();
        contextMenuLayers.clear();
        contextMenuOpenPath.clear();
        contextMenuKeyboardSelectionPath.clear();
    }

    protected static ContextMenuItem menuItem(String label, Runnable action) {
        return new ContextMenuItem(label, action);
    }

    protected static void openContextMenu(int mouseX, int mouseY, List<ContextMenuItem> rootItems) {
        closeContextMenu();
        if (rootItems == null || rootItems.isEmpty()) {
            return;
        }
        contextMenuVisible = true;
        contextMenuAnchorX = mouseX;
        contextMenuAnchorY = mouseY;
        contextMenuRootItems.addAll(rootItems);
        contextMenuKeyboardSelectionPath.add(findFirstEnabledMenuItem(rootItems));
    }

    protected static void reopenOverlayScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        zszlScriptMod.isGuiVisible = true;
        mc.displayGuiScreen(new GuiInventoryOverlayScreen());
    }

    protected static void openOverlayTextInput(String title, Consumer<String> callback) {
        openOverlayTextInput(title, "", callback);
    }

    protected static void openOverlayTextInput(String title, String initialText, Consumer<String> callback) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        closeContextMenu();
        zszlScriptMod.isGuiVisible = true;
        mc.displayGuiScreen(new GuiTextInput(new GuiInventoryOverlayScreen(), title,
                initialText == null ? "" : initialText, value -> {
                    if (callback != null) {
                        callback.accept(value);
                    }
                }));
    }

    protected static void openOverlayConfirm(String title, String message, Runnable onConfirm) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        closeContextMenu();
        zszlScriptMod.isGuiVisible = true;
        mc.displayGuiScreen(new GuiInventoryConfirmScreen(new GuiInventoryOverlayScreen(), title, message, onConfirm));
    }

    protected static List<CategoryTreeRow> buildVisibleCategoryTreeRows() {
        List<CategoryTreeRow> rows = new ArrayList<>();
        List<String> customCategories = new ArrayList<>();
        for (String category : categories) {
            if (isSystemOverlayCategory(category)) {
                rows.add(new CategoryTreeRow(category, "", true));
            } else if (isCustomOverlayCategory(category)) {
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

        for (String category : customCategories) {
            rows.add(new CategoryTreeRow(category, "", false));
            if (!MainUiLayoutManager.isCollapsed(category)) {
                for (String subCategory : MainUiLayoutManager.getSubCategories(category)) {
                    rows.add(new CategoryTreeRow(category, subCategory, false));
                }
            }
        }

        return rows;
    }

    protected static CategoryTreeRow findCategoryRowAt(int mouseX, int mouseY) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    protected static SequenceCardRenderInfo findCustomSequenceCardAt(int mouseX, int mouseY) {
        for (SequenceCardRenderInfo card : visibleCustomSequenceCards) {
            if (card.bounds != null && card.bounds.contains(mouseX, mouseY)) {
                return card;
            }
        }
        return null;
    }

    protected static boolean canReorderCustomSequenceCards(PathSequence source, PathSequence target) {
        if (source == null || target == null || source == target) {
            return false;
        }
        if (!source.isCustom() || !target.isCustom()) {
            return false;
        }
        if (!normalizeText(source.getCategory()).equals(normalizeText(target.getCategory()))) {
            return false;
        }
        return GuiInventory.normalizeSequenceSubCategory(source)
                .equalsIgnoreCase(GuiInventory.normalizeSequenceSubCategory(target));
    }

    protected static SequenceCardRenderInfo findSortableCustomSequenceCardAt(int mouseX, int mouseY,
            PathSequence source) {
        for (SequenceCardRenderInfo card : visibleCustomSequenceCards) {
            if (card.bounds != null && card.bounds.contains(mouseX, mouseY)
                    && canReorderCustomSequenceCards(source, card.sequence)) {
                return card;
            }
        }
        return null;
    }

    protected static boolean shouldUseHorizontalCustomSequenceSplit(SequenceCardRenderInfo targetCard) {
        if (targetCard == null || targetCard.bounds == null) {
            return false;
        }
        for (SequenceCardRenderInfo other : visibleCustomSequenceCards) {
            if (other == null || other == targetCard || other.bounds == null) {
                continue;
            }
            if (!canReorderCustomSequenceCards(targetCard.sequence, other.sequence)) {
                continue;
            }
            if (Math.abs(other.bounds.y - targetCard.bounds.y) <= Math.max(2, targetCard.bounds.height / 3)) {
                return true;
            }
        }
        return false;
    }

    protected static SequenceCardRenderInfo findVisibleCustomSequenceCardByName(String sequenceName) {
        String normalizedName = normalizeText(sequenceName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        for (SequenceCardRenderInfo card : visibleCustomSequenceCards) {
            if (card != null && card.sequence != null
                    && normalizedName.equalsIgnoreCase(normalizeText(card.sequence.getName()))) {
                return card;
            }
        }
        return null;
    }

    protected static CustomSequenceDropTarget findDroppableCategoryRowAt(int mouseX, int mouseY) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY) && row.isDroppableTarget()) {
                return new CustomSequenceDropTarget(row.category, row.subCategory, row.bounds);
            }
        }
        return null;
    }

    protected static CustomSequenceDropTarget findCustomSectionDropTargetAt(int mouseX, int mouseY,
            PathSequence source) {
        for (CustomSequenceDropTarget target : visibleCustomSectionDropTargets) {
            if (target.bounds == null || !target.bounds.contains(mouseX, mouseY)) {
                continue;
            }
            if (source != null && target.matches(source.getCategory(), source.getSubCategory())) {
                continue;
            }
            return target;
        }
        return null;
    }

    protected static void normalizeCategoryState() {
        if ("gui.inventory.category.common".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.common");
        } else if ("gui.inventory.category.rsl".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.rsl");
        } else if ("gui.inventory.category.debug".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.debug");
        } else if ("gui.inventory.category.builtin_script".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.builtin_script");
        }

        if ("gui.inventory.category.common".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.common");
        } else if ("gui.inventory.category.rsl".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.rsl");
        } else if ("gui.inventory.category.debug".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.debug");
        } else if ("gui.inventory.category.builtin_script".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.builtin_script");
        }
    }

    protected static List<String> getBuiltinSecondPageGroup() {
        List<String> names = new ArrayList<>();
        for (String key : BUILTIN_SECOND_PAGE_GROUP_KEYS) {
            names.add(I18n.format(key));
        }
        return names;
    }

    public static void refreshGuiLists() {
        MainUiLayoutManager.ensureLoaded();
        categories.clear();
        categoryItems.clear();
        categoryItemNames.clear();
        itemTooltips.clear();

        categories.add(I18n.format("gui.inventory.category.common"));
        if (!isRslFeaturesHidden()) {
            categories.add(I18n.format("gui.inventory.category.rsl"));
        }
        categories.add(I18n.format("gui.inventory.category.debug"));

        List<String> pathCategories = PathSequenceManager.getVisibleCategories();
        pathCategories.remove(I18n.format("gui.inventory.category.builtin_path"));
        if (isRslFeaturesHidden()) {
            pathCategories.remove(I18n.format("path.category.builtin"));
        }

        categories.add(I18n.format("gui.inventory.category.builtin_script"));
        categories.addAll(GuiInventory.filterStandalonePathCategories(pathCategories));

        List<String> availablePrimaryCategories = GuiInventory.getBuiltinRoutePrimaryCategories();
        if (builtinScriptPrimaryCategory != null
                && !availablePrimaryCategories.contains(builtinScriptPrimaryCategory)) {
            builtinScriptPrimaryCategory = null;
            builtinScriptSubCategory = null;
        }
        if (builtinScriptPrimaryCategory != null && builtinScriptSubCategory != null
                && !builtinScriptSubCategory.trim().isEmpty()) {
            List<String> availableSubCategories = GuiInventory
                    .getBuiltinRouteSubCategoriesByPrimary(builtinScriptPrimaryCategory);
            if (!availableSubCategories.contains(builtinScriptSubCategory)) {
                builtinScriptSubCategory = null;
            }
        }

        if (!categories.contains(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.common");
            currentCustomSubCategory = "";
        }

        syncCurrentCustomCategoryState();
        pruneSelectedCustomSequences();

        String currentPageKey = getCurrentPageKey();
        if (CATEGORY_PAGE_MAP.containsKey(currentPageKey)) {
            currentPage = CATEGORY_PAGE_MAP.get(currentPageKey);
        } else {
            currentPage = GuiInventory.getDefaultPageForCategory(currentCategory);
        }

        List<String> setItems = new ArrayList<>();
        List<String> setItemNames = new ArrayList<>();
        setItems.add("autoeat");
        setItemNames.add(I18n.format("gui.inventory.item.autoeat.name"));
        itemTooltips.put("autoeat", I18n.format("gui.inventory.item.autoeat.tooltip"));
        setItems.add("toggle_auto_fishing");
        setItemNames.add(I18n.format("gui.inventory.item.auto_fishing.name"));
        itemTooltips.put("toggle_auto_fishing", I18n.format("gui.inventory.item.auto_fishing.tooltip"));
        setItems.add("toggle_mouse_detach");
        setItemNames.add(I18n.format("gui.inventory.item.mouse_detach.name"));
        itemTooltips.put("toggle_mouse_detach", I18n.format("gui.inventory.item.mouse_detach.tooltip"));
        setItems.add("toggle_fly");
        setItemNames.add(I18n.format("gui.inventory.item.fly.name"));
        itemTooltips.put("toggle_fly", I18n.format("gui.inventory.item.fly.tooltip"));
        setItems.add("followconfig");
        setItemNames.add(I18n.format("gui.inventory.item.autofollow.name"));
        itemTooltips.put("followconfig", I18n.format("gui.inventory.item.autofollow.tooltip"));

        setItems.add("toggle_kill_aura");
        setItemNames.add(I18n.format("gui.inventory.item.kill_aura.name"));
        itemTooltips.put("toggle_kill_aura", I18n.format("gui.inventory.item.kill_aura.tooltip"));
        // setItems.add("arenaconfig");
        // setItemNames.add(I18n.format("gui.inventory.item.arena_config.name"));
        // itemTooltips.put("arenaconfig",
        // I18n.format("gui.inventory.item.arena_config.tooltip"));
        setItems.add("conditional_execution");
        setItemNames.add(I18n.format("gui.inventory.item.conditional_execution.name"));
        itemTooltips.put("conditional_execution", I18n.format("gui.inventory.item.conditional_execution.tooltip"));
        setItems.add("auto_escape");
        setItemNames.add(I18n.format("gui.inventory.item.auto_escape.name"));
        itemTooltips.put("auto_escape", I18n.format("gui.inventory.item.auto_escape.tooltip"));
        setItems.add("keybind_manager");
        setItemNames.add(I18n.format("gui.inventory.item.keybind_manager.name"));
        itemTooltips.put("keybind_manager", I18n.format("gui.inventory.item.keybind_manager.tooltip"));
        setItems.add("profile_manager");
        setItemNames.add(I18n.format("gui.inventory.item.profile_manager.name"));
        itemTooltips.put("profile_manager", I18n.format("gui.inventory.item.profile_manager.tooltip"));
        setItems.add("chat_optimization");
        setItemNames.add(I18n.format("gui.inventory.item.chat_optimization.name"));
        itemTooltips.put("chat_optimization", I18n.format("gui.inventory.item.chat_optimization.tooltip"));

        setItems.add("toggle_auto_pickup");
        setItemNames.add(I18n.format("gui.inventory.item.auto_pickup.name"));
        itemTooltips.put("toggle_auto_pickup", I18n.format("gui.inventory.item.auto_pickup.tooltip"));

        setItems.add("toggle_auto_use_item");
        setItemNames.add(I18n.format("gui.inventory.item.auto_use_item.name"));
        itemTooltips.put("toggle_auto_use_item", I18n.format("gui.inventory.item.auto_use_item.tooltip"));

        setItems.add("block_replacement_config");
        setItemNames.add(I18n.format("gui.inventory.item.block_replacement.name"));
        itemTooltips.put("block_replacement_config", I18n.format("gui.inventory.item.block_replacement.tooltip"));

        setItems.add("warehouse_manager");
        setItemNames.add(I18n.format("gui.inventory.item.warehouse_manager.name"));
        itemTooltips.put("warehouse_manager", I18n.format("gui.inventory.item.warehouse_manager.tooltip"));

        setItems.add("baritone_settings");
        setItemNames.add(I18n.format("gui.inventory.item.baritone_settings.name"));
        itemTooltips.put("baritone_settings", I18n.format("gui.inventory.item.baritone_settings.tooltip"));

        setItems.add("baritone_parkour");
        setItemNames.add(I18n.format("gui.inventory.item.baritone_parkour.name"));
        itemTooltips.put("baritone_parkour", I18n.format("gui.inventory.item.baritone_parkour.tooltip"));

        setItems.add("toggle_server_feature_visibility");
        setItemNames.add(I18n.format("gui.inventory.item.server_feature_visibility.name"));
        itemTooltips.put("toggle_server_feature_visibility",
                I18n.format("gui.inventory.item.server_feature_visibility.tooltip"));

        setItems.add("setloop");
        setItemNames.add(I18n.format("gui.inventory.item.setloop.name"));
        itemTooltips.put("setloop", I18n.format("gui.inventory.item.setloop.tooltip"));

        categoryItems.put(I18n.format("gui.inventory.category.common"), setItems);
        categoryItemNames.put(I18n.format("gui.inventory.category.common"), setItemNames);
        GuiInventory.rebuildCommonSections(setItems);

        List<String> rslItems = new ArrayList<>();
        List<String> rslItemNames = new ArrayList<>();

        rslItems.add("autoskill");
        rslItemNames.add(I18n.format("gui.inventory.item.autoskill.name"));
        itemTooltips.put("autoskill", I18n.format("gui.inventory.item.autoskill.tooltip"));

        rslItems.add("signin_online_rewards");
        rslItemNames.add(I18n.format("gui.inventory.item.signin_online.name"));
        itemTooltips.put("signin_online_rewards", I18n.format("gui.inventory.item.signin_online.tooltip"));

        rslItems.add("toggle_fast_attack");
        rslItemNames.add(I18n.format("gui.inventory.item.fast_attack.name"));
        itemTooltips.put("toggle_fast_attack", I18n.format("gui.inventory.item.fast_attack.tooltip"));

        rslItems.add("toggle_ad_exp_panel");
        rslItemNames.add(I18n.format("gui.inventory.ad_exp_panel.name"));
        itemTooltips.put("toggle_ad_exp_panel", I18n.format("gui.inventory.item.ad_exp_panel.tooltip"));

        rslItems.add("toggle_shulker_rebound_fix");
        rslItemNames.add(I18n.format("gui.inventory.shulker_rebound_fix.name"));
        itemTooltips.put("toggle_shulker_rebound_fix", I18n.format("gui.inventory.item.shulker_rebound_fix.tooltip"));

        rslItems.add("toggle_kill_timer");
        rslItemNames.add(I18n.format("gui.inventory.kill_timer.name"));
        itemTooltips.put("toggle_kill_timer", I18n.format("gui.inventory.item.kill_timer.tooltip"));

        rslItems.add("toggle_death_auto_rejoin");
        rslItemNames.add(I18n.format("gui.inventory.item.death_auto_rejoin.name"));
        itemTooltips.put("toggle_death_auto_rejoin", I18n.format("gui.inventory.item.death_auto_rejoin.tooltip"));

        rslItems.add("quick_exchange_config");
        rslItemNames.add(I18n.format("gui.inventory.item.quick_exchange.name"));
        itemTooltips.put("quick_exchange_config", I18n.format("gui.inventory.item.quick_exchange.tooltip"));

        rslItems.add("toggle_auto_stack_shulker_boxes");
        rslItemNames.add(I18n.format("gui.inventory.item.auto_stack.name"));
        itemTooltips.put("toggle_auto_stack_shulker_boxes", I18n.format("gui.inventory.item.auto_stack.tooltip"));

        rslItems.add("toggle_auto_raid_loot");
        rslItemNames.add(I18n.format("gui.inventory.item.auto_raid.name"));
        itemTooltips.put("toggle_auto_raid_loot", I18n.format("gui.inventory.item.auto_raid.tooltip"));

        if (!isRslFeaturesHidden()) {
            categoryItems.put(I18n.format("gui.inventory.category.rsl"), rslItems);
            categoryItemNames.put(I18n.format("gui.inventory.category.rsl"), rslItemNames);
        }

        List<String> debugItems = new ArrayList<>();
        List<String> debugItemNames = new ArrayList<>();
        debugItems.add("debug_settings");
        debugItemNames.add(I18n.format("gui.inventory.item.debug_settings.name"));
        itemTooltips.put("debug_settings", I18n.format("gui.inventory.item.debug_settings.tooltip"));
        debugItems.add("current_resolution_info");
        debugItemNames.add(I18n.format("gui.inventory.item.resolution_info.name"));
        itemTooltips.put("current_resolution_info", I18n.format("gui.inventory.item.resolution_info.tooltip"));
        debugItems.add("reload_paths");
        debugItemNames.add(I18n.format("gui.inventory.item.reload_paths.name"));
        itemTooltips.put("reload_paths", I18n.format("gui.inventory.item.reload_paths.tooltip"));
        debugItems.add("player_equipment_viewer");
        debugItemNames.add(I18n.format("gui.inventory.item.player_equipment.name"));
        itemTooltips.put("player_equipment_viewer", I18n.format("gui.inventory.item.player_equipment.tooltip"));
        debugItems.add("packet_handler");
        debugItemNames.add(I18n.format("gui.inventory.item.packet_handler.name"));
        itemTooltips.put("packet_handler", I18n.format("gui.inventory.item.packet_handler.tooltip"));
        debugItems.add("gui_inspector_manager");
        debugItemNames.add(I18n.format("gui.inventory.item.gui_inspector_manager.name"));
        itemTooltips.put("gui_inspector_manager", I18n.format("gui.inventory.item.gui_inspector_manager.tooltip"));
        debugItems.add("performance_monitor");
        debugItemNames.add(I18n.format("gui.inventory.item.performance_monitor.name"));
        itemTooltips.put("performance_monitor", I18n.format("gui.inventory.item.performance_monitor.tooltip"));
        debugItems.add("terrain_scanner");
        debugItemNames.add(I18n.format("gui.inventory.item.terrain_scanner.name"));
        itemTooltips.put("terrain_scanner", I18n.format("gui.inventory.item.terrain_scanner.tooltip"));

        debugItems.add("memory_manager");
        debugItemNames.add(I18n.format("gui.inventory.item.memory_manager.name"));
        itemTooltips.put("memory_manager", I18n.format("gui.inventory.item.memory_manager.tooltip"));

        categoryItems.put(I18n.format("gui.inventory.category.debug"), debugItems);
        categoryItemNames.put(I18n.format("gui.inventory.category.debug"), debugItemNames);

        for (String categoryName : categories) {
            if (categoryName.equals(I18n.format("gui.inventory.category.common"))
                    || categoryName.equals(I18n.format("gui.inventory.category.rsl"))
                    || categoryName.equals(I18n.format("gui.inventory.category.debug")))
                continue;

            List<String> pathItems = new ArrayList<>();
            List<String> pathItemNames = new ArrayList<>();

            List<PathSequence> categorySequences = new ArrayList<>();

            if (categoryName.equals(I18n.format("gui.inventory.category.builtin_script"))) {
                if (builtinScriptPrimaryCategory != null && !builtinScriptPrimaryCategory.trim().isEmpty()
                        && builtinScriptSubCategory != null && !builtinScriptSubCategory.trim().isEmpty()) {
                    pathItems.add(CMD_BUILTIN_SUBCAT_BACK);
                    pathItemNames.add(I18n.format("gui.common.back"));
                    itemTooltips.put(CMD_BUILTIN_SUBCAT_BACK, "返回上一级分组");

                    for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
                        if (GuiInventory.isBuiltinRouteSequence(sequence)
                                && builtinScriptPrimaryCategory.equals(GuiInventory.getBuiltinRoutePrimaryCategory(sequence))
                                && builtinScriptSubCategory.equals(GuiInventory.getBuiltinRouteSubCategory(sequence))) {
                            categorySequences.add(sequence);
                        }
                    }
                } else if (builtinScriptPrimaryCategory != null && !builtinScriptPrimaryCategory.trim().isEmpty()) {
                    pathItems.add(CMD_BUILTIN_PRIMARY_BACK);
                    pathItemNames.add(I18n.format("gui.common.back"));
                    itemTooltips.put(CMD_BUILTIN_PRIMARY_BACK, "返回内置脚本主分组");

                    for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
                        if (GuiInventory.isBuiltinRouteSequence(sequence)
                                && builtinScriptPrimaryCategory.equals(GuiInventory.getBuiltinRoutePrimaryCategory(sequence))
                                && GuiInventory.getBuiltinRouteSubCategory(sequence).isEmpty()) {
                            categorySequences.add(sequence);
                        }
                    }

                    for (String subCategory : GuiInventory
                            .getBuiltinRouteSubCategoriesByPrimary(builtinScriptPrimaryCategory)) {
                        String cmd = CMD_BUILTIN_SUBCAT_PREFIX + subCategory;
                        pathItems.add(cmd);
                        pathItemNames.add(subCategory);
                        itemTooltips.put(cmd, "查看子分类: " + subCategory);
                    }
                } else {
                    for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
                        if (GuiInventory.isBuiltinMainScript(sequence)) {
                            categorySequences.add(sequence);
                        }
                    }

                    GuiInventory.reorderBuiltinScriptsForSecondPage(categorySequences);

                    for (String primaryCategory : GuiInventory.getBuiltinRoutePrimaryCategories()) {
                        String cmd = CMD_BUILTIN_PRIMARY_PREFIX + primaryCategory;
                        pathItems.add(cmd);
                        pathItemNames.add(primaryCategory);
                        itemTooltips.put(cmd, "查看分组: " + primaryCategory);
                    }
                }
            } else {
                for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
                    String seqCategory = sequence.getCategory();
                    if (categoryName.equals(seqCategory)) {
                        categorySequences.add(sequence);
                    }
                }
            }

            for (PathSequence sequence : categorySequences) {
                String command = sequence.isCustom() ? "custom_path:" : "path:";
                command += sequence.getName();
                pathItems.add(command);

                String displayName = sequence.getName();
                if (builtinScriptSubCategory != null && !builtinScriptSubCategory.trim().isEmpty()
                        && categoryName.equals(I18n.format("gui.inventory.category.builtin_script"))) {
                    String prefix = builtinScriptSubCategory;
                    if (displayName.startsWith(prefix)) {
                        displayName = displayName.substring(prefix.length());
                        while (displayName.startsWith("-") || displayName.startsWith("_")
                                || displayName.startsWith(" ")) {
                            displayName = displayName.substring(1);
                        }
                    }
                    if (displayName.isEmpty()) {
                        displayName = sequence.getName();
                    }
                }
                pathItemNames.add(displayName);
                String typeName = sequence.isCustom() ? I18n.format("gui.inventory.path_type.custom")
                        : I18n.format("gui.inventory.path_type.builtin");
                String baseTooltip = I18n.format("gui.inventory.path.tooltip", displayName, typeName);
                String note = sequence.getNote();
                if (note != null) {
                    note = note.trim();
                }
                if (note != null && !note.isEmpty()) {
                    itemTooltips.put(command, baseTooltip + "\n§b备注: §r" + note);
                } else {
                    itemTooltips.put(command, baseTooltip);
                }
            }
            categoryItems.put(categoryName, pathItems);
            categoryItemNames.put(categoryName, pathItemNames);
        }

        GuiInventory.clampCurrentPageToCategoryBounds();
    }

    protected static void closeOverlay() {
        Minecraft mc = Minecraft.getMinecraft();
        setMasterStatusHudEditMode(false);
        zszlScriptMod.isGuiVisible = false;
        if (mc.currentScreen instanceof GuiInventoryOverlayScreen) {
            mc.displayGuiScreen(null);
        } else if (mc.currentScreen == null) {
            mc.mouseHelper.grabMouseCursor();
        }
    }

    protected static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    protected static void drawHorizontalLine(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        drawRect(startX, y, endX + 1, y + 1, color);
    }

    protected static void drawVerticalLine(int x, int startY, int endY, int color) {
        if (endY < startY) {
            int i = startY;
            startY = endY;
            endY = i;
        }
        drawRect(x, startY + 1, x + 1, endY, color);
    }

    protected static void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        int resolved = GuiTheme.resolveTextColor(text, color);
        fontRenderer.drawStringWithShadow(text, (float) (x - fontRenderer.getStringWidth(text) / 2), (float) y,
                resolved);
    }

    protected static void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        int resolved = GuiTheme.resolveTextColor(text, color);
        fontRenderer.drawStringWithShadow(text, (float) x, (float) y, resolved);
    }


}

