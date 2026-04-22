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

public class FreecamHandler {

    public static final FreecamHandler INSTANCE = new FreecamHandler();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean isFastAttackEnabled = false;
    public static int ghostSoulCount = 1;
    public static boolean enableNoCollision = true;
    public static boolean enableAntiKnockback = true;
    public static boolean enableGhostEntity = true;

    private static final class ConfigData {
        boolean isFastAttackEnabled;
        int ghostSoulCount;
        boolean enableNoCollision;
        boolean enableAntiKnockback;
        boolean enableGhostEntity;
    }

    static {
        loadConfig();
    }

    private FreecamHandler() {
    }

    public static void loadConfig() {
        INSTANCE.isFastAttackEnabled = false;
        ghostSoulCount = 1;
        enableNoCollision = true;
        enableAntiKnockback = true;
        enableGhostEntity = true;

        Path file = ProfileManager.getCurrentProfileDir().resolve("freecam_fast_attack.json");
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                INSTANCE.isFastAttackEnabled = data.isFastAttackEnabled;
                ghostSoulCount = Math.max(1, data.ghostSoulCount);
                enableNoCollision = data.enableNoCollision;
                enableAntiKnockback = data.enableAntiKnockback;
                enableGhostEntity = data.enableGhostEntity;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载快速攻击配置失败", e);
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("freecam_fast_attack.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.isFastAttackEnabled = INSTANCE.isFastAttackEnabled;
                data.ghostSoulCount = ghostSoulCount;
                data.enableNoCollision = enableNoCollision;
                data.enableAntiKnockback = enableAntiKnockback;
                data.enableGhostEntity = enableGhostEntity;
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存快速攻击配置失败", e);
        }
    }

    public void toggleFastAttack() {
        isFastAttackEnabled = !isFastAttackEnabled;
        saveConfig();
    }
}
