package com.zszl.zszlScriptMod.shadowbaritone.api.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.ICommand;
import net.minecraft.client.resources.language.I18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class ShadowBaritoneI18n {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {
    }.getType();
    private static volatile Map<String, String> shadowbaritoneTranslations;

    private ShadowBaritoneI18n() {
    }

    public static String tr(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        try {
            String translated = I18n.get(raw);
            if (translated != null && !translated.equals(raw)) {
                return translated;
            }
        } catch (Throwable ignored) {
        }
        return getShadowbaritoneTranslations().getOrDefault(raw, raw);
    }

    public static String trKey(String key, Object... args) {
        return trKeyOrDefault(key, key, args);
    }

    public static String trKeyOrDefault(String key, String fallback, Object... args) {
        if (key == null || key.isEmpty()) {
            return safeFormat(fallback, args);
        }
        try {
            String translated = I18n.get(key, args);
            if (translated != null && !translated.equals(key)) {
                return translated;
            }
        } catch (Throwable ignored) {
        }
        String local = getShadowbaritoneTranslations().get(key);
        if (local != null) {
            return safeFormat(local, args);
        }
        return safeFormat(fallback == null ? key : fallback, args);
    }

    public static String trCommandShortDesc(ICommand command) {
        String key = getCommandKeyPrefix(command) + ".short_desc";
        return trKeyOrDefault(key, tr(command.getShortDesc()));
    }

    public static List<String> trCommandLongDesc(ICommand command) {
        String prefix = getCommandKeyPrefix(command) + ".long_desc.";
        Map<String, String> translations = getShadowbaritoneTranslations();
        List<String> lines = new ArrayList<>();

        int numberedIndex = 1;
        while (true) {
            String numbered = translations.get(prefix + numberedIndex);
            if (numbered == null) {
                break;
            }
            lines.add(numbered);
            numberedIndex++;
        }

        String usage = translations.get(prefix + "usage");
        if (usage != null) {
            if (!lines.isEmpty()) {
                lines.add("");
            }
            lines.add(usage);
        }

        translations.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix + "example."))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(lines::add);

        if (!lines.isEmpty()) {
            return lines;
        }

        return command.getLongDesc().stream()
                .map(ShadowBaritoneI18n::tr)
                .toList();
    }

    private static String getCommandKeyPrefix(ICommand command) {
        String primaryName = command.getNames().isEmpty() ? "unknown" : command.getNames().get(0);
        return "shadowbaritone.command." + primaryName.toLowerCase(Locale.ROOT);
    }

    private static String safeFormat(String pattern, Object... args) {
        if (pattern == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern, args);
        } catch (Throwable ignored) {
            return pattern;
        }
    }

    private static Map<String, String> getShadowbaritoneTranslations() {
        Map<String, String> translations = shadowbaritoneTranslations;
        if (translations != null) {
            return translations;
        }
        synchronized (ShadowBaritoneI18n.class) {
            if (shadowbaritoneTranslations == null) {
                shadowbaritoneTranslations = loadShadowbaritoneTranslations();
            }
            return shadowbaritoneTranslations;
        }
    }

    private static Map<String, String> loadShadowbaritoneTranslations() {
        Map<String, String> json = tryLoadJson("assets/shadowbaritone/lang/zh_cn.json");
        if (!json.isEmpty()) {
            return json;
        }
        Map<String, String> lang = tryLoadLang("assets/shadowbaritone/lang/zh_cn.lang");
        if (!lang.isEmpty()) {
            return lang;
        }
        return Collections.emptyMap();
    }

    private static Map<String, String> tryLoadJson(String resourcePath) {
        try (InputStream stream = ShadowBaritoneI18n.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> parsed = GSON.fromJson(reader, MAP_TYPE);
                if (parsed == null || parsed.isEmpty()) {
                    return Collections.emptyMap();
                }
                return Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
            }
        } catch (Throwable ignored) {
            return Collections.emptyMap();
        }
    }

    private static Map<String, String> tryLoadLang(String resourcePath) {
        try (InputStream stream = ShadowBaritoneI18n.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Collections.emptyMap();
            }
            Properties properties = new Properties();
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            Map<String, String> ordered = new LinkedHashMap<>();
            for (String key : properties.stringPropertyNames()) {
                ordered.put(key, properties.getProperty(key));
            }
            return Collections.unmodifiableMap(ordered);
        } catch (Throwable ignored) {
            return Collections.emptyMap();
        }
    }
}
