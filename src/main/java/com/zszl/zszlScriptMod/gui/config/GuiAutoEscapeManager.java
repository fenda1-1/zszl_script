package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.AutoEscapeHandler;
import com.zszl.zszlScriptMod.system.AutoEscapeRule;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiAutoEscapeManager extends AbstractThreePaneRuleManager<AutoEscapeRule> {
    private static final int BTN_SELECT_ESCAPE_SEQUENCE = 1000;
    private static final int BTN_TOGGLE_WHITELIST = 1001;
    private static final int BTN_TOGGLE_BLACKLIST = 1002;
    private static final int BTN_TOGGLE_RESTART = 1003;
    private static final int BTN_SELECT_RESTART_SEQUENCE = 1004;
    private static final int BTN_TOGGLE_IGNORE_UNTIL_RESTART_COMPLETE = 1005;
    private static final int BTN_TOGGLE_ENABLED = 1006;
    private static final int BTN_TOGGLE_PLAYER_GAME_MODE_FILTER = 1007;
    private static final int BTN_ADD_WHITELIST_CARD = 1008;
    private static final int BTN_DELETE_WHITELIST_CARD = 1009;
    private static final int BTN_ADD_BLACKLIST_CARD = 1010;
    private static final int BTN_DELETE_BLACKLIST_CARD = 1011;
    private static final int BTN_TOGGLE_AREA_BLACKLIST = 1012;
    private static final int BTN_FILL_CURRENT_AREA = 1013;
    private static final int BTN_ADD_AREA_BLACKLIST_CARD = 1014;
    private static final int BTN_DELETE_AREA_BLACKLIST_CARD = 1015;
    private static final int BTN_ENTITY_TYPE_BASE = 1100;
    private static final int BTN_PLAYER_GAME_MODE_BASE = 1200;
    private static final long CARD_DOUBLE_CLICK_WINDOW_MS = 300L;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final String[][] ENTITY_TYPE_OPTIONS = new String[][] {
            { "player", "玩家" },
            { "monster", "怪物" },
            { "neutral", "中立" },
            { "animal", "动物" },
            { "water", "水生" },
            { "ambient", "环境" },
            { "villager", "村民" },
            { "golem", "傀儡" },
            { "tameable", "宠物" },
            { "boss", "首领" },
            { "living", "生物" },
            { "any", "任意" }
    };
    private static final String[][] PLAYER_GAME_MODE_OPTIONS = new String[][] {
            { AutoEscapeRule.PLAYER_GAME_MODE_ALL, "全部" },
            { AutoEscapeRule.PLAYER_GAME_MODE_SURVIVAL, "生存" },
            { AutoEscapeRule.PLAYER_GAME_MODE_CREATIVE, "创造" },
            { AutoEscapeRule.PLAYER_GAME_MODE_SPECTATOR, "旁观" },
            { AutoEscapeRule.PLAYER_GAME_MODE_UNKNOWN, "未知" }
    };

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField entityTypesField;
    private GuiTextField detectionRangeField;
    private GuiTextField escapeSequenceField;
    private GuiTextField whitelistField;
    private GuiTextField blacklistField;
    private GuiTextField areaBlacklistField;
    private GuiTextField areaBlacklistRangeField;
    private GuiTextField restartDelayField;
    private GuiTextField restartSequenceField;

    private GuiButton btnSelectEscapeSequence;
    private GuiButton btnToggleWhitelist;
    private GuiButton btnToggleBlacklist;
    private GuiButton btnToggleRestart;
    private GuiButton btnSelectRestartSequence;
    private GuiButton btnToggleIgnoreUntilRestartComplete;
    private GuiButton btnToggleEnabled;
    private GuiButton btnTogglePlayerGameModeFilter;
    private GuiButton btnAddWhitelistCard;
    private GuiButton btnDeleteWhitelistCard;
    private GuiButton btnAddBlacklistCard;
    private GuiButton btnDeleteBlacklistCard;
    private GuiButton btnToggleAreaBlacklist;
    private GuiButton btnFillCurrentArea;
    private GuiButton btnAddAreaBlacklistCard;
    private GuiButton btnDeleteAreaBlacklistCard;
    private final List<ToggleGuiButton> entityTypeButtons = new ArrayList<>();
    private final List<ToggleGuiButton> playerGameModeButtons = new ArrayList<>();
    private final Set<String> editorEntityTypes = new LinkedHashSet<>();
    private final List<String> editorWhitelistCards = new ArrayList<>();
    private final List<String> editorBlacklistCards = new ArrayList<>();
    private final List<AutoEscapeRule.AreaBlacklistEntry> editorAreaBlacklistCards = new ArrayList<>();

    private boolean editorEnabled = true;
    private boolean editorWhitelistEnabled = false;
    private boolean editorBlacklistEnabled = false;
    private boolean editorAreaBlacklistEnabled = false;
    private boolean editorPlayerGameModeFilterEnabled = false;
    private String editorPlayerGameModeFilter = AutoEscapeRule.PLAYER_GAME_MODE_ALL;
    private boolean editorRestartEnabled = false;
    private boolean editorIgnoreTargetsUntilRestartComplete = false;
    private int selectedWhitelistCardIndex = -1;
    private int selectedBlacklistCardIndex = -1;
    private int selectedAreaBlacklistCardIndex = -1;
    private EditorStateSnapshot pendingRestoreState = null;
    private int lastClickedCardIndex = -1;
    private long lastClickedCardTimeMs = 0L;

    public GuiAutoEscapeManager(GuiScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void initGui() {
        EditorStateSnapshot restoreState = pendingRestoreState;
        pendingRestoreState = null;
        super.initGui();
        if (restoreState != null) {
            applyEditorStateSnapshot(restoreState);
        }
    }

    @Override
    protected String getScreenTitle() {
        return "自动逃离规则";
    }

    @Override
    protected String getGuideText() {
        return "§7自动逃离优先级最高：检测到指定实体后会立即打断当前序列并执行逃离序列。";
    }

    @Override
    protected String getEntityDisplayName() {
        return "规则";
    }

    @Override
    protected List<AutoEscapeRule> getSourceItems() {
        return AutoEscapeHandler.getRulesSnapshot();
    }

    @Override
    protected List<String> getSourceCategories() {
        return AutoEscapeHandler.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return AutoEscapeHandler.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return AutoEscapeHandler.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return AutoEscapeHandler.deleteCategory(category);
    }

    @Override
    protected boolean replaceCategoryOrderInSource(List<String> orderedCategories) {
        AutoEscapeHandler.replaceCategoryOrder(orderedCategories);
        return true;
    }

    @Override
    protected void persistChanges() {
        List<AutoEscapeRule> newRules = new ArrayList<>();
        for (AutoEscapeRule rule : allItems) {
            if (rule == null) {
                continue;
            }
            AutoEscapeRule copy = rule.copy();
            copy.normalize();
            newRules.add(copy);
        }
        AutoEscapeHandler.replaceAllRules(newRules);
    }

    @Override
    protected void reloadSource() {
        AutoEscapeHandler.loadConfig();
    }

    @Override
    protected AutoEscapeRule createNewItem() {
        return new AutoEscapeRule();
    }

    @Override
    protected AutoEscapeRule copyItem(AutoEscapeRule source) {
        return source == null ? new AutoEscapeRule() : source.copy();
    }

    @Override
    protected void addItemToSource(AutoEscapeRule item) {
        if (item != null) {
            allItems.add(item);
        }
    }

    @Override
    protected void removeItemFromSource(AutoEscapeRule item) {
        allItems.remove(item);
    }

    @Override
    protected String getItemName(AutoEscapeRule item) {
        return item == null ? "" : safe(item.name);
    }

    @Override
    protected void setItemName(AutoEscapeRule item, String name) {
        if (item != null) {
            item.name = safe(name);
        }
    }

    @Override
    protected String getItemCategory(AutoEscapeRule item) {
        return item == null ? CATEGORY_DEFAULT : normalizeCategory(item.category);
    }

    @Override
    protected void setItemCategory(AutoEscapeRule item, String category) {
        if (item != null) {
            item.category = normalizeCategory(category);
        }
    }

    @Override
    protected void loadEditor(AutoEscapeRule item) {
        AutoEscapeRule model = item == null ? new AutoEscapeRule() : item.copy();
        model.normalize();

        setText(nameField, safe(model.name));
        setText(categoryField, normalizeCategory(model.category));
        setEditorEntityTypes(model.entityTypes);
        setText(entityTypesField, buildEntityTypeSummary());
        setText(detectionRangeField, formatDouble(model.detectionRange));
        setText(escapeSequenceField, safe(model.escapeSequenceName));
        setEditorStringCards(editorWhitelistCards, model.nameWhitelist);
        setEditorStringCards(editorBlacklistCards, model.nameBlacklist);
        setText(whitelistField, "");
        setText(blacklistField, "");
        setEditorAreaBlacklistCards(model.areaBlacklist);
        setText(areaBlacklistField, "");
        setText(areaBlacklistRangeField, "0");
        setText(restartDelayField, String.valueOf(Math.max(0, model.restartDelaySeconds)));
        setText(restartSequenceField, safe(model.restartSequenceName));

        editorEnabled = model.enabled;
        editorWhitelistEnabled = model.enableNameWhitelist;
        editorBlacklistEnabled = model.enableNameBlacklist;
        editorAreaBlacklistEnabled = model.enableAreaBlacklist;
        editorPlayerGameModeFilterEnabled = model.enablePlayerGameModeFilter;
        editorPlayerGameModeFilter = AutoEscapeRule.normalizePlayerGameModeFilter(model.playerGameModeFilter);
        editorRestartEnabled = model.restartEnabled;
        editorIgnoreTargetsUntilRestartComplete = model.ignoreTargetsUntilRestartComplete;

        clampEditorScroll();
        layoutAllWidgets();
    }

    @Override
    protected AutoEscapeRule buildItemFromEditor(boolean creatingNew, AutoEscapeRule selectedItem) {
        AutoEscapeRule base = creatingNew || selectedItem == null ? new AutoEscapeRule() : selectedItem.copy();
        base.name = safe(nameField.getText()).trim();
        base.category = normalizeCategory(categoryField.getText());
        base.entityTypes = new ArrayList<>(editorEntityTypes);
        base.detectionRange = parseDouble(detectionRangeField.getText(), base.detectionRange);
        base.escapeSequenceName = safe(escapeSequenceField.getText()).trim();

        base.enableNameWhitelist = editorWhitelistEnabled;
        base.nameWhitelist = new ArrayList<>(editorWhitelistCards);

        base.enableNameBlacklist = editorBlacklistEnabled;
        base.nameBlacklist = new ArrayList<>(editorBlacklistCards);

        base.enableAreaBlacklist = editorAreaBlacklistEnabled;
        base.areaBlacklist = copyAreaBlacklistEntries(editorAreaBlacklistCards);

        base.enablePlayerGameModeFilter = editorPlayerGameModeFilterEnabled;
        base.playerGameModeFilter = AutoEscapeRule.normalizePlayerGameModeFilter(editorPlayerGameModeFilter);

        base.restartEnabled = editorRestartEnabled;
        base.restartDelaySeconds = parseInt(restartDelayField.getText(), base.restartDelaySeconds);
        base.restartSequenceName = safe(restartSequenceField.getText()).trim();
        base.ignoreTargetsUntilRestartComplete = editorIgnoreTargetsUntilRestartComplete;

        base.enabled = editorEnabled;
        base.normalize();
        return base;
    }

    @Override
    protected void applyItemValues(AutoEscapeRule target, AutoEscapeRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = source.category;
        target.enabled = source.enabled;
        target.entityTypes = new ArrayList<>(
                source.entityTypes == null ? Collections.<String>emptyList() : source.entityTypes);
        target.detectionRange = source.detectionRange;
        target.enableNameWhitelist = source.enableNameWhitelist;
        target.nameWhitelist = new ArrayList<>(
                source.nameWhitelist == null ? Collections.<String>emptyList() : source.nameWhitelist);
        target.enableNameBlacklist = source.enableNameBlacklist;
        target.nameBlacklist = new ArrayList<>(
                source.nameBlacklist == null ? Collections.<String>emptyList() : source.nameBlacklist);
        target.enableAreaBlacklist = source.enableAreaBlacklist;
        target.areaBlacklist = copyAreaBlacklistEntries(source.areaBlacklist);
        target.enablePlayerGameModeFilter = source.enablePlayerGameModeFilter;
        target.playerGameModeFilter = source.playerGameModeFilter;
        target.escapeSequenceName = source.escapeSequenceName;
        target.restartEnabled = source.restartEnabled;
        target.restartDelaySeconds = source.restartDelaySeconds;
        target.restartSequenceName = source.restartSequenceName;
        target.ignoreTargetsUntilRestartComplete = source.ignoreTargetsUntilRestartComplete;
        target.normalize();
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(2100);
        categoryField = createField(2101);
        entityTypesField = createField(2102);
        detectionRangeField = createField(2103);
        escapeSequenceField = createField(2104);
        whitelistField = createField(2105);
        blacklistField = createField(2106);
        areaBlacklistField = createField(2107);
        areaBlacklistRangeField = createField(2108);
        restartDelayField = createField(2109);
        restartSequenceField = createField(2110);

        entityTypesField.setEnabled(false);
        escapeSequenceField.setEnabled(false);
        restartSequenceField.setEnabled(false);

        btnSelectEscapeSequence = createButton(BTN_SELECT_ESCAPE_SEQUENCE, "选择");
        btnToggleWhitelist = createButton(BTN_TOGGLE_WHITELIST, "");
        btnToggleBlacklist = createButton(BTN_TOGGLE_BLACKLIST, "");
        btnToggleRestart = createButton(BTN_TOGGLE_RESTART, "");
        btnSelectRestartSequence = createButton(BTN_SELECT_RESTART_SEQUENCE, "选择");
        btnToggleIgnoreUntilRestartComplete = createButton(BTN_TOGGLE_IGNORE_UNTIL_RESTART_COMPLETE, "");
        btnToggleEnabled = createButton(BTN_TOGGLE_ENABLED, "");
        btnTogglePlayerGameModeFilter = createButton(BTN_TOGGLE_PLAYER_GAME_MODE_FILTER, "");
        btnAddWhitelistCard = createButton(BTN_ADD_WHITELIST_CARD, "");
        btnDeleteWhitelistCard = createButton(BTN_DELETE_WHITELIST_CARD, "");
        btnAddBlacklistCard = createButton(BTN_ADD_BLACKLIST_CARD, "");
        btnDeleteBlacklistCard = createButton(BTN_DELETE_BLACKLIST_CARD, "");
        btnToggleAreaBlacklist = createButton(BTN_TOGGLE_AREA_BLACKLIST, "");
        btnFillCurrentArea = createButton(BTN_FILL_CURRENT_AREA, "当前区域");
        btnAddAreaBlacklistCard = createButton(BTN_ADD_AREA_BLACKLIST_CARD, "");
        btnDeleteAreaBlacklistCard = createButton(BTN_DELETE_AREA_BLACKLIST_CARD, "");

        this.buttonList.add(btnSelectEscapeSequence);
        this.buttonList.add(btnToggleWhitelist);
        this.buttonList.add(btnToggleBlacklist);
        this.buttonList.add(btnToggleRestart);
        this.buttonList.add(btnSelectRestartSequence);
        this.buttonList.add(btnToggleIgnoreUntilRestartComplete);
        this.buttonList.add(btnToggleEnabled);
        this.buttonList.add(btnTogglePlayerGameModeFilter);
        this.buttonList.add(btnAddWhitelistCard);
        this.buttonList.add(btnDeleteWhitelistCard);
        this.buttonList.add(btnAddBlacklistCard);
        this.buttonList.add(btnDeleteBlacklistCard);
        this.buttonList.add(btnToggleAreaBlacklist);
        this.buttonList.add(btnFillCurrentArea);
        this.buttonList.add(btnAddAreaBlacklistCard);
        this.buttonList.add(btnDeleteAreaBlacklistCard);

        entityTypeButtons.clear();
        for (int i = 0; i < ENTITY_TYPE_OPTIONS.length; i++) {
            ToggleGuiButton button = new ToggleGuiButton(BTN_ENTITY_TYPE_BASE + i, 0, 0, 80, 20, "", false);
            entityTypeButtons.add(button);
            this.buttonList.add(button);
        }

        playerGameModeButtons.clear();
        for (int i = 0; i < PLAYER_GAME_MODE_OPTIONS.length; i++) {
            ToggleGuiButton button = new ToggleGuiButton(BTN_PLAYER_GAME_MODE_BASE + i, 0, 0, 64, 20, "", false);
            playerGameModeButtons.add(button);
            this.buttonList.add(button);
        }
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);
        placeField(entityTypesField, 2, editorFieldX, fullFieldWidth);

        int typeColumns = getEntityTypeGridColumns(fullFieldWidth);
        int typeGap = 6;
        int typeButtonWidth = Math.max(64, (fullFieldWidth - typeGap * (typeColumns - 1)) / typeColumns);
        int typeIndex = 0;
        for (int row = 0; row < getEntityTypeGridRowCount(); row++) {
            int editorRow = getEntityTypeStartRow() + row;
            for (int col = 0; col < typeColumns; col++) {
                if (typeIndex >= entityTypeButtons.size()) {
                    break;
                }
                int buttonX = editorFieldX + col * (typeButtonWidth + typeGap);
                placeButton(entityTypeButtons.get(typeIndex), editorRow, buttonX, typeButtonWidth, 20);
                typeIndex++;
            }
        }

        placeField(detectionRangeField, getDetectionRangeRow(), editorFieldX, halfWidth);

        placeButton(btnTogglePlayerGameModeFilter, getPlayerGameModeToggleRow(), editorFieldX, fullFieldWidth, 20);

        int modeColumns = getPlayerGameModeGridColumns(fullFieldWidth);
        int modeGap = 6;
        int modeButtonWidth = Math.max(50, (fullFieldWidth - modeGap * (modeColumns - 1)) / modeColumns);
        int modeIndex = 0;
        for (int row = 0; row < getPlayerGameModeGridRowCount(); row++) {
            int editorRow = getPlayerGameModeStartRow() + row;
            for (int col = 0; col < modeColumns; col++) {
                if (modeIndex >= playerGameModeButtons.size()) {
                    break;
                }
                int buttonX = editorFieldX + col * (modeButtonWidth + modeGap);
                placeButton(playerGameModeButtons.get(modeIndex), editorRow, buttonX, modeButtonWidth, 20);
                modeIndex++;
            }
        }

        placeField(escapeSequenceField, getEscapeSequenceRow(), editorFieldX, Math.max(60, fullFieldWidth - 70));
        placeButton(btnSelectEscapeSequence, getEscapeSequenceRow(),
                escapeSequenceField.x + escapeSequenceField.width + 6, 64, 20);

        placeButton(btnToggleWhitelist, getWhitelistToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(whitelistField, getWhitelistFieldRow(), editorFieldX, fullFieldWidth);
        placeButton(btnAddWhitelistCard, getWhitelistActionRow(), editorFieldX, halfWidth, 20);
        placeButton(btnDeleteWhitelistCard, getWhitelistActionRow(), editorFieldX + halfWidth + 10, halfWidth, 20);

        placeButton(btnToggleBlacklist, getBlacklistToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(blacklistField, getBlacklistFieldRow(), editorFieldX, fullFieldWidth);
        placeButton(btnAddBlacklistCard, getBlacklistActionRow(), editorFieldX, halfWidth, 20);
        placeButton(btnDeleteBlacklistCard, getBlacklistActionRow(), editorFieldX + halfWidth + 10, halfWidth, 20);

        placeButton(btnToggleAreaBlacklist, getAreaBlacklistToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(areaBlacklistField, getAreaBlacklistFieldRow(), editorFieldX, Math.max(60, fullFieldWidth - 90));
        placeButton(btnFillCurrentArea, getAreaBlacklistFieldRow(),
                areaBlacklistField.x + areaBlacklistField.width + 6, 84, 20);
        placeField(areaBlacklistRangeField, getAreaBlacklistRangeRow(), editorFieldX, halfWidth);
        placeButton(btnAddAreaBlacklistCard, getAreaBlacklistActionRow(), editorFieldX, halfWidth, 20);
        placeButton(btnDeleteAreaBlacklistCard, getAreaBlacklistActionRow(),
                editorFieldX + halfWidth + 10, halfWidth, 20);

        placeButton(btnToggleRestart, getRestartToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeField(restartDelayField, getRestartDelayRow(), editorFieldX, halfWidth);

        placeField(restartSequenceField, getRestartSequenceRow(), editorFieldX, Math.max(60, fullFieldWidth - 70));
        placeButton(btnSelectRestartSequence, getRestartSequenceRow(),
                restartSequenceField.x + restartSequenceField.width + 6, 64, 20);

        placeButton(btnToggleIgnoreUntilRestartComplete, getIgnoreTargetsUntilRestartCompleteRow(),
                editorFieldX, fullFieldWidth, 20);

        placeButton(btnToggleEnabled, getEnabledRow(), editorFieldX, fullFieldWidth, 20);
    }

    @Override
    protected int getEditorTotalRows() {
        return getEnabledRow() + 1;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        if (row == 0) {
            return "规则名";
        }
        if (row == 1) {
            return "所属分组";
        }
        if (row == 2) {
            return "已选实体类型";
        }
        if (row == getEntityTypeStartRow()) {
            return "选择实体类型";
        }
        if (row > getEntityTypeStartRow() && row <= getEntityTypeEndRow()) {
            return "";
        }
        if (row == getDetectionRangeRow()) {
            return "检测范围";
        }
        if (row == getPlayerGameModeToggleRow()) {
            return "玩家模式筛选";
        }
        if (row == getPlayerGameModeStartRow()) {
            return "选择玩家模式";
        }
        if (row > getPlayerGameModeStartRow() && row <= getPlayerGameModeEndRow()) {
            return "";
        }
        if (row == getEscapeSequenceRow()) {
            return "逃离序列";
        }
        if (row == getWhitelistToggleRow()) {
            return "名称白名单";
        }
        if (row == getWhitelistFieldRow()) {
            return "白名单输入";
        }
        if (row == getWhitelistActionRow()) {
            return "白名单管理";
        }
        if (row == getWhitelistCardStartRow()) {
            return "白名单卡片";
        }
        if (row > getWhitelistCardStartRow() && row <= getWhitelistCardEndRow()) {
            return "";
        }
        if (row == getBlacklistToggleRow()) {
            return "名称黑名单";
        }
        if (row == getBlacklistFieldRow()) {
            return "黑名单输入";
        }
        if (row == getBlacklistActionRow()) {
            return "黑名单管理";
        }
        if (row == getBlacklistCardStartRow()) {
            return "黑名单卡片";
        }
        if (row > getBlacklistCardStartRow() && row <= getBlacklistCardEndRow()) {
            return "";
        }
        if (row == getAreaBlacklistToggleRow()) {
            return "区域黑名单";
        }
        if (row == getAreaBlacklistFieldRow()) {
            return "区域标识";
        }
        if (row == getAreaBlacklistRangeRow()) {
            return "区域范围";
        }
        if (row == getAreaBlacklistActionRow()) {
            return "区域管理";
        }
        if (row == getAreaBlacklistCardStartRow()) {
            return "区域卡片";
        }
        if (row > getAreaBlacklistCardStartRow() && row <= getAreaBlacklistCardEndRow()) {
            return "";
        }
        if (row == getRestartToggleRow()) {
            return "重启功能";
        }
        if (row == getRestartDelayRow()) {
            return "重启计时";
        }
        if (row == getRestartSequenceRow()) {
            return "后续序列";
        }
        if (row == getIgnoreTargetsUntilRestartCompleteRow()) {
            return "重启前忽略目标";
        }
        if (row == getEnabledRow()) {
            return "启用";
        }
        return "";
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(entityTypesField);
        fields.add(detectionRangeField);
        fields.add(escapeSequenceField);
        fields.add(whitelistField);
        fields.add(blacklistField);
        fields.add(areaBlacklistField);
        fields.add(areaBlacklistRangeField);
        fields.add(restartDelayField);
        fields.add(restartSequenceField);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorFields();
        drawStringCardList(editorWhitelistCards, selectedWhitelistCardIndex, getWhitelistCardStartRow(), mouseX, mouseY);
        drawStringCardList(editorBlacklistCards, selectedBlacklistCardIndex, getBlacklistCardStartRow(), mouseX, mouseY);
        drawAreaBlacklistCardList(mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawEditorTooltip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!listCollapsed && mouseButton == 0 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleItems.size()) {
                long now = System.currentTimeMillis();
                boolean isDoubleClick = actual == lastClickedCardIndex
                        && (now - lastClickedCardTimeMs) <= CARD_DOUBLE_CLICK_WINDOW_MS;

                selectedVisibleIndex = actual;
                creatingNew = false;
                AutoEscapeRule selected = visibleItems.get(actual);
                loadEditor(selected);
                onSelectionChanged(selected);

                if (isDoubleClick) {
                    toggleRuleEnabledFromCard(selected);
                    lastClickedCardIndex = -1;
                    lastClickedCardTimeMs = 0L;
                } else {
                    lastClickedCardIndex = actual;
                    lastClickedCardTimeMs = now;
                }
                return;
            } else {
                lastClickedCardIndex = -1;
                lastClickedCardTimeMs = 0L;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void drawCard(AutoEscapeRule item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered) {
        int cardBottom = y + height;
        int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
        int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

        drawRect(x, y, x + width, cardBottom, bg);
        drawHorizontalLine(x, x + width, y, border);
        drawHorizontalLine(x, x + width, cardBottom, border);
        drawVerticalLine(x, y, cardBottom, border);
        drawVerticalLine(x + width, y, cardBottom, border);

        String status = item.enabled ? "§a✔" : "§7○";
        drawString(fontRenderer, trimToWidth(status + " " + safe(item.name), width - 16), x + 8, y + 5, 0xFFFFFFFF);
        drawString(fontRenderer,
                trimToWidth("分类: " + normalizeCategory(item.category) + " | 范围: " + formatDouble(item.detectionRange),
                        width - 16),
                x + 8, y + 19, 0xFFDDDDDD);
        drawString(fontRenderer,
                trimToWidth("实体: " + joinEntityTypeLabels(item.entityTypes)
                        + " | 玩家模式: " + getPlayerGameModeCardText(item)
                        + " | 区域排除: " + getAreaBlacklistCardText(item), width - 16),
                x + 8, y + 31, 0xFFBDBDBD);

        String restartText = item.restartEnabled
                ? "重启: " + Math.max(0, item.restartDelaySeconds) + "s -> " + safe(item.restartSequenceName)
                        + (item.ignoreTargetsUntilRestartComplete ? " | 重启前忽略目标" : "")
                : "重启: 关闭";
        drawString(fontRenderer,
                trimToWidth("逃离: " + safe(item.escapeSequenceName) + " | 白/黑名单: "
                        + getNameFilterCardText(item) + " | " + restartText, width - 16),
                x + 8, y + 43, 0xFFB8C7D9);
    }

    @Override
    protected void updateEditorButtonStates() {
        setText(entityTypesField, buildEntityTypeSummary());

        if (btnToggleWhitelist != null) {
            btnToggleWhitelist.displayString = "启用名称白名单(包含即可): " + yesNo(editorWhitelistEnabled);
            btnToggleWhitelist.enabled = btnToggleWhitelist.visible;
        }
        if (btnToggleBlacklist != null) {
            btnToggleBlacklist.displayString = "启用名称黑名单(包含即可): " + yesNo(editorBlacklistEnabled);
            btnToggleBlacklist.enabled = btnToggleBlacklist.visible;
        }
        if (btnToggleAreaBlacklist != null) {
            btnToggleAreaBlacklist.displayString = "启用区域黑名单: " + yesNo(editorAreaBlacklistEnabled);
            btnToggleAreaBlacklist.enabled = btnToggleAreaBlacklist.visible;
        }
        if (btnTogglePlayerGameModeFilter != null) {
            btnTogglePlayerGameModeFilter.displayString = "启用玩家模式筛选: "
                    + yesNo(editorPlayerGameModeFilterEnabled);
            btnTogglePlayerGameModeFilter.enabled = btnTogglePlayerGameModeFilter.visible;
        }
        if (btnToggleRestart != null) {
            btnToggleRestart.displayString = "启用重启功能: " + yesNo(editorRestartEnabled);
            btnToggleRestart.enabled = btnToggleRestart.visible;
        }
        if (btnToggleIgnoreUntilRestartComplete != null) {
            btnToggleIgnoreUntilRestartComplete.displayString = "执行完重启前忽略目标: "
                    + yesNo(editorIgnoreTargetsUntilRestartComplete);
            btnToggleIgnoreUntilRestartComplete.enabled = btnToggleIgnoreUntilRestartComplete.visible
                    && editorRestartEnabled;
        }
        if (btnToggleEnabled != null) {
            btnToggleEnabled.displayString = "启用: " + boolText(editorEnabled);
            btnToggleEnabled.enabled = btnToggleEnabled.visible;
        }

        for (int i = 0; i < entityTypeButtons.size(); i++) {
            ToggleGuiButton button = entityTypeButtons.get(i);
            String token = ENTITY_TYPE_OPTIONS[i][0];
            String label = ENTITY_TYPE_OPTIONS[i][1];
            boolean selected = editorEntityTypes.contains(token);
            button.setEnabledState(selected);
            button.displayString = selected ? "§a" + label : "§7" + label;
            button.enabled = button.visible;
        }

        editorPlayerGameModeFilter = AutoEscapeRule.normalizePlayerGameModeFilter(editorPlayerGameModeFilter);
        for (int i = 0; i < playerGameModeButtons.size(); i++) {
            ToggleGuiButton button = playerGameModeButtons.get(i);
            String token = PLAYER_GAME_MODE_OPTIONS[i][0];
            String label = PLAYER_GAME_MODE_OPTIONS[i][1];
            boolean selected = token.equals(editorPlayerGameModeFilter);
            button.setEnabledState(selected);
            button.displayString = selected ? "§a" + label : "§7" + label;
            button.enabled = button.visible && editorPlayerGameModeFilterEnabled;
        }

        boolean hasSelected = !creatingNew && selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleItems.size();
        if (btnDelete != null) {
            btnDelete.enabled = hasSelected;
        }

        if (btnSelectEscapeSequence != null) {
            btnSelectEscapeSequence.enabled = btnSelectEscapeSequence.visible;
        }
        if (btnSelectRestartSequence != null) {
            btnSelectRestartSequence.enabled = btnSelectRestartSequence.visible && editorRestartEnabled;
        }
        if (btnAddWhitelistCard != null) {
            btnAddWhitelistCard.displayString = selectedWhitelistCardIndex >= 0 ? "更新白名单卡片" : "添加白名单卡片";
            btnAddWhitelistCard.enabled = btnAddWhitelistCard.visible;
        }
        if (btnDeleteWhitelistCard != null) {
            btnDeleteWhitelistCard.displayString = "删除选中白名单";
            btnDeleteWhitelistCard.enabled = btnDeleteWhitelistCard.visible && selectedWhitelistCardIndex >= 0
                    && selectedWhitelistCardIndex < editorWhitelistCards.size();
        }
        if (btnAddBlacklistCard != null) {
            btnAddBlacklistCard.displayString = selectedBlacklistCardIndex >= 0 ? "更新黑名单卡片" : "添加黑名单卡片";
            btnAddBlacklistCard.enabled = btnAddBlacklistCard.visible;
        }
        if (btnDeleteBlacklistCard != null) {
            btnDeleteBlacklistCard.displayString = "删除选中黑名单";
            btnDeleteBlacklistCard.enabled = btnDeleteBlacklistCard.visible && selectedBlacklistCardIndex >= 0
                    && selectedBlacklistCardIndex < editorBlacklistCards.size();
        }
        if (btnFillCurrentArea != null) {
            btnFillCurrentArea.enabled = btnFillCurrentArea.visible;
        }
        if (btnAddAreaBlacklistCard != null) {
            btnAddAreaBlacklistCard.displayString = selectedAreaBlacklistCardIndex >= 0 ? "更新区域卡片" : "添加区域卡片";
            btnAddAreaBlacklistCard.enabled = btnAddAreaBlacklistCard.visible;
        }
        if (btnDeleteAreaBlacklistCard != null) {
            btnDeleteAreaBlacklistCard.displayString = "删除选中区域";
            btnDeleteAreaBlacklistCard.enabled = btnDeleteAreaBlacklistCard.visible
                    && selectedAreaBlacklistCardIndex >= 0
                    && selectedAreaBlacklistCardIndex < editorAreaBlacklistCards.size();
        }

        if (whitelistField != null) {
            whitelistField.setEnabled(true);
        }
        if (blacklistField != null) {
            blacklistField.setEnabled(true);
        }
        if (areaBlacklistField != null) {
            areaBlacklistField.setEnabled(true);
        }
        if (areaBlacklistRangeField != null) {
            areaBlacklistRangeField.setEnabled(true);
        }
        if (restartDelayField != null) {
            restartDelayField.setEnabled(editorRestartEnabled);
        }
        if (entityTypesField != null) {
            entityTypesField.setEnabled(false);
        }
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button.id >= BTN_ENTITY_TYPE_BASE && button.id < BTN_ENTITY_TYPE_BASE + ENTITY_TYPE_OPTIONS.length) {
            int index = button.id - BTN_ENTITY_TYPE_BASE;
            String token = ENTITY_TYPE_OPTIONS[index][0];
            if (editorEntityTypes.contains(token)) {
                editorEntityTypes.remove(token);
            } else {
                editorEntityTypes.add(token);
            }
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_PLAYER_GAME_MODE_FILTER) {
            editorPlayerGameModeFilterEnabled = !editorPlayerGameModeFilterEnabled;
            editorPlayerGameModeFilter = AutoEscapeRule.normalizePlayerGameModeFilter(editorPlayerGameModeFilter);
            layoutAllWidgets();
            return true;
        }
        if (button.id >= BTN_PLAYER_GAME_MODE_BASE
                && button.id < BTN_PLAYER_GAME_MODE_BASE + PLAYER_GAME_MODE_OPTIONS.length) {
            if (editorPlayerGameModeFilterEnabled) {
                int index = button.id - BTN_PLAYER_GAME_MODE_BASE;
                editorPlayerGameModeFilter = PLAYER_GAME_MODE_OPTIONS[index][0];
                layoutAllWidgets();
            }
            return true;
        }
        if (button.id == BTN_SELECT_ESCAPE_SEQUENCE) {
            EditorStateSnapshot snapshot = captureEditorStateSnapshot();
            mc.displayGuiScreen(new GuiSequenceSelector(this, selected -> {
                snapshot.escapeSequenceName = safe(selected);
                pendingRestoreState = snapshot;
                mc.displayGuiScreen(this);
            }));
            return true;
        }
        if (button.id == BTN_TOGGLE_WHITELIST) {
            editorWhitelistEnabled = !editorWhitelistEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_BLACKLIST) {
            editorBlacklistEnabled = !editorBlacklistEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_ADD_WHITELIST_CARD) {
            addOrUpdateStringCard(editorWhitelistCards, whitelistField, true);
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_DELETE_WHITELIST_CARD) {
            deleteSelectedStringCard(editorWhitelistCards, true);
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_ADD_BLACKLIST_CARD) {
            addOrUpdateStringCard(editorBlacklistCards, blacklistField, false);
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_DELETE_BLACKLIST_CARD) {
            deleteSelectedStringCard(editorBlacklistCards, false);
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_AREA_BLACKLIST) {
            editorAreaBlacklistEnabled = !editorAreaBlacklistEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_FILL_CURRENT_AREA) {
            fillCurrentAreaIntoEditor();
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_ADD_AREA_BLACKLIST_CARD) {
            addOrUpdateAreaBlacklistCard();
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_DELETE_AREA_BLACKLIST_CARD) {
            deleteSelectedAreaBlacklistCard();
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_RESTART) {
            editorRestartEnabled = !editorRestartEnabled;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_SELECT_RESTART_SEQUENCE) {
            EditorStateSnapshot snapshot = captureEditorStateSnapshot();
            mc.displayGuiScreen(new GuiSequenceSelector(this, selected -> {
                snapshot.restartSequenceName = safe(selected);
                pendingRestoreState = snapshot;
                mc.displayGuiScreen(this);
            }));
            return true;
        }
        if (button.id == BTN_TOGGLE_IGNORE_UNTIL_RESTART_COMPLETE) {
            editorIgnoreTargetsUntilRestartComplete = !editorIgnoreTargetsUntilRestartComplete;
            layoutAllWidgets();
            return true;
        }
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            layoutAllWidgets();
            return true;
        }
        return false;
    }

    @Override
    protected String validateItem(AutoEscapeRule item) {
        if (item == null) {
            return "规则数据无效";
        }
        if (isBlank(item.name)) {
            return "规则名称不能为空";
        }
        if (item.entityTypes == null || item.entityTypes.isEmpty()) {
            return "请至少选择一种实体类型";
        }
        if (item.detectionRange <= 0) {
            return "检测范围必须大于 0";
        }
        if (isBlank(item.escapeSequenceName)) {
            return "逃离序列不能为空";
        }
        if (item.restartEnabled && isBlank(item.restartSequenceName)) {
            return "已启用重启功能时，后续序列不能为空";
        }
        return null;
    }

    private void toggleRuleEnabledFromCard(AutoEscapeRule rule) {
        if (rule == null) {
            return;
        }

        String ruleName = safe(rule.name);
        rule.enabled = !rule.enabled;
        persistChanges();
        refreshData(true);
        selectByItemName(ruleName);

        if (rule.enabled) {
            setStatus("§a已快速启用规则: " + ruleName, 0xFF8CFF9E);
        } else {
            setStatus("§e已快速关闭规则: " + ruleName, 0xFFFFD27F);
        }
    }

    @Override
    protected void onAfterEditorMouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 || !isInEditor(mouseX, mouseY)) {
            return;
        }

        int whitelistIndex = getStringCardIndexAt(mouseX, mouseY, getWhitelistCardStartRow(), editorWhitelistCards);
        if (whitelistIndex >= 0) {
            selectedWhitelistCardIndex = whitelistIndex;
            if (whitelistIndex < editorWhitelistCards.size()) {
                setText(whitelistField, editorWhitelistCards.get(whitelistIndex));
            } else {
                setText(whitelistField, "");
            }
            layoutAllWidgets();
            return;
        }

        int blacklistIndex = getStringCardIndexAt(mouseX, mouseY, getBlacklistCardStartRow(), editorBlacklistCards);
        if (blacklistIndex >= 0) {
            selectedBlacklistCardIndex = blacklistIndex;
            if (blacklistIndex < editorBlacklistCards.size()) {
                setText(blacklistField, editorBlacklistCards.get(blacklistIndex));
            } else {
                setText(blacklistField, "");
            }
            layoutAllWidgets();
            return;
        }

        int areaIndex = getAreaBlacklistCardIndexAt(mouseX, mouseY);
        if (areaIndex >= 0) {
            selectedAreaBlacklistCardIndex = areaIndex;
            syncSelectedAreaBlacklistCardToFields();
            layoutAllWidgets();
        }
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private GuiButton createButton(int id, String text) {
        return new com.zszl.zszlScriptMod.gui.components.ThemedButton(id, 0, 0, 100, 20, text);
    }

    private int getStringCardIndexAt(int mouseX, int mouseY, int startRow, List<String> cards) {
        int right = editorX + editorWidth - 14;
        int width = Math.max(110, right - editorFieldX);
        int rowCount = getStringCardRowCount(cards);
        for (int i = 0; i < rowCount; i++) {
            int y = getEditorRowY(startRow + i);
            if (y >= 0
                    && mouseX >= editorFieldX
                    && mouseX <= editorFieldX + width
                    && mouseY >= y - 1
                    && mouseY <= y + 17) {
                return cards.isEmpty() ? -1 : i;
            }
        }
        return -1;
    }

    private int getAreaBlacklistCardIndexAt(int mouseX, int mouseY) {
        int right = editorX + editorWidth - 14;
        int width = Math.max(110, right - editorFieldX);
        int rowCount = getAreaBlacklistCardRowCount();
        for (int i = 0; i < rowCount; i++) {
            int y = getEditorRowY(getAreaBlacklistCardStartRow() + i);
            if (y >= 0
                    && mouseX >= editorFieldX
                    && mouseX <= editorFieldX + width
                    && mouseY >= y - 1
                    && mouseY <= y + 17) {
                return editorAreaBlacklistCards.isEmpty() ? -1 : i;
            }
        }
        return -1;
    }

    private void syncSelectedAreaBlacklistCardToFields() {
        if (selectedAreaBlacklistCardIndex < 0 || selectedAreaBlacklistCardIndex >= editorAreaBlacklistCards.size()) {
            return;
        }
        AutoEscapeRule.AreaBlacklistEntry entry = editorAreaBlacklistCards.get(selectedAreaBlacklistCardIndex);
        if (entry == null) {
            return;
        }
        setText(areaBlacklistField, entry.areaKey);
        setText(areaBlacklistRangeField, String.valueOf(Math.max(0, entry.chunkRadius)));
    }

    private void drawEditorTooltip(int mouseX, int mouseY) {
        if (!isInEditor(mouseX, mouseY)) {
            return;
        }

        String text = null;
        for (int row = 0; row < getEditorTotalRows(); row++) {
            int y = getEditorRowY(row);
            if (y >= 0 && mouseY >= y - 2 && mouseY <= y + 18) {
                text = getRowDescription(row);
                break;
            }
        }

        if (text == null || text.trim().isEmpty()) {
            return;
        }
        drawHoveringText(this.fontRenderer.listFormattedStringToWidth(text, 260), mouseX, mouseY);
    }

    private String getRowDescription(int row) {
        if (row == 0) {
            return "规则名：用于区分不同自动逃离方案。";
        }
        if (row == 1) {
            return "所属分组：用于左侧规则树分类管理。";
        }
        if (row == 2) {
            return "已选实体类型：这里会汇总当前勾选的实体类型。";
        }
        if (row >= getEntityTypeStartRow() && row <= getEntityTypeEndRow()) {
            return "实体类型：直接点击按钮进行多选，不再需要手动输入。界面会自动换行，确保不会超出编辑器宽度。";
        }
        if (row == getDetectionRangeRow()) {
            return "检测范围：当匹配实体进入当前玩家多少格范围内时，立即触发自动逃离。";
        }
        if (row == getPlayerGameModeToggleRow()) {
            return "玩家模式筛选：开启后，只有匹配所选玩家游戏模式的玩家才会触发；不开启时不检查玩家模式。";
        }
        if (row >= getPlayerGameModeStartRow() && row <= getPlayerGameModeEndRow()) {
            return "玩家模式：只影响玩家实体；生存会同时接受冒险模式，未知表示客户端暂时拿不到该玩家的模式信息。";
        }
        if (row == getEscapeSequenceRow()) {
            return "逃离序列：检测到目标后优先执行的序列，会立刻终止当前正在执行的序列。选择后会保留当前编辑位置。";
        }
        if (row == getWhitelistToggleRow()) {
            return "名称白名单：单独启用后，仅当实体名称包含白名单卡片中的任意关键字时才触发。";
        }
        if (row == getWhitelistFieldRow()) {
            return "白名单输入：这里填写一个关键字，点“添加白名单卡片”后进入下方卡片列表。";
        }
        if (row == getWhitelistActionRow()) {
            return "白名单管理：支持添加、选中回填编辑、更新、删除。";
        }
        if (row >= getWhitelistCardStartRow() && row <= getWhitelistCardEndRow()) {
            return "白名单卡片：点击卡片可选中并回填到输入框，便于继续编辑。";
        }
        if (row == getBlacklistToggleRow()) {
            return "名称黑名单：单独启用后，实体名称只要包含黑名单卡片中的任意关键字就不会触发。";
        }
        if (row == getBlacklistFieldRow()) {
            return "黑名单输入：这里填写一个关键字，点“添加黑名单卡片”后进入下方卡片列表。";
        }
        if (row == getBlacklistActionRow()) {
            return "黑名单管理：支持添加、选中回填编辑、更新、删除。";
        }
        if (row >= getBlacklistCardStartRow() && row <= getBlacklistCardEndRow()) {
            return "黑名单卡片：点击卡片可选中并回填到输入框，便于继续编辑。";
        }
        if (row == getAreaBlacklistToggleRow()) {
            return "区域黑名单：启用后，只要玩家处在这些区域卡片覆盖的范围内，该规则就会被直接排除。";
        }
        if (row == getAreaBlacklistFieldRow()) {
            return "区域标识：默认格式为 维度:chunkX,chunkZ。可点击“当前区域”自动填入，也可手动输入。";
        }
        if (row == getAreaBlacklistRangeRow()) {
            return "区域范围：以区块为单位的半径。0 表示只排除这一个区块；1 表示中心区块周围一圈都排除。";
        }
        if (row == getAreaBlacklistActionRow()) {
            return "区域管理：支持添加、选中回填编辑、更新、删除。";
        }
        if (row >= getAreaBlacklistCardStartRow() && row <= getAreaBlacklistCardEndRow()) {
            return "区域卡片：卡片里会显示区域标识和范围，点击后会自动回填到编辑框。";
        }
        if (row == getRestartToggleRow()) {
            return "重启功能：逃离序列执行完成后，等待指定秒数，再执行一个新的后续序列。";
        }
        if (row == getRestartDelayRow()) {
            return "重启计时：逃离完成后等待多久再执行后续序列；不是恢复原序列，而是执行你指定的新序列。";
        }
        if (row == getRestartSequenceRow()) {
            return "后续序列：重启功能启用后要执行的新序列。选择后会保留当前滚动和编辑位置。";
        }
        if (row == getIgnoreTargetsUntilRestartCompleteRow()) {
            return "重启前忽略目标：开启后，从逃离序列结束开始，一直到后续序列执行结束前，新的目标检测都不会再次打断并重触发自动逃离。";
        }
        if (row == getEnabledRow()) {
            return "启用：关闭后该规则不会参与检测。";
        }
        return "";
    }

    private void setEditorEntityTypes(List<String> types) {
        editorEntityTypes.clear();
        if (types != null) {
            for (String type : types) {
                String normalized = normalizeTypeToken(type);
                if (!normalized.isEmpty()) {
                    editorEntityTypes.add(normalized);
                }
            }
        }
    }

    private String normalizeTypeToken(String type) {
        String normalized = safe(type).trim().toLowerCase();
        for (String[] option : ENTITY_TYPE_OPTIONS) {
            if (option[0].equalsIgnoreCase(normalized)) {
                return option[0];
            }
        }
        return "";
    }

    private String buildEntityTypeSummary() {
        if (editorEntityTypes.isEmpty()) {
            return "未选择";
        }
        List<String> labels = new ArrayList<>();
        for (String[] option : ENTITY_TYPE_OPTIONS) {
            if (editorEntityTypes.contains(option[0])) {
                labels.add(option[1]);
            }
        }
        return joinList(labels);
    }

    private String joinEntityTypeLabels(List<String> types) {
        if (types == null || types.isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String type : types) {
            String label = getEntityTypeLabel(type);
            labels.add(label.isEmpty() ? safe(type) : label);
        }
        return joinList(labels);
    }

    private String getEntityTypeLabel(String token) {
        String normalized = normalizeTypeToken(token);
        for (String[] option : ENTITY_TYPE_OPTIONS) {
            if (option[0].equalsIgnoreCase(normalized)) {
                return option[1];
            }
        }
        return "";
    }

    private String getPlayerGameModeCardText(AutoEscapeRule rule) {
        if (rule == null || !rule.enablePlayerGameModeFilter) {
            return "关闭";
        }
        return getPlayerGameModeLabel(rule.playerGameModeFilter);
    }

    private String getPlayerGameModeLabel(String token) {
        String normalized = AutoEscapeRule.normalizePlayerGameModeFilter(token);
        for (String[] option : PLAYER_GAME_MODE_OPTIONS) {
            if (option[0].equalsIgnoreCase(normalized)) {
                return option[1];
            }
        }
        return "全部";
    }

    private String getNameFilterCardText(AutoEscapeRule rule) {
        int whitelistCount = rule == null || rule.nameWhitelist == null ? 0 : rule.nameWhitelist.size();
        int blacklistCount = rule == null || rule.nameBlacklist == null ? 0 : rule.nameBlacklist.size();
        String whitelistText = rule != null && rule.enableNameWhitelist ? String.valueOf(whitelistCount) : "关";
        String blacklistText = rule != null && rule.enableNameBlacklist ? String.valueOf(blacklistCount) : "关";
        return whitelistText + "/" + blacklistText;
    }

    private String getAreaBlacklistCardText(AutoEscapeRule rule) {
        if (rule == null || !rule.enableAreaBlacklist) {
            return "关";
        }
        return String.valueOf(rule.areaBlacklist == null ? 0 : rule.areaBlacklist.size());
    }

    private void setEditorStringCards(List<String> target, List<String> source) {
        target.clear();
        if (source != null) {
            for (String value : source) {
                String normalized = safe(value).trim();
                if (!normalized.isEmpty() && !containsIgnoreCase(target, normalized)) {
                    target.add(normalized);
                }
            }
        }
    }

    private void setEditorAreaBlacklistCards(List<AutoEscapeRule.AreaBlacklistEntry> source) {
        editorAreaBlacklistCards.clear();
        if (source != null) {
            for (AutoEscapeRule.AreaBlacklistEntry entry : source) {
                if (entry == null) {
                    continue;
                }
                AutoEscapeRule.AreaBlacklistEntry copy = entry.copy();
                if (!copy.areaKey.isEmpty()) {
                    editorAreaBlacklistCards.add(copy);
                }
            }
        }
        clearEditorCardSelections();
    }

    private void clearEditorCardSelections() {
        selectedWhitelistCardIndex = -1;
        selectedBlacklistCardIndex = -1;
        selectedAreaBlacklistCardIndex = -1;
    }

    private List<AutoEscapeRule.AreaBlacklistEntry> copyAreaBlacklistEntries(
            List<AutoEscapeRule.AreaBlacklistEntry> source) {
        List<AutoEscapeRule.AreaBlacklistEntry> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (AutoEscapeRule.AreaBlacklistEntry entry : source) {
            if (entry != null) {
                copy.add(entry.copy());
            }
        }
        return copy;
    }

    private void drawStringCardList(List<String> cards, int selectedIndex, int startRow, int mouseX, int mouseY) {
        int right = editorX + editorWidth - 14;
        int width = Math.max(110, right - editorFieldX);
        int rowCount = getStringCardRowCount(cards);
        for (int i = 0; i < rowCount; i++) {
            int row = startRow + i;
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }

            boolean empty = cards.isEmpty();
            boolean selected = !empty && i == selectedIndex;
            boolean hovered = mouseX >= editorFieldX && mouseX <= editorFieldX + width
                    && mouseY >= y - 1 && mouseY <= y + 17;

            drawInlineCard(editorFieldX, y - 1, width, 18, selected, hovered);
            String text = empty ? "暂无卡片，先在上方输入后添加" : cards.get(i);
            int color = empty ? 0xFF7F8FA4 : 0xFFE6F2FF;
            drawString(fontRenderer, trimToWidth(text, width - 12), editorFieldX + 6, y + 4, color);
        }
    }

    private void drawAreaBlacklistCardList(int mouseX, int mouseY) {
        int right = editorX + editorWidth - 14;
        int width = Math.max(110, right - editorFieldX);
        int rowCount = getAreaBlacklistCardRowCount();
        for (int i = 0; i < rowCount; i++) {
            int row = getAreaBlacklistCardStartRow() + i;
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }

            boolean empty = editorAreaBlacklistCards.isEmpty();
            boolean selected = !empty && i == selectedAreaBlacklistCardIndex;
            boolean hovered = mouseX >= editorFieldX && mouseX <= editorFieldX + width
                    && mouseY >= y - 1 && mouseY <= y + 17;

            drawInlineCard(editorFieldX, y - 1, width, 18, selected, hovered);
            String text = empty ? "暂无区域卡片，可获取当前区域或手动填写后添加"
                    : buildAreaBlacklistEntryLabel(editorAreaBlacklistCards.get(i));
            int color = empty ? 0xFF7F8FA4 : 0xFFE6F2FF;
            drawString(fontRenderer, trimToWidth(text, width - 12), editorFieldX + 6, y + 4, color);
        }
    }

    private void drawInlineCard(int x, int y, int width, int height, boolean selected, boolean hovered) {
        int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
        int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);
        drawRect(x, y, x + width, y + height, bg);
        drawHorizontalLine(x, x + width, y, border);
        drawHorizontalLine(x, x + width, y + height, border);
        drawVerticalLine(x, y, y + height, border);
        drawVerticalLine(x + width, y, y + height, border);
    }

    private void addOrUpdateStringCard(List<String> cards, GuiTextField field, boolean whitelist) {
        String value = safe(field == null ? "" : field.getText()).trim();
        if (value.isEmpty()) {
            setStatus("§c请输入要加入卡片的关键字", 0xFFFF8E8E);
            return;
        }

        int selectedIndex = whitelist ? selectedWhitelistCardIndex : selectedBlacklistCardIndex;
        int existingIndex = findIgnoreCase(cards, value);
        if (selectedIndex >= 0 && selectedIndex < cards.size()) {
            if (existingIndex >= 0 && existingIndex != selectedIndex) {
                setStatus("§e已存在相同关键字卡片: " + value, 0xFFFFD27F);
                if (whitelist) {
                    selectedWhitelistCardIndex = existingIndex;
                } else {
                    selectedBlacklistCardIndex = existingIndex;
                }
                return;
            }
            cards.set(selectedIndex, value);
            setStatus("§a已更新卡片: " + value, 0xFF8CFF9E);
        } else if (existingIndex >= 0) {
            if (whitelist) {
                selectedWhitelistCardIndex = existingIndex;
            } else {
                selectedBlacklistCardIndex = existingIndex;
            }
            setStatus("§e卡片已存在，已为你选中: " + value, 0xFFFFD27F);
        } else {
            cards.add(value);
            if (whitelist) {
                selectedWhitelistCardIndex = cards.size() - 1;
            } else {
                selectedBlacklistCardIndex = cards.size() - 1;
            }
            setStatus("§a已添加卡片: " + value, 0xFF8CFF9E);
        }
    }

    private void deleteSelectedStringCard(List<String> cards, boolean whitelist) {
        int selectedIndex = whitelist ? selectedWhitelistCardIndex : selectedBlacklistCardIndex;
        if (selectedIndex < 0 || selectedIndex >= cards.size()) {
            setStatus("§e请先选中要删除的卡片", 0xFFFFD27F);
            return;
        }

        String removed = cards.remove(selectedIndex);
        if (whitelist) {
            selectedWhitelistCardIndex = cards.isEmpty() ? -1 : Math.min(selectedIndex, cards.size() - 1);
            setText(whitelistField,
                    selectedWhitelistCardIndex >= 0 ? cards.get(selectedWhitelistCardIndex) : "");
        } else {
            selectedBlacklistCardIndex = cards.isEmpty() ? -1 : Math.min(selectedIndex, cards.size() - 1);
            setText(blacklistField,
                    selectedBlacklistCardIndex >= 0 ? cards.get(selectedBlacklistCardIndex) : "");
        }
        setStatus("§a已删除卡片: " + removed, 0xFF8CFF9E);
    }

    private void fillCurrentAreaIntoEditor() {
        String currentArea = AutoEscapeHandler.getCurrentAreaKey();
        if (currentArea.isEmpty()) {
            setStatus("§e当前没有可读取的玩家区域", 0xFFFFD27F);
            return;
        }
        setText(areaBlacklistField, currentArea);
        if (selectedAreaBlacklistCardIndex < 0) {
            setText(areaBlacklistRangeField, "0");
        }
        setStatus("§a已填入当前区域: " + currentArea, 0xFF8CFF9E);
    }

    private void addOrUpdateAreaBlacklistCard() {
        String normalizedArea = normalizeAreaInput(areaBlacklistField == null ? "" : areaBlacklistField.getText());
        if (normalizedArea.isEmpty()) {
            setStatus("§c区域格式无效；请填写 维度:chunkX,chunkZ，或直接获取当前区域", 0xFFFF8E8E);
            return;
        }

        int radius = Math.max(0, parseInt(areaBlacklistRangeField == null ? "0" : areaBlacklistRangeField.getText(), 0));
        AutoEscapeRule.AreaBlacklistEntry newEntry = new AutoEscapeRule.AreaBlacklistEntry(normalizedArea, radius);
        int existingIndex = findAreaBlacklistEntryIndex(newEntry.areaKey, newEntry.chunkRadius);
        if (selectedAreaBlacklistCardIndex >= 0 && selectedAreaBlacklistCardIndex < editorAreaBlacklistCards.size()) {
            if (existingIndex >= 0 && existingIndex != selectedAreaBlacklistCardIndex) {
                selectedAreaBlacklistCardIndex = existingIndex;
                setStatus("§e已存在相同区域卡片，已为你选中", 0xFFFFD27F);
                syncSelectedAreaBlacklistCardToFields();
                return;
            }
            editorAreaBlacklistCards.set(selectedAreaBlacklistCardIndex, newEntry);
            setStatus("§a已更新区域卡片", 0xFF8CFF9E);
            return;
        }

        if (existingIndex >= 0) {
            selectedAreaBlacklistCardIndex = existingIndex;
            syncSelectedAreaBlacklistCardToFields();
            setStatus("§e区域卡片已存在，已为你选中", 0xFFFFD27F);
            return;
        }

        editorAreaBlacklistCards.add(newEntry);
        selectedAreaBlacklistCardIndex = editorAreaBlacklistCards.size() - 1;
        setStatus("§a已添加区域卡片", 0xFF8CFF9E);
    }

    private void deleteSelectedAreaBlacklistCard() {
        if (selectedAreaBlacklistCardIndex < 0 || selectedAreaBlacklistCardIndex >= editorAreaBlacklistCards.size()) {
            setStatus("§e请先选中要删除的区域卡片", 0xFFFFD27F);
            return;
        }

        editorAreaBlacklistCards.remove(selectedAreaBlacklistCardIndex);
        selectedAreaBlacklistCardIndex = editorAreaBlacklistCards.isEmpty() ? -1
                : Math.min(selectedAreaBlacklistCardIndex, editorAreaBlacklistCards.size() - 1);
        setText(areaBlacklistField, "");
        setText(areaBlacklistRangeField, "0");
        if (selectedAreaBlacklistCardIndex >= 0) {
            syncSelectedAreaBlacklistCardToFields();
        }
        setStatus("§a已删除区域卡片", 0xFF8CFF9E);
    }

    private String normalizeAreaInput(String raw) {
        String normalized = AutoEscapeRule.normalizeAreaKey(raw);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.indexOf(':') >= 0) {
            return normalized;
        }
        if (normalized.indexOf(',') < 0) {
            return "";
        }
        String currentArea = AutoEscapeHandler.getCurrentAreaKey();
        int colonIndex = currentArea.indexOf(':');
        if (colonIndex <= 0) {
            return "";
        }
        return currentArea.substring(0, colonIndex) + ":" + normalized;
    }

    private int findIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) {
            return -1;
        }
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (value != null && value.equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }

    private int findAreaBlacklistEntryIndex(String areaKey, int radius) {
        for (int i = 0; i < editorAreaBlacklistCards.size(); i++) {
            AutoEscapeRule.AreaBlacklistEntry entry = editorAreaBlacklistCards.get(i);
            if (entry != null
                    && entry.chunkRadius == radius
                    && entry.areaKey.equalsIgnoreCase(areaKey)) {
                return i;
            }
        }
        return -1;
    }

    private int getStringCardRowCount(List<String> cards) {
        return Math.max(1, cards == null ? 0 : cards.size());
    }

    private int getAreaBlacklistCardRowCount() {
        return Math.max(1, editorAreaBlacklistCards.size());
    }

    private String buildAreaBlacklistEntryLabel(AutoEscapeRule.AreaBlacklistEntry entry) {
        if (entry == null) {
            return "";
        }
        return entry.areaKey + "  |  范围 ±" + Math.max(0, entry.chunkRadius) + " 区块";
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(safe(text).trim().replace(',', '.'));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(safe(text).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String formatDouble(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    private String yesNo(boolean yes) {
        return yes ? "是" : "否";
    }

    private String boolText(boolean enabled) {
        return enabled ? "开" : "关";
    }

    private int getEntityTypeGridColumns(int fullFieldWidth) {
        if (fullFieldWidth >= 300) {
            return 3;
        }
        if (fullFieldWidth >= 190) {
            return 2;
        }
        return 1;
    }

    private int getEntityTypeGridRowCount() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int columns = getEntityTypeGridColumns(fullFieldWidth);
        return Math.max(1, (entityTypeButtons.size() + columns - 1) / columns);
    }

    private int getPlayerGameModeGridColumns(int fullFieldWidth) {
        if (fullFieldWidth >= 340) {
            return 5;
        }
        if (fullFieldWidth >= 220) {
            return 3;
        }
        if (fullFieldWidth >= 150) {
            return 2;
        }
        return 1;
    }

    private int getPlayerGameModeGridRowCount() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(110, right - editorFieldX);
        int columns = getPlayerGameModeGridColumns(fullFieldWidth);
        return Math.max(1, (playerGameModeButtons.size() + columns - 1) / columns);
    }

    private int getEntityTypeStartRow() {
        return 3;
    }

    private int getEntityTypeEndRow() {
        return getEntityTypeStartRow() + getEntityTypeGridRowCount() - 1;
    }

    private int getDetectionRangeRow() {
        return getEntityTypeEndRow() + 1;
    }

    private int getPlayerGameModeToggleRow() {
        return getDetectionRangeRow() + 1;
    }

    private int getPlayerGameModeStartRow() {
        return getPlayerGameModeToggleRow() + 1;
    }

    private int getPlayerGameModeEndRow() {
        return getPlayerGameModeStartRow() + getPlayerGameModeGridRowCount() - 1;
    }

    private int getEscapeSequenceRow() {
        return getPlayerGameModeEndRow() + 1;
    }

    private int getWhitelistToggleRow() {
        return getEscapeSequenceRow() + 1;
    }

    private int getWhitelistFieldRow() {
        return getWhitelistToggleRow() + 1;
    }

    private int getWhitelistActionRow() {
        return getWhitelistFieldRow() + 1;
    }

    private int getWhitelistCardStartRow() {
        return getWhitelistActionRow() + 1;
    }

    private int getWhitelistCardEndRow() {
        return getWhitelistCardStartRow() + getStringCardRowCount(editorWhitelistCards) - 1;
    }

    private int getBlacklistToggleRow() {
        return getWhitelistCardEndRow() + 1;
    }

    private int getBlacklistFieldRow() {
        return getBlacklistToggleRow() + 1;
    }

    private int getBlacklistActionRow() {
        return getBlacklistFieldRow() + 1;
    }

    private int getBlacklistCardStartRow() {
        return getBlacklistActionRow() + 1;
    }

    private int getBlacklistCardEndRow() {
        return getBlacklistCardStartRow() + getStringCardRowCount(editorBlacklistCards) - 1;
    }

    private int getAreaBlacklistToggleRow() {
        return getBlacklistCardEndRow() + 1;
    }

    private int getAreaBlacklistFieldRow() {
        return getAreaBlacklistToggleRow() + 1;
    }

    private int getAreaBlacklistRangeRow() {
        return getAreaBlacklistFieldRow() + 1;
    }

    private int getAreaBlacklistActionRow() {
        return getAreaBlacklistRangeRow() + 1;
    }

    private int getAreaBlacklistCardStartRow() {
        return getAreaBlacklistActionRow() + 1;
    }

    private int getAreaBlacklistCardEndRow() {
        return getAreaBlacklistCardStartRow() + getAreaBlacklistCardRowCount() - 1;
    }

    private int getRestartToggleRow() {
        return getAreaBlacklistCardEndRow() + 1;
    }

    private int getRestartDelayRow() {
        return getRestartToggleRow() + 1;
    }

    private int getRestartSequenceRow() {
        return getRestartDelayRow() + 1;
    }

    private int getIgnoreTargetsUntilRestartCompleteRow() {
        return getRestartSequenceRow() + 1;
    }

    private int getEnabledRow() {
        return getIgnoreTargetsUntilRestartCompleteRow() + 1;
    }

    private EditorStateSnapshot captureEditorStateSnapshot() {
        EditorStateSnapshot snapshot = new EditorStateSnapshot();
        snapshot.selectedItemName = getSelectedItemName();
        snapshot.creatingNew = creatingNew;
        snapshot.editorScrollOffset = editorScrollOffset;
        snapshot.name = nameField == null ? "" : safe(nameField.getText());
        snapshot.category = categoryField == null ? "" : safe(categoryField.getText());
        snapshot.entityTypes.addAll(editorEntityTypes);
        snapshot.detectionRange = detectionRangeField == null ? "" : safe(detectionRangeField.getText());
        snapshot.escapeSequenceName = escapeSequenceField == null ? "" : safe(escapeSequenceField.getText());
        snapshot.whitelistCards.addAll(editorWhitelistCards);
        snapshot.whitelistEnabled = editorWhitelistEnabled;
        snapshot.whitelist = whitelistField == null ? "" : safe(whitelistField.getText());
        snapshot.selectedWhitelistCardIndex = selectedWhitelistCardIndex;
        snapshot.blacklistCards.addAll(editorBlacklistCards);
        snapshot.blacklistEnabled = editorBlacklistEnabled;
        snapshot.blacklist = blacklistField == null ? "" : safe(blacklistField.getText());
        snapshot.selectedBlacklistCardIndex = selectedBlacklistCardIndex;
        snapshot.areaBlacklistEnabled = editorAreaBlacklistEnabled;
        snapshot.areaBlacklistCards = copyAreaBlacklistEntries(editorAreaBlacklistCards);
        snapshot.areaBlacklist = areaBlacklistField == null ? "" : safe(areaBlacklistField.getText());
        snapshot.areaBlacklistRange = areaBlacklistRangeField == null ? "0" : safe(areaBlacklistRangeField.getText());
        snapshot.selectedAreaBlacklistCardIndex = selectedAreaBlacklistCardIndex;
        snapshot.playerGameModeFilterEnabled = editorPlayerGameModeFilterEnabled;
        snapshot.playerGameModeFilter = AutoEscapeRule.normalizePlayerGameModeFilter(editorPlayerGameModeFilter);
        snapshot.restartEnabled = editorRestartEnabled;
        snapshot.restartDelay = restartDelayField == null ? "" : safe(restartDelayField.getText());
        snapshot.restartSequenceName = restartSequenceField == null ? "" : safe(restartSequenceField.getText());
        snapshot.ignoreTargetsUntilRestartComplete = editorIgnoreTargetsUntilRestartComplete;
        snapshot.enabled = editorEnabled;
        return snapshot;
    }

    private void applyEditorStateSnapshot(EditorStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (snapshot.creatingNew) {
            creatingNew = true;
            selectedVisibleIndex = -1;
            clearEditorForNew();
        } else if (!isBlank(snapshot.selectedItemName)) {
            selectByItemName(snapshot.selectedItemName);
        }

        setText(nameField, snapshot.name);
        setText(categoryField, snapshot.category);
        editorEntityTypes.clear();
        editorEntityTypes.addAll(snapshot.entityTypes);
        setText(entityTypesField, buildEntityTypeSummary());
        setText(detectionRangeField, snapshot.detectionRange);
        setText(escapeSequenceField, snapshot.escapeSequenceName);
        editorWhitelistCards.clear();
        editorWhitelistCards.addAll(snapshot.whitelistCards);
        setText(whitelistField, snapshot.whitelist);
        editorBlacklistCards.clear();
        editorBlacklistCards.addAll(snapshot.blacklistCards);
        setText(blacklistField, snapshot.blacklist);
        editorAreaBlacklistCards.clear();
        editorAreaBlacklistCards.addAll(copyAreaBlacklistEntries(snapshot.areaBlacklistCards));
        setText(areaBlacklistField, snapshot.areaBlacklist);
        setText(areaBlacklistRangeField, snapshot.areaBlacklistRange);
        setText(restartDelayField, snapshot.restartDelay);
        setText(restartSequenceField, snapshot.restartSequenceName);

        editorEnabled = snapshot.enabled;
        editorWhitelistEnabled = snapshot.whitelistEnabled;
        editorBlacklistEnabled = snapshot.blacklistEnabled;
        editorAreaBlacklistEnabled = snapshot.areaBlacklistEnabled;
        selectedWhitelistCardIndex = snapshot.selectedWhitelistCardIndex;
        selectedBlacklistCardIndex = snapshot.selectedBlacklistCardIndex;
        selectedAreaBlacklistCardIndex = snapshot.selectedAreaBlacklistCardIndex;
        editorPlayerGameModeFilterEnabled = snapshot.playerGameModeFilterEnabled;
        editorPlayerGameModeFilter = AutoEscapeRule.normalizePlayerGameModeFilter(snapshot.playerGameModeFilter);
        editorRestartEnabled = snapshot.restartEnabled;
        editorIgnoreTargetsUntilRestartComplete = snapshot.ignoreTargetsUntilRestartComplete;
        editorScrollOffset = snapshot.editorScrollOffset;
        clampEditorScroll();
        layoutAllWidgets();
    }

    private static class EditorStateSnapshot {
        private String selectedItemName = "";
        private boolean creatingNew = false;
        private int editorScrollOffset = 0;
        private String name = "";
        private String category = "";
        private final Set<String> entityTypes = new LinkedHashSet<>();
        private String detectionRange = "";
        private String escapeSequenceName = "";
        private final List<String> whitelistCards = new ArrayList<>();
        private boolean whitelistEnabled = false;
        private String whitelist = "";
        private int selectedWhitelistCardIndex = -1;
        private final List<String> blacklistCards = new ArrayList<>();
        private boolean blacklistEnabled = false;
        private String blacklist = "";
        private int selectedBlacklistCardIndex = -1;
        private boolean areaBlacklistEnabled = false;
        private List<AutoEscapeRule.AreaBlacklistEntry> areaBlacklistCards = new ArrayList<>();
        private String areaBlacklist = "";
        private String areaBlacklistRange = "0";
        private int selectedAreaBlacklistCardIndex = -1;
        private boolean playerGameModeFilterEnabled = false;
        private String playerGameModeFilter = AutoEscapeRule.PLAYER_GAME_MODE_ALL;
        private boolean restartEnabled = false;
        private String restartDelay = "";
        private String restartSequenceName = "";
        private boolean ignoreTargetsUntilRestartComplete = false;
        private boolean enabled = true;
    }
}
