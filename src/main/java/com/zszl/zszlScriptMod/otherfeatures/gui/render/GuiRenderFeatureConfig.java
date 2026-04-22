package com.zszl.zszlScriptMod.otherfeatures.gui.render;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager.FeatureState;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GuiRenderFeatureConfig extends ThemedGuiScreen {

    private static final int CONTROL_HEIGHT = 20;
    private static final int SCROLLBAR_TRACK_WIDTH = 4;
    private static final int SCROLLBAR_GUTTER = 10;
    private static final int BTN_ENABLED = 1;
    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;
    private static final int OPTION_BUTTON_START = 200;

    private final GuiScreen parentScreen;
    private final String featureId;
    private final List<OptionSlot> optionSlots = new ArrayList<>();
    private final List<String> introLines = new ArrayList<>();
    private final List<String> descriptionLines = new ArrayList<>();

    private ToggleGuiButton enabledButton;
    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private boolean draftInitialized;
    private boolean draftEnabled;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int footerY;
    private int contentScrollOffset;
    private int maxContentScroll;
    private boolean draggingScrollbar;
    private int scrollbarDragOffsetY;
    private int stateHintY;
    private int stateSectionBottom;
    private int optionsSectionY;
    private int optionsHintY;
    private int optionsSectionBottom;
    private int infoSectionY;
    private int infoRuntimeY;
    private int infoDraftY;
    private int infoSectionBottom;

    private static final class OptionSlot {
        private final String key;
        private final GuiButton button;
        private String noteText = "";
        private int noteY = Integer.MIN_VALUE;

        private OptionSlot(String key, GuiButton button) {
            this.key = key;
            this.button = button;
        }
    }

    public GuiRenderFeatureConfig(GuiScreen parentScreen, String featureId) {
        this.parentScreen = parentScreen;
        this.featureId = featureId == null ? "" : featureId.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.optionSlots.clear();
        this.draggingScrollbar = false;
        if (!this.draftInitialized) {
            this.contentScrollOffset = 0;
        }
        this.maxContentScroll = 0;
        computeLayout();
        initControls();
        if (!this.draftInitialized) {
            loadDraftFromFeature();
            this.draftInitialized = true;
        }
        relayoutControls();
        refreshButtonTexts();
    }

    private void loadDraftFromFeature() {
        FeatureState feature = getFeature();
        this.draftEnabled = feature != null && feature.isEnabled();
    }

    private FeatureState getFeature() {
        return RenderFeatureManager.getFeature(this.featureId);
    }

    private void rebuildIntro(FeatureState feature) {
        this.introLines.clear();
        String intro = feature == null
                ? "当前功能未找到，无法读取对应的渲染配置。"
                : "左键其他功能页中的“" + feature.name + "”可快速切换总开关；这里可继续细调这个渲染功能的显示参数，界面与移动配置页保持同一套交互风格。";
        int wrapWidth = Math.max(120, this.panelWidth - 32);
        this.introLines.addAll(this.fontRenderer.listFormattedStringToWidth(intro, wrapWidth));
    }

    private void rebuildDescription(FeatureState feature) {
        this.descriptionLines.clear();
        String text = feature == null
                ? "当前功能不可用，无法显示详细说明。"
                : "§7" + feature.description + " 这里的草稿会先改动运行中的渲染参数，点“保存并关闭”后再写入配置。";
        int wrapWidth = Math.max(120, this.panelWidth - 36);
        this.descriptionLines.addAll(this.fontRenderer.listFormattedStringToWidth(text, wrapWidth));
    }

    private void computeLayout() {
        this.panelWidth = Math.min(540, Math.max(380, this.width - 12));
        FeatureState feature = getFeature();
        rebuildIntro(feature);
        rebuildDescription(feature);

        int optionCount = Math.max(1, buildOptionKeys().size());
        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int introHeight = this.introLines.size() * textStep;
        int infoBodyHeight = Math.max(40,
                this.descriptionLines.size() * textStep + 6 + this.fontRenderer.FONT_HEIGHT + 6 + this.fontRenderer.FONT_HEIGHT);
        int numericNotes = countNumericOptions();
        int requiredHeight = 24 + introHeight + 8 + 18 + 10
                + 18 + textStep + 4 + CONTROL_HEIGHT
                + 12 + 18 + textStep + 4 + optionCount * (CONTROL_HEIGHT + 6) + numericNotes * (this.fontRenderer.FONT_HEIGHT + 4)
                + 12 + 18 + infoBodyHeight
                + 8 + 28;
        int maxHeight = Math.max(300, this.height - 8);
        this.panelHeight = Math.min(Math.max(360, requiredHeight), maxHeight);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.contentTop = this.panelY + 24 + introHeight + 8;
        this.footerY = this.panelY + this.panelHeight - 28;
        this.contentBottom = this.footerY - 8;
    }

    private int countNumericOptions() {
        int count = 0;
        for (String key : buildOptionKeys()) {
            if (!isBooleanOption(key) && !getOptionRangeText(key).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void initControls() {
        FeatureState feature = getFeature();
        this.enabledButton = new ToggleGuiButton(BTN_ENABLED, 0, 0, 100, CONTROL_HEIGHT, "", this.draftEnabled);
        this.saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, CONTROL_HEIGHT, "§a保存并关闭");
        this.defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, CONTROL_HEIGHT, "§e恢复默认");
        this.cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, CONTROL_HEIGHT, "取消");

        this.buttonList.add(this.enabledButton);
        this.buttonList.add(this.saveButton);
        this.buttonList.add(this.defaultButton);
        this.buttonList.add(this.cancelButton);

        for (String key : buildOptionKeys()) {
            GuiButton button = isBooleanOption(key)
                    ? new ToggleGuiButton(OPTION_BUTTON_START + this.optionSlots.size(), 0, 0, 100, CONTROL_HEIGHT, "", false)
                    : new ThemedButton(OPTION_BUTTON_START + this.optionSlots.size(), 0, 0, 100, CONTROL_HEIGHT, "");
            button.enabled = feature != null;
            this.optionSlots.add(new OptionSlot(key, button));
            this.buttonList.add(button);
        }
    }

    private void relayoutControls() {
        computeLayout();

        FeatureState feature = getFeature();
        boolean featureAvailable = feature != null;
        int innerPadding = 12;
        int sectionGap = 12;
        int sectionHeaderHeight = 18;
        int textStep = this.fontRenderer.FONT_HEIGHT + 2;
        int fieldX = this.panelX + innerPadding;
        int fieldWidth = this.panelWidth - innerPadding * 2 - SCROLLBAR_GUTTER;
        int layoutBaseY = getScrollableViewportTop();
        int visibleContentHeight = getScrollableViewportHeight();

        for (int pass = 0; pass < 2; pass++) {
            int baseY = layoutBaseY - this.contentScrollOffset;

            this.stateHintY = baseY + sectionHeaderHeight;
            int enabledY = this.stateHintY + textStep + 4;
            this.enabledButton.x = fieldX;
            this.enabledButton.y = enabledY;
            this.enabledButton.width = fieldWidth;
            this.enabledButton.height = CONTROL_HEIGHT;
            this.enabledButton.enabled = featureAvailable;
            this.stateSectionBottom = enabledY + CONTROL_HEIGHT;

            this.optionsSectionY = this.stateSectionBottom + sectionGap;
            this.optionsHintY = this.optionsSectionY + sectionHeaderHeight;
            int currentY = this.optionsHintY + textStep + 4;
            for (OptionSlot slot : this.optionSlots) {
                slot.button.x = fieldX;
                slot.button.y = currentY;
                slot.button.width = fieldWidth;
                slot.button.height = CONTROL_HEIGHT;
                slot.button.enabled = featureAvailable;
                currentY += CONTROL_HEIGHT;

                slot.noteText = getOptionRangeText(slot.key);
                if (!slot.noteText.isEmpty()) {
                    slot.noteY = currentY + 4;
                    currentY += 4 + this.fontRenderer.FONT_HEIGHT;
                } else {
                    slot.noteY = Integer.MIN_VALUE;
                }
                currentY += 6;
            }
            this.optionsSectionBottom = Math.max(this.optionsHintY + textStep + 4, currentY - 6);

            this.infoSectionY = this.optionsSectionBottom + sectionGap;
            currentY = this.infoSectionY + sectionHeaderHeight;
            currentY += this.descriptionLines.size() * textStep;
            currentY += 6;
            this.infoRuntimeY = currentY;
            currentY += this.fontRenderer.FONT_HEIGHT + 6;
            this.infoDraftY = currentY;
            currentY += this.fontRenderer.FONT_HEIGHT;
            this.infoSectionBottom = currentY;

            int totalContentHeight = this.infoSectionBottom - layoutBaseY + 18;
            this.maxContentScroll = Math.max(0, totalContentHeight - visibleContentHeight);
            int clampedScroll = clampInt(this.contentScrollOffset, 0, this.maxContentScroll);
            if (clampedScroll == this.contentScrollOffset) {
                break;
            }
            this.contentScrollOffset = clampedScroll;
        }

        this.enabledButton.visible = featureAvailable
                && isVisibleInScrollableViewport(this.enabledButton.y, this.enabledButton.height);
        for (OptionSlot slot : this.optionSlots) {
            slot.button.visible = isVisibleInScrollableViewport(slot.button.y, slot.button.height);
        }
        layoutFooterButtons();
    }

    private void layoutFooterButtons() {
        int gap = 6;
        int footerButtonW = Math.max(64, (this.panelWidth - 24 - gap * 2) / 3);
        int totalW = footerButtonW * 3 + gap * 2;
        int startX = this.panelX + (this.panelWidth - totalW) / 2;
        layoutFooterButton(this.saveButton, startX, this.footerY, footerButtonW);
        layoutFooterButton(this.defaultButton, startX + footerButtonW + gap, this.footerY, footerButtonW);
        layoutFooterButton(this.cancelButton, startX + (footerButtonW + gap) * 2, this.footerY, footerButtonW);
    }

    private void layoutFooterButton(GuiButton button, int x, int y, int width) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = CONTROL_HEIGHT;
        button.visible = true;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_ENABLED) {
            this.draftEnabled = !this.draftEnabled;
            RenderFeatureManager.setEnabledTransient(this.featureId, this.draftEnabled);
        } else if (button.id == BTN_SAVE) {
            RenderFeatureManager.setEnabledTransient(this.featureId, this.draftEnabled);
            RenderFeatureManager.saveConfig();
            this.mc.setScreen(this.parentScreen);
            return;
        } else if (button.id == BTN_DEFAULT) {
            this.draftEnabled = false;
            RenderFeatureManager.setEnabledTransient(this.featureId, false);
            RenderFeatureManager.applyFeatureDefaultsWithoutSave(this.featureId);
        } else if (button.id == BTN_CANCEL) {
            RenderFeatureManager.loadConfig();
            this.mc.setScreen(this.parentScreen);
            return;
        } else {
            handleOptionButton(button.id - OPTION_BUTTON_START);
        }
        refreshButtonTexts();
        relayoutControls();
    }

    private void handleOptionButton(int optionIndex) {
        if (optionIndex < 0 || optionIndex >= this.optionSlots.size()) {
            return;
        }
        String key = this.optionSlots.get(optionIndex).key;
        if (isBooleanOption(key)) {
            toggleBooleanOption(key);
        } else if ("gamma".equals(key)) {
            openFloatInput("输入 Gamma 值 (1.0 - 16.0)", RenderFeatureManager.brightnessGamma, 1.0F, 16.0F,
                    value -> RenderFeatureManager.brightnessGamma = value);
        } else if ("edit_blocks".equals(key)) {
            this.mc.setScreen(new GuiXrayBlockEditor(this));
        } else if ("max_distance".equals(key)) {
            openDistanceInput();
        } else if ("line_width".equals(key)) {
            openLineWidthInput();
        } else if ("max_steps".equals(key)) {
            openIntInput("输入轨迹步数 (20 - 320)", RenderFeatureManager.trajectoryMaxSteps, 20, 320,
                    value -> RenderFeatureManager.trajectoryMaxSteps = value);
        } else if ("color".equals(key)) {
            openColorInput();
        } else if ("size".equals(key)) {
            openSizeInput();
        } else if ("thickness".equals(key)) {
            openFloatInput("输入准星粗细 (1.0 - 6.0)", RenderFeatureManager.crosshairThickness, 1.0F, 6.0F,
                    value -> RenderFeatureManager.crosshairThickness = value);
        }
        refreshButtonTexts();
    }

    private List<String> buildOptionKeys() {
        List<String> keys = new ArrayList<>();
        switch (this.featureId) {
        case "brightness_boost":
            keys.add("soft_mode");
            keys.add("gamma");
            break;
        case "no_fog":
            keys.add("remove_liquid");
            keys.add("brighten_color");
            break;
        case "entity_visual":
            keys.add("players");
            keys.add("monsters");
            keys.add("animals");
            keys.add("through_walls");
            keys.add("filled_box");
            keys.add("max_distance");
            break;
        case "tracer_line":
            keys.add("players");
            keys.add("monsters");
            keys.add("animals");
            keys.add("through_walls");
            keys.add("max_distance");
            keys.add("line_width");
            break;
        case "entity_tags":
            keys.add("players");
            keys.add("monsters");
            keys.add("animals");
            keys.add("show_health");
            keys.add("show_distance");
            keys.add("show_held_item");
            keys.add("max_distance");
            break;
        case "block_highlight":
            keys.add("storages");
            keys.add("spawners");
            keys.add("ores");
            keys.add("through_walls");
            keys.add("filled_box");
            keys.add("max_distance");
            break;
        case "xray":
            keys.add("edit_blocks");
            break;
        case "item_esp":
            keys.add("show_name");
            keys.add("show_distance");
            keys.add("through_walls");
            keys.add("max_distance");
            break;
        case "trajectory_line":
            keys.add("bows");
            keys.add("pearls");
            keys.add("throwables");
            keys.add("potions");
            keys.add("max_steps");
            break;
        case "custom_crosshair":
            keys.add("dynamic_gap");
            keys.add("color");
            keys.add("size");
            keys.add("thickness");
            break;
        case "anti_bob":
            keys.add("remove_view_bobbing");
            keys.add("remove_hurt_shake");
            break;
        case "radar":
            keys.add("players");
            keys.add("monsters");
            keys.add("animals");
            keys.add("rotate_with_view");
            keys.add("max_distance");
            keys.add("size");
            break;
        case "player_skeleton":
            keys.add("through_walls");
            keys.add("line_width");
            keys.add("max_distance");
            break;
        case "block_outline":
            keys.add("filled_box");
            keys.add("line_width");
            break;
        case "entity_info":
            keys.add("show_health");
            keys.add("show_distance");
            keys.add("show_position");
            keys.add("show_held_item");
            keys.add("max_distance");
            break;
        default:
            break;
        }
        return keys;
    }

    private boolean isBooleanOption(String key) {
        return !"gamma".equals(key)
                && !"edit_blocks".equals(key)
                && !"max_distance".equals(key)
                && !"line_width".equals(key)
                && !"max_steps".equals(key)
                && !"color".equals(key)
                && !"size".equals(key)
                && !"thickness".equals(key);
    }

    private void refreshButtonTexts() {
        FeatureState feature = getFeature();
        boolean featureAvailable = feature != null;
        if (this.enabledButton != null) {
            this.enabledButton.setEnabledState(this.draftEnabled);
            this.enabledButton.enabled = featureAvailable;
            this.enabledButton.displayString = (feature == null ? "功能总开关" : feature.name + " 总开关")
                    + " : " + (this.draftEnabled ? "开启" : "关闭");
        }
        if (this.saveButton != null) {
            this.saveButton.enabled = featureAvailable;
        }
        if (this.defaultButton != null) {
            this.defaultButton.enabled = featureAvailable;
        }
        for (OptionSlot slot : this.optionSlots) {
            slot.button.enabled = featureAvailable && this.draftEnabled;
            slot.button.displayString = buildOptionButtonLabel(slot.key);
            if (slot.button instanceof ToggleGuiButton) {
                ((ToggleGuiButton) slot.button).setEnabledState(getBooleanOptionValue(slot.key));
            }
        }
    }

    private String buildOptionButtonLabel(String key) {
        if (isBooleanOption(key)) {
            return getOptionLabel(key) + " : " + (getBooleanOptionValue(key) ? "开启" : "关闭");
        }
        switch (key) {
        case "gamma":
            return "Gamma : " + formatFloat(RenderFeatureManager.brightnessGamma);
        case "edit_blocks":
            return "透视方块 : " + RenderFeatureManager.getXrayVisibleBlockIds().size() + " 项";
        case "max_distance":
            return "最大范围 : " + formatFloat(getDistanceValue()) + " 格";
        case "line_width":
            return ("player_skeleton".equals(this.featureId) ? "骨架线宽" :
                    "block_outline".equals(this.featureId) ? "边框线宽" : "线宽")
                    + " : " + formatFloat(getLineWidthValue());
        case "max_steps":
            return "轨迹步数 : " + RenderFeatureManager.trajectoryMaxSteps;
        case "color":
            return "准星颜色 : #" + String.format(Locale.ROOT, "%06X", RenderFeatureManager.crosshairColorRgb & 0xFFFFFF);
        case "size":
            if ("radar".equals(this.featureId)) {
                return "雷达尺寸 : " + RenderFeatureManager.radarSize;
            }
            return "准星大小 : " + formatFloat(RenderFeatureManager.crosshairSize);
        case "thickness":
            return "准星粗细 : " + formatFloat(RenderFeatureManager.crosshairThickness);
        default:
            return key;
        }
    }

    private String getOptionLabel(String key) {
        switch (key) {
        case "soft_mode":
            return "柔和模式";
        case "remove_liquid":
            return "移除液体雾";
        case "brighten_color":
            return "提亮雾颜色";
        case "players":
            return "玩家";
        case "monsters":
            return "怪物";
        case "animals":
            return "动物";
        case "through_walls":
            return "穿墙显示";
        case "filled_box":
            return "填充方框";
        case "show_health":
            return "显示血量";
        case "show_distance":
            return "显示距离";
        case "show_held_item":
            return "显示手持物";
        case "storages":
            return "高亮存储箱";
        case "spawners":
            return "高亮刷怪笼";
        case "ores":
            return "高亮矿石";
        case "edit_blocks":
            return "编辑透视方块";
        case "show_name":
            return "显示名称";
        case "bows":
            return "弓箭轨迹";
        case "pearls":
            return "珍珠轨迹";
        case "throwables":
            return "雪球/鸡蛋轨迹";
        case "potions":
            return "药水/经验瓶轨迹";
        case "dynamic_gap":
            return "动态间距";
        case "remove_view_bobbing":
            return "移除走路晃动";
        case "remove_hurt_shake":
            return "移除受伤抖动";
        case "rotate_with_view":
            return "随视角旋转";
        case "show_position":
            return "显示坐标";
        default:
            return key;
        }
    }

    private boolean getBooleanOptionValue(String key) {
        switch (this.featureId) {
        case "brightness_boost":
            return "soft_mode".equals(key) && RenderFeatureManager.brightnessSoftMode;
        case "no_fog":
            if ("remove_liquid".equals(key)) {
                return RenderFeatureManager.noFogRemoveLiquid;
            }
            if ("brighten_color".equals(key)) {
                return RenderFeatureManager.noFogBrightenColor;
            }
            return false;
        case "entity_visual":
            if ("players".equals(key)) {
                return RenderFeatureManager.entityVisualPlayers;
            }
            if ("monsters".equals(key)) {
                return RenderFeatureManager.entityVisualMonsters;
            }
            if ("animals".equals(key)) {
                return RenderFeatureManager.entityVisualAnimals;
            }
            if ("through_walls".equals(key)) {
                return RenderFeatureManager.entityVisualThroughWalls;
            }
            if ("filled_box".equals(key)) {
                return RenderFeatureManager.entityVisualFilledBox;
            }
            return false;
        case "tracer_line":
            if ("players".equals(key)) {
                return RenderFeatureManager.tracerPlayers;
            }
            if ("monsters".equals(key)) {
                return RenderFeatureManager.tracerMonsters;
            }
            if ("animals".equals(key)) {
                return RenderFeatureManager.tracerAnimals;
            }
            if ("through_walls".equals(key)) {
                return RenderFeatureManager.tracerThroughWalls;
            }
            return false;
        case "entity_tags":
            if ("players".equals(key)) {
                return RenderFeatureManager.entityTagPlayers;
            }
            if ("monsters".equals(key)) {
                return RenderFeatureManager.entityTagMonsters;
            }
            if ("animals".equals(key)) {
                return RenderFeatureManager.entityTagAnimals;
            }
            if ("show_health".equals(key)) {
                return RenderFeatureManager.entityTagShowHealth;
            }
            if ("show_distance".equals(key)) {
                return RenderFeatureManager.entityTagShowDistance;
            }
            if ("show_held_item".equals(key)) {
                return RenderFeatureManager.entityTagShowHeldItem;
            }
            return false;
        case "block_highlight":
            if ("storages".equals(key)) {
                return RenderFeatureManager.blockHighlightStorages;
            }
            if ("spawners".equals(key)) {
                return RenderFeatureManager.blockHighlightSpawners;
            }
            if ("ores".equals(key)) {
                return RenderFeatureManager.blockHighlightOres;
            }
            if ("through_walls".equals(key)) {
                return RenderFeatureManager.blockHighlightThroughWalls;
            }
            if ("filled_box".equals(key)) {
                return RenderFeatureManager.blockHighlightFilledBox;
            }
            return false;
        case "item_esp":
            if ("show_name".equals(key)) {
                return RenderFeatureManager.itemEspShowName;
            }
            if ("show_distance".equals(key)) {
                return RenderFeatureManager.itemEspShowDistance;
            }
            if ("through_walls".equals(key)) {
                return RenderFeatureManager.itemEspThroughWalls;
            }
            return false;
        case "trajectory_line":
            if ("bows".equals(key)) {
                return RenderFeatureManager.trajectoryBows;
            }
            if ("pearls".equals(key)) {
                return RenderFeatureManager.trajectoryPearls;
            }
            if ("throwables".equals(key)) {
                return RenderFeatureManager.trajectoryThrowables;
            }
            if ("potions".equals(key)) {
                return RenderFeatureManager.trajectoryPotions;
            }
            return false;
        case "custom_crosshair":
            return "dynamic_gap".equals(key) && RenderFeatureManager.crosshairDynamicGap;
        case "anti_bob":
            if ("remove_view_bobbing".equals(key)) {
                return RenderFeatureManager.antiBobRemoveViewBobbing;
            }
            if ("remove_hurt_shake".equals(key)) {
                return RenderFeatureManager.antiBobRemoveHurtShake;
            }
            return false;
        case "radar":
            if ("players".equals(key)) {
                return RenderFeatureManager.radarPlayers;
            }
            if ("monsters".equals(key)) {
                return RenderFeatureManager.radarMonsters;
            }
            if ("animals".equals(key)) {
                return RenderFeatureManager.radarAnimals;
            }
            if ("rotate_with_view".equals(key)) {
                return RenderFeatureManager.radarRotateWithView;
            }
            return false;
        case "player_skeleton":
            return "through_walls".equals(key) && RenderFeatureManager.skeletonThroughWalls;
        case "block_outline":
            return "filled_box".equals(key) && RenderFeatureManager.blockOutlineFilledBox;
        case "entity_info":
            if ("show_health".equals(key)) {
                return RenderFeatureManager.entityInfoShowHealth;
            }
            if ("show_distance".equals(key)) {
                return RenderFeatureManager.entityInfoShowDistance;
            }
            if ("show_position".equals(key)) {
                return RenderFeatureManager.entityInfoShowPosition;
            }
            if ("show_held_item".equals(key)) {
                return RenderFeatureManager.entityInfoShowHeldItem;
            }
            return false;
        default:
            return false;
        }
    }

    private void toggleBooleanOption(String key) {
        switch (this.featureId) {
        case "brightness_boost":
            if ("soft_mode".equals(key)) {
                RenderFeatureManager.brightnessSoftMode = !RenderFeatureManager.brightnessSoftMode;
            }
            break;
        case "no_fog":
            if ("remove_liquid".equals(key)) {
                RenderFeatureManager.noFogRemoveLiquid = !RenderFeatureManager.noFogRemoveLiquid;
            } else if ("brighten_color".equals(key)) {
                RenderFeatureManager.noFogBrightenColor = !RenderFeatureManager.noFogBrightenColor;
            }
            break;
        case "entity_visual":
            if ("players".equals(key)) {
                RenderFeatureManager.entityVisualPlayers = !RenderFeatureManager.entityVisualPlayers;
            } else if ("monsters".equals(key)) {
                RenderFeatureManager.entityVisualMonsters = !RenderFeatureManager.entityVisualMonsters;
            } else if ("animals".equals(key)) {
                RenderFeatureManager.entityVisualAnimals = !RenderFeatureManager.entityVisualAnimals;
            } else if ("through_walls".equals(key)) {
                RenderFeatureManager.entityVisualThroughWalls = !RenderFeatureManager.entityVisualThroughWalls;
            } else if ("filled_box".equals(key)) {
                RenderFeatureManager.entityVisualFilledBox = !RenderFeatureManager.entityVisualFilledBox;
            }
            break;
        case "tracer_line":
            if ("players".equals(key)) {
                RenderFeatureManager.tracerPlayers = !RenderFeatureManager.tracerPlayers;
            } else if ("monsters".equals(key)) {
                RenderFeatureManager.tracerMonsters = !RenderFeatureManager.tracerMonsters;
            } else if ("animals".equals(key)) {
                RenderFeatureManager.tracerAnimals = !RenderFeatureManager.tracerAnimals;
            } else if ("through_walls".equals(key)) {
                RenderFeatureManager.tracerThroughWalls = !RenderFeatureManager.tracerThroughWalls;
            }
            break;
        case "entity_tags":
            if ("players".equals(key)) {
                RenderFeatureManager.entityTagPlayers = !RenderFeatureManager.entityTagPlayers;
            } else if ("monsters".equals(key)) {
                RenderFeatureManager.entityTagMonsters = !RenderFeatureManager.entityTagMonsters;
            } else if ("animals".equals(key)) {
                RenderFeatureManager.entityTagAnimals = !RenderFeatureManager.entityTagAnimals;
            } else if ("show_health".equals(key)) {
                RenderFeatureManager.entityTagShowHealth = !RenderFeatureManager.entityTagShowHealth;
            } else if ("show_distance".equals(key)) {
                RenderFeatureManager.entityTagShowDistance = !RenderFeatureManager.entityTagShowDistance;
            } else if ("show_held_item".equals(key)) {
                RenderFeatureManager.entityTagShowHeldItem = !RenderFeatureManager.entityTagShowHeldItem;
            }
            break;
        case "block_highlight":
            if ("storages".equals(key)) {
                RenderFeatureManager.blockHighlightStorages = !RenderFeatureManager.blockHighlightStorages;
            } else if ("spawners".equals(key)) {
                RenderFeatureManager.blockHighlightSpawners = !RenderFeatureManager.blockHighlightSpawners;
            } else if ("ores".equals(key)) {
                RenderFeatureManager.blockHighlightOres = !RenderFeatureManager.blockHighlightOres;
            } else if ("through_walls".equals(key)) {
                RenderFeatureManager.blockHighlightThroughWalls = !RenderFeatureManager.blockHighlightThroughWalls;
            } else if ("filled_box".equals(key)) {
                RenderFeatureManager.blockHighlightFilledBox = !RenderFeatureManager.blockHighlightFilledBox;
            }
            break;
        case "item_esp":
            if ("show_name".equals(key)) {
                RenderFeatureManager.itemEspShowName = !RenderFeatureManager.itemEspShowName;
            } else if ("show_distance".equals(key)) {
                RenderFeatureManager.itemEspShowDistance = !RenderFeatureManager.itemEspShowDistance;
            } else if ("through_walls".equals(key)) {
                RenderFeatureManager.itemEspThroughWalls = !RenderFeatureManager.itemEspThroughWalls;
            }
            break;
        case "trajectory_line":
            if ("bows".equals(key)) {
                RenderFeatureManager.trajectoryBows = !RenderFeatureManager.trajectoryBows;
            } else if ("pearls".equals(key)) {
                RenderFeatureManager.trajectoryPearls = !RenderFeatureManager.trajectoryPearls;
            } else if ("throwables".equals(key)) {
                RenderFeatureManager.trajectoryThrowables = !RenderFeatureManager.trajectoryThrowables;
            } else if ("potions".equals(key)) {
                RenderFeatureManager.trajectoryPotions = !RenderFeatureManager.trajectoryPotions;
            }
            break;
        case "custom_crosshair":
            if ("dynamic_gap".equals(key)) {
                RenderFeatureManager.crosshairDynamicGap = !RenderFeatureManager.crosshairDynamicGap;
            }
            break;
        case "anti_bob":
            if ("remove_view_bobbing".equals(key)) {
                RenderFeatureManager.antiBobRemoveViewBobbing = !RenderFeatureManager.antiBobRemoveViewBobbing;
            } else if ("remove_hurt_shake".equals(key)) {
                RenderFeatureManager.antiBobRemoveHurtShake = !RenderFeatureManager.antiBobRemoveHurtShake;
            }
            break;
        case "radar":
            if ("players".equals(key)) {
                RenderFeatureManager.radarPlayers = !RenderFeatureManager.radarPlayers;
            } else if ("monsters".equals(key)) {
                RenderFeatureManager.radarMonsters = !RenderFeatureManager.radarMonsters;
            } else if ("animals".equals(key)) {
                RenderFeatureManager.radarAnimals = !RenderFeatureManager.radarAnimals;
            } else if ("rotate_with_view".equals(key)) {
                RenderFeatureManager.radarRotateWithView = !RenderFeatureManager.radarRotateWithView;
            }
            break;
        case "player_skeleton":
            if ("through_walls".equals(key)) {
                RenderFeatureManager.skeletonThroughWalls = !RenderFeatureManager.skeletonThroughWalls;
            }
            break;
        case "block_outline":
            if ("filled_box".equals(key)) {
                RenderFeatureManager.blockOutlineFilledBox = !RenderFeatureManager.blockOutlineFilledBox;
            }
            break;
        case "entity_info":
            if ("show_health".equals(key)) {
                RenderFeatureManager.entityInfoShowHealth = !RenderFeatureManager.entityInfoShowHealth;
            } else if ("show_distance".equals(key)) {
                RenderFeatureManager.entityInfoShowDistance = !RenderFeatureManager.entityInfoShowDistance;
            } else if ("show_position".equals(key)) {
                RenderFeatureManager.entityInfoShowPosition = !RenderFeatureManager.entityInfoShowPosition;
            } else if ("show_held_item".equals(key)) {
                RenderFeatureManager.entityInfoShowHeldItem = !RenderFeatureManager.entityInfoShowHeldItem;
            }
            break;
        default:
            break;
        }
    }

    private void openFloatInput(String title, float currentValue, float minValue, float maxValue, Consumer<Float> setter) {
        this.mc.setScreen(new GuiTextInput(this, title, formatFloat(currentValue), value -> {
            float parsed = currentValue;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            setter.accept(clampFloat(parsed, minValue, maxValue));
            refreshButtonTexts();
            this.mc.setScreen(this);
        }));
    }

    private void openIntInput(String title, int currentValue, int minValue, int maxValue, Consumer<Integer> setter) {
        this.mc.setScreen(new GuiTextInput(this, title, String.valueOf(currentValue), value -> {
            int parsed = currentValue;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            setter.accept(clampInt(parsed, minValue, maxValue));
            refreshButtonTexts();
            this.mc.setScreen(this);
        }));
    }

    private void openLineWidthInput() {
        switch (this.featureId) {
        case "player_skeleton":
            openFloatInput("输入骨架线宽 (0.5 - 6.0)", RenderFeatureManager.skeletonLineWidth, 0.5F, 6.0F,
                    value -> RenderFeatureManager.skeletonLineWidth = value);
            break;
        case "block_outline":
            openFloatInput("输入边框线宽 (0.5 - 6.0)", RenderFeatureManager.blockOutlineLineWidth, 0.5F, 6.0F,
                    value -> RenderFeatureManager.blockOutlineLineWidth = value);
            break;
        default:
            openFloatInput("输入线宽 (0.5 - 6.0)", RenderFeatureManager.tracerLineWidth, 0.5F, 6.0F,
                    value -> RenderFeatureManager.tracerLineWidth = value);
            break;
        }
    }

    private void openColorInput() {
        String initial = "#" + String.format(Locale.ROOT, "%06X", RenderFeatureManager.crosshairColorRgb & 0xFFFFFF);
        this.mc.setScreen(new GuiTextInput(this, "输入十字准星颜色 (#RRGGBB)", initial, value -> {
            int parsed = RenderFeatureManager.crosshairColorRgb & 0xFFFFFF;
            try {
                String normalized = value == null ? "" : value.trim();
                if (normalized.startsWith("#")) {
                    normalized = normalized.substring(1);
                }
                if (normalized.length() == 6) {
                    parsed = Integer.parseInt(normalized, 16) & 0xFFFFFF;
                }
            } catch (Exception ignored) {
            }
            RenderFeatureManager.crosshairColorRgb = parsed;
            refreshButtonTexts();
            this.mc.setScreen(this);
        }));
    }

    private void openDistanceInput() {
        switch (this.featureId) {
        case "entity_visual":
            openFloatInput("输入实体视觉范围 (4.0 - 128.0)", RenderFeatureManager.entityVisualMaxDistance, 4.0F, 128.0F,
                    value -> RenderFeatureManager.entityVisualMaxDistance = value);
            break;
        case "tracer_line":
            openFloatInput("输入 Tracer 范围 (4.0 - 160.0)", RenderFeatureManager.tracerMaxDistance, 4.0F, 160.0F,
                    value -> RenderFeatureManager.tracerMaxDistance = value);
            break;
        case "entity_tags":
            openFloatInput("输入标签范围 (4.0 - 96.0)", RenderFeatureManager.entityTagMaxDistance, 4.0F, 96.0F,
                    value -> RenderFeatureManager.entityTagMaxDistance = value);
            break;
        case "block_highlight":
            openFloatInput("输入方块高亮范围 (4.0 - 64.0)", RenderFeatureManager.blockHighlightMaxDistance, 4.0F, 64.0F,
                    value -> RenderFeatureManager.blockHighlightMaxDistance = value);
            break;
        case "item_esp":
            openFloatInput("输入物品ESP范围 (4.0 - 64.0)", RenderFeatureManager.itemEspMaxDistance, 4.0F, 64.0F,
                    value -> RenderFeatureManager.itemEspMaxDistance = value);
            break;
        case "radar":
            openFloatInput("输入雷达范围 (8.0 - 128.0)", RenderFeatureManager.radarMaxDistance, 8.0F, 128.0F,
                    value -> RenderFeatureManager.radarMaxDistance = value);
            break;
        case "player_skeleton":
            openFloatInput("输入骨架范围 (4.0 - 128.0)", RenderFeatureManager.skeletonMaxDistance, 4.0F, 128.0F,
                    value -> RenderFeatureManager.skeletonMaxDistance = value);
            break;
        case "entity_info":
            openFloatInput("输入实体信息范围 (4.0 - 96.0)", RenderFeatureManager.entityInfoMaxDistance, 4.0F, 96.0F,
                    value -> RenderFeatureManager.entityInfoMaxDistance = value);
            break;
        default:
            break;
        }
    }

    private void openSizeInput() {
        if ("radar".equals(this.featureId)) {
            openIntInput("输入雷达尺寸 (60 - 180)", RenderFeatureManager.radarSize, 60, 180,
                    value -> RenderFeatureManager.radarSize = value);
            return;
        }
        openFloatInput("输入准星大小 (2.0 - 16.0)", RenderFeatureManager.crosshairSize, 2.0F, 16.0F,
                value -> RenderFeatureManager.crosshairSize = value);
    }

    private float getDistanceValue() {
        switch (this.featureId) {
        case "entity_visual":
            return RenderFeatureManager.entityVisualMaxDistance;
        case "tracer_line":
            return RenderFeatureManager.tracerMaxDistance;
        case "entity_tags":
            return RenderFeatureManager.entityTagMaxDistance;
        case "block_highlight":
            return RenderFeatureManager.blockHighlightMaxDistance;
        case "item_esp":
            return RenderFeatureManager.itemEspMaxDistance;
        case "radar":
            return RenderFeatureManager.radarMaxDistance;
        case "player_skeleton":
            return RenderFeatureManager.skeletonMaxDistance;
        case "entity_info":
            return RenderFeatureManager.entityInfoMaxDistance;
        default:
            return 0.0F;
        }
    }

    private float getLineWidthValue() {
        switch (this.featureId) {
        case "tracer_line":
            return RenderFeatureManager.tracerLineWidth;
        case "player_skeleton":
            return RenderFeatureManager.skeletonLineWidth;
        case "block_outline":
            return RenderFeatureManager.blockOutlineLineWidth;
        default:
            return 0.0F;
        }
    }

    private String getOptionRangeText(String key) {
        switch (key) {
        case "gamma":
            return "§8范围 1.0 - 16.0";
        case "max_distance":
            if ("radar".equals(this.featureId)) {
                return "§8范围 8.0 - 128.0";
            }
            if ("entity_info".equals(this.featureId)) {
                return "§8范围 4.0 - 96.0";
            }
            return "§8范围随功能而变，点击后输入距离";
        case "line_width":
            return "§8范围 0.5 - 6.0";
        case "max_steps":
            return "§8范围 20 - 320";
        case "edit_blocks":
            return "§8点击后打开透视方块编辑界面";
        case "color":
            return "§8格式 #RRGGBB";
        case "size":
            return "radar".equals(this.featureId) ? "§8范围 60 - 180" : "§8范围 2.0 - 16.0";
        case "thickness":
            return "§8范围 1.0 - 6.0";
        default:
            return "";
        }
    }

    private String getDraftSummary() {
        return "§7草稿总开关: " + (this.draftEnabled ? "§a开启" : "§c关闭")
                + " §7| 选项数: §f" + this.optionSlots.size();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            RenderFeatureManager.loadConfig();
            this.mc.setScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (isInside(mouseX, mouseY, this.panelX + 8, this.contentTop + 20, this.panelWidth - 16,
                    Math.max(20, this.contentBottom - this.contentTop - 12))) {
                this.contentScrollOffset = dWheel > 0
                        ? Math.max(0, this.contentScrollOffset - 14)
                        : Math.min(this.maxContentScroll, this.contentScrollOffset + 14);
                relayoutControls();
            }
            return;
        }

        int button = Mouse.getEventButton();
        if (button == -1) {
            return;
        }

        if (Mouse.getEventButtonState()) {
            if (handleMousePressed(mouseX, mouseY, button)) {
                return;
            }
        } else {
            mouseReleased(mouseX, mouseY, button);
        }
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && isMouseOverScrollbarTrack(mouseX, mouseY)) {
            beginScrollbarDrag(mouseY);
            return true;
        }
        return mouseButton == 0 && handleButtonActivation(mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.draggingScrollbar) {
            if (Mouse.isButtonDown(0)) {
                updateScrollFromScrollbar(mouseY);
            } else {
                this.draggingScrollbar = false;
            }
        }

        FeatureState feature = getFeature();
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth,
                feature == null ? "渲染功能设置" : feature.name + " 设置", this.fontRenderer);
        drawIntroLines();
        drawStatusStrip(feature);
        drawScrollableContent(mouseX, mouseY, partialTicks, feature);
        drawContentScrollbar();
        drawFixedButtons(mouseX, mouseY, partialTicks);
        drawHoverTooltips(mouseX, mouseY, feature);
    }

    private void drawIntroLines() {
        int textY = this.panelY + 22;
        int lineStep = this.fontRenderer.FONT_HEIGHT + 2;
        for (String line : this.introLines) {
            drawString(this.fontRenderer, line, this.panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += lineStep;
        }
    }

    private void drawStatusStrip(FeatureState feature) {
        int stripX = this.panelX + 14;
        int stripY = this.contentTop;
        int stripW = this.panelWidth - 28;
        int stripH = 18;
        drawRect(stripX, stripY, stripX + stripW, stripY + stripH, 0x33202A36);
        drawHorizontalLine(stripX, stripX + stripW, stripY, 0x664FA6D9);
        drawHorizontalLine(stripX, stripX + stripW, stripY + stripH, 0x4435536C);
        String summary = "状态: " + (this.draftEnabled ? "§a开启" : "§c关闭")
                + (feature == null ? " §7| 功能: §c未找到" : " §7| 功能: §f" + feature.name)
                + " §7| 运行: §f" + RenderFeatureManager.getFeatureRuntimeSummary(this.featureId);
        drawString(this.fontRenderer, this.fontRenderer.trimStringToWidth(summary, stripW - 12), stripX + 6, stripY + 5,
                0xFFFFFFFF);
    }

    private void drawScrollableContent(int mouseX, int mouseY, float partialTicks, FeatureState feature) {
        int viewportX = getScrollableViewportLeft();
        int viewportY = getScrollableViewportTop();
        int viewportW = getScrollableViewportWidth();
        int viewportH = getScrollableViewportHeight();
        if (viewportW <= 0 || viewportH <= 0) {
            return;
        }

        int rowX = this.panelX + 12;
        int rowRight = getScrollableViewportRight() - 6;
        boolean mouseInViewport = isPointInsideScrollableViewport(mouseX, mouseY);
        int clippedMouseX = mouseInViewport ? mouseX : Integer.MIN_VALUE;
        int clippedMouseY = mouseInViewport ? mouseY : Integer.MIN_VALUE;

        startScissor(viewportX, viewportY, viewportW, viewportH);
        try {
            drawSectionBox("基础状态", rowX, this.stateHintY, rowRight, this.stateSectionBottom);
            drawSectionBox("功能选项", rowX, this.optionsHintY, rowRight, this.optionsSectionBottom);
            drawSectionBox("运行说明", rowX, this.infoSectionY + 18, rowRight, this.infoSectionBottom);

            if (isVisibleInScrollableViewport(this.stateHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7这里的开关会先作用于当前草稿，保存后才写入配置。", rowX, this.stateHintY,
                        0xFFB6C5D6);
            }
            if (this.enabledButton.visible) {
                this.enabledButton.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
            }

            if (isVisibleInScrollableViewport(this.optionsHintY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, "§7布尔项可直接切换，数值项点击后会弹出输入框。", rowX, this.optionsHintY, 0xFFB6C5D6);
            }
            for (OptionSlot slot : this.optionSlots) {
                if (slot.button.visible) {
                    slot.button.drawButton(this.mc, clippedMouseX, clippedMouseY, partialTicks);
                }
                if (!slot.noteText.isEmpty() && isVisibleInScrollableViewport(slot.noteY, this.fontRenderer.FONT_HEIGHT)) {
                    drawString(this.fontRenderer, slot.noteText, rowX, slot.noteY, 0xFF9FB2C8);
                }
            }

            int currentY = this.infoSectionY + 18;
            for (String line : this.descriptionLines) {
                if (isVisibleInScrollableViewport(currentY, this.fontRenderer.FONT_HEIGHT + 2)) {
                    drawString(this.fontRenderer, line, rowX, currentY, 0xFFE2F2FF);
                }
                currentY += this.fontRenderer.FONT_HEIGHT + 2;
            }

            if (isVisibleInScrollableViewport(this.infoRuntimeY, this.fontRenderer.FONT_HEIGHT + 2)) {
                String runtime = feature == null
                        ? "§6运行状态: 未找到对应功能"
                        : "§e运行状态: §f" + RenderFeatureManager.getFeatureRuntimeSummary(this.featureId);
                drawString(this.fontRenderer, runtime, rowX, this.infoRuntimeY, 0xFFFFFFFF);
            }

            if (isVisibleInScrollableViewport(this.infoDraftY, this.fontRenderer.FONT_HEIGHT + 2)) {
                drawString(this.fontRenderer, getDraftSummary(), rowX, this.infoDraftY, 0xFFFFFFFF);
            }
        } finally {
            endScissor();
        }
    }

    private void drawFixedButtons(int mouseX, int mouseY, float partialTicks) {
        for (GuiButton button : this.buttonList) {
            if (isFooterButton(button) && button.visible) {
                button.drawButton(this.mc, mouseX, mouseY, partialTicks);
            }
        }
    }

    private void drawSectionBox(String title, int minX, int minY, int maxX, int maxY) {
        int viewportLeft = getScrollableViewportLeft();
        int viewportTop = getScrollableViewportTop();
        int viewportRight = getScrollableViewportRight();
        int viewportBottom = getScrollableViewportBottom();

        int boxX = minX - 6;
        int boxY = minY - 18;
        int boxW = (maxX - minX) + 12;
        int boxH = (maxY - minY) + 24;
        int boxRight = boxX + boxW;
        int boxBottom = boxY + boxH;

        if (boxRight <= viewportLeft || boxX >= viewportRight || boxBottom <= viewportTop || boxY >= viewportBottom) {
            return;
        }

        int drawLeft = Math.max(boxX, viewportLeft);
        int drawTop = Math.max(boxY, viewportTop);
        int drawRight = Math.min(boxRight, viewportRight);
        int drawBottom = Math.min(boxBottom, viewportBottom);

        drawRect(drawLeft, drawTop, drawRight, drawBottom, 0x44202A36);
        if (boxY >= viewportTop && boxY < viewportBottom) {
            drawHorizontalLine(drawLeft, drawRight, boxY, 0xFF4FA6D9);
        }
        if (boxBottom > viewportTop && boxBottom <= viewportBottom) {
            drawHorizontalLine(drawLeft, drawRight, boxBottom, 0xFF35536C);
        }
        if (boxX >= viewportLeft && boxX < viewportRight) {
            drawVerticalLine(boxX, drawTop, drawBottom, 0xFF35536C);
        }
        if (boxRight > viewportLeft && boxRight <= viewportRight) {
            drawVerticalLine(boxRight, drawTop, drawBottom, 0xFF35536C);
        }

        int titleY = boxY + 5;
        if (titleY + this.fontRenderer.FONT_HEIGHT > viewportTop && titleY < viewportBottom) {
            drawString(this.fontRenderer, "§b" + title, boxX + 6, titleY, 0xFFE8F6FF);
        }
    }

    private void drawHoverTooltips(int mouseX, int mouseY, FeatureState feature) {
        if (feature != null && isMouseOver(mouseX, mouseY, this.enabledButton)) {
            List<String> lines = new ArrayList<>();
            lines.add("§e" + feature.name);
            lines.add("§7左键其他功能页可直接切换开关。");
            lines.add("§7这里调整后需要点“保存并关闭”才会写入配置。");
            drawHoveringText(lines, mouseX, mouseY);
            return;
        }

        for (OptionSlot slot : this.optionSlots) {
            if (isMouseOver(mouseX, mouseY, slot.button)) {
                List<String> lines = new ArrayList<>();
                lines.add("§e" + getOptionLabel(slot.key));
                if (isBooleanOption(slot.key)) {
                    lines.add("§7当前: " + (getBooleanOptionValue(slot.key) ? "§a开启" : "§c关闭"));
                } else {
                    lines.add("§7当前: §f" + buildOptionButtonLabel(slot.key).replaceFirst("^.* : ", ""));
                }
                if (!slot.noteText.isEmpty()) {
                    lines.add(slot.noteText);
                }
                lines.add("edit_blocks".equals(slot.key)
                        ? "§7点击后打开透视方块编辑器。"
                        : "§7点击后可切换或输入新的值。");
                drawHoveringText(lines, mouseX, mouseY);
                return;
            }
        }
    }

    private void drawContentScrollbar() {
        if (this.maxContentScroll <= 0) {
            return;
        }
        int sbX = getScrollbarTrackX();
        int sbY = getScrollbarTrackY();
        int sbH = getScrollbarTrackHeight();
        int thumbY = getScrollbarThumbY();
        int thumbH = getScrollbarThumbHeight();
        drawRect(sbX, sbY, sbX + SCROLLBAR_TRACK_WIDTH, sbY + sbH, 0x55141C24);
        drawRect(sbX, thumbY, sbX + SCROLLBAR_TRACK_WIDTH, thumbY + thumbH, 0xCC7CCBFF);
    }

    private boolean handleButtonActivation(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : this.buttonList) {
            if (button == null || !button.visible || !button.enabled) {
                continue;
            }
            if (!isPointInsideButtonArea(button, mouseX, mouseY)) {
                continue;
            }
            button.playPressSound(this.mc.getSoundManager());
            actionPerformed(button);
            return true;
        }
        return false;
    }

    private boolean isFooterButton(GuiButton button) {
        return button == this.saveButton || button == this.defaultButton || button == this.cancelButton;
    }

    private boolean isScrollableButton(GuiButton button) {
        return button != null && !isFooterButton(button);
    }

    private boolean isMouseOver(int mouseX, int mouseY, GuiButton button) {
        return button != null && button.visible && isPointInsideButtonArea(button, mouseX, mouseY);
    }

    private boolean isPointInsideButtonArea(GuiButton button, int mouseX, int mouseY) {
        if (button == null || !isInside(mouseX, mouseY, button.x, button.y, button.width, button.height)) {
            return false;
        }
        return !isScrollableButton(button) || isPointInsideScrollableViewport(mouseX, mouseY);
    }

    private int getScrollableViewportLeft() {
        return this.panelX + 6;
    }

    private int getScrollableViewportTop() {
        return this.contentTop + 28;
    }

    private int getScrollableViewportWidth() {
        return Math.max(1, this.panelWidth - 12 - SCROLLBAR_GUTTER);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            this.draggingScrollbar = false;
        }
    }

    private int getScrollbarTrackX() {
        return this.panelX + this.panelWidth - 10;
    }

    private int getScrollbarTrackY() {
        return getScrollableViewportTop();
    }

    private int getScrollbarTrackHeight() {
        return Math.max(20, getScrollableViewportHeight());
    }

    private int getScrollbarThumbHeight() {
        int trackHeight = getScrollbarTrackHeight();
        return Math.max(16, (int) ((float) trackHeight * trackHeight
                / Math.max(trackHeight, trackHeight + this.maxContentScroll)));
    }

    private int getScrollbarThumbY() {
        int trackHeight = getScrollbarTrackHeight();
        int thumbHeight = getScrollbarThumbHeight();
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        return getScrollbarTrackY()
                + (int) ((this.contentScrollOffset / (float) Math.max(1, this.maxContentScroll)) * thumbTravel);
    }

    private boolean isMouseOverScrollbarTrack(int mouseX, int mouseY) {
        return this.maxContentScroll > 0
                && isInside(mouseX, mouseY, getScrollbarTrackX(), getScrollbarTrackY(), SCROLLBAR_TRACK_WIDTH,
                        getScrollbarTrackHeight());
    }

    private void beginScrollbarDrag(int mouseY) {
        this.draggingScrollbar = true;
        int thumbY = getScrollbarThumbY();
        int thumbHeight = getScrollbarThumbHeight();
        this.scrollbarDragOffsetY = mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                ? mouseY - thumbY
                : thumbHeight / 2;
        updateScrollFromScrollbar(mouseY);
    }

    private void updateScrollFromScrollbar(int mouseY) {
        if (this.maxContentScroll <= 0) {
            return;
        }
        int trackY = getScrollbarTrackY();
        int trackHeight = getScrollbarTrackHeight();
        int thumbHeight = getScrollbarThumbHeight();
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = clampInt(mouseY - this.scrollbarDragOffsetY, trackY, trackY + trackHeight - thumbHeight);
        float ratio = (thumbY - trackY) / (float) thumbTravel;
        this.contentScrollOffset = clampInt(Math.round(ratio * this.maxContentScroll), 0, this.maxContentScroll);
        relayoutControls();
    }

    private int getScrollableViewportHeight() {
        return Math.max(1, this.contentBottom - getScrollableViewportTop());
    }

    private int getScrollableViewportRight() {
        return getScrollableViewportLeft() + getScrollableViewportWidth();
    }

    private int getScrollableViewportBottom() {
        return getScrollableViewportTop() + getScrollableViewportHeight();
    }

    private boolean isPointInsideScrollableViewport(int mouseX, int mouseY) {
        return isInside(mouseX, mouseY, getScrollableViewportLeft(), getScrollableViewportTop(),
                getScrollableViewportWidth(), getScrollableViewportHeight());
    }

    private boolean isVisibleInScrollableViewport(int y, int height) {
        return y + height >= getScrollableViewportTop() && y <= getScrollableViewportBottom();
    }

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int scaleFactor = scaledResolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scaleFactor, (this.height - (y + height)) * scaleFactor, width * scaleFactor,
                height * scaleFactor);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static float clampFloat(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    private static int clampInt(int value, int minValue, int maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}







