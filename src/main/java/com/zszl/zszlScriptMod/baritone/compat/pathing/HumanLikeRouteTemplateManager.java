package com.zszl.zszlScriptMod.baritone.compat.pathing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class HumanLikeRouteTemplateManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TEMPLATE_LIST_TYPE = new TypeToken<List<Object>>() {
    }.getType();
    private static final String FILE_NAME = "human_like_route_templates.json";

    private HumanLikeRouteTemplateManager() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve(FILE_NAME);
    }

    public static synchronized void load() {
        Path file = getConfigFile();
        if (!Files.exists(file)) {
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            GSON.fromJson(reader, TEMPLATE_LIST_TYPE);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法加载模拟真人路线模板", e);
        }
    }

    public static synchronized void save() {
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(new ArrayList<>(), TEMPLATE_LIST_TYPE, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("无法保存模拟真人路线模板", e);
        }
    }
}

