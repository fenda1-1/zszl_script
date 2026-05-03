package com.zszl.zszlScriptMod;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.client.event.GuiOpenEvent;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.common.gameevent.InputEvent;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.handlers.GuiBlockerHandler;
import com.zszl.zszlScriptMod.listenersupport.PlayerIdleTriggerTracker;
import com.zszl.zszlScriptMod.path.node.NodeTriggerManager;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import com.zszl.zszlScriptMod.path.trigger.PlayerListTriggerSupport;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiElementInspector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public final class GlobalEventListener {
    public static final GlobalEventListener instance = new GlobalEventListener();
    public static int timedMessageTickCounter = 0;

    private static final double ENTITY_NEARBY_TRIGGER_RADIUS = 8.0D;
    private static final int SCOREBOARD_DISPLAY_SLOT_SIDEBAR = 1;
    private static final Random RANDOM = new Random();

    private final PlayerIdleTriggerTracker playerIdleTriggerTracker = new PlayerIdleTriggerTracker();
    private final Set<Integer> activeTriggerKeys = new HashSet<>();

    private int clientTickCounter = 0;
    private int timedMessageIndex = 0;
    private boolean wasPlayerDeadLastTick = false;
    private boolean wasInventoryFullLastCheck = false;
    private String lastInventorySignature = "";
    private String lastAreaKey = "";
    private String lastWorldKey = "";
    private String lastScoreboardSignature = "";
    private String lastNearbyEntitySignature = "";
    private PlayerListTriggerSupport.PlayerSnapshot lastPlayerListSnapshot = new PlayerListTriggerSupport.PlayerSnapshot(
            Collections.emptyList(), "");
    private Screen lastGuiScreen = null;
    private String lastGuiClassName = "";
    private String lastGuiTitle = "";

    private GlobalEventListener() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            resetRuntimeState();
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            handleClientTickStart(mc);
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            handleClientTickEnd(mc);
        }
    }

    private void handleClientTickStart(Minecraft mc) {
        clientTickCounter++;
        NodeTriggerManager.tick();

        boolean needsAreaChangedChecks = NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_AREA_CHANGED)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_AREA_CHANGED);
        boolean needsWorldChangedChecks = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_WORLD_CHANGED);
        boolean needsInventoryChangedChecks = NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_INVENTORY_CHANGED)
                || NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_INVENTORY_FULL)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_INVENTORY_CHANGED)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_INVENTORY_FULL);
        boolean needsNearbyEntityChecks = NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_ENTITY_NEARBY)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_ENTITY_NEARBY);
        boolean needsScoreboardChecks = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_SCOREBOARD_CHANGED);
        boolean needsPlayerListChecks = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_PLAYER_LIST);
        boolean needsTimerTriggers = NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_TIMER)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_TIMER);
        boolean needsHpLowTriggers = NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_HP_LOW)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_HP_LOW);
        boolean needsIdleTracking = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_PLAYER_IDLE);

        boolean playerDeadNow = mc.player.isDeadOrDying() || mc.player.getHealth() <= 0.0F;
        if (playerDeadNow && !wasPlayerDeadLastTick) {
            JsonObject deathTrigger = new JsonObject();
            deathTrigger.addProperty("hp", mc.player.getHealth());
            deathTrigger.addProperty("maxHp", mc.player.getMaxHealth());
            deathTrigger.addProperty("x", mc.player.getX());
            deathTrigger.addProperty("y", mc.player.getY());
            deathTrigger.addProperty("z", mc.player.getZ());
            triggerUnifiedEvent(NodeTriggerManager.TRIGGER_DEATH, LegacySequenceTriggerManager.TRIGGER_DEATH, deathTrigger);
        } else if (!playerDeadNow && wasPlayerDeadLastTick) {
            JsonObject respawnTrigger = new JsonObject();
            respawnTrigger.addProperty("hp", mc.player.getHealth());
            respawnTrigger.addProperty("maxHp", mc.player.getMaxHealth());
            respawnTrigger.addProperty("x", mc.player.getX());
            respawnTrigger.addProperty("y", mc.player.getY());
            respawnTrigger.addProperty("z", mc.player.getZ());
            triggerUnifiedEvent(NodeTriggerManager.TRIGGER_RESPAWN, LegacySequenceTriggerManager.TRIGGER_RESPAWN,
                    respawnTrigger);
        }

        if (needsIdleTracking) {
            playerIdleTriggerTracker.update(mc, playerDeadNow, clientTickCounter);
        } else {
            playerIdleTriggerTracker.reset();
        }

        String currentAreaKey = needsAreaChangedChecks ? buildAreaKey(mc) : "";
        String currentWorldKey = needsWorldChangedChecks ? buildWorldKey(mc) : "";
        if (needsAreaChangedChecks && !lastAreaKey.isEmpty() && !currentAreaKey.equals(lastAreaKey)) {
            JsonObject areaTrigger = new JsonObject();
            areaTrigger.addProperty("from", lastAreaKey);
            areaTrigger.addProperty("to", currentAreaKey);
            areaTrigger.addProperty("x", mc.player.getX());
            areaTrigger.addProperty("y", mc.player.getY());
            areaTrigger.addProperty("z", mc.player.getZ());
            areaTrigger.addProperty("chunkX", mc.player.chunkPosition().x);
            areaTrigger.addProperty("chunkZ", mc.player.chunkPosition().z);
            triggerUnifiedEvent(NodeTriggerManager.TRIGGER_AREA_CHANGED,
                    LegacySequenceTriggerManager.TRIGGER_AREA_CHANGED, areaTrigger);
        }

        if (needsWorldChangedChecks && !lastWorldKey.isEmpty() && !currentWorldKey.equals(lastWorldKey)) {
            JsonObject worldTrigger = new JsonObject();
            worldTrigger.addProperty("from", lastWorldKey);
            worldTrigger.addProperty("to", currentWorldKey);
            worldTrigger.addProperty("dimension", currentWorldKey);
            LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_WORLD_CHANGED, worldTrigger);
        }

        if (needsInventoryChangedChecks && clientTickCounter % 4 == 0) {
            String inventorySignature = buildInventorySignature(mc);
            if (!lastInventorySignature.isEmpty() && !inventorySignature.equals(lastInventorySignature)) {
                JsonObject inventoryTrigger = new JsonObject();
                inventoryTrigger.addProperty("before", lastInventorySignature);
                inventoryTrigger.addProperty("after", inventorySignature);
                inventoryTrigger.addProperty("filledSlots", countFilledSlots(mc));
                triggerUnifiedEvent(NodeTriggerManager.TRIGGER_INVENTORY_CHANGED,
                        LegacySequenceTriggerManager.TRIGGER_INVENTORY_CHANGED, inventoryTrigger);
            }
            boolean inventoryFullNow = isMainInventoryFull(mc);
            if (inventoryFullNow && !wasInventoryFullLastCheck) {
                JsonObject inventoryFullTrigger = new JsonObject();
                int totalSlots = getMainInventorySlotCount(mc);
                int filledSlots = countMainInventoryFilledSlots(mc);
                inventoryFullTrigger.addProperty("filledSlots", filledSlots);
                inventoryFullTrigger.addProperty("totalSlots", totalSlots);
                inventoryFullTrigger.addProperty("emptySlots", Math.max(0, totalSlots - filledSlots));
                inventoryFullTrigger.addProperty("signature", inventorySignature);
                triggerUnifiedEvent(NodeTriggerManager.TRIGGER_INVENTORY_FULL,
                        LegacySequenceTriggerManager.TRIGGER_INVENTORY_FULL, inventoryFullTrigger);
            }
            wasInventoryFullLastCheck = inventoryFullNow;
            lastInventorySignature = inventorySignature;
        }

        if (needsNearbyEntityChecks && clientTickCounter % 5 == 0) {
            NearbyEntitySummary nearbyEntitySummary = scanNearbyEntities(mc);
            String nearbySignature = nearbyEntitySummary.signature;
            if (!lastNearbyEntitySignature.isEmpty() && !nearbySignature.equals(lastNearbyEntitySignature)) {
                JsonObject entityTrigger = new JsonObject();
                entityTrigger.addProperty("before", lastNearbyEntitySignature);
                entityTrigger.addProperty("after", nearbySignature);
                entityTrigger.addProperty("count", nearbyEntitySummary.count);
                triggerUnifiedEvent(NodeTriggerManager.TRIGGER_ENTITY_NEARBY,
                        LegacySequenceTriggerManager.TRIGGER_ENTITY_NEARBY, entityTrigger);
            }
            lastNearbyEntitySignature = nearbySignature;
        }

        if (needsScoreboardChecks && clientTickCounter % 10 == 0) {
            String scoreboardSignature = buildScoreboardSignature(mc);
            if (!lastScoreboardSignature.isEmpty() && !scoreboardSignature.equals(lastScoreboardSignature)) {
                JsonObject scoreboardTrigger = new JsonObject();
                scoreboardTrigger.addProperty("before", lastScoreboardSignature);
                scoreboardTrigger.addProperty("after", scoreboardSignature);
                scoreboardTrigger.addProperty("text", scoreboardSignature);
                LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_SCOREBOARD_CHANGED,
                        scoreboardTrigger);
            }
            lastScoreboardSignature = scoreboardSignature;
        }
        if (needsPlayerListChecks) {
            PlayerListTriggerSupport.PlayerSnapshot playerListSnapshot = PlayerListTriggerSupport.captureSnapshot(mc);
            if (!playerListSnapshot.players.isEmpty()) {
                LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_PLAYER_LIST,
                        PlayerListTriggerSupport.buildTriggerEvent(lastPlayerListSnapshot, playerListSnapshot));
            }
            lastPlayerListSnapshot = playerListSnapshot;
        } else {
            lastPlayerListSnapshot = new PlayerListTriggerSupport.PlayerSnapshot(Collections.emptyList(), "");
        }

        if (needsTimerTriggers && clientTickCounter % 20 == 0) {
            JsonObject timerTrigger = new JsonObject();
            timerTrigger.addProperty("tick", clientTickCounter);
            triggerUnifiedEvent(NodeTriggerManager.TRIGGER_TIMER, LegacySequenceTriggerManager.TRIGGER_TIMER,
                    timerTrigger);
        }

        if (needsHpLowTriggers && mc.player.getHealth() > 0.0F) {
            JsonObject hpTrigger = new JsonObject();
            hpTrigger.addProperty("hp", mc.player.getHealth());
            hpTrigger.addProperty("maxHp", mc.player.getMaxHealth());
            triggerUnifiedEvent(NodeTriggerManager.TRIGGER_HP_LOW, LegacySequenceTriggerManager.TRIGGER_HP_LOW,
                    hpTrigger);
        }

        wasPlayerDeadLastTick = playerDeadNow;
        if (needsAreaChangedChecks) {
            lastAreaKey = currentAreaKey;
        }
        if (needsWorldChangedChecks) {
            lastWorldKey = currentWorldKey;
        }
    }

    private void handleClientTickEnd(Minecraft mc) {
        if (ModConfig.isMouseDetached && mc.mouseHandler != null && mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }

        updateGuiTriggers(mc);
        tickTimedMessages(mc);
    }

    private void updateGuiTriggers(Minecraft mc) {
        Screen currentScreen = mc.screen;
        if (currentScreen != lastGuiScreen) {
            if (lastGuiScreen != null) {
                JsonObject closeTrigger = new JsonObject();
                closeTrigger.addProperty("gui", safe(lastGuiClassName));
                closeTrigger.addProperty("title", safe(lastGuiTitle));
                LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_GUI_CLOSE, closeTrigger);
            }

            if (currentScreen != null) {
                lastGuiClassName = currentScreen.getClass().getName();
                lastGuiTitle = GuiElementInspector.getCurrentGuiTitle(mc);
                JsonObject openTrigger = new JsonObject();
                openTrigger.addProperty("gui", lastGuiClassName);
                openTrigger.addProperty("title", safe(lastGuiTitle));
                triggerUnifiedEvent(NodeTriggerManager.TRIGGER_GUI_OPEN, LegacySequenceTriggerManager.TRIGGER_GUI_OPEN,
                        openTrigger);
            } else {
                lastGuiClassName = "";
                lastGuiTitle = "";
            }
            lastGuiScreen = currentScreen;
            return;
        }

        if (currentScreen != null) {
            lastGuiClassName = currentScreen.getClass().getName();
            lastGuiTitle = GuiElementInspector.getCurrentGuiTitle(mc);
        }
    }

    private void tickTimedMessages(Minecraft mc) {
        ChatOptimizationConfig config = ChatOptimizationConfig.INSTANCE;
        if (!config.enableTimedMessage || config.timedMessages == null || config.timedMessages.isEmpty()) {
            timedMessageTickCounter = 0;
            return;
        }

        timedMessageTickCounter++;
        if (timedMessageTickCounter < Math.max(1, config.timedMessageIntervalSeconds) * 20) {
            return;
        }

        List<String> validMessages = new ArrayList<>();
        for (String message : config.timedMessages) {
            if (message != null && !message.trim().isEmpty()) {
                validMessages.add(message);
            }
        }

        if (!validMessages.isEmpty()) {
            String messageToSend;
            if (config.timedMessageMode == ChatOptimizationConfig.TimedMessageMode.SEQUENTIAL) {
                if (timedMessageIndex >= validMessages.size()) {
                    timedMessageIndex = 0;
                }
                messageToSend = validMessages.get(timedMessageIndex);
                timedMessageIndex++;
            } else {
                messageToSend = validMessages.get(RANDOM.nextInt(validMessages.size()));
            }

            if (messageToSend != null && !messageToSend.trim().isEmpty()) {
                mc.player.connection.sendChat(messageToSend);
            }
        }

        timedMessageTickCounter = 0;
    }

    @SubscribeEvent
    public void onLegacyKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            activeTriggerKeys.clear();
            return;
        }

        int keyCode = Keyboard.getEventKey();
        boolean pressed = Keyboard.getEventKeyState();
        if (keyCode == Keyboard.KEY_NONE) {
            return;
        }

        if (!pressed) {
            activeTriggerKeys.remove(keyCode);
            return;
        }

        if (!activeTriggerKeys.add(keyCode)) {
            return;
        }

        if (!LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_KEY_INPUT)) {
            return;
        }

        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("keyCode", keyCode);
        triggerData.addProperty("keyName", Keyboard.getKeyName(keyCode));
        triggerData.addProperty("screen", mc.screen == null ? "" : mc.screen.getClass().getSimpleName());
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_KEY_INPUT, triggerData);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event != null && event.getGui() != null && GuiBlockerHandler.shouldBlockAndConsume(event.getGui())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onNetworkLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        JsonObject triggerData = buildServerTriggerData(Minecraft.getInstance());
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_SERVER_CONNECT, triggerData);
    }

    @SubscribeEvent
    public void onNetworkLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        JsonObject triggerData = buildServerTriggerData(Minecraft.getInstance());
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_SERVER_DISCONNECT, triggerData);
        resetRuntimeState();
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || event == null || event.getEntity() != mc.player
                || !event.getEntity().level().isClientSide()) {
            return;
        }
        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("damage", event.getAmount());
        triggerData.addProperty("damageSource", event.getSource() == null ? "" : safe(event.getSource().toString()));
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_PLAYER_HURT, triggerData);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || event == null || event.getEntity() != mc.player) {
            return;
        }
        Entity target = event.getTarget();
        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("entityName", target == null ? "" : safe(target.getName().getString()));
        triggerData.addProperty("entityClass", target == null ? "" : safe(target.getClass().getName()));
        triggerData.addProperty("entityType", target == null ? ""
                : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())));
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_ATTACK_ENTITY, triggerData);
    }

    @SubscribeEvent
    public void onTargetKilled(LivingDeathEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || event == null || event.getSource() == null
                || !event.getEntity().level().isClientSide()) {
            return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity != mc.player) {
            return;
        }
        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("entityName", event.getEntity() == null ? "" : safe(event.getEntity().getName().getString()));
        triggerData.addProperty("entityClass",
                event.getEntity() == null ? "" : safe(event.getEntity().getClass().getName()));
        triggerData.addProperty("entityType", event.getEntity() == null ? ""
                : String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType())));
        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_TARGET_KILL, triggerData);
    }

    private void resetRuntimeState() {
        timedMessageTickCounter = 0;
        timedMessageIndex = 0;
        clientTickCounter = 0;
        playerIdleTriggerTracker.reset();
        activeTriggerKeys.clear();
        wasPlayerDeadLastTick = false;
        wasInventoryFullLastCheck = false;
        lastInventorySignature = "";
        lastAreaKey = "";
        lastWorldKey = "";
        lastScoreboardSignature = "";
        lastNearbyEntitySignature = "";
        lastPlayerListSnapshot = new PlayerListTriggerSupport.PlayerSnapshot(Collections.emptyList(), "");
        lastGuiScreen = null;
        lastGuiClassName = "";
        lastGuiTitle = "";
    }

    private JsonObject buildServerTriggerData(Minecraft mc) {
        JsonObject triggerData = new JsonObject();
        if (mc == null) {
            return triggerData;
        }
        ServerData server = mc.getCurrentServer();
        if (server != null) {
            triggerData.addProperty("serverName", safe(server.name));
            triggerData.addProperty("serverIp", safe(server.ip));
        }
        triggerData.addProperty("singleplayer", mc.hasSingleplayerServer());
        if (mc.player != null) {
            triggerData.addProperty("playerName", safe(mc.player.getName().getString()));
        }
        return triggerData;
    }

    private void triggerUnifiedEvent(String nodeTriggerType, String legacyTriggerType, JsonObject eventData) {
        if (eventData == null) {
            return;
        }
        if (nodeTriggerType != null && !nodeTriggerType.trim().isEmpty()
                && NodeTriggerManager.hasGraphsForTrigger(nodeTriggerType)) {
            NodeTriggerManager.trigger(nodeTriggerType, eventData);
        }
        if (legacyTriggerType != null && !legacyTriggerType.trim().isEmpty()
                && LegacySequenceTriggerManager.hasRulesForTrigger(legacyTriggerType)) {
            LegacySequenceTriggerManager.triggerEvent(legacyTriggerType, eventData);
        }
    }

    private String buildAreaKey(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            return "";
        }
        return buildWorldKey(mc) + ":" + mc.player.chunkPosition().x + "," + mc.player.chunkPosition().z;
    }

    private String buildWorldKey(Minecraft mc) {
        if (mc == null || mc.level == null) {
            return "";
        }
        ResourceLocation dimensionId = mc.level.dimension().location();
        return dimensionId == null ? "" : dimensionId.toString();
    }

    private String buildInventorySignature(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return "";
        }
        Inventory inventory = mc.player.getInventory();
        StringBuilder builder = new StringBuilder();
        appendInventorySection(builder, "main", inventory.items);
        appendInventorySection(builder, "armor", inventory.armor);
        appendInventorySection(builder, "offhand", inventory.offhand);
        return builder.toString();
    }

    private void appendInventorySection(StringBuilder builder, String prefix, List<ItemStack> stacks) {
        if (builder == null || stacks == null) {
            return;
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            builder.append(prefix)
                    .append('[')
                    .append(i)
                    .append("]=")
                    .append(itemId == null ? safe(stack.getHoverName().getString()) : itemId.toString())
                    .append('x')
                    .append(stack.getCount());
        }
    }

    private String buildScoreboardSignature(Minecraft mc) {
        if (mc == null || mc.level == null) {
            return "";
        }
        try {
            Scoreboard scoreboard = mc.level.getScoreboard();
            if (scoreboard == null) {
                return "";
            }
            Objective objective = scoreboard.getDisplayObjective(SCOREBOARD_DISPLAY_SLOT_SIDEBAR);
            if (objective == null) {
                return "";
            }

            StringBuilder builder = new StringBuilder();
            builder.append(stripFormatting(objective.getDisplayName().getString()));
            Collection<Score> scores = scoreboard.getPlayerScores(objective);
            List<Score> sorted = new ArrayList<>(scores);
            sorted.sort(Comparator.comparingInt(Score::getScore).reversed().thenComparing(Score::getOwner));

            int count = 0;
            for (Score score : sorted) {
                if (score == null || score.getOwner() == null || score.getOwner().startsWith("#")) {
                    continue;
                }
                PlayerTeam team = scoreboard.getPlayersTeam(score.getOwner());
                String line = PlayerTeam.formatNameForTeam(team, Component.literal(score.getOwner())).getString();
                if (builder.length() > 0) {
                    builder.append(" | ");
                }
                builder.append(stripFormatting(line));
                count++;
                if (count >= 15) {
                    break;
                }
            }
            return builder.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private int countFilledSlots(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return 0;
        }
        int count = 0;
        Inventory inventory = mc.player.getInventory();
        for (ItemStack stack : inventory.items) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        for (ItemStack stack : inventory.armor) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int countMainInventoryFilledSlots(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int getMainInventorySlotCount(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return 0;
        }
        return mc.player.getInventory().items.size();
    }

    private boolean isMainInventoryFull(Minecraft mc) {
        int totalSlots = getMainInventorySlotCount(mc);
        return totalSlots > 0 && countMainInventoryFilledSlots(mc) >= totalSlots;
    }

    private NearbyEntitySummary scanNearbyEntities(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            return NearbyEntitySummary.EMPTY;
        }
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        int count = 0;
        AABB box = mc.player.getBoundingBox().inflate(ENTITY_NEARBY_TRIGGER_RADIUS);
        for (LivingEntity living : mc.level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != mc.player && entity.isAlive())) {
            count++;
            String name = normalizeEntityName(living.getName().getString());
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return new NearbyEntitySummary(String.join(", ", names), count);
    }

    private String normalizeEntityName(String name) {
        String trimmed = safe(name).trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(trimmed.length());
        boolean previousWhitespace = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            boolean whitespace = Character.isWhitespace(ch) || Character.isSpaceChar(ch) || ch == '\u3000';
            if (whitespace) {
                if (!previousWhitespace && normalized.length() > 0) {
                    normalized.append(' ');
                }
                previousWhitespace = true;
            } else {
                normalized.append(ch);
                previousWhitespace = false;
            }
        }
        int length = normalized.length();
        if (length > 0 && normalized.charAt(length - 1) == ' ') {
            normalized.setLength(length - 1);
        }
        return normalized.toString();
    }

    private String stripFormatting(String value) {
        String stripped = ChatFormatting.stripFormatting(safe(value));
        return stripped == null ? safe(value) : stripped.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class NearbyEntitySummary {
        private static final NearbyEntitySummary EMPTY = new NearbyEntitySummary("", 0);

        private final String signature;
        private final int count;

        private NearbyEntitySummary(String signature, int count) {
            this.signature = signature;
            this.count = count;
        }
    }
}
