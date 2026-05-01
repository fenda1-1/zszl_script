package com.zszl.zszlScriptMod.utils;

import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.PerformanceMonitor;
import com.zszl.zszlScriptMod.gui.packet.InputTimelineManager;
import com.zszl.zszlScriptMod.gui.packet.PacketFilterConfig;
import com.zszl.zszlScriptMod.gui.packet.PacketIdRecordManager;
import com.zszl.zszlScriptMod.path.node.NodeTriggerManager;
import com.zszl.zszlScriptMod.path.trigger.LegacySequenceTriggerManager;
import com.zszl.zszlScriptMod.zszlScriptMod;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PacketCaptureHandler extends ChannelDuplexHandler {

    private static final String HANDLER_NAME = "keycommand_packet_handler";
    private static final String OWL_VIEW_CHANNEL = "OwlViewChannel";
    private static final String OWL_CONTROL_CHANNEL = "OwlControlChannel";
    private static final int MAX_RECENT_ITEMS = 200;
    private static final long AGGREGATE_WINDOW_MS = 500L;
    private static final int MAX_CAPTURE_QUEUE = 6000;
    private static final int MAX_CAPTURE_PROCESS_PER_TICK = 48;
    private static final int MAX_CAPTURE_PROCESS_BYTES_PER_TICK = 256 * 1024;
    private static final long MAX_CAPTURE_PROCESS_NANOS_PER_TICK = 2_000_000L;
    private static final int MAX_BUSINESS_TASKS_PER_TICK = 64;
    private static final long MAX_BUSINESS_TASK_NANOS_PER_TICK = 1_500_000L;
    private static final int DEFAULT_MAX_CAPTURED_PACKETS = 3000;
    private static final int CAPTURE_TRIM_BATCH = 120;
    private static final int MAX_RECENT_TEXT_DECODE_BYTES = 64 * 1024;
    private static final long UI_SNAPSHOT_INTERVAL_MS = 500L;
    private static final long RECENT_PACKET_TEXT_TRACKING_GRACE_MS = 15000L;
    private static volatile long lastUiSnapshotAt = 0L;
    private static volatile boolean currentConnectionInjected = false;
    private static volatile long recentPacketTextTrackingUntilMs = 0L;
    private static volatile Connection lastInjectedConnection = null;

    private final ConcurrentLinkedQueue<PendingPacketSnapshot> pendingCaptureQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean captureDrainScheduled = new AtomicBoolean(false);
    private volatile long lastCaptureDropWarnAt = 0L;

    private static final ConcurrentLinkedQueue<Runnable> pendingBusinessTasks = new ConcurrentLinkedQueue<>();
    private static final Map<Class<?>, Field> CHANNEL_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> ENCODE_FAILURE_LOGGED = new ConcurrentHashMap<>();

    public enum CaptureMode {
        BLACKLIST,
        WHITELIST;

        public CaptureMode next() {
            return this == BLACKLIST ? WHITELIST : BLACKLIST;
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
        private long lastTimestamp;
        private int occurrenceCount;
        private int totalPayloadBytes;

        public CapturedPacketData(long timestamp, String packetClassName, boolean isFmlPacket, Integer packetId,
                String channel, byte[] rawData, String decodedData) {
            this.timestamp = timestamp;
            this.packetClassName = packetClassName == null ? "" : packetClassName;
            this.isFmlPacket = isFmlPacket;
            this.packetId = packetId;
            this.channel = channel == null ? "" : channel;
            this.rawData = rawData == null ? new byte[0] : rawData;
            this.decodedData = decodedData == null ? null : decodedData;
            this.lastTimestamp = timestamp;
            this.occurrenceCount = 1;
            this.totalPayloadBytes = this.rawData.length;
        }

        public CapturedPacketData(String packetClassName, boolean isFmlPacket, Integer packetId, String channel,
                byte[] rawData, String decodedData) {
            this(System.currentTimeMillis(), packetClassName, isFmlPacket, packetId, channel, rawData, decodedData);
        }

        public String getHexData() {
            String local = hexData;
            if (local == null) {
                local = bytesToHex(rawData);
                hexData = local == null ? "" : local;
            }
            return hexData == null ? "" : hexData;
        }

        public String getDecodedData() {
            String local = decodedData;
            if (local == null) {
                local = decodePayload(channel, rawData);
                decodedData = local == null ? "" : local;
            }
            return decodedData == null ? "" : decodedData;
        }

        public String getDecodedDetailData() {
            String local = decodedDetailData;
            if (local == null) {
                local = PacketPayloadDecoder.decodeDetailed(rawData);
                if (local == null || local.trim().isEmpty()) {
                    local = getDecodedData();
                }
                decodedDetailData = local == null ? "" : local;
            }
            return decodedDetailData == null ? "" : decodedDetailData;
        }

        public String getDecodedFullData() {
            String local = decodedFullData;
            if (local == null) {
                local = PacketPayloadDecoder.decodeFull(rawData);
                if ((local == null || local.trim().isEmpty()) && decodedData != null) {
                    local = decodedData;
                }
                decodedFullData = local == null ? "" : local;
            }
            return decodedFullData == null ? "" : decodedFullData;
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

        public int getPayloadSize() {
            return rawData.length;
        }

        public boolean isAggregated() {
            return occurrenceCount > 1;
        }

        public void restoreAggregateState(long restoredLastTimestamp, int restoredOccurrenceCount,
                int restoredTotalPayloadBytes) {
            this.lastTimestamp = Math.max(timestamp, restoredLastTimestamp);
            this.occurrenceCount = Math.max(1, restoredOccurrenceCount);
            this.totalPayloadBytes = Math.max(rawData.length, restoredTotalPayloadBytes);
        }

        private boolean canAggregate(CapturedPacketData other) {
            return other != null
                    && Math.abs(other.timestamp - lastTimestamp) <= AGGREGATE_WINDOW_MS
                    && isFmlPacket == other.isFmlPacket
                    && safe(packetClassName).equalsIgnoreCase(safe(other.packetClassName))
                    && safe(channel).equalsIgnoreCase(safe(other.channel))
                    && ((packetId == null && other.packetId == null)
                            || (packetId != null && packetId.equals(other.packetId)))
                    && Arrays.equals(rawData, other.rawData);
        }

        private void mergeFrom(CapturedPacketData other) {
            this.lastTimestamp = Math.max(this.lastTimestamp, other.lastTimestamp);
            this.occurrenceCount += other.occurrenceCount;
            this.totalPayloadBytes += other.totalPayloadBytes;
        }
    }

    public static class PacketCaptureUiSnapshot {
        public final int sentCount;
        public final int receivedCount;
        public final int queueSize;
        public final long droppedCount;
        public final int samplingModulo;
        public final boolean businessPacketProcessingEnabled;
        public final boolean capturingEnabled;
        public final long timestamp;

        public PacketCaptureUiSnapshot(int sentCount, int receivedCount, int queueSize, long droppedCount,
                int samplingModulo, boolean businessPacketProcessingEnabled, boolean capturingEnabled, long timestamp) {
            this.sentCount = sentCount;
            this.receivedCount = receivedCount;
            this.queueSize = queueSize;
            this.droppedCount = droppedCount;
            this.samplingModulo = samplingModulo;
            this.businessPacketProcessingEnabled = businessPacketProcessingEnabled;
            this.capturingEnabled = capturingEnabled;
            this.timestamp = timestamp;
        }
    }

    private static final class PendingPacketSnapshot {
        private final String packetClassName;
        private final boolean isFmlPacket;
        private final Integer packetId;
        private final String channel;
        private final byte[] rawData;
        private final String decodedText;
        private final boolean sent;

        private PendingPacketSnapshot(String packetClassName, boolean isFmlPacket, Integer packetId, String channel,
                byte[] rawData, String decodedText, boolean sent) {
            this.packetClassName = safe(packetClassName);
            this.isFmlPacket = isFmlPacket;
            this.packetId = packetId;
            this.channel = safe(channel);
            this.rawData = rawData == null ? new byte[0] : rawData;
            this.decodedText = safe(decodedText);
            this.sent = sent;
        }
    }

    private static final class BossBarTriggerSnapshot {
        private final String text;
        private final String operation;

        private BossBarTriggerSnapshot(String text, String operation) {
            this.text = safe(text);
            this.operation = safe(operation);
        }
    }

    public static boolean isCapturing = false;
    public static final List<CapturedPacketData> capturedPackets = Collections.synchronizedList(new ArrayList<>());
    public static final List<CapturedPacketData> capturedReceivedPackets = Collections.synchronizedList(new ArrayList<>());

    private static final List<String> recentOwlViewIncomingHex = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> recentOwlViewDecoded = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> recentPacketTexts = Collections.synchronizedList(new ArrayList<>());
    private static volatile long packetTextVersion = 0L;
    private static volatile int lastKnownCaptureQueueSize = 0;
    private static volatile long sampledPacketCount = 0L;
    private static volatile long droppedPacketCount = 0L;
    private static volatile int activeSamplingModulo = 1;
    private static volatile PacketCaptureUiSnapshot lastUiSnapshot = new PacketCaptureUiSnapshot(0, 0, 0, 0, 1,
            true, false, System.currentTimeMillis());

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            injectIntoCurrentConnection();
        } else {
            currentConnectionInjected = false;
            lastInjectedConnection = null;
            lastKnownCaptureQueueSize = 0;
        }

        drainBusinessTasksOnMainThread();

        long now = System.currentTimeMillis();
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        if (now - lastUiSnapshotAt >= UI_SNAPSHOT_INTERVAL_MS) {
            lastUiSnapshotAt = now;
            lastUiSnapshot = new PacketCaptureUiSnapshot(
                    capturedPackets.size(),
                    capturedReceivedPackets.size(),
                    lastKnownCaptureQueueSize,
                    droppedPacketCount,
                    activeSamplingModulo,
                    config == null || config.enableBusinessPacketProcessing,
                    isCapturing,
                    now);
        }
    }

    public static void clearAllPackets() {
        capturedPackets.clear();
        capturedReceivedPackets.clear();
        clearRecentOwlViewIncomingHex();
        clearRecentOwlViewDecoded();
        clearRecentPacketTexts();
        lastKnownCaptureQueueSize = 0;
        sampledPacketCount = 0L;
        droppedPacketCount = 0L;
        activeSamplingModulo = 1;
        lastUiSnapshotAt = 0L;
        lastUiSnapshot = new PacketCaptureUiSnapshot(0, 0, 0, 0, 1, true, false, System.currentTimeMillis());
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
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        long now = System.currentTimeMillis();
        if (now - lastUiSnapshotAt >= UI_SNAPSHOT_INTERVAL_MS) {
            lastUiSnapshotAt = now;
            lastUiSnapshot = new PacketCaptureUiSnapshot(
                    capturedPackets.size(),
                    capturedReceivedPackets.size(),
                    lastKnownCaptureQueueSize,
                    droppedPacketCount,
                    activeSamplingModulo,
                    config == null || config.enableBusinessPacketProcessing,
                    isCapturing,
                    now);
        }
        return lastUiSnapshot;
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
        requestRecentPacketTextTracking();
        synchronized (recentPacketTexts) {
            return new ArrayList<>(recentPacketTexts);
        }
    }

    public static long getRecentPacketTextVersion() {
        return packetTextVersion;
    }

    public static void clearRecentPacketTexts() {
        synchronized (recentPacketTexts) {
            recentPacketTexts.clear();
        }
        packetTextVersion++;
    }

    public static void requestRecentPacketTextTracking() {
        requestRecentPacketTextTracking(RECENT_PACKET_TEXT_TRACKING_GRACE_MS);
    }

    public static void requestRecentPacketTextTracking(long durationMs) {
        long expiresAt = System.currentTimeMillis() + Math.max(1000L, durationMs);
        if (expiresAt > recentPacketTextTrackingUntilMs) {
            recentPacketTextTrackingUntilMs = expiresAt;
        }
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

    public static void injectIntoCurrentConnection() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener listener = mc.getConnection();
        if (listener == null) {
            currentConnectionInjected = false;
            lastInjectedConnection = null;
            return;
        }
        try {
            Connection connection = listener.getConnection();
            if (connection == null) {
                currentConnectionInjected = false;
                lastInjectedConnection = null;
                return;
            }
            if (currentConnectionInjected && connection == lastInjectedConnection) {
                return;
            }
            Channel channel = resolveChannel(connection);
            if (channel == null || channel.pipeline() == null) {
                currentConnectionInjected = false;
                lastInjectedConnection = null;
                return;
            }
            if (channel.pipeline().get(HANDLER_NAME) instanceof PacketCaptureHandler) {
                currentConnectionInjected = true;
                lastInjectedConnection = connection;
                return;
            }
            if (channel.pipeline().get(HANDLER_NAME) != null) {
                channel.pipeline().remove(HANDLER_NAME);
            }
            if (channel.pipeline().get("packet_handler") != null) {
                channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new PacketCaptureHandler());
            } else {
                channel.pipeline().addLast(HANDLER_NAME, new PacketCaptureHandler());
            }
            currentConnectionInjected = true;
            lastInjectedConnection = connection;
        } catch (Exception e) {
            currentConnectionInjected = false;
            lastInjectedConnection = null;
            zszlScriptMod.LOGGER.error("注入 PacketCaptureHandler 失败", e);
        }
    }

    private static Channel resolveChannel(Connection connection) {
        if (connection == null) {
            return null;
        }
        Class<?> connectionClass = connection.getClass();
        Field cachedField = CHANNEL_FIELD_CACHE.get(connectionClass);
        if (cachedField != null) {
            try {
                Object value = cachedField.get(connection);
                if (value instanceof Channel) {
                    return (Channel) value;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        for (Class<?> type = connection.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Channel.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(connection);
                    if (value instanceof Channel) {
                        CHANNEL_FIELD_CACHE.put(connectionClass, field);
                        return (Channel) value;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return null;
    }

    public static void recordSyntheticPacket(boolean sent, String packetClassName, boolean isFmlPacket, Integer packetId,
            String channel, byte[] rawData) {
        if (rawData == null) {
            rawData = new byte[0];
        }
        String decodedText = processPacket(sent, packetClassName, isFmlPacket, packetId, channel, rawData,
                isDecodedTextNeeded(sent), shouldTrackRecentPacketTexts());
        if (!isCapturing) {
            return;
        }
        PacketMeta meta = new PacketMeta(packetClassName, isFmlPacket, packetId, channel, rawData);
        if (!shouldCapture(meta)) {
            return;
        }
        appendCaptured(sent, new CapturedPacketData(packetClassName, isFmlPacket, packetId, channel, rawData, decodedText));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        PerformanceMonitor.PerformanceTimer timer = PerformanceMonitor.startTimer("packet_capture_outbound");
        try {
            if (msg instanceof Packet<?>) {
                boolean captureFeatureEnabled = PerformanceMonitor.isFeatureEnabled("packet_capture_outbound");
                boolean needRawPacketProcessing = isRawPacketProcessingNeeded(true, captureFeatureEnabled, false);
                if (needRawPacketProcessing) {
                    Packet<?> packet = (Packet<?>) msg;
                    PacketMeta meta = inspectPacket(packet, true);
                    String decodedText = processPacket(true, meta.packetClassName, meta.isFmlPacket, meta.packetId,
                            meta.channel, meta.rawData, isDecodedTextNeeded(true), shouldTrackRecentPacketTexts());
                    handlePacketCapture(meta, decodedText, true, captureFeatureEnabled);

                }
            }
            super.write(ctx, msg, promise);
        } finally {
            timer.stop();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        PerformanceMonitor.PerformanceTimer timer = PerformanceMonitor.startTimer("packet_capture_inbound");
        try {
            Object forward = msg;
            PacketMeta meta = null;
            Packet<?> effectivePacket = null;

            if (msg instanceof Packet<?>) {
                boolean captureFeatureEnabled = PerformanceMonitor.isFeatureEnabled("packet_capture_inbound");
                boolean interceptEnabled = PerformanceMonitor.isFeatureEnabled("packet_intercept");
                boolean needRawPacketProcessing = isRawPacketProcessingNeeded(false, captureFeatureEnabled,
                        interceptEnabled);
                effectivePacket = (Packet<?>) msg;
                if (needRawPacketProcessing
                        && !(captureFeatureEnabled && isCapturing)
                        && !interceptEnabled
                        && shouldSkipBusinessPayload(effectivePacket)) {
                    needRawPacketProcessing = false;
                }
                String decodedText = "";
                if (needRawPacketProcessing) {
                    meta = inspectPacket(effectivePacket, false);
                    if (interceptEnabled && meta.rawData.length > 0) {
                        PacketInterceptManager.InterceptResult intercept = PacketInterceptManager
                                .applyInboundRules(new PacketInterceptManager.PacketMeta(meta.channel,
                                        meta.packetClassName,
                                        meta.packetId), meta.rawData);
                        if (intercept.modified && intercept.payload != null) {
                            Packet<?> rebuilt = rebuildInboundPacket(effectivePacket, meta, intercept.payload);
                            if (rebuilt != null) {
                                forward = rebuilt;
                                effectivePacket = rebuilt;
                                meta = new PacketMeta(meta.packetClassName, meta.isFmlPacket, meta.packetId,
                                        meta.channel,
                                        intercept.payload);
                            }
                        }
                    }

                    decodedText = processPacket(false, meta.packetClassName, meta.isFmlPacket, meta.packetId,
                            meta.channel, meta.rawData, isDecodedTextNeeded(false), shouldTrackRecentPacketTexts());
                    handlePacketCapture(meta, decodedText, false, captureFeatureEnabled);
                }
                dispatchInboundTypedTriggers(effectivePacket);
            }

            super.channelRead(ctx, forward);
        } finally {
            timer.stop();
        }
    }

    private static Packet<?> rebuildInboundPacket(Packet<?> originalPacket, PacketMeta meta, byte[] rawData) {
        if (meta == null) {
            return null;
        }
        try {
            if (meta.isFmlPacket && originalPacket instanceof ClientboundCustomPayloadPacket) {
                Identifier identifier = ModUtils.ensureCustomPayloadIdentifier(
                        ((ClientboundCustomPayloadPacket) originalPacket).payload().type().id(), meta.channel);
                return PacketCodecCompat.clientboundCustomPayload(identifier, rawData);
            }
            if (meta.packetId != null) {
                return PacketCodecCompat.decodeStandardPacket(meta.packetId, rawData, false);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.warn("重建入站数据包失败: {}", meta.packetClassName, e);
        }
        return null;
    }

    private static String processPacket(boolean sent, String packetClassName, boolean isFmlPacket, Integer packetId,
            String channel, byte[] rawData, boolean decodePayloadText, boolean collectRecentPacketText) {
        byte[] safeRawData = rawData == null ? new byte[0] : rawData;
        boolean skipPayloadDecode = shouldSkipPayloadDecode(packetClassName, safeRawData);
        String decodedText = decodePayloadText && !skipPayloadDecode ? decodePayload(channel, safeRawData) : "";
        if (collectRecentPacketText && !skipPayloadDecode) {
            appendRecentPacketText(packetClassName, channel, decodedText);
        }

        if (!sent && OWL_VIEW_CHANNEL.equalsIgnoreCase(channel)) {
            appendRecent(recentOwlViewIncomingHex, bytesToHex(safeRawData));
            if (!decodedText.isEmpty()) {
                appendRecent(recentOwlViewDecoded, decodedText);
            }
        }

        if (isBusinessProcessingEnabled() && !isWorldStreamingPacket(packetClassName)) {
            if (hasPacketTriggerConsumers()) {
                dispatchPacketTrigger(packetClassName, packetId, channel, decodedText, sent);
            }
            if (CapturedIdRuleManager.hasEnabledRulesForDirection(sent)) {
                CapturedIdRuleManager.processPacket(channel, sent, safeRawData, decodedText);
            }
            if (PacketFieldRuleManager.hasEnabledRulesForDirection(sent)) {
                PacketFieldRuleManager.processPacket(channel, sent, safeRawData, decodedText, packetClassName);
            }
        }
        return decodedText;
    }

    private static void dispatchPacketTrigger(String packetClassName, Integer packetId, String channel,
            String decodedText, boolean sent) {
        boolean hasNodeTrigger = NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_PACKET);
        boolean hasLegacyTrigger = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_PACKET);
        if (!hasNodeTrigger && !hasLegacyTrigger) {
            return;
        }

        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("channel", safe(channel));
        triggerData.addProperty("packetClass", safe(packetClassName));
        triggerData.addProperty("direction", sent ? "outbound" : "inbound");
        if (packetId != null) {
            triggerData.addProperty("packetId", packetId);
            triggerData.addProperty("packetIdHex", String.format("0x%02X", packetId));
        }

        String packetText = safe(decodedText).trim();
        if (packetText.isEmpty()) {
            packetText = safe(packetClassName);
        }
        triggerData.addProperty("packet", packetText);
        if (!safe(decodedText).trim().isEmpty()) {
            triggerData.addProperty("decoded", decodedText.trim());
        }

        enqueueBusinessTask(() -> {
            if (hasNodeTrigger) {
                NodeTriggerManager.trigger(NodeTriggerManager.TRIGGER_PACKET, triggerData);
            }
            if (hasLegacyTrigger) {
                LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_PACKET, triggerData);
            }
        });
    }

    private static void dispatchInboundTypedTriggers(Packet<?> packet) {
        if (!isBusinessProcessingEnabled() || packet == null) {
            return;
        }

        boolean hasTitleListener = LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_TITLE);
        boolean hasActionbarListener = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_ACTIONBAR);
        boolean hasBossbarListener = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_BOSSBAR);
        boolean hasItemPickupListener = LegacySequenceTriggerManager
                .hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP);

        if (!hasTitleListener && !hasActionbarListener && !hasBossbarListener && !hasItemPickupListener) {
            return;
        }

        enqueueBusinessTask(() -> {
            if (packet instanceof ClientboundSetTitleTextPacket && hasTitleListener) {
                String text = componentToPlainText(((ClientboundSetTitleTextPacket) packet).text());
                if (!text.isEmpty()) {
                    JsonObject triggerData = new JsonObject();
                    triggerData.addProperty("text", text);
                    triggerData.addProperty("type", "TITLE");
                    LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_TITLE, triggerData);
                }
            }

            if (packet instanceof ClientboundSetActionBarTextPacket && hasActionbarListener) {
                String text = componentToPlainText(((ClientboundSetActionBarTextPacket) packet).text());
                if (!text.isEmpty()) {
                    JsonObject triggerData = new JsonObject();
                    triggerData.addProperty("text", text);
                    triggerData.addProperty("type", "ACTIONBAR");
                    LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_ACTIONBAR, triggerData);
                }
            }

            if (packet instanceof ClientboundBossEventPacket && hasBossbarListener) {
                BossBarTriggerSnapshot snapshot = extractBossBarSnapshot((ClientboundBossEventPacket) packet);
                if (!snapshot.text.isEmpty()) {
                    JsonObject triggerData = new JsonObject();
                    triggerData.addProperty("text", snapshot.text);
                    triggerData.addProperty("operation", snapshot.operation);
                    LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_BOSSBAR, triggerData);
                }
            }

            if (packet instanceof ClientboundTakeItemEntityPacket && hasItemPickupListener) {
                dispatchItemPickupTrigger((ClientboundTakeItemEntityPacket) packet);
            }
        });
    }

    private static BossBarTriggerSnapshot extractBossBarSnapshot(ClientboundBossEventPacket packet) {
        if (packet == null) {
            return new BossBarTriggerSnapshot("", "");
        }

        final String[] text = new String[] { "" };
        final String[] operation = new String[] { "" };
        packet.dispatch(new ClientboundBossEventPacket.Handler() {
            @Override
            public void add(UUID id, Component name, float progress, BossEvent.BossBarColor color,
                    BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playMusic, boolean createFog) {
                operation[0] = "ADD";
                text[0] = componentToPlainText(name);
            }

            @Override
            public void remove(UUID id) {
                operation[0] = "REMOVE";
            }

            @Override
            public void updateProgress(UUID id, float progress) {
                operation[0] = "UPDATE_PROGRESS";
            }

            @Override
            public void updateName(UUID id, Component name) {
                operation[0] = "UPDATE_NAME";
                text[0] = componentToPlainText(name);
            }

            @Override
            public void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
                operation[0] = "UPDATE_STYLE";
            }

            @Override
            public void updateProperties(UUID id, boolean darkenScreen, boolean playMusic, boolean createFog) {
                operation[0] = "UPDATE_PROPERTIES";
            }
        });
        return new BossBarTriggerSnapshot(text[0], operation[0]);
    }

    private static void dispatchItemPickupTrigger(ClientboundTakeItemEntityPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || packet == null) {
            return;
        }
        if (packet.getPlayerId() != mc.player.getId()) {
            return;
        }

        JsonObject triggerData = new JsonObject();
        triggerData.addProperty("collectorEntityId", packet.getPlayerId());
        triggerData.addProperty("itemEntityId", packet.getItemId());
        triggerData.addProperty("count", Math.max(1, packet.getAmount()));

        Entity entity = mc.level.getEntity(packet.getItemId());
        if (entity instanceof ItemEntity) {
            ItemStack stack = ((ItemEntity) entity).getItem();
            if (!stack.isEmpty()) {
                triggerData.addProperty("itemName", safe(stack.getHoverName().getString()));
                Identifier registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (registryName != null) {
                    triggerData.addProperty("registryName", registryName.toString());
                }
            }
        }

        LegacySequenceTriggerManager.triggerEvent(LegacySequenceTriggerManager.TRIGGER_ITEM_PICKUP, triggerData);
    }

    private static String componentToPlainText(Component component) {
        return component == null ? "" : safe(component.getString()).trim();
    }

    private static void appendCaptured(boolean sent, CapturedPacketData data) {
        List<CapturedPacketData> target = sent ? capturedPackets : capturedReceivedPackets;
        synchronized (target) {
            if (!target.isEmpty()) {
                CapturedPacketData first = target.get(0);
                if (first != null && first.canAggregate(data)) {
                    first.mergeFrom(data);
                    return;
                }
            }
            target.add(0, data);
            int limit = resolveMaxCapturedPackets();
            while (target.size() > limit + CAPTURE_TRIM_BATCH) {
                target.remove(target.size() - 1);
            }
        }
        PacketIdRecordManager.recordCapturedPacket(sent, data);
    }

    private static void appendRecentPacketText(String packetClassName, String channel, String decodedText) {
        String decoded = safe(decodedText).trim();

        StringBuilder builder = new StringBuilder();
        String safeClassName = safe(packetClassName).trim();
        String safeChannel = safe(channel).trim();
        if (!safeClassName.isEmpty()) {
            builder.append(safeClassName);
        }
        if (!safeChannel.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(safeChannel);
        }
        if (!decoded.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(decoded);
        }

        String text = builder.toString().trim();
        if (text.isEmpty()) {
            return;
        }

        appendRecent(recentPacketTexts, text);
        packetTextVersion++;
    }

    private static void appendRecent(List<String> target, String value) {
        synchronized (target) {
            target.add(0, value == null ? "" : value);
            while (target.size() > MAX_RECENT_ITEMS) {
                target.remove(target.size() - 1);
            }
        }
    }

    private static void enqueueBusinessTask(Runnable task) {
        if (task == null) {
            return;
        }
        pendingBusinessTasks.offer(task);
    }

    private static void drainBusinessTasksOnMainThread() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            pendingBusinessTasks.clear();
            return;
        }

        int processed = 0;
        long startNanos = System.nanoTime();
        while (processed < MAX_BUSINESS_TASKS_PER_TICK) {
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
            processed++;
        }
    }

    private void handlePacketCapture(PacketMeta meta, String decodedText, boolean sent, boolean captureFeatureEnabled) {
        if (!captureFeatureEnabled || !isCapturing || meta == null) {
            return;
        }
        if (!shouldSampleCapture()) {
            droppedPacketCount++;
            return;
        }
        if (!shouldCapture(meta)) {
            return;
        }

        if (pendingCaptureQueue.size() >= MAX_CAPTURE_QUEUE) {
            pendingCaptureQueue.poll();
            droppedPacketCount++;
            long now = System.currentTimeMillis();
            if (now - lastCaptureDropWarnAt > 3000L) {
                lastCaptureDropWarnAt = now;
                zszlScriptMod.LOGGER.warn("[PacketCapture] 捕获队列过高，已丢弃最旧待处理数据包。queue={}",
                        pendingCaptureQueue.size());
            }
        }

        pendingCaptureQueue.offer(new PendingPacketSnapshot(meta.packetClassName, meta.isFmlPacket, meta.packetId,
                meta.channel, meta.rawData, decodedText, sent));
        lastKnownCaptureQueueSize = pendingCaptureQueue.size();
        sampledPacketCount++;
        scheduleDrainCaptureQueue();
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
        return current % activeSamplingModulo == 0;
    }

    private static boolean shouldCapture(PacketMeta meta) {
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        if (meta == null || config == null) {
            return true;
        }
        if (config.captureMode == CaptureMode.WHITELIST) {
            if (config.whitelistFilters == null || config.whitelistFilters.isEmpty()) {
                return true;
            }
            for (String filter : config.whitelistFilters) {
                if (packetMatchesFilter(meta, filter)) {
                    return true;
                }
            }
            return false;
        }
        if (config.blacklistFilters == null || config.blacklistFilters.isEmpty()) {
            return true;
        }
        for (String filter : config.blacklistFilters) {
            if (packetMatchesFilter(meta, filter)) {
                return false;
            }
        }
        return true;
    }

    private static boolean packetMatchesFilter(PacketMeta meta, String keyword) {
        if (meta == null || keyword == null || keyword.trim().isEmpty()) {
            return false;
        }

        String normalizedKeyword = keyword.replace('\u00A0', ' ').trim();
        String lowerKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
        String packetClassLower = safe(meta.packetClassName).toLowerCase(Locale.ROOT);
        String packetChannelLower = safe(meta.channel).toLowerCase(Locale.ROOT);
        String packetIdHexLower = meta.packetId == null ? "" : String.format("0x%02x", meta.packetId);
        String packetIdDecLower = meta.packetId == null ? "" : String.valueOf(meta.packetId);

        if (isRegexKeyword(normalizedKeyword)) {
            return packetMatchesRegex(extractRegexPattern(normalizedKeyword),
                    packetClassLower, packetChannelLower, packetIdHexLower, packetIdDecLower,
                    ByteBufUtil.hexDump(meta.rawData == null ? new byte[0] : meta.rawData).toLowerCase(Locale.ROOT));
        }

        if (packetClassLower.contains(lowerKeyword) || packetChannelLower.contains(lowerKeyword)
                || packetIdHexLower.contains(lowerKeyword) || packetIdDecLower.contains(lowerKeyword)) {
            return true;
        }

        String hexNoSpace = ByteBufUtil.hexDump(meta.rawData == null ? new byte[0] : meta.rawData)
                .toLowerCase(Locale.ROOT);
        String cleanedHexKeyword = normalizeHexText(lowerKeyword);
        if (!cleanedHexKeyword.isEmpty() && lowerKeyword.matches("^[0-9a-f\\s,:]+$")
                && hexNoSpace.contains(cleanedHexKeyword)) {
            return true;
        }

        if (containsNonAscii(normalizedKeyword)) {
            String utf8Hex = toUtf8HexNoSpace(normalizedKeyword);
            if (!utf8Hex.isEmpty() && hexNoSpace.contains(utf8Hex)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isRegexKeyword(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        return text.startsWith("re:") || (text.startsWith("/") && text.endsWith("/") && text.length() > 2);
    }

    private static String extractRegexPattern(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        if (text.startsWith("re:")) {
            return text.substring(3);
        }
        if (text.startsWith("/") && text.endsWith("/") && text.length() > 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static boolean packetMatchesRegex(String regex, String... haystacks) {
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

    private void scheduleDrainCaptureQueue() {
        if (!captureDrainScheduled.compareAndSet(false, true)) {
            return;
        }
        Minecraft.getInstance().execute(this::drainCaptureQueueOnMainThread);
    }

    private void drainCaptureQueueOnMainThread() {
        captureDrainScheduled.set(false);

        Minecraft mc = Minecraft.getInstance();
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
                    snapshot.packetId, snapshot.channel, snapshot.rawData, snapshot.decodedText);
            appendCaptured(snapshot.sent, packetData);

            processed++;
            processedBytes += snapshot.rawData.length;
        }

        if (!pendingCaptureQueue.isEmpty()) {
            lastKnownCaptureQueueSize = pendingCaptureQueue.size();
            scheduleDrainCaptureQueue();
        } else {
            lastKnownCaptureQueueSize = 0;
        }
    }

    private static int resolveMaxCapturedPackets() {
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        if (config == null || config.maxCapturedPackets <= 0) {
            return DEFAULT_MAX_CAPTURED_PACKETS;
        }
        return Math.max(100, Math.min(50000, config.maxCapturedPackets));
    }

    private static boolean isBusinessProcessingEnabled() {
        PacketFilterConfig config = PacketFilterConfig.INSTANCE;
        return config == null || config.enableBusinessPacketProcessing;
    }

    private static boolean shouldTrackRecentPacketTexts() {
        return System.currentTimeMillis() <= recentPacketTextTrackingUntilMs;
    }

    private static boolean hasPacketTriggerConsumers() {
        return NodeTriggerManager.hasGraphsForTrigger(NodeTriggerManager.TRIGGER_PACKET)
                || LegacySequenceTriggerManager.hasRulesForTrigger(LegacySequenceTriggerManager.TRIGGER_PACKET);
    }

    private static boolean isRawPacketProcessingNeeded(boolean outbound, boolean captureFeatureEnabled,
            boolean interceptEnabled) {
        if (captureFeatureEnabled && isCapturing) {
            return true;
        }
        if (interceptEnabled) {
            return true;
        }
        if (!isBusinessProcessingEnabled()) {
            return shouldTrackRecentPacketTexts();
        }
        if (shouldTrackRecentPacketTexts()) {
            return true;
        }
        if (hasPacketTriggerConsumers()) {
            return true;
        }
        if (CapturedIdRuleManager.hasEnabledRulesForDirection(outbound)) {
            return true;
        }
        return PacketFieldRuleManager.hasEnabledRulesForDirection(outbound);
    }

    private static boolean isDecodedTextNeeded(boolean outbound) {
        if (shouldTrackRecentPacketTexts()) {
            return true;
        }
        if (!isBusinessProcessingEnabled()) {
            return false;
        }
        if (hasPacketTriggerConsumers()) {
            return true;
        }
        if (CapturedIdRuleManager.hasDecodedRulesForDirection(outbound)) {
            return true;
        }
        return PacketFieldRuleManager.hasDecodedRulesForDirection(outbound);
    }

    private static boolean shouldSkipBusinessPayload(Packet<?> packet) {
        return packet == null || isWorldStreamingPacket(packet.getClass().getSimpleName());
    }

    private static boolean shouldSkipPayloadDecode(String packetClassName, byte[] rawData) {
        return rawData == null
                || rawData.length == 0
                || rawData.length > MAX_RECENT_TEXT_DECODE_BYTES
                || isWorldStreamingPacket(packetClassName);
    }

    private static boolean isWorldStreamingPacket(String packetClassName) {
        String name = packetClassName == null ? "" : packetClassName.toLowerCase(Locale.ROOT);
        return name.contains("chunk")
                || name.contains("lightupdate")
                || name.contains("levelchunk")
                || name.contains("forgetlevelchunk")
                || name.contains("chunksbiomes");
    }

    private static String normalizeHexText(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");
    }

    private static boolean containsNonAscii(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 127) {
                return true;
            }
        }
        return false;
    }

    private static String toUtf8HexNoSpace(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b & 0xFF));
        }
        return builder.toString();
    }

    private static PacketMeta inspectPacket(Packet<?> packet, boolean outbound) {
        if (packet instanceof BundlePacket<?>) {
            return new PacketMeta(packet.getClass().getSimpleName(), false, null, "", new byte[0]);
        }

        PacketCodecCompat.EncodedPacket encoded = null;
        try {
            encoded = PacketCodecCompat.encode(packet, outbound);
        } catch (Throwable t) {
            logEncodeFailureOnce(packet, t);
        }

        if (packet instanceof ServerboundCustomPayloadPacket) {
            ServerboundCustomPayloadPacket customPacket = (ServerboundCustomPayloadPacket) packet;
            Identifier identifier = ModUtils.ensureCustomPayloadIdentifier(customPacket.payload().type().id(), "");
            return new PacketMeta(packet.getClass().getSimpleName(), true, null, normalizeChannel(identifier),
                    PacketCodecCompat.extractCustomPayloadBytes(customPacket.payload(),
                            encoded == null ? null : encoded.payloadBytes));
        }
        if (packet instanceof ClientboundCustomPayloadPacket) {
            ClientboundCustomPayloadPacket customPacket = (ClientboundCustomPayloadPacket) packet;
            Identifier identifier = ModUtils.ensureCustomPayloadIdentifier(customPacket.payload().type().id(), "");
            return new PacketMeta(packet.getClass().getSimpleName(), true, null, normalizeChannel(identifier),
                    PacketCodecCompat.extractCustomPayloadBytes(customPacket.payload(),
                            encoded == null ? null : encoded.payloadBytes));
        }

        Integer packetId = encoded == null ? null : encoded.packetId;
        return new PacketMeta(packet.getClass().getSimpleName(), false, packetId, "",
                encoded == null ? new byte[0] : encoded.payloadBytes);
    }

    private static byte[] writePacket(Packet<?> packet) {
        if (packet instanceof BundlePacket<?>) {
            return new byte[0];
        }
        try {
            PacketCodecCompat.EncodedPacket encoded = PacketCodecCompat.encode(packet, true);
            return encoded.payloadBytes;
        } catch (Throwable outboundError) {
            try {
                PacketCodecCompat.EncodedPacket encoded = PacketCodecCompat.encode(packet, false);
                return encoded.payloadBytes;
            } catch (Throwable inboundError) {
                logEncodeFailureOnce(packet, inboundError);
            }
        }
        return new byte[0];
    }

    private static void logEncodeFailureOnce(Packet<?> packet, Throwable throwable) {
        String packetName = packet == null ? "<null>" : packet.getClass().getName();
        if (ENCODE_FAILURE_LOGGED.putIfAbsent(packetName, Boolean.TRUE) == null) {
            zszlScriptMod.LOGGER.warn("抓包序列化失败，已跳过该包 raw bytes: {} ({})",
                    packetName, throwable == null ? "unknown" : throwable.getMessage());
            zszlScriptMod.LOGGER.debug("抓包序列化失败详情: {}", packetName, throwable);
        }
    }

    private static String decodePayload(String channel, byte[] rawData) {
        if (rawData == null || rawData.length == 0) {
            return "";
        }
        String decoded = PacketPayloadDecoder.decode(rawData);
        return decoded == null ? "" : decoded;
    }

    private static String normalizeChannel(Identifier identifier) {
        if (identifier == null) {
            return "";
        }
        String id = identifier.toString();
        String path = identifier.getPath();
        if ("owlviewchannel".equalsIgnoreCase(path) || path.toLowerCase(Locale.ROOT).contains("owlview")) {
            return OWL_VIEW_CHANNEL;
        }
        if ("owlcontrolchannel".equalsIgnoreCase(path) || path.toLowerCase(Locale.ROOT).contains("owlcontrol")) {
            return OWL_CONTROL_CHANNEL;
        }
        return id;
    }

    private static String bytesToHex(byte[] bytes) {
        return bytes == null ? "" : ByteBufUtil.hexDump(bytes).toUpperCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class PacketMeta {
        private final String packetClassName;
        private final boolean isFmlPacket;
        private final Integer packetId;
        private final String channel;
        private final byte[] rawData;

        private PacketMeta(String packetClassName, boolean isFmlPacket, Integer packetId, String channel,
                byte[] rawData) {
            this.packetClassName = safe(packetClassName);
            this.isFmlPacket = isFmlPacket;
            this.packetId = packetId;
            this.channel = safe(channel);
            this.rawData = rawData == null ? new byte[0] : rawData;
        }
    }
}
