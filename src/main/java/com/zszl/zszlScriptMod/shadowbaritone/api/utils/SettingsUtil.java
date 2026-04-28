/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zszl.zszlScriptMod.shadowbaritone.api.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.Settings;
import com.zszl.zszlScriptMod.system.ProfileManager;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsUtil {

    public static final String SETTINGS_DEFAULT_NAME = "setting.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
    private static final Pattern SETTING_PATTERN = Pattern.compile("^(?<setting>[^ ]+) +(?<value>.+)"); // key and value split by the first space


    private static boolean isComment(String line) {
        return line.startsWith("#") || line.startsWith("//");
    }

    private static void forEachLine(Path file, Consumer<String> consumer) throws IOException {
        try (BufferedReader scan = Files.newBufferedReader(file)) {
            String line;
            while ((line = scan.readLine()) != null) {
                if (line.isEmpty() || isComment(line)) {
                    continue;
                }
                consumer.accept(line);
            }
        }
    }

    public static void readAndApply(Settings settings, String settingsName) {
        try {
            Path file = settingsByName(settingsName);
            if (settingsName.toLowerCase().endsWith(".json")) {
                readAndApplyFromJson(settings, file);
            } else {
                readAndApplyFromText(settings, file);
            }
        } catch (NoSuchFileException ignored) {
            if (!SETTINGS_DEFAULT_NAME.equalsIgnoreCase(settingsName)) {
                Helper.HELPER.logDirect("ShadowBaritone settings file not found: " + settingsName);
                return;
            }
            try {
                readAndApplyFromText(settings, settingsByName("settings.txt"));
                save(settings);
                Helper.HELPER.logDirect("Loaded legacy settings.txt and migrated it to setting.json");
            } catch (NoSuchFileException ignoredLegacy) {
                Helper.HELPER.logDirect("ShadowBaritone settings file not found, resetting.");
            } catch (Exception ex) {
                Helper.HELPER.logDirect("Exception while reading ShadowBaritone settings, some settings may be reset to default values!");
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            Helper.HELPER.logDirect("Exception while reading ShadowBaritone settings, some settings may be reset to default values!");
            ex.printStackTrace();
        }
    }

    public static synchronized void save(Settings settings) {
        Path target = settingsByName(SETTINGS_DEFAULT_NAME);
        try {
            Files.createDirectories(target.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(target)) {
                GSON.toJson(
                        modifiedSettings(settings).stream().collect(Collectors.toMap(Settings.Setting::getName, SettingsUtil::settingValueToString)),
                        STRING_MAP_TYPE,
                        out
                );
            }
        } catch (Exception ex) {
            Helper.HELPER.logDirect("Exception thrown while saving ShadowBaritone settings!");
            ex.printStackTrace();
        }
    }

    private static Path settingsByName(String name) {
        return ProfileManager.getCurrentProfileDir().resolve("shadowbaritone").resolve(name);
    }

    private static void readAndApplyFromText(Settings settings, Path file) throws IOException {
        forEachLine(file, line -> {
            Matcher matcher = SETTING_PATTERN.matcher(line);
            if (!matcher.matches()) {
                Helper.HELPER.logDirect("Invalid syntax in setting file: " + line);
                return;
            }

            String settingName = matcher.group("setting").toLowerCase();
            String settingValue = matcher.group("value");
            if ("allowjumpat256".equals(settingName)) {
                settingName = "allowjumpatbuildlimit";
            }
            try {
                parseAndApply(settings, settingName, settingValue);
            } catch (Exception ex) {
                Helper.HELPER.logDirect("Unable to parse line " + line);
                ex.printStackTrace();
            }
        });
    }

    private static void readAndApplyFromJson(Settings settings, Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            Map<String, String> loaded = GSON.fromJson(reader, STRING_MAP_TYPE);
            if (loaded == null) {
                return;
            }
            for (Map.Entry<String, String> entry : loaded.entrySet()) {
                String settingName = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
                String settingValue = entry.getValue() == null ? "" : entry.getValue();
                if (settingName.isEmpty()) {
                    continue;
                }
                if ("allowjumpat256".equals(settingName)) {
                    settingName = "allowjumpatbuildlimit";
                }
                try {
                    parseAndApply(settings, settingName, settingValue);
                } catch (Exception ex) {
                    Helper.HELPER.logDirect("Unable to parse setting " + settingName + "=" + settingValue);
                    ex.printStackTrace();
                }
            }
        }
    }

    public static List<Settings.Setting> modifiedSettings(Settings settings) {
        List<Settings.Setting> modified = new ArrayList<>();
        for (Settings.Setting setting : settings.allSettings) {
            if (setting.value == null) {
                System.out.println("NULL SETTING?" + setting.getName());
                continue;
            }
            if (setting.isJavaOnly()) {
                continue; // NO
            }
            if (setting.value == setting.defaultValue) {
                continue;
            }
            modified.add(setting);
        }
        return modified;
    }

    /**
     * Gets the type of a setting and returns it as a string, with package names stripped.
     * <p>
     * For example, if the setting type is {@code java.util.List<java.lang.String>}, this function returns
     * {@code List<String>}.
     *
     * @param setting The setting
     * @return The type
     */
    public static String settingTypeToString(Settings.Setting setting) {
        return setting.getType().getTypeName()
                .replaceAll("(?:\\w+\\.)+(\\w+)", "$1");
    }

    public static <T> String settingValueToString(Settings.Setting<T> setting, T value) throws IllegalArgumentException {
        Parser io = Parser.getParser(setting.getType());

        if (io == null) {
            throw new IllegalStateException("Missing " + setting.getValueClass() + " " + setting.getName());
        }

        return io.toString(setting.getType(), value);
    }

    public static String settingValueToString(Settings.Setting setting) throws IllegalArgumentException {
        //noinspection unchecked
        return settingValueToString(setting, setting.value);
    }

    public static String settingDefaultToString(Settings.Setting setting) throws IllegalArgumentException {
        //noinspection unchecked
        return settingValueToString(setting, setting.defaultValue);
    }

    public static String maybeCensor(int coord) {
        if (BaritoneAPI.getSettings().censorCoordinates.value) {
            return "<censored>";
        }

        return Integer.toString(coord);
    }

    public static String settingToString(Settings.Setting setting) throws IllegalStateException {
        if (setting.isJavaOnly()) {
            return setting.getName();
        }

        return setting.getName() + " " + settingValueToString(setting);
    }

    /**
     * Deprecated. Use {@link Settings.Setting#isJavaOnly()} instead.
     *
     * @param setting The Setting
     * @return true if the setting can not be set or read by the user
     */
    @Deprecated
    public static boolean javaOnlySetting(Settings.Setting setting) {
        return setting.isJavaOnly();
    }

    public static void parseAndApply(Settings settings, String settingName, String settingValue) throws IllegalStateException, NumberFormatException {
        Settings.Setting setting = settings.byLowerName.get(settingName);
        if (setting == null) {
            throw new IllegalStateException("No setting by that name");
        }
        Class intendedType = setting.getValueClass();
        ISettingParser ioMethod = Parser.getParser(setting.getType());
        Object parsed = ioMethod.parse(setting.getType(), settingValue);
        if (!intendedType.isInstance(parsed)) {
            throw new IllegalStateException(ioMethod + " parser returned incorrect type, expected " + intendedType + " got " + parsed + " which is " + parsed.getClass());
        }
        setting.value = parsed;
    }

    private interface ISettingParser<T> {

        T parse(Type type, String raw);

        String toString(Type type, T value);

        boolean accepts(Type type);
    }

    private enum Parser implements ISettingParser {

        DOUBLE(Double.class, Double::parseDouble),
        BOOLEAN(Boolean.class, Boolean::parseBoolean),
        INTEGER(Integer.class, Integer::parseInt),
        FLOAT(Float.class, Float::parseFloat),
        LONG(Long.class, Long::parseLong),
        STRING(String.class, String::new),
        MIRROR(Mirror.class, Mirror::valueOf, Mirror::name),
        ROTATION(Rotation.class, Rotation::valueOf, Rotation::name),
        COLOR(
                Color.class,
                str -> new Color(Integer.parseInt(str.split(",")[0]), Integer.parseInt(str.split(",")[1]), Integer.parseInt(str.split(",")[2])),
                color -> color.getRed() + "," + color.getGreen() + "," + color.getBlue()
        ),
        VEC3I(
                Vec3i.class,
                str -> new Vec3i(Integer.parseInt(str.split(",")[0]), Integer.parseInt(str.split(",")[1]), Integer.parseInt(str.split(",")[2])),
                vec -> vec.getX() + "," + vec.getY() + "," + vec.getZ()
        ),
        BLOCK(
                Block.class,
                str -> BlockUtils.stringToBlockRequired(str.trim()),
                BlockUtils::blockToString
        ),
        ITEM(
                Item.class,
                str -> BuiltInRegistries.ITEM.getValue(Identifier.parse(str.trim())), // TODO this now returns AIR on failure instead of null, is that an issue?
                item -> {
                    Identifier key = item == null ? null : BuiltInRegistries.ITEM.getKey(item);
                    return key == null ? "minecraft:air" : key.toString();
                }
        ),
        LIST() {
            @Override
            public Object parse(Type type, String raw) {
                Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Parser parser = Parser.getParser(elementType);
                return Stream.of(raw.split(","))
                        .map(s -> parser.parse(elementType, s))
                        .collect(Collectors.toList());
            }

            @Override
            public String toString(Type type, Object value) {
                Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Parser parser = Parser.getParser(elementType);

                return ((List<?>) value).stream()
                        .map(o -> parser.toString(elementType, o))
                        .collect(Collectors.joining(","));
            }

            @Override
            public boolean accepts(Type type) {
                return List.class.isAssignableFrom(TypeUtils.resolveBaseClass(type));
            }
        },
        MAPPING() {
            @Override
            public Object parse(Type type, String raw) {
                Type keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
                Parser keyParser = Parser.getParser(keyType);
                Parser valueParser = Parser.getParser(valueType);

                return Stream.of(raw.split(",(?=[^,]*->)"))
                        .map(s -> s.split("->"))
                        .collect(Collectors.toMap(s -> keyParser.parse(keyType, s[0]), s -> valueParser.parse(valueType, s[1])));
            }

            @Override
            public String toString(Type type, Object value) {
                Type keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
                Parser keyParser = Parser.getParser(keyType);
                Parser valueParser = Parser.getParser(valueType);

                return ((Map<?, ?>) value).entrySet().stream()
                        .map(o -> keyParser.toString(keyType, o.getKey()) + "->" + valueParser.toString(valueType, o.getValue()))
                        .collect(Collectors.joining(","));
            }

            @Override
            public boolean accepts(Type type) {
                return Map.class.isAssignableFrom(TypeUtils.resolveBaseClass(type));
            }
        };

        private final Class<?> cla$$;
        private final Function<String, Object> parser;
        private final Function<Object, String> toString;

        Parser() {
            this.cla$$ = null;
            this.parser = null;
            this.toString = null;
        }

        <T> Parser(Class<T> cla$$, Function<String, T> parser) {
            this(cla$$, parser, Object::toString);
        }

        <T> Parser(Class<T> cla$$, Function<String, T> parser, Function<T, String> toString) {
            this.cla$$ = cla$$;
            this.parser = parser::apply;
            this.toString = x -> toString.apply((T) x);
        }

        @Override
        public Object parse(Type type, String raw) {
            Object parsed = this.parser.apply(raw);
            Objects.requireNonNull(parsed);
            return parsed;
        }

        @Override
        public String toString(Type type, Object value) {
            return this.toString.apply(value);
        }

        @Override
        public boolean accepts(Type type) {
            return type instanceof Class && this.cla$$.isAssignableFrom((Class) type);
        }

        public static Parser getParser(Type type) {
            return Stream.of(values())
                    .filter(parser -> parser.accepts(type))
                    .findFirst().orElse(null);
        }
    }
}

