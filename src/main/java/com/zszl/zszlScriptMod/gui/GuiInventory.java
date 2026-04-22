// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/GuiInventory.java
package com.zszl.zszlScriptMod.gui;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.changelog.GuiChangelog;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.config.GuiAdExpPanelConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoEatConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoFishingConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoEquipManager;
import com.zszl.zszlScriptMod.gui.config.GuiAutoEscapeManager;
import com.zszl.zszlScriptMod.gui.config.GuiAutoFollowManager;
import com.zszl.zszlScriptMod.gui.config.GuiAutoPickupConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoSigninOnlineConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoSkillEditor;
import com.zszl.zszlScriptMod.gui.config.GuiAutoStackingConfig;
import com.zszl.zszlScriptMod.gui.config.GuiAutoUseItemConfig;
import com.zszl.zszlScriptMod.gui.config.GuiBaritoneCommandTable;
import com.zszl.zszlScriptMod.gui.config.GuiBaritoneSettings;
import com.zszl.zszlScriptMod.gui.config.GuiBlockReplacementConfig;
import com.zszl.zszlScriptMod.gui.config.GuiChatOptimization;
import com.zszl.zszlScriptMod.gui.config.GuiConditionalExecutionManager;
import com.zszl.zszlScriptMod.gui.config.GuiDeathAutoRejoinConfig;
import com.zszl.zszlScriptMod.gui.config.GuiDebugConfig;
import com.zszl.zszlScriptMod.gui.config.GuiFastAttackConfig;
import com.zszl.zszlScriptMod.gui.config.GuiFlyConfig;
import com.zszl.zszlScriptMod.gui.config.GuiKeybindManager;
import com.zszl.zszlScriptMod.gui.config.GuiKillAuraConfig;
import com.zszl.zszlScriptMod.gui.config.GuiKillTimerConfig;
import com.zszl.zszlScriptMod.gui.config.GuiLoopCountInput;
import com.zszl.zszlScriptMod.gui.config.GuiProfileManager;
import com.zszl.zszlScriptMod.gui.config.GuiQuickExchangeConfig;
import com.zszl.zszlScriptMod.gui.config.GuiResolutionConfig;
import com.zszl.zszlScriptMod.gui.config.GuiServerFeatureVisibilityConfig;
import com.zszl.zszlScriptMod.gui.config.GuiTerrainScannerManager;
import com.zszl.zszlScriptMod.gui.debug.GuiDebugKeybindManager;
import com.zszl.zszlScriptMod.gui.debug.GuiMemoryManager;
import com.zszl.zszlScriptMod.gui.donate.GuiDonationSupport;
import com.zszl.zszlScriptMod.gui.dungeon.GuiWarehouseManager;
import com.zszl.zszlScriptMod.gui.halloffame.GuiHallOfFame;
import com.zszl.zszlScriptMod.gui.path.GuiCustomPathCreator;
import com.zszl.zszlScriptMod.gui.path.GuiPathManager;
import com.zszl.zszlScriptMod.gui.theme.GuiThemeManager;
import com.zszl.zszlScriptMod.handlers.AdExpPanelHandler;
import com.zszl.zszlScriptMod.handlers.AutoEatHandler;
import com.zszl.zszlScriptMod.handlers.AutoEscapeHandler;
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler;
import com.zszl.zszlScriptMod.handlers.AutoSigninOnlineHandler;
import com.zszl.zszlScriptMod.handlers.AutoSkillHandler;
import com.zszl.zszlScriptMod.handlers.AutoUseItemHandler;
import com.zszl.zszlScriptMod.handlers.BlockReplacementHandler;
import com.zszl.zszlScriptMod.handlers.ConditionalExecutionHandler;
import com.zszl.zszlScriptMod.handlers.DeathAutoRejoinHandler;
import com.zszl.zszlScriptMod.handlers.EmbeddedNavigationHandler;
import com.zszl.zszlScriptMod.handlers.FreecamHandler;
import com.zszl.zszlScriptMod.handlers.FlyHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.KillTimerHandler;
import com.zszl.zszlScriptMod.handlers.ShulkerBoxStackingHandler;
import com.zszl.zszlScriptMod.handlers.ShulkerMiningReboundFixHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.inventory.InventoryViewerManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.FeatureDef;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.GroupDef;
import com.zszl.zszlScriptMod.otherfeatures.gui.block.BlockFeatureGuiFactory;
import com.zszl.zszlScriptMod.otherfeatures.gui.item.ItemFeatureGuiFactory;
import com.zszl.zszlScriptMod.otherfeatures.gui.movement.GuiSpeedConfig;
import com.zszl.zszlScriptMod.otherfeatures.gui.movement.MovementFeatureGuiFactory;
import com.zszl.zszlScriptMod.otherfeatures.gui.misc.MiscFeatureGuiFactory;
import com.zszl.zszlScriptMod.otherfeatures.gui.world.WorldFeatureGuiFactory;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.gui.render.RenderFeatureGuiFactory;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.utils.AdExpListManager;
import com.zszl.zszlScriptMod.utils.CapturingFontRenderer;
import com.zszl.zszlScriptMod.utils.DonationLeaderboardManager;
import com.zszl.zszlScriptMod.utils.EnhancementAttrManager;
import com.zszl.zszlScriptMod.utils.ExternalLinkOpener;
import com.zszl.zszlScriptMod.utils.HallOfFameManager;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.CategoryDef;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.ExchangeDef;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.MerchantDef;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import com.zszl.zszlScriptMod.utils.TitleCompendiumManager;
import com.zszl.zszlScriptMod.utils.UpdateChecker;
import com.zszl.zszlScriptMod.utils.UpdateManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.FontRenderer;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.renderer.RenderHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config.GuiUtils;

public class GuiInventory {

    private static final int BASE_SIDE_BUTTON_COLUMN_WIDTH = 80;
    private static final int BASE_TOTAL_WIDTH = 420;
    private static final int BASE_TOTAL_HEIGHT = 280;
    private static final int BASE_GAP = 10;
    private static final int BASE_PADDING = 5;
    private static final int BASE_CATEGORY_PANEL_WIDTH = 110;
    private static final int BASE_CATEGORY_BUTTON_WIDTH = 96;
    private static final int BASE_CATEGORY_BUTTON_HEIGHT = 22;
    private static final int BASE_ITEM_BUTTON_WIDTH = 84;
    private static final int BASE_ITEM_BUTTON_HEIGHT = 22;
    private static final int BASE_TOP_BUTTON_WIDTH = 60;
    private static final int BASE_TOP_BUTTON_HEIGHT = 16;
    private static final int BASE_CONTENT_X_OFFSET = 122;
    private static final int CATEGORY_PANEL_MIN_BASE_WIDTH = 92;
    private static final int CATEGORY_PANEL_MAX_BASE_WIDTH = 180;
    private static final String UNCATEGORIZED_SECTION_TITLE = "未分类";
    private static final String ILLEGAL_CATEGORY_NAME_CHARS = "\\/:*?\"<>|";
    private static final long RECENT_OPEN_HIGHLIGHT_WINDOW_MS = 10L * 60L * 1000L;
    private static final String SEARCH_SCOPE_CURRENT_SUBCATEGORY = "current_subcategory";
    private static final String SEARCH_SCOPE_CURRENT_CATEGORY = "current_category";
    private static final String SEARCH_SCOPE_ALL_CATEGORIES = "all_categories";

    private static class OverlayMetrics {
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

    private static int scaleUi(int base, float scale) {
        return Math.max(1, Math.round(base * scale));
    }

    private static float computeUiScale(int screenWidth, int screenHeight) {
        float sx = screenWidth / 460.0f;
        float sy = screenHeight / 300.0f;
        float s = Math.min(1.0f, Math.min(sx, sy));
        return MathHelper.clamp(s, 0.72f, 1.0f);
    }

    private static String buildOverlayTitle() {
        if (merchantScreenActive) {
            return I18n.format("gui.inventory.merchant.title");
        }
        if (otherFeaturesScreenActive) {
            return I18n.format("gui.inventory.other_features.title");
        }
        return "";
    }

    private static List<String> buildOverlayHeaderLines() {
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

    private static String formatLoopTargetForHeader() {
        if (loopCount < 0) {
            return "∞";
        }
        return String.valueOf(Math.max(0, loopCount));
    }

    private static OverlayMetrics computeOverlayMetrics(int screenWidth, int screenHeight, FontRenderer fontRenderer,
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
            if (isCustomCategorySelection() && isCustomSearchExpanded()) {
                m.topBarHeight = Math.max(m.topBarHeight, m.topButtonHeight * 2 + scaleUi(10, m.scale));
            }
        } else {
            int pathManagerButtonX = m.x + m.totalWidth - m.pathManagerButtonWidth - m.padding;
            int stopButtonX = pathManagerButtonX - m.stopButtonWidth - m.padding;
            int titleAreaStartX = m.x + scaleUi(8, m.scale);
            int titleAreaEndX = stopButtonX - scaleUi(8, m.scale)
                    - getCustomSearchHeaderReservedWidth(fontRenderer, m.scale);
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

    private static OverlayMetrics computeOverlayMetrics(int screenWidth, int screenHeight,
            net.minecraft.client.gui.Font fontRenderer, String title) {
        return computeOverlayMetrics(screenWidth, screenHeight, wrapFont(fontRenderer), title);
    }

    private static final List<String> BUILTIN_SECOND_PAGE_GROUP_KEYS = Arrays.asList(
            "gui.inventory.builtin.evac.helipad", "gui.inventory.builtin.evac.desert_palace",
            "gui.inventory.builtin.evac.transfer_station", "gui.inventory.builtin.evac.snow_inn",
            "gui.inventory.builtin.evac.watchtower", "gui.inventory.builtin.evac.shore_house",
            "gui.inventory.builtin.evac.cliff_house", "gui.inventory.builtin.evac.monitor_station",
            "gui.inventory.builtin.evac.rejoin");

    private static int colorChangeTicker = 0;
    private static final List<ChatFormatting> RAINBOW_COLORS = Arrays.asList(ChatFormatting.RED, ChatFormatting.GOLD,
            ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE);

    private static String sLastCategory = "gui.inventory.category.builtin_script";
    private static int sLastPage = 0;
    private static final Map<String, Integer> CATEGORY_PAGE_MAP = new HashMap<>();
    private static int currentPage = sLastPage;
    private static String currentCategory = sLastCategory;
    private static List<String> categories = new ArrayList<>();
    private static final Map<String, List<String>> categoryItems = new HashMap<>();
    private static final Map<String, List<String>> categoryItemNames = new HashMap<>();
    private static final Map<String, String> itemTooltips = new HashMap<>();
    private static final int COMMON_ROWS_PER_PAGE = 6;

    private static class GroupedItemSection {
        final String key;
        final String title;
        final List<String> commands;

        GroupedItemSection(String key, String title, List<String> commands) {
            this.key = key;
            this.title = title;
            this.commands = new ArrayList<>(commands);
        }
    }

    private static class CommonContentRow {
        final boolean header;
        final String sectionKey;
        final String title;
        final boolean expanded;
        final List<String> commands;

        private CommonContentRow(boolean header, String sectionKey, String title, boolean expanded,
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

    private static class CategoryTreeRow {
        final String category;
        final String subCategory;
        final boolean systemCategory;
        Rectangle bounds;

        private CategoryTreeRow(String category, String subCategory, boolean systemCategory) {
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

    private static class SequenceCardRenderInfo {
        final PathSequence sequence;
        final Rectangle bounds;
        final String displayName;
        final String secondaryText;
        final String tooltip;

        private SequenceCardRenderInfo(PathSequence sequence, Rectangle bounds, String displayName,
                String secondaryText, String tooltip) {
            this.sequence = sequence;
            this.bounds = bounds;
            this.displayName = displayName;
            this.secondaryText = secondaryText;
            this.tooltip = tooltip;
        }
    }

    private static class CustomSectionRenderInfo {
        final String key;
        final String title;
        final String subCategory;
        final Rectangle bounds;

        private CustomSectionRenderInfo(String key, String title, String subCategory, Rectangle bounds) {
            this.key = key;
            this.title = title;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.bounds = bounds;
        }
    }

    private static class CustomSequenceDropTarget {
        final String category;
        final String subCategory;
        final Rectangle bounds;

        private CustomSequenceDropTarget(String category, String subCategory, Rectangle bounds) {
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

    private static class CustomButtonRenderInfo {
        final String action;
        final String category;
        final String subCategory;
        final Rectangle bounds;

        private CustomButtonRenderInfo(String action, String category, String subCategory, Rectangle bounds) {
            this.action = action;
            this.category = category == null ? "" : category;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.bounds = bounds;
        }
    }

    private static class CustomSectionModel {
        final String key;
        final String category;
        final String title;
        final String subCategory;
        final List<PathSequence> sequences;
        final String statsLabel;

        private CustomSectionModel(String key, String category, String title, String subCategory,
                List<PathSequence> sequences) {
            this.key = key;
            this.category = category == null ? "" : category;
            this.title = title;
            this.subCategory = subCategory == null ? "" : subCategory;
            this.sequences = sequences == null ? Collections.emptyList() : new ArrayList<>(sequences);
            this.statsLabel = GuiInventory.buildCustomSectionStatsLabel(this.sequences);
        }
    }

    private static class CustomSectionChunk {
        final CustomSectionModel model;
        final List<PathSequence> pageSequences;
        final boolean continuation;

        private CustomSectionChunk(CustomSectionModel model, List<PathSequence> pageSequences, boolean continuation) {
            this.model = model;
            this.pageSequences = pageSequences == null ? Collections.emptyList() : new ArrayList<>(pageSequences);
            this.continuation = continuation;
        }
    }

    private static class CustomPageLayout {
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

        private CustomPageLayout(CustomGridMetrics grid, List<CustomSectionChunk> sections, int totalPages,
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

    private static class MainPageControlBounds {
        final Rectangle containerBounds;
        final Rectangle prevButtonBounds;
        final Rectangle nextButtonBounds;
        final Rectangle pageInfoBounds;
        final Rectangle pageTrackBounds;
        final Rectangle autoPauseButtonBounds;

        private MainPageControlBounds(Rectangle containerBounds, Rectangle prevButtonBounds,
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

    private static class ContextMenuItem {
        final String label;
        final Runnable action;
        final List<ContextMenuItem> children = new ArrayList<>();
        boolean enabled = true;
        boolean selected = false;

        private ContextMenuItem(String label, Runnable action) {
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

    private static class ContextMenuLayer {
        final List<ContextMenuItem> items;
        final List<Rectangle> itemBounds = new ArrayList<>();
        Rectangle bounds;
        int x;
        int y;
        int width;

        private ContextMenuLayer(List<ContextMenuItem> items) {
            this.items = items;
        }
    }

    private static final List<GroupedItemSection> commonItemSections = new ArrayList<>();
    private static final Map<String, Boolean> commonSectionExpanded = new HashMap<>();
    private static final List<CategoryTreeRow> visibleCategoryRows = new ArrayList<>();
    private static final List<SequenceCardRenderInfo> visibleCustomSequenceCards = new ArrayList<>();
    private static final List<CustomSectionRenderInfo> visibleCustomSectionHeaders = new ArrayList<>();
    private static final List<CustomSequenceDropTarget> visibleCustomSectionDropTargets = new ArrayList<>();
    private static final List<CustomButtonRenderInfo> visibleCustomSearchScopeButtons = new ArrayList<>();
    private static final List<CustomButtonRenderInfo> visibleCustomToolbarButtons = new ArrayList<>();
    private static final List<CustomButtonRenderInfo> visibleCustomEmptySectionButtons = new ArrayList<>();
    private static final Map<String, Boolean> customSectionExpanded = new HashMap<>();
    private static final String CMD_BUILTIN_PRIMARY_PREFIX = "builtin_primary:";
    private static final String CMD_BUILTIN_PRIMARY_BACK = "builtin_primary_back";
    private static final String CMD_BUILTIN_SUBCAT_PREFIX = "builtin_subcat:";
    private static final String CMD_BUILTIN_SUBCAT_BACK = "builtin_subcat_back";
    private static String builtinScriptPrimaryCategory = null;
    private static String builtinScriptSubCategory = null;
    private static String currentCustomSubCategory = "";

    public static int loopCount = 1;
    public static int loopCounter = 0;
    public static boolean isLooping = false;

    public static boolean lockGameInteraction = false;

    private static boolean isDebugRecordingMenuVisible = false;
    private static int debugCategoryRightClickCounter = 0;
    private static long lastDebugCategoryRightClickTime = 0;

    private static int categoryScrollOffset = 0;
    private static int maxCategoryScroll = 0;
    public static boolean isDraggingCategoryScrollbar = false;
    private static int categoryScrollClickY = 0;
    private static int initialCategoryScrollOffset = 0;
    private static boolean isDraggingCategoryRow = false;
    private static CategoryTreeRow pressedCategoryRow = null;
    private static Rectangle pressedCategoryRowRect = null;
    private static int pressedCategoryRowMouseX = 0;
    private static int pressedCategoryRowMouseY = 0;
    private static int draggingCategoryRowMouseX = 0;
    private static int draggingCategoryRowMouseY = 0;
    private static CategoryTreeRow currentCategorySortDropTarget = null;
    private static boolean currentCategorySortDropAfter = false;
    private static boolean isDraggingCustomSequenceCard = false;
    private static PathSequence pressedCustomSequence = null;
    private static Rectangle pressedCustomSequenceRect = null;
    private static int pressedCustomSequenceMouseX = 0;
    private static int pressedCustomSequenceMouseY = 0;
    private static int draggingCustomSequenceMouseX = 0;
    private static int draggingCustomSequenceMouseY = 0;
    private static CustomSequenceDropTarget currentSequenceDropTarget = null;
    private static String currentCustomSequenceSortTargetName = "";
    private static boolean currentCustomSequenceSortAfter = false;
    private static boolean contextMenuVisible = false;
    private static int contextMenuAnchorX = 0;
    private static int contextMenuAnchorY = 0;
    private static final List<ContextMenuItem> contextMenuRootItems = new ArrayList<>();
    private static final List<ContextMenuLayer> contextMenuLayers = new ArrayList<>();
    private static final List<Integer> contextMenuOpenPath = new ArrayList<>();
    private static final List<Integer> contextMenuKeyboardSelectionPath = new ArrayList<>();
    private static GuiTextField customSequenceSearchField;
    private static String customSequenceSearchQuery = "";
    private static String customSequenceSearchScope = SEARCH_SCOPE_CURRENT_CATEGORY;
    private static boolean customSequenceSearchExpanded = false;
    private static boolean customSequenceSearchFocusPending = false;
    private static final Set<String> selectedCustomSequenceNames = new LinkedHashSet<>();
    private static Rectangle customSearchClearButtonBounds;
    private static Rectangle customSearchToggleButtonBounds;
    private static boolean isDraggingCategoryDivider = false;
    private static int categoryDividerMouseOffsetX = 0;
    private static long customSequencePageTurnLockUntil = 0L;
    private static Rectangle categoryDividerBounds;
    private static Rectangle versionClickArea;
    private static Rectangle authorClickArea;

    // --- 新增：左侧功能按钮列表 ---
    private static final List<GuiButton> sideButtons = new ArrayList<>();
    private static final int BTN_ID_THEME_CONFIG = 1000;
    private static final int BTN_ID_UPDATE = 1001;
    private static final int BTN_ID_HALL_OF_FAME = 1002;
    private static final int BTN_ID_TITLE_COMPENDIUM = 1003;
    private static final int BTN_ID_ENHANCEMENT_ATTR = 1004;
    private static final int BTN_ID_AD_EXP_LIST = 1005;
    private static final int BTN_ID_MERCHANT = 1007;
    private static final int BTN_ID_DONATE = 1006;
    private static final int BTN_ID_PERFORMANCE_MONITOR = 1008;
    // --- 新增结束 ---

    private static boolean isRslFeaturesHidden() {
        return ServerFeatureVisibilityManager.shouldHideRslFeatures()
                || PathSequenceManager.isCategoryHidden(I18n.format("path.category.builtin"));
    }

    private static boolean merchantScreenActive = false;
    private static int merchantScreenPage = 0;
    private static int selectedMerchantIndex = 0;
    private static int selectedMerchantCategoryIndex = -1;
    private static int merchantCategoryScrollOffset = 0;
    private static int merchantListScrollOffset = 0;
    private static int maxMerchantListScroll = 0;
    private static boolean isDraggingMerchantListScrollbar = false;
    private static boolean otherFeaturesScreenActive = false;
    private static int otherFeatureScreenPage = 0;
    private static int selectedOtherFeatureGroupIndex = -1;
    private static int otherFeatureGroupScrollOffset = 0;
    private static int maxOtherFeatureGroupScroll = 0;
    private static boolean isDraggingOtherFeatureGroupScrollbar = false;
    private static boolean masterStatusHudEditMode = false;
    private static boolean isDraggingMasterStatusHud = false;
    private static int masterStatusHudDragOffsetX = 0;
    private static int masterStatusHudDragOffsetY = 0;
    private static Rectangle masterStatusHudEditorBounds = null;
    private static Rectangle masterStatusHudExitButtonBounds = null;
    private static boolean masterStatusHudEditPreviousMouseDetached = false;

    private static final class OtherFeatureCardLayout {
        private final FeatureDef feature;
        private final Rectangle bounds;

        private OtherFeatureCardLayout(FeatureDef feature, Rectangle bounds) {
            this.feature = feature;
            this.bounds = bounds;
        }
    }

    private static final class OtherFeaturePageControlBounds {
        private final Rectangle containerBounds;
        private final Rectangle prevButtonBounds;
        private final Rectangle nextButtonBounds;
        private final Rectangle pageInfoBounds;

        private OtherFeaturePageControlBounds(Rectangle containerBounds, Rectangle prevButtonBounds,
                Rectangle nextButtonBounds, Rectangle pageInfoBounds) {
            this.containerBounds = containerBounds;
            this.prevButtonBounds = prevButtonBounds;
            this.nextButtonBounds = nextButtonBounds;
            this.pageInfoBounds = pageInfoBounds;
        }
    }

    private static final class OtherFeaturePageLayout {
        private final List<OtherFeatureCardLayout> cards;
        private final Rectangle contentBounds;
        private final int currentPage;
        private final int totalPages;
        private final OtherFeaturePageControlBounds pageControls;

        private OtherFeaturePageLayout(List<OtherFeatureCardLayout> cards, Rectangle contentBounds, int currentPage,
                int totalPages, OtherFeaturePageControlBounds pageControls) {
            this.cards = cards == null ? Collections.emptyList() : cards;
            this.contentBounds = contentBounds;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.pageControls = pageControls;
        }
    }

    public static void onOpen() {
        merchantScreenActive = false;
        otherFeatureScreenPage = 0;
        isDraggingMerchantListScrollbar = false;
        maxOtherFeatureGroupScroll = 0;
        isDraggingOtherFeatureGroupScrollbar = false;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }

        onOpen();
        zszlScriptMod.isGuiVisible = true;
        mc.setScreen(new GuiInventoryOverlayScreen());
    }

    public static void toggleOverlayScreen() {
        if (zszlScriptMod.isGuiVisible || Minecraft.getInstance().screen instanceof GuiInventoryOverlayScreen) {
            closeOverlay();
            return;
        }
        openOverlayScreen();
    }

    private static void rebuildSideButtons() {
        sideButtons.clear();
        // 始终显示：主题配置、更新脚本、打赏
        sideButtons.add(new ThemedButton(BTN_ID_THEME_CONFIG, 0, 0, 70, 20, I18n.format("gui.inventory.theme_config")));
        sideButtons.add(new ThemedButton(BTN_ID_UPDATE, 0, 0, 70, 20, I18n.format("gui.inventory.update_script")));
        sideButtons.add(new ThemedButton(BTN_ID_DONATE, 0, 0, 70, 20, I18n.format("gui.inventory.donate")));
    }

    private static void updateButtonPositions(int screenWidth, int screenHeight) {
        if (sideButtons.isEmpty()) {
            return;
        }

        FontRenderer fontRenderer = wrapFont(Minecraft.getInstance().font);
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

    private static OverlayMetrics getCurrentOverlayMetrics(int screenWidth, int screenHeight) {
        return computeOverlayMetrics(screenWidth, screenHeight, Minecraft.getInstance().font,
                buildOverlayTitle());
    }

    private static int scaleRawMouseX(int rawMouseX, int screenWidth) {
        return rawMouseX * screenWidth / Minecraft.getInstance().getWindow().getScreenWidth();
    }

    private static int scaleRawMouseY(int rawMouseY, int screenHeight) {
        return screenHeight - rawMouseY * screenHeight / Minecraft.getInstance().getWindow().getScreenHeight() - 1;
    }

    private static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean shouldShowMainPageControls() {
        return !merchantScreenActive && !otherFeaturesScreenActive
                && !(isDebugRecordingMenuVisible && I18n.format("gui.inventory.category.debug").equals(currentCategory));
    }

    private static Rectangle getMainRightPanelBounds(OverlayMetrics m) {
        int x = m.contentPanelX;
        int y = m.contentStartY;
        int width = Math.max(0, m.contentPanelRight - m.contentPanelX);
        int bottom = m.y + m.totalHeight - m.padding;
        return new Rectangle(x, y, width, Math.max(0, bottom - y));
    }

    private static MainPageControlBounds getMainPageControlBounds(OverlayMetrics m) {
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

    private static Rectangle getMainPageTrackThumbBounds(Rectangle pageTrackBounds, int page, int totalPages) {
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

    private static boolean setCurrentPage(int newPage, int totalPages) {
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

    private static String drawMainPageControls(OverlayMetrics m, int contentStartY, int mouseX, int mouseY,
            FontRenderer fontRenderer) {
        if (!shouldShowMainPageControls()) {
            return null;
        }

        MainPageControlBounds pageControls = getMainPageControlBounds(m);
        setCurrentPage(currentPage, getCurrentTotalPages(m, contentStartY));
        int totalPages = getCurrentTotalPages(m, contentStartY);
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
        drawCenteredString(fontRenderer, pageInfo, pageControls.pageInfoBounds.x + pageControls.pageInfoBounds.width / 2,
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

    private static boolean handleMainPageControlsClick(int mouseX, int mouseY, OverlayMetrics m, int contentStartY) {
        if (!shouldShowMainPageControls()) {
            return false;
        }

        MainPageControlBounds pageControls = getMainPageControlBounds(m);
        int totalPages = getCurrentTotalPages(m, contentStartY);
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

    private static boolean shiftCurrentPage(int delta, int totalPages) {
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

    private static void setMasterStatusHudEditMode(boolean editing) {
        Minecraft mc = Minecraft.getInstance();
        masterStatusHudEditMode = editing;
        if (editing) {
            masterStatusHudEditPreviousMouseDetached = ModConfig.isMouseDetached;
            ModConfig.isMouseDetached = true;
            if (mc != null && zszlScriptMod.isGuiVisible && mc.screen == null) {
                mc.setScreen(new GuiInventoryOverlayScreen());
            }
        }
        if (!editing) {
            isDraggingMasterStatusHud = false;
            masterStatusHudEditorBounds = null;
            masterStatusHudExitButtonBounds = null;
            ModConfig.isMouseDetached = masterStatusHudEditPreviousMouseDetached;
            if (!ModConfig.isMouseDetached && mc != null && mc.screen == null) {
                mc.mouseHandler.grabMouse();
            }
        }
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return normalizeText(value).isEmpty();
    }

    private static boolean containsIllegalNameChars(String value) {
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

    private static void showOverlayMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && message != null && !message.isEmpty()) {
            mc.player.sendSystemMessage(new TextComponentString(message));
        }
    }

    private static int clampCategoryPanelBaseWidth(int baseWidth) {
        return MathHelper.clamp(baseWidth, CATEGORY_PANEL_MIN_BASE_WIDTH, CATEGORY_PANEL_MAX_BASE_WIDTH);
    }

    private static String getCategoryRowDisplayLabel(CategoryTreeRow row) {
        return row == null ? "" : (row.isSubCategory() ? row.subCategory : row.category);
    }

    private static String getCategoryRowStorageKey(CategoryTreeRow row) {
        if (row == null) {
            return "";
        }
        return row.isSubCategory() ? row.category + "::" + row.subCategory : row.category;
    }

    private static int getStableAccentColor(String key) {
        int hash = normalizeText(key).hashCode();
        int red = 80 + Math.abs(hash & 0x3F);
        int green = 120 + Math.abs((hash >> 8) & 0x5F);
        int blue = 145 + Math.abs((hash >> 16) & 0x5F);
        return 0xFF000000 | (Math.min(255, red) << 16) | (Math.min(255, green) << 8) | Math.min(255, blue);
    }

    private static int computeAutoCategoryPanelBaseWidth(FontRenderer fontRenderer) {
        int longestText = fontRenderer.getStringWidth("右键空白处新建分类");
        for (CategoryTreeRow row : buildVisibleCategoryTreeRows()) {
            int labelWidth = fontRenderer.getStringWidth(getCategoryRowDisplayLabel(row));
            int reserved = row.isSubCategory() ? 26 : 34;
            longestText = Math.max(longestText, labelWidth + reserved);
        }
        return clampCategoryPanelBaseWidth(longestText + 12);
    }

    private static String findCategoryIgnoreCase(String categoryName) {
        String normalizedTarget = normalizeText(categoryName);
        for (String category : categories) {
            if (normalizedTarget.equalsIgnoreCase(normalizeText(category))) {
                return category;
            }
        }
        return "";
    }

    private static boolean validateCategoryNameInput(String value, String originalName) {
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

    private static boolean validateSubCategoryNameInput(String category, String value, String originalName) {
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

    private static String findSubCategoryIgnoreCase(String category, String subCategoryName) {
        String normalizedTarget = normalizeText(subCategoryName);
        for (String subCategory : MainUiLayoutManager.getSubCategories(category)) {
            if (normalizedTarget.equalsIgnoreCase(normalizeText(subCategory))) {
                return subCategory;
            }
        }
        return "";
    }

    private static int findFirstEnabledMenuItem(List<ContextMenuItem> items) {
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

    private static int getKeyboardMenuSelection(int depth, List<ContextMenuItem> items) {
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

    private static void setKeyboardMenuSelection(int depth, int selectedIndex) {
        while (contextMenuKeyboardSelectionPath.size() <= depth) {
            contextMenuKeyboardSelectionPath.add(-1);
        }
        contextMenuKeyboardSelectionPath.set(depth, selectedIndex);
        while (contextMenuKeyboardSelectionPath.size() > depth + 1) {
            contextMenuKeyboardSelectionPath.remove(contextMenuKeyboardSelectionPath.size() - 1);
        }
    }

    private static int moveKeyboardMenuSelection(List<ContextMenuItem> items, int currentIndex, int step) {
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

    private static void clearSelectedCustomSequences() {
        selectedCustomSequenceNames.clear();
    }

    private static void pruneSelectedCustomSequences() {
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

    private static List<String> getSelectedCustomSequenceNames() {
        pruneSelectedCustomSequences();
        return new ArrayList<>(selectedCustomSequenceNames);
    }

    private static boolean isCustomSequenceSelected(PathSequence sequence) {
        return sequence != null && selectedCustomSequenceNames.contains(sequence.getName());
    }

    private static void toggleCustomSequenceSelection(PathSequence sequence) {
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

    private static boolean isControlDown() {
        return SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL);
    }

    private static boolean canReorderCategoryRows(CategoryTreeRow source, CategoryTreeRow target) {
        if (source == null || target == null || source == target || source.systemCategory || target.systemCategory) {
            return false;
        }
        if (source.isCustomCategoryRoot() && target.isCustomCategoryRoot()) {
            return true;
        }
        return source.isSubCategory() && target.isSubCategory()
                && normalizeText(source.category).equals(normalizeText(target.category));
    }

    private static CategoryTreeRow findSortableCategoryRowAt(int mouseX, int mouseY, CategoryTreeRow source) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY) && canReorderCategoryRows(source, row)) {
                return row;
            }
        }
        return null;
    }

    private static CustomButtonRenderInfo findCustomToolbarButtonAt(int mouseX, int mouseY) {
        for (CustomButtonRenderInfo info : visibleCustomToolbarButtons) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    private static CustomButtonRenderInfo findCustomSearchScopeButtonAt(int mouseX, int mouseY) {
        for (CustomButtonRenderInfo info : visibleCustomSearchScopeButtons) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    private static CustomButtonRenderInfo findCustomEmptySectionButtonAt(int mouseX, int mouseY) {
        for (CustomButtonRenderInfo info : visibleCustomEmptySectionButtons) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    private static void promptCreateCustomSequence(String category, String subCategory) {
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
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.setScreen(GuiPathManager.openForSequence(normalizedCategory, name));
            }
        });
    }

    private static void promptCreateSubCategory(String category) {
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

    private static void promptBatchSubCategoryUpdate(List<String> sequenceNames) {
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

    private static void promptBatchNoteUpdate(List<String> sequenceNames) {
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

    private static boolean applyBatchSequenceChange(List<String> sequenceNames, Consumer<PathSequence> updater) {
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

    private static void moveCustomSequencesToSubCategory(List<String> sequenceNames, String category,
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

    private static List<PathSequence> getMovableSequencesForSubCategory(String category, String targetSubCategory) {
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
            if (normalizedTargetSubCategory.equalsIgnoreCase(normalizeSequenceSubCategory(sequence))) {
                continue;
            }
            result.add(sequence);
        }
        result.sort((left, right) -> {
            int compare = normalizeSequenceSubCategory(left).compareToIgnoreCase(normalizeSequenceSubCategory(right));
            return compare != 0 ? compare : left.getName().compareToIgnoreCase(right.getName());
        });
        return result;
    }

    private static List<ContextMenuItem> buildMoveExistingIntoSectionMenu(String category, String targetSubCategory) {
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
                        && !normalizedTargetSubCategory.equalsIgnoreCase(normalizeSequenceSubCategory(sequence))) {
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
            String sourceSubCategory = normalizeSequenceSubCategory(sequence);
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

    private static boolean isBuiltinScriptCategory(String category) {
        return I18n.format("gui.inventory.category.builtin_script").equals(category);
    }

    private static boolean isSystemOverlayCategory(String category) {
        return I18n.format("gui.inventory.category.common").equals(category)
                || I18n.format("gui.inventory.category.debug").equals(category) || isBuiltinScriptCategory(category);
    }

    private static boolean isCustomOverlayCategory(String category) {
        return category != null && categories.contains(category) && !isSystemOverlayCategory(category);
    }

    private static String getPageKeyForSelection(String category, String subCategory) {
        String normalizedSubCategory = normalizeText(subCategory);
        return normalizedSubCategory.isEmpty() ? category : category + "::" + normalizedSubCategory;
    }

    private static String getCurrentPageKey() {
        if (isCustomOverlayCategory(currentCategory)) {
            return getPageKeyForSelection(currentCategory, currentCustomSubCategory);
        }
        return currentCategory;
    }

    private static void syncCurrentCustomCategoryState() {
        if (!isCustomOverlayCategory(currentCategory)) {
            currentCustomSubCategory = "";
            return;
        }

        List<String> subCategories = MainUiLayoutManager.getSubCategories(currentCategory);
        if (!normalizeText(currentCustomSubCategory).isEmpty() && !subCategories.contains(currentCustomSubCategory)) {
            currentCustomSubCategory = "";
        }
    }

    private static void closeContextMenu() {
        contextMenuVisible = false;
        contextMenuRootItems.clear();
        contextMenuLayers.clear();
        contextMenuOpenPath.clear();
        contextMenuKeyboardSelectionPath.clear();
    }

    private static ContextMenuItem menuItem(String label, Runnable action) {
        return new ContextMenuItem(label, action);
    }

    private static void openContextMenu(int mouseX, int mouseY, List<ContextMenuItem> rootItems) {
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

    private static void reopenOverlayScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        zszlScriptMod.isGuiVisible = true;
        mc.setScreen(new GuiInventoryOverlayScreen());
    }

    private static void openOverlayTextInput(String title, Consumer<String> callback) {
        openOverlayTextInput(title, "", callback);
    }

    private static void openOverlayTextInput(String title, String initialText, Consumer<String> callback) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        closeContextMenu();
        zszlScriptMod.isGuiVisible = true;
        mc.setScreen(new GuiTextInput(new GuiInventoryOverlayScreen(), title,
                initialText == null ? "" : initialText, value -> {
                    if (callback != null) {
                        callback.accept(value);
                    }
                }));
    }

    private static void openOverlayConfirm(String title, String message, Runnable onConfirm) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        closeContextMenu();
        zszlScriptMod.isGuiVisible = true;
        mc.setScreen(new GuiInventoryConfirmScreen(new GuiInventoryOverlayScreen(), title, message, onConfirm));
    }

    private static List<CategoryTreeRow> buildVisibleCategoryTreeRows() {
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

    private static CategoryTreeRow findCategoryRowAt(int mouseX, int mouseY) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    private static SequenceCardRenderInfo findCustomSequenceCardAt(int mouseX, int mouseY) {
        for (SequenceCardRenderInfo card : visibleCustomSequenceCards) {
            if (card.bounds != null && card.bounds.contains(mouseX, mouseY)) {
                return card;
            }
        }
        return null;
    }

    private static boolean canReorderCustomSequenceCards(PathSequence source, PathSequence target) {
        if (source == null || target == null || source == target) {
            return false;
        }
        if (!source.isCustom() || !target.isCustom()) {
            return false;
        }
        if (!normalizeText(source.getCategory()).equals(normalizeText(target.getCategory()))) {
            return false;
        }
        return normalizeSequenceSubCategory(source).equalsIgnoreCase(normalizeSequenceSubCategory(target));
    }

    private static SequenceCardRenderInfo findSortableCustomSequenceCardAt(int mouseX, int mouseY,
            PathSequence source) {
        for (SequenceCardRenderInfo card : visibleCustomSequenceCards) {
            if (card.bounds != null && card.bounds.contains(mouseX, mouseY)
                    && canReorderCustomSequenceCards(source, card.sequence)) {
                return card;
            }
        }
        return null;
    }

    private static boolean shouldUseHorizontalCustomSequenceSplit(SequenceCardRenderInfo targetCard) {
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

    private static SequenceCardRenderInfo findVisibleCustomSequenceCardByName(String sequenceName) {
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

    private static CustomSequenceDropTarget findDroppableCategoryRowAt(int mouseX, int mouseY) {
        for (CategoryTreeRow row : visibleCategoryRows) {
            if (row.bounds != null && row.bounds.contains(mouseX, mouseY) && row.isDroppableTarget()) {
                return new CustomSequenceDropTarget(row.category, row.subCategory, row.bounds);
            }
        }
        return null;
    }

    private static CustomSequenceDropTarget findCustomSectionDropTargetAt(int mouseX, int mouseY,
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

    private static void normalizeCategoryState() {
        if ("gui.inventory.category.common".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.common");
        } else if ("gui.inventory.category.rsl".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.common");
        } else if ("gui.inventory.category.debug".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.debug");
        } else if ("gui.inventory.category.builtin_script".equals(sLastCategory)) {
            sLastCategory = I18n.format("gui.inventory.category.builtin_script");
        }

        if ("gui.inventory.category.common".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.common");
        } else if ("gui.inventory.category.rsl".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.common");
        } else if ("gui.inventory.category.debug".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.debug");
        } else if ("gui.inventory.category.builtin_script".equals(currentCategory)) {
            currentCategory = I18n.format("gui.inventory.category.builtin_script");
        }
    }

    private static List<String> getBuiltinSecondPageGroup() {
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
        categories.add(I18n.format("gui.inventory.category.debug"));

        List<String> pathCategories = PathSequenceManager.getVisibleCategories();
        pathCategories.remove(I18n.format("gui.inventory.category.builtin_path"));

        categories.add(I18n.format("gui.inventory.category.builtin_script"));
        categories.addAll(filterStandalonePathCategories(pathCategories));

        List<String> availablePrimaryCategories = getBuiltinRoutePrimaryCategories();
        if (builtinScriptPrimaryCategory != null
                && !availablePrimaryCategories.contains(builtinScriptPrimaryCategory)) {
            builtinScriptPrimaryCategory = null;
            builtinScriptSubCategory = null;
        }
        if (builtinScriptPrimaryCategory != null && builtinScriptSubCategory != null
                && !builtinScriptSubCategory.trim().isEmpty()) {
            List<String> availableSubCategories = getBuiltinRouteSubCategoriesByPrimary(builtinScriptPrimaryCategory);
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
            currentPage = getDefaultPageForCategory(currentCategory);
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
        setItems.add("toggle_kill_timer");
        setItemNames.add(I18n.format("gui.inventory.kill_timer.name"));
        itemTooltips.put("toggle_kill_timer", I18n.format("gui.inventory.item.kill_timer.tooltip"));
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

        setItems.add("toggle_server_feature_visibility");
        setItemNames.add(I18n.format("gui.inventory.item.server_feature_visibility.name"));
        itemTooltips.put("toggle_server_feature_visibility",
                I18n.format("gui.inventory.item.server_feature_visibility.tooltip"));

        setItems.add("setloop");
        setItemNames.add(I18n.format("gui.inventory.item.setloop.name"));
        itemTooltips.put("setloop", I18n.format("gui.inventory.item.setloop.tooltip"));

        categoryItems.put(I18n.format("gui.inventory.category.common"), setItems);
        categoryItemNames.put(I18n.format("gui.inventory.category.common"), setItemNames);
        rebuildCommonSections(setItems);

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
                        if (isBuiltinRouteSequence(sequence)
                                && builtinScriptPrimaryCategory.equals(getBuiltinRoutePrimaryCategory(sequence))
                                && builtinScriptSubCategory.equals(getBuiltinRouteSubCategory(sequence))) {
                            categorySequences.add(sequence);
                        }
                    }
                } else if (builtinScriptPrimaryCategory != null && !builtinScriptPrimaryCategory.trim().isEmpty()) {
                    pathItems.add(CMD_BUILTIN_PRIMARY_BACK);
                    pathItemNames.add(I18n.format("gui.common.back"));
                    itemTooltips.put(CMD_BUILTIN_PRIMARY_BACK, "返回内置脚本主分组");

                    for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
                        if (isBuiltinRouteSequence(sequence)
                                && builtinScriptPrimaryCategory.equals(getBuiltinRoutePrimaryCategory(sequence))
                                && getBuiltinRouteSubCategory(sequence).isEmpty()) {
                            categorySequences.add(sequence);
                        }
                    }

                    for (String subCategory : getBuiltinRouteSubCategoriesByPrimary(builtinScriptPrimaryCategory)) {
                        String cmd = CMD_BUILTIN_SUBCAT_PREFIX + subCategory;
                        pathItems.add(cmd);
                        pathItemNames.add(subCategory);
                        itemTooltips.put(cmd, "查看子分类: " + subCategory);
                    }
                } else {
                    for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
                        if (isBuiltinMainScript(sequence)) {
                            categorySequences.add(sequence);
                        }
                    }

                    reorderBuiltinScriptsForSecondPage(categorySequences);

                    for (String primaryCategory : getBuiltinRoutePrimaryCategories()) {
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

        clampCurrentPageToCategoryBounds();
    }

    private static boolean isCommonCategory(String category) {
        return I18n.format("gui.inventory.category.common").equals(category);
    }

    private static void rebuildCommonSections(List<String> availableCommands) {
        commonItemSections.clear();
        LinkedHashSet<String> available = new LinkedHashSet<>(availableCommands);

        addCommonSection("automation", "自动化", available, Arrays.asList("autoeat", "toggle_auto_fishing",
                "toggle_auto_pickup", "toggle_auto_use_item", "followconfig"));
        addCommonSection("combat_execute", "战斗与执行", available,
                Arrays.asList("toggle_kill_aura", "toggle_kill_timer", "conditional_execution", "auto_escape", "setloop"));
        addCommonSection("config_interaction", "配置与交互", available, Arrays.asList("toggle_mouse_detach",
                "keybind_manager", "profile_manager", "chat_optimization", "toggle_server_feature_visibility"));
        addCommonSection("movement_scene", "移动与场景", available,
                Arrays.asList("toggle_fly", "block_replacement_config", "warehouse_manager", "baritone_settings"));
        for (GroupedItemSection section : commonItemSections) {
            commonSectionExpanded.putIfAbsent(section.key, Boolean.TRUE);
        }
    }

    private static void addCommonSection(String key, String title, Set<String> availableCommands,
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

    private static int getCommonSectionItemRows(GroupedItemSection section) {
        if (section == null || section.commands == null || section.commands.isEmpty()) {
            return 0;
        }
        return (section.commands.size() + 2) / 3;
    }

    private static int getCommonSectionPageUnits(GroupedItemSection section) {
        boolean expanded = section != null && commonSectionExpanded.getOrDefault(section.key, Boolean.TRUE);
        return expanded ? Math.max(1, 1 + getCommonSectionItemRows(section)) : 1;
    }

    private static List<List<GroupedItemSection>> buildCommonContentPages() {
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

    private static class CustomGridMetrics {
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

    private static boolean isCustomCategorySelection() {
        return isCustomOverlayCategory(currentCategory);
    }

    private static String normalizeSequenceSubCategory(PathSequence sequence) {
        return sequence == null ? "" : normalizeText(sequence.getSubCategory());
    }

    private static List<PathSequence> getCustomSequencesForSelection(String category, String subCategory) {
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

    private static CustomGridMetrics computeCustomGridMetrics(OverlayMetrics m, int contentStartY, String category) {
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

    private static String buildCustomSequenceTooltip(PathSequence sequence) {
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

    private static String getCustomSectionKey(String category, String subCategory) {
        String normalizedSubCategory = normalizeText(subCategory);
        return normalizeText(category) + "::section::" + (normalizedSubCategory.isEmpty() ? "__uncategorized__"
                : normalizedSubCategory.toLowerCase(Locale.ROOT));
    }

    private static boolean isCustomSectionExpanded(String key) {
        return customSectionExpanded.getOrDefault(normalizeText(key), Boolean.TRUE);
    }

    private static void toggleCustomSectionExpanded(String key) {
        String normalizedKey = normalizeText(key);
        customSectionExpanded.put(normalizedKey, !isCustomSectionExpanded(normalizedKey));
    }

    private static void ensureCustomSequenceSearchField(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        int safeWidth = Math.max(40, width);
        int safeHeight = Math.max(16, height);
        if (customSequenceSearchField == null) {
            customSequenceSearchField = new GuiTextField(8600, mc.font, x, y, safeWidth, safeHeight);
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

    private static void clearCustomSequenceSearch(boolean keepFocus) {
        customSequenceSearchQuery = "";
        if (customSequenceSearchField != null) {
            customSequenceSearchField.setText("");
            customSequenceSearchField.setFocused(keepFocus);
        }
        currentPage = 0;
        CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
    }

    private static boolean isCustomSearchExpanded() {
        return customSequenceSearchExpanded;
    }

    private static void setCustomSearchExpanded(boolean expanded, boolean focusField) {
        customSequenceSearchExpanded = expanded;
        if (customSequenceSearchField != null) {
            customSequenceSearchField.setFocused(expanded && focusField);
        }
        customSequenceSearchFocusPending = expanded && focusField && customSequenceSearchField == null;
    }

    private static String getCompactCustomSearchScopeLabel() {
        String effectiveScope = getEffectiveCustomSearchScope();
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(effectiveScope)) {
            return "子类";
        }
        if (SEARCH_SCOPE_ALL_CATEGORIES.equals(effectiveScope)) {
            return "全分类";
        }
        return "分类";
    }

    private static int getCustomSearchHeaderReservedWidth(FontRenderer fontRenderer, float scale) {
        return 0;
    }

    private static void cycleCustomSearchScope() {
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

    private static String getEffectiveCustomSearchScope() {
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(customSequenceSearchScope) && isBlank(currentCustomSubCategory)) {
            return SEARCH_SCOPE_CURRENT_CATEGORY;
        }
        return customSequenceSearchScope;
    }

    private static String getCustomSearchScopeLabel(String scope) {
        if (SEARCH_SCOPE_CURRENT_SUBCATEGORY.equals(scope)) {
            return "当前子分类";
        }
        if (SEARCH_SCOPE_ALL_CATEGORIES.equals(scope)) {
            return "全分类";
        }
        return "当前分类";
    }

    private static void applyCustomSearchScope(String scope) {
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

    private static List<String> getVisibleCustomCategoriesInDisplayOrder() {
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

    private static boolean matchesCustomSequenceSearchNormalized(PathSequence sequence, String normalizedQuery) {
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

    private static String buildCustomSectionDisplayTitle(String category, String subCategory, boolean includeCategory) {
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

    private static long getMostRecentSequenceOpenedAt(List<PathSequence> sequences) {
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

    private static int getRunningSequenceCount(List<PathSequence> sequences) {
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

    private static String buildCustomSectionStatsLabel(List<PathSequence> sequences) {
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

    private static List<String> buildSequenceCardTitleLines(FontRenderer fontRenderer, String title, int maxWidth,
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

    private static void drawCenteredWrappedText(FontRenderer fontRenderer, List<String> lines, int centerX, int topY,
            int width, int lineHeight, int color) {
        for (int i = 0; i < lines.size(); i++) {
            String line = fontRenderer.trimStringToWidth(lines.get(i), Math.max(24, width));
            drawCenteredString(fontRenderer, line, centerX, topY + i * lineHeight, color);
        }
    }

    private static List<PathSequence> getCustomSequencesForActiveSearchScope(String category,
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

    private static boolean matchesCustomSequenceSearch(PathSequence sequence, String query) {
        return matchesCustomSequenceSearchNormalized(sequence, PinyinSearchHelper.normalizeQuery(query));
    }

    private static List<CustomSectionModel> buildCustomSectionModels(String category, String selectedSubCategory) {
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

    private static CustomPageLayout buildCustomPageLayout(OverlayMetrics m, int contentStartY) {
        CustomGridMetrics grid = computeCustomGridMetrics(m, contentStartY, currentCategory);
        FontRenderer fontRenderer = wrapFont(Minecraft.getInstance().font);
        boolean searchExpanded = isCustomSearchExpanded();
        int topButtonWidth = getTopButtonWidth(m, 5);
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

    private static CustomSectionRenderInfo findCustomSectionHeaderAt(int mouseX, int mouseY) {
        for (CustomSectionRenderInfo info : visibleCustomSectionHeaders) {
            if (info.bounds != null && info.bounds.contains(mouseX, mouseY)) {
                return info;
            }
        }
        return null;
    }

    private static String formatRelativeTime(long lastOpenedAt) {
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

    private static boolean isSequenceRecentlyOpened(MainUiLayoutManager.SequenceOpenStats stats) {
        return stats != null && stats.lastOpenedAt > 0L
                && System.currentTimeMillis() - stats.lastOpenedAt <= RECENT_OPEN_HIGHLIGHT_WINDOW_MS;
    }

    private static String getRunningCustomSequenceName() {
        if (!PathSequenceEventListener.instance.isTracking()
                || PathSequenceEventListener.instance.currentSequence == null
                || !PathSequenceEventListener.instance.currentSequence.isCustom()) {
            return "";
        }
        return normalizeText(PathSequenceEventListener.instance.currentSequence.getName());
    }

    private static boolean isCustomCategoryRunning(String category) {
        String runningSequenceName = getRunningCustomSequenceName();
        if (runningSequenceName.isEmpty()) {
            return false;
        }
        PathSequence sequence = PathSequenceManager.getSequence(runningSequenceName);
        return sequence != null && sequence.isCustom()
                && normalizeText(category).equals(normalizeText(sequence.getCategory()));
    }

    private static boolean isCustomCategoryRecentlyOpened(String category) {
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

    private static int getCurrentTotalPages(OverlayMetrics m, int contentStartY) {
        if (isCustomCategorySelection()) {
            return buildCustomPageLayout(m, contentStartY).totalPages;
        }
        return getTotalPagesForCategory(currentCategory);
    }

    private static int getTotalPagesForCategory(String category) {
        if (isCommonCategory(category)) {
            return Math.max(1, buildCommonContentPages().size());
        }
        List<String> items = categoryItems.get(category);
        return Math.max(1, items == null ? 1 : (items.size() + 17) / 18);
    }

    private static void clampCurrentPageToCategoryBounds() {
        int totalPages;
        if (isCustomCategorySelection()) {
            Minecraft mc = Minecraft.getInstance();
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

    private static String findBaseItemName(String command) {
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

    private static String getCommonDisplayName(String command) {
        String baseName = findBaseItemName(command);
        if ("toggle_auto_fishing".equals(command)) {
            return AutoFishingHandler.enabled ? I18n.format("gui.inventory.auto_fishing.on") : baseName;
        }
        if ("toggle_kill_aura".equals(command)) {
            return KillAuraHandler.enabled ? I18n.format("gui.inventory.kill_aura.on") : baseName;
        }
        if ("toggle_kill_timer".equals(command)) {
            return KillTimerHandler.isEnabled ? I18n.format("gui.inventory.kill_timer.on") : baseName;
        }
        if ("toggle_fly".equals(command)) {
            return FlyHandler.enabled ? I18n.format("gui.inventory.fly.on") : baseName;
        }
        return baseName;
    }

    private static GuiTheme.UiState getCommonItemState(String command, boolean isHovering) {
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
        } else if ("toggle_kill_timer".equals(command)) {
            active = KillTimerHandler.isEnabled;
        } else if ("toggle_fly".equals(command)) {
            active = FlyHandler.enabled;
        } else if ("toggle_auto_pickup".equals(command)) {
            active = AutoPickupHandler.globalEnabled;
        } else if ("conditional_execution".equals(command)) {
            active = ConditionalExecutionHandler.isGloballyEnabled();
        } else if ("auto_escape".equals(command)) {
            active = AutoEscapeHandler.isGloballyEnabled();
        } else if ("toggle_auto_use_item".equals(command)) {
            active = AutoUseItemHandler.globalEnabled;
        } else if ("toggle_server_feature_visibility".equals(command)) {
            active = ServerFeatureVisibilityManager.isAnyRuleEnabled();
        }

        if (active) {
            return GuiTheme.UiState.SUCCESS;
        }
        return isHovering ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    private static String drawGroupedCommonItems(OverlayMetrics m, int contentStartY, int mouseX, int mouseY,
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

    private static void drawCategoryTree(OverlayMetrics m, int contentStartY, int mouseX, int mouseY) {
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
                drawString(Minecraft.getInstance().font, arrow, buttonX + 8,
                        buttonY + (categoryButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        hovered ? 0xFFFFFFFF : 0xFFAEDBFF);
            }

            String label = Minecraft.getInstance().font.plainSubstrByWidth(getCategoryRowDisplayLabel(row),
                    buttonWidth - (hasChildren ? 30 : 20));
            drawCenteredString(Minecraft.getInstance().font, label, buttonX + buttonWidth / 2,
                    buttonY + (categoryButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                    0xFFFFFFFF);

            if (runningCategory) {
                drawString(Minecraft.getInstance().font, "▶", buttonX + buttonWidth - 11,
                        buttonY + (categoryButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFF7FD1FF);
            } else if (pinnedCategory) {
                drawString(Minecraft.getInstance().font, "★", buttonX + buttonWidth - 12,
                        buttonY + (categoryButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFD47A);
            } else if (recentCategory) {
                drawString(Minecraft.getInstance().font, "●", buttonX + buttonWidth - 11,
                        buttonY + (categoryButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
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

        drawCenteredString(Minecraft.getInstance().font, "右键空白处新建分类", categoryPanelX + categoryPanelWidth / 2,
                categoryPanelY + categoryPanelHeight - 12, 0xAA9FB2C8);
    }

    private static String drawCustomSequenceCards(OverlayMetrics m, int contentStartY, int mouseX, int mouseY,
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
                        boolean foregroundRunning = PathSequenceEventListener.isSequenceRunningInForeground(sequence.getName());
                        boolean backgroundRunning = PathSequenceEventListener.isSequenceRunningInBackground(sequence.getName());
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

    private static void trimContextMenuOpenPath(int newSize) {
        while (contextMenuOpenPath.size() > newSize) {
            contextMenuOpenPath.remove(contextMenuOpenPath.size() - 1);
        }
    }

    private static void drawSequenceRunStripe(int x, int y, int width, boolean background) {
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

    private static int getContextMenuHorizontalGap() {
        return 4;
    }

    private static int getContextMenuWidth(List<ContextMenuItem> items, FontRenderer fontRenderer) {
        int width = 44;
        for (ContextMenuItem item : items) {
            String label = item.selected ? "√ " + item.label : item.label;
            int reservedWidth = item.hasChildren() ? 26 : 16;
            width = Math.max(width, fontRenderer.getStringWidth(label) + reservedWidth);
        }
        return width;
    }

    private static int getContextMenuChainWidth(List<ContextMenuItem> items, FontRenderer fontRenderer) {
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

    private static int clampContextMenuX(int desiredX, int menuWidth, int screenWidth) {
        int margin = 6;
        return Math.max(margin, Math.min(desiredX, screenWidth - menuWidth - margin));
    }

    private static int clampContextMenuY(int desiredY, int menuHeight, int screenHeight) {
        int margin = 6;
        return Math.max(margin, Math.min(desiredY, screenHeight - menuHeight - margin));
    }

    private static boolean intersectsAnyAncestor(Rectangle candidateBounds, List<Rectangle> ancestorBounds) {
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

    private static int resolveSubMenuX(Rectangle anchorRect, List<ContextMenuItem> childItems, FontRenderer fontRenderer,
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

    private static Rectangle getSubMenuBoundsForItem(Rectangle anchorRect, List<ContextMenuItem> childItems,
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

    private static boolean shouldOpenSubMenuToLeft(Rectangle anchorRect, List<ContextMenuItem> childItems,
            FontRenderer fontRenderer, int screenWidth, int screenHeight, List<Rectangle> ancestorBounds,
            boolean preferLeft) {
        Rectangle bounds = getSubMenuBoundsForItem(anchorRect, childItems, fontRenderer, screenWidth, screenHeight,
                ancestorBounds, preferLeft);
        return bounds != null && bounds.x + bounds.width <= anchorRect.x;
    }

    private static void drawContextMenus(int mouseX, int mouseY, int screenWidth, int screenHeight,
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

    private static void drawCustomSequenceDragGhost(int mouseX, int mouseY, FontRenderer fontRenderer) {
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

    private static void drawCategoryTreeDragGhost(int mouseX, int mouseY, FontRenderer fontRenderer) {
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

    private static boolean isProtectedCustomCategory(String category) {
        return I18n.format("path.category.default").equals(category)
                || I18n.format("path.category.builtin").equals(category);
    }

    private static void activateSequence(PathSequence sequence) {
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

    private static void clearPressedCustomSequence() {
        pressedCustomSequence = null;
        pressedCustomSequenceRect = null;
        isDraggingCustomSequenceCard = false;
        currentSequenceDropTarget = null;
        currentCustomSequenceSortTargetName = "";
        currentCustomSequenceSortAfter = false;
        customSequencePageTurnLockUntil = 0L;
    }

    private static void clearPressedCategoryRow() {
        pressedCategoryRow = null;
        pressedCategoryRowRect = null;
        isDraggingCategoryRow = false;
        currentCategorySortDropTarget = null;
        currentCategorySortDropAfter = false;
    }

    private static void setAllCustomCategoryCollapsed(boolean collapsed) {
        MainUiLayoutManager.setCollapsedForCategories(getVisibleCustomCategoriesInDisplayOrder(), collapsed);
        refreshGuiLists();
    }

    private static ContextMenuItem buildCategoryTreeOrganizeMenu() {
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

    private static List<ContextMenuItem> buildCategoryBlankAreaMenu() {
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

    private static List<ContextMenuItem> buildCategoryContextMenu(String category) {
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

    private static List<ContextMenuItem> buildSubCategoryContextMenu(String category, String subCategory) {
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

    private static ContextMenuItem buildSequenceDestinationMenu(String label, PathSequence sequence, boolean copy) {
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

    private static ContextMenuItem buildBatchSequenceDestinationMenu(String label, List<String> sequenceNames,
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

    private static List<ContextMenuItem> buildBatchMoreMenu(List<String> sequenceNames) {
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

    private static List<ContextMenuItem> buildSequenceCardContextMenu(PathSequence sequence) {
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(menuItem("删除", () -> openOverlayConfirm("删除路径序列", "删除后不可恢复，是否继续？", () -> {
            PathSequenceManager.deleteCustomSequence(sequence.getName());
            MainUiLayoutManager.removeSequenceStats(sequence.getName());
            refreshGuiLists();
        })));
        items.add(buildSequenceDestinationMenu("移动到", sequence, false));
        items.add(buildSequenceDestinationMenu("复制到", sequence, true));
        items.add(menuItem("编辑", () -> {
            closeContextMenu();
            closeOverlay();
            Minecraft.getInstance()
                    .setScreen(GuiPathManager.openForSequence(sequence.getCategory(), sequence.getName()));
        }));
        return items;
    }

    private static void handleCustomToolbarAction(CustomButtonRenderInfo button, int mouseX, int mouseY) {
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

    private static boolean handleContextMenuClick(int mouseX, int mouseY, int mouseButton) {
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

    private static boolean handleCustomSequenceCategoryClick(int mouseX, int mouseY, int mouseButton, OverlayMetrics m,
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

    private static boolean handleGroupedCommonCategoryClick(int mouseX, int mouseY, int mouseButton, OverlayMetrics m,
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

    private static boolean handleCommonCommandClick(String command, int mouseButton, Minecraft mc) throws IOException {
        if ("profile_manager".equals(command)) {
            closeOverlay();
            mc.setScreen(new GuiProfileManager(null));
            return true;
        } else if ("chat_optimization".equals(command)) {
            closeOverlay();
            mc.setScreen(new GuiChatOptimization(null));
            return true;
        } else if ("toggle_auto_pickup".equals(command)) {
            if (mouseButton == 0) {
                AutoPickupHandler.globalEnabled = !AutoPickupHandler.globalEnabled;
                AutoPickupHandler.saveConfig();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiAutoPickupConfig(null));
            }
            return true;
        } else if ("toggle_auto_use_item".equals(command)) {
            if (mouseButton == 0) {
                AutoUseItemHandler.globalEnabled = !AutoUseItemHandler.globalEnabled;
                AutoUseItemHandler.INSTANCE.resetSchedule();
                AutoUseItemHandler.saveConfig();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiAutoUseItemConfig(null));
            }
            return true;
        } else if ("block_replacement_config".equals(command)) {
            if (mouseButton == 0 || mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiBlockReplacementConfig(null));
            }
            return true;
        } else if ("toggle_server_feature_visibility".equals(command)) {
            if (mouseButton == 0 || mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiServerFeatureVisibilityConfig(null));
            }
            return true;
        } else if ("setloop".equals(command)) {
            closeOverlay();
            mc.setScreen(new GuiLoopCountInput(null));
            return true;
        } else if ("autoeat".equals(command)) {
            if (mouseButton == 0) {
                AutoEatHandler.autoEatEnabled = !AutoEatHandler.autoEatEnabled;
                AutoEatHandler.saveAutoEatConfig();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiAutoEatConfig(null));
            }
            return true;
        } else if ("toggle_auto_fishing".equals(command)) {
            if (mouseButton == 0) {
                AutoFishingHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiAutoFishingConfig(null));
            }
            return true;
        } else if ("toggle_mouse_detach".equals(command)) {
            ModConfig.isMouseDetached = !ModConfig.isMouseDetached;
            String mouseStatus = ModConfig.isMouseDetached ? I18n.format("gui.inventory.mouse.detached")
                    : I18n.format("gui.inventory.mouse.reattached");
            if (mc.player != null) {
                mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.inventory.mouse_toggle", mouseStatus)));
            }
            if (ModConfig.isMouseDetached) {
                mc.mouseHandler.releaseMouse();
            } else if (mc.screen == null) {
                mc.mouseHandler.grabMouse();
            }
            refreshGuiLists();
            return true;
        } else if ("followconfig".equals(command)) {
            if (mouseButton == 0) {
                boolean wasActive = AutoFollowHandler.getActiveRule() != null;
                if (wasActive) {
                    AutoFollowHandler.toggleEnabledFromQuickSwitch();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(new TextComponentString("§b[自动追怪] §c已关闭"));
                    }
                } else {
                    if (!AutoFollowHandler.hasAnyRuleConfigured()) {
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(new TextComponentString("§b[自动追怪] §e未配置任何规则，请右键打开配置界面"));
                        }
                    } else {
                        com.zszl.zszlScriptMod.system.AutoFollowRule activatedRule = AutoFollowHandler.toggleEnabledFromQuickSwitch();
                        if (mc.player != null) {
                            String suffix = activatedRule != null && activatedRule.name != null
                                    && !activatedRule.name.trim().isEmpty()
                                            ? " §7规则: §f" + activatedRule.name.trim()
                                            : "";
                            mc.player.sendSystemMessage(new TextComponentString("§b[自动追怪] §a已开启" + suffix));
                        }
                    }
                }
                refreshGuiLists();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiAutoFollowManager(null));
            }
            return true;
        } else if ("conditional_execution".equals(command)) {
            if (mouseButton == 0) {
                ConditionalExecutionHandler.setGlobalEnabled(!ConditionalExecutionHandler.isGloballyEnabled());
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiConditionalExecutionManager(null));
            }
            return true;
        } else if ("auto_escape".equals(command)) {
            if (mouseButton == 0) {
                AutoEscapeHandler.setGlobalEnabled(!AutoEscapeHandler.isGloballyEnabled());
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiAutoEscapeManager(null));
            }
            return true;
        } else if ("keybind_manager".equals(command)) {
            closeOverlay();
            mc.setScreen(new GuiKeybindManager(null));
            return true;
        } else if ("warehouse_manager".equals(command)) {
            closeOverlay();
            mc.setScreen(new GuiWarehouseManager(null));
            return true;
        } else if ("baritone_settings".equals(command)) {
            closeOverlay();
            mc.setScreen(new GuiBaritoneCommandTable(null));
            return true;
        } else if ("toggle_kill_aura".equals(command)) {
            if (mouseButton == 0) {
                KillAuraHandler.INSTANCE.toggleEnabled();
                refreshGuiLists();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiKillAuraConfig(null));
            }
            return true;
        } else if ("toggle_kill_timer".equals(command)) {
            if (mouseButton == 0) {
                KillTimerHandler.toggleEnabled();
                refreshGuiLists();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiKillTimerConfig(null));
            }
            return true;
        } else if ("toggle_fly".equals(command)) {
            if (mouseButton == 0) {
                FlyHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiFlyConfig(null));
            }
            return true;
        }

        return false;
    }

    private static List<String> filterStandalonePathCategories(List<String> pathCategories) {
        List<String> filtered = new ArrayList<>();
        Set<String> builtinRouteSubCategories = new HashSet<>(getBuiltinRouteSubCategories());
        Set<String> builtinRoutePrimaryCategories = new HashSet<>(getBuiltinRoutePrimaryCategories());
        for (String category : pathCategories) {
            if (builtinRouteSubCategories.contains(category)) {
                continue;
            }
            if (builtinRoutePrimaryCategories.contains(category)) {
                continue;
            }
            filtered.add(category);
        }
        return filtered;
    }

    private static String getBuiltinRouteSubCategory(PathSequence sequence) {
        if (sequence == null || sequence.isCustom()) {
            return "";
        }
        String subCategory = sequence.getSubCategory();
        if (subCategory != null && !subCategory.trim().isEmpty()) {
            return subCategory.trim();
        }
        return "";
    }

    private static String getBuiltinRoutePrimaryCategory(PathSequence sequence) {
        if (sequence == null || sequence.isCustom()) {
            return "";
        }
        String category = sequence.getCategory();
        if (category == null || category.trim().isEmpty()) {
            return "";
        }
        return category.trim();
    }

    private static boolean isBuiltinMainScript(PathSequence sequence) {
        return sequence != null && !sequence.isCustom()
                && I18n.format("gui.inventory.category.builtin_path").equals(sequence.getCategory())
                && getBuiltinRouteSubCategory(sequence).isEmpty();
    }

    private static boolean isBuiltinRouteSequence(PathSequence sequence) {
        return sequence != null && !sequence.isCustom() && !isBuiltinMainScript(sequence)
                && !getBuiltinRoutePrimaryCategory(sequence).isEmpty();
    }

    private static boolean isBuiltinRouteCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (category.equals(getBuiltinRouteSubCategory(sequence))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> getBuiltinRouteSubCategories() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            String subCategory = getBuiltinRouteSubCategory(sequence);
            if (!subCategory.isEmpty()) {
                result.add(subCategory);
            }
        }
        return new ArrayList<>(result);
    }

    private static List<String> getBuiltinRoutePrimaryCategories() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (!isBuiltinRouteSequence(sequence)) {
                continue;
            }
            String primaryCategory = getBuiltinRoutePrimaryCategory(sequence);
            if (!primaryCategory.isEmpty() && !shouldHideBuiltinRoutePrimaryCategory(primaryCategory)) {
                result.add(primaryCategory);
            }
        }
        return new ArrayList<>(result);
    }

    private static boolean shouldHideBuiltinRoutePrimaryCategory(String primaryCategory) {
        if (primaryCategory == null || primaryCategory.trim().isEmpty()) {
            return false;
        }
        String normalized = primaryCategory.trim();
        if (ServerFeatureVisibilityManager.shouldHideMotaFeatures() && "魔塔之巅".equals(normalized)) {
            return true;
        }
        return false;
    }

    private static List<String> getBuiltinRouteSubCategoriesByPrimary(String primaryCategory) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (primaryCategory == null || primaryCategory.trim().isEmpty()) {
            return new ArrayList<>(result);
        }
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            if (!isBuiltinRouteSequence(sequence)) {
                continue;
            }
            if (!primaryCategory.equals(getBuiltinRoutePrimaryCategory(sequence))) {
                continue;
            }
            String subCategory = getBuiltinRouteSubCategory(sequence);
            if (!subCategory.isEmpty()) {
                result.add(subCategory);
            }
        }
        return new ArrayList<>(result);
    }

    private static int getDefaultPageForCategory(String category) {
        if (!I18n.format("gui.inventory.category.builtin_script").equals(category)) {
            return 0;
        }
        List<String> items = categoryItems.get(category);
        int totalPages = (items != null) ? Math.max(1, (items.size() + 17) / 18) : 1;
        return totalPages > 1 ? 1 : 0;
    }

    private static void reorderBuiltinScriptsForSecondPage(List<PathSequence> categorySequences) {
        List<PathSequence> grouped = new ArrayList<>();
        List<String> secondPageGroup = getBuiltinSecondPageGroup();

        Iterator<PathSequence> iterator = categorySequences.iterator();
        while (iterator.hasNext()) {
            PathSequence sequence = iterator.next();
            if (isBuiltinSecondPageSequence(sequence.getName())) {
                grouped.add(sequence);
                iterator.remove();
            }
        }

        grouped.sort((a, b) -> {
            int ia = secondPageGroup.indexOf(a.getName());
            int ib = secondPageGroup.indexOf(b.getName());
            if (ia == -1)
                ia = Integer.MAX_VALUE;
            if (ib == -1)
                ib = Integer.MAX_VALUE;
            if (ia != ib)
                return Integer.compare(ia, ib);
            return a.getName().compareTo(b.getName());
        });

        categorySequences.addAll(grouped);
    }

    private static boolean isBuiltinSecondPageSequence(String name) {
        return name != null && getBuiltinSecondPageGroup().contains(name);
    }

    private static int getMerchantTotalPages(MerchantDef merchant) {
        List<Integer> visibleIndices = getMerchantVisibleExchangeIndices(merchant);
        if (visibleIndices.isEmpty()) {
            return 1;
        }
        return Math.max(1, (visibleIndices.size() + 3) / 4);
    }

    private static void normalizeMerchantCategoryState(MerchantDef merchant) {
        if (merchant == null || merchant.categories == null || merchant.categories.isEmpty()) {
            selectedMerchantCategoryIndex = -1;
            merchantCategoryScrollOffset = 0;
            return;
        }
        if (selectedMerchantCategoryIndex < 0 || selectedMerchantCategoryIndex >= merchant.categories.size()) {
            selectedMerchantCategoryIndex = 0;
        }
        merchantCategoryScrollOffset = MathHelper.clamp(merchantCategoryScrollOffset, 0,
                Math.max(0, merchant.categories.size() - 1));
    }

    private static List<Integer> getMerchantVisibleExchangeIndices(MerchantDef merchant) {
        List<Integer> visible = new ArrayList<>();
        if (merchant == null || merchant.exchanges == null || merchant.exchanges.isEmpty()) {
            return visible;
        }

        normalizeMerchantCategoryState(merchant);
        if (merchant.categories == null || merchant.categories.isEmpty() || selectedMerchantCategoryIndex < 0
                || selectedMerchantCategoryIndex >= merchant.categories.size()) {
            for (int i = 0; i < merchant.exchanges.size(); i++) {
                visible.add(i);
            }
            return visible;
        }

        CategoryDef selectedCategory = merchant.categories.get(selectedMerchantCategoryIndex);
        int start = MathHelper.clamp(selectedCategory.startIndex, 0, merchant.exchanges.size() - 1);
        int end = MathHelper.clamp(selectedCategory.endIndex, start, merchant.exchanges.size() - 1);
        for (int i = start; i <= end; i++) {
            visible.add(i);
        }
        return visible;
    }

    private static int getMerchantCategoryButtonWidth(FontRenderer fontRenderer, String text) {
        if (fontRenderer == null) {
            return 52;
        }
        return MathHelper.clamp(fontRenderer.getStringWidth(text) + 16, 52, 96);
    }

    private static int getMerchantCategoryButtonWidth(net.minecraft.client.gui.Font fontRenderer, String text) {
        return getMerchantCategoryButtonWidth(wrapFont(fontRenderer), text);
    }

    private static int getSafeCategoryListButtonWidth(OverlayMetrics m) {
        return Math.max(scaleUi(40, m.scale),
                Math.min(m.categoryButtonWidth, m.categoryPanelWidth - m.padding * 2 - scaleUi(8, m.scale)));
    }

    private static int getTopButtonWidth(OverlayMetrics m, int buttonCount) {
        int gap = m.padding;
        int availableWidth = Math.max(scaleUi(160, m.scale), m.totalWidth - m.padding * 2);
        int fittedWidth = (availableWidth - gap * Math.max(0, buttonCount - 1)) / Math.max(1, buttonCount);
        return Math.max(scaleUi(40, m.scale), Math.min(m.pathManagerButtonWidth, fittedWidth));
    }

    private static void normalizeOtherFeatureGroupState(List<GroupDef> groups) {
        if (groups == null || groups.isEmpty()) {
            selectedOtherFeatureGroupIndex = -1;
            otherFeatureScreenPage = 0;
            otherFeatureGroupScrollOffset = 0;
            return;
        }
        if (selectedOtherFeatureGroupIndex < 0 || selectedOtherFeatureGroupIndex >= groups.size()) {
            selectedOtherFeatureGroupIndex = 0;
        }
        otherFeatureGroupScrollOffset = MathHelper.clamp(otherFeatureGroupScrollOffset, 0, Math.max(0, groups.size() - 1));
    }

    private static boolean isOtherFeatureEnabled(String featureId) {
        if ("speed".equalsIgnoreCase(featureId)) {
            return SpeedHandler.enabled;
        }
        if (MovementFeatureManager.isManagedFeature(featureId)) {
            return MovementFeatureManager.isEnabled(featureId);
        }
        if (BlockFeatureManager.isManagedFeature(featureId)) {
            return BlockFeatureManager.isEnabled(featureId);
        }
        if (RenderFeatureManager.isManagedFeature(featureId)) {
            return RenderFeatureManager.isEnabled(featureId);
        }
        if (WorldFeatureManager.isManagedFeature(featureId)) {
            return WorldFeatureManager.isEnabled(featureId);
        }
        if (ItemFeatureManager.isManagedFeature(featureId)) {
            return ItemFeatureManager.isEnabled(featureId);
        }
        if (MiscFeatureManager.isManagedFeature(featureId)) {
            return MiscFeatureManager.isEnabled(featureId);
        }
        return false;
    }

    private static GuiTheme.UiState getOtherFeatureItemState(String featureId, boolean hover) {
        if (isOtherFeatureEnabled(featureId)) {
            return GuiTheme.UiState.SUCCESS;
        }
        return hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    private static String getTranslatedOtherFeatureTooltip(String featureId) {
        if (featureId == null || featureId.trim().isEmpty()) {
            return null;
        }
        String key = "gui.inventory.other_feature." + featureId + ".tooltip";
        String translated = I18n.format(key);
        return key.equals(translated) ? null : translated;
    }

    private static String buildOtherFeatureTooltip(FeatureDef feature, FontRenderer fontRenderer, int wrapWidth) {
        if (feature == null) {
            return null;
        }

        int safeWrapWidth = Math.max(120, wrapWidth);
        List<String> lines = new ArrayList<>();

        String title = feature.name == null ? "" : feature.name.trim();
        if (!title.isEmpty()) {
            lines.add("§e" + title);
        }

        lines.add("§7当前状态: " + (isOtherFeatureEnabled(feature.id) ? "§a已开启" : "§c已关闭"));
        lines.add("§7左键快速开关  §8|  §7右键打开设置");

        String description = getTranslatedOtherFeatureTooltip(feature.id);
        if (description == null || description.trim().isEmpty()) {
            description = feature.description == null ? "" : feature.description.trim();
        }

        String normalized = description.replace("\\n", "\n");
        for (String rawLine : normalized.split("\n", -1)) {
            String paragraph = rawLine == null ? "" : rawLine.trim();
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String styledParagraph = "§7" + paragraph;
            if (fontRenderer == null) {
                lines.add(styledParagraph);
                continue;
            }

            List<String> wrapped = fontRenderer.listFormattedStringToWidth(styledParagraph, safeWrapWidth);
            if (wrapped == null || wrapped.isEmpty()) {
                lines.add(styledParagraph);
            } else {
                lines.addAll(wrapped);
            }
        }

        if ("timer_accel".equalsIgnoreCase(feature.id) && SpeedHandler.isTimerManagedBySpeed()) {
            lines.add("§6当前已被加速模块的 Timer 接管。");
            lines.add("§6这里的 Timer 暂不生效。");
        }

        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    private static String trimOverlayButtonLabel(FontRenderer fontRenderer, String text, int maxWidth) {
        String safeText = text == null ? "" : text.trim();
        if (fontRenderer == null || safeText.isEmpty() || maxWidth <= 0
                || fontRenderer.getStringWidth(safeText) <= maxWidth) {
            return safeText;
        }
        String ellipsis = "...";
        int ellipsisWidth = fontRenderer.getStringWidth(ellipsis);
        int textWidth = Math.max(0, maxWidth - ellipsisWidth);
        String trimmed = fontRenderer.trimStringToWidth(safeText, textWidth);
        if (trimmed == null || trimmed.isEmpty()) {
            return ellipsis;
        }
        return trimmed + ellipsis;
    }

    private static OtherFeaturePageControlBounds getOtherFeaturePageControlBounds(OverlayMetrics m, int contentX,
            int contentRight) {
        int pageButtonHeight = Math.max(18, m.itemButtonHeight);
        int pageAreaY = Math.max(m.contentStartY + m.padding,
                m.y + m.totalHeight - pageButtonHeight - Math.max(4, m.padding));
        int controlInset = Math.max(4, scaleUi(8, m.scale));
        int availableWidth = Math.max(1, contentRight - contentX - controlInset * 2);
        int controlGap = Math.max(2, Math.min(Math.max(4, m.padding + 2), Math.max(2, availableWidth / 12)));
        int minInfoWidth = Math.max(18,
                Math.min(Math.max(24, scaleUi(24, m.scale)), Math.max(18, availableWidth / 5)));
        int buttonWidth = Math.max(1, Math.min(m.pageButtonWidth, Math.max(24, availableWidth / 3)));
        int pageInfoWidth = availableWidth - buttonWidth * 2 - controlGap * 2;
        if (pageInfoWidth < minInfoWidth) {
            buttonWidth = Math.max(1, (availableWidth - minInfoWidth - controlGap * 2) / 2);
            pageInfoWidth = Math.max(1, availableWidth - buttonWidth * 2 - controlGap * 2);
        }
        int controlsWidth = Math.max(1, buttonWidth * 2 + pageInfoWidth + controlGap * 2);
        int controlsStartX = contentX + controlInset + Math.max(0, (availableWidth - controlsWidth) / 2);
        int containerX = Math.max(contentX, controlsStartX - controlInset);
        int containerY = Math.max(m.contentStartY + m.padding, pageAreaY - Math.max(3, scaleUi(4, m.scale)));
        int containerHeight = pageButtonHeight + Math.max(6, scaleUi(8, m.scale));
        int containerWidth = Math.min(Math.max(1, contentRight - contentX), controlsWidth + controlInset * 2);
        Rectangle prevButtonBounds = new Rectangle(controlsStartX, pageAreaY, buttonWidth, pageButtonHeight);
        Rectangle pageInfoBounds = new Rectangle(controlsStartX + buttonWidth + controlGap, pageAreaY, pageInfoWidth,
                pageButtonHeight);
        Rectangle nextButtonBounds = new Rectangle(pageInfoBounds.x + pageInfoBounds.width + controlGap, pageAreaY,
                buttonWidth, pageButtonHeight);
        return new OtherFeaturePageControlBounds(
                new Rectangle(containerX, containerY, Math.max(1, containerWidth), Math.max(1, containerHeight)),
                prevButtonBounds, nextButtonBounds, pageInfoBounds);
    }

    private static boolean shiftOtherFeatureScreenPage(int delta, int totalPages) {
        int safeTotalPages = Math.max(1, totalPages);
        int targetPage = MathHelper.clamp(otherFeatureScreenPage + delta, 0, safeTotalPages - 1);
        if (targetPage == otherFeatureScreenPage) {
            return false;
        }
        otherFeatureScreenPage = targetPage;
        return true;
    }

    private static OtherFeaturePageLayout buildOtherFeaturePageLayout(GroupDef group, OverlayMetrics m,
            FontRenderer fontRenderer) {
        int contentX = m.contentPanelX + m.padding;
        int contentRight = m.contentPanelRight - m.padding;
        int contentTop = m.contentStartY + m.padding;
        int contentWidth = Math.max(1, contentRight - contentX);
        OtherFeaturePageControlBounds pageControls = getOtherFeaturePageControlBounds(m, contentX, contentRight);
        int startX = contentX + 8;
        int startY = contentTop + 8;
        int availableWidth = Math.max(1, contentWidth - 16);
        int availableHeight = Math.max(m.itemButtonHeight, pageControls.containerBounds.y - 6 - startY);
        int buttonHeight = Math.max(18, m.itemButtonHeight);
        int gap = Math.max(4, m.gap - 2);
        List<OtherFeatureCardLayout> layouts = new ArrayList<>();
        if (group == null || group.features == null || group.features.isEmpty()) {
            return new OtherFeaturePageLayout(layouts,
                    new Rectangle(contentX, contentTop, Math.max(1, contentWidth), Math.max(1, availableHeight)), 0, 1,
                    pageControls);
        }

        List<FeatureDef> visibleFeatures = new ArrayList<>();
        for (FeatureDef feature : group.features) {
            if (feature != null) {
                visibleFeatures.add(feature);
            }
        }
        if (visibleFeatures.isEmpty()) {
            return new OtherFeaturePageLayout(layouts,
                    new Rectangle(contentX, contentTop, Math.max(1, contentWidth), Math.max(1, availableHeight)), 0, 1,
                    pageControls);
        }

        int safeGap = Math.max(4, gap);
        int safeContentWidth = Math.max(1, availableWidth);
        int minCardWidth = 84;
        if (fontRenderer != null) {
            minCardWidth = Math.max(minCardWidth, fontRenderer.getStringWidth("GUI界面下移动") + 20);
        }

        int columnCount = Math.max(1, Math.min(4, (safeContentWidth + safeGap) / (minCardWidth + safeGap)));
        int buttonWidth = (safeContentWidth - safeGap * Math.max(0, columnCount - 1)) / columnCount;
        while (columnCount > 1 && buttonWidth < minCardWidth) {
            columnCount--;
            buttonWidth = (safeContentWidth - safeGap * Math.max(0, columnCount - 1)) / columnCount;
        }

        int rowsPerPage = Math.max(1, (availableHeight + safeGap) / (buttonHeight + safeGap));
        int pageSize = Math.max(1, rowsPerPage * columnCount);
        int totalPages = Math.max(1, (visibleFeatures.size() + pageSize - 1) / pageSize);
        int currentPage = MathHelper.clamp(otherFeatureScreenPage, 0, totalPages - 1);
        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(visibleFeatures.size(), startIndex + pageSize);

        for (int i = startIndex; i < endIndex; i++) {
            int localIndex = i - startIndex;
            int row = localIndex / columnCount;
            int col = localIndex % columnCount;
            int buttonX = startX + col * (buttonWidth + safeGap);
            int buttonY = startY + row * (buttonHeight + safeGap);
            layouts.add(new OtherFeatureCardLayout(visibleFeatures.get(i),
                    new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight)));
        }

        return new OtherFeaturePageLayout(layouts,
                new Rectangle(contentX, contentTop, Math.max(1, contentWidth), Math.max(1, availableHeight)),
                currentPage, totalPages, pageControls);
    }

    private static List<OtherFeatureCardLayout> buildOtherFeatureCardLayouts(GroupDef group, int startX, int startY,
            int contentWidth, int buttonHeight, int gap, FontRenderer fontRenderer) {
        List<OtherFeatureCardLayout> layouts = new ArrayList<>();
        if (group == null || group.features == null || group.features.isEmpty()) {
            return layouts;
        }

        List<FeatureDef> visibleFeatures = new ArrayList<>();
        for (FeatureDef feature : group.features) {
            if (feature != null) {
                visibleFeatures.add(feature);
            }
        }
        if (visibleFeatures.isEmpty()) {
            return layouts;
        }

        int safeGap = Math.max(4, gap);
        int safeContentWidth = Math.max(120, contentWidth);
        int minCardWidth = 84;
        if (fontRenderer != null) {
            minCardWidth = Math.max(minCardWidth, fontRenderer.getStringWidth("GUI界面下移动") + 20);
        }

        int columnCount = Math.max(1, Math.min(4, (safeContentWidth + safeGap) / (minCardWidth + safeGap)));
        int buttonWidth = (safeContentWidth - safeGap * Math.max(0, columnCount - 1)) / columnCount;
        while (columnCount > 1 && buttonWidth < minCardWidth) {
            columnCount--;
            buttonWidth = (safeContentWidth - safeGap * Math.max(0, columnCount - 1)) / columnCount;
        }

        for (int i = 0; i < visibleFeatures.size(); i++) {
            int row = i / columnCount;
            int col = i % columnCount;
            int buttonX = startX + col * (buttonWidth + safeGap);
            int buttonY = startY + row * (buttonHeight + safeGap);
            layouts.add(new OtherFeatureCardLayout(visibleFeatures.get(i),
                    new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight)));
        }

        return layouts;
    }

    private static List<OtherFeatureCardLayout> buildOtherFeatureCardLayouts(GroupDef group, int startX, int startY,
            int contentWidth, int buttonHeight, int gap, net.minecraft.client.gui.Font fontRenderer) {
        return buildOtherFeatureCardLayouts(group, startX, startY, contentWidth, buttonHeight, gap,
                wrapFont(fontRenderer));
    }

    private static boolean handleOtherFeatureClick(FeatureDef feature, int mouseButton, Minecraft mc) throws IOException {
        if (feature == null) {
            return false;
        }

        if ("speed".equalsIgnoreCase(feature.id)) {
            if (mouseButton == 0) {
                SpeedHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.setScreen(new GuiSpeedConfig(null));
            }
            return true;
        }

        if (MovementFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                MovementFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen configScreen = MovementFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.setScreen(configScreen);
                }
            }
            return true;
        }

        if (BlockFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                BlockFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen configScreen = BlockFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.setScreen(configScreen);
                }
            }
            return true;
        }

        if (RenderFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                RenderFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen configScreen = RenderFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.setScreen(configScreen);
                }
            }
            return true;
        }

        if (WorldFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                WorldFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen configScreen = WorldFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.setScreen(configScreen);
                }
            }
            return true;
        }

        if (ItemFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                ItemFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen configScreen = ItemFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.setScreen(configScreen);
                }
            }
            return true;
        }

        if (MiscFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                MiscFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen configScreen = MiscFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.setScreen(configScreen);
                }
            }
            return true;
        }

        return false;
    }

    private static String getOtherFeaturesHudButtonLabel() {
        return "总状态HUD";
    }

    private static String getOtherFeaturesHudEditButtonLabel() {
        return "调整HUD位置";
    }

    private static int getOtherFeaturesHudButtonWidth(OverlayMetrics m, FontRenderer fontRenderer) {
        int textWidth = fontRenderer == null ? 0 : fontRenderer.getStringWidth(getOtherFeaturesHudButtonLabel());
        return Math.max(m.pathManagerButtonWidth, textWidth + scaleUi(18, m.scale));
    }

    private static int getOtherFeaturesHudButtonWidth(OverlayMetrics m, net.minecraft.client.gui.Font fontRenderer) {
        return getOtherFeaturesHudButtonWidth(m, wrapFont(fontRenderer));
    }

    private static int getOtherFeaturesHudEditButtonWidth(OverlayMetrics m, FontRenderer fontRenderer) {
        int textWidth = fontRenderer == null ? 0 : fontRenderer.getStringWidth(getOtherFeaturesHudEditButtonLabel());
        return Math.max(m.pathManagerButtonWidth, textWidth + scaleUi(18, m.scale));
    }

    private static int getOtherFeaturesHudEditButtonWidth(OverlayMetrics m,
            net.minecraft.client.gui.Font fontRenderer) {
        return getOtherFeaturesHudEditButtonWidth(m, wrapFont(fontRenderer));
    }

    private static String drawOtherFeaturesOverlay(OverlayMetrics m, int mouseX, int mouseY, FontRenderer fontRenderer,
            int screenWidth, int screenHeight) {
        List<GroupDef> groups = OtherFeatureGroupManager.getGroups();
        if (groups.isEmpty()) {
            drawCenteredString(fontRenderer, I18n.format("gui.inventory.other_features.empty"),
                    (m.contentPanelX + m.contentPanelRight) / 2, m.y + (m.totalHeight / 2), 0xFFBBBBBB);
            return null;
        }

        normalizeOtherFeatureGroupState(groups);

        int leftStartX = m.x + m.padding * 2;
        int topY = m.contentStartY + m.padding;
        int leftBtnW = getSafeCategoryListButtonWidth(m);
        int leftBtnH = Math.max(18, m.categoryButtonHeight - 2);
        int leftGap = 4;
        int leftListBottom = m.y + m.totalHeight - m.padding - 6;
        int leftListHeight = Math.max(leftBtnH, leftListBottom - topY);
        int visibleGroupCount = Math.max(1, (leftListHeight + leftGap) / (leftBtnH + leftGap));

        maxOtherFeatureGroupScroll = Math.max(0, groups.size() - visibleGroupCount);
        otherFeatureGroupScrollOffset = MathHelper.clamp(otherFeatureGroupScrollOffset, 0, maxOtherFeatureGroupScroll);

        for (int local = 0; local < visibleGroupCount; local++) {
            int index = otherFeatureGroupScrollOffset + local;
            if (index >= groups.size()) {
                break;
            }
            int by = topY + local * (leftBtnH + leftGap);
            if (by + leftBtnH > leftListBottom + 1) {
                break;
            }
            GroupDef group = groups.get(index);
            boolean selectedState = index == selectedOtherFeatureGroupIndex;
            boolean hover = mouseX >= leftStartX && mouseX < leftStartX + leftBtnW && mouseY >= by
                    && mouseY < by + leftBtnH;
            GuiTheme.drawButtonFrame(leftStartX, by, leftBtnW, leftBtnH,
                    selectedState ? GuiTheme.UiState.SELECTED
                            : (hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            drawCenteredString(fontRenderer, group.name, leftStartX + leftBtnW / 2,
                    by + (leftBtnH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        if (maxOtherFeatureGroupScroll > 0) {
            int sbX = leftStartX + leftBtnW + 2;
            int sbY = topY;
            int sbW = 4;
            int sbH = leftListHeight;
            int thumbH = Math.max(12, (int) ((float) visibleGroupCount / groups.size() * sbH));
            int thumbY = sbY + (int) ((otherFeatureGroupScrollOffset / (float) maxOtherFeatureGroupScroll) * (sbH - thumbH));
            drawRect(sbX, sbY, sbX + sbW, sbY + sbH, 0x66101010);
            drawRect(sbX, thumbY, sbX + sbW, thumbY + thumbH, 0xCC8A8A8A);
        }

        int contentX = m.contentPanelX + m.padding;
        int contentRight = m.contentPanelRight - m.padding;
        int contentTop = m.contentStartY + m.padding;
        int contentWidth = Math.max(1, contentRight - contentX);

        GroupDef selectedGroup = groups.get(selectedOtherFeatureGroupIndex);
        OtherFeaturePageLayout pageLayout = buildOtherFeaturePageLayout(selectedGroup, m, fontRenderer);
        otherFeatureScreenPage = pageLayout.currentPage;
        if (pageLayout.cards.isEmpty()) {
            drawCenteredString(fontRenderer, I18n.format("gui.inventory.other_features.category_empty"),
                    (contentX + contentRight) / 2, contentTop + 24, 0xFFBBBBBB);
        }

        String hoveredTooltip = null;
        for (OtherFeatureCardLayout card : pageLayout.cards) {
            if (card == null || card.feature == null) {
                continue;
            }

            boolean hover = card.bounds.contains(mouseX, mouseY);
            boolean enabled = isOtherFeatureEnabled(card.feature.id);
            GuiTheme.UiState state = getOtherFeatureItemState(card.feature.id, hover);
            GuiTheme.drawButtonFrameSafe(card.bounds.x, card.bounds.y, card.bounds.width, card.bounds.height, state);
            GuiTheme.drawCardHighlight(card.bounds.x, card.bounds.y, card.bounds.width, card.bounds.height, hover);
            int textColor = enabled ? GuiTheme.getStateTextColor(GuiTheme.UiState.SUCCESS)
                    : GuiTheme.getStateTextColor(hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, trimOverlayButtonLabel(fontRenderer, card.feature.name, card.bounds.width - 12),
                    card.bounds.x + card.bounds.width / 2,
                    card.bounds.y + (card.bounds.height - fontRenderer.FONT_HEIGHT) / 2, textColor);

            if (hover) {
                hoveredTooltip = buildOtherFeatureTooltip(card.feature, fontRenderer,
                        Math.min(220, Math.max(120, contentWidth - 40)));
            }
        }

        boolean canGoPrev = pageLayout.currentPage > 0;
        boolean canGoNext = pageLayout.currentPage < pageLayout.totalPages - 1;
        boolean hoverPrev = pageLayout.pageControls.prevButtonBounds.contains(mouseX, mouseY);
        boolean hoverNext = pageLayout.pageControls.nextButtonBounds.contains(mouseX, mouseY);
        drawRect(pageLayout.pageControls.containerBounds.x, pageLayout.pageControls.containerBounds.y,
                pageLayout.pageControls.containerBounds.x + pageLayout.pageControls.containerBounds.width,
                pageLayout.pageControls.containerBounds.y + pageLayout.pageControls.containerBounds.height, 0x66324458);
        drawHorizontalLine(pageLayout.pageControls.containerBounds.x,
                pageLayout.pageControls.containerBounds.x + pageLayout.pageControls.containerBounds.width,
                pageLayout.pageControls.containerBounds.y, 0xAA4FA6D9);
        drawHorizontalLine(pageLayout.pageControls.containerBounds.x,
                pageLayout.pageControls.containerBounds.x + pageLayout.pageControls.containerBounds.width,
                pageLayout.pageControls.containerBounds.y + pageLayout.pageControls.containerBounds.height, 0xAA35536C);
        GuiTheme.drawButtonFrame(pageLayout.pageControls.prevButtonBounds.x, pageLayout.pageControls.prevButtonBounds.y,
                pageLayout.pageControls.prevButtonBounds.width, pageLayout.pageControls.prevButtonBounds.height,
                canGoPrev ? (hoverPrev ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL) : GuiTheme.UiState.DISABLED);
        GuiTheme.drawButtonFrame(pageLayout.pageControls.nextButtonBounds.x, pageLayout.pageControls.nextButtonBounds.y,
                pageLayout.pageControls.nextButtonBounds.width, pageLayout.pageControls.nextButtonBounds.height,
                canGoNext ? (hoverNext ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL) : GuiTheme.UiState.DISABLED);
        drawCenteredString(fontRenderer,
                trimOverlayButtonLabel(fontRenderer, I18n.format("gui.inventory.prev_page"),
                        pageLayout.pageControls.prevButtonBounds.width - 8),
                pageLayout.pageControls.prevButtonBounds.x + pageLayout.pageControls.prevButtonBounds.width / 2,
                pageLayout.pageControls.prevButtonBounds.y
                        + (pageLayout.pageControls.prevButtonBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                canGoPrev ? 0xFFFFFFFF : 0xFF8E9AAC);
        drawCenteredString(fontRenderer,
                trimOverlayButtonLabel(fontRenderer, I18n.format("gui.inventory.next_page"),
                        pageLayout.pageControls.nextButtonBounds.width - 8),
                pageLayout.pageControls.nextButtonBounds.x + pageLayout.pageControls.nextButtonBounds.width / 2,
                pageLayout.pageControls.nextButtonBounds.y
                        + (pageLayout.pageControls.nextButtonBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                canGoNext ? 0xFFFFFFFF : 0xFF8E9AAC);
        drawCenteredString(fontRenderer, String.format("%d / %d", pageLayout.currentPage + 1, pageLayout.totalPages),
                pageLayout.pageControls.pageInfoBounds.x + pageLayout.pageControls.pageInfoBounds.width / 2,
                pageLayout.pageControls.pageInfoBounds.y
                        + (pageLayout.pageControls.pageInfoBounds.height - fontRenderer.FONT_HEIGHT) / 2,
                0xFFCFD9E6);

        return hoveredTooltip;
    }

    private static List<String> drawExchangeItemSlot(int x, int y, int size, ItemStack stack, int mouseX, int mouseY,
            FontRenderer fontRenderer) {
        GuiTheme.drawPanelSegment(x, y, size, size, x, y, size, size);
        Gui.drawRect(x + 1, y + 1, x + size - 1, y + size - 1, 0x55324458);
        if (stack == null || stack.isEmpty()) {
            drawCenteredString(fontRenderer, "-", x + size / 2, y + (size - fontRenderer.FONT_HEIGHT) / 2, 0xFFAAAAAA);
            return null;
        }

        RenderHelper.enableGUIStandardItemLighting();
        RenderHelper.disableStandardItemLighting();

        if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size
                && Minecraft.getInstance().player != null) {
            List<String> tooltips = stack.getTooltipLines(Minecraft.getInstance().player,
                    TooltipFlag.Default.ADVANCED).stream().map(net.minecraft.network.chat.Component::getString)
                            .collect(java.util.stream.Collectors.toList());
            return (tooltips != null && !tooltips.isEmpty()) ? tooltips : null;
        }

        return null;
    }

    private static void drawMerchantOverlay(OverlayMetrics m, int mouseX, int mouseY, FontRenderer fontRenderer,
            int screenWidth, int screenHeight) {
        int reloadBtnWidth = m.pathManagerButtonWidth;
        int reloadBtnHeight = m.topButtonHeight;
        int reloadBtnX = m.x + m.totalWidth - reloadBtnWidth - m.padding;
        int reloadBtnY = m.y + scaleUi(4, m.scale);

        boolean reloadHover = mouseX >= reloadBtnX && mouseX < reloadBtnX + reloadBtnWidth && mouseY >= reloadBtnY
                && mouseY < reloadBtnY + reloadBtnHeight;
        GuiTheme.drawButtonFrame(reloadBtnX, reloadBtnY, reloadBtnWidth, reloadBtnHeight,
                reloadHover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawCenteredString(fontRenderer, I18n.format("gui.inventory.merchant.reload"), reloadBtnX + reloadBtnWidth / 2,
                reloadBtnY + (reloadBtnHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);

        List<MerchantDef> merchants = MerchantExchangeManager.getMerchants();
        if (merchants.isEmpty()) {
            drawCenteredString(fontRenderer, I18n.format("gui.inventory.merchant.empty"),
                    (m.contentPanelX + m.contentPanelRight) / 2, m.y + (m.totalHeight / 2), 0xFFBBBBBB);
            return;
        }

        selectedMerchantIndex = MathHelper.clamp(selectedMerchantIndex, 0, merchants.size() - 1);
        MerchantDef selected = merchants.get(selectedMerchantIndex);
        normalizeMerchantCategoryState(selected);
        int totalPages = getMerchantTotalPages(selected);
        merchantScreenPage = MathHelper.clamp(merchantScreenPage, 0, totalPages - 1);

        int leftStartX = m.x + m.padding * 2;
        int topY = m.contentStartY + m.padding;
        int leftBtnW = m.categoryButtonWidth;
        int leftBtnH = Math.max(18, m.categoryButtonHeight - 2);
        int leftGap = 4;
        int pageAreaY = m.y + m.totalHeight - scaleUi(25, m.scale);
        int leftListBottom = pageAreaY - 6;
        int leftListHeight = Math.max(leftBtnH, leftListBottom - topY);
        int visibleMerchantCount = Math.max(1, (leftListHeight + leftGap) / (leftBtnH + leftGap));

        maxMerchantListScroll = Math.max(0, merchants.size() - visibleMerchantCount);
        merchantListScrollOffset = MathHelper.clamp(merchantListScrollOffset, 0, maxMerchantListScroll);

        for (int local = 0; local < visibleMerchantCount; local++) {
            int i = merchantListScrollOffset + local;
            if (i >= merchants.size()) {
                break;
            }
            int by = topY + local * (leftBtnH + leftGap);
            if (by + leftBtnH > leftListBottom + 1) {
                break;
            }
            MerchantDef merchant = merchants.get(i);
            boolean selectedState = i == selectedMerchantIndex;
            boolean hover = mouseX >= leftStartX && mouseX < leftStartX + leftBtnW && mouseY >= by
                    && mouseY < by + leftBtnH;
            GuiTheme.drawButtonFrame(leftStartX, by, leftBtnW, leftBtnH, selectedState ? GuiTheme.UiState.SELECTED
                    : (hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            drawCenteredString(fontRenderer, merchant.name, leftStartX + leftBtnW / 2,
                    by + (leftBtnH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        if (maxMerchantListScroll > 0) {
            int sbX = leftStartX + leftBtnW + 2;
            int sbY = topY;
            int sbW = 4;
            int sbH = leftListHeight;
            int thumbH = Math.max(12, (int) ((float) visibleMerchantCount / merchants.size() * sbH));
            int thumbY = sbY + (int) ((merchantListScrollOffset / (float) maxMerchantListScroll) * (sbH - thumbH));

            drawRect(sbX, sbY, sbX + sbW, sbY + sbH, 0x66101010);
            drawRect(sbX, thumbY, sbX + sbW, thumbY + thumbH, 0xCC8A8A8A);
        }

        int contentX = m.contentPanelX + m.padding;
        int contentY = m.contentStartY + m.padding;
        int rowH = 34;
        int slotSize = 18;
        int symbolGap = 6;
        int categoryBarY = contentY;
        int categoryBarH = Math.max(18, m.itemButtonHeight - 2);
        int rowStartY = contentY + categoryBarH + 4;
        int rowStart = merchantScreenPage * 4;

        List<ExchangeDef> exchanges = selected.exchanges == null ? Collections.emptyList() : selected.exchanges;
        List<Integer> visibleExchangeIndices = getMerchantVisibleExchangeIndices(selected);

        if (selected.categories != null && !selected.categories.isEmpty()) {
            int leftArrowW = 16;
            int leftArrowX = contentX;
            int leftArrowY = categoryBarY;
            int rightArrowW = 16;
            int rightArrowX = m.contentPanelRight - m.padding - rightArrowW;
            int rightArrowY = categoryBarY;
            int buttonX = leftArrowX + leftArrowW + 4;
            int buttonEndX = rightArrowX - 4;

            boolean canScrollLeft = merchantCategoryScrollOffset > 0;
            boolean canScrollRight = false;

            GuiTheme.drawButtonFrame(leftArrowX, leftArrowY, leftArrowW, categoryBarH,
                    canScrollLeft ? GuiTheme.UiState.NORMAL : GuiTheme.UiState.DANGER);
            drawCenteredString(fontRenderer, "<", leftArrowX + leftArrowW / 2,
                    leftArrowY + (categoryBarH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);

            for (int i = merchantCategoryScrollOffset; i < selected.categories.size(); i++) {
                CategoryDef category = selected.categories.get(i);
                String label = (category == null || category.name == null || category.name.trim().isEmpty())
                        ? I18n.format("gui.inventory.merchant.uncategorized")
                        : category.name;
                int btnW = getMerchantCategoryButtonWidth(fontRenderer, label);
                if (buttonX + btnW > buttonEndX) {
                    canScrollRight = true;
                    break;
                }

                GuiTheme.drawButtonFrame(buttonX, categoryBarY, btnW, categoryBarH,
                        i == selectedMerchantCategoryIndex ? GuiTheme.UiState.SELECTED : GuiTheme.UiState.NORMAL);
                drawCenteredString(fontRenderer, label, buttonX + btnW / 2,
                        categoryBarY + (categoryBarH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
                buttonX += btnW + 4;
            }

            GuiTheme.drawButtonFrame(rightArrowX, rightArrowY, rightArrowW, categoryBarH,
                    canScrollRight ? GuiTheme.UiState.NORMAL : GuiTheme.UiState.DANGER);
            drawCenteredString(fontRenderer, ">", rightArrowX + rightArrowW / 2,
                    rightArrowY + (categoryBarH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        List<String> hoveredItemTooltip = null;
        int hoveredTooltipMouseX = mouseX;
        int hoveredTooltipMouseY = mouseY;
        for (int row = 0; row < 4; row++) {
            int visibleIndex = rowStart + row;
            int y = rowStartY + row * rowH;

            Gui.drawRect(contentX, y, m.contentPanelRight - m.padding, y + rowH - 4, 0x332A3342);

            if (visibleIndex >= visibleExchangeIndices.size()) {
                continue;
            }

            int exchangeIndex = visibleExchangeIndices.get(visibleIndex);
            ExchangeDef ex = exchanges.get(exchangeIndex);
            int x1 = contentX + 8;
            int symbolStep = 18;
            int x2 = x1 + slotSize + symbolStep;
            int x3 = x2 + slotSize + symbolStep;
            int x4 = x3 + slotSize + symbolStep;
            int redeemW = 48;
            int redeemH = 18;
            int redeemX = m.contentPanelRight - m.padding - redeemW - 8;
            int redeemY = y + 7;

            if (hoveredItemTooltip == null) {
                hoveredItemTooltip = drawExchangeItemSlot(x1, y + 6, slotSize, ex.leftItem, mouseX, mouseY,
                        fontRenderer);
            } else {
                drawExchangeItemSlot(x1, y + 6, slotSize, ex.leftItem, mouseX, mouseY, fontRenderer);
            }
            drawCenteredString(fontRenderer, "+", x1 + slotSize + symbolGap, y + 10, 0xFFFFFFFF);
            if (hoveredItemTooltip == null) {
                hoveredItemTooltip = drawExchangeItemSlot(x2, y + 6, slotSize, ex.middleItem, mouseX, mouseY,
                        fontRenderer);
            } else {
                drawExchangeItemSlot(x2, y + 6, slotSize, ex.middleItem, mouseX, mouseY, fontRenderer);
            }
            drawCenteredString(fontRenderer, "+", x2 + slotSize + symbolGap, y + 10, 0xFFFFFFFF);
            if (hoveredItemTooltip == null) {
                hoveredItemTooltip = drawExchangeItemSlot(x3, y + 6, slotSize, ex.rightItem, mouseX, mouseY,
                        fontRenderer);
            } else {
                drawExchangeItemSlot(x3, y + 6, slotSize, ex.rightItem, mouseX, mouseY, fontRenderer);
            }
            drawCenteredString(fontRenderer, "=", x3 + slotSize + symbolGap, y + 10, 0xFFFFFFFF);
            if (hoveredItemTooltip == null) {
                hoveredItemTooltip = drawExchangeItemSlot(x4, y + 6, slotSize, ex.resultItem, mouseX, mouseY,
                        fontRenderer);
            } else {
                drawExchangeItemSlot(x4, y + 6, slotSize, ex.resultItem, mouseX, mouseY, fontRenderer);
            }

            boolean hoverRedeem = mouseX >= redeemX && mouseX < redeemX + redeemW && mouseY >= redeemY
                    && mouseY < redeemY + redeemH;
            GuiTheme.drawButtonFrame(redeemX, redeemY, redeemW, redeemH,
                    hoverRedeem ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
            drawCenteredString(fontRenderer, I18n.format("gui.inventory.merchant.redeem"), redeemX + redeemW / 2,
                    redeemY + (redeemH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        int pageButtonHeight = m.itemButtonHeight;
        int pageButtonWidth = m.pageButtonWidth;
        int pageInfoX = (m.contentPanelX + m.contentPanelRight) / 2;
        int prevButtonX = pageInfoX - pageButtonWidth - 28;
        int nextButtonX = pageInfoX + 28;

        boolean isHoveringPrev = mouseX >= prevButtonX && mouseX < prevButtonX + pageButtonWidth && mouseY >= pageAreaY
                && mouseY < pageAreaY + pageButtonHeight;
        GuiTheme.drawButtonFrame(prevButtonX, pageAreaY, pageButtonWidth, pageButtonHeight,
                isHoveringPrev ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawCenteredString(fontRenderer, I18n.format("gui.inventory.prev_page"), prevButtonX + pageButtonWidth / 2,
                pageAreaY + (pageButtonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);

        boolean isHoveringNext = mouseX >= nextButtonX && mouseX < nextButtonX + pageButtonWidth && mouseY >= pageAreaY
                && mouseY < pageAreaY + pageButtonHeight;
        GuiTheme.drawButtonFrame(nextButtonX, pageAreaY, pageButtonWidth, pageButtonHeight,
                isHoveringNext ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawCenteredString(fontRenderer, I18n.format("gui.inventory.next_page"), nextButtonX + pageButtonWidth / 2,
                pageAreaY + (pageButtonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);

        String pageInfo = String.format("%d / %d", merchantScreenPage + 1, totalPages);
        drawCenteredString(fontRenderer, pageInfo, pageInfoX,
                pageAreaY + (pageButtonHeight - fontRenderer.FONT_HEIGHT) / 2, 0xFFBBBBBB);

        if (hoveredItemTooltip != null && !hoveredItemTooltip.isEmpty()) {
            GuiUtils.drawHoveringText(hoveredItemTooltip, hoveredTooltipMouseX, hoveredTooltipMouseY, screenWidth,
                    screenHeight, -1, fontRenderer);
        }
    }

    public static void drawOverlay(int screenWidth, int screenHeight) {
        int mouseX = Mouse.getX() * screenWidth / Minecraft.getInstance().getWindow().getScreenWidth();
        int mouseY = Mouse.getY() * screenHeight / Minecraft.getInstance().getWindow().getScreenHeight();
        drawOverlay(screenWidth, screenHeight, mouseX, mouseY);
    }

    public static void drawOverlay(int screenWidth, int screenHeight, int mouseX, int mouseY) {
        if (masterStatusHudEditMode) {
            return;
        }

        String tooltipToDraw = null;
        int tooltipMouseX = 0;
        int tooltipMouseY = 0;

        try {
            colorChangeTicker++;

            // 每分钟检查一次远端更新；若有变化，顶部版本会实时刷新显示
            UpdateChecker.requestRefreshIfDue(60_000L);
            HallOfFameManager.requestRefreshIfDue(60_000L);
            TitleCompendiumManager.requestRefreshIfDue(60_000L);
            EnhancementAttrManager.requestRefreshIfDue(60_000L);
            AdExpListManager.requestRefreshIfDue(60_000L);
            DonationLeaderboardManager.requestRefreshIfDue(60_000L);

            FontRenderer fontRenderer = wrapFont(Minecraft.getInstance().font);
            String title = buildOverlayTitle();
            OverlayMetrics m = computeOverlayMetrics(screenWidth, screenHeight, fontRenderer, title);

            // --- 左侧功能按钮：增加背景与边框容器（背景使用主题图裁切） ---
            int sideButtonCount = sideButtons.size();
            int sidePanelX = m.x - m.sideButtonColumnWidth - m.gap;
            int sidePanelY = m.y;
            int sidePanelW = m.sideButtonColumnWidth;
            int sidePanelH = m.totalHeight;

            if (sideButtonCount > 0) {
                // 让左侧容器与主面板共享同一张背景采样区域，实现“无缝拼接”效果
                int stitchedGroupX = sidePanelX;
                int stitchedGroupY = m.y;
                int stitchedGroupW = (m.x + m.totalWidth) - sidePanelX;
                int stitchedGroupH = m.totalHeight;
                GuiTheme.drawPanelSegment(sidePanelX, sidePanelY, sidePanelW, sidePanelH, stitchedGroupX,
                        stitchedGroupY, stitchedGroupW, stitchedGroupH);

                for (GuiButton button : sideButtons) {
                    button.width = m.sideButtonWidth;
                    button.height = m.sideButtonHeight;
                    button.x = sidePanelX + (sidePanelW - button.width) / 2;
                }

                int topY = sidePanelY + m.padding;
                int bottomY = sidePanelY + sidePanelH - m.padding - m.sideButtonHeight;
                for (int i = 0; i < sideButtonCount; i++) {
                    GuiButton button = sideButtons.get(i);
                    if (sideButtonCount == 1) {
                        button.y = topY;
                    } else {
                        float t = (float) i / (float) (sideButtonCount - 1);
                        button.y = Math.round(topY + t * (bottomY - topY));
                    }
                    button.drawButton(Minecraft.getInstance(), mouseX, mouseY, 0);
                }
            }
            // --- 左侧功能按钮容器结束 ---

            int topInfoY = m.y - fontRenderer.FONT_HEIGHT - m.padding;
            String versionText = I18n.format("gui.inventory.version", zszlScriptMod.VERSION,
                    UpdateChecker.latestVersion);
            int versionX = m.x;

            boolean isHoveringVersion = mouseX >= versionX
                    && mouseX < versionX + fontRenderer.getStringWidth(versionText) && mouseY >= topInfoY
                    && mouseY < topInfoY + 10;
            String versionTextToDraw = isHoveringVersion ? "§n" + versionText : versionText;
            drawString(fontRenderer, versionTextToDraw, versionX, topInfoY, 0xFFFFFFFF);
            versionClickArea = new Rectangle(versionX, topInfoY, fontRenderer.getStringWidth(versionText), 10);

            if (isHoveringVersion) {
                tooltipToDraw = I18n.format("gui.inventory.tip.view_changelog");
                tooltipMouseX = mouseX;
                tooltipMouseY = mouseY;
            }

            String authorText = I18n.format("gui.inventory.author");
            StringBuilder rainbowText = new StringBuilder();
            int tickerOffset = colorChangeTicker / 3;

            for (int i = 0; i < authorText.length(); i++) {
                char character = authorText.charAt(i);
                ChatFormatting color = RAINBOW_COLORS.get((i + tickerOffset) % RAINBOW_COLORS.size());
                rainbowText.append(color).append(character);
            }

            int authorX = m.x + m.totalWidth - fontRenderer.getStringWidth(authorText);
            drawString(fontRenderer, rainbowText.toString(), authorX, topInfoY, 0xFFFFFFFF);
            authorClickArea = new Rectangle(authorX, topInfoY, fontRenderer.getStringWidth(authorText), 10);

            boolean isHoveringAuthor = authorClickArea.contains(mouseX, mouseY);
            if (isHoveringAuthor) {
                tooltipToDraw = I18n.format("gui.inventory.tip.click_copy");
                tooltipMouseX = mouseX;
                tooltipMouseY = mouseY;
            }

            // 主面板使用与左侧容器相同的拼接组，保证背景是同一整体被切开
            int stitchedGroupX = sideButtons.isEmpty() ? m.x : (m.x - m.sideButtonColumnWidth - m.gap);
            int stitchedGroupY = m.y;
            int stitchedGroupW = m.x + m.totalWidth - stitchedGroupX;
            int stitchedGroupH = m.totalHeight;
            GuiTheme.drawPanelSegment(m.x, m.y, m.totalWidth, m.totalHeight, stitchedGroupX, stitchedGroupY,
                    stitchedGroupW, stitchedGroupH);

            boolean customSearchHeaderActive = !merchantScreenActive && !otherFeaturesScreenActive
                    && isCustomCategorySelection();
            int topButtonCount = customSearchHeaderActive ? 5 : 4;
            int pathManagerButtonWidth = getTopButtonWidth(m, topButtonCount);
            int pathManagerButtonX = m.x + m.totalWidth - pathManagerButtonWidth - m.padding;
            int pathManagerButtonY = m.y + scaleUi(4, m.scale);

            int stopForegroundButtonWidth = pathManagerButtonWidth;
            int stopForegroundButtonX = pathManagerButtonX - stopForegroundButtonWidth - m.padding;
            int stopForegroundButtonY = pathManagerButtonY;
            int stopBackgroundButtonWidth = pathManagerButtonWidth;
            int stopBackgroundButtonX = stopForegroundButtonX - stopBackgroundButtonWidth - m.padding;
            int stopBackgroundButtonY = pathManagerButtonY;
            int otherFeaturesButtonWidth = pathManagerButtonWidth;
            int otherFeaturesButtonX = stopBackgroundButtonX - otherFeaturesButtonWidth - m.padding;
            int otherFeaturesButtonY = pathManagerButtonY;

            int titleAreaStartX = m.x + scaleUi(8, m.scale);
            int customSearchToggleX = customSearchHeaderActive ? (otherFeaturesButtonX - pathManagerButtonWidth - m.padding) : otherFeaturesButtonX;
            int titleAreaEndX = (merchantScreenActive || otherFeaturesScreenActive ? stopForegroundButtonX : customSearchToggleX)
                    - scaleUi(8, m.scale)
                    - getCustomSearchHeaderReservedWidth(fontRenderer, m.scale);
            int titleAreaWidth = Math.max(80, titleAreaEndX - titleAreaStartX);
            List<String> headerLines = buildOverlayHeaderLines();
            int lineHeight = fontRenderer.FONT_HEIGHT + scaleUi(2, m.scale);
            int topBarHeight = merchantScreenActive || otherFeaturesScreenActive
                    ? Math.max(scaleUi(24, m.scale), headerLines.size() * lineHeight)
                    : Math.max(scaleUi(30, m.scale), lineHeight * 2);
            int contentStartY = m.y + topBarHeight + scaleUi(6, m.scale);

            int currentTitleY = m.y + scaleUi(8, m.scale);
            if (merchantScreenActive || otherFeaturesScreenActive) {
                int titleCenterX = titleAreaStartX + titleAreaWidth / 2;
                for (String line : headerLines) {
                    drawCenteredString(fontRenderer, line, titleCenterX, currentTitleY, 0xFFFFFFFF);
                    currentTitleY += lineHeight;
                }
            } else {
                int infoX = titleAreaStartX;
                if (!headerLines.isEmpty()) {
                    drawString(fontRenderer, fontRenderer.trimStringToWidth(headerLines.get(0), titleAreaWidth), infoX,
                            currentTitleY, 0xFFFFFFFF);
                }
                if (headerLines.size() > 1) {
                    drawString(fontRenderer, fontRenderer.trimStringToWidth(headerLines.get(1), titleAreaWidth), infoX,
                            currentTitleY + lineHeight, 0xFFB8C7D9);
                }
            }

            if (merchantScreenActive) {
                int backButtonWidth = m.pathManagerButtonWidth;
                int backButtonX = m.x + m.padding;
                int backButtonY = m.y + scaleUi(4, m.scale);
                boolean isHoveringBack = mouseX >= backButtonX && mouseX < backButtonX + backButtonWidth
                        && mouseY >= backButtonY && mouseY < backButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(backButtonX, backButtonY, backButtonWidth, m.topButtonHeight,
                        isHoveringBack ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawCenteredString(Minecraft.getInstance().font, I18n.format("gui.inventory.merchant.back"),
                        backButtonX + backButtonWidth / 2,
                        backButtonY + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);

                int categoryPanelX = m.x + m.padding;
                int categoryPanelY = contentStartY;
                int categoryPanelWidth = m.categoryPanelWidth;
                int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;
                drawRect(categoryPanelX, categoryPanelY, categoryPanelX + categoryPanelWidth,
                        categoryPanelY + categoryPanelHeight, 0x66324458);

                drawRect(m.contentPanelX, contentStartY, m.contentPanelRight,
                        m.y + m.totalHeight - scaleUi(30, m.scale), 0x66324458);

                drawMerchantOverlay(m, mouseX, mouseY, fontRenderer, screenWidth, screenHeight);
            } else if (otherFeaturesScreenActive) {
                int backButtonWidth = m.pathManagerButtonWidth;
                int backButtonX = m.x + m.padding;
                int backButtonY = m.y + scaleUi(4, m.scale);
                boolean isHoveringBack = mouseX >= backButtonX && mouseX < backButtonX + backButtonWidth
                        && mouseY >= backButtonY && mouseY < backButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(backButtonX, backButtonY, backButtonWidth, m.topButtonHeight,
                        isHoveringBack ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawCenteredString(Minecraft.getInstance().font,
                        I18n.format("gui.inventory.other_features.back"),
                        backButtonX + backButtonWidth / 2,
                        backButtonY + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);

                int masterHudButtonWidth = getOtherFeaturesHudButtonWidth(m, Minecraft.getInstance().font);
                int masterHudButtonX = m.x + m.totalWidth - masterHudButtonWidth - m.padding;
                int masterHudButtonY = backButtonY;
                int editHudButtonWidth = getOtherFeaturesHudEditButtonWidth(m, Minecraft.getInstance().font);
                int editHudButtonX = masterHudButtonX - editHudButtonWidth - m.padding;
                int editHudButtonY = backButtonY;
                boolean isHoveringEditHud = mouseX >= editHudButtonX && mouseX < editHudButtonX + editHudButtonWidth
                        && mouseY >= editHudButtonY && mouseY < editHudButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(editHudButtonX, editHudButtonY, editHudButtonWidth, m.topButtonHeight,
                        masterStatusHudEditMode ? GuiTheme.UiState.SELECTED
                                : (isHoveringEditHud ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
                drawCenteredString(Minecraft.getInstance().font, getOtherFeaturesHudEditButtonLabel(),
                        editHudButtonX + editHudButtonWidth / 2,
                        editHudButtonY + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);
                boolean masterHudEnabled = MovementFeatureManager.isMasterStatusHudEnabled();
                boolean isHoveringMasterHud = mouseX >= masterHudButtonX && mouseX < masterHudButtonX + masterHudButtonWidth
                        && mouseY >= masterHudButtonY && mouseY < masterHudButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(masterHudButtonX, masterHudButtonY, masterHudButtonWidth, m.topButtonHeight,
                        masterHudEnabled ? GuiTheme.UiState.SUCCESS
                                : (isHoveringMasterHud ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
                drawCenteredString(Minecraft.getInstance().font, getOtherFeaturesHudButtonLabel(),
                        masterHudButtonX + masterHudButtonWidth / 2,
                        masterHudButtonY
                                + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);
                if (isHoveringMasterHud) {
                    tooltipToDraw = masterHudEnabled
                            ? "§a总状态HUD已开启§7：所有移动相关状态 HUD 允许显示。"
                            : "§c总状态HUD已关闭§7：会统一隐藏加速和移动功能的状态 HUD。";
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                } else if (isHoveringEditHud) {
                    tooltipToDraw = masterStatusHudEditMode
                            ? "§bHUD位置编辑中§7：拖动预览区域可调整位置，点返回或预览中的“退出编辑”结束。"
                            : "§7点击后显示 HUD 预览并进入拖动编辑模式。";
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                }

                int categoryPanelX = m.x + m.padding;
                int categoryPanelY = contentStartY;
                int categoryPanelWidth = m.categoryPanelWidth;
                int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;
                drawRect(categoryPanelX, categoryPanelY, categoryPanelX + categoryPanelWidth,
                        categoryPanelY + categoryPanelHeight, 0x66324458);

                drawRect(m.contentPanelX, contentStartY, m.contentPanelRight,
                        m.y + m.totalHeight - scaleUi(30, m.scale), 0x66324458);

                String otherFeatureTooltip = drawOtherFeaturesOverlay(m, mouseX, mouseY, fontRenderer, screenWidth,
                        screenHeight);
                if (otherFeatureTooltip != null && !otherFeatureTooltip.isEmpty()) {
                    tooltipToDraw = otherFeatureTooltip;
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                }
            } else {

                boolean isHoveringPathManager = mouseX >= pathManagerButtonX
                        && mouseX < pathManagerButtonX + pathManagerButtonWidth && mouseY >= pathManagerButtonY
                        && mouseY < pathManagerButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(pathManagerButtonX, pathManagerButtonY, pathManagerButtonWidth,
                        m.topButtonHeight, isHoveringPathManager ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawCenteredString(Minecraft.getInstance().font, I18n.format("gui.inventory.path_manager"),
                        pathManagerButtonX + pathManagerButtonWidth / 2,
                        pathManagerButtonY
                                + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);
                if (isHoveringPathManager) {
                    tooltipToDraw = I18n.format("gui.inventory.tip.path_manager");
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                }

                boolean isHoveringOtherFeatures = mouseX >= otherFeaturesButtonX
                        && mouseX < otherFeaturesButtonX + otherFeaturesButtonWidth && mouseY >= otherFeaturesButtonY
                        && mouseY < otherFeaturesButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(otherFeaturesButtonX, otherFeaturesButtonY, otherFeaturesButtonWidth,
                        m.topButtonHeight, isHoveringOtherFeatures ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                drawCenteredString(Minecraft.getInstance().font, I18n.format("gui.inventory.other_features"),
                        otherFeaturesButtonX + otherFeaturesButtonWidth / 2,
                        otherFeaturesButtonY + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight)
                                / 2,
                        0xFFFFFFFF);
                if (isHoveringOtherFeatures) {
                    tooltipToDraw = I18n.format("gui.inventory.tip.other_features");
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                }

                boolean isHoveringStopForeground = mouseX >= stopForegroundButtonX
                        && mouseX < stopForegroundButtonX + stopForegroundButtonWidth
                        && mouseY >= stopForegroundButtonY && mouseY < stopForegroundButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(stopForegroundButtonX, stopForegroundButtonY, stopForegroundButtonWidth,
                        m.topButtonHeight, isHoveringStopForeground ? GuiTheme.UiState.HOVER : GuiTheme.UiState.DANGER);
                drawCenteredString(Minecraft.getInstance().font,
                        I18n.format("gui.inventory.stop_foreground"),
                        stopForegroundButtonX + stopForegroundButtonWidth / 2,
                        stopForegroundButtonY
                                + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);
                if (isHoveringStopForeground) {
                    tooltipToDraw = I18n.format("gui.inventory.tip.stop_foreground");
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                }

                boolean isHoveringStopBackground = mouseX >= stopBackgroundButtonX
                        && mouseX < stopBackgroundButtonX + stopBackgroundButtonWidth
                        && mouseY >= stopBackgroundButtonY && mouseY < stopBackgroundButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(stopBackgroundButtonX, stopBackgroundButtonY, stopBackgroundButtonWidth,
                        m.topButtonHeight, isHoveringStopBackground ? GuiTheme.UiState.HOVER : GuiTheme.UiState.DANGER);
                drawCenteredString(Minecraft.getInstance().font,
                        I18n.format("gui.inventory.stop_background"),
                        stopBackgroundButtonX + stopBackgroundButtonWidth / 2,
                        stopBackgroundButtonY
                                + (m.topButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                        0xFFFFFFFF);
                if (isHoveringStopBackground) {
                    tooltipToDraw = I18n.format("gui.inventory.tip.stop_background");
                    tooltipMouseX = mouseX;
                    tooltipMouseY = mouseY;
                }

                int categoryPanelX = m.x + m.padding;
                int categoryPanelY = contentStartY;
                int categoryPanelWidth = m.categoryPanelWidth;
                int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;
                MainPageControlBounds pageControls = shouldShowMainPageControls() ? getMainPageControlBounds(m) : null;
                int contentPanelBottom = shouldShowMainPageControls()
                        ? Math.max(contentStartY + scaleUi(24, m.scale), pageControls.containerBounds.y - scaleUi(6, m.scale))
                        : m.y + m.totalHeight - scaleUi(30, m.scale);
                drawRect(categoryPanelX, categoryPanelY, categoryPanelX + categoryPanelWidth,
                        categoryPanelY + categoryPanelHeight, 0x66324458);
                drawCategoryTree(m, contentStartY, mouseX, mouseY);

                drawRect(m.contentPanelX, contentStartY, m.contentPanelRight, contentPanelBottom, 0x66324458);

                if (isDebugRecordingMenuVisible
                        && I18n.format("gui.inventory.category.debug").equals(currentCategory)) {
                    int itemAreaStartX = m.contentPanelX + m.padding;
                    int itemAreaStartY = contentStartY + m.padding;
                    int itemButtonWidth = m.itemButtonWidth;
                    int itemButtonHeight = m.itemButtonHeight;

                    int buttonX1 = itemAreaStartX + 10;
                    int buttonY1 = itemAreaStartY + 10;
                    boolean isHoveringRecord = mouseX >= buttonX1 && mouseX < buttonX1 + itemButtonWidth
                            && mouseY >= buttonY1 && mouseY < buttonY1 + itemButtonHeight;
                    GuiTheme.drawButtonFrame(buttonX1, buttonY1, itemButtonWidth, itemButtonHeight,
                            isHoveringRecord ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                    drawCenteredString(Minecraft.getInstance().font,
                            I18n.format("gui.inventory.debug.record_chest"), buttonX1 + itemButtonWidth / 2,
                            buttonY1 + (itemButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                            0xFFFFFFFF);

                    if (isHoveringRecord) {
                        String tooltip = I18n.format("gui.inventory.debug.tip.record_chest");
                        tooltipToDraw = tooltip;
                        tooltipMouseX = mouseX;
                        tooltipMouseY = mouseY;
                    }

                    int buttonX2 = buttonX1 + itemButtonWidth + m.gap;
                    int buttonY2 = itemAreaStartY + 10;
                    boolean isHoveringEquip = mouseX >= buttonX2 && mouseX < buttonX2 + itemButtonWidth
                            && mouseY >= buttonY2 && mouseY < buttonY2 + itemButtonHeight;

                    GuiTheme.UiState equipState = AutoEquipHandler.enabled ? GuiTheme.UiState.SUCCESS
                            : (isHoveringEquip ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                    GuiTheme.drawButtonFrame(buttonX2, buttonY2, itemButtonWidth, itemButtonHeight, equipState);
                    drawCenteredString(Minecraft.getInstance().font,
                            I18n.format("gui.inventory.debug.auto_equip"), buttonX2 + itemButtonWidth / 2,
                            buttonY2 + (itemButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                            0xFFFFFFFF);

                    if (isHoveringEquip) {
                        String tooltip = I18n.format("gui.inventory.debug.tip.auto_equip");
                        tooltipToDraw = tooltip;
                        tooltipMouseX = mouseX;
                        tooltipMouseY = mouseY;
                    }

                    int buttonX3 = buttonX2 + itemButtonWidth + m.gap;
                    int buttonY3 = itemAreaStartY + 10;
                    boolean isHoveringDebugKeys = mouseX >= buttonX3 && mouseX < buttonX3 + itemButtonWidth
                            && mouseY >= buttonY3 && mouseY < buttonY3 + itemButtonHeight;
                    GuiTheme.drawButtonFrame(buttonX3, buttonY3, itemButtonWidth, itemButtonHeight,
                            isHoveringDebugKeys ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                    drawCenteredString(Minecraft.getInstance().font,
                            I18n.format("gui.inventory.debug.debug_keybind"), buttonX3 + itemButtonWidth / 2,
                            buttonY3 + (itemButtonHeight - Minecraft.getInstance().font.lineHeight) / 2,
                            0xFFFFFFFF);

                    if (isHoveringDebugKeys) {
                        String tooltip = I18n.format("gui.inventory.debug.tip.debug_keybind");
                        tooltipToDraw = tooltip;
                        tooltipMouseX = mouseX;
                        tooltipMouseY = mouseY;
                    }

                } else {
                    if (isCommonCategory(currentCategory)) {
                        String commonTooltip = drawGroupedCommonItems(m, contentStartY, mouseX, mouseY, fontRenderer);
                        if (commonTooltip != null && !commonTooltip.isEmpty()) {
                            tooltipToDraw = commonTooltip;
                            tooltipMouseX = mouseX;
                            tooltipMouseY = mouseY;
                        }
                    } else if (isCustomCategorySelection()) {
                        String customTooltip = drawCustomSequenceCards(m, contentStartY, mouseX, mouseY, fontRenderer);
                        if (customTooltip != null && !customTooltip.isEmpty()) {
                            tooltipToDraw = customTooltip;
                            tooltipMouseX = mouseX;
                            tooltipMouseY = mouseY;
                        } else if (customSearchToggleButtonBounds != null
                                && customSearchToggleButtonBounds.contains(mouseX, mouseY)) {
                            tooltipToDraw = isCustomSearchExpanded()
                                    ? (isBlank(customSequenceSearchQuery) ? "收起搜索" : "收起搜索（保留当前筛选）")
                                    : (isBlank(customSequenceSearchQuery) ? "展开搜索（Ctrl+F）" : "展开搜索（当前筛选仍生效）");
                            tooltipMouseX = mouseX;
                            tooltipMouseY = mouseY;
                        } else if (findCustomSearchScopeButtonAt(mouseX, mouseY) != null) {
                            tooltipToDraw = "切换搜索范围: " + getCustomSearchScopeLabel(getEffectiveCustomSearchScope());
                            tooltipMouseX = mouseX;
                            tooltipMouseY = mouseY;
                        } else if (customSearchClearButtonBounds != null
                                && customSearchClearButtonBounds.contains(mouseX, mouseY)) {
                            tooltipToDraw = "清空搜索";
                            tooltipMouseX = mouseX;
                            tooltipMouseY = mouseY;
                        }
                    } else {
                        List<String> items = categoryItems.get(currentCategory);
                        List<String> itemNames = categoryItemNames.get(currentCategory);
                        int itemAreaStartX = m.contentPanelX + m.padding;
                        int itemAreaStartY = contentStartY + m.padding;
                        int itemButtonWidth = m.itemButtonWidth;
                        int itemButtonHeight = m.itemButtonHeight;
                        int itemsPerRow = 3;
                        for (int i = 0; i < 18; i++) {
                            int index = currentPage * 18 + i;
                            if (items == null || index >= items.size())
                                break;

                            int col = i % itemsPerRow;
                            int row = i / itemsPerRow;
                            int buttonX = itemAreaStartX + col * (itemButtonWidth + m.gap);
                            int buttonY = itemAreaStartY + row * (itemButtonHeight + m.gap);
                            boolean isHoveringItem = mouseX >= buttonX && mouseX < buttonX + itemButtonWidth
                                    && mouseY >= buttonY && mouseY < buttonY + itemButtonHeight;
                            int bgColor = isHoveringItem ? 0xFF666666 : 0xFF444444;
                            String command = items.get(index);

                            if (command.equals("autoeat") && AutoEatHandler.autoEatEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_auto_fishing")) {
                                if (AutoFishingHandler.enabled) {
                                    bgColor = 0xFF33AA33;
                                    itemNames.set(index, I18n.format("gui.inventory.auto_fishing.on"));
                                } else {
                                    itemNames.set(index, I18n.format("gui.inventory.item.auto_fishing.name"));
                                }
                            } else if (command.equals("toggle_fast_attack")
                                    && FreecamHandler.INSTANCE.isFastAttackEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_kill_aura")) {
                                if (KillAuraHandler.enabled) {
                                    bgColor = 0xFF33AA33;
                                    itemNames.set(index, I18n.format("gui.inventory.kill_aura.on"));
                                } else {
                                    itemNames.set(index, I18n.format("gui.inventory.item.kill_aura.name"));
                                }
                            } else if (command.equals("warehouse_manager") && WarehouseEventHandler.oneClickDepositMode)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("autoskill") && AutoSkillHandler.autoSkillEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("signin_online_rewards") && AutoSigninOnlineHandler.enabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_mouse_detach") && ModConfig.isMouseDetached)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("followconfig") && AutoFollowHandler.getActiveRule() != null)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("conditional_execution")
                                    && ConditionalExecutionHandler.isGloballyEnabled())
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_auto_pickup") && AutoPickupHandler.globalEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_server_feature_visibility")
                                    && ServerFeatureVisibilityManager.isAnyRuleEnabled())
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_auto_use_item") && AutoUseItemHandler.globalEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("toggle_death_auto_rejoin")) {
                                if (DeathAutoRejoinHandler.deathAutoRejoinEnabled) {
                                    bgColor = 0xFF33AA33;
                                    itemNames.set(index, I18n.format("gui.inventory.death_auto_rejoin.on"));
                                } else {
                                    itemNames.set(index, I18n.format("gui.inventory.item.death_auto_rejoin.name"));
                                }
                            } else if (command.equals("toggle_kill_timer")) {
                                boolean timer = KillTimerHandler.isEnabled;
                                if (timer) {
                                    bgColor = 0xFF33AA33; // 绿色: 只开计时
                                    itemNames.set(index, I18n.format("gui.inventory.kill_timer.on"));
                                } else {
                                    // 默认灰色
                                    itemNames.set(index, I18n.format("gui.inventory.kill_timer.name"));
                                }
                            } else if (command.equals("toggle_ad_exp_panel")) {
                                boolean panel = AdExpPanelHandler.enabled;
                                if (panel) {
                                    bgColor = 0xFF33AA33;
                                    itemNames.set(index, I18n.format("gui.inventory.ad_exp_panel.on"));
                                } else {
                                    itemNames.set(index, I18n.format("gui.inventory.ad_exp_panel.name"));
                                }
                            } else if (command.equals("toggle_shulker_rebound_fix")) {
                                boolean fix = ShulkerMiningReboundFixHandler.enabled;
                                if (fix) {
                                    bgColor = 0xFF33AA33;
                                    itemNames.set(index, I18n.format("gui.inventory.shulker_rebound_fix.on"));
                                } else {
                                    itemNames.set(index, I18n.format("gui.inventory.shulker_rebound_fix.name"));
                                }
                            } else if (command.equals("toggle_kill_timer") && KillTimerHandler.isEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.equals("debug_settings") && ModConfig.isDebugModeEnabled)
                                bgColor = 0xFF00AA00;
                            else if (command.equals("toggle_auto_stack_shulker_boxes")
                                    && ShulkerBoxStackingHandler.autoStackingEnabled)
                                bgColor = 0xFF33AA33;
                            else if (command.startsWith("path:") || command.startsWith("custom_path:")) {
                                String sequenceName = command.substring(command.indexOf(":") + 1);
                                if (PathSequenceEventListener.instance.isTracking()
                                        && PathSequenceEventListener.instance.currentSequence != null
                                        && PathSequenceEventListener.instance.currentSequence.getName()
                                                .equals(sequenceName)) {
                                    bgColor = 0xFF0066AA;
                                }
                            }

                            GuiTheme.UiState itemState = GuiTheme.UiState.NORMAL;
                            if (bgColor == 0xFF33AA33 || bgColor == 0xFF00AA00) {
                                itemState = GuiTheme.UiState.SUCCESS;
                            } else if (bgColor == 0xFF0066AA) {
                                itemState = GuiTheme.UiState.SELECTED;
                            } else if (isHoveringItem) {
                                itemState = GuiTheme.UiState.HOVER;
                            }
                            GuiTheme.drawButtonFrame(buttonX, buttonY, itemButtonWidth, itemButtonHeight, itemState);
                            GuiTheme.drawCardHighlight(buttonX, buttonY, itemButtonWidth, itemButtonHeight,
                                    isHoveringItem);

                            if (isHoveringItem) {
                                String tooltip = itemTooltips.get(command);
                                if (tooltip != null && !tooltip.isEmpty()) {
                                    tooltipToDraw = tooltip;
                                    tooltipMouseX = mouseX;
                                    tooltipMouseY = mouseY;
                                }
                            }

                            if (command.equals("setloop")) {
                                drawCenteredString(Minecraft.getInstance().font, itemNames.get(index),
                                        buttonX + itemButtonWidth / 2, buttonY + 2, 0xFFFFFFFF);
                                String loopText = (loopCount < 0) ? I18n.format("gui.inventory.loop.infinite")
                                        : (loopCount == 0) ? I18n.format("gui.inventory.loop.off")
                                                : I18n.format("gui.inventory.loop.count", loopCount);
                                drawCenteredString(Minecraft.getInstance().font, loopText,
                                        buttonX + itemButtonWidth / 2, buttonY + 11, 0xFFDDDDDD);
                            } else {
                                drawCenteredString(Minecraft.getInstance().font, itemNames.get(index),
                                        buttonX + itemButtonWidth / 2,
                                        buttonY + (itemButtonHeight - Minecraft.getInstance().font.lineHeight)
                                                / 2,
                                        0xFFFFFFFF);
                            }

                            if (command.startsWith("custom_path:")) {
                                int deleteX = buttonX + itemButtonWidth - 10;
                                int deleteY = buttonY;
                                boolean isHoveringDelete = mouseX >= deleteX && mouseX < deleteX + 10
                                        && mouseY >= deleteY && mouseY < deleteY + 10;
                                int deleteColor = isHoveringDelete ? 0xFFFF5555 : 0xFFCC0000;
                                drawRect(deleteX, deleteY, deleteX + 10, deleteY + 10, deleteColor);
                                drawString(Minecraft.getInstance().font, "§fX", deleteX + 2, deleteY + 1,
                                        0xFFFFFFFF);
                            }
                        }
                    }

                    String pageControlsTooltip = drawMainPageControls(m, contentStartY, mouseX, mouseY, fontRenderer);
                    if (pageControlsTooltip != null && !pageControlsTooltip.isEmpty()) {
                        tooltipToDraw = pageControlsTooltip;
                        tooltipMouseX = mouseX;
                        tooltipMouseY = mouseY;
                    }

                }
            }

            if (tooltipToDraw != null) {
                String normalizedTooltip = tooltipToDraw.replace("\\n", "\n");
                GuiUtils.drawHoveringText(Arrays.asList(normalizedTooltip.split("\n")), tooltipMouseX, tooltipMouseY,
                        screenWidth, screenHeight, -1, fontRenderer);
            }

            drawContextMenus(mouseX, mouseY, screenWidth, screenHeight, fontRenderer);
            drawCustomSequenceDragGhost(mouseX, mouseY, fontRenderer);
            drawCategoryTreeDragGhost(mouseX, mouseY, fontRenderer);

        } finally {
        }
    }

    public static void handleMouseClick(int rawMouseX, int rawMouseY, int mouseButton) throws IOException {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getInstance());
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();
        Minecraft mc = Minecraft.getInstance();

        int mouseX = scaleRawMouseX(rawMouseX, screenWidth);
        int mouseY = scaleRawMouseY(rawMouseY, screenHeight);

        if (contextMenuVisible && handleContextMenuClick(mouseX, mouseY, mouseButton)) {
            return;
        }

        updateButtonPositions(screenWidth, screenHeight);
        OverlayMetrics m = getCurrentOverlayMetrics(screenWidth, screenHeight);

        if (masterStatusHudEditMode) {
            if (mouseButton == 0 && masterStatusHudExitButtonBounds != null
                    && masterStatusHudExitButtonBounds.contains(mouseX, mouseY)) {
                setMasterStatusHudEditMode(false);
            } else if (mouseButton == 0 && masterStatusHudEditorBounds != null
                    && masterStatusHudEditorBounds.contains(mouseX, mouseY)) {
                isDraggingMasterStatusHud = true;
                masterStatusHudDragOffsetX = mouseX - masterStatusHudEditorBounds.x;
                masterStatusHudDragOffsetY = mouseY - masterStatusHudEditorBounds.y;
            }
            return;
        }

        if (mouseButton == 0 && categoryDividerBounds != null && categoryDividerBounds.contains(mouseX, mouseY)) {
            isDraggingCategoryDivider = true;
            categoryDividerMouseOffsetX = mouseX - categoryDividerBounds.x;
            return;
        }

        if (versionClickArea != null && versionClickArea.contains(mouseX, mouseY)) {
            UpdateChecker.forceRefresh();
            zszlScriptMod.isGuiVisible = false;
            mc.setScreen(new GuiChangelog(null, UpdateChecker.changelogContent));
            return;
        }

        if (authorClickArea != null && authorClickArea.contains(mouseX, mouseY)) {
            ExternalLinkOpener.OpenResult result = ExternalLinkOpener.copyAndOpen(
                    "https://qm.qq.com/cgi-bin/qm/qr?k=KpXtB7PNkQYan3sAx-eO4_wa8x9BIRhF&jump_from=webapi&authKey=X/HJE1j5AIGgsOP4zT/8r1SsTD6ptqo4A9/PmbJeeWd3lBolMoNWpCuDHyzxrQTj",
                    "qq-group");
            if (mc.player != null) {
                mc.player.sendSystemMessage(new TextComponentString(
                        result.opened()
                                ? I18n.format("msg.qq_group.opening")
                                : I18n.format("msg.qq_group.manual")));
            }
            return;
        }

        for (GuiButton button : sideButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                switch (button.id) {
                case BTN_ID_THEME_CONFIG:
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(new GuiThemeManager(null));
                    break;
                case BTN_ID_UPDATE:
                    UpdateManager.fetchUpdateLinkAndOpen();
                    break;
                case BTN_ID_HALL_OF_FAME:
                    HallOfFameManager.forceRefresh();
                    UpdateChecker.forceRefresh();
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(new GuiHallOfFame(null, HallOfFameManager.content));
                    break;
                case BTN_ID_TITLE_COMPENDIUM:
                    TitleCompendiumManager.forceRefresh();
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(GuiHallOfFame.createTitleCompendiumView(null));
                    break;
                case BTN_ID_ENHANCEMENT_ATTR:
                    EnhancementAttrManager.forceRefresh();
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(GuiHallOfFame.createEnhancementAttrView(null));
                    break;
                case BTN_ID_AD_EXP_LIST:
                    AdExpListManager.forceRefresh();
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(GuiHallOfFame.createAdExpListView(null));
                    break;
                case BTN_ID_MERCHANT:
                    merchantScreenActive = true;
                    otherFeaturesScreenActive = false;
                    isDraggingMerchantListScrollbar = false;
                    MerchantExchangeManager.reload();
                    break;
                case BTN_ID_DONATE:
                    DonationLeaderboardManager.forceRefresh();
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(new GuiDonationSupport(null));
                    break;
                case BTN_ID_PERFORMANCE_MONITOR:
                    zszlScriptMod.isGuiVisible = false;
                    mc.setScreen(new GuiPerformanceMonitor());
                    break;
                }
                return;
            }
        }

        int x = m.x;
        int y = m.y;
        int totalWidth = m.totalWidth;
        int height = m.totalHeight;

        boolean customSearchHeaderActive = !merchantScreenActive && !otherFeaturesScreenActive
                && isCustomCategorySelection();
        int topButtonCount = customSearchHeaderActive ? 5 : 4;
        int pathManagerButtonWidth = getTopButtonWidth(m, topButtonCount);
        int pathManagerButtonX = x + totalWidth - pathManagerButtonWidth - m.padding;
        int pathManagerButtonY = y + scaleUi(4, m.scale);

        int stopForegroundButtonWidth = pathManagerButtonWidth;
        int stopForegroundButtonX = pathManagerButtonX - stopForegroundButtonWidth - m.padding;
        int stopForegroundButtonY = pathManagerButtonY;
        int stopBackgroundButtonWidth = pathManagerButtonWidth;
        int stopBackgroundButtonX = stopForegroundButtonX - stopBackgroundButtonWidth - m.padding;
        int stopBackgroundButtonY = pathManagerButtonY;
        int otherFeaturesButtonWidth = pathManagerButtonWidth;
        int otherFeaturesButtonX = stopBackgroundButtonX - otherFeaturesButtonWidth - m.padding;
        int otherFeaturesButtonY = pathManagerButtonY;

        if (merchantScreenActive) {
            int backButtonWidth = m.pathManagerButtonWidth;
            int backButtonX = x + m.padding;
            int backButtonY = y + scaleUi(4, m.scale);
            int reloadButtonWidth = m.pathManagerButtonWidth;
            int reloadButtonX = x + totalWidth - reloadButtonWidth - m.padding;
            int reloadButtonY = backButtonY;

            if (isMouseOver(mouseX, mouseY, backButtonX, backButtonY, backButtonWidth, m.topButtonHeight)) {
                merchantScreenActive = false;
                return;
            }

            if (isMouseOver(mouseX, mouseY, reloadButtonX, reloadButtonY, reloadButtonWidth, m.topButtonHeight)) {
                MerchantExchangeManager.reload();
                merchantScreenPage = 0;
                selectedMerchantIndex = 0;
                selectedMerchantCategoryIndex = -1;
                merchantCategoryScrollOffset = 0;
                merchantListScrollOffset = 0;
                maxMerchantListScroll = 0;
                isDraggingMerchantListScrollbar = false;
                return;
            }

            List<MerchantDef> merchants = MerchantExchangeManager.getMerchants();
            if (!merchants.isEmpty()) {
                int merchantButtonWidth = m.categoryButtonWidth;
                int merchantButtonHeight = Math.max(18, m.categoryButtonHeight - 2);
                int merchantButtonGap = 4;
                int merchantStartX = x + m.padding * 2;
                int merchantStartY = m.contentStartY + m.padding;
                int pageButtonHeight = m.itemButtonHeight;
                int pageAreaY = y + height - scaleUi(25, m.scale);
                int merchantListBottom = pageAreaY - 6;
                int merchantListHeight = Math.max(merchantButtonHeight, merchantListBottom - merchantStartY);
                int visibleMerchantCount = Math.max(1,
                        (merchantListHeight + merchantButtonGap) / (merchantButtonHeight + merchantButtonGap));

                maxMerchantListScroll = Math.max(0, merchants.size() - visibleMerchantCount);
                merchantListScrollOffset = MathHelper.clamp(merchantListScrollOffset, 0, maxMerchantListScroll);

                int merchantScrollbarX = merchantStartX + merchantButtonWidth + 2;
                if (maxMerchantListScroll > 0
                        && isMouseOver(mouseX, mouseY, merchantScrollbarX, merchantStartY, 4, merchantListHeight)) {
                    isDraggingMerchantListScrollbar = true;
                    return;
                }

                for (int local = 0; local < visibleMerchantCount; local++) {
                    int i = merchantListScrollOffset + local;
                    if (i >= merchants.size()) {
                        break;
                    }

                    int by = merchantStartY + local * (merchantButtonHeight + merchantButtonGap);
                    if (by + merchantButtonHeight > merchantListBottom + 1) {
                        break;
                    }

                    if (isMouseOver(mouseX, mouseY, merchantStartX, by, merchantButtonWidth, merchantButtonHeight)) {
                        selectedMerchantIndex = i;
                        merchantScreenPage = 0;
                        selectedMerchantCategoryIndex = -1;
                        merchantCategoryScrollOffset = 0;
                        return;
                    }
                }

                MerchantDef selected = merchants.get(MathHelper.clamp(selectedMerchantIndex, 0, merchants.size() - 1));
                normalizeMerchantCategoryState(selected);

                if (selected.categories != null && !selected.categories.isEmpty()) {
                    int contentX = m.contentPanelX + m.padding;
                    int contentRight = m.contentPanelRight - m.padding;
                    int categoryBarY = m.contentStartY + m.padding;
                    int categoryBarH = Math.max(18, m.itemButtonHeight - 2);
                    int leftArrowW = 16;
                    int rightArrowW = 16;
                    int leftArrowX = contentX;
                    int rightArrowX = contentRight - rightArrowW;

                    if (isMouseOver(mouseX, mouseY, leftArrowX, categoryBarY, leftArrowW, categoryBarH)) {
                        if (merchantCategoryScrollOffset > 0) {
                            merchantCategoryScrollOffset--;
                        }
                        return;
                    }

                    if (isMouseOver(mouseX, mouseY, rightArrowX, categoryBarY, rightArrowW, categoryBarH)) {
                        if (merchantCategoryScrollOffset + 1 < selected.categories.size()) {
                            merchantCategoryScrollOffset++;
                        }
                        return;
                    }

                    int buttonX = leftArrowX + leftArrowW + 4;
                    int buttonEndX = rightArrowX - 4;
                    for (int i = merchantCategoryScrollOffset; i < selected.categories.size(); i++) {
                        CategoryDef category = selected.categories.get(i);
                        String label = (category == null || category.name == null || category.name.trim().isEmpty())
                                ? I18n.format("gui.inventory.merchant.uncategorized")
                                : category.name;
                        int btnW = getMerchantCategoryButtonWidth(mc.font, label);
                        if (buttonX + btnW > buttonEndX) {
                            break;
                        }
                        if (isMouseOver(mouseX, mouseY, buttonX, categoryBarY, btnW, categoryBarH)) {
                            selectedMerchantCategoryIndex = i;
                            merchantScreenPage = 0;
                            return;
                        }
                        buttonX += btnW + 4;
                    }
                }

                int totalPages = getMerchantTotalPages(selected);
                int pageButtonWidth = m.pageButtonWidth;
                int pageInfoX = (m.contentPanelX + m.contentPanelRight) / 2;
                int prevButtonX = pageInfoX - pageButtonWidth - 28;
                int nextButtonX = pageInfoX + 28;

                if (isMouseOver(mouseX, mouseY, prevButtonX, pageAreaY, pageButtonWidth, pageButtonHeight)) {
                    if (merchantScreenPage > 0) {
                        merchantScreenPage--;
                    }
                    return;
                }

                if (isMouseOver(mouseX, mouseY, nextButtonX, pageAreaY, pageButtonWidth, pageButtonHeight)) {
                    if (merchantScreenPage + 1 < totalPages) {
                        merchantScreenPage++;
                    }
                    return;
                }
            }
            return;
        }

        if (otherFeaturesScreenActive) {
            int backButtonWidth = m.pathManagerButtonWidth;
            int backButtonX = x + m.padding;
            int backButtonY = y + scaleUi(4, m.scale);

            if (isMouseOver(mouseX, mouseY, backButtonX, backButtonY, backButtonWidth, m.topButtonHeight)) {
                if (masterStatusHudEditMode) {
                    setMasterStatusHudEditMode(false);
                } else {
                    otherFeaturesScreenActive = false;
                }
                return;
            }

            int masterHudButtonWidth = getOtherFeaturesHudButtonWidth(m, mc.font);
            int masterHudButtonX = x + totalWidth - masterHudButtonWidth - m.padding;
            int masterHudButtonY = backButtonY;
            int editHudButtonWidth = getOtherFeaturesHudEditButtonWidth(m, mc.font);
            int editHudButtonX = masterHudButtonX - editHudButtonWidth - m.padding;
            int editHudButtonY = backButtonY;
            if (isMouseOver(mouseX, mouseY, editHudButtonX, editHudButtonY, editHudButtonWidth, m.topButtonHeight)) {
                setMasterStatusHudEditMode(!masterStatusHudEditMode);
                return;
            }
            if (isMouseOver(mouseX, mouseY, masterHudButtonX, masterHudButtonY, masterHudButtonWidth, m.topButtonHeight)) {
                MovementFeatureManager.setMasterStatusHudEnabled(!MovementFeatureManager.isMasterStatusHudEnabled());
                return;
            }

            if (masterStatusHudEditMode) {
                if (masterStatusHudExitButtonBounds != null && masterStatusHudExitButtonBounds.contains(mouseX, mouseY)) {
                    setMasterStatusHudEditMode(false);
                    return;
                }
                if (mouseButton == 0 && masterStatusHudEditorBounds != null && masterStatusHudEditorBounds.contains(mouseX, mouseY)) {
                    isDraggingMasterStatusHud = true;
                    masterStatusHudDragOffsetX = mouseX - masterStatusHudEditorBounds.x;
                    masterStatusHudDragOffsetY = mouseY - masterStatusHudEditorBounds.y;
                    return;
                }
            }

            List<GroupDef> groups = OtherFeatureGroupManager.getGroups();
            if (!groups.isEmpty()) {
                normalizeOtherFeatureGroupState(groups);

                int groupButtonWidth = getSafeCategoryListButtonWidth(m);
                int groupButtonHeight = Math.max(18, m.categoryButtonHeight - 2);
                int groupButtonGap = 4;
                int groupStartX = x + m.padding * 2;
                int groupStartY = m.contentStartY + m.padding;
                int groupListBottom = y + height - m.padding - 6;
                int groupListHeight = Math.max(groupButtonHeight, groupListBottom - groupStartY);
                int visibleGroupCount = Math.max(1,
                        (groupListHeight + groupButtonGap) / (groupButtonHeight + groupButtonGap));

                maxOtherFeatureGroupScroll = Math.max(0, groups.size() - visibleGroupCount);
                otherFeatureGroupScrollOffset = MathHelper.clamp(otherFeatureGroupScrollOffset, 0,
                        maxOtherFeatureGroupScroll);

                int groupScrollbarX = groupStartX + groupButtonWidth + 2;
                if (maxOtherFeatureGroupScroll > 0
                        && isMouseOver(mouseX, mouseY, groupScrollbarX, groupStartY, 4, groupListHeight)) {
                    isDraggingOtherFeatureGroupScrollbar = true;
                    return;
                }

                for (int local = 0; local < visibleGroupCount; local++) {
                    int i = otherFeatureGroupScrollOffset + local;
                    if (i >= groups.size()) {
                        break;
                    }

                    int by = groupStartY + local * (groupButtonHeight + groupButtonGap);
                    if (by + groupButtonHeight > groupListBottom + 1) {
                        break;
                    }

                    if (isMouseOver(mouseX, mouseY, groupStartX, by, groupButtonWidth, groupButtonHeight)) {
                        selectedOtherFeatureGroupIndex = i;
                        otherFeatureScreenPage = 0;
                        return;
                    }
                }

                GroupDef selectedGroup = groups.get(selectedOtherFeatureGroupIndex);
                OtherFeaturePageLayout pageLayout = buildOtherFeaturePageLayout(selectedGroup, m, wrapFont(mc.font));
                if (pageLayout.pageControls.prevButtonBounds.contains(mouseX, mouseY)) {
                    shiftOtherFeatureScreenPage(-1, pageLayout.totalPages);
                    return;
                }
                if (pageLayout.pageControls.nextButtonBounds.contains(mouseX, mouseY)) {
                    shiftOtherFeatureScreenPage(1, pageLayout.totalPages);
                    return;
                }
                for (OtherFeatureCardLayout card : pageLayout.cards) {
                    if (card != null && card.bounds != null && card.bounds.contains(mouseX, mouseY)) {
                        handleOtherFeatureClick(card.feature, mouseButton, mc);
                        return;
                    }
                }
            }
            return;
        }

        if (isMouseOver(mouseX, mouseY, otherFeaturesButtonX, otherFeaturesButtonY, otherFeaturesButtonWidth,
                m.topButtonHeight)) {
            OtherFeatureGroupManager.reload();
            merchantScreenActive = false;
            otherFeaturesScreenActive = true;
            otherFeatureScreenPage = 0;
            maxOtherFeatureGroupScroll = 0;
            isDraggingOtherFeatureGroupScrollbar = false;
            return;
        }
        if (isMouseOver(mouseX, mouseY, stopForegroundButtonX, stopForegroundButtonY, stopForegroundButtonWidth,
                m.topButtonHeight)) {
            EmbeddedNavigationHandler.INSTANCE.forceStop("主界面顶部按钮：停止前台导航");
            PathSequenceEventListener.instance.stopTracking();
            isLooping = false;
            return;
        }

        if (isMouseOver(mouseX, mouseY, stopBackgroundButtonX, stopBackgroundButtonY, stopBackgroundButtonWidth,
                m.topButtonHeight)) {
            PathSequenceEventListener.stopAllBackgroundRunners();
            return;
        }

        if (isMouseOver(mouseX, mouseY, pathManagerButtonX, pathManagerButtonY, pathManagerButtonWidth,
                m.topButtonHeight)) {
            closeOverlay();
            mc.setScreen(new GuiPathManager());
            return;
        }

        int contentStartY = m.contentStartY;
        int categoryPanelX = x + m.padding;
        int categoryPanelY = contentStartY;
        int categoryPanelWidth = m.categoryPanelWidth;
        int categoryPanelHeight = y + height - m.padding * 2 - categoryPanelY;

        if (maxCategoryScroll > 0) {
            int scrollbarX = categoryPanelX + categoryPanelWidth - 6;
            if (isMouseOver(mouseX, mouseY, scrollbarX, categoryPanelY + 5, 4, categoryPanelHeight - 10)) {
                isDraggingCategoryScrollbar = true;
                categoryScrollClickY = mouseY;
                initialCategoryScrollOffset = categoryScrollOffset;
                return;
            }
        }

        CategoryTreeRow clickedRow = findCategoryRowAt(mouseX, mouseY);
        if (clickedRow != null) {
            boolean canCollapse = clickedRow.isCustomCategoryRoot()
                    && !MainUiLayoutManager.getSubCategories(clickedRow.category).isEmpty();
            boolean clickedCollapseArrow = canCollapse && clickedRow.bounds != null
                    && mouseX <= clickedRow.bounds.x + 18;
            if (mouseButton == 0 && clickedCollapseArrow) {
                MainUiLayoutManager.toggleCollapsed(clickedRow.category);
                refreshGuiLists();
                return;
            }

            if (I18n.format("gui.inventory.category.debug").equals(clickedRow.category) && mouseButton == 1) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDebugCategoryRightClickTime > 500) {
                    debugCategoryRightClickCounter = 1;
                } else {
                    debugCategoryRightClickCounter++;
                }
                lastDebugCategoryRightClickTime = currentTime;

                if (debugCategoryRightClickCounter >= 2) {
                    isDebugRecordingMenuVisible = true;
                    currentCategory = clickedRow.category;
                    currentCustomSubCategory = "";
                    debugCategoryRightClickCounter = 0;
                }
                return;
            }

            if (mouseButton == 1 && !clickedRow.systemCategory) {
                openContextMenu(mouseX, mouseY,
                        clickedRow.isSubCategory()
                                ? buildSubCategoryContextMenu(clickedRow.category, clickedRow.subCategory)
                                : buildCategoryContextMenu(clickedRow.category));
                return;
            }

            if (mouseButton == 0) {
                pressedCategoryRow = clickedRow;
                pressedCategoryRowRect = clickedRow.bounds;
                pressedCategoryRowMouseX = mouseX;
                pressedCategoryRowMouseY = mouseY;
                draggingCategoryRowMouseX = mouseX;
                draggingCategoryRowMouseY = mouseY;
                isDraggingCategoryRow = false;
                currentCategorySortDropTarget = null;
                return;
            }
        }

        if (mouseButton == 1 && isMouseOver(mouseX, mouseY, categoryPanelX, categoryPanelY, categoryPanelWidth,
                categoryPanelHeight)) {
            openContextMenu(mouseX, mouseY, buildCategoryBlankAreaMenu());
            return;
        }

        if (isDebugRecordingMenuVisible && I18n.format("gui.inventory.category.debug").equals(currentCategory)) {
            int itemAreaStartX = m.contentPanelX + m.padding;
            int itemAreaStartY = contentStartY + m.padding;
            int itemButtonWidth = m.itemButtonWidth;
            int itemButtonHeight = m.itemButtonHeight;

            int buttonX1 = itemAreaStartX + 10;
            int buttonY1 = itemAreaStartY + 10;
            if (isMouseOver(mouseX, mouseY, buttonX1, buttonY1, itemButtonWidth, itemButtonHeight)) {
                closeOverlay();
                mc.setScreen(new GuiCustomPathCreator());
                return;
            }

            int buttonX2 = buttonX1 + itemButtonWidth + m.gap;
            int buttonY2 = itemAreaStartY + 10;
            if (isMouseOver(mouseX, mouseY, buttonX2, buttonY2, itemButtonWidth, itemButtonHeight)) {
                if (mouseButton == 0) {
                    AutoEquipHandler.enabled = !AutoEquipHandler.enabled;
                    AutoEquipHandler.saveConfig();
                } else if (mouseButton == 1) {
                    closeOverlay();
                    mc.setScreen(new GuiAutoEquipManager(null));
                }
                return;
            }

            int buttonX3 = buttonX2 + itemButtonWidth + m.gap;
            int buttonY3 = itemAreaStartY + 10;
            if (isMouseOver(mouseX, mouseY, buttonX3, buttonY3, itemButtonWidth, itemButtonHeight)) {
                closeOverlay();
                mc.setScreen(new GuiDebugKeybindManager(null));
                return;
            }
        } else {
            if (isCustomCategorySelection()) {
                if (mouseButton == 0 && customSearchToggleButtonBounds != null
                        && customSearchToggleButtonBounds.contains(mouseX, mouseY)) {
                    boolean expand = !isCustomSearchExpanded();
                    setCustomSearchExpanded(expand, expand);
                    return;
                }
                if (isCustomSearchExpanded() && customSequenceSearchField != null) {
                    if (mouseButton == 0 && customSearchClearButtonBounds != null
                            && customSearchClearButtonBounds.contains(mouseX, mouseY)) {
                        clearCustomSequenceSearch(true);
                        return;
                    }
                    customSequenceSearchField.mouseClicked(mouseX, mouseY, mouseButton);
                    if (mouseX >= customSequenceSearchField.x
                            && mouseX < customSequenceSearchField.x + customSequenceSearchField.width
                            && mouseY >= customSequenceSearchField.y
                            && mouseY < customSequenceSearchField.y + customSequenceSearchField.height) {
                        return;
                    }
                } else if (customSequenceSearchField != null) {
                    customSequenceSearchField.setFocused(false);
                }
            }

            if (isCommonCategory(currentCategory)
                    && handleGroupedCommonCategoryClick(mouseX, mouseY, mouseButton, m, contentStartY, mc)) {
                return;
            }

            if (isCustomCategorySelection()
                    && handleCustomSequenceCategoryClick(mouseX, mouseY, mouseButton, m, contentStartY, mc)) {
                return;
            }
            if (isCustomCategorySelection()) {
                if (handleMainPageControlsClick(mouseX, mouseY, m, contentStartY)) {
                    return;
                }
                return;
            }

            List<String> items = isCommonCategory(currentCategory) ? null : categoryItems.get(currentCategory);
            int itemAreaStartX = m.contentPanelX + m.padding;
            int itemAreaStartY = contentStartY + m.padding;
            int itemButtonWidth = m.itemButtonWidth;
            int itemButtonHeight = m.itemButtonHeight;
            int itemsPerRow = 3;

            for (int i = 0; i < 18; i++) {
                int index = currentPage * 18 + i;
                if (items == null || index >= items.size()) {
                    break;
                }

                int col = i % itemsPerRow;
                int row = i / itemsPerRow;
                int buttonX = itemAreaStartX + col * (itemButtonWidth + m.gap);
                int buttonY = itemAreaStartY + row * (itemButtonHeight + m.gap);
                String command = items.get(index);

                if (command.startsWith("custom_path:")) {
                    int deleteX = buttonX + itemButtonWidth - 10;
                    int deleteY = buttonY;
                    if (isMouseOver(mouseX, mouseY, deleteX, deleteY, 10, 10) && mouseButton == 0) {
                        String sequenceName = command.substring(command.indexOf(":") + 1);
                        PathSequenceManager.deleteCustomSequence(sequenceName);
                        refreshGuiLists();
                        return;
                    }
                }

                if (!isMouseOver(mouseX, mouseY, buttonX, buttonY, itemButtonWidth, itemButtonHeight)) {
                    continue;
                }

                boolean commandHandled = false;

                if (currentCategory.equals(I18n.format("gui.inventory.category.builtin_script"))) {
                    if (command.startsWith(CMD_BUILTIN_PRIMARY_PREFIX)) {
                        builtinScriptPrimaryCategory = command.substring(CMD_BUILTIN_PRIMARY_PREFIX.length());
                        builtinScriptSubCategory = null;
                        currentPage = 0;
                        refreshGuiLists();
                        commandHandled = true;
                    } else if (command.startsWith(CMD_BUILTIN_SUBCAT_PREFIX)) {
                        builtinScriptSubCategory = command.substring(CMD_BUILTIN_SUBCAT_PREFIX.length());
                        currentPage = 0;
                        refreshGuiLists();
                        commandHandled = true;
                    } else if (CMD_BUILTIN_PRIMARY_BACK.equals(command)) {
                        builtinScriptPrimaryCategory = null;
                        builtinScriptSubCategory = null;
                        currentPage = 0;
                        refreshGuiLists();
                        commandHandled = true;
                    } else if (CMD_BUILTIN_SUBCAT_BACK.equals(command)) {
                        builtinScriptSubCategory = null;
                        currentPage = 0;
                        refreshGuiLists();
                        commandHandled = true;
                    } else if (command.equals("stop")) {
                        EmbeddedNavigationHandler.INSTANCE.forceStop("内置脚本分类按钮：停止当前导航");
                        PathSequenceEventListener.instance.stopTracking();
                        isLooping = false;
                        commandHandled = true;
                    } else if (command.equals("setloop")) {
                        closeOverlay();
                        mc.setScreen(new GuiLoopCountInput(null));
                        commandHandled = true;
                    }
                } else if (currentCategory.equals(I18n.format("gui.inventory.category.common"))
                        || currentCategory.equals(I18n.format("gui.inventory.category.rsl"))) {
                    if (command.equals("profile_manager")) {
                        closeOverlay();
                        mc.setScreen(new GuiProfileManager(null));
                    } else if (command.equals("quick_exchange_config")) {
                        closeOverlay();
                        mc.setScreen(new GuiQuickExchangeConfig(null));
                    } else if (command.equals("chat_optimization")) {
                        closeOverlay();
                        mc.setScreen(new GuiChatOptimization(null));
                    } else if (command.equals("toggle_auto_pickup")) {
                        if (mouseButton == 0) {
                            AutoPickupHandler.globalEnabled = !AutoPickupHandler.globalEnabled;
                            AutoPickupHandler.saveConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoPickupConfig(null));
                        }
                    } else if (command.equals("toggle_auto_use_item")) {
                        if (mouseButton == 0) {
                            AutoUseItemHandler.globalEnabled = !AutoUseItemHandler.globalEnabled;
                            AutoUseItemHandler.INSTANCE.resetSchedule();
                            AutoUseItemHandler.saveConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoUseItemConfig(null));
                        }
                    } else if (command.equals("block_replacement_config")) {
                        if (mouseButton == 0 || mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiBlockReplacementConfig(null));
                        }
                    } else if (command.equals("toggle_server_feature_visibility")) {
                        if (mouseButton == 0 || mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiServerFeatureVisibilityConfig(null));
                        }
                    } else if (command.equals("setloop")) {
                        closeOverlay();
                        mc.setScreen(new GuiLoopCountInput(null));
                    } else if (command.equals("autoeat")) {
                        if (mouseButton == 0) {
                            AutoEatHandler.autoEatEnabled = !AutoEatHandler.autoEatEnabled;
                            AutoEatHandler.saveAutoEatConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoEatConfig(null));
                        }
                    } else if (command.equals("toggle_auto_fishing")) {
                        if (mouseButton == 0) {
                            AutoFishingHandler.INSTANCE.toggleEnabled();
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoFishingConfig(null));
                        }
                    } else if (command.equals("autoskill")) {
                        if (mouseButton == 0) {
                            AutoSkillHandler.autoSkillEnabled = !AutoSkillHandler.autoSkillEnabled;
                            AutoSkillHandler.saveSkillConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoSkillEditor(null));
                        }
                    } else if (command.equals("signin_online_rewards")) {
                        if (mouseButton == 0) {
                            AutoSigninOnlineHandler.enabled = !AutoSigninOnlineHandler.enabled;
                            AutoSigninOnlineHandler.saveConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoSigninOnlineConfig(null));
                        }
                    } else if (command.equals("toggle_fast_attack")) {
                        if (mouseButton == 0) {
                            FreecamHandler.INSTANCE.toggleFastAttack();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiFastAttackConfig(null));
                        }
                        refreshGuiLists();
                    } else if (command.equals("toggle_kill_aura")) {
                        if (mouseButton == 0) {
                            KillAuraHandler.INSTANCE.toggleEnabled();
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiKillAuraConfig(null));
                        }
                    } else if (command.equals("toggle_kill_timer")) {
                        if (mouseButton == 0) {
                            KillTimerHandler.toggleEnabled();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiKillTimerConfig(null));
                        }
                        refreshGuiLists();
                    } else if (command.equals("toggle_ad_exp_panel")) {
                        if (mouseButton == 0) {
                            AdExpPanelHandler.toggleEnabled();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAdExpPanelConfig(null));
                        }
                        refreshGuiLists();
                    } else if (command.equals("toggle_shulker_rebound_fix")) {
                        ShulkerMiningReboundFixHandler.toggleEnabled();
                        refreshGuiLists();
                    } else if (command.equals("toggle_mouse_detach")) {
                        ModConfig.isMouseDetached = !ModConfig.isMouseDetached;
                        String mouseStatus = ModConfig.isMouseDetached ? I18n.format("gui.inventory.mouse.detached")
                                : I18n.format("gui.inventory.mouse.reattached");
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(
                                    new TextComponentString(I18n.format("msg.inventory.mouse_toggle", mouseStatus)));
                        }
                        if (ModConfig.isMouseDetached) {
                            mc.mouseHandler.releaseMouse();
                        } else if (mc.screen == null) {
                            mc.mouseHandler.grabMouse();
                        }
                        refreshGuiLists();
                    } else if (command.equals("followconfig")) {
                        if (mouseButton == 0) {
                            boolean wasActive = AutoFollowHandler.getActiveRule() != null;
                            if (wasActive) {
                                AutoFollowHandler.toggleEnabledFromQuickSwitch();
                                if (mc.player != null) {
                                    mc.player.sendSystemMessage(new TextComponentString("§b[自动追怪] §c已关闭"));
                                }
                            } else if (!AutoFollowHandler.hasAnyRuleConfigured()) {
                                if (mc.player != null) {
                                    mc.player.sendSystemMessage(new TextComponentString("§b[自动追怪] §e未配置任何规则，请右键打开配置界面"));
                                }
                            } else {
                                com.zszl.zszlScriptMod.system.AutoFollowRule activatedRule = AutoFollowHandler.toggleEnabledFromQuickSwitch();
                                if (mc.player != null) {
                                    String suffix = activatedRule != null && activatedRule.name != null
                                            && !activatedRule.name.trim().isEmpty()
                                                    ? " §7规则: §f" + activatedRule.name.trim()
                                                    : "";
                                    mc.player.sendSystemMessage(new TextComponentString("§b[自动追怪] §a已开启" + suffix));
                                }
                            }
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoFollowManager(null));
                        }
                    } else if (command.equals("conditional_execution")) {
                        if (mouseButton == 0) {
                            ConditionalExecutionHandler
                                    .setGlobalEnabled(!ConditionalExecutionHandler.isGloballyEnabled());
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiConditionalExecutionManager(null));
                        }
                    } else if (command.equals("auto_escape")) {
                        if (mouseButton == 0) {
                            AutoEscapeHandler.setGlobalEnabled(!AutoEscapeHandler.isGloballyEnabled());
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoEscapeManager(null));
                        }
                    } else if (command.equals("toggle_death_auto_rejoin")) {
                        if (mouseButton == 0) {
                            DeathAutoRejoinHandler.deathAutoRejoinEnabled = !DeathAutoRejoinHandler.deathAutoRejoinEnabled;
                            DeathAutoRejoinHandler.saveConfig();
                            if (mc.player != null) {
                                String status = DeathAutoRejoinHandler.deathAutoRejoinEnabled
                                        ? I18n.format("gui.inventory.death_auto_rejoin.enabled")
                                        : I18n.format("gui.inventory.death_auto_rejoin.disabled");
                                mc.player.sendSystemMessage(new TextComponentString(
                                        I18n.format("msg.inventory.death_auto_rejoin_status", status)));
                            }
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiDeathAutoRejoinConfig(null));
                        }
                    } else if (command.equals("keybind_manager")) {
                        closeOverlay();
                        mc.setScreen(new GuiKeybindManager(null));
                    } else if (command.equals("warehouse_manager")) {
                        closeOverlay();
                        mc.setScreen(new GuiWarehouseManager(null));
                    } else if (command.equals("baritone_settings")) {
                        closeOverlay();
                        mc.setScreen(new GuiBaritoneCommandTable(null));
                    } else if (command.equals("toggle_auto_stack_shulker_boxes")) {
                        if (mouseButton == 0) {
                            ShulkerBoxStackingHandler.autoStackingEnabled = !ShulkerBoxStackingHandler.autoStackingEnabled;
                            ShulkerBoxStackingHandler.saveConfig();
                            if (mc.player != null) {
                                String status = ShulkerBoxStackingHandler.autoStackingEnabled
                                        ? I18n.format("gui.inventory.autostack.enabled")
                                        : I18n.format("gui.inventory.autostack.disabled");
                                mc.player.sendSystemMessage(
                                        new TextComponentString(I18n.format("msg.inventory.autostack_status", status)));
                            }
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.setScreen(new GuiAutoStackingConfig(null));
                        }
                    }
                    commandHandled = true;
                } else if (currentCategory.equals(I18n.format("gui.inventory.category.debug"))) {
                    if (command.equals("debug_settings")) {
                        if (mouseButton == 0) {
                            closeOverlay();
                            mc.setScreen(new GuiDebugConfig());
                        }
                    } else if (command.equals("memory_manager")) {
                        closeOverlay();
                        mc.setScreen(new GuiMemoryManager());
                    } else if (command.equals("player_equipment_viewer")) {
                        closeOverlay();
                        mc.execute(() -> {
                            if (mouseButton == 0) {
                                InventoryViewerManager.copyInventoryFromTarget();
                            }
                            GuiHandler.openInventoryViewer(mc);
                        });
                    } else if (command.equals("packet_handler")) {
                        closeOverlay();
                        mc.setScreen(new com.zszl.zszlScriptMod.gui.packet.GuiPacketMain(null));
                    } else if (command.equals("gui_inspector_manager")) {
                        closeOverlay();
                        mc.setScreen(new com.zszl.zszlScriptMod.gui.debug.GuiGuiInspectorManager(null));
                    } else if (command.equals("performance_monitor")) {
                        closeOverlay();
                        mc.setScreen(new GuiPerformanceMonitor());
                    } else if (command.equals("current_resolution_info")) {
                        closeOverlay();
                        mc.setScreen(new GuiResolutionConfig());
                    } else if (command.equals("reload_paths")) {
                        PathSequenceManager.initializePathSequences();
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(new TextComponentString(
                                    ChatFormatting.GREEN + I18n.format("msg.inventory.paths_reloaded")));
                        }
                        refreshGuiLists();
                    } else if (command.equals("terrain_scanner")) {
                        closeOverlay();
                        mc.setScreen(new GuiTerrainScannerManager(null));
                    }
                    commandHandled = true;
                }

                if (!commandHandled && (command.startsWith("path:") || command.startsWith("custom_path:"))) {
                    String sequenceName = command.substring(command.indexOf(":") + 1);
                    PathSequence sequence = PathSequenceManager.getSequence(sequenceName);
                    if (sequence != null) {
                        if (sequence.isCustom()) {
                            MainUiLayoutManager.recordSequenceOpened(sequenceName);
                        }
                        if (sequence.shouldCloseGuiAfterStart()) {
                            closeOverlay();
                        }
                        PathSequenceManager.runPathSequence(sequenceName);
                    }
                }
                return;
            }

            if (handleMainPageControlsClick(mouseX, mouseY, m, contentStartY)) {
                return;
            }
        }
    }

    public static boolean handleKeyTyped(char typedChar, int keyCode) {
        if (masterStatusHudEditMode && keyCode == Keyboard.KEY_ESCAPE) {
            setMasterStatusHudEditMode(false);
            return true;
        }

        if (contextMenuVisible) {
            int depth = Math.max(0, contextMenuLayers.isEmpty() ? 0 : contextMenuLayers.size() - 1);
            List<ContextMenuItem> items = contextMenuLayers.isEmpty() ? contextMenuRootItems
                    : contextMenuLayers.get(depth).items;
            int selectedIndex = getKeyboardMenuSelection(depth, items);

            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeContextMenu();
                return true;
            }
            if (keyCode == Keyboard.KEY_UP) {
                setKeyboardMenuSelection(depth, moveKeyboardMenuSelection(items, selectedIndex, -1));
                return true;
            }
            if (keyCode == Keyboard.KEY_DOWN) {
                setKeyboardMenuSelection(depth, moveKeyboardMenuSelection(items, selectedIndex, 1));
                return true;
            }
            if (keyCode == Keyboard.KEY_LEFT) {
                if (depth > 0) {
                    trimContextMenuOpenPath(depth - 1);
                    while (contextMenuKeyboardSelectionPath.size() > depth) {
                        contextMenuKeyboardSelectionPath.remove(contextMenuKeyboardSelectionPath.size() - 1);
                    }
                } else {
                    closeContextMenu();
                }
                return true;
            }
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                ContextMenuItem item = items.get(selectedIndex);
                if (keyCode == Keyboard.KEY_RIGHT && item.hasChildren()) {
                    while (contextMenuOpenPath.size() <= depth) {
                        contextMenuOpenPath.add(-1);
                    }
                    contextMenuOpenPath.set(depth, selectedIndex);
                    setKeyboardMenuSelection(depth + 1, findFirstEnabledMenuItem(item.children));
                    return true;
                }
                if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && item.enabled) {
                    if (item.hasChildren()) {
                        while (contextMenuOpenPath.size() <= depth) {
                            contextMenuOpenPath.add(-1);
                        }
                        contextMenuOpenPath.set(depth, selectedIndex);
                        setKeyboardMenuSelection(depth + 1, findFirstEnabledMenuItem(item.children));
                    } else {
                        closeContextMenu();
                        if (item.action != null) {
                            item.action.run();
                        }
                    }
                    return true;
                }
            }
        }

        if (isCustomCategorySelection()) {
            if (keyCode == Keyboard.KEY_F && isControlDown()) {
                setCustomSearchExpanded(true, true);
                return true;
            }
            if (customSequenceSearchField != null && keyCode == Keyboard.KEY_A && isControlDown()
                    && !customSequenceSearchField.isFocused()) {
                for (SequenceCardRenderInfo info : visibleCustomSequenceCards) {
                    selectedCustomSequenceNames.add(info.sequence.getName());
                }
                return true;
            }

            if (customSequenceSearchField != null && customSequenceSearchField.isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    if (isBlank(customSequenceSearchField.getText())) {
                        setCustomSearchExpanded(false, false);
                    } else {
                        customSequenceSearchField.setFocused(false);
                    }
                    return true;
                }
                if (customSequenceSearchField.textboxKeyTyped(typedChar, keyCode)) {
                    customSequenceSearchQuery = customSequenceSearchField.getText();
                    currentPage = 0;
                    CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
                    pruneSelectedCustomSequences();
                    return true;
                }
            }
        }

        return false;
    }

    public static void handleMouseWheel(int dWheel, int rawMouseX, int rawMouseY) {
        if (masterStatusHudEditMode) {
            return;
        }
        ScaledResolution res = new ScaledResolution(Minecraft.getInstance());
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();
        int mouseX = scaleRawMouseX(rawMouseX, screenWidth);
        int mouseY = scaleRawMouseY(rawMouseY, screenHeight);
        OverlayMetrics m = getCurrentOverlayMetrics(screenWidth, screenHeight);

        if (merchantScreenActive) {
            List<MerchantDef> merchants = MerchantExchangeManager.getMerchants();
            if (!merchants.isEmpty()) {
                int merchantButtonWidth = m.categoryButtonWidth;
                int merchantButtonHeight = Math.max(18, m.categoryButtonHeight - 2);
                int merchantButtonGap = 4;
                int merchantStartX = m.x + m.padding * 2;
                int merchantStartY = m.contentStartY + m.padding;
                int pageAreaY = m.y + m.totalHeight - scaleUi(25, m.scale);
                int merchantListBottom = pageAreaY - 6;
                int merchantListHeight = Math.max(merchantButtonHeight, merchantListBottom - merchantStartY);
                int visibleMerchantCount = Math.max(1,
                        (merchantListHeight + merchantButtonGap) / (merchantButtonHeight + merchantButtonGap));

                maxMerchantListScroll = Math.max(0, merchants.size() - visibleMerchantCount);
                merchantListScrollOffset = MathHelper.clamp(merchantListScrollOffset, 0, maxMerchantListScroll);

                int merchantScrollbarX = merchantStartX + merchantButtonWidth + 2;
                boolean inMerchantList = isMouseOver(mouseX, mouseY, merchantStartX, merchantStartY,
                        merchantButtonWidth, merchantListHeight);
                boolean inMerchantScrollbar = isMouseOver(mouseX, mouseY, merchantScrollbarX, merchantStartY, 4,
                        merchantListHeight);
                if (inMerchantList || inMerchantScrollbar) {
                    if (dWheel > 0) {
                        merchantListScrollOffset = Math.max(0, merchantListScrollOffset - 1);
                    } else {
                        merchantListScrollOffset = Math.min(maxMerchantListScroll, merchantListScrollOffset + 1);
                    }
                }
            }
            return;
        }

        if (otherFeaturesScreenActive) {
            List<GroupDef> groups = OtherFeatureGroupManager.getGroups();
            if (!groups.isEmpty()) {
                normalizeOtherFeatureGroupState(groups);
                int groupButtonWidth = getSafeCategoryListButtonWidth(m);
                int groupButtonHeight = Math.max(18, m.categoryButtonHeight - 2);
                int groupButtonGap = 4;
                int groupStartX = m.x + m.padding * 2;
                int groupStartY = m.contentStartY + m.padding;
                int groupListBottom = m.y + m.totalHeight - m.padding - 6;
                int groupListHeight = Math.max(groupButtonHeight, groupListBottom - groupStartY);
                int visibleGroupCount = Math.max(1,
                        (groupListHeight + groupButtonGap) / (groupButtonHeight + groupButtonGap));

                maxOtherFeatureGroupScroll = Math.max(0, groups.size() - visibleGroupCount);
                otherFeatureGroupScrollOffset = MathHelper.clamp(otherFeatureGroupScrollOffset, 0,
                        maxOtherFeatureGroupScroll);

                int groupScrollbarX = groupStartX + groupButtonWidth + 2;
                boolean inGroupList = isMouseOver(mouseX, mouseY, groupStartX, groupStartY,
                        groupButtonWidth, groupListHeight);
                boolean inGroupScrollbar = isMouseOver(mouseX, mouseY, groupScrollbarX, groupStartY, 4,
                        groupListHeight);
                if (inGroupList || inGroupScrollbar) {
                    if (dWheel > 0) {
                        otherFeatureGroupScrollOffset = Math.max(0, otherFeatureGroupScrollOffset - 1);
                    } else {
                        otherFeatureGroupScrollOffset = Math.min(maxOtherFeatureGroupScroll,
                                otherFeatureGroupScrollOffset + 1);
                    }
                } else {
                    GroupDef selectedGroup = groups.get(selectedOtherFeatureGroupIndex);
                    OtherFeaturePageLayout pageLayout = buildOtherFeaturePageLayout(selectedGroup, m,
                            wrapFont(Minecraft.getInstance().font));
                    boolean inFeatureCards = pageLayout.contentBounds != null
                            && pageLayout.contentBounds.contains(mouseX, mouseY);
                    boolean inPageControls = pageLayout.pageControls != null
                            && pageLayout.pageControls.containerBounds.contains(mouseX, mouseY);
                    if ((inFeatureCards || inPageControls) && pageLayout.totalPages > 1) {
                        if (dWheel > 0) {
                            shiftOtherFeatureScreenPage(-1, pageLayout.totalPages);
                        } else {
                            shiftOtherFeatureScreenPage(1, pageLayout.totalPages);
                        }
                    }
                }
            }
            return;
        }

        if (shouldShowMainPageControls()) {
            Rectangle rightPanelBounds = getMainRightPanelBounds(m);
            if (rightPanelBounds.contains(mouseX, mouseY)) {
                shiftCurrentPage(dWheel > 0 ? -1 : 1, getCurrentTotalPages(m, m.contentStartY));
                return;
            }
        }

        int categoryPanelX = m.x + m.padding;
        int categoryPanelWidth = m.categoryPanelWidth;
        int categoryPanelY = m.contentStartY;
        int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;

        if (isMouseOver(mouseX, mouseY, categoryPanelX, categoryPanelY, categoryPanelWidth, categoryPanelHeight)) {
            if (dWheel > 0) {
                categoryScrollOffset = Math.max(0, categoryScrollOffset - 1);
            } else {
                categoryScrollOffset = Math.min(maxCategoryScroll, categoryScrollOffset + 1);
            }
        }
    }

    public static void handleMouseDrag(int mouseX, int mouseY) {
        ScaledResolution res = new ScaledResolution(Minecraft.getInstance());
        OverlayMetrics m = getCurrentOverlayMetrics(res.getScaledWidth(), res.getScaledHeight());

        if (isDraggingMasterStatusHud) {
            int hudWidth = masterStatusHudEditorBounds == null ? 140 : masterStatusHudEditorBounds.width;
            int hudHeight = masterStatusHudEditorBounds == null ? 60 : masterStatusHudEditorBounds.height;
            int newX = MathHelper.clamp(mouseX - masterStatusHudDragOffsetX + 4, 0, Math.max(0, res.getScaledWidth() - hudWidth + 4));
            int newY = MathHelper.clamp(mouseY - masterStatusHudDragOffsetY + 4, 0, Math.max(0, res.getScaledHeight() - hudHeight + 4));
            MovementFeatureManager.setMasterStatusHudPositionTransient(newX, newY);
            return;
        }

        if (isDraggingCategoryDivider) {
            int panelLeft = m.x + m.padding;
            int contentRight = m.x + m.totalWidth - m.padding;
            int desiredWidth = mouseX - categoryDividerMouseOffsetX - panelLeft + m.gap / 2;
            int scaledMinWidth = scaleUi(CATEGORY_PANEL_MIN_BASE_WIDTH, m.scale);
            int scaledMaxWidth = Math.min(scaleUi(CATEGORY_PANEL_MAX_BASE_WIDTH, m.scale),
                    Math.max(scaledMinWidth, contentRight - panelLeft - scaleUi(140, m.scale)));
            int clampedScaledWidth = MathHelper.clamp(desiredWidth, scaledMinWidth, scaledMaxWidth);
            int unscaledWidth = Math.round(clampedScaledWidth / Math.max(0.01f, m.scale));
            MainUiLayoutManager.setCategoryPanelBaseWidth(clampCategoryPanelBaseWidth(unscaledWidth));
            return;
        }

        if (pressedCategoryRow != null) {
            draggingCategoryRowMouseX = mouseX;
            draggingCategoryRowMouseY = mouseY;
            if (!isDraggingCategoryRow) {
                int deltaX = Math.abs(mouseX - pressedCategoryRowMouseX);
                int deltaY = Math.abs(mouseY - pressedCategoryRowMouseY);
                if (deltaX >= 4 || deltaY >= 4) {
                    isDraggingCategoryRow = true;
                }
            }
            if (isDraggingCategoryRow) {
                int categoryPanelY = m.contentStartY;
                int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;
                int autoScrollMargin = Math.max(14, m.categoryButtonHeight / 2);
                if (mouseY <= categoryPanelY + autoScrollMargin) {
                    categoryScrollOffset = Math.max(0, categoryScrollOffset - 1);
                } else if (mouseY >= categoryPanelY + categoryPanelHeight - autoScrollMargin) {
                    categoryScrollOffset = Math.min(maxCategoryScroll, categoryScrollOffset + 1);
                }
                currentCategorySortDropTarget = findSortableCategoryRowAt(mouseX, mouseY, pressedCategoryRow);
                if (currentCategorySortDropTarget != null && currentCategorySortDropTarget.bounds != null) {
                    currentCategorySortDropAfter = mouseY >= currentCategorySortDropTarget.bounds.y
                            + currentCategorySortDropTarget.bounds.height / 2;
                }
            }
        }

        if (pressedCustomSequence != null) {
            draggingCustomSequenceMouseX = mouseX;
            draggingCustomSequenceMouseY = mouseY;
            if (!isDraggingCustomSequenceCard) {
                int deltaX = Math.abs(mouseX - pressedCustomSequenceMouseX);
                int deltaY = Math.abs(mouseY - pressedCustomSequenceMouseY);
                if (deltaX >= 4 || deltaY >= 4) {
                    isDraggingCustomSequenceCard = true;
                }
            }
            if (isDraggingCustomSequenceCard) {
                MainPageControlBounds pageControls = getMainPageControlBounds(m);
                long now = System.currentTimeMillis();
                if (now >= customSequencePageTurnLockUntil) {
                    int totalPages = getCurrentTotalPages(m, m.contentStartY);
                    int pageDelta = 0;
                    if (pageControls.prevButtonBounds.contains(mouseX, mouseY)) {
                        pageDelta = -1;
                    } else if (pageControls.nextButtonBounds.contains(mouseX, mouseY)) {
                        pageDelta = 1;
                    }
                    if (pageDelta != 0 && shiftCurrentPage(pageDelta, totalPages)) {
                        customSequencePageTurnLockUntil = now + 1000L;
                        currentCustomSequenceSortTargetName = "";
                        currentCustomSequenceSortAfter = false;
                        currentSequenceDropTarget = null;
                        return;
                    }
                }

                SequenceCardRenderInfo sortTarget = findSortableCustomSequenceCardAt(mouseX, mouseY,
                        pressedCustomSequence);
                if (sortTarget != null) {
                    currentCustomSequenceSortTargetName = sortTarget.sequence.getName();
                    boolean useHorizontalSplit = shouldUseHorizontalCustomSequenceSplit(sortTarget);
                    currentCustomSequenceSortAfter = useHorizontalSplit
                            ? mouseX >= sortTarget.bounds.x + sortTarget.bounds.width / 2
                            : mouseY >= sortTarget.bounds.y + sortTarget.bounds.height / 2;
                    currentSequenceDropTarget = null;
                } else {
                    currentCustomSequenceSortTargetName = "";
                    currentCustomSequenceSortAfter = false;
                    currentSequenceDropTarget = findCustomSectionDropTargetAt(mouseX, mouseY, pressedCustomSequence);
                    if (currentSequenceDropTarget == null) {
                        currentSequenceDropTarget = findDroppableCategoryRowAt(mouseX, mouseY);
                    }
                }
            }
        }

        if (isDraggingMerchantListScrollbar) {
            List<MerchantDef> merchants = MerchantExchangeManager.getMerchants();
            if (!merchants.isEmpty()) {
                int merchantButtonWidth = m.categoryButtonWidth;
                int merchantButtonHeight = Math.max(18, m.categoryButtonHeight - 2);
                int merchantButtonGap = 4;
                int merchantStartY = m.contentStartY + m.padding;
                int pageAreaY = m.y + m.totalHeight - scaleUi(25, m.scale);
                int merchantListBottom = pageAreaY - 6;
                int merchantListHeight = Math.max(merchantButtonHeight, merchantListBottom - merchantStartY);
                int visibleMerchantCount = Math.max(1,
                        (merchantListHeight + merchantButtonGap) / (merchantButtonHeight + merchantButtonGap));

                maxMerchantListScroll = Math.max(0, merchants.size() - visibleMerchantCount);
                merchantListScrollOffset = MathHelper.clamp(merchantListScrollOffset, 0, maxMerchantListScroll);

                int merchantScrollbarX = m.x + m.padding * 2 + merchantButtonWidth + 2;
                int merchantScrollbarY = merchantStartY;
                int merchantScrollbarHeight = merchantListHeight;
                int thumbHeight = Math.max(12,
                        (int) ((float) visibleMerchantCount / merchants.size() * merchantScrollbarHeight));
                int scrollableHeight = merchantScrollbarHeight - thumbHeight;

                if (scrollableHeight > 0) {
                    int centerY = mouseY - merchantScrollbarY - thumbHeight / 2;
                    centerY = MathHelper.clamp(centerY, 0, scrollableHeight);
                    float percent = (float) centerY / (float) scrollableHeight;
                    int newOffset = Math.round(percent * maxMerchantListScroll);
                    merchantListScrollOffset = MathHelper.clamp(newOffset, 0, maxMerchantListScroll);
                }

                if (mouseX < merchantScrollbarX - 20 || mouseX > merchantScrollbarX + 24
                        || mouseY < merchantScrollbarY - 20
                        || mouseY > merchantScrollbarY + merchantScrollbarHeight + 20) {
                    // 允许拖拽时轻微越界，不立即取消
                }
            }
            return;
        }

        if (isDraggingOtherFeatureGroupScrollbar) {
            List<GroupDef> groups = OtherFeatureGroupManager.getGroups();
            if (!groups.isEmpty()) {
                int groupButtonWidth = getSafeCategoryListButtonWidth(m);
                int groupButtonHeight = Math.max(18, m.categoryButtonHeight - 2);
                int groupButtonGap = 4;
                int groupStartY = m.contentStartY + m.padding;
                int groupListBottom = m.y + m.totalHeight - m.padding - 6;
                int groupListHeight = Math.max(groupButtonHeight, groupListBottom - groupStartY);
                int visibleGroupCount = Math.max(1,
                        (groupListHeight + groupButtonGap) / (groupButtonHeight + groupButtonGap));

                maxOtherFeatureGroupScroll = Math.max(0, groups.size() - visibleGroupCount);
                otherFeatureGroupScrollOffset = MathHelper.clamp(otherFeatureGroupScrollOffset, 0,
                        maxOtherFeatureGroupScroll);

                int groupScrollbarX = m.x + m.padding * 2 + groupButtonWidth + 2;
                int groupScrollbarY = groupStartY;
                int groupScrollbarHeight = groupListHeight;
                int thumbHeight = Math.max(12,
                        (int) ((float) visibleGroupCount / groups.size() * groupScrollbarHeight));
                int scrollableHeight = groupScrollbarHeight - thumbHeight;

                if (scrollableHeight > 0) {
                    int centerY = mouseY - groupScrollbarY - thumbHeight / 2;
                    centerY = MathHelper.clamp(centerY, 0, scrollableHeight);
                    float percent = (float) centerY / (float) scrollableHeight;
                    int newOffset = Math.round(percent * maxOtherFeatureGroupScroll);
                    otherFeatureGroupScrollOffset = MathHelper.clamp(newOffset, 0, maxOtherFeatureGroupScroll);
                }
            }
            return;
        }

        if (isDraggingCategoryScrollbar) {
            int categoryPanelY = m.contentStartY;
            int categoryPanelHeight = m.y + m.totalHeight - m.padding * 2 - categoryPanelY;
            int scrollbarY = categoryPanelY + 5;
            int scrollbarHeight = categoryPanelHeight - 10;

            if (categories.isEmpty()) {
                return;
            }

            int categoryItemHeight = m.categoryItemHeight;
            int visibleCategories = (categoryPanelHeight - 10) / categoryItemHeight;
            int totalRows = Math.max(1, buildVisibleCategoryTreeRows().size());
            int thumbHeight = Math.max(10, (int) ((float) visibleCategories / totalRows * scrollbarHeight));
            int scrollableHeight = scrollbarHeight - thumbHeight;

            if (scrollableHeight > 0) {
                int thumbTop = MathHelper.clamp(mouseY - scrollbarY - thumbHeight / 2, 0, scrollableHeight);
                float percent = (float) thumbTop / (float) scrollableHeight;
                int newOffset = Math.round(percent * maxCategoryScroll);
                categoryScrollOffset = MathHelper.clamp(newOffset, 0, maxCategoryScroll);
            }
        }
    }

    public static void handleMouseRelease(int mouseX, int mouseY, int mouseButton) {
        ScaledResolution res = new ScaledResolution(Minecraft.getInstance());
        int scaledWidth = res.getScaledWidth();
        int scaledHeight = res.getScaledHeight();
        int scaledMouseX = mouseX;
        int scaledMouseY = mouseY;
        if (mouseX > scaledWidth || mouseY > scaledHeight) {
            scaledMouseX = scaleRawMouseX(mouseX, scaledWidth);
            scaledMouseY = scaleRawMouseY(mouseY, scaledHeight);
        }

        if (mouseButton == 0 && isDraggingMasterStatusHud) {
            isDraggingMasterStatusHud = false;
            MovementFeatureManager.persistMasterStatusHudPosition();
        }

        if (mouseButton == 0 && pressedCategoryRow != null) {
            if (isDraggingCategoryRow && currentCategorySortDropTarget != null
                    && canReorderCategoryRows(pressedCategoryRow, currentCategorySortDropTarget)) {
                if (pressedCategoryRow.isCustomCategoryRoot()) {
                    PathSequenceManager.moveCategory(pressedCategoryRow.category,
                            currentCategorySortDropTarget.category, currentCategorySortDropAfter);
                } else if (pressedCategoryRow.isSubCategory()) {
                    MainUiLayoutManager.moveSubCategory(pressedCategoryRow.category, pressedCategoryRow.subCategory,
                            currentCategorySortDropTarget.subCategory, currentCategorySortDropAfter);
                }
                refreshGuiLists();
            } else if (!isDraggingCategoryRow && pressedCategoryRowRect != null
                    && pressedCategoryRowRect.contains(scaledMouseX, scaledMouseY)) {
                isDebugRecordingMenuVisible = false;
                CATEGORY_PAGE_MAP.put(getCurrentPageKey(), currentPage);
                sLastPage = currentPage;
                sLastCategory = currentCategory;
                currentCategory = pressedCategoryRow.category;
                currentCustomSubCategory = pressedCategoryRow.subCategory;
                clearSelectedCustomSequences();
                if (!I18n.format("gui.inventory.category.builtin_script").equals(currentCategory)) {
                    builtinScriptPrimaryCategory = null;
                    builtinScriptSubCategory = null;
                }
                syncCurrentCustomCategoryState();
                currentPage = CATEGORY_PAGE_MAP.getOrDefault(getCurrentPageKey(),
                        getDefaultPageForCategory(currentCategory));
            }
        }

        if (mouseButton == 0 && pressedCustomSequence != null) {
            if (isDraggingCustomSequenceCard && !normalizeText(currentCustomSequenceSortTargetName).isEmpty()) {
                boolean reordered = PathSequenceManager.moveCustomSequenceRelative(pressedCustomSequence.getName(),
                        currentCustomSequenceSortTargetName, currentCustomSequenceSortAfter);
                if (reordered) {
                    MainUiLayoutManager.setSortMode(pressedCustomSequence.getCategory(),
                            MainUiLayoutManager.SORT_DEFAULT);
                    refreshGuiLists();
                }
            } else if (isDraggingCustomSequenceCard && currentSequenceDropTarget != null) {
                String targetCategory = currentSequenceDropTarget.category;
                String targetSubCategory = currentSequenceDropTarget.subCategory;
                boolean sameTarget = normalizeText(targetCategory)
                        .equals(normalizeText(pressedCustomSequence.getCategory()))
                        && normalizeText(targetSubCategory)
                                .equalsIgnoreCase(normalizeText(pressedCustomSequence.getSubCategory()));
                if (!sameTarget) {
                    PathSequenceManager.moveCustomSequenceTo(pressedCustomSequence.getName(), targetCategory,
                            targetSubCategory);
                    currentCategory = targetCategory;
                    currentCustomSubCategory = targetSubCategory;
                    currentPage = 0;
                    refreshGuiLists();
                }
            } else if (!isDraggingCustomSequenceCard && pressedCustomSequenceRect != null
                    && pressedCustomSequenceRect.contains(scaledMouseX, scaledMouseY)) {
                activateSequence(pressedCustomSequence);
            }
        }

        clearPressedCategoryRow();
        clearPressedCustomSequence();
        isDraggingMerchantListScrollbar = false;
        isDraggingOtherFeatureGroupScrollbar = false;
        isDraggingCategoryScrollbar = false;
        isDraggingCategoryDivider = false;
    }

    public static void closeOverlay() {
        Minecraft mc = Minecraft.getInstance();
        setMasterStatusHudEditMode(false);
        zszlScriptMod.isGuiVisible = false;
        if (mc.screen instanceof GuiInventoryOverlayScreen) {
            mc.setScreen(null);
        } else if (mc.screen == null && !ModConfig.isMouseDetached) {
            mc.mouseHandler.grabMouse();
        }
    }

    private static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    private static void drawHorizontalLine(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        drawRect(startX, y, endX + 1, y + 1, color);
    }

    private static void drawVerticalLine(int x, int startY, int endY, int color) {
        if (endY < startY) {
            int i = startY;
            startY = endY;
            endY = i;
        }
        drawRect(x, startY + 1, x + 1, endY, color);
    }

    private static void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        int resolved = GuiTheme.resolveTextColor(text, color);
        fontRenderer.drawStringWithShadow(text, (float) (x - fontRenderer.getStringWidth(text) / 2), (float) y,
                resolved);
    }

    private static void drawCenteredString(net.minecraft.client.gui.Font fontRenderer, String text, int x, int y,
            int color) {
        drawCenteredString(wrapFont(fontRenderer), text, x, y, color);
    }

    private static void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        int resolved = GuiTheme.resolveTextColor(text, color);
        fontRenderer.drawStringWithShadow(text, (float) x, (float) y, resolved);
    }

    private static void drawString(net.minecraft.client.gui.Font fontRenderer, String text, int x, int y, int color) {
        drawString(wrapFont(fontRenderer), text, x, y, color);
    }

    private static FontRenderer wrapFont(net.minecraft.client.gui.Font font) {
        return new FontRenderer(font);
    }

}



