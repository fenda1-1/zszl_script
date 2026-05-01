package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiCompatContext;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.RenderGameOverlayEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KillTimerHandler {

    public static final KillTimerHandler INSTANCE = new KillTimerHandler();
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean isEnabled = false;

    public static int panelX = 0;
    public static int panelY = 22;
    public static int panelWidth = 219;
    public static int panelHeight = 100;
    public static int panelAlpha = 100;

    public static int combatTimeoutSeconds = 3;
    public static int disengageRemoveSeconds = 3;
    public static int stableDpsIntervalSeconds = 2;
    public static double trackingRangeBlocks = 10.0D;
    public static int maxVisibleTargets = 6;
    public static boolean collapseExtraTargets = true;
    public static boolean showDisengageReason = false;
    public static boolean onlyOwnDamage = false;
    public static boolean targetHostile = true;
    public static boolean targetNeutral = false;
    public static boolean targetPassive = false;
    public static boolean targetPlayers = false;
    public static boolean targetWater = false;
    public static boolean targetAmbient = false;
    public static boolean targetVillager = false;
    public static boolean targetGolem = false;
    public static boolean targetTameable = false;
    public static boolean targetBoss = false;
    public static final String SORT_RECENT = "recent";
    public static final String SORT_DPS = "dps";
    public static final String SORT_HEALTH = "health";
    public static final String SORT_DURATION = "duration";
    public static String sortMode = SORT_RECENT;
    private static final long OWN_DAMAGE_GRACE_MS = 1800L;
    public static final int DEATH_MODE_CHAT = 0;
    public static final int DEATH_MODE_PANEL_HOLD = 1;
    public static int deathDataMode = DEATH_MODE_CHAT;
    public static int deathPanelHoldSeconds = 3;
    public static boolean freeEditMode = false;
    private static boolean pendingMouseGrabAfterEdit = false;
    private static boolean escKeyWasDown = false;

    public static int scrollOffset = 0;

    private static final int MIN_W = 120;
    private static final int MIN_H = 60;
    private static final int HEADER_H = 18;
    private static final int FOOTER_H = 18;
    private static final int CARD_H = 28;
    private static final int CARD_GAP = 4;
    private static final int PADDING = 6;

    private static boolean dragging = false;
    private static boolean resizing = false;
    private static int dragOffsetX;
    private static int dragOffsetY;
    private static int resizeStartX;
    private static int resizeStartY;
    private static int resizeStartW;
    private static int resizeStartH;
    private static int selectedIndex = -1;

    private static final Map<UUID, TrackInfo> tracked = new HashMap<>();
    private static final Map<UUID, Long> recentPlayerAttackMs = new HashMap<>();

    private static class TrackInfo {
        UUID id;
        String name;
        float maxHealth;
        float currentHealth;
        float minHealth;

        long combatStartMs;
        long lastDamageLikeMs;
        boolean paused;
        long pausedAccumulatedMs;
        long pausedStartMs;

        double stableDps;
        long lastStableDpsUpdateMs;
        boolean dead;
        long deathAtMs;
        long deathFreezeUntilMs;
        boolean withinRangeLastTick;
        String disengageReason;
        long disengageDeadlineMs;

        TrackInfo(LivingEntity entity) {
            this.id = entity.getUUID();
            this.name = entity.getName().getString();
            this.maxHealth = Math.max(1.0F, entity.getMaxHealth());
            this.currentHealth = Math.max(0.0F, entity.getHealth());
            this.minHealth = this.currentHealth;
            long now = System.currentTimeMillis();
            this.combatStartMs = now;
            this.lastDamageLikeMs = now;
            this.paused = false;
            this.pausedAccumulatedMs = 0L;
            this.pausedStartMs = 0L;
            this.stableDps = 0.0;
            this.lastStableDpsUpdateMs = now;
            this.dead = false;
            this.deathAtMs = 0L;
            this.deathFreezeUntilMs = 0L;
            this.withinRangeLastTick = true;
            this.disengageReason = "";
            this.disengageDeadlineMs = 0L;
        }

        double getActiveSeconds(long now) {
            long pausedNow = pausedAccumulatedMs;
            if (paused) {
                pausedNow += Math.max(0L, now - pausedStartMs);
            }
            long activeMs = Math.max(1L, now - combatStartMs - pausedNow);
            return activeMs / 1000.0;
        }
    }

    private static class ConfigData {
        boolean enabled = false;
        int panelX = 0;
        int panelY = 22;
        int panelWidth = 219;
        int panelHeight = 100;
        int panelAlpha = 100;
        int combatTimeoutSeconds = 3;
        int disengageRemoveSeconds = 3;
        int stableDpsIntervalSeconds = 2;
        double trackingRangeBlocks = 10.0D;
        int maxVisibleTargets = 6;
        boolean collapseExtraTargets = true;
        boolean showDisengageReason = false;
        boolean onlyOwnDamage = false;
        boolean targetHostile = true;
        boolean targetNeutral = false;
        boolean targetPassive = false;
        boolean targetPlayers = false;
        boolean targetWater = false;
        boolean targetAmbient = false;
        boolean targetVillager = false;
        boolean targetGolem = false;
        boolean targetTameable = false;
        boolean targetBoss = false;
        String sortMode = SORT_RECENT;
        int deathDataMode = DEATH_MODE_CHAT;
        int deathPanelHoldSeconds = 3;
    }

    private KillTimerHandler() {
    }

    public static void loadConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("kill_timer_config.json");
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) {
                return;
            }
            isEnabled = data.enabled;
            panelX = Math.max(0, data.panelX);
            panelY = Math.max(0, data.panelY);
            panelWidth = Math.max(MIN_W, data.panelWidth);
            panelHeight = Math.max(MIN_H, data.panelHeight);
            panelAlpha = MathHelper.clamp(data.panelAlpha, 30, 240);
            combatTimeoutSeconds = Math.max(1, data.combatTimeoutSeconds);
            disengageRemoveSeconds = Math.max(1, data.disengageRemoveSeconds);
            stableDpsIntervalSeconds = Math.max(1, data.stableDpsIntervalSeconds);
            trackingRangeBlocks = Math.max(1.0D, data.trackingRangeBlocks);
            maxVisibleTargets = Math.max(1, data.maxVisibleTargets);
            collapseExtraTargets = data.collapseExtraTargets;
            showDisengageReason = data.showDisengageReason;
            onlyOwnDamage = data.onlyOwnDamage;
            targetHostile = data.targetHostile;
            targetNeutral = data.targetNeutral;
            targetPassive = data.targetPassive;
            targetPlayers = data.targetPlayers;
            targetWater = data.targetWater;
            targetAmbient = data.targetAmbient;
            targetVillager = data.targetVillager;
            targetGolem = data.targetGolem;
            targetTameable = data.targetTameable;
            targetBoss = data.targetBoss;
            sortMode = normalizeSortMode(data.sortMode);
            deathDataMode = data.deathDataMode == DEATH_MODE_PANEL_HOLD ? DEATH_MODE_PANEL_HOLD : DEATH_MODE_CHAT;
            deathPanelHoldSeconds = Math.max(1, data.deathPanelHoldSeconds);
            ensureAtLeastOneTargetTypeEnabled();
        } catch (Exception ignored) {
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("kill_timer_config.json");
        try {
            ensureAtLeastOneTargetTypeEnabled();
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.enabled = isEnabled;
                data.panelX = panelX;
                data.panelY = panelY;
                data.panelWidth = panelWidth;
                data.panelHeight = panelHeight;
                data.panelAlpha = panelAlpha;
                data.combatTimeoutSeconds = combatTimeoutSeconds;
                data.disengageRemoveSeconds = disengageRemoveSeconds;
                data.stableDpsIntervalSeconds = stableDpsIntervalSeconds;
                data.trackingRangeBlocks = trackingRangeBlocks;
                data.maxVisibleTargets = maxVisibleTargets;
                data.collapseExtraTargets = collapseExtraTargets;
                data.showDisengageReason = showDisengageReason;
                data.onlyOwnDamage = onlyOwnDamage;
                data.targetHostile = targetHostile;
                data.targetNeutral = targetNeutral;
                data.targetPassive = targetPassive;
                data.targetPlayers = targetPlayers;
                data.targetWater = targetWater;
                data.targetAmbient = targetAmbient;
                data.targetVillager = targetVillager;
                data.targetGolem = targetGolem;
                data.targetTameable = targetTameable;
                data.targetBoss = targetBoss;
                data.sortMode = sortMode;
                data.deathDataMode = deathDataMode;
                data.deathPanelHoldSeconds = deathPanelHoldSeconds;
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static void toggleEnabled() {
        isEnabled = !isEnabled;
        if (!isEnabled) {
            clearRuntimeState();
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(new TextComponentString(I18n.format("msg.kill_timer.toggle",
                    isEnabled ? I18n.format("msg.kill_timer.state_on") : I18n.format("msg.kill_timer.state_off"))), false);
        }
        saveConfig();
    }

    public static void clearRuntimeState() {
        tracked.clear();
        recentPlayerAttackMs.clear();
        selectedIndex = -1;
        scrollOffset = 0;
        dragging = false;
        resizing = false;
        freeEditMode = false;
        pendingMouseGrabAfterEdit = false;
        escKeyWasDown = false;
    }

    public static void enterFreeEditMode() {
        freeEditMode = true;
        ModConfig.isMouseDetached = true;
        pendingMouseGrabAfterEdit = false;
        escKeyWasDown = false;
        if (mc.mouseHandler != null && mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }
        GuiBlockerHandler.blockNextGui(1);
    }

    public static void exitFreeEditMode() {
        if (!freeEditMode) {
            return;
        }
        freeEditMode = false;
        dragging = false;
        resizing = false;
        ModConfig.isMouseDetached = false;
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
            pendingMouseGrabAfterEdit = false;
        } else {
            pendingMouseGrabAfterEdit = true;
        }
        saveConfig();
        if (mc.player != null) {
            mc.player.displayClientMessage(new TextComponentString(I18n.format("msg.kill_timer.exit_free_edit")), false);
        }
    }

    @SubscribeEvent
    public void onMonsterDeath(LivingDeathEvent event) {
        if (!isEnabled || mc.level == null || event == null) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead == null || !dead.level().isClientSide()) {
            return;
        }

        TrackInfo info = tracked.get(dead.getUUID());
        if (info != null && mc.player != null) {
            long now = System.currentTimeMillis();
            info.currentHealth = 0.0F;
            info.dead = true;
            info.deathAtMs = now;
            info.deathFreezeUntilMs = now + Math.max(1, deathPanelHoldSeconds) * 1000L;
            if (!info.paused) {
                info.paused = true;
                info.pausedStartMs = now;
            }

            double sec = info.getActiveSeconds(now);
            double totalDmg = Math.max(0.0, info.maxHealth - info.minHealth);
            double dps = totalDmg / Math.max(0.001, sec);

            if (deathDataMode == DEATH_MODE_CHAT) {
                tracked.remove(dead.getUUID());
                mc.player.displayClientMessage(new TextComponentString(I18n.format("msg.kill_timer.kill_summary",
                        info.name, sec, formatCompactNumber(totalDmg), formatCompactNumber(dps))), false);
            }
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!isEnabled || event == null || mc.player == null || event.getEntity() != mc.player) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity living) || !matchesConfiguredEntityType(living)) {
            return;
        }
        recentPlayerAttackMs.put(living.getUUID(), System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!isEnabled || event == null || mc.screen != null || mc.player == null || mc.level == null) {
            return;
        }
        int listHeight = Math.max(0, panelHeight - HEADER_H - FOOTER_H - 6);
        int totalRowsHeight = tracked.size() * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, totalRowsHeight - listHeight);
        if (maxScroll <= 0) {
            return;
        }

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();
        int listX = panelX + PADDING;
        int listY = panelY + HEADER_H + 4;
        int listW = panelWidth - PADDING * 2 - 6;
        if (!inside(mouseX, mouseY, listX, listY, listW, listHeight)) {
            return;
        }

        double delta = event.getDeltaY();
        if (Math.abs(delta) < 0.0001D) {
            return;
        }

        scrollOffset = MathHelper.clamp(scrollOffset - (int) Math.round(delta) * (CARD_H + CARD_GAP), 0, maxScroll);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled || event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null
                || mc.level == null) {
            return;
        }

        if (freeEditMode) {
            boolean escDown = SimulatedKeyInputManager.isKeyDown(Keyboard.KEY_ESCAPE);
            if (escDown && !escKeyWasDown) {
                exitFreeEditMode();
            }
            escKeyWasDown = escDown;
        } else {
            escKeyWasDown = false;
        }

        if (pendingMouseGrabAfterEdit && mc.screen == null && !ModConfig.isMouseDetached) {
            mc.mouseHandler.grabMouse();
            pendingMouseGrabAfterEdit = false;
        }

        if (freeEditMode && mc.screen != null) {
            exitFreeEditMode();
        }

        updateFromWorld();
        handleMouseInteractions();
        renderPanel();
    }

    private static void updateFromWorld() {
        long now = System.currentTimeMillis();
        List<Entity> entities = new ArrayList<>();
        Set<UUID> seenEntityIds = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            entities.add(entity);
        }

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!matchesConfiguredEntityType(living)) {
                continue;
            }
            UUID id = living.getUUID();
            seenEntityIds.add(id);
            boolean withinRange = isWithinTrackingRange(living);

            if (!living.isAlive()) {
                TrackInfo track = tracked.get(id);
                if (track != null && track.dead && now <= track.deathFreezeUntilMs) {
                    track.currentHealth = 0.0F;
                } else {
                    tracked.remove(id);
                }
                continue;
            }

            float maxHp = Math.max(1.0F, living.getMaxHealth());
            float hp = Math.max(0.0F, living.getHealth());
            TrackInfo track = tracked.get(id);
            if (track == null) {
                if (withinRange && hp < maxHp && (!onlyOwnDamage || wasRecentlyAttackedByPlayer(id, now))) {
                    tracked.put(id, new TrackInfo(living));
                }
                continue;
            }

            track.dead = false;
            track.deathAtMs = 0L;
            track.deathFreezeUntilMs = 0L;
            track.name = living.getName().getString();
            track.maxHealth = Math.max(track.maxHealth, maxHp);

            if (withinRange) {
                if (!track.withinRangeLastTick) {
                    // Re-entering range should only refresh the baseline snapshot.
                    // Otherwise damage taken outside the configured range would be
                    // back-counted and immediately flip the state to "战斗中" again.
                    track.currentHealth = hp;
                    track.withinRangeLastTick = true;
                    if (!track.dead && !track.paused) {
                        track.disengageReason = "";
                    }
                } else {
                    float previousHp = track.currentHealth;
                    boolean tookDamageNow = hp < previousHp - 0.0001F;
                    track.currentHealth = hp;
                    if (tookDamageNow) {
                        if (!onlyOwnDamage || wasRecentlyAttackedByPlayer(id, now)) {
                            track.minHealth = Math.min(track.minHealth, hp);
                            track.lastDamageLikeMs = now;
                            resumeTrack(track, now);
                        }
                    }
                }
            } else {
                track.withinRangeLastTick = false;
                if (!track.dead) {
                    enterDisengaged(track, now, "range");
                }
            }
        }

        tracked.entrySet().removeIf(entry -> shouldRemoveTrack(now, entry.getKey(), entry.getValue(), seenEntityIds));
        recentPlayerAttackMs.entrySet().removeIf(entry -> now - entry.getValue() > OWN_DAMAGE_GRACE_MS * 3L);

        for (TrackInfo track : tracked.values()) {
            if (track.paused || track.dead) {
                continue;
            }
            if (now - track.lastStableDpsUpdateMs >= stableDpsIntervalSeconds * 1000L) {
                double sec = track.getActiveSeconds(now);
                double totalDmg = Math.max(0.0, track.maxHealth - track.minHealth);
                track.stableDps = totalDmg / Math.max(0.001, sec);
                track.lastStableDpsUpdateMs = now;
            }
        }
    }

    private static void handleMouseInteractions() {
        if (mc.screen != null) {
            dragging = false;
            resizing = false;
            return;
        }

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();
        boolean left = Mouse.isButtonDown(0);

        boolean inPanel = inside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
        boolean inResize = inside(mouseX, mouseY, panelX + panelWidth - 10, panelY + panelHeight - 10, 10, 10);

        boolean shift = SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT);
        boolean ctrl = SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL);
        boolean canDrag = freeEditMode || shift;
        boolean canResize = freeEditMode || ctrl;

        if (left) {
            if (!dragging && !resizing) {
                if (canResize && inResize) {
                    resizing = true;
                    resizeStartX = mouseX;
                    resizeStartY = mouseY;
                    resizeStartW = panelWidth;
                    resizeStartH = panelHeight;
                } else if (canDrag && inPanel) {
                    dragging = true;
                    dragOffsetX = mouseX - panelX;
                    dragOffsetY = mouseY - panelY;
                }
            }

            if (dragging) {
                panelX = Math.max(0, mouseX - dragOffsetX);
                panelY = Math.max(0, mouseY - dragOffsetY);
            }
            if (resizing) {
                panelWidth = Math.max(MIN_W, resizeStartW + (mouseX - resizeStartX));
                panelHeight = Math.max(MIN_H, resizeStartH + (mouseY - resizeStartY));
            }
        } else {
            if (dragging || resizing) {
                saveConfig();
            }
            dragging = false;
            resizing = false;
        }
    }

    private static void renderPanel() {
        int x = panelX;
        int y = panelY;
        int w = panelWidth;
        int h = panelHeight;

        GuiGraphics graphics = GuiCompatContext.current();
        if (graphics == null) {
            return;
        }
        int bg = ((panelAlpha & 0xFF) << 24) | 0x101820;
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.fill(x, y, x + w, y + HEADER_H, 0xCC1E4A69);
        graphics.fill(x, y, x + w, y + 1, 0xFF80D4FF);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF304050);
        graphics.fill(x, y, x + 1, y + h, 0xFF304050);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF304050);

        drawShadow(graphics, I18n.format("gui.kill_timer.panel_title"), x + 6, y + 5, 0xFFFFFF);
        drawShadow(graphics, I18n.format("gui.kill_timer.target_count", tracked.size()), x + w - 68, y + 5, 0xFFFFFF);

        List<TrackInfo> rows = new ArrayList<>(tracked.values());
        rows.sort(buildSortComparator(System.currentTimeMillis()));

        int collapsedCount = 0;
        if (collapseExtraTargets && rows.size() > maxVisibleTargets) {
            collapsedCount = rows.size() - maxVisibleTargets;
            rows = new ArrayList<>(rows.subList(0, maxVisibleTargets));
        }

        int listX = x + PADDING;
        int listY = y + HEADER_H + 4;
        int listW = w - PADDING * 2 - 6;
        int listH = h - HEADER_H - FOOTER_H - 6;

        int totalRowsHeight = rows.size() * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, totalRowsHeight - listH);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();

        if (Mouse.isButtonDown(0) && mc.screen == null) {
            int localY = mouseY - listY + scrollOffset;
            if (inside(mouseX, mouseY, listX, listY, listW, listH)) {
                int idx = localY / (CARD_H + CARD_GAP);
                if (idx >= 0 && idx < rows.size()) {
                    selectedIndex = idx;
                }
            }
        }

        int drawY = listY - scrollOffset;
        for (int i = 0; i < rows.size(); i++) {
            TrackInfo track = rows.get(i);
            int cardY = drawY + i * (CARD_H + CARD_GAP);
            if (cardY + CARD_H < listY || cardY > listY + listH) {
                continue;
            }

            boolean selected = i == selectedIndex;
            int cardBg = selected ? 0xAA2E5F87 : 0x99304050;
            graphics.fill(listX, cardY, listX + listW, cardY + CARD_H, cardBg);

            long calcNow = track.dead ? track.deathAtMs : System.currentTimeMillis();
            double sec = track.getActiveSeconds(calcNow);
            double totalDmg = Math.max(0.0, track.maxHealth - track.minHealth);
            double dpsLive = totalDmg / Math.max(0.001, sec);

            int leftWidth = Math.max(66, listW - 136);
            int statsX = listX + leftWidth + 4;
            int statsW = Math.max(96, listW - leftWidth - 8);
            int topMetricWidth = Math.max(42, (statsW - 8) / 2);
            int topSecondaryX = statsX + topMetricWidth + 8;
            int timeWidth = Math.max(44, Math.min(54, statsW / 3));
            int statusX = statsX + timeWidth + 6;
            int statusWidth = Math.max(36, statsW - timeWidth - 6);

            String name = fitText("§e" + track.name, leftWidth);
            String hpLine = fitText(String.format("§fHP: §c%s§7/§a%s",
                    formatCompactNumber(track.currentHealth), formatCompactNumber(track.maxHealth)), leftWidth);

            String statusText;
            if (track.dead) {
                long remainMs = Math.max(0L, track.deathFreezeUntilMs - System.currentTimeMillis());
                statusText = I18n.format("gui.kill_timer.status_killed", formatSecondsOneDecimal(remainMs / 1000.0));
            } else if (track.paused) {
                long removeAtMs = Math.max(0L, track.disengageDeadlineMs - System.currentTimeMillis());
                statusText = I18n.format("gui.kill_timer.status_disengaged",
                        formatSecondsOneDecimal(removeAtMs / 1000.0));
                if (showDisengageReason && track.disengageReason != null && !track.disengageReason.isEmpty()) {
                    statusText = statusText + "·" + getDisengageReasonDisplay(track.disengageReason);
                }
            } else {
                statusText = I18n.format("gui.kill_timer.status_fighting");
            }

            drawShadow(graphics, name, listX + 4, cardY + 2, 0xFFFFFF);
            drawShadow(graphics, hpLine, listX + 4, cardY + 11, 0xFFFFFF);
            drawMetricField(graphics, "§fDPS:", "§d" + formatCompactNumber(dpsLive), statsX, cardY + 2, topMetricWidth);
            drawMetricField(graphics, "§7固定:", "§b" + formatCompactNumber(track.stableDps), topSecondaryX, cardY + 2,
                    topMetricWidth);
            drawMetricField(graphics, "§f时长:", "§b" + formatSecondsOneDecimal(sec) + "s", statsX, cardY + 11,
                    timeWidth);
            drawStatusField(graphics, statusText, statusX, cardY + 11, statusWidth);
        }

        if (maxScroll > 0) {
            int scrollBarX = x + w - 5;
            int scrollBarY = listY;
            int scrollBarH = listH;
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarH, 0xFF1A1A1A);
            int thumbH = Math.max(12, (int) ((float) listH / Math.max(listH, totalRowsHeight) * scrollBarH));
            int thumbY = scrollBarY + (int) ((float) scrollOffset / maxScroll * (scrollBarH - thumbH));
            graphics.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbH, 0xFF8AAED0);
        }

        if (selectedIndex >= 0 && selectedIndex < rows.size()) {
            int buttonX = x + w - 64;
            int buttonY = y + h - FOOTER_H + 1;
            graphics.fill(buttonX, buttonY, buttonX + 56, buttonY + 14, 0xAA882222);
            drawShadow(graphics, I18n.format("gui.kill_timer.delete"), buttonX + 18, buttonY + 3, 0xFFFFFF);

            if (Mouse.isButtonDown(0) && inside(mouseX, mouseY, buttonX, buttonY, 56, 14)) {
                TrackInfo target = rows.get(selectedIndex);
                tracked.remove(target.id);
                selectedIndex = -1;
            }
        }

        if (collapsedCount > 0) {
            drawShadow(graphics, I18n.format("gui.kill_timer.collapsed_more", collapsedCount), x + 6,
                    y + h - FOOTER_H + 4, 0xFFB8C7D9);
        }

        graphics.fill(x + w - 8, y + h - 8, x + w - 2, y + h - 2, 0xCC66CCFF);
    }

    private static void drawShadow(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(mc.font, text == null ? "" : text, x, y, Gui.withDefaultTextAlpha(color), true);
    }

    private static void drawMetricField(GuiGraphics graphics, String key, String value, int x, int y, int width) {
        String safeKey = key == null ? "" : key;
        String safeValue = value == null ? "" : value;
        int labelWidth = mc.font.width(safeKey);
        int valueWidth = mc.font.width(safeValue);
        int valueX = x + Math.max(labelWidth + 4, width - valueWidth);
        drawShadow(graphics, safeKey, x, y, 0xFFFFFF);
        drawShadow(graphics, safeValue, valueX, y, 0xFFFFFF);
    }

    private static void drawStatusField(GuiGraphics graphics, String statusText, int x, int y, int width) {
        String value = statusText == null ? "" : statusText;
        drawShadow(graphics, value, x, y, 0xFFFFFF);
    }

    private static String fitText(String text, int maxWidth) {
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }
        String raw = text;
        while (raw.length() > 2 && mc.font.width(raw + "§7…") > maxWidth) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw + "§7…";
    }

    private static int getScaledMouseX() {
        ScaledResolution resolution = new ScaledResolution(mc);
        return (int) Math.round(Mouse.getX() * resolution.getScaledWidth()
                / (double) Math.max(1, mc.getWindow().getScreenWidth()));
    }

    private static int getScaledMouseY() {
        ScaledResolution resolution = new ScaledResolution(mc);
        return (int) Math.round(Mouse.getY() * resolution.getScaledHeight()
                / (double) Math.max(1, mc.getWindow().getScreenHeight()));
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static boolean shouldRemoveTrack(long now, UUID id, TrackInfo track, Set<UUID> seenEntityIds) {
        if (track == null) {
            return true;
        }
        if (track.dead) {
            return now > track.deathFreezeUntilMs;
        }

        if (!seenEntityIds.contains(id)) {
            track.withinRangeLastTick = false;
            enterDisengaged(track, now, "lost");
        }

        if (track.withinRangeLastTick) {
            long disengageStartMs = track.lastDamageLikeMs + combatTimeoutSeconds * 1000L;
            if (!track.paused && now >= disengageStartMs) {
                enterDisengaged(track, disengageStartMs, "timeout");
            }
        }

        return track.paused && now >= track.disengageDeadlineMs;
    }

    private static boolean isWithinTrackingRange(LivingEntity living) {
        if (living == null || mc.player == null) {
            return false;
        }
        double range = Math.max(1.0D, trackingRangeBlocks);
        return mc.player.distanceToSqr(living) <= range * range;
    }

    private static String formatSecondsOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", Math.max(0.0D, value));
    }

    private static boolean matchesConfiguredEntityType(LivingEntity entity) {
        if (entity == null || entity == mc.player || entity instanceof ArmorStand) {
            return false;
        }
        if (targetPlayers && entity instanceof Player) {
            return true;
        }
        if (targetBoss && isBoss(entity)) {
            return true;
        }
        if (targetHostile && isHostile(entity)) {
            return true;
        }
        if (targetPassive && isPassive(entity)) {
            return true;
        }
        if (targetWater && entity instanceof WaterAnimal) {
            return true;
        }
        if (targetAmbient && entity instanceof AmbientCreature) {
            return true;
        }
        if (targetVillager && entity instanceof AbstractVillager) {
            return true;
        }
        if (targetGolem && isGolem(entity)) {
            return true;
        }
        if (targetTameable && entity instanceof TamableAnimal) {
            return true;
        }
        return targetNeutral && isNeutral(entity);
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean isPassive(LivingEntity entity) {
        return entity instanceof Animal || entity instanceof AbstractHorse;
    }

    private static boolean isGolem(LivingEntity entity) {
        return entity instanceof AbstractGolem
                || entity.getType().toString().toLowerCase(Locale.ROOT).contains("golem");
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity.getMaxHealth() >= 80.0F;
    }

    private static boolean isNeutral(LivingEntity entity) {
        return !(entity instanceof Player)
                && !isHostile(entity)
                && !isPassive(entity)
                && !(entity instanceof WaterAnimal)
                && !(entity instanceof AmbientCreature)
                && !(entity instanceof AbstractVillager)
                && !isGolem(entity)
                && !(entity instanceof TamableAnimal)
                && !isBoss(entity);
    }

    public static void ensureAtLeastOneTargetTypeEnabled() {
        if (!targetHostile && !targetNeutral && !targetPassive && !targetPlayers
                && !targetWater && !targetAmbient && !targetVillager
                && !targetGolem && !targetTameable && !targetBoss) {
            targetHostile = true;
        }
    }

    private static void enterDisengaged(TrackInfo track, long now, String reason) {
        if (track == null || track.dead) {
            return;
        }
        if (track.paused) {
            if ((track.disengageReason == null || track.disengageReason.isEmpty()) && reason != null) {
                track.disengageReason = reason;
            }
            return;
        }
        track.paused = true;
        track.pausedStartMs = now;
        track.disengageReason = reason == null ? "" : reason;
        track.disengageDeadlineMs = now + Math.max(1, disengageRemoveSeconds) * 1000L;
    }

    private static void resumeTrack(TrackInfo track, long now) {
        if (track == null) {
            return;
        }
        if (track.paused) {
            track.pausedAccumulatedMs += Math.max(0L, now - track.pausedStartMs);
            track.pausedStartMs = 0L;
            track.paused = false;
        }
        track.disengageReason = "";
        track.disengageDeadlineMs = 0L;
    }

    private static boolean wasRecentlyAttackedByPlayer(UUID id, long now) {
        if (id == null) {
            return false;
        }
        Long last = recentPlayerAttackMs.get(id);
        return last != null && now - last.longValue() <= OWN_DAMAGE_GRACE_MS;
    }

    private static Comparator<TrackInfo> buildSortComparator(long now) {
        String normalized = normalizeSortMode(sortMode);
        if (SORT_DPS.equals(normalized)) {
            return Comparator.comparingDouble((TrackInfo track) -> currentDps(track, now)).reversed()
                    .thenComparingLong(track -> track.combatStartMs);
        }
        if (SORT_HEALTH.equals(normalized)) {
            return Comparator.comparingDouble((TrackInfo track) -> healthRatio(track))
                    .thenComparingLong(track -> track.combatStartMs);
        }
        if (SORT_DURATION.equals(normalized)) {
            return Comparator.comparingDouble((TrackInfo track) -> track.getActiveSeconds(now)).reversed()
                    .thenComparingLong(track -> track.combatStartMs);
        }
        return Comparator.comparingLong((TrackInfo track) -> effectiveRecentTime(track)).reversed()
                .thenComparingLong(track -> track.combatStartMs);
    }

    private static double currentDps(TrackInfo track, long now) {
        if (track == null) {
            return 0.0D;
        }
        double sec = track.getActiveSeconds(track.dead ? track.deathAtMs : now);
        double totalDmg = Math.max(0.0D, track.maxHealth - track.minHealth);
        return totalDmg / Math.max(0.001D, sec);
    }

    private static double healthRatio(TrackInfo track) {
        if (track == null || track.maxHealth <= 0.0F) {
            return 1.0D;
        }
        return Math.max(0.0D, track.currentHealth / Math.max(1.0F, track.maxHealth));
    }

    private static long effectiveRecentTime(TrackInfo track) {
        if (track == null) {
            return 0L;
        }
        if (track.dead) {
            return track.deathAtMs;
        }
        return Math.max(track.lastDamageLikeMs, track.combatStartMs);
    }

    private static String normalizeSortMode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (SORT_DPS.equals(normalized) || SORT_HEALTH.equals(normalized) || SORT_DURATION.equals(normalized)) {
            return normalized;
        }
        return SORT_RECENT;
    }

    private static String getDisengageReasonDisplay(String reason) {
        if ("range".equalsIgnoreCase(reason)) {
            return I18n.format("gui.kill_timer.reason.range");
        }
        if ("lost".equalsIgnoreCase(reason)) {
            return I18n.format("gui.kill_timer.reason.lost");
        }
        return I18n.format("gui.kill_timer.reason.timeout");
    }

    private static String formatCompactNumber(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000_000D) {
            return trimTrailingZeros(value / 1_000_000_000_000D) + "T";
        }
        if (abs >= 1_000_000_000D) {
            return trimTrailingZeros(value / 1_000_000_000D) + "B";
        }
        if (abs >= 1_000_000D) {
            return trimTrailingZeros(value / 1_000_000D) + "M";
        }
        if (abs >= 1_000D) {
            return trimTrailingZeros(value / 1_000D) + "K";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return trimTrailingZeros(value);
    }

    private static String trimTrailingZeros(double value) {
        String text = String.format(Locale.US, "%.2f", value);
        if (text.indexOf('.') >= 0) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text;
    }
}
