package com.zszl.zszlScriptMod.system;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

public class AutoEscapeRule {
    public static final double DEFAULT_DETECTION_RANGE = 8.0D;
    public static final int DEFAULT_RESTART_DELAY_SECONDS = 10;

    public String name;
    public String category;
    public boolean enabled;
    public List<String> entityTypes;
    public double detectionRange;

    public boolean enableNameWhitelist;
    public List<String> nameWhitelist;

    public boolean enableNameBlacklist;
    public List<String> nameBlacklist;

    public String escapeSequenceName;

    public boolean restartEnabled;
    public int restartDelaySeconds;
    public String restartSequenceName;

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

        this.escapeSequenceName = "";

        this.restartEnabled = false;
        this.restartDelaySeconds = DEFAULT_RESTART_DELAY_SECONDS;
        this.restartSequenceName = "";
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
        copy.escapeSequenceName = this.escapeSequenceName;
        copy.restartEnabled = this.restartEnabled;
        copy.restartDelaySeconds = this.restartDelaySeconds;
        copy.restartSequenceName = this.restartSequenceName;
        copy.triggerLatched = this.triggerLatched;
        copy.normalize();
        return copy;
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


