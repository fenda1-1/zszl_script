package com.zszl.zszlScriptMod.path.runtime.safety;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class PathSafetyManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class ConfigSnapshot {
        private final boolean safeModeEnabled;
        private final boolean dryRunDangerousActions;
        private final boolean allowPacketActions;
        private final boolean allowInventoryWriteActions;
        private final boolean allowItemDropActions;
        private final boolean allowBackgroundSequences;

        private ConfigSnapshot(Config config) {
            this.safeModeEnabled = config.safeModeEnabled;
            this.dryRunDangerousActions = config.dryRunDangerousActions;
            this.allowPacketActions = config.allowPacketActions;
            this.allowInventoryWriteActions = config.allowInventoryWriteActions;
            this.allowItemDropActions = config.allowItemDropActions;
            this.allowBackgroundSequences = config.allowBackgroundSequences;
        }

        public boolean isSafeModeEnabled() {
            return safeModeEnabled;
        }

        public boolean isDryRunDangerousActions() {
            return dryRunDangerousActions;
        }

        public boolean isAllowPacketActions() {
            return allowPacketActions;
        }

        public boolean isAllowInventoryWriteActions() {
            return allowInventoryWriteActions;
        }

        public boolean isAllowItemDropActions() {
            return allowItemDropActions;
        }

        public boolean isAllowBackgroundSequences() {
            return allowBackgroundSequences;
        }
    }

    public static final class ConfigSnapshotBuilder {
        private boolean safeModeEnabled;
        private boolean dryRunDangerousActions;
        private boolean allowPacketActions;
        private boolean allowInventoryWriteActions;
        private boolean allowItemDropActions;
        private boolean allowBackgroundSequences;

        public ConfigSnapshotBuilder(ConfigSnapshot snapshot) {
            if (snapshot != null) {
                this.safeModeEnabled = snapshot.safeModeEnabled;
                this.dryRunDangerousActions = snapshot.dryRunDangerousActions;
                this.allowPacketActions = snapshot.allowPacketActions;
                this.allowInventoryWriteActions = snapshot.allowInventoryWriteActions;
                this.allowItemDropActions = snapshot.allowItemDropActions;
                this.allowBackgroundSequences = snapshot.allowBackgroundSequences;
            }
        }

        public ConfigSnapshotBuilder setSafeModeEnabled(boolean value) {
            this.safeModeEnabled = value;
            return this;
        }

        public ConfigSnapshotBuilder setDryRunDangerousActions(boolean value) {
            this.dryRunDangerousActions = value;
            return this;
        }

        public ConfigSnapshotBuilder setAllowPacketActions(boolean value) {
            this.allowPacketActions = value;
            return this;
        }

        public ConfigSnapshotBuilder setAllowInventoryWriteActions(boolean value) {
            this.allowInventoryWriteActions = value;
            return this;
        }

        public ConfigSnapshotBuilder setAllowItemDropActions(boolean value) {
            this.allowItemDropActions = value;
            return this;
        }

        public ConfigSnapshotBuilder setAllowBackgroundSequences(boolean value) {
            this.allowBackgroundSequences = value;
            return this;
        }

        public ConfigSnapshot build() {
            Config next = new Config();
            next.safeModeEnabled = safeModeEnabled;
            next.dryRunDangerousActions = dryRunDangerousActions;
            next.allowPacketActions = allowPacketActions;
            next.allowInventoryWriteActions = allowInventoryWriteActions;
            next.allowItemDropActions = allowItemDropActions;
            next.allowBackgroundSequences = allowBackgroundSequences;
            return new ConfigSnapshot(next);
        }
    }

    public static final class SafetyDecision {
        private final boolean blocked;
        private final String reason;

        private SafetyDecision(boolean blocked, String reason) {
            this.blocked = blocked;
            this.reason = reason == null ? "" : reason;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getReason() {
            return reason;
        }
    }

    private static final class Config {
        private boolean safeModeEnabled = false;
        private boolean dryRunDangerousActions = false;
        private boolean allowPacketActions = false;
        private boolean allowInventoryWriteActions = true;
        private boolean allowItemDropActions = false;
        private boolean allowBackgroundSequences = true;
    }

    private static Config config = new Config();
    private static volatile boolean initialized = false;

    private PathSafetyManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        reload();
        initialized = true;
    }

    public static synchronized void reload() {
        config = new Config();
        Path path = getConfigPath();
        ensureConfigExists(path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded != null) {
                config.safeModeEnabled = loaded.safeModeEnabled;
                config.dryRunDangerousActions = loaded.dryRunDangerousActions;
                config.allowPacketActions = loaded.allowPacketActions;
                config.allowInventoryWriteActions = loaded.allowInventoryWriteActions;
                config.allowItemDropActions = loaded.allowItemDropActions;
                config.allowBackgroundSequences = loaded.allowBackgroundSequences;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[PathSafety] 加载配置失败", e);
        }
    }

    public static synchronized void save(ConfigSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        config.safeModeEnabled = snapshot.safeModeEnabled;
        config.dryRunDangerousActions = snapshot.dryRunDangerousActions;
        config.allowPacketActions = snapshot.allowPacketActions;
        config.allowInventoryWriteActions = snapshot.allowInventoryWriteActions;
        config.allowItemDropActions = snapshot.allowItemDropActions;
        config.allowBackgroundSequences = snapshot.allowBackgroundSequences;
        saveCurrentConfig();
    }

    public static synchronized ConfigSnapshot getSnapshot() {
        initialize();
        return new ConfigSnapshot(config);
    }

    public static synchronized SafetyDecision evaluateAction(String actionType) {
        initialize();
        String type = normalize(actionType);
        if (type.isEmpty()) {
            return new SafetyDecision(false, "");
        }

        boolean packetAction = "send_packet".equals(type);
        boolean itemDropAction = "dropfiltereditems".equals(type);
        boolean inventoryWriteAction = isInventoryWriteAction(type);
        boolean dangerous = packetAction || itemDropAction || inventoryWriteAction;

        if (config.dryRunDangerousActions && dangerous) {
            return new SafetyDecision(true, "干跑模式已跳过危险动作");
        }
        if (!config.safeModeEnabled) {
            return new SafetyDecision(false, "");
        }
        if (packetAction && !config.allowPacketActions) {
            return new SafetyDecision(true, "安全模式已屏蔽发包动作");
        }
        if (itemDropAction && !config.allowItemDropActions) {
            return new SafetyDecision(true, "安全模式已屏蔽丢弃物品动作");
        }
        if (inventoryWriteAction && !config.allowInventoryWriteActions) {
            return new SafetyDecision(true, "安全模式已屏蔽背包/容器写动作");
        }
        return new SafetyDecision(false, "");
    }

    public static synchronized SafetyDecision evaluateBackgroundSequence(PathSequence sequence) {
        initialize();
        if (!config.dryRunDangerousActions && (!config.safeModeEnabled || config.allowBackgroundSequences)) {
            return new SafetyDecision(false, "");
        }
        String sequenceName = sequence == null ? "" : sequence.getName();
        if (config.dryRunDangerousActions) {
            return new SafetyDecision(true, "干跑模式已阻止后台序列: " + sequenceName);
        }
        return new SafetyDecision(true, "安全模式已屏蔽后台序列: " + sequenceName);
    }

    private static boolean isInventoryWriteAction(String type) {
        switch (normalize(type)) {
            case "window_click":
            case "conditional_window_click":
            case "takeallitems":
            case "take_all_items_safe":
            case "autochestclick":
            case "move_inventory_items_to_chest_slots":
            case "spread_inventory_item":
            case "stack_inventory_item":
            case "transferitemstowarehouse":
            case "warehouse_auto_deposit":
            case "move_inventory_item_to_hotbar":
            case "switch_hotbar_slot":
            case "silentuse":
            case "use_hotbar_item":
            case "use_held_item":
                return true;
            default:
                return false;
        }
    }

    private static void ensureConfigExists(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                saveCurrentConfig();
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[PathSafety] 初始化配置失败", e);
        }
    }

    private static void saveCurrentConfig() {
        Path path = getConfigPath();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[PathSafety] 保存配置失败", e);
        }
    }

    private static Path getConfigPath() {
        return ProfileManager.getCurrentProfileDir().resolve("path_safety_config.json");
    }

    private static String normalize(String actionType) {
        return actionType == null ? "" : actionType.trim().toLowerCase(Locale.ROOT);
    }
}

