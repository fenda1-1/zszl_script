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

public class AutoSigninOnlineHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean enabled = false;
    public static boolean signinEnabled = true;
    public static boolean onlineEnabled = true;

    private static final class ConfigData {
        boolean enabled;
        boolean signinEnabled;
        boolean onlineEnabled;
    }

    public static void loadConfig() {
        Path configFile = getConfigFile();
        if (!Files.exists(configFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                enabled = data.enabled;
                signinEnabled = data.signinEnabled;
                onlineEnabled = data.onlineEnabled;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载签到/在线后台配置失败", e);
        }
    }

    public static void saveConfig() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.enabled = enabled;
                data.signinEnabled = signinEnabled;
                data.onlineEnabled = onlineEnabled;
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存签到/在线后台配置失败", e);
        }
    }

    public static void toggle() {
        enabled = !enabled;
        saveConfig();
    }

    public static void tick() {
    }

    public static void stop() {
        enabled = false;
        saveConfig();
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_signin_online_config.json");
    }
}
