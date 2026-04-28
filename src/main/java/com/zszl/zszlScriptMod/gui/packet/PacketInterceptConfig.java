package com.zszl.zszlScriptMod.gui.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PacketInterceptConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static PacketInterceptConfig INSTANCE = new PacketInterceptConfig();

    public boolean inboundInterceptEnabled = false;
    public List<InterceptRule> inboundRules = new ArrayList<>();
    private static final String LEGACY_SMART_COPY_RULE_NAME = "聊天run_command动态改copy";

    public static class InterceptRule {
        public String name = "new_rule";
        public boolean enabled = true;
        // 支持标准包过滤：可填 ID:0x44 / ID:68 / SPacketTeams / teams（大小写不敏感）
        public String packetFilter = "";
        public String channel = "";
        public String matchHex = "";
        public String replaceHex = "";
        // 开启后，matchHex 按 Java 正则表达式处理，replaceHex 支持 $1/$2 等分组替换
        public boolean regexEnabled = false;
        public boolean replaceAll = true;

        public InterceptRule copy() {
            InterceptRule c = new InterceptRule();
            c.name = this.name;
            c.enabled = this.enabled;
            c.packetFilter = this.packetFilter;
            c.channel = this.channel;
            c.matchHex = this.matchHex;
            c.replaceHex = this.replaceHex;
            c.regexEnabled = this.regexEnabled;
            c.replaceAll = this.replaceAll;
            return c;
        }
    }

    private PacketInterceptConfig() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("packet_intercept_rules.json");
    }

    public static void save() {
        removeLegacySmartCopyRules();
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to save packet intercept config", e);
        }
    }

    public static void load() {
        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                INSTANCE = GSON.fromJson(reader, PacketInterceptConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new PacketInterceptConfig();
                }
                if (INSTANCE.inboundRules == null) {
                    INSTANCE.inboundRules = new ArrayList<>();
                }
                for (InterceptRule rule : INSTANCE.inboundRules) {
                    normalizeRule(rule);
                }
                removeLegacySmartCopyRules();
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("Failed to load packet intercept config", e);
                INSTANCE = new PacketInterceptConfig();
                removeLegacySmartCopyRules();
            }
        } else {
            INSTANCE = new PacketInterceptConfig();
            removeLegacySmartCopyRules();
        }
    }

    public static void normalizeRule(InterceptRule rule) {
        if (rule == null) {
            return;
        }
        if (rule.name == null)
            rule.name = "new_rule";
        if (rule.packetFilter == null)
            rule.packetFilter = "";
        if (rule.channel == null)
            rule.channel = "";
        if (rule.matchHex == null)
            rule.matchHex = "";
        if (rule.replaceHex == null)
            rule.replaceHex = "";
    }

    private static void removeLegacySmartCopyRules() {
        if (INSTANCE == null) {
            INSTANCE = new PacketInterceptConfig();
        }
        if (INSTANCE.inboundRules == null) {
            INSTANCE.inboundRules = new ArrayList<>();
            return;
        }
        for (int i = INSTANCE.inboundRules.size() - 1; i >= 0; i--) {
            InterceptRule rule = INSTANCE.inboundRules.get(i);
            if (rule != null && LEGACY_SMART_COPY_RULE_NAME.equals(rule.name)) {
                INSTANCE.inboundRules.remove(i);
            }
        }
    }
}


