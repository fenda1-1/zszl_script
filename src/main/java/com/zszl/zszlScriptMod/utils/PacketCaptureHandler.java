// --- Full Java Content (src/main/java/com/zszl/zszlScriptMod/utils/PacketCaptureHandler.java) ---
package com.zszl.zszlScriptMod.utils;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.gui.packet.InputTimelineManager;
import com.zszl.zszlScriptMod.gui.packet.PacketIdRecordManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.zszl.zszlScriptMod.PerformanceMonitor;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.packet.PacketFilterConfig;
import com.zszl.zszlScriptMod.handlers.MailHelper;
import com.zszl.zszlScriptMod.handlers.RefineHelper;
import com.zszl.zszlScriptMod.path.PathSequenceEventListener;
import com.zszl.zszlScriptMod.path.node.NodeTriggerManager;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.server.SPacketCollectItem;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.network.play.server.SPacketUpdateBossInfo;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class PacketCaptureHandler extends ChannelDuplexHandler {

    private static final ExecutorService PACKET_PROCESS_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "zszl-packet-processor");
        t.setDaemon(true);
        return t;
    });

    private static final String OWL_CONTROL_CHANNEL = "OwlControlChannel";
    private static final String OWL_VIEW_CHANNEL = "OwlViewChannel";
    private static final byte[] BUTTON_CLICK_MARKER = "Button_click"
            .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static String lastSessionIdHexForMailInit = null;

    public static boolean isCapturing = false;
    public static final List<CapturedPacketData> capturedPackets = Collections.synchronizedList(new ArrayList<>());
    public static final List<CapturedPacketData> capturedReceivedPackets = Collections
            .synchronizedList(new ArrayList<>());

    // 捕获队列与节流参数：避免同一帧内大量 addScheduledTask 造成主线程卡顿
    private static final int MAX_CAPTURE_QUEUE = 6000;
    private static final int MAX_CAPTURE_PROCESS_PER_TICK = 40;
    private static final int MAX_CAPTURE_PROCESS_BYTES_PER_TICK = 256 * 1024;
    private static final long MAX_CAPTURE_PROCESS_NANOS_PER_TICK = 2_000_000L;
    private static final int MAX_CAPTURED_PACKETS = 3000;
    private static final int CAPTURE_TRIM_BATCH = 120;
    private static final int MAX_RECENT_TEXT_DECODE_BYTES = 64 * 1024;
    private static final long UI_SNAPSHOT_INTERVAL_MS = 500L;
    private static final long AGGREGATION_WINDOW_MS = 500L;
    private static final long MAX_BUSINESS_TASK_NANOS_PER_TICK = 1_500_000L;
    private final ConcurrentLinkedQueue<PendingPacketSnapshot> pendingCaptureQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean captureDrainScheduled = new AtomicBoolean(false);
    private volatile long lastCaptureDropWarnAt = 0L;
    private static final AtomicBoolean businessTaskScheduled = new AtomicBoolean(false);
    private static final ConcurrentLinkedQueue<Runnable> pendingBusinessTasks = new ConcurrentLinkedQueue<>();
    private static volatile int lastKnownCaptureQueueSize = 0;
    private static volatile long sampledPacketCount = 0L;
    private static volatile long droppedPacketCount = 0L;
    private static volatile int activeSamplingModulo = 1;
    private static volatile PacketCaptureUiSnapshot lastUiSnapshot = new PacketCaptureUiSnapshot(0, 0, 0, 0, 1, true,
            true, System.currentTimeMillis());
    private static final AtomicBoolean ruleSyncDirty = new AtomicBoolean(false);
    private static final AtomicBoolean sessionInitDirty = new AtomicBoolean(false);

    private static boolean missingIdNoticeShown = false;

    // 始终缓存最近接收的 OwlView HEX（用于业务逻辑解析，不依赖捕获开关）
    private static final int MAX_RECENT_OWLVIEW_HEX = 120;
    private static final List<String> recentOwlViewIncomingHex = Collections.synchronizedList(new ArrayList<>());
    // 始终缓存最近接收的 OwlView 解码文本（用于业务逻辑解析）
    private static final List<String> recentOwlViewDecoded = Collections.synchronizedList(new ArrayList<>());
    // 始终缓存最近数据包文本（用于等待数据包文本动作）
    private static final int MAX_RECENT_PACKET_TEXTS = 200;
    private static final List<String> recentPacketTexts = Collections.synchronizedList(new ArrayList<>());
    private static volatile long recentPacketTextVersion = 0L;

    private static class PendingPacketSnapshot {
        final String packetClassName;
        final boolean isFmlPacket;
        final Integer packetId;
        final String channel;
        final byte[] rawData;
        final boolean isSent;

        PendingPacketSnapshot(String packetClassName, boolean isFmlPacket, Integer packetId, String channel,
                byte[] rawData, boolean isSent) {
            this.packetClassName = packetClassName;
            this.isFmlPacket = isFmlPacket;
            this.packetId = packetId;
            this.channel = channel;
            this.rawData = rawData;
            this.isSent = isSent;
        }
    }

    public enum CaptureMode {
        BLACKLIST, WHITELIST;

        public CaptureMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    public static class CapturedPacketData {
        public final long timestamp;
        public final String packetClassName;
        public final boolean isFmlPacket;
        public final Integer packetId;
        public final String channel;
        public final byte[] rawData;
        private volatile String hexData;
        private volatile String decodedData;
        private volatile String decodedDetailData;
        private volatile String decodedFullData;
        private final int payloadSize;
        private volatile long lastTimestamp;
        private volatile int occurrenceCount;
        private volatile int totalPayloadBytes;

        public CapturedPacketData(long timestamp, String packetClassName, boolean isFmlPacket, Integer packetId,
                String channel, byte[] rawData, String decodedData) {
            this.timestamp = timestamp;
            this.packetClassName = packetClassName;
            this.isFmlPacket = isFmlPacket;
            this.packetId = packetId;
            this.channel = channel;
            this.rawData = rawData == null ? new byte[0] : rawData;
            this.decodedData = decodedData;
            this.payloadSize = this.rawData.length;
            this.lastTimestamp = timestamp;
            this.occurrenceCount = 1;
            this.totalPayloadBytes = this.payloadSize;
        }

        public CapturedPacketData(String packetClassName, boolean isFmlPacket, Integer packetId, String channel,
                byte[] rawData, String decodedData) {
            this(System.currentTimeMillis(), packetClassName, isFmlPacket, packetId, channel, rawData, decodedData);
        }

        public int getPayloadSize() {
            return payloadSize;
        }

        public long getLastTimestamp() {
            return lastTimestamp;
        }

        public int getOccurrenceCount() {
            return occurrenceCount;
        }

        public int getTotalPayloadBytes() {
            return totalPayloadBytes;
        }

        public boolean isAggregated() {
            return occurrenceCount > 1;
        }

        public String getHexData() {
            String local = hexData;
            if (local == null) {
                local = bytesToHex(rawData);
                hexData = local == null ? "" : local;
            }
            return hexData;
        }

        public String getDecodedData() {
            String local = decodedData;
            if (local == null) {
                if (isFmlPacket && OWL_CONTROL_CHANNEL.equals(channel)) {
                    local = OwlViewPacketDecoder.decode(channel, rawData);
                } else {
                    local = decodePayload(rawData);
                }
                decodedData = local == null ? "" : local;
            }
            return decodedData;
        }

        public String getDecodedDetailData() {
            String local = decodedDetailData;
            if (local == null) {
                if (isFmlPacket && OWL_CONTROL_CHANNEL.equals(channel)) {
                    local = getDecodedData();
                } else {
                    local = PacketPayloadDecoder.decodeDetailed(rawData);
                    if ((local == null || local.trim().isEmpty()) && decodedData != null) {
                        local = decodedData;
                    }
                }
                decodedDetailData = local == null ? "" : local;
            }
            return decodedDetailData;
        }

        public String getDecodedFullData() {
            String local = decodedFullData;
            if (local == null) {
                if (isFmlPacket && OWL_CONTROL_CHANNEL.equals(channel)) {
                    local = getDecodedData();
                } else {
                    local = PacketPayloadDecoder.decodeFull(rawData);
                    if ((local == null || local.trim().isEmpty()) && decodedData != null) {
                        local = decodedData;
                    }
                }
                decodedFullData = local == null ? "" : local;
            }
            return decodedFullData;
        }

        public boolean canAggregate(CapturedPacketData other) {
            if (other == null) {
                return false;
            }
            if (other.timestamp - this.lastTimestamp > AGGREGATION_WINDOW_MS) {
                return false;
            }
            return this.isFmlPacket == other.isFmlPacket && Objects.equals(this.packetClassName, other.packetClassName)
                    && Objects.equals(this.channel, other.channel) && Objects.equals(this.packetId, other.packetId)
                    && Arrays.equals(this.rawData, other.rawData);
        }

        public void mergeFrom(CapturedPacketData other) {
            if (other == null) {
                return;
            }
            this.lastTimestamp = Math.max(this.lastTimestamp, other.lastTimestamp);
            this.occurrenceCount += other.occurrenceCount;
            this.totalPayloadBytes += other.totalPayloadBytes;
        }

        public void restoreAggregateState(long restoredLastTimestamp, int restoredOccurrenceCount,
                int restoredTotalPayloadBytes) {
            this.lastTimestamp = Math.max(this.timestamp, restoredLastTimestamp);
            this.occurrenceCount = Math.max(1, restoredOccurrenceCount);
            this.totalPayloadBytes = Math.max(this.payloadSize, restoredTotalPayloadBytes);
        }
    }

    public static class PacketCaptureUiSnapshot {
        public final int sentCount;
        public final int receivedCount;
        public final int queueSize;
        public final long droppedCount;
        public final int samplingModulo;
        public final boolean businessProcessingEnabled;
        public final boolean adaptiveSamplingEnabled;
        public final long createdAt;

        public PacketCaptureUiSnapshot(int sentCount, int receivedCount, int queueSize, long droppedCount,
                int samplingModulo, boolean businessProcessingEnabled, boolean adaptiveSamplingEnabled,
                long createdAt) {
            this.sentCount = sentCount;
            this.receivedCount = receivedCount;
            this.queueSize = queueSize;
            this.droppedCount = droppedCount;
            this.samplingModulo = samplingModulo;
            this.businessProcessingEnabled = businessProcessingEnabled;
            this.adaptiveSamplingEnabled = adaptiveSamplingEnabled;
            this.createdAt = createdAt;
        }
    }

    public static byte[] getOwlViewSessionID() {
        return CapturedIdRuleManager.getCapturedIdBytes("id");
    }

    public static String getSessionIdAsHex() {
        return CapturedIdRuleManager.getCapturedIdHex("id");
    }

    public static byte[] getJjcID1() {
        return CapturedIdRuleManager.getCapturedIdBytes("jjc_id1");
    }

    public static void setJjcID1(byte[] id) {
        CapturedIdRuleManager.setCapturedId("jjc_id1", id);
    }

    public static String getJjcID1AsHex() {
        return CapturedIdRuleManager.getCapturedIdHex("jjc_id1");
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return null;
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b));
        }
        return hex.toString().trim();
    }

    private static int indexOf(byte[] source, byte[] target) {
        if (source == null || target == null || target.length == 0 || source.length < target.length) {
            return -1;
        }
        for (int i = 0; i <= source.length - target.length; i++) {
            boolean matched = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] tryExtractClickedComponentId(byte[] outboundData) {
        if (outboundData == null || outboundData.length == 0) {
            return null;
        }
        int markerPos = indexOf(outboundData, BUTTON_CLICK_MARKER);
        if (markerPos < 6) {
            return null;
        }
        // 结构: [componentId(4)] 00 0C "Button_click"
        if ((outboundData[markerPos - 1] & 0xFF) != 0x0C || (outboundData[markerPos - 2] & 0xFF) != 0x00) {
            return null;
        }
        return Arrays.copyOfRange(outboundData, markerPos - 6, markerPos - 2);
    }

    private static boolean isSwitchLineConfirmClickPacket(String channel, byte[] outboundData) {
        if (!OWL_VIEW_CHANNEL.equals(channel) || outboundData == null || outboundData.length == 0) {
            return false;
        }
        // 保护：邮件GUI上下文中会大量复用组件ID，避免误判成“换线确认”导致自动化被重置。
        if (MailHelper.INSTANCE.isMailContextActive || MailHelper.INSTANCE.isFingerprintTicketValid) {
            return false;
        }
        // 安全保护：只有会话ID和换线确认按钮ID都已捕获时，才允许触发“清空全部ID”。
        byte[] sessionId = CapturedIdRuleManager.getCapturedIdBytes("id");
        if (sessionId == null || sessionId.length == 0) {
            return false;
        }
        byte[] switchConfirmId = CapturedIdRuleManager.getCapturedIdBytes("switch_line_confirm_button_id");
        if (switchConfirmId == null || switchConfirmId.length != 4) {
            return false;
        }
        byte[] clickedId = tryExtractClickedComponentId(outboundData);
        return clickedId != null && Arrays.equals(clickedId, switchConfirmId);
    }

    public static void resetOwlViewSessionID() {
        CapturedIdRuleManager.clearAllCapturedIds();
        lastSessionIdHexForMailInit = null;
        missingIdNoticeShown = false;
        zszlScriptMod.LOGGER.info("[PacketCapture] OwLView会话ID已重置。");
    }

    private static void tryInitializeMailBySessionChange() {
        String currentSessionHex = getSessionIdAsHex();
        if (currentSessionHex == null || currentSessionHex.trim().isEmpty()) {
            return;
        }
        if (currentSessionHex.equals(lastSessionIdHexForMailInit)) {
            return;
        }

        lastSessionIdHexForMailInit = currentSessionHex;
    }

    public static void notifyIfSessionIdMissing() {
        if (missingIdNoticeShown) {
            return;
        }
        if (CapturedIdRuleManager.getCapturedIdBytes("id") != null) {
            return;
        }
        if (Minecraft.getMinecraft().player != null) {
            missingIdNoticeShown = true;
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "请打开背包一次以获取必要的会话ID，每次进入新线路都需要重新获取。（仅再生之路，其他服务器忽略）"));
        }
    }

    /**
     * 客户端 Tick 钩子（兼容旧调用点）。 当前会话更新逻辑已改为“无自动动作”，此处保留空实现避免编译失败。
     */
    public static void onClientTick() {
        // no-op
    }

    public static void clearAllPackets() {
        capturedPackets.clear();
        capturedReceivedPackets.clear();
        sampledPacketCount = 0L;
        droppedPacketCount = 0L;
        activeSamplingModulo = 1;
        lastKnownCaptureQueueSize = 0;
        InputTimelineManager.clear();
    }

    public static int getPendingCaptureQueueSize() {
        return lastKnownCaptureQueueSize;
    }

    public static long getSampledPacketCount() {
        return sampledPacketCount;
    }

    public static long getDroppedPacketCount() {
        return droppedPacketCount;
    }

    public static int getActiveSamplingModulo() {
        return activeSamplingModulo;
    }

    public static PacketCaptureUiSnapshot getUiSnapshot() {
        PacketCaptureUiSnapshot snapshot = lastUiSnapshot;
        long now = System.currentTimeMillis();
        if (snapshot == null || now - snapshot.createdAt >= UI_SNAPSHOT_INTERVAL_MS) {
            PacketFilterConfig config = PacketFilterConfig.INSTANCE;
            snapshot = new PacketCaptureUiSnapshot(capturedPackets.size(), capturedReceivedPackets.size(),
                    lastKnownCaptureQueueSize, droppedPacketCount, activeSamplingModulo,
                    config == null || config.enableBusinessPacketProcessing,
                    config == null || config.enableAdaptiveSampling, now);
            lastUiSnapshot = snapshot;
        }
        return snapshot;
    }

    public static List<String> getRecentOwlViewIncomingHexSnapshot() {
        synchronized (recentOwlViewIncomingHex) {
            return new ArrayList<>(recentOwlViewIncomingHex);
        }
    }

    public static void clearRecentOwlViewIncomingHex() {
        synchronized (recentOwlViewIncomingHex) {
            recentOwlViewIncomingHex.clear();
        }
    }

    public static List<String> getRecentPacketTextsSnapshot() {
        synchronized (recentPacketTexts) {
            return new ArrayList<>(recentPacketTexts);
        }
    }

    public static long getRecentPacketTextVersion() {
        return recentPacketTextVersion;
    }

    public static void clearRecentPacketTexts() {
        synchronized (recentPacketTexts) {
            recentPacketTexts.clear();
            recentPacketTextVersion = 0L;
        }
    }

    private static void storeIncomingOwlViewHex(byte[] rawData) {
        if (rawData == null || rawData.length == 0) {
            return;
        }
        StringBuilder hex = new StringBuilder(rawData.length * 3);
        for (byte b : rawData) {
            hex.append(String.format("%02X ", b));
        }
        String hexData = hex.toString().trim();
        synchronized (recentOwlViewIncomingHex) {
            recentOwlViewIncomingHex.add(hexData);
            while (recentOwlViewIncomingHex.size() > MAX_RECENT_OWLVIEW_HEX) {
                recentOwlViewIncomingHex.remove(0);
            }
        }
    }

    private boolean isBusinessProcessingEnabled() {
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        return config == null || config.enableBusinessPacketProcessing;
    }

    private boolean hasPacketTriggerListeners() {
        return NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_PACKET)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_PACKET);
    }

    private boolean isRecentPacketTextFeedNeeded() {
        return PathSequenceEventListener.instance != null && PathSequenceEventListener.instance.isTracking();
    }

    private void enqueueBusinessTask(Runnable task) {
        if (task == null) {
            return;
        }
        pendingBusinessTasks.offer(task);
        scheduleBusinessTaskDrain();
    }

    private void scheduleBusinessTaskDrain() {
        if (!businessTaskScheduled.compareAndSet(false, true)) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(this::drainBusinessTasksOnMainThread);
    }

    private void requestRuleSyncOnMainThread() {
        if (ruleSyncDirty.compareAndSet(false, true)) {
            enqueueBusinessTask(() -> {
                ruleSyncDirty.set(false);
                MailHelper.INSTANCE.syncCapturedValuesFromRules();
            });
        }
    }

    private void requestSessionInitCheckOnMainThread() {
        if (sessionInitDirty.compareAndSet(false, true)) {
            enqueueBusinessTask(() -> {
                sessionInitDirty.set(false);
                tryInitializeMailBySessionChange();
            });
        }
    }

    private void drainBusinessTasksOnMainThread() {
        businessTaskScheduled.set(false);
        int budget = 32;
        long startNanos = System.nanoTime();
        while (budget-- > 0) {
            if (System.nanoTime() - startNanos >= MAX_BUSINESS_TASK_NANOS_PER_TICK) {
                break;
            }
            Runnable task = pendingBusinessTasks.poll();
            if (task == null) {
                break;
            }
            try {
                task.run();
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("[PacketCapture] 执行业务任务失败", e);
            }
        }
        if (!pendingBusinessTasks.isEmpty()) {
            scheduleBusinessTaskDrain();
        }
    }

    private boolean shouldSampleCapture() {
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        if (config == null || !config.enableAdaptiveSampling) {
            activeSamplingModulo = 1;
            return true;
        }

        int queueSize = pendingCaptureQueue.size();
        lastKnownCaptureQueueSize = queueSize;
        if (queueSize < config.adaptiveSamplingQueueThreshold) {
            activeSamplingModulo = 1;
            return true;
        }

        activeSamplingModulo = Math.max(2, config.adaptiveSamplingModulo);
        long current = sampledPacketCount + droppedPacketCount + 1L;
        boolean keep = current % activeSamplingModulo == 0;
        if (!keep) {
            sampledPacketCount++;
        }
        return keep;
    }

    // --- 核心修改：修复 channelRead 方法 ---
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean inboundFeatureEnabled = PerformanceMonitor.isFeatureEnabled("packet_capture_inbound");
        boolean businessProcessingEnabled = isBusinessProcessingEnabled();
        PerformanceMonitor.PerformanceTimer timer = PerformanceMonitor.startTimer("packet_capture_inbound");
        try {
            Object inboundMsg = msg;

            if (businessProcessingEnabled && inboundMsg instanceof SPacketSetSlot) {
                try {
                    SPacketSetSlot setSlot = (SPacketSetSlot) inboundMsg;
                    enqueueBusinessTask(() -> RefineHelper.INSTANCE.onPossibleSetSlotPacket(setSlot));
                } catch (Exception ignored) {
                }
            }

            // S->C 拦截与改包：在进入游戏主逻辑前执行
            if (PerformanceMonitor.isFeatureEnabled("packet_intercept") && msg instanceof Packet) {
                PerformanceMonitor.PerformanceTimer interceptTimer = PerformanceMonitor.startTimer("packet_intercept");
                try {
                    Packet<?> inboundPacket = (Packet<?>) msg;
                    boolean skipStandardSerialization = inboundPacket instanceof net.minecraft.network.play.server.SPacketSpawnPlayer
                            || inboundPacket instanceof net.minecraft.network.play.server.SPacketSpawnMob;
                    if (msg instanceof FMLProxyPacket) {
                        FMLProxyPacket origin = (FMLProxyPacket) msg;
                        String channel = origin.channel();
                        ByteBuf payload = origin.payload();
                        byte[] rawData = new byte[payload.readableBytes()];
                        payload.getBytes(payload.readerIndex(), rawData);

                        PacketInterceptManager.PacketMeta meta = new PacketInterceptManager.PacketMeta(channel,
                                origin.getClass().getSimpleName(), null);
                        PacketInterceptManager.InterceptResult interceptResult = PacketInterceptManager
                                .applyInboundRules(meta, rawData);
                        if (interceptResult.modified && interceptResult.payload != null) {
                            // 关键：不要 new FMLProxyPacket（会丢失 dispatcher，导致 NetworkRegistry NPE）
                            // 直接改写原始 payload，保留原包上下文。
                            try {
                                payload.readerIndex(0);
                                payload.writerIndex(0);
                                payload.writeBytes(interceptResult.payload);
                                inboundMsg = origin;
                            } catch (Exception overwriteEx) {
                                zszlScriptMod.LOGGER.warn("[PacketIntercept] 改写FMLProxyPacket载荷失败，回退原包: {}", channel,
                                        overwriteEx);
                                inboundMsg = origin;
                            }
                        }
                    } else if (!skipStandardSerialization) {
                        try {
                            PacketBuffer rawBuffer = new PacketBuffer(Unpooled.buffer());
                            inboundPacket.writePacketData(rawBuffer);
                            byte[] rawData = new byte[rawBuffer.readableBytes()];
                            rawBuffer.readBytes(rawData);

                            Integer packetId = EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND,
                                    inboundPacket);
                            PacketInterceptManager.PacketMeta meta = new PacketInterceptManager.PacketMeta("N/A",
                                    inboundPacket.getClass().getSimpleName(), packetId);
                            PacketInterceptManager.InterceptResult interceptResult = PacketInterceptManager
                                    .applyInboundRules(meta, rawData);

                            if (interceptResult.modified && interceptResult.payload != null) {
                                @SuppressWarnings("unchecked")
                                Packet<?> rebuilt = inboundPacket.getClass().newInstance();
                                PacketBuffer modifiedBuffer = new PacketBuffer(
                                        Unpooled.wrappedBuffer(interceptResult.payload));
                                rebuilt.readPacketData(modifiedBuffer);
                                inboundMsg = rebuilt;
                            }
                        } catch (Exception rebuildEx) {
                            zszlScriptMod.LOGGER.warn("[PacketIntercept] 重建标准S->C包失败，回退原包: {}",
                                    inboundPacket.getClass().getSimpleName(), rebuildEx);
                        }
                    }
                } finally {
                    interceptTimer.stop();
                }
            }

            // --- 第一部分：无条件特殊处理逻辑 ---
            // 无论 isCapturing 开关是否打开，我们都必须检查特定的数据包以实现核心功能。
            if (businessProcessingEnabled && inboundMsg instanceof FMLProxyPacket) {
                FMLProxyPacket fmlPacket = (FMLProxyPacket) inboundMsg;
                final String channel = fmlPacket.channel();
                final boolean packetTriggerListeners = hasPacketTriggerListeners();
                final boolean recentPacketTextFeedNeeded = isRecentPacketTextFeedNeeded();
                final boolean needsCapturedIdRules = CapturedIdRuleManager.hasEnabledRulesForChannel(channel, false);
                final boolean needsFieldRules = PacketFieldRuleManager.hasEnabledRulesForChannel(channel, false);
                final boolean shouldProcessFmlPacket = OWL_VIEW_CHANNEL.equals(channel)
                        || OWL_CONTROL_CHANNEL.equals(channel)
                        || packetTriggerListeners
                        || recentPacketTextFeedNeeded
                        || needsCapturedIdRules
                        || needsFieldRules;
                if (!shouldProcessFmlPacket) {
                    // 无规则、无等待、无触发器关注该 FML 包时，直接透传，避免在进服阶段无意义解码。
                } else {
                // 关键修复：必须在当前 Netty 线程里先拷贝 payload，
                // 不能把 fmlPacket/payload 直接丢到主线程任务里再读，避免 refCnt 已归零导致崩溃。
                ByteBuf payload = fmlPacket.payload();
                final byte[] rawData = new byte[payload.readableBytes()];
                payload.getBytes(payload.readerIndex(), rawData);

                enqueueBusinessTask(() -> {

                    final String decoded;
                    if ("OwlViewChannel".equals(channel)) {
                        storeIncomingOwlViewHex(rawData);
                        decoded = OwlViewPacketDecoder.decode(channel, rawData);
                        storeIncomingOwlViewDecoded(decoded);
                    } else if ("OwlControlChannel".equals(channel)) {
                        decoded = OwlViewPacketDecoder.decode(channel, rawData);
                        storeIncomingOwlViewDecoded(decoded);
                    } else {
                        decoded = null;
                    }
                    if (recentPacketTextFeedNeeded || packetTriggerListeners) {
                        storeRecentPacketText(channel, fmlPacket.getClass().getSimpleName(), decoded, rawData);
                    }

                    if (packetTriggerListeners) {
                        JsonObject triggerData = new JsonObject();
                        triggerData.addProperty("channel", channel);
                        triggerData.addProperty("packetClass", fmlPacket.getClass().getSimpleName());
                        triggerData.addProperty("direction", "inbound");
                        if (decoded != null) {
                            triggerData.addProperty("packet", decoded);
                            triggerData.addProperty("decoded", decoded);
                        }
                        NodeTriggerManager.trigger(NodeTriggerManager.TRIGGER_PACKET, triggerData);
                        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_PACKET,
                                triggerData);
                    }

                    // 异步处理规则匹配，避免主线程卡顿
                    final String finalDecoded = decoded;
                    final String finalChannel = channel;
                    final byte[] finalRawData = rawData;
                    if (needsCapturedIdRules || needsFieldRules) {
                        PACKET_PROCESS_EXECUTOR.execute(() -> {
                            try {
                                if (needsCapturedIdRules) {
                                    CapturedIdRuleManager.processPacket(finalChannel, false, finalRawData, finalDecoded);
                                }
                                if (needsFieldRules) {
                                    String decodedForRules = finalDecoded != null && !finalDecoded.trim().isEmpty()
                                            ? finalDecoded
                                            : decodePayloadFull(finalRawData);
                                    PacketFieldRuleManager.processPacket(finalChannel, false, finalRawData,
                                            decodedForRules,
                                            fmlPacket.getClass().getSimpleName());
                                }
                                requestRuleSyncOnMainThread();
                                requestSessionInitCheckOnMainThread();
                            } catch (Exception e) {
                                zszlScriptMod.LOGGER.error("[PacketCapture] 异步处理数据包失败: {}", finalChannel, e);
                            }
                        });
                    }
                });
                }
            } else if (businessProcessingEnabled && inboundMsg instanceof Packet) {
                Packet<?> inboundPacket = (Packet<?>) inboundMsg;
                try {
                    boolean hasTitleListener = LegacySequenceTriggerManager
                            .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_TITLE);
                    boolean hasActionbarListener = LegacySequenceTriggerManager
                            .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_ACTIONBAR);
                    boolean hasBossbarListener = LegacySequenceTriggerManager
                            .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_BOSSBAR);
                    boolean hasItemPickupListener = LegacySequenceTriggerManager
                            .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP);
                    if (inboundPacket instanceof SPacketTitle && (hasTitleListener || hasActionbarListener)) {
                        SPacketTitle titlePacket = (SPacketTitle) inboundPacket;
                        if (titlePacket.getMessage() != null) {
                            String text = titlePacket.getMessage().getUnformattedText();
                            if (text != null && !text.trim().isEmpty()) {
                                JsonObject triggerData = new JsonObject();
                                triggerData.addProperty("text", text);
                                triggerData.addProperty("type", String.valueOf(titlePacket.getType()));
                                if (titlePacket.getType() == SPacketTitle.Type.ACTIONBAR && hasActionbarListener) {
                                    LegacySequenceTriggerManager.triggerEvent(
                                            LegacySequenceTriggerManager.TRIGGER_ACTIONBAR, triggerData);
                                } else if (hasTitleListener) {
                                    LegacySequenceTriggerManager.triggerEvent(
                                            LegacySequenceTriggerManager.TRIGGER_TITLE, triggerData);
                                }
                            }
                        }
                    }
                    if (inboundPacket instanceof SPacketUpdateBossInfo && hasBossbarListener) {
                        SPacketUpdateBossInfo bossPacket = (SPacketUpdateBossInfo) inboundPacket;
                        if (bossPacket.getName() != null) {
                            String text = bossPacket.getName().getUnformattedText();
                            if (text != null && !text.trim().isEmpty()) {
                                JsonObject triggerData = new JsonObject();
                                triggerData.addProperty("text", text);
                                triggerData.addProperty("operation", String.valueOf(bossPacket.getOperation()));
                                LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_BOSSBAR,
                                        triggerData);
                            }
                        }
                    }
                    if (inboundPacket instanceof SPacketCollectItem && hasItemPickupListener) {
                        SPacketCollectItem collectPacket = (SPacketCollectItem) inboundPacket;
                        Minecraft mc = Minecraft.getMinecraft();
                        if (mc.player != null && collectPacket.getEntityID() == mc.player.getEntityId()) {
                            JsonObject triggerData = new JsonObject();
                            triggerData.addProperty("collectorEntityId", collectPacket.getEntityID());
                            triggerData.addProperty("itemEntityId", collectPacket.getCollectedItemEntityID());
                            triggerData.addProperty("count", 1);
                            if (mc.world != null) {
                                Entity entity = mc.world.getEntityByID(collectPacket.getCollectedItemEntityID());
                                if (entity instanceof EntityItem) {
                                    EntityItem itemEntity = (EntityItem) entity;
                                    if (itemEntity.getItem() != null) {
                                        triggerData.addProperty("itemName", itemEntity.getItem().getDisplayName());
                                        if (itemEntity.getItem().getItem() != null
                                                && itemEntity.getItem().getItem().getRegistryName() != null) {
                                            triggerData.addProperty("registryName",
                                                    String.valueOf(itemEntity.getItem().getItem().getRegistryName()));
                                        }
                                        triggerData.addProperty("count", Math.max(1, itemEntity.getItem().getCount()));
                                    }
                                }
                            }
                            LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP,
                                    triggerData);
                        }
                    }
                    if (!shouldSkipStandardBusinessPayload(inboundPacket)) {
                        boolean packetTriggerListeners = hasPacketTriggerListeners();
                        boolean recentPacketTextFeedNeeded = isRecentPacketTextFeedNeeded();
                        boolean needsCapturedIdRules = CapturedIdRuleManager.hasEnabledRulesForChannel("", false);
                        boolean needsFieldRules = PacketFieldRuleManager.hasEnabledRulesForChannel("", false);
                        boolean needsRawSnapshot = recentPacketTextFeedNeeded || needsCapturedIdRules || needsFieldRules;
                        if (packetTriggerListeners || needsRawSnapshot) {
                            byte[] rawData = null;
                            if (needsRawSnapshot) {
                                PacketBuffer rawBuffer = new PacketBuffer(Unpooled.buffer());
                                inboundPacket.writePacketData(rawBuffer);
                                rawData = new byte[rawBuffer.readableBytes()];
                                rawBuffer.readBytes(rawData);
                            }

                            if (recentPacketTextFeedNeeded && rawData != null) {
                                // 仅在路径序列确实需要等待“数据包文本”时才维护这份最近文本缓存。
                                storeRecentPacketText("N/A", inboundPacket.getClass().getSimpleName(), null, rawData);
                            }

                            if (packetTriggerListeners) {
                                JsonObject triggerData = new JsonObject();
                                triggerData.addProperty("channel", "N/A");
                                triggerData.addProperty("packetClass", inboundPacket.getClass().getSimpleName());
                                triggerData.addProperty("direction", "inbound");
                                triggerData.addProperty("packet", inboundPacket.getClass().getSimpleName());
                                NodeTriggerManager.trigger(NodeTriggerManager.TRIGGER_PACKET, triggerData);
                                LegacySequenceTriggerManager.triggerEvent(
                                        LegacySequenceTriggerManager.TRIGGER_PACKET, triggerData);
                            }

                            if ((needsCapturedIdRules || needsFieldRules) && rawData != null) {
                                final byte[] finalRawData = rawData;
                                PACKET_PROCESS_EXECUTOR.execute(() -> {
                                    try {
                                        if (needsCapturedIdRules) {
                                            CapturedIdRuleManager.processPacket("", false, finalRawData, null);
                                        }
                                        if (needsFieldRules) {
                                            PacketFieldRuleManager.processPacket("", false, finalRawData,
                                                    decodePayloadFull(finalRawData), inboundPacket.getClass().getSimpleName());
                                        }
                                        requestRuleSyncOnMainThread();
                                        requestSessionInitCheckOnMainThread();
                                    } catch (Exception e) {
                                        zszlScriptMod.LOGGER.error("[PacketCapture] 异步处理标准数据包失败", e);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // --- 第二部分：由用户开关控制的通用捕获逻辑 ---
            // 只有当用户在GUI中打开“行为捕获”且性能面板允许时，才执行通用的数据包记录功能
            if (inboundFeatureEnabled && isCapturing && inboundMsg instanceof Packet) {
                handlePacketCapture((Packet<?>) inboundMsg, false);
            }

            // 确保原始数据包继续在Netty管道中传递
            super.channelRead(ctx, inboundMsg);
        } finally {
            timer.stop();
        }
    }
    // --- 修改结束 ---

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        boolean outboundFeatureEnabled = PerformanceMonitor.isFeatureEnabled("packet_capture_outbound");
        boolean businessProcessingEnabled = isBusinessProcessingEnabled();
        PerformanceMonitor.PerformanceTimer timer = PerformanceMonitor.startTimer("packet_capture_outbound");
        try {
            // --- C->S 的特殊处理逻辑（GUI指纹识别），这部分已经是正确的，无需修改 ---
            if (businessProcessingEnabled && msg instanceof CPacketCloseWindow) {
                enqueueBusinessTask(() -> {
                    if (MailHelper.INSTANCE.isMailContextActive || MailHelper.INSTANCE.isFingerprintTicketValid) {
                        ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到 CPacketCloseWindow，重置邮件上下文，避免后续界面误注入按钮。");
                        MailHelper.INSTANCE.deactivateMailContext("CPacketCloseWindow");
                        MailHelper.INSTANCE.stopAutomation("收到 CPacketCloseWindow，关闭邮件相关窗口");
                    }
                });
            }

            if (businessProcessingEnabled && msg instanceof FMLProxyPacket) {
                FMLProxyPacket fmlPacket = (FMLProxyPacket) msg;
                String channel = fmlPacket.channel();
                boolean packetTriggerListeners = hasPacketTriggerListeners();
                boolean recentPacketTextFeedNeeded = isRecentPacketTextFeedNeeded();
                boolean needsCapturedIdRules = CapturedIdRuleManager.hasEnabledRulesForChannel(channel, true);
                boolean needsFieldRules = PacketFieldRuleManager.hasEnabledRulesForChannel(channel, true);
                boolean shouldProcessFmlPacket = OWL_VIEW_CHANNEL.equals(channel)
                        || OWL_CONTROL_CHANNEL.equals(channel)
                        || packetTriggerListeners
                        || recentPacketTextFeedNeeded
                        || needsCapturedIdRules
                        || needsFieldRules;
                if (!shouldProcessFmlPacket) {
                    // 无任何业务依赖时直接透传该 FML 包，避免无意义的出站解码。
                } else {

                ByteBuf outboundPayload = fmlPacket.payload();
                byte[] outboundData = new byte[outboundPayload.readableBytes()];
                outboundPayload.getBytes(outboundPayload.readerIndex(), outboundData);
                String outboundDecoded = null;
                if ("OwlViewChannel".equals(channel) || "OwlControlChannel".equals(channel)) {
                    outboundDecoded = OwlViewPacketDecoder.decode(channel, outboundData);
                }
                if (recentPacketTextFeedNeeded || packetTriggerListeners) {
                    storeRecentPacketText(channel, fmlPacket.getClass().getSimpleName(), outboundDecoded, outboundData);
                }
                if ("OwlViewChannel".equals(channel)) {
                    MailHelper.INSTANCE.onOutboundOwlViewPacket(channel, outboundData);
                }

                if (packetTriggerListeners) {
                    JsonObject triggerData = new JsonObject();
                    triggerData.addProperty("channel", channel);
                    triggerData.addProperty("packetClass", fmlPacket.getClass().getSimpleName());
                    triggerData.addProperty("direction", "outbound");
                    if (outboundDecoded != null) {
                        triggerData.addProperty("packet", outboundDecoded);
                        triggerData.addProperty("decoded", outboundDecoded);
                    }
                    NodeTriggerManager.trigger(NodeTriggerManager.TRIGGER_PACKET, triggerData);
                    LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_PACKET, triggerData);
                }

                // 异步处理规则匹配，避免主线程卡顿
                final String finalOutboundDecoded = outboundDecoded;
                final String finalChannel = channel;
                final byte[] finalOutboundData = outboundData;
                if (needsCapturedIdRules || needsFieldRules) {
                    PACKET_PROCESS_EXECUTOR.execute(() -> {
                        try {
                            if (needsCapturedIdRules) {
                                CapturedIdRuleManager.processPacket(finalChannel, true, finalOutboundData,
                                        finalOutboundDecoded);
                            }
                            if (needsFieldRules) {
                                PacketFieldRuleManager.processPacket(finalChannel, true, finalOutboundData,
                                        finalOutboundDecoded, fmlPacket.getClass().getSimpleName());
                            }
                            enqueueBusinessTask(() -> {
                                if (isSwitchLineConfirmClickPacket(finalChannel, finalOutboundData)) {
                                    resetOwlViewSessionID();
                                    MailHelper.INSTANCE.reset();
                                    ModConfig.debugPrint(DebugModule.MAIL_GUI,
                                            "检测到换线确定按键点击，已自动清空全部已捕获ID，等待新线路重新捕获。");
                                }
                            });
                            requestRuleSyncOnMainThread();
                            requestSessionInitCheckOnMainThread();
                        } catch (Exception e) {
                            zszlScriptMod.LOGGER.error("[PacketCapture] 异步处理出站数据包失败: {}", finalChannel, e);
                        }
                    });
                }
                }
            }

            // --- 通用捕获逻辑，由开关控制 ---
            if (outboundFeatureEnabled && isCapturing && msg instanceof Packet) {
                handlePacketCapture((Packet<?>) msg, true);
            }

            super.write(ctx, msg, promise);
        } finally {
            timer.stop();
        }
    }

    private static String decodePayload(byte[] data) {
        return PacketPayloadDecoder.decode(data);
    }

    private static String decodePayloadFull(byte[] data) {
        return PacketPayloadDecoder.decodeFull(data);
    }

    private void handlePacketCapture(Packet<?> packet, boolean isSent) {
        try {
            if (!shouldSampleCapture()) {
                droppedPacketCount++;
                return;
            }

            PendingPacketSnapshot snapshot = buildSnapshot(packet, isSent);
            if (snapshot == null) {
                return;
            }

            if (!shouldCapture(snapshot)) {
                return;
            }

            if (pendingCaptureQueue.size() >= MAX_CAPTURE_QUEUE) {
                pendingCaptureQueue.poll();
                droppedPacketCount++;
                long now = System.currentTimeMillis();
                if (now - lastCaptureDropWarnAt > 3000L) {
                    lastCaptureDropWarnAt = now;
                    zszlScriptMod.LOGGER.warn("[PacketCapture] 捕获流量过高，已丢弃最旧待处理包以防止卡顿。queue={}",
                            pendingCaptureQueue.size());
                }
            }

            pendingCaptureQueue.offer(snapshot);
            lastKnownCaptureQueueSize = pendingCaptureQueue.size();
            scheduleDrainCaptureQueue();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("捕获并序列化数据包时出错: " + packet.getClass().getName(), e);
            if (Minecraft.getMinecraft().player != null) {
                String packetSimpleName = packet.getClass().getSimpleName();
                // SPacketSpawnPlayer 在部分场景下可能无法稳定序列化，避免在聊天栏刷屏干扰用户。
                if (!"SPacketSpawnPlayer".equals(packetSimpleName)) {
                    String errorMessage = "§c[数据包捕获失败] " + packetSimpleName;
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString(errorMessage));
                }
            }
        }
    }

    private PendingPacketSnapshot buildSnapshot(Packet<?> packet, boolean isSent) throws Exception {
        String packetClassName = packet.getClass().getSimpleName();
        String channel = "N/A";
        byte[] rawData;
        boolean isFml = packet instanceof FMLProxyPacket;
        Integer packetId = null;

        if (isFml) {
            FMLProxyPacket fmlPacket = (FMLProxyPacket) packet;
            channel = fmlPacket.channel();
            ByteBuf payload = fmlPacket.payload();
            rawData = new byte[payload.readableBytes()];
            payload.getBytes(payload.readerIndex(), rawData);
        } else {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            EnumPacketDirection direction = isSent ? EnumPacketDirection.SERVERBOUND : EnumPacketDirection.CLIENTBOUND;
            packetId = EnumConnectionState.PLAY.getPacketId(direction, packet);
            packet.writePacketData(buffer);
            rawData = new byte[buffer.readableBytes()];
            buffer.readBytes(rawData);
        }

        return new PendingPacketSnapshot(packetClassName, isFml, packetId, channel, rawData, isSent);
    }

    private boolean shouldCapture(PendingPacketSnapshot snapshot) {
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        if (config == null) {
            return true;
        }

        if (config.captureMode == CaptureMode.WHITELIST) {
            if (config.whitelistFilters == null || config.whitelistFilters.isEmpty()) {
                return true;
            }
            for (String filter : config.whitelistFilters) {
                if (packetMatchesFilter(snapshot, filter)) {
                    return true;
                }
            }
            return false;
        }

        if (config.blacklistFilters == null || config.blacklistFilters.isEmpty()) {
            return true;
        }
        for (String filter : config.blacklistFilters) {
            if (packetMatchesFilter(snapshot, filter)) {
                return false;
            }
        }
        return true;
    }

    private boolean packetMatchesFilter(PendingPacketSnapshot snapshot, String keyword) {
        if (snapshot == null || keyword == null || keyword.trim().isEmpty()) {
            return false;
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        String lowerKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);

        String packetClassLower = safeLower(snapshot.packetClassName);
        String packetChannelLower = safeLower(snapshot.channel);
        String packetIdHexLower = snapshot.packetId == null ? "" : String.format("0x%02x", snapshot.packetId);
        String packetIdDecLower = snapshot.packetId == null ? "" : String.valueOf(snapshot.packetId);

        if (isRegexKeyword(keyword)) {
            return packetMatchesRegex(extractRegexPattern(keyword), packetClassLower, packetChannelLower,
                    packetIdHexLower, packetIdDecLower);
        }

        if (packetClassLower.contains(lowerKeyword) || packetChannelLower.contains(lowerKeyword)
                || packetIdHexLower.contains(lowerKeyword) || packetIdDecLower.contains(lowerKeyword)) {
            return true;
        }

        if (requiresDeepPayloadInspection(lowerKeyword, normalizedKeyword)) {
            String packetHexNoSpace = ByteBufUtil.hexDump(snapshot.rawData == null ? new byte[0] : snapshot.rawData)
                    .toLowerCase(Locale.ROOT);
            String cleanedHexKeyword = normalizeHexText(lowerKeyword);
            if (!cleanedHexKeyword.isEmpty() && lowerKeyword.matches("^[0-9a-f\\s,:]+$")
                    && packetHexNoSpace.contains(cleanedHexKeyword)) {
                return true;
            }

            if (containsNonAscii(normalizedKeyword)) {
                String utf8Hex = toUtf8HexNoSpace(normalizedKeyword);
                if (!utf8Hex.isEmpty() && packetHexNoSpace.contains(utf8Hex)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean requiresDeepPayloadInspection(String lowerKeyword, String normalizedKeyword) {
        if (lowerKeyword == null || normalizedKeyword == null) {
            return false;
        }
        return lowerKeyword.matches("^[0-9a-f\\s,:]+$") || containsNonAscii(normalizedKeyword);
    }

    private boolean isRegexKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }
        String text = keyword.trim();
        return text.startsWith("re:") || (text.startsWith("/") && text.endsWith("/") && text.length() > 2);
    }

    private String extractRegexPattern(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        if (text.startsWith("re:")) {
            return text.substring(3);
        }
        if (text.startsWith("/") && text.endsWith("/") && text.length() > 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private boolean packetMatchesRegex(String regex, String... haystacks) {
        if (regex == null || regex.isEmpty()) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            for (String hay : haystacks) {
                if (hay != null && pattern.matcher(hay).find()) {
                    return true;
                }
            }
            return false;
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    private boolean containsNonAscii(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }

    private String toUtf8HexNoSpace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private void scheduleDrainCaptureQueue() {
        if (!captureDrainScheduled.compareAndSet(false, true)) {
            return;
        }
        Minecraft.getMinecraft().addScheduledTask(this::drainCaptureQueueOnMainThread);
    }

    private void drainCaptureQueueOnMainThread() {
        captureDrainScheduled.set(false);

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            pendingCaptureQueue.clear();
            lastKnownCaptureQueueSize = 0;
            return;
        }

        int processed = 0;
        int processedBytes = 0;
        long startNanos = System.nanoTime();
        while (processed < MAX_CAPTURE_PROCESS_PER_TICK) {
            if (processedBytes >= MAX_CAPTURE_PROCESS_BYTES_PER_TICK) {
                break;
            }
            if (System.nanoTime() - startNanos >= MAX_CAPTURE_PROCESS_NANOS_PER_TICK) {
                break;
            }

            PendingPacketSnapshot snapshot = pendingCaptureQueue.poll();
            if (snapshot == null) {
                break;
            }

            CapturedPacketData packetData = new CapturedPacketData(snapshot.packetClassName, snapshot.isFmlPacket,
                    snapshot.packetId, snapshot.channel, snapshot.rawData, null);

            if (snapshot.isSent) {
                appendCapturedPacket(capturedPackets, packetData);
            } else {
                appendCapturedPacket(capturedReceivedPackets, packetData);
            }
            PacketIdRecordManager.recordCapturedPacket(snapshot.isSent, packetData);

            processed++;
            processedBytes += snapshot.rawData == null ? 0 : snapshot.rawData.length;
        }

        if (!pendingCaptureQueue.isEmpty()) {
            lastKnownCaptureQueueSize = pendingCaptureQueue.size();
            scheduleDrainCaptureQueue();
        } else {
            lastKnownCaptureQueueSize = 0;
        }
    }

    private static void appendCapturedPacket(List<CapturedPacketData> target, CapturedPacketData data) {
        synchronized (target) {
            int limit = resolveMaxCapturedPackets();
            if (!target.isEmpty()) {
                CapturedPacketData last = target.get(target.size() - 1);
                if (last.canAggregate(data)) {
                    last.mergeFrom(data);
                } else {
                    target.add(data);
                }
            } else {
                target.add(data);
            }
            if (target.size() > limit + CAPTURE_TRIM_BATCH) {
                int toRemove = target.size() - limit;
                target.subList(0, toRemove).clear();
            }
        }
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private String normalizeKeyword(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u00A0', ' ').trim();
    }

    private String normalizeHexText(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");
    }

    private static int resolveMaxCapturedPackets() {
        int fallback = MAX_CAPTURED_PACKETS;
        PacketFilterConfig cfg = PacketFilterConfig.INSTANCE;
        if (cfg == null) {
            return fallback;
        }
        int value = cfg.maxCapturedPackets;
        if (value <= 0) {
            return fallback;
        }
        if (value < 100) {
            return 100;
        }
        if (value > 50000) {
            return 50000;
        }
        return value;
    }

    private static void storeIncomingOwlViewDecoded(String decoded) {
        if (decoded == null)
            return;
        synchronized (recentOwlViewDecoded) {
            recentOwlViewDecoded.add(decoded);
            while (recentOwlViewDecoded.size() > MAX_RECENT_OWLVIEW_HEX) {
                recentOwlViewDecoded.remove(0);
            }
        }
    }

    private static void storeRecentPacketText(String channel, String packetClassName, String decodedText,
            byte[] rawData) {
        StringBuilder sb = new StringBuilder();
        if (packetClassName != null && !packetClassName.trim().isEmpty()) {
            sb.append(packetClassName.trim());
        }
        if (channel != null && !channel.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(channel.trim());
        }
        if (decodedText != null && !decodedText.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(decodedText.trim());
        } else {
            String fallbackDecoded = shouldSkipRecentTextPayloadDecode(packetClassName, rawData)
                    ? ""
                    : decodePayload(rawData);
            if (fallbackDecoded != null && !fallbackDecoded.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append(fallbackDecoded.trim());
            }
        }

        String packetText = sb.toString().trim();
        if (packetText.isEmpty()) {
            return;
        }

        synchronized (recentPacketTexts) {
            recentPacketTexts.add(packetText);
            while (recentPacketTexts.size() > MAX_RECENT_PACKET_TEXTS) {
                recentPacketTexts.remove(0);
            }
            recentPacketTextVersion++;
        }
    }

    private static boolean shouldSkipStandardBusinessPayload(Packet<?> packet) {
        if (packet == null) {
            return true;
        }
        String packetClassName = packet.getClass().getSimpleName();
        return "SPacketSpawnPlayer".equals(packetClassName)
                || "SPacketSpawnMob".equals(packetClassName)
                || isWorldStreamingPacket(packetClassName);
    }

    private static boolean shouldSkipRecentTextPayloadDecode(String packetClassName, byte[] rawData) {
        if (rawData == null || rawData.length == 0) {
            return true;
        }
        return rawData.length > MAX_RECENT_TEXT_DECODE_BYTES || isWorldStreamingPacket(packetClassName);
    }

    private static boolean isWorldStreamingPacket(String packetClassName) {
        String name = packetClassName == null ? "" : packetClassName.toLowerCase(Locale.ROOT);
        return name.contains("chunk")
                || name.contains("lightupdate")
                || name.contains("levelchunk")
                || name.contains("forgetlevelchunk")
                || name.contains("chunksbiomes");
    }

    public static List<String> getRecentOwlViewDecodedSnapshot() {
        synchronized (recentOwlViewDecoded) {
            return new ArrayList<>(recentOwlViewDecoded);
        }
    }

    public static void clearRecentOwlViewDecoded() {
        synchronized (recentOwlViewDecoded) {
            recentOwlViewDecoded.clear();
        }
    }

}
