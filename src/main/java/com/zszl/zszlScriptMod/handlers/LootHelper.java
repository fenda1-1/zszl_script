package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.zszlScriptMod;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LootHelper {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SLOT_CLICK_TEMPLATE = "00 04 56 69 65 77 00 00 {session_id} {view_id} 00 0C 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 00 00 00 12 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 5F 63 6C 69 63 6B 00 00 00 {slot_index_hex} 00 00 00 00";
    private static final String BUTTON_CLICK_TEMPLATE = "00 09 43 6F 6D 70 6F 6E 65 6E 74 00 00 {session_id} {component_id} 00 0C 42 75 74 74 6F 6E 5F 63 6C 69 63 6B 01 00 00 00 00 00 00 00 00 00 00 00 00 00";
    private static final String TASK_TAG_PREFIX = "LOOT_ASSIGN_TASK";
    private static final String AUTO_START_TASK_TAG = TASK_TAG_PREFIX + ":auto_start";
    private static final String PLAYER_CLICK_TASK_TAG = TASK_TAG_PREFIX + ":player_click";
    private static final String PLAYER_TIMEOUT_TASK_TAG = TASK_TAG_PREFIX + ":player_timeout";
    private static final String ASSIGN_RESULT_TIMEOUT_TASK_TAG = TASK_TAG_PREFIX + ":assign_result_timeout";
    private static final String ASSIGN_RESULT_RETRY_TASK_TAG = TASK_TAG_PREFIX + ":assign_result_retry";
    private static final String NEXT_SLOT_TASK_TAG = TASK_TAG_PREFIX + ":next_slot";
    private static final String FINISH_BUTTON_CLICK_TASK_TAG = TASK_TAG_PREFIX + ":finish_button_click";
    private static final int PLAYER_RETRY_DELAY_TICKS = 5;
    private static final int PLAYER_BUTTON_TIMEOUT_TICKS = 100;
    private static final int ASSIGN_RESULT_TIMEOUT_TICKS = 120;
    private static final int ASSIGN_RESULT_RETRY_DELAY_TICKS = 12;
    private static final int AUTO_START_DELAY_TICKS = 2;
    private static final int NEXT_SLOT_DELAY_TICKS = 12;
    private static final int DEFAULT_PLAYER_CLICK_DELAY_TICKS = 2;
    private static final int DEFAULT_MAX_ASSIGN_CLICK_ATTEMPTS = 10;
    private static final int DEFAULT_ASSIGN_PLAYER_POSITION = 1;

    public static final LootHelper INSTANCE = new LootHelper();

    public static final int BTN_AUTO_ASSIGN = 29101;

    public enum CompletionAction {
        NONE,
        SUICIDE,
        DISCONNECT
    }

    public static boolean autoRaidEnabled = false;
    public static boolean autoOneKeyAssignEnabled = true;
    public static int playerClickDelayTicks = DEFAULT_PLAYER_CLICK_DELAY_TICKS;
    public static int maxAssignClickAttempts = DEFAULT_MAX_ASSIGN_CLICK_ATTEMPTS;
    public static int assignPlayerPosition = DEFAULT_ASSIGN_PLAYER_POSITION;
    public static CompletionAction completionAction = CompletionAction.NONE;

    public static class PlayerButtonInfo {
        public final int componentId;
        public final String playerName;

        public PlayerButtonInfo(int componentId, String playerName) {
            this.componentId = componentId;
            this.playerName = playerName;
        }
    }

    public volatile boolean isLootTicketValid = false;
    public volatile boolean isLootContextActive = false;
    public volatile boolean isLootMainGuiOpen = false;

    private volatile int lootMainViewId = -1;
    private volatile boolean lootImageDetected = false;

    private final List<Integer> lootSlotIndices = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> assignedSlotIndices = Collections.synchronizedList(new ArrayList<>());
    private final List<PlayerButtonInfo> playerButtons = Collections.synchronizedList(new ArrayList<>());
    private final Map<Integer, PlayerButtonInfo> playerButtonMap = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> lootSlotComponentToIndexMap = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> pendingLootSlotItemStateMap = new ConcurrentHashMap<>();
    private final List<Integer> nonEmptyLootSlotIndices = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean isAutoAssignRunning = false;
    private volatile int currentSlotPointer = 0;
    private volatile int waitingSlotIndex = -1;
    private volatile boolean waitingForPlayerButtons = false;
    private volatile boolean waitingForAssignResult = false;
    private volatile boolean waitingForFinishClose = false;
    private volatile int pendingPlayerComponentId = -1;
    private volatile String pendingPlayerName = "";
    private volatile int assignClickAttempts = 0;
    private volatile int finishButtonComponentId = -1;
    private volatile int finishClickAttempts = 0;
    private volatile CompletionAction pendingCompletionActionAfterFinish = CompletionAction.NONE;
    private volatile boolean autoAssignStartQueued = false;
    private volatile long lastPlayerButtonCaptureAt = 0L;

    private static final Minecraft mc = Minecraft.getMinecraft();

    static {
        loadConfig();
    }

    private LootHelper() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_raid_loot_config.json");
    }

    public static void loadConfig() {
        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            if (config.has("autoRaidEnabled")) {
                autoRaidEnabled = config.get("autoRaidEnabled").getAsBoolean();
            }
            if (config.has("autoOneKeyAssignEnabled")) {
                autoOneKeyAssignEnabled = config.get("autoOneKeyAssignEnabled").getAsBoolean();
            }
            if (config.has("playerClickDelayTicks")) {
                playerClickDelayTicks = clampPlayerClickDelay(config.get("playerClickDelayTicks").getAsInt());
            }
            if (config.has("maxAssignClickAttempts")) {
                maxAssignClickAttempts = clampMaxAssignClickAttempts(config.get("maxAssignClickAttempts").getAsInt());
            }
            if (config.has("assignPlayerPosition")) {
                assignPlayerPosition = clampAssignPlayerPosition(config.get("assignPlayerPosition").getAsInt());
            }
            if (config.has("completionAction")) {
                try {
                    completionAction = CompletionAction.valueOf(config.get("completionAction").getAsString());
                } catch (Exception ignored) {
                    completionAction = CompletionAction.NONE;
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LootHelper] 加载全自动团本配置失败。", e);
        }
    }

    public static void saveConfig() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            JsonObject config = new JsonObject();
            config.addProperty("autoRaidEnabled", autoRaidEnabled);
            config.addProperty("autoOneKeyAssignEnabled", autoOneKeyAssignEnabled);
            config.addProperty("playerClickDelayTicks", clampPlayerClickDelay(playerClickDelayTicks));
            config.addProperty("maxAssignClickAttempts", clampMaxAssignClickAttempts(maxAssignClickAttempts));
            config.addProperty("assignPlayerPosition", clampAssignPlayerPosition(assignPlayerPosition));
            config.addProperty("completionAction", completionAction.name());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LootHelper] 保存全自动团本配置失败。", e);
        }
    }

    public static int clampPlayerClickDelay(int value) {
        return Math.max(0, Math.min(40, value));
    }

    public static int clampMaxAssignClickAttempts(int value) {
        return Math.max(1, Math.min(50, value));
    }

    public static int clampAssignPlayerPosition(int value) {
        return Math.max(1, Math.min(20, value));
    }

    public void deactivateLootContext(String reason) {
        isLootTicketValid = false;
        isLootContextActive = false;
        isLootMainGuiOpen = false;
        lootMainViewId = -1;
        lootImageDetected = false;
        stopAutoAssign("上下文关闭: " + reason);
        clearRuntimeIds();
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "战利品分配上下文已重置: reason=" + reason);
    }

    private void clearRuntimeIds() {
        synchronized (lootSlotIndices) {
            lootSlotIndices.clear();
        }
        synchronized (assignedSlotIndices) {
            assignedSlotIndices.clear();
        }
        synchronized (playerButtons) {
            playerButtons.clear();
        }
        playerButtonMap.clear();
        lootSlotComponentToIndexMap.clear();
        pendingLootSlotItemStateMap.clear();
        synchronized (nonEmptyLootSlotIndices) {
            nonEmptyLootSlotIndices.clear();
        }
        waitingSlotIndex = -1;
        waitingForPlayerButtons = false;
        waitingForAssignResult = false;
        waitingForFinishClose = false;
        pendingPlayerComponentId = -1;
        pendingPlayerName = "";
        assignClickAttempts = 0;
        finishButtonComponentId = -1;
        finishClickAttempts = 0;
        pendingCompletionActionAfterFinish = CompletionAction.NONE;
        autoAssignStartQueued = false;
        lastPlayerButtonCaptureAt = 0L;
    }

    public boolean onLootViewCreated(int componentId, String viewName) {
        if (viewName == null || !viewName.startsWith("AdventureSpecialAward:")) {
            return false;
        }
        lootMainViewId = componentId;
        isLootContextActive = true;
        isLootMainGuiOpen = true;
        isLootTicketValid = true;
        clearRuntimeIds();
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "检测到战利品分配 View_CreateViewGui: viewId=" + componentId + ", viewName=" + viewName);
        return true;
    }

    public void onLootImageSavePathCaptured(String savePath) {
        onLootImageSavePathCaptured(savePath, -1);
    }

    public void onLootImageSavePathCaptured(String savePath, int parentViewId) {
        if (savePath == null) {
            return;
        }
        if (!savePath.contains("adventure/specialAward/分配战利品")) {
            return;
        }
        if (parentViewId > 0) {
            lootMainViewId = parentViewId;
        }
        isLootContextActive = true;
        lootImageDetected = true;
        isLootMainGuiOpen = true;
        isLootTicketValid = true;
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到战利品分配主图，准备激活左侧按钮。");
        tryStartAutoAssignIfConfigured();
    }

    public boolean shouldInjectButtons() {
        return lootMainViewId > 0 && isLootContextActive && isLootMainGuiOpen && lootImageDetected;
    }

    public void onLootViewOpenStateDetected(int componentId, boolean openFlag, boolean extraFlag) {
        if (componentId != lootMainViewId || componentId <= 0) {
            return;
        }
        if (openFlag && !extraFlag) {
            isLootMainGuiOpen = true;
            return;
        }
        if (!openFlag) {
            isLootMainGuiOpen = false;
        }
    }

    public void onLootViewClosed(int componentId) {
        if (componentId > 0 && componentId == lootMainViewId) {
            if (waitingForFinishClose) {
                CompletionAction action = pendingCompletionActionAfterFinish == null
                        ? CompletionAction.NONE
                        : pendingCompletionActionAfterFinish;
                cancelLootTasks();
                waitingForFinishClose = false;
                finishClickAttempts = 0;
                pendingCompletionActionAfterFinish = CompletionAction.NONE;
                deactivateLootContext("View_set_removeGui_after_finish:" + componentId);
                executeCompletionAction(action);
                return;
            }
            if (isAutoAssignRunning && !waitingForAssignResult && currentSlotPointer > 0) {
                zszlScriptMod.LOGGER.info("[LootHelper] Loot GUI closed after auto-assign progress; treating as completion. pointer={}", currentSlotPointer);
                finishAutoAssign();
                return;
            }
            deactivateLootContext("View_set_removeGui:" + componentId);
        }
    }

    public void onLootSlotIndexCaptured(int componentId, int slotIndex) {
        // 槽位组件本身的 componentId 和主界面 viewId 不是同一个值。
        // 这里只要处于战利品上下文中，就应接受槽位索引。
        if (componentId <= 0 || slotIndex < 0 || !isLootContextActive || lootMainViewId <= 0) {
            return;
        }
        lootSlotComponentToIndexMap.put(componentId, slotIndex);
        Boolean pendingHasItem = pendingLootSlotItemStateMap.remove(componentId);
        synchronized (lootSlotIndices) {
            if (!lootSlotIndices.contains(slotIndex)) {
                lootSlotIndices.add(slotIndex);
                Collections.sort(lootSlotIndices);
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "捕获战利品槽位: viewId=" + componentId + ", slot=" + slotIndex);
            }
        }
        if (pendingHasItem != null) {
            applyLootSlotItemState(slotIndex, pendingHasItem.booleanValue(), componentId);
        }
        tryStartAutoAssignIfConfigured();
    }

    public void onLootSlotItemStateCaptured(int componentId, boolean hasItem) {
        if (componentId <= 0 || !isLootContextActive || lootMainViewId <= 0) {
            return;
        }
        Integer slotIndex = lootSlotComponentToIndexMap.get(componentId);
        if (slotIndex == null || slotIndex.intValue() < 0) {
            pendingLootSlotItemStateMap.put(componentId, Boolean.valueOf(hasItem));
            return;
        }
        applyLootSlotItemState(slotIndex.intValue(), hasItem, componentId);
        if (hasItem) {
            tryStartAutoAssignIfConfigured();
        }
    }

    private void applyLootSlotItemState(int slotIndex, boolean hasItem, int componentId) {
        synchronized (nonEmptyLootSlotIndices) {
            boolean changed = false;
            if (hasItem) {
                if (!nonEmptyLootSlotIndices.contains(slotIndex)) {
                    nonEmptyLootSlotIndices.add(slotIndex);
                    Collections.sort(nonEmptyLootSlotIndices);
                    changed = true;
                }
            } else {
                changed = nonEmptyLootSlotIndices.remove(Integer.valueOf(slotIndex));
            }
            if (changed) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "更新战利品槽位物品状态: componentId=" + componentId + ", slot=" + slotIndex + ", hasItem=" + hasItem);
            }
        }
    }

    public int normalizeSlotIndex(int rawSlotIndex) {
        if (rawSlotIndex <= 0) {
            return rawSlotIndex;
        }
        // 该界面的 slot 索引在抓包里常表现为 index << 8。
        // 例如 46 会出现为 11776 (0x00002E00)。
        if ((rawSlotIndex & 0xFF) == 0 && rawSlotIndex > 255) {
            int shifted = rawSlotIndex >>> 8;
            if (shifted >= 0) {
                return shifted;
            }
        }
        return rawSlotIndex;
    }

    public void onCreateButtonCaptured(int componentId, int parentId, String buttonName) {
        if (componentId <= 0 || buttonName == null) {
            return;
        }
        if (!buttonName.startsWith("addItemButton:")) {
            if ("finishButton".equals(buttonName)) {
                finishButtonComponentId = componentId;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "捕获完成分配按钮: componentId=" + componentId + ", parentId=" + parentId);
            }
            return;
        }
        String playerName = buttonName.substring("addItemButton:".length()).trim();
        PlayerButtonInfo info = new PlayerButtonInfo(componentId, playerName);
        playerButtonMap.put(componentId, info);
        synchronized (playerButtons) {
            boolean updated = false;
            for (int i = 0; i < playerButtons.size(); i++) {
                PlayerButtonInfo button = playerButtons.get(i);
                if (button.componentId == componentId || button.playerName.equals(playerName)) {
                    playerButtons.set(i, info);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                playerButtons.add(info);
            }
        }
        lastPlayerButtonCaptureAt = System.currentTimeMillis();
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "捕获分配目标按钮: componentId=" + componentId + ", parentId=" + parentId + ", player=" + playerName);
    }

    public List<String> getViewerLines() {
        List<String> lines = new ArrayList<>();
        lines.add("主界面 View ID = " + formatId(lootMainViewId));
        lines.add("界面主图已识别 = " + lootImageDetected);
        lines.add("自动分配运行中 = " + isAutoAssignRunning);
        lines.add("等待成功反馈 = " + waitingForAssignResult);
        lines.add(" ");
        lines.add("--- 战利品槽位 ---");
        synchronized (lootSlotIndices) {
            if (lootSlotIndices.isEmpty()) {
                lines.add("暂无捕获");
            } else {
                for (int i = 0; i < lootSlotIndices.size(); i++) {
                    lines.add("slot[" + i + "] = " + lootSlotIndices.get(i));
                }
            }
        }
        lines.add(" ");
        lines.add("--- 分配对象按钮 ---");
        synchronized (playerButtons) {
            if (playerButtons.isEmpty()) {
                lines.add("暂无捕获（需先点开任意一个战利品）");
            } else {
                for (int i = 0; i < playerButtons.size(); i++) {
                    PlayerButtonInfo info = playerButtons.get(i);
                    lines.add("player[" + i + "] = " + info.playerName + " -> " + formatId(info.componentId));
                }
            }
        }
        return lines;
    }

    public void startAutoAssign() {
        if (isAutoAssignRunning) {
            stopAutoAssign("用户重新启动一键分配");
        }
        if (lootMainViewId <= 0 || !lootImageDetected) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "未识别到分配战利品主界面，无法开始自动分配。");
            return;
        }
        synchronized (lootSlotIndices) {
            if (lootSlotIndices.isEmpty()) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "未捕获到任何战利品槽位，无法开始自动分配。");
                return;
            }
        }
        synchronized (nonEmptyLootSlotIndices) {
            if (nonEmptyLootSlotIndices.isEmpty()) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "未捕获到任何有物品的战利品槽位，无法开始自动分配。");
                return;
            }
        }

        isAutoAssignRunning = true;
        currentSlotPointer = 0;
        waitingSlotIndex = -1;
        waitingForPlayerButtons = false;
        waitingForAssignResult = false;
        pendingPlayerComponentId = -1;
        pendingPlayerName = "";
        assignClickAttempts = 0;
        autoAssignStartQueued = false;
        lastPlayerButtonCaptureAt = 0L;
        cancelLootTasks();
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "开始自动分配战利品。按槽位顺序处理，等待成功反馈后再继续下一个槽位。");
        processNextSlot();
    }

    public void stopAutoAssign(String reason) {
        if (!isAutoAssignRunning && !waitingForPlayerButtons && !waitingForAssignResult) {
            return;
        }
        isAutoAssignRunning = false;
        waitingForPlayerButtons = false;
        waitingForAssignResult = false;
        waitingSlotIndex = -1;
        pendingPlayerComponentId = -1;
        pendingPlayerName = "";
        assignClickAttempts = 0;
        autoAssignStartQueued = false;
        cancelLootTasks();
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "战利品自动分配已停止: reason=" + reason);
    }

    private void processNextSlot() {
        if (!isAutoAssignRunning) {
            return;
        }
        List<Integer> slots = copySlots();
        while (currentSlotPointer < slots.size() && shouldSkipSlot(slots.get(currentSlotPointer))) {
            currentSlotPointer++;
        }
        if (currentSlotPointer >= slots.size()) {
            finishAutoAssign();
            return;
        }

        int slotIndex = slots.get(currentSlotPointer);
        waitingSlotIndex = slotIndex;
        waitingForPlayerButtons = true;
        waitingForAssignResult = false;
        pendingPlayerComponentId = -1;
        pendingPlayerName = "";
        assignClickAttempts = 0;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "处理战利品槽位: slot=" + slotIndex + ", index=" + currentSlotPointer + "/" + slots.size());
        sendSlotClickPacket(lootMainViewId, slotIndex);
        startPlayerSelectionWait(slotIndex);
    }

    private boolean tryClickPendingPlayer() {
        if (!isAutoAssignRunning || !waitingForPlayerButtons || waitingSlotIndex < 0) {
            return true;
        }
        List<PlayerButtonInfo> buttons = copyPlayerButtons();
        if (buttons.isEmpty()) {
            return false;
        }
        int desiredIndex = clampAssignPlayerPosition(assignPlayerPosition) - 1;
        if (desiredIndex < 0 || desiredIndex >= buttons.size()) {
            desiredIndex = 0;
        }
        clickChosenPlayer(buttons.get(desiredIndex));
        return true;
    }

    private void clickChosenPlayer(PlayerButtonInfo chosen) {
        if (chosen == null || !isAutoAssignRunning || !waitingForPlayerButtons) {
            return;
        }
        waitingForPlayerButtons = false;
        waitingForAssignResult = true;
        pendingPlayerComponentId = chosen.componentId;
        pendingPlayerName = chosen.playerName;
        assignClickAttempts = 1;
        cancelLootTasks();
        sendButtonClickPacket(pendingPlayerComponentId);
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "已为 slot=" + waitingSlotIndex + " 点击分配对象: " + pendingPlayerName
                        + ", buttonId=" + pendingPlayerComponentId
                        + ", attempt=" + assignClickAttempts);
        startAssignResultTimeout(waitingSlotIndex);
        scheduleAssignResultRetry(waitingSlotIndex);
    }

    private void startPlayerSelectionWait(final int slotIndex) {
        cancelLootTasks();
        schedulePlayerClickAttempt(slotIndex, clampPlayerClickDelay(playerClickDelayTicks));
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (!isAutoAssignRunning || !waitingForPlayerButtons || waitingSlotIndex != slotIndex) {
                return;
            }
            stopAutoAssign("等待分配对象按钮超时: slot=" + slotIndex);
        }, PLAYER_BUTTON_TIMEOUT_TICKS, PLAYER_TIMEOUT_TASK_TAG);
    }

    private void schedulePlayerClickAttempt(final int slotIndex, int delayTicks) {
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (!isAutoAssignRunning || !waitingForPlayerButtons || waitingSlotIndex != slotIndex) {
                return;
            }
            if (!tryClickPendingPlayer()) {
                schedulePlayerClickAttempt(slotIndex, PLAYER_RETRY_DELAY_TICKS);
            }
        }, delayTicks, PLAYER_CLICK_TASK_TAG);
    }

    public void onLootLabelTextCaptured(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (text.contains("分配成功") && text.contains("邮箱")) {
            onAssignSuccessFeedback();
        } else if (isAutoAssignRunning && waitingForAssignResult
                && (text.contains("分配失败") || text.contains("无法分配"))) {
            stopAutoAssign("收到分配失败反馈: " + text);
        }
    }

    private void onAssignSuccessFeedback() {
        if (!isAutoAssignRunning || !waitingForAssignResult || waitingSlotIndex < 0) {
            return;
        }

        int finishedSlot = waitingSlotIndex;
        synchronized (assignedSlotIndices) {
            if (!assignedSlotIndices.contains(finishedSlot)) {
                assignedSlotIndices.add(finishedSlot);
                Collections.sort(assignedSlotIndices);
            }
        }

        waitingSlotIndex = -1;
        waitingForAssignResult = false;
        pendingPlayerComponentId = -1;
        pendingPlayerName = "";
        assignClickAttempts = 0;
        currentSlotPointer++;
        cancelLootTasks();
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "收到分配成功反馈，slot=" + finishedSlot + " 已完成，准备处理下一个槽位。");
        if (hasMoreUnassignedSlots()) {
            scheduleNextStep(NEXT_SLOT_DELAY_TICKS);
        } else {
            finishAutoAssign();
        }
    }

    private void startAssignResultTimeout(final int slotIndex) {
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (!isAutoAssignRunning || !waitingForAssignResult || waitingSlotIndex != slotIndex) {
                return;
            }
            stopAutoAssign("等待分配成功反馈超时: slot=" + slotIndex);
        }, ASSIGN_RESULT_TIMEOUT_TICKS, ASSIGN_RESULT_TIMEOUT_TASK_TAG);
    }

    private void scheduleAssignResultRetry(final int slotIndex) {
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (!isAutoAssignRunning || !waitingForAssignResult || waitingSlotIndex != slotIndex) {
                return;
            }
            if (pendingPlayerComponentId <= 0) {
                stopAutoAssign("等待分配成功时缺少玩家按钮 ID: slot=" + slotIndex);
                return;
            }
            if (assignClickAttempts >= clampMaxAssignClickAttempts(maxAssignClickAttempts)) {
                return;
            }

            assignClickAttempts++;
            sendButtonClickPacket(pendingPlayerComponentId);
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "分配成功反馈未到，重试点击玩家按钮: slot=" + slotIndex
                            + ", player=" + pendingPlayerName
                            + ", buttonId=" + pendingPlayerComponentId
                            + ", attempt=" + assignClickAttempts);
            scheduleAssignResultRetry(slotIndex);
        }, ASSIGN_RESULT_RETRY_DELAY_TICKS, ASSIGN_RESULT_RETRY_TASK_TAG);
    }

    private void scheduleNextStep(int delayTicks) {
        cancelLootTasks();
        ModUtils.DelayScheduler.instance.schedule(this::processNextSlot, delayTicks, NEXT_SLOT_TASK_TAG);
    }

    private boolean hasMoreUnassignedSlots() {
        List<Integer> slots = copySlots();
        for (int i = currentSlotPointer; i < slots.size(); i++) {
            if (!shouldSkipSlot(slots.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void finishAutoAssign() {
        zszlScriptMod.LOGGER.info("[LootHelper] Auto assign finished. completionAction={}", completionAction);
        stopAutoAssign("全部槽位处理完成");
        clickFinishButtonThenCompletion();
    }

    private void clickFinishButtonThenCompletion() {
        CompletionAction action = completionAction == null ? CompletionAction.NONE : completionAction;
        if (finishButtonComponentId <= 0) {
            zszlScriptMod.LOGGER.warn("[LootHelper] finishButton component id missing; skip completion action.");
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "未捕获到完成分配按钮，已停止自动流程，不执行退出动作。");
            return;
        }
        waitingForFinishClose = true;
        pendingCompletionActionAfterFinish = action;
        finishClickAttempts = 0;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "战利品分配完成，准备点击完成分配按钮，随后执行退出动作: " + action.name());
        scheduleFinishButtonClick(clampPlayerClickDelay(playerClickDelayTicks));
    }

    private void scheduleFinishButtonClick(int delayTicks) {
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (!waitingForFinishClose) {
                return;
            }
            if (finishButtonComponentId <= 0) {
                waitingForFinishClose = false;
                CompletionAction action = pendingCompletionActionAfterFinish;
                pendingCompletionActionAfterFinish = CompletionAction.NONE;
                zszlScriptMod.LOGGER.warn("[LootHelper] finishButton component id missing during finish click sequence.");
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "未捕获到完成分配按钮，终止退出动作。");
                return;
            }
            if (finishClickAttempts >= clampMaxAssignClickAttempts(maxAssignClickAttempts)) {
                waitingForFinishClose = false;
                pendingCompletionActionAfterFinish = CompletionAction.NONE;
                zszlScriptMod.LOGGER.warn("[LootHelper] finishButton click max attempts reached without gui close.");
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "完成分配按钮点击超过重试上限，终止退出动作。");
                return;
            }

            finishClickAttempts++;
            sendButtonClickPacket(finishButtonComponentId);
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "点击完成分配按钮: buttonId=" + finishButtonComponentId + ", attempt=" + finishClickAttempts);

            if (waitingForFinishClose) {
                scheduleFinishButtonClick(clampPlayerClickDelay(playerClickDelayTicks));
            }
        }, delayTicks, FINISH_BUTTON_CLICK_TASK_TAG);
    }

    private void executeCompletionAction(CompletionAction action) {
        CompletionAction resolvedAction = action == null ? CompletionAction.NONE : action;
        if (resolvedAction == CompletionAction.NONE) {
            zszlScriptMod.LOGGER.info("[LootHelper] No completion action configured after finish button click.");
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "完成分配按钮已点击，未配置额外退出动作。");
            return;
        }
        zszlScriptMod.LOGGER.info("[LootHelper] Scheduling completion action after finish button click: {}", resolvedAction);
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "完成分配按钮已生效，准备执行退出动作: " + resolvedAction.name());
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (resolvedAction == CompletionAction.SUICIDE) {
                if (mc.player != null) {
                    zszlScriptMod.LOGGER.info("[LootHelper] Sending completion chat command: /suicide");
                    ModConfig.debugPrint(DebugModule.MAIL_GUI, "执行分配完成后动作: /suicide");
                    mc.player.sendChatMessage("/suicide");
                }
            } else if (resolvedAction == CompletionAction.DISCONNECT) {
                zszlScriptMod.LOGGER.info("[LootHelper] Disconnecting after auto assign completion.");
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "执行分配完成后动作: disconnect");
                ModUtils.disconnectFromCurrentWorld();
            }
        }, 4);
    }

    private void tryStartAutoAssignIfConfigured() {
        if (!autoRaidEnabled || !autoOneKeyAssignEnabled || isAutoAssignRunning || autoAssignStartQueued) {
            return;
        }
        if (!lootImageDetected || lootMainViewId <= 0 || !isLootContextActive) {
            return;
        }
        synchronized (nonEmptyLootSlotIndices) {
            if (nonEmptyLootSlotIndices.isEmpty()) {
                return;
            }
        }
        autoAssignStartQueued = true;
        ModUtils.DelayScheduler.instance.schedule(() -> {
            autoAssignStartQueued = false;
            if (autoRaidEnabled && autoOneKeyAssignEnabled && !isAutoAssignRunning) {
                startAutoAssign();
            }
        }, AUTO_START_DELAY_TICKS, AUTO_START_TASK_TAG);
    }

    private void cancelLootTasks() {
        ModUtils.DelayScheduler.instance.cancelTasks(task -> {
            String tag = task.getTag();
            return tag != null && tag.startsWith(TASK_TAG_PREFIX);
        });
    }

    private List<Integer> copySlots() {
        synchronized (lootSlotIndices) {
            return new ArrayList<>(lootSlotIndices);
        }
    }

    private boolean shouldSkipSlot(int slotIndex) {
        return isSlotAssigned(slotIndex) || !hasLootItemInSlot(slotIndex);
    }

    private boolean isSlotAssigned(int slotIndex) {
        synchronized (assignedSlotIndices) {
            return assignedSlotIndices.contains(slotIndex);
        }
    }

    private boolean hasLootItemInSlot(int slotIndex) {
        synchronized (nonEmptyLootSlotIndices) {
            return nonEmptyLootSlotIndices.contains(slotIndex);
        }
    }

    private List<PlayerButtonInfo> copyPlayerButtons() {
        synchronized (playerButtons) {
            return new ArrayList<>(playerButtons);
        }
    }

    private void sendSlotClickPacket(int viewId, int slotIndex) {
        String slotHex = String.format(Locale.ROOT, "%02X", slotIndex & 0xFF);
        sendOwlViewPacket(SLOT_CLICK_TEMPLATE
                .replace("{view_id}", intToHex(viewId))
                .replace("{slot_index_hex}", slotHex));
    }

    private void sendButtonClickPacket(int componentId) {
        sendOwlViewPacket(BUTTON_CLICK_TEMPLATE
                .replace("{component_id}", intToHex(componentId)));
    }

    private void sendOwlViewPacket(String template) {
        if (mc.player == null || mc.getConnection() == null) {
            stopAutoAssign("客户端连接不可用");
            return;
        }
        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        if (sessionIdHex == null || sessionIdHex.trim().isEmpty()) {
            stopAutoAssign("未捕获到 session_id");
            return;
        }
        String finalHexPayload = template.replace("{session_id}", sessionIdHex);
        try {
            byte[] data = hexToBytes(finalHexPayload);
            PacketBuffer payload = new PacketBuffer(Unpooled.wrappedBuffer(data));
            CPacketCustomPayload packet = new CPacketCustomPayload("OwlViewChannel", payload);
            mc.getConnection().sendPacket(packet);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[LootHelper] 发送战利品分配数据包失败。", e);
            stopAutoAssign("发包异常: " + e.getClass().getSimpleName());
        }
    }

    private byte[] hexToBytes(String hex) {
        String cleanHex = hex.replaceAll("\\s+", "");
        byte[] data = new byte[cleanHex.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(cleanHex.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }

    private String intToHex(int value) {
        return String.format(Locale.ROOT, "%02X %02X %02X %02X",
                (value >> 24) & 0xFF,
                (value >> 16) & 0xFF,
                (value >> 8) & 0xFF,
                value & 0xFF);
    }

    private String formatId(int value) {
        if (value <= 0) {
            return "<未捕获>";
        }
        return value + " (" + intToHex(value) + ")";
    }
}
