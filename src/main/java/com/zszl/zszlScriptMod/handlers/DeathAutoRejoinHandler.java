package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.system.ProfileManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class DeathAutoRejoinHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean deathAutoRejoinEnabled = false;
    public static int deathAutoRejoinLineMode = 0; // 0=普通线, 1=隐藏线
    public static boolean deathAutoResumeLastPath = false;
    public static int deathAutoResumeMode = 0; // 0=继续, 1=从头
    public static int deathAutoTeleportDetectMs = 2000;

    public static final double DEATH_RESPAWN_DEFAULT_X = 229.0;
    public static final double DEATH_RESPAWN_DEFAULT_Y = 7.0;
    public static final double DEATH_RESPAWN_DEFAULT_Z = -502.0;
    public static final double DEATH_RESPAWN_DEFAULT_RADIUS = 10.0;

    public static double deathRespawnCenterX = DEATH_RESPAWN_DEFAULT_X;
    public static double deathRespawnCenterY = DEATH_RESPAWN_DEFAULT_Y;
    public static double deathRespawnCenterZ = DEATH_RESPAWN_DEFAULT_Z;
    public static double deathRespawnRadius = DEATH_RESPAWN_DEFAULT_RADIUS;

    static {
        loadConfig();
    }

    private DeathAutoRejoinHandler() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_leaveconfig.json");
    }

    public static void saveConfig() {
        try {
            Path configFile = getConfigFile();
            JsonObject json = new JsonObject();
            json.addProperty("deathAutoRejoinEnabled", deathAutoRejoinEnabled);
            json.addProperty("deathAutoRejoinLineMode", deathAutoRejoinLineMode);
            json.addProperty("deathAutoResumeLastPath", deathAutoResumeLastPath);
            json.addProperty("deathAutoResumeMode", deathAutoResumeMode);
            json.addProperty("deathAutoTeleportDetectMs", deathAutoTeleportDetectMs);
            json.addProperty("deathRespawnCenterX", deathRespawnCenterX);
            json.addProperty("deathRespawnCenterY", deathRespawnCenterY);
            json.addProperty("deathRespawnCenterZ", deathRespawnCenterZ);
            json.addProperty("deathRespawnRadius", deathRespawnRadius);

            Files.createDirectories(configFile.getParent());

            Path tempFile = Files.createTempFile(configFile.getParent(), "death_auto_rejoin", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(json));
            }
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            zszlScriptMod.LOGGER.info("死亡自动重进配置已保存。");
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存死亡自动重进配置失败", e);
        }
    }

    public static void loadConfig() {
        try {
            Path configFile = getConfigFile();
            if (!Files.exists(configFile)) {
                resetToDefaults();
                return;
            }

            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();

                deathAutoRejoinEnabled = json.has("deathAutoRejoinEnabled")
                        && json.get("deathAutoRejoinEnabled").getAsBoolean();
                deathAutoRejoinLineMode = json.has("deathAutoRejoinLineMode")
                        ? clamp(json.get("deathAutoRejoinLineMode").getAsInt(), 0, 1)
                        : 0;
                deathAutoResumeLastPath = json.has("deathAutoResumeLastPath")
                        && json.get("deathAutoResumeLastPath").getAsBoolean();
                deathAutoResumeMode = json.has("deathAutoResumeMode")
                        ? clamp(json.get("deathAutoResumeMode").getAsInt(), 0, 1)
                        : 0;
                deathAutoTeleportDetectMs = json.has("deathAutoTeleportDetectMs")
                        ? Math.max(200, json.get("deathAutoTeleportDetectMs").getAsInt())
                        : 2000;
                deathRespawnCenterX = json.has("deathRespawnCenterX")
                        ? json.get("deathRespawnCenterX").getAsDouble()
                        : DEATH_RESPAWN_DEFAULT_X;
                deathRespawnCenterY = json.has("deathRespawnCenterY")
                        ? json.get("deathRespawnCenterY").getAsDouble()
                        : DEATH_RESPAWN_DEFAULT_Y;
                deathRespawnCenterZ = json.has("deathRespawnCenterZ")
                        ? json.get("deathRespawnCenterZ").getAsDouble()
                        : DEATH_RESPAWN_DEFAULT_Z;
                deathRespawnRadius = json.has("deathRespawnRadius")
                        ? Math.max(1.0, json.get("deathRespawnRadius").getAsDouble())
                        : DEATH_RESPAWN_DEFAULT_RADIUS;
            }

            zszlScriptMod.LOGGER.info("死亡自动重进配置已加载。");
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载死亡自动重进配置失败", e);
            resetToDefaults();
        }
    }

    public static void resetRespawnDefaults() {
        deathRespawnCenterX = DEATH_RESPAWN_DEFAULT_X;
        deathRespawnCenterY = DEATH_RESPAWN_DEFAULT_Y;
        deathRespawnCenterZ = DEATH_RESPAWN_DEFAULT_Z;
        deathRespawnRadius = DEATH_RESPAWN_DEFAULT_RADIUS;
    }

    public static String getRejoinSequenceName() {
        return deathAutoRejoinLineMode == 1 ? "进入已点击的副本-隐藏" : "进入已点击的副本";
    }

    private static void resetToDefaults() {
        deathAutoRejoinEnabled = false;
        deathAutoRejoinLineMode = 0;
        deathAutoResumeLastPath = false;
        deathAutoResumeMode = 0;
        deathAutoTeleportDetectMs = 2000;
        resetRespawnDefaults();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
