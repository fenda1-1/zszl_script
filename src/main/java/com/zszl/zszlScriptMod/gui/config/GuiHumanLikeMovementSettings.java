package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.baritone.compat.HumanLikeMovementController;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiHumanLikeMovementSettings extends ThemedGuiScreen {

    private static final int BTN_ENABLED = 1;
    private static final int BTN_STUCK_RECOVERY = 11;
    private static final int BTN_SAVE = 100;
    private static final int BTN_RELOAD = 101;
    private static final int BTN_RESET_DEFAULTS = 102;
    private static final int BTN_CANCEL = 103;

    private final GuiScreen parentScreen;
    private final List<FieldSlot> fieldSlots = new ArrayList<FieldSlot>();

    private ToggleGuiButton enabledButton;
    private ToggleGuiButton stuckRecoveryButton;

    private GuiTextField minTurnSpeedField;
    private GuiTextField maxTurnSpeedField;
    private GuiTextField viewJitterField;
    private GuiTextField turnOvershootField;
    private GuiTextField accelerationField;
    private GuiTextField decelerationField;
    private GuiTextField turnSlowdownField;
    private GuiTextField narrowSlowdownField;
    private GuiTextField strafeJitterChanceField;
    private GuiTextField strafeJitterStrengthField;
    private GuiTextField microPauseChanceField;
    private GuiTextField rhythmVariationField;
    private GuiTextField corridorBiasStrengthField;
    private GuiTextField routeNoiseStrengthField;
    private GuiTextField routeNoiseScaleField;
    private GuiTextField routeAnchorRadiusField;
    private GuiTextField startTurnThresholdField;
    private GuiTextField lightHopChanceField;
    private GuiTextField lightHopCooldownField;
    private GuiTextField finalApproachDistanceField;
    private GuiTextField finalApproachSlowdownField;
    private GuiTextField stuckRecoveryTicksField;
    private GuiTextField stuckRecoveryStrafeStrengthField;
    private GuiTextField stuckRecoveryMinTicksField;
    private GuiTextField stuckRecoveryMaxTicksField;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentViewportX;
    private int contentViewportY;
    private int contentViewportW;
    private int contentViewportH;
    private int contentScroll;
    private int maxContentScroll;
    private boolean singleColumnLayout;
    private List<String> hoverTooltipLines = new ArrayList<String>();
    private int hoverTooltipX;
    private int hoverTooltipY;

    public GuiHumanLikeMovementSettings(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.fieldSlots.clear();

        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        config.normalize();

        computeResponsiveLayout();

        enabledButton = new ToggleGuiButton(BTN_ENABLED, panelX + 10, panelY + 26, panelWidth - 20, 28,
                buildEnabledLabel(config.enabled), config.enabled);
        this.buttonList.add(enabledButton);

        stuckRecoveryButton = new ToggleGuiButton(BTN_STUCK_RECOVERY, panelX + 10, panelY + 58, panelWidth - 20, 20,
                buildStuckRecoveryLabel(config.enableStuckRecovery), config.enableStuckRecovery);
        this.buttonList.add(stuckRecoveryButton);

        minTurnSpeedField = createField(2, String.valueOf(config.minTurnSpeed));
        maxTurnSpeedField = createField(3, String.valueOf(config.maxTurnSpeed));
        viewJitterField = createField(4, String.valueOf(config.viewJitter));
        turnSlowdownField = createField(5, String.valueOf(config.turnSlowdown));
        accelerationField = createField(6, String.valueOf(config.acceleration));
        decelerationField = createField(7, String.valueOf(config.deceleration));
        narrowSlowdownField = createField(8, String.valueOf(config.narrowSlowdown));
        rhythmVariationField = createField(9, String.valueOf(config.rhythmVariation));
        corridorBiasStrengthField = createField(10, String.valueOf(config.corridorBiasStrength));
        strafeJitterChanceField = createField(12, String.valueOf(config.strafeJitterChance));
        microPauseChanceField = createField(13, String.valueOf(config.microPauseChance));
        startTurnThresholdField = createField(14, String.valueOf(config.startTurnThreshold));
        lightHopChanceField = createField(15, String.valueOf(config.lightHopChance));
        lightHopCooldownField = createField(16, String.valueOf(config.lightHopCooldownTicks));
        finalApproachDistanceField = createField(17, String.valueOf(config.finalApproachDistance));
        finalApproachSlowdownField = createField(18, String.valueOf(config.finalApproachSlowdown));
        stuckRecoveryTicksField = createField(19, String.valueOf(config.stuckRecoveryTicks));
        stuckRecoveryStrafeStrengthField = createField(20, String.valueOf(config.stuckRecoveryStrafeStrength));
        routeNoiseStrengthField = createField(21, String.valueOf(config.routeNoiseStrength));
        routeNoiseScaleField = createField(22, String.valueOf(config.routeNoiseScale));
        routeAnchorRadiusField = createField(23, String.valueOf(config.routeAnchorRadius));
        turnOvershootField = createField(24, String.valueOf(config.turnOvershoot));
        strafeJitterStrengthField = createField(25, String.valueOf(config.strafeJitterStrength));
        stuckRecoveryMinTicksField = createField(26, String.valueOf(config.stuckRecoveryMinTicks));
        stuckRecoveryMaxTicksField = createField(27, String.valueOf(config.stuckRecoveryMaxTicks));

        buildFieldSlots();
        updateFieldLayout();

        int buttonY = panelY + panelHeight - 28;
        int buttonGap = 6;
        int buttonW = (panelWidth - 20 - buttonGap * 3) / 4;
        this.buttonList.add(new ThemedButton(BTN_SAVE, panelX + 10, buttonY, buttonW, 20, "§a保存"));
        this.buttonList.add(new ThemedButton(BTN_RELOAD, panelX + 10 + buttonW + buttonGap, buttonY, buttonW, 20,
                "§b重载配置"));
        this.buttonList
                .add(new ThemedButton(BTN_RESET_DEFAULTS, panelX + 10 + (buttonW + buttonGap) * 2, buttonY, buttonW, 20,
                        "§e恢复默认"));
        this.buttonList.add(new ThemedButton(BTN_CANCEL, panelX + 10 + (buttonW + buttonGap) * 3, buttonY, buttonW, 20,
                "取消"));
    }

    private void computeResponsiveLayout() {
        int availableWidth = Math.max(220, this.width - 16);
        int availableHeight = Math.max(240, this.height - 16);

        this.panelWidth = Math.min(430, availableWidth);
        this.panelWidth = Math.min(this.panelWidth, this.width - 16);

        this.panelHeight = Math.min(560, availableHeight);
        this.panelHeight = Math.min(this.panelHeight, this.height - 16);

        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.singleColumnLayout = this.panelWidth < 390;
        this.contentViewportX = this.panelX + 14;
        this.contentViewportY = this.panelY + 86;
        this.contentViewportW = this.panelWidth - 28;
        this.contentViewportH = Math.max(70, this.panelHeight - 126);
    }

    private GuiTextField createField(int id, String value) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, -2000, -2000, 80, 18);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setText(value == null ? "" : value);
        return field;
    }

    private void buildFieldSlots() {
        fieldSlots.clear();
        addFieldSlot("视角最小转速", minTurnSpeedField, "0.1 ~ 30.0", "越大转头越快");
        addFieldSlot("视角最大转速", maxTurnSpeedField, ">= 最小转速，且 <= 60.0", "大角度转向时允许的最大转速");
        addFieldSlot("视角微抖动", viewJitterField, "0.0 ~ 5.0", "建议较小，过大会持续抖动");
        addFieldSlot("转头过冲强度", turnOvershootField, "0.0 ~ 2.0", "轻微超过目标再修回的幅度");
        addFieldSlot("转弯减速强度", turnSlowdownField, "0.0 ~ 1.0", "越大表示转弯时越会降速");
        addFieldSlot("前进加速系数", accelerationField, "0.01 ~ 1.0", "越大越快贴近目标前进速度");
        addFieldSlot("松开减速系数", decelerationField, "0.01 ~ 1.0", "越大越快减速停下");
        addFieldSlot("狭窄减速强度", narrowSlowdownField, "0.0 ~ 1.0", "狭窄通道中的额外减速程度");
        addFieldSlot("节奏变化强度", rhythmVariationField, "0.0 ~ 0.6", "长直线路段的速度波动幅度");
        addFieldSlot("路径偏置强度", corridorBiasStrengthField, "0.0 ~ 0.45", "贴左/贴右式路径偏置强度");
        addFieldSlot("横移扰动概率", strafeJitterChanceField, "0.0 ~ 1.0", "移动时横移扰动触发概率");
        addFieldSlot("横移扰动强度", strafeJitterStrengthField, "0.0 ~ 1.0", "横移扰动本身的幅度");
        addFieldSlot("路线随机强度", routeNoiseStrengthField, "0.0 ~ 0.35", "A* 路线随机偏好的整体强度");
        addFieldSlot("随机尺度", routeNoiseScaleField, "2 ~ 16", "随机偏好区域的空间尺度");
        addFieldSlot("锚点随机半径", routeAnchorRadiusField, "0.0 ~ 16.0", "中途虚拟锚点可偏移的半径");
        addFieldSlot("微停顿概率", microPauseChanceField, "0.0 ~ 0.5", "过大容易卡顿感明显");
        addFieldSlot("起步转头阈值", startTurnThresholdField, "0.0 ~ 90.0", "超过此角度时优先转头");
        addFieldSlot("轻跳概率", lightHopChanceField, "0.0 ~ 0.85", "建议保持很低");
        addFieldSlot("轻跳冷却 Tick", lightHopCooldownField, "5 ~ 200", "两次轻跳之间的最短间隔");
        addFieldSlot("末段收敛距离", finalApproachDistanceField, "0.0 ~ 8.0", "接近终点时开始收敛的距离");
        addFieldSlot("末段减速强度", finalApproachSlowdownField, "0.0 ~ 0.95", "越接近终点越慢的强度");
        addFieldSlot("卡住判定 Tick", stuckRecoveryTicksField, "5 ~ 120", "连续多少 tick 基本未移动视为卡住");
        addFieldSlot("恢复横移强度", stuckRecoveryStrafeStrengthField, "0.1 ~ 1.0", "卡住恢复时横移幅度");
        addFieldSlot("恢复最短 Tick", stuckRecoveryMinTicksField, "1 ~ 40", "卡住恢复动作最短持续时间");
        addFieldSlot("恢复最长 Tick", stuckRecoveryMaxTicksField, ">= 恢复最短 Tick，且 <= 60", "卡住恢复动作最长持续时间");
    }

    private void addFieldSlot(String label, GuiTextField field, String range, String description) {
        fieldSlots.add(new FieldSlot(label, field, range, description));
    }

    private void updateFieldLayout() {
        int columns = this.singleColumnLayout ? 1 : 2;
        int gapX = 8;
        int rowHeight = 36;
        int cellWidth = columns == 1 ? this.contentViewportW : (this.contentViewportW - gapX) / 2;
        int rows = (this.fieldSlots.size() + columns - 1) / columns;

        int totalContentHeight = rows * rowHeight + 8;
        this.maxContentScroll = Math.max(0, totalContentHeight - this.contentViewportH);
        this.contentScroll = MathHelper.clamp(this.contentScroll, 0, this.maxContentScroll);

        int startY = this.contentViewportY + 6 - this.contentScroll;
        int viewportBottom = this.contentViewportY + this.contentViewportH;

        for (int i = 0; i < this.fieldSlots.size(); i++) {
            FieldSlot slot = this.fieldSlots.get(i);
            int row = i / columns;
            int col = i % columns;
            int x = this.contentViewportX + col * (cellWidth + gapX);
            int y = startY + row * rowHeight;
            boolean visible = y + rowHeight >= this.contentViewportY && y <= viewportBottom;

            if (visible) {
                slot.field.x = x;
                slot.field.y = y + 14;
                slot.field.width = cellWidth;
                slot.field.height = 18;
            } else {
                slot.field.x = -2000;
                slot.field.y = -2000;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_ENABLED) {
            HumanLikeMovementConfig.INSTANCE.enabled = !HumanLikeMovementConfig.INSTANCE.enabled;
            enabledButton.setEnabledState(HumanLikeMovementConfig.INSTANCE.enabled);
            enabledButton.displayString = buildEnabledLabel(HumanLikeMovementConfig.INSTANCE.enabled);
            if (!HumanLikeMovementConfig.INSTANCE.enabled) {
                HumanLikeMovementController.INSTANCE.reset();
            }
            return;
        }

        if (button.id == BTN_STUCK_RECOVERY) {
            HumanLikeMovementConfig.INSTANCE.enableStuckRecovery = !HumanLikeMovementConfig.INSTANCE.enableStuckRecovery;
            stuckRecoveryButton.setEnabledState(HumanLikeMovementConfig.INSTANCE.enableStuckRecovery);
            stuckRecoveryButton.displayString = buildStuckRecoveryLabel(
                    HumanLikeMovementConfig.INSTANCE.enableStuckRecovery);
            return;
        }

        if (button.id == BTN_SAVE) {
            saveValues();
            this.mc.setScreen(parentScreen);
            return;
        }

        if (button.id == BTN_RELOAD) {
            HumanLikeMovementConfig.load();
            HumanLikeMovementController.INSTANCE.reset();
            this.contentScroll = 0;
            initGui();
            return;
        }

        if (button.id == BTN_RESET_DEFAULTS) {
            HumanLikeMovementConfig.INSTANCE = new HumanLikeMovementConfig();
            HumanLikeMovementConfig.INSTANCE.normalize();
            HumanLikeMovementController.INSTANCE.reset();
            this.contentScroll = 0;
            initGui();
            return;
        }

        if (button.id == BTN_CANCEL) {
            HumanLikeMovementConfig.load();
            HumanLikeMovementController.INSTANCE.reset();
            this.mc.setScreen(parentScreen);
        }
    }

    private void saveValues() {
        HumanLikeMovementConfig config = HumanLikeMovementConfig.INSTANCE;
        config.minTurnSpeed = parseFloat(minTurnSpeedField, config.minTurnSpeed);
        config.maxTurnSpeed = parseFloat(maxTurnSpeedField, config.maxTurnSpeed);
        config.viewJitter = parseFloat(viewJitterField, config.viewJitter);
        config.turnOvershoot = parseFloat(turnOvershootField, config.turnOvershoot);
        config.turnSlowdown = parseFloat(turnSlowdownField, config.turnSlowdown);
        config.acceleration = parseFloat(accelerationField, config.acceleration);
        config.deceleration = parseFloat(decelerationField, config.deceleration);
        config.narrowSlowdown = parseFloat(narrowSlowdownField, config.narrowSlowdown);
        config.strafeJitterChance = parseFloat(strafeJitterChanceField, config.strafeJitterChance);
        config.strafeJitterStrength = parseFloat(strafeJitterStrengthField, config.strafeJitterStrength);
        config.microPauseChance = parseFloat(microPauseChanceField, config.microPauseChance);
        config.rhythmVariation = parseFloat(rhythmVariationField, config.rhythmVariation);
        config.corridorBiasStrength = parseFloat(corridorBiasStrengthField, config.corridorBiasStrength);
        config.routeNoiseStrength = parseFloat(routeNoiseStrengthField, config.routeNoiseStrength);
        config.routeNoiseScale = parseInt(routeNoiseScaleField, config.routeNoiseScale);
        config.routeAnchorRadius = parseFloat(routeAnchorRadiusField, config.routeAnchorRadius);
        config.startTurnThreshold = parseFloat(startTurnThresholdField, config.startTurnThreshold);
        config.lightHopChance = parseFloat(lightHopChanceField, config.lightHopChance);
        config.lightHopCooldownTicks = parseInt(lightHopCooldownField, config.lightHopCooldownTicks);
        config.finalApproachDistance = parseFloat(finalApproachDistanceField, config.finalApproachDistance);
        config.finalApproachSlowdown = parseFloat(finalApproachSlowdownField, config.finalApproachSlowdown);
        config.stuckRecoveryTicks = parseInt(stuckRecoveryTicksField, config.stuckRecoveryTicks);
        config.stuckRecoveryStrafeStrength = parseFloat(stuckRecoveryStrafeStrengthField,
                config.stuckRecoveryStrafeStrength);
        config.stuckRecoveryMinTicks = parseInt(stuckRecoveryMinTicksField, config.stuckRecoveryMinTicks);
        config.stuckRecoveryMaxTicks = parseInt(stuckRecoveryMaxTicksField, config.stuckRecoveryMaxTicks);
        config.enableStuckRecovery = stuckRecoveryButton != null && stuckRecoveryButton.getEnabledState();
        config.normalize();
        syncFieldsFromConfig(config);
        HumanLikeMovementConfig.save();
        if (!config.enabled) {
            HumanLikeMovementController.INSTANCE.reset();
        }
    }

    private float parseFloat(GuiTextField field, float fallback) {
        if (field == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(field.getText().trim());
        } catch (Exception ignored) {
            field.setText(String.valueOf(fallback));
            return fallback;
        }
    }

    private int parseInt(GuiTextField field, int fallback) {
        if (field == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ignored) {
            field.setText(String.valueOf(fallback));
            return fallback;
        }
    }

    private String buildEnabledLabel(boolean enabled) {
        return enabled ? "§a模拟真人：已开启" : "§c模拟真人：已关闭";
    }

    private String buildStuckRecoveryLabel(boolean enabled) {
        return enabled ? "§a卡住恢复：已开启" : "§7卡住恢复：已关闭";
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.hoverTooltipLines.clear();
        updateFieldLayout();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "模拟真人设置", this.fontRenderer);

        drawString(this.fontRenderer, "支持自适应分辨率；内容过多时可在面板内滚轮滚动，可点“重载配置”重新读取 json",
                panelX + 14, panelY + panelHeight - 44, 0xFFB8C7D9);

        drawViewportFrame();
        drawFieldLabels(mouseX, mouseY);

        for (FieldSlot slot : this.fieldSlots) {
            if (slot.field.x > -1000) {
                drawThemedTextField(slot.field);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (mouseX >= enabledButton.x && mouseX <= enabledButton.x + enabledButton.width
                && mouseY >= enabledButton.y && mouseY <= enabledButton.y + enabledButton.height) {
            setHoverTooltip(Arrays.asList(
                    "开启后会对 Baritone 的最后一层移动表现进行拟人化修饰",
                    "包括：平滑转头、起步先转头、末段收敛减速、狭窄减速、节奏变化、路径偏置、路线随机化、轻微横移扰动、微停顿、轻跳、卡住恢复"),
                    mouseX, mouseY);
        }

        drawCustomTooltip();
    }

    private void drawViewportFrame() {
        int x1 = this.contentViewportX - 4;
        int y1 = this.contentViewportY - 4;
        int x2 = this.contentViewportX + this.contentViewportW + 4;
        int y2 = this.contentViewportY + this.contentViewportH + 4;
        Gui.drawRect(x1, y1, x2, y2, 0x33273A52);
        Gui.drawRect(this.contentViewportX - 1, this.contentViewportY - 1,
                this.contentViewportX + this.contentViewportW + 1, this.contentViewportY + this.contentViewportH + 1,
                0x553B5573);

        if (this.maxContentScroll > 0) {
            int trackX = this.contentViewportX + this.contentViewportW + 6;
            int trackY = this.contentViewportY;
            int trackH = this.contentViewportH;
            int thumbH = Math.max(18,
                    (int) (trackH * (this.contentViewportH / (float) (this.contentViewportH + this.maxContentScroll))));
            int thumbY = trackY + (int) ((trackH - thumbH) * (this.contentScroll / (float) this.maxContentScroll));
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackH, thumbY, thumbH);
        }
    }

    private void drawFieldLabels(int mouseX, int mouseY) {
        for (FieldSlot slot : this.fieldSlots) {
            if (slot.field.x <= -1000) {
                continue;
            }
            drawString(this.fontRenderer, slot.label, slot.field.x, slot.field.y - 10, GuiTheme.LABEL_TEXT);
            if (mouseX >= slot.field.x && mouseX <= slot.field.x + slot.field.width
                    && mouseY >= slot.field.y - 10 && mouseY <= slot.field.y + slot.field.height) {
                setHoverTooltip(buildFieldTooltip(slot), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (FieldSlot slot : this.fieldSlots) {
            if (slot.field.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (FieldSlot slot : this.fieldSlots) {
            if (slot.field.x > -1000) {
                slot.field.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || this.maxContentScroll <= 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;
        if (mouseX >= this.contentViewportX - 4 && mouseX <= this.contentViewportX + this.contentViewportW + 10
                && mouseY >= this.contentViewportY - 4 && mouseY <= this.contentViewportY + this.contentViewportH + 4) {
            if (wheel < 0) {
                this.contentScroll = Math.min(this.maxContentScroll, this.contentScroll + 18);
            } else {
                this.contentScroll = Math.max(0, this.contentScroll - 18);
            }
            updateFieldLayout();
        }
    }

    private void syncFieldsFromConfig(HumanLikeMovementConfig config) {
        minTurnSpeedField.setText(String.valueOf(config.minTurnSpeed));
        maxTurnSpeedField.setText(String.valueOf(config.maxTurnSpeed));
        viewJitterField.setText(String.valueOf(config.viewJitter));
        turnOvershootField.setText(String.valueOf(config.turnOvershoot));
        turnSlowdownField.setText(String.valueOf(config.turnSlowdown));
        accelerationField.setText(String.valueOf(config.acceleration));
        decelerationField.setText(String.valueOf(config.deceleration));
        narrowSlowdownField.setText(String.valueOf(config.narrowSlowdown));
        rhythmVariationField.setText(String.valueOf(config.rhythmVariation));
        corridorBiasStrengthField.setText(String.valueOf(config.corridorBiasStrength));
        strafeJitterChanceField.setText(String.valueOf(config.strafeJitterChance));
        strafeJitterStrengthField.setText(String.valueOf(config.strafeJitterStrength));
        microPauseChanceField.setText(String.valueOf(config.microPauseChance));
        startTurnThresholdField.setText(String.valueOf(config.startTurnThreshold));
        lightHopChanceField.setText(String.valueOf(config.lightHopChance));
        lightHopCooldownField.setText(String.valueOf(config.lightHopCooldownTicks));
        finalApproachDistanceField.setText(String.valueOf(config.finalApproachDistance));
        finalApproachSlowdownField.setText(String.valueOf(config.finalApproachSlowdown));
        stuckRecoveryTicksField.setText(String.valueOf(config.stuckRecoveryTicks));
        stuckRecoveryStrafeStrengthField.setText(String.valueOf(config.stuckRecoveryStrafeStrength));
        routeNoiseStrengthField.setText(String.valueOf(config.routeNoiseStrength));
        routeNoiseScaleField.setText(String.valueOf(config.routeNoiseScale));
        routeAnchorRadiusField.setText(String.valueOf(config.routeAnchorRadius));
        stuckRecoveryMinTicksField.setText(String.valueOf(config.stuckRecoveryMinTicks));
        stuckRecoveryMaxTicksField.setText(String.valueOf(config.stuckRecoveryMaxTicks));
    }

    private List<String> buildFieldTooltip(FieldSlot slot) {
        List<String> lines = new ArrayList<String>();
        lines.add("§e" + slot.label);
        lines.add("§7范围：§f" + slot.range);
        lines.add("§7说明：§f" + slot.description);
        lines.add("§7超出范围时保存会自动限制到合法区间");
        return lines;
    }

    private void setHoverTooltip(List<String> lines, int mouseX, int mouseY) {
        this.hoverTooltipLines.clear();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        this.hoverTooltipLines.addAll(lines);
        this.hoverTooltipX = mouseX;
        this.hoverTooltipY = mouseY;
    }

    private void drawCustomTooltip() {
        if (this.hoverTooltipLines == null || this.hoverTooltipLines.isEmpty()) {
            return;
        }

        int maxWidth = 0;
        for (String line : this.hoverTooltipLines) {
            maxWidth = Math.max(maxWidth, this.fontRenderer.getStringWidth(line));
        }

        int tooltipX = this.hoverTooltipX + 12;
        int tooltipY = this.hoverTooltipY - 4;
        int tooltipW = maxWidth + 10;
        int tooltipH = this.hoverTooltipLines.size() * 12 + 6;

        if (tooltipX + tooltipW > this.width - 6) {
            tooltipX = this.width - tooltipW - 6;
        }
        if (tooltipY + tooltipH > this.height - 6) {
            tooltipY = this.height - tooltipH - 6;
        }
        if (tooltipY < 6) {
            tooltipY = 6;
        }

        Gui.drawRect(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH, 0xEE152232);
        Gui.drawRect(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + 1, 0xFF4FA6D9);
        Gui.drawRect(tooltipX, tooltipY + tooltipH - 1, tooltipX + tooltipW, tooltipY + tooltipH, 0xFF35556E);

        for (int i = 0; i < this.hoverTooltipLines.size(); i++) {
            this.fontRenderer.drawStringWithShadow(this.hoverTooltipLines.get(i), tooltipX + 5, tooltipY + 4 + i * 12,
                    0xFFFFFFFF);
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private static final class FieldSlot {
        private final String label;
        private final GuiTextField field;
        private final String range;
        private final String description;

        private FieldSlot(String label, GuiTextField field, String range, String description) {
            this.label = label;
            this.field = field;
            this.range = range;
            this.description = description;
        }
    }
}






