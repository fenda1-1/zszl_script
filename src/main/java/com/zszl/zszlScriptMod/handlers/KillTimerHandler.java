package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.ScaledResolution;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            deathDataMode = data.deathDataMode == DEATH_MODE_PANEL_HOLD ? DEATH_MODE_PANEL_HOLD : DEATH_MODE_CHAT;
            deathPanelHoldSeconds = Math.max(1, data.deathPanelHoldSeconds);
        } catch (Exception ignored) {
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("kill_timer_config.json");
        try {
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
            mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.kill_timer.toggle",
                    isEnabled ? I18n.format("msg.kill_timer.state_on") : I18n.format("msg.kill_timer.state_off"))));
        }
        saveConfig();
    }

    public static void clearRuntimeState() {
        tracked.clear();
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
            mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.kill_timer.exit_free_edit")));
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
                mc.player.sendSystemMessage(new TextComponentString(I18n.format("msg.kill_timer.kill_summary",
                        info.name, sec, formatCompactNumber(totalDmg), formatCompactNumber(dps))));
            }
        }
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

        double delta = event.getScrollDelta();
        if (Math.abs(delta) < 0.0001D) {
            return;
        }

        scrollOffset = MathHelper.clamp(scrollOffset - (int) Math.round(delta) * (CARD_H + CARD_GAP), 0, maxScroll);
        event.setCanceled(true);
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
        for (Entity entity : mc.level.entitiesForRendering()) {
            entities.add(entity);
        }

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!(living instanceof Enemy)) {
                continue;
            }
            UUID id = living.getUUID();

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
                if (hp < maxHp) {
                    tracked.put(id, new TrackInfo(living));
                }
                continue;
            }

            track.dead = false;
            track.deathAtMs = 0L;
            track.deathFreezeUntilMs = 0L;
            track.name = living.getName().getString();
            track.maxHealth = Math.max(track.maxHealth, maxHp);

            float previousHp = track.currentHealth;
            boolean tookDamageNow = hp < previousHp - 0.0001F;

            track.currentHealth = hp;
            track.minHealth = Math.min(track.minHealth, hp);

            if (tookDamageNow) {
                track.lastDamageLikeMs = now;
                if (track.paused) {
                    track.pausedAccumulatedMs += Math.max(0L, now - track.pausedStartMs);
                    track.pausedStartMs = 0L;
                    track.paused = false;
                }
            } else {
                if (!track.paused && now - track.lastDamageLikeMs >= combatTimeoutSeconds * 1000L) {
                    track.paused = true;
                    track.pausedStartMs = now;
                }
                if (now - track.lastDamageLikeMs >= (combatTimeoutSeconds + disengageRemoveSeconds) * 1000L) {
                    tracked.remove(id);
                }
            }
        }

        tracked.entrySet().removeIf(entry -> {
            TrackInfo track = entry.getValue();
            return track != null && track.dead && now > track.deathFreezeUntilMs;
        });

        for (TrackInfo track : tracked.values()) {
            if (track.paused || track.dead) {
                continue;
            }
            if (now - track.lastStableDpsUpdateMs >= stableDpsIntervalSeconds * 1000L) {
                double sec = track.getActiveSeconds(now);
                double totalDmg = Math.max(0.0, track.maxHealth - track.currentHealth);
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

        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
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
        rows.sort(Comparator.comparingLong(a -> a.combatStartMs));

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
            double totalDmg = Math.max(0.0, track.maxHealth - track.currentHealth);
            double dpsLive = totalDmg / Math.max(0.001, sec);

            String name = fitText("§e" + track.name, listW - 10);
            String hpLine = String.format("§fHP: §c%s§7/§a%s",
                    formatCompactNumber(track.currentHealth), formatCompactNumber(track.maxHealth));
            String dpsLine = String.format("§fDPS: §d%s §7| 固定: §b%s",
                    formatCompactNumber(dpsLive), formatCompactNumber(track.stableDps));

            String statusText;
            if (track.dead) {
                long remainMs = Math.max(0L, track.deathFreezeUntilMs - System.currentTimeMillis());
                statusText = I18n.format("gui.kill_timer.status_killed", trimTrailingZeros(remainMs / 1000.0));
            } else if (track.paused) {
                long sinceLastDamageMs = Math.max(0L, System.currentTimeMillis() - track.lastDamageLikeMs);
                long removeAtMs = Math.max(0L,
                        (combatTimeoutSeconds + disengageRemoveSeconds) * 1000L - sinceLastDamageMs);
                statusText = I18n.format("gui.kill_timer.status_disengaged",
                        trimTrailingZeros(removeAtMs / 1000.0));
            } else {
                statusText = I18n.format("gui.kill_timer.status_fighting");
            }

            String timeLine = I18n.format("gui.kill_timer.time_status", sec, statusText);
            hpLine = fitText(hpLine, listW - 10);
            dpsLine = fitText(dpsLine, listW - 10);
            timeLine = fitText(timeLine, listW - 10);

            drawShadow(graphics, name, listX + 4, cardY + 2, 0xFFFFFF);
            drawShadow(graphics, hpLine, listX + 4, cardY + 11, 0xFFFFFF);
            drawShadow(graphics, dpsLine, listX + listW / 2, cardY + 2, 0xFFFFFF);
            drawShadow(graphics, timeLine, listX + listW / 2, cardY + 11, 0xFFFFFF);
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

        graphics.fill(x + w - 8, y + h - 8, x + w - 2, y + h - 2, 0xCC66CCFF);
        graphics.flush();
    }

    private static void drawShadow(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(mc.font, text == null ? "" : text, x, y, color, true);
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
