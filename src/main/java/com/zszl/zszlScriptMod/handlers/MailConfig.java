package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MailConfig {
    public static final MailConfig INSTANCE = new MailConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean autoReceiveEnabled = true;
    public boolean amountFilterEnabled = true;
    public int waitTimeoutTicks = 5;
    public int autoReceiveMaxGold = 0;
    public int autoReceiveMaxCoupon = 0;

    private MailConfig() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("mail_settings.json");
    }

    public static void save() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.mail_config.save_failed"), e);
        }
    }

    public static void load() {
        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                MailConfig loadedConfig = GSON.fromJson(reader, MailConfig.class);
                if (loadedConfig != null) {
                    INSTANCE.autoReceiveEnabled = loadedConfig.autoReceiveEnabled;
                    INSTANCE.amountFilterEnabled = loadedConfig.amountFilterEnabled;
                    INSTANCE.waitTimeoutTicks = loadedConfig.waitTimeoutTicks > 0 ? loadedConfig.waitTimeoutTicks : 5;
                    INSTANCE.autoReceiveMaxGold = Math.max(0, loadedConfig.autoReceiveMaxGold);
                    INSTANCE.autoReceiveMaxCoupon = Math.max(0, loadedConfig.autoReceiveMaxCoupon);
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error(I18n.format("log.mail_config.load_failed"), e);
            }
        }
    }
}



