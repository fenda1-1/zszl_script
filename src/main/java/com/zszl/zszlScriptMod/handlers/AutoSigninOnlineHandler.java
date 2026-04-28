package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.path.PathSequenceManager.ActionData;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathSequence;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.TickRangeSpec;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class AutoSigninOnlineHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = false;
    public static boolean signinEnabled = true;
    public static boolean onlineEnabled = true;

    private static final String SIGNIN_SEQUENCE_NAME = "签到";
    private static final String ONLINE_SEQUENCE_NAME = "在线";

    private static long lastRunAtMs = 0L;
    private static final long RUN_INTERVAL_MS = 1000L;
    private static long lastMissingWarnAtMs = 0L;

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_signin_online_config.json");
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
        if (enabled) {
            lastRunAtMs = 0L;
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(TextFormatting.GREEN + "[签到/在线] 后台功能已开启。"));
            }
        } else {
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "[签到/在线] 后台功能已关闭。"));
            }
        }
        saveConfig();
    }

    public static void tick() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRunAtMs < RUN_INTERVAL_MS) {
            return;
        }
        lastRunAtMs = now;

        if (signinEnabled) {
            runSequenceInBackgroundByName(SIGNIN_SEQUENCE_NAME);
        }
        if (onlineEnabled) {
            runSequenceInBackgroundByName(ONLINE_SEQUENCE_NAME);
        }
    }

    public static void stop() {
        enabled = false;
    }

    private static void runSequenceInBackgroundByName(String seqName) {
        PathSequence sequence = PathSequenceManager.getSequence(seqName);
        if (sequence == null) {
            if (System.currentTimeMillis() - lastMissingWarnAtMs > 5000L) {
                lastMissingWarnAtMs = System.currentTimeMillis();
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "[签到/在线] 找不到序列: " + seqName + "，请右键进入设置检查。"));
                }
            }
            return;
        }

        EntityPlayerSP player = mc.player;
        int accumulatedDelayTicks = 0;

        for (PathStep step : sequence.getSteps()) {
            for (ActionData actionData : step.getActions()) {
                if (actionData == null || actionData.type == null) {
                    continue;
                }

                if ("delay".equalsIgnoreCase(actionData.type)) {
                    int delay = 0;
                    try {
                        if (actionData.params != null && actionData.params.has("ticks")) {
                            delay = TickRangeSpec.sample(actionData.params.get("ticks"), 0, 0,
                                    Integer.MAX_VALUE - 1);
                        }
                    } catch (Exception ignore) {
                    }
                    accumulatedDelayTicks += delay;
                    continue;
                }

                Consumer<EntityPlayerSP> action = PathSequenceManager.parseAction(actionData.type, actionData.params);
                if (action == null) {
                    continue;
                }

                final Consumer<EntityPlayerSP> finalAction = action;
                if (accumulatedDelayTicks > 0) {
                    if (ModUtils.DelayScheduler.instance == null) {
                        continue;
                    }
                    ModUtils.DelayScheduler.instance.schedule(() -> {
                        if (mc.player != null && mc.world != null) {
                            try {
                                finalAction.accept(mc.player);
                            } catch (Exception e) {
                                zszlScriptMod.LOGGER.error("执行后台序列动作失败: {}", actionData.type, e);
                            }
                        }
                    }, accumulatedDelayTicks, "auto-signin-online");
                } else {
                    try {
                        finalAction.accept(player);
                    } catch (Exception e) {
                        zszlScriptMod.LOGGER.error("执行后台序列动作失败: {}", actionData.type, e);
                    }
                }
            }
        }
    }

    private static class ConfigData {
        boolean enabled = false;
        boolean signinEnabled = true;
        boolean onlineEnabled = true;
    }
}
