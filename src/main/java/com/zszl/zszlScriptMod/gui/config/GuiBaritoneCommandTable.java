package com.zszl.zszlScriptMod.gui.config;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zszl.zszlScriptMod.baritone.compat.HumanLikeMovementController;
import com.zszl.zszlScriptMod.config.HumanLikeMovementConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.config.GuiHumanLikeMovementSettings;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.Settings;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.ICommand;
import com.zszl.zszlScriptMod.shadowbaritone.api.command.manager.ICommandManager;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.SettingsUtil;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.PinyinSearchHelper;
import net.minecraft.client.Minecraft;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.Gui;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraft.util.math.MathHelper;
import com.zszl.zszlScriptMod.compat.legacy.net.minecraftforge.fml.client.config.GuiUtils;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Keyboard;
import com.zszl.zszlScriptMod.compat.legacy.org.lwjgl.input.Mouse;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GuiBaritoneCommandTable extends ThemedGuiScreen {

    private static final Gson GSON = new Gson();
    private static final String BUILTIN_SETTINGS_RESOURCE = "baritone_settings.json";
    private static final String STATE_FILE_NAME = "baritone_command_table_state.json";

    private static final int BTN_BACK = 100;
    private static final int BTN_EXECUTE = 101;
    private static final int BTN_CLEAR_ARGS = 102;
    private static final int BTN_OPEN_SETTINGS = 103;
    private static final int BTN_FAVORITE_TOGGLE = 104;
    private static final int BTN_CLEAR_HISTORY = 105;
    private static final int BTN_NAV_COMMANDS = 106;
    private static final int BTN_NAV_SETTINGS = 107;
    private static final int BTN_HUMAN_LIKE_MOVEMENT = 108;

    private static final int BTN_FILTER_MODE = 120;
    private static final int BTN_FILTER_CATEGORY = 121;

    private static final int BTN_TAB_PARAMS = 150;
    private static final int BTN_TAB_PREVIEW = 151;
    private static final int BTN_TAB_EXAMPLES = 152;
    private static final int BTN_TAB_DETAILS = 153;

    private static final int BTN_SETTINGS_BACK = 3000;
    private static final int BTN_SETTINGS_SAVE = 3001;
    private static final int BTN_SETTINGS_RESET_PAGE = 3002;
    private static final int BTN_SETTINGS_APPLY_PAGE = 3003;
    private static final int BTN_SETTINGS_PREV_PAGE = 3004;
    private static final int BTN_SETTINGS_NEXT_PAGE = 3005;
    private static final int BTN_SETTINGS_TYPE_FILTER = 3006;
    private static final int BTN_SETTINGS_MODIFIED_ONLY = 3007;
    private static final int BTN_SETTINGS_CLEAR_FILTER = 3008;

    private static final int BTN_EXAMPLE_BASE = 5000;
    private static final int BTN_ARG_CONTROL_BASE = 6000;

    private static final int MAX_HISTORY_ITEMS = 40;
    private static final int MAX_RECENT_COMMANDS = 20;

    private static final Set<String> FAVORITE_COMMANDS = new LinkedHashSet<String>();
    private static final List<String> RECENT_COMMANDS = new ArrayList<String>();
    private static final List<HistoryEntry> EXECUTION_HISTORY = new ArrayList<HistoryEntry>();
    private static boolean PERSISTENT_STATE_LOADED = false;

    private static final SimpleDateFormat HISTORY_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final GuiScreen parentScreen;

    private final List<CommandEntry> allCommands = new ArrayList<CommandEntry>();
    private final List<CommandEntry> filteredCommands = new ArrayList<CommandEntry>();
    private final List<ArgControl> argControls = new ArrayList<ArgControl>();
    private final List<String> wrappedDetailLines = new ArrayList<String>();

    private GuiTextField searchField;
    private GuiTextField rawCommandField;

    private String searchText = "";
    private String selectedCommandName;
    private String statusMessage = "";
    private int statusColor = 0xFFBBBBBB;

    private CommandListMode commandListMode = CommandListMode.ALL;
    private CommandCategory activeCategory = CommandCategory.ALL;
    private RightTab currentRightTab = RightTab.PARAMS;
    private MainSection currentSection = MainSection.COMMANDS;
    private boolean modeDropdownOpen = false;
    private boolean categoryDropdownOpen = false;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int leftPaneX;
    private int leftPaneY;
    private int leftPaneW;
    private int leftPaneH;

    private int commandListX;
    private int commandListY;
    private int commandListW;
    private int commandListH;

    private int middlePaneX;
    private int middlePaneY;
    private int middlePaneW;
    private int middlePaneH;

    private int historyListX;
    private int historyListY;
    private int historyListW;
    private int historyListH;

    private int rightPaneX;
    private int rightPaneY;
    private int rightPaneW;
    private int rightPaneH;

    private int currentContentBoxY;
    private int currentContentBoxH;

    private int commandScroll = 0;
    private int detailScroll = 0;
    private int historyScroll = 0;
    private int argScroll = 0;
    private int rightScroll = 0;
    private int rightContentHeight = 0;
    private int commandVisibleRows = 1;
    private int historyVisibleRows = 1;

    private boolean leftPaneCollapsed = false;
    private boolean historyPaneCollapsed = false;
    private boolean rightPaneCollapsed = false;

    private static final int COMMAND_ROW_HEIGHT = 26;
    private static final int HISTORY_ROW_HEIGHT = 18;
    private static final int ARG_ROW_HEIGHT = 34;

    private String hoverTooltip;
    private int hoverTooltipX;
    private int hoverTooltipY;

    private final List<SettingDef> settingAllDefs = new ArrayList<SettingDef>();
    private final List<SettingDef> settingFilteredDefs = new ArrayList<SettingDef>();
    private final Map<String, String> settingEditingValues = new HashMap<String, String>();
    private final Map<String, String> settingStatusMessages = new HashMap<String, String>();
    private final List<GuiTextField> settingValueFields = new ArrayList<GuiTextField>();
    private final List<GuiButton> settingBoolButtons = new ArrayList<GuiButton>();
    private final List<GuiButton> settingVisualEditorButtons = new ArrayList<GuiButton>();
    private final Map<GuiButton, String> settingButtonToKey = new HashMap<GuiButton, String>();
    private final Map<GuiButton, SettingDef> settingVisualEditorButtonDefs = new HashMap<GuiButton, SettingDef>();

    private GuiTextField settingsSearchField;
    private String settingsSearchText = "";
    private String settingsTypeFilter = "all";
    private boolean settingsModifiedOnly = false;
    private String settingsStatusMessage = "";
    private int settingsStatusColor = 0xFFBBBBBB;

    private int settingsPage = 0;
    private int settingsTotalPages = 1;
    private int settingsItemsPerPage = 24;
    private int settingsRowsPerPage = 8;
    private int settingsColumns = 3;
    private int settingsCardHeight = 58;
    private final int settingsCardGapX = 6;
    private final int settingsCardGapY = 6;
    private int settingsContentX;
    private int settingsContentY;
    private int settingsContentW;

    public GuiBaritoneCommandTable(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.argControls.clear();
        this.wrappedDetailLines.clear();
        this.hoverTooltip = null;
        this.settingValueFields.clear();
        this.settingBoolButtons.clear();
        this.settingVisualEditorButtons.clear();
        this.settingButtonToKey.clear();
        this.settingVisualEditorButtonDefs.clear();

        loadPersistentStateIfNeeded();

        if (this.allCommands.isEmpty()) {
            loadCommands();
        }

        computeLayout();
        this.currentContentBoxY = this.rightPaneY + 102;
        this.currentContentBoxH = Math.max(120, this.rightPaneH - 120);

        if (this.currentSection == MainSection.SETTINGS) {
            initSettingsView();
            return;
        }

        this.searchField = new GuiTextField(9000, this.fontRenderer, this.leftPaneX + 8, this.leftPaneY + 28, Math.max(40, this.leftPaneW - 16), 18);
        this.searchField.setMaxStringLength(128);
        this.searchField.setText(this.searchText == null ? "" : this.searchText);

        refreshFilteredCommands();
        ensureSelectedCommand();

        initTopButtons();
        initSelectorButtons();
        initTabButtons();

        if (!this.historyPaneCollapsed) {
            int clearHistoryW = Math.max(64, Math.min(84, this.middlePaneW - 16));
            this.buttonList.add(new ThemedButton(BTN_CLEAR_HISTORY,
                    this.middlePaneX + 8,
                    this.middlePaneY + this.middlePaneH - 24,
                    clearHistoryW,
                    18,
                    "§e清历史"));
        }

        this.rawCommandField = new GuiTextField(9100, this.fontRenderer, -2000, -2000, Math.max(120, this.rightPaneW - 24), 18);
        this.rawCommandField.setMaxStringLength(512);

        CommandEntry selected = getSelectedCommand();
        if (!this.rightPaneCollapsed && selected != null) {
            this.buttonList.add(new ThemedButton(BTN_FAVORITE_TOGGLE, -2000, -2000, 80, 18,
                    FAVORITE_COMMANDS.contains(selected.primaryName) ? "§6已收藏" : "§7收藏"));
        }

        if (selected != null) {
            this.rawCommandField.setText(selected.primaryName);
            buildArgControls(selected);
            buildDetailLines(selected);
            initExampleButtons(selected);
        } else {
            this.rawCommandField.setText("");
        }
    }

    private void initTopButtons() {
        int titleButtonY = this.panelY + 26;
        int leftX = this.panelX + 12;
        int leftGap = 6;
        int navButtonW = Math.max(60, Math.min(86, (this.panelW - 80) / 7));
        int navSettingsX = leftX + navButtonW + leftGap;

        this.buttonList.add(new ThemedButton(BTN_NAV_COMMANDS, leftX, titleButtonY, navButtonW, 18,
                buildMainSectionLabel(MainSection.COMMANDS)));
        this.buttonList.add(new ThemedButton(BTN_NAV_SETTINGS, navSettingsX, titleButtonY, navButtonW, 18,
                buildMainSectionLabel(MainSection.SETTINGS)));

        int rightEdge = this.panelX + this.panelW - 10;
        int reservedLeft = navSettingsX + navButtonW + 12;
        int availableRightWidth = Math.max(300, rightEdge - reservedLeft);

        int gap = availableRightWidth < 430 ? 4 : 6;
        int[] preferred = new int[]{74, 96, 82, 74, 74}; // 返回 / 拟人化 / 设置 / 清空 / 执行
        int[] minimum = new int[]{54, 68, 60, 54, 54};
        int[] fitted = fitTopButtonWidths(availableRightWidth - gap * 4, preferred, minimum);

        int executeW = fitted[4];
        int clearW = fitted[3];
        int settingsW = fitted[2];
        int humanLikeW = fitted[1];
        int backW = fitted[0];

        int executeX = rightEdge - executeW;
        int clearX = executeX - gap - clearW;
        int settingsX = clearX - gap - settingsW;
        int humanLikeX = settingsX - gap - humanLikeW;
        int backX = humanLikeX - gap - backW;

        this.buttonList.add(new ThemedButton(BTN_EXECUTE, executeX, titleButtonY, executeW, 20, "§a执行"));
        this.buttonList.add(new ThemedButton(BTN_CLEAR_ARGS, clearX, titleButtonY, clearW, 20, "§e清空"));
        this.buttonList.add(new ThemedButton(BTN_OPEN_SETTINGS, settingsX, titleButtonY, settingsW, 20, "§b设置"));
        this.buttonList.add(new ToggleGuiButton(BTN_HUMAN_LIKE_MOVEMENT, humanLikeX, titleButtonY, humanLikeW, 20,
                buildHumanLikeToggleLabel(), HumanLikeMovementConfig.INSTANCE.enabled));
        this.buttonList.add(new ThemedButton(BTN_BACK, backX, titleButtonY, backW, 20, "返回"));
    }

    private int[] fitTopButtonWidths(int targetWidth, int[] preferred, int[] minimum) {
        int[] widths = Arrays.copyOf(preferred, preferred.length);
        int preferredSum = sumInts(preferred);
        int minimumSum = sumInts(minimum);
        if (targetWidth >= preferredSum) {
            return widths;
        }
        if (targetWidth <= minimumSum) {
            return Arrays.copyOf(minimum, minimum.length);
        }

        int shrinkable = preferredSum - minimumSum;
        int needShrink = preferredSum - targetWidth;

        for (int i = 0; i < widths.length; i++) {
            int localShrinkable = preferred[i] - minimum[i];
            int shrink = Math.min(localShrinkable, Math.round(needShrink * (localShrinkable / (float) shrinkable)));
            widths[i] = preferred[i] - shrink;
        }

        int diff = targetWidth - sumInts(widths);
        if (diff != 0) {
            for (int i = widths.length - 1; i >= 0 && diff != 0; i--) {
                int limit = diff > 0 ? preferred[i] : minimum[i];
                while (diff != 0 && widths[i] != limit) {
                    widths[i] += diff > 0 ? 1 : -1;
                    diff += diff > 0 ? -1 : 1;
                }
            }
        }
        return widths;
    }

    private int sumInts(int[] values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum;
    }

    private void initSelectorButtons() {
        if (this.leftPaneCollapsed) {
            return;
        }
        int selectorY = this.leftPaneY + 50;
        int gap = 6;
        int availableW = Math.max(140, this.leftPaneW - 16);
        int modeW = Math.max(72, Math.min(86, availableW / 3));
        int categoryW = Math.max(80, availableW - modeW - gap);

        this.buttonList.add(new ThemedButton(BTN_FILTER_MODE,
                this.leftPaneX + 8,
                selectorY,
                modeW,
                18,
                buildModeDropdownLabel()));
        this.buttonList.add(new ThemedButton(BTN_FILTER_CATEGORY,
                this.leftPaneX + 8 + modeW + gap,
                selectorY,
                categoryW,
                18,
                buildCategoryDropdownLabel()));
    }

    private void initTabButtons() {
        if (this.rightPaneCollapsed) {
            return;
        }
        int tabX = this.rightPaneX + 12;
        int tabGap = 4;
        int tabW = Math.max(60, (this.rightPaneW - 24 - tabGap * 3) / 4);

        this.buttonList.add(new ThemedButton(BTN_TAB_PARAMS, tabX, -2000, tabW, 18, buildTabLabel(RightTab.PARAMS)));
        this.buttonList.add(new ThemedButton(BTN_TAB_PREVIEW, tabX + (tabW + tabGap), -2000, tabW, 18, buildTabLabel(RightTab.PREVIEW)));
        this.buttonList.add(new ThemedButton(BTN_TAB_EXAMPLES, tabX + (tabW + tabGap) * 2, -2000, tabW, 18, buildTabLabel(RightTab.EXAMPLES)));
        this.buttonList.add(new ThemedButton(BTN_TAB_DETAILS, tabX + (tabW + tabGap) * 3, -2000, tabW, 18, buildTabLabel(RightTab.DETAILS)));
    }

    private void computeLayout() {
        this.panelW = Math.min(this.width - 16, 1220);
        this.panelH = Math.min(this.height - 16, 720);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        int sidePadding = 10;
        int columnGap = 10;
        int innerWidth = this.panelW - sidePadding * 2 - columnGap * 2;
        int innerX = this.panelX + sidePadding;

        this.leftPaneY = this.panelY + 56;
        this.leftPaneH = this.panelH - 66;
        this.middlePaneY = this.leftPaneY;
        this.middlePaneH = this.leftPaneH;
        this.rightPaneY = this.leftPaneY;
        this.rightPaneH = this.leftPaneH;

        int leftCollapsedW = getCollapsedSectionWidth("命令");
        int middleCollapsedW = getCollapsedSectionWidth("历史");
        int rightCollapsedW = getCollapsedSectionWidth("详情");

        int leftMin = this.leftPaneCollapsed ? leftCollapsedW : 190;
        int middleMin = this.historyPaneCollapsed ? middleCollapsedW : 120;
        int rightMin = this.rightPaneCollapsed ? rightCollapsedW : 360;

        this.leftPaneW = leftMin;
        this.middlePaneW = middleMin;
        this.rightPaneW = rightMin;

        int remaining = innerWidth - this.leftPaneW - this.middlePaneW - this.rightPaneW;
        if (remaining < 0) {
            int shortage = -remaining;

            int middleFloor = this.historyPaneCollapsed ? middleCollapsedW : 84;
            int leftFloor = this.leftPaneCollapsed ? leftCollapsedW : 160;
            int rightFloor = this.rightPaneCollapsed ? rightCollapsedW : 260;

            int middleReducible = Math.max(0, this.middlePaneW - middleFloor);
            int reduceMiddle = Math.min(shortage, middleReducible);
            this.middlePaneW -= reduceMiddle;
            shortage -= reduceMiddle;

            int leftReducible = Math.max(0, this.leftPaneW - leftFloor);
            int reduceLeft = Math.min(shortage, leftReducible);
            this.leftPaneW -= reduceLeft;
            shortage -= reduceLeft;

            int rightReducible = Math.max(0, this.rightPaneW - rightFloor);
            int reduceRight = Math.min(shortage, rightReducible);
            this.rightPaneW -= reduceRight;
            shortage -= reduceRight;

            remaining = innerWidth - this.leftPaneW - this.middlePaneW - this.rightPaneW;
        }

        if (remaining > 0) {
            int weightSum = 0;
            if (!this.leftPaneCollapsed) {
                weightSum += 55;
            }
            if (!this.historyPaneCollapsed) {
                weightSum += 20;
            }
            if (!this.rightPaneCollapsed) {
                weightSum += 225;
            }
            if (weightSum <= 0) {
                this.rightPaneW += remaining;
            } else {
                int distributed = 0;
                if (!this.leftPaneCollapsed) {
                    int share = remaining * 55 / weightSum;
                    this.leftPaneW += share;
                    distributed += share;
                }
                if (!this.historyPaneCollapsed) {
                    int share = remaining * 20 / weightSum;
                    this.middlePaneW += share;
                    distributed += share;
                }
                this.rightPaneW += remaining - distributed;
            }
        }

        this.leftPaneX = innerX;
        this.middlePaneX = this.leftPaneX + this.leftPaneW + columnGap;
        this.rightPaneX = this.middlePaneX + this.middlePaneW + columnGap;

        this.commandListX = this.leftPaneX + 6;
        this.commandListY = this.leftPaneY + 88;
        this.commandListW = this.leftPaneW - 12;
        this.commandListH = Math.max(40, this.leftPaneH - 96);
        this.commandVisibleRows = Math.max(3, this.commandListH / COMMAND_ROW_HEIGHT);

        this.historyListX = this.middlePaneX + 6;
        this.historyListY = this.middlePaneY + 24;
        this.historyListW = this.middlePaneW - 12;
        this.historyListH = Math.max(40, this.middlePaneH - 54);
        this.historyVisibleRows = Math.max(1, this.historyListH / HISTORY_ROW_HEIGHT);
    }

    private void loadCommands() {
        this.allCommands.clear();
        try {
            ICommandManager commandManager = BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager();
            for (ICommand command : commandManager.getRegistry().descendingStream().toArray(ICommand[]::new)) {
                if (command == null || command.hiddenFromHelp()) {
                    continue;
                }
                List<String> names = command.getNames() == null ? Collections.<String>emptyList() : command.getNames();
                if (names.isEmpty()) {
                    continue;
                }

                String primary = names.get(0);
                List<String> aliases = names.size() > 1 ? new ArrayList<String>(names.subList(1, names.size())) : new ArrayList<String>();
                String shortDesc = safe(command.getShortDesc());
                List<String> longDesc = command.getLongDesc() == null ? new ArrayList<String>() : new ArrayList<String>(command.getLongDesc());

                this.allCommands.add(new CommandEntry(primary, aliases, shortDesc, longDesc, classifyCommand(primary, aliases, shortDesc, longDesc)));
            }

            this.allCommands.sort(Comparator.comparing(entry -> entry.primaryName));
            this.statusMessage = "已加载 " + this.allCommands.size() + " 条 Baritone 命令";
            this.statusColor = 0xFF73D98A;
        } catch (Throwable t) {
            this.statusMessage = "命令加载失败";
            this.statusColor = 0xFFFF6E6E;
        }
    }

    private CommandCategory classifyCommand(String primary, List<String> aliases, String shortDesc, List<String> longDesc) {
        StringBuilder builder = new StringBuilder();
        builder.append(safe(primary)).append(' ');
        if (aliases != null) {
            for (String alias : aliases) {
                builder.append(safe(alias)).append(' ');
            }
        }
        builder.append(safe(shortDesc)).append(' ');
        if (longDesc != null) {
            for (String line : longDesc) {
                builder.append(safe(line)).append(' ');
            }
        }

        String text = builder.toString().toLowerCase(Locale.ROOT);
        if (containsAny(text, "goto", "goal", "follow", "come", "path", "explore", "axis", "tunnel", "farm", "highway")) {
            return CommandCategory.NAVIGATION;
        }
        if (containsAny(text, "mine", "build", "schem", "sel", "surface", "waypoint", "click", "place")) {
            return CommandCategory.WORLD;
        }
        if (containsAny(text, "set", "modified", "reload", "reset", "pause", "resume", "stop", "cancel", "invert", "proc")) {
            return CommandCategory.CONTROL;
        }
        if (containsAny(text, "help", "list", "version", "eta", "wp", "goal")) {
            return CommandCategory.INFO;
        }
        return CommandCategory.OTHER;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private void refreshFilteredCommands() {
        this.filteredCommands.clear();
        String query = PinyinSearchHelper.normalizeQuery(this.searchText);

        List<CommandEntry> baseList = new ArrayList<CommandEntry>();
        if (this.commandListMode == CommandListMode.ALL) {
            baseList.addAll(this.allCommands);
        } else if (this.commandListMode == CommandListMode.FAVORITES) {
            for (CommandEntry entry : this.allCommands) {
                if (FAVORITE_COMMANDS.contains(entry.primaryName)) {
                    baseList.add(entry);
                }
            }
        } else {
            for (String recentName : RECENT_COMMANDS) {
                CommandEntry entry = findCommandByName(recentName);
                if (entry != null) {
                    baseList.add(entry);
                }
            }
        }

        for (CommandEntry entry : baseList) {
            if (this.activeCategory != CommandCategory.ALL && entry.category != this.activeCategory) {
                continue;
            }
            if (query.isEmpty() || PinyinSearchHelper.matchesNormalized(entry.searchText, query)) {
                this.filteredCommands.add(entry);
            }
        }

        int maxScroll = Math.max(0, this.filteredCommands.size() - this.commandVisibleRows);
        this.commandScroll = MathHelper.clamp(this.commandScroll, 0, maxScroll);
    }

    private void ensureSelectedCommand() {
        if (this.filteredCommands.isEmpty()) {
            this.selectedCommandName = this.allCommands.isEmpty() ? null : this.allCommands.get(0).primaryName;
            return;
        }
        for (CommandEntry entry : this.filteredCommands) {
            if (entry.primaryName.equals(this.selectedCommandName)) {
                return;
            }
        }
        this.selectedCommandName = this.filteredCommands.get(0).primaryName;
    }

    private CommandEntry getSelectedCommand() {
        if (this.selectedCommandName == null) {
            return null;
        }
        return findCommandByName(this.selectedCommandName);
    }

    private CommandEntry findCommandByName(String name) {
        if (name == null) {
            return null;
        }
        for (CommandEntry entry : this.allCommands) {
            if (entry.primaryName.equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    private void buildArgControls(CommandEntry entry) {
        this.argControls.clear();
        this.argScroll = 0;

        List<ArgSchema> schemas = resolveSchemas(entry);
        for (int i = 0; i < schemas.size(); i++) {
            ArgSchema schema = schemas.get(i);
            ArgControl control = new ArgControl(schema);
            if (schema.valueType == ArgValueType.STRING || schema.valueType == ArgValueType.INTEGER || schema.valueType == ArgValueType.DECIMAL) {
                control.field = new GuiTextField(9200 + i, this.fontRenderer, -2000, -2000, 90, 16);
                control.field.setMaxStringLength(128);
            } else {
                control.buttonId = BTN_ARG_CONTROL_BASE + i;
                this.buttonList.add(new ThemedButton(control.buttonId, -2000, -2000, 110, 18, ""));
            }
            this.argControls.add(control);
        }

        layoutArgControls();
        validateArgControls(false);
    }

    private List<ArgSchema> resolveSchemas(CommandEntry entry) {
        if (entry == null) {
            return Collections.emptyList();
        }

        String name = entry.primaryName.toLowerCase(Locale.ROOT);
        if ("goto".equals(name)) {
            return Arrays.asList(
                    ArgSchema.text("block", "方块目标", "例如 diamond_ore", false, "方块模式：直接填写目标方块名"),
                    ArgSchema.integer("x", "X坐标", "例如 100", false, -30000000, 30000000, "坐标模式：目标位置 X", true, false),
                    ArgSchema.integer("y", "Y坐标", "例如 64", false, -256, 512, "坐标模式：目标位置 Y", true, false),
                    ArgSchema.integer("z", "Z坐标", "例如 -20", false, -30000000, 30000000, "坐标模式：目标位置 Z", true, false));
        }
        if ("goal".equals(name)) {
            return Arrays.asList(
                    ArgSchema.choice("action", "快捷操作", false, Arrays.asList("clear", "reset", "none"), "清除当前目标；留空则可改用坐标"),
                    ArgSchema.integer("x", "X坐标", "例如 100", false, -30000000, 30000000, "坐标模式：目标位置 X", true, false),
                    ArgSchema.integer("y", "Y坐标", "例如 64", false, -256, 512, "坐标模式：目标位置 Y", true, false),
                    ArgSchema.integer("z", "Z坐标", "例如 -20", false, -30000000, 30000000, "坐标模式：目标位置 Z", true, false));
        }
        if ("mine".equals(name)) {
            return Arrays.asList(
                    ArgSchema.text("block", "方块ID/名称", "例如 diamond_ore", true, "要挖掘的方块名称或 ID"),
                    ArgSchema.integer("count", "数量", "例如 64", false, 1, 999999, "可选：目标数量", false, false));
        }
        if ("follow".equals(name)) {
            return Arrays.asList(
                    ArgSchema.text("groupOrList", "跟随组/列表类型", "例如 players", true,
                            "单参数时填写跟随组；多参数时先填写列表类型"),
                    ArgSchema.text("target1", "目标1", "例如 Steve / minecraft:zombie", false,
                            "可选：实体名或实体类型"),
                    ArgSchema.text("target2", "目标2", "例如 Alex", false,
                            "可继续填写更多目标"));
        }
        if ("come".equals(name)) {
            return Collections.emptyList();
        }
        if ("explore".equals(name)) {
            return Arrays.asList(
                    ArgSchema.integer("x", "中心X", "例如 0", false, -30000000, 30000000, "可选：探索中心 X", true, false),
                    ArgSchema.integer("z", "中心Z", "例如 0", false, -30000000, 30000000, "可选：探索中心 Z", true, false));
        }
        if ("set".equals(name)) {
            return Arrays.asList(
                    ArgSchema.text("setting", "设置项/模式", "例如 allowBreak / list / modified", true, "设置项名称，或 list / modified 等查看模式"),
                    ArgSchema.text("value", "设置值", "例如 true", false, "可选：填写时表示修改设置；留空可用于查看"));
        }
        if ("click".equals(name)) {
            return Collections.emptyList();
        }
        if ("build".equals(name)) {
            return Arrays.asList(
                    ArgSchema.text("schematic", "蓝图名/路径", "例如 base.schematic", true, "要构建的 schematic 名称或路径"),
                    ArgSchema.integer("x", "起点X", "例如 100", false, -30000000, 30000000, "可选：构建起点 X", true, false),
                    ArgSchema.integer("y", "起点Y", "例如 64", false, -256, 512, "可选：构建起点 Y", true, false),
                    ArgSchema.integer("z", "起点Z", "例如 100", false, -30000000, 30000000, "可选：构建起点 Z", true, false));
        }
        if ("farm".equals(name)) {
            return Arrays.asList(
                    ArgSchema.integer("range", "范围", "例如 32", false, 0, 999999, "可选：耕作范围", false, false),
                    ArgSchema.text("waypoint", "路标", "例如 home", false, "可选：从指定路标开始"));
        }
        if ("tunnel".equals(name)) {
            return Arrays.asList(
                    ArgSchema.integer("width", "宽度", "例如 1", false, 1, 7, "可选：隧道宽度", false, false),
                    ArgSchema.integer("height", "高度", "例如 2", false, 2, 6, "可选：隧道高度", false, false),
                    ArgSchema.integer("depth", "长度", "例如 100", false, 1, 100000, "可选：向前挖掘长度", false, false));
        }
        if ("wp".equals(name) || "waypoints".equals(name)) {
            return Arrays.asList(
                    ArgSchema.choice("action", "操作", false, Arrays.asList("list", "save", "delete", "goto", "clear"), "路标操作"),
                    ArgSchema.text("name", "路标名", "例如 home", false, "对应的路标名称"));
        }
        if ("path".equals(name)) {
            return Collections.emptyList();
        }
        if ("thisway".equals(name) || "forward".equals(name)) {
            return Arrays.asList(
                    ArgSchema.decimal("distance", "距离", "例如 100", true, 0.0D, 30000000.0D, "朝当前朝向前进的距离", false, false));
        }
        if ("pause".equals(name) || "resume".equals(name) || "stop".equals(name) || "cancel".equals(name)
                || "eta".equals(name) || "help".equals(name) || "version".equals(name)
                || "modified".equals(name) || "reload".equals(name)
                || "proc".equals(name) || "invert".equals(name)) {
            return Collections.emptyList();
        }

        List<ArgSchema> fallback = new ArrayList<ArgSchema>();
        for (ExampleEntry example : entry.examples) {
            List<String> hints = parseExampleHints(example.commandText);
            if (!hints.isEmpty()) {
                for (int i = 0; i < hints.size() && i < 6; i++) {
                    fallback.add(ArgSchema.text("arg" + (i + 1), "参数" + (i + 1), hints.get(i), false,
                            "来自命令示例的参数提示"));
                }
                break;
            }
        }
        return fallback;
    }

    private List<String> parseExampleHints(String exampleCommand) {
        if (exampleCommand == null) {
            return Collections.emptyList();
        }
        String trimmed = exampleCommand.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length <= 1) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();
        for (int i = 1; i < tokens.length; i++) {
            String cleaned = tokens[i].replace("<", "").replace(">", "")
                    .replace("[", "").replace("]", "")
                    .replace(",", "").trim();
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private void buildDetailLines(CommandEntry entry) {
        this.wrappedDetailLines.clear();
        this.detailScroll = 0;
        if (entry == null) {
            return;
        }

        List<String> paragraphs = new ArrayList<String>();
        paragraphs.add("§b命令：§f" + entry.primaryName);
        paragraphs.add("§b分类：§f" + entry.category.label);
        paragraphs.add("§b收藏：§f" + (FAVORITE_COMMANDS.contains(entry.primaryName) ? "已收藏" : "未收藏"));
        paragraphs.add("§b别名：§f" + (entry.aliases.isEmpty() ? "无" : String.join("、", entry.aliases)));
        paragraphs.add("§b简介：§f" + safe(entry.shortDesc));
        paragraphs.add("");

        if (!this.argControls.isEmpty()) {
            paragraphs.add("§e参数说明：");
            for (ArgControl control : this.argControls) {
                paragraphs.add("§7- §f" + control.schema.label + "§7："
                        + (control.schema.required ? "必填，" : "可选，")
                        + control.schema.buildCompactTip());
            }
            paragraphs.add("");
        }

        if (!entry.longDesc.isEmpty()) {
            paragraphs.add("§e详细说明：");
            paragraphs.addAll(entry.longDesc);
        }

        if (!entry.examples.isEmpty()) {
            paragraphs.add("");
            paragraphs.add("§e示例说明：");
            for (ExampleEntry example : entry.examples) {
                if (example.description.isEmpty()) {
                    paragraphs.add("§7- §f" + example.commandText);
                } else {
                    paragraphs.add("§7- §f" + example.commandText + " §8| §7" + example.description);
                }
            }
        }

        int wrapWidth = Math.max(120, this.rightPaneW - 40);
        for (String paragraph : paragraphs) {
            if (paragraph == null || paragraph.isEmpty()) {
                this.wrappedDetailLines.add("");
                continue;
            }
            List<String> lines = this.fontRenderer.listFormattedStringToWidth(paragraph, wrapWidth);
            if (lines == null || lines.isEmpty()) {
                this.wrappedDetailLines.add(paragraph);
            } else {
                this.wrappedDetailLines.addAll(lines);
            }
        }
    }

    private void initExampleButtons(CommandEntry entry) {
        int maxButtons = Math.min(10, entry.examples.size());
        for (int i = 0; i < maxButtons; i++) {
            ExampleEntry example = entry.examples.get(i);
            this.buttonList.add(new ThemedButton(BTN_EXAMPLE_BASE + i, -2000, -2000, 120, 18,
                    trimToWidth(example.commandText, 112)));
        }
    }

    private void layoutArgControls() {
        int baseX = getContentBoxX() + 10;
        int baseY = getContentBoxY() + 28;
        int fullW = getContentBoxW() - 20;
        int currentRow = 0;

        for (int i = 0; i < this.argControls.size(); i++) {
            ArgControl control = this.argControls.get(i);

            boolean groupedCoordinate = isCoordinateTripletStart(i);
            if (groupedCoordinate) {
                int drawY = baseY + currentRow * ARG_ROW_HEIGHT;
                boolean visible = isInRightViewport(drawY - 12, ARG_ROW_HEIGHT + 16);
                int cellGap = 6;
                int cellW = (fullW - cellGap * 2) / 3;
                for (int c = 0; c < 3; c++) {
                    ArgControl coord = this.argControls.get(i + c);
                    coord.visible = visible;
                    coord.groupedCoordinate = true;
                    coord.validationColor = 0xFF9FB2C8;
                    if (visible) {
                        coord.x = baseX + c * (cellW + cellGap);
                        coord.y = drawY + 10;
                        coord.w = cellW;
                        coord.h = 16;
                        if (coord.field != null) {
                            coord.field.x = coord.x;
                            coord.field.y = coord.y;
                            coord.field.width = coord.w;
                            coord.field.height = coord.h;
                        }
                    } else {
                        coord.x = -2000;
                        coord.y = -2000;
                        if (coord.field != null) {
                            coord.field.x = -2000;
                            coord.field.y = -2000;
                        }
                    }
                }
                currentRow++;
                i += 2;
                continue;
            }

            int drawY = baseY + currentRow * ARG_ROW_HEIGHT;
            boolean visible = isInRightViewport(drawY - 12, ARG_ROW_HEIGHT + 16);

            control.visible = visible;
            control.groupedCoordinate = false;
            if (visible) {
                control.x = baseX;
                control.y = drawY + 10;
                control.w = fullW;
                control.h = 18;
                if (control.field != null) {
                    control.field.x = control.x;
                    control.field.y = control.y;
                    control.field.width = fullW;
                    control.field.height = 16;
                } else {
                    GuiButton button = findButtonById(control.buttonId);
                    if (button != null) {
                        button.visible = true;
                        button.enabled = true;
                        button.x = control.x;
                        button.y = control.y;
                        button.width = fullW;
                        button.height = 18;
                        button.displayString = buildArgButtonLabel(control);
                    }
                }
            } else {
                control.x = -2000;
                control.y = -2000;
                if (control.field != null) {
                    control.field.x = -2000;
                    control.field.y = -2000;
                } else {
                    GuiButton button = findButtonById(control.buttonId);
                    if (button != null) {
                        button.visible = false;
                        button.enabled = false;
                        button.x = -2000;
                        button.y = -2000;
                    }
                }
            }

            currentRow++;
        }
    }

    private boolean isCoordinateTripletStart(int index) {
        if (index + 2 >= this.argControls.size()) {
            return false;
        }
        ArgSchema a = this.argControls.get(index).schema;
        ArgSchema b = this.argControls.get(index + 1).schema;
        ArgSchema c = this.argControls.get(index + 2).schema;
        return a.coordinate && b.coordinate && c.coordinate
                && "x".equalsIgnoreCase(a.key)
                && "y".equalsIgnoreCase(b.key)
                && "z".equalsIgnoreCase(c.key);
    }

    private int getArgTotalRows() {
        int rows = 0;
        for (int i = 0; i < this.argControls.size(); i++) {
            if (isCoordinateTripletStart(i)) {
                rows++;
                i += 2;
            } else {
                rows++;
            }
        }
        return rows;
    }

    private int getArgVisibleRows() {
        return Math.max(1, (getContentBoxH() - 40) / ARG_ROW_HEIGHT);
    }

    private boolean validateArgControls(boolean updateStatus) {
        boolean hasError = false;
        for (ArgControl control : this.argControls) {
            String validation = control.schema.validate(control.value);
            control.validationMessage = validation == null || validation.isEmpty()
                    ? (control.schema.required ? "§a格式正确" : "§7可留空")
                    : "§c" + validation;
            control.invalid = validation != null && !validation.isEmpty();
            control.validationColor = control.invalid ? 0xFFFF8E8E : 0xFF9FB2C8;
            hasError |= control.invalid;
        }

        if (updateStatus) {
            if (hasError) {
                this.statusMessage = "参数校验未通过，请修正红色字段";
                this.statusColor = 0xFFFF6E6E;
            } else if (!this.argControls.isEmpty()) {
                this.statusMessage = "参数校验通过";
                this.statusColor = 0xFF73D98A;
            }
        }
        return hasError;
    }

    private void syncControlValuesFromWidgets() {
        for (ArgControl control : this.argControls) {
            if (control.field != null) {
                control.value = control.field.getText() == null ? "" : control.field.getText().trim();
            }
        }
    }

    private void syncWidgetsFromControlValues() {
        for (ArgControl control : this.argControls) {
            if (control.field != null) {
                control.field.setText(control.value == null ? "" : control.value);
            } else {
                GuiButton button = findButtonById(control.buttonId);
                if (button != null) {
                    button.displayString = buildArgButtonLabel(control);
                }
            }
        }
    }

    private void updateRawCommandFromControls() {
        CommandEntry selected = getSelectedCommand();
        if (selected == null || this.rawCommandField == null) {
            return;
        }
        syncControlValuesFromWidgets();

        StringBuilder builder = new StringBuilder(selected.primaryName);
        for (ArgControl control : this.argControls) {
            String value = control.value == null ? "" : control.value.trim();
            if (!value.isEmpty()) {
                builder.append(' ').append(value);
            }
        }
        this.rawCommandField.setText(builder.toString());
        this.rawCommandField.setCursorPositionEnd();
    }

    private void clearArguments() {
        for (ArgControl control : this.argControls) {
            control.value = "";
        }
        syncWidgetsFromControlValues();
        CommandEntry selected = getSelectedCommand();
        this.rawCommandField.setText(selected == null ? "" : selected.primaryName);
        this.rawCommandField.setCursorPositionEnd();
        validateArgControls(false);
        this.statusMessage = "参数已清空";
        this.statusColor = 0xFFE7C35A;
    }

    private void fillFromExample(ExampleEntry example) {
        if (example == null) {
            return;
        }
        fillFromRawCommand(example.commandText, true);
        this.statusMessage = example.description.isEmpty() ? "已填充示例命令" : "已填充示例：" + example.description;
        this.statusColor = 0xFF73D98A;
    }

    private void fillFromRawCommand(String rawCommand, boolean allowReselect) {
        if (rawCommand == null) {
            return;
        }
        String command = rawCommand.trim();
        if (command.isEmpty()) {
            return;
        }

        String[] tokens = command.split("\\s+");
        if (allowReselect && tokens.length > 0) {
            CommandEntry matched = findCommandByName(tokens[0]);
            if (matched != null && !matched.primaryName.equals(this.selectedCommandName)) {
                this.selectedCommandName = matched.primaryName;
                initGui();
            }
        }

        if (this.rawCommandField != null) {
            this.rawCommandField.setText(command);
            this.rawCommandField.setCursorPositionEnd();
        }

        int startIndex = tokens.length > 0 ? 1 : 0;
        for (int i = 0; i < this.argControls.size(); i++) {
            this.argControls.get(i).value = (startIndex + i) < tokens.length ? tokens[startIndex + i] : "";
        }
        syncWidgetsFromControlValues();
        validateArgControls(false);
    }

    private void executeCurrentCommand() {
        if (this.rawCommandField == null) {
            return;
        }

        updateRawCommandFromControls();
        if (validateArgControls(true)) {
            return;
        }

        String command = this.rawCommandField.getText() == null ? "" : this.rawCommandField.getText().trim();
        if (command.isEmpty()) {
            this.statusMessage = "请输入命令";
            this.statusColor = 0xFFFF6E6E;
            return;
        }

        try {
            boolean ok = BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(command);
            if (ok) {
                String[] tokens = command.split("\\s+");
                if (tokens.length > 0) {
                    recordRecentCommand(tokens[0]);
                }
                recordHistory(command);
                this.statusMessage = "命令已执行：" + command;
                this.statusColor = 0xFF73D98A;
            } else {
                this.statusMessage = "命令未执行，可能命令名或参数无效";
                this.statusColor = 0xFFFF6E6E;
            }
        } catch (Throwable t) {
            this.statusMessage = "执行失败：" + t.getClass().getSimpleName();
            this.statusColor = 0xFFFF6E6E;
        }
    }

    private void recordRecentCommand(String commandName) {
        if (commandName == null || commandName.trim().isEmpty()) {
            return;
        }
        String normalized = commandName.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < RECENT_COMMANDS.size(); i++) {
            if (RECENT_COMMANDS.get(i).equalsIgnoreCase(normalized)) {
                RECENT_COMMANDS.remove(i);
                break;
            }
        }
        RECENT_COMMANDS.add(0, normalized);
        while (RECENT_COMMANDS.size() > MAX_RECENT_COMMANDS) {
            RECENT_COMMANDS.remove(RECENT_COMMANDS.size() - 1);
        }
        savePersistentState();
    }

    private void recordHistory(String rawCommand) {
        EXECUTION_HISTORY.add(0, new HistoryEntry(HISTORY_TIME_FORMAT.format(new Date()), rawCommand));
        while (EXECUTION_HISTORY.size() > MAX_HISTORY_ITEMS) {
            EXECUTION_HISTORY.remove(EXECUTION_HISTORY.size() - 1);
        }
        int maxScroll = Math.max(0, EXECUTION_HISTORY.size() - this.historyVisibleRows);
        this.historyScroll = MathHelper.clamp(this.historyScroll, 0, maxScroll);
        savePersistentState();
    }

    private void toggleFavoriteSelected() {
        CommandEntry selected = getSelectedCommand();
        if (selected == null) {
            return;
        }
        if (FAVORITE_COMMANDS.contains(selected.primaryName)) {
            FAVORITE_COMMANDS.remove(selected.primaryName);
            this.statusMessage = "已取消收藏：" + selected.primaryName;
            this.statusColor = 0xFFE7C35A;
        } else {
            FAVORITE_COMMANDS.add(selected.primaryName);
            this.statusMessage = "已收藏：" + selected.primaryName;
            this.statusColor = 0xFF73D98A;
        }
        savePersistentState();
        initGui();
    }

    private String buildMainSectionLabel(MainSection section) {
        boolean active = this.currentSection == section;
        return (active ? "§b" : "§7") + section.label;
    }

    private String buildHumanLikeToggleLabel() {
        return HumanLikeMovementConfig.INSTANCE.enabled ? "§a拟人化: 开" : "§7拟人化: 关";
    }

    private void toggleHumanLikeMovement() {
        HumanLikeMovementConfig.INSTANCE.enabled = !HumanLikeMovementConfig.INSTANCE.enabled;
        HumanLikeMovementConfig.INSTANCE.normalize();
        HumanLikeMovementConfig.save();
        if (!HumanLikeMovementConfig.INSTANCE.enabled) {
            HumanLikeMovementController.INSTANCE.reset();
        }

        GuiButton button = findButtonById(BTN_HUMAN_LIKE_MOVEMENT);
        if (button != null) {
            button.displayString = buildHumanLikeToggleLabel();
            if (button instanceof ToggleGuiButton) {
                ((ToggleGuiButton) button).setEnabledState(HumanLikeMovementConfig.INSTANCE.enabled);
            }
        }
        this.statusMessage = HumanLikeMovementConfig.INSTANCE.enabled ? "拟人化已开启" : "拟人化已关闭";
        this.statusColor = HumanLikeMovementConfig.INSTANCE.enabled ? 0xFF73D98A : 0xFFE7C35A;
    }

    private String buildModeDropdownLabel() {
        return "§b模式: §f" + this.commandListMode.label + " §7▼";
    }

    private String buildCategoryDropdownLabel() {
        return "§a分类: §f" + this.activeCategory.label + " §7▼";
    }

    private String buildTabLabel(RightTab tab) {
        return (this.currentRightTab == tab ? "§b" : "§7") + tab.label;
    }

    private String buildArgButtonLabel(ArgControl control) {
        String value = control.value == null || control.value.trim().isEmpty() ? "点击选择" : control.value.trim();
        return control.schema.label + ": " + value;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.hoverTooltip = null;

        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelW, this.panelH);

        if (this.currentSection == MainSection.SETTINGS) {
            drawSettingsView(mouseX, mouseY, partialTicks);
        } else {
            drawCommandView(mouseX, mouseY, partialTicks);
        }

        if (this.hoverTooltip != null && !this.hoverTooltip.trim().isEmpty()) {
            GuiUtils.drawHoveringText(Arrays.asList(this.hoverTooltip.split("\n")), this.hoverTooltipX, this.hoverTooltipY,
                    this.width, this.height, -1, this.fontRenderer);
        }
    }

    private void drawCommandView(int mouseX, int mouseY, float partialTicks) {
        drawCenteredString(this.fontRenderer, "Baritone命令表", this.panelX + this.panelW / 2, this.panelY + 10, 0xFFFFFFFF);

        GuiTheme.drawPanelSegment(this.leftPaneX, this.leftPaneY, this.leftPaneW, this.leftPaneH,
                this.panelX, this.panelY, this.panelW, this.panelH);
        GuiTheme.drawSectionTitle(this.leftPaneX + 8, this.leftPaneY + 8, this.leftPaneCollapsed ? "命令" : "命令列表", this.fontRenderer);
        drawCollapseButton(this.leftPaneX, this.leftPaneY, this.leftPaneW, this.leftPaneCollapsed);

        if (!this.leftPaneCollapsed) {
            drawString(this.fontRenderer,
                    String.format("全部 %d / 当前 %d / 收藏 %d / 最近 %d", this.allCommands.size(), this.filteredCommands.size(),
                            FAVORITE_COMMANDS.size(), RECENT_COMMANDS.size()),
                    this.leftPaneX + 8, this.leftPaneY + 76, 0xFF9FB2C8);
            drawCommandList(mouseX, mouseY);
        }

        GuiTheme.drawPanelSegment(this.middlePaneX, this.middlePaneY, this.middlePaneW, this.middlePaneH,
                this.panelX, this.panelY, this.panelW, this.panelH);
        GuiTheme.drawSectionTitle(this.middlePaneX + 8, this.middlePaneY + 8, this.historyPaneCollapsed ? "历史" : "执行历史", this.fontRenderer);
        drawCollapseButton(this.middlePaneX, this.middlePaneY, this.middlePaneW, this.historyPaneCollapsed);

        if (!this.historyPaneCollapsed) {
            drawHistoryPane(mouseX, mouseY);
        }

        drawRightPane(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (!this.leftPaneCollapsed && this.searchField != null) {
            drawThemedTextField(this.searchField);
            if ((this.searchField.getText() == null || this.searchField.getText().isEmpty()) && !this.searchField.isFocused()) {
                drawString(this.fontRenderer, "输入命令名、别名或说明", this.searchField.x + 4, this.searchField.y + 5, 0xFF8A8A8A);
            }
        }

        for (ArgControl control : this.argControls) {
            if (control.field != null && control.visible) {
                if (control.invalid) {
                    Gui.drawRect(control.field.x - 2, control.field.y - 2, control.field.x + control.field.width + 2, control.field.y + control.field.height + 2, 0xAA9A2A2A);
                    Gui.drawRect(control.field.x - 1, control.field.y - 1, control.field.x + control.field.width + 1, control.field.y + control.field.height + 1, 0xFFFF6E6E);
                }
                drawThemedTextField(control.field);
            }
        }

        if (this.currentRightTab == RightTab.PREVIEW && this.rawCommandField != null
                && this.rawCommandField.x > -1000 && this.rawCommandField.y > -1000) {
            drawThemedTextField(this.rawCommandField);
        }

        drawDropdownOverlays(mouseX, mouseY);
        updateTopButtonTooltip(mouseX, mouseY);
    }

    private void updateTopButtonTooltip(int mouseX, int mouseY) {
        GuiButton button = findButtonById(BTN_HUMAN_LIKE_MOVEMENT);
        if (button == null || !button.visible || mouseX < button.x || mouseX >= button.x + button.width
                || mouseY < button.y || mouseY >= button.y + button.height) {
            return;
        }
        this.hoverTooltip = "§e拟人化总开关"
                + "\n§7左键：§f启用/关闭 Baritone 视角与移动节律修饰"
                + "\n§7右键：§f打开详细参数配置"
                + "\n§7当前：§f" + (HumanLikeMovementConfig.INSTANCE.enabled ? "已开启" : "已关闭");
        this.hoverTooltipX = mouseX;
        this.hoverTooltipY = mouseY;
    }

    private void drawCommandList(int mouseX, int mouseY) {
        int startIndex = this.commandScroll;
        int endIndex = Math.min(this.filteredCommands.size(), startIndex + this.commandVisibleRows);

        for (int index = startIndex; index < endIndex; index++) {
            int row = index - startIndex;
            int rowX = this.commandListX;
            int rowY = this.commandListY + row * COMMAND_ROW_HEIGHT;
            int rowW = this.commandListW;
            int rowH = COMMAND_ROW_HEIGHT - 2;

            CommandEntry entry = this.filteredCommands.get(index);
            boolean selected = entry.primaryName.equals(this.selectedCommandName);
            boolean hover = mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH;

            GuiTheme.drawButtonFrame(rowX, rowY, rowW, rowH,
                    selected ? GuiTheme.UiState.SELECTED : (hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            if (selected) {
                Gui.drawRect(rowX + 1, rowY + 1, rowX + 4, rowY + rowH - 1, 0xFF7ED0FF);
            }

            String favoriteIcon = FAVORITE_COMMANDS.contains(entry.primaryName) ? "§6★ " : "";
            drawString(this.fontRenderer, trimToWidth(favoriteIcon + entry.primaryName, rowW - 18), rowX + 6, rowY + 4, 0xFFFFFFFF);
            drawString(this.fontRenderer, trimToWidth(entry.category.label + " · " + entry.shortDesc, rowW - 18), rowX + 6, rowY + 15, 0xFFAAC2D9);

            if (hover) {
                this.hoverTooltip = "§e命令：§f" + entry.primaryName
                        + "\n§7分类：§f" + entry.category.label
                        + "\n§7别名：§f" + (entry.aliases.isEmpty() ? "无" : String.join("、", entry.aliases))
                        + "\n§7简介：§f" + safe(entry.shortDesc);
                this.hoverTooltipX = mouseX;
                this.hoverTooltipY = mouseY;
            }
        }

        if (this.filteredCommands.isEmpty()) {
            GuiTheme.drawEmptyState(this.leftPaneX + this.leftPaneW / 2, this.leftPaneY + 140, "当前筛选下没有命令", this.fontRenderer);
            return;
        }

        int maxScroll = Math.max(0, this.filteredCommands.size() - this.commandVisibleRows);
        if (maxScroll > 0) {
            int scrollbarX = this.leftPaneX + this.leftPaneW - 6;
            int scrollbarY = this.commandListY;
            int scrollbarH = this.commandListH;
            int thumbH = Math.max(20, (int) ((float) this.commandVisibleRows / this.filteredCommands.size() * scrollbarH));
            int thumbY = scrollbarY + (int) ((float) this.commandScroll / maxScroll * (scrollbarH - thumbH));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 4, scrollbarH, thumbY, thumbH);
        }
    }

    private void drawHistoryPane(int mouseX, int mouseY) {
        int boxX = this.middlePaneX + 8;
        int boxY = this.middlePaneY + 8;
        int boxW = this.middlePaneW - 16;

        drawString(this.fontRenderer, "执行记录（点击可回填）", boxX, boxY + 12, 0xFF9FB2C8);

        this.historyVisibleRows = Math.max(1, this.historyListH / HISTORY_ROW_HEIGHT);
        int maxHistoryScroll = Math.max(0, EXECUTION_HISTORY.size() - this.historyVisibleRows);
        this.historyScroll = MathHelper.clamp(this.historyScroll, 0, maxHistoryScroll);

        if (EXECUTION_HISTORY.isEmpty()) {
            GuiTheme.drawEmptyState(this.middlePaneX + this.middlePaneW / 2, this.middlePaneY + this.middlePaneH / 2,
                    "暂无执行历史", this.fontRenderer);
            return;
        }

        for (int i = 0; i < this.historyVisibleRows; i++) {
            int index = this.historyScroll + i;
            if (index >= EXECUTION_HISTORY.size()) {
                break;
            }

            HistoryEntry entry = EXECUTION_HISTORY.get(index);
            int rowY = this.historyListY + i * HISTORY_ROW_HEIGHT;
            boolean hover = mouseX >= this.historyListX && mouseX < this.historyListX + this.historyListW
                    && mouseY >= rowY && mouseY < rowY + HISTORY_ROW_HEIGHT - 1;

            if (hover) {
                GuiTheme.drawCardHighlight(this.historyListX, rowY - 1, this.historyListW - 2, HISTORY_ROW_HEIGHT, true);
                this.hoverTooltip = "§e点击重新填充该命令\n§7时间：§f" + entry.time + "\n§7命令：§f" + entry.command;
                this.hoverTooltipX = mouseX;
                this.hoverTooltipY = mouseY;
            }

            drawString(this.fontRenderer,
                    "§7[" + entry.time + "] §f" + trimToWidth(entry.command, Math.max(40, boxW - 24)),
                    this.historyListX + 2, rowY + 4, 0xFFE6EEF8);
        }

        if (maxHistoryScroll > 0) {
            int scrollbarX = this.middlePaneX + this.middlePaneW - 8;
            int scrollbarY = this.historyListY;
            int scrollbarH = this.historyListH;
            int thumbH = Math.max(16, (int) ((float) this.historyVisibleRows / EXECUTION_HISTORY.size() * scrollbarH));
            int thumbY = scrollbarY + (int) ((float) this.historyScroll / maxHistoryScroll * (scrollbarH - thumbH));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 4, scrollbarH, thumbY, thumbH);
        }
    }

    private void drawRightPane(int mouseX, int mouseY, float partialTicks) {
        GuiTheme.drawPanelSegment(this.rightPaneX, this.rightPaneY, this.rightPaneW, this.rightPaneH,
                this.panelX, this.panelY, this.panelW, this.panelH);
        GuiTheme.drawSectionTitle(this.rightPaneX + 8, this.rightPaneY + 8, this.rightPaneCollapsed ? "详情" : "命令详情", this.fontRenderer);
        drawCollapseButton(this.rightPaneX, this.rightPaneY, this.rightPaneW, this.rightPaneCollapsed);

        if (this.rightPaneCollapsed) {
            hideRightPaneInteractiveControls();
            return;
        }

        CommandEntry selected = getSelectedCommand();
        if (selected == null) {
            hideRightPaneInteractiveControls();
            GuiTheme.drawEmptyState(this.rightPaneX + this.rightPaneW / 2, this.rightPaneY + this.rightPaneH / 2, "没有可显示的命令", this.fontRenderer);
            return;
        }

        int viewportY = getRightViewportY();
        int viewportH = getRightViewportH();
        this.rightScroll = MathHelper.clamp(this.rightScroll, 0, Math.max(0, this.rightContentHeight - viewportH));

        int contentX = this.rightPaneX + 12;
        int topY = viewportY + 4 - this.rightScroll;
        int tabY = topY + 70;
        int tabGap = 4;
        int tabW = Math.max(52, (this.rightPaneW - 24 - tabGap * 3) / 4);

        positionButtonInRightViewport(BTN_FAVORITE_TOGGLE, this.rightPaneX + this.rightPaneW - 92, topY, 80, 18,
                FAVORITE_COMMANDS.contains(selected.primaryName) ? "§6已收藏" : "§7收藏");
        positionButtonInRightViewport(BTN_TAB_PARAMS, this.rightPaneX + 12, tabY, tabW, 18, buildTabLabel(RightTab.PARAMS));
        positionButtonInRightViewport(BTN_TAB_PREVIEW, this.rightPaneX + 12 + (tabW + tabGap), tabY, tabW, 18, buildTabLabel(RightTab.PREVIEW));
        positionButtonInRightViewport(BTN_TAB_EXAMPLES, this.rightPaneX + 12 + (tabW + tabGap) * 2, tabY, tabW, 18, buildTabLabel(RightTab.EXAMPLES));
        positionButtonInRightViewport(BTN_TAB_DETAILS, this.rightPaneX + 12 + (tabW + tabGap) * 3, tabY, tabW, 18, buildTabLabel(RightTab.DETAILS));

        if (isInRightViewport(topY, 62)) {
            int topTextWidth = Math.max(80, this.rightPaneW - 126);
            drawString(this.fontRenderer, "命令名", contentX, topY, 0xFF9FB2C8);
            drawString(this.fontRenderer, trimToWidth(selected.primaryName, topTextWidth), contentX, topY + 12, 0xFFFFFFFF);

            drawString(this.fontRenderer,
                    trimToWidth("分类： " + selected.category.label, this.rightPaneW - 24),
                    contentX, topY + 28, 0xFFE6EEF8);

            drawString(this.fontRenderer,
                    trimToWidth("别名： " + (selected.aliases.isEmpty() ? "无" : String.join("、", selected.aliases)), this.rightPaneW - 24),
                    contentX, topY + 40, 0xFFE6EEF8);

            drawString(this.fontRenderer,
                    trimToWidth("简介： " + selected.shortDesc, this.rightPaneW - 24),
                    contentX, topY + 52, 0xFFFFFFFF);
        }

        this.currentContentBoxY = viewportY + 96 - this.rightScroll;
        this.currentContentBoxH = Math.max(84, measureCurrentRightContentHeight(selected));
        int visibleBoxY = Math.max(getContentBoxY(), getRightViewportY());
        int visibleBoxBottom = Math.min(getContentBoxY() + getContentBoxH(), getRightViewportY() + getRightViewportH());
        if (visibleBoxBottom > visibleBoxY) {
            GuiTheme.drawPanelSegment(getContentBoxX(), visibleBoxY, getContentBoxW(), visibleBoxBottom - visibleBoxY,
                    this.panelX, this.panelY, this.panelW, this.panelH);
        }

        hidePreviewField();
        hideArgControls();
        hideExampleButtons();

        if (this.currentRightTab == RightTab.PARAMS) {
            drawParamsTab(mouseX, mouseY);
        } else if (this.currentRightTab == RightTab.PREVIEW) {
            drawPreviewTab();
        } else if (this.currentRightTab == RightTab.EXAMPLES) {
            drawExamplesTab(mouseX, mouseY, selected);
        } else if (this.currentRightTab == RightTab.DETAILS) {
            drawDetailsTab();
        }

        this.rightContentHeight = 96 + this.currentContentBoxH + 8;
        this.rightScroll = MathHelper.clamp(this.rightScroll, 0, Math.max(0, this.rightContentHeight - viewportH));

        drawString(this.fontRenderer, this.statusMessage == null ? "" : this.statusMessage,
                contentX, this.rightPaneY + this.rightPaneH - 14, this.statusColor);
    }

    private void drawParamsTab(int mouseX, int mouseY) {
        int boxX = getContentBoxX();
        int boxY = getContentBoxY();
        if (isInRightViewport(boxY, 18)) {
            drawString(this.fontRenderer, "参数面板", boxX + 8, boxY + 6, 0xFF9FB2C8);
        }

        if (this.argControls.isEmpty()) {
            if (isInRightViewport(boxY + 22, 18)) {
                drawString(this.fontRenderer, "该命令通常无需参数，可直接切到“命令预览”执行。", boxX + 8, boxY + 28, 0xFFB8C7D8);
            }
            return;
        }

        layoutArgControls();

        for (int i = 0; i < this.argControls.size(); i++) {
            ArgControl control = this.argControls.get(i);
            if (!control.visible) {
                continue;
            }

            int labelY = control.y - 10;
            drawString(this.fontRenderer, control.schema.label + (control.schema.required ? " *" : ""), control.x, labelY, 0xFF9FB2C8);

            if (control.field != null) {
                if ((control.field.getText() == null || control.field.getText().isEmpty()) && !control.field.isFocused()) {
                    drawString(this.fontRenderer, "输入 " + control.schema.placeholder, control.field.x + 4, control.field.y + 4, 0xFF8A8A8A);
                }
            } else {
                GuiButton button = findButtonById(control.buttonId);
                if (button != null) {
                    button.displayString = buildArgButtonLabel(control);
                    if (control.invalid) {
                        Gui.drawRect(button.x - 1, button.y - 1, button.x + button.width + 1, button.y + button.height + 1, 0xFFFF6E6E);
                    }
                }
            }

            drawString(this.fontRenderer, control.validationMessage, control.x, control.y + 18, control.validationColor);

            if (mouseX >= control.x && mouseX < control.x + control.w
                    && mouseY >= control.y && mouseY < control.y + control.h) {
                this.hoverTooltip = "§e" + control.schema.label
                        + "\n§7提示：§f" + control.schema.placeholder
                        + "\n§7说明：§f" + control.schema.description;
                this.hoverTooltipX = mouseX;
                this.hoverTooltipY = mouseY;
            }
        }
    }

    private void drawPreviewTab() {
        int boxX = getContentBoxX();
        int boxY = getContentBoxY();
        int fieldY = boxY + 24;

        if (isInRightViewport(boxY, 18)) {
            drawString(this.fontRenderer, "命令预览 / 可手动编辑", boxX + 8, boxY + 6, 0xFF9FB2C8);
        }

        if (this.rawCommandField != null) {
            if (isInRightViewport(fieldY - 2, 22)) {
                this.rawCommandField.x = boxX + 8;
                this.rawCommandField.y = fieldY;
                this.rawCommandField.width = getContentBoxW() - 16;
                this.rawCommandField.height = 18;
            } else {
                this.rawCommandField.x = -2000;
                this.rawCommandField.y = -2000;
            }
        }

        if (isInRightViewport(boxY + 40, 18)) {
            drawString(this.fontRenderer, "参数变更后会自动同步到下方命令；也可直接手动改写。", boxX + 8, boxY + 48, 0xFFB8C7D8);
        }

        if (this.rawCommandField != null && this.rawCommandField.x > -1000
                && (this.rawCommandField.getText() == null || this.rawCommandField.getText().isEmpty())
                && !this.rawCommandField.isFocused()) {
            drawString(this.fontRenderer, "例如：goto 100 64 100", this.rawCommandField.x + 4, this.rawCommandField.y + 5, 0xFF8A8A8A);
        }
    }

    private void drawExamplesTab(int mouseX, int mouseY, CommandEntry selected) {
        int boxX = getContentBoxX();
        int boxY = getContentBoxY();

        if (isInRightViewport(boxY, 18)) {
            drawString(this.fontRenderer, "快速示例", boxX + 8, boxY + 6, 0xFF9FB2C8);
        }
        if (selected.examples.isEmpty()) {
            if (isInRightViewport(boxY + 22, 18)) {
                drawString(this.fontRenderer, "当前命令没有内置示例。", boxX + 8, boxY + 28, 0xFFB8C7D8);
            }
            return;
        }

        int startX = boxX + 10;
        int startY = boxY + 28;
        int buttonW = Math.max(90, Math.min(190, (getContentBoxW() - 24) / 2));
        int buttonH = 18;
        int gap = 6;

        for (int i = 0; i < Math.min(10, selected.examples.size()); i++) {
            int row = i / 2;
            int col = i % 2;
            int x = startX + col * (buttonW + gap);
            int y = startY + row * (buttonH + gap);
            ExampleEntry example = selected.examples.get(i);
            GuiButton button = findButtonById(BTN_EXAMPLE_BASE + i);
            if (button != null) {
                if (isInRightViewport(y - 1, buttonH + 2)) {
                    button.visible = true;
                    button.enabled = true;
                    button.x = x;
                    button.y = y;
                    button.width = buttonW;
                    button.height = buttonH;
                    button.displayString = trimToWidth(example.commandText, buttonW - 8);
                } else {
                    button.visible = false;
                    button.enabled = false;
                    button.x = -2000;
                    button.y = -2000;
                }
            }
            if (button != null && button.visible && mouseX >= button.x && mouseX < button.x + button.width
                    && mouseY >= button.y && mouseY < button.y + button.height) {
                this.hoverTooltip = example.description.isEmpty()
                        ? "§e" + example.commandText
                        : "§e" + example.commandText + "\n§7" + example.description;
                this.hoverTooltipX = mouseX;
                this.hoverTooltipY = mouseY;
            }
        }

        int detailStartY = startY + ((Math.min(10, selected.examples.size()) + 1) / 2) * (buttonH + gap) + 14;
        if (isInRightViewport(detailStartY - 6, 18)) {
            Gui.drawRect(boxX + 8, detailStartY - 6, boxX + getContentBoxW() - 8, detailStartY - 5, 0x335A7A98);
            drawString(this.fontRenderer, "示例说明", boxX + 8, detailStartY, 0xFF9FB2C8);
        }

        if (isInRightViewport(detailStartY + 16, 18)) {
            drawString(this.fontRenderer, "示例命令：点击按钮即可回填到当前参数和命令预览。", boxX + 8, detailStartY + 16, 0xFFB8C7D8);
        }

        int availableW = getContentBoxW() - 16;
        int currentY = detailStartY + 32;
        for (int i = 0; i < Math.min(5, selected.examples.size()); i++) {
            ExampleEntry example = selected.examples.get(i);
            String line = "§7- §f" + example.commandText + (example.description.isEmpty() ? "" : " §8| §7" + example.description);
            List<String> wrapped = this.fontRenderer.listFormattedStringToWidth(line, availableW);
            for (String text : wrapped) {
                if (isInRightViewport(currentY, this.fontRenderer.FONT_HEIGHT + 2)) {
                    drawString(this.fontRenderer, text, boxX + 8, currentY, 0xFFE6EEF8);
                }
                currentY += this.fontRenderer.FONT_HEIGHT + 2;
            }
        }
    }

    private void drawDetailsTab() {
        int boxX = getContentBoxX();
        int boxY = getContentBoxY();

        if (isInRightViewport(boxY, 18)) {
            drawString(this.fontRenderer, "详细说明", boxX + 8, boxY + 6, 0xFF9FB2C8);
        }

        int lineHeight = this.fontRenderer.FONT_HEIGHT + 2;
        for (int i = 0; i < this.wrappedDetailLines.size(); i++) {
            int drawY = boxY + 20 + i * lineHeight;
            if (isInRightViewport(drawY, lineHeight)) {
                drawString(this.fontRenderer, this.wrappedDetailLines.get(i), boxX + 8, drawY, 0xFFE6EEF8);
            }
        }
    }

    private void drawHistoryTab(int mouseX, int mouseY) {
        int boxX = getContentBoxX();
        int boxY = getContentBoxY();
        int boxW = getContentBoxW();
        int boxH = getContentBoxH();

        drawString(this.fontRenderer, "执行历史（已持久化）", boxX + 8, boxY + 6, 0xFF9FB2C8);

        int listY = boxY + 24;
        this.historyVisibleRows = Math.max(1, (boxH - 30) / HISTORY_ROW_HEIGHT);
        int maxHistoryScroll = Math.max(0, EXECUTION_HISTORY.size() - this.historyVisibleRows);
        this.historyScroll = MathHelper.clamp(this.historyScroll, 0, maxHistoryScroll);

        if (EXECUTION_HISTORY.isEmpty()) {
            drawString(this.fontRenderer, "暂无执行历史，执行命令后会自动保存到本地。", boxX + 8, boxY + 28, 0xFFB8C7D8);
            return;
        }

        for (int i = 0; i < this.historyVisibleRows; i++) {
            int index = this.historyScroll + i;
            if (index >= EXECUTION_HISTORY.size()) {
                break;
            }
            HistoryEntry entry = EXECUTION_HISTORY.get(index);
            int rowY = listY + i * HISTORY_ROW_HEIGHT;
            boolean hover = mouseX >= boxX + 6 && mouseX < boxX + boxW - 8
                    && mouseY >= rowY - 1 && mouseY < rowY + HISTORY_ROW_HEIGHT - 1;

            if (hover) {
                GuiTheme.drawCardHighlight(boxX + 6, rowY - 1, boxW - 14, HISTORY_ROW_HEIGHT - 1, true);
                this.hoverTooltip = "§e点击可重新填充该历史命令\n§7时间：§f" + entry.time + "\n§7命令：§f" + entry.command;
                this.hoverTooltipX = mouseX;
                this.hoverTooltipY = mouseY;
            }

            drawString(this.fontRenderer, "§7[" + entry.time + "] §f" + trimToWidth(entry.command, boxW - 22),
                    boxX + 8, rowY, 0xFFE6EEF8);
        }

        if (maxHistoryScroll > 0) {
            int scrollbarX = boxX + boxW - 6;
            int scrollbarY = boxY + 20;
            int scrollbarH = boxH - 24;
            int thumbH = Math.max(16, (int) ((float) this.historyVisibleRows / EXECUTION_HISTORY.size() * scrollbarH));
            int thumbY = scrollbarY + (int) ((float) this.historyScroll / maxHistoryScroll * (scrollbarH - thumbH));
            GuiTheme.drawScrollbar(scrollbarX, scrollbarY, 4, scrollbarH, thumbY, thumbH);
        }
    }

    private int getTabRowY() {
        return this.currentContentBoxY - 24;
    }

    private int getContentBoxX() {
        return this.rightPaneX + 12;
    }

    private int getContentBoxY() {
        return this.currentContentBoxY;
    }

    private int getContentBoxW() {
        return this.rightPaneW - 24;
    }

    private int getContentBoxH() {
        return this.currentContentBoxH;
    }

    private int getRightViewportY() {
        return this.rightPaneY + 22;
    }

    private int getRightViewportH() {
        return Math.max(80, this.rightPaneH - 38);
    }

    private boolean isInRightViewport(int y, int height) {
        int top = getRightViewportY();
        int bottom = top + getRightViewportH();
        return y + height >= top && y <= bottom;
    }

    private void hidePreviewField() {
        if (this.rawCommandField != null) {
            this.rawCommandField.x = -2000;
            this.rawCommandField.y = -2000;
        }
    }

    private void hideArgControls() {
        for (ArgControl control : this.argControls) {
            control.visible = false;
            control.x = -2000;
            control.y = -2000;
            if (control.field != null) {
                control.field.x = -2000;
                control.field.y = -2000;
            } else {
                GuiButton button = findButtonById(control.buttonId);
                if (button != null) {
                    button.visible = false;
                    button.enabled = false;
                    button.x = -2000;
                    button.y = -2000;
                }
            }
        }
    }

    private void hideExampleButtons() {
        for (int i = 0; i < 20; i++) {
            GuiButton button = findButtonById(BTN_EXAMPLE_BASE + i);
            if (button != null) {
                button.visible = false;
                button.enabled = false;
                button.x = -2000;
                button.y = -2000;
            }
        }
    }

    private void hideRightPaneInteractiveControls() {
        hidePreviewField();
        hideArgControls();
        hideExampleButtons();
        int[] ids = {BTN_FAVORITE_TOGGLE, BTN_TAB_PARAMS, BTN_TAB_PREVIEW, BTN_TAB_EXAMPLES, BTN_TAB_DETAILS};
        for (int id : ids) {
            GuiButton button = findButtonById(id);
            if (button != null) {
                button.visible = false;
                button.enabled = false;
                button.x = -2000;
                button.y = -2000;
            }
        }
    }

    private void positionButtonInRightViewport(int id, int x, int y, int width, int height, String label) {
        GuiButton button = findButtonById(id);
        if (button == null) {
            return;
        }
        boolean visible = isInRightViewport(y, height);
        button.visible = visible;
        button.enabled = visible;
        button.width = width;
        button.height = height;
        button.displayString = label;
        button.x = visible ? x : -2000;
        button.y = visible ? y : -2000;
    }

    private int measureCurrentRightContentHeight(CommandEntry selected) {
        if (this.currentRightTab == RightTab.PARAMS) {
            return this.argControls.isEmpty() ? 58 : 36 + getArgTotalRows() * ARG_ROW_HEIGHT;
        }
        if (this.currentRightTab == RightTab.PREVIEW) {
            return 82;
        }
        if (this.currentRightTab == RightTab.EXAMPLES) {
            int rows = Math.max(1, (Math.min(10, selected.examples.size()) + 1) / 2);
            int buttonH = 18;
            int gap = 6;
            int height = 34 + rows * (buttonH + gap) + 34;

            int availableW = Math.max(120, getContentBoxW() - 16);
            for (int i = 0; i < Math.min(5, selected.examples.size()); i++) {
                ExampleEntry example = selected.examples.get(i);
                String line = "§7- §f" + example.commandText + (example.description.isEmpty() ? "" : " §8| §7" + example.description);
                List<String> wrapped = this.fontRenderer.listFormattedStringToWidth(line, availableW);
                height += wrapped.size() * (this.fontRenderer.FONT_HEIGHT + 2);
            }
            return Math.max(84, height + 12);
        }
        int lineHeight = this.fontRenderer.FONT_HEIGHT + 2;
        return Math.max(84, 24 + this.wrappedDetailLines.size() * lineHeight);
    }

    private boolean isInCollapseButton(int mouseX, int mouseY, int boxX, int boxY, int boxWidth) {
        int btnSize = 14;
        int btnX = boxX + boxWidth - btnSize - 6;
        int btnY = boxY + 5;
        return mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;
    }

    private void drawCollapseButton(int boxX, int boxY, int boxWidth, boolean collapsed) {
        int btnSize = 14;
        int btnX = boxX + boxWidth - btnSize - 6;
        int btnY = boxY + 5;
        drawRect(btnX, btnY, btnX + btnSize, btnY + btnSize, 0xAA203146);
        drawHorizontalLine(btnX, btnX + btnSize, btnY, 0xFF4FA6D9);
        drawHorizontalLine(btnX, btnX + btnSize, btnY + btnSize, 0xFF3F6A8C);
        drawVerticalLine(btnX, btnY, btnY + btnSize, 0xFF3F6A8C);
        drawVerticalLine(btnX + btnSize, btnY, btnY + btnSize, 0xFF3F6A8C);
        drawString(this.fontRenderer, collapsed ? ">" : "<", btnX + 4, btnY + 3, 0xFFEAF7FF);
    }

    private int getCollapsedSectionWidth(String shortTitle) {
        int len = shortTitle == null ? 0 : Math.min(2, shortTitle.length());
        int textWidth = this.fontRenderer == null ? 16
                : this.fontRenderer.getStringWidth(shortTitle == null ? "" : shortTitle.substring(0, len));
        return Math.max(34, textWidth + 24);
    }

    private GuiButton findButtonById(int id) {
        for (GuiButton button : this.buttonList) {
            if (button.id == id) {
                return button;
            }
        }
        return null;
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

    private String safe(String text) {
        return text == null ? "" : text;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (this.currentSection == MainSection.SETTINGS) {
            handleSettingsAction(button);
            return;
        }

        if (button.id == BTN_BACK) {
            this.mc.setScreen(this.parentScreen);
            return;
        }
        if (button.id == BTN_NAV_COMMANDS) {
            this.currentSection = MainSection.COMMANDS;
            initGui();
            return;
        }
        if (button.id == BTN_NAV_SETTINGS || button.id == BTN_OPEN_SETTINGS) {
            this.currentSection = MainSection.SETTINGS;
            initGui();
            return;
        }
        if (button.id == BTN_HUMAN_LIKE_MOVEMENT) {
            toggleHumanLikeMovement();
            return;
        }
        if (button.id == BTN_EXECUTE) {
            executeCurrentCommand();
            return;
        }
        if (button.id == BTN_CLEAR_ARGS) {
            clearArguments();
            return;
        }
        if (button.id == BTN_FAVORITE_TOGGLE) {
            toggleFavoriteSelected();
            return;
        }
        if (button.id == BTN_CLEAR_HISTORY) {
            EXECUTION_HISTORY.clear();
            this.historyScroll = 0;
            savePersistentState();
            this.statusMessage = "执行历史已清空";
            this.statusColor = 0xFFE7C35A;
            return;
        }
        if (button.id == BTN_FILTER_MODE) {
            this.modeDropdownOpen = !this.modeDropdownOpen;
            this.categoryDropdownOpen = false;
            return;
        }
        if (button.id == BTN_FILTER_CATEGORY) {
            this.categoryDropdownOpen = !this.categoryDropdownOpen;
            this.modeDropdownOpen = false;
            return;
        }
        if (button.id == BTN_TAB_PARAMS) {
            this.currentRightTab = RightTab.PARAMS;
            this.rightScroll = 0;
            initGui();
            return;
        }
        if (button.id == BTN_TAB_PREVIEW) {
            this.currentRightTab = RightTab.PREVIEW;
            this.rightScroll = 0;
            initGui();
            return;
        }
        if (button.id == BTN_TAB_EXAMPLES) {
            this.currentRightTab = RightTab.EXAMPLES;
            this.rightScroll = 0;
            initGui();
            return;
        }
        if (button.id == BTN_TAB_DETAILS) {
            this.currentRightTab = RightTab.DETAILS;
            this.rightScroll = 0;
            initGui();
            return;
        }

        if (button.id >= BTN_EXAMPLE_BASE && button.id < BTN_EXAMPLE_BASE + 20) {
            CommandEntry selected = getSelectedCommand();
            if (selected != null) {
                int index = button.id - BTN_EXAMPLE_BASE;
                if (index >= 0 && index < selected.examples.size()) {
                    fillFromExample(selected.examples.get(index));
                }
            }
            return;
        }

        if (button.id >= BTN_ARG_CONTROL_BASE && button.id < BTN_ARG_CONTROL_BASE + 200) {
            int index = button.id - BTN_ARG_CONTROL_BASE;
            if (index >= 0 && index < this.argControls.size()) {
                cycleArgControl(this.argControls.get(index));
                updateRawCommandFromControls();
                validateArgControls(false);
                this.statusMessage = "参数控件已更新";
                this.statusColor = 0xFFE7C35A;
            }
        }
    }

    private void cycleArgControl(ArgControl control) {
        if (control == null) {
            return;
        }
        if (control.schema.valueType == ArgValueType.BOOLEAN) {
            if (control.value == null || control.value.isEmpty()) {
                control.value = "true";
            } else if ("true".equalsIgnoreCase(control.value)) {
                control.value = "false";
            } else if (!control.schema.required) {
                control.value = "";
            } else {
                control.value = "true";
            }
        } else if (control.schema.valueType == ArgValueType.ENUM) {
            int idx = -1;
            for (int i = 0; i < control.schema.enumValues.size(); i++) {
                if (control.schema.enumValues.get(i).equalsIgnoreCase(control.value)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                control.value = control.schema.enumValues.isEmpty() ? "" : control.schema.enumValues.get(0);
            } else {
                int next = idx + 1;
                if (next >= control.schema.enumValues.size()) {
                    control.value = control.schema.required || control.schema.enumValues.isEmpty() ? control.schema.enumValues.get(0) : "";
                } else {
                    control.value = control.schema.enumValues.get(next);
                }
            }
        }
        syncWidgetsFromControlValues();
    }

    private void handleSettingsAction(GuiButton button) {
        if (button.id == BTN_NAV_COMMANDS || button.id == BTN_SETTINGS_BACK) {
            this.currentSection = MainSection.COMMANDS;
            initGui();
            return;
        }
        if (button.id == BTN_NAV_SETTINGS) {
            return;
        }
        if (button.id == BTN_SETTINGS_CLEAR_FILTER) {
            saveSettingsPageEdits();
            this.settingsTypeFilter = "all";
            this.settingsModifiedOnly = false;
            this.settingsSearchText = "";
            this.settingsPage = 0;
            initGui();
            if (this.settingsSearchField != null) {
                this.settingsSearchField.setText("");
            }
            this.settingsStatusMessage = "设置筛选已清空";
            this.settingsStatusColor = 0xFFE7C35A;
            return;
        }
        if (button.id == BTN_SETTINGS_TYPE_FILTER) {
            saveSettingsPageEdits();
            cycleSettingsTypeFilter();
            this.settingsPage = 0;
            initGui();
            return;
        }
        if (button.id == BTN_SETTINGS_MODIFIED_ONLY) {
            saveSettingsPageEdits();
            this.settingsModifiedOnly = !this.settingsModifiedOnly;
            this.settingsPage = 0;
            initGui();
            return;
        }
        if (button.id == BTN_SETTINGS_PREV_PAGE) {
            if (this.settingsPage > 0) {
                saveSettingsPageEdits();
                this.settingsPage--;
                initGui();
            }
            return;
        }
        if (button.id == BTN_SETTINGS_NEXT_PAGE) {
            if (this.settingsPage + 1 < this.settingsTotalPages) {
                saveSettingsPageEdits();
                this.settingsPage++;
                initGui();
            }
            return;
        }
        if (button.id == BTN_SETTINGS_RESET_PAGE) {
            resetCurrentSettingsPage();
            initGui();
            return;
        }
        if (button.id == BTN_SETTINGS_APPLY_PAGE) {
            applyCurrentSettingsPage();
            return;
        }
        if (button.id == BTN_SETTINGS_SAVE) {
            saveSettingsPageEdits();
            applyAllSettingsOnSave();
            this.settingsStatusMessage = "设置已保存";
            this.settingsStatusColor = 0xFF73D98A;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.currentSection == MainSection.SETTINGS) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            handleSettingsMouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        if (mouseButton == 0) {
            if (isInCollapseButton(mouseX, mouseY, this.leftPaneX, this.leftPaneY, this.leftPaneW)) {
                this.leftPaneCollapsed = !this.leftPaneCollapsed;
                initGui();
                return;
            }
            if (isInCollapseButton(mouseX, mouseY, this.middlePaneX, this.middlePaneY, this.middlePaneW)) {
                this.historyPaneCollapsed = !this.historyPaneCollapsed;
                initGui();
                return;
            }
            if (isInCollapseButton(mouseX, mouseY, this.rightPaneX, this.rightPaneY, this.rightPaneW)) {
                this.rightPaneCollapsed = !this.rightPaneCollapsed;
                initGui();
                return;
            }
        }

        if (mouseButton == 1) {
            GuiButton humanLikeButton = findButtonById(BTN_HUMAN_LIKE_MOVEMENT);
            if (humanLikeButton != null && humanLikeButton.visible
                    && mouseX >= humanLikeButton.x && mouseX < humanLikeButton.x + humanLikeButton.width
                    && mouseY >= humanLikeButton.y && mouseY < humanLikeButton.y + humanLikeButton.height) {
                this.mc.setScreen(new GuiHumanLikeMovementSettings(this));
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (handleDropdownClick(mouseX, mouseY)) {
            return;
        }

        if (!this.leftPaneCollapsed && this.searchField != null) {
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (!this.rightPaneCollapsed && this.currentRightTab == RightTab.PREVIEW && this.rawCommandField != null) {
            this.rawCommandField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (ArgControl control : this.argControls) {
            if (!this.rightPaneCollapsed && control.field != null && control.visible) {
                control.field.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        if (!this.leftPaneCollapsed
                && mouseX >= this.commandListX && mouseX < this.commandListX + this.commandListW
                && mouseY >= this.commandListY && mouseY < this.commandListY + this.commandListH) {
            int row = (mouseY - this.commandListY) / COMMAND_ROW_HEIGHT;
            int index = this.commandScroll + row;
            if (index >= 0 && index < this.filteredCommands.size()) {
                this.selectedCommandName = this.filteredCommands.get(index).primaryName;
                this.rightScroll = 0;
                initGui();
                return;
            }
        }

        if (!this.historyPaneCollapsed
                && mouseX >= this.historyListX && mouseX < this.historyListX + this.historyListW
                && mouseY >= this.historyListY && mouseY < this.historyListY + this.historyListH) {
            int row = (mouseY - this.historyListY) / HISTORY_ROW_HEIGHT;
            int index = this.historyScroll + row;
            if (index >= 0 && index < EXECUTION_HISTORY.size()) {
                fillFromRawCommand(EXECUTION_HISTORY.get(index).command, true);
                this.currentRightTab = RightTab.PREVIEW;
                this.rightScroll = 0;
                initGui();
            }
        }
    }

    private boolean handleDropdownClick(int mouseX, int mouseY) {
        GuiButton modeButton = findButtonById(BTN_FILTER_MODE);
        GuiButton categoryButton = findButtonById(BTN_FILTER_CATEGORY);

        if (this.modeDropdownOpen && modeButton != null) {
            if (mouseX >= modeButton.x && mouseX < modeButton.x + modeButton.width
                    && mouseY >= modeButton.y && mouseY < modeButton.y + modeButton.height) {
                return true;
            }
            CommandListMode[] values = CommandListMode.values();
            for (int i = 0; i < values.length; i++) {
                int rowX = modeButton.x;
                int rowY = modeButton.y + modeButton.height + i * 18;
                int rowW = modeButton.width;
                int rowH = 18;
                if (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH) {
                    this.commandListMode = values[i];
                    this.commandScroll = 0;
                    this.modeDropdownOpen = false;
                    initGui();
                    return true;
                }
            }
            this.modeDropdownOpen = false;
        }

        if (this.categoryDropdownOpen && categoryButton != null) {
            if (mouseX >= categoryButton.x && mouseX < categoryButton.x + categoryButton.width
                    && mouseY >= categoryButton.y && mouseY < categoryButton.y + categoryButton.height) {
                return true;
            }
            CommandCategory[] values = CommandCategory.values();
            for (int i = 0; i < values.length; i++) {
                int rowX = categoryButton.x;
                int rowY = categoryButton.y + categoryButton.height + i * 18;
                int rowW = categoryButton.width;
                int rowH = 18;
                if (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH) {
                    this.activeCategory = values[i];
                    this.commandScroll = 0;
                    this.categoryDropdownOpen = false;
                    initGui();
                    return true;
                }
            }
            this.categoryDropdownOpen = false;
        }

        return false;
    }

    private void drawDropdownOverlays(int mouseX, int mouseY) {
        drawModeDropdown(mouseX, mouseY);
        drawCategoryDropdown(mouseX, mouseY);
    }

    private void drawModeDropdown(int mouseX, int mouseY) {
        if (!this.modeDropdownOpen) {
            return;
        }
        GuiButton button = findButtonById(BTN_FILTER_MODE);
        if (button == null) {
            return;
        }

        CommandListMode[] values = CommandListMode.values();
        for (int i = 0; i < values.length; i++) {
            int rowX = button.x;
            int rowY = button.y + button.height + i * 18;
            int rowW = button.width;
            int rowH = 18;
            boolean hover = mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH;
            boolean selected = this.commandListMode == values[i];
            GuiTheme.drawButtonFrame(rowX, rowY, rowW, rowH,
                    selected ? GuiTheme.UiState.SELECTED : (hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            if (selected) {
                Gui.drawRect(rowX + 1, rowY + 1, rowX + 4, rowY + rowH - 1, 0xFF7ED0FF);
            }
            drawString(this.fontRenderer, (selected ? "§b✔ " : "§7") + values[i].label, rowX + 6, rowY + 5, 0xFFFFFFFF);
        }
    }

    private void drawCategoryDropdown(int mouseX, int mouseY) {
        if (!this.categoryDropdownOpen) {
            return;
        }
        GuiButton button = findButtonById(BTN_FILTER_CATEGORY);
        if (button == null) {
            return;
        }

        CommandCategory[] values = CommandCategory.values();
        for (int i = 0; i < values.length; i++) {
            int rowX = button.x;
            int rowY = button.y + button.height + i * 18;
            int rowW = button.width;
            int rowH = 18;
            boolean hover = mouseX >= rowX && mouseX < rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH;
            boolean selected = this.activeCategory == values[i];
            GuiTheme.drawButtonFrame(rowX, rowY, rowW, rowH,
                    selected ? GuiTheme.UiState.SELECTED : (hover ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            if (selected) {
                Gui.drawRect(rowX + 1, rowY + 1, rowX + 4, rowY + rowH - 1, 0xFF7ED0FF);
            }
            drawString(this.fontRenderer, (selected ? "§b✔ " : "§7") + values[i].label, rowX + 6, rowY + 5, 0xFFFFFFFF);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.currentSection == MainSection.SETTINGS) {
            handleSettingsKeyTyped(typedChar, keyCode);
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (this.modeDropdownOpen || this.categoryDropdownOpen) {
                this.modeDropdownOpen = false;
                this.categoryDropdownOpen = false;
                return;
            }
            this.mc.setScreen(this.parentScreen);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            executeCurrentCommand();
            return;
        }

        if (this.searchField != null && this.searchField.isFocused()) {
            this.searchField.textboxKeyTyped(typedChar, keyCode);
            this.searchText = this.searchField.getText();
            initGui();
            this.searchField.setFocused(true);
            this.searchField.setCursorPositionEnd();
            return;
        }

        if (this.currentRightTab == RightTab.PREVIEW && this.rawCommandField != null && this.rawCommandField.isFocused()) {
            this.rawCommandField.textboxKeyTyped(typedChar, keyCode);
            this.statusMessage = "命令预览已修改";
            this.statusColor = 0xFFE7C35A;
            return;
        }

        for (ArgControl control : this.argControls) {
            if (control.field != null && control.field.isFocused()) {
                control.field.textboxKeyTyped(typedChar, keyCode);
                control.value = control.field.getText();
                updateRawCommandFromControls();
                boolean hasError = validateArgControls(false);
                this.statusMessage = hasError ? "参数已更新（存在待修正项）" : "参数已更新";
                this.statusColor = hasError ? 0xFFE7C35A : 0xFF73D98A;
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void handleSettingsKeyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.currentSection = MainSection.COMMANDS;
            initGui();
            return;
        }

        if (this.settingsSearchField != null && this.settingsSearchField.isFocused()) {
            saveSettingsPageEdits();
            this.settingsSearchField.textboxKeyTyped(typedChar, keyCode);
            applySettingsFilter(this.settingsSearchField.getText());
            recalcSettingsPaging();
            this.settingsPage = 0;
            buildSettingsPageControls();
            this.settingsSearchText = this.settingsSearchField.getText();
            return;
        }

        for (GuiTextField tf : this.settingValueFields) {
            if (tf.isFocused()) {
                tf.textboxKeyTyped(typedChar, keyCode);
                this.settingsStatusMessage = "设置值已修改";
                this.settingsStatusColor = 0xFFE7C35A;
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

        if (this.currentSection == MainSection.SETTINGS) {
            if (wheel < 0 && this.settingsPage + 1 < this.settingsTotalPages) {
                saveSettingsPageEdits();
                this.settingsPage++;
                initGui();
            } else if (wheel > 0 && this.settingsPage > 0) {
                saveSettingsPageEdits();
                this.settingsPage--;
                initGui();
            }
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.getWindow().getWidth();
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.getWindow().getHeight() - 1;

        if (handleDropdownWheel(mouseX, mouseY, wheel)) {
            return;
        }

        if (!this.leftPaneCollapsed
                && mouseX >= this.commandListX && mouseX < this.commandListX + this.commandListW
                && mouseY >= this.commandListY && mouseY < this.commandListY + this.commandListH) {
            int maxScroll = Math.max(0, this.filteredCommands.size() - this.commandVisibleRows);
            if (wheel < 0) {
                this.commandScroll = Math.min(maxScroll, this.commandScroll + 1);
            } else {
                this.commandScroll = Math.max(0, this.commandScroll - 1);
            }
            return;
        }

        if (!this.historyPaneCollapsed
                && mouseX >= this.historyListX && mouseX < this.historyListX + this.historyListW
                && mouseY >= this.historyListY && mouseY < this.historyListY + this.historyListH) {
            int maxScroll = Math.max(0, EXECUTION_HISTORY.size() - this.historyVisibleRows);
            if (wheel < 0) {
                this.historyScroll = Math.min(maxScroll, this.historyScroll + 1);
            } else {
                this.historyScroll = Math.max(0, this.historyScroll - 1);
            }
            return;
        }

        if (!this.rightPaneCollapsed
                && mouseX >= this.rightPaneX && mouseX < this.rightPaneX + this.rightPaneW
                && mouseY >= getRightViewportY() && mouseY < getRightViewportY() + getRightViewportH()) {
            int maxScroll = Math.max(0, this.rightContentHeight - getRightViewportH());
            if (wheel < 0) {
                this.rightScroll = Math.min(maxScroll, this.rightScroll + 18);
            } else {
                this.rightScroll = Math.max(0, this.rightScroll - 18);
            }
        }
    }

    private boolean handleDropdownWheel(int mouseX, int mouseY, int wheel) {
        GuiButton modeButton = findButtonById(BTN_FILTER_MODE);
        GuiButton categoryButton = findButtonById(BTN_FILTER_CATEGORY);

        if (this.modeDropdownOpen && modeButton != null) {
            if (mouseX >= modeButton.x && mouseX < modeButton.x + modeButton.width
                    && mouseY >= modeButton.y && mouseY < modeButton.y + modeButton.height + CommandListMode.values().length * 18) {
                int currentIndex = this.commandListMode.ordinal();
                int next = wheel < 0 ? Math.min(CommandListMode.values().length - 1, currentIndex + 1)
                        : Math.max(0, currentIndex - 1);
                this.commandListMode = CommandListMode.values()[next];
                this.commandScroll = 0;
                initGui();
                this.modeDropdownOpen = true;
                return true;
            }
        }

        if (this.categoryDropdownOpen && categoryButton != null) {
            if (mouseX >= categoryButton.x && mouseX < categoryButton.x + categoryButton.width
                    && mouseY >= categoryButton.y && mouseY < categoryButton.y + categoryButton.height + CommandCategory.values().length * 18) {
                int currentIndex = this.activeCategory.ordinal();
                int next = wheel < 0 ? Math.min(CommandCategory.values().length - 1, currentIndex + 1)
                        : Math.max(0, currentIndex - 1);
                this.activeCategory = CommandCategory.values()[next];
                this.commandScroll = 0;
                initGui();
                this.categoryDropdownOpen = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        savePersistentState();
        super.onGuiClosed();
    }

    private void drawSettingsView(int mouseX, int mouseY, float partialTicks) {
        drawCenteredString(this.fontRenderer, "Baritone设置中心", this.panelX + this.panelW / 2, this.panelY + 10, 0xFFFFFFFF);

        if (this.settingsSearchField != null) {
            drawThemedTextField(this.settingsSearchField);
            if ((this.settingsSearchField.getText() == null || this.settingsSearchField.getText().isEmpty())
                    && !this.settingsSearchField.isFocused()) {
                drawString(this.fontRenderer, "搜索设置（键名 / 描述）", this.settingsSearchField.x + 4, this.settingsSearchField.y + 5, 0xFF8A8A8A);
            }
        }

        int modifiedCount = 0;
        for (SettingDef def : this.settingAllDefs) {
            String value = this.settingEditingValues.getOrDefault(def.key, def.defaultValue);
            if (!isSettingDefaultValue(def, value)) {
                modifiedCount++;
            }
        }

        drawString(this.fontRenderer,
                String.format(Locale.ROOT, "当前页: %d/%d   本页容量: %d   结果: %d   已修改: %d",
                        this.settingsPage + 1, this.settingsTotalPages, this.settingsItemsPerPage, this.settingFilteredDefs.size(), modifiedCount),
                this.panelX + 10, this.panelY + 34, 0xFFCFCFCF);

        int start = this.settingsPage * this.settingsItemsPerPage;
        int end = Math.min(this.settingFilteredDefs.size(), start + this.settingsItemsPerPage);
        int cardW = getSettingsCardWidth();

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % this.settingsColumns;
            int row = local / this.settingsColumns;
            int cardX = this.settingsContentX + col * (cardW + this.settingsCardGapX);
            int cardY = this.settingsContentY + row * (this.settingsCardHeight + this.settingsCardGapY);

            SettingDef def = this.settingFilteredDefs.get(i);

            GuiTheme.drawPanelSegment(cardX, cardY, cardW, this.settingsCardHeight, cardX, cardY, cardW, this.settingsCardHeight);

            String descText = trimToWidth(def.desc, cardW - 12);
            String keyText = trimToWidth(def.key, cardW - 12);
            String metaText = trimToWidth("类型: " + def.type + "  默认: " + getResolvedSettingDefaultValue(def), cardW - 12);

            drawString(this.fontRenderer, descText, cardX + 6, cardY + 4, 0xFFFFFFFF);
            drawString(this.fontRenderer, keyText, cardX + 6, cardY + 16, 0xFF98B9FF);
            drawString(this.fontRenderer, metaText, cardX + 6, cardY + 28, 0xFFAAAAAA);

            String msg = this.settingStatusMessages.get(def.key);
            if (msg != null && !msg.isEmpty()) {
                int color = "应用失败".equals(msg) ? 0xFFFF6E6E : 0xFF66DD66;
                drawString(this.fontRenderer, trimToWidth(msg, 42), cardX + cardW - 42, cardY + 4, color);
            }

            if (mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + this.settingsCardHeight) {
                this.hoverTooltip = "§e设置项: " + def.key
                        + "\n§7描述: " + def.desc
                        + "\n§7类型: " + def.type
                        + "\n§7默认: " + getResolvedSettingDefaultValue(def);
                this.hoverTooltipX = mouseX;
                this.hoverTooltipY = mouseY;
            }
        }

        for (GuiButton b : this.settingBoolButtons) {
            b.drawButton(this.mc, mouseX, mouseY, partialTicks);
        }
        for (GuiButton b : this.settingVisualEditorButtons) {
            b.drawButton(this.mc, mouseX, mouseY, partialTicks);
        }
        for (GuiTextField tf : this.settingValueFields) {
            drawThemedTextField(tf);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawString(this.fontRenderer, this.settingsStatusMessage == null ? "" : this.settingsStatusMessage,
                this.panelX + 10, this.panelY + this.panelH - 14, this.settingsStatusColor);
    }

    private void buildSettingsDefsIfNeeded() {
        if (!this.settingAllDefs.isEmpty()) {
            return;
        }
        if (loadBuiltInSettingDefinitions()) {
            return;
        }
        buildFallbackSettingDefinitionsFromRuntime();
    }

    private boolean loadBuiltInSettingDefinitions() {
        try (InputStream input = GuiBaritoneCommandTable.class.getClassLoader().getResourceAsStream(BUILTIN_SETTINGS_RESOURCE)) {
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
                    this.settingAllDefs.add(new SettingDef(def.key.trim(), defaultValue, type, desc));
                }
                return !this.settingAllDefs.isEmpty();
            }
        } catch (IOException | JsonSyntaxException ignored) {
            return false;
        }
    }

    private void buildFallbackSettingDefinitionsFromRuntime() {
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
            this.settingAllDefs.add(new SettingDef(key, defaultValue, type, key));
        }
    }

    private void initSettingsView() {
        buildSettingsDefsIfNeeded();
        if (this.settingEditingValues.isEmpty()) {
            for (SettingDef def : this.settingAllDefs) {
                this.settingEditingValues.put(def.key, getCurrentSettingValue(def));
            }
        }

        updateSettingsResponsiveLayout();
        this.settingsContentX = this.panelX + 10;
        this.settingsContentY = this.panelY + 60;
        this.settingsContentW = this.panelW - 20;

        int searchW = Math.max(180, Math.min(360, this.panelW / 3));
        int searchX = this.panelX + this.panelW - searchW - 10;
        int searchY = this.panelY + 10;
        this.settingsSearchField = new GuiTextField(9900, this.fontRenderer, searchX, searchY, searchW, 18);
        this.settingsSearchField.setMaxStringLength(128);
        this.settingsSearchField.setText(this.settingsSearchText == null ? "" : this.settingsSearchText);

        int topY = this.panelY + 26;
        this.buttonList.add(new ThemedButton(BTN_NAV_COMMANDS, this.panelX + 12, topY, 86, 18, buildMainSectionLabel(MainSection.COMMANDS)));
        this.buttonList.add(new ThemedButton(BTN_NAV_SETTINGS, this.panelX + 104, topY, 86, 18, buildMainSectionLabel(MainSection.SETTINGS)));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_TYPE_FILTER, this.panelX + 196, topY, 112, 18, buildSettingsTypeFilterLabel()));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_MODIFIED_ONLY, this.panelX + 314, topY, 102, 18, buildSettingsModifiedFilterLabel()));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_CLEAR_FILTER, this.panelX + 422, topY, 78, 18, "§f清空"));

        applySettingsFilter(this.settingsSearchField.getText());
        recalcSettingsPaging();
        this.settingsPage = MathHelper.clamp(this.settingsPage, 0, this.settingsTotalPages - 1);

        int bottomY = this.panelY + this.panelH - 24;
        int navW = 54;
        int actionW = Math.max(74, Math.min(110, (this.panelW - 220) / 4));
        int leftX = this.panelX + 10;
        int rightX = this.panelX + this.panelW - 10;

        this.buttonList.add(new ThemedButton(BTN_SETTINGS_PREV_PAGE, leftX, bottomY, navW, 20, "上一页"));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_NEXT_PAGE, leftX + navW + 6, bottomY, navW, 20, "下一页"));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_SAVE, rightX - actionW, bottomY, actionW, 20, "§a保存设置"));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_RESET_PAGE, rightX - actionW * 2 - 6, bottomY, actionW, 20, "§e恢复本页"));
        this.buttonList.add(new ThemedButton(BTN_SETTINGS_APPLY_PAGE, rightX - actionW * 3 - 12, bottomY, actionW, 20, "§b应用本页"));

        buildSettingsPageControls();
    }

    private void buildSettingsPageControls() {
        this.settingValueFields.clear();
        this.settingBoolButtons.clear();
        this.settingVisualEditorButtons.clear();
        this.settingButtonToKey.clear();
        this.settingVisualEditorButtonDefs.clear();

        int start = this.settingsPage * this.settingsItemsPerPage;
        int end = Math.min(this.settingFilteredDefs.size(), start + this.settingsItemsPerPage);
        int cardW = getSettingsCardWidth();
        int idBase = 10000;

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % this.settingsColumns;
            int row = local / this.settingsColumns;
            int cardX = this.settingsContentX + col * (cardW + this.settingsCardGapX);
            int cardY = this.settingsContentY + row * (this.settingsCardHeight + this.settingsCardGapY);

            SettingDef def = this.settingFilteredDefs.get(i);
            String value = this.settingEditingValues.getOrDefault(def.key, def.defaultValue);

            int controlX = cardX + 6;
            int controlY = cardY + this.settingsCardHeight - 22;
            int controlW = cardW - 12;
            SettingBlockEditorMode editorMode = getSettingBlockEditorMode(def);

            if ("boolean".equals(def.type)) {
                boolean on = parseSettingBoolean(value, def.defaultValue);
                ThemedButton btn = new ThemedButton(idBase + local, controlX, controlY, controlW, 16, buildSettingBoolLabel(on));
                this.settingBoolButtons.add(btn);
                this.settingButtonToKey.put(btn, def.key);
            } else if (editorMode != SettingBlockEditorMode.NONE) {
                ThemedButton btn = new ThemedButton(idBase + local, controlX, controlY, controlW, 16,
                        buildSettingVisualEditorLabel(def, value, editorMode));
                this.settingVisualEditorButtons.add(btn);
                this.settingVisualEditorButtonDefs.put(btn, def);
            } else {
                GuiTextField field = new GuiTextField(idBase + local, this.fontRenderer, controlX, controlY, controlW, 16);
                field.setMaxStringLength(Integer.MAX_VALUE);
                field.setText(value);
                this.settingValueFields.add(field);
            }
        }
    }

    private void recalcSettingsPaging() {
        int bottomY = this.panelY + this.panelH - 24;
        int contentBottom = bottomY - 10;
        int availableH = Math.max(80, contentBottom - this.settingsContentY);
        this.settingsRowsPerPage = Math.max(1, (availableH + this.settingsCardGapY) / (this.settingsCardHeight + this.settingsCardGapY));
        this.settingsItemsPerPage = Math.max(this.settingsColumns, this.settingsRowsPerPage * this.settingsColumns);
        this.settingsTotalPages = Math.max(1, (this.settingFilteredDefs.size() + this.settingsItemsPerPage - 1) / this.settingsItemsPerPage);
    }

    private void applySettingsFilter(String query) {
        this.settingsSearchText = query == null ? "" : query.trim();
        String q = PinyinSearchHelper.normalizeQuery(this.settingsSearchText);
        this.settingFilteredDefs.clear();

        for (SettingDef def : this.settingAllDefs) {
            String searchContent = (def.key == null ? "" : def.key) + " " + (def.desc == null ? "" : def.desc);
            boolean textMatch = q.isEmpty() || PinyinSearchHelper.matchesNormalized(searchContent, q);
            boolean typeMatch = matchesSettingsTypeFilter(def);
            boolean modifiedMatch = !this.settingsModifiedOnly || isSettingsModifiedValue(def);
            if (textMatch && typeMatch && modifiedMatch) {
                this.settingFilteredDefs.add(def);
            }
        }
    }

    private boolean matchesSettingsTypeFilter(SettingDef def) {
        if ("all".equals(this.settingsTypeFilter)) {
            return true;
        }
        String t = def.type == null ? "" : def.type.toLowerCase(Locale.ROOT);
        if (t.equals(this.settingsTypeFilter)) {
            return true;
        }
        return t.startsWith(this.settingsTypeFilter + "<");
    }

    private boolean isSettingsModifiedValue(SettingDef def) {
        String value = this.settingEditingValues.getOrDefault(def.key, def.defaultValue);
        return !isSettingDefaultValue(def, value);
    }

    private boolean parseSettingBoolean(String value, String defaultValue) {
        String v = value == null ? defaultValue : value;
        return "true".equalsIgnoreCase(v.trim());
    }

    private String buildSettingBoolLabel(boolean on) {
        return on ? "§a开启" : "§c关闭";
    }

    private String buildSettingsTypeFilterLabel() {
        String label = "all".equals(this.settingsTypeFilter) ? "全部" : this.settingsTypeFilter;
        return "§b类型: " + label;
    }

    private String buildSettingsModifiedFilterLabel() {
        return this.settingsModifiedOnly ? "§a仅已修改" : "§7全部";
    }

    private void saveSettingsPageEdits() {
        int start = this.settingsPage * this.settingsItemsPerPage;
        int end = Math.min(this.settingFilteredDefs.size(), start + this.settingsItemsPerPage);
        int textIdx = 0;
        for (int i = start; i < end; i++) {
            SettingDef def = this.settingFilteredDefs.get(i);
            if ("boolean".equals(def.type) || getSettingBlockEditorMode(def) != SettingBlockEditorMode.NONE) {
                continue;
            }
            if (textIdx < this.settingValueFields.size()) {
                this.settingEditingValues.put(def.key, this.settingValueFields.get(textIdx).getText().trim());
                textIdx++;
            }
        }
    }

    private void resetCurrentSettingsPage() {
        int start = this.settingsPage * this.settingsItemsPerPage;
        int end = Math.min(this.settingFilteredDefs.size(), start + this.settingsItemsPerPage);
        for (int i = start; i < end; i++) {
            SettingDef def = this.settingFilteredDefs.get(i);
            this.settingEditingValues.put(def.key, getResolvedSettingDefaultValue(def));
            this.settingStatusMessages.put(def.key, "已重置");
        }
        saveSettingsPageEdits();
        this.settingsStatusMessage = "当前页设置已恢复默认";
        this.settingsStatusColor = 0xFFE7C35A;
    }

    private void applyCurrentSettingsPage() {
        saveSettingsPageEdits();
        int start = this.settingsPage * this.settingsItemsPerPage;
        int end = Math.min(this.settingFilteredDefs.size(), start + this.settingsItemsPerPage);
        for (int i = start; i < end; i++) {
            SettingDef def = this.settingFilteredDefs.get(i);
            String value = this.settingEditingValues.getOrDefault(def.key, def.defaultValue);
            try {
                applySettingDirectly(def, value);
                this.settingEditingValues.put(def.key, getCurrentSettingValue(def));
                this.settingStatusMessages.put(def.key, "已应用");
            } catch (Exception e) {
                this.settingStatusMessages.put(def.key, "应用失败");
            }
        }
        SettingsUtil.save(BaritoneAPI.getSettings());
        this.settingsStatusMessage = "当前页设置已应用";
        this.settingsStatusColor = 0xFF73D98A;
    }

    private boolean isSettingDefaultValue(SettingDef def, String value) {
        String v = value == null ? "" : value.trim();
        String d = getResolvedSettingDefaultValue(def).trim();
        if ("boolean".equals(def.type)) {
            return parseSettingBoolean(v, d) == parseSettingBoolean(d, d);
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

    private void applyAllSettingsOnSave() {
        for (SettingDef def : this.settingAllDefs) {
            String value = this.settingEditingValues.getOrDefault(def.key, def.defaultValue);
            try {
                applySettingDirectly(def, value);
                this.settingEditingValues.put(def.key, getCurrentSettingValue(def));
                this.settingStatusMessages.put(def.key, "已应用");
            } catch (Exception e) {
                this.settingStatusMessages.put(def.key, "应用失败");
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

    private String getResolvedSettingDefaultValue(SettingDef def) {
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

    private Settings.Setting<?> getBaritoneSetting(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return BaritoneAPI.getSettings().byLowerName.get(key.toLowerCase(Locale.ROOT));
    }

    private SettingBlockEditorMode getSettingBlockEditorMode(SettingDef def) {
        return def == null ? SettingBlockEditorMode.NONE : getSettingBlockEditorMode(getBaritoneSetting(def.key));
    }

    private SettingBlockEditorMode getSettingBlockEditorMode(Settings.Setting<?> setting) {
        if (setting == null) {
            return SettingBlockEditorMode.NONE;
        }
        Type type = setting.getType();
        if (isListOfBlocksType(type) || isListOfPlaceableItemsType(setting)) {
            return SettingBlockEditorMode.BLOCK_LIST;
        }
        if (isMapOfBlockListsType(type)) {
            return SettingBlockEditorMode.BLOCK_MAP;
        }
        return SettingBlockEditorMode.NONE;
    }

    private boolean isListOfBlocksType(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!(parameterizedType.getRawType() instanceof Class)
                || !List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
            return false;
        }
        Type[] args = parameterizedType.getActualTypeArguments();
        return args.length == 1 && args[0] instanceof Class && Block.class.isAssignableFrom((Class<?>) args[0]);
    }

    private boolean isMapOfBlockListsType(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!(parameterizedType.getRawType() instanceof Class)
                || !Map.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
            return false;
        }
        Type[] args = parameterizedType.getActualTypeArguments();
        return args.length == 2
                && args[0] instanceof Class
                && Block.class.isAssignableFrom((Class<?>) args[0])
                && isListOfBlocksType(args[1]);
    }

    private boolean isListOfPlaceableItemsType(Settings.Setting<?> setting) {
        if (setting == null || !"acceptableThrowawayItems".equalsIgnoreCase(setting.getName())) {
            return false;
        }
        Type type = setting.getType();
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!(parameterizedType.getRawType() instanceof Class)
                || !List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
            return false;
        }
        Type[] args = parameterizedType.getActualTypeArguments();
        return args.length == 1 && args[0] instanceof Class && Item.class.isAssignableFrom((Class<?>) args[0]);
    }

    private String buildSettingVisualEditorLabel(SettingDef def, String value, SettingBlockEditorMode editorMode) {
        if (def != null && "acceptableThrowawayItems".equalsIgnoreCase(def.key)) {
            return "编辑垫脚块列表 (" + BaritoneBlockSettingEditorSupport.parseBlockListValue(value).size() + ")";
        }
        if (def != null && "blockingBlocks".equalsIgnoreCase(def.key)) {
            return "编辑阻挡方块列表 (" + BaritoneBlockSettingEditorSupport.parseBlockListValue(value).size() + ")";
        }
        if (def != null && "interactionBlocks".equalsIgnoreCase(def.key)) {
            return "编辑交互方块列表 (" + BaritoneBlockSettingEditorSupport.parseBlockListValue(value).size() + ")";
        }
        if (def != null && "dangerousBlocks".equalsIgnoreCase(def.key)) {
            return "编辑危险方块列表 (" + BaritoneBlockSettingEditorSupport.parseBlockListValue(value).size() + ")";
        }
        if (editorMode == SettingBlockEditorMode.BLOCK_MAP) {
            return "编辑方块映射 (" + BaritoneBlockSettingEditorSupport.parseBlockMapValue(value).size() + "组)";
        }
        return "编辑方块列表 (" + BaritoneBlockSettingEditorSupport.parseBlockListValue(value).size() + ")";
    }

    private void openSettingVisualEditor(final SettingDef def) {
        SettingBlockEditorMode editorMode = getSettingBlockEditorMode(def);
        String currentValue = this.settingEditingValues.getOrDefault(def.key, def.defaultValue);
        String defaultValue = getResolvedSettingDefaultValue(def);

        if (editorMode == SettingBlockEditorMode.BLOCK_LIST) {
            this.mc.setScreen(new GuiBaritoneBlockListEditor(this, def.key, def.desc,
                    BaritoneBlockSettingEditorSupport.parseBlockListValue(currentValue),
                    BaritoneBlockSettingEditorSupport.parseBlockListValue(defaultValue),
                    new Consumer<String>() {
                        @Override
                        public void accept(String value) {
                            applyVisualEditorValue(def, value);
                        }
                    }));
            return;
        }

        if (editorMode == SettingBlockEditorMode.BLOCK_MAP) {
            this.mc.setScreen(new GuiBaritoneBlockMapEditor(this, def.key, def.desc,
                    BaritoneBlockSettingEditorSupport.parseBlockMapValue(currentValue),
                    BaritoneBlockSettingEditorSupport.parseBlockMapValue(defaultValue),
                    new Consumer<String>() {
                        @Override
                        public void accept(String value) {
                            applyVisualEditorValue(def, value);
                        }
                    }));
        }
    }

    private void applyVisualEditorValue(SettingDef def, String value) {
        String normalized = normalizeSettingValue(def, value);
        this.settingEditingValues.put(def.key, normalized);
        this.settingStatusMessages.put(def.key, isSettingDefaultValue(def, normalized) ? "已恢复默认" : "待应用");
        this.settingsStatusMessage = def.key + " 已修改";
        this.settingsStatusColor = 0xFFE7C35A;
    }

    private void cycleSettingsTypeFilter() {
        String[] filters = {"all", "boolean", "int", "long", "float", "double", "string", "list", "map", "color", "vec3i"};
        int idx = 0;
        for (int i = 0; i < filters.length; i++) {
            if (filters[i].equals(this.settingsTypeFilter)) {
                idx = i;
                break;
            }
        }
        this.settingsTypeFilter = filters[(idx + 1) % filters.length];
    }

    private void updateSettingsResponsiveLayout() {
        if (this.panelW >= 1100) {
            this.settingsColumns = 4;
            this.settingsCardHeight = 56;
        } else if (this.panelW >= 820) {
            this.settingsColumns = 3;
            this.settingsCardHeight = 58;
        } else if (this.panelW >= 560) {
            this.settingsColumns = 2;
            this.settingsCardHeight = 60;
        } else {
            this.settingsColumns = 1;
            this.settingsCardHeight = 64;
        }
    }

    private int getSettingsCardWidth() {
        return (this.settingsContentW - (this.settingsColumns - 1) * this.settingsCardGapX) / this.settingsColumns;
    }

    private void handleSettingsMouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.settingsSearchField != null) {
            this.settingsSearchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        for (GuiTextField tf : this.settingValueFields) {
            tf.mouseClicked(mouseX, mouseY, mouseButton);
        }

        for (GuiButton b : this.settingBoolButtons) {
            if (b.mousePressed(this.mc, mouseX, mouseY)) {
                String key = this.settingButtonToKey.get(b);
                if (key != null) {
                    boolean current = parseSettingBoolean(this.settingEditingValues.get(key), "false");
                    boolean next = !current;
                    this.settingEditingValues.put(key, String.valueOf(next));
                    b.displayString = buildSettingBoolLabel(next);
                    this.settingStatusMessages.put(key, "待应用");
                    this.settingsStatusMessage = "布尔设置已修改";
                    this.settingsStatusColor = 0xFFE7C35A;
                }
                return;
            }
        }

        for (GuiButton b : this.settingVisualEditorButtons) {
            if (b.mousePressed(this.mc, mouseX, mouseY)) {
                SettingDef def = this.settingVisualEditorButtonDefs.get(b);
                if (def != null) {
                    openSettingVisualEditor(def);
                }
                return;
            }
        }
    }

    private static File getStateFile() {
        try {
            Path profileDir = ProfileManager.getCurrentProfileDir();
            File profileFile = profileDir.toFile();
            if (!profileFile.exists()) {
                profileFile.mkdirs();
            }
            return new File(profileFile, STATE_FILE_NAME);
        } catch (Exception ignored) {
            return getLegacyStateFile();
        }
    }

    private static File getLegacyStateFile() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        File modDir = new File(configDir, "zszl_script");
        return new File(modDir, STATE_FILE_NAME);
    }

    public static void cleanupLegacyStateDirectory() {
        cleanupLegacyStateArtifacts(false);
    }

    private static void cleanupLegacyStateArtifacts(boolean deleteLegacyFile) {
        try {
            File legacyFile = getLegacyStateFile();
            if (deleteLegacyFile && legacyFile.isFile()) {
                legacyFile.delete();
            }

            File legacyDir = legacyFile.getParentFile();
            if (legacyDir == null || !legacyDir.isDirectory()) {
                return;
            }

            String[] children = legacyDir.list();
            if (children != null && children.length == 0) {
                legacyDir.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadPersistentStateIfNeeded() {
        if (PERSISTENT_STATE_LOADED) {
            return;
        }
        PERSISTENT_STATE_LOADED = true;
        cleanupLegacyStateArtifacts(false);

        FAVORITE_COMMANDS.clear();
        RECENT_COMMANDS.clear();
        EXECUTION_HISTORY.clear();

        File file = getStateFile();
        File legacyFile = getLegacyStateFile();
        File loadFile = file.isFile() ? file : legacyFile;
        if (!loadFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(loadFile))) {
            PersistentState state = GSON.fromJson(reader, PersistentState.class);
            if (state == null) {
                return;
            }
            if (state.favorites != null) {
                FAVORITE_COMMANDS.addAll(state.favorites);
            }
            if (state.recent != null) {
                RECENT_COMMANDS.addAll(state.recent);
            }
            if (state.history != null) {
                for (HistoryEntry historyEntry : state.history) {
                    if (historyEntry != null && historyEntry.command != null) {
                        EXECUTION_HISTORY.add(historyEntry);
                    }
                }
            }
            if (!file.isFile() && loadFile.equals(legacyFile)) {
                savePersistentState();
                cleanupLegacyStateArtifacts(true);
            }
        } catch (Exception ignored) {
        }
    }

    private static void savePersistentState() {
        try {
            File file = getStateFile();
            PersistentState state = new PersistentState();
            state.favorites = new ArrayList<String>(FAVORITE_COMMANDS);
            state.recent = new ArrayList<String>(RECENT_COMMANDS);
            state.history = new ArrayList<HistoryEntry>(EXECUTION_HISTORY);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                GSON.toJson(state, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private enum MainSection {
        COMMANDS("命令"),
        SETTINGS("设置");

        private final String label;

        MainSection(String label) {
            this.label = label;
        }
    }

    private enum CommandListMode {
        ALL("全部"),
        FAVORITES("收藏"),
        RECENT("最近");

        private final String label;

        CommandListMode(String label) {
            this.label = label;
        }
    }

    private enum CommandCategory {
        ALL("全部"),
        NAVIGATION("导航"),
        WORLD("世界"),
        CONTROL("控制"),
        INFO("信息"),
        OTHER("其他");

        private final String label;

        CommandCategory(String label) {
            this.label = label;
        }
    }

    private enum RightTab {
        PARAMS("参数"),
        PREVIEW("预览"),
        EXAMPLES("示例"),
        DETAILS("说明");

        private final String label;

        RightTab(String label) {
            this.label = label;
        }
    }

    private enum SettingBlockEditorMode {
        NONE,
        BLOCK_LIST,
        BLOCK_MAP
    }

    private enum ArgValueType {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        ENUM
    }

    private static final class ArgSchema {
        private final String key;
        private final String label;
        private final String placeholder;
        private final boolean required;
        private final ArgValueType valueType;
        private final Double minValue;
        private final Double maxValue;
        private final List<String> enumValues;
        private final String description;
        private final boolean coordinate;
        private final boolean preferredToggle;

        private ArgSchema(String key, String label, String placeholder, boolean required, ArgValueType valueType,
                Double minValue, Double maxValue, List<String> enumValues, String description,
                boolean coordinate, boolean preferredToggle) {
            this.key = key;
            this.label = label;
            this.placeholder = placeholder;
            this.required = required;
            this.valueType = valueType;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.enumValues = enumValues == null ? Collections.<String>emptyList() : enumValues;
            this.description = description == null ? "" : description;
            this.coordinate = coordinate;
            this.preferredToggle = preferredToggle;
        }

        private static ArgSchema text(String key, String label, String placeholder, boolean required, String description) {
            return new ArgSchema(key, label, placeholder, required, ArgValueType.STRING, null, null,
                    Collections.<String>emptyList(), description, false, false);
        }

        private static ArgSchema integer(String key, String label, String placeholder, boolean required,
                Integer min, Integer max, String description, boolean coordinate, boolean preferredToggle) {
            return new ArgSchema(key, label, placeholder, required, ArgValueType.INTEGER,
                    min == null ? null : min.doubleValue(),
                    max == null ? null : max.doubleValue(),
                    Collections.<String>emptyList(), description, coordinate, preferredToggle);
        }

        private static ArgSchema decimal(String key, String label, String placeholder, boolean required,
                Double min, Double max, String description, boolean coordinate, boolean preferredToggle) {
            return new ArgSchema(key, label, placeholder, required, ArgValueType.DECIMAL,
                    min, max, Collections.<String>emptyList(), description, coordinate, preferredToggle);
        }

        private static ArgSchema choice(String key, String label, boolean required, List<String> values, String description) {
            return new ArgSchema(key, label, "点击选择", required, ArgValueType.ENUM,
                    null, null, values, description, false, true);
        }

        private static ArgSchema bool(String key, String label, boolean required, String description) {
            return new ArgSchema(key, label, "点击切换", required, ArgValueType.BOOLEAN,
                    null, null, Arrays.asList("true", "false"), description, false, true);
        }

        private String validate(String value) {
            String text = value == null ? "" : value.trim();
            if (text.isEmpty()) {
                return this.required ? "该参数不能为空" : "";
            }

            try {
                switch (this.valueType) {
                    case INTEGER:
                        int intValue = Integer.parseInt(text);
                        if (this.minValue != null && intValue < this.minValue.intValue()) {
                            return "不能小于 " + this.minValue.intValue();
                        }
                        if (this.maxValue != null && intValue > this.maxValue.intValue()) {
                            return "不能大于 " + this.maxValue.intValue();
                        }
                        return "";
                    case DECIMAL:
                        double decimalValue = Double.parseDouble(text);
                        if (this.minValue != null && decimalValue < this.minValue) {
                            return "不能小于 " + this.minValue;
                        }
                        if (this.maxValue != null && decimalValue > this.maxValue) {
                            return "不能大于 " + this.maxValue;
                        }
                        return "";
                    case BOOLEAN:
                        if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                            return "只能填写 true / false";
                        }
                        return "";
                    case ENUM:
                        for (String enumValue : this.enumValues) {
                            if (enumValue.equalsIgnoreCase(text)) {
                                return "";
                            }
                        }
                        return "可选值: " + String.join("/", this.enumValues);
                    default:
                        return "";
                }
            } catch (NumberFormatException ignored) {
                return this.valueType == ArgValueType.INTEGER ? "需要填写整数" : "需要填写数字";
            }
        }

        private String buildCompactTip() {
            if (this.valueType == ArgValueType.INTEGER) {
                return "整数，提示：" + this.placeholder;
            }
            if (this.valueType == ArgValueType.DECIMAL) {
                return "数字，提示：" + this.placeholder;
            }
            if (this.valueType == ArgValueType.ENUM) {
                return "可选 " + String.join("/", this.enumValues);
            }
            if (this.valueType == ArgValueType.BOOLEAN) {
                return "布尔切换（true/false）";
            }
            return this.description.isEmpty() ? this.placeholder : this.description;
        }
    }

    private static final class ArgControl {
        private final ArgSchema schema;
        private GuiTextField field;
        private int buttonId = -1;
        private String value = "";
        private boolean visible;
        private boolean invalid;
        private boolean groupedCoordinate;
        private int x;
        private int y;
        private int w;
        private int h;
        private int validationColor = 0xFF9FB2C8;
        private String validationMessage = "§7无校验";

        private ArgControl(ArgSchema schema) {
            this.schema = schema;
        }
    }

    private static final class ExampleEntry {
        private final String commandText;
        private final String description;

        private ExampleEntry(String commandText, String description) {
            this.commandText = commandText == null ? "" : commandText;
            this.description = description == null ? "" : description;
        }

        private static ExampleEntry parse(String rawLine) {
            if (rawLine == null) {
                return null;
            }
            String trimmed = rawLine.trim();
            if (trimmed.startsWith(">")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (trimmed.isEmpty()) {
                return null;
            }

            int splitIndex = trimmed.indexOf(" - ");
            if (splitIndex > 0) {
                String commandPart = trimmed.substring(0, splitIndex).trim();
                String descriptionPart = trimmed.substring(splitIndex + 3).trim();
                if (!commandPart.isEmpty()) {
                    return new ExampleEntry(commandPart, descriptionPart);
                }
            }
            return new ExampleEntry(trimmed, "");
        }
    }

    private static final class CommandEntry {
        private final String primaryName;
        private final List<String> aliases;
        private final String shortDesc;
        private final List<String> longDesc;
        private final List<ExampleEntry> examples;
        private final String searchText;
        private final CommandCategory category;

        private CommandEntry(String primaryName, List<String> aliases, String shortDesc, List<String> longDesc, CommandCategory category) {
            this.primaryName = primaryName == null ? "" : primaryName;
            this.aliases = aliases == null ? new ArrayList<String>() : aliases;
            this.shortDesc = shortDesc == null ? "" : shortDesc;
            this.longDesc = longDesc == null ? new ArrayList<String>() : longDesc;
            this.examples = extractExamples(longDesc);
            this.searchText = buildSearchText(this.primaryName, this.aliases, this.shortDesc, this.longDesc, this.examples);
            this.category = category == null ? CommandCategory.OTHER : category;
        }

        private static List<ExampleEntry> extractExamples(List<String> longDesc) {
            if (longDesc == null || longDesc.isEmpty()) {
                return Collections.emptyList();
            }
            List<ExampleEntry> result = new ArrayList<ExampleEntry>();
            for (String line : longDesc) {
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.startsWith(">")) {
                    ExampleEntry parsed = ExampleEntry.parse(trimmed);
                    if (parsed != null) {
                        result.add(parsed);
                    }
                }
            }
            return result;
        }

        private static String buildSearchText(String primaryName, List<String> aliases, String shortDesc,
                List<String> longDesc, List<ExampleEntry> examples) {
            StringBuilder builder = new StringBuilder();
            builder.append(primaryName).append(' ');
            if (aliases != null) {
                for (String alias : aliases) {
                    builder.append(alias).append(' ');
                }
            }
            builder.append(shortDesc).append(' ');
            if (longDesc != null) {
                for (String line : longDesc) {
                    builder.append(line).append(' ');
                }
            }
            if (examples != null) {
                for (ExampleEntry example : examples) {
                    builder.append(example.commandText).append(' ').append(example.description).append(' ');
                }
            }
            return builder.toString();
        }
    }

    private static final class HistoryEntry {
        private String time;
        private String command;

        private HistoryEntry() {
        }

        private HistoryEntry(String time, String command) {
            this.time = time == null ? "" : time;
            this.command = command == null ? "" : command;
        }
    }

    private static final class PersistentState {
        private List<String> favorites = new ArrayList<String>();
        private List<String> recent = new ArrayList<String>();
        private List<HistoryEntry> history = new ArrayList<HistoryEntry>();
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






