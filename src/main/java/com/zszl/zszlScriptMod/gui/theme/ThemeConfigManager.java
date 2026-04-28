package com.zszl.zszlScriptMod.gui.theme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ThemeConfigManager {
    private static final String BUILTIN_SCHEME = "builtin:";
    private static final String BUILTIN_SEASIDE_ID = "seaside";
    private static final String BUILTIN_PANEL_IMAGE = BUILTIN_SCHEME + "img/seaside-girl.jpg";
    private static final String BUILTIN_BUTTON_IMAGE = BUILTIN_SCHEME + "img/seaside-cat.jpg";
    private static final String BUILTIN_INPUT_IMAGE = BUILTIN_SCHEME + "img/starlight.jpg";
    private static final String LEGACY_BUILTIN_PANEL_IMAGE = BUILTIN_SCHEME + "img/海边少女.jpg";
    private static final String LEGACY_BUILTIN_BUTTON_IMAGE = BUILTIN_SCHEME + "img/海边猫咪.jpg";
    private static final String LEGACY_BUILTIN_INPUT_IMAGE = BUILTIN_SCHEME + "img/星光.jpg";

    private ThemeConfigManager() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RANDOM = new Random();
    private static final List<ThemeProfile> PROFILES = new ArrayList<>();
    private static int activeIndex = 0;
    private static boolean loaded = false;

    public static class ThemeProfile {
        public String name;
        public boolean builtIn = false;
        public String builtInId = "";

        public int panelBorder;
        public int panelBgTop;
        public int panelBgBottom;
        public int titleLeft;
        public int titleRight;
        public int titleText;
        public int labelText;
        public int subText;

        public int stateSuccess;
        public int stateWarning;
        public int stateDanger;
        public int stateDisabled;
        public int stateSelected;

        public int buttonBgNormal;
        public int buttonBgHover;
        public int buttonBgPressed;
        public int buttonBorderNormal;
        public int buttonBorderHover;

        public int inputBg;
        public int inputBorder;
        public int inputBorderHover;

        public int panelOpacityPercent = 100;
        public int buttonOpacityPercent = 100;
        public int inputOpacityPercent = 100;
        public int textOpacityPercent = 100;

        public String panelImagePath = "";
        public String buttonImagePath = "";
        public String inputImagePath = "";

        public int panelImageScale = 10;
        public int buttonImageScale = 10;
        public int inputImageScale = 10;

        public int panelCropX = 0;
        public int panelCropY = 0;
        public int buttonCropX = 0;
        public int buttonCropY = 0;
        public int inputCropX = 0;
        public int inputCropY = 0;

        public String panelImageQuality = "MEDIUM";
        public String buttonImageQuality = "MEDIUM";
        public String inputImageQuality = "MEDIUM";

        public static ThemeProfile fromCurrent(String name) {
            ThemeProfile p = new ThemeProfile();
            p.name = name;
            p.builtIn = false;
            p.builtInId = "";
            p.panelBorder = GuiTheme.PANEL_BORDER;
            p.panelBgTop = GuiTheme.PANEL_BG_TOP;
            p.panelBgBottom = GuiTheme.PANEL_BG_BOTTOM;
            p.titleLeft = GuiTheme.TITLE_LEFT;
            p.titleRight = GuiTheme.TITLE_RIGHT;
            p.titleText = GuiTheme.TITLE_TEXT;
            p.labelText = GuiTheme.LABEL_TEXT;
            p.subText = GuiTheme.SUB_TEXT;
            p.stateSuccess = GuiTheme.STATE_SUCCESS;
            p.stateWarning = GuiTheme.STATE_WARNING;
            p.stateDanger = GuiTheme.STATE_DANGER;
            p.stateDisabled = GuiTheme.STATE_DISABLED;
            p.stateSelected = GuiTheme.STATE_SELECTED;
            p.buttonBgNormal = GuiTheme.BUTTON_BG_NORMAL;
            p.buttonBgHover = GuiTheme.BUTTON_BG_HOVER;
            p.buttonBgPressed = GuiTheme.BUTTON_BG_PRESSED;
            p.buttonBorderNormal = GuiTheme.BUTTON_BORDER_NORMAL;
            p.buttonBorderHover = GuiTheme.BUTTON_BORDER_HOVER;
            p.inputBg = GuiTheme.INPUT_BG;
            p.inputBorder = GuiTheme.INPUT_BORDER;
            p.inputBorderHover = GuiTheme.INPUT_BORDER_HOVER;
            p.panelOpacityPercent = GuiTheme.PANEL_OPACITY_PERCENT;
            p.buttonOpacityPercent = GuiTheme.BUTTON_OPACITY_PERCENT;
            p.inputOpacityPercent = GuiTheme.INPUT_OPACITY_PERCENT;
            p.textOpacityPercent = GuiTheme.TEXT_OPACITY_PERCENT;
            p.panelImagePath = GuiTheme.PANEL_IMAGE_PATH;
            p.buttonImagePath = GuiTheme.BUTTON_IMAGE_PATH;
            p.inputImagePath = GuiTheme.INPUT_IMAGE_PATH;
            p.panelImageScale = GuiTheme.PANEL_IMAGE_SCALE;
            p.buttonImageScale = GuiTheme.BUTTON_IMAGE_SCALE;
            p.inputImageScale = GuiTheme.INPUT_IMAGE_SCALE;
            p.panelCropX = GuiTheme.PANEL_CROP_X;
            p.panelCropY = GuiTheme.PANEL_CROP_Y;
            p.buttonCropX = GuiTheme.BUTTON_CROP_X;
            p.buttonCropY = GuiTheme.BUTTON_CROP_Y;
            p.inputCropX = GuiTheme.INPUT_CROP_X;
            p.inputCropY = GuiTheme.INPUT_CROP_Y;
            p.panelImageQuality = GuiTheme.PANEL_IMAGE_QUALITY;
            p.buttonImageQuality = GuiTheme.BUTTON_IMAGE_QUALITY;
            p.inputImageQuality = GuiTheme.INPUT_IMAGE_QUALITY;
            return p;
        }

        public ThemeProfile copy() {
            ThemeProfile c = new ThemeProfile();
            c.name = this.name;
            c.builtIn = this.builtIn;
            c.builtInId = this.builtInId;
            c.panelBorder = this.panelBorder;
            c.panelBgTop = this.panelBgTop;
            c.panelBgBottom = this.panelBgBottom;
            c.titleLeft = this.titleLeft;
            c.titleRight = this.titleRight;
            c.titleText = this.titleText;
            c.labelText = this.labelText;
            c.subText = this.subText;
            c.stateSuccess = this.stateSuccess;
            c.stateWarning = this.stateWarning;
            c.stateDanger = this.stateDanger;
            c.stateDisabled = this.stateDisabled;
            c.stateSelected = this.stateSelected;
            c.buttonBgNormal = this.buttonBgNormal;
            c.buttonBgHover = this.buttonBgHover;
            c.buttonBgPressed = this.buttonBgPressed;
            c.buttonBorderNormal = this.buttonBorderNormal;
            c.buttonBorderHover = this.buttonBorderHover;
            c.inputBg = this.inputBg;
            c.inputBorder = this.inputBorder;
            c.inputBorderHover = this.inputBorderHover;
            c.panelOpacityPercent = this.panelOpacityPercent;
            c.buttonOpacityPercent = this.buttonOpacityPercent;
            c.inputOpacityPercent = this.inputOpacityPercent;
            c.textOpacityPercent = this.textOpacityPercent;
            c.panelImagePath = this.panelImagePath;
            c.buttonImagePath = this.buttonImagePath;
            c.inputImagePath = this.inputImagePath;
            c.panelImageScale = this.panelImageScale;
            c.buttonImageScale = this.buttonImageScale;
            c.inputImageScale = this.inputImageScale;
            c.panelCropX = this.panelCropX;
            c.panelCropY = this.panelCropY;
            c.buttonCropX = this.buttonCropX;
            c.buttonCropY = this.buttonCropY;
            c.inputCropX = this.inputCropX;
            c.inputCropY = this.inputCropY;
            c.panelImageQuality = this.panelImageQuality;
            c.buttonImageQuality = this.buttonImageQuality;
            c.inputImageQuality = this.inputImageQuality;
            return c;
        }
    }

    private static class ThemeStore {
        int activeIndex = 0;
        List<ThemeProfile> profiles = new ArrayList<>();
    }

    private static Path getFile() {
        return ProfileManager.getCurrentProfileDir().resolve("gui_themes.json");
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        load();
    }

    public static void load() {
        PROFILES.clear();
        Path f = getFile();
        if (Files.exists(f)) {
            try (BufferedReader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                ThemeStore store = GSON.fromJson(reader, ThemeStore.class);
                if (store != null && store.profiles != null && !store.profiles.isEmpty()) {
                    PROFILES.addAll(store.profiles);
                    activeIndex = Math.max(0, Math.min(store.activeIndex, PROFILES.size() - 1));
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("加载主题配置失败", e);
            }
        }

        if (!PROFILES.isEmpty() && isLegacyPresetPack(PROFILES)) {
            PROFILES.clear();
            PROFILES.addAll(buildDefaultPresets());
            activeIndex = 0;
        }

        if (PROFILES.isEmpty()) {
            PROFILES.addAll(buildDefaultPresets());
            activeIndex = 0;
        }

        if (normalizeProfilesState()) {
            save();
        }

        applyActiveProfile();
    }

    private static ThemeProfile buildPreset(String name, int border, int top, int bottom, int titleL, int titleR) {
        ThemeProfile p = ThemeProfile.fromCurrent(name);
        p.panelBorder = border;
        p.panelBgTop = top;
        p.panelBgBottom = bottom;
        p.titleLeft = titleL;
        p.titleRight = titleR;
        return p;
    }

    private static List<ThemeProfile> buildDefaultPresets() {
        List<ThemeProfile> list = new ArrayList<>();
        list.add(buildSeasideDefaultProfile());
        return list;
    }

    private static ThemeProfile buildSeasideDefaultProfile() {
        ThemeProfile p = ThemeProfile.fromCurrent("海边");
        p.builtIn = true;
        p.builtInId = BUILTIN_SEASIDE_ID;

        p.panelBorder = -12821896;
        p.panelBgTop = -801351048;
        p.panelBgBottom = -803719596;
        p.titleLeft = -12821896;
        p.titleRight = -10979692;
        p.titleText = -17722;
        p.labelText = -17722;
        p.subText = -2649442;

        p.stateSuccess = -13652130;
        p.stateWarning = -2055936;
        p.stateDanger = -4830646;
        p.stateDisabled = -11907242;
        p.stateSelected = -12679755;

        p.buttonBgNormal = -14268828;
        p.buttonBgHover = -12952968;
        p.buttonBgPressed = -15847860;
        p.buttonBorderNormal = -13084554;
        p.buttonBorderHover = -11308143;

        p.inputBg = -16374204;
        p.inputBorder = -15058344;
        p.inputBorderHover = -13084554;

        p.panelOpacityPercent = 60;
        p.buttonOpacityPercent = 60;
        p.inputOpacityPercent = 45;
        p.textOpacityPercent = 100;

        p.panelImagePath = BUILTIN_PANEL_IMAGE;
        p.buttonImagePath = BUILTIN_BUTTON_IMAGE;
        p.inputImagePath = BUILTIN_INPUT_IMAGE;

        p.panelImageScale = 40;
        p.buttonImageScale = 10;
        p.inputImageScale = 10;

        p.panelCropX = 600;
        p.panelCropY = 0;
        p.buttonCropX = 100;
        p.buttonCropY = 1111;
        p.inputCropX = 0;
        p.inputCropY = 0;

        p.panelImageQuality = "HIGH";
        p.buttonImageQuality = "MEDIUM";
        p.inputImageQuality = "MEDIUM";

        return p;
    }

    private static ThemeProfile buildPresetFromRgb(String name, int r, int g, int b) {
        int base = 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        int dark = darken(base, 42);
        int mid = darken(base, 20);

        ThemeProfile p = ThemeProfile.fromCurrent(name);
        p.panelBorder = base;
        p.panelBgTop = withAlpha(mid, 0xD0);
        p.panelBgBottom = withAlpha(dark, 0xD0);
        p.titleLeft = base;
        p.titleRight = lighten(base, 26);

        p.buttonBgNormal = mid;
        p.buttonBgHover = lighten(mid, 20);
        p.buttonBgPressed = darken(mid, 24);
        p.buttonBorderNormal = lighten(mid, 18);
        p.buttonBorderHover = lighten(mid, 44);

        p.inputBg = darken(base, 52);
        p.inputBorder = darken(base, 16);
        p.inputBorderHover = lighten(base, 28);

        int luminance = (r * 299 + g * 587 + b * 114) / 1000;
        if (luminance > 170) {
            p.titleText = 0xFF1E242D;
            p.labelText = 0xFF1E242D;
            p.subText = 0xFF4A5563;
        } else {
            p.titleText = 0xFFF2F8FF;
            p.labelText = 0xFFE6EEF8;
            p.subText = 0xFF9FB2C8;
        }

        return p;
    }

    private static boolean isLegacyPresetPack(List<ThemeProfile> list) {
        if (list.isEmpty()) {
            return false;
        }
        for (ThemeProfile p : list) {
            String n = p == null ? "" : p.name;
            if (n == null) {
                n = "";
            }
            if (!("海蓝".equals(n) || n.matches("预设\\d+"))) {
                return false;
            }
        }
        return list.size() >= 8;
    }

    public static void save() {
        try {
            normalizeProfilesState();
            Path f = getFile();
            Files.createDirectories(f.getParent());
            ThemeStore store = new ThemeStore();
            store.activeIndex = activeIndex;
            store.profiles = new ArrayList<>(PROFILES);
            try (BufferedWriter writer = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
                GSON.toJson(store, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存主题配置失败", e);
        }
    }

    public static List<ThemeProfile> getProfiles() {
        ensureLoaded();
        normalizeProfilesState();
        return PROFILES;
    }

    public static int getActiveIndex() {
        ensureLoaded();
        return activeIndex;
    }

    public static void setActiveIndex(int idx) {
        ensureLoaded();
        normalizeProfilesState();
        if (idx < 0 || idx >= PROFILES.size()) {
            return;
        }
        activeIndex = idx;
        applyActiveProfile();
    }

    public static ThemeProfile getActiveProfile() {
        ensureLoaded();
        normalizeProfilesState();
        if (PROFILES.isEmpty()) {
            ThemeProfile p = ThemeProfile.fromCurrent("默认主题");
            PROFILES.add(p);
        }
        activeIndex = Math.max(0, Math.min(activeIndex, PROFILES.size() - 1));
        return PROFILES.get(activeIndex);
    }

    public static void applyActiveProfile() {
        ensureLoaded();
        normalizeProfilesState();
        ThemeProfile p = getActiveProfile();
        GuiTheme.applyProfile(p);
    }

    public static ThemeProfile addRandomTheme(String name) {
        ThemeProfile p = createRandomProfile(name);
        PROFILES.add(p);
        activeIndex = PROFILES.size() - 1;
        applyActiveProfile();
        return p;
    }

    public static ThemeProfile createRandomProfile(String name) {
        ThemeProfile p = ThemeProfile.fromCurrent(name);
        randomizeProfile(p);
        return p;
    }

    public static void addProfile(ThemeProfile profile) {
        if (profile == null) {
            return;
        }
        clearBuiltInMetadata(profile);
        PROFILES.add(profile);
        activeIndex = PROFILES.size() - 1;
        applyActiveProfile();
    }

    public static void deleteIndices(List<Integer> indices) {
        ensureLoaded();
        if (indices == null || indices.isEmpty()) {
            return;
        }
        indices.sort((a, b) -> Integer.compare(b, a));
        for (int idx : indices) {
            if (idx >= 0 && idx < PROFILES.size() && !isBuiltInProfile(PROFILES.get(idx))) {
                PROFILES.remove(idx);
            }
        }
        if (PROFILES.isEmpty()) {
            PROFILES.addAll(buildDefaultPresets());
            activeIndex = 0;
        } else {
            activeIndex = Math.max(0, Math.min(activeIndex, PROFILES.size() - 1));
        }
        applyActiveProfile();
    }

    public static void randomizeProfile(ThemeProfile p) {
        int hueBase = RANDOM.nextInt(360);
        p.panelBgTop = hsvToArgb(200, hueBase, randBetween(0, 100), randBetween(0, 100));
        p.panelBgBottom = hsvToArgb(200, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.panelBorder = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.titleLeft = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.titleRight = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.titleText = 0xFFF4FAFF;
        p.labelText = 0xFFE8F1FF;
        p.subText = 0xFF9FB2C8;

        p.buttonBgNormal = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.buttonBgHover = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.buttonBgPressed = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.buttonBorderNormal = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.buttonBorderHover = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));

        p.stateSuccess = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.stateWarning = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.stateDanger = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.stateDisabled = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.stateSelected = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));

        p.inputBg = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.inputBorder = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));
        p.inputBorderHover = hsvToArgb(255, RANDOM.nextInt(360), randBetween(0, 100), randBetween(0, 100));

        p.panelOpacityPercent = randBetween(10, 100);
        p.buttonOpacityPercent = randBetween(10, 100);
        p.inputOpacityPercent = randBetween(10, 100);
        p.textOpacityPercent = randBetween(10, 100);

        // 随机抽卡不改图片缩放，保持当前/用户设置值
        p.panelCropX = randBetween(0, 300);
        p.panelCropY = randBetween(0, 300);
        p.buttonCropX = randBetween(0, 300);
        p.buttonCropY = randBetween(0, 300);
        p.inputCropX = randBetween(0, 300);
        p.inputCropY = randBetween(0, 300);
    }

    private static int randBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + RANDOM.nextInt(max - min + 1);
    }

    public static void ensureDefaultPresets() {
        ensureLoaded();
        if (normalizeProfilesState()) {
            save();
        }
    }

    @Deprecated
    public static void ensureTwentyPresets() {
        ensureDefaultPresets();
    }

    private static int lighten(int color, int delta) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + delta);
        int g = Math.min(255, ((color >> 8) & 0xFF) + delta);
        int b = Math.min(255, (color & 0xFF) + delta);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color, int delta) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - delta);
        int g = Math.max(0, ((color >> 8) & 0xFF) - delta);
        int b = Math.max(0, (color & 0xFF) - delta);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static boolean isBuiltInProfile(ThemeProfile profile) {
        if (profile == null) {
            return false;
        }
        return profile.builtIn || !safeTrim(profile.builtInId).isEmpty();
    }

    private static boolean normalizeProfilesState() {
        List<ThemeProfile> current = new ArrayList<>(PROFILES);
        ThemeProfile oldActive = (activeIndex >= 0 && activeIndex < current.size()) ? current.get(activeIndex) : null;
        ThemeProfile seaside = buildSeasideDefaultProfile();
        List<ThemeProfile> normalized = new ArrayList<>();
        normalized.add(seaside);

        boolean changed = current.isEmpty();
        boolean foundSeaside = false;
        for (ThemeProfile profile : current) {
            if (profile == null) {
                changed = true;
                continue;
            }
            if (normalizeLegacyBuiltinImagePaths(profile)) {
                changed = true;
            }
            if (isSeasideBuiltinCandidate(profile)) {
                if (!foundSeaside && !sameProfile(profile, seaside)) {
                    changed = true;
                } else if (foundSeaside) {
                    changed = true;
                }
                foundSeaside = true;
                continue;
            }
            if (clearBuiltInMetadata(profile)) {
                changed = true;
            }
            normalized.add(profile);
        }

        if (!foundSeaside) {
            changed = true;
        }

        int newActiveIndex = 0;
        if (oldActive != null && !isSeasideBuiltinCandidate(oldActive)) {
            int idx = normalized.indexOf(oldActive);
            if (idx >= 0) {
                newActiveIndex = idx;
            }
        }
        newActiveIndex = Math.max(0, Math.min(newActiveIndex, normalized.size() - 1));

        if (!changed && (normalized.size() != PROFILES.size() || newActiveIndex != activeIndex)) {
            changed = true;
        }

        PROFILES.clear();
        PROFILES.addAll(normalized);
        activeIndex = newActiveIndex;
        return changed;
    }

    private static boolean isSeasideBuiltinCandidate(ThemeProfile profile) {
        if (profile == null) {
            return false;
        }
        if (BUILTIN_SEASIDE_ID.equalsIgnoreCase(safeTrim(profile.builtInId))) {
            return true;
        }
        if (!"海边".equals(safeTrim(profile.name))) {
            return false;
        }
        return sameProfile(profile, buildSeasideDefaultProfile()) || hasBuiltInSeasideImages(profile);
    }

    private static boolean clearBuiltInMetadata(ThemeProfile profile) {
        if (profile == null) {
            return false;
        }
        boolean changed = profile.builtIn || !safeTrim(profile.builtInId).isEmpty();
        profile.builtIn = false;
        profile.builtInId = "";
        return changed;
    }

    private static boolean sameProfile(ThemeProfile left, ThemeProfile right) {
        return GSON.toJson(left).equals(GSON.toJson(right));
    }

    private static boolean hasBuiltInSeasideImages(ThemeProfile profile) {
        return isBuiltInSeasideImage(profile.panelImagePath, BUILTIN_PANEL_IMAGE, LEGACY_BUILTIN_PANEL_IMAGE)
                || isBuiltInSeasideImage(profile.buttonImagePath, BUILTIN_BUTTON_IMAGE, LEGACY_BUILTIN_BUTTON_IMAGE)
                || isBuiltInSeasideImage(profile.inputImagePath, BUILTIN_INPUT_IMAGE, LEGACY_BUILTIN_INPUT_IMAGE);
    }

    private static boolean normalizeLegacyBuiltinImagePaths(ThemeProfile profile) {
        if (profile == null) {
            return false;
        }

        boolean changed = false;
        String normalizedPanel = normalizeBuiltInSeasideImage(profile.panelImagePath, BUILTIN_PANEL_IMAGE, LEGACY_BUILTIN_PANEL_IMAGE);
        if (!safeTrim(profile.panelImagePath).equals(normalizedPanel)) {
            profile.panelImagePath = normalizedPanel;
            changed = true;
        }

        String normalizedButton = normalizeBuiltInSeasideImage(profile.buttonImagePath, BUILTIN_BUTTON_IMAGE, LEGACY_BUILTIN_BUTTON_IMAGE);
        if (!safeTrim(profile.buttonImagePath).equals(normalizedButton)) {
            profile.buttonImagePath = normalizedButton;
            changed = true;
        }

        String normalizedInput = normalizeBuiltInSeasideImage(profile.inputImagePath, BUILTIN_INPUT_IMAGE, LEGACY_BUILTIN_INPUT_IMAGE);
        if (!safeTrim(profile.inputImagePath).equals(normalizedInput)) {
            profile.inputImagePath = normalizedInput;
            changed = true;
        }

        return changed;
    }

    private static boolean isBuiltInSeasideImage(String value, String currentPath, String legacyPath) {
        String trimmed = safeTrim(value);
        return currentPath.equals(trimmed) || legacyPath.equals(trimmed);
    }

    private static String normalizeBuiltInSeasideImage(String value, String currentPath, String legacyPath) {
        String trimmed = safeTrim(value);
        return legacyPath.equals(trimmed) ? currentPath : trimmed;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static int hsvToArgb(int a, int h, int s, int v) {
        float hf = (h % 360) / 360f;
        float sf = Math.max(0f, Math.min(1f, s / 100f));
        float vf = Math.max(0f, Math.min(1f, v / 100f));
        int rgb = java.awt.Color.HSBtoRGB(hf, sf, vf);
        return (a & 0xFF) << 24 | (rgb & 0x00FFFFFF);
    }
}

