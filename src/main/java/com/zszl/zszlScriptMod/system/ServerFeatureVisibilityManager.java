package com.zszl.zszlScriptMod.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ServerFeatureVisibilityManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ServerFeatureRule {
        public String id;
        public String name;
        public boolean enabled;

        public ServerFeatureRule(String id, String name, boolean enabled) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
        }
    }

    public static boolean globalEnabled = true;
    public static final List<ServerFeatureRule> rules = new ArrayList<>();

    private ServerFeatureVisibilityManager() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("server_feature_visibility.json");
    }

    private static void ensureDefaults() {
        if (rules.stream().noneMatch(r -> Objects.equals(r.id, "mota"))) {
            rules.add(new ServerFeatureRule("mota", "魔塔之巅", true));
        }
    }

    public static void loadConfig() {
        rules.clear();
        Path file = getConfigFile();
        if (!Files.exists(file)) {
            ensureDefaults();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("globalEnabled")) {
                globalEnabled = root.get("globalEnabled").getAsBoolean();
            }
            if (root.has("rules")) {
                Type listType = new TypeToken<ArrayList<ServerFeatureRule>>() {
                }.getType();
                List<ServerFeatureRule> loaded = GSON.fromJson(root.get("rules"), listType);
                if (loaded != null) {
                    rules.addAll(loaded);
                }
            }
            ensureDefaults();
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载服务器功能可见性配置失败", e);
            rules.clear();
            ensureDefaults();
        }
    }

    public static void saveConfig() {
        try {
            Path file = getConfigFile();
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonObject();
                root.addProperty("globalEnabled", globalEnabled);
                root.add("rules", GSON.toJsonTree(rules));
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存服务器功能可见性配置失败", e);
        }
    }

    public static boolean shouldHideRslFeatures() {
        return false;
    }

    public static boolean shouldHideMotaFeatures() {
        for (ServerFeatureRule r : rules) {
            if ("mota".equals(r.id)) {
                return r.enabled;
            }
        }
        return false;
    }

    public static boolean isAnyRuleEnabled() {
        for (ServerFeatureRule r : rules) {
            if (r != null && r.enabled) {
                return true;
            }
        }
        return false;
    }
}
