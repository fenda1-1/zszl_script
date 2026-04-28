package com.zszl.zszlScriptMod.otherfeatures.gui.item;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.otherfeatures.gui.common.SimpleFeatureConfigScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager.FeatureState;
import net.minecraft.client.gui.GuiScreen;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SingleItemFeatureConfigScreen extends SimpleFeatureConfigScreen {

    public SingleItemFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        super(parentScreen, featureId, title);
    }

    @Override
    protected FeatureView getFeatureView() {
        FeatureState state = ItemFeatureManager.getFeature(this.featureId);
        if (state == null) {
            return new FeatureView("", "当前功能不存在。", false, true, false, "未找到");
        }
        return new FeatureView(state.name, state.description, state.isEnabled(), state.isStatusHudEnabled(),
                state.behaviorImplemented, ItemFeatureManager.getFeatureRuntimeSummary(this.featureId));
    }

    @Override
    protected void saveDraftState(boolean enabled, boolean statusHudEnabled) {
        ItemFeatureManager.setEnabled(this.featureId, enabled);
        ItemFeatureManager.setFeatureStatusHudEnabled(this.featureId, statusHudEnabled);
    }

    @Override
    protected void resetFeatureState() {
        ItemFeatureManager.resetFeature(this.featureId);
    }

    @Override
    protected String getDefaultTitle() {
        return "物品功能设置";
    }

    @Override
    protected boolean hasPrimaryOptionButton() {
        return true;
    }

    @Override
    protected boolean hasSecondaryOptionButton() {
        return "drop_all".equals(this.featureId);
    }

    @Override
    protected boolean isPrimaryOptionButtonEnabled() {
        return "chest_steal".equals(this.featureId)
                || "auto_equip".equals(this.featureId)
                || "drop_all".equals(this.featureId);
    }

    @Override
    protected boolean isSecondaryOptionButtonEnabled() {
        return "drop_all".equals(this.featureId);
    }

    @Override
    protected String getPrimaryOptionButtonLabel() {
        switch (this.featureId) {
        case "chest_steal":
            return "箱子窃取间隔 : " + ItemFeatureManager.getChestStealDelayTicks() + " tick";
        case "auto_equip":
            return "扫描间隔 : " + ItemFeatureManager.getAutoEquipIntervalTicks() + " tick";
        case "always_critical":
            return "触发方式 : 攻击前补发暴击包";
        case "drop_all":
            String summary = ItemFeatureManager.getDropAllExpressionSummary();
            return "丢弃表达式 : " + (summary.isEmpty() ? "未配置" : summary);
        case "inventory_sort":
            return "整理范围 : 主背包 9-35 格";
        case "shulker_preview":
            return "预览模式 : 悬停显示";
        default:
            return "无额外设置";
        }
    }

    @Override
    protected String getSecondaryOptionButtonLabel() {
        if ("drop_all".equals(this.featureId)) {
            return "扔出物品间隔 : " + ItemFeatureManager.getDropAllDelayTicks() + " tick";
        }
        return "";
    }

    @Override
    protected String getOptionSectionHintText() {
        return "§7这里可以单独配置当前物品功能的附加参数。";
    }

    @Override
    protected List<String> getPrimaryOptionTooltipLines() {
        switch (this.featureId) {
        case "chest_steal":
            return Arrays.asList("§e箱子窃取间隔",
                    "§7控制打开箱子后每次搬运物品的间隔 tick。",
                    "§7数值越小，窃取越快。");
        case "auto_equip":
            return Arrays.asList("§e自动装备扫描间隔",
                    "§7控制自动检查并替换装备的频率。",
                    "§7数值越小，响应越快。");
        case "always_critical":
            return Arrays.asList("§e刀刀暴击",
                    "§7攻击实体前发送短暂伪下落位置包。",
                    "§7当前功能没有额外可调参数。");
        case "drop_all":
            return Arrays.asList("§e丢弃表达式",
                    "§7可配置多条背包物品过滤表达式。",
                    "§7命中任意表达式的物品会被自动丢弃。");
        case "inventory_sort":
            return Arrays.asList("§e整理范围",
                    "§7当前固定整理主背包 9-35 格，不整理热栏。",
                    "§7这个功能目前没有额外可调参数。");
        case "shulker_preview":
            return Arrays.asList("§e预览模式",
                    "§7当前为悬停显示潜影盒内容预览。",
                    "§7这个功能目前没有额外可调参数。");
        default:
            return Collections.emptyList();
        }
    }

    @Override
    protected List<String> getSecondaryOptionTooltipLines() {
        if ("drop_all".equals(this.featureId)) {
            return Arrays.asList("§e扔出物品间隔",
                    "§7控制自动扔出两次物品之间的间隔 tick。");
        }
        return Collections.emptyList();
    }

    @Override
    protected void onPrimaryOptionButtonPressed() {
        switch (this.featureId) {
        case "chest_steal":
            openIntegerInput("输入箱子窃取间隔 (0 - 20 tick)", ItemFeatureManager.getChestStealDelayTicks(), 0, 20,
                    ItemFeatureManager::setChestStealDelayTicks);
            return;
        case "auto_equip":
            openIntegerInput("输入自动装备扫描间隔 (1 - 40 tick)", ItemFeatureManager.getAutoEquipIntervalTicks(), 1, 40,
                    ItemFeatureManager::setAutoEquipIntervalTicks);
            return;
        case "drop_all":
            this.mc.displayGuiScreen(new GuiDropAllExpressionConfig(this));
            return;
        default:
            return;
        }
    }

    @Override
    protected void onSecondaryOptionButtonPressed() {
        if (!"drop_all".equals(this.featureId)) {
            return;
        }
        openIntegerInput("输入扔出物品间隔 (0 - 20 tick)", ItemFeatureManager.getDropAllDelayTicks(), 0, 20,
                ItemFeatureManager::setDropAllDelayTicks);
    }

    private void openIntegerInput(String titleText, int currentValue, int min, int max,
            java.util.function.IntConsumer setter) {
        this.mc.displayGuiScreen(new GuiTextInput(this, titleText, String.valueOf(currentValue), value -> {
            int parsed = currentValue;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            setter.accept(Math.max(min, Math.min(max, parsed)));
            this.mc.displayGuiScreen(this);
        }));
    }
}
