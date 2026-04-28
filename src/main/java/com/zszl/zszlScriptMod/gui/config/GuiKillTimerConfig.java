package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.handlers.KillTimerHandler;

import java.io.IOException;
import java.util.Locale;

public class GuiKillTimerConfig extends ThemedGuiScreen {

    private static final int BTN_FREE_EDIT = 10;
    private static final int BTN_RESET = 11;
    private static final int BTN_DEATH_MODE = 20;
    private static final int BTN_SORT_MODE = 21;
    private static final int BTN_ONLY_OWN_DAMAGE = 22;
    private static final int BTN_COLLAPSE_EXTRA = 23;
    private static final int BTN_SHOW_REASON = 24;
    private static final int BTN_TYPE_HOSTILE = 40;
    private static final int BTN_TYPE_NEUTRAL = 41;
    private static final int BTN_TYPE_PASSIVE = 42;
    private static final int BTN_TYPE_PLAYER = 43;
    private static final int BTN_TYPE_WATER = 44;
    private static final int BTN_TYPE_AMBIENT = 45;
    private static final int BTN_TYPE_VILLAGER = 46;
    private static final int BTN_TYPE_GOLEM = 47;
    private static final int BTN_TYPE_TAMEABLE = 48;
    private static final int BTN_TYPE_BOSS = 49;
    private static final int BTN_SAVE = 100;
    private static final int ROW_HEIGHT = 24;

    private final GuiScreen parent;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField wField;
    private GuiTextField hField;
    private GuiTextField alphaField;
    private GuiTextField rangeField;
    private GuiTextField combatTimeoutField;
    private GuiTextField disengageRemoveField;
    private GuiTextField stableDpsIntervalField;
    private GuiTextField maxTargetsField;
    private GuiTextField deathHoldSecField;
    private GuiButton deathModeButton;
    private GuiButton sortModeButton;
    private GuiButton onlyOwnDamageButton;
    private GuiButton collapseExtraButton;
    private GuiButton showReasonButton;
    private GuiButton hostileButton;
    private GuiButton neutralButton;
    private GuiButton passiveButton;
    private GuiButton playerButton;
    private GuiButton waterButton;
    private GuiButton ambientButton;
    private GuiButton villagerButton;
    private GuiButton golemButton;
    private GuiButton tameableButton;
    private GuiButton bossButton;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int fieldX;
    private int fieldW;
    private int labelX;
    private int contentTopY;
    private int contentHeight;
    private int footerY;
    private int contentScroll = 0;
    private int maxContentScroll = 0;

    public GuiKillTimerConfig(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        panelW = Math.min(392, this.width - 24);
        panelH = Math.min(470, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        labelX = panelX + 12;
        fieldX = panelX + 178;
        fieldW = Math.min(184, panelW - 190);
        contentTopY = panelY + 34;
        footerY = panelY + panelH - 28;
        contentHeight = Math.max(80, footerY - contentTopY - 8);

        xField = createField(1, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.panelX));
        yField = createField(2, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.panelY));
        wField = createField(3, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.panelWidth));
        hField = createField(4, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.panelHeight));
        alphaField = createField(5, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.panelAlpha));
        rangeField = createField(6, fieldX, contentTopY, fieldW, trimDouble(KillTimerHandler.trackingRangeBlocks));
        combatTimeoutField = createField(7, fieldX, contentTopY, fieldW,
                String.valueOf(KillTimerHandler.combatTimeoutSeconds));
        disengageRemoveField = createField(8, fieldX, contentTopY, fieldW,
                String.valueOf(KillTimerHandler.disengageRemoveSeconds));
        stableDpsIntervalField = createField(9, fieldX, contentTopY, fieldW,
                String.valueOf(KillTimerHandler.stableDpsIntervalSeconds));
        maxTargetsField = createField(10, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.maxVisibleTargets));

        hostileButton = new ThemedButton(BTN_TYPE_HOSTILE, fieldX, contentTopY, fieldW, 20, "");
        neutralButton = new ThemedButton(BTN_TYPE_NEUTRAL, fieldX, contentTopY, fieldW, 20, "");
        passiveButton = new ThemedButton(BTN_TYPE_PASSIVE, fieldX, contentTopY, fieldW, 20, "");
        playerButton = new ThemedButton(BTN_TYPE_PLAYER, fieldX, contentTopY, fieldW, 20, "");
        waterButton = new ThemedButton(BTN_TYPE_WATER, fieldX, contentTopY, fieldW, 20, "");
        ambientButton = new ThemedButton(BTN_TYPE_AMBIENT, fieldX, contentTopY, fieldW, 20, "");
        villagerButton = new ThemedButton(BTN_TYPE_VILLAGER, fieldX, contentTopY, fieldW, 20, "");
        golemButton = new ThemedButton(BTN_TYPE_GOLEM, fieldX, contentTopY, fieldW, 20, "");
        tameableButton = new ThemedButton(BTN_TYPE_TAMEABLE, fieldX, contentTopY, fieldW, 20, "");
        bossButton = new ThemedButton(BTN_TYPE_BOSS, fieldX, contentTopY, fieldW, 20, "");
        this.buttonList.add(hostileButton);
        this.buttonList.add(neutralButton);
        this.buttonList.add(passiveButton);
        this.buttonList.add(playerButton);
        this.buttonList.add(waterButton);
        this.buttonList.add(ambientButton);
        this.buttonList.add(villagerButton);
        this.buttonList.add(golemButton);
        this.buttonList.add(tameableButton);
        this.buttonList.add(bossButton);

        sortModeButton = new ThemedButton(BTN_SORT_MODE, fieldX, contentTopY, fieldW, 20, buildSortModeDisplay());
        this.buttonList.add(sortModeButton);

        onlyOwnDamageButton = new ThemedButton(BTN_ONLY_OWN_DAMAGE, fieldX, contentTopY, fieldW, 20,
                buildToggleLabel("gui.killtimer.only_own_damage", KillTimerHandler.onlyOwnDamage));
        this.buttonList.add(onlyOwnDamageButton);

        collapseExtraButton = new ThemedButton(BTN_COLLAPSE_EXTRA, fieldX, contentTopY, fieldW, 20,
                buildToggleLabel("gui.killtimer.collapse_extra", KillTimerHandler.collapseExtraTargets));
        this.buttonList.add(collapseExtraButton);

        showReasonButton = new ThemedButton(BTN_SHOW_REASON, fieldX, contentTopY, fieldW, 20,
                buildToggleLabel("gui.killtimer.show_reason", KillTimerHandler.showDisengageReason));
        this.buttonList.add(showReasonButton);

        deathModeButton = new ThemedButton(BTN_DEATH_MODE, fieldX, contentTopY, fieldW, 20, getDeathModeDisplay());
        this.buttonList.add(deathModeButton);

        deathHoldSecField = createField(11, fieldX, contentTopY, fieldW, String.valueOf(KillTimerHandler.deathPanelHoldSeconds));

        this.buttonList.add(new ThemedButton(BTN_FREE_EDIT, panelX + 10, footerY, 110, 20,
                I18n.format("gui.killtimer.btn.free_edit")));
        this.buttonList.add(new ThemedButton(BTN_RESET, panelX + 128, footerY, 110, 20,
                I18n.format("gui.killtimer.btn.reset")));
        this.buttonList.add(new ThemedButton(BTN_SAVE, panelX + panelW - 90, footerY, 80, 20,
                I18n.format("gui.killtimer.btn.save")));
        refreshButtonLabels();
        updateScrollableLayout();
    }

    private GuiTextField createField(int id, int x, int y, int width, String text) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, width, 20);
        field.setText(text == null ? "" : text);
        return field;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_FREE_EDIT) {
            this.mc.setScreen(null);
            KillTimerHandler.enterFreeEditMode();
            if (this.mc.player != null) {
                this.mc.player.displayClientMessage(
                        new com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString(
                                I18n.format("msg.killtimer.enter_free_edit")), false);
            }
            return;
        }
        if (button.id == BTN_RESET) {
            resetDefaults();
            syncFromHandler();
            return;
        }
        if (button.id == BTN_DEATH_MODE) {
            KillTimerHandler.deathDataMode = KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_CHAT
                    ? KillTimerHandler.DEATH_MODE_PANEL_HOLD
                    : KillTimerHandler.DEATH_MODE_CHAT;
            refreshButtonLabels();
            return;
        }
        if (button.id == BTN_SORT_MODE) {
            KillTimerHandler.sortMode = nextSortMode(KillTimerHandler.sortMode);
            refreshButtonLabels();
            return;
        }
        if (button.id == BTN_ONLY_OWN_DAMAGE) {
            KillTimerHandler.onlyOwnDamage = !KillTimerHandler.onlyOwnDamage;
            refreshButtonLabels();
            return;
        }
        if (button.id == BTN_COLLAPSE_EXTRA) {
            KillTimerHandler.collapseExtraTargets = !KillTimerHandler.collapseExtraTargets;
            refreshButtonLabels();
            return;
        }
        if (button.id == BTN_SHOW_REASON) {
            KillTimerHandler.showDisengageReason = !KillTimerHandler.showDisengageReason;
            refreshButtonLabels();
            return;
        }
        if (button.id >= BTN_TYPE_HOSTILE && button.id <= BTN_TYPE_BOSS) {
            toggleTargetType(button.id);
            KillTimerHandler.ensureAtLeastOneTargetTypeEnabled();
            refreshButtonLabels();
            return;
        }
        if (button.id == BTN_SAVE) {
            applyFieldsToHandler();
            KillTimerHandler.ensureAtLeastOneTargetTypeEnabled();
            KillTimerHandler.saveConfig();
            this.mc.setScreen(parent);
        }
    }

    private void resetDefaults() {
        KillTimerHandler.panelX = 0;
        KillTimerHandler.panelY = 22;
        KillTimerHandler.panelWidth = 219;
        KillTimerHandler.panelHeight = 100;
        KillTimerHandler.panelAlpha = 100;
        KillTimerHandler.trackingRangeBlocks = 10.0D;
        KillTimerHandler.combatTimeoutSeconds = 3;
        KillTimerHandler.disengageRemoveSeconds = 3;
        KillTimerHandler.stableDpsIntervalSeconds = 2;
        KillTimerHandler.maxVisibleTargets = 6;
        KillTimerHandler.collapseExtraTargets = true;
        KillTimerHandler.showDisengageReason = false;
        KillTimerHandler.onlyOwnDamage = false;
        KillTimerHandler.targetHostile = true;
        KillTimerHandler.targetNeutral = false;
        KillTimerHandler.targetPassive = false;
        KillTimerHandler.targetPlayers = false;
        KillTimerHandler.targetWater = false;
        KillTimerHandler.targetAmbient = false;
        KillTimerHandler.targetVillager = false;
        KillTimerHandler.targetGolem = false;
        KillTimerHandler.targetTameable = false;
        KillTimerHandler.targetBoss = false;
        KillTimerHandler.sortMode = KillTimerHandler.SORT_RECENT;
        KillTimerHandler.deathDataMode = KillTimerHandler.DEATH_MODE_CHAT;
        KillTimerHandler.deathPanelHoldSeconds = 3;
    }

    private void applyFieldsToHandler() {
        KillTimerHandler.panelX = Math.max(0, parseIntOrDefault(xField.getText(), KillTimerHandler.panelX));
        KillTimerHandler.panelY = Math.max(0, parseIntOrDefault(yField.getText(), KillTimerHandler.panelY));
        KillTimerHandler.panelWidth = Math.max(120,
                parseIntOrDefault(wField.getText(), KillTimerHandler.panelWidth));
        KillTimerHandler.panelHeight = Math.max(60,
                parseIntOrDefault(hField.getText(), KillTimerHandler.panelHeight));
        KillTimerHandler.panelAlpha = Math.max(30, Math.min(240,
                parseIntOrDefault(alphaField.getText(), KillTimerHandler.panelAlpha)));
        KillTimerHandler.trackingRangeBlocks = Math.max(1.0D,
                parseDoubleOrDefault(rangeField.getText(), KillTimerHandler.trackingRangeBlocks));
        KillTimerHandler.combatTimeoutSeconds = Math.max(1,
                parseIntOrDefault(combatTimeoutField.getText(), KillTimerHandler.combatTimeoutSeconds));
        KillTimerHandler.disengageRemoveSeconds = Math.max(1,
                parseIntOrDefault(disengageRemoveField.getText(), KillTimerHandler.disengageRemoveSeconds));
        KillTimerHandler.stableDpsIntervalSeconds = Math.max(1,
                parseIntOrDefault(stableDpsIntervalField.getText(), KillTimerHandler.stableDpsIntervalSeconds));
        KillTimerHandler.maxVisibleTargets = Math.max(1,
                parseIntOrDefault(maxTargetsField.getText(), KillTimerHandler.maxVisibleTargets));
        KillTimerHandler.deathPanelHoldSeconds = Math.max(1,
                parseIntOrDefault(deathHoldSecField.getText(), KillTimerHandler.deathPanelHoldSeconds));
    }

    private void syncFromHandler() {
        xField.setText(String.valueOf(KillTimerHandler.panelX));
        yField.setText(String.valueOf(KillTimerHandler.panelY));
        wField.setText(String.valueOf(KillTimerHandler.panelWidth));
        hField.setText(String.valueOf(KillTimerHandler.panelHeight));
        alphaField.setText(String.valueOf(KillTimerHandler.panelAlpha));
        rangeField.setText(trimDouble(KillTimerHandler.trackingRangeBlocks));
        combatTimeoutField.setText(String.valueOf(KillTimerHandler.combatTimeoutSeconds));
        disengageRemoveField.setText(String.valueOf(KillTimerHandler.disengageRemoveSeconds));
        stableDpsIntervalField.setText(String.valueOf(KillTimerHandler.stableDpsIntervalSeconds));
        maxTargetsField.setText(String.valueOf(KillTimerHandler.maxVisibleTargets));
        deathHoldSecField.setText(String.valueOf(KillTimerHandler.deathPanelHoldSeconds));
        refreshButtonLabels();
        updateScrollableLayout();
    }

    private void refreshButtonLabels() {
        if (deathModeButton != null) {
            deathModeButton.displayString = getDeathModeDisplay();
        }
        if (sortModeButton != null) {
            sortModeButton.displayString = buildSortModeDisplay();
        }
        if (onlyOwnDamageButton != null) {
            onlyOwnDamageButton.displayString = buildToggleLabel("gui.killtimer.only_own_damage",
                    KillTimerHandler.onlyOwnDamage);
        }
        if (collapseExtraButton != null) {
            collapseExtraButton.displayString = buildToggleLabel("gui.killtimer.collapse_extra",
                    KillTimerHandler.collapseExtraTargets);
        }
        if (showReasonButton != null) {
            showReasonButton.displayString = buildToggleLabel("gui.killtimer.show_reason",
                    KillTimerHandler.showDisengageReason);
        }
        if (hostileButton != null) {
            hostileButton.displayString = buildToggleLabel("gui.killtimer.type.hostile", KillTimerHandler.targetHostile);
            neutralButton.displayString = buildToggleLabel("gui.killtimer.type.neutral", KillTimerHandler.targetNeutral);
            passiveButton.displayString = buildToggleLabel("gui.killtimer.type.passive", KillTimerHandler.targetPassive);
            playerButton.displayString = buildToggleLabel("gui.killtimer.type.player", KillTimerHandler.targetPlayers);
            waterButton.displayString = buildToggleLabel("gui.killtimer.type.water", KillTimerHandler.targetWater);
            ambientButton.displayString = buildToggleLabel("gui.killtimer.type.ambient", KillTimerHandler.targetAmbient);
            villagerButton.displayString = buildToggleLabel("gui.killtimer.type.villager", KillTimerHandler.targetVillager);
            golemButton.displayString = buildToggleLabel("gui.killtimer.type.golem", KillTimerHandler.targetGolem);
            tameableButton.displayString = buildToggleLabel("gui.killtimer.type.tameable", KillTimerHandler.targetTameable);
            bossButton.displayString = buildToggleLabel("gui.killtimer.type.boss", KillTimerHandler.targetBoss);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, I18n.format("gui.killtimer.title"), this.fontRenderer);
        drawRect(panelX + 8, contentTopY - 2, panelX + panelW - 12, contentTopY + contentHeight + 2, 0x22161F2B);

        updateScrollableLayout();

        drawScrollableLabel(0, "gui.killtimer.x");
        drawScrollableLabel(1, "gui.killtimer.y");
        drawScrollableLabel(2, "gui.killtimer.w");
        drawScrollableLabel(3, "gui.killtimer.h");
        drawScrollableLabel(4, "gui.killtimer.alpha");
        drawScrollableLabel(5, "gui.killtimer.range");
        drawScrollableLabel(6, "gui.killtimer.combat_timeout");
        drawScrollableLabel(7, "gui.killtimer.disengage_remove");
        drawScrollableLabel(8, "gui.killtimer.stable_dps_interval");
        drawScrollableLabel(9, "gui.killtimer.max_targets");
        drawScrollableLabel(10, "gui.killtimer.entity_types");

        drawScrollableField(xField);
        drawScrollableField(yField);
        drawScrollableField(wField);
        drawScrollableField(hField);
        drawScrollableField(alphaField);
        drawScrollableField(rangeField);
        drawScrollableField(combatTimeoutField);
        drawScrollableField(disengageRemoveField);
        drawScrollableField(stableDpsIntervalField);
        drawScrollableField(maxTargetsField);

        drawScrollableButton(hostileButton);
        drawScrollableButton(neutralButton);
        drawScrollableButton(passiveButton);
        drawScrollableButton(playerButton);
        drawScrollableButton(waterButton);
        drawScrollableButton(ambientButton);
        drawScrollableButton(villagerButton);
        drawScrollableButton(golemButton);
        drawScrollableButton(tameableButton);
        drawScrollableButton(bossButton);
        drawScrollableButton(sortModeButton);
        drawScrollableButton(onlyOwnDamageButton);
        drawScrollableButton(collapseExtraButton);
        drawScrollableButton(showReasonButton);
        drawScrollableButton(deathModeButton);

        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD) {
            drawScrollableLabel(26, "gui.killtimer.hold_sec");
            drawScrollableField(deathHoldSecField);
        }

        int tipY = getScrollRowY(KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD ? 27 : 26);
        if (isRowVisible(tipY)) {
            this.drawString(this.fontRenderer, I18n.format("gui.killtimer.tip"), labelX, tipY, 0xFFBBBBBB);
        }

        drawScrollbar();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawScrollableLabel(int rowIndex, String key) {
        int y = getScrollRowY(rowIndex);
        if (isRowVisible(y)) {
            this.drawString(this.fontRenderer, I18n.format(key), labelX, y, 0xFFFFFF);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (textboxKeyTyped(xField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(yField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(wField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(hField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(alphaField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(rangeField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(combatTimeoutField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(disengageRemoveField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(stableDpsIntervalField, typedChar, keyCode))
            return;
        if (textboxKeyTyped(maxTargetsField, typedChar, keyCode))
            return;
        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD
                && textboxKeyTyped(deathHoldSecField, typedChar, keyCode))
            return;
        super.keyTyped(typedChar, keyCode);
    }

    private boolean textboxKeyTyped(GuiTextField field, char typedChar, int keyCode) {
        return field != null && field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        clickFieldIfVisible(xField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(yField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(wField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(hField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(alphaField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(rangeField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(combatTimeoutField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(disengageRemoveField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(stableDpsIntervalField, mouseX, mouseY, mouseButton);
        clickFieldIfVisible(maxTargetsField, mouseX, mouseY, mouseButton);
        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD) {
            clickFieldIfVisible(deathHoldSecField, mouseX, mouseY, mouseButton);
        }
    }

    private void clickFieldIfVisible(GuiTextField field, int mouseX, int mouseY, int mouseButton) {
        if (field != null && isFieldVisible(field)) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private void toggleTargetType(int buttonId) {
        switch (buttonId) {
            case BTN_TYPE_HOSTILE:
                KillTimerHandler.targetHostile = !KillTimerHandler.targetHostile;
                break;
            case BTN_TYPE_NEUTRAL:
                KillTimerHandler.targetNeutral = !KillTimerHandler.targetNeutral;
                break;
            case BTN_TYPE_PASSIVE:
                KillTimerHandler.targetPassive = !KillTimerHandler.targetPassive;
                break;
            case BTN_TYPE_PLAYER:
                KillTimerHandler.targetPlayers = !KillTimerHandler.targetPlayers;
                break;
            case BTN_TYPE_WATER:
                KillTimerHandler.targetWater = !KillTimerHandler.targetWater;
                break;
            case BTN_TYPE_AMBIENT:
                KillTimerHandler.targetAmbient = !KillTimerHandler.targetAmbient;
                break;
            case BTN_TYPE_VILLAGER:
                KillTimerHandler.targetVillager = !KillTimerHandler.targetVillager;
                break;
            case BTN_TYPE_GOLEM:
                KillTimerHandler.targetGolem = !KillTimerHandler.targetGolem;
                break;
            case BTN_TYPE_TAMEABLE:
                KillTimerHandler.targetTameable = !KillTimerHandler.targetTameable;
                break;
            case BTN_TYPE_BOSS:
                KillTimerHandler.targetBoss = !KillTimerHandler.targetBoss;
                break;
            default:
                break;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }
        int mouseX = Mouse.getEventX() * this.width / Math.max(1, this.mc.getWindow().getScreenWidth());
        int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(1, this.mc.getWindow().getScreenHeight()) - 1;
        if (isHoverRegion(mouseX, mouseY, panelX + 8, contentTopY, panelW - 20, contentHeight)) {
            contentScroll = Math.max(0, Math.min(maxContentScroll, contentScroll + (dWheel < 0 ? 14 : -14)));
            unfocusInvisibleFields();
            updateScrollableLayout();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private int parseIntOrDefault(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private double parseDoubleOrDefault(String text, double def) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String trimDouble(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void updateScrollableLayout() {
        int totalRows = KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD ? 28 : 27;
        int contentBottom = totalRows * ROW_HEIGHT + 8;
        maxContentScroll = Math.max(0, contentBottom - contentHeight);
        contentScroll = Math.max(0, Math.min(contentScroll, maxContentScroll));

        layoutField(xField, 0);
        layoutField(yField, 1);
        layoutField(wField, 2);
        layoutField(hField, 3);
        layoutField(alphaField, 4);
        layoutField(rangeField, 5);
        layoutField(combatTimeoutField, 6);
        layoutField(disengageRemoveField, 7);
        layoutField(stableDpsIntervalField, 8);
        layoutField(maxTargetsField, 9);
        layoutButton(hostileButton, 11);
        layoutButton(neutralButton, 12);
        layoutButton(passiveButton, 13);
        layoutButton(playerButton, 14);
        layoutButton(waterButton, 15);
        layoutButton(ambientButton, 16);
        layoutButton(villagerButton, 17);
        layoutButton(golemButton, 18);
        layoutButton(tameableButton, 19);
        layoutButton(bossButton, 20);
        layoutButton(sortModeButton, 21);
        layoutButton(onlyOwnDamageButton, 22);
        layoutButton(collapseExtraButton, 23);
        layoutButton(showReasonButton, 24);
        layoutButton(deathModeButton, 25);
        if (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD) {
            layoutField(deathHoldSecField, 26);
        } else {
            deathHoldSecField.y = -1000;
        }
    }

    private void layoutField(GuiTextField field, int rowIndex) {
        if (field == null) {
            return;
        }
        field.x = fieldX;
        field.y = getScrollRowY(rowIndex) - 4;
        field.width = fieldW;
    }

    private void layoutButton(GuiButton button, int rowIndex) {
        if (button == null) {
            return;
        }
        button.x = fieldX;
        button.y = getScrollRowY(rowIndex) - 4;
        button.width = fieldW;
        button.visible = isRowVisible(getScrollRowY(rowIndex));
    }

    private int getScrollRowY(int rowIndex) {
        return contentTopY + rowIndex * ROW_HEIGHT - contentScroll;
    }

    private boolean isRowVisible(int y) {
        return y >= contentTopY - 18 && y <= contentTopY + contentHeight - 2;
    }

    private boolean isFieldVisible(GuiTextField field) {
        return field != null && field.y + field.height >= contentTopY && field.y <= contentTopY + contentHeight;
    }

    private void drawScrollableField(GuiTextField field) {
        if (isFieldVisible(field)) {
            drawThemedTextField(field);
        }
    }

    private void drawScrollableButton(GuiButton button) {
        if (button != null) {
            button.visible = isRowVisible(button.y + 4);
        }
    }

    private void drawScrollbar() {
        if (maxContentScroll <= 0) {
            return;
        }
        int barX = panelX + panelW - 8;
        int barY = contentTopY;
        int barH = contentHeight;
        int thumbH = Math.max(18, (int) ((contentHeight / (float) (contentHeight + maxContentScroll)) * barH));
        int track = Math.max(1, barH - thumbH);
        int thumbY = barY + (int) ((contentScroll / (float) Math.max(1, maxContentScroll)) * track);
        GuiTheme.drawScrollbar(barX, barY, 4, barH, thumbY, thumbH);
    }

    private void unfocusInvisibleFields() {
        GuiTextField[] fields = { xField, yField, wField, hField, alphaField, rangeField, combatTimeoutField,
                disengageRemoveField, stableDpsIntervalField, maxTargetsField, deathHoldSecField };
        for (GuiTextField field : fields) {
            if (field != null && !isFieldVisible(field)) {
                field.setFocused(false);
            }
        }
    }

    private String buildToggleLabel(String key, boolean enabled) {
        return I18n.format(key) + ": " + stateText(enabled);
    }

    private String stateText(boolean enabled) {
        return enabled ? I18n.format("gui.common.enabled") : I18n.format("gui.common.disabled");
    }

    private String buildSortModeDisplay() {
        return I18n.format("gui.killtimer.sort_mode") + ": " + I18n.format(getSortModeKey(KillTimerHandler.sortMode));
    }

    private String getDeathModeDisplay() {
        return I18n.format("gui.killtimer.death_mode") + ": "
                + (KillTimerHandler.deathDataMode == KillTimerHandler.DEATH_MODE_PANEL_HOLD
                        ? I18n.format("gui.killtimer.mode.hold")
                        : I18n.format("gui.killtimer.mode.chat"));
    }

    private String nextSortMode(String current) {
        String normalized = KillTimerHandler.SORT_RECENT;
        if (KillTimerHandler.SORT_DPS.equalsIgnoreCase(current)) {
            normalized = KillTimerHandler.SORT_DPS;
        } else if (KillTimerHandler.SORT_HEALTH.equalsIgnoreCase(current)) {
            normalized = KillTimerHandler.SORT_HEALTH;
        } else if (KillTimerHandler.SORT_DURATION.equalsIgnoreCase(current)) {
            normalized = KillTimerHandler.SORT_DURATION;
        }

        if (KillTimerHandler.SORT_RECENT.equals(normalized)) {
            return KillTimerHandler.SORT_DPS;
        }
        if (KillTimerHandler.SORT_DPS.equals(normalized)) {
            return KillTimerHandler.SORT_HEALTH;
        }
        if (KillTimerHandler.SORT_HEALTH.equals(normalized)) {
            return KillTimerHandler.SORT_DURATION;
        }
        return KillTimerHandler.SORT_RECENT;
    }

    private String getSortModeKey(String sortMode) {
        if (KillTimerHandler.SORT_DPS.equalsIgnoreCase(sortMode)) {
            return "gui.killtimer.sort_mode.dps";
        }
        if (KillTimerHandler.SORT_HEALTH.equalsIgnoreCase(sortMode)) {
            return "gui.killtimer.sort_mode.health";
        }
        if (KillTimerHandler.SORT_DURATION.equalsIgnoreCase(sortMode)) {
            return "gui.killtimer.sort_mode.duration";
        }
        return "gui.killtimer.sort_mode.recent";
    }
}
