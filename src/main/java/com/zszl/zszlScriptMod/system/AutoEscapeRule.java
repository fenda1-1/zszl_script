package com.zszl.zszlScriptMod.system;

import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

public class AutoEscapeRule {
    public static final double DEFAULT_DETECTION_RANGE = 8.0D;
    public static final int DEFAULT_RESTART_DELAY_SECONDS = 10;
    public static final String PLAYER_GAME_MODE_ALL = "all";
    public static final String PLAYER_GAME_MODE_SURVIVAL = "survival";
    public static final String PLAYER_GAME_MODE_CREATIVE = "creative";
    public static final String PLAYER_GAME_MODE_SPECTATOR = "spectator";
    public static final String PLAYER_GAME_MODE_UNKNOWN = "unknown";

    public String name;
    public String category;
    public boolean enabled;
    public List<String> entityTypes;
    public double detectionRange;

    public boolean enableNameWhitelist;
    public List<String> nameWhitelist;

    public boolean enableNameBlacklist;
    public List<String> nameBlacklist;

    public boolean enablePlayerGameModeFilter;
    public String playerGameModeFilter;

    public String escapeSequenceName;

    public boolean restartEnabled;
    public int restartDelaySeconds;
    public String restartSequenceName;
    public boolean ignoreTargetsUntilRestartComplete;

    // 运行时状态（不持久化）
    public transient boolean triggerLatched = false;

    public AutoEscapeRule() {
        this.name = I18n.format("rule.auto_escape.default_name");
        this.category = "默认";
        this.enabled = true;

        this.entityTypes = new ArrayList<>();
        this.entityTypes.add("player");
        this.entityTypes.add("monster");

        this.detectionRange = DEFAULT_DETECTION_RANGE;

        this.enableNameWhitelist = false;
        this.nameWhitelist = new ArrayList<>();

        this.enableNameBlacklist = false;
        this.nameBlacklist = new ArrayList<>();

        this.enablePlayerGameModeFilter = false;
        this.playerGameModeFilter = PLAYER_GAME_MODE_ALL;

        this.escapeSequenceName = "";

        this.restartEnabled = false;
        this.restartDelaySeconds = DEFAULT_RESTART_DELAY_SECONDS;
        this.restartSequenceName = "";
        this.ignoreTargetsUntilRestartComplete = false;
    }

    public void ensureLists() {
        if (entityTypes == null) {
            entityTypes = new ArrayList<>();
        }
        if (nameWhitelist == null) {
            nameWhitelist = new ArrayList<>();
        }
        if (nameBlacklist == null) {
            nameBlacklist = new ArrayList<>();
        }
    }

    public void normalize() {
        if (name == null || name.trim().isEmpty()) {
            name = I18n.format("rule.auto_escape.default_name");
        } else {
            name = name.trim();
        }

        category = category == null || category.trim().isEmpty() ? "默认" : category.trim();
        detectionRange = detectionRange <= 0 ? DEFAULT_DETECTION_RANGE : detectionRange;
        restartDelaySeconds = Math.max(0, restartDelaySeconds);

        escapeSequenceName = escapeSequenceName == null ? "" : escapeSequenceName.trim();
        restartSequenceName = restartSequenceName == null ? "" : restartSequenceName.trim();
        playerGameModeFilter = normalizePlayerGameModeFilter(playerGameModeFilter);

        ensureLists();
        entityTypes = sanitizeStringList(entityTypes);
        nameWhitelist = sanitizeStringList(nameWhitelist);
        nameBlacklist = sanitizeStringList(nameBlacklist);
    }

    public void resetRuntimeState() {
        this.triggerLatched = false;
    }

    public AutoEscapeRule copy() {
        AutoEscapeRule copy = new AutoEscapeRule();
        copy.name = this.name;
        copy.category = this.category;
        copy.enabled = this.enabled;
        copy.entityTypes = new ArrayList<>(this.entityTypes == null ? new ArrayList<String>() : this.entityTypes);
        copy.detectionRange = this.detectionRange;
        copy.enableNameWhitelist = this.enableNameWhitelist;
        copy.nameWhitelist = new ArrayList<>(this.nameWhitelist == null ? new ArrayList<String>() : this.nameWhitelist);
        copy.enableNameBlacklist = this.enableNameBlacklist;
        copy.nameBlacklist = new ArrayList<>(this.nameBlacklist == null ? new ArrayList<String>() : this.nameBlacklist);
        copy.enablePlayerGameModeFilter = this.enablePlayerGameModeFilter;
        copy.playerGameModeFilter = this.playerGameModeFilter;
        copy.escapeSequenceName = this.escapeSequenceName;
        copy.restartEnabled = this.restartEnabled;
        copy.restartDelaySeconds = this.restartDelaySeconds;
        copy.restartSequenceName = this.restartSequenceName;
        copy.ignoreTargetsUntilRestartComplete = this.ignoreTargetsUntilRestartComplete;
        copy.triggerLatched = this.triggerLatched;
        copy.normalize();
        return copy;
    }

    public static String normalizePlayerGameModeFilter(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        switch (normalized) {
            case "生存":
            case "冒险":
            case "adventure":
            case PLAYER_GAME_MODE_SURVIVAL:
                return PLAYER_GAME_MODE_SURVIVAL;
            case "创造":
            case PLAYER_GAME_MODE_CREATIVE:
                return PLAYER_GAME_MODE_CREATIVE;
            case "旁观":
            case PLAYER_GAME_MODE_SPECTATOR:
                return PLAYER_GAME_MODE_SPECTATOR;
            case "未知":
            case PLAYER_GAME_MODE_UNKNOWN:
                return PLAYER_GAME_MODE_UNKNOWN;
            default:
                return PLAYER_GAME_MODE_ALL;
        }
    }

    private static List<String> sanitizeStringList(List<String> source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String value : source) {
            String trimmed = value == null ? "" : value.trim();
            if (!trimmed.isEmpty() && !containsIgnoreCase(result, trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }
}
