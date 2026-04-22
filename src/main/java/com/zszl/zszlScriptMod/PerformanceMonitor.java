package com.zszl.zszlScriptMod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.system.ProfileManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring system for tracking and controlling lag-causing
 * features
 */
public class PerformanceMonitor {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "performance_monitor.json";

    private static final Map<String, Boolean> DEFAULT_FEATURE_STATES = new LinkedHashMap<>();

    // Feature enable/disable states
    private static final Map<String, Boolean> featureStates = new ConcurrentHashMap<>();
    private static final Map<String, Long> temporaryDisabledUntil = new ConcurrentHashMap<>();

    // Performance statistics
    private static final Map<String, PerformanceStats> performanceStats = new ConcurrentHashMap<>();

    // Spike guard settings
    private static volatile boolean spikeGuardEnabled = false;
    private static volatile long spikeThresholdNanos = 500_000_000L;
    private static volatile long spikeDisableDurationMs = 500L;

    // Initialize default feature states
    static {
        initializeDefaultFeatureStates();
        resetToDefaults();
    }

    private static void initializeDefaultFeatureStates() {
        // Core features that can cause lag
        DEFAULT_FEATURE_STATES.put("auto_equip", true);
        DEFAULT_FEATURE_STATES.put("auto_pickup", true);
        DEFAULT_FEATURE_STATES.put("auto_follow", true);
        DEFAULT_FEATURE_STATES.put("path_sequence", true);
        DEFAULT_FEATURE_STATES.put("conditional_execution", true);
        DEFAULT_FEATURE_STATES.put("debuff_detector", true);
        DEFAULT_FEATURE_STATES.put("freecam", true);
        DEFAULT_FEATURE_STATES.put("goto_open", true);
        DEFAULT_FEATURE_STATES.put("warehouse", true);

        // Packet/network features that can cause lag
        DEFAULT_FEATURE_STATES.put("packet_capture_inbound", true);
        DEFAULT_FEATURE_STATES.put("packet_capture_outbound", true);
        DEFAULT_FEATURE_STATES.put("packet_intercept", true);
        DEFAULT_FEATURE_STATES.put("packet_send_fml", true);
        DEFAULT_FEATURE_STATES.put("packet_send_standard", true);
    }

    private static void resetToDefaults() {
        featureStates.clear();
        featureStates.putAll(DEFAULT_FEATURE_STATES);
        temporaryDisabledUntil.clear();
        spikeGuardEnabled = false;
        spikeThresholdNanos = 500_000_000L;
        spikeDisableDurationMs = 500L;
        ensurePerformanceStatsEntries();
    }

    private static void ensurePerformanceStatsEntries() {
        for (String feature : featureStates.keySet()) {
            performanceStats.computeIfAbsent(feature, key -> new PerformanceStats());
        }
    }

    private static void ensureDefaultFeatureStates() {
        for (Map.Entry<String, Boolean> entry : DEFAULT_FEATURE_STATES.entrySet()) {
            featureStates.putIfAbsent(entry.getKey(), entry.getValue());
            performanceStats.computeIfAbsent(entry.getKey(), key -> new PerformanceStats());
        }
        ensurePerformanceStatsEntries();
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve(CONFIG_FILE_NAME);
    }

    private static double sanitizeSpikeThresholdMillis(double millis) {
        if (Double.isNaN(millis) || Double.isInfinite(millis)) {
            return 20.0;
        }
        return Math.max(0.1, millis);
    }

    private static long sanitizeSpikeDisableDurationMs(long durationMs) {
        return Math.max(1L, durationMs);
    }

    public static synchronized void loadConfig() {
        resetToDefaults();

        Path file = getConfigFile();
        if (!Files.exists(file)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

            if (root.has("featureStates") && root.get("featureStates").isJsonObject()) {
                JsonObject loadedStates = root.getAsJsonObject("featureStates");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : loadedStates.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isJsonPrimitive()
                            && entry.getValue().getAsJsonPrimitive().isBoolean()) {
                        featureStates.put(entry.getKey(), entry.getValue().getAsBoolean());
                    }
                }
            }

            if (root.has("spikeGuardEnabled")) {
                spikeGuardEnabled = root.get("spikeGuardEnabled").getAsBoolean();
            }
            if (root.has("spikeThresholdMillis")) {
                double thresholdMillis = sanitizeSpikeThresholdMillis(root.get("spikeThresholdMillis").getAsDouble());
                spikeThresholdNanos = (long) (thresholdMillis * 1_000_000.0);
            }
            if (root.has("spikeDisableDurationMs")) {
                spikeDisableDurationMs = sanitizeSpikeDisableDurationMs(
                        root.get("spikeDisableDurationMs").getAsLong());
            }

            ensureDefaultFeatureStates();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to load performance monitor config", e);
            resetToDefaults();
        }
    }

    public static synchronized void saveConfig() {
        try {
            Path file = getConfigFile();
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.add("featureStates", GSON.toJsonTree(new LinkedHashMap<>(featureStates)));
                root.addProperty("spikeGuardEnabled", spikeGuardEnabled);
                root.addProperty("spikeThresholdMillis", getSpikeThresholdMillis());
                root.addProperty("spikeDisableDurationMs", spikeDisableDurationMs);
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to save performance monitor config", e);
        }
    }

    /**
     * Check if a feature is enabled
     */
    public static boolean isFeatureEnabled(String featureName) {
        return isFeatureManuallyEnabled(featureName) && !isFeatureTemporarilyDisabled(featureName);
    }

    public static boolean isFeatureManuallyEnabled(String featureName) {
        return featureStates.getOrDefault(featureName, true);
    }

    public static boolean isFeatureTemporarilyDisabled(String featureName) {
        Long disabledUntil = temporaryDisabledUntil.get(featureName);
        if (disabledUntil == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (disabledUntil <= now) {
            temporaryDisabledUntil.remove(featureName, disabledUntil);
            return false;
        }
        return true;
    }

    public static long getTemporaryDisableRemainingMs(String featureName) {
        Long disabledUntil = temporaryDisabledUntil.get(featureName);
        if (disabledUntil == null) {
            return 0L;
        }
        long remaining = disabledUntil - System.currentTimeMillis();
        if (remaining <= 0L) {
            temporaryDisabledUntil.remove(featureName, disabledUntil);
            return 0L;
        }
        return remaining;
    }

    /**
     * Enable or disable a feature
     */
    public static void setFeatureEnabled(String featureName, boolean enabled) {
        featureStates.put(featureName, enabled);
        temporaryDisabledUntil.remove(featureName);
        performanceStats.computeIfAbsent(featureName, key -> new PerformanceStats());
        if (!enabled) {
            resetPerformanceStats(featureName);
        }
        saveConfig();
    }

    public static boolean isSpikeGuardEnabled() {
        return spikeGuardEnabled;
    }

    public static void setSpikeGuardEnabled(boolean enabled) {
        spikeGuardEnabled = enabled;
        if (!enabled) {
            temporaryDisabledUntil.clear();
        }
        saveConfig();
    }

    public static double getSpikeThresholdMillis() {
        return spikeThresholdNanos / 1_000_000.0;
    }

    public static void setSpikeThresholdMillis(double millis) {
        double sanitized = sanitizeSpikeThresholdMillis(millis);
        spikeThresholdNanos = (long) (sanitized * 1_000_000.0);
        saveConfig();
    }

    public static long getSpikeDisableDurationMs() {
        return spikeDisableDurationMs;
    }

    public static void setSpikeDisableDurationMs(long durationMs) {
        spikeDisableDurationMs = sanitizeSpikeDisableDurationMs(durationMs);
        saveConfig();
    }

    /**
     * Get performance statistics for a feature
     */
    public static PerformanceStats getPerformanceStats(String featureName) {
        return performanceStats.get(featureName);
    }

    /**
     * Get all feature states
     */
    public static Map<String, Boolean> getAllFeatureStates() {
        return new ConcurrentHashMap<>(featureStates);
    }

    /**
     * Get all performance statistics
     */
    public static Map<String, PerformanceStats> getAllPerformanceStats() {
        return new ConcurrentHashMap<>(performanceStats);
    }

    /**
     * Reset performance statistics for a feature
     */
    public static void resetPerformanceStats(String featureName) {
        PerformanceStats stats = performanceStats.get(featureName);
        if (stats != null) {
            stats.reset();
        }
    }

    /**
     * Start a performance timer for a feature
     */
    public static PerformanceTimer startTimer(String featureName) {
        performanceStats.computeIfAbsent(featureName, key -> new PerformanceStats());
        PerformanceTimer timer = new PerformanceTimer(featureName, isFeatureEnabled(featureName));
        timer.start();
        return timer;
    }

    /**
     * Performance timer for measuring execution time
     */
    public static class PerformanceTimer {
        private final String featureName;
        private final boolean active;
        private long startTime;
        private boolean running;

        public PerformanceTimer(String featureName) {
            this(featureName, true);
        }

        public PerformanceTimer(String featureName, boolean active) {
            this.featureName = featureName;
            this.active = active;
            this.running = false;
        }

        public void start() {
            if (active && !running) {
                startTime = System.nanoTime();
                running = true;
            }
        }

        public void stop() {
            if (active && running) {
                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                running = false;

                // Record the measurement
                PerformanceStats stats = performanceStats.get(featureName);
                if (stats != null) {
                    stats.recordMeasurement(duration);
                }

                maybeTemporarilyDisableFeature(featureName, duration);
            }
        }
    }

    private static void maybeTemporarilyDisableFeature(String featureName, long durationNanos) {
        if (!spikeGuardEnabled || !isFeatureManuallyEnabled(featureName)) {
            return;
        }
        if (!"packet_capture_inbound".equals(featureName) && !"packet_capture_outbound".equals(featureName)) {
            return;
        }
        if (durationNanos < spikeThresholdNanos) {
            return;
        }
        temporaryDisabledUntil.put(featureName, System.currentTimeMillis() + spikeDisableDurationMs);
    }

    /**
     * Performance statistics tracker
     */
    public static class PerformanceStats {
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong measurementCount = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        private volatile long lastMeasurement = 0;

        public void recordMeasurement(long nanoTime) {
            totalTime.addAndGet(nanoTime);
            measurementCount.incrementAndGet();

            // Update min/max atomically
            long currentMin = minTime.get();
            while (nanoTime < currentMin && !minTime.compareAndSet(currentMin, nanoTime)) {
                currentMin = minTime.get();
            }

            long currentMax = maxTime.get();
            while (nanoTime > currentMax && !maxTime.compareAndSet(currentMax, nanoTime)) {
                currentMax = maxTime.get();
            }

            lastMeasurement = nanoTime;
        }

        public long getAverageTimeNanos() {
            long count = measurementCount.get();
            return count > 0 ? totalTime.get() / count : 0;
        }

        public long getMinTimeNanos() {
            return minTime.get() == Long.MAX_VALUE ? 0 : minTime.get();
        }

        public long getMaxTimeNanos() {
            return maxTime.get();
        }

        public long getTotalTimeNanos() {
            return totalTime.get();
        }

        public long getMeasurementCount() {
            return measurementCount.get();
        }

        public long getLastMeasurementNanos() {
            return lastMeasurement;
        }

        public double getAverageTimeMillis() {
            return getAverageTimeNanos() / 1_000_000.0;
        }

        public double getMinTimeMillis() {
            return getMinTimeNanos() / 1_000_000.0;
        }

        public double getMaxTimeMillis() {
            return getMaxTimeNanos() / 1_000_000.0;
        }

        public double getLastMeasurementMillis() {
            return getLastMeasurementNanos() / 1_000_000.0;
        }

        public void reset() {
            totalTime.set(0);
            measurementCount.set(0);
            minTime.set(Long.MAX_VALUE);
            maxTime.set(0);
            lastMeasurement = 0;
        }
    }
}

