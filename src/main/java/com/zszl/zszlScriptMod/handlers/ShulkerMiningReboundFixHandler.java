package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.system.ProfileManager;
import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ShulkerMiningReboundFixHandler {

    public static final ShulkerMiningReboundFixHandler INSTANCE = new ShulkerMiningReboundFixHandler();

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean enabled = false;

    private static final class ConfigData {
        boolean enabled = false;
    }

    private ShulkerMiningReboundFixHandler() {
    }

    public static void loadConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("shulker_mining_rebound_fix_config.json");
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data != null) {
                enabled = data.enabled;
            }
        } catch (Exception ignored) {
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("shulker_mining_rebound_fix_config.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.enabled = enabled;
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static void toggleEnabled() {
        enabled = !enabled;
        saveConfig();
        if (mc.player != null) {
            mc.player.sendSystemMessage(new TextComponentString(enabled
                    ? "§a[常用] 潜影盒回弹修复已开启"
                    : "§c[常用] 潜影盒回弹修复已关闭"));
        }
    }

    public void clearRuntimeState() {
    }
}

