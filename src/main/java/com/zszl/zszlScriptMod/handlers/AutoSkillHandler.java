// 文件路径: src/main/java/com/zszl/zszlScriptMod/handlers/AutoSkillHandler.java
package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler; // 导入PacketCaptureHandler
import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoSkillHandler {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACKET_CHANNEL = "OwlControlChannel";

    public static class Skill {
        public String name;
        public boolean enabled;
        public int cooldownSeconds;
        public List<String> hexPayloads;
        public transient long lastUsedTimestamp;

        public Skill(String name, int cooldown, List<String> payloads) {
            this.name = name;
            this.enabled = false;
            this.cooldownSeconds = cooldown;
            this.hexPayloads = payloads;
            this.lastUsedTimestamp = 0;
        }
    }

    public static boolean autoSkillEnabled = false;
    public static List<Skill> skills = new ArrayList<>();

    // --- 新增：用于处理ID未获取时的消息冷却 ---
    private static int idMissingMessageCooldown = 0;

    static {
        loadSkillConfig();
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_skills_v2.json");
    }

    public static void loadSkillConfig() {
        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                ConfigWrapper wrapper = GSON.fromJson(reader, ConfigWrapper.class);
                if (wrapper != null && wrapper.skills != null && wrapper.skills.size() == 4) {
                    skills = wrapper.skills;
                    autoSkillEnabled = wrapper.autoSkillEnabled;
                    if (ensureDefaultHexPayloads()) {
                        saveSkillConfig();
                    }
                } else {
                    initializeDefaultSkills();
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("加载自动技能配置失败，将使用默认配置。", e);
                initializeDefaultSkills();
            }
        } else {
            initializeDefaultSkills();
        }
    }

    public static void saveSkillConfig() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                ConfigWrapper wrapper = new ConfigWrapper();
                wrapper.autoSkillEnabled = autoSkillEnabled;
                wrapper.skills = skills;
                GSON.toJson(wrapper, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动技能配置失败。", e);
        }
    }

    private static class ConfigWrapper {
        boolean autoSkillEnabled;
        List<Skill> skills;
    }

    private static void initializeDefaultSkills() {
        skills = buildDefaultSkills();
        autoSkillEnabled = false;
        saveSkillConfig();
    }

    private static List<Skill> buildDefaultSkills() {
        List<Skill> defaults = new ArrayList<>();
        // --- 核心修改：使用占位符 {id} 替换硬编码的ID ---
        defaults.add(new Skill("R", 1, new ArrayList<>(Arrays.asList(
                "00 10 43 6F 6E 74 72 6F 6C 5F 6B 65 79 62 6F 61 72 64 00 00 {id} 00 16 43 6F 6E 74 72 6F 6C 5F 73 65 74 5F 6B 65 79 62 6F 61 72 64 4F 6E 00 0A 52 E9 94 AE E6 8C 89 E4 B8 8B 00 00 00 01 00 00 00 13"))));
        defaults.add(new Skill("Z", 1, new ArrayList<>(Arrays.asList(
                "00 10 43 6F 6E 74 72 6F 6C 5F 6B 65 79 62 6F 61 72 64 00 00 {id} 00 16 43 6F 6E 74 72 6F 6C 5F 73 65 74 5F 6B 65 79 62 6F 61 72 64 4F 6E 00 0A 5A E9 94 AE E6 8C 89 E4 B8 8B 00 00 00 01 00 00 00 2C"))));
        defaults.add(new Skill("X", 1, new ArrayList<>(Arrays.asList(
                "00 10 43 6F 6E 74 72 6F 6C 5F 6B 65 79 62 6F 61 72 64 00 00 {id} 00 16 43 6F 6E 74 72 6F 6C 5F 73 65 74 5F 6B 65 79 62 6F 61 72 64 4F 6E 00 0A 58 E9 94 AE E6 8C 89 E4 B8 8B 00 00 00 01 00 00 00 2D"))));
        defaults.add(new Skill("C", 1, new ArrayList<>(Arrays.asList(
                "00 10 43 6F 6E 74 72 6F 6C 5F 6B 65 79 62 6F 61 72 64 00 00 {id} 00 16 43 6F 6E 74 72 6F 6C 5F 73 65 74 5F 6B 65 79 62 6F 61 72 64 4F 6E 00 0A 43 E9 94 AE E6 8C 89 E4 B8 8B 00 00 00 01 00 00 00 2E"))));
        return defaults;
    }

    private static boolean ensureDefaultHexPayloads() {
        List<Skill> defaults = buildDefaultSkills();
        if (skills == null || skills.size() != defaults.size()) {
            skills = defaults;
            return true;
        }

        boolean changed = false;
        for (int i = 0; i < skills.size(); i++) {
            Skill current = skills.get(i);
            Skill def = defaults.get(i);

            if (current.hexPayloads == null) {
                current.hexPayloads = new ArrayList<>();
                changed = true;
            }

            String defaultHex = def.hexPayloads.get(0);
            if (current.hexPayloads.isEmpty()) {
                current.hexPayloads.add(defaultHex);
                changed = true;
            } else if (!defaultHex.equals(current.hexPayloads.get(0))) {
                int existingIndex = current.hexPayloads.indexOf(defaultHex);
                if (existingIndex >= 0) {
                    current.hexPayloads.remove(existingIndex);
                    current.hexPayloads.add(0, defaultHex);
                } else {
                    current.hexPayloads.add(0, defaultHex);
                }
                changed = true;
            }
        }

        return changed;
    }

    public static void updateAutoSkills() {
        if (!autoSkillEnabled || mc.player == null || mc.getConnection() == null) {
            return;
        }

        // --- 新增：处理ID未获取时的消息冷却 ---
        if (idMissingMessageCooldown > 0) {
            idMissingMessageCooldown--;
        }

        long currentTime = System.currentTimeMillis();

        for (Skill skill : skills) {
            if (skill.enabled && (currentTime - skill.lastUsedTimestamp) >= (skill.cooldownSeconds * 1000L)) {

                // --- 核心修改：在尝试发送前检查会话ID ---
                if (PacketCaptureHandler.getOwlViewSessionID() == null) {
                    if (idMissingMessageCooldown <= 0) {
                        mc.player.sendSystemMessage(
                                new TextComponentString(ChatFormatting.RED + "[自动技能] 未获取到当前会话ID，请打开一次背包(默认E键)以捕获ID。"));
                        idMissingMessageCooldown = 100; // 5秒冷却 (5 * 20 ticks)
                    }
                    return; // 阻止技能触发
                }

                if (ModConfig.isDebugFlagEnabled(DebugModule.AUTO_SKILL)) {
                    zszlScriptMod.LOGGER.info("[自动技能] 触发技能: {}, CD: {}s", skill.name, skill.cooldownSeconds);
                }

                skill.lastUsedTimestamp = currentTime;
                sendSkillPackets(skill);
                return; // 每次tick只触发一个技能
            }
        }
    }

    public static void sendSkillPackets(Skill skill) {
        if (skill.hexPayloads == null || skill.hexPayloads.isEmpty()) {
            return;
        }

        // --- 核心修改：获取会话ID并替换占位符 ---
        String idAsHexString = PacketCaptureHandler.getSessionIdAsHex();
        if (idAsHexString == null) {
            // 这个检查是双重保险，理论上 updateAutoSkills 已经处理了
            return;
        }

        for (int i = 0; i < skill.hexPayloads.size(); i++) {
            final String originalHex = skill.hexPayloads.get(i);

            // 替换占位符
            final String finalHex = originalHex.replace("{id}", idAsHexString);

            ModUtils.DelayScheduler.instance.schedule(() -> {
                try {
                    ModUtils.sendFmlPacket(PACKET_CHANNEL, finalHex);
                } catch (Exception e) {
                    zszlScriptMod.LOGGER.error("[自动技能] 发送数据包失败 for skill " + skill.name, e);
                }
            }, i + 1);
        }
    }
}





