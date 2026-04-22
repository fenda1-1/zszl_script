package com.zszl.zszlScriptMod.gui.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.Settings;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.SettingsUtil;
import com.zszl.zszlScriptMod.config.BaritoneParkourSettingsHelper;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;

import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.resources.I18n;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config.GuiUtils;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GuiBaritoneSettings extends ThemedGuiScreen {

    private static final String BUILTIN_SETTINGS_RESOURCE = "baritone_settings.json";
    private static final Gson GSON = new Gson();

    private static final int BTN_SAVE_CLOSE = 100;
    private static final int BTN_CANCEL = 101;
    private static final int BTN_RESET_PAGE = 102;
    private static final int BTN_APPLY_PAGE = 103;
    private static final int BTN_PREV_PAGE = 104;
    private static final int BTN_NEXT_PAGE = 105;
    private static final int BTN_TYPE_FILTER = 106;
    private static final int BTN_MODIFIED_ONLY = 107;
    private static final int BTN_CLEAR_FILTER = 108;

    private static final String[] TYPE_FILTERS = {
            "all", "boolean", "int", "long", "float", "double", "string", "list", "map", "color", "vec3i"
    };

    private final GuiScreen parentScreen;
    private final boolean parkourOnly;

    private final List<SettingDef> allDefs = new ArrayList<>();
    private final List<SettingDef> filteredDefs = new ArrayList<>();
    private final Map<String, String> editingValues = new HashMap<>();
    private final Map<String, String> statusMessages = new HashMap<>();

    private final List<GuiTextField> valueFields = new ArrayList<>();
    private final List<GuiButton> boolButtons = new ArrayList<>();
    private final Map<GuiButton, String> buttonToKey = new HashMap<>();

    private int page = 0;
    private int totalPages = 1;
    private int itemsPerPage = 24;
    private int rowsPerPage = 8;
    private int columns = 3;
    private int cardHeight = 58;
    private int cardGapX = 6;
    private int cardGapY = 6;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentY;
    private int contentW;
    private GuiTextField searchField;
    private String searchText = "";
    private String typeFilter = "all";
    private boolean modifiedOnly = false;

    private String hoverTooltip;
    private int hoverX;
    private int hoverY;

    public GuiBaritoneSettings(GuiScreen parent) {
        this(parent, false);
    }

    public GuiBaritoneSettings(GuiScreen parent, boolean parkourOnly) {
        this.parentScreen = parent;
        this.parkourOnly = parkourOnly;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.valueFields.clear();
        this.boolButtons.clear();
        this.buttonToKey.clear();

        buildDefsIfNeeded();
        if (editingValues.isEmpty()) {
            for (SettingDef def : allDefs) {
                editingValues.put(def.key, getCurrentSettingValue(def));
            }
        }

        updateResponsiveLayout();
        contentX = panelX + 10;
        contentY = panelY + 60;
        contentW = panelW - 20;

        int searchW = Math.max(180, Math.min(360, panelW / 3));
        int searchX = panelX + panelW - searchW - 10;
        int searchY = panelY + 10;
        searchField = new GuiTextField(9000, this.fontRenderer, searchX, searchY, searchW, 18);
        searchField.setMaxStringLength(128);
        searchField.setText(searchText == null ? "" : searchText);

        int filterBtnY = panelY + 10;
        int filterBtnX = panelX + 10;
        int typeBtnW = Math.max(90, Math.min(140, panelW / 7));
        int modifiedBtnW = Math.max(96, Math.min(130, panelW / 7));
        int clearBtnW = 84;

        this.buttonList.add(new ThemedButton(BTN_TYPE_FILTER, filterBtnX, filterBtnY, typeBtnW, 20,
                buildTypeFilterLabel()));
        this.buttonList.add(new ThemedButton(BTN_MODIFIED_ONLY, filterBtnX + typeBtnW + 6, filterBtnY,
                modifiedBtnW, 20, buildModifiedFilterLabel()));
        this.buttonList.add(new ThemedButton(BTN_CLEAR_FILTER, filterBtnX + typeBtnW + modifiedBtnW + 12, filterBtnY,
                clearBtnW, 20, "§f清空"));

        applyFilter(searchField.getText());
        recalcPaging();
        this.page = MathHelper.clamp(this.page, 0, this.totalPages - 1);

        int bottomY = panelY + panelH - 24;
        int navW = 54;
        int smallW = Math.max(68, Math.min(110, (panelW - 220) / 4));
        int leftX = panelX + 10;
        int rightX = panelX + panelW - 10;

        this.buttonList
                .add(new ThemedButton(BTN_PREV_PAGE, leftX, bottomY, navW, 20, I18n.format("gui.inventory.prev_page")));
        this.buttonList.add(new ThemedButton(BTN_NEXT_PAGE, leftX + navW + 6, bottomY, navW, 20,
                I18n.format("gui.inventory.next_page")));

        this.buttonList.add(new ThemedButton(BTN_SAVE_CLOSE, rightX - smallW, bottomY, smallW, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new ThemedButton(BTN_CANCEL, rightX - smallW * 2 - 6, bottomY, smallW, 20,
                I18n.format("gui.common.cancel")));
        this.buttonList.add(new ThemedButton(BTN_RESET_PAGE, rightX - smallW * 3 - 12, bottomY, smallW, 20,
                "§e" + I18n.format("gui.common.reset_default")));
        this.buttonList.add(new ThemedButton(BTN_APPLY_PAGE, rightX - smallW * 4 - 18, bottomY, smallW, 20,
                "§b应用本页"));

        buildPageControls();
    }

    private void buildDefsIfNeeded() {
        if (!allDefs.isEmpty()) {
            return;
        }
        if (loadBuiltInDefinitions()) {
            appendMissingDefinitionsFromRuntime();
            return;
        }
        buildFallbackDefinitionsFromRuntime();
    }

    private boolean loadBuiltInDefinitions() {
        try (InputStream input = GuiBaritoneSettings.class.getClassLoader()
                .getResourceAsStream(BUILTIN_SETTINGS_RESOURCE)) {
            if (input == null) {
                return false;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
                SettingsManifest manifest = GSON.fromJson(reader, SettingsManifest.class);
                if (manifest == null || manifest.settings == null || manifest.settings.isEmpty()) {
                    return false;
                }
                for (SettingDef def : manifest.settings) {
                    if (def == null || def.key == null || def.key.trim().isEmpty()) {
                        continue;
                    }
                    String type = def.type == null || def.type.trim().isEmpty() ? "string" : def.type.trim();
                    String defaultValue = def.defaultValue == null ? "" : def.defaultValue;
                    String desc = def.desc == null || def.desc.trim().isEmpty() ? def.key : def.desc;
                    allDefs.add(new SettingDef(def.key.trim(), defaultValue, type, desc));
                }
                return !allDefs.isEmpty();
            }
        } catch (IOException | JsonSyntaxException ignored) {
            return false;
        }
    }

    private void buildFallbackDefinitionsFromRuntime() {
        for (Settings.Setting<?> setting : BaritoneAPI.getSettings().allSettings) {
            if (setting == null || setting.isJavaOnly()) {
                continue;
            }
            String key = setting.getName();
            String defaultValue;
            try {
                defaultValue = SettingsUtil.settingDefaultToString(setting);
            } catch (Exception ignored) {
                defaultValue = "";
            }
            String type;
            try {
                type = SettingsUtil.settingTypeToString(setting).toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {
                type = "string";
            }
            allDefs.add(new SettingDef(key, defaultValue, type, key));
        }
    }

    private void appendMissingDefinitionsFromRuntime() {
        Map<String, SettingDef> existing = new HashMap<>();
        for (SettingDef def : allDefs) {
            if (def == null || def.key == null) {
                continue;
            }
            existing.put(def.key.trim().toLowerCase(Locale.ROOT), def);
        }

        for (Settings.Setting<?> setting : BaritoneAPI.getSettings().allSettings) {
            if (setting == null || setting.isJavaOnly()) {
                continue;
            }
            String key = setting.getName();
            if (key == null || existing.containsKey(key.toLowerCase(Locale.ROOT))) {
                continue;
            }

            String defaultValue;
            try {
                defaultValue = SettingsUtil.settingDefaultToString(setting);
            } catch (Exception ignored) {
                defaultValue = "";
            }

            String type;
            try {
                type = SettingsUtil.settingTypeToString(setting).toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {
                type = "string";
            }

            allDefs.add(new SettingDef(key, defaultValue, type, key));
            existing.put(key.toLowerCase(Locale.ROOT), new SettingDef(key, defaultValue, type, key));
        }
    }

    private void buildPageControls() {
        valueFields.clear();
        boolButtons.clear();
        buttonToKey.clear();

        int start = page * itemsPerPage;
        int end = Math.min(filteredDefs.size(), start + itemsPerPage);
        int cardW = getCardWidth();

        int idBase = 1000;
        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % columns;
            int row = local / columns;
            int cardX = contentX + col * (cardW + cardGapX);
            int cardY = contentY + row * (cardHeight + cardGapY);

            SettingDef def = filteredDefs.get(i);
            String value = editingValues.getOrDefault(def.key, def.defaultValue);

            int controlX = cardX + 6;
            int controlY = cardY + cardHeight - 22;
            int controlW = cardW - 12;

            if ("boolean".equals(def.type)) {
                boolean on = parseBoolean(value, def.defaultValue);
                ThemedButton btn = new ThemedButton(idBase + local, controlX, controlY, controlW, 16,
                        buildBoolLabel(on));
                boolButtons.add(btn);
                buttonToKey.put(btn, def.key);
            } else {
                GuiTextField field = new GuiTextField(idBase + local, this.fontRenderer, controlX, controlY, controlW,
                        16);
                field.setMaxStringLength(Integer.MAX_VALUE);
                field.setText(value);
                valueFields.add(field);
            }
        }
    }

    private void recalcPaging() {
        int bottomY = panelY + panelH - 24;
        int contentBottom = bottomY - 10;
        int availableH = Math.max(80, contentBottom - contentY);
        rowsPerPage = Math.max(1, (availableH + cardGapY) / (cardHeight + cardGapY));
        itemsPerPage = Math.max(columns, rowsPerPage * columns);
        totalPages = Math.max(1, (filteredDefs.size() + itemsPerPage - 1) / itemsPerPage);
    }

    private void applyFilter(String query) {
        searchText = query == null ? "" : query.trim();
        String q = PinyinSearchHelper.normalizeQuery(searchText);
        filteredDefs.clear();
        for (SettingDef def : allDefs) {
            if (!shouldDisplayDefinition(def)) {
                continue;
            }
            String searchContent = (def.key == null ? "" : def.key) + " " + (def.desc == null ? "" : def.desc);
            boolean textMatch = q.isEmpty() || PinyinSearchHelper.matchesNormalized(searchContent, q);
            boolean typeMatch = matchesTypeFilter(def);
            boolean modifiedMatch = !modifiedOnly || isModifiedValue(def);
            if (textMatch && typeMatch && modifiedMatch) {
                filteredDefs.add(def);
            }
        }
    }

    private boolean matchesTypeFilter(SettingDef def) {
        if ("all".equals(typeFilter)) {
            return true;
        }
        String t = def.type == null ? "" : def.type.toLowerCase(Locale.ROOT);
        if (t.equals(typeFilter)) {
            return true;
        }
        return t.startsWith(typeFilter + "<");
    }

    private boolean isModifiedValue(SettingDef def) {
        String value = editingValues.getOrDefault(def.key, def.defaultValue);
        return !isDefaultValue(def, value);
    }

    private boolean parseBoolean(String value, String defaultValue) {
        String v = value == null ? defaultValue : value;
        return "true".equalsIgnoreCase(v.trim());
    }

    private String buildBoolLabel(boolean on) {
        return on ? "§a" + I18n.format("gui.common.enabled") : "§c" + I18n.format("gui.common.disabled");
    }

    private String buildTypeFilterLabel() {
        String label = "all".equals(typeFilter) ? "全部" : typeFilter;
        return "§b类型: " + label;
    }

    private String buildModifiedFilterLabel() {
        return modifiedOnly ? "§a仅已修改" : "§7全部";
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        hoverTooltip = null;

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        drawCenteredString(this.fontRenderer, getScreenTitle(), panelX + panelW / 2, panelY + 8, 0xFFFFFFFF);

        if (searchField != null) {
            drawThemedTextField(searchField);
            if ((searchField.getText() == null || searchField.getText().isEmpty()) && !searchField.isFocused()) {
                drawString(this.fontRenderer, getSearchPlaceholder(), searchField.x + 4, searchField.y + 5, 0xFF8A8A8A);
            }
        }

        int modifiedCount = 0;
        for (SettingDef def : allDefs) {
            String value = editingValues.getOrDefault(def.key, def.defaultValue);
            if (!isDefaultValue(def, value)) {
                modifiedCount++;
            }
        }

        drawString(this.fontRenderer,
                String.format(Locale.ROOT, "当前页: %d/%d   本页容量: %d   结果: %d   已修改: %d",
                        page + 1, totalPages, itemsPerPage, filteredDefs.size(), modifiedCount),
                panelX + 10, panelY + 34, 0xFFCFCFCF);

        int start = page * itemsPerPage;
        int end = Math.min(filteredDefs.size(), start + itemsPerPage);
        int cardW = getCardWidth();

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % columns;
            int row = local / columns;
            int cardX = contentX + col * (cardW + cardGapX);
            int cardY = contentY + row * (cardHeight + cardGapY);

            SettingDef def = filteredDefs.get(i);

            GuiTheme.drawPanelSegment(cardX, cardY, cardW, cardHeight, cardX, cardY, cardW, cardHeight);

            String descText = trimToWidth(def.desc, cardW - 12);
            String keyText = trimToWidth(def.key, cardW - 12);
            String metaText = trimToWidth("类型: " + def.type + "  默认: " + getResolvedDefaultValue(def), cardW - 12);

            drawString(this.fontRenderer, descText, cardX + 6, cardY + 4, 0xFFFFFFFF);
            drawString(this.fontRenderer, keyText, cardX + 6, cardY + 16, 0xFF98B9FF);
            drawString(this.fontRenderer, metaText, cardX + 6, cardY + 28, 0xFFAAAAAA);

            String msg = statusMessages.get(def.key);
            if (msg != null && !msg.isEmpty()) {
                int statusColor = "应用失败".equals(msg) ? 0xFFFF6E6E : 0xFF66DD66;
                drawString(this.fontRenderer, msg, cardX + cardW - 46, cardY + 4, statusColor);
            }

            if (mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardHeight) {
                hoverTooltip = "§e设置项: " + def.key
                        + "\n§7描述: " + def.desc
                        + "\n§7类型: " + def.type
                        + "\n§7默认: " + getResolvedDefaultValue(def);
                hoverX = mouseX;
                hoverY = mouseY;
            }
        }

        for (GuiButton b : boolButtons) {
            b.drawButton(this.mc, mouseX, mouseY, partialTicks);
        }
        for (GuiTextField tf : valueFields) {
            drawThemedTextField(tf);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (hoverTooltip != null) {
            GuiUtils.drawHoveringText(Arrays.asList(hoverTooltip.split("\\n")), hoverX, hoverY, width, height, -1,
                    this.fontRenderer);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (GuiTextField tf : valueFields) {
            tf.mouseClicked(mouseX, mouseY, mouseButton);
        }

        for (GuiButton b : boolButtons) {
            if (b.mousePressed(this.mc, mouseX, mouseY)) {
                String key = buttonToKey.get(b);
                if (key != null) {
                    boolean current = parseBoolean(editingValues.get(key), "false");
                    boolean next = !current;
                    editingValues.put(key, String.valueOf(next));
                    b.displayString = buildBoolLabel(next);
                    statusMessages.put(key, "待应用");
                }
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.setScreen(parentScreen);
            return;
        }
        if (searchField != null && searchField.isFocused()) {
            savePageEdits();
            searchField.textboxKeyTyped(typedChar, keyCode);
            applyFilter(searchField.getText());
            recalcPaging();
            page = 0;
            buildPageControls();
            return;
        }
        for (GuiTextField tf : valueFields) {
            if (tf.isFocused()) {
                tf.textboxKeyTyped(typedChar, keyCode);
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        if (wheel < 0 && page + 1 < totalPages) {
            savePageEdits();
            page++;
            initGui();
        } else if (wheel > 0 && page > 0) {
            savePageEdits();
            page--;
            initGui();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_CLEAR_FILTER) {
            savePageEdits();
            typeFilter = "all";
            modifiedOnly = false;
            searchText = "";
            page = 0;
            initGui();
            if (searchField != null) {
                searchField.setText("");
            }
            return;
        }

        if (button.id == BTN_TYPE_FILTER) {
            savePageEdits();
            cycleTypeFilter();
            page = 0;
            initGui();
            return;
        }

        if (button.id == BTN_MODIFIED_ONLY) {
            savePageEdits();
            modifiedOnly = !modifiedOnly;
            page = 0;
            initGui();
            return;
        }

        if (button.id == BTN_PREV_PAGE) {
            if (page > 0) {
                savePageEdits();
                page--;
                initGui();
            }
            return;
        }
        if (button.id == BTN_NEXT_PAGE) {
            if (page + 1 < totalPages) {
                savePageEdits();
                page++;
                initGui();
            }
            return;
        }

        if (button.id == BTN_CANCEL) {
            this.mc.setScreen(parentScreen);
            return;
        }

        if (button.id == BTN_RESET_PAGE) {
            resetCurrentPage();
            initGui();
            return;
        }

        if (button.id == BTN_APPLY_PAGE) {
            applyCurrentPageToBaritone();
            return;
        }

        if (button.id == BTN_SAVE_CLOSE) {
            savePageEdits();
            applyAllChangedOnSaveClose();
            this.mc.setScreen(parentScreen);
        }
    }

    private void savePageEdits() {
        int start = page * itemsPerPage;
        int end = Math.min(filteredDefs.size(), start + itemsPerPage);
        int textIdx = 0;
        for (int i = start; i < end; i++) {
            SettingDef def = filteredDefs.get(i);
            if ("boolean".equals(def.type)) {
                continue;
            }
            if (textIdx < valueFields.size()) {
                editingValues.put(def.key, valueFields.get(textIdx).getText().trim());
                textIdx++;
            }
        }
    }

    private void resetCurrentPage() {
        int start = page * itemsPerPage;
        int end = Math.min(filteredDefs.size(), start + itemsPerPage);
        for (int i = start; i < end; i++) {
            SettingDef def = filteredDefs.get(i);
            editingValues.put(def.key, getResolvedDefaultValue(def));
            statusMessages.put(def.key, "已重置");
        }
        savePageEdits();
    }

    private void applyCurrentPageToBaritone() {
        savePageEdits();
        int start = page * itemsPerPage;
        int end = Math.min(filteredDefs.size(), start + itemsPerPage);
        for (int i = start; i < end; i++) {
            SettingDef def = filteredDefs.get(i);
            String value = editingValues.getOrDefault(def.key, def.defaultValue);
            try {
                applySettingDirectly(def, value);
                editingValues.put(def.key, getCurrentSettingValue(def));
                statusMessages.put(def.key, "已应用");
            } catch (Exception e) {
                statusMessages.put(def.key, "应用失败");
            }
        }
        SettingsUtil.save(BaritoneAPI.getSettings());
    }

    private boolean isDefaultValue(SettingDef def, String value) {
        String v = value == null ? "" : value.trim();
        String d = getResolvedDefaultValue(def).trim();
        if ("boolean".equals(def.type)) {
            return parseBoolean(v, d) == parseBoolean(d, d);
        }
        return v.equalsIgnoreCase(d);
    }

    private void applySettingDirectly(SettingDef def, String value) {
        Settings settings = BaritoneAPI.getSettings();
        Settings.Setting<?> setting = settings.byLowerName.get(def.key.toLowerCase(Locale.ROOT));
        if (setting == null) {
            throw new IllegalStateException("Unknown setting: " + def.key);
        }
        if (setting.isJavaOnly()) {
            throw new IllegalStateException("Java-only setting: " + def.key);
        }
        SettingsUtil.parseAndApply(settings, def.key.toLowerCase(Locale.ROOT), normalizeSettingValue(def, value));
    }

    private void applyAllChangedOnSaveClose() {
        for (SettingDef def : allDefs) {
            String value = editingValues.getOrDefault(def.key, def.defaultValue);
            try {
                applySettingDirectly(def, value);
                editingValues.put(def.key, getCurrentSettingValue(def));
                statusMessages.put(def.key, "已应用");
            } catch (Exception e) {
                statusMessages.put(def.key, "应用失败");
            }
        }
        SettingsUtil.save(BaritoneAPI.getSettings());
    }

    private String getCurrentSettingValue(SettingDef def) {
        Settings.Setting<?> setting = BaritoneAPI.getSettings().byLowerName.get(def.key.toLowerCase(Locale.ROOT));
        if (setting == null || setting.isJavaOnly()) {
            return def.defaultValue;
        }
        try {
            return SettingsUtil.settingValueToString(setting);
        } catch (Exception ignored) {
            return def.defaultValue;
        }
    }

    private String getResolvedDefaultValue(SettingDef def) {
        if (def.defaultValue != null && !def.defaultValue.trim().isEmpty()) {
            return def.defaultValue;
        }
        Settings.Setting<?> setting = BaritoneAPI.getSettings().byLowerName.get(def.key.toLowerCase(Locale.ROOT));
        if (setting == null || setting.isJavaOnly()) {
            return "";
        }
        try {
            return SettingsUtil.settingDefaultToString(setting);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeSettingValue(SettingDef def, String value) {
        String normalized = value == null ? "" : value.trim();
        if ("list".equals(def.type) || "map".equals(def.type)) {
            if ((normalized.startsWith("[") && normalized.endsWith("]"))
                    || (normalized.startsWith("{") && normalized.endsWith("}"))) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
            return normalized;
        }
        if ("color".equals(def.type)) {
            String upper = normalized.toUpperCase(Locale.ROOT);
            try {
                Field field = Color.class.getField(upper);
                Object colorObject = field.get(null);
                if (colorObject instanceof Color) {
                    Color color = (Color) colorObject;
                    return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return normalized;
    }

    private void cycleTypeFilter() {
        int idx = 0;
        for (int i = 0; i < TYPE_FILTERS.length; i++) {
            if (TYPE_FILTERS[i].equals(typeFilter)) {
                idx = i;
                break;
            }
        }
        typeFilter = TYPE_FILTERS[(idx + 1) % TYPE_FILTERS.length];
    }

    private void updateResponsiveLayout() {
        panelW = Math.min(this.width - 8, 1220);
        panelH = Math.min(this.height - 8, 720);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        if (panelW >= 1100) {
            columns = 4;
            cardHeight = 56;
        } else if (panelW >= 820) {
            columns = 3;
            cardHeight = 58;
        } else if (panelW >= 560) {
            columns = 2;
            cardHeight = 60;
        } else {
            columns = 1;
            cardHeight = 64;
        }
    }

    private int getCardWidth() {
        return (contentW - (columns - 1) * cardGapX) / columns;
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        String value = text;
        while (!value.isEmpty() && this.fontRenderer.getStringWidth(value) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.equals(text) && value.length() > 2) {
            return value.substring(0, value.length() - 2) + "..";
        }
        return value;
    }

    private boolean shouldDisplayDefinition(SettingDef def) {
        boolean isParkourSetting = BaritoneParkourSettingsHelper.isParkourSettingKey(def.key);
        return this.parkourOnly ? isParkourSetting : !isParkourSetting;
    }

    private String getScreenTitle() {
        return this.parkourOnly ? "Baritone跑酷" : "Baritone 设置";
    }

    private String getSearchPlaceholder() {
        return this.parkourOnly ? "搜索跑酷设置（键名 / 描述）" : "搜索设置（键名 / 描述）";
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private static final class SettingDef {
        private final String key;
        private final String defaultValue;
        private final String type;
        private final String desc;

        private SettingDef(String key, String defaultValue, String type, String desc) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.type = type;
            this.desc = desc;
        }
    }

    private static final class SettingsManifest {
        private int formatVersion;
        private List<SettingDef> settings;
    }
}






