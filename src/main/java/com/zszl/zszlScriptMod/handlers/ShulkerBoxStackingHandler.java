package com.zszl.zszlScriptMod.handlers;

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

public class ShulkerBoxStackingHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean autoStackingEnabled = false;
    public static List<String> stackableItemKeywords = new ArrayList<>();

    public static final class StackingLogEntry {
        public int sourceSlot;
        public int destinationSlot;
        public byte[] sessionID;
        public String pickupHexPayload = "";
        public String placeHexPayload = "";
    }

    private static final class ConfigData {
        boolean autoStackingEnabled;
        List<String> stackableItemKeywords;
    }

    static {
        loadConfig();
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_stacking_shulker.json");
    }

    public static void loadConfig() {
        autoStackingEnabled = false;
        stackableItemKeywords = new ArrayList<>();

        Path file = getConfigFile();
        if (!Files.exists(file)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                autoStackingEnabled = data.autoStackingEnabled;
                if (data.stackableItemKeywords != null) {
                    stackableItemKeywords = new ArrayList<>(data.stackableItemKeywords);
                }
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载自动潜影盒堆叠配置失败", e);
        }
    }

    public static void saveConfig() {
        ConfigData data = new ConfigData();
        data.autoStackingEnabled = autoStackingEnabled;
        data.stackableItemKeywords = new ArrayList<>(stackableItemKeywords);

        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动潜影盒堆叠配置失败", e);
        }
    }
}
