// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/GuiInventory.java
package com.zszl.zszlScriptMod.gui;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
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
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.CategoryDef;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.ExchangeDef;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.MerchantDef;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.config.GuiUtils;

abstract class GuiInventoryFeatureScreens extends GuiInventoryCustomSupport {
    protected static List<String> filterStandalonePathCategories(List<String> pathCategories) {
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

    protected static String getBuiltinRouteSubCategory(PathSequence sequence) {
        if (sequence == null || sequence.isCustom()) {
            return "";
        }
        String subCategory = sequence.getSubCategory();
        if (subCategory != null && !subCategory.trim().isEmpty()) {
            return subCategory.trim();
        }
        return "";
    }

    protected static String getBuiltinRoutePrimaryCategory(PathSequence sequence) {
        if (sequence == null || sequence.isCustom()) {
            return "";
        }
        String category = sequence.getCategory();
        if (category == null || category.trim().isEmpty()) {
            return "";
        }
        return category.trim();
    }

    protected static boolean isBuiltinMainScript(PathSequence sequence) {
        return sequence != null && !sequence.isCustom()
                && I18n.format("gui.inventory.category.builtin_path").equals(sequence.getCategory())
                && getBuiltinRouteSubCategory(sequence).isEmpty();
    }

    protected static boolean isBuiltinRouteSequence(PathSequence sequence) {
        return sequence != null && !sequence.isCustom() && !isBuiltinMainScript(sequence)
                && !getBuiltinRoutePrimaryCategory(sequence).isEmpty();
    }

    protected static boolean isBuiltinRouteCategory(String category) {
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

    protected static List<String> getBuiltinRouteSubCategories() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (PathSequence sequence : PathSequenceManager.getAllSequences()) {
            String subCategory = getBuiltinRouteSubCategory(sequence);
            if (!subCategory.isEmpty()) {
                result.add(subCategory);
            }
        }
        return new ArrayList<>(result);
    }

    protected static List<String> getBuiltinRoutePrimaryCategories() {
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

    protected static boolean shouldHideBuiltinRoutePrimaryCategory(String primaryCategory) {
        if (primaryCategory == null || primaryCategory.trim().isEmpty()) {
            return false;
        }
        String normalized = primaryCategory.trim();
        return ServerFeatureVisibilityManager.shouldHideMotaFeatures() && "魔塔之巅".equals(normalized);
    }

    protected static List<String> getBuiltinRouteSubCategoriesByPrimary(String primaryCategory) {
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

    protected static int getDefaultPageForCategory(String category) {
        if (!I18n.format("gui.inventory.category.builtin_script").equals(category)) {
            return 0;
        }
        List<String> items = categoryItems.get(category);
        int totalPages = (items != null) ? Math.max(1, (items.size() + 17) / 18) : 1;
        return totalPages > 1 ? 1 : 0;
    }

    protected static void reorderBuiltinScriptsForSecondPage(List<PathSequence> categorySequences) {
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

    protected static boolean isBuiltinSecondPageSequence(String name) {
        return name != null && getBuiltinSecondPageGroup().contains(name);
    }

    protected static int getMerchantTotalPages(MerchantDef merchant) {
        List<Integer> visibleIndices = getMerchantVisibleExchangeIndices(merchant);
        if (visibleIndices.isEmpty()) {
            return 1;
        }
        return Math.max(1, (visibleIndices.size() + 3) / 4);
    }

    protected static void normalizeMerchantCategoryState(MerchantDef merchant) {
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

    protected static List<Integer> getMerchantVisibleExchangeIndices(MerchantDef merchant) {
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

    protected static int getMerchantCategoryButtonWidth(FontRenderer fontRenderer, String text) {
        if (fontRenderer == null) {
            return 52;
        }
        return MathHelper.clamp(fontRenderer.getStringWidth(text) + 16, 52, 96);
    }

    protected static int getSafeCategoryListButtonWidth(OverlayMetrics m) {
        return Math.max(scaleUi(40, m.scale),
                Math.min(m.categoryButtonWidth, m.categoryPanelWidth - m.padding * 2 - scaleUi(8, m.scale)));
    }

    protected static int getTopButtonWidth(OverlayMetrics m, int buttonCount) {
        int gap = m.padding;
        int availableWidth = Math.max(scaleUi(160, m.scale), m.totalWidth - m.padding * 2);
        int fittedWidth = (availableWidth - gap * Math.max(0, buttonCount - 1)) / Math.max(1, buttonCount);
        return Math.max(scaleUi(40, m.scale), Math.min(m.pathManagerButtonWidth, fittedWidth));
    }

    protected static void normalizeOtherFeatureGroupState(List<GroupDef> groups) {
        if (groups == null || groups.isEmpty()) {
            selectedOtherFeatureGroupIndex = -1;
            otherFeatureGroupScrollOffset = 0;
            return;
        }
        if (selectedOtherFeatureGroupIndex < 0 || selectedOtherFeatureGroupIndex >= groups.size()) {
            selectedOtherFeatureGroupIndex = 0;
        }
        otherFeatureGroupScrollOffset = MathHelper.clamp(otherFeatureGroupScrollOffset, 0,
                Math.max(0, groups.size() - 1));
    }

    protected static boolean isOtherFeatureEnabled(String featureId) {
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

    protected static GuiTheme.UiState getOtherFeatureItemState(String featureId, boolean hover) {
        if (isOtherFeatureEnabled(featureId)) {
            return GuiTheme.UiState.SUCCESS;
        }
        return hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL;
    }

    protected static String getTranslatedOtherFeatureTooltip(String featureId) {
        if (featureId == null || featureId.trim().isEmpty()) {
            return null;
        }
        String key = "gui.inventory.other_feature." + featureId + ".tooltip";
        String translated = I18n.format(key);
        return key.equals(translated) ? null : translated;
    }

    protected static String buildOtherFeatureTooltip(FeatureDef feature, FontRenderer fontRenderer, int wrapWidth) {
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

    protected static String trimOverlayButtonLabel(FontRenderer fontRenderer, String text, int maxWidth) {
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

    protected static OtherFeaturePageControlBounds getOtherFeaturePageControlBounds(OverlayMetrics m, int contentX,
            int contentRight) {
        int pageButtonHeight = Math.max(18, m.itemButtonHeight);
        int pageAreaY = Math.max(m.contentStartY + m.padding,
                m.y + m.totalHeight - pageButtonHeight - Math.max(4, m.padding));
        int controlInset = Math.max(4, scaleUi(8, m.scale));
        int availableWidth = Math.max(1, contentRight - contentX - controlInset * 2);
        int controlGap = Math.max(2, Math.min(Math.max(4, m.padding + 2), Math.max(2, availableWidth / 12)));
        int minInfoWidth = Math.max(18, Math.min(Math.max(24, scaleUi(24, m.scale)), Math.max(18, availableWidth / 5)));
        int buttonWidth = Math.max(1, Math.min(m.pageButtonWidth, Math.max(24, availableWidth / 3)));
        int pageInfoWidth = availableWidth - buttonWidth * 2 - controlGap * 2;
        if (pageInfoWidth < minInfoWidth) {
            buttonWidth = Math.max(1, (availableWidth - minInfoWidth - controlGap * 2) / 2);
            pageInfoWidth = Math.max(1, availableWidth - buttonWidth * 2 - controlGap * 2);
        }
        int controlsWidth = Math.max(1, buttonWidth * 2 + pageInfoWidth + controlGap * 2);
        int controlsStartX = contentX + controlInset
                + Math.max(0, (availableWidth - controlsWidth) / 2);
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

    protected static boolean shiftOtherFeatureScreenPage(int delta, int totalPages) {
        int safeTotalPages = Math.max(1, totalPages);
        int targetPage = MathHelper.clamp(otherFeatureScreenPage + delta, 0, safeTotalPages - 1);
        if (targetPage == otherFeatureScreenPage) {
            return false;
        }
        otherFeatureScreenPage = targetPage;
        return true;
    }

    protected static OtherFeaturePageLayout buildOtherFeaturePageLayout(GroupDef group, OverlayMetrics m,
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

    protected static boolean handleOtherFeatureClick(FeatureDef feature, int mouseButton, Minecraft mc)
            throws IOException {
        if (feature == null) {
            return false;
        }

        if ("speed".equalsIgnoreCase(feature.id)) {
            if (mouseButton == 0) {
                SpeedHandler.INSTANCE.toggleEnabled();
            } else if (mouseButton == 1) {
                closeOverlay();
                mc.displayGuiScreen(new GuiSpeedConfig(null));
            }
            return true;
        }

        if (MovementFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                MovementFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                net.minecraft.client.gui.GuiScreen configScreen = MovementFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.displayGuiScreen(configScreen);
                }
            }
            return true;
        }

        if (BlockFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                BlockFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                net.minecraft.client.gui.GuiScreen configScreen = BlockFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.displayGuiScreen(configScreen);
                }
            }
            return true;
        }

        if (RenderFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                RenderFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                net.minecraft.client.gui.GuiScreen configScreen = RenderFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.displayGuiScreen(configScreen);
                }
            }
            return true;
        }

        if (WorldFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                WorldFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                net.minecraft.client.gui.GuiScreen configScreen = WorldFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.displayGuiScreen(configScreen);
                }
            }
            return true;
        }

        if (ItemFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                ItemFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                net.minecraft.client.gui.GuiScreen configScreen = ItemFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.displayGuiScreen(configScreen);
                }
            }
            return true;
        }

        if (MiscFeatureManager.isManagedFeature(feature.id)) {
            if (mouseButton == 0) {
                MiscFeatureManager.toggleFeature(feature.id);
            } else if (mouseButton == 1) {
                closeOverlay();
                net.minecraft.client.gui.GuiScreen configScreen = MiscFeatureGuiFactory.create(null, feature.id);
                if (configScreen != null) {
                    mc.displayGuiScreen(configScreen);
                }
            }
            return true;
        }

        return false;
    }

    protected static String getOtherFeaturesHudButtonLabel() {
        return "总状态HUD";
    }

    protected static String getOtherFeaturesHudEditButtonLabel() {
        return "调整HUD位置";
    }

    protected static int getOtherFeaturesHudButtonWidth(OverlayMetrics m, FontRenderer fontRenderer) {
        int textWidth = fontRenderer == null ? 0 : fontRenderer.getStringWidth(getOtherFeaturesHudButtonLabel());
        return Math.max(m.pathManagerButtonWidth, textWidth + scaleUi(18, m.scale));
    }

    protected static int getOtherFeaturesHudEditButtonWidth(OverlayMetrics m, FontRenderer fontRenderer) {
        int textWidth = fontRenderer == null ? 0 : fontRenderer.getStringWidth(getOtherFeaturesHudEditButtonLabel());
        return Math.max(m.pathManagerButtonWidth, textWidth + scaleUi(18, m.scale));
    }

    protected static String drawOtherFeaturesOverlay(OverlayMetrics m, int mouseX, int mouseY, FontRenderer fontRenderer,
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
            drawCenteredString(fontRenderer, trimOverlayButtonLabel(fontRenderer, group.name, leftBtnW - 10),
                    leftStartX + leftBtnW / 2,
                    by + (leftBtnH - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
        }

        if (maxOtherFeatureGroupScroll > 0) {
            int sbX = leftStartX + leftBtnW + 2;
            int sbY = topY;
            int sbW = 4;
            int sbH = leftListHeight;
            int thumbH = Math.max(12, (int) ((float) visibleGroupCount / groups.size() * sbH));
            int thumbY = sbY
                    + (int) ((otherFeatureGroupScrollOffset / (float) maxOtherFeatureGroupScroll) * (sbH - thumbH));
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

    protected static List<String> drawExchangeItemSlot(int x, int y, int size, ItemStack stack, int mouseX, int mouseY,
            FontRenderer fontRenderer) {
        GuiTheme.drawPanelSegment(x, y, size, size, x, y, size, size);
        Gui.drawRect(x + 1, y + 1, x + size - 1, y + size - 1, 0x55324458);
        if (stack == null || stack.isEmpty()) {
            drawCenteredString(fontRenderer, "-", x + size / 2, y + (size - fontRenderer.FONT_HEIGHT) / 2, 0xFFAAAAAA);
            return null;
        }

        RenderHelper.enableGUIStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, x + 1, y + 1);
        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(fontRenderer, stack, x + 1, y + 1, null);
        RenderHelper.disableStandardItemLighting();

        if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size
                && Minecraft.getMinecraft().player != null) {
            List<String> tooltips = stack.getTooltip(Minecraft.getMinecraft().player,
                    ITooltipFlag.TooltipFlags.ADVANCED);
            return (tooltips != null && !tooltips.isEmpty()) ? tooltips : null;
        }

        return null;
    }

    protected static void drawMerchantOverlay(OverlayMetrics m, int mouseX, int mouseY, FontRenderer fontRenderer,
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


}

