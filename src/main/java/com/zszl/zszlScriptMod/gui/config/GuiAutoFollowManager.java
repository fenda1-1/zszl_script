package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.path.GuiPathManager;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.AutoFollowHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.system.AutoFollowRule;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GuiAutoFollowManager extends ThemedGuiScreen {

    private static final String CATEGORY_ALL = "__all__";
    private static final String CATEGORY_DEFAULT = "默认";

    private static final int BTN_NEW = 100;
    private static final int BTN_DELETE = 101;
    private static final int BTN_SAVE = 102;
    private static final int BTN_DONE = 103;
    private static final int BTN_GET_P1 = 104;
    private static final int BTN_GET_P2 = 105;
    private static final int BTN_GET_P3 = 106;
    private static final int BTN_TOGGLE_ACTIVE = 107;
    private static final int BTN_TOGGLE_OUT_OF_RANGE_SEQUENCE = 108;
    private static final int BTN_TOGGLE_VISUALIZE = 109;
    private static final int BTN_SELECT_OUT_OF_RANGE_SEQUENCE = 110;
    private static final int BTN_ADD_RETURN_POINT = 111;
    private static final int BTN_REMOVE_RETURN_POINT = 112;
    private static final int BTN_TOGGLE_PATROL_MODE = 113;
    private static final int BTN_TOGGLE_NAME_LIST = 114;
    private static final int BTN_TOGGLE_TARGET_INVISIBLE = 115;
    private static final int BTN_TOGGLE_TARGET_SPECIAL = 116;
    private static final int BTN_TOGGLE_VISUALIZE_LOCK_CHASE = 117;
    private static final int BTN_TOGGLE_MONSTER_CHASE_MODE = 118;
    private static final int BTN_ENTITY_TYPE_BASE = 200;

    private static final int TREE_ROW_HEIGHT = 18;
    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 6;
    private static final long CARD_DOUBLE_CLICK_WINDOW_MS = 300L;
    private static final int EDITOR_ROW_HEIGHT = 24;
    private static final int RETURN_LIST_BOX_ROWS = 4;
    private static final int RETURN_LIST_ITEM_HEIGHT = 20;
    private static final int MONSTER_SCORE_BOX_ROWS = 5;
    private static final int MONSTER_SCORE_ITEM_HEIGHT = 40;

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");
    private static final String[][] ENTITY_TYPE_OPTIONS = new String[][] {
            { AutoFollowRule.ENTITY_TYPE_MONSTER, "怪物" },
            { AutoFollowRule.ENTITY_TYPE_BOSS, "首领" },
            { AutoFollowRule.ENTITY_TYPE_GOLEM, "傀儡" },
            { AutoFollowRule.ENTITY_TYPE_NEUTRAL, "中立" },
            { AutoFollowRule.ENTITY_TYPE_ANIMAL, "动物" },
            { AutoFollowRule.ENTITY_TYPE_WATER, "水生" },
            { AutoFollowRule.ENTITY_TYPE_AMBIENT, "环境" },
            { AutoFollowRule.ENTITY_TYPE_VILLAGER, "村民" },
            { AutoFollowRule.ENTITY_TYPE_TAMEABLE, "宠物" },
            { AutoFollowRule.ENTITY_TYPE_PLAYER, "玩家" },
            { AutoFollowRule.ENTITY_TYPE_LIVING, "生物" },
            { AutoFollowRule.ENTITY_TYPE_ANY, "任意" }
    };

    private final GuiScreen parentScreen;

    private final List<AutoFollowRule> allRules = new ArrayList<>();
    private final List<AutoFollowRule> visibleRules = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<TreeRow> visibleTreeRows = new ArrayList<>();
    private final Set<String> expandedCategories = new LinkedHashSet<>();
    private final List<AutoFollowHandler.Point> editorReturnPoints = new ArrayList<>();

    private String selectedCategory = CATEGORY_ALL;
    private int selectedVisibleIndex = -1;
    private int selectedTreeIndex = 0;
    private int treeScrollOffset = 0;
    private int listScrollOffset = 0;
    private int editorScrollOffset = 0;
    private int returnPointListScrollOffset = 0;
    private int monsterScoreListScrollOffset = 0;
    private boolean creatingNew = false;
    private boolean treeCollapsed = false;
    private boolean listCollapsed = false;
    private int lastClickedCardIndex = -1;
    private long lastClickedCardTimeMs = 0L;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int treeX;
    private int treeY;
    private int treeWidth;
    private int treeHeight;

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    private int editorX;
    private int editorY;
    private int editorWidth;
    private int editorHeight;
    private int editorLabelX;
    private int editorFieldX;
    private int editorRowStartY;
    private int editorVisibleRows;

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField point1XField;
    private GuiTextField point1ZField;
    private GuiTextField point2XField;
    private GuiTextField point2ZField;
    private GuiTextField returnPointXField;
    private GuiTextField returnPointZField;
    private GuiTextField returnStayMillisField;
    private GuiTextField returnArriveDistanceField;
    private GuiTextField maxRecoveryDistanceField;
    private GuiTextField monsterVerticalRangeField;
    private GuiTextField monsterUpwardRangeField;
    private GuiTextField monsterDownwardRangeField;
    private GuiTextField monsterStopDistanceField;
    private GuiTextField monsterFixedDistanceField;
    private GuiTextField monsterEntityTypesField;
    private GuiTextField monsterWhitelistField;
    private GuiTextField monsterBlacklistField;
    private GuiTextField lockChaseOutOfBoundsDistanceField;

    private GuiButton btnNew;
    private GuiButton btnDelete;
    private GuiButton btnSave;
    private GuiButton btnDone;
    private GuiButton btnGetP1;
    private GuiButton btnGetP2;
    private GuiButton btnGetP3;
    private GuiButton btnAddReturnPoint;
    private GuiButton btnRemoveReturnPoint;
    private GuiButton btnToggleActive;
    private GuiButton btnToggleOutOfRangeSequence;
    private GuiButton btnToggleVisualize;
    private GuiButton btnSelectOutOfRangeSequence;
    private GuiButton btnTogglePatrolMode;
    private GuiButton btnToggleNameList;
    private GuiButton btnToggleTargetInvisible;
    private GuiButton btnToggleTargetSpecial;
    private GuiButton btnToggleVisualizeLockChase;
    private GuiButton btnToggleMonsterChaseMode;
    private final List<ToggleGuiButton> entityTypeButtons = new ArrayList<>();

    private boolean editorActive = false;
    private boolean editorRunSequenceWhenOutOfRecoveryRange = false;
    private String editorOutOfRangeSequenceName = "";
    private boolean editorVisualizeRange = false;
    private boolean editorVisualizeLockChaseRadius = false;
    private String editorPatrolMode = AutoFollowRule.PATROL_MODE_ORDER;
    private boolean editorEnableMonsterNameList = false;
    private boolean editorTargetInvisibleMonsters = false;
    private boolean editorTargetSpecialMobs = true;
    private String editorMonsterChaseMode = AutoFollowRule.MONSTER_CHASE_MODE_APPROACH;
    private final Set<String> editorEntityTypes = new LinkedHashSet<>();
    private final Set<Integer> selectedReturnPointIndices = new LinkedHashSet<>();
    private int returnPointSelectionAnchor = -1;

    private final List<ContextMenuItem> contextMenuItems = new ArrayList<>();
    private boolean contextMenuVisible = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private int contextMenuWidth = 170;
    private int contextMenuItemHeight = 18;
    private int contextMenuTargetIndex = -1;
    private String contextMenuTargetType = "card";
    private String contextMenuTargetCategory = "";
    private int pendingTreePressIndex = -1;
    private int pendingTreePressMouseX = 0;
    private int pendingTreePressMouseY = 0;
    private boolean treeDragging = false;
    private DragPayload activeDragPayload = null;
    private TreeDropTarget currentTreeDropTarget = null;

    private String statusMessage = "§7使用指南：左键筛选/折叠分组，右键分组或卡片打开菜单，双击规则卡片可快速开关，滚轮可滚动右侧规则编辑器";
    private int statusColor = 0xFFB8C7D9;
    private final String[] editorSections = new String[] { "基础", "巡逻", "目标筛选", "评分调试", "高级" };
    private int activeEditorSection = 0;
    private int editorTabScrollOffset = 0;

    public GuiAutoFollowManager(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        applyReadableUiScaleForLargeScreen(1180, 660);

        recalcLayout();
        initEditorFields();
        initButtons();
        refreshData(false);
        layoutEditorFields();
    }

    private void recalcLayout() {
        panelWidth = Math.min(1140, this.width - 20);
        panelHeight = Math.min(620, this.height - 20);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        int sidePadding = 12;
        int columnGap = this.width < 960 ? 8 : 10;
        int availableWidth = panelWidth - sidePadding * 2;

        treeX = panelX + sidePadding;
        treeY = panelY + 46;
        treeHeight = panelHeight - 104;
        treeWidth = treeCollapsed
                ? getCollapsedSectionWidth("规则")
                : Math.max(122, Math.min(170, availableWidth / 6));

        listX = treeX + treeWidth + columnGap;
        listY = treeY;
        listHeight = treeHeight;
        listWidth = listCollapsed
                ? getCollapsedSectionWidth("卡片")
                : Math.max(190, Math.min(250, availableWidth / 4));

        int rightX = listX + listWidth + columnGap;
        editorX = rightX;
        editorY = treeY;
        editorWidth = panelX + panelWidth - sidePadding - rightX;
        editorHeight = treeHeight;

        editorLabelX = editorX + 10;
        editorFieldX = editorX + 92;
        editorRowStartY = editorY + 52;
        editorVisibleRows = Math.max(1, (editorHeight - 62) / EDITOR_ROW_HEIGHT);
    }

    private void initEditorFields() {
        nameField = createField(2001);
        categoryField = createField(2002);
        point1XField = createField(2003);
        point1ZField = createField(2004);
        point2XField = createField(2005);
        point2ZField = createField(2006);
        returnPointXField = createField(2007);
        returnPointZField = createField(2008);
        returnStayMillisField = createField(2009);
        returnArriveDistanceField = createField(2017);
        maxRecoveryDistanceField = createField(2010);
        monsterVerticalRangeField = createField(2011);
        monsterUpwardRangeField = createField(2012);
        monsterDownwardRangeField = createField(2013);
        monsterStopDistanceField = createField(2018);
        monsterFixedDistanceField = createField(2019);
        monsterEntityTypesField = createField(2020);
        monsterWhitelistField = createField(2014);
        monsterBlacklistField = createField(2015);
        lockChaseOutOfBoundsDistanceField = createField(2016);
        monsterEntityTypesField.setEnabled(false);
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private void initButtons() {
        int bottomY = panelY + panelHeight - 28;

        btnNew = new ThemedButton(BTN_NEW, treeX, bottomY, 72, 20, "新增");
        btnDelete = new ThemedButton(BTN_DELETE, treeX + 80, bottomY, 72, 20, "删除");

        btnSave = new ThemedButton(BTN_SAVE, panelX + panelWidth - 186, bottomY, 84, 20, "§a保存");
        btnDone = new ThemedButton(BTN_DONE, panelX + panelWidth - 94, bottomY, 84, 20, "完成");

        btnGetP1 = new ThemedButton(BTN_GET_P1, 0, 0, 56, 20, "取点1");
        btnGetP2 = new ThemedButton(BTN_GET_P2, 0, 0, 56, 20, "取点2");
        btnGetP3 = new ThemedButton(BTN_GET_P3, 0, 0, 68, 20, "取当前点");
        btnAddReturnPoint = new ThemedButton(BTN_ADD_RETURN_POINT, 0, 0, 68, 20, "添加回点");
        btnRemoveReturnPoint = new ThemedButton(BTN_REMOVE_RETURN_POINT, 0, 0, 68, 20, "删除选中");
        btnToggleActive = new ThemedButton(BTN_TOGGLE_ACTIVE, 0, 0, 100, 20, "");
        btnToggleOutOfRangeSequence = new ThemedButton(BTN_TOGGLE_OUT_OF_RANGE_SEQUENCE, 0, 0, 100, 20, "");
        btnToggleVisualize = new ThemedButton(BTN_TOGGLE_VISUALIZE, 0, 0, 100, 20, "");
        btnSelectOutOfRangeSequence = new ThemedButton(BTN_SELECT_OUT_OF_RANGE_SEQUENCE, 0, 0, 100, 20, "");
        btnTogglePatrolMode = new ThemedButton(BTN_TOGGLE_PATROL_MODE, 0, 0, 100, 20, "");
        btnToggleNameList = new ThemedButton(BTN_TOGGLE_NAME_LIST, 0, 0, 100, 20, "");
        btnToggleTargetInvisible = new ThemedButton(BTN_TOGGLE_TARGET_INVISIBLE, 0, 0, 100, 20, "");
        btnToggleTargetSpecial = null;
        btnToggleVisualizeLockChase = new ThemedButton(BTN_TOGGLE_VISUALIZE_LOCK_CHASE, 0, 0, 100, 20, "");
        btnToggleMonsterChaseMode = new ThemedButton(BTN_TOGGLE_MONSTER_CHASE_MODE, 0, 0, 100, 20, "");

        this.buttonList.add(btnNew);
        this.buttonList.add(btnDelete);
        this.buttonList.add(btnSave);
        this.buttonList.add(btnDone);
        this.buttonList.add(btnGetP1);
        this.buttonList.add(btnGetP2);
        this.buttonList.add(btnGetP3);
        this.buttonList.add(btnAddReturnPoint);
        this.buttonList.add(btnRemoveReturnPoint);
        this.buttonList.add(btnToggleActive);
        this.buttonList.add(btnToggleOutOfRangeSequence);
        this.buttonList.add(btnToggleVisualize);
        this.buttonList.add(btnSelectOutOfRangeSequence);
        this.buttonList.add(btnTogglePatrolMode);
        this.buttonList.add(btnToggleNameList);
        this.buttonList.add(btnToggleTargetInvisible);
        this.buttonList.add(btnToggleVisualizeLockChase);
        this.buttonList.add(btnToggleMonsterChaseMode);

        entityTypeButtons.clear();
        for (int i = 0; i < ENTITY_TYPE_OPTIONS.length; i++) {
            ToggleGuiButton button = new ToggleGuiButton(BTN_ENTITY_TYPE_BASE + i, 0, 0, 80, 20, "", false);
            entityTypeButtons.add(button);
            this.buttonList.add(button);
        }
    }

    private void layoutEditorFields() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(120, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);
        int pointButtonWidth = Math.min(84, fullFieldWidth);

        int returnSectionLeftWidth = getReturnSectionLeftWidth(fullFieldWidth);
        int returnHalfWidth = Math.max(60, (returnSectionLeftWidth - 10) / 2);
        int actionGap = 6;
        int actionWidth = Math.max(52, (returnSectionLeftWidth - actionGap * 2) / 3);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);

        placeField(point1XField, 2, editorFieldX, halfWidth);
        placeField(point1ZField, 2, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetP1, 3, editorFieldX, pointButtonWidth, 20);

        placeField(point2XField, 4, editorFieldX, halfWidth);
        placeField(point2ZField, 4, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetP2, 5, editorFieldX, pointButtonWidth, 20);

        placeField(returnPointXField, 6, editorFieldX, returnHalfWidth);
        placeField(returnPointZField, 6, editorFieldX + returnHalfWidth + 10, returnHalfWidth);
        placeButton(btnGetP3, 7, editorFieldX, actionWidth, 20);
        placeButton(btnAddReturnPoint, 7, editorFieldX + actionWidth + actionGap, actionWidth, 20);
        placeButton(btnRemoveReturnPoint, 7, editorFieldX + (actionWidth + actionGap) * 2, actionWidth, 20);

        placeField(returnStayMillisField, getReturnStayRow(), editorFieldX, returnSectionLeftWidth);
        placeField(returnArriveDistanceField, getReturnArriveRow(), editorFieldX, returnSectionLeftWidth);
        placeButton(btnTogglePatrolMode, getPatrolModeRow(), editorFieldX, returnSectionLeftWidth, 20);

        placeField(maxRecoveryDistanceField, getMaxRecoveryRow(), editorFieldX, halfWidth);
        placeField(monsterVerticalRangeField, getMonsterVerticalRangeRow(), editorFieldX, halfWidth);
        placeField(monsterUpwardRangeField, getMonsterUpwardRangeRow(), editorFieldX, halfWidth);
        placeField(monsterDownwardRangeField, getMonsterDownwardRangeRow(), editorFieldX, halfWidth);
        placeButton(btnToggleMonsterChaseMode, getMonsterChaseModeRow(), editorFieldX, fullFieldWidth, 20);
        if (isEditorMonsterFixedDistanceMode()) {
            placeField(monsterFixedDistanceField, getMonsterDistanceValueRow(), editorFieldX, halfWidth);
            hideField(monsterStopDistanceField);
        } else {
            placeField(monsterStopDistanceField, getMonsterDistanceValueRow(), editorFieldX, halfWidth);
            hideField(monsterFixedDistanceField);
        }
        placeField(monsterEntityTypesField, getMonsterEntityTypeSummaryRow(), editorFieldX, fullFieldWidth);
        int typeColumns = getEntityTypeGridColumns(fullFieldWidth);
        int typeGap = 6;
        int typeButtonWidth = Math.max(64, (fullFieldWidth - typeGap * (typeColumns - 1)) / typeColumns);
        int typeIndex = 0;
        for (int row = 0; row < getEntityTypeGridRowCount(); row++) {
            int editorRow = getMonsterEntityTypeStartRow() + row;
            for (int col = 0; col < typeColumns; col++) {
                if (typeIndex >= entityTypeButtons.size()) {
                    break;
                }
                int buttonX = editorFieldX + col * (typeButtonWidth + typeGap);
                placeButton(entityTypeButtons.get(typeIndex), editorRow, buttonX, typeButtonWidth, 20);
                typeIndex++;
            }
        }
        placeButton(btnToggleNameList, getEnableMonsterNameListRow(), editorFieldX, fullFieldWidth, 20);
        placeField(monsterWhitelistField, getMonsterWhitelistRow(), editorFieldX, fullFieldWidth);
        placeField(monsterBlacklistField, getMonsterBlacklistRow(), editorFieldX, fullFieldWidth);
        placeButton(btnToggleTargetInvisible, getTargetInvisibleMonstersRow(), editorFieldX, fullFieldWidth, 20);
        placeField(lockChaseOutOfBoundsDistanceField, getLockChaseOutOfBoundsRow(), editorFieldX, halfWidth);
        placeButton(btnToggleVisualizeLockChase, getVisualizeLockChaseRow(), editorFieldX, fullFieldWidth, 20);

        placeButton(btnToggleActive, getActiveRow(), editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleOutOfRangeSequence, getOutOfRangeSequenceToggleRow(), editorFieldX, fullFieldWidth, 20);
        placeButton(btnSelectOutOfRangeSequence, getSelectOutOfRangeSequenceRow(), editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleVisualize, getVisualizeRow(), editorFieldX, fullFieldWidth, 20);

        clampEditorScroll();
        clampReturnPointListScroll();
        updateButtonStates();
    }

    private int getReturnSectionLeftWidth(int fullFieldWidth) {
        int leftWidth = Math.min(260, Math.max(190, fullFieldWidth / 2));
        return Math.min(leftWidth, fullFieldWidth);
    }

    private void placeField(GuiTextField field, int row, int x, int width) {
        if (field == null) {
            return;
        }
        int y = getEditorRowY(row);
        field.x = x;
        field.width = width;
        field.height = 16;
        field.y = y >= 0 ? y : -2000;
    }

    private void hideField(GuiTextField field) {
        if (field == null) {
            return;
        }
        field.x = editorFieldX;
        field.y = -2000;
        field.width = Math.max(100, field.width);
        field.height = 16;
    }

    private void placeButton(GuiButton button, int row, int x, int width, int height) {
        if (button == null) {
            return;
        }
        int y = getEditorRowY(row);
        boolean visible = y >= 0;
        button.visible = visible;
        button.x = x;
        button.y = visible ? y - 2 : -2000;
        button.width = width;
        button.height = height;
        if (!visible) {
            button.enabled = false;
        }
    }

    private int getEditorRowY(int row) {
        int filteredIndex = getFilteredIndexForRow(row);
        if (filteredIndex < 0) {
            return -1;
        }
        int visibleIndex = filteredIndex - editorScrollOffset;
        if (visibleIndex < 0 || visibleIndex >= editorVisibleRows) {
            return -1;
        }
        return editorRowStartY + visibleIndex * EDITOR_ROW_HEIGHT;
    }

    private int getReturnListStartRow() {
        return 8;
    }

    private int getReturnHintRow() {
        return getReturnListStartRow() + RETURN_LIST_BOX_ROWS;
    }

    private int getReturnStayRow() {
        return getReturnHintRow() + 1;
    }

    private int getReturnArriveRow() {
        return getReturnStayRow() + 1;
    }

    private int getPatrolModeRow() {
        return getReturnArriveRow() + 1;
    }

    private int getMaxRecoveryRow() {
        return getPatrolModeRow() + 1;
    }

    private int getMonsterVerticalRangeRow() {
        return getMaxRecoveryRow() + 1;
    }

    private int getMonsterUpwardRangeRow() {
        return getMonsterVerticalRangeRow() + 1;
    }

    private int getMonsterDownwardRangeRow() {
        return getMonsterUpwardRangeRow() + 1;
    }

    private int getMonsterChaseModeRow() {
        return getMonsterDownwardRangeRow() + 1;
    }

    private int getMonsterDistanceValueRow() {
        return getMonsterChaseModeRow() + 1;
    }

    private int getMonsterStopDistanceRow() {
        return getMonsterDistanceValueRow();
    }

    private int getMonsterEntityTypeSummaryRow() {
        return getMonsterDistanceValueRow() + 1;
    }

    private int getMonsterEntityTypeStartRow() {
        return getMonsterEntityTypeSummaryRow() + 1;
    }

    private int getMonsterEntityTypeEndRow() {
        return getMonsterEntityTypeStartRow() + getEntityTypeGridRowCount() - 1;
    }

    private int getEnableMonsterNameListRow() {
        return getMonsterEntityTypeEndRow() + 1;
    }

    private int getMonsterWhitelistRow() {
        return getEnableMonsterNameListRow() + 1;
    }

    private int getMonsterBlacklistRow() {
        return getMonsterWhitelistRow() + 1;
    }

    private int getTargetInvisibleMonstersRow() {
        return getMonsterBlacklistRow() + 1;
    }

    private int getTargetSpecialMobsRow() {
        return -1;
    }

    private int getLockChaseOutOfBoundsRow() {
        return getTargetInvisibleMonstersRow() + 1;
    }

    private int getVisualizeLockChaseRow() {
        return getLockChaseOutOfBoundsRow() + 1;
    }

    private int getActiveRow() {
        return getVisualizeLockChaseRow() + 1;
    }

    private int getOutOfRangeSequenceToggleRow() {
        return getActiveRow() + 1;
    }

    private int getSelectOutOfRangeSequenceRow() {
        return getOutOfRangeSequenceToggleRow() + 1;
    }

    private int getVisualizeRow() {
        return getSelectOutOfRangeSequenceRow() + 1;
    }

    private int getMonsterScoreBoxStartRow() {
        return getVisualizeRow() + 1;
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
        int fullFieldWidth = Math.max(120, right - editorFieldX);
        int columns = getEntityTypeGridColumns(fullFieldWidth);
        return Math.max(1, (entityTypeButtons.size() + columns - 1) / columns);
    }

    private int getEffectiveEditorTotalRows() {
        return getRowsForActiveEditorSection().size();
    }

    private int getMaxEditorScroll() {
        return Math.max(0, getEffectiveEditorTotalRows() - editorVisibleRows);
    }

    private void clampEditorScroll() {
        editorScrollOffset = Math.max(0, Math.min(editorScrollOffset, getMaxEditorScroll()));
    }

    private int getReturnPointListVisibleCount() {
        int usableHeight = getReturnListBoxHeight() - 8;
        return Math.max(1, usableHeight / RETURN_LIST_ITEM_HEIGHT);
    }

    private int getReturnPointListMaxScroll() {
        return Math.max(0, editorReturnPoints.size() - getReturnPointListVisibleCount());
    }

    private void clampReturnPointListScroll() {
        returnPointListScrollOffset = Math.max(0, Math.min(returnPointListScrollOffset, getReturnPointListMaxScroll()));
    }

    private int getReturnListBoxX() {
        return editorFieldX;
    }

    private int getReturnListBoxY() {
        int rowY = getEditorRowY(getReturnListStartRow());
        return rowY < 0 ? -2000 : rowY - 2;
    }

    private int getReturnListBoxWidth() {
        int right = editorX + editorWidth - 14;
        return Math.max(120, right - editorFieldX);
    }

    private int getReturnListBoxHeight() {
        return RETURN_LIST_BOX_ROWS * EDITOR_ROW_HEIGHT - 6;
    }

    private boolean isInReturnPointListBox(int mouseX, int mouseY) {
        int x = getReturnListBoxX();
        int y = getReturnListBoxY();
        int width = getReturnListBoxWidth();
        int height = getReturnListBoxHeight();
        return y >= editorRowStartY
                && mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private int getEditorTabBarX() {
        return editorX + 8;
    }

    private int getEditorTabBarY() {
        return editorY + 24;
    }

    private int getEditorTabBarWidth() {
        return Math.max(80, editorWidth - 16);
    }

    private int getEditorTabBarHeight() {
        return 20;
    }

    private List<Integer> getRowsForActiveEditorSection() {
        List<Integer> rows = new ArrayList<>();
        int startRow;
        int endRow;

        switch (activeEditorSection) {
            case 0: // 基础
                startRow = 0;
                endRow = 5;
                break;
            case 1: // 巡逻
                startRow = 6;
                endRow = getMaxRecoveryRow();
                break;
            case 2: // 目标筛选
                startRow = getMonsterVerticalRangeRow();
                endRow = getVisualizeLockChaseRow();
                break;
            case 3: // 评分调试
                startRow = getMonsterScoreBoxStartRow();
                endRow = getMonsterScoreBoxStartRow() + MONSTER_SCORE_BOX_ROWS - 1;
                break;
            case 4: // 高级
            default:
                startRow = getActiveRow();
                endRow = getVisualizeRow();
                break;
        }

        for (int row = startRow; row <= endRow; row++) {
            rows.add(row);
        }
        return rows;
    }

    private int getFilteredIndexForRow(int row) {
        List<Integer> rows = getRowsForActiveEditorSection();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) == row) {
                return i;
            }
        }
        return -1;
    }

    private void jumpToEditorSection(int sectionIndex) {
        if (sectionIndex < 0 || sectionIndex >= editorSections.length) {
            return;
        }
        activeEditorSection = sectionIndex;
        editorScrollOffset = 0;
        layoutEditorFields();
    }

    private int getEditorTabArrowWidth() {
        return 18;
    }

    private int getEditorTabViewportX() {
        return getEditorTabBarX() + getEditorTabArrowWidth() + 2;
    }

    private int getEditorTabViewportWidth() {
        return Math.max(40, getEditorTabBarWidth() - getEditorTabArrowWidth() * 2 - 4);
    }

    private int getEditorTabsContentWidth() {
        int total = 0;
        for (String section : editorSections) {
            total += Math.max(56, this.fontRenderer.getStringWidth(section) + 18) + 6;
        }
        return Math.max(0, total - 6);
    }

    private void clampEditorTabScroll() {
        int max = Math.max(0, getEditorTabsContentWidth() - getEditorTabViewportWidth());
        editorTabScrollOffset = Math.max(0, Math.min(editorTabScrollOffset, max));
    }

    private boolean isInEditorTabBar(int mouseX, int mouseY) {
        return mouseX >= getEditorTabBarX() && mouseX <= getEditorTabBarX() + getEditorTabBarWidth()
                && mouseY >= getEditorTabBarY() && mouseY <= getEditorTabBarY() + getEditorTabBarHeight();
    }

    private boolean isInEditorTabLeftArrow(int mouseX, int mouseY) {
        return mouseX >= getEditorTabBarX()
                && mouseX <= getEditorTabBarX() + getEditorTabArrowWidth()
                && mouseY >= getEditorTabBarY()
                && mouseY <= getEditorTabBarY() + getEditorTabBarHeight();
    }

    private boolean isInEditorTabRightArrow(int mouseX, int mouseY) {
        int x = getEditorTabBarX() + getEditorTabBarWidth() - getEditorTabArrowWidth();
        return mouseX >= x
                && mouseX <= x + getEditorTabArrowWidth()
                && mouseY >= getEditorTabBarY()
                && mouseY <= getEditorTabBarY() + getEditorTabBarHeight();
    }

    private void drawEditorTabs(int mouseX, int mouseY) {
        int tabX = getEditorTabBarX();
        int tabY = getEditorTabBarY();
        int tabBarWidth = getEditorTabBarWidth();
        int tabBarHeight = getEditorTabBarHeight();
        int arrowWidth = getEditorTabArrowWidth();
        int viewportX = getEditorTabViewportX();
        int viewportWidth = getEditorTabViewportWidth();

        GuiTheme.drawButtonFrameSafe(tabX, tabY, tabBarWidth, tabBarHeight, GuiTheme.UiState.NORMAL);
        clampEditorTabScroll();

        boolean canScrollLeft = editorTabScrollOffset > 0;
        boolean canScrollRight = editorTabScrollOffset < Math.max(0, getEditorTabsContentWidth() - viewportWidth);

        GuiTheme.drawButtonFrameSafe(tabX + 1, tabY + 1, arrowWidth - 2, tabBarHeight - 2,
                isInEditorTabLeftArrow(mouseX, mouseY) ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawString(fontRenderer, "<", tabX + 6, tabY + 6, canScrollLeft ? 0xFFFFFFFF : 0xFF777777);

        int rightArrowX = tabX + tabBarWidth - arrowWidth;
        GuiTheme.drawButtonFrameSafe(rightArrowX + 1, tabY + 1, arrowWidth - 2, tabBarHeight - 2,
                isInEditorTabRightArrow(mouseX, mouseY) ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
        drawString(fontRenderer, ">", rightArrowX + 6, tabY + 6, canScrollRight ? 0xFFFFFFFF : 0xFF777777);

        drawRect(viewportX, tabY + 1, viewportX + viewportWidth, tabY + tabBarHeight - 1, 0x221A2533);

        int currentX = viewportX - editorTabScrollOffset;
        for (int i = 0; i < editorSections.length; i++) {
            String section = editorSections[i];
            int width = Math.max(56, this.fontRenderer.getStringWidth(section) + 18);
            boolean active = i == activeEditorSection;
            boolean hovered = mouseX >= currentX && mouseX <= currentX + width
                    && mouseY >= tabY && mouseY <= tabY + tabBarHeight;

            if (currentX >= viewportX && currentX + width <= viewportX + viewportWidth) {
                GuiTheme.drawButtonFrameSafe(currentX, tabY, width, tabBarHeight,
                        active ? GuiTheme.UiState.SELECTED
                                : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
                drawString(fontRenderer, section,
                        currentX + (width - this.fontRenderer.getStringWidth(section)) / 2,
                        tabY + 6, active ? 0xFFFFFFFF : 0xFFE8F1FA);
            }
            currentX += width + 6;
        }
    }

    private boolean handleEditorTabClick(int mouseX, int mouseY) {
        if (!isInEditorTabBar(mouseX, mouseY)) {
            return false;
        }

        if (isInEditorTabLeftArrow(mouseX, mouseY)) {
            editorTabScrollOffset -= 80;
            clampEditorTabScroll();
            return true;
        }
        if (isInEditorTabRightArrow(mouseX, mouseY)) {
            editorTabScrollOffset += 80;
            clampEditorTabScroll();
            return true;
        }

        int viewportX = getEditorTabViewportX();
        int viewportWidth = getEditorTabViewportWidth();
        int currentX = viewportX - editorTabScrollOffset;
        for (int i = 0; i < editorSections.length; i++) {
            int width = Math.max(56, this.fontRenderer.getStringWidth(editorSections[i]) + 18);
            if (currentX >= viewportX && currentX + width <= viewportX + viewportWidth
                    && mouseX >= currentX && mouseX <= currentX + width
                    && mouseY >= getEditorTabBarY() && mouseY <= getEditorTabBarY() + getEditorTabBarHeight()) {
                jumpToEditorSection(i);
                return true;
            }
            currentX += width + 6;
        }
        return false;
    }

    private int getMonsterScoreBoxX() {
        return editorFieldX;
    }

    private int getMonsterScoreBoxY() {
        int rowY = getEditorRowY(getMonsterScoreBoxStartRow());
        return rowY < 0 ? -2000 : rowY - 2;
    }

    private int getMonsterScoreBoxWidth() {
        int right = editorX + editorWidth - 14;
        return Math.max(120, right - editorFieldX);
    }

    private int getMonsterScoreBoxHeight() {
        return MONSTER_SCORE_BOX_ROWS * EDITOR_ROW_HEIGHT - 6;
    }

    private int getMonsterScoreVisibleCount() {
        int usableHeight = getMonsterScoreBoxHeight() - 8;
        return Math.max(1, usableHeight / MONSTER_SCORE_ITEM_HEIGHT);
    }

    private int getMonsterScoreMaxScroll() {
        return Math.max(0, AutoFollowHandler.getLastScoredMonstersSnapshot().size() - getMonsterScoreVisibleCount());
    }

    private void clampMonsterScoreScroll() {
        monsterScoreListScrollOffset = Math.max(0, Math.min(monsterScoreListScrollOffset, getMonsterScoreMaxScroll()));
    }

    private void scrollMonsterScoreList(int wheel) {
        int max = getMonsterScoreMaxScroll();
        if (max <= 0) {
            monsterScoreListScrollOffset = 0;
            return;
        }
        if (wheel < 0) {
            monsterScoreListScrollOffset = Math.min(max, monsterScoreListScrollOffset + 1);
        } else {
            monsterScoreListScrollOffset = Math.max(0, monsterScoreListScrollOffset - 1);
        }
        clampMonsterScoreScroll();
    }

    private boolean isInMonsterScoreBox(int mouseX, int mouseY) {
        int x = getMonsterScoreBoxX();
        int y = getMonsterScoreBoxY();
        int width = getMonsterScoreBoxWidth();
        int height = getMonsterScoreBoxHeight();
        return y >= editorRowStartY
                && mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private void refreshData(boolean keepSelection) {
        String keepRuleName = keepSelection ? getSelectedRuleName() : "";

        allRules.clear();
        allRules.addAll(AutoFollowHandler.rules);

        rebuildCategories();
        rebuildTreeRows();
        rebuildVisibleRules();

        if (!isBlank(keepRuleName) && selectByRuleName(keepRuleName)) {
            return;
        }

        if (visibleRules.isEmpty()) {
            selectedVisibleIndex = -1;
            creatingNew = true;
            clearEditorForNew();
            return;
        }

        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            selectedVisibleIndex = 0;
        }
        creatingNew = false;
        loadEditor(visibleRules.get(selectedVisibleIndex));
    }

    private void rebuildCategories() {
        categories.clear();
        LinkedHashSet<String> seen = new LinkedHashSet<>(AutoFollowHandler.getCategoriesSnapshot());
        for (AutoFollowRule rule : allRules) {
            seen.add(getRuleCategory(rule));
        }
        if (seen.isEmpty()) {
            seen.add(CATEGORY_DEFAULT);
        }
        categories.addAll(seen);
    }

    private void rebuildTreeRows() {
        visibleTreeRows.clear();
        visibleTreeRows.add(TreeRow.allRow("全部规则"));
        for (String category : categories) {
            visibleTreeRows.add(TreeRow.categoryRow(category));
            if (expandedCategories.contains(category)) {
                for (int ruleIndex = 0; ruleIndex < allRules.size(); ruleIndex++) {
                    AutoFollowRule rule = allRules.get(ruleIndex);
                    if (category.equals(getRuleCategory(rule))) {
                        visibleTreeRows.add(TreeRow.ruleRow(category, safe(rule.name), safe(rule.name), ruleIndex));
                    }
                }
            }
        }
        selectedTreeIndex = Math.max(0, Math.min(selectedTreeIndex, Math.max(0, visibleTreeRows.size() - 1)));
    }

    private void rebuildVisibleRules() {
        visibleRules.clear();
        for (AutoFollowRule rule : allRules) {
            if (CATEGORY_ALL.equals(selectedCategory) || selectedCategory.equals(getRuleCategory(rule))) {
                visibleRules.add(rule);
            }
        }

        int max = Math.max(0, visibleRules.size() - getVisibleCardCount());
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));
        if (selectedVisibleIndex >= visibleRules.size()) {
            selectedVisibleIndex = visibleRules.isEmpty() ? -1 : 0;
        }
    }

    private void clearEditorForNew() {
        AutoFollowRule draft = new AutoFollowRule();
        draft.category = isConcreteCategory(selectedCategory) ? selectedCategory : CATEGORY_DEFAULT;
        loadEditor(draft);
        creatingNew = true;
        editorScrollOffset = 0;
        layoutEditorFields();
    }

    private void loadEditor(AutoFollowRule rule) {
        AutoFollowRule model = rule == null ? new AutoFollowRule() : rule;
        model.updateBounds();
        model.ensureReturnPoints();

        setText(nameField, safe(model.name));
        setText(categoryField, getRuleCategory(model));
        setText(point1XField, formatDouble(model.point1 == null ? 0.0 : model.point1.x));
        setText(point1ZField, formatDouble(model.point1 == null ? 0.0 : model.point1.z));
        setText(point2XField, formatDouble(model.point2 == null ? 0.0 : model.point2.x));
        setText(point2ZField, formatDouble(model.point2 == null ? 0.0 : model.point2.z));

        AutoFollowHandler.Point primaryReturnPoint = model.getPrimaryReturnPoint();
        setText(returnPointXField, formatDouble(primaryReturnPoint == null ? 0.0 : primaryReturnPoint.x));
        setText(returnPointZField, formatDouble(primaryReturnPoint == null ? 0.0 : primaryReturnPoint.z));
        setText(returnStayMillisField, String.valueOf(model.returnStayMillis > 0
                ? model.returnStayMillis
                : AutoFollowRule.DEFAULT_RETURN_STAY_MILLIS));
        setText(returnArriveDistanceField, formatDouble(model.returnArriveDistance > 0
                ? model.returnArriveDistance
                : AutoFollowRule.DEFAULT_RETURN_ARRIVE_DISTANCE));
        setText(maxRecoveryDistanceField, formatDouble(model.maxRecoveryDistance));
        setText(monsterVerticalRangeField, formatDouble(model.monsterVerticalRange > 0
                ? model.monsterVerticalRange
                : AutoFollowRule.DEFAULT_MONSTER_VERTICAL_RANGE));
        setText(monsterUpwardRangeField, formatDouble(model.monsterUpwardRange > 0
                ? model.monsterUpwardRange
                : AutoFollowRule.DEFAULT_MONSTER_UPWARD_RANGE));
        setText(monsterDownwardRangeField, formatDouble(model.monsterDownwardRange > 0
                ? model.monsterDownwardRange
                : AutoFollowRule.DEFAULT_MONSTER_DOWNWARD_RANGE));
        editorMonsterChaseMode = AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE.equalsIgnoreCase(model.monsterChaseMode)
                ? AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE
                : AutoFollowRule.MONSTER_CHASE_MODE_APPROACH;
        setText(monsterStopDistanceField, formatDouble(model.monsterStopDistance > 0
                ? model.monsterStopDistance
                : AutoFollowRule.DEFAULT_MONSTER_STOP_DISTANCE));
        setText(monsterFixedDistanceField, formatDouble(model.monsterFixedDistance > 0
                ? model.monsterFixedDistance
                : AutoFollowRule.DEFAULT_MONSTER_FIXED_DISTANCE));
        setEditorEntityTypes(model.entityTypes);
        setText(monsterEntityTypesField, buildEntityTypeSummary());
        setText(monsterWhitelistField, joinNameList(model.monsterWhitelistNames));
        setText(monsterBlacklistField, joinNameList(model.monsterBlacklistNames));
        setText(lockChaseOutOfBoundsDistanceField, formatDouble(model.lockChaseOutOfBoundsDistance > 0
                ? model.lockChaseOutOfBoundsDistance
                : AutoFollowRule.DEFAULT_LOCK_CHASE_OUT_OF_BOUNDS_DISTANCE));

        editorReturnPoints.clear();
        if (model.returnPoints != null) {
            for (AutoFollowHandler.Point point : model.returnPoints) {
                if (point != null) {
                    editorReturnPoints.add(AutoFollowRule.copyPoint(point));
                }
            }
        }
        if (editorReturnPoints.isEmpty() && primaryReturnPoint != null) {
            editorReturnPoints.add(AutoFollowRule.copyPoint(primaryReturnPoint));
        }

        selectedReturnPointIndices.clear();
        returnPointSelectionAnchor = editorReturnPoints.isEmpty() ? -1 : 0;
        if (!editorReturnPoints.isEmpty()) {
            selectedReturnPointIndices.add(0);
        }

        editorActive = model.enabled;
        editorRunSequenceWhenOutOfRecoveryRange = model.runSequenceWhenOutOfRecoveryRange;
        editorOutOfRangeSequenceName = safe(model.outOfRangeSequenceName);
        editorVisualizeRange = model.visualizeRange;
        editorVisualizeLockChaseRadius = model.visualizeLockChaseRadius;
        editorPatrolMode = AutoFollowRule.PATROL_MODE_RANDOM.equalsIgnoreCase(model.patrolMode)
                ? AutoFollowRule.PATROL_MODE_RANDOM
                : AutoFollowRule.PATROL_MODE_ORDER;
        editorEnableMonsterNameList = model.enableMonsterNameList;
        editorTargetInvisibleMonsters = model.targetInvisibleMonsters;
        editorTargetSpecialMobs = model.targetSpecialMobs;

        if (activeEditorSection < 0 || activeEditorSection >= editorSections.length) {
            activeEditorSection = 0;
        }
        clampEditorTabScroll();
        returnPointListScrollOffset = 0;
        monsterScoreListScrollOffset = 0;
        clampEditorScroll();
        clampReturnPointListScroll();
        clampMonsterScoreScroll();
        layoutEditorFields();
    }

    private void updateButtonStates() {
        btnToggleActive.displayString = "当前激活: " + boolText(editorActive);
        btnToggleOutOfRangeSequence.displayString = "超过最大范围后执行序列: "
                + yesNo(editorRunSequenceWhenOutOfRecoveryRange);
        String seqText = isBlank(editorOutOfRangeSequenceName) ? "点击选择序列" : "§f" + editorOutOfRangeSequenceName;
        btnSelectOutOfRangeSequence.displayString = trimToWidth(seqText,
                Math.max(40, btnSelectOutOfRangeSequence.width - 12));
        btnToggleVisualize.displayString = "显示范围框: " + yesNo(editorVisualizeRange);
        btnToggleVisualizeLockChase.displayString = "显示锁定追击超距半径: " + yesNo(editorVisualizeLockChaseRadius);
        btnTogglePatrolMode.displayString = "巡逻模式: " + getPatrolModeDisplay(editorPatrolMode);
        btnToggleMonsterChaseMode.displayString = "追怪模式: " + getMonsterChaseModeDisplay(editorMonsterChaseMode);
        btnToggleNameList.displayString = "启用怪物名称黑白名单: " + yesNo(editorEnableMonsterNameList);
        btnToggleTargetInvisible.displayString = "追隐形怪: " + yesNo(editorTargetInvisibleMonsters);
        setText(monsterEntityTypesField, buildEntityTypeSummary());
        for (int i = 0; i < entityTypeButtons.size() && i < ENTITY_TYPE_OPTIONS.length; i++) {
            ToggleGuiButton button = entityTypeButtons.get(i);
            String token = ENTITY_TYPE_OPTIONS[i][0];
            button.displayString = ENTITY_TYPE_OPTIONS[i][1];
            button.setEnabledState(editorEntityTypes.contains(token));
            button.enabled = button.visible;
        }

        boolean hasSelected = !creatingNew && selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleRules.size();
        btnDelete.enabled = hasSelected;
        btnSave.enabled = true;

        btnGetP1.enabled = btnGetP1.visible;
        btnGetP2.enabled = btnGetP2.visible;
        normalizeReturnPointSelection();

        btnGetP3.enabled = btnGetP3.visible;
        btnAddReturnPoint.enabled = btnAddReturnPoint.visible;
        btnRemoveReturnPoint.enabled = btnRemoveReturnPoint.visible && !selectedReturnPointIndices.isEmpty();
        btnTogglePatrolMode.enabled = btnTogglePatrolMode.visible;
        btnToggleMonsterChaseMode.enabled = btnToggleMonsterChaseMode.visible;
        btnToggleNameList.enabled = btnToggleNameList.visible;
        btnToggleTargetInvisible.enabled = btnToggleTargetInvisible.visible;
        btnToggleVisualizeLockChase.enabled = btnToggleVisualizeLockChase.visible;
        btnToggleActive.enabled = btnToggleActive.visible;
        btnToggleOutOfRangeSequence.enabled = btnToggleOutOfRangeSequence.visible;
        btnSelectOutOfRangeSequence.enabled = btnSelectOutOfRangeSequence.visible
                && editorRunSequenceWhenOutOfRecoveryRange;
        btnToggleVisualize.enabled = btnToggleVisualize.visible;
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
        return joinNameList(labels);
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
        return joinNameList(labels);
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

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DONE) {
            AutoFollowHandler.saveFollowConfig();
            if (parentScreen instanceof GuiPathManager) {
                ((GuiPathManager) parentScreen).requestReloadFromManager();
            }
            mc.displayGuiScreen(parentScreen);
            return;
        }
        if (button.id == BTN_NEW) {
            creatingNew = true;
            selectedVisibleIndex = -1;
            clearEditorForNew();
            setStatus("§a已进入新建规则模式", 0xFF8CFF9E);
            return;
        }
        if (button.id == BTN_DELETE) {
            deleteSelectedRule();
            return;
        }
        if (button.id == BTN_SAVE) {
            saveCurrentRule();
            return;
        }
        if (button.id == BTN_GET_P1) {
            updatePointFromPlayer(point1XField, point1ZField);
            return;
        }
        if (button.id == BTN_GET_P2) {
            updatePointFromPlayer(point2XField, point2ZField);
            return;
        }
        if (button.id == BTN_GET_P3) {
            updatePointFromPlayer(returnPointXField, returnPointZField);
            return;
        }
        if (button.id == BTN_ADD_RETURN_POINT) {
            addReturnPointFromInputs();
            return;
        }
        if (button.id == BTN_REMOVE_RETURN_POINT) {
            beginDeleteSelectedReturnPoints();
            return;
        }
        if (button.id == BTN_TOGGLE_PATROL_MODE) {
            editorPatrolMode = AutoFollowRule.PATROL_MODE_RANDOM.equalsIgnoreCase(editorPatrolMode)
                    ? AutoFollowRule.PATROL_MODE_ORDER
                    : AutoFollowRule.PATROL_MODE_RANDOM;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_MONSTER_CHASE_MODE) {
            editorMonsterChaseMode = AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE.equalsIgnoreCase(editorMonsterChaseMode)
                    ? AutoFollowRule.MONSTER_CHASE_MODE_APPROACH
                    : AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE;
            layoutEditorFields();
            updateButtonStates();
            return;
        }
        if (button.id >= BTN_ENTITY_TYPE_BASE && button.id < BTN_ENTITY_TYPE_BASE + ENTITY_TYPE_OPTIONS.length) {
            String token = ENTITY_TYPE_OPTIONS[button.id - BTN_ENTITY_TYPE_BASE][0];
            if (editorEntityTypes.contains(token)) {
                editorEntityTypes.remove(token);
            } else {
                editorEntityTypes.add(token);
            }
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_NAME_LIST) {
            editorEnableMonsterNameList = !editorEnableMonsterNameList;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_TARGET_INVISIBLE) {
            editorTargetInvisibleMonsters = !editorTargetInvisibleMonsters;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_VISUALIZE_LOCK_CHASE) {
            editorVisualizeLockChaseRadius = !editorVisualizeLockChaseRadius;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_ACTIVE) {
            editorActive = !editorActive;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_TOGGLE_OUT_OF_RANGE_SEQUENCE) {
            editorRunSequenceWhenOutOfRecoveryRange = !editorRunSequenceWhenOutOfRecoveryRange;
            updateButtonStates();
            return;
        }
        if (button.id == BTN_SELECT_OUT_OF_RANGE_SEQUENCE) {
            mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                editorOutOfRangeSequenceName = safe(seq);
                mc.displayGuiScreen(this);
            }));
            return;
        }
        if (button.id == BTN_TOGGLE_VISUALIZE) {
            editorVisualizeRange = !editorVisualizeRange;
            updateButtonStates();
            return;
        }

        super.actionPerformed(button);
    }

    private void saveCurrentRule() {
        try {
            AutoFollowRule model = buildRuleFromEditor();
            if (isBlank(model.name)) {
                setStatus("§c规则名称不能为空", 0xFFFF8E8E);
                return;
            }

            if (creatingNew) {
                if (editorActive) {
                    for (AutoFollowRule rule : AutoFollowHandler.rules) {
                        rule.enabled = false;
                    }
                }
                AutoFollowHandler.rules.add(model);
                creatingNew = false;
                if (editorActive) {
                    AutoFollowHandler.setActiveRule(model);
                } else {
                    AutoFollowHandler.saveFollowConfig();
                }
                refreshData(true);
                selectByRuleName(model.name);
                setStatus("§a已新增规则: " + model.name, 0xFF8CFF9E);
                return;
            }

            if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
                setStatus("§c请先选择一条规则", 0xFFFF8E8E);
                return;
            }

            AutoFollowRule target = visibleRules.get(selectedVisibleIndex);
            applyRuleValues(target, model);

            if (editorActive) {
                AutoFollowHandler.setActiveRule(target);
            } else {
                if (target.enabled) {
                    target.enabled = false;
                    if (AutoFollowHandler.getActiveRule() == target) {
                        AutoFollowHandler.setActiveRule(null);
                    } else {
                        AutoFollowHandler.saveFollowConfig();
                    }
                } else {
                    AutoFollowHandler.saveFollowConfig();
                }
            }

            refreshData(true);
            selectByRuleName(target.name);
            setStatus("§a规则已保存", 0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c保存失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void deleteSelectedRule() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先选择一条规则", 0xFFFF8E8E);
            return;
        }

        AutoFollowRule target = visibleRules.get(selectedVisibleIndex);
        if (AutoFollowHandler.getActiveRule() == target) {
            AutoFollowHandler.setActiveRule(null);
        }
        AutoFollowHandler.rules.remove(target);
        AutoFollowHandler.saveFollowConfig();
        refreshData(false);
        setStatus("§a已删除规则: " + safe(target.name), 0xFF8CFF9E);
    }

    private void duplicateSelectedRule() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }

        AutoFollowRule source = visibleRules.get(selectedVisibleIndex);
        AutoFollowRule copy = copyRule(source);
        copy.name = safe(source.name).trim().isEmpty() ? "规则副本" : safe(source.name).trim() + " 副本";
        copy.enabled = false;
        if (!isConcreteCategory(copy.category)) {
            copy.category = isConcreteCategory(selectedCategory) ? selectedCategory : CATEGORY_DEFAULT;
        }

        AutoFollowHandler.rules.add(copy);
        AutoFollowHandler.saveFollowConfig();
        refreshData(true);
        selectByRuleName(copy.name);
        setStatus("§a已复制规则: " + copy.name, 0xFF8CFF9E);
    }

    private void copySelectedRuleName() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }
        AutoFollowRule rule = visibleRules.get(selectedVisibleIndex);
        setClipboardString(safe(rule.name));
        setStatus("§a已复制规则名: " + safe(rule.name), 0xFF8CFF9E);
    }

    private void moveSelectedRuleToCurrentCategory() {
        if (!isConcreteCategory(selectedCategory)) {
            setStatus("§c请先在左侧选择一个具体分组", 0xFFFF8E8E);
            return;
        }
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            setStatus("§c请先右键选择一条规则卡片", 0xFFFF8E8E);
            return;
        }

        AutoFollowRule rule = visibleRules.get(selectedVisibleIndex);
        rule.category = selectedCategory;
        AutoFollowHandler.saveFollowConfig();
        refreshData(true);
        selectByRuleName(rule.name);
        setStatus("§a已移动规则到分组: " + selectedCategory, 0xFF8CFF9E);
    }

    private void reloadAllRules() {
        AutoFollowHandler.loadFollowConfig();
        refreshData(false);
        setStatus("§a已重载自动追怪规则", 0xFF8CFF9E);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        resetTreeDragState();

        if (mouseButton == 2) {
            handleMiddleClickReturnPointAction();
            return;
        }

        if (contextMenuVisible) {
            if (handleContextMenuClick(mouseX, mouseY, mouseButton)) {
                return;
            }
            closeContextMenu();
        }

        if (mouseButton == 0) {
            if (isInTreeCollapseButton(mouseX, mouseY)) {
                treeCollapsed = !treeCollapsed;
                recalcLayout();
                layoutEditorFields();
                return;
            }
            if (isInListCollapseButton(mouseX, mouseY)) {
                listCollapsed = !listCollapsed;
                recalcLayout();
                layoutEditorFields();
                return;
            }
        }

        if (!listCollapsed && mouseButton == 1 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleRules.size()) {
                selectedVisibleIndex = actual;
                creatingNew = false;
                loadEditor(visibleRules.get(actual));
                openContextMenu(mouseX, mouseY, actual);
                return;
            }
            openContextMenu(mouseX, mouseY, -1);
            return;
        }

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            if (mouseButton == 0) {
                int actualIndex = getTreeRowIndexAt(mouseY);
                if (actualIndex >= 0 && actualIndex < visibleTreeRows.size()) {
                    pendingTreePressIndex = actualIndex;
                    pendingTreePressMouseX = mouseX;
                    pendingTreePressMouseY = mouseY;
                }
                return;
            }
            if (mouseButton == 1) {
                handleTreeRightClick(mouseX, mouseY);
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (!listCollapsed && mouseButton == 0 && isInCardList(mouseX, mouseY)) {
            int actual = getCardIndexAt(mouseY);
            if (actual >= 0 && actual < visibleRules.size()) {
                long now = System.currentTimeMillis();
                boolean isDoubleClick = actual == lastClickedCardIndex
                        && (now - lastClickedCardTimeMs) <= CARD_DOUBLE_CLICK_WINDOW_MS;

                selectedVisibleIndex = actual;
                creatingNew = false;
                loadEditor(visibleRules.get(actual));

                if (isDoubleClick) {
                    toggleRuleEnabledFromCard(visibleRules.get(actual));
                    lastClickedCardIndex = -1;
                    lastClickedCardTimeMs = 0L;
                } else {
                    lastClickedCardIndex = actual;
                    lastClickedCardTimeMs = now;
                }
            } else {
                lastClickedCardIndex = -1;
                lastClickedCardTimeMs = 0L;
            }
            return;
        }

        if (mouseButton == 0 && handleEditorTabClick(mouseX, mouseY)) {
            return;
        }

        if (isInReturnPointListBox(mouseX, mouseY) && mouseButton == 0) {
            handleReturnPointListSelectionClick(mouseX, mouseY);
            return;
        }

        GuiTextField focusedField = null;
        for (GuiTextField field : visibleFields()) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
            if (mouseX >= field.x && mouseX < field.x + field.width
                    && mouseY >= field.y && mouseY < field.y + field.height) {
                focusedField = field;
            }
        }

        for (GuiTextField field : allFields()) {
            field.setFocused(field == focusedField);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            if (treeDragging) {
                completeTreeDrag();
                resetTreeDragState();
                return;
            }
            if (pendingTreePressIndex >= 0) {
                handleTreeClickByIndex(pendingTreePressIndex);
                pendingTreePressIndex = -1;
                return;
            }
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0 && pendingTreePressIndex >= 0 && !treeDragging) {
            if (Math.abs(mouseX - pendingTreePressMouseX) >= 4 || Math.abs(mouseY - pendingTreePressMouseY) >= 4) {
                TreeRow row = pendingTreePressIndex >= 0 && pendingTreePressIndex < visibleTreeRows.size()
                        ? visibleTreeRows.get(pendingTreePressIndex)
                        : null;
                activeDragPayload = buildDragPayload(row);
                treeDragging = activeDragPayload != null;
                currentTreeDropTarget = treeDragging ? computeTreeDropTarget(mouseX, mouseY, activeDragPayload) : null;
            }
        } else if (treeDragging) {
            currentTreeDropTarget = computeTreeDropTarget(mouseX, mouseY, activeDragPayload);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void handleTreeClick(int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }
        handleTreeClickByIndex(actualIndex);
    }

    private void handleTreeClickByIndex(int actualIndex) {
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }
        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (row.type == TreeRow.TYPE_ALL) {
            selectedCategory = CATEGORY_ALL;
            rebuildVisibleRules();
            refreshSelectionAfterFilter();
            return;
        }

        if (row.type == TreeRow.TYPE_CATEGORY) {
            if (expandedCategories.contains(row.category)) {
                expandedCategories.remove(row.category);
            } else {
                expandedCategories.add(row.category);
            }
            selectedCategory = row.category;
            rebuildTreeRows();
            rebuildVisibleRules();
            refreshSelectionAfterFilter();
            return;
        }

        if (row.type == TreeRow.TYPE_RULE) {
            selectedCategory = row.category;
            rebuildVisibleRules();
            selectByRuleName(row.ruleName);
        }
    }

    private void handleTreeRightClick(int mouseX, int mouseY) {
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return;
        }

        selectedTreeIndex = actualIndex;
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (row.type == TreeRow.TYPE_RULE) {
            setStatus("§7左侧规则子项暂不支持独立菜单，请右键中间规则卡片", 0xFFB8C7D9);
            return;
        }

        if (row.type == TreeRow.TYPE_CATEGORY) {
            selectedCategory = safe(row.category);
            rebuildVisibleRules();
        }

        openTreeContextMenu(mouseX, mouseY, row);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (isInEditorTabBar(mouseX, mouseY)) {
            if (wheel < 0) {
                editorTabScrollOffset += 60;
            } else {
                editorTabScrollOffset -= 60;
            }
            clampEditorTabScroll();
            return;
        }

        if (isInReturnPointListBox(mouseX, mouseY)) {
            if (wheel < 0) {
                returnPointListScrollOffset = Math.min(getReturnPointListMaxScroll(), returnPointListScrollOffset + 1);
            } else {
                returnPointListScrollOffset = Math.max(0, returnPointListScrollOffset - 1);
            }
            clampReturnPointListScroll();
            return;
        }

        if (isInMonsterScoreBox(mouseX, mouseY)) {
            scrollMonsterScoreList(wheel);
            return;
        }

        if (activeEditorSection == 3 && isInEditor(mouseX, mouseY)) {
            scrollMonsterScoreList(wheel);
            return;
        }

        if (isInEditor(mouseX, mouseY)) {
            if (wheel < 0) {
                editorScrollOffset = Math.min(getMaxEditorScroll(), editorScrollOffset + 1);
            } else {
                editorScrollOffset = Math.max(0, editorScrollOffset - 1);
            }
            layoutEditorFields();
            return;
        }

        if (!treeCollapsed && isInTree(mouseX, mouseY)) {
            int max = Math.max(0, visibleTreeRows.size() - getVisibleTreeRowCount());
            treeScrollOffset = wheel < 0 ? Math.min(max, treeScrollOffset + 1) : Math.max(0, treeScrollOffset - 1);
            return;
        }

        if (!listCollapsed && isInCardList(mouseX, mouseY)) {
            int max = Math.max(0, visibleRules.size() - getVisibleCardCount());
            listScrollOffset = wheel < 0 ? Math.min(max, listScrollOffset + 1) : Math.max(0, listScrollOffset - 1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (contextMenuVisible) {
                closeContextMenu();
                return;
            }
            AutoFollowHandler.loadFollowConfig();
            if (parentScreen instanceof GuiPathManager) {
                ((GuiPathManager) parentScreen).requestReloadFromManager();
            }
            mc.displayGuiScreen(parentScreen);
            return;
        }

        boolean consumed = false;
        for (GuiTextField field : allFields()) {
            if (field.isFocused() && field.textboxKeyTyped(typedChar, keyCode)) {
                consumed = true;
                break;
            }
        }
        if (!consumed) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : allFields()) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mouseX = toReadableMouseX(mouseX);
        mouseY = toReadableMouseY(mouseY);
        pushReadableUiScale();
        try {
            drawDefaultBackground();

            GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
            GuiTheme.drawTitleBar(panelX, panelY, panelWidth,
                    I18n.format("gui.autofollow.manager.title"), this.fontRenderer);

            GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 22, panelWidth - 20, 18, false, true);
            drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 27, statusColor);

            drawTreePanel(mouseX, mouseY);
            drawCardPanel(mouseX, mouseY);
            drawEditorPanel(mouseX, mouseY);

            super.drawScreen(mouseX, mouseY, partialTicks);

            if (treeDragging) {
                drawTreeDragOverlay(mouseX, mouseY);
            }

            if (contextMenuVisible) {
                drawContextMenu(mouseX, mouseY);
            } else {
                List<String> hoverLines = getCardHoverTooltipLines(mouseX, mouseY);
                if ((hoverLines == null || hoverLines.isEmpty())) {
                    hoverLines = getEditorHoverTooltipLines(mouseX, mouseY);
                }
                if (hoverLines != null && !hoverLines.isEmpty()) {
                    drawHoveringText(hoverLines, mouseX, mouseY);
                }
            }
        } finally {
            popReadableUiScale();
        }
    }

    private void drawTreePanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(treeX, treeY, treeWidth, treeHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(treeX + 8, treeY + 8, treeCollapsed ? "规则" : "规则树（右键打开菜单）", this.fontRenderer);
        drawCollapseButton(treeX, treeY, treeWidth, treeCollapsed);
        if (treeCollapsed) {
            return;
        }

        int contentY = treeY + 22;
        int visibleRows = getVisibleTreeRowCount();
        int max = Math.max(0, visibleTreeRows.size() - visibleRows);
        treeScrollOffset = Math.max(0, Math.min(treeScrollOffset, max));

        if (visibleTreeRows.isEmpty()) {
            GuiTheme.drawEmptyState(treeX + treeWidth / 2, treeY + treeHeight / 2, "暂无规则", this.fontRenderer);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = treeScrollOffset + i;
            if (index >= visibleTreeRows.size()) {
                break;
            }

            TreeRow row = visibleTreeRows.get(index);
            int rowY = contentY + i * TREE_ROW_HEIGHT;
            boolean selected = index == selectedTreeIndex
                    || (row.type == TreeRow.TYPE_CATEGORY && row.category.equals(selectedCategory))
                    || (row.type == TreeRow.TYPE_ALL && CATEGORY_ALL.equals(selectedCategory));
            boolean hovered = mouseX >= treeX + 6 && mouseX <= treeX + treeWidth - 10
                    && mouseY >= rowY && mouseY <= rowY + TREE_ROW_HEIGHT - 2;

            GuiTheme.drawButtonFrameSafe(treeX + 6, rowY, treeWidth - 14, TREE_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            int textX = treeX + 10 + row.indent * 12;
            int maxTextWidth = Math.max(32, treeWidth - (textX - treeX) - 18);

            if (row.type == TreeRow.TYPE_CATEGORY) {
                String arrow = expandedCategories.contains(row.category) ? "▼" : "▶";
                drawString(fontRenderer, arrow, textX, rowY + 5, 0xFF9FDFFF);
                drawString(fontRenderer, trimToWidth(row.label, maxTextWidth - 12), textX + 10, rowY + 5,
                        0xFFE8F1FA);
            } else if (row.type == TreeRow.TYPE_RULE) {
                drawString(fontRenderer, "•", textX, rowY + 5, 0xFFB8C7D9);
                drawString(fontRenderer, trimToWidth(row.label, maxTextWidth - 8), textX + 8, rowY + 5,
                        0xFFD9E6F2);
            } else {
                drawString(fontRenderer, trimToWidth(row.label, maxTextWidth), textX, rowY + 5, 0xFFE8F1FA);
            }
        }

        if (treeDragging && currentTreeDropTarget != null) {
            drawTreeDropIndicator(currentTreeDropTarget);
        }

        if (visibleTreeRows.size() > visibleRows) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, visibleTreeRows.size())) * (treeHeight - 28)));
            int track = Math.max(1, (treeHeight - 28) - thumbHeight);
            int thumbY = contentY + (int) ((treeScrollOffset / (float) Math.max(1, max)) * track);
            GuiTheme.drawScrollbar(treeX + treeWidth - 8, contentY, 4, treeHeight - 28, thumbY, thumbHeight);
        }
    }

    private void drawCardPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(listX, listY, listWidth, listHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(listX + 8, listY + 8, listCollapsed ? "卡片" : "规则卡片（右键打开菜单）", this.fontRenderer);
        drawCollapseButton(listX, listY, listWidth, listCollapsed);
        if (listCollapsed) {
            return;
        }

        int contentY = listY + 22;
        int visible = getVisibleCardCount();
        int max = Math.max(0, visibleRules.size() - visible);
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, max));

        if (visibleRules.isEmpty()) {
            GuiTheme.drawEmptyState(listX + listWidth / 2, listY + listHeight / 2, "该分组下暂无规则", this.fontRenderer);
            return;
        }

        for (int i = 0; i < visible; i++) {
            int actual = listScrollOffset + i;
            if (actual >= visibleRules.size()) {
                break;
            }

            AutoFollowRule rule = visibleRules.get(actual);
            rule.updateBounds();
            rule.ensureReturnPoints();

            int cardTop = contentY + i * (CARD_HEIGHT + CARD_GAP);
            int cardBottom = cardTop + CARD_HEIGHT;
            boolean selected = actual == selectedVisibleIndex;
            boolean hovered = mouseX >= listX + 2 && mouseX <= listX + listWidth - 2
                    && mouseY >= cardTop && mouseY <= cardBottom;

            int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
            int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

            drawRect(listX + 2, cardTop, listX + listWidth - 2, cardBottom, bg);
            drawHorizontalLine(listX + 2, listX + listWidth - 2, cardTop, border);
            drawHorizontalLine(listX + 2, listX + listWidth - 2, cardBottom, border);
            drawVerticalLine(listX + 2, cardTop, cardBottom, border);
            drawVerticalLine(listX + listWidth - 2, cardTop, cardBottom, border);

            String status = rule.enabled ? "§a✔" : "§7○";
            drawString(fontRenderer, trimToWidth(status + " " + safe(rule.name), listWidth - 18),
                    listX + 8, cardTop + 5, 0xFFFFFFFF);
            drawString(fontRenderer,
                    trimToWidth("分类: " + getRuleCategory(rule), listWidth - 18),
                    listX + 8, cardTop + 19, 0xFFDDDDDD);
            drawString(fontRenderer,
                    trimToWidth("点1(" + formatDouble(rule.point1 == null ? 0 : rule.point1.x) + ", "
                                    + formatDouble(rule.point1 == null ? 0 : rule.point1.z) + ")  点2("
                                    + formatDouble(rule.point2 == null ? 0 : rule.point2.x) + ", "
                                    + formatDouble(rule.point2 == null ? 0 : rule.point2.z) + ")",
                            listWidth - 18),
                    listX + 8, cardTop + 31, 0xFFBDBDBD);

            String chaseDistanceText = AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE.equalsIgnoreCase(rule.monsterChaseMode)
                    ? "定距:" + formatDouble(rule.monsterFixedDistance > 0
                    ? rule.monsterFixedDistance
                    : AutoFollowRule.DEFAULT_MONSTER_FIXED_DISTANCE)
                    : "追距:" + formatDouble(rule.monsterStopDistance > 0
                    ? rule.monsterStopDistance
                    : AutoFollowRule.DEFAULT_MONSTER_STOP_DISTANCE);
            String bottomLeftText = getReturnPointSummary(rule) + "  最大:" + formatDouble(rule.maxRecoveryDistance)
                    + "  " + chaseDistanceText;
            String distanceText = "距最近回点: " + getDistanceToReturnPointText(rule);
            int distanceWidth = this.fontRenderer.getStringWidth(distanceText);
            int bottomLeftMaxWidth = Math.max(40, listWidth - 26 - distanceWidth - 8);

            drawString(fontRenderer,
                    trimToWidth(bottomLeftText, bottomLeftMaxWidth),
                    listX + 8, cardTop + 43, 0xFFB8C7D9);
            drawString(fontRenderer, distanceText,
                    listX + listWidth - 8 - distanceWidth, cardTop + 43, 0xFF9FDFFF);
        }

        if (visibleRules.size() > visible) {
            int thumbHeight = Math.max(18,
                    (int) ((visible / (float) Math.max(visible, visibleRules.size())) * (listHeight - 28)));
            int track = Math.max(1, (listHeight - 28) - thumbHeight);
            int thumbY = contentY + (int) ((listScrollOffset / (float) Math.max(1, max)) * track);
            GuiTheme.drawScrollbar(listX + listWidth - 8, contentY, 4, listHeight - 28, thumbY, thumbHeight);
        }
    }

    private void drawEditorPanel(int mouseX, int mouseY) {
        GuiTheme.drawPanelSegment(editorX, editorY, editorWidth, editorHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(editorX + 8, editorY + 8,
                creatingNew ? "规则编辑器 - 新建" : "规则编辑器 - 编辑", this.fontRenderer);

        layoutEditorFields();
        drawEditorTabs(mouseX, mouseY);
        drawReturnPointListBox();
        drawReturnPointHintLine();
        drawMonsterScoreListBox();

        List<Integer> sectionRows = getRowsForActiveEditorSection();
        int visibleStart = editorScrollOffset;
        int visibleEnd = Math.min(sectionRows.size(), visibleStart + editorVisibleRows);
        for (int i = visibleStart; i < visibleEnd; i++) {
            int row = sectionRows.get(i);
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }
            drawLabel(editorLabelX, y + 4, getRowLabel(row));
        }

        for (GuiTextField field : visibleFields()) {
            drawThemedTextField(field);
        }

        if (getEffectiveEditorTotalRows() > editorVisibleRows) {
            int thumbHeight = Math.max(18,
                    (int) ((editorVisibleRows / (float) getEffectiveEditorTotalRows()) * (editorHeight - 12)));
            int track = Math.max(1, (editorHeight - 12) - thumbHeight);
            int thumbY = editorY + 6 + (int) ((editorScrollOffset / (float) Math.max(1, getMaxEditorScroll())) * track);
            GuiTheme.drawScrollbar(editorX + editorWidth - 8, editorY + 6, 4, editorHeight - 12, thumbY, thumbHeight);
        }
    }

    private List<String> getEditorHoverTooltipLines(int mouseX, int mouseY) {
        if (!isInEditor(mouseX, mouseY)) {
            return null;
        }

        String text = null;
        if (isInEditorTabBar(mouseX, mouseY)) {
            text = "分组标签：左右两侧箭头或滚轮可横向滚动，确保所有分组都能完整显示；点击后只显示对应分组内容。";
        } else {
            List<Integer> rows = getRowsForActiveEditorSection();
            for (Integer row : rows) {
                int y = getEditorRowY(row);
                if (y >= 0 && mouseY >= y - 2 && mouseY <= y + 18) {
                    text = getRowDescription(row);
                    break;
                }
            }

            if (text == null && isInReturnPointListBox(mouseX, mouseY)) {
                text = "已添加回点列表：这里显示当前规则的所有回点；支持 Ctrl 多选、Shift 连选，并通过“删除选中”移除。";
            }
            if (text == null && isInMonsterScoreBox(mouseX, mouseY)) {
                text = "怪物评分列表：动态显示范围内候选怪物的总分和各项子分，用于观察当前选怪策略。";
            }
        }

        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return this.fontRenderer.listFormattedStringToWidth(text, 260);
    }

    private String getRowDescription(int row) {
        switch (row) {
            case 0:
                return "规则名：当前自动追怪规则的名称，用于在规则列表中区分不同配置。";
            case 1:
                return "所属分组：将规则归类到左侧分组树，便于按用途或地图进行管理。";
            case 2:
                return "范围点1：圈定自动追怪区域的第一个角点坐标。";
            case 3:
                return "范围点1操作：使用当前位置快速记录范围点1。";
            case 4:
                return "范围点2：圈定自动追怪区域的第二个角点坐标，与范围点1共同形成追怪区域。";
            case 5:
                return "范围点2操作：使用当前位置快速记录范围点2。";
            case 6:
                return "回归点输入：填写一个回归点坐标，用于巡逻或战斗结束后的回归。回归点现在不是必填。";
            case 7:
                return "回归点操作：可取当前点、添加回点、删除选中的回点。只有手动添加时才会真正写入回点。";
        }
        if (row == getReturnListStartRow()) {
            return "已添加回点：显示当前规则已经手动添加的回点列表；为空时不会自动补点。";
        }
        if (row == getReturnHintRow()) {
            return "提示：展示回点编辑和选择操作的使用说明。";
        }
        if (row == getReturnStayRow()) {
            return "停留毫秒：到达回点或随机巡逻点后停留的时间；如果没有回点，也会按这个时间在范围内随机巡逻。";
        }
        if (row == getReturnArriveRow()) {
            return "到达归点范围：距离当前回点或随机巡逻点在多少格以内时，判定为已到达并开始停留。默认1。";
        }
        if (row == getPatrolModeRow()) {
            return "巡逻模式：有多个回点时，可选择按添加顺序巡逻或回点间随机巡逻。";
        }
        if (row == getMaxRecoveryRow()) {
            return "最大恢复距离：玩家距离当前回归点过远时，自动追怪逻辑会暂停，避免跑得太散。";
        }
        if (row == getMonsterVerticalRangeRow()) {
            return "兼容垂直范围：旧版兼容字段，建议主要使用下面的向上/向下追击范围。";
        }
        if (row == getMonsterUpwardRangeRow()) {
            return "向上追击范围：以玩家为基准，允许追击头顶上方多少格以内的目标。";
        }
        if (row == getMonsterDownwardRangeRow()) {
            return "向下追击范围：以玩家为基准，允许追击脚下方多少格以内的目标。";
        }
        if (row == getMonsterChaseModeRow()) {
            return "追怪模式：追击模式会靠近到指定追击距离内，固定距离模式会尽量和目标保持固定半径。";
        }
        if (row == getMonsterStopDistanceRow()) {
            return isEditorMonsterFixedDistanceMode()
                    ? "固定距离：固定距离模式下，自动追怪会尽量和怪物保持这个距离。默认3。"
                    : "追击距离：距离目标怪物小于等于多少格时停止继续贴近，避免角色过于贴脸。默认1.1。";
        }
        if (row == getMonsterEntityTypeSummaryRow()) {
            return "已选实体类型：这里会汇总当前勾选的目标实体类型。默认会包含怪物，并补上首领/傀儡等常见战斗目标。";
        }
        if (row >= getMonsterEntityTypeStartRow() && row <= getMonsterEntityTypeEndRow()) {
            return "实体类型：直接点击下面的类型按钮进行多选。目标筛选不再只限于 IMob，可按怪物、首领、傀儡、玩家等细分。";
        }
        if (row == getEnableMonsterNameListRow()) {
            return "名称黑白名单：开启后，会按和杀戮光环相同的名称规范化逻辑匹配实体显示名/名称。";
        }
        if (row == getMonsterWhitelistRow()) {
            return "白名单名称：多个关键字用逗号分隔；填写后仅追击名称中包含这些关键字的实体。";
        }
        if (row == getMonsterBlacklistRow()) {
            return "黑名单名称：多个关键字用逗号分隔；名称命中的实体会被排除，不参与追击。";
        }
        if (row == getTargetInvisibleMonstersRow()) {
            return "追隐形怪：开启后，隐身怪物也会被纳入目标筛选和评分。";
        }
        if (row == getLockChaseOutOfBoundsRow()) {
            return "锁定追击超距停止：怪物先在范围内被锁定后，即使被打出范围，也会继续追击，直到超出范围边界超过该距离才停止。默认10。";
        }
        if (row == getVisualizeLockChaseRow()) {
            return "显示锁定追击超距半径：在世界中显示‘锁定追击超距停止’的有效边界，便于观察杀戮与自动追怪共用的限制墙。";
        }
        if (row == getActiveRow()) {
            return "启用：将当前规则设为自动追怪的活动规则；若杀戮光环当前正在使用自己的追怪，则这里不会抢占它的移动控制。";
        }
        if (row == getOutOfRangeSequenceToggleRow()) {
            return "超距执行序列：当玩家离当前回归点超过最大恢复距离时，可选择执行一个额外序列。";
        }
        if (row == getSelectOutOfRangeSequenceRow()) {
            return "选择超距序列：为超出最大恢复距离后的行为指定具体路径序列。";
        }
        if (row == getVisualizeRow()) {
            return "显示范围框：在游戏世界中显示巡逻区域和回归点范围框，方便校对。";
        }
        if (row == getMonsterScoreBoxStartRow()) {
            return "怪物评分：调试当前目标选择策略，查看怪物在距离、可见性、可达性、高差、锁定等维度上的得分。";
        }
        return "说明：当前条目的功能说明暂未单独定义。";
    }

    private void drawReturnPointListBox() {
        int boxY = getReturnListBoxY();
        if (boxY < 0) {
            return;
        }

        int boxX = getReturnListBoxX();
        int boxWidth = getReturnListBoxWidth();
        int boxHeight = getReturnListBoxHeight();

        GuiTheme.drawButtonFrameSafe(boxX, boxY, boxWidth, boxHeight, GuiTheme.UiState.NORMAL);

        int innerX = boxX + 4;
        int innerY = boxY + 4;
        int innerWidth = boxWidth - 12;
        int visibleCount = getReturnPointListVisibleCount();

        if (editorReturnPoints.isEmpty()) {
            drawString(fontRenderer,
                    trimToWidth("暂无回归点，使用上方坐标并点击“添加回点”", innerWidth - 4),
                    innerX + 2, innerY + 2, 0xFFB8C7D9);
            return;
        }

        clampReturnPointListScroll();

        for (int i = 0; i < visibleCount; i++) {
            int actualIndex = returnPointListScrollOffset + i;
            if (actualIndex >= editorReturnPoints.size()) {
                break;
            }

            AutoFollowHandler.Point point = editorReturnPoints.get(actualIndex);
            boolean selected = selectedReturnPointIndices.contains(actualIndex);

            int itemTop = innerY + i * RETURN_LIST_ITEM_HEIGHT;
            int itemBottom = itemTop + RETURN_LIST_ITEM_HEIGHT - 2;

            int bg = selected ? 0xAA255D8A : 0x99222222;
            int border = selected ? 0xFF5FB8FF : 0xFF4B4B4B;

            drawRect(innerX, itemTop, innerX + innerWidth - 4, itemBottom, bg);
            drawHorizontalLine(innerX, innerX + innerWidth - 4, itemTop, border);
            drawHorizontalLine(innerX, innerX + innerWidth - 4, itemBottom, border);
            drawVerticalLine(innerX, itemTop, itemBottom, border);
            drawVerticalLine(innerX + innerWidth - 4, itemTop, itemBottom, border);

            String prefix = selected ? "选 " : "";
            String text = prefix + (actualIndex + 1) + ". (" + formatDouble(point.x) + ", " + formatDouble(point.z) + ")";
            int color = actualIndex == 0 ? 0xFF8CFF9E : 0xFFE8F1FA;
            drawString(fontRenderer, trimToWidth(text, innerWidth - 14), innerX + 5, itemTop + 5, color);
        }

        if (editorReturnPoints.size() > visibleCount) {
            int barX = boxX + boxWidth - 6;
            int barY = boxY + 2;
            int barHeight = boxHeight - 4;
            int thumbHeight = Math.max(18,
                    (int) ((visibleCount / (float) Math.max(visibleCount, editorReturnPoints.size())) * barHeight));
            int track = Math.max(1, barHeight - thumbHeight);
            int thumbY = barY + (int) ((returnPointListScrollOffset / (float) Math.max(1, getReturnPointListMaxScroll())) * track);
            GuiTheme.drawScrollbar(barX, barY, 4, barHeight, thumbY, thumbHeight);
        }
    }

    private void drawReturnPointHintLine() {
        int y = getEditorRowY(getReturnHintRow());
        if (y < 0) {
            return;
        }
        int width = Math.max(120, (editorX + editorWidth - 14) - editorFieldX);
        String text = "提示：支持 Ctrl 多选、Shift 连续多选；中键对准范围内方块可添加回点；删除请在列表中选中后点击“删除选中”。";
        int color = 0xFFB8C7D9;
        drawString(fontRenderer, trimToWidth(text, width), editorFieldX, y + 4, color);
    }

    private void drawMonsterScoreListBox() {
        int boxY = getMonsterScoreBoxY();
        if (boxY < 0) {
            return;
        }

        int boxX = getMonsterScoreBoxX();
        int boxWidth = getMonsterScoreBoxWidth();
        int boxHeight = getMonsterScoreBoxHeight();
        List<AutoFollowHandler.ScoredMonsterInfo> scores = AutoFollowHandler.getLastScoredMonstersSnapshot();

        GuiTheme.drawButtonFrameSafe(boxX, boxY, boxWidth, boxHeight, GuiTheme.UiState.NORMAL);

        int innerX = boxX + 4;
        int innerY = boxY + 4;
        int innerWidth = boxWidth - 12;
        int visibleCount = getMonsterScoreVisibleCount();

        if (scores.isEmpty()) {
            drawString(fontRenderer,
                    trimToWidth("当前范围内暂无可评分怪物", innerWidth - 4),
                    innerX + 2, innerY + 2, 0xFFB8C7D9);
            return;
        }

        clampMonsterScoreScroll();

        for (int i = 0; i < visibleCount; i++) {
            int actualIndex = monsterScoreListScrollOffset + i;
            if (actualIndex >= scores.size()) {
                break;
            }

            AutoFollowHandler.ScoredMonsterInfo info = scores.get(actualIndex);
            boolean top = actualIndex == 0;

            int itemTop = innerY + i * MONSTER_SCORE_ITEM_HEIGHT;
            int itemBottom = itemTop + MONSTER_SCORE_ITEM_HEIGHT - 2;

            int bg = top ? 0xAA255D8A : 0x99222222;
            int border = top ? 0xFF5FB8FF : 0xFF4B4B4B;

            drawRect(innerX, itemTop, innerX + innerWidth - 4, itemBottom, bg);
            drawHorizontalLine(innerX, innerX + innerWidth - 4, itemTop, border);
            drawHorizontalLine(innerX, innerX + innerWidth - 4, itemBottom, border);
            drawVerticalLine(innerX, itemTop, itemBottom, border);
            drawVerticalLine(innerX + innerWidth - 4, itemTop, itemBottom, border);

            int titleColor = top ? 0xFF8CFF9E : 0xFFE8F1FA;
            int subColor = top ? 0xFFE8FFF0 : 0xFFB8C7D9;

            String line1 = (top ? "当前目标候选 " : "")
                    + safe(info.name)
                    + "  总分:" + formatDouble(info.totalScore);
            String line2 = "距离:" + formatDouble(info.distance)
                    + "  高差:" + formatDouble(info.verticalDiff)
                    + "  视线:" + yesNo(info.visible)
                    + "  可达:" + yesNo(info.reachable);
            String line3 = "距分:" + formatDouble(info.distanceScore)
                    + "  视分:" + formatDouble(info.visibilityScore)
                    + "  路分:" + formatDouble(info.reachabilityScore);
            String line4 = "高分:" + formatDouble(info.verticalScore)
                    + "  锁分:" + formatDouble(info.lockBonusScore);

            drawString(fontRenderer, trimToWidth(line1, innerWidth - 14), innerX + 5, itemTop + 4, titleColor);
            drawString(fontRenderer, line2, innerX + 5, itemTop + 13, subColor);
            drawString(fontRenderer, line3, innerX + 5, itemTop + 22, subColor);
            drawString(fontRenderer, line4, innerX + 5, itemTop + 31, subColor);
        }

        if (scores.size() > visibleCount) {
            int barX = boxX + boxWidth - 6;
            int barY = boxY + 2;
            int barHeight = boxHeight - 4;
            int thumbHeight = Math.max(18,
                    (int) ((visibleCount / (float) Math.max(visibleCount, scores.size())) * barHeight));
            int track = Math.max(1, barHeight - thumbHeight);
            int thumbY = barY + (int) ((monsterScoreListScrollOffset / (float) Math.max(1, getMonsterScoreMaxScroll())) * track);
            GuiTheme.drawScrollbar(barX, barY, 4, barHeight, thumbY, thumbHeight);
        }
    }

    private void drawLabel(int x, int y, String text) {
        drawString(fontRenderer, text, x, y, GuiTheme.SUB_TEXT);
    }

    private String getRowLabel(int row) {
        if (row == 0) {
            return "规则名";
        }
        if (row == 1) {
            return "所属分组";
        }
        if (row == 2) {
            return "范围点1";
        }
        if (row == 3) {
            return "范围点1操作";
        }
        if (row == 4) {
            return "范围点2";
        }
        if (row == 5) {
            return "范围点2操作";
        }
        if (row == 6) {
            return "回归点输入";
        }
        if (row == 7) {
            return "回归点操作";
        }

        int returnListStartRow = getReturnListStartRow();
        int returnStayRow = getReturnStayRow();
        if (row == returnListStartRow) {
            return "已添加回点";
        }
        if (row > returnListStartRow && row < getReturnHintRow()) {
            return "";
        }
        if (row == getReturnHintRow()) {
            return "提示";
        }
        if (row == returnStayRow) {
            return "停留毫秒";
        }
        if (row == getReturnArriveRow()) {
            return "到达归点范围";
        }
        if (row == getPatrolModeRow()) {
            return "巡逻模式";
        }
        if (row == getMaxRecoveryRow()) {
            return "最大恢复距离";
        }
        if (row == getMonsterVerticalRangeRow()) {
            return "兼容垂直范围";
        }
        if (row == getMonsterUpwardRangeRow()) {
            return "向上追击范围";
        }
        if (row == getMonsterDownwardRangeRow()) {
            return "向下追击范围";
        }
        if (row == getMonsterChaseModeRow()) {
            return "追怪模式";
        }
        if (row == getMonsterStopDistanceRow()) {
            return isEditorMonsterFixedDistanceMode() ? "固定距离" : "追击距离";
        }
        if (row == getMonsterEntityTypeSummaryRow()) {
            return "已选实体类型";
        }
        if (row >= getMonsterEntityTypeStartRow() && row <= getMonsterEntityTypeEndRow()) {
            return row == getMonsterEntityTypeStartRow() ? "实体类型" : "";
        }
        if (row == getEnableMonsterNameListRow()) {
            return "名称黑白名单";
        }
        if (row == getMonsterWhitelistRow()) {
            return "白名单名称";
        }
        if (row == getMonsterBlacklistRow()) {
            return "黑名单名称";
        }
        if (row == getTargetInvisibleMonstersRow()) {
            return "追隐形怪";
        }
        if (row == getLockChaseOutOfBoundsRow()) {
            return "锁定追击超距停止";
        }
        if (row == getVisualizeLockChaseRow()) {
            return "显示锁定追击超距半径";
        }
        if (row == getActiveRow()) {
            return "启用";
        }
        if (row == getOutOfRangeSequenceToggleRow()) {
            return "超距执行序列";
        }
        if (row == getSelectOutOfRangeSequenceRow()) {
            return "选择超距序列";
        }
        if (row == getVisualizeRow()) {
            return "显示范围框";
        }
        if (row == getMonsterScoreBoxStartRow()) {
            return "怪物评分";
        }
        if (row > getMonsterScoreBoxStartRow()) {
            return "";
        }
        return "";
    }

    private List<GuiTextField> allFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(point1XField);
        fields.add(point1ZField);
        fields.add(point2XField);
        fields.add(point2ZField);
        fields.add(returnPointXField);
        fields.add(returnPointZField);
        fields.add(returnStayMillisField);
        fields.add(returnArriveDistanceField);
        fields.add(maxRecoveryDistanceField);
        fields.add(monsterVerticalRangeField);
        fields.add(monsterUpwardRangeField);
        fields.add(monsterDownwardRangeField);
        fields.add(monsterStopDistanceField);
        fields.add(monsterFixedDistanceField);
        fields.add(monsterEntityTypesField);
        fields.add(monsterWhitelistField);
        fields.add(monsterBlacklistField);
        fields.add(lockChaseOutOfBoundsDistanceField);
        return fields;
    }

    private List<GuiTextField> visibleFields() {
        List<GuiTextField> fields = new ArrayList<>();
        for (GuiTextField field : allFields()) {
            if (field != null && field.y >= editorRowStartY
                    && field.y < editorRowStartY + editorVisibleRows * EDITOR_ROW_HEIGHT) {
                fields.add(field);
            }
        }
        return fields;
    }

    private AutoFollowRule buildRuleFromEditor() {
        AutoFollowRule base = creatingNew ? new AutoFollowRule()
                : (selectedVisibleIndex >= 0 && selectedVisibleIndex < visibleRules.size()
                ? copyRule(visibleRules.get(selectedVisibleIndex))
                : new AutoFollowRule());

        base.name = safe(nameField.getText()).trim();
        base.category = safe(categoryField.getText()).trim();
        if (base.category.isEmpty()) {
            base.category = CATEGORY_DEFAULT;
        }
        base.enabled = editorActive;
        base.runSequenceWhenOutOfRecoveryRange = editorRunSequenceWhenOutOfRecoveryRange;
        base.outOfRangeSequenceName = safe(editorOutOfRangeSequenceName).trim();
        base.visualizeRange = editorVisualizeRange;
        base.visualizeLockChaseRadius = editorVisualizeLockChaseRadius;

        if (base.point1 == null) {
            base.point1 = new AutoFollowHandler.Point(0, 0);
        }
        if (base.point2 == null) {
            base.point2 = new AutoFollowHandler.Point(0, 0);
        }
        if (base.point3 == null) {
            base.point3 = new AutoFollowHandler.Point(0, 0);
        }

        base.point1.x = parseDouble(point1XField.getText(), base.point1.x);
        base.point1.z = parseDouble(point1ZField.getText(), base.point1.z);
        base.point2.x = parseDouble(point2XField.getText(), base.point2.x);
        base.point2.z = parseDouble(point2ZField.getText(), base.point2.z);
        base.maxRecoveryDistance = parseDouble(maxRecoveryDistanceField.getText(), base.maxRecoveryDistance);
        base.monsterVerticalRange = Math.max(0.1, parseDouble(monsterVerticalRangeField.getText(),
                base.monsterVerticalRange > 0 ? base.monsterVerticalRange : AutoFollowRule.DEFAULT_MONSTER_VERTICAL_RANGE));
        base.monsterUpwardRange = Math.max(0.1, parseDouble(monsterUpwardRangeField.getText(),
                base.monsterUpwardRange > 0 ? base.monsterUpwardRange : AutoFollowRule.DEFAULT_MONSTER_UPWARD_RANGE));
        base.monsterDownwardRange = Math.max(0.1, parseDouble(monsterDownwardRangeField.getText(),
                base.monsterDownwardRange > 0 ? base.monsterDownwardRange : AutoFollowRule.DEFAULT_MONSTER_DOWNWARD_RANGE));
        base.monsterChaseMode = isEditorMonsterFixedDistanceMode()
                ? AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE
                : AutoFollowRule.MONSTER_CHASE_MODE_APPROACH;
        base.monsterStopDistance = Math.max(0.1, parseDouble(monsterStopDistanceField.getText(),
                base.monsterStopDistance > 0 ? base.monsterStopDistance : AutoFollowRule.DEFAULT_MONSTER_STOP_DISTANCE));
        base.monsterFixedDistance = Math.max(0.1, parseDouble(monsterFixedDistanceField.getText(),
                base.monsterFixedDistance > 0 ? base.monsterFixedDistance : AutoFollowRule.DEFAULT_MONSTER_FIXED_DISTANCE));
        base.entityTypes = new ArrayList<>(editorEntityTypes);
        base.enableMonsterNameList = editorEnableMonsterNameList;
        base.monsterWhitelistNames = parseNameList(monsterWhitelistField.getText());
        base.monsterBlacklistNames = parseNameList(monsterBlacklistField.getText());
        base.targetInvisibleMonsters = editorTargetInvisibleMonsters;
        base.targetSpecialMobs = editorEntityTypes.contains(AutoFollowRule.ENTITY_TYPE_BOSS)
                || editorEntityTypes.contains(AutoFollowRule.ENTITY_TYPE_GOLEM);
        base.lockChaseOutOfBoundsDistance = Math.max(0.1, parseDouble(lockChaseOutOfBoundsDistanceField.getText(),
                base.lockChaseOutOfBoundsDistance > 0
                        ? base.lockChaseOutOfBoundsDistance
                        : AutoFollowRule.DEFAULT_LOCK_CHASE_OUT_OF_BOUNDS_DISTANCE));
        base.returnStayMillis = Math.max(1, parseInt(returnStayMillisField.getText(),
                base.returnStayMillis > 0 ? base.returnStayMillis : AutoFollowRule.DEFAULT_RETURN_STAY_MILLIS));
        base.returnArriveDistance = Math.max(0.1, parseDouble(returnArriveDistanceField.getText(),
                base.returnArriveDistance > 0
                        ? base.returnArriveDistance
                        : AutoFollowRule.DEFAULT_RETURN_ARRIVE_DISTANCE));
        base.patrolMode = AutoFollowRule.PATROL_MODE_RANDOM.equalsIgnoreCase(editorPatrolMode)
                ? AutoFollowRule.PATROL_MODE_RANDOM
                : AutoFollowRule.PATROL_MODE_ORDER;
        base.updateBounds();

        List<AutoFollowHandler.Point> sanitizedReturnPoints = new ArrayList<>();
        boolean validateReturnPointBounds = isReturnPointBoundsValidationReady();
        for (AutoFollowHandler.Point point : editorReturnPoints) {
            if (point == null) {
                continue;
            }
            if (validateReturnPointBounds && !base.isPointWithinBounds(point)) {
                throw new IllegalArgumentException("存在回归点超出范围点1和范围点2");
            }
            sanitizedReturnPoints.add(AutoFollowRule.copyPoint(point));
        }

        base.returnPoints = sanitizedReturnPoints;
        base.ensureReturnPoints();
        return base;
    }

    private AutoFollowRule copyRule(AutoFollowRule src) {
        AutoFollowRule copy = new AutoFollowRule();
        if (src == null) {
            return copy;
        }
        copy.name = src.name;
        copy.category = src.category;
        copy.enabled = src.enabled;
        copy.point1 = new AutoFollowHandler.Point(src.point1 == null ? 0.0 : src.point1.x,
                src.point1 == null ? 0.0 : src.point1.z);
        copy.point2 = new AutoFollowHandler.Point(src.point2 == null ? 0.0 : src.point2.x,
                src.point2 == null ? 0.0 : src.point2.z);
        copy.point3 = new AutoFollowHandler.Point(src.point3 == null ? 0.0 : src.point3.x,
                src.point3 == null ? 0.0 : src.point3.z);
        copy.returnPoints = copyReturnPoints(src.returnPoints);
        copy.returnStayMillis = src.returnStayMillis;
        copy.returnArriveDistance = src.returnArriveDistance;
        copy.patrolMode = src.patrolMode;
        copy.monsterVerticalRange = src.monsterVerticalRange;
        copy.monsterUpwardRange = src.monsterUpwardRange;
        copy.monsterDownwardRange = src.monsterDownwardRange;
        copy.monsterChaseMode = src.monsterChaseMode;
        copy.monsterStopDistance = src.monsterStopDistance;
        copy.monsterFixedDistance = src.monsterFixedDistance;
        copy.entityTypes = src.entityTypes == null ? new ArrayList<>() : new ArrayList<>(src.entityTypes);
        copy.enableMonsterNameList = src.enableMonsterNameList;
        copy.monsterWhitelistNames = src.monsterWhitelistNames == null ? new ArrayList<>() : new ArrayList<>(src.monsterWhitelistNames);
        copy.monsterBlacklistNames = src.monsterBlacklistNames == null ? new ArrayList<>() : new ArrayList<>(src.monsterBlacklistNames);
        copy.targetInvisibleMonsters = src.targetInvisibleMonsters;
        copy.targetSpecialMobs = src.targetSpecialMobs;
        copy.lockChaseOutOfBoundsDistance = src.lockChaseOutOfBoundsDistance;
        copy.maxRecoveryDistance = src.maxRecoveryDistance;
        copy.runSequenceWhenOutOfRecoveryRange = src.runSequenceWhenOutOfRecoveryRange;
        copy.outOfRangeSequenceName = src.outOfRangeSequenceName;
        copy.visualizeRange = src.visualizeRange;
        copy.visualizeLockChaseRadius = src.visualizeLockChaseRadius;
        copy.updateBounds();
        copy.ensureReturnPoints();
        return copy;
    }

    private void applyRuleValues(AutoFollowRule target, AutoFollowRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = source.category;
        target.enabled = source.enabled;
        target.point1 = source.point1;
        target.point2 = source.point2;
        target.point3 = source.point3;
        target.returnPoints = copyReturnPoints(source.returnPoints);
        target.returnStayMillis = source.returnStayMillis;
        target.returnArriveDistance = source.returnArriveDistance;
        target.patrolMode = source.patrolMode;
        target.monsterVerticalRange = source.monsterVerticalRange;
        target.monsterUpwardRange = source.monsterUpwardRange;
        target.monsterDownwardRange = source.monsterDownwardRange;
        target.monsterChaseMode = source.monsterChaseMode;
        target.monsterStopDistance = source.monsterStopDistance;
        target.monsterFixedDistance = source.monsterFixedDistance;
        target.entityTypes = source.entityTypes == null ? new ArrayList<>() : new ArrayList<>(source.entityTypes);
        target.enableMonsterNameList = source.enableMonsterNameList;
        target.monsterWhitelistNames = source.monsterWhitelistNames == null ? new ArrayList<>() : new ArrayList<>(source.monsterWhitelistNames);
        target.monsterBlacklistNames = source.monsterBlacklistNames == null ? new ArrayList<>() : new ArrayList<>(source.monsterBlacklistNames);
        target.targetInvisibleMonsters = source.targetInvisibleMonsters;
        target.targetSpecialMobs = source.targetSpecialMobs;
        target.lockChaseOutOfBoundsDistance = source.lockChaseOutOfBoundsDistance;
        target.maxRecoveryDistance = source.maxRecoveryDistance;
        target.runSequenceWhenOutOfRecoveryRange = source.runSequenceWhenOutOfRecoveryRange;
        target.outOfRangeSequenceName = source.outOfRangeSequenceName;
        target.visualizeRange = source.visualizeRange;
        target.visualizeLockChaseRadius = source.visualizeLockChaseRadius;
        target.updateBounds();
        target.ensureReturnPoints();
    }

    private boolean selectByRuleName(String name) {
        if (isBlank(name)) {
            return false;
        }

        for (AutoFollowRule rule : allRules) {
            if (name.equalsIgnoreCase(safe(rule.name))) {
                selectedCategory = getRuleCategory(rule);
                rebuildTreeRows();
                rebuildVisibleRules();

                for (int i = 0; i < visibleRules.size(); i++) {
                    if (name.equalsIgnoreCase(safe(visibleRules.get(i).name))) {
                        selectedVisibleIndex = i;
                        creatingNew = false;
                        loadEditor(visibleRules.get(i));
                        ensureCardSelectionVisible();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getSelectedRuleName() {
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            return "";
        }
        return safe(visibleRules.get(selectedVisibleIndex).name);
    }

    private String getRuleCategory(AutoFollowRule rule) {
        if (rule == null) {
            return CATEGORY_DEFAULT;
        }
        String category = safe(rule.category).trim();
        return category.isEmpty() ? CATEGORY_DEFAULT : category;
    }

    private void refreshSelectionAfterFilter() {
        if (visibleRules.isEmpty()) {
            creatingNew = true;
            clearEditorForNew();
            return;
        }
        if (selectedVisibleIndex < 0 || selectedVisibleIndex >= visibleRules.size()) {
            selectedVisibleIndex = 0;
        }
        creatingNew = false;
        loadEditor(visibleRules.get(selectedVisibleIndex));
    }

    private void toggleRuleEnabledFromCard(AutoFollowRule rule) {
        if (rule == null) {
            return;
        }

        String ruleName = safe(rule.name);
        boolean willEnable = !rule.enabled;

        if (willEnable) {
            AutoFollowHandler.setActiveRule(rule);
            setStatus("§a已快速启用规则: " + ruleName, 0xFF8CFF9E);
        } else {
            AutoFollowHandler.setActiveRule(null);
            setStatus("§e已快速关闭规则: " + ruleName, 0xFFFFD27F);
        }

        refreshData(true);
        selectByRuleName(ruleName);
    }

    private List<String> getCardHoverTooltipLines(int mouseX, int mouseY) {
        if (listCollapsed || !isInCardList(mouseX, mouseY)) {
            return null;
        }
        int actual = getCardIndexAt(mouseY);
        if (actual < 0 || actual >= visibleRules.size()) {
            return null;
        }

        AutoFollowRule rule = visibleRules.get(actual);
        List<String> lines = new ArrayList<>();
        lines.add("§e规则卡片");
        lines.add("§7单击：选中并加载到右侧编辑器");
        lines.add("§7右键：打开规则菜单");
        lines.add("§7双击：快速" + (rule != null && rule.enabled ? "关闭" : "启用") + "该规则");
        if (rule != null) {
            lines.add("§7当前状态：" + (rule.enabled ? "§a已启用" : "§c未启用"));
            lines.add("§7实体类型：" + (rule.entityTypes == null || rule.entityTypes.isEmpty()
                    ? "默认怪物"
                    : joinEntityTypeLabels(rule.entityTypes)));
            if (rule.enableMonsterNameList) {
                lines.add("§7名称筛选：§a已启用");
            }
        }
        return lines;
    }

    private void updatePointFromPlayer(GuiTextField xField, GuiTextField zField) {
        if (mc.getRenderViewEntity() != null) {
            setText(xField, formatDouble(mc.getRenderViewEntity().posX));
            setText(zField, formatDouble(mc.getRenderViewEntity().posZ));
        }
    }

    private void addReturnPointFromInputs() {
        double x = parseDouble(returnPointXField.getText(), Double.NaN);
        double z = parseDouble(returnPointZField.getText(), Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(z)) {
            setStatus("§c回归点坐标无效", 0xFFFF8E8E);
            return;
        }

        if (isReturnPointBoundsValidationReady()) {
            double minX = Math.min(parseDouble(point1XField.getText(), 0.0), parseDouble(point2XField.getText(), 0.0));
            double maxX = Math.max(parseDouble(point1XField.getText(), 0.0), parseDouble(point2XField.getText(), 0.0));
            double minZ = Math.min(parseDouble(point1ZField.getText(), 0.0), parseDouble(point2ZField.getText(), 0.0));
            double maxZ = Math.max(parseDouble(point1ZField.getText(), 0.0), parseDouble(point2ZField.getText(), 0.0));

            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                setStatus("§c添加失败：回归点必须位于范围点1和范围点2范围内", 0xFFFF8E8E);
                return;
            }
        }

        for (AutoFollowHandler.Point point : editorReturnPoints) {
            if (point != null && Math.abs(point.x - x) < 0.01 && Math.abs(point.z - z) < 0.01) {
                setStatus("§e该回归点已存在，无需重复添加", 0xFFFFD27F);
                return;
            }
        }

        editorReturnPoints.add(new AutoFollowHandler.Point(x, z));
        selectedReturnPointIndices.clear();
        selectedReturnPointIndices.add(editorReturnPoints.size() - 1);
        returnPointSelectionAnchor = editorReturnPoints.size() - 1;
        ensureReturnPointVisible(editorReturnPoints.size() - 1);
        clampReturnPointListScroll();
        updateButtonStates();
        setStatus("§a已添加回归点 (" + formatDouble(x) + ", " + formatDouble(z) + ")", 0xFF8CFF9E);
    }

    private void removeLastReturnPoint() {
        if (editorReturnPoints.isEmpty()) {
            setStatus("§c当前没有可删除的回归点", 0xFFFF8E8E);
            return;
        }

        AutoFollowHandler.Point removed = editorReturnPoints.remove(editorReturnPoints.size() - 1);
        if (removed != null) {
            setText(returnPointXField, formatDouble(removed.x));
            setText(returnPointZField, formatDouble(removed.z));
        }
        clampReturnPointListScroll();
        updateButtonStates();
        setStatus("§a已删除最后一个回归点", 0xFF8CFF9E);
    }

    private void normalizeReturnPointSelection() {
        selectedReturnPointIndices.removeIf(index -> index == null || index < 0 || index >= editorReturnPoints.size());
        if (returnPointSelectionAnchor >= editorReturnPoints.size()) {
            returnPointSelectionAnchor = editorReturnPoints.isEmpty() ? -1 : editorReturnPoints.size() - 1;
        }
        if (editorReturnPoints.isEmpty()) {
            returnPointSelectionAnchor = -1;
        }
    }

    private boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private int getReturnPointIndexAt(int mouseX, int mouseY) {
        if (!isInReturnPointListBox(mouseX, mouseY) || editorReturnPoints.isEmpty()) {
            return -1;
        }
        int innerY = getReturnListBoxY() + 4;
        int localY = mouseY - innerY;
        if (localY < 0) {
            return -1;
        }
        int slot = localY / RETURN_LIST_ITEM_HEIGHT;
        int actualIndex = returnPointListScrollOffset + slot;
        return actualIndex >= 0 && actualIndex < editorReturnPoints.size() ? actualIndex : -1;
    }

    private void ensureReturnPointVisible(int index) {
        if (index < 0) {
            return;
        }
        int visibleCount = getReturnPointListVisibleCount();
        if (index < returnPointListScrollOffset) {
            returnPointListScrollOffset = index;
        } else if (index >= returnPointListScrollOffset + visibleCount) {
            returnPointListScrollOffset = index - visibleCount + 1;
        }
        clampReturnPointListScroll();
    }

    private void handleReturnPointListSelectionClick(int mouseX, int mouseY) {
        int index = getReturnPointIndexAt(mouseX, mouseY);
        if (index < 0) {
            if (!isCtrlDown() && !isShiftDown()) {
                selectedReturnPointIndices.clear();
                returnPointSelectionAnchor = -1;
                updateButtonStates();
            }
            return;
        }

        boolean shift = isShiftDown();
        boolean ctrl = isCtrlDown();

        if (shift && returnPointSelectionAnchor >= 0) {
            int start = Math.min(returnPointSelectionAnchor, index);
            int end = Math.max(returnPointSelectionAnchor, index);
            if (!ctrl) {
                selectedReturnPointIndices.clear();
            }
            for (int i = start; i <= end; i++) {
                selectedReturnPointIndices.add(i);
            }
        } else if (ctrl) {
            if (selectedReturnPointIndices.contains(index)) {
                selectedReturnPointIndices.remove(index);
            } else {
                selectedReturnPointIndices.add(index);
            }
            returnPointSelectionAnchor = index;
        } else {
            selectedReturnPointIndices.clear();
            selectedReturnPointIndices.add(index);
            returnPointSelectionAnchor = index;
        }

        ensureReturnPointVisible(index);
        updateButtonStates();
    }

    private void beginDeleteSelectedReturnPoints() {
        normalizeReturnPointSelection();
        if (selectedReturnPointIndices.isEmpty()) {
            setStatus("§c请先选择要删除的回点", 0xFFFF8E8E);
            return;
        }

        List<Integer> indexes = new ArrayList<>(selectedReturnPointIndices);
        Collections.sort(indexes);
        Collections.reverse(indexes);

        for (int index : indexes) {
            if (index >= 0 && index < editorReturnPoints.size()) {
                editorReturnPoints.remove(index);
            }
        }

        selectedReturnPointIndices.clear();
        returnPointSelectionAnchor = editorReturnPoints.isEmpty() ? -1 : 0;
        if (!editorReturnPoints.isEmpty()) {
            selectedReturnPointIndices.add(Math.min(returnPointSelectionAnchor, editorReturnPoints.size() - 1));
        }
        clampReturnPointListScroll();
        updateButtonStates();
        setStatus("§a已删除选中回点", 0xFF8CFF9E);
    }

    private AutoFollowHandler.Point getLookedBlockPoint() {
        RayTraceResult result = mc == null ? null : mc.objectMouseOver;
        if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || result.getBlockPos() == null) {
            return null;
        }
        BlockPos pos = result.getBlockPos();
        return new AutoFollowHandler.Point(pos.getX(), pos.getZ());
    }

    private int findReturnPointIndex(double x, double z) {
        for (int i = 0; i < editorReturnPoints.size(); i++) {
            AutoFollowHandler.Point point = editorReturnPoints.get(i);
            if (point != null && Math.abs(point.x - x) < 0.01 && Math.abs(point.z - z) < 0.01) {
                return i;
            }
        }
        return -1;
    }

    private int findLookedReturnPointIndex() {
        if (mc == null || mc.player == null || editorReturnPoints.isEmpty()) {
            return -1;
        }

        Vec3d eyePos = mc.player.getPositionEyes(1.0F);
        Vec3d lookVec = mc.player.getLook(1.0F);
        Vec3d end = eyePos.add(lookVec.scale(64.0));

        double baseY = mc.player.posY - 1.0;
        double topY = mc.player.posY + 2.0;

        int bestIndex = -1;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < editorReturnPoints.size(); i++) {
            AutoFollowHandler.Point point = editorReturnPoints.get(i);
            if (point == null) {
                continue;
            }

            AxisAlignedBB box = new AxisAlignedBB(
                    point.x - 2.0,
                    baseY,
                    point.z - 2.0,
                    point.x + 3.0,
                    topY,
                    point.z + 3.0);

            RayTraceResult intercept = box.calculateIntercept(eyePos, end);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }

            double distSq = eyePos.squareDistanceTo(intercept.hitVec);
            if (distSq < bestDistanceSq) {
                bestDistanceSq = distSq;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private void handleMiddleClickReturnPointAction() {
        int lookedReturnPointIndex = findLookedReturnPointIndex();
        if (lookedReturnPointIndex >= 0) {
            selectedReturnPointIndices.clear();
            selectedReturnPointIndices.add(lookedReturnPointIndex);
            returnPointSelectionAnchor = lookedReturnPointIndex;
            ensureReturnPointVisible(lookedReturnPointIndex);
            updateButtonStates();
            AutoFollowHandler.Point point = editorReturnPoints.get(lookedReturnPointIndex);
            setStatus("§e该回点已存在 (" + formatDouble(point.x) + ", " + formatDouble(point.z) + ")，删除请在列表中选中后点击“删除选中”",
                    0xFFFFD27F);
            return;
        }

        AutoFollowHandler.Point target = getLookedBlockPoint();
        if (target == null) {
            setStatus("§e未命中回点框，也未对准方块，无法通过中键添加/删除回点", 0xFFFFD27F);
            return;
        }

        double minX = Math.min(parseDouble(point1XField.getText(), 0.0), parseDouble(point2XField.getText(), 0.0));
        double maxX = Math.max(parseDouble(point1XField.getText(), 0.0), parseDouble(point2XField.getText(), 0.0));
        double minZ = Math.min(parseDouble(point1ZField.getText(), 0.0), parseDouble(point2ZField.getText(), 0.0));
        double maxZ = Math.max(parseDouble(point1ZField.getText(), 0.0), parseDouble(point2ZField.getText(), 0.0));

        if (target.x < minX || target.x > maxX || target.z < minZ || target.z > maxZ) {
            setStatus("§c目标方块不在范围点1和范围点2圈定范围内", 0xFFFF8E8E);
            return;
        }

        int existingIndex = findReturnPointIndex(target.x, target.z);
        if (existingIndex >= 0) {
            selectedReturnPointIndices.clear();
            selectedReturnPointIndices.add(existingIndex);
            returnPointSelectionAnchor = existingIndex;
            ensureReturnPointVisible(existingIndex);
            updateButtonStates();
            setStatus("§e该回点已存在，删除请在列表中选中后点击“删除选中”", 0xFFFFD27F);
            return;
        }

        editorReturnPoints.add(new AutoFollowHandler.Point(target.x, target.z));
        selectedReturnPointIndices.clear();
        selectedReturnPointIndices.add(editorReturnPoints.size() - 1);
        returnPointSelectionAnchor = editorReturnPoints.size() - 1;
        ensureReturnPointVisible(editorReturnPoints.size() - 1);
        clampReturnPointListScroll();
        updateButtonStates();
        setStatus("§a已通过中键添加回点 (" + formatDouble(target.x) + ", " + formatDouble(target.z) + ")", 0xFF8CFF9E);
    }

    private List<AutoFollowHandler.Point> copyReturnPoints(List<AutoFollowHandler.Point> source) {
        List<AutoFollowHandler.Point> copied = new ArrayList<>();
        if (source == null) {
            return copied;
        }
        for (AutoFollowHandler.Point point : source) {
            if (point != null) {
                copied.add(AutoFollowRule.copyPoint(point));
            }
        }
        return copied;
    }

    private void drawContextMenu(int mouseX, int mouseY) {
        int height = contextMenuItems.size() * contextMenuItemHeight + 4;
        int x = Math.min(contextMenuX, this.width - contextMenuWidth - 6);
        int y = Math.min(contextMenuY, this.height - height - 6);

        drawRect(x, y, x + contextMenuWidth, y + height, 0xEE111A22);
        drawHorizontalLine(x, x + contextMenuWidth, y, 0xFF6FB8FF);
        drawHorizontalLine(x, x + contextMenuWidth, y + height, 0xFF35536C);
        drawVerticalLine(x, y, y + height, 0xFF35536C);
        drawVerticalLine(x + contextMenuWidth, y, y + height, 0xFF35536C);

        for (int i = 0; i < contextMenuItems.size(); i++) {
            ContextMenuItem item = contextMenuItems.get(i);
            int itemY = y + 2 + i * contextMenuItemHeight;
            boolean hovered = mouseX >= x + 2 && mouseX <= x + contextMenuWidth - 2
                    && mouseY >= itemY && mouseY <= itemY + contextMenuItemHeight - 1;
            if (hovered) {
                drawRect(x + 2, itemY, x + contextMenuWidth - 2, itemY + contextMenuItemHeight - 1, 0xCC2B5A7C);
            }
            drawString(fontRenderer, item.label, x + 8, itemY + 5, item.enabled ? 0xFFFFFFFF : 0xFF777777);
        }
    }

    private void drawTreeDragOverlay(int mouseX, int mouseY) {
        if (activeDragPayload == null) {
            return;
        }
        String label = activeDragPayload.type == DragPayload.TYPE_CATEGORY
                ? "移动分组: " + activeDragPayload.label
                : "移动规则: " + activeDragPayload.label;
        int width = Math.min(260, fontRenderer.getStringWidth(label) + 16);
        int x = Math.min(mouseX + 12, this.width - width - 8);
        int y = Math.min(mouseY + 10, this.height - 22);
        drawRect(x, y, x + width, y + 18, 0xDD16212B);
        drawHorizontalLine(x, x + width, y, 0xFF77C4FF);
        drawHorizontalLine(x, x + width, y + 18, 0xFF35536C);
        drawVerticalLine(x, y, y + 18, 0xFF35536C);
        drawVerticalLine(x + width, y, y + 18, 0xFF35536C);
        drawString(fontRenderer, trimToWidth(label, width - 10), x + 5, y + 5, 0xFFFFFFFF);
    }

    private void drawTreeDropIndicator(TreeDropTarget target) {
        if (target == null) {
            return;
        }
        if (target.highlightCategoryIndex >= 0 && target.highlightCategoryIndex < visibleTreeRows.size()) {
            int drawIndex = target.highlightCategoryIndex - treeScrollOffset;
            if (drawIndex >= 0 && drawIndex < getVisibleTreeRowCount()) {
                int rowY = treeY + 22 + drawIndex * TREE_ROW_HEIGHT;
                drawRect(treeX + 5, rowY, treeX + treeWidth - 9, rowY + TREE_ROW_HEIGHT - 2, 0x553A86B8);
            }
        }
        if (target.lineY > 0) {
            drawRect(treeX + 8, target.lineY - 1, treeX + treeWidth - 12, target.lineY + 1, 0xFF7FD4FF);
            drawRect(treeX + 8, target.lineY - 3, treeX + 12, target.lineY + 3, 0xFF7FD4FF);
            drawRect(treeX + treeWidth - 16, target.lineY - 3, treeX + treeWidth - 12, target.lineY + 3, 0xFF7FD4FF);
        }
    }

    private DragPayload buildDragPayload(TreeRow row) {
        if (row == null) {
            return null;
        }
        if (row.type == TreeRow.TYPE_CATEGORY) {
            return DragPayload.forCategory(row.category, row.label);
        }
        if (row.type == TreeRow.TYPE_RULE && row.ruleIndex >= 0 && row.ruleIndex < allRules.size()) {
            return DragPayload.forRule(allRules.get(row.ruleIndex), row.label);
        }
        return null;
    }

    private TreeDropTarget computeTreeDropTarget(int mouseX, int mouseY, DragPayload payload) {
        if (payload == null || treeCollapsed || !isInTree(mouseX, mouseY)) {
            return null;
        }
        int actualIndex = getTreeRowIndexAt(mouseY);
        if (actualIndex < 0 || actualIndex >= visibleTreeRows.size()) {
            return null;
        }
        TreeRow row = visibleTreeRows.get(actualIndex);
        if (payload.type == DragPayload.TYPE_CATEGORY) {
            if (row.type != TreeRow.TYPE_CATEGORY || row.category.equalsIgnoreCase(payload.category)) {
                return null;
            }
            boolean after = mouseY >= getTreeRowTop(actualIndex) + TREE_ROW_HEIGHT / 2;
            return TreeDropTarget.forCategory(actualIndex, row.category, after, getInsertionLineY(actualIndex, after));
        }
        if (row.type == TreeRow.TYPE_ALL || !isConcreteCategory(row.category)) {
            return null;
        }
        if (row.type == TreeRow.TYPE_CATEGORY) {
            boolean append = mouseY >= getTreeRowTop(actualIndex) + TREE_ROW_HEIGHT / 2;
            int anchorIndex = append ? findLastRowIndexOfCategory(row.category) : actualIndex;
            return TreeDropTarget.forCategoryRule(row.category, append, actualIndex,
                    getInsertionLineY(anchorIndex, append));
        }
        if (row.ruleIndex < 0 || row.ruleIndex >= allRules.size()) {
            return null;
        }
        AutoFollowRule targetRule = allRules.get(row.ruleIndex);
        if (payload.rule == targetRule) {
            return null;
        }
        boolean before = mouseY < getTreeRowTop(actualIndex) + TREE_ROW_HEIGHT / 2;
        return TreeDropTarget.forRule(row.category, targetRule, before, !before, actualIndex,
                getInsertionLineY(actualIndex, !before));
    }

    private int getTreeRowTop(int actualIndex) {
        return treeY + 22 + (actualIndex - treeScrollOffset) * TREE_ROW_HEIGHT;
    }

    private int getInsertionLineY(int actualIndex, boolean afterRow) {
        return getTreeRowTop(actualIndex) + (afterRow ? TREE_ROW_HEIGHT - 2 : 0);
    }

    private int findLastRowIndexOfCategory(String category) {
        int last = -1;
        for (int i = 0; i < visibleTreeRows.size(); i++) {
            TreeRow row = visibleTreeRows.get(i);
            if (safe(category).equalsIgnoreCase(row.category)) {
                last = i;
            } else if (last >= 0 && row.type == TreeRow.TYPE_CATEGORY) {
                break;
            }
        }
        return last >= 0 ? last : 0;
    }

    private void completeTreeDrag() {
        if (!treeDragging || activeDragPayload == null || currentTreeDropTarget == null) {
            return;
        }
        boolean changed = activeDragPayload.type == DragPayload.TYPE_CATEGORY
                ? applyCategoryDrag(activeDragPayload, currentTreeDropTarget)
                : applyRuleDrag(activeDragPayload, currentTreeDropTarget);
        if (!changed) {
            return;
        }
        refreshData(false);
        if (activeDragPayload.rule != null) {
            selectByRuleName(activeDragPayload.rule.name);
        }
    }

    private boolean applyCategoryDrag(DragPayload payload, TreeDropTarget target) {
        if (payload == null || target == null || !isConcreteCategory(payload.category)
                || !isConcreteCategory(target.category)) {
            return false;
        }
        List<String> reordered = new ArrayList<>(categories);
        int fromIndex = findCategoryIndex(reordered, payload.category);
        int targetIndex = findCategoryIndex(reordered, target.category);
        if (fromIndex < 0 || targetIndex < 0 || fromIndex == targetIndex) {
            return false;
        }
        String moved = reordered.remove(fromIndex);
        int insertIndex = target.after ? targetIndex + (fromIndex < targetIndex ? 0 : 1) : targetIndex;
        insertIndex = Math.max(0, Math.min(insertIndex, reordered.size()));
        reordered.add(insertIndex, moved);
        categories.clear();
        categories.addAll(reordered);
        persistTreeStructureChanges();
        setStatus("§a已调整分组顺序: " + moved, 0xFF8CFF9E);
        return true;
    }

    private boolean applyRuleDrag(DragPayload payload, TreeDropTarget target) {
        if (payload == null || payload.rule == null || target == null || !isConcreteCategory(target.category)) {
            return false;
        }
        List<String> categoryOrder = new ArrayList<>(categories);
        if (findCategoryIndex(categoryOrder, target.category) < 0) {
            categoryOrder.add(target.category);
        }
        java.util.LinkedHashMap<String, List<AutoFollowRule>> grouped = buildGroupedRules(categoryOrder);
        String sourceCategory = getRuleCategory(payload.rule);
        List<AutoFollowRule> sourceRules = grouped.get(sourceCategory);
        if (sourceRules == null || !sourceRules.remove(payload.rule)) {
            return false;
        }
        payload.rule.category = target.category;
        List<AutoFollowRule> targetRules = grouped.get(target.category);
        if (targetRules == null) {
            targetRules = new ArrayList<>();
            grouped.put(target.category, targetRules);
        }
        int insertIndex;
        if (target.targetRule != null) {
            insertIndex = targetRules.indexOf(target.targetRule);
            if (insertIndex < 0) {
                insertIndex = targetRules.size();
            } else if (target.after) {
                insertIndex++;
            }
        } else {
            insertIndex = target.appendToCategory ? targetRules.size() : 0;
        }
        insertIndex = Math.max(0, Math.min(insertIndex, targetRules.size()));
        targetRules.add(insertIndex, payload.rule);

        allRules.clear();
        for (String category : categoryOrder) {
            List<AutoFollowRule> rules = grouped.get(safe(category));
            if (rules != null) {
                allRules.addAll(rules);
            }
        }
        persistTreeStructureChanges();
        selectedCategory = target.category;
        setStatus("§a已移动规则: " + safe(payload.rule.name), 0xFF8CFF9E);
        return true;
    }

    private java.util.LinkedHashMap<String, List<AutoFollowRule>> buildGroupedRules(List<String> categoryOrder) {
        java.util.LinkedHashMap<String, List<AutoFollowRule>> grouped = new java.util.LinkedHashMap<>();
        for (String category : categoryOrder) {
            grouped.put(safe(category), new ArrayList<AutoFollowRule>());
        }
        for (AutoFollowRule rule : allRules) {
            String category = getRuleCategory(rule);
            if (!grouped.containsKey(category)) {
                grouped.put(category, new ArrayList<AutoFollowRule>());
            }
            grouped.get(category).add(rule);
        }
        return grouped;
    }

    private int findCategoryIndex(List<String> source, String category) {
        for (int i = 0; i < source.size(); i++) {
            if (safe(source.get(i)).equalsIgnoreCase(safe(category))) {
                return i;
            }
        }
        return -1;
    }

    private void persistTreeStructureChanges() {
        AutoFollowHandler.replaceCategoryOrder(new ArrayList<>(categories));
        AutoFollowHandler.rules.clear();
        AutoFollowHandler.rules.addAll(allRules);
        AutoFollowHandler.saveFollowConfig();
        if (parentScreen instanceof GuiPathManager) {
            ((GuiPathManager) parentScreen).requestReloadFromManager();
        }
    }

    private void resetTreeDragState() {
        pendingTreePressIndex = -1;
        treeDragging = false;
        activeDragPayload = null;
        currentTreeDropTarget = null;
    }

    private boolean handleContextMenuClick(int mouseX, int mouseY, int mouseButton) {
        if (!contextMenuVisible) {
            return false;
        }

        int height = contextMenuItems.size() * contextMenuItemHeight + 4;
        int x = Math.min(contextMenuX, this.width - contextMenuWidth - 6);
        int y = Math.min(contextMenuY, this.height - height - 6);

        if (mouseX < x || mouseX > x + contextMenuWidth || mouseY < y || mouseY > y + height) {
            return false;
        }

        if (mouseButton != 0) {
            closeContextMenu();
            return true;
        }

        int local = mouseY - (y + 2);
        int index = local / contextMenuItemHeight;
        if (index < 0 || index >= contextMenuItems.size()) {
            closeContextMenu();
            return true;
        }

        ContextMenuItem item = contextMenuItems.get(index);
        if (!item.enabled) {
            closeContextMenu();
            return true;
        }

        int previousTargetIndex = contextMenuTargetIndex;
        String targetType = contextMenuTargetType;
        String targetCategory = contextMenuTargetCategory;
        selectedVisibleIndex = contextMenuTargetIndex;
        closeContextMenu();

        if ("tree".equals(targetType)) {
            handleTreeContextMenuAction(item.key, targetCategory);
            return true;
        }

        selectedVisibleIndex = previousTargetIndex;
        if ("new".equals(item.key)) {
            creatingNew = true;
            clearEditorForNew();
        } else if ("duplicate".equals(item.key)) {
            duplicateSelectedRule();
        } else if ("copy_name".equals(item.key)) {
            copySelectedRuleName();
        } else if ("delete".equals(item.key)) {
            deleteSelectedRule();
        } else if ("move".equals(item.key)) {
            moveSelectedRuleToCurrentCategory();
        } else if ("reload".equals(item.key)) {
            reloadAllRules();
        }
        return true;
    }

    private void openContextMenu(int mouseX, int mouseY, int targetIndex) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetIndex = targetIndex;
        contextMenuTargetType = "card";
        contextMenuTargetCategory = "";
        contextMenuItems.clear();

        AutoFollowRule rule = null;
        if (targetIndex >= 0 && targetIndex < visibleRules.size()) {
            rule = visibleRules.get(targetIndex);
        }

        contextMenuItems.add(new ContextMenuItem("new", "新增规则", true));
        contextMenuItems.add(new ContextMenuItem("reload", "重载规则", true));

        if (rule != null) {
            contextMenuItems.add(new ContextMenuItem("duplicate", "复制当前规则", true));
            contextMenuItems.add(new ContextMenuItem("copy_name", "复制规则名", !isBlank(rule.name)));
            contextMenuItems.add(new ContextMenuItem("delete", "删除规则", true));
            boolean canMove = isConcreteCategory(selectedCategory)
                    && !selectedCategory.equalsIgnoreCase(getRuleCategory(rule));
            contextMenuItems.add(new ContextMenuItem("move", "移动到当前分组", canMove));
        }
    }

    private void openTreeContextMenu(int mouseX, int mouseY, TreeRow row) {
        contextMenuVisible = true;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        contextMenuTargetIndex = -1;
        contextMenuTargetType = "tree";
        contextMenuTargetCategory = row == null ? "" : safe(row.category);
        contextMenuItems.clear();

        boolean isAllRow = row != null && row.type == TreeRow.TYPE_ALL;
        boolean isCategoryRow = row != null && row.type == TreeRow.TYPE_CATEGORY;
        boolean concreteCategory = isConcreteCategory(contextMenuTargetCategory);

        contextMenuItems.add(new ContextMenuItem("tree_new", "新建分组", true));
        contextMenuItems.add(new ContextMenuItem("tree_paste", "从剪贴板新建分组", true));

        if (isCategoryRow) {
            contextMenuItems.add(new ContextMenuItem("tree_copy_name", "复制分组名", true));
            contextMenuItems.add(new ContextMenuItem("tree_rename", "重命名分组", concreteCategory));
            contextMenuItems.add(new ContextMenuItem("tree_delete", "删除分组", concreteCategory));
        } else if (isAllRow) {
            contextMenuItems.add(new ContextMenuItem("tree_reload", "重载规则", true));
        }
    }

    private void closeContextMenu() {
        contextMenuVisible = false;
        contextMenuItems.clear();
        contextMenuTargetIndex = -1;
        contextMenuTargetType = "card";
        contextMenuTargetCategory = "";
    }

    private void handleTreeContextMenuAction(String key, String targetCategory) {
        if ("tree_new".equals(key)) {
            openAddCategoryDialog();
            return;
        }
        if ("tree_paste".equals(key)) {
            pasteCategoryFromClipboard();
            return;
        }
        if ("tree_copy_name".equals(key)) {
            copyCategoryName(targetCategory);
            return;
        }
        if ("tree_rename".equals(key)) {
            openRenameCategoryDialog(targetCategory);
            return;
        }
        if ("tree_delete".equals(key)) {
            deleteCategoryByName(targetCategory);
            return;
        }
        if ("tree_reload".equals(key)) {
            reloadAllRules();
        }
    }

    private void openAddCategoryDialog() {
        mc.displayGuiScreen(new GuiTextInput(this, "输入新分组名称", value -> {
            String normalized = value == null ? "" : value.trim();
            if (normalized.isEmpty()) {
                setStatus("§7已取消创建分组", 0xFFB8C7D9);
                return;
            }
            boolean ok = AutoFollowHandler.addCategory(normalized);
            if (ok) {
                selectedCategory = normalized;
                expandedCategories.add(normalized);
                refreshData(true);
                setStatus("§a已创建分组: " + normalized, 0xFF8CFF9E);
            } else {
                setStatus("§c创建分组失败，名称可能重复或无效", 0xFFFF8E8E);
            }
        }));
    }

    private void pasteCategoryFromClipboard() {
        String text = getClipboardString();
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            setStatus("§c剪贴板为空，无法新建分组", 0xFFFF8E8E);
            return;
        }
        boolean ok = AutoFollowHandler.addCategory(normalized);
        if (ok) {
            selectedCategory = normalized;
            expandedCategories.add(normalized);
            refreshData(true);
            setStatus("§a已从剪贴板新建分组: " + normalized, 0xFF8CFF9E);
        } else {
            setStatus("§c新建分组失败，名称可能重复或无效", 0xFFFF8E8E);
        }
    }

    private void copyCategoryName(String category) {
        String normalized = safe(category).trim();
        if (normalized.isEmpty()) {
            setStatus("§c没有可复制的分组名", 0xFFFF8E8E);
            return;
        }
        setClipboardString(normalized);
        setStatus("§a已复制分组名: " + normalized, 0xFF8CFF9E);
    }

    private void openRenameCategoryDialog(String category) {
        final String normalized = safe(category).trim();
        if (!isConcreteCategory(normalized)) {
            setStatus("§c该分组不能重命名", 0xFFFF8E8E);
            return;
        }
        mc.displayGuiScreen(new GuiTextInput(this, "重命名分组", normalized, value -> {
            String newName = value == null ? "" : value.trim();
            if (newName.isEmpty()) {
                setStatus("§7已取消重命名分组", 0xFFB8C7D9);
                return;
            }
            boolean ok = AutoFollowHandler.renameCategory(normalized, newName);
            if (ok) {
                expandedCategories.remove(normalized);
                expandedCategories.add(newName);
                selectedCategory = newName;
                refreshData(true);
                setStatus("§a已重命名分组: " + normalized + " -> " + newName, 0xFF8CFF9E);
            } else {
                setStatus("§c重命名分组失败，名称可能重复或无效", 0xFFFF8E8E);
            }
        }));
    }

    private void deleteCategoryByName(String category) {
        String normalized = safe(category).trim();
        if (!isConcreteCategory(normalized)) {
            setStatus("§c该分组不能删除", 0xFFFF8E8E);
            return;
        }
        if (AutoFollowHandler.deleteCategory(normalized)) {
            expandedCategories.remove(normalized);
            selectedCategory = CATEGORY_ALL;
            refreshData(false);
            setStatus("§a已删除分组: " + normalized + "（原分组规则已转为默认分组）", 0xFF8CFF9E);
        } else {
            setStatus("§c删除分组失败", 0xFFFF8E8E);
        }
    }

    private boolean isConcreteCategory(String category) {
        return !isBlank(category) && !CATEGORY_ALL.equals(category);
    }

    private int getVisibleTreeRowCount() {
        return Math.max(1, (treeHeight - 26) / TREE_ROW_HEIGHT);
    }

    private int getVisibleCardCount() {
        return Math.max(1, (listHeight - 28) / (CARD_HEIGHT + CARD_GAP));
    }

    private int getTreeRowIndexAt(int mouseY) {
        int contentY = treeY + 22;
        int localY = mouseY - contentY;
        if (localY < 0) {
            return -1;
        }
        int localIndex = localY / TREE_ROW_HEIGHT;
        int actualIndex = treeScrollOffset + localIndex;
        return actualIndex >= 0 && actualIndex < visibleTreeRows.size() ? actualIndex : -1;
    }

    private int getCardIndexAt(int mouseY) {
        int contentY = listY + 22;
        int local = mouseY - contentY;
        if (local < 0) {
            return -1;
        }
        int step = CARD_HEIGHT + CARD_GAP;
        int localIndex = local / step;
        int yInCard = local % step;
        if (yInCard >= CARD_HEIGHT) {
            return -1;
        }
        return listScrollOffset + localIndex;
    }

    private void ensureCardSelectionVisible() {
        if (selectedVisibleIndex < 0) {
            return;
        }
        int visible = getVisibleCardCount();
        if (selectedVisibleIndex < listScrollOffset) {
            listScrollOffset = selectedVisibleIndex;
        } else if (selectedVisibleIndex >= listScrollOffset + visible) {
            listScrollOffset = selectedVisibleIndex - visible + 1;
        }
    }

    private boolean isInTree(int mouseX, int mouseY) {
        return mouseX >= treeX && mouseX <= treeX + treeWidth && mouseY >= treeY && mouseY <= treeY + treeHeight;
    }

    private boolean isInCardList(int mouseX, int mouseY) {
        return mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight;
    }

    private boolean isInEditor(int mouseX, int mouseY) {
        return mouseX >= editorX && mouseX <= editorX + editorWidth
                && mouseY >= editorY && mouseY <= editorY + editorHeight;
    }

    private boolean isInTreeCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, treeX, treeY, treeWidth);
    }

    private boolean isInListCollapseButton(int mouseX, int mouseY) {
        return isInCollapseButton(mouseX, mouseY, listX, listY, listWidth);
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

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void setText(GuiTextField field, String value) {
        if (field != null) {
            field.setText(value == null ? "" : value);
        }
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
        return COORD_FORMAT.format(value);
    }

    private String getReturnPointSummary(AutoFollowRule rule) {
        if (rule == null) {
            return "回点: --";
        }
        rule.updateBounds();
        rule.ensureReturnPoints();
        int count = rule.returnPoints == null ? 0 : rule.returnPoints.size();
        AutoFollowHandler.Point primary = rule.getPrimaryReturnPoint();
        return "回点数:" + count
                + " 首点(" + formatDouble(primary == null ? 0 : primary.x)
                + ", " + formatDouble(primary == null ? 0 : primary.z) + ")"
                + " 停留:" + Math.max(1, rule.returnStayMillis) + "ms"
                + " 到达:" + formatDouble(rule.returnArriveDistance <= 0
                ? AutoFollowRule.DEFAULT_RETURN_ARRIVE_DISTANCE
                : rule.returnArriveDistance)
                + " 上:" + formatDouble(rule.monsterUpwardRange <= 0
                ? AutoFollowRule.DEFAULT_MONSTER_UPWARD_RANGE
                : rule.monsterUpwardRange)
                + " 下:" + formatDouble(rule.monsterDownwardRange <= 0
                ? AutoFollowRule.DEFAULT_MONSTER_DOWNWARD_RANGE
                : rule.monsterDownwardRange)
                + " 追界:" + formatDouble(rule.lockChaseOutOfBoundsDistance <= 0
                ? AutoFollowRule.DEFAULT_LOCK_CHASE_OUT_OF_BOUNDS_DISTANCE
                : rule.lockChaseOutOfBoundsDistance)
                + " " + getPatrolModeDisplay(rule.patrolMode);
    }

    private String getDistanceToReturnPointText(AutoFollowRule rule) {
        if (rule == null || this.mc == null || this.mc.player == null) {
            return "--";
        }
        rule.updateBounds();
        rule.ensureReturnPoints();

        if (rule.returnPoints == null || rule.returnPoints.isEmpty()) {
            AutoFollowHandler.Point primary = rule.point3;
            if (primary == null) {
                return "--";
            }
            double dx = this.mc.player.posX - primary.x;
            double dz = this.mc.player.posZ - primary.z;
            return formatDouble(Math.sqrt(dx * dx + dz * dz));
        }

        double best = Double.MAX_VALUE;
        for (AutoFollowHandler.Point point : rule.returnPoints) {
            if (point == null) {
                continue;
            }
            double dx = this.mc.player.posX - point.x;
            double dz = this.mc.player.posZ - point.z;
            best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
        }
        return best == Double.MAX_VALUE ? "--" : formatDouble(best);
    }

    private String boolText(boolean enabled) {
        return enabled ? "开" : "关";
    }

    private String yesNo(boolean yes) {
        return yes ? "是" : "否";
    }

    private String getPatrolModeDisplay(String patrolMode) {
        return AutoFollowRule.PATROL_MODE_RANDOM.equalsIgnoreCase(patrolMode) ? "回点间随机" : "按添加顺序";
    }

    private String getMonsterChaseModeDisplay(String chaseMode) {
        return AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE.equalsIgnoreCase(chaseMode) ? "固定距离模式" : "追击模式";
    }

    private boolean isEditorMonsterFixedDistanceMode() {
        return AutoFollowRule.MONSTER_CHASE_MODE_FIXED_DISTANCE.equalsIgnoreCase(editorMonsterChaseMode);
    }

    private boolean isApproximatelyZero(double value) {
        return Math.abs(value) < 0.01;
    }

    private boolean isReturnPointBoundsValidationReady() {
        double point1X = parseDouble(point1XField == null ? "" : point1XField.getText(), 0.0);
        double point1Z = parseDouble(point1ZField == null ? "" : point1ZField.getText(), 0.0);
        double point2X = parseDouble(point2XField == null ? "" : point2XField.getText(), 0.0);
        double point2Z = parseDouble(point2ZField == null ? "" : point2ZField.getText(), 0.0);

        boolean point1Unset = isApproximatelyZero(point1X) && isApproximatelyZero(point1Z);
        boolean point2Unset = isApproximatelyZero(point2X) && isApproximatelyZero(point2Z);
        return !point1Unset && !point2Unset;
    }

    private List<String> parseNameList(String text) {
        List<String> result = new ArrayList<>();
        String normalized = safe(text).replace('，', ',').replace('\n', ',').replace('\r', ',');
        for (String part : normalized.split(",")) {
            String item = KillAuraHandler.normalizeFilterName(part);
            if (!item.isEmpty() && !containsIgnoreCase(result, item)) {
                result.add(item);
            }
        }
        return result;
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

    private String joinNameList(List<String> values) {
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

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0 || this.fontRenderer == null) {
            return "";
        }
        if (this.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && this.fontRenderer.getStringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return end <= 0 ? ellipsis : text.substring(0, end) + ellipsis;
    }

    private static final class TreeRow {
        private static final int TYPE_ALL = 0;
        private static final int TYPE_CATEGORY = 1;
        private static final int TYPE_RULE = 2;

        private final int type;
        private final String label;
        private final String category;
        private final String ruleName;
        private final int indent;
        private final int ruleIndex;

        private TreeRow(int type, String label, String category, String ruleName, int indent, int ruleIndex) {
            this.type = type;
            this.label = label == null ? "" : label;
            this.category = category == null ? "" : category;
            this.ruleName = ruleName == null ? "" : ruleName;
            this.indent = indent;
            this.ruleIndex = ruleIndex;
        }

        private static TreeRow allRow(String label) {
            return new TreeRow(TYPE_ALL, label, CATEGORY_ALL, "", 0, -1);
        }

        private static TreeRow categoryRow(String category) {
            return new TreeRow(TYPE_CATEGORY, category, category, "", 0, -1);
        }

        private static TreeRow ruleRow(String category, String ruleName, String displayName, int ruleIndex) {
            return new TreeRow(TYPE_RULE, displayName, category, ruleName, 1, ruleIndex);
        }
    }

    private static final class ContextMenuItem {
        private final String key;
        private final String label;
        private final boolean enabled;

        private ContextMenuItem(String key, String label, boolean enabled) {
            this.key = key;
            this.label = label;
            this.enabled = enabled;
        }
    }

    private static final class DragPayload {
        private static final int TYPE_CATEGORY = 1;
        private static final int TYPE_RULE = 2;

        private final int type;
        private final String category;
        private final String label;
        private final AutoFollowRule rule;

        private DragPayload(int type, String category, String label, AutoFollowRule rule) {
            this.type = type;
            this.category = category == null ? "" : category;
            this.label = label == null ? "" : label;
            this.rule = rule;
        }

        private static DragPayload forCategory(String category, String label) {
            return new DragPayload(TYPE_CATEGORY, category, label, null);
        }

        private static DragPayload forRule(AutoFollowRule rule, String label) {
            return new DragPayload(TYPE_RULE, "", label, rule);
        }
    }

    private static final class TreeDropTarget {
        private final String category;
        private final AutoFollowRule targetRule;
        private final boolean after;
        private final boolean appendToCategory;
        private final int highlightCategoryIndex;
        private final int lineY;

        private TreeDropTarget(String category, AutoFollowRule targetRule, boolean after,
                boolean appendToCategory, int highlightCategoryIndex, int lineY) {
            this.category = category == null ? "" : category;
            this.targetRule = targetRule;
            this.after = after;
            this.appendToCategory = appendToCategory;
            this.highlightCategoryIndex = highlightCategoryIndex;
            this.lineY = lineY;
        }

        private static TreeDropTarget forCategory(int highlightCategoryIndex, String category, boolean after, int lineY) {
            return new TreeDropTarget(category, null, after, false, highlightCategoryIndex, lineY);
        }

        private static TreeDropTarget forRule(String category, AutoFollowRule targetRule, boolean before, boolean after,
                int highlightCategoryIndex, int lineY) {
            return new TreeDropTarget(category, targetRule, after, false, highlightCategoryIndex, lineY);
        }

        private static TreeDropTarget forCategoryRule(String category, boolean appendToCategory,
                int highlightCategoryIndex, int lineY) {
            return new TreeDropTarget(category, null, appendToCategory, appendToCategory,
                    highlightCategoryIndex, lineY);
        }
    }
}
