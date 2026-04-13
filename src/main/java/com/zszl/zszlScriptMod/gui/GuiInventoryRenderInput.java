// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/GuiInventory.java
package com.zszl.zszlScriptMod.gui;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.config.BaritoneParkourSettingsHelper;
import com.zszl.zszlScriptMod.gui.changelog.GuiChangelog;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
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
import com.zszl.zszlScriptMod.gui.config.GuiBaritoneParkourSettings;
import com.zszl.zszlScriptMod.gui.config.GuiBlockReplacementConfig;
import com.zszl.zszlScriptMod.gui.config.GuiChatOptimization;
import com.zszl.zszlScriptMod.gui.config.GuiConditionalExecutionManager;
import com.zszl.zszlScriptMod.gui.config.GuiDeathAutoRejoinConfig;
import com.zszl.zszlScriptMod.gui.config.GuiDebugConfig;
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
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;
import com.zszl.zszlScriptMod.handlers.AutoEquipHandler;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler;
import com.zszl.zszlScriptMod.handlers.AutoSigninOnlineHandler;
import com.zszl.zszlScriptMod.handlers.AutoSkillHandler;
import com.zszl.zszlScriptMod.handlers.AutoUseItemHandler;
import com.zszl.zszlScriptMod.handlers.ConditionalExecutionHandler;
import com.zszl.zszlScriptMod.handlers.DeathAutoRejoinHandler;
import com.zszl.zszlScriptMod.handlers.EmbeddedNavigationHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.handlers.KillTimerHandler;
import com.zszl.zszlScriptMod.handlers.ShulkerBoxStackingHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.inventory.InventoryViewerManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager;
import com.zszl.zszlScriptMod.otherfeatures.OtherFeatureGroupManager.GroupDef;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.system.AutoFollowRule;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.utils.AdExpListManager;
import com.zszl.zszlScriptMod.utils.CapturingFontRenderer;
import com.zszl.zszlScriptMod.utils.DonationLeaderboardManager;
import com.zszl.zszlScriptMod.utils.EnhancementAttrManager;
import com.zszl.zszlScriptMod.utils.HallOfFameManager;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.CategoryDef;
import com.zszl.zszlScriptMod.utils.MerchantExchangeManager.MerchantDef;
import com.zszl.zszlScriptMod.utils.TitleCompendiumManager;
import com.zszl.zszlScriptMod.utils.UpdateChecker;
import com.zszl.zszlScriptMod.utils.UpdateManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;

abstract class GuiInventoryRenderInput extends GuiInventoryFeatureScreens {
    public static void drawOverlay(int screenWidth, int screenHeight) {
        if (masterStatusHudEditMode) {
            return;
        }

        String tooltipToDraw = null;
        int tooltipMouseX = 0;
        int tooltipMouseY = 0;

        if (Minecraft.getMinecraft().fontRenderer instanceof CapturingFontRenderer) {
            ((CapturingFontRenderer) Minecraft.getMinecraft().fontRenderer).disableCapture();
        }

        try {
            colorChangeTicker++;

            // 每分钟检查一次远端更新；若有变化，顶部版本会实时刷新显示
            UpdateChecker.requestRefreshIfDue(60_000L);
            HallOfFameManager.requestRefreshIfDue(60_000L);
            TitleCompendiumManager.requestRefreshIfDue(60_000L);
            EnhancementAttrManager.requestRefreshIfDue(60_000L);
            AdExpListManager.requestRefreshIfDue(60_000L);
            DonationLeaderboardManager.requestRefreshIfDue(60_000L);

            FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
            String title = buildOverlayTitle();
            OverlayMetrics m = computeOverlayMetrics(screenWidth, screenHeight, fontRenderer, title);

            int mouseX = Mouse.getX() * screenWidth / Minecraft.getMinecraft().displayWidth;
            int mouseY = screenHeight - Mouse.getY() * screenHeight / Minecraft.getMinecraft().displayHeight - 1;

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
                    button.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, 0);
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
                TextFormatting color = RAINBOW_COLORS.get((i + tickerOffset) % RAINBOW_COLORS.size());
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
            int customSearchToggleX = customSearchHeaderActive
                    ? (otherFeaturesButtonX - pathManagerButtonWidth - m.padding)
                    : otherFeaturesButtonX;
            int titleAreaEndX = (merchantScreenActive || otherFeaturesScreenActive ? stopForegroundButtonX
                    : customSearchToggleX)
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
                drawCenteredString(Minecraft.getMinecraft().fontRenderer, I18n.format("gui.inventory.merchant.back"),
                        backButtonX + backButtonWidth / 2,
                        backButtonY + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                        I18n.format("gui.inventory.other_features.back"),
                        backButtonX + backButtonWidth / 2,
                        backButtonY + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                        0xFFFFFFFF);

                int masterHudButtonWidth = getOtherFeaturesHudButtonWidth(m, Minecraft.getMinecraft().fontRenderer);
                int masterHudButtonX = m.x + m.totalWidth - masterHudButtonWidth - m.padding;
                int masterHudButtonY = backButtonY;
                int editHudButtonWidth = getOtherFeaturesHudEditButtonWidth(m, Minecraft.getMinecraft().fontRenderer);
                int editHudButtonX = masterHudButtonX - editHudButtonWidth - m.padding;
                int editHudButtonY = backButtonY;
                boolean isHoveringEditHud = mouseX >= editHudButtonX && mouseX < editHudButtonX + editHudButtonWidth
                        && mouseY >= editHudButtonY && mouseY < editHudButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(editHudButtonX, editHudButtonY, editHudButtonWidth, m.topButtonHeight,
                        masterStatusHudEditMode ? GuiTheme.UiState.SELECTED
                                : (isHoveringEditHud ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
                drawCenteredString(Minecraft.getMinecraft().fontRenderer, getOtherFeaturesHudEditButtonLabel(),
                        editHudButtonX + editHudButtonWidth / 2,
                        editHudButtonY + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
                        0xFFFFFFFF);
                boolean masterHudEnabled = MovementFeatureManager.isMasterStatusHudEnabled();
                boolean isHoveringMasterHud = mouseX >= masterHudButtonX
                        && mouseX < masterHudButtonX + masterHudButtonWidth
                        && mouseY >= masterHudButtonY && mouseY < masterHudButtonY + m.topButtonHeight;
                GuiTheme.drawButtonFrame(masterHudButtonX, masterHudButtonY, masterHudButtonWidth, m.topButtonHeight,
                        masterHudEnabled ? GuiTheme.UiState.SUCCESS
                                : (isHoveringMasterHud ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
                drawCenteredString(Minecraft.getMinecraft().fontRenderer, getOtherFeaturesHudButtonLabel(),
                        masterHudButtonX + masterHudButtonWidth / 2,
                        masterHudButtonY
                                + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                drawCenteredString(Minecraft.getMinecraft().fontRenderer, I18n.format("gui.inventory.path_manager"),
                        pathManagerButtonX + pathManagerButtonWidth / 2,
                        pathManagerButtonY
                                + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                drawCenteredString(Minecraft.getMinecraft().fontRenderer, I18n.format("gui.inventory.other_features"),
                        otherFeaturesButtonX + otherFeaturesButtonWidth / 2,
                        otherFeaturesButtonY + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT)
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
                drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                        I18n.format("gui.inventory.stop_foreground"),
                        stopForegroundButtonX + stopForegroundButtonWidth / 2,
                        stopForegroundButtonY
                                + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                        I18n.format("gui.inventory.stop_background"),
                        stopBackgroundButtonX + stopBackgroundButtonWidth / 2,
                        stopBackgroundButtonY
                                + (m.topButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                        ? Math.max(contentStartY + scaleUi(24, m.scale),
                                pageControls.containerBounds.y - scaleUi(6, m.scale))
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
                    drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                            I18n.format("gui.inventory.debug.record_chest"), buttonX1 + itemButtonWidth / 2,
                            buttonY1 + (itemButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                    drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                            I18n.format("gui.inventory.debug.auto_equip"), buttonX2 + itemButtonWidth / 2,
                            buttonY2 + (itemButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                    drawCenteredString(Minecraft.getMinecraft().fontRenderer,
                            I18n.format("gui.inventory.debug.debug_keybind"), buttonX3 + itemButtonWidth / 2,
                            buttonY3 + (itemButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT) / 2,
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
                            } else if (command.equals("toggle_kill_aura")) {
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
                            else if (command.equals("baritone_parkour")
                                    && BaritoneParkourSettingsHelper.isParkourModeEnabled())
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
                                drawCenteredString(Minecraft.getMinecraft().fontRenderer, itemNames.get(index),
                                        buttonX + itemButtonWidth / 2, buttonY + 2, 0xFFFFFFFF);
                                String loopText = (loopCount < 0) ? I18n.format("gui.inventory.loop.infinite")
                                        : (loopCount == 0) ? I18n.format("gui.inventory.loop.off")
                                                : I18n.format("gui.inventory.loop.count", loopCount);
                                drawCenteredString(Minecraft.getMinecraft().fontRenderer, loopText,
                                        buttonX + itemButtonWidth / 2, buttonY + 11, 0xFFDDDDDD);
                            } else {
                                drawCenteredString(Minecraft.getMinecraft().fontRenderer, itemNames.get(index),
                                        buttonX + itemButtonWidth / 2,
                                        buttonY + (itemButtonHeight - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT)
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
                                drawString(Minecraft.getMinecraft().fontRenderer, "§fX", deleteX + 2, deleteY + 1,
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
            if (Minecraft.getMinecraft().fontRenderer instanceof CapturingFontRenderer) {
                ((CapturingFontRenderer) Minecraft.getMinecraft().fontRenderer).enableCapture();
            }
        }
    }

    public static void handleMouseClick(int rawMouseX, int rawMouseY, int mouseButton) throws IOException {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();
        Minecraft mc = Minecraft.getMinecraft();

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
            mc.displayGuiScreen(new GuiChangelog(null, UpdateChecker.changelogContent));
            return;
        }

        if (authorClickArea != null && authorClickArea.contains(mouseX, mouseY)) {
            try {
                Desktop.getDesktop().browse(new URI(
                        "https://qm.qq.com/cgi-bin/qm/qr?k=KpXtB7PNkQYan3sAx-eO4_wa8x9BIRhF&jump_from=webapi&authKey=X/HJE1j5AIGgsOP4zT/8r1SsTD6ptqo4A9/PmbJeeWd3lBolMoNWpCuDHyzxrQTj"));
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString("§a已打开QQ群添加链接"));
                }
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString("§c打开QQ群链接失败"));
                }
            }
            return;
        }

        for (GuiButton button : sideButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) {
                switch (button.id) {
                    case BTN_ID_THEME_CONFIG:
                        zszlScriptMod.isGuiVisible = false;
                        mc.displayGuiScreen(new GuiThemeManager(null));
                        break;
                    case BTN_ID_UPDATE:
                        UpdateManager.fetchUpdateLinkAndOpen();
                        break;
                    case BTN_ID_HALL_OF_FAME:
                        HallOfFameManager.forceRefresh();
                        UpdateChecker.forceRefresh();
                        zszlScriptMod.isGuiVisible = false;
                        mc.displayGuiScreen(new GuiHallOfFame(null, HallOfFameManager.content));
                        break;
                    case BTN_ID_TITLE_COMPENDIUM:
                        TitleCompendiumManager.forceRefresh();
                        zszlScriptMod.isGuiVisible = false;
                        mc.displayGuiScreen(GuiHallOfFame.createTitleCompendiumView(null));
                        break;
                    case BTN_ID_ENHANCEMENT_ATTR:
                        EnhancementAttrManager.forceRefresh();
                        zszlScriptMod.isGuiVisible = false;
                        mc.displayGuiScreen(GuiHallOfFame.createEnhancementAttrView(null));
                        break;
                    case BTN_ID_AD_EXP_LIST:
                        AdExpListManager.forceRefresh();
                        zszlScriptMod.isGuiVisible = false;
                        mc.displayGuiScreen(GuiHallOfFame.createAdExpListView(null));
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
                        mc.displayGuiScreen(new GuiDonationSupport(null));
                        break;
                    case BTN_ID_PERFORMANCE_MONITOR:
                        zszlScriptMod.isGuiVisible = false;
                        mc.displayGuiScreen(new GuiPerformanceMonitor());
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
                        int btnW = getMerchantCategoryButtonWidth(mc.fontRenderer, label);
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

            int masterHudButtonWidth = getOtherFeaturesHudButtonWidth(m, mc.fontRenderer);
            int masterHudButtonX = x + totalWidth - masterHudButtonWidth - m.padding;
            int masterHudButtonY = backButtonY;
            int editHudButtonWidth = getOtherFeaturesHudEditButtonWidth(m, mc.fontRenderer);
            int editHudButtonX = masterHudButtonX - editHudButtonWidth - m.padding;
            int editHudButtonY = backButtonY;
            if (isMouseOver(mouseX, mouseY, editHudButtonX, editHudButtonY, editHudButtonWidth, m.topButtonHeight)) {
                setMasterStatusHudEditMode(!masterStatusHudEditMode);
                return;
            }
            if (isMouseOver(mouseX, mouseY, masterHudButtonX, masterHudButtonY, masterHudButtonWidth,
                    m.topButtonHeight)) {
                MovementFeatureManager.setMasterStatusHudEnabled(!MovementFeatureManager.isMasterStatusHudEnabled());
                return;
            }

            if (masterStatusHudEditMode) {
                if (masterStatusHudExitButtonBounds != null
                        && masterStatusHudExitButtonBounds.contains(mouseX, mouseY)) {
                    setMasterStatusHudEditMode(false);
                    return;
                }
                if (mouseButton == 0 && masterStatusHudEditorBounds != null
                        && masterStatusHudEditorBounds.contains(mouseX, mouseY)) {
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
                OtherFeaturePageLayout pageLayout = buildOtherFeaturePageLayout(selectedGroup, m, mc.fontRenderer);
                otherFeatureScreenPage = pageLayout.currentPage;
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
            EmbeddedNavigationHandler.INSTANCE.stop();
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
            mc.displayGuiScreen(new GuiPathManager());
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
                mc.displayGuiScreen(new GuiCustomPathCreator());
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
                    mc.displayGuiScreen(new GuiAutoEquipManager(null));
                }
                return;
            }

            int buttonX3 = buttonX2 + itemButtonWidth + m.gap;
            int buttonY3 = itemAreaStartY + 10;
            if (isMouseOver(mouseX, mouseY, buttonX3, buttonY3, itemButtonWidth, itemButtonHeight)) {
                closeOverlay();
                mc.displayGuiScreen(new GuiDebugKeybindManager(null));
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
                        EmbeddedNavigationHandler.INSTANCE.stop();
                        PathSequenceEventListener.instance.stopTracking();
                        isLooping = false;
                        commandHandled = true;
                    } else if (command.equals("setloop")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiLoopCountInput(null));
                        commandHandled = true;
                    }
                } else if (currentCategory.equals(I18n.format("gui.inventory.category.common"))
                        || currentCategory.equals(I18n.format("gui.inventory.category.rsl"))) {
                    if (command.equals("profile_manager")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiProfileManager(null));
                    } else if (command.equals("quick_exchange_config")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiQuickExchangeConfig(null));
                    } else if (command.equals("chat_optimization")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiChatOptimization(null));
                    } else if (command.equals("toggle_auto_pickup")) {
                        if (mouseButton == 0) {
                            AutoPickupHandler.globalEnabled = !AutoPickupHandler.globalEnabled;
                            AutoPickupHandler.saveConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoPickupConfig(null));
                        }
                    } else if (command.equals("toggle_auto_use_item")) {
                        if (mouseButton == 0) {
                            AutoUseItemHandler.globalEnabled = !AutoUseItemHandler.globalEnabled;
                            AutoUseItemHandler.INSTANCE.resetSchedule();
                            AutoUseItemHandler.saveConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoUseItemConfig(null));
                        }
                    } else if (command.equals("block_replacement_config")) {
                        if (mouseButton == 0 || mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiBlockReplacementConfig(null));
                        }
                    } else if (command.equals("toggle_server_feature_visibility")) {
                        if (mouseButton == 0 || mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiServerFeatureVisibilityConfig(null));
                        }
                    } else if (command.equals("setloop")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiLoopCountInput(null));
                    } else if (command.equals("autoeat")) {
                        if (mouseButton == 0) {
                            AutoEatHandler.autoEatEnabled = !AutoEatHandler.autoEatEnabled;
                            AutoEatHandler.saveAutoEatConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoEatConfig(null));
                        }
                    } else if (command.equals("toggle_auto_fishing")) {
                        if (mouseButton == 0) {
                            AutoFishingHandler.INSTANCE.toggleEnabled();
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoFishingConfig(null));
                        }
                    } else if (command.equals("autoskill")) {
                        if (mouseButton == 0) {
                            AutoSkillHandler.autoSkillEnabled = !AutoSkillHandler.autoSkillEnabled;
                            AutoSkillHandler.saveSkillConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoSkillEditor(null));
                        }
                    } else if (command.equals("signin_online_rewards")) {
                        if (mouseButton == 0) {
                            AutoSigninOnlineHandler.enabled = !AutoSigninOnlineHandler.enabled;
                            AutoSigninOnlineHandler.saveConfig();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoSigninOnlineConfig(null));
                        }
                    } else if (command.equals("toggle_kill_aura")) {
                        if (mouseButton == 0) {
                            KillAuraHandler.INSTANCE.toggleEnabled();
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiKillAuraConfig(null));
                        }
                    } else if (command.equals("toggle_kill_timer")) {
                        if (mouseButton == 0) {
                            KillTimerHandler.toggleEnabled();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiKillTimerConfig(null));
                        }
                        refreshGuiLists();
                    } else if (command.equals("toggle_ad_exp_panel")) {
                        if (mouseButton == 0) {
                            AdExpPanelHandler.toggleEnabled();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAdExpPanelConfig(null));
                        }
                        refreshGuiLists();
                    } else if (command.equals("toggle_mouse_detach")) {
                        ModConfig.isMouseDetached = !ModConfig.isMouseDetached;
                        String mouseStatus = ModConfig.isMouseDetached ? I18n.format("gui.inventory.mouse.detached")
                                : I18n.format("gui.inventory.mouse.reattached");
                        if (mc.player != null) {
                            mc.player.sendMessage(
                                    new TextComponentString(I18n.format("msg.inventory.mouse_toggle", mouseStatus)));
                        }
                        if (!ModConfig.isMouseDetached && mc.currentScreen == null) {
                            mc.mouseHelper.grabMouseCursor();
                        }
                        refreshGuiLists();
                    } else if (command.equals("followconfig")) {
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
                                        mc.player.sendMessage(
                                                new TextComponentString("§b[自动追怪] §e未配置任何规则，请右键打开配置界面"));
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
                    } else if (command.equals("conditional_execution")) {
                        if (mouseButton == 0) {
                            ConditionalExecutionHandler
                                    .setGlobalEnabled(!ConditionalExecutionHandler.isGloballyEnabled());
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiConditionalExecutionManager(null));
                        }
                    } else if (command.equals("auto_escape")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiAutoEscapeManager(null));
                    } else if (command.equals("toggle_death_auto_rejoin")) {
                        if (mouseButton == 0) {
                            DeathAutoRejoinHandler.deathAutoRejoinEnabled = !DeathAutoRejoinHandler.deathAutoRejoinEnabled;
                            DeathAutoRejoinHandler.saveConfig();
                            if (mc.player != null) {
                                String status = DeathAutoRejoinHandler.deathAutoRejoinEnabled
                                        ? I18n.format("gui.inventory.death_auto_rejoin.enabled")
                                        : I18n.format("gui.inventory.death_auto_rejoin.disabled");
                                mc.player.sendMessage(new TextComponentString(
                                        I18n.format("msg.inventory.death_auto_rejoin_status", status)));
                            }
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiDeathAutoRejoinConfig(null));
                        }
                    } else if (command.equals("keybind_manager")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiKeybindManager(null));
                    } else if (command.equals("warehouse_manager")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiWarehouseManager(null));
                    } else if (command.equals("baritone_settings")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiBaritoneCommandTable(null));
                    } else if (command.equals("baritone_parkour")) {
                        if (mouseButton == 0) {
                            BaritoneParkourSettingsHelper.toggleParkourMode();
                            refreshGuiLists();
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiBaritoneParkourSettings(null));
                        }
                    } else if (command.equals("toggle_auto_stack_shulker_boxes")) {
                        if (mouseButton == 0) {
                            ShulkerBoxStackingHandler.autoStackingEnabled = !ShulkerBoxStackingHandler.autoStackingEnabled;
                            ShulkerBoxStackingHandler.saveConfig();
                            if (mc.player != null) {
                                String status = ShulkerBoxStackingHandler.autoStackingEnabled
                                        ? I18n.format("gui.inventory.autostack.enabled")
                                        : I18n.format("gui.inventory.autostack.disabled");
                                mc.player.sendMessage(
                                        new TextComponentString(I18n.format("msg.inventory.autostack_status", status)));
                            }
                        } else if (mouseButton == 1) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiAutoStackingConfig(null));
                        }
                    }
                    commandHandled = true;
                } else if (currentCategory.equals(I18n.format("gui.inventory.category.debug"))) {
                    if (command.equals("debug_settings")) {
                        if (mouseButton == 0) {
                            closeOverlay();
                            mc.displayGuiScreen(new GuiDebugConfig());
                        }
                    } else if (command.equals("memory_manager")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiMemoryManager());
                    } else if (command.equals("player_equipment_viewer")) {
                        closeOverlay();
                        mc.addScheduledTask(() -> {
                            if (mouseButton == 0) {
                                InventoryViewerManager.copyInventoryFromTarget();
                            }
                            mc.player.openGui(zszlScriptMod.instance, GuiHandler.INVENTORY_VIEWER, mc.world, 0, 0, 0);
                        });
                    } else if (command.equals("packet_handler")) {
                        closeOverlay();
                        mc.displayGuiScreen(new com.zszl.zszlScriptMod.gui.packet.GuiPacketMain(null));
                    } else if (command.equals("gui_inspector_manager")) {
                        closeOverlay();
                        mc.displayGuiScreen(new com.zszl.zszlScriptMod.gui.debug.GuiGuiInspectorManager(null));
                    } else if (command.equals("performance_monitor")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiPerformanceMonitor());
                    } else if (command.equals("current_resolution_info")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiResolutionConfig());
                    } else if (command.equals("reload_paths")) {
                        PathSequenceManager.initializePathSequences();
                        if (mc.player != null) {
                            mc.player.sendMessage(new TextComponentString(
                                    TextFormatting.GREEN + I18n.format("msg.inventory.paths_reloaded")));
                        }
                        refreshGuiLists();
                    } else if (command.equals("terrain_scanner")) {
                        closeOverlay();
                        mc.displayGuiScreen(new GuiTerrainScannerManager(null));
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
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
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
                    return;
                }

                GroupDef selectedGroup = groups.get(selectedOtherFeatureGroupIndex);
                OtherFeaturePageLayout pageLayout = buildOtherFeaturePageLayout(selectedGroup, m,
                        Minecraft.getMinecraft().fontRenderer);
                otherFeatureScreenPage = pageLayout.currentPage;
                boolean inCardArea = pageLayout.cardAreaBounds.contains(mouseX, mouseY);
                boolean inPageControls = pageLayout.pageControls.containerBounds.contains(mouseX, mouseY);
                if (inCardArea || inPageControls) {
                    shiftOtherFeatureScreenPage(dWheel > 0 ? -1 : 1, pageLayout.totalPages);
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
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
        OverlayMetrics m = getCurrentOverlayMetrics(res.getScaledWidth(), res.getScaledHeight());

        if (isDraggingMasterStatusHud) {
            int hudWidth = masterStatusHudEditorBounds == null ? 140 : masterStatusHudEditorBounds.width;
            int hudHeight = masterStatusHudEditorBounds == null ? 60 : masterStatusHudEditorBounds.height;
            int newX = MathHelper.clamp(mouseX - masterStatusHudDragOffsetX + 4, 0,
                    Math.max(0, res.getScaledWidth() - hudWidth + 4));
            int newY = MathHelper.clamp(mouseY - masterStatusHudDragOffsetY + 4, 0,
                    Math.max(0, res.getScaledHeight() - hudHeight + 4));
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

    public static void handleMouseRelease(int rawMouseX, int rawMouseY, int mouseButton) {
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
        int scaledMouseX = scaleRawMouseX(rawMouseX, res.getScaledWidth());
        int scaledMouseY = scaleRawMouseY(rawMouseY, res.getScaledHeight());
        handleMouseReleaseScaled(scaledMouseX, scaledMouseY, mouseButton);
    }

    public static void handleMouseReleaseScaled(int scaledMouseX, int scaledMouseY, int mouseButton) {

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


}

